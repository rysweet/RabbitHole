#!/usr/bin/env python3
"""Evaluate the PR #401 merge-ready evidence gate.

The gate is intentionally stricter than GitHub's mergeability state.  It only
returns ready when the local branch, PR metadata, check rollup, focused
validation evidence, docs review, audit cycles, diff scope, and PR body all
describe the same current PR head.
"""

from __future__ import annotations

import argparse
import json
import logging
import re
import subprocess
import sys
from pathlib import Path
from typing import Any, Iterable, Mapping, Sequence


LOGGER = logging.getLogger("pr401_merge_ready_gate")

PR_NUMBER = 401
BRANCH = "wave6-ui-action-menu-contract-1778302300"
BASE_BRANCH = "develop"
NODE_OPTIONS = "--max-old-space-size=32768"
JAVA_CONTRACT = "org.alice.ide.croquet.models.AliceMenuBarContractTest"

REQUIRED_CHECKS = (
    "build",
    "coverage",
    "package-netbeans",
    "test",
    "GitGuardian Security Checks",
)
REQUIRED_CHECKS_SET = frozenset(REQUIRED_CHECKS)

REQUIRED_QA_COMMANDS = (
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh",
)

REQUIRED_PR_BODY_FRAGMENTS = (
    "GitHub Actions",
    "QA/scenario evidence",
    "Docs impact",
    "Quality audit",
    "SEEK -> VALIDATE -> FIX",
    "Diff scope",
    "Accepted claim",
    "Non-claims",
    "NOT_MERGE_READY:",
)

ALLOWED_DIFF_FILES = frozenset(
    {
        "core/ide/src/test/java/org/alice/ide/croquet/models/AliceMenuBarContractTest.java",
        "docs/howto/alice-desktop-outside-in-qa.md",
        "docs/howto/characterize-headless-safe-desktop-actions.md",
        "qa/outside-in/alice-desktop/scenarios/menu-action-smoke.yaml",
        "qa/outside-in/alice-desktop/schema/scenario.schema.json",
        "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
        "qa/outside-in/alice-desktop/runners/run-scenario.sh",
        "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
        "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
        "qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh",
        "docs/reference/alice-desktop-outside-in-qa.md",
        "docs/reference/desktop-procedure-edit-and-save-automation.md",
        "docs/reference/headless-safe-desktop-action-characterization.md",
        "docs/reference/pr401-ui-action-menu-contract-evidence.md",
        "docs/reference/window-menu-action-contract.md",
        "docs/tutorials/desktop-action-journey-characterization.md",
        "docs/index.md",
        "qa/outside-in/alice-desktop/README.md",
        "tests/test_pr401_ui_action_menu_contract_evidence.py",
        "tests/test_pr401_merge_ready_gate.py",
        "scripts/pr401-merge-ready-gate.py",
        "pyproject.toml",
    }
)

BLOCKER_LOCAL_HEAD = "NOT_MERGE_READY: local HEAD does not match PR headRefOid"
BLOCKER_CHECKS = "NOT_MERGE_READY: GitHub Actions are not green for the current PR head"
BLOCKER_QA = "NOT_MERGE_READY: runnable QA/scenario evidence is incomplete"
BLOCKER_DOCS = "NOT_MERGE_READY: docs impact review is incomplete"
BLOCKER_AUDIT_COUNT = "NOT_MERGE_READY: fewer than three quality-audit cycles are documented"
BLOCKER_DIFF = "NOT_MERGE_READY: diff scope includes unrelated files"
BLOCKER_DRAFT = "NOT_MERGE_READY: PR is still marked draft"
BLOCKER_MERGE_STATE = "NOT_MERGE_READY: PR merge state is not clean"
BLOCKER_WORKTREE = "NOT_MERGE_READY: local working tree has uncommitted changes"
BLOCKER_BODY_MISSING_HEAD = "NOT_MERGE_READY: PR body lacks current-head evidence"
BLOCKER_BODY_STALE_HEAD = "NOT_MERGE_READY: PR body contains stale head evidence"
BLOCKER_BODY_OVERCLAIM = "NOT_MERGE_READY: PR body overclaims UI behavior"
BLOCKER_BODY_UNRESOLVED = "NOT_MERGE_READY: PR body records unresolved blockers"
BLOCKER_CONTEXT_LOAD = "NOT_MERGE_READY: unable to collect merge-ready context"

