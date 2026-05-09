# Issue Reporting Progress Worker Coverage

This reference defines the focused `core/issue-reporting` coverage improvement
for `IssueSubmissionProgressWorker`.

The executable proof is
`org.lgna.issue.IssueSubmissionProgressWorkerTest`. It characterizes the
background issue-submission worker without launching dialogs, touching the
network, or collecting real diagnostics. The proof is intentionally scoped to
`core/issue-reporting`; it is not evidence for Save, render, export, runtime, or
player behavior.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [Protected seams](#protected-seams)
- [Configuration](#configuration)
- [Evidence boundaries](#evidence-boundaries)
- [Examples](#examples)

## Scope

The coverage improvement is exactly one focused JUnit test:

```text
org.lgna.issue.IssueSubmissionProgressWorkerTest#backgroundSubmissionPublishesStartThenEndAroundSubmissionResult
```

The test covers the deterministic background path:

```text
new IssueSubmissionProgressWorker(submitPane, attachProject)
  -> initial SwingWorker state is PENDING
  -> do_onBackgroundThread() publishes START_MESSAGE
  -> owner.createIssueBuilder() creates the issue builder
  -> doInternal_onBackgroundThread(builder) receives that builder
  -> the protected `isProjectAttachmentDesired` attachment preference is visible to the submission seam
  -> a false seam result is returned as false
  -> END_MESSAGE is published after the submission seam returns normally
```

The test uses synthetic issue data only. It asserts the created issue type,
thread, throwable, progress-message order, and failure result. It does not submit
an issue remotely and does not prove any UI dialog behavior.

## Usage

Run the focused `core/issue-reporting` validation from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

To run only the new proof while reviewing the ratchet:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

The scoped result to report is the focused `core/issue-reporting` test outcome.
Do not use this lane to claim a repository-wide aggregate coverage percentage or
an aggregate target.

## Behavior contract

`IssueSubmissionProgressWorkerTest` owns the executable claim.

| Observation | Required behavior |
| --- | --- |
| Initial state | A newly constructed worker is still `SwingWorker.StateValue.PENDING` before background execution. |
| Start progress | `do_onBackgroundThread()` publishes `START_MESSAGE` before invoking the submission seam. |
| Issue builder creation | The worker asks its `JSubmitPane` owner to create the `Issue.Builder`. |
| Builder contents | The built issue is a bug report and keeps the exact thread and throwable supplied to the submit pane. |
| Attachment preference | The same-package recording subclass observes the protected `isProjectAttachmentDesired` attachment preference in the submission seam. |
| Failure result | A deterministic false result from the submission seam is returned as `false`; it is not coerced into success. |
| End progress | `END_MESSAGE` is published after the submission seam returns normally. |
| Progress order | The asserted order is `START_MESSAGE`, the seam progress message, then `END_MESSAGE`. |

The proof fails closed if the worker skips the start message, reorders the end
message, drops the issue builder, loses the original thread or throwable, hides a
false result, or changes the protected attachment preference observed by the
submission seam.

## Protected seams

These implementation and test seams are inside `core/issue-reporting`. They are
not Alice product APIs.

### `IssueSubmissionProgressWorker(JSubmitPane owner, boolean isProjectAttachmentDesired)`

Creates a background worker for one submit pane. The `owner` supplies the issue
builder. The `isProjectAttachmentDesired` flag records whether the current
submission path should attach the project.

### `protected final boolean isProjectAttachmentDesired`

Records the constructor attachment preference for subclasses in the same package.
The characterization observes this field from the deterministic submission seam;
it does not prove real project attachment behavior.

### `protected final Boolean do_onBackgroundThread()`

Runs the background submission sequence. The method publishes the start marker,
creates the issue builder from the owner, delegates submission work to
`doInternal_onBackgroundThread(Issue.Builder)`, publishes the end marker, and
returns the delegated Boolean result.

The method is final so tests and subclasses observe the same production wrapper
ordering.

### `protected Boolean doInternal_onBackgroundThread(Issue.Builder issueBuilder)`

Runs the submission body between the start and end progress markers. The default
implementation publishes diagnostic progress and returns `true` after the
existing simulated progress loop.

Tests may override this method to make the submission body deterministic, avoid
sleeping, capture the builder, and return a controlled `true` or `false` result.
Overrides must not perform network submission or collect real user diagnostics.

### `protected void publishProgressMessage(String message)`

Publishes one progress message. Production code routes messages through the
`SwingWorker` progress pipeline. Tests may override this method to record
messages synchronously without opening Swing dialogs.

## Configuration

No Alice product preference, authentication token, remote service, display
server, or coverage threshold is required.

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserves the repository's saved Node-backed orchestration memory setting when Maven is launched through scripts or wrappers. |
| Maven module | `-pl core/issue-reporting -am` | Bounds validation to issue-reporting and required reactor dependencies. |
| Surefire flags | `-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false` | Keeps focused module validation stable when the reactor includes dependencies without matching tests. |

## Evidence boundaries

This documentation and its test may claim only:

- `IssueSubmissionProgressWorker` publishes the start marker, delegated progress,
  and end marker in order for the characterized path.
- The worker creates and passes an issue builder based on the submit pane's
  thread and throwable.
- The protected `isProjectAttachmentDesired` attachment preference reaches the
  submission seam.
- A false delegated result remains a false worker result.
- The focused `core/issue-reporting` Maven command validates the scoped behavior.

This documentation and its test must not claim:

- repository-wide aggregate coverage;
- any aggregate coverage target;
- real issue submission;
- dialog rendering or event-dispatch-thread behavior;
- Save, render, export, runtime, or player behavior;
- authentication, persistence, or remote issue tracker behavior.

## Examples

### Review checklist

When reviewing the test, confirm that it uses a same-package recording subclass
and synthetic `JSubmitPane` inputs. The recording subclass should override only
the protected submission and progress seams, and should observe the protected
attachment-preference field only for deterministic evidence.

Required assertions:

```java
assertEquals(SwingWorker.StateValue.PENDING, worker.getState());
assertFalse(result);
assertEquals(List.of("START_MESSAGE", "submission:true", "END_MESSAGE"), messages);
assertEquals(IssueType.BUG, capturedIssue.getType());
assertSame(thread, capturedIssue.getThread());
assertSame(throwable, capturedIssue.getThrowable());
```

### Scoped report wording

Use scoped wording like this in a pull request or issue comment:

```text
core/issue-reporting: added one behavior-backed
IssueSubmissionProgressWorker characterization covering pending state, ordered
progress publication, issue builder contents, attachment preference propagation,
and false seam-result handling. Validated with the focused
core/issue-reporting Maven test command.
```

Do not append aggregate coverage percentages or target claims to this scoped
report.
