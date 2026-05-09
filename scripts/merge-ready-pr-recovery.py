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
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Any, Callable, Iterable


LOGGER = logging.getLogger("merge-ready-pr-recovery")

MODEL_EXPORT_TEST = "core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java"
NODE_OPTIONS = "--max-old-space-size=32768"
NON_CLAIMS = (
    "no full UI automation, visible rendering correctness, grading, creative "
    "assessment, full lesson completion, or full Tweedle/player decode claimed"
)
GREEN_CONCLUSIONS = {"SUCCESS"}


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


def require_success(result: SimpleNamespace, context: str) -> None:
    if result.returncode != 0:
        stderr = (result.stderr or "").strip()
        stdout = (result.stdout or "").strip()
        detail = stderr or stdout or f"exit code {result.returncode}"
        raise RecoveryEvidenceError(f"{context} failed: {detail}")


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

    status = "blocker" if blockers else "focused"
    return {"status": status, "changed_files": files, "allowed_paths": sorted(allowed_paths), "blockers": blockers}


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

    return {"status": "blocker" if blockers else "green", "checks": checks, "blockers": blockers}


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
    return {"status": "blocker" if blockers else "reviewable", "blockers": blockers}


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

    applicable = any(path.startswith("qa/outside-in/") or path.endswith((".yaml", ".yml")) for path in changed_files)
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
    requirements = {
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
    missing: list[str] = []
    for label, needles in requirements.items():
        if not any(needle in body for needle in needles):
            missing.append(label)
    blockers = [f"NOT_MERGE_READY: PR description missing {label} evidence." for label in missing]
    return {"status": "blocker" if blockers else "sufficient", "missing": missing, "blockers": blockers}


def evaluate_bounded_claims(body: str) -> dict[str, Any]:
    overclaims = [
        phrase
        for phrase in (
            "full UI automation passed",
            "visible rendering correctness verified",
            "grading verified",
            "creative assessment verified",
            "full lesson completion verified",
            "full Tweedle/player decode verified",
        )
        if phrase.lower() in body.lower()
    ]
    blockers = [
        f"NOT_MERGE_READY: PR description overclaims unproven behavior: {', '.join(overclaims)}."
    ] if overclaims else []
    return {"status": "blocker" if blockers else "bounded", "blockers": blockers}


def collect_blockers(*items: dict[str, Any]) -> list[str]:
    blockers: list[str] = []
    for item in items:
        blockers.extend(str(blocker) for blocker in item.get("blockers", []))
    return blockers


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
        {
            "cycle": 1,
            "SEEK": "Diff scope drift, unrelated files, binary artifacts, and generated noise.",
            "VALIDATE": "Compared the PR diff against the expected focused path set.",
            "FIX": "No fix required." if not cycle1_blockers else "Block readiness until scope is narrowed or design is updated.",
            "status": "clean" if not cycle1_blockers else "blocker",
            "blockers": cycle1_blockers,
        },
        {
            "cycle": 2,
            "SEEK": "Test adequacy gaps for the model export characterization claims.",
            "VALIDATE": "Ran focused model-loading validation and reviewed the protected behavior surface.",
            "FIX": "No fix required." if not cycle2_blockers else "Block readiness until focused validation passes.",
            "status": "clean" if not cycle2_blockers else "blocker",
            "blockers": cycle2_blockers,
        },
        {
            "cycle": 3,
            "SEEK": "Evidence completeness gaps across CI, QA, docs, PR metadata, review state, and bounded claims.",
            "VALIDATE": "Compared all collected evidence against the merge-ready recovery contract.",
            "FIX": "No fix required." if not cycle3_blockers else "Block readiness until missing evidence is supplied.",
            "status": "clean" if not cycle3_blockers else "blocker",
            "blockers": cycle3_blockers,
        },
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
    require_success(result, context)
    return result


def recover_pr(
    inputs: RecoveryInputs,
    *,
    command_runner: CommandRunner = run_command,
    repo_root: Path | None = None,
) -> dict[str, Any]:
    root = repo_root or Path.cwd()
    LOGGER.info("Recovering PR #%s on branch %s", inputs.pr_number, inputs.head_branch)

    run_and_require(
        command_runner,
        ["git", "fetch", "origin", inputs.head_branch],
        cwd=root,
        context=f"fetch origin {inputs.head_branch}",
    )
    run_and_require(command_runner, ["git", "checkout", inputs.head_branch], cwd=root, context="checkout PR branch")
    run_and_require(
        command_runner,
        ["git", "pull", "--ff-only", "origin", inputs.head_branch],
        cwd=root,
        context=f"pull origin {inputs.head_branch}",
    )

    metadata_result = run_and_require(
        command_runner,
        [
            "gh",
            "pr",
            "view",
            str(inputs.pr_number),
            "--json",
            "number,state,isDraft,reviewDecision,baseRefName,headRefName,headRefOid,title,url",
        ],
        cwd=root,
        context="read PR metadata",
    )
    metadata = parse_pr_metadata(metadata_result.stdout)

    local_head_result = run_and_require(
        command_runner,
        ["git", "rev-parse", "HEAD"],
        cwd=root,
        context="read local HEAD",
    )
    local_head = local_head_result.stdout.strip()
    head_blockers: list[str] = []
    try:
        validate_head_alignment(local_head, metadata)
    except RecoveryEvidenceError as exc:
        head_blockers.append(f"NOT_MERGE_READY: {exc}")

    run_and_require(
        command_runner,
        ["git", "fetch", "origin", inputs.base_branch],
        cwd=root,
        context=f"fetch origin {inputs.base_branch}",
    )
    diff_result = run_and_require(
        command_runner,
        ["git", "diff", "--name-status", f"origin/{inputs.base_branch}...HEAD"],
        cwd=root,
        context="read PR diff scope",
    )
    changed = parse_name_status(diff_result.stdout)
    paths = changed_paths(changed)
    diff_scope = analyze_diff_scope(
        changed_files=changed,
        allowed_paths=inputs.expected_diff_paths,
        design_scope=inputs.design_scope,
    )

    run_and_require(
        command_runner,
        ["git", "submodule", "update", "--init", "tweedle-lang"],
        cwd=root,
        context="initialize tweedle-lang submodule",
    )
    validation = model_export_validation_command()
    validation_env = {**os.environ, **validation["env"]}
    validation_result = command_runner(validation["command"], cwd=root, env=validation_env)
    local_validation_blockers: list[str] = []
    if validation_result.returncode != 0:
        detail = (validation_result.stderr or validation_result.stdout or "").strip()
        local_validation_blockers.append(
            "NOT_MERGE_READY: Focused local validation failed"
            + (f": {detail}" if detail else f" with exit code {validation_result.returncode}")
        )
    test_adequacy = {"status": "sufficient" if not local_validation_blockers else "blocker", "blockers": local_validation_blockers}

    head_before_result = run_and_require(
        command_runner,
        ["gh", "pr", "view", str(inputs.pr_number), "--json", "headRefOid"],
        cwd=root,
        context="read PR head before checks",
    )
    checks_result = run_and_require(
        command_runner,
        ["gh", "pr", "checks", str(inputs.pr_number), "--json", "name,state,conclusion,link"],
        cwd=root,
        context="read PR checks",
    )
    head_after_result = run_and_require(
        command_runner,
        ["gh", "pr", "view", str(inputs.pr_number), "--json", "headRefOid"],
        cwd=root,
        context="read PR head after checks",
    )
    checks = parse_json_list(checks_result.stdout, "gh pr checks")
    head_before = parse_json_object(head_before_result.stdout, "PR head before checks")["headRefOid"]
    head_after = parse_json_object(head_after_result.stdout, "PR head after checks")["headRefOid"]
    ci = evaluate_checks(checks=checks, head_before=str(head_before), head_after=str(head_after), local_head=local_head)

    find_result = command_runner(["find", "qa/outside-in", "-maxdepth", "4", "-type", "f"], cwd=root, env=None)
    discovered_paths = find_result.stdout.splitlines() if find_result.returncode == 0 else []
    grep_result = command_runner(
        ["grep", "-R", "export\\|model", "-n", "qa/outside-in", "docs/reference", "docs/howto"],
        cwd=root,
        env=None,
    )
    if grep_result.returncode == 0:
        discovered_paths.extend(line.split(":", 1)[0] for line in grep_result.stdout.splitlines() if ":" in line)
    qa = classify_qa_evidence(
        changed_files=paths,
        discovered_paths=sorted(set(discovered_paths)),
        executed_evidence=[],
        ci_evidence=[],
    )

    docs = review_docs_impact(changed_files=paths, design_scope=inputs.design_scope, behavior_changed=False)

    body_result = run_and_require(
        command_runner,
        ["gh", "pr", "view", str(inputs.pr_number), "--json", "body"],
        cwd=root,
        context="read PR body",
    )
    body = str(parse_json_object(body_result.stdout, "PR body").get("body") or "")
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

    result = "MERGE_READY" if not blockers and cycles[-1]["status"] == "clean" else "NOT_MERGE_READY"
    command_text = " ".join(validation["command"])
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
            f"NODE_OPTIONS={NODE_OPTIONS} {command_text}: "
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
    report = recover_pr(
        RecoveryInputs(
            pr_number=args.pr_number,
            head_branch=args.head_branch,
            base_branch=args.base_branch,
            expected_diff_paths=expected_paths,
            design_scope=args.design_scope,
            branch_policy_requires_approval=args.approval_required,
        )
    )
    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
    else:
        print(render_report(report))
    return 0 if report["result"] == "MERGE_READY" else 1


if __name__ == "__main__":
    raise SystemExit(main())
