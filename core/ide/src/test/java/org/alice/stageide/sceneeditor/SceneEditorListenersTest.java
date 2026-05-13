package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * TDD contract tests for the extracted SceneEditorListeners helper class.
 * Consolidates the 7 anonymous ValueListener fields formerly declared
 * inline in StorytellingSceneEditor (lines 246-293).
 *
 * Pure reflection — no GUI, no instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class SceneEditorListenersTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.SceneEditorListeners";
  private static Class<?> clazz;

  @BeforeClass
  public static void loadClass() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneEditorListeners must exist as a top-level class: " + e.getMessage());
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

  // ── All 7 listener fields exist ───────────────────────────────────

  @Test
  public void hasShowSnapGridListener() {
    assertDeclaredField("showSnapGridListener");
  }

  @Test
  public void hasSnapEnabledListener() {
    assertDeclaredField("snapEnabledListener");
  }

  @Test
  public void hasSnapGridSpacingListener() {
    assertDeclaredField("snapGridSpacingListener");
  }

  @Test
  public void hasCameraMarkerFieldSelectionListener() {
    assertDeclaredField("cameraMarkerFieldSelectionListener");
  }

  @Test
  public void hasObjectMarkerFieldSelectionListener() {
    assertDeclaredField("objectMarkerFieldSelectionListener");
  }

  @Test
  public void hasInstanceFactorySelectionListener() {
    assertDeclaredField("instanceFactorySelectionListener");
  }

  @Test
  public void hasMainCameraViewSelectionObserver() {
    assertDeclaredField("mainCameraViewSelectionObserver");
  }

  // ── Listener fields are final ─────────────────────────────────────

  @Test
  public void allListenerFieldsAreFinal() {
    String[] fieldNames = {
        "showSnapGridListener",
        "snapEnabledListener",
        "snapGridSpacingListener",
        "cameraMarkerFieldSelectionListener",
        "objectMarkerFieldSelectionListener",
        "instanceFactorySelectionListener",
        "mainCameraViewSelectionObserver"
    };
    for (String name : fieldNames) {
      try {
        Field f = clazz.getDeclaredField(name);
        assertTrue(name + " must be final", Modifier.isFinal(f.getModifiers()));
      } catch (NoSuchFieldException e) {
        fail("Missing field: " + name);
      }
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private static void assertDeclaredField(String name) {
    try {
      clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      fail("Missing declared field: " + name);
    }
  }
}
