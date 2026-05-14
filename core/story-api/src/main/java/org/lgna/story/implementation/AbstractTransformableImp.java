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

import edu.cmu.cs.dennisc.animation.Animated;
import edu.cmu.cs.dennisc.animation.DurationBasedAnimation;
import edu.cmu.cs.dennisc.animation.Style;
import edu.cmu.cs.dennisc.math.EpsilonUtilities;
import edu.cmu.cs.dennisc.scenegraph.AbstractTransformable;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Angle;
import org.alice.math.immutable.Orientation;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.UnitQuaternion;
import org.alice.math.immutable.Vector3;

/**
 * @author Dennis Cosgrove
 */
public abstract class AbstractTransformableImp extends EntityImp implements Animated {

  private final TransformOperations transformOps = new TransformOperations(this);
  private final TransformAnimator transformAnimator = new TransformAnimator(this);

  @Override
  public abstract AbstractTransformable getSgComposite();

  public boolean isFacing(EntityImp other) {
    AffineMatrix4x4 m = other.getTransformation(this);
    return m.translation().z() < 0.0;
  }

  @Override
  public void applyAnimation() {
    getSgComposite().notifyTransformationListeners();
  }

  public AffineMatrix4x4 getLocalTransformation() {
    return this.getSgComposite().getLocalTransformation();
  }

  public Point3 getLocalPosition() {
    return this.getLocalTransformation().translation();
  }

  public OrthogonalMatrix3x3 getLocalOrientation() {
    return this.getLocalTransformation().orientation();
  }

  public void setLocalTransformation(AffineMatrix4x4 transformation) {
    this.getSgComposite().setLocalTransformation(transformation);
  }

  void setLocalOrientation(OrthogonalMatrix3x3 orientation) {
    AffineMatrix4x4 m = this.getLocalTransformation();
    this.setLocalTransformation(new AffineMatrix4x4(orientation, m.translation()));
  }

  @Override
  protected void postCheckSetVehicle(EntityImp vehicle) {
    SceneImp scene = this.getScene();
    AffineMatrix4x4 absTransform = scene != null ? this.getTransformation(scene) : null;
    super.postCheckSetVehicle(vehicle);
    if ((vehicle != null) && (scene != null)) {
      this.setTransformation(scene, absTransform);
      applyAnimation();
    }
  }

  // --- Direct transform operations (kept here for subclass inheritance) ---

  public void applyTranslation(double x, double y, double z, ReferenceFrame asSeenBy) {
    this.getSgComposite().applyTranslation(x, y, z, asSeenBy.getSgReferenceFrame());
  }

  public void applyTranslation(Point3 translation, ReferenceFrame asSeenBy) {
    this.applyTranslation(translation.x(), translation.y(), translation.z(), asSeenBy);
  }

  public void applyRotationInRadians(Vector3 axis, double angleInRadians, ReferenceFrame asSeenBy) {
    this.getSgComposite().applyRotationAboutArbitraryAxisInRadians(axis, angleInRadians, asSeenBy.getSgReferenceFrame());
  }

  public void applyRotationInRadians(Vector3 axis, double angleInRadians) {
    this.applyRotationInRadians(axis, angleInRadians, this);
  }

  private void applyRotationInRevolutions(Vector3 axis, double angleInRevolutions, ReferenceFrame asSeenBy) {
    this.applyRotationInRadians(axis, angleInRevolutions * Angle.REVOLUTIONS_TO_RADIANS, asSeenBy);
  }

  public void applyRotationInRevolutions(Vector3 axis, double angleInRadians) {
    this.applyRotationInRevolutions(axis, angleInRadians, this);
  }

  // --- VantagePoint data (protected subclass API) ---

  protected abstract static class VantagePointData {
    private final AbstractTransformableImp subject;

    VantagePointData(AbstractTransformableImp subject) {
      this.subject = subject;
    }

    public AbstractTransformableImp getSubject() {
      return this.subject;
    }

    protected abstract void setM(AffineMatrix4x4 m);

    protected abstract AffineMatrix4x4 getM0();

    protected abstract AffineMatrix4x4 getM1();

    protected abstract Point3 getT0();

    protected abstract Point3 getT1();

    protected abstract UnitQuaternion getQ0();

    protected abstract UnitQuaternion getQ1();

    public void setPortion(double portion) {
      Point3 t = getT0().interpolate(getT1(), portion);
      UnitQuaternion q = getQ0().interpolate(getQ1(), portion);
      this.setM(new AffineMatrix4x4(q.asMatrix3x3(), t));
    }

    public void epilogue() {
      this.setM(this.getM1());
    }
  }

  protected static class PreSetVantagePointData extends VantagePointData {
    private final EntityImp other;
    private final AffineMatrix4x4 m0;
    private final AffineMatrix4x4 m1;
    private final UnitQuaternion q0;
    private final UnitQuaternion q1;

    PreSetVantagePointData(AbstractTransformableImp subject, EntityImp other) {
      super(subject);
      this.other = other;
      this.m0 = subject.getTransformation(other);
      this.m1 = AffineMatrix4x4.IDENTITY;
      this.q0 = this.m0.orientation().asUnitQuaternion();
      this.q1 = this.m1.orientation().asUnitQuaternion();
    }

    @Override
    protected AffineMatrix4x4 getM0() {
      return this.m0;
    }

    @Override
    protected AffineMatrix4x4 getM1() {
      return this.m1;
    }

    @Override
    protected UnitQuaternion getQ0() {
      return this.q0;
    }

    @Override
    protected UnitQuaternion getQ1() {
      return this.q1;
    }

