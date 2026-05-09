package org.lgna.issue;

import edu.cmu.cs.dennisc.issue.Issue;
import edu.cmu.cs.dennisc.issue.IssueType;
import org.junit.Test;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class IssueSubmissionProgressWorkerTest {
  private static final List<String> EXPECTED_PROGRESS_MESSAGES = List.of("START_MESSAGE", "submission:true", "END_MESSAGE");

  @Test
  public void backgroundSubmissionPublishesStartThenEndAroundSubmissionResult() throws Exception {
    Thread thread = new Thread("issue-reporting-worker");
    Throwable throwable = new IllegalStateException("submission source");
    RecordingIssueSubmissionProgressWorker worker = new RecordingIssueSubmissionProgressWorker(thread, throwable, true, false);

    assertEquals(SwingWorker.StateValue.PENDING, worker.getState());

    Boolean result = worker.do_onBackgroundThread();

    assertEquals(Boolean.FALSE, result);
    assertEquals(EXPECTED_PROGRESS_MESSAGES, worker.progressMessages);
    Issue capturedIssue = worker.capturedIssueBuilder.build();
    assertEquals(IssueType.BUG, capturedIssue.getType());
    assertEquals("", capturedIssue.getDescription());
    assertEquals("", capturedIssue.getSteps());
    assertEquals("", capturedIssue.getReportedBy());
    assertEquals("", capturedIssue.getEmailAddress());
    assertNull(capturedIssue.getEnvironment());
    assertEquals(0, capturedIssue.getAttachments().length);
    assertSame(thread, capturedIssue.getThread());
    assertSame(throwable, capturedIssue.getThrowable());
  }

  private static final class RecordingIssueSubmissionProgressWorker extends IssueSubmissionProgressWorker {
    private final Boolean submissionResult;
    private final Thread thread;
    private final Throwable throwable;
    private final List<String> progressMessages = new ArrayList<>(EXPECTED_PROGRESS_MESSAGES.size());
    private Issue.Builder capturedIssueBuilder;

    private RecordingIssueSubmissionProgressWorker(Thread thread, Throwable throwable, boolean isProjectAttachmentDesired, boolean submissionResult) {
      super(null, isProjectAttachmentDesired);
      this.thread = thread;
      this.throwable = throwable;
      this.submissionResult = Boolean.valueOf(submissionResult);
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
      return this.submissionResult;
    }

    @Override
    protected void publishProgressMessage(String message) {
      this.progressMessages.add(message);
    }
  }
}
