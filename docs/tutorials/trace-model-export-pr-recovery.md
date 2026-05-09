# Tutorial: Trace a model export PR recovery

This tutorial walks through the evidence path for a model export boundary PR
using PR #425 as the example. It teaches the recovery sequence and the claim
boundaries for a test-only characterization change.

For the repeatable checklist, use
[Run merge-ready PR recovery](../howto/run-merge-ready-pr-recovery.md). For the
gate contract, see the
[merge-ready PR recovery reference](../reference/merge-ready-pr-recovery.md).

## Starting point

PR #425 is a model export characterization lane. Its design is narrow: the code
or test change under review should stay focused on
`core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java`
unless the design explicitly expands. Recovery documentation like this tutorial
is useful, but it is not neutral evidence for a test-only PR and should be
separate unless the PR is intentionally a documentation-retcon PR.

Recovery starts from the remote PR head, not from the local checkout:

```sh
git fetch origin feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow
git checkout feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow
git pull --ff-only origin feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow
```

Then compare the local SHA with GitHub's PR head and record the reviewable
state:

```sh
gh pr view 425 \
  --json state,isDraft,reviewDecision,headRefOid
git rev-parse HEAD
```

When the two SHAs match, every later evidence item can be tied to the same code.
A draft PR, requested-changes review state, or required-but-missing approval is
reported as `NOT_MERGE_READY`.

## Trace the focused diff

The diff check answers one question: is this still the model export boundary
lane?

```sh
git fetch origin develop
git diff --name-status origin/develop...HEAD
```

A focused model export characterization PR keeps the changed-code surface in
`ModelExportTest.java`. If docs are present in this PR, the recovery result must
say whether the PR scope was changed to a documentation-retcon scope. If
unrelated production, desktop QA, decoder, generated binary, or cleanup files
appear, the recovery result names a focused-diff blocker.

## Trace the test evidence

Open `ModelExportTest.java` and map each readiness claim to a concrete
assertion:

| Claim | Evidence to find |
| --- | --- |
| Resource enum naming is protected | Assertions for default resources, texture-only variants, combined model/texture names, and forced enum names. |
| Generated constants are protected | Assertions for generated enum constants and resource type constructor arguments. |
| XML identity is protected | Assertions for `AliceModel`, `Resource`, `resourceName`, `modelName`, and `textureName`. |
| Generated Java remains compilable | Calls that compile generated source through the Java compiler. |

Run the focused validation:

```sh
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

This evidence supports model export characterization only. It does not prove the
desktop can render the model, that a learner completed a lesson, that grading is
correct, or that arbitrary Tweedle/player archives decode.

## Trace QA and docs evidence

Search for runnable QA that applies to the changed behavior:

```sh
find qa/outside-in -maxdepth 4 -type f | sort
grep -R "export\\|model" -n qa/outside-in docs/reference docs/howto | head -50
```

If no runnable desktop/export scenario applies to the test-only change, classify
QA evidence as `not-applicable` and record why focused validation is the
applicable executable proof. If an applicable runnable scenario exists but was
not run, classify the gap as `NOT_MERGE_READY`.

Review docs impact next. A characterization-only PR can have a no-doc-change
rationale when existing docs already describe the exporter behavior and no
durable user or contributor contract changed. If the PR adds the recovery
workflow docs, the PR is no longer test-only; treat that as an explicit scope
change or split the docs into their own PR.

## Trace the final gate

Collect GitHub checks only when the PR head SHA is stable across the query:

```sh
HEAD_BEFORE=$(gh pr view 425 --json headRefOid --jq .headRefOid)
gh pr checks 425 --json name,state,conclusion,link
HEAD_AFTER=$(gh pr view 425 --json headRefOid --jq .headRefOid)
test "$HEAD_BEFORE" = "$HEAD_AFTER"
test "$HEAD_BEFORE" = "$(git rev-parse HEAD)"
gh pr view 425 --json body --jq .body
```

The final recovery result is conservative:

```text
Result: NOT_MERGE_READY
Reason: The PR remains blocked until every required gate has evidence for the exact head SHA.
```

Switch to `MERGE_READY` only after the final quality-audit cycle is clean and
there are no blockers for CI, runnable QA, docs impact, PR metadata, draft state,
review state, diff scope, or bounded claims.
