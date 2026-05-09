package org.lgna.issue;

import edu.cmu.cs.dennisc.issue.Issue;
import edu.cmu.cs.dennisc.issue.IssueType;
import org.junit.Test;
import org.lgna.issue.swing.JSubmitPane;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class IssueSubmissionProgressWorkerTest {
  @Test
  public void backgroundSubmissionPublishesStartThenEndAroundSubmissionResult() throws Exception {
    Thread thread = new Thread("issue-reporting-worker");
    Throwable throwable = new IllegalStateException("submission source");
    JSubmitPane submitPane = new JSubmitPane(thread, throwable, throwable, new TestIssueConfiguration());
    RecordingIssueSubmissionProgressWorker worker = new RecordingIssueSubmissionProgressWorker(submitPane, true, false);

    assertEquals(SwingWorker.StateValue.PENDING, worker.getState());

    Boolean result = worker.do_onBackgroundThread();

    assertFalse(result);
    assertEquals(List.of("START_MESSAGE", "submission:true", "END_MESSAGE"), worker.progressMessages);
    Issue capturedIssue = worker.capturedIssueBuilder.build();
    assertEquals(IssueType.BUG, capturedIssue.getType());
    assertEquals("", capturedIssue.getDescription());
    assertEquals("", capturedIssue.getSteps());
    assertEquals("", capturedIssue.getReportedBy());
    assertEquals("", capturedIssue.getEmailAddress());
    assertEquals(0, capturedIssue.getAttachments().length);
    assertSame(thread, capturedIssue.getThread());
    assertSame(throwable, capturedIssue.getThrowable());
  }

  private static final class RecordingIssueSubmissionProgressWorker extends IssueSubmissionProgressWorker {
    private final boolean submissionResult;
    private final List<String> progressMessages = new ArrayList<>();
    private Issue.Builder capturedIssueBuilder;

    private RecordingIssueSubmissionProgressWorker(JSubmitPane owner, boolean isProjectAttachmentDesired, boolean submissionResult) {
      super(owner, isProjectAttachmentDesired);
      this.submissionResult = submissionResult;
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

  private static final class TestIssueConfiguration implements ApplicationIssueConfiguration {
    @Override
    public String getApplicationName() {
      return "Test";
    }

    @Override
    public String getDownloadUrlSpec() {
      return "https://example.invalid/download";
    }

    @Override
    public String getDownloadUrlText() {
      return "download";
    }

    @Override
    public String getSubmitActionName() {
      return "Submit";
    }

    @Override
    public JPanel createHeaderPane(Thread thread, Throwable originalThrowable, Throwable originalThrowableOrTarget) {
      return new JPanel();
    }

    @Override
    public void submit(JSubmitPane jSubmitPane) {
      throw new AssertionError("submit action is not used by this worker test");
    }
  }
}
