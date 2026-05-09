#!/usr/bin/env python3
"""Evaluate PR #389 recovery evidence before any merge-ready claim."""

from __future__ import annotations

import argparse
import json
import logging
import re
import shlex
import sys
from pathlib import Path
from typing import Any, Iterable


EXPECTED_REPOSITORY = "rysweet/RabbitHole"
EXPECTED_PR_NUMBER = 389
EXPECTED_BRANCH = "wave5-netbeans-ant-1778295741"
EXPECTED_REMOTE_REF = f"origin/{EXPECTED_BRANCH}"
EXPECTED_BASE_REF = "origin/develop"
EXPECTED_SCENARIO = "alice-desktop-exported-project-smoke"
EXPECTED_WORKFLOW = "exported-project-ant-build-smoke"
EXPECTED_AUTOMATION_MODE = "gated-command-smoke"
EXPECTED_GATE = "ALICE_QA_RUN_GATED_SMOKES"
EXPECTED_NODE_OPTIONS = "--max-old-space-size=32768"
EXPECTED_TEST_CLASS = "org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest"
EXPECTED_FOCUSED_MAVEN_ARGV = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "netbeans",
    "-am",
    f"-Dtest={EXPECTED_TEST_CLASS}",
    "test",
)
REQUIRED_COMMAND_LOG_MARKERS = frozenset(
    (
        "Alice3ProjectTemplateAntSmokeTest",
        "ANT_RUN_PROBE_OK",
        "ANT_RESOURCE_PROBE_OK",
        "ANT_RUNTIME_CONFIGURATION_PROBE_OK",
        "ANT_TEST_MAIN_PROBE_OK",
    )
)
FORBIDDEN_COMMAND_LOG_MARKERS = frozenset(("Java Result:",))
REQUIRED_QA_OUTCOMES = (
    ("scenarioValidation", "scenario-validation-not-passed"),
    ("runnerContract", "runner-contract-not-passed"),
    ("schemaContract", "schema-contract-not-passed"),
)
EXPECTED_GATED_SMOKE_FIELDS = (
    ("scenario", EXPECTED_SCENARIO, "wrong-gated-smoke-scenario"),
    ("workflow", EXPECTED_WORKFLOW, "wrong-gated-smoke-workflow"),
    ("automationMode", EXPECTED_AUTOMATION_MODE, "wrong-gated-smoke-automation-mode"),
)
PR_DESCRIPTION_FLAGS = {
    "hasCurrentHeadEvidence": "pr-description-missing-current-head-evidence",
    "hasQaEvidence": "pr-description-missing-qa-evidence",
    "hasDocsImpact": "pr-description-missing-docs-impact",
    "hasDiffScope": "pr-description-missing-diff-scope",
    "hasQualityAuditCycles": "pr-description-missing-quality-audit-cycles",
}
ALLOWED_DIFF_PREFIXES = (
    "netbeans/src/test/java/org/alice/netbeans/project/",
    "qa/outside-in/alice-desktop/",
)
ALLOWED_DIFF_FILES = {
    ".copilot-evidence/default-workflow-attempt.log",
    "docs/howto/alice-desktop-outside-in-qa.md",
    "docs/howto/finalize-exported-netbeans-ant-smoke-recovery.md",
    "docs/index.md",
    "docs/reference/alice-desktop-outside-in-qa.md",
    "docs/reference/exported-netbeans-ant-project-behavior.md",
    "docs/reference/gadugi-exported-launcher-evidence.md",
    "docs/reference/modernization-corpus-manifest.json",
    "docs/reference/modernization-scorecard.md",
    "netbeans/src/test/java/org/alice/netbeans/Alice3LibraryClasspathTestSupport.java",
    "pyproject.toml",
    "scripts/pr389_recovery_gate.py",
    "tests/test_pr389_recovery_gate.py",
}
FORBIDDEN_PATH_FRAGMENTS = (
    ".env",
    "credential",
    "credentials",
    "secret",
    "secrets",
    "signing/",
)
GREEN_CONCLUSIONS = frozenset(("success",))
COMPLETED_CHECK_STATUSES = frozenset(("completed",))
TIMEOUT_WRAPPER_COMMANDS = frozenset(("timeout", "gtimeout"))


LOGGER = logging.getLogger("pr389_recovery_gate")


