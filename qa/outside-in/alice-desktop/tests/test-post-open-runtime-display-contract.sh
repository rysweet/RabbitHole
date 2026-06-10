#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCHEMA="$BASE_DIR/schema/scenario.schema.json"
SCENARIO_ID=alice-desktop-post-open-runtime-display-accessibility-evidence
WORKFLOW=post-open-runtime-display-accessibility-evidence
SCENARIO_FILE="$BASE_DIR/scenarios/post-open-runtime-display-accessibility-evidence.yaml"
ARTIFACT=post-open-runtime-display-accessibility-evidence.json
TARGET_STARTER_DISPLAY_NAME="Africa Full"
TARGET_STARTER_REPOSITORY_PATH="core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_file_exists "$SCENARIO_FILE" "post-open runtime/display scenario file exists"

"$VALIDATOR" --dump-json "$SCENARIO_ID" >"$tmp_root/scenario.json" 2>"$tmp_root/scenario.err"
status=$?
assert_success "$status" "validator dumps the post-open runtime/display scenario by id"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$tmp_root/scenario.json" \
    "$SCENARIO_ID" \
    "$WORKFLOW" \
    "$ARTIFACT" \
    "$TARGET_STARTER_DISPLAY_NAME" \
    "$TARGET_STARTER_REPOSITORY_PATH" \
    >"$tmp_root/scenario-contract.out" \
    2>"$tmp_root/scenario-contract.err" <<'PY'
import json
import re
import sys

(
    scenario_path,
    expected_id,
    expected_workflow,
    expected_artifact,
    expected_target_display_name,
    expected_target_repository_path,
) = sys.argv[1:7]
scenario = json.load(open(scenario_path, encoding="utf-8"))
errors = []

if scenario.get("id") != expected_id:
    errors.append(f"id must be {expected_id!r}")
if scenario.get("workflow") != expected_workflow:
    errors.append(f"workflow must be {expected_workflow!r}")
if scenario.get("automationMode") != "xvfb-real-alice":
    errors.append("scenario must use xvfb-real-alice so runtime/display evidence is executable")

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
if automation.get("cwd") != "alice-ide":
    errors.append("automation.cwd must reuse the Alice IDE launch path")
if automation.get("argv") != expected_argv:
    errors.append("automation.argv must launch Alice with the AT-SPI wrapper execution")
if not isinstance(automation.get("timeoutSeconds"), int) or automation["timeoutSeconds"] < 1:
    errors.append("automation.timeoutSeconds must be a positive integer")
if not isinstance(automation.get("readyWaitSeconds"), int) or automation["readyWaitSeconds"] < 1:
    errors.append("automation.readyWaitSeconds must be a positive integer")

target_starter = scenario.get("targetStarter")
if not isinstance(target_starter, dict):
    errors.append("scenario must declare targetStarter so the reused post-open setup opens a fixed project")
else:
    if target_starter.get("displayName") != expected_target_display_name:
        errors.append(f"targetStarter.displayName must be {expected_target_display_name!r}")
    if target_starter.get("repositoryPath") != expected_target_repository_path:
        errors.append(f"targetStarter.repositoryPath must be {expected_target_repository_path!r}")

evidence_text = "\n".join(scenario.get("evidence", {}).get("required", []))
expected_outcomes = "\n".join(scenario.get("expectedOutcomes", []))
user_actions = "\n".join(scenario.get("userActions", []))
all_claim_text = "\n".join([scenario.get("title", ""), expected_outcomes, evidence_text])

for required in (
    "status.txt",
    expected_artifact,
    "postOpenRuntimeDisplayAccessibilityObserved",
    "runtimeDisplayCandidateCount",
    "runtimeDisplayCandidates",
    "controlledDisplayPixelStatus",
    "runtime-display-accessibility-status.txt",
):
    if required not in evidence_text and required not in expected_outcomes:
        errors.append(f"scenario must require machine-readable {required} evidence")

if "post-open runtime/display accessibility evidence" not in all_claim_text.lower():
    errors.append("claim text must use narrow post-open runtime/display accessibility evidence wording")

for forbidden in (
    "rendering correctness",
    "full rendering",
    "world execution",
    "lesson completion",
    "grading",
    "installer success",
):
    if forbidden in all_claim_text.lower():
        errors.append(f"scenario must not claim {forbidden}")

for forbidden_scope in ("ProjectSave", "ProjectSaveTargetPlan", "story-api-migration", "decoder"):
    if forbidden_scope.lower() in user_actions.lower() or forbidden_scope.lower() in evidence_text.lower():
        errors.append(f"scenario must avoid active Save/decoder scope: {forbidden_scope}")

if re.search(r"\b(select project|select-project)\b", expected_outcomes.lower()):
    errors.append("expected outcomes must not make the evidence claim about Select Project")

supporting = set(scenario.get("supportingEvidence", []))
if "alice-desktop-post-project-open-window-state" not in supporting:
    errors.append("scenario must reuse existing post-open setup as supporting evidence")

if errors:
    raise AssertionError("\n".join(errors))
PY
  scenario_contract_status=$?
