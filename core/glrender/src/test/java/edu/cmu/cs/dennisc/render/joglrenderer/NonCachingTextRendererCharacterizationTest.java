package edu.cmu.cs.dennisc.render.joglrenderer;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.CharacterIterator;

import static org.junit.Assert.*;

/**
 * Characterization tests for NonCachingTextRenderer.
 *
 * Pins observable behavior of inner classes and static helpers
 * BEFORE any refactoring. Tests here run WITHOUT a real GL context.
 *
 * Test groups:
 *   1. Constants – verify static/package-private field values
 *   2. preNormalize – pure static geometry method
 *   3. CharSequenceIterator – CharacterIterator over CharSequence
 *   4. TextData – value object for cached text rectangles
 *   5. DefaultRenderDelegate – font metrics delegation
 *   6. CharacterCache – fast Character boxing for ASCII
 *   7. Constructor / accessors – guarded with Assume
 */
public class NonCachingTextRendererCharacterizationTest {

  private static final Font TEST_FONT = new Font(Font.DIALOG, Font.PLAIN, 18);
  private static FontRenderContext headlessFrc;
  private static Method preNormalizeMethod;
  private static Class<?> characterCacheClass;
  private static Method charCacheValueOf;
  private static DefaultRenderDelegate defaultDelegate;
  private static Class<?> charSeqIterClass;
  private static Constructor<?> charSeqIterCtor;
  private static Constructor<?> charSeqIterNoArgCtor;
  private static NonCachingTextRenderer sharedRenderer;

  @BeforeClass
  public static void setUp() throws Exception {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    headlessFrc = g.getFontRenderContext();
    g.dispose();

    preNormalizeMethod = NonCachingTextRenderer.class.getDeclaredMethod("preNormalize", Rectangle2D.class);
    preNormalizeMethod.setAccessible(true);

    characterCacheClass = Class.forName(
        "edu.cmu.cs.dennisc.render.joglrenderer.CharacterCache");
    charCacheValueOf = characterCacheClass.getDeclaredMethod("valueOf", char.class);
    charCacheValueOf.setAccessible(true);

    charSeqIterClass = Class.forName(
        "edu.cmu.cs.dennisc.render.joglrenderer.CharSequenceIterator");
    charSeqIterCtor = charSeqIterClass.getDeclaredConstructor(CharSequence.class);
    charSeqIterCtor.setAccessible(true);
    charSeqIterNoArgCtor = charSeqIterClass.getDeclaredConstructor();
    charSeqIterNoArgCtor.setAccessible(true);

    defaultDelegate = new DefaultRenderDelegate();

    try {
      sharedRenderer = new NonCachingTextRenderer(TEST_FONT);
    } catch (Exception | Error e) {
      sharedRenderer = null;
    }
  }

  // ── 1. Constants ──────────────────────────────────────────────────

  @Test
  public void kSize_is256() {
    assertEquals("kSize must be 256", 256, NonCachingTextRenderer.kSize);
  }

  @Test
  public void kQuadsPerBuffer_is100() {
    assertEquals(100, NonCachingTextRenderer.kQuadsPerBuffer);
  }

  @Test
  public void kCoordsPerVertVerts_is3() {
    assertEquals(3, NonCachingTextRenderer.kCoordsPerVertVerts);
  }

  @Test
  public void kCoordsPerVertTex_is2() {
    assertEquals(2, NonCachingTextRenderer.kCoordsPerVertTex);
  }

  @Test
  public void kVertsPerQuad_is4() {
    assertEquals(4, NonCachingTextRenderer.kVertsPerQuad);
  }

  @Test
  public void kTotalBufferSizeVerts_isProductOfQuadsAndVerts() {
    assertEquals(
        NonCachingTextRenderer.kQuadsPerBuffer * NonCachingTextRenderer.kVertsPerQuad,
        NonCachingTextRenderer.kTotalBufferSizeVerts);
  }

  @Test
  public void kSizeInBytes_vertexData_isThreeFloats() {
    assertEquals(3 * 4, NonCachingTextRenderer.kSizeInBytes_OneVertices_VertexData);
  }

  @Test
  public void disableGlyphCache_isTrue() throws Exception {
    Field f = NonCachingTextRenderer.class.getDeclaredField("DISABLE_GLYPH_CACHE");
    f.setAccessible(true);
    assertTrue("DISABLE_GLYPH_CACHE must be true — this is the whole point of the class",
        (boolean) f.get(null));
  }

