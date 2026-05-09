#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: project-archive-reopen-edit-noop-guard.sh [candidate-path] [--print-root]
       project-archive-reopen-edit-noop-guard.sh [candidate-path] --allow-noop-evidence evidence-file --expected-head sha

Resolves candidate-path through git rev-parse --show-toplevel and evaluates the
actual linked worktree root. Without --print-root, exits non-zero when that
worktree has no uncommitted changes unless exact-head no-op evidence is supplied.
USAGE
}

fail() {
  local code="$1"
  shift
  echo "error: $*" >&2
  exit "$code"
}

fail_usage() {
  local code="$1"
  shift
  echo "error: $*" >&2
  usage
  exit "$code"
}

candidate_path="."
allow_noop_evidence_file=""
expected_head=""
if [[ $# -gt 0 && "$1" != --* ]]; then
  candidate_path="$1"
  shift
fi

print_root=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --print-root)
      print_root=true
      ;;
    --allow-noop-evidence)
      if [[ $# -lt 2 ]]; then
        fail_usage 64 "--allow-noop-evidence requires an evidence file path"
      fi
      allow_noop_evidence_file="$2"
      shift
      ;;
    --expected-head)
      if [[ $# -lt 2 ]]; then
        fail_usage 64 "--expected-head requires a 40-character commit SHA"
      fi
      expected_head="$2"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail_usage 64 "unknown argument: $1"
      ;;
  esac
  shift
done

if [[ -n "$allow_noop_evidence_file" && -z "$expected_head" ]]; then
  fail 64 "--expected-head is required when --allow-noop-evidence is used"
fi

if [[ -z "$allow_noop_evidence_file" && -n "$expected_head" ]]; then
  fail 64 "--expected-head requires --allow-noop-evidence"
fi

if [[ -n "$expected_head" && ! "$expected_head" =~ ^[0-9a-fA-F]{40}$ ]]; then
  fail 64 "expected head must be a 40-character commit SHA: $expected_head"
fi

if ! repo_root="$(git -C "$candidate_path" rev-parse --show-toplevel 2>/dev/null)"; then
  fail 2 "candidate path is not inside a git worktree: $candidate_path"
fi

repo_root="$(cd "$repo_root" && pwd -P)"

if [[ "$print_root" == true ]]; then
  printf '%s\n' "$repo_root"
  exit 0
fi

if [[ -n "$expected_head" ]]; then
  actual_head="$(git -C "$repo_root" rev-parse HEAD)"
  if [[ "$actual_head" != "$expected_head" ]]; then
    fail 1 "expected head $expected_head does not match worktree HEAD $actual_head"
  fi
fi

if [[ -n "$(git -C "$repo_root" status --short)" ]]; then
  exit 0
fi

if [[ -n "$allow_noop_evidence_file" ]]; then
  if [[ ! -f "$allow_noop_evidence_file" ]]; then
    fail 1 "no-op evidence file is missing: $allow_noop_evidence_file"
  fi

  evidence_text="$(tr '[:upper:]' '[:lower:]' < "$allow_noop_evidence_file")"

  grep -Fq "No-op justification:" "$allow_noop_evidence_file" \
    || fail 1 "no-op evidence must include a No-op justification section"

  if grep -Fq "Files modified:" "$allow_noop_evidence_file"; then
    fail 1 "clean no-op evidence must not also list modified files"
  fi

  grep -Fq "PR head: $expected_head" "$allow_noop_evidence_file" \
    || fail 1 "stale no-op evidence for expected head $expected_head"
  grep -Fq "Local HEAD: $expected_head" "$allow_noop_evidence_file" \
    || fail 1 "stale no-op evidence for expected head $expected_head"

  if ! grep -A8 -F "No-op justification:" "$allow_noop_evidence_file" | grep -Fq "$expected_head"; then
    fail 1 "no-op justification must reference expected head $expected_head"
  fi

  grep -Fq "Scope exclusions:" "$allow_noop_evidence_file" \
    || fail 1 "no-op evidence must include scope exclusions"
  grep -Fq "Positive claim scope:" "$allow_noop_evidence_file" \
    || fail 1 "no-op evidence must include positive claim scope"
  grep -Fq "Stale evidence note:" "$allow_noop_evidence_file" \
    || fail 1 "no-op evidence must include a stale evidence note"

  for required_exclusion in \
    "full desktop lesson automation" \
    "visible rendering correctness" \
    "grading" \
    "full save completion"; do
    if [[ "$evidence_text" != *"$required_exclusion"* ]]; then
      fail 1 "no-op evidence scope exclusions must mention $required_exclusion"
    fi
  done

  if grep -Eiq '^[[:space:]]*(full desktop lesson automation|visible rendering correctness|grading workflow|grading|full save completion)[[:space:]]*:[[:space:]]*(proven|validated|supported|complete|passed|ready)' "$allow_noop_evidence_file"; then
    fail 1 "no-op evidence contains out-of-scope desktop, rendering, grading, or full Save claims"
  fi

  exit 0
fi

fail 1 "no project archive reopen/edit changes found in git worktree: $repo_root"
