#!/usr/bin/env bash
set -euo pipefail

EXPECTED_HEADLESS_GUI_MESSAGE="Alice desktop launch requires a graphical environment."
SUBMODULE_FIX_COMMAND="git submodule update --init tweedle-lang"
DEFAULT_LAUNCH_TIMEOUT_SECONDS=60
MAX_LAUNCH_TIMEOUT_SECONDS=600
LAUNCH_TIMEOUT_SECONDS="${RABBITHOLE_LAUNCH_TIMEOUT_SECONDS:-${DEFAULT_LAUNCH_TIMEOUT_SECONDS}}"
MAVEN_RETRY_ATTEMPTS="${RABBITHOLE_MAVEN_RETRY_ATTEMPTS:-3}"

TEMP_PATHS=()
trap 'rm -rf "${TEMP_PATHS[@]}"' EXIT

usage() {
  cat <<'USAGE'
Usage: ./scripts/validate-getting-started.sh [--headless|--gui|--all|--help]

Validates the documented RabbitHole Getting Started flow for this checkout.

Modes:
  --headless  CI-safe default. Checks Git/submodule setup, runs the no-Sims
              Maven test lane, then verifies the no-Sims launch reaches the
              expected GUI-required boundary in headless mode.
  --gui       GUI lane. Requires a real or Xvfb graphical environment and
              fails when GUI validation is unavailable or blocked.
  --all       Runs --headless first, then attempts --gui when supported. GUI
              unavailability or the macOS Apple Silicon blocker is
              reported as skipped/blocked after headless validation passes.
  --help      Show this usage text.

Submodule fix:
  git submodule update --init tweedle-lang
USAGE
}

info() {
  printf '[getting-started] %s\n' "$*"
}

fail() {
  printf '[getting-started] ERROR: %s\n' "$*" >&2
  exit 1
}

usage_error() {
  printf '[getting-started] ERROR: Unknown option: %s\n' "$1" >&2
  usage >&2
  exit 2
}

add_temp_path() {
  TEMP_PATHS+=("$1")
}

require_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    fail "Required command '${command_name}' was not found on PATH."
  fi
}

resolve_repo_root() {
  local repo_root
  if ! repo_root="$(git rev-parse --show-toplevel 2>/dev/null)"; then
    fail "This validator must be run from inside a Git checkout."
  fi
  cd "${repo_root}"
  info "Using repository root: ${repo_root}"
}

require_tweedle_submodule() {
  if [[ ! -e tweedle-lang ]]; then
    fail "tweedle-lang submodule is missing or not initialized. Run: ${SUBMODULE_FIX_COMMAND}"
  fi
  if [[ ! -d tweedle-lang/Grammar ]]; then
    fail "tweedle-lang submodule is not initialized or missing tweedle-lang/Grammar. Run: ${SUBMODULE_FIX_COMMAND}"
  fi
  if [[ ! -f tweedle-lang/Grammar/TweedleLexer.g4 || ! -f tweedle-lang/Grammar/TweedleParser.g4 ]]; then
    fail "tweedle-lang/Grammar is incomplete. Run: ${SUBMODULE_FIX_COMMAND}"
  fi
}

require_common_prerequisites() {
  require_command git
  resolve_repo_root
  require_tweedle_submodule
  require_command java
  require_command mvn
}

validate_launch_timeout_seconds() {
  if [[ ! "${LAUNCH_TIMEOUT_SECONDS}" =~ ^[0-9]+$ ]]; then
    fail "RABBITHOLE_LAUNCH_TIMEOUT_SECONDS must be an integer from 1 to ${MAX_LAUNCH_TIMEOUT_SECONDS}."
  fi
  if (( LAUNCH_TIMEOUT_SECONDS < 1 || LAUNCH_TIMEOUT_SECONDS > MAX_LAUNCH_TIMEOUT_SECONDS )); then
    fail "RABBITHOLE_LAUNCH_TIMEOUT_SECONDS must be an integer from 1 to ${MAX_LAUNCH_TIMEOUT_SECONDS}."
  fi
}

validate_maven_retry_attempts() {
  if [[ ! "${MAVEN_RETRY_ATTEMPTS}" =~ ^[0-9]+$ ]]; then
    fail "RABBITHOLE_MAVEN_RETRY_ATTEMPTS must be an integer from 1 to 5."
  fi
  if (( MAVEN_RETRY_ATTEMPTS < 1 || MAVEN_RETRY_ATTEMPTS > 5 )); then
    fail "RABBITHOLE_MAVEN_RETRY_ATTEMPTS must be an integer from 1 to 5."
  fi
}

