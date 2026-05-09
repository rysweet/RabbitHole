# Finalize exported NetBeans Ant smoke recovery

Use this guide to recover or finalize an exported NetBeans Ant smoke change when
the implementation already exists on a review branch and the remaining work is
readiness, review, and finalization evidence.

For the behavior contract, see [Exported NetBeans Ant Project
Behavior](../reference/exported-netbeans-ant-project-behavior.md). For the QA
runner contract, see [Alice desktop outside-in QA
reference](../reference/alice-desktop-outside-in-qa.md).

## Contents

- [Before you start](#before-you-start)
- [Configuration and public API](#configuration-and-public-api)
- [Verify the authoritative PR head](#verify-the-authoritative-pr-head)
- [Check the diff scope](#check-the-diff-scope)
- [Review scenario and runner contracts](#review-scenario-and-runner-contracts)
- [Run the focused exported Ant smoke](#run-the-focused-exported-ant-smoke)
- [Run three quality-audit cycles](#run-three-quality-audit-cycles)
- [Review documentation impact](#review-documentation-impact)
- [Verify GitHub Actions for the same head](#verify-github-actions-for-the-same-head)
- [Verify the pull request description](#verify-the-pull-request-description)
- [Finalize the recovery](#finalize-the-recovery)
- [Keep the claim narrow](#keep-the-claim-narrow)
- [Troubleshooting](#troubleshooting)

## Before you start

Run commands from the repository root. Use the existing review branch and the
current remote pull request head; do not merge the pull request manually as part
of recovery.

Initialize the Tweedle grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Do not wrap the validation commands in external timeout wrappers. The checked-in
scenario metadata and test code own their bounded execution behavior.

## Configuration and public API

The recovery workflow has no new public Java API. Its stable surface is the
review procedure, the existing QA scenario contract, the existing NetBeans Ant
smoke command, and the PR #389 recovery evidence gate script.

| Configuration | Required value | Purpose |
| --- | --- | --- |
| Branch | `origin/wave5-netbeans-ant-1778295741` | Authoritative PR #389 head for recovery evidence. |
| Base | `origin/develop` | Diff-scope comparison point. |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Standard local validation memory setting. |
| `ALICE_QA_RUN_GATED_SMOKES` | `1` for runnable gated smoke evidence | Allows `alice-desktop-exported-project-smoke` to execute its focused Maven command instead of preparing evidence only. |
| `tweedle-lang/Grammar` | Present after `git submodule update --init tweedle-lang` | Required Maven prerequisite for generated Tweedle parser inputs. |
| `scripts/pr389_recovery_gate.py` | Evidence JSON evaluator | Emits `MERGE_READY` only when all current-head, QA, Maven, audit, docs, Actions, PR-description, and command-safety gates pass. |

The workflow uses checked-in commands: `validate-scenarios.sh`,
`run-scenario.sh`, focused Maven tests, `scripts/pr389_recovery_gate.py`, `git`,
and `gh`.

## Verify the authoritative PR head

Fetch the recovery branch and reset a disposable local branch to the remote PR
head before collecting evidence. Do not run this in a worktree that contains
local-only changes you need to keep:

```bash
git fetch origin wave5-netbeans-ant-1778295741 --no-tags
git switch -C wave5-netbeans-ant-1778295741 origin/wave5-netbeans-ant-1778295741
git branch --set-upstream-to=origin/wave5-netbeans-ant-1778295741
git --no-pager status --short --branch
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git rev-parse origin/wave5-netbeans-ant-1778295741
gh pr view 389 --repo rysweet/RabbitHole --json headRefName,headRefOid,baseRefName,url
```

The accepted branch is `wave5-netbeans-ant-1778295741`, and the accepted base is
`develop`. `HEAD`, `origin/wave5-netbeans-ant-1778295741`, and the PR
`headRefOid` must be the same commit. If they differ, stop the recovery evidence
path and report `NOT_MERGE_READY: pr-head-sha-mismatch`.

The branch check is readiness evidence only. A clean source tree is acceptable
when the current head already satisfies the gate and no implementation change is
required. Treat an owner exit previously classified as `RATE_LIMIT` as recovery
metadata, not as readiness evidence and not as a blocker by itself.

## Check the diff scope

Review the pull request diff against `origin/develop` before running expensive
checks:

```bash
git --no-pager diff --name-status origin/develop...HEAD
git --no-pager diff --stat origin/develop...HEAD
```

The focused recovery scope is limited to exported NetBeans Ant smoke behavior,
QA scenario or runner wiring, focused tests, and directly related documentation.
If the diff includes unrelated product behavior, broad formatting churn, manual
merge artifacts, generated evidence, or credentials, report
`NOT_MERGE_READY: unfocused-diff-scope` until the branch is narrowed.

## Review scenario and runner contracts

Validate the scenario catalog and the shell contract tests that keep schema,
workflow allowlists, argv allowlists, and gated command behavior synchronized:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh

NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh

NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
```

These checks prove the exported project smoke remains a strict
`gated-command-smoke` scenario with an allowlisted Maven argv. They do not run a
full desktop UI automation path.

Run the gated outside-in scenario when collecting PR-applicable QA evidence:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-exported-project-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/exported-project-smoke
```

Accept the scenario only when `status.txt` reports `outcome=passed`,
`exitCode=0`, and an `argv=` line for the focused
`org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest` Maven selector.
Use `command.log` for the Maven, Surefire, and Ant output evidence from that
argv. A prepare-only run or a run without `ALICE_QA_RUN_GATED_SMOKES=1` is
useful for contract inspection, but it is not runnable smoke evidence for merge
readiness. If the gated smoke cannot run in the recovery environment, report
`NOT_MERGE_READY: gated-smoke-not-run`.

## Run the focused exported Ant smoke

Run the current bounded Maven evidence path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl netbeans -am \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest \
  test
```

Accept the smoke only when the focused test completes successfully and the Ant
logs contain the expected probe evidence:

```text
ANT_RUN_PROBE_OK
ANT_RESOURCE_PROBE_OK
ANT_RUNTIME_CONFIGURATION_PROBE_OK
ANT_TEST_MAIN_PROBE_OK
```

No Ant log may contain:

```text
Java Result:
```

The accepted evidence is limited to exported NetBeans Ant behavior: generated
classes, jar packaging, manifest contents, generated resource packaging, Ant
`jar`, `run`, `run-test-with-main`, `clean`, and runtime metadata propagation up
to the headless probe boundary.

This focused Maven command is the same runnable evidence reached by the gated QA
scenario. Running both paths is acceptable: the scenario proves the outside-in QA
wiring and evidence artifacts, while the focused Maven command gives direct
Surefire output for the NetBeans Ant smoke.

## Run three quality-audit cycles

Recovery finalization requires at least three documented
`SEEK -> VALIDATE -> FIX` cycles. Each cycle uses the same shape:

| Step | Required action | Accepted result |
| --- | --- | --- |
| `SEEK` | Inspect one independent risk surface for defects. | A named finding, or `clean` with the searched surface named. |
| `VALIDATE` | Prove whether the finding is real with source, test, scenario, or command evidence. | `confirmed`, `rejected`, or `clean`. |
| `FIX` | Patch only confirmed PR-caused issues, then rerun the applicable evidence. | Fixed evidence, or `not-needed` when validation rejected the finding or the cycle was clean. |

Use independent surfaces so the cycles are meaningful:

| Cycle | Default surface | Examples of acceptable SEEK checks |
| --- | --- | --- |
| 1 | Scenario and runner contract | Schema allowlist, workflow allowlist, argv allowlist, gated execution path. |
| 2 | NetBeans Ant smoke behavior | Probe markers, Ant target failure handling, generated jar/resource assertions, no `Java Result:` leakage. |
| 3 | Readiness and claim boundary | Docs wording, PR description evidence, GitHub Actions SHA match, diff scope, no overbroad claims. |

The last recorded cycle must be clean. If cycle 3 or a later cycle finds a
confirmed issue, fix it and record another cycle until the final cycle has no
open or unresolved findings. If any confirmed issue remains unfixed, report
`NOT_MERGE_READY: quality-audit-open-finding`.

Use this compact evidence format in the PR description or recovery handoff:

```text
Quality audit:
- Cycle 1 SEEK scenario/runner contract; VALIDATE <evidence>; FIX <result>.
- Cycle 2 SEEK NetBeans Ant smoke behavior; VALIDATE <evidence>; FIX <result>.
- Cycle 3 SEEK readiness and claim boundary; VALIDATE clean; FIX not-needed.
- Cycle 4+ only when needed after review feedback; the highest-numbered cycle
  is the final cycle and must be clean.
```

## Review documentation impact

Documentation impact is part of the merge-ready gate. Review the docs touched by
the diff and the linked behavior reference:

```bash
git --no-pager diff --name-only origin/develop...HEAD -- docs qa
```

Accepted documentation impact is one of:

| Result | Meaning |
| --- | --- |
| `docs-updated` | The branch changed user-facing behavior, configuration, QA commands, evidence artifacts, or claim boundaries, and the related docs explain the finished behavior. |
| `docs-not-needed` | The branch changed only implementation internals and existing docs already describe the public behavior and evidence path. |
| `docs-blocked` | The docs are missing, stale, unlinked, or overclaim beyond executable evidence. |

Use `docs-blocked` as `NOT_MERGE_READY: docs-impact-not-reviewed` when review is
missing, or `NOT_MERGE_READY: docs-overclaim-unproven-behavior` when the docs
overstate what the executable evidence proves. Do not mark a PR ready when
documentation claims full UI automation, visible rendering correctness, grading,
creative assessment, full lesson completion, or full Tweedle/player decode
unless a separate executable proof directly supports that claim.

## Verify GitHub Actions for the same head

Green GitHub Actions are necessary but not sufficient. Verify checks against the
exact head SHA recorded earlier:

```bash
gh pr checks 389 --repo rysweet/RabbitHole --watch
gh pr view 389 --repo rysweet/RabbitHole --json headRefOid,statusCheckRollup
```

Every required check must be successful for the same `headRefOid`: `build`,
`coverage`, `package-netbeans`, `test`, and `GitGuardian Security Checks`. A
skipped, pending, stale, failed, missing, or SHA-mismatched check is a blocker:

```text
NOT_MERGE_READY: github-actions-not-green
```

Do not use local test success as a substitute for required remote checks. Do not
use green remote checks as a substitute for the runnable QA/scenario evidence,
quality-audit cycles, docs review, focused diff review, or PR description
evidence.

## Verify the pull request description

The PR description is the durable review handoff. It must name the exact current
head and include bounded evidence:

| Required PR description evidence | Required content |
| --- | --- |
| Head | PR number, branch, base, and exact head SHA. |
| Diff scope | Short statement that the diff is scoped to exported NetBeans Ant smoke recovery, QA wiring, tests, and docs, or a named blocker. |
| QA/scenario evidence | Scenario validation, contract tests, and gated `alice-desktop-exported-project-smoke` result. |
| Maven evidence | Focused `Alice3ProjectTemplateAntSmokeTest` command and result. |
| Quality audit | At least three `SEEK -> VALIDATE -> FIX` cycles with a clean final cycle. |
| Docs impact | `docs-updated` or `docs-not-needed` with file references. |
| GitHub Actions | Required checks green for the exact head SHA. |
| Claim boundary | Explicit non-claims for full UI automation, visible rendering correctness, grading, creative assessment, lesson completion, and full Tweedle/player decode. |

If the description lacks any required evidence, update it before claiming
readiness. If the description cannot be updated, report
`NOT_MERGE_READY: pr-description-evidence-missing`.

## Finalize the recovery

Use this finalization evidence set in the pull request handoff or recovery note:

| Evidence | Required result |
| --- | --- |
| Branch/head readiness | The checked-out branch, remote branch, and PR head SHA all match. |
| Diff scope | The diff remains focused on exported NetBeans Ant smoke recovery, QA wiring, tests, and docs. |
| Worktree review | No unrelated repository changes are mixed into the recovery. |
| Tweedle grammar | `tweedle-lang/Grammar` exists. |
| Scenario validation | `validate-scenarios.sh` exits successfully. |
| Schema contract | `test-schema-contract.sh` exits successfully. |
| Gated command contract | `test-gated-command-contract.sh` exits successfully. |
| Workflow contract | `test-workflow-contract.sh` exits successfully. |
| Gated QA smoke | `alice-desktop-exported-project-smoke` runs with `ALICE_QA_RUN_GATED_SMOKES=1`; `status.txt` records `outcome=passed`, `exitCode=0`, and focused Maven `argv=...`; `command.log` records the Maven/Surefire/Ant output evidence. |
| Focused Ant smoke | `Alice3ProjectTemplateAntSmokeTest` exits successfully. |
| Quality audit | At least three `SEEK -> VALIDATE -> FIX` cycles are documented, and the final cycle is clean. |
| Documentation impact | Related docs are updated or explicitly not needed without stale or overbroad claims. |
| GitHub Actions | Required checks are green for the exact PR head SHA. |
| PR description | The description contains current-head evidence, QA results, docs impact, diff scope, quality-audit cycles, and bounded claims. |
| Recovery evidence gate | `scripts/pr389_recovery_gate.py` evaluates the collected evidence JSON and reports `MERGE_READY` with no blockers. |
| Claim boundary | The handoff cites only bounded exported NetBeans Ant smoke evidence and excludes full UI automation, rendering correctness, grading, creative assessment, and lesson completion. |

When no implementation change is required, the final handoff must include a
current-head no-op justification. Keep it executable and specific:

```text
No-op justification: current head <commit> on <branch> already satisfies the
exported NetBeans Ant smoke readiness, scenario contract, gated command contract,
workflow contract, and focused Alice3ProjectTemplateAntSmokeTest evidence path;
no repository changes were required.
```

If any check fails, patch only the failing boundary and rerun the same evidence
path. Do not broaden the claim or substitute unrelated full-reactor, rendering,
grading, or manual workflow evidence for the failed check.

If any criterion remains missing, the only accepted final status is
`NOT_MERGE_READY` with explicit blockers. Use this format:

```text
NOT_MERGE_READY
Blockers:
- <blocker-name>: <missing evidence or failed gate>
Files modified: <none or docs/code paths changed by the recovery>
```

Use `MERGE_READY` only when every row in the finalization table passes on the
same current PR head.

## Keep the claim narrow

Cite this recovery only as silver-thread exported NetBeans Ant smoke evidence.
It proves the checked-in exported Ant project template can compile generated
Alice source, package a jar, run headless probes through the exported runtime
classpath, load generated resources, and clean Ant build outputs.

Do not cite this evidence as proof of:

- manual PR merge safety
- installer validation
- full GUI export journey completion
- full UI automation
- visible rendering correctness
- grading or creative assessment
- lesson completion

Those claims require separate executable evidence and separate documentation.

## Troubleshooting

| Symptom | Use this blocker | Next step |
| --- | --- | --- |
| `tweedle-lang/Grammar` is missing. | `tweedle-grammar-submodule-missing` | Run `git submodule update --init tweedle-lang` and confirm the grammar directory exists. |
| Scenario validation rejects the exported project smoke. | `scenario-contract-mismatch` | Fix the scenario, schema, validator, and contract tests together so workflow and argv allowlists match. |
| The gated command contract fails. | `gated-command-contract-mismatch` | Keep `ALICE_QA_RUN_GATED_SMOKES=1` as the only path that executes the focused Maven smoke through the runner. |
| The gated smoke was not executed. | `gated-smoke-not-run` | Run `alice-desktop-exported-project-smoke` with `ALICE_QA_RUN_GATED_SMOKES=1`, or report `NOT_MERGE_READY`. |
| Maven cannot find the NetBeans project template zip. | `project-template-zip-missing` | Re-run the focused NetBeans Maven command so test resources are processed, then inspect the NetBeans test resources phase. |
| An Ant target exits nonzero. | `ant-target-failed` | Preserve the command log and name the failing target: `jar`, `run`, `run-test-with-main`, or `clean`. |
| Required jar, class, manifest, or resource output is absent. | `generated-jar-output-missing` | Preserve the assertion message and generated project listing. |
| A quality-audit cycle has an unresolved confirmed issue. | `quality-audit-open-finding` | Fix the issue, rerun applicable evidence, and complete a clean final cycle. |
| Docs were not reviewed. | `docs-impact-not-reviewed` | Review the diff docs and linked behavior reference, or leave `NOT_MERGE_READY`. |
| Docs are stale or overclaim. | `docs-overclaim-unproven-behavior` | Update docs to describe only directly proven behavior, or leave `NOT_MERGE_READY`. |
| Checks are not green for the PR head SHA. | `github-actions-not-green` | Wait for or fix the required checks on the exact current head. |
| The PR body lacks required evidence. | `pr-description-evidence-missing` | Update the PR description with head SHA, evidence, docs impact, diff scope, audit cycles, and bounded claims. |
