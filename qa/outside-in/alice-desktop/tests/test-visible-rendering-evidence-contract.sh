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

assert_contains "$RUNNER" 'screenshot-pixels\.txt\.raw' "runner derives screenshot dimensions from the existing screenshot analysis artifact"
assert_contains "$RUNNER" 'worldCanvasPixelTarget' "runner records the world-canvas pixel target status"
assert_contains "$RUNNER" 'unsupportedClaims' "runner records unsupported visible-rendering claims"
assert_contains "$RUNNER" "$BLOCKER_ARTIFACT" "runner writes the fixed pixel-target blocker artifact"

finish
