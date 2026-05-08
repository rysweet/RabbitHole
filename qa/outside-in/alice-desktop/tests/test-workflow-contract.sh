#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

"$RUNNER" list >"$tmp_root/list.out" 2>"$tmp_root/list.err"
status=$?
assert_success "$status" "runner lists scenario catalog"

"$VALIDATOR" --dump-json >"$tmp_root/catalog.json" 2>"$tmp_root/catalog.err"
status=$?
assert_success "$status" "validator dumps scenario catalog for workflow checks"

python3 - "$tmp_root/catalog.json" >"$tmp_root/workflow-contract.out" 2>"$tmp_root/workflow-contract.err" <<'PY'
from collections import Counter
import json
import sys

with open(sys.argv[1], encoding="utf-8") as catalog_file:
    catalog_list = json.load(catalog_file)
catalog = {scenario["id"]: scenario for scenario in catalog_list}
workflow_counts = Counter(scenario["workflow"] for scenario in catalog_list)
required_workflows = [
    "instructor-student-setup",
    "launch",
    "menu-action-smoke",
    "scene-creation",
    "run-debug",
    "save-load",
    "open-load-save",
    "export",
    "exported-project-smoke",
    "netbeans-package-smoke",
    "package-install-smoke",
    "project-io-smoke",
    "failure-path-smoke",
    "future-ui-smoke",
    "wizard-palette-completion-smoke",
]
manual_scenarios = [
    "alice-desktop-instructor-student-setup",
    "alice-desktop-scene-creation",
    "alice-desktop-run-debug",
    "alice-desktop-save-load",
    "alice-desktop-open-load-save",
    "alice-desktop-export",
]
gated_scenarios = [
    "alice-desktop-exported-project-smoke",
    "alice-desktop-netbeans-package-smoke",
    "alice-desktop-package-install-smoke",
    "alice-desktop-project-io-smoke",
    "alice-desktop-failure-path-smoke",
    "alice-desktop-future-ui-smoke",
    "alice-desktop-menu-action-smoke",
    "alice-desktop-wizard-palette-completion-smoke",
]
errors = []

for workflow in required_workflows:
    count = workflow_counts[workflow]
    if count != 1:
        errors.append(f"catalog must contain exactly one {workflow} workflow scenario, found {count}")

for scenario_id in manual_scenarios:
    scenario = catalog.get(scenario_id)
    if scenario is None:
        errors.append(f"catalog must contain {scenario_id}")
        continue
    if scenario["automationMode"] != "manual-evidence-required":
        errors.append(f"{scenario_id} must remain manual-evidence-required until GUI automation exists")
    supporting = scenario.get("supportingEvidence", [])
    if "alice-desktop-launch" not in supporting:
        errors.append(f"{scenario_id} must link alice-desktop-launch as supportingEvidence")
    evidence_text = "\n".join(scenario["evidence"]["required"]).lower()
    if "screenshot" not in evidence_text:
        errors.append(f"{scenario_id} must require screenshot evidence")
    if not any(token in evidence_text for token in ("a3p", "artifact", "log", "notes")):
        errors.append(f"{scenario_id} must require a durable artifact, log, or notes")
    if "review-notes.txt" not in evidence_text:
        errors.append(f"{scenario_id} must require review-notes.txt for manual acceptance")

for scenario_id in gated_scenarios:
    scenario = catalog.get(scenario_id)
    if scenario is None:
        errors.append(f"catalog must contain {scenario_id}")
        continue
    if scenario["automationMode"] != "gated-command-smoke":
        errors.append(f"{scenario_id} must use gated-command-smoke to avoid mandatory heavy GUI/build work")
    automation = scenario.get("automation", {})
    for field in ("cwd", "argv", "timeoutSeconds", "readyWaitSeconds"):
        if field not in automation:
            errors.append(f"{scenario_id} automation must include {field}")
    if "immediate-qa-backlog" not in scenario.get("tags", []):
        errors.append(f"{scenario_id} must be tagged as immediate-qa-backlog coverage")
    evidence_text = "\n".join(scenario["evidence"]["required"]).lower()
    if "status.txt" not in evidence_text:
        errors.append(f"{scenario_id} must require status.txt evidence")
    if not any(token in evidence_text for token in ("command.log", "artifact", "project", "failure")):
        errors.append(f"{scenario_id} must require command, artifact, project, or failure-path evidence")

if errors:
    raise AssertionError("\n".join(errors))
PY
status=$?
assert_success "$status" "workflow catalog counts and manual evidence contract are valid"

"$RUNNER" run alice-desktop-instructor-student-setup --evidence-dir "$tmp_root/evidence" >"$tmp_root/manual.out" 2>"$tmp_root/manual.err"
status=$?
assert_success "$status" "workflow runner prepares instructor/student evidence"
run_dir=$(single_child_dir "$tmp_root/evidence/alice-desktop-instructor-student-setup")
status=$?
assert_success "$status" "workflow runner creates one evidence directory"
checklist="$run_dir/manual-evidence-checklist.txt"
assert_file_exists "$checklist" "workflow runner writes manual checklist"
assert_contains "$checklist" '^Required evidence$' "workflow checklist includes required evidence section"
assert_contains "$checklist" '^Completion status$' "workflow checklist includes completion status section"
assert_contains "$checklist" 'Human reviewer|human performs the workflow' "workflow checklist names human review requirement"

finish
