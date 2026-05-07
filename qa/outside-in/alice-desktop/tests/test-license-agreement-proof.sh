#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-license-agreement-proof.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PREP="$BASE_DIR/runners/prepare-license-acceptance.py"
PROBE="$BASE_DIR/runners/license-dialog-probe.py"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

prefs_root="$tmp_root/java-user-prefs"
acceptance_output="$tmp_root/license-acceptance.json"
python3 "$PREP" --user-root "$prefs_root" --output "$acceptance_output" --accept-for-tests
status=$?
assert_success "$status" "license acceptance prep writes explicit test-only Java preferences"
assert_file_exists "$prefs_root/.java/.userPrefs/org/lgna/project/prefs.xml" "Alice license preference file is written"
assert_file_exists "$prefs_root/.java/.userPrefs/edu/cmu/cs/dennisc/nebulous/prefs.xml" "Sims license preference file is written"
assert_contains "$prefs_root/.java/.userPrefs/org/lgna/project/prefs.xml" '<entry key="isLicenseAccepted" value="true"' "Alice license key is accepted"
assert_contains "$prefs_root/.java/.userPrefs/edu/cmu/cs/dennisc/nebulous/prefs.xml" '<entry key="isLicenseAccepted" value="true"' "Sims license key is accepted"
assert_contains "$acceptance_output" '"status": "prepared"' "acceptance artifact records prepared status"
assert_contains "$acceptance_output" '"testOnly": true' "acceptance artifact marks the bypass as test-only"
assert_contains "$acceptance_output" '"java.util.prefs.userRoot": ".*/java-user-prefs"' "acceptance artifact records JVM preference root property"
assert_contains "$acceptance_output" '"title": "License Agreement \(Part 1 of 2\): Alice 3"' "acceptance artifact records Alice dialog title"
assert_contains "$acceptance_output" '"title": "License Agreement \(Part 2 of 2\): The Sims \(TM\) 2 Art Assets"' "acceptance artifact records Sims dialog title"

disabled_output="$tmp_root/license-acceptance-disabled.json"
python3 "$PREP" --user-root "$tmp_root/disabled-prefs" --output "$disabled_output"
status=$?
assert_exit_code "$status" 2 "license acceptance prep refuses to write without explicit test opt-in"
assert_contains "$disabled_output" '"status": "blocked"' "disabled artifact records blocked status"
assert_contains "$disabled_output" '"blocker": "license-test-acceptance-not-enabled"' "disabled artifact names exact opt-in blocker"
if [ -e "$tmp_root/disabled-prefs/.java/.userPrefs/org/lgna/project/prefs.xml" ]; then
  fail "disabled prep must not create Alice license preference state"
else
  pass "disabled prep does not create Alice license preference state"
fi

inventory="$tmp_root/x-window-inventory.json"
dialog_output="$tmp_root/license-dialog.json"
cat > "$inventory" <<'JSON'
{
  "windows": [
    {
      "title": "License Agreement (Part 1 of 2): Alice 3",
      "class": "sun-awt-X11-XDialogPeer",
      "pid": 2468,
      "processName": "java",
      "geometry": {"x": 330, "y": 180, "width": 620, "height": 500, "screen": 0}
    }
  ]
}
JSON
python3 "$PROBE" "$inventory" "$dialog_output"
status=$?
assert_success "$status" "license dialog probe classifies observed first-run license dialog"
assert_contains "$dialog_output" '"status": "observed"' "dialog artifact records observed status"
assert_contains "$dialog_output" '"blocker": "first-run-license-agreement-visible"' "dialog artifact names first-run license blocker"
assert_contains "$dialog_output" '"dialogTitle": "License Agreement \(Part 1 of 2\): Alice 3"' "dialog artifact records exact title"
assert_contains "$dialog_output" 'Please read the following license agreement carefully\.' "dialog artifact records expected header text"
assert_contains "$dialog_output" 'I accept the terms in the License Agreement' "dialog artifact records accept checkbox text"
assert_contains "$dialog_output" 'I do not accept the terms in the License Agreement' "dialog artifact records reject checkbox text"
assert_contains "$dialog_output" '"OK"' "dialog artifact records OK button"
assert_contains "$dialog_output" '"Cancel"' "dialog artifact records Cancel button"
assert_contains "$dialog_output" '\.java/\.userPrefs/org/lgna/project/prefs\.xml' "dialog artifact names Alice preference file"

negative_inventory="$tmp_root/x-window-inventory-negative.json"
negative_output="$tmp_root/license-dialog-negative.json"
cat > "$negative_inventory" <<'JSON'
{
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
assert_success "$status" "license dialog probe plainly reports unsupported negative case"
assert_contains "$negative_output" '"status": "not-observed"' "negative dialog case does not pretend a license dialog"
assert_contains "$negative_output" '"blocker": "license-agreement-window-not-found"' "negative dialog case names absent license dialog"
assert_contains "$negative_output" '"expectedControls": \[\]' "negative dialog case does not invent controls"

finish
