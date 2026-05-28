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

package edu.cmu.cs.dennisc.render.gl.imp.adapters;

import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.pattern.Releasable;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.adorn.GlrPivotFigure;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.adorn.GlrStickFigure;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrMainTitle;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrOvertitle;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrSpeechBubble;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrSubtitle;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrThoughtBubble;
import edu.cmu.cs.dennisc.scenegraph.*;
import edu.cmu.cs.dennisc.scenegraph.adorn.PivotFigure;
import edu.cmu.cs.dennisc.scenegraph.adorn.StickFigure;
import edu.cmu.cs.dennisc.scenegraph.graphics.MainTitle;
import edu.cmu.cs.dennisc.scenegraph.graphics.Overtitle;
import edu.cmu.cs.dennisc.scenegraph.graphics.SpeechBubble;
import edu.cmu.cs.dennisc.scenegraph.graphics.Subtitle;
import edu.cmu.cs.dennisc.scenegraph.graphics.ThoughtBubble;
import edu.cmu.cs.dennisc.texture.BufferedImageTexture;
import edu.cmu.cs.dennisc.texture.CustomTexture;
import edu.cmu.cs.dennisc.texture.Texture;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Dennis Cosgrove
 */
public class AdapterFactory {
  private static final Map<Releasable, GlrObject<? extends Releasable>> s_elementToAdapterMap = Maps.newHashMap();
  private static final Map<Class<? extends Releasable>, Supplier<? extends GlrObject<?>>> s_supplierMap = Maps.newHashMap();

  static {
    // Scenegraph core adapters
    register(AmbientLight.class, GlrAmbientLight::new);
    register(Background.class, GlrBackground::new);
    register(Box.class, GlrBox::new);
    register(ClippingPlane.class, GlrClippingPlane::new);
    register(Cylinder.class, GlrCylinder::new);
    register(DirectionalLight.class, GlrDirectionalLight::new);
    register(Disc.class, GlrDisc::new);
    register(ExponentialFog.class, GlrExponentialFog::new);
    register(ExponentialSquaredFog.class, GlrExponentialSquaredFog::new);
    register(Ghost.class, GlrGhost::new);
    register(HorizontalSurface.class, GlrHorizontalSurface::new);
    register(IndexedQuadrilateralArray.class, GlrIndexedQuadrilateralArray::new);
    register(IndexedTriangleArray.class, GlrIndexedTriangleArray::new);
    register(Joint.class, GlrJoint::new);
    register(Layer.class, GlrLayer::new);
    register(LineArray.class, GlrLineArray::new);
    register(LineLoop.class, GlrLineLoop::new);
    register(LineStrip.class, GlrLineStrip::new);
    register(LinearFog.class, GlrLinearFog::new);
    register(Mesh.class, GlrMesh::new);
    register(OldMesh.class, GlrOldMesh::new);
    register(OrthographicCamera.class, GlrOrthographicCamera::new);
    register(PlanarReflector.class, GlrPlanarReflector::new);
    register(PointArray.class, GlrPointArray::new);
    register(PointLight.class, GlrPointLight::new);
    register(QuadArray.class, GlrQuadArray::new);
    register(QuadStrip.class, GlrQuadStrip::new);
    register(Scalable.class, GlrScalable::new);
    register(Scene.class, GlrScene::new);
    register(Silhouette.class, GlrSilhouette::new);
    register(SimpleAppearance.class, GlrSimpleAppearance::new);
    register(SkeletonVisual.class, GlrSkeletonVisual::new);
    register(Sphere.class, GlrSphere::new);
    register(SpotLight.class, GlrSpotLight::new);
    register(Sprite.class, GlrSprite::new);
    register(StandIn.class, GlrStandIn::new);
    register(SymmetricPerspectiveCamera.class, GlrSymmetricPerspectiveCamera::new);
    register(TexturedAppearance.class, GlrTexturedAppearance::new);
    register(TexturedVisual.class, GlrTexturedVisual::new);
    register(Torus.class, GlrTorus::new);
    register(Transformable.class, GlrTransformable::new);
    register(TransformableVisual.class, GlrTransformableVisual::new);
    register(TriangleArray.class, GlrTriangleArray::new);
    register(TriangleFan.class, GlrTriangleFan::new);
    register(TriangleStrip.class, GlrTriangleStrip::new);
    register(Visual.class, GlrVisual::new);
    register(WeightedMesh.class, GlrWeightedMesh::new);

    // Texture adapters
    register(BufferedImageTexture.class, GlrBufferedImageTexture::new);
    register(CustomTexture.class, GlrCustomTexture::new);

    // Graphics adapters (scenegraph.Text maps to adapters.GlrText, not graphics.GlrText)
    register(edu.cmu.cs.dennisc.scenegraph.Text.class, GlrText::new);
    register(MainTitle.class, GlrMainTitle::new);
    register(Overtitle.class, GlrOvertitle::new);
    register(SpeechBubble.class, GlrSpeechBubble::new);
    register(Subtitle.class, GlrSubtitle::new);
    register(ThoughtBubble.class, GlrThoughtBubble::new);

    // Adorn adapters
    register(StickFigure.class, GlrStickFigure::new);
    register(PivotFigure.class, GlrPivotFigure::new);
  }

