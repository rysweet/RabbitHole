package edu.cmu.cs.dennisc.render.joglrenderer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.packrect.BackingStoreManager;
import com.jogamp.opengl.util.packrect.Rect;
import com.jogamp.opengl.util.packrect.RectVisitor;
import com.jogamp.opengl.util.packrect.RectanglePacker;
import com.jogamp.opengl.util.texture.TextureCoords;
import jogamp.opengl.Debug;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A near clone of its superclass, TextRenderer, for the sole purpose of setting DISABLE_GLYPH_CACHE to false.
 * Turning off glyph caching changes the behavior in GlyphProducer.getGlyphs().
 *
 * Doing this corrects the handling of RTL fonts such as Hebrew and Arabic.
 *
 * Code that was not copied includes unused constructors and the RenderDelegate interface.
 * More could be done to reduce the code here, but I am keeping it simple, if verbose, so it can be updated with
 * future jogl versions.
 *
 * Suppressing checkstyle to ease comparison with TextRenderer.
 */
@SuppressWarnings("CheckStyle")
public class NonCachingTextRenderer extends TextRenderer {
  private static final boolean DEBUG;

  static {
    Debug.initSingleton();
    DEBUG = PropertyAccess.isPropertyDefined("jogl.debug.NonCachingTextRenderer", true);
  }

  // These are occasionally useful for more in-depth debugging
  static final boolean DISABLE_GLYPH_CACHE = true;
  static final boolean DRAW_BBOXES = false;

  static final int kSize = 256;

  // Every certain number of render cycles, flush the strings which
  // haven't been used recently
  private static final int CYCLES_PER_FLUSH = 100;

  // The amount of vertical dead space on the backing store before we
  // force a compaction
  private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;
  static final int kQuadsPerBuffer = 100;
  static final int kCoordsPerVertVerts = 3;
  static final int kCoordsPerVertTex = 2;
  static final int kVertsPerQuad = 4;
  static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
  static final int kTotalBufferSizeCoordsVerts = kQuadsPerBuffer * kVertsPerQuad * kCoordsPerVertVerts;
  static final int kTotalBufferSizeCoordsTex = kQuadsPerBuffer * kVertsPerQuad * kCoordsPerVertTex;
  static final int kTotalBufferSizeBytesVerts = kTotalBufferSizeCoordsVerts * 4;
  static final int kTotalBufferSizeBytesTex = kTotalBufferSizeCoordsTex * 4;
  static final int kSizeInBytes_OneVertices_VertexData = kCoordsPerVertVerts * 4;
  static final int kSizeInBytes_OneVertices_TexData = kCoordsPerVertTex * 4;
  final Font font;
  private final boolean antialiased;
  private final boolean useFractionalMetrics;

  // Whether we're attempting to use automatic mipmap generation support
  private boolean mipmap;
  RectanglePacker packer;
  private boolean haveMaxSize;
  final TextRenderer.RenderDelegate renderDelegate;
  private TextureRenderer cachedBackingStore;
  private Graphics2D cachedGraphics;
  private FontRenderContext cachedFontRenderContext;
  private final Map<String, Rect> stringLocations = new HashMap<String, Rect>();
  private final TextRendererGlyphProducer mGlyphProducer;

  private int numRenderCycles;

  // Need to keep track of whether we're in a beginRendering() /
  // endRendering() cycle so we can re-enter the exact same state if
  // we have to reallocate the backing store
  private boolean inBeginEndPair;
  private boolean isOrthoMode;
  private int beginRenderingWidth;
  private int beginRenderingHeight;
  private boolean beginRenderingDepthTestDisabled;

  // For resetting the color after disposal of the old backing store
  private boolean haveCachedColor;
  private float cachedR;
  private float cachedG;
  private float cachedB;
  private float cachedA;
  private Color cachedColor;
  private boolean needToResetColor;

  // For debugging only
  private Frame dbgFrame;

  // Debugging purposes only
  private boolean debugged;
  TextRendererQuadRenderer mPipelinedQuadRenderer;

  //emzic: added boolean flag
  private boolean useVertexArrays = true;

  //emzic: added boolean flag
  boolean isExtensionAvailable_GL_VERSION_1_5;
  private boolean checkFor_isExtensionAvailable_GL_VERSION_1_5;

