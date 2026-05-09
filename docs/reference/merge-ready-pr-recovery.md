# Merge-ready PR recovery reference

Merge-ready PR recovery is the review gate for recovering an open pull request
after an interrupted automation run. The executable entry point is
`scripts/merge-ready-pr-recovery.py`. It validates the exact remote PR head,
local evidence, GitHub Actions, runnable QA evidence, documentation impact,
quality-audit cycles, focused diff scope, draft/review state, and PR metadata
before anyone claims a PR is ready.

This workflow is a readiness classifier. It does not merge, rebase, squash,
force-push, or edit PR metadata unless a separate task explicitly authorizes
that change.

For step-by-step usage, see
[Run merge-ready PR recovery](../howto/run-merge-ready-pr-recovery.md).

## Scope

Use this recovery gate when a PR has been left in an uncertain state by a failed
or rate-limited automation run and the next action must be evidence-based.

These docs define a reusable recovery feature. They are not evidence that any
specific PR is merge-ready. If a code or test PR is intentionally scoped to one
file, landing these docs belongs in a separate documentation PR unless that PR's
scope explicitly includes the recovery workflow.

The gate accepts only two final states:

| State | Meaning |
| --- | --- |
| `MERGE_READY` | The exact PR head has all required evidence: focused diff, local validation, applicable runnable QA evidence, no required docs gap, at least three quality-audit cycles with a clean final cycle, green completed checks, non-draft/reviewable state, and sufficient PR description evidence. |
| `NOT_MERGE_READY` | One or more required evidence items are missing, stale, failing, blocked, out of scope, or unreviewable. The result names each blocker explicitly. |

Green GitHub Actions are required for `MERGE_READY`, but they are never
sufficient by themselves.

## Configuration

Recovery runs use repository tooling plus authenticated `git` and GitHub CLI
(`gh`) access. The external service integration is intentionally read-only:
`gh pr view` and `gh pr checks` collect PR metadata, body text, review state,
head SHA, and check conclusions. The workflow does not merge, rebase, push,
approve, close, or edit PR metadata.

Set the existing heap preference when running local validation commands:

```sh
export NODE_OPTIONS=--max-old-space-size=32768
```

Do not wrap recovery commands with `timeout`, `gtimeout`, or equivalent timeout
helpers. If a command cannot complete in the current environment, classify the
missing evidence as blocked instead of manufacturing a pass.

The current executable classifier is intentionally fixed to PR `425`,
head branch
`feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow`, and base
branch `develop`. If CLI inputs or GitHub PR metadata disagree with that target,
the classifier stops before CI/QA evidence collection and reports
`NOT_MERGE_READY`.

Run the classifier with the fixed PR inputs and one or more expected diff paths:

```sh
scripts/merge-ready-pr-recovery.py \
  --pr-number 425 \
  --head-branch feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow \
  --base-branch develop \
  --expected-diff-path core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java
```

## PR head alignment contract

All evidence is tied to the exact GitHub PR head SHA for the fixed target:

```sh
PR_NUMBER=425
HEAD_BRANCH=feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow
BASE_BRANCH=develop

git fetch origin "$HEAD_BRANCH"
git checkout "$HEAD_BRANCH"
git pull --ff-only origin "$HEAD_BRANCH"

gh pr view "$PR_NUMBER" \
  --json number,state,isDraft,reviewDecision,baseRefName,headRefName,headRefOid,title,url
git rev-parse HEAD
```

The PR number, metadata `headRefName`, metadata `baseRefName`, and local `HEAD`
must match the fixed target before local validation, QA evidence, or
quality-audit findings can be used for readiness.

If the head moves after evidence is collected, rerun the recovery gate for the
new SHA.

## Diff scope contract

The recovery gate compares the PR head against the base branch:

```sh
git fetch origin "$BASE_BRANCH"
git diff --name-status "origin/$BASE_BRANCH"...HEAD
```

The expected scope comes from the PR design. Broad cleanups, unrelated
production rewrites, unrelated desktop QA changes, unrelated decoder changes, or
generated binary artifacts are `NOT_MERGE_READY` blockers unless the PR design,
description, and tests prove that broader scope was intentional.

Documentation changes are not neutral. A PR that was designed as code-only or
test-only is out of scope if recovery docs are added to that same PR without an
explicit design update.

## Local validation contract

Initialize the Tweedle grammar submodule before Maven validation:

```sh
git submodule update --init tweedle-lang
```

Run the focused validation command for the PR's changed surface. For model
export characterization recovery, the command is:

