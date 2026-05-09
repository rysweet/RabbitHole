"""Workflow tests for scripts/merge-ready-pr-recovery.py (TDD — tests first).

These tests exercise the full recovery pipeline using subprocess calls with
mocked external commands (gh, mvn, validate-scenarios.sh, etc.).
They will FAIL until the implementation is created at:
    scripts/merge-ready-pr-recovery.py

Reference specification:
    docs/reference/merge-ready-pr-recovery.md
    docs/howto/run-merge-ready-pr-recovery.md
"""

import json
import os
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "merge-ready-pr-recovery.py"
SCENARIO_DIR = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios"

EVIDENCE_HEADING = "## Merge-Ready Evidence"


def _write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def _write_executable(path: Path, content: str) -> None:
    _write_file(path, content)
    path.chmod(0o755)


def _create_mock_env(root: Path, scenario: str = "model-export-boundary-smoke") -> dict:
    """Create a mock repository layout with fake QA scripts and a scenario YAML."""
    scenario_dir = root / "qa" / "outside-in" / "alice-desktop" / "scenarios"
    runners_dir = root / "qa" / "outside-in" / "alice-desktop" / "runners"
    tests_dir = root / "qa" / "outside-in" / "alice-desktop" / "tests"
    bin_dir = root / "bin"

    _write_file(
        scenario_dir / f"{scenario}.yaml",
        textwrap.dedent(f"""\
            id: alice-desktop-{scenario}
            workflow: {scenario}
            summary: Mock smoke scenario
            automation:
              cwd: "."
              argv:
                - mvn
                - -DincludeSims=false
                - -Dinstall4j.skip
                - -pl
                - core/model-loading
                - -am
                - -DfailIfNoTests=false
                - -Dsurefire.failIfNoSpecifiedTests=false
                - -Dtest=ModelExportTest
                - test
              timeoutSeconds: 600
              readyWaitSeconds: 0
        """),
    )

    _write_executable(
        runners_dir / "validate-scenarios.sh",
        textwrap.dedent("""\
            #!/usr/bin/env bash
            echo "All scenarios valid"
            exit 0
        """),
    )

    _write_executable(
        tests_dir / "test-schema-contract.sh",
        textwrap.dedent("""\
            #!/usr/bin/env bash
            echo "Schema contract OK"
            exit 0
        """),
    )

    _write_executable(
        runners_dir / "run-scenario.sh",
        textwrap.dedent("""\
            #!/usr/bin/env bash
            shift  # consume 'run'
            scenario_id="$1"
            shift
            evidence_dir=""
            while [ $# -gt 0 ]; do
                case "$1" in
                    --evidence-dir) evidence_dir="$2"; shift 2 ;;
                    *) shift ;;
                esac
            done
            ts=$(date -u +%Y%m%dT%H%M%SZ)
            out_dir="${evidence_dir}/${scenario_id}/${ts}"
            mkdir -p "$out_dir"
            cat > "${out_dir}/status.txt" <<EOF
            outcome=passed
            test_count=23
            scenario=${scenario_id}
            EOF
            echo "Scenario ${scenario_id} passed"
            exit 0
        """),
    )

    _write_executable(
        bin_dir / "gh",
        textwrap.dedent("""\
            #!/usr/bin/env bash
            if [[ "$*" == *"pr view"*"--json body"* ]]; then
                echo '{"body":"## Summary\\n\\nTest PR body."}'
            elif [[ "$*" == *"pr view"*"--json state"* ]]; then
                echo '{"state":"OPEN"}'
            elif [[ "$*" == *"pr edit"* ]]; then
                echo "PR updated"
            fi
            exit 0
        """),
    )

    _write_executable(
        bin_dir / "git",
        textwrap.dedent("""\
            #!/usr/bin/env bash
            if [[ "$*" == *"diff --name-only"* ]]; then
                echo "core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceExporter.java"
            elif [[ "$*" == *"push"* ]]; then
                echo "Pushed"
            fi
            exit 0
        """),
    )

    return {
        **os.environ,
        "PATH": f"{bin_dir}:{os.environ.get('PATH', '')}",
        "ALICE_QA_SCENARIO_DIR": str(scenario_dir),
        "ALICE_QA_RUN_GATED_SMOKES": "1",
    }


