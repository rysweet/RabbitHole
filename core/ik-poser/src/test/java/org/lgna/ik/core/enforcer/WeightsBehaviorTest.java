package org.lgna.ik.core.enforcer;

import org.junit.Test;
import org.lgna.story.resources.JointId;

import static org.junit.Assert.assertEquals;

public class WeightsBehaviorTest {
  @Test
  public void getEffectiveJointWeight_usesDefaultWhenNoOverrideExists() {
    Weights weights = new Weights();
    JointId jointId = new JointId(null, null);

    weights.setDefaultJointWeight(0.35);

    assertEquals(0.35, weights.getEffectiveJointWeight(jointId), 0.0);
  }

  @Test
  public void getEffectiveJointWeight_prefersExplicitJointOverride() {
    Weights weights = new Weights();
    JointId jointId = new JointId(null, null);

    weights.setDefaultJointWeight(0.35);
    weights.setJointWeight(jointId, 0.8);

    assertEquals(0.8, weights.getEffectiveJointWeight(jointId), 0.0);
  }
}
