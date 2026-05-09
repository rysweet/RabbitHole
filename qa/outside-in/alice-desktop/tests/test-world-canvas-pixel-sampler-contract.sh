#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-world-canvas-pixel-sampler-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SAMPLER="$BASE_DIR/runners/world-canvas-pixel-sampler.py"
POST_OPEN_RUNTIME_DISPLAY_ARTIFACT=post-open-runtime-display-accessibility-evidence.json
CONTROLLED_ARTIFACT=controlled-display-pixel-observation.json
PIXEL_OBSERVATION_ARTIFACT=visible-rendering-pixel-observation.json
PIXEL_SAMPLING_BLOCKER_ARTIFACT=visible-rendering-pixel-sampling-blocker.json
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

write_screenshot_metadata() {
  local case_dir=$1
  cat >"$case_dir/screenshot-pixels.txt.raw" <<'EOF'
width=640
height=480
minima=0
maxima=1
mean=0.42
colors=42
EOF
}

write_runtime_display_artifact() {
  local case_dir=$1
  local shape=$2

  case "$shape" in
    ready)
      cat >"$case_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
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
      ;;
    missing)
      cat >"$case_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
{
  "automationMode": "xvfb-real-alice",
  "blocker": "runtime-display-candidate-missing",
  "blockerDetail": "No runtime/display candidate was found.",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": false,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 0,
  "runtimeDisplayCandidates": [],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "blocked",
  "traversalErrors": []
}
EOF
      ;;
    ambiguous)
      cat >"$case_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
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
      "screenExtents": {"coordinateType": "screen", "height": 240, "width": 320, "x": 160, "y": 120},
      "states": ["enabled", "showing", "visible"]
    },
    {
      "childCount": 0,
      "geometryStatus": "available",
      "name": "Scene display B",
      "path": "application/0/4",
      "role": "canvas",
      "screenExtents": {"coordinateType": "screen", "height": 240, "width": 320, "x": 500, "y": 120},
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF
      ;;
    invalid)
      cat >"$case_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'EOF'
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
      "screenExtents": {"coordinateType": "screen", "height": 240, "width": 0, "x": 160, "y": 120},
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
EOF
      ;;
    *)
      printf 'unknown runtime display fixture: %s\n' "$shape" >&2
      return 1
      ;;
  esac
}

write_controlled_artifact() {
  local case_dir=$1
  local candidate_count=$2

  write_screenshot_metadata "$case_dir"
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
      "$3" \
      "$2/post-open-runtime-display-accessibility-evidence.json"
  ' _ "$RUNNER" "$case_dir" "$candidate_count"
}

write_sampler_fixture() {
  local sampler_script=$1
  local mode=$2
  local invocation_log=$3

  {
    printf '#!/usr/bin/env bash\n'
    printf 'set -u\n\n'
    printf 'mode=%s\n' "$(printf '%q' "$mode")"
    printf 'invocation_log=%s\n' "$(printf '%q' "$invocation_log")"
    cat <<'SH'
target_json=
output_json=

while [ "$#" -gt 0 ]; do
  case "$1" in
    --target-json)
      target_json=$2
      shift 2
      ;;
    --output)
      output_json=$2
      shift 2
      ;;
    *)
      printf 'unexpected sampler argument: %s\n' "$1" >&2
      exit 64
      ;;
  esac
done

printf 'invoked\n' >>"$invocation_log"

python3 - "$mode" "$target_json" "$output_json" <<'PY'
import json
import sys
from pathlib import Path

