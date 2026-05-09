import os
import subprocess
import tempfile
import unittest
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator


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
        with self.linked_worktree() as linked_worktree:
            nested_candidate = linked_worktree / "core" / "ide"
            result = self.run_guard(nested_candidate, "--print-root")

        self.assertEqual("", result.stderr)
        self.assertEqual(0, result.returncode)
        self.assertEqual(str(linked_worktree.resolve()), result.stdout.strip())

    def test_guard_accepts_clean_worktree_with_exact_head_noop_evidence(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            evidence_file = linked_worktree.parent / "readiness-evidence.md"
            evidence_file.write_text(self.exact_head_noop_evidence(head), encoding="utf-8")

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(evidence_file),
                "--expected-head",
                head,
            )

        self.assertEqual("", result.stderr)
        self.assertEqual(0, result.returncode)

    def test_guard_rejects_clean_worktree_noop_evidence_for_stale_head(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            stale_head = "0" * 40 if head != "0" * 40 else "1" * 40
            evidence_file = linked_worktree.parent / "readiness-evidence.md"
            evidence_file.write_text(self.exact_head_noop_evidence(stale_head), encoding="utf-8")

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(evidence_file),
                "--expected-head",
                head,
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("stale", combined_output)
        self.assertIn("expected head", combined_output)

    def test_guard_rejects_expected_head_that_is_not_worktree_head(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            stale_head = "0" * 40 if head != "0" * 40 else "1" * 40
            evidence_file = linked_worktree.parent / "readiness-evidence.md"
            evidence_file.write_text(self.exact_head_noop_evidence(stale_head), encoding="utf-8")

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(evidence_file),
                "--expected-head",
                stale_head,
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("worktree head", combined_output)

    def test_guard_rejects_clean_worktree_evidence_without_noop_justification(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            evidence_file = linked_worktree.parent / "readiness-evidence.md"
            evidence_file.write_text(
                "\n".join(
                    [
                        "PR: 402",
                        "Branch: wave6-project-reopen-edit-chain-1778302300",
                        "Base: develop",
                        f"PR head: {head}",
                        f"Local HEAD: {head}",
                        "Merge-base status: merge-base equals origin/develop",
                        "Worktree status: clean",
                        "Diff summary: limited to project archive reopen/edit characterization/readiness surfaces",
                        "Validation command: NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/story-api-migration -am -Dtest=org.lgna.project.io.IoUtilitiesTest test",
                        f"Validation result: exit 0 PASS at {head}",
                        "Checks: no scoped PR check blocker",
                        "Files modified: none",
                    ]
                ),
                encoding="utf-8",
            )

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(evidence_file),
                "--expected-head",
                head,
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("no-op justification", combined_output)

    def test_guard_rejects_clean_worktree_evidence_with_files_modified_claim(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            evidence_file = linked_worktree.parent / "readiness-evidence.md"
            evidence_file.write_text(
                self.exact_head_noop_evidence(head) + "\nFiles modified: none\n",
                encoding="utf-8",
            )

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(evidence_file),
                "--expected-head",
                head,
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("modified files", combined_output)

    def test_guard_rejects_noop_justification_without_expected_head(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            evidence_file = linked_worktree.parent / "readiness-evidence.md"
            evidence_file.write_text(
                "\n".join(
                    [
                        "PR: 402",
                        "Branch: wave6-project-reopen-edit-chain-1778302300",
                        "Base: develop",
                        f"PR head: {head}",
                        f"Local HEAD: {head}",
                        "Merge-base status: merge-base equals origin/develop",
                        "Worktree status: clean",
                        "Diff summary: limited to project archive reopen/edit characterization/readiness surfaces",
                        "Validation command: NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/story-api-migration -am -Dtest=org.lgna.project.io.IoUtilitiesTest test",
                        f"Validation result: exit 0 PASS at {head}",
                        "Checks: no scoped PR check blocker",
                        "No-op justification:",
                        "  PR 402 branch wave6-project-reopen-edit-chain-1778302300 already points at",
                        "  the validated PR head, local HEAD matches the PR head, merge-base equals origin/develop, the",
                        "  origin/develop...HEAD diff is limited to project archive reopen/edit",
                        "  characterization/readiness surfaces, focused archive reopen/edit validation",
                        "  passed, and no scoped PR check blocker requires a code or docs",
                        "  change.",
                    ]
                ),
                encoding="utf-8",
            )

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(evidence_file),
                "--expected-head",
                head,
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("no-op justification", combined_output)
        self.assertIn("expected head", combined_output)

    def test_guard_reports_missing_noop_evidence_file(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.git_output(linked_worktree, "rev-parse", "HEAD")
            missing_evidence_file = linked_worktree.parent / "missing-readiness-evidence.md"

            result = self.run_guard(
                linked_worktree,
                "--allow-noop-evidence",
                str(missing_evidence_file),
                "--expected-head",
                head,
            )

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("evidence", combined_output)
        self.assertIn("missing", combined_output)

    @contextmanager
    def linked_worktree(self) -> Iterator[Path]:
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
                yield linked_worktree
            finally:
                subprocess.run(
                    ["git", "worktree", "remove", "--force", str(linked_worktree)],
                    cwd=REPO_ROOT,
                    check=False,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                )

    def git_output(self, worktree: Path, *args: str) -> str:
        return subprocess.run(
            ["git", "-C", str(worktree), *args],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        ).stdout.strip()

    def exact_head_noop_evidence(self, head: str) -> str:
        return "\n".join(
            [
                "PR: 402",
                "Branch: wave6-project-reopen-edit-chain-1778302300",
                "Base: develop",
                f"PR head: {head}",
                f"Local HEAD: {head}",
                "Merge-base status: merge-base equals origin/develop",
                "Worktree status: clean",
                "Diff summary: limited to project archive reopen/edit characterization/readiness surfaces",
                "Validation command: NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/story-api-migration -am -Dtest=org.lgna.project.io.IoUtilitiesTest test",
                f"Validation result: exit 0 PASS at {head}",
                "Checks: no scoped PR check blocker",
                "No-op justification:",
                "  PR 402 branch wave6-project-reopen-edit-chain-1778302300 already points at",
                f"  {head}, local HEAD matches the PR head, merge-base equals origin/develop, the",
                "  origin/develop...HEAD diff is limited to project archive reopen/edit",
                "  characterization/readiness surfaces, focused archive reopen/edit validation",
                f"  passed at {head}, and no scoped PR check blocker requires a code or docs",
                "  change.",
            ]
        )

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
