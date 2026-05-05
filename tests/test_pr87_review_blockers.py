import re
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CANONICAL_METHOD = "savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported"
CANONICAL_REFERENCE = f"IoUtilitiesTest.{CANONICAL_METHOD}"
PLAIN_WORKFLOW = (
    "saving, reopening, editing, saving again, reopening again, and exporting"
)

IO_UTILITIES_TEST = (
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
    / "IoUtilitiesTest.java"
)

USER_FACING_FILES = [
    REPO_ROOT / "docs" / "howto" / "characterize-project-save-export-operations.md",
    REPO_ROOT / "docs" / "reference" / "decode-coverage-characterization.md",
    REPO_ROOT / "docs" / "tutorials" / "trace-save-load-recovery.md",
    REPO_ROOT / "docs" / "howto" / "alice-desktop-outside-in-qa.md",
    REPO_ROOT / "docs" / "reference" / "alice-desktop-outside-in-qa.md",
    REPO_ROOT / "docs" / "howto" / "characterize-headless-safe-desktop-actions.md",
    REPO_ROOT / "docs" / "reference" / "project-save-export-operations.md",
    REPO_ROOT / "docs" / "reference" / "coverage-reporting.md",
    REPO_ROOT
    / "docs"
    / "reference"
    / "headless-safe-desktop-action-characterization.md",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "project-io-smoke.yaml",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "open-load-save.yaml",
]

OBSOLETE_TEST_REFERENCES = [
    "reopenedEditedProjectCanBeSavedAndExported",
    "reopenedEditedProjectCanBeSavedAndExportedWithCurrentState",
    "savedProjectCanBeReopenedEditedAndExported",
    "savedProjectCanBeReopenedEditedAndSavedAgain",
    "saveAfterOpen",
    "saveReloadEditExport",
]

STALE_USER_FACING_PHRASES = [
    re.compile(pattern, flags=re.IGNORECASE)
    for pattern in [
        r"\bproject IO\b",
        r"save/reopen/edit/export",
        r"saving/reopening/editing/exporting",
        r"save-after-open",
        r"\breload\b",
        r"\bresave\b",
    ]
]


def git_output(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


def pr_branch_output(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise unittest.SkipTest(
            f"PR branch contract requires origin/develop: {result.stderr.strip()}"
        )
    return result.stdout


def pr_branch_commits() -> list[str]:
    commits = pr_branch_output("log", "--format=%s", "origin/develop..HEAD").splitlines()
    if not commits:
        raise unittest.SkipTest("PR branch contract is only enforced when HEAD is ahead of origin/develop.")
    return commits


class Pr87ReviewBlockerContractTest(unittest.TestCase):
    def test_io_utilities_test_declares_the_canonical_regression_method(self) -> None:
        source = IO_UTILITIES_TEST.read_text(encoding="utf-8")

        declarations = re.findall(rf"\bvoid\s+{re.escape(CANONICAL_METHOD)}\s*\(", source)

        self.assertEqual(
            [f"void {CANONICAL_METHOD}("],
            declarations,
            "IoUtilitiesTest should own exactly one canonical save/reopen/edit/export regression method.",
        )

    def test_user_facing_docs_reference_the_canonical_regression_method(self) -> None:
        for path in USER_FACING_FILES:
            text = path.read_text(encoding="utf-8")
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                if "IoUtilitiesTest" in text:
                    self.assertIn(CANONICAL_REFERENCE, text)
                for obsolete_reference in OBSOLETE_TEST_REFERENCES:
                    self.assertNotIn(obsolete_reference, text)

    def test_user_facing_docs_use_plain_project_workflow_wording(self) -> None:
        missing_plain_workflow = []
        stale_matches = []

        for path in USER_FACING_FILES:
            text = path.read_text(encoding="utf-8")
            if CANONICAL_REFERENCE in text and PLAIN_WORKFLOW not in text:
                missing_plain_workflow.append(str(path.relative_to(REPO_ROOT)))
            for pattern in STALE_USER_FACING_PHRASES:
                for match in pattern.finditer(text):
                    stale_matches.append(
                        f"{path.relative_to(REPO_ROOT)}:{text.count(chr(10), 0, match.start()) + 1}:"
                        f"{match.group(0)}"
                    )

        self.assertEqual(
            [],
            missing_plain_workflow,
            "Docs that cite the regression method should also describe the workflow in plain language.",
        )
        self.assertEqual(
            [],
            stale_matches,
            "User-facing docs should avoid internal shorthand; stable IDs such as project-io-smoke are allowed.",
        )

    def test_pr_branch_does_not_introduce_lfs_configuration_or_tracked_files(self) -> None:
        pr_branch_commits()
        changed_files = set(pr_branch_output("diff", "--name-only", "origin/develop...HEAD").splitlines())

        self.assertFalse(
            {".gitattributes", ".lfsconfig"} & changed_files,
            "PR #87 must not introduce Git LFS configuration.",
        )
        for file_name in changed_files:
            self.assertNotIn(
                ".git/lfs",
                file_name,
                "PR #87 must not introduce LFS-managed artifacts.",
            )

    def test_pr_branch_history_is_squashed_and_has_no_wip_or_checkpoint_commit(self) -> None:
        commits = pr_branch_commits()
        disallowed_subject = re.compile(r"\b(wip|checkpoint)\b", flags=re.IGNORECASE)

        self.assertEqual(
            1,
            len(commits),
            "PR #87 should be represented by one squashed, merge-ready commit.",
        )
        self.assertEqual(
            [],
            [subject for subject in commits if disallowed_subject.search(subject)],
            "PR #87 history must not contain WIP or checkpoint commits.",
        )


if __name__ == "__main__":
    unittest.main()
