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
SECTION_HEADING_PATTERN = re.compile(r"^## (?P<heading>.+)\n", flags=re.MULTILINE)
ACCEPTED_CLAIM = (
    "WindowMenuModel is registered in the Alice desktop menu-bar model, keeps the\n"
    "expected stable identity, and is reachable through menu-bar membership lookup."
)
RECOVERY_CHECKS = [
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh",
    JAVA_CONTRACT,
]
NON_CLAIMS = [
    "Full UI automation",
    "Visible rendering correctness",
    "Live Swing menu opening or click behavior",
    "Save, Save As, export, or write/readback completion",
    "First-lesson completion",
    "Lesson correctness",
    "Grading, learner assessment, or rubric correctness",
]


def markdown_sections(text: str) -> dict[str, str]:
    headings = list(SECTION_HEADING_PATTERN.finditer(text))
    sections: dict[str, str] = {}
    for index, match in enumerate(headings):
        next_heading = headings[index + 1].start() if index + 1 < len(headings) else len(text)
        sections[match.group("heading")] = text[match.end() : next_heading]
    return sections


def section(sections: dict[str, str], heading: str) -> str:
    try:
        return sections[heading]
    except KeyError as error:
        raise AssertionError(f"Missing section: {heading}") from error


def fenced_block(sections: dict[str, str], heading: str, language: str = "text") -> str:
    match = re.search(
        rf"```{re.escape(language)}\n(?P<block>.*?)\n```",
        section(sections, heading),
        flags=re.DOTALL,
    )
    if not match:
        raise AssertionError(f"Missing {language} fenced block in section: {heading}")
    return match.group("block")


def normalized(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def assert_contains_all(test_case: unittest.TestCase, text: str, expected: list[str]) -> None:
    normalized_text = normalized(text)
    for phrase in expected:
        with test_case.subTest(phrase=phrase):
            test_case.assertIn(normalized(phrase), normalized_text)


class Pr401UiActionMenuContractEvidenceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.evidence = EVIDENCE_PATH.read_text(encoding="utf-8")
        cls.sections = markdown_sections(cls.evidence)
        cls.index = INDEX_PATH.read_text(encoding="utf-8")
        cls.scenario = SCENARIO_PATH.read_text(encoding="utf-8")
        cls.schema_contract = SCHEMA_CONTRACT_PATH.read_text(encoding="utf-8")

    def test_handoff_keeps_source_contract_head_separate_from_documentation_head(self) -> None:
        for heading in [
            "Evidence basis",
            "Readiness evidence",
            "Review evidence",
            "Finalization evidence",
            "No-op source justification",
        ]:
            with self.subTest(heading=heading):
                self.assertIn(EXPECTED_SOURCE_CONTRACT_HEAD, section(self.sections, heading))

        evidence_basis = section(self.sections, "Evidence basis")
        assert_contains_all(
            self,
            evidence_basis,
            [
                f"| Validated source-contract head | `{EXPECTED_SOURCE_CONTRACT_HEAD}` |",
                "Documentation-only commits that add or refine this handoff are not source",
                "documentation checkout HEAD differ",
                f"`git rev-parse HEAD`",
                "executable current-head checks",
                "Review/finalization handoff only; no Java, runner, schema, validator, scenario, or test change is required",
            ],
        )
        self.assertNotIn("| Reviewed head |", self.evidence)
        self.assertIn(
            f"Validated source-contract head: {EXPECTED_SOURCE_CONTRACT_HEAD}",
            fenced_block(self.sections, "Review evidence"),
        )

    def test_scope_artifacts_and_noop_source_outcome_stay_bounded(self) -> None:
        assert_contains_all(
            self,
            self.evidence,
            [
                ACCEPTED_CLAIM,
                "Window menu model registration, stable identity, and menu-bar membership lookup",
                SCENARIO_ID,
                WORKFLOW,
                AUTOMATION_MODE,
                JAVA_CONTRACT,
                "fixed, gated",
                "Maven selector, command log, or Surefire report naming `AliceMenuBarContractTest`",
                "`command.log` is accepted only for an intentionally gated runner execution outside this no-wrapper recovery",
                "without a timeout-wrapper execution",
                "Set to `1` only when intentionally executing the gated runner path",
                "not required for the direct-Maven/no-wrapper recovery path",
            ],
        )
        self.assertNotIn("Direct Maven/Surefire output names", self.evidence)
        self.assertNotIn("Required value", section(self.sections, "Configuration"))

    def test_readiness_review_and_finalization_use_only_executable_bounded_checks(self) -> None:
        review_block = fenced_block(self.sections, "Review evidence")
        finalization = section(self.sections, "Finalization evidence")
        noop = section(self.sections, "No-op source justification")

        assert_contains_all(
            self,
            review_block,
            [
                "Source blocker: none found",
                "Source outcome: no source change required for PR #401 recovery",
                "Handoff outcome: review/finalization handoff only",
            ],
        )
        assert_contains_all(
            self,
            finalization,
            [
                f"`wave6-ui-action-menu-contract-1778302300` at `{EXPECTED_SOURCE_CONTRACT_HEAD}`",
                "no manual merge",
                "bounded menu/action contract handoff only",
                "does not prove full UI automation",
                "does not prove full",
                "desktop Save behavior",
                "full desktop Save completion",
            ],
        )
        assert_contains_all(
            self,
            noop,
            [
                "No source modification is required for this recovery",
                "No-op justification:",
                "at validated source-contract head",
                "no Java, runner, schema, validator, scenario, or test change required",
            ],
        )
        assert_contains_all(self, finalization, RECOVERY_CHECKS)

    def test_blockers_and_non_claims_are_explicitly_non_blocking(self) -> None:
        blockers = section(self.sections, "Blocker register")
        non_claims = section(self.sections, "Non-claims")

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

        assert_contains_all(self, non_claims, NON_CLAIMS)
        for pattern in [
            r"full UI automation (?:passed|proven|complete|established)",
            r"visible rendering correctness (?:passed|proven|complete|established)",
            r"grading correctness (?:passed|proven|complete|established)",
            r"lesson completion correctness (?:passed|proven|complete|established)",
        ]:
            with self.subTest(pattern=pattern):
                self.assertIsNone(re.search(pattern, self.evidence, flags=re.IGNORECASE))

    def test_index_and_existing_wiring_reference_the_same_contract_surfaces(self) -> None:
        assert_contains_all(
            self,
            "\n".join([self.index, self.scenario, self.schema_contract]),
            [
                "./reference/pr401-ui-action-menu-contract-evidence.md",
                SCENARIO_ID,
                f"workflow: {WORKFLOW}",
                f"automationMode: {AUTOMATION_MODE}",
                f"-Dtest={JAVA_CONTRACT}",
                f'"-Dtest={JAVA_CONTRACT}"',
            ],
        )


if __name__ == "__main__":
    unittest.main()
