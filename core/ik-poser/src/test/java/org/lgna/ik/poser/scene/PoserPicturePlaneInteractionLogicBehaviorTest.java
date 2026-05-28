package org.lgna.ik.poser.scene;

import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Ray;
import org.alice.math.immutable.Vector3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class PoserPicturePlaneInteractionLogicBehaviorTest {
  @Test
  public void getSphereRayIntersectionLengthReturnsNearestIntersectionDistanceFromCamera() {
    Ray ray = new Ray(new Point3(0, 0, -5), new Vector3(0, 0, 1));

    assertEquals(1.0, PoserPicturePlaneInteractionLogic.getSphereRayIntersectionLength(ray, Point3.ORIGIN, 1.0), 0.00001);
  }

  @Test
  public void getSphereRayIntersectionLengthReturnsMinusOneWhenRayMissesSphere() {
    Ray ray = new Ray(new Point3(3, 0, -5), new Vector3(3, 0, 1));

    assertEquals(-1.0, PoserPicturePlaneInteractionLogic.getSphereRayIntersectionLength(ray, Point3.ORIGIN, 1.0), 0.00001);
  }

  @Test
  public void pickPreferredUsesCameraDistanceWhenDifferenceIsDecisive() {
    assertSame(PoserPicturePlaneInteractionLogic.Selection.FIRST,
        PoserPicturePlaneInteractionLogic.pickPreferred(1.0, 2.0, 10.0, 0.5, 0.1));
    assertSame(PoserPicturePlaneInteractionLogic.Selection.SECOND,
        PoserPicturePlaneInteractionLogic.pickPreferred(3.0, 1.0, 0.5, 10.0, 0.1));
  }

  @Test
  public void pickPreferredBreaksNearTiesWithIntersectionDistance() {
    assertSame(PoserPicturePlaneInteractionLogic.Selection.SECOND,
        PoserPicturePlaneInteractionLogic.pickPreferred(1.0, 1.05, 4.0, 1.0, 0.1));
    assertSame(PoserPicturePlaneInteractionLogic.Selection.FIRST,
        PoserPicturePlaneInteractionLogic.pickPreferred(2.0, 2.0, 1.0, 2.0, 0.1));
  }
}
