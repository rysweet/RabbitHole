#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import logging
import os
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Iterable, Sequence


def _current_repository_head() -> str:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=Path(__file__).resolve().parents[1],
            check=True,
            capture_output=True,
            text=True,
        )
    except subprocess.CalledProcessError as exc:
        details = exc.stderr.strip() or exc.stdout.strip() or f"exit code {exc.returncode}"
        raise RuntimeError(f"unable to determine current repository HEAD: {details}") from exc
    return result.stdout.strip()


EXPECTED_PR_HEAD = os.environ.get("PR437_EXPECTED_HEAD") or _current_repository_head()
EXPECTED_REPO = "rysweet/RabbitHole"
EXPECTED_PR_NUMBER = 437
EXPECTED_BRANCH = "feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo"
PR_JSON_FIELDS = (
    "number,title,state,headRefName,headRefOid,baseRefName,isDraft,"
    "mergeStateStatus,reviewDecision,statusCheckRollup,url"
)

LOGGER = logging.getLogger("pr437-finalization")


class FinalizationError(Exception):
    """Base error for PR #437 finalization failures."""


class HeadMismatchError(FinalizationError):
    """Raised when local or remote evidence is not tied to the required PR head."""


class ExternalMetadataError(FinalizationError):
    """Raised when current GitHub metadata cannot be collected or parsed."""


class ScopeViolationError(FinalizationError):
    """Raised when Select Project evidence expands beyond the starter lane."""


class NoOpDirtyRecoveryError(FinalizationError):
    """Raised when dirty PR repair is incorrectly represented as no-op work."""


@dataclass(frozen=True)
class HeadVerification:
    local_head: str
    evidence_basis_sha: str


@dataclass(frozen=True)
class CheckEvidence:
    name: str
    status: str
    conclusion: str
    required: bool


@dataclass(frozen=True)
class GitHubEvidence:
    number: int
    title: str
    state: str
    head_ref_name: str
    head_ref_oid: str
    base_ref_name: str
    is_draft: bool
    merge_state_status: str
    review_decision: str
    checks: tuple[CheckEvidence, ...]
    url: str


@dataclass(frozen=True)
class MergeReadiness:
    merge_ready: bool
    reason: str
    blocker: str
    review_state: str
    approved: bool
    requires_disposable_merge_check: bool
    required_check_conclusion: str


@dataclass(frozen=True)
class FocusedValidation:
    accepted_claims: list[str]
    blocker: str


@dataclass(frozen=True)
class GateDecision:
    action: str
    reason: str


@dataclass(frozen=True)
class DirtyRecoveryPlan:
    action: str
    repo: str
    pr_number: int
    branch: str
    base_ref: str
    reconcile_ref: str
    noop_allowed: bool
    reconcile_commands: tuple[str, ...]
    validation_commands: tuple[str, ...]
    focused_evidence_claims: list[str]
    changed_files: list[str]
    blocker: str


FOCUSED_SELECT_PROJECT_VALIDATION_COMMANDS = (
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-select-project-proof.sh",
    "qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh",
    "qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh",
    "python3 -m unittest "
    "tests/test_pr437_select_project_recovery_contract.py "
    "tests/test_pr437_finalization_workflow.py "
    "tests/test_pr437_noop_recovery_report_contract.py "
    "tests/test_pr437_dirty_recovery_workflow.py",
)

FOCUSED_SELECT_PROJECT_EVIDENCE_CLAIMS = (
    "Select Project visibility",
    "Starters tab activation",
    "Africa Full target selection evidence",
    "Africa Full open attempt evidence",
)


@dataclass(frozen=True)
class ExternalRetryPolicy:
    attempts: int = 3
    delay_seconds: float = 0.5

    @classmethod
    def from_environment(cls) -> "ExternalRetryPolicy":
        attempts = _positive_int_from_env("GH_EXTERNAL_ATTEMPTS", cls.attempts)
        delay_seconds = _non_negative_float_from_env(
            "GH_EXTERNAL_RETRY_SECONDS",
            cls.delay_seconds,
        )
        return cls(attempts=attempts, delay_seconds=delay_seconds)


class CommandRunner:
    def __init__(self, cwd: Path | None = None) -> None:
        self.cwd = cwd

    def run(self, command: Sequence[str], **_: Any) -> str:
        LOGGER.info("running command: %s", " ".join(command))
        try:
            result = subprocess.run(
                list(command),
                cwd=self.cwd,
                check=True,
                capture_output=True,
                text=True,
            )
        except subprocess.CalledProcessError as exc:
            stderr = exc.stderr.strip()
            stdout = exc.stdout.strip()
            details = stderr or stdout or f"exit code {exc.returncode}"
            raise RuntimeError(f"command failed: {' '.join(command)}: {details}") from exc
        return result.stdout


