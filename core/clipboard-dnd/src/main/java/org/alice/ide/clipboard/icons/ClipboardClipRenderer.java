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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Renders the clipboard clip (shadow, body, ridge, outline, highlight).
 * Extracted from ClipboardIcon — 5 shape nodes + 1 composite orchestrator.
 */
class ClipboardClipRenderer {

  void paintComposite(Graphics2D g, float origAlpha) {
    // _0_0_2_0_9_0
    g.setComposite(AlphaComposite.getInstance(3, 0.44117647f * origAlpha));
    AffineTransform trans_0_0_2_0_9_0 = g.getTransform();
    g.transform(new AffineTransform(1.0502474308013916f, 0.0f, 0.0f, 1.0502474308013916f, 294.7621154785156f, 50.33353042602539f));
    paintShapeNode_0_0_2_0_9_0(g);
    g.setTransform(trans_0_0_2_0_9_0);
    // _0_0_2_0_9_1
    g.setComposite(AlphaComposite.getInstance(3, origAlpha));
    paintShapeNode_0_0_2_0_9_1(g);
    // _0_0_2_0_9_2
    g.setComposite(AlphaComposite.getInstance(3, 0.5f * origAlpha));
    paintShapeNode_0_0_2_0_9_2(g);
    // _0_0_2_0_9_3
    g.setComposite(AlphaComposite.getInstance(3, origAlpha));
    paintShapeNode_0_0_2_0_9_3(g);
    // _0_0_2_0_9_4
    g.setComposite(AlphaComposite.getInstance(3, 0.2f * origAlpha));
    paintShapeNode_0_0_2_0_9_4(g);
  }

  private void paintShapeNode_0_0_2_0_9_0(Graphics2D g) {
    GeneralPath shape10 = new GeneralPath();
    shape10.moveTo(16.722015f, 14.506832f);
    shape10.lineTo(32.354477f, 14.506832f);
    shape10.curveTo(33.326748f, 14.551023f, 33.381283f, 12.617748f, 33.381283f, 12.617748f);
    shape10.lineTo(30.497072f, 10.307271f);
    shape10.lineTo(30.506172f, 9.552748f);
    shape10.curveTo(30.506172f, 9.552748f, 18.491043f, 9.532198f, 18.491043f, 9.532198f);
    shape10.lineTo(18.491043f, 10.4223995f);
    shape10.lineTo(15.890176f, 12.665036f);
    shape10.curveTo(15.890176f, 12.665036f, 15.838146f, 14.462638f, 16.722025f, 14.506832f);
    shape10.closePath();
    g.setPaint(Color.BLACK);
    g.fill(shape10);
  }

