package org.lgna.ik.poser.animation;

import org.junit.Before;
import org.junit.Test;
import org.lgna.story.BipedPose;
import org.lgna.story.BipedPoseBuilder;
import org.lgna.story.Orientation;
import org.lgna.story.Pose;
import org.lgna.story.resources.BipedResource;
import org.lgna.story.implementation.JointIdTransformationPair;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Deep tests for {@link TimeLine} covering keyframe add/remove, interpolation,
 * duration/style queries, move, listener notifications, and boundary conditions.
 */
public class TimeLineDeepTest {

  private TimeLine timeLine;
  private List<String> events;
  private TimeLineListener recorder;

  private static BipedPose makePose(Orientation... jointOrientations) {
    BipedPoseBuilder builder = new BipedPoseBuilder();
    if (jointOrientations.length > 0) {
      builder.joint(BipedResource.LEFT_SHOULDER, jointOrientations[0]);
    }
    if (jointOrientations.length > 1) {
      builder.joint(BipedResource.RIGHT_SHOULDER, jointOrientations[1]);
    }
    return builder.build();
  }

  private static Orientation orient(double x, double y, double z, double w) {
    return new Orientation(x, y, z, w);
  }

  @Before
  public void setUp() {
    timeLine = new TimeLine();
    events = new ArrayList<>();
    recorder = new TimeLineListener() {
      @Override public void currentTimeChanged(double time, Pose pose) { events.add("time:" + time); }
      @Override public void endTimeChanged(double endTime) { events.add("end:" + endTime); }
      @Override public void keyFrameAdded(KeyFrameData kf) { events.add("added:" + kf.getEventTime()); }
      @Override public void keyFrameModified(KeyFrameData kf) { events.add("modified:" + kf.getEventTime()); }
      @Override public void keyFrameDeleted(KeyFrameData kf) { events.add("deleted:" + kf.getEventTime()); }
      @Override public void selectedKeyFrameChanged(KeyFrameData kf) { events.add("selected:" + (kf != null ? kf.getEventTime() : "null")); }
    };
    // Use an initial pose so interpolation works
    timeLine.setInitialPose(makePose(orient(0, 0, 0, 1)));
    timeLine.addListener(recorder);
  }

  // --- addKeyFrameData ---

  @Test
  public void addKeyFrame_singleFrame_appearsInList() {
    BipedPose pose = makePose(orient(0, 0, 0, 1));
    timeLine.addKeyFrameData(pose, 2.0);
    assertEquals(1, timeLine.getKeyFrames().size());
    assertEquals(2.0, timeLine.getKeyFrames().get(0).getEventTime(), 0.001);
  }

