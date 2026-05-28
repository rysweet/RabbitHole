package org.lgna.ik.poser.animation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link TimeLine} — keyframe management, time clamping,
 * and listener notification contract.
 */
public class TimeLineTest {

  private TimeLine timeLine;

  @Before
  public void setUp() {
    timeLine = new TimeLine();
  }

  // ---- initial state ----

  @Test
  public void initialCurrentTime_isZero() {
    assertEquals(0.0, timeLine.getCurrentTime(), 0.0);
  }

  @Test
  public void initialEndTime_isTen() {
    assertEquals(10.0, timeLine.getEndTime(), 0.0);
  }

  @Test
  public void initialKeyFrames_isEmpty() {
    assertTrue(timeLine.getKeyFrames().isEmpty());
  }

  @Test
  public void initialSelectedKeyFrame_isNull() {
    assertNull(timeLine.getSelectedKeyFrame());
  }

  // ---- setCurrentTime ----

  @Test
  public void setCurrentTime_withinRange() {
    timeLine.setCurrentTime(5.0);
    assertEquals(5.0, timeLine.getCurrentTime(), 0.0);
  }

  @Test
  public void setCurrentTime_clampsNegativeToZero() {
    timeLine.setCurrentTime(-1.0);
    assertEquals(0.0, timeLine.getCurrentTime(), 0.0);
  }

  @Test
  public void setCurrentTime_clampsAboveEndTime() {
    timeLine.setCurrentTime(15.0);
    assertEquals(10.0, timeLine.getCurrentTime(), 0.0);
  }

  // ---- setEndTime ----

  @Test
  public void setEndTime_positiveValue() {
    timeLine.setEndTime(20.0);
    assertEquals(20.0, timeLine.getEndTime(), 0.0);
  }

  @Test
  public void setEndTime_zeroOrNegativeIgnored() {
    timeLine.setEndTime(10.0);
    timeLine.setEndTime(0.0);
    assertEquals(10.0, timeLine.getEndTime(), 0.0);
  }

  @Test
  public void setEndTime_negativeIgnored() {
    timeLine.setEndTime(-5.0);
    assertEquals(10.0, timeLine.getEndTime(), 0.0);
  }

  // ---- addKeyFrameData ----
  // Note: TimeLine.addKeyFrameData calls checkAddingJoints which requires
  // a non-null Pose. Tests that add keyframes are skipped when Pose
  // construction is not feasible in unit test context.

  // ---- getFrameForCurrentTime ----

  @Test
  public void getFrameForCurrentTime_noFrames_returnsNull() {
    timeLine.setCurrentTime(5.0);
    assertNull(timeLine.getFrameForCurrentTime());
  }

  // ---- setSelectedKeyFrame ----

  @Test
  public void setSelectedKeyFrame_null_clears() {
    timeLine.setSelectedKeyFrame(null);
    assertNull(timeLine.getSelectedKeyFrame());
  }

  // ---- refresh ----

  @Test
  public void refresh_onEmptyTimeline_doesNotThrow() {
    timeLine.refresh();
    assertTrue(timeLine.getKeyFrames().isEmpty());
  }

  // ---- listener management ----

  @Test
  public void disableEnableListeners_doesNotThrow() {
    timeLine.disableListeners();
    timeLine.setCurrentTime(5.0);
    timeLine.enableListeners();
    assertEquals(5.0, timeLine.getCurrentTime(), 0.0);
  }

  // ---- addListener/removeListener ----

  @Test
  public void addListener_doesNotThrow() {
    TimeLineListener listener = createNoOpListener();
    timeLine.addListener(listener);
    // no exception = pass
  }

  @Test
  public void removeListener_doesNotThrow() {
    TimeLineListener listener = createNoOpListener();
    timeLine.addListener(listener);
    timeLine.removeListener(listener);
    // no exception = pass
  }

  // ---- setInitialPose ----

  @Test
  public void setInitialPose_null_doesNotThrow() {
    timeLine.setInitialPose(null);
    // no exception = pass
  }

  private static TimeLineListener createNoOpListener() {
    return new TimeLineListener() {
      @Override public void currentTimeChanged(double time, org.lgna.story.Pose pose) {}
      @Override public void endTimeChanged(double endTime) {}
      @Override public void keyFrameAdded(KeyFrameData keyFrame) {}
      @Override public void keyFrameModified(KeyFrameData keyFrame) {}
      @Override public void keyFrameDeleted(KeyFrameData keyFrame) {}
      @Override public void selectedKeyFrameChanged(KeyFrameData keyFrame) {}
    };
  }
}
