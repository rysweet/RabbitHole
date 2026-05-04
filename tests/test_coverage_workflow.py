import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
COVERAGE_WORKFLOW = REPO_ROOT / ".github" / "workflows" / "alice-coverage-ci.yml"


class CoverageWorkflowContractTest(unittest.TestCase):
    def test_coverage_workflow_generates_reports_before_running_ratchets(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")

        generate_step = workflow.index(
            "name: Generate no-Sims aggregate and per-module coverage reports"
        )
        summarize_step = workflow.index("name: Summarize and gate line coverage")

        self.assertLess(generate_step, summarize_step)
        self.assertIn("mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify", workflow)

    def test_coverage_workflow_enforces_aggregate_and_module_ratchets(self) -> None:
        workflow = COVERAGE_WORKFLOW.read_text(encoding="utf-8")
        expected_thresholds = [
            "--min-aggregate-line-percent 8.0",
            "--min-module-line-percent core/ast=18.0",
            "--min-module-line-percent core/model-loading=10.0",
            "--min-module-line-percent core/story-api-migration=75.0",
            "--min-module-line-percent core/tweedle=50.0",
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
        self.assertIn("if: always()", workflow[summary_step.end() :])


if __name__ == "__main__":
    unittest.main()
