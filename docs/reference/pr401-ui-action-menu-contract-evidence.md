# PR #401 UI Action Menu Contract Handoff

This reference is the bounded recovery handoff for RabbitHole PR #401. It ties
readiness, review, and finalization evidence to validated source-contract head
`ae63ebfb94111aa01b42e5b164dd27687dbd9622`.

It is not a general Alice desktop status report. Durable menu/action behavior
belongs in the related contract references linked below.

## Contents

- [Scope](#scope)
- [Feature boundary](#feature-boundary)
- [Evidence basis](#evidence-basis)
- [Usage](#usage)
- [Configuration](#configuration)
- [Readiness evidence](#readiness-evidence)
- [Review evidence](#review-evidence)
- [Finalization evidence](#finalization-evidence)
- [Blocker register](#blocker-register)
- [No-op source justification](#no-op-source-justification)
- [Non-claims](#non-claims)
- [Examples](#examples)
- [Related documentation](#related-documentation)

## Scope

PR #401 recovery accepts only this menu/action contract claim:

```text
WindowMenuModel is registered in the Alice desktop menu-bar model, keeps the
expected stable identity, and is reachable through menu-bar membership lookup.
```

No source change is required when the checked-in scenario, runner, schema,
validator, allowlist, and focused Java test remain wired and pass at the
validated source-contract head.

## Feature boundary

The recovered feature is a repo-owned validation seam:

1. `WindowMenuModel` remains registered in the Alice desktop menu-bar model.
2. Its migration identity remains `58a7297b-a5f8-499a-abd1-db6fca4083c8`.
3. `AliceMenuBar` membership lookup can reach the registered model.
4. The QA catalog exposes `alice-desktop-menu-action-smoke` as a fixed, gated
   command smoke.

This is not live menu automation, visible rendering proof, lesson completion,
assessment/grading evidence, installer validation, Sims coverage, or full Save
behavior.

## Evidence basis

| Field | Value |
| --- | --- |
| Pull request | RabbitHole PR #401 |
| Validated source-contract head | `ae63ebfb94111aa01b42e5b164dd27687dbd9622` |
| Scenario | `alice-desktop-menu-action-smoke` |
| Workflow | `menu-action-smoke` |
| Automation mode | `gated-command-smoke` |
| Focused Java contract | `org.alice.ide.croquet.models.AliceMenuBarContractTest` |
| Accepted artifacts | Maven selector, command log, or Surefire report naming `AliceMenuBarContractTest`; `status.txt` and `command.log` only when the gated runner path is intentionally used outside this no-wrapper recovery |
| Accepted scope | Window menu model registration, stable identity, and menu-bar membership lookup |
| Workflow recovery checks | Scenario validation, schema contract, workflow contract, Save proof contract, and silver-thread status report |
| Handoff outcome | Review/finalization handoff only; no Java, runner, schema, validator, scenario, or test change is required at the validated source-contract head. |

Documentation-only commits that add or refine this handoff are not source
evidence for PR #401, and may make the documentation checkout HEAD differ from
the validated source-contract head. Before citing readiness, the source-contract
checkout must report `ae63ebfb94111aa01b42e5b164dd27687dbd9622` from
`git rev-parse HEAD`, and any later handoff checkout must rerun the same
executable current-head checks.

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

Run the bounded workflow recovery checks before finalization:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh
bash qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
```

The Save proof contract check validates no-timeout runner wiring, canonical
artifact fields, and fail-closed evidence validation. It is not proof of full
desktop Save completion. The silver-thread report must print
`status:silver_thread=covered_bounded`; optional Save/reopen gaps remain
non-claims for full Save/reopen behavior.

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

## Readiness evidence

Readiness is complete only when all rows pass for validated source-contract head
`ae63ebfb94111aa01b42e5b164dd27687dbd9622`.

| Requirement | Evidence |
| --- | --- |
| Source-contract head identified | `git rev-parse HEAD` reports `ae63ebfb94111aa01b42e5b164dd27687dbd9622` in the source-contract checkout. |
| Scenario catalog valid | `validate-scenarios.sh` accepts the checked-in catalog, including `menu-action-smoke`. |
| Scenario contract present | `validate-scenarios.sh --dump-json alice-desktop-menu-action-smoke` exposes workflow `menu-action-smoke`, automation mode `gated-command-smoke`, and argv selecting `AliceMenuBarContractTest`. |
| Runner, schema, and allowlist wired | `run-scenario.sh`, `scenario.schema.json`, `validate-scenarios.sh`, and shell contract tests accept the fixed Maven argv and reject ad hoc command expansion. |
| Command evidence names contract | The Maven selector, command log, or Surefire report names `org.alice.ide.croquet.models.AliceMenuBarContractTest`; `command.log` is accepted only for an intentionally gated runner execution outside this no-wrapper recovery. |
| Focused Java contract passes | The direct Maven selector for `AliceMenuBarContractTest` exits successfully. |
| Workflow recovery checks pass | Scenario validation, schema contract, workflow contract, Save proof contract, and silver-thread status report all exit successfully. |
| Claim boundary preserved | Evidence wording stays limited to Window menu model registration, stable identity, and menu-bar membership lookup. |

## Review evidence

The review evidence for validated source-contract head
`ae63ebfb94111aa01b42e5b164dd27687dbd9622` is:

```text
Validated source-contract head: ae63ebfb94111aa01b42e5b164dd27687dbd9622
Scenario: alice-desktop-menu-action-smoke
Workflow: menu-action-smoke
Automation mode: gated-command-smoke
Focused contract: org.alice.ide.croquet.models.AliceMenuBarContractTest
Accepted result: scenario catalog valid, focused Java contract passed, schema contract passed, workflow contract passed, Save proof contract passed, silver-thread required seams covered
Accepted claim: Window menu model registration, stable identity, and menu-bar membership lookup only
Silver-thread Save/reopen boundary: optional Save/reopen evidence may report not_covered_optional and remains a non-claim for full desktop Save completion
Source blocker: none found in the scenario, runner, validator, schema, allowlist, or Java contract surfaces
Source outcome: no source change required for PR #401 recovery
Handoff outcome: review/finalization handoff only; no Java, runner, schema, validator, scenario, or test change required
```

For this no-wrapper recovery, cite the validated source-contract head plus
executable current-head output from the direct Maven selector and bounded
workflow recovery checks. Attach generated scenario evidence only when the gated
runner path was intentionally used.

## Finalization evidence

PR #401 finalization is ready only through the default workflow path, with no
manual merge, and only as a bounded menu/action contract handoff.

| Check | Accepted finalization evidence |
| --- | --- |
| Source-contract branch/head | `wave6-ui-action-menu-contract-1778302300` at `ae63ebfb94111aa01b42e5b164dd27687dbd9622`. |
| Scenario validation | `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` validates the checked-in catalog. |
| Schema contract | `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` accepts the schema, argv allowlists, and Save proof no-timeout boundary. |
| Workflow contract | `qa/outside-in/alice-desktop/tests/test-workflow-contract.sh` accepts aligned scenario, schema, validator, runner, and documentation workflow records. |
| Save proof contract | `qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh` accepts canonical Save proof artifact validation and no shell timeout-wrapper invocation. |
| Silver-thread status | `qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh` reports `status:silver_thread=covered_bounded` for required seams, explicit non-claim lines, and optional Save/reopen gaps as non-blocking. |
| Focused Java contract | Direct Maven execution of `org.alice.ide.croquet.models.AliceMenuBarContractTest` exits successfully without a timeout wrapper. |
| Final claim | Recover PR #401 through the default workflow as a bounded menu/action contract handoff only. |

The finalization evidence does not prove full UI automation, rendering
correctness, grading, creative assessment, lesson completion, desktop Save
behavior, or full desktop Save completion.

## Blocker register

No blocking source blocker was found for the validated source-contract head. The
items below are non-blocking by scope unless a future exact-head rerun fails the
scenario catalog, schema/workflow contracts, or focused Java contract.

| Blocker | Disposition | Reason |
| --- | --- | --- |
| Repeated rate-limit exits before finalization | Non-blocking for PR #401 evidence | They interrupted prior recovery attempts but do not affect checked-in contract wiring once evidence is regenerated. |
| Full UI automation not available | Non-blocking by scope | PR #401 recovery accepts only the menu/action contract. |
| Visible rendering correctness not proven | Non-blocking by scope | The contract does not inspect pixels, screenshots, or rendered menu correctness. |
| Grading or learner assessment not proven | Non-blocking by scope | PR #401 is not a grading or assessment recovery. |
| Lesson completion not proven | Non-blocking by scope | PR #401 does not claim first-lesson completion or lesson-completion correctness. |
| Installer, package, or Sims validation not run | Non-blocking by scope | The focused contract uses `-Dinstall4j.skip` and `-DincludeSims=false`. |
| Later documentation-only handoff commit | Non-blocking for PR #401 source evidence | Documentation commits may refine the handoff, but source evidence still comes from source-contract head `ae63ebfb94111aa01b42e5b164dd27687dbd9622`. |

Future failures in `validate-scenarios.sh`, the schema/workflow contract checks,
or the focused `AliceMenuBarContractTest` selector are blocking for this handoff.

## No-op source justification

No source modification is required for this recovery when the exact-head evidence
above passes. The existing implementation already provides:

1. `AliceMenuBarContractTest` for Window menu model registration, stable
   identity, and menu-bar membership lookup.
2. `menu-action-smoke.yaml` for the checked-in outside-in scenario.
3. `validate-scenarios.sh`, `run-scenario.sh`, `scenario.schema.json`, and shell
   contract tests for fixed Maven argv and gated command-smoke behavior.

No-op justification: at validated source-contract head
`ae63ebfb94111aa01b42e5b164dd27687dbd9622`, the focused Java contract, scenario
validation, schema contract, workflow contract, Save proof contract, and
silver-thread status report accept the existing scenario, runner, validator,
schema, documentation, and test surfaces. The accepted work is therefore
review/finalization handoff only, with no Java, runner, schema, validator,
scenario, or test change required.

## Non-claims

Do not cite this handoff as evidence for:

- Full UI automation.
- Visible rendering correctness.
- Live Swing menu opening or click behavior.
- Save, Save As, export, or write/readback completion.
- First-lesson completion.
- Lesson correctness.
- Grading, learner assessment, or rubric correctness.
- World execution or playback.
- Package installation or deployed installer behavior.
- Sims validation.

## Examples

### Accepted readiness statement

```text
PR #401 readiness was reviewed at validated source-contract head
ae63ebfb94111aa01b42e5b164dd27687dbd9622. The accepted evidence is the
menu/action scenario contract, focused Java contract, schema/workflow/Save proof
contracts, and silver-thread status report. The claim is limited to
WindowMenuModel registration, stable identity, and menu-bar membership lookup.
```

### Rejected overclaim

```text
PR #401 proves the Alice desktop UI works end to end.
```

Reject this because the contract does not prove full UI automation, visible
rendering correctness, grading correctness, or lesson completion correctness.

## Related documentation

- [Window menu action contract](./window-menu-action-contract.md) defines the focused Window menu registration contract.
- [Headless-safe desktop action characterization](./headless-safe-desktop-action-characterization.md) describes the surrounding desktop action characterization lane.
- [Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md) documents scenario schema, runner commands, configuration, and evidence artifacts.
