package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Contract tests for SceneFieldCodeGenerator extraction (issue #528).
 * Verifies the delegate exists, has the expected API, and that
 * StorytellingSceneEditor delegates to it via a 'codeGenerator' field.
 *
 * Pure reflection — no GUI, no singleton instantiation.
 */
public class SceneFieldCodeGeneratorTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.SceneFieldCodeGenerator";
  private static final String SSE_FQCN = "org.alice.stageide.sceneeditor.StorytellingSceneEditor";
  private static Class<?> clazz;
  private static Class<?> sseClazz;

  @BeforeClass
  public static void loadClasses() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneFieldCodeGenerator class not found: " + e.getMessage());
    }
    try {
      sseClazz = Class.forName(SSE_FQCN);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor class not found: " + e.getMessage());
    }
  }

  // ── SceneFieldCodeGenerator is a top-level package-private class ──

  @Test
  public void isTopLevelClass() {
    assertNull("SceneFieldCodeGenerator must be top-level (no enclosing class)",
        clazz.getEnclosingClass());
  }

  @Test
  public void isPackagePrivate() {
    int mods = clazz.getModifiers();
    assertFalse("must not be public", Modifier.isPublic(mods));
    assertFalse("must not be private", Modifier.isPrivate(mods));
    assertFalse("must not be protected", Modifier.isProtected(mods));
  }

  // ── Constructor takes StorytellingSceneEditor ────────────────────

  @Test
  public void hasConstructorTakingEditor() {
    try {
      clazz.getDeclaredConstructor(sseClazz);
    } catch (NoSuchMethodException e) {
      fail("SceneFieldCodeGenerator must have a constructor taking StorytellingSceneEditor");
    }
  }

  // ── Key methods exist on the delegate ────────────────────────────

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

  // ── Static utility methods ───────────────────────────────────────

  @Test
  public void asSetVehicleCall_isStatic() {
    assertStaticMethod("asSetVehicleCall");
  }

  @Test
  public void isSetVehicleInvocation_isStatic() {
    assertStaticMethod("isSetVehicleInvocation");
  }

  // ── StorytellingSceneEditor has a 'codeGenerator' field ──────────

  @Test
  public void fieldManagerHasCodeGeneratorField() {
    try {
      Class<?> fmClazz = Class.forName("org.alice.stageide.sceneeditor.SceneEditorFieldManager");
      Field f = fmClazz.getDeclaredField("codeGenerator");
      assertEquals("codeGenerator must be of type SceneFieldCodeGenerator",
          FQCN, f.getType().getName());
    } catch (ClassNotFoundException e) {
      fail("SceneEditorFieldManager not found: " + e.getMessage());
    } catch (NoSuchFieldException e) {
      fail("SceneEditorFieldManager must have a 'codeGenerator' field");
    }
  }

  @Test
  public void codeGeneratorFieldIsFinal() {
    try {
      Class<?> fmClazz = Class.forName("org.alice.stageide.sceneeditor.SceneEditorFieldManager");
      Field f = fmClazz.getDeclaredField("codeGenerator");
      assertTrue("codeGenerator must be final", Modifier.isFinal(f.getModifiers()));
    } catch (ClassNotFoundException e) {
      fail("SceneEditorFieldManager not found: " + e.getMessage());
    } catch (NoSuchFieldException e) {
      fail("Missing 'codeGenerator' field");
    }
  }

  // ── Private helper methods moved out of SSE ──────────────────────

  @Test
  public void sseDoesNotHaveFillInAutomaticSetUpMethod() {
    assertSseDoesNotDeclare("fillInAutomaticSetUpMethod");
  }

  @Test
  public void sseDoesNotHaveReplaceReferencesInExpression() {
    assertSseDoesNotDeclare("replaceReferencesInExpression");
  }

  @Test
  public void sseDoesNotHaveDoesSetVehicleImplyVehicle() {
    assertSseDoesNotDeclare("doesSetVehicleImplyVehicle");
  }

  @Test
  public void sseDoesNotHaveIsDirectRider() {
    assertSseDoesNotDeclare("isDirectRider");
  }

  @Test
  public void sseDoesNotHaveIsJointRider() {
    assertSseDoesNotDeclare("isJointRider");
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static void assertHasMethod(String name) {
    boolean found = Arrays.stream(clazz.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertTrue("SceneFieldCodeGenerator must have method: " + name, found);
  }

  private static void assertStaticMethod(String name) {
    boolean found = Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> m.getName().equals(name))
        .anyMatch(m -> Modifier.isStatic(m.getModifiers()));
    assertTrue("SceneFieldCodeGenerator." + name + " must be static", found);
  }

  private static void assertSseDoesNotDeclare(String name) {
    Set<String> methods = Arrays.stream(sseClazz.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());
    assertFalse("StorytellingSceneEditor should not have '" + name + "' (moved to delegate)",
        methods.contains(name));
  }
}
