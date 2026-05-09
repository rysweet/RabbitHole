# Trace Issue Submission Progress Worker Behavior

This tutorial walks through the focused issue-reporting worker seam that protects background submission ordering without running rendered UI automation.

## Contents

- [Start with the worker boundary](#start-with-the-worker-boundary)
- [Trace the production entry point](#trace-the-production-entry-point)
- [Trace a successful delegate](#trace-a-successful-delegate)
- [Trace attachment intent](#trace-attachment-intent)
- [Trace a failing delegate](#trace-a-failing-delegate)
- [Run the focused check](#run-the-focused-check)
- [Trace the merge-ready evidence gate](#trace-the-merge-ready-evidence-gate)

## Start with the worker boundary

Open the production worker:

```text
core/issue-reporting/src/main/java/org/lgna/issue/IssueSubmissionProgressWorker.java
```

The background entry point is `do_onBackgroundThread()`. Read it as a small ordered script:

```text
publish START_MESSAGE
create Issue.Builder
run internal submission work
publish END_MESSAGE
return submission result
```

The Swing progress dialog is handled separately on the event-dispatch thread. This tutorial stays on the background side of the seam.

## Trace the production entry point

Open the production caller:

```text
alice-ide/src/main/java/org/alice/ide/issue/AliceIssueConfiguration.java
```

The submit path is intentionally small:

```text
ask whether to attach the current project
if the user does not cancel, mark submit attempted
construct IssueSubmissionProgressWorker(jSubmitPane, option == YES)
execute the worker
```

That flow shows where the attachment intent enters the worker. It does not assert project archive attachment contents, network submission, or rendered dialog correctness.

## Trace a successful delegate

Open the focused characterization:

```text
core/issue-reporting/src/test/java/org/lgna/issue/IssueSubmissionProgressWorkerTest.java
```

The recording worker overrides `publishProgressMessage(String)` so the test can inspect progress without opening Swing UI. For a successful delegate return, the protected order is:

```text
START_MESSAGE
submission:true
END_MESSAGE
```

The returned `Boolean` is the delegate result. A `true` delegate returns `Boolean.TRUE`; a `false` delegate returns `Boolean.FALSE`.

## Trace attachment intent

The worker constructor receives `isProjectAttachmentDesired`. The test delegate publishes that value as part of its recorded progress:

```text
submission:true
submission:false
```

This shows the background submission implementation can observe the user's attachment choice. It does not assert project archive attachment contents or external issue-service behavior.

## Trace a failing delegate

The failure characterization supplies a delegate that throws after publishing its own progress. The expected messages are:

```text
START_MESSAGE
submission:true
```

There is no `END_MESSAGE` after the exception. The exception propagates to the caller, which keeps the worker infrastructure responsible for failure handling instead of converting the failure into a successful-looking completion.

## Run the focused check

From the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest \
  test
```

Use the broader `core/issue-reporting` module test command when the change affects more than this worker:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/issue-reporting -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## Trace the merge-ready evidence gate

The PR #428 gate is a second seam around review evidence. It does not test more product behavior. It verifies that the handoff for this worker change is current, bounded, and honest.

Open the gate:

```text
scripts/pr428_merge_ready_gate.py
```

Read the evidence checks as a checklist:

```text
branch head matches remote PR head
base SHA is recorded in handoff or PR-body evidence
diff files stay inside the worker lane
focused worker Maven command passed on this head
docs impact was assessed
desktop scenario evidence is direct or explicitly not applicable
three SEEK / VALIDATE / FIX cycles are recorded
GitHub checks collected after branch/head verification are completed and green, skipped, or neutral
PR description contains the same current-head evidence and bounded non-claims
```

Then run the gate against the evidence package:

```bash
python3 scripts/pr428_merge_ready_gate.py /path/to/pr428-evidence.json
```

A ready result is explicit:

```json
{
  "blockers": [],
  "ready": true
}
```

A blocked result starts each reason with `NOT_MERGE_READY`. For example, stale validation from an older commit blocks the PR even if GitHub checks are green:

```json
{
  "blockers": [
    "NOT_MERGE_READY: runnable evidence must be from the current head <current-pr-head-sha>, got <stale-sha>"
  ],
  "ready": false
}
```

The gate validates the PR head through the branch evidence and runnable evidence. It does not validate a separate base-SHA field or a check-run head SHA, so keep those as explicit handoff notes collected after branch/head verification. Treat a blocked gate as an evidence problem to fix on the current PR branch, not as permission to widen this worker seam or recreate the PR.
