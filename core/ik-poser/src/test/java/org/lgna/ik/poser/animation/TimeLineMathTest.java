package org.lgna.ik.poser.animation;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TimeLineMathTest {
  private static List<KeyFrameData> createKeyFrames() {
    return List.of(
        new KeyFrameData(1.0, null),
        new KeyFrameData(3.0, null),
        new KeyFrameData(5.0, null));
  }

  @Test
  public void clampCurrentTime_clampsOutsideBounds() {
    assertEquals(0.0, TimeLineMath.clampCurrentTime(-1.0, 10.0), 0.0);
    assertEquals(10.0, TimeLineMath.clampCurrentTime(12.0, 10.0), 0.0);
    assertEquals(4.5, TimeLineMath.clampCurrentTime(4.5, 10.0), 0.0);
  }

  @Test
  public void findKeyFrameWindow_beforeFirstFrame_hasOnlyAfter() {
    List<KeyFrameData> keyFrames = createKeyFrames();

    TimeLineMath.KeyFrameWindow window = TimeLineMath.findKeyFrameWindow(keyFrames, 0.5);

    assertNull(window.getBefore());
    assertSame(keyFrames.get(0), window.getAfter());
  }

  @Test
  public void findKeyFrameWindow_betweenFrames_returnsNeighbors() {
    List<KeyFrameData> keyFrames = createKeyFrames();

    TimeLineMath.KeyFrameWindow window = TimeLineMath.findKeyFrameWindow(keyFrames, 4.0);

    assertSame(keyFrames.get(1), window.getBefore());
    assertSame(keyFrames.get(2), window.getAfter());
  }

  @Test
  public void findKeyFrameWindow_afterLastFrame_hasOnlyBefore() {
    List<KeyFrameData> keyFrames = createKeyFrames();

    TimeLineMath.KeyFrameWindow window = TimeLineMath.findKeyFrameWindow(keyFrames, 9.0);

    assertSame(keyFrames.get(2), window.getBefore());
    assertNull(window.getAfter());
  }

  @Test
  public void calculateInterpolationPortion_returnsFractionAcrossRange() {
    assertEquals(0.25, TimeLineMath.calculateInterpolationPortion(2.0, 6.0, 3.0), 0.0);
  }

  @Test
  public void calculateInterpolationPortion_handlesDegenerateRange() {
    assertEquals(0.0, TimeLineMath.calculateInterpolationPortion(2.0, 2.0, 2.0), 0.0);
  }
}
