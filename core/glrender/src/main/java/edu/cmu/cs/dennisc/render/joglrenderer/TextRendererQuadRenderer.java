package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.util.awt.TextureRenderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Extracted from NonCachingTextRenderer.Pipelined_QuadRenderer inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class TextRendererQuadRenderer {
  private final NonCachingTextRenderer textRenderer;
  int mOutstandingGlyphsVerticesPipeline = 0;
  FloatBuffer mTexCoords;
  FloatBuffer mVertCoords;
  boolean usingVBOs;
  int mVBO_For_ResuableTileVertices;
  int mVBO_For_ResuableTileTexCoords;

  TextRendererQuadRenderer(final NonCachingTextRenderer textRenderer) {
    this.textRenderer = textRenderer;
    final GL2 gl = GLContext.getCurrentGL().getGL2();
    mVertCoords = Buffers.newDirectFloatBuffer(NonCachingTextRenderer.kTotalBufferSizeCoordsVerts);
    mTexCoords = Buffers.newDirectFloatBuffer(NonCachingTextRenderer.kTotalBufferSizeCoordsTex);

    usingVBOs = textRenderer.getMyUseVertexArrays() && textRenderer.is15Available(gl);

    if (usingVBOs) {
      try {
        final int[] vbos = new int[2];
        gl.glGenBuffers(2, IntBuffer.wrap(vbos));

        mVBO_For_ResuableTileVertices = vbos[0];
        mVBO_For_ResuableTileTexCoords = vbos[1];

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
            mVBO_For_ResuableTileVertices);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, NonCachingTextRenderer.kTotalBufferSizeBytesVerts,
            null, GL2ES2.GL_STREAM_DRAW); // stream draw because this is a single quad use pipeline

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
            mVBO_For_ResuableTileTexCoords);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, NonCachingTextRenderer.kTotalBufferSizeBytesTex,
            null, GL2ES2.GL_STREAM_DRAW); // stream draw because this is a single quad use pipeline
      } catch (final Exception e) {
        textRenderer.isExtensionAvailable_GL_VERSION_1_5 = false;
        usingVBOs = false;
      }
    }
  }

  public void glTexCoord2f(final float v, final float v1) {
    mTexCoords.put(v);
    mTexCoords.put(v1);
  }

  public void glVertex3f(final float inX, final float inY, final float inZ) {
    mVertCoords.put(inX);
    mVertCoords.put(inY);
    mVertCoords.put(inZ);

    mOutstandingGlyphsVerticesPipeline++;

    if (mOutstandingGlyphsVerticesPipeline >= NonCachingTextRenderer.kTotalBufferSizeVerts) {
      this.draw();
    }
  }

  void draw() {
    if (textRenderer.getMyUseVertexArrays()) {
      drawVertexArrays();
    } else {
      drawIMMEDIATE();
    }
  }

  private void drawVertexArrays() {
    if (mOutstandingGlyphsVerticesPipeline > 0) {
      final GL2 gl = GLContext.getCurrentGL().getGL2();

      final TextureRenderer renderer = textRenderer.getBackingStore();
      renderer.getTexture(); // triggers texture uploads.  Maybe this should be more obvious?

      mVertCoords.rewind();
      mTexCoords.rewind();

      gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);

      if (usingVBOs) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
            mVBO_For_ResuableTileVertices);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
            mOutstandingGlyphsVerticesPipeline * NonCachingTextRenderer.kSizeInBytes_OneVertices_VertexData,
            mVertCoords); // upload only the new stuff
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
      } else {
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, mVertCoords);
      }

      gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);

      if (usingVBOs) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
            mVBO_For_ResuableTileTexCoords);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
            mOutstandingGlyphsVerticesPipeline * NonCachingTextRenderer.kSizeInBytes_OneVertices_TexData,
            mTexCoords); // upload only the new stuff
        gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
      } else {
        gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, mTexCoords);
      }

      gl.glDrawArrays(GL2ES3.GL_QUADS, 0,
          mOutstandingGlyphsVerticesPipeline);

      mVertCoords.rewind();
      mTexCoords.rewind();
      mOutstandingGlyphsVerticesPipeline = 0;
    }
  }

  private void drawIMMEDIATE() {
    if (mOutstandingGlyphsVerticesPipeline > 0) {
      final TextureRenderer renderer = textRenderer.getBackingStore();
      renderer.getTexture(); // triggers texture uploads.  Maybe this should be more obvious?

      final GL2 gl = GLContext.getCurrentGL().getGL2();
      gl.glBegin(GL2ES3.GL_QUADS);

      try {
        final int numberOfQuads = mOutstandingGlyphsVerticesPipeline / 4;
        mVertCoords.rewind();
        mTexCoords.rewind();

        for (int i = 0; i < numberOfQuads; i++) {
          gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
          gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
              mVertCoords.get());

          gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
          gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
              mVertCoords.get());

          gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
          gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
              mVertCoords.get());

          gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
          gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
              mVertCoords.get());
        }
      } catch (final Exception e) {
        e.printStackTrace();
      } finally {
        gl.glEnd();
        mVertCoords.rewind();
        mTexCoords.rewind();
        mOutstandingGlyphsVerticesPipeline = 0;
      }
    }
  }

  public void dispose() {
    final GL2 gl = GLContext.getCurrentGL().getGL2();
    final int[] vbos = new int[2];
    vbos[0] = mVBO_For_ResuableTileVertices;
    vbos[1] = mVBO_For_ResuableTileTexCoords;
    gl.glDeleteBuffers(2, IntBuffer.wrap(vbos));
  }
}
