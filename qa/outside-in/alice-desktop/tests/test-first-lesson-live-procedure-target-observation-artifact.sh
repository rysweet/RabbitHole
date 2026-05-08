#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-first-lesson-live-procedure-target-observation-artifact.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCHEMA="$BASE_DIR/schema/scenario.schema.json"
SCENARIO_ID=alice-desktop-first-lesson-live-procedure-target-observation
WORKFLOW=first-lesson-live-procedure-target-observation
SCENARIO_FILE="$BASE_DIR/scenarios/first-lesson-live-procedure-target-observation.yaml"
ARTIFACT=first-lesson-live-procedure-target-observation.json
SEAM=live-first-lesson-project-open-to-procedure-target-observable
PROCEDURE_SELECTOR=scene.eatmeFirstLesson
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
):
    require(required.lower() in scenario_lower, f"scenario must tie the seam to {required}")

evidence_text = "\n".join(scenario.get("evidence", {}).get("required", []))
for required in (
    "status.txt",
    expected_artifact,
    "procedureTargetObservationEvidence",
    "procedureTargetObservationStatus",
    "procedureTargetObservationBlocker",
    "downstreamBlockedStep=desktop-procedure-edit",
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
ALICE_QA_DISABLE_XVFB=1 "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$evidence_dir" >"$tmp_root/no-xvfb.out" 2>"$tmp_root/no-xvfb.err"
status=$?
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
  assert_contains "$status_file" '^downstreamBlockedStep=desktop-procedure-edit$' "status names the downstream edit step"

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
require(artifact.get("status") in {"observed", "blocked"}, "artifact status must be observed or blocked")
require(artifact.get("seam") == expected_seam, "artifact seam must name the live first-lesson target observation transition")
require(artifact.get("downstreamBlockedStep") == "desktop-procedure-edit", "artifact must name desktop-procedure-edit as the downstream blocked step")

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
    require(required_target.get("procedureSelector") == expected_selector, "requiredTarget.procedureSelector must be scene.eatmeFirstLesson")
    require(required_target.get("targetKind") == "procedure-tab-or-code-editor", "requiredTarget.targetKind must name the accepted target seam")
    require(
        required_target.get("minimumStableAutomationTarget") == "reacquirable live desktop procedure tab or code-editor target",
        "requiredTarget.minimumStableAutomationTarget must name the stable automation target",
    )

if artifact.get("status") == "blocked":
    require(artifact.get("observedTarget") is None, "blocked artifact observedTarget must be null")
    require(artifact.get("blocker") in {
        "select-project-open-not-observed",
        "post-open-window-not-observed",
        "procedure-target-not-found",
        "procedure-target-not-stable",
        "at-spi-or-atk-unavailable",
        "display-prerequisite-unavailable",
    }, "blocked artifact must use an accepted blocker code")
    require(isinstance(artifact.get("blockerDetail"), str) and artifact.get("blockerDetail"), "blocked artifact must include blockerDetail")
else:
    observed = artifact.get("observedTarget")
    require(artifact.get("blocker") == "none", "observed artifact blocker must be none")
    require(isinstance(observed, dict), "observed artifact observedTarget must be an object")
    if isinstance(observed, dict):
        require(observed.get("procedureSelector") == expected_selector, "observedTarget.procedureSelector must be scene.eatmeFirstLesson")
        require(observed.get("targetKind") in {"procedure-tab", "code-editor"}, "observedTarget.targetKind must be procedure-tab or code-editor")
        require(isinstance(observed.get("automationPath"), str) and observed["automationPath"], "observedTarget.automationPath must be non-empty")
        require(observed.get("readyForDesktopEditAction") is True, "observedTarget must be ready for the next desktop edit action")

out_of_scope = artifact.get("outOfScope")
require(isinstance(out_of_scope, list), "artifact outOfScope must be a list")
if isinstance(out_of_scope, list):
    normalized = {str(item).lower() for item in out_of_scope}
    for required in (
        "desktop procedure edit mutation",
        "save",
        "rendering correctness",
        "learner assessment",
        "full first-lesson completion",
    ):
        require(required in normalized, f"artifact outOfScope must include {required}")

for forbidden in ("credentials", "environment", "screenshotPixels", "sourceContents", "accessibilityTree"):
    require(forbidden not in artifact, f"artifact must not include broad or sensitive field {forbidden}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  artifact_status=$?
  assert_success "$artifact_status" "first-lesson procedure target artifact has observed-or-blocked machine-readable shape"
else
  fail "first-lesson procedure target fallback artifacts could not be inspected"
fi

finish
