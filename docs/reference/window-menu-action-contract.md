# Window Menu Action Contract

This reference defines the finished contract for Alice desktop Window menu model
registration and the bounded menu/action smoke that verifies it.

## Contents

- [Scope](#scope)
- [Contract](#contract)
- [Implementation surfaces](#implementation-surfaces)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Usage](#usage)
- [Evidence](#evidence)
- [Recovery use](#recovery-use)
- [Examples](#examples)
- [Non-claims](#non-claims)
- [Related documentation](#related-documentation)

## Scope

The Window menu action contract covers one deterministic desktop menu-bar
registration seam:

1. The Alice menu-bar model registers exactly one `WindowMenuModel`.
2. The registered model keeps the stable migration identity
   `58a7297b-a5f8-499a-abd1-db6fca4083c8`.
3. The registered model is reachable through menu-bar membership lookup.

The contract is headless-safe. It validates Croquet menu model registration and
membership without launching JavaFX, requiring Xvfb, painting Swing widgets, or
driving a live menu.

## Contract

| Subject | Required behavior |
| --- | --- |
| Menu-bar registration | `MenuBarComposite` adds `WindowMenuModel` to the Alice menu-bar model list exactly once. |
| Registration position | `MenuBarComposite` registers Window after Run and before Help; this is a code-review invariant, not an assertion made by `AliceMenuBarContractTest`. |
| Stable identity | `WindowMenuModel` uses migration UUID `58a7297b-a5f8-499a-abd1-db6fca4083c8`. |
| Membership lookup | `AliceMenuBar.contains(windowMenuModel)` returns true for the registered instance. |
| Test boundary | `AliceMenuBarContractTest` asserts registration, identity, and membership only. |
| Outside-in smoke | `alice-desktop-menu-action-smoke` runs the focused Maven selector only when gated smokes are enabled. |

## Implementation surfaces

| Surface | Role |
| --- | --- |
| `core/ide/src/main/java/org/alice/ide/croquet/models/MenuBarComposite.java` | Owns the Alice menu-bar model list and registers `WindowMenuModel`. |
| `core/ide/src/main/java/org/alice/ide/croquet/models/menubar/WindowMenuModel.java` | Defines the Window menu model and its stable migration identity. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/AliceMenuBarContractTest.java` | Characterizes the registration, UUID, and membership contract. |
| `qa/outside-in/alice-desktop/scenarios/menu-action-smoke.yaml` | Declares the gated outside-in command smoke for the focused contract test. |
| `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` | Validates the scenario catalog and the fixed command shape. |
| `qa/outside-in/alice-desktop/runners/run-scenario.sh` | Prepares evidence and executes the gated command when enabled. |
| `qa/outside-in/alice-desktop/schema/scenario.schema.json` | Publishes the scenario schema and argv allowlist contract. |
| `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` | Protects the schema and scenario command contract. |

## API reference

This is an internal model registration contract, not a public extension API.

### `WindowMenuModel`

Package: `org.alice.ide.croquet.models.menubar`

| Member | Contract |
| --- | --- |
| Constructor | `WindowMenuModel(ProjectDocumentFrame projectDocumentFrame)` creates the Window menu model for the current project frame context. |
| Migration ID | `getMigrationId()` returns `58a7297b-a5f8-499a-abd1-db6fca4083c8`. |
| Menu item creation | `createModels()` may use the current `ProjectDocumentFrame` perspective state when present and remains safe when constructed with `null` for the registration contract test. |

### `MenuBarComposite`

Package: `org.alice.ide.croquet.models`

The Alice menu bar registers `WindowMenuModel` after the Run menu and before the
Help menu. Code that changes menu-bar registration must keep the Window entry
single, discoverable, and identity-stable.

### `AliceMenuBarContractTest`

Package: `org.alice.ide.croquet.models`

The focused test constructs `AliceMenuBar` with a `null` project frame, scans the
model children for `WindowMenuModel`, asserts exactly one match, verifies the
stable migration ID, and checks menu-bar membership lookup.

## Configuration

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserved orchestrator preference for runs launched from Node-backed tooling. |
| `ALICE_QA_RUN_GATED_SMOKES` | Set to `1` only when intentionally executing the gated runner path | Enables execution of gated command smoke scenarios. Without it, `alice-desktop-menu-action-smoke` records `gated-not-run` evidence and exits non-zero unless `--prepare-only` is used. |
| `tweedle-lang` submodule | Initialized | Required before Maven reactor validation in this repository. |
| `-DincludeSims=false` | Maven property | Keeps the focused contract independent of Sims validation. |
| `-Dinstall4j.skip` | Maven property | Avoids installer packaging work for this contract. |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Maven property | Allows focused reactor selection with `-am` when upstream modules do not contain the selected test. |

Initialize the required submodule before validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Usage

Run the focused contract directly from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

Prepare outside-in smoke evidence without executing the gated command:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/menu-action
```

Without `--prepare-only`, a missing gate records `gated-not-run` evidence and
returns non-zero. That failure is intentional: the scenario did not execute the
Maven command.

Execute the gated smoke in a prepared checkout:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/menu-action
```

The scenario command is fixed in `menu-action-smoke.yaml`; reviewers should not
add local-only Maven properties or replace it with ad hoc shell strings.

## Evidence

Accepted evidence names the focused contract test and stays within the
registration boundary.

| Evidence artifact | Accepted content |
| --- | --- |
| `status.txt` | Gated command outcome for `alice-desktop-menu-action-smoke`, including `gated-not-run` when the gate is missing or `--prepare-only` is used. This proves runner/gate behavior only. |
| `command.log` | The fixed Maven argv for `AliceMenuBarContractTest` from `menu-action-smoke.yaml`. This proves command wiring only. |
| Maven/Surefire output | A successful same-head run of `org.alice.ide.croquet.models.AliceMenuBarContractTest`; this is the readiness artifact for the focused contract. |
| Review notes | A bounded statement that Window menu model registration and menu-bar membership lookup are covered. |

Do not publish generated evidence directories as durable docs. Keep generated
evidence under `qa/outside-in/alice-desktop/evidence/` and attach it to the PR
or review thread when needed. `status.txt`, `gated-not-run`, and `command.log`
are not readiness proof unless paired with successful same-head Maven/Surefire
evidence for `AliceMenuBarContractTest`.

## Recovery use

Use this contract as the feature specification for PR #401-style menu/action
recoveries. A recovery is ready only when the same PR head supplies all of these
evidence classes:

| Evidence class | Required proof |
| --- | --- |
| Head alignment | Local `git rev-parse HEAD` equals the PR `headRefOid` from `gh pr view`. |
| GitHub Actions | Required checks are completed successfully for the same head. |
| Runnable contract | The focused Maven selector for `AliceMenuBarContractTest` exits successfully for the same PR head without a timeout wrapper. |
| Scenario wiring | Scenario validation plus schema, workflow, and gated-command shell contracts accept `alice-desktop-menu-action-smoke`. |
| Docs impact | User-facing docs keep the claim limited to Window menu model registration, stable identity, and menu-bar membership lookup. |
| Quality audit | Three `SEEK -> VALIDATE -> FIX` cycles are documented; the final cycle is clean. |
| PR description | The PR body records current-head checks, QA/scenario evidence, docs impact, diff scope, audit cycles, bounded claims, and any `NOT_MERGE_READY` blockers. |

If a gated desktop smoke is unavailable and no equivalent accepted artifact
proves the same contract at the same head, mark the recovery `NOT_MERGE_READY`.
Do not infer live UI behavior from the headless-safe registration test.

## Examples

### Accepted direct Maven evidence

```text
Command: mvn -DincludeSims=false -Dinstall4j.skip -Dsurefire.failIfNoSpecifiedTests=false -pl core/ide -am -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest test
Accepted claim: WindowMenuModel is registered exactly once, keeps migration UUID 58a7297b-a5f8-499a-abd1-db6fca4083c8, and is reachable through Alice menu-bar membership lookup.
```

### Accepted outside-in smoke evidence

```text
Scenario: alice-desktop-menu-action-smoke
Automation mode: gated-command-smoke
Gate: ALICE_QA_RUN_GATED_SMOKES=1
Evidence: status.txt, command.log, Maven/Surefire output naming AliceMenuBarContractTest
Accepted claim: bounded Window menu model registration and membership lookup only.
```

### Review checklist

Use this checklist when reviewing a menu/action-only change:

1. Confirm `WindowMenuModel` appears once in the menu-bar model list.
2. Confirm the Window menu remains after Run and before Help by reviewing `MenuBarComposite`; do not treat this as executable evidence from `AliceMenuBarContractTest`.
3. Confirm the Window menu migration UUID is unchanged.
4. Confirm `AliceMenuBarContractTest` still asserts exact registration, identity, and membership lookup.
5. Confirm outside-in scenario wording and evidence do not claim rendering, live menu interaction, Save completion, first-lesson completion, installer success, Sims validation, or broad decode behavior.

## Non-claims

This contract does not prove:

- Full UI automation.
- Visible rendering correctness.
- Live Swing menu opening or click behavior.
- Save completion.
- First-lesson completion.
- World execution or playback.
- Deployed installer success.
- Grading or learner assessment.
- Sims validation.
- Broad Tweedle or player archive decode behavior.

## Related documentation

- [Headless-safe desktop action characterization](./headless-safe-desktop-action-characterization.md) describes the broader desktop action lane that this focused contract belongs to.
- [Characterize headless-safe desktop actions](../howto/characterize-headless-safe-desktop-actions.md) explains how to add or review adjacent desktop action characterization.
- [Tutorial: Trace a Desktop Action Journey](../tutorials/desktop-action-journey-characterization.md) walks through the outside-in smoke and focused contract test.
- [Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md) documents scenario schema, runner commands, automation modes, and evidence artifacts.
