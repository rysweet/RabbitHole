package edu.cmu.cs.dennisc.render.joglrenderer;

/**
 * Extracted from NonCachingTextRenderer.CharacterCache inner class.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
class CharacterCache {
  private CharacterCache() {
  }

  static final Character cache[] = new Character[127 + 1];

  static {
    for (int i = 0; i < cache.length; i++) {
      cache[i] = Character.valueOf((char) i);
    }
  }

  public static Character valueOf(final char c) {
    if (c <= 127) { // must cache
      return CharacterCache.cache[c];
    }
    return Character.valueOf(c);
  }
}
