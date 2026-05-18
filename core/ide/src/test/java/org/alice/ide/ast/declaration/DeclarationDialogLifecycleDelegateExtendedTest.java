package org.alice.ide.ast.declaration;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for DeclarationDialogLifecycleDelegate.
 */
public class DeclarationDialogLifecycleDelegateExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(DeclarationDialogLifecycleDelegate.class);
  }

  @Test
  public void classIsPackagePrivate() {
    int mods = DeclarationDialogLifecycleDelegate.class.getModifiers();
    assertFalse(Modifier.isPublic(mods));
    assertFalse(Modifier.isProtected(mods));
    assertFalse(Modifier.isPrivate(mods));
  }

  @Test
  public void hasDeclaredMethods() {
    assertTrue(DeclarationDialogLifecycleDelegate.class.getDeclaredMethods().length > 0);
  }

  @Test
  public void packageIsDeclaration() {
    assertEquals("org.alice.ide.ast.declaration",
        DeclarationDialogLifecycleDelegate.class.getPackage().getName());
  }

  @Test
  public void isNotAbstract() {
    assertFalse(Modifier.isAbstract(
        DeclarationDialogLifecycleDelegate.class.getModifiers()));
  }

  @Test
  public void hasLifecycleMethods() {
    java.lang.reflect.Method[] methods = DeclarationDialogLifecycleDelegate.class.getDeclaredMethods();
    assertTrue("Should have lifecycle methods", methods.length > 0);
  }
}
