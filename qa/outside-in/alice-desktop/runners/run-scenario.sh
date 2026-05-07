#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
VALIDATOR="$SCRIPT_DIR/validate-scenarios.sh"

usage() {
  cat <<'EOF'
usage:
  run-scenario.sh list
  run-scenario.sh validate
  run-scenario.sh run <scenario-id-or-path> [--evidence-dir <dir>] [--timeout-seconds <seconds>] [--prepare-only]

Environment:
  ALICE_QA_SCENARIO_DIR  Override the directory containing scenario YAML files.
  ALICE_QA_DISPLAY       Reuse a specific X display, for example :99.
  ALICE_QA_SCREEN        Xvfb screen geometry, default 1280x900x24.
  ALICE_QA_READY_WAIT_SECONDS
                         Override GUI readiness wait before screenshot capture.
  ALICE_QA_RUN_GATED_SMOKES=1
                          Execute gated command smoke scenarios.
                          Without it, gated smokes write gated-not-run evidence
                          and exit non-zero unless --prepare-only is requested.
  ALICE_QA_DISABLE_XVFB=1
                          Force the Xvfb-unavailable fallback for contract tests.
EOF
}

json_fields() {
  local scenario_json=$1
  shift
  SCENARIO_JSON="$scenario_json" python3 - "$@" <<'PY'
import json
import os
import sys

scenario = json.loads(os.environ["SCENARIO_JSON"])
for field in sys.argv[1:]:
    value = scenario
    for part in field.split("."):
        value = value[part]
    print(value)
PY
}

json_list_nul() {
  local scenario_json=$1
  local field=$2
  SCENARIO_JSON="$scenario_json" python3 - "$field" <<'PY'
import json
import os
import sys

value = json.loads(os.environ["SCENARIO_JSON"])
for part in sys.argv[1].split("."):
    value = value[part]
for item in value:
    sys.stdout.buffer.write(item.encode("utf-8") + b"\0")
PY
}

validate_allowed_automation() {
  local cwd=$1
  shift

  if [ "$cwd" = alice-ide ] &&
    [ "$#" -eq 3 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = exec:java ] &&
    [ "$3" = -Dalice-ide ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 8 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -pl ] &&
    [ "$5" = netbeans ] &&
    [ "$6" = -am ] &&
    [ "$7" = -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest ] &&
    [ "$8" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/story-api-migration ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest ] &&
    [ "${10}" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = core/story-api-migration ] &&
    [ "$7" = -am ] &&
    [ "$8" = -Dtest=org.lgna.project.io.IoUtilitiesTest ] &&
    [ "$9" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = core/ide ] &&
    [ "$7" = -am ] &&
    { [ "$8" = -Dtest=org.alice.ide.ProjectSaveTargetPlanTest ] || [ "$8" = -Dtest=org.alice.ide.ProjectLoadFailureDispatchPlanTest ]; } &&
    [ "$9" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = core/ide ] &&
    [ "$7" = -am ] &&
    [ "$8" = -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest ] &&
    [ "$9" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 1 ] &&
    [ "$1" = qa/outside-in/alice-desktop/runners/netbeans-package-smoke.sh ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 1 ] &&
    [ "$1" = qa/outside-in/alice-desktop/runners/package-install-smoke.sh ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 8 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -pl ] &&
    [ "$5" = netbeans ] &&
    [ "$6" = -am ] &&
    [ "$7" = -Dtest=org.alice.netbeans.Alice3ProjectTemplateWizardIteratorTest,org.alice.netbeans.palette.Alice3PaletteFactoryTest,org.alice.netbeans.palette.items.AliceComponentPaletteUtilitiesTest,org.alice.netbeans.palette.items.resources.PaletteBundleLocalizationTest,org.alice.netbeans.completion.Alice3CompletionItemTest ] &&
    [ "$8" = test ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 7 ] &&
    [ "$1" = qa/outside-in/alice-desktop/runners/run-scenario.sh ] &&
    [ "$2" = run ] &&
    [ "$3" = alice-desktop-launch ] &&
    [ "$4" = --timeout-seconds ] &&
    [ "$5" = 30 ] &&
    [ "$6" = --evidence-dir ] &&
    [ "$7" = qa/outside-in/alice-desktop/evidence/future-ui-launch ]; then
    return 0
  fi

  printf '%s\n' 'automation.argv is restricted to the allowed Alice QA command set' >&2
  return 2
}

