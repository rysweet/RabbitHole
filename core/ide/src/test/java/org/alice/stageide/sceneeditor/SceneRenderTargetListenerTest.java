package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * TDD contract tests for the SceneRenderTargetListener extraction (issue #528).
 *
 * SceneRenderTargetListener encapsulates the RenderTargetListener callbacks
 * and the paintHorizonLine helper, previously implemented directly on
 * StorytellingSceneEditor.
 *
 * Pure reflection — no GUI, no singleton instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class SceneRenderTargetListenerTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.SceneRenderTargetListener";
  private static final String SSE_FQCN = "org.alice.stageide.sceneeditor.StorytellingSceneEditor";
  private static final String RTL_IFACE = "edu.cmu.cs.dennisc.render.event.RenderTargetListener";
  private static Class<?> clazz;
  private static Class<?> sseClazz;

  @BeforeClass
  public static void loadClasses() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneRenderTargetListener must exist as a top-level class: " + e.getMessage());
    }
    try {
      sseClazz = Class.forName(SSE_FQCN);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor class not found: " + e.getMessage());
    }
  }

  // ── Class structure ──────────────────────────────────────────────

  @Test
  public void isTopLevelClass() {
    assertNull("SceneRenderTargetListener must be top-level (no enclosing class)",
        clazz.getEnclosingClass());
  }

  @Test
  public void isPackagePrivate() {
    int mods = clazz.getModifiers();
    assertFalse("must not be public", Modifier.isPublic(mods));
    assertFalse("must not be private", Modifier.isPrivate(mods));
    assertFalse("must not be protected", Modifier.isProtected(mods));
  }

  @Test
  public void implementsRenderTargetListener() {
    Set<String> ifaces = Arrays.stream(clazz.getInterfaces())
        .map(Class::getName)
        .collect(Collectors.toSet());
    assertTrue("must implement RenderTargetListener",
        ifaces.contains(RTL_IFACE));
  }

  // ── Constructor ──────────────────────────────────────────────────

  @Test
  public void constructorTakesStorytellingSceneEditor() {
    try {
      clazz.getDeclaredConstructor(sseClazz);
    } catch (NoSuchMethodException e) {
      fail("SceneRenderTargetListener must have constructor(StorytellingSceneEditor)");
    }
  }

  // ── Back-reference field ─────────────────────────────────────────

  @Test
  public void hasEditorField() {
    try {
      Field f = clazz.getDeclaredField("editor");
      assertEquals("editor field must be StorytellingSceneEditor",
          SSE_FQCN, f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("Missing 'editor' field");
    }
  }

  // ── RenderTargetListener callback methods ────────────────────────

  @Test
  public void hasInitialized() {
    assertPublicMethod("initialized");
  }

  @Test
  public void hasCleared() {
    assertPublicMethod("cleared");
  }

  @Test
  public void hasRendered() {
    assertPublicMethod("rendered");
  }

  @Test
  public void hasResized() {
    assertPublicMethod("resized");
  }

  @Test
  public void hasDisplayChanged() {
    assertPublicMethod("displayChanged");
  }

  // ── paintHorizonLine helper ──────────────────────────────────────

  @Test
  public void hasPaintHorizonLine() {
    boolean found = Arrays.stream(clazz.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals("paintHorizonLine"));
    assertTrue("SceneRenderTargetListener must have paintHorizonLine method", found);
  }

  @Test
  public void paintHorizonLineIsPrivate() {
    boolean foundPrivate = Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> m.getName().equals("paintHorizonLine"))
        .anyMatch(m -> Modifier.isPrivate(m.getModifiers()));
    assertTrue("paintHorizonLine must be private", foundPrivate);
  }

  // ── SSE no longer implements RenderTargetListener ─────────────────

  @Test
  public void sseDoesNotImplementRenderTargetListener() {
    Set<String> ifaces = Arrays.stream(sseClazz.getInterfaces())
        .map(Class::getName)
        .collect(Collectors.toSet());
    assertFalse("SSE must no longer directly implement RenderTargetListener",
        ifaces.contains(RTL_IFACE));
  }

  // ── SSE no longer declares RenderTargetListener methods ───────────

  @Test
  public void sseDoesNotDeclare_initialized() {
    assertSseDoesNotDeclare("initialized");
  }

  @Test
  public void sseDoesNotDeclare_cleared() {
    assertSseDoesNotDeclare("cleared");
  }

  @Test
  public void sseDoesNotDeclare_rendered() {
    assertSseDoesNotDeclare("rendered");
  }

  @Test
  public void sseDoesNotDeclare_resized() {
    assertSseDoesNotDeclare("resized");
  }

  @Test
  public void sseDoesNotDeclare_displayChanged() {
    assertSseDoesNotDeclare("displayChanged");
  }

  @Test
  public void sseDoesNotDeclare_paintHorizonLine() {
    assertSseDoesNotDeclare("paintHorizonLine");
  }

  // ── SSE has renderTargetListener field ───────────────────────────

  @Test
  public void sseHasRenderTargetListenerField() {
    try {
      Field f = sseClazz.getDeclaredField("renderTargetListener");
      assertEquals("renderTargetListener must be of type SceneRenderTargetListener",
          FQCN, f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("SSE must have a 'renderTargetListener' field");
    }
  }

  // ── Aggregate guardrail ──────────────────────────────────────────

  @Test
  public void methodCount_between6and8() {
    long count = Arrays.stream(clazz.getDeclaredMethods()).count();
    assertTrue("Expected 6-8 methods on SceneRenderTargetListener, found " + count,
        count >= 6 && count <= 8);
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static void assertPublicMethod(String name) {
    boolean found = Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> m.getName().equals(name))
        .anyMatch(m -> Modifier.isPublic(m.getModifiers()));
    assertTrue("SceneRenderTargetListener must have public method: " + name, found);
  }

  private static void assertSseDoesNotDeclare(String name) {
    Set<String> methods = Arrays.stream(sseClazz.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());
    assertFalse("SSE should not have '" + name + "' (moved to SceneRenderTargetListener)",
        methods.contains(name));
  }
}
