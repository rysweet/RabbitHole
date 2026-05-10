import argparse
import contextlib
import functools
import io
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = REPO_ROOT / "tests" / "test_runtime_event_dispatch_docs_contract.py"
REFERENCE_PATH = REPO_ROOT / "docs" / "reference" / "generated-story-api-listener-source-characterization.md"
INDEX_PATH = REPO_ROOT / "docs" / "index.md"
TEST_BRANCH = "runtime-event-dispatch-guard-test-branch"
RECOVERY_BRANCH = "wave6-runtime-event-dispatch-1778302300"
RECOVERY_PR_NUMBER = "403"
RECOVERY_PR_HEAD_COMMAND = (
    f'EXPECTED_PR_HEAD="$(gh pr view {RECOVERY_PR_NUMBER} --json headRefOid --jq .headRefOid)"'
)
RECOVERY_PR_VIEW_COMMAND = (
    f"gh pr view {RECOVERY_PR_NUMBER} "
    "--json number,title,headRefName,headRefOid,mergeStateStatus,reviewDecision,statusCheckRollup"
)
RECOVERY_PR_CHECKS_COMMAND = f"gh pr checks {RECOVERY_PR_NUMBER}"
RECOVERY_HEAD_CHECK = 'test "$(git rev-parse HEAD)" = "$EXPECTED_PR_HEAD"'
NON_CLAIM_TERMS = [
    "desktop runtime execution",
    "full world playback",
    "visible correctness",
    "grading",
    "Save completion",
    "full UI automation",
]


@functools.cache
def reference_text() -> str:
    return REFERENCE_PATH.read_text(encoding="utf-8")


@functools.cache
def docs_index_text() -> str:
    return INDEX_PATH.read_text(encoding="utf-8")


def git_command(*args: str, cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", "-C", str(cwd), *args],
        capture_output=True,
        text=True,
    )


def resolve_git_root(worktree: Path) -> tuple[Path | None, int]:
    resolved = git_command("rev-parse", "--show-toplevel", cwd=worktree)
    if resolved.returncode != 0:
        print(
            f"{worktree} is not inside a Git worktree; refusing no-op guard fallback.",
            file=sys.stderr,
        )
        if resolved.stderr:
            print(resolved.stderr.strip(), file=sys.stderr)
        return None, resolved.returncode

    return Path(resolved.stdout.strip()).resolve(), 0


def verify_git_value(root: Path, git_args: tuple[str, ...], expected: str, label: str) -> int:
    result = git_command(*git_args, cwd=root)
    if result.returncode != 0:
        print(f"Unable to determine {label} for Git worktree {root}.", file=sys.stderr)
        if result.stderr:
            print(result.stderr.strip(), file=sys.stderr)
        return result.returncode

    actual = result.stdout.strip()
    if actual != expected:
        print(
            f"Git worktree {root} is at {label} {actual!r}; "
            f"expected {label} {expected!r}.",
            file=sys.stderr,
        )
        return 1

    print(f"Verified expected {label}: {actual}")
    return 0


def verify_no_pending_changes(root: Path) -> int:
    status = git_command("status", "--short", cwd=root)
    if status.returncode != 0:
        print(f"Unable to inspect Git status for worktree {root}.", file=sys.stderr)
        if status.stderr:
            print(status.stderr.strip(), file=sys.stderr)
        return status.returncode

    if status.stdout.strip():
        print(
            f"Git worktree {root} has changes; refusing to report a no-op.",
            file=sys.stderr,
        )
        print(status.stdout.rstrip(), file=sys.stderr)
        return 1

    print(f"Git worktree {root} has no pending changes.")
    return 0


def run_noop_guard(
    worktree: Path,
    expected_branch: str,
    expected_head: str | None,
    check_only: bool,
) -> int:
    root, exit_code = resolve_git_root(worktree)
    if exit_code != 0:
        return exit_code

    assert root is not None
    print(f"Resolved Git worktree root: {root}")
    exit_code = verify_git_value(root, ("branch", "--show-current"), expected_branch, "branch")
    if exit_code != 0:
        return exit_code
    if expected_head is not None:
        exit_code = verify_git_value(root, ("rev-parse", "HEAD"), expected_head, "HEAD")
        if exit_code != 0:
            return exit_code
    if check_only:
        return 0

    return verify_no_pending_changes(root)


