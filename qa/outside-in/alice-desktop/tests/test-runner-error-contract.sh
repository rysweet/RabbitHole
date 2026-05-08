#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-runner-error-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

bash -c 'set -euo pipefail; source "$1"; read_inventory_json_fields fields "$2" status blocker; printf "unexpected-success\n"' \
  bash "$RUNNER" "$tmp_root/missing-json-fields.json" \
  >"$tmp_root/missing-json-fields.out" 2>"$tmp_root/missing-json-fields.err"
status=$?
assert_failure "$status" "batched JSON field reads fail closed when an artifact is missing"
assert_not_contains "$tmp_root/missing-json-fields.out" 'unexpected-success' "missing JSON field read does not continue with empty values"

printf '{"status":"observed","blocker":""}\n' > "$tmp_root/empty-json-field.json"
bash -c 'set -euo pipefail; source "$1"; read_inventory_json_fields fields "$2" status blocker; printf "count=%s\nstatus=%s\nblocker=<%s>\n" "${#fields[@]}" "${fields[0]}" "${fields[1]}"' \
  bash "$RUNNER" "$tmp_root/empty-json-field.json" \
  >"$tmp_root/empty-json-field.out" 2>"$tmp_root/empty-json-field.err"
status=$?
assert_exit_code "$status" 0 "batched JSON field reads preserve empty trailing fields"
assert_contains "$tmp_root/empty-json-field.out" '^count=2$' "empty trailing JSON field remains addressable"
assert_contains "$tmp_root/empty-json-field.out" '^status=observed$' "batched JSON field read preserves populated fields"
assert_contains "$tmp_root/empty-json-field.out" '^blocker=<>$' "batched JSON field read preserves empty field value"

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

write_legacy_bare_launch_argv() {
  python3 - "$1" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
text = re.sub(
    r"  argv:\n(?:    - .+\n)+  timeoutSeconds:",
    "  argv:\n    - mvn\n    - exec:java\n    - -Dalice-ide\n  timeoutSeconds:",
    text,
    count=1,
)
path.write_text(text, encoding="utf-8")
PY
}

"$RUNNER" run alice-desktop-scene-creation --evidence-dir >"$tmp_root/missing-evidence-dir.out" 2>"$tmp_root/missing-evidence-dir.err"
status=$?
assert_exit_code "$status" 2 "missing --evidence-dir value is a command-line usage error"
assert_contains "$tmp_root/missing-evidence-dir.err" '--evidence-dir requires a value' "missing evidence-dir value reports the specific option"

"$RUNNER" run alice-desktop-launch --timeout-seconds >"$tmp_root/missing-timeout.out" 2>"$tmp_root/missing-timeout.err"
status=$?
assert_exit_code "$status" 2 "missing --timeout-seconds value is a command-line usage error"
assert_contains "$tmp_root/missing-timeout.err" '--timeout-seconds requires a value' "missing timeout value reports the specific option"

"$RUNNER" run alice-desktop-scene-creation --not-a-runner-option >"$tmp_root/unknown-option.out" 2>"$tmp_root/unknown-option.err"
status=$?
assert_exit_code "$status" 2 "unknown runner options are command-line usage errors"
assert_contains "$tmp_root/unknown-option.err" 'unknown argument: --not-a-runner-option' "unknown option error names the rejected option"

