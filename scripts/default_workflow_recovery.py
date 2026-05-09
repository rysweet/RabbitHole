"""Default workflow recovery guard and report helpers."""

import json
import logging
import re
import shlex
import subprocess
import time
from collections.abc import Iterable as IterableABC
from dataclasses import asdict, dataclass, is_dataclass
from pathlib import Path
from typing import Callable, Iterable, Mapping, Optional, Sequence, Union


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

MERGE_READY_REPORT_SECTIONS = (
    "Summary",
    "Files modified",
    "Validation",
    "QA / scenario evidence",
    "Docs impact",
    "Scope / bounded claims",
    "GitHub and PR evidence",
    "Quality-audit cycles",
    "Readiness decision",
)

NODE_OPTIONS_REQUIREMENT = "NODE_OPTIONS=--max-old-space-size=32768"

UNSUPPORTED_MERGE_READY_CLAIMS = (
    "full UI automation",
    "visible rendering correctness",
    "grading",
    "creative assessment",
    "full lesson completion",
    "full Tweedle/player decode",
    "full world execution",
)

MAVEN_COMMAND_PATTERN = re.compile(r"(^|\s)mvn(\s|$)")

CLAIM_NEGATION_PATTERN = re.compile(
    r"(does not claim|do not claim|not claim|doesn't claim|without claiming|"
    r"no claim of|does not prove|do not prove|not prove|explicitly avoids?)"
)

FOCUSED_DIFF_PREFIXES = (
    "scripts/",
    "tests/",
    "docs/",
    "qa/outside-in/alice-desktop/",
    "core/ide/src/test/",
    "core/ide/src/main/java/org/alice/tools/",
)

DEFAULT_PR_METADATA_FIELDS = (
    "number",
    "headRefName",
    "headRefOid",
    "baseRefName",
    "mergeStateStatus",
    "mergeable",
    "state",
    "isDraft",
    "reviewDecision",
    "statusCheckRollup",
    "url",
)

DEFAULT_WORKFLOW_RUN_FIELDS = (
    "databaseId",
    "name",
    "status",
    "conclusion",
    "headSha",
    "url",
)

logger = logging.getLogger(__name__)
logger.addHandler(logging.NullHandler())


class RepoPathResolutionError(RuntimeError):
    """Raised when the recovery workflow cannot verify the PR worktree."""


class WorkflowReportError(RuntimeError):
    """Raised when a recovery report omits required structured output."""


class GitHubServiceError(RuntimeError):
    """Raised when read-only GitHub service evidence cannot be collected."""


CommandRunner = Callable[[Sequence[str]], subprocess.CompletedProcess[str]]


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


@dataclass(frozen=True)
class EvidenceResult:
    status: str
    blockers: list[str]


@dataclass(frozen=True)
class PrHeadEvidence(EvidenceResult):
    pr_number: int
    branch: str
    local_head_sha: str
    pr_head_oid: str


@dataclass(frozen=True)
class GitHubCheckEvidence(EvidenceResult):
    head_sha: str
    merge_state_status: str
    mergeable: str
    workflow_runs: list[Mapping[str, object]]


@dataclass(frozen=True)
class DiffScopeEvidence(EvidenceResult):
    changed_files: list[str]


@dataclass(frozen=True)
class ValidationCommandEvidence(EvidenceResult):
    commands: list[str]


@dataclass(frozen=True)
class ReadinessDecision:
    decision: str
    blockers: list[str]


def _git(repo_or_input_path: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo_or_input_path), *args],
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.rstrip("\n")


