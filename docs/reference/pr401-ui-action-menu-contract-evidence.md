# PR #401 UI Action Menu Contract Handoff

This reference defines the bounded feature contract for recovering RabbitHole PR
#401 as a UI action menu handoff. It ties readiness, review, and finalization
evidence to validated source-contract head
`ae63ebfb94111aa01b42e5b164dd27687dbd9622`.

This is a point-in-time PR recovery handoff, not a general Alice desktop status
report. Use the durable contract references linked below for ongoing menu/action
behavior after PR #401.

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

PR #401 recovery accepts only the menu/action contract evidence for the Alice
desktop Window menu model. The accepted claim is:

```text
WindowMenuModel is registered in the Alice desktop menu-bar model, keeps the
expected stable identity, and is reachable through menu-bar membership lookup.
```

This is a contract and command-smoke recovery, not a desktop UI redesign. It
does not require a source change when the checked-in scenario, runner, schema,
validator, allowlist, and focused Java test remain wired and pass at the exact
validated source-contract head.

## Feature boundary

The feature being recovered is a repo-owned menu/action validation seam:

1. `WindowMenuModel` remains registered in the Alice desktop menu-bar model.
2. The registered model keeps stable migration identity
   `58a7297b-a5f8-499a-abd1-db6fca4083c8`.
3. The registered model is reachable through `AliceMenuBar` membership lookup.
4. The outside-in QA catalog exposes a fixed, gated command smoke named
   `alice-desktop-menu-action-smoke`.

The feature is not live menu automation, visible rendering proof, lesson
completion, assessment/grading evidence, installer validation, or Sims coverage.
Those claims belong to separate contracts and must not be inferred from PR #401.

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

This handoff applies only to the validated source-contract head named above. If
a reviewer checks a different source commit, refresh this document with the new
exact SHA and rerun the same focused evidence commands before citing readiness.

Documentation-only commits that add or refine this handoff are not source
evidence for PR #401, and may make the documentation checkout HEAD differ from
the validated source-contract head. The source checkout for the contract must
report `ae63ebfb94111aa01b42e5b164dd27687dbd9622` from `git rev-parse HEAD`
before the scenario catalog, direct focused Java contract, or intentionally
gated runner evidence is cited.

## Usage

Use the scenario catalog validator first:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The checked-in outside-in scenario remains the catalog entry for this contract.
For this no-wrapper recovery, validate the catalog entry without executing the
gated runner path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json \
  alice-desktop-menu-action-smoke
