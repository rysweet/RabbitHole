"""TDD contract for the neutral Alice QA command wrapper.

The old branch-installable assistant wrapper is intentionally not imported here.
These tests fail until the wrapper is renamed to ``alice_qa.py`` and exposes a
neutral RabbitHole/Alice command surface.
"""

from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path
from types import ModuleType
from unittest import mock


REPO_ROOT = Path(__file__).resolve().parents[1]
WRAPPER_PATH = REPO_ROOT / "alice_qa.py"
PYPROJECT_PATH = REPO_ROOT / "pyproject.toml"
OLD_WRAPPER_PATH = REPO_ROOT / ("alice_qa_ampli" + "hack.py")
ARCHIVE_FIXTURE_SCENARIO = (
    REPO_ROOT
    / "qa"
    / "outside-in"
    / "alice-desktop"
    / "scenarios"
    / "archive-fixture-smoke.yaml"
)


def load_alice_qa() -> ModuleType:
    if not WRAPPER_PATH.is_file():
        raise AssertionError("Expected neutral wrapper at alice_qa.py")
    spec = importlib.util.spec_from_file_location("alice_qa", WRAPPER_PATH)
    if spec is None or spec.loader is None:
        raise AssertionError("Unable to import alice_qa.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def write_executable(path: Path, content: str) -> None:
    write_file(path, content)
    path.chmod(0o755)


def write_wrapper_repo(root: Path) -> None:
    write_executable(
        root / "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
        "#!/usr/bin/env bash\n",
    )
    write_executable(
        root / "qa/outside-in/alice-desktop/runners/run-scenario.sh",
        "#!/usr/bin/env bash\n",
    )
    write_executable(
        root / "scripts/validate-getting-started.sh",
        textwrap.dedent(
            """\
            #!/usr/bin/env bash
            printf 'getting-started cwd=%s args=%s\\n' "$PWD" "$*"
            """
        ),
    )
    write_file(
        root / "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
        "#!/usr/bin/env bash\n",
    )
    write_file(
        root / "scripts/generate-modernization-scorecard.py",
        textwrap.dedent(
            """\
            import json
            import os
            import sys

            print(json.dumps({"cwd": os.getcwd(), "argv": sys.argv[1:]}))
            """
        ),
    )


def write_logged_command(path: Path, log_path: Path, command_name: str) -> None:
    write_executable(
        path,
        textwrap.dedent(
            f"""\
            #!/usr/bin/env bash
            echo {command_name} "$@" >> {log_path}
            echo {command_name}-node-options "${{NODE_OPTIONS:-}}" >> {log_path}
            """
        ),
    )


def write_validate_scenarios_logger(path: Path, log_path: Path) -> None:
    write_logged_command(path, log_path, "validate-scenarios")


class AliceQaPackagingContract(unittest.TestCase):
    def test_neutral_wrapper_file_replaces_old_branded_wrapper(self) -> None:
        self.assertTrue(WRAPPER_PATH.is_file(), "Expected alice_qa.py to exist.")
        self.assertFalse(
            OLD_WRAPPER_PATH.exists(),
            "The branded Alice QA wrapper should be removed.",
        )

    def test_pyproject_exposes_neutral_console_script(self) -> None:
        text = PYPROJECT_PATH.read_text(encoding="utf-8")

        self.assertIn('alice-qa = "alice_qa:main"', text)
        self.assertIn('py-modules = ["alice_qa"]', text)
        self.assertNotIn("ampli" + "hack", text.lower())
        self.assertNotIn("alice_qa_ampli" + "hack", text)


class AliceQaWrapperContract(unittest.TestCase):
    def test_node_options_env_inherits_environment_when_memory_flag_is_already_set(self) -> None:
        alice_qa = load_alice_qa()

        with mock.patch.dict(
            os.environ,
            {"NODE_OPTIONS": "--trace-warnings --max-old-space-size=32768"},
        ):
            self.assertIsNone(alice_qa.node_options_env())

    def test_node_options_env_appends_memory_flag_when_missing(self) -> None:
        alice_qa = load_alice_qa()

        with mock.patch.dict(os.environ, {"NODE_OPTIONS": "--trace-warnings"}):
            env = alice_qa.node_options_env()

        self.assertIsNotNone(env)
        assert env is not None
        self.assertEqual("--trace-warnings --max-old-space-size=32768", env["NODE_OPTIONS"])

    def test_help_lists_neutral_commands_without_requiring_checkout(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            result = subprocess.run(
                [sys.executable, str(WRAPPER_PATH), "--help"],
                cwd=temp_dir,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("python3 alice_qa.py alice-scorecard [--root <dir>] [--output <path>]", result.stdout)
        self.assertIn("python3 alice_qa.py alice-qa save-negative-contract", result.stdout)
        self.assertIn("python3 alice_qa.py getting-started validate", result.stdout)
        self.assertIn("python3 alice_qa.py archive-player-boundary verify", result.stdout)
        self.assertIn("python3 alice_qa.py tweedle-decode verify", result.stdout)
        self.assertNotIn("ampli" + "hack", result.stdout.lower())
        self.assertNotIn("gadu" + "gi", result.stdout.lower())
        self.assertNotIn("full UI automation", result.stdout)
        self.assertNotIn("rendering correctness", result.stdout)
        self.assertNotIn("grading", result.stdout)

    def test_scorecard_command_delegates_to_generator_from_repo_root(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)
            child = root / "docs" / "reference"
            child.mkdir(parents=True)

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "alice-scorecard",
                    "--output",
                    "scorecard.md",
                ],
                cwd=child,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        payload = json.loads(result.stdout)
        self.assertEqual(str(root), payload["cwd"])
        self.assertEqual(["--output", "scorecard.md"], payload["argv"])

    def test_save_negative_contract_delegates_to_repo_shell_contract(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)
            contract = root / "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh"
            contract.write_text(
                textwrap.dedent(
                    """\
                    #!/usr/bin/env bash
                    printf 'save-negative-contract cwd=%s\\n' "$PWD"
                    """
                ),
                encoding="utf-8",
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "alice-qa",
                    "save-negative-contract",
                ],
                cwd=root / "qa" / "outside-in",
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn(f"save-negative-contract cwd={root}", result.stdout)

    def test_getting_started_validate_delegates_to_repo_validator_from_repo_root(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)
            child = root / "docs"
            child.mkdir()

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "getting-started",
                    "validate",
                    "--all",
                ],
                cwd=child,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn(f"getting-started cwd={root} args=--all", result.stdout)

    def test_getting_started_rejects_unknown_subcommand(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "getting-started",
                    "probe",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(2, result.returncode)
        self.assertIn("getting-started usage", result.stderr)
        self.assertNotIn("ampli" + "hack", result.stderr.lower())

    def test_tweedle_decode_verify_delegates_to_focused_maven_test(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)
            bin_dir = root / "bin"
            log_path = root / "commands.log"
            write_logged_command(bin_dir / "git", log_path, "git")
            write_logged_command(bin_dir / "mvn", log_path, "mvn")

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "tweedle-decode",
                    "verify",
                    "simple-if-method-call",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env={
                    **os.environ,
                    "NODE_OPTIONS": "--trace-warnings",
                    "PATH": f"{bin_dir}:{os.environ.get('PATH', '')}",
                },
            )

            log = log_path.read_text(encoding="utf-8") if log_path.exists() else ""

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("PASS: simple-if-method-call", result.stdout)
        self.assertIn("git submodule update --init tweedle-lang", log)
        self.assertIn("git-node-options --trace-warnings --max-old-space-size=32768", log)
        self.assertIn("-pl core/ast", log)
        self.assertIn("mvn-node-options --trace-warnings --max-old-space-size=32768", log)
        self.assertIn(
            "-Dtest=TweedleEncoderDecoderTest#decodeClassWithSimpleIfMethodCallBodyCreatesConditionalMethodInvocation",
            log,
        )

    def test_archive_player_boundary_verify_runs_bounded_readiness_commands(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)
            bin_dir = root / "bin"
            log_path = root / "commands.log"
            write_validate_scenarios_logger(
                root / "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
                log_path,
            )
            write_logged_command(bin_dir / "git", log_path, "git")
            write_logged_command(bin_dir / "mvn", log_path, "mvn")

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "archive-player-boundary",
                    "verify",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env={
                    **os.environ,
                    "NODE_OPTIONS": "--trace-warnings",
                    "PATH": f"{bin_dir}:{os.environ.get('PATH', '')}",
                },
            )

            log = log_path.read_text(encoding="utf-8") if log_path.exists() else ""

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("PASS: archive-player-boundary", result.stdout)
        self.assertIn("validate-scenarios", log)
        self.assertIn("git submodule update --init tweedle-lang", log)
        self.assertIn("mvn -Dinstall4j.skip", log)
        self.assertIn("-DfailIfNoTests=false", log)
        self.assertIn("-Dsurefire.failIfNoSpecifiedTests=false", log)
        self.assertIn("-pl core/story-api-migration", log)
        self.assertIn("-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest", log)
        self.assertIn("validate-scenarios-node-options --trace-warnings --max-old-space-size=32768", log)
        self.assertIn("git-node-options --trace-warnings --max-old-space-size=32768", log)
        self.assertIn("mvn-node-options --trace-warnings --max-old-space-size=32768", log)
        self.assertNotIn("timeout ", log)
        self.assertNotIn("gh pr merge", log)

    def test_archive_player_boundary_verify_rejects_extra_arguments(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_wrapper_repo(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(WRAPPER_PATH),
                    "archive-player-boundary",
                    "verify",
                    "--full-ui",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(2, result.returncode)
        self.assertIn("archive-player-boundary usage", result.stderr)
        self.assertNotIn("full UI", result.stdout + result.stderr)

    def test_archive_fixture_smoke_scenario_stays_on_resource_recovery_boundary(self) -> None:
        scenario = ARCHIVE_FIXTURE_SCENARIO.read_text(encoding="utf-8")

        self.assertIn("legacy player resource-recovery boundary", scenario)
        self.assertIn("manifest-declared image resources", scenario)
        self.assertIn("unsupported manifest-declared program", scenario)
        self.assertIn("XML fallback", scenario)
        self.assertNotIn("full historical archive", scenario)
        self.assertNotIn("full player", scenario)
        self.assertNotIn("proves rendering", scenario.lower())
        self.assertNotIn("proves grading", scenario.lower())


if __name__ == "__main__":
    unittest.main()
