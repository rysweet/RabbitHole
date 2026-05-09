# Tutorial: Trace the Issue Reporting Progress Worker Coverage Proof

This tutorial walks through the implemented `core/issue-reporting`
characterization for `IssueSubmissionProgressWorker`.

For the full contract, see the [Issue Reporting Progress Worker Coverage
reference](../reference/issue-reporting-progress-worker-coverage.md). For the
command checklist, see [Run the Issue Reporting Progress Worker Coverage
Proof](../howto/run-issue-reporting-progress-worker-coverage.md).

## What the proof covers

The proof follows one background worker path:

```text
pending worker
  -> START_MESSAGE
  -> create issue builder from JSubmitPane
  -> deterministic submission seam
  -> false seam result
  -> END_MESSAGE after the seam returns normally
```

This is a behavior-backed coverage improvement because it asserts the worker's
observable state, message order, issue contents, attachment preference, and
failure result. It is not a line-only coverage bump.

## 1. Start from synthetic issue data

The test creates a thread and throwable with test-owned values:

```text
thread: issue-reporting-worker
throwable: IllegalStateException("submission source")
```

Those values are passed through `JSubmitPane` so the worker can create the same
kind of issue builder it uses in production. No real user path, environment
value, credential, host name, or project file is captured.

## 2. Observe the worker before execution

Before invoking background work, the test asserts:

```java
assertEquals(SwingWorker.StateValue.PENDING, worker.getState());
```

This verifies that the characterization starts before the worker has been
scheduled or completed.

## 3. Replace timing with a protected seam

The production wrapper remains the path under test. The recording subclass only
overrides the protected submission and progress seams, and reads the protected
attachment-preference field:

```text
doInternal_onBackgroundThread(builder)
publishProgressMessage(message)
isProjectAttachmentDesired
```

That keeps the proof deterministic. The test does not wait for the Swing event
queue, open the progress dialog, sleep through simulated progress, or submit an
issue remotely.

## 4. Assert ordered progress

The expected progress sequence is:

```text
START_MESSAGE
submission:true
END_MESSAGE
```

`START_MESSAGE` and `END_MESSAGE` come from the production wrapper for the
normal-return path. The middle message comes from the deterministic seam and
records that the protected `isProjectAttachmentDesired` attachment preference
was visible to the submission body.

## 5. Assert issue builder contents

After background execution, the test builds the captured issue and checks:

```java
assertEquals(IssueType.BUG, capturedIssue.getType());
assertSame(thread, capturedIssue.getThread());
assertSame(throwable, capturedIssue.getThrowable());
```

These assertions prove that the worker did not drop or replace the original
diagnostic source while creating the issue builder.

## 6. Assert false result handling

The deterministic submission seam returns `false`. The worker result must also
be false:

```java
assertFalse(result);
```

This protects the failure-result path from being accidentally reported as a
`true` worker result.

## 7. Report only the scoped evidence

The supported report is limited to the component and behavior:

```text
core/issue-reporting: IssueSubmissionProgressWorkerTest covers pending state,
ordered progress publication, issue builder contents, attachment preference
propagation, and false seam-result handling.
```

Do not attach repository-wide aggregate coverage percentages, aggregate targets,
or claims about Save, render, export, runtime, or player behavior to this proof.
