import importlib
import unittest


REQUIRED_REMOTE_REF = "origin/feat/issue-408-rabbithole-wave7-coverage-ratchet-lane-follow-defa"
FIXTURE_HEAD_SHA = "2b8a961d67f2365d38b9f6ea833e700e99e351a2"
STALE_HEAD_SHA = "ffffffffffffffffffffffffffffffffffffffff"
BASE_SHA = "2e1e43c3937a7d163bcc76f1882903a8ad31f1cc"
STALE_BASE_SHA = "0366dfa17f0f41e2d878c293a6c33fb1f841993a"
FOCUSED_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 "
    "mvn -pl core/issue-reporting -am -DfailIfNoTests=false "
    "-Dsurefire.failIfNoSpecifiedTests=false "
    "-Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest test"
)
REAL_PR_DIFF_FILES = [
    "core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java",
    "core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java",
    "docs/howto/characterize-issue-submission-progress-worker.md",
    "docs/index.md",
    "docs/reference/issue-submission-progress-worker.md",
    "docs/tutorials/trace-issue-submission-progress-worker.md",
    "pyproject.toml",
    "scripts/pr428_merge_ready_gate.py",
    "tests/test_pr428_merge_ready_gate.py",
]


def complete_evidence_package():
    return {
        "branch": {
            "current_ref": REQUIRED_REMOTE_REF,
            "local_head": FIXTURE_HEAD_SHA,
            "remote_head": FIXTURE_HEAD_SHA,
            "manual_merge_seen": False,
        },
        "base": {
            "base_ref": "origin/develop",
            "base_sha": BASE_SHA,
        },
        "expected_base_sha": BASE_SHA,
        "diff_files": REAL_PR_DIFF_FILES,
        "runnable_evidence": [{"command": FOCUSED_COMMAND, "passed": True, "head_sha": FIXTURE_HEAD_SHA}],
        "docs_impact": {"assessed": True, "files": ["docs/reference/issue-submission-progress-worker.md"]},
        "scenario_evidence": {
            "applicability": "not_applicable",
            "reason": (
                "No Alice desktop workflow impact because IssueSubmissionProgressWorker "
                "is a non-UI issue-reporting worker seam."
            ),
        },
        "quality_audit_cycles": [
            {"seek": "worker seam", "validate": "source review", "fix": "none", "clean": True},
            {"seek": "docs claims", "validate": "bounded-claim scan", "fix": "none", "clean": True},
            {"seek": "evidence gates", "validate": "PR body review", "fix": "none", "clean": True},
        ],
        "github_checks": [{"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS", "head_sha": FIXTURE_HEAD_SHA}],
        "pr_description": f"""
        Head validated: {FIXTURE_HEAD_SHA}
        Base validated: {BASE_SHA}
        Focused validation: {FOCUSED_COMMAND} passed.
        Module validation: core/issue-reporting passed.
        Docs impact: reference docs reviewed.
        Scenario evidence: not applicable; no Alice desktop workflow impact because this is a non-UI issue-reporting worker seam.
        Diff scope checked: origin/develop...HEAD includes only allowed worker, docs, version metadata, gate, and test files.
        Quality audit: three SEEK / VALIDATE / FIX cycles completed with a clean final cycle.
        GitHub Actions: all current-head checks completed successfully for {FIXTURE_HEAD_SHA}.
        Does not claim full UI automation, visible rendering correctness, grading,
        creative assessment, full lesson completion, project archive attachment contents,
        real issue-service submission, or full Tweedle/player decode.
        """,
        "expected_head_sha": FIXTURE_HEAD_SHA,
    }


def gate_module():
    return importlib.import_module("scripts.pr428_merge_ready_gate")


