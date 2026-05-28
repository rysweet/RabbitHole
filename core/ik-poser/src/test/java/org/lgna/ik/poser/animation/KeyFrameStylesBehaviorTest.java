package org.lgna.ik.poser.animation;

import org.junit.Test;
import org.lgna.story.AnimationStyle;

import static org.junit.Assert.assertEquals;

public class KeyFrameStylesBehaviorTest {
  @Test
  public void animationStyleUsesPreviousSlowOutAndCurrentSlowInFlags() {
    assertEquals(
        AnimationStyle.BEGIN_AND_END_ABRUPTLY,
        KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
            KeyFrameStyles.ARRIVE_GENTLY_AND_EXIT_ABRUPTLY,
            KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY));
    assertEquals(
        AnimationStyle.BEGIN_ABRUPTLY_AND_END_GENTLY,
        KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
            KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY,
            KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY));
    assertEquals(
        AnimationStyle.BEGIN_GENTLY_AND_END_ABRUPTLY,
        KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
            KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY,
            KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY));
    assertEquals(
        AnimationStyle.BEGIN_AND_END_GENTLY,
        KeyFrameStyles.getAnimationStyleFromTwoKeyFrameStyles(
            null,
            KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY));
  }

  @Test
  public void keyFrameStyleRoundTripsAllFourAnimationStyleCombinations() {
    assertEquals(
        KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY,
        KeyFrameStyles.getKeyFrameStyleFromTwoAnimationStyles(
            AnimationStyle.BEGIN_AND_END_ABRUPTLY,
            AnimationStyle.BEGIN_AND_END_ABRUPTLY));
    assertEquals(
        KeyFrameStyles.ARRIVE_ABRUPTLY_AND_EXIT_GENTLY,
        KeyFrameStyles.getKeyFrameStyleFromTwoAnimationStyles(
            AnimationStyle.BEGIN_AND_END_ABRUPTLY,
            AnimationStyle.BEGIN_AND_END_GENTLY));
    assertEquals(
        KeyFrameStyles.ARRIVE_GENTLY_AND_EXIT_ABRUPTLY,
        KeyFrameStyles.getKeyFrameStyleFromTwoAnimationStyles(
            AnimationStyle.BEGIN_AND_END_GENTLY,
            AnimationStyle.BEGIN_AND_END_ABRUPTLY));
    assertEquals(
        KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY,
        KeyFrameStyles.getKeyFrameStyleFromTwoAnimationStyles(
            AnimationStyle.BEGIN_AND_END_GENTLY,
            AnimationStyle.BEGIN_AND_END_GENTLY));
  }
}
