#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-runner-contract.sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

evidence_dir="$tmp_root/evidence"
"$RUNNER" run alice-desktop-scene-creation --evidence-dir "$evidence_dir" >"$tmp_root/manual.out" 2>"$tmp_root/manual.err"
status=$?
assert_success "$status" "manual scenario run prepares evidence directory"

run_dir=$(single_child_dir "$evidence_dir/alice-desktop-scene-creation")
status=$?
assert_success "$status" "manual scenario run creates one evidence directory"
status_file="$run_dir/status.txt"
assert_file_exists "$status_file" "manual scenario run writes status.txt"
assert_contains "$status_file" '^scenario=alice-desktop-scene-creation$' "manual status records scenario id"
assert_contains "$status_file" '^automationMode=manual-evidence-required$' "manual status records automation mode"
assert_contains "$status_file" '^outcome=manual-evidence-required$' "manual status records that evidence still needs human execution"
assert_contains "$status_file" '^checklist=manual-evidence-checklist.txt$' "manual status points to generated checklist"

checklist="$run_dir/manual-evidence-checklist.txt"
assert_file_exists "$checklist" "manual scenario run writes checklist"
assert_contains "$checklist" '^Completion status$' "checklist includes completion status section"
assert_contains "$checklist" 'not complete until required evidence is attached' "checklist states manual run is not complete"
assert_contains "$checklist" 'review-notes\.txt' "checklist names the required manual acceptance review notes artifact"

path_evidence_dir="$tmp_root/path-evidence"
"$RUNNER" run "$BASE_DIR/scenarios/save-load.yaml" --evidence-dir "$path_evidence_dir" >"$tmp_root/path.out" 2>"$tmp_root/path.err"
status=$?
assert_success "$status" "runner accepts a scenario YAML path"
path_run_dir=$(single_child_dir "$path_evidence_dir/alice-desktop-save-load")
status=$?
assert_success "$status" "path-based run creates one evidence directory"
assert_file_exists "$path_run_dir/status.txt" "path-based run writes evidence under the scenario id"

outside_path="$tmp_root/outside.yaml"
printf 'id: alice-desktop-save-load\n' > "$outside_path"
set +e
"$RUNNER" run "$outside_path" --evidence-dir "$tmp_root/outside-evidence" >"$tmp_root/outside.out" 2>"$tmp_root/outside.err"
status=$?
set -e
assert_failure "$status" "runner rejects scenario paths outside the active catalog"
assert_contains "$tmp_root/outside.err" 'inside active scenario directory' "outside path error names catalog boundary"

bad_timeout_evidence="$tmp_root/bad-timeout-evidence"
set +e
"$RUNNER" run alice-desktop-scene-creation --evidence-dir "$bad_timeout_evidence" --timeout-seconds not-a-number >"$tmp_root/bad-timeout.out" 2>"$tmp_root/bad-timeout.err"
status=$?
set -e
assert_failure "$status" "runner rejects non-integer timeout values for every mode"
assert_contains "$tmp_root/bad-timeout.err" 'timeout.*positive integer|invalid timeout' "invalid timeout error is actionable"

set +e
"$RUNNER" run alice-desktop-not-a-scenario --evidence-dir "$tmp_root/unknown-evidence" >"$tmp_root/unknown.out" 2>"$tmp_root/unknown.err"
status=$?
set -e
assert_failure "$status" "runner rejects unknown scenario ids"
assert_contains "$tmp_root/unknown.err" 'not found|unknown scenario|alice-desktop-not-a-scenario' "unknown scenario error names requested id"

set +e
ALICE_QA_DISABLE_XVFB=1 "$RUNNER" run alice-desktop-launch --evidence-dir "$tmp_root/no-xvfb-evidence" >"$tmp_root/no-xvfb.out" 2>"$tmp_root/no-xvfb.err"
status=$?
set -e
assert_exit_code "$status" 2 "xvfb launch reports unsupported display setup without pretending success"
no_xvfb_run_dir=$(single_child_dir "$tmp_root/no-xvfb-evidence/alice-desktop-launch")
status=$?
assert_success "$status" "xvfb fallback creates one evidence directory"
observation="$no_xvfb_run_dir/controlled-display-pixel-observation.json"
assert_file_exists "$observation" "xvfb fallback writes controlled display pixel observation artifact"
assert_contains "$observation" '"status": "blocked"' "xvfb fallback records blocked status"
assert_contains "$observation" '"blocker": "x-server-unavailable"' "xvfb fallback names missing X server blocker"
assert_contains "$observation" '"missingExecutable": "Xvfb"' "xvfb fallback names exact missing executable"
assert_contains "$observation" '"pixelsObserved": false' "xvfb fallback does not claim pixel observation"
assert_contains "$observation" '"claim": "no-visible-pixel-proof"' "xvfb fallback avoids visible rendering claims"
assert_contains "$observation" '"lifecyclePoint": "before-x-server-start"' "xvfb fallback records the lifecycle point before window probing"
assert_contains "$observation" '"windowInventoryFile": "x-window-inventory.json"' "xvfb fallback points to window inventory artifact"
assert_contains "$observation" '"windowInventoryStatus": "not-attempted"' "xvfb fallback records window inventory was not attempted"
window_inventory="$no_xvfb_run_dir/x-window-inventory.json"
assert_file_exists "$window_inventory" "xvfb fallback writes a window inventory artifact"
assert_contains "$window_inventory" '"status": "not-attempted"' "window inventory unsupported case is explicit"
assert_contains "$window_inventory" '"blocker": "x-server-unavailable"' "window inventory unsupported case names missing X server"
assert_contains "$window_inventory" '"lifecyclePoint": "before-x-server-start"' "window inventory unsupported case records lifecycle point"
assert_contains "$window_inventory" '"windows": \[\]' "window inventory unsupported case does not invent windows"
assert_contains "$tmp_root/no-xvfb.err" 'Xvfb is not available' "xvfb fallback stderr names missing Xvfb"

finish
