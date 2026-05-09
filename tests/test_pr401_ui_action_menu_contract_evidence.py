import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
EVIDENCE_PATH = REPO_ROOT / "docs" / "reference" / "pr401-ui-action-menu-contract-evidence.md"
INDEX_PATH = REPO_ROOT / "docs" / "index.md"
SCENARIO_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "menu-action-smoke.yaml"
SCHEMA_CONTRACT_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-schema-contract.sh"

EXPECTED_HEAD = "0366dfa17f0f41e2d878c293a6c33fb1f841993a"
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

    def test_handoff_names_exact_reviewed_head_in_all_review_surfaces(self) -> None:
        for heading in ["Evidence basis", "Readiness evidence", "Review evidence"]:
            with self.subTest(heading=heading):
                self.assertIn(EXPECTED_HEAD, section(self.evidence, heading))

        evidence_basis = section(self.evidence, "Evidence basis")
        self.assertIn(f"| Reviewed head | `{EXPECTED_HEAD}` |", self.evidence)
        self.assertIn("Documentation-only commits that add or refine this handoff are not source", evidence_basis)
        self.assertIn(f"`{EXPECTED_HEAD}` from `git rev-parse HEAD`", evidence_basis)
        self.assertIn(
            "Documentation/test recovery only; no Java, runner, schema, validator, or scenario source change is required",
            evidence_basis,
        )
        self.assertIn(
            f"| Exact head identified | `git rev-parse HEAD` reports `{EXPECTED_HEAD}` in the reviewed checkout. |",
            self.evidence,
        )
        review_block = fenced_block(self.evidence, "Review evidence")
        self.assertIn(f"Reviewed head: {EXPECTED_HEAD}", review_block)

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

    def test_readiness_review_and_noop_sections_document_no_source_blocker(self) -> None:
        readiness = section(self.evidence, "Readiness evidence")
        review_block = fenced_block(self.evidence, "Review evidence")
        blockers = section(self.evidence, "Blocker register")
        noop = section(self.evidence, "No-op source justification")

        self.assertIn("Source blocker: none found", review_block)
        self.assertIn("Source outcome: no source change required for PR #401 recovery", review_block)
        self.assertIn("Handoff outcome: documentation/test recovery only", review_block)
        self.assertIn("No blocking source blocker was found for the reviewed head.", blockers)
        self.assertIn("No source modification is required for this recovery", noop)
        self.assertIn("exact-head evidence", noop)
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