  @Test
  public void drawBBoxes_isFalse() throws Exception {
    Field f = NonCachingTextRenderer.class.getDeclaredField("DRAW_BBOXES");
    f.setAccessible(true);
    assertFalse("DRAW_BBOXES should be false in production", (boolean) f.get(null));
  }

  @Test
  public void cyclesPerFlush_is100() throws Exception {
    Field f = NonCachingTextRenderer.class.getDeclaredField("CYCLES_PER_FLUSH");
    f.setAccessible(true);
    assertEquals(100, f.getInt(null));
  }

  @Test
  public void maxVerticalFragmentation_is0point7() throws Exception {
    Field f = NonCachingTextRenderer.class.getDeclaredField("MAX_VERTICAL_FRAGMENTATION");
    f.setAccessible(true);
    assertEquals(0.7f, f.getFloat(null), 0.001f);
  }

  // ── 2. preNormalize ───────────────────────────────────────────────

  @Test
  public void preNormalize_expandsByOnePxSlop() throws Exception {
    Rectangle2D input = new Rectangle2D.Double(0.5, 0.5, 10.0, 10.0);
    Rectangle2D result = (Rectangle2D) preNormalizeMethod.invoke(null, input);
    // floor(0.5)-1 = -1, ceil(0.5+10.0)+1 = 12; width=13, height=13
    assertEquals("minX: floor(0.5)-1", -1.0, result.getMinX(), 0.0);
    assertEquals("minY: floor(0.5)-1", -1.0, result.getMinY(), 0.0);
    assertEquals("width: 12 - (-1) = 13", 13.0, result.getWidth(), 0.0);
    assertEquals("height: 12 - (-1) = 13", 13.0, result.getHeight(), 0.0);
  }

  @Test
  public void preNormalize_handlesNegativeCoordinates() throws Exception {
    Rectangle2D input = new Rectangle2D.Double(-3.2, -1.8, 5.0, 4.0);
    Rectangle2D result = (Rectangle2D) preNormalizeMethod.invoke(null, input);
    // minX: floor(-3.2)-1 = -5, minY: floor(-1.8)-1 = -3
    // maxX: ceil(-3.2+5.0)+1 = ceil(1.8)+1 = 3, maxY: ceil(-1.8+4.0)+1 = ceil(2.2)+1 = 4
    assertEquals(-5.0, result.getMinX(), 0.0);
    assertEquals(-3.0, result.getMinY(), 0.0);
    assertEquals(8.0, result.getWidth(), 0.0);
    assertEquals(7.0, result.getHeight(), 0.0);
  }

  @Test
  public void preNormalize_integerInput_stillExpandsBySlop() throws Exception {
    Rectangle2D input = new Rectangle2D.Double(0, 0, 10, 10);
    Rectangle2D result = (Rectangle2D) preNormalizeMethod.invoke(null, input);
    // floor(0)-1 = -1, ceil(10)+1 = 11; width=12
    assertEquals(-1.0, result.getMinX(), 0.0);
    assertEquals(-1.0, result.getMinY(), 0.0);
    assertEquals(12.0, result.getWidth(), 0.0);
    assertEquals(12.0, result.getHeight(), 0.0);
  }

  // ── 3. CharSequenceIterator ───────────────────────────────────────

  private Object newCharSequenceIterator(CharSequence seq) throws Exception {
    return charSeqIterCtor.newInstance(seq);
  }

  private CharacterIterator asCharIter(Object csi) {
    return (CharacterIterator) csi;
  }