  // Whether GL_LINEAR filtering is enabled for the backing store
  private boolean smoothing = true;

  /** Creates a new TextRenderer with the given font, using no
   antialiasing or fractional metrics, and the default
   RenderDelegate. Equivalent to <code>TextRenderer(font, false,
   false)</code>.

   @param font the font to render with
   */
  public NonCachingTextRenderer(final Font font) {
    this(font, false, false, null, false);
  }

  /** Creates a new TextRenderer with the given Font, specified font
   properties, and given RenderDelegate. The
   <code>antialiased</code> and <code>useFractionalMetrics</code>
   flags provide control over the same properties at the Java 2D
   level. The <code>renderDelegate</code> provides more control
   over the text rendered. If <CODE>mipmap</CODE> is true, attempts
   to use OpenGL's automatic mipmap generation for better smoothing
   when rendering the TextureRenderer's contents at a distance.

   @param font the font to render with
   @param antialiased whether to use antialiased fonts
   @param useFractionalMetrics whether to use fractional font
   metrics at the Java 2D level
   @param renderDelegate the render delegate to use to draw the
   text's bitmap, or null to use the default one
   @param mipmap whether to attempt use of automatic mipmap generation
   */
  public NonCachingTextRenderer(final Font font, final boolean antialiased,
                                final boolean useFractionalMetrics, TextRenderer.RenderDelegate renderDelegate,
                                final boolean mipmap) {
    super(font, antialiased, useFractionalMetrics, renderDelegate, mipmap);
    this.font = font;
    this.antialiased = antialiased;
    this.useFractionalMetrics = useFractionalMetrics;
    this.mipmap = mipmap;

    // FIXME: consider adjusting the size based on font size
    // (it will already automatically resize if necessary)
    packer = new RectanglePacker(new NonCachingTextRenderer.Manager(), kSize, kSize);

    if (renderDelegate == null) {
      renderDelegate = new NonCachingTextRenderer.DefaultRenderDelegate();
    }

    this.renderDelegate = renderDelegate;

    mGlyphProducer = new TextRendererGlyphProducer(font.getNumGlyphs(), this);
  }

  /** Returns the bounding rectangle of the given String, assuming it
   was rendered at the origin. See {@link #getBounds(CharSequence)
  getBounds(CharSequence)}. */
  public Rectangle2D getBounds(final String str) {
    return getBounds((CharSequence) str);
  }

  /** Returns the bounding rectangle of the given CharSequence,
   assuming it was rendered at the origin. The coordinate system of
   the returned rectangle is Java 2D's, with increasing Y
   coordinates in the downward direction. The relative coordinate
   (0, 0) in the returned rectangle corresponds to the baseline of
   the leftmost character of the rendered string, in similar
   fashion to the results returned by, for example, {@link
  java.awt.font.GlyphVector#getVisualBounds}. Most applications
   will use only the width and height of the returned Rectangle for
   the purposes of centering or justifying the String. It is not
   specified which Java 2D bounds ({@link
  java.awt.font.GlyphVector#getVisualBounds getVisualBounds},
   {@link java.awt.font.GlyphVector#getPixelBounds getPixelBounds},
   etc.) the returned bounds correspond to, although every effort
   is made to ensure an accurate bound. */
  public Rectangle2D getBounds(final CharSequence str) {
    // FIXME: this should be more optimized and use the glyph cache
    final Rect r = stringLocations.get(str);

    if (r != null) {
      final NonCachingTextRenderer.TextData data = (NonCachingTextRenderer.TextData) r.getUserData();

      // Reconstitute the Java 2D results based on the cached values
      return new Rectangle2D.Double(-data.origin().x, -data.origin().y,
          r.w(), r.h());
    }

    // Must return a Rectangle compatible with the layout algorithm --
    // must be idempotent
    return normalize(renderDelegate.getBounds(str, font,
        getFontRenderContext()));
  }

  /** Returns the Font this renderer is using. */
  public Font getFont() {
    return font;
  }

  /** Returns a FontRenderContext which can be used for external
   text-related size computations. This object should be considered
   transient and may become invalidated between beginRendering
   endRendering pairs. */
  public FontRenderContext getFontRenderContext() {
    if (cachedFontRenderContext == null) {
      cachedFontRenderContext = getGraphics2D().getFontRenderContext();
    }

    return cachedFontRenderContext;
  }