class ExternalCommandServiceAdapter:
    def __init__(
        self,
        runner: Any | None = None,
        retry_policy: ExternalRetryPolicy | None = None,
        sleeper: Callable[[float], None] | None = None,
    ) -> None:
        self.runner = runner or CommandRunner()
        self.retry_policy = retry_policy or ExternalRetryPolicy.from_environment()
        self.sleeper = sleeper or time.sleep

    def run(self, command: Sequence[str], *, dependency_name: str) -> str:
        attempts = max(1, self.retry_policy.attempts)
        last_error: Exception | None = None
        for attempt in range(1, attempts + 1):
            try:
                return self.runner.run(command)
            except Exception as exc:
                last_error = exc
                if attempt >= attempts:
                    break
                LOGGER.info(
                    "external service call failed on attempt %s/%s; retrying in %ss: %s",
                    attempt,
                    attempts,
                    self.retry_policy.delay_seconds,
                    _safe_command(command),
                )
                self.sleeper(self.retry_policy.delay_seconds)
        category = _classify_external_failure(last_error)
        raise ExternalMetadataError(
            f"environment dependency: {dependency_name} failed after {attempts} attempts "
            f"({category})"
        ) from last_error


class CurrentHeadVerifier:
    def __init__(self, expected_head: str = EXPECTED_PR_HEAD, runner: Any | None = None) -> None:
        self.expected_head = expected_head
        self.runner = runner or CommandRunner()

    def verify(self) -> HeadVerification:
        local_head = self.runner.run(["git", "rev-parse", "HEAD"]).strip()
        if local_head != self.expected_head:
            raise HeadMismatchError(
                f"local HEAD {local_head} does not match required PR #437 head {self.expected_head}"
            )
        return HeadVerification(
            local_head=local_head,
            evidence_basis_sha=self.expected_head,
        )


class GitHubEvidenceCollector:
    def __init__(
        self,
        pr_number: int = EXPECTED_PR_NUMBER,
        repo: str = EXPECTED_REPO,
        expected_head: str = EXPECTED_PR_HEAD,
        runner: Any | None = None,
        service_adapter: ExternalCommandServiceAdapter | None = None,
    ) -> None:
        self.pr_number = pr_number
        self.repo = repo
        self.expected_head = expected_head
        self.runner = runner or CommandRunner()
        self.service_adapter = service_adapter or ExternalCommandServiceAdapter(self.runner)

    def collect(self) -> GitHubEvidence:
        command = [
            "gh",
            "pr",
            "view",
            str(self.pr_number),
            "--repo",
            self.repo,
            "--json",
            PR_JSON_FIELDS,
        ]
        try:
            output = self.service_adapter.run(
                command,
                dependency_name="GitHub PR metadata",
            )
            payload = json.loads(output)
        except HeadMismatchError:
            raise
        except ExternalMetadataError:
            raise
        except Exception as exc:
            raise ExternalMetadataError(
                f"unable to parse current GitHub metadata for PR #{self.pr_number}: {exc}"
            ) from exc

        head_ref_oid = str(payload.get("headRefOid") or "")
        if head_ref_oid != self.expected_head:
            raise HeadMismatchError(
                f"PR #{self.pr_number} headRefOid {head_ref_oid} does not match required head {self.expected_head}"
            )

        checks = tuple(_parse_check(item) for item in payload.get("statusCheckRollup") or [])
        return GitHubEvidence(
            number=int(payload.get("number") or self.pr_number),
            title=str(payload.get("title") or ""),
            state=str(payload.get("state") or ""),
            head_ref_name=str(payload.get("headRefName") or ""),
            head_ref_oid=head_ref_oid,
            base_ref_name=str(payload.get("baseRefName") or ""),
            is_draft=bool(payload.get("isDraft")),
            merge_state_status=str(payload.get("mergeStateStatus") or ""),
            review_decision=str(payload.get("reviewDecision") or ""),
            checks=checks,
            url=str(payload.get("url") or ""),
        )


