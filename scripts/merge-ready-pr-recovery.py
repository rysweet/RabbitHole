#!/usr/bin/env python3
"""Recover a pull request through the merge-ready evidence gate.

The script is intentionally a classifier: it fetches evidence for the exact PR
head and reports MERGE_READY or NOT_MERGE_READY. It does not merge, push, or edit
PR metadata.
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Any, Callable, Iterable


LOGGER = logging.getLogger("merge-ready-pr-recovery")

EXPECTED_PR_NUMBER = 425
EXPECTED_HEAD_BRANCH = "feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow"
EXPECTED_BASE_BRANCH = "develop"
MODEL_EXPORT_TEST = "core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java"
NODE_OPTIONS = "--max-old-space-size=32768"
NON_CLAIMS = (
    "no full UI automation, visible rendering correctness, grading, creative "
    "assessment, full lesson completion, or full Tweedle/player decode claimed"
)
GREEN_CONCLUSIONS = {"SUCCESS"}
PR_METADATA_FIELDS = "number,state,isDraft,reviewDecision,baseRefName,headRefName,headRefOid,title,url"
PR_BODY_REQUIREMENTS = {
    "Head SHA": ["Head SHA", "head sha"],
    "State": ["State:"],
    "Draft": ["Draft:"],
    "Review state": ["Review state"],
    "Focused diff": ["Focused diff"],
    "CI/check status": ["CI/check status", "GitHub Actions"],
    "Local validation": ["Local validation"],
    "QA/scenario": ["QA/scenario"],
    "Docs impact": ["Docs impact"],
    "Quality audit cycles": ["Quality audit cycles"],
    "Non-claims": ["Non-claims"],
}
OVERCLAIM_PHRASES = (
    "full UI automation passed",
    "visible rendering correctness verified",
    "grading verified",
    "creative assessment verified",
    "full lesson completion verified",
    "full Tweedle/player decode verified",
)
QA_SURFACE_PREFIX = "qa/outside-in/"
QA_SCENARIO_EXTENSIONS = (".yaml", ".yml")


class RecoveryEvidenceError(RuntimeError):
    """Raised when required recovery evidence is malformed or unavailable."""


@dataclass(frozen=True)
class RecoveryInputs:
    pr_number: int
    head_branch: str
    base_branch: str
    expected_diff_paths: set[str]
    design_scope: str = "test-only"
    branch_policy_requires_approval: bool = True


@dataclass(frozen=True)
class PRMetadata:
    number: int
    state: str
    is_draft: bool
    review_decision: str
    base_ref_name: str
    head_ref_name: str
    head_ref_oid: str
    title: str
    url: str


CommandRunner = Callable[..., SimpleNamespace]


def run_command(
    command: list[str],
    *,
    cwd: Path | None = None,
    env: dict[str, str] | None = None,
) -> SimpleNamespace:
    """Run a command and return captured output without shell expansion."""

    LOGGER.info("Running command: %s", " ".join(command))
    completed = subprocess.run(
        command,
        cwd=cwd,
        env=env,
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return SimpleNamespace(
        returncode=completed.returncode,
        stdout=completed.stdout,
        stderr=completed.stderr,
    )


def sanitize_output_excerpt(text: str, *, max_chars: int = 240) -> str:
    compact = " ".join(line.strip() for line in text.splitlines() if line.strip())
    if not compact:
        return ""
    home = str(Path.home())
    if home:
        compact = compact.replace(home, "~")
    compact = re.sub(r"(?i)(token|secret|password|authorization|api[_-]?key)=\S+", r"\1=<redacted>", compact)
    compact = re.sub(r"(?i)Bearer\s+\S+", "Bearer <redacted>", compact)
    compact = re.sub(r"(?<![\w.-])(?:~|/(?:home|tmp|var|workspace|mnt|Users))/[^\s:;]+", "<path>", compact)
    if len(compact) > max_chars:
        return compact[: max_chars - 3].rstrip() + "..."
    return compact


def command_label(command: list[str] | None) -> str:
    if not command:
        return "command"
    if command[:3] == ["gh", "pr", "view"]:
        return "gh pr view"
    if command[:3] == ["gh", "pr", "checks"]:
        return "gh pr checks"
    if command[:2] == ["git", "diff"]:
        return "git diff"
    if command[:2] == ["git", "fetch"]:
        return "git fetch"
    if command[:2] == ["git", "checkout"]:
        return "git checkout"
    if command[:2] == ["git", "pull"]:
        return "git pull"
    if command[:2] == ["git", "submodule"]:
        return "git submodule"
    return command[0]


def format_command_failure(context: str, result: SimpleNamespace, command: list[str] | None = None) -> str:
    detail = f"{context} failed ({command_label(command)} exit {result.returncode})"
    excerpt = sanitize_output_excerpt((result.stderr or result.stdout or ""))
    if excerpt:
        detail += f": {excerpt}"
    return detail


def require_success(result: SimpleNamespace, context: str, command: list[str] | None = None) -> None:
    if result.returncode != 0:
        raise RecoveryEvidenceError(format_command_failure(context, result, command))


def parse_json_object(raw_json: str, context: str) -> dict[str, Any]:
    try:
        value = json.loads(raw_json)
    except json.JSONDecodeError as exc:
        raise RecoveryEvidenceError(f"{context} returned invalid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise RecoveryEvidenceError(f"{context} returned {type(value).__name__}, expected object")
    return value


def parse_json_list(raw_json: str, context: str) -> list[dict[str, Any]]:
    try:
        value = json.loads(raw_json)
    except json.JSONDecodeError as exc:
        raise RecoveryEvidenceError(f"{context} returned invalid JSON: {exc}") from exc
    if not isinstance(value, list):
        raise RecoveryEvidenceError(f"{context} returned {type(value).__name__}, expected list")
    items: list[dict[str, Any]] = []
    for index, item in enumerate(value):
        if not isinstance(item, dict):
            raise RecoveryEvidenceError(f"{context} item {index} is {type(item).__name__}, expected object")
        items.append(item)
    return items


def parse_pr_metadata(raw_json: str) -> PRMetadata:
    payload = parse_json_object(raw_json, "gh pr view metadata")
    required = ["number", "state", "isDraft", "baseRefName", "headRefName", "headRefOid", "title", "url"]
    missing = [field for field in required if field not in payload]
    if missing:
        raise RecoveryEvidenceError(f"gh pr view metadata missing required fields: {', '.join(missing)}")
    review_decision = payload.get("reviewDecision") or "none"
    return PRMetadata(
        number=int(payload["number"]),
        state=str(payload["state"]),
        is_draft=bool(payload["isDraft"]),
        review_decision=str(review_decision),
        base_ref_name=str(payload["baseRefName"]),
        head_ref_name=str(payload["headRefName"]),
        head_ref_oid=str(payload["headRefOid"]),
        title=str(payload["title"]),
        url=str(payload["url"]),
    )


def value_from_mapping_or_object(source: Any, key: str) -> Any:
    if isinstance(source, dict):
        return source[key]
    return getattr(source, key)


def status_from_blockers(blockers: list[str], clean_status: str) -> str:
    return "blocker" if blockers else clean_status


def validate_requested_target(inputs: RecoveryInputs) -> dict[str, Any]:
    blockers: list[str] = []
    if inputs.pr_number != EXPECTED_PR_NUMBER:
        blockers.append(
            "NOT_MERGE_READY: Recovery target is fixed to "
            f"PR #{EXPECTED_PR_NUMBER}, but requested PR #{inputs.pr_number}."
        )
    if inputs.head_branch != EXPECTED_HEAD_BRANCH:
        blockers.append(
            "NOT_MERGE_READY: Recovery head branch is fixed to "
            f"{EXPECTED_HEAD_BRANCH}, but requested {inputs.head_branch}."
        )
    if inputs.base_branch != EXPECTED_BASE_BRANCH:
        blockers.append(
            "NOT_MERGE_READY: Recovery base branch is fixed to "
            f"{EXPECTED_BASE_BRANCH}, but requested {inputs.base_branch}."
        )
    return {"status": status_from_blockers(blockers, "valid"), "blockers": blockers}


def validate_metadata_target(inputs: RecoveryInputs, metadata: PRMetadata) -> dict[str, Any]:
    blockers: list[str] = []
    if metadata.number != inputs.pr_number:
        blockers.append(
            "NOT_MERGE_READY: GitHub metadata PR number "
            f"#{metadata.number} does not match requested PR #{inputs.pr_number}."
        )
    if metadata.head_ref_name != inputs.head_branch:
        blockers.append(
            "NOT_MERGE_READY: GitHub metadata head branch "
            f"{metadata.head_ref_name} does not match requested {inputs.head_branch}."
        )
    if metadata.base_ref_name != inputs.base_branch:
        blockers.append(
            "NOT_MERGE_READY: GitHub metadata base branch "
            f"{metadata.base_ref_name} does not match requested {inputs.base_branch}."
        )
    return {"status": status_from_blockers(blockers, "valid"), "blockers": blockers}


def validate_head_alignment(local_head: str, metadata: Any) -> dict[str, str]:
    expected_head = value_from_mapping_or_object(metadata, "head_ref_oid")
    local = local_head.strip()
    if local != expected_head:
        raise RecoveryEvidenceError(
            f"Local HEAD is stale: git rev-parse HEAD={local}, PR headRefOid={expected_head}"
        )
    return {"status": "aligned", "head_sha": local}


def parse_name_status(raw_diff: str) -> list[tuple[str, str]]:
    changed: list[tuple[str, str]] = []
    for line in raw_diff.splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 2:
            raise RecoveryEvidenceError(f"Could not parse git diff --name-status line: {line!r}")
        status = parts[0]
        path = parts[-1]
        changed.append((status, path))
    return changed


def changed_paths(changed_files: Iterable[str | tuple[str, str]]) -> list[str]:
    paths: list[str] = []
    for item in changed_files:
        if isinstance(item, tuple):
            paths.append(item[1])
        else:
            paths.append(item)
    return paths


def analyze_diff_scope(
    *,
    changed_files: Iterable[tuple[str, str]],
    allowed_paths: set[str],
    design_scope: str,
) -> dict[str, Any]:
    files = list(changed_files)
    paths = changed_paths(files)
    blockers: list[str] = []
    if not paths:
        blockers.append("NOT_MERGE_READY: PR diff is empty; no focused recovery evidence can be tied to changes.")

    out_of_scope = [path for path in paths if path not in allowed_paths]
    if out_of_scope:
        blockers.append(
            "NOT_MERGE_READY: Diff scope includes files outside "
            f"{design_scope} design: {', '.join(out_of_scope)}."
        )

    return {
        "status": status_from_blockers(blockers, "focused"),
        "changed_files": files,
        "allowed_paths": sorted(allowed_paths),
        "blockers": blockers,
    }


def model_export_validation_command() -> dict[str, Any]:
    return {
        "command": [
            "mvn",
            "-pl",
            "core/model-loading",
            "-am",
            "-DfailIfNoTests=false",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "-Dtest=ModelExportTest",
            "test",
        ],
        "env": {"NODE_OPTIONS": NODE_OPTIONS},
    }


def evaluate_checks(
    *,
    checks: list[dict[str, Any]],
    head_before: str,
    head_after: str,
    local_head: str,
) -> dict[str, Any]:
    blockers: list[str] = []
    if head_before != head_after or head_before != local_head:
        blockers.append(
            "NOT_MERGE_READY: Check evidence is stale because PR head SHA changed "
            f"(before={head_before}, after={head_after}, local={local_head})."
        )
    if not checks:
        blockers.append("NOT_MERGE_READY: No GitHub Actions checks were returned for the exact PR head SHA.")

    for check in checks:
        name = str(check.get("name") or "<unnamed check>")
        state = str(check.get("state") or "")
        conclusion = check.get("conclusion")
        if state != "COMPLETED" or conclusion not in GREEN_CONCLUSIONS:
            blockers.append(
                "NOT_MERGE_READY: GitHub Actions check "
                f"{name} is state={state or 'unknown'} conclusion={conclusion or 'none'}."
            )

    return {"status": status_from_blockers(blockers, "green"), "checks": checks, "blockers": blockers}


def evaluate_pr_state(
    metadata: dict[str, Any] | PRMetadata,
    *,
    branch_policy_requires_approval: bool,
) -> dict[str, Any]:
    state = value_from_mapping_or_object(metadata, "state")
    is_draft = value_from_mapping_or_object(metadata, "is_draft")
    review_decision = value_from_mapping_or_object(metadata, "review_decision") or "none"
    blockers: list[str] = []
    if state != "OPEN":
        blockers.append(f"NOT_MERGE_READY: PR state is {state}; recovery requires OPEN.")
    if is_draft:
        blockers.append("NOT_MERGE_READY: PR is still draft.")
    if review_decision == "CHANGES_REQUESTED":
        blockers.append("NOT_MERGE_READY: PR review state is CHANGES_REQUESTED.")
    if branch_policy_requires_approval and review_decision != "APPROVED":
        blockers.append(
            "NOT_MERGE_READY: Branch policy requires approval, "
            f"but review state is {review_decision}."
        )
    return {"status": status_from_blockers(blockers, "reviewable"), "blockers": blockers}


def classify_qa_evidence(
    *,
    changed_files: list[str],
    discovered_paths: list[str],
    executed_evidence: list[dict[str, str]],
    ci_evidence: list[dict[str, str]],
) -> dict[str, Any]:
    if ci_evidence:
        return {
            "classification": "ci-provided",
            "rationale": "CI provided applicable runnable QA evidence for the exact PR head.",
            "blockers": [],
        }

    if any(item.get("kind") == "executed" for item in executed_evidence):
        return {
            "classification": "executed",
            "rationale": "Applicable checked-in QA evidence was executed for the exact PR head.",
            "blockers": [],
        }

    applicable = qa_surface_applies(changed_files)
    if not applicable:
        return {
            "classification": "not-applicable",
            "rationale": "No runnable desktop QA path applies to the changed surface; focused validation is the applicable evidence.",
            "blockers": [],
        }

    if any(item.get("kind") == "prepare-only" for item in executed_evidence):
        return {
            "classification": "prepare-only",
            "rationale": "Only setup or scenario-shape validation evidence was executed.",
            "blockers": ["NOT_MERGE_READY: QA/scenario evidence is prepare-only, not runnable behavior proof."],
        }

    if discovered_paths:
        return {
            "classification": "documented-manual",
            "rationale": "Applicable QA paths were discovered, but no runnable proof was executed.",
            "blockers": ["NOT_MERGE_READY: QA/scenario evidence is documented or manual only."],
        }

    return {
        "classification": "missing",
        "rationale": "Applicable runnable QA/scenario evidence should exist but was not found.",
        "blockers": ["NOT_MERGE_READY: QA/scenario evidence is missing for the changed surface."],
    }


def qa_surface_applies(changed_files: Iterable[str]) -> bool:
    return any(path.startswith(QA_SURFACE_PREFIX) or path.endswith(QA_SCENARIO_EXTENSIONS) for path in changed_files)


def review_docs_impact(
    *,
    changed_files: list[str],
    design_scope: str,
    behavior_changed: bool,
) -> dict[str, Any]:
    docs_files = [path for path in changed_files if path.startswith("docs/")]
    if docs_files and design_scope == "test-only":
        return {
            "status": "blocker",
            "rationale": "Documentation changes are out of scope for a test-only recovery unless the design is updated.",
            "blockers": [
                "NOT_MERGE_READY: Test-only PR includes documentation files outside the focused scope: "
                f"{', '.join(docs_files)}."
            ],
        }
    if behavior_changed and not docs_files:
        return {
            "status": "blocker",
            "rationale": "Behavior changed without durable documentation evidence.",
            "blockers": ["NOT_MERGE_READY: Documentation impact is missing for behavior-changing work."],
        }
    if docs_files:
        return {"status": "changed-docs", "rationale": f"Documentation changed: {', '.join(docs_files)}.", "blockers": []}
    return {
        "status": "no-doc-change",
        "rationale": f"No documentation change required for {design_scope} characterization coverage with unchanged behavior.",
        "blockers": [],
    }


def validate_pr_description(body: str) -> dict[str, Any]:
    missing = [
        label
        for label, needles in PR_BODY_REQUIREMENTS.items()
        if not any(needle in body for needle in needles)
    ]
    blockers = [f"NOT_MERGE_READY: PR description missing {label} evidence." for label in missing]
    return {"status": status_from_blockers(blockers, "sufficient"), "missing": missing, "blockers": blockers}


def evaluate_bounded_claims(body: str) -> dict[str, Any]:
    overclaims = [
        phrase
        for phrase in OVERCLAIM_PHRASES
        if phrase.lower() in body.lower()
    ]
    blockers = [
        f"NOT_MERGE_READY: PR description overclaims unproven behavior: {', '.join(overclaims)}."
    ] if overclaims else []
    return {"status": status_from_blockers(blockers, "bounded"), "blockers": blockers}


def collect_blockers(*items: dict[str, Any]) -> list[str]:
    blockers: list[str] = []
    for item in items:
        blockers.extend(str(blocker) for blocker in item.get("blockers", []))
    return blockers


def quality_cycle(
    *,
    number: int,
    seek: str,
    validate: str,
    blocker_fix: str,
    blockers: list[str],
) -> dict[str, Any]:
    return {
        "cycle": number,
        "SEEK": seek,
        "VALIDATE": validate,
        "FIX": "No fix required." if not blockers else blocker_fix,
        "status": status_from_blockers(blockers, "clean"),
        "blockers": blockers,
    }


def run_quality_audit_cycles(evidence: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    cycle1_blockers = collect_blockers(evidence["diff_scope"])
    cycle2_blockers = collect_blockers(evidence["test_adequacy"])
    cycle3_blockers = collect_blockers(
        evidence["ci"],
        evidence["qa"],
        evidence["docs"],
        evidence["pr_metadata"],
        evidence["pr_state"],
        evidence["claims"],
    )
    return [
        quality_cycle(
            number=1,
            seek="Diff scope drift, unrelated files, binary artifacts, and generated noise.",
            validate="Compared the PR diff against the expected focused path set.",
            blocker_fix="Block readiness until scope is narrowed or design is updated.",
            blockers=cycle1_blockers,
        ),
        quality_cycle(
            number=2,
            seek="Test adequacy gaps for the model export characterization claims.",
            validate="Ran focused model-loading validation and reviewed the protected behavior surface.",
            blocker_fix="Block readiness until focused validation passes.",
            blockers=cycle2_blockers,
        ),
        quality_cycle(
            number=3,
            seek="Evidence completeness gaps across CI, QA, docs, PR metadata, review state, and bounded claims.",
            validate="Compared all collected evidence against the merge-ready recovery contract.",
            blocker_fix="Block readiness until missing evidence is supplied.",
            blockers=cycle3_blockers,
        ),
    ]


def run_and_require(
    runner: CommandRunner,
    command: list[str],
    *,
    cwd: Path,
    context: str,
    env: dict[str, str] | None = None,
) -> SimpleNamespace:
    result = runner(command, cwd=cwd, env=env)
    require_success(result, context, command)
    return result


def sync_pr_branch(inputs: RecoveryInputs, runner: CommandRunner, root: Path) -> None:
    run_and_require(
        runner,
        ["git", "fetch", "origin", inputs.head_branch],
        cwd=root,
        context=f"fetch origin {inputs.head_branch}",
    )
    run_and_require(runner, ["git", "checkout", inputs.head_branch], cwd=root, context="checkout PR branch")
    run_and_require(
        runner,
        ["git", "pull", "--ff-only", "origin", inputs.head_branch],
        cwd=root,
        context=f"pull origin {inputs.head_branch}",
    )


def read_pr_metadata(runner: CommandRunner, root: Path, pr_number: int) -> PRMetadata:
    result = run_and_require(
        runner,
        ["gh", "pr", "view", str(pr_number), "--json", PR_METADATA_FIELDS],
        cwd=root,
        context="read PR metadata",
    )
    return parse_pr_metadata(result.stdout)


def read_local_head(runner: CommandRunner, root: Path) -> str:
    result = run_and_require(runner, ["git", "rev-parse", "HEAD"], cwd=root, context="read local HEAD")
    return result.stdout.strip()


def head_alignment_blockers(local_head: str, metadata: PRMetadata) -> list[str]:
    try:
        validate_head_alignment(local_head, metadata)
    except RecoveryEvidenceError as exc:
        return [f"NOT_MERGE_READY: {exc}"]
    return []


def read_changed_files(inputs: RecoveryInputs, runner: CommandRunner, root: Path) -> list[tuple[str, str]]:
    run_and_require(
        runner,
        ["git", "fetch", "origin", inputs.base_branch],
        cwd=root,
        context=f"fetch origin {inputs.base_branch}",
    )
    diff_result = run_and_require(
        runner,
        ["git", "diff", "--name-status", f"origin/{inputs.base_branch}...HEAD"],
        cwd=root,
        context="read PR diff scope",
    )
    return parse_name_status(diff_result.stdout)


def run_focused_validation(runner: CommandRunner, root: Path) -> dict[str, Any]:
    run_and_require(
        runner,
        ["git", "submodule", "update", "--init", "tweedle-lang"],
        cwd=root,
        context="initialize tweedle-lang submodule",
    )
    validation = model_export_validation_command()
    result = runner(validation["command"], cwd=root, env={**os.environ, **validation["env"]})
    blockers: list[str] = []
    if result.returncode != 0:
        blockers.append(f"NOT_MERGE_READY: {format_command_failure('focused local validation', result, validation['command'])}")
    return {
        "command": validation["command"],
        "blockers": blockers,
        "test_adequacy": {"status": status_from_blockers(blockers, "sufficient"), "blockers": blockers},
    }


def read_pr_head_oid(runner: CommandRunner, root: Path, pr_number: int, context: str) -> str:
    result = run_and_require(
        runner,
        ["gh", "pr", "view", str(pr_number), "--json", "headRefOid"],
        cwd=root,
        context=context,
    )
    return str(parse_json_object(result.stdout, context)["headRefOid"])


def collect_ci_evidence(
    inputs: RecoveryInputs,
    runner: CommandRunner,
    root: Path,
    *,
    local_head: str,
    head_before: str,
) -> dict[str, Any]:
    checks_result = run_and_require(
        runner,
        ["gh", "pr", "checks", str(inputs.pr_number), "--json", "name,state,conclusion,link"],
        cwd=root,
        context="read PR checks",
    )
    head_after = read_pr_head_oid(runner, root, inputs.pr_number, "read PR head after checks")
    checks = parse_json_list(checks_result.stdout, "gh pr checks")
    return evaluate_checks(checks=checks, head_before=head_before, head_after=head_after, local_head=local_head)


def discover_qa_paths(runner: CommandRunner, root: Path) -> list[str]:
    discovered_paths: list[str] = []
    find_result = runner(["find", "qa/outside-in", "-maxdepth", "4", "-type", "f"], cwd=root, env=None)
    if find_result.returncode == 0:
        discovered_paths.extend(find_result.stdout.splitlines())

    grep_result = runner(
        ["grep", "-R", "export\\|model", "-n", "qa/outside-in", "docs/reference", "docs/howto"],
        cwd=root,
        env=None,
    )
    if grep_result.returncode == 0:
        discovered_paths.extend(line.split(":", 1)[0] for line in grep_result.stdout.splitlines() if ":" in line)
    return sorted(set(discovered_paths))


def collect_qa_evidence(
    *,
    changed_files: list[str],
    runner: CommandRunner,
    root: Path,
) -> dict[str, Any]:
    discovered_paths = discover_qa_paths(runner, root) if qa_surface_applies(changed_files) else []
    return classify_qa_evidence(
        changed_files=changed_files,
        discovered_paths=discovered_paths,
        executed_evidence=[],
        ci_evidence=[],
    )


def read_pr_body(runner: CommandRunner, root: Path, pr_number: int) -> str:
    result = run_and_require(
        runner,
        ["gh", "pr", "view", str(pr_number), "--json", "body"],
        cwd=root,
        context="read PR body",
    )
    return str(parse_json_object(result.stdout, "PR body").get("body") or "")


def build_report(
    *,
    inputs: RecoveryInputs,
    metadata: PRMetadata,
    local_head: str,
    validation_command: list[str],
    local_validation_blockers: list[str],
    ci: dict[str, Any],
    qa: dict[str, Any],
    docs: dict[str, Any],
    diff_scope: dict[str, Any],
    pr_description: dict[str, Any],
    cycles: list[dict[str, Any]],
    blockers: list[str],
) -> dict[str, Any]:
    result = "MERGE_READY" if not blockers and cycles[-1]["status"] == "clean" else "NOT_MERGE_READY"
    return {
        "result": result,
        "pr": metadata.number,
        "head": local_head,
        "base": inputs.base_branch,
        "branch": inputs.head_branch,
        "state": metadata.state,
        "draft": metadata.is_draft,
        "review_state": metadata.review_decision,
        "files_modified": [],
        "no_op_justification": (
            "No repository changes were required by the recovery classifier; evidence was tied to "
            f"head {local_head}, GitHub Actions status {ci['status']}, QA/scenario classification "
            f"{qa['classification']}, and blockers {len(blockers)}."
        ),
        "diff_scope": diff_scope["status"],
        "local_validation": (
            f"NODE_OPTIONS={NODE_OPTIONS} {' '.join(validation_command)}: "
            + ("passed" if not local_validation_blockers else "blocked")
        ),
        "github_actions": "green for exact SHA" if ci["status"] == "green" else "blocker",
        "qa_scenario_evidence": f"{qa['classification']}: {qa['rationale']}",
        "docs_impact": f"{docs['status']}: {docs['rationale']}",
        "quality_audit_cycles": cycles,
        "pr_description_evidence": pr_description["status"],
        "non_claims": NON_CLAIMS,
        "blockers": blockers,
    }


def build_target_blocked_report(
    *,
    inputs: RecoveryInputs,
    blockers: list[str],
    metadata: PRMetadata | None = None,
) -> dict[str, Any]:
    cycles = [
        quality_cycle(
            number=1,
            seek="Fixed PR, head branch, and base branch target drift.",
            validate="Compared requested inputs and GitHub metadata to the PR #425 recovery contract.",
            blocker_fix="Run recovery only for the fixed PR #425 target and stop if GitHub metadata disagrees.",
            blockers=blockers,
        )
    ]
    return {
        "result": "NOT_MERGE_READY",
        "pr": metadata.number if metadata else inputs.pr_number,
        "head": metadata.head_ref_oid if metadata else "unknown",
        "base": metadata.base_ref_name if metadata else inputs.base_branch,
        "branch": metadata.head_ref_name if metadata else inputs.head_branch,
        "state": metadata.state if metadata else "unknown",
        "draft": metadata.is_draft if metadata else "unknown",
        "review_state": metadata.review_decision if metadata else "unknown",
        "files_modified": [],
        "no_op_justification": "Recovery stopped before evidence collection because the fixed target contract failed.",
        "diff_scope": "not evaluated",
        "local_validation": "not run: fixed target contract blocked recovery",
        "github_actions": "not collected",
        "qa_scenario_evidence": "not evaluated: fixed target contract blocked recovery",
        "docs_impact": "not evaluated: fixed target contract blocked recovery",
        "quality_audit_cycles": cycles,
        "pr_description_evidence": "not evaluated",
        "non_claims": NON_CLAIMS,
        "blockers": blockers,
    }


def recover_pr(
    inputs: RecoveryInputs,
    *,
    command_runner: CommandRunner = run_command,
    repo_root: Path | None = None,
) -> dict[str, Any]:
    root = repo_root or Path.cwd()
    LOGGER.info("Recovering PR #%s on branch %s", inputs.pr_number, inputs.head_branch)

    requested_target = validate_requested_target(inputs)
    if requested_target["blockers"]:
        return build_target_blocked_report(inputs=inputs, blockers=requested_target["blockers"])

    sync_pr_branch(inputs, command_runner, root)
    metadata = read_pr_metadata(command_runner, root, inputs.pr_number)
    metadata_target = validate_metadata_target(inputs, metadata)
    if metadata_target["blockers"]:
        return build_target_blocked_report(inputs=inputs, metadata=metadata, blockers=metadata_target["blockers"])

    local_head = read_local_head(command_runner, root)
    head_blockers = head_alignment_blockers(local_head, metadata)

    changed = read_changed_files(inputs, command_runner, root)
    paths = changed_paths(changed)
    diff_scope = analyze_diff_scope(
        changed_files=changed,
        allowed_paths=inputs.expected_diff_paths,
        design_scope=inputs.design_scope,
    )

    validation = run_focused_validation(command_runner, root)
    local_validation_blockers = validation["blockers"]
    test_adequacy = validation["test_adequacy"]

    ci = collect_ci_evidence(inputs, command_runner, root, local_head=local_head, head_before=metadata.head_ref_oid)
    qa = collect_qa_evidence(changed_files=paths, runner=command_runner, root=root)

    docs = review_docs_impact(changed_files=paths, design_scope=inputs.design_scope, behavior_changed=False)

    body = read_pr_body(command_runner, root, inputs.pr_number)
    pr_description = validate_pr_description(body)
    claims = evaluate_bounded_claims(body)
    pr_state = evaluate_pr_state(metadata, branch_policy_requires_approval=inputs.branch_policy_requires_approval)

    evidence = {
        "diff_scope": diff_scope,
        "test_adequacy": test_adequacy,
        "ci": ci,
        "qa": qa,
        "docs": docs,
        "pr_metadata": pr_description,
        "pr_state": pr_state,
        "claims": claims,
    }
    cycles = run_quality_audit_cycles(evidence)
    blockers = head_blockers + collect_blockers(diff_scope, test_adequacy, ci, qa, docs, pr_description, pr_state, claims)
    for cycle in cycles:
        blockers.extend(blocker for blocker in cycle.get("blockers", []) if blocker not in blockers)

    return build_report(
        inputs=inputs,
        metadata=metadata,
        local_head=local_head,
        validation_command=validation["command"],
        local_validation_blockers=local_validation_blockers,
        ci=ci,
        qa=qa,
        docs=docs,
        diff_scope=diff_scope,
        pr_description=pr_description,
        cycles=cycles,
        blockers=blockers,
    )


def render_report(report: dict[str, Any]) -> str:
    blockers = report["blockers"] or ["none"]
    cycles = "; ".join(f"cycle {cycle['cycle']} {cycle['status']}" for cycle in report["quality_audit_cycles"])
    return "\n".join(
        [
            f"Result: {report['result']}",
            f"PR: #{report['pr']}",
            f"Head: {report['head']}",
            f"Base: {report['base']}",
            f"Branch: {report['branch']}",
            f"State: {report['state']}",
            f"Draft: {str(report['draft']).lower()}",
            f"Review state: {report['review_state']}",
            "Files modified: " + (", ".join(report["files_modified"]) if report["files_modified"] else "none"),
            f"Diff scope: {report['diff_scope']}",
            f"Local validation: {report['local_validation']}",
            f"GitHub Actions: {report['github_actions']}",
            f"QA/scenario evidence: {report['qa_scenario_evidence']}",
            f"Docs impact: {report['docs_impact']}",
            f"Quality audit cycles: {cycles}",
            f"PR description evidence: {report['pr_description_evidence']}",
            f"Non-claims: {report['non_claims']}",
            "Blockers:",
            *[f"- {blocker}" for blocker in blockers],
        ]
    )


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pr-number", type=int, required=True)
    parser.add_argument("--head-branch", required=True)
    parser.add_argument("--base-branch", required=True)
    parser.add_argument(
        "--expected-diff-path",
        action="append",
        default=[],
        help="Allowed changed path. May be supplied more than once.",
    )
    parser.add_argument("--design-scope", default="test-only")
    parser.add_argument("--approval-required", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--json", action="store_true", help="Emit machine-readable JSON instead of text.")
    parser.add_argument("--verbose", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    logging.basicConfig(level=logging.INFO if args.verbose else logging.WARNING, format="%(levelname)s: %(message)s")
    expected_paths = set(args.expected_diff_path or [MODEL_EXPORT_TEST])
    inputs = RecoveryInputs(
        pr_number=args.pr_number,
        head_branch=args.head_branch,
        base_branch=args.base_branch,
        expected_diff_paths=expected_paths,
        design_scope=args.design_scope,
        branch_policy_requires_approval=args.approval_required,
    )
    requested_target = validate_requested_target(inputs)
    if requested_target["blockers"]:
        report = build_target_blocked_report(inputs=inputs, blockers=requested_target["blockers"])
    else:
        report = recover_pr(inputs)
    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
    else:
        print(render_report(report))
    return 0 if report["result"] == "MERGE_READY" else 1


if __name__ == "__main__":
    raise SystemExit(main())
