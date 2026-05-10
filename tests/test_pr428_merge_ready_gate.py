"""TDD contract tests for the PR #428 merge-ready gate.

These tests define the expected behavior of scripts/pr428_merge_ready_gate.py
which evaluates issue-reporting test-seam characterization evidence before any
merge-ready claim.  They follow the PR #389 gate pattern: load the gate module
dynamically, test each verifier with positive/negative evidence, test the
integration evaluate_readiness entry point, and verify the 5-file plumbing
sync contract for the issue-reporting-smoke QA scenario.

TDD red phase: these tests FAIL until the gate script is implemented.
"""

import copy
import importlib.util
import json
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
GATE_SCRIPT_PATH = REPO_ROOT / "scripts" / "pr428_merge_ready_gate.py"
EXPECTED_BRANCH = "feat/issue-408-rabbithole-wave7-coverage-ratchet-lane-follow-defa"
EXPECTED_REMOTE_REF = f"origin/{EXPECTED_BRANCH}"
EXPECTED_PR = 428
HEAD_SHA = "cd25b49fa7bd43d844c5ee452a1333163b32b1b6"
FOCUSED_MAVEN_ARGV = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/issue-reporting",
    "-am",
    "-Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest",
    "test",
]
EXPECTED_SCENARIO = "alice-desktop-issue-reporting-smoke"
EXPECTED_WORKFLOW = "issue-reporting-smoke"
EXPECTED_AUTOMATION_MODE = "gated-command-smoke"
EXPECTED_TEST_CLASS = "org.lgna.issue.IssueSubmissionProgressWorkerTest"
FORBIDDEN_CLAIMS = [
    "full UI automation",
    "visible rendering correctness",
    "grading",
    "creative assessment",
    "full lesson completion",
    "full Tweedle/player decode",
    "real issue-service HTTP submission",
    "JProgressPane dialog rendering",
    "JSubmitPane form validation",
]

# Sorted list of files changed in PR #428 vs origin/develop.
# Includes this test file and the gate script (which will be created
# in the implementation step).
PR428_CHANGED_FILES = [
    ".copilot-evidence/default-workflow-attempt.log",
    ".github/workflows/alice-checkstyle-ci.yml",
    ".github/workflows/alice-coverage-ci.yml",
    ".github/workflows/alice-netbeans-package-ci.yml",
    ".github/workflows/alice-test-ci.yml",
    "alice_qa_amplihack.py",
    "core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java",
    "core/ide/src/test/java/org/alice/ide/ProjectBackupRecoveryIoTest.java",
    "core/ide/src/test/java/org/alice/ide/ProjectBackupSelectorTest.java",
    "core/ide/src/test/java/org/alice/ide/ProjectFileUtilitiesTest.java",
    "core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java",
    "core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java",
    "core/story-api-migration/src/main/java/org/lgna/project/io/DataSourceIo.java",
    "core/story-api-migration/src/main/java/org/lgna/project/io/IoUtilities.java",
    "core/story-api-migration/src/main/java/org/lgna/project/io/XmlProjectIo.java",
    "core/story-api-migration/src/main/java/org/lgna/project/io/ZipEntryContainer.java",
    "core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java",
    "docs/howto/characterize-issue-submission-progress-worker.md",
    "docs/howto/characterize-source-code-generator.md",
    "docs/howto/finalize-source-code-generator.md",
    "docs/howto/use-formal-spec-artifacts.md",
    "docs/howto/validate-archive-player-boundary.md",
    "docs/index.md",
    "docs/reference/archive-player-boundary.md",
    "docs/reference/ci-efficiency.md",
    "docs/reference/exported-netbeans-ant-project-behavior.md",
    "docs/reference/formal-spec-contracts.md",
    "docs/reference/gadugi-exported-launcher-evidence.md",
    "docs/reference/generated-story-api-listener-source-characterization.md",
    "docs/reference/issue-submission-progress-worker.md",
    "docs/tutorials/archive-player-boundary-characterization.md",
    "docs/tutorials/trace-issue-submission-progress-worker.md",
    "docs/tutorials/trace-save-load-recovery.md",
    "docs/tutorials/trace-source-code-generator-characterization.md",
    "netbeans/src/main/java/org/alice/netbeans/project/ProjectCodeGenerator.java",
    "netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorTest.java",
    "pyproject.toml",
    "qa/outside-in/alice-desktop/runners/run-scenario.sh",
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/issue-reporting-smoke.yaml",
    "qa/outside-in/alice-desktop/schema/scenario.schema.json",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "scripts/pr428_merge_ready_gate.py",
    "tests/test_alice_qa_amplihack.py",
    "tests/test_archive_player_boundary_docs.py",
    "tests/test_ci_noop_workflow_contract.py",
    "tests/test_coverage_workflow.py",
    "tests/test_formal_spec_contracts_reference.py",
    "tests/test_pr426_formal_contract_wiring.py",
    "tests/test_pr428_merge_ready_gate.py",
]

