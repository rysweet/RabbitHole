package org.lgna.ik.poser.scene;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AbstractPoserSceneLogicBehaviorTest {
  @Test
  public void computeBackupAmountUsesLargestDimension() {
    assertEquals(10.0, AbstractPoserSceneLogic.computeBackupAmount(4.0, 2.0), 0.00001);
    assertEquals(12.5, AbstractPoserSceneLogic.computeBackupAmount(3.0, 5.0), 0.00001);
  }

  @Test
  public void computeBackupAmountReturnsZeroForEmptyBounds() {
    assertEquals(0.0, AbstractPoserSceneLogic.computeBackupAmount(0.0, 0.0), 0.00001);
  }
}
