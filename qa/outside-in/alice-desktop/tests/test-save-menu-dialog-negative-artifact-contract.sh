#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCENARIO_ID=alice-desktop-save-menu-dialog-write-proof
WORKFLOW=save-menu-dialog-write-proof
ARTIFACT=robot-save-menu-dialog-write-readback-proof.json
FIXTURE_DIR="$SCRIPT_DIR/fixtures/save-proof-evidence"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT
artifact_path="$tmp_root/$ARTIFACT"

assert_validation_failure_contains() {
  local artifact_path=$1
  local label=$2
  local expected_pattern=$3
  local started_at_epoch=${4:-4102444800}
  local expected_scenario=${5:-$SCENARIO_ID}
  local expected_workflow=${6:-$WORKFLOW}
  local expected_run_id=${7:-contract-run-1}
  local output_path="$tmp_root/$label.out"
  local status

  if "$RUNNER" validate-save-proof-evidence "$artifact_path" \
    --scenario "$expected_scenario" \
    --workflow "$expected_workflow" \
    --run-id "$expected_run_id" \
    --started-at-epoch "$started_at_epoch" \
    >"$output_path" 2>&1; then
    status=0
  else
    status=$?
  fi

  assert_failure "$status" "$label is rejected by fail-closed Save proof artifact validation"
  assert_contains "$output_path" "$expected_pattern" "$label rejection gives an explicit Save proof artifact diagnostic"
}

assert_fixture_rejected() {
  local fixture=$1
  local label=$2
  local expected_pattern=$3
  local started_at_epoch=${4:-4102444800}
  local expected_scenario=${5:-$SCENARIO_ID}
  local expected_workflow=${6:-$WORKFLOW}
  local expected_run_id=${7:-contract-run-1}

  rm -f "$artifact_path"
  cp "$FIXTURE_DIR/$fixture" "$artifact_path"
  assert_validation_failure_contains "$artifact_path" "$label" "$expected_pattern" "$started_at_epoch" "$expected_scenario" "$expected_workflow" "$expected_run_id"
}

assert_validation_failure_contains "$artifact_path" "missing-artifact" 'missing Save proof evidence artifact|No such file|not found'

wrong_name_artifact="$tmp_root/non-canonical-save-proof.json"
cp "$FIXTURE_DIR/missing-required-flags.json" "$wrong_name_artifact"
assert_validation_failure_contains "$wrong_name_artifact" "wrong-artifact-name" 'canonical filename|robot-save-menu-dialog-write-readback-proof\.json'

rm -f "$artifact_path"
if ln -s "$FIXTURE_DIR/missing-required-flags.json" "$artifact_path"; then
  assert_validation_failure_contains "$artifact_path" "symlink-artifact" 'must not be a symlink'
else
  fail "symlink-artifact setup creates a symlinked Save proof artifact"
fi

rm -f "$artifact_path"
printf '{\n' >"$artifact_path"
assert_validation_failure_contains "$artifact_path" "malformed-artifact" 'invalid Save proof evidence JSON'

printf '[]\n' >"$artifact_path"
assert_validation_failure_contains "$artifact_path" "non-object-artifact" 'Save proof evidence must be a JSON object'

assert_fixture_rejected "stale-generated-at.json" "stale-generated-at" 'stale|generatedAtUtc|mtime'
assert_fixture_rejected "missing-required-flags.json" "scenario-mismatch" 'scenario mismatch' 0 "alice-desktop-different-save-proof"
assert_fixture_rejected "missing-required-flags.json" "workflow-mismatch" 'workflow mismatch' 0 "$SCENARIO_ID" "different-save-workflow"
assert_fixture_rejected "missing-required-flags.json" "run-id-mismatch" 'runId mismatch' 0 "$SCENARIO_ID" "$WORKFLOW" "different-run-1"
assert_fixture_rejected "missing-required-flags.json" "missing-required-flags" 'missing|required|menu|dialog|control|write|readback'
assert_fixture_rejected "inconsistent-proven.json" "inconsistent-proven" 'inconsistent|fileWritten|markerPresent|outputSizeBytes'
assert_fixture_rejected "future-generated-at.json" "future-generated-at" 'future Save proof evidence|clock skew' 0
assert_fixture_rejected "blocked-known-kind.json" "blocked-known-kind" 'blocked.*dialog_not_observed|non-proven|status'
assert_fixture_rejected "blocked-unknown-kind.json" "blocked-unknown-kind" 'unknown.*blocker|unsupported.*blocker|not_allowed_blocker'

finish
