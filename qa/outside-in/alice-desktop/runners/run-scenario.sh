#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
VALIDATOR="$SCRIPT_DIR/validate-scenarios.sh"
ROOT_DIRECTORY_PREP="$SCRIPT_DIR/prepare-root-directory.py"
LICENSE_ACCEPTANCE_PREP="$SCRIPT_DIR/prepare-license-acceptance.py"
LICENSE_DIALOG_PROBE="$SCRIPT_DIR/license-dialog-probe.py"
SELECT_PROJECT_PROBE="$SCRIPT_DIR/select-project-probe.py"
SWING_WIDGET_PROBE="$SCRIPT_DIR/swing-widget-probe.py"
TAB_CLICK_PROBE="$SCRIPT_DIR/tab-click-probe.py"
POST_PROJECT_OPEN_PROBE="$SCRIPT_DIR/post-project-open-probe.py"
POST_OPEN_RUNTIME_DISPLAY_PROBE="$SCRIPT_DIR/post-open-runtime-display-probe.py"
POST_OPEN_RUNTIME_DISPLAY_SCENARIO=alice-desktop-post-open-runtime-display-accessibility-evidence
POST_OPEN_RUNTIME_DISPLAY_ARTIFACT=post-open-runtime-display-accessibility-evidence.json

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
  ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1
                           Prepare isolated first-run License Agreement acceptance
                           state for this controlled QA launch only.
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

python_with_module() {
  local module=$1
  local candidate

  for candidate in "${ALICE_QA_PYTHON:-}" python3 /usr/bin/python3; do
    [ -n "$candidate" ] || continue
    if command -v "$candidate" >/dev/null 2>&1 &&
      "$candidate" - "$module" >/dev/null 2>&1 <<'PY'
import importlib
import sys

importlib.import_module(sys.argv[1])
PY
    then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  printf '%s\n' python3
}

target_starter_fields() {
  local scenario_json=$1
  SCENARIO_JSON="$scenario_json" python3 - <<'PY'
import json
import os

scenario = json.loads(os.environ["SCENARIO_JSON"])
target = scenario.get("targetStarter") or {}
print(target.get("displayName", ""))
print(target.get("repositoryPath", ""))
PY
}

validate_allowed_automation() {
  local cwd=$1
  shift

  if [ "$cwd" = alice-ide ] &&
    [ "$#" -eq 8 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dcheckstyle.skip ] &&
    [ "$5" = -DskipTests ] &&
    [ "$6" = compile ] &&
    [ "$7" = exec:java ] &&
    [ "$8" = -Dalice-ide ]; then
    return 0
  fi

  if [ "$cwd" = alice-ide ] &&
    [ "$#" -eq 7 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dcheckstyle.skip ] &&
    [ "$5" = -DskipTests ] &&
    [ "$6" = compile ] &&
    [ "$7" = exec:exec@alice-ide-atk ]; then
    return 0
  fi

  if [ "$cwd" = . ] &&
    [ "$#" -eq 9 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$5" = -pl ] &&
    [ "$6" = netbeans ] &&
    [ "$7" = -am ] &&
    [ "$8" = -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest ] &&
    [ "$9" = test ]; then
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
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest ] &&
    [ "${10}" = test ]; then
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
    [ "$7" = core/ide ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.ide.uricontent.FileProjectLoaderTest ] &&
    [ "${10}" = test ]; then
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

  if [ "$cwd" = . ] &&
    [ "$#" -eq 10 ] &&
    [ "$1" = mvn ] &&
    [ "$2" = -DincludeSims=false ] &&
    [ "$3" = -Dinstall4j.skip ] &&
    [ "$4" = -DfailIfNoTests=false ] &&
    [ "$5" = -Dsurefire.failIfNoSpecifiedTests=false ] &&
    [ "$6" = -pl ] &&
    [ "$7" = core/ast ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeCreatesMethodInvocation ] &&
    [ "${10}" = test ]; then
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
    [ "$7" = core/ast ] &&
    [ "$8" = -am ] &&
    [ "$9" = -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall+zeroArgumentThisMethodCallDecodeRejectsImplicitTarget ] &&
    [ "${10}" = test ]; then
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
  local lifecycle_point=${17:-unknown}
  local window_inventory_status=${18:-not-attempted}
  local window_inventory_file=${19:-x-window-inventory.json}
  local alice_window_candidate_count=${20:-0}

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
  CONTROLLED_DISPLAY_LIFECYCLE_POINT="$lifecycle_point" \
  CONTROLLED_DISPLAY_WINDOW_INVENTORY_STATUS="$window_inventory_status" \
  CONTROLLED_DISPLAY_WINDOW_INVENTORY_FILE="$window_inventory_file" \
  CONTROLLED_DISPLAY_ALICE_WINDOW_CANDIDATE_COUNT="$alice_window_candidate_count" \
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
    "lifecyclePoint": value("CONTROLLED_DISPLAY_LIFECYCLE_POINT"),
    "windowInventoryStatus": value("CONTROLLED_DISPLAY_WINDOW_INVENTORY_STATUS"),
    "windowInventoryFile": value("CONTROLLED_DISPLAY_WINDOW_INVENTORY_FILE"),
    "aliceWindowCandidateCount": int(value("CONTROLLED_DISPLAY_ALICE_WINDOW_CANDIDATE_COUNT") or "0"),
}
missing_executable = value("CONTROLLED_DISPLAY_MISSING_EXECUTABLE")
if missing_executable:
    payload["missingExecutable"] = missing_executable

