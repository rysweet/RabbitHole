#!/usr/bin/env python3
"""Evidence-based merge-ready gate for RabbitHole PR #428.

The gate is intentionally stricter than GitHub's green-check signal. It only
returns ready when the current PR head, focused validation evidence, docs
impact, scenario applicability, quality audit cycles, checks, and PR body all
agree on the same bounded evidence package.
"""

from __future__ import annotations

import argparse
import json
import re
import shlex
import sys
from dataclasses import dataclass
from pathlib import PurePosixPath
from typing import Iterable, Mapping, Sequence


REQUIRED_REMOTE_REF = "origin/feat/issue-408-rabbithole-wave7-coverage-ratchet-lane-follow-defa"
DEFAULT_EXPECTED_HEAD_SHA = "41333b64a0d772ca7aab982090bfaa135ed02e2e"
FOCUSED_WORKER_TEST = "org.lgna.issue.IssueSubmissionProgressWorkerTest"
FOCUSED_MAVEN_FRAGMENT = "mvn -pl core/issue-reporting -am -DfailIfNoTests=false"
REQUIRED_NODE_OPTIONS = "NODE_OPTIONS=--max-old-space-size=32768"

ALLOWED_DIFF_FILES = {
    "core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java",
    "core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java",
    "docs/reference/issue-submission-progress-worker.md",
    "docs/howto/characterize-issue-submission-progress-worker.md",
    "docs/tutorials/trace-issue-submission-progress-worker.md",
    "docs/index.md",
    "pyproject.toml",
}

SUCCESS_CONCLUSIONS = {"SUCCESS", "NEUTRAL", "SKIPPED"}
GH_CHECK_BUCKETS = {
    "pass": ("COMPLETED", "SUCCESS"),
    "skipping": ("COMPLETED", "SKIPPED"),
    "fail": ("COMPLETED", "FAILURE"),
    "cancel": ("COMPLETED", "CANCELLED"),
    "pending": ("IN_PROGRESS", ""),
}
GH_COMPLETED_STATES = {"SUCCESS", "FAILURE", "CANCELLED", "SKIPPED", "NEUTRAL"}
RATE_LIMIT_RE = re.compile(r"\brate[\s_-]*limit(?:ed| exceeded)?\b", re.IGNORECASE)
TIMEOUT_FRAGMENT_RE = re.compile(r"(?:^|\s)(?:(?:/usr/bin|/bin)/)?g?timeout(?:\s|$)")
TIMEOUT_PROGRAMS = {"timeout", "gtimeout"}
OVERCLAIM_PATTERNS = {
    "full UI automation": re.compile(r"\bprove[sd]?\s+full\s+ui\s+automation\b", re.IGNORECASE),
    "visible rendering correctness": re.compile(
        r"\bprove[sd]?.*visible\s+rendering\s+correctness\b", re.IGNORECASE | re.DOTALL
    ),
    "grading": re.compile(r"\bprove[sd]?.*\bgrading\b", re.IGNORECASE | re.DOTALL),
    "creative assessment": re.compile(
        r"\bprove[sd]?.*creative\s+assessment\b", re.IGNORECASE | re.DOTALL
    ),
    "full lesson completion": re.compile(
        r"\bprove[sd]?.*full\s+lesson\s+completion\b", re.IGNORECASE | re.DOTALL
    ),
    "full Tweedle/player decode": re.compile(
        r"\bprove[sd]?.*full\s+tweedle/player\s+decode\b", re.IGNORECASE | re.DOTALL
    ),
}


@dataclass(frozen=True)
class GateResult:
    """Result for one or more merge-ready predicates."""

    ready: bool
    blockers: list[str]


@dataclass(frozen=True)
class ExitClassification:
    """Classification for owner-free workflow exits."""

    kind: str
    ready: bool
    blocker: str


def ready_result() -> GateResult:
    return GateResult(ready=True, blockers=[])


def blocked_result(message: str) -> GateResult:
    return GateResult(ready=False, blockers=[f"NOT_MERGE_READY: {message}"])


def combine_results(results: Iterable[GateResult]) -> GateResult:
    blockers: list[str] = []
    for result in results:
        blockers.extend(result.blockers)
    return GateResult(ready=not blockers, blockers=blockers)


def _clean_sha(value: object) -> str:
    return str(value or "").strip()


def _as_text(value: object) -> str:
    return str(value or "").strip()


