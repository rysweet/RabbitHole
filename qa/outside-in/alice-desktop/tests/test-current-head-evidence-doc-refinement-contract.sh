#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-current-head-evidence-doc-refinement-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

HOWTO_DOC="$REPO_ROOT/docs/howto/alice-desktop-outside-in-qa.md"
POST_OPEN_REFERENCE_DOC="$REPO_ROOT/docs/reference/post-open-runtime-display-accessibility-evidence.md"
NONCLAIM_REFERENCE_DOC="$REPO_ROOT/docs/reference/visible-rendering-evidence-nonclaim-contract.md"
QA_REFERENCE_DOC="$REPO_ROOT/docs/reference/alice-desktop-outside-in-qa.md"

for doc in "$HOWTO_DOC" "$POST_OPEN_REFERENCE_DOC" "$NONCLAIM_REFERENCE_DOC" "$QA_REFERENCE_DOC"; do
  assert_file_exists "$doc" "current-head evidence docs refinement input exists: ${doc#$REPO_ROOT/}"
done

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_no_adjacent_sampling_results() {
  local path=$1
  local label=$2
  python3 - "$path" <<'PY' >"$tmp_root/adjacent-sampling-results.out" 2>"$tmp_root/adjacent-sampling-results.err"
import re
import sys
from pathlib import Path

text = Path(sys.argv[1]).read_text(encoding="utf-8")
patterns = (
    r"visible-rendering-pixel-observation\.json\s*\n\s*visible-rendering-pixel-sampling-blocker\.json",
    r"visible-rendering-pixel-sampling-blocker\.json\s*\n\s*visible-rendering-pixel-observation\.json",
)
raise SystemExit(1 if any(re.search(pattern, text) for pattern in patterns) else 0)
PY
  assert_success "$?" "$label"
}

assert_literal_in_file "$HOWTO_DOC" "<pr-number>" "how-to recovery command parameterizes the PR number"
assert_literal_in_file "$HOWTO_DOC" "<pr-branch>" "how-to recovery command parameterizes the PR branch"

for stale_token in \
  "refs/pull/419" \
  "origin/pr/419" \
  "feat/issue-416" \
  "rabbithole-wave7-accessibility-target-lane-follow"; do
  assert_literal_absent_from_file "$HOWTO_DOC" "$stale_token" "how-to avoids hard-coded PR419 token $stale_token"
done

for metadata_token in gitHead originDevelopHead mergeBase; do
  for doc in "$HOWTO_DOC" "$POST_OPEN_REFERENCE_DOC" "$NONCLAIM_REFERENCE_DOC" "$QA_REFERENCE_DOC"; do
    assert_literal_absent_from_file "$doc" "$metadata_token" "${doc#$REPO_ROOT/} avoids runner-emitted Git metadata field $metadata_token"
  done
done

assert_literal_in_file \
  "$POST_OPEN_REFERENCE_DOC" \
  "The current runner does not emit Git SHAs in \`environment.txt\`; reviewers record those externally." \
  "post-open reference separates runner metadata from external Git SHA review metadata"
assert_literal_in_file \
  "$HOWTO_DOC" \
  "\`environment.txt\`, so keep them in the PR notes, review notes, or external CI" \
  "how-to keeps PR/develop/merge-base SHAs outside runner-emitted metadata"

sampling_result_line="visible-rendering-pixel-observation.json OR visible-rendering-pixel-sampling-blocker.json"
assert_literal_in_file "$HOWTO_DOC" "$sampling_result_line" "how-to shows pixel observation OR blocker as one-run result"
assert_literal_in_file "$POST_OPEN_REFERENCE_DOC" "$sampling_result_line" "post-open reference shows pixel observation OR blocker as one-run result"
assert_literal_in_file \
  "$HOWTO_DOC" \
  "Pixel sampling writes either the observation artifact or the blocker artifact, never both as the result for one run" \
  "how-to states pixel sampling writes observation or blocker, never both"
assert_literal_in_file \
  "$HOWTO_DOC" \
  "Target readiness writes \`visible-rendering-pixel-target-blocker.json\` only when target identification is blocked." \
  "how-to includes target-readiness blocker wording"
assert_literal_in_file \
  "$HOWTO_DOC" \
  "visible-rendering-pixel-target-blocker.json (when target readiness is blocked)" \
  "how-to artifact list names target blocker separately from sampling results"
assert_literal_in_file \
  "$POST_OPEN_REFERENCE_DOC" \
  "target blocker JSON when target readiness is blocked" \
  "post-open current-head artifacts include target-readiness blocker"
assert_literal_in_file "$HOWTO_DOC" "if rg '(<{7}|={7}|>{7})' docs qa pyproject.toml; then" "how-to shows fail-on-match conflict-marker check"
assert_literal_in_file "$HOWTO_DOC" "conflict markers remain" "how-to names the conflict-marker failure"
assert_literal_in_file "$HOWTO_DOC" "should print no paths or marker" "how-to explains rg no-match success output"

assert_no_adjacent_sampling_results "$HOWTO_DOC" "how-to does not list pixel observation and sampling blocker as simultaneous artifacts"
assert_no_adjacent_sampling_results "$POST_OPEN_REFERENCE_DOC" "post-open reference does not list pixel observation and sampling blocker as simultaneous artifacts"

for scenario_id in \
  "alice-desktop-procedure-edit-seam-smoke" \
  "alice-desktop-procedure-edit-handoff-smoke"; do
  assert_exact_count_in_file "$QA_REFERENCE_DOC" "| \`$scenario_id\` |" 1 "QA reference lists $scenario_id exactly once"
done

for bounded_term in \
  "does not establish visible correctness" \
  "does not execute Alice worlds" \
  "do not launch a world or certify visual output"; do
  assert_literal_in_file "$NONCLAIM_REFERENCE_DOC" "$bounded_term" "nonclaim reference preserves bounded wording: $bounded_term"
done

finish