mode, target_path, output_path = sys.argv[1:4]
target = json.load(open(target_path, encoding="utf-8"))
extents = target["screenExtents"]
samples = [
    {"point": {"x": extents["x"] + 1, "y": extents["y"] + 1}, "rgba": [0, 0, 0, 255], "checked": True},
    {"point": {"x": extents["x"] + extents["width"] // 2, "y": extents["y"] + extents["height"] // 2}, "rgba": [16, 32, 48, 255], "checked": True},
    {"point": {"x": extents["x"] + extents["width"] - 1, "y": extents["y"] + extents["height"] - 1}, "rgba": [64, 96, 128, 255], "checked": True},
]

payload = {
    "schemaVersion": 1,
    "status": "observed",
    "samplingMethod": "fixture-rgba-samples",
    "sampleCount": len(samples),
    "samples": samples,
}
if mode == "overclaim":
    payload["visibleRenderingCorrectnessEstablished"] = True
    payload["claim"] = "visible rendering correctness established"
elif mode == "visual-validation-overclaim":
    payload["visualValidationStatus"] = "passed"
elif mode == "correctness-check-overclaim":
    payload["correctnessCheck"] = "performed"
elif mode == "claim-scope-overclaim":
    payload["claimScope"] = "full-visible-rendering-correctness"

Path(output_path).write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
PY
SH
  } >"$sampler_script"
  chmod +x "$sampler_script"
}

run_sampling_evidence() {
  local case_dir=$1
  local sampler_path=$2

  ALICE_QA_WORLD_CANVAS_PIXEL_SAMPLER="$sampler_path" \
  bash -c '
    . "$1"
    write_visible_rendering_pixel_sampling_evidence \
      "$2" \
      "$2/controlled-display-pixel-observation.json"
  ' _ "$RUNNER" "$case_dir"
}

assert_no_forbidden_correctness_claims() {
  local artifact=$1
  python3 - "$artifact" <<'PY'
import json
import sys

payload = json.load(open(sys.argv[1], encoding="utf-8"))
errors = []
forbidden = (
    "visible rendering correctness established",
    "visible rendering correctness passed",
    "rendered-world correctness established",
    "world canvas pixel correctness passed",
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
        lowered = value.lower()
        for phrase in forbidden:
            if phrase in lowered:
                errors.append(f"forbidden correctness phrase present: {phrase}")


walk(payload)
if errors:
    raise AssertionError("\n".join(errors))
PY
}

assert_file_exists "$SAMPLER" "world-canvas pixel sampler script is checked in at the documented path"

python3 - "$SAMPLER" >"$tmp_root/channel-normalization.out" 2>"$tmp_root/channel-normalization.err" <<'PY'
import importlib.util
import sys

sys.dont_write_bytecode = True
sampler_path = sys.argv[1]
spec = importlib.util.spec_from_file_location("world_canvas_pixel_sampler", sampler_path)
if spec is None or spec.loader is None:
    raise AssertionError("sampler module could not be loaded")
sampler = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sampler)

cases = {
    -1: 0,
    0: 0,
    255: 255,
    65535: 255,
}
for value, expected in cases.items():
    actual = sampler.normalize_channel(value)
    if actual != expected:
        raise AssertionError(f"normalize_channel({value}) returned {actual}, expected {expected}")

parsed = sampler.parse_rgba("srgba(-1,65535,32,1)")
if parsed != [0, 255, 32, 255]:
    raise AssertionError(f"parse_rgba did not normalize channels into 0..255: {parsed!r}")
PY
status=$?
assert_success "$status" "standalone sampler normalizes RGBA channels into the 0..255 range"

unreadable_target_out="$tmp_root/unreadable-target-sampler.json"
python3 "$SAMPLER" \
  --target-json "$tmp_root/missing-target.json" \
  --output "$unreadable_target_out" \
  >"$tmp_root/unreadable-target-sampler.out" \
  2>"$tmp_root/unreadable-target-sampler.err"
