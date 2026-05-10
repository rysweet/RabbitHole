import importlib.util
import re
import subprocess
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = REPO_ROOT / "scripts" / "default_workflow_recovery.py"

REQUIRED_REPORT_SECTIONS = [
    "Summary",
    "Files modified",
    "Validation",
    "Scope / bounded claims",
    "Readiness evidence",
]

FOCUSED_RUN_GAP_VALIDATION = [
    "git submodule update --init tweedle-lang",
    "NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    (
        "NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ide -am "
        "-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false "
        "-Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest test"
    ),
]

UNSUPPORTED_RUN_GAP_CLAIMS = [
    "full world execution",
    "playback",
    "visible rendering correctness",
    "full UI automation",
    "Save completion",
    "grading",
    "Sims validation",
    "deployed installer success",
]


def load_recovery_module():
    if not MODULE_PATH.is_file():
        raise AssertionError(
            "Expected scripts/default_workflow_recovery.py to implement the default-workflow "
            "recovery contract."
        )
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


def run_git(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=repo,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def init_git_repo(root: Path) -> Path:
    root.mkdir(parents=True)
    run_git(root, "init", "--initial-branch", "main")
    run_git(root, "config", "user.email", "alice@example.invalid")
    run_git(root, "config", "user.name", "Alice Test")
    (root / "tracked.txt").write_text("baseline\n", encoding="utf-8")
    run_git(root, "add", "tracked.txt")
    run_git(root, "commit", "-m", "Initial test repo")
    return root


class DefaultWorkflowRepoPathResolverContractTest(unittest.TestCase):
    def test_resolves_explicit_subdirectory_to_authoritative_git_worktree(self) -> None:
        module = load_recovery_module()
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = init_git_repo(Path(temp_dir) / "pr-worktree")
            nested = repo / "docs" / "reference"
            nested.mkdir(parents=True)

            resolution = module.resolve_repo_path(
                explicit_pr_worktree_path=nested,
                expected_pr_worktree_path=repo,
            )

        self.assertEqual("resolved", field(resolution, "status"))
        self.assertEqual(str(repo.resolve()), field(resolution, "resolvedRepoPath"))
        self.assertEqual(str(repo.resolve()), field(resolution, "gitTopLevel"))
        self.assertEqual(str(nested.resolve()), field(resolution, "inputPath"))
        self.assertEqual("main", field(resolution, "branch"))
        self.assertRegex(field(resolution, "headSha"), r"^[0-9a-f]{40}$")
        self.assertIsNone(field(resolution, "blocker"))

    def test_rejects_non_git_path_instead_of_falling_back_to_launcher_cwd(self) -> None:
        module = load_recovery_module()
        error_type = getattr(module, "RepoPathResolutionError")
        with tempfile.TemporaryDirectory() as temp_dir:
            non_git_path = Path(temp_dir) / "stale-linked-worktree"
            non_git_path.mkdir()

            with self.assertRaises(error_type) as raised:
                module.resolve_repo_path(explicit_pr_worktree_path=non_git_path)

        message = str(raised.exception)
        self.assertIn("not-a-git-worktree", message)
        self.assertIn("stale-linked-worktree", message)

    def test_rejects_path_that_resolves_outside_expected_pr_worktree(self) -> None:
        module = load_recovery_module()
        error_type = getattr(module, "RepoPathResolutionError")
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = init_git_repo(Path(temp_dir) / "actual-pr-worktree")
            other_repo = init_git_repo(Path(temp_dir) / "wrong-worktree")

            with self.assertRaises(error_type) as raised:
                module.resolve_repo_path(
                    explicit_pr_worktree_path=other_repo,
                    expected_pr_worktree_path=repo,
                )

        message = str(raised.exception)
        self.assertIn("resolved-path-mismatch", message)
        self.assertIn(str(repo.resolve()), message)
        self.assertIn(str(other_repo.resolve()), message)


class DefaultWorkflowNoOpGuardContractTest(unittest.TestCase):
    def test_no_op_guard_checks_resolved_repo_path_and_reports_dirty_files(self) -> None:
        module = load_recovery_module()
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = init_git_repo(Path(temp_dir) / "pr-worktree")
            nested = repo / "core" / "ide"
            nested.mkdir(parents=True)
            stale_non_git_path = Path(temp_dir) / "stale-session-path"
            stale_non_git_path.mkdir()
            (repo / "tracked.txt").write_text("changed in PR worktree\n", encoding="utf-8")

            resolution = module.resolve_repo_path(
                explicit_pr_worktree_path=nested,
                expected_pr_worktree_path=repo,
            )
            guard = module.evaluate_no_op_guard(resolution, launcher_cwd=stale_non_git_path)

        self.assertEqual("changes-present", field(guard, "outcome"))
        self.assertEqual(str(repo.resolve()), field(guard, "checkedPath"))
        self.assertEqual("main", field(guard, "branch"))
        self.assertIn("M tracked.txt", field(guard, "statusShort"))
        self.assertIn("tracked.txt", field(guard, "diffStat"))
        self.assertEqual(["tracked.txt"], field(guard, "filesModified"))

    def test_no_op_guard_reports_no_changes_only_for_clean_resolved_repo(self) -> None:
        module = load_recovery_module()
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = init_git_repo(Path(temp_dir) / "clean-pr-worktree")

            resolution = module.resolve_repo_path(
                explicit_pr_worktree_path=repo,
                expected_pr_worktree_path=repo,
            )
            guard = module.evaluate_no_op_guard(resolution)

        self.assertEqual("no-changes", field(guard, "outcome"))
        self.assertEqual("", field(guard, "statusShort"))
        self.assertEqual("", field(guard, "diffStat"))
        self.assertEqual([], field(guard, "filesModified"))


class DefaultWorkflowReportContractTest(unittest.TestCase):
    def test_report_always_emits_required_sections_and_none_for_empty_files_modified(self) -> None:
        module = load_recovery_module()

        report = module.render_workflow_report(
            summary="Merged current develop into the existing PR branch with no feature changes.",
            files_modified=[],
            validation_results=[
                {"command": FOCUSED_RUN_GAP_VALIDATION[0], "status": "passed"},
            ],
            scope_bounded_claims=(
                "Validated only the bounded desktop Run execution gap report evidence. "
                "This does not claim full world execution, playback, visible rendering correctness, "
                "full UI automation, Save completion, grading, Sims validation, or deployed installer success."
            ),
            readiness_evidence={
                "repoPath": "/worktrees/wave6-run-execution-gap-1778302300",
                "branch": "wave6-run-execution-gap-1778302300",
                "headSha": "0123456789abcdef0123456789abcdef01234567",
                "statusShortBranch": "## wave6-run-execution-gap-1778302300",
            },
        )

        section_positions = [report.index(section) for section in REQUIRED_REPORT_SECTIONS]
        self.assertEqual(sorted(section_positions), section_positions)
        self.assertRegex(report, r"(?m)^Files modified\nNone\n")
        self.assertIn(FOCUSED_RUN_GAP_VALIDATION[0], report)
        self.assertIn("0123456789abcdef0123456789abcdef01234567", report)

    def test_report_lists_changed_files_relative_to_resolved_repo(self) -> None:
        module = load_recovery_module()

        report = module.render_workflow_report(
            summary="Focused run-gap recovery changed tests and docs.",
            files_modified=[
                "tests/test_default_workflow_recovery_contract.py",
                "docs/reference/default-workflow-recovery-report.md",
            ],
            validation_results=[],
            scope_bounded_claims="Validated only the bounded desktop Run execution gap report evidence.",
            readiness_evidence=None,
        )

        self.assertRegex(
            report,
            (
                r"(?m)^Files modified\n"
                r"- tests/test_default_workflow_recovery_contract\.py\n"
                r"- docs/reference/default-workflow-recovery-report\.md\n"
            ),
        )
        self.assertRegex(report, r"(?m)^Validation\nNone\n")
        self.assertRegex(report, r"(?m)^Readiness evidence\nNone\n")

    def test_report_rejects_missing_files_modified_section(self) -> None:
        module = load_recovery_module()
        error_type = getattr(module, "WorkflowReportError")

        with self.assertRaises(error_type) as raised:
            module.validate_workflow_report(
                "Summary\nRecovered PR branch.\n\nValidation\nNone\n",
            )

        self.assertIn("Files modified", str(raised.exception))


class DefaultWorkflowRunExecutionGapIntegrationContractTest(unittest.TestCase):
    def test_recovery_report_uses_focused_run_gap_validation_and_bounded_claims(self) -> None:
        module = load_recovery_module()
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = init_git_repo(Path(temp_dir) / "run-gap-pr-worktree")
            nested = repo / "qa" / "outside-in" / "alice-desktop"
            nested.mkdir(parents=True)
            (repo / "tracked.txt").write_text("focused run gap doc change\n", encoding="utf-8")

            report = module.build_recovery_report(
                explicit_pr_worktree_path=nested,
                expected_pr_worktree_path=repo,
                validation_results=[
                    {"command": command, "status": "passed"}
                    for command in FOCUSED_RUN_GAP_VALIDATION
                ],
                scope=(
                    "Validated only the bounded desktop Run execution gap report and its blocker evidence."
                ),
            )

        for section in REQUIRED_REPORT_SECTIONS:
            self.assertIn(section, report)
        self.assertIn("tracked.txt", report)
        for command in FOCUSED_RUN_GAP_VALIDATION:
            self.assertIn(command, report)
        for unsupported_claim in UNSUPPORTED_RUN_GAP_CLAIMS:
            self.assertRegex(
                report,
                rf"(?i)(?:does not claim|do not claim|not claim)[^.\n]*{re.escape(unsupported_claim)}",
                msg=f"Recovery report must explicitly avoid claiming {unsupported_claim!r}.",
            )
        self.assertIn(str(repo.resolve()), report)


if __name__ == "__main__":
    unittest.main()
