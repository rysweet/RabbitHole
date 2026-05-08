package org.lgna.issue;

import org.junit.Test;
import org.lgna.common.LgnaRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class AbstractUncaughtExceptionHandlerTest {
  @Test
  public void plainThrowableRoutesToGenericHandler() {
    RecordingHandler handler = new RecordingHandler(true);
    Thread thread = new Thread("issue-reporting-plain");
    Throwable throwable = new Throwable("plain");

    handler.uncaughtException(thread, throwable);

    assertEquals(List.of("generic"), handler.callbacks);
    assertEquals(0, handler.lgnaInvocationCount);
    assertEquals(1, handler.genericInvocationCount);
    assertSame(thread, handler.genericThread);
    assertSame(throwable, handler.genericOriginalThrowable);
    assertSame(throwable, handler.genericResolvedThrowable);
  }

  @Test
  public void directLgnaRuntimeExceptionRoutesToLgnaHandlerWhenHandled() {
    RecordingHandler handler = new RecordingHandler(true);
    Thread thread = new Thread("issue-reporting-lgna");
    TestLgnaRuntimeException throwable = new TestLgnaRuntimeException("lgna");

    handler.uncaughtException(thread, throwable);

    assertEquals(List.of("lgna"), handler.callbacks);
    assertEquals(1, handler.lgnaInvocationCount);
    assertEquals(0, handler.genericInvocationCount);
    assertSame(thread, handler.lgnaThread);
    assertSame(throwable, handler.lgnaOriginalThrowable);
    assertSame(throwable, handler.lgnaResolvedThrowable);
  }

  @Test
  public void lgnaRuntimeExceptionFallsBackToGenericHandlerWhenNotHandled() {
    RecordingHandler handler = new RecordingHandler(false);
    Thread thread = new Thread("issue-reporting-lgna-fallback");
    TestLgnaRuntimeException throwable = new TestLgnaRuntimeException("not handled");

    handler.uncaughtException(thread, throwable);

    assertEquals(List.of("lgna", "generic"), handler.callbacks);
    assertEquals(1, handler.lgnaInvocationCount);
    assertEquals(1, handler.genericInvocationCount);
    assertSame(thread, handler.lgnaThread);
    assertSame(throwable, handler.lgnaOriginalThrowable);
    assertSame(throwable, handler.lgnaResolvedThrowable);
    assertSame(thread, handler.genericThread);
    assertSame(throwable, handler.genericOriginalThrowable);
    assertSame(throwable, handler.genericResolvedThrowable);
  }

  @Test
  public void invocationTargetExceptionCauseUsesTargetForDispatchAndPreservesOriginalThrowable() {
    RecordingHandler handler = new RecordingHandler(true);
    Thread thread = new Thread("issue-reporting-invocation-target");
    TestLgnaRuntimeException target = new TestLgnaRuntimeException("target");
    Throwable throwable = new Throwable("wrapper", new InvocationTargetException(target));

    handler.uncaughtException(thread, throwable);

    assertEquals(List.of("lgna"), handler.callbacks);
    assertEquals(1, handler.lgnaInvocationCount);
    assertEquals(0, handler.genericInvocationCount);
    assertSame(thread, handler.lgnaThread);
    assertSame(throwable, handler.lgnaOriginalThrowable);
    assertSame(target, handler.lgnaResolvedThrowable);
  }

  private static final class RecordingHandler extends AbstractUncaughtExceptionHandler {
    private final boolean lgnaHandlingResult;
    private final List<String> callbacks = new ArrayList<>();
    private int lgnaInvocationCount;
    private int genericInvocationCount;
    private Thread lgnaThread;
    private Thread genericThread;
    private Throwable lgnaOriginalThrowable;
    private Throwable genericOriginalThrowable;
    private LgnaRuntimeException lgnaResolvedThrowable;
    private Throwable genericResolvedThrowable;

    private RecordingHandler(boolean lgnaHandlingResult) {
      this.lgnaHandlingResult = lgnaHandlingResult;
    }

    @Override
    protected boolean handleUncaughtLgnaRuntimeException(Thread thread, Throwable originalThrowable, LgnaRuntimeException originalThrowableOrTarget) {
      this.callbacks.add("lgna");
      this.lgnaInvocationCount++;
      this.lgnaThread = thread;
      this.lgnaOriginalThrowable = originalThrowable;
      this.lgnaResolvedThrowable = originalThrowableOrTarget;
      return this.lgnaHandlingResult;
    }

    @Override
    protected void handleUncaughtException(Thread thread, Throwable originalThrowable, Throwable originalThrowableOrTarget) {
      this.callbacks.add("generic");
      this.genericInvocationCount++;
      this.genericThread = thread;
      this.genericOriginalThrowable = originalThrowable;
      this.genericResolvedThrowable = originalThrowableOrTarget;
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
