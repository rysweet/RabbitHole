package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * TDD contract tests that verify StorytellingSceneEditor's post-extraction state.
 * After extracting SceneEditorDropReceptor, LookingGlassPanel, and
 * SceneEditorListeners, the outer class structure must change:
 *   - Inner class count: 4 → 2 (SingletonHolder + SceneEditorProgramImp remain)
 *   - 7 listener fields removed, replaced by single 'listeners' field
 *   - dropReceptor and lookingGlassPanel field types become top-level classes
 *
 * Pure reflection — no GUI, no singleton instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class StorytellingSceneEditorExtractionTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.StorytellingSceneEditor";
  private static Class<?> clazz;

  @BeforeClass
  public static void loadClass() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor class not found: " + e.getMessage());
    }
  }

  // ── Inner class count ─────────────────────────────────────────────

  @Test
  public void innerClassCount_exactlyTwo() {
    assertEquals("After extraction, must have exactly 2 inner classes "
            + "(SingletonHolder + SceneEditorProgramImp)",
        2, clazz.getDeclaredClasses().length);
  }

  @Test
  public void remainingInnerClasses_areSingletonHolderAndSceneEditorProgramImp() {
    Set<String> names = Arrays.stream(clazz.getDeclaredClasses())
        .map(Class::getSimpleName)
        .collect(Collectors.toSet());
    assertTrue("SingletonHolder must remain as inner class",
        names.contains("SingletonHolder"));
    assertTrue("SceneEditorProgramImp must remain as inner class",
        names.contains("SceneEditorProgramImp"));
  }

  @Test
  public void noInnerClass_SceneEditorDropReceptor() {
    Set<String> names = Arrays.stream(clazz.getDeclaredClasses())
        .map(Class::getSimpleName)
        .collect(Collectors.toSet());
    assertFalse("SceneEditorDropReceptor must be extracted (no longer inner class)",
        names.contains("SceneEditorDropReceptor"));
  }

  @Test
  public void noInnerClass_LookingGlassPanel() {
    Set<String> names = Arrays.stream(clazz.getDeclaredClasses())
        .map(Class::getSimpleName)
        .collect(Collectors.toSet());
    assertFalse("LookingGlassPanel must be extracted (no longer inner class)",
        names.contains("LookingGlassPanel"));
  }

  // ── New 'listeners' field ─────────────────────────────────────────

  @Test
  public void hasListenersField() {
    try {
      clazz.getDeclaredField("listeners");
    } catch (NoSuchFieldException e) {
      fail("Must have a 'listeners' field for SceneEditorListeners");
    }
  }

  @Test
  public void listenersFieldType_isSceneEditorListeners() {
    try {
      Field f = clazz.getDeclaredField("listeners");
      assertEquals("listeners field must be of type SceneEditorListeners",
          "org.alice.stageide.sceneeditor.SceneEditorListeners",
          f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("Missing 'listeners' field");
    }
  }

  // ── Extracted field types are top-level classes ────────────────────

  @Test
  public void dropReceptorFieldType_isTopLevel() {
    try {
      Field f = clazz.getDeclaredField("dropReceptor");
      assertNull("dropReceptor type must be a top-level class (no enclosing class)",
          f.getType().getEnclosingClass());
      assertEquals("org.alice.stageide.sceneeditor.SceneEditorDropReceptor",
          f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("Missing 'dropReceptor' field");
    }
  }

  @Test
  public void lookingGlassPanelFieldType_isTopLevel() {
    try {
      Field f = clazz.getDeclaredField("lookingGlassPanel");
      assertNull("lookingGlassPanel type must be a top-level class (no enclosing class)",
          f.getType().getEnclosingClass());
      assertEquals("org.alice.stageide.sceneeditor.LookingGlassPanel",
          f.getType().getName());
    } catch (NoSuchFieldException e) {
      fail("Missing 'lookingGlassPanel' field");
    }
  }

  // ── Listener fields removed from outer class ──────────────────────

  @Test
  public void listenerFieldsRemovedFromOuterClass() {
    String[] removedFields = {
        "showSnapGridListener",
        "snapEnabledListener",
        "snapGridSpacingListener",
        "cameraMarkerFieldSelectionListener",
        "objectMarkerFieldSelectionListener",
        "instanceFactorySelectionListener",
        "mainCameraViewSelectionObserver"
    };
    Set<String> declaredFieldNames = Arrays.stream(clazz.getDeclaredFields())
        .map(Field::getName)
        .collect(Collectors.toSet());
    for (String name : removedFields) {
      assertFalse("Listener field '" + name + "' must be moved to SceneEditorListeners",
          declaredFieldNames.contains(name));
    }
  }

  // ── Widened members for cross-file access ─────────────────────────

  @Test
  public void lookingGlassPanel_isPackagePrivate() {
    assertFieldIsPackagePrivate("lookingGlassPanel");
  }

  @Test
  public void globalDragAdapter_isPackagePrivate() {
    assertFieldIsPackagePrivate("globalDragAdapter");
  }

  @Test
  public void selectionIsFromInstanceSelector_isPackagePrivate() {
    assertFieldIsPackagePrivate("selectionIsFromInstanceSelector");
  }

  @Test
  public void handleCameraMarkerFieldSelection_isPackagePrivate() {
    assertMethodIsPackagePrivate("handleCameraMarkerFieldSelection");
  }

  @Test
  public void handleObjectMarkerFieldSelection_isPackagePrivate() {
    assertMethodIsPackagePrivate("handleObjectMarkerFieldSelection");
  }

  @Test
  public void handleMainCameraViewSelection_isPackagePrivate() {
    assertMethodIsPackagePrivate("handleMainCameraViewSelection");
  }

  @Test
  public void setSelectedInstance_isPackagePrivate() {
    assertMethodIsPackagePrivate("setSelectedInstance");
  }

  // ── Aggregate guardrail ───────────────────────────────────────────

  @Test
  public void declaredFieldCount_stillAtLeast20() {
    int count = clazz.getDeclaredFields().length;
    assertTrue("Expected ≥20 declared fields after extraction, found " + count,
        count >= 20);
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private static void assertFieldIsPackagePrivate(String name) {
    try {
      Field f = clazz.getDeclaredField(name);
      int mods = f.getModifiers();
      assertFalse(name + " must not be public", Modifier.isPublic(mods));
      assertFalse(name + " must not be private", Modifier.isPrivate(mods));
      assertFalse(name + " must not be protected", Modifier.isProtected(mods));
    } catch (NoSuchFieldException e) {
      fail("Missing field: " + name);
    }
  }

  private static void assertMethodIsPackagePrivate(String name) {
    boolean found = Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> m.getName().equals(name))
        .anyMatch(m -> {
          int mods = m.getModifiers();
          return !Modifier.isPublic(mods)
              && !Modifier.isPrivate(mods)
              && !Modifier.isProtected(mods);
        });
    assertTrue(name + " must be package-private", found);
  }
}
