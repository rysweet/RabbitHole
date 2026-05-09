import os
import subprocess
import tempfile
import unittest
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator


REPO_ROOT = Path(__file__).resolve().parents[1]
GUARD_SCRIPT = REPO_ROOT / "scripts" / "project-archive-reopen-edit-noop-guard.sh"
PR_BRANCH = "wave6-project-reopen-edit-chain-1778302300"
BASE_BRANCH = "develop"
VALIDATION_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false "
    "-Dinstall4j.skip -DfailIfNoTests=false "
    "-Dsurefire.failIfNoSpecifiedTests=false -pl core/story-api-migration -am "
    "-Dtest=org.lgna.project.io.IoUtilitiesTest test"
)
SCOPE_EXCLUSIONS = (
    "Scope exclusions: no full desktop lesson automation, full UI automation, "
    "desktop Save-menu completion, visible rendering correctness, grading, "
    "grading correctness, full Save completion, full first-lesson completion, "
    "player runtime behavior, or broad migration correctness claims"
)
INCOMPLETE_SCOPE_EXCLUSIONS = (
    "Scope exclusions: no full desktop lesson automation, visible rendering "
    "correctness, grading, or full Save completion claims"
)
REQUIRED_PR_CHECKS = (
    "GitGuardian Security Checks",
    "Alice Checkstyle CI/build (pull_request)",
    "Alice Coverage Reports/coverage (pull_request)",
    "Alice NetBeans Package CI/package-netbeans (pull_request)",
    "Alice Test CI/test (pull_request)",
)
CURRENT_PR_CHECK_EVIDENCE = "Checks: " + "; ".join(
    f"{check_name} successful at current PR head" for check_name in REQUIRED_PR_CHECKS
)
INCOMPLETE_PR_CHECK_EVIDENCE = "Checks: " + "; ".join(
    f"{check_name} successful at current PR head" for check_name in REQUIRED_PR_CHECKS[1:]
)
FAILING_PR_CHECK_EVIDENCE = "Checks: " + "; ".join(
    [
        f"{REQUIRED_PR_CHECKS[0]} successful at current PR head",
        f"{REQUIRED_PR_CHECKS[1]} failure at current PR head",
        f"{REQUIRED_PR_CHECKS[2]} successful at current PR head",
        f"{REQUIRED_PR_CHECKS[3]} successful at current PR head",
        f"{REQUIRED_PR_CHECKS[4]} successful at current PR head",
    ]
)


