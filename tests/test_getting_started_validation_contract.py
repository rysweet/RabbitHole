"""Contract tests for executable Getting Started validation.

These tests specify the validator that will make the documented fresh-checkout
setup path executable. They intentionally avoid running Maven or launching the
desktop; the shell script owns that end-to-end behavior.
"""
import re
import stat
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
VALIDATOR_PATH = REPO_ROOT / "scripts" / "validate-getting-started.sh"
README_PATH = REPO_ROOT / "README.md"
GETTING_STARTED_PATH = REPO_ROOT / "docs" / "getting-started.md"
TESTING_DOC_PATH = REPO_ROOT / "docs" / "testing.md"
ALICE_TEST_WORKFLOW_PATH = (
    REPO_ROOT / ".github" / "workflows" / "alice-test-ci.yml"
)
SETUP_XVFB_ACTION_PATH = REPO_ROOT / ".github" / "actions" / "setup-xvfb" / "action.yml"

HEADLESS_MAVEN_FLAGS = (
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-Dcheckstyle.skip",
    "-Djava.awt.headless=true",
    "clean",
    "install",
)
LAUNCH_MAVEN_FLAGS = (
    "-DincludeSims=false",
    "exec:java",
    "-Dalice-ide",
)
HEADED_MAVEN_FLAGS = (
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-Dcheckstyle.skip",
    "-DskipTests",
    "-Djava.awt.headless=false",
    "clean",
    "install",
)
HEADED_GUI_LAUNCH_FLAGS = (
    "-DincludeSims=false",
    "-Djava.awt.headless=false",
    "exec:java",
    "-Dalice-ide",
)
EXPECTED_HEADLESS_GUI_MESSAGE = (
    "Alice desktop launch requires a graphical environment."
)
SUBMODULE_FIX_COMMAND = "git submodule update --init tweedle-lang"
SETUP_XVFB_ACTION_OUTPUT = "steps.setup-xvfb.outputs.xvfb-run"


@lru_cache(maxsize=None)
def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def script_text() -> str:
    if not VALIDATOR_PATH.exists():
        raise AssertionError(
            "Expected executable validator at scripts/validate-getting-started.sh"
        )
    return read_text(VALIDATOR_PATH)


def function_body(source: str, function_name: str) -> str:
    pattern = rf"(?ms)^{re.escape(function_name)}\(\) \{{\n(?P<body>.*?)(?=^\}}\n)"
    match = re.search(pattern, source)
    if match is None:
        raise AssertionError(f"Missing shell function: {function_name}")
    return match.group("body")


def setup_xvfb_action_text() -> str:
    if not SETUP_XVFB_ACTION_PATH.exists():
        raise AssertionError(
            "Expected shared Xvfb setup action at .github/actions/setup-xvfb/action.yml"
        )
    return read_text(SETUP_XVFB_ACTION_PATH)


def workflow_job_block(workflow: str, job_name: str) -> str:
    match = re.search(
        rf"(?ms)^  {re.escape(job_name)}:\n(?P<body>.*?)(?=^  [A-Za-z0-9_-]+:\n|\Z)",
        workflow,
    )
    if match is None:
        raise AssertionError(f"Missing workflow job: {job_name}")
    return match.group("body")


class GettingStartedValidatorFileContract(unittest.TestCase):
    def test_validator_script_exists_at_documented_path(self) -> None:
        self.assertTrue(
            VALIDATOR_PATH.exists(),
            "Expected executable validator at scripts/validate-getting-started.sh",
        )

    def test_validator_script_is_executable(self) -> None:
        self.assertTrue(
            VALIDATOR_PATH.exists(),
            "Expected executable validator at scripts/validate-getting-started.sh",
        )
        mode = VALIDATOR_PATH.stat().st_mode
        executable_bits = stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH
        self.assertTrue(
            mode & executable_bits,
            "scripts/validate-getting-started.sh must have an executable bit set.",
        )

    def test_validator_uses_bash_strict_mode_and_no_dynamic_eval(self) -> None:
        source = script_text()
        self.assertTrue(source.startswith("#!/usr/bin/env bash"))
        self.assertIn("set -euo pipefail", source)
        self.assertNotRegex(source, r"(?m)^\s*eval\b")
        self.assertNotIn("${var@P}", source)


