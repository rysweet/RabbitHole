# Headless-Safe Desktop Action Characterization

This reference is the build contract for the desktop action characterization lane: required headless-safe startup behavior, existing Croquet action-flow seams, outside-in QA evidence, configuration, and compatibility rules.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Headless startup contract](#headless-startup-contract)
- [Desktop action contracts](#desktop-action-contracts)
- [API reference](#api-reference)
- [Outside-in QA usage](#outside-in-qa-usage)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Scope

This lane characterizes observable Alice desktop behavior without changing normal graphical behavior. It documents the headless startup guard, desktop action-flow seams, and outside-in evidence boundaries for the lane.

It covers:

| Area | Contract |
| --- | --- |
| JavaFX/Swing startup | Alice detects a truly headless environment before starting Swing or JavaFX desktop UI work and fails with a clear diagnostic instead of an obscure toolkit stack trace. |
| Menu/action registration | The Alice desktop menu bar registers the Window menu model and exposes it through menu-bar membership lookup. |
| Save and export flow | Save, Save As, and Export keep their prompt, cancel, wait-cursor, retry, and `UserActivity` finish/cancel behavior. |
| Outside-in evidence | Desktop action smoke evidence is collected through the checked-in Alice desktop QA runner, not through ad hoc shell commands. |

This lane builds on the existing Alice desktop outside-in QA lane and the project save/export operation characterization. It does not recreate archive/resource-manifest, Tweedle recovery, coverage-ratchet, NetBeans Ant export/package, or baseline menu/action work that is already covered elsewhere.

## Artifact inventory

| Artifact | Purpose |
| --- | --- |
| `alice-ide/src/main/java/org/alice/stageide/EntryPoint.java` | Desktop startup boundary. The headless guard runs here before Swing or JavaFX launch work. |
| `alice-ide/src/test/java/org/alice/stageide/EntryPointHeadlessGuardTest.java` | Focused characterization that the desktop startup boundary rejects headless launch and allows graphical launch. |
| `core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/SaveOperationFlow.java` | Package-private seam for Save, Save As, and Export flow behavior. |
| `core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/AbstractSaveOperation.java` | Production adapter from Croquet `UserActivity`, active `StageIDE`, document frame dialogs, wait cursor hooks, and save/export callbacks into `SaveOperationFlow`. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/AliceMenuBarContractTest.java` | Headless-safe Window menu model registration characterization. |
| `core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/SaveOperationFlowTest.java` | Headless-safe action journey characterization for direct save, prompt cancel, backup copy naming, retry-after-`IOException`, and cancel-after-failure behavior. |
| `qa/outside-in/alice-desktop/scenarios/menu-action-smoke.yaml` | Gated outside-in smoke scenario for the menu/action contract test. |
| `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` | Scenario catalog validator. |
| `qa/outside-in/alice-desktop/runners/run-scenario.sh` | Scenario runner for listing, preparing, and executing desktop QA scenarios. |

## Headless startup contract

Alice is a desktop application. A real desktop launch requires a graphical environment, either a physical display or a virtual display such as Xvfb.

When `GraphicsEnvironment.isHeadless()` is `false`, Alice preserves the existing startup path:

1. Crash detection runs.
2. Version and theme setup run.
3. Renderer native libraries initialize.
4. Swing setup is queued.
5. `StageIDE` initializes and the desktop frame is shown.
6. JavaFX `Application.launch(args)` runs.

When `GraphicsEnvironment.isHeadless()` is `true`, `EntryPoint.main(String[] args)` fails before Swing or JavaFX desktop initialization. The failure contract is:

| Condition | Behavior |
| --- | --- |
| Headless environment | Alice throws `IllegalStateException` with the exact message `Alice desktop launch requires a graphical environment.` |
| Crash dialog path | No Swing crash-warning dialog is displayed. |
| Swing setup | No Swing look-and-feel setup, `SwingUtilities.invokeLater(...)`, or Swing root-frame creation is attempted. |
| JavaFX startup | `Application.launch(args)` is not called. |
| Desktop frame | No root frame is created or marked visible. |
| Exit result | The launch exits non-zero when invoked from Maven or a command-line process. |

The guard is intentionally narrow. It does not convert Alice into a headless application, and it does not make desktop launch scenarios pass without a display. CI jobs that need launch evidence still run Alice under Xvfb through the outside-in QA runner.

## Desktop action contracts

### Menu/action registration

`AliceMenuBarContractTest` characterizes Window menu registration without requiring a display. The test constructs the desktop menu-bar model, locates the registered `WindowMenuModel`, and verifies that it is reachable through menu-bar membership lookup. It does not assert full menu order, invoke menu actions, validate visible UI rendering, or exercise Save/File-menu behavior.

### Save and export action journey

`SaveOperationFlow` owns the shared action journey for Save, Save As, and Export after the concrete operation supplies the prompt rule, file extension, and save/export callback.

| Journey | Observable behavior |
| --- | --- |
| Save to writable current file | Alice does not prompt, wraps the save callback with wait cursor show/hide, and finishes the `UserActivity`. |
| Prompt cancellation | Alice cancels the `UserActivity`, does not show the wait cursor, and does not call save/export. |
| Backup save | Alice prompts with the main project base name plus ` Copy` and the correct extension. |
| Current-file Save `IOException` retry | Alice reports `Unable to save file`, hides the wait cursor, prompts again with the current project base name, and retries. |
| Current-file Save failure followed by prompt cancellation | Alice reports the error, hides the wait cursor, cancels the `UserActivity`, and does not finish it. |
| Prompted Save As or Export-style `IOException` retry | Alice reports the error, hides the wait cursor, and retries through a prompt. When a current project file exists, the retry suggestion uses that current project base name; when no current file exists, the retry prompt has no suggested base name. |

Save and Save As use the Alice project extension. Export uses the export
extension and delegates to export behavior. Retry behavior is characterized
through the shared flow seam; archive contents remain covered by lower-level
tests that save Alice projects, reopen them, edit them, save again, reopen
again, and export them.

## API reference

The action-flow seam is package-private and belongs to `org.alice.ide.croquet.models.projecturi`. It exists for characterization and refactoring safety; it is not a public extension API.

### `SaveOperationFlow.run(...)`

```java
static void run(
    Context context,
    PromptDecision promptDecision,
    String extension,
    SaveAction saveAction)
```

| Parameter | Meaning |
| --- | --- |
| `context` | Supplies the current file, backup state, dialog behavior, wait cursor hooks, error reporting, and activity outcome hooks. |
| `promptDecision` | Decides whether the current file can be reused without showing a save dialog. |
| `extension` | File extension passed to the save dialog. |
| `saveAction` | Callback that performs the concrete save or export and may throw `IOException`. |

### `SaveOperationFlow.Context`

| Method | Contract |
| --- | --- |
| `getCurrentFile()` | Returns the current project file or `null`. |
| `isBackup()` | Returns whether the active save is a backup-copy save. |
| `getMainProjectFile()` | Returns the main project file used to derive the backup copy name. |
| `getDefaultDirectory()` | Returns the directory used when prompting. |
| `showSaveFileDialog(File, String, String)` | Returns the selected destination, or `null` when the user cancels. |
| `showWaitCursor()` / `hideWaitCursor()` | Wrap each attempted save/export callback. |
| `showError(String, String)` | Reports save/export failure to the user. |
| `finish()` | Finishes the Croquet activity after success. |
| `cancel()` | Cancels the Croquet activity when the user cancels. |

### `AbstractSaveOperation`

`AbstractSaveOperation.perform(UserActivity)` adapts the live Alice desktop state into `SaveOperationFlow`:

| Production collaborator | Flow responsibility |
| --- | --- |
| `StageIDE.getActiveInstance()` | Supplies the active application. |
| `application.getUri()` | Supplies the current file. |
| `application.isBackup()` and `application.getMainProjectFile()` | Supply backup-copy context. |
| `application.getDocumentFrame().showSaveFileDialog(...)` | Supplies the destination chooser. |
| `application.showWaitCursor()` and `application.hideWaitCursor()` | Preserve wait-cursor behavior. |
| `Dialogs.showError(...)` | Preserves the user-visible error prompt. |
| `activity.finish()` and `activity.cancel()` | Preserve Croquet activity outcome. |

## Outside-in QA usage

Use the menu/action smoke scenario to collect reviewable command evidence for launch-adjacent desktop action registration:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

`--prepare-only` records intentional gated smoke preparation. To execute the focused command in a prepared checkout:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The enabled scenario runs the focused `AliceMenuBarContractTest` Maven command and records `command.log`, `status.txt`, and environment evidence.

## Configuration

| Setting | Purpose |
| --- | --- |
| `java.awt.headless` | JVM-level headless detection input. Do not set this to `true` for real Alice desktop launch scenarios. |
| `DISPLAY` | X11 display used by Swing/JavaFX on Linux. Xvfb-backed launch scenarios set or select this through the QA runner. |
| `ALICE_QA_DISPLAY` | Reuse a specific display for outside-in Xvfb runs. |
| `ALICE_QA_SCREEN` | Xvfb screen geometry. Defaults to `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Override readiness wait before screenshot capture. |
| `ALICE_QA_RUN_GATED_SMOKES` | Set to `1` to execute gated command smoke scenarios. |
| `NODE_OPTIONS` | Use `--max-old-space-size=32768` when a surrounding Node-based orchestrator invokes this lane. The Java/Maven tests do not require Node. |

Example:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
export ALICE_QA_SCREEN=1600x1000x24
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

The guard does not add an Alice product preference that disables headless detection. Graphical users keep the normal desktop behavior; headless launches fail clearly.

## Validation commands

Initialize the required grammar submodule before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the focused action characterization:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest,org.alice.ide.croquet.models.projecturi.SaveOperationFlowTest \
  test
```

Run the focused headless startup guard characterization:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl alice-ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.stageide.EntryPointHeadlessGuardTest \
  test
```

Run the outside-in QA lane:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/runners/run-scenario.sh list
qa/outside-in/alice-desktop/tests/run-tests.sh
```

Run the full touched module validation:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide,alice-ide -am \
  -DfailIfNoTests=false \
  test
```

Run NetBeans validation only when the change touches NetBeans launcher, export, package, or JavaFX handoff behavior:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  test
```

## Compatibility rules

1. Graphical Alice startup behavior stays unchanged.
2. Headless desktop startup fails clearly with `IllegalStateException: Alice desktop launch requires a graphical environment.`; it never reports GUI launch success without a display.
3. Xvfb remains the supported way to collect automated launch evidence in CI.
4. The Window menu model remains registered in the desktop menu-bar model.
5. The registered Window menu model remains reachable through menu-bar membership lookup.
6. Save to a writable current file does not prompt.
7. Save As and Export prompt for destinations.
8. Prompt cancellation cancels the `UserActivity` and does not save or export.
9. Successful save/export finishes the `UserActivity`.
10. The characterized current-file Save `IOException` path reports an error, hides the wait cursor, and retries through a prompt.
11. Tests assert user-observable action outcomes and stable seams, not private UI painting details.
12. Outside-in runners execute checked-in argv lists only; arbitrary shell strings are not accepted.

## Examples

### Headless desktop launch

```text
Environment: java.awt.headless=true
Command: cd alice-ide && mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests compile exec:java -Dalice-ide
Result: non-zero launch failure
Exception: IllegalStateException
Diagnostic: Alice desktop launch requires a graphical environment.
JavaFX launch: not attempted
```

### Xvfb desktop launch

```text
Environment: DISPLAY=:99 from Xvfb
Command: qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
Result: launch evidence directory
Artifacts: environment.txt, launch.log, xvfb.log, status.txt, screenshot.png or screenshot.xwd
```

### Save retry journey

```text
Current file: /home/dev/alice-projects/world.a3p
First save: throws IOException("disk full")
User-visible error: Unable to save file
Retry prompt suggestion: world
Second destination: /home/dev/alice-projects/world-retry.a3p
Final result: wait cursor hidden and UserActivity.finish() called
```
