import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
EVIDENCE_PATH = REPO_ROOT / "docs" / "reference" / "pr401-ui-action-menu-contract-evidence.md"
INDEX_PATH = REPO_ROOT / "docs" / "index.md"
SCENARIO_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "menu-action-smoke.yaml"
SCHEMA_CONTRACT_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-schema-contract.sh"

EXPECTED_SOURCE_CONTRACT_HEAD = "ae63ebfb94111aa01b42e5b164dd27687dbd9622"
SCENARIO_ID = "alice-desktop-menu-action-smoke"
WORKFLOW = "menu-action-smoke"
AUTOMATION_MODE = "gated-command-smoke"
JAVA_CONTRACT = "org.alice.ide.croquet.models.AliceMenuBarContractTest"
ACCEPTED_CLAIM = (
    "WindowMenuModel is registered in the Alice desktop menu-bar model, keeps the\n"
    "expected stable identity, and is reachable through menu-bar membership lookup."
)
NON_CLAIMS = [
    "Full UI automation",
    "Visible rendering correctness",
    "Live Swing menu opening or click behavior",
    "Save, Save As, export, or write/readback completion",
    "First-lesson completion",
    "Lesson correctness",
    "Grading, learner assessment, or rubric correctness",
]


def section(text: str, heading: str) -> str:
    pattern = rf"^## {re.escape(heading)}\n(?P<body>.*?)(?=^## |\Z)"
    match = re.search(pattern, text, flags=re.MULTILINE | re.DOTALL)
    if not match:
        raise AssertionError(f"Missing section: {heading}")
    return match.group("body")


def fenced_block(text: str, heading: str, language: str = "text") -> str:
    body = section(text, heading)
    match = re.search(rf"```{re.escape(language)}\n(?P<block>.*?)\n```", body, flags=re.DOTALL)
    if not match:
        raise AssertionError(f"Missing {language} fenced block in section: {heading}")
    return match.group("block")


class Pr401UiActionMenuContractEvidenceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.evidence = EVIDENCE_PATH.read_text(encoding="utf-8")

    def test_handoff_names_validated_source_contract_head_in_all_review_surfaces(self) -> None:
        for heading in [
            "Evidence basis",
            "Readiness evidence",
            "Review evidence",
            "Finalization evidence",
            "No-op source justification",
        ]:
            with self.subTest(heading=heading):
                self.assertIn(EXPECTED_SOURCE_CONTRACT_HEAD, section(self.evidence, heading))

        evidence_basis = section(self.evidence, "Evidence basis")
        self.assertIn(
            f"| Validated source-contract head | `{EXPECTED_SOURCE_CONTRACT_HEAD}` |",
            self.evidence,
        )
        self.assertNotIn("| Reviewed head |", self.evidence)
        self.assertIn(
            "Documentation-only commits that add or refine this handoff are not source",
            evidence_basis,
        )
        self.assertIn("documentation checkout HEAD differ", evidence_basis)
        self.assertIn(
            f"`{EXPECTED_SOURCE_CONTRACT_HEAD}` from `git rev-parse HEAD`",
            evidence_basis,
        )
        self.assertIn(
            "Review/finalization handoff only; no Java, runner, schema, validator, scenario, or test change is required",
            evidence_basis,
        )
        self.assertIn(
            f"| Source-contract head identified | `git rev-parse HEAD` reports `{EXPECTED_SOURCE_CONTRACT_HEAD}` in the source-contract checkout. |",
            self.evidence,
        )
        review_block = fenced_block(self.evidence, "Review evidence")
        self.assertIn(f"Validated source-contract head: {EXPECTED_SOURCE_CONTRACT_HEAD}", review_block)
        self.assertNotIn("Reviewed head:", review_block)

    def test_handoff_scope_is_limited_to_existing_menu_action_contract_wiring(self) -> None:
        scope = section(self.evidence, "Scope")
        feature_boundary = section(self.evidence, "Feature boundary")
        evidence_basis = section(self.evidence, "Evidence basis")

        self.assertIn(ACCEPTED_CLAIM, scope)
        self.assertIn("Window menu model registration, stable identity, and menu-bar membership lookup", evidence_basis)
        self.assertIn(SCENARIO_ID, evidence_basis)
        self.assertIn(WORKFLOW, evidence_basis)
        self.assertIn(AUTOMATION_MODE, evidence_basis)
        self.assertIn(JAVA_CONTRACT, evidence_basis)
        self.assertIn("fixed, gated command smoke", feature_boundary)

    def test_handoff_accepts_direct_maven_or_report_evidence_without_requiring_gated_runner(self) -> None:
        evidence_basis = section(self.evidence, "Evidence basis")
        usage = section(self.evidence, "Usage")
        configuration = section(self.evidence, "Configuration")
        readiness = section(self.evidence, "Readiness evidence")

        self.assertIn(
            "Maven selector, command log, or Surefire report naming `AliceMenuBarContractTest`",
            evidence_basis,
        )
        self.assertIn(
            "`command.log` is accepted only for an intentionally gated runner execution outside this no-wrapper recovery",
            readiness,
        )
        self.assertIn("without a timeout-wrapper execution path", usage)
        self.assertIn("Set to `1` only when intentionally executing the gated runner path", configuration)
        self.assertIn("not required for the direct-Maven/no-wrapper recovery path", configuration)
        self.assertNotIn("Direct Maven/Surefire output names", self.evidence)
        self.assertNotIn("Required value", configuration)

    def test_readiness_review_and_noop_sections_document_no_source_blocker(self) -> None:
        readiness = section(self.evidence, "Readiness evidence")
        review_block = fenced_block(self.evidence, "Review evidence")
        blockers = section(self.evidence, "Blocker register")
        noop = section(self.evidence, "No-op source justification")

        self.assertIn("Source blocker: none found", review_block)
        self.assertIn("Source outcome: no source change required for PR #401 recovery", review_block)
        self.assertIn("Handoff outcome: review/finalization handoff only", review_block)
        self.assertIn("No blocking source blocker was found for the validated source-contract head.", blockers)
        self.assertIn("No source modification is required for this recovery", noop)
        self.assertIn("No-op justification:", noop)
        self.assertIn("at validated source-contract head", noop)
        self.assertIn("Claim boundary preserved", readiness)

        for blocker in [
            "Repeated rate-limit exits before finalization",
            "Full UI automation not available",
            "Visible rendering correctness not proven",
            "Grading or learner assessment not proven",
            "Lesson completion not proven",
            "Later documentation-only handoff commit",
        ]:
            with self.subTest(blocker=blocker):
                self.assertRegex(blockers, rf"\| {re.escape(blocker)} \| Non-blocking")

    def test_finalization_evidence_is_bounded_to_executable_current_head_checks(self) -> None:
        finalization = section(self.evidence, "Finalization evidence")

        self.assertIn(
            f"`wave6-ui-action-menu-contract-1778302300` at `{EXPECTED_SOURCE_CONTRACT_HEAD}`",
            finalization,
        )
        for command in [
            "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
            "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
            "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
            "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh",
            "qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh",
            JAVA_CONTRACT,
        ]:
            with self.subTest(command=command):
                self.assertIn(command, finalization)

        self.assertIn("no manual merge is performed", finalization)
        self.assertIn("bounded menu/action contract handoff only", finalization)
        self.assertIn("does not prove full UI automation", finalization)
        self.assertIn("does not prove full", finalization)
        self.assertIn("desktop Save behavior", finalization)
        self.assertIn("full desktop Save completion", finalization)

    def test_non_claims_are_explicit_without_success_wording_for_out_of_scope_behaviors(self) -> None:
        non_claims = section(self.evidence, "Non-claims")

        for phrase in NON_CLAIMS:
            with self.subTest(phrase=phrase):
                self.assertIn(phrase, non_claims)

        disallowed_success_patterns = [
            r"full UI automation (?:passed|proven|complete|established)",
            r"visible rendering correctness (?:passed|proven|complete|established)",
            r"grading correctness (?:passed|proven|complete|established)",
            r"lesson completion correctness (?:passed|proven|complete|established)",
        ]
        for pattern in disallowed_success_patterns:
            with self.subTest(pattern=pattern):
                self.assertIsNone(re.search(pattern, self.evidence, flags=re.IGNORECASE))

    def test_index_and_existing_wiring_reference_the_same_contract_surfaces(self) -> None:
        index = INDEX_PATH.read_text(encoding="utf-8")
        scenario = SCENARIO_PATH.read_text(encoding="utf-8")
        schema_contract = SCHEMA_CONTRACT_PATH.read_text(encoding="utf-8")

        self.assertIn("./reference/pr401-ui-action-menu-contract-evidence.md", index)
        self.assertIn(SCENARIO_ID, scenario)
        self.assertIn(f"workflow: {WORKFLOW}", scenario)
        self.assertIn(f"automationMode: {AUTOMATION_MODE}", scenario)
        self.assertIn(f"-Dtest={JAVA_CONTRACT}", scenario)
        self.assertIn(f'"-Dtest={JAVA_CONTRACT}"', schema_contract)


if __name__ == "__main__":
    unittest.main()
