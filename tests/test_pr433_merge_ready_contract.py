import re
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
PR_BRANCH = "feat/issue-411-rabbithole-wave7-legacy-fixture-roundtrip-lane-fol"
EXPECTED_PR433_CHANGED_FILES = {
    "core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java",
    "docs/howto/characterize-legacy-fixture-roundtrip-readiness.md",
    "docs/index.md",
    "docs/reference/alice-desktop-outside-in-qa.md",
    "docs/reference/legacy-fixture-roundtrip-readiness.md",
    "docs/tutorials/legacy-fixture-roundtrip-readiness.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/tests/test-scenario-validation.sh",
    "tests/test_pr433_merge_ready_contract.py",
}
REFERENCE_DOC = REPO_ROOT / "docs" / "reference" / "legacy-fixture-roundtrip-readiness.md"
HOWTO_DOC = REPO_ROOT / "docs" / "howto" / "characterize-legacy-fixture-roundtrip-readiness.md"
TUTORIAL_DOC = REPO_ROOT / "docs" / "tutorials" / "legacy-fixture-roundtrip-readiness.md"
DOCS_INDEX = REPO_ROOT / "docs" / "index.md"
ARCHIVE_SCENARIO = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "archive-fixture-smoke.yaml"
)
VALIDATOR = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "validate-scenarios.sh"
RUNNER = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "run-scenario.sh"
SCHEMA = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "schema" / "scenario.schema.json"
SCHEMA_CONTRACT = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-schema-contract.sh"
)
CHARACTERIZATION_TEST = (
    REPO_ROOT
    / "core"
    / "story-api-migration"
    / "src"
    / "test"
    / "java"
    / "org"
    / "lgna"
    / "project"
    / "io"
    / "HistoricalArchiveRoundTripCharacterizationTest.java"
)

FOCUSED_MAVEN_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip "
    "-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false "
    "-pl core/story-api-migration -am "
    "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest test"
)
FINAL_GITHUB_READINESS_CHECKS = (
    "headRefOid",
    "git rev-parse HEAD",
    "open and not draft",
    "mergeStateStatus",
    "CLEAN",
    "statusCheckRollup",
    "reviewDecision",
    "An empty value is not approval.",
)
EVIDENCE_AREAS = (
    "QA/scenario",
    "Docs",
    "Quality audit",
    "CI",
    "Focused validation",
    "Focused scope",
)
NON_CLAIMS = (
    "full historical archive migration",
    "full Tweedle decode",
    "full player decode",
    "arbitrary user archive support",
    "desktop UI behavior",
)
QA_WIRING_FILES = (ARCHIVE_SCENARIO, VALIDATOR, RUNNER, SCHEMA, SCHEMA_CONTRACT)
QA_WORKFLOW_FILES = (ARCHIVE_SCENARIO, VALIDATOR, SCHEMA)
_current_branch = None


def git_output(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=check,
        capture_output=True,
        text=True,
    )


def current_branch() -> str:
    global _current_branch
    if _current_branch is None:
        _current_branch = git_output("branch", "--show-current").stdout.strip()
    return _current_branch


