import copy
import importlib.util
import json
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RECOVERY_GATE_PATH = REPO_ROOT / "scripts" / "pr463_recovery_gate.py"
EXPECTED_PR = 463
EXPECTED_BRANCH = "feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr"
EXPECTED_HEAD_SHA = "fb6e8468ece394e35b30b68211023f3080d872d5"

REQUIRED_CHECKS = [
    "Alice Coverage Reports/coverage",
    "package-netbeans",
    "test",
    "build",
    "GitGuardian Security Checks",
]

FOCUSED_REPAIR_PATHS = [
    "scripts/pr463_recovery_gate.py",
    "tests/test_pr463_owner_free_recovery_gate.py",
    "docs/reference/player-archive-unsupported-tweedle-diagnostics.md",
    "docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md",
    "docs/tutorials/player-archive-unsupported-this-call-diagnostic.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml",
    "qa/outside-in/alice-desktop/runners/run-scenario.sh",
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/schema/scenario.schema.json",
    "core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java",
    "core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java",
    "tests/test_pr463_archive_player_boundary_contract.py",
    "pyproject.toml",
]

ARCHIVE_SMOKE_ARGV = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/story-api-migration",
    "-am",
    "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest",
    "test",
]

TWEEDLE_SMOKE_ARGV = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ast",
    "-am",
    (
        "-Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#"
        "zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+"
        "zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+"
        "zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+"
        "zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsChainedCall"
    ),
    "test",
]


