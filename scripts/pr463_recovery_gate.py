#!/usr/bin/env python3
"""Evaluate PR #463 focused archive/player recovery evidence before merge-ready claims."""

from __future__ import annotations

import argparse
import json
import logging
import re
import shlex
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Callable, Iterable


EXPECTED_REPOSITORY = "rysweet/RabbitHole"
EXPECTED_PR_NUMBER = 463
EXPECTED_BRANCH = "feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr"
EXPECTED_BASE_REF = "develop"
EXPECTED_NODE_OPTIONS = "--max-old-space-size=32768"
EXPECTED_AUTOMATION_MODE = "gated-command-smoke"
EXPECTED_RECOVERY_MODE = "focused-archive-player-repair"
EXPECTED_SCOPE = "archive/player-boundary"
DEFAULT_GITHUB_TIMEOUT_SECONDS = 20.0
DEFAULT_GITHUB_RETRY_ATTEMPTS = 3
DEFAULT_GITHUB_RETRY_DELAY_SECONDS = 2.0
GITHUB_PR_VIEW_FIELDS = (
    "baseRefName",
    "headRefName",
    "headRefOid",
    "mergeable",
    "mergeStateStatus",
    "statusCheckRollup",
    "url",
)

ARCHIVE_SMOKE_ARGV = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/story-api-migration",
    "-am",
    "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest",
    "test",
)
TWEEDLE_SMOKE_ARGV = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ast",
    "-am",
    (
        "-Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#"
        "zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+"
        "zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+"
        "zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+"
        "zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+"
        "zeroArgumentThisMethodCallDecodeRejectsChainedCall"
    ),
    "test",
)
EXPECTED_WORKFLOW_ARGV = {
    "archive-fixture-smoke": ARCHIVE_SMOKE_ARGV,
    "tweedle-decoder-boundary-smoke": TWEEDLE_SMOKE_ARGV,
}
FOCUSED_REPAIR_PATHS = (
    "scripts/pr463_recovery_gate.py",
    "tests/test_pr463_owner_free_recovery_gate.py",
    "tests/test_pr463_archive_player_boundary_contract.py",
    "docs/reference/player-archive-unsupported-tweedle-diagnostics.md",
    "docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md",
    "docs/tutorials/player-archive-unsupported-this-call-diagnostic.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml",
)
FOCUSED_REPAIR_PATH_SET = frozenset(FOCUSED_REPAIR_PATHS)
ARCHIVE_PLAYER_EVIDENCE_SURFACES = (
    "docs/reference/player-archive-unsupported-tweedle-diagnostics.md",
    "docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md",
    "docs/tutorials/player-archive-unsupported-this-call-diagnostic.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml",
    (
        "core/story-api-migration/src/test/java/org/lgna/project/io/"
        "HistoricalArchiveRoundTripCharacterizationTest.java"
    ),
    "core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java",
)
ARCHIVE_PLAYER_EVIDENCE_SURFACE_SET = frozenset(ARCHIVE_PLAYER_EVIDENCE_SURFACES)
REQUIRED_VALIDATION_NAMES = frozenset(
    (
        "python-pr463-contracts",
        "alice-desktop-scenario-catalog",
        "story-api-migration-characterization",
        "core-ast-decoder-boundary",
    )
)
MAVEN_VALIDATION_NAMES = frozenset(
    (
        "story-api-migration-characterization",
        "core-ast-decoder-boundary",
    )
)
REQUIRED_STATUS_CHECK_NAMES = frozenset(
    (
        "Alice Coverage Reports/coverage",
        "package-netbeans",
        "test",
        "build",
        "GitGuardian Security Checks",
    )
)
BOUNDARY_CURRENT_FLAGS = (
    "referenceDocCurrent",
    "howtoCurrent",
    "tutorialCurrent",
    "archiveScenarioCurrent",
    "tweedleScenarioCurrent",
    "characterizationTestsCurrent",
)
EXPECTED_NONCLAIMS = frozenset(
    (
        "full Tweedle/player decode",
        "historical archive migration completeness",
        "full UI automation",
        "visible rendering correctness",
        "grading",
        "Save/Open guarantees",
        "lesson completion",
    )
)
GREEN_CONCLUSIONS = frozenset(("success",))
COMPLETED_CHECK_STATUSES = frozenset(("completed",))
GENERATED_ARCHIVE_SUFFIXES = (".a3w",)
BINARY_CORPUS_SUFFIXES = (".zip", ".jar", ".tar", ".tgz", ".gz", ".bin")
SENSITIVE_OUTPUT_REDACTIONS = (
    (re.compile(r"\b(authorization:\s*(?:bearer|token)\s+)[^\s]+", re.IGNORECASE), r"\1<redacted>"),
    (re.compile(r"\b((?:github_)?token|password|secret)\s*=\s*[^\s]+", re.IGNORECASE), r"\1=<redacted>"),
    (re.compile(r"\b(?:gh[pousr]|github_pat)_[A-Za-z0-9_]{20,}\b"), "<redacted>"),
)