HEX_SHA_RE = re.compile(r"\b[0-9a-f]{40}\b")
OVERCLAIM_RE = re.compile(
    r"\b(?:proves?|passed|passes|complete|completed|establish(?:es|ed)?|correct)\b"
    r"(?:(?!\bno\b|\bnon-claims?\b).){0,120}?"
    r"\b(?:full UI automation|visible rendering correctness|grading|creative assessment|"
    r"full lesson completion|full Save completion|full Tweedle/player decode)\b",
    re.IGNORECASE | re.DOTALL,
)
NON_CLAIM_LINE_RE = re.compile(r"\b(?:no|non-claims?|not claim|without claiming)\b", re.IGNORECASE)


def validation_plan() -> list[dict[str, Any]]:
    """Return the focused no-timeout validation plan for PR #401."""
    plan: list[dict[str, Any]] = [
        {
            "name": "focused-java-menu-contract",
            "env": {"NODE_OPTIONS": NODE_OPTIONS},
            "command": [
                "mvn",
                "-DincludeSims=false",
                "-Dinstall4j.skip",
                "-DfailIfNoTests=false",
                "-Dsurefire.failIfNoSpecifiedTests=false",
                "-pl",
                "core/ide",
                "-am",
                f"-Dtest={JAVA_CONTRACT}",
                "test",
            ],
        }
    ]
    plan.extend(
        {
            "name": command.rsplit("/", 1)[-1],
            "env": {"NODE_OPTIONS": NODE_OPTIONS},
            "command": [command],
        }
        for command in REQUIRED_QA_COMMANDS
    )
    return plan


def expected_validation_commands() -> tuple[tuple[str, ...], frozenset[tuple[str, ...]]]:
    """Return the exact command arrays accepted as validation evidence."""
    plan = validation_plan()
    focused_command = tuple(str(part) for part in _list(plan[0].get("command")))
    qa_commands = frozenset(
        tuple(str(part) for part in _list(item.get("command"))) for item in plan[1:]
    )
    return focused_command, qa_commands


def contains_timeout_wrapper(command: Sequence[str]) -> bool:
    """Return true when a command is wrapped by timeout/gtimeout."""
    if not command:
        return False
    executable = Path(command[0]).name
    return executable in {"timeout", "gtimeout"}


def validate_pr_body(body: str, expected_head: str) -> list[str]:
    """Validate the PR description evidence contract for the expected head."""
    blockers: list[str] = []
    if expected_head not in body or f"Current PR head: {expected_head}" not in body:
        blockers.append(BLOCKER_BODY_MISSING_HEAD)

    stale_heads = {sha for sha in HEX_SHA_RE.findall(body) if sha != expected_head}
    if stale_heads:
        blockers.append(BLOCKER_BODY_STALE_HEAD)

    if _contains_overclaim(body):
        blockers.append(BLOCKER_BODY_OVERCLAIM)

    missing_fragments = [fragment for fragment in REQUIRED_PR_BODY_FRAGMENTS if fragment not in body]
    if missing_fragments:
        blockers.append(
            "NOT_MERGE_READY: PR body lacks required evidence sections: "
            + ", ".join(missing_fragments)
        )

    if body.count("SEEK -> VALIDATE -> FIX") < 3:
        blockers.append(BLOCKER_AUDIT_COUNT)

    if "NOT_MERGE_READY: none" not in body:
        blockers.append(BLOCKER_BODY_UNRESOLVED)

    return blockers


def evaluate_merge_readiness(context: Mapping[str, Any]) -> dict[str, Any]:
    """Evaluate all PR #401 merge-ready gates against a supplied context."""
    blockers: list[str] = []
    pr = _mapping(context.get("pr"))
    pr_head = _string(pr.get("headRefOid"))
    local_head = _string(context.get("local_head"))

    if context.get("manual_merge_performed"):
        blockers.append("NOT_MERGE_READY: manual merge behavior was detected")

    if context.get("pr_number") != PR_NUMBER:
        blockers.append("NOT_MERGE_READY: context is not for PR #401")

    if pr.get("headRefName") != BRANCH:
        blockers.append("NOT_MERGE_READY: PR head branch does not match recovery branch")

    if pr.get("baseRefName") != BASE_BRANCH:
        blockers.append("NOT_MERGE_READY: PR base branch does not match develop")

    if pr.get("isDraft") is True:
        blockers.append(BLOCKER_DRAFT)

    merge_state = _string(pr.get("mergeStateStatus"))
    if merge_state and merge_state != "CLEAN":
        blockers.append(BLOCKER_MERGE_STATE)

    if not pr_head or local_head != pr_head:
        blockers.append(BLOCKER_LOCAL_HEAD)

    if context.get("working_tree_clean") is False:
        blockers.append(BLOCKER_WORKTREE)

    if not _checks_green_for_head(pr.get("statusCheckRollup"), pr_head):
        blockers.append(BLOCKER_CHECKS)

    if not _validation_complete(_mapping(context.get("validation")), pr_head):
        blockers.append(BLOCKER_QA)

    if not _docs_review_complete(_mapping(context.get("docs_impact"))):
        blockers.append(BLOCKER_DOCS)

    audit_blockers = _audit_cycle_blockers(_list(context.get("audit_cycles")))
    blockers.extend(audit_blockers)

    if not _diff_scope_focused(_list(context.get("diff_files"))):
        blockers.append(BLOCKER_DIFF)

    blockers.extend(validate_pr_body(_string(context.get("pr_body")), expected_head=pr_head))

    blockers = _deduplicate(blockers)
    accepted_claim = (
        "Window menu model registration, stable identity, and menu-bar membership lookup only"
    )
    return {
        "ready": not blockers,
        "blockers": blockers,
        "accepted_claim": accepted_claim,
    }


