/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package edu.cmu.cs.dennisc.render.gl.imp;

import com.jogamp.opengl.GL;
import edu.cmu.cs.dennisc.image.ImageGenerator;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.nio.DoubleBuffer;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES1.GL_ALPHA_SCALE;
import static com.jogamp.opengl.GL2GL3.GL_FILL;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/**
 * @author Dennis Cosgrove
 */
/*package-private*/class Graphics2D extends edu.cmu.cs.dennisc.render.Graphics2D {
  private static final Paint DEFAULT_PAINT = Color.BLACK;
  private static final Color DEFAULT_BACKGROUND = Color.WHITE;
  private static final Font DEFAULT_FONT = new Font(null, Font.PLAIN, 12);
  private static final Stroke DEFAULT_STROKE = new BasicStroke(1);

  RenderContext renderContext;
  private Paint paint = DEFAULT_PAINT;
  private Color background = DEFAULT_BACKGROUND;
  private Font font = DEFAULT_FONT;
  private Stroke stroke = DEFAULT_STROKE;
  private RenderingHints renderingHints;
  private AffineTransform affineTransform = new AffineTransform();
  private double[] glTransform = new double[16];
  private DoubleBuffer glTransformBuffer = DoubleBuffer.wrap(glTransform);
  private int width = -1;
  private int height = -1;

  private final GlPrimitiveShapeRenderer primitiveRenderer;
  private final GlTessellationRenderer tessellationRenderer;
  private final GlTextRenderer textRenderer;
  private final GlImageRenderer imageRenderer;

  public Graphics2D(RenderContext renderContext) {
    assert renderContext != null;
    this.renderContext = renderContext;
    this.primitiveRenderer = new GlPrimitiveShapeRenderer(this);
    this.tessellationRenderer = new GlTessellationRenderer(this);
    this.textRenderer = new GlTextRenderer(this);
    this.imageRenderer = new GlImageRenderer(this);
    Map<RenderingHints.Key, Object> map = new HashMap<RenderingHints.Key, Object>();
    map.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
    map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    map.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_DEFAULT);
    map.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DEFAULT);
    map.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
    //todo: investigate
    //map.put( java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_DEFAULT );
    map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
    map.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
    Object antialiasTextValue;
    if (Boolean.getBoolean("swing.aatext")) {
      antialiasTextValue = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
    } else {
      antialiasTextValue = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
    }
    map.put(RenderingHints.KEY_TEXT_ANTIALIASING, antialiasTextValue);
    this.setRenderingHints(map);
  }

  public void initialize(Dimension surfaceSize) {
    assert this.renderContext.gl != null;
    this.width = surfaceSize.width;
    this.height = surfaceSize.height;
    setPaint(DEFAULT_PAINT);
    setBackground(DEFAULT_BACKGROUND);
    setFont(DEFAULT_FONT);
    this.renderContext.gl.glMatrixMode(GL_PROJECTION);
    this.renderContext.gl.glPushMatrix();
    this.renderContext.gl.glLoadIdentity();
    this.renderContext.gl.glOrtho(0, this.width - 1, this.height - 1, 0, -1, 1);
    this.renderContext.gl.glMatrixMode(GL_MODELVIEW);
    this.renderContext.gl.glPushMatrix();
    this.renderContext.gl.glLoadIdentity();
    this.affineTransform.setToIdentity();
    this.renderContext.gl.glDisable(GL_DEPTH_TEST);
    this.renderContext.gl.glDisable(GL_LIGHTING);
    this.renderContext.gl.glDisable(GL_CULL_FACE);
    this.renderContext.setDiffuseColorTextureAdapter(null, false);
    this.renderContext.setBumpTextureAdapter(null);
    this.renderContext.gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
  }

  // Package-private accessors for delegates

  int getWidth() { return this.width; }
  int getHeight() { return this.height; }
  AffineTransform getAffineTransformRef() { return this.affineTransform; }
  Font getFontField() { return this.font; }
  Paint getPaintField() { return this.paint; }
  Color getBackgroundField() { return this.background; }
  Stroke getStrokeField() { return this.stroke; }

  // java.awt.Graphics

  @Override
  public void dispose() {
    if (isValid()) {
      this.renderContext.gl.glFlush();
      this.renderContext.gl.glMatrixMode(GL_MODELVIEW);
      this.renderContext.gl.glPopMatrix();
      this.renderContext.gl.glMatrixMode(GL_PROJECTION);
      this.renderContext.gl.glPopMatrix();
      this.width = -1;
      this.height = -1;
    }
  }

  @Override
  public boolean isValid() {
    return (this.width != -1) && (this.height != -1);
  }

  @Override
  public Graphics create() { throw new RuntimeException("not implemented"); }
  @Override
  public Color getColor() {
    if (this.paint instanceof Color color) {
      return color;
    } else {
      throw new RuntimeException("use getPaint()");
    }
  }
  @Override
  public void setColor(Color color) { setPaint(color); }
  @Override
  public void setPaintMode() { throw new RuntimeException("not implemented"); }
  @Override
  public void setXORMode(Color c1) { throw new RuntimeException("not implemented"); }
  @Override
  public Font getFont() { return this.font; }
  @Override
  public void setFont(Font font) { this.font = font; }
  @Override
  public FontMetrics getFontMetrics(Font f) { return Toolkit.getDefaultToolkit().getFontMetrics(f); }
  @Override
  public Rectangle getClipBounds() { throw new RuntimeException("not implemented"); }
  @Override
  public void clipRect(int x, int y, int width, int height) { throw new RuntimeException("not implemented"); }
  @Override
  public void setClip(int x, int y, int width, int height) { throw new RuntimeException("not implemented"); }
  @Override
  public Shape getClip() { throw new RuntimeException("not implemented"); }
  @Override
  public void setClip(Shape clip) { throw new RuntimeException("not implemented"); }
  @Override
  public void copyArea(int x, int y, int width, int height, int dx, int dy) { throw new RuntimeException("not implemented"); }

  // Primitive shape methods — delegated to GlPrimitiveShapeRenderer

  @Override
  public void drawLine(int x1, int y1, int x2, int y2) { primitiveRenderer.drawLine(x1, y1, x2, y2); }
  @Override
  public void fillRect(int x, int y, int width, int height) { primitiveRenderer.fillRect(x, y, width, height); }
  @Override
  public void clearRect(int x, int y, int width, int height) { primitiveRenderer.clearRect(x, y, width, height); }
  @Override
  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { primitiveRenderer.drawRoundRect(x, y, width, height, arcWidth, arcHeight); }
  @Override
  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { primitiveRenderer.fillRoundRect(x, y, width, height, arcWidth, arcHeight); }
  @Override
  public void drawOval(int x, int y, int width, int height) { primitiveRenderer.drawOval(x, y, width, height); }
  @Override
  public void fillOval(int x, int y, int width, int height) { primitiveRenderer.fillOval(x, y, width, height); }
  @Override
  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) { throw new RuntimeException("not implemented"); }
  @Override
  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) { throw new RuntimeException("not implemented"); }
  @Override
  public void drawPolyline(int xPoints[], int yPoints[], int nPoints) { primitiveRenderer.drawPolyline(xPoints, yPoints, nPoints); }
  @Override
  public void drawPolygon(int xPoints[], int yPoints[], int nPoints) { primitiveRenderer.drawPolygon(xPoints, yPoints, nPoints); }
  @Override
  public void fillPolygon(int xPoints[], int yPoints[], int nPoints) { primitiveRenderer.fillPolygon(xPoints, yPoints, nPoints); }

  // String/char/byte drawing

  @Override
  public void drawString(String str, int x, int y) { drawString(str, (float) x, (float) y); }
  @Override
  public void drawString(AttributedCharacterIterator iterator, int x, int y) { throw new RuntimeException("not implemented"); }
  @Override
  public void drawChars(char[] data, int offset, int length, int x, int y) { drawString(new String(data, offset, length), x, y); }
  @Override
  public void drawBytes(byte[] data, int offset, int length, int x, int y) { drawString(new String(data, offset, length), x, y); }

  // Image drawing — bridging method stays here, lifecycle delegated to GlImageRenderer

  @Override
  public boolean drawImage(Image image, int x, int y, ImageObserver observer) {
    boolean wasRemembered = imageRenderer.isRemembered(image);
    if (!wasRemembered) {
      imageRenderer.remember(image);
    }
    try {
      imageRenderer.paint(imageRenderer.getImageGenerator(image), x, y, 1.0f);
    } finally {
      if (!wasRemembered) {
        imageRenderer.forget(image);
      }
    }
    return true;
  }

  @Override
  public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver observer) { throw new RuntimeException("not implemented"); }
  @Override
  public boolean drawImage(Image image, int x, int y, Color bgcolor, ImageObserver observer) { throw new RuntimeException("not implemented"); }
  @Override
  public boolean drawImage(Image image, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) { throw new RuntimeException("not implemented"); }
  @Override
  public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) { throw new RuntimeException("not implemented"); }
  @Override
  public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) { throw new RuntimeException("not implemented"); }

  // java.awt.Graphics2D

  @Override
  public void draw3DRect(int x, int y, int width, int height, boolean raised) { throw new RuntimeException("not implemented"); }
  @Override
  public void fill3DRect(int x, int y, int width, int height, boolean raised) { throw new RuntimeException("not implemented"); }
  @Override
  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) { throw new RuntimeException("not implemented"); }
  @Override
  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) { throw new RuntimeException("not implemented"); }
  @Override
  public void drawRenderedImage(RenderedImage img, AffineTransform xform) { throw new RuntimeException("not implemented"); }
  @Override
  public void drawRenderableImage(RenderableImage img, AffineTransform xform) { throw new RuntimeException("not implemented"); }

  //  @Override
  //  public void drawString( String str, int x, int y ) {
  //    throw new RuntimeException( "not implemented" );
  //  }

  @Override
  public void drawString(String text, float x, float y) { textRenderer.drawString(text, x, y); }

  //  @Override
  //  public void drawString( java.text.AttributedCharacterIterator iterator, int x, int y ) {
  //    throw new RuntimeException( "not implemented" );
  //  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    throw new RuntimeException("todo: use drawString( String, float, float ) for now");
  }

  @Override
  public void drawGlyphVector(GlyphVector g, float x, float y) {
    int n = g.getNumGlyphs();
    this.translate(x, y);
    for (int i = 0; i < n; i++) {
      Shape shapeI = g.getGlyphOutline(i);
      this.fill(shapeI);
    }
    this.translate(-x, -y);
  }

  // Tessellation — delegated to GlTessellationRenderer

  @Override
  public void draw(Shape s) { tessellationRenderer.draw(s); }
  @Override
  public void fill(Shape s) { tessellationRenderer.fill(s); }

  @Override
  public boolean hit(Rectangle rect, Shape s, boolean onStroke) { throw new RuntimeException("not implemented"); }
  @Override
  public GraphicsConfiguration getDeviceConfiguration() { throw new RuntimeException("not implemented"); }
  @Override
  public Composite getComposite() { throw new RuntimeException("not implemented"); }
  @Override
  public void setComposite(Composite comp) { throw new RuntimeException("not implemented"); }

  @Override
  public Color getBackground() { return this.background; }
  @Override
  public void setBackground(Color color) { this.background = color; }

  void glSetColor(Color color) {
    assert color != null;
    this.renderContext.gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
    if (color.getAlpha() != 255) {
      this.renderContext.gl.glEnable(GL_BLEND);
      this.renderContext.gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
      this.renderContext.gl.glPixelTransferf(GL_ALPHA_SCALE, color.getAlpha() / 255.0f);
    } else {
      this.renderContext.gl.glDisable(GL_BLEND);
      this.renderContext.gl.glPixelTransferf(GL_ALPHA_SCALE, 1.0f);
    }
  }

  void glSetPaint(Paint paint) {
    if (paint instanceof Color color) {
      glSetColor(color);
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  @Override
  public Paint getPaint() { return this.paint; }

  @Override
  public void setPaint(Paint paint) {
    if (paint instanceof Color color) {
      glSetColor(color);
      this.paint = paint;
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  @Override
  public Stroke getStroke() { return this.stroke; }
  @Override
  public void setStroke(Stroke stroke) { this.stroke = stroke; }
  @Override
  public Object getRenderingHint(RenderingHints.Key hintKey) { return this.renderingHints.get(hintKey); }
  @Override
  public RenderingHints getRenderingHints() { return this.renderingHints; }
  @Override
  public void addRenderingHints(Map<?, ?> hints) { this.renderingHints.add(new RenderingHints((Map<RenderingHints.Key, ?>) hints)); }
  @Override
  public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) { this.renderingHints.put(hintKey, hintValue); }
  @Override
  public void setRenderingHints(Map<?, ?> hints) { this.renderingHints = new RenderingHints((Map<RenderingHints.Key, ?>) hints); }

  private final double[] s_matrix = new double[6];

  private void glUpdateTransform() {
    this.affineTransform.getMatrix(s_matrix);
    if (s_matrix[4] != this.affineTransform.getTranslateX()) {
      System.err.println("WARNING: translate x: " + s_matrix[4] + " != " + this.affineTransform.getTranslateX());
    }
    if (s_matrix[5] != this.affineTransform.getTranslateY()) {
      System.err.println("WARNING: translate y: " + s_matrix[5] + " != " + this.affineTransform.getTranslateY());
    }
    this.glTransform[0] = s_matrix[0];
    this.glTransform[4] = s_matrix[2];
    this.glTransform[8] = 0;
    this.glTransform[12] = s_matrix[4];
    this.glTransform[1] = s_matrix[1];
    this.glTransform[5] = s_matrix[3];
    this.glTransform[9] = 0;
    this.glTransform[13] = s_matrix[5];
    this.glTransform[2] = 0;
    this.glTransform[6] = 0;
    this.glTransform[10] = 1;
    this.glTransform[14] = 0;
    this.glTransform[3] = 0;
    this.glTransform[7] = 0;
    this.glTransform[11] = 0;
    this.glTransform[15] = 1;
    this.renderContext.gl.glLoadMatrixd(this.glTransformBuffer);
  }

  @Override
  public void translate(int x, int y) { this.affineTransform.translate(x, y); glUpdateTransform(); }
  @Override
  public void translate(double x, double y) { this.affineTransform.translate(x, y); glUpdateTransform(); }
  @Override
  public void rotate(double theta) { this.affineTransform.rotate(theta); glUpdateTransform(); }
  @Override
  public void rotate(double theta, double x, double y) { this.affineTransform.rotate(theta, x, y); glUpdateTransform(); }
  @Override
  public void scale(double sx, double sy) { this.affineTransform.scale(sx, sy); glUpdateTransform(); }
  @Override
  public void shear(double shx, double shy) { this.affineTransform.shear(shx, shy); glUpdateTransform(); }
  @Override
  public void transform(AffineTransform Tx) { this.affineTransform.concatenate(Tx); glUpdateTransform(); }
  @Override
  public AffineTransform getTransform() { return new AffineTransform(this.affineTransform); }
  @Override
  public void setTransform(AffineTransform Tx) { this.affineTransform.setTransform(Tx); glUpdateTransform(); }
  @Override
  public void clip(Shape s) { throw new RuntimeException("not implemented"); }

  @Override
  public FontRenderContext getFontRenderContext() {
    boolean isAntiAliased = getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING) == RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
    boolean usesFractionalMetrics = getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS) == RenderingHints.VALUE_FRACTIONALMETRICS_ON;
    return new FontRenderContext(getTransform(), isAntiAliased, usesFractionalMetrics);
  }

  // edu.cmu.cs.dennisc.render.Graphics2D — font lifecycle delegated to GlTextRenderer

  @Override
  public boolean isRemembered(Font font) { return textRenderer.isRemembered(font); }
  @Override
  public void remember(Font font) { textRenderer.remember(font); }
  @Override
  public Rectangle2D getBounds(String text, Font font) { return textRenderer.getBounds(text, font); }
  @Override
  public void forget(Font font) { textRenderer.forget(font); }
  @Override
  public void disposeForgottenFonts() { textRenderer.disposeForgottenFonts(); }

  // edu.cmu.cs.dennisc.render.Graphics2D — image lifecycle delegated to GlImageRenderer

  @Override
  public boolean isRemembered(ImageGenerator imageGenerator) { return imageRenderer.isRemembered(imageGenerator); }
  @Override
  public void remember(ImageGenerator imageGenerator) { imageRenderer.remember(imageGenerator); }
  @Override
  public void paint(ImageGenerator imageGenerator, float x, float y, float alpha) { imageRenderer.paint(imageGenerator, x, y, alpha); }
  @Override
  public void forget(ImageGenerator imageGenerator) { imageRenderer.forget(imageGenerator); }

  // Bug preserved: disposes image generators but clears font map instead of image map (L1137)
  @Override
  public void disposeForgottenImageGenerators() {
    imageRenderer.disposeForgottenImageGenerators();
    textRenderer.clearForgottenMap();
  }

  @Override
  public boolean isRemembered(Image image) { return imageRenderer.isRemembered(image); }
  @Override
  public void remember(Image image) { imageRenderer.remember(image); }
  @Override
  public void forget(Image image) { imageRenderer.forget(image); }
  @Override
  public void disposeForgottenImages() { imageRenderer.disposeForgottenImages(); }

  public GL getGL() { return this.renderContext.gl; }
}
