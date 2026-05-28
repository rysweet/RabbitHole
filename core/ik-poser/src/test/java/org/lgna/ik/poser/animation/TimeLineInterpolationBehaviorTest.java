package org.lgna.ik.poser.animation;

import org.alice.math.immutable.UnitQuaternion;
import org.junit.Before;
import org.junit.Test;
import org.lgna.story.BipedPose;
import org.lgna.story.BipedPoseBuilder;
import org.lgna.story.Orientation;
import org.lgna.story.Pose;
import org.lgna.story.resources.BipedResource;
import org.lgna.story.resources.JointId;
import org.lgna.story.implementation.JointIdTransformationPair;

import static org.junit.Assert.*;

public class TimeLineInterpolationBehaviorTest {
  private TimeLine timeLine;
  private Pose<?> lastPose;

  @Before
  public void setUp() {
    timeLine = new TimeLine();
    timeLine.addListener(new TimeLineListener() {
      @Override public void currentTimeChanged(double time, Pose pose) { lastPose = pose; }
      @Override public void endTimeChanged(double endTime) {}
      @Override public void keyFrameAdded(KeyFrameData keyFrame) {}
      @Override public void keyFrameModified(KeyFrameData keyFrame) {}
      @Override public void keyFrameDeleted(KeyFrameData keyFrame) {}
      @Override public void selectedKeyFrameChanged(KeyFrameData keyFrame) {}
    });
  }

  @Test
  public void interpolateBeforeFirstKeyFrame_blendsFromInitialPose() {
    Orientation initialLeft = orientation(0.0, 0.0, 0.0, 1.0);
    Orientation keyLeft = orientation(0.70710678, 0.0, 0.0, 0.70710678);
    timeLine.setInitialPose(poseWithLeftRight(initialLeft, orientation(0.0, 0.0, 0.0, 1.0)));
    timeLine.addKeyFrameData(poseWithLeft(keyLeft), 4.0);

    Pose<?> interpolated = capturePoseAt(2.0);
    UnitQuaternion expected = initialLeft.asUnitQuaternion().interpolate(keyLeft.asUnitQuaternion(), 0.5);

    assertQuaternionAligned(expected, quaternionFor(interpolated, BipedResource.LEFT_SHOULDER));
  }

  @Test
  public void addingNewJoint_backfillsEarlierPoseActualsFromInitialPose() {
    Orientation initialLeft = orientation(0.0, 0.0, 0.0, 1.0);
    Orientation initialRight = orientation(0.0, 0.0, 0.38268343, 0.92387953);
    timeLine.setInitialPose(poseWithLeftRight(initialLeft, initialRight));

    timeLine.addKeyFrameData(poseWithLeft(orientation(0.70710678, 0.0, 0.0, 0.70710678)), 2.0);
    timeLine.addKeyFrameData(poseWithRight(orientation(0.0, 0.70710678, 0.0, 0.70710678)), 6.0);

    KeyFrameData first = timeLine.getKeyFrames().get(0);
    KeyFrameData second = timeLine.getKeyFrames().get(1);

    assertQuaternionAligned(initialRight.asUnitQuaternion(), quaternionFor(first.getPoseActual(), BipedResource.RIGHT_SHOULDER));
    assertQuaternionAligned(initialLeft.asUnitQuaternion(), quaternionFor(second.getPoseActual(), BipedResource.LEFT_SHOULDER));
  }

  @Test
  public void interpolationForOmittedJoint_fallsBackToInitialPose() {
    Orientation initialLeft = orientation(0.0, 0.0, 0.0, 1.0);
    Orientation initialRight = orientation(0.0, 0.0, 0.0, 1.0);
    Orientation firstLeft = orientation(0.70710678, 0.0, 0.0, 0.70710678);
    Orientation secondRight = orientation(0.0, 0.70710678, 0.0, 0.70710678);
    timeLine.setInitialPose(poseWithLeftRight(initialLeft, initialRight));

    timeLine.addKeyFrameData(poseWithLeft(firstLeft), 2.0);
    timeLine.addKeyFrameData(poseWithRight(secondRight), 6.0);

    Pose<?> interpolated = capturePoseAt(4.0);

    assertQuaternionAligned(
        firstLeft.asUnitQuaternion().interpolate(initialLeft.asUnitQuaternion(), 0.5),
        quaternionFor(interpolated, BipedResource.LEFT_SHOULDER));
    assertQuaternionAligned(
        initialRight.asUnitQuaternion().interpolate(secondRight.asUnitQuaternion(), 0.5),
        quaternionFor(interpolated, BipedResource.RIGHT_SHOULDER));
  }

  private Pose<?> capturePoseAt(double time) {
    lastPose = null;
    timeLine.setCurrentTime(time);
    assertNotNull(lastPose);
    return lastPose;
  }

  private static BipedPose poseWithLeft(Orientation left) {
    return new BipedPoseBuilder()
        .joint(BipedResource.LEFT_SHOULDER, left)
        .build();
  }

  private static BipedPose poseWithRight(Orientation right) {
    return new BipedPoseBuilder()
        .joint(BipedResource.RIGHT_SHOULDER, right)
        .build();
  }

  private static BipedPose poseWithLeftRight(Orientation left, Orientation right) {
    return new BipedPoseBuilder()
        .joint(BipedResource.LEFT_SHOULDER, left)
        .joint(BipedResource.RIGHT_SHOULDER, right)
        .build();
  }

  private static Orientation orientation(double x, double y, double z, double w) {
    return new Orientation(x, y, z, w);
  }

  private static UnitQuaternion quaternionFor(Pose<?> pose, JointId jointId) {
    for (JointIdTransformationPair pair : pose.getJointIdTransformationPairs()) {
      if (pair.getJointId().equals(jointId)) {
        return pair.getTransformation().orientation().asUnitQuaternion();
      }
    }
    fail("Missing joint: " + jointId);
    return UnitQuaternion.NaN;
  }

  private static void assertQuaternionAligned(UnitQuaternion expected, UnitQuaternion actual) {
    assertTrue("Expected " + expected + " but was " + actual, expected.isAlignedWith(actual));
  }
}
