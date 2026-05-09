"""Default workflow recovery guard and report helpers."""

import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, Optional, Sequence, Union


REQUIRED_REPORT_SECTIONS = (
    "Summary",
    "Files modified",
    "Validation",
    "Scope / bounded claims",
    "Readiness evidence",
)

UNSUPPORTED_RUN_GAP_CLAIMS = (
    "full world execution",
    "playback",
    "visible rendering correctness",
    "full UI automation",
    "Save completion",
    "grading",
    "Sims validation",
    "deployed installer success",
)


class RepoPathResolutionError(RuntimeError):
    """Raised when the recovery workflow cannot verify the PR worktree."""


class WorkflowReportError(RuntimeError):
    """Raised when a recovery report omits required structured output."""


@dataclass(frozen=True)
class RepoPathResolution:
    status: str
    inputPath: str
    resolvedRepoPath: str
    gitTopLevel: str
    branch: str
    headSha: str
    blocker: Optional[str]


@dataclass(frozen=True)
class NoOpGuardResult:
    outcome: str
    checkedPath: str
    branch: str
    headSha: str
    statusShort: str
    diffStat: str
    filesModified: list[str]


def _git(repo_or_input_path: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo_or_input_path), *args],
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.rstrip("\n")


def _resolve_git_top_level(input_path: Path, *, label: str) -> Path:
    try:
        top_level = _git(input_path, "rev-parse", "--show-toplevel")
    except (OSError, subprocess.CalledProcessError) as exc:
        raise RepoPathResolutionError(
            f"not-a-git-worktree: {label}={input_path.resolve()}"
        ) from exc
    return Path(top_level).resolve()


def resolve_repo_path(
    *,
    explicit_pr_worktree_path: Optional[Union[str, Path]] = None,
    expected_pr_worktree_path: Optional[Union[str, Path]] = None,
) -> RepoPathResolution:
    """Resolve and verify the authoritative Git worktree for recovery checks."""

    input_path = Path(explicit_pr_worktree_path) if explicit_pr_worktree_path else Path.cwd()
    input_path = input_path.resolve()
    resolved_repo_path = _resolve_git_top_level(input_path, label="inputPath")

    if expected_pr_worktree_path is not None:
        expected_input = Path(expected_pr_worktree_path).resolve()
        expected_repo_path = _resolve_git_top_level(expected_input, label="expectedPrWorktreePath")
        if resolved_repo_path != expected_repo_path:
            raise RepoPathResolutionError(
                "resolved-path-mismatch: "
                f"expected={expected_repo_path} resolved={resolved_repo_path} input={input_path}"
            )

    branch = _git(resolved_repo_path, "branch", "--show-current")
    head_sha = _git(resolved_repo_path, "rev-parse", "HEAD")
    return RepoPathResolution(
        status="resolved",
        inputPath=str(input_path),
        resolvedRepoPath=str(resolved_repo_path),
        gitTopLevel=str(resolved_repo_path),
        branch=branch,
        headSha=head_sha,
        blocker=None,
    )


def _parse_status_paths(status_short: str) -> list[str]:
    paths: list[str] = []
    for line in status_short.splitlines():
        if not line:
            continue
        path = line[3:] if len(line) > 3 else line
        if " -> " in path:
            path = path.rsplit(" -> ", 1)[1]
        paths.append(path)
    return paths


def evaluate_no_op_guard(
    resolution: RepoPathResolution,
    *,
    launcher_cwd: Optional[Union[str, Path]] = None,
) -> NoOpGuardResult:
    """Evaluate repository changes only in the resolved PR worktree."""

    del launcher_cwd
    repo_path = Path(resolution.resolvedRepoPath)
    status_short = _git(repo_path, "status", "--short")
    unstaged_stat = _git(repo_path, "diff", "--stat")
    staged_stat = _git(repo_path, "diff", "--cached", "--stat")
    diff_stat = "\n".join(part for part in (unstaged_stat, staged_stat) if part)
    files_modified = _parse_status_paths(status_short)
    outcome = "changes-present" if status_short or diff_stat else "no-changes"
    return NoOpGuardResult(
        outcome=outcome,
        checkedPath=str(repo_path.resolve()),
        branch=resolution.branch,
        headSha=resolution.headSha,
        statusShort=status_short,
        diffStat=diff_stat,
        filesModified=files_modified,
    )