COMMAND_SAFETY_BLOCKER_CASES = (
    (
        "manual merge flag",
        lambda evidence: evidence.update({"manualMergeUsed": True}),
        "manual-merge-used",
    ),
    (
        "direct timeout argv",
        lambda evidence: evidence["commands"].append(["timeout", "600", "mvn", "test"]),
        "timeout-wrapper-used",
    ),
    (
        "env-wrapped timeout argv",
        lambda evidence: evidence["commands"].append(["env", "timeout", "600", "mvn", "test"]),
        "timeout-wrapper-used",
    ),
    (
        "shell separator manual merge",
        lambda evidence: evidence["commands"].append("cd repo && gh pr merge 428"),
        "manual-merge-used",
    ),
    (
        "nested bash timeout",
        lambda evidence: evidence["commands"].append(["bash", "-lc", "timeout 600 mvn test"]),
        "timeout-wrapper-used",
    ),
    (
        "nested sh git merge",
        lambda evidence: evidence["commands"].append(["sh", "-c", "git fetch && git merge HEAD"]),
        "manual-merge-used",
    ),
    (
        "semicolon manual merge",
        lambda evidence: evidence["commands"].append("echo ok;gh pr merge 428"),
        "manual-merge-used",
    ),
    (
        "nested semicolon manual merge",
        lambda evidence: evidence["commands"].append(["bash", "-lc", "echo ok;gh pr merge 428"]),
        "manual-merge-used",
    ),
    (
        "adjacent operator manual merge",
        lambda evidence: evidence["commands"].append("true&&gh pr merge 428"),
        "manual-merge-used",
    ),
    (
        "semicolon timeout",
        lambda evidence: evidence["commands"].append("echo ok;timeout 600 mvn test"),
        "timeout-wrapper-used",
    ),
    (
        "nested semicolon timeout",
        lambda evidence: evidence["commands"].append(
            ["bash", "-lc", "echo ok;timeout 600 mvn test"]
        ),
        "timeout-wrapper-used",
    ),
)


# ---------------------------------------------------------------------------
# QA plumbing 5-file sync constants
# ---------------------------------------------------------------------------
SCENARIO_YAML = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "issue-reporting-smoke.yaml"
)
SCHEMA_JSON = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "schema" / "scenario.schema.json"
)
VALIDATE_SH = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "validate-scenarios.sh"
)
RUN_SCENARIO_SH = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "run-scenario.sh"
)
SCHEMA_CONTRACT_SH = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-schema-contract.sh"
)

ISSUE_REPORTING_DOCS = [
    REPO_ROOT / "docs" / "reference" / "issue-submission-progress-worker.md",
    REPO_ROOT / "docs" / "howto" / "characterize-issue-submission-progress-worker.md",
    REPO_ROOT / "docs" / "tutorials" / "trace-issue-submission-progress-worker.md",
]


