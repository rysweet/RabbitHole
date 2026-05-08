#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROBE="$BASE_DIR/runners/post-project-open-probe.py"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

# --- Helper: shared valid inventory with Alice 3 main window ---
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

# --- Helper: tab-click observation with projectOpenObserved=true ---
write_tab_click_opened() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame is no longer present in the AT-SPI tree; project opening is observed.",
  "toggleTabNodeCount": 5
}
JSON
}

# --- Helper: tab-click observation with projectOpenObserved=false ---
write_tab_click_not_opened() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "ok-button-not-clicked",
  "projectOpenObserved": false,
  "projectOpenDetail": "OK button click did not succeed."
}
JSON
}

# --- Helper: inventory with a non-Alice Java process/window only ---
write_non_alice_java_inventory() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "windows": [
    {
      "id": "41943051",
      "title": "Maven Test Harness",
      "class": "sun-awt-X11-XFramePeer",
      "pid": 1357,
      "processName": "java",
      "geometry": {"x": 20, "y": 20, "width": 800, "height": 600, "screen": 0}
    }
  ]
}
JSON
}

# --- Helper: target-starter selection without target-specific open proof ---
write_target_selected_not_opened() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "target-starter-open-not-observed",
  "projectOpenObserved": true,
  "projectOpenDetail": "Generic Select Project dismissal was observed, but Africa Full was not proven opened.",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel"
  },
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": true,
  "openedStarter": null,
  "evidenceStatus": "selected",
  "nextBlocker": {
    "observedAtspiState": "Africa Full selection evidence exists, but openedStarter is not Africa Full.",
    "actionAttempted": "Click OK/Open after selecting Africa Full.",
    "expectedNextAction": "Observe projectOpenObserved=true with openedStarter set to Africa Full.",
    "reasonProgressStopped": "The generic main-window transition is insufficient target-specific proof."
  }
}
JSON
}

# --- Helper: target-starter opening proof from tab-click probe ---
write_target_opened() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame is no longer present in the AT-SPI tree; target starter project opening is observed.",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel"
  },
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": true,
  "openedStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "evidenceStatus": "opened",
  "nextBlocker": null
}
JSON
}

write_target_opened_without_observed() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame is no longer present in the AT-SPI tree; target starter project opening is observed.",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": true,
  "openedStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "evidenceStatus": "opened",
  "nextBlocker": null
}
JSON
}

write_target_opened_without_selection_flag() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame is no longer present in the AT-SPI tree; target starter project opening is observed.",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel"
  },
  "targetStarterOpenAttempted": true,
  "openedStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "evidenceStatus": "opened",
  "nextBlocker": null
}
JSON
}

write_target_opened_without_open_attempt() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame is no longer present in the AT-SPI tree; target starter project opening is observed.",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel"
  },
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": false,
  "openedStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "evidenceStatus": "opened",
  "nextBlocker": null
}
JSON
}

write_wrong_target_opened() {
  local path=$1
  cat > "$path" <<'JSON'
{
  "status": "observed",
  "blocker": "none",
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame is no longer present in the AT-SPI tree; target starter project opening is observed.",
  "targetStarter": {
    "displayName": "Wonderland",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/Wonderland.a3p"
  },
  "targetStarterObserved": {
    "name": "Wonderland",
    "role": "panel"
  },
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": true,
  "openedStarter": {
    "displayName": "Wonderland",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/Wonderland.a3p"
  },
  "evidenceStatus": "opened",
  "nextBlocker": null
}
JSON
}

# ---- 1. Missing inventory → blocked ----
missing_out="$tmp_root/missing-inventory-out.json"
python3 "$PROBE" "$tmp_root/no-inventory.json" "$tmp_root/no-tab-click.json" "$missing_out"
status=$?
assert_success "$status" "probe exits 0 when inventory file is missing"
assert_contains "$missing_out" '"status": "blocked"' "missing-inventory records blocked status"
assert_contains "$missing_out" '"blocker": "input-unreadable"' "missing-inventory names input-unreadable blocker"
assert_contains "$missing_out" '"postOpenWindowObserved": false' "missing-inventory does not claim post-open observed"
assert_contains "$missing_out" '"mainWindowObservationBlocker": "input-unreadable"' "missing-inventory names exact mainWindowObservationBlocker"

