package org.lgna.ik.core.enforcer;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Downstream contract test verifying that core/ide can import
 * PositionConstraint as a standalone top-level class from core/story-api.
 *
 * This is the exact compilation contract broken by issue #557: IkProgram
 * imported TightPositionalIkEnforcer.PositionConstraint, but PR #558
 * promoted PositionConstraint to a top-level class. If anyone accidentally
 * reverts the extraction, this test will fail to compile — catching the
 * break before it reaches develop.
 */
public class PositionConstraintTopLevelContractTest {

  @Test
  public void positionConstraintIsImportableAsTopLevelClass() {
    // This line would fail to compile if PositionConstraint were still
    // an inner class of TightPositionalIkEnforcer
    Class<?> clazz = PositionConstraint.class;

    assertFalse("PositionConstraint must be a top-level class (not nested inside TightPositionalIkEnforcer)",
        clazz.isMemberClass());
    assertTrue("PositionConstraint must be public",
        Modifier.isPublic(clazz.getModifiers()));
    assertEquals("PositionConstraint must live in org.lgna.ik.core.enforcer package",
        "org.lgna.ik.core.enforcer", clazz.getPackage().getName());
  }

  @Test
  public void positionConstraintExtendsConstraintNotInnerClass() {
    // Verify class hierarchy: PositionConstraint → Constraint (abstract)
    // If it were still TightPositionalIkEnforcer.PositionConstraint,
    // the enclosing class would be non-null
    assertEquals("PositionConstraint superclass must be Constraint",
        Constraint.class, PositionConstraint.class.getSuperclass());
    assertEquals("PositionConstraint must not have an enclosing class",
        null, PositionConstraint.class.getEnclosingClass());
  }

  @Test
  public void tightPositionalIkEnforcerHasNoPositionConstraintInnerClass() {
    // Verify that TightPositionalIkEnforcer no longer declares
    // PositionConstraint as a member class
    for (Class<?> inner : TightPositionalIkEnforcer.class.getDeclaredClasses()) {
      assertFalse(
          "TightPositionalIkEnforcer should not have an inner class named PositionConstraint",
          "PositionConstraint".equals(inner.getSimpleName()));
    }
  }
}
