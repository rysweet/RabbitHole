# Finalize a Source-Code-Generator Pull Request

Use this guide to finalize a source-code-generator pull request when the
implementation already exists on the review branch and the remaining work is
current-head readiness, review evidence, and a merge-ready or no-op decision.

For the stable behavior contract, see
[Generated Story API and AST Source Characterization](../reference/generated-story-api-listener-source-characterization.md).
For the focused characterization workflow, see
[Characterize Source-Code-Generator Behavior](./characterize-source-code-generator.md).

## Contents

- [Before you start](#before-you-start)
- [Inputs and public API](#inputs-and-public-api)
- [Refresh GitHub pull request evidence](#refresh-github-pull-request-evidence)
- [Check the local worktree](#check-the-local-worktree)
- [Audit the source-code-generator scope](#audit-the-source-code-generator-scope)
- [Validate only when needed](#validate-only-when-needed)
- [Record review evidence](#record-review-evidence)
- [Finalize with a no-op decision](#finalize-with-a-no-op-decision)
- [Push only focused fixes](#push-only-focused-fixes)
- [Troubleshooting](#troubleshooting)

## Before you start

Run commands from the repository root. Use read-only GitHub and Git commands
until a focused source-code-generator issue is confirmed.

Set the pull request inputs for the branch you are finalizing:

```bash
REPO=rysweet/RabbitHole
PR_NUMBER="<pull-request-number>"
BASE_BRANCH="<baseRefName>"
```

Initialize the Tweedle grammar submodule before Maven validation in a fresh
checkout:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Do not wrap validation commands in a shell timeout. The finalization workflow
uses the normal Maven command and waits for it to complete.

## Inputs and public API

Source-code-generator finalization has no new Java API. Its stable surface is
the review procedure, the existing source-code-generator characterization tests,
and the GitHub pull request metadata used to make a merge-ready or no-op
decision.

| Input | Required value | Purpose |
| --- | --- | --- |
| `REPO` | `rysweet/RabbitHole` | Authoritative repository for PR metadata. |
| `PR_NUMBER` | Active source-code-generator pull request number | Authoritative review target. |
| `BASE_BRANCH` | The PR `baseRefName` | Diff-scope comparison point. |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Standard local validation memory setting. |
| `tweedle-lang/Grammar` | Present before Maven validation | Required Maven prerequisite for generated Tweedle parser inputs. |
| Focused test selector | `SourceCodeGeneratorTest` | Keeps validation scoped to `core/ast` source generation. |

The workflow uses existing commands: `gh pr view`, `gh pr checks`, `git`, and
the focused Maven source-code-generator test command.

## Refresh GitHub pull request evidence

Read the current GitHub state before making a final decision:

```bash
gh pr view "$PR_NUMBER" --repo "$REPO" \
  --json number,state,isDraft,headRefName,headRefOid,baseRefName,mergeStateStatus,reviewDecision,statusCheckRollup,latestReviews,url

gh pr checks "$PR_NUMBER" --repo "$REPO" --watch
gh pr checks "$PR_NUMBER" --repo "$REPO" --required
```

Accept the pull request metadata only when:

| Evidence | Required result |
| --- | --- |
| PR state | `OPEN`. |
| Draft state | `isDraft` is `false`. |
| Base branch | `baseRefName` matches `BASE_BRANCH`. |
| Current head | `headRefOid` is recorded and used for every later check. |
| Merge state | `mergeStateStatus` is clean or otherwise mergeable according to GitHub. |
| Visible status rollup | `statusCheckRollup` has no failed, pending, stale, or missing visible checks for the same `headRefOid`. |
| Branch-protection required checks | Required checks reported by `gh pr checks --required` or branch policy are successful for the same `headRefOid`, or branch policy confirms none are required. |
| Review decision | Record the value exactly. `REVIEW_REQUIRED` or an empty approval state is not a blocker by itself when branch policy allows owner-free merge readiness. |

Do not treat the visible status rollup as proof of branch-protection
requirements by itself. The status rollup answers "what checks are visible on
this PR head"; required-check configuration answers "which checks must pass
before merge." Record both when they differ.

Do not rely on stale local evidence or a previously recorded commit SHA. If a
new commit appears during finalization, refresh the pull request metadata and
repeat the final evidence review for the new `headRefOid`.

## Check the local worktree

Inspect local state without destructive cleanup:

```bash
git --no-pager status --short --branch
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
```

The worktree is acceptable when no unrelated dirty changes are mixed into the
finalization. If unrelated files are dirty, leave them untouched and do not
include them in any source-code-generator fix or push.

Local `HEAD` does not replace GitHub evidence. The authoritative commit for
merge readiness is the PR `headRefOid` returned by `gh pr view`.

## Audit the source-code-generator scope

Review the current pull request diff against the base branch:

```bash
git fetch origin "$BASE_BRANCH" --no-tags
git --no-pager diff --name-status "origin/$BASE_BRANCH"...HEAD
git --no-pager diff --stat "origin/$BASE_BRANCH"...HEAD
```

The finalization remains in scope when the diff is limited to
source-code-generator characterization under `core/ast`, especially:

```text
core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java
```

Implementation files are in scope only when a focused characterization proves a
real generator defect:

```text
core/ast/src/main/java/org/lgna/project/ast/SourceCodeGenerator.java
core/ast/src/main/java/org/lgna/project/ast/JavaCodeGenerator.java
```

Treat these as out of scope for source-code-generator finalization unless a
separate change explicitly owns them:

- desktop QA scenarios, runners, schemas, and evidence artifacts;
- NetBeans project generator behavior;
- Save, Save As, export, load, or recovery behavior;
- broad Tweedle/player decode behavior;
- visible rendering, grading, assessment, or lesson-completion claims;
- generated binaries, generated projects, local logs, or secrets;
- documentation-only churn that is not required to describe the durable
  source-code-generator workflow.

## Validate only when needed

When GitHub evidence is current, required checks are green, the worktree is
safe, and the diff scope is focused, no local validation rerun is required for a
no-op finalization.

Run the focused validation when evidence is stale or insufficient, when checks
need local reproduction, or after any focused code or characterization change:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SourceCodeGeneratorTest \
  test
```

This command validates the `core/ast` source-code-generator characterization. It
does not prove full Alice desktop behavior, generated NetBeans project behavior,
visible rendering correctness, grading, broad Tweedle/player decode, Save
completion, or lesson completion.

## Record review evidence

Keep point-in-time evidence in the workflow-owned review handoff or pull request
description, not in durable repository docs.

Use this evidence shape:

```text
Source-code-generator finalization evidence
- PR: <pull-request-number> in rysweet/RabbitHole.
- Head: <headRefName> @ <headRefOid>, base <baseRefName>.
- State: open, non-draft, merge state <mergeStateStatus>.
- Visible status rollup: <all visible checks successful for headRefOid, or named blocker>.
- Required checks: <all branch-protection required checks successful for headRefOid, or named blocker>.
- Review decision: <reviewDecision>; no formal approval recorded when applicable.
- Scope: <diff summary limited to core/ast source-code-generator characterization>.
- Local worktree: <clean, or unrelated dirty files excluded from finalization>.
- Validation: <not rerun because current GitHub evidence was sufficient, or focused SourceCodeGeneratorTest command result>.
- Claim boundary: core/ast source-code-generator characterization only; no desktop UI, rendering, grading, Save, lesson-completion, broad decode, or generated NetBeans claim.
```

Owner-free evidence means no formal approval is recorded. It is not a readiness
blocker by itself when the pull request is non-draft, mergeable, green for the
required checks, focused, and the repository's branch policy permits merge
readiness without an owner approval.

## Finalize with a no-op decision

Prefer a literal no-op decision when the current GitHub head is already
merge-ready and the finalization finds no focused issue.

Use this shape in the handoff:

```text
No-op justification: PR <pull-request-number> current head <headRefOid> on
<headRefName> is non-draft, mergeable, green for visible and required checks;
review evidence records <reviewDecision> with no formal approval when
applicable; the diff scope remains limited to core/ast source-code-generator
characterization; the local worktree has no related dirty changes; no repository
changes were required.
```

Use `MERGE_READY` only when every finalization condition is true for the same
current PR head:

| Condition | Required result |
| --- | --- |
| Current head | Evidence names the latest `headRefOid` from GitHub. |
| Draft state | PR is not draft. |
| Merge state | GitHub reports the PR as clean or mergeable. |
| Visible status rollup | Visible checks are successful for the same head SHA. |
| Required checks | Branch-protection required checks are successful for the same head SHA. |
| Review state | Review decision is recorded, including owner-free/no-approval state when present. |
| Scope | Diff remains focused on `core/ast` source-code-generator characterization. |
| Worktree | No related dirty files are mixed into finalization. |
| Validation | Focused `SourceCodeGeneratorTest` was rerun only when needed or after changes. |
| Claim boundary | Handoff excludes unrelated desktop, rendering, grading, Save, lesson, decode, and NetBeans claims. |

If any condition is missing or failed, the final status is not merge-ready:

```text
NOT_MERGE_READY
Blockers:
- <blocker-name>: <missing evidence or failed gate>
Files modified: <none or focused core/ast paths>
```

## Push only focused fixes

Push only when finalization finds a focused source-code-generator issue that
requires a code or characterization change.

Patch in this order:

1. `core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java`
2. `core/ast/src/main/java/org/lgna/project/ast/SourceCodeGenerator.java`
3. `core/ast/src/main/java/org/lgna/project/ast/JavaCodeGenerator.java`

Modify implementation files only when the focused characterization proves a real
generator defect. After any change, rerun the focused Maven command from
[Validate only when needed](#validate-only-when-needed), then push only the
focused `core/ast` changes.

Do not push unrelated dirty files, generated artifacts, local configuration, or
documentation changes as part of source-code-generator PR finalization unless
the pull request explicitly owns a durable documentation update. Push
workflow-owned evidence logs only when finalization explicitly owns an evidence
artifact refresh, and then re-check the new GitHub head after the push.

## Troubleshooting

| Symptom | Use this blocker | Next step |
| --- | --- | --- |
| `gh pr view` returns a different head SHA than previously recorded. | `stale-pr-evidence` | Refresh all evidence for the new `headRefOid`. |
| PR is draft. | `pr-is-draft` | Leave `NOT_MERGE_READY` until the PR is marked ready for review. |
| GitHub reports a blocked or dirty merge state. | `merge-state-not-clean` | Name the merge state and do not claim readiness. |
| Visible checks are pending, skipped, stale, missing, or failed. | `github-status-rollup-not-green` | Wait for or fix checks on the exact current head. |
| Required checks are pending, skipped, stale, missing, failed, or unavailable. | `github-required-checks-not-green` | Resolve required checks or record the missing branch-protection evidence. |
| Review decision has no approval. | `owner-free-no-formal-approval` | Record the state explicitly; treat it as evidence, not an automatic blocker. |
| Diff includes unrelated QA, NetBeans, desktop, docs, or generated artifacts. | `unfocused-diff-scope` | Narrow the branch or report not merge-ready. |
| Local worktree has unrelated dirty files. | `unrelated-local-changes-present` | Exclude them from finalization and do not push them. |
| `tweedle-lang/Grammar` is missing before Maven validation. | `tweedle-grammar-submodule-missing` | Run `git submodule update --init tweedle-lang` and confirm the grammar directory exists. |
| Focused Maven validation fails. | `source-code-generator-test-failed` | Fix only the failing source-code-generator characterization or proven generator defect, then rerun the same command. |
| Evidence would require desktop UI, rendering, Save, grading, or lesson claims. | `claim-boundary-overreach` | Remove the overclaim or route it to the owning workflow with separate executable evidence. |
