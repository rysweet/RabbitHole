package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.opengl.util.awt.TextRenderer;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Extracted from NonCachingTextRenderer.DefaultRenderDelegate inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
public class DefaultRenderDelegate implements TextRenderer.RenderDelegate {
  @Override
  public boolean intensityOnly() {
    return true;
  }

  @Override
  public Rectangle2D getBounds(final CharSequence str, final Font font,
                               final FontRenderContext frc) {
    return getBounds(font.createGlyphVector(frc,
            new CharSequenceIterator(str)),
        frc);
  }

  @Override
  public Rectangle2D getBounds(final String str, final Font font,
                               final FontRenderContext frc) {
    return getBounds(font.createGlyphVector(frc, str), frc);
  }

  @Override
  public Rectangle2D getBounds(final GlyphVector gv, final FontRenderContext frc) {
    return gv.getVisualBounds();
  }

  @Override
  public void drawGlyphVector(final Graphics2D graphics, final GlyphVector str,
                              final int x, final int y) {
    graphics.drawGlyphVector(str, x, y);
  }

  @Override
  public void draw(final Graphics2D graphics, final String str, final int x, final int y) {
    graphics.drawString(str, x, y);
  }
}