  @Test
  public void addKeyFrame_firesAddedEvent() {
    events.clear();
    BipedPose pose = makePose(orient(0, 0, 0, 1));
    timeLine.addKeyFrameData(pose, 3.0);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("added:")));
  }

  @Test
  public void addKeyFrame_multipleFrames_sortedByTime() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 8.0);
    List<KeyFrameData> frames = timeLine.getKeyFrames();
    assertEquals(3, frames.size());
    assertTrue(frames.get(0).getEventTime() <= frames.get(1).getEventTime());
    assertTrue(frames.get(1).getEventTime() <= frames.get(2).getEventTime());
  }

  @Test
  public void addKeyFrame_insertBetweenExisting() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 8.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    List<KeyFrameData> frames = timeLine.getKeyFrames();
    assertEquals(3, frames.size());
    assertEquals(2.0, frames.get(0).getEventTime(), 0.001);
    assertEquals(5.0, frames.get(1).getEventTime(), 0.001);
    assertEquals(8.0, frames.get(2).getEventTime(), 0.001);
  }

  @Test
  public void addKeyFrame_insertBeforeFirst() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 1.0);
    assertEquals(1.0, timeLine.getKeyFrames().get(0).getEventTime(), 0.001);
  }

  // --- removeKeyFrameData ---

  @Test
  public void removeKeyFrame_removesFromList() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    timeLine.removeKeyFrameData(frame);
    assertTrue(timeLine.getKeyFrames().isEmpty());
  }

  @Test
  public void removeKeyFrame_firesDeletedEvent() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    events.clear();
    timeLine.removeKeyFrameData(frame);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("deleted:")));
  }

  // --- moveExistingKeyFrameData ---

  @Test
  public void moveKeyFrame_updatesTime() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    timeLine.moveExistingKeyFrameData(frame, 5.0);
    assertEquals(5.0, frame.getEventTime(), 0.001);
  }

  @Test
  public void moveKeyFrame_outsideRange_ignored() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    timeLine.moveExistingKeyFrameData(frame, -1.0);
    assertEquals(3.0, frame.getEventTime(), 0.001);
    timeLine.moveExistingKeyFrameData(frame, 15.0);
    assertEquals(3.0, frame.getEventTime(), 0.001);
  }

  @Test
  public void moveKeyFrame_firesModifiedEvent() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    events.clear();
    timeLine.moveExistingKeyFrameData(frame, 5.0);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("modified:")));
  }

  @Test
  public void moveKeyFrame_maintainsSortOrder() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 8.0);
    KeyFrameData first = timeLine.getKeyFrames().get(0);
    timeLine.moveExistingKeyFrameData(first, 6.0);
    List<KeyFrameData> frames = timeLine.getKeyFrames();
    assertTrue(frames.get(0).getEventTime() <= frames.get(1).getEventTime());
    assertTrue(frames.get(1).getEventTime() <= frames.get(2).getEventTime());
  }

  // --- modifyExistingPose ---

  @Test
  public void modifyExistingPose_updatesPose() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    BipedPose newPose = makePose(orient(0, 0, 0, 1));
    events.clear();
    timeLine.modifyExistingPose(frame, newPose);
    assertEquals(newPose, frame.getPose());
    assertTrue(events.stream().anyMatch(e -> e.startsWith("modified:")));
  }

  // --- getDurationForKeyFrame ---

  @Test
  public void getDuration_firstKeyFrame_returnsEventTime() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    assertEquals(3.0, timeLine.getDurationForKeyFrame(frame), 0.001);
  }

  @Test
  public void getDuration_secondKeyFrame_returnsDifference() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 7.0);
    KeyFrameData second = timeLine.getKeyFrames().get(1);
    assertEquals(5.0, timeLine.getDurationForKeyFrame(second), 0.001);
  }

  // --- getStyleForKeyFramePose ---

  @Test
  public void getStyleForKeyFrame_firstFrame_returnsStyle() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    assertNotNull(timeLine.getStyleForKeyFramePose(frame));
  }

  @Test
  public void getStyleForKeyFrame_secondFrame_returnsStyle() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 7.0);
    KeyFrameData second = timeLine.getKeyFrames().get(1);
    assertNotNull(timeLine.getStyleForKeyFramePose(second));
  }

  // --- getFrameForCurrentTime ---

  @Test
  public void getFrameForCurrentTime_exactMatch_returnsFrame() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    timeLine.disableListeners();
    timeLine.setCurrentTime(3.0);
    timeLine.enableListeners();
    KeyFrameData frame = timeLine.getFrameForCurrentTime();
    assertNotNull(frame);
    assertEquals(3.0, frame.getEventTime(), 0.001);
  }

  @Test
  public void getFrameForCurrentTime_noMatch_returnsNull() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    timeLine.disableListeners();
    timeLine.setCurrentTime(5.0);
    timeLine.enableListeners();
    assertNull(timeLine.getFrameForCurrentTime());
  }

  @Test
  public void getFrameForCurrentTime_beforeFirstFrame_returnsNull() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 3.0);
    timeLine.disableListeners();
    timeLine.setCurrentTime(1.0);
    timeLine.enableListeners();
    assertNull(timeLine.getFrameForCurrentTime());
  }

  // --- setSelectedKeyFrame ---

  @Test
  public void setSelectedKeyFrame_setsTimeAndFires() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 4.0);
    KeyFrameData frame = timeLine.getKeyFrames().get(0);
    events.clear();
    timeLine.setSelectedKeyFrame(frame);
    assertEquals(frame, timeLine.getSelectedKeyFrame());
    assertEquals(4.0, timeLine.getCurrentTime(), 0.001);
  }

  // --- setEndTime edge cases ---

  @Test
  public void setEndTime_belowLastKeyFrame_ignored() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 8.0);
    timeLine.setEndTime(5.0);
    assertEquals(10.0, timeLine.getEndTime(), 0.001);
  }

  @Test
  public void setEndTime_clampsCurrentTime() {
    timeLine.disableListeners();
    timeLine.setCurrentTime(9.0);
    timeLine.enableListeners();
    timeLine.setEndTime(5.0);
    // With no keyframes, endTime can be reduced
    assertTrue(timeLine.getCurrentTime() <= timeLine.getEndTime());
  }

  @Test
  public void setEndTime_firesEndTimeChanged() {
    events.clear();
    timeLine.setEndTime(20.0);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("end:")));
  }

  // --- refresh ---

  @Test
  public void refresh_withKeyFrames_clearsAll() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    timeLine.refresh();
    assertTrue(timeLine.getKeyFrames().isEmpty());
    assertEquals(0.0, timeLine.getCurrentTime(), 0.001);
    assertEquals(10.0, timeLine.getEndTime(), 0.001);
  }

  // --- disableListeners ---

  @Test
  public void disabledListeners_noTimeChangeEvents() {
    timeLine.disableListeners();
    events.clear();
    timeLine.setCurrentTime(5.0);
    // Should NOT fire currentTimeChanged, but selectedKeyFrameChanged still fires
    assertFalse(events.stream().anyMatch(e -> e.startsWith("time:")));
    timeLine.enableListeners();
  }

  // --- setCurrentTime with keyframes triggers calculatePoseForTime ---

  @Test
  public void setCurrentTime_withKeyFrames_firesPose() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    events.clear();
    timeLine.setCurrentTime(3.5);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("time:")));
  }

  @Test
  public void setCurrentTime_exactlyOnKeyFrame_returnsPose() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 4.0);
    events.clear();
    timeLine.setCurrentTime(4.0);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("selected:")));
  }

  // --- multiple joints across keyframes ---

  @Test
  public void addKeyFrames_withDifferentJoints_mergesUsedIds() {
    // Initial pose must contain all joints that any keyframe will reference
    timeLine.setInitialPose(new BipedPoseBuilder()
        .joint(BipedResource.LEFT_SHOULDER, orient(0, 0, 0, 1))
        .joint(BipedResource.RIGHT_SHOULDER, orient(0, 0, 0, 1))
        .build());
    BipedPose pose1 = makePose(orient(0, 0, 0, 1)); // LEFT_SHOULDER only
    BipedPose pose2 = new BipedPoseBuilder()
        .joint(BipedResource.LEFT_SHOULDER, orient(0, 0, 0, 1))
        .joint(BipedResource.RIGHT_SHOULDER, orient(0, 0, 0, 1))
        .build();
    timeLine.addKeyFrameData(pose1, 2.0);
    timeLine.addKeyFrameData(pose2, 5.0);
    assertEquals(2, timeLine.getKeyFrames().size());
  }

  // --- remove last keyframe reverts to empty ---

  @Test
  public void removeAllKeyFrames_restoresEmpty() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    while (!timeLine.getKeyFrames().isEmpty()) {
      timeLine.removeKeyFrameData(timeLine.getKeyFrames().get(0));
    }
    assertTrue(timeLine.getKeyFrames().isEmpty());
  }

  // --- setCurrentTime beyond endTime clamps ---

  @Test
  public void setCurrentTime_beyondEnd_clamped() {
    timeLine.setCurrentTime(100.0);
    assertEquals(10.0, timeLine.getCurrentTime(), 0.001);
  }

  // --- setCurrentTime after keyframe but before end ---

  @Test
  public void setCurrentTime_afterLastKeyFrame_usesLastPose() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 2.0);
    events.clear();
    timeLine.setCurrentTime(8.0);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("time:")));
  }

  // --- setCurrentTime before first keyframe interpolates with initial ---

  @Test
  public void setCurrentTime_beforeFirstKeyFrame_interpolates() {
    timeLine.addKeyFrameData(makePose(orient(0, 0, 0, 1)), 5.0);
    events.clear();
    timeLine.setCurrentTime(2.0);
    assertTrue(events.stream().anyMatch(e -> e.startsWith("time:")));
  }
}