# ---- 2. Missing tab-click observation → blocked ----
inventory2="$tmp_root/inventory2.json"
write_alice_inventory "$inventory2"
missing_tab_out="$tmp_root/missing-tab-click-out.json"
python3 "$PROBE" "$inventory2" "$tmp_root/no-tab-click.json" "$missing_tab_out"
status=$?
assert_success "$status" "probe exits 0 when tab-click observation is missing"
assert_contains "$missing_tab_out" '"status": "blocked"' "missing-tab-click records blocked status"
assert_contains "$missing_tab_out" '"blocker": "input-unreadable"' "missing-tab-click names input-unreadable blocker"
assert_contains "$missing_tab_out" '"postOpenWindowObserved": false' "missing-tab-click does not claim post-open observed"

# ---- 3. Malformed inventory JSON → blocked ----
malformed_inv="$tmp_root/malformed-inv.json"
printf 'not-json\n' > "$malformed_inv"
malformed_inv_out="$tmp_root/malformed-inv-out.json"
python3 "$PROBE" "$malformed_inv" "$tmp_root/no-tab-click.json" "$malformed_inv_out"
status=$?
assert_success "$status" "probe exits 0 for malformed inventory JSON"
assert_contains "$malformed_inv_out" '"status": "blocked"' "malformed-inventory records blocked status"
assert_contains "$malformed_inv_out" '"blocker": "input-unreadable"' "malformed-inventory names input-unreadable blocker"

# ---- 4. Malformed tab-click JSON → blocked ----
inventory4="$tmp_root/inventory4.json"
write_alice_inventory "$inventory4"
malformed_tab="$tmp_root/malformed-tab.json"
printf 'not-json\n' > "$malformed_tab"
malformed_tab_out="$tmp_root/malformed-tab-out.json"
python3 "$PROBE" "$inventory4" "$malformed_tab" "$malformed_tab_out"
status=$?
assert_success "$status" "probe exits 0 for malformed tab-click JSON"
assert_contains "$malformed_tab_out" '"status": "blocked"' "malformed-tab-click records blocked status"
assert_contains "$malformed_tab_out" '"blocker": "input-unreadable"' "malformed-tab-click names input-unreadable blocker"

# ---- 5. projectOpenObserved=false → blocked with project-not-opened ----
inventory5="$tmp_root/inventory5.json"
write_alice_inventory "$inventory5"
not_opened_tab="$tmp_root/not-opened-tab.json"
write_tab_click_not_opened "$not_opened_tab"
not_opened_out="$tmp_root/not-opened-out.json"
python3 "$PROBE" "$inventory5" "$not_opened_tab" "$not_opened_out"
status=$?
assert_success "$status" "probe exits 0 when project was not opened"
assert_contains "$not_opened_out" '"status": "blocked"' "project-not-opened records blocked status"
assert_contains "$not_opened_out" '"blocker": "project-not-opened"' "project-not-opened names exact blocker"
assert_contains "$not_opened_out" '"postOpenWindowObserved": false' "project-not-opened does not claim observation"
assert_contains "$not_opened_out" '"mainWindowObservationBlocker": "project-not-opened"' "project-not-opened names exact mainWindowObservationBlocker"
assert_contains "$not_opened_out" '"mainFrameNames": \[\]' "project-not-opened records empty mainFrameNames"
assert_contains "$not_opened_out" '"mainFrameChildCounts": \[\]' "project-not-opened records empty mainFrameChildCounts"