```sh
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

Evidence supports only the behavior covered by the focused tests. For model
export characterization, that may include resource enum naming, generated
constants, XML identity, XML metadata, thumbnail handling when covered by the
test, and generated Java compilation. It does not prove full UI automation,
visible rendering correctness, grading, creative assessment, full lesson
completion, or full Tweedle/player archive decode.

## GitHub Actions contract

Collect check results only when the PR head SHA is stable across the check
query:

```sh
HEAD_BEFORE=$(gh pr view "$PR_NUMBER" --json headRefOid --jq .headRefOid)
gh pr checks "$PR_NUMBER" --json name,state,conclusion,link
HEAD_AFTER=$(gh pr view "$PR_NUMBER" --json headRefOid --jq .headRefOid)
test "$HEAD_BEFORE" = "$HEAD_AFTER"
test "$HEAD_BEFORE" = "$(git rev-parse HEAD)"
```

All required completed checks must be green for `MERGE_READY`. Pending,
cancelled, skipped-required, failing, stale, or SHA-mismatched checks are
blockers.

## Draft and review-state contract

Recovery records `state`, `isDraft`, and `reviewDecision` from `gh pr view`.

| Field | Merge-ready requirement |
| --- | --- |
| `state` | Must be `OPEN`. Closed or merged PRs are not recovered through this gate. |
| `isDraft` | Must be `false`. Draft PRs are `NOT_MERGE_READY`. |
| `reviewDecision` | Must not be `CHANGES_REQUESTED`. If branch policy requires approval, it must be `APPROVED`. |

Any failing field is reported as a first-class blocker.

## Runnable QA evidence contract

Recovery classifies applicable QA or scenario evidence as one of these values:

| Classification | Readiness effect |
| --- | --- |
| `executed` | An existing runnable script or test was run for the exact PR head and produced reviewable evidence. |
| `ci-provided` | CI ran the applicable QA for the exact PR head and the artifact is reviewable. |
| `documented-manual` | A manual checklist exists, but no executable proof was produced for the PR head. This is not enough for `MERGE_READY` when runnable QA is applicable. |
| `prepare-only` | The scenario validates inputs or setup only. This is not executable end-to-end proof. |
| `not-applicable` | No runnable QA path applies to the changed surface, and the result explains why focused validation is the applicable evidence. |
| `missing` | Applicable runnable QA should exist for the changed surface, but no evidence was found. |
| `blocked` | The applicable QA cannot run in the environment and the blocker is named. |

The classifier skips recursive QA discovery when the changed paths are outside
QA/scenario surfaces. When QA/scenario paths are touched, discovery still runs;
missing, prepare-only, documented-manual, or blocked evidence is a
`NOT_MERGE_READY` blocker unless CI provided equivalent runnable evidence for
the exact head SHA.

## Documentation impact contract

Review whether the PR changes durable contributor or user behavior. A test-only
characterization PR may require no documentation change when existing reference
docs already describe the protected behavior and the PR does not alter the
contract.

If documentation is required, update durable docs under `docs/` and link them
from `docs/index.md`. Do not commit status reports, transient validation logs,
or point-in-time recovery notes as documentation.

If the PR's design says no docs changes, either split the documentation into a
separate PR or update the design before treating docs changes as in scope.

## Quality-audit loop contract

Recovery performs at least three explicit SEEK/VALIDATE/FIX cycles:

| Cycle | SEEK | VALIDATE | FIX |
| --- | --- | --- | --- |
| 1 | Diff scope drift, unrelated files, binary artifacts, generated noise. | Compare `origin/$BASE_BRANCH...HEAD` and inspect changed files. | Remove or block unrelated scope. |
| 2 | Test adequacy gaps for the claimed behavior. | Inspect focused tests and run the applicable local validation. | Add or block missing coverage. |
| 3 | Evidence completeness gaps: CI, QA, docs, PR body, draft state, review state, and bounded claims. | Compare all collected evidence against this reference contract. | Update allowed metadata/docs or return explicit blockers. |

The final cycle must be clean for `MERGE_READY`. If the final cycle finds a gap,
the result is `NOT_MERGE_READY`.

## PR description evidence contract

The PR body must let reviewers evaluate readiness without reconstructing the
entire recovery session. It includes:

- exact head SHA;
- open/draft/review state;
- focused diff summary;
- CI/check status tied to that SHA;
- local validation command and bounded result;
- QA/scenario evidence classification or blocker;
- documentation impact rationale;
- quality-audit cycle summary;
- explicit non-claims for unproven behavior.

If the PR body lacks required evidence and the recovery task does not authorize
editing metadata, the recovery result is `NOT_MERGE_READY`.

## Output API

The recovery report is the workflow API. It is written as a bounded, reviewable
classification:

```text
Result: MERGE_READY | NOT_MERGE_READY
PR: #<pr-number>
Head: <sha>
Base: <base-branch>
Branch: <head-branch>
State: <OPEN | CLOSED | MERGED>
Draft: <true | false>
Review state: <APPROVED | CHANGES_REQUESTED | REVIEW_REQUIRED | none>
Files modified: <none | list>
Diff scope: <focused | blocker>
Local validation: <command and bounded result | blocker>
GitHub Actions: <green for sha | blocker>
QA/scenario evidence: <classification and rationale>
Docs impact: <changed docs | no-doc-change rationale | blocker>
Quality audit cycles: <cycle summaries>
PR description evidence: <sufficient | blocker>
Non-claims: <explicitly unproven behavior>
Blockers: <none | NOT_MERGE_READY blocker list>
```

`MERGE_READY` is valid only when `Blockers` is `none`, `Draft` is `false`, and
the review state is compatible with the branch policy.
