#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-run-window-docs-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
REVIEW_HOWTO="$REPO_ROOT/docs/howto/review-run-window-creation-wiring-contract.md"
QA_HOWTO="$REPO_ROOT/docs/howto/alice-desktop-outside-in-qa.md"
QA_REFERENCE="$REPO_ROOT/docs/reference/alice-desktop-outside-in-qa.md"
RUN_WINDOW_REFERENCE="$REPO_ROOT/docs/reference/run-window-creation-wiring-contract.md"
QA_README="$REPO_ROOT/qa/outside-in/alice-desktop/README.md"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

assert_cached_contains() {
  local content_var=$1
  local pattern=$2
  local label=$3

  if grep -Eq -- "$pattern" <<<"${!content_var}"; then
    pass "$label"
  else
    fail "$label (pattern not found: $pattern)"
  fi
}

read_cached_doc() {
  local path=$1

  if [ -f "$path" ]; then
    printf '%s' "$(<"$path")"
  fi
}

assert_file_exists "$REVIEW_HOWTO" "Run-window review how-to exists"
assert_file_exists "$QA_HOWTO" "Alice desktop QA how-to exists"
assert_file_exists "$QA_REFERENCE" "Alice desktop QA reference exists"
assert_file_exists "$RUN_WINDOW_REFERENCE" "Run-window contract reference exists"
assert_file_exists "$QA_README" "Alice desktop QA README exists"

REVIEW_HOWTO_CONTENT=$(read_cached_doc "$REVIEW_HOWTO")
QA_HOWTO_CONTENT=$(read_cached_doc "$QA_HOWTO")
QA_REFERENCE_CONTENT=$(read_cached_doc "$QA_REFERENCE")
RUN_WINDOW_REFERENCE_CONTENT=$(read_cached_doc "$RUN_WINDOW_REFERENCE")
QA_README_CONTENT=$(read_cached_doc "$QA_README")

assert_cached_contains REVIEW_HOWTO_CONTENT 'test-run-window-contract\.sh' "review how-to names the Run-window contract check"
assert_cached_contains REVIEW_HOWTO_CONTENT 'test-silver-thread-status-report\.sh' "review how-to names the silver-thread status check"
assert_cached_contains REVIEW_HOWTO_CONTENT 'status:silver_thread=covered_bounded' "review how-to requires bounded silver-thread coverage status"
assert_cached_contains REVIEW_HOWTO_CONTENT 'claim-boundary:<claim>=not_claimed' "review how-to requires claim-boundary output"
assert_cached_contains REVIEW_HOWTO_CONTENT 'merge readiness' "review how-to keeps contract evidence separate from merge readiness"
assert_cached_contains REVIEW_HOWTO_CONTENT 'Do not manually merge a PR' "review how-to blocks manual merge from bounded evidence"
assert_cached_contains REVIEW_HOWTO_CONTENT 'gap:save_reopen=not_covered_optional' "review how-to accepts optional Save/reopen gap marker"
assert_cached_contains REVIEW_HOWTO_CONTENT 'optional_gaps=1' "review how-to documents optional gap count"
assert_cached_contains REVIEW_HOWTO_CONTENT 'non-blocking output for this bounded lane' "review how-to keeps optional Save/reopen gap non-blocking"
assert_cached_contains REVIEW_HOWTO_CONTENT 'creative[[:space:]]+assessment' "review how-to excludes creative assessment claims"
assert_cached_contains REVIEW_HOWTO_CONTENT 'lesson-completion evidence' "review how-to excludes lesson-completion claims"
assert_cached_contains REVIEW_HOWTO_CONTENT 'scope excludes creative assessment and lesson completion' "review how-to keeps creative and lesson exclusions scoped"
assert_cached_contains REVIEW_HOWTO_CONTENT 'Do not add timeout wrappers' "review how-to preserves the no-timeout-wrapper command contract"
assert_cached_contains REVIEW_HOWTO_CONTENT 'intentionally passive until' "review how-to documents passive default evidence behavior"
assert_cached_contains REVIEW_HOWTO_CONTENT 'fail-closed' "review how-to documents configured evidence write failures"

assert_cached_contains QA_HOWTO_CONTENT 'Creative assessment and lesson completion are excluded by the Run-window' "QA how-to explains creative and lesson exclusions by scope"
assert_cached_contains QA_HOWTO_CONTENT 'not by dedicated `creative_assessment_claimed` or' "QA how-to avoids inventing a creative-assessment artifact boolean"
assert_cached_contains QA_HOWTO_CONTENT '`lesson_completion_claimed` artifact fields' "QA how-to avoids inventing a lesson-completion artifact boolean"

assert_cached_contains QA_REFERENCE_CONTENT 'creative-assessment' "QA reference excludes creative-assessment artifacts"
assert_cached_contains QA_REFERENCE_CONTENT 'lesson-completion[[:space:]]+artifact' "QA reference excludes lesson-completion artifacts"
assert_cached_contains QA_REFERENCE_CONTENT 'does not expose separate creative-assessment or' "QA reference documents absent creative-assessment boolean"
assert_cached_contains QA_REFERENCE_CONTENT 'lesson-completion booleans' "QA reference documents absent lesson-completion boolean"
assert_cached_contains QA_REFERENCE_CONTENT 'must not be inferred from a passing artifact' "QA reference blocks inference from passing artifacts"
assert_cached_contains QA_REFERENCE_CONTENT 'Run-window creation/wiring contract' "QA reference evidence table covers the Run-window contract"
assert_cached_contains QA_REFERENCE_CONTENT 'focused seam test output naming `EatmeRunWindowEvidenceTest`' "QA reference names the focused Run-window test evidence"

assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'test-run-window-contract\.sh' "Run-window reference names the shortest contract readiness check"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'not a broad desktop automation run' "Run-window reference keeps the readiness check bounded"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'does not define separate creative-assessment or' "Run-window reference documents absent creative-assessment boolean"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'lesson-completion booleans' "Run-window reference documents absent lesson-completion boolean"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'must not use the' "Run-window reference blocks using the artifact beyond scope"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'creative-assessment or lesson-completion evidence' "Run-window reference excludes creative and lesson evidence claims"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'passive no-op for' "Run-window reference documents unconfigured evidence behavior"
assert_cached_contains RUN_WINDOW_REFERENCE_CONTENT 'success-shaped runs' "Run-window reference documents fail-closed configured evidence behavior"

python3 - "$QA_README" <<'PY'
import sys
from pathlib import Path

readme = Path(sys.argv[1]).read_text(encoding="utf-8")
marker = "## Scenario authoring checklist"
if marker not in readme:
    raise AssertionError("README must keep the scenario authoring checklist section")
section = readme.split(marker, 1)[1]
if "\nrun-window-contract\n" not in section:
    raise AssertionError("README scenario authoring checklist must list run-window-contract")
PY
status=$?
assert_success "$status" "README scenario authoring checklist lists the Run-window contract workflow"

finish
