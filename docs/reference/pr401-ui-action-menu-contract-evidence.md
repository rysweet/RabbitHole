# PR #401 UI Action Menu Contract Handoff

This reference defines the bounded feature contract for recovering RabbitHole PR
#401 as a UI action menu handoff. It ties readiness and review evidence to exact
head `0366dfa17f0f41e2d878c293a6c33fb1f841993a`.

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
reviewed head.

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
| Reviewed head | `0366dfa17f0f41e2d878c293a6c33fb1f841993a` |
| Scenario | `alice-desktop-menu-action-smoke` |
| Workflow | `menu-action-smoke` |
| Automation mode | `gated-command-smoke` |
| Focused Java contract | `org.alice.ide.croquet.models.AliceMenuBarContractTest` |
| Accepted artifacts | `status.txt`, `command.log`, Maven/Surefire output naming `AliceMenuBarContractTest` |
| Accepted scope | Window menu model registration, stable identity, and menu-bar membership lookup |
| Handoff outcome | Documentation/test recovery only; no Java, runner, schema, validator, or scenario source change is required at the reviewed head. |

This handoff applies only to the reviewed head named above. If a reviewer checks a
different commit, refresh this document with the new exact SHA and rerun the
same focused evidence commands before citing readiness.

Documentation-only commits that add or refine this handoff are not source
evidence for PR #401. The reviewed checkout for the source contract must report
`0366dfa17f0f41e2d878c293a6c33fb1f841993a` from `git rev-parse HEAD` before the
scenario catalog, gated smoke, or focused Java contract evidence is cited.

## Usage

Use the scenario catalog validator first:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Run the gated menu/action smoke from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/pr401-menu-action
```

Run the focused Java contract directly when a reviewer wants the Maven selector
separate from the outside-in wrapper:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

## Configuration

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserved orchestration preference for QA shell and Node-backed tooling. |
| `ALICE_QA_RUN_GATED_SMOKES` | `1` | Enables `gated-command-smoke` execution instead of prepare-only or gated-not-run evidence. |
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
all entries in this table are true for head
`0366dfa17f0f41e2d878c293a6c33fb1f841993a`.

| Requirement | Evidence |
| --- | --- |
| Exact head identified | `git rev-parse HEAD` reports `0366dfa17f0f41e2d878c293a6c33fb1f841993a` in the reviewed checkout. |
| Scenario catalog valid | `validate-scenarios.sh` accepts the checked-in desktop QA scenario catalog, including `menu-action-smoke`. |
| Runner contract present | `run-scenario.sh` accepts the fixed `alice-desktop-menu-action-smoke` argv shape and rejects ad hoc shell command expansion. |
| Schema and allowlist present | `scenario.schema.json`, `validate-scenarios.sh`, and shell contract tests allow the focused `AliceMenuBarContractTest` argv. |
| Gated smoke passes | `status.txt` records `scenario=alice-desktop-menu-action-smoke`, `automationMode=gated-command-smoke`, `outcome=passed`, and `exitCode=0`. |
| Command evidence names contract | `command.log` contains the Maven command selecting `org.alice.ide.croquet.models.AliceMenuBarContractTest`. |
| Focused Java contract passes | The direct Maven selector for `AliceMenuBarContractTest` exits successfully. |
| Claim boundary preserved | Evidence wording stays limited to Window menu model registration, stable identity, and menu-bar membership lookup. |

## Review evidence

The review evidence for head `0366dfa17f0f41e2d878c293a6c33fb1f841993a` is:

```text
Reviewed head: 0366dfa17f0f41e2d878c293a6c33fb1f841993a
Scenario: alice-desktop-menu-action-smoke
Workflow: menu-action-smoke
Automation mode: gated-command-smoke
Focused contract: org.alice.ide.croquet.models.AliceMenuBarContractTest
Accepted result: scenario catalog valid, gated smoke passed, focused Java contract passed
Accepted claim: Window menu model registration, stable identity, and menu-bar membership lookup only
Source blocker: none found in the scenario, runner, validator, schema, allowlist, or Java contract surfaces
Source outcome: no source change required for PR #401 recovery
Handoff outcome: documentation/test recovery only; no Java, runner, schema, validator, or scenario source change required
```

Reviewers should attach or cite the generated scenario evidence directory rather
than copying full logs into durable docs. The durable requirement is that the
attached evidence names the exact head and includes `status.txt`, `command.log`,
and Maven/Surefire output naming `AliceMenuBarContractTest`.

## Blocker register

No blocking source blocker was found for the reviewed head. The observed issues
below are non-blocking by scope unless a future exact-head rerun fails the
scenario catalog, gated smoke, or focused Java contract.

| Blocker | Disposition | Reason |
| --- | --- | --- |
| Repeated rate-limit exits before finalization | Non-blocking for PR #401 evidence | They interrupted prior recovery attempts but do not affect the checked-in contract wiring or the exact-head evidence once regenerated. |
| Full UI automation not available | Non-blocking by scope | PR #401 recovery accepts the menu/action contract only and explicitly does not claim full UI automation. |
| Visible rendering correctness not proven | Non-blocking by scope | The menu/action contract does not inspect pixels, screenshots, or rendered menu correctness. |
| Grading or learner assessment not proven | Non-blocking by scope | PR #401 is not a grading or assessment recovery. |
| Lesson completion not proven | Non-blocking by scope | PR #401 does not claim first-lesson completion or any lesson-completion correctness. |
| Installer, package, or Sims validation not run | Non-blocking by scope | The focused contract uses `-Dinstall4j.skip` and `-DincludeSims=false`; installer and Sims evidence belongs to separate lanes. |
| Later documentation-only handoff commit | Non-blocking for PR #401 source evidence | Documentation commits may record the recovery handoff, but the source contract evidence must come from a checkout whose `git rev-parse HEAD` is `0366dfa17f0f41e2d878c293a6c33fb1f841993a`. |

Any future failure in `validate-scenarios.sh`, the gated scenario run, or the
focused `AliceMenuBarContractTest` Maven selector is blocking for this handoff
and must be fixed or documented before PR #401 readiness is cited.

## No-op source justification

No source modification is required for this recovery when the exact-head evidence
above passes. The existing implementation already provides:

1. `AliceMenuBarContractTest` for Window menu model registration, stable
   identity, and menu-bar membership lookup.
2. `menu-action-smoke.yaml` for the checked-in outside-in scenario.
3. `validate-scenarios.sh`, `run-scenario.sh`, `scenario.schema.json`, and shell
   contract tests for the fixed Maven argv and gated command-smoke behavior.

The recovery work is feature-contract finalization and documentation. Changing
Java, runner, schema, or scenario source would only be justified by a failed
exact-head contract check or missing wiring in the surfaces listed above.

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
PR #401 readiness was reviewed at head 0366dfa17f0f41e2d878c293a6c33fb1f841993a.
The accepted evidence is the menu/action command smoke plus
AliceMenuBarContractTest. The claim is limited to WindowMenuModel registration,
stable identity, and menu-bar membership lookup.
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