nested_catalog="$tmp_root/catalog"
mkdir -p "$nested_catalog/nested"
cp "$BASE_DIR"/scenarios/*.yaml "$nested_catalog"/
cp "$BASE_DIR/scenarios/save-load.yaml" "$nested_catalog/nested/save-load.yaml"
ALICE_QA_SCENARIO_DIR="$nested_catalog" "$RUNNER" run "$nested_catalog/nested/save-load.yaml" --evidence-dir "$tmp_root/evidence" >"$tmp_root/nested-path.out" 2>"$tmp_root/nested-path.err"
status=$?
assert_exit_code "$status" 2 "nested scenario YAML paths are rejected as catalog boundary errors"
assert_contains "$tmp_root/nested-path.err" 'directly inside active scenario directory' "nested path error explains the direct catalog file requirement"

unsafe_catalog="$tmp_root/unsafe-catalog"
mkdir -p "$unsafe_catalog"
cp "$BASE_DIR"/scenarios/*.yaml "$unsafe_catalog"/
python3 - "$unsafe_catalog/launch.yaml" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8").replace("    - mvn\n", "    - bash\n", 1)
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$unsafe_catalog" "$RUNNER" run alice-desktop-launch --evidence-dir "$tmp_root/unsafe-evidence" >"$tmp_root/unsafe.out" 2>"$tmp_root/unsafe.err"
status=$?
assert_failure "$status" "runner rejects unapproved automation argv before launch"
assert_contains "$tmp_root/unsafe.err" 'automation\.argv is restricted' "runner surfaces automation allowlist failures"

legacy_bare_catalog="$tmp_root/legacy-bare-catalog"
mkdir -p "$legacy_bare_catalog"
cp "$BASE_DIR"/scenarios/*.yaml "$legacy_bare_catalog"/
write_legacy_bare_launch_argv "$legacy_bare_catalog/launch.yaml"
ALICE_QA_SCENARIO_DIR="$legacy_bare_catalog" "$RUNNER" run alice-desktop-launch --evidence-dir "$tmp_root/legacy-bare-evidence" >"$tmp_root/legacy-bare.out" 2>"$tmp_root/legacy-bare.err"
status=$?
assert_failure "$status" "runner rejects bare exec:java launch before it can report false display success"
assert_contains "$tmp_root/legacy-bare.err" 'automation\.argv is restricted' "runner bare exec:java error names allowlist"

traversal_cwd_catalog="$tmp_root/traversal-cwd-catalog"
mkdir -p "$traversal_cwd_catalog"
cp "$BASE_DIR"/scenarios/*.yaml "$traversal_cwd_catalog"/
set_launch_cwd "$traversal_cwd_catalog/launch.yaml" ".."
ALICE_QA_SCENARIO_DIR="$traversal_cwd_catalog" "$RUNNER" run alice-desktop-launch --evidence-dir "$tmp_root/traversal-cwd-evidence" >"$tmp_root/traversal-cwd.out" 2>"$tmp_root/traversal-cwd.err"
status=$?
assert_failure "$status" "runner rejects automation cwd path traversal before launch"
assert_contains "$tmp_root/traversal-cwd.err" 'automation\.cwd.*\.\. path traversal' "runner traversal cwd error names path traversal"

absolute_cwd_catalog="$tmp_root/absolute-cwd-catalog"
mkdir -p "$absolute_cwd_catalog"
cp "$BASE_DIR"/scenarios/*.yaml "$absolute_cwd_catalog"/
set_launch_cwd "$absolute_cwd_catalog/launch.yaml" "/"
ALICE_QA_SCENARIO_DIR="$absolute_cwd_catalog" "$RUNNER" run alice-desktop-launch --evidence-dir "$tmp_root/absolute-cwd-evidence" >"$tmp_root/absolute-cwd.out" 2>"$tmp_root/absolute-cwd.err"
status=$?
assert_failure "$status" "runner rejects absolute automation cwd before launch"
assert_contains "$tmp_root/absolute-cwd.err" 'automation\.cwd.*repository-relative.*absolute' "runner absolute cwd error names repository-relative requirement"

symlink_cwd_catalog="$tmp_root/symlink-cwd-catalog"
mkdir -p "$symlink_cwd_catalog"
cp "$BASE_DIR"/scenarios/*.yaml "$symlink_cwd_catalog"/
escape_link="$tmp_root/escape-link"
ln -s "$REPO_ROOT/.." "$escape_link"
escape_cwd=$(python3 - "$REPO_ROOT" "$escape_link" <<'PY'
import os
import sys

print(os.path.relpath(sys.argv[2], sys.argv[1]))
PY
)
set_launch_cwd "$symlink_cwd_catalog/launch.yaml" "$escape_cwd"
ALICE_QA_SCENARIO_DIR="$symlink_cwd_catalog" "$RUNNER" run alice-desktop-launch --evidence-dir "$tmp_root/symlink-cwd-evidence" >"$tmp_root/symlink-cwd.out" 2>"$tmp_root/symlink-cwd.err"
status=$?
assert_failure "$status" "runner rejects automation cwd symlinks that resolve outside the repo"
assert_contains "$tmp_root/symlink-cwd.err" 'automation\.cwd.*resolve inside repository root' "runner symlink cwd error names realpath repo boundary"

finish