def validate_branch_sync(
    *,
    current_ref: str,
    local_head: str,
    remote_head: str,
    manual_merge_seen: bool,
    required_ref: str = REQUIRED_REMOTE_REF,
) -> GateResult:
    """Accept only the required remote PR branch at the current remote head."""

    blockers: list[str] = []
    if current_ref != required_ref:
        blockers.append(
            f"NOT_MERGE_READY: branch sync must use {required_ref}, got {current_ref or '<missing>'}"
        )
    if not _clean_sha(local_head) or _clean_sha(local_head) != _clean_sha(remote_head):
        blockers.append(
            "NOT_MERGE_READY: local head must match the current remote head before validation"
        )
    if manual_merge_seen:
        blockers.append("NOT_MERGE_READY: manual merge/rebase/squash evidence is not allowed")
    return GateResult(ready=not blockers, blockers=blockers)


def _normalize_diff_path(path: str) -> str:
    normalized = PurePosixPath(path.replace("\\", "/")).as_posix()
    if normalized.startswith("../") or normalized == ".." or normalized.startswith("/"):
        raise ValueError(f"diff path must be repository-relative: {path}")
    return normalized


def audit_diff_scope(
    changed_files: Iterable[str],
    allowed_files: set[str] | None = None,
) -> GateResult:
    """Require the PR diff to stay within the documented worker lane."""

    allowed = allowed_files or ALLOWED_DIFF_FILES
    unexpected: list[str] = []
    for path in changed_files:
        normalized = _normalize_diff_path(path)
        if normalized not in allowed:
            unexpected.append(normalized)
    if unexpected:
        files = ", ".join(sorted(unexpected))
        return blocked_result(f"diff scope includes unrelated files: {files}")
    return ready_result()


def _command_tokens(command: str) -> list[str]:
    try:
        return shlex.split(command)
    except ValueError as exc:
        raise ValueError(f"validation command is not parseable: {command}") from exc


def _uses_timeout_wrapper(command: str) -> bool:
    tokens = _command_tokens(command)
    for token in tokens:
        program = PurePosixPath(token).name
        if program in TIMEOUT_PROGRAMS or TIMEOUT_FRAGMENT_RE.search(token):
            return True
    return False


def _has_focused_worker_command(command: str) -> bool:
    command_text = " ".join(command.split())
    return (
        REQUIRED_NODE_OPTIONS in command_text
        and FOCUSED_MAVEN_FRAGMENT in command_text
        and f"-Dtest={FOCUSED_WORKER_TEST}" in command_text
        and " test" in command_text
    )


def validate_runnable_evidence(
    evidence: Iterable[Mapping[str, object]],
    expected_head_sha: str = DEFAULT_EXPECTED_HEAD_SHA,
) -> GateResult:
    """Require current-head focused worker validation with no timeout wrapper."""

    blockers: list[str] = []
    focused_seen = False
    for index, entry in enumerate(evidence, start=1):
        command = _as_text(entry.get("command"))
        if not command:
            blockers.append(f"NOT_MERGE_READY: runnable evidence #{index} is missing a command")
            continue
        if _uses_timeout_wrapper(command):
            blockers.append(
                f"NOT_MERGE_READY: runnable evidence #{index} uses a timeout wrapper: {command}"
            )
        if not bool(entry.get("passed")):
            blockers.append(f"NOT_MERGE_READY: runnable evidence #{index} did not pass")
        head_sha = _clean_sha(entry.get("head_sha"))
        if expected_head_sha and head_sha != expected_head_sha:
            blockers.append(
                "NOT_MERGE_READY: runnable evidence must be from the current head "
                f"{expected_head_sha}, got {head_sha or '<missing>'}"
            )
        if _has_focused_worker_command(command):
            focused_seen = True
    if not focused_seen:
        blockers.append(
            "NOT_MERGE_READY: focused QA must include IssueSubmissionProgressWorkerTest "
            "on core/issue-reporting with the saved NODE_OPTIONS setting"
        )
    return GateResult(ready=not blockers, blockers=blockers)


def validate_docs_impact(docs_impact: Mapping[str, object] | None) -> GateResult:
    """Require an explicit docs-impact assessment."""

    if not docs_impact or not docs_impact.get("assessed"):
        return blocked_result("docs impact must be assessed for this PR head")
    files = docs_impact.get("files")
    if files is not None and (
        isinstance(files, str) or not isinstance(files, Sequence)
    ):
        return blocked_result("docs impact files must be listed when supplied")
    return ready_result()


