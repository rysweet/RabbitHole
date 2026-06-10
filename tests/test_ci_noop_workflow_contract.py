import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
WORKFLOW_DIR = REPO_ROOT / ".github" / "workflows"

EVENT_AWARE_MAVEN_GATE = (
    "github.event_name != 'pull_request' || "
    "steps.change-scope.outputs.maven-required == 'true'"
)
EVENT_AWARE_ALWAYS_GATE = f"always() && ({EVENT_AWARE_MAVEN_GATE})"

WORKFLOWS = {
    "checkstyle": {
        "path": WORKFLOW_DIR / "alice-checkstyle-ci.yml",
        "workflow_name": "Alice Checkstyle CI",
        "job": "build",
        "maven_step": "Run Checkstyle with Maven",
        "maven_command": "mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml",
        "dependent_steps": [],
    },
    "test": {
        "path": WORKFLOW_DIR / "alice-test-ci.yml",
        "workflow_name": "Alice Test CI",
        "job": "test",
        "validation_steps": [
            {
                "name": "Run Getting Started headless validation",
                "fragments": [
                    "MAVEN_SETTINGS_PATH=.github/maven/jogamp-ci-settings.xml ./scripts/validate-getting-started.sh --headless",
                ],
                "requires_checkstyle_skip": False,
            },
            {
                "name": "Run dual-baseline replay harness fallback",
                "fragments": [
                    "mvn --settings .github/maven/jogamp-ci-settings.xml -pl core/story-api-migration",
                    "-DincludeSims=false",
                    "-Dinstall4j.skip",
                    "-Dcheckstyle.skip",
                    "-Djava.awt.headless=true",
                    "-Dtest=DualBaselineReplayHarnessTest",
                    "test",
                ],
                "requires_checkstyle_skip": True,
            },
        ],
        "dependent_steps": [],
    },
    "coverage": {
        "path": WORKFLOW_DIR / "alice-coverage-ci.yml",
        "workflow_name": "Alice Coverage Reports",
        "job": "coverage",
        "maven_step": "Generate no-Sims aggregate and per-module coverage reports",
        "maven_command": "mvn -U -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -Pcoverage verify",
        "maven_fragments": [
            "mvn -U -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -Pcoverage verify",
            "Coverage Maven command failed; retrying",
        ],
        "dependent_steps": [
            "Summarize and gate line coverage",
            "Upload coverage reports",
        ],
    },
    "netbeans-package": {
        "path": WORKFLOW_DIR / "alice-netbeans-package-ci.yml",
        "workflow_name": "Alice NetBeans Package CI",
        "job": "package-netbeans",
        "maven_step": "Build NetBeans package without Sims assets",
        "maven_command": "mvn --settings .github/maven/jogamp-ci-settings.xml -U -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -pl netbeans -am package -DskipTests",
        "maven_fragments": [
            "mvn --settings .github/maven/jogamp-ci-settings.xml -U -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -pl netbeans -am package -DskipTests",
            "NetBeans package Maven command failed; retrying",
        ],
        "dependent_steps": [
            "Verify NetBeans package artifacts",
        ],
    },
}


def validation_steps(spec: dict) -> list[dict]:
    if "validation_steps" in spec:
        return spec["validation_steps"]
    return [
        {
            "name": spec["maven_step"],
            "fragments": spec.get("maven_fragments", [f"run: {spec['maven_command']}"]),
            "unique_command": spec["maven_command"],
            "requires_checkstyle_skip": spec["workflow_name"] != "Alice Checkstyle CI",
        }
    ]


def read_workflow(key: str) -> str:
    return WORKFLOWS[key]["path"].read_text(encoding="utf-8")


def step_block(workflow: str, step_name: str) -> str:
    step_match = re.search(
        rf"(?ms)^      - name: {re.escape(step_name)}\n"
        r"(?P<body>.*?)(?=^      - name: |\Z)",
        workflow,
    )
    if step_match is None:
        raise AssertionError(f"Missing workflow step: {step_name}")
    return step_match.group("body")


def change_scope_block(workflow: str) -> str:
    block = step_block(workflow, "Classify pull request change scope")
    if "id: change-scope" not in block:
        raise AssertionError(
            "Missing PR-only change-scope step with id: change-scope."
        )
    return block


