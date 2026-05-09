#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-run-window-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCHEMA="$BASE_DIR/schema/scenario.schema.json"
SCENARIO_ID=alice-desktop-run-window-contract
WORKFLOW=run-window-contract
SCENARIO_FILE="$BASE_DIR/scenarios/run-window-contract.yaml"
ARTIFACT=run-window-created.json
TEST_SELECTOR=org.alice.tools.EatmeRunWindowEvidenceTest
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_file_exists "$SCENARIO_FILE" "Run-window contract scenario file exists"

"$VALIDATOR" --dump-json "$SCENARIO_ID" >"$tmp_root/scenario.json" 2>"$tmp_root/scenario.err"
status=$?
assert_success "$status" "validator dumps the Run-window contract scenario by id"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$tmp_root/scenario.json" \
    "$SCENARIO_ID" \
    "$WORKFLOW" \
    "$ARTIFACT" \
    "$TEST_SELECTOR" \
    >"$tmp_root/scenario-contract.out" \
    2>"$tmp_root/scenario-contract.err" <<'PY'
import json
import re
import sys

scenario_path, expected_id, expected_workflow, expected_artifact, expected_selector = sys.argv[1:6]
scenario = json.load(open(scenario_path, encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


expected_argv = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ide",
    "-am",
    f"-Dtest={expected_selector}",
    "test",
]

require(scenario.get("id") == expected_id, f"id must be {expected_id!r}")
require(scenario.get("workflow") == expected_workflow, f"workflow must be {expected_workflow!r}")
require(scenario.get("automationMode") == "gated-command-smoke", "scenario must use the gated command lane")
automation = scenario.get("automation", {})
require(automation.get("cwd") == ".", "automation.cwd must stay at the repository root")
require(automation.get("argv") == expected_argv, "automation.argv must be the exact focused Maven seam command")
require(isinstance(automation.get("readyWaitSeconds"), int), "automation.readyWaitSeconds must be explicit")

scenario_text = json.dumps(scenario, sort_keys=True)
scenario_lower = scenario_text.lower()
for required in (
    "run-window-created.json",
    "schema_version=eatme.alice-run-window-created/v1",
    "status=created",
    "contract_scope=run-window-creation-wiring",
    "evidence_source=org.alice.stageide.run.runcomposite#handlepreshowwindow",
    "artifact=run-window-created.json",
    "does_not_claim",
    "active_rendering_claimed=false",
    "run_program_claimed=false",
    "run_execution_claimed=false",
    "world_execution_claimed=false",
    "rendering_correctness_claimed=false",
    "save_claimed=false",
    "grading_claimed=false",
    "full_ui_automation_claimed=false",
):
    require(required in scenario_lower, f"scenario must require {required}")

for boundary in (
    "active-rendering",
    "run-execution",
    "world-execution-correctness",
    "rendering-correctness",
    "save",
    "grading",
    "full-ui-automation",
):
    require(boundary in scenario_lower, f"scenario must name non-claim boundary {boundary}")

evidence_text = "\n".join(scenario.get("evidence", {}).get("required", []))
for required in ("status.txt", "command.log", expected_artifact, expected_selector):
    require(required in evidence_text or required in scenario_text, f"scenario must wire evidence for {required}")

claim_text = "\n\n".join([
    scenario.get("title", ""),
    "\n".join(scenario.get("preconditions", [])),
    "\n".join(scenario.get("userActions", [])),
    "\n".join(scenario.get("expectedOutcomes", [])),
    evidence_text,
    "\n".join(scenario.get("fallback", {}).get("notes", [])),
])
for forbidden in (
    "run the program",
    "program runs",
    "program behavior",
    "world execution",
    "visual correctness",
    "visible correctness",
    "rendering correctness",
    "save coverage",
    "save behavior",
    "grading",
    "full ui automation",
    "full-ui-automation",
):
    matches = [paragraph for paragraph in re.split(r"\n\s*\n", claim_text.lower()) if forbidden in paragraph]
    for paragraph in matches:
        if not any(marker in paragraph for marker in ("not ", "no ", "does not", "do not", "unclaimed", "non-claim", "outside", "without")):
            errors.append(f"scenario must not overclaim {forbidden}: {paragraph[:160]}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  scenario_contract_status=$?
else
  printf 'scenario was not dumped; skipping detailed scenario contract\n' >"$tmp_root/scenario-contract.err"
  scenario_contract_status=1
fi
assert_success "$scenario_contract_status" "scenario contract is creation/wiring-only and boundary-explicit"

python3 - "$SCHEMA" "$WORKFLOW" "$TEST_SELECTOR" >"$tmp_root/schema-contract.out" 2>"$tmp_root/schema-contract.err" <<'PY'
import json
import sys

schema_path, expected_workflow, expected_selector = sys.argv[1:4]
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
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ide",
    "-am",
    f"-Dtest={expected_selector}",
    "test",
)
if expected_argv not in allowed_argv:
    raise AssertionError("schema must allow only the focused Run-window evidence seam argv for this lane")
PY
status=$?
assert_success "$status" "schema recognizes the Run-window contract workflow and exact focused argv"

assert_contains "$VALIDATOR" "\"$WORKFLOW\"" "validator allowlists the Run-window contract workflow"
assert_contains "$VALIDATOR" "$TEST_SELECTOR" "validator allowlists the exact Run-window contract Maven selector"
assert_contains "$RUNNER" "$TEST_SELECTOR" "runner allowlist permits the focused Run-window contract Maven selector"
assert_contains "$RUNNER" 'org\.alice\.eatme\.runWindowEvidenceDir' "runner injects the Run-window evidence directory property"

"$RUNNER" list >"$tmp_root/list.out" 2>"$tmp_root/list.err"
status=$?
assert_success "$status" "runner lists the scenario catalog with the Run-window contract lane"
assert_contains "$tmp_root/list.out" "$SCENARIO_ID" "runner list includes the Run-window contract scenario"

prepare_evidence="$tmp_root/prepare-evidence"
"$RUNNER" run "$SCENARIO_ID" --prepare-only --evidence-dir "$prepare_evidence" >"$tmp_root/prepare.out" 2>"$tmp_root/prepare.err"
status=$?
assert_success "$status" "prepare-only Run-window contract scenario records an intentional gated skip"

run_dir=$(single_child_dir "$prepare_evidence/$SCENARIO_ID")
run_dir_status=$?
assert_success "$run_dir_status" "prepare-only Run-window contract scenario creates one evidence directory"
if [ "$run_dir_status" -eq 0 ]; then
  status_file="$run_dir/status.txt"
  checklist="$run_dir/manual-evidence-checklist.txt"
  assert_file_exists "$status_file" "prepare-only Run-window contract writes status.txt"
  assert_file_exists "$checklist" "prepare-only Run-window contract writes manual evidence checklist"
  assert_contains "$status_file" "^scenario=$SCENARIO_ID$" "status records Run-window contract scenario id"
  assert_contains "$status_file" '^automationMode=gated-command-smoke$' "status records gated command mode"
  assert_contains "$status_file" '^outcome=gated-not-run$' "status records intentional gated skip"
  assert_contains "$status_file" '^skipMode=prepare-only$' "status records prepare-only skip mode"
  assert_contains "$checklist" "$ARTIFACT" "prepare-only checklist names the fixed Run-window artifact"
  assert_contains "$checklist" 'contract_scope=run-window-creation-wiring' "prepare-only checklist preserves contract scope"
  assert_contains "$checklist" 'rendering_correctness_claimed=false' "prepare-only checklist preserves rendering non-claim"
  assert_contains "$checklist" 'full_ui_automation_claimed=false' "prepare-only checklist preserves full UI automation non-claim"
fi

fake_bin="$tmp_root/bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/mvn" <<'SH'
#!/usr/bin/env bash
set -eu
evidence_dir=
for arg in "$@"; do
  case "$arg" in
    -Dorg.alice.eatme.runWindowEvidenceDir=*) evidence_dir=${arg#*=} ;;
  esac
done
if [ -z "$evidence_dir" ] || [ ! -d "$evidence_dir" ]; then
  printf 'missing Run-window evidence directory property\n' >&2
  printf 'argv=%s\n' "$*" >&2
  exit 64
fi
if [ "${ALICE_RUN_WINDOW_EVIDENCE_DIR:-}" != "$evidence_dir" ]; then
  printf 'missing Run-window evidence directory environment binding\n' >&2
  printf 'env=%s property=%s\n' "${ALICE_RUN_WINDOW_EVIDENCE_DIR:-}" "$evidence_dir" >&2
  exit 65
fi
python3 - "$evidence_dir/run-window-created.json" <<'PY'
import json
import sys
from pathlib import Path

artifact = Path(sys.argv[1])
payload = {
    "schema_version": "eatme.alice-run-window-created/v1",
    "status": "created",
    "contract_scope": "run-window-creation-wiring",
    "evidence_source": "org.alice.stageide.run.RunComposite#handlePreShowWindow",
    "artifact": "run-window-created.json",
    "frame_title": "Run Alice",
    "program_type": "Program",
    "active_rendering_claimed": False,
    "run_program_claimed": False,
    "run_execution_claimed": False,
    "world_execution_claimed": False,
    "rendering_correctness_claimed": False,
    "save_claimed": False,
    "grading_claimed": False,
    "full_ui_automation_claimed": False,
    "does_not_claim": [
        "active-rendering",
        "run-execution",
        "world-execution-correctness",
        "rendering-correctness",
        "save",
        "grading",
        "full-ui-automation",
    ],
}
artifact.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
PY
printf 'fake Run-window seam completed\n'
printf 'argv=%s\n' "$*"
SH
chmod +x "$fake_bin/mvn"

enabled_evidence="$tmp_root/enabled-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$enabled_evidence" >"$tmp_root/enabled.out" 2>"$tmp_root/enabled.err"
status=$?
assert_success "$status" "enabled Run-window contract runner validates canonical evidence"
enabled_run_dir=$(single_child_dir "$enabled_evidence/$SCENARIO_ID")
enabled_status=$?
assert_success "$enabled_status" "enabled Run-window contract creates one evidence directory"
if [ "$enabled_status" -eq 0 ]; then
  artifact_path="$enabled_run_dir/$ARTIFACT"
  assert_file_exists "$enabled_run_dir/command.log" "enabled Run-window contract writes command.log"
  assert_file_exists "$artifact_path" "enabled Run-window contract writes canonical evidence artifact"
  assert_file_exists "$enabled_run_dir/run-window-validation.log" "enabled Run-window contract writes validation log"
  assert_contains "$enabled_run_dir/command.log" 'org\.alice\.eatme\.runWindowEvidenceDir=' "Maven command receives Run-window evidence directory property"
  assert_contains "$enabled_run_dir/status.txt" '^outcome=passed$' "enabled Run-window contract records pass outcome"
  assert_contains "$enabled_run_dir/status.txt" '^runWindowEvidence=run-window-created\.json$' "status links canonical Run-window evidence"
  assert_contains "$enabled_run_dir/status.txt" '^runWindowEvidenceStatus=created$' "status records validated Run-window evidence"
  assert_contains "$enabled_run_dir/run-window-validation.log" 'Run-window evidence created' "validation log records created Run-window evidence"
  python3 - "$artifact_path" >"$tmp_root/enabled-artifact.out" 2>"$tmp_root/enabled-artifact.err" <<'PY'
import json
import sys

payload = json.load(open(sys.argv[1], encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


for key, expected in {
    "schema_version": "eatme.alice-run-window-created/v1",
    "status": "created",
    "contract_scope": "run-window-creation-wiring",
    "evidence_source": "org.alice.stageide.run.RunComposite#handlePreShowWindow",
    "artifact": "run-window-created.json",
}.items():
    require(payload.get(key) == expected, f"{key} must be {expected!r}")

for field in (
    "active_rendering_claimed",
    "run_program_claimed",
    "run_execution_claimed",
    "world_execution_claimed",
    "rendering_correctness_claimed",
    "save_claimed",
    "grading_claimed",
    "full_ui_automation_claimed",
):
    require(payload.get(field) is False, f"{field} must be false")

does_not_claim = set(payload.get("does_not_claim", []))
for claim in (
    "active-rendering",
    "run-execution",
    "world-execution-correctness",
    "rendering-correctness",
    "save",
    "grading",
    "full-ui-automation",
):
    require(claim in does_not_claim, f"does_not_claim must include {claim}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  artifact_status=$?
  assert_success "$artifact_status" "enabled Run-window artifact keeps required contract fields and non-claims"
fi

finish
