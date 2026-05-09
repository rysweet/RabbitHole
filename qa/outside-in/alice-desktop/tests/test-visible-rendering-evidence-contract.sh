#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCENARIO_ID=alice-desktop-post-open-runtime-display-accessibility-evidence
POST_OPEN_RUNTIME_DISPLAY_ARTIFACT=post-open-runtime-display-accessibility-evidence.json
CONTROLLED_ARTIFACT=controlled-display-pixel-observation.json
BLOCKER_ARTIFACT=visible-rendering-pixel-target-blocker.json
PIXEL_SAMPLING_BLOCKER_ARTIFACT=visible-rendering-pixel-sampling-blocker.json
FIXTURE_DIR="$SCRIPT_DIR/fixtures/visible-rendering"
VALID_NONCLAIM_FIXTURE="$FIXTURE_DIR/valid-nonclaim-render-evidence.json"
INVALID_OVERCLAIM_FIXTURE="$FIXTURE_DIR/invalid-overclaim-render-evidence.json"
VALID_NEGATED_WORDING_FIXTURE="$FIXTURE_DIR/valid-negated-nonclaim-render-wording.txt"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_render_nonclaim_jsons() {
  if [ "$#" -eq 0 ] || [ $(( $# % 2 )) -ne 0 ]; then
    printf 'assert_render_nonclaim_jsons requires artifact/label pairs\n' >&2
    return 64
  fi

  python3 - "$@" <<'PY'
import json
import re
import sys

positive_correctness_patterns = (
    re.compile(r"\b(proves?|proved|establish(?:es|ed)?|confirms?|validates?|verifies?)\b.{0,80}?\b(visible|visual|rendered|rendering)\b.{0,60}?\b(correct|correctness|correctly|valid|passed)\b"),
    re.compile(r"\b(visible|visual|rendered|rendering)\b.{0,60}?\b(correct|correctness|correctly)\b.{0,60}?\b(proven|proved|established|confirmed|validated|verified|passed)\b"),
    re.compile(r"\bvisibly correct\b"),
)
negation_markers = (
    "cannot ",
    "can not ",
    "does not ",
    "do not ",
    "did not ",
    "must not ",
    "not ",
    "no ",
    "never ",
    "without ",
    "unsupported",
    "nonclaim",
    "not-asserted",
)
render_evidence_keys = {
    "generatedFiles",
    "pixelObservation",
    "pixelSampling",
    "renderArtifacts",
    "renderedWorldPixelsObserved",
    "sampleCount",
    "sampledPixels",
    "samples",
    "screenshot",
    "screenshotFile",
    "screenshotPath",
    "sourceArtifact",
    "worldCanvasPixelTarget",
}


def collect_contract_inputs(value, state):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in render_evidence_keys:
                state["has_render_evidence"] = True
            if key == "correctnessCheck":
                state["correctness_checks"].append(child)
            if key == "unsupportedClaims":
                continue
            if key_implies_visible_correctness(key) and positive_correctness_value(child):
                state["positive_correctness_fields"].append((key, child))
            collect_contract_inputs(child, state)
    elif isinstance(value, list):
        for child in value:
            collect_contract_inputs(child, state)
    elif isinstance(value, str):
        state["texts"].append(value)


def sentences(text):
    return [part.strip().lower() for part in re.split(r"(?<=[.!?])\s+|\n+", text) if part.strip()]


def is_negated(sentence):
    return any(marker in sentence for marker in negation_markers)


def key_implies_visible_correctness(key):
    lower = key.lower()
    return "correct" in lower and any(
        token in lower
        for token in ("visible", "visual", "rendered", "rendering", "world")
    )


def positive_correctness_value(value):
    if value is True:
        return True
    if isinstance(value, str):
        lower = value.strip().lower()
        if any(marker in lower for marker in negation_markers):
            return False
        return lower in {
            "accepted",
            "confirmed",
            "correct",
            "established",
            "observed",
            "passed",
            "performed",
            "success",
            "validated",
            "verified",
        }
    if isinstance(value, dict):
        return any(positive_correctness_value(child) for child in value.values())
    if isinstance(value, list):
        return any(positive_correctness_value(child) for child in value)
    return False


def correctness_claim_is_negated(sentence, match):
    clause_start = 0
    for delimiter in (";", ",", ".", "!", "?", "\n"):
        index = sentence.rfind(delimiter, 0, match.start())
        if index >= clause_start:
            clause_start = index + 1
    claim_clause = sentence[clause_start:match.end()]
    return is_negated(claim_clause)


def explicit_visible_correctness_observation(value):
    evidence = value.get("visibleCorrectnessObservationEvidence") if isinstance(value, dict) else None
    return isinstance(evidence, dict) and evidence.get("status") == "observed"


def unsupported_claims_document_boundary(value):
    claims = value.get("unsupportedClaims") if isinstance(value, dict) else None
    return isinstance(claims, list) and "full-visible-rendering-correctness" in claims


def text_documents_boundary(texts):
    return any(
        ("visible" in sentence or "visual" in sentence or "render" in sentence)
        and ("correct" in sentence or "correctness" in sentence)
        and is_negated(sentence)
        for text in texts
        for sentence in sentences(text)
    )


def check_payload(path, label, errors):
    with open(path, encoding="utf-8") as artifact:
        payload = json.load(artifact)
    state = {
        "has_render_evidence": False,
        "correctness_checks": [],
        "positive_correctness_fields": [],
        "texts": [],
    }
    collect_contract_inputs(payload, state)

    if state["has_render_evidence"] and not explicit_visible_correctness_observation(payload):
        if payload.get("visibleRenderingCorrectnessEstablished") is not False:
            errors.append(f"{label} must keep visibleRenderingCorrectnessEstablished=false without observation evidence")
        for value in state["correctness_checks"]:
            if value != "not-performed":
                errors.append(f"{label} correctnessCheck must be not-performed without observation evidence")
        for key, value in state["positive_correctness_fields"]:
            errors.append(f"{label} must not report {key}={value!r} without observation evidence")
        if not unsupported_claims_document_boundary(payload) and not text_documents_boundary(state["texts"]):
            errors.append(f"{label} must document the visible-correctness nonclaim boundary")

    for text in state["texts"]:
        for sentence in sentences(text):
            claim_error = False
            for pattern in positive_correctness_patterns:
                for match in pattern.finditer(sentence):
                    if not correctness_claim_is_negated(sentence, match):
                        errors.append(f"{label} must not make positive visible-correctness claims from render evidence: {sentence}")
                        claim_error = True
                        break
                if claim_error:
                    break


errors = []
args = sys.argv[1:]
for index in range(0, len(args), 2):
    check_payload(args[index], args[index + 1], errors)

if errors:
    raise AssertionError("\n".join(errors))
PY
}

assert_render_nonclaim_json() {
  assert_render_nonclaim_jsons "$1" "$2"
}

python3 - \
  "$FIXTURE_DIR/world-canvas-pixel-target-ready.json" \
  "$FIXTURE_DIR/world-canvas-pixel-target-blocked.json" \
  >"$tmp_root/fixture-contract.out" \
  2>"$tmp_root/fixture-contract.err" <<'PY'
import json
import math
import sys

ready_path, blocked_path = sys.argv[1:3]
ready = json.load(open(ready_path, encoding="utf-8"))
blocked = json.load(open(blocked_path, encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def numeric(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def assert_no_overclaiming(payload, label):
    forbidden_phrases = (
        "world canvas pixel correctness passed",
        "world-canvas pixel correctness passed",
        "full visible rendering correctness passed",
        "visible rendering correctness passed",
        "full ui automation succeeded",
        "full-ui-automation succeeded",
        "grading passed",
        "save behavior passed",
        "first lesson completed",
        "first-lesson completion passed",
    )

    def walk(value, key_path=()):
        if key_path and key_path[-1] == "unsupportedClaims":
            return
        if isinstance(value, dict):
            for key, child in value.items():
                walk(child, key_path + (str(key),))
        elif isinstance(value, list):
            for child in value:
                walk(child, key_path)
        elif isinstance(value, str):
            text = value.lower()
            for phrase in forbidden_phrases:
                require(phrase not in text, f"{label} must not overclaim with phrase: {phrase}")

    walk(payload)


def require_unsupported_claims(payload, label):
    unsupported = payload.get("unsupportedClaims")
    require(isinstance(unsupported, list), f"{label} must list unsupportedClaims")
    if isinstance(unsupported, list):
        for claim in (
            "world-canvas-pixel-correctness",
            "full-visible-rendering-correctness",
            "full-ui-automation",
            "grading",
            "save-behavior",
            "first-lesson-completion",
        ):
            require(claim in unsupported, f"{label} unsupportedClaims must include {claim}")


require(ready.get("schemaVersion") == 1, "target-ready fixture must use schemaVersion=1")
require(ready.get("claimScope") == "controlled-display-screenshot-consistency", "target-ready fixture stays scoped to screenshot consistency")
require(ready.get("status") == "observed", "target-ready fixture must be observed screenshot evidence")
require_unsupported_claims(ready, "target-ready fixture")
target = ready.get("worldCanvasPixelTarget")
require(isinstance(target, dict), "target-ready fixture must include worldCanvasPixelTarget")
if isinstance(target, dict):
    require(target.get("identified") is True, "target-ready fixture must identify the pixel target")
    require(target.get("status") == "target-ready", "target-ready fixture must use status=target-ready")
    require(target.get("geometryStatus") == "available", "target-ready fixture must require available geometry")
    require(target.get("sourceArtifact") == "post-open-runtime-display-accessibility-evidence.json", "target-ready fixture must cite runtime/display source")
    require(target.get("selectionRule") == "single-visible-showing-runtime-display-candidate-with-valid-screen-extents", "target-ready fixture must record deterministic selection rule")
    states = target.get("candidateStates")
    require(isinstance(states, list), "target-ready fixture must include candidateStates")
    if isinstance(states, list):
        require("visible" in states and "showing" in states, "target-ready fixture requires visible and showing candidate states")
    extents = target.get("screenExtents")
    require(isinstance(extents, dict), "target-ready fixture must include screenExtents")
    if isinstance(extents, dict):
        require(extents.get("coordinateType") == "screen", "target-ready screenExtents must use screen coordinates")
        for key in ("x", "y", "width", "height"):
            require(numeric(extents.get(key)), f"target-ready screenExtents.{key} must be numeric")
        if numeric(extents.get("width")):
            require(extents["width"] > 0, "target-ready screenExtents.width must be positive")
        if numeric(extents.get("height")):
            require(extents["height"] > 0, "target-ready screenExtents.height must be positive")

require(blocked.get("schemaVersion") == 1, "blocked fixture must use schemaVersion=1")
require(blocked.get("status") == "blocked", "blocked fixture must be blocked")
require(blocked.get("claimScope") == "visible-rendering-world-canvas-pixel-target", "blocked fixture must use target-readiness claim scope")
require(blocked.get("claimScopeDetail") == "target-readiness-only", "blocked fixture must be target-readiness-only")
require(blocked.get("missingTarget") == "run-window-world-canvas-screen-extents", "blocked fixture must name the exact missing target")
require(blocked.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target", "blocked fixture must name the exact next unblocker")
require(blocked.get("geometryStatus") in {"missing-component-interface", "missing-extents", "invalid-extents", "ambiguous-candidates"}, "blocked fixture must use a strict geometryStatus enum")
require(isinstance(blocked.get("runtimeDisplayCandidateCount"), int), "blocked fixture must preserve runtimeDisplayCandidateCount")
require_unsupported_claims(blocked, "blocked fixture")
blocked_target = blocked.get("worldCanvasPixelTarget")
require(isinstance(blocked_target, dict), "blocked fixture must include worldCanvasPixelTarget")
if isinstance(blocked_target, dict):
    require(blocked_target.get("identified") is False, "blocked target must not be identified")
    require(blocked_target.get("status") == "blocked", "blocked target must use status=blocked")
    require(blocked_target.get("missingTarget") == "run-window-world-canvas-screen-extents", "blocked target must name the exact missing target")
    require(blocked_target.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target", "blocked target must name the exact next unblocker")
    require(blocked_target.get("geometryStatus") == blocked.get("geometryStatus"), "blocked target geometryStatus must match top-level geometryStatus")
    require(blocked_target.get("runtimeDisplayCandidateCount") == blocked.get("runtimeDisplayCandidateCount"), "blocked target must preserve runtimeDisplayCandidateCount")
    require(blocked_target.get("sourceArtifact") == "post-open-runtime-display-accessibility-evidence.json", "blocked target must cite runtime/display source")

assert_no_overclaiming(ready, "target-ready fixture")
assert_no_overclaiming(blocked, "blocked fixture")

if errors:
    raise AssertionError("\n".join(errors))
PY
fixture_status=$?
assert_success "$fixture_status" "fixture contract covers target-ready and exact blocker world-canvas pixel-target shapes"

assert_render_nonclaim_json "$VALID_NONCLAIM_FIXTURE" "valid nonclaim fixture" \
  >"$tmp_root/valid-nonclaim-fixture.out" \
  2>"$tmp_root/valid-nonclaim-fixture.err"
status=$?
assert_success "$status" "valid nonclaim fixture preserves render evidence as a visible-correctness nonclaim"

assert_render_nonclaim_json "$INVALID_OVERCLAIM_FIXTURE" "invalid overclaim fixture" \
  >"$tmp_root/invalid-overclaim-fixture.out" \
  2>"$tmp_root/invalid-overclaim-fixture.err"
status=$?
assert_failure "$status" "invalid overclaim fixture is rejected by the nonclaim contract"
assert_contains "$INVALID_OVERCLAIM_FIXTURE" '"expectedContractResult": "rejected"' "invalid overclaim fixture is explicitly marked as a negative fixture"
assert_contains "$tmp_root/invalid-overclaim-fixture.err" 'visibleRenderingCorrectnessEstablished=false|correctnessCheck must be not-performed|positive visible-correctness claims' "invalid overclaim fixture fails for visible-correctness overclaim semantics"

unobserved_correctness_status_payload="$tmp_root/invalid-unobserved-correctness-status-fixture.json"
python3 - "$unobserved_correctness_status_payload" <<'PY'
import json
import sys

payload = {
    "schemaVersion": 1,
    "status": "observed",
    "visibleRenderingCorrectnessEstablished": False,
    "visibleCorrectnessStatus": "observed",
    "generatedFiles": ["screenshot.png", "visible-rendering-pixel-observation.json"],
    "pixelSampling": {
        "correctnessCheck": "not-performed",
        "pixelsSampled": True,
        "sampleCount": 1,
    },
    "limitations": ["Render evidence does not establish visible correctness."],
    "unsupportedClaims": ["full-visible-rendering-correctness"],
}
with open(sys.argv[1], "w", encoding="utf-8") as output:
    json.dump(payload, output, indent=2, sort_keys=True)
    output.write("\n")
PY
status=$?
assert_success "$status" "unobserved visible-correctness status fixture is wrapped as render-evidence contract input"
assert_render_nonclaim_json "$unobserved_correctness_status_payload" "invalid unobserved correctness status fixture" \
  >"$tmp_root/invalid-unobserved-correctness-status-fixture.out" \
  2>"$tmp_root/invalid-unobserved-correctness-status-fixture.err"
status=$?
assert_failure "$status" "success-shaped visible-correctness status is rejected without observation evidence"
assert_contains "$tmp_root/invalid-unobserved-correctness-status-fixture.err" 'visibleCorrectnessStatus' "unobserved correctness status fixture fails for the success-shaped field"

wording_payload="$tmp_root/valid-negated-wording-fixture.json"
python3 - "$VALID_NEGATED_WORDING_FIXTURE" "$wording_payload" <<'PY'
import json
import sys

wording = open(sys.argv[1], encoding="utf-8").read()
payload = {
    "schemaVersion": 1,
    "status": "observed",
    "visibleRenderingCorrectnessEstablished": False,
    "renderArtifacts": [{"kind": "wording-fixture", "supports": "nonclaim-boundary-wording-only"}],
    "limitations": [wording],
    "unsupportedClaims": ["full-visible-rendering-correctness"],
}
with open(sys.argv[2], "w", encoding="utf-8") as output:
    json.dump(payload, output, indent=2, sort_keys=True)
    output.write("\n")
PY
status=$?
assert_success "$status" "negated wording fixture is wrapped as render-evidence contract input"
assert_render_nonclaim_json "$wording_payload" "valid negated wording fixture" \
  >"$tmp_root/valid-negated-wording-fixture.out" \
  2>"$tmp_root/valid-negated-wording-fixture.err"
status=$?
assert_success "$status" "negated wording fixture is accepted by the nonclaim contract"
assert_contains "$VALID_NEGATED_WORDING_FIXTURE" 'does not establish visible correctness' "negated wording fixture includes allowed nonclaim boundary wording"
assert_contains "$VALID_NEGATED_WORDING_FIXTURE" 'do not prove the rendered result was visibly correct' "negated wording fixture covers render-output overclaim wording in negated form"

misleading_negation_payload="$tmp_root/invalid-misleading-negation-fixture.json"
python3 - "$misleading_negation_payload" <<'PY'
import json
import sys

payload = {
    "schemaVersion": 1,
    "status": "observed",
    "visibleRenderingCorrectnessEstablished": False,
    "generatedFiles": ["screenshot.png", "visible-rendering-pixel-observation.json"],
    "renderArtifacts": [{"kind": "screenshot", "supports": "artifact-production-only"}],
    "pixelSampling": {
        "correctnessCheck": "not-performed",
        "pixelsSampled": True,
        "sampleCount": 1,
    },
    "limitations": [
        "Render evidence does not establish visible correctness, but render artifacts prove visual correctness."
    ],
    "unsupportedClaims": ["full-visible-rendering-correctness"],
}
with open(sys.argv[1], "w", encoding="utf-8") as output:
    json.dump(payload, output, indent=2, sort_keys=True)
    output.write("\n")
PY
status=$?
assert_success "$status" "misleading-negation fixture is wrapped as render-evidence contract input"
assert_render_nonclaim_json "$misleading_negation_payload" "invalid misleading-negation fixture" \
  >"$tmp_root/invalid-misleading-negation-fixture.out" \
  2>"$tmp_root/invalid-misleading-negation-fixture.err"
status=$?
assert_failure "$status" "misleading negation does not hide a later positive visible-correctness claim"
assert_contains "$tmp_root/invalid-misleading-negation-fixture.err" 'positive visible-correctness claims' "misleading-negation fixture fails for the unnegated overclaim clause"

evidence_dir="$tmp_root/no-xvfb-evidence"
ALICE_QA_DISABLE_XVFB=1 "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$evidence_dir" >"$tmp_root/no-xvfb.out" 2>"$tmp_root/no-xvfb.err"
status=$?
assert_exit_code "$status" 2 "missing X server keeps visible-rendering evidence blocked"

run_dir=$(single_child_dir "$evidence_dir/$SCENARIO_ID")
run_dir_status=$?
assert_success "$run_dir_status" "visible-rendering fallback creates one evidence directory"
if [ "$run_dir_status" -eq 0 ]; then
  controlled="$run_dir/$CONTROLLED_ARTIFACT"
  blocker="$run_dir/$BLOCKER_ARTIFACT"
  sampling_blocker="$run_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT"
  status_file="$run_dir/status.txt"
  assert_file_exists "$controlled" "runner writes controlled-display screenshot-consistency artifact"
  assert_file_exists "$blocker" "runner writes precise visible-rendering pixel-target blocker artifact"
  assert_file_exists "$sampling_blocker" "runner writes precise visible-rendering pixel-sampling blocker artifact"
  assert_file_exists "$status_file" "runner writes visible-rendering status linkage"
  assert_contains "$status_file" "^visibleRenderingPixelTargetBlocker=$BLOCKER_ARTIFACT$" "status links the visible-rendering blocker artifact"
  assert_contains "$status_file" "^visibleRenderingPixelSamplingStatus=blocked$" "status keeps pixel sampling fail-closed"
  assert_contains "$status_file" "^visibleRenderingPixelSamplingArtifact=$PIXEL_SAMPLING_BLOCKER_ARTIFACT$" "status links the visible-rendering pixel-sampling blocker artifact"

  python3 - \
    "$controlled" \
    "$blocker" \
    "$sampling_blocker" \
    "$CONTROLLED_ARTIFACT" \
    "$BLOCKER_ARTIFACT" \
    >"$tmp_root/artifact-contract.out" \
    2>"$tmp_root/artifact-contract.err" <<'PY'
import json
import os
import sys

controlled_path, blocker_path, sampling_blocker_path, controlled_name, blocker_name = sys.argv[1:6]
controlled = json.load(open(controlled_path, encoding="utf-8"))
blocker = json.load(open(blocker_path, encoding="utf-8"))
sampling_blocker = json.load(open(sampling_blocker_path, encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(controlled.get("schemaVersion") == 1, "controlled artifact must have schemaVersion=1")
require(
    controlled.get("claimScope") == "controlled-display-screenshot-consistency",
    "controlled artifact must use the narrow screenshot-consistency claim scope",
)
require(controlled.get("claim") == "no-visible-pixel-proof", "blocked controlled artifact must not claim visible pixels")
require(
    controlled.get("visibleRenderingCorrectnessEstablished") is False,
    "blocked controlled render evidence must explicitly keep visibleRenderingCorrectnessEstablished=false",
)

screenshot = controlled.get("screenshot")
require(isinstance(screenshot, dict), "controlled artifact must include screenshot metadata")
if isinstance(screenshot, dict):
    require(screenshot.get("status") == "not-attempted", "missing X server keeps screenshot status not-attempted")
    require(screenshot.get("path") is None, "blocked screenshot path must be null, not absolute or invented")
    dimensions = screenshot.get("dimensions")
    require(isinstance(dimensions, dict), "screenshot dimensions object must always be present")
    if isinstance(dimensions, dict):
        require(dimensions.get("width") is None, "blocked screenshot width must be null")
        require(dimensions.get("height") is None, "blocked screenshot height must be null")

pixel_observation = controlled.get("pixelObservation")
require(isinstance(pixel_observation, dict), "controlled artifact must include pixelObservation")
if isinstance(pixel_observation, dict):
    require(pixel_observation.get("status") == "not-attempted", "blocked pixel observation status must be not-attempted")
    require(pixel_observation.get("pixelsObserved") is False, "blocked pixelObservation must not claim observed pixels")
    require(
        pixel_observation.get("consistentWithScreenshot") is False,
        "blocked pixelObservation must not claim screenshot consistency",
    )

target = controlled.get("worldCanvasPixelTarget")
require(isinstance(target, dict), "controlled artifact must include worldCanvasPixelTarget")
if isinstance(target, dict):
    require(target.get("identified") is False, "world canvas pixel target must remain unidentified")
    require(target.get("status") == "blocked", "world canvas pixel target must fail closed to blocked")
    require(
        target.get("missingTarget") == "run-window-world-canvas-screen-extents",
        "world canvas target must name the exact missing target",
    )
    require(
        target.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
        "world canvas target must name the exact next unblocker",
    )

unsupported = controlled.get("unsupportedClaims")
require(isinstance(unsupported, list), "controlled artifact must list unsupportedClaims")
if isinstance(unsupported, list):
    for claim in (
        "world-canvas-pixel-correctness",
        "full-visible-rendering-correctness",
        "full-ui-automation",
        "grading",
        "save-behavior",
        "first-lesson-completion",
    ):
        require(claim in unsupported, f"unsupportedClaims must include {claim}")

require(blocker.get("schemaVersion") == 1, "blocker artifact must have schemaVersion=1")
require(blocker.get("status") == "blocked", "blocker artifact must be blocked")
require(
    blocker.get("visibleRenderingCorrectnessEstablished") is False,
    "target blocker render evidence must explicitly keep visibleRenderingCorrectnessEstablished=false",
)
require(blocker.get("sourceArtifact") == controlled_name, "blocker artifact must point to controlled artifact")
require(
    blocker.get("missingTarget") == "run-window-world-canvas-screen-extents",
    "blocker artifact must name the exact missing target",
)
require(
    blocker.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
    "blocker artifact must name the single exact next unblocker",
)
require(blocker.get("claimScopeDetail") == "target-readiness-only", "blocker artifact must keep target-readiness-only scope detail")
require(
    blocker.get("geometryStatus") in {"missing-component-interface", "missing-extents", "invalid-extents", "ambiguous-candidates"},
    "blocker artifact must use the strict geometryStatus enum",
)
blocker_target = blocker.get("worldCanvasPixelTarget")
require(isinstance(blocker_target, dict), "blocker artifact must include worldCanvasPixelTarget")
if isinstance(blocker_target, dict):
    require(blocker_target.get("identified") is False, "blocker target must be unidentified")
    require(blocker_target.get("status") == "blocked", "blocker target must be blocked")
    require(blocker_target.get("missingTarget") == "run-window-world-canvas-screen-extents", "blocker target must name exact missing target")
    require(blocker_target.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target", "blocker target must name exact unblocker")

for path in (controlled.get("screenshot") or {}).get("path"), blocker.get("screenshotPath"):
    if path is not None:
        require(not os.path.isabs(path), f"artifact paths must be relative: {path}")
        require(path in ("screenshot.png", "screenshot.xwd"), f"unexpected screenshot path: {path}")

require(blocker_name == "visible-rendering-pixel-target-blocker.json", "test must track the fixed blocker artifact name")
require(sampling_blocker.get("status") == "blocked", "pixel sampling artifact must be blocked")
require(sampling_blocker.get("claimScope") == "visible-rendering-world-canvas-pixel-sampling", "pixel sampling artifact must use the next seam scope")
require(sampling_blocker.get("prerequisiteTargetStatus") == "blocked", "pixel sampling artifact must preserve blocked target prerequisite")
require(sampling_blocker.get("renderedWorldPixelsObserved") is False, "pixel sampling artifact must not claim rendered-world pixels")
sampling = sampling_blocker.get("pixelSampling")
require(isinstance(sampling, dict), "pixel sampling artifact must include pixelSampling object")
if isinstance(sampling, dict):
    require(sampling.get("pixelsSampled") is False, "blocked pixelSampling must not claim sampled pixels")
    require(
        sampling.get("correctnessCheck") == "not-performed",
        "blocked pixelSampling must explicitly record correctnessCheck=not-performed",
    )

if errors:
    raise AssertionError("\n".join(errors))
PY
  artifact_status=$?
  assert_success "$artifact_status" "visible-rendering artifacts preserve narrow screenshot and blocker contract"
  assert_render_nonclaim_jsons \
    "$controlled" "fallback ${controlled##*/}" \
    "$blocker" "fallback ${blocker##*/}" \
    "$sampling_blocker" "fallback ${sampling_blocker##*/}" \
    >"$tmp_root/fallback-artifacts.nonclaim.out" \
    2>"$tmp_root/fallback-artifacts.nonclaim.err"
  status=$?
  assert_success "$status" "fallback artifacts preserve render evidence as visible-correctness nonclaims"
else
  fail "visible-rendering fallback artifacts could not be inspected"
fi

observed_fixture_dir="$tmp_root/observed-fixture"
mkdir -p "$observed_fixture_dir"
cat >"$observed_fixture_dir/screenshot-pixels.txt.raw" <<'EOF'
width=640
height=480
minima=0
maxima=1
mean=0.42
colors=42
EOF

bash -c '
  . "$1"
  write_controlled_display_pixel_observation \
    "$2" \
    observed \
    none \
    "" \
    ":99" \
    true \
    controlled-display-pixels-observed-rendering-not-asserted \
    "" \
    alice-window-found \
    running \
    screenshot-captured \
    "$2/screenshot.png" \
    /usr/bin/Xvfb \
    import \
    non-black-pixels \
    "Screenshot is 640x480 with non-black pixel data." \
    after-readiness-wait \
    observed \
    x-window-inventory.json \
    1
  write_visible_rendering_pixel_target_blocker \
    "$2" \
    observed \
    none \
    "$2/screenshot.png" \
    screenshot-captured \
    non-black-pixels
' _ "$RUNNER" "$observed_fixture_dir" >"$tmp_root/observed-fixture.out" 2>"$tmp_root/observed-fixture.err"
status=$?
assert_success "$status" "runner evidence writer can be exercised as a focused rendering-seam fixture"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$observed_fixture_dir/$CONTROLLED_ARTIFACT" \
    "$observed_fixture_dir/$BLOCKER_ARTIFACT" \
    >"$tmp_root/observed-artifact-contract.out" \
    2>"$tmp_root/observed-artifact-contract.err" <<'PY'
import json
import os
import sys

controlled = json.load(open(sys.argv[1], encoding="utf-8"))
blocker = json.load(open(sys.argv[2], encoding="utf-8"))
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(controlled.get("schemaVersion") == 1, "observed artifact must have schemaVersion=1")
require(controlled.get("status") == "observed", "observed artifact must be observed")
require(controlled.get("blocker") == "none", "observed artifact must have blocker=none")
require(
    controlled.get("claimScope") == "controlled-display-screenshot-consistency",
    "observed artifact must stay scoped to screenshot consistency",
)
require(
    controlled.get("claim") == "controlled-display-pixels-observed-rendering-not-asserted",
    "observed artifact must avoid world-rendering claims",
)
require(
    controlled.get("visibleRenderingCorrectnessEstablished") is False,
    "observed screenshot/render evidence must explicitly keep visibleRenderingCorrectnessEstablished=false",
)
require(controlled.get("screenshotFile") == "screenshot.png", "top-level screenshotFile must be relative")
require(controlled.get("xvfbExecutable") == "Xvfb", "xvfbExecutable must not expose an absolute path")

screenshot = controlled.get("screenshot")
require(isinstance(screenshot, dict), "observed artifact must include screenshot metadata")
if isinstance(screenshot, dict):
    require(screenshot.get("path") == "screenshot.png", "nested screenshot path must be relative")
    require(not os.path.isabs(screenshot.get("path") or ""), "nested screenshot path must not be absolute")
    require(screenshot.get("status") == "screenshot-captured", "screenshot status must be captured")
    require(screenshot.get("metadataSource") == "screenshot-pixels.txt.raw", "dimensions must cite the raw pixel metadata source")
    dimensions = screenshot.get("dimensions")
    require(isinstance(dimensions, dict), "screenshot dimensions must be an object")
    if isinstance(dimensions, dict):
        require(dimensions.get("width") == 640, "screenshot width must come from pixel metadata")
        require(dimensions.get("height") == 480, "screenshot height must come from pixel metadata")

pixel_observation = controlled.get("pixelObservation")
require(isinstance(pixel_observation, dict), "observed artifact must include pixelObservation")
if isinstance(pixel_observation, dict):
    require(pixel_observation.get("status") == "non-black-pixels", "pixel status must be non-black-pixels")
    require(pixel_observation.get("pixelsObserved") is True, "pixelObservation must record observed pixels")
    require(
        pixel_observation.get("consistentWithScreenshot") is True,
        "pixelObservation must be consistent only when screenshot, dimensions, and pixel status agree",
    )

target = controlled.get("worldCanvasPixelTarget")
require(isinstance(target, dict), "observed artifact must include worldCanvasPixelTarget")
if isinstance(target, dict):
    require(target.get("identified") is False, "world canvas pixel target must remain unidentified")
    require(target.get("status") == "blocked", "observed world canvas pixel target must fail closed to blocked")
    require(
        target.get("missingTarget") == "run-window-world-canvas-screen-extents",
        "observed world canvas target must preserve the exact missing target",
    )
    require(
        target.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
        "observed world canvas target must preserve the exact next unblocker",
    )

unsupported = controlled.get("unsupportedClaims")
require(isinstance(unsupported, list), "observed artifact must list unsupported claims")
if isinstance(unsupported, list):
    require("world-canvas-pixel-correctness" in unsupported, "unsupported claims must include world-canvas pixel correctness")
    require("full-ui-automation" in unsupported, "unsupported claims must include full UI automation")

require(blocker.get("status") == "blocked", "visible-rendering blocker must remain blocked")
require(
    blocker.get("visibleRenderingCorrectnessEstablished") is False,
    "visible-rendering blocker must explicitly keep visibleRenderingCorrectnessEstablished=false",
)
require(blocker.get("screenshotPath") == "screenshot.png", "blocker screenshot path must be relative")
require(
    blocker.get("missingTarget") == "run-window-world-canvas-screen-extents",
    "blocker must preserve the exact missing target",
)
require(
    blocker.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
    "blocker must preserve the exact next unblocker",
)

if errors:
    raise AssertionError("\n".join(errors))
PY
  observed_artifact_status=$?
  assert_success "$observed_artifact_status" "observed screenshot-consistency artifact records relative paths, dimensions, and non-claims"
  assert_render_nonclaim_jsons \
    "$observed_fixture_dir/$CONTROLLED_ARTIFACT" "observed fixture $CONTROLLED_ARTIFACT" \
    "$observed_fixture_dir/$BLOCKER_ARTIFACT" "observed fixture $BLOCKER_ARTIFACT" \
    >"$tmp_root/observed-artifacts.nonclaim.out" \
    2>"$tmp_root/observed-artifacts.nonclaim.err"
  status=$?
  assert_success "$status" "observed fixture artifacts preserve render evidence as visible-correctness nonclaims"
else
  fail "observed screenshot-consistency fixture could not be inspected"
fi

target_ready_dir="$tmp_root/target-ready-fixture"
mkdir -p "$target_ready_dir"
cat >"$target_ready_dir/screenshot-pixels.txt.raw" <<'EOF'
width=640
height=480
minima=0
maxima=1
mean=0.42
colors=42
EOF
cat >"$target_ready_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
{
  "automationMode": "xvfb-real-alice",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 1,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": {
        "coordinateType": "screen",
        "height": 240,
        "width": 320,
        "x": 160,
        "y": 120
      },
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF

bash -c '
  . "$1"
  write_controlled_display_pixel_observation \
    "$2" \
    observed \
    none \
    "" \
    ":99" \
    true \
    controlled-display-pixels-observed-rendering-not-asserted \
    "" \
    alice-window-found \
    running \
    screenshot-captured \
    "$2/screenshot.png" \
    /usr/bin/Xvfb \
    import \
    non-black-pixels \
    "Screenshot is 640x480 with non-black pixel data." \
    after-readiness-wait \
    observed \
    x-window-inventory.json \
    1 \
    "$2/post-open-runtime-display-accessibility-evidence.json"
' _ "$RUNNER" "$target_ready_dir" >"$tmp_root/target-ready-writer.out" 2>"$tmp_root/target-ready-writer.err"
status=$?
assert_success "$status" "runner can embed target-ready world-canvas pixel target from a single valid runtime/display candidate"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$target_ready_dir/$CONTROLLED_ARTIFACT" \
    >"$tmp_root/target-ready-contract.out" \
    2>"$tmp_root/target-ready-contract.err" <<'PY'
import json
import math
import sys

controlled = json.load(open(sys.argv[1], encoding="utf-8"))
target = controlled.get("worldCanvasPixelTarget")
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def numeric(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


require(isinstance(target, dict), "controlled artifact must include target object")
require(
    controlled.get("visibleRenderingCorrectnessEstablished") is False,
    "target-ready controlled render evidence must explicitly keep visibleRenderingCorrectnessEstablished=false",
)
if isinstance(target, dict):
    require(target.get("identified") is True, "target-ready writer must identify exactly one target")
    require(target.get("status") == "target-ready", "target-ready writer must use status=target-ready")
    require(target.get("geometryStatus") == "available", "target-ready writer must preserve available geometry status")
    require(target.get("sourceArtifact") == "post-open-runtime-display-accessibility-evidence.json", "target-ready writer must cite runtime/display source artifact")
    require(target.get("candidatePath") == "application/0/3", "target-ready writer must preserve candidate path")
    require(target.get("candidateName") == "Scene display", "target-ready writer must preserve candidate name")
    require(target.get("candidateRole") == "canvas", "target-ready writer must preserve candidate role")
    require(target.get("runtimeDisplayCandidateCount") == 1, "target-ready writer must preserve candidate count")
    require(target.get("selectionRule") == "single-visible-showing-runtime-display-candidate-with-valid-screen-extents", "target-ready writer must record deterministic selection rule")
    states = target.get("candidateStates")
    require(isinstance(states, list) and "visible" in states and "showing" in states, "target-ready writer must require visible/showing candidate states")
    extents = target.get("screenExtents")
    require(isinstance(extents, dict), "target-ready writer must include screenExtents")
    if isinstance(extents, dict):
        require(extents.get("coordinateType") == "screen", "target-ready extents must use screen coordinates")
        for key in ("x", "y", "width", "height"):
            require(numeric(extents.get(key)), f"target-ready extent {key} must be numeric")
        require(extents.get("width", 0) > 0, "target-ready width must be positive")
        require(extents.get("height", 0) > 0, "target-ready height must be positive")

if errors:
    raise AssertionError("\n".join(errors))
PY
  target_ready_status=$?
  assert_success "$target_ready_status" "target-ready writer output matches pixel-sampling target contract"
  assert_render_nonclaim_json "$target_ready_dir/$CONTROLLED_ARTIFACT" "target-ready controlled artifact" \
    >"$tmp_root/target-ready-controlled.nonclaim.out" \
    2>"$tmp_root/target-ready-controlled.nonclaim.err"
  status=$?
  assert_success "$status" "target-ready controlled artifact preserves render evidence as a visible-correctness nonclaim"
fi

target_ready_sampling_dir="$tmp_root/target-ready-sampling-fixture"
mkdir -p "$target_ready_sampling_dir"
cat >"$target_ready_sampling_dir/screenshot-pixels.txt.raw" <<'EOF'
width=640
height=480
minima=0
maxima=1
mean=0.42
colors=42
EOF
cp "$target_ready_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" "$target_ready_sampling_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"

bash -c '
  . "$1"
  write_controlled_display_pixel_observation \
    "$2" \
    observed \
    none \
    "" \
    ":99" \
    true \
    controlled-display-pixels-observed-rendering-not-asserted \
    "" \
    alice-window-found \
    running \
    screenshot-captured \
    "$2/screenshot.png" \
    /usr/bin/Xvfb \
    import \
    non-black-pixels \
    "Screenshot is 640x480 with non-black pixel data." \
    after-readiness-wait \
    observed \
    x-window-inventory.json \
    1 \
    "$2/post-open-runtime-display-accessibility-evidence.json"
  write_visible_rendering_pixel_sampling_blocker \
    "$2" \
    "$2/controlled-display-pixel-observation.json"
' _ "$RUNNER" "$target_ready_sampling_dir" >"$tmp_root/target-ready-sampling-writer.out" 2>"$tmp_root/target-ready-sampling-writer.err"
status=$?
assert_success "$status" "target-ready pixel sampling seam fails closed when rendered pixels are not sampled"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$target_ready_sampling_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" \
    >"$tmp_root/target-ready-sampling-contract.out" \
    2>"$tmp_root/target-ready-sampling-contract.err" <<'PY'
import json
import sys

payload = json.load(open(sys.argv[1], encoding="utf-8"))
target = payload.get("worldCanvasPixelTarget")
sampling = payload.get("pixelSampling")
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(payload.get("schemaVersion") == 1, "pixel sampling blocker must have schemaVersion=1")
require(payload.get("status") == "blocked", "pixel sampling seam must be blocked without sampled pixels")
require(payload.get("blocker") == "world-canvas-pixel-sampler-unavailable", "blocker must name unavailable world-canvas pixel sampler")
require(payload.get("claimScope") == "visible-rendering-world-canvas-pixel-sampling", "claim scope must be the next pixel-sampling seam")
require(payload.get("claimScopeDetail") == "target-ready-sampling-not-observed", "claim scope detail must not imply rendered-world correctness")
require(payload.get("sourceArtifact") == "controlled-display-pixel-observation.json", "pixel sampling blocker must cite controlled-display source")
require(payload.get("prerequisiteTargetStatus") == "target-ready", "pixel sampling blocker must require target-ready prerequisite")
require(payload.get("renderedWorldPixelsObserved") is False, "pixel sampling blocker must not claim observed rendered-world pixels")
require(payload.get("sampleCount") == 0, "pixel sampling blocker must have zero samples")
require(payload.get("exactNextUnblocker") == "provide-world-canvas-pixel-sampler", "pixel sampling blocker must name the next unblocker")
require(isinstance(target, dict), "pixel sampling blocker must copy the target-ready metadata")
if isinstance(target, dict):
    require(target.get("identified") is True, "pixel sampling blocker target must preserve target-ready identification")
    require(target.get("status") == "target-ready", "pixel sampling blocker target must preserve target-ready status")
    require(target.get("screenExtents", {}).get("coordinateType") == "screen", "pixel sampling blocker target must preserve screen extents")
require(isinstance(sampling, dict), "pixel sampling blocker must include pixelSampling decision object")
if isinstance(sampling, dict):
    require(sampling.get("status") == "blocked", "pixelSampling must be blocked")
    require(sampling.get("blocker") == "world-canvas-pixel-sampler-unavailable", "pixelSampling must name unavailable sampler")
    require(sampling.get("pixelsSampled") is False, "pixelSampling must not claim sampled pixels")
    require(sampling.get("sampleCount") == 0, "pixelSampling sampleCount must be zero")
    require(sampling.get("samplingMethod") is None, "pixelSampling must not name a sampling method")
    require(
        sampling.get("correctnessCheck") == "not-performed",
        "pixelSampling must explicitly record correctnessCheck=not-performed",
    )
unsupported = payload.get("unsupportedClaims")
require(isinstance(unsupported, list), "pixel sampling blocker must list unsupportedClaims")
if isinstance(unsupported, list):
    for claim in (
        "world-canvas-pixel-correctness",
        "full-visible-rendering-correctness",
        "rendered-world-correctness",
    ):
        require(claim in unsupported, f"unsupportedClaims must include {claim}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  target_ready_sampling_status=$?
  assert_success "$target_ready_sampling_status" "target-ready pixel sampling blocker records fail-closed non-claim semantics"
  assert_render_nonclaim_json "$target_ready_sampling_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "target-ready sampling blocker" \
    >"$tmp_root/target-ready-sampling.nonclaim.out" \
    2>"$tmp_root/target-ready-sampling.nonclaim.err"
  status=$?
  assert_success "$status" "target-ready sampling blocker preserves render evidence as a visible-correctness nonclaim"
else
  fail "target-ready pixel sampling blocker artifact could not be inspected"
fi

source_boundary_root="$tmp_root/pixel-sampling-source-boundary"
source_boundary_nonclaim_args=()
mkdir -p "$source_boundary_root"

for source_case in missing malformed array scalar wrong-name-target-ready semantically-invalid-target-ready; do
  case_dir="$source_boundary_root/$source_case"
  source_path="$case_dir/$CONTROLLED_ARTIFACT"
  mkdir -p "$case_dir"
  case "$source_case" in
    missing)
      ;;
    malformed)
      printf '{\n' >"$source_path"
      ;;
    array)
      printf '[]\n' >"$source_path"
      ;;
    scalar)
      printf '"target-ready"\n' >"$source_path"
      ;;
    wrong-name-target-ready)
      source_path="$case_dir/not-$CONTROLLED_ARTIFACT"
      cat >"$source_path" <<'EOF'
{
  "schemaVersion": 1,
  "status": "observed",
  "claimScope": "controlled-display-screenshot-consistency",
  "screenshotStatus": "screenshot-captured",
  "screenshotPixelStatus": "non-black-pixels",
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "geometryStatus": "available",
    "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
    "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents",
    "candidateStates": ["visible", "showing"],
    "screenExtents": {
      "coordinateType": "screen",
      "x": 144,
      "y": 188,
      "width": 996,
      "height": 642
    }
  }
}
EOF
      ;;
    semantically-invalid-target-ready)
      cat >"$source_path" <<'EOF'
{
  "schemaVersion": 1,
  "status": "observed",
  "claimScope": "full-visible-rendering-correctness",
  "claim": "rendered-world-correctness",
  "screenshotStatus": "screenshot-captured",
  "screenshotPixelStatus": "non-black-pixels",
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "geometryStatus": "available",
    "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
    "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents",
    "candidateStates": ["visible", "showing"],
    "screenExtents": {
      "coordinateType": "screen",
      "x": 144,
      "y": 188,
      "width": 996,
      "height": 642
    }
  }
}
EOF
      ;;
  esac

  bash -c '
    . "$1"
    write_visible_rendering_pixel_sampling_blocker "$2" "$3"
  ' _ "$RUNNER" "$case_dir" "$source_path" >"$case_dir/writer.out" 2>"$case_dir/writer.err"
  status=$?
  assert_success "$status" "pixel sampling blocker writes fail-closed artifact for $source_case source"
  source_boundary_nonclaim_args+=(
    "$case_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT"
    "$source_case source pixel sampling blocker"
  )
done

assert_render_nonclaim_jsons "${source_boundary_nonclaim_args[@]}" \
  >"$tmp_root/pixel-sampling-source-boundary.nonclaim.out" \
  2>"$tmp_root/pixel-sampling-source-boundary.nonclaim.err"
status=$?
assert_success "$status" "source-boundary pixel sampling blockers preserve render evidence as visible-correctness nonclaims"

python3 - \
  "$source_boundary_root" \
  "$PIXEL_SAMPLING_BLOCKER_ARTIFACT" \
  >"$tmp_root/pixel-sampling-source-boundary-contract.out" \
  2>"$tmp_root/pixel-sampling-source-boundary-contract.err" <<'PY'
import json
import sys
from pathlib import Path

root = Path(sys.argv[1])
artifact_name = sys.argv[2]
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


for source_case in ("missing", "malformed", "array", "scalar", "wrong-name-target-ready", "semantically-invalid-target-ready"):
    payload_path = root / source_case / artifact_name
    require(payload_path.exists(), f"{source_case} source must produce a blocker artifact")
    if not payload_path.exists():
        continue
    payload = json.load(open(payload_path, encoding="utf-8"))
    sampling = payload.get("pixelSampling")
    target = payload.get("worldCanvasPixelTarget")
    unsupported = payload.get("unsupportedClaims")

    require(payload.get("schemaVersion") == 1, f"{source_case} blocker must use schemaVersion=1")
    require(payload.get("status") == "blocked", f"{source_case} blocker must stay blocked")
    require(payload.get("blocker") == "world-canvas-pixel-target-not-ready", f"{source_case} blocker must not advance past target validation")
    require(payload.get("claimScope") == "visible-rendering-world-canvas-pixel-sampling", f"{source_case} blocker must keep pixel-sampling claim scope")
    require(payload.get("claimScopeDetail") == "target-selection-blocked", f"{source_case} blocker must report target-selection-blocked")
    require(payload.get("sourceArtifact") == "controlled-display-pixel-observation.json", f"{source_case} blocker must preserve fixed source artifact name")
    require(payload.get("prerequisiteTargetStatus") == "unavailable", f"{source_case} blocker must treat source target as unavailable")
    require(payload.get("renderedWorldPixelsObserved") is False, f"{source_case} blocker must not claim rendered-world pixels")
    require(payload.get("sampleCount") == 0, f"{source_case} blocker must keep zero samples")
    require(target == {}, f"{source_case} blocker must not trust source target metadata")
    require(isinstance(sampling, dict), f"{source_case} blocker must include pixelSampling object")
    if isinstance(sampling, dict):
        require(sampling.get("status") == "blocked", f"{source_case} pixelSampling must be blocked")
        require(sampling.get("pixelsSampled") is False, f"{source_case} pixelSampling must not claim sampled pixels")
        require(sampling.get("sampleCount") == 0, f"{source_case} pixelSampling must keep zero samples")
        require(sampling.get("samplingMethod") is None, f"{source_case} pixelSampling must not name a sampling method")
        require(sampling.get("correctnessCheck") == "not-performed", f"{source_case} pixelSampling must record correctnessCheck=not-performed")
    require(isinstance(unsupported, list), f"{source_case} blocker must list unsupportedClaims")
    if isinstance(unsupported, list):
        for claim in (
            "world-canvas-pixel-correctness",
            "full-visible-rendering-correctness",
            "rendered-world-correctness",
        ):
            require(claim in unsupported, f"{source_case} unsupportedClaims must include {claim}")

if errors:
    raise AssertionError("\n".join(errors))
PY
source_boundary_status=$?
assert_success "$source_boundary_status" "pixel sampling blocker fails closed across malformed, wrong-name, and semantically invalid source artifacts"

target_blocked_dir="$tmp_root/target-blocked-fixture"
mkdir -p "$target_blocked_dir"
cat >"$target_blocked_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
{
  "automationMode": "xvfb-real-alice",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 1,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "geometryStatus": "missing-extents",
      "name": "Scene display",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": null,
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF

bash -c '
  . "$1"
  write_visible_rendering_pixel_target_blocker \
    "$2" \
    observed \
    none \
    "$2/screenshot.png" \
    screenshot-captured \
    non-black-pixels \
    "$2/post-open-runtime-display-accessibility-evidence.json"
' _ "$RUNNER" "$target_blocked_dir" >"$tmp_root/target-blocked-writer.out" 2>"$tmp_root/target-blocked-writer.err"
status=$?
assert_success "$status" "runner can emit exact blocker when runtime/display candidate lacks screen extents"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$target_blocked_dir/$BLOCKER_ARTIFACT" \
    >"$tmp_root/target-blocked-contract.out" \
    2>"$tmp_root/target-blocked-contract.err" <<'PY'
import json
import sys

blocker = json.load(open(sys.argv[1], encoding="utf-8"))
target = blocker.get("worldCanvasPixelTarget")
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


require(blocker.get("status") == "blocked", "blocker writer must emit blocked status")
require(blocker.get("missingTarget") == "run-window-world-canvas-screen-extents", "blocker writer must name exact missing target")
require(blocker.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target", "blocker writer must name exact next unblocker")
require(blocker.get("runtimeDisplayCandidateCount") == 1, "blocker writer must preserve candidate count")
require(blocker.get("geometryStatus") == "missing-extents", "blocker writer must preserve missing-extents geometry status")
require(blocker.get("claimScopeDetail") == "target-readiness-only", "blocker writer must keep target-readiness-only scope")
require(isinstance(target, dict), "blocker writer must include worldCanvasPixelTarget")
if isinstance(target, dict):
    require(target.get("identified") is False, "blocked target must not be identified")
    require(target.get("status") == "blocked", "blocked target must use status=blocked")
    require(target.get("missingTarget") == "run-window-world-canvas-screen-extents", "blocked target must name exact missing target")
    require(target.get("exactNextUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target", "blocked target must name exact next unblocker")
    require(target.get("geometryStatus") == "missing-extents", "blocked target must preserve missing-extents")

if errors:
    raise AssertionError("\n".join(errors))
PY
  target_blocked_status=$?
  assert_success "$target_blocked_status" "target blocker writer output preserves exact blocker contract"
  assert_render_nonclaim_json "$target_blocked_dir/$BLOCKER_ARTIFACT" "target blocked blocker" \
    >"$tmp_root/target-blocked.nonclaim.out" \
    2>"$tmp_root/target-blocked.nonclaim.err"
  status=$?
  assert_success "$status" "target blocked blocker preserves render evidence as a visible-correctness nonclaim"
fi

target_multiple_missing_dir="$tmp_root/target-multiple-missing-fixture"
mkdir -p "$target_multiple_missing_dir"
cat >"$target_multiple_missing_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
{
  "automationMode": "xvfb-real-alice",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 2,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "geometryStatus": "missing-extents",
      "name": "Scene display A",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": null,
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "missing-extents",
      "name": "Scene display B",
      "path": "application/0/4",
      "role": "canvas",
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF

bash -c '
  . "$1"
  write_controlled_display_pixel_observation \
    "$2" \
    observed \
    none \
    "" \
    ":99" \
    true \
    controlled-display-pixels-observed-rendering-not-asserted \
    "" \
    alice-window-found \
    running \
    screenshot-captured \
    "$2/screenshot.png" \
    /usr/bin/Xvfb \
    import \
    non-black-pixels \
    "Screenshot is 640x480 with non-black pixel data." \
    after-readiness-wait \
    observed \
    x-window-inventory.json \
    2 \
    "$2/post-open-runtime-display-accessibility-evidence.json"
  write_visible_rendering_pixel_target_blocker \
    "$2" \
    observed \
    none \
    "$2/screenshot.png" \
    screenshot-captured \
    non-black-pixels \
    "$2/post-open-runtime-display-accessibility-evidence.json"
' _ "$RUNNER" "$target_multiple_missing_dir" >"$tmp_root/target-multiple-missing-writer.out" 2>"$tmp_root/target-multiple-missing-writer.err"
status=$?
assert_success "$status" "runner handles multiple visible/showing candidates with missing extents as non-ambiguous blockers"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$target_multiple_missing_dir/$CONTROLLED_ARTIFACT" \
    "$target_multiple_missing_dir/$BLOCKER_ARTIFACT" \
    >"$tmp_root/target-multiple-missing-contract.out" \
    2>"$tmp_root/target-multiple-missing-contract.err" <<'PY'
import json
import sys

controlled = json.load(open(sys.argv[1], encoding="utf-8"))
blocker = json.load(open(sys.argv[2], encoding="utf-8"))
controlled_target = controlled.get("worldCanvasPixelTarget") or {}
blocker_target = blocker.get("worldCanvasPixelTarget") or {}

for label, payload, target in (
    ("controlled", controlled, controlled_target),
    ("blocker", blocker, blocker_target),
):
    if payload.get("geometryStatus") == "ambiguous-candidates":
        raise AssertionError(f"{label} artifact must not mark missing extents as ambiguous")
    if target.get("geometryStatus") != "missing-extents":
        raise AssertionError(f"{label} target must preserve missing-extents, got {target.get('geometryStatus')!r}")
    if target.get("identified") is not False:
        raise AssertionError(f"{label} target must not be identified with zero valid extents")
    if target.get("status") != "blocked":
        raise AssertionError(f"{label} target must stay blocked with zero valid extents")
    if target.get("runtimeDisplayCandidateCount") != 2:
        raise AssertionError(f"{label} target must preserve raw candidate count")
if controlled.get("claim") != "controlled-display-pixels-observed-rendering-not-asserted":
    raise AssertionError("controlled artifact may keep only screenshot-consistency claim")
PY
  target_multiple_missing_status=$?
  assert_success "$target_multiple_missing_status" "multiple missing-extents candidates do not produce ambiguity or readiness"
  assert_render_nonclaim_jsons \
    "$target_multiple_missing_dir/$CONTROLLED_ARTIFACT" "multiple missing $CONTROLLED_ARTIFACT" \
    "$target_multiple_missing_dir/$BLOCKER_ARTIFACT" "multiple missing $BLOCKER_ARTIFACT" \
    >"$tmp_root/multiple-missing-artifacts.nonclaim.out" \
    2>"$tmp_root/multiple-missing-artifacts.nonclaim.err"
  status=$?
  assert_success "$status" "multiple missing artifacts preserve render evidence as visible-correctness nonclaims"
fi

target_multiple_invalid_dir="$tmp_root/target-multiple-invalid-fixture"
mkdir -p "$target_multiple_invalid_dir"
cat >"$target_multiple_invalid_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
{
  "automationMode": "xvfb-real-alice",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 5,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display zero",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": {"coordinateType": "screen", "height": 240, "width": 0, "x": 160, "y": 120},
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display negative",
      "path": "application/0/4",
      "role": "canvas",
      "screenExtents": {"coordinateType": "screen", "height": -1, "width": 320, "x": 500, "y": 120},
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display malformed",
      "path": "application/0/5",
      "role": "canvas",
      "screenExtents": {"coordinateType": "screen", "height": 240, "width": "wide", "x": 840, "y": 120},
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display non-screen",
      "path": "application/0/6",
      "role": "canvas",
      "screenExtents": {"coordinateType": "component", "height": 240, "width": 320, "x": 0, "y": 0},
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display hidden",
      "path": "application/0/7",
      "role": "canvas",
      "screenExtents": {"coordinateType": "screen", "height": 240, "width": 320, "x": 1180, "y": 120},
      "states": ["enabled"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF

bash -c '
  . "$1"
  write_controlled_display_pixel_observation \
    "$2" \
    observed \
    none \
    "" \
    ":99" \
    true \
    controlled-display-pixels-observed-rendering-not-asserted \
    "" \
    alice-window-found \
    running \
    screenshot-captured \
    "$2/screenshot.png" \
    /usr/bin/Xvfb \
    import \
    non-black-pixels \
    "Screenshot is 640x480 with non-black pixel data." \
    after-readiness-wait \
    observed \
    x-window-inventory.json \
    5 \
    "$2/post-open-runtime-display-accessibility-evidence.json"
  write_visible_rendering_pixel_target_blocker \
    "$2" \
    observed \
    none \
    "$2/screenshot.png" \
    screenshot-captured \
    non-black-pixels \
    "$2/post-open-runtime-display-accessibility-evidence.json"
' _ "$RUNNER" "$target_multiple_invalid_dir" >"$tmp_root/target-multiple-invalid-writer.out" 2>"$tmp_root/target-multiple-invalid-writer.err"
status=$?
assert_success "$status" "runner handles multiple visible/showing candidates with invalid extents as non-ambiguous blockers"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$target_multiple_invalid_dir/$CONTROLLED_ARTIFACT" \
    "$target_multiple_invalid_dir/$BLOCKER_ARTIFACT" \
    >"$tmp_root/target-multiple-invalid-contract.out" \
    2>"$tmp_root/target-multiple-invalid-contract.err" <<'PY'
import json
import sys

controlled = json.load(open(sys.argv[1], encoding="utf-8"))
blocker = json.load(open(sys.argv[2], encoding="utf-8"))

for label, payload in (("controlled", controlled), ("blocker", blocker)):
    target = payload.get("worldCanvasPixelTarget") or {}
    if payload.get("geometryStatus") == "ambiguous-candidates":
        raise AssertionError(f"{label} artifact must not mark invalid extents as ambiguous")
    if target.get("geometryStatus") != "invalid-extents":
        raise AssertionError(f"{label} target must preserve invalid-extents, got {target.get('geometryStatus')!r}")
    if target.get("identified") is not False:
        raise AssertionError(f"{label} target must not be identified with invalid extents")
    if target.get("status") != "blocked":
        raise AssertionError(f"{label} target must stay blocked with invalid extents")
    if target.get("runtimeDisplayCandidateCount") != 5:
        raise AssertionError(f"{label} target must preserve raw candidate count")
PY
  target_multiple_invalid_status=$?
  assert_success "$target_multiple_invalid_status" "multiple invalid-extents candidates do not produce ambiguity or readiness"
  assert_render_nonclaim_jsons \
    "$target_multiple_invalid_dir/$CONTROLLED_ARTIFACT" "multiple invalid $CONTROLLED_ARTIFACT" \
    "$target_multiple_invalid_dir/$BLOCKER_ARTIFACT" "multiple invalid $BLOCKER_ARTIFACT" \
    >"$tmp_root/multiple-invalid-artifacts.nonclaim.out" \
    2>"$tmp_root/multiple-invalid-artifacts.nonclaim.err"
  status=$?
  assert_success "$status" "multiple invalid artifacts preserve render evidence as visible-correctness nonclaims"
fi

target_ambiguous_dir="$tmp_root/target-ambiguous-fixture"
mkdir -p "$target_ambiguous_dir"
cat >"$target_ambiguous_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
{
  "automationMode": "xvfb-real-alice",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 2,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display A",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": {
        "coordinateType": "screen",
        "height": 240,
        "width": 320,
        "x": 160,
        "y": 120
      },
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display B",
      "path": "application/0/4",
      "role": "canvas",
      "screenExtents": {
        "coordinateType": "screen",
        "height": 240,
        "width": 320,
        "x": 500,
        "y": 120
      },
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF

bash -c '
  . "$1"
  write_controlled_display_pixel_observation \
    "$2" \
    observed \
    none \
    "" \
    ":99" \
    true \
    controlled-display-pixels-observed-rendering-not-asserted \
    "" \
    alice-window-found \
    running \
    screenshot-captured \
    "$2/screenshot.png" \
    /usr/bin/Xvfb \
    import \
    non-black-pixels \
    "Screenshot is 640x480 with non-black pixel data." \
    after-readiness-wait \
    observed \
    x-window-inventory.json \
    2 \
    "$2/post-open-runtime-display-accessibility-evidence.json"
  write_visible_rendering_pixel_target_blocker \
    "$2" \
    observed \
    none \
    "$2/screenshot.png" \
    screenshot-captured \
    non-black-pixels \
    "$2/post-open-runtime-display-accessibility-evidence.json"
' _ "$RUNNER" "$target_ambiguous_dir" >"$tmp_root/target-ambiguous-writer.out" 2>"$tmp_root/target-ambiguous-writer.err"
status=$?
assert_success "$status" "runner can fail closed when multiple valid runtime/display targets are ambiguous"

if [ "$status" -eq 0 ]; then
  python3 - \
    "$target_ambiguous_dir/$CONTROLLED_ARTIFACT" \
    "$target_ambiguous_dir/$BLOCKER_ARTIFACT" \
    >"$tmp_root/target-ambiguous-contract.out" \
    2>"$tmp_root/target-ambiguous-contract.err" <<'PY'
import json
import sys

controlled = json.load(open(sys.argv[1], encoding="utf-8"))
blocker = json.load(open(sys.argv[2], encoding="utf-8"))

for label, payload in (("controlled", controlled), ("blocker", blocker)):
    target = payload.get("worldCanvasPixelTarget") or {}
    if target.get("geometryStatus") != "ambiguous-candidates":
        raise AssertionError(f"{label} target must preserve geometryStatus=ambiguous-candidates")
    if target.get("identified") is not False:
        raise AssertionError(f"{label} target must fail closed without identifying a target")
    if target.get("status") != "blocked":
        raise AssertionError(f"{label} target must remain blocked when multiple valid targets exist")
    if target.get("runtimeDisplayCandidateCount") != 2:
        raise AssertionError(f"{label} target must preserve candidate count")
if blocker.get("geometryStatus") != "ambiguous-candidates":
    raise AssertionError("ambiguous blocker must use geometryStatus=ambiguous-candidates")
if controlled.get("claim") != "controlled-display-pixels-observed-rendering-not-asserted":
    raise AssertionError("controlled artifact may keep only screenshot-consistency claim")
PY
  target_ambiguous_status=$?
  assert_success "$target_ambiguous_status" "ambiguous target writers preserve fail-closed geometry status"
  assert_render_nonclaim_jsons \
    "$target_ambiguous_dir/$CONTROLLED_ARTIFACT" "ambiguous $CONTROLLED_ARTIFACT" \
    "$target_ambiguous_dir/$BLOCKER_ARTIFACT" "ambiguous $BLOCKER_ARTIFACT" \
    >"$tmp_root/ambiguous-artifacts.nonclaim.out" \
    2>"$tmp_root/ambiguous-artifacts.nonclaim.err"
  status=$?
  assert_success "$status" "ambiguous artifacts preserve render evidence as visible-correctness nonclaims"
fi

assert_contains "$RUNNER" 'screenshot-pixels\.txt\.raw' "runner derives screenshot dimensions from the existing screenshot analysis artifact"
assert_contains "$RUNNER" 'worldCanvasPixelTarget' "runner records the world-canvas pixel target status"
assert_contains "$RUNNER" 'unsupportedClaims' "runner records unsupported visible-rendering claims"
assert_contains "$RUNNER" "$BLOCKER_ARTIFACT" "runner writes the fixed pixel-target blocker artifact"
assert_contains "$RUNNER" "$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "runner writes the fixed pixel-sampling blocker artifact"
assert_contains "$RUNNER" 'screenExtents' "runner threads runtime/display screen extents into the pixel-target contract"
assert_contains "$RUNNER" 'exactNextUnblocker' "runner names the exact next unblocker for blocked pixel targets"

finish
