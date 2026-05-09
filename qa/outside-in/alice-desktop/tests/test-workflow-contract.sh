#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCHEMA="$BASE_DIR/schema/scenario.schema.json"
QA_REFERENCE_DOC="$BASE_DIR/../../../docs/reference/alice-desktop-outside-in-qa.md"
QA_HOWTO_DOC="$BASE_DIR/../../../docs/howto/alice-desktop-outside-in-qa.md"
QA_TUTORIAL_DOC="$BASE_DIR/../../../docs/tutorials/alice-desktop-outside-in-qa.md"
README_DOC="$BASE_DIR/README.md"
PROCEDURE_EDIT_SEAM_DOC="$BASE_DIR/../../../docs/reference/first-lesson-procedure-edit-seam.md"
LEARNER_WORLD_BOUNDARY="$BASE_DIR/contracts/learner-world-assessment-boundary.json"
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

python3 - "$tmp_root/catalog.json" "$SCHEMA" "$VALIDATOR" "$QA_REFERENCE_DOC" "$QA_HOWTO_DOC" "$QA_TUTORIAL_DOC" "$README_DOC" "$PROCEDURE_EDIT_SEAM_DOC" "$LEARNER_WORLD_BOUNDARY" >"$tmp_root/workflow-contract.out" 2>"$tmp_root/workflow-contract.err" <<'PY'
from collections import Counter
import json
import re
import sys
from pathlib import Path

with open(sys.argv[1], encoding="utf-8") as catalog_file:
    catalog_list = json.load(catalog_file)
schema = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
validator_text = Path(sys.argv[3]).read_text(encoding="utf-8")
qa_reference_text = Path(sys.argv[4]).read_text(encoding="utf-8")
qa_howto_text = Path(sys.argv[5]).read_text(encoding="utf-8")
qa_tutorial_text = Path(sys.argv[6]).read_text(encoding="utf-8")
readme_text = Path(sys.argv[7]).read_text(encoding="utf-8")
procedure_edit_seam_text = Path(sys.argv[8]).read_text(encoding="utf-8")
learner_world_boundary_path = Path(sys.argv[9])
if learner_world_boundary_path.is_file():
    learner_world_boundary_text = learner_world_boundary_path.read_text(encoding="utf-8")
    learner_world_boundary = json.loads(learner_world_boundary_text)
else:
    learner_world_boundary_text = ""
    learner_world_boundary = {}
