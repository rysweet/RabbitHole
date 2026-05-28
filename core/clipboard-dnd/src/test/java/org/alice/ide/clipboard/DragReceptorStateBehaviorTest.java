package org.alice.ide.clipboard;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DragReceptorStateBehaviorTest {
  @Test
  public void idleColorsMatchClipboardPalette() {
    assertEquals(new Color(226, 179, 105), DragReceptorState.IDLE.getBoardColor());
    assertEquals(Color.WHITE, DragReceptorState.IDLE.getPaperColor());
  }

  @Test
  public void startedStateUsesYellowBoardAndPaper() {
    assertEquals(Color.YELLOW, DragReceptorState.STARTED.getBoardColor());
    assertEquals(Color.YELLOW, DragReceptorState.STARTED.getPaperColor());
  }

  @Test
  public void enteredStateUsesGreenHighlightPalette() {
    assertEquals(new Color(0xCCFF99), DragReceptorState.ENTERED.getBoardColor());
    assertEquals(new Color(0x44FF44), DragReceptorState.ENTERED.getPaperColor());
  }

  @Test
  public void statesExposeDistinctBoardColors() {
    assertNotEquals(DragReceptorState.IDLE.getBoardColor(), DragReceptorState.STARTED.getBoardColor());
    assertNotEquals(DragReceptorState.STARTED.getBoardColor(), DragReceptorState.ENTERED.getBoardColor());
  }
}
