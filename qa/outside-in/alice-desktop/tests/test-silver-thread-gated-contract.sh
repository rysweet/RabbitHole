#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-silver-thread-gated-contract.sh
#
# Contract tests for the silver-thread-launch-build-run QA scenario.
# Verifies gated-command-smoke behavior through run-scenario.sh:
#   1. Gate-unset path produces evidence with gated-not-run outcome
#   2. Gate-enabled path with fake mvn captures the correct argv
#   3. Prepare-only path records intentional skip
#   4. Scenario YAML name field is present and non-empty
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
SCENARIO_ID=alice-desktop-silver-thread-launch-build-run
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

# ── 1. Gate-unset: scenario exits non-zero with gated-not-run evidence ────

gated_evidence="$tmp_root/gated-evidence"
"$RUNNER" run "$SCENARIO_ID" --evidence-dir "$gated_evidence" >"$tmp_root/gated.out" 2>"$tmp_root/gated.err"
status=$?
assert_exit_code "$status" 3 "silver-thread gated smoke exits 3 when gate is unset"
run_dir=$(single_child_dir "$gated_evidence/$SCENARIO_ID")
status=$?
assert_success "$status" "silver-thread gated smoke creates one evidence directory"
assert_file_exists "$run_dir/status.txt" "silver-thread gated smoke writes status.txt"
assert_file_exists "$run_dir/manual-evidence-checklist.txt" "silver-thread gated smoke writes fallback checklist"
assert_contains "$run_dir/status.txt" '^automationMode=gated-command-smoke$' "silver-thread status records automation mode"
assert_contains "$run_dir/status.txt" '^outcome=gated-not-run$' "silver-thread status records skipped command outcome"
assert_contains "$run_dir/status.txt" '^gate=ALICE_QA_RUN_GATED_SMOKES$' "silver-thread status names enabling variable"
assert_contains "$run_dir/status.txt" '^skipMode=missing-gate$' "silver-thread status records unset-gate skip mode"
assert_contains "$tmp_root/gated.err" 'pass --prepare-only' "silver-thread gate-unset stderr tells callers how to prepare intentionally"

# ── 2. Prepare-only: scenario records intentional skip ────────────────────

prepare_evidence="$tmp_root/prepare-evidence"
"$RUNNER" run "$SCENARIO_ID" --prepare-only --evidence-dir "$prepare_evidence" >"$tmp_root/prepare.out" 2>"$tmp_root/prepare.err"
status=$?
assert_success "$status" "silver-thread prepare-only records intentional skip successfully"
prepare_run_dir=$(single_child_dir "$prepare_evidence/$SCENARIO_ID")
status=$?
assert_success "$status" "silver-thread prepare-only creates one evidence directory"
assert_contains "$prepare_run_dir/status.txt" '^outcome=gated-not-run$' "prepare-only status records skipped command outcome"
assert_contains "$prepare_run_dir/status.txt" '^skipMode=prepare-only$' "prepare-only status records intentional skip mode"
assert_file_exists "$prepare_run_dir/manual-evidence-checklist.txt" "prepare-only writes fallback checklist"

# ── 3. Gate-enabled with fake mvn: scenario captures correct argv ─────────

fake_bin="$tmp_root/bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/mvn" <<'SH'
#!/usr/bin/env bash
printf 'gated-command-ran\n'
printf 'argv=%s\n' "$*"
SH
chmod +x "$fake_bin/mvn"

enabled_evidence="$tmp_root/enabled-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run "$SCENARIO_ID" --evidence-dir "$enabled_evidence" >"$tmp_root/enabled.out" 2>"$tmp_root/enabled.err"
status=$?
assert_success "$status" "silver-thread enabled gated smoke executes argv directly"
enabled_run_dir=$(single_child_dir "$enabled_evidence/$SCENARIO_ID")
status=$?
assert_success "$status" "silver-thread enabled gated smoke creates one evidence directory"
assert_file_exists "$enabled_run_dir/command.log" "silver-thread enabled gated smoke writes command.log"
assert_contains "$enabled_run_dir/command.log" 'gated-command-ran' "silver-thread enabled gated smoke captures command output"
assert_contains "$enabled_run_dir/command.log" 'SilverThreadLaunchBuildRunTest' "silver-thread smoke passes focused test selector as argv"
assert_contains "$enabled_run_dir/status.txt" '^outcome=passed$' "silver-thread enabled gated smoke records pass outcome"
assert_not_contains "$enabled_run_dir/status.txt" '^outcome=gated-not-run$' "silver-thread enabled gated smoke is not reported as a skip"
assert_contains "$enabled_run_dir/status.txt" '^exitCode=0$' "silver-thread enabled gated smoke records exit code"

# ── 4. Argv content verification: exact flags reach the command ───────────

assert_contains "$enabled_run_dir/command.log" '-DincludeSims=false' "silver-thread argv includes -DincludeSims=false"
assert_contains "$enabled_run_dir/command.log" '-Dinstall4j.skip' "silver-thread argv includes -Dinstall4j.skip"
assert_contains "$enabled_run_dir/command.log" '-DfailIfNoTests=false' "silver-thread argv includes -DfailIfNoTests=false"
assert_contains "$enabled_run_dir/command.log" '-Dsurefire.failIfNoSpecifiedTests=false' "silver-thread argv includes surefire flag"
assert_contains "$enabled_run_dir/command.log" 'core/ide' "silver-thread argv targets core/ide module"

# ── 5. Scenario YAML has name field (title) ───────────────────────────────

scenario_file="$BASE_DIR/scenarios/silver-thread-launch-build-run.yaml"
assert_file_exists "$scenario_file" "silver-thread scenario YAML file exists"
assert_contains "$scenario_file" '^title:' "silver-thread scenario YAML has title field"
assert_contains "$scenario_file" '^id: alice-desktop-silver-thread-launch-build-run$' "silver-thread scenario YAML has correct id"
assert_contains "$scenario_file" '^workflow: silver-thread-launch-build-run$' "silver-thread scenario YAML has correct workflow"
assert_contains "$scenario_file" '^automationMode: gated-command-smoke$' "silver-thread scenario YAML uses gated-command-smoke mode"

# ── 6. Scenario lists silver-thread tags ──────────────────────────────────

assert_contains "$scenario_file" 'silver-thread' "silver-thread scenario includes silver-thread tag"
assert_contains "$scenario_file" 'end-to-end' "silver-thread scenario includes end-to-end tag"

finish
