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
 * TDD contract tests for the SceneEditorFieldManager extraction (issue #528).
 *
 * SceneEditorFieldManager consolidates selection/manipulator wiring, camera
 * switching, marker handling, right-click menu, show/hide lifecycle, rendering
 * control, and code generation delegation — all extracted from
 * StorytellingSceneEditor.
 *
 * Pure reflection — no GUI, no singleton instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class SceneEditorFieldManagerTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.SceneEditorFieldManager";
  private static final String SSE_FQCN = "org.alice.stageide.sceneeditor.StorytellingSceneEditor";
  private static Class<?> clazz;
  private static Class<?> sseClazz;
  private static Set<String> methodNames;

  @BeforeClass
  public static void loadClasses() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneEditorFieldManager must exist as a top-level class: " + e.getMessage());
    }
    try {
      sseClazz = Class.forName(SSE_FQCN);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor class not found: " + e.getMessage());
    }
    methodNames = Arrays.stream(clazz.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());
  }

  // ── Class structure ──────────────────────────────────────────────

  @Test
  public void isTopLevelClass() {
    assertNull("SceneEditorFieldManager must be top-level (no enclosing class)",
        clazz.getEnclosingClass());
  }

  @Test
  public void isPackagePrivate() {
    int mods = clazz.getModifiers();
    assertFalse("must not be public", Modifier.isPublic(mods));
    assertFalse("must not be private", Modifier.isPrivate(mods));
    assertFalse("must not be protected", Modifier.isProtected(mods));
  }

  // ── Constructor ──────────────────────────────────────────────────

  @Test
  public void constructorTakesStorytellingSceneEditor() {
    try {
      clazz.getDeclaredConstructor(sseClazz);
    } catch (NoSuchMethodException e) {
      fail("SceneEditorFieldManager must have constructor(StorytellingSceneEditor)");
    }
  }

  // ── Back-reference field ─────────────────────────────────────────

  @Test
  public void hasEditorField() {
    assertHasField("editor");
  }

  @Test
  public void editorFieldType_isSSE() {
    try {
      Field f = clazz.getDeclaredField("editor");
      assertEquals("editor field must be StorytellingSceneEditor",
          SSE_FQCN, f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("Missing 'editor' field");
    }
  }

  // ── Owns SceneFieldCodeGenerator ─────────────────────────────────

  @Test
  public void hasCodeGeneratorField() {
    assertHasField("codeGenerator");
  }

  @Test
  public void codeGeneratorFieldType_isSceneFieldCodeGenerator() {
    try {
      Field f = clazz.getDeclaredField("codeGenerator");
      assertEquals("codeGenerator must be of type SceneFieldCodeGenerator",
          "org.alice.stageide.sceneeditor.SceneFieldCodeGenerator",
          f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("Missing 'codeGenerator' field");
    }
  }

  @Test
  public void codeGeneratorFieldIsFinal() {
    try {
      Field f = clazz.getDeclaredField("codeGenerator");
      assertTrue("codeGenerator must be final", Modifier.isFinal(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("Missing 'codeGenerator' field");
    }
  }

  // ── Selection and manipulator wiring methods ─────────────────────

  @Test
  public void hasSetSelectedFieldOnManipulator() {
    assertHasMethod("setSelectedFieldOnManipulator");
  }

  @Test
  public void hasSetSelectedExpressionOnManipulator() {
    assertHasMethod("setSelectedExpressionOnManipulator");
  }

  @Test
  public void hasHandleManipulatorSelection() {
    assertHasMethod("handleManipulatorSelection");
  }

  @Test
  public void hasSetSelectedInstance() {
    assertHasMethod("setSelectedInstance");
  }

  // ── Camera switching methods ─────────────────────────────────────

  @Test
  public void hasSwitchToCamera() {
    assertHasMethod("switchToCamera");
  }

  @Test
  public void hasSwitchToOrthographicCamera() {
    assertHasMethod("switchToOrthographicCamera");
  }

  @Test
  public void hasSwitchToPerspectiveCamera() {
    assertHasMethod("switchToPerspectiveCamera");
  }

  // ── Marker handling methods ──────────────────────────────────────

  @Test
  public void hasHandleCameraMarkerFieldSelection() {
    assertHasMethod("handleCameraMarkerFieldSelection");
  }

  @Test
  public void hasHandleObjectMarkerFieldSelection() {
    assertHasMethod("handleObjectMarkerFieldSelection");
  }

  @Test
  public void hasSetSelectedObjectMarker() {
    assertHasMethod("setSelectedObjectMarker");
  }

  @Test
  public void hasSetSelectedCameraMarker() {
    assertHasMethod("setSelectedCameraMarker");
  }

  @Test
  public void hasHandleMainCameraViewSelection() {
    assertHasMethod("handleMainCameraViewSelection");
  }

  // ── Right-click context menu ─────────────────────────────────────

  @Test
  public void hasShowRightClickMenuForModel() {
    assertHasMethod("showRightClickMenuForModel");
  }

  // ── Show/hide lifecycle ──────────────────────────────────────────

  @Test
  public void hasShowLookingGlassPanel() {
    assertHasMethod("showLookingGlassPanel");
  }

  @Test
  public void hasHideLookingGlassPanel() {
    assertHasMethod("hideLookingGlassPanel");
  }

  @Test
  public void hasHandleShowing() {
    assertHasMethod("handleShowing");
  }

  @Test
  public void hasHandleHiding() {
    assertHasMethod("handleHiding");
  }

  // ── Rendering control methods ────────────────────────────────────

  @Test
  public void hasEnableRendering() {
    assertHasMethod("enableRendering");
  }

  @Test
  public void hasDisableRendering() {
    assertHasMethod("disableRendering");
  }

  @Test
  public void hasPreScreenCapture() {
    assertHasMethod("preScreenCapture");
  }

  @Test
  public void hasPostScreenCapture() {
    assertHasMethod("postScreenCapture");
  }

  @Test
  public void hasSetHandleVisibilityForObject() {
    assertHasMethod("setHandleVisibilityForObject");
  }

  // ── Camera/marker accessor helpers ───────────────────────────────

  @Test
  public void hasGetTransformForNewCameraMarker() {
    assertHasMethod("getTransformForNewCameraMarker");
  }

  @Test
  public void hasGetTransformForNewObjectMarker() {
    assertHasMethod("getTransformForNewObjectMarker");
  }

  @Test
  public void hasGetColorForNewObjectMarker() {
    assertHasMethod("getColorForNewObjectMarker");
  }

  @Test
  public void hasGetColorForNewCameraMarker() {
    assertHasMethod("getColorForNewCameraMarker");
  }

  @Test
  public void hasGetGoodPointOfViewInSceneForObject() {
    assertHasMethod("getGoodPointOfViewInSceneForObject");
  }

  @Test
  public void hasGetMarkerForField() {
    assertHasMethod("getMarkerForField");
  }

  // ── Code generation delegation methods ───────────────────────────

  @Test
  public void hasGetCurrentStateCodeForField() {
    assertHasMethod("getCurrentStateCodeForField");
  }

  @Test
  public void hasGenerateCodeForSetUp() {
    assertHasMethod("generateCodeForSetUp");
  }

  @Test
  public void hasGetDoStatementsForCopyField() {
    assertHasMethod("getDoStatementsForCopyField");
  }

  @Test
  public void hasGetDoStatementsForAddField() {
    assertHasMethod("getDoStatementsForAddField");
  }

  @Test
  public void hasGetUndoStatementsForAddField() {
    assertHasMethod("getUndoStatementsForAddField");
  }

  @Test
  public void hasGetRiders() {
    assertHasMethod("getRiders");
  }

  @Test
  public void hasGetDoStatementsForRemoveField() {
    assertHasMethod("getDoStatementsForRemoveField");
  }

  @Test
  public void hasGetUndoStatementsForRemoveField() {
    assertHasMethod("getUndoStatementsForRemoveField");
  }

  // ── SSE no longer has codeGenerator field ────────────────────────

  @Test
  public void sseDoesNotHaveCodeGeneratorField() {
    Set<String> sseFields = Arrays.stream(sseClazz.getDeclaredFields())
        .map(Field::getName)
        .collect(Collectors.toSet());
    assertFalse("codeGenerator must move from SSE to SceneEditorFieldManager",
        sseFields.contains("codeGenerator"));
  }

  // ── SSE has fieldManager field ───────────────────────────────────

  @Test
  public void sseHasFieldManagerField() {
    try {
      Field f = sseClazz.getDeclaredField("fieldManager");
      assertEquals("fieldManager must be of type SceneEditorFieldManager",
          FQCN, f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("SSE must have a 'fieldManager' field");
    }
  }

  @Test
  public void sseFieldManagerFieldIsFinal() {
    try {
      Field f = sseClazz.getDeclaredField("fieldManager");
      assertTrue("fieldManager must be final", Modifier.isFinal(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("Missing 'fieldManager' field");
    }
  }

  // ── Methods that moved entirely — SSE no longer declares them ───

  @Test
  public void sseDoesNotDeclare_setSelectedFieldOnManipulator() {
    assertSseDoesNotDeclare("setSelectedFieldOnManipulator");
  }

  @Test
  public void sseDoesNotDeclare_setSelectedExpressionOnManipulator() {
    assertSseDoesNotDeclare("setSelectedExpressionOnManipulator");
  }

  @Test
  public void sseDoesNotDeclare_handleManipulatorSelection() {
    assertSseDoesNotDeclare("handleManipulatorSelection");
  }

  @Test
  public void sseDoesNotDeclare_showRightClickMenuForModel() {
    assertSseDoesNotDeclare("showRightClickMenuForModel");
  }

  @Test
  public void sseDoesNotDeclare_switchToCamera() {
    assertSseDoesNotDeclare("switchToCamera");
  }

  @Test
  public void sseDoesNotDeclare_setSelectedCameraMarker() {
    assertSseDoesNotDeclare("setSelectedCameraMarker");
  }

  @Test
  public void sseDoesNotDeclare_showLookingGlassPanel() {
    assertSseDoesNotDeclare("showLookingGlassPanel");
  }

  @Test
  public void sseDoesNotDeclare_hideLookingGlassPanel() {
    assertSseDoesNotDeclare("hideLookingGlassPanel");
  }

  // ── Aggregate guardrail ──────────────────────────────────────────

  @Test
  public void methodCount_atLeast30() {
    long count = Arrays.stream(clazz.getDeclaredMethods()).count();
    assertTrue("Expected ≥30 methods on SceneEditorFieldManager, found " + count,
        count >= 30);
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static void assertHasField(String name) {
    try {
      clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      fail("Missing field: " + name);
    }
  }

  private static void assertHasMethod(String name) {
    assertTrue("SceneEditorFieldManager must have method: " + name,
        methodNames.contains(name));
  }

  private static void assertSseDoesNotDeclare(String name) {
    Set<String> sseMethods = Arrays.stream(sseClazz.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());
    assertFalse("SSE should not have '" + name + "' (moved to SceneEditorFieldManager)",
        sseMethods.contains(name));
  }
}
