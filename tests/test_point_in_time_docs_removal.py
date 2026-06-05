"""Repository hygiene contract for durable RabbitHole documentation.

These tests define the contract for removing non-durable point-in-time artifacts
while preserving maintained RabbitHole/Alice documentation.
"""

from __future__ import annotations

import re
import subprocess
import unittest
from functools import lru_cache
from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[1]
DOCS_DIR = REPO_ROOT / "docs"
MKDOCS_YML = REPO_ROOT / "mkdocs.yml"
GITIGNORE = REPO_ROOT / ".gitignore"

TEMPORARY_TRACKED_PATH_RE = re.compile(
    r"(?:"
    r"^\." + r"co" + r"pilot(?:-|/)"
    r"|^\." + r"ampli" + r"hack/"
    r"|^\.github/hooks/"
    r"|^drinkme/"
    r"|^refactor-[^/]+\.log$"
    r"|(?:^|/)[^/]*workflow[^/]*\.(?:log|status|summary|exit)$"
    r"|(?:^|/)[^/]*(?:progress|status)-report[^/]*\.(?:md|txt|log|sh)$"
    r"|(?:^|/)coverage-summary-[^/]*\.md$"
    r"|^qa/outside-in/alice-desktop/logs/.+"
    r"|^qa/outside-in/alice-desktop/outputs/.+"
    r"|^qa/outside-in/alice-desktop/evidence/.+"
    r"|^qa/outside-in/alice-desktop/(?:gadu" + r"gi|gadu" + r"gi-scenarios)/"
    r"|^qa/outside-in/alice-desktop/tests/test-ampli" + r"hack-cli-contract\.sh$"
    r"|^qa/outside-in/alice-desktop/tests/test-gadu" + r"gi[^/]*\.sh$"
    r"|^tests/test_alice_qa_ampli" + r"hack(?:_docs_contract)?\.py$"
    r"|^tests/test_gadu" + r"gi[^/]*\.py$"
    r"|^alice_qa_ampli" + r"hack\.py$"
    r")",
    re.IGNORECASE,
)

POINT_IN_TIME_REFERENCE_RE = re.compile(
    r"(?:"
    r"\bPR\s*#\d{2,}"
    r"|\bpr[-_ ]#?\d{2,}"
    r"|\bpull/\d{2,}"
    r"|\bissues/\d{2,}"
    r"|(?<![A-Za-z0-9])#\d{2,}\b"
    r")",
    re.IGNORECASE,
)

TEMPORARY_BRANDING_RE = re.compile(
    r"\b(?:ampli" + r"hack|co" + r"pilot|gadu" + r"gi|gadu" + r"gi-test)\b",
    re.IGNORECASE,
)

POINT_IN_TIME_LANGUAGE_RE = re.compile(
    r"\b(?:"
    r"current " + r"status"
    r"|coverage " + r"snapshot"
    r"|refactor " + r"progress"
    r"|status " + r"report"
    r"|work in " + r"progress"
    r"|implementation " + r"pending"
    r"|coverage " + r"push"
    r"|remaining " + r"work"
    r"|next " + r"steps"
    r"|session " + r"artifact"
    r"|assistant " + r"trace"
    r"|workflow " + r"log"
    r")\b",
    re.IGNORECASE,
)

DURABLE_CONTENT_SUFFIXES = {".md", ".yaml", ".yml", ".toml", ".sh", ".py"}
DURABLE_CONTENT_ROOTS = (
    "README.md",
    "AGENTS.md",
    "docs/",
    "core/croquet/TESTING.md",
    "core/story-api/TESTING.md",
    "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/GlResourceCache.md",
    "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/RenderTargetGlEventHandler.md",
    "core/ide/src/test/resources/org/alice/ide/coverage/small-class-targets.txt",
    "qa/outside-in/alice-desktop/",
    "pyproject.toml",
)
CONTENT_SCAN_EXCLUDES = (
    "qa/outside-in/alice-desktop/tests/fixtures/",
    "qa/outside-in/alice-desktop/schema/",
    "qa/outside-in/alice-desktop/runners/",
)
REQUIRED_IGNORES = (
    "." + "co" + "pilot/",
    "." + "co" + "pilot-*",
    "." + "ampli" + "hack/",
    ".github/hooks/",
    "refactor-*.log",
    "*workflow*.log",
    "*progress*.md",
    "*status*.md",
    "qa/outside-in/alice-desktop/logs/",
    "qa/outside-in/alice-desktop/outputs/",
    "qa/outside-in/alice-desktop/evidence/",
)

REMOVED_POINT_IN_TIME_DOCS = (
    "docs/reference/modernization-scorecard.md",
)

EXPECTED_DURABLE_DOCS = (
    "README.md",
    "AGENTS.md",
    "docs/index.md",
    "docs/getting-started.md",
    "docs/testing.md",
    "docs/contributing.md",
    "docs/repository-hygiene.md",
    "qa/outside-in/alice-desktop/README.md",
)


class _MkdocsLoader(yaml.SafeLoader):
    """SafeLoader subclass that accepts mkdocs custom Python tags."""


_MkdocsLoader.add_multi_constructor(
    "tag:yaml.org,2002:python/name:",
    lambda loader, suffix, node: f"!!python/name:{suffix}",
)


