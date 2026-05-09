"""TDD tests for PR #401 gadugi scenario integration with the merge-ready gate.

These tests define the contract for gadugi integration:
1. New gadugi files must be in the merge-ready gate's allowed diff scope
2. The gadugi YAML must structurally match the contract test expectations
3. The merge-ready gate must accept the complete PR diff including gadugi files
4. Timeout-wrapped commands must be rejected even when other evidence passes
5. Manual merge must be detected and blocked

Tests are written FIRST to specify expected behavior; failing tests indicate
implementation gaps that need fixing.
"""

import importlib.util
import sys
import unittest
from functools import lru_cache
from pathlib import Path

import yaml

REPO_ROOT = Path(__file__).resolve().parents[1]
GATE_SCRIPT = REPO_ROOT / "scripts" / "pr401-merge-ready-gate.py"
GADUGI_YAML = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "gadugi" / "window-menu-registration-evidence.yaml"
CONTRACT_TEST = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-gadugi-window-menu-contract.sh"

PR_HEAD = "c35f8c95ab72a3f5585d1222de2bb3ad49d1d18a"
BRANCH = "wave6-ui-action-menu-contract-1778302300"
JAVA_CONTRACT = "org.alice.ide.croquet.models.AliceMenuBarContractTest"

GADUGI_FILES_IN_PR = [
    "qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml",
    "qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh",
    "docs/howto/run-gadugi-window-menu-registration-evidence.md",
    "docs/reference/gadugi-window-menu-registration-evidence.md",
    "docs/tutorials/gadugi-window-menu-registration-evidence.md",
]

REQUIRED_CHECKS = [
    "build",
    "coverage",
    "package-netbeans",
    "test",
    "GitGuardian Security Checks",
]

REQUIRED_QA_COMMANDS = [
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh",
]

NO_OP_SOURCE_FINALIZATION = """\
No-op source finalization:
Current PR head: {head}
Checks: build, coverage, package-netbeans, test, and GitGuardian Security Checks are green for the current PR head; workflow-publish, pre-commit, and finalization failure areas are not failing on the current head.
Scope: focused Window menu model registration recovery only; diff remains limited to menu registration contract, bounded QA wiring, evidence docs, PR-specific gates, and directly related test metadata.
Review/finalization evidence: current-head PR metadata, focused diff review, docs impact review, and three default-workflow SEEK -> VALIDATE -> FIX cycles are refreshed; final cycle clean.
Repository source changes: none required.
"""


