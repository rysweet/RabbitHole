# Characterize Headless-Safe Desktop Actions

Use this guide to add or review desktop action characterization while preserving Alice's current graphical behavior and keeping CI-safe headless boundaries.

## Contents

- [Prerequisites](#prerequisites)
- [Choose the journey](#choose-the-journey)
- [Use the existing seams](#use-the-existing-seams)
- [Apply headless guards](#apply-headless-guards)
- [Update outside-in QA when needed](#update-outside-in-qa-when-needed)
- [Run validation](#run-validation)
- [Review the result](#review-the-result)

## Prerequisites

Run commands from the repository root. Initialize the grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

If a surrounding Node-based orchestrator is running the lane, keep the saved memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Choose the journey

Pick a user-observable desktop action journey that is not already covered by merged characterization. Good candidates answer one of these questions:

| Question | Good characterization target |
| --- | --- |
| Does the user see the same action availability? | Menu registration, action lookup, command gating. |
| Does the user keep control when prompted? | Save/Open/Export cancel behavior and no destructive follow-up after cancel. |
| Does Alice recover from a failed action attempt? | Error reporting, wait cursor cleanup, retry prompt, final finish/cancel outcome. |
| Does CI fail clearly without a display? | Required startup guard at the JavaFX/Swing desktop boundary. |

Avoid tests that assert private layout details, localized strings unrelated to the contract, object identity of temporary UI widgets, or lower-level archive contents already covered by tests that save Alice projects, reopen them, edit them, save again, reopen again, and export them.

## Use the existing seams

### Menu/action registration

Use `AliceMenuBarContractTest` when the behavior is about the top-level Alice desktop menu contract. It verifies the stable user-facing menu order and controller lookup registration without launching Swing or JavaFX.

Run it directly:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

### Save, Save As, and Export flow

Use `SaveOperationFlow` for project Save, Save As, and Export journey behavior. This seam keeps tests headless-safe while preserving the production adapter through `AbstractSaveOperation`.

Prefer scenarios shaped like:

```text
Given a writable current project file
When Save runs
Then no prompt is shown
And the save callback receives the current file
And the wait cursor is shown and hidden
And the UserActivity is finished
```

For cancellation:

```text
Given Save As prompts for a destination
When the user cancels the prompt
Then no save/export callback runs
And the UserActivity is canceled
And no wait cursor is shown
```

For retry paths, characterize the current suggestion and outcome without changing the desktop behavior:

```text
Given the first Save attempt throws IOException
When Alice reports the error
Then the wait cursor is hidden
And Alice prompts again with the current project base name when one exists
And Alice prompts again without a suggested base name when no current file exists
And a later successful attempt finishes the UserActivity
```

Add dedicated flow tests before claiming any additional Save As or Export retry journey beyond these shared prompt, error, wait-cursor, finish, and cancel outcomes.

Run the flow tests directly:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.SaveOperationFlowTest \
  test
```

## Apply headless guards

Add headless guards only at real desktop boundaries that cannot operate without a display. This lane adopts one required guard at `EntryPoint.main(String[] args)`.

Use this behavior contract:

| Environment | Expected behavior |
| --- | --- |
| Graphical desktop or Xvfb | Existing Alice startup path runs. |
| `GraphicsEnvironment.isHeadless() == true` | Alice throws `IllegalStateException` with `Alice desktop launch requires a graphical environment.` before crash-warning dialogs, Swing setup, root-frame creation, or JavaFX launch work. |

Do not catch broad Swing or JavaFX exceptions and convert them into success. Do not skip action-flow tests just because the current JVM is headless; action-flow tests should use seams that do not need a display.

## Update outside-in QA when needed

Use the existing menu/action smoke scenario when the change is covered by menu registration or action lookup:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Enable the gated command only in a prepared checkout:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Add a new outside-in scenario only when the behavior has a distinct user journey not represented by the existing catalog. If a scenario changes, update the scenario YAML, schema, validator, runner contract tests, and docs together.

## Run validation

Run the outside-in QA checks:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/runners/run-scenario.sh list
qa/outside-in/alice-desktop/tests/run-tests.sh
```

Run the touched Maven module:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  test
```

If the change touches NetBeans JavaFX launcher/export behavior, also run:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  test
```

## Review the result

A finished desktop action characterization has:

| Requirement | Accepted result |
| --- | --- |
| Behavior preservation | Graphical Alice behavior is unchanged unless the change is explicitly a headless diagnostic. |
| Headless safety | CI either runs display-free tests through seams or fails desktop launch clearly before Swing/JavaFX startup. |
| Meaningful assertions | Tests assert prompt, cancel, retry, wait-cursor, action registration, or activity outcome behavior. |
| Outside-in evidence | Any QA scenario change validates through the checked-in runners and tests. |
| No duplicate lane work | The change builds on existing menu/action, archive, Tweedle, coverage, export/package, and resource-manifest characterization instead of recreating it. |
