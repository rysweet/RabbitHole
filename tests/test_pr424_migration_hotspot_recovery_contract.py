import re
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
PYPROJECT = REPO_ROOT / "pyproject.toml"
DOCS_INDEX = REPO_ROOT / "docs" / "index.md"
MIGRATION_TEST = (
    REPO_ROOT
    / "core"
    / "story-api-migration"
    / "src"
    / "test"
    / "java"
    / "org"
    / "lgna"
    / "project"
    / "migration"
    / "ProjectMigrationManagerTest.java"
)
SCOPED_DOCS = [
    DOCS_INDEX,
    REPO_ROOT / "docs" / "concepts" / "migration-hotspot-characterization.md",
    REPO_ROOT / "docs" / "howto" / "characterize-project-migration-manager.md",
    REPO_ROOT / "docs" / "reference" / "project-migration-manager-characterization.md",
    REPO_ROOT / "docs" / "tutorials" / "project-migration-manager-characterization.md",
]
EXPECTED_PR_SCOPE = {
    "core/story-api-migration/src/test/java/org/lgna/project/migration/ProjectMigrationManagerTest.java",
    "docs/concepts/migration-hotspot-characterization.md",
    "docs/howto/characterize-project-migration-manager.md",
    "docs/index.md",
    "docs/reference/project-migration-manager-characterization.md",
    "docs/tutorials/project-migration-manager-characterization.md",
    "pyproject.toml",
    "tests/test_pr424_migration_hotspot_recovery_contract.py",
}
EXPECTED_DOC_LINKS = [
    "./concepts/migration-hotspot-characterization.md",
    "./reference/project-migration-manager-characterization.md",
    "./howto/characterize-project-migration-manager.md",
    "./tutorials/project-migration-manager-characterization.md",
]


def git_output(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def origin_develop_ref() -> str:
    try:
        return git_output("rev-parse", "--verify", "origin/develop")
    except subprocess.CalledProcessError as exc:
        raise unittest.SkipTest(
            f"PR #424 recovery contract requires origin/develop: {exc.stderr.strip()}"
        ) from exc


class Pr424MigrationHotspotRecoveryContractTest(unittest.TestCase):
    def test_pr_branch_contains_current_origin_develop_before_review(self) -> None:
        develop = origin_develop_ref()
        result = subprocess.run(
            ["git", "merge-base", "--is-ancestor", develop, "HEAD"],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
        )

        self.assertEqual(
            0,
            result.returncode,
            "PR #424 must be reconciled by merging origin/develop into the PR branch, "
            "not by merging the PR into develop.",
        )

    def test_pyproject_preserves_current_develop_package_version(self) -> None:
        text = PYPROJECT.read_text(encoding="utf-8")

        version_match = re.search(r'^version = "(?P<version>[^"]+)"$', text, flags=re.MULTILINE)

        self.assertIsNotNone(version_match, "pyproject.toml must declare a project version.")
        assert version_match is not None
        self.assertEqual(
            "0.11.0",
            version_match.group("version"),
            "PR #424 recovery must keep origin/develop package metadata unless migration scope requires otherwise.",
        )

    def test_pr_diff_stays_within_protected_migration_hotspot_scope(self) -> None:
        origin_develop_ref()
        changed_files = set(
            git_output("diff", "--name-only", "origin/develop...HEAD").splitlines()
        )

        self.assertEqual(
            set(),
            changed_files - EXPECTED_PR_SCOPE,
            "PR #424 recovery must not add unrelated cleanup, refactors, CI no-op changes, or behavior changes.",
        )

    def test_docs_index_preserves_migration_characterization_links(self) -> None:
        index = DOCS_INDEX.read_text(encoding="utf-8")

        for link in EXPECTED_DOC_LINKS:
            with self.subTest(link=link):
                self.assertEqual(
                    1,
                    index.count(link),
                    "docs/index.md must preserve each PR #424 migration characterization link exactly once.",
                )

    def test_scoped_recovery_files_have_no_conflict_markers(self) -> None:
        blocked_patterns = [
            "<" * 7,
            "=" * 7,
            ">" * 7,
        ]
        files = [PYPROJECT, MIGRATION_TEST, *SCOPED_DOCS]

        for path in files:
            text = path.read_text(encoding="utf-8")
            for pattern in blocked_patterns:
                with self.subTest(path=path.relative_to(REPO_ROOT), pattern=pattern):
                    self.assertNotIn(pattern, text)

    def test_project_migration_manager_characterization_tests_remain_in_scope(self) -> None:
        source = MIGRATION_TEST.read_text(encoding="utf-8")
        expected_methods = [
            "textMigrationResultVersionsAreValidRoundTrippableAndIncreasing",
            "astMigrationResultVersionsAreValidRoundTrippableAndIncreasing",
            "migrationIsApplicableOnlyBeforeItsResultVersion",
            "textMigrationRewritesKnownLegacyStoryAndResourceNames",
            "textMigrationCascadesLegacyDresserThroughIntermediateResourceNames",
            "textMigrationCascadesLegacyDresserFieldThroughIntermediateResourceNames",
            "textMigrationStartingAfterDresserPackageMoveStillAppliesLaterConsolidations",
            "textMigrationDoesNotRewriteWhenVersionIsAlreadyAtThreshold",
            "textMigrationRewritesLegacyJointFieldsAndAccessors",
            "textMigrationRewritesVersion3_2_110ResourceFields",
            "textMigrationCharacterizesVersion3_2_111BonePileBoundary",
            "managerReportsNoPendingMigrationsAtCurrentVersion",
        ]

        for method in expected_methods:
            with self.subTest(method=method):
                self.assertIn(f"void {method}(", source)


if __name__ == "__main__":
    unittest.main()
