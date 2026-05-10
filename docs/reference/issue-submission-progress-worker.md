# IssueSubmissionProgressWorker Characterization

This reference documents the `IssueSubmissionProgressWorker` background submission lifecycle, its three test seams, the `RecordingIssueSubmissionProgressWorker` characterization harness, the QA scenario contract, and compatibility rules.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Background submission lifecycle](#background-submission-lifecycle)
- [Test seams](#test-seams)
- [API reference](#api-reference)
- [QA scenario contract](#qa-scenario-contract)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Non-claims](#non-claims)

## Scope

This characterization covers the `IssueSubmissionProgressWorker` background thread lifecycle in `core/issue-reporting`. It proves that the worker publishes ordered progress messages (START → submission → END), that the issue builder carries the correct thread, throwable, and attachment opt-in/opt-out state, and that submission exceptions propagate without publishing the END message.

It covers:

| Area | Contract |
| --- | --- |
| Progress message ordering | `do_onBackgroundThread()` publishes `START_MESSAGE` before delegation, `END_MESSAGE` after delegation, with the subclass submission message in between. |
| Issue builder construction | `createIssueBuilder()` provides a builder with the originating thread, throwable, bug type, and empty user-supplied fields. |
| Attachment opt-in/opt-out | The `isProjectAttachmentDesired` flag is carried through to the submission seam and does not affect issue builder structure. |
| Exception propagation | When `doInternal_onBackgroundThread` throws, the exception propagates to the caller and `END_MESSAGE` is never published. |

This characterization does not cover Swing UI rendering, real issue-service HTTP submission, `JProgressPane` dialog display, or `JSubmitPane` form validation.

## Artifact inventory

| Artifact | Purpose |
| --- | --- |
| `core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java` | Production worker. Extends `WorkerWithProgress<Boolean, String>`. Orchestrates the background submission lifecycle with START/END message framing. |
| `core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java` | Characterization tests. Three `@Test` methods exercise the background thread lifecycle through `RecordingIssueSubmissionProgressWorker`. |
| `core/issue-reporting/src/main/java/org/lgna/issue/swing/JProgressPane.java` | Swing progress display. Consumes the `String` chunks published by the worker on the Event Dispatch Thread. |
| `core/issue-reporting/src/main/java/org/lgna/issue/swing/JSubmitPane.java` | Swing submit form. Owns the issue builder factory and triggers submission through `ApplicationIssueConfiguration.submit()`. |
| `qa/outside-in/alice-desktop/scenarios/issue-reporting-smoke.yaml` | Gated outside-in smoke scenario that runs the focused Maven test as a QA evidence path. |

## Background submission lifecycle

The `do_onBackgroundThread()` method runs on a SwingWorker background thread and follows a strict three-phase lifecycle:

```
┌──────────────────────────────────────────────────────────┐
│ do_onBackgroundThread()                                  │
│                                                          │
│  1. publish(START_MESSAGE)                               │
│  2. issueBuilder = createIssueBuilder()                  │
│  3. result = doInternal_onBackgroundThread(issueBuilder)  │
│  4. publish(END_MESSAGE)                                 │
│  5. return result                                        │
│                                                          │
│  If step 3 throws: exception propagates, step 4 skipped │
└──────────────────────────────────────────────────────────┘
```

On the Event Dispatch Thread, `handleProcess_onEventDispatchThread` responds to the published chunks:
- `START_MESSAGE` → opens the progress dialog
- `END_MESSAGE` → hides the progress dialog
- Any other string → appends to the progress text area

On completion, `handleDone_onEventDispatchThread` shows a success or failure `JOptionPane` message, or logs the result if the progress pane was backgrounded.

## Test seams

The characterization tests exercise three protected/package-private seams without touching Swing, HTTP, or the Event Dispatch Thread:

### 1. `doInternal_onBackgroundThread(Issue.Builder)`

**Type:** Protected method override.

The production implementation first publishes the builder's `toString()` and the `isProjectAttachmentDesired` flag, then iterates with `Thread.sleep` calls publishing numbered progress messages. The test subclass `RecordingIssueSubmissionProgressWorker` overrides this to:
- Capture the `Issue.Builder` for assertion
- Publish a deterministic `"submission:<isProjectAttachmentDesired>"` message
- Return a configurable `boolean` result or throw a configurable `RuntimeException`

### 2. `publishProgressMessage(String)`

**Type:** Protected method override.

The production implementation delegates to `SwingWorker.publish(String)`, which requires an active Event Dispatch Thread. The test subclass overrides this to record messages into a `List<String>` for assertion.

### 3. `createIssueBuilder()`

**Type:** Package-private method override.

The production implementation delegates to `JSubmitPane.createIssueBuilder()`. The test subclass overrides this to return a builder with known thread, throwable, and empty user fields.

## API reference

### `IssueSubmissionProgressWorker`

```java
package org.lgna.issue;

public class IssueSubmissionProgressWorker extends WorkerWithProgress<Boolean, String> {
  // Constructor: requires a JSubmitPane owner and attachment preference flag
  public IssueSubmissionProgressWorker(JSubmitPane owner, boolean isProjectAttachmentDesired);

  // Background thread lifecycle (final — delegates to the three seams)
  protected final Boolean do_onBackgroundThread() throws Exception;

  // Seam 1: subclass submission logic
  protected Boolean doInternal_onBackgroundThread(Issue.Builder issueBuilder) throws Exception;

  // Seam 2: progress message publishing
  protected void publishProgressMessage(String message);

  // Seam 3: issue builder construction (package-private)
  Issue.Builder createIssueBuilder();

  // EDT handlers (final)
  protected final void handleProcess_onEventDispatchThread(List<String> chunks);
  protected final void handleDone_onEventDispatchThread(Boolean value);

  // UI helper
  public void hideOwnerDialog();
}
```

### `RecordingIssueSubmissionProgressWorker` (test-only)

```java
// Private inner class of IssueSubmissionProgressWorkerTest
private static final class RecordingIssueSubmissionProgressWorker
    extends IssueSubmissionProgressWorker {

  // Readable fields for assertions
  final List<String> progressMessages;
  Issue.Builder capturedIssueBuilder;

  // Constructor: configurable thread, throwable, attachment flag, result, exception
  RecordingIssueSubmissionProgressWorker(
      Thread thread, Throwable throwable,
      boolean isProjectAttachmentDesired,
      boolean submissionResult,
      RuntimeException submissionException);
}
```

## QA scenario contract

The `issue-reporting-smoke` scenario is a `gated-command-smoke` workflow in the Alice desktop outside-in QA system.

**Scenario file:** `qa/outside-in/alice-desktop/scenarios/issue-reporting-smoke.yaml`

| Field | Value |
| --- | --- |
| `id` | `alice-desktop-issue-reporting-smoke` |
| `workflow` | `issue-reporting-smoke` |
| `automationMode` | `gated-command-smoke` |
| `timeoutSeconds` | `600` |

**Automation command:**

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/issue-reporting -am \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

**Required evidence:**

- `status.txt` recording the gated command outcome.
- `command.log` from the issue-reporting progress worker smoke command.
- Test output or surefire report naming `IssueSubmissionProgressWorkerTest`.

**Plumbing contract:**

The scenario workflow value `issue-reporting-smoke` appears in exactly 5 synchronized locations:

1. `qa/outside-in/alice-desktop/scenarios/issue-reporting-smoke.yaml` — scenario definition
2. `qa/outside-in/alice-desktop/schema/scenario.schema.json` — workflow enum and argv oneOf entry
3. `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` — workflow allowlist and automation tuple
4. `qa/outside-in/alice-desktop/runners/run-scenario.sh` — argv allowlist if-block
5. `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` — expected argv tuple assertion

## Configuration

No special configuration beyond the standard Maven reactor setup:

| Setting | Value | Purpose |
| --- | --- | --- |
| `-DincludeSims=false` | Skip Sims 2 asset processing | Keeps the build fast and avoids license-restricted content. |
| `-Dinstall4j.skip` | Skip installer packaging | Not needed for unit test execution. |
| `-DfailIfNoTests=false` | Allow modules with no tests | Prevents false failures in transitive dependency modules. |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Allow empty test matches | Prevents false failures when `-Dtest=` narrows to a single class. |
| `-pl core/issue-reporting -am` | Focused module with dependencies | Builds only the issue-reporting module and its transitive dependencies. |

## Validation commands

Run all three validation layers from the repository root:

```bash
# 1. Maven characterization tests
git submodule update --init tweedle-lang
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/issue-reporting -am \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test

# 2. QA scenario validation (all scenarios)
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

# 3. Schema contract test (argv plumbing sync)
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

## Compatibility rules

1. **Do not remove or rename the three test seams.** `doInternal_onBackgroundThread`, `publishProgressMessage`, and `createIssueBuilder` are the characterization boundary. Changing their signatures breaks the `RecordingIssueSubmissionProgressWorker` harness.

2. **Do not change the START/END message framing.** The progress dialog lifecycle depends on exact `START_MESSAGE` and `END_MESSAGE` string matching in `handleProcess_onEventDispatchThread`.

3. **Do not add real HTTP submission to the characterization tests.** The tests exercise the background thread lifecycle seams only. Real submission testing requires a separate integration test with a mock server.

4. **Keep the QA plumbing in sync.** Adding, removing, or renaming the `issue-reporting-smoke` workflow requires updating all 5 plumbing files listed in the [QA scenario contract](#qa-scenario-contract) section.

5. **Initialize the tweedle-lang submodule before Maven validation.** The Maven enforcer plugin requires `tweedle-lang/Grammar/TweedleLexer.g4` and `TweedleParser.g4`.

## Non-claims

This characterization does **not** prove:

- That the `JProgressPane` dialog renders correctly or is usable.
- That the real issue-service HTTP endpoint accepts the submission.
- That the `JSubmitPane` form correctly validates user input.
- That the `WorkerWithProgress` base class correctly dispatches between threads.
- That `handleDone_onEventDispatchThread` shows the correct JOptionPane under all conditions.
- That the "run in background" button in `JProgressPane` works correctly.
- Visual rendering, pixel-level correctness, or accessible widget identity.
