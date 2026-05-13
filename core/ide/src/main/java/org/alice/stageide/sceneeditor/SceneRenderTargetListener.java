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
package org.alice.stageide.sceneeditor;

import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import edu.cmu.cs.dennisc.render.event.*;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Vector3;

import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Encapsulates the RenderTargetListener callbacks and paintHorizonLine helper.
 * Extracted from StorytellingSceneEditor (issue #528).
 */
class SceneRenderTargetListener implements RenderTargetListener {

  final StorytellingSceneEditor editor;

  SceneRenderTargetListener(StorytellingSceneEditor editor) {
    this.editor = editor;
  }

  @Override
  public void initialized(RenderTargetInitializeEvent e) {
  }

  @Override
  public void cleared(RenderTargetRenderEvent e) {
  }

  @Override
  public void rendered(RenderTargetRenderEvent e) {
    if ((editor.onscreenRenderTarget.getSgCameraCount() > 0) && (editor.onscreenRenderTarget.getSgCameraAt(0) instanceof OrthographicCamera)) {
      paintHorizonLine(e.getGraphics2D(), editor.onscreenRenderTarget, (OrthographicCamera) editor.onscreenRenderTarget.getSgCameraAt(0));
    }
  }

  @Override
  public void resized(RenderTargetResizeEvent e) {
  }

  @Override
  public void displayChanged(RenderTargetDisplayChangeEvent e) {
  }

  private void paintHorizonLine(Graphics graphics, OnscreenRenderTarget renderTarget, OrthographicCamera camera) {
    AffineMatrix4x4 cameraTransform = camera.getAbsoluteTransformation();
    double dotProd = cameraTransform.orientation().up().dotProduct(Vector3.POSITIVE_Y_AXIS);
    if ((dotProd == 1) || (dotProd == -1)) {
      Dimension lookingGlassSize = renderTarget.getSurfaceSize();

      Point3 cameraPosition = camera.getAbsoluteTransformation().translation();

      var dummyPlane = camera.picturePlane.getValue().completeFrom(renderTarget.getActualViewport(camera));

      double lookingGlassHeight = lookingGlassSize.getHeight();

      double yRatio = editor.onscreenRenderTarget.getSurfaceHeight() / dummyPlane.getHeight();
      double horizonInCameraSpace = 0.0d - cameraPosition.y();
      double distanceFromMaxY = dummyPlane.getYMaximum() - horizonInCameraSpace;
      int horizonLinePixelVal = (int) (yRatio * distanceFromMaxY);
      if ((horizonLinePixelVal >= 0) && (horizonLinePixelVal <= lookingGlassHeight)) {
        graphics.setColor(java.awt.Color.BLACK);
        graphics.drawLine(0, horizonLinePixelVal, lookingGlassSize.width, horizonLinePixelVal);
      }
    }
  }
}
