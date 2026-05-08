#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROBE="$BASE_DIR/runners/post-open-runtime-display-probe.py"
SCENARIO_ID=alice-desktop-post-open-runtime-display-accessibility-evidence
ARTIFACT=post-open-runtime-display-accessibility-evidence.json
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

write_alice_inventory() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "windows": [
    {
      "id": "41943050",
      "title": "Alice 3",
      "class": "sun-awt-X11-XFramePeer",
      "pid": 2468,
      "processName": "java",
      "geometry": {"x": 0, "y": 0, "width": 1280, "height": 900, "screen": 0}
    }
  ]
}
JSON
}

write_post_open_observed() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "postOpenWindowObserved": true,
  "mainFrameNames": ["Alice 3"],
  "mainFrameChildCounts": [4],
  "mainWindowObservationBlocker": "none"
}
JSON
}

write_post_open_not_observed() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "blocked",
  "blocker": "no-non-select-project-frame-visible",
  "postOpenWindowObserved": false,
  "mainFrameNames": ["Select Project"],
  "mainFrameChildCounts": [3],
  "mainWindowObservationBlocker": "no-non-select-project-frame-visible"
}
JSON
}

write_no_java_inventory() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "windows": [
    {
      "title": "Some Browser",
      "pid": 9999,
      "processName": "chromium",
      "geometry": {"width": 1280, "height": 900}
    }
  ]
}
JSON
}

run_probe() {
  local inventory=$1
  local post_open=$2
  local output=$3
  local status_file=$4
  shift 4
  python3 "$PROBE" \
    --inventory "$inventory" \
    --post-open-window-observation "$post_open" \
    --output "$output" \
    --status-file "$status_file" \
    --scenario-id "$SCENARIO_ID" \
    --automation-mode xvfb-real-alice \
    "$@"
}

assert_status_file_blocked() {
  local path=$1
  local blocker=$2
  assert_file_exists "$path" "probe writes status.txt for $blocker"
  assert_contains "$path" "^scenario=$SCENARIO_ID$" "status records scenario id for $blocker"
  assert_contains "$path" '^automationMode=xvfb-real-alice$' "status records automation mode for $blocker"
  assert_contains "$path" '^outcome=blocked$' "status records blocked outcome for $blocker"
  assert_contains "$path" "^runtimeDisplayAccessibilityEvidence=$ARTIFACT$" "status points to runtime/display artifact for $blocker"
  assert_contains "$path" '^runtimeDisplayAccessibilityStatus=blocked$' "status records runtime/display blocked status for $blocker"
  assert_contains "$path" "^runtimeDisplayAccessibilityBlocker=$blocker$" "status records exact runtime/display blocker $blocker"
}

write_fake_pyatspi() {
  local fake_dir=$1
  mkdir -p "$fake_dir"
  cat > "$fake_dir/pyatspi.py" <<'PY'
import os

STATE_ENABLED = "enabled"
STATE_SHOWING = "showing"
STATE_VISIBLE = "visible"
DESKTOP_COORDS = "desktop"


class _State:
    def __init__(self, states):
        self._states = set(states)

    def contains(self, state):
        return state in self._states

    def getStates(self):
        return list(self._states)


class _Extents:
    def __init__(self, x, y, width, height):
        self.x = x
        self.y = y
        self.width = width
        self.height = height


class _Component:
    def __init__(self, extents):
        self._extents = extents

    def getExtents(self, coordinate_type):
        return _Extents(**self._extents)


class _Accessible:
    def __init__(self, name="", role="panel", states=None, children=None, pid=None, extents=None):
        self.name = name
        self._role = role
        self._states = states or [STATE_ENABLED, STATE_SHOWING, STATE_VISIBLE]
        self._children = children or []
        self._pid = pid
        self._extents = extents

    @property
    def childCount(self):
        return len(self._children)

    def getChildAtIndex(self, index):
        return self._children[index]

    def get_process_id(self):
        return self._pid

    def getRoleName(self):
        return self._role

    def getState(self):
        return _State(self._states)

    def queryComponent(self):
        if self._extents is None:
            raise RuntimeError("component interface unavailable")
        return _Component(self._extents)


class Registry:
    @staticmethod
    def getDesktop(index):
        pid = int(os.environ.get("FAKE_PYATSPI_PID", "2468"))
        mode = os.environ.get("FAKE_PYATSPI_MODE", "runtime-display")
        if mode == "select-project-only":
            app_children = [
                _Accessible(
                    name="Select Project",
                    role="frame",
                    children=[_Accessible(name="Templates", role="page tab list")],
                )
            ]
        else:
            app_children = [
                _Accessible(
                    name="Alice 3",
                    role="frame",
                    children=[
                        _Accessible(name="Toolbar", role="tool bar"),
                        _Accessible(
                            name="Scene display",
                            role="canvas",
                            extents={"x": 160, "y": 120, "width": 320, "height": 240},
                        ),
                        _Accessible(name="Runtime controls", role="panel"),
                    ],
                )
            ]
        alice_app = _Accessible(name="Alice 3", role="application", children=app_children, pid=pid)
        return _Accessible(name="desktop", role="desktop frame", children=[alice_app])
PY
}