class Pr428MergeReadyGateContractTest(unittest.TestCase):
    def assertBlocked(self, result, expected_fragment: str) -> None:
        self.assertFalse(result.ready)
        self.assertTrue(
            any("NOT_MERGE_READY" in blocker and expected_fragment in blocker for blocker in result.blockers),
            result.blockers,
        )

    def test_branch_sync_accepts_only_exact_current_remote_head(self) -> None:
        gate = gate_module()

        synced = gate.validate_branch_sync(
            current_ref=REQUIRED_REMOTE_REF,
            local_head=FIXTURE_HEAD_SHA,
            remote_head=FIXTURE_HEAD_SHA,
            manual_merge_seen=False,
        )
        drifted = gate.validate_branch_sync(
            current_ref=REQUIRED_REMOTE_REF,
            local_head=STALE_HEAD_SHA,
            remote_head=FIXTURE_HEAD_SHA,
            manual_merge_seen=False,
        )
        manually_merged = gate.validate_branch_sync(
            current_ref=REQUIRED_REMOTE_REF,
            local_head=FIXTURE_HEAD_SHA,
            remote_head=FIXTURE_HEAD_SHA,
            manual_merge_seen=True,
        )
        malformed = gate.validate_branch_sync(
            current_ref=REQUIRED_REMOTE_REF,
            local_head="deadbeef",
            remote_head="deadbeef",
            manual_merge_seen=False,
        )

        self.assertTrue(synced.ready)
        self.assertEqual([], synced.blockers)
        self.assertBlocked(drifted, "remote head")
        self.assertBlocked(manually_merged, "manual merge")
        self.assertBlocked(malformed, "40-character hex commit SHA")

    def test_diff_scope_allows_only_worker_tests_docs_and_justified_metadata(self) -> None:
        gate = gate_module()
        unrelated_files = REAL_PR_DIFF_FILES + ["core/ide/src/main/java/org/alice/ide/Unrelated.java"]
        absolute_path = "/tmp/private-checkout/core/issue-reporting/IssueSubmissionProgressWorker.java"

        self.assertTrue(gate.audit_diff_scope(REAL_PR_DIFF_FILES).ready)
        self.assertBlocked(gate.audit_diff_scope(unrelated_files), "diff scope")
        self.assertBlocked(gate.audit_diff_scope(REAL_PR_DIFF_FILES + ["pom.xml"]), "pom.xml")
        invalid_path_result = gate.audit_diff_scope([absolute_path])
        self.assertBlocked(invalid_path_result, "repository-relative")
        self.assertFalse(any(absolute_path in blocker for blocker in invalid_path_result.blockers))

    def test_complete_evidence_requires_current_develop_base_sha(self) -> None:
        gate = gate_module()
        evidence = complete_evidence_package()
        stale_base = {
            **evidence,
            "base": {"base_ref": "origin/develop", "base_sha": STALE_BASE_SHA},
        }
        missing_base = {key: value for key, value in evidence.items() if key != "base"}
        missing_expected_base = {
            key: value for key, value in evidence.items() if key != "expected_base_sha"
        }
        malformed_base = {
            **evidence,
            "base": {"base_ref": "origin/develop", "base_sha": "deadbeef"},
            "expected_base_sha": "deadbeef",
        }

        self.assertTrue(gate.evaluate_merge_ready(evidence).ready)
        self.assertBlocked(gate.evaluate_merge_ready(stale_base), "base")
        self.assertBlocked(gate.evaluate_merge_ready(missing_base), "base")
        self.assertBlocked(gate.evaluate_merge_ready(missing_expected_base), "expected base SHA")
        self.assertBlocked(gate.evaluate_merge_ready(malformed_base), "40-character hex commit SHA")

    def test_github_actions_are_tied_to_current_pr_head(self) -> None:
        gate = gate_module()
        evidence = complete_evidence_package()
        stale_check_head = {
            **evidence,
            "github_checks": [
                {"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS", "head_sha": STALE_HEAD_SHA}
            ],
        }
        missing_check_head = {
            **evidence,
            "github_checks": [{"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS"}],
        }
        malformed_check_head = {
            **evidence,
            "expected_head_sha": "deadbeef",
            "branch": {
                **evidence["branch"],
                "local_head": "deadbeef",
                "remote_head": "deadbeef",
            },
            "github_checks": [
                {"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS", "head_sha": "deadbeef"}
            ],
        }

        self.assertTrue(gate.evaluate_merge_ready(evidence).ready)
        self.assertBlocked(gate.evaluate_merge_ready(stale_check_head), "current head")
        self.assertBlocked(gate.evaluate_merge_ready(missing_check_head), "40-character hex commit SHA")
        self.assertBlocked(gate.evaluate_merge_ready(malformed_check_head), "40-character hex commit SHA")

    def test_scenario_non_applicability_requires_specific_non_ui_worker_rationale(self) -> None:
        gate = gate_module()
        specific = {
            "applicability": "not_applicable",
            "reason": (
                "No Alice desktop workflow impact because IssueSubmissionProgressWorker "
                "is a non-UI issue-reporting worker seam."
            ),
        }
        vague = {"applicability": "not_applicable", "reason": "scenario skipped"}

        self.assertTrue(gate.validate_scenario_evidence(specific).ready)
        self.assertBlocked(gate.validate_scenario_evidence(vague), "non-UI issue-reporting worker seam")

    def test_validation_commands_reject_timeout_wrappers_and_require_focused_worker_evidence(self) -> None:
        gate = gate_module()

        focused = gate.validate_runnable_evidence(
            [
                {
                    "command": FOCUSED_COMMAND,
                    "passed": True,
                    "head_sha": FIXTURE_HEAD_SHA,
                }
            ],
            expected_head_sha=FIXTURE_HEAD_SHA,
        )
        timeout_wrapped = gate.validate_runnable_evidence(
            [
                {
                    "command": "timeout 300 " + FOCUSED_COMMAND,
                    "passed": True,
                    "head_sha": FIXTURE_HEAD_SHA,
                }
            ]
        )
        stale_head = gate.validate_runnable_evidence(
            [
                {
                    "command": FOCUSED_COMMAND,
                    "passed": True,
                    "head_sha": STALE_HEAD_SHA,
                }
            ],
            expected_head_sha=FIXTURE_HEAD_SHA,
        )
        malformed_head = gate.validate_runnable_evidence(
            [
                {
                    "command": FOCUSED_COMMAND,
                    "passed": True,
                    "head_sha": "deadbeef",
                }
            ],
            expected_head_sha="deadbeef",
        )
        command_with_secret = "mvn 'unterminated --token=secret-value"
        malformed_command = gate.validate_runnable_evidence(
            [
                {
                    "command": command_with_secret,
                    "passed": True,
                    "head_sha": FIXTURE_HEAD_SHA,
                }
            ],
            expected_head_sha=FIXTURE_HEAD_SHA,
        )

        self.assertTrue(focused.ready)
        self.assertBlocked(timeout_wrapped, "timeout")
        self.assertBlocked(stale_head, "current head")
        self.assertBlocked(malformed_head, "40-character hex commit SHA")
        self.assertBlocked(malformed_command, "not parseable")
        self.assertFalse(any(command_with_secret in blocker for blocker in malformed_command.blockers))

    def test_quality_audit_requires_three_seek_validate_fix_cycles_with_clean_final_cycle(self) -> None:
        gate = gate_module()
        cycles = [
            {"seek": "worker seam", "validate": "source review", "fix": "none", "clean": True},
            {"seek": "docs claims", "validate": "bounded-claim scan", "fix": "none", "clean": True},
            {"seek": "evidence gates", "validate": "PR body review", "fix": "none", "clean": True},
        ]

        self.assertTrue(gate.validate_quality_audit_cycles(cycles).ready)
        self.assertBlocked(gate.validate_quality_audit_cycles(cycles[:2]), "three")
        self.assertBlocked(
            gate.validate_quality_audit_cycles(cycles[:-1] + [{**cycles[-1], "clean": False}]),
            "clean final",
        )

    def test_green_actions_alone_are_not_merge_ready(self) -> None:
        gate = gate_module()
        checks = [
            {"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS"},
            {"name": "test", "status": "COMPLETED", "conclusion": "SUCCESS"},
        ]

        self.assertTrue(gate.validate_github_actions(checks).ready)
        self.assertBlocked(
            gate.evaluate_merge_ready({"github_checks": checks}),
            "focused QA",
        )

    def test_github_actions_accepts_external_gh_pr_checks_shape(self) -> None:
        gate = gate_module()
        checks = [
            {"name": "coverage", "state": "SUCCESS", "bucket": "pass"},
            {"name": "optional-skip", "state": "SKIPPED", "bucket": "skipping"},
        ]
        pending_checks = checks + [{"name": "package", "state": "PENDING", "bucket": "pending"}]
        failed_checks = checks + [{"name": "test", "state": "FAILURE", "bucket": "fail"}]

        self.assertTrue(gate.validate_github_actions(checks).ready)
        self.assertEqual(
            [
                {"name": "coverage", "status": "COMPLETED", "conclusion": "SUCCESS"},
                {"name": "optional-skip", "status": "COMPLETED", "conclusion": "SKIPPED"},
            ],
            gate.parse_github_checks_json(
                '[{"name":"coverage","state":"SUCCESS","bucket":"pass"},'
                '{"name":"optional-skip","state":"SKIPPED","bucket":"skipping"}]'
            ),
        )
        self.assertBlocked(gate.validate_github_actions(pending_checks), "not completed")
        self.assertBlocked(gate.validate_github_actions(failed_checks), "not green")

    def test_pr_description_must_contain_current_head_evidence_and_bounded_non_claims(self) -> None:
        gate = gate_module()
        body = f"""
        Head validated: {FIXTURE_HEAD_SHA}
        Base validated: {BASE_SHA}
        Focused validation: {FOCUSED_COMMAND} passed.
        Module validation: core/issue-reporting passed.
        Docs impact: reference, how-to, tutorial, and index reviewed.
        Scenario evidence: not applicable; no Alice desktop workflow impact because this is a non-UI issue-reporting worker seam.
        Diff scope checked: origin/develop...HEAD includes only allowed worker, docs, version metadata, gate, and test files.
        Quality audit: three SEEK / VALIDATE / FIX cycles completed with a clean final cycle.
        GitHub Actions: all current-head checks completed successfully for {FIXTURE_HEAD_SHA}.
        Does not claim full UI automation, visible rendering correctness, grading,
        creative assessment, full lesson completion, project archive attachment contents,
        real issue-service submission, or full Tweedle/player decode.
        """
        headings_only_body = f"""
        Head validated: {FIXTURE_HEAD_SHA}
        Focused validation:
        Docs impact:
        Scenario evidence:
        Diff scope:
        Quality audit: SEEK / VALIDATE / FIX clean final cycle.
        GitHub Actions:
        Does not claim:
        """
        overclaiming_body = body + "\nThis proves full UI automation and visible rendering correctness."

        self.assertTrue(
            gate.validate_pr_description(
                body,
                expected_head_sha=FIXTURE_HEAD_SHA,
                expected_base_sha=BASE_SHA,
            ).ready
        )
        self.assertBlocked(
            gate.validate_pr_description(body.replace(FIXTURE_HEAD_SHA, STALE_HEAD_SHA), FIXTURE_HEAD_SHA),
            "head",
        )
        self.assertBlocked(
            gate.validate_pr_description(
                body.replace(BASE_SHA, STALE_BASE_SHA),
                expected_head_sha=FIXTURE_HEAD_SHA,
                expected_base_sha=BASE_SHA,
            ),
            "base",
        )
        self.assertBlocked(gate.validate_pr_description(headings_only_body, FIXTURE_HEAD_SHA), "exact focused")
        self.assertBlocked(gate.validate_pr_description(overclaiming_body, FIXTURE_HEAD_SHA), "overclaim")

    def test_owner_free_exit_with_rate_limit_text_is_classified_as_rate_limit_not_ready(self) -> None:
        gate = gate_module()

        classification = gate.classify_owner_free_exit(
            exit_code=0,
            owner=None,
            stderr="GitHub API rate limit exceeded while resolving PR owner",
        )

        self.assertEqual("RATE_LIMIT", classification.kind)
        self.assertFalse(classification.ready)
        self.assertIn("NOT_MERGE_READY", classification.blocker)

    def test_complete_evidence_package_is_merge_ready_only_when_every_gate_passes(self) -> None:
        gate = gate_module()
        evidence = complete_evidence_package()

        ready = gate.evaluate_merge_ready(evidence)
        missing_docs = gate.evaluate_merge_ready({**evidence, "docs_impact": {"assessed": False}})

        self.assertTrue(ready.ready)
        self.assertEqual([], ready.blockers)
        self.assertBlocked(missing_docs, "docs impact")


if __name__ == "__main__":
    unittest.main()
