package org.lgna.ik.poser.scene;

import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractPoserSceneDeepBehaviorTest {
  @Test
  public void computeBackupAmountHandlesZeroAndSquareDimensions() {
    assertEquals(0.0, AbstractPoserSceneLogic.computeBackupAmount(0.0, 0.0), 0.00001);
    assertEquals(7.5, AbstractPoserSceneLogic.computeBackupAmount(3.0, 3.0), 0.00001);
  }
}