def validate_scenario_evidence(scenario_evidence: Mapping[str, object] | None) -> GateResult:
    """Require scenario applicability without overstating non-UI worker coverage."""

    if not scenario_evidence:
        return blocked_result("scenario evidence must be assessed")
    applicability = _as_text(scenario_evidence.get("applicability")).lower()
    reason = _as_text(scenario_evidence.get("reason"))
    if applicability == "not_applicable":
        if "worker" not in reason.lower() and "scenario" not in reason.lower():
            return blocked_result(
                "scenario evidence non-applicability must explain the worker seam"
            )
        return ready_result()
    if applicability in {"applicable", "covered"} and scenario_evidence.get("passed"):
        return ready_result()
    return blocked_result("scenario evidence must be passing or explicitly not applicable")


def validate_quality_audit_cycles(cycles: Iterable[Mapping[str, object]]) -> GateResult:
    """Require at least three SEEK / VALIDATE / FIX cycles and a clean final cycle."""

    cycle_list = list(cycles)
    blockers: list[str] = []
    if len(cycle_list) < 3:
        blockers.append("NOT_MERGE_READY: quality audit requires at least three cycles")
    for index, cycle in enumerate(cycle_list, start=1):
        missing = [
            key
            for key in ("seek", "validate", "fix")
            if not _as_text(cycle.get(key))
        ]
        if missing:
            blockers.append(
                "NOT_MERGE_READY: quality audit cycle "
                f"{index} is missing {', '.join(missing)} evidence"
            )
    if cycle_list and not bool(cycle_list[-1].get("clean")):
        blockers.append("NOT_MERGE_READY: quality audit requires a clean final cycle")
    return GateResult(ready=not blockers, blockers=blockers)


def normalize_github_action_check(check: Mapping[str, object]) -> dict[str, str]:
    """Normalize check-run API and ``gh pr checks --json`` shapes."""

    name = _as_text(check.get("name")) or "<unnamed check>"
    status = _as_text(check.get("status")).upper()
    conclusion = _as_text(check.get("conclusion")).upper()
    bucket = _as_text(check.get("bucket")).lower()
    state = _as_text(check.get("state")).upper()

    if bucket:
        bucket_status, bucket_conclusion = GH_CHECK_BUCKETS.get(bucket, ("", ""))
        status = status or bucket_status
        conclusion = conclusion or (state if state in GH_COMPLETED_STATES else bucket_conclusion)
    elif state and not status and not conclusion:
        if state in GH_COMPLETED_STATES:
            status = "COMPLETED"
            conclusion = state
        else:
            status = state

    return {"name": name, "status": status, "conclusion": conclusion}


def parse_github_checks_json(payload: str) -> list[dict[str, str]]:
    """Parse and normalize GitHub CLI PR check JSON output."""

    data = json.loads(payload)
    if not isinstance(data, list):
        raise ValueError("GitHub checks JSON must be a list")
    checks: list[dict[str, str]] = []
    for index, entry in enumerate(data, start=1):
        if not isinstance(entry, Mapping):
            raise ValueError(f"GitHub check #{index} must be an object")
        checks.append(normalize_github_action_check(entry))
    return checks


def validate_github_actions(checks: Iterable[Mapping[str, object]]) -> GateResult:
    """Require all reported GitHub Actions checks to be completed and green."""

    check_list = list(checks)
    if not check_list:
        return blocked_result("GitHub Actions checks are missing")
    blockers: list[str] = []
    for check in check_list:
        normalized_check = normalize_github_action_check(check)
        name = normalized_check["name"]
        status = normalized_check["status"]
        conclusion = normalized_check["conclusion"]
        if status != "COMPLETED":
            blockers.append(f"NOT_MERGE_READY: GitHub Actions check {name} is not completed")
        if conclusion not in SUCCESS_CONCLUSIONS:
            blockers.append(
                f"NOT_MERGE_READY: GitHub Actions check {name} is not green: "
                f"{conclusion or '<missing conclusion>'}"
            )
    return GateResult(ready=not blockers, blockers=blockers)


def _contains_all(text: str, fragments: Iterable[str]) -> list[str]:
    lower_text = text.lower()
    return [fragment for fragment in fragments if fragment.lower() not in lower_text]