def load_merge_ready_gate():
    """Dynamically load scripts/pr428_merge_ready_gate.py as a module."""
    if not GATE_SCRIPT_PATH.exists():
        raise AssertionError(
            "scripts/pr428_merge_ready_gate.py must implement the PR #428 "
            "merge-ready gate contract.  Create the script to make these tests pass."
        )
    spec = importlib.util.spec_from_file_location("pr428_merge_ready_gate", GATE_SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def merge_ready_evidence() -> dict:
    """Return a complete, valid merge-ready evidence dict for PR #428."""
    return {
        "repository": "rysweet/RabbitHole",
        "prNumber": EXPECTED_PR,
        "branch": EXPECTED_BRANCH,
        "remoteRef": EXPECTED_REMOTE_REF,
        "headSha": HEAD_SHA,
        "originHeadSha": HEAD_SHA,
        "prHeadSha": HEAD_SHA,
        "baseRef": "origin/develop",
        "manualMergeUsed": False,
        "commands": [
            ["git", "fetch", "origin", EXPECTED_BRANCH, "--no-tags"],
            ["git", "switch", "-C", EXPECTED_BRANCH, EXPECTED_REMOTE_REF],
            ["git", "submodule", "update", "--init", "tweedle-lang"],
            [
                "env",
                "ALICE_QA_RUN_GATED_SMOKES=1",
                "NODE_OPTIONS=--max-old-space-size=32768",
                "qa/outside-in/alice-desktop/runners/run-scenario.sh",
                "run",
                EXPECTED_SCENARIO,
                "--evidence-dir",
                "qa/outside-in/alice-desktop/evidence/pr428",
            ],
            FOCUSED_MAVEN_ARGV,
            ["gh", "pr", "checks", "428", "--repo", "rysweet/RabbitHole"],
        ],
        "changedFiles": PR428_CHANGED_FILES,
        "qaEvidence": {
            "scenarioValidation": {"outcome": "passed"},
            "runnerContract": {"outcome": "passed"},
            "schemaContract": {"outcome": "passed"},
            "gatedSmoke": {
                "scenario": EXPECTED_SCENARIO,
                "workflow": EXPECTED_WORKFLOW,
                "automationMode": EXPECTED_AUTOMATION_MODE,
                "gate": "ALICE_QA_RUN_GATED_SMOKES",
                "gateValue": "1",
                "statusTxt": {
                    "outcome": "passed",
                    "exitCode": "0",
                    "argv": " ".join(FOCUSED_MAVEN_ARGV),
                },
                "commandLog": {
                    "contains": [
                        "IssueSubmissionProgressWorkerTest",
                        "backgroundSubmissionPublishesStartThenEndAroundSubmissionResult",
                        "backgroundSubmissionCarriesAttachmentOptOutAndSuccessfulResult",
                        "backgroundSubmissionPropagatesSubmissionExceptionWithoutPublishingCompletion",
                    ],
                    "notContains": ["Java Result:"],
                },
            },
        },
        "mavenEvidence": {
            "tweedleLangInitialized": True,
            "nodeOptions": "--max-old-space-size=32768",
            "argv": FOCUSED_MAVEN_ARGV,
            "exitCode": 0,
            "testClass": EXPECTED_TEST_CLASS,
            "module": "core/issue-reporting",
        },
        "qualityAuditCycles": [
            {
                "cycle": 1,
                "seek": "QA plumbing and argv sync",
                "validate": "Confirmed 5-file plumbing contract intact",
                "fix": "No issues found",
                "clean": False,
            },
            {
                "cycle": 2,
                "seek": "Issue-reporting test seam behavior",
                "validate": "Confirmed 3-test characterization and bounded claims",
                "fix": "No PR-caused code defects found",
                "clean": False,
            },
            {
                "cycle": 3,
                "seek": "Final readiness gate",
                "validate": "No unresolved blockers",
                "fix": "None required",
                "clean": True,
            },
        ],
        "docsImpact": {
            "reviewed": True,
            "files": [
                "docs/reference/issue-submission-progress-worker.md",
                "docs/howto/characterize-issue-submission-progress-worker.md",
                "docs/tutorials/trace-issue-submission-progress-worker.md",
                "docs/index.md",
            ],
            "boundedClaimsOnly": True,
            "forbiddenClaims": [],
        },
        "githubActions": {
            "headSha": HEAD_SHA,
            "checks": [
                {"name": "build", "status": "completed", "conclusion": "success"},
                {"name": "coverage", "status": "completed", "conclusion": "success"},
                {"name": "package-netbeans", "status": "completed", "conclusion": "success"},
                {"name": "test", "status": "completed", "conclusion": "success"},
                {
                    "name": "GitGuardian Security Checks",
                    "status": "completed",
                    "conclusion": "success",
                },
            ],
        },
        "prDescription": {
            "headSha": HEAD_SHA,
            "hasCurrentHeadEvidence": True,
            "hasQaEvidence": True,
            "hasDocsImpact": True,
            "hasDiffScope": True,
            "hasQualityAuditCycles": True,
            "hasBoundedClaims": True,
            "forbiddenClaims": [],
        },
    }


# ===========================================================================
# Unit tests — one per gate verifier function
# ===========================================================================


class Pr428MergeReadyGateUnitTest(unittest.TestCase):
    """Unit tests for individual verifier functions in the PR #428 gate."""

    def setUp(self) -> None:
        self.module = load_merge_ready_gate()
        self.evidence = merge_ready_evidence()

    def assert_no_blockers(self, component: str) -> None:
        verifier = getattr(self.module, component)
        self.assertEqual([], verifier(self.evidence))

    def assert_has_blocker(self, component: str, mutate, expected_code: str) -> None:
        evidence = copy.deepcopy(self.evidence)
        mutate(evidence)
        verifier = getattr(self.module, component)
        blockers = verifier(evidence)
        self.assertIn(expected_code, blockers)

    # --- Head verification ---

    def test_head_verifier_requires_exact_authoritative_pr_head(self) -> None:
        self.assert_no_blockers("verify_head")
        self.assert_has_blocker(
            "verify_head",
            lambda e: e.update({"prHeadSha": "deadbeef"}),
            "pr-head-sha-mismatch",
        )
        self.assert_has_blocker(
            "verify_head",
            lambda e: e.update({"remoteRef": "origin/other-branch"}),
            "wrong-authoritative-branch",
        )

    # --- Diff scope verification ---

    def test_diff_scope_verifier_allows_only_pr_applicable_paths(self) -> None:
        self.assert_no_blockers("verify_diff_scope")
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda e: e["changedFiles"].append("installer/signing/secrets.txt"),
            "unfocused-diff-scope",
        )
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda e: e["changedFiles"].append("core/croquet/src/main/java/Unrelated.java"),
            "unfocused-diff-scope",
        )

    def test_diff_scope_rejects_parent_traversal_paths(self) -> None:
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda e: e["changedFiles"].append("../etc/passwd"),
            "unfocused-diff-scope",
        )

    def test_diff_scope_rejects_secret_paths(self) -> None:
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda e: e["changedFiles"].append(".env"),
            "unfocused-diff-scope",
        )
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda e: e["changedFiles"].append("secrets/token.json"),
            "unfocused-diff-scope",
        )

    # --- QA scenario evidence ---

    def test_qa_scenario_verifier_requires_executed_gated_issue_reporting_smoke(self) -> None:
        self.assert_no_blockers("verify_qa_evidence")
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda e: e["qaEvidence"]["gatedSmoke"]["statusTxt"].update(
                {"outcome": "gated-not-run"}
            ),
            "gated-smoke-not-run",
        )
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda e: e["qaEvidence"]["gatedSmoke"]["statusTxt"].update(
                {"argv": "mvn -Dtest=SomeOtherTest test"}
            ),
            "missing-focused-issue-reporting-smoke-argv",
        )

    def test_qa_verifier_requires_all_validation_outcomes_passed(self) -> None:
        for outcome_key in ("scenarioValidation", "runnerContract", "schemaContract"):
            with self.subTest(outcome=outcome_key):
                self.assert_has_blocker(
                    "verify_qa_evidence",
                    lambda e, k=outcome_key: e["qaEvidence"][k].update({"outcome": "failed"}),
                    f"{outcome_key.replace('V', '-v').replace('C', '-c')}-not-passed"
                    if outcome_key != "scenarioValidation"
                    else "scenario-validation-not-passed",
                )

    def test_qa_verifier_requires_correct_scenario_workflow_and_automation_mode(self) -> None:
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda e: e["qaEvidence"]["gatedSmoke"].update(
                {"scenario": "alice-desktop-wrong-scenario"}
            ),
            "wrong-gated-smoke-scenario",
        )
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda e: e["qaEvidence"]["gatedSmoke"].update({"workflow": "wrong-workflow"}),
            "wrong-gated-smoke-workflow",
        )
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda e: e["qaEvidence"]["gatedSmoke"].update(
                {"automationMode": "wrong-mode"}
            ),
            "wrong-gated-smoke-automation-mode",
        )

    # --- Maven evidence ---

    def test_maven_verifier_requires_tweedle_submodule_and_focused_no_sims_command(self) -> None:
        self.assert_no_blockers("verify_maven_evidence")
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda e: e["mavenEvidence"].update({"tweedleLangInitialized": False}),
            "tweedle-lang-not-initialized",
        )
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda e: e["mavenEvidence"].update({"nodeOptions": ""}),
            "missing-node-options",
        )

    def test_maven_verifier_requires_correct_module_and_test_class(self) -> None:
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda e: e["mavenEvidence"].update({"testClass": "org.Wrong.TestClass"}),
            "wrong-maven-test-class",
        )
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda e: e["mavenEvidence"].update({"module": "core/wrong-module"}),
            "wrong-maven-module",
        )

    def test_maven_verifier_requires_zero_exit_code(self) -> None:
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda e: e["mavenEvidence"].update({"exitCode": 1}),
            "maven-test-failed",
        )

    # --- Quality audit ---

    def test_quality_audit_verifier_requires_three_seek_validate_fix_cycles_and_clean_final_cycle(
        self,
    ) -> None:
        self.assert_no_blockers("verify_quality_audit")
        self.assert_has_blocker(
            "verify_quality_audit",
            lambda e: e.update({"qualityAuditCycles": e["qualityAuditCycles"][:2]}),
            "insufficient-quality-audit-cycles",
        )
        self.assert_has_blocker(
            "verify_quality_audit",
            lambda e: e["qualityAuditCycles"][2].update({"clean": False}),
            "final-quality-audit-cycle-not-clean",
        )

    def test_quality_audit_rejects_open_findings_in_any_cycle(self) -> None:
        self.assert_has_blocker(
            "verify_quality_audit",
            lambda e: e["qualityAuditCycles"].append(
                {
                    "cycle": 4,
                    "seek": "Post-review readiness",
                    "validate": "Confirmed unresolved review blocker",
                    "fix": "Pending",
                    "clean": False,
                    "unresolvedFindings": ["review-blocker"],
                }
            ),
            "quality-audit-open-finding",
        )

    # --- Docs impact ---

    def test_docs_impact_verifier_rejects_unbounded_or_unreviewed_claims(self) -> None:
        self.assert_no_blockers("verify_docs_impact")
        self.assert_has_blocker(
            "verify_docs_impact",
            lambda e: e["docsImpact"].update({"reviewed": False}),
            "docs-impact-not-reviewed",
        )
        self.assert_has_blocker(
            "verify_docs_impact",
            lambda e: e["docsImpact"].update(
                {"boundedClaimsOnly": False, "forbiddenClaims": FORBIDDEN_CLAIMS[:1]}
            ),
            "docs-overclaim-unproven-behavior",
        )

    def test_docs_impact_requires_issue_reporting_docs_listed(self) -> None:
        self.assert_has_blocker(
            "verify_docs_impact",
            lambda e: e["docsImpact"].update({"files": ["docs/index.md"]}),
            "missing-issue-reporting-docs",
        )

    # --- GitHub Actions ---

    def test_github_actions_verifier_requires_green_checks_on_the_same_head_sha(self) -> None:
        self.assert_no_blockers("verify_github_actions")
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"].update({"headSha": "deadbeef"}),
            "github-actions-stale-head",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"]["checks"][0].update({"conclusion": "failure"}),
            "github-actions-not-green",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"]["checks"][0].update(
                {"status": "in_progress", "conclusion": None}
            ),
            "github-actions-not-complete",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda e: e["githubActions"].update(
                {"checks": [{"name": "test", "status": "completed", "conclusion": "success"}]}
            ),
            "github-actions-required-check-missing",
        )

    def test_github_actions_verifier_accepts_uppercase_status_casing(self) -> None:
        self.evidence["githubActions"]["checks"] = [
            {"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS"},
            {"name": "coverage", "status": "COMPLETED", "conclusion": "SUCCESS"},
            {"name": "package-netbeans", "status": "COMPLETED", "conclusion": "SUCCESS"},
            {"name": "test", "status": "COMPLETED", "conclusion": "SUCCESS"},
            {
                "name": "GitGuardian Security Checks",
                "status": " COMPLETED ",
                "conclusion": " SUCCESS ",
            },
        ]
        self.assert_no_blockers("verify_github_actions")

    # --- PR description ---

    def test_pr_description_verifier_requires_evidence_sections(self) -> None:
        self.assert_no_blockers("verify_pr_description")
        self.assert_has_blocker(
            "verify_pr_description",
            lambda e: e["prDescription"].update({"hasQaEvidence": False}),
            "pr-description-missing-qa-evidence",
        )
        self.assert_has_blocker(
            "verify_pr_description",
            lambda e: e["prDescription"].update({"headSha": "deadbeef"}),
            "pr-description-stale-head",
        )
        self.assert_has_blocker(
            "verify_pr_description",
            lambda e: e["prDescription"].update(
                {"hasBoundedClaims": False, "forbiddenClaims": FORBIDDEN_CLAIMS[:2]}
            ),
            "pr-description-overclaims-unproven-behavior",
        )

    def test_pr_description_requires_all_evidence_flags(self) -> None:
        for flag in (
            "hasCurrentHeadEvidence",
            "hasQaEvidence",
            "hasDocsImpact",
            "hasDiffScope",
            "hasQualityAuditCycles",
        ):
            with self.subTest(flag=flag):
                blocker_code = f"pr-description-missing-{flag.replace('has', '').replace('C', '-c').replace('E', '-e').replace('I', '-i').replace('D', '-d').replace('Q', '-q').replace('A', '-a').replace('S', '-s').lower().strip('-')}"
                self.assert_has_blocker(
                    "verify_pr_description",
                    lambda e, f=flag: e["prDescription"].update({f: False}),
                    blocker_code,
                )

    # --- Command safety ---

    def test_command_verifier_rejects_manual_merge_and_timeout_wrappers(self) -> None:
        self.assert_no_blockers("verify_command_safety")
        for name, mutate, expected_blocker in COMMAND_SAFETY_BLOCKER_CASES:
            with self.subTest(name=name):
                self.assert_has_blocker(
                    "verify_command_safety",
                    mutate,
                    expected_blocker,
                )


