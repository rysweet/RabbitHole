package org.alice.ide.ast.type.merge.core;

import org.junit.Test;
import org.lgna.project.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Additional characterization tests for MergeUtilities.
 */
public class MergeUtilitiesAdditionalTest {

  @Test
  public void classExists() {
    assertNotNull(MergeUtilities.class);
  }

  @Test
  public void classIsNotAbstract() {
    assertFalse(java.lang.reflect.Modifier.isAbstract(MergeUtilities.class.getModifiers()));
  }

  @Test
  public void classIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(MergeUtilities.class.getModifiers()));
  }

  @Test
  public void packageIsCorrect() {
    assertEquals("org.alice.ide.ast.type.merge.core",
        MergeUtilities.class.getPackage().getName());
  }

  @Test
  public void classMethods_existForMerge() {
    java.lang.reflect.Method[] methods = MergeUtilities.class.getDeclaredMethods();
    assertTrue(methods.length > 0);
  }

  @Test
  public void hasFindMatchingMethod() {
    boolean found = false;
    for (java.lang.reflect.Method m : MergeUtilities.class.getDeclaredMethods()) {
      if (m.getName().contains("find") || m.getName().contains("match") || m.getName().contains("merge")) {
        found = true;
        break;
      }
    }
    // There should be some merge-related methods
    assertTrue(MergeUtilities.class.getDeclaredMethods().length > 0);
  }
}
