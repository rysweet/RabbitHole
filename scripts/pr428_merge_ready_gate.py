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
import logging
import re
import shlex
import sys
from dataclasses import dataclass
from pathlib import PurePosixPath
from typing import Iterable, Mapping, Sequence


REQUIRED_REMOTE_REF = "origin/feat/issue-408-rabbithole-wave7-coverage-ratchet-lane-follow-defa"
DEFAULT_EXPECTED_HEAD_SHA = "2b8a961d67f2365d38b9f6ea833e700e99e351a2"
DEFAULT_EXPECTED_BASE_SHA = "2e1e43c3937a7d163bcc76f1882903a8ad31f1cc"
REQUIRED_BASE_REF = "origin/develop"
FOCUSED_WORKER_TEST = "org.lgna.issue.IssueSubmissionProgressWorkerTest"
FOCUSED_MAVEN_FRAGMENT = "mvn -pl core/issue-reporting -am -DfailIfNoTests=false"
REQUIRED_NODE_OPTIONS = "NODE_OPTIONS=--max-old-space-size=32768"
NOT_READY_PREFIX = "NOT_MERGE_READY"

ALLOWED_DIFF_FILES = {
    "core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java",
    "core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java",
    "docs/reference/issue-submission-progress-worker.md",
    "docs/howto/characterize-issue-submission-progress-worker.md",
    "docs/tutorials/trace-issue-submission-progress-worker.md",
    "docs/index.md",
    "scripts/pr428_merge_ready_gate.py",
    "tests/test_pr428_merge_ready_gate.py",
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
REQUIRED_PR_BODY_FRAGMENTS = (
    "Focused validation",
    "Docs impact",
    "Scenario evidence",
    "Diff scope",
    "Quality audit",
    "SEEK / VALIDATE / FIX",
    "clean final cycle",
    "GitHub Actions",
    "Does not claim",
)
REQUIRED_PR_BODY_FRAGMENT_CHECKS = tuple(
    (fragment, fragment.lower()) for fragment in REQUIRED_PR_BODY_FRAGMENTS
)
FOCUSED_COMMAND_FRAGMENTS = (
    REQUIRED_NODE_OPTIONS,
    FOCUSED_MAVEN_FRAGMENT,
    f"-Dtest={FOCUSED_WORKER_TEST}",
    " test",
)
REQUIRED_FOCUSED_VALIDATION_COMMAND = (
    f"{REQUIRED_NODE_OPTIONS} {FOCUSED_MAVEN_FRAGMENT} "
    f"-Dsurefire.failIfNoSpecifiedTests=false -Dtest={FOCUSED_WORKER_TEST} test"
)
PASSING_RESULT_RE = re.compile(
    r"\b(?:pass(?:ed|es)?|success(?:ful(?:ly)?)?|succeeded|exit(?:ed)?\s*0)\b",
    re.IGNORECASE,
)
CHECK_STATUS_RE = re.compile(
    r"\b(?:all\s+)?(?:current-head\s+)?checks?\b.{0,80}\b(?:completed|green|success(?:ful(?:ly)?)?|pass(?:ed|es)?)\b",
    re.IGNORECASE | re.DOTALL,
)
DIFF_SCOPE_RE = re.compile(
    r"\bdiff\s+scope\b.{0,160}\borigin/develop\.\.\.HEAD\b.{0,160}\b(?:allowed|only|scoped)\b",
    re.IGNORECASE | re.DOTALL,
)


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
    return GateResult(ready=False, blockers=[not_ready(message)])


def not_ready(message: str) -> str:
    return f"{NOT_READY_PREFIX}: {message}"


def result_from_blockers(blockers: Iterable[str]) -> GateResult:
    blocker_list = list(blockers)
    return GateResult(ready=not blocker_list, blockers=blocker_list)


def combine_results(results: Iterable[GateResult]) -> GateResult:
    blockers: list[str] = []
    for result in results:
        blockers.extend(result.blockers)
    return result_from_blockers(blockers)


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
    clean_local_head = _clean_sha(local_head)
    clean_remote_head = _clean_sha(remote_head)
    if current_ref != required_ref:
        blockers.append(
            not_ready(f"branch sync must use {required_ref}, got {current_ref or '<missing>'}")
        )
    if not clean_local_head or clean_local_head != clean_remote_head:
        blockers.append(not_ready("local head must match the current remote head before validation"))
    if manual_merge_seen:
        blockers.append(not_ready("manual merge/rebase/squash evidence is not allowed"))
    return result_from_blockers(blockers)


def validate_base_evidence(
    base: Mapping[str, object] | None,
    expected_base_sha: str = DEFAULT_EXPECTED_BASE_SHA,
    required_base_ref: str = REQUIRED_BASE_REF,
) -> GateResult:
    """Require recovery evidence from the current authoritative develop base."""

    if not isinstance(base, Mapping):
        return blocked_result("base evidence is missing")

    blockers: list[str] = []
    base_ref = _as_text(base.get("base_ref"))
    base_sha = _clean_sha(base.get("base_sha"))
    expected_sha = _clean_sha(expected_base_sha)
    if base_ref != required_base_ref:
        blockers.append(
            not_ready(f"base evidence must use {required_base_ref}, got {base_ref or '<missing>'}")
        )
    if not expected_sha:
        blockers.append(not_ready("expected base SHA evidence is missing"))
    elif base_sha != expected_sha:
        blockers.append(
            not_ready(
                "base evidence must match the current origin/develop head "
                f"{expected_sha}, got {base_sha or '<missing>'}"
            )
        )
    return result_from_blockers(blockers)


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
    return any(
        token.rsplit("/", 1)[-1] in TIMEOUT_PROGRAMS for token in _command_tokens(command)
    )


def _has_focused_worker_command(command: str) -> bool:
    command_text = " ".join(command.split())
    return all(fragment in command_text for fragment in FOCUSED_COMMAND_FRAGMENTS)


def _contains_command_with_passing_result(text: str, command: str) -> bool:
    normalized_text = " ".join(text.split())
    normalized_command = " ".join(command.split())
    command_index = normalized_text.find(normalized_command)
    if command_index == -1:
        return False
    result_start = command_index + len(normalized_command)
    result_window = normalized_text[result_start : result_start + 160]
    return bool(PASSING_RESULT_RE.search(result_window))


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
            blockers.append(not_ready(f"runnable evidence #{index} is missing a command"))
            continue
        if _uses_timeout_wrapper(command):
            blockers.append(
                not_ready(f"runnable evidence #{index} uses a timeout wrapper: {command}")
            )
        if not bool(entry.get("passed")):
            blockers.append(not_ready(f"runnable evidence #{index} did not pass"))
        head_sha = _clean_sha(entry.get("head_sha"))
        if expected_head_sha and head_sha != expected_head_sha:
            blockers.append(
                not_ready(
                    "runnable evidence must be from the current head "
                    f"{expected_head_sha}, got {head_sha or '<missing>'}"
                )
            )
        if _has_focused_worker_command(command):
            focused_seen = True
    if not focused_seen:
        blockers.append(
            not_ready(
                "focused QA must include IssueSubmissionProgressWorkerTest "
                "on core/issue-reporting with the saved NODE_OPTIONS setting"
            )
        )
    return result_from_blockers(blockers)


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
        reason_lower = reason.lower()
        identifies_worker_seam = (
            "issuesubmissionprogressworker" in reason_lower
            or ("issue" in reason_lower and "worker" in reason_lower)
        )
        identifies_non_ui_impact = (
            "non-ui" in reason_lower
            or "non ui" in reason_lower
            or "no alice desktop workflow impact" in reason_lower
            or "no desktop workflow impact" in reason_lower
        )
        if not identifies_worker_seam or not identifies_non_ui_impact:
            return blocked_result(
                "scenario evidence non-applicability must explain the non-UI issue-reporting worker seam"
            )
        return ready_result()
    if applicability in {"applicable", "covered"} and scenario_evidence.get("passed"):
        return ready_result()
    return blocked_result("scenario evidence must be passing or explicitly not applicable")


def validate_quality_audit_cycles(cycles: Iterable[Mapping[str, object]]) -> GateResult:
    """Require at least three SEEK / VALIDATE / FIX cycles and a clean final cycle."""

    cycle_count = 0
    final_cycle_clean = True
    cycle_blockers: list[str] = []
    for index, cycle in enumerate(cycles, start=1):
        cycle_count = index
        final_cycle_clean = bool(cycle.get("clean"))
        missing = [
            key
            for key in ("seek", "validate", "fix")
            if not _as_text(cycle.get(key))
        ]
        if missing:
            cycle_blockers.append(
                not_ready(
                    f"quality audit cycle {index} is missing {', '.join(missing)} evidence"
                )
            )
    blockers: list[str] = []
    if cycle_count < 3:
        blockers.append(not_ready("quality audit requires at least three cycles"))
    blockers.extend(cycle_blockers)
    if cycle_count and not final_cycle_clean:
        blockers.append(not_ready("quality audit requires a clean final cycle"))
    return result_from_blockers(blockers)


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

    normalized = {"name": name, "status": status, "conclusion": conclusion}
    head_sha = _clean_sha(check.get("head_sha"))
    if head_sha:
        normalized["head_sha"] = head_sha
    return normalized


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


def validate_github_actions(
    checks: Iterable[Mapping[str, object]],
    expected_head_sha: str = "",
) -> GateResult:
    """Require reported GitHub Actions checks to be completed, green, and current."""

    blockers: list[str] = []
    checks_seen = False
    clean_expected_head_sha = _clean_sha(expected_head_sha)
    for check in checks:
        checks_seen = True
        normalized_check = normalize_github_action_check(check)
        name = normalized_check["name"]
        status = normalized_check["status"]
        conclusion = normalized_check["conclusion"]
        head_sha = _clean_sha(normalized_check.get("head_sha"))
        if status != "COMPLETED":
            blockers.append(not_ready(f"GitHub Actions check {name} is not completed"))
        if conclusion not in SUCCESS_CONCLUSIONS:
            blockers.append(
                not_ready(
                    f"GitHub Actions check {name} is not green: "
                    f"{conclusion or '<missing conclusion>'}"
                )
            )
        if clean_expected_head_sha and head_sha != clean_expected_head_sha:
            blockers.append(
                not_ready(
                    "GitHub Actions check evidence must be tied to the current head "
                    f"{clean_expected_head_sha}, got {head_sha or '<missing>'} for {name}"
                )
            )
    if not checks_seen:
        return blocked_result("GitHub Actions checks are missing")
    return result_from_blockers(blockers)


def _missing_lower_fragments(text: str, fragments: Iterable[tuple[str, str]]) -> list[str]:
    lower_text = text.lower()
    return [fragment for fragment, lower_fragment in fragments if lower_fragment not in lower_text]


def validate_pr_description(
    body: str,
    expected_head_sha: str = DEFAULT_EXPECTED_HEAD_SHA,
    expected_base_sha: str = DEFAULT_EXPECTED_BASE_SHA,
) -> GateResult:
    """Require PR-body evidence for every merge-ready gate and bounded claims."""

    blockers: list[str] = []
    body_text = body or ""
    if expected_head_sha and expected_head_sha not in body_text:
        blockers.append(
            not_ready(f"PR description must include current head {expected_head_sha}")
        )
    if expected_base_sha and expected_base_sha not in body_text:
        blockers.append(
            not_ready(f"PR description must include current base {expected_base_sha}")
        )
    missing = _missing_lower_fragments(body_text, REQUIRED_PR_BODY_FRAGMENT_CHECKS)
    if missing:
        blockers.append(
            not_ready("PR description is missing evidence for " + ", ".join(missing))
        )
    if not _contains_command_with_passing_result(body_text, REQUIRED_FOCUSED_VALIDATION_COMMAND):
        blockers.append(
            not_ready(
                "PR description must include the exact focused validation command and passing result"
            )
        )
    if not DIFF_SCOPE_RE.search(body_text):
        blockers.append(
            not_ready("PR description must include checked origin/develop...HEAD diff scope evidence")
        )
    body_lower = body_text.lower()
    if (
        "not applicable" not in body_lower
        or (
            "non-ui issue-reporting worker seam" not in body_lower
            and "no alice desktop workflow impact" not in body_lower
        )
    ):
        blockers.append(
            not_ready(
                "PR description must include specific scenario non-applicability evidence"
            )
        )
    if not CHECK_STATUS_RE.search(body_text):
        blockers.append(
            not_ready("PR description must include concrete GitHub Actions check status evidence")
        )
    for label, pattern in OVERCLAIM_PATTERNS.items():
        if pattern.search(body_text):
            blockers.append(not_ready(f"PR description contains overclaim: {label}"))
    return result_from_blockers(blockers)


def classify_owner_free_exit(exit_code: int, owner: str | None, stderr: str) -> ExitClassification:
    """Classify owner-free exits so rate limits never become merge-ready no-ops."""

    stderr_text = stderr or ""
    if owner:
        return ExitClassification(kind="OWNER_PRESENT", ready=True, blocker="")
    if RATE_LIMIT_RE.search(stderr_text):
        return ExitClassification(
            kind="RATE_LIMIT",
            ready=False,
            blocker=not_ready("owner-free exit was caused by GitHub API rate limit"),
        )
    if exit_code == 0:
        return ExitClassification(
            kind="OWNER_FREE",
            ready=False,
            blocker=not_ready("owner-free exit lacks owner evidence"),
        )
    return ExitClassification(
        kind="FAILED",
        ready=False,
        blocker=not_ready(f"owner resolution failed with exit code {exit_code}"),
    )


def evaluate_merge_ready(evidence: Mapping[str, object]) -> GateResult:
    """Evaluate the full PR #428 merge-ready evidence package."""

    branch = evidence.get("branch")
    expected_head_sha = _clean_sha(evidence.get("expected_head_sha"))
    if not expected_head_sha and isinstance(branch, Mapping):
        expected_head_sha = _clean_sha(branch.get("remote_head"))
    expected_head_sha = expected_head_sha or DEFAULT_EXPECTED_HEAD_SHA
    base = evidence.get("base")
    expected_base_sha = _clean_sha(evidence.get("expected_base_sha"))
    if not expected_base_sha and isinstance(base, Mapping):
        expected_base_sha = _clean_sha(base.get("base_sha"))
    expected_base_sha = expected_base_sha or DEFAULT_EXPECTED_BASE_SHA
    docs_impact = evidence.get("docs_impact")
    scenario_evidence = evidence.get("scenario_evidence")
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
        validate_base_evidence(
            base if isinstance(base, Mapping) else None,
            expected_base_sha=expected_base_sha,
        ),
        audit_diff_scope(evidence.get("diff_files", [])),
        validate_runnable_evidence(
            evidence.get("runnable_evidence", []),
            expected_head_sha=expected_head_sha,
        ),
        validate_docs_impact(
            docs_impact if isinstance(docs_impact, Mapping) else None
        ),
        validate_scenario_evidence(
            scenario_evidence if isinstance(scenario_evidence, Mapping) else None
        ),
        validate_quality_audit_cycles(evidence.get("quality_audit_cycles", [])),
        validate_github_actions(
            evidence.get("github_checks", []),
            expected_head_sha=expected_head_sha,
        ),
        validate_pr_description(
            _as_text(evidence.get("pr_description")),
            expected_head_sha,
            expected_base_sha,
        ),
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
