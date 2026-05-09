#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)

failures=0

pass() {
  printf 'ok - %s\n' "$1"
}

fail() {
  printf 'not ok - %s\n' "$1" >&2
  failures=$((failures + 1))
}

assert_exit_code() {
  local actual=$1
  local expected=$2
  local label=$3

  if [ "$actual" -eq "$expected" ]; then
    pass "$label"
  else
    fail "$label (expected exit $expected, got $actual)"
  fi
}

assert_line() {
  local path=$1
  local expected=$2
  local label=$3

  if [ -f "$path" ] && grep -Fqx -- "$expected" "$path"; then
    pass "$label"
  else
    fail "$label (line not found: $expected)"
  fi
}

assert_contains_literal() {
  local path=$1
  local expected=$2
  local label=$3

  if [ -f "$path" ] && grep -Fq -- "$expected" "$path"; then
    pass "$label"
  else
    fail "$label (literal not found: $expected)"
  fi
}

assert_not_contains_literal() {
  local path=$1
  local forbidden=$2
  local label=$3

  if [ -f "$path" ] && ! grep -Fq -- "$forbidden" "$path"; then
    pass "$label"
  else
    fail "$label (unexpected literal found: $forbidden)"
  fi
}

finish() {
  if [ "$failures" -eq 0 ]; then
    exit 0
  fi
  printf '%s assertion(s) failed\n' "$failures" >&2
  exit 1
}

run_report_under_test() {
  printf 'status:silver_thread=blocked required=5 covered=0 gaps=5 optional_gaps=1\n'
  printf 'gap:launch=missing_required path=qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml marker=xvfb-real-alice reason=implementation_pending\n'
  printf 'gap:starter_world_or_program_change=missing_required path=core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java marker=edited-project.a3p reason=implementation_pending\n'
  printf 'gap:object_placement=missing_required path=core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java marker=scene.diff.json reason=implementation_pending\n'
  printf 'gap:procedure_edit=missing_required path=core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java marker=wave4-code-editor-action-proof reason=implementation_pending\n'
  printf 'gap:run_window_or_render_affordance=missing_required path=qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml marker=visible-rendering-pixel-sampling-blocker.json reason=implementation_pending\n'
  printf 'gap:save_reopen=not_covered_optional reason=implementation_pending\n'
  printf 'claim-boundary:full_ui_automation=not_claimed\n'
  printf 'claim-boundary:visible_rendering_correctness=not_claimed\n'
  printf 'claim-boundary:full_world_execution_semantics=not_claimed\n'
  printf 'claim-boundary:grading=not_claimed\n'
  printf 'claim-boundary:creative_assessment=not_claimed\n'
  printf 'claim-boundary:full_desktop_save_completion=not_claimed\n'
  printf 'claim-boundary:new_scenario_workflow=not_added\n'
  return 1
}

tmp_dir="$SCRIPT_DIR/.test-scratch/silver-thread-status-report-$$"
mkdir -p "$tmp_dir" || exit 2
trap 'rm -rf "$tmp_dir"' EXIT

report_out="$tmp_dir/report.out"
report_err="$tmp_dir/report.err"

(
  cd "$REPO_ROOT" || exit 2
  run_report_under_test
) >"$report_out" 2>"$report_err"
status=$?

assert_exit_code "$status" 0 "silver-thread report exits successfully when all required evidence is covered"
assert_line "$report_out" "status:silver_thread=covered_bounded required=5 covered=5 gaps=0 optional_gaps=0" "status line reports bounded coverage with no required gaps"

assert_line "$report_out" "evidence:launch=covered_bounded path=qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml marker=xvfb-real-alice" "launch evidence is reported from the executable Alice desktop QA launch seam"
assert_line "$report_out" "evidence:starter_world_or_program_change=covered_bounded path=core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java marker=edited-project.a3p" "starter world/program change evidence is reported from the placed-to-edited project seam"
assert_line "$report_out" "evidence:object_placement=covered_bounded path=core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java marker=scene.diff.json" "object placement evidence is reported from the deterministic placement seam"
assert_line "$report_out" "evidence:procedure_edit=covered_bounded path=core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java marker=wave4-code-editor-action-proof" "procedure edit evidence is reported from the first-lesson code-editor action proof"
assert_line "$report_out" "evidence:run_window_or_render_affordance=covered_bounded path=qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml marker=visible-rendering-pixel-sampling-blocker.json" "run-window/render-affordance evidence is reported without claiming visible rendering correctness"
assert_line "$report_out" "evidence:save_reopen=covered_bounded path=core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java marker=robotSaveMenuRoundTripMarker" "bounded Save/reopen evidence is reported only from the direct Robot write/readback seam"

assert_line "$report_out" "claim-boundary:full_ui_automation=not_claimed" "report explicitly does not claim full UI automation"
assert_line "$report_out" "claim-boundary:visible_rendering_correctness=not_claimed" "report explicitly does not claim visible rendering correctness"
assert_line "$report_out" "claim-boundary:full_world_execution_semantics=not_claimed" "report explicitly does not claim full world execution semantics"
assert_line "$report_out" "claim-boundary:grading=not_claimed" "report explicitly does not claim grading"
assert_line "$report_out" "claim-boundary:creative_assessment=not_claimed" "report explicitly does not claim creative assessment"
assert_line "$report_out" "claim-boundary:full_desktop_save_completion=not_claimed" "report explicitly does not claim full desktop Save completion"
assert_line "$report_out" "claim-boundary:new_scenario_workflow=not_added" "report confirms no new scenario workflow is added"

if grep -Fq 'status:silver_thread=blocked ' "$report_out"; then
  assert_contains_literal "$report_out" "gap:launch=missing_required" "blocked report lists missing launch evidence"
  assert_contains_literal "$report_out" "gap:starter_world_or_program_change=missing_required" "blocked report lists missing starter world/program change evidence"
  assert_contains_literal "$report_out" "gap:object_placement=missing_required" "blocked report lists missing object placement evidence"
  assert_contains_literal "$report_out" "gap:procedure_edit=missing_required" "blocked report lists missing procedure edit evidence"
  assert_contains_literal "$report_out" "gap:run_window_or_render_affordance=missing_required" "blocked report lists missing run-window/render-affordance evidence"
fi

assert_not_contains_literal "$report_out" "full UI automation succeeded" "report does not overclaim full UI automation"
assert_not_contains_literal "$report_out" "visible rendering correctness passed" "report does not overclaim visible rendering correctness"
assert_not_contains_literal "$report_out" "full world execution semantics passed" "report does not overclaim full execution semantics"
assert_not_contains_literal "$report_out" "grading passed" "report does not overclaim grading"
assert_not_contains_literal "$report_out" "creative assessment passed" "report does not overclaim creative assessment"
assert_not_contains_literal "$report_out" "full desktop Save completion passed" "report does not overclaim full desktop Save completion"

finish
