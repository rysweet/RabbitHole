import importlib.util
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = REPO_ROOT / "scripts" / "default_workflow_recovery.py"

HEAD_BRANCH = "wave6-run-execution-gap-1778302300"
HEAD_SHA = "a83947243fbba93cf3b1b672c9bec84da729981c"
OTHER_SHA = "0123456789abcdef0123456789abcdef01234567"

MERGE_READY_REPORT_SECTIONS = [
    "Summary",
    "Files modified",
    "Validation",
    "QA / scenario evidence",
    "Docs impact",
    "Scope / bounded claims",
    "GitHub and PR evidence",
    "Quality-audit cycles",
    "Readiness decision",
]

FOCUSED_VALIDATION_COMMANDS = [
    "git submodule update --init tweedle-lang",
    "NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh",
    (
        "NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ide -am "
        "-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false "
        "-Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest test"
    ),
]

UNSUPPORTED_CLAIMS = [
    "full UI automation",
    "visible rendering correctness",
    "grading",
    "creative assessment",
    "full lesson completion",
    "full Tweedle/player decode",
    "full world execution",
]


def load_recovery_module():
    spec = importlib.util.spec_from_file_location("default_workflow_recovery", MODULE_PATH)
    if spec is None or spec.loader is None:
        raise AssertionError(f"Could not load {MODULE_PATH.relative_to(REPO_ROOT)}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def field(record, name):
    if isinstance(record, dict):
        return record[name]
    return getattr(record, name)


def blockers(record):
    if isinstance(record, dict):
        return record.get("blockers", [])
    return getattr(record, "blockers", [])


def clean_quality_cycles():
    return [
        {
            "seek": "Verify local evidence is tied to the current PR head.",
            "validate": "Compared git rev-parse HEAD with gh pr view headRefOid.",
            "fix": "No-op; SHAs matched.",
            "clean": True,
        },
        {
            "seek": "Check bounded desktop Run claims.",
            "validate": "Reviewed docs and PR text against focused QA evidence.",
            "fix": "No-op; wording stayed bounded.",
            "clean": True,
        },
        {
            "seek": "Confirm no remaining merge-ready blockers.",
            "validate": "Checked clean worktree, green Actions, QA evidence, docs, scope, and PR text.",
            "fix": "Clean final cycle; no blockers found.",
            "clean": True,
        },
    ]


def complete_recovery_evidence():
    return {
        "head": {
            "status": "matched",
            "pr_number": 404,
            "branch": HEAD_BRANCH,
            "local_head_sha": HEAD_SHA,
            "pr_head_oid": HEAD_SHA,
            "blockers": [],
        },
        "github": {
            "status": "passed",
            "head_sha": HEAD_SHA,
            "merge_state_status": "CLEAN",
            "mergeable": "MERGEABLE",
            "workflow_runs": [
                {"name": "CI", "status": "completed", "conclusion": "success", "headSha": HEAD_SHA},
                {"name": "QA", "status": "completed", "conclusion": "success", "headSha": HEAD_SHA},
            ],
            "blockers": [],
        },
        "diff_scope": {
            "status": "focused",
            "changed_files": [
                "scripts/default_workflow_recovery.py",
                "tests/test_default_workflow_merge_ready_contract.py",
                "docs/reference/default-workflow-recovery-report.md",
            ],
            "blockers": [],
        },
        "validation": {
            "status": "passed",
            "commands": FOCUSED_VALIDATION_COMMANDS,
            "blockers": [],
        },
        "qa": {
            "status": "passed",
            "evidence": [
                "validate-scenarios.sh: passed",
                "test-run-execution-gap-contract.sh: passed",
                "EatmeDesktopRunExecutionEvidenceTest: passed",
            ],
            "blockers": [],
        },
        "docs": {
            "status": "reviewed",
            "files": [
                "docs/reference/default-workflow-recovery-report.md",
                "docs/howto/recover-pr-with-default-workflow.md",
                "docs/tutorials/trace-no-timeout-pr-recovery.md",
            ],
            "pr_description_reviewed": True,
            "blockers": [],
        },
        "quality_audit_cycles": clean_quality_cycles(),
    }


class PrHeadVerifierContractTest(unittest.TestCase):
    def test_verifies_exact_local_head_matches_current_pr_head(self) -> None:
        module = load_recovery_module()

        evidence = module.verify_current_pr_head(
            pr_number=404,
            head_branch=HEAD_BRANCH,
            local_head_sha=HEAD_SHA,
            pr_head_oid=HEAD_SHA,
        )

        self.assertEqual("matched", field(evidence, "status"))
        self.assertEqual(404, field(evidence, "pr_number"))
        self.assertEqual(HEAD_BRANCH, field(evidence, "branch"))
        self.assertEqual(HEAD_SHA, field(evidence, "local_head_sha"))
        self.assertEqual(HEAD_SHA, field(evidence, "pr_head_oid"))
        self.assertEqual([], blockers(evidence))

    def test_blocks_stale_local_head_instead_of_accepting_local_validation(self) -> None:
        module = load_recovery_module()

        evidence = module.verify_current_pr_head(
            pr_number=404,
            head_branch=HEAD_BRANCH,
            local_head_sha=OTHER_SHA,
            pr_head_oid=HEAD_SHA,
        )

        self.assertEqual("blocked", field(evidence, "status"))
        self.assertIn("NOT_MERGE_READY", "\n".join(blockers(evidence)))
        self.assertIn("local HEAD does not match current PR headRefOid", "\n".join(blockers(evidence)))
        self.assertIn(OTHER_SHA, "\n".join(blockers(evidence)))
        self.assertIn(HEAD_SHA, "\n".join(blockers(evidence)))


class GitHubCheckCollectorContractTest(unittest.TestCase):
    def test_requires_completed_successful_workflows_for_the_same_head_sha(self) -> None:
        module = load_recovery_module()

        evidence = module.collect_github_check_evidence(
            head_sha=HEAD_SHA,
            merge_state_status="CLEAN",
            mergeable="MERGEABLE",
            required_workflows=["CI", "QA"],
            workflow_runs=[
                {"name": "CI", "status": "completed", "conclusion": "success", "headSha": HEAD_SHA},
                {"name": "QA", "status": "completed", "conclusion": "success", "headSha": HEAD_SHA},
            ],
        )

        self.assertEqual("passed", field(evidence, "status"))
        self.assertEqual([], blockers(evidence))

    def test_stops_scanning_workflow_runs_after_current_head_success(self) -> None:
        module = load_recovery_module()

        class RunThatShouldNotBeClassified(dict):
            def get(self, key, default=None):
                if key == "name":
                    return "CI"
                raise AssertionError(f"unexpected classification of extra run key {key!r}")

        evidence = module.collect_github_check_evidence(
            head_sha=HEAD_SHA,
            merge_state_status="CLEAN",
            mergeable="MERGEABLE",
            required_workflows=["CI"],
            workflow_runs=[
                {"name": "CI", "status": "completed", "conclusion": "success", "headSha": HEAD_SHA},
                RunThatShouldNotBeClassified(),
            ],
        )

        self.assertEqual("passed", field(evidence, "status"))
        self.assertEqual([], blockers(evidence))

    def test_blocks_greenish_but_stale_or_incomplete_github_evidence(self) -> None:
        module = load_recovery_module()

        evidence = module.collect_github_check_evidence(
            head_sha=HEAD_SHA,
            merge_state_status="CLEAN",
            mergeable="MERGEABLE",
            required_workflows=["CI", "QA", "Docs"],
            workflow_runs=[
                {"name": "CI", "status": "completed", "conclusion": "success", "headSha": OTHER_SHA},
                {"name": "QA", "status": "in_progress", "conclusion": None, "headSha": HEAD_SHA},
            ],
        )

        joined = "\n".join(blockers(evidence))
        self.assertEqual("blocked", field(evidence, "status"))
        self.assertIn("NOT_MERGE_READY", joined)
        self.assertIn("stale", joined)
        self.assertIn("in progress", joined)
        self.assertIn("Docs", joined)


class GitHubServiceAdapterContractTest(unittest.TestCase):
    def test_gh_cli_client_retries_transient_external_failures(self) -> None:
        module = load_recovery_module()
        command_results = [
            subprocess.CompletedProcess(
                ["gh"],
                1,
                stdout="",
                stderr="temporary network failure",
            ),
            subprocess.CompletedProcess(
                ["gh"],
                0,
                stdout=f'{{"headRefOid":"{HEAD_SHA}"}}',
                stderr="",
            ),
        ]
        calls = []

        def runner(command):
            calls.append(list(command))
            return command_results.pop(0)

        client = module.GhCliClient(
            repo="rysweet/RabbitHole",
            command_runner=runner,
            max_attempts=2,
            retry_delay_seconds=0,
        )

        payload = client.pr_view(404, fields=["headRefOid"])

        self.assertEqual({"headRefOid": HEAD_SHA}, payload)
        self.assertEqual(2, len(calls))
        self.assertEqual(["gh", "pr", "view", "404"], calls[0][:4])
        self.assertNotIn("timeout", calls[0])

    def test_service_adapter_builds_head_and_github_evidence_from_client_payloads(self) -> None:
        module = load_recovery_module()

        class FakeClient:
            def pr_view(self, pr_number, fields):
                self.pr_number = pr_number
                self.pr_fields = fields
                return {
                    "headRefName": HEAD_BRANCH,
                    "headRefOid": HEAD_SHA,
                    "mergeStateStatus": "CLEAN",
                    "mergeable": "MERGEABLE",
                }

            def run_list(self, *, branch, commit, fields):
                self.run_branch = branch
                self.run_commit = commit
                self.run_fields = fields
                return [
                    {
                        "name": "CI",
                        "status": "completed",
                        "conclusion": "success",
                        "headSha": HEAD_SHA,
                    },
                    {
                        "name": "QA",
                        "status": "completed",
                        "conclusion": "success",
                        "headSha": HEAD_SHA,
                    },
                ]

        client = FakeClient()
        evidence = module.collect_github_service_evidence(
            pr_number=404,
            repo="rysweet/RabbitHole",
            local_head_sha=HEAD_SHA,
            required_workflows=["CI", "QA"],
            client=client,
        )

        self.assertEqual("matched", evidence["head"]["status"])
        self.assertEqual("passed", evidence["github"]["status"])
        self.assertEqual(HEAD_BRANCH, client.run_branch)
        self.assertEqual(HEAD_SHA, client.run_commit)
        self.assertIn("headRefOid", client.pr_fields)
        self.assertIn("headSha", client.run_fields)

    def test_service_adapter_surfaces_external_errors_as_not_merge_ready_blockers(self) -> None:
        module = load_recovery_module()

        class FailingClient:
            def pr_view(self, pr_number, fields):
                raise module.GitHubServiceError("gh pr view failed after 2 attempts")

        evidence = module.collect_github_service_evidence(
            pr_number=404,
            repo="rysweet/RabbitHole",
            local_head_sha=HEAD_SHA,
            required_workflows=["CI", "QA"],
            client=FailingClient(),
        )

        joined = "\n".join(evidence["head"]["blockers"] + evidence["github"]["blockers"])
        self.assertEqual("blocked", evidence["head"]["status"])
        self.assertEqual("blocked", evidence["github"]["status"])
        self.assertIn("NOT_MERGE_READY", joined)
        self.assertIn("GitHub PR metadata fetch failed", joined)
        self.assertIn("gh pr view failed", joined)


class DiffScopeInspectorContractTest(unittest.TestCase):
    def test_accepts_only_focused_recovery_qa_tests_and_docs_scope(self) -> None:
        module = load_recovery_module()

        evidence = module.inspect_diff_scope(
            changed_files=[
                "scripts/default_workflow_recovery.py",
                "qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh",
                "tests/test_default_workflow_merge_ready_contract.py",
                "docs/reference/default-workflow-recovery-report.md",
                "pyproject.toml",
            ]
        )

        self.assertEqual("focused", field(evidence, "status"))
        self.assertEqual([], blockers(evidence))

    def test_blocks_unrelated_diff_scope(self) -> None:
        module = load_recovery_module()

        evidence = module.inspect_diff_scope(
            changed_files=[
                "scripts/default_workflow_recovery.py",
                "core/player/src/main/java/org/alice/player/Player.java",
            ]
        )

        joined = "\n".join(blockers(evidence))
        self.assertEqual("blocked", field(evidence, "status"))
        self.assertIn("NOT_MERGE_READY", joined)
        self.assertIn("core/player/src/main/java/org/alice/player/Player.java", joined)


class QaEvidenceRunnerContractTest(unittest.TestCase):
    def test_accepts_no_timeout_focused_validation_commands_with_node_options(self) -> None:
        module = load_recovery_module()

        evidence = module.validate_recovery_commands(FOCUSED_VALIDATION_COMMANDS)

        self.assertEqual("passed", field(evidence, "status"))
        self.assertEqual([], blockers(evidence))

    def test_rejects_outer_timeout_wrappers_and_missing_node_options(self) -> None:
        module = load_recovery_module()

        evidence = module.validate_recovery_commands(
            [
                "timeout 600 qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
                "qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh",
            ]
        )

        joined = "\n".join(blockers(evidence))
        self.assertEqual("blocked", field(evidence, "status"))
        self.assertIn("NOT_MERGE_READY", joined)
        self.assertIn("timeout wrappers", joined)
        self.assertIn("NODE_OPTIONS=--max-old-space-size=32768", joined)

    def test_rejects_documented_equivalent_timeout_wrappers(self) -> None:
        module = load_recovery_module()

        evidence = module.validate_recovery_commands(
            [
                (
                    "NODE_OPTIONS=--max-old-space-size=32768 "
                    "perl -e 'alarm 600; exec @ARGV' mvn test"
                ),
                (
                    "NODE_OPTIONS=--max-old-space-size=32768 "
                    "bash -lc 'timeout 600 qa/outside-in/alice-desktop/runners/validate-scenarios.sh'"
                ),
                (
                    "NODE_OPTIONS=--max-old-space-size=32768 "
                    "bash -lc 'bash -lc \"bash -lc \\\"bash -lc timeout 600 mvn test\\\"\"'"
                ),
            ]
        )

        joined = "\n".join(blockers(evidence))
        self.assertEqual("blocked", field(evidence, "status"))
        self.assertIn("NOT_MERGE_READY", joined)
        self.assertIn("timeout wrappers", joined)
        self.assertNotIn("requires NODE_OPTIONS=--max-old-space-size=32768", joined)


class ReadinessEvaluatorContractTest(unittest.TestCase):
    def test_emits_merge_ready_only_when_every_gate_is_clean(self) -> None:
        module = load_recovery_module()

        decision = module.evaluate_readiness(complete_recovery_evidence())

        self.assertEqual("MERGE_READY", field(decision, "decision"))
        self.assertEqual([], blockers(decision))

    def test_missing_qa_and_too_few_quality_cycles_force_not_merge_ready(self) -> None:
        module = load_recovery_module()
        evidence = complete_recovery_evidence()
        evidence["qa"] = {"status": "missing", "evidence": [], "blockers": []}
        evidence["quality_audit_cycles"] = clean_quality_cycles()[:2]

        decision = module.evaluate_readiness(evidence)

        joined = "\n".join(blockers(decision))
        self.assertEqual("NOT_MERGE_READY", field(decision, "decision"))
        self.assertIn("runnable QA", joined)
        self.assertIn("fewer than three quality-audit cycles", joined)

    def test_dirty_final_quality_cycle_forces_not_merge_ready(self) -> None:
        module = load_recovery_module()
        evidence = complete_recovery_evidence()
        evidence["quality_audit_cycles"][-1] = {
            "seek": "Confirm no remaining merge-ready blockers.",
            "validate": "Checked GitHub Actions.",
            "fix": "Blocked; QA evidence was not run.",
            "clean": False,
        }

        decision = module.evaluate_readiness(evidence)

        self.assertEqual("NOT_MERGE_READY", field(decision, "decision"))
        self.assertIn("final quality-audit cycle is not clean", "\n".join(blockers(decision)))


class MergeReadyRecoveryReportIntegrationContractTest(unittest.TestCase):
    def test_renders_expanded_report_sections_with_no_op_justification_and_blockers(self) -> None:
        module = load_recovery_module()
        evidence = complete_recovery_evidence()
        evidence["github"]["status"] = "blocked"
        evidence["github"]["blockers"] = [
            "NOT_MERGE_READY: GitHub Actions are in progress for current head."
        ]

        report = module.render_merge_ready_recovery_report(
            summary=(
                "No repository implementation changes were required after evaluating "
                "the current PR head."
            ),
            files_modified=[],
            evidence=evidence,
        )

        positions = [report.index(section) for section in MERGE_READY_REPORT_SECTIONS]
        self.assertEqual(sorted(positions), positions)
        self.assertRegex(report, r"(?m)^Files modified\nNone\n")
        self.assertIn("workflow-accepted No-op justification", report)
        self.assertIn(HEAD_SHA, report)
        self.assertIn("GitHub Actions are in progress", report)
        self.assertIn("NOT_MERGE_READY", report)
        self.assertNotRegex(report, r"(?m)^MERGE_READY$")

    def test_rejects_report_that_overclaims_unproven_ui_or_execution_behavior(self) -> None:
        module = load_recovery_module()
        error_type = getattr(module, "WorkflowReportError")
        evidence = complete_recovery_evidence()
        evidence["scope_bounded_claims"] = "This proves full UI automation and visible rendering correctness."

        with self.assertRaises(error_type) as raised:
            module.render_merge_ready_recovery_report(
                summary="Recovered PR #404.",
                files_modified=[],
                evidence=evidence,
            )

        message = str(raised.exception)
        for unsupported_claim in UNSUPPORTED_CLAIMS[:2]:
            self.assertIn(unsupported_claim, message)


if __name__ == "__main__":
    unittest.main()
