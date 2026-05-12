package edu.cmu.cs.dennisc.render.joglrenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Extracted from NonCachingTextRenderer.TextData inner class.
 *
 * Data associated with each rectangle of text.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class TextData {
  // Back-pointer to String this TextData describes, if it
  // represents a String rather than a single glyph
  private final String str;

  // If this TextData represents a single glyph, this is its
  // unicode ID
  int unicodeID;

  // The following must be defined and used VERY precisely. This is
  // the offset from the upper-left corner of this rectangle (Java
  // 2D coordinate system) at which the string must be rasterized in
  // order to fit within the rectangle -- the leftmost point of the
  // baseline.
  private final Point origin;

  // This represents the pre-normalized rectangle, which fits
  // within the rectangle on the backing store. We keep a
  // one-pixel border around entries on the backing store to
  // prevent bleeding of adjacent letters when using GL_LINEAR
  // filtering for rendering. The origin of this rectangle is
  // equivalent to the origin above.
  private final Rectangle2D origRect;

  private boolean used; // Whether this text was used recently

  TextData(final String str, final Point origin, final Rectangle2D origRect, final int unicodeID) {
    this.str = str;
    this.origin = origin;
    this.origRect = origRect;
    this.unicodeID = unicodeID;
  }

  String string() {
    return str;
  }

  Point origin() {
    return origin;
  }

  // The following three methods are used to locate the glyph
  // within the expanded rectangle coming from normalize()
  int origOriginX() {
    return (int) -origRect.getMinX();
  }

  int origOriginY() {
    return (int) -origRect.getMinY();
  }

  Rectangle2D origRect() {
    return origRect;
  }

  boolean used() {
    return used;
  }

  void markUsed() {
    used = true;
  }

  void clearUsed() {
    used = false;
  }
}