def load_recovery_gate():
    if not RECOVERY_GATE_PATH.exists():
        raise AssertionError(
            "scripts/pr463_recovery_gate.py must implement the PR #463 owner-free recovery gate."
        )
    spec = importlib.util.spec_from_file_location("pr463_recovery_gate", RECOVERY_GATE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def completed_check(name: str, conclusion: str = "success") -> dict:
    return {"name": name, "status": "COMPLETED", "conclusion": conclusion.upper()}


def github_pr_view_payload(checks: list[dict] | None = None) -> dict:
    return {
        "url": "https://github.com/rysweet/RabbitHole/pull/463",
        "baseRefName": "develop",
        "headRefName": EXPECTED_BRANCH,
        "headRefOid": EXPECTED_HEAD_SHA,
        "mergeable": "MERGEABLE",
        "mergeStateStatus": "CLEAN",
        "statusCheckRollup": checks or [
            {"name": "coverage", "status": "COMPLETED", "conclusion": "SUCCESS"}
        ],
    }


def github_success_result(command, checks: list[dict] | None = None) -> subprocess.CompletedProcess:
    return subprocess.CompletedProcess(
        command,
        0,
        stdout=json.dumps(github_pr_view_payload(checks)),
        stderr="",
    )


def boundary_evidence() -> dict:
    return {
        "headSha": EXPECTED_HEAD_SHA,
        "referenceDocCurrent": True,
        "howtoCurrent": True,
        "tutorialCurrent": True,
        "archiveScenarioCurrent": True,
        "tweedleScenarioCurrent": True,
        "characterizationTestsCurrent": True,
        "forbiddenClaims": [],
        "nonclaims": [
            "full Tweedle/player decode",
            "historical archive migration completeness",
            "full UI automation",
            "visible rendering correctness",
            "grading",
            "Save/Open guarantees",
            "lesson completion",
        ],
        "generatedArchivesCommitted": [],
        "binaryCorpusPayloadsCommitted": [],
    }


def qa_scenario_contracts() -> dict:
    return {
        "validated": True,
        "workflows": {
            "archive-fixture-smoke": workflow_contract(ARCHIVE_SMOKE_ARGV),
            "tweedle-decoder-boundary-smoke": workflow_contract(TWEEDLE_SMOKE_ARGV),
        },
    }


def workflow_contract(argv: list[str]) -> dict:
    return {
        "allowlistedInValidator": True,
        "allowlistedInRunner": True,
        "listedInSchema": True,
        "argv": argv,
        "automationMode": "gated-command-smoke",
    }


def merge_ready_evidence() -> dict:
    return {
        "repository": "rysweet/RabbitHole",
        "prNumber": EXPECTED_PR,
        "branch": EXPECTED_BRANCH,
        "baseRef": "develop",
        "headSha": EXPECTED_HEAD_SHA,
        "localHeadSha": EXPECTED_HEAD_SHA,
        "prHeadSha": EXPECTED_HEAD_SHA,
        "worktreeClean": True,
        "mergeable": "MERGEABLE",
        "mergeStateStatus": "CLEAN",
        "manualMergeUsed": False,
        "noOpJustificationUsed": True,
        "repairRequired": False,
        "pushedRepair": False,
        "commands": [
            ["git", "--no-pager", "status", "--short"],
            ["gh", "pr", "view", "463", "--json", "headRefOid,mergeable,mergeStateStatus,statusCheckRollup"],
            ["git", "submodule", "update", "--init", "tweedle-lang"],
            ["python3", "-m", "unittest", "tests.test_pr463_archive_player_boundary_contract"],
            ["bash", "qa/outside-in/alice-desktop/runners/validate-scenarios.sh"],
        ],
        "changedFiles": [],
        "githubActions": {"headSha": EXPECTED_HEAD_SHA, "checks": [completed_check(check) for check in REQUIRED_CHECKS]},
        "boundaryEvidence": boundary_evidence(),
        "qaScenarioContracts": qa_scenario_contracts(),
        "validations": {
            "tweedleLangInitialized": True,
            "nodeOptions": "--max-old-space-size=32768",
            "pythonContract": {"outcome": "passed"},
            "scenarioValidation": {"outcome": "passed"},
        },
        "prEvidence": {
            "headSha": EXPECTED_HEAD_SHA,
            "currentHeadEvidence": True,
            "mergeReadyCriteriaUpdated": True,
            "boundedArchivePlayerClaimsOnly": True,
            "blockers": [],
        },
    }


class Pr463OwnerFreeRecoveryGateUnitTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_recovery_gate()
        self.evidence = merge_ready_evidence()

    def assert_no_blockers(self, verifier_name: str) -> None:
        verifier = getattr(self.module, verifier_name)
        self.assertEqual([], verifier(self.evidence))

    def assert_has_blocker(self, verifier_name: str, mutate, expected_blocker: str) -> None:
        evidence = copy.deepcopy(self.evidence)
        mutate(evidence)
        verifier = getattr(self.module, verifier_name)
        blockers = verifier(evidence)
        self.assertIn(expected_blocker, blockers)

    def test_pr_state_verifier_requires_clean_current_owner_free_noop_inputs(self) -> None:
        self.assert_no_blockers("verify_pr_state")
        blocker_cases = [
            ("dirty worktree", lambda e: e.update({"worktreeClean": False}), "dirty-worktree"),
            ("stale local head", lambda e: e.update({"localHeadSha": "deadbeef"}), "local-head-sha-mismatch"),
            ("stale pr head", lambda e: e.update({"prHeadSha": "deadbeef"}), "pr-head-sha-mismatch"),
            ("not mergeable", lambda e: e.update({"mergeable": "CONFLICTING"}), "pr-not-mergeable"),
            ("unstable merge state", lambda e: e.update({"mergeStateStatus": "UNSTABLE"}), "pr-merge-state-not-clean"),
            ("manual merge used", lambda e: e.update({"manualMergeUsed": True}), "manual-merge-used"),
        ]
        for name, mutate, expected_blocker in blocker_cases:
            with self.subTest(name=name):
                self.assert_has_blocker("verify_pr_state", mutate, expected_blocker)

    def test_github_checks_verifier_blocks_pending_failure_and_missing_required_checks(self) -> None:
        self.assert_no_blockers("verify_github_actions")
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"]["checks"][0].update({"status": "IN_PROGRESS", "conclusion": None}),
            "github-actions-not-complete",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"]["checks"][1].update({"conclusion": "FAILURE"}),
            "github-actions-not-green",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"].update({"checks": e["githubActions"]["checks"][1:]}),
            "github-actions-required-check-missing",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"].update({"headSha": "deadbeef"}),
            "github-actions-stale-head",
        )

    def test_github_checks_verifier_accepts_live_coverage_check_shorthand(self) -> None:
        self.evidence["githubActions"]["checks"][0]["name"] = "coverage"

        self.assert_no_blockers("verify_github_actions")

    def test_external_service_verifier_blocks_failed_github_refresh(self) -> None:
        self.evidence["externalServiceErrors"] = [
            {
                "service": "github",
                "operation": "gh pr view",
                "message": "gh pr view timed out after 20s",
            }
        ]

        self.assertEqual(
            ["github-pr-service-unavailable"],
            self.module.verify_external_service_errors(self.evidence),
        )

    def test_fetch_github_pr_evidence_normalizes_live_pr_evidence(self) -> None:
        calls = []

        def runner(command, **kwargs):
            calls.append((command, kwargs))
            return github_success_result(command)

        evidence = self.module.fetch_github_pr_evidence(
            timeout_seconds=15,
            max_attempts=1,
            runner=runner,
            sleeper=lambda _: None,
        )

        self.assertEqual(EXPECTED_HEAD_SHA, evidence["headSha"])
        self.assertEqual(EXPECTED_HEAD_SHA, evidence["prHeadSha"])
        self.assertEqual(EXPECTED_BRANCH, evidence["branch"])
        self.assertEqual("develop", evidence["baseRef"])
        self.assertEqual("coverage", evidence["githubActions"]["checks"][0]["name"])
        self.assertEqual(
            [
                "gh",
                "pr",
                "view",
                "463",
                "--repo",
                "rysweet/RabbitHole",
                "--json",
                "baseRefName,headRefName,headRefOid,mergeable,mergeStateStatus,statusCheckRollup,url",
            ],
            calls[0][0],
        )
        self.assertEqual(15, calls[0][1]["timeout"])

    def test_fetch_github_pr_evidence_retries_transient_cli_failure_before_success(self) -> None:
        attempts = []
        sleeps = []

        def runner(command, **kwargs):
            attempts.append(command)
            if len(attempts) == 1:
                return subprocess.CompletedProcess(command, 1, stdout="", stderr="temporary outage")
            return github_success_result(
                command,
                [{"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS"}],
            )

        evidence = self.module.fetch_github_pr_evidence(
            max_attempts=2,
            retry_delay_seconds=0.25,
            runner=runner,
            sleeper=sleeps.append,
        )

        self.assertEqual(EXPECTED_HEAD_SHA, evidence["headSha"])
        self.assertEqual(2, len(attempts))
        self.assertEqual([0.25], sleeps)

    def test_fetch_github_pr_evidence_retries_transient_invalid_json_before_success(self) -> None:
        attempts = []

        def runner(command, **kwargs):
            attempts.append(command)
            if len(attempts) == 1:
                return subprocess.CompletedProcess(command, 0, stdout="{not json", stderr="")
            return github_success_result(
                command,
                [{"name": "coverage", "status": "COMPLETED", "conclusion": "SUCCESS"}],
            )

        evidence = self.module.fetch_github_pr_evidence(
            max_attempts=2,
            retry_delay_seconds=0,
            runner=runner,
            sleeper=lambda _: None,
        )

        self.assertEqual(EXPECTED_HEAD_SHA, evidence["headSha"])
        self.assertEqual("coverage", evidence["githubActions"]["checks"][0]["name"])
        self.assertEqual(2, len(attempts))

    def test_fetch_github_pr_evidence_raises_explicit_error_after_retry_exhaustion(self) -> None:
        def runner(command, **kwargs):
            return subprocess.CompletedProcess(command, 1, stdout="", stderr="authentication failed")

        with self.assertRaisesRegex(
            self.module.GitHubServiceError,
            "gh pr view failed with exit code 1: authentication failed",
        ):
            self.module.fetch_github_pr_evidence(
                max_attempts=2,
                retry_delay_seconds=0,
                runner=runner,
                sleeper=lambda _: None,
            )

    def test_fetch_github_pr_evidence_redacts_secret_like_cli_stderr(self) -> None:
        secret = "gh" + "p_" + ("a" * 36)
        authorization_header = "Authorization"
        token_variable = "GITHUB_" + "TOKEN"

        def runner(command, **kwargs):
            return subprocess.CompletedProcess(
                command,
                1,
                stdout="",
                stderr=f"{authorization_header}: token {secret} {token_variable}={secret}",
            )

        with self.assertRaisesRegex(self.module.GitHubServiceError, "<redacted>") as context:
            self.module.fetch_github_pr_evidence(
                max_attempts=1,
                runner=runner,
                sleeper=lambda _: None,
            )

        self.assertNotIn(secret, str(context.exception))

    def test_repair_scope_verifier_allows_only_archive_player_boundary_surfaces(self) -> None:
        repair_evidence = copy.deepcopy(self.evidence)
        repair_evidence.update(
            {
                "repairRequired": True,
                "noOpJustificationUsed": False,
                "pushedRepair": True,
                "changedFiles": FOCUSED_REPAIR_PATHS[:3],
            }
        )

        self.assertEqual([], self.module.verify_repair_scope(repair_evidence))
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e.update(
                {
                    "repairRequired": True,
                    "changedFiles": ["docs/reference/unrelated-recovery.md"],
                }
            ),
            "unfocused-diff-scope",
        )
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e.update(
                {
                    "repairRequired": True,
                    "changedFiles": ["qa/fixtures/generated-player-archive.a3w"],
                }
            ),
            "generated-archive-committed",
        )
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e.update({"repairRequired": True, "pushedRepair": False}),
            "focused-repair-not-pushed",
        )

    def test_boundary_evidence_verifier_rejects_stale_docs_overclaims_and_binary_fixtures(self) -> None:
        self.assert_no_blockers("verify_boundary_evidence")
        self.assert_has_blocker(
            "verify_boundary_evidence",
            lambda e: e["boundaryEvidence"].update({"referenceDocCurrent": False}),
            "archive-player-evidence-stale",
        )
        self.assert_has_blocker(
            "verify_boundary_evidence",
            lambda e: e["boundaryEvidence"]["forbiddenClaims"].append("proves full Tweedle/player decode"),
            "archive-player-evidence-overclaims",
        )
        self.assert_has_blocker(
            "verify_boundary_evidence",
            lambda e: e["boundaryEvidence"]["generatedArchivesCommitted"].append("fixtures/generated.a3w"),
            "generated-archive-committed",
        )
        self.assert_has_blocker(
            "verify_boundary_evidence",
            lambda e: e["boundaryEvidence"]["binaryCorpusPayloadsCommitted"].append("fixtures/generated.zip"),
            "binary-corpus-payload-committed",
        )

    def test_qa_scenario_contract_verifier_requires_workflow_schema_runner_validator_alignment(self) -> None:
        self.assert_no_blockers("verify_qa_scenario_contracts")
        self.assert_has_blocker(
            "verify_qa_scenario_contracts",
            lambda e: e["qaScenarioContracts"]["workflows"]["archive-fixture-smoke"].update(
                {"allowlistedInRunner": False}
            ),
            "qa-workflow-runner-not-allowlisted",
        )
        self.assert_has_blocker(
            "verify_qa_scenario_contracts",
            lambda e: e["qaScenarioContracts"]["workflows"]["tweedle-decoder-boundary-smoke"].update(
                {"listedInSchema": False}
            ),
            "qa-workflow-schema-not-allowlisted",
        )
        self.assert_has_blocker(
            "verify_qa_scenario_contracts",
            lambda e: e["qaScenarioContracts"]["workflows"]["archive-fixture-smoke"].update(
                {"argv": ["mvn", "test"]}
            ),
            "qa-workflow-argv-not-focused",
        )
        self.assert_has_blocker(
            "verify_qa_scenario_contracts",
            lambda e: e["qaScenarioContracts"].update({"validated": False}),
            "qa-scenario-validation-not-run",
        )

    def test_validation_verifier_requires_focused_contracts_and_saved_node_options(self) -> None:
        self.assert_no_blockers("verify_validation_evidence")
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"].update({"tweedleLangInitialized": False}),
            "tweedle-lang-not-initialized",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"].update({"nodeOptions": ""}),
            "missing-node-options",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"]["pythonContract"].update({"outcome": "failed"}),
            "python-contract-validation-failed",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"]["scenarioValidation"].update({"outcome": "failed"}),
            "qa-scenario-validation-failed",
        )

    def test_command_safety_verifier_rejects_manual_merge_and_unsafe_noop_or_push(self) -> None:
        self.assert_no_blockers("verify_command_safety")
        blocker_cases = [
            ("gh merge", lambda e: e["commands"].append(["gh", "pr", "merge", "463"]), "manual-merge-used"),
            ("git merge", lambda e: e["commands"].append(["git", "merge", "origin/develop"]), "manual-merge-used"),
            ("shell gh merge", lambda e: e["commands"].append("true && gh pr merge 463"), "manual-merge-used"),
            ("noop with repair", lambda e: e.update({"repairRequired": True, "noOpJustificationUsed": True}), "noop-used-despite-required-repair"),
            ("push without repair", lambda e: e.update({"repairRequired": False, "pushedRepair": True}), "unexpected-push-without-repair"),
        ]
        for name, mutate, expected_blocker in blocker_cases:
            with self.subTest(name=name):
                self.assert_has_blocker("verify_command_safety", mutate, expected_blocker)


