#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
VALIDATOR="$SCRIPT_DIR/validate-scenarios.sh"
ROOT_DIRECTORY_PREP="$SCRIPT_DIR/prepare-root-directory.py"
LICENSE_ACCEPTANCE_PREP="$SCRIPT_DIR/prepare-license-acceptance.py"
LICENSE_DIALOG_PROBE="$SCRIPT_DIR/license-dialog-probe.py"
SELECT_PROJECT_PROBE="$SCRIPT_DIR/select-project-probe.py"
SWING_WIDGET_PROBE="$SCRIPT_DIR/swing-widget-probe.py"
TAB_CLICK_PROBE="$SCRIPT_DIR/tab-click-probe.py"
POST_PROJECT_OPEN_PROBE="$SCRIPT_DIR/post-project-open-probe.py"
POST_OPEN_RUNTIME_DISPLAY_PROBE="$SCRIPT_DIR/post-open-runtime-display-probe.py"
FIRST_LESSON_PROCEDURE_TARGET_PROBE="$SCRIPT_DIR/first-lesson-procedure-target-probe.py"
POST_OPEN_RUNTIME_DISPLAY_SCENARIO=alice-desktop-post-open-runtime-display-accessibility-evidence
POST_OPEN_RUNTIME_DISPLAY_ARTIFACT=post-open-runtime-display-accessibility-evidence.json
VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER=visible-rendering-pixel-target-blocker.json
VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER=visible-rendering-pixel-sampling-blocker.json
VISIBLE_RENDERING_PIXEL_OBSERVATION=visible-rendering-pixel-observation.json
WORLD_CANVAS_PIXEL_SAMPLER="$SCRIPT_DIR/world-canvas-pixel-sampler.py"
FIRST_LESSON_PROCEDURE_TARGET_SCENARIO=alice-desktop-first-lesson-live-procedure-target-observation
FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT=first-lesson-live-procedure-target-observation.json
FIRST_LESSON_PROCEDURE_SELECTOR=scene.eatmeFirstLesson
RUN_WINDOW_CONTRACT_SCENARIO=alice-desktop-run-window-contract
RUN_WINDOW_CONTRACT_ARTIFACT=run-window-created.json
LEARNER_WORLD_BOUNDARY_ARTIFACT="$BASE_DIR/contracts/learner-world-assessment-boundary.json"

usage() {
  cat <<'EOF'
usage:
  run-scenario.sh list
  run-scenario.sh validate
  run-scenario.sh run <scenario-id-or-path> [--evidence-dir <dir>] [--timeout-seconds <seconds>] [--prepare-only]

Environment:
  ALICE_QA_SCENARIO_DIR  Override the directory containing scenario YAML files.
  ALICE_QA_DISPLAY       Reuse a specific X display, for example :99.
  ALICE_QA_SCREEN        Xvfb screen geometry, default 1280x900x24.
  ALICE_QA_READY_WAIT_SECONDS
                          Override GUI readiness wait before screenshot capture.
  ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1
                           Prepare isolated first-run License Agreement acceptance
                           state for this controlled QA launch only.
  ALICE_QA_RUN_GATED_SMOKES=1
                           Execute gated command smoke scenarios.
                          Without it, gated smokes write gated-not-run evidence
                          and exit non-zero unless --prepare-only is requested.
  ALICE_QA_DISABLE_XVFB=1
                          Force the Xvfb-unavailable fallback for contract tests.
EOF
}

json_fields() {
  local scenario_json=$1
  shift
  SCENARIO_JSON="$scenario_json" python3 - "$@" <<'PY'
import json
import os
import sys

scenario = json.loads(os.environ["SCENARIO_JSON"])
for field in sys.argv[1:]:
    value = scenario
    for part in field.split("."):
        value = value[part]
    print(value)
PY
}

json_list_nul() {
  local scenario_json=$1
  local field=$2
  SCENARIO_JSON="$scenario_json" python3 - "$field" <<'PY'
import json
import os
import sys

value = json.loads(os.environ["SCENARIO_JSON"])
for part in sys.argv[1].split("."):
    value = value[part]
for item in value:
    sys.stdout.buffer.write(item.encode("utf-8") + b"\0")
PY
}

python_with_module() {
  local module=$1
  local candidate

  for candidate in "${ALICE_QA_PYTHON:-}" python3 /usr/bin/python3; do
    [ -n "$candidate" ] || continue
    if command -v "$candidate" >/dev/null 2>&1 &&
      "$candidate" - "$module" >/dev/null 2>&1 <<'PY'
import importlib
import sys

importlib.import_module(sys.argv[1])
PY
    then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  printf '%s\n' python3
}

target_starter_fields() {
  local scenario_json=$1
  SCENARIO_JSON="$scenario_json" python3 - <<'PY'
import json
import os

scenario = json.loads(os.environ["SCENARIO_JSON"])
target = scenario.get("targetStarter") or {}
print(target.get("displayName", ""))
print(target.get("repositoryPath", ""))
PY
}

validate_allowed_automation() {
  local cwd=$1
  shift

  if [ "$cwd" = alice-ide ] &&
    [ "$#" -eq 8 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dcheckstyle.skip ] &&
    [ "$5" = -DskipTests ] &&
    [ "$6" = compile ] &&
    [ "$7" = exec:java ] &&
    [ "$8" = -Dalice-ide ]; then
    return 0
  fi

  if [ "$cwd" = alice-ide ] &&
    [ "$#" -eq 7 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dcheckstyle.skip ] &&
    [ "$5" = -DskipTests ] &&
    [ "$6" = compile ] &&
    [ "$7" = exec:exec@alice-ide-atk ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = netbeans ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ast ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.lgna.project.virtualmachine.VirtualMachineHeadlessRuntimeEventTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = netbeans ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/story-api-migration ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = core/story-api-migration ] &&
    [ "$7" = -am ] &&
    [ "$8" = -Dtest=org.lgna.project.io.IoUtilitiesTest ] &&
    [ "$9" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = core/ide ] &&
    [ "$7" = -am ] &&
    { [ "$8" = -Dtest=org.alice.ide.ProjectSaveTargetPlanTest ] || [ "$8" = -Dtest=org.alice.ide.ProjectLoadFailureDispatchPlanTest ]; } &&
    [ "$9" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = core/ide ] &&
    [ "$7" = -am ] &&
    [ "$8" = -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest ] &&
    [ "$9" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.ide.uricontent.FileProjectLoaderTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.ide.SilverThreadLaunchBuildRunTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 1 ] &&
    [ "$1" = qa/outside-in/alice-desktop/runners/netbeans-package-smoke.sh ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 1 ] &&
    [ "$1" = qa/outside-in/alice-desktop/runners/package-install-smoke.sh ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 8 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -pl ] &&
    [ "$5" = netbeans ] &&
    [ "$6" = -am ] &&
    [ "$7" = -Dtest=org.alice.netbeans.Alice3ProjectTemplateWizardIteratorTest,org.alice.netbeans.palette.Alice3PaletteFactoryTest,org.alice.netbeans.palette.items.AliceComponentPaletteUtilitiesTest,org.alice.netbeans.palette.items.resources.PaletteBundleLocalizationTest,org.alice.netbeans.completion.Alice3CompletionItemTest ] &&
    [ "$8" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 7 ] &&
    [ "$1" = qa/outside-in/alice-desktop/runners/run-scenario.sh ] &&
    [ "$2" = run ] &&
    [ "$3" = alice-desktop-launch ] &&
    [ "$4" = --timeout-seconds ] &&
    [ "$5" = 30 ] &&
    [ "$6" = --evidence-dir ] &&
    [ "$7" = qa/outside-in/alice-desktop/evidence/future-ui-launch ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ast ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeCreatesMethodInvocation ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ast ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.tools.EatmeEditProcedureTest#editsSceneProcedureAndWritesEatmeProofArtifacts ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.tools.EatmeEditProcedureTest#chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/model-loading ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.lgna.story.resourceutilities.ModelExportTest ] &&
    [ "$7" = core/issue-reporting ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  printf '%s\n' 'automation.argv is restricted to the allowed Alice QA command set' >&2
  return 2
}

format_argv() {
  local IFS=' '
  printf '%s' "$*"
}
write_gated_timeout_policy() {
  local scenario_id=$1
  local run_timeout=$2

  case "$scenario_id" in
    alice-desktop-save-menu-dialog-write-proof|alice-desktop-project-io-smoke|"$RUN_WINDOW_CONTRACT_SCENARIO")
      printf 'timeoutPolicy=none\n'
      ;;
    *)
      printf 'timeoutSeconds=%s\n' "$run_timeout"
      ;;
  esac
}
validate_scenario_automation_cwd() {
  local scenario_json=$1
  local cwd
  cwd=$(json_fields "$scenario_json" "automation.cwd" 2>/dev/null) || return 0
  if [ -n "$cwd" ]; then
    resolve_automation_cwd "$cwd" > /dev/null
  fi
}
resolve_automation_cwd() {
  local cwd=$1
  python3 - "$REPO_ROOT" "$cwd" <<'PY'
import sys
from pathlib import Path

repo_root = Path(sys.argv[1]).resolve(strict=True)
cwd = sys.argv[2]
cwd_path = Path(cwd)

if cwd_path.is_absolute():
    print("automation.cwd must be repository-relative, not absolute", file=sys.stderr)
    sys.exit(2)
if any(part == ".." for part in cwd_path.parts):
    print("automation.cwd must not contain .. path traversal", file=sys.stderr)
    sys.exit(2)

try:
    resolved = (repo_root / cwd_path).resolve(strict=True)
except FileNotFoundError:
    print("automation.cwd must be an existing directory inside repository root", file=sys.stderr)
    sys.exit(2)

if not resolved.is_dir():
    print("automation.cwd must be an existing directory inside repository root", file=sys.stderr)
    sys.exit(2)

try:
    resolved.relative_to(repo_root)
except ValueError:
    print("automation.cwd must resolve inside repository root", file=sys.stderr)
    sys.exit(2)

print(resolved)
PY
}

