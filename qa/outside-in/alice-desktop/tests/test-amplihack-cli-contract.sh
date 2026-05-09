#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-amplihack-cli-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

assert_contains "$REPO_ROOT/pyproject.toml" '^amplihack = "alice_qa_amplihack:main"$' "pyproject exposes the amplihack console script"
assert_contains "$REPO_ROOT/pyproject.toml" '^py-modules = \["alice_qa_amplihack"\]$' "pyproject includes the wrapper module"

(
  cd "$REPO_ROOT" &&
    PYTHONDONTWRITEBYTECODE=1 python3 -m alice_qa_amplihack alice-qa list
) >"$tmp_root/list.out" 2>"$tmp_root/list.err"
status=$?
assert_success "$status" "amplihack wrapper lists Alice QA scenarios"
assert_contains "$tmp_root/list.out" 'alice-desktop-launch[[:space:]]+xvfb-real-alice' "amplihack list output includes launch scenario"
assert_contains "$tmp_root/list.out" 'alice-desktop-save-load[[:space:]]+manual-evidence-required' "amplihack list output includes manual scenario"

(
  cd "$REPO_ROOT" &&
    PYTHONDONTWRITEBYTECODE=1 python3 -m alice_qa_amplihack alice-qa save-negative-contract
) >"$tmp_root/save-negative-contract.out" 2>"$tmp_root/save-negative-contract.err"
status=$?
assert_success "$status" "amplihack wrapper runs the Save negative artifact contract"
assert_contains "$tmp_root/save-negative-contract.out" 'missing-artifact is rejected' "amplihack Save negative contract reports missing artifact rejection"
assert_contains "$tmp_root/save-negative-contract.out" 'blocked-unknown-kind is rejected' "amplihack Save negative contract reports unknown blocker rejection"

(
  cd "$REPO_ROOT" &&
    PYTHONDONTWRITEBYTECODE=1 python3 -m alice_qa_amplihack alice-qa run alice-desktop-save-load --evidence-dir "$tmp_root/evidence"
) >"$tmp_root/run.out" 2>"$tmp_root/run.err"
status=$?
assert_success "$status" "amplihack wrapper prepares manual evidence"
run_dir=$(single_child_dir "$tmp_root/evidence/alice-desktop-save-load")
status=$?
assert_success "$status" "amplihack wrapper creates one evidence directory"
assert_file_exists "$run_dir/manual-evidence-checklist.txt" "amplihack wrapper writes manual checklist"
assert_file_exists "$run_dir/status.txt" "amplihack wrapper writes manual status"

(
  cd "$REPO_ROOT" &&
    PYTHONDONTWRITEBYTECODE=1 python3 -m alice_qa_amplihack alice-qa validate unexpected
) >"$tmp_root/bad.out" 2>"$tmp_root/bad.err"
status=$?
assert_exit_code "$status" 2 "amplihack wrapper rejects invalid validate arguments"
assert_contains "$tmp_root/bad.err" 'does not accept extra arguments' "amplihack wrapper explains invalid validate arguments"

finish
