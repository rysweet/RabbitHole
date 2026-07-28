package edu.cmu.cs.dennisc.render.gl.imp;

import org.junit.Assume;
import java.awt.GraphicsEnvironment;



import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.common.os.DynamicLibraryBundle;
import edu.cmu.cs.dennisc.image.ImageGenerator;
import edu.cmu.cs.dennisc.texture.MipMapGenerationPolicy;
import edu.cmu.cs.dennisc.render.RenderTarget;
import edu.cmu.cs.dennisc.render.gl.ForgettableBinding;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrTexture;
import edu.cmu.cs.dennisc.render.gl.imp.testing.HeadlessRecordingGL2;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.texture.Texture;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class CoverageBoostBehaviorTest {

  @org.junit.Before
  public void skipIfHeadless() {
    Assume.assumeTrue("Requires display", !GraphicsEnvironment.isHeadless());
  }



  @Test
  public void graphics2DDelegatesFontAndImageLifecycleMethods() throws Exception {
    HeadlessRecordingGL2 gl = new HeadlessRecordingGL2();
    RenderContext renderContext = new RenderContext();
    renderContext.setGL(gl);
    edu.cmu.cs.dennisc.render.gl.imp.Graphics2D graphics = new edu.cmu.cs.dennisc.render.gl.imp.Graphics2D(renderContext);
    graphics.initialize(new Dimension(64, 32));

    Font font = new Font(Font.DIALOG, Font.BOLD, 18);
    Object textRenderer = getField(graphics, "textRenderer");
    Object headlessRenderer = createHeadlessTextRenderer(font);
    Object holder = createTextRendererHolder(headlessRenderer, gl);
    @SuppressWarnings("unchecked")
    Map<Font, ReferencedObject<Object>> active = (Map<Font, ReferencedObject<Object>>) getField(textRenderer, "activeFontToTextRendererMap");
    active.put(font, new ReferencedObject<>(holder, 0));
    graphics.remember(font);
    assertTrue(graphics.isRemembered(font));
    assertNotNull(graphics.getBounds("Hello", font));
    graphics.forget(font);

    Map<?, ?> forgottenFonts = (Map<?, ?>) getField(textRenderer, "forgottenFontToTextRendererMap");
    assertEquals(1, forgottenFonts.size());
    graphics.disposeForgottenImageGenerators();
    assertTrue(forgottenFonts.isEmpty());

    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
    assertTrue(graphics.drawImage(image, 1, 1, null));
    graphics.remember(image);
    assertTrue(graphics.isRemembered(image));
    graphics.forget(image);
    graphics.disposeForgottenImages();
    assertFalse(graphics.isRemembered(image));
  }

  @Test
  public void graphics2DGlHelpersAndDrawStringUseInjectedHeadlessRenderer() throws Exception {
    HeadlessRecordingGL2 gl = new HeadlessRecordingGL2().withInteger(GL.GL_MAX_TEXTURE_SIZE, 128);
    RenderContext renderContext = new RenderContext();
    renderContext.setGL(gl);
    edu.cmu.cs.dennisc.render.gl.imp.Graphics2D graphics = new edu.cmu.cs.dennisc.render.gl.imp.Graphics2D(renderContext);
    graphics.initialize(new Dimension(40, 20));
    graphics.setColor(new Color(10, 20, 30, 128));
    graphics.translate(3, 4);

    invoke(graphics, "glSetPaint", Color.MAGENTA);
    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> invoke(graphics, "glSetPaint", new GradientPaint(0, 0, Color.RED, 1, 1, Color.BLUE)));
    assertEquals("not implemented", thrown.getMessage());
    assertSame(gl, graphics.getGL());

    Font font = new Font(Font.DIALOG, Font.PLAIN, 16);
    Object textRenderer = getField(graphics, "textRenderer");
    Object headlessRenderer = createHeadlessTextRenderer(font);
    Object holder = createTextRendererHolder(headlessRenderer, gl);
    @SuppressWarnings("unchecked")
    Map<Font, ReferencedObject<Object>> active = (Map<Font, ReferencedObject<Object>>) getField(textRenderer, "activeFontToTextRendererMap");
    active.put(font, new ReferencedObject<>(holder, 1));
    graphics.setFont(font);

    try (MinimalGLContext ignored = new MinimalGLContext(gl)) {
      graphics.drawString("abc", 5.0f, 6.0f);
    }

    @SuppressWarnings("unchecked")
    Map<String, ?> stringLocations = (Map<String, ?>) getReflectiveField(headlessRenderer, "stringLocations");
    assertTrue(stringLocations.containsKey("abc"));
  }

  @Test
  public void resourceCacheTextureLifecycleAndUnusedTextureListenersAreTracked() throws Exception {
    GlResourceCache cache = new GlResourceCache();
    HeadlessRecordingGL2 gl = new HeadlessRecordingGL2();
    RenderContext renderContext = new RenderContext();
    renderContext.setGL(gl);

    TrackingTextureAdapter texture = new TrackingTextureAdapter();
    TrackingBinding binding = new TrackingBinding();
    @SuppressWarnings("unchecked")
    Map<GlrTexture<? extends Texture>, ForgettableBinding> textureMap = (Map<GlrTexture<? extends Texture>, ForgettableBinding>) getField(cache, "textureBindingMap");
    textureMap.put(texture, binding);

    cache.forgetTextureAdapter(texture, true, renderContext);
    assertFalse(textureMap.containsKey(texture));
    assertEquals(1, texture.removedContexts);
    cache.actuallyForgetTexturesIfNecessary(renderContext);
    assertEquals(1, binding.forgetCount);

    textureMap.put(texture, null);
    cache.forgetTextureAdapter(texture, false, renderContext);
    cache.forgetAllCachedItems(renderContext);

    AtomicInteger clearedCount = new AtomicInteger();
    RenderContext.UnusedTexturesListener listener = unused -> clearedCount.incrementAndGet();
    GlResourceCache.addUnusedTexturesListener(listener);
    GlResourceCache.clearUnusedTextures(gl);
    GlResourceCache.removeUnusedTexturesListener(listener);
    GlResourceCache.clearUnusedTextures(gl);
    assertEquals(1, clearedCount.get());
  }

  @Test
  public void offscreenDrawableAndSoftwareDrawableExposeFallbackBehavior() {
    AtomicInteger callbackCount = new AtomicInteger();
    StubOffscreenDrawable drawable = new StubOffscreenDrawable(gl -> callbackCount.incrementAndGet());
    drawable.setDrawableSize(7, 11);

    assertNotNull(drawable.getCallback());
    assertEquals(11, drawable.getSize(new Dimension()).width);
    assertEquals(11, drawable.getSize(new Dimension()).height);
    drawable.fire();
    assertEquals(1, callbackCount.get());

    SoftwareOffscreenDrawable software = new SoftwareOffscreenDrawable(gl -> callbackCount.incrementAndGet());
    assertFalse(software.isHardwareAccelerated());
    assertNull(software.getGlDrawable());
    assertThrows(AssertionError.class, software::destroy);
    GLException thrown = assertThrows(GLException.class, software::display);
    assertTrue(thrown.getMessage().contains("glDrawable is null"));
    assertTrue(thrown.getMessage().contains("glContext is null"));
  }

  private static RenderTarget createRenderTargetProxy(Dimension surfaceSize, Map<AbstractCamera, Rectangle> viewports, boolean[] renderingEnabled) {
    return (RenderTarget) Proxy.newProxyInstance(
        RenderTarget.class.getClassLoader(),
        new Class[]{RenderTarget.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getSurfaceSize":
              return surfaceSize;
            case "getActualViewportAsAwtRectangle":
              return viewports.get(args[0]);
            case "isRenderingEnabled":
              return renderingEnabled[0];
            default:
              return defaultValue(method.getReturnType());
          }
        });
  }

  private static Object getField(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static Object getReflectiveField(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static Object createHeadlessTextRenderer(Font font) throws Exception {
    Class<?> factoryClass = Class.forName("edu.cmu.cs.dennisc.render.joglrenderer.HeadlessTextRendererFactory");
    Method createRenderer = factoryClass.getDeclaredMethod("createRenderer", Font.class);
    createRenderer.setAccessible(true);
    return createRenderer.invoke(null, font);
  }

  private static Object createTextRendererHolder(Object renderer, GL gl) throws Exception {
    Class<?> holderClass = Class.forName("edu.cmu.cs.dennisc.render.gl.imp.GlTextRenderer$TextRendererHolder");
    java.lang.reflect.Constructor<?> constructor = holderClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object holder = constructor.newInstance();
    Field textRendererField = holderClass.getDeclaredField("textRenderer");
    textRendererField.setAccessible(true);
    textRendererField.set(holder, renderer);
    Field glField = holderClass.getDeclaredField("gl");
    glField.setAccessible(true);
    glField.set(holder, gl);
    return holder;
  }

  private static Object invoke(Object target, String name, Object arg) {
    try {
      Method method = target.getClass().getDeclaredMethod(name, arg.getClass().getInterfaces().length == 0 ? arg.getClass() : arg.getClass().getInterfaces()[0]);
      method.setAccessible(true);
      return method.invoke(target, arg);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new AssertionError(e);
    }
  }

  private static Object defaultValue(Class<?> type) {
    if (type == Boolean.TYPE) {
      return false;
    } else if (type == Integer.TYPE) {
      return 0;
    } else if (type == Long.TYPE) {
      return 0L;
    } else if (type == Float.TYPE) {
      return 0f;
    } else if (type == Double.TYPE) {
      return 0d;
    }
    return null;
  }

  private static final class TestImageGenerator implements ImageGenerator {
    @Override
    public MipMapGenerationPolicy getMipMapGenerationPolicy() {
      return MipMapGenerationPolicy.PAINT_ONLY_HIGHEST_LEVEL_THEN_SCALE_REMAINING;
    }

    @Override
    public int getWidth() {
      return 2;
    }

    @Override
    public int getHeight() {
      return 2;
    }

    @Override
    public boolean isPotentiallyAlphaBlended() {
      return true;
    }

    @Override
    public boolean isMipMappingDesired() {
      return false;
    }

    @Override
    public void paint(Graphics2D g2, int width, int height) {
      g2.setColor(Color.ORANGE);
      g2.fillRect(0, 0, width, height);
    }
  }

  private static final class TrackingTextureAdapter extends GlrTexture<Texture> {
    private int removedContexts;

    @Override
    public void removeRenderContext(RenderContext rc) {
      super.removeRenderContext(rc);
      this.removedContexts++;
    }

    @Override
    protected com.jogamp.opengl.util.texture.TextureData newTextureData(GL gl, com.jogamp.opengl.util.texture.TextureData currentTexture) {
      return null;
    }
  }

  private static final class TrackingBinding implements ForgettableBinding {
    private int forgetCount;

    @Override
    public void forget(RenderContext rc) {
      this.forgetCount++;
    }
  }

  private static final class StubOffscreenDrawable extends OffscreenDrawable {
    private GLDrawable glDrawable;

    private StubOffscreenDrawable(DisplayCallback callback) {
      super(callback);
    }

    private void setDrawableSize(int width, int height) {
      this.glDrawable = (GLDrawable) Proxy.newProxyInstance(
          GLDrawable.class.getClassLoader(),
          new Class[]{GLDrawable.class},
          (proxy, method, args) -> {
            if (method.getName().equals("getSurfaceWidth")) {
              return width;
            } else if (method.getName().equals("getSurfaceHeight")) {
              return height;
            }
            return defaultValue(method.getReturnType());
          });
    }

    private void fire() {
      fireDisplay(null);
    }

    @Override
    public void initialize(com.jogamp.opengl.GLCapabilities glRequestedCapabilities, com.jogamp.opengl.GLCapabilitiesChooser glCapabilitiesChooser, GLContext glShareContext, int width, int height) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void display() {
    }

    @Override
    public boolean isHardwareAccelerated() {
      return false;
    }

    @Override
    protected GLDrawable getGlDrawable() {
      return this.glDrawable;
    }
  }

  private static final class MinimalGLContext extends GLContext implements AutoCloseable {
    private final GLContext previous;
    private final GL gl;

    private MinimalGLContext(GL gl) {
      this.previous = GLContext.getCurrent();
      this.gl = gl;
      this.contextHandle = 1;
      setCurrent(this);
    }

    @Override
    public void close() {
      setCurrent(this.previous);
    }

    protected static void setCurrent(GLContext context) {
      try {
        Method method = GLContext.class.getDeclaredMethod("setCurrent", GLContext.class);
        method.setAccessible(true);
        method.invoke(null, context);
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(e);
      }
    }

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
    public GL getRootGL() { return this.gl; }
    @Override
    public GL getGL() { return this.gl; }
    @Override
    public GL setGL(GL gl) { return gl; }
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
    public void glDebugMessageControl(int source, int type, int severity, int count, IntBuffer ids, boolean enabled) { }
    @Override
    public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, int idsOffset, boolean enabled) { }
    @Override
    public void glDebugMessageInsert(int source, int type, int id, int severity, String buf) { }
  }
}
