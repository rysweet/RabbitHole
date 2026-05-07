#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-select-project-proof.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROBE="$BASE_DIR/runners/select-project-probe.py"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

inventory="$tmp_root/x-window-inventory.json"
output="$tmp_root/select-project-window.json"
cat > "$inventory" <<'JSON'
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
    },
    {
      "id": "41943069",
      "title": "Select Project",
      "class": "sun-awt-X11-XDialogPeer",
      "pid": 2468,
      "processName": "java",
      "geometry": {"x": 220, "y": 80, "width": 840, "height": 700, "screen": 0}
    }
  ]
}
JSON
python3 "$PROBE" "$inventory" "$output"
status=$?
assert_success "$status" "select project probe classifies exact Select Project window inventory"
assert_contains "$output" '"status": "observed"' "select project artifact records observed status"
assert_contains "$output" '"blocker": "none"' "select project artifact has no blocker when exact window is present"
assert_contains "$output" '"interactionProof": "select-project-window-visible"' "select project artifact names the proven interaction step"
assert_contains "$output" '"projectWorldInteraction": "not-observed"' "select project artifact does not claim opened project/world interaction"
assert_contains "$output" '"title": "Select Project"' "select project artifact records exact title"
assert_contains "$output" '"class": "sun-awt-X11-XDialogPeer"' "select project artifact records exact class"
assert_contains "$output" '"width": 840' "select project artifact records geometry"
assert_contains "$output" '"Blank Slates"' "select project artifact records resource-backed Blank Slates tab label"
assert_contains "$output" '"Starters"' "select project artifact records resource-backed Starters tab label"
assert_contains "$output" '"My Projects"' "select project artifact records resource-backed My Projects tab label"
assert_contains "$output" '"Recent"' "select project artifact records resource-backed Recent tab label"
assert_contains "$output" '"File System"' "select project artifact records resource-backed File System tab label"
assert_contains "$output" '"sidekickLabels"' "select project artifact separates sidekick labels from tab order"
assert_contains "$output" '"Open for VR"' "select project artifact records resource-backed VR sidekick label"
assert_contains "$output" '"widgetObservationStatus": "resource-contract-only"' "select project artifact is honest about widget evidence limits"
assert_contains "$output" '"widgetObservationBlocker": "swing-widget-inventory-not-collected"' "select project artifact names missing widget introspection blocker"

negative_inventory="$tmp_root/x-window-inventory-negative.json"
negative_output="$tmp_root/select-project-window-negative.json"
cat > "$negative_inventory" <<'JSON'
{
  "status": "observed",
  "windows": [
    {
      "title": "Alice 3",
      "pid": 1357,
      "processName": "java",
      "geometry": {"width": 800, "height": 600}
    }
  ]
}
JSON
python3 "$PROBE" "$negative_inventory" "$negative_output"
status=$?
assert_success "$status" "select project probe plainly reports unsupported negative case"
assert_contains "$negative_output" '"status": "not-observed"' "negative case does not pretend Select Project interaction"
assert_contains "$negative_output" '"blocker": "select-project-window-not-found"' "negative case names absent Select Project window"
assert_contains "$negative_output" '"interactionProof": "not-proven"' "negative case does not prove interaction"
assert_contains "$negative_output" '"expectedWidgetLabels": \[\]' "negative case does not invent widget labels"
assert_contains "$negative_output" '"projectWorldInteraction": "not-observed"' "negative case does not claim project/world interaction"

non_java_inventory="$tmp_root/x-window-inventory-non-java.json"
non_java_output="$tmp_root/select-project-window-non-java.json"
cat > "$non_java_inventory" <<'JSON'
{
  "status": "observed",
  "windows": [
    {
      "title": "Select Project",
      "pid": 9753,
      "processName": "chromium",
      "geometry": {"width": 640, "height": 480}
    }
  ]
}
JSON
python3 "$PROBE" "$non_java_inventory" "$non_java_output"
status=$?
assert_success "$status" "select project probe rejects non-Java Select Project windows"
assert_contains "$non_java_output" '"status": "not-observed"' "non-Java Select Project window does not prove Alice interaction"
assert_contains "$non_java_output" '"blocker": "select-project-window-not-java"' "non-Java Select Project case names exact blocker"
assert_contains "$non_java_output" '"interactionProof": "not-proven"' "non-Java Select Project case does not prove interaction"
assert_contains "$non_java_output" '"expectedWidgetLabels": \[\]' "non-Java Select Project case does not invent widget labels"
assert_contains "$non_java_output" '"widgetObservationBlocker": "select-project-window-not-java"' "non-Java Select Project widget blocker matches top-level blocker"

bad_output="$tmp_root/select-project-window-bad.json"
python3 "$PROBE" "$tmp_root/missing-inventory.json" "$bad_output"
status=$?
assert_success "$status" "select project probe writes explicit blocked artifact for unreadable inventory"
assert_contains "$bad_output" '"status": "blocked"' "unreadable inventory records blocked status"
assert_contains "$bad_output" '"blocker": "window-inventory-unreadable"' "unreadable inventory names exact blocker"

finish
