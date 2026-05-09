# Tutorial: Trace PR #430 No-Op Finalization

This tutorial walks through a clean PR #430 finalization where no code, docs, or
push changes are required. The goal is to learn how the merge-ready gate ties
the current PR head, green checks, clean tree, and Save negative artifact
evidence into one bounded no-op decision.

The tutorial does not teach Save implementation work. It keeps the claim limited
to invalid Save proof artifacts failing closed.

Steps 1 through 3 are manual preflight checks. The gate in step 5 evaluates the
evidence JSON and refreshed GitHub PR diff/check/description data; it does not
read local `HEAD`, run `git status`, or verify the PR draft/merge state.

## 1. Start from the PR head

Fetch the PR head and compare it to the current checkout:

```bash
git fetch origin pull/430/head:refs/remotes/origin/pr/430
local_head=$(git rev-parse HEAD)
pr_head=$(git rev-parse refs/remotes/origin/pr/430)
test "$local_head" = "$pr_head"
```

The comparison must succeed. A mismatch means the checkout is not evaluating the
current PR head.

## 2. Check for local changes

```bash
git status --short
```

For a no-op finalization, this prints nothing. If it prints paths, the result is
not a no-op unless each path is an intentional focused PR #430 recovery change.

## 3. Read current GitHub state

```bash
gh pr view 430 --json number,state,isDraft,mergeStateStatus,headRefName,headRefOid,statusCheckRollup
```

Confirm the response describes PR #430, the branch
`feat/issue-407-rabbithole-wave7-save-negative-contract-lane-follo`, an open
non-draft PR, a clean merge state, and completed green checks for the same
`headRefOid` as the local checkout.

## 4. Keep the QA claim narrow

The focused Save evidence is the negative artifact contract:

```bash
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

Passing output means bad artifacts are rejected with diagnostics. It does not
mean Alice launched, a project was saved, a `.a3p` file was read back, or a
visual workflow completed.

## 5. Evaluate the evidence

Run the programmatic gate:

```bash
python3 scripts/pr430_merge_ready_gate.py --evidence pr430-evidence.json --refresh-github
```

A clean no-op finalization returns `MERGE_READY`, an empty `blockers` array, and
an empty `files_modified` array with `no_op_justification`. If `files_modified`
is non-empty, the result may still be merge-ready, but it is not a no-op. The
`files_modified` value comes from the evidence document, not from local
`git status`.

## 6. Write the final no-op result

Use a no-op result only after all previous checks agree:

```text
No-op: local HEAD matches the current PR #430 head, the working tree is clean,
GitHub checks for that head are complete and green, and scoped Save negative
artifact evidence shows invalid Save proof artifacts fail closed. No code,
documentation, test, or push changes are required.
```

That statement is intentionally specific. It ties the decision to current head
identity, clean tree state, current-head green checks, and the scoped negative
artifact contract. It does not claim broader Save behavior or broader QA
coverage.

## What a blocker looks like

This result is not merge-ready:

```json
{
  "blockers": [
    "Evaluated head SHA is stale: it must match the current remote PR head SHA."
  ],
  "files_modified": [],
  "ready": false,
  "status": "NOT_MERGE_READY"
}
```

Resolve only the named blocker. Do not add broad Save changes, broad QA, or
unrelated documentation while recovering PR #430.

## Related documentation

- [Finalize PR #430 Recovery](../howto/finalize-pr430-recovery.md)
- [PR #430 Merge-Ready Gate](../reference/pr430-merge-ready-gate.md)
- [Save Menu Dialog Negative Artifact Contract](../reference/save-menu-dialog-negative-artifact-contract.md)
