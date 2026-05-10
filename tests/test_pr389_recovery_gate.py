import importlib.util
import copy
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RECOVERY_GATE_PATH = REPO_ROOT / "scripts" / "pr389_recovery_gate.py"
EXPECTED_BRANCH = "wave5-netbeans-ant-1778295741"
EXPECTED_REMOTE_REF = f"origin/{EXPECTED_BRANCH}"
EXPECTED_PR = 389
HEAD_SHA = "a7c843c15a24c21065b151c3f2a78f3a83a37f38"
FOCUSED_MAVEN_ARGV = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "netbeans",
    "-am",
    "-Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest",
    "test",
]
FORBIDDEN_CLAIMS = [
    "full UI automation",
    "visible rendering correctness",
    "grading",
    "creative assessment",
    "full lesson completion",
    "full Tweedle/player decode",
]
PR389_CHANGED_FILES = [
    ".copilot-evidence/default-workflow-attempt.log",
    "docs/howto/alice-desktop-outside-in-qa.md",
    "docs/howto/finalize-exported-netbeans-ant-smoke-recovery.md",
    "docs/index.md",
    "docs/reference/alice-desktop-outside-in-qa.md",
    "docs/reference/exported-netbeans-ant-project-behavior.md",
    "docs/reference/gadugi-exported-launcher-evidence.md",
    "docs/reference/modernization-corpus-manifest.json",
    "docs/reference/modernization-scorecard.md",
    "netbeans/src/test/java/org/alice/netbeans/Alice3LibraryClasspathTestSupport.java",
    "netbeans/src/test/java/org/alice/netbeans/project/Alice3ProjectTemplateAntSmokeTest.java",
    "pyproject.toml",
    "qa/outside-in/alice-desktop/README.md",
    "qa/outside-in/alice-desktop/runners/run-scenario.sh",
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/scenarios/exported-project-smoke.yaml",
    "qa/outside-in/alice-desktop/schema/scenario.schema.json",
    "qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "scripts/pr389_recovery_gate.py",
    "tests/test_pr389_recovery_gate.py",
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
        lambda evidence: evidence["commands"].append("cd repo && gh pr merge 389"),
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
        lambda evidence: evidence["commands"].append("echo ok;gh pr merge 389"),
        "manual-merge-used",
    ),
    (
        "nested semicolon manual merge",
        lambda evidence: evidence["commands"].append(["bash", "-lc", "echo ok;gh pr merge 389"]),
        "manual-merge-used",
    ),
    (
        "adjacent operator manual merge",
        lambda evidence: evidence["commands"].append("true&&gh pr merge 389"),
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


def load_recovery_gate():
    if not RECOVERY_GATE_PATH.exists():
        raise AssertionError(
            "scripts/pr389_recovery_gate.py must implement the PR #389 recovery readiness contract."
        )
    spec = importlib.util.spec_from_file_location("pr389_recovery_gate", RECOVERY_GATE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def merge_ready_evidence() -> dict:
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
                "alice-desktop-exported-project-smoke",
                "--evidence-dir",
                "qa/outside-in/alice-desktop/evidence/pr389",
            ],
            FOCUSED_MAVEN_ARGV,
            ["gh", "pr", "checks", "389", "--repo", "rysweet/RabbitHole"],
        ],
        "changedFiles": PR389_CHANGED_FILES,
        "qaEvidence": {
            "scenarioValidation": {"outcome": "passed"},
            "runnerContract": {"outcome": "passed"},
            "schemaContract": {"outcome": "passed"},
            "gatedSmoke": {
                "scenario": "alice-desktop-exported-project-smoke",
                "workflow": "exported-project-ant-build-smoke",
                "automationMode": "gated-command-smoke",
                "gate": "ALICE_QA_RUN_GATED_SMOKES",
                "gateValue": "1",
                "statusTxt": {
                    "outcome": "passed",
                    "exitCode": "0",
                    "argv": " ".join(FOCUSED_MAVEN_ARGV),
                },
                "commandLog": {
                    "contains": [
                        "Alice3ProjectTemplateAntSmokeTest",
                        "ANT_RUN_PROBE_OK",
                        "ANT_RESOURCE_PROBE_OK",
                        "ANT_RUNTIME_CONFIGURATION_PROBE_OK",
                        "ANT_TEST_MAIN_PROBE_OK",
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
            "testClass": "org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest",
        },
        "qualityAuditCycles": [
            {
                "cycle": 1,
                "seek": "Recovery docs, branch, and QA wiring",
                "validate": "Confirmed command and branch contracts",
                "fix": "Updated stale evidence wording",
                "clean": False,
            },
            {
                "cycle": 2,
                "seek": "NetBeans Ant smoke behavior",
                "validate": "Confirmed focused smoke selectors and bounded claims",
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
                "docs/howto/finalize-exported-netbeans-ant-smoke-recovery.md",
                "docs/reference/exported-netbeans-ant-project-behavior.md",
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


class Pr389RecoveryGateUnitTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_recovery_gate()
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

    def test_head_verifier_requires_exact_authoritative_pr_head(self) -> None:
        self.assert_no_blockers("verify_head")
        self.assert_has_blocker(
            "verify_head",
            lambda evidence: evidence.update({"prHeadSha": "deadbeef"}),
            "pr-head-sha-mismatch",
        )
        self.assert_has_blocker(
            "verify_head",
            lambda evidence: evidence.update({"remoteRef": "origin/other-branch"}),
            "wrong-authoritative-branch",
        )

    def test_diff_scope_verifier_allows_only_pr_applicable_paths(self) -> None:
        self.assert_no_blockers("verify_diff_scope")
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda evidence: evidence["changedFiles"].append("installer/signing/secrets.txt"),
            "unfocused-diff-scope",
        )
        self.assert_has_blocker(
            "verify_diff_scope",
            lambda evidence: evidence["changedFiles"].append("docs/reference/unrelated.md"),
            "unfocused-diff-scope",
        )

    def test_diff_scope_allowlist_matches_actual_pr_diff(self) -> None:
        current_branch = subprocess.check_output(
            ["git", "branch", "--show-current"],
            cwd=REPO_ROOT,
            text=True,
        ).strip()
        if current_branch != EXPECTED_BRANCH:
            self.skipTest(
                "PR #389 actual-diff assertion only applies on the PR #389 recovery branch."
            )

        actual_changed_files = subprocess.check_output(
            ["git", "--no-pager", "diff", "--name-only", "origin/develop...HEAD"],
            cwd=REPO_ROOT,
            text=True,
        ).splitlines()
        self.assertEqual(PR389_CHANGED_FILES, actual_changed_files)

        evidence = copy.deepcopy(self.evidence)
        evidence["changedFiles"] = actual_changed_files
        self.assertEqual([], self.module.verify_diff_scope(evidence))

    def test_qa_scenario_verifier_requires_executed_gated_exported_ant_smoke(self) -> None:
        self.assert_no_blockers("verify_qa_evidence")
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda evidence: evidence["qaEvidence"]["gatedSmoke"]["statusTxt"].update(
                {"outcome": "gated-not-run"}
            ),
            "gated-smoke-not-run",
        )
        self.assert_has_blocker(
            "verify_qa_evidence",
            lambda evidence: evidence["qaEvidence"]["gatedSmoke"]["statusTxt"].update(
                {"argv": "mvn -Dtest=ProjectCodeGeneratorStandaloneProjectTest test"}
            ),
            "missing-focused-ant-smoke-argv",
        )

    def test_maven_verifier_requires_tweedle_submodule_and_focused_no_sims_command(self) -> None:
        self.assert_no_blockers("verify_maven_evidence")
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda evidence: evidence["mavenEvidence"].update({"tweedleLangInitialized": False}),
            "tweedle-lang-not-initialized",
        )
        self.assert_has_blocker(
            "verify_maven_evidence",
            lambda evidence: evidence["mavenEvidence"].update({"nodeOptions": ""}),
            "missing-node-options",
        )

    def test_quality_audit_verifier_requires_three_seek_validate_fix_cycles_and_clean_final_cycle(self) -> None:
        self.assert_no_blockers("verify_quality_audit")
        self.assert_has_blocker(
            "verify_quality_audit",
            lambda evidence: evidence.update(
                {"qualityAuditCycles": evidence["qualityAuditCycles"][:2]}
            ),
            "insufficient-quality-audit-cycles",
        )
        self.assert_has_blocker(
            "verify_quality_audit",
            lambda evidence: evidence["qualityAuditCycles"][2].update({"clean": False}),
            "final-quality-audit-cycle-not-clean",
        )
        self.assert_has_blocker(
            "verify_quality_audit",
            lambda evidence: evidence["qualityAuditCycles"].append(
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
        evidence = copy.deepcopy(self.evidence)
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

    def test_docs_impact_verifier_rejects_unbounded_or_unreviewed_claims(self) -> None:
        self.assert_no_blockers("verify_docs_impact")
        self.assert_has_blocker(
            "verify_docs_impact",
            lambda evidence: evidence["docsImpact"].update({"reviewed": False}),
            "docs-impact-not-reviewed",
        )
        self.assert_has_blocker(
            "verify_docs_impact",
            lambda evidence: evidence["docsImpact"].update(
                {"boundedClaimsOnly": False, "forbiddenClaims": FORBIDDEN_CLAIMS[:1]}
            ),
            "docs-overclaim-unproven-behavior",
        )

    def test_github_actions_verifier_requires_green_checks_on_the_same_head_sha(self) -> None:
        self.assert_no_blockers("verify_github_actions")
        self.assert_has_blocker(
            "verify_github_actions",
            lambda evidence: evidence["githubActions"].update({"headSha": "deadbeef"}),
            "github-actions-stale-head",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda evidence: evidence["githubActions"]["checks"][0].update(
                {"conclusion": "failure"}
            ),
            "github-actions-not-green",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda evidence: evidence["githubActions"]["checks"][0].update(
                {"status": "in_progress", "conclusion": None}
            ),
            "github-actions-not-complete",
        )
        self.assert_has_blocker(
            "verify_github_actions",
            lambda evidence: evidence["githubActions"].update(
                {"checks": [{"name": "test", "status": "completed", "conclusion": "success"}]}
            ),
            "github-actions-required-check-missing",
        )

    def test_github_actions_verifier_accepts_gh_pr_view_status_check_rollup_casing(self) -> None:
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

    def test_pr_description_verifier_requires_current_head_evidence_and_bounded_claims(self) -> None:
        self.assert_no_blockers("verify_pr_description")
        self.assert_has_blocker(
            "verify_pr_description",
            lambda evidence: evidence["prDescription"].update({"hasQaEvidence": False}),
            "pr-description-missing-qa-evidence",
        )
        self.assert_has_blocker(
            "verify_pr_description",
            lambda evidence: evidence["prDescription"].update({"headSha": "deadbeef"}),
            "pr-description-stale-head",
        )
        self.assert_has_blocker(
            "verify_pr_description",
            lambda evidence: evidence["prDescription"].update(
                {"hasBoundedClaims": False, "forbiddenClaims": FORBIDDEN_CLAIMS[:2]}
            ),
            "pr-description-overclaims-unproven-behavior",
        )

    def test_command_verifier_rejects_manual_merge_and_timeout_wrappers(self) -> None:
        self.assert_no_blockers("verify_command_safety")
        for name, mutate, expected_blocker in COMMAND_SAFETY_BLOCKER_CASES:
            with self.subTest(name=name):
                self.assert_has_blocker(
                    "verify_command_safety",
                    mutate,
                    expected_blocker,
                )


class Pr389RecoveryGateIntegrationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_recovery_gate()

    def test_merge_ready_requires_every_gate_to_pass(self) -> None:
        result = self.module.evaluate_readiness(merge_ready_evidence())

        self.assertEqual("MERGE_READY", result["status"])
        self.assertEqual([], result["blockers"])
        self.assertEqual(HEAD_SHA, result["headSha"])
        self.assertEqual(
            "Bounded exported NetBeans Ant smoke recovery evidence is complete for PR #389.",
            result["summary"],
        )

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

    def test_missing_evidence_is_reported_as_blockers_instead_of_success_shaped_defaults(self) -> None:
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


if __name__ == "__main__":
    unittest.main()