print_command() {
  printf '[getting-started] Running:'
  printf ' %q' "$@"
  printf '\n'
}

run_maven_with_retries() {
  local attempt
  local status

  for attempt in $(seq 1 "${MAVEN_RETRY_ATTEMPTS}"); do
    print_command "$@"
    if "$@"; then
      return 0
    else
      status=$?
    fi
    if (( attempt == MAVEN_RETRY_ATTEMPTS )); then
      return "${status}"
    fi
    info "Maven command failed with exit status ${status}; retrying (${attempt}/${MAVEN_RETRY_ATTEMPTS})."
    sleep $((attempt * 10))
  done
}

process_tree_pids() {
  local root_pid="$1"
  local child_pid

  printf '%s\n' "${root_pid}"
  for child_pid in $(pgrep -P "${root_pid}" 2>/dev/null || true); do
    process_tree_pids "${child_pid}"
  done
}

send_signal_to_pids() {
  local signal="$1"
  shift
  local pid

  for pid in "$@"; do
    kill "-${signal}" "${pid}" 2>/dev/null || true
  done
}

any_pid_alive() {
  local pid

  for pid in "$@"; do
    kill -0 "${pid}" 2>/dev/null && return 0
  done
  return 1
}

run_with_timeout() {
  local output_file="$1"
  local timeout_seconds="$2"
  local work_dir="$3"
  shift 3

  (
    cd "${work_dir}"
    "$@"
  ) >"${output_file}" 2>&1 &

  local command_pid=$!
  local elapsed_seconds=0
  while kill -0 "${command_pid}" 2>/dev/null; do
    if (( elapsed_seconds >= timeout_seconds )); then
      local timed_out_pids=()
      local timed_out_pid
      while IFS= read -r timed_out_pid; do
        timed_out_pids+=("${timed_out_pid}")
      done < <(process_tree_pids "${command_pid}")
      send_signal_to_pids TERM "${timed_out_pids[@]}"
      local grace_seconds=5
      while (( grace_seconds > 0 )) && any_pid_alive "${timed_out_pids[@]}"; do
        sleep 1
        grace_seconds=$((grace_seconds - 1))
      done
      send_signal_to_pids KILL "${timed_out_pids[@]}"
      wait "${command_pid}" 2>/dev/null || true
      return 124
    fi
    sleep 1
    elapsed_seconds=$((elapsed_seconds + 1))
  done

  wait "${command_pid}"
}

show_captured_output_tail() {
  local output_file="$1"
  if [[ -s "${output_file}" ]]; then
    printf '[getting-started] Captured launch output:\n' >&2
    tail -n 80 "${output_file}" >&2
  else
    printf '[getting-started] Captured launch output was empty.\n' >&2
  fi
}

run_headless_lane() {
  local mvn_cmd=(
    mvn
    -DincludeSims=false
    -Dinstall4j.skip
    -Dcheckstyle.skip
    -Djava.awt.headless=true
    clean
    install
  )
  local headless_launch_maven=(
    mvn
    -DincludeSims=false
    -Djava.awt.headless=true
    exec:java
    -Dalice-ide
  )
  local launch_output

  info "Running headless no-Sims Maven validation."
  run_maven_with_retries "${mvn_cmd[@]}"

  launch_output="$(mktemp)"
  add_temp_path "${launch_output}"

  info "Probing no-Sims Alice launch in headless mode."
  print_command "${headless_launch_maven[@]}"
  local launch_status=0
  run_with_timeout "${launch_output}" "${LAUNCH_TIMEOUT_SECONDS}" "alice-ide" "${headless_launch_maven[@]}" || launch_status=$?
  if [[ "${launch_status}" == "0" ]]; then
    show_captured_output_tail "${launch_output}"
    fail "Unexpected successful launch in headless mode; expected GUI-required boundary: ${EXPECTED_HEADLESS_GUI_MESSAGE}"
  fi

  if [[ "${launch_status}" == "124" ]]; then
    show_captured_output_tail "${launch_output}"
    fail "Headless launch probe timed out; expected GUI-required boundary: ${EXPECTED_HEADLESS_GUI_MESSAGE}"
  fi

  if grep -Fq "${EXPECTED_HEADLESS_GUI_MESSAGE}" "${launch_output}"; then
    info "Headless launch reached expected GUI-required boundary."
  else
    show_captured_output_tail "${launch_output}"
    fail "Unexpected launch failure; expected GUI-required boundary: ${EXPECTED_HEADLESS_GUI_MESSAGE}"
  fi
}

is_macos_apple_silicon() {
  [[ "$(uname -s)" == "Darwin" ]] || return 1
  case "$(uname -m)" in
    arm64|aarch64)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

