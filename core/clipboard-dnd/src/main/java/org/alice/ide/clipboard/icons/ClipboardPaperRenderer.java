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
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

/**
 * Renders the clipboard paper (shadow, fill, stroke, fold, highlight).
 * Extracted from ClipboardIcon — 6 shape nodes + 2 composite wrappers + orchestration.
 */
class ClipboardPaperRenderer {

  void paintAll(Graphics2D g, float origAlpha, DragReceptorState state) {
    // _0_0_2_0_1
    g.setComposite(AlphaComposite.getInstance(3, 0.2f * origAlpha));
    paintShapeNode_0_0_2_0_1(g);
    // _0_0_2_0_2
    g.setComposite(AlphaComposite.getInstance(3, origAlpha));
    // _0_0_2_0_4
    AffineTransform trans_0_0_2_0_4 = g.getTransform();
    g.transform(new AffineTransform(0.6232035160064697f, 0.0f, 0.0f, 0.6771684288978577f, 164.3101348876953f, 56.7651481628418f));
    paintCompositeGraphicsNode_0_0_2_0_4(g, state);
    g.setTransform(trans_0_0_2_0_4);
    // _0_0_2_0_5
    AffineTransform trans_0_0_2_0_5 = g.getTransform();
    g.transform(new AffineTransform(0.6232035160064697f, 0.0f, 0.0f, 0.6771684288978577f, 164.3101348876953f, 56.7651481628418f));
    paintCompositeGraphicsNode_0_0_2_0_5(g);
    g.setTransform(trans_0_0_2_0_5);
    // _0_0_2_0_6
    g.setComposite(AlphaComposite.getInstance(3, 0.2f * origAlpha));
    AffineTransform trans_0_0_2_0_6 = g.getTransform();
    g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 0.903225839138031f, -167.00027465820312f, 5.119354724884033f));
    paintShapeNode_0_0_2_0_6(g);
    g.setTransform(trans_0_0_2_0_6);
    // _0_0_2_0_7
    g.setComposite(AlphaComposite.getInstance(3, origAlpha));
    paintShapeNode_0_0_2_0_7(g);
    // _0_0_2_0_8
    paintShapeNode_0_0_2_0_8(g);
  }

  private void paintShapeNode_0_0_2_0_1(Graphics2D g) {
    GeneralPath shape4 = new GeneralPath();
    shape4.moveTo(142.93753f, 60.5f);
    shape4.lineTo(164.068f, 60.5f);
    shape4.curveTo(164.86101f, 60.5f, 165.49942f, 61.138416f, 165.49942f, 61.931427f);
    shape4.lineTo(165.49973f, 82.5f);
    shape4.curveTo(165.49973f, 82.5f, 160.49973f, 87.5f, 160.49973f, 87.5f);
    shape4.lineTo(142.93753f, 87.5057f);
    shape4.curveTo(142.14452f, 87.5057f, 141.5061f, 86.86729f, 141.5061f, 86.07427f);
    shape4.lineTo(141.5061f, 61.931465f);
    shape4.curveTo(141.5061f, 61.138454f, 142.14452f, 60.50004f, 142.93753f, 60.50004f);
    shape4.closePath();
    g.setPaint(Color.BLACK);
    g.fill(shape4);
    g.setStroke(new BasicStroke(1.0000001f, 0, 0, 4.0f, null, 0.0f));
    g.draw(shape4);
  }

  private void paintShapeNode_0_0_2_0_4_0(Graphics2D g, DragReceptorState state) {
    GeneralPath shape5 = new GeneralPath();
    shape5.moveTo(-34.29474f, 4.03866f);
    shape5.lineTo(-0.3885193f, 4.03866f);
    shape5.curveTo(0.88395476f, 4.03866f, 1.9083652f, 4.9814334f, 1.9083652f, 6.1525016f);
    shape5.lineTo(1.9088448f, 36.526886f);
    shape5.curveTo(1.9088448f, 36.526886f, -6.114217f, 43.910576f, -6.114217f, 43.910576f);
    shape5.lineTo(-34.29474f, 43.918976f);
    shape5.curveTo(-35.56721f, 43.918976f, -36.59162f, 42.976204f, -36.59162f, 41.805134f);
    shape5.lineTo(-36.59162f, 6.152546f);
    shape5.curveTo(-36.59162f, 4.9814777f, -35.56721f, 4.0387044f, -34.29474f, 4.0387044f);
    shape5.closePath();
    Color paperColor = state.getPaperColor();
    g.setPaint(GradientPaintFactory.new_RadialGradientPaint(new Point2D.Double(-117.93485260009766, 5.198304176330566), 18.000002f, new Point2D.Double(-117.93485260009766, 5.198304176330566), new float[] {0.0f, 1.0f}, new Color[] {paperColor, new Color(211, 215, 207, 255)},
                                       new AffineTransform(3.19461989402771f, 0.0f, 0.0f, 1.5470696687698364f, 365.3152770996094f, 23.795835494995117f)));
    g.fill(shape5);
  }

  private void paintCompositeGraphicsNode_0_0_2_0_4(Graphics2D g, DragReceptorState state) {
    paintShapeNode_0_0_2_0_4_0(g, state);
  }

  private void paintShapeNode_0_0_2_0_5_0(Graphics2D g) {
    GeneralPath shape6 = new GeneralPath();
    shape6.moveTo(-34.29474f, 4.03866f);
    shape6.lineTo(-0.3885193f, 4.03866f);
    shape6.curveTo(0.88395476f, 4.03866f, 1.9083652f, 4.9814334f, 1.9083652f, 6.1525016f);
    shape6.lineTo(1.9088448f, 36.526886f);
    shape6.curveTo(1.9088448f, 36.526886f, -6.114217f, 43.910576f, -6.114217f, 43.910576f);
    shape6.lineTo(-34.29474f, 43.918976f);
    shape6.curveTo(-35.56721f, 43.918976f, -36.59162f, 42.976204f, -36.59162f, 41.805134f);
    shape6.lineTo(-36.59162f, 6.152546f);
    shape6.curveTo(-36.59162f, 4.9814777f, -35.56721f, 4.0387044f, -34.29474f, 4.0387044f);
    shape6.closePath();
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(-367.9520568847656, 16.063671112060547), new Point2D.Double(-393.3939208984375, -46.69970703125), new float[] {0.0f, 1.0f}, new Color[] {new Color(85, 87, 83, 255), new Color(186, 189, 182, 255)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 356.0f, 50.0f)));
    g.setStroke(new BasicStroke(1.5393476f, 0, 0, 4.0f, null, 0.0f));
    g.draw(shape6);
  }

  private void paintCompositeGraphicsNode_0_0_2_0_5(Graphics2D g) {
    paintShapeNode_0_0_2_0_5_0(g);
  }

  private void paintShapeNode_0_0_2_0_6(Graphics2D g) {
    GeneralPath shape7 = new GeneralPath();
    shape7.moveTo(331.5f, 84.5f);
    shape7.lineTo(326.5f, 84.5f);
    shape7.lineTo(326.5f, 89.5f);
    shape7.lineTo(331.5f, 84.5f);
    shape7.closePath();
    g.setPaint(Color.BLACK);
    g.fill(shape7);
  }

  private void paintShapeNode_0_0_2_0_7(Graphics2D g) {
    GeneralPath shape8 = new GeneralPath();
    shape8.moveTo(165.49973f, 81.5f);
    shape8.lineTo(160.49973f, 81.5f);
    shape8.lineTo(160.49973f, 86.5f);
    shape8.lineTo(165.49973f, 81.5f);
    shape8.closePath();
    g.setPaint(GradientPaintFactory.new_RadialGradientPaint(new Point2D.Double(328.5484619140625, 85.5484619140625), 3.0f, new Point2D.Double(328.5484619140625, 85.5484619140625), new float[] {0.0f, 1.0f}, new Color[] {new Color(255, 255, 255, 255), new Color(136, 138, 133, 255)},
                                       new AffineTransform(0.6394925117492676f, 0.0f, 0.0f, 0.6394924521446228f, -48.60456085205078f, 27.792404174804688f)));
    g.fill(shape8);
    g.setPaint(GradientPaintFactory.new_RadialGradientPaint(new Point2D.Double(327.53125, 84.5), 3.0f, new Point2D.Double(327.53125, 84.5), new float[] {0.0f, 1.0f}, new Color[] {new Color(211, 215, 207, 255), new Color(85, 87, 83, 255)},
                                       new AffineTransform(1.9562286138534546f, 0.0f, 0.0f, 1.9562286138534546f, -480.1950378417969f, -83.80131530761719f)));
    g.setStroke(new BasicStroke(1.0f, 0, 1, 4.0f, null, 0.0f));
    g.draw(shape8);
  }

  private void paintShapeNode_0_0_2_0_8(Graphics2D g) {
    GeneralPath shape9 = new GeneralPath();
    shape9.moveTo(143.07256f, 60.5f);
    shape9.lineTo(163.92691f, 60.5f);
    shape9.curveTo(164.24425f, 60.5f, 164.49973f, 60.755478f, 164.49973f, 61.07282f);
    shape9.lineTo(164.49973f, 81.0f);
    shape9.curveTo(164.49973f, 81.0f, 159.99973f, 85.5f, 159.99973f, 85.5f);
    shape9.lineTo(143.07254f, 85.5f);
    shape9.curveTo(142.7552f, 85.5f, 142.49973f, 85.24452f, 142.49973f, 84.927185f);
    shape9.lineTo(142.49973f, 61.07282f);
    shape9.curveTo(142.49973f, 60.755478f, 142.7552f, 60.5f, 143.07254f, 60.5f);
    shape9.closePath();
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(325.5882263183594, 82.02571105957031), new Point2D.Double(333.8441162109375, 90.2815933227539), new float[] {0.0f, 1.0f}, new Color[] {new Color(255, 255, 255, 255), new Color(255, 255, 255, 0)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -167.00027465820312f, -3.0f)));
    g.setStroke(new BasicStroke(0.99999946f, 0, 0, 4.0f, null, 0.0f));
    g.draw(shape9);
  }
}
