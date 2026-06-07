package org.lgna.croquet;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class ProcessTerminatorTest {
  private ProcessTerminator.Handler previousHandler;

  @After
  public void restoreHandler() {
    ProcessTerminator.setHandler(this.previousHandler);
  }

  @Test
  public void requestExitWithoutHandlerThrowsTerminationRequestWithStatus() {
    installHandler(null);

    ProcessTerminationRequestedException request = assertThrows(
        ProcessTerminationRequestedException.class,
        () -> ProcessTerminator.requestExit(17));

    assertEquals(17, request.getStatus());
  }

  @Test
  public void requestExitInvokesInstalledHandlerWithRequestedStatus() {
    AtomicInteger requestedStatus = new AtomicInteger(Integer.MIN_VALUE);
    installHandler(requestedStatus::set);

    ProcessTerminationRequestedException request = assertThrows(
        ProcessTerminationRequestedException.class,
        () -> ProcessTerminator.requestExit(42));

    assertEquals(42, requestedStatus.get());
    assertEquals(42, request.getStatus());
  }

  @Test
  public void returnedHandlerFallsBackToTerminationRequestException() {
    installHandler(status -> {
    });

    ProcessTerminationRequestedException request = assertThrows(
        ProcessTerminationRequestedException.class,
        () -> ProcessTerminator.requestExit(-1));

    assertEquals(-1, request.getStatus());
  }

  @Test
  public void handlerExceptionPropagatesWithoutBeingReplacedByFallback() {
    RuntimeException handlerFailure = new RuntimeException("handler failed");
    installHandler(status -> {
      throw handlerFailure;
    });

    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> ProcessTerminator.requestExit(3));

    assertSame(handlerFailure, thrown);
  }

  @Test
  public void setHandlerReturnsPreviousHandlerSoTestsCanRestoreGlobalState() {
    ProcessTerminator.Handler first = status -> {
    };
    ProcessTerminator.Handler second = status -> {
    };

    ProcessTerminator.Handler original = ProcessTerminator.setHandler(first);
    this.previousHandler = original;
    ProcessTerminator.Handler returned = ProcessTerminator.setHandler(second);

    assertSame(first, returned);
  }

  @Test
  public void processTerminationRequestedExceptionPreservesStatus() {
    ProcessTerminationRequestedException request = new ProcessTerminationRequestedException(-1);

    assertEquals(-1, request.getStatus());
  }

  private void installHandler(ProcessTerminator.Handler handler) {
    this.previousHandler = ProcessTerminator.setHandler(handler);
  }
}