  /** Begins rendering with this {@link TextRenderer TextRenderer}
   into the current OpenGL drawable, pushing the projection and
   modelview matrices and some state bits and setting up a
   two-dimensional orthographic projection with (0, 0) as the
   lower-left coordinate and (width, height) as the upper-right
   coordinate. Binds and enables the internal OpenGL texture
   object, sets the texture environment mode to GL_MODULATE, and
   changes the current color to the last color set with this
   TextRenderer via {@link #setColor setColor}. This method
   disables the depth test and is equivalent to
   beginRendering(width, height, true).

   @param width the width of the current on-screen OpenGL drawable
   @param height the height of the current on-screen OpenGL drawable
   @throws com.jogamp.opengl.GLException If an OpenGL context is not current when this method is called
   */
  public void beginRendering(final int width, final int height) throws GLException {
    beginRendering(width, height, true);
  }

  /** Begins rendering with this {@link TextRenderer TextRenderer}
   into the current OpenGL drawable, pushing the projection and
   modelview matrices and some state bits and setting up a
   two-dimensional orthographic projection with (0, 0) as the
   lower-left coordinate and (width, height) as the upper-right
   coordinate. Binds and enables the internal OpenGL texture
   object, sets the texture environment mode to GL_MODULATE, and
   changes the current color to the last color set with this
   TextRenderer via {@link #setColor setColor}. Disables the depth
   test if the disableDepthTest argument is true.

   @param width the width of the current on-screen OpenGL drawable
   @param height the height of the current on-screen OpenGL drawable
   @param disableDepthTest whether to disable the depth test
   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void beginRendering(final int width, final int height, final boolean disableDepthTest)
      throws GLException {
    beginRendering(true, width, height, disableDepthTest);
  }

  /** Begins rendering of 2D text in 3D with this {@link TextRenderer
  TextRenderer} into the current OpenGL drawable. Assumes the end
   user is responsible for setting up the modelview and projection
   matrices, and will render text using the {@link #draw3D draw3D}
   method. This method pushes some OpenGL state bits, binds and
   enables the internal OpenGL texture object, sets the texture
   environment mode to GL_MODULATE, and changes the current color
   to the last color set with this TextRenderer via {@link
  #setColor setColor}.

   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void begin3DRendering() throws GLException {
    beginRendering(false, 0, 0, false);
  }

  /** Changes the current color of this TextRenderer to the supplied
   one. The default color is opaque white.

   @param color the new color to use for rendering text
   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void setColor(final Color color) throws GLException {
    final boolean noNeedForFlush = (haveCachedColor && (cachedColor != null) &&
        color.equals(cachedColor));

    if (!noNeedForFlush) {
      flushGlyphPipeline();
    }

    getBackingStore().setColor(color);
    haveCachedColor = true;
    cachedColor = color;
  }

  /** Changes the current color of this TextRenderer to the supplied
   one, where each component ranges from 0.0f - 1.0f. The alpha
   component, if used, does not need to be premultiplied into the
   color channels as described in the documentation for {@link
  com.jogamp.opengl.util.texture.Texture Texture}, although
   premultiplied colors are used internally. The default color is
   opaque white.

   @param r the red component of the new color
   @param g the green component of the new color
   @param b the blue component of the new color
   @param a the alpha component of the new color, 0.0f = completely
   transparent, 1.0f = completely opaque
   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void setColor(final float r, final float g, final float b, final float a)
      throws GLException {
    final boolean noNeedForFlush = (haveCachedColor && (cachedColor == null) &&
        (r == cachedR) && (g == cachedG) && (b == cachedB) &&
        (a == cachedA));

    if (!noNeedForFlush) {
      flushGlyphPipeline();
    }

    getBackingStore().setColor(r, g, b, a);
    haveCachedColor = true;
    cachedR = r;
    cachedG = g;
    cachedB = b;
    cachedA = a;
    cachedColor = null;
  }

  /** Draws the supplied CharSequence at the desired location using
   the renderer's current color. The baseline of the leftmost
   character is at position (x, y) specified in OpenGL coordinates,
   where the origin is at the lower-left of the drawable and the Y
   coordinate increases in the upward direction.

   @param str the string to draw
   @param x the x coordinate at which to draw
   @param y the y coordinate at which to draw
   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void draw(final CharSequence str, final int x, final int y) throws GLException {
    draw3D(str, x, y, 0, 1);
  }

  /** Draws the supplied String at the desired location using the
   renderer's current color. See {@link #draw(CharSequence, int,
      int) draw(CharSequence, int, int)}. */
  public void draw(final String str, final int x, final int y) throws GLException {
    draw3D(str, x, y, 0, 1);
  }

