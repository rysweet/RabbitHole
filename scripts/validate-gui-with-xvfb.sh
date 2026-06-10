#!/usr/bin/env bash
set -euo pipefail

DEFAULT_TIMEOUT_SECONDS=60
XVFB_SCREEN_ARGS="-screen 0 1024x768x24 -ac"

usage() {
  cat <<'USAGE'
Usage: ./scripts/validate-gui-with-xvfb.sh [OPTIONS] -- COMMAND [ARG...]

Runs a GUI validation command under xvfb-run with the repository's standard
virtual display settings and bounded execution.

Options:
  --timeout-seconds N   Maximum command runtime in seconds. Defaults to
                        RABBITHOLE_LAUNCH_TIMEOUT_SECONDS or 60.
  --expect success     Expect the wrapped command to exit 0. This is the default.
  --expect failure     Expect the wrapped command to exit non-zero.
  --xvfb-run PATH      xvfb-run executable to use. Defaults to xvfb-run on PATH.
  --help               Show this usage text.
USAGE
}

error() {
  printf '[xvfb-gui] ERROR: %s\n' "$*" >&2
}

usage_error() {
  error "$*"
  usage >&2
  exit 2
}

resolve_executable() {
  local executable="$1"

  if [[ "${executable}" == */* ]]; then
    [[ -x "${executable}" ]] || return 1
    printf '%s\n' "${executable}"
    return 0
  fi

  command -v "${executable}"
}

timeout_seconds="${RABBITHOLE_LAUNCH_TIMEOUT_SECONDS:-${DEFAULT_TIMEOUT_SECONDS}}"
expected_result="success"
xvfb_run=""

while (($# > 0)); do
  case "$1" in
    --timeout-seconds)
      (($# >= 2)) || usage_error "--timeout-seconds requires a value."
      timeout_seconds="$2"
      shift 2
      ;;
    --expect)
      (($# >= 2)) || usage_error "--expect requires a value."
      expected_result="$2"
      shift 2
      ;;
    --xvfb-run)
      (($# >= 2)) || usage_error "--xvfb-run requires a value."
      xvfb_run="$2"
      shift 2
      ;;
    --help)
      usage
      exit 0
      ;;
    --)
      shift
      break
      ;;
    --*)
      usage_error "Unknown option: $1"
      ;;
    *)
      usage_error "Command must be separated from harness options with --."
      ;;
  esac
done

(($# > 0)) || usage_error "Missing command after --."

if [[ ! "${timeout_seconds}" =~ ^[0-9]+$ ]] || ((timeout_seconds < 1)); then
  error "--timeout-seconds must be a positive integer."
  exit 2
fi

case "${expected_result}" in
  success|failure)
    ;;
  *)
    error "--expect must be either 'success' or 'failure'."
    exit 2
    ;;
esac

if ! command -v timeout >/dev/null 2>&1; then
  error "Required command 'timeout' was not found on PATH."
  exit 127
fi

if [[ -z "${xvfb_run}" ]]; then
  xvfb_run="xvfb-run"
fi

if ! xvfb_run="$(resolve_executable "${xvfb_run}")"; then
  error "Required command 'xvfb-run' was not found or is not executable."
  exit 127
fi

status=0
timeout "${timeout_seconds}s" \
  "${xvfb_run}" --auto-servernum -s "${XVFB_SCREEN_ARGS}" "$@" || status=$?

if [[ "${status}" == "124" ]]; then
  error "Command timed out after ${timeout_seconds} seconds."
  exit 124
fi

case "${expected_result}:${status}" in
  success:0)
    exit 0
    ;;
  success:*)
    error "Wrapped command failed with exit status ${status}; expected success."
    exit "${status}"
    ;;
  failure:0)
    error "Wrapped command succeeded; expected failure."
    exit 1
    ;;
  failure:*)
    exit 0
    ;;
esac
