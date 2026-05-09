#!/usr/bin/env python3
"""Evaluate PR #430 merge-readiness evidence without merging."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from collections.abc import Callable
from copy import deepcopy
from pathlib import Path
from typing import Any


EXPECTED_PR_NUMBER = 430
EXPECTED_BRANCH = "feat/issue-407-rabbithole-wave7-save-negative-contract-lane-follo"
GITHUB_PR_VIEW_FIELDS = "number,headRefName,headRefOid,body,statusCheckRollup,files"
SHA_RE = re.compile(r"^[0-9a-f]{40}$")

PYTHON_TEST_COMMAND = "NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests"
NEGATIVE_CONTRACT_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 "
    "bash qa/outside-in/alice-desktop/tests/"
    "test-save-menu-dialog-negative-artifact-contract.sh"
)
REQUIRED_QA_COMMANDS = (PYTHON_TEST_COMMAND, NEGATIVE_CONTRACT_COMMAND)

SUPPORTED_DIFF_PATHS = (
    "alice_qa_amplihack.py",
    "docs/howto/",
    "docs/reference/",
    "docs/tutorials/",
    "docs/index.md",
    "pyproject.toml",
    "qa/outside-in/alice-desktop/",
    "scripts/pr430_merge_ready_gate.py",
    "tests/",
)

UNBOUNDED_CLAIM_RE = re.compile(
    r"\b(full\s+UI\s+automation|visible\s+rendering\s+correctness|grading|"
    r"creative\s+assessment|full\s+lesson\s+completion|lesson\s+completion|"
    r"full\s+Tweedle|player\s+decode)\b",
    re.IGNORECASE,
)
NEGATED_CLAIM_RE = re.compile(r"\bdoes\s+not\s+prove\b|\bnon-claims?\b", re.IGNORECASE)

CommandRunner = Callable[[list[str]], subprocess.CompletedProcess[str]]


class ExternalServiceError(RuntimeError):
    """Raised when read-only GitHub evidence cannot be collected."""


class GitHubCliClient:
    """Read PR metadata/check evidence through the GitHub CLI."""

    def __init__(
        self,
        runner: CommandRunner | None = None,
        *,
        max_attempts: int = 3,
        retry_delay_seconds: float = 1.0,
    ) -> None:
        if max_attempts < 1:
            raise ValueError("max_attempts must be at least 1")
        if retry_delay_seconds < 0:
            raise ValueError("retry_delay_seconds must be non-negative")
        self._runner = runner or self._default_runner
        self._max_attempts = max_attempts
        self._retry_delay_seconds = retry_delay_seconds

    def fetch_pr_evidence(self, pr_number: int = EXPECTED_PR_NUMBER) -> dict[str, Any]:
        payload = self._run_json(
            ["gh", "pr", "view", str(pr_number), "--json", GITHUB_PR_VIEW_FIELDS]
        )
        if not isinstance(payload, dict):
            raise ExternalServiceError("GitHub PR response must be a JSON object.")
        return _normalize_github_pr_payload(payload)

    def _run_json(self, command: list[str]) -> Any:
        last_result: subprocess.CompletedProcess[str] | None = None
        for attempt in range(1, self._max_attempts + 1):
            result = self._runner(command)
            last_result = result
            if result.returncode == 0:
                try:
                    return json.loads(result.stdout)
                except json.JSONDecodeError as exc:
                    raise ExternalServiceError(
                        f"GitHub CLI returned invalid JSON for {' '.join(command)}: {exc.msg}"
                    ) from exc
            if attempt < self._max_attempts and _retryable_failure(result):
                if self._retry_delay_seconds:
                    time.sleep(self._retry_delay_seconds)
                continue
            break
        raise ExternalServiceError(
            "GitHub CLI failed while collecting PR evidence from "
            f"{' '.join(command)}: {_failure_detail(last_result)}"
        )

    @staticmethod
    def _default_runner(command: list[str]) -> subprocess.CompletedProcess[str]:
        return subprocess.run(command, check=False, capture_output=True, text=True)


def refresh_github_evidence(
    evidence: dict[str, Any],
    client: GitHubCliClient | None = None,
) -> dict[str, Any]:
    """Merge read-only GitHub PR metadata into an evidence document."""

    if not isinstance(evidence, dict):
        raise ValueError("Evidence must be a JSON object before GitHub refresh.")

    refreshed = deepcopy(evidence)
    for section, payload in (client or GitHubCliClient()).fetch_pr_evidence().items():
        current = _dict(refreshed.get(section)).copy()
        current.update(payload)
        refreshed[section] = current
    return refreshed


def evaluate_merge_readiness(evidence: dict[str, Any]) -> dict[str, Any]:
    """Return MERGE_READY only when all evidence gates pass."""

    if not isinstance(evidence, dict):
        return _result(["Evidence must be a JSON object."], [], None)

    blockers: list[str] = []
    _check_pr_head(evidence, blockers)
    _check_workflow(evidence, blockers)
    _check_diff_scope(evidence, blockers)
    _check_runnable_qa(evidence, blockers)
    _check_docs(evidence, blockers)
    _check_quality_audits(evidence, blockers)
    _check_github_actions(evidence, blockers)
    _check_pr_description(evidence, blockers)
    _check_bounded_claims(evidence, blockers)
    _check_no_op(evidence, blockers)

    diff = _dict(evidence.get("diff"))
    return _result(
        blockers,
        _string_list(diff.get("files_modified")),
        _optional_string(_dict(evidence.get("no_op")).get("justification")),
    )


def _check_pr_head(evidence: dict[str, Any], blockers: list[str]) -> None:
    pr = _dict(evidence.get("pr"))
    remote_head = _optional_string(pr.get("remote_head_sha"))
    evaluated_head = _optional_string(pr.get("evaluated_head_sha"))

    if pr.get("number") != EXPECTED_PR_NUMBER:
        blockers.append("PR evidence must target PR #430.")
    if pr.get("branch") != EXPECTED_BRANCH:
        blockers.append(
            f"PR evidence must use the authoritative remote PR branch {EXPECTED_BRANCH}."
        )
    if pr.get("manual_merge") is not False:
        blockers.append("Manual merge evidence is not allowed for this recovery.")
    if not remote_head or not SHA_RE.fullmatch(remote_head):
        blockers.append("Remote PR head SHA is missing or invalid.")
    if not evaluated_head or not SHA_RE.fullmatch(evaluated_head):
        blockers.append("Evaluated head SHA is missing or invalid.")
    if remote_head and evaluated_head and remote_head != evaluated_head:
        blockers.append(
            "Evaluated head SHA is stale: it must match the current remote PR head SHA."
        )


def _check_workflow(evidence: dict[str, Any], blockers: list[str]) -> None:
    workflow = _dict(evidence.get("workflow"))
    if workflow.get("owner_exit_classification") != "NO_OP_GUARD":
        blockers.append("Workflow owner exit classification must be NO_OP_GUARD.")
    if workflow.get("no_timeout_wrappers") is not True:
        blockers.append("Workflow evidence must confirm no timeout wrappers were used.")


def _check_diff_scope(evidence: dict[str, Any], blockers: list[str]) -> None:
    changed_files = _string_list(_dict(evidence.get("diff")).get("changed_files"))
    if not changed_files:
        blockers.append("Focused diff scope evidence is missing changed files.")
        return

    out_of_scope = [
        path
        for path in changed_files
        if not _is_supported_diff_path(path)
    ]
    if out_of_scope:
        blockers.append(
            "Focused diff scope failed; malformed or out-of-scope files: "
            + ", ".join(out_of_scope)
        )


def _is_supported_diff_path(path: str) -> bool:
    if not _is_safe_repo_relative_path(path):
        return False
    return any(
        path.startswith(allowed) if allowed.endswith("/") else path == allowed
        for allowed in SUPPORTED_DIFF_PATHS
    )


def _is_safe_repo_relative_path(path: str) -> bool:
    if not path or path.startswith("/") or "\\" in path or "\x00" in path:
        return False
    return all(part not in {"", ".", ".."} for part in path.split("/"))


def _check_runnable_qa(evidence: dict[str, Any], blockers: list[str]) -> None:
    qa = _dict(evidence.get("qa"))
    passed_commands = {
        _optional_string(item.get("command"))
        for item in _list_of_dicts(qa.get("runnable_evidence"))
        if _optional_string(item.get("status")) == "passed"
    }
    missing_commands = [command for command in REQUIRED_QA_COMMANDS if command not in passed_commands]
    if missing_commands:
        blockers.append(
            "Runnable QA/scenario evidence is missing passed commands: "
            + "; ".join(missing_commands)
        )

    scenario = _dict(qa.get("scenario_evidence"))
    if scenario.get("applicable") is True:
        if _optional_string(scenario.get("status")) != "passed":
            blockers.append(
                "Runnable QA scenario evidence for the Save menu dialog negative artifact "
                "contract must be passed."
            )
        claim = _optional_string(scenario.get("claim"))
        if not claim or not re.search(r"rejects|fail[s]? closed|invalid", claim, re.IGNORECASE):
            blockers.append(
                "Scenario evidence must bound the Save negative contract claim to invalid "
                "artifact rejection/fail-closed behavior."
            )
    elif scenario.get("applicable") is False:
        if not _optional_string(scenario.get("rationale")):
            blockers.append("Scenario evidence is marked non-applicable without a rationale.")
    else:
        blockers.append("Runnable QA scenario evidence applicability is missing.")


def _check_docs(evidence: dict[str, Any], blockers: list[str]) -> None:
    docs = _dict(evidence.get("docs"))
    if docs.get("impact_assessed") is not True:
        blockers.append("Documentation impact must be explicitly assessed.")
    if docs.get("bounded_language") is not True:
        blockers.append("Documentation impact evidence must confirm bounded language.")
    unsupported = _string_list(docs.get("unsupported_claims"))
    if unsupported:
        blockers.append(
            "Documentation contains unsupported claims or unbounded claims: "
            + "; ".join(unsupported)
        )


def _check_quality_audits(evidence: dict[str, Any], blockers: list[str]) -> None:
    cycles = _list_of_dicts(evidence.get("quality_audit_cycles"))
    cycle_blocker = (
        "Three quality-audit SEEK / VALIDATE / FIX cycles are required, "
        "with a clean final cycle."
    )
    if len(cycles) < 3:
        blockers.append(cycle_blocker)
        return

    for expected, cycle in enumerate(cycles[:3], start=1):
        if cycle.get("cycle") != expected:
            blockers.append(f"Quality-audit cycle {expected} is missing or misnumbered.")
        for field in ("seek", "validate", "fix"):
            if not _optional_string(cycle.get(field)):
                blockers.append(f"Quality-audit cycle {expected} lacks {field.upper()} evidence.")

    final_cycle = cycles[-1]
    final_fix = _optional_string(final_cycle.get("fix")) or ""
    if _optional_string(final_cycle.get("status")) != "clean" or re.search(
        r"\bpending\b|\bneeds?_fix\b", final_fix, re.IGNORECASE
    ):
        blockers.append(
            "Three quality-audit SEEK / VALIDATE / FIX cycles are required; "
            "the final cycle must be clean."
        )


def _check_github_actions(evidence: dict[str, Any], blockers: list[str]) -> None:
    actions = _dict(evidence.get("github_actions"))
    remote_head = _optional_string(_dict(evidence.get("pr")).get("remote_head_sha"))
    if _optional_string(actions.get("head_sha")) != remote_head:
        blockers.append("GitHub Actions evidence must be for the current PR head SHA.")

    checks = _list_of_dicts(actions.get("checks"))
    if not checks:
        blockers.append("GitHub Actions checks evidence is missing.")
        return

    not_green = []
    for check in checks:
        name = _optional_string(check.get("name")) or "<unnamed check>"
        status = _optional_string(check.get("status"))
        conclusion = _optional_string(check.get("conclusion"))
        if status != "completed" or conclusion not in {"success", "skipped", "neutral"}:
            not_green.append(
                f"{name} ({status or 'missing status'}/{conclusion or 'missing conclusion'})"
            )
    if not_green:
        blockers.append(
            "GitHub Actions checks must be complete and green: " + "; ".join(not_green)
        )


def _check_pr_description(evidence: dict[str, Any], blockers: list[str]) -> None:
    description = _dict(evidence.get("pr_description"))
    remote_head = _optional_string(_dict(evidence.get("pr")).get("remote_head_sha"))
    if _optional_string(description.get("head_sha")) != remote_head:
        blockers.append("PR description evidence must reference the current head SHA.")

    required = {
        "contains_current_evidence": "current runnable evidence",
        "mentions_docs_impact": "documentation impact",
        "mentions_three_audit_cycles": "three quality-audit cycles",
        "mentions_green_actions": "green GitHub Actions",
        "bounded_non_claims": "bounded non-claims",
    }
    missing = [label for field, label in required.items() if description.get(field) is not True]
    if missing:
        blockers.append(
            "PR description/pull request description is missing: " + ", ".join(missing)
        )


def _check_bounded_claims(evidence: dict[str, Any], blockers: list[str]) -> None:
    unsupported = [
        claim
        for claim in _string_list(evidence.get("claims"))
        if UNBOUNDED_CLAIM_RE.search(claim) and not NEGATED_CLAIM_RE.search(claim)
    ]
    if unsupported:
        blockers.append(
            "Unsupported claim or unbounded claim detected: " + "; ".join(unsupported)
        )


def _check_no_op(evidence: dict[str, Any], blockers: list[str]) -> None:
    if _string_list(_dict(evidence.get("diff")).get("files_modified")):
        return

    no_op = _dict(evidence.get("no_op"))
    justification = _optional_string(no_op.get("justification"))
    if no_op.get("accepted") is not True or not justification:
        blockers.append(
            "No-op recovery requires explicit workflow-accepted justification tied to "
            "current head, checks, diff scope, QA/docs evidence, audit cycles, and PR description."
        )
        return
    if not re.search(r"current remote PR head|remote PR head", justification, re.IGNORECASE):
        blockers.append("No-op justification must mention the current remote PR head.")
    if not re.search(r"all merge-ready gates|merge-ready gates", justification, re.IGNORECASE):
        blockers.append("No-op justification must mention the merge-ready gates.")


def _normalize_github_pr_payload(payload: dict[str, Any]) -> dict[str, Any]:
    head_sha = _optional_string(payload.get("headRefOid"))
    body = payload.get("body") if isinstance(payload.get("body"), str) else ""
    return {
        "pr": {
            "number": payload.get("number"),
            "branch": _optional_string(payload.get("headRefName")),
            "remote_head_sha": head_sha,
            "evaluated_head_sha": head_sha,
            "manual_merge": False,
        },
        "diff": {"changed_files": _github_changed_files(payload.get("files"))},
        "github_actions": {
            "head_sha": head_sha,
            "checks": _github_checks(payload.get("statusCheckRollup")),
        },
        "pr_description": _pr_description_evidence(body, head_sha),
    }


def _github_changed_files(value: Any) -> list[str]:
    return [
        path
        for path in (_optional_string(item.get("path")) for item in _list_of_dicts(value))
        if path
    ]


def _github_checks(value: Any) -> list[dict[str, str | None]]:
    checks = []
    for item in _list_of_dicts(value):
        status, conclusion = _normalize_github_check_state(item)
        checks.append(
            {
                "name": (
                    _optional_string(item.get("name"))
                    or _optional_string(item.get("context"))
                    or _optional_string(item.get("workflowName"))
                    or "<unnamed check>"
                ),
                "status": status,
                "conclusion": conclusion,
            }
        )
    return checks


def _normalize_github_check_state(item: dict[str, Any]) -> tuple[str | None, str | None]:
    status = _external_token(item.get("status"))
    conclusion = _external_token(item.get("conclusion"))
    state = _external_token(item.get("state"))

    if state and not conclusion:
        if state in {"success", "skipped", "neutral"}:
            return "completed", state
        if state in {"failure", "failed", "error", "cancelled", "timed_out"}:
            return "completed", "failure"
        if state in {"pending", "queued", "in_progress", "expected"}:
            return "in_progress", None

    if status in {"queued", "pending", "expected"}:
        status = "in_progress"
    if conclusion in {"cancelled", "timed_out"}:
        conclusion = "failure"
    return status, conclusion


def _pr_description_evidence(body: str, head_sha: str | None) -> dict[str, Any]:
    contains_current_evidence = bool(
        head_sha
        and head_sha in body
        and "python3 -m unittest discover -s tests" in body
        and "test-save-menu-dialog-negative-artifact-contract.sh" in body
    )
    mentions_docs_impact = bool(
        re.search(r"\bdocs?\s+impact\b|\bdocumentation\s+impact\b", body, re.IGNORECASE)
    )
    mentions_three_audit_cycles = bool(
        re.search(
            r"\b(three|3)\b.{0,60}\bquality[- ]audit\b|"
            r"\bquality[- ]audit\b.{0,60}\b(three|3)\b",
            body,
            re.IGNORECASE | re.DOTALL,
        )
    )
    mentions_green_actions = bool(
        re.search(
            r"\b(green|success(?:ful)?|passed|passing)\b.{0,60}\b(GitHub Actions|actions|checks|CI)\b|"
            r"\b(GitHub Actions|actions|checks|CI)\b.{0,60}\b(green|success(?:ful)?|passed|passing)\b",
            body,
            re.IGNORECASE | re.DOTALL,
        )
    )
    bounded_non_claims = bool(
        NEGATED_CLAIM_RE.search(body)
        and not re.search(
            r"(?<!does not )\bproves?\s+"
            r"(full\s+UI\s+automation|visible\s+rendering\s+correctness|grading|"
            r"creative\s+assessment|full\s+lesson\s+completion|lesson\s+completion|"
            r"full\s+Tweedle|player\s+decode)\b",
            body,
            re.IGNORECASE,
        )
    )
    return {
        "head_sha": head_sha,
        "contains_current_evidence": contains_current_evidence,
        "mentions_docs_impact": mentions_docs_impact,
        "mentions_three_audit_cycles": mentions_three_audit_cycles,
        "mentions_green_actions": mentions_green_actions,
        "bounded_non_claims": bounded_non_claims,
    }


def _retryable_failure(result: subprocess.CompletedProcess[str]) -> bool:
    detail = f"{result.stderr}\n{result.stdout}".lower()
    return any(
        marker in detail
        for marker in (
            "temporarily unavailable",
            "temporary failure",
            "temporary rate limit",
            "connection reset",
            "connection refused",
            "network",
            "rate limit",
            "502",
            "503",
            "504",
        )
    )


def _failure_detail(result: subprocess.CompletedProcess[str] | None) -> str:
    if result is None:
        return "command was not run"
    detail = result.stderr.strip() or result.stdout.strip()
    return f"exit {result.returncode}: {detail}" if detail else f"exit {result.returncode}"


def _result(
    blockers: list[str],
    files_modified: list[str],
    no_op_justification: str | None,
) -> dict[str, Any]:
    ready = not blockers
    payload: dict[str, Any] = {
        "status": "MERGE_READY" if ready else "NOT_MERGE_READY",
        "ready": ready,
        "blockers": blockers,
        "files_modified": files_modified,
    }
    if ready and not files_modified and no_op_justification:
        payload["no_op_justification"] = no_op_justification
    return payload


def _dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _list_of_dicts(value: Any) -> list[dict[str, Any]]:
    return [item for item in value if isinstance(item, dict)] if isinstance(value, list) else []


def _string_list(value: Any) -> list[str]:
    return [item for item in value if isinstance(item, str)] if isinstance(value, list) else []


def _optional_string(value: Any) -> str | None:
    return value.strip() if isinstance(value, str) and value.strip() else None


def _external_token(value: Any) -> str | None:
    text = _optional_string(value)
    return text.lower().replace("-", "_") if text else None


def _load_evidence(path: Path) -> dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as evidence_file:
            evidence = json.load(evidence_file)
    except json.JSONDecodeError as exc:
        raise ValueError(f"invalid evidence JSON: malformed evidence at {exc}") from exc
    except OSError as exc:
        raise ValueError(f"invalid evidence JSON: could not read {path}: {exc}") from exc

    if not isinstance(evidence, dict):
        raise ValueError("invalid evidence JSON: evidence root must be an object")
    return evidence


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Evaluate PR #430 merge-ready evidence without merging."
    )
    parser.add_argument("--evidence", required=True, type=Path)
    parser.add_argument("--refresh-github", action="store_true")
    parser.add_argument("--github-retries", type=int, default=3)
    args = parser.parse_args(argv)

    try:
        evidence = _load_evidence(args.evidence)
        if args.refresh_github:
            evidence = refresh_github_evidence(
                evidence,
                GitHubCliClient(max_attempts=args.github_retries),
            )
    except (ExternalServiceError, ValueError) as exc:
        print(str(exc), file=sys.stderr)
        return 2

    result = evaluate_merge_readiness(evidence)
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if result["ready"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
