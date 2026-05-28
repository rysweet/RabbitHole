package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Ray;
import org.alice.math.immutable.Vector3;
import org.junit.Test;

import static org.junit.Assert.*;

public class PoserPicturePlaneInteractionDeepBehaviorTest {
  @Test
  public void getSphereRayIntersectionLengthReturnsNegativeOneWhenRayMisses() {
    Ray ray = new Ray(new Point3(0, 0, -5), new Vector3(5, 0, 0));

    assertEquals(-1.0, PoserPicturePlaneInteractionLogic.getSphereRayIntersectionLength(ray, Point3.ORIGIN, 1.0), 0.00001);
  }

  @Test
  public void pickPreferredUsesIntersectionDistanceWhenCameraDistancesTie() {
    assertEquals(PoserPicturePlaneInteractionLogic.Selection.FIRST,
        PoserPicturePlaneInteractionLogic.pickPreferred(1.0, 1.0, 0.5, 1.0, 0.1));
  }
}
