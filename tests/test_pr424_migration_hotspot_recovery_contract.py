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
# Allowlist, not exact manifest — a diff may touch a subset of these files.
EXPECTED_PR_SCOPE = {
    "core/story-api-migration/src/test/java/org/lgna/project/migration/ProjectMigrationManagerTest.java",
    "docs/concepts/migration-hotspot-characterization.md",
    "docs/howto/characterize-project-migration-manager.md",
    "docs/index.md",
    "docs/reference/project-migration-manager-characterization.md",
    "docs/tutorials/project-migration-manager-characterization.md",
    "pyproject.toml",
    "qa/outside-in/alice-desktop/runners/run-scenario.sh",
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/scenarios/migration-hotspot-characterization-smoke.yaml",
    "qa/outside-in/alice-desktop/schema/scenario.schema.json",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
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
            "0.13.17",
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
        # All 18 @Test methods — original 12 characterization + 6 edge-case tests.
        # Update this list whenever a test method is added or renamed.
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
            "textMigrationOfEmptyStringIsNoOp",
            "textMigrationAtCurrentVersionReturnsInputUnchanged",
            "migrationListsAreNonEmpty",
            "textMigrationIsStableWhenReappliedFromResultVersion",
            "textMigrationResultVersionNeverExceedsCurrentVersion",
            "astMigrationResultVersionNeverExceedsCurrentVersion",
        ]

        for method in expected_methods:
            with self.subTest(method=method):
                self.assertIn(f"void {method}(", source)

        # Guard against silent test removal: count must match the inventory
        actual_count = source.count("@Test")
        self.assertEqual(
            len(expected_methods),
            actual_count,
            f"Expected {len(expected_methods)} @Test methods but found {actual_count}. "
            "Update expected_methods when adding or removing tests.",
        )

    def test_documentation_triad_files_exist_and_have_minimum_content(self) -> None:
        """Each scoped doc file must exist and contain real content, not stubs."""
        for path in SCOPED_DOCS:
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                self.assertTrue(path.exists(), f"{path.name} must exist")
                text = path.read_text(encoding="utf-8")
                self.assertGreater(
                    len(text), 200,
                    f"{path.name} must have substantive content (>200 bytes), not be a stub",
                )
                self.assertTrue(
                    text.startswith("# "),
                    f"{path.name} must start with a Markdown heading",
                )

    def test_documentation_cross_references_follow_diataxis_pattern(self) -> None:
        """Reference, howto, and tutorial docs must cross-link to each other."""
        ref = (REPO_ROOT / "docs" / "reference" / "project-migration-manager-characterization.md")
        howto = (REPO_ROOT / "docs" / "howto" / "characterize-project-migration-manager.md")
        tutorial = (REPO_ROOT / "docs" / "tutorials" / "project-migration-manager-characterization.md")
        concept = (REPO_ROOT / "docs" / "concepts" / "migration-hotspot-characterization.md")

        ref_text = ref.read_text(encoding="utf-8")
        howto_text = howto.read_text(encoding="utf-8")
        tutorial_text = tutorial.read_text(encoding="utf-8")

        with self.subTest(doc="reference links to howto"):
            self.assertIn("characterize-project-migration-manager.md", ref_text)
        with self.subTest(doc="reference links to tutorial"):
            self.assertIn("project-migration-manager-characterization.md", ref_text)
        with self.subTest(doc="reference links to concept"):
            self.assertIn("migration-hotspot-characterization.md", ref_text)
        with self.subTest(doc="howto links to reference"):
            self.assertIn("project-migration-manager-characterization.md", howto_text)
        with self.subTest(doc="tutorial links to concept"):
            self.assertIn("migration-hotspot-characterization.md", tutorial_text)

    def test_java_test_package_matches_production_package(self) -> None:
        """The test must live in org.lgna.project.migration, not org.alice.stageide.migration."""
        source = MIGRATION_TEST.read_text(encoding="utf-8")
        self.assertIn("package org.lgna.project.migration;", source)
        self.assertNotIn("org.alice.stageide.migration", source)

    def test_index_migration_section_links_all_four_diataxis_types(self) -> None:
        """The migration section in index.md must have concept, reference, howto, and tutorial links."""
        index = DOCS_INDEX.read_text(encoding="utf-8")
        diataxis_prefixes = {
            "concept": "./concepts/migration-hotspot-characterization.md",
            "reference": "./reference/project-migration-manager-characterization.md",
            "howto": "./howto/characterize-project-migration-manager.md",
            "tutorial": "./tutorials/project-migration-manager-characterization.md",
        }
        for doc_type, link in diataxis_prefixes.items():
            with self.subTest(doc_type=doc_type):
                self.assertIn(link, index, f"index.md must link to the {doc_type} doc")


if __name__ == "__main__":
    unittest.main()
