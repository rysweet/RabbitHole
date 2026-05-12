package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.packrect.BackingStoreManager;
import com.jogamp.opengl.util.packrect.Rect;

import java.awt.*;

/**
 * Extracted from NonCachingTextRenderer.Manager inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class Manager implements BackingStoreManager {
  private final NonCachingTextRenderer textRenderer;
  private Graphics2D g;

  Manager(final NonCachingTextRenderer textRenderer) {
    this.textRenderer = textRenderer;
  }

  @Override
  public Object allocateBackingStore(final int w, final int h) {
    // FIXME: should consider checking Font's attributes to see
    // whether we're likely to need to support a full RGBA backing
    // store (i.e., non-default Paint, foreground color, etc.), but
    // for now, let's just be more efficient
    TextureRenderer renderer;

    if (textRenderer.renderDelegate.intensityOnly()) {
      renderer = TextureRenderer.createAlphaOnlyRenderer(w, h, textRenderer.mipmap);
    } else {
      renderer = new TextureRenderer(w, h, true, textRenderer.mipmap);
    }
    renderer.setSmoothing(textRenderer.smoothing);

    if (NonCachingTextRenderer.DEBUG) {
      System.err.println(" TextRenderer allocating backing store " +
          w + " x " + h);
    }

    return renderer;
  }

  @Override
  public void deleteBackingStore(final Object backingStore) {
    ((TextureRenderer) backingStore).dispose();
  }

  @Override
  public boolean preExpand(final Rect cause, final int attemptNumber) {
    // Only try this one time; clear out potentially obsolete entries
    // NOTE: this heuristic and the fact that it clears the used bit
    // of all entries seems to cause cycling of entries in some
    // situations, where the backing store becomes small compared to
    // the amount of text on the screen (see the TextFlow demo) and
    // the entries continually cycle in and out of the backing
    // store, decreasing performance. If we added a little age
    // information to the entries, and only cleared out entries
    // above a certain age, this behavior would be eliminated.
    // However, it seems the system usually stabilizes itself, so
    // for now we'll just keep things simple. Note that if we don't
    // clear the used bit here, the backing store tends to increase
    // very quickly to its maximum size, at least with the TextFlow
    // demo when the text is being continually re-laid out.
    if (attemptNumber == 0) {
      if (NonCachingTextRenderer.DEBUG) {
        System.err.println(
            "Clearing unused entries in preExpand(): attempt number " +
                attemptNumber);
      }

      if (textRenderer.inBeginEndPair) {
        // Draw any outstanding glyphs
        textRenderer.flush();
      }

      textRenderer.clearUnusedEntries();

      return true;
    }

    return false;
  }

  @Override
  public boolean additionFailed(final Rect cause, final int attemptNumber) {
    // Heavy hammer -- might consider doing something different
    textRenderer.packer.clear();
    textRenderer.stringLocations.clear();
    textRenderer.mGlyphProducer.clearAllCacheEntries();

    if (NonCachingTextRenderer.DEBUG) {
      System.err.println(
          " *** Cleared all text because addition failed ***");
    }

    if (attemptNumber == 0) {
      return true;
    }

    return false;
  }

  @Override
  public boolean canCompact() {
    return true;
  }

  @Override
  public void beginMovement(final Object oldBackingStore, final Object newBackingStore) {
    // Exit the begin / end pair if necessary
    if (textRenderer.inBeginEndPair) {
      // Draw any outstanding glyphs
      textRenderer.flush();

      final GL2 gl = GLContext.getCurrentGL().getGL2();

      // Pop client attrib bits used by the pipelined quad renderer
      gl.glPopClientAttrib();

      // The OpenGL spec is unclear about whether this changes the
      // buffer bindings, so preemptively zero out the GL_ARRAY_BUFFER
      // binding
      if (textRenderer.getMyUseVertexArrays() && textRenderer.is15Available(gl)) {
        try {
          gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        } catch (final Exception e) {
          textRenderer.isExtensionAvailable_GL_VERSION_1_5 = false;
        }
      }

      if (textRenderer.isOrthoMode) {
        ((TextureRenderer) oldBackingStore).endOrthoRendering();
      } else {
        ((TextureRenderer) oldBackingStore).end3DRendering();
      }
    }

    final TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
    g = newRenderer.createGraphics();
  }

  @Override
  public void move(final Object oldBackingStore, final Rect oldLocation,
                   final Object newBackingStore, final Rect newLocation) {
    final TextureRenderer oldRenderer = (TextureRenderer) oldBackingStore;
    final TextureRenderer newRenderer = (TextureRenderer) newBackingStore;

    if (oldRenderer == newRenderer) {
      // Movement on the same backing store -- easy case
      g.copyArea(oldLocation.x(), oldLocation.y(), oldLocation.w(),
          oldLocation.h(), newLocation.x() - oldLocation.x(),
          newLocation.y() - oldLocation.y());
    } else {
      // Need to draw from the old renderer's image into the new one
      final Image img = oldRenderer.getImage();
      g.drawImage(img, newLocation.x(), newLocation.y(),
          newLocation.x() + newLocation.w(),
          newLocation.y() + newLocation.h(), oldLocation.x(),
          oldLocation.y(), oldLocation.x() + oldLocation.w(),
          oldLocation.y() + oldLocation.h(), null);
    }
  }

  @Override
  public void endMovement(final Object oldBackingStore, final Object newBackingStore) {
    g.dispose();

    // Sync the whole surface
    final TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
    newRenderer.markDirty(0, 0, newRenderer.getWidth(),
        newRenderer.getHeight());

    // Re-enter the begin / end pair if necessary
    if (textRenderer.inBeginEndPair) {
      if (textRenderer.isOrthoMode) {
        ((TextureRenderer) newBackingStore).beginOrthoRendering(textRenderer.beginRenderingWidth,
            textRenderer.beginRenderingHeight, textRenderer.beginRenderingDepthTestDisabled);
      } else {
        ((TextureRenderer) newBackingStore).begin3DRendering();
      }

      // Push client attrib bits used by the pipelined quad renderer
      final GL2 gl = GLContext.getCurrentGL().getGL2();
      gl.glPushClientAttrib((int) GL2.GL_ALL_CLIENT_ATTRIB_BITS);

      if (textRenderer.haveCachedColor) {
        if (textRenderer.cachedColor == null) {
          ((TextureRenderer) newBackingStore).setColor(textRenderer.cachedR,
              textRenderer.cachedG, textRenderer.cachedB, textRenderer.cachedA);
        } else {
          ((TextureRenderer) newBackingStore).setColor(textRenderer.cachedColor);
        }
      }
    } else {
      textRenderer.needToResetColor = true;
    }
  }
}
