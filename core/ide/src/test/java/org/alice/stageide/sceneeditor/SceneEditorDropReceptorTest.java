package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * TDD contract tests for the extracted SceneEditorDropReceptor class.
 * Defines the expected structure AFTER extraction from
 * StorytellingSceneEditor inner class to a top-level package-private class.
 *
 * Pure reflection — no GUI, no instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class SceneEditorDropReceptorTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.SceneEditorDropReceptor";
  private static Class<?> clazz;

  @BeforeClass
  public static void loadClass() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneEditorDropReceptor must exist as a top-level class: " + e.getMessage());
    }
  }

  // ── Visibility and structure ──────────────────────────────────────

  @Test
  public void isPackagePrivate() {
    int mods = clazz.getModifiers();
    assertFalse("must not be public", Modifier.isPublic(mods));
    assertFalse("must not be private", Modifier.isPrivate(mods));
    assertFalse("must not be protected", Modifier.isProtected(mods));
  }

  @Test
  public void isNotInnerClass() {
    assertNull("must not be an inner/nested class", clazz.getEnclosingClass());
  }

  @Test
  public void isNotAbstract() {
    assertFalse("must be concrete", Modifier.isAbstract(clazz.getModifiers()));
  }

  // ── Hierarchy ─────────────────────────────────────────────────────

  @Test
  public void extendsAbstractDropReceptor() {
    assertEquals("org.lgna.croquet.AbstractDropReceptor", clazz.getSuperclass().getName());
  }

  // ── Constructor ───────────────────────────────────────────────────

  @Test
  public void constructorTakesStorytellingSceneEditor() {
    Constructor<?>[] ctors = clazz.getDeclaredConstructors();
    boolean found = false;
    for (Constructor<?> c : ctors) {
      Class<?>[] params = c.getParameterTypes();
      if (params.length == 1
          && params[0].getName().equals("org.alice.stageide.sceneeditor.StorytellingSceneEditor")) {
        found = true;
      }
    }
    assertTrue("must have constructor(StorytellingSceneEditor)", found);
  }

  // ── Override methods from AbstractDropReceptor / DropReceptor ─────

  @Test
  public void hasIsPotentiallyAcceptingOf() {
    assertHasMethod("isPotentiallyAcceptingOf", "org.lgna.croquet.DragModel");
  }

  @Test
  public void hasDragStarted() {
    assertHasMethod("dragStarted", "org.lgna.croquet.history.DragStep");
  }

  @Test
  public void hasDragEntered() {
    assertHasMethod("dragEntered", "org.lgna.croquet.history.DragStep");
  }

  @Test
  public void hasDragUpdated() {
    assertHasMethod("dragUpdated", "org.lgna.croquet.history.DragStep");
  }

  @Test
  public void hasDragDroppedPostRejectorCheck() {
    assertHasMethod("dragDroppedPostRejectorCheck", "org.lgna.croquet.history.DragStep");
  }

  @Test
  public void hasDragExited() {
    assertHasMethodByName("dragExited");
  }

  @Test
  public void hasDragStopped() {
    assertHasMethod("dragStopped", "org.lgna.croquet.history.DragStep");
  }

  @Test
  public void hasGetTrackableShape() {
    assertHasMethodByName("getTrackableShape");
  }

  @Test
  public void hasGetViewController() {
    assertHasMethodByName("getViewController");
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private static void assertHasMethod(String name, String... paramTypeNames) {
    try {
      Class<?>[] paramTypes = new Class<?>[paramTypeNames.length];
      for (int i = 0; i < paramTypeNames.length; i++) {
        paramTypes[i] = Class.forName(paramTypeNames[i]);
      }
      clazz.getDeclaredMethod(name, paramTypes);
    } catch (Exception e) {
      fail("Missing method: " + name + "("
          + String.join(", ", paramTypeNames) + "): " + e.getMessage());
    }
  }

  private static void assertHasMethodByName(String name) {
    boolean found = Arrays.stream(clazz.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertTrue("Missing method: " + name, found);
  }
}
