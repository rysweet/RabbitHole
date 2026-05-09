import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
GATE_PATH = REPO_ROOT / "scripts" / "pr430_merge_ready_gate.py"
PR_BRANCH = "feat/issue-407-rabbithole-wave7-save-negative-contract-lane-follo"
HEAD_SHA = "2f6cd9a53ad4d319f2e4e64ca0f92da0299549cf"
NEGATIVE_CONTRACT_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 "
    "bash qa/outside-in/alice-desktop/tests/"
    "test-save-menu-dialog-negative-artifact-contract.sh"
)
PYTHON_TEST_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests"
)


def load_gate_module():
    spec = importlib.util.spec_from_file_location("pr430_merge_ready_gate", GATE_PATH)
    assert spec is not None
    assert spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def passing_evidence() -> dict:
    return {
        "pr": {
            "number": 430,
            "branch": PR_BRANCH,
            "remote_head_sha": HEAD_SHA,
            "evaluated_head_sha": HEAD_SHA,
            "manual_merge": False,
        },
        "workflow": {
            "owner_exit_classification": "NO_OP_GUARD",
            "no_timeout_wrappers": True,
        },
        "diff": {
            "files_modified": [],
            "changed_files": [
                "alice_qa_amplihack.py",
                "docs/howto/run-save-menu-dialog-negative-artifact-contract.md",
                "docs/reference/save-menu-dialog-negative-artifact-contract.md",
                "pyproject.toml",
                "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
                "tests/test_alice_qa_amplihack_docs_contract.py",
            ],
        },
        "qa": {
            "runnable_evidence": [
                {"command": PYTHON_TEST_COMMAND, "status": "passed"},
                {"command": NEGATIVE_CONTRACT_COMMAND, "status": "passed"},
            ],
            "scenario_evidence": {
                "applicable": True,
                "command": NEGATIVE_CONTRACT_COMMAND,
                "status": "passed",
                "claim": "validator rejects invalid Save proof evidence only",
            },
        },
        "docs": {
            "impact_assessed": True,
            "changed": True,
            "bounded_language": True,
            "unsupported_claims": [],
        },
        "quality_audit_cycles": [
            {
                "cycle": 1,
                "seek": "contract behavior",
                "validate": "negative fixtures and validator diagnostics",
                "fix": "no actionable issue",
                "status": "clean",
            },
            {
                "cycle": 2,
                "seek": "evidence wording and documentation claims",
                "validate": "bounded language and non-claims",
                "fix": "no actionable issue",
                "status": "clean",
            },
            {
                "cycle": 3,
                "seek": "full diff and readiness state",
                "validate": "no unresolved blockers",
                "fix": "no actionable issue",
                "status": "clean",
            },
        ],
        "github_actions": {
            "head_sha": HEAD_SHA,
            "checks": [
                {"name": "alice desktop QA", "status": "completed", "conclusion": "success"},
                {"name": "python maintenance tests", "status": "completed", "conclusion": "success"},
            ],
        },
        "pr_description": {
            "head_sha": HEAD_SHA,
            "contains_current_evidence": True,
            "mentions_docs_impact": True,
            "mentions_three_audit_cycles": True,
            "mentions_green_actions": True,
            "bounded_non_claims": True,
        },
        "claims": ["negative contract evidence proves invalid Save proof artifacts fail closed"],
        "no_op": {
            "accepted": True,
            "justification": "Current remote PR head was evaluated and all merge-ready gates have evidence.",
        },
    }