def _as_mapping(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _as_bool(value: Any) -> bool:
    return value is True


def _text(value: Any) -> str:
    return "" if value is None else str(value)


def _normalized_text(value: Any) -> str:
    return _text(value).strip().lower()


def _head_sha(evidence: dict[str, Any]) -> str:
    return _text(evidence.get("headSha"))


def _normalize_argv(value: Any) -> tuple[str, ...]:
    if isinstance(value, list):
        return tuple(_text(item) for item in value)
    if isinstance(value, str):
        return tuple(value.split())
    return ()


def _is_focused_maven_argv(value: Any) -> bool:
    return _normalize_argv(value) == EXPECTED_FOCUSED_MAVEN_ARGV


def _dedupe(blockers: Iterable[str]) -> list[str]:
    return list(dict.fromkeys(blockers))


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


def _has_timeout_wrapper_command(command: tuple[str, ...]) -> bool:
    return any(token in TIMEOUT_WRAPPER_COMMANDS for token in command)


def _path_in_focused_scope(path: str) -> bool:
    return path in ALLOWED_DIFF_FILES or path.startswith(ALLOWED_DIFF_PREFIXES)


def _has_parent_traversal(path: str) -> bool:
    return path == ".." or path.startswith("../") or path.endswith("/..") or "/../" in path


def _has_open_quality_findings(cycle: dict[str, Any]) -> bool:
    return bool(_as_list(cycle.get("openFindings"))) or bool(
        _as_list(cycle.get("unresolvedFindings"))
    ) or cycle.get("unresolved") is True


def verify_head(evidence: dict[str, Any]) -> list[str]:
    """Require the local head, remote branch, and PR head evidence to match."""
    blockers: list[str] = []
    head_sha = _head_sha(evidence)
    origin_sha = _text(evidence.get("originHeadSha"))
    pr_sha = _text(evidence.get("prHeadSha"))

    if not head_sha or not origin_sha or not pr_sha:
        blockers.append("missing-pr-head-evidence")
    if evidence.get("repository") != EXPECTED_REPOSITORY:
        blockers.append("wrong-repository")
    if evidence.get("prNumber") != EXPECTED_PR_NUMBER:
        blockers.append("wrong-pr-number")
    if evidence.get("branch") != EXPECTED_BRANCH:
        blockers.append("wrong-authoritative-branch")
    if evidence.get("remoteRef") != EXPECTED_REMOTE_REF:
        blockers.append("wrong-authoritative-branch")
    if evidence.get("baseRef") != EXPECTED_BASE_REF:
        blockers.append("wrong-base-ref")
    if head_sha and origin_sha and head_sha != origin_sha:
        blockers.append("origin-head-sha-mismatch")
    if head_sha and pr_sha and head_sha != pr_sha:
        blockers.append("pr-head-sha-mismatch")

    return _dedupe(blockers)


def verify_diff_scope(evidence: dict[str, Any]) -> list[str]:
    """Reject broad, secret-bearing, or unrelated paths in the PR diff."""
    changed_files = evidence.get("changedFiles")
    if not isinstance(changed_files, list) or not changed_files:
        return ["missing-diff-scope-evidence"]

    blockers: list[str] = []
    for raw_path in changed_files:
        path = _text(raw_path)
        if not path or path.startswith("/") or _has_parent_traversal(path):
            blockers.append("unfocused-diff-scope")
            break
        if not _path_in_focused_scope(path):
            blockers.append("unfocused-diff-scope")
            break
        lowered_path = path.lower()
        if any(fragment in lowered_path for fragment in FORBIDDEN_PATH_FRAGMENTS):
            blockers.append("unfocused-diff-scope")
            break

    return blockers


def verify_qa_evidence(evidence: dict[str, Any]) -> list[str]:
    """Require runnable QA evidence for the gated exported Ant smoke."""
    qa = _as_mapping(evidence.get("qaEvidence"))
    if not qa:
        return ["missing-qa-evidence"]

    blockers: list[str] = []
    for key, blocker in REQUIRED_QA_OUTCOMES:
        if _as_mapping(qa.get(key)).get("outcome") != "passed":
            blockers.append(blocker)

    gated_smoke = _as_mapping(qa.get("gatedSmoke"))
    status_txt = _as_mapping(gated_smoke.get("statusTxt"))
    command_log = _as_mapping(gated_smoke.get("commandLog"))
    if not gated_smoke or not status_txt:
        blockers.append("missing-gated-smoke-evidence")
    for field, expected, blocker in EXPECTED_GATED_SMOKE_FIELDS:
        if gated_smoke.get(field) != expected:
            blockers.append(blocker)
    if gated_smoke.get("gate") != EXPECTED_GATE or _text(gated_smoke.get("gateValue")) != "1":
        blockers.append("gated-smoke-not-enabled")
    if status_txt.get("outcome") != "passed":
        blockers.append("gated-smoke-not-run")
    if _text(status_txt.get("exitCode")) != "0":
        blockers.append("gated-smoke-failed")
    if not _is_focused_maven_argv(status_txt.get("argv")):
        blockers.append("missing-focused-ant-smoke-argv")

    contains = {_text(item) for item in _as_list(command_log.get("contains"))}
    not_contains = {_text(item) for item in _as_list(command_log.get("notContains"))}
    if not all(marker in contains for marker in REQUIRED_COMMAND_LOG_MARKERS):
        blockers.append("missing-ant-smoke-command-log-evidence")
    if any(marker not in not_contains for marker in FORBIDDEN_COMMAND_LOG_MARKERS):
        blockers.append("missing-ant-smoke-failure-sentinel-check")

    return _dedupe(blockers)


def verify_maven_evidence(evidence: dict[str, Any]) -> list[str]:
    """Require the focused no-Sims Maven smoke command and prerequisites."""
    maven = _as_mapping(evidence.get("mavenEvidence"))
    if not maven:
        return ["missing-maven-evidence"]

    blockers: list[str] = []
    if not _as_bool(maven.get("tweedleLangInitialized")):
        blockers.append("tweedle-lang-not-initialized")
    if maven.get("nodeOptions") != EXPECTED_NODE_OPTIONS:
        blockers.append("missing-node-options")
    if not _is_focused_maven_argv(maven.get("argv")):
        blockers.append("missing-focused-ant-smoke-argv")
    if maven.get("exitCode") != 0:
        blockers.append("focused-maven-smoke-failed")
    if maven.get("testClass") != EXPECTED_TEST_CLASS:
        blockers.append("wrong-focused-maven-test-class")

    return _dedupe(blockers)


def verify_quality_audit(evidence: dict[str, Any]) -> list[str]:
    """Require at least three SEEK/VALIDATE/FIX cycles with a clean final cycle."""
    cycles = _as_list(evidence.get("qualityAuditCycles"))
    blockers: list[str] = []
    if len(cycles) < 3:
        blockers.append("insufficient-quality-audit-cycles")
        return blockers

    numbered_cycles: list[tuple[int, dict[str, Any]]] = []
    seen_cycle_numbers: set[int] = set()
    for raw_cycle in cycles:
        cycle = _as_mapping(raw_cycle)
        cycle_number = cycle.get("cycle")
        if (
            not isinstance(cycle_number, int)
            or isinstance(cycle_number, bool)
            or cycle_number < 1
            or cycle_number in seen_cycle_numbers
        ):
            blockers.append("quality-audit-cycle-order-invalid")
        else:
            seen_cycle_numbers.add(cycle_number)
            numbered_cycles.append((cycle_number, cycle))
        if not _text(cycle.get("seek")) or not _text(cycle.get("validate")) or not _text(cycle.get("fix")):
            blockers.append("quality-audit-cycle-incomplete")

    expected_cycle_numbers = set(range(1, max(seen_cycle_numbers, default=0) + 1))
    if seen_cycle_numbers != expected_cycle_numbers:
        blockers.append("quality-audit-cycle-order-invalid")

    final_cycle = max(numbered_cycles, default=(0, {}), key=lambda item: item[0])[1]
    if final_cycle.get("clean") is not True:
        blockers.append("final-quality-audit-cycle-not-clean")
    if _has_open_quality_findings(final_cycle):
        blockers.append("quality-audit-open-finding")

    return _dedupe(blockers)


def verify_docs_impact(evidence: dict[str, Any]) -> list[str]:
    """Require reviewed docs with no unproven behavior claims."""
    docs = _as_mapping(evidence.get("docsImpact"))
    blockers: list[str] = []
    if not _as_bool(docs.get("reviewed")):
        blockers.append("docs-impact-not-reviewed")
    if not _as_list(docs.get("files")):
        blockers.append("docs-impact-files-missing")
    if docs.get("boundedClaimsOnly") is not True or _as_list(docs.get("forbiddenClaims")):
        blockers.append("docs-overclaim-unproven-behavior")
    return _dedupe(blockers)


def verify_github_actions(evidence: dict[str, Any]) -> list[str]:
    """Require completed successful GitHub Actions checks for the exact head SHA."""
    actions = _as_mapping(evidence.get("githubActions"))
    if not actions:
        return ["missing-github-actions-evidence"]

    blockers: list[str] = []
    if actions.get("headSha") != _head_sha(evidence):
        blockers.append("github-actions-stale-head")

    checks = _as_list(actions.get("checks"))
    if not checks:
        blockers.append("missing-github-actions-checks")
    for raw_check in checks:
        check = _as_mapping(raw_check)
        status = _normalized_text(check.get("status"))
        conclusion = _normalized_text(check.get("conclusion"))
        if status not in COMPLETED_CHECK_STATUSES:
            blockers.append("github-actions-not-complete")
        elif conclusion not in GREEN_CONCLUSIONS:
            blockers.append("github-actions-not-green")

    return _dedupe(blockers)


def verify_pr_description(evidence: dict[str, Any]) -> list[str]:
    """Require PR description evidence tied to the current head and bounded claims."""
    description = _as_mapping(evidence.get("prDescription"))
    if not description:
        return ["pr-description-evidence-missing"]

    blockers: list[str] = []
    if description.get("headSha") != _head_sha(evidence):
        blockers.append("pr-description-stale-head")
    for flag, blocker in PR_DESCRIPTION_FLAGS.items():
        if description.get(flag) is not True:
            blockers.append(blocker)
    if description.get("hasBoundedClaims") is not True or _as_list(description.get("forbiddenClaims")):
        blockers.append("pr-description-overclaims-unproven-behavior")
    return _dedupe(blockers)


def verify_command_safety(evidence: dict[str, Any]) -> list[str]:
    """Reject manual merges and external timeout wrapper commands."""
    blockers: list[str] = []
    if evidence.get("manualMergeUsed") is True:
        blockers.append("manual-merge-used")

    commands = _as_list(evidence.get("commands"))
    if not commands:
        blockers.append("missing-command-evidence")
    for command in commands:
        tokens = _command_tokens(command)
        if not tokens:
            continue
        if _has_timeout_wrapper_command(tokens):
            blockers.append("timeout-wrapper-used")
        if _has_manual_merge_command(tokens):
            blockers.append("manual-merge-used")

    return _dedupe(blockers)


VERIFIERS = (
    verify_head,
    verify_command_safety,
    verify_diff_scope,
    verify_qa_evidence,
    verify_maven_evidence,
    verify_quality_audit,
    verify_docs_impact,
    verify_github_actions,
    verify_pr_description,
)


def evaluate_readiness(evidence: dict[str, Any]) -> dict[str, Any]:
    """Return MERGE_READY only when every recovery gate has evidence."""
    blockers = _dedupe(blocker for verifier in VERIFIERS for blocker in verifier(evidence))
    head_sha = _head_sha(evidence)

    if blockers:
        LOGGER.info("PR #389 recovery gate blocked by %d criterion/criteria", len(blockers))
        return {
            "status": "NOT_MERGE_READY",
            "headSha": head_sha,
            "blockers": blockers,
            "summary": "Recovery readiness is blocked: " + ", ".join(blockers),
        }

    LOGGER.info("PR #389 recovery gate passed for head %s", head_sha)
    return {
        "status": "MERGE_READY",
        "headSha": head_sha,
        "blockers": [],
        "summary": "Bounded exported NetBeans Ant smoke recovery evidence is complete for PR #389.",
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
        description="Evaluate PR #389 recovery evidence and print readiness JSON."
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
    result = evaluate_readiness(load_evidence(args.evidence))
    indent = 2 if args.pretty else None
    print(json.dumps(result, indent=indent, sort_keys=True))
    return 0 if result["status"] == "MERGE_READY" else 1


if __name__ == "__main__":
    raise SystemExit(main())
