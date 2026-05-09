#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-pr419-readiness-evidence-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
EVIDENCE_LOG="$REPO_ROOT/.copilot-evidence/default-workflow-attempt.log"
SILVER_THREAD_DOC="$REPO_ROOT/docs/reference/accessibility-target-discovery-silver-thread.md"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_file_exists "$EVIDENCE_LOG" "PR419 readiness evidence log exists"
assert_file_exists "$SILVER_THREAD_DOC" "silver-thread reference doc exists"

current_branch=$(git -C "$REPO_ROOT" branch --show-current)
head_sha=$(git -C "$REPO_ROOT" rev-parse HEAD)
develop_sha=$(git -C "$REPO_ROOT" rev-parse origin/develop)
merge_base=$(git -C "$REPO_ROOT" merge-base HEAD origin/develop)

python3 - \
  "$EVIDENCE_LOG" \
  "$SILVER_THREAD_DOC" \
  "$current_branch" \
  "$head_sha" \
  "$develop_sha" \
  "$merge_base" \
  >"$tmp_root/pr419-readiness-contract.out" \
  2>"$tmp_root/pr419-readiness-contract.err" <<'PY'
import re
import sys
from pathlib import Path

(
    evidence_path,
    doc_path,
    current_branch,
    head_sha,
    develop_sha,
    merge_base,
) = sys.argv[1:7]
evidence = Path(evidence_path).read_text(encoding="utf-8")
doc = Path(doc_path).read_text(encoding="utf-8")
combined = f"{evidence}\n\n{doc}"
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def require_literal(text, token, label):
    require(token in text, f"{label} must include {token!r}")


def require_pattern(text, pattern, label):
    require(re.search(pattern, text, flags=re.MULTILINE), f"{label} must match /{pattern}/")


require_literal(evidence, "Default-workflow recovery evidence for PR #419", "evidence log")
require_literal(evidence, "PR: https://github.com/rysweet/RabbitHole/pull/419", "evidence log")
require_literal(evidence, f"Branch: {current_branch}", "evidence log")
require_literal(evidence, "Base branch: develop", "evidence log")
require_literal(
    evidence,
    "Evidence stage: final PR419 exact-head readiness handoff",
    "evidence log",
)
require_literal(
    evidence,
    "PR #419 points at the validated exact HEAD",
    "evidence log",
)
require_literal(evidence, "Worktree end state:", "evidence log")
require_literal(evidence, "Intentional changes:", "evidence log")
require_literal(evidence, "Focused validation completed:", "evidence log")
require_literal(evidence, "Observed validation outcomes:", "evidence log")
require_literal(evidence, "Readiness claim:", "evidence log")
require_literal(evidence, "Explicit non-claims:", "evidence log")

require_pattern(
    evidence,
    r"^Exact checked HEAD at final validation: [0-9a-f]{40}$",
    "evidence log",
)
require_literal(evidence, f"origin/develop at recovery: {develop_sha}", "evidence log")
require_literal(evidence, f"merge-base(HEAD, origin/develop): {merge_base}", "evidence log")

for command in (
    "NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-docs-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-world-canvas-pixel-sampler-contract.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh",
):
    require_literal(evidence, command, "evidence log validation command list")

for forbidden in (
    "PR #388",
    "PR388",
    "pull/388",
    "pre-finalization",
    "pre-finalization documentation recovery",
):
    require(forbidden not in combined, f"PR419 readiness surfaces must not carry stale or provisional token {forbidden!r}")

bounded_scope_terms = (
    "full UI automation",
    "visible rendering correctness",
    "full world execution",
    "grading",
    "Save completion",
    "Sims validation",
    "deployed installer success",
    "broad accessibility compliance",
)
for term in bounded_scope_terms:
    require(term.lower() in evidence.lower(), f"evidence log must explicitly preserve non-claim boundary for {term}")

require(
    "discovered accessibility/runtime display target" in evidence.lower()
    or "accessibility/runtime display targets" in evidence.lower(),
    "evidence log must keep readiness claim tied to discovered accessibility/runtime display targets",
)
require(
    ".copilot-evidence/default-workflow-attempt.log" in doc,
    "silver-thread doc must name the full readiness evidence log path",
)
require(
    "final PR419 exact-head readiness handoff" in doc,
    "silver-thread doc must describe the final PR419 readiness evidence stage",
)
require(
    "stale PR388" not in doc and "PR #388" not in doc,
    "silver-thread doc must not preserve stale PR388 readiness language",
)

if errors:
    raise AssertionError("\n".join(errors))

print("PR419 readiness evidence contract satisfied")
PY
status=$?
assert_success "$status" "PR419 readiness evidence is final, exact-head, and bounded"

finish
