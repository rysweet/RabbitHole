import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
EVIDENCE_PATH = REPO_ROOT / "docs" / "reference" / "pr401-ui-action-menu-contract-evidence.md"
WINDOW_CONTRACT_PATH = REPO_ROOT / "docs" / "reference" / "window-menu-action-contract.md"
INDEX_PATH = REPO_ROOT / "docs" / "index.md"
SCENARIO_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "menu-action-smoke.yaml"
SCHEMA_CONTRACT_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-schema-contract.sh"

SCENARIO_ID = "alice-desktop-menu-action-smoke"
WORKFLOW = "menu-action-smoke"
AUTOMATION_MODE = "gated-command-smoke"
JAVA_CONTRACT = "org.alice.ide.croquet.models.AliceMenuBarContractTest"
TRANSIENT_SHA_PATTERN = re.compile(r"\b[0-9a-f]{40}\b")
SECTION_HEADING_PATTERN = re.compile(r"^## (?P<heading>.+)\n", flags=re.MULTILINE)
ACCEPTED_CLAIM = (
    "WindowMenuModel is registered in the Alice desktop menu-bar model, keeps the\n"
    "expected stable identity, and is reachable through menu-bar membership lookup."
)
RECOVERY_CHECKS = [
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh",
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
    "Grading, learner assessment, rubric correctness, or creative assessment",
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
        cls.window_contract = WINDOW_CONTRACT_PATH.read_text(encoding="utf-8")
        cls.index = INDEX_PATH.read_text(encoding="utf-8")
        cls.scenario = SCENARIO_PATH.read_text(encoding="utf-8")
        cls.schema_contract = SCHEMA_CONTRACT_PATH.read_text(encoding="utf-8")

    def test_handoff_is_durable_contract_not_point_in_time_status_report(self) -> None:
        for text_name, text in [
            ("evidence", self.evidence),
            ("window contract", self.window_contract),
        ]:
            with self.subTest(text=text_name):
                self.assertIsNone(TRANSIENT_SHA_PATTERN.search(text))

        evidence_basis = section(self.sections, "Evidence basis")
        pr_description = fenced_block(self.sections, "PR description evidence")
        assert_contains_all(
            self,
            "\n".join([self.evidence, evidence_basis, pr_description]),
            [
                "without embedding a point-in-time PR-head SHA",
                "Exact command output, current-head SHAs, check conclusions, and remaining blockers belong in the pull request body",
                "Record the exact PR `headRefOid` in the PR body and verify local `HEAD` matches it; do not commit point-in-time SHAs to this reference.",
                "Current PR head: <current-pr-head-sha>",
                "no committed exact-head SHA",
            ],
        )
        self.assertNotIn("| Current PR head |", self.evidence)
        self.assertNotIn("| Validated source-contract head |", self.evidence)
        self.assertNotIn("| Reviewed head |", self.evidence)

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
                "Same-head Maven/Surefire success naming `AliceMenuBarContractTest`",
                "`status.txt` and `command.log` prove gated runner behavior only",
                "without a timeout-wrapper execution",
                "Set to `1` only when intentionally executing the gated runner path",
                "not required for the direct-Maven/no-wrapper recovery path",
            ],
        )
        self.assertNotIn("Direct Maven/Surefire output names", self.evidence)
        self.assertNotIn("Required value", section(self.sections, "Configuration"))

    def test_readiness_requires_same_head_maven_and_runner_gate_is_not_enough(self) -> None:
        evidence_basis = section(self.sections, "Evidence basis")
        qa_evidence = section(self.sections, "QA and scenario evidence")
        window_evidence = section(markdown_sections(self.window_contract), "Evidence")

        assert_contains_all(
            self,
            "\n".join([evidence_basis, qa_evidence, window_evidence]),
            [
                "Same-head Maven/Surefire success naming `AliceMenuBarContractTest`",
                "`status.txt` and `command.log` prove gated runner behavior only",
                "pair them with same-head Maven/Surefire success before claiming readiness",
                "they do not prove readiness unless paired with successful same-head Maven/Surefire evidence",
                "`gated-not-run`",
                "The fixed Maven argv for `AliceMenuBarContractTest`",
            ],
        )

    def test_merge_gate_diff_scope_and_finalization_inputs_are_complete(self) -> None:
        merge_gate = section(self.sections, "Merge-ready gate")
        diff_scope = section(self.sections, "Diff scope")
        noop = section(self.sections, "No-op source justification")

        assert_contains_all(
            self,
            merge_gate,
            [
                "same current PR head",
                "Green checks and workflow completion are necessary but not sufficient",
                "directly related test-package metadata",
                "PR body contains current-head evidence",
                "Any missing gate is recorded as `NOT_MERGE_READY`",
            ],
        )
        assert_contains_all(
            self,
            diff_scope,
            [
                "Evidence tests that protect the documentation contract",
                "Directly related test-package metadata, such as the `pyproject.toml` version",
                "unrelated packaging metadata",
            ],
        )
        assert_contains_all(
            self,
            noop,
            [
                "No Java, runner, schema, validator, scenario, or test modification is required",
                "for the PR head under review",
                "Missing evidence is a `NOT_MERGE_READY` blocker, not a reason to infer success",
            ],
        )
        assert_contains_all(self, self.evidence, RECOVERY_CHECKS)

    def test_blockers_and_non_claims_are_explicit(self) -> None:
        blockers = section(self.sections, "NOT_MERGE_READY blockers")
        non_claims = section(self.sections, "Non-claims")

        for blocker in [
            "Local HEAD differs from `gh pr view 401` headRefOid",
            "GitHub Actions are pending, failing, or tied to another SHA",
            "Focused Maven contract was not run and no accepted same-head artifact exists",
            "Scenario/schema/workflow/gated-command checks were not run",
            "Docs contain stale SHA, stale claims, or overclaims",
            "PR body lacks current-head evidence",
            "Diff includes unrelated behavior",
        ]:
            with self.subTest(blocker=blocker):
                self.assertIn(blocker, blockers)

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
                "./reference/window-menu-action-contract.md",
                SCENARIO_ID,
                f"workflow: {WORKFLOW}",
                f"automationMode: {AUTOMATION_MODE}",
                f"-Dtest={JAVA_CONTRACT}",
                f'"-Dtest={JAVA_CONTRACT}"',
            ],
        )


if __name__ == "__main__":
    unittest.main()