    @Override
    protected Point3 getT0() {
      return this.m0.translation();
    }

    @Override
    protected Point3 getT1() {
      return this.m1.translation();
    }

    @Override
    protected void setM(AffineMatrix4x4 m) {
      this.getSubject().getSgComposite().setTransformation(m, other.getSgReferenceFrame());
    }
  }

  void animateVantagePoint(final VantagePointData data, double duration, Style style) {
    duration = adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, RIGHT_NOW)) {
      data.epilogue();
    } else {
      perform(new DurationBasedAnimation(duration, style) {
        @Override
        public Animated getAnimated() {
          return data.subject;
        }

        @Override
        protected void prologue() {
        }

        @Override
        protected void setPortion(double portion) {
          data.setPortion(portion);
        }

        @Override
        protected void epilogue() {
          data.epilogue();
        }
      });
    }
  }

  // --- Delegation facades: animation ---

  public void animateApplyTranslation(Point3 translation, ReferenceFrame asSeenBy, double duration, Style style) {
    transformAnimator.animateApplyTranslation(translation, asSeenBy, duration, style);
  }

  public void animateApplyTranslation(double x, double y, double z, ReferenceFrame asSeenBy, double duration, Style style) {
    transformAnimator.animateApplyTranslation(x, y, z, asSeenBy, duration, style);
  }

  public void animateApplyRotationInRevolutions(Vector3 axis, double angleInRevolutions, ReferenceFrame asSeenBy, double duration, Style style) {
    transformAnimator.animateApplyRotationInRevolutions(axis, angleInRevolutions, asSeenBy, duration, style);
  }

  void setLocalOrientationOnly(OrthogonalMatrix3x3 localOrientation) {
    transformAnimator.setLocalOrientationOnly(localOrientation);
  }

  public void animateLocalOrientationOnly(final OrthogonalMatrix3x3 localOrientation, double duration, Style style) {
    transformAnimator.animateLocalOrientationOnly(localOrientation, duration, style);
  }

  public void animateOrientationOnly(final EntityImp target, Orientation offset, double duration, Style style) {
    transformAnimator.animateOrientationOnly(target, offset, duration, style);
  }

  public void animateOrientationOnlyToFace(EntityImp target, Point3 offset, double duration, Style style) {
    transformAnimator.animateOrientationOnlyToFace(target, offset, duration, style);
  }

  public void animateOrientationToUpright(ReferenceFrame upAsSeenBy, double duration, Style style) {
    transformAnimator.animateOrientationToUpright(upAsSeenBy, duration, style);
  }

  public void animateOrientationToPointAt(EntityImp target, ReferenceFrame upAsSeenBy, double duration, Style style) {
    transformAnimator.animateOrientationToPointAt(target, upAsSeenBy, duration, style);
  }

  public void setOrientationOnlyToPointAt(ReferenceFrame target) {
    transformAnimator.setOrientationOnlyToPointAt(target);
  }

  public void animatePositionOnly(final EntityImp target, Point3 offset, boolean isSmooth, double duration, Style style) {
    transformAnimator.animatePositionOnly(target, offset, isSmooth, duration, style);
  }

  public void animateTransformation(final ReferenceFrame target, AffineMatrix4x4 offset, boolean isSmooth, double duration, Style style) {
    transformAnimator.animateTransformation(target, offset, isSmooth, duration, style);
  }

  public void animateTransformation(ReferenceFrame target, AffineMatrix4x4 offset) {
    transformAnimator.animateTransformation(target, offset);
  }

  public void animatePlace(SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset, ReferenceFrame asSeenBy, boolean isSmooth, double duration, Style style) {
    transformAnimator.animatePlace(spatialRelation, target, alongAxisOffset, asSeenBy, isSmooth, duration, style);
  }

  // --- Delegation facades: transform operations ---

  void setPositionOnly(EntityImp target) {
    transformOps.setPositionOnly(target);
  }

  public void place(SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset, ReferenceFrame asSeenBy) {
    transformOps.place(spatialRelation, target, alongAxisOffset, asSeenBy);
  }

  public void place(SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset) {
    transformOps.place(spatialRelation, target, alongAxisOffset);
  }

  public void place(SpatialRelationImp spatialRelation, EntityImp target) {
    transformOps.place(spatialRelation, target);
  }

  public void setTransformation(ReferenceFrame target, AffineMatrix4x4 offset) {
    transformOps.setTransformation(target, offset);
  }

  public void setTransformation(ReferenceFrame target) {
    transformOps.setTransformation(target);
  }

  public double getDistanceTo(EntityImp other) {
    return transformOps.getDistanceTo(other);
  }

  public double getDistanceAbove(EntityImp other, ReferenceFrame asSeenBy) {
    return transformOps.getDistanceAbove(other, asSeenBy);
  }

  public double getDistanceBelow(EntityImp other, ReferenceFrame asSeenBy) {
    return transformOps.getDistanceBelow(other, asSeenBy);
  }

  public double getDistanceToTheLeftOf(EntityImp other, ReferenceFrame asSeenBy) {
    return transformOps.getDistanceToTheLeftOf(other, asSeenBy);
  }

  public double getDistanceToTheRightOf(EntityImp other, ReferenceFrame asSeenBy) {
    return transformOps.getDistanceToTheRightOf(other, asSeenBy);
  }

  public double getDistanceBehind(EntityImp other, ReferenceFrame asSeenBy) {
    return transformOps.getDistanceBehind(other, asSeenBy);
  }

  public double getDistanceInFrontOf(EntityImp other, ReferenceFrame asSeenBy) {
    return transformOps.getDistanceInFrontOf(other, asSeenBy);
  }
}