status=$?
assert_success "$status" "standalone sampler exits 0 with structured blocker for unreadable target JSON"
assert_file_exists "$unreadable_target_out" "standalone sampler writes blocker JSON for unreadable target JSON"
assert_contains "$unreadable_target_out" '"status": "blocked"' "standalone sampler records blocked status for unreadable target JSON"
assert_contains "$unreadable_target_out" '"blocker": "target-json-unreadable"' "standalone sampler names unreadable target blocker"
assert_contains "$unreadable_target_out" '"claimScope": "visible-rendering-world-canvas-pixel-sampling"' "standalone sampler keeps bounded pixel-sampling claim scope on blockers"
assert_contains "$unreadable_target_out" '"claimScopeDetail": "target-scoped-raw-pixel-observation-only"' "standalone sampler keeps raw-observation-only scope detail on blockers"
assert_contains "$unreadable_target_out" '"renderedWorldPixelsObserved": false' "standalone sampler blocker does not claim sampled rendered-world pixels"
assert_contains "$unreadable_target_out" '"visibleRenderingCorrectnessEstablished": false' "standalone sampler blocker does not claim visible rendering correctness"
assert_contains "$unreadable_target_out" '"unsupportedClaims":' "standalone sampler blocker lists unsupported claim classes"
assert_contains "$unreadable_target_out" '"full-visible-rendering-correctness"' "standalone sampler blocker explicitly excludes full visible rendering correctness"
assert_contains "$unreadable_target_out" '"rendered-world-correctness"' "standalone sampler blocker explicitly excludes rendered-world correctness"
assert_contains "$unreadable_target_out" '"world-execution"' "standalone sampler blocker explicitly excludes world execution"

standalone_dir="$tmp_root/standalone-sampler"
mkdir -p "$standalone_dir/fake-bin"
cat >"$standalone_dir/target.json" <<'JSON'
{
  "screenExtents": {
    "coordinateType": "screen",
    "height": 20,
    "width": 20,
    "x": 10,
    "y": 20
  },
  "status": "target-ready"
}
JSON
cat >"$standalone_dir/fake-bin/xwd" <<'SH'
#!/usr/bin/env bash
set -u
printf 'xwd\n' >>"$SAMPLER_XWD_LOG"
printf 'fake-root-window-image'
SH
cat >"$standalone_dir/fake-bin/convert" <<'SH'
#!/usr/bin/env bash
set -u
printf 'convert %s\n' "$*" >>"$SAMPLER_CONVERT_LOG"
while IFS= read -r _sampler_input; do
  :
done
printf 'srgba(16,32,48,1)\n'
printf 'srgb(64,96,128)\n'
printf '(0,0,0,255)\n'
SH
chmod +x "$standalone_dir/fake-bin/xwd" "$standalone_dir/fake-bin/convert"
standalone_out="$standalone_dir/pixel-observation.json"
SAMPLER_XWD_LOG="$standalone_dir/xwd.log" \
SAMPLER_CONVERT_LOG="$standalone_dir/convert.log" \
PATH="$standalone_dir/fake-bin:$PATH" \
  python3 "$SAMPLER" \
    --target-json "$standalone_dir/target.json" \
    --output "$standalone_out" \
    >"$standalone_dir/sampler.out" \
    2>"$standalone_dir/sampler.err"
status=$?
assert_success "$status" "standalone sampler observes target pixels with fake capture tools"
assert_file_exists "$standalone_out" "standalone sampler writes bounded pixel observation"
assert_contains "$standalone_out" '"status": "observed"' "standalone sampler records observed status"
assert_contains "$standalone_out" '"sampleCount": 3' "standalone sampler records the three checked sample points"
assert_contains "$standalone_out" '"samplingMethod": "xwd-convert-target-scoped-raw-rgba"' "standalone sampler records the raw RGBA sampling method"
xwd_count=0
convert_count=0
if [ -f "$standalone_dir/xwd.log" ]; then
  xwd_count=$(grep -c '^xwd$' "$standalone_dir/xwd.log")
fi
if [ -f "$standalone_dir/convert.log" ]; then
  convert_count=$(grep -c '^convert ' "$standalone_dir/convert.log")
fi
if [ "$xwd_count" -eq 1 ]; then
  pass "standalone sampler captures the root window once per target"
else
  fail "standalone sampler should capture once per target (got $xwd_count captures)"
fi
if [ "$convert_count" -eq 1 ]; then
  pass "standalone sampler extracts all checked sample points with one convert invocation"
else
  fail "standalone sampler should extract checked pixels with one convert invocation (got $convert_count extracts)"
fi

