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

require_evidence_contains() {
  local needle="$1"
  local message="$2"
  if [[ "$evidence_text" != *"$needle"* ]]; then
    fail 1 "$message"
  fi
}

case_insensitive_contains() {
  local haystack="$1"
  local needle="$2"
  local restore_nocasematch=false
  if ! shopt -q nocasematch; then
    restore_nocasematch=true
    shopt -s nocasematch
  fi
  [[ "$haystack" == *"$needle"* ]]
  local matched=$?
  if [[ "$restore_nocasematch" == true ]]; then
    shopt -u nocasematch
  fi
  return "$matched"
}

case_insensitive_matches() {
  local haystack="$1"
  local pattern="$2"
  local restore_nocasematch=false
  if ! shopt -q nocasematch; then
    restore_nocasematch=true
    shopt -s nocasematch
  fi
  [[ "$haystack" =~ $pattern ]]
  local matched=$?
  if [[ "$restore_nocasematch" == true ]]; then
    shopt -u nocasematch
  fi
  return "$matched"
}

trim_token_punctuation() {
  normalized_token="$1"
  while [[ -n "$normalized_token" ]]; do
    case "${normalized_token:0:1}" in
      "'"|'"'|'`'|'('| '['|'{')
        normalized_token="${normalized_token:1}"
        ;;
      *)
        break
        ;;
    esac
  done
  while [[ -n "$normalized_token" ]]; do
    case "${normalized_token: -1}" in
      "'"|'"'|'`'|')'|']'|'}'|','|';')
        normalized_token="${normalized_token:0:${#normalized_token}-1}"
        ;;
      *)
        break
        ;;
    esac
  done
}

token_is_timeout_command() {
  trim_token_punctuation "$1"
  local command_name="${normalized_token##*/}"
  [[ "$command_name" == timeout || "$command_name" == gtimeout ]]
}

duration_candidate_indicates_timeout_wrapper() {
  trim_token_punctuation "$1"
  [[ "$normalized_token" =~ ^[0-9] || "$normalized_token" == \$* ]]
}