  /** Draws the supplied CharSequence at the desired 3D location using
   the renderer's current color. The baseline of the leftmost
   character is placed at position (x, y, z) in the current
   coordinate system.

   @param str the string to draw
   @param x the x coordinate at which to draw
   @param y the y coordinate at which to draw
   @param z the z coordinate at which to draw
   @param scaleFactor a uniform scale factor applied to the width and height of the drawn rectangle
   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void draw3D(final CharSequence str, final float x, final float y, final float z,
                     final float scaleFactor) {
    internal_draw3D(str, x, y, z, scaleFactor);
  }

  /** Draws the supplied String at the desired 3D location using the
   renderer's current color. See {@link #draw3D(CharSequence,
      float, float, float, float) draw3D(CharSequence, float, float,
  float, float)}. */
  public void draw3D(final String str, final float x, final float y, final float z, final float scaleFactor) {
    internal_draw3D(str, x, y, z, scaleFactor);
  }

  /** Returns the pixel width of the given character. */
  public float getCharWidth(final char inChar) {
    return mGlyphProducer.getGlyphPixelWidth(inChar);
  }

  /** Causes the TextRenderer to flush any internal caches it may be
   maintaining and draw its rendering results to the screen. This
   should be called after each call to draw() if you are setting
   OpenGL state such as the modelview matrix between calls to
   draw(). */
  public void flush() {
    flushGlyphPipeline();
  }

