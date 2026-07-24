"""Contract tests for the Dependabot version-updates configuration (issue #981).

These tests specify the required shape of ``.github/dependabot.yml`` so pinned
Maven dependencies, GitHub Actions, and mkdocs (pip) docs dependencies do not
rot. They are written test-first: they FAIL until the configuration file exists
with the documented ecosystems, schedules, grouping, and review gate.
"""

import unittest
from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[1]
DEPENDABOT_PATH = REPO_ROOT / ".github" / "dependabot.yml"

REQUIRED_REVIEWER = "rysweet"


def load_config() -> dict:
    """Parse the Dependabot config, failing the calling test if it is absent."""
    if not DEPENDABOT_PATH.exists():
        raise AssertionError(
            f"Missing Dependabot configuration file: {DEPENDABOT_PATH}"
        )
    with DEPENDABOT_PATH.open(encoding="utf-8") as handle:
        return yaml.safe_load(handle)


def updates_for(config: dict, ecosystem: str) -> list[dict]:
    return [
        update
        for update in config.get("updates", [])
        if update.get("package-ecosystem") == ecosystem
    ]


class DependabotConfigContractTest(unittest.TestCase):
    def test_config_file_exists_and_parses_as_yaml(self) -> None:
        config = load_config()
        self.assertIsInstance(
            config, dict, "Dependabot config must parse to a YAML mapping."
        )

    def test_schema_version_is_two(self) -> None:
        config = load_config()
        self.assertEqual(2, config.get("version"), "Dependabot schema must be version 2.")

    def test_declares_exactly_the_three_required_ecosystems(self) -> None:
        config = load_config()
        updates = config.get("updates")
        self.assertIsInstance(updates, list, "updates: must be a list of ecosystems.")
        ecosystems = [entry.get("package-ecosystem") for entry in updates]
        self.assertEqual(
            {"maven", "github-actions", "pip"},
            set(ecosystems),
            "Config must cover exactly maven, github-actions, and pip.",
        )
        self.assertEqual(
            len(ecosystems),
            len(set(ecosystems)),
            "Each ecosystem must appear in a single update block.",
        )

    def test_every_update_block_runs_weekly_with_rysweet_review_gate(self) -> None:
        config = load_config()
        for update in config.get("updates", []):
            ecosystem = update.get("package-ecosystem")
            with self.subTest(ecosystem=ecosystem):
                self.assertEqual(
                    "weekly",
                    (update.get("schedule") or {}).get("interval"),
                    "Every ecosystem must be scheduled weekly.",
                )
                self.assertIn(
                    REQUIRED_REVIEWER,
                    update.get("reviewers", []),
                    "Every ecosystem must keep the rysweet human review gate.",
                )

    def test_maven_block_targets_root_groups_minor_patch_and_limits_prs(self) -> None:
        config = load_config()
        maven_blocks = updates_for(config, "maven")
        self.assertEqual(1, len(maven_blocks), "Exactly one maven block is expected.")
        maven = maven_blocks[0]

        self.assertEqual("/", maven.get("directory"), "Maven must scan the reactor root '/'.")

        limit = maven.get("open-pull-requests-limit")
        self.assertIsInstance(limit, int, "Maven must set a numeric PR limit.")
        self.assertGreater(limit, 0, "Maven PR limit must be a positive number.")
        self.assertLessEqual(limit, 20, "Maven PR limit must stay reasonable (<= 20).")

        groups = maven.get("groups") or {}
        self.assertTrue(groups, "Maven must group updates to reduce PR churn.")
        group_spec = next(iter(groups.values()))
        self.assertEqual(
            ["minor", "patch"],
            sorted(group_spec.get("update-types", [])),
            "Maven grouping must combine minor and patch updates.",
        )

    def test_github_actions_block_covers_workflows_and_setup_xvfb(self) -> None:
        config = load_config()
        gha_blocks = updates_for(config, "github-actions")
        self.assertEqual(
            1, len(gha_blocks), "Exactly one github-actions block is expected."
        )
        gha = gha_blocks[0]

        directories = gha.get("directories")
        self.assertIsInstance(
            directories,
            list,
            "github-actions must use directories: [] to cover multiple paths.",
        )
        self.assertIn("/", directories, "Workflows live at the repository root '/'.")
        self.assertIn(
            "/.github/actions/setup-xvfb",
            directories,
            "The composite setup-xvfb action must be tracked.",
        )

    def test_pip_block_targets_docs_directory(self) -> None:
        config = load_config()
        pip_blocks = updates_for(config, "pip")
        self.assertEqual(1, len(pip_blocks), "Exactly one pip block is expected.")
        self.assertEqual(
            "/docs",
            pip_blocks[0].get("directory"),
            "pip must scan the '/docs' mkdocs dependencies.",
        )


if __name__ == "__main__":
    unittest.main()
