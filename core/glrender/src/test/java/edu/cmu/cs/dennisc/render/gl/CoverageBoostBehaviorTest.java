package edu.cmu.cs.dennisc.render.gl;



import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.common.os.DynamicLibraryBundle;
import edu.cmu.cs.dennisc.render.RenderCapabilities;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.AdapterFactory;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.ChangeHandler;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import org.alice.math.immutable.Ray;
import org.alice.math.immutable.Vector4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sun.misc.Unsafe;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

public class CoverageBoostBehaviorTest {



  private static final Unsafe UNSAFE = lookupUnsafe();

  @Before
  public void setUp() throws Exception {
    AdapterFactory.forgetAllElements();
    setStaticField(ChangeHandler.class, "eventCount", 0);
  }

  @After
  public void tearDown() throws Exception {
    setStaticField(ChangeHandler.class, "eventCount", 0);
  }

  @Test
  public void animatorRunAndStartUpdateState() throws Exception {
    CountingAnimator yieldingAnimator = new CountingAnimator(Animator.ThreadDeferenceAction.YIELD, 1);
    yieldingAnimator.setSleepMillis(0);
    setField(Animator.class, yieldingAnimator, "isActive", true);
    yieldingAnimator.run();
    assertEquals(1, yieldingAnimator.getFrameCount());

    CountingAnimator sleepingAnimator = new CountingAnimator(Animator.ThreadDeferenceAction.SLEEP, 1);
    sleepingAnimator.setSleepMillis(0);
    setField(Animator.class, sleepingAnimator, "isActive", true);
    sleepingAnimator.run();
    assertEquals(1, sleepingAnimator.getFrameCount());

    CountingAnimator startedAnimator = new CountingAnimator(Animator.ThreadDeferenceAction.SLEEP, 1);
    startedAnimator.setSleepMillis(0);
    startedAnimator.start();
    waitUntil(() -> startedAnimator.getFrameCount() > 0, 2000);
    startedAnimator.stop();

    assertTrue(startedAnimator.getStartTimeMillis() > 0L);
    assertTrue(startedAnimator.getFrameCount() > 0);
  }

  @Test
  public void renderTargetPrivateMathAndRenderingFlagAreHeadlessSafe() throws Exception {
    DummyMinimalTarget target = allocate(DummyMinimalTarget.class);
    target.surfaceSize = new Dimension(800, 600);
    setField(GlrRenderTarget.class, target, "m_isRenderingEnabled", true);
    SymmetricPerspectiveCamera camera = TestRenderTargetSupport.perspectiveCamera();
    Rectangle viewport = new Rectangle(0, 0, 100, 80);

    Vector4 awtToViewport = (Vector4) invokeGlrRenderTarget(target, "transformFromAWTToViewport", new Class[]{Point.class, double.class, Rectangle.class}, new Point(10, 20), 5.0, viewport);
    assertEquals(10.0, awtToViewport.x(), 0.0);
    assertEquals(60.0, awtToViewport.y(), 0.0);
    assertEquals(5.0, awtToViewport.z(), 0.0);

    Point viewportToAwt = (Point) invokeGlrRenderTarget(target, "transformFromViewportToAWT", new Class[]{Vector4.class, Rectangle.class}, awtToViewport, viewport);
    assertEquals(new Point(10, 20), viewportToAwt);
    assertEquals(1.0, (Double) invokeGlrRenderTarget(target, "getNear", new Class[]{edu.cmu.cs.dennisc.scenegraph.AbstractCamera.class}, camera), 0.0);
    assertEquals(100.0, (Double) invokeGlrRenderTarget(target, "getFar", new Class[]{edu.cmu.cs.dennisc.scenegraph.AbstractCamera.class}, camera), 0.0);

    Vector4 projection = (Vector4) invokeGlrRenderTarget(target, "transformFromViewportToProjection", new Class[]{Vector4.class, edu.cmu.cs.dennisc.scenegraph.AbstractCamera.class, Rectangle.class}, awtToViewport, camera, viewport);
    Vector4 roundTrip = (Vector4) invokeGlrRenderTarget(target, "transformFromProjectionToViewport", new Class[]{Vector4.class, edu.cmu.cs.dennisc.scenegraph.AbstractCamera.class, Rectangle.class}, projection, camera, viewport);
    assertEquals(awtToViewport.x(), roundTrip.x(), 0.01);
    assertEquals(awtToViewport.y(), roundTrip.y(), 0.01);
    assertEquals(awtToViewport.z(), roundTrip.z(), 0.01);

    assertTrue(target.isRenderingEnabled());
    target.setRenderingEnabled(false);
    target.setRenderingEnabled(false);
    target.setRenderingEnabled(true);
    assertEquals(2, target.repaintCount);
  }