class MergeReadinessEvaluator:
    def evaluate(self, evidence: GitHubEvidence | dict[str, Any]) -> MergeReadiness:
        if isinstance(evidence, GitHubEvidence):
            merge_state = evidence.merge_state_status
            review_decision = evidence.review_decision
            checks = evidence.checks
        else:
            merge_state = str(evidence.get("mergeStateStatus") or "")
            review_decision = str(evidence.get("reviewDecision") or "")
            checks = tuple(_parse_check(item) for item in evidence.get("statusCheckRollup") or [])
        required_checks = tuple(check for check in checks if check.required)
        review_state = review_decision or "owner-free/unset"
        approved = review_decision.upper() == "APPROVED"

        if not required_checks:
            return MergeReadiness(
                merge_ready=False,
                reason="required-checks-unavailable",
                blocker="required checks metadata is unavailable",
                review_state=review_state,
                approved=approved,
                requires_disposable_merge_check=False,
                required_check_conclusion="NO_REQUIRED_CHECKS",
            )

        if merge_state != "CLEAN":
            requires_disposable = merge_state in {"DIRTY", "UNKNOWN", "UNSTABLE", ""}
            return MergeReadiness(
                merge_ready=False,
                reason="merge-state-blocked",
                blocker=(
                    "merge dirtiness: "
                    f"mergeStateStatus is {merge_state or 'unavailable'}, not CLEAN"
                ),
                review_state=review_state,
                approved=approved,
                requires_disposable_merge_check=requires_disposable,
                required_check_conclusion=_summarize_required_checks(required_checks),
            )

        failed_check_names = ", ".join(
            check.name
            for check in required_checks
            if check.status != "COMPLETED" or check.conclusion != "SUCCESS"
        )
        if failed_check_names:
            return MergeReadiness(
                merge_ready=False,
                reason="required-checks-blocked",
                blocker=f"required checks are not all SUCCESS: {failed_check_names}",
                review_state=review_state,
                approved=approved,
                requires_disposable_merge_check=False,
                required_check_conclusion=_summarize_required_checks(required_checks),
            )

        return MergeReadiness(
            merge_ready=True,
            reason="clean-green",
            blocker="",
            review_state=review_state,
            approved=approved,
            requires_disposable_merge_check=False,
            required_check_conclusion="SUCCESS",
        )


class FocusedSelectProjectValidator:
    ALLOWED_CLAIMS = {
        "Select Project visibility",
        "Starters tab activation",
        "Africa Full target selection/open attempt",
        "Africa Full target selection evidence",
        "Africa Full open attempt evidence",
    }
    ALLOWED_BLOCKER_PREFIX = "exact blocker:"
    FORBIDDEN_TERMS = {
        "rendering",
        "save",
        "grading",
        "lesson",
        "world interaction",
        "world",
    }

    def validate(self, claims: Iterable[str]) -> FocusedValidation:
        accepted: list[str] = []
        blockers: list[str] = []
        for claim in claims:
            normalized = claim.strip()
            if not normalized:
                continue
            lowered = normalized.lower()
            if any(term in lowered for term in self.FORBIDDEN_TERMS):
                raise ScopeViolationError(
                    f"Select Project evidence is over-scoped: {normalized}"
                )
            if normalized in self.ALLOWED_CLAIMS:
                accepted.append(normalized)
                continue
            if lowered.startswith(self.ALLOWED_BLOCKER_PREFIX):
                blockers.append(normalized)
                continue
            raise ScopeViolationError(
                "Select Project evidence must be limited to visibility, Starters tab activation, "
                f"Africa Full target selection/open attempt, or an exact blocker: {normalized}"
            )

        return FocusedValidation(
            accepted_claims=accepted,
            blocker="; ".join(blockers),
        )


class DirtyRecoveryPlanner:
    def __init__(
        self,
        *,
        repo: str,
        pr_number: int,
        branch: str,
        base_ref: str,
        node_options: str,
    ) -> None:
        self.repo = repo
        self.pr_number = pr_number
        self.branch = branch
        self.base_ref = base_ref
        self.reconcile_ref = f"origin/{base_ref}"
        self.validation_commands = tuple(
            f"NODE_OPTIONS={node_options} {command}"
            for command in FOCUSED_SELECT_PROJECT_VALIDATION_COMMANDS
        )

    def plan(
        self,
        *,
        local_head: str,
        pr_head: str,
        readiness: MergeReadiness,
        changed_files: Sequence[str],
        requested_action: str = "EDIT_AND_PUSH",
    ) -> DirtyRecoveryPlan:
        if requested_action == "NO_OP":
            raise NoOpDirtyRecoveryError(
                "PR #437 dirty recovery must use edit-and-push repair evidence; no-op mode is prohibited"
            )

        blocker = ""
        if local_head != pr_head:
            blocker = f"head mismatch: local HEAD {local_head} does not match PR head {pr_head}"
        elif readiness.merge_ready:
            blocker = "PR metadata is already clean; dirty recovery planning is not required"
        elif not readiness.requires_disposable_merge_check:
            blocker = readiness.blocker

        action = "NOT_MERGE_READY" if blocker else "EDIT_AND_PUSH"
        return DirtyRecoveryPlan(
            action=action,
            repo=self.repo,
            pr_number=self.pr_number,
            branch=self.branch,
            base_ref=self.base_ref,
            reconcile_ref=self.reconcile_ref,
            noop_allowed=False,
            reconcile_commands=(
                f"git fetch origin {self.base_ref}",
                f"git merge --no-edit {self.reconcile_ref}",
            ),
            validation_commands=self.validation_commands,
            focused_evidence_claims=list(FOCUSED_SELECT_PROJECT_EVIDENCE_CLAIMS),
            changed_files=list(changed_files),
            blocker=blocker,
        )


