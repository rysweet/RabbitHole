package edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics;



import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import edu.cmu.cs.dennisc.java.awt.MultilineText;
import edu.cmu.cs.dennisc.render.gl.imp.RenderContext;
import edu.cmu.cs.dennisc.render.gl.imp.testing.HeadlessRecordingGL2;
import edu.cmu.cs.dennisc.scenegraph.graphics.Bubble;
import edu.cmu.cs.dennisc.scenegraph.graphics.BubbleManager;
import edu.cmu.cs.dennisc.scenegraph.graphics.OnscreenBubble;
import edu.cmu.cs.dennisc.scenegraph.graphics.SpeechBubble;
import edu.cmu.cs.dennisc.scenegraph.graphics.Text;
import edu.cmu.cs.dennisc.scenegraph.graphics.ThoughtBubble;
import org.junit.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.util.Map;

import static org.junit.Assert.*;

public class MoreCoverageBehaviorTest {



  @Test
  public void speechBubbleBaseAndSubclassRenderCreateAndReuseBubble() throws Exception {
    SpeechBubble sg = new SpeechBubble(originator());
    sg.text.setValue("hello world");
    sg.portion.setValue(0.5);
    RecordingSpeechBubbleAdapter adapter = new RecordingSpeechBubbleAdapter();
    adapter.initialize(sg);

    Font baseFont = sg.font.getValue();
    Font derivedFont = baseFont.deriveFont(baseFont.getSize2D());
    edu.cmu.cs.dennisc.render.Graphics2D graphics = createGraphics(baseFont, derivedFont);
    Rectangle viewport = new Rectangle(0, 0, 640, 360);

    try (MinimalGLContext ignored = new MinimalGLContext(new HeadlessRecordingGL2().withInteger(GL.GL_MAX_TEXTURE_SIZE, 128))) {
      adapter.render(graphics, null, viewport, null);
      assertNotNull(BubbleManager.getInstance().getBubble(sg));
      sg.portion.setValue(1.0);
      adapter.render(graphics, null, viewport, null);
    }

    assertEquals(2, adapter.renderCalls);
    BubbleManager.getInstance().removeBubble(sg);
  }

  @Test
  public void thoughtBubbleAndTextAdaptersHandlePropertiesAndForgetFonts() throws Exception {
    ThoughtBubble bubble = new ThoughtBubble(originator());
    bubble.text.setValue("thinking");
    bubble.portion.setValue(0.5);
    RecordingThoughtBubbleAdapter thoughtAdapter = new RecordingThoughtBubbleAdapter();
    thoughtAdapter.initialize(bubble);

    Font baseFont = bubble.font.getValue();
    Font derivedFont = baseFont.deriveFont(baseFont.getSize2D());
    edu.cmu.cs.dennisc.render.Graphics2D graphics = createGraphics(baseFont, derivedFont);
    Rectangle viewport = new Rectangle(0, 0, 640, 360);

    try (MinimalGLContext ignored = new MinimalGLContext(new HeadlessRecordingGL2().withInteger(GL.GL_MAX_TEXTURE_SIZE, 128))) {
      thoughtAdapter.render(graphics, null, viewport, null);
      bubble.portion.setValue(1.0);
      thoughtAdapter.render(graphics, null, viewport, null);
    }
    assertEquals(2, thoughtAdapter.renderCalls);
    BubbleManager.getInstance().removeBubble(bubble);

    TestSceneText text = new TestSceneText();
    text.text.setValue("caption");
    RecordingTextAdapter textAdapter = new RecordingTextAdapter();
    textAdapter.initialize(text);
    injectFont(text.font.getValue(), graphics);
    textAdapter.render(graphics, null, viewport, null);
    assertEquals(123.0f, textAdapter.wrapWidth, 0.0f);
    assertNotNull(textAdapter.lastMultilineText);

    text.text.setValue("updated");
    textAdapter.propertyChanged(text.text);
    assertNull(getField(textAdapter, "multilineText"));
    Font newFont = text.font.getValue().deriveFont(24.0f);
    text.font.setValue(newFont);
    injectFont(newFont, graphics);
    textAdapter.propertyChanged(text.font);
    textAdapter.render(graphics, null, viewport, null);
    assertSame(newFont, getField(textAdapter, "rememberedFont"));
    textAdapter.forget(graphics);
    assertNull(getField(textAdapter, "rememberedFont"));
  }

