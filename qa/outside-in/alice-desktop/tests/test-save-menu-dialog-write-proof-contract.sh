#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
SCENARIO_ID=alice-desktop-save-menu-dialog-write-proof
WORKFLOW=save-menu-dialog-write-proof
ARTIFACT=robot-save-menu-dialog-write-readback-proof.json
FIXTURE_DIR="$SCRIPT_DIR/fixtures/save-proof-evidence"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

copy_catalog() {
  local destination=$1
  mkdir -p "$destination"
  cp "$BASE_DIR"/scenarios/*.yaml "$destination"/
}

assert_validation_failure_contains() {
  local artifact_path=$1
  local label=$2
  local expected_pattern=$3
  local stdout_path="$tmp_root/$label.out"
  local stderr_path="$tmp_root/$label.err"

  "$RUNNER" validate-save-proof-evidence "$artifact_path" \
    --scenario "$SCENARIO_ID" \
    --workflow "$WORKFLOW" \
    --run-id contract-run-1 \
    --started-at-epoch 4102444800 \
    >"$stdout_path" 2>"$stderr_path"
  local status=$?
  assert_failure "$status" "$label is rejected by fail-closed Save proof evidence validation"
  assert_contains "$stderr_path" "$expected_pattern" "$label rejection names the exact evidence problem"
}

python3 - "$BASE_DIR/scenarios/save-menu-dialog-write-proof.yaml" >"$tmp_root/scenario-shape.out" 2>"$tmp_root/scenario-shape.err" <<'PY'
import re
import sys
from pathlib import Path

scenario_path = Path(sys.argv[1])
text = scenario_path.read_text(encoding="utf-8")
errors = []

def require(condition, message):
    if not condition:
        errors.append(message)

require("workflow: save-menu-dialog-write-proof" in text, "Save proof scenario must keep the canonical workflow")
require("timeoutSeconds:" not in text, "Save proof scenario must not declare automation.timeoutSeconds")
require("-Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest" in text, "Save proof scenario must target the rendered Robot proof test")
require("robot-save-menu-dialog-write-readback-proof.json" in text, "Save proof scenario must require the canonical artifact")
require("readyWaitSeconds:" in text, "Save proof scenario still records readiness metadata")
if re.search(r"\btimeout\s+--foreground\b|\bshell timeout\b", text):
    errors.append("Save proof scenario text must not document shell timeout wrapping")

if errors:
    raise AssertionError("\n".join(errors))
PY
status=$?
assert_success "$status" "Save proof scenario is wired without workflow-level timeout"

python3 - "$BASE_DIR/../../../core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java" >"$tmp_root/java-timeout.out" 2>"$tmp_root/java-timeout.err" <<'PY'
import sys
from pathlib import Path

source_path = Path(sys.argv[1]).resolve()
text = source_path.read_text(encoding="utf-8")
errors = []

if "@Test(timeout" in text:
    errors.append("Robot Save proof test must not rely on a JUnit method timeout")
if "robot-save-menu-dialog-write-readback-proof.json" not in text:
    errors.append("Robot Save proof test must emit the canonical artifact")
if '\\"status\\": \\"blocked\\"' not in text:
    errors.append("Robot Save proof test must write an executable blocked artifact on unmet preconditions")
if '\\"status\\": \\"proven\\"' not in text:
    errors.append("Robot Save proof test must be able to write a proven artifact only for the single rendered path")
if "Robot Save proof blocked" not in text or "fail(" not in text:
    errors.append("Robot Save proof test must fail closed after writing a blocked artifact")

if errors:
    raise AssertionError("\n".join(errors))
PY
status=$?
assert_success "$status" "Robot Save proof Java test has no method timeout and retains proven/blocked artifact contract"

timeout_catalog="$tmp_root/timeout-catalog"
copy_catalog "$timeout_catalog"
python3 - "$timeout_catalog/save-menu-dialog-write-proof.yaml" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
if "  timeoutSeconds:" not in text:
    text = re.sub(r"(  readyWaitSeconds:\s*[0-9]+\n)", "  timeoutSeconds: 600\n\\1", text, count=1)
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$timeout_catalog" "$VALIDATOR" >"$tmp_root/timeout-catalog.out" 2>"$tmp_root/timeout-catalog.err"
status=$?
assert_failure "$status" "validator rejects Save proof scenarios that reintroduce automation.timeoutSeconds"
assert_contains "$tmp_root/timeout-catalog.err" 'save-menu-dialog-write-proof.*timeoutSeconds|timeoutSeconds.*save-menu-dialog-write-proof' "timeout rejection names the Save proof workflow"

prepare_evidence="$tmp_root/prepare-evidence"
"$RUNNER" run "$SCENARIO_ID" --prepare-only --evidence-dir "$prepare_evidence" >"$tmp_root/prepare.out" 2>"$tmp_root/prepare.err"
status=$?
assert_success "$status" "prepare-only Save proof records no-timeout runner wiring"
prepare_run_dir=$(single_child_dir "$prepare_evidence/$SCENARIO_ID")
prepare_status=$?
assert_success "$prepare_status" "prepare-only Save proof creates one evidence directory"
if [ "$prepare_status" -eq 0 ]; then
  assert_file_exists "$prepare_run_dir/status.txt" "prepare-only Save proof writes status.txt"
  assert_not_contains "$prepare_run_dir/status.txt" '^timeoutSeconds=' "prepare-only Save proof status omits workflow timeout"
  assert_contains "$prepare_run_dir/status.txt" '^timeoutPolicy=none$|^workflowTimeout=disabled$' "prepare-only Save proof status records no-timeout policy"
fi

fake_bin="$tmp_root/bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/timeout" <<'SH'
#!/usr/bin/env bash
printf 'timeout invoked for Save proof: %s\n' "$*" >> "${ALICE_QA_TIMEOUT_LOG:?}"
exit 77
SH
cat > "$fake_bin/mvn" <<'SH'
#!/usr/bin/env bash
set -eu
scenario=
run_id=
evidence_path=
for arg in "$@"; do
  case "$arg" in
    -Dorg.alice.eatme.saveProof.scenario=*) scenario=${arg#*=} ;;
    -Dorg.alice.eatme.saveProof.runId=*) run_id=${arg#*=} ;;
    -Dorg.alice.eatme.saveProof.evidencePath=*) evidence_path=${arg#*=} ;;
  esac
done
if [ "$scenario" != "alice-desktop-save-menu-dialog-write-proof" ] || [ -z "$run_id" ] || [ -z "$evidence_path" ]; then
  printf 'missing Save proof Maven properties\n' >&2
  printf 'argv=%s\n' "$*" >&2
  exit 64
fi
mkdir -p "$(dirname -- "$evidence_path")/projects"
output_path="$(dirname -- "$evidence_path")/projects/robot-save-menu-proof.a3p"
printf 'robotSaveMenuRoundTripMarker\n' > "$output_path"
python3 - "$evidence_path" "$scenario" "$run_id" "$output_path" <<'PY'
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

evidence_path = Path(sys.argv[1])
scenario = sys.argv[2]
run_id = sys.argv[3]
output_path = Path(sys.argv[4])
payload = {
    "schemaVersion": "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1",
    "scenario": scenario,
    "workflow": "save-menu-dialog-write-proof",
    "runId": run_id,
    "generatedAtUtc": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    "status": "proven",
    "claim": "AWT Robot opened File, clicked the production Save menu item, controlled the rendered Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker",
    "menu": {
        "fileMenuOpened": True,
        "saveMenuItemInvoked": True,
        "saveActionIdentityMatched": True,
    },
    "dialog": {
        "saveDialogObserved": True,
        "dialogType": "Swing JFileChooser",
        "dialogShowing": True,
        "ambiguousChooserDiscovery": False,
    },
    "control": {
        "selectedPathSet": True,
        "approvedSelection": True,
        "selectedPathMatchesExpected": True,
        "targetInsideProofRoot": True,
    },
    "write": {
        "fileWritten": True,
        "fileNonempty": True,
        "fileHasExpectedExtension": True,
        "outputPath": str(output_path),
        "outputSizeBytes": output_path.stat().st_size,
    },
    "readback": {
        "projectReadable": True,
        "marker": "robotSaveMenuRoundTripMarker",
        "markerPresent": True,
    },
    "blocker": None,
    "doesNotClaim": [
        "Save As coverage",
        "all Save variants",
        "full lesson completion",
        "visible rendering correctness",
        "grading correctness",
        "broad UI automation coverage",
        "native dialog coverage",
    ],
}
evidence_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
PY
printf 'fake save proof mvn completed\n'
printf 'argv=%s\n' "$*"
SH
chmod +x "$fake_bin/timeout" "$fake_bin/mvn"

enabled_evidence="$tmp_root/enabled-evidence"
timeout_log="$tmp_root/timeout.log"
: > "$timeout_log"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 ALICE_QA_TIMEOUT_LOG="$timeout_log" \
  "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$enabled_evidence" >"$tmp_root/enabled.out" 2>"$tmp_root/enabled.err"
status=$?
assert_success "$status" "enabled Save proof runner bypasses shell timeout and validates canonical evidence"
enabled_run_dir=$(single_child_dir "$enabled_evidence/$SCENARIO_ID")
enabled_status=$?
assert_success "$enabled_status" "enabled Save proof creates one evidence directory"
if [ "$enabled_status" -eq 0 ]; then
  assert_file_exists "$enabled_run_dir/command.log" "enabled Save proof writes command.log"
  assert_file_exists "$enabled_run_dir/$ARTIFACT" "enabled Save proof writes canonical evidence artifact"
  assert_not_contains "$timeout_log" 'timeout invoked' "enabled Save proof does not invoke shell timeout"
  assert_contains "$enabled_run_dir/command.log" 'org\.alice\.eatme\.saveProof\.scenario=alice-desktop-save-menu-dialog-write-proof' "Maven command receives Save proof scenario property"
  assert_contains "$enabled_run_dir/command.log" 'org\.alice\.eatme\.saveProof\.runId=' "Maven command receives strict runId property"
  assert_contains "$enabled_run_dir/command.log" 'org\.alice\.eatme\.saveProof\.evidencePath=' "Maven command receives canonical evidence path property"
  assert_not_contains "$enabled_run_dir/status.txt" '^timeoutSeconds=' "enabled Save proof status omits workflow timeout"
  assert_contains "$enabled_run_dir/status.txt" '^saveProofEvidence=robot-save-menu-dialog-write-readback-proof\.json$' "status links canonical Save proof evidence"
fi

valid_dir="$tmp_root/valid-evidence"
mkdir -p "$valid_dir/projects"
valid_output="$valid_dir/projects/robot-save-menu-proof.a3p"
printf 'robotSaveMenuRoundTripMarker\n' > "$valid_output"
valid_artifact="$valid_dir/$ARTIFACT"
python3 - "$valid_artifact" "$valid_output" <<'PY'
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

artifact = Path(sys.argv[1])
output = Path(sys.argv[2])
payload = {
    "schemaVersion": "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1",
    "scenario": "alice-desktop-save-menu-dialog-write-proof",
    "workflow": "save-menu-dialog-write-proof",
    "runId": "contract-run-1",
    "generatedAtUtc": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    "status": "proven",
    "claim": "AWT Robot opened File, clicked the production Save menu item, controlled the rendered Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker",
    "menu": {"fileMenuOpened": True, "saveMenuItemInvoked": True, "saveActionIdentityMatched": True},
    "dialog": {"saveDialogObserved": True, "dialogType": "Swing JFileChooser", "dialogShowing": True, "ambiguousChooserDiscovery": False},
    "control": {"selectedPathSet": True, "approvedSelection": True, "selectedPathMatchesExpected": True, "targetInsideProofRoot": True},
    "write": {"fileWritten": True, "fileNonempty": True, "fileHasExpectedExtension": True, "outputPath": str(output), "outputSizeBytes": output.stat().st_size},
    "readback": {"projectReadable": True, "marker": "robotSaveMenuRoundTripMarker", "markerPresent": True},
    "blocker": None,
    "doesNotClaim": [
        "Save As coverage",
        "all Save variants",
        "full lesson completion",
        "visible rendering correctness",
        "grading correctness",
        "broad UI automation coverage",
        "native dialog coverage",
    ],
}
artifact.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
PY
"$RUNNER" validate-save-proof-evidence "$valid_artifact" \
  --scenario "$SCENARIO_ID" \
  --workflow "$WORKFLOW" \
  --run-id contract-run-1 \
  --started-at-epoch 0 \
  >"$tmp_root/valid-evidence.out" 2>"$tmp_root/valid-evidence.err"
status=$?
assert_success "$status" "runner accepts complete, fresh, internally consistent proven Save proof evidence"

wrong_name_artifact="$tmp_root/wrong-save-proof-name.json"
cp "$valid_artifact" "$wrong_name_artifact"
assert_validation_failure_contains "$wrong_name_artifact" "wrong-evidence-name" 'canonical filename|robot-save-menu-dialog-write-readback-proof\.json'

symlink_artifact="$tmp_root/$ARTIFACT"
if ln -s "$valid_artifact" "$symlink_artifact" 2>/dev/null; then
  assert_validation_failure_contains "$symlink_artifact" "symlink-evidence" 'must not be a symlink'
fi

missing_dir="$tmp_root/missing-evidence-dir"
mkdir -p "$missing_dir"
assert_validation_failure_contains "$missing_dir/$ARTIFACT" "missing-evidence" 'missing.*robot-save-menu-dialog-write-readback-proof|No such file|not found'

stale_dir="$tmp_root/stale-evidence-dir"
mkdir -p "$stale_dir"
stale_artifact="$stale_dir/$ARTIFACT"
cp "$valid_artifact" "$stale_artifact"
python3 - "$stale_artifact" <<'PY'
import json
import sys
from pathlib import Path

path = Path(sys.argv[1])
payload = json.loads(path.read_text(encoding="utf-8"))
payload["generatedAtUtc"] = "2000-01-01T00:00:00Z"
path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
PY
assert_validation_failure_contains "$stale_artifact" "stale-evidence" 'stale|generatedAtUtc|mtime'

partial_dir="$tmp_root/partial-evidence-dir"
mkdir -p "$partial_dir"
cp "$FIXTURE_DIR/missing-required-flags.json" "$partial_dir/$ARTIFACT"
assert_validation_failure_contains "$partial_dir/$ARTIFACT" "partial-evidence" 'missing|required|menu|dialog|control|write|readback'

inconsistent_dir="$tmp_root/inconsistent-proven-dir"
mkdir -p "$inconsistent_dir"
cp "$FIXTURE_DIR/inconsistent-proven.json" "$inconsistent_dir/$ARTIFACT"
assert_validation_failure_contains "$inconsistent_dir/$ARTIFACT" "inconsistent-proven" 'inconsistent|fileWritten|markerPresent|outputSizeBytes'

blocked_known_dir="$tmp_root/blocked-known-kind-dir"
mkdir -p "$blocked_known_dir"
cp "$FIXTURE_DIR/blocked-known-kind.json" "$blocked_known_dir/$ARTIFACT"
assert_validation_failure_contains "$blocked_known_dir/$ARTIFACT" "blocked-known-kind" 'blocked.*dialog_not_observed|non-proven|status'

blocked_unknown_dir="$tmp_root/blocked-unknown-kind-dir"
mkdir -p "$blocked_unknown_dir"
cp "$FIXTURE_DIR/blocked-unknown-kind.json" "$blocked_unknown_dir/$ARTIFACT"
assert_validation_failure_contains "$blocked_unknown_dir/$ARTIFACT" "blocked-unknown-kind" 'unknown.*blocker|unsupported.*blocker|not_allowed_blocker'

finish
