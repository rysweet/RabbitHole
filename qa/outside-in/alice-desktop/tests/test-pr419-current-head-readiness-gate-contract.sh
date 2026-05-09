#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-pr419-current-head-readiness-gate-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

EVIDENCE_LOG="$REPO_ROOT/.copilot-evidence/default-workflow-attempt.log"
SILVER_THREAD_DOC="$REPO_ROOT/docs/reference/accessibility-target-discovery-silver-thread.md"
PR_NUMBER=419
EXPECTED_PR_URL="https://github.com/rysweet/RabbitHole/pull/419"
EXPECTED_BRANCH="feat/issue-416-rabbithole-wave7-accessibility-target-lane-follow"

assert_file_exists "$EVIDENCE_LOG" "current-head readiness evidence log exists"
assert_file_exists "$SILVER_THREAD_DOC" "current-head readiness reference doc exists"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

current_branch=$(git -C "$REPO_ROOT" branch --show-current)
local_head=$(git -C "$REPO_ROOT" rev-parse HEAD)
pr_metadata="$tmp_root/pr-metadata.json"
pr_checks="$tmp_root/pr-checks.tsv"
pr_fields="$tmp_root/pr-fields.tsv"
pr_number=
pr_url=
pr_branch=
pr_head=
pr_merge_state=
pr_mergeable=
pr_body_has_current_head=
pr_body_has_gate_evidence=
pr_body_has_mergeability_evidence=
GH_NO_UPDATE_NOTIFIER=1 gh pr view "$PR_NUMBER" \
  --json number,url,headRefName,headRefOid,mergeStateStatus,mergeable,statusCheckRollup,body \
  >"$pr_metadata" \
  2>"$tmp_root/pr-metadata.err"
pr_metadata_status=$?
: >"$pr_checks"
if [ "$pr_metadata_status" -eq 0 ]; then
  python3 - "$pr_metadata" "$local_head" "$pr_fields" "$pr_checks" \
    >"$tmp_root/pr-metadata-parse.out" \
    2>"$tmp_root/pr-metadata-parse.err" <<'PY'
import json
import sys
from pathlib import Path

metadata_path, local_head, fields_path, checks_path = sys.argv[1:5]
metadata = json.loads(Path(metadata_path).read_text(encoding="utf-8"))
body = metadata.get("body") or ""
checks = metadata.get("statusCheckRollup") or []

field_values = (
    str(metadata.get("number") or ""),
    metadata.get("url") or "",
    metadata.get("headRefName") or "",
    metadata.get("headRefOid") or "",
    metadata.get("mergeStateStatus") or "",
    metadata.get("mergeable") or "",
    "true" if local_head in body else "false",
    "true" if "test-pr419-current-head-readiness-gate-contract.sh" in body else "false",
    "true" if "mergeStateStatus=CLEAN" in body and "mergeable=MERGEABLE" in body else "false",
)
Path(fields_path).write_text("\t".join(field_values) + "\n", encoding="utf-8")
with Path(checks_path).open("w", encoding="utf-8") as checks_file:
    for check in checks:
        checks_file.write(
            "\t".join(
                (
                    str(check.get("name") or ""),
                    str(check.get("status") or ""),
                    str(check.get("conclusion") or ""),
                )
            )
            + "\n"
        )
PY
  pr_parse_status=$?
  if [ "$pr_parse_status" -eq 0 ]; then
    tab=$(printf '\t')
    IFS=$tab read -r pr_number pr_url pr_branch pr_head pr_merge_state pr_mergeable pr_body_has_current_head pr_body_has_gate_evidence pr_body_has_mergeability_evidence <"$pr_fields"
  fi
else
  pr_parse_status=1
fi

assert_success "$pr_metadata_status" "PR419 metadata can be read in one GitHub request"
assert_success "$pr_parse_status" "PR419 cached metadata can be parsed for live gate evidence"

if [ "$pr_number" = "$PR_NUMBER" ]; then
  pass "PR419 number can be read from cached metadata"
else
  fail "PR419 number can be read from cached metadata (got $pr_number)"
fi

if [ "$pr_url" = "$EXPECTED_PR_URL" ]; then
  pass "PR419 URL can be read from cached metadata"
else
  fail "PR419 URL can be read from cached metadata (got $pr_url)"
fi

if [ -n "$pr_head" ]; then
  pass "PR419 headRefOid can be read from cached metadata"
else
  fail "PR419 headRefOid can be read from cached metadata"
fi

if [ -n "$pr_branch" ]; then
  pass "PR419 headRefName can be read from cached metadata"
else
  fail "PR419 headRefName can be read from cached metadata"
fi

if [ "$current_branch" = "$EXPECTED_BRANCH" ]; then
  pass "current-head gate runs on requested PR419 branch"
else
  fail "current-head gate runs on requested PR419 branch (got $current_branch)"
fi

if [ "$pr_branch" = "$EXPECTED_BRANCH" ]; then
  pass "PR419 head branch matches requested branch"
else
  fail "PR419 head branch matches requested branch (got $pr_branch)"
fi

if [ "$local_head" = "$pr_head" ]; then
  pass "local HEAD matches PR419 headRefOid before readiness evaluation"
else
  fail "local HEAD matches PR419 headRefOid before readiness evaluation (local $local_head, PR $pr_head)"
fi