  private static Bubble.Originator originator() {
    return (origin, body, textBoundsOffset, bubble, renderTarget, actualViewport, camera, textSize) -> {
      origin.setLocation(20, 30);
      body.setLocation(60, 40);
      textBoundsOffset.setLocation(5, 5);
    };
  }

  private static edu.cmu.cs.dennisc.render.Graphics2D createGraphics(Font... fonts) throws Exception {
    HeadlessRecordingGL2 gl = new HeadlessRecordingGL2().withInteger(GL.GL_MAX_TEXTURE_SIZE, 128);
    RenderContext renderContext = new RenderContext();
    renderContext.setGL(gl);
    Class<?> graphicsType = Class.forName("edu.cmu.cs.dennisc.render.gl.imp.Graphics2D");
    Constructor<?> ctor = graphicsType.getDeclaredConstructor(RenderContext.class);
    ctor.setAccessible(true);
    Object graphics = ctor.newInstance(renderContext);
    Method initialize = graphicsType.getDeclaredMethod("initialize", java.awt.Dimension.class);
    initialize.setAccessible(true);
    initialize.invoke(graphics, new java.awt.Dimension(640, 360));
    for (Font font : fonts) {
      injectFont(font, graphics);
    }
    return (edu.cmu.cs.dennisc.render.Graphics2D) graphics;
  }

  private static void injectFont(Font font, Object graphics) throws Exception {
    Object textRenderer = getField(graphics, "textRenderer");
    @SuppressWarnings("unchecked")
    Map<Font, Object> active = (Map<Font, Object>) getField(textRenderer, "activeFontToTextRendererMap");
    Object holder = createTextRendererHolder(font, (GL) getField(getField(graphics, "renderContext"), "gl"));
    Class<?> refType = Class.forName("edu.cmu.cs.dennisc.render.gl.imp.ReferencedObject");
    Constructor<?> refCtor = refType.getDeclaredConstructor(Object.class, int.class);
    refCtor.setAccessible(true);
    active.put(font, refCtor.newInstance(holder, 0));
  }