# ===========================================================================
# Integration tests
# ===========================================================================


class Pr428MergeReadyGateIntegrationTest(unittest.TestCase):
    """Integration tests for the evaluate_readiness entry point."""

    def setUp(self) -> None:
        self.module = load_merge_ready_gate()

    def test_merge_ready_requires_every_gate_to_pass(self) -> None:
        result = self.module.evaluate_readiness(merge_ready_evidence())

        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])
        self.assertEqual(HEAD_SHA, result["headSha"])
        self.assertIn("PR #428", result["summary"])
        self.assertIn("issue-reporting", result["summary"].lower())

    def test_any_missing_criterion_returns_not_merge_ready_with_explicit_blockers(self) -> None:
        evidence = merge_ready_evidence()
        evidence["githubActions"]["checks"][1]["conclusion"] = "failure"
        evidence["qaEvidence"]["gatedSmoke"]["statusTxt"]["outcome"] = "gated-not-run"
        evidence["qualityAuditCycles"][2]["clean"] = False

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("github-actions-not-green", result["blockers"])
        self.assertIn("gated-smoke-not-run", result["blockers"])
        self.assertIn("final-quality-audit-cycle-not-clean", result["blockers"])

    def test_missing_evidence_is_reported_as_blockers_not_success(self) -> None:
        evidence = {
            "repository": "rysweet/RabbitHole",
            "prNumber": EXPECTED_PR,
            "branch": EXPECTED_BRANCH,
            "headSha": HEAD_SHA,
        }

        result = self.module.evaluate_readiness(evidence)

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertIn("missing-pr-head-evidence", result["blockers"])
        self.assertIn("missing-qa-evidence", result["blockers"])
        self.assertIn("missing-github-actions-evidence", result["blockers"])
        self.assertNotIn("MERGE_READY", result.get("summary", ""))

    def test_open_quality_finding_produces_not_merge_ready(self) -> None:
        evidence = merge_ready_evidence()
        evidence["qualityAuditCycles"].append(
            {
                "cycle": 4,
                "seek": "Post-review readiness",
                "validate": "Confirmed unresolved review blocker",
                "fix": "Pending",
                "clean": False,
                "unresolvedFindings": ["review-blocker"],
            }
        )

        result = self.module.evaluate_readiness(evidence)
        self.assertEqual("NOT_MERGE_READY", result["status"])


