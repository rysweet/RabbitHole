import importlib.util
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
FINALIZATION_GATE_PATH = REPO_ROOT / "scripts" / "pr402_finalization_gate.py"

HEAD_SHA = "0123456789abcdef0123456789abcdef01234567"
STALE_HEAD_SHA = "fedcba9876543210fedcba9876543210fedcba98"
EXPECTED_VALIDATION_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 "
    "mvn -pl core/story-api-migration -am -DfailIfNoTests=false "
    "-Dsurefire.failIfNoSpecifiedTests=false "
    "-Dtest=org.lgna.project.io.IoUtilitiesTest#"
    "savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported test"
)
CLASS_WIDE_VALIDATION_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 "
    "mvn -pl core/story-api-migration -am -DfailIfNoTests=false "
    "-Dsurefire.failIfNoSpecifiedTests=false "
    "-Dtest=org.lgna.project.io.IoUtilitiesTest test"
)
REQUIRED_CHECKS = (
    "GitGuardian Security Checks",
    "Alice Checkstyle CI/build (pull_request)",
    "Alice Coverage Reports/coverage (pull_request)",
    "Alice NetBeans Package CI/package-netbeans (pull_request)",
    "Alice Test CI/test (pull_request)",
)


def load_finalization_gate():
    if not FINALIZATION_GATE_PATH.exists():
        raise AssertionError(
            "scripts/pr402_finalization_gate.py must implement the workflow-owned "
            "PR #402 finalization evidence contract."
        )
    spec = importlib.util.spec_from_file_location(
        "pr402_finalization_gate",
        FINALIZATION_GATE_PATH,
    )
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def successful_checks() -> list[dict[str, str]]:
    return [
        {"name": check_name, "status": "COMPLETED", "conclusion": "SUCCESS"}
        for check_name in REQUIRED_CHECKS
    ]


def current_pr_body(
    head_sha: str = HEAD_SHA,
    *,
    command: str = EXPECTED_VALIDATION_COMMAND,
    checks_line: str | None = None,
    extra_line: str = "",
) -> str:
    checks = checks_line or f"- GitHub checks: green for `{head_sha}`"
    return "\n".join(
        [
            "## Merge-ready evidence",
            "",
            f"- Current PR head: `{head_sha}`",
            checks,
            "- Focused validation:",
            f"  `{command}`",
            f"- Focused validation result: passed for `{head_sha}`",
            "- Scope: evidence-only finalization; no source, behavior, broad documentation, unrelated QA, or upstream changes.",
            extra_line,
        ]
    )


def finalization_snapshot(
    *,
    observed_head: str = HEAD_SHA,
    reread_head: str = HEAD_SHA,
    pr_body: str | None = None,
    checks_head: str = HEAD_SHA,
    checks: list[dict[str, str]] | None = None,
    validation_command: str = EXPECTED_VALIDATION_COMMAND,
    validation_exit_code: int = 0,
    validation_head: str = HEAD_SHA,
) -> dict:
    return {
        "prNumber": 402,
        "observedHeadSha": observed_head,
        "rereadHeadSha": reread_head,
        "checksHeadSha": checks_head,
        "statusCheckRollup": checks if checks is not None else successful_checks(),
        "prBody": pr_body if pr_body is not None else current_pr_body(observed_head),
        "validation": {
            "command": validation_command,
            "exitCode": validation_exit_code,
            "testSelector": (
                "org.lgna.project.io.IoUtilitiesTest#"
                "savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported"
            ),
            "tweedleLangInitialized": True,
            "validationHeadSha": validation_head,
        },
        "scope": "evidence-only",
    }


