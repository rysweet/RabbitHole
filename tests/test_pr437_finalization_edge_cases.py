# tests/test_pr437_finalization_edge_cases.py
"""TDD edge-case tests for scripts/pr437-finalization.py.

Covers untested paths: failure classification, worktree change collection,
ChangeGate boundary conditions, MergeReadinessEvaluator edge cases,
FocusedSelectProjectValidator blocker prefix handling, and environment
config parsing.
"""
from __future__ import annotations

import importlib.util
import subprocess
import sys
import unittest
from functools import cache
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


@cache
def load_finalizer() -> Any:
    if not FINALIZER_PATH.exists():
        raise AssertionError(f"Expected PR #437 finalization workflow at {FINALIZER_PATH}")
    spec = importlib.util.spec_from_file_location("pr437_finalization_edge", FINALIZER_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class FakeCommandRunner:
    def __init__(self, outputs: dict[tuple[str, ...], str] | None = None) -> None:
        self.outputs = outputs or {}
        self.calls: list[tuple[str, ...]] = []

    def run(self, command: list[str] | tuple[str, ...], **_: Any) -> str:
        normalized = tuple(command)
        self.calls.append(normalized)
        if normalized not in self.outputs:
            raise RuntimeError(f"Unexpected command: {' '.join(normalized)}")
        return self.outputs[normalized]


def pr_json(
    *,
    head: str = EXPECTED_PR_HEAD,
    merge_state: str = "CLEAN",
    conclusion: str = "SUCCESS",
    status: str = "COMPLETED",
    review_decision: str | None = None,
    required: bool = True,
) -> dict[str, Any]:
    return {
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
                "isRequired": required,
            }
        ],
        "url": "https://github.com/rysweet/RabbitHole/pull/437",
    }