class ChangeGate:
    def decide(
        self,
        *,
        local_head: str,
        pr_head: str,
        merge_ready: bool,
        review_state: str,
        focused_scope_valid: bool,
        worktree_changes: Sequence[str],
        stale_evidence: bool,
    ) -> GateDecision:
        if local_head != pr_head:
            return GateDecision(
                action="BLOCKED_WITH_REASON",
                reason=f"head mismatch: local HEAD {local_head} does not match PR head {pr_head}",
            )
        if not merge_ready:
            return GateDecision(
                action="BLOCKED_WITH_REASON",
                reason="merge-ready evidence is missing or blocked",
            )
        if review_state != "owner-free/unset":
            return GateDecision(
                action="BLOCKED_WITH_REASON",
                reason=f"unexpected review metadata for owner-free finalization: {review_state}",
            )
        if not focused_scope_valid:
            return GateDecision(
                action="BLOCKED_WITH_REASON",
                reason="Select Project starter evidence is outside the allowed scope",
            )
        if stale_evidence:
            return GateDecision(
                action="EDIT_AND_PUSH",
                reason="current-head evidence, docs, contracts, or QA artifacts are stale",
            )
        if worktree_changes:
            return GateDecision(
                action="EDIT_AND_PUSH",
                reason="repository file changes exist and must be reported as pushed changes",
            )
        return GateDecision(
            action="NO_OP",
            reason="current head is clean/green with owner-free review metadata and no repository files are modified",
        )


class FinalReportGenerator:
    def generate(
        self,
        *,
        action: str,
        current_head: str,
        branch: str,
        merge_state_status: str,
        required_check_conclusion: str,
        review_state: str,
        modified_files: Sequence[str],
        focused_evidence: Sequence[str],
    ) -> str:
        focused_lines = "\n".join(f"- {item}" for item in focused_evidence) or "- None"
        if action == "NO_OP":
            return (
                "No-op justification:\n"
                f"Current branch: `{branch}`\n"
                f"Current head: `{current_head}`\n"
                "Worktree cleanliness: no repository files are modified\n"
                "Merge-ready evidence: "
                f"mergeStateStatus={merge_state_status}; required checks={required_check_conclusion}\n"
                f"Review metadata: reviewDecision is {review_state}, not an approval claim\n"
                "Focused validation:\n"
                f"{focused_lines}\n"
            )
        if action == "EDIT_AND_PUSH":
            files = "\n".join(f"- {path}" for path in modified_files) or "- None"
            return (
                "Report path: `EDIT_AND_PUSH`\n"
                f"Current branch: `{branch}`\n"
                f"Current head: `{current_head}`\n"
                "Merge-ready evidence: "
                f"mergeStateStatus={merge_state_status}; required checks={required_check_conclusion}\n"
                f"Review metadata: reviewDecision is {review_state}\n"
                "Files modified:\n"
                f"{files}\n"
                "Focused validation:\n"
                f"{focused_lines}\n"
                "Publish this summary after the commit/push.\n"
            )
        if action == "BLOCKED_WITH_REASON":
            return (
                "Report path: `NOT_MERGE_READY`\n"
                f"Current branch: `{branch}`\n"
                f"Current head: `{current_head}`\n"
                "Concrete blockers:\n"
                f"{focused_lines}\n"
                "Do not merge manually.\n"
                "Do not use no-op mode.\n"
                "Finalization is blocked; do not publish a no-op or merge-ready report.\n"
            )
        raise ValueError(f"unknown finalization report action: {action}")


