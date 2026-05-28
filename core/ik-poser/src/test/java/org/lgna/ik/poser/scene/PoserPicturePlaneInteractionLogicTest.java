package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Ray;
import org.alice.math.immutable.Vector3;
import org.junit.Test;

import static org.junit.Assert.*;

public class PoserPicturePlaneInteractionLogicTest {
  @Test
  public void getSphereRayIntersectionLengthReturnsDistanceToIntersection() {
    Ray ray = new Ray(new Point3(0, 0, -5), new Vector3(0, 0, 1));

    assertEquals(1.0, PoserPicturePlaneInteractionLogic.getSphereRayIntersectionLength(ray, Point3.ORIGIN, 1.0), 0.00001);
  }

  @Test
  public void pickPreferredUsesCameraDistanceWhenClearlyDifferent() {
    assertEquals(PoserPicturePlaneInteractionLogic.Selection.FIRST, PoserPicturePlaneInteractionLogic.pickPreferred(1.0, 2.0, 10.0, 1.0, 0.1));
  }

  @Test
  public void pickPreferredFallsBackToIntersectionDistanceWhenCameraDistanceSimilar() {
    assertEquals(PoserPicturePlaneInteractionLogic.Selection.SECOND, PoserPicturePlaneInteractionLogic.pickPreferred(1.0, 1.05, 4.0, 1.0, 0.1));
  }
}