success_dir="$tmp_root/success"
mkdir -p "$success_dir"
write_runtime_display_artifact "$success_dir" ready
write_controlled_artifact "$success_dir" 1 >"$tmp_root/success-controlled.out" 2>"$tmp_root/success-controlled.err"
status=$?
assert_success "$status" "success fixture creates controlled artifact with one target-ready world-canvas candidate"
success_sampler="$tmp_root/success-sampler"
success_invocations="$tmp_root/success-sampler.invocations"
write_sampler_fixture "$success_sampler" observed "$success_invocations"
run_sampling_evidence "$success_dir" "$success_sampler" >"$tmp_root/success-sampling.out" 2>"$tmp_root/success-sampling.err"
status=$?
assert_success "$status" "target-ready sampling seam invokes sampler and writes bounded observation"
assert_file_exists "$success_dir/$PIXEL_OBSERVATION_ARTIFACT" "target-ready success writes visible-rendering pixel observation artifact"
assert_contains "$success_invocations" '^invoked$' "target-ready success invokes the sampler exactly through the validated seam"

if [ -f "$success_dir/$PIXEL_OBSERVATION_ARTIFACT" ]; then
  python3 - \
    "$success_dir/$PIXEL_OBSERVATION_ARTIFACT" \
    "$CONTROLLED_ARTIFACT" \
    >"$tmp_root/success-observation-contract.out" \
    2>"$tmp_root/success-observation-contract.err" <<'PY'
import json
import math
import sys

