package org.alice.ide.ast.export;

import org.junit.Test;
import org.lgna.project.ast.UserConstructor;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

public class ConstructorInfoExtendedTest {

  @Test
  public void classIsLoadableFromExpectedPackage() throws Exception {
    Class<?> constructorInfoClass = Class.forName("org.alice.ide.ast.export.ConstructorInfo");

    assertSame(ConstructorInfo.class, constructorInfoClass);
    assertEquals("org.alice.ide.ast.export", constructorInfoClass.getPackage().getName());
    assertEquals("ConstructorInfo", constructorInfoClass.getSimpleName());
  }

  @Test
  public void classExtendsMemberInfo() {
    assertEquals(MemberInfo.class, ConstructorInfo.class.getSuperclass());
  }

  @Test
  public void constructorParameterTypesMatchProjectInfoAndUserConstructor() {
    Constructor<?>[] constructors = ConstructorInfo.class.getDeclaredConstructors();

    assertEquals(1, constructors.length);
    assertArrayEquals(new Class<?>[] {ProjectInfo.class, UserConstructor.class},
        constructors[0].getParameterTypes());
  }
}
