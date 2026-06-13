#!/usr/bin/env python3
"""Generate merge-ready evidence for a RabbitHole pull request."""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable, Iterable, Optional, Sequence

from merge_ready_evidence_render import (
    SECTION_END,
    SECTION_START,
    PatchBodyError,
    fenced_excerpt,
    patch_body,
    is_secret_flag_arg,
    redact_text,
    render_markdown,
)

PASSING_CI_BUCKETS = {"pass"}


class EvidenceError(RuntimeError):
    """Raised when required merge-ready evidence is missing or not clean."""


@dataclass(frozen=True)
class CommandResult:
    args: tuple[str, ...]
    returncode: int
    stdout: str
    stderr: str


@dataclass(frozen=True)
class EvidenceConfig:
    root: Path
    pr: int
    repo: Optional[str]
    base_ref: str
    quality_audit_file: Path
    scenario_command: str
    scenario_directory: Path
    scenario_name: Optional[str]
    scenario_config: Optional[Path]
    scenario_strict: bool
    patch_pr_description: bool
    dry_run: bool


Runner = Callable[
    [Sequence[str], Path, Iterable[int], Optional[str]],
    CommandResult,
]
ExecutableResolver = Callable[[str], str]


def command_text(args: Sequence[str]) -> str:
    redacted = []
    redact_next = False
    for arg in args:
        if redact_next:
            redacted.append("[REDACTED]")
            redact_next = False
            continue
        redacted.append(redact_text(arg))
        redact_next = is_secret_flag_arg(arg)
    return " ".join(redacted)


def run_command(
    args: Sequence[str],
    cwd: Path,
    allowed_exit_codes: Iterable[int] = (0,),
    input_text: Optional[str] = None,
) -> CommandResult:
    result = subprocess.run(
        list(args),
        cwd=cwd,
        input=input_text,
        capture_output=True,
        text=True,
        check=False,
    )
    allowed = set(allowed_exit_codes)
    command_result = CommandResult(
        args=tuple(args),
        returncode=result.returncode,
        stdout=result.stdout,
        stderr=result.stderr,
    )
    if result.returncode not in allowed:
        raise EvidenceError(
            "\n".join(
                [
                    f"Command failed with exit {result.returncode}: {command_text(args)}",
                    "stdout:",
                    redact_text(result.stdout.strip()) or "(empty)",
                    "stderr:",
                    redact_text(result.stderr.strip()) or "(empty)",
                ]
            )
        )
    return command_result


def resolve_executable(command: str) -> str:
    if "/" in command:
        path = Path(command)
        if path.is_file():
            return str(path)
        raise EvidenceError(f"Required scenario evidence command is missing: {command}")

    resolved = shutil.which(command)
    if resolved:
        return resolved
    raise EvidenceError(f"Required scenario evidence command is missing on PATH: {command}")


def gh_args(args: Sequence[str], repo: Optional[str]) -> list[str]:
    command = ["gh", *args]
    if repo:
        command.extend(["--repo", repo])
    return command


def parse_json(stdout: str, label: str):
    try:
        return json.loads(stdout)
    except json.JSONDecodeError as exc:
        raise EvidenceError(f"{label} did not return valid JSON: {exc}") from exc


def fetch_pr_metadata(config: EvidenceConfig, runner: Runner) -> dict:
    fields = [
        "number",
        "title",
        "url",
        "body",
        "headRefName",
        "baseRefName",
        "baseRefOid",
        "isDraft",
        "mergeable",
        "reviewDecision",
        "headRefOid",
    ]
    result = runner(
        gh_args(["pr", "view", str(config.pr), "--json", ",".join(fields)], config.repo),
        config.root,
        (0,),
        None,
    )
    metadata = parse_json(result.stdout, "PR metadata")
    if not metadata.get("url"):
        raise EvidenceError(f"PR access is missing or incomplete for PR {config.pr}")
    if not metadata.get("headRefName") or not metadata.get("baseRefName"):
        raise EvidenceError(f"PR metadata is missing branch refs for PR {config.pr}")
    if not metadata.get("headRefOid"):
        raise EvidenceError(f"PR metadata is missing headRefOid for PR {config.pr}")
    if not metadata.get("baseRefOid"):
        raise EvidenceError(f"PR metadata is missing baseRefOid for PR {config.pr}")
    return metadata


def verify_local_head_matches_pr(config: EvidenceConfig, pr: dict, runner: Runner) -> None:
    result = runner(["git", "rev-parse", "HEAD"], config.root, (0,), None)
    local_head = result.stdout.strip()
    pr_head = str(pr["headRefOid"]).strip()
    if local_head != pr_head:
        raise EvidenceError(
            "scope review refused: local HEAD does not match PR head "
            f"({local_head[:12]} != {pr_head[:12]})"
        )


