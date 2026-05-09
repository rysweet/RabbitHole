# PR #430 Merge-Ready Gate

This reference documents the merge-readiness gate for RabbitHole PR #430. The
gate evaluates PR #430 without merging it and returns merge-ready only when the
checked-out head, GitHub PR state, GitHub checks, focused QA evidence, and
bounded documentation claims all agree.

The gate is intentionally narrow. It protects the Save menu dialog negative
artifact contract: invalid Save proof artifacts must fail closed with explicit
diagnostics. It does not expand Save behavior, Save As behavior, visible
rendering correctness, grading, lesson completion, learner assessment, broad UI
automation, native dialog automation, or player/Tweedle decode claims.

## Command

Run the gate from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
python3 scripts/pr430_merge_ready_gate.py --evidence pr430-evidence.json --refresh-github
```

`--refresh-github` reads current PR metadata and check status through the
authenticated `gh` CLI. The command does not merge, push, edit the PR, or modify
the working tree.

## Exit codes

| Exit code | Meaning |
| --- | --- |
| `0` | Every merge-ready gate passed. The JSON result has `status: "MERGE_READY"` and `ready: true`. |
| `1` | Evidence was valid JSON, but at least one merge-ready gate failed. The JSON result has `status: "NOT_MERGE_READY"` and blocker messages. |
| `2` | The evidence file could not be read, was malformed, or current GitHub evidence could not be collected. The command prints the error to stderr and does not emit success-shaped JSON. |

## Required evidence

The input evidence file is a JSON object. The gate accepts only evidence for PR
#430 and the authoritative PR branch:

```text
feat/issue-407-rabbithole-wave7-save-negative-contract-lane-follo
```

The evidence must prove all of the following:

1. The evaluated local head SHA exactly matches the current PR `headRefOid`.
2. PR #430 is open, non-draft, and has a clean merge state.
3. Manual merge evidence is absent.
4. The working tree is clean or contains only intentional PR #430 recovery changes.
5. Changed files stay inside the focused Save negative artifact contract surface.
6. GitHub checks for the current PR head are complete and green.
7. Focused runnable QA evidence is current for the evaluated head.
8. Documentation impact is assessed and claim wording stays bounded.
9. The PR description cites current-head evidence, docs impact, quality-audit cycles, green checks, and non-claims.
10. No owner handoff, ambiguous blocker, or pending workflow exit remains.

## Focused diff scope

The gate accepts only repo-relative, normalized paths inside the PR #430 recovery
surface:

| Path | Accepted purpose |
| --- | --- |
| `alice_qa_amplihack.py` | Branch-installable QA wrapper for the negative contract. |
| `docs/howto/` | Usage documentation for the scoped contract and recovery workflow. |
| `docs/reference/` | Reference documentation for artifact and merge-ready contracts. |
| `docs/tutorials/` | Tutorials that teach the scoped contract without broadening claims. |
| `docs/index.md` | Navigation entry for documentation discoverability. |
| `pyproject.toml` | Packaging metadata required by the QA wrapper. |
| `qa/outside-in/alice-desktop/` | Save negative artifact contract scripts, fixtures, and QA docs. |
| `scripts/pr430_merge_ready_gate.py` | Programmatic merge-ready gate. |
| `tests/` | Unit or contract tests for the gate and wrapper. |

Absolute paths, parent-directory traversal, empty path segments, Windows
separators, NUL bytes, and paths outside this list fail the scope gate.

## Required focused QA

The merge-ready evidence includes these passed commands:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

The negative artifact command is the only Save-specific QA required by this
gate unless a focused recovery change touches another validated surface. The
negative command proves only that invalid Save proof artifacts fail closed. It is
not positive Save write/readback evidence.

## GitHub checks

GitHub check evidence is current only when it is tied to the PR `headRefOid` that
matches the evaluated local head. Each check must be complete with one of these
accepted conclusions:

```text
success
skipped
neutral
```

Pending, queued, in-progress, expected, failed, cancelled, timed-out, missing,
or stale checks block merge readiness.

## No-op contract

When no code or documentation changes are required, a no-op result is valid only
when the evidence includes an explicit accepted justification. The justification
must mention both the current remote PR head and the merge-ready gates.

Use this form for the final response after the gate returns `MERGE_READY`, the
working tree is clean, and no push is required:

```text
No-op: local HEAD matches the current PR #430 head, the working tree is clean,
GitHub checks for that head are complete and green, and scoped Save negative
artifact evidence shows invalid Save proof artifacts fail closed. No code,
documentation, test, or push changes are required.
```

Do not use the no-op form when evidence is stale, checks are pending, the local
head differs from the PR head, the working tree contains unrelated changes, or
the Save negative evidence is missing or broader than invalid artifact
rejection.

## Output API

The gate prints a JSON object.

### Merge-ready result

```json
{
  "blockers": [],
  "files_modified": [],
  "no_op_justification": "Current remote PR head was evaluated and all merge-ready gates have evidence.",
  "ready": true,
  "status": "MERGE_READY"
}
```

`no_op_justification` appears only when the gate is ready and the evidence says
no files were modified.

### Blocked result

```json
{
  "blockers": [
    "Evaluated head SHA is stale: it must match the current remote PR head SHA.",
    "GitHub Actions checks must be complete and green: required gate (in_progress/missing conclusion)"
  ],
  "files_modified": [
    "docs/reference/pr430-merge-ready-gate.md"
  ],
  "ready": false,
  "status": "NOT_MERGE_READY"
}
```

Blocker messages are actionable. A blocked result is not merge-ready even if
some individual checks or local QA commands passed.

## Evidence schema

This is the minimal evidence shape. `--refresh-github` fills or replaces the
current PR, changed-file, check, and PR-description fields from GitHub.

```json
{
  "pr": {
    "number": 430,
    "branch": "feat/issue-407-rabbithole-wave7-save-negative-contract-lane-follo",
    "remote_head_sha": "0123456789abcdef0123456789abcdef01234567",
    "evaluated_head_sha": "0123456789abcdef0123456789abcdef01234567",
    "state": "OPEN",
    "is_draft": false,
    "merge_state_status": "CLEAN",
    "manual_merge": false
  },
  "workflow": {
    "owner_exit_classification": "NO_OP_GUARD",
    "no_timeout_wrappers": true,
    "owner_handoff_remaining": false
  },
  "diff": {
    "files_modified": [],
    "changed_files": [
      "docs/reference/save-menu-dialog-negative-artifact-contract.md"
    ]
  },
  "qa": {
    "runnable_evidence": [
      {
        "command": "NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests",
        "status": "passed"
      },
      {
        "command": "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
        "status": "passed"
      }
    ],
    "scenario_evidence": {
      "applicable": true,
      "command": "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
      "status": "passed",
      "claim": "validator rejects invalid Save proof evidence only"
    }
  },
  "docs": {
    "impact_assessed": true,
    "bounded_language": true,
    "unsupported_claims": []
  },
  "quality_audit_cycles": [
    {
      "cycle": 1,
      "seek": "contract behavior",
      "validate": "negative fixtures and validator diagnostics",
      "fix": "no actionable issue",
      "status": "clean"
    },
    {
      "cycle": 2,
      "seek": "evidence wording and documentation claims",
      "validate": "bounded language and non-claims",
      "fix": "no actionable issue",
      "status": "clean"
    },
    {
      "cycle": 3,
      "seek": "full diff and readiness state",
      "validate": "no unresolved blockers",
      "fix": "no actionable issue",
      "status": "clean"
    }
  ],
  "github_actions": {
    "head_sha": "0123456789abcdef0123456789abcdef01234567",
    "checks": [
      {
        "name": "python maintenance tests",
        "status": "completed",
        "conclusion": "success"
      }
    ]
  },
  "pr_description": {
    "head_sha": "0123456789abcdef0123456789abcdef01234567",
    "contains_current_evidence": true,
    "mentions_docs_impact": true,
    "mentions_three_audit_cycles": true,
    "mentions_green_actions": true,
    "bounded_non_claims": true
  },
  "claims": [
    "negative contract evidence proves invalid Save proof artifacts fail closed"
  ],
  "no_op": {
    "accepted": true,
    "justification": "Current remote PR head was evaluated and all merge-ready gates have evidence."
  }
}
```

Replace the example SHA values with the current PR head SHA before running the
gate without `--refresh-github`.

## Related documentation

- [Run PR #430 recovery finalization](../howto/finalize-pr430-recovery.md)
- [Save Menu Dialog Negative Artifact Contract](./save-menu-dialog-negative-artifact-contract.md)
- [Run the Save Menu Dialog Negative Artifact Contract](../howto/run-save-menu-dialog-negative-artifact-contract.md)