resolve_scenario_id() {
  local request=$1
  local scenario_dir=${ALICE_QA_SCENARIO_DIR:-$BASE_DIR/scenarios}

  if [ -f "$request" ]; then
    python3 - "$request" "$scenario_dir" <<'PY'
import re
import sys
from pathlib import Path

scenario_path = Path(sys.argv[1]).resolve()
scenario_dir = Path(sys.argv[2]).resolve()

try:
    scenario_path.relative_to(scenario_dir)
except ValueError:
    print(
        f"scenario path must be inside active scenario directory: {scenario_dir}",
        file=sys.stderr,
    )
    sys.exit(2)

if scenario_path.parent != scenario_dir:
    print(
        f"scenario path must be directly inside active scenario directory: {scenario_dir}",
        file=sys.stderr,
    )
    sys.exit(2)

if scenario_path.suffix != ".yaml":
    print(f"scenario path must be a .yaml file: {scenario_path}", file=sys.stderr)
    sys.exit(2)

last_line_number = 0
for line_number, line in enumerate(scenario_path.read_text(encoding="utf-8").splitlines(), 1):
    last_line_number = line_number
    if not line.strip() or line.lstrip().startswith("#"):
        continue
    match = re.fullmatch(r"id:\s*['\"]?([^'\"]+)['\"]?", line.strip())
    if match:
        print(match.group(1))
        sys.exit(0)

print(f"{scenario_path}:{last_line_number}: missing top-level id field", file=sys.stderr)
sys.exit(1)
PY
  elif [[ "$request" == */* || "$request" == *.yaml ]]; then
    printf 'scenario path not found: %s\n' "$request" >&2
    return 2
  else
    printf '%s\n' "$request"
  fi
}

write_checklist() {
  local scenario_json=$1
  local run_dir=$2
  SCENARIO_JSON="$scenario_json" RUN_DIR="$run_dir" LEARNER_WORLD_BOUNDARY_ARTIFACT="$LEARNER_WORLD_BOUNDARY_ARTIFACT" python3 - <<'PY'
import json
import os
from pathlib import Path

scenario = json.loads(os.environ["SCENARIO_JSON"])
run_dir = Path(os.environ["RUN_DIR"])
boundary_path = Path(os.environ["LEARNER_WORLD_BOUNDARY_ARTIFACT"])
path = run_dir / "manual-evidence-checklist.txt"

def section(lines, title, values):
    lines.append("")
    lines.append(title)
    lines.append("-" * len(title))
    for index, value in enumerate(values, 1):
        lines.append(f"{index}. {value}")

def require_string(value, field):
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{boundary_path}: {field} must be a non-empty string")
    return value

def require_string_list(value, field):
    if not isinstance(value, list) or not value:
        raise ValueError(f"{boundary_path}: {field} must be a non-empty string list")
    for index, item in enumerate(value, 1):
        if not isinstance(item, str) or not item.strip():
            raise ValueError(f"{boundary_path}: {field}[{index}] must be a non-empty string")
    return value

def assessment_boundary_values():
    if scenario["id"] != "alice-desktop-instructor-student-setup":
        return []

    boundary = json.loads(boundary_path.read_text(encoding="utf-8"))
    selected_scenario = require_string(boundary.get("selectedScenario"), "selectedScenario")
    if selected_scenario != scenario["id"]:
        raise ValueError(
            f"{boundary_path}: selectedScenario must match {scenario['id']} for generated manual evidence"
        )
    if require_string(boundary.get("automationMode"), "automationMode") != "manual-evidence-required":
        raise ValueError(f"{boundary_path}: automationMode must be manual-evidence-required")

    supported_evidence = require_string_list(boundary.get("supportedEvidence"), "supportedEvidence")
    assessment_limits = require_string_list(boundary.get("assessmentLimits"), "assessmentLimits")
    next_boundary = require_string(boundary.get("nextBoundary"), "nextBoundary")
    manual_limitation_summary = require_string(
        boundary.get("manualLimitationSummary"),
        "manualLimitationSummary",
    )
    required_contract_topics = require_string_list(
        boundary.get("requiresReviewedAssessmentContractBefore"),
        "requiresReviewedAssessmentContractBefore",
    )
    blocker = boundary.get("blocker")
    if not isinstance(blocker, dict):
        raise ValueError(f"{boundary_path}: blocker must be a mapping")
    blocker_id = require_string(blocker.get("id"), "blocker.id")
    blocker_description = require_string(blocker.get("description"), "blocker.description")

    values = [
        "Manual evidence required.",
        f"Scope: {require_string(boundary.get('scope'), 'scope')}.",
    ]
    values.extend(f"Supported evidence: {item}." for item in supported_evidence)
    values.append(manual_limitation_summary)
    values.extend(f"Assessment limit: {item}." for item in assessment_limits)
    values.append(f"Next boundary: {next_boundary}.")
    values.extend(
        f"Manual/unsupported until reviewed contract: {item}."
        for item in required_contract_topics
    )
    values.append(f"Blocker: {blocker_id}.")
    values.append(blocker_description)
    return values

lines = [
    f"Scenario: {scenario['id']}",
    f"Title: {scenario['title']}",
    f"Workflow: {scenario['workflow']}",
    f"Automation mode: {scenario['automationMode']}",
]
section(lines, "Preconditions", scenario["preconditions"])
section(lines, "User actions", scenario["userActions"])
section(lines, "Expected outcomes", scenario["expectedOutcomes"])
section(lines, "Required evidence", scenario["evidence"]["required"])
section(lines, "Fallback notes", scenario["fallback"]["notes"])
assessment_values = assessment_boundary_values()
if assessment_values:
    section(lines, "Assessment boundary", assessment_values)
section(
    lines,
    "Completion status",
    [
        "Checklist generation is not complete until required evidence is attached.",
        "A human performs the workflow, adds artifacts to this run directory, and records review notes.",
        "Human reviewer accepts the scenario only after the required evidence matches the expected outcomes.",
    ],
)

path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(path)
PY
}

write_manual_status() {
  local run_dir=$1
  local checklist_path=$2
  local scenario_id=$3
  local automation_mode=$4
  local status_path="$run_dir/status.txt"

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    printf 'outcome=manual-evidence-required\n'
    printf 'checklist=%s\n' "$(basename "$checklist_path")"
  } > "$status_path"

  STATUS_PATH="$status_path" SCENARIO_ID="$scenario_id" LEARNER_WORLD_BOUNDARY_ARTIFACT="$LEARNER_WORLD_BOUNDARY_ARTIFACT" python3 - <<'PY'
import json
import os
from pathlib import Path

status_path = Path(os.environ["STATUS_PATH"])
scenario_id = os.environ["SCENARIO_ID"]
boundary_path = Path(os.environ["LEARNER_WORLD_BOUNDARY_ARTIFACT"])
boundary_scenario = "alice-desktop-instructor-student-setup"

if scenario_id != boundary_scenario:
    raise SystemExit(0)

boundary = json.loads(boundary_path.read_text(encoding="utf-8"))

def require_string(value, field):
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{boundary_path}: {field} must be a non-empty string")
    return value

def require_string_list(value, field):
    if not isinstance(value, list) or not value:
        raise ValueError(f"{boundary_path}: {field} must be a non-empty string list")
    for index, item in enumerate(value, 1):
        if not isinstance(item, str) or not item.strip():
            raise ValueError(f"{boundary_path}: {field}[{index}] must be a non-empty string")
    return value

selected_scenario = require_string(boundary.get("selectedScenario"), "selectedScenario")
if selected_scenario != scenario_id:
    raise ValueError(
        f"{boundary_path}: selectedScenario must match {scenario_id} for generated status evidence"
    )

if require_string(boundary.get("automationMode"), "automationMode") != "manual-evidence-required":
    raise ValueError(f"{boundary_path}: automationMode must be manual-evidence-required")

blocker = boundary.get("blocker")
if not isinstance(blocker, dict):
    raise ValueError(f"{boundary_path}: blocker must be a mapping")

status_lines = [
    f"assessmentBoundary={require_string(boundary.get('nextBoundary'), 'nextBoundary')}",
    "assessmentBoundaryMode=manual/unsupported",
    f"assessmentBoundaryScope={require_string(boundary.get('scope'), 'scope')}",
    f"assessmentLimitation={require_string(boundary.get('manualLimitationSummary'), 'manualLimitationSummary')}",
    f"assessmentLimits={'; '.join(require_string_list(boundary.get('assessmentLimits'), 'assessmentLimits'))}",
    "assessmentUnsupportedUntilReviewedContract="
    f"{'; '.join(require_string_list(boundary.get('requiresReviewedAssessmentContractBefore'), 'requiresReviewedAssessmentContractBefore'))}",
    f"assessmentBlocker={require_string(blocker.get('id'), 'blocker.id')}",
    f"assessmentBlockerDescription={require_string(blocker.get('description'), 'blocker.description')}",
]

with status_path.open("a", encoding="utf-8") as status_file:
    for line in status_lines:
        status_file.write(line + "\n")
PY
}

validate_positive_integer() {
  local value=$1
  local label=$2
  if [[ ! "$value" =~ ^[1-9][0-9]*$ ]]; then
    printf 'invalid %s: must be a positive integer\n' "$label" >&2
    exit 2
  fi
}

validate_save_proof_evidence() {
  local artifact_path=$1
  shift
  local scenario= workflow= run_id= started_at_epoch=

  while [ "$#" -gt 0 ]; do
    case "$1" in
      --scenario)
        if [ "$#" -lt 2 ] || [ -z "${2:-}" ]; then
          printf 'validate-save-proof-evidence option %s requires a value\n' "$1" >&2
          return 2
        fi
        scenario=${2:-}
        shift 2
        ;;
      --workflow)
        if [ "$#" -lt 2 ] || [ -z "${2:-}" ]; then
          printf 'validate-save-proof-evidence option %s requires a value\n' "$1" >&2
          return 2
        fi
        workflow=${2:-}
        shift 2
        ;;
      --run-id)
        if [ "$#" -lt 2 ] || [ -z "${2:-}" ]; then
          printf 'validate-save-proof-evidence option %s requires a value\n' "$1" >&2
          return 2
        fi
        run_id=${2:-}
        shift 2
        ;;
      --started-at-epoch)
        if [ "$#" -lt 2 ] || [ -z "${2:-}" ]; then
          printf 'validate-save-proof-evidence option %s requires a value\n' "$1" >&2
          return 2
        fi
        started_at_epoch=${2:-}
        shift 2
        ;;
      *)
        printf 'unknown validate-save-proof-evidence argument: %s\n' "$1" >&2
        return 2
        ;;
    esac
  done

  if [ -z "$scenario" ]; then
    printf '%s\n' 'validate-save-proof-evidence requires --scenario' >&2
    return 2
  fi
  if [ -z "$workflow" ]; then
    printf '%s\n' 'validate-save-proof-evidence requires --workflow' >&2
    return 2
  fi
  if [ -z "$run_id" ]; then
    printf '%s\n' 'validate-save-proof-evidence requires --run-id' >&2
    return 2
  fi
  if [ -z "$started_at_epoch" ]; then
    printf '%s\n' 'validate-save-proof-evidence requires --started-at-epoch' >&2
    return 2
  fi

  python3 - "$artifact_path" "$scenario" "$workflow" "$run_id" "$started_at_epoch" <<'PY'
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

artifact = Path(sys.argv[1])
expected_scenario = sys.argv[2]
expected_workflow = sys.argv[3]
expected_run_id = sys.argv[4]
started_at_epoch_value = sys.argv[5] or "0"

SCHEMA_VERSION = "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1"
MARKER = "robotSaveMenuRoundTripMarker"
EXPECTED_CLAIM = (
    "AWT Robot opened File, clicked the production Save menu item, controlled the rendered "
    "Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified "
    "robotSaveMenuRoundTripMarker"
)
REQUIRED_NON_CLAIMS = {
    "Save As coverage",
    "all Save variants",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage",
}
MAX_FUTURE_SKEW_SECONDS = 300
KNOWN_BLOCKERS = {
    "headless_awt",
    "robot_unavailable",
    "file_menu_not_showing",
    "save_item_not_attributed",
    "dialog_not_observed",
    "ambiguous_chooser_discovery",
    "chooser_control_failed",
    "target_path_rejected",
    "write_not_observed",
    "readback_failed",
    "marker_missing",
}

def fail(message):
    print(message, file=sys.stderr)
    sys.exit(1)

try:
    started_at_epoch = int(started_at_epoch_value)
except ValueError:
    fail("Save proof started-at-epoch must be an integer")
if started_at_epoch < 0:
    fail("Save proof started-at-epoch must be non-negative")

if artifact.name != "robot-save-menu-dialog-write-readback-proof.json":
    fail("Save proof evidence path must use canonical filename robot-save-menu-dialog-write-readback-proof.json")
if artifact.is_symlink():
    fail("Save proof evidence artifact must not be a symlink")
if not artifact.is_file():
    fail(f"missing Save proof evidence artifact robot-save-menu-dialog-write-readback-proof.json: {artifact}")

try:
    payload = json.loads(artifact.read_text(encoding="utf-8"))
except json.JSONDecodeError as exc:
    fail(f"invalid Save proof evidence JSON at line {exc.lineno}: {exc.msg}")

if not isinstance(payload, dict):
    fail("Save proof evidence must be a JSON object")

for key, expected in {
    "schemaVersion": SCHEMA_VERSION,
    "scenario": expected_scenario,
    "workflow": expected_workflow,
    "runId": expected_run_id,
}.items():
    value = payload.get(key)
    if value != expected:
        fail(f"Save proof evidence {key} mismatch: expected {expected!r}, got {value!r}")

if not re.fullmatch(r"[A-Za-z0-9._-]+", expected_run_id or ""):
    fail("Save proof runId must be a non-empty safe token")

status = payload.get("status")
if status not in {"proven", "blocked"}:
    fail("Save proof evidence status must be proven or blocked")

blocker = payload.get("blocker")
if status == "blocked":
    if not isinstance(blocker, dict):
        fail("blocked Save proof evidence must include blocker object")
    kind = blocker.get("kind")
    if kind not in KNOWN_BLOCKERS:
        fail(f"unknown unsupported blocker kind: {kind}")
    for field in ("observed", "required"):
        if not isinstance(blocker.get(field), str) or not blocker.get(field).strip():
            fail(f"blocked Save proof evidence blocker.{field} must be a non-empty string")
    fail(f"blocked Save proof evidence is non-proven status: {kind}")
elif blocker is not None:
    fail("proven Save proof evidence must have blocker null")

if payload.get("claim") != EXPECTED_CLAIM:
    fail("missing bounded proven Save proof claim")
does_not_claim = payload.get("doesNotClaim")
if not isinstance(does_not_claim, list) or not all(isinstance(item, str) for item in does_not_claim):
    fail("missing bounded Save proof doesNotClaim list")
missing_non_claims = sorted(REQUIRED_NON_CLAIMS - set(does_not_claim))
if missing_non_claims:
    fail("missing bounded Save proof non-claim(s): " + ", ".join(missing_non_claims))

def required_object(name):
    value = payload.get(name)
    if not isinstance(value, dict):
        fail(f"missing required {name} object")
    return value

menu = required_object("menu")
dialog = required_object("dialog")
control = required_object("control")
write = required_object("write")
readback = required_object("readback")

required_true = [
    ("menu.fileMenuOpened", menu.get("fileMenuOpened")),
    ("menu.saveMenuItemInvoked", menu.get("saveMenuItemInvoked")),
    ("menu.saveActionIdentityMatched", menu.get("saveActionIdentityMatched")),
    ("dialog.saveDialogObserved", dialog.get("saveDialogObserved")),
    ("dialog.dialogShowing", dialog.get("dialogShowing")),
    ("control.selectedPathSet", control.get("selectedPathSet")),
    ("control.approvedSelection", control.get("approvedSelection")),
    ("control.selectedPathMatchesExpected", control.get("selectedPathMatchesExpected")),
    ("control.targetInsideProofRoot", control.get("targetInsideProofRoot")),
    ("write.fileWritten", write.get("fileWritten")),
    ("write.fileNonempty", write.get("fileNonempty")),
    ("write.fileHasExpectedExtension", write.get("fileHasExpectedExtension")),
    ("readback.projectReadable", readback.get("projectReadable")),
    ("readback.markerPresent", readback.get("markerPresent")),
]
missing_or_false = [name for name, value in required_true if value is not True]
if missing_or_false:
    fail("missing required proven Save proof flag(s): " + ", ".join(missing_or_false))

if dialog.get("dialogType") != "Swing JFileChooser":
    fail("Save proof evidence dialog.dialogType must be Swing JFileChooser")
if dialog.get("ambiguousChooserDiscovery") is not False:
    fail("inconsistent proven Save proof evidence: dialog.ambiguousChooserDiscovery must be false")
if readback.get("marker") != MARKER:
    fail("Save proof evidence readback.marker mismatch")

generated_at = payload.get("generatedAtUtc")
if not isinstance(generated_at, str) or not generated_at:
    fail("Save proof evidence generatedAtUtc must be a non-empty timestamp")
try:
    generated_epoch = datetime.fromisoformat(generated_at.replace("Z", "+00:00")).timestamp()
except ValueError:
    fail("Save proof evidence generatedAtUtc is not an ISO timestamp")
now_epoch = datetime.now(timezone.utc).timestamp()
mtime_epoch = artifact.stat().st_mtime
if started_at_epoch and (generated_epoch + 1 < started_at_epoch or mtime_epoch + 1 < started_at_epoch):
    fail("stale Save proof evidence: generatedAtUtc/mtime predates command start")
if generated_epoch > now_epoch + MAX_FUTURE_SKEW_SECONDS or mtime_epoch > now_epoch + MAX_FUTURE_SKEW_SECONDS:
    fail("future Save proof evidence: generatedAtUtc/mtime exceeds validator clock skew")

output_size = write.get("outputSizeBytes")
if not isinstance(output_size, int) or output_size <= 0:
    fail("inconsistent proven Save proof evidence: outputSizeBytes must be a positive integer")
output_path_value = write.get("outputPath")
if not isinstance(output_path_value, str) or not output_path_value.strip():
    fail("missing required write.outputPath")
output_path = Path(output_path_value)
if output_path.is_absolute():
    resolved_output = output_path.resolve()
else:
    resolved_output = (artifact.parent / output_path).resolve()
artifact_parent = artifact.parent.resolve()
try:
    resolved_output.relative_to(artifact_parent)
except ValueError:
    fail("inconsistent proven Save proof evidence: outputPath escapes the evidence directory")
if not resolved_output.is_file():
    fail(f"inconsistent proven Save proof evidence: output file is missing: {resolved_output}")
actual_size = resolved_output.stat().st_size
if actual_size != output_size:
    fail(f"inconsistent proven Save proof evidence: outputSizeBytes {output_size} does not match actual size {actual_size}")
print("Save proof evidence proven")
PY
}

validate_run_window_evidence() {
  local artifact_path=$1

  python3 - "$artifact_path" <<'PY'
import json
import sys
from pathlib import Path

artifact = Path(sys.argv[1])

SCHEMA_VERSION = "eatme.alice-run-window-created/v1"
CONTRACT_SCOPE = "run-window-creation-wiring"
EVIDENCE_SOURCE = "org.alice.stageide.run.RunComposite#handlePreShowWindow"
REQUIRED_FALSE_FIELDS = {
    "active_rendering_claimed",
    "run_program_claimed",
    "run_execution_claimed",
    "world_execution_claimed",
    "rendering_correctness_claimed",
    "save_claimed",
    "grading_claimed",
    "full_ui_automation_claimed",
}
REQUIRED_NON_CLAIMS = {
    "active-rendering",
    "run-execution",
    "world-execution-correctness",
    "rendering-correctness",
    "save",
    "grading",
    "full-ui-automation",
}


def fail(message):
    print(message, file=sys.stderr)
    sys.exit(1)


if artifact.name != "run-window-created.json":
    fail("Run-window evidence path must use canonical filename run-window-created.json")
if artifact.is_symlink():
    fail("Run-window evidence artifact must not be a symlink")
if not artifact.is_file():
    fail(f"missing Run-window evidence artifact run-window-created.json: {artifact}")

try:
    payload = json.loads(artifact.read_text(encoding="utf-8"))
except json.JSONDecodeError as exc:
    fail(f"invalid Run-window evidence JSON at line {exc.lineno}: {exc.msg}")

if not isinstance(payload, dict):
    fail("Run-window evidence must be a JSON object")

for key, expected in {
    "schema_version": SCHEMA_VERSION,
    "status": "created",
    "contract_scope": CONTRACT_SCOPE,
    "evidence_source": EVIDENCE_SOURCE,
    "artifact": "run-window-created.json",
}.items():
    value = payload.get(key)
    if value != expected:
        fail(f"Run-window evidence {key} mismatch: expected {expected!r}, got {value!r}")

for field in REQUIRED_FALSE_FIELDS:
    if payload.get(field) is not False:
        fail(f"Run-window evidence {field} must be false")

does_not_claim = payload.get("does_not_claim")
if not isinstance(does_not_claim, list) or not all(isinstance(item, str) for item in does_not_claim):
    fail("Run-window evidence must include does_not_claim string list")
missing_non_claims = sorted(REQUIRED_NON_CLAIMS - set(does_not_claim))
if missing_non_claims:
    fail("missing Run-window non-claim(s): " + ", ".join(missing_non_claims))

for metadata_field in ("frame_title", "program_type"):
    if not isinstance(payload.get(metadata_field), str):
        fail(f"Run-window evidence {metadata_field} must be a string")

if artifact.stat().st_size <= 0:
    fail("Run-window evidence artifact must be non-empty")

print("Run-window evidence created")
PY
}

write_environment() {
  local run_dir=$1
  local display=${2:-${DISPLAY:-}}
  {
    printf 'timestamp_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'repo_root=%s\n' "$REPO_ROOT"
    printf 'display=%s\n' "$display"
    printf '\n[java]\n'
    java -version 2>&1 || printf 'java version unavailable (exit %s)\n' "$?"
    printf '\n[maven]\n'
    mvn -version 2>&1 || printf 'maven version unavailable (exit %s)\n' "$?"
    printf '\n[uname]\n'
    uname -a 2>&1 || printf 'uname unavailable (exit %s)\n' "$?"
  } > "$run_dir/environment.txt"
}

write_controlled_display_pixel_observation() {
  local run_dir=$1
  local status=$2
  local blocker=$3
  local blocker_detail=$4
  local display=${5:-}
  local pixels_observed=${6:-false}
  local claim=${7:-no-visible-pixel-proof}
  local missing_executable=${8:-}
  local ready_status=${9:-not-attempted}
  local process_status=${10:-not-started}
  local screenshot_status=${11:-not-attempted}
  local screenshot_file=${12:-}
  local xvfb_executable=${13:-}
  local screenshot_tool=${14:-}
  local screenshot_pixel_status=${15:-not-attempted}
  local screenshot_pixel_detail=${16:-}
  local lifecycle_point=${17:-unknown}
  local window_inventory_status=${18:-not-attempted}
  local window_inventory_file=${19:-x-window-inventory.json}
  local alice_window_candidate_count=${20:-0}
  local runtime_display_artifact=${21:-}

  CONTROLLED_DISPLAY_STATUS="$status" \
  CONTROLLED_DISPLAY_BLOCKER="$blocker" \
  CONTROLLED_DISPLAY_BLOCKER_DETAIL="$blocker_detail" \
  CONTROLLED_DISPLAY_DISPLAY="$display" \
  CONTROLLED_DISPLAY_PIXELS_OBSERVED="$pixels_observed" \
  CONTROLLED_DISPLAY_CLAIM="$claim" \
  CONTROLLED_DISPLAY_MISSING_EXECUTABLE="$missing_executable" \
  CONTROLLED_DISPLAY_READY_STATUS="$ready_status" \
  CONTROLLED_DISPLAY_PROCESS_STATUS="$process_status" \
  CONTROLLED_DISPLAY_SCREENSHOT_STATUS="$screenshot_status" \
  CONTROLLED_DISPLAY_SCREENSHOT_FILE="$screenshot_file" \
  CONTROLLED_DISPLAY_XVFB_EXECUTABLE="$xvfb_executable" \
  CONTROLLED_DISPLAY_SCREENSHOT_TOOL="$screenshot_tool" \
  CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_STATUS="$screenshot_pixel_status" \
  CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_DETAIL="$screenshot_pixel_detail" \
  CONTROLLED_DISPLAY_LIFECYCLE_POINT="$lifecycle_point" \
  CONTROLLED_DISPLAY_WINDOW_INVENTORY_STATUS="$window_inventory_status" \
  CONTROLLED_DISPLAY_WINDOW_INVENTORY_FILE="$window_inventory_file" \
  CONTROLLED_DISPLAY_ALICE_WINDOW_CANDIDATE_COUNT="$alice_window_candidate_count" \
  CONTROLLED_DISPLAY_RUNTIME_DISPLAY_ARTIFACT="$runtime_display_artifact" \
  python3 - "$run_dir/controlled-display-pixel-observation.json" <<'PY'
import json
import math
import os
import sys
from pathlib import Path

path = Path(sys.argv[1])
MISSING_TARGET = "run-window-world-canvas-screen-extents"
NEXT_UNBLOCKER = "reliable-run-window-world-canvas-pixel-sampling-target"
SOURCE_ARTIFACT = "post-open-runtime-display-accessibility-evidence.json"
SELECTION_RULE = "single-visible-showing-runtime-display-candidate-with-valid-screen-extents"
BLOCKED_GEOMETRY_STATUSES = {
    "missing-component-interface",
    "missing-extents",
    "invalid-extents",
    "ambiguous-candidates",
}

def value(name):
    return os.environ.get(name, "")

def nullable_relative_path(raw_path):
    if not raw_path:
        return None
    return Path(raw_path).name

def relative_path_or_empty(raw_path):
    relative = nullable_relative_path(raw_path)
    return relative if relative is not None else ""

def executable_name_or_empty(raw_path):
    if not raw_path:
        return ""
    return Path(raw_path).name

def parse_screenshot_dimensions(output_path):
    raw_path = output_path.parent / "screenshot-pixels.txt.raw"
    dimensions = {"width": None, "height": None}
    if not raw_path.is_file():
        return dimensions, None
    values = {}
    for line in raw_path.read_text(encoding="utf-8").splitlines():
        if "=" not in line:
            continue
        key, raw_value = line.split("=", 1)
        values[key] = raw_value
    for key in ("width", "height"):
        raw_value = values.get(key, "")
        if raw_value.isdigit():
            dimensions[key] = int(raw_value)
    return dimensions, "screenshot-pixels.txt.raw"

def is_number(raw_value):
    return isinstance(raw_value, (int, float)) and not isinstance(raw_value, bool) and math.isfinite(raw_value)

def valid_screen_extents(candidate):
    extents = candidate.get("screenExtents")
    if not isinstance(extents, dict):
        return None
    if extents.get("coordinateType") != "screen":
        return None
    for key in ("x", "y", "width", "height"):
        if not is_number(extents.get(key)):
            return None
    if extents["width"] <= 0 or extents["height"] <= 0:
        return None
    return {
        "coordinateType": "screen",
        "x": extents["x"],
        "y": extents["y"],
        "width": extents["width"],
        "height": extents["height"],
    }

def visible_showing(candidate):
    states = candidate.get("states")
    if not isinstance(states, list):
        return False
    state_set = {str(state).lower() for state in states}
    return "visible" in state_set and "showing" in state_set

def has_valid_visible_screen_extents(candidate):
    return (
        isinstance(candidate, dict)
        and visible_showing(candidate)
        and derived_geometry_status(candidate) == "available"
        and valid_screen_extents(candidate) is not None
    )

def valid_visible_screen_extent_candidates(candidates):
    return [
        candidate
        for candidate in candidates
        if has_valid_visible_screen_extents(candidate)
    ]

def derived_geometry_status(candidate):
    status = str(candidate.get("geometryStatus") or "")
    if status in BLOCKED_GEOMETRY_STATUSES or status == "available":
        return status
    extents = candidate.get("screenExtents")
    if extents is None:
        return "missing-extents"
    if not isinstance(extents, dict):
        return "invalid-extents"
    if extents.get("coordinateType") != "screen":
        return "invalid-extents"
    return "available" if valid_screen_extents(candidate) is not None else "invalid-extents"

def zero_valid_candidate_geometry_status(candidates):
    visible_candidates = [
        candidate for candidate in candidates
        if isinstance(candidate, dict) and visible_showing(candidate)
    ]
    if not visible_candidates:
        return "missing-extents"
    statuses = []
    for candidate in visible_candidates:
        geometry_status = derived_geometry_status(candidate)
        if geometry_status == "available":
            geometry_status = "invalid-extents"
        statuses.append(geometry_status)
    if "invalid-extents" in statuses:
        return "invalid-extents"
    if "missing-component-interface" in statuses:
        return "missing-component-interface"
    return "missing-extents"

def blocked_world_canvas_target(geometry_status, candidate_count):
    return {
        "identified": False,
        "status": "blocked",
        "missingTarget": MISSING_TARGET,
        "exactNextUnblocker": NEXT_UNBLOCKER,
        "geometryStatus": geometry_status,
        "sourceArtifact": SOURCE_ARTIFACT,
        "runtimeDisplayCandidateCount": candidate_count,
    }

def target_ready_payload(candidate, candidate_count):
    extents = valid_screen_extents(candidate)
    return {
        "identified": True,
        "status": "target-ready",
        "sourceArtifact": SOURCE_ARTIFACT,
        "candidatePath": str(candidate.get("path", "")),
        "candidateName": str(candidate.get("name", "")),
        "candidateRole": str(candidate.get("role", "")),
        "candidateStates": candidate.get("states") if isinstance(candidate.get("states"), list) else [],
        "geometryStatus": "available",
        "screenExtents": extents,
        "selectionRule": SELECTION_RULE,
        "runtimeDisplayCandidateCount": candidate_count,
    }

def world_canvas_target_from_runtime_display(runtime_display_path):
    if not runtime_display_path:
        return blocked_world_canvas_target("missing-extents", 0)
    try:
        runtime_display = json.loads(Path(runtime_display_path).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return blocked_world_canvas_target("missing-extents", 0)
    if not isinstance(runtime_display, dict):
        return blocked_world_canvas_target("missing-extents", 0)
    candidates = runtime_display.get("runtimeDisplayCandidates")
    if not isinstance(candidates, list):
        candidates = []
    reported_count = runtime_display.get("runtimeDisplayCandidateCount")
    candidate_count = reported_count if isinstance(reported_count, int) and reported_count >= 0 else len(candidates)
    valid_candidates = valid_visible_screen_extent_candidates(candidates)
    if len(valid_candidates) == 1:
        return target_ready_payload(valid_candidates[0], candidate_count)
    if len(valid_candidates) > 1:
        return blocked_world_canvas_target("ambiguous-candidates", candidate_count)
    return blocked_world_canvas_target(
        zero_valid_candidate_geometry_status(candidates),
        candidate_count,
    )

pixels_observed = value("CONTROLLED_DISPLAY_PIXELS_OBSERVED") == "true"
screenshot_status = value("CONTROLLED_DISPLAY_SCREENSHOT_STATUS")
screenshot_file = nullable_relative_path(value("CONTROLLED_DISPLAY_SCREENSHOT_FILE"))
screenshot_pixel_status = value("CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_STATUS")
screenshot_dimensions, screenshot_metadata_source = parse_screenshot_dimensions(path)
world_canvas_pixel_target = world_canvas_target_from_runtime_display(
    value("CONTROLLED_DISPLAY_RUNTIME_DISPLAY_ARTIFACT")
)
consistent_with_screenshot = (
    pixels_observed
    and screenshot_status == "screenshot-captured"
    and screenshot_pixel_status == "non-black-pixels"
    and screenshot_dimensions["width"] is not None
    and screenshot_dimensions["height"] is not None
    and screenshot_file is not None
)

payload = {
    "schemaVersion": 1,
    "claimScope": "controlled-display-screenshot-consistency",
    "status": value("CONTROLLED_DISPLAY_STATUS"),
    "blocker": value("CONTROLLED_DISPLAY_BLOCKER"),
    "blockerDetail": value("CONTROLLED_DISPLAY_BLOCKER_DETAIL"),
    "display": value("CONTROLLED_DISPLAY_DISPLAY"),
    "pixelsObserved": pixels_observed,
    "claim": value("CONTROLLED_DISPLAY_CLAIM"),
    "readyStatus": value("CONTROLLED_DISPLAY_READY_STATUS"),
    "processStatus": value("CONTROLLED_DISPLAY_PROCESS_STATUS"),
    "visibleRenderingCorrectnessEstablished": False,
    "screenshotStatus": screenshot_status,
    "screenshotFile": relative_path_or_empty(value("CONTROLLED_DISPLAY_SCREENSHOT_FILE")),
    "xvfbExecutable": executable_name_or_empty(value("CONTROLLED_DISPLAY_XVFB_EXECUTABLE")),
    "screenshotTool": value("CONTROLLED_DISPLAY_SCREENSHOT_TOOL"),
    "screenshotPixelStatus": screenshot_pixel_status,
    "screenshotPixelDetail": value("CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_DETAIL"),
    "lifecyclePoint": value("CONTROLLED_DISPLAY_LIFECYCLE_POINT"),
    "windowInventoryStatus": value("CONTROLLED_DISPLAY_WINDOW_INVENTORY_STATUS"),
    "windowInventoryFile": value("CONTROLLED_DISPLAY_WINDOW_INVENTORY_FILE"),
    "aliceWindowCandidateCount": int(value("CONTROLLED_DISPLAY_ALICE_WINDOW_CANDIDATE_COUNT") or "0"),
    "screenshot": {
        "path": screenshot_file,
        "status": screenshot_status,
        "tool": value("CONTROLLED_DISPLAY_SCREENSHOT_TOOL") or None,
        "dimensions": screenshot_dimensions,
        "metadataSource": screenshot_metadata_source,
    },
    "pixelObservation": {
        "status": screenshot_pixel_status,
        "detail": value("CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_DETAIL"),
        "pixelsObserved": pixels_observed,
        "consistentWithScreenshot": consistent_with_screenshot,
    },
    "worldCanvasPixelTarget": world_canvas_pixel_target,
    "unsupportedClaims": [
        "world-canvas-pixel-correctness",
        "full-visible-rendering-correctness",
        "full-ui-automation",
        "world-execution",
        "grading",
        "save-behavior",
        "first-lesson-completion",
    ],
}
missing_executable = value("CONTROLLED_DISPLAY_MISSING_EXECUTABLE")
if missing_executable:
    payload["missingExecutable"] = missing_executable

with path.open("w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

write_visible_rendering_pixel_target_blocker() {
  local run_dir=$1
  local controlled_display_status=${2:-blocked}
  local controlled_display_blocker=${3:-unknown}
  local screenshot_file=${4:-}
  local screenshot_status=${5:-not-attempted}
  local screenshot_pixel_status=${6:-not-attempted}
  local runtime_display_artifact=${7:-}

  VISIBLE_RENDERING_CONTROLLED_DISPLAY_STATUS="$controlled_display_status" \
  VISIBLE_RENDERING_CONTROLLED_DISPLAY_BLOCKER="$controlled_display_blocker" \
  VISIBLE_RENDERING_SCREENSHOT_FILE="$screenshot_file" \
  VISIBLE_RENDERING_SCREENSHOT_STATUS="$screenshot_status" \
  VISIBLE_RENDERING_SCREENSHOT_PIXEL_STATUS="$screenshot_pixel_status" \
  VISIBLE_RENDERING_RUNTIME_DISPLAY_ARTIFACT="$runtime_display_artifact" \
  python3 - "$run_dir/$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER" <<'PY'
import json
import math
import os
import sys
from pathlib import Path

output_path = Path(sys.argv[1])
screenshot_file = os.environ.get("VISIBLE_RENDERING_SCREENSHOT_FILE", "")
screenshot_path = Path(screenshot_file).name if screenshot_file else None
MISSING_TARGET = "run-window-world-canvas-screen-extents"
NEXT_UNBLOCKER = "reliable-run-window-world-canvas-pixel-sampling-target"
SOURCE_ARTIFACT = "post-open-runtime-display-accessibility-evidence.json"
BLOCKED_GEOMETRY_STATUSES = {
    "missing-component-interface",
    "missing-extents",
    "invalid-extents",
    "ambiguous-candidates",
}
unsupported_claims = [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "full-ui-automation",
    "world-execution",
    "grading",
    "save-behavior",
    "first-lesson-completion",
]

def is_number(raw_value):
    return isinstance(raw_value, (int, float)) and not isinstance(raw_value, bool) and math.isfinite(raw_value)

def valid_screen_extents(candidate):
    extents = candidate.get("screenExtents")
    if not isinstance(extents, dict):
        return None
    if extents.get("coordinateType") != "screen":
        return None
    for key in ("x", "y", "width", "height"):
        if not is_number(extents.get(key)):
            return None
    if extents["width"] <= 0 or extents["height"] <= 0:
        return None
    return extents

def visible_showing(candidate):
    states = candidate.get("states")
    if not isinstance(states, list):
        return False
    state_set = {str(state).lower() for state in states}
    return "visible" in state_set and "showing" in state_set

def has_valid_visible_screen_extents(candidate):
    return (
        isinstance(candidate, dict)
        and visible_showing(candidate)
        and derived_geometry_status(candidate) == "available"
        and valid_screen_extents(candidate) is not None
    )

def valid_visible_screen_extent_candidates(candidates):
    return [
        candidate
        for candidate in candidates
        if has_valid_visible_screen_extents(candidate)
    ]

def derived_geometry_status(candidate):
    status = str(candidate.get("geometryStatus") or "")
    if status in BLOCKED_GEOMETRY_STATUSES or status == "available":
        return status
    extents = candidate.get("screenExtents")
    if extents is None:
        return "missing-extents"
    if not isinstance(extents, dict):
        return "invalid-extents"
    if extents.get("coordinateType") != "screen":
        return "invalid-extents"
    return "available" if valid_screen_extents(candidate) is not None else "invalid-extents"

def zero_valid_candidate_geometry_status(candidates):
    visible_candidates = [
        candidate for candidate in candidates
        if isinstance(candidate, dict) and visible_showing(candidate)
    ]
    if not visible_candidates:
        return "missing-extents"
    statuses = []
    for candidate in visible_candidates:
        geometry_status = derived_geometry_status(candidate)
        if geometry_status == "available":
            geometry_status = "invalid-extents"
        statuses.append(geometry_status)
    if "invalid-extents" in statuses:
        return "invalid-extents"
    if "missing-component-interface" in statuses:
        return "missing-component-interface"
    return "missing-extents"

def blocker_metadata(runtime_display_path):
    if not runtime_display_path:
        return "missing-extents", 0
    try:
        runtime_display = json.loads(Path(runtime_display_path).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return "missing-extents", 0
    if not isinstance(runtime_display, dict):
        return "missing-extents", 0
    candidates = runtime_display.get("runtimeDisplayCandidates")
    if not isinstance(candidates, list):
        candidates = []
    reported_count = runtime_display.get("runtimeDisplayCandidateCount")
    candidate_count = reported_count if isinstance(reported_count, int) and reported_count >= 0 else len(candidates)
    valid_candidates = valid_visible_screen_extent_candidates(candidates)
    if len(valid_candidates) > 1:
        return "ambiguous-candidates", candidate_count
    return zero_valid_candidate_geometry_status(candidates), candidate_count

geometry_status, candidate_count = blocker_metadata(
    os.environ.get("VISIBLE_RENDERING_RUNTIME_DISPLAY_ARTIFACT", "")
)
target = {
    "identified": False,
    "status": "blocked",
    "missingTarget": MISSING_TARGET,
    "exactNextUnblocker": NEXT_UNBLOCKER,
    "geometryStatus": geometry_status,
    "sourceArtifact": SOURCE_ARTIFACT,
    "runtimeDisplayCandidateCount": candidate_count,
}
payload = {
    "schemaVersion": 1,
    "status": "blocked",
    "blocker": "world-canvas-pixel-target-not-identified",
    "blockerDetail": (
        "No reliable Run-window/world-canvas screen-coordinate pixel sampling target has been "
        "identified. Controlled-display screenshots can support screenshot "
        "consistency only; they cannot prove rendered-world pixel correctness."
    ),
    "claimScope": "visible-rendering-world-canvas-pixel-target",
    "claimScopeDetail": "target-readiness-only",
    "missingTarget": MISSING_TARGET,
    "exactNextUnblocker": NEXT_UNBLOCKER,
    "sourceArtifact": "controlled-display-pixel-observation.json",
    "controlledDisplayStatus": os.environ.get("VISIBLE_RENDERING_CONTROLLED_DISPLAY_STATUS", ""),
    "controlledDisplayBlocker": os.environ.get("VISIBLE_RENDERING_CONTROLLED_DISPLAY_BLOCKER", ""),
    "screenshotPath": screenshot_path,
    "screenshotStatus": os.environ.get("VISIBLE_RENDERING_SCREENSHOT_STATUS", ""),
    "screenshotPixelStatus": os.environ.get("VISIBLE_RENDERING_SCREENSHOT_PIXEL_STATUS", ""),
    "runtimeDisplayCandidateCount": candidate_count,
    "geometryStatus": geometry_status,
    "worldCanvasPixelTarget": target,
    "visibleRenderingCorrectnessEstablished": False,
    "unsupportedClaims": unsupported_claims,
}
with output_path.open("w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

write_visible_rendering_pixel_sampling_blocker() {
  local run_dir=$1
  local controlled_display_artifact=${2:-}

  VISIBLE_RENDERING_CONTROLLED_DISPLAY_ARTIFACT="$controlled_display_artifact" \
  python3 - "$run_dir/$VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER" <<'PY'
import json
import os
import sys
from pathlib import Path

output_path = Path(sys.argv[1])
controlled_display_path = Path(os.environ.get("VISIBLE_RENDERING_CONTROLLED_DISPLAY_ARTIFACT", ""))
SOURCE_ARTIFACT = "controlled-display-pixel-observation.json"
CONTROLLED_DISPLAY_CLAIM_SCOPE = "controlled-display-screenshot-consistency"
CONTROLLED_DISPLAY_STATUSES = {"observed", "blocked"}
unsupported_claims = [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness",
    "full-ui-automation",
    "world-execution",
    "grading",
    "save-behavior",
    "first-lesson-completion",
]


def read_controlled_display(path):
    if path.name != SOURCE_ARTIFACT:
        return None
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    if not isinstance(payload, dict):
        return None
    if payload.get("schemaVersion") != 1:
        return None
    if payload.get("claimScope") != CONTROLLED_DISPLAY_CLAIM_SCOPE:
        return None
    if payload.get("status") not in CONTROLLED_DISPLAY_STATUSES:
        return None
    return payload


def screenshot_path(payload):
    if not isinstance(payload, dict):
        return None
    screenshot = payload.get("screenshot")
    if isinstance(screenshot, dict) and screenshot.get("path"):
        return Path(str(screenshot["path"])).name
    raw_path = payload.get("screenshotFile")
    return Path(str(raw_path)).name if raw_path else None


controlled = read_controlled_display(controlled_display_path)
target = {}
if isinstance(controlled, dict) and isinstance(controlled.get("worldCanvasPixelTarget"), dict):
    target = controlled["worldCanvasPixelTarget"]

target_ready = target.get("identified") is True and target.get("status") == "target-ready"
if target_ready:
    blocker = "world-canvas-pixel-sampler-unavailable"
    blocker_detail = (
        "A single world-canvas pixel target is ready, but no usable target-scoped "
        "world-canvas pixel sampler is available for this run."
    )
    claim_scope_detail = "target-ready-sampling-not-observed"
    prerequisite_status = "target-ready"
    exact_next_unblocker = "provide-world-canvas-pixel-sampler"
else:
    blocker = "world-canvas-pixel-target-not-ready"
    blocker_detail = (
        "World-canvas pixel sampling requires worldCanvasPixelTarget.status=target-ready; "
        "target selection is blocked or unavailable, so no rendered-world pixels were sampled."
    )
    claim_scope_detail = "target-selection-blocked"
    prerequisite_status = str(target.get("status") or "unavailable")
    exact_next_unblocker = str(
        target.get("exactNextUnblocker") or "reliable-run-window-world-canvas-pixel-sampling-target"
    )

payload = {
    "schemaVersion": 1,
    "status": "blocked",
    "blocker": blocker,
    "blockerDetail": blocker_detail,
    "claimScope": "visible-rendering-world-canvas-pixel-sampling",
    "claimScopeDetail": claim_scope_detail,
    "sourceArtifact": SOURCE_ARTIFACT,
    "prerequisiteTargetStatus": prerequisite_status,
    "exactNextUnblocker": exact_next_unblocker,
    "renderedWorldPixelsObserved": False,
    "visibleRenderingCorrectnessEstablished": False,
    "sampleCount": 0,
    "screenshotPath": screenshot_path(controlled),
    "screenshotStatus": controlled.get("screenshotStatus") if isinstance(controlled, dict) else "",
    "screenshotPixelStatus": controlled.get("screenshotPixelStatus") if isinstance(controlled, dict) else "",
    "worldCanvasPixelTarget": target,
    "pixelSampling": {
        "status": "blocked",
        "blocker": blocker,
        "pixelsSampled": False,
        "sampleCount": 0,
        "samplingMethod": None,
        "correctnessCheck": "not-performed",
    },
    "unsupportedClaims": unsupported_claims,
}

with output_path.open("w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

write_visible_rendering_pixel_sampling_evidence() {
  local run_dir=$1
  local controlled_display_artifact=${2:-}
  local sampler=${ALICE_QA_WORLD_CANVAS_PIXEL_SAMPLER:-$WORLD_CANVAS_PIXEL_SAMPLER}

  VISIBLE_RENDERING_CONTROLLED_DISPLAY_ARTIFACT="$controlled_display_artifact" \
  VISIBLE_RENDERING_WORLD_CANVAS_PIXEL_SAMPLER="$sampler" \
  VISIBLE_RENDERING_OBSERVATION_PATH="$run_dir/$VISIBLE_RENDERING_PIXEL_OBSERVATION" \
  VISIBLE_RENDERING_BLOCKER_PATH="$run_dir/$VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER" \
  python3 - <<'PY'
import json
import math
import os
import subprocess
import sys
import tempfile
from pathlib import Path

SOURCE_ARTIFACT = "controlled-display-pixel-observation.json"
TARGET_SOURCE_ARTIFACT = "post-open-runtime-display-accessibility-evidence.json"
CONTROLLED_DISPLAY_CLAIM_SCOPE = "controlled-display-screenshot-consistency"
SELECTION_RULE = "single-visible-showing-runtime-display-candidate-with-valid-screen-extents"
CLAIM_SCOPE = "visible-rendering-world-canvas-pixel-sampling"
unsupported_claims = [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness",
    "full-ui-automation",
    "world-execution",
    "grading",
    "save-behavior",
    "first-lesson-completion",
]
forbidden_claim_phrases = (
    "visible rendering correctness established",
    "visible rendering correctness passed",
    "rendered-world correctness established",
    "world canvas pixel correctness passed",
)

controlled_display_path = Path(os.environ.get("VISIBLE_RENDERING_CONTROLLED_DISPLAY_ARTIFACT", ""))
sampler_path = Path(os.environ.get("VISIBLE_RENDERING_WORLD_CANVAS_PIXEL_SAMPLER", ""))
observation_path = Path(os.environ["VISIBLE_RENDERING_OBSERVATION_PATH"])
blocker_path = Path(os.environ["VISIBLE_RENDERING_BLOCKER_PATH"])


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as stream:
        json.dump(payload, stream, indent=2, sort_keys=True)
        stream.write("\n")


def is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def read_controlled_display(path):
    if path.name != SOURCE_ARTIFACT:
        return None, "world-canvas-pixel-source-artifact-invalid", "Controlled-display source artifact must use the fixed artifact name."
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return None, "world-canvas-pixel-source-artifact-invalid", f"Controlled-display source artifact is unreadable: {exc}"
    if not isinstance(payload, dict):
        return None, "world-canvas-pixel-source-artifact-invalid", "Controlled-display source artifact must be a JSON object."
    if payload.get("schemaVersion") != 1:
        return None, "world-canvas-pixel-source-artifact-invalid", "Controlled-display source artifact must use schemaVersion=1."
    if payload.get("claimScope") != CONTROLLED_DISPLAY_CLAIM_SCOPE:
        return None, "world-canvas-pixel-source-artifact-invalid", "Controlled-display source artifact has an unsupported claim scope."
    if payload.get("status") not in {"observed", "blocked"}:
        return None, "world-canvas-pixel-source-artifact-invalid", "Controlled-display source artifact has an unsupported status."
    return payload, "", ""


def screenshot_path(payload):
    if not isinstance(payload, dict):
        return None
    screenshot = payload.get("screenshot")
    if isinstance(screenshot, dict) and screenshot.get("path"):
        return Path(str(screenshot["path"])).name
    raw_path = payload.get("screenshotFile")
    return Path(str(raw_path)).name if raw_path else None


def target_extents(target):
    extents = target.get("screenExtents") if isinstance(target, dict) else None
    if not isinstance(extents, dict) or extents.get("coordinateType") != "screen":
        return None
    for key in ("x", "y", "width", "height"):
        if not is_number(extents.get(key)):
            return None
    if extents["width"] <= 0 or extents["height"] <= 0:
        return None
    return {
        "coordinateType": "screen",
        "x": extents["x"],
        "y": extents["y"],
        "width": extents["width"],
        "height": extents["height"],
    }


def target_has_visible_showing_state(target):
    states = target.get("candidateStates") if isinstance(target, dict) else None
    if not isinstance(states, list):
        return False
    normalized = {str(state).lower() for state in states}
    return "visible" in normalized and "showing" in normalized


def validated_target(controlled):
    target = controlled.get("worldCanvasPixelTarget") if isinstance(controlled, dict) else None
    if not isinstance(target, dict):
        return None, {}
    extents = target_extents(target)
    if (
        target.get("identified") is True
        and target.get("status") == "target-ready"
        and target.get("sourceArtifact") == TARGET_SOURCE_ARTIFACT
        and target.get("geometryStatus") == "available"
        and target.get("selectionRule") == SELECTION_RULE
        and target_has_visible_showing_state(target)
        and extents is not None
    ):
        sanitized = dict(target)
        sanitized["screenExtents"] = extents
        return sanitized, sanitized
    return None, target


def base_blocker(blocker, blocker_detail, claim_scope_detail, prerequisite_status, exact_next_unblocker, controlled=None, target=None, sampling=None):
    target_payload = target if isinstance(target, dict) and prerequisite_status == "target-ready" else {}
    if isinstance(target, dict) and target.get("status") == "blocked":
        target_payload = target
    payload = {
        "schemaVersion": 1,
        "status": "blocked",
        "blocker": blocker,
        "blockerDetail": blocker_detail,
        "claimScope": CLAIM_SCOPE,
        "claimScopeDetail": claim_scope_detail,
        "sourceArtifact": SOURCE_ARTIFACT,
        "prerequisiteTargetStatus": prerequisite_status,
        "exactNextUnblocker": exact_next_unblocker,
        "renderedWorldPixelsObserved": False,
        "visibleRenderingCorrectnessEstablished": False,
        "sampleCount": 0,
        "screenshotPath": screenshot_path(controlled),
        "screenshotStatus": controlled.get("screenshotStatus") if isinstance(controlled, dict) else "",
        "screenshotPixelStatus": controlled.get("screenshotPixelStatus") if isinstance(controlled, dict) else "",
        "worldCanvasPixelTarget": target_payload,
        "pixelSampling": sampling
        or {
            "status": "blocked",
            "blocker": blocker,
            "pixelsSampled": False,
            "sampleCount": 0,
            "samplingMethod": None,
            "correctnessCheck": "not-performed",
        },
        "unsupportedClaims": unsupported_claims,
    }
    return payload


def write_blocker(blocker, blocker_detail, claim_scope_detail, prerequisite_status, exact_next_unblocker, controlled=None, target=None, sampling=None):
    if observation_path.exists():
        observation_path.unlink()
    write_json(
        blocker_path,
        base_blocker(
            blocker,
            blocker_detail,
            claim_scope_detail,
            prerequisite_status,
            exact_next_unblocker,
            controlled=controlled,
            target=target,
            sampling=sampling,
        ),
    )


def string_values(value):
    if isinstance(value, dict):
        for child in value.values():
            yield from string_values(child)
    elif isinstance(value, list):
        for child in value:
            yield from string_values(child)
    elif isinstance(value, str):
        yield value


def positive_correctness_value(value):
    if value is True:
        return True
    if isinstance(value, str):
        return value.strip().lower() in {
            "accepted",
            "confirmed",
            "correct",
            "established",
            "observed",
            "passed",
            "performed",
            "success",
            "valid",
            "validated",
            "verified",
        }
    if isinstance(value, dict):
        return any(positive_correctness_value(child) for child in value.values())
    if isinstance(value, list):
        return any(positive_correctness_value(child) for child in value)
    return False


def key_implies_visible_correctness(key):
    lower = key.lower()
    return any(token in lower for token in ("correct", "validat", "validity")) and any(
        token in lower
        for token in ("visible", "visual", "rendered", "rendering", "world")
    )


def claim_value_implies_visible_correctness(key, value):
    if key not in {"claim", "claimScope", "claimScopeDetail", "boundedClaim"} or not isinstance(value, str):
        return False
    lower = value.strip().lower()
    if any(marker in lower for marker in ("cannot ", "does not ", "do not ", "must not ", "not ", "unsupported", "nonclaim", "not-asserted")):
        return False
    return any(
        token in lower
        for token in (
            "full-visible-rendering-correctness",
            "rendered-world-correctness",
            "visible-rendering-correctness",
            "visual-correctness",
            "world-canvas-pixel-correctness",
        )
    )


def contains_success_shaped_correctness_field(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key == "unsupportedClaims":
                continue
            if key == "correctnessCheck" and child != "not-performed":
                return True
            if claim_value_implies_visible_correctness(str(key), child):
                return True
            if key_implies_visible_correctness(str(key)) and positive_correctness_value(child):
                return True
            if contains_success_shaped_correctness_field(child):
                return True
    elif isinstance(value, list):
        return any(contains_success_shaped_correctness_field(child) for child in value)
    return False


def sampler_overclaims(payload):
    if payload.get("visibleRenderingCorrectnessEstablished") is True:
        return True
    if contains_success_shaped_correctness_field(payload):
        return True
    return any(phrase in value.lower() for value in string_values(payload) for phrase in forbidden_claim_phrases)


def point_inside(point, extents):
    return (
        is_number(point.get("x"))
        and is_number(point.get("y"))
        and extents["x"] <= point["x"] < extents["x"] + extents["width"]
        and extents["y"] <= point["y"] < extents["y"] + extents["height"]
    )


def normalize_samples(payload, extents):
    samples = payload.get("samples")
    if not isinstance(samples, list) or not samples:
        return None, "world-canvas-pixel-sampling-incomplete", "Sampler output did not include a non-empty samples list."
    declared_count = payload.get("sampleCount")
    if declared_count != len(samples):
        return None, "world-canvas-pixel-sampling-incomplete", "Sampler sampleCount did not match the sample list length."
    normalized = []
    for index, sample in enumerate(samples):
        if not isinstance(sample, dict):
            return None, "world-canvas-pixel-sampling-incomplete", f"Sampler sample {index} is not an object."
        if sample.get("checked") is not True:
            return None, "world-canvas-pixel-samples-unchecked", f"Sampler sample {index} was not checked."
        point = sample.get("point")
        rgba = sample.get("rgba")
        if not isinstance(point, dict) or not point_inside(point, extents):
            return None, "world-canvas-pixel-sampling-incomplete", f"Sampler sample {index} point is outside the validated target."
        if not isinstance(rgba, list) or len(rgba) != 4:
            return None, "world-canvas-pixel-sampling-incomplete", f"Sampler sample {index} does not include an RGBA value."
        if any(not isinstance(channel, int) or isinstance(channel, bool) or channel < 0 or channel > 255 for channel in rgba):
            return None, "world-canvas-pixel-sampling-incomplete", f"Sampler sample {index} RGBA channels must be integers from 0 through 255."
        normalized_sample = {
            "point": {"x": point["x"], "y": point["y"]},
            "rgba": list(rgba),
            "checked": True,
        }
        if sample.get("name"):
            normalized_sample["name"] = str(sample["name"])
        normalized.append(normalized_sample)
    return normalized, "", ""


controlled, source_blocker, source_detail = read_controlled_display(controlled_display_path)
if controlled is None:
    write_blocker(
        "world-canvas-pixel-target-not-ready",
        source_detail,
        "target-selection-blocked",
        "unavailable",
        "valid-controlled-display-pixel-observation-source-artifact",
    )
    raise SystemExit(0)

valid_target, source_target = validated_target(controlled)
if valid_target is None:
    prerequisite_status = str(source_target.get("status") or "unavailable") if isinstance(source_target, dict) else "unavailable"
    write_blocker(
        "world-canvas-pixel-target-not-ready",
        "Target-scoped pixel sampling requires exactly one visible/showing target with valid positive screen-coordinate extents.",
        "target-selection-blocked",
        prerequisite_status,
        str(source_target.get("exactNextUnblocker") or "reliable-run-window-world-canvas-pixel-sampling-target") if isinstance(source_target, dict) else "reliable-run-window-world-canvas-pixel-sampling-target",
        controlled=controlled,
        target=source_target if isinstance(source_target, dict) else {},
    )
    raise SystemExit(0)

if not sampler_path.is_file() or not os.access(sampler_path, os.X_OK):
    write_blocker(
        "world-canvas-pixel-sampler-unavailable",
        "A valid world-canvas target was identified, but the target-scoped pixel sampler is unavailable or not executable.",
        "target-ready-sampling-not-observed",
        "target-ready",
        "provide-world-canvas-pixel-sampler",
        controlled=controlled,
        target=valid_target,
    )
    raise SystemExit(0)

with tempfile.TemporaryDirectory(prefix="alice-world-canvas-pixels-") as temp_dir:
    target_path = Path(temp_dir) / "world-canvas-target.json"
    sampler_output_path = Path(temp_dir) / "sampler-output.json"
    write_json(target_path, valid_target)
    try:
        completed = subprocess.run(
            [str(sampler_path), "--target-json", str(target_path), "--output", str(sampler_output_path)],
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        write_blocker(
            "world-canvas-pixel-sampling-failed",
            f"Sampler could not run: {exc}",
            "sampling-failed",
            "target-ready",
            "sample-run-window-world-canvas-pixels",
            controlled=controlled,
            target=valid_target,
        )
        raise SystemExit(0)
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout or "Sampler exited non-zero.").strip()
        write_blocker(
            "world-canvas-pixel-sampling-failed",
            detail[:500],
            "sampling-failed",
            "target-ready",
            "sample-run-window-world-canvas-pixels",
            controlled=controlled,
            target=valid_target,
        )
        raise SystemExit(0)
    try:
        sampler_payload = json.loads(sampler_output_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        write_blocker(
            "world-canvas-pixel-sampling-incomplete",
            f"Sampler output was unreadable: {exc}",
            "sampling-output-invalid",
            "target-ready",
            "complete-run-window-world-canvas-pixel-sample-set",
            controlled=controlled,
            target=valid_target,
        )
        raise SystemExit(0)

if not isinstance(sampler_payload, dict) or sampler_payload.get("status") != "observed":
    write_blocker(
        "world-canvas-pixel-sampling-failed",
        "Sampler output did not report status=observed.",
        "sampling-failed",
        "target-ready",
        "sample-run-window-world-canvas-pixels",
        controlled=controlled,
        target=valid_target,
    )
    raise SystemExit(0)

if sampler_overclaims(sampler_payload):
    write_blocker(
        "world-canvas-pixel-sampler-overclaimed",
        "Sampler output attempted to assert a correctness claim instead of bounded raw pixel observation.",
        "sampler-output-overclaimed",
        "target-ready",
        "remove-correctness-claims-from-sampler-output",
        controlled=controlled,
        target=valid_target,
    )
    raise SystemExit(0)

samples, sample_blocker, sample_detail = normalize_samples(sampler_payload, valid_target["screenExtents"])
if samples is None:
    write_blocker(
        sample_blocker,
        sample_detail,
        "sampling-output-invalid",
        "target-ready",
        "check-target-scoped-pixel-samples-before-claiming-observation"
        if sample_blocker == "world-canvas-pixel-samples-unchecked"
        else "complete-run-window-world-canvas-pixel-sample-set",
        controlled=controlled,
        target=valid_target,
    )
    raise SystemExit(0)

sampling_method = str(sampler_payload.get("samplingMethod") or "target-scoped-controlled-display-raw-rgba")
payload = {
    "schemaVersion": 1,
    "status": "observed",
    "blocker": "none",
    "blockerDetail": "",
    "claim": "run-window-world-canvas-target-sampled-rendering-correctness-not-asserted",
    "claimScope": CLAIM_SCOPE,
    "claimScopeDetail": "target-scoped-raw-pixel-observation-only",
    "boundedClaim": "The run-window/world-canvas target was identified and sampled under controlled conditions.",
    "sourceArtifact": SOURCE_ARTIFACT,
    "targetSourceArtifact": TARGET_SOURCE_ARTIFACT,
    "visibleRenderingCorrectnessEstablished": False,
    "renderedWorldPixelsObserved": True,
    "prerequisiteTargetStatus": "target-ready",
    "sampleCount": len(samples),
    "samplingMethod": sampling_method,
    "samples": samples,
    "worldCanvasPixelTarget": valid_target,
    "pixelSampling": {
        "status": "observed",
        "pixelsSampled": True,
        "samplesChecked": True,
        "sampleCount": len(samples),
        "samplingMethod": sampling_method,
        "sampler": sampler_path.name,
        "samplePoints": samples,
        "samplePointRule": "inside-target-bounds-only",
        "correctnessCheck": "not-performed",
    },
    "limitations": [
        "Raw RGBA samples are bounded observation data only.",
        "No color expectation, image baseline, visual diff, world execution assertion, or rendered-world oracle was applied.",
    ],
    "unsupportedClaims": unsupported_claims,
}
if blocker_path.exists():
    blocker_path.unlink()
write_json(observation_path, payload)
PY
}

write_x_window_inventory() {
  local run_dir=$1
  local status=$2
  local blocker=$3
  local blocker_detail=$4
  local display=${5:-}
  local lifecycle_point=${6:-unknown}
  local alice_launch_pid=${7:-}
  local missing_executable=${8:-}
  local detector_status=${9:-not-started}

  WINDOW_INVENTORY_STATUS="$status" \
  WINDOW_INVENTORY_BLOCKER="$blocker" \
  WINDOW_INVENTORY_BLOCKER_DETAIL="$blocker_detail" \
  WINDOW_INVENTORY_DISPLAY="$display" \
  WINDOW_INVENTORY_LIFECYCLE_POINT="$lifecycle_point" \
  WINDOW_INVENTORY_ALICE_LAUNCH_PID="$alice_launch_pid" \
  WINDOW_INVENTORY_MISSING_EXECUTABLE="$missing_executable" \
  WINDOW_INVENTORY_DETECTOR_STATUS="$detector_status" \
  python3 - "$run_dir/x-window-inventory.json" <<'PY'
import json
import os
import sys

payload = {
    "status": os.environ.get("WINDOW_INVENTORY_STATUS", ""),
    "blocker": os.environ.get("WINDOW_INVENTORY_BLOCKER", ""),
    "blockerDetail": os.environ.get("WINDOW_INVENTORY_BLOCKER_DETAIL", ""),
    "display": os.environ.get("WINDOW_INVENTORY_DISPLAY", ""),
    "lifecyclePoint": os.environ.get("WINDOW_INVENTORY_LIFECYCLE_POINT", ""),
    "aliceLaunchPid": os.environ.get("WINDOW_INVENTORY_ALICE_LAUNCH_PID", ""),
    "detectorStatus": os.environ.get("WINDOW_INVENTORY_DETECTOR_STATUS", ""),
    "aliceWindowCandidateCount": 0,
    "javaWindowCount": 0,
    "windows": [],
}
missing = os.environ.get("WINDOW_INVENTORY_MISSING_EXECUTABLE", "")
if missing:
    payload["missingExecutable"] = missing

with open(sys.argv[1], "w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

collect_x_window_inventory() {
  local run_dir=$1
  local display=$2
  local lifecycle_point=$3
  local alice_launch_pid=${4:-}

  if [ "${ALICE_QA_DISABLE_WINDOW_DETECTOR:-}" = "1" ] || ! command -v xdotool >/dev/null 2>&1; then
    write_x_window_inventory \
      "$run_dir" \
      blocked \
      window-detector-unavailable \
      "xdotool is not available, so visible X windows cannot be enumerated safely." \
      "$display" \
      "$lifecycle_point" \
      "$alice_launch_pid" \
      xdotool \
      unavailable
    return 0
  fi

  DISPLAY="$display" WINDOW_INVENTORY_LIFECYCLE_POINT="$lifecycle_point" WINDOW_INVENTORY_ALICE_LAUNCH_PID="$alice_launch_pid" \
  python3 - "$run_dir/x-window-inventory.json" <<'PY'
import json
import os
import re
import subprocess
import sys
from pathlib import Path

output_path = Path(sys.argv[1])
display = os.environ.get("DISPLAY", "")
lifecycle_point = os.environ.get("WINDOW_INVENTORY_LIFECYCLE_POINT", "")
alice_launch_pid = os.environ.get("WINDOW_INVENTORY_ALICE_LAUNCH_PID", "")


def run(args):
    try:
        completed = subprocess.run(
            args,
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            env=os.environ.copy(),
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        return 127, "", str(exc)
    return completed.returncode, completed.stdout.strip(), completed.stderr.strip()


def command_output(args):
    code, stdout, _ = run(args)
    return stdout if code == 0 else ""


def process_name(pid):
    if not pid:
        return ""
    comm = Path("/proc") / str(pid) / "comm"
    try:
        return comm.read_text(encoding="utf-8").strip()
    except OSError:
        return ""


def parent_pid(pid):
    if not pid:
        return None
    stat = Path("/proc") / str(pid) / "stat"
    try:
        content = stat.read_text(encoding="utf-8")
    except OSError:
        return None
    try:
        return int(content.rsplit(")", 1)[1].split()[1])
    except (IndexError, ValueError):
        return None


def process_descends_from(pid, ancestor_pid):
    if not pid or not ancestor_pid:
        return False
    try:
        current = int(pid)
        ancestor = int(ancestor_pid)
    except ValueError:
        return False
    seen = set()
    while current > 1 and current not in seen:
        if current == ancestor:
            return True
        seen.add(current)
        next_pid = parent_pid(current)
        if next_pid is None:
            return False
        current = next_pid
    return False


def window_class_for(window_id):
    window_class = command_output(["xdotool", "getwindowclassname", window_id])
    if window_class:
        return window_class
    code, stdout, _ = run(["xprop", "-id", window_id, "WM_CLASS"])
    if code != 0:
        return ""
    values = re.findall(r'"([^"]*)"', stdout)
    if values:
        return values[-1]
    return ""


def geometry_for(window_id):
    stdout = command_output(["xdotool", "getwindowgeometry", "--shell", window_id])
    values = {}
    for line in stdout.splitlines():
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    return {
        "x": int(values["X"]) if values.get("X", "").lstrip("-").isdigit() else None,
        "y": int(values["Y"]) if values.get("Y", "").lstrip("-").isdigit() else None,
        "width": int(values["WIDTH"]) if values.get("WIDTH", "").isdigit() else None,
        "height": int(values["HEIGHT"]) if values.get("HEIGHT", "").isdigit() else None,
        "screen": int(values["SCREEN"]) if values.get("SCREEN", "").isdigit() else None,
    }


search_code, search_stdout, search_stderr = run(["xdotool", "search", "--onlyvisible", "--class", ".*"])
window_ids = []
if search_code == 0:
    seen = set()
    for line in search_stdout.splitlines():
        window_id = line.strip()
        if not window_id or window_id in seen:
            continue
        seen.add(window_id)
        window_ids.append(window_id)

windows = []
alice_candidate_count = 0
java_window_count = 0
for window_id in window_ids:
    title = command_output(["xdotool", "getwindowname", window_id])
    window_class = window_class_for(window_id)
    pid = command_output(["xdotool", "getwindowpid", window_id])
    proc_name = process_name(pid)
    reasons = []
    if "alice" in title.lower():
        reasons.append("title-contains-alice")
    if "alice" in window_class.lower():
        reasons.append("class-contains-alice")
    if "alice" in proc_name.lower():
        reasons.append("process-name-contains-alice")
    java_process = proc_name == "java"
    java_reasons = []
    if java_process:
        java_reasons.append("process-name-java")
    if process_descends_from(pid, alice_launch_pid):
        reasons.append("descends-from-alice-launch")
    known_alice_title = title in {
        "Alice 3",
        "Select Project",
        "Application Root Error",
        "License Agreement (Part 1 of 2): Alice 3",
        "License Agreement (Part 2 of 2): The Sims (TM) 2 Art Assets",
    }
    if known_alice_title:
        reasons.append("known-alice-window-title")
    if not reasons:
        continue
    alice_candidate_count += 1
    if java_process:
        java_window_count += 1
    windows.append(
        {
            "id": window_id,
            "title": title,
            "class": window_class,
            "pid": int(pid) if re.fullmatch(r"[0-9]+", pid or "") else None,
            "processName": proc_name,
            "geometry": geometry_for(window_id),
            "aliceCandidate": bool(reasons),
            "javaWindow": java_process,
            "candidateReasons": reasons,
            "javaWindowReasons": java_reasons,
        }
    )

if windows:
    status = "observed"
    blocker = "none"
    detail = f"Enumerated {len(windows)} Alice-related visible X window(s) on {display}; inspect windows[] for exact title/class/process/geometry."
else:
    status = "blocked"
    blocker = "no-alice-related-x-windows"
    detail = "xdotool did not identify any Alice-related visible X windows after the launch readiness wait."
    if search_stderr:
        detail = f"{detail} xdotool stderr: {search_stderr}"

payload = {
    "status": status,
    "blocker": blocker,
    "blockerDetail": detail,
    "display": display,
    "lifecyclePoint": lifecycle_point,
    "aliceLaunchPid": alice_launch_pid,
    "detectorStatus": "available",
    "detector": "xdotool",
    "aliceWindowCandidateCount": alice_candidate_count,
    "javaWindowCount": java_window_count,
    "windows": windows,
}

with output_path.open("w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

inventory_json_field() {
  local inventory_path=$1
  local field=$2
  python3 - "$inventory_path" "$field" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as stream:
    value = json.load(stream)
for part in sys.argv[2].split("."):
    value = value.get(part, "") if isinstance(value, dict) else ""
print(value)
PY
}

inventory_json_fields() {
  local inventory_path=$1
  shift
  python3 - "$inventory_path" "__alice_inventory_json_fields_end__" "$@" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as stream:
    root = json.load(stream)

for field in sys.argv[3:]:
    value = root
    for part in field.split("."):
        value = value.get(part, "") if isinstance(value, dict) else ""
    print(value)
print(sys.argv[2])
PY
}

read_inventory_json_fields() {
  local -n fields_ref=$1
  local inventory_path=$2
  shift 2
  local fields_output
  local sentinel=__alice_inventory_json_fields_end__

  fields_output=$(inventory_json_fields "$inventory_path" "$@")
  mapfile -t fields_ref <<< "$fields_output"
  local fields_count=${#fields_ref[@]}
  local sentinel_index=$((fields_count - 1))
  if [ "$fields_count" -eq 0 ] || [ "${fields_ref[$sentinel_index]}" != "$sentinel" ]; then
    printf 'Failed to read expected JSON fields from %s\n' "$inventory_path" >&2
    return 1
  fi
  unset "fields_ref[$sentinel_index]"
}

inventory_json_compact_field() {
  local inventory_path=$1
  local field=$2
  python3 - "$inventory_path" "$field" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as stream:
    value = json.load(stream)
for part in sys.argv[2].split("."):
    value = value.get(part, "") if isinstance(value, dict) else ""
if value == "":
    print("")
elif value is None:
    print("null")
else:
    print(json.dumps(value, sort_keys=True, separators=(",", ":")))
PY
}

write_application_root_error_probe() {
  local inventory_path=$1
  local output_path=$2

  python3 "$SCRIPT_DIR/application-root-error-probe.py" "$inventory_path" "$output_path"
}

write_license_dialog_probe() {
  local inventory_path=$1
  local output_path=$2

  python3 "$LICENSE_DIALOG_PROBE" "$inventory_path" "$output_path"
}

write_select_project_probe() {
  local inventory_path=$1
  local output_path=$2

  python3 "$SELECT_PROJECT_PROBE" "$inventory_path" "$output_path"
}

write_swing_widget_probe() {
  local inventory_path=$1
  local output_path=$2
  local python

  python=$(python_with_module pyatspi)
  "$python" "$SWING_WIDGET_PROBE" "$inventory_path" "$output_path"
}

write_tab_click_probe() {
  local inventory_path=$1
  local output_path=$2
  local python
  local target_display_name=${3:-}
  local target_repo_path=${4:-}

  python=$(python_with_module pyatspi)
  TARGET_STARTER_DISPLAY_NAME="$target_display_name" \
  TARGET_STARTER_REPO_PATH="$target_repo_path" \
  "$python" "$TAB_CLICK_PROBE" "$inventory_path" "$output_path"
}

write_post_project_open_probe() {
  local inventory_path=$1
  local tab_click_path=$2
  local output_path=$3
  local python

  python=$(python_with_module pyatspi)
  "$python" "$POST_PROJECT_OPEN_PROBE" "$inventory_path" "$tab_click_path" "$output_path"
}

write_post_open_runtime_display_blocker() {
  local run_dir=$1
  local scenario_id=$2
  local automation_mode=$3
  local blocker=$4
  local blocker_detail=$5
  local display=${6:-}
  local timeout_seconds=${7:-}

  write_visible_rendering_pixel_target_blocker \
    "$run_dir" \
    blocked \
    "$blocker" \
    "" \
    not-attempted \
    not-attempted
  write_visible_rendering_pixel_sampling_blocker \
    "$run_dir" \
    "$run_dir/controlled-display-pixel-observation.json"

  RUNTIME_DISPLAY_SCENARIO="$scenario_id" \
  RUNTIME_DISPLAY_AUTOMATION_MODE="$automation_mode" \
  RUNTIME_DISPLAY_BLOCKER="$blocker" \
  RUNTIME_DISPLAY_BLOCKER_DETAIL="$blocker_detail" \
  python3 - "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'PY'
import json
import os
import sys

payload = {
    "status": "blocked",
    "blocker": os.environ["RUNTIME_DISPLAY_BLOCKER"],
    "blockerDetail": os.environ["RUNTIME_DISPLAY_BLOCKER_DETAIL"],
    "claim": "post-open-runtime-display-accessibility-evidence",
    "scenario": os.environ["RUNTIME_DISPLAY_SCENARIO"],
    "automationMode": os.environ["RUNTIME_DISPLAY_AUTOMATION_MODE"],
    "javaPid": None,
    "postOpenWindowObserved": False,
    "postOpenRuntimeDisplayAccessibilityObserved": False,
    "runtimeDisplayCandidateCount": 0,
    "runtimeDisplayCandidates": [],
    "traversalErrors": [],
}
with open(sys.argv[1], "w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    if [ -n "$display" ]; then
      printf 'display=%s\n' "$display"
    fi
    printf 'outcome=blocked\n'
    printf 'runtimeDisplayAccessibilityEvidence=%s\n' "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
    printf 'runtimeDisplayAccessibilityStatus=blocked\n'
    printf 'runtimeDisplayAccessibilityBlocker=%s\n' "$blocker"
    printf 'controlledDisplayPixelStatus=blocked\n'
    printf 'controlledDisplayPixelBlocker=%s\n' "$blocker"
    printf 'visibleRenderingPixelTargetStatus=blocked\n'
    printf 'visibleRenderingPixelTargetArtifact=%s\n' "$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER"
    printf 'visibleRenderingPixelTargetBlocker=%s\n' "$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER"
    printf 'visibleRenderingPixelSamplingStatus=blocked\n'
    printf 'visibleRenderingPixelSamplingArtifact=%s\n' "$VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER"
    printf 'visibleRenderingPixelSamplingBlocker=world-canvas-pixel-target-not-ready\n'
    printf 'visibleRenderingCorrectnessEstablished=false\n'
    if [ -n "$timeout_seconds" ]; then
      printf 'timeoutSeconds=%s\n' "$timeout_seconds"
    fi
  } > "$run_dir/status.txt"
}

write_post_open_runtime_display_probe() {
  local inventory_path=$1
  local post_open_path=$2
  local output_path=$3
  local status_path=$4
  local scenario_id=$5
  local automation_mode=$6
  local python

  python=$(python_with_module pyatspi)
  "$python" "$POST_OPEN_RUNTIME_DISPLAY_PROBE" \
    --inventory "$inventory_path" \
    --post-open-window-observation "$post_open_path" \
    --output "$output_path" \
    --status-file "$status_path" \
    --scenario-id "$scenario_id" \
    --automation-mode "$automation_mode"
}

write_first_lesson_procedure_target_blocker() {
  local run_dir=$1
  local scenario_id=$2
  local automation_mode=$3
  local blocker=$4
  local blocker_detail=$5
  local target_display_name=${6:-}
  local target_repo_path=${7:-}
  local opened_via_select_project=${8:-false}
  local post_open_window_observed=${9:-false}

  FIRST_LESSON_PROCEDURE_TARGET_SCENARIO_ID="$scenario_id" \
  FIRST_LESSON_PROCEDURE_TARGET_AUTOMATION_MODE="$automation_mode" \
  FIRST_LESSON_PROCEDURE_TARGET_BLOCKER="$blocker" \
  FIRST_LESSON_PROCEDURE_TARGET_BLOCKER_DETAIL="$blocker_detail" \
  FIRST_LESSON_PROCEDURE_TARGET_DISPLAY_NAME="$target_display_name" \
  FIRST_LESSON_PROCEDURE_TARGET_REPO_PATH="$target_repo_path" \
  FIRST_LESSON_PROCEDURE_TARGET_OPENED="$opened_via_select_project" \
  FIRST_LESSON_PROCEDURE_TARGET_POST_OPEN="$post_open_window_observed" \
  FIRST_LESSON_PROCEDURE_TARGET_WORKFLOW="first-lesson-live-procedure-target-observation" \
  FIRST_LESSON_PROCEDURE_TARGET_SEAM="live-first-lesson-procedure-target-to-desktop-edit-action" \
  FIRST_LESSON_PROCEDURE_SELECTOR="$FIRST_LESSON_PROCEDURE_SELECTOR" \
  python3 - "$run_dir/$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT" <<'PY'
import json
import os
import sys
from pathlib import Path

output_path = Path(sys.argv[1])
if output_path.exists() and output_path.is_symlink():
    print(f"refusing to overwrite symlink artifact: {output_path}", file=sys.stderr)
    sys.exit(2)

OUT_OF_SCOPE = [
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
    "creative assessment",
    "full first-lesson completion",
]
RUN_FAILURE_BLOCKER_MESSAGES = {
    "select-project-open-not-observed": "Select Project did not open the configured first-lesson starter",
    "post-open-window-not-observed": "post-open Alice main window was not observed",
    "procedure-target-not-found": "scene.eatmeFirstLesson procedure/code-editor target was not found",
    "procedure-target-not-stable": "scene.eatmeFirstLesson target was not reacquirable through a stable automation path",
    "at-spi-or-atk-unavailable": "AT-SPI/ATK accessibility infrastructure was unavailable",
    "display-prerequisite-unavailable": "Xvfb display prerequisite was unavailable",
}
blocker_kind = os.environ["FIRST_LESSON_PROCEDURE_TARGET_BLOCKER"]
blocker = {
    "kind": blocker_kind,
    "message": RUN_FAILURE_BLOCKER_MESSAGES.get(
        blocker_kind,
        os.environ["FIRST_LESSON_PROCEDURE_TARGET_BLOCKER_DETAIL"],
    ),
}
payload = {
    "schemaVersion": "eatme.first-lesson-live-procedure-target-observation/v1",
    "scenario": os.environ["FIRST_LESSON_PROCEDURE_TARGET_SCENARIO_ID"],
    "workflow": os.environ["FIRST_LESSON_PROCEDURE_TARGET_WORKFLOW"],
    "automationMode": os.environ["FIRST_LESSON_PROCEDURE_TARGET_AUTOMATION_MODE"],
    "status": "blocked",
    "seam": os.environ["FIRST_LESSON_PROCEDURE_TARGET_SEAM"],
    "project": {
        "targetStarterDisplayName": os.environ.get("FIRST_LESSON_PROCEDURE_TARGET_DISPLAY_NAME", ""),
        "targetStarterRepositoryPath": os.environ.get("FIRST_LESSON_PROCEDURE_TARGET_REPO_PATH", ""),
        "openedViaSelectProject": os.environ.get("FIRST_LESSON_PROCEDURE_TARGET_OPENED") == "true",
        "postOpenWindowObserved": os.environ.get("FIRST_LESSON_PROCEDURE_TARGET_POST_OPEN") == "true",
    },
    "requiredTarget": {
        "procedureName": os.environ["FIRST_LESSON_PROCEDURE_SELECTOR"],
        "kind": "procedure-or-code-editor-target",
        "minimumStableAutomationTarget": "reacquirable live desktop procedure tab or code-editor target",
    },
    "observedTarget": None,
    "desktopEditAction": {
        "status": "blocked",
        "readyForDesktopEditAction": False,
        "targetSelector": os.environ["FIRST_LESSON_PROCEDURE_SELECTOR"],
        "invocationContract": None,
        "blocker": blocker,
        "doesNotClaim": OUT_OF_SCOPE,
    },
    "blocker": blocker,
    "blockerDetail": os.environ["FIRST_LESSON_PROCEDURE_TARGET_BLOCKER_DETAIL"],
    "downstreamBlockedStep": "desktop-procedure-edit-action-proof",
    "outOfScope": OUT_OF_SCOPE,
}

output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
PY
}

write_first_lesson_procedure_target_status() {
  local run_dir=$1
  local scenario_id=$2
  local automation_mode=$3
  local outcome=$4
  local status=$5
  local blocker=$6
  local display=${7:-}
  local timeout_seconds=${8:-}

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    if [ -n "$display" ]; then
      printf 'display=%s\n' "$display"
    fi
    printf 'outcome=%s\n' "$outcome"
    printf 'procedureTargetObservationEvidence=%s\n' "$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT"
    printf 'procedureTargetObservationStatus=%s\n' "$status"
    printf 'procedureTargetObservationBlocker=%s\n' "$blocker"
    printf 'downstreamBlockedStep=desktop-procedure-edit-action-proof\n'
    if [ -n "$timeout_seconds" ]; then
      printf 'timeoutSeconds=%s\n' "$timeout_seconds"
    fi
  } > "$run_dir/status.txt"
}

write_first_lesson_procedure_target_probe() {
  local inventory_path=$1
  local tab_click_path=$2
  local post_open_path=$3
  local output_path=$4
  local scenario_id=$5
  local automation_mode=$6
  local target_display_name=$7
  local target_repo_path=$8
  local python

  python=$(python_with_module pyatspi)
  "$python" "$FIRST_LESSON_PROCEDURE_TARGET_PROBE" \
    --inventory "$inventory_path" \
    --tab-click-observation "$tab_click_path" \
    --post-open-window-observation "$post_open_path" \
    --output "$output_path" \
    --scenario-id "$scenario_id" \
    --automation-mode "$automation_mode" \
    --target-starter-display-name "$target_display_name" \
    --target-starter-repository-path "$target_repo_path" \
    --procedure-selector "$FIRST_LESSON_PROCEDURE_SELECTOR"
}

select_display() {
  if [ -n "${ALICE_QA_DISPLAY:-}" ]; then
    printf '%s\n' "$ALICE_QA_DISPLAY"
    return 0
  fi

  local number
  for number in {90..120}; do
    if ! DISPLAY=":$number" xdpyinfo >/dev/null 2>&1; then
      printf ':%s\n' "$number"
      return 0
    fi
  done

  return 1
}

capture_screenshot() {
  local output=$1
  if command -v import >/dev/null 2>&1; then
    import -window root "$output"
  elif command -v gnome-screenshot >/dev/null 2>&1; then
    gnome-screenshot --file "$output"
  elif command -v xwd >/dev/null 2>&1; then
    xwd -root -silent -out "${output%.png}.xwd"
  else
    return 3
  fi
}

screenshot_tool_name() {
  if command -v import >/dev/null 2>&1; then
    printf 'import\n'
  elif command -v gnome-screenshot >/dev/null 2>&1; then
    printf 'gnome-screenshot\n'
  elif command -v xwd >/dev/null 2>&1; then
    printf 'xwd\n'
  else
    printf 'none\n'
  fi
}

analyze_screenshot_pixels() {
  local image_path=$1
  local output_path=$2
  if ! command -v identify >/dev/null 2>&1; then
    {
      printf 'status=analysis-unavailable\n'
      printf 'detail=ImageMagick identify is not available; screenshot pixels cannot be classified.\n'
    } > "$output_path"
    return 0
  fi

  if ! identify -quiet \
      -format 'width=%w\nheight=%h\nminima=%[fx:minima]\nmaxima=%[fx:maxima]\nmean=%[fx:mean]\ncolors=%k\n' \
      "$image_path" > "$output_path.raw" 2>"$output_path.err"; then
    {
      printf 'status=analysis-failed\n'
      printf 'detail=ImageMagick identify could not inspect the screenshot; see screenshot-pixels.txt.err.\n'
    } > "$output_path"
    return 0
  fi

  local maxima colors width height
  maxima=$(sed -n 's/^maxima=//p' "$output_path.raw")
  colors=$(sed -n 's/^colors=//p' "$output_path.raw")
  width=$(sed -n 's/^width=//p' "$output_path.raw")
  height=$(sed -n 's/^height=//p' "$output_path.raw")
  if [[ ! "$maxima" =~ ^[0-9]+([.][0-9]+)?([eE][-+]?[0-9]+)?$ ]]; then
    {
      printf 'status=analysis-failed\n'
      printf 'detail=ImageMagick identify did not report a numeric maxima value; screenshot pixels cannot be classified safely.\n'
      cat "$output_path.raw"
    } > "$output_path"
    return 0
  fi
  if awk "BEGIN { exit !($maxima == 0) }"; then
    {
      printf 'status=uniform-black\n'
      printf 'detail=Screenshot is %sx%s with %s color(s), but every sampled pixel is black.\n' "$width" "$height" "$colors"
      cat "$output_path.raw"
    } > "$output_path"
    return 0
  fi

  {
    printf 'status=non-black-pixels\n'
    printf 'detail=Screenshot is %sx%s with non-black pixel data.\n' "$width" "$height"
    cat "$output_path.raw"
  } > "$output_path"
}

run_xvfb_real_alice() {
  local scenario_json=$1
  local run_dir=$2
  local timeout_override=$3

  local automation_fields cwd configured_timeout ready_wait run_timeout display scenario_id automation_mode resolved_cwd
  local target_starter_display_name target_starter_repo_path
  local -a target_fields
  local needs_select_project_wait=0 needs_tab_click_probe=0
  local xvfb_executable xdotool_executable
  local root_directory_prep_status root_directory_prep_blocker
  local -a argv
  mapfile -t automation_fields < <(json_fields "$scenario_json" "automation.cwd" "automation.timeoutSeconds" "automation.readyWaitSeconds" "id" "automationMode")
  cwd=${automation_fields[0]}
  configured_timeout=${automation_fields[1]}
  ready_wait="${ALICE_QA_READY_WAIT_SECONDS:-${automation_fields[2]}}"
  scenario_id=${automation_fields[3]}
  automation_mode=${automation_fields[4]}
  mapfile -d '' -t argv < <(json_list_nul "$scenario_json" "automation.argv")
  case "$scenario_id" in
    alice-desktop-select-project-*|alice-desktop-post-project-open-window-state|"$POST_OPEN_RUNTIME_DISPLAY_SCENARIO"|"$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO")
      needs_select_project_wait=1
      ;;
  esac
  case "$scenario_id" in
    alice-desktop-select-project-tab-click-exec|alice-desktop-post-project-open-window-state|"$POST_OPEN_RUNTIME_DISPLAY_SCENARIO"|"$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO")
      needs_tab_click_probe=1
      mapfile -t target_fields < <(target_starter_fields "$scenario_json")
      target_starter_display_name=${target_fields[0]:?}
      target_starter_repo_path=${target_fields[1]:?}
      ;;
    *)
      target_starter_display_name=
      target_starter_repo_path=
      ;;
  esac
  run_timeout="${timeout_override:-$configured_timeout}"
  xvfb_pid=
  alice_pid=
  xvfb_executable=$(command -v Xvfb 2>/dev/null || true)
  xdotool_executable=$(command -v xdotool 2>/dev/null || true)
  if [ "${ALICE_QA_DISABLE_XVFB:-}" = "1" ]; then
    xvfb_executable=
  fi

  validate_allowed_automation "$cwd" "${argv[@]}"
  resolved_cwd=$(resolve_automation_cwd "$cwd")

  if [ -z "$xvfb_executable" ]; then
    write_environment "$run_dir"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      x-server-unavailable \
      "Xvfb executable is not available on PATH; no X server exists for window enumeration." \
      "" \
      before-x-server-start \
      "" \
      Xvfb \
      not-started
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      x-server-unavailable \
      "Xvfb executable is not available on PATH; install Xvfb before retrying this controlled-display runner." \
      "" \
      false \
      no-visible-pixel-proof \
      Xvfb \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "" \
      "" \
      not-attempted \
      "" \
      before-x-server-start \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        x-server-unavailable \
        "Xvfb executable is not available on PATH; no X server exists for post-open runtime/display accessibility evidence." \
        "" \
        "$run_timeout"
    fi
    if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
      write_first_lesson_procedure_target_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        display-prerequisite-unavailable \
        "Xvfb executable is not available on PATH; no live desktop display exists for first-lesson procedure target observation." \
        "$target_starter_display_name" \
        "$target_starter_repo_path" \
        false \
        false
      write_first_lesson_procedure_target_status \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        blocked \
        blocked \
        display-prerequisite-unavailable \
        "" \
        "$run_timeout"
    fi
    printf 'Xvfb is not available; wrote manual fallback checklist to %s\n' "$run_dir" >&2
    return 2
  fi

  if ! display=$(select_display); then
    write_environment "$run_dir"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      display-allocation-unavailable \
      "No free X display could be selected; no display exists for window enumeration." \
      "" \
      before-x-server-start \
      "" \
      "" \
      not-started
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      display-allocation-unavailable \
      "No free X display could be selected; set ALICE_QA_DISPLAY to a reachable display and retry." \
      "" \
      false \
      no-visible-pixel-proof \
      "" \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "$xvfb_executable" \
      "" \
      not-attempted \
      "" \
      before-x-server-start \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        display-allocation-unavailable \
        "No free X display could be selected; no display exists for post-open runtime/display accessibility evidence." \
        "" \
        "$run_timeout"
    fi
    if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
      write_first_lesson_procedure_target_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        display-prerequisite-unavailable \
        "No free X display could be selected; no live desktop display exists for first-lesson procedure target observation." \
        "$target_starter_display_name" \
        "$target_starter_repo_path" \
        false \
        false
      write_first_lesson_procedure_target_status \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        blocked \
        blocked \
        display-prerequisite-unavailable \
        "" \
        "$run_timeout"
    fi
    printf 'No free X display found; wrote manual fallback checklist to %s\n' "$run_dir" >&2
    return 2
  fi

  if ! python3 "$ROOT_DIRECTORY_PREP" \
      --repo-root "$REPO_ROOT" \
      --alice-cwd "$cwd" \
      --output "$run_dir/root-directory-prep.json" \
      --log "$run_dir/root-directory-prep.log"; then
    write_environment "$run_dir" "$display"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    if [ -f "$run_dir/root-directory-prep.json" ]; then
      local -a failed_root_directory_prep_fields
      read_inventory_json_fields failed_root_directory_prep_fields "$run_dir/root-directory-prep.json" status blocker
      root_directory_prep_status=${failed_root_directory_prep_fields[0]}
      root_directory_prep_blocker=${failed_root_directory_prep_fields[1]}
    else
      root_directory_prep_status=blocked
      root_directory_prep_blocker=root-directory-prep-script-failed
      cat > "$run_dir/root-directory-prep.json" <<'JSON'
{
  "blocker": "root-directory-prep-script-failed",
  "blockerDetail": "prepare-root-directory.py failed before writing root-directory-prep.json; inspect the runner stderr/stdout and retry after fixing the helper invocation.",
  "configuredRootDirectory": "",
  "distributionExists": false,
  "expectedRootDirectory": "../core/resources/target/distribution",
  "mavenPhase": "process-resources",
  "mavenProject": "core/resources",
  "prepAttempted": false,
  "resolvedRootDirectory": "",
  "rootDirectoryProperty": "org.alice.ide.rootDirectory",
  "status": "blocked"
}
JSON
    fi
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      "$root_directory_prep_blocker" \
      "Alice rootDirectory launch preparation failed before Xvfb start; inspect root-directory-prep.json and root-directory-prep.log." \
      "$display" \
      before-x-server-start \
      "" \
      "" \
      not-started
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      "$root_directory_prep_blocker" \
      "Alice rootDirectory launch preparation failed before Xvfb start; inspect root-directory-prep.json for the exact property, Maven phase, and distribution path." \
      "$display" \
      false \
      no-visible-pixel-proof \
      "" \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "$xvfb_executable" \
      "" \
      not-attempted \
      "" \
      before-x-server-start \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        "$root_directory_prep_blocker" \
        "Alice rootDirectory launch preparation failed before post-open runtime/display accessibility evidence could be collected."
    fi
    if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
      write_first_lesson_procedure_target_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        display-prerequisite-unavailable \
        "Alice rootDirectory launch preparation failed before first-lesson procedure target observation could be collected." \
        "$target_starter_display_name" \
        "$target_starter_repo_path" \
        false \
        false
    fi
    {
      printf 'scenario=%s\n' "$scenario_id"
      printf 'automationMode=%s\n' "$automation_mode"
      printf 'display=%s\n' "$display"
      printf 'rootDirectoryPrep=%s\n' root-directory-prep.json
      printf 'rootDirectoryPrepStatus=%s\n' "$root_directory_prep_status"
      printf 'rootDirectoryPrepBlocker=%s\n' "$root_directory_prep_blocker"
      printf 'readyStatus=not-attempted\n'
      printf 'processStatus=not-started\n'
      printf 'screenshotStatus=not-attempted\n'
      printf 'windowInventory=%s\n' x-window-inventory.json
      printf 'applicationRootError=not-attempted\n'
      if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
        printf 'outcome=blocked\n'
        printf 'runtimeDisplayAccessibilityEvidence=%s\n' "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
        printf 'runtimeDisplayAccessibilityStatus=blocked\n'
        printf 'runtimeDisplayAccessibilityBlocker=%s\n' "$root_directory_prep_blocker"
        printf 'controlledDisplayPixelStatus=blocked\n'
        printf 'controlledDisplayPixelBlocker=%s\n' "$root_directory_prep_blocker"
        printf 'visibleRenderingPixelTargetStatus=blocked\n'
        printf 'visibleRenderingPixelTargetArtifact=%s\n' "$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER"
        printf 'visibleRenderingPixelTargetBlocker=%s\n' "$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER"
      fi
      if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
        printf 'outcome=blocked\n'
        printf 'procedureTargetObservationEvidence=%s\n' "$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT"
        printf 'procedureTargetObservationStatus=blocked\n'
        printf 'procedureTargetObservationBlocker=display-prerequisite-unavailable\n'
        printf 'downstreamBlockedStep=desktop-procedure-edit-action-proof\n'
      fi
      printf 'timeoutSeconds=%s\n' "$run_timeout"
    } > "$run_dir/status.txt"
    printf 'Alice rootDirectory launch preparation blocked: %s; see %s/root-directory-prep.json\n' "$root_directory_prep_blocker" "$run_dir" >&2
    return 2
  fi
  local -a root_directory_prep_fields
  read_inventory_json_fields root_directory_prep_fields "$run_dir/root-directory-prep.json" status blocker
  root_directory_prep_status=${root_directory_prep_fields[0]}
  root_directory_prep_blocker=${root_directory_prep_fields[1]}

  local license_acceptance_status license_acceptance_blocker license_prefs_user_root license_jvm_option
  license_prefs_user_root="$(cd "$run_dir" && pwd)/java-user-prefs"
  license_jvm_option="-Djava.util.prefs.userRoot=$license_prefs_user_root"
  if [ "${ALICE_QA_ACCEPT_LICENSES_FOR_TESTS:-}" = "1" ]; then
    if ! python3 "$LICENSE_ACCEPTANCE_PREP" \
        --user-root "$license_prefs_user_root" \
        --output "$run_dir/license-acceptance.json" \
        --accept-for-tests; then
      write_environment "$run_dir" "$display"
      write_checklist "$scenario_json" "$run_dir" >/dev/null
      write_x_window_inventory \
        "$run_dir" \
        not-attempted \
        license-acceptance-prep-failed \
        "Alice first-run license acceptance prep failed before launch; inspect license-acceptance.json." \
        "$display" \
        before-alice-launch \
        "" \
        "" \
        not-started
      write_controlled_display_pixel_observation \
        "$run_dir" \
        blocked \
        license-acceptance-prep-failed \
        "Alice first-run license acceptance prep failed before launch; inspect license-acceptance.json." \
        "$display" \
        false \
        no-visible-pixel-proof \
        "" \
        not-attempted \
        not-started \
        not-attempted \
        "" \
        "$xvfb_executable" \
        "" \
        not-attempted \
        "" \
        before-alice-launch \
        not-attempted \
        x-window-inventory.json \
        0
      if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
        write_post_open_runtime_display_blocker \
          "$run_dir" \
          "$scenario_id" \
          "$automation_mode" \
          license-acceptance-prep-failed \
          "Alice first-run license acceptance prep failed before post-open runtime/display accessibility evidence could be collected." \
          "$display" \
          "$run_timeout"
      fi
      if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
        write_first_lesson_procedure_target_blocker \
          "$run_dir" \
          "$scenario_id" \
          "$automation_mode" \
          display-prerequisite-unavailable \
          "Alice first-run license acceptance prep failed before first-lesson procedure target observation could be collected." \
          "$target_starter_display_name" \
          "$target_starter_repo_path" \
          false \
          false
        write_first_lesson_procedure_target_status \
          "$run_dir" \
          "$scenario_id" \
          "$automation_mode" \
          blocked \
          blocked \
          display-prerequisite-unavailable \
          "$display" \
          "$run_timeout"
      fi
      printf 'Alice license acceptance prep failed; see %s/license-acceptance.json\n' "$run_dir" >&2
      return 2
    fi
  else
    python3 "$LICENSE_ACCEPTANCE_PREP" \
      --user-root "$license_prefs_user_root" \
      --output "$run_dir/license-acceptance.json" >/dev/null 2>&1 || true
    license_jvm_option=
  fi
  local -a license_acceptance_fields
  read_inventory_json_fields license_acceptance_fields "$run_dir/license-acceptance.json" status blocker
  license_acceptance_status=${license_acceptance_fields[0]}
  license_acceptance_blocker=${license_acceptance_fields[1]}

  Xvfb "$display" -screen 0 "${ALICE_QA_SCREEN:-1280x900x24}" > "$run_dir/xvfb.log" 2>&1 &
  xvfb_pid=$!
  alice_pid=

  cleanup() {
    if [ -n "${alice_pid:-}" ] && kill -0 "$alice_pid" >/dev/null 2>&1; then
      if ! kill "$alice_pid" >/dev/null 2>&1; then
        printf 'warning: failed to stop Alice launch process %s\n' "$alice_pid" >&2
      fi
      if ! wait "$alice_pid" >/dev/null 2>&1; then
        :
      fi
    fi
    if kill -0 "$xvfb_pid" >/dev/null 2>&1; then
      if ! kill "$xvfb_pid" >/dev/null 2>&1; then
        printf 'warning: failed to stop Xvfb process %s\n' "$xvfb_pid" >&2
      fi
      if ! wait "$xvfb_pid" >/dev/null 2>&1; then
        :
      fi
    fi
  }
  trap cleanup EXIT

  sleep 2
  if ! kill -0 "$xvfb_pid" >/dev/null 2>&1; then
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      x-server-start-failed \
      "Xvfb exited before Alice launch; no running X server exists for window enumeration." \
      "$display" \
      before-alice-launch \
      "" \
      "" \
      not-started
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      x-server-start-failed \
      "Xvfb exited before Alice launch; inspect xvfb.log for display permissions, screen geometry, or backend startup errors." \
      "$display" \
      false \
      no-visible-pixel-proof \
      "" \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "$xvfb_executable" \
      "" \
      not-attempted \
      "" \
      before-alice-launch \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        x-server-start-failed \
        "Xvfb exited before Alice launch; post-open runtime/display accessibility evidence could not be collected." \
        "$display" \
        "$run_timeout"
    fi
    if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
      write_first_lesson_procedure_target_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        display-prerequisite-unavailable \
        "Xvfb exited before Alice launch; first-lesson procedure target observation could not be collected." \
        "$target_starter_display_name" \
        "$target_starter_repo_path" \
        false \
        false
      write_first_lesson_procedure_target_status \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        blocked \
        blocked \
        display-prerequisite-unavailable \
        "$display" \
        "$run_timeout"
    fi
    printf 'Xvfb exited before Alice launch; see %s/xvfb.log\n' "$run_dir" >&2
    return 2
  fi

  export DISPLAY=$display
  write_environment "$run_dir" "$display"

  (
    cd "$resolved_cwd"
    if [ -n "$license_jvm_option" ]; then
      if [ -n "${MAVEN_OPTS:-}" ]; then
        export MAVEN_OPTS="${MAVEN_OPTS} ${license_jvm_option}"
      else
        export MAVEN_OPTS="$license_jvm_option"
      fi
      if [ -n "${JAVA_TOOL_OPTIONS:-}" ]; then
        export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} ${license_jvm_option}"
      else
        export JAVA_TOOL_OPTIONS="$license_jvm_option"
      fi
    fi
    timeout --foreground -k 10s "${run_timeout}s" "${argv[@]}" < /dev/null
  ) > "$run_dir/launch.log" 2>&1 &
  alice_pid=$!

  local ready_status=not-checked
  local waited=0
  if [ "${ALICE_QA_DISABLE_WINDOW_DETECTOR:-}" != "1" ] && [ -n "$xdotool_executable" ]; then
    ready_status=not-found
    while [ "$waited" -lt "$ready_wait" ]; do
      if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
        ready_status=process-exited
        break
      fi
      if "$xdotool_executable" search --onlyvisible --name Alice >/dev/null 2>&1 ||
          "$xdotool_executable" search --onlyvisible --class Alice >/dev/null 2>&1 ||
          "$xdotool_executable" search --onlyvisible --class alice >/dev/null 2>&1; then
        ready_status=alice-window-found
        break
      fi
      sleep 1
      waited=$((waited + 1))
    done
    if [ "$ready_status" = not-found ] &&
        kill -0 "$alice_pid" >/dev/null 2>&1 &&
        "$xdotool_executable" search --onlyvisible --class ".*" >/dev/null 2>&1; then
      ready_status=non-alice-visible-window-found
    fi
  else
    sleep "$ready_wait"
    ready_status=window-detector-unavailable
  fi

  local select_project_wait_status=not-requested
  if [ "$needs_select_project_wait" -eq 1 ]; then
    if [ "${ALICE_QA_DISABLE_WINDOW_DETECTOR:-}" != "1" ] && [ -n "$xdotool_executable" ]; then
      select_project_wait_status=not-found
      local select_waited=0
      while [ "$select_waited" -lt "$ready_wait" ]; do
        if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
          select_project_wait_status=process-exited
          break
        fi
        if "$xdotool_executable" search --onlyvisible --name '^Select Project$' >/dev/null 2>&1; then
          select_project_wait_status=select-project-window-found
          break
        fi
        sleep 1
        select_waited=$((select_waited + 1))
      done
    else
      select_project_wait_status=window-detector-unavailable
    fi
  fi

  collect_x_window_inventory "$run_dir" "$display" after-readiness-wait "$alice_pid"
  write_application_root_error_probe "$run_dir/x-window-inventory.json" "$run_dir/application-root-error.json"
  write_license_dialog_probe "$run_dir/x-window-inventory.json" "$run_dir/license-dialog.json"
  write_select_project_probe "$run_dir/x-window-inventory.json" "$run_dir/select-project-window.json"
  local swing_widget_status=not-requested swing_widget_blocker=not-requested
  if [ "$scenario_id" = alice-desktop-select-project-widget-introspection ] || \
     [ "$scenario_id" = alice-desktop-select-project-atk-exec ]; then
    # Allow the Swing accessibility tree to build before probing.
    sleep 3
    write_swing_widget_probe "$run_dir/x-window-inventory.json" "$run_dir/swing-widget-observation.json"
    local -a swing_widget_fields
    read_inventory_json_fields swing_widget_fields "$run_dir/swing-widget-observation.json" status blocker
    swing_widget_status=${swing_widget_fields[0]}
    swing_widget_blocker=${swing_widget_fields[1]}
  fi
  local tab_click_status=not-requested tab_click_blocker=not-requested
  local select_project_evidence_status=not-requested
  local select_project_target_display_name=not-requested
  local select_project_target_repo_path=not-requested
  local select_project_opened_display_name=not-requested
  local select_project_opened_repo_path=not-requested
  local select_project_project_open_observed=not-requested
  local select_project_target_selection_observed=not-requested
  local select_project_open_attempted=not-requested
  local select_project_next_blocker=not-requested
  local select_project_window_context=not-requested
  local select_project_alice_java_pid=not-requested
  local select_project_starters_tab_safety=not-requested
  local select_project_target_metadata_echo_valid=not-requested
  if [ "$needs_tab_click_probe" -eq 1 ]; then
    # Allow the Swing accessibility tree to build before probing, then run
    # the target-specific Select Project starter proof.
    sleep 3
    write_tab_click_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/tab-click-observation.json" \
      "$target_starter_display_name" \
      "$target_starter_repo_path"
    local -a tab_click_fields
    read_inventory_json_fields tab_click_fields \
      "$run_dir/tab-click-observation.json" \
      status \
      blocker \
      evidenceStatus \
      targetStarter.displayName \
      targetStarter.repositoryPath \
      openedStarter.displayName \
      openedStarter.repositoryPath \
      projectOpenObserved \
      targetSelectionObserved \
      openAttempted \
      javaPid
    tab_click_status=${tab_click_fields[0]}
    tab_click_blocker=${tab_click_fields[1]}
    if [ "${tab_click_fields[3]}" = "$target_starter_display_name" ] \
        && [ "${tab_click_fields[4]}" = "$target_starter_repo_path" ]; then
      select_project_target_metadata_echo_valid=true
      select_project_evidence_status=${tab_click_fields[2]}
      select_project_target_display_name=${tab_click_fields[3]}
      select_project_target_repo_path=${tab_click_fields[4]}
      select_project_opened_display_name=${tab_click_fields[5]}
      select_project_opened_repo_path=${tab_click_fields[6]}
      select_project_project_open_observed=${tab_click_fields[7]}
      select_project_target_selection_observed=${tab_click_fields[8]}
      select_project_open_attempted=${tab_click_fields[9]}
      select_project_next_blocker=$(inventory_json_compact_field "$run_dir/tab-click-observation.json" nextBlocker)
      select_project_window_context=$(inventory_json_compact_field "$run_dir/tab-click-observation.json" selectProjectWindowContext)
      select_project_alice_java_pid=${tab_click_fields[10]}
      select_project_starters_tab_safety=$(inventory_json_compact_field "$run_dir/tab-click-observation.json" startersTabSafety)
    else
      select_project_target_metadata_echo_valid=false
      select_project_evidence_status=target-metadata-echo-mismatch
      select_project_next_blocker='{"actionAttempted":"Promote Select Project tab-click evidence.","expectedNextAction":"Rerun tab-click probe with validated Africa Full targetStarter metadata.","observedAtspiState":"tab-click-observation.json did not echo the validated targetStarter metadata.","reasonProgressStopped":"Runner refused to promote target-specific evidence from mismatched or missing targetStarter metadata."}'
    fi
  fi
  local post_open_status=not-requested post_open_blocker=not-requested
  if [ "$scenario_id" = alice-desktop-post-project-open-window-state ] \
      || [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ] \
      || [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
    write_post_project_open_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/tab-click-observation.json" \
      "$run_dir/post-project-open-observation.json"
    local -a post_open_fields
    read_inventory_json_fields post_open_fields "$run_dir/post-project-open-observation.json" status blocker
    post_open_status=${post_open_fields[0]}
    post_open_blocker=${post_open_fields[1]}
  fi
  local procedure_target_status=not-requested procedure_target_blocker=not-requested
  if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
    write_first_lesson_procedure_target_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/tab-click-observation.json" \
      "$run_dir/post-project-open-observation.json" \
      "$run_dir/$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT" \
      "$scenario_id" \
      "$automation_mode" \
      "$target_starter_display_name" \
      "$target_starter_repo_path"
    local -a procedure_target_fields
    read_inventory_json_fields procedure_target_fields "$run_dir/$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT" status blocker.kind
    procedure_target_status=${procedure_target_fields[0]}
    procedure_target_blocker=${procedure_target_fields[1]}
  fi
  local runtime_display_status=not-requested runtime_display_blocker=not-requested
  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
    write_post_open_runtime_display_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/post-project-open-observation.json" \
      "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" \
      "$run_dir/runtime-display-accessibility-status.txt" \
      "$scenario_id" \
      "$automation_mode"
    local -a runtime_display_fields
    read_inventory_json_fields runtime_display_fields "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" status blocker
    runtime_display_status=${runtime_display_fields[0]}
    runtime_display_blocker=${runtime_display_fields[1]}
  fi
  local window_inventory_status alice_window_candidate_count application_root_error_status application_root_error_blocker license_dialog_status license_dialog_blocker select_project_status select_project_blocker select_project_interaction
  local -a window_inventory_fields application_root_error_fields license_dialog_fields select_project_fields
  read_inventory_json_fields window_inventory_fields "$run_dir/x-window-inventory.json" status aliceWindowCandidateCount
  read_inventory_json_fields application_root_error_fields "$run_dir/application-root-error.json" status blocker
  read_inventory_json_fields license_dialog_fields "$run_dir/license-dialog.json" status blocker
  read_inventory_json_fields select_project_fields "$run_dir/select-project-window.json" status blocker interactionProof
  window_inventory_status=${window_inventory_fields[0]}
  alice_window_candidate_count=${window_inventory_fields[1]}
  application_root_error_status=${application_root_error_fields[0]}
  application_root_error_blocker=${application_root_error_fields[1]}
  license_dialog_status=${license_dialog_fields[0]}
  license_dialog_blocker=${license_dialog_fields[1]}
  select_project_status=${select_project_fields[0]}
  select_project_blocker=${select_project_fields[1]}
  select_project_interaction=${select_project_fields[2]}

  local screenshot_tool screenshot_status
  screenshot_tool=$(screenshot_tool_name)
  screenshot_status=screenshot-captured
  if ! capture_screenshot "$run_dir/screenshot.png" > "$run_dir/screenshot.log" 2>&1; then
    screenshot_status=screenshot-failed
    write_checklist "$scenario_json" "$run_dir" >/dev/null
  fi

  local process_status=running
  if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
    process_status=exited-before-capture
  fi

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    printf 'display=%s\n' "$display"
    printf 'readyStatus=%s\n' "$ready_status"
    printf 'processStatus=%s\n' "$process_status"
    printf 'screenshotStatus=%s\n' "$screenshot_status"
    printf 'rootDirectoryPrep=%s\n' root-directory-prep.json
    printf 'rootDirectoryPrepStatus=%s\n' "$root_directory_prep_status"
    printf 'rootDirectoryPrepBlocker=%s\n' "$root_directory_prep_blocker"
    printf 'licenseAcceptance=%s\n' license-acceptance.json
    printf 'licenseAcceptanceStatus=%s\n' "$license_acceptance_status"
    printf 'licenseAcceptanceBlocker=%s\n' "$license_acceptance_blocker"
    printf 'windowInventory=%s\n' x-window-inventory.json
    printf 'windowInventoryStatus=%s\n' "$window_inventory_status"
    printf 'aliceWindowCandidateCount=%s\n' "$alice_window_candidate_count"
    printf 'applicationRootError=%s\n' application-root-error.json
    printf 'applicationRootErrorStatus=%s\n' "$application_root_error_status"
    printf 'applicationRootErrorBlocker=%s\n' "$application_root_error_blocker"
    printf 'licenseDialog=%s\n' license-dialog.json
    printf 'licenseDialogStatus=%s\n' "$license_dialog_status"
    printf 'licenseDialogBlocker=%s\n' "$license_dialog_blocker"
    printf 'selectProjectWindow=%s\n' select-project-window.json
    printf 'selectProjectStatus=%s\n' "$select_project_status"
    printf 'selectProjectBlocker=%s\n' "$select_project_blocker"
    printf 'selectProjectInteraction=%s\n' "$select_project_interaction"
    printf 'selectProjectWaitStatus=%s\n' "$select_project_wait_status"
    printf 'swingWidgetObservation=%s\n' swing-widget-observation.json
    printf 'swingWidgetStatus=%s\n' "$swing_widget_status"
    printf 'swingWidgetBlocker=%s\n' "$swing_widget_blocker"
    printf 'tabClickObservation=%s\n' tab-click-observation.json
    printf 'tabClickStatus=%s\n' "$tab_click_status"
    printf 'tabClickBlocker=%s\n' "$tab_click_blocker"
    printf 'selectProjectEvidenceStatus=%s\n' "$select_project_evidence_status"
    printf 'selectProjectTargetDisplayName=%s\n' "$select_project_target_display_name"
    printf 'selectProjectTargetRepositoryPath=%s\n' "$select_project_target_repo_path"
    printf 'selectProjectOpenedStarterDisplayName=%s\n' "$select_project_opened_display_name"
    printf 'selectProjectOpenedStarterRepositoryPath=%s\n' "$select_project_opened_repo_path"
    printf 'selectProjectProjectOpenObserved=%s\n' "$select_project_project_open_observed"
    printf 'selectProjectTargetSelectionObserved=%s\n' "$select_project_target_selection_observed"
    printf 'selectProjectOpenAttempted=%s\n' "$select_project_open_attempted"
    printf 'selectProjectNextBlocker=%s\n' "$select_project_next_blocker"
    printf 'selectProjectWindowContext=%s\n' "$select_project_window_context"
    printf 'selectProjectAliceJavaPid=%s\n' "$select_project_alice_java_pid"
    printf 'selectProjectStartersTabSafety=%s\n' "$select_project_starters_tab_safety"
    printf 'selectProjectTargetMetadataEchoValid=%s\n' "$select_project_target_metadata_echo_valid"
    printf 'postProjectOpenObservation=%s\n' post-project-open-observation.json
    printf 'postProjectOpenStatus=%s\n' "$post_open_status"
    printf 'postProjectOpenBlocker=%s\n' "$post_open_blocker"
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      printf 'runtimeDisplayAccessibilityEvidence=%s\n' "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
    else
      printf 'runtimeDisplayAccessibilityEvidence=not-requested\n'
    fi
    printf 'runtimeDisplayAccessibilityStatus=%s\n' "$runtime_display_status"
    printf 'runtimeDisplayAccessibilityBlocker=%s\n' "$runtime_display_blocker"
    if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
      printf 'procedureTargetObservationEvidence=%s\n' "$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT"
    else
      printf 'procedureTargetObservationEvidence=not-requested\n'
    fi
    printf 'procedureTargetObservationStatus=%s\n' "$procedure_target_status"
    printf 'procedureTargetObservationBlocker=%s\n' "$procedure_target_blocker"
    printf 'downstreamBlockedStep=desktop-procedure-edit-action-proof\n'
    printf 'timeoutSeconds=%s\n' "$run_timeout"
  } > "$run_dir/status.txt"

  local observation_status observation_blocker observation_detail pixels_observed observation_claim screenshot_file
  observation_status=observed
  observation_blocker=none
  observation_detail="Xvfb display was reachable, a visible Alice window was detected, and a root screenshot with non-black pixels was captured; this does not assert Alice rendering correctness."
  pixels_observed=true
  observation_claim=controlled-display-pixels-observed-rendering-not-asserted
  screenshot_file=screenshot.png
  if [ "$screenshot_tool" = xwd ]; then
    screenshot_file=screenshot.xwd
  fi
  local screenshot_pixel_status screenshot_pixel_detail
  screenshot_pixel_status=not-attempted
  screenshot_pixel_detail=
  if [ "$screenshot_status" = screenshot-captured ] && [ "$screenshot_tool" != xwd ]; then
    analyze_screenshot_pixels "$run_dir/$screenshot_file" "$run_dir/screenshot-pixels.txt"
    screenshot_pixel_status=$(sed -n 's/^status=//p' "$run_dir/screenshot-pixels.txt" | head -1)
    screenshot_pixel_detail=$(sed -n 's/^detail=//p' "$run_dir/screenshot-pixels.txt" | head -1)
  elif [ "$screenshot_status" = screenshot-captured ]; then
    screenshot_pixel_status=analysis-unavailable
    screenshot_pixel_detail="xwd screenshot capture is available, but this runner does not classify xwd pixel contents."
  fi
  if [ "$screenshot_status" != screenshot-captured ]; then
    observation_status=blocked
    observation_blocker=screenshot-capture-failed
    observation_detail="Screenshot capture failed on DISPLAY=$display; install import, gnome-screenshot, or xwd and inspect screenshot.log."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$process_status" != running ]; then
    observation_status=blocked
    observation_blocker=application-exited-before-pixel-capture
    observation_detail="Alice launch process exited before pixel capture could prove a visible display; inspect launch.log."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$ready_status" = process-exited ]; then
    observation_status=blocked
    observation_blocker=application-exited-before-window-ready
    observation_detail="Alice launch process exited before window readiness; inspect launch.log."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$application_root_error_status" = observed ]; then
    observation_status=blocked
    observation_blocker="$application_root_error_blocker"
    observation_detail="A Java window titled Application Root Error was observed before any Alice desktop candidate; inspect application-root-error.json for exact expected dialog text and next invocation change."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$license_dialog_status" = observed ]; then
    observation_status=blocked
    observation_blocker="$license_dialog_blocker"
    observation_detail="A first-run License Agreement dialog was observed; inspect license-dialog.json for exact title, controls, and the isolated test-only Java Preferences bypass."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$ready_status" != alice-window-found ]; then
    observation_status=blocked
    observation_blocker=alice-window-not-found
    observation_detail="Xvfb was reachable, but xdotool did not find a visible Alice window before screenshot capture; readyStatus=$ready_status. Inspect x-window-inventory.json for exact Alice-related visible X window title/class/process/geometry."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$screenshot_pixel_status" = uniform-black ]; then
    observation_status=blocked
    observation_blocker=screenshot-captured-uniform-black
    observation_detail="A controlled Xvfb screenshot was captured after Alice window readiness, but the image was uniformly black."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$screenshot_pixel_status" != non-black-pixels ]; then
    observation_status=attempted
    observation_blocker=screenshot-pixel-analysis-unavailable
    observation_detail="$screenshot_pixel_detail"
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  fi
  local runtime_display_artifact_path=
  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
    runtime_display_artifact_path="$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
  fi

  write_controlled_display_pixel_observation \
    "$run_dir" \
    "$observation_status" \
    "$observation_blocker" \
    "$observation_detail" \
    "$display" \
    "$pixels_observed" \
    "$observation_claim" \
    "" \
    "$ready_status" \
    "$process_status" \
    "$screenshot_status" \
    "$screenshot_file" \
    "$xvfb_executable" \
    "$screenshot_tool" \
    "$screenshot_pixel_status" \
    "$screenshot_pixel_detail" \
    after-readiness-wait \
    "$window_inventory_status" \
    x-window-inventory.json \
    "$alice_window_candidate_count" \
    "$runtime_display_artifact_path"

  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
    local visible_rendering_pixel_target_status visible_rendering_pixel_target_artifact
    local visible_rendering_pixel_sampling_status visible_rendering_pixel_sampling_artifact visible_rendering_pixel_sampling_blocker
    visible_rendering_pixel_target_status=$(inventory_json_field "$run_dir/controlled-display-pixel-observation.json" worldCanvasPixelTarget.status)
    if [ "$visible_rendering_pixel_target_status" = target-ready ]; then
      visible_rendering_pixel_target_artifact=controlled-display-pixel-observation.json
    else
      visible_rendering_pixel_target_status=blocked
      visible_rendering_pixel_target_artifact="$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER"
      write_visible_rendering_pixel_target_blocker \
        "$run_dir" \
        "$observation_status" \
        "$observation_blocker" \
        "$screenshot_file" \
        "$screenshot_status" \
        "$screenshot_pixel_status" \
        "$runtime_display_artifact_path"
    fi
    write_visible_rendering_pixel_sampling_evidence \
      "$run_dir" \
      "$run_dir/controlled-display-pixel-observation.json"
    local -a visible_rendering_pixel_sampling_fields
    if [ -f "$run_dir/$VISIBLE_RENDERING_PIXEL_OBSERVATION" ]; then
      visible_rendering_pixel_sampling_artifact="$VISIBLE_RENDERING_PIXEL_OBSERVATION"
    else
      visible_rendering_pixel_sampling_artifact="$VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER"
    fi
    read_inventory_json_fields visible_rendering_pixel_sampling_fields "$run_dir/$visible_rendering_pixel_sampling_artifact" status blocker
    visible_rendering_pixel_sampling_status=${visible_rendering_pixel_sampling_fields[0]}
    visible_rendering_pixel_sampling_blocker=${visible_rendering_pixel_sampling_fields[1]}
    scenario_outcome=blocked
    if [ "$observation_status" = observed ] && [ "$runtime_display_status" = observed ] && [ "$visible_rendering_pixel_sampling_status" = observed ]; then
      scenario_outcome=passed
    fi
    {
      cat "$run_dir/status.txt"
      printf 'outcome=%s\n' "$scenario_outcome"
      printf 'controlledDisplayPixelStatus=%s\n' "$observation_status"
      printf 'controlledDisplayPixelBlocker=%s\n' "$observation_blocker"
      printf 'visibleRenderingPixelTargetStatus=%s\n' "$visible_rendering_pixel_target_status"
      printf 'visibleRenderingPixelTargetArtifact=%s\n' "$visible_rendering_pixel_target_artifact"
      if [ "$visible_rendering_pixel_target_artifact" = "$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER" ]; then
        printf 'visibleRenderingPixelTargetBlocker=%s\n' "$VISIBLE_RENDERING_PIXEL_TARGET_BLOCKER"
      fi
      printf 'visibleRenderingPixelSamplingStatus=%s\n' "$visible_rendering_pixel_sampling_status"
      printf 'visibleRenderingPixelSamplingArtifact=%s\n' "$visible_rendering_pixel_sampling_artifact"
      printf 'visibleRenderingPixelSamplingBlocker=%s\n' "$visible_rendering_pixel_sampling_blocker"
      printf 'visibleRenderingCorrectnessEstablished=false\n'
    } > "$run_dir/status.txt.tmp"
    mv "$run_dir/status.txt.tmp" "$run_dir/status.txt"
  fi
  if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ]; then
    scenario_outcome=blocked
    if [ "$observation_status" = observed ] &&
      { [ "$procedure_target_status" = edit-ready ] ||
        { [ "$procedure_target_status" = blocked ] && [ "$procedure_target_blocker" = missing-desktop-edit-action-contract ]; }; }; then
      scenario_outcome=passed
    fi
    {
      cat "$run_dir/status.txt"
      printf 'outcome=%s\n' "$scenario_outcome"
      printf 'controlledDisplayPixelStatus=%s\n' "$observation_status"
      printf 'controlledDisplayPixelBlocker=%s\n' "$observation_blocker"
    } > "$run_dir/status.txt.tmp"
    mv "$run_dir/status.txt.tmp" "$run_dir/status.txt"
  fi

  if [ "$screenshot_status" != screenshot-captured ]; then
    printf 'Screenshot capture failed; see %s/screenshot.log\n' "$run_dir" >&2
    return 2
  fi
  if [ "$process_status" != running ]; then
    printf 'Alice launch process exited before evidence capture; see %s/launch.log\n' "$run_dir" >&2
    return 1
  fi
  if [ "$ready_status" = process-exited ]; then
    printf 'Alice launch process exited before window readiness; see %s/launch.log\n' "$run_dir" >&2
    return 1
  fi
  if [ "$ready_status" = not-found ]; then
    printf 'No visible Alice desktop window was detected; see %s/status.txt\n' "$run_dir" >&2
    return 1
  fi
  if [ "$observation_status" != observed ]; then
    printf 'Controlled display pixel proof blocked: %s; see %s/controlled-display-pixel-observation.json\n' "$observation_blocker" "$run_dir" >&2
    return 1
  fi
  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ] && [ "$runtime_display_status" != observed ]; then
    printf 'Post-open runtime/display accessibility evidence blocked: %s; see %s/%s\n' "$runtime_display_blocker" "$run_dir" "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" >&2
    return 2
  fi
  if [ "$scenario_id" = "$FIRST_LESSON_PROCEDURE_TARGET_SCENARIO" ] &&
    ! { [ "$procedure_target_status" = edit-ready ] ||
      { [ "$procedure_target_status" = blocked ] && [ "$procedure_target_blocker" = missing-desktop-edit-action-contract ]; }; }; then
    printf 'First-lesson procedure target observation blocked: %s; see %s/%s\n' "$procedure_target_blocker" "$run_dir" "$FIRST_LESSON_PROCEDURE_TARGET_ARTIFACT" >&2
    return 2
  fi
  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ] && [ "${visible_rendering_pixel_sampling_status:-blocked}" != observed ]; then
    printf 'World-canvas pixel sampling evidence blocked: %s; see %s/%s\n' "${visible_rendering_pixel_sampling_blocker:-world-canvas-pixel-sampling-not-observed}" "$run_dir" "$VISIBLE_RENDERING_PIXEL_SAMPLING_BLOCKER" >&2
    return 2
  fi

  printf 'Evidence written to %s\n' "$run_dir"
}

run_gated_command_smoke() {
  local scenario_json=$1
  local run_dir=$2
  local timeout_override=$3
  local prepare_only=$4

  local automation_fields cwd configured_timeout scenario_id automation_mode run_timeout checklist exit_code outcome resolved_cwd
  local save_proof_artifact save_proof_run_id save_proof_validation_status save_proof_validation_exit command_start_epoch
  local run_window_evidence_dir run_window_artifact run_window_validation_status run_window_validation_exit
  local -a argv command_argv
  mapfile -t automation_fields < <(SCENARIO_JSON="$scenario_json" python3 - <<'PY'
import json
import os

scenario = json.loads(os.environ["SCENARIO_JSON"])
automation = scenario["automation"]
print(automation["cwd"])
print(automation.get("timeoutSeconds", ""))
print(scenario["id"])
print(scenario["automationMode"])
PY
  )
  cwd=${automation_fields[0]}
  configured_timeout=${automation_fields[1]}
  scenario_id=${automation_fields[2]}
  automation_mode=${automation_fields[3]}
  mapfile -d '' -t argv < <(json_list_nul "$scenario_json" "automation.argv")
  run_timeout="${timeout_override:-$configured_timeout}"

  validate_allowed_automation "$cwd" "${argv[@]}"
  resolved_cwd=$(resolve_automation_cwd "$cwd")
  write_environment "$run_dir"
  save_proof_artifact=
  save_proof_run_id=
  run_window_artifact=
  if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
    if [ -n "$timeout_override" ]; then
      printf '%s workflow does not accept --timeout-seconds\n' "$scenario_id" >&2
      return 2
    fi
  fi
  if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
    save_proof_artifact="$(CDPATH= cd -- "$run_dir" && pwd)/robot-save-menu-dialog-write-readback-proof.json"
    save_proof_run_id=$(basename "$run_dir")
    if [[ ! "$save_proof_run_id" =~ ^[A-Za-z0-9._-]+$ ]]; then
      printf 'generated Save proof runId is not a safe token: %s\n' "$save_proof_run_id" >&2
      return 2
    fi
  fi
  if [ "$scenario_id" = "$RUN_WINDOW_CONTRACT_SCENARIO" ]; then
    if [ -n "$timeout_override" ]; then
      printf 'Run-window contract workflow does not accept --timeout-seconds\n' >&2
      return 2
    fi
    run_window_evidence_dir="$(CDPATH= cd -- "$run_dir" && pwd)"
    run_window_artifact="$run_window_evidence_dir/$RUN_WINDOW_CONTRACT_ARTIFACT"
  fi

  if [ "$prepare_only" = "1" ] || [ "${ALICE_QA_RUN_GATED_SMOKES:-}" != "1" ]; then
    checklist=$(write_checklist "$scenario_json" "$run_dir")
    {
      printf 'scenario=%s\n' "$scenario_id"
      printf 'automationMode=%s\n' "$automation_mode"
      printf 'outcome=gated-not-run\n'
      printf 'executionStatus=not-run\n'
      printf 'executionClaim=no-gui-execution\n'
      printf 'gate=ALICE_QA_RUN_GATED_SMOKES\n'
      if [ "$prepare_only" = "1" ]; then
        printf 'skipMode=prepare-only\n'
      else
        printf 'skipMode=missing-gate\n'
      fi
      printf 'checklist=%s\n' "$(basename "$checklist")"
      printf 'argv=%s\n' "$(format_argv "${argv[@]}")"
      printf 'cwd=%s\n' "$cwd"
      write_gated_timeout_policy "$scenario_id" "$run_timeout"
      if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
        printf 'saveProofEvidence=%s\n' robot-save-menu-dialog-write-readback-proof.json
      elif [ "$scenario_id" = "$RUN_WINDOW_CONTRACT_SCENARIO" ]; then
        printf 'runWindowEvidence=%s\n' "$RUN_WINDOW_CONTRACT_ARTIFACT"
        printf 'runWindowEvidenceStatus=not-run\n'
      fi
    } > "$run_dir/status.txt"
    if [ "$prepare_only" = "1" ]; then
      printf 'Gated command scenario prepared without execution: %s\n' "$scenario_id"
      return 0
    fi
    printf 'Gated command scenario not run: set ALICE_QA_RUN_GATED_SMOKES=1 to execute %s, or pass --prepare-only to record an intentional skip.\n' "$scenario_id" >&2
    return 3
  fi

  command_argv=("${argv[@]}")
  if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
    command_argv=(
      "${argv[0]}"
      "-Dorg.alice.eatme.saveProof.scenario=$scenario_id"
      "-Dorg.alice.eatme.saveProof.runId=$save_proof_run_id"
      "-Dorg.alice.eatme.saveProof.evidencePath=$save_proof_artifact"
      "${argv[@]:1}"
    )
  elif [ "$scenario_id" = "$RUN_WINDOW_CONTRACT_SCENARIO" ]; then
    command_argv=(
      "${argv[0]}"
      "-Dorg.alice.eatme.runWindowEvidenceDir=$run_window_evidence_dir"
      "${argv[@]:1}"
    )
  fi

  command_start_epoch=$(date -u +%s)
  set +e
  (
    cd "$resolved_cwd"
    export NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=32768}"
    if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
      export ALICE_SAVE_PROOF_SCENARIO="$scenario_id"
      export ALICE_SAVE_PROOF_RUN_ID="$save_proof_run_id"
      export ALICE_SAVE_PROOF_EVIDENCE_PATH="$save_proof_artifact"
    fi
    if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ] ||
      [ "$scenario_id" = alice-desktop-project-io-smoke ]; then
      "${command_argv[@]}" < /dev/null
    elif [ "$scenario_id" = "$RUN_WINDOW_CONTRACT_SCENARIO" ]; then
      export ALICE_RUN_WINDOW_EVIDENCE_DIR="$run_window_evidence_dir"
      "${command_argv[@]}" < /dev/null
    else
      timeout --foreground -k 10s "${run_timeout}s" "${command_argv[@]}" < /dev/null
    fi
  ) > "$run_dir/command.log" 2>&1
  exit_code=$?
  set -e
  save_proof_validation_status=not-requested
  run_window_validation_status=not-requested
  if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
    set +e
    validate_save_proof_evidence "$save_proof_artifact" \
      --scenario "$scenario_id" \
      --workflow save-menu-dialog-write-proof \
      --run-id "$save_proof_run_id" \
      --started-at-epoch "$command_start_epoch" \
      > "$run_dir/save-proof-validation.log" 2>&1
    save_proof_validation_exit=$?
    set -e
    if [ "$save_proof_validation_exit" -eq 0 ]; then
      save_proof_validation_status=proven
    else
      save_proof_validation_status=failed
      if [ "$exit_code" -eq 0 ]; then
        exit_code=$save_proof_validation_exit
      fi
    fi
  fi
  if [ "$scenario_id" = "$RUN_WINDOW_CONTRACT_SCENARIO" ]; then
    if validate_run_window_evidence "$run_window_artifact" > "$run_dir/run-window-validation.log" 2>&1; then
      run_window_validation_status=created
    else
      run_window_validation_exit=$?
      run_window_validation_status=failed
      if [ "$exit_code" -eq 0 ]; then
        exit_code=$run_window_validation_exit
      fi
    fi
  fi

  outcome=failed
  if [ "$exit_code" -eq 0 ]; then
    outcome=passed
  fi

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    printf 'outcome=%s\n' "$outcome"
    printf 'exitCode=%s\n' "$exit_code"
    printf 'executionStatus=executed\n'
    printf 'executionClaim=gated-command-executed\n'
    printf 'commandLog=command.log\n'
    printf 'argv=%s\n' "$(format_argv "${argv[@]}")"
    printf 'cwd=%s\n' "$cwd"
    write_gated_timeout_policy "$scenario_id" "$run_timeout"
    if [ "$scenario_id" = alice-desktop-save-menu-dialog-write-proof ]; then
      printf 'saveProofEvidence=%s\n' robot-save-menu-dialog-write-readback-proof.json
      printf 'saveProofEvidenceStatus=%s\n' "$save_proof_validation_status"
      printf 'saveProofValidationLog=%s\n' save-proof-validation.log
    elif [ "$scenario_id" = "$RUN_WINDOW_CONTRACT_SCENARIO" ]; then
      printf 'runWindowEvidence=%s\n' "$RUN_WINDOW_CONTRACT_ARTIFACT"
      printf 'runWindowEvidenceStatus=%s\n' "$run_window_validation_status"
      printf 'runWindowValidationLog=%s\n' run-window-validation.log
    fi
  } > "$run_dir/status.txt"

  if [ "$exit_code" -ne 0 ]; then
    printf 'Gated command scenario failed; see %s/command.log\n' "$run_dir" >&2
    return "$exit_code"
  fi

  printf 'Evidence written to %s\n' "$run_dir"
}
main() {
  local command_name scenario_request evidence_base timeout_override prepare_only artifact_path
  local scenario_id scenario_json timestamp run_dir automation_mode checklist

  command_name=${1:-}
  case "$command_name" in
    list)
      "$VALIDATOR" --list
      ;;
    validate)
      "$VALIDATOR"
      ;;
    validate-save-proof-evidence)
      shift
      if [ "$#" -lt 1 ]; then
        printf '%s\n' 'validate-save-proof-evidence requires an artifact path' >&2
        exit 2
      fi
      artifact_path=$1
      shift
      validate_save_proof_evidence "$artifact_path" "$@"
      ;;
    run)
      shift
      scenario_request=${1:-}
      if [ -z "$scenario_request" ]; then
        usage >&2
        exit 2
      fi
      shift

      evidence_base="$BASE_DIR/evidence"
      timeout_override=
      prepare_only=0
      while [ "$#" -gt 0 ]; do
        case "$1" in
          --evidence-dir)
            if [ "$#" -lt 2 ]; then
              printf '%s\n' '--evidence-dir requires a value' >&2
              exit 2
            fi
            evidence_base=$2
            shift 2
            ;;
          --timeout-seconds)
            if [ "$#" -lt 2 ]; then
              printf '%s\n' '--timeout-seconds requires a value' >&2
              exit 2
            fi
            timeout_override=$2
            validate_positive_integer "$timeout_override" "timeout"
            shift 2
            ;;
          --prepare-only)
            prepare_only=1
            shift
            ;;
          *)
            printf 'unknown argument: %s\n' "$1" >&2
            usage >&2
            exit 2
            ;;
        esac
      done

      scenario_id=$(resolve_scenario_id "$scenario_request")
      scenario_json=$("$VALIDATOR" --dump-json "$scenario_id")
      automation_mode=$(json_fields "$scenario_json" "automationMode")
      # xvfb-real-alice and gated-command-smoke validate cwd internally via
      # resolve_automation_cwd; skip the redundant early validation for them.
      case "$automation_mode" in
        xvfb-real-alice|gated-command-smoke) ;;
        *) validate_scenario_automation_cwd "$scenario_json" ;;
      esac
      timestamp=$(date -u +%Y%m%dT%H%M%SZ)
      run_dir="$evidence_base/$scenario_id/$timestamp"
      mkdir -p "$run_dir"

      case "$automation_mode" in
        xvfb-real-alice)
          run_xvfb_real_alice "$scenario_json" "$run_dir" "$timeout_override"
          ;;
        gated-command-smoke)
          run_gated_command_smoke "$scenario_json" "$run_dir" "$timeout_override" "$prepare_only"
          ;;
        manual-evidence-required)
          write_environment "$run_dir"
          checklist=$(write_checklist "$scenario_json" "$run_dir")
          write_manual_status "$run_dir" "$checklist" "$scenario_id" "$automation_mode"
          printf 'Manual scenario prepared: %s\n' "$checklist"
          ;;
        *)
          printf 'unsupported automationMode: %s\n' "$automation_mode" >&2
          exit 1
          ;;
      esac
      ;;
    -h|--help|help|"")
      usage
      ;;
    *)
      printf 'unknown command: %s\n' "$command_name" >&2
      usage >&2
      exit 2
      ;;
  esac
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
