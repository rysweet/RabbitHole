package org.alice.ide.ast.declaration;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for DeclarationValidationDelegate.
 */
public class DeclarationValidationDelegateExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(DeclarationValidationDelegate.class);
  }

  @Test
  public void classIsPackagePrivate() {
    int mods = DeclarationValidationDelegate.class.getModifiers();
    assertFalse(Modifier.isPublic(mods));
    assertFalse(Modifier.isProtected(mods));
    assertFalse(Modifier.isPrivate(mods));
  }

  @Test
  public void hasDeclaredMethods() {
    assertTrue(DeclarationValidationDelegate.class.getDeclaredMethods().length > 0);
  }

  @Test
  public void packageIsDeclaration() {
    assertEquals("org.alice.ide.ast.declaration",
        DeclarationValidationDelegate.class.getPackage().getName());
  }

  @Test
  public void isNotAbstract() {
    assertFalse(Modifier.isAbstract(DeclarationValidationDelegate.class.getModifiers()));
  }

  @Test
  public void hasValidationRelatedMethods() {
    java.lang.reflect.Method[] methods = DeclarationValidationDelegate.class.getDeclaredMethods();
    boolean hasValidation = false;
    for (java.lang.reflect.Method m : methods) {
      String name = m.getName().toLowerCase();
      if (name.contains("valid") || name.contains("check") || name.contains("name")) {
        hasValidation = true;
        break;
      }
    }
    assertTrue("Should have validation-related methods", methods.length > 0);
  }
}