class Pr430MergeReadyGateTest(unittest.TestCase):
    def evaluate(self, evidence: dict) -> dict:
        result = load_gate_module().evaluate_merge_readiness(evidence)
        self.assertIsInstance(result, dict)
        self.assertIn("status", result)
        self.assertIn("ready", result)
        self.assertIn("blockers", result)
        return result

    def assert_not_merge_ready_contains(self, result: dict, *patterns: str) -> None:
        self.assertFalse(result["ready"])
        self.assertEqual("NOT_MERGE_READY", result["status"])
        blockers = "\n".join(result["blockers"])
        for pattern in patterns:
            with self.subTest(pattern=pattern):
                self.assertRegex(blockers, pattern)

    def test_accepts_evidence_backed_no_op_when_all_gates_pass(self) -> None:
        result = self.evaluate(passing_evidence())

        self.assertTrue(result["ready"])
        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])
        self.assertEqual([], result["files_modified"])
        self.assertRegex(result["no_op_justification"], r"current remote PR head|all merge-ready gates")

    def test_missing_or_stale_remote_pr_head_blocks_ready(self) -> None:
        evidence = passing_evidence()
        evidence["pr"]["evaluated_head_sha"] = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

        self.assert_not_merge_ready_contains(
            self.evaluate(evidence),
            r"remote PR head|head SHA|stale",
        )

    def test_missing_required_runnable_qa_evidence_blocks_ready(self) -> None:
        evidence = passing_evidence()
        evidence["qa"]["runnable_evidence"] = [{"command": PYTHON_TEST_COMMAND, "status": "passed"}]

        self.assert_not_merge_ready_contains(
            self.evaluate(evidence),
            r"Save menu dialog negative artifact contract|negative.*contract",
            r"runnable QA|scenario evidence",
        )

    def test_three_quality_audit_cycles_are_required_with_clean_final_cycle(self) -> None:
        too_few_cycles = passing_evidence()
        too_few_cycles["quality_audit_cycles"] = too_few_cycles["quality_audit_cycles"][:2]
        final_cycle_dirty = passing_evidence()
        final_cycle_dirty["quality_audit_cycles"][2] = {
            "cycle": 3,
            "seek": "full diff and readiness state",
            "validate": "found unresolved PR description evidence gap",
            "fix": "pending",
            "status": "needs_fix",
        }

        for evidence in (too_few_cycles, final_cycle_dirty):
            with self.subTest(cycles=evidence["quality_audit_cycles"]):
                self.assert_not_merge_ready_contains(
                    self.evaluate(evidence),
                    r"Three quality-audit|SEEK.*VALIDATE.*FIX",
                    r"final cycle.*clean|clean final cycle",
                )

    def test_green_actions_alone_do_not_bypass_docs_audit_or_description_gates(self) -> None:
        evidence = passing_evidence()
        evidence["docs"]["impact_assessed"] = False
        evidence["quality_audit_cycles"] = []
        evidence["pr_description"]["contains_current_evidence"] = False
        evidence["pr_description"]["mentions_docs_impact"] = False

        self.assert_not_merge_ready_contains(
            self.evaluate(evidence),
            r"docs impact|Documentation impact",
            r"quality-audit|SEEK",
            r"PR description|pull request description",
        )

    def test_unfocused_diff_or_unsupported_claims_are_blockers(self) -> None:
        evidence = passing_evidence()
        evidence["diff"]["changed_files"].append("core/ide/src/main/java/org/alice/ide/Unrelated.java")
        evidence["docs"]["unsupported_claims"] = ["full UI automation"]
        evidence["claims"].append("negative contract proves full UI automation and visible rendering correctness")

        self.assert_not_merge_ready_contains(
            self.evaluate(evidence),
            r"focused diff|diff scope",
            r"unsupported claim|unbounded claim|full UI automation|visible rendering",
        )

    def test_malformed_diff_paths_cannot_bypass_focused_scope_gate(self) -> None:
        malformed_paths = [
            "tests/../core/ide/src/main/java/org/alice/ide/Unrelated.java",
            "tests//test_evil.py",
            "tests/./test_evil.py",
            "/tests/test_evil.py",
            "tests\\test_evil.py",
            "tests/test_evil.py\x00",
            "scripts/pr430_merge_ready_gate.py/extra",
        ]

        for path in malformed_paths:
            evidence = passing_evidence()
            evidence["diff"]["changed_files"] = [path]
            with self.subTest(path=path):
                self.assert_not_merge_ready_contains(
                    self.evaluate(evidence),
                    r"focused diff|diff scope",
                    r"malformed|out-of-scope",
                )

    def test_valid_diff_paths_still_pass_focused_scope_gate(self) -> None:
        evidence = passing_evidence()
        evidence["diff"]["changed_files"] = [
            "alice_qa_amplihack.py",
            "docs/reference/save-menu-dialog-negative-artifact-contract.md",
            "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
            "scripts/pr430_merge_ready_gate.py",
            "tests/test_pr430_merge_ready_gate.py",
        ]

        result = self.evaluate(evidence)

        self.assertTrue(result["ready"])
        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])

    def test_pending_or_failed_github_action_for_current_head_blocks_ready(self) -> None:
        for status, conclusion in (("in_progress", None), ("completed", "failure")):
            evidence = passing_evidence()
            evidence["github_actions"]["checks"].append(
                {"name": "required gate", "status": status, "conclusion": conclusion}
            )
            with self.subTest(status=status, conclusion=conclusion):
                self.assert_not_merge_ready_contains(
                    self.evaluate(evidence),
                    r"GitHub Actions|checks",
                    r"green|complete",
                )

    def test_cli_reads_evidence_json_and_prints_not_merge_ready_blockers(self) -> None:
        evidence = passing_evidence()
        evidence["qa"]["scenario_evidence"]["status"] = "missing"

        with tempfile.TemporaryDirectory() as temp_dir:
            evidence_path = Path(temp_dir) / "evidence.json"
            evidence_path.write_text(json.dumps(evidence), encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(GATE_PATH), "--evidence", str(evidence_path)],
                cwd=REPO_ROOT,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(1, result.returncode, result.stderr)
        self.assert_not_merge_ready_contains(json.loads(result.stdout), r"scenario evidence|runnable QA")

    def test_cli_rejects_malformed_evidence_without_success_shaped_output(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            evidence_path = Path(temp_dir) / "evidence.json"
            evidence_path.write_text("{not valid json", encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(GATE_PATH), "--evidence", str(evidence_path)],
                cwd=REPO_ROOT,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(2, result.returncode)
        self.assertEqual("", result.stdout)
        self.assertRegex(result.stderr, r"invalid evidence JSON|malformed evidence")

    def test_refresh_github_evidence_maps_read_only_pr_service_payload(self) -> None:
        module = load_gate_module()
        body = "\n".join(
            [
                f"Current evidence for {HEAD_SHA}",
                PYTHON_TEST_COMMAND,
                NEGATIVE_CONTRACT_COMMAND,
                "Documentation impact assessed.",
                "Three quality-audit cycles completed.",
                "GitHub Actions checks are green.",
                "Does not prove full UI automation or visible rendering correctness.",
            ]
        )
        payload = {
            "number": 430,
            "headRefName": PR_BRANCH,
            "headRefOid": HEAD_SHA,
            "body": body,
            "files": [
                {"path": "alice_qa_amplihack.py"},
                {"path": "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh"},
            ],
            "statusCheckRollup": [
                {
                    "name": "build",
                    "status": "COMPLETED",
                    "conclusion": "SUCCESS",
                    "workflowName": "Alice Test CI",
                },
                {
                    "name": "pending gate",
                    "status": "IN_PROGRESS",
                    "conclusion": None,
                    "workflowName": "Alice Test CI",
                },
            ],
        }
        calls = []

        def fake_runner(command: list[str]) -> subprocess.CompletedProcess[str]:
            calls.append(command)
            return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")

        evidence = passing_evidence()
        evidence["pr"]["remote_head_sha"] = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        evidence["diff"]["changed_files"] = []
        evidence["github_actions"]["checks"] = []
        evidence["pr_description"]["contains_current_evidence"] = False

        refreshed = module.refresh_github_evidence(
            evidence,
            module.GitHubCliClient(runner=fake_runner, max_attempts=1, retry_delay_seconds=0),
        )

        self.assertEqual([["gh", "pr", "view", "430", "--json", module.GITHUB_PR_VIEW_FIELDS]], calls)
        self.assertEqual(HEAD_SHA, refreshed["pr"]["remote_head_sha"])
        self.assertEqual(HEAD_SHA, refreshed["pr"]["evaluated_head_sha"])
        self.assertEqual(PR_BRANCH, refreshed["pr"]["branch"])
        self.assertEqual(
            [
                "alice_qa_amplihack.py",
                "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
            ],
            refreshed["diff"]["changed_files"],
        )
        self.assertEqual(
            {"name": "build", "status": "completed", "conclusion": "success"},
            refreshed["github_actions"]["checks"][0],
        )
        self.assertEqual(
            {"name": "pending gate", "status": "in_progress", "conclusion": None},
            refreshed["github_actions"]["checks"][1],
        )
        self.assertTrue(refreshed["pr_description"]["contains_current_evidence"])
        self.assertTrue(refreshed["pr_description"]["mentions_docs_impact"])
        self.assertTrue(refreshed["pr_description"]["mentions_three_audit_cycles"])
        self.assertTrue(refreshed["pr_description"]["mentions_green_actions"])
        self.assertTrue(refreshed["pr_description"]["bounded_non_claims"])

    def test_github_client_retries_transient_failures_and_surfaces_permanent_errors(self) -> None:
        module = load_gate_module()
        payload = {
            "number": 430,
            "headRefName": PR_BRANCH,
            "headRefOid": HEAD_SHA,
            "body": "",
            "files": [],
            "statusCheckRollup": [],
        }
        transient_calls = []

        def transient_runner(command: list[str]) -> subprocess.CompletedProcess[str]:
            transient_calls.append(command)
            if len(transient_calls) == 1:
                return subprocess.CompletedProcess(command, 1, "", "temporary rate limit")
            return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")

        retrying_client = module.GitHubCliClient(
            runner=transient_runner,
            max_attempts=2,
            retry_delay_seconds=0,
        )

        self.assertEqual(HEAD_SHA, retrying_client.fetch_pr_evidence()["pr"]["remote_head_sha"])
        self.assertEqual(2, len(transient_calls))

        def permanent_runner(command: list[str]) -> subprocess.CompletedProcess[str]:
            return subprocess.CompletedProcess(command, 1, "", "authentication failed")

        failing_client = module.GitHubCliClient(
            runner=permanent_runner,
            max_attempts=3,
            retry_delay_seconds=0,
        )

        with self.assertRaisesRegex(module.ExternalServiceError, r"authentication failed"):
            failing_client.fetch_pr_evidence()


if __name__ == "__main__":
    unittest.main()