# ---- 6. No Alice 3 Java window in inventory → blocked with alice-window-java-pid-not-identified ----
no_java_inv="$tmp_root/no-java-inv.json"
cat > "$no_java_inv" <<'JSON'
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
opened_tab6="$tmp_root/opened-tab6.json"
write_tab_click_opened "$opened_tab6"
no_java_out="$tmp_root/no-java-out.json"
python3 "$PROBE" "$no_java_inv" "$opened_tab6" "$no_java_out"
status=$?
assert_success "$status" "probe exits 0 when no Java window is in inventory"
assert_contains "$no_java_out" '"status": "blocked"' "no-java-pid records blocked status"
assert_contains "$no_java_out" '"blocker": "alice-window-java-pid-not-identified"' "no-java-pid names exact blocker"
assert_contains "$no_java_out" 'Unable to identify the Java process for the Alice 3 main window' "no-java-pid explains missing Alice 3 window Java process"
assert_contains "$no_java_out" 'Refusing to introspect an arbitrary Java process' "no-java-pid refuses arbitrary Java introspection"
assert_contains "$no_java_out" '"postOpenWindowObserved": false' "no-java-pid does not claim post-open observed"
assert_contains "$no_java_out" '"mainWindowObservationBlocker": "alice-window-java-pid-not-identified"' "no-java-pid names exact mainWindowObservationBlocker"

# ---- 7. Non-Alice Java process is rejected instead of introspected ----
non_alice_java_inv="$tmp_root/non-alice-java-inv.json"
write_non_alice_java_inventory "$non_alice_java_inv"
opened_tab7="$tmp_root/opened-tab7.json"
write_tab_click_opened "$opened_tab7"
non_alice_java_out="$tmp_root/non-alice-java-out.json"
python3 "$PROBE" "$non_alice_java_inv" "$opened_tab7" "$non_alice_java_out"
status=$?
assert_success "$status" "probe exits 0 when only a non-Alice Java process is in inventory"
assert_contains "$non_alice_java_out" '"status": "blocked"' "non-Alice Java process records blocked status"
assert_contains "$non_alice_java_out" '"blocker": "alice-window-java-pid-not-identified"' "non-Alice Java process names exact blocker"
assert_contains "$non_alice_java_out" '"javaPid": null' "non-Alice Java process is not selected for introspection"
assert_contains "$non_alice_java_out" 'Unable to identify the Java process for the Alice 3 main window' "non-Alice Java blocker explains missing Alice 3 window Java process"
assert_contains "$non_alice_java_out" 'Refusing to introspect an arbitrary Java process' "non-Alice Java blocker refuses arbitrary Java introspection"
assert_contains "$non_alice_java_out" '"postOpenWindowObserved": false' "non-Alice Java process does not claim post-open observed"
assert_contains "$non_alice_java_out" '"mainWindowObservationBlocker": "alice-window-java-pid-not-identified"' "non-Alice Java process names exact mainWindowObservationBlocker"
assert_not_contains "$non_alice_java_out" '"javaPid": 1357' "non-Alice Java PID is not recorded as selected"

# ---- 8. pyatspi not installed → blocked with pyatspi-not-installed ----
# The probe falls through to probe_post_open when project is open and PID is found.
# Without a live AT-SPI session, pyatspi import fails on most test machines.
# We assert the probe exits 0 and records either pyatspi-not-installed or
# at-spi-registry-unavailable (both are legitimate blocked outcomes in CI).
inventory8="$tmp_root/inventory8.json"
write_alice_inventory "$inventory8"
opened_tab8="$tmp_root/opened-tab8.json"
write_tab_click_opened "$opened_tab8"
atk_out="$tmp_root/atk-out.json"
python3 "$PROBE" "$inventory8" "$opened_tab8" "$atk_out"
status=$?
assert_success "$status" "probe exits 0 when AT-SPI is not available in test environment"
assert_contains "$atk_out" '"status": "blocked"' "no-AT-SPI records blocked status"
assert_contains "$atk_out" '"postOpenWindowObserved": false' "no-AT-SPI does not claim post-open observed"
# The blocker is either pyatspi-not-installed or at-spi-registry-unavailable or atk-wrapper-not-loaded.
# Use a broad regex to capture all three legitimate blockers.
assert_contains "$atk_out" '"blocker": "(pyatspi-not-installed|at-spi-registry-unavailable|atk-wrapper-not-loaded)"' \
  "no-AT-SPI names a precise AT-SPI-related blocker"