def validate_pr_description(
    body: str,
    expected_head_sha: str = DEFAULT_EXPECTED_HEAD_SHA,
) -> GateResult:
    """Require PR-body evidence for every merge-ready gate and bounded claims."""

    blockers: list[str] = []
    body_text = body or ""
    if expected_head_sha and expected_head_sha not in body_text:
        blockers.append(
            f"NOT_MERGE_READY: PR description must include current head {expected_head_sha}"
        )
    required_fragments = [
        "Focused validation",
        "Docs impact",
        "Scenario evidence",
        "Diff scope",
        "Quality audit",
        "SEEK / VALIDATE / FIX",
        "clean final cycle",
        "GitHub Actions",
        "Does not claim",
    ]
    missing = _contains_all(body_text, required_fragments)
    if missing:
        blockers.append(
            "NOT_MERGE_READY: PR description is missing evidence for "
            + ", ".join(missing)
        )
    for label, pattern in OVERCLAIM_PATTERNS.items():
        if pattern.search(body_text):
            blockers.append(f"NOT_MERGE_READY: PR description contains overclaim: {label}")
    return GateResult(ready=not blockers, blockers=blockers)


def classify_owner_free_exit(exit_code: int, owner: str | None, stderr: str) -> ExitClassification:
    """Classify owner-free exits so rate limits never become merge-ready no-ops."""

    stderr_text = stderr or ""
    if owner:
        return ExitClassification(kind="OWNER_PRESENT", ready=True, blocker="")
    if RATE_LIMIT_RE.search(stderr_text):
        return ExitClassification(
            kind="RATE_LIMIT",
            ready=False,
            blocker="NOT_MERGE_READY: owner-free exit was caused by GitHub API rate limit",
        )
    if exit_code == 0:
        return ExitClassification(
            kind="OWNER_FREE",
            ready=False,
            blocker="NOT_MERGE_READY: owner-free exit lacks owner evidence",
        )
    return ExitClassification(
        kind="FAILED",
        ready=False,
        blocker=f"NOT_MERGE_READY: owner resolution failed with exit code {exit_code}",
    )


def evaluate_merge_ready(evidence: Mapping[str, object]) -> GateResult:
    """Evaluate the full PR #428 merge-ready evidence package."""

    expected_head_sha = _clean_sha(evidence.get("expected_head_sha")) or DEFAULT_EXPECTED_HEAD_SHA
    branch = evidence.get("branch")
    branch_result = (
        validate_branch_sync(
            current_ref=_as_text(branch.get("current_ref")),
            local_head=_as_text(branch.get("local_head")),
            remote_head=_as_text(branch.get("remote_head")),
            manual_merge_seen=bool(branch.get("manual_merge_seen")),
        )
        if isinstance(branch, Mapping)
        else blocked_result("branch sync evidence is missing")
    )
    results = [
        branch_result,
        audit_diff_scope(evidence.get("diff_files", [])),
        validate_runnable_evidence(
            evidence.get("runnable_evidence", []),
            expected_head_sha=expected_head_sha,
        ),
        validate_docs_impact(
            evidence.get("docs_impact") if isinstance(evidence.get("docs_impact"), Mapping) else None
        ),
        validate_scenario_evidence(
            evidence.get("scenario_evidence")
            if isinstance(evidence.get("scenario_evidence"), Mapping)
            else None
        ),
        validate_quality_audit_cycles(evidence.get("quality_audit_cycles", [])),
        validate_github_actions(evidence.get("github_checks", [])),
        validate_pr_description(_as_text(evidence.get("pr_description")), expected_head_sha),
    ]
    return combine_results(results)


def load_evidence(path: str) -> Mapping[str, object]:
    with open(path, encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, Mapping):
        raise ValueError("evidence JSON must be an object")
    return data


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("evidence_json", help="Path to a JSON evidence package")
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Enable diagnostic logging while evaluating the gate",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    import logging

    logging.basicConfig(
        level=logging.INFO if args.verbose else logging.WARNING,
        format="%(levelname)s: %(message)s",
    )
    result = evaluate_merge_ready(load_evidence(args.evidence_json))
    if result.ready:
        logging.info("PR #428 merge-ready gate passed")
    else:
        logging.warning("PR #428 merge-ready gate blocked: %s", "; ".join(result.blockers))
    output = {"ready": result.ready, "blockers": result.blockers}
    print(json.dumps(output, indent=2, sort_keys=True))
    return 0 if result.ready else 1


if __name__ == "__main__":
    sys.exit(main())
