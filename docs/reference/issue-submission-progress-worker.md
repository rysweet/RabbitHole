# Issue Submission Progress Worker

This reference describes the `core/issue-reporting` background submission worker seam for bug-report progress messages, issue-builder creation, attachment intent, and completion behavior.

## Contents

- [Scope](#scope)
- [Usage flow](#usage-flow)
- [Artifact inventory](#artifact-inventory)
- [Background submission contract](#background-submission-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Workflow evidence requirements](#workflow-evidence-requirements)
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
| Progress pane creation | The Swing progress pane is created lazily on first `getProgressPane()` use, normally from `START_MESSAGE` handling. |

## Usage flow

Production issue submission reaches the worker through the existing submit-pane configuration path:

1. `JSubmitPane` invokes `ApplicationIssueConfiguration.submit(JSubmitPane)` from its submit button action.
2. `AliceIssueConfiguration.submit(JSubmitPane)` asks whether the current project should be attached to the bug report.
3. When the user chooses Yes or No, `JSubmitPane.setSubmitAttempted(true)` records the attempt.
4. `AliceIssueConfiguration` constructs `IssueSubmissionProgressWorker` with the submit pane and the selected attachment intent.
5. `execute()` starts the Swing worker, which runs `do_onBackgroundThread()` and publishes progress for the event-dispatch progress pane.

The focused characterization tests do not drive that rendered flow. They exercise the worker-owned background method directly with a test subclass so they can prove ordering, builder creation, attachment intent, return values, and exception propagation without opening Swing UI or contacting an issue service.

## Artifact inventory

| Artifact | Purpose |
| --- | --- |
| `alice-ide/src/main/java/org/alice/ide/issue/AliceIssueConfiguration.java` | Production submit path that turns the user's project-attachment choice into the worker constructor argument and starts the worker. |
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
| `START_MESSAGE` | Normally creates the `JProgressPane` on first `getProgressPane()` use, builds the "Uploading Bug Report" dialog, adds the progress pane, packs, and shows it. |
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

Runs the current internal submission delegate work after the builder is created. Production behavior publishes diagnostic progress messages, publishes the attachment intent, emits simple progress counts, and returns `Boolean.TRUE`.

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

## Workflow evidence requirements

Use this checklist when preparing or reviewing a PR that only changes the issue-reporting background submission worker seam. Record the current branch and HEAD with the executable validation output; do not carry forward stale results from another checkout or prior session.

| Evidence | Accepted current-head proof |
| --- | --- |
| Branch scope | `git --no-pager diff --name-status origin/develop...HEAD` shows the change is limited to `IssueSubmissionProgressWorker`, its focused test, directly related docs, and any explicitly justified metadata needed for the validation wrapper. |
| Readiness | The focused `IssueSubmissionProgressWorkerTest` command above passes with `NODE_OPTIONS=--max-old-space-size=32768`. Run the full `core/issue-reporting` module command when handing off the PR or when any issue-reporting production code changes. |
| Scenario applicability | Alice desktop outside-in scenarios are non-applicable unless an existing scenario directly exercises this bug-report worker seam. Do not substitute unrelated launch, Save, lesson, render, or wrapper-smoke scenarios as proof for this worker. |
| Review | Source review confirms `createIssueBuilder()` still delegates to `JSubmitPane.createIssueBuilder()`, the progress pane remains lazy through `getProgressPane()`, and `do_onBackgroundThread()` still publishes start, delegates submission work, then publishes completion only after a normal delegate return. |
| Quality audit | Record at least three SEEK / VALIDATE / FIX cycles against the current head. A clean final cycle has no remaining worker, docs, scenario-applicability, diff-scope, or evidence issue requiring a fix. |
| PR description | The PR body names the exact head validated, focused and module validation commands, docs impact, scenario applicability, diff scope, quality-audit cycles, GitHub Actions status, and bounded non-claims. |
| Finalization | `git --no-pager status --short --branch`, `gh pr view 428`, and `gh pr checks 428 --watch=false` describe the open PR state and checks without manually merging the PR. |
| Claim boundary | Handoff notes cite only the worker seam, background ordering, attachment intent, exception propagation, source review, and Maven/PR-check evidence. They do not claim rendered UI automation, real issue-service submission, grading, or full end-to-end coverage. |

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