class ClassifyExternalFailureTest(unittest.TestCase):
    """Unit contract: _classify_external_failure categorises error messages correctly."""

    def test_auth_failure(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("gh auth login required"))
        self.assertEqual("GitHub CLI authentication", result)

    def test_rate_limit_failure(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("secondary rate limit hit"))
        self.assertEqual("rate limiting", result)

    def test_network_failure(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("network connection timed out"))
        self.assertEqual("network connectivity", result)

    def test_generic_failure(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("something else"))
        self.assertEqual("GitHub CLI command failure", result)

    def test_none_error(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(None)
        self.assertEqual("unknown external service failure", result)

    def test_credential_classified_as_auth(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("missing credential"))
        self.assertEqual("GitHub CLI authentication", result)

    def test_could_not_resolve_classified_as_network(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("could not resolve host"))
        self.assertEqual("network connectivity", result)

    def test_tls_classified_as_network(self) -> None:
        finalizer = load_finalizer()
        result = finalizer._classify_external_failure(RuntimeError("tls handshake error"))
        self.assertEqual("network connectivity", result)


class CollectWorktreeChangesTest(unittest.TestCase):
    """Unit contract: collect_worktree_changes parses git status --short output."""

    def test_no_changes(self) -> None:
        finalizer = load_finalizer()
        runner = FakeCommandRunner({("git", "status", "--short"): ""})
        result = finalizer.collect_worktree_changes(runner)
        self.assertEqual([], result)

    def test_modified_and_new_files(self) -> None:
        finalizer = load_finalizer()
        output = " M tests/test_example.py\nA  new_file.py\n?? untracked.txt\n"
        runner = FakeCommandRunner({("git", "status", "--short"): output})
        result = finalizer.collect_worktree_changes(runner)
        self.assertIn("tests/test_example.py", result)
        self.assertIn("new_file.py", result)
        self.assertIn("untracked.txt", result)
        self.assertEqual(3, len(result))

    def test_blank_lines_skipped(self) -> None:
        finalizer = load_finalizer()
        output = " M file.py\n\n\n"
        runner = FakeCommandRunner({("git", "status", "--short"): output})
        result = finalizer.collect_worktree_changes(runner)
        self.assertEqual(["file.py"], result)


class MergeReadinessEdgeCasesTest(unittest.TestCase):
    """Edge contract: MergeReadinessEvaluator handles missing/ambiguous metadata."""

    def test_no_required_checks_blocks_readiness(self) -> None:
        finalizer = load_finalizer()
        evidence = pr_json(required=False)
        evidence["statusCheckRollup"][0]["isRequired"] = False
        readiness = finalizer.MergeReadinessEvaluator().evaluate(evidence)
        self.assertFalse(readiness.merge_ready)
        self.assertIn("required checks", readiness.blocker)

    def test_empty_check_rollup_blocks_readiness(self) -> None:
        finalizer = load_finalizer()
        evidence = pr_json()
        evidence["statusCheckRollup"] = []
        readiness = finalizer.MergeReadinessEvaluator().evaluate(evidence)
        self.assertFalse(readiness.merge_ready)
        self.assertEqual("NO_REQUIRED_CHECKS", readiness.required_check_conclusion)

    def test_unknown_merge_state_requires_disposable_check(self) -> None:
        finalizer = load_finalizer()
        for state in ["UNKNOWN", "UNSTABLE", ""]:
            with self.subTest(state=state):
                readiness = finalizer.MergeReadinessEvaluator().evaluate(
                    pr_json(merge_state=state)
                )
                self.assertFalse(readiness.merge_ready)
                self.assertTrue(readiness.requires_disposable_merge_check)

    def test_blocked_merge_state_does_not_require_disposable(self) -> None:
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(merge_state="BLOCKED")
        )
        self.assertFalse(readiness.merge_ready)
        self.assertFalse(readiness.requires_disposable_merge_check)

    def test_approved_review_is_recognized(self) -> None:
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(review_decision="APPROVED")
        )
        self.assertTrue(readiness.approved)
        self.assertEqual("APPROVED", readiness.review_state)

    def test_pending_check_blocks_readiness(self) -> None:
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(status="IN_PROGRESS", conclusion="")
        )
        self.assertFalse(readiness.merge_ready)
        self.assertIn("required checks", readiness.blocker)

    def test_dict_input_accepted(self) -> None:
        """MergeReadinessEvaluator accepts both GitHubEvidence and dict."""
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(pr_json())
        self.assertTrue(readiness.merge_ready)


