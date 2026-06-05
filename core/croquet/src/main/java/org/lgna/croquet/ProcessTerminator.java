package org.lgna.croquet;

import java.util.Objects;
import java.util.function.IntConsumer;

public final class ProcessTerminator {
  private static final IntConsumer THROWING_HANDLER = status -> {
    throw new ProcessTerminationRequestedException(status, "Process termination requested with status " + status);
  };
  private static volatile IntConsumer handler = THROWING_HANDLER;

  private ProcessTerminator() {
  }

  public static void setHandler(IntConsumer handler) {
    ProcessTerminator.handler = Objects.requireNonNull(handler);
  }

  public static void resetHandler() {
    handler = THROWING_HANDLER;
  }

  public static void requestExit(int status, String message) {
    handler.accept(status);
    throw new ProcessTerminationRequestedException(status, message);
  }
}