# ===========================================================================
# 5-file plumbing sync contract tests
# ===========================================================================


class IssueReportingPlumbingSyncTest(unittest.TestCase):
    """Verify the issue-reporting-smoke workflow is wired in all 5 plumbing files."""

    def test_scenario_yaml_exists_with_correct_workflow(self) -> None:
        self.assertTrue(
            SCENARIO_YAML.exists(),
            f"Missing scenario YAML: {SCENARIO_YAML.relative_to(REPO_ROOT)}",
        )
        import yaml  # noqa: delayed import — not all envs have PyYAML

        content = yaml.safe_load(SCENARIO_YAML.read_text())
        self.assertEqual("issue-reporting-smoke", content["workflow"])
        self.assertEqual("alice-desktop-issue-reporting-smoke", content["id"])
        self.assertEqual("gated-command-smoke", content["automationMode"])

    def test_scenario_yaml_argv_matches_focused_maven_command(self) -> None:
        import yaml

        content = yaml.safe_load(SCENARIO_YAML.read_text())
        argv = content["automation"]["argv"]
        self.assertEqual(FOCUSED_MAVEN_ARGV, argv)

    def test_schema_json_contains_workflow_enum_value(self) -> None:
        schema = json.loads(SCHEMA_JSON.read_text())
        workflow_enum = schema["properties"]["workflow"]["enum"]
        self.assertIn(
            "issue-reporting-smoke",
            workflow_enum,
            "issue-reporting-smoke missing from schema workflow enum",
        )

    def test_schema_json_contains_argv_oneof_entry(self) -> None:
        schema = json.loads(SCHEMA_JSON.read_text())
        argv_entries = schema["properties"]["automation"]["properties"]["argv"]["oneOf"]
        found = any(
            isinstance(entry, dict)
            and "prefixItems" in entry
            and any(
                isinstance(item, dict) and item.get("const") == "core/issue-reporting"
                for item in entry["prefixItems"]
            )
            for entry in argv_entries
        )
        self.assertTrue(found, "Schema argv oneOf missing core/issue-reporting entry")

    def test_validate_scenarios_sh_contains_workflow_value(self) -> None:
        content = VALIDATE_SH.read_text()
        self.assertIn(
            "issue-reporting-smoke",
            content,
            "validate-scenarios.sh missing issue-reporting-smoke workflow",
        )

    def test_run_scenario_sh_contains_argv_allowlist_block(self) -> None:
        content = RUN_SCENARIO_SH.read_text()
        self.assertIn(
            "core/issue-reporting",
            content,
            "run-scenario.sh missing core/issue-reporting argv allowlist block",
        )
        self.assertIn(
            "IssueSubmissionProgressWorkerTest",
            content,
            "run-scenario.sh missing IssueSubmissionProgressWorkerTest in argv allowlist",
        )

    def test_schema_contract_sh_contains_expected_argv_tuple(self) -> None:
        content = SCHEMA_CONTRACT_SH.read_text()
        self.assertIn(
            "IssueSubmissionProgressWorkerTest",
            content,
            "test-schema-contract.sh missing IssueSubmissionProgressWorkerTest argv tuple",
        )

    def test_scenario_count_matches_schema_workflow_enum_count(self) -> None:
        schema = json.loads(SCHEMA_JSON.read_text())
        workflow_enum = schema["properties"]["workflow"]["enum"]
        scenario_dir = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios"
        yaml_files = sorted(scenario_dir.glob("*.yaml"))
        self.assertEqual(
            len(workflow_enum),
            len(yaml_files),
            f"Schema has {len(workflow_enum)} workflows but {len(yaml_files)} scenario YAMLs",
        )


