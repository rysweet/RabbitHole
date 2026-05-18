package org.alice.ide.ast.export;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link MethodInfo}.
 */
public class MethodInfoExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(MethodInfo.class);
  }

  @Test
  public void extendsMemberInfo() {
    assertTrue(MemberInfo.class.isAssignableFrom(MethodInfo.class));
  }

  @Test
  public void hasTwoParameterConstructor() throws NoSuchMethodException {
    Constructor<?> ctor = MethodInfo.class.getConstructor(
        ProjectInfo.class, org.lgna.project.ast.UserMethod.class);
    assertNotNull(ctor);
  }

  @Test
  public void constructorParameterCount() {
    Constructor<?>[] ctors = MethodInfo.class.getConstructors();
    assertEquals(1, ctors.length);
    assertEquals(2, ctors[0].getParameterCount());
  }

  @Test
  public void isConcrete() {
    assertFalse(java.lang.reflect.Modifier.isAbstract(MethodInfo.class.getModifiers()));
  }

  @Test
  public void isPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(MethodInfo.class.getModifiers()));
  }

  @Test
  public void packageIsExport() {
    assertEquals("org.alice.ide.ast.export", MethodInfo.class.getPackage().getName());
  }
}
