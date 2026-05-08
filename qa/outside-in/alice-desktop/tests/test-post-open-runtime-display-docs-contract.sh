#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-docs-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

REFERENCE_DOC="$REPO_ROOT/docs/reference/post-open-runtime-display-accessibility-evidence.md"
HOWTO_DOC="$REPO_ROOT/docs/howto/alice-desktop-outside-in-qa.md"
TUTORIAL_DOC="$REPO_ROOT/docs/tutorials/alice-desktop-outside-in-qa.md"
QA_REFERENCE_DOC="$REPO_ROOT/docs/reference/alice-desktop-outside-in-qa.md"
README_DOC="$BASE_DIR/README.md"
SCENARIO_FILE="$BASE_DIR/scenarios/post-open-runtime-display-accessibility-evidence.yaml"

for doc in "$REFERENCE_DOC" "$HOWTO_DOC" "$TUTORIAL_DOC" "$QA_REFERENCE_DOC" "$README_DOC" "$SCENARIO_FILE"; do
  assert_file_exists "$doc" "post-open runtime/display docs contract input exists: ${doc#$REPO_ROOT/}"
done

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

python3 - \
  "$REFERENCE_DOC" \
  "$HOWTO_DOC" \
  "$TUTORIAL_DOC" \
  "$QA_REFERENCE_DOC" \
  "$README_DOC" \
  "$SCENARIO_FILE" \
  > "$tmp_root/docs-contract.out" \
  2> "$tmp_root/docs-contract.err" <<'PY'
import re
import sys
from pathlib import Path

paths = {
    "reference": Path(sys.argv[1]),
    "howto": Path(sys.argv[2]),
    "tutorial": Path(sys.argv[3]),
    "qa_reference": Path(sys.argv[4]),
    "readme": Path(sys.argv[5]),
    "scenario": Path(sys.argv[6]),
}
texts = {name: path.read_text(encoding="utf-8") for name, path in paths.items()}
errors = []

claim_token = "post-open-runtime-display-accessibility-evidence"
scenario_id = "alice-desktop-post-open-runtime-display-accessibility-evidence"
artifact_name = "post-open-runtime-display-accessibility-evidence.json"
supporting_artifacts = [
    "tab-click-observation.json",
    "post-project-open-observation.json",
    "controlled-display-pixel-observation.json",
]
decision_fields = [
    "automationMode",
    "blocker",
    "blockerDetail",
    "claim",
    "javaPid",
    "postOpenRuntimeDisplayAccessibilityObserved",
    "postOpenWindowObserved",
    "runtimeDisplayCandidateCount",
    "runtimeDisplayCandidates",
    "scenario",
    "status",
    "traversalErrors",
]
candidate_fields = ["childCount", "name", "path", "role", "states"]

for name, text in texts.items():
    if name == "scenario":
        continue
    for required in (claim_token, scenario_id, artifact_name):
        if required not in text:
            errors.append(f"{name} doc must mention {required}")

for name in ("reference", "howto", "tutorial", "qa_reference", "readme"):
    text = texts[name]
    for artifact in supporting_artifacts:
        if artifact not in text:
            errors.append(f"{name} doc must name supporting setup artifact {artifact}")
    for required_status in (
        "runtime-display-accessibility-status.txt",
        "controlledDisplayPixelStatus",
        "outcome=passed",
    ):
        if required_status not in text:
            errors.append(f"{name} doc must document final/probe status contract: {required_status}")

reference_text = texts["reference"]
for field in decision_fields:
    if f"`{field}`" not in reference_text and f'"{field}"' not in reference_text:
        errors.append(f"reference doc must document artifact field {field}")
for field in candidate_fields:
    if field not in reference_text:
        errors.append(f"reference doc must document runtimeDisplayCandidates.{field}")

howto_text = texts["howto"]
for field in decision_fields:
    if f'"{field}"' not in howto_text and f"`{field}`" not in howto_text:
        errors.append(f"howto doc must show emitted artifact field {field}")
for field in candidate_fields:
    if field not in howto_text:
        errors.append(f"howto doc must show runtimeDisplayCandidates.{field}")

scenario_text = texts["scenario"]
for required in [
    "status=blocked with a precise blocker",
    "controlledDisplayPixelStatus=observed",
    "runtime-display-accessibility-status.txt",
    "Do not claim postOpenRuntimeDisplayAccessibilityObserved=true",
    "does not assert full visible rendering correctness",
]:
    if required not in scenario_text:
        errors.append(f"scenario fallback must preserve blocked/limited wording: {required}")
for artifact in supporting_artifacts:
    if artifact not in scenario_text:
        errors.append(f"scenario evidence must require supporting artifact {artifact}")

negation_markers = [
    "does not",
    "do not",
    "must not",
    "not prove",
    "not assert",
    "not a",
    "without",
    "instead of",
    "gap report",
    "blocked",
]
forbidden_terms = [
    "full visible rendering correctness",
    "visible rendering correctness",
    "visible ui correctness",
    "deployed installer success",
    "deployed installer",
    "installer success",
    "installer deployment",
    "full world execution",
    "world execution",
    "world runs",
    "grading",
    "lesson completion",
    "lesson completes",
    "active save",
    "save behavior",
    "save succeeds",
    "active select project",
    "select project behavior",
    "select project succeeds",
    "decoder behavior",
    "decoder fallback",
]

for name, text in texts.items():
    normalized = re.sub(r"\n(?=\S)", " ", text)
    paragraphs = re.split(r"\n\s*\n", normalized)
    for paragraph in paragraphs:
        lower = paragraph.lower()
        matched_terms = [term for term in forbidden_terms if term in lower]
        if not matched_terms:
            continue
        if not any(marker in lower for marker in negation_markers):
            errors.append(
                f"{name} has possible expanded evidence claim for "
                f"{', '.join(matched_terms)}: {paragraph[:160]}"
            )

if "Generated evidence is local run output. Keep it uncommitted." not in reference_text:
    errors.append("reference doc must tell reviewers to keep generated evidence uncommitted")
if "Generated evidence is ignored by Git." not in texts["readme"]:
    errors.append("README must state generated evidence is ignored by Git")
if "status=observed" not in texts["qa_reference"] or "status=blocked" not in texts["qa_reference"]:
    errors.append("QA reference must describe both observed and blocked runtime/display outcomes")

if errors:
    raise AssertionError("\n".join(errors))

print("post-open runtime/display docs contract satisfied")
PY
status=$?
assert_success "$status" "post-open runtime/display docs preserve narrow evidence contract"

finish