payload = json.load(open(sys.argv[1], encoding="utf-8"))
source_artifact = sys.argv[2]
target = payload.get("worldCanvasPixelTarget")
pixel_sampling = payload.get("pixelSampling")
samples = payload.get("samples")
unsupported = payload.get("unsupportedClaims")
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def numeric(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


require(payload.get("schemaVersion") == 1, "observation must use schemaVersion=1")
require(payload.get("status") == "observed", "observation must be observed")
require(payload.get("claimScope") == "visible-rendering-world-canvas-pixel-sampling", "observation must keep pixel-sampling claim scope")
require(payload.get("claimScopeDetail") == "target-scoped-raw-pixel-observation-only", "observation must state raw pixel observation only")
require(payload.get("sourceArtifact") == source_artifact, "observation must cite controlled-display source")
require(payload.get("visibleRenderingCorrectnessEstablished") is False, "observation must explicitly not establish visible rendering correctness")
require(payload.get("renderedWorldPixelsObserved") is True, "observation may only claim raw sampled pixels were observed")
require(payload.get("sampleCount") == 3, "observation must preserve checked sample count")
require(payload.get("samplingMethod") == "fixture-rgba-samples", "observation must preserve sampler method")
require(isinstance(pixel_sampling, dict), "observation must include pixelSampling decision object")
if isinstance(pixel_sampling, dict):
    require(pixel_sampling.get("correctnessCheck") == "not-performed", "pixelSampling must explicitly record correctnessCheck=not-performed")
require(isinstance(target, dict), "observation must preserve validated worldCanvasPixelTarget")
if isinstance(target, dict):
    require(target.get("identified") is True, "observation target must be identified")
    require(target.get("status") == "target-ready", "observation target must preserve target-ready status")
    extents = target.get("screenExtents")
    require(isinstance(extents, dict), "observation target must include screenExtents")
else:
    extents = None
require(isinstance(samples, list), "observation must include sample list")
if isinstance(samples, list) and isinstance(extents, dict):
    require(len(samples) == 3, "observation must include exactly the checked fixture samples")
    for index, sample in enumerate(samples):
        point = sample.get("point") if isinstance(sample, dict) else None
        rgba = sample.get("rgba") if isinstance(sample, dict) else None
        require(sample.get("checked") is True, f"sample {index} must be checked")
        require(isinstance(point, dict), f"sample {index} point must be an object")
        if isinstance(point, dict):
            require(numeric(point.get("x")) and numeric(point.get("y")), f"sample {index} point must be numeric")
            require(extents["x"] <= point["x"] < extents["x"] + extents["width"], f"sample {index} x must be inside target")
            require(extents["y"] <= point["y"] < extents["y"] + extents["height"], f"sample {index} y must be inside target")
        require(isinstance(rgba, list) and len(rgba) == 4, f"sample {index} must include RGBA list")
        if isinstance(rgba, list):
            for channel in rgba:
                require(isinstance(channel, int) and not isinstance(channel, bool) and 0 <= channel <= 255, f"sample {index} RGBA channels must be 0..255 integers")
require(isinstance(unsupported, list), "observation must list unsupportedClaims")
if isinstance(unsupported, list):
    for claim in ("world-canvas-pixel-correctness", "full-visible-rendering-correctness", "rendered-world-correctness"):
        require(claim in unsupported, f"unsupportedClaims must include {claim}")

if errors:
    raise AssertionError("\n".join(errors))
PY
  observation_status=$?
  assert_success "$observation_status" "bounded pixel observation preserves target, raw samples, and non-correctness claim"
  assert_no_forbidden_correctness_claims "$success_dir/$PIXEL_OBSERVATION_ARTIFACT" >"$tmp_root/success-forbidden.out" 2>"$tmp_root/success-forbidden.err"
  status=$?
  assert_success "$status" "successful observation contains no forbidden visible-rendering correctness claim"
fi

unavailable_dir="$tmp_root/sampler-unavailable"
mkdir -p "$unavailable_dir"
write_runtime_display_artifact "$unavailable_dir" ready
write_controlled_artifact "$unavailable_dir" 1 >"$tmp_root/unavailable-controlled.out" 2>"$tmp_root/unavailable-controlled.err"
status=$?
assert_success "$status" "sampler-unavailable fixture creates target-ready controlled artifact"
run_sampling_evidence "$unavailable_dir" "$tmp_root/does-not-exist/world-canvas-pixel-sampler.py" >"$tmp_root/unavailable-sampling.out" 2>"$tmp_root/unavailable-sampling.err"
status=$?
assert_success "$status" "missing sampler path writes fail-closed sampler-unavailable blocker"
assert_file_exists "$unavailable_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "missing sampler writes visible-rendering pixel-sampling blocker"
if [ -f "$unavailable_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" ]; then
  assert_contains "$unavailable_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"blocker": "world-canvas-pixel-sampler-unavailable"' "missing sampler names sampler-unavailable blocker"
  assert_contains "$unavailable_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"renderedWorldPixelsObserved": false' "missing sampler does not claim rendered-world pixels"
  assert_contains "$unavailable_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"visibleRenderingCorrectnessEstablished": false' "missing sampler explicitly does not establish correctness"
fi

for blocked_shape in missing ambiguous invalid; do
  case_dir="$tmp_root/$blocked_shape-target"
  sampler="$tmp_root/$blocked_shape-sampler"
  invocations="$tmp_root/$blocked_shape-sampler.invocations"
  mkdir -p "$case_dir"
  write_runtime_display_artifact "$case_dir" "$blocked_shape"
  case "$blocked_shape" in
    missing) candidate_count=0 ;;
    ambiguous) candidate_count=2 ;;
    invalid) candidate_count=1 ;;
  esac
  write_controlled_artifact "$case_dir" "$candidate_count" >"$tmp_root/$blocked_shape-controlled.out" 2>"$tmp_root/$blocked_shape-controlled.err"
  status=$?
  assert_success "$status" "$blocked_shape target fixture creates controlled source artifact"
  write_sampler_fixture "$sampler" observed "$invocations"
  run_sampling_evidence "$case_dir" "$sampler" >"$tmp_root/$blocked_shape-sampling.out" 2>"$tmp_root/$blocked_shape-sampling.err"
  status=$?
  assert_success "$status" "$blocked_shape target writes fail-closed blocker without invoking sampler"
  assert_file_exists "$case_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "$blocked_shape target writes pixel-sampling blocker"
  if [ -f "$invocations" ]; then
    fail "$blocked_shape target must not invoke sampler before a valid target is identified"
  else
    pass "$blocked_shape target does not invoke sampler before a valid target is identified"
  fi
  if [ -f "$case_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" ]; then
    assert_contains "$case_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"blocker": "world-canvas-pixel-target-not-ready"' "$blocked_shape target records target-not-ready blocker"
    assert_contains "$case_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"renderedWorldPixelsObserved": false' "$blocked_shape target does not claim sampled pixels"
  fi
