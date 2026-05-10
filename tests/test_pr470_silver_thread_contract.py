"""PR #470 contract tests: silver-thread-launch-build-run 4-layer allowlist consistency.

Verifies that the silver-thread scenario is correctly wired across all four
allowlist layers (schema, validator, runner, contract test) and that the
JUnit test class exists with the expected test methods.
"""

import json
import re
import unittest
from functools import lru_cache
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

SCENARIO_YAML = Path(
    "qa/outside-in/alice-desktop/scenarios/silver-thread-launch-build-run.yaml"
)
SCHEMA_JSON = Path("qa/outside-in/alice-desktop/schema/scenario.schema.json")
VALIDATOR_SH = Path("qa/outside-in/alice-desktop/runners/validate-scenarios.sh")
RUNNER_SH = Path("qa/outside-in/alice-desktop/runners/run-scenario.sh")
SCHEMA_CONTRACT_SH = Path("qa/outside-in/alice-desktop/tests/test-schema-contract.sh")
JUNIT_TEST = Path(
    "core/ide/src/test/java/org/alice/ide/SilverThreadLaunchBuildRunTest.java"
)

SILVER_THREAD_WORKFLOW = "silver-thread-launch-build-run"
SILVER_THREAD_ID = "alice-desktop-silver-thread-launch-build-run"
SILVER_THREAD_TEST_CLASS = "org.alice.ide.SilverThreadLaunchBuildRunTest"

EXPECTED_ARGV = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ide",
    "-am",
    f"-Dtest={SILVER_THREAD_TEST_CLASS}",
    "test",
)


@lru_cache(maxsize=None)
def _read(relative: Path) -> str:
    return (REPO_ROOT / relative).read_text(encoding="utf-8")


@lru_cache(maxsize=None)
def _load_schema() -> dict:
    return json.loads(_read(SCHEMA_JSON))