class GettingStartedValidatorArgumentContract(unittest.TestCase):
    def test_usage_documents_only_supported_flags(self) -> None:
        source = script_text()

        for flag in ("--headless", "--gui", "--all", "--help"):
            with self.subTest(flag=flag):
                self.assertIn(flag, source)

        self.assertNotIn("--skip-build", source)
        self.assertNotIn("--force-gui", source)

    def test_unknown_flags_fail_closed(self) -> None:
        source = script_text()

        self.assertRegex(source, r"(?i)unknown (flag|option)")
        self.assertRegex(source, r"exit\s+2\b|return\s+2\b")

    def test_default_lane_is_headless(self) -> None:
        source = script_text()

        self.assertRegex(source, r"mode=.*headless|lane=.*headless|HEADLESS")
        self.assertIn("--headless", source)


class GettingStartedValidatorCheckoutContract(unittest.TestCase):
    def test_validator_resolves_git_root_before_running_checks(self) -> None:
        source = script_text()

        self.assertIn("git rev-parse --show-toplevel", source)
        self.assertRegex(source, r"\bcd\b.*repo_root|\bcd\b.*REPO_ROOT")

    def test_validator_fails_clearly_when_tweedle_submodule_is_missing(self) -> None:
        source = script_text()

        self.assertIn("tweedle-lang", source)
        self.assertIn("tweedle-lang/Grammar", source)
        self.assertIn(SUBMODULE_FIX_COMMAND, source)
        self.assertRegex(
            source, r"(?i)submodule.*(missing|not initialized|uninitialized)"
        )

    def test_validator_checks_submodule_before_invoking_maven(self) -> None:
        source = script_text()
        submodule_check = min(
            index
            for index in (
                source.find("tweedle-lang/Grammar"),
                source.find("tweedle-lang"),
            )
            if index != -1
        )
        maven_invocation = source.find("mvn")

        self.assertGreaterEqual(
            submodule_check, 0, "Submodule validation must be present."
        )
        self.assertGreater(
            maven_invocation,
            submodule_check,
            "Maven must not run before submodule validation.",
        )


class GettingStartedValidatorHeadlessContract(unittest.TestCase):
    def test_headless_lane_runs_documented_no_sims_build_command(self) -> None:
        text = re.sub(r"\s+", " ", script_text())

        self.assertIn("mvn", text)
        for token in HEADLESS_MAVEN_FLAGS:
            with self.subTest(token=token):
                self.assertIn(token, text)

    def test_headless_lane_uses_fixed_maven_command_array(self) -> None:
        source = script_text()

        self.assertRegex(
            source, r"\bmvn_cmd=\(|\bheadless_maven=\(|\bMAVEN_HEADLESS_CMD=\("
        )
        self.assertNotRegex(source, r"(?m)^\s*(mvn|./mvnw)\s+\$\{")

    def test_headless_launch_probe_uses_documented_no_sims_launch_path(self) -> None:
        text = re.sub(r"\s+", " ", script_text())

        self.assertIn("alice-ide", text)
        self.assertRegex(text, r"\bcd\b .*alice-ide|\balice-ide\b")
        for token in LAUNCH_MAVEN_FLAGS:
            with self.subTest(token=token):
                self.assertIn(token, text)

    def test_headless_launch_probe_accepts_only_expected_gui_boundary(self) -> None:
        source = script_text()

        self.assertIn(EXPECTED_HEADLESS_GUI_MESSAGE, source)
        self.assertRegex(source, r"mktemp")
        self.assertRegex(source, r"trap .*rm")
        self.assertRegex(source, r"(?i)(unexpected|failed).*launch|launch.*(unexpected|failed)")


