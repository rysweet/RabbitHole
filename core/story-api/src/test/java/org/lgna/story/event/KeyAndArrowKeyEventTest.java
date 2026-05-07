package org.lgna.story.event;

import org.junit.Test;
import org.lgna.story.Key;
import org.lgna.story.MoveDirection;
import org.lgna.story.TurnDirection;

import java.awt.Canvas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KeyAndArrowKeyEventTest {
  private static final Canvas SOURCE = new Canvas();

  @Test
  public void keyLookupReturnsKnownKeysAndNullForUnknownCodes() {
    assertEquals(Key.A, Key.getInstanceFromKeyCode(java.awt.event.KeyEvent.VK_A));
    assertEquals(Key.DIGIT_5, Key.getInstanceFromKeyCode(java.awt.event.KeyEvent.VK_5));
    assertNull(Key.getInstanceFromKeyCode(Integer.MIN_VALUE));
  }

  @Test
  public void keyEventClassifiesLetterDigitAndOtherCharacters() {
    KeyEvent letter = new KeyEvent(awtKeyEvent(java.awt.event.KeyEvent.VK_A, 'A'));
    KeyEvent digit = new KeyEvent(awtKeyEvent(java.awt.event.KeyEvent.VK_5, '5'));
    KeyEvent space = new KeyEvent(awtKeyEvent(java.awt.event.KeyEvent.VK_SPACE, ' '));

    assertEquals(Key.A, letter.getKey());
    assertTrue(letter.isKey(Key.A));
    assertTrue(letter.isLetter());
    assertFalse(letter.isDigit());

    assertEquals(Key.DIGIT_5, digit.getKey());
    assertTrue(digit.isDigit());
    assertFalse(digit.isLetter());

    assertEquals(Key.SPACE, space.getKey());
    assertFalse(space.isDigit());
    assertFalse(space.isLetter());
  }

  @Test
  public void arrowKeyEventMapsWasdKeysToMoveAndTurnDirections() {
    assertDirections(java.awt.event.KeyEvent.VK_A, 'a', MoveDirection.LEFT, MoveDirection.LEFT, TurnDirection.LEFT);
    assertDirections(java.awt.event.KeyEvent.VK_D, 'd', MoveDirection.RIGHT, MoveDirection.RIGHT, TurnDirection.RIGHT);
    assertDirections(java.awt.event.KeyEvent.VK_W, 'w', MoveDirection.FORWARD, MoveDirection.UP, TurnDirection.FORWARD);
    assertDirections(java.awt.event.KeyEvent.VK_S, 's', MoveDirection.BACKWARD, MoveDirection.DOWN, TurnDirection.BACKWARD);
  }

  @Test
  public void arrowKeyEventMapsArrowKeysToMoveAndTurnDirections() {
    assertDirections(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.KeyEvent.CHAR_UNDEFINED,
        MoveDirection.LEFT, MoveDirection.LEFT, TurnDirection.LEFT);
    assertDirections(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.KeyEvent.CHAR_UNDEFINED,
        MoveDirection.RIGHT, MoveDirection.RIGHT, TurnDirection.RIGHT);
    assertDirections(java.awt.event.KeyEvent.VK_UP, java.awt.event.KeyEvent.CHAR_UNDEFINED,
        MoveDirection.FORWARD, MoveDirection.UP, TurnDirection.FORWARD);
    assertDirections(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.KeyEvent.CHAR_UNDEFINED,
        MoveDirection.BACKWARD, MoveDirection.DOWN, TurnDirection.BACKWARD);
  }

  @Test
  public void arrowKeyEventCopyConstructorKeepsOriginalKeyMapping() {
    KeyEvent base = new KeyEvent(awtKeyEvent(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.KeyEvent.CHAR_UNDEFINED));
    ArrowKeyEvent copied = new ArrowKeyEvent(base);

    assertEquals(Key.RIGHT, copied.getKey());
    assertEquals(MoveDirection.RIGHT, copied.getMoveDirection(ArrowKeyEvent.MoveDirectionPlane.FORWARD_BACKWARD_LEFT_RIGHT));
    assertEquals(TurnDirection.RIGHT, copied.getTurnDirection());
  }

  private static void assertDirections(int keyCode, char keyChar, MoveDirection horizontalPlane,
      MoveDirection verticalPlane, TurnDirection turnDirection) {
    ArrowKeyEvent event = new ArrowKeyEvent(awtKeyEvent(keyCode, keyChar));

    assertEquals(horizontalPlane, event.getMoveDirection(ArrowKeyEvent.MoveDirectionPlane.FORWARD_BACKWARD_LEFT_RIGHT));
    assertEquals(verticalPlane, event.getMoveDirection(ArrowKeyEvent.MoveDirectionPlane.UP_DOWN_LEFT_RIGHT));
    assertEquals(turnDirection, event.getTurnDirection());
  }

  private static java.awt.event.KeyEvent awtKeyEvent(int keyCode, char keyChar) {
    return new java.awt.event.KeyEvent(SOURCE, java.awt.event.KeyEvent.KEY_PRESSED, 0L, 0, keyCode, keyChar);
  }
}