class Pr463OwnerFreeRecoveryGateIntegrationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_recovery_gate()

    def test_clean_green_current_head_returns_merge_ready_owner_free_noop(self) -> None:
        result = self.module.evaluate_readiness(merge_ready_evidence())

        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])
        self.assertEqual(EXPECTED_HEAD_SHA, result["headSha"])
        self.assertFalse(result["repairRequired"])
        self.assertIn("literal owner-free no-op", result["summary"].lower())

    def test_pending_coverage_returns_not_merge_ready_without_archive_repair_scope(self) -> None:
        evidence = merge_ready_evidence()
        evidence.update({"mergeStateStatus": "UNSTABLE", "noOpJustificationUsed": False})
        evidence["githubActions"]["checks"][0].update({"status": "IN_PROGRESS", "conclusion": None})

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("github-actions-not-complete", result["blockers"])
        self.assertFalse(result["repairRequired"])
        self.assertEqual([], result["allowedRepairPaths"])
        self.assertNotIn("MERGE_READY", result.get("summary", ""))

    def test_relevant_archive_player_failure_returns_focused_repair_required_scope(self) -> None:
        evidence = merge_ready_evidence()
        evidence.update(
            {
                "mergeStateStatus": "UNSTABLE",
                "noOpJustificationUsed": False,
                "repairRequired": True,
                "pushedRepair": False,
            }
        )
        evidence["githubActions"]["checks"][2].update(
            {
                "name": "archive/player boundary contract",
                "conclusion": "FAILURE",
                "failureSurface": "archive-player-boundary",
            }
        )
        evidence["boundaryEvidence"].update({"referenceDocCurrent": False})

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("REPAIR_REQUIRED", result["status"])
        self.assertIn("archive-player-evidence-stale", result["blockers"])
        self.assertEqual(FOCUSED_REPAIR_PATHS, result["allowedRepairPaths"])
        self.assertFalse(result["mayUseNoOpJustification"])

    def test_pushed_owner_free_guard_repair_can_be_merge_ready_after_checks_pass(self) -> None:
        evidence = merge_ready_evidence()
        evidence.update(
            {
                "noOpJustificationUsed": False,
                "repairRequired": True,
                "pushedRepair": True,
                "changedFiles": [
                    "scripts/pr463_recovery_gate.py",
                    "tests/test_pr463_owner_free_recovery_gate.py",
                ],
            }
        )
        evidence["commands"].append(["git", "push", "origin", EXPECTED_BRANCH])

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])
        self.assertFalse(result["mayUseNoOpJustification"])
        self.assertIn("focused archive/player guard repair", result["summary"])
        self.assertNotIn("literal owner-free no-op", result["summary"].lower())

    def test_missing_evidence_reports_blockers_instead_of_success_shaped_defaults(self) -> None:
        result = self.module.evaluate_readiness(
            {
                "repository": "rysweet/RabbitHole",
                "prNumber": EXPECTED_PR,
                "branch": EXPECTED_BRANCH,
            }
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("missing-pr-head-evidence", result["blockers"])
        self.assertIn("missing-github-actions-evidence", result["blockers"])
        self.assertIn("missing-boundary-evidence", result["blockers"])
        self.assertIn("missing-qa-scenario-contract-evidence", result["blockers"])
        self.assertNotIn("MERGE_READY", result.get("summary", ""))


if __name__ == "__main__":
    unittest.main()
