#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
RUNNER="$BASE_DIR/runners/run-scenario.sh"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
package_marker="$REPO_ROOT/installer/target/qa-package-install-smoke-marker.txt"
trap 'rm -rf "$tmp_root"; rm -f "$package_marker"' EXIT

gated_evidence="$tmp_root/gated-evidence"
"$RUNNER" run alice-desktop-netbeans-package-smoke --evidence-dir "$gated_evidence" >"$tmp_root/gated.out" 2>"$tmp_root/gated.err"
status=$?
assert_exit_code "$status" 3 "gated command scenario is non-success by default when the gate is unset"
run_dir=$(single_child_dir "$gated_evidence/alice-desktop-netbeans-package-smoke")
status=$?
assert_success "$status" "gated command scenario creates one evidence directory"
assert_file_exists "$run_dir/status.txt" "gated command scenario writes status.txt"
assert_file_exists "$run_dir/manual-evidence-checklist.txt" "gated command scenario writes fallback checklist"
assert_contains "$run_dir/status.txt" '^automationMode=gated-command-smoke$' "gated status records automation mode"
assert_contains "$run_dir/status.txt" '^outcome=gated-not-run$' "gated status records skipped command outcome"
assert_contains "$run_dir/status.txt" '^gate=ALICE_QA_RUN_GATED_SMOKES$' "gated status names enabling variable"
assert_contains "$run_dir/status.txt" '^skipMode=missing-gate$' "gated status records unset-gate skip mode"
assert_contains "$tmp_root/gated.err" 'pass --prepare-only' "default gated skip tells callers how to prepare intentionally"

package_evidence="$tmp_root/package-evidence"
"$RUNNER" run alice-desktop-package-install-smoke --evidence-dir "$package_evidence" >"$tmp_root/package.out" 2>"$tmp_root/package.err"
status=$?
assert_exit_code "$status" 3 "package/install smoke is non-success by default when the gate is unset"
package_run_dir=$(single_child_dir "$package_evidence/alice-desktop-package-install-smoke")
status=$?
assert_success "$status" "package/install gated scenario creates one evidence directory"
assert_contains "$package_run_dir/status.txt" '^outcome=gated-not-run$' "package/install status records skipped command outcome"

prepare_evidence="$tmp_root/prepare-evidence"
"$RUNNER" run alice-desktop-netbeans-package-smoke --prepare-only --evidence-dir "$prepare_evidence" >"$tmp_root/prepare.out" 2>"$tmp_root/prepare.err"
status=$?
assert_success "$status" "prepare-only gated command scenario records intentional skip successfully"
prepare_run_dir=$(single_child_dir "$prepare_evidence/alice-desktop-netbeans-package-smoke")
status=$?
assert_success "$status" "prepare-only gated command creates one evidence directory"
assert_contains "$prepare_run_dir/status.txt" '^outcome=gated-not-run$' "prepare-only status records skipped command outcome"
assert_contains "$prepare_run_dir/status.txt" '^skipMode=prepare-only$' "prepare-only status records intentional skip mode"
assert_file_exists "$prepare_run_dir/manual-evidence-checklist.txt" "prepare-only gated command writes fallback checklist"

fake_bin="$tmp_root/bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/mvn" <<'SH'
#!/usr/bin/env bash
printf 'gated-command-ran\n'
printf 'argv=%s\n' "$*"
if [ -n "${ALICE_QA_FAKE_PACKAGE_MARKER:-}" ]; then
  mkdir -p "$(dirname -- "$ALICE_QA_FAKE_PACKAGE_MARKER")"
  printf 'fake package artifact\n' > "$ALICE_QA_FAKE_PACKAGE_MARKER"
fi
SH
chmod +x "$fake_bin/mvn"

enabled_evidence="$tmp_root/enabled-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-project-io-smoke --evidence-dir "$enabled_evidence" >"$tmp_root/enabled.out" 2>"$tmp_root/enabled.err"
status=$?
assert_success "$status" "enabled gated command scenario executes argv directly"
enabled_run_dir=$(single_child_dir "$enabled_evidence/alice-desktop-project-io-smoke")
status=$?
assert_success "$status" "enabled gated command scenario creates one evidence directory"
assert_file_exists "$enabled_run_dir/command.log" "enabled gated command scenario writes command.log"
assert_contains "$enabled_run_dir/command.log" 'gated-command-ran' "enabled gated command captures command output"
assert_contains "$enabled_run_dir/status.txt" '^outcome=passed$' "enabled gated command records pass outcome"
assert_not_contains "$enabled_run_dir/status.txt" '^outcome=gated-not-run$' "enabled gated command is not reported as a skip"
assert_contains "$enabled_run_dir/status.txt" '^exitCode=0$' "enabled gated command records exit code"

package_enabled_evidence="$tmp_root/package-enabled-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 ALICE_QA_FAKE_PACKAGE_MARKER="$package_marker" \
  "$RUNNER" run alice-desktop-package-install-smoke --evidence-dir "$package_enabled_evidence" >"$tmp_root/package-enabled.out" 2>"$tmp_root/package-enabled.err"
status=$?
assert_success "$status" "enabled package/install smoke runs package wrapper from repo root"
package_enabled_run_dir=$(single_child_dir "$package_enabled_evidence/alice-desktop-package-install-smoke")
status=$?
assert_success "$status" "enabled package/install scenario creates one evidence directory"
assert_contains "$package_enabled_run_dir/command.log" 'installer/target.*qa-package-install-smoke-marker.txt' "package/install smoke lists package artifacts"
assert_contains "$package_enabled_run_dir/status.txt" '^outcome=passed$' "package/install smoke records pass outcome"

wizard_evidence="$tmp_root/wizard-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-wizard-palette-completion-smoke --evidence-dir "$wizard_evidence" >"$tmp_root/wizard.out" 2>"$tmp_root/wizard.err"
status=$?
assert_success "$status" "enabled wizard/palette/completion smoke executes argv directly"
wizard_run_dir=$(single_child_dir "$wizard_evidence/alice-desktop-wizard-palette-completion-smoke")
status=$?
assert_success "$status" "enabled wizard/palette/completion scenario creates one evidence directory"
assert_contains "$wizard_run_dir/command.log" 'Alice3ProjectTemplateWizardIteratorTest' "wizard smoke passes focused test selector as argv"
assert_contains "$wizard_run_dir/command.log" 'Alice3CompletionItemTest' "completion smoke passes focused test selector as argv"
assert_contains "$wizard_run_dir/status.txt" '^outcome=passed$' "wizard smoke records pass outcome"

finish
