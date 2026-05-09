# tests/test_pr437_dirty_recovery_workflow.py
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
FOCUSED_SELECT_PROJECT_VALIDATION = [
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-select-project-proof.sh",
    "qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh",
    "qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh",
    (
        "python3 -m unittest "
        "tests/test_pr437_select_project_recovery_contract.py "
        "tests/test_pr437_finalization_workflow.py "
        "tests/test_pr437_noop_recovery_report_contract.py "
        "tests/test_pr437_dirty_recovery_workflow.py"
    ),
]


def load_finalizer() -> Any:
    if not FINALIZER_PATH.exists():
        raise AssertionError(f"Expected PR #437 finalization workflow at {FINALIZER_PATH}")
    spec = importlib.util.spec_from_file_location("pr437_finalization", FINALIZER_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def current_git_head() -> str:
    return subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    ).stdout.strip()


def pr_json(
    *,
    head: str,
    merge_state: str,
    status: str = "COMPLETED",
    conclusion: str = "SUCCESS",
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
        "reviewDecision": None,
        "statusCheckRollup": [
            {
                "__typename": "CheckRun",
                "name": "Select Project QA contract",
                "status": status,
                "conclusion": conclusion,
                "isRequired": True,
            }
        ],
        "url": "https://github.com/rysweet/RabbitHole/pull/437",
    }


class Pr437DirtyRecoveryWorkflowTddTest(unittest.TestCase):
    def test_expected_pr_head_constant_is_refreshed_after_repair_commit(self) -> None:
        """Unit contract: finalization constants must track the current pushed PR head."""
        finalizer = load_finalizer()

        self.assertEqual(
            current_git_head(),
            finalizer.EXPECTED_PR_HEAD,
            "scripts/pr437-finalization.py must be refreshed whenever a dirty-repair commit changes PR #437 HEAD",
        )

    def test_dirty_metadata_is_merge_dirtiness_not_clean_green_or_noop(self) -> None:
        """Edge contract: DIRTY PR metadata requires dirty recovery evidence, never no-op evidence."""
        finalizer = load_finalizer()

        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(head=finalizer.EXPECTED_PR_HEAD, merge_state="DIRTY")
        )

        self.assertFalse(readiness.merge_ready)
        self.assertTrue(readiness.requires_disposable_merge_check)
        self.assertIn("merge dirtiness", readiness.blocker.lower())
        self.assertNotEqual("clean-green", readiness.reason)

    def test_dirty_recovery_planner_uses_edit_and_push_with_focused_validation(self) -> None:
        """Integration contract: dirty recovery reconciles develop and validates the Select Project lane."""
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(head=finalizer.EXPECTED_PR_HEAD, merge_state="DIRTY")
        )

        plan = finalizer.DirtyRecoveryPlanner(
            repo=EXPECTED_REPO,
            pr_number=437,
            branch=EXPECTED_BRANCH,
            base_ref="develop",
            node_options="--max-old-space-size=32768",
        ).plan(
            local_head=finalizer.EXPECTED_PR_HEAD,
            pr_head=finalizer.EXPECTED_PR_HEAD,
            readiness=readiness,
            changed_files=[
                "scripts/pr437-finalization.py",
                "tests/test_pr437_dirty_recovery_workflow.py",
            ],
        )

        self.assertEqual("EDIT_AND_PUSH", plan.action)
        self.assertEqual("origin/develop", plan.reconcile_ref)
        self.assertFalse(plan.noop_allowed)
        self.assertIn("git fetch origin develop", plan.reconcile_commands)
        self.assertNotIn("git merge develop", plan.reconcile_commands)
        for command in FOCUSED_SELECT_PROJECT_VALIDATION:
            with self.subTest(command=command):
                self.assertIn(command, plan.validation_commands)
        self.assertEqual(
            [
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection evidence",
                "Africa Full open attempt evidence",
            ],
            plan.focused_evidence_claims,
        )

    def test_dirty_recovery_rejects_noop_mode_even_when_no_files_remain_changed(self) -> None:
        """Error contract: dirty repair cannot be represented as no-op after conflict resolution."""
        finalizer = load_finalizer()
        readiness = finalizer.MergeReadinessEvaluator().evaluate(
            pr_json(head=finalizer.EXPECTED_PR_HEAD, merge_state="DIRTY")
        )
        planner = finalizer.DirtyRecoveryPlanner(
            repo=EXPECTED_REPO,
            pr_number=437,
            branch=EXPECTED_BRANCH,
            base_ref="develop",
            node_options="--max-old-space-size=32768",
        )

        with self.assertRaises(finalizer.NoOpDirtyRecoveryError):
            planner.plan(
                local_head=finalizer.EXPECTED_PR_HEAD,
                pr_head=finalizer.EXPECTED_PR_HEAD,
                readiness=readiness,
                changed_files=[],
                requested_action="NO_OP",
            )

    def test_focused_evidence_validator_tracks_africa_full_selection_and_open_separately(self) -> None:
        """Unit contract: evidence scope is narrow but distinguishes target selection from open attempt."""
        finalizer = load_finalizer()

        focused = finalizer.FocusedSelectProjectValidator().validate(
            [
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection evidence",
                "Africa Full open attempt evidence",
            ]
        )

        self.assertTrue(focused.valid)
        self.assertEqual(
            [
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection evidence",
                "Africa Full open attempt evidence",
            ],
            focused.accepted_claims,
        )

    def test_blocked_report_uses_not_merge_ready_with_concrete_blockers(self) -> None:
        """Error contract: blocked dirty recovery reports explicit NOT_MERGE_READY blockers."""
        finalizer = load_finalizer()

        report = finalizer.FinalReportGenerator().generate(
            action="BLOCKED_WITH_REASON",
            current_head=finalizer.EXPECTED_PR_HEAD,
            branch=EXPECTED_BRANCH,
            merge_state_status="DIRTY",
            required_check_conclusion="SUCCESS",
            review_state="owner-free/unset",
            modified_files=[],
            focused_evidence=[
                "exact blocker: merge dirtiness against current origin/develop",
                "exact blocker: missing focused Select Project validation",
            ],
        )

        self.assertIn("NOT_MERGE_READY", report)
        self.assertIn("Concrete blockers:", report)
        self.assertIn("merge dirtiness against current origin/develop", report)
        self.assertIn("missing focused Select Project validation", report)
        self.assertIn("Do not merge manually", report)
        self.assertIn("Do not use no-op mode", report)
        self.assertNotIn("No-op justification:", report)

    def test_live_pr_metadata_payload_for_dirty_repair_is_serializable_and_head_bound(self) -> None:
        """Unit contract: dirty-repair evidence remains tied to exact live PR metadata fields."""
        finalizer = load_finalizer()
        payload = pr_json(head=finalizer.EXPECTED_PR_HEAD, merge_state="DIRTY")

        encoded = json.dumps(payload)
        decoded = json.loads(encoded)

        self.assertEqual(437, decoded["number"])
        self.assertEqual(EXPECTED_BRANCH, decoded["headRefName"])
        self.assertEqual(finalizer.EXPECTED_PR_HEAD, decoded["headRefOid"])
        self.assertEqual("develop", decoded["baseRefName"])
        self.assertEqual("DIRTY", decoded["mergeStateStatus"])
        self.assertIn("statusCheckRollup", decoded)


if __name__ == "__main__":
    unittest.main()
