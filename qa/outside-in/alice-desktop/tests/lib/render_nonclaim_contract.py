import json
import re
import sys

__all__ = ["main"]

POSITIVE_CORRECTNESS_PATTERNS = (
    re.compile(r"\b(proves?|proved|shows?|showed|demonstrates?|demonstrated|establish(?:es|ed)?|confirms?|validates?|verifies?|certifies?|asserts?)\b.{0,80}?\b(visible|visual|rendered|rendering)\b.{0,60}?\b(correct|correctness|correctly|valid|validity|passed)\b"),
    re.compile(r"\b(visible|visual|rendered|rendering)\b.{0,60}?\b(correct|correctness|correctly|valid|validation|validity)\b.{0,60}?\b(proven|proved|shown|showed|demonstrated|established|confirmed|validated|verified|certified|asserted|passed)\b"),
    re.compile(r"\bvisibly correct\b"),
)
NEGATION_MARKERS = (
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
RENDER_EVIDENCE_KEYS = {
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
CLAIM_KEYS = {"claim", "claimScope", "claimScopeDetail", "boundedClaim"}
POSITIVE_VISIBLE_CORRECTNESS_TOKENS = (
    "full-visible-rendering-correctness",
    "rendered-world-correctness",
    "visible-rendering-correctness",
    "visual-correctness",
    "world-canvas-pixel-correctness",
)


def _sentences(text):
    return [part.strip().lower() for part in re.split(r"(?<=[.!?])\s+|\n+", text) if part.strip()]


def _is_negated(sentence):
    return any(marker in sentence for marker in NEGATION_MARKERS)


def _key_implies_visible_correctness(key):
    lower = key.lower()
    return any(token in lower for token in ("correct", "validat", "validity")) and any(
        token in lower
        for token in ("visible", "visual", "rendered", "rendering", "world")
    )


def _positive_correctness_value(value):
    if value is True:
        return True
    if isinstance(value, str):
        lower = value.strip().lower()
        if any(marker in lower for marker in NEGATION_MARKERS):
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
            "valid",
            "validated",
            "verified",
        }
    if isinstance(value, dict):
        return any(_positive_correctness_value(child) for child in value.values())
    if isinstance(value, list):
        return any(_positive_correctness_value(child) for child in value)
    return False


def _claim_value_implies_visible_correctness(key, value):
    if key not in CLAIM_KEYS or not isinstance(value, str):
        return False
    lower = value.strip().lower()
    if any(marker in lower for marker in NEGATION_MARKERS):
        return False
    return any(token in lower for token in POSITIVE_VISIBLE_CORRECTNESS_TOKENS)


def _collect_contract_inputs(value, state):
    if isinstance(value, dict):
        _collect_mapping_inputs(value, state)
    elif isinstance(value, list):
        for child in value:
            _collect_contract_inputs(child, state)
    elif isinstance(value, str):
        state["texts"].append(value)


def _collect_mapping_inputs(value, state):
    for key, child in value.items():
        if key in RENDER_EVIDENCE_KEYS:
            state["has_render_evidence"] = True
        if key == "correctnessCheck":
            state["correctness_checks"].append(child)
        if key == "unsupportedClaims":
            continue
        if _claim_value_implies_visible_correctness(key, child):
            state["positive_correctness_fields"].append((key, child))
        if _key_implies_visible_correctness(key) and _positive_correctness_value(child):
            state["positive_correctness_fields"].append((key, child))
        _collect_contract_inputs(child, state)


def _correctness_claim_is_negated(sentence, match):
    clause_start = 0
    for delimiter in (";", ",", ".", "!", "?", "\n"):
        index = sentence.rfind(delimiter, 0, match.start())
        if index >= clause_start:
            clause_start = index + 1
    return _is_negated(sentence[clause_start:match.end()])


def _explicit_visible_correctness_observation(value):
    evidence = value.get("visibleCorrectnessObservationEvidence") if isinstance(value, dict) else None
    return isinstance(evidence, dict) and evidence.get("status") == "observed"


def _unsupported_claims_document_boundary(value):
    claims = value.get("unsupportedClaims") if isinstance(value, dict) else None
    return isinstance(claims, list) and "full-visible-rendering-correctness" in claims


def _text_documents_boundary(texts):
    return any(
        ("visible" in sentence or "visual" in sentence or "render" in sentence)
        and ("correct" in sentence or "correctness" in sentence or "validity" in sentence)
        and _is_negated(sentence)
        for text in texts
        for sentence in _sentences(text)
    )


def _new_state():
    return {
        "has_render_evidence": False,
        "correctness_checks": [],
        "positive_correctness_fields": [],
        "texts": [],
    }


def _check_payload(path, label, errors):
    with open(path, encoding="utf-8") as artifact:
        payload = json.load(artifact)
    state = _new_state()
    _collect_contract_inputs(payload, state)
    _check_render_evidence_invariants(payload, label, state, errors)
    _check_text_claims(label, state["texts"], errors)


def _check_render_evidence_invariants(payload, label, state, errors):
    if not state["has_render_evidence"] or _explicit_visible_correctness_observation(payload):
        return
    if payload.get("visibleRenderingCorrectnessEstablished") is not False:
        errors.append(f"{label} must keep visibleRenderingCorrectnessEstablished=false without observation evidence")
    for value in state["correctness_checks"]:
        if value != "not-performed":
            errors.append(f"{label} correctnessCheck must be not-performed without observation evidence")
    for key, value in state["positive_correctness_fields"]:
        errors.append(f"{label} must not report {key}={value!r} without observation evidence")
    if not _unsupported_claims_document_boundary(payload) and not _text_documents_boundary(state["texts"]):
        errors.append(f"{label} must document the visible-correctness nonclaim boundary")


def _check_text_claims(label, texts, errors):
    for text in texts:
        for sentence in _sentences(text):
            _check_sentence_claim(label, sentence, errors)


def _check_sentence_claim(label, sentence, errors):
    for pattern in POSITIVE_CORRECTNESS_PATTERNS:
        for match in pattern.finditer(sentence):
            if not _correctness_claim_is_negated(sentence, match):
                errors.append(f"{label} must not make positive visible-correctness claims from render evidence: {sentence}")
                return


def main():
    args = sys.argv[1:]
    if not args or len(args) % 2:
        raise SystemExit("render_nonclaim_contract.py requires artifact/label pairs")
    errors = []
    for index in range(0, len(args), 2):
        _check_payload(args[index], args[index + 1], errors)
    if errors:
        raise AssertionError("\n".join(errors))


if __name__ == "__main__":
    main()
