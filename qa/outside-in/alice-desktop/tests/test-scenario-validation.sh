#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-scenario-validation.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

TARGET_SCENARIO_ID=alice-desktop-select-project-tab-click-exec
POST_OPEN_SCENARIO_ID=alice-desktop-post-project-open-window-state
TARGET_DISPLAY_NAME="Africa Full"
TARGET_REPOSITORY_PATH="core/resources/src/application/resources/starter-projects/AfricaFull.a3p"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

"$VALIDATOR" --dump-json "$TARGET_SCENARIO_ID" >"$tmp_root/target-scenario.json" 2>"$tmp_root/target-scenario.err"
status=$?
assert_success "$status" "validator dumps the Select Project tab-click scenario"
python3 - "$tmp_root/target-scenario.json" "$TARGET_DISPLAY_NAME" "$TARGET_REPOSITORY_PATH" <<'PY'
import json
import sys

scenario = json.load(open(sys.argv[1], encoding="utf-8"))
expected_display_name = sys.argv[2]
expected_repository_path = sys.argv[3]
target = scenario.get("targetStarter")
if not isinstance(target, dict):
    raise AssertionError("scenario must declare targetStarter metadata")
if target.get("displayName") != expected_display_name:
    raise AssertionError(
        f"targetStarter.displayName must be {expected_display_name!r}, got {target.get('displayName')!r}"
    )
if target.get("repositoryPath") != expected_repository_path:
    raise AssertionError(
        "targetStarter.repositoryPath must name the committed AfricaFull.a3p starter, got "
        f"{target.get('repositoryPath')!r}"
    )
if target["repositoryPath"].startswith("/"):
    raise AssertionError("targetStarter.repositoryPath must be repository-relative")
PY
assert_success "$?" "Select Project scenario declares Africa Full target starter metadata"

"$VALIDATOR" --dump-json "$POST_OPEN_SCENARIO_ID" >"$tmp_root/post-open-scenario.json" 2>"$tmp_root/post-open-scenario.err"
status=$?
assert_success "$status" "validator dumps the post-project-open scenario"
python3 - "$tmp_root/post-open-scenario.json" "$TARGET_DISPLAY_NAME" "$TARGET_REPOSITORY_PATH" <<'PY'
import json
import sys

scenario = json.load(open(sys.argv[1], encoding="utf-8"))
expected_display_name = sys.argv[2]
expected_repository_path = sys.argv[3]
target = scenario.get("targetStarter")
if not isinstance(target, dict):
    raise AssertionError("post-project-open scenario must declare targetStarter metadata")
if target.get("displayName") != expected_display_name:
    raise AssertionError(
        f"targetStarter.displayName must be {expected_display_name!r}, got {target.get('displayName')!r}"
    )
if target.get("repositoryPath") != expected_repository_path:
    raise AssertionError(
        "targetStarter.repositoryPath must name the committed AfricaFull.a3p starter, got "
        f"{target.get('repositoryPath')!r}"
    )
PY
assert_success "$?" "Post-project-open scenario declares Africa Full target starter metadata"

python3 - "$tmp_root/target-scenario.json" "$TARGET_REPOSITORY_PATH" <<'PY'
import json
import sys
from pathlib import Path

scenario = json.load(open(sys.argv[1], encoding="utf-8"))
repository_path = Path(sys.argv[2])
required = "\n".join(scenario["evidence"]["required"])
expected_outcomes = "\n".join(scenario["expectedOutcomes"])
for text in (required, expected_outcomes):
    if "Africa Full" not in text:
        raise AssertionError("scenario evidence contract must name Africa Full")
    if str(repository_path) not in text:
        raise AssertionError("scenario evidence contract must record the committed AfricaFull.a3p path")
PY
assert_success "$?" "Select Project scenario evidence contract names the target and committed repository path"

assert_contains "$RUNNER" 'TARGET_STARTER_DISPLAY_NAME' "runner passes target starter display name to the AT-SPI probe"
assert_contains "$RUNNER" 'TARGET_STARTER_REPO_PATH' "runner passes target starter repository path to the AT-SPI probe"

missing_target_dir="$tmp_root/missing-target"
mkdir -p "$missing_target_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$missing_target_dir"/
python3 - "$missing_target_dir/post-project-open-window-state.yaml" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
lines = path.read_text(encoding="utf-8").splitlines()
filtered = []
skip = False
for line in lines:
    if line.startswith("targetStarter:"):
        skip = True
        continue
    if skip and line.startswith("  "):
        continue
    skip = False
    filtered.append(line)
path.write_text("\n".join(filtered) + "\n", encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$missing_target_dir" "$VALIDATOR" >"$tmp_root/missing-target.out" 2>"$tmp_root/missing-target.err"
status=$?
assert_failure "$status" "validator rejects the post-project-open scenario without targetStarter"
assert_contains "$tmp_root/missing-target.err" 'targetStarter.*required|missing.*targetStarter' "missing targetStarter error names the required metadata"

wrong_target_dir="$tmp_root/wrong-target"
mkdir -p "$wrong_target_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$wrong_target_dir"/
python3 - "$wrong_target_dir/select-project-tab-click-exec.yaml" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
if "targetStarter:" not in text:
    text = text.replace(
        "automation:\n",
        "targetStarter:\n"
        "  displayName: Africa Full\n"
        "  repositoryPath: /tmp/AfricaFull.a3p\n"
        "automation:\n",
        1,
    )
else:
    lines = []
    for line in text.splitlines():
        if line.strip().startswith("repositoryPath:"):
            lines.append("  repositoryPath: /tmp/AfricaFull.a3p")
        else:
            lines.append(line)
    text = "\n".join(lines) + "\n"
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$wrong_target_dir" "$VALIDATOR" >"$tmp_root/wrong-target.out" 2>"$tmp_root/wrong-target.err"
status=$?
assert_failure "$status" "validator rejects absolute targetStarter repository paths"
assert_contains "$tmp_root/wrong-target.err" 'targetStarter\.repositoryPath.*repository-relative|targetStarter\.repositoryPath.*absolute' "absolute target repository path error is actionable"

drift_target_dir="$tmp_root/drift-target"
mkdir -p "$drift_target_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$drift_target_dir"/
python3 - "$drift_target_dir/select-project-tab-click-exec.yaml" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
if "targetStarter:" not in text:
    text = text.replace(
        "automation:\n",
        "targetStarter:\n"
        "  displayName: Africa Full\n"
        "  repositoryPath: core/resources/src/application/resources/starter-projects/Wrong.a3p\n"
        "automation:\n",
        1,
    )
else:
    lines = []
    for line in text.splitlines():
        if line.strip().startswith("repositoryPath:"):
            lines.append("  repositoryPath: core/resources/src/application/resources/starter-projects/Wrong.a3p")
        else:
            lines.append(line)
    text = "\n".join(lines) + "\n"
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$drift_target_dir" "$VALIDATOR" >"$tmp_root/drift-target.out" 2>"$tmp_root/drift-target.err"
status=$?
assert_failure "$status" "validator rejects Select Project targetStarter path drift"
assert_contains "$tmp_root/drift-target.err" 'AfricaFull\.a3p|targetStarter\.repositoryPath' "target path drift error names AfricaFull.a3p"

finish
