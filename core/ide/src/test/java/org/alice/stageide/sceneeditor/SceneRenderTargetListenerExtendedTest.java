package org.alice.stageide.sceneeditor;

import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import edu.cmu.cs.dennisc.render.event.RenderTargetRenderEvent;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import edu.cmu.cs.dennisc.scenegraph.Transformable;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.FixedRectangle;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class SceneRenderTargetListenerExtendedTest {
  @Test
  public void renderedDoesNothingWhenThereAreNoCameras() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    OnscreenRenderTarget renderTarget = createRenderTarget(0, null, 200, 100);
    editor.onscreenRenderTarget = renderTarget;
    SceneRenderTargetListener listener = new SceneRenderTargetListener(editor);

    listener.rendered(new RenderTargetRenderEvent(renderTarget, null));
  }

  @Test
  public void paintHorizonLineDrawsBlackLineWhenVisible() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    OnscreenRenderTarget renderTarget = createRenderTarget(1, new OrthographicCamera(), 200, 100);
    editor.onscreenRenderTarget = renderTarget;
    SceneRenderTargetListener listener = new SceneRenderTargetListener(editor);
    OrthographicCamera camera = new OrthographicCamera();
    BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);

    invokePaintHorizonLine(listener, image, renderTarget, camera);

    assertEquals(Color.BLACK.getRGB(), image.getRGB(10, 50));
  }

  @Test
  public void paintHorizonLineSkipsWhenComputedLineIsOffscreen() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    OnscreenRenderTarget renderTarget = createRenderTarget(1, new OrthographicCamera(), 200, 100);
    editor.onscreenRenderTarget = renderTarget;
    SceneRenderTargetListener listener = new SceneRenderTargetListener(editor);
    OrthographicCamera camera = new OrthographicCamera();
    Transformable parent = new Transformable();
    parent.localTransformation.setValue(AffineMatrix4x4.createTranslation(0, 10, 0));
    camera.setParent(parent);
    BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);

    invokePaintHorizonLine(listener, image, renderTarget, camera);

    assertNotEquals(Color.BLACK.getRGB(), image.getRGB(10, 50));
  }

  @Test
  public void emptyCallbacksRemainNoOps() throws Exception {
    SceneRenderTargetListener listener = new SceneRenderTargetListener(newEditor());

    listener.initialized(null);
    listener.cleared(null);
    listener.resized(null);
    listener.displayChanged(null);
  }

  @Test
  public void renderedIgnoresPerspectiveCamera() throws Exception {
    StorytellingSceneEditor editor = newEditor();
    OnscreenRenderTarget renderTarget = createRenderTarget(1, new SymmetricPerspectiveCamera(), 200, 100);
    editor.onscreenRenderTarget = renderTarget;
    SceneRenderTargetListener listener = new SceneRenderTargetListener(editor);

    listener.rendered(new RenderTargetRenderEvent(renderTarget, null));
  }

  private static void invokePaintHorizonLine(SceneRenderTargetListener listener, BufferedImage image,
      OnscreenRenderTarget renderTarget, OrthographicCamera camera) throws Exception {
    Method method = SceneRenderTargetListener.class.getDeclaredMethod(
        "paintHorizonLine", java.awt.Graphics.class, OnscreenRenderTarget.class, OrthographicCamera.class);
    method.setAccessible(true);
    method.invoke(listener, image.getGraphics(), renderTarget, camera);
  }

  private static StorytellingSceneEditor newEditor() throws Exception {
    return (StorytellingSceneEditor) getUnsafe().allocateInstance(StorytellingSceneEditor.class);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }

  private static OnscreenRenderTarget createRenderTarget(int cameraCount, AbstractCamera camera, int width, int height) {
    return (OnscreenRenderTarget) Proxy.newProxyInstance(
        OnscreenRenderTarget.class.getClassLoader(),
        new Class<?>[]{OnscreenRenderTarget.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getSgCameraCount" -> cameraCount;
          case "getSgCameraAt" -> camera;
          case "getSurfaceHeight" -> height;
          case "getSurfaceWidth" -> width;
          case "getSurfaceSize" -> new Dimension(width, height);
          case "getActualViewport" -> new FixedRectangle(0, 0, width, height);
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == double.class) {
      return 0.0;
    }
    if (type == float.class) {
      return 0.0f;
    }
    if (type == long.class) {
      return 0L;
    }
    return null;
  }
}
