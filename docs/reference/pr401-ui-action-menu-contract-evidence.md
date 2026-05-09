# PR #401 UI Action Menu Contract Handoff

This reference describes the durable recovery contract for RabbitHole PR #401.
It ties the bounded Window menu/action feature, runnable evidence requirements,
quality-audit cycles, PR description requirements, and merge-ready blockers to
the feature contract without embedding a point-in-time PR-head SHA.

This is not a general Alice desktop status report. Exact command output,
current-head SHAs, check conclusions, and remaining blockers belong in the pull
request body, CI logs, or attached review evidence.

## Contents

- [Scope](#scope)
- [Feature boundary](#feature-boundary)
- [Evidence basis](#evidence-basis)
- [Merge-ready gate](#merge-ready-gate)
- [Usage](#usage)
- [Configuration](#configuration)
- [QA and scenario evidence](#qa-and-scenario-evidence)
- [Quality-audit cycles](#quality-audit-cycles)
- [Docs impact](#docs-impact)
- [Diff scope](#diff-scope)
- [PR description evidence](#pr-description-evidence)
- [No-op source justification](#no-op-source-justification)
- [NOT_MERGE_READY blockers](#not_merge_ready-blockers)
- [Non-claims](#non-claims)
- [Examples](#examples)
- [Related documentation](#related-documentation)

## Scope

PR #401 recovery accepts only this menu/action contract claim:

```text
WindowMenuModel is registered in the Alice desktop menu-bar model, keeps the
expected stable identity, and is reachable through menu-bar membership lookup.
```

No Java, runner, schema, validator, scenario, or test source change is required
when the checked-in contract surfaces remain wired and pass at the PR head being
reviewed. Documentation-only recovery updates may refine this handoff, but they
do not broaden the feature claim.

## Feature boundary

The recovered feature is a repo-owned validation seam:

1. `WindowMenuModel` remains registered in the Alice desktop menu-bar model.
2. Its migration identity remains `58a7297b-a5f8-499a-abd1-db6fca4083c8`.
3. `AliceMenuBar` membership lookup can reach the registered model.
4. The QA catalog exposes `alice-desktop-menu-action-smoke` as a fixed, gated
   command smoke.
5. The recovery workflow records merge-ready evidence only after same-head
   checks, runnable QA/scenario validation, docs review, focused diff review,
   and three quality-audit cycles are complete.

This is not live menu automation, visible rendering proof, lesson completion,
assessment/grading evidence, installer validation, Sims coverage, Save
completion, or full Tweedle/player decode behavior.

## Evidence basis

| Field | Value |
| --- | --- |
| Pull request | RabbitHole PR #401 |
| Branch | `wave6-ui-action-menu-contract-1778302300` |
| Head evidence | Record the exact PR `headRefOid` in the PR body and verify local `HEAD` matches it; do not commit point-in-time SHAs to this reference. |
| Base branch | `develop` |
| Scenario | `alice-desktop-menu-action-smoke` |
| Workflow | `menu-action-smoke` |
| Automation mode | `gated-command-smoke` |
| Focused Java contract | `org.alice.ide.croquet.models.AliceMenuBarContractTest` |
| Required GitHub checks | `build`, `coverage`, `package-netbeans`, `test`, `GitGuardian Security Checks` |
| Accepted readiness artifact | Same-head Maven/Surefire success naming `AliceMenuBarContractTest`. |
| Runner/gate artifacts | `status.txt` and `command.log` prove gated runner behavior only; pair them with same-head Maven/Surefire success before claiming readiness. |
| Accepted scope | Window menu model registration, stable identity, and menu-bar membership lookup |
| Documentation role | Durable recovery contract and merge-ready gate definition; PR body owns exact-head evidence and blockers. |

Before citing readiness, the local checkout and PR metadata must agree:

```bash
git rev-parse HEAD
gh pr view 401 --json headRefName,headRefOid,statusCheckRollup,mergeStateStatus
```

Both commands must identify branch `wave6-ui-action-menu-contract-1778302300`
and the same PR head. If a later push changes the PR head, rerun the evidence
and update the PR body instead of carrying forward old exact-head claims.

## Merge-ready gate

PR #401 is merge-ready only when every row passes for the same current PR head.
Green checks and workflow completion are necessary but not sufficient.

| Gate | Ready evidence |
| --- | --- |
| Branch and head alignment | `gh pr view 401` reports branch `wave6-ui-action-menu-contract-1778302300`; `git rev-parse HEAD` equals the PR `headRefOid`. |
| No manual merge | No local merge commit, no manual merge of PR #401, and no push to protected branch state. |
| GitHub Actions | All required checks complete successfully for the same `headRefOid`. |
| Runnable QA/scenario evidence | Focused Maven contract and applicable QA scenario/schema/workflow/gated-command checks run without timeout wrappers. |
| Docs impact | Changed reference, how-to, tutorial, and QA docs are accurate for the feature boundary and do not overclaim. |
| Quality audit | At least three `SEEK -> VALIDATE -> FIX` cycles are documented; the final cycle is clean. |
| Focused diff scope | `git diff --name-status origin/develop...HEAD` stays limited to UI action/menu contract recovery, QA scenario wiring, tests, evidence docs, and directly related test-package metadata. |
| PR description | PR body contains current-head evidence for checks, runnable QA, docs impact, audit cycles, diff scope, and bounded claims. |
| Blockers | Any missing gate is recorded as `NOT_MERGE_READY` instead of inferred from adjacent success. |

## Usage

Validate the scenario catalog:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Inspect the checked-in PR #401 scenario without executing the gated runner path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json \
  alice-desktop-menu-action-smoke
```

Run the focused Java contract directly, without a timeout-wrapper execution
path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

Run the bounded shell recovery checks:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh
bash qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
```

The Save proof and silver-thread checks validate runner wiring, artifact
contracts, and bounded status reporting. They are not proof of full desktop Save
completion.

## Configuration

| Setting | Value or condition | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserved orchestration preference for QA shell and Node-backed tooling. |
| `ALICE_QA_RUN_GATED_SMOKES` | Set to `1` only when intentionally executing the gated runner path | Enables `gated-command-smoke`; it is not required for the direct-Maven/no-wrapper recovery path. |
| `tweedle-lang` | Initialized | Required before Maven reactor validation. |
| `-DincludeSims=false` | Maven property | Keeps this focused contract independent from Sims validation. |
| `-Dinstall4j.skip` | Maven property | Avoids installer packaging work outside the PR #401 scope. |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Maven property | Allows focused `-Dtest=...` selection across the reactor. |

Initialize the grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not wrap recovery commands with `timeout`, `gtimeout`, or equivalent
timeout-wrapper commands. The scenario metadata may declare
`timeoutSeconds`, but the recovery shell commands themselves run directly.

## QA and scenario evidence

Accepted evidence names the focused contract and remains inside the menu/action
registration boundary.

| Evidence item | Required content |
| --- | --- |
| Scenario catalog | `alice-desktop-menu-action-smoke` has workflow `menu-action-smoke` and automation mode `gated-command-smoke`. |
| Scenario argv | Fixed Maven argv includes `-Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest`. |
| Direct Maven output | Surefire output names `AliceMenuBarContractTest` and exits successfully for the same PR head. |
| Schema/workflow contracts | Shell contract checks accept the scenario, schema, validator, runner, and documentation workflow records. |
| Gated command contract | Gated smoke behavior fails closed when the gate is missing and executes only when `ALICE_QA_RUN_GATED_SMOKES=1`. |
| Generated runner evidence | `status.txt` and `command.log` prove runner/gate behavior only; they do not prove readiness unless paired with successful same-head Maven/Surefire evidence. |

If focused core IDE tests or gated desktop smoke execution are not runnable in
the environment, record `NOT_MERGE_READY` unless another accepted artifact proves
the same focused contract at the same PR head.

## Quality-audit cycles

Document three cycles in the PR body before claiming readiness.

| Cycle | SEEK | VALIDATE | FIX |
| --- | --- | --- | --- |
| 1. Java/menu contract | Search `MenuBarComposite`, `WindowMenuModel`, and `AliceMenuBarContractTest` for registration, UUID, and membership drift. | Run or cite same-head `AliceMenuBarContractTest` evidence. | Fix direct Java/test contract drift only; if no drift exists, write `no fix required`. |
| 2. QA scenario wiring | Search scenario, schema, validator, runner, and allowlist surfaces for argv or workflow drift. | Run scenario validation plus schema, workflow, and gated-command contract checks. | Fix broken scenario/schema/runner wiring only; if no drift exists, write `no fix required`. |
| 3. Evidence and readiness | Search docs, PR body, diff scope, and check metadata for stale SHA, missing blocker, or overclaim drift. | Re-query PR head/checks, inspect diff scope, and confirm docs impact. | Fix docs/PR evidence only; the final cycle must be clean before readiness is claimed. |

A cycle may end with `no fix required` only when its `SEEK` and `VALIDATE`
steps justify that result. If the third cycle finds a new issue, fix it and run
another final clean cycle.

## Docs impact

The PR #401 documentation surface is:

| Document | Role |
| --- | --- |
| `docs/reference/window-menu-action-contract.md` | Canonical feature reference for Window menu model registration, stable identity, smoke usage, and non-claims. |
| `docs/reference/pr401-ui-action-menu-contract-evidence.md` | Recovery handoff, merge-ready gate, audit-cycle specification, and PR body evidence contract. |
| `docs/reference/headless-safe-desktop-action-characterization.md` | Broader desktop action characterization lane that links the menu/action registration contract. |
| `docs/howto/characterize-headless-safe-desktop-actions.md` | Contributor workflow for adjacent headless-safe desktop action characterization. |
| `docs/tutorials/desktop-action-journey-characterization.md` | Guided walkthrough for outside-in action smoke evidence and focused contracts. |
| `docs/index.md` | Discoverability entry for the PR #401 handoff and Window menu action contract. |
| `qa/outside-in/alice-desktop/README.md` and Alice desktop QA docs | User-facing scenario and evidence workflow references. |

Docs are sufficient only when they describe runnable commands, configuration,
bounded evidence, and non-claims without using internal shorthand that hides the
workflow from readers.

## Diff scope

The focused PR #401 diff is limited to:

1. Window menu/action contract test coverage.
2. QA scenario wiring for `alice-desktop-menu-action-smoke`.
3. Scenario schema, validator, runner, and shell contract allowlists.
4. Reference/how-to/tutorial/index documentation for the bounded contract.
5. Evidence tests that protect the documentation contract.
6. Directly related test-package metadata, such as the `pyproject.toml` version
   used to identify the recovery/test package.

Unrelated product behavior, broad UI automation, Save implementation changes,
installer packaging changes, Sims validation changes, decoder/player changes,
or unrelated packaging metadata are outside this recovery scope and require
`NOT_MERGE_READY` until removed or separately justified.

## PR description evidence

The PR body is the durable review handoff for exact-head evidence. It must
contain these fields before readiness is claimed:

```text
Current PR head: <current-pr-head-sha>
Branch: wave6-ui-action-menu-contract-1778302300
GitHub Actions: build, coverage, package-netbeans, test, and GitGuardian Security Checks successful for the same head
Focused Java contract: org.alice.ide.croquet.models.AliceMenuBarContractTest
QA/scenario evidence: validate-scenarios.sh, test-schema-contract.sh, test-workflow-contract.sh, test-gated-command-contract.sh, test-save-menu-dialog-write-proof-contract.sh, and test-silver-thread-status-report.sh
Docs impact: window menu action contract and PR #401 handoff docs reviewed for bounded claims and no committed exact-head SHA
Quality audit: three SEEK -> VALIDATE -> FIX cycles documented; final cycle clean
Diff scope: focused on UI action/menu contract recovery, QA scenario wiring, tests, evidence docs, and directly related test-package metadata
Accepted claim: Window menu model registration, stable identity, and menu-bar membership lookup only
Non-claims: no full UI automation, visible rendering correctness, grading, creative assessment, full lesson completion, full Save completion, or full Tweedle/player decode claim
NOT_MERGE_READY: none
```

If any field cannot be completed, replace `NOT_MERGE_READY: none` with explicit
blockers.

## No-op source justification

No Java, runner, schema, validator, scenario, or test modification is required
for this recovery when exact-head evidence passes. The existing implementation
already provides:

1. `AliceMenuBarContractTest` for Window menu model registration, stable
   identity, and menu-bar membership lookup.
2. `menu-action-smoke.yaml` for the checked-in outside-in scenario.
3. `validate-scenarios.sh`, `run-scenario.sh`, `scenario.schema.json`, and shell
   contract tests for fixed Maven argv and gated command-smoke behavior.

No-op source justification: for the PR head under review, the recovery may
remain documentation-only if the focused Java contract, scenario validation,
schema contract, workflow contract, gated-command contract, Save proof contract,
silver-thread status report, same-head GitHub checks, diff-scope review, docs
impact review, and three quality-audit cycles are complete. Missing evidence is
a `NOT_MERGE_READY` blocker, not a reason to infer success.

## NOT_MERGE_READY blockers

Use `NOT_MERGE_READY` for any missing or stale gate. Common blockers:

| Blocker | Required disposition |
| --- | --- |
| Local HEAD differs from `gh pr view 401` headRefOid | Re-check out the PR head or stop recovery; do not use local evidence. |
| GitHub Actions are pending, failing, or tied to another SHA | Wait for same-head green checks or record the failing check. |
| Focused Maven contract was not run and no accepted same-head artifact exists | Run the direct Maven selector or record the missing contract evidence. |
| Scenario/schema/workflow/gated-command checks were not run | Run the shell recovery checks or record missing runnable QA evidence. |
| Gated desktop smoke is required but unavailable | Record the environment blocker; do not claim full smoke execution. |
| Docs contain stale SHA, stale claims, or overclaims | Fix docs or record docs impact as blocking. |
| Fewer than three quality-audit cycles are documented | Complete cycles or record missing audit evidence. |
| Final quality-audit cycle is not clean | Fix findings and run another final cycle. |
| PR body lacks current-head evidence | Update the PR body or record missing PR description evidence. |
| Diff includes unrelated behavior | Remove unrelated drift or record focused-diff blocker. |

Repeated rate-limit exits are not source blockers after evidence is regenerated,
but an interrupted recovery that leaves missing evidence is `NOT_MERGE_READY`.

## Non-claims

Do not cite this handoff as evidence for:

- Full UI automation.
- Visible rendering correctness.
- Live Swing menu opening or click behavior.
- Save, Save As, export, or write/readback completion.
- First-lesson completion.
- Lesson correctness.
- Grading, learner assessment, rubric correctness, or creative assessment.
- World execution or playback.
- Package installation or deployed installer behavior.
- Sims validation.
- Full Tweedle or player archive decode behavior.

## Examples

### Accepted readiness statement

```text
PR #401 readiness was reviewed at the PR head recorded in the pull request body.
The accepted evidence is same-head green Actions, scenario catalog validation,
the focused AliceMenuBarContractTest selector, schema/workflow/gated-command/Save
proof/silver-thread contracts, docs impact review, focused diff review, and
three documented quality-audit cycles with a clean final cycle. The claim is
limited to WindowMenuModel registration, stable identity, and menu-bar membership
lookup.
```

### Accepted blocker statement

```text
NOT_MERGE_READY:
- Gated desktop smoke execution was required but not runnable in this
  environment.
- The PR body has not yet recorded three SEEK -> VALIDATE -> FIX quality-audit
  cycles with a clean final cycle.
```

### Rejected overclaim

```text
PR #401 proves the Alice desktop UI works end to end.
```

Reject this because the contract does not prove full UI automation, visible
rendering correctness, grading correctness, lesson completion correctness, Save
completion, or full Tweedle/player decode behavior.

## Related documentation

- [Window menu action contract](./window-menu-action-contract.md) defines the focused Window menu registration contract.
- [Headless-safe desktop action characterization](./headless-safe-desktop-action-characterization.md) describes the surrounding desktop action characterization lane.
- [Characterize headless-safe desktop actions](../howto/characterize-headless-safe-desktop-actions.md) explains how to add or review adjacent desktop action characterization.
- [Tutorial: Trace a Desktop Action Journey](../tutorials/desktop-action-journey-characterization.md) walks through outside-in action smoke evidence and focused contracts.
- [Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md) documents scenario schema, runner commands, configuration, and evidence artifacts.
