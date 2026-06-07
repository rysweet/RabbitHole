package org.alice.ide.issue;

import org.junit.Test;
import org.lgna.common.LgnaRuntimeException;
import org.lgna.croquet.ProcessTerminationRequestedException;
import org.lgna.croquet.ProcessTerminator;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for {@link DefaultExceptionHandler} and {@link ExceptionHandler}.
 * Tests the exception classification and dispatch logic without triggering GUI dialogs.
 */
public class DefaultExceptionHandlerTest {

  /**
   * Test subclass that records which handler was called, avoiding GUI dependencies.
   */
  private static class RecordingExceptionHandler extends ExceptionHandler {
    boolean lgnaHandled = false;
    boolean throwableHandled = false;
    Thread handledThread;
    Throwable handledThrowable;
    LgnaRuntimeException handledLgna;

    @Override
    protected boolean handleLgnaRuntimeException(Thread thread, LgnaRuntimeException lgnare) {
      lgnaHandled = true;
      handledThread = thread;
      handledLgna = lgnare;
      return true;
    }

    @Override
    protected void handleThrowable(Thread thread, Throwable throwable) {
      throwableHandled = true;
      handledThread = thread;
      handledThrowable = throwable;
    }
  }

  private static LgnaRuntimeException createTestLgna(String message) {
    return new LgnaRuntimeException(message) {
      @Override
      protected void appendFormattedString(StringBuilder sb) {
        sb.append(getMessage());
      }
    };
  }

  @Test
  public void uncaughtException_lgnaRuntimeException_routesToLgnaHandler() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    LgnaRuntimeException lgnare = createTestLgna("test error");
    Thread t = Thread.currentThread();

    handler.uncaughtException(t, lgnare);

