package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Import-cleanup contract tests for issue #528.
 *
 * After extracting SceneEditorDropReceptor, LookingGlassPanel, and
 * SceneEditorListeners from StorytellingSceneEditor, the 8 imports that
 * were only used by the extracted inner classes must be:
 *   1. ABSENT from StorytellingSceneEditor.java
 *   2. PRESENT in the extracted file(s) that need them
 *
 * These tests read source files directly — they verify import hygiene
 * at the source level, complementing the reflection-based structural
 * tests in the sibling test classes.
 *
 * TDD: these tests FAIL if the unused imports are left in place.
 */
public class ImportCleanupContractTest {

  private static final String SRC_ROOT =
      "core/ide/src/main/java/org/alice/stageide/sceneeditor/";

  private static Set<String> editorImports;
  private static Set<String> dropReceptorImports;
  private static Set<String> lookingGlassPanelImports;
  private static Set<String> listenersImports;

  @BeforeClass
  public static void loadImports() throws IOException {
    editorImports = readImports(SRC_ROOT + "StorytellingSceneEditor.java");
    dropReceptorImports = readImports(SRC_ROOT + "SceneEditorDropReceptor.java");
    lookingGlassPanelImports = readImports(SRC_ROOT + "LookingGlassPanel.java");
    listenersImports = readImports(SRC_ROOT + "SceneEditorListeners.java");
  }

  // ── 1. Removed imports must NOT be in StorytellingSceneEditor ─────

  @Test
  public void editor_noImport_GalleryDragModel() {
    assertImportAbsent(editorImports, "GalleryDragModel",
        "moved to SceneEditorDropReceptor");
  }

  @Test
  public void editor_noImport_SceneDropSite() {
    assertImportAbsent(editorImports, "SceneDropSite",
        "moved to SceneEditorDropReceptor");
  }

  @Test
  public void editor_noImport_ValueEvent() {
    assertImportAbsent(editorImports, "ValueEvent",
        "no longer needed after listener extraction");
  }

  @Test
  public void editor_noImport_ValueListener() {
    assertImportAbsent(editorImports, "ValueListener",
        "moved to SceneEditorListeners");
  }

  @Test
  public void editor_noImport_DragStep() {
    assertImportAbsent(editorImports, "DragStep",
        "moved to SceneEditorDropReceptor");
  }

  @Test
  public void editor_noImport_JPanel() {
    assertImportAbsent(editorImports, "JPanel",
        "moved to LookingGlassPanel");
  }

  @Test
  public void editor_noImport_SpringLayout() {
    assertImportAbsent(editorImports, "SpringLayout",
        "moved to LookingGlassPanel");
  }

  @Test
  public void editor_noImport_Point() {
    assertImportAbsent(editorImports, "Point",
        "moved to SceneEditorDropReceptor");
  }

  // ── 2. Extracted files DO import the types they need ──────────────

  @Test
  public void dropReceptor_imports_GalleryDragModel() {
    assertImportPresent(dropReceptorImports, "GalleryDragModel",
        "SceneEditorDropReceptor uses GalleryDragModel in isPotentiallyAcceptingOf");
  }

  @Test
  public void dropReceptor_imports_SceneDropSite() {
    assertImportPresent(dropReceptorImports, "SceneDropSite",
        "SceneEditorDropReceptor uses SceneDropSite in dragUpdated");
  }

  @Test
  public void dropReceptor_imports_DragStep() {
    assertImportPresent(dropReceptorImports, "DragStep",
        "SceneEditorDropReceptor uses DragStep in all drag lifecycle methods");
  }

  @Test
  public void dropReceptor_imports_Point() {
    assertImportPresent(dropReceptorImports, "Point",
        "SceneEditorDropReceptor uses Point in isDropLocationOverLookingGlass");
  }

  @Test
  public void lookingGlassPanel_imports_JPanel() {
    assertImportPresent(lookingGlassPanelImports, "JPanel",
        "LookingGlassPanel uses JPanel in createJPanel");
  }

  @Test
  public void lookingGlassPanel_imports_SpringLayout() {
    assertImportPresent(lookingGlassPanelImports, "SpringLayout",
        "LookingGlassPanel uses SpringLayout in setNorthWestComponent");
  }

  @Test
  public void listeners_imports_ValueListener() {
    assertImportPresent(listenersImports, "ValueListener",
        "SceneEditorListeners declares 7 ValueListener fields");
  }

  // ── 3. Editor retains imports it still uses ───────────────────────

  @Test
  public void editor_retains_AbstractSceneEditor() {
    assertImportPresent(editorImports, "AbstractSceneEditor",
        "StorytellingSceneEditor extends AbstractSceneEditor");
  }

  @Test
  public void editor_retains_RenderTargetListener() {
    assertImportPresent(editorImports, "RenderTargetListener",
        "StorytellingSceneEditor implements RenderTargetListener via wildcard");
  }

