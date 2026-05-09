# Issue Submission Progress Worker

This reference describes the `core/issue-reporting` background submission worker seam for bug-report progress messages, issue-builder creation, attachment intent, and completion behavior.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Background submission contract](#background-submission-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Scope

`IssueSubmissionProgressWorker` coordinates the issue-reporting background submission path after a user starts bug-report submission from `JSubmitPane`. It is a narrow characterization seam for background work and progress publication. It is not a rendered UI automation contract, a complete external issue-submission integration test, or a grading/assessment claim.

It covers:

| Area | Contract |
| --- | --- |
| Issue-builder creation | The worker obtains its `Issue.Builder` through the owner-compatible creation path before internal submission work runs. |
| Attachment intent | The worker preserves the caller-provided `isProjectAttachmentDesired` value and makes it available to the submission implementation. |
| Progress publishing | The background path publishes `START_MESSAGE`, delegates submission work, then publishes `END_MESSAGE` only after successful delegate return. |
| Failure behavior | Exceptions thrown by submission work propagate; completion progress is not published after a failed submission delegate. |
| Progress pane creation | The Swing progress pane remains lazily created from the event-dispatch progress handling path. |

## Artifact inventory

| Artifact | Purpose |
| --- | --- |
| `core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java` | Production worker that creates issue builders, runs background submission work, publishes progress messages, opens/closes the progress dialog, and handles final user feedback. |
| `core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java` | Focused characterization for background submission ordering, issue-builder content, attachment intent, success result propagation, and exception behavior. |

## Background submission contract

`do_onBackgroundThread()` is the worker-owned background entry point. Its ordered contract is:

1. Publish `START_MESSAGE`.
2. Create an `Issue.Builder`.
3. Run `doInternal_onBackgroundThread(Issue.Builder)`.
4. Publish `END_MESSAGE`.
5. Return the delegate result.

If `doInternal_onBackgroundThread(Issue.Builder)` throws, the exception propagates to the worker infrastructure. In that failure path, `END_MESSAGE` is not published by `do_onBackgroundThread()`.

The event-dispatch progress path interprets messages as follows:

| Message | Event-dispatch behavior |
| --- | --- |
| `START_MESSAGE` | Lazily create the `JProgressPane`, build the "Uploading Bug Report" dialog, add the progress pane, pack, and show it. |
| `END_MESSAGE` | Hide the root window for the progress pane. |
| Any other message | Add the message to the progress pane. |

Final result handling remains Swing-owned:

| Condition | Behavior |
| --- | --- |
| Progress pane backgrounded or owner root not visible | Log `issue submission result:` and the Boolean value. |
| Visible owner and `true` result | Show the success dialog and hide the owner dialog. |
| Visible owner and `false` result | Show the failure dialog and hide the owner dialog. |

## API reference

`IssueSubmissionProgressWorker` belongs to `org.lgna.issue`. The characterization seam is intentionally small and exists to protect behavior while avoiding external submission side effects in tests.

### Constructor

```java
public IssueSubmissionProgressWorker(JSubmitPane owner, boolean isProjectAttachmentDesired)
```

| Parameter | Meaning |
| --- | --- |
| `owner` | Submit pane that owns the worker and supplies the production `Issue.Builder`. |
| `isProjectAttachmentDesired` | Caller-selected project attachment intent used by submission work. |

### `doInternal_onBackgroundThread(Issue.Builder)`

```java
protected Boolean doInternal_onBackgroundThread(Issue.Builder issueBuilder) throws Exception
```

Runs the actual submission work after the builder is created. Production behavior publishes diagnostic progress messages, publishes the attachment intent, emits simple progress counts, and returns `Boolean.TRUE`.

Tests may override this protected method to characterize ordering, result propagation, and exception behavior without opening UI or submitting to an external service.

### `publishProgressMessage(String)`

```java
protected void publishProgressMessage(String message)
```

Publishes a message through the worker progress channel. Tests may override it to record progress messages synchronously.

### `createIssueBuilder()`

```java
Issue.Builder createIssueBuilder()
```

Creates the builder through `owner.createIssueBuilder()`. The method is package-private so issue-reporting tests can replace builder creation without making it a public extension API.

### `hideOwnerDialog()`

```java
public void hideOwnerDialog()
```

Hides the root window that contains the submit pane. This remains part of the existing Swing interaction path and is not used as a headless UI automation assertion.

## Configuration

Run commands from the repository root. Initialize the Tweedle grammar submodule before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

When a surrounding Node-based workflow invokes validation, keep the saved memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

The issue-reporting worker seam does not add product configuration, credentials, tokens, network settings, or new external submission endpoints.

## Validation commands

Run the focused worker characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

Run the full touched module validation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## Compatibility rules

1. The production worker still creates its issue builder through `JSubmitPane`.
2. The progress pane is still created lazily when the event-dispatch progress handler receives `START_MESSAGE`.
3. `START_MESSAGE` is published before submission work.
4. `END_MESSAGE` is published only after submission work returns normally.
5. Submission exceptions propagate instead of being converted into a success-shaped result.
6. Attachment intent remains available to submission work through `isProjectAttachmentDesired`.
7. Tests do not perform real network submission or assert rendered Swing layout, pixels, screenshots, grading, or full end-to-end UI behavior.

## Examples

### Focused test double

```java
private static final class RecordingIssueSubmissionProgressWorker
    extends IssueSubmissionProgressWorker {
  private final List<String> messages = new ArrayList<>();
  private Issue.Builder capturedBuilder;

  private RecordingIssueSubmissionProgressWorker(boolean attachProject) {
    super(null, attachProject);
  }

  @Override
  Issue.Builder createIssueBuilder() {
    return new Issue.Builder()
        .type(IssueType.BUG)
        .description("")
        .steps("")
        .reportedBy("")
        .emailAddress("");
  }

  @Override
  protected Boolean doInternal_onBackgroundThread(Issue.Builder issueBuilder) {
    this.capturedBuilder = issueBuilder;
    this.publishProgressMessage("submission:" + this.isProjectAttachmentDesired);
    return Boolean.TRUE;
  }

  @Override
  protected void publishProgressMessage(String message) {
    this.messages.add(message);
  }
}
```

Expected message ordering for a successful submission delegate:

```text
START_MESSAGE
submission:true
END_MESSAGE
```

Expected message ordering when the submission delegate throws after publishing its own progress:

```text
START_MESSAGE
submission:true
```
