import copy
import importlib.util
import json
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RECOVERY_GATE_PATH = REPO_ROOT / "scripts" / "pr463_recovery_gate.py"
EXPECTED_PR = 463
EXPECTED_REPOSITORY = "rysweet/RabbitHole"
EXPECTED_BRANCH = "feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr"
EXPECTED_BASE_REF = "develop"
EXPECTED_DEVELOP_BASE_SHA = "1" * 40
EXPECTED_HEAD_SHA = "2" * 40
EXPECTED_NODE_OPTIONS = "--max-old-space-size=32768"
EXPECTED_RECOVERY_MODE = "focused-archive-player-repair"
EXPECTED_SCOPE = "archive/player-boundary"

REQUIRED_CHECKS = [
    "Alice Coverage Reports/coverage",
    "package-netbeans",
    "test",
    "build",
    "GitGuardian Security Checks",
]

ARCHIVE_PLAYER_EVIDENCE_SURFACES = [
    "docs/reference/player-archive-unsupported-tweedle-diagnostics.md",
    "docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md",
    "docs/tutorials/player-archive-unsupported-this-call-diagnostic.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml",
    "core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java",
    "core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java",
]

FOCUSED_REPAIR_PATHS = [
    "scripts/pr463_recovery_gate.py",
    "tests/test_pr463_owner_free_recovery_gate.py",
    "tests/test_pr463_archive_player_boundary_contract.py",
    "docs/reference/player-archive-unsupported-tweedle-diagnostics.md",
    "docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md",
    "docs/tutorials/player-archive-unsupported-this-call-diagnostic.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml",
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

VALIDATION_COMMANDS = {
    "python-pr463-contracts": (
        "NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest "
        "tests.test_pr463_owner_free_recovery_gate "
        "tests.test_pr463_archive_player_boundary_contract"
    ),
    "alice-desktop-scenario-catalog": (
        "NODE_OPTIONS=--max-old-space-size=32768 bash "
        "qa/outside-in/alice-desktop/runners/validate-scenarios.sh && "
        "NODE_OPTIONS=--max-old-space-size=32768 bash "
        "qa/outside-in/alice-desktop/tests/test-schema-contract.sh"
    ),
    "story-api-migration-characterization": (
        "NODE_OPTIONS=--max-old-space-size=32768 mvn "
        "-pl core/story-api-migration -am "
        "-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false "
        "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest test"
    ),
    "core-ast-decoder-boundary": (
        "NODE_OPTIONS=--max-old-space-size=32768 mvn "
        "-pl core/ast -am "
        "-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false "
        "-Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#"
        "zeroArgumentThisMethodCallDecodeCreatesMethodInvocation+"
        "zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+"
        "zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+"
        "zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+"
        "zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsChainedCall test"
    ),
}


def load_recovery_gate():
    if not RECOVERY_GATE_PATH.exists():
        raise AssertionError(
            "scripts/pr463_recovery_gate.py must implement the PR #463 recovery gate."
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
        "baseRefName": EXPECTED_BASE_REF,
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


def validation_record(name: str, outcome: str = "passed", head_sha: str = EXPECTED_HEAD_SHA) -> dict:
    return {
        "name": name,
        "command": VALIDATION_COMMANDS[name],
        "outcome": outcome,
        "headSha": head_sha,
    }


def validation_evidence() -> list[dict]:
    return [validation_record(name) for name in VALIDATION_COMMANDS]


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


def repaired_branch_evidence() -> dict:
    return {
        "repository": EXPECTED_REPOSITORY,
        "prNumber": EXPECTED_PR,
        "branch": EXPECTED_BRANCH,
        "baseRef": EXPECTED_BASE_REF,
        "developBaseSha": EXPECTED_DEVELOP_BASE_SHA,
        "headSha": EXPECTED_HEAD_SHA,
        "localHeadSha": EXPECTED_HEAD_SHA,
        "prHeadSha": EXPECTED_HEAD_SHA,
        "worktreeClean": True,
        "mergeable": "MERGEABLE",
        "mergeStateStatus": "CLEAN",
        "recoveryMode": EXPECTED_RECOVERY_MODE,
        "manualMergePerformed": False,
        "replacementPullRequestCreated": False,
        "noOpModeUsed": False,
        "scope": EXPECTED_SCOPE,
        "repairRequired": True,
        "pushedRepair": True,
        "archivePlayerEvidenceSurfaces": ARCHIVE_PLAYER_EVIDENCE_SURFACES,
        "repairDiffFiles": FOCUSED_REPAIR_PATHS,
        "tweedleLangInitialized": True,
        "nodeOptions": EXPECTED_NODE_OPTIONS,
        "commands": [
            ["git", "fetch", "origin", "develop"],
            ["git", "submodule", "update", "--init", "tweedle-lang"],
            ["python3", "-m", "unittest", "tests.test_pr463_owner_free_recovery_gate"],
            ["bash", "qa/outside-in/alice-desktop/runners/validate-scenarios.sh"],
            ["gh", "pr", "view", "463", "--json", "headRefOid,mergeStateStatus,statusCheckRollup"],
        ],
        "githubActions": {
            "headSha": EXPECTED_HEAD_SHA,
            "checks": [completed_check(check) for check in REQUIRED_CHECKS],
        },
        "boundaryEvidence": boundary_evidence(),
        "qaScenarioContracts": qa_scenario_contracts(),
        "validations": validation_evidence(),
        "prEvidence": {
            "headSha": EXPECTED_HEAD_SHA,
            "currentHeadEvidence": True,
            "mergeReadyCriteriaUpdated": True,
            "boundedArchivePlayerClaimsOnly": True,
            "blockers": [],
        },
    }


class Pr463RecoveryGateUnitTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_recovery_gate()
        self.evidence = repaired_branch_evidence()

    def assert_no_blockers(self, verifier_name: str) -> None:
        verifier = getattr(self.module, verifier_name)
        self.assertEqual([], verifier(self.evidence))

    def assert_has_blocker(self, verifier_name: str, mutate, expected_blocker: str) -> None:
        evidence = copy.deepcopy(self.evidence)
        mutate(evidence)
        verifier = getattr(self.module, verifier_name)
        blockers = verifier(evidence)
        self.assertIn(expected_blocker, blockers)

    def test_pr_state_verifier_requires_repaired_existing_branch_against_current_develop(self) -> None:
        self.assert_no_blockers("verify_pr_state")
        blocker_cases = [
            ("dirty worktree", lambda e: e.update({"worktreeClean": False}), "dirty-worktree"),
            ("missing develop base", lambda e: e.update({"developBaseSha": ""}), "missing-develop-base-sha"),
            ("stale local head", lambda e: e.update({"localHeadSha": "deadbeef"}), "local-head-sha-mismatch"),
            ("stale pr head", lambda e: e.update({"prHeadSha": "deadbeef"}), "pr-head-sha-mismatch"),
            ("not mergeable", lambda e: e.update({"mergeable": "CONFLICTING"}), "pr-not-mergeable"),
            ("unstable merge state", lambda e: e.update({"mergeStateStatus": "DIRTY"}), "pr-merge-state-not-clean"),
            ("wrong recovery mode", lambda e: e.update({"recoveryMode": "owner-free-noop"}), "wrong-recovery-mode"),
            ("wrong scope", lambda e: e.update({"scope": "general-qa"}), "wrong-recovery-scope"),
            ("manual merge", lambda e: e.update({"manualMergePerformed": True}), "manual-merge-performed"),
            (
                "replacement PR",
                lambda e: e.update({"replacementPullRequestCreated": True}),
                "replacement-pr-created",
            ),
            ("no-op mode", lambda e: e.update({"noOpModeUsed": True}), "noop-mode-used"),
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
        self.assertEqual(EXPECTED_BASE_REF, evidence["baseRef"])
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

    def test_repair_scope_verifier_uses_repair_diff_files_separate_from_evidence_surfaces(self) -> None:
        self.assert_no_blockers("verify_repair_scope")
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e.update({"repairDiffFiles": []}),
            "missing-focused-repair-diff",
        )
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e["repairDiffFiles"].append("docs/reference/unrelated-recovery.md"),
            "unfocused-diff-scope",
        )
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e["repairDiffFiles"].append("qa/fixtures/generated-player-archive.a3w"),
            "generated-archive-committed",
        )
        self.assert_has_blocker(
            "verify_repair_scope",
            lambda e: e.update({"pushedRepair": False}),
            "focused-repair-not-pushed",
        )

    def test_boundary_evidence_verifier_rejects_broadened_surface_scope_and_overclaims(self) -> None:
        self.assert_no_blockers("verify_boundary_evidence")
        self.assert_has_blocker(
            "verify_boundary_evidence",
            lambda e: e.update({"archivePlayerEvidenceSurfaces": []}),
            "archive-player-evidence-surfaces-missing",
        )
        self.assert_has_blocker(
            "verify_boundary_evidence",
            lambda e: e["archivePlayerEvidenceSurfaces"].append(
                "qa/outside-in/alice-desktop/scenarios/save-load.yaml"
            ),
            "archive-player-evidence-scope-broadened",
        )
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

    def test_validation_verifier_requires_structured_focused_records_for_final_head(self) -> None:
        self.assert_no_blockers("verify_validation_evidence")
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e.update({"tweedleLangInitialized": False}),
            "tweedle-lang-not-initialized",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e.update({"nodeOptions": ""}),
            "missing-node-options",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"][0].update({"outcome": "failed"}),
            "python-contract-validation-failed",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"][1].update({"outcome": "failed"}),
            "qa-scenario-validation-failed",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"][2].update({"headSha": "deadbeef"}),
            "validation-stale-head",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e["validations"][3].update({"command": ""}),
            "validation-command-missing",
        )
        self.assert_has_blocker(
            "verify_validation_evidence",
            lambda e: e.update(
                {
                    "validations": [
                        record
                        for record in e["validations"]
                        if record["name"] != "core-ast-decoder-boundary"
                    ]
                }
            ),
            "focused-maven-validation-missing",
        )

    def test_pr_evidence_verifier_requires_current_head_and_bounded_nonclaims(self) -> None:
        self.assert_no_blockers("verify_pr_evidence")
        self.assert_has_blocker(
            "verify_pr_evidence",
            lambda e: e["prEvidence"].update({"headSha": "deadbeef"}),
            "pr-evidence-stale-head",
        )
        self.assert_has_blocker(
            "verify_pr_evidence",
            lambda e: e["prEvidence"].update({"currentHeadEvidence": False}),
            "pr-evidence-missing-current-head",
        )
        self.assert_has_blocker(
            "verify_pr_evidence",
            lambda e: e["prEvidence"].update({"boundedArchivePlayerClaimsOnly": False}),
            "pr-evidence-overclaims-archive-player-boundary",
        )

    def test_command_safety_verifier_rejects_manual_merge_noop_replacement_and_unsafe_push(self) -> None:
        self.assert_no_blockers("verify_command_safety")
        blocker_cases = [
            ("gh merge", lambda e: e["commands"].append(["gh", "pr", "merge", "463"]), "manual-merge-used"),
            ("shell gh merge", lambda e: e["commands"].append("true && gh pr merge 463"), "manual-merge-used"),
            ("manual merge flag", lambda e: e.update({"manualMergePerformed": True}), "manual-merge-performed"),
            ("replacement PR", lambda e: e.update({"replacementPullRequestCreated": True}), "replacement-pr-created"),
            ("no-op mode", lambda e: e.update({"noOpModeUsed": True}), "noop-mode-used"),
            ("push without repair", lambda e: e.update({"repairRequired": False, "pushedRepair": True}), "unexpected-push-without-repair"),
        ]
        for name, mutate, expected_blocker in blocker_cases:
            with self.subTest(name=name):
                self.assert_has_blocker("verify_command_safety", mutate, expected_blocker)