with open(path, "w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

write_x_window_inventory() {
  local run_dir=$1
  local status=$2
  local blocker=$3
  local blocker_detail=$4
  local display=${5:-}
  local lifecycle_point=${6:-unknown}
  local alice_launch_pid=${7:-}
  local missing_executable=${8:-}
  local detector_status=${9:-not-started}

  WINDOW_INVENTORY_STATUS="$status" \
  WINDOW_INVENTORY_BLOCKER="$blocker" \
  WINDOW_INVENTORY_BLOCKER_DETAIL="$blocker_detail" \
  WINDOW_INVENTORY_DISPLAY="$display" \
  WINDOW_INVENTORY_LIFECYCLE_POINT="$lifecycle_point" \
  WINDOW_INVENTORY_ALICE_LAUNCH_PID="$alice_launch_pid" \
  WINDOW_INVENTORY_MISSING_EXECUTABLE="$missing_executable" \
  WINDOW_INVENTORY_DETECTOR_STATUS="$detector_status" \
  python3 - "$run_dir/x-window-inventory.json" <<'PY'
import json
import os
import sys

payload = {
    "status": os.environ.get("WINDOW_INVENTORY_STATUS", ""),
    "blocker": os.environ.get("WINDOW_INVENTORY_BLOCKER", ""),
    "blockerDetail": os.environ.get("WINDOW_INVENTORY_BLOCKER_DETAIL", ""),
    "display": os.environ.get("WINDOW_INVENTORY_DISPLAY", ""),
    "lifecyclePoint": os.environ.get("WINDOW_INVENTORY_LIFECYCLE_POINT", ""),
    "aliceLaunchPid": os.environ.get("WINDOW_INVENTORY_ALICE_LAUNCH_PID", ""),
    "detectorStatus": os.environ.get("WINDOW_INVENTORY_DETECTOR_STATUS", ""),
    "aliceWindowCandidateCount": 0,
    "javaWindowCount": 0,
    "windows": [],
}
missing = os.environ.get("WINDOW_INVENTORY_MISSING_EXECUTABLE", "")
if missing:
    payload["missingExecutable"] = missing

with open(sys.argv[1], "w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

collect_x_window_inventory() {
  local run_dir=$1
  local display=$2
  local lifecycle_point=$3
  local alice_launch_pid=${4:-}

  if [ "${ALICE_QA_DISABLE_WINDOW_DETECTOR:-}" = "1" ] || ! command -v xdotool >/dev/null 2>&1; then
    write_x_window_inventory \
      "$run_dir" \
      blocked \
      window-detector-unavailable \
      "xdotool is not available, so visible X windows cannot be enumerated safely." \
      "$display" \
      "$lifecycle_point" \
      "$alice_launch_pid" \
      xdotool \
      unavailable
    return 0
  fi

  DISPLAY="$display" WINDOW_INVENTORY_LIFECYCLE_POINT="$lifecycle_point" WINDOW_INVENTORY_ALICE_LAUNCH_PID="$alice_launch_pid" \
  python3 - "$run_dir/x-window-inventory.json" <<'PY'
import json
import os
import re
import subprocess
import sys
from pathlib import Path

output_path = Path(sys.argv[1])
display = os.environ.get("DISPLAY", "")
lifecycle_point = os.environ.get("WINDOW_INVENTORY_LIFECYCLE_POINT", "")
alice_launch_pid = os.environ.get("WINDOW_INVENTORY_ALICE_LAUNCH_PID", "")


def run(args):
    try:
        completed = subprocess.run(
            args,
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            env=os.environ.copy(),
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        return 127, "", str(exc)
    return completed.returncode, completed.stdout.strip(), completed.stderr.strip()


def command_output(args):
    code, stdout, _ = run(args)
    return stdout if code == 0 else ""


def process_name(pid):
    if not pid:
        return ""
    comm = Path("/proc") / str(pid) / "comm"
    try:
        return comm.read_text(encoding="utf-8").strip()
    except OSError:
        return ""


def parent_pid(pid):
    if not pid:
        return None
    stat = Path("/proc") / str(pid) / "stat"
    try:
        content = stat.read_text(encoding="utf-8")
    except OSError:
        return None
    try:
        return int(content.rsplit(")", 1)[1].split()[1])
    except (IndexError, ValueError):
        return None


def process_descends_from(pid, ancestor_pid):
    if not pid or not ancestor_pid:
        return False
    try:
        current = int(pid)
        ancestor = int(ancestor_pid)
    except ValueError:
        return False
    seen = set()
    while current > 1 and current not in seen:
        if current == ancestor:
            return True
        seen.add(current)
        next_pid = parent_pid(current)
        if next_pid is None:
            return False
        current = next_pid
    return False


def window_class_for(window_id):
    window_class = command_output(["xdotool", "getwindowclassname", window_id])
    if window_class:
        return window_class
    code, stdout, _ = run(["xprop", "-id", window_id, "WM_CLASS"])
    if code != 0:
        return ""
    values = re.findall(r'"([^"]*)"', stdout)
    if values:
        return values[-1]
    return ""


def geometry_for(window_id):
    stdout = command_output(["xdotool", "getwindowgeometry", "--shell", window_id])
    values = {}
    for line in stdout.splitlines():
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    return {
        "x": int(values["X"]) if values.get("X", "").lstrip("-").isdigit() else None,
        "y": int(values["Y"]) if values.get("Y", "").lstrip("-").isdigit() else None,
        "width": int(values["WIDTH"]) if values.get("WIDTH", "").isdigit() else None,
        "height": int(values["HEIGHT"]) if values.get("HEIGHT", "").isdigit() else None,
        "screen": int(values["SCREEN"]) if values.get("SCREEN", "").isdigit() else None,
    }


search_code, search_stdout, search_stderr = run(["xdotool", "search", "--onlyvisible", "--class", ".*"])
window_ids = []
if search_code == 0:
    seen = set()
    for line in search_stdout.splitlines():
        window_id = line.strip()
        if not window_id or window_id in seen:
            continue
        seen.add(window_id)
        window_ids.append(window_id)

windows = []
alice_candidate_count = 0
java_window_count = 0
for window_id in window_ids:
    title = command_output(["xdotool", "getwindowname", window_id])
    window_class = window_class_for(window_id)
    pid = command_output(["xdotool", "getwindowpid", window_id])
    proc_name = process_name(pid)
    reasons = []
    if "alice" in title.lower():
        reasons.append("title-contains-alice")
    if "alice" in window_class.lower():
        reasons.append("class-contains-alice")
    if "alice" in proc_name.lower():
        reasons.append("process-name-contains-alice")
    java_process = proc_name == "java"
    java_reasons = []
    if java_process:
        java_reasons.append("process-name-java")
    if process_descends_from(pid, alice_launch_pid):
        reasons.append("descends-from-alice-launch")
    known_alice_title = title in {
        "Alice 3",
        "Select Project",
        "Application Root Error",
        "License Agreement (Part 1 of 2): Alice 3",
        "License Agreement (Part 2 of 2): The Sims (TM) 2 Art Assets",
    }
    if known_alice_title:
        reasons.append("known-alice-window-title")
    if not reasons:
        continue
    alice_candidate_count += 1
    if java_process:
        java_window_count += 1
    windows.append(
        {
            "id": window_id,
            "title": title,
            "class": window_class,
            "pid": int(pid) if re.fullmatch(r"[0-9]+", pid or "") else None,
            "processName": proc_name,
            "geometry": geometry_for(window_id),
            "aliceCandidate": bool(reasons),
            "javaWindow": java_process,
            "candidateReasons": reasons,
            "javaWindowReasons": java_reasons,
        }
    )

if windows:
    status = "observed"
    blocker = "none"
    detail = f"Enumerated {len(windows)} Alice-related visible X window(s) on {display}; inspect windows[] for exact title/class/process/geometry."
else:
    status = "blocked"
    blocker = "no-alice-related-x-windows"
    detail = "xdotool did not identify any Alice-related visible X windows after the launch readiness wait."
    if search_stderr:
        detail = f"{detail} xdotool stderr: {search_stderr}"

payload = {
    "status": status,
    "blocker": blocker,
    "blockerDetail": detail,
    "display": display,
    "lifecyclePoint": lifecycle_point,
    "aliceLaunchPid": alice_launch_pid,
    "detectorStatus": "available",
    "detector": "xdotool",
    "aliceWindowCandidateCount": alice_candidate_count,
    "javaWindowCount": java_window_count,
    "windows": windows,
}

with output_path.open("w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY
}

inventory_json_field() {
  local inventory_path=$1
  local field=$2
  python3 - "$inventory_path" "$field" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as stream:
    value = json.load(stream)
for part in sys.argv[2].split("."):
    value = value.get(part, "") if isinstance(value, dict) else ""
print(value)
PY
}

write_application_root_error_probe() {
  local inventory_path=$1
  local output_path=$2

  python3 "$SCRIPT_DIR/application-root-error-probe.py" "$inventory_path" "$output_path"
}

write_license_dialog_probe() {
  local inventory_path=$1
  local output_path=$2

  python3 "$LICENSE_DIALOG_PROBE" "$inventory_path" "$output_path"
}

write_select_project_probe() {
  local inventory_path=$1
  local output_path=$2

  python3 "$SELECT_PROJECT_PROBE" "$inventory_path" "$output_path"
}

write_swing_widget_probe() {
  local inventory_path=$1
  local output_path=$2
  local python

  python=$(python_with_module pyatspi)
  "$python" "$SWING_WIDGET_PROBE" "$inventory_path" "$output_path"
}

write_tab_click_probe() {
  local inventory_path=$1
  local output_path=$2
  local python
  local target_display_name=${3:-}
  local target_repo_path=${4:-}

  python=$(python_with_module pyatspi)
  TARGET_STARTER_DISPLAY_NAME="$target_display_name" \
  TARGET_STARTER_REPO_PATH="$target_repo_path" \
  "$python" "$TAB_CLICK_PROBE" "$inventory_path" "$output_path"
}

write_post_project_open_probe() {
  local inventory_path=$1
  local tab_click_path=$2
  local output_path=$3
  local python

  python=$(python_with_module pyatspi)
  "$python" "$POST_PROJECT_OPEN_PROBE" "$inventory_path" "$tab_click_path" "$output_path"
}

write_post_open_runtime_display_blocker() {
  local run_dir=$1
  local scenario_id=$2
  local automation_mode=$3
  local blocker=$4
  local blocker_detail=$5
  local display=${6:-}
  local timeout_seconds=${7:-}

  RUNTIME_DISPLAY_SCENARIO="$scenario_id" \
  RUNTIME_DISPLAY_AUTOMATION_MODE="$automation_mode" \
  RUNTIME_DISPLAY_BLOCKER="$blocker" \
  RUNTIME_DISPLAY_BLOCKER_DETAIL="$blocker_detail" \
  python3 - "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" <<'PY'
import json
import os
import sys

payload = {
    "status": "blocked",
    "blocker": os.environ["RUNTIME_DISPLAY_BLOCKER"],
    "blockerDetail": os.environ["RUNTIME_DISPLAY_BLOCKER_DETAIL"],
    "claim": "post-open-runtime-display-accessibility-evidence",
    "scenario": os.environ["RUNTIME_DISPLAY_SCENARIO"],
    "automationMode": os.environ["RUNTIME_DISPLAY_AUTOMATION_MODE"],
    "javaPid": None,
    "postOpenWindowObserved": False,
    "postOpenRuntimeDisplayAccessibilityObserved": False,
    "runtimeDisplayCandidateCount": 0,
    "runtimeDisplayCandidates": [],
    "traversalErrors": [],
}
with open(sys.argv[1], "w", encoding="utf-8") as stream:
    json.dump(payload, stream, indent=2, sort_keys=True)
    stream.write("\n")
PY

  {
    printf 'scenario=%s\n' "$scenario_id"
    printf 'automationMode=%s\n' "$automation_mode"
    if [ -n "$display" ]; then
      printf 'display=%s\n' "$display"
    fi
    printf 'outcome=blocked\n'
    printf 'runtimeDisplayAccessibilityEvidence=%s\n' "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
    printf 'runtimeDisplayAccessibilityStatus=blocked\n'
    printf 'runtimeDisplayAccessibilityBlocker=%s\n' "$blocker"
    if [ -n "$timeout_seconds" ]; then
      printf 'timeoutSeconds=%s\n' "$timeout_seconds"
    fi
  } > "$run_dir/status.txt"
}

write_post_open_runtime_display_probe() {
  local inventory_path=$1
  local post_open_path=$2
  local output_path=$3
  local status_path=$4
  local scenario_id=$5
  local automation_mode=$6
  local python

  python=$(python_with_module pyatspi)
  "$python" "$POST_OPEN_RUNTIME_DISPLAY_PROBE" \
    --inventory "$inventory_path" \
    --post-open-window-observation "$post_open_path" \
    --output "$output_path" \
    --status-file "$status_path" \
    --scenario-id "$scenario_id" \
    --automation-mode "$automation_mode"
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
  local target_starter_display_name target_starter_repo_path
  local -a target_fields
  local needs_select_project_wait=0 needs_tab_click_probe=0
  local xvfb_executable xdotool_executable
  local root_directory_prep_status root_directory_prep_blocker
  local -a argv
  mapfile -t automation_fields < <(json_fields "$scenario_json" "automation.cwd" "automation.timeoutSeconds" "automation.readyWaitSeconds" "id" "automationMode")
  cwd=${automation_fields[0]}
  configured_timeout=${automation_fields[1]}
  ready_wait="${ALICE_QA_READY_WAIT_SECONDS:-${automation_fields[2]}}"
  scenario_id=${automation_fields[3]}
  automation_mode=${automation_fields[4]}
  mapfile -d '' -t argv < <(json_list_nul "$scenario_json" "automation.argv")
  case "$scenario_id" in
    alice-desktop-select-project-*|alice-desktop-post-project-open-window-state|"$POST_OPEN_RUNTIME_DISPLAY_SCENARIO")
      needs_select_project_wait=1
      ;;
  esac
  case "$scenario_id" in
    alice-desktop-select-project-tab-click-exec|alice-desktop-post-project-open-window-state|"$POST_OPEN_RUNTIME_DISPLAY_SCENARIO")
      needs_tab_click_probe=1
      mapfile -t target_fields < <(target_starter_fields "$scenario_json")
      target_starter_display_name=${target_fields[0]:-}
      target_starter_repo_path=${target_fields[1]:-}
      ;;
    *)
      target_starter_display_name=
      target_starter_repo_path=
      ;;
  esac
  run_timeout="${timeout_override:-$configured_timeout}"
  xvfb_pid=
  alice_pid=
  xvfb_executable=$(command -v Xvfb 2>/dev/null || true)
  xdotool_executable=$(command -v xdotool 2>/dev/null || true)
  if [ "${ALICE_QA_DISABLE_XVFB:-}" = "1" ]; then
    xvfb_executable=
  fi

  validate_allowed_automation "$cwd" "${argv[@]}"
  resolved_cwd=$(resolve_automation_cwd "$cwd")

  if [ -z "$xvfb_executable" ]; then
    write_environment "$run_dir"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      x-server-unavailable \
      "Xvfb executable is not available on PATH; no X server exists for window enumeration." \
      "" \
      before-x-server-start \
      "" \
      Xvfb \
      not-started
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      x-server-unavailable \
      "Xvfb executable is not available on PATH; install Xvfb before retrying this controlled-display runner." \
      "" \
      false \
      no-visible-pixel-proof \
      Xvfb \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "" \
      "" \
      not-attempted \
      "" \
      before-x-server-start \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        x-server-unavailable \
        "Xvfb executable is not available on PATH; no X server exists for post-open runtime/display accessibility evidence." \
        "" \
        "$run_timeout"
    fi
    printf 'Xvfb is not available; wrote manual fallback checklist to %s\n' "$run_dir" >&2
    return 2
  fi

  if ! display=$(select_display); then
    write_environment "$run_dir"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      display-allocation-unavailable \
      "No free X display could be selected; no display exists for window enumeration." \
      "" \
      before-x-server-start \
      "" \
      "" \
      not-started
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
      "$xvfb_executable" \
      "" \
      not-attempted \
      "" \
      before-x-server-start \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        display-allocation-unavailable \
        "No free X display could be selected; no display exists for post-open runtime/display accessibility evidence." \
        "" \
        "$run_timeout"
    fi
    printf 'No free X display found; wrote manual fallback checklist to %s\n' "$run_dir" >&2
    return 2
  fi

  if ! python3 "$ROOT_DIRECTORY_PREP" \
      --repo-root "$REPO_ROOT" \
      --alice-cwd "$cwd" \
      --output "$run_dir/root-directory-prep.json" \
      --log "$run_dir/root-directory-prep.log"; then
    write_environment "$run_dir" "$display"
    write_checklist "$scenario_json" "$run_dir" >/dev/null
    if [ -f "$run_dir/root-directory-prep.json" ]; then
      root_directory_prep_status=$(inventory_json_field "$run_dir/root-directory-prep.json" status)
      root_directory_prep_blocker=$(inventory_json_field "$run_dir/root-directory-prep.json" blocker)
    else
      root_directory_prep_status=blocked
      root_directory_prep_blocker=root-directory-prep-script-failed
      cat > "$run_dir/root-directory-prep.json" <<'JSON'
{
  "blocker": "root-directory-prep-script-failed",
  "blockerDetail": "prepare-root-directory.py failed before writing root-directory-prep.json; inspect the runner stderr/stdout and retry after fixing the helper invocation.",
  "configuredRootDirectory": "",
  "distributionExists": false,
  "expectedRootDirectory": "../core/resources/target/distribution",
  "mavenPhase": "process-resources",
  "mavenProject": "core/resources",
  "prepAttempted": false,
  "resolvedRootDirectory": "",
  "rootDirectoryProperty": "org.alice.ide.rootDirectory",
  "status": "blocked"
}
JSON
    fi
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      "$root_directory_prep_blocker" \
      "Alice rootDirectory launch preparation failed before Xvfb start; inspect root-directory-prep.json and root-directory-prep.log." \
      "$display" \
      before-x-server-start \
      "" \
      "" \
      not-started
    write_controlled_display_pixel_observation \
      "$run_dir" \
      blocked \
      "$root_directory_prep_blocker" \
      "Alice rootDirectory launch preparation failed before Xvfb start; inspect root-directory-prep.json for the exact property, Maven phase, and distribution path." \
      "$display" \
      false \
      no-visible-pixel-proof \
      "" \
      not-attempted \
      not-started \
      not-attempted \
      "" \
      "$xvfb_executable" \
      "" \
      not-attempted \
      "" \
      before-x-server-start \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        "$root_directory_prep_blocker" \
        "Alice rootDirectory launch preparation failed before post-open runtime/display accessibility evidence could be collected."
    fi
    {
      printf 'scenario=%s\n' "$scenario_id"
      printf 'automationMode=%s\n' "$automation_mode"
      printf 'display=%s\n' "$display"
      printf 'rootDirectoryPrep=%s\n' root-directory-prep.json
      printf 'rootDirectoryPrepStatus=%s\n' "$root_directory_prep_status"
      printf 'rootDirectoryPrepBlocker=%s\n' "$root_directory_prep_blocker"
      printf 'readyStatus=not-attempted\n'
      printf 'processStatus=not-started\n'
      printf 'screenshotStatus=not-attempted\n'
      printf 'windowInventory=%s\n' x-window-inventory.json
      printf 'applicationRootError=not-attempted\n'
      if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
        printf 'outcome=blocked\n'
        printf 'runtimeDisplayAccessibilityEvidence=%s\n' "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
        printf 'runtimeDisplayAccessibilityStatus=blocked\n'
        printf 'runtimeDisplayAccessibilityBlocker=%s\n' "$root_directory_prep_blocker"
      fi
      printf 'timeoutSeconds=%s\n' "$run_timeout"
    } > "$run_dir/status.txt"
    printf 'Alice rootDirectory launch preparation blocked: %s; see %s/root-directory-prep.json\n' "$root_directory_prep_blocker" "$run_dir" >&2
    return 2
  fi
  root_directory_prep_status=$(inventory_json_field "$run_dir/root-directory-prep.json" status)
  root_directory_prep_blocker=$(inventory_json_field "$run_dir/root-directory-prep.json" blocker)

  local license_acceptance_status license_acceptance_blocker license_prefs_user_root license_jvm_option
  license_prefs_user_root="$(cd "$run_dir" && pwd)/java-user-prefs"
  license_jvm_option="-Djava.util.prefs.userRoot=$license_prefs_user_root"
  if [ "${ALICE_QA_ACCEPT_LICENSES_FOR_TESTS:-}" = "1" ]; then
    if ! python3 "$LICENSE_ACCEPTANCE_PREP" \
        --user-root "$license_prefs_user_root" \
        --output "$run_dir/license-acceptance.json" \
        --accept-for-tests; then
      write_environment "$run_dir" "$display"
      write_checklist "$scenario_json" "$run_dir" >/dev/null
      write_x_window_inventory \
        "$run_dir" \
        not-attempted \
        license-acceptance-prep-failed \
        "Alice first-run license acceptance prep failed before launch; inspect license-acceptance.json." \
        "$display" \
        before-alice-launch \
        "" \
        "" \
        not-started
      write_controlled_display_pixel_observation \
        "$run_dir" \
        blocked \
        license-acceptance-prep-failed \
        "Alice first-run license acceptance prep failed before launch; inspect license-acceptance.json." \
        "$display" \
        false \
        no-visible-pixel-proof \
        "" \
        not-attempted \
        not-started \
        not-attempted \
        "" \
        "$xvfb_executable" \
        "" \
        not-attempted \
        "" \
        before-alice-launch \
        not-attempted \
        x-window-inventory.json \
        0
      if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
        write_post_open_runtime_display_blocker \
          "$run_dir" \
          "$scenario_id" \
          "$automation_mode" \
          license-acceptance-prep-failed \
          "Alice first-run license acceptance prep failed before post-open runtime/display accessibility evidence could be collected." \
          "$display" \
          "$run_timeout"
      fi
      printf 'Alice license acceptance prep failed; see %s/license-acceptance.json\n' "$run_dir" >&2
      return 2
    fi
  else
    python3 "$LICENSE_ACCEPTANCE_PREP" \
      --user-root "$license_prefs_user_root" \
      --output "$run_dir/license-acceptance.json" >/dev/null 2>&1 || true
    license_jvm_option=
  fi
  license_acceptance_status=$(inventory_json_field "$run_dir/license-acceptance.json" status)
  license_acceptance_blocker=$(inventory_json_field "$run_dir/license-acceptance.json" blocker)

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
    write_x_window_inventory \
      "$run_dir" \
      not-attempted \
      x-server-start-failed \
      "Xvfb exited before Alice launch; no running X server exists for window enumeration." \
      "$display" \
      before-alice-launch \
      "" \
      "" \
      not-started
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
      "$xvfb_executable" \
      "" \
      not-attempted \
      "" \
      before-alice-launch \
      not-attempted \
      x-window-inventory.json \
      0
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      write_post_open_runtime_display_blocker \
        "$run_dir" \
        "$scenario_id" \
        "$automation_mode" \
        x-server-start-failed \
        "Xvfb exited before Alice launch; post-open runtime/display accessibility evidence could not be collected." \
        "$display" \
        "$run_timeout"
    fi
    printf 'Xvfb exited before Alice launch; see %s/xvfb.log\n' "$run_dir" >&2
    return 2
  fi

  export DISPLAY=$display
  write_environment "$run_dir" "$display"

  (
    cd "$resolved_cwd"
    if [ -n "$license_jvm_option" ]; then
      if [ -n "${MAVEN_OPTS:-}" ]; then
        export MAVEN_OPTS="${MAVEN_OPTS} ${license_jvm_option}"
      else
        export MAVEN_OPTS="$license_jvm_option"
      fi
      if [ -n "${JAVA_TOOL_OPTIONS:-}" ]; then
        export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} ${license_jvm_option}"
      else
        export JAVA_TOOL_OPTIONS="$license_jvm_option"
      fi
    fi
    timeout --foreground -k 10s "${run_timeout}s" "${argv[@]}" < /dev/null
  ) > "$run_dir/launch.log" 2>&1 &
  alice_pid=$!

  local ready_status=not-checked
  local waited=0
  if [ "${ALICE_QA_DISABLE_WINDOW_DETECTOR:-}" != "1" ] && [ -n "$xdotool_executable" ]; then
    ready_status=not-found
    while [ "$waited" -lt "$ready_wait" ]; do
      if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
        ready_status=process-exited
        break
      fi
      if "$xdotool_executable" search --onlyvisible --name Alice >/dev/null 2>&1 ||
          "$xdotool_executable" search --onlyvisible --class Alice >/dev/null 2>&1 ||
          "$xdotool_executable" search --onlyvisible --class alice >/dev/null 2>&1; then
        ready_status=alice-window-found
        break
      fi
      sleep 1
      waited=$((waited + 1))
    done
    if [ "$ready_status" = not-found ] &&
        kill -0 "$alice_pid" >/dev/null 2>&1 &&
        "$xdotool_executable" search --onlyvisible --class ".*" >/dev/null 2>&1; then
      ready_status=non-alice-visible-window-found
    fi
  else
    sleep "$ready_wait"
    ready_status=window-detector-unavailable
  fi

  local select_project_wait_status=not-requested
  if [ "$needs_select_project_wait" -eq 1 ]; then
    if [ "${ALICE_QA_DISABLE_WINDOW_DETECTOR:-}" != "1" ] && [ -n "$xdotool_executable" ]; then
      select_project_wait_status=not-found
      local select_waited=0
      while [ "$select_waited" -lt "$ready_wait" ]; do
        if ! kill -0 "$alice_pid" >/dev/null 2>&1; then
          select_project_wait_status=process-exited
          break
        fi
        if "$xdotool_executable" search --onlyvisible --name '^Select Project$' >/dev/null 2>&1; then
          select_project_wait_status=select-project-window-found
          break
        fi
        sleep 1
        select_waited=$((select_waited + 1))
      done
    else
      select_project_wait_status=window-detector-unavailable
    fi
  fi

  collect_x_window_inventory "$run_dir" "$display" after-readiness-wait "$alice_pid"
  write_application_root_error_probe "$run_dir/x-window-inventory.json" "$run_dir/application-root-error.json"
  write_license_dialog_probe "$run_dir/x-window-inventory.json" "$run_dir/license-dialog.json"
  write_select_project_probe "$run_dir/x-window-inventory.json" "$run_dir/select-project-window.json"
  local swing_widget_status=not-requested swing_widget_blocker=not-requested
  if [ "$scenario_id" = alice-desktop-select-project-widget-introspection ] || \
     [ "$scenario_id" = alice-desktop-select-project-atk-exec ]; then
    # Allow the Swing accessibility tree to build before probing.
    sleep 3
    write_swing_widget_probe "$run_dir/x-window-inventory.json" "$run_dir/swing-widget-observation.json"
    swing_widget_status=$(inventory_json_field "$run_dir/swing-widget-observation.json" status)
    swing_widget_blocker=$(inventory_json_field "$run_dir/swing-widget-observation.json" blocker)
  fi
  local tab_click_status=not-requested tab_click_blocker=not-requested
  if [ "$needs_tab_click_probe" -eq 1 ]; then
    # Allow the Swing accessibility tree to build before probing, then run
    # the tab structure diagnosis and click attempt.
    sleep 3
    write_tab_click_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/tab-click-observation.json" \
      "$target_starter_display_name" \
      "$target_starter_repo_path"
    tab_click_status=$(inventory_json_field "$run_dir/tab-click-observation.json" status)
    tab_click_blocker=$(inventory_json_field "$run_dir/tab-click-observation.json" blocker)
  fi
  local post_open_status=not-requested post_open_blocker=not-requested
  if [ "$scenario_id" = alice-desktop-post-project-open-window-state ] \
      || [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
    write_post_project_open_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/tab-click-observation.json" \
      "$run_dir/post-project-open-observation.json"
    post_open_status=$(inventory_json_field "$run_dir/post-project-open-observation.json" status)
    post_open_blocker=$(inventory_json_field "$run_dir/post-project-open-observation.json" blocker)
  fi
  local runtime_display_status=not-requested runtime_display_blocker=not-requested
  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
    write_post_open_runtime_display_probe \
      "$run_dir/x-window-inventory.json" \
      "$run_dir/post-project-open-observation.json" \
      "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" \
      "$run_dir/runtime-display-accessibility-status.txt" \
      "$scenario_id" \
      "$automation_mode"
    runtime_display_status=$(inventory_json_field "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" status)
    runtime_display_blocker=$(inventory_json_field "$run_dir/$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" blocker)
  fi
  local window_inventory_status alice_window_candidate_count application_root_error_status application_root_error_blocker license_dialog_status license_dialog_blocker select_project_status select_project_blocker select_project_interaction
  window_inventory_status=$(inventory_json_field "$run_dir/x-window-inventory.json" status)
  alice_window_candidate_count=$(inventory_json_field "$run_dir/x-window-inventory.json" aliceWindowCandidateCount)
  application_root_error_status=$(inventory_json_field "$run_dir/application-root-error.json" status)
  application_root_error_blocker=$(inventory_json_field "$run_dir/application-root-error.json" blocker)
  license_dialog_status=$(inventory_json_field "$run_dir/license-dialog.json" status)
  license_dialog_blocker=$(inventory_json_field "$run_dir/license-dialog.json" blocker)
  select_project_status=$(inventory_json_field "$run_dir/select-project-window.json" status)
  select_project_blocker=$(inventory_json_field "$run_dir/select-project-window.json" blocker)
  select_project_interaction=$(inventory_json_field "$run_dir/select-project-window.json" interactionProof)

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
    printf 'rootDirectoryPrep=%s\n' root-directory-prep.json
    printf 'rootDirectoryPrepStatus=%s\n' "$root_directory_prep_status"
    printf 'rootDirectoryPrepBlocker=%s\n' "$root_directory_prep_blocker"
    printf 'licenseAcceptance=%s\n' license-acceptance.json
    printf 'licenseAcceptanceStatus=%s\n' "$license_acceptance_status"
    printf 'licenseAcceptanceBlocker=%s\n' "$license_acceptance_blocker"
    printf 'windowInventory=%s\n' x-window-inventory.json
    printf 'windowInventoryStatus=%s\n' "$window_inventory_status"
    printf 'aliceWindowCandidateCount=%s\n' "$alice_window_candidate_count"
    printf 'applicationRootError=%s\n' application-root-error.json
    printf 'applicationRootErrorStatus=%s\n' "$application_root_error_status"
    printf 'applicationRootErrorBlocker=%s\n' "$application_root_error_blocker"
    printf 'licenseDialog=%s\n' license-dialog.json
    printf 'licenseDialogStatus=%s\n' "$license_dialog_status"
    printf 'licenseDialogBlocker=%s\n' "$license_dialog_blocker"
    printf 'selectProjectWindow=%s\n' select-project-window.json
    printf 'selectProjectStatus=%s\n' "$select_project_status"
    printf 'selectProjectBlocker=%s\n' "$select_project_blocker"
    printf 'selectProjectInteraction=%s\n' "$select_project_interaction"
    printf 'selectProjectWaitStatus=%s\n' "$select_project_wait_status"
    printf 'swingWidgetObservation=%s\n' swing-widget-observation.json
    printf 'swingWidgetStatus=%s\n' "$swing_widget_status"
    printf 'swingWidgetBlocker=%s\n' "$swing_widget_blocker"
    printf 'tabClickObservation=%s\n' tab-click-observation.json
    printf 'tabClickStatus=%s\n' "$tab_click_status"
    printf 'tabClickBlocker=%s\n' "$tab_click_blocker"
    printf 'postProjectOpenObservation=%s\n' post-project-open-observation.json
    printf 'postProjectOpenStatus=%s\n' "$post_open_status"
    printf 'postProjectOpenBlocker=%s\n' "$post_open_blocker"
    if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
      printf 'runtimeDisplayAccessibilityEvidence=%s\n' "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT"
    else
      printf 'runtimeDisplayAccessibilityEvidence=not-requested\n'
    fi
    printf 'runtimeDisplayAccessibilityStatus=%s\n' "$runtime_display_status"
    printf 'runtimeDisplayAccessibilityBlocker=%s\n' "$runtime_display_blocker"
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
  elif [ "$application_root_error_status" = observed ]; then
    observation_status=blocked
    observation_blocker="$application_root_error_blocker"
    observation_detail="A Java window titled Application Root Error was observed before any Alice desktop candidate; inspect application-root-error.json for exact expected dialog text and next invocation change."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$license_dialog_status" = observed ]; then
    observation_status=blocked
    observation_blocker="$license_dialog_blocker"
    observation_detail="A first-run License Agreement dialog was observed; inspect license-dialog.json for exact title, controls, and the isolated test-only Java Preferences bypass."
    pixels_observed=false
    observation_claim=no-visible-pixel-proof
  elif [ "$ready_status" != alice-window-found ]; then
    observation_status=blocked
    observation_blocker=alice-window-not-found
    observation_detail="Xvfb was reachable, but xdotool did not find a visible Alice window before screenshot capture; readyStatus=$ready_status. Inspect x-window-inventory.json for exact Alice-related visible X window title/class/process/geometry."
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
    "$screenshot_pixel_detail" \
    after-readiness-wait \
    "$window_inventory_status" \
    x-window-inventory.json \
    "$alice_window_candidate_count"

  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ]; then
    scenario_outcome=blocked
    if [ "$observation_status" = observed ] && [ "$runtime_display_status" = observed ]; then
      scenario_outcome=passed
    fi
    {
      cat "$run_dir/status.txt"
      printf 'outcome=%s\n' "$scenario_outcome"
      printf 'controlledDisplayPixelStatus=%s\n' "$observation_status"
      printf 'controlledDisplayPixelBlocker=%s\n' "$observation_blocker"
    } > "$run_dir/status.txt.tmp"
    mv "$run_dir/status.txt.tmp" "$run_dir/status.txt"
  fi

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
  if [ "$scenario_id" = "$POST_OPEN_RUNTIME_DISPLAY_SCENARIO" ] && [ "$runtime_display_status" != observed ]; then
    printf 'Post-open runtime/display accessibility evidence blocked: %s; see %s/%s\n' "$runtime_display_blocker" "$run_dir" "$POST_OPEN_RUNTIME_DISPLAY_ARTIFACT" >&2
    return 2
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
    timeout --foreground -k 10s "${run_timeout}s" "${argv[@]}" < /dev/null
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