def build_runtime_context(
    *,
    pr_number: int,
    base_ref: str,
    validation_evidence: Mapping[str, Any] | None = None,
) -> dict[str, Any]:
    """Collect metadata only; complete readiness requires a context JSON with evidence."""
    pr = _run_json(
        [
            "gh",
            "pr",
            "view",
            str(pr_number),
            "--json",
            "headRefName,headRefOid,baseRefName,mergeStateStatus,isDraft,statusCheckRollup,body",
        ]
    )
    local_head = _run_text(["git", "rev-parse", "HEAD"])
    working_tree_status = _run_text(["git", "status", "--porcelain"])
    diff_files = _run_text(["git", "diff", "--name-only", f"origin/{base_ref}...HEAD"]).splitlines()

    return {
        "context_source": "live-metadata-only",
        "pr_number": pr_number,
        "local_head": local_head,
        "pr": pr,
        "working_tree_clean": not working_tree_status,
        "validation": validation_evidence or {},
        "audit_cycles": [],
        "docs_impact": {},
        "diff_files": diff_files,
        "pr_body": _string(pr.get("body")),
        "manual_merge_performed": False,
    }


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pr", type=int, default=PR_NUMBER, help="Pull request number to evaluate")
    parser.add_argument(
        "--base-ref",
        default=BASE_BRANCH,
        help="Remote base branch used for focused diff scope checks",
    )
    parser.add_argument(
        "--context-json",
        type=Path,
        help="Evaluate a complete evidence context JSON file instead of live metadata-only mode",
    )
    parser.add_argument(
        "--validation-plan",
        action="store_true",
        help="Print the focused no-timeout validation plan and exit",
    )
    parser.add_argument("--json", action="store_true", help="Print machine-readable evaluation")
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

    if args.validation_plan:
        print(json.dumps(validation_plan(), indent=2))
        return 0

    try:
        if args.context_json:
            LOGGER.info("Reading merge-ready context from %s", args.context_json)
            context = _load_context_json(args.context_json)
        else:
            LOGGER.info(
                "Collecting live PR #%s metadata only; validation evidence requires --context-json",
                args.pr,
            )
            context = build_runtime_context(pr_number=args.pr, base_ref=args.base_ref)
    except (subprocess.CalledProcessError, json.JSONDecodeError, OSError, ValueError) as exc:
        result = _context_load_failure_result(exc)
        if args.json:
            print(json.dumps(result, indent=2))
        else:
            LOGGER.error("%s", result["blockers"][0])
            _print_human_result(result)
        return 1

    result = evaluate_merge_readiness(context)
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        _print_human_result(result)
    return 0 if result["ready"] else 1


def _contains_overclaim(body: str) -> bool:
    claim_text = "\n".join(line for line in body.splitlines() if not NON_CLAIM_LINE_RE.search(line))
    return OVERCLAIM_RE.search(claim_text) is not None


def _checks_green_for_head(checks: Any, head: str) -> bool:
    if not head:
        return False

    seen: set[str] = set()
    for check in _list(checks):
        check_map = _mapping(check)
        name = _string(check_map.get("name") or check_map.get("context") or check_map.get("workflowName"))
        if name not in REQUIRED_CHECKS_SET:
            continue

        check_head = _string(
            check_map.get("headSha")
            or check_map.get("head_sha")
            or _mapping(check_map.get("details")).get("headSha")
        )
        if check_head != head:
            return False

        status = _string(check_map.get("status") or check_map.get("state")).upper()
        conclusion = _string(check_map.get("conclusion")).upper()
        state = _string(check_map.get("state")).upper()
        if status not in {"", "COMPLETED", "SUCCESS"}:
            return False
        if conclusion not in {"", "SUCCESS", "NEUTRAL", "SKIPPED"}:
            return False
        if state not in {"", "SUCCESS"}:
            return False
        seen.add(name)

    return REQUIRED_CHECKS_SET.issubset(seen)


