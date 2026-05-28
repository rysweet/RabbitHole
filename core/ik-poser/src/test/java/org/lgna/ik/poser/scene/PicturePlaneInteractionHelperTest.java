package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Ray;
import org.alice.math.immutable.Vector3;
import org.junit.Test;

import static org.junit.Assert.*;

public class PicturePlaneInteractionHelperTest {
  @Test
  public void hasPlaneDragTreatsNaNAsInactive() {
    assertFalse(PicturePlaneInteractionHelper.hasPlaneDrag(Double.NaN));
    assertTrue(PicturePlaneInteractionHelper.hasPlaneDrag(0.25));
  }

  @Test
  public void hasRayDragRequiresNonNullRay() {
    assertFalse(PicturePlaneInteractionHelper.hasRayDrag(null));
    assertTrue(PicturePlaneInteractionHelper.hasRayDrag(new Ray(Point3.ORIGIN, Vector3.POSITIVE_Z_AXIS)));
  }

  @Test
  public void modeSwitchesTrackShiftState() {
    assertTrue(PicturePlaneInteractionHelper.shouldSwitchToRay(true, true));
    assertFalse(PicturePlaneInteractionHelper.shouldSwitchToRay(false, true));
    assertTrue(PicturePlaneInteractionHelper.shouldSwitchToPlane(false, true));
    assertFalse(PicturePlaneInteractionHelper.shouldSwitchToPlane(true, true));
  }

  @Test
  public void calculateRayTUsesPixelDelta() {
    assertEquals(2.5, PicturePlaneInteractionHelper.calculateRayT(2.0, 10.0, 30.0, 0.025), 0.00001);
  }
}