class Pr463RecoveryGateIntegrationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_recovery_gate()

    def test_repaired_current_head_returns_merge_ready_without_noop_language(self) -> None:
        result = self.module.evaluate_readiness(repaired_branch_evidence())

        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])
        self.assertEqual(EXPECTED_HEAD_SHA, result["headSha"])
        self.assertFalse(result["mayUseNoOpJustification"])
        self.assertEqual(EXPECTED_RECOVERY_MODE, result["recoveryMode"])
        self.assertIn("focused archive/player repair", result["summary"].lower())
        self.assertNotIn("owner-free", result["summary"].lower())
        self.assertNotIn("no-op", result["summary"].lower())

    def test_clean_metadata_without_focused_repair_is_blocked_instead_of_noop_ready(self) -> None:
        evidence = repaired_branch_evidence()
        evidence.update(
            {
                "recoveryMode": "owner-free-noop",
                "noOpModeUsed": True,
                "repairRequired": False,
                "pushedRepair": False,
                "repairDiffFiles": [],
            }
        )

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("noop-mode-used", result["blockers"])
        self.assertIn("wrong-recovery-mode", result["blockers"])
        self.assertFalse(result["mayUseNoOpJustification"])
        self.assertNotIn("MERGE_READY", result.get("summary", ""))

    def test_pending_coverage_returns_not_merge_ready_without_archive_repair_scope(self) -> None:
        evidence = repaired_branch_evidence()
        evidence.update({"mergeStateStatus": "UNSTABLE", "repairRequired": False, "pushedRepair": False})
        evidence["githubActions"]["checks"][0].update({"status": "IN_PROGRESS", "conclusion": None})

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("github-actions-not-complete", result["blockers"])
        self.assertFalse(result["repairRequired"])
        self.assertEqual([], result["allowedRepairPaths"])
        self.assertNotIn("MERGE_READY", result.get("summary", ""))

    def test_relevant_archive_player_failure_returns_focused_repair_required_scope(self) -> None:
        evidence = repaired_branch_evidence()
        evidence.update(
            {
                "mergeStateStatus": "UNSTABLE",
                "repairRequired": True,
                "pushedRepair": False,
                "repairDiffFiles": [],
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

    def test_missing_evidence_reports_blockers_instead_of_success_shaped_defaults(self) -> None:
        result = self.module.evaluate_readiness(
            {
                "repository": EXPECTED_REPOSITORY,
                "prNumber": EXPECTED_PR,
                "branch": EXPECTED_BRANCH,
            }
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("missing-pr-head-evidence", result["blockers"])
        self.assertIn("missing-github-actions-evidence", result["blockers"])
        self.assertIn("missing-boundary-evidence", result["blockers"])
        self.assertIn("missing-qa-scenario-contract-evidence", result["blockers"])
        self.assertIn("missing-validation-evidence", result["blockers"])
        self.assertNotIn("MERGE_READY", result.get("summary", ""))


if __name__ == "__main__":
    unittest.main()
