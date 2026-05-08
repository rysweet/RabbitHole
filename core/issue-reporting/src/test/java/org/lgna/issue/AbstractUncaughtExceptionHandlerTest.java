package org.lgna.issue;

import org.junit.Test;
import org.lgna.common.LgnaRuntimeException;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AbstractUncaughtExceptionHandlerTest {
  @Test
  public void plainThrowableRoutesToGenericHandler() {
    RecordingHandler handler = new RecordingHandler(true);
    Thread thread = new Thread("issue-reporting-plain");
    Throwable throwable = new Throwable("plain");

    recordUncaughtException(handler, thread, throwable);

    assertEquals(List.of(new Callback("generic", thread, throwable, throwable)), handler.callbacks);
  }

  @Test
  public void directLgnaRuntimeExceptionRoutesToLgnaHandlerWhenHandled() {
    RecordingHandler handler = new RecordingHandler(true);
    Thread thread = new Thread("issue-reporting-lgna");
    TestLgnaRuntimeException throwable = new TestLgnaRuntimeException("lgna");

    recordUncaughtException(handler, thread, throwable);

    assertEquals(List.of(new Callback("lgna", thread, throwable, throwable)), handler.callbacks);
  }

  @Test
  public void lgnaRuntimeExceptionFallsBackToGenericHandlerWhenNotHandled() {
    RecordingHandler handler = new RecordingHandler(false);
    Thread thread = new Thread("issue-reporting-lgna-fallback");
    TestLgnaRuntimeException throwable = new TestLgnaRuntimeException("not handled");

    recordUncaughtException(handler, thread, throwable);

    assertEquals(List.of(
        new Callback("lgna", thread, throwable, throwable),
        new Callback("generic", thread, throwable, throwable)
    ), handler.callbacks);
  }

  @Test
  public void invocationTargetExceptionCauseUsesTargetForDispatchAndPreservesOriginalThrowable() {
    RecordingHandler handler = new RecordingHandler(true);
    Thread thread = new Thread("issue-reporting-invocation-target");
    TestLgnaRuntimeException target = new TestLgnaRuntimeException("target");
    Throwable throwable = new Throwable("wrapper", new InvocationTargetException(target));

    recordUncaughtException(handler, thread, throwable);

    assertEquals(List.of(new Callback("lgna", thread, throwable, target)), handler.callbacks);
  }

  private record Callback(String name, Thread thread, Throwable originalThrowable, Throwable resolvedThrowable) {
  }

  private static void recordUncaughtException(RecordingHandler handler, Thread thread, Throwable throwable) {
    PrintStream originalErr = System.err;
    try (PrintStream silentErr = new PrintStream(OutputStream.nullOutputStream())) {
      System.setErr(silentErr);
      handler.uncaughtException(thread, throwable);
    } finally {
      System.setErr(originalErr);
    }
  }

  private static final class RecordingHandler extends AbstractUncaughtExceptionHandler {
    private final boolean lgnaHandlingResult;
    private final List<Callback> callbacks = new ArrayList<>(2);

    private RecordingHandler(boolean lgnaHandlingResult) {
      this.lgnaHandlingResult = lgnaHandlingResult;
    }

    @Override
    protected boolean handleUncaughtLgnaRuntimeException(Thread thread, Throwable originalThrowable, LgnaRuntimeException originalThrowableOrTarget) {
      this.callbacks.add(new Callback("lgna", thread, originalThrowable, originalThrowableOrTarget));
      return this.lgnaHandlingResult;
    }

    @Override
    protected void handleUncaughtException(Thread thread, Throwable originalThrowable, Throwable originalThrowableOrTarget) {
      this.callbacks.add(new Callback("generic", thread, originalThrowable, originalThrowableOrTarget));
    }
  }

  private static final class TestLgnaRuntimeException extends LgnaRuntimeException {
    private TestLgnaRuntimeException(String message) {
      super(message);
    }

    @Override
    protected void appendFormattedString(StringBuilder sb) {
      sb.append(getMessage());
    }
  }
}
