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
            "docs/reference/gadugi-run-execution-gap-evidence.md",
            "docs/tutorials/trace-no-timeout-pr-recovery.md",
            "qa/outside-in/alice-desktop/gadugi/run-execution-gap-evidence.yaml",
            "qa/outside-in/alice-desktop/tests/test-gadugi-run-execution-gap-contract.sh",
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


GADUGI_EVIDENCE_DOC = "docs/reference/gadugi-run-execution-gap-evidence.md"

GADUGI_EVIDENCE_NON_CLAIMS = [
    "Full world execution",
    "Visible rendering correctness",
    "Grading",
    "Save completion",
    "Full UI automation",
    "Playback",
    "Sims validation",
    "Deployed installer success",
    "Full lesson completion",
    "Creative assessment",
]

GADUGI_EVIDENCE_IMPLEMENTATION_FILES = [
    "qa/outside-in/alice-desktop/gadugi/run-execution-gap-evidence.yaml",
    "qa/outside-in/alice-desktop/tests/test-gadugi-run-execution-gap-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh",
]


class GadugiRunExecutionGapEvidenceDocsContractTest(unittest.TestCase):
    def test_gadugi_evidence_doc_lists_every_bounded_non_claim(self) -> None:
        """Evidence boundaries section must list every non-claim to prevent drift."""
        text = read_doc(GADUGI_EVIDENCE_DOC)
        does_not_prove_section = text.split("It does not prove:", 1)
        self.assertEqual(
            2,
            len(does_not_prove_section),
            "Gadugi evidence doc must contain an 'It does not prove:' section",
        )
        boundaries_text = does_not_prove_section[1].split("##", 1)[0]

        for non_claim in GADUGI_EVIDENCE_NON_CLAIMS:
            self.assertIn(
                non_claim.lower().rstrip("."),
                boundaries_text.lower(),
                f"Evidence boundaries must list {non_claim!r}",
            )

    def test_gadugi_evidence_doc_lists_implementation_files(self) -> None:
        """Implementation files section must reference the YAML and both contract tests."""
        text = read_doc(GADUGI_EVIDENCE_DOC)

        for impl_file in GADUGI_EVIDENCE_IMPLEMENTATION_FILES:
            self.assertIn(
                impl_file,
                text,
                f"Gadugi evidence doc must reference {impl_file!r}",
            )

    def test_gadugi_evidence_doc_references_correct_scenario_name_and_directory(self) -> None:
        """Doc must reference the stable scenario name and Gadugi directory path."""
        text = read_doc(GADUGI_EVIDENCE_DOC)

        self.assertIn("run-execution-gap-evidence", text)
        self.assertIn("qa/outside-in/alice-desktop/gadugi", text)
        self.assertNotIn(
            "qa/outside-in/alice-desktop/scenarios/run-execution-gap-evidence",
            text,
            "Doc must not confuse Gadugi directory with custom scenario catalog",
        )

    def test_gadugi_evidence_doc_validation_commands_reference_real_scripts(self) -> None:
        """Validation commands section must reference scripts that actually exist."""
        text = read_doc(GADUGI_EVIDENCE_DOC)

        validation_scripts = [
            "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
            "qa/outside-in/alice-desktop/tests/run-tests.sh",
            "qa/outside-in/alice-desktop/tests/test-gadugi-run-execution-gap-contract.sh",
            "qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh",
        ]
        for script in validation_scripts:
            self.assertIn(script, text, f"Validation commands must reference {script!r}")
            self.assertTrue(
                (REPO_ROOT / script).is_file(),
                f"Referenced script {script!r} must exist on disk",
            )

    def test_gadugi_evidence_doc_does_not_overclaim_scope(self) -> None:
        """Doc scope section must use bounded wording and not overclaim."""
        text = read_doc(GADUGI_EVIDENCE_DOC)
        scope_match = re.search(
            r"## Scope\n\n(.*?)(?=\n## )", text, re.DOTALL
        )
        self.assertIsNotNone(scope_match, "Doc must have a Scope section")
        scope_text = scope_match.group(1) if scope_match else ""

        self.assertIn("narrower than", scope_text)
        self.assertIn("prepare-only", scope_text)
        self.assertNotIn("proves full world execution", scope_text.lower())
        self.assertNotIn("validates rendering", scope_text.lower())


if __name__ == "__main__":
    unittest.main()
