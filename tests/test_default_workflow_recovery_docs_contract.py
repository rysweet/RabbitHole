import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]


def read_doc(relative_path: str) -> str:
    return (REPO_ROOT / relative_path).read_text(encoding="utf-8")


class DefaultWorkflowRecoveryDocsContractTest(unittest.TestCase):
    def test_recovery_howto_uses_parameterized_inputs_instead_of_pr404_literals(self) -> None:
        text = read_doc("docs/howto/recover-pr-with-default-workflow.md")

        self.assertIn('export PR_NUMBER="REPLACE_WITH_PULL_REQUEST_NUMBER"', text)
        self.assertIn('export HEAD_BRANCH="REPLACE_WITH_PULL_REQUEST_HEAD_BRANCH"', text)
        self.assertIn("For a concrete PR #404 run", text)
        self.assertNotIn("pr_number=404", text)
        self.assertNotIn("head_branch=wave6-run-execution-gap-1778302300", text)
        self.assertNotIn("git fetch origin wave6-run-execution-gap-1778302300", text)

    def test_recovery_report_design_inventory_lists_added_files_explicitly(self) -> None:
        text = read_doc("docs/reference/default-workflow-recovery-report.md")

        inventory = re.search(r"```yaml\nnew_files:\n(?P<body>(?:  - .+\n)+)```", text)
        self.assertIsNotNone(inventory, "design inventory must include a non-empty new_files block")
        inventory_body = inventory.group("body") if inventory else ""

        expected_added_files = [
            "docs/howto/recover-pr-with-default-workflow.md",
            "docs/reference/default-workflow-recovery-report.md",
            "docs/reference/desktop-run-execution-gap-report.md",
            "docs/tutorials/trace-no-timeout-pr-recovery.md",
            "qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh",
            "scripts/default_workflow_recovery.py",
            "tests/test_default_workflow_merge_ready_contract.py",
            "tests/test_default_workflow_recovery_docs_contract.py",
            "tests/test_default_workflow_recovery_contract.py",
        ]
        for added_file in expected_added_files:
            self.assertIn(f"  - {added_file}\n", inventory_body)

        self.assertIn("Do not summarize this feature as\n`new_files: []`", text)

    def test_gap_report_reference_names_implementation_enforced_non_claims(self) -> None:
        text = read_doc("docs/reference/desktop-run-execution-gap-report.md")

        self.assertIn(
            "doesNotClaim must include every implementation-enforced prohibited claim category",
            text,
        )
        self.assertIn("Contains the implementation-enforced categories", text)
        self.assertNotIn("doesNotClaim must include every prohibited claim category", text)

    def test_desktop_qa_howto_intro_keeps_scenario_families_scanable(self) -> None:
        text = read_doc("docs/howto/alice-desktop-outside-in-qa.md")
        intro = text.split("## Contents", 1)[0]
        normalized_intro = re.sub(r"\s+", " ", intro)

        self.assertIn("validate the scenario catalog", intro)
        self.assertIn("collect reviewable evidence for user-like workflows", intro)
        for scenario_family in [
            "launch and Select Project inventory",
            "first-lesson procedure seams",
            "bounded run/debug-like behavior",
            "save/load and export flows",
            "post-open runtime/display accessibility evidence",
        ]:
            self.assertIn(scenario_family, normalized_intro)


if __name__ == "__main__":
    unittest.main()