def verify_worktree_clean(config: EvidenceConfig, runner: Runner) -> None:
    result = runner(["git", "status", "--porcelain"], config.root, (0,), None)
    if result.stdout.strip():
        raise EvidenceError("scope review refused: local worktree has uncommitted changes")


def normalized_ref_name(ref: str) -> str:
    for prefix in ("refs/remotes/origin/", "refs/heads/", "origin/"):
        if ref.startswith(prefix):
            return ref[len(prefix) :]
    return ref


def verify_scope_base_matches_pr(config: EvidenceConfig, pr: dict) -> None:
    expected = str(pr["baseRefName"])
    actual = normalized_ref_name(config.base_ref)
    if actual != expected:
        raise EvidenceError(
            f"scope review refused: base ref {config.base_ref} does not match PR base {expected}"
        )


def verify_scope_base_oid_matches_pr(config: EvidenceConfig, pr: dict, runner: Runner) -> None:
    result = runner(["git", "rev-parse", config.base_ref], config.root, (0,), None)
    local_base_oid = result.stdout.strip()
    pr_base_oid = str(pr["baseRefOid"]).strip()
    if local_base_oid != pr_base_oid:
        raise EvidenceError(
            "scope review refused: base ref does not match PR base commit "
            f"({local_base_oid[:12]} != {pr_base_oid[:12]})"
        )


def collect_ci_status(config: EvidenceConfig, runner: Runner) -> list[dict]:
    result = runner(
        gh_args(
            [
                "pr",
                "checks",
                str(config.pr),
                "--json",
                "bucket,completedAt,link,name,state,workflow",
            ],
            config.repo,
        ),
        config.root,
        (0, 8),
        None,
    )
    checks = parse_json(result.stdout or "[]", "PR checks")
    if not checks:
        raise EvidenceError(f"CI status is missing for PR {config.pr}: no checks returned")

    blocking = [
        check
        for check in checks
        if str(check.get("bucket", "")).lower() not in PASSING_CI_BUCKETS
    ]
    if blocking:
        details = ", ".join(
            f"{check.get('name', '(unnamed)')}={check.get('bucket') or check.get('state')}"
            for check in blocking
        )
        raise EvidenceError(f"CI is not merge-ready for PR {config.pr}: {details}")
    return checks


def collect_scenario_evidence(
    config: EvidenceConfig,
    runner: Runner,
    executable_resolver: ExecutableResolver,
) -> dict:
    command = executable_resolver(config.scenario_command)
    scenario_directory = (
        config.scenario_directory
        if config.scenario_directory.is_absolute()
        else config.root / config.scenario_directory
    )
    validate_args = [command, "validate", "--directory", str(scenario_directory)]
    if config.scenario_strict:
        validate_args.append("--strict")

    run_args = [command, "run", "--directory", str(scenario_directory)]
    if config.scenario_name:
        run_args.extend(["--scenario", config.scenario_name])
    if config.scenario_config:
        scenario_config = (
            config.scenario_config
            if config.scenario_config.is_absolute()
            else config.root / config.scenario_config
        )
        run_args.extend(["--config", str(scenario_config)])

    validate = runner(validate_args, config.root, (0,), None)
    run = runner(run_args, config.root, (0,), None)
    return {
        "validate_command": command_text(validate.args),
        "validate_output": validate.stdout.strip(),
        "run_command": command_text(run.args),
        "run_output": run.stdout.strip(),
    }


def read_quality_audit_summary(path: Path) -> dict:
    if not path.is_file():
        raise EvidenceError(f"quality-audit evidence is required and was not found: {path}")
    text = path.read_text(encoding="utf-8")
    if not text.strip():
        raise EvidenceError(f"quality-audit evidence is empty: {path}")

    lines = [line.strip() for line in text.splitlines() if line.strip()]
    clean = any(line == "QUALITY AUDIT: CLEAN" for line in lines)
    if not clean:
        raise EvidenceError(
            "quality-audit evidence is not clean: missing 'QUALITY AUDIT: CLEAN'"
        )

    return {
        "clean": clean,
        "summary_lines": lines[:20],
        "path": path.name,
    }


def parse_name_status(stdout: str) -> list[dict]:
    entries = []
    for line in stdout.splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        status = parts[0]
        if status.startswith(("R", "C")) and len(parts) >= 3:
            entries.append({"status": status, "path": parts[2], "previous_path": parts[1]})
        elif len(parts) >= 2:
            entries.append({"status": status, "path": parts[1]})
    return entries


