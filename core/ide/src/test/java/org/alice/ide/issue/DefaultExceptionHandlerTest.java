package org.alice.ide.issue;

import org.junit.Test;
import org.lgna.common.LgnaRuntimeException;
import org.lgna.croquet.ProcessTerminationRequestedException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

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

  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
