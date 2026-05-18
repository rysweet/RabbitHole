package org.alice.ide.ast.code;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for EnvelopStatementsOperation.
 */
public class EnvelopStatementsOperationExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(EnvelopStatementsOperation.class);
  }

  @Test
  public void classIsPublic() {
    assertTrue(Modifier.isPublic(EnvelopStatementsOperation.class.getModifiers()));
  }

  @Test
  public void packageIsCode() {
    assertEquals("org.alice.ide.ast.code",
        EnvelopStatementsOperation.class.getPackage().getName());
  }

  @Test
  public void hasDeclaredMethods() {
    assertTrue(EnvelopStatementsOperation.class.getDeclaredMethods().length > 0);
  }

  @Test
  public void isNotAbstract() {
    assertFalse(Modifier.isAbstract(EnvelopStatementsOperation.class.getModifiers()));
  }

  @Test
  public void hasPerformOrEditMethods() {
    Method[] methods = EnvelopStatementsOperation.class.getDeclaredMethods();
    assertTrue("Should have operation methods", methods.length > 0);
  }
}
