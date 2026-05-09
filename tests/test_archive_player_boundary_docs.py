import re
import unittest
from pathlib import Path

import alice_qa_amplihack


REPO_ROOT = Path(__file__).resolve().parents[1]
ARCHIVE_PLAYER_DOCS = [
    REPO_ROOT / "docs/reference/archive-player-boundary.md",
    REPO_ROOT / "docs/howto/validate-archive-player-boundary.md",
    REPO_ROOT / "docs/tutorials/archive-player-boundary-characterization.md",
]
ARCHIVE_FIXTURE_SCENARIO = (
    REPO_ROOT
    / "qa"
    / "outside-in"
    / "alice-desktop"
    / "scenarios"
    / "archive-fixture-smoke.yaml"
)
REFERENCE_DOC = REPO_ROOT / "docs/reference/archive-player-boundary.md"
EXPECTED_DIRECT_MAVEN_FLAGS = [
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-pl core/story-api-migration -am",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest",
]
DOC_CONTRACT_TEST_PATH = "tests/test_archive_player_boundary_docs.py"


def read_doc(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def scenario_argv() -> list[str]:
    scenario = ARCHIVE_FIXTURE_SCENARIO.read_text(encoding="utf-8")
    match = re.search(r"(?m)^  argv:\n(?P<body>(?:    - .+\n)+)", scenario)
    if match is None:
        raise AssertionError("archive fixture smoke scenario must declare automation.argv")
    return [
        line.strip().split("- ", 1)[1]
        for line in match.group("body").splitlines()
        if line.strip()
    ]


class ArchivePlayerBoundaryDocsContractTest(unittest.TestCase):
    def test_archive_player_docs_use_branch_placeholder_with_review_instruction(self) -> None:
        for path in ARCHIVE_PLAYER_DOCS:
            with self.subTest(doc=path.relative_to(REPO_ROOT)):
                doc = read_doc(path)

                self.assertIn(
                    "uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch>",
                    doc,
                )
                self.assertIn("Replace `<branch>` with the branch under review", doc)
                self.assertNotIn("RabbitHole.git@feat/", doc)
                self.assertNotIn("RabbitHole.git@main", doc)
                self.assertNotIn("RabbitHole.git@develop", doc)

    def test_direct_maven_examples_match_bounded_wrapper_and_scenario_flags(self) -> None:
        wrapper_command = " ".join(alice_qa_amplihack.ARCHIVE_PLAYER_BOUNDARY_COMMAND)
        scenario_command = " ".join(scenario_argv())

        for required_flag in EXPECTED_DIRECT_MAVEN_FLAGS:
            with self.subTest(source="wrapper", flag=required_flag):
                self.assertIn(required_flag, wrapper_command)
            with self.subTest(source="scenario", flag=required_flag):
                self.assertIn(required_flag, scenario_command)

        for path in ARCHIVE_PLAYER_DOCS:
            with self.subTest(doc=path.relative_to(REPO_ROOT)):
                doc = read_doc(path)

                self.assertIn(
                    "NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip",
                    doc,
                )
                for required_flag in EXPECTED_DIRECT_MAVEN_FLAGS:
                    self.assertIn(required_flag, doc)
                self.assertNotIn("mvn -pl core/story-api-migration -am", doc)

    def test_archive_player_docs_preserve_nonclaim_boundary(self) -> None:
        forbidden_claims = [
            "complete player support",
            "full player support",
            "visible rendering proof",
            "Save completion proof",
            "grading proof",
            "Sims validation",
            "lesson completion proof",
        ]

        for path in ARCHIVE_PLAYER_DOCS:
            with self.subTest(doc=path.relative_to(REPO_ROOT)):
                doc = read_doc(path)

                self.assertIn("archive/player", doc)
                self.assertRegex(doc, r"fail[- ]closed")
                self.assertIn("HistoricalArchiveRoundTripCharacterizationTest", doc)
                for forbidden_claim in forbidden_claims:
                    self.assertNotIn(f"proves {forbidden_claim}", doc)

    def test_reference_doc_names_docs_contract_guard(self) -> None:
        doc = read_doc(REFERENCE_DOC)

        self.assertIn(DOC_CONTRACT_TEST_PATH, doc)
        self.assertIn("wrapper, scenario, and direct\nMaven evidence stay aligned", doc)


if __name__ == "__main__":
    unittest.main()