class Pr433MergeReadyContractTest(unittest.TestCase):
    def test_pr_branch_diff_stays_inside_legacy_fixture_round_trip_scope(self) -> None:
        if current_branch() != PR_BRANCH:
            raise unittest.SkipTest(f"PR #433 diff-scope contract only runs on {PR_BRANCH}")

        changed_files = set(
            git_output("diff", "--name-only", "origin/develop...HEAD").stdout.splitlines()
        )

        self.assertEqual(
            EXPECTED_PR433_CHANGED_FILES,
            changed_files,
            "PR #433 must stay limited to the legacy fixture round-trip test, docs, QA scenario, and contract surfaces.",
        )
        archive_fixtures = {
            path for path in changed_files if path.endswith((".a3p", ".a3w", ".a3c"))
        }
        self.assertEqual(
            set(),
            archive_fixtures,
            "PR #433 must not add checked-in binary Alice archive fixtures.",
        )

    def test_pr_head_merges_cleanly_into_current_develop(self) -> None:
        if current_branch() != PR_BRANCH:
            raise unittest.SkipTest(f"PR #433 mergeability contract only runs on {PR_BRANCH}")

        result = git_output("merge-tree", "--write-tree", "origin/develop", "HEAD", check=False)

        self.assertEqual(
            0,
            result.returncode,
            "PR #433 must be cleanly mergeable into current origin/develop before merge-ready evidence is refreshed.",
        )

    def test_reference_doc_defines_the_canonical_merge_ready_evidence_contract(self) -> None:
        text = REFERENCE_DOC.read_text(encoding="utf-8")
        normalized = re.sub(r"\s+", " ", text)

        self.assertIn("## Merge-ready evidence contract", text)
        self.assertIn(
            "Do not rebase, force-push, or merge the PR branch into the target branch manually.",
            normalized,
        )
        self.assertIn(
            "Resolve conflicts only in the bounded legacy fixture round-trip lane",
            normalized,
        )
        self.assertIn(
            "A clean focused current-head review is not merge-ready evidence after the target branch changes.",
            normalized,
        )
        self.assertIn("Do not wrap this focused gate in an external timeout helper", normalized)
        self.assertIn("timeoutSeconds", text)
        for check in FINAL_GITHUB_READINESS_CHECKS:
            self.assertIn(check, text)
        self.assertIn(FOCUSED_MAVEN_COMMAND, normalized)
        for area in EVIDENCE_AREAS:
            self.assertRegex(
                text,
                rf"\|\s*{re.escape(area)}\s*\|",
                f"merge-ready evidence contract must include {area}",
            )
        for non_claim in NON_CLAIMS:
            self.assertIn(non_claim, text)

    def test_howto_and_tutorial_defer_to_the_reference_evidence_contract(self) -> None:
        for path in (HOWTO_DOC, TUTORIAL_DOC):
            text = path.read_text(encoding="utf-8")
            normalized = re.sub(r"\s+", " ", text)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                self.assertIn(
                    "../reference/legacy-fixture-roundtrip-readiness.md#merge-ready-evidence-contract",
                    text,
                )
                self.assertIn("Do not wrap the command in an external timeout helper", normalized)
                self.assertIn("timeoutSeconds", text)
                self.assertNotIn("merge or rebase", normalized.lower())
                self.assertNotIn("merging or rebasing", normalized.lower())

    def test_docs_index_points_reviewers_to_merge_ready_pr_evidence(self) -> None:
        text = DOCS_INDEX.read_text(encoding="utf-8")

        self.assertIn("merge-ready evidence contract", text)
        self.assertIn("PR evidence", text)
        self.assertIn("merge-ready PR wording", text)

    def test_archive_fixture_lane_keeps_scenario_wiring_and_bounded_claims(self) -> None:
        qa_text_by_path = {
            path: path.read_text(encoding="utf-8")
            for path in set(QA_WIRING_FILES + QA_WORKFLOW_FILES)
        }
        scenario_text = qa_text_by_path[ARCHIVE_SCENARIO]
        characterization_text = CHARACTERIZATION_TEST.read_text(encoding="utf-8")

        for path in QA_WIRING_FILES:
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                text = qa_text_by_path[path]
                self.assertIn("HistoricalArchiveRoundTripCharacterizationTest", text)
        for path in QA_WORKFLOW_FILES:
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                text = qa_text_by_path[path]
                self.assertIn("archive-fixture-smoke", text)

        for required in (
            "generated LFS-independent .a3p, .a3c, and .a3w archive boundaries",
            "generated legacy fixture boundary smoke command exits successfully",
            "fail explicitly",
            "Generated XML fallback .a3p fixtures write, read, and preserve resources",
            "Generated XML fallback .a3c fixtures write, read, and preserve type resources",
            "Simple supported .a3w fixtures export, read, re-export, and reread",
        ):
            self.assertIn(required, scenario_text)
        self.assertIn("archive-io", scenario_text)
        self.assertIn("assertThrows", characterization_text)
        for forbidden in (
            "full historical archive migration",
            "full Tweedle language decode",
            "arbitrary user archive support",
        ):
            self.assertNotIn(forbidden, scenario_text)


if __name__ == "__main__":
    unittest.main()
