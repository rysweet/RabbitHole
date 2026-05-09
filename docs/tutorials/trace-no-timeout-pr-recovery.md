# Tutorial: Trace no-timeout pull request recovery

This tutorial walks through a recovery review for an existing pull request
branch. It uses PR `#404` and branch
`wave6-run-execution-gap-1778302300` as the concrete example, but the same gates
apply to the no-timeout default-workflow recovery extension.

## What you will do

1. Tie local evidence to the current pull request head.
2. Collect GitHub checks and mergeability for that head.
3. Review focused diff scope.
4. Run runnable QA evidence without outer command-level timeout wrappers.
5. Review docs impact and pull request wording.
6. Record three quality-audit cycles.
7. Emit `MERGE_READY` only when every gate passes.

## Step 1: Prepare the worktree

Start at the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

The Tweedle grammar check prevents broad Maven validation from failing before
the recovery reaches the pull request evidence.

## Step 2: Tie evidence to the current PR head

Fetch and switch to the pull request branch:

```bash
pr_number=404
head_branch=wave6-run-execution-gap-1778302300

git fetch origin "$head_branch"
git switch "$head_branch"
```

Compare the local head with the remote branch and GitHub's PR head:

```bash
git rev-parse HEAD
git rev-parse "origin/$head_branch"

gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json headRefOid \
  --jq .headRefOid
```

All three SHAs must match exactly. If they do not, the tutorial stops with
`NOT_MERGE_READY`; none of the later local commands can be used as current-head
evidence.

## Step 3: Read PR state and workflow evidence

Inspect the pull request state:

```bash
gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json number,headRefName,headRefOid,baseRefName,mergeStateStatus,mergeable,state,isDraft,reviewDecision,statusCheckRollup,url
```

Inspect workflow runs for the exact head:

```bash
head_sha=$(git rev-parse HEAD)

gh run list \
  --repo rysweet/RabbitHole \
  --branch "$head_branch" \
  --commit "$head_sha" \
  --json databaseId,name,status,conclusion,headSha,url
```

Completed green Actions satisfy only the CI gate. They do not replace focused
QA, docs review, diff-scope review, PR-description review, or quality-audit
cycles.

The implemented helper can collect the PR and workflow payloads through its
read-only GitHub `gh` service adapter. If the adapter cannot fetch current
metadata after retrying transient failures, the recovery stays fail-closed with
a `NOT_MERGE_READY` blocker.

## Step 4: Review diff scope

Review changed files against the target branch:

```bash
git fetch origin develop
git diff --name-status "origin/develop...HEAD"
```

For a desktop Run execution gap recovery, expected changes stay near the Run
evidence implementation, QA scenario contracts, tests, and documentation. A
file outside the intended scope is not automatically wrong, but it needs direct
evidence. If it cannot be explained and validated, record a
`NOT_MERGE_READY` blocker.

## Step 5: Run bounded QA evidence

Run focused commands directly:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh

NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

Do not add an outer timeout wrapper to these commands. The recovery evidence is
the direct command result. Existing scenario-level timeout fields and internal
QA runner guards remain part of the scenario contract.

The accepted claim is narrow: bounded Run-window evidence and an explicit
deterministic world-advance blocker. The result does not prove full UI
automation, visible rendering correctness, grading, creative assessment, full
lesson completion, full world execution, playback, Save completion, Sims
validation, deployed installer success, or full Tweedle/player decode.

## Step 6: Review docs and PR wording

Check docs touched by the diff:

```bash
git diff --name-only "origin/develop...HEAD" -- docs
```

Read the PR body:

```bash
gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json body \
  --jq .body
```

The wording should say what the evidence proves and what remains blocked. If the
text says or implies more than the runnable evidence proves, the recovery report
must either record the wording fix or emit `NOT_MERGE_READY`.

## Step 7: Write three quality-audit cycles

Trace the recovery with three cycles:

```text
1. SEEK: Current-head evidence drift.
   VALIDATE: Compare local HEAD, PR headRefOid, and workflow headSha.
   FIX: No-op if all match; otherwise NOT_MERGE_READY.
2. SEEK: Unsupported desktop Run claims.
   VALIDATE: Review focused QA output, gap report contract, docs, and PR body.
   FIX: No-op only when all wording stays bounded.
3. SEEK: Final merge-ready completeness.
   VALIDATE: Recheck green current-head Actions, clean worktree, mergeability,
   focused diff, runnable QA, docs impact, PR evidence, and blocker list.
   FIX: Clean final cycle or explicit NOT_MERGE_READY blockers.
```

The third cycle is the final gate. If it finds a blocker, the decision is
`NOT_MERGE_READY`.

## Step 8: Emit the decision

Use `MERGE_READY` only when every gate is clean:

```text
Readiness decision
MERGE_READY
```

Otherwise, keep the decision explicit:

```text
Readiness decision
NOT_MERGE_READY

Blockers
- GitHub Actions are still in progress for the current PR head.
- PR body overclaims full UI automation.
- Runnable QA evidence was not run for the changed scenario contract.
```

The blocker list is part of the successful recovery output. It prevents a
partial or no-op recovery from being mistaken for merge approval.

For the task-oriented checklist, see [Recover a pull request with no-timeout
default workflow](../howto/recover-pr-with-default-workflow.md). For the full
field contract, see the [Default workflow recovery report
reference](../reference/default-workflow-recovery-report.md).
