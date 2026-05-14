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

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.scenegraph.AsSeenBy;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.Point3;

/**
 * Instance helper for place, distance, position, and transformation methods.
 * Extracted from AbstractTransformableImp.
 */
class TransformOperations {

  private static final double DEFAULT_PLACE_ALONG_AXIS_OFFSET = 0.0;

  private final AbstractTransformableImp owner;

  TransformOperations(AbstractTransformableImp owner) {
    this.owner = owner;
  }

  // --- Position ---

  void setPositionOnly(EntityImp target, Point3 offset) {
    owner.getSgComposite().setTranslationOnly(offset != null ? offset : Point3.ORIGIN,
                                              target != null ? target.getSgComposite() : AsSeenBy.SCENE);
  }

  void setPositionOnly(EntityImp target) {
    setPositionOnly(target, Point3.ORIGIN);
  }

  // --- Distance ---

  double getDistanceTo(EntityImp other) {
    Point3 translation = owner.getSgComposite().getTranslation(other.getSgComposite());
    return translation.asVector().magnitude();
  }

  private Point3 getMax(EntityImp entity, ReferenceFrame asSeenBy) {
    if (entity instanceof ModelImp) {
      AxisAlignedBox bbox = entity.getDynamicAxisAlignedMinimumBoundingBox(asSeenBy);
      return bbox.maximum();
    } else {
      return entity.getSgComposite().getTranslation(asSeenBy.getSgReferenceFrame());
    }
  }

  private Point3 getMin(EntityImp entity, ReferenceFrame asSeenBy) {
    if (entity instanceof ModelImp) {
      AxisAlignedBox bbox = entity.getDynamicAxisAlignedMinimumBoundingBox(asSeenBy);
      return bbox.minimum();
    } else {
      return entity.getSgComposite().getTranslation(asSeenBy.getSgReferenceFrame());
    }
  }

  double getDistanceAbove(EntityImp other, ReferenceFrame asSeenBy) {
    return getDistanceAbove(other, owner, asSeenBy);
  }

  double getDistanceBelow(EntityImp other, ReferenceFrame asSeenBy) {
    return getDistanceAbove(owner, other, asSeenBy);
  }

  private double getDistanceAbove(EntityImp a, EntityImp b, ReferenceFrame asSeenBy) {
    return differenceToEpsilon(getMin(b, asSeenBy).y(), getMax(a, asSeenBy).y());
  }

  double getDistanceToTheLeftOf(EntityImp other, ReferenceFrame asSeenBy) {
    return getDistanceToTheLeftOf(owner, other, asSeenBy);
  }

  double getDistanceToTheRightOf(EntityImp other, ReferenceFrame asSeenBy) {
    return getDistanceToTheLeftOf(other, owner, asSeenBy);
  }

  private double getDistanceToTheLeftOf(EntityImp a, EntityImp b, ReferenceFrame asSeenBy) {
    return differenceToEpsilon(getMin(b, asSeenBy).x(), getMax(a, asSeenBy).x());
  }

  double getDistanceBehind(EntityImp other, ReferenceFrame asSeenBy) {
    return getDistanceBehind(owner, other, asSeenBy);
  }

  double getDistanceInFrontOf(EntityImp other, ReferenceFrame asSeenBy) {
    return getDistanceBehind(other, owner, asSeenBy);
  }

  private double getDistanceBehind(EntityImp a, EntityImp b, ReferenceFrame asSeenBy) {
    //Front and back calculations are flipped because -Z is front
    return differenceToEpsilon(getMin(a, asSeenBy).z(), getMax(b, asSeenBy).z());
  }

  double differenceToEpsilon(double a, double b) {
    double value = a - b;
    if (Math.abs(value) < .01d) {
      return 0;
    }
    return value;
  }

  // --- Place ---

  static class PlaceData {
    final AbstractTransformableImp subject;
    private final SpatialRelationImp spatialRelation;
    private final EntityImp target;
    private final double alongAxisOffset;
    private final ReferenceFrame asSeenBy;

    PlaceData(AbstractTransformableImp subject, SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset, ReferenceFrame asSeenBy) {
      assert subject != null;
      assert asSeenBy != null;
      assert spatialRelation != null;
      assert !Double.isNaN(alongAxisOffset);
      this.subject = subject;
      this.spatialRelation = spatialRelation;
      this.target = target;
      this.alongAxisOffset = alongAxisOffset;
      this.asSeenBy = asSeenBy;
    }

    AffineMatrix4x4 calculateTranslation0() {
      return this.subject.getTransformation(this.asSeenBy);
    }

    AffineMatrix4x4 calculateTranslation1(AffineMatrix4x4 t0) {
      AxisAlignedBox bbSubject = this.subject.getAxisAlignedMinimumBoundingBox();
      if (this.target != null) {
        AxisAlignedBox bbTarget;
        bbTarget = this.target.getAxisAlignedMinimumBoundingBox(this.asSeenBy);
        assert bbSubject != null;
        assert bbTarget != null;
        if (bbSubject.isNaN() || bbTarget.isNaN()) {
          Logger.severe("bounding box is NaN", bbSubject, bbTarget);
          return t0;
        } else {
          AffineMatrix4x4 m = this.target.getTransformation(this.asSeenBy);
          return new AffineMatrix4x4(m.orientation(), this.spatialRelation.getPlaceLocation(this.alongAxisOffset, bbSubject, bbTarget));
        }
      } else {
        double y = bbSubject.isNaN() ? 0 : -bbSubject.minimum().y();
        return AffineMatrix4x4.createTranslation(t0.translation().x(), y, t0.translation().z());
      }
    }

    public void setTranslation(AffineMatrix4x4 translation) {
      this.subject.getSgComposite().setTransformation(translation, this.asSeenBy.getSgReferenceFrame());
    }
  }

  void place(SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset, ReferenceFrame asSeenBy) {
    PlaceData placeData = new PlaceData(owner, spatialRelation, target, alongAxisOffset, asSeenBy);
    placeData.setTranslation(placeData.calculateTranslation1(placeData.calculateTranslation0()));
  }

  void place(SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset) {
    place(spatialRelation, target, alongAxisOffset, target);
  }

  void place(SpatialRelationImp spatialRelation, EntityImp target) {
    place(spatialRelation, target, DEFAULT_PLACE_ALONG_AXIS_OFFSET);
  }

  // --- Transformation ---

  void setTransformation(ReferenceFrame target, AffineMatrix4x4 offset) {
    assert target != null : owner;
    assert owner.getSgComposite() != null : owner;
    if (offset == null) {
      offset = AffineMatrix4x4.IDENTITY;
    }
    owner.getSgComposite().setTransformation(offset, target.getSgReferenceFrame());
  }

  void setTransformation(ReferenceFrame target) {
    setTransformation(target, AffineMatrix4x4.IDENTITY);
  }
}