class FocusedSelectProjectValidatorEdgesTest(unittest.TestCase):
    """Edge contract: FocusedSelectProjectValidator handles blocker prefixes and forbidden terms."""

    def test_blocker_prefix_accepted(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        result = validator.validate([
            "exact blocker: no live AT-SPI artifact required for contract-only TDD",
        ])
        self.assertEqual([], result.accepted_claims)
        self.assertIn("no live AT-SPI artifact", result.blocker)

    def test_empty_claims_accepted(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        result = validator.validate([])
        self.assertEqual([], result.accepted_claims)
        self.assertEqual("", result.blocker)

    def test_whitespace_only_claims_skipped(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        result = validator.validate(["", "  ", "\n"])
        self.assertEqual([], result.accepted_claims)

    def test_forbidden_rendering_term_raises(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        with self.assertRaises(finalizer.ScopeViolationError):
            validator.validate(["visible rendering correctness"])

    def test_forbidden_save_term_raises(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        with self.assertRaises(finalizer.ScopeViolationError):
            validator.validate(["Save completion"])

    def test_forbidden_grading_term_raises(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        with self.assertRaises(finalizer.ScopeViolationError):
            validator.validate(["grading results"])

    def test_unrecognized_claim_raises(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        with self.assertRaises(finalizer.ScopeViolationError):
            validator.validate(["some random claim"])

    def test_all_allowed_claims_accepted(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        result = validator.validate([
            "Select Project visibility",
            "Starters tab activation",
            "Africa Full target selection/open attempt",
            "Africa Full target selection evidence",
            "Africa Full open attempt evidence",
        ])
        self.assertEqual(5, len(result.accepted_claims))

    def test_multiple_blockers_joined(self) -> None:
        finalizer = load_finalizer()
        validator = finalizer.FocusedSelectProjectValidator()
        result = validator.validate([
            "exact blocker: first issue",
            "exact blocker: second issue",
        ])
        self.assertIn("first issue", result.blocker)
        self.assertIn("second issue", result.blocker)
        self.assertIn("; ", result.blocker)


class ChangeGateEdgeCasesTest(unittest.TestCase):
    """Edge contract: ChangeGate boundary decisions for review state, scope, and staleness."""

    def test_unexpected_review_state_blocks(self) -> None:
        finalizer = load_finalizer()
        decision = finalizer.ChangeGate().decide(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=True,
            review_state="CHANGES_REQUESTED",
            focused_scope_valid=True,
            worktree_changes=[],
            stale_evidence=False,
        )
        self.assertEqual("BLOCKED_WITH_REASON", decision.action)
        self.assertIn("review metadata", decision.reason)

    def test_invalid_scope_blocks(self) -> None:
        finalizer = load_finalizer()
        decision = finalizer.ChangeGate().decide(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=True,
            review_state="owner-free/unset",
            focused_scope_valid=False,
            worktree_changes=[],
            stale_evidence=False,
        )
        self.assertEqual("BLOCKED_WITH_REASON", decision.action)
        self.assertIn("scope", decision.reason)

    def test_stale_evidence_triggers_edit_push(self) -> None:
        finalizer = load_finalizer()
        decision = finalizer.ChangeGate().decide(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=True,
            review_state="owner-free/unset",
            focused_scope_valid=True,
            worktree_changes=[],
            stale_evidence=True,
        )
        self.assertEqual("EDIT_AND_PUSH", decision.action)
        self.assertIn("stale", decision.reason)

    def test_merge_not_ready_blocks(self) -> None:
        finalizer = load_finalizer()
        decision = finalizer.ChangeGate().decide(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            merge_ready=False,
            review_state="owner-free/unset",
            focused_scope_valid=True,
            worktree_changes=[],
            stale_evidence=False,
        )
        self.assertEqual("BLOCKED_WITH_REASON", decision.action)

    def test_priority_order_head_mismatch_first(self) -> None:
        """Head mismatch should block even when merge is not ready."""
        finalizer = load_finalizer()
        decision = finalizer.ChangeGate().decide(
            local_head="aaa",
            pr_head="bbb",
            merge_ready=False,
            review_state="owner-free/unset",
            focused_scope_valid=False,
            worktree_changes=["file.py"],
            stale_evidence=True,
        )
        self.assertEqual("BLOCKED_WITH_REASON", decision.action)
        self.assertIn("head mismatch", decision.reason)


class FinalReportGeneratorEdgeCasesTest(unittest.TestCase):
    """Edge contract: FinalReportGenerator handles unknown actions and empty evidence."""

    def test_unknown_action_raises(self) -> None:
        finalizer = load_finalizer()
        with self.assertRaises(ValueError):
            finalizer.FinalReportGenerator().generate(
                action="UNKNOWN_ACTION",
                current_head=EXPECTED_PR_HEAD,
                branch=EXPECTED_BRANCH,
                merge_state_status="CLEAN",
                required_check_conclusion="SUCCESS",
                review_state="owner-free/unset",
                modified_files=[],
                focused_evidence=[],
            )

    def test_noop_with_no_evidence_uses_none_placeholder(self) -> None:
        finalizer = load_finalizer()
        report = finalizer.FinalReportGenerator().generate(
            action="NO_OP",
            current_head=EXPECTED_PR_HEAD,
            branch=EXPECTED_BRANCH,
            merge_state_status="CLEAN",
            required_check_conclusion="SUCCESS",
            review_state="owner-free/unset",
            modified_files=[],
            focused_evidence=[],
        )
        self.assertIn("- None", report)

    def test_edit_push_with_multiple_files(self) -> None:
        finalizer = load_finalizer()
        report = finalizer.FinalReportGenerator().generate(
            action="EDIT_AND_PUSH",
            current_head=EXPECTED_PR_HEAD,
            branch=EXPECTED_BRANCH,
            merge_state_status="CLEAN",
            required_check_conclusion="SUCCESS",
            review_state="owner-free/unset",
            modified_files=["file1.py", "file2.py", "file3.py"],
            focused_evidence=["Select Project visibility"],
        )
        self.assertIn("file1.py", report)
        self.assertIn("file2.py", report)
        self.assertIn("file3.py", report)
        self.assertIn("after the commit/push", report)

    def test_blocked_report_includes_all_blockers(self) -> None:
        finalizer = load_finalizer()
        report = finalizer.FinalReportGenerator().generate(
            action="BLOCKED_WITH_REASON",
            current_head=EXPECTED_PR_HEAD,
            branch=EXPECTED_BRANCH,
            merge_state_status="DIRTY",
            required_check_conclusion="FAILURE",
            review_state="owner-free/unset",
            modified_files=[],
            focused_evidence=["exact blocker: first", "exact blocker: second"],
        )
        self.assertIn("first", report)
        self.assertIn("second", report)
        self.assertIn("Do not merge manually", report)


class DirtyRecoveryPlannerEdgeCasesTest(unittest.TestCase):
    """Edge contract: DirtyRecoveryPlanner handles head mismatch and clean PR states."""

    def test_head_mismatch_blocks_dirty_recovery(self) -> None:
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(merge_state="DIRTY")
        )
        plan = finalizer.DirtyRecoveryPlanner(
            repo=EXPECTED_REPO,
            pr_number=437,
            branch=EXPECTED_BRANCH,
            base_ref="develop",
            node_options="--max-old-space-size=32768",
        ).plan(
            local_head="aaa",
            pr_head="bbb",
            readiness=readiness,
            changed_files=[],
        )
        self.assertEqual("NOT_MERGE_READY", plan.action)
        self.assertIn("head mismatch", plan.blocker)

    def test_clean_pr_blocks_dirty_recovery(self) -> None:
        """Dirty recovery is unnecessary when PR is already clean."""
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(pr_json(merge_state="CLEAN"))
        plan = finalizer.DirtyRecoveryPlanner(
            repo=EXPECTED_REPO,
            pr_number=437,
            branch=EXPECTED_BRANCH,
            base_ref="develop",
            node_options="--max-old-space-size=32768",
        ).plan(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            readiness=readiness,
            changed_files=[],
        )
        self.assertEqual("NOT_MERGE_READY", plan.action)
        self.assertIn("already clean", plan.blocker)

    def test_validation_commands_include_node_options(self) -> None:
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(merge_state="DIRTY")
        )
        plan = finalizer.DirtyRecoveryPlanner(
            repo=EXPECTED_REPO,
            pr_number=437,
            branch=EXPECTED_BRANCH,
            base_ref="develop",
            node_options="--max-old-space-size=32768",
        ).plan(
            local_head=EXPECTED_PR_HEAD,
            pr_head=EXPECTED_PR_HEAD,
            readiness=readiness,
            changed_files=["test.py"],
        )
        for cmd in plan.validation_commands:
            self.assertTrue(
                cmd.startswith("NODE_OPTIONS=--max-old-space-size=32768 "),
                f"Command missing NODE_OPTIONS prefix: {cmd}",
            )


class ExternalRetryPolicyTest(unittest.TestCase):
    """Unit contract: ExternalRetryPolicy has sane defaults."""

    def test_default_values(self) -> None:
        finalizer = load_finalizer()
        policy = finalizer.ExternalRetryPolicy()
        self.assertEqual(3, policy.attempts)
        self.assertEqual(0.5, policy.delay_seconds)

    def test_custom_values(self) -> None:
        finalizer = load_finalizer()
        policy = finalizer.ExternalRetryPolicy(attempts=5, delay_seconds=2.0)
        self.assertEqual(5, policy.attempts)
        self.assertEqual(2.0, policy.delay_seconds)


class ExternalServiceAdapterRetryEdgesTest(unittest.TestCase):
    """Integration contract: ExternalCommandServiceAdapter exhaustion wraps as ExternalMetadataError."""

    def test_all_retries_exhausted_raises_external_metadata_error(self) -> None:
        finalizer = load_finalizer()

        class AlwaysFailRunner:
            calls: list[tuple[str, ...]] = []

            def run(self, command: list[str] | tuple[str, ...], **_: Any) -> str:
                self.calls.append(tuple(command))
                raise RuntimeError("persistent failure")

        runner = AlwaysFailRunner()
        adapter = finalizer.ExternalCommandServiceAdapter(
            runner=runner,
            retry_policy=finalizer.ExternalRetryPolicy(attempts=2, delay_seconds=0),
            sleeper=lambda _: None,
        )
        with self.assertRaises(finalizer.ExternalMetadataError) as ctx:
            adapter.run(["gh", "pr", "view"], dependency_name="test-dep")
        self.assertIn("test-dep failed after 2 attempts", str(ctx.exception))
        self.assertEqual(2, len(runner.calls))


class ParseCheckTest(unittest.TestCase):
    """Unit contract: _parse_check handles various check formats."""

    def test_check_run_format(self) -> None:
        finalizer = load_finalizer()
        check = finalizer._parse_check({
            "__typename": "CheckRun",
            "name": "CI Build",
            "status": "COMPLETED",
            "conclusion": "SUCCESS",
            "isRequired": True,
        })
        self.assertEqual("CI Build", check.name)
        self.assertEqual("COMPLETED", check.status)
        self.assertEqual("SUCCESS", check.conclusion)
        self.assertTrue(check.required)

    def test_status_context_format(self) -> None:
        finalizer = load_finalizer()
        check = finalizer._parse_check({
            "__typename": "StatusContext",
            "context": "PR docs contract",
            "state": "SUCCESS",
            "isRequired": True,
        })
        self.assertEqual("PR docs contract", check.name)
        self.assertEqual("SUCCESS", check.conclusion)

    def test_missing_fields_use_defaults(self) -> None:
        finalizer = load_finalizer()
        check = finalizer._parse_check({})
        self.assertEqual("unnamed check", check.name)
        self.assertEqual("COMPLETED", check.status)
        self.assertTrue(check.required)


class SummarizeRequiredChecksTest(unittest.TestCase):
    """Unit contract: _summarize_required_checks aggregates conclusions."""

    def test_all_success(self) -> None:
        finalizer = load_finalizer()
        checks = [
            finalizer.CheckEvidence("a", "COMPLETED", "SUCCESS", True),
            finalizer.CheckEvidence("b", "COMPLETED", "SUCCESS", True),
        ]
        self.assertEqual("SUCCESS", finalizer._summarize_required_checks(checks))

    def test_mixed_conclusions(self) -> None:
        finalizer = load_finalizer()
        checks = [
            finalizer.CheckEvidence("a", "COMPLETED", "SUCCESS", True),
            finalizer.CheckEvidence("b", "COMPLETED", "FAILURE", True),
        ]
        result = finalizer._summarize_required_checks(checks)
        self.assertIn("FAILURE", result)
        self.assertIn("SUCCESS", result)

    def test_empty_checks(self) -> None:
        finalizer = load_finalizer()
        self.assertEqual("NO_REQUIRED_CHECKS", finalizer._summarize_required_checks([]))


if __name__ == "__main__":
    unittest.main()