def _parse_check(item: dict[str, Any]) -> CheckEvidence:
    name = str(item.get("name") or item.get("context") or "unnamed check")
    status = str(item.get("status") or "COMPLETED")
    conclusion = str(item.get("conclusion") or item.get("state") or "")
    required = bool(item.get("isRequired", True))
    return CheckEvidence(
        name=name,
        status=status,
        conclusion=conclusion,
        required=required,
    )


def _summarize_required_checks(checks: Sequence[CheckEvidence]) -> str:
    if not checks:
        return "NO_REQUIRED_CHECKS"
    conclusions = {check.conclusion or check.status for check in checks}
    if conclusions == {"SUCCESS"}:
        return "SUCCESS"
    return ",".join(sorted(conclusions))


def _positive_int_from_env(name: str, default: int) -> int:
    value = os.environ.get(name)
    if value is None:
        return default
    try:
        parsed = int(value)
    except ValueError as exc:
        raise ExternalMetadataError(f"{name} must be a positive integer") from exc
    if parsed < 1:
        raise ExternalMetadataError(f"{name} must be a positive integer")
    return parsed


def _non_negative_float_from_env(name: str, default: float) -> float:
    value = os.environ.get(name)
    if value is None:
        return default
    try:
        parsed = float(value)
    except ValueError as exc:
        raise ExternalMetadataError(f"{name} must be a non-negative number") from exc
    if parsed < 0:
        raise ExternalMetadataError(f"{name} must be a non-negative number")
    return parsed


def _classify_external_failure(error: Exception | None) -> str:
    if error is None:
        return "unknown external service failure"
    text = str(error).lower()
    if any(term in text for term in ("auth", "authenticate", "login", "credential")):
        return "GitHub CLI authentication"
    if any(term in text for term in ("rate limit", "secondary rate", "too many requests")):
        return "rate limiting"
    if any(
        term in text
        for term in (
            "network",
            "connection",
            "could not resolve",
            "timed out",
            "timeout",
            "tls",
            "temporary failure",
        )
    ):
        return "network connectivity"
    return "GitHub CLI command failure"


def _safe_command(command: Sequence[str]) -> str:
    return " ".join(command)


def collect_worktree_changes(runner: Any | None = None) -> list[str]:
    command_runner = runner or CommandRunner()
    output = command_runner.run(["git", "status", "--short"])
    changes: list[str] = []
    for line in output.splitlines():
        if not line.strip():
            continue
        changes.append(line[3:].strip())
    return changes


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Finalize RabbitHole PR #437 evidence.")
    parser.add_argument("--repo", default=EXPECTED_REPO)
    parser.add_argument("--pr-number", type=int, default=EXPECTED_PR_NUMBER)
    parser.add_argument("--expected-head", default=EXPECTED_PR_HEAD)
    parser.add_argument("--branch", default=EXPECTED_BRANCH)
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    args = build_arg_parser().parse_args(argv)
    runner = CommandRunner()

    try:
        head = CurrentHeadVerifier(args.expected_head, runner).verify()
        evidence = GitHubEvidenceCollector(
            pr_number=args.pr_number,
            repo=args.repo,
            expected_head=args.expected_head,
            runner=runner,
        ).collect()
        readiness = MergeReadinessEvaluator().evaluate(evidence)
        focused = FocusedSelectProjectValidator().validate(
            [
                "Select Project visibility",
                "Starters tab activation",
                "Africa Full target selection/open attempt",
            ]
        )
        changes = collect_worktree_changes(runner)
        decision = ChangeGate().decide(
            local_head=head.local_head,
            pr_head=evidence.head_ref_oid,
            merge_ready=readiness.merge_ready,
            review_state=readiness.review_state,
            focused_scope_valid=True,
            worktree_changes=changes,
            stale_evidence=False,
        )
        report = FinalReportGenerator().generate(
            action=decision.action,
            current_head=head.local_head,
            branch=evidence.head_ref_name or args.branch,
            merge_state_status=evidence.merge_state_status,
            required_check_conclusion=readiness.required_check_conclusion,
            review_state=readiness.review_state,
            modified_files=changes,
            focused_evidence=focused.accepted_claims or ([focused.blocker] if focused.blocker else []),
        )
        print(report)
        if decision.action == "BLOCKED_WITH_REASON":
            LOGGER.error(decision.reason)
            return 1
        return 0
    except FinalizationError as exc:
        LOGGER.error("%s", exc)
        return 1


if __name__ == "__main__":
    sys.exit(main())
