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

import edu.cmu.cs.dennisc.image.ImageGenerator;
import edu.cmu.cs.dennisc.texture.BufferedImageTexture;
import edu.cmu.cs.dennisc.texture.CustomTexture;
import edu.cmu.cs.dennisc.texture.Texture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES1.GL_ALPHA_SCALE;

/**
 * Delegate for image lifecycle management and paint operations.
 * Extracted from Graphics2D to reduce class size.
 */
/*package-private*/ final class GlImageRenderer {

  private final Graphics2D graphics2D;

  private final Map<Image, ImageGenerator> imageToImageGeneratorMap = new HashMap<Image, ImageGenerator>();
  private final Map<ImageGenerator, ReferencedObject<Pixels>> activeImageGeneratorToPixelsMap = new HashMap<ImageGenerator, ReferencedObject<Pixels>>();
  private final Map<ImageGenerator, ReferencedObject<Pixels>> forgottenImageGeneratorToPixelsMap = new HashMap<ImageGenerator, ReferencedObject<Pixels>>();

  GlImageRenderer(Graphics2D graphics2D) {
    assert graphics2D != null;
    this.graphics2D = graphics2D;
  }

  boolean isRemembered(ImageGenerator imageGenerator) {
    return this.activeImageGeneratorToPixelsMap.containsKey(imageGenerator);
  }

  void remember(ImageGenerator imageGenerator) {
    assert imageGenerator != null;
    ReferencedObject<Pixels> referencedObject = this.activeImageGeneratorToPixelsMap.get(imageGenerator);
    if (referencedObject == null) {
      referencedObject = this.forgottenImageGeneratorToPixelsMap.get(imageGenerator);
      if (referencedObject != null) {
        this.forgottenImageGeneratorToPixelsMap.remove(imageGenerator);
      } else {
        if (imageGenerator instanceof Texture texture) {

          if (texture instanceof CustomTexture customTexture) {
            customTexture.layoutIfNecessary(graphics2D);
          }

          Pixels pixels = new Pixels(texture);
          referencedObject = new ReferencedObject<Pixels>(pixels, 0);

        } else {
          throw new RuntimeException("TODO");
        }
      }
      this.activeImageGeneratorToPixelsMap.put(imageGenerator, referencedObject);
    }
    referencedObject.addReference();
  }

  void paint(ImageGenerator imageGenerator, float x, float y, float alpha) {
    ReferencedObject<Pixels> referencedObject = this.activeImageGeneratorToPixelsMap.get(imageGenerator);
    assert referencedObject != null;
    assert referencedObject.isReferenced();

    Pixels pixels = referencedObject.getObject();
    graphics2D.renderContext.gl.glEnable(GL_BLEND);
    graphics2D.renderContext.gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    graphics2D.renderContext.gl.glPixelTransferf(GL_ALPHA_SCALE, alpha);

    int xPixel = (int) x;
    int yPixel = (int) y + pixels.getHeight();
    graphics2D.renderContext.gl.glRasterPos2i(0, 0);
    graphics2D.renderContext.gl.glBitmap(0, 0, 0, 0, xPixel, -yPixel, null);
    graphics2D.renderContext.gl.glDrawPixels(pixels.getWidth(), pixels.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, pixels.getRGBA());
    graphics2D.renderContext.gl.glDisable(GL_BLEND);
    graphics2D.renderContext.gl.glPixelTransferf(GL_ALPHA_SCALE, 1.0f);
  }

  void forget(ImageGenerator imageGenerator) {
    ReferencedObject<Pixels> referencedObject = this.activeImageGeneratorToPixelsMap.get(imageGenerator);
    assert referencedObject != null;
    assert referencedObject.isReferenced();
    referencedObject.removeReference();
    if (!referencedObject.isReferenced()) {
      this.activeImageGeneratorToPixelsMap.remove(imageGenerator);
      this.forgottenImageGeneratorToPixelsMap.put(imageGenerator, referencedObject);
    }
  }

  private void disposeImageGenerator(ImageGenerator imageGenerator) {
    ReferencedObject<Pixels> referencedObject = this.forgottenImageGeneratorToPixelsMap.get(imageGenerator);
    referencedObject.getObject().release();
  }

  // Iterates and disposes forgotten image generators but does NOT clear the map,
  // preserving the L1137 bug where the font map is cleared instead.
  void disposeForgottenImageGenerators() {
    synchronized (this.forgottenImageGeneratorToPixelsMap) {
      for (ReferencedObject<Pixels> referencedObject : this.forgottenImageGeneratorToPixelsMap.values()) {
        referencedObject.getObject().release();
      }
    }
  }

  boolean isRemembered(Image image) {
    ImageGenerator imageGenerator = this.imageToImageGeneratorMap.get(image);
    if (imageGenerator != null) {
      return isRemembered(imageGenerator);
    } else {
      return false;
    }
  }

  void remember(Image image) {
    ImageGenerator imageGenerator = this.imageToImageGeneratorMap.get(image);
    if (imageGenerator == null) {
      if (image instanceof BufferedImage bufferedImage) {
        BufferedImageTexture bufferedImageTexture = new BufferedImageTexture();
        bufferedImageTexture.setBufferedImage(bufferedImage);
        bufferedImageTexture.setMipMappingDesired(false);
        imageGenerator = bufferedImageTexture;
      } else {
        throw new RuntimeException("todo");
      }
      this.imageToImageGeneratorMap.put(image, imageGenerator);
    }
    remember(imageGenerator);
  }

  void forget(Image image) {
    ImageGenerator imageGenerator = this.imageToImageGeneratorMap.get(image);
    forget(imageGenerator);
  }

  void disposeForgottenImages() {
    //todo
    for (ImageGenerator imageGenerator : this.imageToImageGeneratorMap.values()) {
      if (this.forgottenImageGeneratorToPixelsMap.containsKey(imageGenerator)) {
        disposeImageGenerator(imageGenerator);
        this.forgottenImageGeneratorToPixelsMap.remove(imageGenerator);
      }
    }
  }

  ImageGenerator getImageGenerator(Image image) {
    return this.imageToImageGeneratorMap.get(image);
  }
}