  private AdapterFactory() {
    throw new AssertionError();
  }

  @SuppressWarnings("unchecked")
  public static <SG extends Releasable> void register(Class<SG> sgClass, Supplier<? extends GlrObject<?>> supplier) {
    s_supplierMap.put(sgClass, supplier);
  }

  private static void createNecessaryProxies(Releasable sgElement) {
    GlrObject<?> unused = getAdapterForElement(sgElement);
    if (sgElement instanceof Composite sgComposite) {
      for (Component sgComponent : sgComposite.getComponents()) {
        createNecessaryProxies(sgComponent);
      }
    }
  }

  private static <SG extends Releasable, GLR extends GlrObject<SG>> GLR createAdapterFor(SG sgElement) {
    Class<?> sgClass = sgElement.getClass();
    Supplier<? extends GlrObject<?>> supplier = s_supplierMap.get(sgClass);
    // Walk up the hierarchy to find a registered adapter
    if (supplier == null) {
      Class<?> search = sgClass.getSuperclass();
      while (search != null && supplier == null) {
        supplier = s_supplierMap.get(search);
        search = search.getSuperclass();
      }
      if (supplier != null) {
        // Cache the resolved supplier for this concrete class
        final Supplier<? extends GlrObject<?>> resolved = supplier;
        s_supplierMap.put((Class<? extends Releasable>) sgClass, resolved);
      }
    }
    GLR rv;
    if (supplier != null) {
      try {
        rv = (GLR) supplier.get();
      } catch (Throwable t) {
        Logger.throwable(t, sgClass);
        rv = null;
      }
    } else {
      Logger.severe("cannot find adapter for", sgClass);
      rv = null;
    }
    return rv;
  }

  private static <SG extends Releasable, GLR extends GlrObject<SG>> GLR getAdapterForElement(SG sgElement) {
    GLR rv;
    if (sgElement != null) {
      synchronized (s_elementToAdapterMap) {
        rv = (GLR) s_elementToAdapterMap.get(sgElement);
        if (rv == null) {
          rv = createAdapterFor(sgElement);
          if (rv != null) {
            s_elementToAdapterMap.put(sgElement, rv);
            rv.initialize(sgElement);
            ChangeHandler.addListeners(sgElement);
            createNecessaryProxies(sgElement);
          } else {
            // todo
            // edu.cmu.cs.dennisc.pattern.AbstractElement.warnln( "warning: could
            // not create rv for: " + sgElement );
          }
        } else {
          if (rv.getOwner() == null) {
            rv = null;
            // todo
            // edu.cmu.cs.dennisc.pattern.AbstractElement.warnln( sgElement + "'s
            // rv has null for a sgElement" );
          }
        }
      }
    } else {
      rv = null;
    }
    return rv;
  }

