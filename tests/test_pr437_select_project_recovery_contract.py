import json
import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
HOWTO_PATH = REPO_ROOT / "docs" / "howto" / "open-africa-full-through-select-project-atspi.md"
REFERENCE_PATH = REPO_ROOT / "docs" / "reference" / "select-project-africa-full-atspi-evidence.md"
VALIDATOR_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "validate-scenarios.sh"
RUNNER_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "run-scenario.sh"
SCHEMA_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "schema" / "scenario.schema.json"

TARGET_STARTER = "Africa Full"
TARGET_STARTER_PATH = "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
SELECT_PROJECT_WORKFLOW = "select-project-tab-click-smoke"
FOCUSED_SELECT_PROJECT_CHECKS = [
    "qa/outside-in/alice-desktop/tests/test-select-project-proof.sh",
    "qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh",
    "qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh",
]
DOWNSTREAM_NON_CLAIMS = [
    "full UI automation",
    "visible rendering correctness",
    "Save completion",
    "grading",
    "full lesson execution",
]
BLOCKERS = ["merge dirtiness", "missing evidence", "failing validation", "environment dependency"]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def heading_index(text: str, heading: str) -> int:
    index = text.find(heading)
    if index == -1:
        raise AssertionError(f"Missing heading: {heading}")
    return index


def fenced_block_containing(text: str, marker: str) -> str:
    for block in re.findall(r"```(?:bash|sh)?\n(.*?)\n```", text, flags=re.DOTALL):
        if marker in block:
            return block
    raise AssertionError(f"Missing fenced command block containing {marker!r}")


