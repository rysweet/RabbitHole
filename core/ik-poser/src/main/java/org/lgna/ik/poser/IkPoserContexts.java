package org.lgna.ik.poser;

import java.util.ServiceLoader;

public final class IkPoserContexts {
  private IkPoserContexts() {
    throw new AssertionError();
  }

  private static class Holder {
    private static final IkPoserContext INSTANCE = ServiceLoader.load(IkPoserContext.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No IkPoserContext implementation found"));
  }

  public static IkPoserContext getInstance() {
    return Holder.INSTANCE;
  }
}
