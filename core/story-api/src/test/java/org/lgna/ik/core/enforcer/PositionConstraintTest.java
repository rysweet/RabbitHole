package org.lgna.ik.core.enforcer;

import org.alice.math.immutable.Point3;
import org.lgna.ik.core.solver.Chain;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for PositionConstraint after its extraction from
 * TightPositionalIkEnforcer inner class to a top-level class (PR #558).
 *
 * These tests verify the structural contract that downstream consumers
 * (e.g. IkProgram in core/ide) depend on:
 *   - PositionConstraint is a top-level public class
 *   - It extends Constraint (the abstract extracted base)
 *   - Its constructor accepts (Chain, Point3, IkEnforcerContext)
 *   - setEeDesiredPosition and computeDesiredDisplacement are accessible
 *
 * Chain requires a full scenegraph so behavioral tests would need
 * integration-level setup. These structural tests guard the extraction
 * contract that caused the compilation break in issue #557.
 */
public class PositionConstraintTest {

  // --- Structural contract tests (guard against accidental re-nesting) ---

  @Test
  public void isTopLevelClass() {
    // PositionConstraint must NOT be an inner/nested class
    assertFalse("PositionConstraint should be a top-level class, not a nested class",
        PositionConstraint.class.isMemberClass());
    assertFalse("PositionConstraint should not be a local class",
        PositionConstraint.class.isLocalClass());
    assertFalse("PositionConstraint should not be anonymous",
        PositionConstraint.class.isAnonymousClass());
  }

  @Test
  public void isPublicClass() {
    assertTrue("PositionConstraint should be public",
        Modifier.isPublic(PositionConstraint.class.getModifiers()));
  }

  @Test
  public void extendsConstraint() {
    assertEquals("PositionConstraint should extend Constraint",
        Constraint.class, PositionConstraint.class.getSuperclass());
  }

  @Test
  public void hasExpectedConstructor() throws NoSuchMethodException {
    Constructor<?> ctor = PositionConstraint.class.getConstructor(
        Chain.class, Point3.class, IkEnforcerContext.class);
    assertNotNull("Constructor(Chain, Point3, IkEnforcerContext) must exist", ctor);
    assertTrue("Constructor should be public",
        Modifier.isPublic(ctor.getModifiers()));
  }

  @Test
  public void hasSetEeDesiredPositionMethod() throws NoSuchMethodException {
    Method m = PositionConstraint.class.getMethod("setEeDesiredPosition", Point3.class);
    assertNotNull("setEeDesiredPosition(Point3) must exist", m);
    assertTrue("setEeDesiredPosition should be public",
        Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void hasComputeDesiredDisplacementMethod() throws NoSuchMethodException {
    Method m = PositionConstraint.class.getMethod("computeDesiredDisplacement");
    assertNotNull("computeDesiredDisplacement() must exist", m);
    assertEquals("computeDesiredDisplacement should return Displacement",
        Displacement.class, m.getReturnType());
  }

  @Test
  public void hasIsMetMethod() throws NoSuchMethodException {
    Method m = PositionConstraint.class.getMethod("isMet");
    assertNotNull("isMet() must exist", m);
    assertEquals("isMet should return boolean",
        boolean.class, m.getReturnType());
  }

  @Test
  public void hasComputeJacobianMethod() throws NoSuchMethodException {
    Method m = PositionConstraint.class.getMethod("computeJacobian");
    assertNotNull("computeJacobian() must exist", m);
    assertEquals("computeJacobian should return Jacobian",
        Jacobian.class, m.getReturnType());
  }

  @Test
  public void livesInExpectedPackage() {
    assertEquals("PositionConstraint must be in org.lgna.ik.core.enforcer",
        "org.lgna.ik.core.enforcer", PositionConstraint.class.getPackage().getName());
  }
}
