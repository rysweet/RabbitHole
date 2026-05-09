# Characterize Issue Submission Progress Worker Behavior

Use this guide to add or review focused `IssueSubmissionProgressWorker` characterization while preserving the current issue-reporting background submission behavior.

## Contents

- [Prerequisites](#prerequisites)
- [Choose the behavior](#choose-the-behavior)
- [Use the existing seam](#use-the-existing-seam)
- [Keep the boundary narrow](#keep-the-boundary-narrow)
- [Assess scenario applicability](#assess-scenario-applicability)
- [Run validation](#run-validation)
- [Prepare handoff evidence](#prepare-handoff-evidence)
- [Review the result](#review-the-result)

## Prerequisites

Run commands from the repository root. Initialize the grammar submodule before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use `NODE_OPTIONS=--max-old-space-size=32768` for this validation lane when a Node-based workflow invokes Maven:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Choose the behavior

Good issue-reporting worker characterization answers one narrow question:

| Question | Good target |
| --- | --- |
| Is background submission bracketed by progress markers? | Assert `START_MESSAGE`, delegate progress, and `END_MESSAGE` ordering. |
| Is the production builder path preserved? | Confirm by source review that production uses `JSubmitPane.createIssueBuilder()`; assert with a test double that builder creation happens before delegate work runs. |
| Is project attachment intent preserved? | Assert the delegate observes the constructor-provided attachment choice. |
| Does failure stay visible to worker infrastructure? | Assert delegate exceptions propagate and no completion marker is published after the failure. |

Avoid tests that assert Swing painting, screenshots, localized dialog copy beyond the worker contract, external issue-service behavior, or full rendered UI flow.

## Use the existing seam

Extend `IssueSubmissionProgressWorker` inside a test and override only the narrow seams needed for characterization:

```java
private static final class RecordingIssueSubmissionProgressWorker
    extends IssueSubmissionProgressWorker {
  private final List<String> progressMessages = new ArrayList<>();
  private final Thread thread;
  private final Throwable throwable;
  private Issue.Builder capturedIssueBuilder;

  private RecordingIssueSubmissionProgressWorker(
      Thread thread, Throwable throwable, boolean attachProject) {
    super(null, attachProject);
    this.thread = thread;
    this.throwable = throwable;
  }

  @Override
  Issue.Builder createIssueBuilder() {
    return new Issue.Builder()
        .type(IssueType.BUG)
        .description("")
        .steps("")
        .reportedBy("")
        .emailAddress("")
        .threadAndThrowable(this.thread, this.throwable);
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
Thread thread = new Thread("issue-reporting-worker");
Throwable throwable = new IllegalStateException("submission source");
RecordingIssueSubmissionProgressWorker worker =
    new RecordingIssueSubmissionProgressWorker(thread, throwable, true);

Boolean result = worker.do_onBackgroundThread();

assertEquals(Boolean.TRUE, result);
assertEquals(
    List.of("START_MESSAGE", "submission:true", "END_MESSAGE"),
    worker.progressMessages);
assertEquals(IssueType.BUG, worker.capturedIssueBuilder.build().getType());
assertSame(thread, worker.capturedIssueBuilder.build().getThread());
assertSame(throwable, worker.capturedIssueBuilder.build().getThrowable());
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

## Assess scenario applicability

Treat Alice desktop outside-in QA as non-applicable for this worker seam unless an existing scenario directly exercises bug-report submission through `IssueSubmissionProgressWorker`.

Use this decision table when writing review notes or PR evidence:

| Scenario evidence question | Accepted answer |
| --- | --- |
| Does the scenario invoke the bug-report submit path and worker? | Cite the scenario name, runner command, evidence directory, and result. |
| Does the scenario only launch Alice, Save a project, inspect rendering, exercise lessons, or smoke a wrapper command? | Mark scenario evidence as not applicable to this worker seam. |
| Is there no direct worker scenario? | Record `Scenario evidence: not applicable; covered by focused core/issue-reporting Maven characterization instead.` |

Do not claim full UI automation, visible rendering correctness, grading, creative assessment, full lesson completion, real issue-service submission, or project attachment contents from this lane.

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

## Prepare handoff evidence

Before marking a worker-seam PR ready, collect current-head evidence instead of carrying forward stale output:

| Gate | What to record |
| --- | --- |
| Branch head | The branch name and exact `HEAD` SHA that validation used. |
| Diff scope | `git --no-pager diff --name-status origin/develop...HEAD`; explain any file outside the worker, focused test, directly related docs, and explicitly justified validation-wrapper metadata. |
| Focused validation | The exact focused Maven command and result for `IssueSubmissionProgressWorkerTest`. |
| Module validation | The exact full `core/issue-reporting` Maven command and result when production issue-reporting code changed. |
| Docs impact | The reference, how-to, tutorial, or index entries updated, or an explicit no-op justification if docs already matched the behavior. |
| Scenario applicability | A direct worker scenario result, or the explicit non-applicable statement from this guide. |
| Quality audit | At least three SEEK / VALIDATE / FIX cycles, with a clean final cycle. |
| GitHub Actions | Completed green PR checks from the current PR head. |
| Claim boundary | A statement that the evidence is limited to the issue-reporting worker seam and does not prove rendered UI, grading, lesson completion, project archive attachment contents, or real issue-service submission. |

If any gate is missing, write an explicit `NOT_MERGE_READY` blocker with the missing evidence instead of treating green CI as sufficient.

## Review the result

A finished issue-reporting worker characterization has:

| Requirement | Accepted result |
| --- | --- |
| Production behavior preserved | The default worker still uses `JSubmitPane.createIssueBuilder()` and lazy Swing progress-pane creation. |
| Background ordering protected | Tests prove start, delegate, end ordering for a normal delegate return. |
| Failure remains explicit | Tests prove delegate exceptions propagate and do not publish the completion marker. |
| No external side effects | Tests do not contact an issue service, write credentials, or submit a real report. |
| Bounded claims | Documentation and review notes do not claim rendering, grading, or full end-to-end UI automation. |