done

overclaim_dir="$tmp_root/overclaim"
mkdir -p "$overclaim_dir"
write_runtime_display_artifact "$overclaim_dir" ready
write_controlled_artifact "$overclaim_dir" 1 >"$tmp_root/overclaim-controlled.out" 2>"$tmp_root/overclaim-controlled.err"
status=$?
assert_success "$status" "overclaim fixture creates target-ready controlled artifact"
overclaim_sampler="$tmp_root/overclaim-sampler"
overclaim_invocations="$tmp_root/overclaim-sampler.invocations"
write_sampler_fixture "$overclaim_sampler" overclaim "$overclaim_invocations"
run_sampling_evidence "$overclaim_dir" "$overclaim_sampler" >"$tmp_root/overclaim-sampling.out" 2>"$tmp_root/overclaim-sampling.err"
status=$?
assert_success "$status" "sampler output with correctness overclaim is rejected into a blocker artifact"
assert_file_exists "$overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "overclaiming sampler writes blocker instead of observation"
if [ -f "$overclaim_dir/$PIXEL_OBSERVATION_ARTIFACT" ]; then
  fail "overclaiming sampler must not write a success-shaped pixel observation"
else
  pass "overclaiming sampler does not write a success-shaped pixel observation"
fi
if [ -f "$overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" ]; then
  assert_contains "$overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"blocker": "world-canvas-pixel-sampler-overclaimed"' "overclaiming sampler names exact overclaim blocker"
  assert_contains "$overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"visibleRenderingCorrectnessEstablished": false' "overclaim blocker explicitly does not establish correctness"
  assert_no_forbidden_correctness_claims "$overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" >"$tmp_root/overclaim-forbidden.out" 2>"$tmp_root/overclaim-forbidden.err"
  status=$?
  assert_success "$status" "overclaim blocker contains no forbidden visible-rendering correctness phrase"
fi

visual_validation_overclaim_dir="$tmp_root/visual-validation-overclaim"
mkdir -p "$visual_validation_overclaim_dir"
write_runtime_display_artifact "$visual_validation_overclaim_dir" ready
write_controlled_artifact "$visual_validation_overclaim_dir" 1 >"$tmp_root/visual-validation-overclaim-controlled.out" 2>"$tmp_root/visual-validation-overclaim-controlled.err"
status=$?
assert_success "$status" "visual-validation overclaim fixture creates target-ready controlled artifact"
visual_validation_overclaim_sampler="$tmp_root/visual-validation-overclaim-sampler"
visual_validation_overclaim_invocations="$tmp_root/visual-validation-overclaim-sampler.invocations"
write_sampler_fixture "$visual_validation_overclaim_sampler" visual-validation-overclaim "$visual_validation_overclaim_invocations"
run_sampling_evidence "$visual_validation_overclaim_dir" "$visual_validation_overclaim_sampler" >"$tmp_root/visual-validation-overclaim-sampling.out" 2>"$tmp_root/visual-validation-overclaim-sampling.err"
status=$?
assert_success "$status" "sampler output with success-shaped visual validation is rejected into a blocker artifact"
assert_file_exists "$visual_validation_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "visual-validation overclaiming sampler writes blocker instead of observation"
if [ -f "$visual_validation_overclaim_dir/$PIXEL_OBSERVATION_ARTIFACT" ]; then
  fail "visual-validation overclaiming sampler must not write a success-shaped pixel observation"
else
  pass "visual-validation overclaiming sampler does not write a success-shaped pixel observation"
fi
if [ -f "$visual_validation_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" ]; then
  assert_contains "$visual_validation_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"blocker": "world-canvas-pixel-sampler-overclaimed"' "visual-validation overclaiming sampler names exact overclaim blocker"
  assert_contains "$visual_validation_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"visibleRenderingCorrectnessEstablished": false' "visual-validation overclaim blocker explicitly does not establish correctness"
fi