  public static GlrObject<?> getAdapterFor(Releasable releasable) {
    return (GlrObject<?>) getAdapterForElement(releasable);
  }

  public static GlrElement<?> getAdapterFor(Element sgElement) {
    return (GlrElement<?>) getAdapterForElement(sgElement);
  }

  public static GlrAbstractCamera<?> getAdapterFor(AbstractCamera sgCamera) {
    return (GlrAbstractCamera<?>) getAdapterForElement(sgCamera);
  }

  public static GlrOrthographicCamera getAdapterFor(OrthographicCamera sgOrthographicCamera) {
    return (GlrOrthographicCamera) getAdapterForElement(sgOrthographicCamera);
  }

  public static GlrSymmetricPerspectiveCamera getAdapterFor(SymmetricPerspectiveCamera sgSymmetricPerspectiveCamera) {
    return (GlrSymmetricPerspectiveCamera) getAdapterForElement(sgSymmetricPerspectiveCamera);
  }

  public static GlrBackground getAdapterFor(Background sgBackground) {
    return (GlrBackground) getAdapterForElement(sgBackground);
  }

  public static GlrComponent<?> getAdapterFor(Component sgComponent) {
    return (GlrComponent<?>) getAdapterForElement(sgComponent);
  }

  public static GlrComposite<?> getAdapterFor(Composite sgComposite) {
    return (GlrComposite<?>) getAdapterForElement(sgComposite);
  }

  public static GlrAppearance<?> getAdapterFor(Appearance sgAppearance) {
    return (GlrAppearance<?>) getAdapterForElement(sgAppearance);
  }

  public static GlrTexturedAppearance getAdapterFor(TexturedAppearance sgSingleAppearance) {
    return (GlrTexturedAppearance) getAdapterForElement(sgSingleAppearance);
  }

  public static GlrGeometry<?> getAdapterFor(Geometry sgGeometry) {
    return (GlrGeometry<?>) getAdapterForElement(sgGeometry);
  }

  public static GlrScene getAdapterFor(Scene sgScene) {
    return (GlrScene) getAdapterForElement(sgScene);
  }

  public static GlrAbstractTransformable<?> getAdapterFor(AbstractTransformable sgTransformable) {
    return (GlrAbstractTransformable<?>) getAdapterForElement(sgTransformable);
  }

  public static GlrTransformable<?> getAdapterFor(Transformable sgTransformable) {
    return (GlrTransformable<?>) getAdapterForElement(sgTransformable);
  }

  public static GlrGhost getAdapterFor(Ghost sgGhost) {
    return (GlrGhost) getAdapterForElement(sgGhost);
  }

  public static GlrTexture<?> getAdapterFor(Texture texture) {
    return (GlrTexture<?>) getAdapterForElement(texture);
  }

  public static GlrLayer getAdapterFor(Layer sgLayer) {
    return (GlrLayer) getAdapterForElement(sgLayer);
  }

  public static GlrGraphic<?> getAdapterFor(Graphic sgGraphic) {
    return (GlrGraphic<?>) getAdapterForElement(sgGraphic);
  }

  public static GlrSilhouette getAdapterFor(Silhouette sgSilhouette) {
    return (GlrSilhouette) getAdapterForElement(sgSilhouette);
  }

  public static <E extends GlrObject> E[] getAdaptersFor(Releasable[] sgElements, Class<? extends E> componentType) {
    if (sgElements != null) {
      E[] proxies = (E[]) Array.newInstance(componentType, sgElements.length);
      for (int i = 0; i < sgElements.length; i++) {
        proxies[i] = (E) getAdapterForElement(sgElements[i]);
      }
      return proxies;
    } else {
      return null;
    }
  }

  public static void forget(Releasable sgElement) {
    synchronized (s_elementToAdapterMap) {
      s_elementToAdapterMap.remove(sgElement);
    }
  }

  public static void forgetAllElements() {
    synchronized (s_elementToAdapterMap) {
      s_elementToAdapterMap.clear();
    }
  }
}
