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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Renders the clipboard board (shadow + rectangle).
 * Extracted from ClipboardIcon — 4 shape nodes + 1 composite orchestrator.
 */
class ClipboardBoardRenderer {

  void paintComposite(Graphics2D g, float origAlpha, DragReceptorState state) {
    // _0_0_2_0_0_0
    g.setComposite(AlphaComposite.getInstance(3, 0.62650603f * origAlpha));
    AffineTransform trans_0_0_2_0_0_0 = g.getTransform();
    g.transform(new AffineTransform(0.9065836071968079f, 0.0f, 0.0f, 0.3078975975513458f, 298.0328674316406f, 80.26600646972656f));
    paintShapeNode_0_0_2_0_0_0(g);
    g.setTransform(trans_0_0_2_0_0_0);
    // _0_0_2_0_0_1
    g.setComposite(AlphaComposite.getInstance(3, 0.1927711f * origAlpha));
    AffineTransform trans_0_0_2_0_0_1 = g.getTransform();
    g.transform(new AffineTransform(1.156583547592163f, 0.0f, 0.0f, 0.7117437720298767f, 291.9234924316406f, 64.15302276611328f));
    paintShapeNode_0_0_2_0_0_1(g);
    g.setTransform(trans_0_0_2_0_0_1);
    // _0_0_2_0_0_2
    g.setComposite(AlphaComposite.getInstance(3, origAlpha));
    AffineTransform trans_0_0_2_0_0_2 = g.getTransform();
    g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f));
    paintShapeNode_0_0_2_0_0_2(g, state);
    g.setTransform(trans_0_0_2_0_0_2);
    // _0_0_2_0_0_3
    g.setComposite(AlphaComposite.getInstance(3, 0.3f * origAlpha));
    AffineTransform trans_0_0_2_0_0_3 = g.getTransform();
    g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f));
    paintShapeNode_0_0_2_0_0_3(g);
    g.setTransform(trans_0_0_2_0_0_3);
  }

  private void paintShapeNode_0_0_2_0_0_0(Graphics2D g) {
    RoundRectangle2D.Double shape0 = new RoundRectangle2D.Double(6.874999523162842, 35.875, 35.125, 6.5, 6.499999523162842, 6.5);
    g.setPaint(Color.BLACK);
    g.fill(shape0);
  }

  private void paintShapeNode_0_0_2_0_0_1(Graphics2D g) {
    RoundRectangle2D.Double shape1 = new RoundRectangle2D.Double(6.874999523162842, 35.875, 35.125, 6.5, 6.499999523162842, 6.5);
    g.fill(shape1);
  }

  private void paintShapeNode_0_0_2_0_0_2(Graphics2D g, DragReceptorState state) {
    RoundRectangle2D.Double shape2 = new RoundRectangle2D.Double(305.5, -92.5, 29.999996185302734, 31.999998092651367, 5.0, 5.0);

    Color boardColor = state.getBoardColor();
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(25.5, -13.625), new Point2D.Double(26.0, -39.125), new float[] {0.0f, 1.0f}, new Color[] {boardColor, new Color(199, 155, 85, 255)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 296.0f, -52.0f)));

    g.fill(shape2);
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(18.39735221862793, -37.160858154296875), new Point2D.Double(10.831841468811035, 4.028111457824707), new float[] {0.0f, 1.0f}, new Color[] {new Color(143, 89, 2, 255), new Color(233, 185, 110, 255)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 296.0f, -50.0f)));
    g.setStroke(new BasicStroke(1.0f, 0, 0, 4.0f, null, 0.0f));
    g.draw(shape2);
  }

  private void paintShapeNode_0_0_2_0_0_3(Graphics2D g) {
    RoundRectangle2D.Double shape3 = new RoundRectangle2D.Double(306.5, -91.5, 28.00001335144043, 30.000003814697266, 3.0, 3.0);
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(14.787761688232422, -9.017683982849121), new Point2D.Double(14.787761688232422, -69.46895599365234), new float[] {0.0f, 1.0f}, new Color[] {new Color(255, 255, 255, 255), new Color(255, 255, 255, 0)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 296.0f, -52.0f)));
    g.setStroke(new BasicStroke(0.99999994f, 0, 0, 4.0f, null, 0.0f));
    g.draw(shape3);
  }
}
