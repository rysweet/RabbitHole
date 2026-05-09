# Recover a pull request with no-timeout default workflow

Use this guide to recover an existing pull request branch without rewriting
history, without manually merging the pull request, and without outer
command-level timeout wrappers. The output is either `MERGE_READY` with
complete evidence or `NOT_MERGE_READY` with explicit blockers.

Implementation ownership: `scripts/default_workflow_recovery.py` provides the
report rendering, validation helpers, and read-only GitHub `gh` service adapter
used by this guide.

For the full report contract, see the [Default workflow recovery report
reference](../reference/default-workflow-recovery-report.md).

## Before you start

Run commands from the repository root of the pull request worktree:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Do not wrap recovery evidence commands with `timeout`, `gtimeout`, alarm
scripts, or similar outer command-level timeout shims. Scenario-level timeout
fields and internal QA runner guards remain valid when they are part of the
existing scenario contract.

## Verify the PR head

Fetch the branch and compare local `HEAD` to the current pull request head:

```bash
pr_number=404
head_branch=wave6-run-execution-gap-1778302300

git fetch origin "$head_branch"
git switch "$head_branch"

local_head=$(git rev-parse HEAD)
remote_head=$(git rev-parse "origin/$head_branch")
pr_head=$(gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json headRefOid \
  --jq .headRefOid)

test "$local_head" = "$remote_head"
test "$local_head" = "$pr_head"
```

If either SHA comparison fails, stop and report `NOT_MERGE_READY` with a
current-head mismatch blocker. Local validation from a stale head is not
merge-ready evidence.

## Recover conflicts against current develop

When GitHub marks the pull request dirty or conflicting, recover the pull
request branch against the current `origin/develop` head. Do not merge the pull
request into `develop`, do not push to `develop`, and do not use a web/manual
merge as evidence.

```bash
base_branch=develop
git fetch origin "$base_branch"
base_head=$(git rev-parse "origin/$base_branch")
git status --short --branch
```

If the recovery task names an expected develop SHA, verify it before changing
the branch:

```bash
test "$base_head" = "$expected_develop_head"
```

Then update only the pull request branch with the current base and resolve
conflicts in the working tree:

```bash
git merge --no-ff "origin/$base_branch"
```

If conflicts appear, keep the resolution surgical:

1. Prefer current `origin/develop` in shared documentation, scenario, and QA
   contract files unless that would remove the pull request's bounded evidence
   contract.
2. Reapply only the PR-scoped Run-window/debug evidence wording and validation
   hooks needed by the branch.
3. Preserve explicit non-claims: no full world execution, playback, visible
   rendering correctness, full UI automation, Save completion, grading, Sims
   validation, deployed installer success, or broad UI automation.
4. Do not resolve conflicts by adding unrelated cleanup, new scenarios, or broad
   desktop automation claims.

After resolving conflicts, confirm the branch has no merge state left and record
the exact base head used:

```bash
git status --short --branch
git rev-parse "origin/$base_branch"
```

If `git merge --no-ff "origin/$base_branch"` reports the branch is already up to
date and no files change, continue through the no-op evidence path instead of
creating a synthetic change.

## Collect GitHub and mergeability evidence

Collect read-only pull request metadata:

```bash
gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json number,title,headRefName,headRefOid,baseRefName,mergeStateStatus,mergeable,state,isDraft,reviewDecision,statusCheckRollup,url
```

Collect workflow runs for the exact head:

```bash
gh run list \
  --repo rysweet/RabbitHole \
  --branch "$head_branch" \
  --commit "$local_head" \
  --json databaseId,name,status,conclusion,headSha,url
```

Green checks and completed workflows are required, but they are not sufficient.
Continue through the remaining gates before using `MERGE_READY`.

The helper's GitHub adapter can collect this same read-only metadata. Adapter
failures, stale workflow heads, missing required workflows, or conflicting
mergeability must be recorded as `NOT_MERGE_READY` blockers.

## Inspect focused diff scope

Review the base-to-head diff:

```bash
git fetch origin develop
git diff --name-status "origin/develop...HEAD"
git diff --stat "origin/develop...HEAD"
```

The diff must stay within the recovery's intended QA, tests, docs, or feature
contract scope. If unrelated files appear, report `NOT_MERGE_READY` until they
are explained, removed, or separately validated.

## Run focused QA evidence

Run only existing focused validation that matches the pull request scope. For
desktop Run execution gap recovery, use:

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

If the PR changes a different QA lane, run that lane's focused contract instead.
Do not claim full UI automation, visible rendering correctness, full world
execution, grading, creative assessment, full lesson completion, Save
completion, Sims validation, deployed installer success, or full Tweedle/player
decode unless a runnable test directly proves that claim.

## Review docs impact and PR evidence

Check whether the diff changes docs or requires docs updates:

```bash
git diff --name-only "origin/develop...HEAD" -- docs
```

Read the pull request body:

```bash
gh pr view "$pr_number" \
  --repo rysweet/RabbitHole \
  --json body \
  --jq .body
```

The PR description must match the runnable evidence. Replace unsupported
readiness wording with bounded evidence language, or report
`NOT_MERGE_READY` with a PR-description blocker.

## Record three quality-audit cycles

Document at least three cycles in the recovery output:

```text
1. SEEK: Verify current-head identity.
   VALIDATE: Compare git rev-parse HEAD with gh pr view headRefOid.
   FIX: No-op when the SHAs match; blocker when they do not.
2. SEEK: Verify runnable QA and bounded claims.
   VALIDATE: Run focused QA and review docs or PR wording.
   FIX: Update docs or PR wording, accept a verified no-op, or record blocker.
3. SEEK: Verify final merge-ready gate.
   VALIDATE: Recheck clean worktree, green current-head Actions, focused diff,
   docs impact, PR evidence, and previous blockers.
   FIX: Clean final cycle or explicit NOT_MERGE_READY blocker.
```

The final cycle must be clean before `MERGE_READY`.

## Emit the recovery decision

Use this report skeleton:

```text
Summary
<current-head recovery outcome>

Files modified
<relative files or None>

Validation
<commands actually run and results>

QA / scenario evidence
<runnable evidence and bounded supporting artifacts>

Docs impact
<docs reviewed, docs changed, or no-docs-impact justification>

Scope / bounded claims
<focused scope and unsupported claim categories>

GitHub and PR evidence
<HEAD SHA, PR head SHA, mergeability, check and workflow evidence>

Quality-audit cycles
<three SEEK / VALIDATE / FIX cycles>

Readiness decision
MERGE_READY
```

When any gate is missing, use:

```text
Readiness decision
NOT_MERGE_READY

Blockers
- <explicit blocker>
```

## No-op recovery

A no-op recovery is valid only when the report ties the clean worktree to the
current PR head, the current `origin/develop` head, and the evidence reviewed.
Use:

```text
Files modified
None
```

The no-op justification must mention current-head identity, current checks,
current develop head, focused QA evidence, docs impact, PR description evidence,
quality-audit cycles, preserved bounded Run-window/debug scope, and either a
clean `MERGE_READY` decision or explicit `NOT_MERGE_READY` blockers.