LOGGER = logging.getLogger("pr463_recovery_gate")


class GitHubServiceError(RuntimeError):
    """Raised when live GitHub PR evidence cannot be retrieved safely."""


def _github_pr_view_command(repository: str, pr_number: int) -> list[str]:
    return [
        "gh",
        "pr",
        "view",
        str(pr_number),
        "--repo",
        repository,
        "--json",
        ",".join(GITHUB_PR_VIEW_FIELDS),
    ]


def fetch_github_pr_evidence(
    *,
    repository: str = EXPECTED_REPOSITORY,
    pr_number: int = EXPECTED_PR_NUMBER,
    timeout_seconds: float = DEFAULT_GITHUB_TIMEOUT_SECONDS,
    max_attempts: int = DEFAULT_GITHUB_RETRY_ATTEMPTS,
    retry_delay_seconds: float = DEFAULT_GITHUB_RETRY_DELAY_SECONDS,
    runner: Callable[..., subprocess.CompletedProcess] = subprocess.run,
    sleeper: Callable[[float], None] = time.sleep,
) -> dict[str, Any]:
    """Fetch and normalize live PR state through `gh pr view`."""
    command = _github_pr_view_command(repository, pr_number)
    pr_view = _fetch_github_pr_view_json(
        command=command,
        timeout_seconds=timeout_seconds,
        max_attempts=max(1, max_attempts),
        retry_delay_seconds=max(0.0, retry_delay_seconds),
        runner=runner,
        sleeper=sleeper,
    )
    return github_pr_view_to_evidence(pr_view)


def _fetch_github_pr_view_json(
    *,
    command: list[str],
    timeout_seconds: float,
    max_attempts: int,
    retry_delay_seconds: float,
    runner: Callable[..., subprocess.CompletedProcess],
    sleeper: Callable[[float], None],
) -> dict[str, Any]:
    last_error: GitHubServiceError | None = None
    for attempt in range(1, max_attempts + 1):
        try:
            result = runner(
                command,
                check=False,
                capture_output=True,
                text=True,
                timeout=timeout_seconds,
            )
            if result.returncode == 0:
                return _parse_github_json(result.stdout)
            last_error = _github_exit_error(result)
        except FileNotFoundError as exc:
            raise GitHubServiceError("gh CLI is not installed or not on PATH") from exc
        except subprocess.TimeoutExpired as exc:
            last_error = _github_timeout_error(exc, timeout_seconds)
        except GitHubServiceError as exc:
            last_error = exc
        if attempt < max_attempts:
            LOGGER.info(
                "GitHub PR evidence fetch attempt %d/%d failed: %s",
                attempt,
                max_attempts,
                last_error,
            )
            sleeper(retry_delay_seconds)
    raise last_error or GitHubServiceError("gh pr view failed without an error message")


def _github_exit_error(result: subprocess.CompletedProcess) -> GitHubServiceError:
    return GitHubServiceError(
        "gh pr view failed with exit code "
        f"{result.returncode}: {_safe_service_output(result.stderr)}"
    )


def _github_timeout_error(
    exc: subprocess.TimeoutExpired, timeout_seconds: float
) -> GitHubServiceError:
    message = f"gh pr view timed out after {timeout_seconds:g}s"
    if exc.stderr:
        message = f"{message}: {_safe_service_output(exc.stderr)}"
    return GitHubServiceError(message)


