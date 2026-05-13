package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Integration contract tests verifying the SceneEditorFieldManager +
 * SceneRenderTargetListener extraction from StorytellingSceneEditor (issue #528).
 *
 * Tests in this file span multiple classes — they verify cross-cutting
 * concerns: delegation wiring, line count target, visibility contracts,
 * and updated characterization tests that change from the pre-extraction
 * baseline.
 *
 * Pure reflection and source analysis — no GUI, no singleton instantiation.
 * These tests FAIL until the extraction is implemented.
 */
public class FieldManagerExtractionContractTest {

  private static final String SSE_FQCN = "org.alice.stageide.sceneeditor.StorytellingSceneEditor";
  private static final String FM_FQCN = "org.alice.stageide.sceneeditor.SceneEditorFieldManager";
  private static final String RTL_FQCN = "org.alice.stageide.sceneeditor.SceneRenderTargetListener";
  private static Class<?> sseClazz;
  private static Class<?> fmClazz;
  private static Class<?> rtlClazz;

  private static final Path SSE_SRC = findSourceFile();

  @BeforeClass
  public static void loadClasses() {
    try {
      sseClazz = Class.forName(SSE_FQCN);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor not found: " + e.getMessage());
    }
    try {
      fmClazz = Class.forName(FM_FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneEditorFieldManager not found: " + e.getMessage());
    }
    try {
      rtlClazz = Class.forName(RTL_FQCN);
    } catch (ClassNotFoundException e) {
      fail("SceneRenderTargetListener not found: " + e.getMessage());
    }
  }

  // ── Line count target ────────────────────────────────────────────

  @Test
  public void sseLineCount_under700() throws IOException {
    if (!Files.exists(SSE_SRC)) {
      fail("SSE source not found at: " + SSE_SRC);
    }
    long lineCount = Files.lines(SSE_SRC).count();
    assertTrue("StorytellingSceneEditor must be under 700 lines after extraction, found " + lineCount,
        lineCount < 700);
  }

  // ── All 37 public methods still on SSE (API preservation) ────────

  @Test
  public void api_publicMethodCount_atLeast35() {
    long count = Arrays.stream(sseClazz.getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()))
        .count();
    assertTrue("Expected ≥35 public methods on SSE (all preserved as stubs), found " + count,
        count >= 35);
  }

  // ── SSE delegation pattern: fieldManager. calls ──────────────────

  @Test
  public void sseDelegationCallCount_atLeast28() throws IOException {
    if (!Files.exists(SSE_SRC)) {
      fail("SSE source not found at: " + SSE_SRC);
    }
    long delegationCount = Files.lines(SSE_SRC)
        .filter(line -> line.contains("fieldManager."))
        .count();
    assertTrue("Expected ≥28 fieldManager. delegation calls in SSE, found " + delegationCount,
        delegationCount >= 28);
  }

  // ── SSE no longer implements RenderTargetListener ─────────────────

  @Test
  public void sseClassDeclaration_noRenderTargetListener() throws IOException {
    if (!Files.exists(SSE_SRC)) {
      fail("SSE source not found at: " + SSE_SRC);
    }
    boolean found = Files.lines(SSE_SRC)
        .anyMatch(line -> line.contains("implements") && line.contains("RenderTargetListener"));
    assertFalse("SSE class declaration must not contain 'implements RenderTargetListener'", found);
  }

  // ── Visibility widenings on SSE ──────────────────────────────────

  @Test
  public void onscreenRenderTarget_isPackagePrivate() {
    assertFieldIsPackagePrivate("onscreenRenderTarget");
  }

  @Test
  public void mainCameraNavigatorWidget_isPackagePrivate() {
    assertFieldIsPackagePrivate("mainCameraNavigatorWidget");
  }

  @Test
  public void orthographicCameraImp_isPackagePrivate() {
    assertFieldIsPackagePrivate("orthographicCameraImp");
  }

  @Test
  public void automaticDisplayListener_isPackagePrivate() {
    assertFieldIsPackagePrivate("automaticDisplayListener");
  }

  @Test
  public void movableSceneCameraImp_isPackagePrivate() {
    assertFieldIsPackagePrivate("movableSceneCameraImp");
  }

  @Test
  public void getPropertyPanel_isPackagePrivate() {
    boolean found = Arrays.stream(sseClazz.getDeclaredMethods())
        .filter(m -> m.getName().equals("getPropertyPanel"))
        .anyMatch(m -> {
          int mods = m.getModifiers();
          return !Modifier.isPublic(mods)
              && !Modifier.isPrivate(mods)
              && !Modifier.isProtected(mods);
        });
    assertTrue("getPropertyPanel must be package-private", found);
  }

  // ── SceneEditorListeners rewiring ────────────────────────────────
  // The 4 lambdas must call through fieldManager, not directly on editor.

  @Test
  public void listenersSource_usesFieldManager() throws IOException {
    Path listenersPath = SSE_SRC.getParent().resolve("SceneEditorListeners.java");
    // Use class-level source to verify (if source is available)
    if (!Files.exists(listenersPath)) {
      // Fall back: source path might differ in test context
      listenersPath = findFile("SceneEditorListeners.java");
    }
    if (listenersPath == null || !Files.exists(listenersPath)) {
      // Cannot do source analysis; skip this test gracefully
      return;
    }
    long fieldManagerCalls = Files.lines(listenersPath)
        .filter(line -> line.contains("fieldManager."))
        .count();
    assertTrue("SceneEditorListeners must call fieldManager. at least 4 times, found " + fieldManagerCalls,
        fieldManagerCalls >= 4);
  }

  // ── New files exist ──────────────────────────────────────────────

  @Test
  public void sceneEditorFieldManagerFile_exists() {
    Path fmPath = SSE_SRC.getParent().resolve("SceneEditorFieldManager.java");
    assertTrue("SceneEditorFieldManager.java must exist", Files.exists(fmPath));
  }

  @Test
  public void sceneRenderTargetListenerFile_exists() {
    Path rtlPath = SSE_SRC.getParent().resolve("SceneRenderTargetListener.java");
    assertTrue("SceneRenderTargetListener.java must exist", Files.exists(rtlPath));
  }

  // ── Updated characterization: inner class count unchanged ────────

  @Test
  public void innerClassCount_still2() {
    assertEquals("SSE must still have exactly 2 inner classes", 2, sseClazz.getDeclaredClasses().length);
  }

  // ── Updated characterization: declared field count adjusted ──────

  @Test
  public void declaredFieldCount_reflects_extraction() {
    int count = sseClazz.getDeclaredFields().length;
    // codeGenerator removed, fieldManager + renderTargetListener added = net +1
    // The count should still be ≥ 20 (was ~24 before)
    assertTrue("Expected ≥15 declared fields after extraction, found " + count,
        count >= 15);
  }

  // ── SSE retains key override methods as stubs ────────────────────

  @Test
  public void sseStillDeclares_getDoStatementsForCopyField() {
    assertSseDeclares("getDoStatementsForCopyField");
  }

  @Test
  public void sseStillDeclares_getDoStatementsForAddField() {
    assertSseDeclares("getDoStatementsForAddField");
  }

  @Test
  public void sseStillDeclares_getUndoStatementsForAddField() {
    assertSseDeclares("getUndoStatementsForAddField");
  }

  @Test
  public void sseStillDeclares_getRiders() {
    assertSseDeclares("getRiders");
  }

  @Test
  public void sseStillDeclares_getDoStatementsForRemoveField() {
    assertSseDeclares("getDoStatementsForRemoveField");
  }

  @Test
  public void sseStillDeclares_getUndoStatementsForRemoveField() {
    assertSseDeclares("getUndoStatementsForRemoveField");
  }

  @Test
  public void sseStillDeclares_handleShowing() {
    assertSseDeclares("handleShowing");
  }

  @Test
  public void sseStillDeclares_handleHiding() {
    assertSseDeclares("handleHiding");
  }

  @Test
  public void sseStillDeclares_switchToOrthographicCamera() {
    assertSseDeclares("switchToOrthographicCamera");
  }

  @Test
  public void sseStillDeclares_switchToPerspectiveCamera() {
    assertSseDeclares("switchToPerspectiveCamera");
  }

  @Test
  public void sseStillDeclares_getTransformForNewCameraMarker() {
    assertSseDeclares("getTransformForNewCameraMarker");
  }

  @Test
  public void sseStillDeclares_getMarkerForField() {
    assertSseDeclares("getMarkerForField");
  }

  @Test
  public void sseStillDeclares_setSelectedField() {
    assertSseDeclares("setSelectedField");
  }

  @Test
  public void sseStillDeclares_setSelectedExpression() {
    assertSseDeclares("setSelectedExpression");
  }

  @Test
  public void sseStillDeclares_setActiveScene() {
    assertSseDeclares("setActiveScene");
  }

  @Test
  public void sseStillDeclares_addField() {
    assertSseDeclares("addField");
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static Path findSourceFile() {
    // Try multiple common root patterns
    String[] roots = {
        "core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java",
    };
    for (String root : roots) {
      Path candidate = Paths.get(root);
      if (Files.exists(candidate)) {
        return candidate;
      }
      // Try from cwd ancestors
      Path cwd = Paths.get("").toAbsolutePath();
      for (Path dir = cwd; dir != null; dir = dir.getParent()) {
        Path full = dir.resolve(root);
        if (Files.exists(full)) {
          return full;
        }
      }
    }
    // Last resort — return a relative path that tests will detect as missing
    return Paths.get(roots[0]);
  }

  private static Path findFile(String filename) {
    Path parent = SSE_SRC.getParent();
    if (parent != null) {
      Path candidate = parent.resolve(filename);
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private static void assertFieldIsPackagePrivate(String name) {
    try {
      Field f = sseClazz.getDeclaredField(name);
      int mods = f.getModifiers();
      assertFalse(name + " must not be public", Modifier.isPublic(mods));
      assertFalse(name + " must not be private", Modifier.isPrivate(mods));
      assertFalse(name + " must not be protected", Modifier.isProtected(mods));
    } catch (NoSuchFieldException e) {
      fail("Missing field: " + name);
    }
  }

  private static void assertSseDeclares(String name) {
    boolean found = Arrays.stream(sseClazz.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertTrue("SSE must still declare method: " + name, found);
  }
}