def _validation_complete(validation: Mapping[str, Any], head: str) -> bool:
    if validation.get("node_options") != NODE_OPTIONS:
        return False

    expected_focused_command, expected_qa_commands = expected_validation_commands()
    focused_maven = _mapping(validation.get("focused_maven"))
    focused_command = tuple(str(item) for item in _list(focused_maven.get("command")))
    if (
        not focused_maven.get("passed")
        or focused_maven.get("head") != head
        or contains_timeout_wrapper(focused_command)
        or focused_command != expected_focused_command
    ):
        return False

    observed_commands: set[tuple[str, ...]] = set()
    for item in _list(validation.get("qa_commands")):
        evidence = _mapping(item)
        command = tuple(str(part) for part in _list(evidence.get("command")))
        if not evidence.get("passed") or evidence.get("head") != head:
            return False
        if contains_timeout_wrapper(command):
            return False
        observed_commands.add(command)

    if not expected_qa_commands.issubset(observed_commands):
        return False

    gated_smoke = _mapping(validation.get("gated_desktop_smoke"))
    if gated_smoke.get("required") and gated_smoke.get("passed") is not True:
        return False

    return True


def _docs_review_complete(docs_impact: Mapping[str, Any]) -> bool:
    return all(
        docs_impact.get(key) is True
        for key in ("reviewed", "bounded_claims", "no_committed_exact_head_sha")
    )


def _audit_cycle_blockers(audit_cycles: Sequence[Any]) -> list[str]:
    cycles = [_mapping(cycle) for cycle in audit_cycles]
    if len(cycles) < 3:
        return [BLOCKER_AUDIT_COUNT]

    blockers: list[str] = []
    for index, cycle_map in enumerate(cycles, start=1):
        for field in ("seek", "validate", "fix"):
            if not _string(cycle_map.get(field)):
                blockers.append(f"NOT_MERGE_READY: quality-audit cycle {index} lacks {field}")
        if cycle_map.get("clean") is not True:
            blockers.append(f"NOT_MERGE_READY: quality-audit cycle {index} is not clean")

    if cycles[-1].get("clean") is not True:
        blockers.append("NOT_MERGE_READY: final quality-audit cycle is not clean")

    return blockers


def _diff_scope_focused(diff_files: Sequence[Any]) -> bool:
    for path in diff_files:
        path_text = str(path)
        if path_text and path_text not in ALLOWED_DIFF_FILES:
            return False
    return True


def _deduplicate(items: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for item in items:
        if item and item not in seen:
            result.append(item)
            seen.add(item)
    return result


def _mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _string(value: Any) -> str:
    return value if isinstance(value, str) else ""


def _load_context_json(path: Path) -> dict[str, Any]:
    parsed = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(parsed, dict):
        raise ValueError(f"Expected JSON object in {path}")
    return parsed


def _context_load_failure_result(exc: Exception) -> dict[str, Any]:
    return {
        "ready": False,
        "blockers": [f"{BLOCKER_CONTEXT_LOAD}: {_context_load_error_message(exc)}"],
        "accepted_claim": "",
    }


def _context_load_error_message(exc: Exception) -> str:
    if isinstance(exc, subprocess.CalledProcessError):
        stderr = _string(exc.stderr).strip()
        stdout = _string(exc.output).strip()
        details = stderr or stdout
        suffix = f": {details}" if details else ""
        return f"command failed ({exc.returncode}): {_format_command(exc.cmd)}{suffix}"
    if isinstance(exc, json.JSONDecodeError):
        return f"invalid JSON at line {exc.lineno} column {exc.colno}: {exc.msg}"
    return str(exc) or exc.__class__.__name__


def _format_command(command: Any) -> str:
    if isinstance(command, (list, tuple)):
        return " ".join(str(part) for part in command)
    return str(command)


def _run_text(command: Sequence[str]) -> str:
    LOGGER.debug("Running command: %s", " ".join(command))
    completed = subprocess.run(command, check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return completed.stdout.strip()


def _run_json(command: Sequence[str]) -> dict[str, Any]:
    output = _run_text(command)
    parsed = json.loads(output)
    if not isinstance(parsed, dict):
        raise ValueError(f"Expected JSON object from {' '.join(command)}")
    return parsed


def _print_human_result(result: Mapping[str, Any]) -> None:
    print(f"ready: {str(result.get('ready')).lower()}")
    print(f"accepted_claim: {result.get('accepted_claim', '')}")
    blockers = _list(result.get("blockers"))
    if blockers:
        print("blockers:")
        for blocker in blockers:
            print(f"- {blocker}")
    else:
        print("blockers: none")


if __name__ == "__main__":
    sys.exit(main())
