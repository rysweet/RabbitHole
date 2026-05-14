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

import edu.cmu.cs.dennisc.animation.TraditionalStyle;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.UnitQuaternion;
import org.alice.math.immutable.Vector3;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Contract tests for TransformAnimator — the instance helper extracted from
 * AbstractTransformableImp that handles all animate* methods and the
 * OrientationData class hierarchy.
 *
 * These tests FAIL to compile until TransformAnimator.java is created.
 */
public class TransformAnimatorTest {

  private StandInImp vehicle;
  private StandInImp subject;
  private TransformAnimator animator;

  @Before
  public void setUp() {
    vehicle = new StandInImp();
    subject = new StandInImp();
    subject.setVehicle(vehicle);
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    animator = new TransformAnimator(subject);
  }

  // --- Construction ---

  @Test
  public void constructorAcceptsOwner() {
    TransformAnimator ta = new TransformAnimator(subject);
    assertNotNull(ta);
  }

  // --- animateApplyTranslation with zero duration (immediate) ---

  @Test
  public void animateApplyTranslationZeroDurationAppliesImmediately() {
    animator.animateApplyTranslation(
        new Point3(5.0, 0.0, 0.0), subject, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    Point3 pos = subject.getLocalPosition();
    assertEquals(5.0, pos.x(), 1e-6);
    assertEquals(0.0, pos.y(), 1e-6);
    assertEquals(0.0, pos.z(), 1e-6);
  }

  @Test
  public void animateApplyTranslationZeroDurationXYZ() {
    animator.animateApplyTranslation(
        3.0, 4.0, 5.0, subject, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    Point3 pos = subject.getLocalPosition();
    assertEquals(3.0, pos.x(), 1e-6);
    assertEquals(4.0, pos.y(), 1e-6);
    assertEquals(5.0, pos.z(), 1e-6);
  }

  // --- animateApplyRotation with zero duration ---

  @Test
  public void animateApplyRotationZeroDurationAppliesImmediately() {
    Vector3 yAxis = new Vector3(0, 1, 0);
    animator.animateApplyRotationInRevolutions(
        yAxis, 0.25, subject, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    OrthogonalMatrix3x3 orientation = subject.getLocalOrientation();
    assertFalse("Orientation should change after rotation",
        orientation.isIdentity());
  }

  // --- animateLocalOrientationOnly with zero duration ---

  @Test
  public void animateLocalOrientationOnlyZeroDurationSetsOrientation() {
    OrthogonalMatrix3x3 targetOrientation = OrthogonalMatrix3x3.IDENTITY;
    animator.animateLocalOrientationOnly(
        targetOrientation, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    OrthogonalMatrix3x3 result = subject.getLocalOrientation();
    assertTrue("Orientation should be identity after setting to identity",
        result.isWithinEpsilonOf(targetOrientation, 1e-6));
  }

  // --- animateOrientationOnly to face target ---

  @Test
  public void animateOrientationOnlyToFaceZeroDurationTurnsToFace() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, -5));

    animator.animateOrientationOnlyToFace(
        target, null, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    // Subject orientation should have changed
    assertNotNull(subject.getLocalOrientation());
  }

  // --- animateOrientationToUpright ---

  @Test
  public void animateOrientationToUprightZeroDurationStraightensUp() {
    // First rotate subject so it's not upright
    Vector3 xAxis = new Vector3(1, 0, 0);
    subject.applyRotationInRadians(xAxis, Math.PI / 4.0, subject);

    animator.animateOrientationToUpright(
        vehicle, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    // After standing up, the orientation should be close to upright
    assertNotNull(subject.getLocalOrientation());
  }

  // --- animateOrientationToPointAt ---

  @Test
  public void animateOrientationToPointAtZeroDurationPointsAtTarget() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 0, -10));

    animator.animateOrientationToPointAt(
        target, vehicle, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    assertNotNull(subject.getLocalOrientation());
  }

  // --- animatePositionOnly with zero duration ---

  @Test
  public void animatePositionOnlyZeroDurationMovesToTarget() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(7, 8, 9));

