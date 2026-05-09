# Tutorial: Trace the IssueSubmissionProgressWorker Characterization

This tutorial walks through the `IssueSubmissionProgressWorker` characterization by running the QA scenario smoke, then tracing the three test methods in `IssueSubmissionProgressWorkerTest` to understand the background submission lifecycle seams.

## What you will do

You will:

1. Validate the QA scenario catalog.
2. Run the focused issue-reporting characterization smoke.
3. Trace the success-path test and its progress message assertions.
4. Trace the attachment opt-out test and its issue builder assertions.
5. Trace the exception-path test and its incomplete progress sequence.

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

The catalog is ready when the validator reports all checked-in scenarios as valid. The list includes `issue-reporting-smoke`, which is the outside-in entry point for this characterization.

## Step 2: Run the focused Maven smoke

Run:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/issue-reporting -am \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

All three tests pass. The surefire report is at `core/issue-reporting/target/surefire-reports/org.lgna.issue.IssueSubmissionProgressWorkerTest.txt`.

## Step 3: Trace the success-path test

Open `core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java`.

Find `backgroundSubmissionPublishesStartThenEndAroundSubmissionResult`:

```java
RecordingIssueSubmissionProgressWorker worker =
    new RecordingIssueSubmissionProgressWorker(thread, throwable, true, false);
Boolean result = worker.do_onBackgroundThread();
```

This test creates a worker with:
- `isProjectAttachmentDesired = true`
- `submissionResult = false` (the submission "fails" but the lifecycle completes)
- No exception configured

When `do_onBackgroundThread()` runs:

1. **`publishProgressMessage("START_MESSAGE")`** — recorded as the first entry in `progressMessages`.
2. **`createIssueBuilder()`** — returns a builder with `IssueType.BUG`, empty user fields, and the given thread/throwable.
3. **`doInternal_onBackgroundThread(issueBuilder)`** — captures the builder, publishes `"submission:true"` (because `isProjectAttachmentDesired` is `true`), returns `false`.
4. **`publishProgressMessage("END_MESSAGE")`** — recorded as the third entry.

The assertions verify:

| Assertion | Purpose |
| --- | --- |
| `assertEquals(Boolean.FALSE, result)` | The submission result propagates from the seam |
| `assertEquals(EXPECTED_PROGRESS_MESSAGES, worker.progressMessages)` | The three-phase lifecycle ran in order: START → submission:true → END |
| `assertEquals(IssueType.BUG, capturedIssue.getType())` | The builder defaults to BUG type |
| `assertEquals("", capturedIssue.getDescription())` | User fields are empty in the test harness |
| `assertNull(capturedIssue.getEnvironment())` | No environment info is set |
| `assertEquals(0, capturedIssue.getAttachments().length)` | No attachments in the default builder |
| `assertSame(thread, capturedIssue.getThread())` | The originating thread is carried through |
| `assertSame(throwable, capturedIssue.getThrowable())` | The originating throwable is carried through |

## Step 4: Trace the attachment opt-out test

Find `backgroundSubmissionCarriesAttachmentOptOutAndSuccessfulResult`:

```java
RecordingIssueSubmissionProgressWorker worker =
    new RecordingIssueSubmissionProgressWorker(thread, throwable, false, true);
```

This test flips both flags:
- `isProjectAttachmentDesired = false` → the progress message becomes `"submission:false"`
- `submissionResult = true` → the return value is `Boolean.TRUE`

The assertions verify:

| Assertion | Purpose |
| --- | --- |
| `assertEquals(Boolean.TRUE, result)` | The successful result propagates |
| `assertEquals(List.of("START_MESSAGE", "submission:false", "END_MESSAGE"), ...)` | The opt-out flag is visible in the progress sequence |
| `assertSame(thread, capturedIssue.getThread())` | Thread identity is preserved regardless of attachment preference |

This proves that `isProjectAttachmentDesired` is carried through the seam without affecting the issue builder structure or the lifecycle framing.

## Step 5: Trace the exception-path test

Find `backgroundSubmissionPropagatesSubmissionExceptionWithoutPublishingCompletion`:

```java
RuntimeException submissionException = new IllegalStateException("submission failed");
RecordingIssueSubmissionProgressWorker worker =
    new RecordingIssueSubmissionProgressWorker(
        thread, throwable, true, false, submissionException);
```

This test configures a `RuntimeException` that the submission seam throws.

When `do_onBackgroundThread()` runs:

1. `publishProgressMessage("START_MESSAGE")` — recorded.
2. `createIssueBuilder()` — the builder is created and captured.
3. `doInternal_onBackgroundThread(issueBuilder)` — publishes `"submission:true"`, then **throws** the configured exception.
4. `publishProgressMessage("END_MESSAGE")` — **never reached**.

The assertions verify:

| Assertion | Purpose |
| --- | --- |
| `assertSame(submissionException, assertThrows(...))` | The exact exception propagates without wrapping |
| `assertEquals(List.of("START_MESSAGE", "submission:true"), ...)` | Only two messages: END is missing because the exception interrupted the lifecycle |
| `assertSame(thread, capturedIssue.getThread())` | The builder was created before the exception |

This proves that submission failures are observable: the missing `END_MESSAGE` signals an incomplete lifecycle to any consumer that reads the progress sequence.

## What you proved

By tracing these three tests, you verified:

1. **Lifecycle ordering** — The START → delegate → END sequence is enforced by the `final` `do_onBackgroundThread()` method.
2. **Seam isolation** — All three seams (`doInternal_onBackgroundThread`, `publishProgressMessage`, `createIssueBuilder`) are independently overridable without touching Swing.
3. **Exception safety** — Submission failures propagate cleanly and leave the progress sequence in an observable incomplete state.
4. **Builder fidelity** — The issue builder carries thread and throwable identity through the seam boundary.

## What this does not prove

- The `JProgressPane` dialog opens, shows messages, and closes correctly on the Event Dispatch Thread.
- The real issue submission HTTP endpoint responds successfully.
- The `JSubmitPane` form captures user input correctly.
- The "run in background" button in `JProgressPane` works.
- That `handleDone_onEventDispatchThread` shows the correct success or failure dialog.
