package org.alice.ide.issue;

import org.junit.Test;
import org.lgna.croquet.ProcessTerminationRequestedException;
import org.lgna.croquet.ProcessTerminator;
import org.lgna.issue.ApplicationIssueConfiguration;
import org.lgna.issue.swing.JSubmitPane;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

  @Test
  public void headlessStartupFailureAttemptsDialogBeforeProcessTerminatorExit() throws Exception {
    org.junit.Assume.assumeTrue("Requires headless mode to avoid blocking on modal dialogs", GraphicsEnvironment.isHeadless());

    IdeUncaughtExceptionHandler handler = new TestIdeUncaughtExceptionHandler();
    AtomicInteger exitRequests = new AtomicInteger();
    ProcessTerminator.Handler previousHandler = ProcessTerminator.setHandler(status -> exitRequests.incrementAndGet());

    try {
      String stderr = captureStandardError(() -> handler.uncaughtException(Thread.currentThread(), new RuntimeException("boom")));

      assertEquals("Headless startup failures should still count as one handled crash", 1, readIntField(handler, "count"));
      assertEquals("Headless mode should fail at JOptionPane before ProcessTerminator is reached", 0, exitRequests.get());
      assertTrue("Headless mode should report the dialog failure instead of blocking: " + stderr,
          stderr.contains(HeadlessException.class.getName()));
    } finally {
      ProcessTerminator.setHandler(previousHandler);
    }
  }

  @Test
  public void startupFailurePathUsesProcessTerminatorAndNotSystemExit() throws Exception {
    String source = readHandlerSource("IdeUncaughtExceptionHandler.java");
    int dialogIndex = source.indexOf("JOptionPane.showMessageDialog(null, \"Exception occurred before application was able to show window.  Exiting.\")");
    int terminatorIndex = source.indexOf("ProcessTerminator.requestExit(-1)");

    assertTrue("Startup failure path should show the exit dialog", dialogIndex >= 0);
    assertTrue("Startup failure path should delegate termination through ProcessTerminator", terminatorIndex >= 0);
    assertTrue("Startup failure path must attempt the dialog before requesting exit", dialogIndex < terminatorIndex);
    assertFalse("Startup failure path must not call System.exit directly", source.contains("System.exit("));
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

  private static String readHandlerSource(String fileName) throws IOException {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      Path modulePath = current.resolve("src/main/java/org/alice/ide/issue/").resolve(fileName);
      if (Files.isRegularFile(modulePath)) {
        return Files.readString(modulePath, StandardCharsets.UTF_8);
      }
      Path repositoryPath = current.resolve("core/ide/src/main/java/org/alice/ide/issue/").resolve(fileName);
      if (Files.isRegularFile(repositoryPath)) {
        return Files.readString(repositoryPath, StandardCharsets.UTF_8);
      }
      current = current.getParent();
    }
    throw new AssertionError("Could not locate source for " + fileName);
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