@lru_cache(maxsize=1)
def tracked_files() -> tuple[str, ...]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return tuple(path for path in result.stdout.splitlines() if path)


@lru_cache(maxsize=1)
def mkdocs_config() -> dict:
    return yaml.load(MKDOCS_YML.read_text(encoding="utf-8"), Loader=_MkdocsLoader)


def collect_nav_refs(nav, refs: list[str] | None = None) -> list[str]:
    if refs is None:
        refs = []
    if isinstance(nav, str):
        refs.append(nav)
    elif isinstance(nav, dict):
        for value in nav.values():
            collect_nav_refs(value, refs)
    elif isinstance(nav, list):
        for item in nav:
            collect_nav_refs(item, refs)
    return refs


def durable_content_paths(exclude: tuple[str, ...] = ()) -> list[str]:
    paths: list[str] = []
    for path in tracked_files():
        if not path.endswith(tuple(DURABLE_CONTENT_SUFFIXES)):
            continue
        if not any(path == root or path.startswith(root) for root in DURABLE_CONTENT_ROOTS):
            continue
        if any(path.startswith(excluded) for excluded in CONTENT_SCAN_EXCLUDES):
            continue
        if path in exclude:
            continue
        paths.append(path)
    return paths


def matching_lines(pattern: re.Pattern[str], paths: list[str]) -> list[str]:
    matches: list[str] = []
    for relative_path in paths:
        text = (REPO_ROOT / relative_path).read_text(encoding="utf-8")
        for line_number, line in enumerate(text.splitlines(), start=1):
            if pattern.search(line):
                matches.append(f"{relative_path}:{line_number}:{line.strip()}")
    return matches


class RepositoryArtifactPrunerContract(unittest.TestCase):
    def test_no_tracked_temporary_artifacts_remain(self) -> None:
        offenders = [
            path for path in tracked_files() if TEMPORARY_TRACKED_PATH_RE.search(path)
        ]

        self.assertEqual(
            [],
            offenders,
            "Tracked generated workflow outputs, session/evidence outputs, branded wrappers, or "
            "point-in-time reports remain in the repository.",
        )

    def test_ignore_rules_cover_removed_artifact_families(self) -> None:
        ignore_text = GITIGNORE.read_text(encoding="utf-8")

        missing = [pattern for pattern in REQUIRED_IGNORES if pattern not in ignore_text]

        self.assertEqual(
            [],
            missing,
            "Removed local workflow outputs and session files must be ignored so they "
            "are not reintroduced.",
        )


class DurableDocumentationRewriteContract(unittest.TestCase):
    def test_core_user_facing_docs_survive_cleanup(self) -> None:
        missing = [
            path for path in EXPECTED_DURABLE_DOCS if not (REPO_ROOT / path).is_file()
        ]

        self.assertEqual([], missing, "Cleanup deleted durable user-facing docs.")

    def test_point_in_time_scorecard_snapshot_is_not_tracked(self) -> None:
        tracked = set(tracked_files())
        remaining = [path for path in REMOVED_POINT_IN_TIME_DOCS if path in tracked]

        self.assertEqual(
            [],
            remaining,
            "Generated scorecard/status snapshots should be deleted or rewritten as "
            "durable generator documentation.",
        )

    def test_durable_docs_do_not_contain_concrete_tracking_references(self) -> None:
        matches = matching_lines(POINT_IN_TIME_REFERENCE_RE, durable_content_paths())

        self.assertEqual(
            [],
            matches,
            "Durable docs and QA metadata may use generic issue/pull request "
            "language, but not concrete PR/issue numbers.",
        )

    def test_durable_docs_do_not_use_point_in_time_language(self) -> None:
        matches = matching_lines(
            POINT_IN_TIME_LANGUAGE_RE,
            durable_content_paths(),
        )

        self.assertEqual(
            [],
            matches,
            "Durable docs should describe the maintained contract, not branch-local "
            "status, progress, next-step, or session narratives.",
        )

    def test_durable_docs_do_not_keep_tool_specific_branding(self) -> None:
        matches = matching_lines(TEMPORARY_BRANDING_RE, durable_content_paths())

        self.assertEqual(
            [],
            matches,
            "RabbitHole documentation and retained QA contracts should use neutral "
            "Alice/RabbitHole names rather than assistant/tool branding.",
        )


class MkDocsNavigationContract(unittest.TestCase):
    def test_mkdocs_parses_and_all_nav_refs_exist(self) -> None:
        config = mkdocs_config()
        refs = collect_nav_refs(config.get("nav", []))

        self.assertGreater(len(refs), 0, "mkdocs.yml must define navigation.")
        missing = [ref for ref in refs if not (DOCS_DIR / ref).is_file()]

        self.assertEqual([], missing, "mkdocs.yml references missing docs.")

    def test_nav_does_not_link_deleted_point_in_time_docs(self) -> None:
        refs = collect_nav_refs(mkdocs_config().get("nav", []))

        stale = [ref for ref in refs if f"docs/{ref}" in REMOVED_POINT_IN_TIME_DOCS]

        self.assertEqual([], stale, "mkdocs.yml still links point-in-time docs.")

    def test_strict_docs_mode_remains_enabled(self) -> None:
        config = mkdocs_config()

        self.assertTrue(config.get("strict"), "mkdocs strict mode should remain enabled.")


if __name__ == "__main__":
    unittest.main()
