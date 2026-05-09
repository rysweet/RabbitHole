# Run merge-ready PR recovery

Use this guide to recover a pull request through the merge-ready gate after an
interrupted automation run. The goal is to produce a bounded readiness
classification with `scripts/merge-ready-pr-recovery.py`, not to merge the PR.

For the gate contract and output fields, see the
[merge-ready PR recovery reference](../reference/merge-ready-pr-recovery.md).

## 1. Set recovery inputs

Replace the values with the PR being recovered:

```sh
PR_NUMBER=<pr-number>
HEAD_BRANCH=<head-branch>
BASE_BRANCH=<base-branch>
EXPECTED_DIFF_PATH=core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java
```

For the model export characterization lane, the executable classifier can run
the full gate directly:

```sh
scripts/merge-ready-pr-recovery.py \
  --pr-number "$PR_NUMBER" \
  --head-branch "$HEAD_BRANCH" \
  --base-branch "$BASE_BRANCH" \
  --expected-diff-path "$EXPECTED_DIFF_PATH"
```

The remaining steps document the evidence collected by that command.

## 2. Align to the remote PR head

Fetch and checkout the exact PR branch. Do not trust stale local state.

```sh
git fetch origin "$HEAD_BRANCH"
git checkout "$HEAD_BRANCH"
git pull --ff-only origin "$HEAD_BRANCH"
```

Record the PR metadata and local SHA:

```sh
gh pr view "$PR_NUMBER" \
  --json number,state,isDraft,reviewDecision,baseRefName,headRefName,headRefOid,title,url
git rev-parse HEAD
```

Continue only when `git rev-parse HEAD` matches `headRefOid`. If it does not,
repeat the fetch and checkout step.

## 3. Confirm the diff is in scope

Compare the PR to the base branch:

```sh
git fetch origin "$BASE_BRANCH"
git diff --name-status "origin/$BASE_BRANCH"...HEAD
```

Compare the file list with the PR design. If unrelated files appear, either
remove the unrelated changes in an allowed implementation step or return
`NOT_MERGE_READY` with a focused-diff blocker.

Documentation files count as scope. If the PR was designed as code-only or
test-only, do not add recovery docs to that PR unless the design is updated to
include them.

## 4. Review the behavior surface

Inspect the changed tests and the production surfaces they protect. Accept only
claims that the focused evidence proves.

For a model export characterization PR, the relevant files are usually:

- `core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java`
- `core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceExporter.java`
- `core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceJavaGenerator.java`
- `core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceXmlGenerator.java`

Bounded model export claims may cover generated resource names, constants, XML
identity, XML metadata, and generated Java compilation when the assertions
directly cover those surfaces.

Do not claim full UI automation, visible rendering correctness, grading,
creative assessment, full lesson completion, or full Tweedle/player decode from
that test evidence.

## 5. Run focused validation

Initialize the Tweedle grammar submodule before Maven validation:

```sh
git submodule update --init tweedle-lang
```

Run the focused validation command for the changed surface. For model export
characterization, use:

```sh
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

Do not wrap the command in `timeout` or an equivalent helper. If validation
cannot complete, report the validation failure or environment blocker.

## 6. Collect GitHub Actions evidence

Collect checks only if the PR head stays unchanged before and after the query:

```sh
HEAD_BEFORE=$(gh pr view "$PR_NUMBER" --json headRefOid --jq .headRefOid)
gh pr checks "$PR_NUMBER" --json name,state,conclusion,link
HEAD_AFTER=$(gh pr view "$PR_NUMBER" --json headRefOid --jq .headRefOid)
test "$HEAD_BEFORE" = "$HEAD_AFTER"
test "$HEAD_BEFORE" = "$(git rev-parse HEAD)"
```

All required completed checks must be green. A pending, failing, cancelled,
skipped-required, stale, or SHA-mismatched check blocks readiness.

## 7. Record draft and review state

Capture first-class PR state:

```sh
gh pr view "$PR_NUMBER" --json state,isDraft,reviewDecision
```

Return `NOT_MERGE_READY` when the PR is not open, is still a draft, has
requested changes, or lacks required approval under the branch policy.

## 8. Classify QA and scenario evidence

Search for an applicable existing scenario or QA script before deciding that no
runnable evidence applies:

```sh
find qa/outside-in -maxdepth 4 -type f | sort
grep -R "export\\|model" -n qa/outside-in docs/reference docs/howto | head -50
```

Classify the evidence:

| Classification | Use it when |
| --- | --- |
| `executed` | You ran an applicable checked-in script or test for the exact PR head. |
| `ci-provided` | GitHub Actions produced the applicable runnable evidence for the exact PR head. |
| `documented-manual` | Only a manual checklist exists. |
| `prepare-only` | The script validates setup or scenario shape but does not prove the behavior. |
| `not-applicable` | No runnable QA path applies to the changed surface, and you record why. |
| `missing` | Applicable runnable evidence should exist but was not found. |
| `blocked` | The runnable path exists but cannot run in the current environment. |

If runnable QA is applicable and the classification is not `executed` or
`ci-provided`, return `NOT_MERGE_READY`.

## 9. Review documentation impact

Check whether the PR changes user-facing behavior or durable contributor
contracts. If the PR is only characterization coverage and existing docs already
describe the protected behavior, record a no-doc-change rationale.

If docs are required, update durable documentation under `docs/` and link it
from `docs/index.md`. Keep recovery logs, check results, and point-in-time
status out of repository docs.

## 10. Run three quality-audit cycles

Perform the cycles explicitly:

1. SEEK diff-scope drift. VALIDATE with `git diff --name-status "origin/$BASE_BRANCH"...HEAD`. FIX unrelated scope or block.
2. SEEK test adequacy gaps. VALIDATE by inspecting the focused tests and running focused validation. FIX missing coverage or block.
3. SEEK evidence completeness gaps. VALIDATE CI, QA/scenario classification, docs impact, PR body evidence, draft state, review state, and bounded claims. FIX allowed docs or metadata gaps, or block.

The final cycle must be clean to report `MERGE_READY`.

## 11. Review PR metadata

Read the PR body:

```sh
gh pr view "$PR_NUMBER" --json body --jq .body
```

The body must include the exact SHA, open/draft/review state, focused diff
summary, CI/check evidence, local validation, QA/scenario evidence or blocker,
docs rationale, audit-cycle summary, and bounded non-claims. If it does not and
metadata updates are not authorized, return `NOT_MERGE_READY`.

## 12. Emit the recovery result

Return `MERGE_READY` only when every gate is satisfied. Otherwise return
`NOT_MERGE_READY` and name each blocker.

Use this report shape:

```text
Result: NOT_MERGE_READY
PR: #<pr-number>
Head: <sha>
Base: <base-branch>
Branch: <head-branch>
State: <OPEN | CLOSED | MERGED>
Draft: <true | false>
Review state: <APPROVED | CHANGES_REQUESTED | REVIEW_REQUIRED | none>
Files modified: <none or file list>
Diff scope: <focused | blocker>
Local validation: <focused command and result>
GitHub Actions: <check status for sha>
QA/scenario evidence: <classification>
Docs impact: <no-doc-change rationale or changed docs>
Quality audit cycles: <three cycle summaries>
PR description evidence: <sufficient or blocker>
Non-claims: <explicitly unproven behavior>
Blockers:
- NOT_MERGE_READY: <explicit blocker>
```

If no repository changes were required, include a no-op justification tied to
the current head, checks, and evidence. If repository files changed, list the
modified files.