format_argv() {
  local IFS=' '
  printf '%s' "$*"
}
resolve_automation_cwd() {
  local cwd=$1
  python3 - "$REPO_ROOT" "$cwd" <<'PY'
import sys
from pathlib import Path

repo_root = Path(sys.argv[1]).resolve(strict=True)
cwd = sys.argv[2]
cwd_path = Path(cwd)

if cwd_path.is_absolute():
    print("automation.cwd must be repository-relative, not absolute", file=sys.stderr)
    sys.exit(2)
if any(part == ".." for part in cwd_path.parts):
    print("automation.cwd must not contain .. path traversal", file=sys.stderr)
    sys.exit(2)

try:
    resolved = (repo_root / cwd_path).resolve(strict=True)
except FileNotFoundError:
    print("automation.cwd must be an existing directory inside repository root", file=sys.stderr)
    sys.exit(2)

if not resolved.is_dir():
    print("automation.cwd must be an existing directory inside repository root", file=sys.stderr)
    sys.exit(2)

try:
    resolved.relative_to(repo_root)
except ValueError:
    print("automation.cwd must resolve inside repository root", file=sys.stderr)
    sys.exit(2)

print(resolved)
PY
}

validate_scenario_automation_cwd() {
  local scenario_json=$1
  local cwd

  cwd=$(SCENARIO_JSON="$scenario_json" python3 - <<'PY'
import json
import os
import sys

scenario = json.loads(os.environ["SCENARIO_JSON"])
automation = scenario.get("automation")
if not isinstance(automation, dict):
    sys.exit(0)

cwd = automation.get("cwd")
if not isinstance(cwd, str) or not cwd.strip():
    print("automation.cwd must be a non-empty string", file=sys.stderr)
    sys.exit(2)

print(cwd)
PY
  )
  [ -z "$cwd" ] || resolve_automation_cwd "$cwd" >/dev/null
}