```

Run the focused Java contract directly when a reviewer wants the Maven selector
without a timeout-wrapper execution path:

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
artifact field names, and fail-closed evidence validation. It is not proof of
full desktop Save completion. The silver-thread report must print
`status:silver_thread=covered_bounded` for required seams; an optional
`gap:save_reopen=not_covered_optional` line remains a bounded non-claim for full
Save/reopen behavior.

## Configuration

| Setting | Value or condition | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserved orchestration preference for QA shell and Node-backed tooling. |
| `ALICE_QA_RUN_GATED_SMOKES` | Set to `1` only when intentionally executing the gated runner path | Enables `gated-command-smoke` execution instead of prepare-only or gated-not-run evidence. It is not required for the direct-Maven/no-wrapper recovery path. |
| `tweedle-lang` | Initialized | Required before Maven reactor validation. |
| `-DincludeSims=false` | Maven property | Keeps this focused contract independent from Sims validation. |
| `-Dinstall4j.skip` | Maven property | Avoids installer packaging work outside the PR #401 contract scope. |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Maven property | Allows focused `-Dtest=...` selection across the reactor. |

Initialize the required grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Readiness evidence

Readiness for PR #401 is bounded to this feature contract. It is complete when
all entries in this table are true for validated source-contract head
`ae63ebfb94111aa01b42e5b164dd27687dbd9622`.

| Requirement | Evidence |
| --- | --- |
| Source-contract head identified | `git rev-parse HEAD` reports `ae63ebfb94111aa01b42e5b164dd27687dbd9622` in the source-contract checkout. |
| Scenario catalog valid | `validate-scenarios.sh` accepts the checked-in desktop QA scenario catalog, including `menu-action-smoke`. |
| Runner contract present | `run-scenario.sh` accepts the fixed `alice-desktop-menu-action-smoke` argv shape and rejects ad hoc shell command expansion. |
| Schema and allowlist present | `scenario.schema.json`, `validate-scenarios.sh`, and shell contract tests allow the focused `AliceMenuBarContractTest` argv. |
| Menu/action scenario contract present | `validate-scenarios.sh --dump-json alice-desktop-menu-action-smoke` exposes workflow `menu-action-smoke`, automation mode `gated-command-smoke`, and argv selecting `AliceMenuBarContractTest`. |
| Command evidence names contract | The Maven selector, command log, or Surefire report names `org.alice.ide.croquet.models.AliceMenuBarContractTest`; `command.log` is accepted only for an intentionally gated runner execution outside this no-wrapper recovery. |
| Focused Java contract passes | The direct Maven selector for `AliceMenuBarContractTest` exits successfully. |
| Schema contract passes | `test-schema-contract.sh` preserves the scenario schema, argv allowlist, and Save proof no-timeout contract. |
| Workflow contract passes | `test-workflow-contract.sh` keeps scenario, schema, validator, runner, and documentation workflow lists aligned. |
| Save proof contract passes | `test-save-menu-dialog-write-proof-contract.sh` verifies the bounded Save proof contract, canonical artifact validation, and no shell timeout invocation. |
| Silver-thread report passes | `test-silver-thread-status-report.sh` reports `status:silver_thread=covered_bounded` for required seams and keeps optional Save/reopen evidence bounded when absent. |
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

Reviewers should attach or cite generated scenario evidence only when the gated
runner path was intentionally used. For this no-wrapper recovery, the durable
requirement is the validated source-contract head plus executable output from
the direct focused Maven selector and the bounded workflow recovery checks.

## Finalization evidence

PR #401 finalization is ready when the validated source-contract head workflow
evidence remains bounded to executable checks and no manual merge is performed.

| Check | Accepted finalization evidence |
| --- | --- |
| Source-contract branch/head | `wave6-ui-action-menu-contract-1778302300` at `ae63ebfb94111aa01b42e5b164dd27687dbd9622`. |
| Scenario validation | `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` validates the checked-in catalog. |
| Schema contract | `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` accepts the schema, argv allowlists, and Save proof no-timeout boundary. |
| Workflow contract | `qa/outside-in/alice-desktop/tests/test-workflow-contract.sh` accepts aligned scenario, schema, validator, runner, and documentation workflow records. |
| Save proof contract | `qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh` accepts canonical Save proof artifact validation and verifies the runner bypasses shell timeout wrappers. |
| Silver-thread status | `qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh` reports `status:silver_thread=covered_bounded` for required seams, explicit non-claim lines, and any optional Save/reopen gap as non-blocking. |
| Focused Java contract | Direct Maven execution of `org.alice.ide.croquet.models.AliceMenuBarContractTest` exits successfully without a timeout wrapper. |
| Final claim | Recover PR #401 through the default workflow as a bounded menu/action contract handoff only. |

Do not turn finalization evidence into broader UI claims. The Save proof contract
check protects the contract and validation surface; it does not prove full
desktop Save behavior. The silver-thread report aggregates required executable
seams; it does not prove full UI automation, rendering correctness, grading,
creative assessment, lesson completion, or full desktop Save completion.

## Blocker register

No blocking source blocker was found for the validated source-contract head. The
observed issues below are non-blocking by scope unless a future exact-head rerun
fails the scenario catalog, schema/workflow contracts, or focused Java contract.

| Blocker | Disposition | Reason |
| --- | --- | --- |
| Repeated rate-limit exits before finalization | Non-blocking for PR #401 evidence | They interrupted prior recovery attempts but do not affect the checked-in contract wiring or the exact-head evidence once regenerated. |
| Full UI automation not available | Non-blocking by scope | PR #401 recovery accepts the menu/action contract only and explicitly does not claim full UI automation. |
| Visible rendering correctness not proven | Non-blocking by scope | The menu/action contract does not inspect pixels, screenshots, or rendered menu correctness. |
| Grading or learner assessment not proven | Non-blocking by scope | PR #401 is not a grading or assessment recovery. |
| Lesson completion not proven | Non-blocking by scope | PR #401 does not claim first-lesson completion or any lesson-completion correctness. |
| Installer, package, or Sims validation not run | Non-blocking by scope | The focused contract uses `-Dinstall4j.skip` and `-DincludeSims=false`; installer and Sims evidence belongs to separate lanes. |
| Later documentation-only handoff commit | Non-blocking for PR #401 source evidence | Documentation commits may record or refine the recovery handoff, but the source contract evidence must come from a source checkout whose `git rev-parse HEAD` is `ae63ebfb94111aa01b42e5b164dd27687dbd9622`. |

Any future failure in `validate-scenarios.sh`, the schema/workflow contract
checks, or the focused `AliceMenuBarContractTest` Maven selector is blocking for
this handoff and must be fixed or documented before PR #401 readiness is cited.

## No-op source justification

No source modification is required for this recovery when the exact-head evidence
above passes. The existing implementation already provides:

1. `AliceMenuBarContractTest` for Window menu model registration, stable
   identity, and menu-bar membership lookup.
2. `menu-action-smoke.yaml` for the checked-in outside-in scenario.
3. `validate-scenarios.sh`, `run-scenario.sh`, `scenario.schema.json`, and shell
   contract tests for the fixed Maven argv and gated command-smoke behavior.

The recovery work is feature-contract review/finalization handoff. Changing
Java, runner, schema, validator, scenario, or test source would only be
justified by a failed exact-head contract check or missing wiring in the
surfaces listed above.

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
ae63ebfb94111aa01b42e5b164dd27687dbd9622.
The accepted evidence is the menu/action scenario contract, the focused Java
contract, the schema/workflow/Save proof contracts, and the silver-thread status
report. The PR #401 claim is limited to WindowMenuModel registration, stable
identity, and menu-bar membership lookup.
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
