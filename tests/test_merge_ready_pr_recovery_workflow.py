"""tests/test_merge_ready_pr_recovery_workflow.py

Failing integration and edge-case contract tests for merge-ready PR recovery.
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import sys
import tempfile
import unittest
from functools import lru_cache
from pathlib import Path
from types import SimpleNamespace
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "merge-ready-pr-recovery.py"
BRANCH = "feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow"
BASE = "develop"
HEAD_SHA = "f" * 40
MODEL_EXPORT_TEST = "core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java"


@lru_cache(maxsize=1)
def load_recovery_module():
    if not SCRIPT_PATH.exists():
        raise AssertionError(f"Expected merge-ready recovery implementation at {SCRIPT_PATH}")
    spec = importlib.util.spec_from_file_location("merge_ready_pr_recovery", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def as_mapping(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return value
    if hasattr(value, "__dict__"):
        return dict(value.__dict__)
    raise AssertionError(f"Expected mapping-like result, got {type(value)!r}: {value!r}")


class FakeRunner:
    def __init__(
        self,
        *,
        body: str,
        checks: list[dict[str, Any]],
        head_after: str = HEAD_SHA,
        metadata_overrides: dict[str, Any] | None = None,
        diff_output: str | None = None,
        find_stdout: str = "",
        find_returncode: int = 0,
        find_stderr: str = "",
        grep_stdout: str = "",
        grep_returncode: int = 0,
        grep_stderr: str = "",
    ) -> None:
        self.body = body
        self.checks = checks
        self.head_after = head_after
        self.metadata_overrides = metadata_overrides or {}
        self.diff_output = diff_output or f"M\t{MODEL_EXPORT_TEST}\n"
        self.find_stdout = find_stdout
        self.find_returncode = find_returncode
        self.find_stderr = find_stderr
        self.grep_stdout = grep_stdout
        self.grep_returncode = grep_returncode
        self.grep_stderr = grep_stderr
        self.commands: list[list[str]] = []

    def __call__(
        self,
        command: list[str],
        *,
        cwd: Path | None = None,
        env: dict[str, str] | None = None,
    ) -> SimpleNamespace:
        self.commands.append(command)
        rendered = " ".join(command).lower()
        if (command and command[0] in {"timeout", "gtimeout"}) or " timeout " in f" {rendered} ":
            raise AssertionError(f"Recovery commands must not use timeout wrappers: {command!r}")

        if command[:3] == ["git", "fetch", "origin"]:
            return SimpleNamespace(returncode=0, stdout="", stderr="")
        if command[:2] == ["git", "checkout"]:
            return SimpleNamespace(returncode=0, stdout="", stderr="")
        if command[:3] == ["git", "pull", "--ff-only"]:
            return SimpleNamespace(returncode=0, stdout="", stderr="")
        if command[:3] == ["git", "rev-parse", "HEAD"]:
            return SimpleNamespace(returncode=0, stdout=f"{HEAD_SHA}\n", stderr="")
        if command[:3] == ["git", "diff", "--name-status"]:
            return SimpleNamespace(returncode=0, stdout=self.diff_output, stderr="")
        if command[:4] == ["git", "submodule", "update", "--init"]:
            return SimpleNamespace(returncode=0, stdout="", stderr="")
        if command and command[0] == "mvn":
            self._assert_maven_contract(command, env)
            return SimpleNamespace(returncode=0, stdout="[INFO] BUILD SUCCESS\n", stderr="")
        if command[:3] == ["gh", "pr", "checks"]:
            return SimpleNamespace(returncode=0, stdout=json.dumps(self.checks), stderr="")
        if command[:3] == ["gh", "pr", "view"] and "--json" in command:
            json_fields = command[command.index("--json") + 1]
            if json_fields == "headRefOid":
                head = self.head_after if self._checks_already_queried() else HEAD_SHA
                return SimpleNamespace(returncode=0, stdout=json.dumps({"headRefOid": head}), stderr="")
            if json_fields == "body":
                return SimpleNamespace(returncode=0, stdout=json.dumps({"body": self.body}), stderr="")
            metadata = {
                "number": 425,
                "state": "OPEN",
                "isDraft": False,
                "reviewDecision": "APPROVED",
                "baseRefName": BASE,
                "headRefName": BRANCH,
                "headRefOid": HEAD_SHA,
                "title": "Characterize model export boundary lane follow",
                "url": "https://github.com/rysweet/RabbitHole/pull/425",
            }
            metadata.update(self.metadata_overrides)
            return SimpleNamespace(returncode=0, stdout=json.dumps(metadata), stderr="")
        if command[:3] == ["find", "qa/outside-in", "-maxdepth"]:
            return SimpleNamespace(returncode=self.find_returncode, stdout=self.find_stdout, stderr=self.find_stderr)
        if command and command[0] == "grep":
            return SimpleNamespace(returncode=self.grep_returncode, stdout=self.grep_stdout, stderr=self.grep_stderr)
        raise AssertionError(f"Unexpected command in recovery workflow: {command!r}")

    def _checks_already_queried(self) -> bool:
        return any(command[:3] == ["gh", "pr", "checks"] for command in self.commands)

    def _assert_maven_contract(self, command: list[str], env: dict[str, str] | None) -> None:
        rendered = " ".join(command)
        assert env is not None
        assert env.get("NODE_OPTIONS") == "--max-old-space-size=32768"
        assert "-pl core/model-loading" in rendered
        assert "-Dsurefire.failIfNoSpecifiedTests=false" in rendered
        assert "-Dtest=ModelExportTest" in rendered


def complete_pr_body() -> str:
    return "\n".join(
        [
            f"Head SHA: {HEAD_SHA}",
            "State: OPEN",
            "Draft: false",
            "Review state: APPROVED",
            f"Focused diff: {MODEL_EXPORT_TEST}",
            "CI/check status: green for exact SHA",
            "Local validation: NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/model-loading -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ModelExportTest test",
            "QA/scenario evidence: not-applicable; focused validation is the applicable evidence",
            "Docs impact: no documentation change required for test-only characterization coverage",
            "Quality audit cycles: cycle 1 diff scope clean; cycle 2 test adequacy clean; cycle 3 evidence completeness clean",
            "Non-claims: no full UI automation, visible rendering correctness, grading, creative assessment, full lesson completion, or full Tweedle/player decode claimed",
        ]
    )


class MergeReadyRecoveryWorkflowContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.recovery = load_recovery_module()

    def recovery_inputs(
        self,
        *,
        pr_number: int = 425,
        head_branch: str = BRANCH,
        base_branch: str = BASE,
        expected_diff_paths: set[str] | None = None,
    ) -> Any:
        return self.recovery.RecoveryInputs(
            pr_number=pr_number,
            head_branch=head_branch,
            base_branch=base_branch,
            expected_diff_paths=expected_diff_paths or {MODEL_EXPORT_TEST},
            design_scope="test-only",
        )

    def test_recover_pr_happy_path_returns_bounded_merge_ready_noop_report(self) -> None:
        runner = FakeRunner(
            body=complete_pr_body(),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
                {"name": "tests", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/tests"},
            ],
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        self.assertEqual("MERGE_READY", report["result"])
        self.assertEqual(425, report["pr"])
        self.assertEqual(HEAD_SHA, report["head"])
        self.assertEqual(BASE, report["base"])
        self.assertEqual(BRANCH, report["branch"])
        self.assertEqual("OPEN", report["state"])
        self.assertFalse(report["draft"])
        self.assertEqual("APPROVED", report["review_state"])
        self.assertEqual([], report["files_modified"])
        self.assertIn("No repository changes were required", report["no_op_justification"])
        self.assertEqual([], report["blockers"])
        self.assertEqual(3, len(report["quality_audit_cycles"]))
        self.assertIn("full UI automation", report["non_claims"])
        self.assertTrue(any(command[:3] == ["gh", "pr", "checks"] for command in runner.commands))
        head_oid_reads = [
            command
            for command in runner.commands
            if command[:3] == ["gh", "pr", "view"]
            and "--json" in command
            and command[command.index("--json") + 1] == "headRefOid"
        ]
        self.assertEqual(1, len(head_oid_reads))
        self.assertFalse(any(command[:3] == ["find", "qa/outside-in", "-maxdepth"] for command in runner.commands))
        self.assertFalse(any(command and command[0] == "grep" for command in runner.commands))
        self.assertFalse(any(command and command[0] in {"timeout", "gtimeout"} for command in runner.commands))

    def test_green_checks_are_not_sufficient_when_qa_or_pr_description_evidence_is_missing(self) -> None:
        runner = FakeRunner(
            body="Local validation passed.",
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
                {"name": "tests", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/tests"},
            ],
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        self.assertEqual("NOT_MERGE_READY", report["result"])
        blockers = "\n".join(report["blockers"])
        self.assertIn("NOT_MERGE_READY", blockers)
        self.assertIn("PR description", blockers)
        self.assertIn("QA/scenario", blockers)
        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertEqual("green for exact SHA", report["github_actions"])

    def test_requested_pr_head_and_base_mismatch_blocks_before_commands(self) -> None:
        cases = [
            self.recovery_inputs(pr_number=426),
            self.recovery_inputs(head_branch="feature/wrong"),
            self.recovery_inputs(base_branch="main"),
        ]
        for inputs in cases:
            with self.subTest(inputs=inputs):
                runner = FakeRunner(body=complete_pr_body(), checks=[])

                report = as_mapping(self.recovery.recover_pr(inputs, command_runner=runner, repo_root=REPO_ROOT))

                self.assertEqual("NOT_MERGE_READY", report["result"])
                blockers = "\n".join(report["blockers"])
                self.assertIn("NOT_MERGE_READY", blockers)
                self.assertIn("fixed", blockers)
                self.assertEqual([], runner.commands)

    def test_main_reports_fixed_target_mismatch_without_recovery_commands(self) -> None:
        stdout = io.StringIO()

        with contextlib.redirect_stdout(stdout):
            exit_code = self.recovery.main(
                [
                    "--pr-number",
                    "426",
                    "--head-branch",
                    BRANCH,
                    "--base-branch",
                    BASE,
                    "--json",
                ]
            )

        report = json.loads(stdout.getvalue())
        self.assertEqual(1, exit_code)
        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertIn("fixed to PR #425", "\n".join(report["blockers"]))

    def test_metadata_pr_head_and_base_mismatch_blocks_before_evidence_collection(self) -> None:
        cases = [
            {"number": 426},
            {"headRefName": "feature/wrong"},
            {"baseRefName": "main"},
        ]
        for metadata_overrides in cases:
            with self.subTest(metadata_overrides=metadata_overrides):
                runner = FakeRunner(
                    body=complete_pr_body(),
                    checks=[{"name": "build", "state": "SUCCESS", "bucket": "pass"}],
                    metadata_overrides=metadata_overrides,
                )

                report = as_mapping(
                    self.recovery.recover_pr(self.recovery_inputs(), command_runner=runner, repo_root=REPO_ROOT)
                )

                self.assertEqual("NOT_MERGE_READY", report["result"])
                self.assertIn("metadata", "\n".join(report["blockers"]))
                self.assertFalse(any(command[:3] == ["gh", "pr", "checks"] for command in runner.commands))
                self.assertFalse(any(command and command[0] == "mvn" for command in runner.commands))
                self.assertFalse(any(command[:3] == ["find", "qa/outside-in", "-maxdepth"] for command in runner.commands))

    def test_qa_surface_diff_runs_discovery_and_blocks_manual_only_evidence(self) -> None:
        qa_path = "qa/outside-in/alice-desktop/scenarios/export-model.yaml"
        runner = FakeRunner(
            body=complete_pr_body().replace(
                "QA/scenario evidence: not-applicable; focused validation is the applicable evidence",
                "QA/scenario evidence: documented-manual; applicable QA path was discovered but not run",
            ),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            ],
            diff_output=f"M\t{qa_path}\n",
            find_stdout="qa/outside-in/alice-desktop/tests/run-tests.sh\n",
            grep_stdout="docs/howto/run-merge-ready-pr-recovery.md:Search model export QA\n",
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(expected_diff_paths={qa_path}),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertIn("documented-manual", report["qa_scenario_evidence"])
        self.assertIn("QA/scenario evidence is documented or manual only", "\n".join(report["blockers"]))
        self.assertTrue(any(command[:3] == ["find", "qa/outside-in", "-maxdepth"] for command in runner.commands))
        self.assertTrue(any(command and command[0] == "grep" for command in runner.commands))

    def test_qa_discovery_failure_surfaces_sanitized_blocker(self) -> None:
        qa_path = "qa/outside-in/alice-desktop/scenarios/export-model.yaml"
        runner = FakeRunner(
            body=complete_pr_body(),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            ],
            diff_output=f"M\t{qa_path}\n",
            find_returncode=2,
            find_stderr="/home/azureuser/src/private/qa.log password: hunter2",
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(expected_diff_paths={qa_path}),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        blockers = "\n".join(report["blockers"])
        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertIn("QA/scenario find discovery failed", blockers)
        self.assertIn("<path>", blockers)
        self.assertIn("password: <redacted>", blockers)
        self.assertNotIn("/home/azureuser/src/private", blockers)
        self.assertNotIn("hunter2", blockers)
        self.assertFalse(any(command and command[0] == "grep" for command in runner.commands))

    def test_missing_qa_discovery_search_path_blocks_before_shell_discovery(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            runner = FakeRunner(body=complete_pr_body(), checks=[])

            with self.assertRaisesRegex(self.recovery.RecoveryEvidenceError, "missing or inaccessible"):
                self.recovery.discover_qa_paths(runner, root)

            self.assertEqual([], runner.commands)

    def test_grep_discovery_failure_surfaces_sanitized_blocker(self) -> None:
        qa_path = "qa/outside-in/alice-desktop/scenarios/export-model.yaml"
        runner = FakeRunner(
            body=complete_pr_body(),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            ],
            diff_output=f"M\t{qa_path}\n",
            grep_returncode=2,
            grep_stderr="/home/azureuser/src/private/qa.log Authorization: Bearer grep-secret",
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(expected_diff_paths={qa_path}),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        blockers = "\n".join(report["blockers"])
        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertIn("QA/scenario grep discovery failed", blockers)
        self.assertIn("Authorization: Bearer <redacted>", blockers)
        self.assertIn("<path>", blockers)
        self.assertNotIn("/home/azureuser/src/private", blockers)
        self.assertNotIn("grep-secret", blockers)

    def test_grep_no_matches_is_not_treated_as_discovery_failure(self) -> None:
        qa_path = "qa/outside-in/alice-desktop/scenarios/export-model.yaml"
        runner = FakeRunner(
            body=complete_pr_body(),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            ],
            diff_output=f"M\t{qa_path}\n",
            grep_returncode=1,
            grep_stderr="",
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(expected_diff_paths={qa_path}),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        blockers = "\n".join(report["blockers"])
        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertIn("QA/scenario evidence is missing", blockers)
        self.assertNotIn("QA/scenario grep discovery failed", blockers)
        self.assertTrue(any(command and command[0] == "grep" for command in runner.commands))

    def test_head_movement_during_check_collection_blocks_readiness(self) -> None:
        runner = FakeRunner(
            body=complete_pr_body(),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            ],
            head_after="e" * 40,
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        self.assertEqual("NOT_MERGE_READY", report["result"])
        self.assertIn("head", "\n".join(report["blockers"]).lower())
        self.assertIn("SHA", "\n".join(report["blockers"]))

    def test_report_output_contract_contains_all_reviewable_fields(self) -> None:
        runner = FakeRunner(
            body=complete_pr_body(),
            checks=[
                {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            ],
        )

        report = as_mapping(
            self.recovery.recover_pr(
                self.recovery_inputs(),
                command_runner=runner,
                repo_root=REPO_ROOT,
            )
        )

        required_fields = {
            "result",
            "pr",
            "head",
            "base",
            "branch",
            "state",
            "draft",
            "review_state",
            "files_modified",
            "diff_scope",
            "local_validation",
            "github_actions",
            "qa_scenario_evidence",
            "docs_impact",
            "quality_audit_cycles",
            "pr_description_evidence",
            "non_claims",
            "blockers",
        }
        self.assertLessEqual(required_fields, set(report))


if __name__ == "__main__":
    unittest.main()