class ProjectArchiveReopenEditNoopGuardTest(unittest.TestCase):
    _worktree_tempdir = None
    _linked_worktree = None
    _linked_worktree_head = None

    @classmethod
    def setUpClass(cls) -> None:
        cls._worktree_tempdir = tempfile.TemporaryDirectory(prefix="alice-archive-guard-")
        cls._linked_worktree = Path(cls._worktree_tempdir.name) / "linked-worktree"
        try:
            subprocess.run(
                ["git", "worktree", "add", "--detach", str(cls._linked_worktree), "HEAD"],
                cwd=REPO_ROOT,
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
        except (OSError, subprocess.CalledProcessError):
            cls._worktree_tempdir.cleanup()
            cls._worktree_tempdir = None
            cls._linked_worktree = None
            raise
        cls._linked_worktree_head = cls.git_output(cls._linked_worktree, "rev-parse", "HEAD")

    @classmethod
    def tearDownClass(cls) -> None:
        if cls._linked_worktree is not None:
            subprocess.run(
                ["git", "worktree", "remove", "--force", str(cls._linked_worktree)],
                cwd=REPO_ROOT,
                check=False,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            cls._linked_worktree = None
        if cls._worktree_tempdir is not None:
            cls._worktree_tempdir.cleanup()
            cls._worktree_tempdir = None
        cls._linked_worktree_head = None

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

    def test_guard_accepts_dirty_worktree_with_recovery_scope_change(self) -> None:
        with self.linked_worktree() as linked_worktree:
            scoped_file = linked_worktree / "docs" / "reference" / "pr-402-reopen-edit-recovery-output-contract.md"
            scoped_file.write_text(
                scoped_file.read_text(encoding="utf-8") + "\nScoped guard test change.\n",
                encoding="utf-8",
            )

            result = self.run_guard(linked_worktree)

        self.assertEqual("", result.stderr)
        self.assertEqual(0, result.returncode)

    def test_guard_rejects_dirty_worktree_with_only_unrelated_changes(self) -> None:
        with self.linked_worktree() as linked_worktree:
            unrelated_file = linked_worktree / "unrelated-review-note.txt"
            unrelated_file.write_text("not part of the project archive reopen/edit seam\n", encoding="utf-8")

            result = self.run_guard(linked_worktree)

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("outside project archive reopen/edit recovery scope", combined_output)
        self.assertIn("unrelated-review-note.txt", combined_output)

    def test_guard_rejects_dirty_worktree_mixing_scoped_and_unrelated_changes(self) -> None:
        with self.linked_worktree() as linked_worktree:
            scoped_file = linked_worktree / "scripts" / "project-archive-reopen-edit-noop-guard.sh"
            scoped_file.write_text(
                scoped_file.read_text(encoding="utf-8") + "\n# Scoped guard test change.\n",
                encoding="utf-8",
            )
            unrelated_file = linked_worktree / "README-unrelated-review-note.txt"
            unrelated_file.write_text("not part of the project archive reopen/edit seam\n", encoding="utf-8")

            result = self.run_guard(linked_worktree)

        self.assertNotEqual(0, result.returncode)
        combined_output = (result.stdout + result.stderr).lower()
        self.assertIn("outside project archive reopen/edit recovery scope", combined_output)
        self.assertIn("readme-unrelated-review-note.txt", combined_output)

    def test_guard_accepts_clean_worktree_with_exact_head_noop_evidence(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(linked_worktree, self.exact_head_noop_evidence(head))

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
            head = self.linked_worktree_head()
            stale_head = "0" * 40 if head != "0" * 40 else "1" * 40
            evidence_file = self.write_evidence(linked_worktree, self.exact_head_noop_evidence(stale_head))

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
            head = self.linked_worktree_head()
            stale_head = "0" * 40 if head != "0" * 40 else "1" * 40
            evidence_file = self.write_evidence(linked_worktree, self.exact_head_noop_evidence(stale_head))

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
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, include_noop_justification=False),
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
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head) + "\nFiles modified: none\n",
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

    def test_guard_rejects_clean_worktree_noop_evidence_without_scope_exclusions(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, include_scope_exclusions=False),
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
        self.assertIn("scope exclusions", combined_output)

    def test_guard_rejects_clean_worktree_noop_evidence_with_incomplete_scope_exclusions(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, scope_exclusions=INCOMPLETE_SCOPE_EXCLUSIONS),
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
        self.assertIn("scope exclusions", combined_output)
        self.assertIn("save-menu", combined_output)
        self.assertIn("player runtime", combined_output)

    def test_guard_rejects_clean_worktree_noop_evidence_with_out_of_scope_claims(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head)
                + "\n"
                + "\n".join(
                    [
                        "Full desktop lesson automation: proven",
                        "Visible rendering correctness: proven",
                        "Grading workflow: proven",
                        "Full Save completion: proven",
                    ]
                ),
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
        self.assertIn("out-of-scope", combined_output)

    def test_guard_rejects_noop_evidence_with_save_menu_player_or_lesson_completion_claims(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head)
                + "\n"
                + "\n".join(
                    [
                        "Desktop Save-menu completion: validated",
                        "Player runtime behavior: ready",
                        "Full first-lesson completion: complete",
                    ]
                ),
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
        self.assertIn("out-of-scope", combined_output)

    def test_guard_rejects_clean_worktree_noop_evidence_without_current_pr_check_evidence(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, include_current_pr_checks=False),
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
        self.assertIn("check", combined_output)
        self.assertIn("current pr head", combined_output)

    def test_guard_rejects_clean_worktree_noop_evidence_with_incomplete_current_pr_checks(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, pr_check_evidence=INCOMPLETE_PR_CHECK_EVIDENCE),
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
        self.assertIn("gitguardian security checks", combined_output)
        self.assertIn("check", combined_output)

    def test_guard_rejects_clean_worktree_noop_evidence_when_required_check_is_not_successful(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, pr_check_evidence=FAILING_PR_CHECK_EVIDENCE),
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
        self.assertIn("successful", combined_output)
        self.assertIn("current pr head", combined_output)

    def test_guard_rejects_noop_justification_without_expected_head(self) -> None:
        with self.linked_worktree() as linked_worktree:
            head = self.linked_worktree_head()
            evidence_file = self.write_evidence(
                linked_worktree,
                self.exact_head_noop_evidence(head, include_noop_head=False),
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
            head = self.linked_worktree_head()
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
        if self._linked_worktree is None:
            self.fail("linked guard worktree was not initialized")
        self.clean_linked_worktree()
        try:
            yield self._linked_worktree
        finally:
            self.clean_linked_worktree()

    def linked_worktree_head(self) -> str:
        if self._linked_worktree_head is None:
            self.fail("linked guard worktree HEAD was not initialized")
        return self._linked_worktree_head

    @staticmethod
    def git_output(worktree: Path, *args: str) -> str:
        return subprocess.run(
            ["git", "-C", str(worktree), *args],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        ).stdout.strip()

    def clean_linked_worktree(self) -> None:
        if self._linked_worktree is None:
            self.fail("linked guard worktree was not initialized")
        subprocess.run(
            ["git", "-C", str(self._linked_worktree), "reset", "--hard"],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        subprocess.run(
            ["git", "-C", str(self._linked_worktree), "clean", "-fd"],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

    def write_evidence(self, linked_worktree: Path, evidence_text: str) -> Path:
        evidence_file = linked_worktree.parent / "readiness-evidence.md"
        evidence_file.write_text(evidence_text, encoding="utf-8")
        return evidence_file

    def exact_head_noop_evidence(
        self,
        head: str,
        *,
        include_noop_justification: bool = True,
        include_scope_exclusions: bool = True,
        include_noop_head: bool = True,
        include_current_pr_checks: bool = True,
        scope_exclusions: str = SCOPE_EXCLUSIONS,
        pr_check_evidence: str = CURRENT_PR_CHECK_EVIDENCE,
    ) -> str:
        lines = [
            "PR: 402",
            f"Branch: {PR_BRANCH}",
            f"Base: {BASE_BRANCH}",
            f"PR head: {head}",
            f"Local HEAD: {head}",
            f"Remote branch HEAD: {head}",
            f"origin/{BASE_BRANCH} HEAD: {head}",
            f"Merge-base: {head}",
            f"Merge-base status: merge-base equals origin/{BASE_BRANCH}",
            "Worktree status: clean",
            "Diff summary: limited to project archive reopen/edit characterization/readiness surfaces",
            f"Validation command: {VALIDATION_COMMAND}",
            f"Validation result: exit 0 PASS at {head}",
            "Positive claim scope: repository-owned archive reopen/edit behavior only",
        ]
        if include_current_pr_checks:
            lines.append(pr_check_evidence)
        if include_scope_exclusions:
            lines.append(scope_exclusions)
        lines.append("Stale evidence note: older evidence must not be reused for a different HEAD")
        if include_noop_justification:
            validated_head = head if include_noop_head else "the validated PR head"
            state_head = head if include_noop_head else "the recorded base and merge-base"
            validation_result = f"passed at {head}," if include_noop_head else "passed,"
            lines.extend(
                [
                    "No-op justification:",
                    f"  PR 402 branch {PR_BRANCH} already points at",
                    f"  {validated_head}, local HEAD matches both the PR head and remote",
                    f"  branch head, origin/{BASE_BRANCH} is {state_head}, merge-base is {state_head},",
                    f"  merge-base equals origin/{BASE_BRANCH}, the origin/{BASE_BRANCH}...HEAD diff is limited to",
                    "  project archive reopen/edit characterization/readiness surfaces,",
                    "  focused archive reopen/edit validation",
                    f"  {validation_result} and no scoped PR check blocker requires a code or docs",
                    "  change.",
                ]
            )
        return "\n".join(lines)

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
