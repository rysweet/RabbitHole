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

HEADLESS_MAVEN_FLAGS = (
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-Dcheckstyle.skip",
    "-Djava.awt.headless=true",
    "clean",
    "test",
)
LAUNCH_MAVEN_FLAGS = (
    "-DincludeSims=false",
    "exec:java",
    "-Dalice-ide",
)
EXPECTED_HEADLESS_GUI_MESSAGE = (
    "Alice desktop launch requires a graphical environment."
)
SUBMODULE_FIX_COMMAND = "git submodule update --init tweedle-lang"


@lru_cache(maxsize=None)
def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def script_text() -> str:
    if not VALIDATOR_PATH.exists():
        raise AssertionError(
            "Expected executable validator at scripts/validate-getting-started.sh"
        )
    return read_text(VALIDATOR_PATH)


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

        self.assertIn("#848", source)
        self.assertRegex(source, r"Darwin|macOS")
        self.assertRegex(source, r"arm64|aarch64|Apple Silicon")
        self.assertRegex(source, r"(?i)blocked")


class GettingStartedValidationDocsContract(unittest.TestCase):
    def test_readme_exposes_validator_and_lanes(self) -> None:
        text = read_text(README_PATH)

        self.assertIn("./scripts/validate-getting-started.sh", text)
        self.assertIn("./scripts/validate-getting-started.sh --gui", text)
        self.assertIn("--all", text)
        self.assertIn("#848", text)

    def test_getting_started_docs_name_commands_and_failure_guidance(self) -> None:
        text = read_text(GETTING_STARTED_PATH)

        for command in (
            "./scripts/validate-getting-started.sh",
            "./scripts/validate-getting-started.sh --headless",
            "./scripts/validate-getting-started.sh --gui",
            "./scripts/validate-getting-started.sh --all",
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
        self.assertIn("Missing `tweedle-lang` or `tweedle-lang/Grammar`", text)
        self.assertIn("No desktop display", text)
        self.assertIn("Unknown validator flag", text)
        self.assertIn("#848", text)
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


class GettingStartedValidationCiContract(unittest.TestCase):
    def test_alice_test_ci_uses_headless_validator_only_when_wired(self) -> None:
        workflow = read_text(ALICE_TEST_WORKFLOW_PATH)

        self.assertNotIn("./scripts/validate-getting-started.sh --gui", workflow)
        self.assertNotIn("./scripts/validate-getting-started.sh --all", workflow)
        if "./scripts/validate-getting-started.sh" in workflow:
            self.assertRegex(
                workflow,
                r"(?ms)- name: .*Getting Started.*\n.*run: ./scripts/validate-getting-started.sh(?: --headless)?",
            )
            self.assertIn(
                "if: github.event_name != 'pull_request' || "
                "steps.change-scope.outputs.maven-required == 'true'",
                workflow,
            )


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


if __name__ == "__main__":
    unittest.main()