class SilverThreadScenarioExistsTest(unittest.TestCase):
    """The scenario YAML file exists and has the required fields."""

    def test_scenario_yaml_exists(self) -> None:
        self.assertTrue(
            (REPO_ROOT / SCENARIO_YAML).is_file(),
            f"Missing scenario: {SCENARIO_YAML.as_posix()}",
        )

    def test_scenario_has_id(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertTrue(
            re.search(rf"^id:\s+{re.escape(SILVER_THREAD_ID)}$", text, re.M),
            "Scenario must have correct id",
        )

    def test_scenario_has_title(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertTrue(
            re.search(r"^title:\s+\S", text, re.M),
            "Scenario must have a non-empty title",
        )

    def test_scenario_has_workflow(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertTrue(
            re.search(rf"^workflow:\s+{re.escape(SILVER_THREAD_WORKFLOW)}$", text, re.M),
            "Scenario must have correct workflow",
        )

    def test_scenario_uses_gated_command_smoke(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertTrue(
            re.search(r"^automationMode:\s+gated-command-smoke$", text, re.M),
            "Scenario must use gated-command-smoke mode",
        )

    def test_scenario_has_preconditions(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertIn("preconditions:", text)

    def test_scenario_has_expected_outcomes(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertIn("expectedOutcomes:", text)

    def test_scenario_has_evidence_block(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertIn("evidence:", text)

    def test_scenario_has_fallback_block(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertIn("fallback:", text)

    def test_scenario_tags_include_silver_thread(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertIn("silver-thread", text)

    def test_scenario_tags_include_end_to_end(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertIn("end-to-end", text)


class SilverThreadSchemaLayerTest(unittest.TestCase):
    """Layer 1: scenario.schema.json contains the workflow enum and argv oneOf."""

    def test_workflow_enum_includes_silver_thread(self) -> None:
        schema = _load_schema()
        workflow_enum = schema["properties"]["workflow"]["enum"]
        self.assertIn(
            SILVER_THREAD_WORKFLOW,
            workflow_enum,
            "schema workflow enum must include silver-thread-launch-build-run",
        )

    def test_argv_allowlist_includes_silver_thread(self) -> None:
        schema = _load_schema()
        argv_schema = schema["properties"]["automation"]["properties"]["argv"]
        allowed_argv = {
            tuple(item.get("const") for item in option.get("prefixItems", []))
            for option in argv_schema.get("oneOf", [])
        }
        self.assertIn(
            EXPECTED_ARGV,
            allowed_argv,
            "schema argv oneOf must include the silver-thread Maven test command",
        )

    def test_argv_option_is_exact_length(self) -> None:
        schema = _load_schema()
        argv_schema = schema["properties"]["automation"]["properties"]["argv"]
        for option in argv_schema.get("oneOf", []):
            argv_tuple = tuple(
                item.get("const") for item in option.get("prefixItems", [])
            )
            if argv_tuple == EXPECTED_ARGV:
                size = len(option["prefixItems"])
                self.assertEqual(option.get("minItems"), size)
                self.assertEqual(option.get("maxItems"), size)
                self.assertFalse(option.get("items", True))
                return
        self.fail("Silver-thread argv option not found in schema")


class SilverThreadValidatorLayerTest(unittest.TestCase):
    """Layer 2: validate-scenarios.sh Python allowlist includes the workflow and argv."""

    def test_validator_workflow_values_include_silver_thread(self) -> None:
        text = _read(VALIDATOR_SH)
        self.assertIn(
            f'"{SILVER_THREAD_WORKFLOW}"',
            text,
            "validator workflow_values must include silver-thread-launch-build-run",
        )

    def test_validator_allowed_automation_includes_silver_thread_argv(self) -> None:
        text = _read(VALIDATOR_SH)
        self.assertIn(
            f'"-Dtest={SILVER_THREAD_TEST_CLASS}"',
            text,
            "validator allowed_automation must include SilverThreadLaunchBuildRunTest",
        )


class SilverThreadRunnerLayerTest(unittest.TestCase):
    """Layer 3: run-scenario.sh validate_allowed_automation() includes the argv."""

    def test_runner_allows_silver_thread_argv(self) -> None:
        text = _read(RUNNER_SH)
        self.assertIn(
            f"-Dtest={SILVER_THREAD_TEST_CLASS}",
            text,
            "runner validate_allowed_automation must include SilverThreadLaunchBuildRunTest",
        )

    def test_runner_checks_correct_arg_count(self) -> None:
        text = _read(RUNNER_SH)
        # The runner checks [ "$#" -eq 10 ] in the block containing SilverThread
        has_count_check = False
        lines = text.splitlines()
        for i, line in enumerate(lines):
            if "SilverThreadLaunchBuildRunTest" in line:
                # Look in a window around this line for the arg count check
                window = "\n".join(lines[max(0, i - 15):i + 5])
                if '[ "$#" -eq 10 ]' in window:
                    has_count_check = True
                    break
        self.assertTrue(
            has_count_check,
            "runner must check argc=10 for silver-thread argv block",
        )


class SilverThreadContractTestLayerTest(unittest.TestCase):
    """Layer 4: test-schema-contract.sh expected_argv includes the silver-thread tuple."""

    def test_contract_test_expected_argv_includes_silver_thread(self) -> None:
        text = _read(SCHEMA_CONTRACT_SH)
        self.assertIn(
            f"-Dtest={SILVER_THREAD_TEST_CLASS}",
            text,
            "test-schema-contract.sh expected_argv must include SilverThreadLaunchBuildRunTest",
        )

    def test_contract_test_asserts_silver_thread_workflow(self) -> None:
        text = _read(SCHEMA_CONTRACT_SH)
        self.assertIn(
            SILVER_THREAD_WORKFLOW,
            text,
            "test-schema-contract.sh must assert silver-thread-launch-build-run workflow",
        )


class SilverThreadJUnitTestExistsTest(unittest.TestCase):
    """The backing JUnit test class exists with expected test methods."""

    def test_junit_test_file_exists(self) -> None:
        self.assertTrue(
            (REPO_ROOT / JUNIT_TEST).is_file(),
            f"Missing JUnit test: {JUNIT_TEST.as_posix()}",
        )

    def test_junit_has_round_trip_test(self) -> None:
        text = _read(JUNIT_TEST)
        self.assertIn(
            "createProjectAddCommentSaveReopenExecuteAndVerifyRoundTrip",
            text,
            "JUnit test must include the 7-step round-trip test method",
        )

    def test_junit_has_starter_project_test(self) -> None:
        text = _read(JUNIT_TEST)
        self.assertIn(
            "loadRealStarterProjectInspectSaveCopyAndReopen",
            text,
            "JUnit test must include the starter project load test method",
        )

    def test_junit_uses_release_virtual_machine(self) -> None:
        text = _read(JUNIT_TEST)
        self.assertIn(
            "ReleaseVirtualMachine",
            text,
            "JUnit test must use ReleaseVirtualMachine for headless execution",
        )

    def test_junit_verifies_comment_text_survives_round_trip(self) -> None:
        text = _read(JUNIT_TEST)
        self.assertIn(
            "silver thread: student added this comment tile",
            text,
            "JUnit test must verify the comment text survives serialization",
        )

    def test_junit_verifies_vm_listener_events(self) -> None:
        text = _read(JUNIT_TEST)
        self.assertIn(
            "executing:BlockStatement",
            text,
            "JUnit test must verify VM listener captures execution events",
        )

    def test_junit_has_test_annotation(self) -> None:
        text = _read(JUNIT_TEST)
        self.assertGreaterEqual(
            text.count("@Test"),
            2,
            "JUnit test must have at least 2 @Test-annotated methods",
        )


class SilverThreadCrossLayerConsistencyTest(unittest.TestCase):
    """Cross-layer consistency: all four allowlist layers agree on the argv."""

    def test_all_layers_reference_same_test_class(self) -> None:
        for layer_path in (SCHEMA_JSON, VALIDATOR_SH, RUNNER_SH, SCHEMA_CONTRACT_SH):
            text = _read(layer_path)
            self.assertIn(
                SILVER_THREAD_TEST_CLASS,
                text,
                f"{layer_path.as_posix()} must reference {SILVER_THREAD_TEST_CLASS}",
            )

    def test_all_layers_reference_same_workflow(self) -> None:
        for layer_path in (VALIDATOR_SH, SCHEMA_CONTRACT_SH):
            text = _read(layer_path)
            self.assertIn(
                SILVER_THREAD_WORKFLOW,
                text,
                f"{layer_path.as_posix()} must reference {SILVER_THREAD_WORKFLOW}",
            )

    def test_scenario_argv_matches_expected(self) -> None:
        """The YAML argv must match the expected tuple exactly."""
        text = _read(SCENARIO_YAML)
        for element in EXPECTED_ARGV:
            self.assertIn(
                element,
                text,
                f"Scenario YAML must contain argv element: {element}",
            )


class SilverThreadEdgeCasesTest(unittest.TestCase):
    """Edge case and error handling tests."""

    def test_scenario_id_follows_naming_convention(self) -> None:
        self.assertTrue(
            SILVER_THREAD_ID.startswith("alice-desktop-"),
            "Scenario id must start with alice-desktop-",
        )
        self.assertRegex(
            SILVER_THREAD_ID,
            r"^alice-desktop-[a-z0-9-]+$",
            "Scenario id must match the schema pattern",
        )

    def test_scenario_cwd_is_repo_root(self) -> None:
        text = _read(SCENARIO_YAML)
        self.assertTrue(
            re.search(r"^\s+cwd:\s+\.$", text, re.M),
            "Scenario cwd must be repo root (.)",
        )

    def test_scenario_timeout_is_positive(self) -> None:
        text = _read(SCENARIO_YAML)
        match = re.search(r"timeoutSeconds:\s+(\d+)", text)
        self.assertIsNotNone(match, "Scenario must specify timeoutSeconds")
        self.assertGreater(int(match.group(1)), 0, "timeoutSeconds must be positive")

    def test_scenario_ready_wait_is_small(self) -> None:
        text = _read(SCENARIO_YAML)
        match = re.search(r"readyWaitSeconds:\s+(\d+)", text)
        self.assertIsNotNone(match, "Scenario must specify readyWaitSeconds")
        self.assertLessEqual(
            int(match.group(1)),
            10,
            "readyWaitSeconds for gated-command-smoke should be small (no GUI wait)",
        )

    def test_no_shell_expansion_in_argv(self) -> None:
        """Argv must not contain shell expansion characters."""
        text = _read(SCENARIO_YAML)
        # Extract lines between argv: and the next top-level key
        in_argv = False
        for line in text.splitlines():
            if line.strip().startswith("argv:"):
                in_argv = True
                continue
            if in_argv:
                if line.strip() and not line.startswith(" ") and not line.startswith("-"):
                    break
                if line.strip().startswith("- "):
                    value = line.strip()[2:]
                    self.assertNotIn(
                        "$", value, f"argv element must not use shell expansion: {value}"
                    )
                    self.assertNotIn(
                        "`", value, f"argv element must not use backticks: {value}"
                    )

    def test_supporting_evidence_references_known_scenario(self) -> None:
        text = _read(SCENARIO_YAML)
        if "supportingEvidence:" in text:
            self.assertIn(
                "alice-desktop-launch",
                text,
                "supportingEvidence should reference the alice-desktop-launch scenario",
            )


if __name__ == "__main__":
    unittest.main()
