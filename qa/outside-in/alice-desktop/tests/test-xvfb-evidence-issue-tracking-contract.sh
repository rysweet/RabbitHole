#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-xvfb-evidence-issue-tracking-contract.sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

assert_file_absent() {
  local path=$1
  local label=$2
  if [ ! -e "$path" ]; then
    pass "$label"
  else
    fail "$label (unexpected path exists: $path)"
  fi
}

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

gated_evidence="$tmp_root/gated-evidence"
set +e
"$RUNNER" run alice-desktop-netbeans-package-smoke --evidence-dir "$gated_evidence" >"$tmp_root/gated.out" 2>"$tmp_root/gated.err"
status=$?
set -e
assert_exit_code "$status" 3 "unset gated Xvfb/package scenario exits as non-executed evidence"
gated_run_dir=$(single_child_dir "$gated_evidence/alice-desktop-netbeans-package-smoke")
status=$?
assert_success "$status" "unset gated Xvfb/package scenario creates one evidence directory"
assert_contains "$gated_run_dir/status.txt" '^outcome=gated-not-run$' "unset gated scenario records gated-not-run outcome"
assert_contains "$gated_run_dir/status.txt" '^skipMode=missing-gate$' "unset gated scenario records missing-gate skip mode"
assert_contains "$gated_run_dir/status.txt" '^executionStatus=not-run$' "unset gated scenario explicitly records that no command executed"
assert_contains "$gated_run_dir/status.txt" '^executionClaim=no-gui-execution$' "unset gated scenario explicitly prevents GUI execution claims"
assert_not_contains "$gated_run_dir/status.txt" '^commandLog=' "unset gated scenario does not point to command logs"
assert_file_absent "$gated_run_dir/command.log" "unset gated scenario does not create command.log"

prepare_evidence="$tmp_root/prepare-evidence"
"$RUNNER" run alice-desktop-netbeans-package-smoke --prepare-only --evidence-dir "$prepare_evidence" >"$tmp_root/prepare.out" 2>"$tmp_root/prepare.err"
status=$?
assert_success "$status" "prepare-only gated scenario exits successfully as intentional non-execution"
prepare_run_dir=$(single_child_dir "$prepare_evidence/alice-desktop-netbeans-package-smoke")
status=$?
assert_success "$status" "prepare-only gated scenario creates one evidence directory"
assert_contains "$prepare_run_dir/status.txt" '^outcome=gated-not-run$' "prepare-only scenario records gated-not-run outcome"
assert_contains "$prepare_run_dir/status.txt" '^skipMode=prepare-only$' "prepare-only scenario records prepare-only skip mode"
assert_contains "$prepare_run_dir/status.txt" '^executionStatus=not-run$' "prepare-only scenario explicitly records that no command executed"
assert_contains "$prepare_run_dir/status.txt" '^executionClaim=no-gui-execution$' "prepare-only scenario explicitly prevents GUI execution claims"
assert_not_contains "$prepare_run_dir/status.txt" '^commandLog=' "prepare-only scenario does not point to command logs"
assert_file_absent "$prepare_run_dir/command.log" "prepare-only scenario does not create command.log"

fake_bin="$tmp_root/bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/mvn" <<'SH'
#!/usr/bin/env bash
printf 'gated-command-ran\n'
printf 'argv=%s\n' "$*"
SH
chmod +x "$fake_bin/mvn"

enabled_evidence="$tmp_root/enabled-evidence"
set +e
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-project-io-smoke --evidence-dir "$enabled_evidence" >"$tmp_root/enabled.out" 2>"$tmp_root/enabled.err"
status=$?
set -e
assert_success "$status" "enabled gated project IO scenario executes"
enabled_run_dir=$(single_child_dir "$enabled_evidence/alice-desktop-project-io-smoke")
status=$?
assert_success "$status" "enabled gated project IO scenario creates one evidence directory"
assert_contains "$enabled_run_dir/status.txt" '^outcome=passed$' "enabled gated scenario records pass outcome"
assert_contains "$enabled_run_dir/status.txt" '^executionStatus=executed$' "enabled gated scenario explicitly records command execution"
assert_contains "$enabled_run_dir/status.txt" '^executionClaim=gated-command-executed$' "enabled gated scenario permits only a bounded command-executed claim"
assert_contains "$enabled_run_dir/status.txt" '^commandLog=command.log$' "enabled gated scenario points to command log"
assert_file_exists "$enabled_run_dir/command.log" "enabled gated scenario creates command.log"

finish