  private static Object createTextRendererHolder(Font font, GL gl) throws Exception {
    Class<?> factoryClass = Class.forName("edu.cmu.cs.dennisc.render.joglrenderer.HeadlessTextRendererFactory");
    Method createRenderer = factoryClass.getDeclaredMethod("createRenderer", Font.class);
    createRenderer.setAccessible(true);
    Object renderer = createRenderer.invoke(null, font);

    Class<?> holderClass = Class.forName("edu.cmu.cs.dennisc.render.gl.imp.GlTextRenderer$TextRendererHolder");
    Constructor<?> constructor = holderClass.getDeclaredConstructor();
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

  private static Object getField(Object target, String name) throws Exception {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
      } catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  private static final class RecordingSpeechBubbleAdapter extends GlrSpeechBubble {
    private int renderCalls;

    @Override
    protected void render(edu.cmu.cs.dennisc.render.Graphics2D g2, edu.cmu.cs.dennisc.render.RenderTarget renderTarget, Rectangle actualViewport, edu.cmu.cs.dennisc.scenegraph.AbstractCamera camera, MultilineText multilineText, Font font, Color textColor, float wrapWidth, Color fillColor, Color outlineColor, OnscreenBubble bubble, double portion) {
      this.renderCalls++;
      super.render(g2, renderTarget, actualViewport, camera, multilineText, font, textColor, wrapWidth, fillColor, outlineColor, bubble, portion);
    }
  }

  private static final class RecordingThoughtBubbleAdapter extends GlrThoughtBubble {
    private int renderCalls;

    @Override
    protected void render(edu.cmu.cs.dennisc.render.Graphics2D g2, edu.cmu.cs.dennisc.render.RenderTarget renderTarget, Rectangle actualViewport, edu.cmu.cs.dennisc.scenegraph.AbstractCamera camera, MultilineText multilineText, Font font, Color textColor, float wrapWidth, Color fillColor, Color outlineColor, OnscreenBubble bubble, double portion) {
      this.renderCalls++;
      super.render(g2, renderTarget, actualViewport, camera, multilineText, font, textColor, wrapWidth, fillColor, outlineColor, bubble, portion);
    }
  }

  private static final class TestSceneText extends Text {
    private TestSceneText() {
      super(edu.cmu.cs.dennisc.color.Color4f.BLACK, new Font(Font.DIALOG, Font.PLAIN, 14));
    }
  }

  private static final class RecordingTextAdapter extends GlrText<TestSceneText> {
    private float wrapWidth;
    private MultilineText lastMultilineText;

    @Override
    protected float getWrapWidth(Rectangle actualViewport) {
      return 123.0f;
    }

    @Override
    protected void render(edu.cmu.cs.dennisc.render.Graphics2D g2, edu.cmu.cs.dennisc.render.RenderTarget renderTarget, Rectangle actualViewport, edu.cmu.cs.dennisc.scenegraph.AbstractCamera camera, MultilineText multilineText, Font font, Color textColor, float wrapWidth) {
      this.wrapWidth = wrapWidth;
      this.lastMultilineText = multilineText;
    }
  }

  private static final class MinimalGLContext extends GLContext implements AutoCloseable {
    private final GLContext previous;
    private final GL gl;

    private MinimalGLContext(GL gl) {
      this.previous = GLContext.getCurrent();
      this.gl = gl;
      this.contextHandle = 1;
      setCurrentContext(this);
    }

    @Override
    public void close() {
      setCurrentContext(this.previous);
    }

    protected static void setCurrentContext(GLContext context) {
      try {
        Method method = GLContext.class.getDeclaredMethod("setCurrent", GLContext.class);
        method.setAccessible(true);
        method.invoke(null, context);
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(e);
      }
    }

    @Override public com.jogamp.opengl.GLDrawable setGLDrawable(com.jogamp.opengl.GLDrawable readWrite, boolean setWriteOnly) { return null; }
    @Override public com.jogamp.opengl.GLDrawable getGLDrawable() { return null; }
    @Override public boolean isGLReadDrawableAvailable() { return false; }
    @Override public com.jogamp.opengl.GLDrawable setGLReadDrawable(com.jogamp.opengl.GLDrawable read) { return null; }
    @Override public com.jogamp.opengl.GLDrawable getGLReadDrawable() { return null; }
    @Override public int makeCurrent() { return CONTEXT_CURRENT; }
    @Override public void release() { }
    @Override public void copy(GLContext source, int mask) { }
    @Override public void destroy() { }
    @Override public GL getRootGL() { return this.gl; }
    @Override public GL getGL() { return this.gl; }
    @Override public GL setGL(GL gl) { return gl; }
    @Override public boolean isFunctionAvailable(String glFunctionName) { return true; }
    @Override public boolean isExtensionAvailable(String glExtensionName) { return true; }
    @Override public int getPlatformExtensionCount() { return 0; }
    @Override public String getPlatformExtensionsString() { return ""; }
    @Override public int getGLExtensionCount() { return 0; }
    @Override public String getGLExtensionsString() { return ""; }
    @Override public int getContextCreationFlags() { return 0; }
    @Override public void setContextCreationFlags(int flags) { }
    @Override public int getDefaultVAO() { return 0; }
    @Override public int getBoundFramebuffer(int target) { return 0; }
    @Override public int getDefaultDrawFramebuffer() { return 0; }
    @Override public int getDefaultReadFramebuffer() { return 0; }
    @Override public int getDefaultDrawBuffer() { return 0; }
    @Override public int getDefaultReadBuffer() { return 0; }
    @Override public int getDefaultPixelDataType() { return 0; }
    @Override public int getDefaultPixelDataFormat() { return 0; }
    @Override public com.jogamp.common.os.DynamicLibraryBundle getDynamicLibraryBundle() { return null; }
    @Override public String getGLDebugMessageExtension() { return null; }
    @Override public boolean isGLDebugSynchronous() { return false; }
    @Override public void setGLDebugSynchronous(boolean synchronous) { }
    @Override public boolean isGLDebugMessageEnabled() { return false; }
    @Override public void enableGLDebugMessage(boolean enable) { }
    @Override public void addGLDebugListener(com.jogamp.opengl.GLDebugListener listener) { }
    @Override public void removeGLDebugListener(com.jogamp.opengl.GLDebugListener listener) { }
    @Override public void glDebugMessageControl(int source, int type, int severity, int count, IntBuffer ids, boolean enabled) { }
    @Override public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, int idsOffset, boolean enabled) { }
    @Override public void glDebugMessageInsert(int source, int type, int id, int severity, String buf) { }
  }
}