def collect_scope_review(config: EvidenceConfig, pr: dict, runner: Runner) -> dict:
    base_oid = str(pr["baseRefOid"]).strip()
    head_oid = str(pr["headRefOid"]).strip()
    result = runner(
        ["git", "diff", "--name-status", f"{base_oid}...{head_oid}"],
        config.root,
        (0,),
        None,
    )
    changed_files = parse_name_status(result.stdout)
    if not changed_files:
        raise EvidenceError(
            f"scope review found no changed files against {config.base_ref}; "
            "merge-ready evidence requires a non-empty PR diff"
        )
    return {
        "base_ref": config.base_ref,
        "changed_files": changed_files,
        "categories": categorize_changed_files(entry["path"] for entry in changed_files),
    }


def categorize_changed_files(paths: Iterable[str]) -> dict[str, list[str]]:
    categories: dict[str, list[str]] = {
        "docs": [],
        "tests": [],
        "automation": [],
        "ci": [],
        "source": [],
    }
    for path in paths:
        if path.startswith("docs/") or path in {"README.md", "AGENTS.md"} or path.endswith(".md"):
            categories["docs"].append(path)
        elif path.startswith("tests/") or "/test/" in path or path.endswith("_test.py"):
            categories["tests"].append(path)
        elif path.startswith("scripts/") or path == "pyproject.toml":
            categories["automation"].append(path)
        elif path.startswith(".github/workflows/"):
            categories["ci"].append(path)
        else:
            categories["source"].append(path)
    return categories


def docs_impact_check(scope_review: dict) -> dict:
    categories = scope_review["categories"]
    docs_changed = bool(categories["docs"])
    docs_likely_required = bool(
        categories["automation"] or categories["ci"] or categories["source"]
    )
    if docs_likely_required and docs_changed:
        outcome = "PASS"
        rationale = "User-facing or workflow-affecting changes include documentation updates."
    elif docs_likely_required:
        outcome = "REVIEW"
        rationale = "Changes may affect users or contributors; confirm documentation is not needed."
    else:
        outcome = "N/A"
        rationale = "Only tests or documentation changed."
    return {
        "outcome": outcome,
        "docs_changed": docs_changed,
        "docs_likely_required": docs_likely_required,
        "rationale": rationale,
    }


def generate_evidence(
    config: EvidenceConfig,
    runner: Runner = run_command,
    executable_resolver: ExecutableResolver = resolve_executable,
) -> dict:
    pr = fetch_pr_metadata(config, runner)
    verify_local_head_matches_pr(config, pr, runner)
    verify_worktree_clean(config, runner)
    verify_scope_base_matches_pr(config, pr)
    verify_scope_base_oid_matches_pr(config, pr, runner)
    scenario = collect_scenario_evidence(config, runner, executable_resolver)
    verify_local_head_matches_pr(config, pr, runner)
    verify_worktree_clean(config, runner)
    verify_scope_base_oid_matches_pr(config, pr, runner)
    quality = read_quality_audit_summary(config.quality_audit_file)
    ci_status = collect_ci_status(config, runner)
    scope_review = collect_scope_review(config, pr, runner)
    docs_impact = docs_impact_check(scope_review)
    evidence = {
        "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "pr": pr,
        "scenario": scenario,
        "quality_audit": quality,
        "ci_status": ci_status,
        "scope_review": scope_review,
        "docs_impact": docs_impact,
    }
    evidence["markdown"] = render_markdown(evidence)
    return evidence


def patch_pr_description(config: EvidenceConfig, evidence: dict, runner: Runner) -> None:
    current_pr = fetch_pr_metadata(config, runner)
    if str(current_pr["headRefOid"]).strip() != str(evidence["pr"]["headRefOid"]).strip():
        raise EvidenceError("PR description patch refused: PR head changed after evidence collection")
    if str(current_pr["baseRefOid"]).strip() != str(evidence["pr"]["baseRefOid"]).strip():
        raise EvidenceError("PR description patch refused: PR base changed after evidence collection")
    if current_pr.get("body") != evidence["pr"].get("body"):
        raise EvidenceError("PR description patch refused: PR body changed after evidence collection")
    try:
        body = patch_body(current_pr.get("body"), evidence["markdown"])
    except PatchBodyError as exc:
        raise EvidenceError(str(exc)) from exc
    if config.dry_run:
        print("DRY RUN: PR description patch not applied.", file=sys.stderr)
        return

    runner(
        gh_args(["pr", "edit", str(config.pr), "--body-file", "-"], config.repo),
        config.root,
        (0,),
        body,
    )
