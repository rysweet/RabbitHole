import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
HOWTO_PATH = REPO_ROOT / "docs" / "howto" / "open-africa-full-through-select-project-atspi.md"
REFERENCE_PATH = REPO_ROOT / "docs" / "reference" / "select-project-africa-full-atspi-evidence.md"

EXPECTED_BRANCH = "feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo"
EXPECTED_HEAD_MARKER = "<verified headRefOid matching git rev-parse HEAD>"

FOCUSED_CHECKS = [
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-scenario-validation.sh",
    "qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-select-project-proof.sh",
    "qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh",
    "qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh",
    "tests/test_pr437_select_project_recovery_contract.py",
    "tests/test_pr437_noop_recovery_report_contract.py",
]


class Pr437NoopRecoveryReportContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.howto = HOWTO_PATH.read_text(encoding="utf-8")
        cls.reference = REFERENCE_PATH.read_text(encoding="utf-8")
        cls.combined = f"{cls.howto}\n{cls.reference}"

    def test_noop_justification_uses_required_current_head_report_labels(self) -> None:
        """Unit contract: the no-op report has explicit labels for each required fact."""
        for label in [
            "No-op justification:",
            "Current branch:",
            "Current head:",
            "PR metadata command:",
            "Worktree cleanliness:",
            "Merge-ready evidence:",
            "Review metadata:",
            "Focused validation:",
            "Live artifact exception:",
        ]:
            with self.subTest(label=label):
                self.assertIn(label, self.combined)

    def test_noop_justification_requires_verified_branch_and_head_match(self) -> None:
        """Unit contract: PR #437 recovery evidence is tied to the verified branch/head."""
        self.assertIn(EXPECTED_BRANCH, self.combined)
        self.assertIn(EXPECTED_HEAD_MARKER, self.combined)
        self.assertRegex(
            self.combined,
            rf"No-op justification:[\s\S]*Current branch:\s*`?{re.escape(EXPECTED_BRANCH)}`?",
        )
        self.assertRegex(
            self.combined,
            rf"No-op justification:[\s\S]*Current head:\s*`?{re.escape(EXPECTED_HEAD_MARKER)}`?",
        )

    def test_focused_validation_command_list_includes_this_noop_contract(self) -> None:
        """Integration contract: readiness evidence runs every focused Select Project check."""
        for check in FOCUSED_CHECKS:
            with self.subTest(check=check):
                self.assertIn(check, self.howto)
                self.assertIn(check, self.reference)

    def test_no_live_artifact_exception_cannot_be_used_for_live_success_claims(self) -> None:
        """Edge contract: the no-op exception is explicit and cannot masquerade as live proof."""
        self.assertIn("Live artifact exception:", self.combined)
        self.assertRegex(
            self.combined,
            r"Live artifact exception:[\s\S]*verified existing documentation/contracts",
        )
        self.assertRegex(
            self.combined,
            r"Live artifact exception:[\s\S]*does not claim live Select Project success",
        )
        self.assertRegex(
            self.combined,
            r"Any claim that Africa Full was selected, opened, or observed after opening requires live artifacts",
        )

    def test_error_handling_blocks_noop_when_current_head_or_external_metadata_is_unverified(self) -> None:
        """Error contract: drift or unavailable GitHub metadata is a blocker, not a no-op."""
        for blocker_rule in [
            "branch/head drift",
            "Current blocker: environment dependency",
            "must not publish `No-op justification:`",
            "Do not silently substitute cached, historical, or manually typed PR metadata",
        ]:
            with self.subTest(blocker_rule=blocker_rule):
                self.assertIn(blocker_rule, self.combined)

    def test_final_output_shape_separates_readiness_review_and_finalization_evidence(self) -> None:
        """Integration contract: finalization reports stay evidence-ready and non-mutating."""
        for section in [
            "Readiness evidence:",
            "Review evidence:",
            "Finalization evidence:",
            "Files modified:",
        ]:
            with self.subTest(section=section):
                self.assertIn(section, self.combined)

        self.assertIn("evidence-ready only", self.combined)
        self.assertIn("does not authorize an agent to approve, merge, close, rebase", self.combined)


if __name__ == "__main__":
    unittest.main()
