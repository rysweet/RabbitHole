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

python3 - \
  "$EVIDENCE_LOG" \
  "$HOWTO_DOC" \
  "$POST_OPEN_REFERENCE_DOC" \
  "$NONCLAIM_REFERENCE_DOC" \
  "$current_branch" \
  "$unmerged_paths" \
  >"$tmp_root/pr419-finalization-contract.out" \
  2>"$tmp_root/pr419-finalization-contract.err" <<'PY'
import re
import sys
from pathlib import Path

(
    evidence_path,
    howto_path,
    post_open_reference_path,
    nonclaim_reference_path,
    current_branch,
    unmerged_paths,
) = sys.argv[1:7]

texts = {
    "evidence": Path(evidence_path).read_text(encoding="utf-8"),
    "howto": Path(howto_path).read_text(encoding="utf-8"),
    "post_open_reference": Path(post_open_reference_path).read_text(encoding="utf-8"),
    "nonclaim_reference": Path(nonclaim_reference_path).read_text(encoding="utf-8"),
}
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def require_literal(text, token, label):
    require(token in text, f"{label} must include {token!r}")


evidence = texts["evidence"]
combined_docs = "\n\n".join(texts.values())

# Unit-level evidence fields for the finalization component.
require_literal(evidence, "Default-workflow recovery evidence for PR #419", "evidence log")
require_literal(evidence, f"Branch: {current_branch}", "evidence log")
require_literal(evidence, "Workflow readiness/review/finalization evidence:", "evidence log")
require_literal(
    evidence,
    "Current-head evidence boundary: claims below apply only to commands listed in this recovery pass.",
    "evidence log",
)
require_literal(
    evidence,
    "No timeout wrappers were used; NODE_OPTIONS=--max-old-space-size=32768 was exported for focused checks.",
    "evidence log",
)
require_literal(evidence, "No manual PR merge was performed for PR #419.", "evidence log")
require_literal(
    evidence,
    "Step 8b external service integration was skipped because this recovery lane",
    "evidence log",
)
require_literal(
    evidence,
    "introduces no API client implementation, service adapter, or runtime external",
    "evidence log",
)
require_literal(
    evidence,
    "not a repository runtime integration; no retry path was added.",
    "evidence log",
)
require_literal(
    evidence,
    "Unmerged-path check: git diff --name-only --diff-filter=U produced no output.",
    "evidence log",
)
require_literal(
    evidence,
    "Conflict-marker check: rg '(<{7}|={7}|>{7})' docs qa pyproject.toml produced no output.",
    "evidence log",
)

# Integration-level workflow expectations for the checked-out PR recovery worktree.
require(
    current_branch == "feat/issue-416-rabbithole-wave7-accessibility-target-lane-follow",
    "finalization contract must run on the requested PR419 recovery branch",
)
require(not unmerged_paths.strip(), f"finalization contract requires no unmerged paths, found: {unmerged_paths!r}")
for command in (
    "NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-docs-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-world-canvas-pixel-sampler-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-pr419-readiness-evidence-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-current-head-evidence-doc-refinement-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-pr419-finalization-evidence-contract.sh",
):
    require_literal(evidence, command, "evidence log focused finalization command list")

# Edge-case evidence boundaries: docs and logs may describe only bounded silver-thread evidence.
for unsupported_claim in (
    "full UI automation",
    "visible rendering correctness",
    "grading",
    "creative assessment",
    "lesson completion",
):
    require(
        re.search(
            rf"(does not|do not|not claim|non-claim|no claim|without)[^.:\n]*{re.escape(unsupported_claim)}",
            combined_docs,
            flags=re.IGNORECASE,
        ),
        f"finalization docs/evidence must preserve an explicit non-claim for {unsupported_claim}",
    )

# Error-handling evidence: forbidden operational shortcuts must be absent from the recorded commands.
for forbidden_command in (
    r"(?m)^-\s+timeout\s+",
    r"(?m)^-\s+gtimeout\s+",
    r"(?m)^-\s+gh pr merge\b",
    r"(?m)^-\s+hub merge\b",
):
    require(
        not re.search(forbidden_command, evidence),
        f"evidence log must not record forbidden finalization shortcut {forbidden_command!r}",
    )

if errors:
    raise AssertionError("\n".join(errors))

print("PR419 finalization evidence contract satisfied")
PY
status=$?
if [ "$status" -ne 0 ]; then
  cat "$tmp_root/pr419-finalization-contract.err" >&2
fi
assert_success "$status" "PR419 finalization evidence is explicit, bounded, and operationally safe"

finish