else
  printf 'scenario was not dumped; skipping detailed scenario contract\n' >"$tmp_root/scenario-contract.err"
  scenario_contract_status=1
fi
assert_success "$scenario_contract_status" "scenario contract is narrow, executable, and avoids excluded scopes"

"$RUNNER" list >"$tmp_root/list.out" 2>"$tmp_root/list.err"
status=$?
assert_success "$status" "runner lists scenario catalog for post-open runtime/display workflow"
assert_contains "$tmp_root/list.out" "$SCENARIO_ID" "runner list includes the new post-open runtime/display scenario"

python3 - "$SCHEMA" "$WORKFLOW" >"$tmp_root/schema-workflow.out" 2>"$tmp_root/schema-workflow.err" <<'PY'
import json
import sys

schema_path, expected_workflow = sys.argv[1:3]
schema = json.load(open(schema_path, encoding="utf-8"))
workflow_enum = schema["properties"]["workflow"]["enum"]
if expected_workflow not in workflow_enum:
    raise AssertionError(f"schema workflow enum is missing {expected_workflow}")

automation = schema["properties"]["automation"]["properties"]["argv"]
allowed_argv = {
    tuple(item.get("const") for item in option.get("prefixItems", []))
    for option in automation.get("oneOf", [])
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
    raise AssertionError("schema must allow the AT-SPI Alice launch argv used by this workflow")
PY
status=$?
assert_success "$status" "schema recognizes the post-open runtime/display workflow and AT-SPI argv"

assert_contains "$RUNNER" '^python_with_module\(\) \{' "runner can select a Python interpreter by required module"
assert_contains "$RUNNER" 'python=\$\(python_with_module pyatspi\)' "runner uses a pyatspi-capable Python for AT-SPI probes"
assert_contains "$RUNNER" '"\$python" "\$POST_PROJECT_OPEN_PROBE"' "post-open setup probe escapes uvx Python when needed"
assert_contains "$RUNNER" '"\$python" "\$POST_OPEN_RUNTIME_DISPLAY_PROBE"' "runtime/display probe escapes uvx Python when needed"
assert_contains "$VALIDATOR" '"alice-desktop-post-open-runtime-display-accessibility-evidence"' "validator requires targetStarter for runtime/display post-open setup"
assert_contains "$RUNNER" 'target_starter_display_name=\$\{automation_fields\[5\]:\?\}' "runner fails closed when runtime/display targetStarter display name is missing"
assert_contains "$RUNNER" 'target_starter_repo_path=\$\{automation_fields\[6\]:\?\}' "runner fails closed when runtime/display targetStarter repository path is missing"

evidence_dir="$tmp_root/no-xvfb-evidence"
set +e
ALICE_QA_DISABLE_XVFB=1 "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$evidence_dir" >"$tmp_root/no-xvfb.out" 2>"$tmp_root/no-xvfb.err"
status=$?
set -e
assert_exit_code "$status" 2 "missing Xvfb produces a structured blocker for the runtime/display scenario"
run_dir=$(single_child_dir "$evidence_dir/$SCENARIO_ID")
run_dir_status=$?
assert_success "$run_dir_status" "runtime/display fallback creates one evidence directory"
if [ "$run_dir_status" -eq 0 ]; then
  status_file="$run_dir/status.txt"
  artifact_file="$run_dir/$ARTIFACT"
  assert_file_exists "$status_file" "runtime/display fallback writes status.txt"
  assert_file_exists "$artifact_file" "runtime/display fallback writes structured JSON artifact"
  assert_contains "$status_file" "^scenario=$SCENARIO_ID$" "status records scenario id"
  assert_contains "$status_file" '^automationMode=xvfb-real-alice$' "status records automation mode"
  assert_contains "$status_file" '^outcome=blocked$' "status records blocked outcome"
  assert_contains "$status_file" "^runtimeDisplayAccessibilityEvidence=$ARTIFACT$" "status points to runtime/display artifact"
  assert_contains "$status_file" '^runtimeDisplayAccessibilityStatus=blocked$' "status records runtime/display blocked status"
  assert_contains "$status_file" '^runtimeDisplayAccessibilityBlocker=x-server-unavailable$' "status records exact display blocker"
  assert_contains "$status_file" '^controlledDisplayPixelStatus=blocked$' "status records controlled display blocked status"
  assert_contains "$status_file" '^controlledDisplayPixelBlocker=x-server-unavailable$' "status records exact controlled display blocker"
  assert_contains "$artifact_file" '"status": "blocked"' "artifact records blocked status"
  assert_contains "$artifact_file" '"blocker": "x-server-unavailable"' "artifact names missing X server blocker"
  assert_contains "$artifact_file" '"claim": "post-open-runtime-display-accessibility-evidence"' "artifact uses narrow claim token"
  assert_contains "$artifact_file" '"postOpenRuntimeDisplayAccessibilityObserved": false' "blocked artifact does not claim observation"
  assert_contains "$artifact_file" '"runtimeDisplayCandidateCount": 0' "blocked artifact does not invent candidates"
else
  fail "runtime/display fallback artifacts could not be inspected"
fi

finish
