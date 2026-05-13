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
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.awt.TextRenderer;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.render.joglrenderer.NonCachingTextRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;

/**
 * Delegate for font lifecycle management and drawString operations.
 * Extracted from Graphics2D to reduce class size.
 */
/*package-private*/ final class GlTextRenderer {

  private static final class TextRendererHolder {
    private TextRenderer textRenderer;
    private GL gl;

    public TextRenderer getTextRenderer(Font font, GL gl) {
      if (this.textRenderer != null && this.gl != gl) {
        disposeTextRendererSafely();
      }
      if (this.textRenderer == null) {
        this.textRenderer = new NonCachingTextRenderer(font);
      }
      return this.textRenderer;
    }

    public void dispose() {
      if (this.textRenderer != null) {
        disposeTextRendererSafely();
      }
      this.gl = null;
    }

    private void disposeTextRendererSafely() {
      try {
        this.textRenderer.dispose();
      } catch (GLException gle) {
        Logger.info("Unable to dispose of text renderer.", gle);
      }
      this.textRenderer = null;
    }
  }

  private final Graphics2D graphics2D;

  private final Map<Font, ReferencedObject<TextRendererHolder>> activeFontToTextRendererMap = Maps.newHashMap();
  private final Map<Font, ReferencedObject<TextRendererHolder>> forgottenFontToTextRendererMap = Maps.newHashMap();

  GlTextRenderer(Graphics2D graphics2D) {
    assert graphics2D != null;
    this.graphics2D = graphics2D;
  }

  boolean isRemembered(Font font) {
    return this.activeFontToTextRendererMap.containsKey(font);
  }

  void remember(Font font) {
    ReferencedObject<TextRendererHolder> referencedObject = this.activeFontToTextRendererMap.get(font);
    if (referencedObject == null) {
      referencedObject = this.forgottenFontToTextRendererMap.get(font);
      if (referencedObject != null) {
        this.forgottenFontToTextRendererMap.remove(font);
      } else {
        referencedObject = new ReferencedObject<TextRendererHolder>(new TextRendererHolder(), 0);
      }
      this.activeFontToTextRendererMap.put(font, referencedObject);
    }
    referencedObject.addReference();
  }

  Rectangle2D getBounds(String text, Font font) {
    ReferencedObject<TextRendererHolder> referencedObject = this.activeFontToTextRendererMap.get(font);
    assert referencedObject != null;
    assert referencedObject.isReferenced();
    Rectangle2D bounds = referencedObject.getObject().getTextRenderer(font, graphics2D.renderContext.gl).getBounds(text);
    assert bounds != null;
    return bounds;
  }

  void forget(Font font) {
    ReferencedObject<TextRendererHolder> referencedObject = this.activeFontToTextRendererMap.get(font);
    assert referencedObject != null;
    assert referencedObject.isReferenced();
    referencedObject.removeReference();
    if (!referencedObject.isReferenced()) {
      this.activeFontToTextRendererMap.remove(font);
      this.forgottenFontToTextRendererMap.put(font, referencedObject);
    }
  }

  void disposeForgottenFonts() {
    synchronized (this.forgottenFontToTextRendererMap) {
      for (ReferencedObject<TextRendererHolder> referencedObject : this.forgottenFontToTextRendererMap.values()) {
        referencedObject.getObject().dispose();
      }
      this.forgottenFontToTextRendererMap.clear();
    }
  }

  void drawString(String text, float x, float y) {
    Font font = graphics2D.getFontField();
    ReferencedObject<TextRendererHolder> referencedObject = this.activeFontToTextRendererMap.get(font);
    //todo?
    if (referencedObject == null) {
      remember(font);
      referencedObject = this.activeFontToTextRendererMap.get(font);
    }
    assert referencedObject != null;
    TextRenderer glTextRenderer = referencedObject.getObject().getTextRenderer(font, graphics2D.renderContext.gl);
    glTextRenderer.beginRendering(graphics2D.getWidth(), graphics2D.getHeight());
    if (graphics2D.getPaintField() instanceof Color color) {
      glTextRenderer.setColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
    } else {
      //todo?
    }
    int xPixel = (int) (x + graphics2D.getAffineTransformRef().getTranslateX());
    int yPixel = (int) (y + graphics2D.getAffineTransformRef().getTranslateY());
    glTextRenderer.draw(text, xPixel, graphics2D.getHeight() - yPixel);
    glTextRenderer.endRendering();
  }

  // Called by Graphics2D.disposeForgottenImageGenerators() to preserve L1137 bug
  void clearForgottenMap() {
    this.forgottenFontToTextRendererMap.clear();
  }
}
