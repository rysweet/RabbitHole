#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

SCENARIO_ID=alice-desktop-run-debug

"$VALIDATOR" --dump-json "$SCENARIO_ID" >"$tmp_root/run-debug.json" 2>"$tmp_root/run-debug.err"
status=$?
assert_success "$status" "validator dumps the Run execution gap scenario"

python3 - "$tmp_root/run-debug.json" >"$tmp_root/run-gap-unit.out" 2>"$tmp_root/run-gap-unit.err" <<'PY'
import json
import re
import sys

scenario = json.load(open(sys.argv[1], encoding="utf-8"))
errors = []

if scenario.get("workflow") != "run-debug":
    errors.append("run-debug scenario must keep the run-debug workflow value")
if scenario.get("automationMode") != "manual-evidence-required":
    errors.append("run-debug scenario must remain manual-evidence-required until full UI automation exists")

required_evidence = scenario.get("evidence", {}).get("required", [])
scenario_text = json.dumps(scenario, sort_keys=True)
evidence_text = "\n".join(required_evidence)
evidence_text_lower = evidence_text.lower()

supporting_vm_artifacts = ("desktop-run-execution.json", "desktop-run-runtime.log")
if not any(all(artifact in item for artifact in supporting_vm_artifacts) for item in required_evidence):
    errors.append("run-debug required evidence must collect both VM-listener support artifacts together")
if "opt-in desktop run execution evidence" not in evidence_text_lower:
    errors.append("run-debug VM-listener evidence must stay conditional on opt-in desktop Run execution evidence")

payload_artifacts = [
    "desktop-run-render-affordance.json",
    "desktop-run-pixel-boundary.json",
    "desktop-run-pixel-observation.json",
    "desktop-first-lesson-next-action.json",
    "desktop-save-menu-action-target.json",
    "desktop-run-status-summary.json",
]
for artifact in payload_artifacts:
    if artifact not in evidence_text:
        errors.append(f"run-debug required evidence must name report-referenced artifact {artifact}")

gap_report_lines = [
    item for item in required_evidence
    if "desktop-run-execution-gap-report.json" in item
]
if not gap_report_lines:
    errors.append("run-debug must require desktop-run-execution-gap-report.json")
else:
    gap_report_text = "\n".join(gap_report_lines)
    for artifact in supporting_vm_artifacts:
        if artifact in gap_report_text:
            errors.append(f"{artifact} must not be described as a gap-report executableToday payload artifact")

if not re.search(
    r"review-notes\.txt.*(?:exact (?:location|path)|precisely link|exact evidence directory)",
    scenario_text,
    re.IGNORECASE | re.DOTALL,
):
    errors.append("run-debug review-notes.txt must require exact paths or precise links for report-referenced artifacts")

claim_limits = [
    "full world execution",
    "playback",
    "visible rendering correctness",
    "full UI automation",
    "Save completion",
    "grading",
    "Sims validation",
    "deployed installer success",
]
for claim in claim_limits:
    pattern = rf"(?:does not claim|do not claim|must not claim|not proof of)[^.\\n]*{re.escape(claim)}"
    if not re.search(pattern, scenario_text, re.IGNORECASE):
        errors.append(f"run-debug scenario must explicitly avoid claiming {claim!r}")

if errors:
    raise AssertionError("\n".join(errors))
PY
assert_success "$?" "Run execution gap scenario keeps bounded artifact and non-claim contract"

"$RUNNER" run "$SCENARIO_ID" --prepare-only --evidence-dir "$tmp_root/evidence" >"$tmp_root/runner.out" 2>"$tmp_root/runner.err"
status=$?
assert_success "$status" "runner prepares the Run execution gap manual checklist"
checklist=$(find "$tmp_root/evidence" -name manual-evidence-checklist.txt -type f | sort | head -n 1)
if [ -z "$checklist" ]; then
    checklist="$tmp_root/evidence/manual-evidence-checklist.txt"
