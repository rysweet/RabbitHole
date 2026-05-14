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
import edu.cmu.cs.dennisc.math.animation.AffineMatrix4x4Animation;
import edu.cmu.cs.dennisc.math.animation.Point3Animation;
import edu.cmu.cs.dennisc.math.animation.UnitQuaternionAnimation;
import edu.cmu.cs.dennisc.math.polynomial.HermiteCubic;
import edu.cmu.cs.dennisc.scenegraph.AsSeenBy;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Angle;
import org.alice.math.immutable.ForwardAndUpGuide;
import org.alice.math.immutable.Orientation;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.UnitQuaternion;
import org.alice.math.immutable.Vector3;

/**
 * Instance helper for all animate* methods and orientation data hierarchy.
 * Extracted from AbstractTransformableImp.
 */
class TransformAnimator {

  private static final boolean DEFAULT_IS_SMOOTH = true;

  private final AbstractTransformableImp owner;

  TransformAnimator(AbstractTransformableImp owner) {
    this.owner = owner;
  }

  // --- Translation animation ---

  void animateApplyTranslation(Point3 translation, ReferenceFrame asSeenBy, double duration, Style style) {
    assert !translation.isNaN();
    assert duration >= 0 : "Invalid argument: duration " + duration + " must be >= 0";
    assert style != null;
    assert asSeenBy != null;
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      owner.applyTranslation(translation, asSeenBy);
      owner.applyAnimation();
    } else {
      class TranslateAnimation extends DurationBasedAnimation {
        private final ReferenceFrame asSeenBy;
        private final double x;
        private final double y;
        private final double z;
        private double xSum;
        private double ySum;
        private double zSum;

        private TranslateAnimation(Number duration, Style style, Point3 translation, ReferenceFrame asSeenBy) {
          super(duration, style);
          this.x = translation.x();
          this.y = translation.y();
          this.z = translation.z();
          this.asSeenBy = asSeenBy;
        }

        @Override
        protected void prologue() {
          this.xSum = 0;
          this.ySum = 0;
          this.zSum = 0;
        }

        @Override
        protected void setPortion(double portion) {
          double xPortion = (this.x * portion) - this.xSum;
          double yPortion = (this.y * portion) - this.ySum;
          double zPortion = (this.z * portion) - this.zSum;

          owner.applyTranslation(xPortion, yPortion, zPortion, this.asSeenBy);

          this.xSum += xPortion;
          this.ySum += yPortion;
          this.zSum += zPortion;
        }

        @Override
        protected void epilogue() {
          owner.applyTranslation(this.x - this.xSum, this.y - this.ySum, this.z - this.zSum, this.asSeenBy);
        }

        @Override
        public Animated getAnimated() {
          return owner;
        }
      }
      owner.perform(new TranslateAnimation(duration, style, translation, asSeenBy));
    }
  }

  void animateApplyTranslation(double x, double y, double z, ReferenceFrame asSeenBy, double duration, Style style) {
    animateApplyTranslation(new Point3(x, y, z), asSeenBy, duration, style);
  }

  // --- Rotation animation ---

  void animateApplyRotationInRadians(Vector3 axis, double angleInRadians, ReferenceFrame asSeenBy, double duration, Style style) {
    assert axis != null;
    assert duration >= 0 : "Invalid argument: duration " + duration + " must be >= 0";
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      owner.applyRotationInRadians(axis, angleInRadians, asSeenBy);
      owner.applyAnimation();
    } else {
      class RotateAnimation extends DurationBasedAnimation {
        private final ReferenceFrame asSeenBy;
        private final Vector3 axis;
        private final double angleInRadians;
        private double angleSumInRadians;

        private RotateAnimation(Number duration, Style style, Vector3 axis, double angleInRadians, ReferenceFrame asSeenBy) {
          super(duration, style);
          this.axis = axis;
          this.angleInRadians = angleInRadians;
          this.asSeenBy = asSeenBy;
        }

        @Override
        protected void prologue() {
          this.angleSumInRadians = 0;
        }

        @Override
        protected void setPortion(double portion) {
          double anglePortionInRadians = (this.angleInRadians * portion) - this.angleSumInRadians;

          owner.applyRotationInRadians(this.axis, anglePortionInRadians, this.asSeenBy);

          this.angleSumInRadians += anglePortionInRadians;
        }

        @Override
        protected void epilogue() {
          owner.applyRotationInRadians(this.axis, this.angleInRadians - this.angleSumInRadians, this.asSeenBy);
        }

        @Override
        public Animated getAnimated() {
          return owner;
        }
      }
      owner.perform(new RotateAnimation(duration, style, axis, angleInRadians, asSeenBy));
    }
  }

  void animateApplyRotationInRevolutions(Vector3 axis, double angleInRevolutions, ReferenceFrame asSeenBy, double duration, Style style) {
    animateApplyRotationInRadians(axis, angleInRevolutions * Angle.REVOLUTIONS_TO_RADIANS, asSeenBy, duration, style);
  }

  // --- Orientation data hierarchy ---

  private abstract static class OrientationData {
    private final AbstractTransformableImp subject;

    OrientationData(AbstractTransformableImp subject) {
      this.subject = subject;
    }

    public AbstractTransformableImp getSubject() {
      return this.subject;
    }

    protected abstract void setM(OrthogonalMatrix3x3 m);

    protected final void setQ(UnitQuaternion q) {
      this.setM(q.asMatrix3x3());
    }

    protected abstract OrthogonalMatrix3x3 getM0();

    protected abstract OrthogonalMatrix3x3 getM1();

    protected abstract UnitQuaternion getQ0();

    protected abstract UnitQuaternion getQ1();

    public void setPortion(double portion) {
      UnitQuaternion q0 = this.getQ0();
      UnitQuaternion q1 = this.getQ1();
      assert !q0.isNaN() : this;
      assert !q1.isNaN() : this;
      this.setQ(q0.interpolate(q1, portion));
    }

    public void epilogue() {
      this.setM(this.getM1());
    }
  }

  private abstract static class PreSetOrientationData extends OrientationData {
    private final OrthogonalMatrix3x3 m0;
    private final OrthogonalMatrix3x3 m1;
    private UnitQuaternion q0;
    private UnitQuaternion q1;

    PreSetOrientationData(AbstractTransformableImp subject, OrthogonalMatrix3x3 m0, OrthogonalMatrix3x3 m1) {
      super(subject);
      this.m0 = m0;
      this.m1 = m1;
    }

    @Override
    protected final OrthogonalMatrix3x3 getM0() {
      return this.m0;
    }

    @Override
    protected final OrthogonalMatrix3x3 getM1() {
      return this.m1;
    }

    @Override
    protected UnitQuaternion getQ0() {
      if (this.q0 == null) {
        this.q0 = this.m0.asUnitQuaternion();
      }
      return this.q0;
    }

    @Override
    protected UnitQuaternion getQ1() {
      if (this.q1 == null) {
        this.q1 = this.m1.asUnitQuaternion();
      }
      return this.q1;
    }
  }

  private static class LocalOrientationData extends PreSetOrientationData {
    LocalOrientationData(AbstractTransformableImp subject, OrthogonalMatrix3x3 m1) {
      super(subject, subject.getSgComposite().getLocalTransformation().orientation(), m1);
    }

    @Override
    protected void setM(OrthogonalMatrix3x3 orientation) {
      AffineMatrix4x4 prevM = this.getSubject().getSgComposite().getLocalTransformation();
      AffineMatrix4x4 nextM = new AffineMatrix4x4(orientation, prevM.translation());
      this.getSubject().getSgComposite().setLocalTransformation(nextM);
    }
  }

  private static class TurnToFaceOrientationData extends LocalOrientationData {
    TurnToFaceOrientationData(AbstractTransformableImp subject, EntityImp target) {
      super(subject, VehicleManager.calculateTurnToFaceAxes(subject, target));
    }
  }

  private static class OrientToUprightData extends PreSetOrientationData {
    private final edu.cmu.cs.dennisc.scenegraph.ReferenceFrame sgRef;

    public static OrientToUprightData createInstance(AbstractTransformableImp subject, ReferenceFrame upAsSeenBy) {
      OrthogonalMatrix3x3 orientation0 = subject.getTransformation(upAsSeenBy).orientation();
      OrthogonalMatrix3x3 orientation1 = orientation0.asStandUp();
      return new OrientToUprightData(subject, orientation0, orientation1, upAsSeenBy);
    }

    private OrientToUprightData(AbstractTransformableImp subject, OrthogonalMatrix3x3 orientation0, OrthogonalMatrix3x3 orientation1, ReferenceFrame upAsSeenBy) {
      super(subject, orientation0, orientation1);
      this.sgRef = upAsSeenBy.getSgReferenceFrame();
    }

    @Override
    protected void setM(OrthogonalMatrix3x3 m) {
      this.getSubject().getSgComposite().setAxesOnly(m, this.sgRef);
    }
  }

  private static class OrientToPointAtData extends PreSetOrientationData {
    private final edu.cmu.cs.dennisc.scenegraph.ReferenceFrame sgRef;

    public static OrientToPointAtData createInstance(AbstractTransformableImp subject, EntityImp target, ReferenceFrame upAsSeenBy) {
      AffineMatrix4x4 m0 = subject.getTransformation(upAsSeenBy);
      Point3 t0 = m0.translation();
      Point3 t1 = target.getTransformation(upAsSeenBy).translation();
      Vector3 forward = t1.minus(t0);
      OrthogonalMatrix3x3 o1;
      if (forward.isZero()) {
        o1 = m0.orientation();
        //no op
      } else {
        o1 = new ForwardAndUpGuide(forward, null).asMatrix3x3();
      }
      return new OrientToPointAtData(subject, m0.orientation(), o1, upAsSeenBy);
    }

    private OrientToPointAtData(AbstractTransformableImp subject, OrthogonalMatrix3x3 orientation0, OrthogonalMatrix3x3 orientation1, ReferenceFrame upAsSeenBy) {
      super(subject, orientation0, orientation1);
      this.sgRef = upAsSeenBy.getSgReferenceFrame();
    }

    @Override
    protected void setM(OrthogonalMatrix3x3 m) {
      this.getSubject().getSgComposite().setAxesOnly(m, this.sgRef);
    }
  }

  // --- Orientation methods ---

  private void setOrientationOnly(OrientationData data) {
    data.epilogue();
  }

  private void animateOrientationOnly(final OrientationData data, double duration, Style style) {
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      data.epilogue();
    } else {
      owner.perform(new DurationBasedAnimation(duration, style) {
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

  void setLocalOrientationOnly(OrthogonalMatrix3x3 localOrientation) {
    setOrientationOnly(new LocalOrientationData(owner, localOrientation));
  }

  void animateLocalOrientationOnly(OrthogonalMatrix3x3 localOrientation, double duration, Style style) {
    animateOrientationOnly(new LocalOrientationData(owner, localOrientation), duration, style);
  }

  private void setOrientationOnly(EntityImp target, Orientation offset) {
    owner.getSgComposite().setAxesOnly(offset != null ? offset : OrthogonalMatrix3x3.IDENTITY, target.getSgReferenceFrame());
  }

  void animateOrientationOnly(final EntityImp target, Orientation offset, double duration, Style style) {
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      setOrientationOnly(target, offset);
      owner.applyAnimation();
    } else {
      final OrthogonalMatrix3x3 targetOrientation = owner.getTransformation(target).orientation().normalized();
      UnitQuaternion q0 = targetOrientation.asUnitQuaternion();
      UnitQuaternion q1 = offset == null ? UnitQuaternion.IDENTITY : offset.asUnitQuaternion();
      owner.perform(new UnitQuaternionAnimation(duration, style, q0, q1) {
        @Override
        protected void updateValue(UnitQuaternion q) {
          setOrientationOnly(target, q);
        }
        @Override
        public Animated getAnimated() {
          return owner;
        }
      });
    }
  }

  void animateOrientationOnlyToFace(EntityImp target, Point3 offset, double duration, Style style) {
    animateOrientationOnly(new TurnToFaceOrientationData(owner, target), duration, style);
  }

  void animateOrientationToUpright(ReferenceFrame upAsSeenBy, double duration, Style style) {
    animateOrientationOnly(OrientToUprightData.createInstance(owner, upAsSeenBy), duration, style);
  }

  void animateOrientationToPointAt(EntityImp target, ReferenceFrame upAsSeenBy, double duration, Style style) {
    animateOrientationOnly(OrientToPointAtData.createInstance(owner, target, upAsSeenBy), duration, style);
  }

  void setOrientationOnlyToPointAt(ReferenceFrame target) {
    owner.getSgComposite().setAxesOnlyToPointAt(target.getActualEntityImplementation(owner).getSgComposite());
  }

  // --- Position animation ---

  private abstract static class SmoothAffineMatrix4x4Animation extends DurationBasedAnimation {
    final AffineMatrix4x4 m1;
    final HermiteCubic xHermite;
    final HermiteCubic yHermite;
    final HermiteCubic zHermite;

    SmoothAffineMatrix4x4Animation(AffineMatrix4x4 m0, AffineMatrix4x4 m1, double duration, Style style) {
      super(duration, style);
      this.m1 = m1;

      double s = -8;
      Point3 t0 = m0.translation();
      Point3 t1 = m1.translation();
      Vector3 b0 = m0.orientation().backward();
      Vector3 b1 = m1.orientation().backward();
      this.xHermite = new HermiteCubic(t0.x(), t1.x(), s * b0.x(), s * b1.x());
      this.yHermite = new HermiteCubic(t0.y(), t1.y(), s * b0.y(), s * b1.y());
      this.zHermite = new HermiteCubic(t0.z(), t1.z(), s * b0.z(), s * b1.z());
    }

    @Override
    protected void prologue() {
    }
  }

  private static class SmoothPositionAnimation extends SmoothAffineMatrix4x4Animation {
    private final AbstractTransformableImp subject;
    private final edu.cmu.cs.dennisc.scenegraph.ReferenceFrame sgRef;

    SmoothPositionAnimation(AbstractTransformableImp subject, AffineMatrix4x4 m1, ReferenceFrame asSeenBy, double duration, Style style) {
      super(subject.getTransformation(asSeenBy), m1, duration, style);
      this.subject = subject;
      this.sgRef = asSeenBy.getSgReferenceFrame();
    }

    @Override
    public Animated getAnimated() {
      return subject;
    }

    @Override
    protected void setPortion(double portion) {
      double x = this.xHermite.evaluate(portion);
      double y = this.yHermite.evaluate(portion);
      double z = this.zHermite.evaluate(portion);

      this.subject.getSgComposite().setTranslationOnly(x, y, z, this.sgRef);
    }

    @Override
    protected void epilogue() {
      this.subject.getSgComposite().setTranslationOnly(this.m1.translation(), this.sgRef);
    }
  }

  private void setPositionOnly(EntityImp target, Point3 offset) {
    owner.getSgComposite().setTranslationOnly(offset != null ? offset : Point3.ORIGIN,
                                              target != null ? target.getSgComposite() : AsSeenBy.SCENE);
  }

  void animatePositionOnly(final EntityImp target, Point3 offset, boolean isSmooth, double duration, Style style) {
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      setPositionOnly(target, offset);
      owner.applyAnimation();
    } else {
      if (isSmooth) {
        owner.perform(new SmoothPositionAnimation(owner, AffineMatrix4x4.IDENTITY, target, duration, style));
      } else {
        AffineMatrix4x4 m0 = owner.getTransformation(target);
        owner.perform(new Point3Animation(duration, style, m0.translation(), offset != null ? offset : Point3.ORIGIN) {
          @Override
          public Animated getAnimated() {
            return owner;
          }

          @Override
          protected void updateValue(Point3 t) {
            setPositionOnly(target, t);
          }
        });
      }
    }
  }

  // --- Place animation ---

  private static class PlaceAnimation extends DurationBasedAnimation {
    private final TransformOperations.PlaceData placeData;
    private Point3 p0;
    private UnitQuaternion q0;
    private Point3 p1;
    private UnitQuaternion q1;
    private AffineMatrix4x4 finalTransform;

    PlaceAnimation(TransformOperations.PlaceData placeData, double duration, Style style) {
      super(duration, style);
      this.placeData = placeData;
    }

    @Override
    protected void prologue() {
      AffineMatrix4x4 m0 = this.placeData.calculateTranslation0();
      AffineMatrix4x4 m1 = this.placeData.calculateTranslation1(m0);
      this.p0 = m0.translation();
      this.q0 = m0.orientation().asUnitQuaternion();
      this.p1 = m1.translation();
      this.q1 = m1.orientation().asUnitQuaternion();
      this.finalTransform = m1;
    }

    @Override
    protected void setPortion(double portion) {
      Point3 p = p0.interpolate(p1, portion);
      UnitQuaternion q = q0.interpolate(q1, portion);
      this.placeData.setTranslation(new AffineMatrix4x4(q.asMatrix3x3(), p));
    }

    @Override
    public Animated getAnimated() {
      return placeData.subject;
    }

    @Override
    protected void epilogue() {
      this.placeData.setTranslation(this.finalTransform);
    }
  }

  void animatePlace(SpatialRelationImp spatialRelation, EntityImp target, double alongAxisOffset, ReferenceFrame asSeenBy, boolean isSmooth, double duration, Style style) {
    TransformOperations.PlaceData placeData = new TransformOperations.PlaceData(owner, spatialRelation, target, alongAxisOffset, asSeenBy);
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      AffineMatrix4x4 m0 = placeData.calculateTranslation0();
      assert !m0.isNaN() : owner;
      AffineMatrix4x4 m1 = placeData.calculateTranslation1(m0);
      assert !m1.isNaN() : owner;
      placeData.setTranslation(m1);
    } else {
      owner.perform(new PlaceAnimation(placeData, duration, style));
    }
  }

  // --- Transformation animation ---

  void animateTransformation(final ReferenceFrame target, AffineMatrix4x4 offset, boolean isSmooth, double duration, Style style) {
    duration = owner.adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, PropertyOwnerImp.RIGHT_NOW)) {
      if ((offset == null) || !offset.isNaN()) {
        owner.setTransformation(target, offset);
        owner.applyAnimation();
      }
    } else {
      AffineMatrix4x4 m1;
      if (offset != null) {
        m1 = offset;
      } else {
        m1 = AffineMatrix4x4.IDENTITY;
      }
      AffineMatrix4x4 m0 = owner.getTransformation(target);
      owner.perform(new AffineMatrix4x4Animation(duration, style, m0, m1) {
        @Override
        public Animated getAnimated() {
          return owner;
        }

        @Override
        protected void updateValue(AffineMatrix4x4 m) {
          owner.getSgComposite().setTransformation(m, target.getSgReferenceFrame());
        }

        @Override
        protected void epilogue() {
          super.epilogue();
          owner.getSgComposite().notifyTransformationListeners();
        }
      });
    }
  }

  void animateTransformation(ReferenceFrame target, AffineMatrix4x4 offset) {
    animateTransformation(target, offset, DEFAULT_IS_SMOOTH, EntityImp.DEFAULT_DURATION, EntityImp.DEFAULT_STYLE);
  }
}