class GettingStartedValidatorGuiContract(unittest.TestCase):
    def test_gui_lane_is_gated_on_actual_display_capability(self) -> None:
        source = script_text()

        self.assertRegex(
            source, r"DISPLAY|WAYLAND_DISPLAY|GraphicsEnvironment|osascript|xset|xdpyinfo"
        )
        self.assertRegex(
            source, r"(?i)(no graphical|no desktop|gui.*unavailable|display.*unavailable)"
        )

    def test_explicit_gui_unavailability_fails_but_all_mode_can_skip(self) -> None:
        source = script_text()

        self.assertIn("--gui", source)
        self.assertIn("--all", source)
        self.assertRegex(source, r"(?i)skip")
        self.assertRegex(source, r"(?i)(explicit|requested).*gui|gui.*(explicit|requested)")

    def test_macos_apple_silicon_gui_blocker_is_documented_in_script(self) -> None:
        source = script_text()

        self.assertRegex(source, r"Darwin|macOS")
        self.assertRegex(source, r"arm64|aarch64|Apple Silicon")
        self.assertRegex(source, r"(?i)blocked")

    def test_gui_capability_check_forces_java_awt_non_headless_mode(self) -> None:
        source = script_text()
        body = function_body(source, "check_gui_capability")

        self.assertIn("-Djava.awt.headless=false", body)
        self.assertNotIn("-Djava.awt.headless=true", body)
        self.assertRegex(
            body,
            r"java\b.*-Djava\.awt\.headless=false.*AwtDisplayCheck\.java",
        )

    def test_gui_launch_probe_forces_non_headless_maven_launch(self) -> None:
        source = script_text()
        body = function_body(source, "run_gui_launch")
        normalized = re.sub(r"\s+", " ", body)

        for token in HEADED_GUI_LAUNCH_FLAGS:
            with self.subTest(token=token):
                self.assertIn(token, normalized)

        self.assertNotIn("-Djava.awt.headless=true", body)
        self.assertIn("run_with_timeout", body)
        self.assertIn("LAUNCH_TIMEOUT_SECONDS", body)
        self.assertIn("124", body)

    def test_gui_lane_runs_non_headless_maven_validation_before_launch(self) -> None:
        source = script_text()
        body = function_body(source, "run_gui_maven_validation")
        lane_body = function_body(source, "run_gui_lane")
        normalized = re.sub(r"\s+", " ", body)

        for token in HEADED_MAVEN_FLAGS:
            with self.subTest(token=token):
                self.assertIn(token, normalized)
        self.assertLess(
            lane_body.index("run_gui_maven_validation"),
            lane_body.index("run_gui_launch"),
        )

    def test_all_lane_runs_non_headless_maven_validation_before_gui_launch(self) -> None:
        source = script_text()
        body = function_body(source, "run_all_lane")

        self.assertLess(
            body.index("run_gui_maven_validation"),
            body.index("run_gui_launch"),
        )

    def test_timeout_kills_stubborn_gui_processes_after_grace_period(self) -> None:
        source = script_text()
        body = function_body(source, "run_with_timeout")
        tree_body = function_body(source, "process_tree_pids")
        signal_body = function_body(source, "send_signal_to_pids")

        self.assertIn("process_tree_pids \"${command_pid}\"", body)
        self.assertIn("send_signal_to_pids TERM \"${timed_out_pids[@]}\"", body)
        self.assertIn("grace_seconds", body)
        self.assertIn("any_pid_alive \"${timed_out_pids[@]}\"", body)
        self.assertIn("send_signal_to_pids KILL \"${timed_out_pids[@]}\"", body)
        self.assertIn("pgrep -P \"${root_pid}\"", tree_body)
        self.assertIn("kill \"-${signal}\" \"${pid}\"", signal_body)
        self.assertLess(
            body.index("send_signal_to_pids TERM \"${timed_out_pids[@]}\""),
            body.index("send_signal_to_pids KILL \"${timed_out_pids[@]}\""),
        )


class SharedXvfbSetupActionContract(unittest.TestCase):
    def test_shared_xvfb_setup_action_exists_at_workflow_local_path(self) -> None:
        self.assertTrue(
            SETUP_XVFB_ACTION_PATH.exists(),
            "Expected shared Xvfb setup action at .github/actions/setup-xvfb/action.yml",
        )

    def test_shared_xvfb_setup_action_uses_composite_strict_bash(self) -> None:
        source = setup_xvfb_action_text()

        self.assertIn("using: composite", source)
        self.assertIn("shell: bash", source)
        self.assertIn("set -euo pipefail", source)

    def test_shared_xvfb_setup_action_installs_only_ubuntu_xvfb_from_apt(self) -> None:
        source = setup_xvfb_action_text()

        self.assertIn("sudo apt-get update", source)
        self.assertIn("sudo apt-get install -y --no-install-recommends xvfb", source)
        self.assertNotRegex(source, r"\b(curl|wget|npm|pip|brew|dnf|yum)\b")

    def test_shared_xvfb_setup_action_exposes_absolute_xvfb_run_output(self) -> None:
        source = setup_xvfb_action_text()
        normalized = re.sub(r"\s+", " ", source)

        self.assertIn("xvfb-run", source)
        self.assertRegex(source, r"command\s+-v\s+xvfb-run")
        self.assertRegex(source, r"case\s+\"\$\{xvfb_run\}\"")
        self.assertRegex(source, r"/\*\)")
        self.assertNotRegex(source, r"\*/\)")
        self.assertRegex(source, r"GITHUB_OUTPUT")
        self.assertRegex(normalized, r"outputs: .*xvfb-run:")

    def test_shared_xvfb_setup_action_does_not_accept_arbitrary_execution_inputs(self) -> None:
        source = setup_xvfb_action_text()

        self.assertNotRegex(source, r"(?m)^inputs:\s*$")
        self.assertNotRegex(source, r"(?m)^\s*eval\b")
        self.assertNotIn("${var@P}", source)


