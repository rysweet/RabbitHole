# Run the Issue Reporting Progress Worker Coverage Proof

Use this guide to run and review the focused `core/issue-reporting`
characterization for `IssueSubmissionProgressWorker`.

For the full contract, see the [Issue Reporting Progress Worker Coverage
reference](../reference/issue-reporting-progress-worker-coverage.md).

## Before you start

Run commands from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

The proof uses synthetic issue data and the Maven reactor. It does not require a
display server, credentials, remote issue tracker access, or Alice product
configuration.

## Run the focused module validation

Run the canonical command for this component:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

For a narrower review loop, run only the progress-worker proof:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

## Review the proof

Review
`core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java`
for one behavior-backed test:

| Assertion | Meaning |
| --- | --- |
| Worker state is `PENDING` before execution. | The proof starts from the expected `SwingWorker` lifecycle state. |
| Progress messages are `START_MESSAGE`, seam message, `END_MESSAGE`. | The background wrapper publishes progress in the expected order. |
| The captured issue is `IssueType.BUG`. | The submit pane's builder creates the expected issue kind. |
| The captured issue keeps the same thread and throwable. | The worker does not lose the original diagnostic source. |
| The seam observes the attachment preference. | The protected `isProjectAttachmentDesired` constructor flag reaches the submission body. |
| The returned result is `false`. | A `false` seam result is not reported as `true`. |

The recording subclass is the deterministic observation point. It should capture
the builder and progress messages without opening Swing dialogs, sleeping, or
submitting anything remotely.

## Keep the report scoped

When recording the result, cite only the focused component:

```text
core/issue-reporting focused tests passed with
IssueSubmissionProgressWorkerTest covering ordered progress publication, issue
builder contents, attachment preference propagation, and false seam-result handling.
```

Do not report a repository-wide aggregate coverage percentage or an aggregate
target for this lane.