def parse_guard_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate the runtime-event-dispatch linked worktree before no-op checks."
    )
    parser.add_argument("--guard-check", action="store_true")
    parser.add_argument("--worktree", type=Path, required=True)
    parser.add_argument("--expected-branch", required=True)
    parser.add_argument("--expected-head")
    parser.add_argument("--check-only", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    if "--guard-check" not in argv:
        unittest.main(argv=[sys.argv[0], *argv])
        return 0

    args = parse_guard_args(argv)
    return run_noop_guard(
        args.worktree,
        args.expected_branch,
        args.expected_head,
        args.check_only,
    )


def run_guard(*args: str) -> subprocess.CompletedProcess[str]:
    guard_args = ["--guard-check", *args]
    stdout = io.StringIO()
    stderr = io.StringIO()
    with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
        try:
            returncode = main(guard_args)
        except SystemExit as exc:
            returncode = exc.code if isinstance(exc.code, int) else 1
    return subprocess.CompletedProcess(
        [sys.executable, str(CONTRACT_PATH), *guard_args],
        returncode,
        stdout.getvalue(),
        stderr.getvalue(),
    )


class RuntimeEventDispatchDocsContractTest(unittest.TestCase):
    def test_reference_is_linked_from_docs_index_with_bounded_headless_wording(self) -> None:
        index = docs_index_text()

        self.assertIn(
            "[Headless Runtime Dispatch and Generated Story API Listener Source Characterization]"
            "(./reference/generated-story-api-listener-source-characterization.md)",
            index,
        )
        self.assertIn("bounded headless virtual-machine listener dispatch", index)

    def test_reference_names_guard_path_and_git_root_resolution_contract(self) -> None:
        reference = reference_text()

        self.assertIn("tests/test_runtime_event_dispatch_docs_contract.py", reference)
        self.assertIn("git rev-parse --show-toplevel", reference)
        self.assertIn('git -C "$WORKTREE_ROOT"', reference)
        self.assertIn("fail closed", reference)
        self.assertIn("verifies the expected branch", reference)
        self.assertIn("not silently fall back", reference)

    def test_reference_keeps_claims_inside_headless_characterization_scope(self) -> None:
        reference = reference_text()

        for non_claim in NON_CLAIM_TERMS:
            with self.subTest(non_claim=non_claim):
                self.assertIn(non_claim, reference)
        self.assertIn("claim desktop runtime execution", reference)
        self.assertIn("This characterization does not prove", reference)


class RuntimeEventDispatchNoOpGuardContractTest(unittest.TestCase):
    def create_guard_worktree(self, directory: str, branch: str = TEST_BRANCH) -> Path:
        root = Path(directory)
        init = git_command("init", "--initial-branch", branch, cwd=root)
        self.assertEqual(0, init.returncode, init.stderr + init.stdout)

        worktree_path = root / "docs"
        worktree_path.mkdir()
        return worktree_path

    def commit_empty_guard_head(self, directory: str) -> str:
        root = Path(directory)
        commit = git_command(
            "-c",
            "user.email=runtime-event-dispatch@example.invalid",
            "-c",
            "user.name=Runtime Event Dispatch Test",
            "commit",
            "--allow-empty",
            "-m",
            "initial guard commit",
            cwd=root,
        )
        self.assertEqual(0, commit.returncode, commit.stderr + commit.stdout)
        head = git_command("rev-parse", "HEAD", cwd=root)
        self.assertEqual(0, head.returncode, head.stderr + head.stdout)
        return head.stdout.strip()

    def test_guard_accepts_linked_worktree_and_reports_resolved_root(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            worktree_path = self.create_guard_worktree(directory)
            result = run_guard(
                "--worktree",
                str(worktree_path),
                "--expected-branch",
                TEST_BRANCH,
                "--check-only",
            )

            self.assertEqual(0, result.returncode, result.stderr + result.stdout)
            self.assertIn(str(Path(directory).resolve()), result.stdout)
            self.assertIn(TEST_BRANCH, result.stdout)

    def test_guard_rejects_non_git_path_without_clean_noop_fallback(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result = run_guard(
                "--worktree",
                directory,
                "--expected-branch",
                TEST_BRANCH,
                "--check-only",
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = result.stdout + result.stderr
        self.assertIn("not inside a Git worktree", combined_output)
        self.assertNotIn("working tree clean", combined_output.lower())
        self.assertNotIn("no changes", combined_output.lower())

    def test_guard_rejects_unexpected_branch_before_status_checks(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            worktree_path = self.create_guard_worktree(directory)
            result = run_guard(
                "--worktree",
                str(worktree_path),
                "--expected-branch",
                "not-the-runtime-event-dispatch-branch",
                "--check-only",
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = result.stdout + result.stderr
        self.assertIn("expected branch", combined_output)
        self.assertIn("not-the-runtime-event-dispatch-branch", combined_output)

    def test_guard_accepts_expected_head_for_current_head_recovery(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            worktree_path = self.create_guard_worktree(directory)
            expected_head = self.commit_empty_guard_head(directory)

            result = run_guard(
                "--worktree",
                str(worktree_path),
                "--expected-branch",
                TEST_BRANCH,
                "--expected-head",
                expected_head,
                "--check-only",
            )

        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn(expected_head, result.stdout)

    def test_guard_rejects_unexpected_head_before_status_checks(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            worktree_path = self.create_guard_worktree(directory)
            self.commit_empty_guard_head(directory)
            (worktree_path / "dirty-marker.txt").write_text("dirty", encoding="utf-8")

            unexpected_head = "0" * 40
            result = run_guard(
                "--worktree",
                str(worktree_path),
                "--expected-branch",
                TEST_BRANCH,
                "--expected-head",
                unexpected_head,
                "--check-only",
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = result.stdout + result.stderr
        self.assertIn("expected HEAD", combined_output)
        self.assertIn(unexpected_head, combined_output)
        self.assertNotIn("has changes", combined_output)


class RuntimeEventDispatchPr403RecoveryContractTest(unittest.TestCase):
    def test_reference_documents_pr403_current_head_recovery_evidence(self) -> None:
        reference = reference_text()

        required_evidence = [
            "# Runtime Event Dispatch PR Recovery",
            "PR #403",
            RECOVERY_BRANCH,
            RECOVERY_PR_HEAD_COMMAND,
            'test "$(git rev-parse --abbrev-ref HEAD)" = '
            f'"{RECOVERY_BRANCH}"',
            RECOVERY_HEAD_CHECK,
            "git submodule update --init tweedle-lang",
            "NODE_OPTIONS=--max-old-space-size=32768 "
            "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
            "NODE_OPTIONS=--max-old-space-size=32768 "
            "bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
            "NODE_OPTIONS=--max-old-space-size=32768 "
            "mvn -pl core/ast -am -DfailIfNoTests=false "
            "-Dsurefire.failIfNoSpecifiedTests=false "
            "-Dtest=org.lgna.project.virtualmachine.VirtualMachineHeadlessRuntimeEventTest "
            "test -q",
            "NODE_OPTIONS=--max-old-space-size=32768 "
            "mvn -pl netbeans -am -DfailIfNoTests=false "
            "-Dsurefire.failIfNoSpecifiedTests=false "
            "-Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest "
            "test -q",
            "python3 -m unittest tests.test_runtime_event_dispatch_docs_contract",
            "python3 tests/test_runtime_event_dispatch_docs_contract.py "
            f"--guard-check --worktree . --expected-branch {RECOVERY_BRANCH} "
            '--expected-head "$EXPECTED_PR_HEAD"',
        ]
        for expected_text in required_evidence:
            with self.subTest(expected_text=expected_text):
                self.assertIn(expected_text, reference)

    def test_reference_pr403_recovery_section_does_not_pin_a_stale_sha(self) -> None:
        reference = reference_text()
        recovery_section = reference.split("## Runtime Event Dispatch PR Recovery", 1)[1].split(
            "## Feature intent",
            1,
        )[0]

        self.assertNotRegex(recovery_section, re.compile(r"\b[0-9a-f]{40}\b"))

    def test_reference_documents_no_manual_merge_and_noop_finalization_boundary(self) -> None:
        reference = reference_text()
        normalized_reference = " ".join(reference.split())

        required_boundary_text = [
            "Do not manually merge PR #403",
            RECOVERY_PR_VIEW_COMMAND,
            RECOVERY_PR_CHECKS_COMMAND,
            "mergeStateStatus",
            "reviewDecision",
            "statusCheckRollup",
            "No-op justification:",
            "checked-out HEAD matches the live PR head",
            "No-op",
        ]
        for expected_text in required_boundary_text:
            with self.subTest(expected_text=expected_text):
                self.assertIn(expected_text, reference)
        self.assertIn("no pending repository changes remain", normalized_reference)
        self.assertIn("no scoped defect is found", normalized_reference)
        self.assertIn("GitHub checks are green for the live PR head", normalized_reference)

        bounded_non_claims = [
            "full UI automation",
            "rendering correctness",
            "grading",
            "creative assessment",
            "lesson completion",
        ]
        for non_claim in bounded_non_claims:
            with self.subTest(non_claim=non_claim):
                self.assertIn(non_claim, reference)


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