class GettingStartedValidationDocsContract(unittest.TestCase):
    def test_readme_exposes_validator_and_lanes(self) -> None:
        text = read_text(README_PATH)
        normalized = " ".join(text.split())

        self.assertIn("./scripts/validate-getting-started.sh", text)
        self.assertIn("./scripts/validate-getting-started.sh --gui", text)
        self.assertIn("python3 alice_qa.py getting-started validate --headless", text)
        self.assertIn("--all", text)
        self.assertIn("known platform blocker", normalized)

    def test_getting_started_docs_name_commands_and_failure_guidance(self) -> None:
        text = read_text(GETTING_STARTED_PATH)

        for command in (
            "./scripts/validate-getting-started.sh",
            "./scripts/validate-getting-started.sh --headless",
            "./scripts/validate-getting-started.sh --gui",
            "./scripts/validate-getting-started.sh --all",
            "python3 alice_qa.py getting-started validate --headless",
            SUBMODULE_FIX_COMMAND,
            EXPECTED_HEADLESS_GUI_MESSAGE,
        ):
            with self.subTest(command=command):
                self.assertIn(command, text)

        for token in HEADLESS_MAVEN_FLAGS:
            with self.subTest(token=token):
                self.assertIn(token, text)

        for token in LAUNCH_MAVEN_FLAGS:
            with self.subTest(token=token):
                self.assertIn(token, text)

    def test_testing_docs_define_skip_fail_and_block_semantics(self) -> None:
        text = read_text(TESTING_DOC_PATH)
        self.assertIn("Getting Started validation lanes", text)
        self.assertIn("Getting Started validation lanes", text)
        self.assertIn("python3 alice_qa.py getting-started validate", text)
        self.assertIn("Missing `tweedle-lang` or `tweedle-lang/Grammar`", text)
        self.assertIn("No desktop display", text)
        self.assertIn("Unknown validator flag", text)
        self.assertIn("known platform blocker", text)
        self.assertIn("--headless", text)
        self.assertIn("--gui", text)
        self.assertIn("--all", text)

    def test_docs_link_the_validator_surfaces(self) -> None:
        readme = read_text(README_PATH)
        getting_started = read_text(GETTING_STARTED_PATH)
        testing = read_text(TESTING_DOC_PATH)

        self.assertIn("docs/getting-started.md#validate-this-checkout", readme)
        self.assertIn("docs/testing.md#getting-started-validation-lanes", readme)
        self.assertIn("## Validate this checkout", getting_started)
        self.assertIn("## Getting Started validation lanes", testing)

    def test_docs_define_current_headless_and_headed_xvfb_ci_semantics(self) -> None:
        combined = "\n".join(
            read_text(path)
            for path in (README_PATH, GETTING_STARTED_PATH, TESTING_DOC_PATH)
        )
        normalized = " ".join(combined.split())

        self.assertIn("headless validation", normalized)
        self.assertIn("headed Ubuntu Xvfb", normalized)
        self.assertIn("java.awt.headless=true", combined)
        self.assertIn("java.awt.headless=false", combined)
        self.assertIn("RABBITHOLE_LAUNCH_TIMEOUT_SECONDS", combined)
        self.assertIn("GUI/display-dependent behavior", normalized)
        self.assertNotIn("[PLANNED", combined)
        self.assertNotIn("Implementation Pending", combined)
        self.assertNotRegex(normalized, r"\bplanned headed Ubuntu Xvfb\b")


