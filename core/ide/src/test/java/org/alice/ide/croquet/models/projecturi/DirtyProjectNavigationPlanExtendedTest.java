package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for DirtyProjectNavigationPlan.
 */
public class DirtyProjectNavigationPlanExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(DirtyProjectNavigationPlan.class);
  }

  @Test
  public void classIsFinal() {
    assertTrue(Modifier.isFinal(DirtyProjectNavigationPlan.class.getModifiers()));
  }

  @Test
  public void packageIsProjecturi() {
    assertEquals("org.alice.ide.croquet.models.projecturi",
        DirtyProjectNavigationPlan.class.getPackage().getName());
  }

  @Test
  public void hasDeclaredMethods() {
    assertTrue(DirtyProjectNavigationPlan.class.getDeclaredMethods().length > 0 ||
        DirtyProjectNavigationPlan.class.getMethods().length > 0);
  }

  @Test
  public void isNotAbstract() {
    assertFalse(Modifier.isAbstract(DirtyProjectNavigationPlan.class.getModifiers()));
  }
}