  private void paintShapeNode_0_0_2_0_9_1(Graphics2D g) {
    GeneralPath shape11 = new GeneralPath();
    shape11.moveTo(312.72202f, 64.50683f);
    shape11.lineTo(328.3545f, 64.50683f);
    shape11.curveTo(329.32675f, 64.55102f, 329.3813f, 62.617744f, 329.3813f, 62.617744f);
    shape11.lineTo(326.49707f, 60.307266f);
    shape11.lineTo(326.50607f, 56.552742f);
    shape11.curveTo(326.50607f, 55.73013f, 325.81427f, 54.619465f, 324.80728f, 54.619465f);
    shape11.lineTo(316.28723f, 54.531075f);
    shape11.curveTo(315.0748f, 54.531075f, 314.49094f, 55.719227f, 314.49094f, 56.532192f);
    shape11.lineTo(314.49094f, 60.422394f);
    shape11.lineTo(311.89008f, 62.66503f);
    shape11.curveTo(311.89008f, 62.66503f, 311.83807f, 64.46263f, 312.72192f, 64.50683f);
    shape11.closePath();
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(24.635435104370117, 3.519411563873291), new Point2D.Double(24.635435104370117, 11.540999412536621), new float[] {0.0f, 0.13349205f, 0.53102833f, 0.78739f, 1.0f}, new Color[] {new Color(186, 189, 182, 255), new Color(238, 238, 236, 255), new Color(186, 189, 182, 255), new Color(255, 255, 255, 255), new Color(156, 152, 138, 255)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 296.0f, 52.0f)));
    g.fill(shape11);
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(32.91161346435547, 16.214149475097656), new Point2D.Double(31.417892456054688, 4.031081199645996), new float[] {0.0f, 1.0f}, new Color[] {new Color(85, 87, 83, 255), new Color(186, 189, 182, 255)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 296.0f, 50.0f)));
    g.setStroke(new BasicStroke(1.0f, 0, 0, 4.0f, null, 0.0f));
    g.draw(shape11);
  }

  private void paintShapeNode_0_0_2_0_9_2(Graphics2D g) {
    RoundRectangle2D.Double shape12 = new RoundRectangle2D.Double(313.0, 62.0, 15.0, 1.0416321754455566, 1.0416321754455566, 1.0416321754455566);
    g.setPaint(Color.WHITE);
    g.fill(shape12);
  }

  private void paintShapeNode_0_0_2_0_9_3(Graphics2D g) {
    GeneralPath shape13 = new GeneralPath();
    shape13.moveTo(316.0f, 60.0f);
    shape13.lineTo(316.0f, 57.0f);
    shape13.curveTo(315.9606f, 56.368443f, 316.20798f, 55.966385f, 317.0f, 56.0f);
    shape13.lineTo(324.0f, 56.0f);
    shape13.curveTo(324.46307f, 56.07386f, 324.94202f, 56.11598f, 325.0f, 57.0f);
    shape13.lineTo(325.0f, 60.0f);
    shape13.lineTo(324.0f, 57.0f);
    shape13.lineTo(317.0f, 57.0f);
    shape13.lineTo(316.0f, 60.0f);
    shape13.closePath();
    g.setPaint(GradientPaintFactory.new_LinearGradientPaint(new Point2D.Double(24.49800682067871, 3.9980428218841553), new Point2D.Double(24.49800682067871, 8.0), new float[] {0.0f, 1.0f}, new Color[] {Color.WHITE, new Color(255, 255, 255, 0)},
                                       new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 296.0f, 52.0f)));
    g.fill(shape13);
  }

  private void paintShapeNode_0_0_2_0_9_4(Graphics2D g) {
    GeneralPath shape14 = new GeneralPath();
    shape14.moveTo(316.28125f, 55.40625f);
    shape14.curveTo(315.96063f, 55.40625f, 315.79126f, 55.524544f, 315.625f, 55.75f);
    shape14.curveTo(315.45874f, 55.975456f, 315.375f, 56.32886f, 315.375f, 56.53125f);
    shape14.lineTo(315.375f, 60.4375f);
    shape14.curveTo(315.37088f, 60.691208f, 315.25687f, 60.930645f, 315.0625f, 61.09375f);
    shape14.lineTo(312.78125f, 63.03125f);
    shape14.curveTo(312.79025f, 63.164944f, 312.77625f, 63.212463f, 312.81244f, 63.375f);
    shape14.curveTo(312.84085f, 63.503906f, 312.88025f, 63.571346f, 312.90613f, 63.625f);
    shape14.lineTo(328.24988f, 63.625f);
    shape14.curveTo(328.2776f, 63.58374f, 328.3303f, 63.499012f, 328.37488f, 63.34375f);
    shape14.curveTo(328.41977f, 63.18762f, 328.41977f, 63.136784f, 328.43738f, 63.0f);
    shape14.lineTo(325.93738f, 61.0f);
    shape14.curveTo(325.73462f, 60.82988f, 325.61972f, 60.57713f, 325.62488f, 60.3125f);
    shape14.lineTo(325.62488f, 56.5625f);
    shape14.curveTo(325.62488f, 56.392494f, 325.51932f, 56.03895f, 325.34363f, 55.8125f);
    shape14.curveTo(325.16806f, 55.58605f, 324.99304f, 55.5f, 324.8125f, 55.5f);
    shape14.lineTo(316.28125f, 55.40625f);
    shape14.closePath();
    g.setPaint(Color.WHITE);
    g.draw(shape14);
  }
}
