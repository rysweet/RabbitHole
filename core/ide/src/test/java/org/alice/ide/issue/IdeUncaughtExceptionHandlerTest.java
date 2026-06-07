package org.alice.ide.issue;

import org.junit.Test;
import org.lgna.croquet.ProcessTerminationRequestedException;
import org.lgna.issue.ApplicationIssueConfiguration;
import org.lgna.issue.swing.JSubmitPane;

import javax.swing.JPanel;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IdeUncaughtExceptionHandlerTest {
  @Test
  public void terminationRequestIsControlFlowNotAnotherCrashReport() throws Exception {
    IdeUncaughtExceptionHandler handler = new TestIdeUncaughtExceptionHandler();
    ProcessTerminationRequestedException request = new ProcessTerminationRequestedException(-1);

    String stderr = captureStandardError(() -> handler.uncaughtException(Thread.currentThread(), request));

    assertEquals("Intentional termination must not increment the crash counter", 0, readIntField(handler, "count"));
    assertFalse("Intentional termination must not be printed as an uncaught exception: " + stderr,
        stderr.contains(ProcessTerminationRequestedException.class.getName()));
  }

  private static int readIntField(Object instance, String fieldName) throws Exception {
    Field field = IdeUncaughtExceptionHandler.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.getInt(instance);
  }

  private static String captureStandardError(ThrowingRunnable runnable) throws Exception {
    PrintStream previous = System.err;
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (PrintStream replacement = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
      System.setErr(replacement);
      runnable.run();
    } finally {
      System.setErr(previous);
    }
    return bytes.toString(StandardCharsets.UTF_8);
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private static final class TestIdeUncaughtExceptionHandler extends IdeUncaughtExceptionHandler {
    private TestIdeUncaughtExceptionHandler() {
      super(new TestApplicationIssueConfiguration());
    }
  }

  private static final class TestApplicationIssueConfiguration implements ApplicationIssueConfiguration {
    @Override
    public String getApplicationName() {
      return "Alice";
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
    }
  }
}
