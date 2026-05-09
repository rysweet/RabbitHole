# Finalize PR #430 Recovery

Use this guide to finalize RabbitHole PR #430 after a clean, owner-free
workflow exit. The process confirms that the current checkout matches the PR
head, the working tree is clean or intentionally scoped, GitHub checks are
green, and the Save negative artifact contract remains limited to invalid
artifact rejection.

This guide is only for PR #430. It is not a general Save workflow guide and it
does not prove desktop Save completion.

## Prerequisites

Run commands from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

The `gh` CLI must be authenticated for read-only PR and check evidence.

## Confirm the checked-out head

Fetch the PR head and compare it to `HEAD`:

```bash
git fetch origin pull/430/head:refs/remotes/origin/pr/430
git rev-parse HEAD
git rev-parse refs/remotes/origin/pr/430
```

The two SHAs must match before finalization can continue. If they differ, check
out the current PR head or stop and refresh the recovery evidence.

## Confirm the working tree scope

Check the tree:

```bash
git status --short
```

An empty result means the tree is clean. If files are listed, each file must be
an intentional PR #430 recovery change inside the focused surface documented in
[PR #430 Merge-Ready Gate](../reference/pr430-merge-ready-gate.md#focused-diff-scope).

Do not include unrelated cleanup, broad Save behavior changes, generated status
reports, or files outside the Save negative artifact contract lane.

## Confirm PR state and checks

Refresh current PR evidence:

```bash
gh pr view 430 --json number,state,isDraft,mergeStateStatus,headRefName,headRefOid,statusCheckRollup
```

The PR is ready for gate evaluation only when:

1. `state` is `OPEN`.
2. `isDraft` is `false`.
3. `mergeStateStatus` is `CLEAN`.
4. `headRefOid` matches local `HEAD`.
5. Every current-head check is complete and green.

Treat missing, stale, pending, failed, cancelled, timed-out, or ambiguous check
evidence as a blocker.

## Run focused QA only when needed

Use existing evidence when it is current for the checked-out PR head and covers
the focused Save negative artifact contract. Rerun only the focused commands
when evidence is stale, missing, affected by a recovery edit, or not tied to the
current head:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

The negative artifact command proves only that invalid Save proof artifacts fail
closed with explicit diagnostics. Do not cite it as positive Save
write/readback evidence.

## Run the merge-ready gate

Evaluate the evidence document and refresh GitHub state:

```bash
python3 scripts/pr430_merge_ready_gate.py --evidence pr430-evidence.json --refresh-github
```

The gate must return:

```json
{
  "blockers": [],
  "files_modified": [],
  "ready": true,
  "status": "MERGE_READY"
}
```

If `blockers` is non-empty, make only the narrowest focused recovery change,
rerun the affected focused validation, and push only that scoped fix.

## Use the no-op finalization form

When the gate is merge-ready, the working tree is clean, checks are green for
the current head, and no push is required, use this exact meaning in the final
result:

```text
No-op: local HEAD matches the current PR #430 head, the working tree is clean,
GitHub checks for that head are complete and green, and scoped Save negative
artifact evidence shows invalid Save proof artifacts fail closed. No code,
documentation, test, or push changes are required.
```

Do not use the no-op form if any evidence is stale, missing, pending,
out-of-scope, or broader than invalid Save proof artifact rejection.

## Push only focused recovery changes

Push only when a blocker requires a narrow recovery patch:

```bash
git status --short
git diff --name-only
git push origin HEAD
```

Before pushing, confirm that every changed path is in the focused diff surface,
that the affected focused QA passed, and that the PR description remains bounded
to the Save negative artifact contract.

## Related documentation

- [PR #430 Merge-Ready Gate](../reference/pr430-merge-ready-gate.md)
- [Save Menu Dialog Negative Artifact Contract](../reference/save-menu-dialog-negative-artifact-contract.md)
- [Run the Save Menu Dialog Negative Artifact Contract](./run-save-menu-dialog-negative-artifact-contract.md)