resolve_scenario_id() {
  local request=$1
  local scenario_dir=${ALICE_QA_SCENARIO_DIR:-$BASE_DIR/scenarios}

  if [ -f "$request" ]; then
    python3 - "$request" "$scenario_dir" <<'PY'
import re
import sys
from pathlib import Path

scenario_path = Path(sys.argv[1]).resolve()
scenario_dir = Path(sys.argv[2]).resolve()

try:
    scenario_path.relative_to(scenario_dir)
except ValueError:
    print(
        f"scenario path must be inside active scenario directory: {scenario_dir}",
        file=sys.stderr,
    )
    sys.exit(2)

if scenario_path.parent != scenario_dir:
    print(
        f"scenario path must be directly inside active scenario directory: {scenario_dir}",
        file=sys.stderr,
    )
    sys.exit(2)

if scenario_path.suffix != ".yaml":
    print(f"scenario path must be a .yaml file: {scenario_path}", file=sys.stderr)
    sys.exit(2)

last_line_number = 0
for line_number, line in enumerate(scenario_path.read_text(encoding="utf-8").splitlines(), 1):
    last_line_number = line_number
    if not line.strip() or line.lstrip().startswith("#"):
        continue
    match = re.fullmatch(r"id:\s*['\"]?([^'\"]+)['\"]?", line.strip())
    if match:
        print(match.group(1))
        sys.exit(0)

print(f"{scenario_path}:{last_line_number}: missing top-level id field", file=sys.stderr)
sys.exit(1)
PY
  elif [[ "$request" == */* || "$request" == *.yaml ]]; then
    printf 'scenario path not found: %s\n' "$request" >&2
    return 2
  else
    printf '%s\n' "$request"
  fi
}

write_checklist() {
  local scenario_json=$1
  local run_dir=$2
  SCENARIO_JSON="$scenario_json" RUN_DIR="$run_dir" python3 - <<'PY'
import json
import os
from pathlib import Path

scenario = json.loads(os.environ["SCENARIO_JSON"])
run_dir = Path(os.environ["RUN_DIR"])
path = run_dir / "manual-evidence-checklist.txt"

def section(lines, title, values):
    lines.append("")
    lines.append(title)
    lines.append("-" * len(title))
    for index, value in enumerate(values, 1):
        lines.append(f"{index}. {value}")

lines = [
    f"Scenario: {scenario['id']}",
    f"Title: {scenario['title']}",
    f"Workflow: {scenario['workflow']}",
    f"Automation mode: {scenario['automationMode']}",
]
section(lines, "Preconditions", scenario["preconditions"])
section(lines, "User actions", scenario["userActions"])
section(lines, "Expected outcomes", scenario["expectedOutcomes"])
section(lines, "Required evidence", scenario["evidence"]["required"])
section(lines, "Fallback notes", scenario["fallback"]["notes"])
section(
    lines,
    "Completion status",
    [
        "Checklist generation is not complete until required evidence is attached.",
        "A human performs the workflow, adds artifacts to this run directory, and records review notes.",
        "Human reviewer accepts the scenario only after the required evidence matches the expected outcomes.",
    ],
)

path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(path)
PY
}

write_manual_status() {
  local run_dir=$1
  local checklist_path=$2
  local scenario_id=$3
  local automation_mode=$4

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    printf 'outcome=manual-evidence-required\n'
    printf 'checklist=%s\n' "$(basename "$checklist_path")"
  } > "$run_dir/status.txt"
}

validate_positive_integer() {
  local value=$1
  local label=$2
  if [[ ! "$value" =~ ^[1-9][0-9]*$ ]]; then
    printf 'invalid %s: must be a positive integer\n' "$label" >&2
    exit 2
  fi
}

write_environment() {
  local run_dir=$1
  local display=${2:-${DISPLAY:-}}
  {
    printf 'timestamp_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'repo_root=%s\n' "$REPO_ROOT"
    printf 'display=%s\n' "$display"
    printf '\n[java]\n'
    java -version 2>&1
    printf '\n[maven]\n'
    mvn -version 2>&1
    printf '\n[uname]\n'
    uname -a 2>&1
  } > "$run_dir/environment.txt"
}

write_controlled_display_pixel_observation() {
  local run_dir=$1
  local status=$2
  local blocker=$3
  local blocker_detail=$4
  local display=${5:-}
  local pixels_observed=${6:-false}
  local claim=${7:-no-visible-pixel-proof}
  local missing_executable=${8:-}
  local ready_status=${9:-not-attempted}
  local process_status=${10:-not-started}
  local screenshot_status=${11:-not-attempted}
  local screenshot_file=${12:-}
  local xvfb_executable=${13:-}
  local screenshot_tool=${14:-}
  local screenshot_pixel_status=${15:-not-attempted}
  local screenshot_pixel_detail=${16:-}

  CONTROLLED_DISPLAY_STATUS="$status" \
  CONTROLLED_DISPLAY_BLOCKER="$blocker" \
  CONTROLLED_DISPLAY_BLOCKER_DETAIL="$blocker_detail" \
  CONTROLLED_DISPLAY_DISPLAY="$display" \
  CONTROLLED_DISPLAY_PIXELS_OBSERVED="$pixels_observed" \
  CONTROLLED_DISPLAY_CLAIM="$claim" \
  CONTROLLED_DISPLAY_MISSING_EXECUTABLE="$missing_executable" \
  CONTROLLED_DISPLAY_READY_STATUS="$ready_status" \
  CONTROLLED_DISPLAY_PROCESS_STATUS="$process_status" \
  CONTROLLED_DISPLAY_SCREENSHOT_STATUS="$screenshot_status" \
  CONTROLLED_DISPLAY_SCREENSHOT_FILE="$screenshot_file" \
  CONTROLLED_DISPLAY_XVFB_EXECUTABLE="$xvfb_executable" \
  CONTROLLED_DISPLAY_SCREENSHOT_TOOL="$screenshot_tool" \
  CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_STATUS="$screenshot_pixel_status" \
  CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_DETAIL="$screenshot_pixel_detail" \
  python3 - "$run_dir/controlled-display-pixel-observation.json" <<'PY'
import json
import os
import sys

path = sys.argv[1]

def value(name):
    return os.environ.get(name, "")

payload = {
    "status": value("CONTROLLED_DISPLAY_STATUS"),
    "blocker": value("CONTROLLED_DISPLAY_BLOCKER"),
    "blockerDetail": value("CONTROLLED_DISPLAY_BLOCKER_DETAIL"),
    "display": value("CONTROLLED_DISPLAY_DISPLAY"),
    "pixelsObserved": value("CONTROLLED_DISPLAY_PIXELS_OBSERVED") == "true",
    "claim": value("CONTROLLED_DISPLAY_CLAIM"),
    "readyStatus": value("CONTROLLED_DISPLAY_READY_STATUS"),
    "processStatus": value("CONTROLLED_DISPLAY_PROCESS_STATUS"),
    "screenshotStatus": value("CONTROLLED_DISPLAY_SCREENSHOT_STATUS"),
    "screenshotFile": value("CONTROLLED_DISPLAY_SCREENSHOT_FILE"),
    "xvfbExecutable": value("CONTROLLED_DISPLAY_XVFB_EXECUTABLE"),
    "screenshotTool": value("CONTROLLED_DISPLAY_SCREENSHOT_TOOL"),
    "screenshotPixelStatus": value("CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_STATUS"),
    "screenshotPixelDetail": value("CONTROLLED_DISPLAY_SCREENSHOT_PIXEL_DETAIL"),
}
missing_executable = value("CONTROLLED_DISPLAY_MISSING_EXECUTABLE")
if missing_executable:
    payload["missingExecutable"] = missing_executable

with open(path, "w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

select_display() {
  if [ -n "${ALICE_QA_DISPLAY:-}" ]; then
    printf '%s\n' "$ALICE_QA_DISPLAY"
    return 0
  fi

  local number
  for number in {90..120}; do
    if ! DISPLAY=":$number" xdpyinfo >/dev/null 2>&1; then
      printf ':%s\n' "$number"
      return 0
    fi
  done

  return 1
}

capture_screenshot() {
  local output=$1
  if command -v import >/dev/null 2>&1; then
    import -window root "$output"
  elif command -v gnome-screenshot >/dev/null 2>&1; then
    gnome-screenshot --file "$output"
  elif command -v xwd >/dev/null 2>&1; then
    xwd -root -silent -out "${output%.png}.xwd"
  else
    return 3
  fi
}

screenshot_tool_name() {
  if command -v import >/dev/null 2>&1; then
    printf 'import\n'
  elif command -v gnome-screenshot >/dev/null 2>&1; then
    printf 'gnome-screenshot\n'
  elif command -v xwd >/dev/null 2>&1; then
    printf 'xwd\n'
  else
    printf 'none\n'
  fi
}

analyze_screenshot_pixels() {
  local image_path=$1
  local output_path=$2
  if ! command -v identify >/dev/null 2>&1; then
    {
      printf 'status=analysis-unavailable\n'
      printf 'detail=ImageMagick identify is not available; screenshot pixels cannot be classified.\n'
    } > "$output_path"
    return 0
  fi

  if ! identify -quiet \
      -format 'width=%w\nheight=%h\nminima=%[fx:minima]\nmaxima=%[fx:maxima]\nmean=%[fx:mean]\ncolors=%k\n' \
      "$image_path" > "$output_path.raw" 2>"$output_path.err"; then
    {
      printf 'status=analysis-failed\n'
      printf 'detail=ImageMagick identify could not inspect the screenshot; see screenshot-pixels.txt.err.\n'
    } > "$output_path"
    return 0
  fi

  local maxima colors width height
  maxima=$(sed -n 's/^maxima=//p' "$output_path.raw")
  colors=$(sed -n 's/^colors=//p' "$output_path.raw")
  width=$(sed -n 's/^width=//p' "$output_path.raw")
  height=$(sed -n 's/^height=//p' "$output_path.raw")
  if [[ ! "$maxima" =~ ^[0-9]+([.][0-9]+)?([eE][-+]?[0-9]+)?$ ]]; then
    {
      printf 'status=analysis-failed\n'
      printf 'detail=ImageMagick identify did not report a numeric maxima value; screenshot pixels cannot be classified safely.\n'
      cat "$output_path.raw"
    } > "$output_path"
    return 0
  fi
  if awk "BEGIN { exit !($maxima == 0) }"; then
    {
      printf 'status=uniform-black\n'
      printf 'detail=Screenshot is %sx%s with %s color(s), but every sampled pixel is black.\n' "$width" "$height" "$colors"
      cat "$output_path.raw"
    } > "$output_path"
    return 0
  fi

  {
    printf 'status=non-black-pixels\n'
    printf 'detail=Screenshot is %sx%s with non-black pixel data.\n' "$width" "$height"
    cat "$output_path.raw"
  } > "$output_path"
}

run_xvfb_real_alice() {
  local scenario_json=$1
  local run_dir=$2
  local timeout_override=$3

  local automation_fields cwd configured_timeout ready_wait run_timeout display scenario_id automation_mode resolved_cwd
  local -a argv
  mapfile -t automation_fields < <(json_fields "$scenario_json" "automation.cwd" "automation.timeoutSeconds" "automation.readyWaitSeconds" "id" "automationMode")
  cwd=${automation_fields[0]}
  configured_timeout=${automation_fields[1]}
  ready_wait="${ALICE_QA_READY_WAIT_SECONDS:-${automation_fields[2]}}"
  scenario_id=${automation_fields[3]}
  automation_mode=${automation_fields[4]}
  mapfile -d '' -t argv < <(json_list_nul "$scenario_json" "automation.argv")
  run_timeout="${timeout_override:-$configured_timeout}"
  xvfb_pid=
  alice_pid=
  xvfb_executable=$(command -v Xvfb 2>/dev/null || true)
  if [ "${ALICE_QA_DISABLE_XVFB:-}" = "1" ]; then
    xvfb_executable=
  fi

  validate_allowed_automation "$cwd" "${argv[@]}"
  resolved_cwd=$(resolve_automation_cwd "$cwd")

  if [ -z "$xvfb_executable" ]; then
    write_environment "$run_dir"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      x-server-unavailable \
      "Xvfb executable is not available on PATH; install Xvfb before retrying this controlled-display runner." \
      "" \
      false \
      no-visible-pixel-proof \
      Xvfb
    printf 'Xvfb is not available; wrote manual fallback checklist to %s\n' "$run_dir" >&2
    return 2
  fi

  if ! display=$(select_display); then
    write_environment "$run_dir"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      display-allocation-unavailable \
      "No free X display could be selected; set ALICE_QA_DISPLAY to a reachable display and retry." \
      "" \
      false \
      no-visible-pixel-proof \
      "" \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "$xvfb_executable"
    printf 'No free X display found; wrote manual fallback checklist to %s\n' "$run_dir" >&2
    return 2
  fi

  Xvfb "$display" -screen 0 "${ALICE_QA_SCREEN:-1280x900x24}" > "$run_dir/xvfb.log" 2>&1 &
  xvfb_pid=$!
  alice_pid=

  cleanup() {
    if [ -n "${alice_pid:-}" ] && kill -0 "$alice_pid" >/dev/null 2>&1; then
      if ! kill "$alice_pid" >/dev/null 2>&1; then
        printf 'warning: failed to stop Alice launch process %s\n' "$alice_pid" >&2
      fi
      if ! wait "$alice_pid" >/dev/null 2>&1; then
        :
      fi
    fi
    if kill -0 "$xvfb_pid" >/dev/null 2>&1; then
      if ! kill "$xvfb_pid" >/dev/null 2>&1; then
        printf 'warning: failed to stop Xvfb process %s\n' "$xvfb_pid" >&2
      fi
      if ! wait "$xvfb_pid" >/dev/null 2>&1; then
        :
      fi
    fi
  }
  trap cleanup EXIT

  sleep 2
  if ! kill -0 "$xvfb_pid" >/dev/null 2>&1; then
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      x-server-start-failed \
      "Xvfb exited before Alice launch; inspect xvfb.log for display permissions, screen geometry, or backend startup errors." \
      "$display" \
      false \
      no-visible-pixel-proof \
      "" \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "$xvfb_executable"
    printf 'Xvfb exited before Alice launch; see %s/xvfb.log\n' "$run_dir" >&2
    return 2
  fi

  export DISPLAY=$display
  write_environment "$run_dir" "$display"

  (
    cd "$resolved_cwd"
    timeout -k 10s "${run_timeout}s" "${argv[@]}"
  ) > "$run_dir/launch.log" 2>&1 &
  alice_pid=$!

  local ready_status=not-checked
  local waited=0
  if command -v xdotool >/dev/null 2>&1; then
    ready_status=not-found
    while [ "$waited" -lt "$ready_wait" ]; do
      if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
        ready_status=process-exited
        break
      fi
      if xdotool search --onlyvisible --name Alice >/dev/null 2>&1 ||
          xdotool search --onlyvisible --class Alice >/dev/null 2>&1 ||
          xdotool search --onlyvisible --class alice >/dev/null 2>&1; then
        ready_status=alice-window-found
        break
      fi
      sleep 1
      waited=$((waited + 1))
    done
    if [ "$ready_status" = not-found ] &&
        kill -0 "$alice_pid" >/dev/null 2>&1 &&
        xdotool search --onlyvisible --class ".*" >/dev/null 2>&1; then
      ready_status=non-alice-visible-window-found
    fi
  else
    sleep "$ready_wait"
    ready_status=waited-without-window-detector
  fi

  local screenshot_tool screenshot_status
  screenshot_tool=$(screenshot_tool_name)
  screenshot_status=screenshot-captured
  if ! capture_screenshot "$run_dir/screenshot.png" > "$run_dir/screenshot.log" 2>&1; then
    screenshot_status=screenshot-failed
    write_checklist "$scenario_json" "$run_dir" >/dev/null
  fi

  local process_status=running
  if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
    process_status=exited-before-capture
  fi

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    printf 'display=%s\n' "$display"
    printf 'readyStatus=%s\n' "$ready_status"
    printf 'processStatus=%s\n' "$process_status"
    printf 'screenshotStatus=%s\n' "$screenshot_status"
    printf 'timeoutSeconds=%s\n' "$run_timeout"
  } > "$run_dir/status.txt"

  local observation_status observation_blocker observation_detail pixels_observed observation_claim screenshot_file
  observation_status=observed
  observation_blocker=none
  observation_detail="Xvfb display was reachable, a visible Alice window was detected, and a root screenshot with non-black pixels was captured; this does not assert Alice rendering correctness."
  pixels_observed=true
  observation_claim=controlled-display-pixels-observed-rendering-not-asserted
  screenshot_file=screenshot.png
  if [ "$screenshot_tool" = xwd ]; then
    screenshot_file=screenshot.xwd
  fi
  local screenshot_pixel_status screenshot_pixel_detail
  screenshot_pixel_status=not-attempted
  screenshot_pixel_detail=
  if [ "$screenshot_status" = screenshot-captured ] && [ "$screenshot_tool" != xwd ]; then
    analyze_screenshot_pixels "$run_dir/$screenshot_file" "$run_dir/screenshot-pixels.txt"
    screenshot_pixel_status=$(sed -n 's/^status=//p' "$run_dir/screenshot-pixels.txt" | head -1)
    screenshot_pixel_detail=$(sed -n 's/^detail=//p' "$run_dir/screenshot-pixels.txt" | head -1)
  elif [ "$screenshot_status" = screenshot-captured ]; then
    screenshot_pixel_status=analysis-unavailable
    screenshot_pixel_detail="xwd screenshot capture is available, but this runner does not classify xwd pixel contents."
  fi
  if [ "$screenshot_status" != screenshot-captured ]; then
    observation_status=blocked
    observation_blocker=screenshot-capture-failed
    observation_detail="Screenshot capture failed on DISPLAY=$display; install import, gnome-screenshot, or xwd and inspect screenshot.log."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$process_status" != running ]; then
    observation_status=blocked
    observation_blocker=application-exited-before-pixel-capture
    observation_detail="Alice launch process exited before pixel capture could prove a visible display; inspect launch.log."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$ready_status" = process-exited ]; then
    observation_status=blocked
    observation_blocker=application-exited-before-window-ready
    observation_detail="Alice launch process exited before window readiness; inspect launch.log."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$ready_status" != alice-window-found ]; then
    observation_status=blocked
    observation_blocker=alice-window-not-found
    observation_detail="Xvfb was reachable, but xdotool did not find a visible Alice window before screenshot capture; readyStatus=$ready_status."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$screenshot_pixel_status" = uniform-black ]; then
    observation_status=blocked
    observation_blocker=screenshot-captured-uniform-black
    observation_detail="A controlled Xvfb screenshot was captured after Alice window readiness, but the image was uniformly black."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$screenshot_pixel_status" != non-black-pixels ]; then
    observation_status=attempted
    observation_blocker=screenshot-pixel-analysis-unavailable
    observation_detail="$screenshot_pixel_detail"
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  fi
  write_controlled_display_pixel_observation \
    "$run_dir" \
    "$observation_status" \
    "$observation_blocker" \
    "$observation_detail" \
    "$display" \
    "$pixels_observed" \
    "$observation_claim" \
    "" \
    "$ready_status" \
    "$process_status" \
    "$screenshot_status" \
    "$screenshot_file" \
    "$xvfb_executable" \
    "$screenshot_tool" \
    "$screenshot_pixel_status" \
    "$screenshot_pixel_detail"

  if [ "$screenshot_status" != screenshot-captured ]; then
    printf 'Screenshot capture failed; see %s/screenshot.log\n' "$run_dir" >&2
    return 2
  fi
  if [ "$process_status" != running ]; then
    printf 'Alice launch process exited before evidence capture; see %s/launch.log\n' "$run_dir" >&2
    return 1
  fi
  if [ "$ready_status" = process-exited ]; then
    printf 'Alice launch process exited before window readiness; see %s/launch.log\n' "$run_dir" >&2
    return 1
  fi
  if [ "$ready_status" = not-found ]; then
    printf 'No visible Alice desktop window was detected; see %s/status.txt\n' "$run_dir" >&2
    return 1
  fi
  if [ "$observation_status" != observed ]; then
    printf 'Controlled display pixel proof blocked: %s; see %s/controlled-display-pixel-observation.json\n' "$observation_blocker" "$run_dir" >&2
    return 1
  fi

  printf 'Evidence written to %s\n' "$run_dir"
}

run_gated_command_smoke() {
  local scenario_json=$1
  local run_dir=$2
  local timeout_override=$3
  local prepare_only=$4

  local automation_fields cwd configured_timeout scenario_id automation_mode run_timeout checklist exit_code outcome resolved_cwd
  local -a argv
  mapfile -t automation_fields < <(json_fields "$scenario_json" "automation.cwd" "automation.timeoutSeconds" "id" "automationMode")
  cwd=${automation_fields[0]}
  configured_timeout=${automation_fields[1]}
  scenario_id=${automation_fields[2]}
  automation_mode=${automation_fields[3]}
  mapfile -d '' -t argv < <(json_list_nul "$scenario_json" "automation.argv")
  run_timeout="${timeout_override:-$configured_timeout}"

  validate_allowed_automation "$cwd" "${argv[@]}"
  resolved_cwd=$(resolve_automation_cwd "$cwd")
  write_environment "$run_dir"

  if [ "$prepare_only" = "1" ] || [ "${ALICE_QA_RUN_GATED_SMOKES:-}" != "1" ]; then
    checklist=$(write_checklist "$scenario_json" "$run_dir")
    {
      printf 'scenario=%s\n' "$scenario_id"
      printf 'automationMode=%s\n' "$automation_mode"
      printf 'outcome=gated-not-run\n'
      printf 'gate=ALICE_QA_RUN_GATED_SMOKES\n'
      if [ "$prepare_only" = "1" ]; then
        printf 'skipMode=prepare-only\n'
      else
        printf 'skipMode=missing-gate\n'
      fi
      printf 'checklist=%s\n' "$(basename "$checklist")"
      printf 'argv=%s\n' "$(format_argv "${argv[@]}")"
      printf 'cwd=%s\n' "$cwd"
      printf 'timeoutSeconds=%s\n' "$run_timeout"
    } > "$run_dir/status.txt"
    if [ "$prepare_only" = "1" ]; then
      printf 'Gated command scenario prepared without execution: %s\n' "$scenario_id"
      return 0
    fi
    printf 'Gated command scenario not run: set ALICE_QA_RUN_GATED_SMOKES=1 to execute %s, or pass --prepare-only to record an intentional skip.\n' "$scenario_id" >&2
    return 3
  fi

  set +e
  (
    cd "$resolved_cwd"
    timeout -k 10s "${run_timeout}s" "${argv[@]}"
  ) > "$run_dir/command.log" 2>&1
  exit_code=$?
  set -e

  outcome=failed
  if [ "$exit_code" -eq 0 ]; then
    outcome=passed
  fi

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    printf 'outcome=%s\n' "$outcome"
    printf 'exitCode=%s\n' "$exit_code"
    printf 'commandLog=command.log\n'
    printf 'argv=%s\n' "$(format_argv "${argv[@]}")"
    printf 'cwd=%s\n' "$cwd"
    printf 'timeoutSeconds=%s\n' "$run_timeout"
  } > "$run_dir/status.txt"

  if [ "$exit_code" -ne 0 ]; then
    printf 'Gated command scenario failed; see %s/command.log\n' "$run_dir" >&2
    return "$exit_code"
  fi

  printf 'Evidence written to %s\n' "$run_dir"
}
command_name=${1:-}
case "$command_name" in
  list)
    "$VALIDATOR" --list
    ;;
  validate)
    "$VALIDATOR"
    ;;
  run)
    shift
    scenario_request=${1:-}
    if [ -z "$scenario_request" ]; then
      usage >&2
      exit 2
    fi
    shift

    evidence_base="$BASE_DIR/evidence"
    timeout_override=
    prepare_only=0
    while [ "$#" -gt 0 ]; do
      case "$1" in
        --evidence-dir)
          if [ "$#" -lt 2 ]; then
            printf '%s\n' '--evidence-dir requires a value' >&2
            exit 2
          fi
          evidence_base=$2
          shift 2
          ;;
        --timeout-seconds)
          if [ "$#" -lt 2 ]; then
            printf '%s\n' '--timeout-seconds requires a value' >&2
            exit 2
          fi
          timeout_override=$2
          validate_positive_integer "$timeout_override" "timeout"
          shift 2
          ;;
        --prepare-only)
          prepare_only=1
          shift
          ;;
        *)
          printf 'unknown argument: %s\n' "$1" >&2
          usage >&2
          exit 2
          ;;
        esac
    done

    scenario_id=$(resolve_scenario_id "$scenario_request")
    scenario_json=$("$VALIDATOR" --dump-json "$scenario_id")
    validate_scenario_automation_cwd "$scenario_json"
    timestamp=$(date -u +%Y%m%dT%H%M%SZ)
    run_dir="$evidence_base/$scenario_id/$timestamp"
    mkdir -p "$run_dir"

    automation_mode=$(json_fields "$scenario_json" "automationMode")
    case "$automation_mode" in
      xvfb-real-alice)
        run_xvfb_real_alice "$scenario_json" "$run_dir" "$timeout_override"
        ;;
      gated-command-smoke)
        run_gated_command_smoke "$scenario_json" "$run_dir" "$timeout_override" "$prepare_only"
        ;;
      manual-evidence-required)
        write_environment "$run_dir"
        checklist=$(write_checklist "$scenario_json" "$run_dir")
        write_manual_status "$run_dir" "$checklist" "$scenario_id" "$automation_mode"
        printf 'Manual scenario prepared: %s\n' "$checklist"
        ;;
      *)
        printf 'unsupported automationMode: %s\n' "$automation_mode" >&2
        exit 1
        ;;
    esac
    ;;
  -h|--help|help|"")
    usage
    ;;
  *)
    printf 'unknown command: %s\n' "$command_name" >&2
    usage >&2
    exit 2
    ;;
esac