  @Test
  public void glDrawableUtilsFallsBackToStoredLinuxSizesAndSharesContext() throws Exception {
    GLOffscreenAutoDrawable offscreenDrawable = (GLOffscreenAutoDrawable) Proxy.newProxyInstance(
        GLOffscreenAutoDrawable.class.getClassLoader(),
        new Class[]{GLOffscreenAutoDrawable.class},
        (proxy, method, args) -> {
          if (method.getName().equals("getSurfaceWidth") || method.getName().equals("getSurfaceHeight")) {
            return 0;
          }
          return defaultValue(method.getReturnType());
        });

    Field linuxSizesField = GlDrawableUtils.class.getDeclaredField("linuxDrawableSizes");
    linuxSizesField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<GLOffscreenAutoDrawable, Dimension> linuxSizes = (Map<GLOffscreenAutoDrawable, Dimension>) linuxSizesField.get(null);
    if (linuxSizes != null) {
      linuxSizes.put(offscreenDrawable, new Dimension(7, 9));
      assertEquals(7, GlDrawableUtils.getGlDrawableWidth(offscreenDrawable));
      assertEquals(9, GlDrawableUtils.getGlDrawableHeight(offscreenDrawable));
    }

    GLDrawable plainDrawable = (GLDrawable) Proxy.newProxyInstance(
        GLDrawable.class.getClassLoader(),
        new Class[]{GLDrawable.class},
        (proxy, method, args) -> {
          if (method.getName().equals("getSurfaceWidth")) {
            return 5;
          } else if (method.getName().equals("getSurfaceHeight")) {
            return 6;
          }
          return defaultValue(method.getReturnType());
        });
    assertEquals(5, GlDrawableUtils.getGLJPanelWidth(plainDrawable));
    assertEquals(6, GlDrawableUtils.getGLJPanelHeight(plainDrawable));

    DummyMinimalTarget target = allocate(DummyMinimalTarget.class);
    GLContext context = new MinimalGLContext();
    target.drawable = (GLAutoDrawable) Proxy.newProxyInstance(
        GLAutoDrawable.class.getClassLoader(),
        new Class[]{GLAutoDrawable.class},
        (proxy, method, args) -> method.getName().equals("getContext") ? context : defaultValue(method.getReturnType()));
    assertSame(context, GlDrawableUtils.getGlContextToShare(target));
  }

  private static void invokeRenderTargetImp(Object imp, String name, Object arg) throws Exception {
    Method method = imp.getClass().getDeclaredMethod(name, arg.getClass());
    method.setAccessible(true);
    method.invoke(imp, arg);
  }

  private static void waitUntil(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
    GlRenderTestWait.until(condition, "GL render condition within " + timeoutMillis + "ms");
  }

  private static Object getField(Class<?> type, Object target, String name) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void setStaticField(Class<?> type, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }

