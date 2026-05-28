package org.alice.ide.croquet.models.clipboard;

import org.alice.ide.operations.InconsequentialActionOperation;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ClipboardOperationsStructureTest {

  private static final Class<?>[] CLASSES = {
      CopyOperation.class,
      CutOperation.class,
      PasteOperation.class
  };

  private static void assertSingletonOperationShape(Class<?> type) throws Exception {
    assertTrue("class must be public", Modifier.isPublic(type.getModifiers()));
    assertFalse("class must be concrete", Modifier.isAbstract(type.getModifiers()));
    assertTrue("class must extend InconsequentialActionOperation",
        InconsequentialActionOperation.class.isAssignableFrom(type));

    Class<?> holder = Class.forName(type.getName() + "$SingletonHolder", false, type.getClassLoader());
    assertTrue("SingletonHolder must be private", Modifier.isPrivate(holder.getModifiers()));

    Method getInstance = type.getMethod("getInstance");
    assertTrue("getInstance must be public", Modifier.isPublic(getInstance.getModifiers()));
    assertTrue("getInstance must be static", Modifier.isStatic(getInstance.getModifiers()));
    assertEquals("getInstance must return declaring type", type, getInstance.getReturnType());

    Constructor<?> constructor = type.getDeclaredConstructor();
    assertTrue("constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
    assertEquals(0, constructor.getParameterCount());

    Method performInternal = type.getDeclaredMethod("performInternal");
    assertTrue("performInternal must be protected", Modifier.isProtected(performInternal.getModifiers()));
    assertEquals(void.class, performInternal.getReturnType());
  }

  @Test
  public void clipboardOperations_whenCounted_includeThreeClasses() {
    assertEquals(3, CLASSES.length);
  }

  @Test
  public void clipboardOperations_whenReflected_arePublicConcreteSingletonOperations() throws Exception {
    for (Class<?> type : CLASSES) {
      assertSingletonOperationShape(type);
    }
  }

  @Test
  public void clipboardOperations_getInstance_whenReflected_haveNoParameters() throws Exception {
    for (Class<?> type : CLASSES) {
      Method method = type.getMethod("getInstance");
      assertEquals(0, method.getParameterCount());
      assertEquals(type, method.getReturnType());
    }
  }

  @Test
  public void clipboardOperations_constructors_whenReflected_areNotPublic() {
    for (Class<?> type : CLASSES) {
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        assertFalse(type.getSimpleName() + " constructor must not be public",
            Modifier.isPublic(constructor.getModifiers()));
      }
    }
  }

  @Test
  public void clipboardOperations_declaredPublicMethods_whenReflected_onlyExposeGetInstance() {
    for (Class<?> type : CLASSES) {
      int publicCount = 0;
      for (Method method : type.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          publicCount++;
          assertEquals("getInstance", method.getName());
        }
      }
      assertEquals(type.getSimpleName() + " should declare one public method", 1, publicCount);
    }
  }

  @Test
  public void clipboardOperations_names_whenCollected_areUnique() {
    Set<String> names = new HashSet<>();
    for (Class<?> type : CLASSES) {
      assertTrue("duplicate simple name: " + type.getSimpleName(), names.add(type.getSimpleName()));
    }
  }

  @Test
  public void copyOperation_whenReflected_declaresOwnPerformInternal() throws Exception {
    Method method = CopyOperation.class.getDeclaredMethod("performInternal");
    assertEquals(CopyOperation.class, method.getDeclaringClass());
    assertEquals(void.class, method.getReturnType());
  }

  @Test
  public void cutOperation_whenReflected_declaresOwnPerformInternal() throws Exception {
    Method method = CutOperation.class.getDeclaredMethod("performInternal");
    assertEquals(CutOperation.class, method.getDeclaringClass());
    assertEquals(void.class, method.getReturnType());
  }

  @Test
  public void pasteOperation_whenReflected_declaresOwnPerformInternal() throws Exception {
    Method method = PasteOperation.class.getDeclaredMethod("performInternal");
    assertEquals(PasteOperation.class, method.getDeclaringClass());
    assertEquals(void.class, method.getReturnType());
  }

  @Test
  public void clipboardOperations_whenLoaded_areAccessible() {
    assertNotNull(CopyOperation.class);
    assertNotNull(CutOperation.class);
    assertNotNull(PasteOperation.class);
  }

  @Test
  public void clipboardOperations_inheritance_whenReflected_matchesDirectSuperclass() {
    assertEquals(InconsequentialActionOperation.class, CopyOperation.class.getSuperclass());
    assertEquals(InconsequentialActionOperation.class, CutOperation.class.getSuperclass());
    assertEquals(InconsequentialActionOperation.class, PasteOperation.class.getSuperclass());
  }
}
