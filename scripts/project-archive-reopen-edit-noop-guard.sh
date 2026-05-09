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
        echo "error: --allow-noop-evidence requires an evidence file path" >&2
        usage
        exit 64
      fi
      allow_noop_evidence_file="$2"
      shift
      ;;
    --expected-head)
      if [[ $# -lt 2 ]]; then
        echo "error: --expected-head requires a 40-character commit SHA" >&2
        usage
        exit 64
      fi
      expected_head="$2"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      exit 64
      ;;
  esac
  shift
done

if [[ -n "$allow_noop_evidence_file" && -z "$expected_head" ]]; then
  echo "error: --expected-head is required when --allow-noop-evidence is used" >&2
  exit 64
fi

if [[ -z "$allow_noop_evidence_file" && -n "$expected_head" ]]; then
  echo "error: --expected-head requires --allow-noop-evidence" >&2
  exit 64
fi

if [[ -n "$expected_head" && ! "$expected_head" =~ ^[0-9a-fA-F]{40}$ ]]; then
  echo "error: expected head must be a 40-character commit SHA: $expected_head" >&2
  exit 64
fi

if [[ ! -e "$candidate_path" ]]; then
  echo "error: candidate path is not inside a git worktree: $candidate_path" >&2
  exit 2
fi

if ! repo_root="$(git -C "$candidate_path" rev-parse --show-toplevel 2>/dev/null)"; then
  echo "error: candidate path is not inside a git worktree: $candidate_path" >&2
  exit 2
fi

repo_root="$(cd "$repo_root" && pwd -P)"

if [[ "$print_root" == true ]]; then
  printf '%s\n' "$repo_root"
  exit 0
fi

if [[ -n "$expected_head" ]]; then
  actual_head="$(git -C "$repo_root" rev-parse HEAD)"
  if [[ "$actual_head" != "$expected_head" ]]; then
    echo "error: expected head $expected_head does not match worktree HEAD $actual_head" >&2
    exit 1
  fi
fi

if [[ -n "$(git -C "$repo_root" status --short)" ]]; then
  exit 0
fi

if [[ -n "$allow_noop_evidence_file" ]]; then
  if [[ ! -f "$allow_noop_evidence_file" ]]; then
    echo "error: no-op evidence file is missing: $allow_noop_evidence_file" >&2
    exit 1
  fi

  if ! grep -Fq "No-op justification:" "$allow_noop_evidence_file"; then
    echo "error: no-op evidence must include a No-op justification section" >&2
    exit 1
  fi

  if grep -Fq "Files modified:" "$allow_noop_evidence_file"; then
    echo "error: clean no-op evidence must not also list modified files" >&2
    exit 1
  fi

  if ! grep -Fq "PR head: $expected_head" "$allow_noop_evidence_file" \
    || ! grep -Fq "Local HEAD: $expected_head" "$allow_noop_evidence_file" \
    || ! grep -Fq "$expected_head" "$allow_noop_evidence_file"; then
    echo "error: stale no-op evidence for expected head $expected_head" >&2
    exit 1
  fi

  if ! grep -A8 -F "No-op justification:" "$allow_noop_evidence_file" | grep -Fq "$expected_head"; then
    echo "error: no-op justification must reference expected head $expected_head" >&2
    exit 1
  fi

  exit 0
fi

echo "error: no project archive reopen/edit changes found in git worktree: $repo_root" >&2
exit 1