def _as_mapping(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _text(value: Any) -> str:
    return "" if value is None else str(value)


def _normalized_text(value: Any) -> str:
    return _text(value).strip().lower()


def _safe_service_output(value: Any, limit: int = 400) -> str:
    if isinstance(value, bytes):
        value = value.decode("utf-8", errors="replace")
    text = " ".join(_text(value).split())
    for pattern, replacement in SENSITIVE_OUTPUT_REDACTIONS:
        text = pattern.sub(replacement, text)
    return text[:limit] if text else "no stderr"


def _parse_github_json(stdout: str) -> dict[str, Any]:
    try:
        data = json.loads(stdout)
    except json.JSONDecodeError as exc:
        raise GitHubServiceError("gh pr view returned invalid JSON") from exc
    if not isinstance(data, dict):
        raise GitHubServiceError("gh pr view JSON must be an object")
    return data


def _normalize_github_check(check: dict[str, Any]) -> dict[str, Any]:
    normalized = {
        "name": _text(check.get("name")).strip(),
        "status": _text(check.get("status")).strip(),
        "conclusion": _text(check.get("conclusion")).strip(),
    }
    if check.get("workflowName") is not None:
        normalized["workflowName"] = _text(check.get("workflowName")).strip()
    return normalized


def github_pr_view_to_evidence(pr_view: dict[str, Any]) -> dict[str, Any]:
    """Convert `gh pr view` output into the evidence shape consumed by the gate."""
    required_fields = (
        "baseRefName",
        "headRefName",
        "headRefOid",
        "mergeable",
        "mergeStateStatus",
        "statusCheckRollup",
    )
    missing = [field for field in required_fields if field not in pr_view]
    if missing:
        raise GitHubServiceError("gh pr view response missing fields: " + ", ".join(missing))

    head_sha = _text(pr_view.get("headRefOid"))
    checks = [
        _normalize_github_check(check)
        for check in _as_list(pr_view.get("statusCheckRollup"))
        if isinstance(check, dict)
    ]
    return {
        "repository": EXPECTED_REPOSITORY,
        "prNumber": EXPECTED_PR_NUMBER,
        "branch": _text(pr_view.get("headRefName")),
        "baseRef": _text(pr_view.get("baseRefName")),
        "headSha": head_sha,
        "prHeadSha": head_sha,
        "mergeable": _text(pr_view.get("mergeable")),
        "mergeStateStatus": _text(pr_view.get("mergeStateStatus")),
        "githubActions": {
            "headSha": head_sha,
            "checks": checks,
        },
    }


def merge_github_evidence(evidence: dict[str, Any], github_evidence: dict[str, Any]) -> dict[str, Any]:
    """Overlay live GitHub state while preserving local validation evidence."""
    merged = dict(evidence)
    for key in (
        "repository",
        "prNumber",
        "branch",
        "baseRef",
        "headSha",
        "prHeadSha",
        "mergeable",
        "mergeStateStatus",
    ):
        merged[key] = github_evidence[key]
    github_actions = dict(_as_mapping(merged.get("githubActions")))
    github_actions.update(_as_mapping(github_evidence.get("githubActions")))
    merged["githubActions"] = github_actions
    return merged


def record_external_service_error(
    evidence: dict[str, Any], *, service: str, operation: str, message: str
) -> dict[str, Any]:
    merged = dict(evidence)
    errors = list(_as_list(merged.get("externalServiceErrors")))
    errors.append({"service": service, "operation": operation, "message": message})
    merged["externalServiceErrors"] = errors
    return merged


def _head_sha(evidence: dict[str, Any]) -> str:
    return _text(evidence.get("headSha"))


def _dedupe(blockers: Iterable[str]) -> list[str]:
    return list(dict.fromkeys(blockers))


def _normalize_argv(value: Any) -> tuple[str, ...]:
    if isinstance(value, list):
        return tuple(_text(item) for item in value)
    if isinstance(value, tuple):
        return tuple(_text(item) for item in value)
    if isinstance(value, str):
        try:
            return tuple(shlex.split(value))
        except ValueError:
            return tuple(value.split())
    return ()


def _split_command_text(command: str) -> tuple[str, ...]:
    try:
        lexer = shlex.shlex(command, posix=True, punctuation_chars=";&|()<>")
        lexer.whitespace_split = True
        return tuple(lexer)
    except ValueError:
        return tuple(
            token
            for segment in re.split(r"&&|\|\||[;&|()<>]", command)
            for token in segment.split()
        )


def _command_tokens(command: Any) -> tuple[str, ...]:
    if isinstance(command, list):
        tokens = tuple(_text(part) for part in command)
    elif isinstance(command, str):
        tokens = _split_command_text(command)
    else:
        return ()

    expanded: list[str] = []
    for token in tokens:
        expanded.append(token)
        if any(character.isspace() or character in ";&|()<>" for character in token):
            expanded.extend(_split_command_text(token))
    return tuple(expanded)


def _has_manual_merge_command(command: tuple[str, ...]) -> bool:
    for index, token in enumerate(command):
        if token == "gh" and command[index + 1 : index + 3] == ("pr", "merge"):
            return True
        if token == "git" and "merge" in command[index + 1 : index + 5]:
            return True
    return False


def _has_git_push_command(command: tuple[str, ...]) -> bool:
    for index, token in enumerate(command):
        if token == "git" and "push" in command[index + 1 : index + 4]:
            return True
    return False


def _has_parent_traversal(path: str) -> bool:
    return path == ".." or path.startswith("../") or path.endswith("/..") or "/../" in path


def _path_payload_flags(path: str) -> tuple[bool, bool]:
    lower_path = path.lower()
    is_generated_archive = lower_path.endswith(GENERATED_ARCHIVE_SUFFIXES)
    is_binary_payload = lower_path.endswith(BINARY_CORPUS_SUFFIXES)
    return is_generated_archive, is_binary_payload


def _path_in_focused_scope(path: str) -> bool:
    return path in FOCUSED_REPAIR_PATH_SET


def _check_name(check: dict[str, Any]) -> str:
    return _text(check.get("name")).strip()


def _canonical_check_name(check: dict[str, Any]) -> str:
    name = _check_name(check)
    if name == "coverage":
        return "Alice Coverage Reports/coverage"
    return name


def _iter_status_checks(evidence: dict[str, Any]) -> Iterable[dict[str, Any]]:
    return (
        check
        for check in _as_list(_as_mapping(evidence.get("githubActions")).get("checks"))
        if isinstance(check, dict)
    )


def _has_archive_player_failure(evidence: dict[str, Any]) -> bool:
    if evidence.get("repairRequired") is True:
        return True
    for check in _iter_status_checks(evidence):
        conclusion = _normalized_text(check.get("conclusion"))
        failure_surface = _normalized_text(check.get("failureSurface"))
        name = _normalized_text(check.get("name"))
        if conclusion == "failure" and (
            failure_surface == "archive-player-boundary"
            or "archive/player" in name
            or "archive-player" in name
        ):
            return True
    return False


def verify_external_service_errors(evidence: dict[str, Any]) -> list[str]:
    """Block success when live external evidence could not be refreshed."""
    has_github_pr_service_error = False
    has_other_service_error = False
    for raw_error in _as_list(evidence.get("externalServiceErrors")):
        error = _as_mapping(raw_error)
        service = _normalized_text(error.get("service"))
        operation = _normalized_text(error.get("operation"))
        if service == "github" and operation == "gh pr view":
            has_github_pr_service_error = True
        else:
            has_other_service_error = True

    blockers: list[str] = []
    if has_github_pr_service_error:
        blockers.append("github-pr-service-unavailable")
    if has_other_service_error:
        blockers.append("external-service-error")
    return blockers


def verify_pr_state(evidence: dict[str, Any]) -> list[str]:
    """Require a clean, repaired PR head against the recorded develop base."""
    blockers: list[str] = []
    head_sha = _head_sha(evidence)
    local_head_sha = _text(evidence.get("localHeadSha"))
    pr_head_sha = _text(evidence.get("prHeadSha"))

    if not head_sha or not local_head_sha or not pr_head_sha:
        blockers.append("missing-pr-head-evidence")
    if evidence.get("repository") != EXPECTED_REPOSITORY:
        blockers.append("wrong-repository")
    if evidence.get("prNumber") != EXPECTED_PR_NUMBER:
        blockers.append("wrong-pr-number")
    if evidence.get("branch") != EXPECTED_BRANCH:
        blockers.append("wrong-authoritative-branch")
    if evidence.get("baseRef") not in (EXPECTED_BASE_REF, f"origin/{EXPECTED_BASE_REF}"):
        blockers.append("wrong-base-ref")
    if not _text(evidence.get("developBaseSha")):
        blockers.append("missing-develop-base-sha")
    if evidence.get("worktreeClean") is not True:
        blockers.append("dirty-worktree")
    if head_sha and local_head_sha and head_sha != local_head_sha:
        blockers.append("local-head-sha-mismatch")
    if head_sha and pr_head_sha and head_sha != pr_head_sha:
        blockers.append("pr-head-sha-mismatch")
    if _text(evidence.get("mergeable")).upper() != "MERGEABLE":
        blockers.append("pr-not-mergeable")
    if _text(evidence.get("mergeStateStatus")).upper() != "CLEAN":
        blockers.append("pr-merge-state-not-clean")
    if evidence.get("recoveryMode") != EXPECTED_RECOVERY_MODE:
        blockers.append("wrong-recovery-mode")
    if evidence.get("scope") != EXPECTED_SCOPE:
        blockers.append("wrong-recovery-scope")
    if evidence.get("manualMergePerformed") is True:
        blockers.append("manual-merge-performed")
    if evidence.get("manualMergeUsed") is True:
        blockers.append("manual-merge-used")
    if evidence.get("replacementPullRequestCreated") is True:
        blockers.append("replacement-pr-created")
    if evidence.get("noOpModeUsed") is True or evidence.get("noOpJustificationUsed") is True:
        blockers.append("noop-mode-used")

    return blockers


def verify_github_actions(evidence: dict[str, Any]) -> list[str]:
    """Require completed successful GitHub checks for the exact PR head."""
    actions = _as_mapping(evidence.get("githubActions"))
    if not actions:
        return ["missing-github-actions-evidence"]

    blockers: list[str] = []
    if actions.get("headSha") != _head_sha(evidence):
        blockers.append("github-actions-stale-head")

    checks = _as_list(actions.get("checks"))
    if not checks:
        blockers.append("missing-github-actions-checks")
    seen_check_names: set[str] = set()
    has_incomplete_check = False
    has_non_green_check = False
    for raw_check in checks:
        check = _as_mapping(raw_check)
        check_name = _canonical_check_name(check)
        if check_name:
            seen_check_names.add(check_name)
        status = _normalized_text(check.get("status"))
        conclusion = _normalized_text(check.get("conclusion"))
        if status not in COMPLETED_CHECK_STATUSES:
            has_incomplete_check = True
        elif conclusion not in GREEN_CONCLUSIONS:
            has_non_green_check = True
    if has_incomplete_check:
        blockers.append("github-actions-not-complete")
    if has_non_green_check:
        blockers.append("github-actions-not-green")
    if not REQUIRED_STATUS_CHECK_NAMES.issubset(seen_check_names):
        blockers.append("github-actions-required-check-missing")

    return blockers


def verify_repair_scope(evidence: dict[str, Any]) -> list[str]:
    """Allow repair diffs only on PR #463 guard and archive/player evidence surfaces."""
    blockers: list[str] = []
    repair_diff_files = evidence.get("repairDiffFiles")
    changed_file_list = _as_list(repair_diff_files)

    if evidence.get("repairRequired") is not True:
        return blockers

    if evidence.get("pushedRepair") is not True:
        blockers.append("focused-repair-not-pushed")
    if not changed_file_list:
        blockers.append("missing-focused-repair-diff")

    has_unfocused_diff = False
    has_generated_archive = False
    has_binary_payload = False
    for raw_path in changed_file_list:
        path = _text(raw_path)
        if not path or path.startswith("/") or _has_parent_traversal(path):
            has_unfocused_diff = True
            continue
        is_generated_archive, is_binary_payload = _path_payload_flags(path)
        if is_generated_archive:
            has_generated_archive = True
        if is_binary_payload and not is_generated_archive:
            has_binary_payload = True
        if not _path_in_focused_scope(path):
            has_unfocused_diff = True
    if has_unfocused_diff:
        blockers.append("unfocused-diff-scope")
    if has_generated_archive:
        blockers.append("generated-archive-committed")
    if has_binary_payload:
        blockers.append("binary-corpus-payload-committed")

    return blockers


def verify_boundary_evidence(evidence: dict[str, Any]) -> list[str]:
    """Require current bounded archive/player docs, QA scenarios, and tests."""
    boundary = _as_mapping(evidence.get("boundaryEvidence"))
    if not boundary:
        return ["missing-boundary-evidence"]

    blockers: list[str] = []
    surfaces = _as_list(evidence.get("archivePlayerEvidenceSurfaces"))
    if not surfaces:
        blockers.append("archive-player-evidence-surfaces-missing")
    else:
        broadened_scope = any(
            not _text(surface) or _text(surface) not in ARCHIVE_PLAYER_EVIDENCE_SURFACE_SET
            for surface in surfaces
        )
        if broadened_scope:
            blockers.append("archive-player-evidence-scope-broadened")

    evidence_stale = boundary.get("headSha") != _head_sha(evidence) or any(
        boundary.get(flag) is not True for flag in BOUNDARY_CURRENT_FLAGS
    )
    if evidence_stale:
        blockers.append("archive-player-evidence-stale")
    if _as_list(boundary.get("forbiddenClaims")):
        blockers.append("archive-player-evidence-overclaims")

    nonclaims = {_text(item) for item in _as_list(boundary.get("nonclaims"))}
    if not EXPECTED_NONCLAIMS.issubset(nonclaims):
        blockers.append("archive-player-nonclaims-missing")
    if _as_list(boundary.get("generatedArchivesCommitted")):
        blockers.append("generated-archive-committed")
    if _as_list(boundary.get("binaryCorpusPayloadsCommitted")):
        blockers.append("binary-corpus-payload-committed")

    return blockers


def verify_qa_scenario_contracts(evidence: dict[str, Any]) -> list[str]:
    """Require schema, runner, validator, and argv alignment for QA scenarios."""
    contracts = _as_mapping(evidence.get("qaScenarioContracts"))
    if not contracts:
        return ["missing-qa-scenario-contract-evidence"]

    blockers: list[str] = []
    if contracts.get("validated") is not True:
        blockers.append("qa-scenario-validation-not-run")

    workflows = _as_mapping(contracts.get("workflows"))
    for workflow, expected_argv in EXPECTED_WORKFLOW_ARGV.items():
        contract = _as_mapping(workflows.get(workflow))
        if not contract:
            blockers.append("qa-workflow-contract-missing")
            continue
        if contract.get("allowlistedInValidator") is not True:
            blockers.append("qa-workflow-validator-not-allowlisted")
        if contract.get("allowlistedInRunner") is not True:
            blockers.append("qa-workflow-runner-not-allowlisted")
        if contract.get("listedInSchema") is not True:
            blockers.append("qa-workflow-schema-not-allowlisted")
        if contract.get("automationMode") != EXPECTED_AUTOMATION_MODE:
            blockers.append("qa-workflow-automation-mode-not-gated")
        if _normalize_argv(contract.get("argv")) != expected_argv:
            blockers.append("qa-workflow-argv-not-focused")

    return _dedupe(blockers)


def verify_validation_evidence(evidence: dict[str, Any]) -> list[str]:
    """Require focused local validation evidence with the saved Node option."""
    validations = _as_list(evidence.get("validations"))
    if not validations:
        return ["missing-validation-evidence"]

    blockers: list[str] = []
    if evidence.get("tweedleLangInitialized") is not True:
        blockers.append("tweedle-lang-not-initialized")
    if evidence.get("nodeOptions") != EXPECTED_NODE_OPTIONS:
        blockers.append("missing-node-options")

    records_by_name: dict[str, dict[str, Any]] = {}
    for raw_record in validations:
        record = _as_mapping(raw_record)
        name = _text(record.get("name"))
        if name:
            records_by_name[name] = record
        if not _text(record.get("command")):
            blockers.append("validation-command-missing")
        if record.get("headSha") != _head_sha(evidence):
            blockers.append("validation-stale-head")

    missing_names = REQUIRED_VALIDATION_NAMES - records_by_name.keys()
    if "python-pr463-contracts" in missing_names or _normalized_text(
        records_by_name.get("python-pr463-contracts", {}).get("outcome")
    ) != "passed":
        blockers.append("python-contract-validation-failed")
    if "alice-desktop-scenario-catalog" in missing_names or _normalized_text(
        records_by_name.get("alice-desktop-scenario-catalog", {}).get("outcome")
    ) != "passed":
        blockers.append("qa-scenario-validation-failed")
    if missing_names & MAVEN_VALIDATION_NAMES:
        blockers.append("focused-maven-validation-missing")
    elif any(
        _normalized_text(records_by_name[name].get("outcome")) != "passed"
        for name in MAVEN_VALIDATION_NAMES
    ):
        blockers.append("focused-maven-validation-failed")

    return _dedupe(blockers)


def verify_pr_evidence(evidence: dict[str, Any]) -> list[str]:
    """Require PR description evidence to be current and bounded."""
    pr_evidence = _as_mapping(evidence.get("prEvidence"))
    if not pr_evidence:
        return ["missing-pr-evidence"]

    blockers: list[str] = []
    if pr_evidence.get("headSha") != _head_sha(evidence):
        blockers.append("pr-evidence-stale-head")
    if pr_evidence.get("currentHeadEvidence") is not True:
        blockers.append("pr-evidence-missing-current-head")
    if pr_evidence.get("mergeReadyCriteriaUpdated") is not True:
        blockers.append("pr-evidence-missing-merge-ready-criteria")
    if pr_evidence.get("boundedArchivePlayerClaimsOnly") is not True:
        blockers.append("pr-evidence-overclaims-archive-player-boundary")
    if _as_list(pr_evidence.get("blockers")):
        blockers.append("pr-evidence-open-blockers")

    return blockers


def verify_command_safety(evidence: dict[str, Any]) -> list[str]:
    """Reject manual merge commands and unsafe no-op or push disposition."""
    has_manual_merge = evidence.get("manualMergeUsed") is True
    has_missing_commands = False
    has_unexpected_push = False

    commands = _as_list(evidence.get("commands"))
    if not commands:
        has_missing_commands = True
    for command in commands:
        tokens = _command_tokens(command)
        if not tokens:
            continue
        if _has_manual_merge_command(tokens):
            has_manual_merge = True
        if _has_git_push_command(tokens) and evidence.get("repairRequired") is not True:
            has_unexpected_push = True

    blockers: list[str] = []
    if evidence.get("manualMergePerformed") is True:
        blockers.append("manual-merge-performed")
    if evidence.get("replacementPullRequestCreated") is True:
        blockers.append("replacement-pr-created")
    if evidence.get("noOpModeUsed") is True or evidence.get("noOpJustificationUsed") is True:
        blockers.append("noop-mode-used")
    if has_manual_merge:
        blockers.append("manual-merge-used")
    if has_missing_commands:
        blockers.append("missing-command-evidence")
    if evidence.get("repairRequired") is True and evidence.get("noOpJustificationUsed") is True:
        blockers.append("noop-used-despite-required-repair")
    if evidence.get("repairRequired") is not True and evidence.get("pushedRepair") is True:
        has_unexpected_push = True
    if has_unexpected_push:
        blockers.append("unexpected-push-without-repair")

    return blockers


VERIFIERS = (
    verify_external_service_errors,
    verify_pr_state,
    verify_github_actions,
    verify_repair_scope,
    verify_boundary_evidence,
    verify_qa_scenario_contracts,
    verify_validation_evidence,
    verify_pr_evidence,
    verify_command_safety,
)


def evaluate_readiness(evidence: dict[str, Any]) -> dict[str, Any]:
    """Return PR #463 readiness with explicit blockers and repair scope."""
    blockers = _dedupe(blocker for verifier in VERIFIERS for blocker in verifier(evidence))
    head_sha = _head_sha(evidence)
    repair_required = _has_archive_player_failure(evidence)

    if not blockers:
        LOGGER.info("PR #463 focused archive/player recovery gate passed for head %s", head_sha)
        summary = (
            "PR #463 is merge-ready after focused archive/player repair: "
            "the current head is clean, checks are green, mergeability is clean, "
            "and archive/player boundary evidence is current."
        )
        return {
            "status": "MERGE_READY",
            "headSha": head_sha,
            "blockers": [],
            "repairRequired": False,
            "recoveryMode": evidence.get("recoveryMode"),
            "allowedRepairPaths": [],
            "mayUseNoOpJustification": False,
            "summary": summary,
        }

    if repair_required:
        LOGGER.info("PR #463 recovery requires focused archive/player repair")
        return {
            "status": "REPAIR_REQUIRED",
            "headSha": head_sha,
            "blockers": blockers,
            "repairRequired": True,
            "allowedRepairPaths": list(FOCUSED_REPAIR_PATHS),
            "mayUseNoOpJustification": False,
            "summary": "Focused archive/player boundary repair is required: " + ", ".join(blockers),
        }

    LOGGER.info("PR #463 recovery gate blocked by %d criterion/criteria", len(blockers))
    return {
        "status": "NOT_MERGE_READY",
        "headSha": head_sha,
        "blockers": blockers,
        "repairRequired": False,
        "allowedRepairPaths": [],
        "mayUseNoOpJustification": False,
        "summary": "Recovery readiness is blocked: " + ", ".join(blockers),
    }


def load_evidence(path: Path | None) -> dict[str, Any]:
    """Load evidence JSON from a file, or stdin when no file is provided."""
    if path is None:
        data = json.load(sys.stdin)
    else:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError("recovery evidence JSON must be an object")
    return data


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Evaluate PR #463 focused archive/player recovery evidence and print readiness JSON."
    )
    parser.add_argument(
        "evidence",
        nargs="?",
        type=Path,
        help="Path to evidence JSON. Reads stdin when omitted.",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Pretty-print the readiness result.",
    )
    parser.add_argument(
        "--refresh-github",
        action="store_true",
        help="Refresh live PR state and GitHub check evidence with gh before evaluation.",
    )
    parser.add_argument(
        "--github-timeout",
        type=float,
        default=DEFAULT_GITHUB_TIMEOUT_SECONDS,
        help="Seconds to wait for each gh pr view attempt when --refresh-github is used.",
    )
    parser.add_argument(
        "--github-retries",
        type=int,
        default=DEFAULT_GITHUB_RETRY_ATTEMPTS,
        help="Number of gh pr view attempts when --refresh-github is used.",
    )
    parser.add_argument(
        "--github-retry-delay",
        type=float,
        default=DEFAULT_GITHUB_RETRY_DELAY_SECONDS,
        help="Seconds to wait between gh pr view attempts when --refresh-github is used.",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Enable informational logging on stderr.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    logging.basicConfig(
        level=logging.INFO if args.verbose else logging.WARNING,
        format="%(levelname)s: %(message)s",
    )
    evidence = load_evidence(args.evidence)
    if args.refresh_github:
        try:
            github_evidence = fetch_github_pr_evidence(
                timeout_seconds=args.github_timeout,
                max_attempts=args.github_retries,
                retry_delay_seconds=args.github_retry_delay,
            )
            evidence = merge_github_evidence(evidence, github_evidence)
        except GitHubServiceError as exc:
            LOGGER.error("Could not refresh GitHub PR evidence: %s", exc)
            evidence = record_external_service_error(
                evidence,
                service="github",
                operation="gh pr view",
                message=str(exc),
            )

    result = evaluate_readiness(evidence)
    indent = 2 if args.pretty else None
    print(json.dumps(result, indent=indent, sort_keys=True))
    return 0 if result["status"] == "MERGE_READY" else 1


if __name__ == "__main__":
    raise SystemExit(main())
