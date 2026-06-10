package edu.cmu.cs.dennisc.render.gl;

import edu.cmu.cs.dennisc.render.gl.imp.RenderContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GlRenderContractTest {
  @Test
  public void forgettableBindingReceivesRenderContext() {
    RecordingBinding binding = new RecordingBinding();
    RenderContext context = new RenderContext();

    binding.forget(context);

    assertSame(context, binding.lastContext);
    assertEquals(1, binding.callCount);
  }

  @Test
  public void renderContextBeginsWithHeadlessSafeDefaults() {
    RenderContext context = new RenderContext();

    assertFalse(context.isFogEnabled());
    assertFalse(context.isTextureEnabled());
    assertTrue(context.isLightingEnabled());
    assertFalse(context.isShadingEnabled());
  }

  @Test
  public void renderContextAffectorSetupResetsTransientRenderState() {
    RenderContext context = new RenderContext();
    context.setIsFogEnabled(true);
    context.setGlobalBrightness(0.5f);

    context.beginAffectorSetup();

    assertFalse(context.isFogEnabled());
    assertEquals(0.5f, context.getGlobalBrightness(), 0.0001f);
    assertEquals(0x4000, context.getNextLightID());
  }

  private static final class RecordingBinding implements ForgettableBinding {
    private RenderContext lastContext;
    private int callCount;

    @Override
    public void forget(RenderContext rc) {
      this.lastContext = rc;
      this.callCount++;
    }
  }
}
