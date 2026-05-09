# Characterize IssueSubmissionProgressWorker Behavior

Use this guide to add or review characterization tests for the `IssueSubmissionProgressWorker` background submission lifecycle in `core/issue-reporting`.

## Contents

- [Prerequisites](#prerequisites)
- [Understand the test seams](#understand-the-test-seams)
- [Add a characterization test](#add-a-characterization-test)
- [Run the focused tests](#run-the-focused-tests)
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

## Understand the test seams

`IssueSubmissionProgressWorker` has three overridable seams that allow headless characterization without Swing, HTTP, or the Event Dispatch Thread:

| Seam | Type | Production behavior | Test override |
| --- | --- | --- | --- |
| `doInternal_onBackgroundThread(Issue.Builder)` | Protected method | Publishes builder toString and attachment flag, then numbered progress messages with `Thread.sleep` calls | Captures the builder, publishes a deterministic marker, returns a configurable result or throws |
| `publishProgressMessage(String)` | Protected method | Delegates to `SwingWorker.publish(String)` for EDT delivery | Records messages into a `List<String>` |
| `createIssueBuilder()` | Package-private method | Delegates to `JSubmitPane.createIssueBuilder()` | Returns a builder with known thread, throwable, and empty user fields |

The test subclass `RecordingIssueSubmissionProgressWorker` overrides all three seams and passes `null` as the `JSubmitPane` owner, avoiding any Swing dependency.

## Add a characterization test

1. Open `core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java`.

2. Add a new `@Test` method. Follow the existing pattern:

```java
@Test
public void yourNewBehaviorDescription() throws Exception {
  Thread thread = new Thread("issue-reporting-worker-your-case");
  Throwable throwable = new IllegalStateException("your source");
  RecordingIssueSubmissionProgressWorker worker =
      new RecordingIssueSubmissionProgressWorker(
          thread, throwable,
          /* isProjectAttachmentDesired */ true,
          /* submissionResult */ true);

  Boolean result = worker.do_onBackgroundThread();

  // Assert the expected progress message sequence
  assertEquals(EXPECTED_PROGRESS_MESSAGES, worker.progressMessages);

  // Assert issue builder state
  Issue capturedIssue = worker.capturedIssueBuilder.build();
  assertSame(thread, capturedIssue.getThread());
  assertSame(throwable, capturedIssue.getThrowable());

  // Assert the return value
  assertEquals(Boolean.TRUE, result);
}
```

3. If your test exercises an exception path, use the five-argument `RecordingIssueSubmissionProgressWorker` constructor:

```java
RecordingIssueSubmissionProgressWorker worker =
    new RecordingIssueSubmissionProgressWorker(
        thread, throwable,
        /* isProjectAttachmentDesired */ true,
        /* submissionResult (ignored when exception is set) */ false,
        /* submissionException */ new IllegalStateException("expected failure"));

assertSame(submissionException,
    assertThrows(RuntimeException.class, worker::do_onBackgroundThread));

// END_MESSAGE is NOT published on the exception path
assertEquals(List.of("START_MESSAGE", "submission:true"), worker.progressMessages);
```

4. Choose what to assert carefully. This characterization lane proves the **background thread lifecycle contract**, not Swing rendering or HTTP submission:

| Assert | Do not assert |
| --- | --- |
| Progress message order and content | JProgressPane dialog visibility |
| Issue builder field values (type, thread, throwable) | HTTP response codes from a real server |
| `isProjectAttachmentDesired` carry-through | JSubmitPane form validation |
| Exception propagation behavior | SwingWorker state machine transitions |
| Return value from `doInternal_onBackgroundThread` | JOptionPane message text in `handleDone` |

## Run the focused tests

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/issue-reporting -am \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

To run the full `core/issue-reporting` module test suite:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -pl core/issue-reporting -am \
  test
```

## Update outside-in QA when needed

If you change the test class name or Maven coordinates, update the automation argv in all 5 plumbing files. See the [QA scenario contract](../reference/issue-submission-progress-worker.md#qa-scenario-contract) in the reference for the exact list.

If your changes add a new scenario (not just new test methods within the existing class), follow the [Alice desktop outside-in QA how-to](./alice-desktop-outside-in-qa.md) to register the new workflow value, update the schema, validator, runner, and contract test.

## Run validation

After any changes, run all three validation layers:

```bash
# Maven characterization tests
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/issue-reporting -am \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test

# QA scenario validation
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

# Schema contract test
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

All three must pass before the change is merge-ready.

## Review the result

Check that:

1. **Progress message ordering** — Every test asserts the exact `List<String>` sequence. `START_MESSAGE` is always first. `END_MESSAGE` is last on the success path and absent on the exception path.

2. **Issue builder fields** — The captured `Issue.Builder` carries the correct `IssueType`, thread, and throwable. User-supplied fields (description, steps, reportedBy, emailAddress) are empty in the test harness.

3. **No Swing dependency** — The test passes `null` for the `JSubmitPane` owner and overrides all three seams. No test method touches the Event Dispatch Thread.

4. **QA plumbing sync** — `validate-scenarios.sh` reports the expected scenario count and `test-schema-contract.sh` passes with the exact argv tuple set.