check_gui_capability() {
  if is_macos_apple_silicon; then
    printf 'macOS Apple Silicon desktop GUI launch is blocked by a known RabbitHole platform guard.'
    return 2
  fi

  case "$(uname -s)" in
    Linux)
      if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
        printf 'No graphical environment detected: DISPLAY and WAYLAND_DISPLAY are unset.'
        return 1
      fi
      ;;
  esac

  local awt_check_dir
  awt_check_dir="$(mktemp -d)"
  add_temp_path "${awt_check_dir}"
  cat >"${awt_check_dir}/AwtDisplayCheck.java" <<'JAVA'
 import java.awt.GraphicsEnvironment;

 public final class AwtDisplayCheck {
   public static void main(String[] args) {
     if (GraphicsEnvironment.isHeadless()) {
       System.err.println("No graphical environment detected: Java AWT reports headless.");
       System.exit(1);
     }
   }
  }
JAVA

  local awt_check_output
  if ! awt_check_output="$(java -Djava.awt.headless=false "${awt_check_dir}/AwtDisplayCheck.java" 2>&1)"; then
    if [[ "${awt_check_output}" == *"No graphical environment detected"* ]]; then
      printf 'No graphical environment detected: Java AWT reports headless.'
    else
      printf 'Unable to run Java AWT display check with java source-file mode. Output: %s' "${awt_check_output:-<empty>}"
    fi
    return 1
  fi
}

run_gui_launch() {
  local gui_launch_maven=(
    mvn
    -DincludeSims=false
    -Djava.awt.headless=false
    exec:java
    -Dalice-ide
  )
  local launch_output

  launch_output="$(mktemp)"
  add_temp_path "${launch_output}"

  info "Launching Alice no-Sims GUI path. The probe succeeds if the process starts and remains alive for ${LAUNCH_TIMEOUT_SECONDS}s."
  print_command "${gui_launch_maven[@]}"
  local launch_status=0
  run_with_timeout "${launch_output}" "${LAUNCH_TIMEOUT_SECONDS}" "alice-ide" "${gui_launch_maven[@]}" || launch_status=$?
  if [[ "${launch_status}" == "0" ]]; then
    info "GUI launch command exited cleanly."
    return 0
  fi

  if [[ "${launch_status}" == "124" ]]; then
    info "GUI launch stayed alive through the startup probe; stopping the validation process."
    return 0
  fi

  show_captured_output_tail "${launch_output}"
  fail "GUI launch failed unexpectedly."
}

run_gui_maven_validation() {
  local mvn_cmd=(
    mvn
    -DincludeSims=false
    -Dinstall4j.skip
    -Dcheckstyle.skip
    -DskipTests
    -Djava.awt.headless=false
    clean
    install
  )

  info "Running GUI no-Sims Maven validation."
  run_maven_with_retries "${mvn_cmd[@]}"
}

run_gui_lane() {
  info "Checking desktop GUI capability for requested GUI validation."
  local gui_status=0
  local gui_message
  gui_message="$(check_gui_capability)" || gui_status=$?
  if [[ "${gui_status}" == "2" ]]; then
    fail "${gui_message}"
  fi
  if [[ "${gui_status}" != "0" ]]; then
    fail "GUI validation was explicitly requested but is unavailable. ${gui_message}"
  fi

  run_gui_maven_validation
  run_gui_launch
}

run_all_lane() {
  run_headless_lane

  info "Checking whether GUI validation can run after headless validation."
  local gui_status=0
  local gui_message
  gui_message="$(check_gui_capability)" || gui_status=$?
  if [[ "${gui_status}" == "2" ]]; then
    info "BLOCKED: ${gui_message}"
    return 0
  fi
  if [[ "${gui_status}" != "0" ]]; then
    info "SKIP: GUI validation unavailable in --all mode. ${gui_message}"
    return 0
  fi

  run_gui_maven_validation
  run_gui_launch
}

mode="headless"
if (( "$#" > 1 )); then
  usage_error "$*"
fi

if (( "$#" == 1 )); then
  case "$1" in
    --headless)
      mode="headless"
      ;;
    --gui)
      mode="gui"
      ;;
    --all)
      mode="all"
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      usage_error "$1"
      ;;
  esac
fi

require_common_prerequisites
validate_launch_timeout_seconds
validate_maven_retry_attempts

case "${mode}" in
  headless)
    run_headless_lane
    ;;
  gui)
    run_gui_lane
    ;;
  all)
    run_all_lane
    ;;
esac

info "Getting Started validation completed."