catalog = {scenario["id"]: scenario for scenario in catalog_list}
workflow_counts = Counter(scenario["workflow"] for scenario in catalog_list)
required_workflows = [
    "archive-fixture-smoke",
    "export",
    "exported-project-smoke",
    "failure-path-smoke",
    "file-loader-smoke",
    "first-lesson-live-procedure-target-observation",
    "future-ui-smoke",
    "instructor-student-setup",
    "launch",
    "menu-action-smoke",
    "netbeans-package-smoke",
    "open-load-save",
    "package-install-smoke",
    "post-open-runtime-display-accessibility-evidence",
    "post-project-open-window-state-smoke",
    "procedure-edit-handoff-smoke",
    "procedure-edit-seam-smoke",
    "project-io-smoke",
    "scene-creation",
    "run-debug",
    "save-load",
    "save-menu-dialog-write-proof",
    "select-project-atk-exec-smoke",
    "select-project-interaction-smoke",
    "select-project-tab-click-smoke",
    "select-project-widget-introspection-smoke",
    "tweedle-decoder-boundary-smoke",
    "tweedle-decoder-this-call-smoke",
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
manual_command_scenarios = [
    "alice-desktop-procedure-edit-seam-smoke",
]
gated_scenarios = [
    "alice-desktop-exported-project-smoke",
    "alice-desktop-netbeans-package-smoke",
    "alice-desktop-package-install-smoke",
    "alice-desktop-project-io-smoke",
    "alice-desktop-failure-path-smoke",
    "alice-desktop-future-ui-smoke",
    "alice-desktop-menu-action-smoke",
    "alice-desktop-procedure-edit-handoff-smoke",
    "alice-desktop-save-menu-dialog-write-proof",
    "alice-desktop-tweedle-decoder-boundary-smoke",
    "alice-desktop-tweedle-decoder-this-call-smoke",
    "alice-desktop-wizard-palette-completion-smoke",
]
errors = []

scenario_workflows = {scenario["workflow"] for scenario in catalog_list}
schema_workflows = set(schema["properties"]["workflow"]["enum"])
validator_match = re.search(r"workflow_values = \{(.*?)\n\}", validator_text, re.S)
if not validator_match:
    errors.append("validator workflow_values set could not be parsed")
    validator_workflows = set()
else:
    validator_workflows = set(re.findall(r'"([^"]+)"', validator_match.group(1)))

def documented_workflows(text, marker):
    match = re.search(r"### Workflow values\n\n```text\n(.*?)\n```", text, re.S)
    if marker == "readme":
        match = re.search(r"Use one of the supported workflows:\n\n```text\n(.*?)\n```", text, re.S)
    if not match:
        errors.append(f"{marker} workflow list could not be parsed")
        return set()
    return {line.strip() for line in match.group(1).splitlines() if line.strip()}

qa_reference_workflows = documented_workflows(qa_reference_text, "qa reference")
readme_workflows = documented_workflows(readme_text, "readme")
for name, workflows in (
    ("schema", schema_workflows),
    ("validator", validator_workflows),
    ("QA reference docs", qa_reference_workflows),
    ("README docs", readme_workflows),
):
    if workflows != scenario_workflows:
        errors.append(
            f"{name} workflows must match checked-in scenarios exactly; "
            f"missing={sorted(scenario_workflows - workflows)} extra={sorted(workflows - scenario_workflows)}"
        )

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

for scenario_id in manual_command_scenarios:
    scenario = catalog.get(scenario_id)
    if scenario is None:
        errors.append(f"catalog must contain {scenario_id}")
        continue
    if scenario["automationMode"] != "manual-evidence-required":
        errors.append(f"{scenario_id} must use manual-evidence-required to avoid workflow timeout fields")
    if "automation" in scenario:
        errors.append(f"{scenario_id} must not define automation while the focused proof forbids workflow timeouts")
    if "immediate-qa-backlog" not in scenario.get("tags", []):
        errors.append(f"{scenario_id} must be tagged as immediate-qa-backlog coverage")
    evidence_text = "\n".join(scenario["evidence"]["required"]).lower()
    if "status.txt" not in evidence_text:
        errors.append(f"{scenario_id} must require status.txt evidence")
    if not any(token in evidence_text for token in ("command.log", "artifact", "project", "failure")):
        errors.append(f"{scenario_id} must require command, artifact, project, or failure-path evidence")

procedure_doc_normalized = re.sub(r"\s+", " ", procedure_edit_seam_text)
if re.search(r"\balice-desktop-procedure-edit-seam-smoke\b(?:\s+\S+){0,12}\s+--timeout-seconds\b", procedure_doc_normalized):
    errors.append("procedure edit seam docs must not document the manual scenario with timeout fields")
if "alice-desktop-procedure-edit-seam-smoke --evidence-dir" in procedure_doc_normalized:
    errors.append("procedure edit seam docs must not document the manual scenario as a timeout-based runner command")
for forbidden in (
    "procedure-ui-action-no-go.json",
    "scene.<methodName>",
    "creates it when missing",
):
    if forbidden in procedure_edit_seam_text:
        errors.append(f"procedure edit seam docs must not contain stale contract wording: {forbidden}")
for required in (
    "`alice-desktop-procedure-edit-seam-smoke` QA scenario is `manual-evidence-required`",
    "has no automation block or timeout fields",
    "The only supported selector is `scene.eatmeFirstLesson`",
    "fails closed when the target method is missing",
):
    if required not in procedure_doc_normalized:
        errors.append(f"procedure edit seam docs must preserve current contract wording: {required}")

instructor_student = catalog.get("alice-desktop-instructor-student-setup")
if instructor_student is None:
    errors.append("catalog must contain alice-desktop-instructor-student-setup for learner-world boundary checks")
else:
    if instructor_student["automationMode"] != "manual-evidence-required":
        errors.append("instructor/student learner-world setup must remain manual-evidence-required")
    scenario_text = json.dumps(instructor_student, sort_keys=True).lower()
    for required in (
        "setup/open/save evidence review only",
        "learner-world grading",
        "rubric scoring",
        "correctness assessment",
        "creative assessment",
    ):
        if required not in scenario_text:
            errors.append(f"instructor/student scenario must preserve learner-world boundary wording: {required}")

learner_world_next_blocker = "define-reviewed-assessment-contract"
for name, text in (
    ("README docs", readme_text),
    ("QA reference docs", qa_reference_text),
    ("QA how-to docs", qa_howto_text),
    ("QA tutorial docs", qa_tutorial_text),
):
    lower = re.sub(r"\s+", " ", text.lower())
    if "rabbithole learner-world qa currently supports setup/open/save evidence review" not in lower:
        errors.append(f"{name} must document the learner-world setup/open/save-only boundary")
    if learner_world_next_blocker not in text:
        errors.append(f"{name} must name the learner-world assessment blocker artifact")

if not learner_world_boundary_path.is_file():
    errors.append("learner-world assessment boundary artifact must exist")
else:
    expected_boundary = {
        "id": "learner-world-assessment-boundary",
        "scope": "instructor-student learner-world setup/open/save evidence",
        "currentCapability": "collects evidence for setup, open, and save workflow review",
        "selectedScenario": "alice-desktop-instructor-student-setup",
        "automationMode": "manual-evidence-required",
    }
    for field, expected in expected_boundary.items():
        if learner_world_boundary.get(field) != expected:
            errors.append(f"learner-world boundary artifact field {field} must be {expected!r}")
    supported_evidence = set(learner_world_boundary.get("supportedEvidence", []))
    if "setup/open/save evidence review only" not in supported_evidence:
        errors.append("learner-world boundary artifact must name setup/open/save evidence review only as supportedEvidence")
    assessment_limits = learner_world_boundary.get("assessmentLimits", [])
    for required in (
        "no automated grading",
        "no rubric scoring",
        "no correctness assessment",
        "no creative assessment",
    ):
        if required not in assessment_limits:
            errors.append(f"learner-world boundary artifact assessmentLimits must include {required}")
    non_capabilities = set(learner_world_boundary.get("nonCapabilities", []))
    for required in (
        "learner-world grading",
        "rubric scoring",
        "correctness assessment",
        "creative assessment",
    ):
        if required not in non_capabilities:
            errors.append(f"learner-world boundary artifact must exclude {required}")
    if learner_world_boundary.get("nextBoundary") != learner_world_next_blocker:
        errors.append("learner-world boundary artifact must name define-reviewed-assessment-contract as nextBoundary")
    expected_summary = (
        "Learner-world grading, rubric scoring, correctness assessment, and creative "
        "assessment remain manual/unsupported until a reviewed assessment contract exists."
    )
    if learner_world_boundary.get("manualLimitationSummary") != expected_summary:
        errors.append("learner-world boundary artifact must expose the manual limitation summary")
    if learner_world_boundary.get("requiresReviewedAssessmentContractBefore") != [
        "learner-world grading",
        "rubric scoring",
        "correctness assessment",
        "creative assessment",
    ]:
        errors.append("learner-world boundary artifact must list capabilities requiring a reviewed assessment contract")
    next_blocker = learner_world_boundary.get("nextBlocker", {})
    if next_blocker.get("id") != learner_world_next_blocker:
        errors.append("learner-world boundary artifact must name define-reviewed-assessment-contract as nextBlocker.id")
    blocker = learner_world_boundary.get("blocker", {})
    if blocker.get("id") != learner_world_next_blocker:
        errors.append("learner-world boundary artifact must name define-reviewed-assessment-contract as blocker.id")
    blocker_description = blocker.get("description", "")
    if "learner-world state extraction" not in blocker_description or "blocked" not in blocker_description:
        errors.append("learner-world boundary artifact blocker must make learner-world state extraction an explicit blocker")
    description = next_blocker.get("description", "")
    if "reviewed assessment contract" not in description or "evidence mapping" not in description:
        errors.append("learner-world boundary artifact must describe the reviewed assessment contract and evidence mapping blocker")
    for required in (
        "learner-world grading",
        "rubric scoring",
        "correctness assessment",
        "creative assessment",
    ):
        if required not in description:
            errors.append(f"learner-world boundary artifact blocker description must name {required}")
    forbidden_artifact_fields = {
        "assessmentAlgorithm",
        "creativeAssessmentEngine",
        "gradingAlgorithm",
        "rubricSchema",
        "scoreSchema",
        "runnerIntegration",
    }
    present_forbidden = forbidden_artifact_fields.intersection(learner_world_boundary)
    if present_forbidden:
        errors.append(f"learner-world boundary artifact must stay declarative; remove {sorted(present_forbidden)}")

negation_markers = (
    "does not",
    "do not",
    "must not",
    "not ",
    "no ",
    "noncapabilities",
    "future ",
    "requires",
    "required before",
    "only",
    "blocker",
    "cannot currently",
    "manual/unsupported",
)
overclaim_terms = (
    "learner-work grading",
    "learner work grading",
    "automated grading",
    "rubric scoring",
    "correctness assessment",
    "creativity assessment",
    "creative assessment",
    "assess creativity",
)
texts_for_overclaim_scan = (
    ("instructor/student scenario", scenario_text if instructor_student else ""),
    ("README docs", readme_text),
    ("QA reference docs", qa_reference_text),
    ("QA how-to docs", qa_howto_text),
    ("QA tutorial docs", qa_tutorial_text),
    ("learner-world boundary artifact", learner_world_boundary_text),
)
for name, text in texts_for_overclaim_scan:
    normalized = re.sub(r"\n(?=\S)", " ", text)
    paragraphs = re.split(r"\n\s*\n", normalized)
    for paragraph in paragraphs:
        lower = paragraph.lower()
        matched_terms = [term for term in overclaim_terms if term in lower]
        if not matched_terms:
            continue
        if not any(marker in lower for marker in negation_markers):
            errors.append(
                f"{name} has possible learner-world assessment overclaim for "
                f"{', '.join(matched_terms)}: {paragraph[:160]}"
            )

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
status_file="$run_dir/status.txt"
assert_file_exists "$checklist" "workflow runner writes manual checklist"
assert_file_exists "$status_file" "workflow runner writes manual status"
assert_contains "$checklist" '^Required evidence$' "workflow checklist includes required evidence section"
assert_contains "$checklist" '^Completion status$' "workflow checklist includes completion status section"
assert_contains "$checklist" 'Human reviewer|human performs the workflow' "workflow checklist names human review requirement"
assert_contains "$checklist" '^Assessment boundary$' "workflow checklist includes assessment boundary section"
assert_contains "$checklist" '[Mm]anual evidence required' "workflow checklist requires manual evidence for assessment boundary"
assert_contains "$checklist" 'setup/open/save evidence review only' "workflow checklist limits learner-world scope to setup open save evidence"
assert_contains "$checklist" 'Learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported until a reviewed assessment contract exists\.' "workflow checklist renders manual limitation summary"
assert_contains "$checklist" 'no automated grading' "workflow checklist rejects automated grading"
assert_contains "$checklist" 'no rubric scoring' "workflow checklist rejects rubric scoring"
assert_contains "$checklist" 'no correctness assessment' "workflow checklist rejects correctness assessment"
assert_contains "$checklist" 'no creative assessment' "workflow checklist rejects creative assessment"
assert_not_contains "$checklist" 'correctness scoring' "workflow checklist does not use scoring wording for correctness"
assert_contains "$checklist" 'Next boundary: define-reviewed-assessment-contract' "workflow checklist names next assessment boundary"
assert_contains "$checklist" 'Manual/unsupported until reviewed contract: learner-world grading' "workflow checklist keeps learner-world grading manual unsupported"
assert_contains "$checklist" 'Manual/unsupported until reviewed contract: rubric scoring' "workflow checklist keeps rubric scoring manual unsupported"
assert_contains "$checklist" 'Manual/unsupported until reviewed contract: correctness assessment' "workflow checklist keeps correctness assessment manual unsupported"
assert_contains "$checklist" 'Manual/unsupported until reviewed contract: creative assessment' "workflow checklist keeps creative assessment manual unsupported"
assert_contains "$checklist" '[Ll]earner-world state extraction.*blocked|blocked.*learner-world state extraction' "workflow checklist exposes learner-world extraction blocker"
assert_contains "$checklist" 'define-reviewed-assessment-contract' "workflow checklist names assessment blocker artifact"
assert_contains "$status_file" '^assessmentBoundary=define-reviewed-assessment-contract$' "workflow status names assessment boundary"
assert_contains "$status_file" '^assessmentBoundaryMode=manual/unsupported$' "workflow status keeps assessment boundary manual unsupported"
assert_contains "$status_file" '^assessmentBoundaryScope=instructor-student learner-world setup/open/save evidence$' "workflow status records assessment scope"
assert_contains "$status_file" 'Learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported until a reviewed assessment contract exists\.' "workflow status renders manual limitation summary"
assert_contains "$status_file" 'assessmentLimits=.*no automated grading.*no rubric scoring.*no correctness assessment.*no creative assessment' "workflow status rejects unsupported assessment limits"
assert_contains "$status_file" 'assessmentUnsupportedUntilReviewedContract=.*learner-world grading.*rubric scoring.*correctness assessment.*creative assessment' "workflow status records capabilities requiring reviewed contract"
assert_contains "$status_file" '^assessmentBlocker=define-reviewed-assessment-contract$' "workflow status names assessment blocker"
assert_contains "$status_file" '[Ll]earner-world state extraction.*blocked|blocked.*learner-world state extraction' "workflow status exposes learner-world extraction blocker"
assert_not_contains "$status_file" 'correctness scoring' "workflow status does not use scoring wording for correctness"

finish