  @Test
  public void charSeqIter_firstOnHello_returnsH() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("Hello"));
    assertEquals('H', it.first());
  }

  @Test
  public void charSeqIter_firstOnEmpty_returnsDone() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator(""));
    assertEquals(CharacterIterator.DONE, it.first());
  }

  @Test
  public void charSeqIter_lastOnHello_returnso() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("Hello"));
    assertEquals('o', it.last());
  }

  @Test
  public void charSeqIter_lastOnEmpty_returnsDone() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator(""));
    // last() sets index to max(0, 0-1)=0, current() on length==0 → DONE
    assertEquals(CharacterIterator.DONE, it.last());
  }

  @Test
  public void charSeqIter_nextAdvancesThroughString() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("AB"));
    it.first();
    assertEquals('B', it.next());
    assertEquals(CharacterIterator.DONE, it.next());
  }

  @Test
  public void charSeqIter_previousClampsAtZero() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("AB"));
    it.first();
    // previous from index 0: max(0-1, 0) = 0
    assertEquals('A', it.previous());
  }

  @Test
  public void charSeqIter_setIndexPositions() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("ABCDE"));
    assertEquals('C', it.setIndex(2));
    assertEquals(2, it.getIndex());
  }

  @Test
  public void charSeqIter_getBeginIndex_isZero() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("test"));
    assertEquals(0, it.getBeginIndex());
  }

  @Test
  public void charSeqIter_getEndIndex_isLength() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("test"));
    assertEquals(4, it.getEndIndex());
  }

  @Test
  public void charSeqIter_clonePreservesState() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("ABCD"));
    it.setIndex(2);
    CharacterIterator cloned = (CharacterIterator) it.clone();
    assertEquals('C', cloned.current());
    assertEquals(2, cloned.getIndex());
    // Mutating original doesn't affect clone
    it.first();
    assertEquals('C', cloned.current());
  }

  @Test
  public void charSeqIter_currentPastEnd_returnsDone() throws Exception {
    CharacterIterator it = asCharIter(newCharSequenceIterator("X"));
    it.first();
    it.next(); // past end
    assertEquals(CharacterIterator.DONE, it.current());
  }

  @Test
  public void charSeqIter_initFromCharSequence_resets() throws Exception {
    Object csi = charSeqIterNoArgCtor.newInstance();
    Method init = charSeqIterClass.getDeclaredMethod("initFromCharSequence", CharSequence.class);
    init.setAccessible(true);
    init.invoke(csi, "New");
    CharacterIterator it = (CharacterIterator) csi;
    assertEquals('N', it.current());
    assertEquals(3, it.getEndIndex());
  }

  // ── 4. TextData ───────────────────────────────────────────────────

  @Test
  public void textData_string_returnsConstructorArg() {
    TextData td = new TextData(
        "hello", new Point(5, 10), new Rectangle2D.Double(-2, -3, 20, 15), 42);
    assertEquals("hello", td.string());
  }

  @Test
  public void textData_origin_returnsConstructorPoint() {
    Point p = new Point(5, 10);
    TextData td = new TextData(
        "x", p, new Rectangle2D.Double(0, 0, 10, 10), -1);
    assertSame(p, td.origin());
  }

  @Test
  public void textData_origOriginX_isNegativeMinX() {
    Rectangle2D origRect = new Rectangle2D.Double(-3.0, -5.0, 20, 15);
    TextData td = new TextData(
        "x", new Point(0, 0), origRect, -1);
    // origOriginX = (int) -origRect.getMinX() = (int) -(-3.0) = 3
    assertEquals(3, td.origOriginX());
  }

  @Test
  public void textData_origOriginY_isNegativeMinY() {
    Rectangle2D origRect = new Rectangle2D.Double(-3.0, -5.0, 20, 15);
    TextData td = new TextData(
        "x", new Point(0, 0), origRect, -1);
    assertEquals(5, td.origOriginY());
  }

  @Test
  public void textData_origRect_returnsSameInstance() {
    Rectangle2D origRect = new Rectangle2D.Double(0, 0, 10, 10);
    TextData td = new TextData(
        "x", new Point(0, 0), origRect, -1);
    assertSame(origRect, td.origRect());
  }

  @Test
  public void textData_usedLifecycle() {
    TextData td = new TextData(
        "x", new Point(0, 0), new Rectangle2D.Double(0, 0, 10, 10), -1);
    assertFalse("starts unused", td.used());
    td.markUsed();
    assertTrue("after markUsed", td.used());
    td.clearUsed();
    assertFalse("after clearUsed", td.used());
  }

  @Test
  public void textData_unicodeID_isAccessible() {
    TextData td = new TextData(
        null, new Point(0, 0), new Rectangle2D.Double(0, 0, 1, 1), 65);
    assertEquals(65, td.unicodeID);
  }

  @Test
  public void textData_nullString_isAllowed() {
    TextData td = new TextData(
        null, new Point(0, 0), new Rectangle2D.Double(0, 0, 1, 1), -1);
    assertNull(td.string());
  }

  // ── 5. DefaultRenderDelegate ──────────────────────────────────────

  @Test
  public void defaultRenderDelegate_intensityOnly_isTrue() {
    assertTrue(defaultDelegate.intensityOnly());
  }

  @Test
  public void defaultRenderDelegate_getBoundsString_returnsNonNull() {
    Rectangle2D bounds = defaultDelegate.getBounds("Hello", TEST_FONT, headlessFrc);
    assertNotNull(bounds);
    assertTrue("width > 0 for non-empty string", bounds.getWidth() > 0);
    assertTrue("height > 0 for non-empty string", bounds.getHeight() > 0);
  }

  @Test
  public void defaultRenderDelegate_getBoundsCharSequence_returnsNonNull() {
    CharSequence cs = "World";
    Rectangle2D bounds = defaultDelegate.getBounds(cs, TEST_FONT, headlessFrc);
    assertNotNull(bounds);
    assertTrue("width > 0", bounds.getWidth() > 0);
  }

  @Test
  public void defaultRenderDelegate_getBoundsGlyphVector_returnsVisualBounds() {
    GlyphVector gv = TEST_FONT.createGlyphVector(headlessFrc, "Test");
    Rectangle2D expected = gv.getVisualBounds();
    Rectangle2D actual = defaultDelegate.getBounds(gv, headlessFrc);
    assertEquals(expected, actual);
  }

  @Test
  public void defaultRenderDelegate_getBoundsString_matchesCharSequenceOverload() {
    String text = "Matching";
    Rectangle2D fromString = defaultDelegate.getBounds(text, TEST_FONT, headlessFrc);
    Rectangle2D fromCharSeq = defaultDelegate.getBounds((CharSequence) text, TEST_FONT, headlessFrc);
    // Both create glyph vectors from the same text, should produce same visual bounds
    assertEquals("width match", fromString.getWidth(), fromCharSeq.getWidth(), 0.001);
    assertEquals("height match", fromString.getHeight(), fromCharSeq.getHeight(), 0.001);
  }

  @Test
  public void defaultRenderDelegate_longerString_hasWiderBounds() {
    Rectangle2D boundsShort = defaultDelegate.getBounds("A", TEST_FONT, headlessFrc);
    Rectangle2D boundsLong = defaultDelegate.getBounds("AAAAAA", TEST_FONT, headlessFrc);
    assertTrue("longer string should be wider",
        boundsLong.getWidth() > boundsShort.getWidth());
  }

  // ── 6. CharacterCache ─────────────────────────────────────────────

  @Test
  public void characterCache_asciiReturnsCachedInstance() throws Exception {
    Character a1 = (Character) charCacheValueOf.invoke(null, 'A');
    Character a2 = (Character) charCacheValueOf.invoke(null, 'A');
    assertSame("ASCII chars should return identity-equal cached instances", a1, a2);
  }

  @Test
  public void characterCache_cacheSize_is128() throws Exception {
    Field cacheField = characterCacheClass.getDeclaredField("cache");
    cacheField.setAccessible(true);
    Character[] cache = (Character[]) cacheField.get(null);
    assertEquals(128, cache.length);
  }

  // ── 7. Constructor / accessors (guarded) ──────────────────────────

  private NonCachingTextRenderer tryConstruct() {
    try {
      return new NonCachingTextRenderer(TEST_FONT);
    } catch (Exception | Error e) {
      return null;
    }
  }

  @Test
  public void constructor_getFont_returnsSameFont() {
    Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
    assertSame(TEST_FONT, sharedRenderer.getFont());
  }

  @Test
  public void constructor_useVertexArrays_defaultsTrue() {
    Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
    assertTrue(sharedRenderer.getMyUseVertexArrays());
  }

  @Test
  public void constructor_smoothing_defaultsTrue() {
    Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
    assertTrue(sharedRenderer.getSmoothing());
  }

  @Test
  public void constructor_setUseVertexArrays_changes() {
    NonCachingTextRenderer renderer = tryConstruct();
    Assume.assumeTrue("Needs headless JOGL to construct", renderer != null);
    renderer.setUseVertexArrays(false);
    assertFalse(renderer.getMyUseVertexArrays());
  }

  @Test
  public void constructor_antialiased_storedCorrectly() throws Exception {
    Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
    Field f = NonCachingTextRenderer.class.getDeclaredField("antialiased");
    f.setAccessible(true);
    assertFalse("default constructor passes false for antialiased", f.getBoolean(sharedRenderer));
  }

  @Test
  public void constructor_renderDelegate_isDefaultWhenNull() throws Exception {
    Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
    Field f = NonCachingTextRenderer.class.getDeclaredField("renderDelegate");
    f.setAccessible(true);
    Object delegate = f.get(sharedRenderer);
    assertTrue("null renderDelegate should create DefaultRenderDelegate",
        delegate instanceof DefaultRenderDelegate);
  }

  @Test
  public void constructor_glyphProducer_isInitialized() throws Exception {
    Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
    Field f = NonCachingTextRenderer.class.getDeclaredField("mGlyphProducer");
    f.setAccessible(true);
    assertNotNull("GlyphProducer should be initialized", f.get(sharedRenderer));
  }
}
