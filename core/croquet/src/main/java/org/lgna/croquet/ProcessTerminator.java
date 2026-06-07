package org.lgna.croquet;

import java.util.concurrent.atomic.AtomicReference;

public final class ProcessTerminator {
  private static final AtomicReference<Handler> HANDLER = new AtomicReference<>();

  private ProcessTerminator() {
  }

  public static Handler setHandler(Handler handler) {
    return HANDLER.getAndSet(handler);
  }

  public static void requestExit(int status) {
    Handler handler = HANDLER.get();
    if (handler != null) {
      handler.requestExit(status);
    }
    throw new ProcessTerminationRequestedException(status);
  }

  @FunctionalInterface
  public interface Handler {
    void requestExit(int status);
  }
}