fi
assert_file_exists "$checklist" "Run execution gap manual checklist exists"

python3 - "$checklist" >"$tmp_root/run-gap-checklist.out" 2>"$tmp_root/run-gap-checklist.err" <<'PY'
import re
import sys
from pathlib import Path

checklist = Path(sys.argv[1]).read_text(encoding="utf-8")
errors = []

required_sections = [
    "Scenario: alice-desktop-run-debug",
    "Required evidence",
    "Fallback notes",
    "Completion status",
]
for section in required_sections:
    if section not in checklist:
        errors.append(f"manual checklist must include {section!r}")

for artifact in (
    "desktop-run-execution.json",
    "desktop-run-runtime.log",
    "desktop-run-render-affordance.json",
    "desktop-run-pixel-boundary.json",
    "desktop-run-pixel-observation.json",
    "desktop-first-lesson-next-action.json",
    "desktop-save-menu-action-target.json",
    "desktop-run-status-summary.json",
    "desktop-run-execution-gap-report.json",
):
    if artifact not in checklist:
        errors.append(f"manual checklist must name {artifact}")

if not re.search(
    r"review-notes\.txt.*(?:exact (?:location|path)|precisely link|exact evidence directory)",
    checklist,
    re.IGNORECASE | re.DOTALL,
):
    errors.append("manual checklist must require exact paths or precise links for report-referenced artifacts")

for forbidden_claim in (
    "full world execution",
    "playback",
    "visible rendering correctness",
    "full UI automation",
    "Save completion",
    "grading",
    "Sims validation",
    "deployed installer success",
):
    pattern = rf"(?:does not claim|do not claim|must not claim|not proof of)[^.\\n]*{re.escape(forbidden_claim)}"
    if not re.search(pattern, checklist, re.IGNORECASE):
        errors.append(f"manual checklist must explicitly avoid claiming {forbidden_claim!r}")

if errors:
    raise AssertionError("\n".join(errors))
PY
assert_success "$?" "Run execution gap checklist renders bounded review requirements"

missing_vm_dir="$tmp_root/missing-vm-artifacts"
mkdir -p "$missing_vm_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$missing_vm_dir"/
python3 - "$missing_vm_dir/run-debug.yaml" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
lines = path.read_text(encoding="utf-8").splitlines()
filtered = [
    line for line in lines
    if "desktop-run-execution.json and desktop-run-runtime.log" not in line
]
path.write_text("\n".join(filtered) + "\n", encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$missing_vm_dir" "$VALIDATOR" >"$tmp_root/missing-vm.out" 2>"$tmp_root/missing-vm.err"
status=$?
assert_failure "$status" "validator rejects run-debug without VM-listener supporting artifacts"
assert_contains "$tmp_root/missing-vm.err" 'desktop-run-execution\.json|desktop-run-runtime\.log|VM-listener' "missing VM-listener evidence error is actionable"

payload_drift_dir="$tmp_root/payload-drift"
mkdir -p "$payload_drift_dir"
cp "$BASE_DIR"/scenarios/*.yaml "$payload_drift_dir"/
python3 - "$payload_drift_dir/run-debug.yaml" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
text = text.replace(
    "desktop-run-execution-gap-report.json from the same opt-in desktop Run path, naming the bounded executable Run-window evidence and the deterministic world-advance proof blocker.",
    "desktop-run-execution-gap-report.json from the same opt-in desktop Run path, naming desktop-run-execution.json as executableToday evidence and the deterministic world-advance proof blocker.",
)
path.write_text(text, encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$payload_drift_dir" "$VALIDATOR" >"$tmp_root/payload-drift.out" 2>"$tmp_root/payload-drift.err"
status=$?
assert_failure "$status" "validator rejects VM-listener artifacts promoted into executableToday wording"
assert_contains "$tmp_root/payload-drift.err" 'desktop-run-execution\.json|executableToday|gap-report' "payload drift error is actionable"

finish
