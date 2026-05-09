#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: project-archive-reopen-edit-noop-guard.sh [candidate-path] [--print-root]
       project-archive-reopen-edit-noop-guard.sh [candidate-path] --allow-noop-evidence evidence-file --expected-head sha

Resolves candidate-path through git rev-parse --show-toplevel and evaluates the
actual linked worktree root. Without --print-root, exits non-zero when that
worktree has no scoped recovery changes unless exact-head no-op evidence is supplied.
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

is_recovery_scope_path() {
  local path="$1"
  case "$path" in
    core/story-api-migration/src/main/java/org/lgna/project/io/*|\
    core/story-api-migration/src/test/java/org/lgna/project/io/*|\
    core/ide/src/test/java/org/alice/ide/ProjectOpenSaveExportJourneyTest.java|\
    docs/howto/characterize-project-io-corpus.md|\
    docs/howto/characterize-project-save-export-operations.md|\
    docs/howto/validate-project-archive-reopen-edit-seam.md|\
    docs/index.md|\
    docs/reference/alice-desktop-outside-in-qa.md|\
    docs/reference/pr-402-reopen-edit-recovery-output-contract.md|\
    docs/reference/project-archive-reopen-edit-seam.md|\
    docs/reference/project-io-corpus-characterization.md|\
    docs/reference/project-save-export-operations.md|\
    docs/tutorials/project-io-corpus-characterization.md|\
    docs/tutorials/trace-project-archive-reopen-edit-seam.md|\
    pyproject.toml|\
    qa/outside-in/alice-desktop/scenarios/project-io-smoke.yaml|\
    scripts/project-archive-reopen-edit-noop-guard.sh|\
    tests/test_project_archive_reopen_edit_noop_guard.py)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
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

worktree_status="$(git -C "$repo_root" status --short)"
if [[ -n "$worktree_status" ]]; then
  unscoped_changes=()
  while IFS= read -r status_line; do
    [[ -z "$status_line" ]] && continue
    status_path="${status_line:3}"
    if [[ "$status_path" == *" -> "* ]]; then
      old_path="${status_path%% -> *}"
      new_path="${status_path##* -> }"
      if ! is_recovery_scope_path "$old_path" || ! is_recovery_scope_path "$new_path"; then
        unscoped_changes+=("$status_line")
      fi
    elif ! is_recovery_scope_path "$status_path"; then
      unscoped_changes+=("$status_line")
    fi
  done <<< "$worktree_status"

  if [[ "${#unscoped_changes[@]}" -gt 0 ]]; then
    fail 1 "uncommitted changes include paths outside project archive reopen/edit recovery scope: ${unscoped_changes[*]}"
  fi

  exit 0
fi

if [[ -n "$allow_noop_evidence_file" ]]; then
  if [[ ! -f "$allow_noop_evidence_file" ]]; then
    fail 1 "no-op evidence file is missing: $allow_noop_evidence_file"
  fi

  evidence_text="$(< "$allow_noop_evidence_file")"
  evidence_text_lower="$(printf '%s' "$evidence_text" | tr '[:upper:]' '[:lower:]')"

  [[ "$evidence_text" == *"No-op justification:"* ]] \
    || fail 1 "no-op evidence must include a No-op justification section"

  if [[ "$evidence_text" == *"Files modified:"* ]]; then
    fail 1 "clean no-op evidence must not also list modified files"
  fi

  [[ "$evidence_text" == *"PR head: $expected_head"* ]] \
    || fail 1 "stale no-op evidence for expected head $expected_head"
  [[ "$evidence_text" == *"Local HEAD: $expected_head"* ]] \
    || fail 1 "stale no-op evidence for expected head $expected_head"
  [[ "$evidence_text" == *"Remote branch HEAD: $expected_head"* ]] \
    || fail 1 "stale no-op evidence for expected head $expected_head"

  if ! grep -A8 -F "No-op justification:" <<< "$evidence_text" | grep -Fq "$expected_head"; then
    fail 1 "no-op justification must reference expected head $expected_head"
  fi

  [[ "$evidence_text" == *"Scope exclusions:"* ]] \
    || fail 1 "no-op evidence must include scope exclusions"
  [[ "$evidence_text" == *"Positive claim scope:"* ]] \
    || fail 1 "no-op evidence must include positive claim scope"
  [[ "$evidence_text" == *"Stale evidence note:"* ]] \
    || fail 1 "no-op evidence must include a stale evidence note"

  missing_checks=()
  for required_check in \
    "gitguardian security checks" \
    "alice checkstyle ci/build (pull_request)" \
    "alice coverage reports/coverage (pull_request)" \
    "alice netbeans package ci/package-netbeans (pull_request)" \
    "alice test ci/test (pull_request)"; do
    if ! grep -Fqi "$required_check successful at current pr head" <<< "$evidence_text"; then
      missing_checks+=("$required_check")
    fi
  done
  if [[ "${#missing_checks[@]}" -gt 0 ]]; then
    fail 1 "no-op evidence checks must mark ${missing_checks[*]} successful at current PR head"
  fi

  missing_exclusions=()
  for required_exclusion in \
    "full desktop lesson automation" \
    "full ui automation" \
    "desktop save-menu completion" \
    "visible rendering correctness" \
    "grading" \
    "full save completion" \
    "full first-lesson completion" \
    "player runtime behavior"; do
    if [[ "$evidence_text_lower" != *"$required_exclusion"* ]]; then
      missing_exclusions+=("$required_exclusion")
    fi
  done
  if [[ "${#missing_exclusions[@]}" -gt 0 ]]; then
    fail 1 "no-op evidence scope exclusions must mention ${missing_exclusions[*]}"
  fi

  if grep -Eiq '^[[:space:]]*(full desktop lesson automation|full ui automation|desktop save-menu completion|save dialog completion|visible rendering correctness|grading workflow|grading|full save completion|full first-lesson completion|lesson completion|player runtime behavior)[[:space:]]*:[[:space:]]*(proven|validated|supported|complete|passed|ready)' <<< "$evidence_text"; then
    fail 1 "no-op evidence contains out-of-scope desktop, rendering, grading, Save, lesson, or player claims"
  fi

  exit 0
fi

fail 1 "no project archive reopen/edit changes found in git worktree: $repo_root"
