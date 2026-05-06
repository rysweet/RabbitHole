#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-validator-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

set_launch_cwd() {
  python3 - "$1" "$2" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
cwd = sys.argv[2]
text = path.read_text(encoding="utf-8").replace("  cwd: alice-ide\n", f"  cwd: {cwd}\n", 1)
path.write_text(text, encoding="utf-8")
PY
}

"$VALIDATOR" >"$tmp_root/valid.out" 2>"$tmp_root/valid.err"
status=$?
assert_success "$status" "current scenario catalog validates"

"$VALIDATOR" --dump-json >"$tmp_root/catalog.json" 2>"$tmp_root/catalog.err"
status=$?
assert_success "$status" "validator dumps normalized scenario catalog JSON"
python3 - "$tmp_root/catalog.json" "$BASE_DIR/scenarios" >"$tmp_root/catalog-check.out" 2>"$tmp_root/catalog-check.err" <<'PY'
import json
import sys
from pathlib import Path

catalog = json.load(open(sys.argv[1], encoding="utf-8"))
expected_count = len(list(Path(sys.argv[2]).glob("*.yaml")))
if len(catalog) != expected_count:
    raise AssertionError(f"expected {expected_count} scenarios, found {len(catalog)}")
if not all("id" in scenario for scenario in catalog):
    raise AssertionError("every dumped scenario must include an id")
PY
status=$?
assert_success "$status" "catalog JSON contains all scenarios"

missing_dir="$tmp_root/missing-workflow"
mkdir -p "$missing_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$missing_dir"/
rm "$missing_dir/export.yaml"
ALICE_QA_SCENARIO_DIR="$missing_dir" "$VALIDATOR" >"$tmp_root/missing.out" 2>"$tmp_root/missing.err"
status=$?
assert_failure "$status" "validator rejects catalogs missing required workflow coverage"
assert_contains "$tmp_root/missing.err" 'missing workflow.*export|export.*workflow' "missing workflow error names export"

duplicate_dir="$tmp_root/duplicate-workflow"
mkdir -p "$duplicate_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$duplicate_dir"/
perl -0pi -e 's/workflow: export/workflow: save-load/' "$duplicate_dir/export.yaml"
ALICE_QA_SCENARIO_DIR="$duplicate_dir" "$VALIDATOR" >"$tmp_root/duplicate.out" 2>"$tmp_root/duplicate.err"
status=$?
assert_failure "$status" "validator rejects duplicate workflow coverage"
assert_contains "$tmp_root/duplicate.err" 'duplicate workflow|workflow.*save-load' "duplicate workflow error names workflow"

missing_ref_dir="$tmp_root/missing-supporting-evidence"
mkdir -p "$missing_ref_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$missing_ref_dir"/
perl -0pi -e 's/alice-desktop-launch/alice-desktop-does-not-exist/g' "$missing_ref_dir/scene-creation.yaml"
ALICE_QA_SCENARIO_DIR="$missing_ref_dir" "$VALIDATOR" >"$tmp_root/ref.out" 2>"$tmp_root/ref.err"
status=$?
assert_failure "$status" "validator rejects missing supportingEvidence references"
assert_contains "$tmp_root/ref.err" 'supportingEvidence|does-not-exist' "missing supportingEvidence error names reference"

manual_automation_dir="$tmp_root/manual-automation"
mkdir -p "$manual_automation_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$manual_automation_dir"/
cat >> "$manual_automation_dir/save-load.yaml" <<'YAML'
automation:
  argv:
    - mvn
    - exec:java
    - -Dalice-ide
YAML
ALICE_QA_SCENARIO_DIR="$manual_automation_dir" "$VALIDATOR" >"$tmp_root/manual-automation.out" 2>"$tmp_root/manual-automation.err"
status=$?
assert_failure "$status" "validator enforces strict automation object parity when present"
assert_contains "$tmp_root/manual-automation.err" 'automation must include cwd|automation must include timeoutSeconds|automation must include readyWaitSeconds' "strict automation error names missing fields"