  @Test
  public void editor_retains_OnscreenRenderTarget() {
    assertImportPresent(editorImports, "OnscreenRenderTarget",
        "StorytellingSceneEditor declares onscreenRenderTarget field");
  }

  @Test
  public void editor_retains_GlobalDragAdapter() {
    assertImportPresent(editorImports, "GlobalDragAdapter",
        "StorytellingSceneEditor declares globalDragAdapter field");
  }

  // ── 4. No stale wildcard imports hiding removed types ─────────────

  @Test
  public void editor_noWildcard_croquetEvent() {
    assertNoWildcardImport(editorImports, "org.lgna.croquet.event.*",
        "ValueEvent/ValueListener must not sneak in via wildcard");
  }

  @Test
  public void editor_noWildcard_croquetHistory() {
    assertNoWildcardImport(editorImports, "org.lgna.croquet.history.*",
        "DragStep must not sneak in via wildcard");
  }

  // ── 5. Source-level compilation contract ──────────────────────────

  @Test
  public void editorClass_loadsSuccessfully() {
    try {
      Class<?> c = Class.forName(
          "org.alice.stageide.sceneeditor.StorytellingSceneEditor");
      assertNotNull("Class must load without import errors", c);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor must compile and load: " + e.getMessage());
    }
  }

  @Test
  public void extractedClasses_allLoadSuccessfully() {
    String[] fqcns = {
        "org.alice.stageide.sceneeditor.SceneEditorDropReceptor",
        "org.alice.stageide.sceneeditor.LookingGlassPanel",
        "org.alice.stageide.sceneeditor.SceneEditorListeners"
    };
    for (String fqcn : fqcns) {
      try {
        Class<?> c = Class.forName(fqcn);
        assertNotNull(fqcn + " must load", c);
      } catch (ClassNotFoundException e) {
        fail(fqcn + " must compile and load: " + e.getMessage());
      }
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────

  /**
   * Reads a Java source file and extracts all import lines as a set.
   * Each entry is the raw import line text (e.g., "import javax.swing.JPanel;").
   */
  private static Set<String> readImports(String relativePath) throws IOException {
    Path path = resolveSourceFile(relativePath);
    List<String> lines = Files.readAllLines(path);
    return lines.stream()
        .map(String::trim)
        .filter(line -> line.startsWith("import "))
        .collect(Collectors.toSet());
  }

  /**
   * Resolves a relative source path from the project root,
   * trying CWD first, then walking up to find the project root.
   */
  private static Path resolveSourceFile(String relativePath) {
    Path cwd = Paths.get(System.getProperty("user.dir"));
    // Try from CWD (Maven runs from project root or module root)
    Path candidate = cwd.resolve(relativePath);
    if (Files.exists(candidate)) {
      return candidate;
    }
    // Walk up to find repository root (has a core/ directory)
    Path dir = cwd;
    while (dir != null) {
      candidate = dir.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    fail("Cannot find source file: " + relativePath + " from " + cwd);
    return null; // unreachable
  }

  private static void assertImportAbsent(Set<String> imports, String simpleTypeName, String reason) {
    boolean found = imports.stream().anyMatch(line ->
        line.contains("." + simpleTypeName + ";"));
    assertFalse("Import of " + simpleTypeName + " must be absent (" + reason + ")",
        found);
  }

  private static void assertImportPresent(Set<String> imports, String simpleTypeName, String reason) {
    // Check for explicit import or a wildcard import of the package
    boolean found = imports.stream().anyMatch(line ->
        line.contains("." + simpleTypeName + ";")
            || (line.endsWith(".*;") && couldContain(line, simpleTypeName)));
    assertTrue("Import of " + simpleTypeName + " must be present (" + reason + ")",
        found);
  }

  /**
   * Rough heuristic: a wildcard import "import foo.bar.*;" could contain any type.
   * For render.event.* we know it contains RenderTargetListener, etc.
   */
  private static boolean couldContain(String wildcardImport, String simpleTypeName) {
    // Known wildcard→type mappings for this codebase
    if (wildcardImport.contains("render.event.*")
        && Set.of("RenderTargetListener", "RenderTargetInitializeEvent",
            "RenderTargetRenderEvent", "RenderTargetResizeEvent",
            "RenderTargetDisplayChangeEvent", "AutomaticDisplayListener",
            "AutomaticDisplayEvent").contains(simpleTypeName)) {
      return true;
    }
    if (wildcardImport.contains("scenegraph.*")
        && Set.of("SymmetricPerspectiveCamera", "AbstractCamera").contains(simpleTypeName)) {
      return true;
    }
    return false;
  }

  private static void assertNoWildcardImport(Set<String> imports, String wildcardImport, String reason) {
    assertFalse("Wildcard import '" + wildcardImport + "' must not be present (" + reason + ")",
        imports.contains("import " + wildcardImport));
  }
}