class Pr402FinalizationUnitContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_finalization_gate()

    def test_focused_validation_command_is_exact_archive_reopen_edit_method(self) -> None:
        self.assertEqual(
            ["git", "submodule", "update", "--init", "tweedle-lang"],
            self.module.tweedle_submodule_command(),
        )
        self.assertEqual(
            EXPECTED_VALIDATION_COMMAND,
            self.module.focused_validation_command(),
        )
        self.assertNotIn(
            CLASS_WIDE_VALIDATION_COMMAND,
            [self.module.focused_validation_command()],
            "Finalization must not substitute the class-wide IoUtilitiesTest selector for the focused method.",
        )

    def test_checks_require_same_observed_head_and_all_green_results(self) -> None:
        self.assertEqual(
            [],
            self.module.evaluate_checks(HEAD_SHA, HEAD_SHA, successful_checks()),
        )

        self.assertIn(
            "github-checks-stale-head",
            self.module.evaluate_checks(HEAD_SHA, STALE_HEAD_SHA, successful_checks()),
        )

        failing_checks = successful_checks()
        failing_checks[0] = {
            "name": REQUIRED_CHECKS[0],
            "status": "COMPLETED",
            "conclusion": "FAILURE",
        }
        self.assertIn(
            "github-checks-not-green",
            self.module.evaluate_checks(HEAD_SHA, HEAD_SHA, failing_checks),
        )

        pending_checks = successful_checks()
        pending_checks[1] = {
            "name": REQUIRED_CHECKS[1],
            "status": "IN_PROGRESS",
            "conclusion": "",
        }
        self.assertIn(
            "github-checks-not-complete",
            self.module.evaluate_checks(HEAD_SHA, HEAD_SHA, pending_checks),
        )

        arbitrary_single_check = [
            {
                "name": "Unrequired Check",
                "status": "COMPLETED",
                "conclusion": "SUCCESS",
            }
        ]
        self.assertIn(
            "github-checks-missing-required",
            self.module.evaluate_checks(HEAD_SHA, HEAD_SHA, arbitrary_single_check),
        )

    def test_pr_body_requires_current_head_green_checks_and_focused_validation(self) -> None:
        self.assertEqual(
            [],
            self.module.evaluate_pr_body(
                current_pr_body(),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )

        self.assertIn(
            "pr-body-stale-head",
            self.module.evaluate_pr_body(
                current_pr_body(STALE_HEAD_SHA),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )
        self.assertIn(
            "pr-body-checks-not-tied-to-head",
            self.module.evaluate_pr_body(
                current_pr_body(checks_line="- GitHub checks: green"),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )
        self.assertIn(
            "pr-body-checks-not-tied-to-head",
            self.module.evaluate_pr_body(
                current_pr_body(checks_line=f"- GitHub checks: not green for `{HEAD_SHA}`"),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )
        self.assertIn(
            "pr-body-missing-focused-validation",
            self.module.evaluate_pr_body(
                current_pr_body(command=CLASS_WIDE_VALIDATION_COMMAND),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )
        self.assertIn(
            "pr-body-stale-head",
            self.module.evaluate_pr_body(
                current_pr_body(extra_line=f"- Previous PR head: `{STALE_HEAD_SHA}`"),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )
        self.assertIn(
            "pr-body-qa-evidence-not-current",
            self.module.evaluate_pr_body(
                current_pr_body(extra_line="- Desktop QA scenario validation: passed"),
                HEAD_SHA,
                EXPECTED_VALIDATION_COMMAND,
            ),
        )

    def test_validation_result_requires_tweedle_init_exact_command_and_success(self) -> None:
        self.assertEqual(
            [],
            self.module.evaluate_validation(finalization_snapshot()["validation"], HEAD_SHA),
        )

        missing_tweedle = finalization_snapshot()["validation"]
        missing_tweedle["tweedleLangInitialized"] = False
        self.assertIn(
            "tweedle-lang-not-initialized",
            self.module.evaluate_validation(missing_tweedle, HEAD_SHA),
        )

        broad_selector = finalization_snapshot()["validation"]
        broad_selector["command"] = CLASS_WIDE_VALIDATION_COMMAND
        self.assertIn(
            "focused-validation-command-mismatch",
            self.module.evaluate_validation(broad_selector, HEAD_SHA),
        )

        failed_validation = finalization_snapshot()["validation"]
        failed_validation["exitCode"] = 1
        self.assertIn(
            "focused-validation-failed",
            self.module.evaluate_validation(failed_validation, HEAD_SHA),
        )

        missing_validation_head = finalization_snapshot()["validation"]
        missing_validation_head.pop("validationHeadSha")
        self.assertIn(
            "invalid-validation-head-sha",
            self.module.evaluate_validation(missing_validation_head, HEAD_SHA),
        )

        stale_validation_head = finalization_snapshot(validation_head=STALE_HEAD_SHA)["validation"]
        self.assertIn(
            "focused-validation-stale-head",
            self.module.evaluate_validation(stale_validation_head, HEAD_SHA),
        )


class Pr402FinalizationWorkflowContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_finalization_gate()

    def test_noop_requires_current_body_green_checks_and_focused_validation(self) -> None:
        result = self.module.decide_finalization(finalization_snapshot())

        self.assertEqual("NO_OP_GUARD", result["status"])
        self.assertFalse(result["updatePrBody"])
        self.assertEqual([], result["blockers"])
        self.assertIn(HEAD_SHA, result["noOpJustification"])
        self.assertIn("green checks", result["noOpJustification"].lower())
        self.assertIn(
            "IoUtilitiesTest#savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported",
            result["noOpJustification"],
        )
        self.assertIn("evidence-only", result["noOpJustification"])

    def test_stale_body_with_unchanged_head_updates_only_merge_ready_evidence(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(pr_body=current_pr_body(STALE_HEAD_SHA))
        )

        self.assertEqual("UPDATE_PR_BODY", result["status"])
        self.assertTrue(result["updatePrBody"])
        self.assertEqual([], result["sourceFilesToChange"])
        self.assertIn("## Merge-ready evidence", result["prBodyPatch"])
        self.assertIn(HEAD_SHA, result["prBodyPatch"])
        self.assertIn(EXPECTED_VALIDATION_COMMAND, result["prBodyPatch"])
        self.assertNotIn("Desktop QA scenario validation", result["prBodyPatch"])

    def test_head_change_before_body_update_blocks_mutation_as_stale_evidence(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(
                observed_head=HEAD_SHA,
                reread_head=STALE_HEAD_SHA,
                pr_body=current_pr_body(STALE_HEAD_SHA),
            )
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertFalse(result["updatePrBody"])
        self.assertIn("pr-head-changed-during-finalization", result["blockers"])
        self.assertNotIn("prBodyPatch", result)

    def test_untrusted_green_check_subset_blocks_noop(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(
                checks=[
                    {
                        "name": "Unrequired Check",
                        "status": "COMPLETED",
                        "conclusion": "SUCCESS",
                    }
                ]
            )
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertFalse(result["updatePrBody"])
        self.assertIn("github-checks-missing-required", result["blockers"])

    def test_stale_validation_head_blocks_noop(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(validation_head=STALE_HEAD_SHA)
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertFalse(result["updatePrBody"])
        self.assertIn("focused-validation-stale-head", result["blockers"])

    def test_top_level_validation_head_is_accepted_for_snapshot_compatibility(self) -> None:
        snapshot = finalization_snapshot()
        snapshot["validation"].pop("validationHeadSha")
        snapshot["validationHeadSha"] = HEAD_SHA

        result = self.module.decide_finalization(snapshot)

        self.assertEqual("NO_OP_GUARD", result["status"])
        self.assertFalse(result["updatePrBody"])

    def test_contradictory_body_check_evidence_is_refreshed_not_nooped(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(
                pr_body=current_pr_body(
                    checks_line=f"- GitHub checks: not green for `{HEAD_SHA}`"
                )
            )
        )

        self.assertEqual("UPDATE_PR_BODY", result["status"])
        self.assertTrue(result["updatePrBody"])
        self.assertIn("pr-body-checks-not-tied-to-head", result["bodyEvidenceBlockers"])

    def test_malformed_head_sha_blocks_noop_and_body_update(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(
                observed_head="abc123",
                reread_head="abc123",
                checks_head="abc123",
                validation_head="abc123",
                pr_body=current_pr_body("abc123"),
            )
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertFalse(result["updatePrBody"])
        self.assertIn("invalid-observed-head-sha", result["blockers"])
        self.assertIn("invalid-reread-head-sha", result["blockers"])
        self.assertIn("invalid-checks-head-sha", result["blockers"])
        self.assertIn("invalid-validation-head-sha", result["blockers"])
        self.assertNotIn("prBodyPatch", result)

    def test_unavailable_github_checks_are_reported_not_nooped(self) -> None:
        result = self.module.decide_finalization(
            finalization_snapshot(checks=[], pr_body=current_pr_body())
        )

        self.assertEqual("NOT_MERGE_READY", result["status"])
        self.assertFalse(result["updatePrBody"])
        self.assertIn("github-checks-unavailable", result["blockers"])
        self.assertNotIn("NO_OP_GUARD", result.get("summary", ""))


if __name__ == "__main__":
    unittest.main()
