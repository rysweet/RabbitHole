#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-first-lesson-live-procedure-target-observation-artifact.sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCHEMA="$BASE_DIR/schema/scenario.schema.json"
SCENARIO_ID=alice-desktop-first-lesson-live-procedure-target-observation
WORKFLOW=first-lesson-live-procedure-target-observation
SCENARIO_FILE="$BASE_DIR/scenarios/first-lesson-live-procedure-target-observation.yaml"
ARTIFACT=first-lesson-live-procedure-target-observation.json
SEAM=live-first-lesson-procedure-target-to-desktop-edit-action
DOWNSTREAM_BLOCKED_STEP=desktop-procedure-edit-action-proof
PROCEDURE_SELECTOR=scene.eatmeFirstLesson
MISSING_DESKTOP_EDIT_ACTION_CONTRACT="missing public CodeEditor/CodeComposite edit invocation contract"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_file_exists "$SCENARIO_FILE" "first-lesson live procedure target observation scenario file exists"

"$VALIDATOR" --dump-json "$SCENARIO_ID" >"$tmp_root/scenario.json" 2>"$tmp_root/scenario.err"
status=$?
assert_success "$status" "validator dumps the first-lesson procedure target observation scenario by id"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$tmp_root/scenario.json" \
    "$SCENARIO_ID" \
    "$WORKFLOW" \
    "$ARTIFACT" \
    "$SEAM" \
    "$PROCEDURE_SELECTOR" \
    >"$tmp_root/scenario-contract.out" \
    2>"$tmp_root/scenario-contract.err" <<'PY'
import json
import os
import re
import sys