class GettingStartedValidationCiContract(unittest.TestCase):
    def test_alice_test_ci_uses_shared_xvfb_action_instead_of_inline_install(self) -> None:
        workflow = read_text(ALICE_TEST_WORKFLOW_PATH)

        self.assertIn("uses: ./.github/actions/setup-xvfb", workflow)
        self.assertIn("id: setup-xvfb", workflow)
        self.assertNotIn("sudo apt-get install -y --no-install-recommends xvfb", workflow)
        self.assertNotRegex(workflow, r"command\s+-v\s+xvfb-run")

    def test_headless_lane_is_preserved_as_cli_docs_safe_validation(self) -> None:
        workflow = read_text(ALICE_TEST_WORKFLOW_PATH)
        test_job = workflow_job_block(workflow, "test")

        self.assertIn("run: ./scripts/validate-getting-started.sh --headless", test_job)
        self.assertNotIn("steps.setup-xvfb.outputs.xvfb-run", test_job)
        self.assertNotIn("--auto-servernum", test_job)
        self.assertIn("-Djava.awt.headless=true", test_job)
        self.assertNotIn("./scripts/validate-getting-started.sh --gui", test_job)

    def test_headed_ubuntu_xvfb_job_is_distinct_from_matrix_headless_job(self) -> None:
        workflow = read_text(ALICE_TEST_WORKFLOW_PATH)
        headed_job = workflow_job_block(workflow, "headed-ubuntu-xvfb")

        self.assertIn("runs-on: ubuntu-latest", headed_job)
        self.assertNotIn("matrix.os", headed_job)
        self.assertIn("uses: actions/checkout@v4", headed_job)
        self.assertIn("git submodule update --init tweedle-lang", headed_job)
        self.assertIn("uses: actions/setup-java@v4", headed_job)
        self.assertIn("java-version: '21'", headed_job)
        self.assertIn("uses: ./.github/actions/setup-xvfb", headed_job)
        self.assertIn("id: setup-xvfb", headed_job)
        self.assertIn(SETUP_XVFB_ACTION_OUTPUT, headed_job)

    def test_headed_ubuntu_xvfb_job_runs_bounded_gui_getting_started_validation(self) -> None:
        workflow = read_text(ALICE_TEST_WORKFLOW_PATH)
        headed_job = workflow_job_block(workflow, "headed-ubuntu-xvfb")

        self.assertIn("scripts/validate-gui-with-xvfb.sh", headed_job)
        self.assertIn("--timeout-seconds", headed_job)
        self.assertIn("RABBITHOLE_LAUNCH_TIMEOUT_SECONDS", headed_job)
        self.assertIn("RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS", headed_job)
        self.assertIn("--expect success", headed_job)
        self.assertIn("--xvfb-run", headed_job)
        self.assertIn(SETUP_XVFB_ACTION_OUTPUT, headed_job)
        self.assertNotIn("-Djava.awt.headless=true", headed_job)
        self.assertNotIn("--headless", headed_job)
        self.assertIn("scripts/validate-getting-started.sh --gui", headed_job)
        self.assertNotIn("--auto-servernum", headed_job)

    def test_alice_test_ci_keeps_least_privilege_develop_pr_semantics(self) -> None:
        workflow = read_text(ALICE_TEST_WORKFLOW_PATH)

        self.assertRegex(workflow, r"(?ms)^permissions:\n  contents: read\n")
        self.assertIn("pull_request:", workflow)
        self.assertIn("branches: [develop]", workflow)
        self.assertNotIn("pull_request_target", workflow)
        self.assertNotRegex(workflow, r"(?m)^\s+contents: write$")
        self.assertNotRegex(workflow, r"(?m)^\s+pull-requests: write$")


class GettingStartedValidatorSafetyContract(unittest.TestCase):
    def test_validator_does_not_install_dependencies_or_clone_recursively(self) -> None:
        source = script_text()

        self.assertNotRegex(source, r"(?m)^\s*sudo\b")
        self.assertNotRegex(source, r"(?m)^\s*(apt|apt-get|brew|dnf|yum|pacman)\b")
        self.assertNotRegex(source, r"(?m)^\s*git\s+clone\b")

    def test_validator_does_not_dump_environment_or_credentials(self) -> None:
        source = script_text()

        self.assertNotRegex(source, r"(?m)^\s*(env|printenv|set)\s*$")
        self.assertNotIn("GITHUB_TOKEN", source)
        self.assertNotIn("MAVEN_OPTS", source)
        self.assertNotRegex(source, r"(?i)password|secret|credential")

    def test_validator_bounds_environment_timeout_override(self) -> None:
        source = script_text()

        self.assertIn("RABBITHOLE_LAUNCH_TIMEOUT_SECONDS", source)
        self.assertIn("MAX_LAUNCH_TIMEOUT_SECONDS", source)
        self.assertRegex(source, r"\^\[0-9\]\+\$")
        self.assertRegex(source, r"LAUNCH_TIMEOUT_SECONDS\s*<\s*1")
        self.assertRegex(source, r"LAUNCH_TIMEOUT_SECONDS\s*>\s*MAX_LAUNCH_TIMEOUT_SECONDS")


if __name__ == "__main__":
    unittest.main()
