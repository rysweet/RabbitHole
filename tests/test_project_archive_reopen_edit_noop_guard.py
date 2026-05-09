import os
import subprocess
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
GUARD_SCRIPT = REPO_ROOT / "scripts" / "project-archive-reopen-edit-noop-guard.sh"


class ProjectArchiveReopenEditNoopGuardTest(unittest.TestCase):
    def test_guard_script_exists_as_repo_owned_entrypoint(self) -> None:
        self.assertTrue(
            GUARD_SCRIPT.is_file(),
            f"missing repo-owned guard script: {GUARD_SCRIPT}",
        )
        self.assertTrue(
            os.access(GUARD_SCRIPT, os.X_OK),
            f"guard script must be executable: {GUARD_SCRIPT}",
        )

    def test_guard_rejects_non_git_candidate_path_clearly(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            result = self.run_guard(Path(temporary_directory), "--print-root")

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("git", combined_output)
        self.assertIn("worktree", combined_output)

    def test_guard_resolves_linked_worktree_root_from_nested_candidate_path(self) -> None:
        with tempfile.TemporaryDirectory(prefix="alice-archive-guard-") as temporary_directory:
            linked_worktree = Path(temporary_directory) / "linked-worktree"
            subprocess.run(
                ["git", "worktree", "add", "--detach", str(linked_worktree), "HEAD"],
                cwd=REPO_ROOT,
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            try:
                nested_candidate = linked_worktree / "core" / "ide"
                result = self.run_guard(nested_candidate, "--print-root")
            finally:
                subprocess.run(
                    ["git", "worktree", "remove", "--force", str(linked_worktree)],
                    cwd=REPO_ROOT,
                    check=False,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )

        self.assertEqual("", result.stderr)
        self.assertEqual(0, result.returncode)
        self.assertEqual(str(linked_worktree.resolve()), result.stdout.strip())

    def run_guard(self, candidate_path: Path, *extra_args: str) -> subprocess.CompletedProcess[str]:
        if not GUARD_SCRIPT.is_file():
            self.fail(f"missing repo-owned guard script: {GUARD_SCRIPT}")
        return subprocess.run(
            [str(GUARD_SCRIPT), str(candidate_path), *extra_args],
            cwd=REPO_ROOT,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )


if __name__ == "__main__":
    unittest.main()