class FullRecoveryFlowTest(unittest.TestCase):
    """End-to-end recovery flow with mocked commands."""

    def test_full_recovery_exits_zero_on_success(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}")

    def test_full_recovery_prints_qa_passed_summary(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertIn("passed", result.stdout.lower())
        self.assertIn("model-export-boundary-smoke", result.stdout)

    def test_full_recovery_reports_audit_cycle_count(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertIn("3 cycles", result.stdout)
        self.assertIn("clean", result.stdout.lower())


class MissingScenarioTest(unittest.TestCase):
    """Recovery must fail when the scenario YAML is missing."""

    def test_missing_scenario_yaml_exits_nonzero(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root, scenario="existing-scenario")
            env["ALICE_QA_SCENARIO_DIR"] = str(
                root / "qa" / "outside-in" / "alice-desktop" / "scenarios"
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "nonexistent-scenario",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("nonexistent-scenario", result.stderr)

    def test_missing_scenario_reports_expected_path(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "does-not-exist",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertIn("does-not-exist.yaml", result.stderr)


class FailedGatedSmokeTest(unittest.TestCase):
    """Recovery must fail when the gated smoke reports outcome!=passed."""

    def test_failed_smoke_exits_nonzero(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            runners_dir = root / "qa" / "outside-in" / "alice-desktop" / "runners"
            _write_executable(
                runners_dir / "run-scenario.sh",
                textwrap.dedent("""\
                    #!/usr/bin/env bash
                    shift
                    scenario_id="$1"
                    shift
                    evidence_dir=""
                    while [ $# -gt 0 ]; do
                        case "$1" in
                            --evidence-dir) evidence_dir="$2"; shift 2 ;;
                            *) shift ;;
                        esac
                    done
                    ts=$(date -u +%Y%m%dT%H%M%SZ)
                    out_dir="${evidence_dir}/${scenario_id}/${ts}"
                    mkdir -p "$out_dir"
                    cat > "${out_dir}/status.txt" <<EOF
                    outcome=failed
                    test_count=0
                    scenario=${scenario_id}
                    EOF
                    echo "Scenario ${scenario_id} FAILED"
                    exit 1
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("failed", result.stderr.lower())


class ValidatorFailureTest(unittest.TestCase):
    """Recovery must fail when validate-scenarios.sh or test-schema-contract.sh fails."""

    def test_validator_failure_exits_nonzero(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            runners_dir = root / "qa" / "outside-in" / "alice-desktop" / "runners"
            _write_executable(
                runners_dir / "validate-scenarios.sh",
                textwrap.dedent("""\
                    #!/usr/bin/env bash
                    echo "Validation FAILED: unknown workflow"
                    exit 1
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertNotEqual(0, result.returncode)

    def test_schema_contract_failure_exits_nonzero(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            tests_dir = root / "qa" / "outside-in" / "alice-desktop" / "tests"
            _write_executable(
                tests_dir / "test-schema-contract.sh",
                textwrap.dedent("""\
                    #!/usr/bin/env bash
                    echo "Schema contract FAILED"
                    exit 1
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertNotEqual(0, result.returncode)


class DryRunWorkflowTest(unittest.TestCase):
    """Dry-run mode prints the plan without executing commands."""

    def test_dry_run_does_not_call_run_scenario(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            marker = root / "run-scenario-called.marker"
            runners_dir = root / "qa" / "outside-in" / "alice-desktop" / "runners"
            _write_executable(
                runners_dir / "run-scenario.sh",
                textwrap.dedent(f"""\
                    #!/usr/bin/env bash
                    touch {marker}
                    exit 0
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--dry-run",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse(marker.exists(), "Dry-run must not execute run-scenario.sh")

    def test_dry_run_does_not_call_gh_pr_edit(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            marker = root / "gh-edit-called.marker"
            bin_dir = root / "bin"
            _write_executable(
                bin_dir / "gh",
                textwrap.dedent(f"""\
                    #!/usr/bin/env bash
                    if [[ "$*" == *"pr edit"* ]]; then
                        touch {marker}
                    fi
                    exit 0
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--dry-run",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse(marker.exists(), "Dry-run must not call gh pr edit")

    def test_dry_run_prints_scenario_path(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--dry-run",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("model-export-boundary-smoke.yaml", result.stdout)


class SkipPushWorkflowTest(unittest.TestCase):
    """--skip-push mode commits locally but does not push."""

    def test_skip_push_does_not_call_git_push(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            marker = root / "git-push-called.marker"
            bin_dir = root / "bin"
            _write_executable(
                bin_dir / "git",
                textwrap.dedent(f"""\
                    #!/usr/bin/env bash
                    if [[ "$*" == *"push"* ]]; then
                        touch {marker}
                    fi
                    if [[ "$*" == *"diff --name-only"* ]]; then
                        echo "some/file.java"
                    fi
                    exit 0
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}")
        self.assertFalse(marker.exists(), "--skip-push must not call git push")


class PrDescriptionWorkflowTest(unittest.TestCase):
    """PR description update is additive (compatibility rule 4)."""

    def test_pr_body_updated_with_evidence_section(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)
            body_file = root / "pr-body-captured.txt"
            bin_dir = root / "bin"
            _write_executable(
                bin_dir / "gh",
                textwrap.dedent(f"""\
                    #!/usr/bin/env bash
                    if [[ "$*" == *"pr view"*"--json body"* ]]; then
                        echo '{{"body":"## Summary\\n\\nOriginal body."}}'
                    elif [[ "$*" == *"pr edit"* ]]; then
                        for arg in "$@"; do
                            if [[ "$prev" == "--body" ]]; then
                                echo "$arg" > {body_file}
                                break
                            fi
                            prev="$arg"
                        done
                    fi
                    exit 0
                """),
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}")
        if body_file.exists():
            captured = body_file.read_text(encoding="utf-8")
            self.assertIn("Original body.", captured)
            self.assertIn(EVIDENCE_HEADING, captured)


class NoMergeGuardTest(unittest.TestCase):
    """Compatibility rule 5: script does not merge, force-push, rebase, or modify history."""

    def test_script_source_does_not_contain_merge_commands(self) -> None:
        source = SCRIPT_PATH.read_text(encoding="utf-8")
        forbidden = [
            "pr merge",
            "git merge",
            "git rebase",
            "force-push",
            "--force",
            "git push --force",
            "git push -f",
        ]
        for term in forbidden:
            with self.subTest(term=term):
                self.assertNotIn(term, source)

    def test_script_source_does_not_create_ci_workflows(self) -> None:
        source = SCRIPT_PATH.read_text(encoding="utf-8")
        self.assertNotIn(".github/workflows", source)


class ExtraCyclesTest(unittest.TestCase):
    """Custom --audit-cycles count is respected."""

    def test_five_audit_cycles_reported(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            env = _create_mock_env(root)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--pr", "425",
                    "--scenario", "model-export-boundary-smoke",
                    "--evidence-dir", str(root / "evidence"),
                    "--audit-cycles", "5",
                    "--skip-push",
                ],
                cwd=root,
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

        self.assertEqual(0, result.returncode, f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}")
        self.assertIn("5 cycles", result.stdout)


if __name__ == "__main__":
    unittest.main()