def _default_command_runner(command: Sequence[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        list(command),
        check=False,
        capture_output=True,
        text=True,
    )


def _transient_external_failure(message: str) -> bool:
    lowered = message.lower()
    transient_markers = (
        "temporar",
        "timed out",
        "timeout",
        "connection reset",
        "connection refused",
        "connection aborted",
        "network",
        "tls handshake",
        "stream error",
        "rate limit",
        "secondary rate",
        "502",
        "503",
        "504",
    )
    return any(marker in lowered for marker in transient_markers)


def _evidence_mapping(record: object) -> dict[str, object]:
    if isinstance(record, Mapping):
        return dict(record)
    if is_dataclass(record):
        return asdict(record)
    raise TypeError(f"Unsupported evidence record type: {type(record).__name__}")


class GhCliClient:
    """Read-only GitHub adapter backed by the authenticated gh CLI."""

    def __init__(
        self,
        *,
        repo: str,
        command_runner: Optional[CommandRunner] = None,
        max_attempts: int = 3,
        retry_delay_seconds: float = 1.0,
    ) -> None:
        if max_attempts < 1:
            raise ValueError("max_attempts must be at least 1")
        self.repo = repo
        self.command_runner = command_runner or _default_command_runner
        self.max_attempts = max_attempts
        self.retry_delay_seconds = retry_delay_seconds

    def pr_view(self, pr_number: int, *, fields: Sequence[str]) -> Mapping[str, object]:
        payload = self._run_json(
            [
                "gh",
                "pr",
                "view",
                str(pr_number),
                "--repo",
                self.repo,
                "--json",
                ",".join(fields),
            ],
            operation=f"gh pr view #{pr_number}",
        )
        if not isinstance(payload, Mapping):
            raise GitHubServiceError(f"gh pr view #{pr_number} returned non-object JSON.")
        return payload

    def run_list(
        self,
        *,
        branch: str,
        commit: str,
        fields: Sequence[str],
    ) -> list[Mapping[str, object]]:
        payload = self._run_json(
            [
                "gh",
                "run",
                "list",
                "--repo",
                self.repo,
                "--branch",
                branch,
                "--commit",
                commit,
                "--json",
                ",".join(fields),
            ],
            operation=f"gh run list for {branch}@{commit}",
        )
        if not isinstance(payload, list):
            raise GitHubServiceError(f"gh run list for {branch}@{commit} returned non-list JSON.")
        runs: list[Mapping[str, object]] = []
        for index, item in enumerate(payload):
            if not isinstance(item, Mapping):
                raise GitHubServiceError(
                    f"gh run list for {branch}@{commit} returned non-object item {index}."
                )
            runs.append(item)
        return runs

    def _run_json(self, command: Sequence[str], *, operation: str) -> object:
        last_error = "no command attempts were made"
        for attempt in range(1, self.max_attempts + 1):
            try:
                result = self.command_runner(command)
            except OSError as exc:
                last_error = f"{operation} failed to start: {exc}"
                if attempt < self.max_attempts:
                    self._sleep_before_retry()
                    continue
                break

            stderr = (result.stderr or "").strip()
            stdout = (result.stdout or "").strip()
            if result.returncode == 0:
                try:
                    return json.loads(stdout or "null")
                except json.JSONDecodeError as exc:
                    raise GitHubServiceError(
                        f"{operation} returned invalid JSON on attempt {attempt}: {exc}"
                    ) from exc

            detail = stderr or stdout or "no output"
            last_error = f"{operation} exited {result.returncode}: {detail}"
            if attempt < self.max_attempts and _transient_external_failure(detail):
                self._sleep_before_retry()
                continue
            break

        raise GitHubServiceError(
            f"{operation} failed after {attempt} attempt(s): {last_error}"
        )

    def _sleep_before_retry(self) -> None:
        if self.retry_delay_seconds > 0:
            time.sleep(self.retry_delay_seconds)


def _as_list(value: object) -> list[object]:
    if value is None:
        return []
    if isinstance(value, (str, bytes)):
        return [value]
    if isinstance(value, IterableABC):
        return list(value)
    return [value]


def _mapping_get(mapping: Mapping[str, object], key: str, default: object = None) -> object:
    return mapping[key] if key in mapping else default


def _evidence_status(evidence: Mapping[str, object]) -> str:
    return str(_mapping_get(evidence, "status", "")).strip().lower()


def _evidence_blockers(evidence: Mapping[str, object]) -> list[str]:
    return [str(blocker) for blocker in _as_list(_mapping_get(evidence, "blockers", []))]


def _format_blocker(message: str) -> str:
    message = message.strip()
    if message.startswith("NOT_MERGE_READY"):
        return message
    return f"NOT_MERGE_READY: {message}"


def _collect_blockers_from_evidence(name: str, evidence: object) -> list[str]:
    if not isinstance(evidence, Mapping):
        return [_format_blocker(f"{name} evidence is missing or malformed.")]

    blockers = _evidence_blockers(evidence)
    if blockers:
        return [_format_blocker(blocker) for blocker in blockers]

    status = _evidence_status(evidence)
    if status in {"passed", "matched", "focused", "reviewed", "clean"}:
        return []
    if not status:
        return [_format_blocker(f"{name} evidence does not include a status.")]
    return [_format_blocker(f"{name} evidence status is {status}.")]


def _has_outer_timeout_wrapper(command: str) -> bool:
    try:
        tokens = shlex.split(command)
    except ValueError as exc:
        raise WorkflowReportError(f"Invalid validation command syntax: {command}") from exc
    if not tokens:
        return False

    first = tokens[0]
    return first in {"timeout", "gtimeout"} or first.endswith("/timeout") or first.endswith("/gtimeout")


def _requires_node_options(command: str) -> bool:
    return (
        "qa/outside-in/alice-desktop/" in command
        or MAVEN_COMMAND_PATTERN.search(command) is not None
    )


def _line_items(values: Iterable[object]) -> list[str]:
    items: list[str] = []
    for value in values:
        item = str(value).strip()
        if item:
            items.append(item)
    return [f"- {item}" for item in items] or ["None"]


def _render_evidence_mapping(evidence: Mapping[str, object]) -> list[str]:
    lines: list[str] = []
    for key, value in evidence.items():
        if key == "blockers":
            continue
        if isinstance(value, list):
            if not value:
                lines.append(f"- {key}: None")
            else:
                lines.append(f"- {key}:")
                lines.extend(f"  - {item}" for item in value)
        else:
            lines.append(f"- {key}: {value}")

    blockers = _evidence_blockers(evidence)
    if blockers:
        lines.append("- blockers:")
        lines.extend(f"  - {_format_blocker(blocker)}" for blocker in blockers)
    return lines or ["None"]


def _metadata_text(metadata: Mapping[str, object], key: str, default: str = "") -> str:
    return str(metadata.get(key) or default).strip()


def _workflow_run_state(run: Mapping[str, object], head_sha: str) -> str:
    run_head = str(run.get("headSha", run.get("head_sha", ""))).strip()
    status = str(run.get("status", "")).strip().lower()
    conclusion_value = run.get("conclusion")
    conclusion = "" if conclusion_value is None else str(conclusion_value).strip().lower()
    if run_head != head_sha:
        return "stale"
    if status != "completed":
        return "in progress"
    if conclusion != "success":
        return "did not succeed"
    return "passed"


def _workflow_readiness_blockers(
    workflow: str,
    runs: Sequence[Mapping[str, object]],
    head_sha: str,
) -> list[str]:
    if not runs:
        return [_format_blocker(f"required workflow {workflow} is missing.")]

    states: set[str] = set()
    for run in runs:
        state = _workflow_run_state(run, head_sha)
        if state == "passed":
            return []
        states.add(state)

    blockers: list[str] = []
    state_messages = (
        ("stale", f"required workflow {workflow} is stale for current head."),
        ("in progress", f"required workflow {workflow} is in progress."),
        ("did not succeed", f"required workflow {workflow} did not succeed."),
    )
    for state, message in state_messages:
        if state in states:
            blockers.append(_format_blocker(message))
    return blockers or [
        _format_blocker(f"required workflow {workflow} has no successful completed run.")
    ]


def _blocked_github_service_evidence(
    *,
    pr_number: int,
    branch: str,
    local_head_sha: str,
    blocker: str,
) -> dict[str, object]:
    return {
        "head": _evidence_mapping(
            PrHeadEvidence(
                status="blocked",
                blockers=[blocker],
                pr_number=pr_number,
                branch=branch,
                local_head_sha=local_head_sha,
                pr_head_oid="",
            )
        ),
        "github": _evidence_mapping(
            GitHubCheckEvidence(
                status="blocked",
                blockers=[blocker],
                head_sha=local_head_sha,
                merge_state_status="UNKNOWN",
                mergeable="UNKNOWN",
                workflow_runs=[],
            )
        ),
        "pr_metadata": {},
    }


def _collect_current_head_workflow_runs(
    *,
    client: object,
    branch: str,
    head_sha: str,
) -> tuple[list[Mapping[str, object]], list[str]]:
    if not branch or not head_sha:
        return [], [
            _format_blocker(
                "GitHub PR metadata is missing headRefName or headRefOid; "
                "workflow evidence cannot be tied to the current head."
            )
        ]

    try:
        return list(
            client.run_list(
                branch=branch,
                commit=head_sha,
                fields=DEFAULT_WORKFLOW_RUN_FIELDS,
            )
        ), []
    except GitHubServiceError as exc:
        return [], [_format_blocker(f"GitHub workflow fetch failed: {exc}")]


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
    diff_stat = _git(repo_path, "diff", "--stat", "HEAD")
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


def verify_current_pr_head(
    *,
    pr_number: int,
    head_branch: str,
    local_head_sha: str,
    pr_head_oid: str,
) -> PrHeadEvidence:
    """Verify local evidence is tied to the exact current PR head."""

    blockers: list[str] = []
    if not local_head_sha or not pr_head_oid:
        blockers.append(
            _format_blocker(
                f"PR #{pr_number} head verification is incomplete for branch {head_branch}."
            )
        )
    elif local_head_sha != pr_head_oid:
        blockers.append(
            _format_blocker(
                "local HEAD does not match current PR headRefOid "
                f"for PR #{pr_number} on {head_branch}: local={local_head_sha} "
                f"headRefOid={pr_head_oid}."
            )
        )

    status = "blocked" if blockers else "matched"
    if blockers:
        logger.warning("PR head verification blocked readiness: %s", "; ".join(blockers))
    return PrHeadEvidence(
        status=status,
        blockers=blockers,
        pr_number=pr_number,
        branch=head_branch,
        local_head_sha=local_head_sha,
        pr_head_oid=pr_head_oid,
    )


def collect_github_check_evidence(
    *,
    head_sha: str,
    merge_state_status: str,
    mergeable: str,
    required_workflows: Sequence[str],
    workflow_runs: Sequence[Mapping[str, object]],
) -> GitHubCheckEvidence:
    """Validate GitHub mergeability and workflow evidence for the same head SHA."""

    blockers: list[str] = []
    normalized_merge_state = merge_state_status.strip().upper()
    normalized_mergeable = mergeable.strip().upper()
    if normalized_merge_state not in {"CLEAN", "HAS_HOOKS"}:
        blockers.append(_format_blocker(f"mergeStateStatus is {merge_state_status}."))
    if normalized_mergeable != "MERGEABLE":
        blockers.append(_format_blocker(f"mergeable state is {mergeable}."))

    workflow_run_list = list(workflow_runs)
    required_workflow_names = set(required_workflows)
    runs_by_name: dict[str, list[Mapping[str, object]]] = {}
    for run in workflow_run_list:
        name = str(run.get("name", "")).strip()
        if name in required_workflow_names:
            runs_by_name.setdefault(name, []).append(run)

    for workflow in required_workflows:
        blockers.extend(
            _workflow_readiness_blockers(workflow, runs_by_name.get(workflow, []), head_sha)
        )

    status = "blocked" if blockers else "passed"
    if blockers:
        logger.warning("GitHub check evidence blocked readiness: %s", "; ".join(blockers))
    return GitHubCheckEvidence(
        status=status,
        blockers=blockers,
        head_sha=head_sha,
        merge_state_status=merge_state_status,
        mergeable=mergeable,
        workflow_runs=workflow_run_list,
    )


def collect_github_service_evidence(
    *,
    pr_number: int,
    repo: str,
    local_head_sha: str,
    required_workflows: Sequence[str],
    head_branch: Optional[str] = None,
    client: Optional[object] = None,
) -> dict[str, object]:
    """Collect read-only GitHub evidence through a service adapter and fail closed."""

    github_client = client or GhCliClient(repo=repo)
    try:
        pr_metadata = github_client.pr_view(
            pr_number,
            fields=DEFAULT_PR_METADATA_FIELDS,
        )
    except GitHubServiceError as exc:
        blocker = _format_blocker(f"GitHub PR metadata fetch failed: {exc}")
        return _blocked_github_service_evidence(
            pr_number=pr_number,
            branch=head_branch or "",
            local_head_sha=local_head_sha,
            blocker=blocker,
        )

    if not isinstance(pr_metadata, Mapping):
        raise GitHubServiceError("GitHub PR metadata adapter returned non-object evidence.")

    pr_head_oid = _metadata_text(pr_metadata, "headRefOid")
    resolved_head_branch = _metadata_text(pr_metadata, "headRefName", head_branch or "")
    merge_state_status = _metadata_text(pr_metadata, "mergeStateStatus", "UNKNOWN")
    mergeable = _metadata_text(pr_metadata, "mergeable", "UNKNOWN")
    head_evidence = verify_current_pr_head(
        pr_number=pr_number,
        head_branch=resolved_head_branch,
        local_head_sha=local_head_sha,
        pr_head_oid=pr_head_oid,
    )

    workflow_runs, service_blockers = _collect_current_head_workflow_runs(
        client=github_client,
        branch=resolved_head_branch,
        head_sha=pr_head_oid,
    )

    github_evidence = collect_github_check_evidence(
        head_sha=pr_head_oid or local_head_sha,
        merge_state_status=merge_state_status,
        mergeable=mergeable,
        required_workflows=required_workflows,
        workflow_runs=workflow_runs,
    )
    github_mapping = _evidence_mapping(github_evidence)
    if service_blockers:
        github_mapping["status"] = "blocked"
        github_mapping["blockers"] = [
            *[str(blocker) for blocker in github_mapping.get("blockers", [])],
            *service_blockers,
        ]

    return {
        "head": _evidence_mapping(head_evidence),
        "github": github_mapping,
        "pr_metadata": dict(pr_metadata),
    }


def inspect_diff_scope(*, changed_files: Sequence[str]) -> DiffScopeEvidence:
    """Confirm changed files stay inside the focused recovery, QA, test, and docs scope."""

    files = [path for path in changed_files if path]
    unrelated = [
        path
        for path in files
        if not any(path == prefix.rstrip("/") or path.startswith(prefix) for prefix in FOCUSED_DIFF_PREFIXES)
    ]
    blockers = [
        _format_blocker(f"unrelated diff scope includes {path}.")
        for path in unrelated
    ]
    status = "blocked" if blockers else "focused"
    if blockers:
        logger.warning("Diff scope blocked readiness: %s", "; ".join(blockers))
    return DiffScopeEvidence(status=status, blockers=blockers, changed_files=files)


def validate_recovery_commands(commands: Sequence[str]) -> ValidationCommandEvidence:
    """Validate focused recovery commands use no outer timeout wrappers and required Node heap."""

    blockers: list[str] = []
    command_list = [command.strip() for command in commands if command.strip()]
    if not command_list:
        blockers.append(_format_blocker("focused validation commands are missing."))

    for command in command_list:
        if _has_outer_timeout_wrapper(command):
            blockers.append(_format_blocker(f"validation command uses timeout wrappers: {command}"))
        if _requires_node_options(command) and NODE_OPTIONS_REQUIREMENT not in command:
            blockers.append(
                _format_blocker(
                    f"validation command must include {NODE_OPTIONS_REQUIREMENT}: {command}"
                )
            )

    status = "blocked" if blockers else "passed"
    if blockers:
        logger.warning("Recovery command validation blocked readiness: %s", "; ".join(blockers))
    return ValidationCommandEvidence(status=status, blockers=blockers, commands=command_list)


def evaluate_readiness(evidence: Mapping[str, object]) -> ReadinessDecision:
    """Evaluate every merge-ready gate and fail closed with explicit blockers."""

    blockers: list[str] = []
    required_evidence = (
        ("current PR head", evidence.get("head")),
        ("GitHub Actions and PR", evidence.get("github")),
        ("focused diff scope", evidence.get("diff_scope")),
        ("validation", evidence.get("validation")),
        ("runnable QA", evidence.get("qa")),
        ("docs impact", evidence.get("docs")),
    )
    for name, item in required_evidence:
        blockers.extend(_collect_blockers_from_evidence(name, item))

    quality_cycles = evidence.get("quality_audit_cycles")
    cycles = _as_list(quality_cycles)
    if len(cycles) < 3:
        blockers.append(_format_blocker("fewer than three quality-audit cycles are documented."))
    elif not isinstance(cycles[-1], Mapping) or not bool(cycles[-1].get("clean")):
        blockers.append(_format_blocker("final quality-audit cycle is not clean."))

    decision = "NOT_MERGE_READY" if blockers else "MERGE_READY"
    if blockers:
        logger.warning("Readiness evaluation blocked merge-ready status: %s", "; ".join(blockers))
    return ReadinessDecision(decision=decision, blockers=blockers)


def _validate_no_overclaims(text: str) -> None:
    lowered = text.lower()
    found: list[str] = []
    for claim in UNSUPPORTED_MERGE_READY_CLAIMS:
        claim_text = claim.lower()
        search_start = 0
        claim_is_positive = False
        while True:
            index = lowered.find(claim_text, search_start)
            if index == -1:
                break
            sentence_start = max(lowered.rfind(".", 0, index), lowered.rfind("\n", 0, index)) + 1
            before_claim = lowered[sentence_start:index]
            if not CLAIM_NEGATION_PATTERN.search(before_claim):
                claim_is_positive = True
                break
            search_start = index + len(claim_text)
        if claim_is_positive:
            found.append(claim)
    if found:
        raise WorkflowReportError(
            "Recovery report overclaims unproven behavior: " + ", ".join(found)
        )


def validate_merge_ready_recovery_report(report: str) -> None:
    """Validate expanded merge-ready recovery report section ordering."""

    missing = [section for section in MERGE_READY_REPORT_SECTIONS if section not in report]
    if missing:
        raise WorkflowReportError(
            f"Missing required merge-ready recovery report sections: {', '.join(missing)}"
        )

    section_positions = [report.index(section) for section in MERGE_READY_REPORT_SECTIONS]
    if section_positions != sorted(section_positions):
        raise WorkflowReportError("Merge-ready recovery report sections are out of order")


def _mapping_lines_or_none(value: object) -> list[str]:
    return _render_evidence_mapping(value) if isinstance(value, Mapping) else ["None"]


def _merge_ready_list_lines(
    evidence: Mapping[str, object],
    evidence_key: str,
    list_key: str,
) -> list[str]:
    value = evidence.get(evidence_key, {})
    if not isinstance(value, Mapping):
        return ["None"]
    return _line_items(value.get(list_key, []))


def _merge_ready_github_lines(evidence: Mapping[str, object]) -> list[str]:
    lines: list[str] = []
    for key in ("head", "github"):
        value = evidence.get(key, {})
        if isinstance(value, Mapping):
            lines.extend(_render_evidence_mapping(value))

    diff_scope = evidence.get("diff_scope", {})
    if isinstance(diff_scope, Mapping):
        lines.append("- diff_scope:")
        lines.extend(f"  {line}" for line in _render_evidence_mapping(diff_scope))
    return lines or ["None"]


def _quality_audit_cycle_lines(evidence: Mapping[str, object]) -> list[str]:
    lines: list[str] = []
    for index, cycle in enumerate(_as_list(evidence.get("quality_audit_cycles")), start=1):
        if isinstance(cycle, Mapping):
            seek = cycle.get("seek", "None")
            validate = cycle.get("validate", "None")
            fix = cycle.get("fix", "None")
            clean = bool(cycle.get("clean"))
            lines.append(f"- Cycle {index}:")
            lines.append(f"  - SEEK: {seek}")
            lines.append(f"  - VALIDATE: {validate}")
            lines.append(f"  - FIX: {fix}")
            lines.append(f"  - clean: {clean}")
        else:
            lines.append(f"- Cycle {index}: {cycle}")
    return lines or ["None"]


def _readiness_decision_lines(
    decision: ReadinessDecision,
    files_modified: Sequence[str],
) -> list[str]:
    lines = [decision.decision, *[f"- {blocker}" for blocker in decision.blockers]]
    if not files_modified:
        lines.append(
            "workflow-accepted No-op justification: no repository implementation "
            "changes are listed; readiness is tied to the supplied current-head evidence "
            "and any explicit merge-ready blockers above."
        )
    return lines


def render_merge_ready_recovery_report(
    *,
    summary: str,
    files_modified: Sequence[str],
    evidence: Mapping[str, object],
) -> str:
    """Render current-head recovery evidence and a fail-closed readiness decision."""

    scope_bounded_claims = str(
        evidence.get(
            "scope_bounded_claims",
            (
                "Evidence is bounded to current-head checks, focused QA contracts, "
                "docs impact, diff scope, and PR description review."
            ),
        )
    ).strip()
    _validate_no_overclaims(scope_bounded_claims)

    decision = evaluate_readiness(evidence)
    files_lines = [f"- {path}" for path in files_modified] or ["None"]

    sections = [
        ("Summary", [summary.strip() or "None"]),
        ("Files modified", files_lines),
        ("Validation", _merge_ready_list_lines(evidence, "validation", "commands")),
        ("QA / scenario evidence", _merge_ready_list_lines(evidence, "qa", "evidence")),
        ("Docs impact", _mapping_lines_or_none(evidence.get("docs", {}))),
        ("Scope / bounded claims", [scope_bounded_claims]),
        ("GitHub and PR evidence", _merge_ready_github_lines(evidence)),
        ("Quality-audit cycles", _quality_audit_cycle_lines(evidence)),
        ("Readiness decision", _readiness_decision_lines(decision, files_modified)),
    ]
    report = "\n\n".join(f"{heading}\n" + "\n".join(lines) for heading, lines in sections)
    validate_merge_ready_recovery_report(report)
    return report + "\n"


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
