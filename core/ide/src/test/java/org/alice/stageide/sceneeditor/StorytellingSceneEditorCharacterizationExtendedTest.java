package org.alice.stageide.sceneeditor;

import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import org.alice.interact.manipulator.scenegraph.SnapGrid;
import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.story.SBiped;
import org.lgna.story.SCamera;
import org.lgna.story.SCameraMarker;
import org.lgna.story.SThingMarker;
import org.lgna.story.SVRHand;
import org.lgna.story.SVRHeadset;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class StorytellingSceneEditorCharacterizationExtendedTest {
  @Test
  public void isSelectableTypeRejectsMarkersAndVrDevices() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    Method method = StorytellingSceneEditor.class.getDeclaredMethod("isSelectableType", org.lgna.project.ast.AbstractType.class);
    method.setAccessible(true);

    assertEquals(Boolean.FALSE, method.invoke(editor, JavaType.getInstance(SThingMarker.class)));
    assertEquals(Boolean.FALSE, method.invoke(editor, JavaType.getInstance(SCameraMarker.class)));
    assertEquals(Boolean.FALSE, method.invoke(editor, JavaType.getInstance(SVRHand.class)));
    assertEquals(Boolean.FALSE, method.invoke(editor, JavaType.getInstance(SVRHeadset.class)));
  }

  @Test
  public void isSelectableTypeAllowsRegularStoryTypes() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    Method method = StorytellingSceneEditor.class.getDeclaredMethod("isSelectableType", org.lgna.project.ast.AbstractType.class);
    method.setAccessible(true);

    assertEquals(Boolean.TRUE, method.invoke(editor, JavaType.getInstance(SBiped.class)));
    assertEquals(Boolean.TRUE, method.invoke(editor, JavaType.getInstance(SCamera.class)));
  }

  @Test
  public void setShowSnapGridAndSpacingUpdateSnapGridWhenPresent() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    editor.snapGrid = new SnapGrid();

    editor.setShowSnapGrid(true);
    editor.setSnapGridSpacing(3.5);

    assertTrue(editor.snapGrid.getShowing());
    assertEquals(3.5, getGridSpacing(editor.snapGrid), 0.0);
  }

  @Test
  public void getOnscreenRenderTargetReturnsAssignedField() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    OnscreenRenderTarget renderTarget = (OnscreenRenderTarget) Proxy.newProxyInstance(
        OnscreenRenderTarget.class.getClassLoader(),
        new Class<?>[]{OnscreenRenderTarget.class},
        (proxy, method, args) -> null);
    editor.onscreenRenderTarget = renderTarget;

    assertSame(renderTarget, editor.getOnscreenRenderTarget());
  }

  @Test
  public void getDropReceptorReturnsAssignedField() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    SceneEditorDropReceptor receptor = new SceneEditorDropReceptor(editor);
    setField(editor, StorytellingSceneEditor.class, "dropReceptor", receptor);

    assertSame(receptor, editor.getDropReceptor());
  }

  private static StorytellingSceneEditor newEditor() throws Exception {
    return (StorytellingSceneEditor) getUnsafe().allocateInstance(StorytellingSceneEditor.class);
  }

  private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static double getGridSpacing(SnapGrid snapGrid) throws Exception {
    Field field = SnapGrid.class.getDeclaredField("gridSpacing");
    field.setAccessible(true);
    return field.getDouble(snapGrid);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }
}