evidence_uses_timeout_wrapper() {
  local text="$1"
  local line
  local words
  while IFS= read -r line; do
    read -r -a words <<< "$line"
    local word_index
    for ((word_index = 0; word_index < ${#words[@]}; word_index++)); do
      if ! token_is_timeout_command "${words[$word_index]}"; then
        continue
      fi

      local candidate_index=$((word_index + 1))
      while (( candidate_index < ${#words[@]} )); do
        trim_token_punctuation "${words[$candidate_index]}"
        case "$normalized_token" in
          --kill-after|--signal|-k|-s)
            candidate_index=$((candidate_index + 2))
            continue
            ;;
          --preserve-status|--foreground|--verbose|-v|--kill-after=*|--signal=*|-k*|-s*)
            candidate_index=$((candidate_index + 1))
            continue
            ;;
          -*)
            candidate_index=$((candidate_index + 1))
            continue
            ;;
        esac

        if duration_candidate_indicates_timeout_wrapper "$normalized_token"; then
          return 0
        fi
        break
      done
    done
  done <<< "$text"
  return 1
}

evidence_section_block() {
  local section_label="$1"
  local line
  local in_section=false
  while IFS= read -r line; do
    if [[ "$in_section" == true ]]; then
      if [[ "$line" =~ ^[^[:space:]].*:[[:space:]]* ]]; then
        return 0
      fi
      printf '%s\n' "$line"
    elif [[ "$line" == *"$section_label"* ]]; then
      in_section=true
      printf '%s\n' "$line"
    fi
  done <<< "$evidence_text"
  [[ "$in_section" == true ]]
}

noop_justification_references_expected_head() {
  local line
  local remaining_lines=0
  while IFS= read -r line; do
    if [[ "$line" == *"No-op justification:"* ]]; then
      remaining_lines=9
    fi
    if (( remaining_lines > 0 )); then
      if [[ "$line" == *"$expected_head"* ]]; then
        return 0
      fi
      remaining_lines=$((remaining_lines - 1))
    fi
  done <<< "$evidence_text"
  return 1
}

evidence_has_out_of_scope_claim() {
  local claim_pattern='^[[:space:]]*(full desktop lesson automation|full ui automation|desktop save-menu completion|save dialog completion|visible rendering correctness|grading workflow|grading|creative assessment|full tweedle/player decode|full save completion|full first-lesson completion|lesson completion|player runtime behavior)[[:space:]]*:[[:space:]]*(proven|validated|supported|complete|passed|ready)'
  local line
  local restore_nocasematch=false
  if ! shopt -q nocasematch; then
    restore_nocasematch=true
    shopt -s nocasematch
  fi
  while IFS= read -r line; do
    if [[ "$line" =~ $claim_pattern ]]; then
      if [[ "$restore_nocasematch" == true ]]; then
        shopt -u nocasematch
      fi
      return 0
    fi
  done <<< "$evidence_text"
  if [[ "$restore_nocasematch" == true ]]; then
    shopt -u nocasematch
  fi
  return 1
}

cycle_block_has_only_clean_result() {
  local block="$1"
  local line
  local found_result=false
  local restore_nocasematch=false
  if ! shopt -q nocasematch; then
    restore_nocasematch=true
    shopt -s nocasematch
  fi
  while IFS= read -r line; do
    if [[ "$line" =~ ^[[:space:]]*result:[[:space:]]* ]]; then
      if [[ "$line" =~ ^[[:space:]]*result:[[:space:]]*clean[[:space:]]*$ ]]; then
        found_result=true
      else
        if [[ "$restore_nocasematch" == true ]]; then
          shopt -u nocasematch
        fi
        return 1
      fi
    fi
  done <<< "$block"
  if [[ "$restore_nocasematch" == true ]]; then
    shopt -u nocasematch
  fi
  [[ "$found_result" == true ]]
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

  external_failure_pattern='github api unavailable|github unavailable|gh auth (failed|failure)|rate[- ]limited|rate limit|service unavailable|network error|could not resolve host|pr description unavailable|checks unavailable|check state unavailable'
  if case_insensitive_matches "$evidence_text" "$external_failure_pattern"; then
    fail 1 "no-op evidence must not claim readiness when external GitHub service evidence is unavailable"
  fi

  require_evidence_contains \
    "No-op justification:" \
    "no-op evidence must include a No-op justification section"

  for required_metadata in \
    "PR: 402" \
    "Branch: wave6-project-reopen-edit-chain-1778302300" \
    "Base: develop"; do
    require_evidence_contains \
      "$required_metadata" \
      "no-op evidence must include expected ${required_metadata%%:*} metadata"
  done

  if [[ "$evidence_text" == *"Files modified:"* ]]; then
    fail 1 "clean no-op evidence must not also list modified files"
  fi

  if [[ "$evidence_text" == *"NOT_MERGE_READY"* ]]; then
    fail 1 "NO_OP_GUARD no-op evidence must not include a NOT_MERGE_READY outcome"
  fi

  for current_head_field in "PR head" "Local HEAD" "Remote branch HEAD"; do
    require_evidence_contains \
      "$current_head_field: $expected_head" \
      "stale no-op evidence for expected head $expected_head"
  done

  if ! noop_justification_references_expected_head; then
    fail 1 "no-op justification must reference expected head $expected_head"
  fi

  for required_section in \
    "Scope exclusions:" \
    "Positive claim scope:" \
    "Stale evidence note:"; do
    require_evidence_contains \
      "$required_section" \
      "no-op evidence must include ${required_section%:}"
  done

  require_evidence_contains \
    "Runnable QA/scenario evidence:" \
    "no-op evidence must include runnable QA/scenario evidence"
  qa_scenario_evidence="$(evidence_section_block "Runnable QA/scenario evidence:")"
  if evidence_uses_timeout_wrapper "$qa_scenario_evidence"; then
    fail 1 "runnable QA/scenario evidence must not use timeout wrappers"
  fi
  if [[ "$qa_scenario_evidence" != *"$expected_head"* ]]; then
    fail 1 "runnable QA/scenario evidence must reference expected head $expected_head"
  fi

  require_evidence_contains \
    "Docs impact:" \
    "no-op evidence must include docs impact review evidence"
  require_evidence_contains \
    "PR description evidence:" \
    "no-op evidence must include PR description evidence"
  pr_description_evidence="$(evidence_section_block "PR description evidence:")"
  if [[ "$pr_description_evidence" != *"$expected_head"* ]]; then
    fail 1 "PR description evidence must reference expected head $expected_head"
  fi

  missing_checks=()
  for required_check in \
    "gitguardian security checks" \
    "alice checkstyle ci/build (pull_request)" \
    "alice coverage reports/coverage (pull_request)" \
    "alice netbeans package ci/package-netbeans (pull_request)" \
    "alice test ci/test (pull_request)"; do
    if ! case_insensitive_contains "$evidence_text" "$required_check successful at current pr head"; then
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
    "creative assessment" \
    "full tweedle/player decode" \
    "player runtime behavior"; do
    if ! case_insensitive_contains "$evidence_text" "$required_exclusion"; then
      missing_exclusions+=("$required_exclusion")
    fi
  done
  if [[ "${#missing_exclusions[@]}" -gt 0 ]]; then
    fail 1 "no-op evidence scope exclusions must mention ${missing_exclusions[*]}"
  fi

  if evidence_has_out_of_scope_claim; then
    fail 1 "no-op evidence contains out-of-scope desktop, rendering, grading, creative assessment, Tweedle/player decode, Save, lesson, or player claims"
  fi

  audit_cycle_count=0
  max_audit_cycle=0
  current_audit_cycle=0
  declare -a audit_cycle_blocks=()
  while IFS= read -r line; do
    if [[ "$line" =~ ^Quality-audit\ cycle\ ([0-9]+): ]]; then
      current_audit_cycle="${BASH_REMATCH[1]}"
      if [[ -z "${audit_cycle_blocks[$current_audit_cycle]+set}" ]]; then
        audit_cycle_count=$((audit_cycle_count + 1))
      fi
      if (( current_audit_cycle > max_audit_cycle )); then
        max_audit_cycle="$current_audit_cycle"
      fi
      audit_cycle_blocks[$current_audit_cycle]="$line"$'\n'
    elif (( current_audit_cycle > 0 )); then
      audit_cycle_blocks[$current_audit_cycle]+="$line"$'\n'
    fi
  done <<< "$evidence_text"
  if (( audit_cycle_count < 3 )); then
    fail 1 "no-op evidence must include at least three quality-audit cycles"
  fi
  for cycle_number in 1 2 3; do
    cycle_block="${audit_cycle_blocks[$cycle_number]-}"
    if [[ -z "$cycle_block" ]]; then
      fail 1 "no-op evidence is missing quality-audit cycle $cycle_number"
    fi
    for required_step in "SEEK:" "VALIDATE:" "FIX:" "Result:"; do
      if [[ "$cycle_block" != *"$required_step"* ]]; then
        fail 1 "quality-audit cycle $cycle_number must include $required_step"
      fi
    done
  done
  final_cycle_block="${audit_cycle_blocks[$max_audit_cycle]-}"
  if ! cycle_block_has_only_clean_result "$final_cycle_block"; then
    fail 1 "final quality-audit cycle must be clean"
  fi

  exit 0
fi

fail 1 "no project archive reopen/edit changes found in git worktree: $repo_root"