# ---- 9. Generic main-window observation does not imply Africa Full proof ----
inventory9="$tmp_root/inventory9.json"
write_alice_inventory "$inventory9"
target_selected_tab="$tmp_root/target-selected-tab.json"
write_target_selected_not_opened "$target_selected_tab"
target_selected_out="$tmp_root/target-selected-out.json"
python3 "$PROBE" "$inventory9" "$target_selected_tab" "$target_selected_out"
status=$?
assert_success "$status" "probe exits 0 when Africa Full target evidence is selected but not opened"
assert_contains "$target_selected_out" '"status": "blocked"' "target-selected-not-opened records blocked status"
assert_contains "$target_selected_out" '"blocker": "target-starter-open-not-proven"' "target-selected-not-opened refuses generic main-window proof"
assert_contains "$target_selected_out" '"postOpenWindowObserved": false' "target-selected-not-opened does not claim post-open window observation"
assert_contains "$target_selected_out" '"mainWindowObservationBlocker": "target-starter-open-not-proven"' "target-selected-not-opened names exact mainWindowObservationBlocker"

# ---- 10. Opened status without target observation proof is not enough ----
inventory10c="$tmp_root/inventory10c.json"
write_alice_inventory "$inventory10c"
missing_observed_tab="$tmp_root/missing-observed-tab.json"
write_target_opened_without_observed "$missing_observed_tab"
missing_observed_out="$tmp_root/missing-observed-out.json"
python3 "$PROBE" "$inventory10c" "$missing_observed_tab" "$missing_observed_out"
status=$?
assert_success "$status" "probe exits 0 when opened evidence omits targetStarterObserved"
assert_contains "$missing_observed_out" '"status": "blocked"' "missing targetStarterObserved records blocked status"
assert_contains "$missing_observed_out" '"blocker": "target-starter-open-not-proven"' "missing targetStarterObserved refuses post-open proof"
assert_contains "$missing_observed_out" '"targetStarterSelected": true' "missing targetStarterObserved preserves target selection proof"
assert_contains "$missing_observed_out" '"targetStarterOpenAttempted": true' "missing targetStarterObserved preserves target open-attempt proof"
assert_contains "$missing_observed_out" '"postOpenWindowObserved": false' "missing targetStarterObserved does not claim post-open observation"

# ---- 11. Opened status without target selection proof is not enough ----
inventory10a="$tmp_root/inventory10a.json"
write_alice_inventory "$inventory10a"
missing_selection_tab="$tmp_root/missing-selection-tab.json"
write_target_opened_without_selection_flag "$missing_selection_tab"
missing_selection_out="$tmp_root/missing-selection-out.json"
python3 "$PROBE" "$inventory10a" "$missing_selection_tab" "$missing_selection_out"
status=$?
assert_success "$status" "probe exits 0 when opened evidence omits targetStarterSelected"
assert_contains "$missing_selection_out" '"status": "blocked"' "missing targetStarterSelected records blocked status"
assert_contains "$missing_selection_out" '"blocker": "target-starter-open-not-proven"' "missing targetStarterSelected refuses post-open proof"
assert_contains "$missing_selection_out" '"targetStarterOpenAttempted": true' "missing targetStarterSelected preserves the open-attempt flag"
assert_contains "$missing_selection_out" '"postOpenWindowObserved": false' "missing targetStarterSelected does not claim post-open observation"

# ---- 12. Opened status without target open-attempt proof is not enough ----
inventory10b="$tmp_root/inventory10b.json"
write_alice_inventory "$inventory10b"
missing_open_attempt_tab="$tmp_root/missing-open-attempt-tab.json"
write_target_opened_without_open_attempt "$missing_open_attempt_tab"
missing_open_attempt_out="$tmp_root/missing-open-attempt-out.json"
python3 "$PROBE" "$inventory10b" "$missing_open_attempt_tab" "$missing_open_attempt_out"
status=$?
assert_success "$status" "probe exits 0 when opened evidence has targetStarterOpenAttempted=false"
assert_contains "$missing_open_attempt_out" '"status": "blocked"' "false targetStarterOpenAttempted records blocked status"
assert_contains "$missing_open_attempt_out" '"blocker": "target-starter-open-not-proven"' "false targetStarterOpenAttempted refuses post-open proof"
assert_contains "$missing_open_attempt_out" '"targetStarterSelected": true' "false targetStarterOpenAttempted preserves the selection flag"
assert_contains "$missing_open_attempt_out" '"targetStarterOpenAttempted": false' "false targetStarterOpenAttempted is preserved"
assert_contains "$missing_open_attempt_out" '"postOpenWindowObserved": false' "false targetStarterOpenAttempted does not claim post-open observation"