@lru_cache(maxsize=1)
def load_gate():
    spec = importlib.util.spec_from_file_location("pr401_merge_ready_gate", GATE_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def green_checks(head: str = PR_HEAD) -> list[dict[str, str]]:
    return [
        {"name": check, "status": "COMPLETED", "conclusion": "SUCCESS", "headSha": head}
        for check in REQUIRED_CHECKS
    ]


def valid_pr_body(head: str = PR_HEAD) -> str:
    return f"""\
Current PR head: {head}
Branch: {BRANCH}
GitHub Actions: build, coverage, package-netbeans, test, and GitGuardian Security Checks successful for the same head
Focused Java contract: {JAVA_CONTRACT}
QA/scenario evidence: validate-scenarios.sh, test-schema-contract.sh, test-workflow-contract.sh, test-gated-command-contract.sh, test-save-menu-dialog-write-proof-contract.sh, and test-silver-thread-status-report.sh
Docs impact: window menu action contract and PR #401 handoff docs reviewed for bounded claims and no committed exact-head SHA
Quality audit:
- Cycle 1 SEEK -> VALIDATE -> FIX: Java/menu contract reviewed; {JAVA_CONTRACT} passed; no fix required.
- Cycle 2 SEEK -> VALIDATE -> FIX: QA scenario/schema/runner wiring reviewed; shell contracts passed; no fix required.
- Cycle 3 SEEK -> VALIDATE -> FIX: docs, PR body, checks, and diff scope reviewed; final cycle clean; no fix required.
Diff scope: focused on UI action/menu contract recovery, QA scenario wiring, tests, evidence docs, and directly related test-package metadata
Accepted claim: Window menu model registration, stable identity, and menu-bar membership lookup only
Non-claims: no full UI automation, visible rendering correctness, grading, creative assessment, full lesson completion, full Save completion, or full Tweedle/player decode claim
NOT_MERGE_READY: none
""" + NO_OP_SOURCE_FINALIZATION.format(head=head)


def valid_context_with_gadugi_diff() -> dict:
    """Build a valid context that includes gadugi files in the diff."""
    gate = load_gate()
    all_pr_diff_files = list(gate.ALLOWED_DIFF_FILES) + GADUGI_FILES_IN_PR
    return {
        "pr_number": 401,
        "local_head": PR_HEAD,
        "pr": {
            "headRefName": BRANCH,
            "headRefOid": PR_HEAD,
            "baseRefName": "develop",
            "mergeStateStatus": "CLEAN",
            "isDraft": False,
            "statusCheckRollup": green_checks(),
        },
        "working_tree_clean": True,
        "validation": {
            "node_options": "--max-old-space-size=32768",
            "focused_maven": {
                "command": [
                    "mvn",
                    "-DincludeSims=false",
                    "-Dinstall4j.skip",
                    "-DfailIfNoTests=false",
                    "-Dsurefire.failIfNoSpecifiedTests=false",
                    "-pl",
                    "core/ide",
                    "-am",
                    f"-Dtest={JAVA_CONTRACT}",
                    "test",
                ],
                "passed": True,
                "head": PR_HEAD,
            },
            "qa_commands": [
                {"command": [command], "passed": True, "head": PR_HEAD}
                for command in REQUIRED_QA_COMMANDS
            ],
            "gated_desktop_smoke": {
                "required": False,
                "status": "not-required-for-direct-maven-recovery",
            },
        },
        "audit_cycles": [
            {
                "cycle": i,
                "seek": f"cycle {i} seek",
                "validate": f"cycle {i} validate",
                "fix": "no fix required",
                "clean": True,
            }
            for i in range(1, 4)
        ],
        "docs_impact": {
            "reviewed": True,
            "bounded_claims": True,
            "no_committed_exact_head_sha": True,
        },
        "diff_files": all_pr_diff_files,
        "pr_body": valid_pr_body(),
        "manual_merge_performed": False,
    }


class GadugiFilesInAllowedDiffTest(unittest.TestCase):
    """FAILING: gadugi files must be in the merge-ready gate's ALLOWED_DIFF_FILES."""

    def test_gadugi_yaml_is_in_allowed_diff_scope(self) -> None:
        gate = load_gate()
        self.assertIn(
            "qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml",
            gate.ALLOWED_DIFF_FILES,
        )

    def test_gadugi_contract_test_is_in_allowed_diff_scope(self) -> None:
        gate = load_gate()
        self.assertIn(
            "qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh",
            gate.ALLOWED_DIFF_FILES,
        )

    def test_gadugi_howto_doc_is_in_allowed_diff_scope(self) -> None:
        gate = load_gate()
        self.assertIn(
            "docs/howto/run-gadugi-window-menu-registration-evidence.md",
            gate.ALLOWED_DIFF_FILES,
        )

    def test_gadugi_reference_doc_is_in_allowed_diff_scope(self) -> None:
        gate = load_gate()
        self.assertIn(
            "docs/reference/gadugi-window-menu-registration-evidence.md",
            gate.ALLOWED_DIFF_FILES,
        )

    def test_gadugi_tutorial_doc_is_in_allowed_diff_scope(self) -> None:
        gate = load_gate()
        self.assertIn(
            "docs/tutorials/gadugi-window-menu-registration-evidence.md",
            gate.ALLOWED_DIFF_FILES,
        )


class GadugiDiffScopeIntegrationTest(unittest.TestCase):
    """FAILING: merge-ready gate must accept the real PR diff including gadugi files."""

    def test_gate_accepts_diff_with_gadugi_files(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()

        result = gate.evaluate_merge_readiness(context)

        self.assertTrue(result["ready"], f"Expected ready but got blockers: {result['blockers']}")
        self.assertEqual([], result["blockers"])

    def test_gate_rejects_diff_with_gadugi_files_outside_allowed_paths(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["diff_files"].append("qa/outside-in/alice-desktop/gadugi/unrelated-scenario.yaml")

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn(
            "NOT_MERGE_READY: diff scope includes unrelated files",
            result["blockers"],
        )


class GadugiYamlStructureTest(unittest.TestCase):
    """Tests that the gadugi YAML follows the required structural contract."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.scenario = yaml.safe_load(GADUGI_YAML.read_text(encoding="utf-8"))

    def test_scenario_name_matches_evidence_convention(self) -> None:
        self.assertEqual("window-menu-registration-evidence", self.scenario["name"])

    def test_scenario_delegates_to_existing_qa_only(self) -> None:
        step_commands = [
            step["params"]["command"] for step in self.scenario["steps"]
        ]
        for command in step_commands:
            self.assertTrue(
                command.startswith("qa/outside-in/alice-desktop/"),
                f"Step command {command!r} must delegate to existing QA paths",
            )

    def test_scenario_has_no_overclaiming_assertions(self) -> None:
        self.assertEqual([], self.scenario.get("assertions", []))

    def test_scenario_tags_include_gadugi_and_pr_401(self) -> None:
        tags = set(self.scenario["metadata"]["tags"])
        self.assertIn("gadugi", tags)
        self.assertIn("pr-401", tags)

    def test_scenario_description_includes_non_claims(self) -> None:
        desc = self.scenario["description"].lower()
        for non_claim in (
            "does not validate visible rendering",
            "rendered pixels",
            "visible desktop windows",
        ):
            with self.subTest(non_claim=non_claim):
                self.assertIn(non_claim, desc)

    def test_all_step_timeouts_are_positive_integers(self) -> None:
        for step in self.scenario["steps"]:
            with self.subTest(step=step["name"]):
                self.assertIsInstance(step["timeout"], int)
                self.assertGreater(step["timeout"], 0)

    def test_evidence_dir_is_unique(self) -> None:
        gadugi_dir = GADUGI_YAML.parent
        evidence_dirs = set()
        for yaml_file in gadugi_dir.glob("*.yaml"):
            scenario = yaml.safe_load(yaml_file.read_text(encoding="utf-8"))
            for step in scenario.get("steps", []):
                args = step.get("params", {}).get("args", [])
                for i, arg in enumerate(args):
                    if arg == "--evidence-dir" and i + 1 < len(args):
                        evidence_dirs.add(args[i + 1])
        self.assertEqual(
            len(evidence_dirs),
            len(list(gadugi_dir.glob("*.yaml"))),
            "Each gadugi YAML should use a unique evidence directory",
        )


class TimeoutWrapperRejectionTest(unittest.TestCase):
    """Tests that timeout-wrapped commands are always rejected by the gate."""

    def test_timeout_wrapped_focused_maven_is_rejected(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["validation"]["focused_maven"]["command"] = [
            "timeout",
            "300",
            "mvn",
            "-DincludeSims=false",
            "-Dinstall4j.skip",
            "-DfailIfNoTests=false",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "-pl",
            "core/ide",
            "-am",
            f"-Dtest={JAVA_CONTRACT}",
            "test",
        ]

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn(
            "NOT_MERGE_READY: runnable QA/scenario evidence is incomplete",
            result["blockers"],
        )

    def test_gtimeout_wrapped_qa_command_is_rejected(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["validation"]["qa_commands"][0]["command"] = [
            "gtimeout",
            "120",
            REQUIRED_QA_COMMANDS[0],
        ]

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn(
            "NOT_MERGE_READY: runnable QA/scenario evidence is incomplete",
            result["blockers"],
        )

    def test_contains_timeout_wrapper_detects_timeout(self) -> None:
        gate = load_gate()
        self.assertTrue(gate.contains_timeout_wrapper(["timeout", "60", "mvn", "test"]))

    def test_contains_timeout_wrapper_detects_gtimeout(self) -> None:
        gate = load_gate()
        self.assertTrue(gate.contains_timeout_wrapper(["gtimeout", "60", "mvn", "test"]))

    def test_contains_timeout_wrapper_allows_plain_mvn(self) -> None:
        gate = load_gate()
        self.assertFalse(gate.contains_timeout_wrapper(["mvn", "-pl", "core/ide", "test"]))

    def test_contains_timeout_wrapper_handles_empty_command(self) -> None:
        gate = load_gate()
        self.assertFalse(gate.contains_timeout_wrapper([]))


class ManualMergeDetectionTest(unittest.TestCase):
    """Tests that manual merge is detected and blocked."""

    def test_manual_merge_flag_blocks_readiness(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["manual_merge_performed"] = True

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn(
            "NOT_MERGE_READY: manual merge behavior was detected",
            result["blockers"],
        )


class AuditCycleEdgeCasesTest(unittest.TestCase):
    """Tests edge cases around audit cycle validation."""

    def test_audit_cycle_missing_seek_field_is_blocker(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["audit_cycles"][1] = {
            "cycle": 2,
            "validate": "checked",
            "fix": "none",
            "clean": True,
        }

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        blockers_text = " ".join(result["blockers"])
        self.assertIn("cycle 2 lacks seek", blockers_text)

    def test_audit_cycle_missing_validate_field_is_blocker(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["audit_cycles"][0] = {
            "cycle": 1,
            "seek": "checked",
            "fix": "none",
            "clean": True,
        }

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        blockers_text = " ".join(result["blockers"])
        self.assertIn("cycle 1 lacks validate", blockers_text)

    def test_audit_cycle_missing_fix_field_is_blocker(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["audit_cycles"][2] = {
            "cycle": 3,
            "seek": "checked",
            "validate": "checked",
            "clean": True,
        }

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        blockers_text = " ".join(result["blockers"])
        self.assertIn("cycle 3 lacks fix", blockers_text)

    def test_zero_audit_cycles_is_blocker(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["audit_cycles"] = []

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn(
            "NOT_MERGE_READY: fewer than three quality-audit cycles are documented",
            result["blockers"],
        )

    def test_non_list_audit_cycles_treated_as_empty(self) -> None:
        gate = load_gate()
        context = valid_context_with_gadugi_diff()
        context["audit_cycles"] = "three cycles were performed"

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn(
            "NOT_MERGE_READY: fewer than three quality-audit cycles are documented",
            result["blockers"],
        )


class PrBodyEdgeCasesTest(unittest.TestCase):
    """Tests edge cases around PR body validation."""

    def test_pr_body_with_non_claims_context_does_not_trigger_overclaim(self) -> None:
        gate = load_gate()
        body = valid_pr_body()
        body += "\nNon-claims: this PR does not prove full UI automation passed.\n"

        blockers = gate.validate_pr_body(body, expected_head=PR_HEAD)

        self.assertNotIn("NOT_MERGE_READY: PR body overclaims UI behavior", blockers)

    def test_pr_body_with_overclaim_outside_non_claims_is_detected(self) -> None:
        gate = load_gate()
        body = valid_pr_body().replace(
            "NOT_MERGE_READY: none",
            "This proves full UI automation. NOT_MERGE_READY: none",
        )

        blockers = gate.validate_pr_body(body, expected_head=PR_HEAD)

        self.assertIn("NOT_MERGE_READY: PR body overclaims UI behavior", blockers)

    def test_pr_body_without_unresolved_marker_is_blocker(self) -> None:
        gate = load_gate()
        body = valid_pr_body().replace("NOT_MERGE_READY: none", "All blockers resolved")

        blockers = gate.validate_pr_body(body, expected_head=PR_HEAD)

        self.assertIn("NOT_MERGE_READY: PR body records unresolved blockers", blockers)

    def test_empty_pr_body_produces_multiple_blockers(self) -> None:
        gate = load_gate()

        blockers = gate.validate_pr_body("", expected_head=PR_HEAD)

        self.assertTrue(len(blockers) >= 3, f"Expected >= 3 blockers from empty body, got {blockers}")


class ContractTestFileTest(unittest.TestCase):
    """Tests that the bash contract test file is properly configured."""

    def test_contract_test_exists_and_is_executable(self) -> None:
        self.assertTrue(CONTRACT_TEST.exists())
        import os
        self.assertTrue(os.access(CONTRACT_TEST, os.X_OK))

    def test_contract_test_references_gadugi_yaml(self) -> None:
        content = CONTRACT_TEST.read_text(encoding="utf-8")
        self.assertIn("window-menu-registration-evidence.yaml", content)

    def test_contract_test_uses_yaml_safe_load(self) -> None:
        content = CONTRACT_TEST.read_text(encoding="utf-8")
        self.assertIn("yaml.safe_load", content)
        self.assertNotIn("yaml.load(", content)

    def test_contract_test_validates_non_claims(self) -> None:
        content = CONTRACT_TEST.read_text(encoding="utf-8")
        self.assertIn("does not validate visible rendering", content)


if __name__ == "__main__":
    unittest.main()
