"""TDD tests for issue #828: remove point-in-time documentation artifacts.

These tests define the contract for what the docs/ tree must look like
after cleanup.  They are written *before* the final deletions so they
start RED and turn GREEN once the implementation is complete.
"""

import itertools
import re
import unittest
from functools import lru_cache
from pathlib import Path

import yaml

REPO_ROOT = Path(__file__).resolve().parents[1]
DOCS_DIR = REPO_ROOT / "docs"
MKDOCS_YML = REPO_ROOT / "mkdocs.yml"

# ── directories that must NOT exist after cleanup ─────────────────────
DELETED_DIRS = [
    DOCS_DIR / "howto",
    DOCS_DIR / "tutorials",
    DOCS_DIR / "reference",
    DOCS_DIR / "atlas" / "bug-reports",
    DOCS_DIR / "testing",           # testing *directory* (testing.md is kept)
]

# ── loose files that must NOT exist after cleanup ─────────────────────
DELETED_LOOSE_FILES = [
    DOCS_DIR / "evidence-json-writer-saveproof-delegate.md",
    DOCS_DIR / "test-coverage-cascade-fillerinner-editors.md",
]

# ── nav sections that must NOT appear in mkdocs.yml ───────────────────
DELETED_NAV_SECTIONS = [
    "How-to Guides",
    "Reference",
    "Tutorials",
    "Testing notes",
    "Additional docs",
]

# ── nav entries that MUST survive ─────────────────────────────────────
EXPECTED_NAV_SECTIONS = [
    "Home",
    "Start here",
    "Concepts",
    "Architecture Atlas",
]

# ── files that MUST exist (kept documentation) ────────────────────────
KEPT_FILES = [
    DOCS_DIR / "index.md",
    DOCS_DIR / "getting-started.md",
    DOCS_DIR / "architecture.md",
    DOCS_DIR / "testing.md",
    DOCS_DIR / "contributing.md",
    DOCS_DIR / "concepts" / "formal-spec-lane.md",
    DOCS_DIR / "concepts" / "migration-hotspot-characterization.md",
    DOCS_DIR / "atlas" / "index.md",
]

# Regex for PR / issue references that signal point-in-time content
POINT_IN_TIME_RE = re.compile(
    r"(?:"
    r"PR\s*#\d{2,}"            # PR #123
    r"|pull/\d{2,}"            # pull/123
    r"|issues/\d{2,}"          # issues/123
    r"|sprint[\s-]*\d"         # sprint-1, sprint 2
    r"|extraction[\s-]*trace"  # extraction trace
    r"|proof[\s-]*of[\s-]*extraction"
    r")",
    re.IGNORECASE,
)


class _MkdocsLoader(yaml.SafeLoader):
    """SafeLoader subclass that handles !!python/name tags in mkdocs.yml."""

_MkdocsLoader.add_multi_constructor(
    "tag:yaml.org,2002:python/name:",
    lambda loader, suffix, node: f"!!python/name:{suffix}",
)


@lru_cache(maxsize=1)
def _load_mkdocs() -> dict:
    return yaml.load(MKDOCS_YML.read_text(encoding="utf-8"), Loader=_MkdocsLoader)


def _nav_section_titles(nav: list) -> list[str]:
    """Extract top-level nav section titles from mkdocs nav list."""
    titles = []
    for entry in nav:
        if isinstance(entry, dict):
            titles.extend(entry.keys())
        elif isinstance(entry, str):
            titles.append(entry)
    return titles


def _collect_nav_file_refs(nav, refs=None) -> list[str]:
    """Recursively collect all file path strings from the nav tree."""
    if refs is None:
        refs = []
    if isinstance(nav, str):
        refs.append(nav)
    elif isinstance(nav, dict):
        for v in nav.values():
            _collect_nav_file_refs(v, refs)
    elif isinstance(nav, list):
        for item in nav:
            _collect_nav_file_refs(item, refs)
    return refs


