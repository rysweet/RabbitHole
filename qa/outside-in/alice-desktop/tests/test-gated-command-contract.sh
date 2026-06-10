#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
set -eu

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
set +e
"$RUNNER" run alice-desktop-netbeans-package-smoke --evidence-dir "$gated_evidence" >"$tmp_root/gated.out" 2>"$tmp_root/gated.err"
status=$?
set -e
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
set +e
"$RUNNER" run alice-desktop-package-install-smoke --evidence-dir "$package_evidence" >"$tmp_root/package.out" 2>"$tmp_root/package.err"
status=$?
set -e
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
cat > "$fake_bin/timeout" <<'SH'
#!/usr/bin/env bash
printf 'timeout should not wrap project-io smoke\n' >&2
exit 99
SH
chmod +x "$fake_bin/timeout"

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
assert_contains "$enabled_run_dir/command.log" 'IoUtilitiesTest' "saving, reopening, editing, saving again, reopening again, and exporting smoke passes focused archive test selector as argv"
assert_contains "$enabled_run_dir/status.txt" '^outcome=passed$' "enabled gated command records pass outcome"
assert_not_contains "$enabled_run_dir/status.txt" '^outcome=gated-not-run$' "enabled gated command is not reported as a skip"
assert_contains "$enabled_run_dir/status.txt" '^exitCode=0$' "enabled gated command records exit code"
assert_contains "$enabled_run_dir/status.txt" '^timeoutPolicy=none$' "project IO smoke records no-timeout policy"
assert_not_contains "$enabled_run_dir/status.txt" '^timeoutSeconds=' "project IO smoke omits workflow timeout"
rm -f "$fake_bin/timeout"

exported_ant_evidence="$tmp_root/exported-ant-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-exported-project-smoke --evidence-dir "$exported_ant_evidence" >"$tmp_root/exported-ant.out" 2>"$tmp_root/exported-ant.err"
status=$?
assert_success "$status" "enabled exported project Ant build smoke executes argv directly"
exported_ant_run_dir=$(single_child_dir "$exported_ant_evidence/alice-desktop-exported-project-smoke")
status=$?
assert_success "$status" "enabled exported project Ant build scenario creates one evidence directory"
assert_contains "$exported_ant_run_dir/command.log" 'Alice3ProjectTemplateAntSmokeTest' "exported project Ant build smoke selects the actual Ant/template smoke test"
assert_contains "$exported_ant_run_dir/command.log" '^-DincludeSims=false|-DincludeSims=false' "exported project Ant build smoke is no-Sims"
assert_contains "$exported_ant_run_dir/command.log" '-DfailIfNoTests=false' "exported project Ant build smoke keeps focused reactor fail-if-no-tests flag"
assert_not_contains "$exported_ant_run_dir/command.log" 'ProjectCodeGeneratorStandaloneProjectTest' "exported project Ant build smoke must not stop at standalone generator coverage"
assert_contains "$exported_ant_run_dir/status.txt" '^outcome=passed$' "exported project Ant build smoke records pass outcome when the focused command exits zero"

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

menu_action_evidence="$tmp_root/menu-action-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-menu-action-smoke --evidence-dir "$menu_action_evidence" >"$tmp_root/menu-action.out" 2>"$tmp_root/menu-action.err"
status=$?
assert_success "$status" "enabled menu/action smoke executes argv directly"
menu_action_run_dir=$(single_child_dir "$menu_action_evidence/alice-desktop-menu-action-smoke")
status=$?
assert_success "$status" "enabled menu/action scenario creates one evidence directory"
assert_contains "$menu_action_run_dir/command.log" 'AliceMenuBarContractTest' "menu/action smoke passes focused test selector as argv"
assert_contains "$menu_action_run_dir/status.txt" '^outcome=passed$' "menu/action smoke records pass outcome"

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

tweedle_this_evidence="$tmp_root/tweedle-this-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-tweedle-decoder-this-call-smoke --evidence-dir "$tweedle_this_evidence" >"$tmp_root/tweedle-this.out" 2>"$tmp_root/tweedle-this.err"
status=$?
assert_success "$status" "enabled Tweedle decoder this-call smoke executes argv directly"
tweedle_this_run_dir=$(single_child_dir "$tweedle_this_evidence/alice-desktop-tweedle-decoder-this-call-smoke")
status=$?
assert_success "$status" "enabled Tweedle decoder this-call scenario creates one evidence directory"
assert_contains "$tweedle_this_run_dir/command.log" 'zeroArgumentThisMethodCallDecodeCreatesMethodInvocation' "this-call smoke passes focused positive decoder test selector as argv"
assert_contains "$tweedle_this_run_dir/status.txt" '^outcome=passed$' "this-call smoke records pass outcome"

tweedle_boundary_evidence="$tmp_root/tweedle-boundary-evidence"
PATH="$fake_bin:$PATH" ALICE_QA_RUN_GATED_SMOKES=1 \
  "$RUNNER" run alice-desktop-tweedle-decoder-boundary-smoke --evidence-dir "$tweedle_boundary_evidence" >"$tmp_root/tweedle-boundary.out" 2>"$tmp_root/tweedle-boundary.err"
status=$?
assert_success "$status" "enabled Tweedle decoder boundary smoke executes argv directly"
tweedle_boundary_run_dir=$(single_child_dir "$tweedle_boundary_evidence/alice-desktop-tweedle-decoder-boundary-smoke")
status=$?
assert_success "$status" "enabled Tweedle decoder boundary scenario creates one evidence directory"
assert_contains "$tweedle_boundary_run_dir/command.log" 'zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall' "boundary smoke passes focused rejection decoder test selector as argv"
assert_contains "$tweedle_boundary_run_dir/status.txt" '^outcome=passed$' "boundary smoke records pass outcome"

finish
