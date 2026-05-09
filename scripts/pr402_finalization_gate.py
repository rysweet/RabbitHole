#!/usr/bin/env python3
"""Evaluate workflow-owned PR #402 finalization evidence."""

from __future__ import annotations

import argparse
import json
import logging
import re
import sys
from pathlib import Path
from typing import Any, Iterable


EXPECTED_PR_NUMBER = 402
EXPECTED_NODE_OPTIONS = "--max-old-space-size=32768"
EXPECTED_TEST_SELECTOR = (
    "org.lgna.project.io.IoUtilitiesTest#"
    "savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported"
)
EXPECTED_VALIDATION_COMMAND = (
    "NODE_OPTIONS=--max-old-space-size=32768 "
    "mvn -pl core/story-api-migration -am -DfailIfNoTests=false "
    "-Dsurefire.failIfNoSpecifiedTests=false "
    f"-Dtest={EXPECTED_TEST_SELECTOR} test"
)
GREEN_CHECK_CONCLUSIONS = frozenset(("success",))
COMPLETED_CHECK_STATUSES = frozenset(("completed",))
QA_OVERCLAIM_PATTERN = re.compile(
    r"\b(desktop qa|manual scenario|scenario validation|qa scenario)\b",
    re.IGNORECASE,
)


LOGGER = logging.getLogger("pr402_finalization_gate")


def _is_merge_ready_evidence_heading(line: str) -> bool:
    return line.strip().lower() in {
        "## merge-ready evidence",
        "## current-head merge-ready gate evidence",
    }


def tweedle_submodule_command() -> list[str]:
    """Return the exact submodule initialization command required before Maven."""
    return ["git", "submodule", "update", "--init", "tweedle-lang"]


def focused_validation_command() -> str:
    """Return the focused archive reopen/edit validation command."""
    return EXPECTED_VALIDATION_COMMAND


def _text(value: Any) -> str:
    return "" if value is None else str(value)


def _normalized_text(value: Any) -> str:
    return _text(value).strip().lower()