# ===========================================================================
# Documentation link contract tests
# ===========================================================================


class IssueReportingDocsContractTest(unittest.TestCase):
    """Verify issue-reporting docs exist and are linked from docs/index.md."""

    def test_all_three_docs_exist(self) -> None:
        for doc_path in ISSUE_REPORTING_DOCS:
            with self.subTest(doc=doc_path.name):
                self.assertTrue(
                    doc_path.exists(),
                    f"Missing doc: {doc_path.relative_to(REPO_ROOT)}",
                )

    def test_docs_index_links_all_three_issue_reporting_docs(self) -> None:
        index_path = REPO_ROOT / "docs" / "index.md"
        self.assertTrue(index_path.exists(), "docs/index.md does not exist")
        index_content = index_path.read_text()
        expected_links = [
            "reference/issue-submission-progress-worker.md",
            "howto/characterize-issue-submission-progress-worker.md",
            "tutorials/trace-issue-submission-progress-worker.md",
        ]
        for link in expected_links:
            with self.subTest(link=link):
                self.assertIn(
                    link,
                    index_content,
                    f"docs/index.md missing link to {link}",
                )

    def test_docs_index_has_issue_reporting_section_heading(self) -> None:
        index_content = (REPO_ROOT / "docs" / "index.md").read_text()
        self.assertIn(
            "## Issue-reporting characterization",
            index_content,
            "docs/index.md missing '## Issue-reporting characterization' section",
        )

    def test_reference_doc_covers_three_test_seams(self) -> None:
        ref_path = REPO_ROOT / "docs" / "reference" / "issue-submission-progress-worker.md"
        content = ref_path.read_text()
        for seam in (
            "doInternal_onBackgroundThread",
            "publishProgressMessage",
            "createIssueBuilder",
        ):
            with self.subTest(seam=seam):
                self.assertIn(seam, content, f"Reference doc missing seam: {seam}")

    def test_reference_doc_has_non_claims_section(self) -> None:
        ref_path = REPO_ROOT / "docs" / "reference" / "issue-submission-progress-worker.md"
        content = ref_path.read_text()
        self.assertIn("## Non-claims", content)

    def test_howto_doc_has_validation_commands(self) -> None:
        howto = REPO_ROOT / "docs" / "howto" / "characterize-issue-submission-progress-worker.md"
        content = howto.read_text()
        self.assertIn("validate-scenarios.sh", content)
        self.assertIn("test-schema-contract.sh", content)
        self.assertIn("IssueSubmissionProgressWorkerTest", content)


