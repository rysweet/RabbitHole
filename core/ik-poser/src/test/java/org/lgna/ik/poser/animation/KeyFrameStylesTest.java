package org.lgna.ik.poser.animation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link KeyFrameStyles} — enum constants, slow-in/out flags,
 * and animation style combination logic.
 */
public class KeyFrameStylesTest {

  // ---- enum constants exist ----

  @Test
  public void enumValuesCount() {
    assertEquals(4, KeyFrameStyles.values().length);
  }

  @Test
  public void arriveAndExitAbruptly_noSlowInOrOut() {
    KeyFrameStyles style = KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY;
    assertFalse(style.getIsSlowInDesired());
    assertFalse(style.getIsSlowOutDesired());
  }

  @Test
  public void arriveAbruptlyAndExitGently_slowOutOnly() {
    KeyFrameStyles style = KeyFrameStyles.ARRIVE_ABRUPTLY_AND_EXIT_GENTLY;
    assertFalse(style.getIsSlowInDesired());
    assertTrue(style.getIsSlowOutDesired());
  }

  @Test
  public void arriveGentlyAndExitAbruptly_slowInOnly() {
    KeyFrameStyles style = KeyFrameStyles.ARRIVE_GENTLY_AND_EXIT_ABRUPTLY;
    assertTrue(style.getIsSlowInDesired());
    assertFalse(style.getIsSlowOutDesired());
  }

  @Test
  public void arriveAndExitGently_bothSlowInAndOut() {
    KeyFrameStyles style = KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY;
    assertTrue(style.getIsSlowInDesired());
    assertTrue(style.getIsSlowOutDesired());
  }

  // ---- getAnimationStyleFromTwoKeyFrameStyles ----

  @Test
  public void animationStyle_bothGentle_returnsNonNull() {
    assertNotNull(KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
        KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY,
        KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY));
  }

  @Test
  public void animationStyle_bothAbrupt_returnsNonNull() {
    assertNotNull(KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
        KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY,
        KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY));
  }

  @Test
  public void animationStyle_mixedStyles_returnsNonNull() {
    assertNotNull(KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
        KeyFrameStyles.ARRIVE_GENTLY_AND_EXIT_ABRUPTLY,
        KeyFrameStyles.ARRIVE_ABRUPTLY_AND_EXIT_GENTLY));
  }

  // ---- valueOf round-trip ----

  @Test
  public void valueOf_roundTrips() {
    for (KeyFrameStyles style : KeyFrameStyles.values()) {
      assertEquals(style, KeyFrameStyles.valueOf(style.name()));
    }
  }
}
