"""Contract tests for the reusable Xvfb GUI validation harness."""
import os
import stat
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
HARNESS_PATH = REPO_ROOT / "scripts" / "validate-gui-with-xvfb.sh"
ALICE_TEST_WORKFLOW_PATH = REPO_ROOT / ".github" / "workflows" / "alice-test-ci.yml"


def write_executable(path: Path, source: str) -> Path:
    path.write_text(textwrap.dedent(source).lstrip(), encoding="utf-8")
    mode = path.stat().st_mode
    path.chmod(mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    return path


def workflow_job_block(workflow: str, job_name: str) -> str:
    import re

    match = re.search(
        rf"(?ms)^  {re.escape(job_name)}:\n(?P<body>.*?)(?=^  [A-Za-z0-9_-]+:\n|\Z)",
        workflow,
    )
    if match is None:
        raise AssertionError(f"Missing workflow job: {job_name}")
    return match.group("body")


class XvfbGuiHarnessContract(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.work_dir = Path(self.temp_dir.name)
        self.args_file = self.work_dir / "xvfb-args.txt"
        self.xvfb_run = write_executable(
            self.work_dir / "xvfb-run",
            f"""\
            #!/usr/bin/env bash
            set -euo pipefail
            printf '%s\n' "$@" > {self.args_file}
            while (($# > 0)); do
              case "$1" in
                --auto-servernum)
                  shift
                  ;;
                -s)
                  shift 2
                  ;;
                *)
                  break
                  ;;
              esac
            done
            exec "$@"
            """,
        )
        self.success_command = write_executable(
            self.work_dir / "success-command",
            """\
            #!/usr/bin/env bash
            set -euo pipefail
            printf 'wrapped stdout\n'
            printf 'wrapped stderr\n' >&2
            """,
        )
        self.failure_command = write_executable(
            self.work_dir / "failure-command",
            """\
            #!/usr/bin/env bash
            set -euo pipefail
            printf 'failing stdout\n'
            printf 'failing stderr\n' >&2
            exit 7
            """,
        )
        self.slow_command = write_executable(
            self.work_dir / "slow-command",
            """\
            #!/usr/bin/env bash
            set -euo pipefail
            sleep 5
            """,
        )

    def run_harness(self, *args: str) -> subprocess.CompletedProcess[str]:
        env = os.environ.copy()
        env["PATH"] = f"{self.work_dir}{os.pathsep}{env.get('PATH', '')}"
        return subprocess.run(
            [str(HARNESS_PATH), *args],
            cwd=REPO_ROOT,
            env=env,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
            check=False,
        )

    def test_successful_command_returns_zero(self) -> None:
        result = self.run_harness(
            "--xvfb-run", str(self.xvfb_run), "--", str(self.success_command)
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout, "wrapped stdout\n")
        self.assertIn("wrapped stderr\n", result.stderr)

    def test_failing_command_returns_nonzero_by_default(self) -> None:
        result = self.run_harness(
            "--xvfb-run", str(self.xvfb_run), "--", str(self.failure_command)
        )

        self.assertEqual(result.returncode, 7)
        self.assertIn("failing stdout\n", result.stdout)
        self.assertIn("failing stderr\n", result.stderr)
        self.assertIn("expected success", result.stderr)

    def test_expect_failure_accepts_nonzero_command(self) -> None:
        result = self.run_harness(
            "--expect",
            "failure",
            "--xvfb-run",
            str(self.xvfb_run),
            "--",
            str(self.failure_command),
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("failing stdout\n", result.stdout)
        self.assertIn("failing stderr\n", result.stderr)

    def test_expect_failure_rejects_successful_command(self) -> None:
        result = self.run_harness(
            "--expect",
            "failure",
            "--xvfb-run",
            str(self.xvfb_run),
            "--",
            str(self.success_command),
        )

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("expected failure", result.stderr)

    def test_timeout_returns_failure(self) -> None:
        result = self.run_harness(
            "--timeout-seconds",
            "1",
            "--expect",
            "failure",
            "--xvfb-run",
            str(self.xvfb_run),
            "--",
            str(self.slow_command),
        )

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("timed out", result.stderr)

    def test_passes_standard_xvfb_display_arguments(self) -> None:
        result = self.run_harness(
            "--xvfb-run", str(self.xvfb_run), "--", str(self.success_command), "arg-one"
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(
            self.args_file.read_text(encoding="utf-8").splitlines(),
            [
                "--auto-servernum",
                "-s",
                "-screen 0 1024x768x24 -ac",
                str(self.success_command),
                "arg-one",
            ],
        )

    def test_requires_command_after_separator(self) -> None:
        result = self.run_harness("--xvfb-run", str(self.xvfb_run), str(self.success_command))

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("separated", result.stderr)


class XvfbGuiHarnessWorkflowContract(unittest.TestCase):
    def test_workflow_delegates_gui_validation_to_harness(self) -> None:
        workflow = ALICE_TEST_WORKFLOW_PATH.read_text(encoding="utf-8")
        headed_job = workflow_job_block(workflow, "headed-ubuntu-xvfb")
        test_job = workflow_job_block(workflow, "test")

        self.assertIn("scripts/validate-gui-with-xvfb.sh", headed_job)
        self.assertIn("--timeout-seconds", headed_job)
        self.assertIn("RABBITHOLE_LAUNCH_TIMEOUT_SECONDS: '60'", headed_job)
        self.assertIn("RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS", headed_job)
        self.assertIn(":-1800", headed_job)
        self.assertIn("--expect success", headed_job)
        self.assertIn("--xvfb-run", headed_job)
        self.assertIn("steps.setup-xvfb.outputs.xvfb-run", headed_job)
        self.assertIn("scripts/validate-getting-started.sh --gui", headed_job)
        self.assertNotRegex(headed_job, r"xvfb-run[^\n]*--auto-servernum")
        self.assertIn("./scripts/validate-getting-started.sh --headless", test_job)
        self.assertNotIn("validate-gui-with-xvfb.sh", test_job)