# ===========================================================================
# Java characterization test existence contract
# ===========================================================================


class IssueReportingJavaTestContractTest(unittest.TestCase):
    """Verify the Java characterization test structure matches the contract."""

    JAVA_TEST_PATH = (
        REPO_ROOT
        / "core"
        / "issue-reporting"
        / "src"
        / "test"
        / "java"
        / "org"
        / "lgna"
        / "issue"
        / "IssueSubmissionProgressWorkerTest.java"
    )

    def test_java_test_file_exists(self) -> None:
        self.assertTrue(
            self.JAVA_TEST_PATH.exists(),
            "IssueSubmissionProgressWorkerTest.java must exist",
        )

    def test_java_test_has_three_test_methods(self) -> None:
        content = self.JAVA_TEST_PATH.read_text()
        test_count = content.count("@Test")
        self.assertEqual(
            3,
            test_count,
            f"Expected 3 @Test methods, found {test_count}",
        )

    def test_java_test_has_success_path_method(self) -> None:
        content = self.JAVA_TEST_PATH.read_text()
        self.assertIn(
            "backgroundSubmissionPublishesStartThenEndAroundSubmissionResult",
            content,
        )

    def test_java_test_has_attachment_opt_out_method(self) -> None:
        content = self.JAVA_TEST_PATH.read_text()
        self.assertIn(
            "backgroundSubmissionCarriesAttachmentOptOutAndSuccessfulResult",
            content,
        )

    def test_java_test_has_exception_path_method(self) -> None:
        content = self.JAVA_TEST_PATH.read_text()
        self.assertIn(
            "backgroundSubmissionPropagatesSubmissionExceptionWithoutPublishingCompletion",
            content,
        )

    def test_java_test_uses_recording_harness_not_real_swing(self) -> None:
        content = self.JAVA_TEST_PATH.read_text()
        self.assertIn("RecordingIssueSubmissionProgressWorker", content)
        self.assertNotIn("JOptionPane", content)
        self.assertNotIn("JProgressPane", content)
        self.assertNotIn("SwingUtilities.invokeLater", content)

    def test_java_test_asserts_start_end_message_lifecycle(self) -> None:
        content = self.JAVA_TEST_PATH.read_text()
        self.assertIn("START_MESSAGE", content)
        self.assertIn("END_MESSAGE", content)


if __name__ == "__main__":
    unittest.main()
