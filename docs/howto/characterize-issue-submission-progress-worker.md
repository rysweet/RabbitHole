# Characterize Issue Submission Progress Worker Behavior

Use this guide to add or review focused `IssueSubmissionProgressWorker` characterization while preserving the current issue-reporting background submission behavior.

## Contents

- [Prerequisites](#prerequisites)
- [Choose the behavior](#choose-the-behavior)
- [Use the existing seam](#use-the-existing-seam)
- [Keep the boundary narrow](#keep-the-boundary-narrow)
- [Run validation](#run-validation)
- [Review the result](#review-the-result)

## Prerequisites

Run commands from the repository root. Initialize the grammar submodule before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Keep the saved Node memory setting when a Node-based workflow invokes the lane:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Choose the behavior

Good issue-reporting worker characterization answers one narrow question:

| Question | Good target |
| --- | --- |
| Is background submission bracketed by progress markers? | Assert `START_MESSAGE`, delegate progress, and `END_MESSAGE` ordering. |
| Is the production builder path preserved? | Assert the worker creates an `Issue.Builder` before delegate submission work runs. |
| Is project attachment intent preserved? | Assert the delegate observes the constructor-provided attachment choice. |
| Does failure stay visible to worker infrastructure? | Assert delegate exceptions propagate and no completion marker is published after the failure. |

Avoid tests that assert Swing painting, screenshots, localized dialog copy beyond the worker contract, external issue-service behavior, or full rendered UI flow.

## Use the existing seam

Extend `IssueSubmissionProgressWorker` inside a test and override only the narrow seams needed for characterization:

```java
private static final class RecordingIssueSubmissionProgressWorker
    extends IssueSubmissionProgressWorker {
  private final List<String> progressMessages = new ArrayList<>();
  private Issue.Builder capturedIssueBuilder;

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
    this.capturedIssueBuilder = issueBuilder;
    this.publishProgressMessage("submission:" + this.isProjectAttachmentDesired);
    return Boolean.TRUE;
  }

  @Override
  protected void publishProgressMessage(String message) {
    this.progressMessages.add(message);
  }
}
```

Exercise `do_onBackgroundThread()` directly. That keeps the test focused on background ordering and avoids launching the Swing progress dialog:

```java
RecordingIssueSubmissionProgressWorker worker =
    new RecordingIssueSubmissionProgressWorker(true);

Boolean result = worker.do_onBackgroundThread();

assertEquals(Boolean.TRUE, result);
assertEquals(
    List.of("START_MESSAGE", "submission:true", "END_MESSAGE"),
    worker.progressMessages);
assertEquals(IssueType.BUG, worker.capturedIssueBuilder.build().getType());
```

## Keep the boundary narrow

Use this lane for issue-reporting background-submission characterization only:

| Allowed | Not claimed by this lane |
| --- | --- |
| Builder creation before delegate submission. | Real issue tracker submission success. |
| Progress marker ordering. | Rendered progress dialog layout or pixels. |
| Attachment-intent propagation. | Project archive attachment content. |
| Exception propagation from delegate submission work. | Full Alice desktop end-to-end UI automation. |

If a change needs rendered desktop proof, use the Alice desktop outside-in QA lane instead of broadening this worker test.

## Run validation

Run the focused worker characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

Run the full touched module before handing off broader issue-reporting changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## Review the result

A finished issue-reporting worker characterization has:

| Requirement | Accepted result |
| --- | --- |
| Production behavior preserved | The default worker still uses `JSubmitPane.createIssueBuilder()` and lazy Swing progress-pane creation. |
| Background ordering protected | Tests prove start, delegate, end ordering for a normal delegate return. |
| Failure remains explicit | Tests prove delegate exceptions propagate and do not publish the completion marker. |
| No external side effects | Tests do not contact an issue service, write credentials, or submit a real report. |
| Bounded claims | Documentation and review notes do not claim rendering, grading, or full end-to-end UI automation. |
