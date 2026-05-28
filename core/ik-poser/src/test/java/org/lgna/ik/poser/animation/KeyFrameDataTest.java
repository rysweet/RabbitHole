package org.lgna.ik.poser.animation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link KeyFrameData} — constructor, accessors, and style defaults.
 */
public class KeyFrameDataTest {

  @Test
  public void constructor_setsTimeAndPose() {
    KeyFrameData data = new KeyFrameData(2.5, null);
    assertEquals(2.5, data.getEventTime(), 0.0);
    assertNull(data.getPose());
  }

  @Test
  public void defaultStyle_isArriveAndExitGently() {
    KeyFrameData data = new KeyFrameData(0.0, null);
    assertEquals(KeyFrameStyles.ARRIVE_AND_EXIT_GENTLY, data.getEventStyle());
  }

  @Test
  public void setStyle_updatesStyle() {
    KeyFrameData data = new KeyFrameData(0.0, null);
    data.setStyle(KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY);
    assertEquals(KeyFrameStyles.ARRIVE_AND_EXIT_ABRUPTLY, data.getEventStyle());
  }

  @Test
  public void setTime_updatesTime() {
    KeyFrameData data = new KeyFrameData(1.0, null);
    data.setTime(3.0);
    assertEquals(3.0, data.getEventTime(), 0.0);
  }

  @Test
  public void setPose_updatesNullToNull() {
    KeyFrameData data = new KeyFrameData(0.0, null);
    data.setPose(null);
    assertNull(data.getPose());
  }

  @Test
  public void setPoseActual_storesSeparately() {
    KeyFrameData data = new KeyFrameData(0.0, null);
    data.setPoseActual(null);
    assertNull(data.getPoseActual());
  }

  @Test
  public void poseActual_initiallyNull() {
    KeyFrameData data = new KeyFrameData(0.0, null);
    assertNull(data.getPoseActual());
  }
}