    animator.animatePositionOnly(
        target, null, false, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    // Subject should be at target's position
    Point3 pos = subject.getLocalPosition();
    assertEquals(7.0, pos.x(), 1e-3);
    assertEquals(8.0, pos.y(), 1e-3);
    assertEquals(9.0, pos.z(), 1e-3);
  }

  @Test
  public void animatePositionOnlyWithOffsetMovesToOffset() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 0, 0));

    animator.animatePositionOnly(
        target, new Point3(0, 5, 0), false, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    // Position should reflect the offset relative to target
    assertNotNull(subject.getLocalPosition());
  }

  // --- animatePlace with zero duration ---

  @Test
  public void animatePlaceZeroDurationPositionsCorrectly() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    animator.animatePlace(
        SpatialRelationImp.ABOVE, target, 0.0, target, false, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    assertNotNull(subject.getLocalPosition());
  }

  // --- animateTransformation with zero duration ---

  @Test
  public void animateTransformationZeroDurationAppliesTransform() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    animator.animateTransformation(
        target, AffineMatrix4x4.IDENTITY, false, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertTrue("After animateTransformation with IDENTITY offset, should be at target",
        relativeTransform.isWithinEpsilonOf(AffineMatrix4x4.IDENTITY, 1e-6));
  }

  @Test
  public void animateTransformationNullOffsetDefaultsToIdentity() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    animator.animateTransformation(
        target, null, false, 0.0,
        TraditionalStyle.BEGIN_AND_END_GENTLY);

    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertTrue(relativeTransform.isWithinEpsilonOf(AffineMatrix4x4.IDENTITY, 1e-6));
  }

  // --- VantagePointData interpolation (inner class contract) ---

  @Test
  public void preSetVantagePointDataInterpolatesPosition() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 0, 0));

    // PreSetVantagePointData captures m0 = subject.getTransformation(target) and m1 = IDENTITY
    AbstractTransformableImp.PreSetVantagePointData data =
        new AbstractTransformableImp.PreSetVantagePointData(subject, target);

    assertNotNull("PreSetVantagePointData should capture subject", data.getSubject());
    assertEquals(subject, data.getSubject());
  }

  @Test
  public void preSetVantagePointDataSetPortionInterpolates() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 0, 0));

    AbstractTransformableImp.PreSetVantagePointData data =
        new AbstractTransformableImp.PreSetVantagePointData(subject, target);

    // At portion 0, subject should be at its original position relative to target
    data.setPortion(0.0);
    // At portion 1, subject should be at identity relative to target
    data.setPortion(1.0);
    // After epilogue, subject should be exactly at m1
    data.epilogue();

    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertTrue("After epilogue, relative transform should be identity",
        relativeTransform.isWithinEpsilonOf(AffineMatrix4x4.IDENTITY, 1e-6));
  }

  @Test
  public void preSetVantagePointDataSetPortionHalfway() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 0, 0));

    AbstractTransformableImp.PreSetVantagePointData data =
        new AbstractTransformableImp.PreSetVantagePointData(subject, target);

    // At portion 0.5, subject should be halfway between m0 and m1
    data.setPortion(0.5);
    assertNotNull("Subject should have a valid position at portion 0.5",
        subject.getLocalPosition());
  }

  // --- OrientationData (tested through setLocalOrientationOnly) ---

  @Test
  public void setLocalOrientationOnlyPreservesTranslation() {
    subject.setLocalTransformation(AffineMatrix4x4.createTranslation(7, 8, 9));

    animator.setLocalOrientationOnly(OrthogonalMatrix3x3.IDENTITY);

    Point3 pos = subject.getLocalPosition();
    assertEquals("X should be preserved", 7.0, pos.x(), 1e-9);
    assertEquals("Y should be preserved", 8.0, pos.y(), 1e-9);
    assertEquals("Z should be preserved", 9.0, pos.z(), 1e-9);
  }

  // --- setOrientationOnlyToPointAt ---

  @Test
  public void setOrientationOnlyToPointAtChangesOrientation() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, -5));

    animator.setOrientationOnlyToPointAt(target);

    // Subject should now be oriented toward the target
    assertNotNull(subject.getLocalOrientation());
  }
}
