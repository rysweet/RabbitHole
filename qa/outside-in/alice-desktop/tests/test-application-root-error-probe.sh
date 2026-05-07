#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-application-root-error-probe.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROBE="$BASE_DIR/runners/application-root-error-probe.py"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

inventory="$tmp_root/x-window-inventory.json"
output="$tmp_root/application-root-error.json"
proc_root="$tmp_root/proc"

cat > "$inventory" <<'JSON'
{
  "windows": [
    {
      "title": "Application Root Error",
      "class": "sun-awt-X11-XDialogPeer",
      "pid": 4321,
      "processName": "java",
      "geometry": {"x": 395, "y": 390, "width": 490, "height": 119, "screen": 0}
    }
  ]
}
JSON
mkdir -p "$proc_root/4321"
printf 'java\0-Dorg.alice.ide.rootDirectory=%s/missing-distribution\0org.alice.stageide.EntryPoint\0' "$tmp_root" > "$proc_root/4321/cmdline"

python3 "$PROBE" "$inventory" "$output" --proc-root "$proc_root"
status=$?
assert_success "$status" "probe classifies observed Application Root Error window"
assert_file_exists "$output" "probe writes application root error artifact"
assert_contains "$output" '"status": "observed"' "artifact records observed application root error"
assert_contains "$output" '"blocker": "application-root-directory-missing"' "artifact names missing root directory blocker"
assert_contains "$output" '"errorTitle": "Application Root Error"' "artifact records exact dialog title"
assert_contains "$output" '"configuredRootDirectory": ".*/missing-distribution"' "artifact records configured missing root directory"
assert_contains "$output" 'system property: org\.alice\.ide\.rootDirectory is incorrectly set\.[\\]n.*/missing-distribution does not exist\.[\\]nAlice will not work until this is addressed\.' "artifact records exact expected dialog text"
assert_contains "$output" 'Ensure org\.alice\.ide\.rootDirectory points to an existing Alice distribution root before launching EntryPoint' "artifact gives next invocation change"
assert_contains "$output" '"width": 490' "artifact preserves blocker window geometry"
assert_contains "$output" '"height": 119' "artifact preserves blocker window geometry height"

unset_inventory="$tmp_root/x-window-inventory-unset.json"
unset_output="$tmp_root/application-root-error-unset.json"
cat > "$unset_inventory" <<'JSON'
{
  "windows": [
    {
      "title": "Application Root Error",
      "pid": 9876,
      "processName": "java",
      "geometry": {"width": 490, "height": 119}
    }
  ]
}
JSON
mkdir -p "$proc_root/9876"
mkdir -p "$tmp_root/alice-ide"
ln -s "$tmp_root/alice-ide" "$proc_root/9876/cwd"
printf 'java\0org.codehaus.plexus.classworlds.launcher.Launcher\0compile\0exec:java\0-Dalice-ide\0' > "$proc_root/9876/cmdline"
python3 "$PROBE" "$unset_inventory" "$unset_output" --proc-root "$proc_root"
status=$?
assert_success "$status" "probe handles missing rootDirectory system property"
assert_contains "$unset_output" '"blocker": "application-root-property-not-set"' "artifact names unset system property blocker"
assert_contains "$unset_output" 'system property: org\.alice\.ide\.rootDirectory is not set\.[\\]nAlice will not work until this is addressed\.' "artifact records unset property dialog text"
assert_contains "$unset_output" 'For this Maven exec:java launch path, add -Dorg\.alice\.ide\.rootDirectory=../core/resources/target/distribution to the Maven argv' "artifact gives Maven-specific next invocation change"

negative_inventory="$tmp_root/x-window-inventory-negative.json"
negative_output="$tmp_root/application-root-error-negative.json"
cat > "$negative_inventory" <<'JSON'
{
  "windows": [
    {
      "title": "Maven",
      "pid": 1234,
      "processName": "java",
      "geometry": {"width": 200, "height": 100}
    }
  ]
}
JSON
python3 "$PROBE" "$negative_inventory" "$negative_output" --proc-root "$proc_root"
status=$?
assert_success "$status" "probe plainly reports unsupported negative case"
assert_contains "$negative_output" '"status": "not-observed"' "negative case does not pretend application root error"
assert_contains "$negative_output" '"blocker": "application-root-error-window-not-found"' "negative case names absent error window"
assert_contains "$negative_output" '"expectedDialogText": ""' "negative case does not invent dialog text"

finish
