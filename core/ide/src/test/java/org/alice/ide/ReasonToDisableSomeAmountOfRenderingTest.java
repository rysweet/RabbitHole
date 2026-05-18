package org.alice.ide;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReasonToDisableSomeAmountOfRenderingTest {

  @Test
  public void values_hasThreeConstants() {
    assertEquals(3, ReasonToDisableSomeAmountOfRendering.values().length);
  }

  @Test
  public void valueOf_modalDialog() {
    assertEquals(ReasonToDisableSomeAmountOfRendering.MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN,
        ReasonToDisableSomeAmountOfRendering.valueOf("MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN"));
  }

  @Test
  public void valueOf_dragAndDrop() {
    assertEquals(ReasonToDisableSomeAmountOfRendering.DRAG_AND_DROP,
        ReasonToDisableSomeAmountOfRendering.valueOf("DRAG_AND_DROP"));
  }

  @Test
  public void valueOf_clickAndClack() {
    assertEquals(ReasonToDisableSomeAmountOfRendering.CLICK_AND_CLACK,
        ReasonToDisableSomeAmountOfRendering.valueOf("CLICK_AND_CLACK"));
  }

  @Test
  public void ordinals_areSequential() {
    assertEquals(0, ReasonToDisableSomeAmountOfRendering.MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN.ordinal());
    assertEquals(1, ReasonToDisableSomeAmountOfRendering.DRAG_AND_DROP.ordinal());
    assertEquals(2, ReasonToDisableSomeAmountOfRendering.CLICK_AND_CLACK.ordinal());
  }

  @Test
  public void name_matchesConstant() {
    for (ReasonToDisableSomeAmountOfRendering r : ReasonToDisableSomeAmountOfRendering.values()) {
      assertEquals(r, ReasonToDisableSomeAmountOfRendering.valueOf(r.name()));
    }
  }

  @Test
  public void toString_matchesName() {
    for (ReasonToDisableSomeAmountOfRendering r : ReasonToDisableSomeAmountOfRendering.values()) {
      assertEquals(r.name(), r.toString());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalidName_throws() {
    ReasonToDisableSomeAmountOfRendering.valueOf("INVALID");
  }
}