inventory="$tmp_root/inventory.json"
post_open="$tmp_root/post-open-window.json"
write_alice_inventory "$inventory"
write_post_open_observed "$post_open"

# 1. Missing inventory is a blocker, not a silent pass.
missing_out="$tmp_root/missing-inventory/$ARTIFACT"
missing_status="$tmp_root/missing-inventory/status.txt"
mkdir -p "$(dirname "$missing_out")"
run_probe "$tmp_root/no-inventory.json" "$post_open" "$missing_out" "$missing_status" >"$tmp_root/missing-inventory.out" 2>"$tmp_root/missing-inventory.err"
status=$?
assert_success "$status" "probe exits 0 when inventory input is unreadable"
assert_file_exists "$missing_out" "probe writes JSON artifact when inventory is unreadable"
assert_contains "$missing_out" '"status": "blocked"' "missing inventory records blocked status"
assert_contains "$missing_out" '"blocker": "input-unreadable"' "missing inventory names input-unreadable blocker"
assert_contains "$missing_out" '"claim": "post-open-runtime-display-accessibility-evidence"' "missing inventory keeps narrow claim token"
assert_contains "$missing_out" '"postOpenRuntimeDisplayAccessibilityObserved": false' "missing inventory does not claim runtime/display evidence"
assert_contains "$missing_out" '"runtimeDisplayCandidateCount": 0' "missing inventory does not invent candidates"
assert_status_file_blocked "$missing_status" input-unreadable

# 2. Existing post-open setup must be observed before runtime/display accessibility is claimed.
not_open="$tmp_root/not-open.json"
write_post_open_not_observed "$not_open"
not_open_out="$tmp_root/not-open/$ARTIFACT"
not_open_status="$tmp_root/not-open/status.txt"
mkdir -p "$(dirname "$not_open_out")"
run_probe "$inventory" "$not_open" "$not_open_out" "$not_open_status" >"$tmp_root/not-open.out" 2>"$tmp_root/not-open.err"
status=$?
assert_success "$status" "probe exits 0 when post-open setup was not observed"
assert_contains "$not_open_out" '"status": "blocked"' "post-open-not-observed records blocked status"
assert_contains "$not_open_out" '"blocker": "post-open-window-not-observed"' "post-open-not-observed names exact blocker"
assert_contains "$not_open_out" '"postOpenRuntimeDisplayAccessibilityObserved": false' "post-open-not-observed does not claim runtime/display evidence"
assert_status_file_blocked "$not_open_status" post-open-window-not-observed

# 3. A Java/Alice PID is required before AT-SPI runtime/display probing.
no_java_inventory="$tmp_root/no-java-inventory.json"
write_no_java_inventory "$no_java_inventory"
no_java_out="$tmp_root/no-java/$ARTIFACT"
no_java_status="$tmp_root/no-java/status.txt"
mkdir -p "$(dirname "$no_java_out")"
run_probe "$no_java_inventory" "$post_open" "$no_java_out" "$no_java_status" >"$tmp_root/no-java.out" 2>"$tmp_root/no-java.err"
status=$?
assert_success "$status" "probe exits 0 when Java PID is absent from inventory"
assert_contains "$no_java_out" '"status": "blocked"' "no-java records blocked status"
assert_contains "$no_java_out" '"blocker": "java-pid-not-in-inventory"' "no-java names exact blocker"
assert_contains "$no_java_out" '"postOpenRuntimeDisplayAccessibilityObserved": false' "no-java does not claim runtime/display evidence"
assert_status_file_blocked "$no_java_status" java-pid-not-in-inventory

