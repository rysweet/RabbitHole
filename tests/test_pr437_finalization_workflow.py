from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import unittest
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
FINALIZER_PATH = REPO_ROOT / "scripts" / "pr437-finalization.py"
EXPECTED_BRANCH = "feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo"
EXPECTED_REPO = "rysweet/RabbitHole"


def current_git_head() -> str:
    return subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    ).stdout.strip()


EXPECTED_PR_HEAD = current_git_head()


def load_finalizer() -> Any:
    if not FINALIZER_PATH.exists():
        raise AssertionError(f"Expected PR #437 finalization workflow at {FINALIZER_PATH}")
    spec = importlib.util.spec_from_file_location("pr437_finalization", FINALIZER_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def clean_green_pr_json(
    *,
    head: str = EXPECTED_PR_HEAD,
    review_decision: str | None = None,
    conclusion: str = "SUCCESS",
    status: str = "COMPLETED",
    merge_state: str = "CLEAN",
) -> str:
    return json.dumps(
        {
            "number": 437,
            "title": "Select Project starter recovery",
            "state": "OPEN",
            "headRefName": EXPECTED_BRANCH,
            "headRefOid": head,
            "baseRefName": "develop",
            "isDraft": False,
            "mergeStateStatus": merge_state,
            "reviewDecision": review_decision,
            "statusCheckRollup": [
                {
                    "__typename": "CheckRun",
                    "name": "Select Project QA contract",
                    "status": status,
                    "conclusion": conclusion,
                    "isRequired": True,
                },
                {
                    "__typename": "StatusContext",
                    "context": "PR docs contract",
                    "state": conclusion,
                    "isRequired": True,
                },
            ],
            "url": "https://github.com/rysweet/RabbitHole/pull/437",
        }
    )


class FakeCommandRunner:
    def __init__(self, outputs: dict[tuple[str, ...], str] | None = None, error: Exception | None = None) -> None:
        self.outputs = outputs or {}
        self.error = error
        self.calls: list[tuple[str, ...]] = []

    def run(self, command: list[str] | tuple[str, ...], **_: Any) -> str:
        normalized = tuple(command)
        self.calls.append(normalized)
        if self.error is not None:
            raise self.error
        if normalized not in self.outputs:
            raise AssertionError(f"Unexpected command: {' '.join(normalized)}")
        return self.outputs[normalized]


class FlakyCommandRunner:
    def __init__(self, responses: list[Exception | str]) -> None:
        self.responses = responses
        self.calls: list[tuple[str, ...]] = []

    def run(self, command: list[str] | tuple[str, ...], **_: Any) -> str:
        self.calls.append(tuple(command))
        if not self.responses:
            raise AssertionError("No response configured")
        response = self.responses.pop(0)
        if isinstance(response, Exception):
            raise response
        return response


class Pr437FinalizationWorkflowTddTest(unittest.TestCase):
    def test_current_head_verifier_requires_exact_pr_head_before_evidence_conclusions(self) -> None:
        """Unit contract: local evidence is usable only after HEAD equals the PR head."""
        finalizer = load_finalizer()
        runner = FakeCommandRunner({("git", "rev-parse", "HEAD"): f"{EXPECTED_PR_HEAD}\n"})

        verification = finalizer.CurrentHeadVerifier(
            expected_head=EXPECTED_PR_HEAD,
            runner=runner,
        ).verify()

        self.assertEqual(EXPECTED_PR_HEAD, verification.local_head)
        self.assertEqual(EXPECTED_PR_HEAD, verification.evidence_basis_sha)

        mismatch_runner = FakeCommandRunner(
            {("git", "rev-parse", "HEAD"): "d72185e8c8d552c7e3d9b92f795caebc45b6acb5\n"}
        )
        with self.assertRaises(finalizer.HeadMismatchError):
            finalizer.CurrentHeadVerifier(
                expected_head=EXPECTED_PR_HEAD,
                runner=mismatch_runner,
            ).verify()

    def test_github_evidence_collector_fetches_current_pr_metadata_for_exact_head(self) -> None:
        """Unit contract: GitHub evidence is live PR #437 metadata tied to the exact head."""
        finalizer = load_finalizer()
        runner = FakeCommandRunner(
            {
                (
                    "gh",
                    "pr",
                    "view",
                    "437",
                    "--repo",
                    EXPECTED_REPO,
                    "--json",
                    "number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url",
                ): clean_green_pr_json()
            }
        )

        evidence = finalizer.GitHubEvidenceCollector(
            pr_number=437,
            repo=EXPECTED_REPO,
            expected_head=EXPECTED_PR_HEAD,
            runner=runner,
        ).collect()

        self.assertEqual(EXPECTED_PR_HEAD, evidence.head_ref_oid)
        self.assertEqual("CLEAN", evidence.merge_state_status)
        self.assertEqual("", evidence.review_decision)
        self.assertEqual(EXPECTED_BRANCH, evidence.head_ref_name)
        self.assertEqual("OPEN", evidence.state)
        self.assertFalse(evidence.is_draft)
        self.assertTrue(all(check.required for check in evidence.checks))
        self.assertTrue(all(check.status == "COMPLETED" for check in evidence.checks))

        command_text = " ".join(runner.calls[0])
        self.assertNotIn("timeout", command_text)
        self.assertIn("gh pr view 437 --repo rysweet/RabbitHole", command_text)

    def test_github_service_adapter_retries_transient_metadata_failures(self) -> None:
        """Service boundary contract: transient external metadata failures retry before blocking."""
        finalizer = load_finalizer()
        runner = FlakyCommandRunner(
            [
                RuntimeError("network connectivity failed"),
                RuntimeError("rate limiting"),
                clean_green_pr_json(),
            ]
        )
        service_adapter = finalizer.ExternalCommandServiceAdapter(
            runner=runner,
            retry_policy=finalizer.ExternalRetryPolicy(attempts=3, delay_seconds=0),
            sleeper=lambda _: None,
        )

        evidence = finalizer.GitHubEvidenceCollector(
            pr_number=437,
            repo=EXPECTED_REPO,
            expected_head=EXPECTED_PR_HEAD,
            runner=runner,
            service_adapter=service_adapter,
        ).collect()

        self.assertEqual(EXPECTED_PR_HEAD, evidence.head_ref_oid)
        self.assertEqual(3, len(runner.calls))
        self.assertTrue(all(call[0:3] == ("gh", "pr", "view") for call in runner.calls))

    def test_github_evidence_collector_rejects_stale_or_unavailable_metadata(self) -> None:
        """Error contract: stale heads and unavailable metadata block finalization."""
        finalizer = load_finalizer()
        stale_runner = FakeCommandRunner(
            {
                (
                    "gh",
                    "pr",
                    "view",
                    "437",
                    "--repo",
                    EXPECTED_REPO,
                    "--json",
                    "number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url",
                ): clean_green_pr_json(head="d72185e8c8d552c7e3d9b92f795caebc45b6acb5")
            }
        )

        with self.assertRaises(finalizer.HeadMismatchError):
            finalizer.GitHubEvidenceCollector(
                pr_number=437,
                repo=EXPECTED_REPO,
                expected_head=EXPECTED_PR_HEAD,
                runner=stale_runner,
            ).collect()

        unavailable_runner = FakeCommandRunner(error=RuntimeError("gh pr view failed"))
        with self.assertRaises(finalizer.ExternalMetadataError):
            finalizer.GitHubEvidenceCollector(
                pr_number=437,
                repo=EXPECTED_REPO,
                expected_head=EXPECTED_PR_HEAD,
                runner=unavailable_runner,
            ).collect()

    def test_merge_readiness_evaluator_treats_clean_success_as_merge_ready_owner_free_not_approved(self) -> None:
        """Unit contract: CLEAN plus required SUCCESS checks is ready, not approval."""
        finalizer = load_finalizer()

        readiness = finalizer.MergeReadinessEvaluator().evaluate(json.loads(clean_green_pr_json()))

        self.assertTrue(readiness.merge_ready)
        self.assertEqual("clean-green", readiness.reason)
        self.assertEqual("owner-free/unset", readiness.review_state)
        self.assertFalse(readiness.approved)
        self.assertFalse(readiness.requires_disposable_merge_check)

    def test_merge_readiness_evaluator_blocks_failed_or_pending_required_checks(self) -> None:
        """Edge contract: one failed or pending required check blocks merge readiness."""
        finalizer = load_finalizer()

        failed = finalizer.MergeReadinessEvaluator().evaluate(
            json.loads(clean_green_pr_json(conclusion="FAILURE"))
        )
        pending = finalizer.MergeReadinessEvaluator().evaluate(
            json.loads(clean_green_pr_json(status="IN_PROGRESS", conclusion=""))
        )

        self.assertFalse(failed.merge_ready)
        self.assertIn("required checks", failed.blocker)
        self.assertFalse(pending.merge_ready)
        self.assertIn("required checks", pending.blocker)

    def test_focused_select_project_validator_accepts_only_starter_lane_claims(self) -> None:
        """Unit contract: Select Project evidence cannot expand into unrelated Alice behavior."""
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()

        focused = validator.validate(
            [
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection/open attempt",
            ]
        )

        self.assertEqual(
            [
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection/open attempt",
            ],
            focused.accepted_claims,
        )

        for forbidden_claim in [
            "visible rendering correctness",
            "Save completion",
            "grading",
            "lesson completion",
            "world interaction",
        ]:
            with self.subTest(forbidden_claim=forbidden_claim):
                with self.assertRaises(finalizer.ScopeViolationError):
                    validator.validate(["Select Project visibility", forbidden_claim])

    def test_change_gate_separates_noop_edit_push_and_blocked_decisions(self) -> None:
        """Integration contract: no-op is impossible when files changed or evidence is blocked."""
        finalizer = load_finalizer()
        gate = finalizer.ChangeGate()

        no_op = gate.decide(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=True,
            review_state="owner-free/unset",
            focused_scope_valid=True,
            worktree_changes=[],
            stale_evidence=False,
        )
        edit_and_push = gate.decide(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=True,
            review_state="owner-free/unset",
            focused_scope_valid=True,
            worktree_changes=["tests/test_pr437_finalization_workflow.py"],
            stale_evidence=False,
        )
        blocked = gate.decide(
            local_head="d72185e8c8d552c7e3d9b92f795caebc45b6acb5",
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=True,
            review_state="owner-free/unset",
            focused_scope_valid=True,
            worktree_changes=[],
            stale_evidence=False,
        )

        self.assertEqual("NO_OP", no_op.action)
        self.assertEqual("EDIT_AND_PUSH", edit_and_push.action)
        self.assertEqual("BLOCKED_WITH_REASON", blocked.action)
        self.assertIn("head mismatch", blocked.reason)

    def test_final_report_generator_documents_noop_without_approval_or_broadened_scope(self) -> None:
        """Integration contract: no-op reports cite exact current-head evidence and narrow scope."""
        finalizer = load_finalizer()

        report = finalizer.FinalReportGenerator().generate(
            action="NO_OP",
            current_head=EXPECTED_PR_HEAD,
            branch=EXPECTED_BRANCH,
            merge_state_status="CLEAN",
            required_check_conclusion="SUCCESS",
            review_state="owner-free/unset",
            modified_files=[],
            focused_evidence=[
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection/open attempt",
            ],
        )

        self.assertIn("No-op justification:", report)
        self.assertIn(EXPECTED_PR_HEAD, report)
        self.assertIn("mergeStateStatus=CLEAN", report)
        self.assertIn("SUCCESS", report)
        self.assertIn("reviewDecision", report)
        self.assertIn("owner-free/unset", report)
        self.assertIn("no repository files are modified", report)
        self.assertIn("Select Project visibility", report)
        self.assertIn("Starters tab activation", report)
        self.assertIn("Africa Full target selection/open attempt", report)
        self.assertNotIn("approved", report.lower())
        self.assertNotIn("rendering", report.lower())
        self.assertNotIn("Save", report)
        self.assertNotIn("grading", report.lower())
        self.assertNotIn("lesson", report.lower())
        self.assertNotIn("world interaction", report.lower())

    def test_final_report_generator_documents_edit_and_push_with_modified_files_after_push(self) -> None:
        """Integration contract: pushed changes use EDIT_AND_PUSH, not no-op language."""
        finalizer = load_finalizer()

        report = finalizer.FinalReportGenerator().generate(
            action="EDIT_AND_PUSH",
            current_head=EXPECTED_PR_HEAD,
            branch=EXPECTED_BRANCH,
            merge_state_status="CLEAN",
            required_check_conclusion="SUCCESS",
            review_state="owner-free/unset",
            modified_files=["tests/test_pr437_finalization_workflow.py"],
            focused_evidence=["exact blocker: no live AT-SPI artifact required for contract-only TDD"],
        )

        self.assertIn("Report path: `EDIT_AND_PUSH`", report)
        self.assertIn("tests/test_pr437_finalization_workflow.py", report)
        self.assertIn("after the commit/push", report)
        self.assertIn(EXPECTED_PR_HEAD, report)
        self.assertIn("owner-free/unset", report)
        self.assertNotIn("No-op justification:", report)


if __name__ == "__main__":
    unittest.main()
