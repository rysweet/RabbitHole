package edu.cmu.cs.dennisc.render.joglrenderer;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Extracted from NonCachingTextRenderer.GlyphProducer inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class TextRendererGlyphProducer {
  static final int undefined = -2;
  final FontRenderContext fontRenderContext = null; // FIXME: Never initialized!
  private final NonCachingTextRenderer textRenderer;
  List<TextRendererGlyph> glyphsOutput = new ArrayList<TextRendererGlyph>();
  HashMap<String, GlyphVector> fullGlyphVectorCache = new HashMap<String, GlyphVector>();
  HashMap<Character, GlyphMetrics> glyphMetricsCache = new HashMap<Character, GlyphMetrics>();
  // The mapping from unicode character to font-specific glyph ID
  int[] unicodes2Glyphs;
  // The mapping from glyph ID to Glyph
  TextRendererGlyph[] glyphCache;
  // We re-use this for each incoming string
  NonCachingTextRenderer.CharSequenceIterator iter = new NonCachingTextRenderer.CharSequenceIterator();

  TextRendererGlyphProducer(final int fontLengthInGlyphs, final NonCachingTextRenderer textRenderer) {
    this.textRenderer = textRenderer;
    unicodes2Glyphs = new int[512];
    glyphCache = new TextRendererGlyph[fontLengthInGlyphs];
    clearAllCacheEntries();
  }

  public List<TextRendererGlyph> getGlyphs(final CharSequence inString) {
    glyphsOutput.clear();
    GlyphVector fullRunGlyphVector;
    fullRunGlyphVector = fullGlyphVectorCache.get(inString.toString());
    if (fullRunGlyphVector == null) {
      iter.initFromCharSequence(inString);
      fullRunGlyphVector = textRenderer.font.createGlyphVector(textRenderer.getFontRenderContext(), iter);
      fullGlyphVectorCache.put(inString.toString(), fullRunGlyphVector);
    }
    final boolean complex = (fullRunGlyphVector.getLayoutFlags() != 0);

    // Copied entire class for this. Disabling the glyph cache
    if (complex || NonCachingTextRenderer.DISABLE_GLYPH_CACHE) {
      // Punt to the robust version of the renderer
      glyphsOutput.add(new TextRendererGlyph(inString.toString(), false, textRenderer));
      return glyphsOutput;
    }

    final int lengthInGlyphs = fullRunGlyphVector.getNumGlyphs();
    int i = 0;
    while (i < lengthInGlyphs) {
      final Character letter = NonCachingTextRenderer.CharacterCache.valueOf(inString.charAt(i));
      GlyphMetrics metrics = glyphMetricsCache.get(letter);
      if (metrics == null) {
        metrics = fullRunGlyphVector.getGlyphMetrics(i);
        glyphMetricsCache.put(letter, metrics);
      }
      final TextRendererGlyph glyph = getGlyph(inString, metrics, i);
      if (glyph != null) {
        glyphsOutput.add(glyph);
        i++;
      } else {
        // Assemble a run of characters that don't fit in
        // the cache
        final StringBuilder buf = new StringBuilder();
        while (i < lengthInGlyphs
            && getGlyph(inString, fullRunGlyphVector.getGlyphMetrics(i), i) == null) {
          buf.append(inString.charAt(i++));
        }
        glyphsOutput.add(new TextRendererGlyph(buf.toString(),
            // Any more glyphs after this run?
            i < lengthInGlyphs, textRenderer));
      }
    }
    return glyphsOutput;
  }

  public void clearCacheEntry(final int unicodeID) {
    final int glyphID = unicodes2Glyphs[unicodeID];
    if (glyphID != undefined) {
      final TextRendererGlyph glyph = glyphCache[glyphID];
      if (glyph != null) {
        glyph.clear();
      }
      glyphCache[glyphID] = null;
    }
    unicodes2Glyphs[unicodeID] = undefined;
  }

  public void clearAllCacheEntries() {
    for (int i = 0; i < unicodes2Glyphs.length; i++) {
      clearCacheEntry(i);
    }
  }

  public void register(final TextRendererGlyph glyph) {
    unicodes2Glyphs[glyph.getUnicodeID()] = glyph.getGlyphCode();
    glyphCache[glyph.getGlyphCode()] = glyph;
  }

  public float getGlyphPixelWidth(final char unicodeID) {
    final TextRendererGlyph glyph = getGlyph(unicodeID);
    if (glyph != null) {
      return glyph.getAdvance();
    }

    // Have to do this the hard / uncached way
    textRenderer.singleUnicode[0] = unicodeID;
    if (null == fontRenderContext) { // FIXME: Never initialized!
      throw new InternalError("fontRenderContext never initialized!");
    }
    final GlyphVector gv = textRenderer.font.createGlyphVector(fontRenderContext,
        textRenderer.singleUnicode);
    return gv.getGlyphMetrics(0).getAdvance();
  }

  // Returns a glyph object for this single glyph. Returns null
  // if the unicode or glyph ID would be out of bounds of the
  // glyph cache.
  private TextRendererGlyph getGlyph(final CharSequence inString,
                                     final GlyphMetrics glyphMetrics,
                                     final int index) {
    final char unicodeID = inString.charAt(index);

    if (unicodeID >= unicodes2Glyphs.length) {
      return null;
    }

    final int glyphID = unicodes2Glyphs[unicodeID];
    if (glyphID != undefined) {
      return glyphCache[glyphID];
    }

    // Must fabricate the glyph
    textRenderer.singleUnicode[0] = unicodeID;
    final GlyphVector gv = textRenderer.font.createGlyphVector(textRenderer.getFontRenderContext(),
        textRenderer.singleUnicode);
    return getGlyph(unicodeID, gv, glyphMetrics);
  }

  // It's unclear whether this variant might produce less
  // optimal results than if we can see the entire GlyphVector
  // for the incoming string
  private TextRendererGlyph getGlyph(final int unicodeID) {
    if (unicodeID >= unicodes2Glyphs.length) {
      return null;
    }

    final int glyphID = unicodes2Glyphs[unicodeID];
    if (glyphID != undefined) {
      return glyphCache[glyphID];
    }
    textRenderer.singleUnicode[0] = (char) unicodeID;
    final GlyphVector gv = textRenderer.font.createGlyphVector(textRenderer.getFontRenderContext(),
        textRenderer.singleUnicode);
    return getGlyph(unicodeID, gv, gv.getGlyphMetrics(0));
  }

  private TextRendererGlyph getGlyph(final int unicodeID,
                                     final GlyphVector singleUnicodeGlyphVector,
                                     final GlyphMetrics metrics) {
    final int glyphCode = singleUnicodeGlyphVector.getGlyphCode(0);
    // Have seen huge glyph codes (65536) coming out of some fonts in some Unicode situations
    if (glyphCode >= glyphCache.length) {
      return null;
    }
    final TextRendererGlyph glyph = new TextRendererGlyph(unicodeID,
        glyphCode,
        metrics.getAdvance(),
        singleUnicodeGlyphVector,
        this, textRenderer);
    register(glyph);
    return glyph;
  }
}
