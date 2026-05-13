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

import edu.cmu.cs.dennisc.math.SineCosineCache;

import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_LINE_LOOP;
import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_TRIANGLE_FAN;
import static com.jogamp.opengl.GL2.GL_POLYGON;

/**
 * Delegate for primitive shape drawing: lines, rects, ovals, polygons.
 * Extracted from Graphics2D to reduce class size.
 */
/*package-private*/ final class GlPrimitiveShapeRenderer {
  private static SineCosineCache s_sineCosineCache = new SineCosineCache(8);

  private final Graphics2D graphics2D;

  GlPrimitiveShapeRenderer(Graphics2D graphics2D) {
    assert graphics2D != null;
    this.graphics2D = graphics2D;
  }

  void drawLine(int x1, int y1, int x2, int y2) {
    graphics2D.renderContext.gl.glBegin(GL_LINES);
    graphics2D.renderContext.gl.glVertex2i(x1, y1);
    graphics2D.renderContext.gl.glVertex2i(x2, y2);
    graphics2D.renderContext.gl.glEnd();
  }

  void fillRect(int x, int y, int width, int height) {
    graphics2D.renderContext.gl.glBegin(GL_POLYGON);
    graphics2D.renderContext.gl.glVertex2i(x, y);
    graphics2D.renderContext.gl.glVertex2i(x + width, y);
    graphics2D.renderContext.gl.glVertex2i(x + width, y + height);
    graphics2D.renderContext.gl.glVertex2i(x, y + height);
    graphics2D.renderContext.gl.glEnd();
  }

  void clearRect(int x, int y, int width, int height) {
    graphics2D.glSetColor(graphics2D.getBackgroundField());
    fillRect(x, y, width, height);
    graphics2D.glSetPaint(graphics2D.getPaintField());
  }

  private void glQuarterOval(double centerX, double centerY, double radiusX, double radiusY, int quadrant) {
    int n = s_sineCosineCache.cosines.length;
    int max = n - 1;
    for (int lcv = 0; lcv < n; lcv++) {
      int i = max - lcv;
      double cos = s_sineCosineCache.getCosine(quadrant, i);
      double sin = s_sineCosineCache.getSine(quadrant, i);
      graphics2D.renderContext.gl.glVertex2d(centerX + (cos * radiusX), centerY + (sin * radiusY));
    }
  }

  private void glRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    int x1 = x + arcWidth;
    int x2 = (x + width) - arcWidth;
    int y1 = y + arcHeight;
    int y2 = (y + height) - arcHeight;

    glQuarterOval(x1, y2, arcWidth, arcHeight, 1);
    glQuarterOval(x2, y2, arcWidth, arcHeight, 0);
    glQuarterOval(x2, y1, arcWidth, arcHeight, 3);
    glQuarterOval(x1, y1, arcWidth, arcHeight, 2);
  }

  void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    graphics2D.renderContext.gl.glBegin(GL_LINE_LOOP);
    glRoundRect(x, y, width, height, arcWidth, arcHeight);
    graphics2D.renderContext.gl.glEnd();
  }

  void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    graphics2D.renderContext.gl.glBegin(GL_TRIANGLE_FAN);
    glRoundRect(x, y, width, height, arcWidth, arcHeight);
    graphics2D.renderContext.gl.glEnd();
  }

  private void glOval(int x, int y, int width, int height) {
    double radiusX = width * 0.5;
    double radiusY = height * 0.5;
    double centerX = x + radiusX;
    double centerY = y + radiusY;
    glQuarterOval(centerX, centerY, radiusX, radiusY, 3);
    glQuarterOval(centerX, centerY, radiusX, radiusY, 2);
    glQuarterOval(centerX, centerY, radiusX, radiusY, 1);
    glQuarterOval(centerX, centerY, radiusX, radiusY, 0);
  }

  void drawOval(int x, int y, int width, int height) {
    graphics2D.renderContext.gl.glBegin(GL_LINE_LOOP);
    glOval(x, y, width, height);
    graphics2D.renderContext.gl.glEnd();
  }

  void fillOval(int x, int y, int width, int height) {
    graphics2D.renderContext.gl.glBegin(GL_TRIANGLE_FAN);
    glOval(x, y, width, height);
    graphics2D.renderContext.gl.glEnd();
  }

  private void glPoly(int xPoints[], int yPoints[], int nPoints) {
    for (int i = 0; i < nPoints; i++) {
      graphics2D.renderContext.gl.glVertex2i(xPoints[i], yPoints[i]);
    }
  }

  void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
    graphics2D.renderContext.gl.glBegin(GL_LINE_STRIP);
    glPoly(xPoints, yPoints, nPoints);
    graphics2D.renderContext.gl.glEnd();
  }

  void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
    graphics2D.renderContext.gl.glBegin(GL_LINE_LOOP);
    glPoly(xPoints, yPoints, nPoints);
    graphics2D.renderContext.gl.glEnd();
  }

  void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
    graphics2D.renderContext.gl.glBegin(GL_POLYGON);
    glPoly(xPoints, yPoints, nPoints);
    graphics2D.renderContext.gl.glEnd();
  }
}
