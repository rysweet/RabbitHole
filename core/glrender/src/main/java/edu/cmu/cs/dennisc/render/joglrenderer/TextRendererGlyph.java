package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.packrect.Rect;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Extracted from NonCachingTextRenderer.Glyph inner class.
 *
 * A Glyph represents either a single unicode glyph or a
 * substring of characters to be drawn. The reason for the dual
 * behavior is so that we can take in a sequence of unicode
 * characters and partition them into runs of individual glyphs,
 * but if we encounter complex text and/or unicode sequences we
 * don't understand, we can render them using the
 * string-by-string method.
 *
 * Glyphs need to be able to re-upload themselves to the backing
 * store on demand as we go along in the render sequence.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class TextRendererGlyph {
  private final NonCachingTextRenderer textRenderer;
  // If this Glyph represents an individual unicode glyph, this
  // is its unicode ID. If it represents a String, this is -1.
  private int unicodeID;
  // If the above field isn't -1, then these fields are used.
  // The glyph code in the font
  private int glyphCode;
  // The GlyphProducer which created us
  private TextRendererGlyphProducer producer;
  // The advance of this glyph
  private float advance;
  // The GlyphVector for this single character; this is passed
  // in during construction but cleared during the upload
  // process
  private GlyphVector singleUnicodeGlyphVector;
  // The rectangle of this glyph on the backing store, or null
  // if it has been cleared due to space pressure
  private Rect glyphRectForTextureMapping;
  // If this Glyph represents a String, this is the sequence of
  // characters
  private String str;
  // Whether we need a valid advance when rendering this string
  // (i.e., whether it has other single glyphs coming after it)
  private boolean needAdvance;

  // Creates a Glyph representing an individual Unicode character
  public TextRendererGlyph(final int unicodeID,
                           final int glyphCode,
                           final float advance,
                           final GlyphVector singleUnicodeGlyphVector,
                           final TextRendererGlyphProducer producer,
                           final NonCachingTextRenderer textRenderer) {
    this.textRenderer = textRenderer;
    this.unicodeID = unicodeID;
    this.glyphCode = glyphCode;
    this.advance = advance;
    this.singleUnicodeGlyphVector = singleUnicodeGlyphVector;
    this.producer = producer;
  }

  // Creates a Glyph representing a sequence of characters, with
  // an indication of whether additional single glyphs are being
  // rendered after it
  public TextRendererGlyph(final String str, final boolean needAdvance,
                           final NonCachingTextRenderer textRenderer) {
    this.textRenderer = textRenderer;
    this.str = str;
    this.needAdvance = needAdvance;
  }

  /** Returns this glyph's unicode ID */
  public int getUnicodeID() {
    return unicodeID;
  }

  /** Returns this glyph's (font-specific) glyph code */
  public int getGlyphCode() {
    return glyphCode;
  }

  /** Returns the advance for this glyph */
  public float getAdvance() {
    return advance;
  }

  /** Draws this glyph and returns the (x) advance for this glyph */
  public float draw3D(final float inX, final float inY, final float z, final float scaleFactor) {
    if (str != null) {
      textRenderer.draw3D_ROBUST(str, inX, inY, z, scaleFactor);
      if (!needAdvance) {
        return 0;
      }
      // Compute and return the advance for this string
      final GlyphVector gv = textRenderer.font.createGlyphVector(textRenderer.getFontRenderContext(), str);
      float totalAdvance = 0;
      for (int i = 0; i < gv.getNumGlyphs(); i++) {
        totalAdvance += gv.getGlyphMetrics(i).getAdvance();
      }
      return totalAdvance;
    }

    // This is the code path taken for individual glyphs
    if (glyphRectForTextureMapping == null) {
      upload();
    }

    try {
      if (textRenderer.mPipelinedQuadRenderer == null) {
        textRenderer.mPipelinedQuadRenderer = new TextRendererQuadRenderer(textRenderer);
      }

      final TextureRenderer renderer = textRenderer.getBackingStore();
      // Handles case where NPOT texture is used for backing store
      final TextureCoords wholeImageTexCoords = renderer.getTexture().getImageTexCoords();
      final float xScale = wholeImageTexCoords.right();
      final float yScale = wholeImageTexCoords.bottom();

      final Rect rect = glyphRectForTextureMapping;
      final NonCachingTextRenderer.TextData data = (NonCachingTextRenderer.TextData) rect.getUserData();
      data.markUsed();

      final Rectangle2D origRect = data.origRect();

      final float x = inX - (scaleFactor * data.origOriginX());
      final float y = inY - (scaleFactor * ((float) origRect.getHeight() - data.origOriginY()));

      final int texturex = rect.x() + (data.origin().x - data.origOriginX());
      final int texturey = renderer.getHeight() - rect.y() - (int) origRect.getHeight() -
          (data.origin().y - data.origOriginY());
      final int width = (int) origRect.getWidth();
      final int height = (int) origRect.getHeight();

      final float tx1 = xScale * texturex / renderer.getWidth();
      final float ty1 = yScale * (1.0f -
          ((float) texturey / (float) renderer.getHeight()));
      final float tx2 = xScale * (texturex + width) / renderer.getWidth();
      final float ty2 = yScale * (1.0f -
          ((float) (texturey + height) / (float) renderer.getHeight()));

      textRenderer.mPipelinedQuadRenderer.glTexCoord2f(tx1, ty1);
      textRenderer.mPipelinedQuadRenderer.glVertex3f(x, y, z);
      textRenderer.mPipelinedQuadRenderer.glTexCoord2f(tx2, ty1);
      textRenderer.mPipelinedQuadRenderer.glVertex3f(x + (width * scaleFactor), y,
          z);
      textRenderer.mPipelinedQuadRenderer.glTexCoord2f(tx2, ty2);
      textRenderer.mPipelinedQuadRenderer.glVertex3f(x + (width * scaleFactor),
          y + (height * scaleFactor), z);
      textRenderer.mPipelinedQuadRenderer.glTexCoord2f(tx1, ty2);
      textRenderer.mPipelinedQuadRenderer.glVertex3f(x,
          y + (height * scaleFactor), z);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return advance;
  }

  /** Notifies this glyph that it's been cleared out of the cache */
  public void clear() {
    glyphRectForTextureMapping = null;
  }

  private void upload() {
    final GlyphVector gv = getGlyphVector();
    final Rectangle2D origBBox = NonCachingTextRenderer.preNormalize(
        textRenderer.renderDelegate.getBounds(gv, textRenderer.getFontRenderContext()));
    final Rectangle2D bbox = textRenderer.normalize(origBBox);
    final Point origin = new Point((int) -bbox.getMinX(),
        (int) -bbox.getMinY());
    final Rect rect = new Rect(0, 0, (int) bbox.getWidth(),
        (int) bbox.getHeight(),
        new NonCachingTextRenderer.TextData(null, origin, origBBox, unicodeID));
    textRenderer.packer.add(rect);
    glyphRectForTextureMapping = rect;
    final Graphics2D g = textRenderer.getGraphics2D();
    // OK, should now have an (x, y) for this rectangle; rasterize
    // the glyph
    final int strx = rect.x() + origin.x;
    final int stry = rect.y() + origin.y;

    // Clear out the area we're going to draw into
    g.setComposite(AlphaComposite.Clear);
    g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
    g.setComposite(AlphaComposite.Src);

    // Draw the string
    textRenderer.renderDelegate.drawGlyphVector(g, gv, strx, stry);

    if (NonCachingTextRenderer.DRAW_BBOXES) {
      final NonCachingTextRenderer.TextData data = (NonCachingTextRenderer.TextData) rect.getUserData();
      // Draw a bounding box on the backing store
      g.drawRect(strx - data.origOriginX(),
          stry - data.origOriginY(),
          (int) data.origRect().getWidth(),
          (int) data.origRect().getHeight());
      g.drawRect(strx - data.origin().x,
          stry - data.origin().y,
          rect.w(),
          rect.h());
    }

    // Mark this region of the TextureRenderer as dirty
    textRenderer.getBackingStore().markDirty(rect.x(), rect.y(), rect.w(),
        rect.h());
    // Re-register ourselves with our producer
    producer.register(this);
  }

  private GlyphVector getGlyphVector() {
    final GlyphVector gv = singleUnicodeGlyphVector;
    if (gv != null) {
      singleUnicodeGlyphVector = null; // Don't need this anymore
      return gv;
    }
    textRenderer.singleUnicode[0] = (char) unicodeID;
    return textRenderer.font.createGlyphVector(textRenderer.getFontRenderContext(),
        textRenderer.singleUnicode);
  }
}
