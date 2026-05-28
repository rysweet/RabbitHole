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
package org.alice.ide.clipboard.icons;

import org.alice.ide.clipboard.DragReceptorState;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * This class has been automatically generated using svg2java
 *
 */
public class ClipboardIcon implements Icon {

  private float origAlpha = 1.0f;
  private final ClipboardBoardRenderer boardRenderer = new ClipboardBoardRenderer();
  private final ClipboardPaperRenderer paperRenderer = new ClipboardPaperRenderer();
  private final ClipboardClipRenderer clipRenderer = new ClipboardClipRenderer();

  /**
   * Paints the transcoded SVG image on the specified graphics context. You
   * can install a custom transformation on the graphics context to scale the
   * image.
   *
   * @param g Graphics context.
   */
  public void paint(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    origAlpha = 1.0f;
    Composite origComposite = g.getComposite();
    if (origComposite instanceof AlphaComposite origAlphaComposite) {
      if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
        origAlpha = origAlphaComposite.getAlpha();
      }
    }

    // _0
    AffineTransform trans_0 = g.getTransform();
    paintRootGraphicsNode_0(g);
    g.setTransform(trans_0);

  }

  private void paintCompositeGraphicsNode_0_0_2_0(Graphics2D g) {
    // _0_0_2_0_0
    AffineTransform trans_0_0_2_0_0 = g.getTransform();
    g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -167.00001525878906f, -3.0f));
    boardRenderer.paintComposite(g, origAlpha, this.dragReceptorState);
    g.setTransform(trans_0_0_2_0_0);
    if (this.isFull || (this.dragReceptorState == DragReceptorState.ENTERED)) {
      paperRenderer.paintAll(g, origAlpha, this.dragReceptorState);
    }
    // _0_0_2_0_9
    AffineTransform trans_0_0_2_0_9 = g.getTransform();
    g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -167.00001525878906f, -3.0f));
    clipRenderer.paintComposite(g, origAlpha);
    g.setTransform(trans_0_0_2_0_9);
  }

  private void paintCompositeGraphicsNode_0_0_2(Graphics2D g) {
    paintCompositeGraphicsNode_0_0_2_0(g);
  }

  private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
    // _0_0_2
    AffineTransform trans_0_0_2 = g.getTransform();
    g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -128.79701232910156f, -51.03125f));
    paintCompositeGraphicsNode_0_0_2(g);
    g.setTransform(trans_0_0_2);
  }

  private void paintRootGraphicsNode_0(Graphics2D g) {
    g.setComposite(AlphaComposite.getInstance(3, origAlpha));
    paintCanvasGraphicsNode_0_0(g);
  }

  /**
   * Returns the X of the bounding box of the original SVG image.
   *
   * @return The X of the bounding box of the original SVG image.
   */
  public int getOrigX() {
    return 1;
  }

  /**
   * Returns the Y of the bounding box of the original SVG image.
   *
   * @return The Y of the bounding box of the original SVG image.
   */
  public int getOrigY() {
    return 0;
  }

  /**
   * Returns the width of the bounding box of the original SVG image.
   *
   * @return The width of the bounding box of the original SVG image.
   */
  public int getOrigWidth() {
    return 48;
  }

  /**
   * Returns the height of the bounding box of the original SVG image.
   *
   * @return The height of the bounding box of the original SVG image.
   */
  public int getOrigHeight() {
    return 43;
  }

  private boolean isFull;

  /**
   * The current width of this resizable icon.
   */
  private int width;

  /**
   * The current height of this resizable icon.
   */
  private int height;

  private DragReceptorState dragReceptorState = DragReceptorState.IDLE;

  /**
   * Creates a new transcoded SVG image.
   */
  public ClipboardIcon() {
    this.width = getOrigWidth();
    this.height = getOrigHeight();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.Icon#getIconHeight()
   */
  @Override
  public int getIconHeight() {
    return height;
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.Icon#getIconWidth()
   */
  @Override
  public int getIconWidth() {
    return width;
  }

  /*
   * Set the dimension of the icon.
   */

  public void setDimension(Dimension newDimension) {
    this.width = newDimension.width;
    this.height = newDimension.height;
  }

  public DragReceptorState getDragReceptorState() {
    return this.dragReceptorState;
  }

  public void setDragReceptorState(DragReceptorState dragReceptorState) {
    this.dragReceptorState = dragReceptorState;
  }

  public boolean isFull() {
    return this.isFull;
  }

  public void setFull(boolean isFull) {
    this.isFull = isFull;
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
   */
  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.translate(x, y);

    double coef1 = (double) this.width / (double) getOrigWidth();
    double coef2 = (double) this.height / (double) getOrigHeight();
    double coef = Math.min(coef1, coef2);
    g2d.scale(coef, coef);
    paint(g2d);
    g2d.dispose();
  }
}