def _as_mapping(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _dedupe(blockers: Iterable[str]) -> list[str]:
    return list(dict.fromkeys(blockers))


def _contains_head(text: str, head_sha: str) -> bool:
    return bool(head_sha) and head_sha in text


def _extract_merge_ready_section(pr_body: str) -> str:
    lines = pr_body.splitlines()
    start_index: int | None = None
    start_level = 0
    heading_pattern = re.compile(r"^(#{1,6})\s+\S")

    for index, line in enumerate(lines):
        if _is_merge_ready_evidence_heading(line):
            start_index = index
            start_level = 2
            break

    if start_index is None:
        return ""

    end_index = len(lines)
    for index in range(start_index + 1, len(lines)):
        match = heading_pattern.match(lines[index])
        if match and len(match.group(1)) <= start_level:
            end_index = index
            break

    return "\n".join(lines[start_index:end_index])


def _line_mentions_checks_for_head(line: str, head_sha: str) -> bool:
    normalized = line.lower()
    return (
        "github checks" in normalized
        and "green" in normalized
        and _contains_head(line, head_sha)
    )


def _line_is_untied_qa_claim(line: str, head_sha: str) -> bool:
    if not QA_OVERCLAIM_PATTERN.search(line):
        return False
    lowered = line.lower()
    if "passed" not in lowered and "validated" not in lowered and "green" not in lowered:
        return False
    return not _contains_head(line, head_sha)


def evaluate_checks(
    observed_head_sha: str,
    checks_head_sha: str,
    status_check_rollup: list[dict[str, Any]],
) -> list[str]:
    """Require completed successful GitHub checks for the same observed head SHA."""
    blockers: list[str] = []
    if not observed_head_sha or not checks_head_sha or observed_head_sha != checks_head_sha:
        blockers.append("github-checks-stale-head")

    if not status_check_rollup:
        blockers.append("github-checks-unavailable")
        return _dedupe(blockers)

    for raw_check in status_check_rollup:
        check = _as_mapping(raw_check)
        status = _normalized_text(check.get("status"))
        conclusion = _normalized_text(check.get("conclusion"))
        if status not in COMPLETED_CHECK_STATUSES:
            blockers.append("github-checks-not-complete")
        elif conclusion not in GREEN_CHECK_CONCLUSIONS:
            blockers.append("github-checks-not-green")

    return _dedupe(blockers)


def evaluate_pr_body(
    pr_body: str,
    observed_head_sha: str,
    validation_command: str,
) -> list[str]:
    """Require current-head PR body evidence without untied QA overclaims."""
    section = _extract_merge_ready_section(_text(pr_body))
    evidence_text = section or _text(pr_body)
    blockers: list[str] = []

    if not _contains_head(evidence_text, observed_head_sha):
        blockers.append("pr-body-stale-head")
    if not any(
        _line_mentions_checks_for_head(line, observed_head_sha)
        for line in evidence_text.splitlines()
    ):
        blockers.append("pr-body-checks-not-tied-to-head")
    if validation_command not in evidence_text:
        blockers.append("pr-body-missing-focused-validation")
    if not re.search(
        rf"focused validation result:\s*passed\b.*{re.escape(observed_head_sha)}",
        evidence_text,
        re.IGNORECASE,
    ):
        blockers.append("pr-body-missing-focused-validation-result")
    if any(_line_is_untied_qa_claim(line, observed_head_sha) for line in evidence_text.splitlines()):
        blockers.append("pr-body-qa-evidence-not-current")

    return _dedupe(blockers)


def evaluate_validation(validation: dict[str, Any]) -> list[str]:
    """Require Tweedle init and the exact focused Maven validation success."""
    evidence = _as_mapping(validation)
    blockers: list[str] = []

    if evidence.get("tweedleLangInitialized") is not True:
        blockers.append("tweedle-lang-not-initialized")
    if evidence.get("command") != focused_validation_command():
        blockers.append("focused-validation-command-mismatch")
    if evidence.get("testSelector") != EXPECTED_TEST_SELECTOR:
        blockers.append("focused-validation-command-mismatch")
    if evidence.get("exitCode") != 0:
        blockers.append("focused-validation-failed")

    return _dedupe(blockers)


def render_merge_ready_evidence(head_sha: str) -> str:
    """Render the authoritative current-head merge-ready evidence block."""
    return "\n".join(
        [
            "## Merge-ready evidence",
            "",
            f"- Current PR head: `{head_sha}`",
            f"- GitHub checks: green for `{head_sha}`",
            "- Focused validation:",
            f"  `{focused_validation_command()}`",
            f"- Focused validation result: passed for `{head_sha}`",
            "- Scope: evidence-only finalization; no source, behavior, broad documentation, unrelated QA, or upstream changes.",
        ]
    )


def update_merge_ready_evidence(pr_body: str, head_sha: str) -> str:
    """Replace only the merge-ready evidence section, or append it if absent."""
    replacement = render_merge_ready_evidence(head_sha)
    body = _text(pr_body)
    lines = body.splitlines()
    start_index: int | None = None
    heading_pattern = re.compile(r"^(#{1,6})\s+\S")

    for index, line in enumerate(lines):
        if _is_merge_ready_evidence_heading(line):
            start_index = index
            break

    if start_index is None:
        if body.strip():
            return body.rstrip() + "\n\n" + replacement
        return replacement

    end_index = len(lines)
    for index in range(start_index + 1, len(lines)):
        match = heading_pattern.match(lines[index])
        if match and len(match.group(1)) <= 2:
            end_index = index
            break

    return "\n".join(lines[:start_index] + replacement.splitlines() + lines[end_index:])


def _no_op_justification(head_sha: str) -> str:
    return (
        f"No-op justification: PR #402 body already references current head {head_sha}, "
        f"GitHub green checks are tied to {head_sha}, focused validation "
        f"{EXPECTED_TEST_SELECTOR} passed for that head, and the scope remains evidence-only."
    )


def decide_finalization(snapshot: dict[str, Any]) -> dict[str, Any]:
    """Decide whether PR #402 finalization is blocked, a no-op, or a body update."""
    evidence = _as_mapping(snapshot)
    observed_head_sha = _text(evidence.get("observedHeadSha"))
    reread_head_sha = _text(evidence.get("rereadHeadSha"))
    checks_head_sha = _text(evidence.get("checksHeadSha"))
    validation = _as_mapping(evidence.get("validation"))
    pr_body = _text(evidence.get("prBody"))

    if evidence.get("prNumber") != EXPECTED_PR_NUMBER:
        LOGGER.info("PR #402 finalization blocked by wrong-pr-number")
        return {
            "status": "NOT_MERGE_READY",
            "updatePrBody": False,
            "blockers": ["wrong-pr-number"],
            "summary": "Finalization is blocked: wrong-pr-number",
        }

    if observed_head_sha and reread_head_sha and observed_head_sha != reread_head_sha:
        LOGGER.info("PR #402 finalization blocked because the PR head changed")
        return {
            "status": "NOT_MERGE_READY",
            "updatePrBody": False,
            "blockers": ["pr-head-changed-during-finalization"],
            "summary": "Finalization evidence is stale because PR head changed during finalization.",
        }

    hard_blockers = _dedupe(
        evaluate_checks(observed_head_sha, checks_head_sha, _as_list(evidence.get("statusCheckRollup")))
        + evaluate_validation(validation)
    )
    if hard_blockers:
        LOGGER.info("PR #402 finalization blocked by %d criterion/criteria", len(hard_blockers))
        return {
            "status": "NOT_MERGE_READY",
            "updatePrBody": False,
            "blockers": hard_blockers,
            "summary": "Finalization is blocked: " + ", ".join(hard_blockers),
        }

    body_blockers = evaluate_pr_body(
        pr_body,
        observed_head_sha,
        _text(validation.get("command")) or focused_validation_command(),
    )
    if body_blockers:
        LOGGER.info("PR #402 finalization requires a PR body evidence refresh")
        return {
            "status": "UPDATE_PR_BODY",
            "updatePrBody": True,
            "blockers": [],
            "bodyEvidenceBlockers": body_blockers,
            "sourceFilesToChange": [],
            "prBodyPatch": render_merge_ready_evidence(observed_head_sha),
            "updatedPrBody": update_merge_ready_evidence(pr_body, observed_head_sha),
            "summary": "Refresh only the merge-ready evidence section for the current PR head.",
        }

    LOGGER.info("PR #402 finalization is already current for head %s", observed_head_sha)
    return {
        "status": "NO_OP_GUARD",
        "updatePrBody": False,
        "blockers": [],
        "noOpJustification": _no_op_justification(observed_head_sha),
        "summary": "NO_OP_GUARD: current-head merge-ready evidence is already present.",
    }


def load_snapshot(path: Path | None) -> dict[str, Any]:
    """Load finalization evidence JSON from a file, or stdin when no file is provided."""
    if path is None:
        data = json.load(sys.stdin)
    else:
        with path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError("finalization evidence JSON must be an object")
    return data


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Evaluate PR #402 workflow-owned finalization evidence and print JSON."
    )
    parser.add_argument(
        "snapshot",
        nargs="?",
        type=Path,
        help="Path to finalization evidence JSON. Reads stdin when omitted.",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Pretty-print the finalization result.",
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
    result = decide_finalization(load_snapshot(args.snapshot))
    indent = 2 if args.pretty else None
    print(json.dumps(result, indent=indent, sort_keys=True))
    return 0 if result["status"] in {"NO_OP_GUARD", "UPDATE_PR_BODY"} else 1


if __name__ == "__main__":
    raise SystemExit(main())