class Pr437SelectProjectRecoveryContractTest(unittest.TestCase):
    def test_pr_state_inspector_requires_exact_pr_head_and_check_metadata(self) -> None:
        text = read(REFERENCE_PATH)

        self.assertIn("gh pr view 437 --repo rysweet/RabbitHole", text)
        self.assertIn("headRefName", text)
        self.assertIn("headRefOid", text)
        self.assertIn("baseRefName", text)
        self.assertIn("isDraft", text)
        self.assertIn("mergeStateStatus", text)
        self.assertIn("statusCheckRollup", text)
        self.assertIn('PR_HEAD_OID="$(printf', text)
        self.assertIn("gh pr checkout 437 --repo rysweet/RabbitHole", text)
        self.assertIn('test "$LOCAL_HEAD_SHA" = "$PR_HEAD_OID"', text)
        self.assertIn("must not mark the PR ready from a `develop` checkout", text)

    def test_github_external_service_boundary_uses_retry_and_explicit_failure(self) -> None:
        text = read(REFERENCE_PATH)
        block = fenced_block_containing(text, "with_external_retry")

        self.assertIn("with_external_retry()", block)
        self.assertIn("GH_EXTERNAL_ATTEMPTS", block)
        self.assertIn("GH_EXTERNAL_RETRY_SECONDS", block)
        self.assertIn("external service call failed after", block)
        self.assertIn("with_external_retry gh pr view 437 --repo rysweet/RabbitHole", block)
        self.assertIn('PR_JSON="$(with_external_retry gh pr view 437 --repo rysweet/RabbitHole', block)
        self.assertIn('with_external_retry git fetch origin "$BASE_REF"', block)
        self.assertIn("GitHub CLI authentication", text)
        self.assertIn("network connectivity", text)
        self.assertIn("rate limiting", text)
        self.assertIn("environment dependency", text)
        self.assertIn("Do not use gh auth status --show-token", text)
        self.assertIn("Do not silently substitute cached, historical, or manually typed PR metadata", text)

    def test_howto_keeps_general_prerequisites_before_pr_specific_recovery_note(self) -> None:
        text = read(HOWTO_PATH)

        self.assertLess(
            heading_index(text, "## Prerequisites"),
            heading_index(text, "## PR #437 recovery note"),
            "The generic Select Project how-to should establish local prerequisites before the PR #437 finalization detour.",
        )

    def test_merge_reproduction_uses_trap_guarded_disposable_worktree_cleanup(self) -> None:
        text = read(REFERENCE_PATH)
        block = fenced_block_containing(text, "MERGE_WORKTREE")

        self.assertIn("git worktree add", block)
        self.assertIn("git merge --no-commit --no-ff", block)
        self.assertIn("git diff --name-only --diff-filter=U", block)
        self.assertIn("git merge --abort", block)
        self.assertIn("git worktree remove", block)
        self.assertRegex(
            block,
            r"(?s)\btrap\b.*git merge --abort.*git worktree remove",
            "Disposable merge checks need trap-guarded abort/remove cleanup so failures cannot leave staged merge state.",
        )

    def test_conflict_resolver_scope_records_exact_conflict_reason_resolution_and_next_action(self) -> None:
        text = read(REFERENCE_PATH)

        self.assertIn("git diff --name-only --diff-filter=U", text)
        for required_field in ["Conflict file", "Conflict reason", "Resolution", "Next action"]:
            with self.subTest(required_field=required_field):
                self.assertIn(required_field, text)
        self.assertIn("Resolve only files reported by the local merge check as unmerged", text)
        self.assertIn("If the conflict touches behavior-sensitive Alice code", text)
        self.assertIn("records `merge dirtiness` as the current blocker instead of guessing", text)

    def test_select_project_evidence_lane_lists_focused_checks_and_excludes_downstream_claims(self) -> None:
        howto = read(HOWTO_PATH)
        reference = read(REFERENCE_PATH)

        for check in FOCUSED_SELECT_PROJECT_CHECKS:
            with self.subTest(check=check):
                self.assertIn(check, howto)
                self.assertIn(Path(check).name, reference)
        self.assertIn("qa/outside-in/alice-desktop/runners/validate-scenarios.sh", howto)
        for non_claim in DOWNSTREAM_NON_CLAIMS:
            with self.subTest(non_claim=non_claim):
                self.assertIn(non_claim, howto)
                self.assertIn(non_claim, reference)

    def test_scenario_schema_runner_and_validator_share_target_contract_terms(self) -> None:
        schema = json.loads(read(SCHEMA_PATH))
        validator = read(VALIDATOR_PATH)
        runner = read(RUNNER_PATH)

        self.assertIn(SELECT_PROJECT_WORKFLOW, schema["properties"]["workflow"]["enum"])
        schema_text = json.dumps(schema)
        self.assertIn("targetStarter", schema_text)
        self.assertIn("displayName", schema_text)
        self.assertIn("repositoryPath", schema_text)

        with self.subTest(surface="validator"):
            self.assertIn("targetStarter", validator)
            self.assertIn(TARGET_STARTER, validator)
            self.assertIn(TARGET_STARTER_PATH, validator)
            self.assertIn(SELECT_PROJECT_WORKFLOW, validator)
        with self.subTest(surface="runner"):
            self.assertIn("targetStarter", runner)
            self.assertIn("targetStarter.displayName", runner)
            self.assertIn("targetStarter.repositoryPath", runner)
            self.assertIn("TARGET_STARTER_DISPLAY_NAME", runner)
            self.assertIn("TARGET_STARTER_REPO_PATH", runner)

    def test_verified_report_shape_separates_evidence_assumptions_and_single_blocker(self) -> None:
        howto = read(HOWTO_PATH)
        reference = read(REFERENCE_PATH)

        for heading in ["## Verified evidence", "## Unverified assumptions", "## Current blocker"]:
            with self.subTest(heading=heading):
                self.assertIn(heading, howto)
        for label in ["`Verified evidence`", "`Unverified assumptions`", "`Current blocker`"]:
            with self.subTest(label=label):
                self.assertIn(label, reference)
        for blocker in BLOCKERS:
            with self.subTest(blocker=blocker):
                self.assertIn(blocker, howto)
                self.assertIn(blocker, reference)
        self.assertIn("Do not publish multiple blockers as a grab bag", reference)
        self.assertIn("Use `Current blocker: None` only when every PR finalization gate passes", howto)

    def test_pr_finalization_gate_keeps_draft_until_merge_validation_evidence_and_claims_are_clean(self) -> None:
        text = read(REFERENCE_PATH)

        self.assertIn("PR #437 remains draft unless all finalization conditions are true", text)
        for ready_gate in [
            "PR head checked out",
            "Local merge state",
            "Conflict scope",
            "Focused validation",
            "Evidence truthfulness",
            "Claim boundary",
        ]:
            with self.subTest(ready_gate=ready_gate):
                self.assertIn(ready_gate, text)
        self.assertIn("Passing GitHub checks are useful context", text)
        self.assertIn("they do not make a dirty, under-evidenced, or overclaiming PR ready for review", text)

    def test_environment_dependency_blocker_cannot_be_reported_as_live_ui_proof(self) -> None:
        text = read(REFERENCE_PATH)

        self.assertIn("Live AT-SPI or supporting desktop/runtime dependencies prevent execution", text)
        self.assertIn("the docs name the missing dependency instead of claiming proof", text)
        self.assertIn("It must not report visible rendering correctness", text)
        self.assertIn("full UI automation", text)


if __name__ == "__main__":
    unittest.main()
