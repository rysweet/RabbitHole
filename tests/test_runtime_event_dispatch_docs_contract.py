import argparse
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = REPO_ROOT / "tests" / "test_runtime_event_dispatch_docs_contract.py"
REFERENCE_PATH = REPO_ROOT / "docs" / "reference" / "generated-story-api-listener-source-characterization.md"
INDEX_PATH = REPO_ROOT / "docs" / "index.md"
EXPECTED_BRANCH = "wave6-runtime-event-dispatch-1778302300"
NON_CLAIM_TERMS = [
    "desktop runtime execution",
    "full world playback",
    "visible correctness",
    "grading",
    "Save completion",
    "full UI automation",
]


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


def verify_expected_branch(root: Path, expected_branch: str) -> int:
    branch = git_command("branch", "--show-current", cwd=root)
    if branch.returncode != 0:
        print(f"Unable to determine branch for Git worktree {root}.", file=sys.stderr)
        if branch.stderr:
            print(branch.stderr.strip(), file=sys.stderr)
        return branch.returncode

    actual_branch = branch.stdout.strip()
    if actual_branch != expected_branch:
        print(
            f"Git worktree {root} is on branch {actual_branch!r}; "
            f"expected branch {expected_branch!r}.",
            file=sys.stderr,
        )
        return 1

    print(f"Resolved Git worktree root: {root}")
    print(f"Verified expected branch: {actual_branch}")
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


def run_noop_guard(worktree: Path, expected_branch: str, check_only: bool) -> int:
    root, exit_code = resolve_git_root(worktree)
    if exit_code != 0:
        return exit_code

    assert root is not None
    exit_code = verify_expected_branch(root, expected_branch)
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
    parser.add_argument("--check-only", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    if "--guard-check" not in argv:
        unittest.main(argv=[sys.argv[0], *argv])
        return 0

    args = parse_guard_args(argv)
    return run_noop_guard(args.worktree, args.expected_branch, args.check_only)


def run_guard(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            sys.executable,
            str(CONTRACT_PATH),
            "--guard-check",
            *args,
        ],
        cwd=cwd or REPO_ROOT,
        capture_output=True,
        text=True,
    )


class RuntimeEventDispatchDocsContractTest(unittest.TestCase):
    def test_reference_is_linked_from_docs_index_with_bounded_headless_wording(self) -> None:
        index = INDEX_PATH.read_text(encoding="utf-8")

        self.assertIn(
            "[Headless Runtime Dispatch and Generated Story API Listener Source Characterization]"
            "(./reference/generated-story-api-listener-source-characterization.md)",
            index,
        )
        self.assertIn("bounded headless virtual-machine listener dispatch", index)

    def test_reference_names_guard_path_and_git_root_resolution_contract(self) -> None:
        reference = REFERENCE_PATH.read_text(encoding="utf-8")

        self.assertIn("tests/test_runtime_event_dispatch_docs_contract.py", reference)
        self.assertIn("git rev-parse --show-toplevel", reference)
        self.assertIn('git -C "$WORKTREE_ROOT"', reference)
        self.assertIn("fail closed", reference)
        self.assertIn("verifies the expected branch", reference)
        self.assertIn("not silently fall back", reference)

    def test_reference_keeps_claims_inside_headless_characterization_scope(self) -> None:
        reference = REFERENCE_PATH.read_text(encoding="utf-8")

        for non_claim in NON_CLAIM_TERMS:
            with self.subTest(non_claim=non_claim):
                self.assertIn(non_claim, reference)
        self.assertIn("claim desktop runtime execution", reference)
        self.assertIn("This characterization does not prove", reference)


class RuntimeEventDispatchNoOpGuardContractTest(unittest.TestCase):
    def test_guard_accepts_current_linked_worktree_and_reports_resolved_root(self) -> None:
        result = run_guard(
            "--worktree",
            str(REPO_ROOT / "docs"),
            "--expected-branch",
            EXPECTED_BRANCH,
            "--check-only",
        )

        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn(str(REPO_ROOT), result.stdout)
        self.assertIn(EXPECTED_BRANCH, result.stdout)

    def test_guard_rejects_non_git_path_without_clean_noop_fallback(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result = run_guard(
                "--worktree",
                directory,
                "--expected-branch",
                EXPECTED_BRANCH,
                "--check-only",
                cwd=Path(directory),
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = result.stdout + result.stderr
        self.assertIn("not inside a Git worktree", combined_output)
        self.assertNotIn("working tree clean", combined_output.lower())
        self.assertNotIn("no changes", combined_output.lower())

    def test_guard_rejects_unexpected_branch_before_status_checks(self) -> None:
        result = run_guard(
            "--worktree",
            str(REPO_ROOT),
            "--expected-branch",
            "not-the-runtime-event-dispatch-branch",
            "--check-only",
        )

        self.assertNotEqual(0, result.returncode)
        combined_output = result.stdout + result.stderr
        self.assertIn("expected branch", combined_output)
        self.assertIn("not-the-runtime-event-dispatch-branch", combined_output)


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