    assertTrue(handler.lgnaHandled);
    assertFalse(handler.throwableHandled);
    assertSame(t, handler.handledThread);
    assertSame(lgnare, handler.handledLgna);
  }

  @Test
  public void uncaughtException_regularException_routesToThrowableHandler() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    RuntimeException re = new RuntimeException("regular error");
    Thread t = Thread.currentThread();

    handler.uncaughtException(t, re);

    assertFalse(handler.lgnaHandled);
    assertTrue(handler.throwableHandled);
    assertSame(re, handler.handledThrowable);
  }

  @Test
  public void uncaughtException_runtimeWithITEWrappingLgna_unwrapsToLgnaHandler() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    LgnaRuntimeException lgnare = createTestLgna("wrapped lgna");
    InvocationTargetException ite = new InvocationTargetException(lgnare);
    RuntimeException re = new RuntimeException(ite);
    Thread t = Thread.currentThread();

    handler.uncaughtException(t, re);

    assertTrue(handler.lgnaHandled);
    assertSame(lgnare, handler.handledLgna);
  }

  @Test
  public void uncaughtException_runtimeWithITEWrappingNonLgna_routesToThrowable() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    IllegalArgumentException inner = new IllegalArgumentException("not lgna");
    InvocationTargetException ite = new InvocationTargetException(inner);
    RuntimeException re = new RuntimeException(ite);

    handler.uncaughtException(Thread.currentThread(), re);

    assertFalse(handler.lgnaHandled);
    assertTrue(handler.throwableHandled);
    // Original RuntimeException passed since inner isn't LgnaRuntimeException
    assertSame(re, handler.handledThrowable);
  }

  @Test
  public void uncaughtException_runtimeWithNonITECause_routesToThrowable() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    RuntimeException re = new RuntimeException(new IllegalStateException("other"));

    handler.uncaughtException(Thread.currentThread(), re);

    assertFalse(handler.lgnaHandled);
    assertTrue(handler.throwableHandled);
    assertSame(re, handler.handledThrowable);
  }

  @Test
  public void uncaughtException_checkedThrowable_routesToThrowable() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    Throwable t = new Exception("checked");

    handler.uncaughtException(Thread.currentThread(), t);

    assertFalse(handler.lgnaHandled);
    assertTrue(handler.throwableHandled);
  }

  @Test
  public void uncaughtException_error_routesToThrowable() {
    RecordingExceptionHandler handler = new RecordingExceptionHandler();
    Error error = new OutOfMemoryError("oom");

    handler.uncaughtException(Thread.currentThread(), error);

    assertFalse(handler.lgnaHandled);
    assertTrue(handler.throwableHandled);
    assertSame(error, handler.handledThrowable);
  }

  @Test
  public void uncaughtException_lgnaNotHandled_fallsThrough() {
    ExceptionHandler handler = new ExceptionHandler() {
      boolean throwableHandled = false;
      @Override
      protected boolean handleLgnaRuntimeException(Thread thread, LgnaRuntimeException lgnare) {
        return false; // decline to handle
      }
      @Override
      protected void handleThrowable(Thread thread, Throwable throwable) {
        throwableHandled = true;
      }
    };
    LgnaRuntimeException lgnare = createTestLgna("unhandled");
    // When lgna handler returns false, it should fall through to handleThrowable
    handler.uncaughtException(Thread.currentThread(), lgnare);
  }

  // ---- DefaultExceptionHandler specific ----

  @Test
  public void defaultHandler_setTitle_doesNotThrow() {
    DefaultExceptionHandler handler = new DefaultExceptionHandler();
    handler.setTitle("Test Title");
  }

  @Test
  public void defaultHandler_setApplicationName_doesNotThrow() {
    DefaultExceptionHandler handler = new DefaultExceptionHandler();
    handler.setApplicationName("TestApp");
  }

  @Test
  public void defaultHandler_implementsUncaughtExceptionHandler() {
    DefaultExceptionHandler handler = new DefaultExceptionHandler();
    assertTrue(handler instanceof Thread.UncaughtExceptionHandler);
  }

  @Test
  public void defaultHandler_extendsExceptionHandler() {
    DefaultExceptionHandler handler = new DefaultExceptionHandler();
    assertTrue(handler instanceof ExceptionHandler);
  }

  @Test
  public void defaultHandler_treatsProcessTerminationRequestAsControlFlow() throws Exception {
    DefaultExceptionHandler handler = new DefaultExceptionHandler();
    ProcessTerminationRequestedException request = new ProcessTerminationRequestedException(-1);

    String stderr = captureStandardError(() -> handler.uncaughtException(Thread.currentThread(), request));

    assertEquals("Intentional termination must not increment the crash counter", 0, readIntField(handler, "count"));
    assertFalse("Intentional termination must not be printed as an uncaught exception: " + stderr,
        stderr.contains(ProcessTerminationRequestedException.class.getName()));
  }

  @Test
  public void defaultHandler_headlessStartupFailureAttemptsDialogBeforeProcessTerminatorExit() throws Exception {
    org.junit.Assume.assumeTrue("Requires headless mode to avoid blocking on modal dialogs", GraphicsEnvironment.isHeadless());

    DefaultExceptionHandler handler = new DefaultExceptionHandler();
    AtomicInteger exitRequests = new AtomicInteger();
    ProcessTerminator.Handler previousHandler = ProcessTerminator.setHandler(status -> exitRequests.incrementAndGet());

    try {
      HeadlessException thrown = assertThrows(
          HeadlessException.class,
          () -> handler.uncaughtException(Thread.currentThread(), new RuntimeException("boom")));

      assertNotNull(thrown);
      assertEquals("Headless startup failures should still count as one handled crash", 1, readIntField(handler, "count"));
      assertEquals("Headless mode should fail at JOptionPane before ProcessTerminator is reached", 0, exitRequests.get());
    } finally {
      ProcessTerminator.setHandler(previousHandler);
    }
  }

  @Test
  public void defaultHandlerStartupFailurePathUsesProcessTerminatorAndNotSystemExit() throws Exception {
    String source = readHandlerSource("DefaultExceptionHandler.java");
    int dialogIndex = source.indexOf("JOptionPane.showMessageDialog(null, \"Exception occurred before application was able to show window.  Exiting.\")");
    int terminatorIndex = source.indexOf("ProcessTerminator.requestExit(-1)");

    assertTrue("Startup failure path should show the exit dialog", dialogIndex >= 0);
    assertTrue("Startup failure path should delegate termination through ProcessTerminator", terminatorIndex >= 0);
    assertTrue("Startup failure path must attempt the dialog before requesting exit", dialogIndex < terminatorIndex);
    assertFalse("Startup failure path must not call System.exit directly", source.contains("System.exit("));
  }

  private static int readIntField(Object instance, String fieldName) throws Exception {
    Field field = DefaultExceptionHandler.class.getDeclaredField(fieldName);
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
}
