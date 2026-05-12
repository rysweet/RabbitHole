package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextureRenderer;

import java.awt.*;

/**
 * Extracted from NonCachingTextRenderer.DebugListener inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class DebugListener implements GLEventListener {
  private final NonCachingTextRenderer textRenderer;
  private GLU glu;
  private Frame frame;

  DebugListener(final NonCachingTextRenderer textRenderer, final GL gl, final Frame frame) {
    this.textRenderer = textRenderer;
    this.glu = GLU.createGLU(gl);
    this.frame = frame;
  }

  @Override
  public void display(final GLAutoDrawable drawable) {
    final GL2 gl = GLContext.getCurrentGL().getGL2();
    gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

    if (textRenderer.packer == null) {
      return;
    }

    final TextureRenderer rend = textRenderer.getBackingStore();
    final int w = rend.getWidth();
    final int h = rend.getHeight();
    rend.beginOrthoRendering(w, h);
    rend.drawOrthoRect(0, 0);
    rend.endOrthoRendering();

    if ((frame.getWidth() != w) || (frame.getHeight() != h)) {
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          frame.setSize(w, h);
        }
      });
    }
  }

  @Override
  public void dispose(final GLAutoDrawable drawable) {
    textRenderer.mPipelinedQuadRenderer.dispose();
    // n/a glu.destroy(); ??
    glu=null;
    frame=null;
  }

  // Unused methods
  @Override
  public void init(final GLAutoDrawable drawable) {
  }

  @Override
  public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width,
                      final int height) {
  }

  public void displayChanged(final GLAutoDrawable drawable,
                             final boolean modeChanged, final boolean deviceChanged) {
  }
}
