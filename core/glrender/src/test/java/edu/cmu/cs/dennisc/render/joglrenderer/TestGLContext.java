package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.opengl.*;
import com.jogamp.common.os.DynamicLibraryBundle;

import java.lang.reflect.Method;
import java.nio.IntBuffer;

final class TestGLContext extends GLContext implements AutoCloseable {
  private static final Method SET_CURRENT = lookupSetCurrent();

  private final GLContext previous;
  private GL gl;

  private TestGLContext(GL gl) {
    super();
    this.gl = gl;
    this.contextHandle = 1;
    this.previous = GLContext.getCurrent();
    setCurrentContext(this);
  }

  static TestGLContext makeCurrent(GL gl) {
    return new TestGLContext(gl);
  }

  @Override
  public void close() {
    setCurrentContext(this.previous);
  }

  private static Method lookupSetCurrent() {
    try {
      Method method = GLContext.class.getDeclaredMethod("setCurrent", GLContext.class);
      method.setAccessible(true);
      return method;
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  protected static void setCurrentContext(GLContext context) {
    try {
      SET_CURRENT.invoke(null, context);
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
  public GL setGL(GL gl) { this.gl = gl; return gl; }
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
  public void addGLDebugListener(GLDebugListener listener) { }
  @Override
  public void removeGLDebugListener(GLDebugListener listener) { }
  @Override
  public void glDebugMessageControl(int source, int type, int severity, int count, IntBuffer ids, boolean enabled) { }
  @Override
  public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, int idsOffset, boolean enabled) { }
  @Override
  public void glDebugMessageInsert(int source, int type, int id, int severity, String buf) { }
}