if [ "$pr_merge_state" = "CLEAN" ] && [ "$pr_mergeable" = "MERGEABLE" ]; then
  pass "PR419 live GitHub mergeability is clean"
else
  fail "PR419 live GitHub mergeability is clean (mergeStateStatus=$pr_merge_state, mergeable=$pr_mergeable)"
fi

if [ "$pr_body_has_current_head" = "true" ]; then
  pass "PR419 body contains current-head evidence for the live PR head"
else
  fail "PR419 body contains current-head evidence for the live PR head ($local_head)"
fi

if [ "$pr_body_has_gate_evidence" = "true" ]; then
  pass "PR419 body names the current-head readiness gate contract"
else
  fail "PR419 body names the current-head readiness gate contract"
fi

if [ "$pr_body_has_mergeability_evidence" = "true" ]; then
  pass "PR419 body names the live mergeability result for the current head"
else
  fail "PR419 body names the live mergeability result for the current head"
fi

unmerged_paths=$(git -C "$REPO_ROOT" diff --name-only --diff-filter=U)
if [ -z "$unmerged_paths" ]; then
  pass "current-head gate has no unmerged paths"
else
  fail "current-head gate has no unmerged paths (found: $unmerged_paths)"
fi

git -C "$REPO_ROOT" grep -n -E '(<{7}|={7}|>{7})' -- docs qa pyproject.toml >"$tmp_root/conflict-markers.out" 2>"$tmp_root/conflict-markers.err"
conflict_status=$?
if [ "$conflict_status" -eq 1 ]; then
  pass "current-head gate has no conflict markers"
elif [ "$conflict_status" -eq 0 ]; then
  fail "current-head gate has no conflict markers (see $tmp_root/conflict-markers.out)"
else
  fail "current-head gate conflict-marker scan completed (exit $conflict_status)"
fi

if [ -s "$pr_checks" ]; then
  pass "PR419 check rollup can be read from cached metadata"
else
  fail "PR419 check rollup can be read from cached metadata"
fi

if [ -s "$pr_checks" ] && awk -F '\t' '$2 != "COMPLETED" || $3 != "SUCCESS" { bad = 1 } END { exit bad }' "$pr_checks"; then
  pass "PR419 GitHub Actions are completed and green"
else
  fail "PR419 GitHub Actions are completed and green"
fi

assert_literal_in_file "$EVIDENCE_LOG" "Current-head merge-ready gate model:" "evidence log has current-head gate model section"
assert_literal_in_file "$EVIDENCE_LOG" "Live current-head proof is generated transiently by test-pr419-current-head-readiness-gate-contract.sh at runtime" "evidence log keeps live proof transient"
assert_literal_in_file "$EVIDENCE_LOG" "mergeStateStatus,mergeable" "evidence log records live mergeability fields"
assert_literal_in_file "$EVIDENCE_LOG" "live GitHub mergeability" "evidence log records live mergeability requirement"
assert_literal_in_file "$EVIDENCE_LOG" "Tracked evidence must not store or predict the current PR head SHA." "evidence log rejects static current-head SHA proof"
assert_literal_in_file "$EVIDENCE_LOG" "The PR body must be updated after the final commit to name the live head SHA and focused gate evidence." "evidence log records PR body live-head requirement"
assert_literal_in_file "$EVIDENCE_LOG" "The PR body must also name the live mergeability result" "evidence log records PR body live mergeability requirement"
assert_literal_in_file "$EVIDENCE_LOG" "green checks are necessary but not sufficient" "evidence log does not infer readiness from green checks alone"

for component in PRHeadAlignment EvidenceCollector QAValidationRunner QualityAuditCycleRecorder ReadinessGate; do
  assert_contains "$EVIDENCE_LOG" "^$component: " "evidence log records $component"
done

for cycle in 1 2 3; do
  for step in SEEK VALIDATE FIX; do
    assert_contains "$EVIDENCE_LOG" "^Cycle $cycle $step:" "evidence log records Cycle $cycle $step"
  done
done

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
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-pr419-finalization-evidence-contract.sh" \
  "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-pr419-current-head-readiness-gate-contract.sh"; do
  assert_literal_in_file "$EVIDENCE_LOG" "$command" "evidence log records focused command: $command"
done

for unsupported_claim in \
  "full UI automation" \
  "visible rendering correctness" \
  "grading" \
  "creative assessment" \
  "full lesson completion" \
  "full Tweedle/player decode"; do
  assert_contains "$EVIDENCE_LOG" "(does not|do not|not claim|non-claim|no claim)[^.:]*$unsupported_claim" "evidence log bounds claim: $unsupported_claim"
done

assert_literal_in_file "$SILVER_THREAD_DOC" "test-pr419-current-head-readiness-gate-contract.sh" "reference doc names current-head gate contract"
assert_literal_in_file "$SILVER_THREAD_DOC" "transient current-head proof" "reference doc describes transient current-head proof"
assert_literal_in_file "$SILVER_THREAD_DOC" "mergeStateStatus,mergeable" "reference doc names live mergeability fields"
assert_literal_in_file "$SILVER_THREAD_DOC" "live GitHub mergeability" "reference doc requires live mergeability"
assert_literal_in_file "$SILVER_THREAD_DOC" "MERGE_READY" "reference doc names merge-ready decision"
assert_literal_in_file "$SILVER_THREAD_DOC" "NOT_MERGE_READY" "reference doc names not-merge-ready decision"

finish
