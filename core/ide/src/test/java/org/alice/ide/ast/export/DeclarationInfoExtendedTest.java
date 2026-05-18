package org.alice.ide.ast.export;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link DeclarationInfo}.
 */
public class DeclarationInfoExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(DeclarationInfo.class);
  }

  @Test
  public void isAbstract() {
    assertTrue(java.lang.reflect.Modifier.isAbstract(DeclarationInfo.class.getModifiers()));
  }

  @Test
  public void isPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(DeclarationInfo.class.getModifiers()));
  }

  @Test
  public void hasAppendDesiredMethod() {
    boolean found = Arrays.stream(DeclarationInfo.class.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals("appendDesired"));
    assertTrue(found);
  }

  @Test
  public void hasResetRequiredMethod() {
    boolean found = Arrays.stream(DeclarationInfo.class.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals("resetRequired"));
    assertTrue(found);
  }

  @Test
  public void hasUpdateRequiredMethod() {
    boolean found = Arrays.stream(DeclarationInfo.class.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals("updateRequired"));
    assertTrue(found);
  }

  @Test
  public void hasUpdateSwingMethod() {
    boolean found = Arrays.stream(DeclarationInfo.class.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals("updateSwing"));
    assertTrue(found);
  }

  @Test
  public void hasGetDeclarationMethod() throws NoSuchMethodException {
    Method method = DeclarationInfo.class.getMethod("getDeclaration");
    assertNotNull(method);
  }

  @Test
  public void typeInfoExtendsDeclarationInfo() {
    assertTrue(DeclarationInfo.class.isAssignableFrom(TypeInfo.class));
  }

  @Test
  public void memberInfoExtendsDeclarationInfo() {
    assertTrue(DeclarationInfo.class.isAssignableFrom(MemberInfo.class));
  }

  @Test
  public void packageIsExport() {
    assertEquals("org.alice.ide.ast.export", DeclarationInfo.class.getPackage().getName());
  }
}
