package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * TDD contract tests for the extracted LookingGlassPanel class.
 * Defines the expected structure AFTER extraction from
 * StorytellingSceneEditor inner class to a top-level package-private class.
 *
 * Pure reflection — no GUI, no instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class LookingGlassPanelTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.LookingGlassPanel";
  private static Class<?> clazz;

  @BeforeClass
  public static void loadClass() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("LookingGlassPanel must exist as a top-level class: " + e.getMessage());
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
  public void extendsCompassPointSpringPanel() {
    assertEquals("org.lgna.croquet.views.CompassPointSpringPanel",
        clazz.getSuperclass().getName());
  }

  // ── Constructor ───────────────────────────────────────────────────

  @Test
  public void constructorTakesOnscreenRenderTarget() {
    Constructor<?>[] ctors = clazz.getDeclaredConstructors();
    boolean found = false;
    for (Constructor<?> c : ctors) {
      Class<?>[] params = c.getParameterTypes();
      if (params.length == 1
          && params[0].getName().equals("edu.cmu.cs.dennisc.render.OnscreenRenderTarget")) {
        found = true;
      }
    }
    assertTrue("must have constructor(OnscreenRenderTarget)", found);
  }

  // ── Overridden methods ────────────────────────────────────────────

  @Test
  public void overridesCreateJPanel() {
    try {
      Method m = clazz.getDeclaredMethod("createJPanel");
      assertNotNull("createJPanel must be declared", m);
    } catch (NoSuchMethodException e) {
      fail("must override createJPanel()");
    }
  }

  @Test
  public void overridesSetNorthWestComponent() {
    try {
      Class<?> awtComponentView = Class.forName("org.lgna.croquet.views.AwtComponentView");
      Method m = clazz.getDeclaredMethod("setNorthWestComponent", awtComponentView);
      assertNotNull("setNorthWestComponent must be declared", m);
    } catch (Exception e) {
      fail("must override setNorthWestComponent(AwtComponentView): " + e.getMessage());
    }
  }
}