# ---- 13. Target opened evidence survives post-open AT-SPI blockers ----
inventory10="$tmp_root/inventory10.json"
write_alice_inventory "$inventory10"
target_opened_tab="$tmp_root/target-opened-tab.json"
write_target_opened "$target_opened_tab"
target_opened_out="$tmp_root/target-opened-out.json"
python3 "$PROBE" "$inventory10" "$target_opened_tab" "$target_opened_out"
status=$?
assert_success "$status" "probe exits 0 after target-specific opened Select Project evidence"
assert_contains "$target_opened_out" '"targetStarter": \{' "target-opened post-open evidence preserves targetStarter"
assert_contains "$target_opened_out" '"displayName": "Africa Full"' "target-opened post-open evidence preserves target display name"
assert_contains "$target_opened_out" '"repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull\.a3p"' "target-opened post-open evidence preserves target repository path"
assert_contains "$target_opened_out" '"openedStarter": \{' "target-opened post-open evidence preserves openedStarter"
assert_contains "$target_opened_out" '"evidenceStatus": "opened"' "target-opened post-open evidence preserves opened evidenceStatus"
assert_contains "$target_opened_out" '"targetStarterObserved": \{' "target-opened post-open evidence preserves target observation proof"
assert_contains "$target_opened_out" '"targetStarterSelected": true' "target-opened post-open evidence preserves target selection proof"
assert_contains "$target_opened_out" '"targetStarterOpenAttempted": true' "target-opened post-open evidence preserves target open-attempt proof"
assert_contains "$target_opened_out" '"targetProjectOpenObserved": true' "target-opened post-open evidence preserves the Select Project project-open observation"
assert_contains "$target_opened_out" '"javaPid": 2468' "target-opened post-open evidence preserves the Alice Java/window PID"

# ---- 14. Wrong target metadata is not accepted as Africa Full proof ----
inventory11="$tmp_root/inventory11.json"
write_alice_inventory "$inventory11"
wrong_target_tab="$tmp_root/wrong-target-tab.json"
write_wrong_target_opened "$wrong_target_tab"
wrong_target_out="$tmp_root/wrong-target-out.json"
python3 "$PROBE" "$inventory11" "$wrong_target_tab" "$wrong_target_out"
status=$?
assert_success "$status" "probe exits 0 when opened evidence names the wrong target"
assert_contains "$wrong_target_out" '"status": "blocked"' "wrong target metadata records blocked status"
assert_contains "$wrong_target_out" '"blocker": "target-starter-metadata-invalid"' "wrong target metadata names exact blocker"
assert_contains "$wrong_target_out" '"postOpenWindowObserved": false' "wrong target metadata does not claim post-open observation"
assert_contains "$wrong_target_out" '"displayName": "Africa Full"' "wrong target metadata records the expected Africa Full target"

# ---- 15. Probe output is valid JSON ----
for out_file in "$missing_out" "$missing_tab_out" "$malformed_inv_out" "$malformed_tab_out" \
                "$not_opened_out" "$no_java_out" "$non_alice_java_out" "$atk_out" \
                "$target_selected_out" "$missing_observed_out" "$missing_selection_out" \
                "$missing_open_attempt_out" "$target_opened_out" "$wrong_target_out"; do
  python3 - "$out_file" <<'PY'
import json, sys
try:
    json.load(open(sys.argv[1]))
except Exception as e:
    print(f"invalid JSON in {sys.argv[1]}: {e}", file=sys.stderr)
    sys.exit(1)
PY
  assert_success "$?" "probe output is valid JSON: $(basename "$out_file")"
done

finish
