package org.lgna.ik.core.enforcer;

import org.alice.math.immutable.Vector3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class IkEnforcerMathTest {
  @Test
  public void clampToMagnitude_returnsOriginalVectorWhenAlreadyWithinLimit() {
    Vector3 vector = new Vector3(1.0, 2.0, 2.0);

    assertSame(vector, IkEnforcerMath.clampToMagnitude(vector, 3.0));
  }

  @Test
  public void clampToMagnitude_scalesLongVectorToRequestedMagnitude() {
    Vector3 clamped = IkEnforcerMath.clampToMagnitude(new Vector3(3.0, 4.0, 0.0), 2.0);

    assertEquals(1.2, clamped.x(), 0.00001);
    assertEquals(1.6, clamped.y(), 0.00001);
    assertEquals(0.0, clamped.z(), 0.00001);
  }

  @Test
  public void reduceDeltaTimeForError_halvesUntilErrorIsAcceptable() {
    double deltaTime = IkEnforcerMath.reduceDeltaTimeForError(1.0, 0.05, 3.0, dt -> dt * 10.0);

    assertEquals(0.25, deltaTime, 0.0);
  }

  @Test
  public void reduceDeltaTimeForError_stopsWhenNextHalvingWouldCrossMinimum() {
    double deltaTime = IkEnforcerMath.reduceDeltaTimeForError(1.0, 0.2, 0.1, dt -> dt * 10.0);

    assertEquals(0.25, deltaTime, 0.0);
  }

  @Test
  public void computeWeightedAxisContribution_scalesAxisByTimeSpeedAndWeight() {
    Vector3 contribution = IkEnforcerMath.computeWeightedAxisContribution(new Vector3(0.0, 1.0, 0.0), 0.5, 6.0, 0.25);

    assertEquals(0.0, contribution.x(), 0.0);
    assertEquals(0.75, contribution.y(), 0.0);
    assertEquals(0.0, contribution.z(), 0.0);
  }
}
