#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-run-window-docs-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
REVIEW_HOWTO="$REPO_ROOT/docs/howto/review-run-window-creation-wiring-contract.md"
QA_HOWTO="$REPO_ROOT/docs/howto/alice-desktop-outside-in-qa.md"
QA_REFERENCE="$REPO_ROOT/docs/reference/alice-desktop-outside-in-qa.md"
RUN_WINDOW_REFERENCE="$REPO_ROOT/docs/reference/run-window-creation-wiring-contract.md"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

assert_file_exists "$REVIEW_HOWTO" "Run-window review how-to exists"
assert_file_exists "$QA_HOWTO" "Alice desktop QA how-to exists"
assert_file_exists "$QA_REFERENCE" "Alice desktop QA reference exists"
assert_file_exists "$RUN_WINDOW_REFERENCE" "Run-window contract reference exists"

assert_contains "$REVIEW_HOWTO" 'test-run-window-contract\.sh' "review how-to names the Run-window contract check"
assert_contains "$REVIEW_HOWTO" 'test-silver-thread-status-report\.sh' "review how-to names the silver-thread status check"
assert_contains "$REVIEW_HOWTO" 'status:silver_thread=covered_bounded' "review how-to requires bounded silver-thread coverage status"
assert_contains "$REVIEW_HOWTO" 'claim-boundary:<claim>=not_claimed' "review how-to requires claim-boundary output"
assert_contains "$REVIEW_HOWTO" 'merge readiness' "review how-to keeps contract evidence separate from merge readiness"
assert_contains "$REVIEW_HOWTO" 'Do not manually merge a PR' "review how-to blocks manual merge from bounded evidence"
assert_contains "$REVIEW_HOWTO" 'gap:save_reopen=not_covered_optional' "review how-to accepts optional Save/reopen gap marker"
assert_contains "$REVIEW_HOWTO" 'optional_gaps=1' "review how-to documents optional gap count"
assert_contains "$REVIEW_HOWTO" 'non-blocking output for this bounded lane' "review how-to keeps optional Save/reopen gap non-blocking"
assert_contains "$REVIEW_HOWTO" 'creative[[:space:]]+assessment' "review how-to excludes creative assessment claims"
assert_contains "$REVIEW_HOWTO" 'lesson-completion evidence' "review how-to excludes lesson-completion claims"
assert_contains "$REVIEW_HOWTO" 'Do not add timeout wrappers' "review how-to preserves the no-timeout-wrapper command contract"

assert_contains "$QA_HOWTO" 'Creative assessment and lesson completion are excluded by the Run-window' "QA how-to explains creative and lesson exclusions by scope"
assert_contains "$QA_HOWTO" 'not by dedicated `creative_assessment_claimed` or' "QA how-to avoids inventing a creative-assessment artifact boolean"
assert_contains "$QA_HOWTO" '`lesson_completion_claimed` artifact fields' "QA how-to avoids inventing a lesson-completion artifact boolean"

assert_contains "$QA_REFERENCE" 'creative-assessment' "QA reference excludes creative-assessment artifacts"
assert_contains "$QA_REFERENCE" 'lesson-completion[[:space:]]+artifact' "QA reference excludes lesson-completion artifacts"
assert_contains "$QA_REFERENCE" 'does not expose separate creative-assessment or' "QA reference documents absent creative-assessment boolean"
assert_contains "$QA_REFERENCE" 'lesson-completion booleans' "QA reference documents absent lesson-completion boolean"
assert_contains "$QA_REFERENCE" 'must not be inferred from a passing artifact' "QA reference blocks inference from passing artifacts"

assert_contains "$RUN_WINDOW_REFERENCE" 'test-run-window-contract\.sh' "Run-window reference names the shortest contract readiness check"
assert_contains "$RUN_WINDOW_REFERENCE" 'not a broad desktop automation run' "Run-window reference keeps the readiness check bounded"
assert_contains "$RUN_WINDOW_REFERENCE" 'does not define separate creative-assessment or' "Run-window reference documents absent creative-assessment boolean"
assert_contains "$RUN_WINDOW_REFERENCE" 'lesson-completion booleans' "Run-window reference documents absent lesson-completion boolean"
assert_contains "$RUN_WINDOW_REFERENCE" 'must not use the' "Run-window reference blocks using the artifact beyond scope"
assert_contains "$RUN_WINDOW_REFERENCE" 'creative-assessment or lesson-completion evidence' "Run-window reference excludes creative and lesson evidence claims"

finish
