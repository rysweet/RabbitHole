import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
COVERAGE_WORKFLOW = REPO_ROOT / ".github" / "workflows" / "alice-coverage-ci.yml"


class CoverageWorkflowContractTest(unittest.TestCase):
    def test_coverage_workflow_generates_reports_before_running_ratchets(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")

        generate_step = workflow.index(
            "name: Generate open-asset aggregate and per-module coverage reports"
        )
        summarize_step = workflow.index("name: Summarize and gate line coverage")

        self.assertLess(generate_step, summarize_step)
        coverage_command = re.search(r"mvn .* -Pcoverage verify", workflow)
        self.assertIsNotNone(coverage_command)
        assert coverage_command is not None
        for flag in (
            "-Dinstall4j.skip",
            "-Dcheckstyle.skip",
            "-Dmaven.test.failure.ignore=true",
            "-Dmdep.skip=true",
            "-Pcoverage",
        ):
            with self.subTest(flag=flag):
                self.assertIn(flag, coverage_command.group(0))

    def test_coverage_workflow_defers_submodule_initialization_until_maven_runs(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")
        checkout_step = re.search(
            r"name: Check out source(?P<body>.*?)- name: Initialize Tweedle grammar submodule",
            workflow,
            flags=re.DOTALL,
        )
        self.assertIsNotNone(checkout_step)
        assert checkout_step is not None

        self.assertIn("uses: actions/checkout@v4", checkout_step.group("body"))
        self.assertIn("lfs: false", checkout_step.group("body"))
        self.assertIn("submodules: false", checkout_step.group("body"))
        self.assertNotIn("git lfs", workflow.lower())

        submodule_step = re.search(
            r"name: Initialize Tweedle grammar submodule(?P<body>.*?)- name: Set up JDK 21",
            workflow,
            flags=re.DOTALL,
        )
        self.assertIsNotNone(submodule_step)
        assert submodule_step is not None
        self.assertIn(
            "if: github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true'",
            submodule_step.group("body"),
        )
        self.assertIn(
            "run: git submodule update --init tweedle-lang",
            submodule_step.group("body"),
        )

    def test_coverage_workflow_enforces_aggregate_and_module_ratchets(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")
        expected_thresholds = [
            "--min-aggregate-line-percent 8.0",
            "--min-module-line-percent core/ast=18.0",
            "--min-module-line-percent core/model-loading=10.0",
            "--min-module-line-percent core/story-api-migration=75.0",
            "--min-module-line-percent core/tweedle=50.0",
            "--min-module-line-percent core/scenegraph=10.0",
            "--min-module-line-percent netbeans=25.0",
        ]

        for threshold in expected_thresholds:
            with self.subTest(threshold=threshold):
                self.assertEqual(1, workflow.count(threshold))

    def test_coverage_workflow_keeps_summary_and_artifact_steps_on_failure(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")

        summary_step = re.search(
            r"name: Summarize and gate line coverage(?P<body>.*?)- name: Upload coverage reports",
            workflow,
            flags=re.DOTALL,
        )
        self.assertIsNotNone(summary_step)
        assert summary_step is not None
        self.assertIn("if: always()", summary_step.group("body"))
        self.assertIn("--output coverage-summary.md", summary_step.group("body"))
        self.assertIn(
            "--evidence-manifest coverage-evidence-manifest.json",
            summary_step.group("body"),
        )
        self.assertIn("if: always()", workflow[summary_step.end() :])

    def test_coverage_workflow_records_long_term_target_without_raising_ci_floor(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")
        summary_step = re.search(
            r"name: Summarize and gate line coverage(?P<body>.*?)- name: Upload coverage reports",
            workflow,
            flags=re.DOTALL,
        )
        self.assertIsNotNone(summary_step)
        assert summary_step is not None
        body = summary_step.group("body")

        self.assertIn("--target-aggregate-line-percent 70.0", body)
        self.assertEqual(1, body.count("--target-aggregate-line-percent"))
        self.assertIn("--min-aggregate-line-percent 8.0", body)
        self.assertNotIn("--min-aggregate-line-percent 70.0", body)

    def test_coverage_workflow_uploads_summary_and_diagnostics_without_requiring_success(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")
        upload_step = re.search(
            r"name: Upload coverage reports(?P<body>.*)",
            workflow,
            flags=re.DOTALL,
        )
        self.assertIsNotNone(upload_step)
        assert upload_step is not None

        body = upload_step.group("body")
        expected_artifacts = [
            "coverage-summary.md",
            "coverage-evidence-manifest.json",
            "coverage-report/target/site/jacoco-aggregate/**",
            "**/target/site/jacoco/**",
            "**/target/jacoco.exec",
            "**/target/surefire-reports/**",
        ]

        self.assertIn("if: always()", body)
        self.assertIn("name: alice-coverage-evidence-open-assets", body)
        self.assertIn("if-no-files-found: warn", body)
        for artifact in expected_artifacts:
            with self.subTest(artifact=artifact):
                self.assertIn(artifact, body)


if __name__ == "__main__":
    unittest.main()