# 4. A visible AT-SPI runtime/display candidate proves only the narrow accessibility evidence.
fake_pyatspi_dir="$tmp_root/fake-pyatspi"
write_fake_pyatspi "$fake_pyatspi_dir"
observed_out="$tmp_root/observed/$ARTIFACT"
observed_status="$tmp_root/observed/status.txt"
mkdir -p "$(dirname "$observed_out")"
PYTHONPATH="$fake_pyatspi_dir${PYTHONPATH:+:$PYTHONPATH}" FAKE_PYATSPI_MODE=runtime-display \
  run_probe "$inventory" "$post_open" "$observed_out" "$observed_status" >"$tmp_root/observed.out" 2>"$tmp_root/observed.err"
status=$?
assert_success "$status" "probe exits 0 when runtime/display accessibility evidence is observed"
assert_contains "$observed_out" '"status": "observed"' "observed artifact records observed status"
assert_contains "$observed_out" '"blocker": "none"' "observed artifact has no blocker"
assert_contains "$observed_out" '"claim": "post-open-runtime-display-accessibility-evidence"' "observed artifact uses narrow claim token"
assert_contains "$observed_out" '"postOpenRuntimeDisplayAccessibilityObserved": true' "observed artifact records runtime/display evidence"
assert_contains "$observed_out" '"runtimeDisplayCandidateCount": 1' "observed artifact counts one runtime/display candidate"
assert_contains "$observed_out" '"name": "Scene display"' "observed artifact names the accessible display candidate"
assert_contains "$observed_out" '"role": "canvas"' "observed artifact records the display candidate role"
assert_contains "$observed_out" '"geometryStatus": "available"' "observed artifact records available candidate geometry"
assert_contains "$observed_out" '"coordinateType": "screen"' "observed artifact records screen-coordinate candidate extents"
assert_contains "$observed_out" '"width": 320' "observed artifact records positive candidate extent width"
assert_contains "$observed_out" '"height": 240' "observed artifact records positive candidate extent height"
assert_not_contains "$observed_out" 'rendering correctness|world execution|lesson completion|grading|installer success' "observed artifact avoids overclaiming"
assert_file_exists "$observed_status" "observed probe writes status.txt"
assert_contains "$observed_status" '^outcome=passed$' "observed status records passed outcome"
assert_contains "$observed_status" '^runtimeDisplayAccessibilityStatus=observed$' "observed status records runtime/display observed status"
assert_contains "$observed_status" '^runtimeDisplayAccessibilityBlocker=none$' "observed status records no blocker"

# 5. Select Project alone is not runtime/display evidence.
select_only_out="$tmp_root/select-only/$ARTIFACT"
select_only_status="$tmp_root/select-only/status.txt"
mkdir -p "$(dirname "$select_only_out")"
PYTHONPATH="$fake_pyatspi_dir${PYTHONPATH:+:$PYTHONPATH}" FAKE_PYATSPI_MODE=select-project-only \
  run_probe "$inventory" "$post_open" "$select_only_out" "$select_only_status" >"$tmp_root/select-only.out" 2>"$tmp_root/select-only.err"
status=$?
assert_success "$status" "probe exits 0 when only Select Project is accessible"
assert_contains "$select_only_out" '"status": "blocked"' "select-only records blocked status"
assert_contains "$select_only_out" '"blocker": "runtime-display-accessible-candidate-not-found"' "select-only names missing runtime/display candidate blocker"
assert_contains "$select_only_out" '"postOpenRuntimeDisplayAccessibilityObserved": false' "select-only does not claim runtime/display evidence"
assert_contains "$select_only_out" '"runtimeDisplayCandidateCount": 0' "select-only does not count Select Project as runtime/display evidence"
assert_status_file_blocked "$select_only_status" runtime-display-accessible-candidate-not-found

for out_file in "$missing_out" "$not_open_out" "$no_java_out" "$observed_out" "$select_only_out"; do
  python3 - "$out_file" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as stream:
    json.load(stream)
PY
  assert_success "$?" "probe output is valid JSON: $(basename "$(dirname "$out_file")")"
done

finish
