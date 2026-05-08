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
FIXTURE_DIR="$SCRIPT_DIR/fixtures/visible-rendering"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

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
  status_file="$run_dir/status.txt"
  assert_file_exists "$controlled" "runner writes controlled-display screenshot-consistency artifact"
  assert_file_exists "$blocker" "runner writes precise visible-rendering pixel-target blocker artifact"
  assert_file_exists "$status_file" "runner writes visible-rendering status linkage"
  assert_contains "$status_file" "^visibleRenderingPixelTargetBlocker=$BLOCKER_ARTIFACT$" "status links the visible-rendering blocker artifact"

  python3 - \
    "$controlled" \
    "$blocker" \
    "$CONTROLLED_ARTIFACT" \
    "$BLOCKER_ARTIFACT" \
    >"$tmp_root/artifact-contract.out" \
    2>"$tmp_root/artifact-contract.err" <<'PY'
import json
import os
import sys

controlled_path, blocker_path, controlled_name, blocker_name = sys.argv[1:5]
controlled = json.load(open(controlled_path, encoding="utf-8"))
blocker = json.load(open(blocker_path, encoding="utf-8"))
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

if errors:
    raise AssertionError("\n".join(errors))
PY
  artifact_status=$?
  assert_success "$artifact_status" "visible-rendering artifacts preserve narrow screenshot and blocker contract"
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
  assert_success "$target_ready_status" "target-ready writer output matches future pixel-sampling target contract"
fi

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
fi

assert_contains "$RUNNER" 'screenshot-pixels\.txt\.raw' "runner derives screenshot dimensions from the existing screenshot analysis artifact"
assert_contains "$RUNNER" 'worldCanvasPixelTarget' "runner records the world-canvas pixel target status"
assert_contains "$RUNNER" 'unsupportedClaims' "runner records unsupported visible-rendering claims"
assert_contains "$RUNNER" "$BLOCKER_ARTIFACT" "runner writes the fixed pixel-target blocker artifact"
assert_contains "$RUNNER" 'screenExtents' "runner threads runtime/display screen extents into the pixel-target contract"
assert_contains "$RUNNER" 'exactNextUnblocker' "runner names the exact next unblocker for blocked pixel targets"

finish
