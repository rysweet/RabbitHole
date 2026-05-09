#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCENARIO="$BASE_DIR/scenarios/select-project-tab-click-exec.yaml"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

assert_contains "$SCENARIO" '^id: alice-desktop-select-project-tab-click-exec$' \
  "completion proof scenario keeps the focused Select Project id"
assert_contains "$SCENARIO" '^workflow: select-project-tab-click-smoke$' \
  "completion proof scenario keeps the focused Select Project workflow"
assert_contains "$SCENARIO" '^  displayName: Africa Full$' \
  "completion proof scenario targets Africa Full by display name"
assert_contains "$SCENARIO" '^  repositoryPath: core/resources/src/application/resources/starter-projects/AfricaFull\.a3p$' \
  "completion proof scenario targets the committed Africa Full starter path"
python3 - "$SCENARIO" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text(encoding="utf-8")
if "core/resources/src/application/resources/starter-projects/" not in text:
    raise AssertionError("scenario must name the committed starter-projects path")
for forbidden in ("Wonderland.a3p", "Bunny.a3p", "LookingGlass.a3p"):
    if forbidden in text:
        raise AssertionError(f"scenario must not introduce alternate starter target {forbidden}")
PY
assert_success "$?" "completion proof scenario keeps Africa Full as the single committed starter target"

assert_contains "$RUNNER" 'selectProjectEvidenceStatus' \
  "runner status publisher exposes the narrow Select Project evidenceStatus"
assert_contains "$RUNNER" 'selectProjectTargetDisplayName' \
  "runner status publisher exposes the target starter display name"
assert_contains "$RUNNER" 'selectProjectTargetRepositoryPath' \
  "runner status publisher exposes the target starter repository path"
assert_contains "$RUNNER" 'selectProjectOpenedStarterDisplayName' \
  "runner status publisher exposes the opened starter display name only after proof"
assert_contains "$RUNNER" 'selectProjectOpenedStarterRepositoryPath' \
  "runner status publisher exposes the opened starter path only after proof"
assert_contains "$RUNNER" 'selectProjectProjectOpenObserved' \
  "runner status publisher exposes the target project-open observation"
assert_contains "$RUNNER" 'selectProjectTargetSelectionObserved' \
  "runner status publisher exposes canonical targetSelectionObserved proof"
assert_contains "$RUNNER" 'selectProjectOpenAttempted' \
  "runner status publisher exposes canonical openAttempted proof"
assert_contains "$RUNNER" 'selectProjectNextBlocker' \
  "runner status publisher exposes exactly one next blocker when opening is not proven"
assert_contains "$RUNNER" 'selectProjectWindowContext' \
  "runner status publisher exposes Select Project window context"
assert_contains "$RUNNER" 'selectProjectAliceJavaPid' \
  "runner status publisher exposes the Alice Java/window PID used for Select Project proof"
assert_contains "$RUNNER" 'selectProjectStartersTabSafety' \
  "runner status publisher exposes the Starters-tab safety state"
assert_contains "$RUNNER" 'targetSelectionObserved' \
  "runner reads canonical targetSelectionObserved from tab-click observation"
assert_contains "$RUNNER" 'openAttempted' \
  "runner reads canonical openAttempted from tab-click observation"
assert_contains "$RUNNER" 'selectProjectTargetMetadataEchoValid' \
  "runner gates promoted Select Project evidence on validated target metadata echo"
assert_not_contains "$RUNNER" 'selectProjectVisibleRendering|selectProjectGrading|selectProjectLessonExecution|selectProjectFullUiAutomation' \
  "runner status does not publish out-of-scope Select Project success claims"

finish