def assert_step_has_gate(
    test_case: unittest.TestCase,
    workflow: str,
    step_name: str,
    expected_gate: str,
) -> None:
    block = step_block(workflow, step_name)
    test_case.assertIn(
        f"if: {expected_gate}",
        block,
        f"{step_name} must be gated by the Maven-required classifier.",
    )


class CiNoopWorkflowContractTest(unittest.TestCase):
    def test_required_workflows_keep_status_surfaces_and_do_not_use_path_filters(self) -> None:
        for key, spec in WORKFLOWS.items():
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                self.assertIn(f"name: {spec['workflow_name']}", workflow)
                self.assertRegex(workflow, rf"(?m)^  {re.escape(spec['job'])}:$")
                self.assertIn("pull_request:", workflow)
                self.assertIn("push:", workflow)
                self.assertIn("branches: [develop]", workflow)
                self.assertRegex(
                    workflow,
                    r"(?ms)^permissions:\n  contents: read\n",
                )
                self.assertNotRegex(workflow, r"(?m)^\s+contents: write$")
                self.assertNotRegex(workflow, r"(?m)^\s+pull-requests: write$")
                self.assertNotIn("pull_request_target", workflow)
                self.assertNotRegex(workflow, r"(?m)^\s+paths(?:-ignore)?:")

    def test_pull_request_concurrency_cancels_only_runs_for_the_same_pr(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                self.assertIn("concurrency:", workflow)
                self.assertIn("github.event.pull_request.number", workflow)
                self.assertIn(
                    "cancel-in-progress: ${{ github.event_name == 'pull_request' }}",
                    workflow,
                )
                self.assertNotIn("github.head_ref", workflow)

    def test_each_workflow_has_pr_only_change_scope_step(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                block = change_scope_block(workflow)
                self.assertIn("if: github.event_name == 'pull_request'", block)
                self.assertIn("shell: bash", block)
                self.assertIn("maven-required", block)
                self.assertIn("reason", block)
                self.assertIn("GITHUB_OUTPUT", block)

    def test_change_scope_uses_exact_pr_shas_and_fails_closed_on_diff_uncertainty(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                block = change_scope_block(workflow)
                self.assertIn("${{ github.event.pull_request.base.sha }}", block)
                self.assertIn("${{ github.event.pull_request.head.sha }}", block)
                self.assertRegex(block, r"git\s+fetch\b")
                self.assertRegex(block, r"for attempt in 1 2 3")
                self.assertRegex(block, r"after retries")
                self.assertRegex(block, r"git\s+diff\b")
                self.assertRegex(block, r"--name-status\b")
                self.assertRegex(block, r"maven-required=true")
                self.assertRegex(block, r"(diff|fetch|uncertain|failed)")

    def test_change_scope_accounts_for_renames_and_copies_with_old_and_new_paths(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                block = change_scope_block(workflow)
                self.assertRegex(block, r"\bR\*?|\bR[0-9]+")
                self.assertRegex(block, r"\bC\*?|\bC[0-9]+")
                self.assertRegex(block, r"old[_-]?path|previous[_-]?path|from[_-]?path")
                self.assertRegex(block, r"new[_-]?path|current[_-]?path|to[_-]?path")

    def test_change_scope_allowlist_is_case_sensitive_and_docs_license_only(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                block = change_scope_block(workflow)
                self.assertIn("docs/", block)
                self.assertRegex(block, r"\*\.md\b|\.md\)")
                self.assertRegex(block, r'\[\[ "\$\{path\}" != \*/\* \]\]')
                for license_name in ("LICENSE", "LICENSE.md", "NOTICE", "NOTICE.md"):
                    self.assertIn(license_name, block)
                self.assertNotIn("nocasematch", block)
                self.assertNotIn("DOCS/", block)
                self.assertNotIn("docs/*|*.md", block)

    def test_change_scope_fails_closed_for_unknown_malformed_and_mixed_changes(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                block = change_scope_block(workflow)
                self.assertRegex(block, r"maven-required=true")
                self.assertRegex(block, r"unknown|unrecognized|default|otherwise|\*\)")
                self.assertRegex(block, r"malformed|invalid|absolute|\.\.")
                self.assertRegex(block, r"mixed|impacting|validation")
                self.assertNotIn("has_allowed", block)
                self.assertNotIn("skip_maven", block)

    def test_maven_commands_preserve_validation_surfaces_and_event_aware_gates(self) -> None:
        for key, spec in WORKFLOWS.items():
            workflow = read_workflow(key)
            for step in validation_steps(spec):
                with self.subTest(workflow=key, step=step["name"]):
                    block = step_block(workflow, step["name"])
                    for fragment in step["fragments"]:
                        self.assertIn(fragment, block)
                    if "unique_command" in step:
                        self.assertEqual(1, workflow.count(step["unique_command"]))
                    self.assertIn(f"if: {EVENT_AWARE_MAVEN_GATE}", block)
                    if step["requires_checkstyle_skip"]:
                        self.assertIn("-Dcheckstyle.skip", block)
                    else:
                        self.assertNotIn("-Dcheckstyle.skip", block)

    def test_maven_setup_runs_only_when_maven_validation_is_required(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                checkout_block = step_block(workflow, "Check out source")
                self.assertIn("uses: actions/checkout@v4", checkout_block)
                self.assertIn("lfs: false", checkout_block)
                self.assertIn("submodules: false", checkout_block)
                self.assertIn("fetch-depth: 1", checkout_block)
                self.assertNotIn("submodules: recursive", checkout_block)

                submodule_block = step_block(
                    workflow, "Initialize Tweedle grammar submodule"
                )
                self.assertIn(f"if: {EVENT_AWARE_MAVEN_GATE}", submodule_block)
                self.assertIn(
                    "run: git submodule update --init tweedle-lang",
                    submodule_block,
                )

                jdk_block = step_block(workflow, "Set up JDK 21")
                self.assertIn(f"if: {EVENT_AWARE_MAVEN_GATE}", jdk_block)
                self.assertIn("uses: actions/setup-java@v4", jdk_block)
                self.assertIn("cache: maven", jdk_block)

                maven_version_block = step_block(workflow, "Show Maven version")
                self.assertIn(f"if: {EVENT_AWARE_MAVEN_GATE}", maven_version_block)
                self.assertIn("run: mvn -v", maven_version_block)

                self.assertNotIn("actions/cache/restore", workflow)
                self.assertNotIn("Restore Maven cache fallback", workflow)

    def test_docs_only_noop_step_is_present_and_logs_only_reason_and_paths(self) -> None:
        for key in WORKFLOWS:
            workflow = read_workflow(key)
            with self.subTest(workflow=key):
                block = step_block(workflow, "Report Maven validation no-op")
                self.assertIn(
                    "if: github.event_name == 'pull_request' && "
                    "steps.change-scope.outputs.maven-required == 'false'",
                    block,
                )
                self.assertIn("steps.change-scope.outputs.reason", block)
                self.assertRegex(block, r"changed[_-]?files|changed files")
                self.assertNotIn("toJson(github)", block)
                self.assertNotRegex(block, r"\b(env|printenv)\b")
                self.assertNotRegex(block, r"(?m)^\s*set\s*$")

    def test_maven_dependent_evidence_steps_are_gated_with_the_same_decision(self) -> None:
        for key, spec in WORKFLOWS.items():
            workflow = read_workflow(key)
            for step_name in spec["dependent_steps"]:
                with self.subTest(workflow=key, step=step_name):
                    expected_gate = (
                        EVENT_AWARE_ALWAYS_GATE
                        if key == "coverage"
                        else EVENT_AWARE_MAVEN_GATE
                    )
                    assert_step_has_gate(self, workflow, step_name, expected_gate)

    def test_coverage_summary_and_artifact_steps_keep_failure_evidence_when_maven_runs(self) -> None:
        workflow = read_workflow("coverage")

        for step_name in ("Summarize and gate line coverage", "Upload coverage reports"):
            with self.subTest(step=step_name):
                block = step_block(workflow, step_name)
                self.assertIn(f"if: {EVENT_AWARE_ALWAYS_GATE}", block)


if __name__ == "__main__":
    unittest.main()