correctness_check_overclaim_dir="$tmp_root/correctness-check-overclaim"
mkdir -p "$correctness_check_overclaim_dir"
write_runtime_display_artifact "$correctness_check_overclaim_dir" ready
write_controlled_artifact "$correctness_check_overclaim_dir" 1 >"$tmp_root/correctness-check-overclaim-controlled.out" 2>"$tmp_root/correctness-check-overclaim-controlled.err"
status=$?
assert_success "$status" "correctness-check overclaim fixture creates target-ready controlled artifact"
correctness_check_overclaim_sampler="$tmp_root/correctness-check-overclaim-sampler"
correctness_check_overclaim_invocations="$tmp_root/correctness-check-overclaim-sampler.invocations"
write_sampler_fixture "$correctness_check_overclaim_sampler" correctness-check-overclaim "$correctness_check_overclaim_invocations"
run_sampling_evidence "$correctness_check_overclaim_dir" "$correctness_check_overclaim_sampler" >"$tmp_root/correctness-check-overclaim-sampling.out" 2>"$tmp_root/correctness-check-overclaim-sampling.err"
status=$?
assert_success "$status" "sampler output with correctnessCheck=performed is rejected into a blocker artifact"
assert_file_exists "$correctness_check_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "correctness-check overclaiming sampler writes blocker instead of observation"
if [ -f "$correctness_check_overclaim_dir/$PIXEL_OBSERVATION_ARTIFACT" ]; then
  fail "correctness-check overclaiming sampler must not write a success-shaped pixel observation"
else
  pass "correctness-check overclaiming sampler does not write a success-shaped pixel observation"
fi
if [ -f "$correctness_check_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" ]; then
  assert_contains "$correctness_check_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"blocker": "world-canvas-pixel-sampler-overclaimed"' "correctness-check overclaiming sampler names exact overclaim blocker"
  assert_contains "$correctness_check_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"visibleRenderingCorrectnessEstablished": false' "correctness-check overclaim blocker explicitly does not establish correctness"
fi

claim_scope_overclaim_dir="$tmp_root/claim-scope-overclaim"
mkdir -p "$claim_scope_overclaim_dir"
write_runtime_display_artifact "$claim_scope_overclaim_dir" ready
write_controlled_artifact "$claim_scope_overclaim_dir" 1 >"$tmp_root/claim-scope-overclaim-controlled.out" 2>"$tmp_root/claim-scope-overclaim-controlled.err"
status=$?
assert_success "$status" "claim-scope overclaim fixture creates target-ready controlled artifact"
claim_scope_overclaim_sampler="$tmp_root/claim-scope-overclaim-sampler"
claim_scope_overclaim_invocations="$tmp_root/claim-scope-overclaim-sampler.invocations"
write_sampler_fixture "$claim_scope_overclaim_sampler" claim-scope-overclaim "$claim_scope_overclaim_invocations"
run_sampling_evidence "$claim_scope_overclaim_dir" "$claim_scope_overclaim_sampler" >"$tmp_root/claim-scope-overclaim-sampling.out" 2>"$tmp_root/claim-scope-overclaim-sampling.err"
status=$?
assert_success "$status" "sampler output with visible-correctness claimScope is rejected into a blocker artifact"
assert_file_exists "$claim_scope_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" "claim-scope overclaiming sampler writes blocker instead of observation"
if [ -f "$claim_scope_overclaim_dir/$PIXEL_OBSERVATION_ARTIFACT" ]; then
  fail "claim-scope overclaiming sampler must not write a success-shaped pixel observation"
else
  pass "claim-scope overclaiming sampler does not write a success-shaped pixel observation"
fi
if [ -f "$claim_scope_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" ]; then
  assert_contains "$claim_scope_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"blocker": "world-canvas-pixel-sampler-overclaimed"' "claim-scope overclaiming sampler names exact overclaim blocker"
  assert_contains "$claim_scope_overclaim_dir/$PIXEL_SAMPLING_BLOCKER_ARTIFACT" '"visibleRenderingCorrectnessEstablished": false' "claim-scope overclaim blocker explicitly does not establish correctness"
fi

finish
