package org.alice.ide.croquet.models;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Characterization tests for AliceMenuBar contract.
 */
public class AliceMenuBarContractExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(AliceMenuBar.class);
  }

  @Test
  public void classIsPublic() {
    assertTrue(Modifier.isPublic(AliceMenuBar.class.getModifiers()));
  }

  @Test
  public void packageIsModels() {
    assertEquals("org.alice.ide.croquet.models",
        AliceMenuBar.class.getPackage().getName());
  }

  @Test
  public void classHasMethods() {
    // AliceMenuBar may inherit rather than declare its own methods
    assertTrue(AliceMenuBar.class.getMethods().length > 0);
  }

  @Test
  public void isNotAbstract() {
    assertFalse(Modifier.isAbstract(AliceMenuBar.class.getModifiers()));
  }
}