  private static Unsafe lookupUnsafe() {
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return (Unsafe) field.get(null);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static <T> T allocate(Class<T> type) {
    try {
      return type.cast(UNSAFE.allocateInstance(type));
    } catch (InstantiationException e) {
      throw new AssertionError(e);
    }
  }

  private static Object defaultValue(Class<?> returnType) {
    if (returnType == Boolean.TYPE) {
      return false;
    } else if (returnType == Integer.TYPE) {
      return 0;
    } else if (returnType == Long.TYPE) {
      return 0L;
    } else if (returnType == Float.TYPE) {
      return 0f;
    } else if (returnType == Double.TYPE) {
      return 0d;
    }
    return null;
  }

  private static final class CountingAnimator extends Animator {
    private final ThreadDeferenceAction action;
    private final int stopAfter;
    private int steps;

    private CountingAnimator(ThreadDeferenceAction action, int stopAfter) {
      this.action = action;
      this.stopAfter = stopAfter;
    }

    @Override
    protected ThreadDeferenceAction step() {
      this.steps++;
      if (this.steps >= this.stopAfter) {
        stop();
      }
      return this.action;
    }
  }

  private static Object invokeGlrRenderTarget(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
    Method method = GlrRenderTarget.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  private static class DummyMinimalTarget extends GlrRenderTarget {
    private Dimension surfaceSize;
    private int repaintCount;
    private GLAutoDrawable drawable;

    private DummyMinimalTarget() {
      super(new RenderCapabilities.Builder().build());
    }

    @Override
    public java.awt.Dimension getSurfaceSize() {
      return this.surfaceSize;
    }

    @Override
    public void repaint() {
      this.repaintCount++;
    }

    @Override
    public GLAutoDrawable getGLAutoDrawable() {
      return this.drawable;
    }
  }


  private static final class MinimalGLContext extends GLContext {
    @Override
    public GLDrawable setGLDrawable(GLDrawable readWrite, boolean setWriteOnly) { return null; }
    @Override
    public GLDrawable getGLDrawable() { return null; }
    @Override
    public boolean isGLReadDrawableAvailable() { return false; }
    @Override
    public GLDrawable setGLReadDrawable(GLDrawable read) { return null; }
    @Override
    public GLDrawable getGLReadDrawable() { return null; }
    @Override
    public int makeCurrent() { return CONTEXT_CURRENT; }
    @Override
    public void release() { }
    @Override
    public void copy(GLContext source, int mask) { }
    @Override
    public void destroy() { }
    @Override
    public com.jogamp.opengl.GL getRootGL() { return null; }
    @Override
    public com.jogamp.opengl.GL getGL() { return null; }
    @Override
    public com.jogamp.opengl.GL setGL(com.jogamp.opengl.GL gl) { return gl; }
    @Override
    public boolean isFunctionAvailable(String glFunctionName) { return true; }
    @Override
    public boolean isExtensionAvailable(String glExtensionName) { return true; }
    @Override
    public int getPlatformExtensionCount() { return 0; }
    @Override
    public String getPlatformExtensionsString() { return ""; }
    @Override
    public int getGLExtensionCount() { return 0; }
    @Override
    public String getGLExtensionsString() { return ""; }
    @Override
    public int getContextCreationFlags() { return 0; }
    @Override
    public void setContextCreationFlags(int flags) { }
    @Override
    public int getDefaultVAO() { return 0; }
    @Override
    public int getBoundFramebuffer(int target) { return 0; }
    @Override
    public int getDefaultDrawFramebuffer() { return 0; }
    @Override
    public int getDefaultReadFramebuffer() { return 0; }
    @Override
    public int getDefaultDrawBuffer() { return 0; }
    @Override
    public int getDefaultReadBuffer() { return 0; }
    @Override
    public int getDefaultPixelDataType() { return 0; }
    @Override
    public int getDefaultPixelDataFormat() { return 0; }
    @Override
    public DynamicLibraryBundle getDynamicLibraryBundle() { return null; }
    @Override
    public String getGLDebugMessageExtension() { return null; }
    @Override
    public boolean isGLDebugSynchronous() { return false; }
    @Override
    public void setGLDebugSynchronous(boolean synchronous) { }
    @Override
    public boolean isGLDebugMessageEnabled() { return false; }
    @Override
    public void enableGLDebugMessage(boolean enable) { }
    @Override
    public void addGLDebugListener(com.jogamp.opengl.GLDebugListener listener) { }
    @Override
    public void removeGLDebugListener(com.jogamp.opengl.GLDebugListener listener) { }
    @Override
    public void glDebugMessageControl(int source, int type, int severity, int count, java.nio.IntBuffer ids, boolean enabled) { }
    @Override
    public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, int idsOffset, boolean enabled) { }
    @Override
    public void glDebugMessageInsert(int source, int type, int id, int severity, String buf) { }
  }
}