def _validation_line(result: Mapping[str, object]) -> str:
    command = str(result.get("command", "")).strip()
    status = str(result.get("status", "")).strip()
    if command and status:
        return f"- {command}: {status}"
    if command:
        return f"- {command}"
    if status:
        return f"- {status}"
    return "- unspecified validation result"


def _render_mapping(mapping: Mapping[str, object]) -> list[str]:
    return [f"- {key}: {value}" for key, value in mapping.items()]


def render_workflow_report(
    *,
    summary: str,
    files_modified: Sequence[str],
    validation_results: Iterable[Mapping[str, object]],
    scope_bounded_claims: str,
    readiness_evidence: Optional[Mapping[str, object]],
) -> str:
    """Render the required recovery report sections in stable order."""

    validation_lines = [_validation_line(result) for result in validation_results]
    files_lines = [f"- {path}" for path in files_modified] or ["None"]
    readiness_lines = _render_mapping(readiness_evidence) if readiness_evidence else ["None"]
    sections = [
        ("Summary", [summary.strip() or "None"]),
        ("Files modified", files_lines),
        ("Validation", validation_lines or ["None"]),
        ("Scope / bounded claims", [scope_bounded_claims.strip() or "None"]),
        ("Readiness evidence", readiness_lines),
    ]
    report = "\n\n".join(f"{heading}\n" + "\n".join(lines) for heading, lines in sections)
    validate_workflow_report(report)
    return report + "\n"


def validate_workflow_report(report: str) -> None:
    missing = [section for section in REQUIRED_REPORT_SECTIONS if section not in report]
    if missing:
        raise WorkflowReportError(f"Missing required workflow report sections: {', '.join(missing)}")

    section_positions = [report.index(section) for section in REQUIRED_REPORT_SECTIONS]
    if section_positions != sorted(section_positions):
        raise WorkflowReportError("Workflow report sections are out of order")


def build_recovery_report(
    *,
    explicit_pr_worktree_path: Optional[Union[str, Path]] = None,
    expected_pr_worktree_path: Optional[Union[str, Path]] = None,
    validation_results: Iterable[Mapping[str, object]],
    scope: str,
) -> str:
    """Build a bounded default-workflow recovery report for the resolved checkout."""

    resolution = resolve_repo_path(
        explicit_pr_worktree_path=explicit_pr_worktree_path,
        expected_pr_worktree_path=expected_pr_worktree_path,
    )
    guard = evaluate_no_op_guard(resolution)
    readiness_evidence: Optional[dict[str, object]]
    if guard.outcome == "no-changes":
        readiness_evidence = {
            "repoPath": resolution.resolvedRepoPath,
            "branch": resolution.branch,
            "headSha": resolution.headSha,
            "statusShortBranch": _git(Path(resolution.resolvedRepoPath), "status", "--short", "--branch"),
        }
    else:
        readiness_evidence = {
            "repoPath": resolution.resolvedRepoPath,
            "branch": resolution.branch,
            "headSha": resolution.headSha,
            "statusShort": guard.statusShort,
        }

    unsupported = ", ".join(UNSUPPORTED_RUN_GAP_CLAIMS)
    bounded_claims = (
        f"{scope.strip()} This does not claim {unsupported}."
    )
    return render_workflow_report(
        summary=(
            "Default-workflow recovery checked the resolved PR worktree and kept "
            "claims bounded to the focused desktop Run execution gap report evidence."
        ),
        files_modified=guard.filesModified,
        validation_results=validation_results,
        scope_bounded_claims=bounded_claims,
        readiness_evidence=readiness_evidence,
    )
