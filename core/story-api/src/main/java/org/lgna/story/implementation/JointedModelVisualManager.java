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

package org.lgna.story.implementation;

import edu.cmu.cs.dennisc.property.InstanceProperty;
import edu.cmu.cs.dennisc.scenegraph.Composite;
import edu.cmu.cs.dennisc.scenegraph.Scalable;
import edu.cmu.cs.dennisc.scenegraph.SimpleAppearance;
import edu.cmu.cs.dennisc.scenegraph.Visual;
import org.alice.math.immutable.Dimension3;
import org.alice.math.immutable.Matrix3x3;
import org.lgna.story.implementation.JointedModelImp.JointImplementationAndVisualDataFactory;
import org.lgna.story.implementation.JointedModelImp.VisualData;
import org.lgna.story.resources.JointedModelResource;

import java.util.Objects;

/**
 * Manages VisualData lifecycle, scale properties, and visual appearance
 * extracted from JointedModelImp.
 */
class JointedModelVisualManager<R extends JointedModelResource> {

  private JointImplementationAndVisualDataFactory<R> factory;
  private final Scalable sgScalable;
  private VisualData<R> visualData;

  JointedModelVisualManager(JointImplementationAndVisualDataFactory<R> factory,
                            Scalable sgScalable) {
    this.factory = Objects.requireNonNull(factory, "factory");
    this.sgScalable = sgScalable;
    this.visualData = factory.createVisualData();
  }

  VisualData<R> getVisualData() {
    return this.visualData;
  }

  Visual[] getSgVisuals() {
    return this.visualData.getSgVisuals();
  }

  SimpleAppearance[] getSgAppearances() {
    return this.visualData.getSgAppearances();
  }

  void replaceVisualData() {
    this.visualData = this.factory.createVisualData();
  }

  void attachToParent(Composite parent) {
    this.visualData.setSGParent(parent);
  }

  void detachOldVisualData(VisualData<?> oldData) {
    oldData.setSGParent(null);
  }

  boolean hasSgScalable() {
    return this.sgScalable != null;
  }

  Dimension3 getScale() {
    if (this.sgScalable != null) {
      return this.sgScalable.scale.getValue();
    } else {
      Matrix3x3 scale = this.visualData.getSgVisuals()[0].scale.getValue();
      return new Dimension3(scale.getRight().x(), scale.getUp().y(), scale.getBackward().z());
    }
  }

  InstanceProperty[] getScaleProperties() {
    if (this.sgScalable != null) {
      return new InstanceProperty[] {this.sgScalable.scale};
    } else {
      return new InstanceProperty[] {this.visualData.getSgVisuals()[0].scale};
    }
  }

  void setScaleViaSgScalable(Dimension3 scale) {
    this.sgScalable.scale.setValue(scale);
  }

  void setScaleOnVisuals(Dimension3 scale) {
    Matrix3x3 m = scale.asScaleMatrix();
    for (Visual sgVisual : this.visualData.getSgVisuals()) {
      sgVisual.scale.setValue(m);
    }
  }

  void updateFactory(JointImplementationAndVisualDataFactory<R> newFactory) {
    this.factory = Objects.requireNonNull(newFactory, "newFactory");
  }
}