unknown_automation_dir="$tmp_root/unknown-automation"
mkdir -p "$unknown_automation_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$unknown_automation_dir"/
perl -0pi -e 's/(  readyWaitSeconds: [0-9]+\n)/$1  extraField: not-supported\n/' "$unknown_automation_dir/launch.yaml"
ALICE_QA_SCENARIO_DIR="$unknown_automation_dir" "$VALIDATOR" >"$tmp_root/unknown-automation.out" 2>"$tmp_root/unknown-automation.err"
status=$?
assert_failure "$status" "validator rejects unknown automation fields"
assert_contains "$tmp_root/unknown-automation.err" 'automation has unknown field.*extraField' "unknown automation error names field"

legacy_command_dir="$tmp_root/legacy-command"
mkdir -p "$legacy_command_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$legacy_command_dir"/
python3 - "$legacy_command_dir/launch.yaml" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
text = text.replace("  argv:\n    - mvn\n    - exec:java\n    - -Dalice-ide\n", "  " + "command: mvn exec:java -Dalice-ide\n")
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$legacy_command_dir" "$VALIDATOR" >"$tmp_root/legacy-command.out" 2>"$tmp_root/legacy-command.err"
status=$?
assert_failure "$status" "validator rejects legacy shell-string automation"
assert_contains "$tmp_root/legacy-command.err" 'automation has unknown field.*command|automation must include argv' "legacy command error requires argv"

unsafe_argv_dir="$tmp_root/unsafe-argv"
mkdir -p "$unsafe_argv_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$unsafe_argv_dir"/
python3 - "$unsafe_argv_dir/launch.yaml" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8").replace("    - mvn\n", "    - bash\n", 1)
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$unsafe_argv_dir" "$VALIDATOR" >"$tmp_root/unsafe-argv.out" 2>"$tmp_root/unsafe-argv.err"
status=$?
assert_failure "$status" "validator rejects unapproved automation argv"
assert_contains "$tmp_root/unsafe-argv.err" 'automation\.argv is restricted' "unsafe argv error names allowlist"

traversal_cwd_dir="$tmp_root/traversal-cwd"
mkdir -p "$traversal_cwd_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$traversal_cwd_dir"/
set_launch_cwd "$traversal_cwd_dir/launch.yaml" ".."
ALICE_QA_SCENARIO_DIR="$traversal_cwd_dir" "$VALIDATOR" >"$tmp_root/traversal-cwd.out" 2>"$tmp_root/traversal-cwd.err"
status=$?
assert_failure "$status" "validator rejects automation cwd path traversal"
assert_contains "$tmp_root/traversal-cwd.err" 'automation\.cwd.*\.\. path traversal' "cwd traversal error names path traversal"

absolute_cwd_dir="$tmp_root/absolute-cwd"
mkdir -p "$absolute_cwd_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$absolute_cwd_dir"/
set_launch_cwd "$absolute_cwd_dir/launch.yaml" "/"
ALICE_QA_SCENARIO_DIR="$absolute_cwd_dir" "$VALIDATOR" >"$tmp_root/absolute-cwd.out" 2>"$tmp_root/absolute-cwd.err"
status=$?
assert_failure "$status" "validator rejects absolute automation cwd"
assert_contains "$tmp_root/absolute-cwd.err" 'automation\.cwd.*repository-relative.*absolute' "absolute cwd error names repository-relative requirement"

symlink_cwd_dir="$tmp_root/symlink-cwd"
mkdir -p "$symlink_cwd_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$symlink_cwd_dir"/
escape_link="$tmp_root/escape-link"
ln -s "$REPO_ROOT/.." "$escape_link"
escape_cwd=$(python3 - "$REPO_ROOT" "$escape_link" <<'PY'
import os
import sys

print(os.path.relpath(sys.argv[2], sys.argv[1]))
PY
)
set_launch_cwd "$symlink_cwd_dir/launch.yaml" "$escape_cwd"
ALICE_QA_SCENARIO_DIR="$symlink_cwd_dir" "$VALIDATOR" >"$tmp_root/symlink-cwd.out" 2>"$tmp_root/symlink-cwd.err"
status=$?
assert_failure "$status" "validator rejects automation cwd symlinks that resolve outside the repo"
assert_contains "$tmp_root/symlink-cwd.err" 'automation\.cwd.*resolve inside repository root' "symlink cwd error names realpath repo boundary"

finish
