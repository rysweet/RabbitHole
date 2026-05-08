#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCENARIO_ID=alice-desktop-post-open-runtime-display-accessibility-evidence
CONTROLLED_ARTIFACT=controlled-display-pixel-observation.json
BLOCKER_ARTIFACT=visible-rendering-pixel-target-blocker.json
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

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
    require(
        target.get("missingUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
        "world canvas target must name the exact missing unblocker",
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
    blocker.get("missingUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
    "blocker artifact must name the single exact missing unblocker",
)
blocker_target = blocker.get("worldCanvasPixelTarget")
require(isinstance(blocker_target, dict), "blocker artifact must include worldCanvasPixelTarget")
if isinstance(blocker_target, dict):
    require(blocker_target.get("identified") is False, "blocker target must be unidentified")

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
    require(
        target.get("missingUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
        "world canvas target must preserve the exact missing unblocker",
    )

unsupported = controlled.get("unsupportedClaims")
require(isinstance(unsupported, list), "observed artifact must list unsupported claims")
if isinstance(unsupported, list):
    require("world-canvas-pixel-correctness" in unsupported, "unsupported claims must include world-canvas pixel correctness")
    require("full-ui-automation" in unsupported, "unsupported claims must include full UI automation")

require(blocker.get("status") == "blocked", "visible-rendering blocker must remain blocked")
require(blocker.get("screenshotPath") == "screenshot.png", "blocker screenshot path must be relative")
require(
    blocker.get("missingUnblocker") == "reliable-run-window-world-canvas-pixel-sampling-target",
    "blocker must preserve the exact missing unblocker",
)

if errors:
    raise AssertionError("\n".join(errors))
PY
  observed_artifact_status=$?
  assert_success "$observed_artifact_status" "observed screenshot-consistency artifact records relative paths, dimensions, and non-claims"
else
  fail "observed screenshot-consistency fixture could not be inspected"
fi

assert_contains "$RUNNER" 'screenshot-pixels\.txt\.raw' "runner derives screenshot dimensions from the existing screenshot analysis artifact"
assert_contains "$RUNNER" 'worldCanvasPixelTarget' "runner records the world-canvas pixel target status"
assert_contains "$RUNNER" 'unsupportedClaims' "runner records unsupported visible-rendering claims"
assert_contains "$RUNNER" "$BLOCKER_ARTIFACT" "runner writes the fixed pixel-target blocker artifact"

finish