# ══════════════════════════════════════════════════════════════════════
# Test suite
# ══════════════════════════════════════════════════════════════════════


class TestDeletedDirectories(unittest.TestCase):
    """Directories containing point-in-time artifacts must be gone."""

    def test_deleted_dirs_do_not_exist(self):
        for d in DELETED_DIRS:
            with self.subTest(directory=str(d.relative_to(REPO_ROOT))):
                self.assertFalse(
                    d.is_dir(),
                    f"{d.relative_to(REPO_ROOT)} still exists as a directory",
                )

    def test_deleted_dirs_contain_no_tracked_files(self):
        """Even if empty dirs linger, no files should remain."""
        for d in DELETED_DIRS:
            with self.subTest(directory=str(d.relative_to(REPO_ROOT))):
                if d.is_dir():
                    files = list(d.rglob("*"))
                    real_files = [f for f in files if f.is_file()]
                    self.assertEqual(
                        real_files,
                        [],
                        f"Files remain in {d.relative_to(REPO_ROOT)}: {real_files}",
                    )


class TestDeletedLooseFiles(unittest.TestCase):
    """Individual point-in-time markdown files must be gone."""

    def test_loose_pit_files_do_not_exist(self):
        for f in DELETED_LOOSE_FILES:
            with self.subTest(file=str(f.relative_to(REPO_ROOT))):
                self.assertFalse(
                    f.exists(),
                    f"{f.relative_to(REPO_ROOT)} still exists",
                )


class TestKeptFilesIntact(unittest.TestCase):
    """Core documentation files must survive the cleanup."""

    def test_kept_files_exist(self):
        for f in KEPT_FILES:
            with self.subTest(file=str(f.relative_to(REPO_ROOT))):
                self.assertTrue(
                    f.is_file(),
                    f"{f.relative_to(REPO_ROOT)} is missing — cleanup over-deleted",
                )

    def test_atlas_subdirectories_intact(self):
        """Each atlas layer dir must have at least a README and one diagram."""
        atlas_layers = [
            "repo-surface",
            "ast-lsp-bindings",
            "compile-deps",
            "runtime-topology",
            "api-contracts",
            "data-flow",
            "service-components",
            "user-journeys",
        ]
        for layer in atlas_layers:
            layer_dir = DOCS_DIR / "atlas" / layer
            with self.subTest(layer=layer):
                self.assertTrue(
                    layer_dir.is_dir(),
                    f"Atlas layer dir {layer} is missing",
                )
                self.assertTrue(
                    (layer_dir / "README.md").is_file(),
                    f"Atlas layer {layer}/README.md is missing",
                )
                has_diagram = any(
                    itertools.chain(layer_dir.glob("*.dot"), layer_dir.glob("*.mmd"))
                )
                self.assertTrue(
                    has_diagram,
                    f"Atlas layer {layer} has no diagram files",
                )


