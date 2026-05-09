import contextlib
import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import unittest
from functools import lru_cache
from pathlib import Path
from unittest.mock import patch


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "pr401-merge-ready-gate.py"

PR_HEAD = "a5f0c12f4aa1d0f49e277110d0d4bbafab01eee8"
STALE_HEAD = "479fab8b963c18839c7da4c26bf366064d325d54"
BRANCH = "wave6-ui-action-menu-contract-1778302300"
JAVA_CONTRACT = "org.alice.ide.croquet.models.AliceMenuBarContractTest"
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
ALLOWED_DIFF = [
    "core/ide/src/test/java/org/alice/ide/croquet/models/AliceMenuBarContractTest.java",
    "qa/outside-in/alice-desktop/scenarios/menu-action-smoke.yaml",
    "qa/outside-in/alice-desktop/schema/scenario.schema.json",
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/runners/run-scenario.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh",
    "docs/reference/pr401-ui-action-menu-contract-evidence.md",
    "docs/reference/window-menu-action-contract.md",
    "docs/index.md",
    "tests/test_pr401_ui_action_menu_contract_evidence.py",
    "pyproject.toml",
]


@lru_cache(maxsize=1)
def load_gate():
    if not SCRIPT_PATH.exists():
        raise AssertionError(f"Expected PR #401 merge-ready gate implementation at {SCRIPT_PATH}")
    spec = importlib.util.spec_from_file_location("pr401_merge_ready_gate", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def green_checks(head: str = PR_HEAD) -> list[dict[str, str]]:
    return [
        {
            "name": check,
            "status": "COMPLETED",
            "conclusion": "SUCCESS",
            "headSha": head,
        }
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
"""


def valid_context() -> dict:
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
                "cycle": 1,
                "seek": "Java/menu contract registration and UUID drift",
                "validate": f"same-head {JAVA_CONTRACT} passed",
                "fix": "no fix required",
                "clean": True,
            },
            {
                "cycle": 2,
                "seek": "QA scenario/schema/runner argv drift",
                "validate": "scenario validator and shell contracts passed",
                "fix": "no fix required",
                "clean": True,
            },
            {
                "cycle": 3,
                "seek": "docs, PR body, check metadata, and diff-scope drift",
                "validate": "same-head checks green and docs bounded",
                "fix": "no fix required",
                "clean": True,
            },
        ],
        "docs_impact": {
            "reviewed": True,
            "bounded_claims": True,
            "no_committed_exact_head_sha": True,
        },
        "diff_files": ALLOWED_DIFF,
        "pr_body": valid_pr_body(),
        "manual_merge_performed": False,
    }


class Pr401MergeReadyGateTest(unittest.TestCase):
    def test_merge_ready_requires_same_branch_head_green_checks_and_all_evidence(self) -> None:
        gate = load_gate()

        result = gate.evaluate_merge_readiness(valid_context())

        self.assertTrue(result["ready"], result)
        self.assertEqual([], result["blockers"])
        self.assertIn("Window menu model registration", result["accepted_claim"])
        self.assertNotIn("full UI automation", result["accepted_claim"].lower())

    def test_head_or_check_mismatch_is_not_merge_ready_even_when_local_tests_pass(self) -> None:
        gate = load_gate()
        context = valid_context()
        context["local_head"] = STALE_HEAD
        context["pr"]["statusCheckRollup"] = green_checks(STALE_HEAD)

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: local HEAD does not match PR headRefOid", result["blockers"])
        self.assertIn("NOT_MERGE_READY: GitHub Actions are not green for the current PR head", result["blockers"])

    def test_successful_required_check_without_head_sha_is_not_same_head_evidence(self) -> None:
        gate = load_gate()
        context = valid_context()
        del context["pr"]["statusCheckRollup"][0]["headSha"]

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: GitHub Actions are not green for the current PR head", result["blockers"])

    def test_validation_requires_exact_focused_maven_command(self) -> None:
        gate = load_gate()
        context = valid_context()
        context["validation"]["focused_maven"]["command"] = [
            "mvn",
            "-pl",
            "core/ide",
            "-am",
            f"-Dtest={JAVA_CONTRACT}",
            "help:effective-pom",
        ]

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: runnable QA/scenario evidence is incomplete", result["blockers"])

    def test_validation_rejects_qa_command_argument_containment(self) -> None:
        gate = load_gate()
        context = valid_context()
        context["validation"]["qa_commands"][0]["command"] = ["echo", REQUIRED_QA_COMMANDS[0]]

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: runnable QA/scenario evidence is incomplete", result["blockers"])

    def test_missing_runnable_qa_docs_diff_or_audit_cycle_creates_explicit_blockers(self) -> None:
        gate = load_gate()
        context = valid_context()
        context["validation"]["qa_commands"] = context["validation"]["qa_commands"][:2]
        context["docs_impact"]["reviewed"] = False
        context["diff_files"].append("core/ide/src/main/java/org/alice/ide/UnrelatedBehavior.java")
        context["audit_cycles"] = context["audit_cycles"][:2]

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: runnable QA/scenario evidence is incomplete", result["blockers"])
        self.assertIn("NOT_MERGE_READY: docs impact review is incomplete", result["blockers"])
        self.assertIn("NOT_MERGE_READY: fewer than three quality-audit cycles are documented", result["blockers"])
        self.assertIn("NOT_MERGE_READY: diff scope includes unrelated files", result["blockers"])

    def test_pr_body_must_record_current_head_audit_cycles_bounded_claims_and_blockers(self) -> None:
        gate = load_gate()
        stale_body = valid_pr_body(STALE_HEAD).replace(
            "NOT_MERGE_READY: none",
            "PR #401 proves full UI automation and visible rendering correctness.",
        )

        blockers = gate.validate_pr_body(stale_body, expected_head=PR_HEAD)

        self.assertIn("NOT_MERGE_READY: PR body lacks current-head evidence", blockers)
        self.assertIn("NOT_MERGE_READY: PR body contains stale head evidence", blockers)
        self.assertIn("NOT_MERGE_READY: PR body overclaims UI behavior", blockers)

    def test_draft_unclean_merge_state_or_dirty_worktree_blocks_readiness(self) -> None:
        gate = load_gate()
        context = valid_context()
        context["pr"]["isDraft"] = True
        context["pr"]["mergeStateStatus"] = "DIRTY"
        context["working_tree_clean"] = False

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: PR is still marked draft", result["blockers"])
        self.assertIn("NOT_MERGE_READY: PR merge state is not clean", result["blockers"])
        self.assertIn(
            "NOT_MERGE_READY: local working tree has uncommitted changes",
            result["blockers"],
        )

    def test_final_quality_audit_cycle_is_the_last_documented_cycle(self) -> None:
        gate = load_gate()
        context = valid_context()
        context["audit_cycles"].append(
            {
                "cycle": 4,
                "seek": "final same-head verification after PR body update",
                "validate": "remote PR metadata and local evidence rechecked",
                "fix": "follow-up blocker remains unresolved",
                "clean": False,
            }
        )

        result = gate.evaluate_merge_readiness(context)

        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: quality-audit cycle 4 is not clean", result["blockers"])
        self.assertIn("NOT_MERGE_READY: final quality-audit cycle is not clean", result["blockers"])

    def test_live_runtime_context_is_metadata_only_without_complete_evidence(self) -> None:
        gate = load_gate()
        pr = {
            "headRefName": BRANCH,
            "headRefOid": PR_HEAD,
            "baseRefName": "develop",
            "mergeStateStatus": "CLEAN",
            "isDraft": False,
            "statusCheckRollup": green_checks(),
            "body": valid_pr_body(),
        }

        def fake_run_text(command: list[str]) -> str:
            if command[:2] == ["git", "rev-parse"]:
                return PR_HEAD
            if command[:2] == ["git", "status"]:
                return ""
            if command[:3] == ["git", "diff", "--name-only"]:
                return "\n".join(ALLOWED_DIFF)
            raise AssertionError(f"Unexpected command: {command}")

        with patch.object(gate, "_run_json", return_value=pr), patch.object(
            gate, "_run_text", side_effect=fake_run_text
        ):
            context = gate.build_runtime_context(pr_number=401, base_ref="develop")

        result = gate.evaluate_merge_readiness(context)

        self.assertEqual("live-metadata-only", context["context_source"])
        self.assertFalse(result["ready"])
        self.assertIn("NOT_MERGE_READY: runnable QA/scenario evidence is incomplete", result["blockers"])
        self.assertIn("NOT_MERGE_READY: docs impact review is incomplete", result["blockers"])
        self.assertIn("NOT_MERGE_READY: fewer than three quality-audit cycles are documented", result["blockers"])

    def test_live_runtime_context_subprocess_failure_returns_blocker(self) -> None:
        gate = load_gate()
        stdout = io.StringIO()
        failure = subprocess.CalledProcessError(
            1,
            ["gh", "pr", "view", "401"],
            stderr="authentication required",
        )

        with patch.object(gate, "_run_text", side_effect=failure), contextlib.redirect_stdout(stdout):
            exit_code = gate.main(["--json"])

        self.assertEqual(1, exit_code)
        payload = json.loads(stdout.getvalue())
        self.assertFalse(payload["ready"])
        self.assertEqual(1, len(payload["blockers"]))
        self.assertIn(
            "NOT_MERGE_READY: unable to collect merge-ready context",
            payload["blockers"][0],
        )
        self.assertIn("gh pr view 401", payload["blockers"][0])
        self.assertIn("authentication required", payload["blockers"][0])

    def test_live_runtime_context_invalid_json_returns_blocker(self) -> None:
        gate = load_gate()
        stdout = io.StringIO()

        with patch.object(gate, "_run_text", return_value="not json"), contextlib.redirect_stdout(stdout):
            exit_code = gate.main(["--json"])

        self.assertEqual(1, exit_code)
        payload = json.loads(stdout.getvalue())
        self.assertFalse(payload["ready"])
        self.assertEqual(1, len(payload["blockers"]))
        self.assertIn(
            "NOT_MERGE_READY: unable to collect merge-ready context",
            payload["blockers"][0],
        )
        self.assertIn("invalid JSON", payload["blockers"][0])

    def test_malformed_context_json_returns_blocker(self) -> None:
        gate = load_gate()
        stdout = io.StringIO()

        with tempfile.TemporaryDirectory() as tmp_dir:
            context_path = Path(tmp_dir) / "merge-ready-context.json"
            context_path.write_text("not json", encoding="utf-8")

            with contextlib.redirect_stdout(stdout):
                exit_code = gate.main(["--json", "--context-json", str(context_path)])

        self.assertEqual(1, exit_code)
        payload = json.loads(stdout.getvalue())
        self.assertFalse(payload["ready"])
        self.assertEqual(1, len(payload["blockers"]))
        self.assertIn(
            "NOT_MERGE_READY: unable to collect merge-ready context",
            payload["blockers"][0],
        )
        self.assertIn("invalid JSON", payload["blockers"][0])

    def test_missing_context_json_returns_blocker(self) -> None:
        gate = load_gate()
        stdout = io.StringIO()

        with tempfile.TemporaryDirectory() as tmp_dir, contextlib.redirect_stdout(stdout):
            exit_code = gate.main(
                ["--json", "--context-json", str(Path(tmp_dir) / "missing-context.json")]
            )

        self.assertEqual(1, exit_code)
        payload = json.loads(stdout.getvalue())
        self.assertFalse(payload["ready"])
        self.assertEqual(1, len(payload["blockers"]))
        self.assertIn(
            "NOT_MERGE_READY: unable to collect merge-ready context",
            payload["blockers"][0],
        )
        self.assertIn("missing-context.json", payload["blockers"][0])

    def test_validation_plan_uses_node_options_and_no_timeout_wrappers(self) -> None:
        gate = load_gate()

        plan = gate.validation_plan()
        expected_focused_command, expected_qa_commands = gate.expected_validation_commands()

        rendered_commands = [" ".join(item["command"]) for item in plan]
        self.assertTrue(any(JAVA_CONTRACT in command for command in rendered_commands))
        for required in REQUIRED_QA_COMMANDS:
            with self.subTest(required=required):
                self.assertTrue(any(required in command for command in rendered_commands))
                self.assertIn((required,), expected_qa_commands)
        self.assertEqual(tuple(plan[0]["command"]), expected_focused_command)

        for item in plan:
            with self.subTest(command=item["command"]):
                self.assertEqual("--max-old-space-size=32768", item["env"]["NODE_OPTIONS"])
                self.assertFalse(gate.contains_timeout_wrapper(item["command"]))
                self.assertNotIn("timeout", item["command"][0])
                self.assertNotIn("gtimeout", item["command"][0])


if __name__ == "__main__":
    unittest.main()
