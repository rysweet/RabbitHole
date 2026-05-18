package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for ExportProjectOperation.
 */
public class ExportProjectOperationExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(ExportProjectOperation.class);
  }

  @Test
  public void classIsPublic() {
    assertTrue(Modifier.isPublic(ExportProjectOperation.class.getModifiers()));
  }

  @Test
  public void packageIsProjecturi() {
    assertEquals("org.alice.ide.croquet.models.projecturi",
        ExportProjectOperation.class.getPackage().getName());
  }

  @Test
  public void hasDeclaredMethods() {
    assertTrue(ExportProjectOperation.class.getDeclaredMethods().length > 0);
  }

  @Test
  public void isNotAbstract() {
    assertFalse(Modifier.isAbstract(ExportProjectOperation.class.getModifiers()));
  }
}
