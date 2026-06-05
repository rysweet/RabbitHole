package org.lgna.croquet;

import java.util.Objects;
import java.util.function.IntConsumer;

public final class ProcessTerminator {
  private static final IntConsumer DEFAULT_HANDLER = status -> {
  };
  private static volatile IntConsumer handler = DEFAULT_HANDLER;

  private ProcessTerminator() {
  }

  public static void setHandler(IntConsumer handler) {
    ProcessTerminator.handler = Objects.requireNonNull(handler);
  }

  public static void resetHandler() {
    handler = DEFAULT_HANDLER;
  }

  public static void requestExit(int status, String message) {
    handler.accept(status);
    throw new ProcessTerminationRequestedException(status, message);
  }
}
