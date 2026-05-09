#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
set -uo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 2
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd) || exit 2

REQUIRED_TOTAL=5
required_covered=0
required_gap_categories=0
optional_gaps=0
evidence_lines=()
gap_lines=()

has_literal_marker() {
  local path=$1
  local marker=$2

  [ -f "$REPO_ROOT/$path" ] && grep -Fq -- "$marker" "$REPO_ROOT/$path"
}

append_missing_marker_gap() {
  local category=$1
  local path=$2
  local marker=$3

  if [ -f "$REPO_ROOT/$path" ]; then
    gap_lines+=("gap:${category}=missing_required path=${path} marker=${marker} reason=marker_missing")
  else
    gap_lines+=("gap:${category}=missing_required path=${path} reason=file_missing")
  fi
}

record_required_evidence() {
  local category=$1
  local evidence_path=$2
  local evidence_marker=$3
  local missing=0
  local path marker
  shift 3

  if [ "$#" -eq 0 ] || [ $(( $# % 2 )) -ne 0 ]; then
    printf 'status:silver_thread=blocked required=%s covered=0 gaps=%s optional_gaps=1\n' "$REQUIRED_TOTAL" "$REQUIRED_TOTAL"
    printf 'gap:%s=missing_required reason=internal_contract_error\n' "$category"
    print_claim_boundaries
    exit 2
  fi

  while [ "$#" -gt 0 ]; do
    path=$1
    marker=$2
    shift 2
    if ! has_literal_marker "$path" "$marker"; then
      append_missing_marker_gap "$category" "$path" "$marker"
      missing=1
    fi
  done

  if [ "$missing" -eq 0 ]; then
    evidence_lines+=("evidence:${category}=covered_bounded path=${evidence_path} marker=${evidence_marker}")
    required_covered=$((required_covered + 1))
  else
    required_gap_categories=$((required_gap_categories + 1))
  fi
}

record_optional_save_reopen() {
  local path="core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java"
  local missing=0
  local marker

  for marker in \
    "Robot File menu Save activation joined to dialog/write/readback evidence" \
    "robot-save-menu-dialog-write-readback-proof.json" \
    "robotSaveMenuRoundTripMarker" \
    "IoUtilities.readProject"; do
    if ! has_literal_marker "$path" "$marker"; then
      missing=1
    fi
  done

  if [ "$missing" -eq 0 ]; then
    evidence_lines+=("evidence:save_reopen=covered_bounded path=${path} marker=robotSaveMenuRoundTripMarker")
  else
    gap_lines+=("gap:save_reopen=not_covered_optional reason=direct_save_reopen_seam_missing")
    optional_gaps=1
  fi
}

print_claim_boundaries() {
  printf 'claim-boundary:full_ui_automation=not_claimed\n'
  printf 'claim-boundary:visible_rendering_correctness=not_claimed\n'
  printf 'claim-boundary:full_world_execution_semantics=not_claimed\n'
  printf 'claim-boundary:grading=not_claimed\n'
  printf 'claim-boundary:creative_assessment=not_claimed\n'
  printf 'claim-boundary:full_desktop_save_completion=not_claimed\n'
  printf 'claim-boundary:new_scenario_workflow=not_added\n'
}

if [ ! -e "$REPO_ROOT/.git" ] || [ ! -d "$REPO_ROOT/qa/outside-in/alice-desktop/tests" ]; then
  printf 'status:silver_thread=blocked required=%s covered=0 gaps=%s optional_gaps=1\n' "$REQUIRED_TOTAL" "$REQUIRED_TOTAL"
  printf 'gap:repository_layout=missing_required path=qa/outside-in/alice-desktop/tests reason=repository_root_not_found\n'
  print_claim_boundaries
  exit 2
fi

if [ "$#" -ne 0 ]; then
  printf 'status:silver_thread=blocked required=%s covered=0 gaps=%s optional_gaps=1\n' "$REQUIRED_TOTAL" "$REQUIRED_TOTAL"
  printf 'gap:invocation=missing_required reason=arguments_not_supported\n'
  print_claim_boundaries
  exit 2
fi

record_required_evidence \
  "launch" \
  "qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml" \
  "xvfb-real-alice" \
  "qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml" \
  "xvfb-real-alice" \
  "qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml" \
  "exec:exec@alice-ide-atk" \
  "qa/outside-in/alice-desktop/runners/run-scenario.sh" \
  "xvfb-real-alice"

record_required_evidence \
  "starter_world_or_program_change" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "edited-project.a3p" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "placed-project.a3p" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "edited-project.a3p" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "IoUtilities.readProject"

record_required_evidence \
  "object_placement" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "scene.diff.json" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "EatmePlaceObject.run" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "placed-project.a3p" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "placement.json" \
  "core/ide/src/test/java/org/alice/tools/EatmeEditProcedureTest.java" \
  "scene.diff.json"

record_required_evidence \
  "procedure_edit" \
  "core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java" \
  "wave4-code-editor-action-proof" \
  "core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java" \
  "wave4-code-editor-action-proof" \
  "core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java" \
  "scene.eatmeFirstLesson" \
  "core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java" \
  "append-comment" \
  "core/ide/src/test/java/org/alice/tools/FirstLessonCodeEditorActionProofTest.java" \
  "first-lesson-code-editor-action-proof.json"

record_required_evidence \
  "run_window_or_render_affordance" \
  "qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml" \
  "visible-rendering-pixel-sampling-blocker.json" \
  "qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml" \
  "worldCanvasPixelTarget" \
  "qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml" \
  "visible-rendering-pixel-sampling-blocker.json" \
  "qa/outside-in/alice-desktop/runners/run-scenario.sh" \
  "worldCanvasPixelTarget" \
  "qa/outside-in/alice-desktop/runners/run-scenario.sh" \
  "VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER"

record_optional_save_reopen

if [ "$required_gap_categories" -eq 0 ]; then
  printf 'status:silver_thread=covered_bounded required=%s covered=%s gaps=0 optional_gaps=%s\n' \
    "$REQUIRED_TOTAL" "$required_covered" "$optional_gaps"
else
  printf 'status:silver_thread=blocked required=%s covered=%s gaps=%s optional_gaps=%s\n' \
    "$REQUIRED_TOTAL" "$required_covered" "$required_gap_categories" "$optional_gaps"
fi

for line in "${evidence_lines[@]}"; do
  printf '%s\n' "$line"
done

for line in "${gap_lines[@]}"; do
  printf '%s\n' "$line"
done

print_claim_boundaries

if [ "$required_gap_categories" -eq 0 ]; then
  exit 0
fi

exit 1