class TestMkdocsNavCleaned(unittest.TestCase):
    """mkdocs.yml nav must not reference deleted sections."""

    @classmethod
    def setUpClass(cls):
        cls.config = _load_mkdocs()
        cls.nav = cls.config.get("nav", [])
        cls.section_titles = _nav_section_titles(cls.nav)
        cls.nav_refs = _collect_nav_file_refs(cls.nav)

    def test_mkdocs_parses_as_valid_yaml(self):
        self.assertIsInstance(self.config, dict)
        self.assertIn("nav", self.config)

    def test_deleted_nav_sections_absent(self):
        for section in DELETED_NAV_SECTIONS:
            with self.subTest(section=section):
                self.assertNotIn(
                    section,
                    self.section_titles,
                    f"Stale nav section '{section}' still in mkdocs.yml",
                )

    def test_expected_nav_sections_present(self):
        for section in EXPECTED_NAV_SECTIONS:
            with self.subTest(section=section):
                self.assertIn(
                    section,
                    self.section_titles,
                    f"Expected nav section '{section}' missing from mkdocs.yml",
                )

    def test_all_nav_refs_resolve_to_existing_files(self):
        self.assertGreater(len(self.nav_refs), 0, "No nav file refs found")
        for ref in self.nav_refs:
            with self.subTest(ref=ref):
                target = DOCS_DIR / ref
                self.assertTrue(
                    target.is_file(),
                    f"Nav ref '{ref}' -> {target.relative_to(REPO_ROOT)} does not exist",
                )

    def test_nav_does_not_reference_deleted_paths(self):
        """No nav entry should point into a deleted directory."""
        deleted_prefixes = (
            "howto/",
            "tutorials/",
            "reference/",
            "atlas/bug-reports/",
            "testing/",
        )
        for ref in self.nav_refs:
            with self.subTest(ref=ref):
                for prefix in deleted_prefixes:
                    self.assertFalse(
                        ref.startswith(prefix),
                        f"Nav ref '{ref}' points into deleted dir '{prefix}'",
                    )

    def test_no_nav_references_to_deleted_loose_files(self):
        deleted_basenames = {f.name for f in DELETED_LOOSE_FILES}
        for ref in self.nav_refs:
            with self.subTest(ref=ref):
                self.assertNotIn(
                    Path(ref).name,
                    deleted_basenames,
                    f"Nav ref '{ref}' points to a deleted loose file",
                )


class TestNoPointInTimeContentInKeptDocs(unittest.TestCase):
    """Remaining docs must not themselves be point-in-time artifacts.

    We scan only .md files in the *kept* tree (excluding atlas diagrams).
    A file is flagged if its first 20 lines contain point-in-time markers.
    """

    def _kept_md_files(self):
        """Yield markdown files in docs/ that should be permanent."""
        for f in DOCS_DIR.rglob("*.md"):
            rel = f.relative_to(DOCS_DIR)
            parts = rel.parts
            if parts[0] in ("howto", "tutorials", "reference", "testing"):
                continue  # already deleted (or should be)
            if len(parts) >= 2 and parts[0] == "atlas" and parts[1] == "bug-reports":
                continue
            yield f

    def test_kept_docs_are_not_point_in_time(self):
        flagged = []
        for md in self._kept_md_files():
            with md.open(encoding="utf-8") as fh:
                head = "\n".join(itertools.islice(fh, 20))
            if POINT_IN_TIME_RE.search(head):
                flagged.append(str(md.relative_to(REPO_ROOT)))
        self.assertEqual(
            flagged,
            [],
            f"Kept docs appear to be point-in-time artifacts: {flagged}",
        )


class TestFileCountInvariants(unittest.TestCase):
    """Sanity-check the total file count after cleanup."""

    def test_docs_dir_exists(self):
        self.assertTrue(DOCS_DIR.is_dir(), "docs/ directory itself must exist")

    def test_images_dir_untouched(self):
        """docs/images/ is out of scope and must remain."""
        images_dir = DOCS_DIR / "images"
        self.assertTrue(
            images_dir.is_dir(),
            "docs/images/ should not have been deleted",
        )


class TestMkdocsValidation(unittest.TestCase):
    """Ensure mkdocs.yml remains structurally valid after edits."""

    @classmethod
    def setUpClass(cls):
        cls.config = _load_mkdocs()

    def test_strict_mode_enabled(self):
        self.assertTrue(
            self.config.get("strict", False),
            "mkdocs strict mode should remain enabled",
        )

    def test_validation_links_not_found_ignore(self):
        """Must keep 'ignore' for orphaned cross-refs during transition."""
        val = self.config.get("validation", {})
        links = val.get("links", {})
        self.assertEqual(
            links.get("not_found"),
            "ignore",
            "validation.links.not_found must be 'ignore'",
        )

    def test_docs_dir_setting(self):
        self.assertEqual(
            self.config.get("docs_dir"),
            "docs",
            "docs_dir must point to 'docs'",
        )

    def test_theme_configured(self):
        self.assertIn("theme", self.config)
        self.assertEqual(self.config["theme"].get("name"), "material")


if __name__ == "__main__":
    unittest.main()
