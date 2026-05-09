#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-pr419-finalization-evidence-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

EVIDENCE_LOG="$REPO_ROOT/.copilot-evidence/default-workflow-attempt.log"
HOWTO_DOC="$REPO_ROOT/docs/howto/alice-desktop-outside-in-qa.md"
POST_OPEN_REFERENCE_DOC="$REPO_ROOT/docs/reference/post-open-runtime-display-accessibility-evidence.md"
NONCLAIM_REFERENCE_DOC="$REPO_ROOT/docs/reference/visible-rendering-evidence-nonclaim-contract.md"

for input in "$EVIDENCE_LOG" "$HOWTO_DOC" "$POST_OPEN_REFERENCE_DOC" "$NONCLAIM_REFERENCE_DOC"; do
  assert_file_exists "$input" "PR419 finalization contract input exists: ${input#$REPO_ROOT/}"
done

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

current_branch=$(git -C "$REPO_ROOT" branch --show-current)
unmerged_paths=$(git -C "$REPO_ROOT" diff --name-only --diff-filter=U)

combined_docs="$tmp_root/pr419-finalization-docs.txt"
cat "$EVIDENCE_LOG" "$HOWTO_DOC" "$POST_OPEN_REFERENCE_DOC" "$NONCLAIM_REFERENCE_DOC" >"$combined_docs"

assert_nonclaim_wording() {
  local claim=$1
  local pattern="(does not|do not|not claim|non-claim|no claim|without)[^.:]*${claim}"
  if grep -Eiq -- "$pattern" "$combined_docs"; then
    pass "finalization docs/evidence preserve non-claim for $claim"
  else
    fail "finalization docs/evidence preserve non-claim for $claim"
  fi
}

if [ "$current_branch" = "feat/issue-416-rabbithole-wave7-accessibility-target-lane-follow" ]; then
  pass "finalization contract runs on requested PR419 recovery branch"
else
  fail "finalization contract runs on requested PR419 recovery branch (got $current_branch)"
fi

if [ -z "$unmerged_paths" ]; then
  pass "finalization contract has no unmerged paths"
else
  fail "finalization contract has no unmerged paths (found: $unmerged_paths)"
fi

assert_literal_in_file "$EVIDENCE_LOG" "Default-workflow recovery evidence for PR #419" "evidence log names PR419 recovery"
assert_literal_in_file "$EVIDENCE_LOG" "Branch: $current_branch" "evidence log names checked branch"
assert_literal_in_file "$EVIDENCE_LOG" "Workflow readiness/review/finalization evidence:" "evidence log has finalization section"
assert_literal_in_file \
  "$EVIDENCE_LOG" \
  "Current-head evidence boundary: claims below apply only to commands listed in this recovery pass." \
  "evidence log bounds current-head claims"
assert_literal_in_file \
  "$EVIDENCE_LOG" \
  "No timeout wrappers were used; NODE_OPTIONS=--max-old-space-size=32768 was exported for focused checks." \
  "evidence log records no timeout wrappers and required NODE_OPTIONS"
assert_literal_in_file "$EVIDENCE_LOG" "No manual PR merge was performed for PR #419." "evidence log records no manual PR merge"
assert_literal_in_file "$EVIDENCE_LOG" "Step 8b external service integration was skipped because this recovery lane" "evidence log records skipped service integration scope"
assert_literal_in_file "$EVIDENCE_LOG" "introduces no API client implementation, service adapter, or runtime external" "evidence log states no runtime external service implementation"
assert_literal_in_file "$EVIDENCE_LOG" "not a repository runtime integration; no retry path was added." "evidence log avoids retry-path overclaim"
assert_literal_in_file "$EVIDENCE_LOG" "Unmerged-path check: git diff --name-only --diff-filter=U produced no output." "evidence log records unmerged-path check"
assert_literal_in_file "$EVIDENCE_LOG" "Conflict-marker check: rg '(<{7}|={7}|>{7})' docs qa pyproject.toml produced no output." "evidence log records conflict-marker check"

for command in \
  "NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-docs-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-world-canvas-pixel-sampler-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-pr419-readiness-evidence-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-current-head-evidence-doc-refinement-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-pr419-finalization-evidence-contract.sh"; do
  assert_literal_in_file "$EVIDENCE_LOG" "$command" "evidence log records focused command: $command"
done

for unsupported_claim in \
  "full UI automation" \
  "visible rendering correctness" \
  "grading" \
  "creative assessment" \
  "lesson completion"; do
  assert_nonclaim_wording "$unsupported_claim"
done

for forbidden_command in \
  '^- *timeout +' \
  '^- *gtimeout +' \
  '^- *gh pr merge\b' \
  '^- *hub merge\b'; do
  assert_pattern_absent_from_file "$EVIDENCE_LOG" "$forbidden_command" "evidence log does not record forbidden shortcut $forbidden_command"
done

finish
