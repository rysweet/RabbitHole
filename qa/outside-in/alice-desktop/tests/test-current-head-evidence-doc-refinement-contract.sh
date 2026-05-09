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

python3 - \
  "$HOWTO_DOC" \
  "$POST_OPEN_REFERENCE_DOC" \
  "$NONCLAIM_REFERENCE_DOC" \
  "$QA_REFERENCE_DOC" \
  >"$tmp_root/current-head-doc-refinement.out" \
  2>"$tmp_root/current-head-doc-refinement.err" <<'PY'
import re
import sys
from pathlib import Path

howto_path, post_open_path, nonclaim_path, qa_reference_path = map(Path, sys.argv[1:5])
texts = {
    "howto": howto_path.read_text(encoding="utf-8"),
    "post_open_reference": post_open_path.read_text(encoding="utf-8"),
    "nonclaim_reference": nonclaim_path.read_text(encoding="utf-8"),
    "qa_reference": qa_reference_path.read_text(encoding="utf-8"),
}
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


howto = texts["howto"]
post_open = texts["post_open_reference"]
qa_reference = texts["qa_reference"]

require("<pr-number>" in howto, "how-to recovery command must parameterize the PR number")
require("<pr-branch>" in howto, "how-to recovery command must parameterize the PR branch")
for stale_token in (
    "refs/pull/419",
    "origin/pr/419",
    "feat/issue-416",
    "rabbithole-wave7-accessibility-target-lane-follow",
):
    require(stale_token not in howto, f"how-to must not hard-code PR419 recovery token {stale_token!r}")

for metadata_token in ("gitHead", "originDevelopHead", "mergeBase"):
    for name, text in texts.items():
        require(
            metadata_token not in text,
            f"{name} must not describe runner-emitted environment metadata field {metadata_token!r}",
        )
require(
    "The current runner does not emit Git SHAs in `environment.txt`; reviewers record those externally." in post_open,
    "post-open reference must distinguish runner-emitted environment.txt from external Git SHA review metadata",
)
require(
    "`environment.txt`, so keep them in the PR notes, review notes, or external CI" in howto,
    "how-to must keep PR/develop/merge-base SHAs outside runner-emitted environment.txt metadata",
)

sampling_result_line = (
    "visible-rendering-pixel-observation.json OR "
    "visible-rendering-pixel-sampling-blocker.json"
)
require(sampling_result_line in howto, "how-to must show pixel observation OR blocker as a single-run result")
require(sampling_result_line in post_open, "post-open reference must show pixel observation OR blocker as a single-run result")
require(
    "Pixel sampling writes either the observation artifact or the blocker artifact, never both as the result for one run" in howto,
    "how-to must state pixel sampling writes observation or blocker, never both",
)
require(
    "Target readiness writes `visible-rendering-pixel-target-blocker.json` only when target identification is blocked." in howto,
    "how-to must include the target-readiness blocker in the artifact list wording",
)
require(
    "visible-rendering-pixel-target-blocker.json (when target readiness is blocked)" in howto,
    "how-to artifact list must name the target blocker separately from sampling results",
)
require(
    "target blocker JSON when target readiness is blocked" in post_open,
    "post-open current-head decision artifacts must include the target-readiness blocker",
)
require(
    "if rg '(<{7}|={7}|>{7})' docs qa pyproject.toml; then" in howto
    and "conflict markers remain" in howto
    and "should print no paths or marker" in howto,
    "how-to conflict-marker check must make rg no-match behavior explicit",
)

both_artifact_patterns = (
    r"visible-rendering-pixel-observation\.json\s*\n\s*visible-rendering-pixel-sampling-blocker\.json",
    r"visible-rendering-pixel-sampling-blocker\.json\s*\n\s*visible-rendering-pixel-observation\.json",
)
for name, text in (("howto", howto), ("post_open_reference", post_open)):
    for pattern in both_artifact_patterns:
        require(
            re.search(pattern, text) is None,
            f"{name} must not list pixel observation and sampling blocker as simultaneously emitted artifacts",
        )

for scenario_id in (
    "alice-desktop-procedure-edit-seam-smoke",
    "alice-desktop-procedure-edit-handoff-smoke",
):
    require(
        qa_reference.count(f"| `{scenario_id}` |") == 1,
        f"QA reference must list {scenario_id} exactly once",
    )

bounded_scope_terms = (
    "does not establish visible correctness",
    "does not execute Alice worlds",
    "do not launch a world or certify visual output",
)
nonclaim = texts["nonclaim_reference"]
for term in bounded_scope_terms:
    require(term in nonclaim, f"nonclaim reference must preserve bounded wording: {term}")

if errors:
    raise AssertionError("\n".join(errors))

print("current-head evidence documentation refinement contract satisfied")
PY
status=$?
assert_success "$status" "current-head evidence docs stay parameterized, bounded, and non-duplicative"

finish
