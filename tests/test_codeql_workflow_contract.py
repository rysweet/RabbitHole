"""Contract tests for the CodeQL SAST workflow (issue #982).

These tests specify the required shape of ``.github/workflows/codeql.yml``:
CodeQL static analysis for the ``java-kotlin`` language, triggered on push and
pull requests to ``develop`` plus a weekly schedule, running with least
privilege and building with the canonical Tweedle-aware Maven invocation.

The workflow is parsed both as raw text (to assert on exact command fragments
and step ordering) and as YAML (to assert on structured semantics such as
triggers and permissions).
"""

import re
import unittest
from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[1]
CODEQL_WORKFLOW = REPO_ROOT / ".github" / "workflows" / "codeql.yml"
JOGAMP_SETTINGS = REPO_ROOT / ".github" / "maven" / "jogamp-ci-settings.xml"


def read_workflow_text() -> str:
    return CODEQL_WORKFLOW.read_text(encoding="utf-8")


def load_workflow() -> dict:
    return yaml.safe_load(read_workflow_text())


def step_block(workflow: str, step_name: str) -> str:
    """Return the YAML body of a single named step under ``steps:``."""
    match = re.search(
        rf"(?ms)^      - name: {re.escape(step_name)}\n"
        r"(?P<body>.*?)(?=^      - name: |\Z)",
        workflow,
    )
    if match is None:
        raise AssertionError(f"Missing workflow step: {step_name}")
    return match.group("body")


class CodeqlWorkflowFileTest(unittest.TestCase):
    def test_workflow_file_exists(self) -> None:
        self.assertTrue(
            CODEQL_WORKFLOW.is_file(),
            f"Expected CodeQL workflow at {CODEQL_WORKFLOW}",
        )

    def test_workflow_is_well_formed_yaml(self) -> None:
        data = load_workflow()
        self.assertIsInstance(data, dict)
        self.assertEqual(data.get("name"), "CodeQL")


class CodeqlWorkflowTriggerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.data = load_workflow()
        # PyYAML parses the bare ``on:`` key as the boolean True.
        self.triggers = self.data.get(True, self.data.get("on"))

    def test_triggers_on_push_to_develop(self) -> None:
        self.assertIn("push", self.triggers)
        self.assertEqual(self.triggers["push"]["branches"], ["develop"])

    def test_triggers_on_pull_request_to_develop(self) -> None:
        self.assertIn("pull_request", self.triggers)
        self.assertEqual(self.triggers["pull_request"]["branches"], ["develop"])

    def test_has_weekly_cron_schedule(self) -> None:
        self.assertIn("schedule", self.triggers)
        crons = [entry["cron"] for entry in self.triggers["schedule"]]
        self.assertEqual(len(crons), 1)
        cron = crons[0]
        fields = cron.split()
        self.assertEqual(
            len(fields), 5, f"cron must have five fields, got: {cron!r}"
        )
        day_of_week = fields[4]
        self.assertNotEqual(
            day_of_week,
            "*",
            "A weekly schedule must pin a day-of-week, not run daily.",
        )

    def test_does_not_use_pull_request_target(self) -> None:
        # pull_request_target grants a read/write token to untrusted fork code.
        self.assertNotIn("pull_request_target", read_workflow_text())


class CodeqlWorkflowPermissionsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.data = load_workflow()

    def test_top_level_permissions_are_read_only(self) -> None:
        self.assertEqual(self.data.get("permissions"), {"contents": "read"})

    def test_analyze_job_has_least_privilege_permissions(self) -> None:
        perms = self.data["jobs"]["analyze"]["permissions"]
        self.assertEqual(
            perms,
            {
                "security-events": "write",
                "contents": "read",
                "actions": "read",
            },
        )

    def test_no_write_permissions_beyond_security_events(self) -> None:
        workflow = read_workflow_text()
        self.assertNotRegex(workflow, r"(?m)^\s+contents: write$")
        self.assertNotRegex(workflow, r"(?m)^\s+pull-requests: write$")


class CodeqlWorkflowStepsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.workflow = read_workflow_text()
        self.data = load_workflow()
        self.steps = self.data["jobs"]["analyze"]["steps"]

    def test_checks_out_source_without_recursive_submodules(self) -> None:
        block = step_block(self.workflow, "Check out source")
        self.assertIn("uses: actions/checkout@v4", block)
        self.assertIn("submodules: false", block)
        self.assertNotIn("submodules: recursive", block)

    def test_initializes_tweedle_submodule_before_build(self) -> None:
        block = step_block(self.workflow, "Initialize Tweedle grammar submodule")
        self.assertIn(
            "run: git submodule update --init tweedle-lang", block
        )
        submodule_idx = self.workflow.index("Initialize Tweedle grammar submodule")
        build_idx = self.workflow.index("Build (Maven)")
        self.assertLess(
            submodule_idx,
            build_idx,
            "Submodule must be initialized before the Maven build.",
        )

    def test_sets_up_temurin_jdk_21(self) -> None:
        block = step_block(self.workflow, "Set up JDK 21")
        self.assertIn("uses: actions/setup-java@v4", block)
        self.assertIn("distribution: temurin", block)
        self.assertIn("java-version: '21'", block)

    def test_initializes_codeql_for_java_kotlin_with_manual_build(self) -> None:
        block = step_block(self.workflow, "Initialize CodeQL")
        self.assertIn("uses: github/codeql-action/init@v3", block)
        self.assertIn("languages: java-kotlin", block)
        self.assertIn("build-mode: manual", block)

    def test_manual_build_uses_canonical_tweedle_aware_maven_command(self) -> None:
        block = step_block(self.workflow, "Build (Maven)")
        for fragment in (
            "mvn",
            "--settings .github/maven/jogamp-ci-settings.xml",
            "-DskipTests",
            "-Dcheckstyle.skip",
            "-Dlicense.skipAggregateDownloadLicenses=true",
            "-Dinstall4j.skip",
            "-Djava.awt.headless=true",
            "-q install",
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, block)

    def test_manual_build_does_not_hardcode_java_home(self) -> None:
        # setup-java exports JAVA_HOME for the Temurin 21 JDK; hardcoding a
        # distro path (java-21-openjdk-amd64) would shadow it.
        block = step_block(self.workflow, "Build (Maven)")
        self.assertNotIn("JAVA_HOME=", block)
        self.assertNotIn("java-21-openjdk-amd64", block)

    def test_analyze_step_runs_after_build(self) -> None:
        block = step_block(self.workflow, "Perform CodeQL Analysis")
        self.assertIn("uses: github/codeql-action/analyze@v3", block)
        self.assertIn("category: /language:java-kotlin", block)
        build_idx = self.workflow.index("Build (Maven)")
        analyze_idx = self.workflow.index("Perform CodeQL Analysis")
        self.assertLess(
            build_idx,
            analyze_idx,
            "CodeQL analysis must run after the Maven build.",
        )

    def test_referenced_jogamp_settings_file_exists(self) -> None:
        self.assertTrue(
            JOGAMP_SETTINGS.is_file(),
            f"Build references settings file that must exist: {JOGAMP_SETTINGS}",
        )


class CodeqlWorkflowMaintainabilityTest(unittest.TestCase):
    def setUp(self) -> None:
        self.workflow = read_workflow_text()
        self.data = load_workflow()

    def test_pull_request_concurrency_cancels_only_same_pr(self) -> None:
        self.assertIn("concurrency:", self.workflow)
        self.assertIn("github.event.pull_request.number", self.workflow)
        self.assertIn(
            "cancel-in-progress: ${{ github.event_name == 'pull_request' }}",
            self.workflow,
        )

    def test_analyze_job_has_a_timeout(self) -> None:
        self.assertIn("timeout-minutes", self.data["jobs"]["analyze"])

    def test_runs_on_ubuntu(self) -> None:
        self.assertEqual(self.data["jobs"]["analyze"]["runs-on"], "ubuntu-latest")


if __name__ == "__main__":
    unittest.main()