(
    scenario_path,
    expected_id,
    expected_workflow,
    expected_artifact,
    expected_seam,
    expected_procedure_selector,
) = sys.argv[1:7]
scenario = json.load(open(scenario_path, encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(scenario.get("id") == expected_id, f"id must be {expected_id!r}")
require(scenario.get("workflow") == expected_workflow, f"workflow must be {expected_workflow!r}")
require(scenario.get("automationMode") == "xvfb-real-alice", "scenario must execute as xvfb-real-alice")

automation = scenario.get("automation", {})
expected_argv = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-Dcheckstyle.skip",
    "-DskipTests",
    "compile",
    "exec:exec@alice-ide-atk",
]
require(automation.get("cwd") == "alice-ide", "automation.cwd must reuse the Alice IDE AT-SPI launch path")
require(automation.get("argv") == expected_argv, "automation.argv must be the narrow Alice AT-SPI launch argv")
require(isinstance(automation.get("timeoutSeconds"), int) and automation["timeoutSeconds"] >= 1, "automation.timeoutSeconds must be positive")
require(isinstance(automation.get("readyWaitSeconds"), int) and automation["readyWaitSeconds"] >= 1, "automation.readyWaitSeconds must be positive")

target_starter = scenario.get("targetStarter")
require(isinstance(target_starter, dict), "scenario must declare targetStarter for the first-lesson starter opened through Select Project")
if isinstance(target_starter, dict):
    display_name = target_starter.get("displayName", "")
    repository_path = target_starter.get("repositoryPath", "")
    require(isinstance(display_name, str) and display_name.strip(), "targetStarter.displayName must be non-empty")
    require(isinstance(repository_path, str) and repository_path.strip(), "targetStarter.repositoryPath must be non-empty")
    require(not os.path.isabs(repository_path), "targetStarter.repositoryPath must be repository-relative")
    require(".." not in repository_path.split("/"), "targetStarter.repositoryPath must reject traversal")
    require(repository_path.endswith(".a3p"), "targetStarter.repositoryPath must identify the first-lesson .a3p starter")
    require("tbd" not in display_name.lower(), "targetStarter.displayName must not be TBD placeholder text")
    require("tbd" not in repository_path.lower(), "targetStarter.repositoryPath must not be TBD placeholder text")

scenario_text = json.dumps(scenario, sort_keys=True)
scenario_lower = scenario_text.lower()
for required in (
    "first-lesson",
    expected_seam,
    expected_procedure_selector,
    "procedure tab",
    "code-editor",
    "select project",
    "post-project-open-observation.json",
    "tab-click-observation.json",
    expected_artifact,
    "missing public CodeEditor/CodeComposite edit invocation contract",
):
    require(required.lower() in scenario_lower, f"scenario must tie the seam to {required}")

evidence_text = "\n".join(scenario.get("evidence", {}).get("required", []))
for required in (
    "status.txt",
    expected_artifact,
    "procedureTargetObservationEvidence",
    "procedureTargetObservationStatus",
    "procedureTargetObservationBlocker",
    "downstreamBlockedStep=desktop-procedure-edit-action-proof",
):
    require(required in evidence_text, f"evidence.required must name {required}")

supporting = set(scenario.get("supportingEvidence", []))
for required in (
    "alice-desktop-select-project-tab-click-exec",
    "alice-desktop-post-project-open-window-state",
):
    require(required in supporting, f"scenario must cite supporting evidence {required}")

tags = set(scenario.get("tags", []))
for required in ("first-lesson", "procedure-target", "observation-only"):
    require(required in tags, f"scenario tags must include {required}")

claim_text = "\n".join([
    scenario.get("title", ""),
    "\n".join(scenario.get("expectedOutcomes", [])),
    evidence_text,
    "\n".join(scenario.get("fallback", {}).get("notes", [])),
])
for forbidden in (
    "save proof",
    "save completion",
    "rendering correctness",
    "learner assessment",
    "grading",
    "creative assessment",
    "full first-lesson completion",
):
    matches = [paragraph for paragraph in re.split(r"\n\s*\n", claim_text.lower()) if forbidden in paragraph]
    for paragraph in matches:
        if not any(marker in paragraph for marker in ("not ", "no ", "does not", "do not", "out of scope", "exclude", "without")):
            errors.append(f"scenario must not overclaim {forbidden}: {paragraph[:160]}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  scenario_contract_status=$?
else
  printf 'scenario was not dumped; skipping detailed scenario contract\n' >"$tmp_root/scenario-contract.err"
  scenario_contract_status=1
fi
assert_success "$scenario_contract_status" "scenario contract is first-lesson scoped, observation-only, and executable"

"$RUNNER" list >"$tmp_root/list.out" 2>"$tmp_root/list.err"
status=$?
assert_success "$status" "runner lists scenario catalog for first-lesson procedure target observation"
assert_contains "$tmp_root/list.out" "$SCENARIO_ID" "runner list includes the first-lesson procedure target observation scenario"

python3 - "$SCHEMA" "$WORKFLOW" >"$tmp_root/schema-workflow.out" 2>"$tmp_root/schema-workflow.err" <<'PY'
import json
import sys

schema_path, expected_workflow = sys.argv[1:3]
schema = json.load(open(schema_path, encoding="utf-8"))
workflow_enum = set(schema["properties"]["workflow"]["enum"])
if expected_workflow not in workflow_enum:
    raise AssertionError(f"schema workflow enum is missing {expected_workflow}")

allowed_argv = {
    tuple(item.get("const") for item in option.get("prefixItems", []))
    for option in schema["properties"]["automation"]["properties"]["argv"].get("oneOf", [])
}
expected_argv = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-Dcheckstyle.skip",
    "-DskipTests",
    "compile",
    "exec:exec@alice-ide-atk",
)
if expected_argv not in allowed_argv:
    raise AssertionError("schema must allow only the existing Alice AT-SPI launch argv needed by this shard")
PY
status=$?
assert_success "$status" "schema recognizes the first-lesson procedure target workflow and narrow AT-SPI argv"

assert_contains "$VALIDATOR" "\"$WORKFLOW\"" "validator allowlists the first-lesson procedure target workflow"
assert_contains "$VALIDATOR" "\"$SCENARIO_ID\"" "validator treats the first-lesson procedure target scenario as target-specific"
assert_contains "$RUNNER" "$ARTIFACT" "runner names the fixed first-lesson procedure target artifact"
assert_contains "$RUNNER" "procedureTargetObservationEvidence" "runner status links the procedure target observation artifact"
assert_contains "$RUNNER" "desktop-procedure-edit" "runner records the downstream desktop procedure edit blocker"
assert_contains "$RUNNER" "$PROCEDURE_SELECTOR" "runner probes for the scene.eatmeFirstLesson live desktop target"

evidence_dir="$tmp_root/no-xvfb-evidence"
set +e
ALICE_QA_DISABLE_XVFB=1 "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$evidence_dir" >"$tmp_root/no-xvfb.out" 2>"$tmp_root/no-xvfb.err"
status=$?
set -e
assert_exit_code "$status" 2 "missing Xvfb produces a structured blocker for the first-lesson procedure target scenario"

run_dir=$(single_child_dir "$evidence_dir/$SCENARIO_ID")
run_dir_status=$?
assert_success "$run_dir_status" "first-lesson procedure target fallback creates one evidence directory"
if [ "$run_dir_status" -eq 0 ]; then
  status_file="$run_dir/status.txt"
  artifact_file="$run_dir/$ARTIFACT"
  assert_file_exists "$status_file" "first-lesson procedure target fallback writes status.txt"
  assert_file_exists "$artifact_file" "first-lesson procedure target fallback writes structured JSON artifact"
  assert_contains "$status_file" "^scenario=$SCENARIO_ID$" "status records scenario id"
  assert_contains "$status_file" '^automationMode=xvfb-real-alice$' "status records automation mode"
  assert_contains "$status_file" '^outcome=blocked$' "status records blocked outcome"
  assert_contains "$status_file" "^procedureTargetObservationEvidence=$ARTIFACT$" "status points to procedure target artifact"
  assert_contains "$status_file" '^procedureTargetObservationStatus=blocked$' "status records procedure target blocked status"
  assert_contains "$status_file" '^procedureTargetObservationBlocker=display-prerequisite-unavailable$' "status records display prerequisite blocker"
  assert_contains "$status_file" "^downstreamBlockedStep=$DOWNSTREAM_BLOCKED_STEP$" "status names the downstream edit-action proof step"

  python3 - \
    "$artifact_file" \
    "$SCENARIO_ID" \
    "$WORKFLOW" \
    "$SEAM" \
    "$PROCEDURE_SELECTOR" \
    >"$tmp_root/artifact-contract.out" \
    2>"$tmp_root/artifact-contract.err" <<'PY'
import json
import os
import sys

artifact_path, expected_id, expected_workflow, expected_seam, expected_selector = sys.argv[1:6]
artifact = json.load(open(artifact_path, encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(artifact.get("schemaVersion") == "eatme.first-lesson-live-procedure-target-observation/v1", "artifact schemaVersion must be v1")
require(artifact.get("scenario") == expected_id, "artifact scenario must match the scenario id")
require(artifact.get("workflow") == expected_workflow, "artifact workflow must match the workflow")
require(artifact.get("automationMode") == "xvfb-real-alice", "artifact automationMode must be xvfb-real-alice")
require(artifact.get("status") in {"edit-ready", "blocked"}, "artifact status must be edit-ready or blocked, not target-only observed")
require(artifact.get("seam") == expected_seam, "artifact seam must name the procedure/code-editor target to desktop edit-action transition")
require(artifact.get("downstreamBlockedStep") in {"none", "desktop-procedure-edit-action-proof"}, "artifact must name the desktop edit-action proof boundary")

project = artifact.get("project")
require(isinstance(project, dict), "artifact project must be an object")
if isinstance(project, dict):
    for field in ("targetStarterDisplayName", "targetStarterRepositoryPath", "openedViaSelectProject", "postOpenWindowObserved"):
        require(field in project, f"artifact project must include {field}")
    path = project.get("targetStarterRepositoryPath")
    if isinstance(path, str) and path:
        require(not os.path.isabs(path), "artifact targetStarterRepositoryPath must be relative")
        require(".." not in path.split("/"), "artifact targetStarterRepositoryPath must not traverse")

required_target = artifact.get("requiredTarget")
require(isinstance(required_target, dict), "artifact requiredTarget must be an object")
if isinstance(required_target, dict):
    require(required_target.get("procedureName") == expected_selector, "requiredTarget.procedureName must be scene.eatmeFirstLesson")
    require(required_target.get("kind") == "procedure-or-code-editor-target", "requiredTarget.kind must name the accepted target seam")
    require(
        required_target.get("minimumStableAutomationTarget") == "reacquirable live desktop procedure tab or code-editor target",
        "requiredTarget.minimumStableAutomationTarget must name the stable automation target",
    )

blocker = artifact.get("blocker")
require(isinstance(blocker, dict), "artifact blocker must be a machine-readable object")
if isinstance(blocker, dict):
    require(isinstance(blocker.get("kind"), str) and blocker.get("kind"), "artifact blocker.kind must be non-empty")
    require(isinstance(blocker.get("message"), str), "artifact blocker.message must be a string")

if artifact.get("status") == "blocked":
    require(artifact.get("downstreamBlockedStep") == "desktop-procedure-edit-action-proof", "blocked artifact must point at the edit-action proof boundary")
    require(isinstance(artifact.get("desktopEditAction"), dict), "blocked artifact must include desktopEditAction readiness or blocker evidence")
    if isinstance(blocker, dict) and blocker.get("kind") in {
        "select-project-open-not-observed",
        "post-open-window-not-observed",
        "procedure-target-not-found",
        "procedure-target-not-stable",
        "at-spi-or-atk-unavailable",
        "display-prerequisite-unavailable",
    }:
        require(artifact.get("observedTarget") is None, "run-failure blocked artifact observedTarget must be null")
        require(isinstance(artifact.get("blockerDetail"), str) and artifact.get("blockerDetail"), "run-failure blocked artifact must include blockerDetail")
    else:
        require(isinstance(artifact.get("observedTarget"), dict), "accepted action-seam blocker must include observedTarget")
        require(blocker.get("kind") == "missing-desktop-edit-action-contract", "accepted action-seam blocker must name missing-desktop-edit-action-contract")
        require(
            blocker.get("message") == "missing public CodeEditor/CodeComposite edit invocation contract",
            "accepted action-seam blocker must name the missing public CodeEditor/CodeComposite edit invocation contract",
        )
else:
    observed = artifact.get("observedTarget")
    desktop_edit_action = artifact.get("desktopEditAction")
    require(artifact.get("status") == "edit-ready", "non-blocked artifact must be edit-ready")
    require(artifact.get("downstreamBlockedStep") == "none", "edit-ready artifact must not name a downstream blocker")
    require(isinstance(blocker, dict) and blocker.get("kind") == "none" and blocker.get("message") == "", "edit-ready artifact blocker must be none")
    require(isinstance(observed, dict), "observed artifact observedTarget must be an object")
    if isinstance(observed, dict):
        require(observed.get("procedureName") == expected_selector, "observedTarget.procedureName must be scene.eatmeFirstLesson")
        require(observed.get("kind") in {"procedure-tab", "code-editor", "procedure-code-editor-composite"}, "observedTarget.kind must be procedure-tab, code-editor, or composite")
        require(isinstance(observed.get("automationPath"), str) and observed["automationPath"], "observedTarget.automationPath must be non-empty")
        require(observed.get("readyForDesktopEditAction") is True, "edit-ready observedTarget must record edit-action readiness")
    require(isinstance(desktop_edit_action, dict), "edit-ready artifact must include desktopEditAction readiness evidence")
    if isinstance(desktop_edit_action, dict):
        require(desktop_edit_action.get("status") == "ready", "desktopEditAction.status must be ready")
        require(desktop_edit_action.get("readyForDesktopEditAction") is True, "desktopEditAction readiness must be true")
        nested_blocker = desktop_edit_action.get("blocker")
        require(isinstance(nested_blocker, dict) and nested_blocker == blocker, "desktopEditAction.blocker must mirror the top-level blocker")

out_of_scope = artifact.get("outOfScope")
require(isinstance(out_of_scope, list), "artifact outOfScope must be a list")
if isinstance(out_of_scope, list):
    normalized = {str(item).lower() for item in out_of_scope}
    for required in (
        "desktop procedure edit mutation",
        "save",
        "rendering correctness",
        "learner assessment",
        "creative assessment",
        "full first-lesson completion",
    ):
        require(required in normalized, f"artifact outOfScope must include {required}")

for forbidden in ("credentials", "environment", "screenshotPixels", "sourceContents", "accessibilityTree"):
    require(forbidden not in artifact, f"artifact must not include broad or sensitive field {forbidden}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  artifact_status=$?
  assert_success "$artifact_status" "first-lesson procedure target artifact has structured machine-readable shape"
else
  fail "first-lesson procedure target fallback artifacts could not be inspected"
fi

fake_pyatspi_dir="$tmp_root/fake-pyatspi"
mkdir -p "$fake_pyatspi_dir"
python3 - "$fake_pyatspi_dir/pyatspi.py" >"$tmp_root/write-fake-pyatspi.out" 2>"$tmp_root/write-fake-pyatspi.err" <<'PY'
from pathlib import Path
import sys

Path(sys.argv[1]).write_text(
    """
STATE_SHOWING = "showing"
STATE_VISIBLE = "visible"


class _StateSet:
    def getStates(self):
        return ["showing", "visible", "enabled"]

    def contains(self, state):
        return state in {"showing", "visible"}


class _Accessible:
    def __init__(self, name, role, children=None, pid=None):
        self.name = name
        self._role = role
        self._children = children or []
        self._pid = pid
        self.childCount = len(self._children)

    def getRoleName(self):
        return self._role

    def getState(self):
        return _StateSet()

    def getChildAtIndex(self, index):
        return self._children[index]

    def get_process_id(self):
        return self._pid


_TARGET = _Accessible("scene.eatmeFirstLesson code editor", "panel")
_APP = _Accessible("Alice 3", "application", [_TARGET], pid=4242)


class _Desktop:
    childCount = 1

    def getChildAtIndex(self, index):
        if index != 0:
            raise IndexError(index)
        return _APP


class Registry:
    @staticmethod
    def getDesktop(index):
        if index != 0:
            raise IndexError(index)
        return _Desktop()
""".lstrip(),
    encoding="utf-8",
)
PY
status=$?
assert_success "$status" "fake pyatspi module for observed target seam fixture is written"

mock_input_dir="$tmp_root/mock-observed-input"
mkdir -p "$mock_input_dir"
python3 - \
  "$mock_input_dir/x-window-inventory.json" \
  "$mock_input_dir/tab-click-observation.json" \
  "$mock_input_dir/post-project-open-observation.json" \
  >"$tmp_root/write-mock-input.out" \
  2>"$tmp_root/write-mock-input.err" <<'PY'
import json
from pathlib import Path
import sys

inventory_path, tab_click_path, post_open_path = [Path(arg) for arg in sys.argv[1:4]]
display_name = "Africa Full"
repository_path = "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"

inventory_path.write_text(
    json.dumps(
        {
            "windows": [
                {
                    "title": "Alice 3",
                    "processName": "java",
                    "pid": 4242,
                }
            ]
        },
        indent=2,
        sort_keys=True,
    )
    + "\n",
    encoding="utf-8",
)
tab_click_path.write_text(
    json.dumps(
        {
            "targetStarter": {
                "displayName": display_name,
                "repositoryPath": repository_path,
            },
            "openedStarter": {
                "displayName": display_name,
                "repositoryPath": repository_path,
            },
            "evidenceStatus": "opened",
            "targetStarterSelected": True,
            "targetStarterOpenAttempted": True,
            "projectOpenObserved": True,
        },
        indent=2,
        sort_keys=True,
    )
    + "\n",
    encoding="utf-8",
)
post_open_path.write_text(
    json.dumps({"postOpenWindowObserved": True}, indent=2, sort_keys=True) + "\n",
    encoding="utf-8",
)
PY
status=$?
assert_success "$status" "mock supporting evidence for observed target seam is written"

mock_observed_artifact="$tmp_root/mock-observed-$ARTIFACT"
PYTHONPATH="$fake_pyatspi_dir" python3 "$BASE_DIR/runners/first-lesson-procedure-target-probe.py" \
  --inventory "$mock_input_dir/x-window-inventory.json" \
  --tab-click-observation "$mock_input_dir/tab-click-observation.json" \
  --post-open-window-observation "$mock_input_dir/post-project-open-observation.json" \
  --output "$mock_observed_artifact" \
  --scenario-id "$SCENARIO_ID" \
  --automation-mode xvfb-real-alice \
  --target-starter-display-name "Africa Full" \
  --target-starter-repository-path "core/resources/src/application/resources/starter-projects/AfricaFull.a3p" \
  --procedure-selector "$PROCEDURE_SELECTOR" \
  >"$tmp_root/mock-observed-probe.out" \
  2>"$tmp_root/mock-observed-probe.err"
status=$?
assert_success "$status" "mock observed target probe writes first-lesson action seam artifact"

python3 - \
  "$mock_observed_artifact" \
  "$PROCEDURE_SELECTOR" \
  "$MISSING_DESKTOP_EDIT_ACTION_CONTRACT" \
  >"$tmp_root/mock-observed-contract.out" \
  2>"$tmp_root/mock-observed-contract.err" <<'PY'
import json
import sys

artifact_path, expected_selector, expected_missing_contract = sys.argv[1:4]
artifact = json.load(open(artifact_path, encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(artifact.get("status") == "blocked", "mock probe must produce the accepted no-go action-seam blocker, not target-only observed")
require(artifact.get("seam") == "live-first-lesson-procedure-target-to-desktop-edit-action", "mock probe must report the action seam")
require(artifact.get("downstreamBlockedStep") == "desktop-procedure-edit-action-proof", "artifact must keep the downstream edit-action proof boundary")

top_level_blocker = artifact.get("blocker")
require(isinstance(top_level_blocker, dict), "top-level blocker must be an object")
if isinstance(top_level_blocker, dict):
    require(top_level_blocker.get("kind") == "missing-desktop-edit-action-contract", "top-level blocker kind must be exact")
    require(top_level_blocker.get("message") == expected_missing_contract, "top-level blocker message must name the missing public CodeEditor/CodeComposite edit invocation contract")

observed = artifact.get("observedTarget")
require(isinstance(observed, dict), "observed artifact must include observedTarget")
if isinstance(observed, dict):
    require(observed.get("procedureName") == expected_selector, "observedTarget must bind scene.eatmeFirstLesson")
    require(observed.get("kind") == "code-editor", "mock observed target must be a code-editor target")
    require(observed.get("readyForDesktopEditAction") is False, "observed target must not claim desktop edit-action readiness without a public contract")

desktop_edit_action = artifact.get("desktopEditAction")
require(isinstance(desktop_edit_action, dict), "observed target with no public edit contract must include desktopEditAction")
if isinstance(desktop_edit_action, dict):
    require(desktop_edit_action.get("status") == "blocked", "desktopEditAction must be blocked")
    require(desktop_edit_action.get("readyForDesktopEditAction") is False, "desktopEditAction must record readiness false")
    blocker = desktop_edit_action.get("blocker")
    require(isinstance(blocker, dict), "desktopEditAction.blocker must be an object")
    if isinstance(blocker, dict):
        require(blocker.get("kind") == "missing-desktop-edit-action-contract", "desktopEditAction blocker kind must be exact")
        require(blocker.get("message") == expected_missing_contract, "desktopEditAction blocker message must name the missing public CodeEditor/CodeComposite edit invocation contract")
        require(blocker == top_level_blocker, "desktopEditAction.blocker must mirror the top-level blocker")
    require(
        desktop_edit_action.get("invocationContract") is None,
        "blocked desktopEditAction must not claim an invocation contract",
    )

out_of_scope = {str(item).lower() for item in artifact.get("outOfScope", [])}
for forbidden_claim in ("save", "rendering correctness", "learner assessment", "creative assessment", "full first-lesson completion"):
    require(forbidden_claim in out_of_scope, f"artifact must keep {forbidden_claim} out of scope")

does_not_claim = {str(item).lower() for item in desktop_edit_action.get("doesNotClaim", [])} if isinstance(desktop_edit_action, dict) else set()
for forbidden_claim in ("save", "rendering correctness", "learner assessment", "creative assessment", "full first-lesson completion"):
    require(forbidden_claim in does_not_claim, f"desktopEditAction must keep {forbidden_claim} out of scope")

if errors:
    raise AssertionError("\n".join(errors))
PY
status=$?
assert_success "$status" "observed target artifact records exact CodeEditor/CodeComposite edit-action no-go blocker"

finish
