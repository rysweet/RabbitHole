# Tutorial: Trace a Desktop Action Journey

This tutorial walks through the Alice desktop action characterization lane by tracing current menu/action smoke evidence to the headless-safe Save action flow, then checking the headless launch guard contract.

## What you will do

You will:

1. Validate the Alice desktop QA scenario catalog.
2. Prepare menu/action smoke evidence.
3. Run the focused menu/action contract test.
4. Run the Save operation journey tests.
5. Check the headless launch guard contract.

## Before you start

Open a terminal at the repository root:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

If Node-based orchestration wraps your run, set:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Validate the scenario catalog

Run:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The catalog is ready when the validator reports all checked-in scenarios as valid. The list includes `alice-desktop-menu-action-smoke`, which is the outside-in entry point for menu/action characterization.

## Step 2: List the desktop QA scenarios

Run:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh list
```

Find:

```text
alice-desktop-menu-action-smoke
```

This scenario is a gated command smoke. It does not run a heavy command by accident.

## Step 3: Prepare menu/action evidence

Run:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

Open the generated run directory. The prepared evidence includes:

```text
environment.txt
status.txt
manual-evidence-checklist.txt
```

`status.txt` records `outcome=gated-not-run`. That is the expected result for `--prepare-only`; it means the scenario was prepared for review without executing the gated Maven command.

## Step 4: Run the focused menu/action smoke

Enable gated smokes in a prepared checkout:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

The enabled scenario runs the focused menu/action contract:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

Review the generated `command.log` and `status.txt`. The action smoke is accepted when the command exits successfully and the evidence names `AliceMenuBarContractTest`.

## Step 5: Run the Save action journey tests

Run:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.SaveOperationFlowTest \
  test
```

These tests cover the user-visible Save journey without launching a desktop:

| Tested journey | Expected result |
| --- | --- |
| Writable current file | Saves without prompting and finishes the activity. |
| Prompt cancellation | Cancels the activity and does not save. |
| Backup save | Prompts with the main project base name plus ` Copy`. |
| First current-file Save fails, retry succeeds | Reports the error, hides the wait cursor, prompts again, and finishes after success. |
| First current-file Save fails, retry prompt is canceled | Reports the error, hides the wait cursor, and cancels the activity. |
| First prompted destination fails with a current project | Reports the error, hides the wait cursor, and retries with the current project base name. |
| First prompted destination fails without a current project | Reports the error, hides the wait cursor, and retry cancellation keeps no suggested base name. |

## Step 6: Check the headless launch contract

Desktop action tests are headless-safe because they use seams. Real Alice desktop launch is different: it needs a display.

The guard contract is: in a headless JVM, `EntryPoint.main(String[] args)` throws `IllegalStateException` with `Alice desktop launch requires a graphical environment.` before crash-warning dialogs, Swing setup, root-frame creation, or JavaFX startup. In a graphical environment or under Xvfb, the normal launch path remains available.

Use the outside-in launch scenario when you need launch evidence:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs \
  --timeout-seconds 180
```

A successful launch evidence directory contains:

```text
environment.txt
launch.log
status.txt
xvfb.log
screenshot.png or screenshot.xwd
```

## Step 7: Keep evidence out of commits

Evidence directories are local run artifacts. Keep them for review or attach them to the relevant PR or issue, but do not commit them.

Commit durable documentation, scenario definitions, schema changes, runner changes, and Java characterization tests. Do not commit generated files from `qa/outside-in/alice-desktop/evidence/`.
