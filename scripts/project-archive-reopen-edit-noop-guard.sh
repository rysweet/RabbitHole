#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: project-archive-reopen-edit-noop-guard.sh [candidate-path] [--print-root]

Resolves candidate-path through git rev-parse --show-toplevel and evaluates the
actual linked worktree root. Without --print-root, exits non-zero when that
worktree has no uncommitted changes.
USAGE
}

candidate_path="."
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

if [[ -n "$(git -C "$repo_root" status --short)" ]]; then
  exit 0
fi

echo "error: no project archive reopen/edit changes found in git worktree: $repo_root" >&2
exit 1
