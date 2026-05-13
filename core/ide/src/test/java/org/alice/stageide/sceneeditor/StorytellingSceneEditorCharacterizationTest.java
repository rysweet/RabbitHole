package org.alice.stageide.sceneeditor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Characterization tests for StorytellingSceneEditor (issue #528).
 *
 * Pure reflection — no GUI, no singleton instantiation.
 * Locks down the existing public API surface, inner class structure,
 * and key field declarations before any extraction or refactoring.
 *
 * Pattern: same as InnerClassExtractionContractTest in core/glrender.
 */
public class StorytellingSceneEditorCharacterizationTest {

  private static final String FQCN = "org.alice.stageide.sceneeditor.StorytellingSceneEditor";
  private static Class<?> clazz;
  private static final Map<String, Class<?>> classCache = new HashMap<>();
  private static Class<?>[] cachedInnerClasses;
  private static final Map<String, Class<?>> innerClassMap = new HashMap<>();

  @BeforeClass
  public static void loadClass() {
    try {
      clazz = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("StorytellingSceneEditor class not found: " + e.getMessage());
    }
    cachedInnerClasses = clazz.getDeclaredClasses();
    for (Class<?> c : cachedInnerClasses) {
      innerClassMap.put(c.getSimpleName(), c);
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private static Class<?> resolve(String fqcn) {
    return classCache.computeIfAbsent(fqcn, name -> {
      try {
        return Class.forName(name);
      } catch (ClassNotFoundException e) {
        fail("Could not resolve class: " + name);
        return null;
      }
    });
  }

  private static void assertPublicMethod(String name, Class<?>... paramTypes) {
    try {
      Method m = clazz.getDeclaredMethod(name, paramTypes);
      assertTrue(name + " must be public", Modifier.isPublic(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("Missing public method: " + name + "(" + Arrays.stream(paramTypes).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")");
    }
  }

  private static void assertDeclaredField(String name) {
    try {
      clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      fail("Missing declared field: " + name);
    }
  }

  private static Class<?>[] innerClasses() {
    return cachedInnerClasses;
  }

  private static Class<?> findInner(String simpleName) {
    return innerClassMap.get(simpleName);
  }

  // ── 1. Class hierarchy ────────────────────────────────────────────

  @Test
  public void extendsAbstractSceneEditor() {
    Class<?> superClass = clazz.getSuperclass();
    assertEquals("org.alice.ide.sceneeditor.AbstractSceneEditor", superClass.getName());
  }

  @Test
  public void implementsRenderTargetListener() {
    Set<String> ifaces = Arrays.stream(clazz.getInterfaces())
        .map(Class::getName)
        .collect(Collectors.toSet());
    assertTrue("must implement RenderTargetListener",
        ifaces.contains("edu.cmu.cs.dennisc.render.event.RenderTargetListener"));
  }

  // ── 2. Singleton pattern ──────────────────────────────────────────

  @Test
  public void hasPrivateConstructor() {
    Constructor<?>[] ctors = clazz.getDeclaredConstructors();
    boolean foundPrivate = false;
    for (Constructor<?> c : ctors) {
      if (c.getParameterCount() == 0 && Modifier.isPrivate(c.getModifiers())) {
        foundPrivate = true;
      }
    }
    assertTrue("must have a private no-arg constructor", foundPrivate);
  }

  @Test
  public void hasGetInstanceMethod() {
    assertPublicMethod("getInstance");
  }

  @Test
  public void getInstanceReturnsOwnType() {
    try {
      Method m = clazz.getDeclaredMethod("getInstance");
      assertTrue("getInstance must be static", Modifier.isStatic(m.getModifiers()));
      assertEquals("getInstance must return StorytellingSceneEditor", clazz, m.getReturnType());
    } catch (NoSuchMethodException e) {
      fail("getInstance method not found");
    }
  }

  // ── 3. Inner classes ──────────────────────────────────────────────

  @Test
  public void innerClass_SingletonHolder_exists() {
    assertNotNull("SingletonHolder must exist", findInner("SingletonHolder"));
  }

  @Test
  public void innerClass_SingletonHolder_isPrivateStatic() {
    Class<?> c = findInner("SingletonHolder");
    assertNotNull("SingletonHolder must exist", c);
    int mods = c.getModifiers();
    assertTrue("SingletonHolder must be private", Modifier.isPrivate(mods));
    assertTrue("SingletonHolder must be static", Modifier.isStatic(mods));
  }

  @Test
  public void innerClass_SceneEditorDropReceptor_extractedToTopLevel() {
    assertNull("SceneEditorDropReceptor must be extracted (no longer inner class)", findInner("SceneEditorDropReceptor"));
    assertNotNull("SceneEditorDropReceptor must exist as top-level class", resolve("org.alice.stageide.sceneeditor.SceneEditorDropReceptor"));
  }

  @Test
  public void innerClass_SceneEditorDropReceptor_isPackagePrivateTopLevel() {
    Class<?> c = resolve("org.alice.stageide.sceneeditor.SceneEditorDropReceptor");
    assertNotNull(c);
    int mods = c.getModifiers();
    assertFalse("SceneEditorDropReceptor must not be public", Modifier.isPublic(mods));
    assertFalse("SceneEditorDropReceptor must not be private", Modifier.isPrivate(mods));
    assertFalse("SceneEditorDropReceptor must not be protected", Modifier.isProtected(mods));
    assertNull("SceneEditorDropReceptor must be top-level (no enclosing class)", c.getEnclosingClass());
  }

  @Test
  public void innerClass_LookingGlassPanel_extractedToTopLevel() {
    assertNull("LookingGlassPanel must be extracted (no longer inner class)", findInner("LookingGlassPanel"));
    assertNotNull("LookingGlassPanel must exist as top-level class", resolve("org.alice.stageide.sceneeditor.LookingGlassPanel"));
  }

  @Test
  public void innerClass_LookingGlassPanel_isPackagePrivateTopLevel() {
    Class<?> c = resolve("org.alice.stageide.sceneeditor.LookingGlassPanel");
    assertNotNull(c);
    int mods = c.getModifiers();
    assertFalse("LookingGlassPanel must not be public", Modifier.isPublic(mods));
    assertFalse("LookingGlassPanel must not be private", Modifier.isPrivate(mods));
    assertFalse("LookingGlassPanel must not be protected", Modifier.isProtected(mods));
    assertNull("LookingGlassPanel must be top-level (no enclosing class)", c.getEnclosingClass());
  }

  @Test
  public void innerClass_SceneEditorProgramImp_exists() {
    assertNotNull("SceneEditorProgramImp must exist", findInner("SceneEditorProgramImp"));
  }

  @Test
  public void innerClass_SceneEditorProgramImp_isPublicStatic() {
    Class<?> c = findInner("SceneEditorProgramImp");
    assertNotNull(c);
    int mods = c.getModifiers();
    assertTrue("SceneEditorProgramImp must be public", Modifier.isPublic(mods));
    assertTrue("SceneEditorProgramImp must be static", Modifier.isStatic(mods));
  }

  @Test
  public void innerClassCount_exactly2() {
    assertEquals("must have exactly 2 inner classes (SingletonHolder + SceneEditorProgramImp)", 2, innerClasses().length);
  }

  // ── 4. SceneEditorProgramImp key override ─────────────────────────

  @Test
  public void sceneEditorProgramImp_hasGetAnimator() {
    Class<?> c = findInner("SceneEditorProgramImp");
    assertNotNull("SceneEditorProgramImp must exist", c);
    try {
      Method m = c.getDeclaredMethod("getAnimator");
      assertTrue("getAnimator must be public", Modifier.isPublic(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("SceneEditorProgramImp must have getAnimator()");
    }
  }

  // ── 5. Outer-class public API surface ─────────────────────────────

  @Test
  public void api_getDropReceptor() {
    assertPublicMethod("getDropReceptor");
  }

  @Test
  public void api_isStartingCameraView() {
    assertPublicMethod("isStartingCameraView");
  }

  @Test
  public void api_setStartingCameraMarkerTransformation() {
    assertPublicMethod("setStartingCameraMarkerTransformation",
        resolve("org.alice.math.immutable.AffineMatrix4x4"));
  }

  @Test
  public void api_setSelectedExpression() {
    assertPublicMethod("setSelectedExpression",
        resolve("org.lgna.project.ast.Expression"));
  }

  @Test
  public void api_centerCameraOnSelectedField() {
    assertPublicMethod("centerCameraOnSelectedField",
        resolve("org.lgna.croquet.history.UserActivity"));
  }

  @Test
  public void api_setSelectedField() {
    assertPublicMethod("setSelectedField",
        resolve("org.lgna.project.ast.UserType"),
        resolve("org.lgna.project.ast.UserField"));
  }

  @Test
  public void api_isVrActive() {
    assertPublicMethod("isVrActive");
  }

  @Test
  public void api_setSelectedObjectMarker() {
    assertPublicMethod("setSelectedObjectMarker",
        resolve("org.lgna.project.ast.UserField"));
  }

  @Test
  public void api_switchToOrthographicCamera() {
    assertPublicMethod("switchToOrthographicCamera");
  }

  @Test
  public void api_switchToPerspectiveCamera() {
    assertPublicMethod("switchToPerspectiveCamera",
        resolve("edu.cmu.cs.dennisc.scenegraph.AbstractCamera"));
  }

  @Test
  public void api_addField() {
    assertPublicMethod("addField",
        resolve("org.lgna.project.ast.UserType"),
        resolve("org.lgna.project.ast.UserField"),
        int.class,
        resolve("[Lorg.lgna.project.ast.Statement;"));
  }

  @Test
  public void api_enableRendering() {
    assertPublicMethod("enableRendering",
        resolve("org.alice.ide.ReasonToDisableSomeAmountOfRendering"));
  }

  @Test
  public void api_disableRendering() {
    assertPublicMethod("disableRendering",
        resolve("org.alice.ide.ReasonToDisableSomeAmountOfRendering"));
  }

  @Test
  public void api_preScreenCapture() {
    assertPublicMethod("preScreenCapture");
  }

  @Test
  public void api_postScreenCapture() {
    assertPublicMethod("postScreenCapture");
  }

  @Test
  public void api_setFieldToState() {
    assertPublicMethod("setFieldToState",
        resolve("org.lgna.project.ast.UserField"),
        resolve("[Lorg.lgna.project.ast.Statement;"));
  }

  @Test
  public void api_getCurrentStateCodeForField() {
    assertPublicMethod("getCurrentStateCodeForField",
        resolve("org.lgna.project.ast.UserField"));
  }

  @Test
  public void api_generateCodeForSetUp() {
    assertPublicMethod("generateCodeForSetUp",
        resolve("org.lgna.project.ast.StatementListProperty"));
  }

  @Test
  public void api_getDoStatementsForCopyField() {
    assertPublicMethod("getDoStatementsForCopyField",
        resolve("org.lgna.project.ast.UserField"),
        resolve("org.lgna.project.ast.UserField"),
        resolve("org.alice.math.immutable.AffineMatrix4x4"));
  }

  @Test
  public void api_getDoStatementsForAddField() {
    assertPublicMethod("getDoStatementsForAddField",
        resolve("org.lgna.project.ast.UserField"),
        resolve("org.alice.math.immutable.AffineMatrix4x4"));
  }

  @Test
  public void api_getUndoStatementsForAddField() {
    assertPublicMethod("getUndoStatementsForAddField",
        resolve("org.lgna.project.ast.UserField"));
  }

  @Test
  public void api_getRiders() {
    assertPublicMethod("getRiders",
        resolve("org.lgna.project.ast.UserField"));
  }

  @Test
  public void api_getDoStatementsForRemoveField() {
    assertPublicMethod("getDoStatementsForRemoveField",
        resolve("org.lgna.project.ast.UserField"),
        java.util.Map.class);
  }

  @Test
  public void api_getUndoStatementsForRemoveField() {
    assertPublicMethod("getUndoStatementsForRemoveField",
        resolve("org.lgna.project.ast.UserField"),
        java.util.Map.class);
  }

  @Test
  public void api_handleShowing() {
    assertPublicMethod("handleShowing");
  }

  @Test
  public void api_handleHiding() {
    assertPublicMethod("handleHiding");
  }

  @Test
  public void api_setHandleVisibilityForObject() {
    assertPublicMethod("setHandleVisibilityForObject",
        resolve("org.lgna.story.implementation.TransformableImp"),
        boolean.class);
  }

  @Test
  public void api_getTransformForNewCameraMarker() {
    assertPublicMethod("getTransformForNewCameraMarker");
  }

  @Test
  public void api_getTransformForNewObjectMarker() {
    assertPublicMethod("getTransformForNewObjectMarker");
  }

  @Test
  public void api_getColorForNewObjectMarker() {
    assertPublicMethod("getColorForNewObjectMarker");
  }

  @Test
  public void api_getColorForNewCameraMarker() {
    assertPublicMethod("getColorForNewCameraMarker");
  }

  @Test
  public void api_getGoodPointOfViewInSceneForObject() {
    assertPublicMethod("getGoodPointOfViewInSceneForObject",
        resolve("org.alice.math.immutable.AxisAlignedBox"));
  }

  @Test
  public void api_getMarkerForField() {
    assertPublicMethod("getMarkerForField",
        resolve("org.lgna.project.ast.UserField"));
  }

  @Test
  public void api_getSgCameraForCreatingThumbnails() {
    assertPublicMethod("getSgCameraForCreatingThumbnails");
  }

  @Test
  public void api_setShowSnapGrid() {
    assertPublicMethod("setShowSnapGrid", boolean.class);
  }

  @Test
  public void api_setSnapGridSpacing() {
    assertPublicMethod("setSnapGridSpacing", double.class);
  }

  @Test
  public void api_getOnscreenRenderTarget() {
    assertPublicMethod("getOnscreenRenderTarget");
  }

  // ── 6. RenderTargetListener overrides ─────────────────────────────

  @Test
  public void renderTargetListener_initialized() {
    assertPublicMethod("initialized",
        resolve("edu.cmu.cs.dennisc.render.event.RenderTargetInitializeEvent"));
  }

  @Test
  public void renderTargetListener_cleared() {
    assertPublicMethod("cleared",
        resolve("edu.cmu.cs.dennisc.render.event.RenderTargetRenderEvent"));
  }

  @Test
  public void renderTargetListener_rendered() {
    assertPublicMethod("rendered",
        resolve("edu.cmu.cs.dennisc.render.event.RenderTargetRenderEvent"));
  }

  @Test
  public void renderTargetListener_resized() {
    assertPublicMethod("resized",
        resolve("edu.cmu.cs.dennisc.render.event.RenderTargetResizeEvent"));
  }

  @Test
  public void renderTargetListener_displayChanged() {
    assertPublicMethod("displayChanged",
        resolve("edu.cmu.cs.dennisc.render.event.RenderTargetDisplayChangeEvent"));
  }

  // ── 7. Key field declarations ─────────────────────────────────────

  @Test
  public void field_isVrScene() {
    assertDeclaredField("isVrScene");
  }

  @Test
  public void field_dropReceptor() {
    assertDeclaredField("dropReceptor");
  }

  @Test
  public void field_automaticDisplayListener() {
    assertDeclaredField("automaticDisplayListener");
  }

  @Test
  public void field_onscreenRenderTarget() {
    assertDeclaredField("onscreenRenderTarget");
  }

  @Test
  public void field_animator() {
    assertDeclaredField("animator");
  }

  @Test
  public void field_lookingGlassPanel() {
    assertDeclaredField("lookingGlassPanel");
  }

  @Test
  public void field_globalDragAdapter() {
    assertDeclaredField("globalDragAdapter");
  }

  @Test
  public void field_movableSceneCameraImp() {
    assertDeclaredField("movableSceneCameraImp");
  }

  @Test
  public void field_sceneCameraImp() {
    assertDeclaredField("sceneCameraImp");
  }

  @Test
  public void field_mainCameraNavigatorWidget() {
    assertDeclaredField("mainCameraNavigatorWidget");
  }

  @Test
  public void field_expandButton() {
    assertDeclaredField("expandButton");
  }

  @Test
  public void field_contractButton() {
    assertDeclaredField("contractButton");
  }

  @Test
  public void field_runButton() {
    assertDeclaredField("runButton");
  }

  @Test
  public void field_orthographicCameraImp() {
    assertDeclaredField("orthographicCameraImp");
  }

  @Test
  public void field_layoutCameraImp() {
    assertDeclaredField("layoutCameraImp");
  }

  @Test
  public void field_snapGrid() {
    assertDeclaredField("snapGrid");
  }

  @Test
  public void field_isInitialized() {
    assertDeclaredField("isInitialized");
  }

  @Test
  public void field_selectionIsFromInstanceSelector() {
    assertDeclaredField("selectionIsFromInstanceSelector");
  }

  @Test
  public void field_selectionIsFromMain() {
    assertDeclaredField("selectionIsFromMain");
  }

  @Test
  public void field_mainCameraMarkerList() {
    assertDeclaredField("mainCameraMarkerList");
  }

  @Test
  public void field_savedSceneEditorViewSelection() {
    assertDeclaredField("savedSceneEditorViewSelection");
  }

  // ── 8. Aggregate stability guardrails ─────────────────────────────

  @Test
  public void publicMethodCount_atLeast35() {
    long count = Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()))
        .count();
    assertTrue("Expected ≥35 public methods on outer class, found " + count, count >= 35);
  }

  @Test
  public void declaredFieldCount_atLeast20() {
    int count = clazz.getDeclaredFields().length;
    assertTrue("Expected ≥20 declared fields, found " + count, count >= 20);
  }
}