  /** Ends a render cycle with this {@link TextRenderer TextRenderer}.
   Restores the projection and modelview matrices as well as
   several OpenGL state bits. Should be paired with beginRendering.

   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void endRendering() throws GLException {
    endRendering(true);
  }

  /** Ends a 3D render cycle with this {@link TextRenderer TextRenderer}.
   Restores several OpenGL state bits. Should be paired with {@link
  #begin3DRendering begin3DRendering}.

   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void end3DRendering() throws GLException {
    endRendering(false);
  }

  /** Disposes of all resources this TextRenderer is using. It is not
   valid to use the TextRenderer after this method is called.

   @throws GLException If an OpenGL context is not current when this method is called
   */
  public void dispose() throws GLException {
    if( null != mPipelinedQuadRenderer ) {
      mPipelinedQuadRenderer.dispose();
    }
    packer.dispose();
    packer = null;
    cachedBackingStore = null;
    cachedGraphics = null;
    cachedFontRenderContext = null;

    if (dbgFrame != null) {
      dbgFrame.dispose();
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  static Rectangle2D preNormalize(final Rectangle2D src) {
    // Need to round to integer coordinates
    // Also give ourselves a little slop around the reported
    // bounds of glyphs because it looks like neither the visual
    // nor the pixel bounds works perfectly well
    final int minX = (int) Math.floor(src.getMinX()) - 1;
    final int minY = (int) Math.floor(src.getMinY()) - 1;
    final int maxX = (int) Math.ceil(src.getMaxX()) + 1;
    final int maxY = (int) Math.ceil(src.getMaxY()) + 1;
    return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
  }


  Rectangle2D normalize(final Rectangle2D src) {
    // Give ourselves a boundary around each entity on the backing
    // store in order to prevent bleeding of nearby Strings due to
    // the fact that we use linear filtering

    // NOTE that this boundary is quite heuristic and is related
    // to how far away in 3D we may view the text --
    // heuristically, 1.5% of the font's height
    final int boundary = (int) Math.max(1, 0.015 * font.getSize());

    return new Rectangle2D.Double((int) Math.floor(src.getMinX() - boundary),
        (int) Math.floor(src.getMinY() - boundary),
        (int) Math.ceil(src.getWidth() + 2 * boundary),
        (int) Math.ceil(src.getHeight()) + 2 * boundary);
  }

  TextureRenderer getBackingStore() {
    final TextureRenderer renderer = (TextureRenderer) packer.getBackingStore();

    if (renderer != cachedBackingStore) {
      // Backing store changed since last time; discard any cached Graphics2D
      if (cachedGraphics != null) {
        cachedGraphics.dispose();
        cachedGraphics = null;
        cachedFontRenderContext = null;
      }

      cachedBackingStore = renderer;
    }

    return cachedBackingStore;
  }

  Graphics2D getGraphics2D() {
    final TextureRenderer renderer = getBackingStore();

    if (cachedGraphics == null) {
      cachedGraphics = renderer.createGraphics();

      // Set up composite, font and rendering hints
      cachedGraphics.setComposite(AlphaComposite.Src);
      cachedGraphics.setColor(Color.WHITE);
      cachedGraphics.setFont(font);
      cachedGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
          (antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
              : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
      cachedGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
          (useFractionalMetrics
              ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
              : RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
    }

    return cachedGraphics;
  }

  private void beginRendering(final boolean ortho, final int width, final int height,
                              final boolean disableDepthTestForOrtho) {
    final GL2 gl = GLContext.getCurrentGL().getGL2();

    if (DEBUG && !debugged) {
      debug(gl);
    }

    inBeginEndPair = true;
    isOrthoMode = ortho;
    beginRenderingWidth = width;
    beginRenderingHeight = height;
    beginRenderingDepthTestDisabled = disableDepthTestForOrtho;

    if (ortho) {
      getBackingStore().beginOrthoRendering(width, height,
          disableDepthTestForOrtho);
    } else {
      getBackingStore().begin3DRendering();
    }

    // Push client attrib bits used by the pipelined quad renderer
    gl.glPushClientAttrib((int) GL2.GL_ALL_CLIENT_ATTRIB_BITS);

    if (!haveMaxSize) {
      // Query OpenGL for the maximum texture size and set it in the
      // RectanglePacker to keep it from expanding too large
      final int[] sz = new int[1];
      gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, sz, 0);
      packer.setMaxSize(sz[0], sz[0]);
      haveMaxSize = true;
    }

    if (needToResetColor && haveCachedColor) {
      if (cachedColor == null) {
        getBackingStore().setColor(cachedR, cachedG, cachedB, cachedA);
      } else {
        getBackingStore().setColor(cachedColor);
      }

      needToResetColor = false;
    }

    // Disable future attempts to use mipmapping if TextureRenderer
    // doesn't support it
    if (mipmap && !getBackingStore().isUsingAutoMipmapGeneration()) {
      if (DEBUG) {
        System.err.println("Disabled mipmapping in TextRenderer");
      }

      mipmap = false;
    }
  }

  /**
   * emzic: here the call to glBindBuffer crashes on certain graphicscard/driver combinations
   * this is why the ugly try-catch block has been added, which falls back to the old textrenderer
   *
   * @param ortho
   * @throws GLException
   */
  private void endRendering(final boolean ortho) throws GLException {
    flushGlyphPipeline();

    inBeginEndPair = false;

    final GL2 gl = GLContext.getCurrentGL().getGL2();

    // Pop client attrib bits used by the pipelined quad renderer
    gl.glPopClientAttrib();

    // The OpenGL spec is unclear about whether this changes the
    // buffer bindings, so preemptively zero out the GL_ARRAY_BUFFER
    // binding
    if (getMyUseVertexArrays() && is15Available(gl)) {
      try {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      } catch (final Exception e) {
        isExtensionAvailable_GL_VERSION_1_5 = false;
      }
    }

    if (ortho) {
      getBackingStore().endOrthoRendering();
    } else {
      getBackingStore().end3DRendering();
    }

    if (++numRenderCycles >= CYCLES_PER_FLUSH) {
      numRenderCycles = 0;

      if (DEBUG) {
        System.err.println("Clearing unused entries in endRendering()");
      }

      clearUnusedEntries();
    }
  }

  private void clearUnusedEntries() {
    final java.util.List<Rect> deadRects = new ArrayList<Rect>();

    // Iterate through the contents of the backing store, removing
    // text strings that haven't been used recently
    packer.visit(new RectVisitor() {
      @Override
      public void visit(final Rect rect) {
        final NonCachingTextRenderer.TextData data = (NonCachingTextRenderer.TextData) rect.getUserData();

        if (data.used()) {
          data.clearUsed();
        } else {
          deadRects.add(rect);
        }
      }
    });

    for (final Rect r : deadRects) {
      packer.remove(r);
      stringLocations.remove(((NonCachingTextRenderer.TextData) r.getUserData()).string());

      final int unicodeToClearFromCache = ((NonCachingTextRenderer.TextData) r.getUserData()).unicodeID;

      if (unicodeToClearFromCache > 0) {
        mGlyphProducer.clearCacheEntry(unicodeToClearFromCache);
      }

      //      if (DEBUG) {
      //        Graphics2D g = getGraphics2D();
      //        g.setComposite(AlphaComposite.Clear);
      //        g.fillRect(r.x(), r.y(), r.w(), r.h());
      //        g.setComposite(AlphaComposite.Src);
      //      }
    }

    // If we removed dead rectangles this cycle, try to do a compaction
    final float frag = packer.verticalFragmentationRatio();

    if (!deadRects.isEmpty() && (frag > MAX_VERTICAL_FRAGMENTATION)) {
      if (DEBUG) {
        System.err.println(
            "Compacting TextRenderer backing store due to vertical fragmentation " +
                frag);
      }

      packer.compact();
    }

    if (DEBUG) {
      getBackingStore().markDirty(0, 0, getBackingStore().getWidth(),
          getBackingStore().getHeight());
    }
  }

  private void internal_draw3D(final CharSequence str, float x, final float y, final float z,
                               final float scaleFactor) {
    for (final TextRendererGlyph glyph : mGlyphProducer.getGlyphs(str)) {
      final float advance = glyph.draw3D(x, y, z, scaleFactor);
      x += advance * scaleFactor;
    }
  }

  private void flushGlyphPipeline() {
    if (mPipelinedQuadRenderer != null) {
      mPipelinedQuadRenderer.draw();
    }
  }

  void draw3D_ROBUST(final CharSequence str, final float x, final float y, final float z,
                             final float scaleFactor) {
    String curStr;
    if (str instanceof String string) {
      curStr = string;
    } else {
      curStr = str.toString();
    }

    // Look up the string on the backing store
    Rect rect = stringLocations.get(curStr);

    if (rect == null) {
      // Rasterize this string and place it on the backing store
      Graphics2D g = getGraphics2D();
      final Rectangle2D origBBox = preNormalize(renderDelegate.getBounds(curStr, font, getFontRenderContext()));
      final Rectangle2D bbox = normalize(origBBox);
      final Point origin = new Point((int) -bbox.getMinX(),
          (int) -bbox.getMinY());
      rect = new Rect(0, 0, (int) bbox.getWidth(),
          (int) bbox.getHeight(),
          new NonCachingTextRenderer.TextData(curStr, origin, origBBox, -1));

      packer.add(rect);
      stringLocations.put(curStr, rect);

      // Re-fetch the Graphics2D in case the addition of the rectangle
      // caused the old backing store to be thrown away
      g = getGraphics2D();

      // OK, should now have an (x, y) for this rectangle; rasterize
      // the String
      final int strx = rect.x() + origin.x;
      final int stry = rect.y() + origin.y;

      // Clear out the area we're going to draw into
      g.setComposite(AlphaComposite.Clear);
      g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
      g.setComposite(AlphaComposite.Src);

      // Draw the string
      renderDelegate.draw(g, curStr, strx, stry);

      if (DRAW_BBOXES) {
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
      getBackingStore().markDirty(rect.x(), rect.y(), rect.w(),
          rect.h());
    }

    // OK, now draw the portion of the backing store to the screen
    final TextureRenderer renderer = getBackingStore();

    // NOTE that the rectangles managed by the packer have their
    // origin at the upper-left but the TextureRenderer's origin is
    // at its lower left!!!
    final NonCachingTextRenderer.TextData data = (NonCachingTextRenderer.TextData) rect.getUserData();
    data.markUsed();

    final Rectangle2D origRect = data.origRect();

    // Align the leftmost point of the baseline to the (x, y, z) coordinate requested
    renderer.draw3DRect(x - (scaleFactor * data.origOriginX()),
        y - (scaleFactor * ((float) origRect.getHeight() - data.origOriginY())), z,
        rect.x() + (data.origin().x - data.origOriginX()),
        renderer.getHeight() - rect.y() - (int) origRect.getHeight() -
            (data.origin().y - data.origOriginY()),
        (int) origRect.getWidth(), (int) origRect.getHeight(), scaleFactor);
  }

  //----------------------------------------------------------------------
  // Debugging functionality
  //
  private void debug(final GL gl) {
    dbgFrame = new Frame("TextRenderer Debug Output");

    final GLCanvas dbgCanvas = new GLCanvas(new GLCapabilities(gl.getGLProfile()));
    dbgCanvas.setSharedContext(GLContext.getCurrent());
    dbgCanvas.addGLEventListener(new NonCachingTextRenderer.DebugListener(gl, dbgFrame));
    dbgFrame.add(dbgCanvas);

    final FPSAnimator anim = new FPSAnimator(dbgCanvas, 10);
    dbgFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        // Run this on another thread than the AWT event queue to
        // make sure the call to Animator.stop() completes before
        // exiting
        new InterruptSource.Thread(null, new Runnable() {
          @Override
          public void run() {
            anim.stop();
          }
        }).start();
      }
    });
    dbgFrame.setSize(kSize, kSize);
    dbgFrame.setVisible(true);
    anim.start();
    debugged = true;
  }

  static class CharSequenceIterator implements CharacterIterator {
    CharSequence mSequence;
    int mLength;
    int mCurrentIndex;

    CharSequenceIterator() {
    }

    CharSequenceIterator(final CharSequence sequence) {
      initFromCharSequence(sequence);
    }

    public void initFromCharSequence(final CharSequence sequence) {
      mSequence = sequence;
      mLength = mSequence.length();
      mCurrentIndex = 0;
    }

    @Override
    public char last() {
      mCurrentIndex = Math.max(0, mLength - 1);

      return current();
    }

    @Override
    public char current() {
      if ((mLength == 0) || (mCurrentIndex >= mLength)) {
        return CharacterIterator.DONE;
      }

      return mSequence.charAt(mCurrentIndex);
    }

    @Override
    public char next() {
      mCurrentIndex++;

      return current();
    }

    @Override
    public char previous() {
      mCurrentIndex = Math.max(mCurrentIndex - 1, 0);

      return current();
    }

    @Override
    public char setIndex(final int position) {
      mCurrentIndex = position;

      return current();
    }

    @Override
    public int getBeginIndex() {
      return 0;
    }

    @Override
    public int getEndIndex() {
      return mLength;
    }

    @Override
    public int getIndex() {
      return mCurrentIndex;
    }

    @Override
    public Object clone() {
      final NonCachingTextRenderer.CharSequenceIterator iter = new NonCachingTextRenderer.CharSequenceIterator(mSequence);
      iter.mCurrentIndex = mCurrentIndex;

      return iter;
    }

    @Override
    public char first() {
      if (mLength == 0) {
        return CharacterIterator.DONE;
      }

      mCurrentIndex = 0;

      return current();
    }
  }

  // Data associated with each rectangle of text
  static class TextData {
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

  class Manager implements BackingStoreManager {
    private Graphics2D g;

    @Override
    public Object allocateBackingStore(final int w, final int h) {
      // FIXME: should consider checking Font's attributes to see
      // whether we're likely to need to support a full RGBA backing
      // store (i.e., non-default Paint, foreground color, etc.), but
      // for now, let's just be more efficient
      TextureRenderer renderer;

      if (renderDelegate.intensityOnly()) {
        renderer = TextureRenderer.createAlphaOnlyRenderer(w, h, mipmap);
      } else {
        renderer = new TextureRenderer(w, h, true, mipmap);
      }
      renderer.setSmoothing(smoothing);

      if (DEBUG) {
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
        if (DEBUG) {
          System.err.println(
              "Clearing unused entries in preExpand(): attempt number " +
                  attemptNumber);
        }

        if (inBeginEndPair) {
          // Draw any outstanding glyphs
          flush();
        }

        clearUnusedEntries();

        return true;
      }

      return false;
    }

    @Override
    public boolean additionFailed(final Rect cause, final int attemptNumber) {
      // Heavy hammer -- might consider doing something different
      packer.clear();
      stringLocations.clear();
      mGlyphProducer.clearAllCacheEntries();

      if (DEBUG) {
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
      if (inBeginEndPair) {
        // Draw any outstanding glyphs
        flush();

        final GL2 gl = GLContext.getCurrentGL().getGL2();

        // Pop client attrib bits used by the pipelined quad renderer
        gl.glPopClientAttrib();

        // The OpenGL spec is unclear about whether this changes the
        // buffer bindings, so preemptively zero out the GL_ARRAY_BUFFER
        // binding
        if (getMyUseVertexArrays() && is15Available(gl)) {
          try {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
          } catch (final Exception e) {
            isExtensionAvailable_GL_VERSION_1_5 = false;
          }
        }

        if (isOrthoMode) {
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
      if (inBeginEndPair) {
        if (isOrthoMode) {
          ((TextureRenderer) newBackingStore).beginOrthoRendering(beginRenderingWidth,
              beginRenderingHeight, beginRenderingDepthTestDisabled);
        } else {
          ((TextureRenderer) newBackingStore).begin3DRendering();
        }

        // Push client attrib bits used by the pipelined quad renderer
        final GL2 gl = GLContext.getCurrentGL().getGL2();
        gl.glPushClientAttrib((int) GL2.GL_ALL_CLIENT_ATTRIB_BITS);

        if (haveCachedColor) {
          if (cachedColor == null) {
            ((TextureRenderer) newBackingStore).setColor(cachedR,
                cachedG, cachedB, cachedA);
          } else {
            ((TextureRenderer) newBackingStore).setColor(cachedColor);
          }
        }
      } else {
        needToResetColor = true;
      }
    }
  }

  public static class DefaultRenderDelegate implements TextRenderer.RenderDelegate {
    @Override
    public boolean intensityOnly() {
      return true;
    }

    @Override
    public Rectangle2D getBounds(final CharSequence str, final Font font,
                                 final FontRenderContext frc) {
      return getBounds(font.createGlyphVector(frc,
              new NonCachingTextRenderer.CharSequenceIterator(str)),
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

  //----------------------------------------------------------------------
  // Glyph-by-glyph rendering support
  //

  // A temporary to prevent excessive garbage creation
  final char[] singleUnicode = new char[1];



  static class CharacterCache {
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
        return NonCachingTextRenderer.CharacterCache.cache[c];
      }
      return Character.valueOf(c);
    }
  }


  class DebugListener implements GLEventListener {
    private GLU glu;
    private Frame frame;

    DebugListener(final GL gl, final Frame frame) {
      this.glu = GLU.createGLU(gl);
      this.frame = frame;
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
      final GL2 gl = GLContext.getCurrentGL().getGL2();
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

      if (packer == null) {
        return;
      }

      final TextureRenderer rend = getBackingStore();
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
      mPipelinedQuadRenderer.dispose();
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

  /**
   * Sets whether vertex arrays are being used internally for
   * rendering, or whether text is rendered using the OpenGL
   * immediate mode commands. This is provided as a concession for
   * certain graphics cards which have poor vertex array
   * performance. Defaults to true.
   */
  public void setUseVertexArrays(final boolean useVertexArrays) {
    this.useVertexArrays = useVertexArrays;
  }

  /**
   * Indicates whether vertex arrays are being used internally for
   * rendering, or whether text is rendered using the OpenGL
   * immediate mode commands. Defaults to true.
   */
  public final boolean getMyUseVertexArrays() {
    return useVertexArrays;
  }

  /**
   * Sets whether smoothing (i.e., GL_LINEAR filtering) is enabled
   * in the backing TextureRenderer of this NonCachingTextRenderer. A few
   * graphics cards do not behave well when this is enabled,
   * resulting in fuzzy text. Defaults to true.
   */
  public void setSmoothing(final boolean smoothing) {
    this.smoothing = smoothing;
    getBackingStore().setSmoothing(smoothing);
  }

  /**
   * Indicates whether smoothing is enabled in the backing
   * TextureRenderer of this NonCachingTextRenderer. A few graphics cards do
   * not behave well when this is enabled, resulting in fuzzy text.
   * Defaults to true.
   */
  public boolean getSmoothing() {
    return smoothing;
  }

  final boolean is15Available(final GL gl) {
    if (!checkFor_isExtensionAvailable_GL_VERSION_1_5) {
      isExtensionAvailable_GL_VERSION_1_5 = gl.isExtensionAvailable(GLExtensions.VERSION_1_5);
      checkFor_isExtensionAvailable_GL_VERSION_1_5 = true;
    }
    return isExtensionAvailable_GL_VERSION_1_5;
  }
}
