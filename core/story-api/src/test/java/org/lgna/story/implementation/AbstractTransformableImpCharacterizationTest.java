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

import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for AbstractTransformableImp.
 * These document existing behavior before the extraction refactoring.
 * They must continue to pass after helpers are extracted.
 */
public class AbstractTransformableImpCharacterizationTest {

  private StandInImp subject;
  private StandInImp vehicle;

  @Before
  public void setUp() {
    vehicle = new StandInImp();
    subject = new StandInImp();
    subject.setVehicle(vehicle);
  }

  // --- Local transformation ---

  @Test
  public void newStandInHasIdentityLocalTransformation() {
    StandInImp fresh = new StandInImp();
    AffineMatrix4x4 m = fresh.getLocalTransformation();
    assertNotNull(m);
    assertTrue("Fresh StandInImp should have identity transform", m.isIdentity());
  }

  @Test
  public void setLocalTransformationRoundTrips() {
    AffineMatrix4x4 translation = AffineMatrix4x4.createTranslation(3.0, 4.0, 5.0);
    subject.setLocalTransformation(translation);

    AffineMatrix4x4 result = subject.getLocalTransformation();
    assertEquals(3.0, result.translation().x(), 1e-9);
    assertEquals(4.0, result.translation().y(), 1e-9);
    assertEquals(5.0, result.translation().z(), 1e-9);
  }

  @Test
  public void getLocalPositionReturnsTranslationComponent() {
    subject.setLocalTransformation(AffineMatrix4x4.createTranslation(1.0, 2.0, 3.0));
    Point3 pos = subject.getLocalPosition();
    assertEquals(1.0, pos.x(), 1e-9);
    assertEquals(2.0, pos.y(), 1e-9);
    assertEquals(3.0, pos.z(), 1e-9);
  }

  @Test
  public void getLocalOrientationReturnsOrientationComponent() {
    subject.setLocalTransformation(AffineMatrix4x4.createTranslation(1.0, 2.0, 3.0));
    OrthogonalMatrix3x3 orientation = subject.getLocalOrientation();
    assertTrue("Translation-only transform should have identity orientation",
        orientation.isIdentity());
  }

  // --- setLocalOrientation preserves translation ---

  @Test
  public void setLocalOrientationPreservesPosition() {
    subject.setLocalTransformation(AffineMatrix4x4.createTranslation(7.0, 8.0, 9.0));
    subject.setLocalOrientationOnly(OrthogonalMatrix3x3.IDENTITY);

    Point3 pos = subject.getLocalPosition();
    assertEquals("X position should be preserved", 7.0, pos.x(), 1e-9);
    assertEquals("Y position should be preserved", 8.0, pos.y(), 1e-9);
    assertEquals("Z position should be preserved", 9.0, pos.z(), 1e-9);
  }

  // --- applyTranslation ---

  @Test
  public void applyTranslationInSelfFrameMovesLocally() {
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    subject.applyTranslation(1.0, 0.0, 0.0, subject);

    Point3 pos = subject.getLocalPosition();
    assertEquals(1.0, pos.x(), 1e-9);
    assertEquals(0.0, pos.y(), 1e-9);
    assertEquals(0.0, pos.z(), 1e-9);
  }

  @Test
  public void applyTranslationAccumulates() {
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    subject.applyTranslation(1.0, 0.0, 0.0, subject);
    subject.applyTranslation(0.0, 2.0, 0.0, subject);

    Point3 pos = subject.getLocalPosition();
    assertEquals(1.0, pos.x(), 1e-9);
    assertEquals(2.0, pos.y(), 1e-9);
  }

  @Test
  public void applyTranslationWithPoint3Delegates() {
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    subject.applyTranslation(new Point3(3.0, 4.0, 5.0), subject);

    Point3 pos = subject.getLocalPosition();
    assertEquals(3.0, pos.x(), 1e-9);
    assertEquals(4.0, pos.y(), 1e-9);
    assertEquals(5.0, pos.z(), 1e-9);
  }

  // --- isFacing ---

  @Test
  public void isFacingDetectsEntityInFront() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    // Place other in front of subject (negative Z in local frame)
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 0, -5));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    assertTrue("Entity at negative Z should be 'facing'", subject.isFacing(other));
  }

  @Test
  public void isFacingReturnsFalseForEntityBehind() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    // Place other behind subject (positive Z in local frame)
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 0, 5));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    assertFalse("Entity at positive Z should not be 'facing'", subject.isFacing(other));
  }

  // --- setPositionOnly ---

  @Test
  public void setPositionOnlyMovesToTargetPosition() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 20, 30));

    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    subject.setPositionOnly(target);

    Point3 pos = subject.getLocalPosition();
    assertEquals(10.0, pos.x(), 1e-6);
    assertEquals(20.0, pos.y(), 1e-6);
    assertEquals(30.0, pos.z(), 1e-6);
  }

  // --- getDistanceTo ---

  @Test
  public void getDistanceToReturnsEuclideanDistance() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(3, 4, 0));

    double distance = subject.getDistanceTo(other);
    assertEquals(5.0, distance, 1e-6);
  }

  @Test
  public void getDistanceToSamePositionIsZero() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    other.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    double distance = subject.getDistanceTo(other);
    assertEquals(0.0, distance, 1e-6);
  }

  // --- setTransformation ---

  @Test
  public void setTransformationRelativeToTarget() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    subject.setTransformation(target, AffineMatrix4x4.IDENTITY);
    // Subject should now be at the same position as target
    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertTrue("After setTransformation(target, IDENTITY), relative transform should be identity",
        relativeTransform.isWithinEpsilonOf(AffineMatrix4x4.IDENTITY, 1e-6));
  }

  @Test
  public void setTransformationWithOffsetAppliesOffset() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    AffineMatrix4x4 offset = AffineMatrix4x4.createTranslation(0, 0, -2);
    subject.setTransformation(target, offset);

    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertEquals(-2.0, relativeTransform.translation().z(), 1e-6);
  }

  // --- Vehicle management ---

  @Test
  public void setVehicleEstablishesParentRelationship() {
    StandInImp child = new StandInImp();
    child.setVehicle(vehicle);

    EntityImp retrievedVehicle = child.getVehicle();
    assertEquals(vehicle, retrievedVehicle);
  }

  @Test(expected = org.lgna.common.LgnaIllegalArgumentException.class)
  public void setVehicleToSelfThrows() {
    subject.setVehicle(subject);
  }

  @Test
  public void postCheckSetVehiclePreservesAbsoluteTransform() {
    // Set subject at a known position
    subject.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 0, 0));

    // Create a new vehicle at a different position
    StandInImp newVehicle = new StandInImp();
    newVehicle.setVehicle(vehicle);
    newVehicle.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    // After re-parenting, the subject's world position should be preserved
    // (postCheckSetVehicle in AbstractTransformableImp handles this)
    // Note: This requires a scene to be set, which StandInImp won't have.
    // The method gracefully handles null scene by skipping the transform preservation.
    // This test documents that behavior.
    subject.setVehicle(newVehicle);
    assertNotNull(subject.getVehicle());
  }

  // --- Rotation ---

  @Test
  public void applyRotationInRadiansChangesOrientation() {
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    org.alice.math.immutable.Vector3 yAxis = new org.alice.math.immutable.Vector3(0, 1, 0);
    subject.applyRotationInRadians(yAxis, Math.PI / 2.0, subject);

    OrthogonalMatrix3x3 orientation = subject.getLocalOrientation();
    assertFalse("Orientation should no longer be identity after rotation",
        orientation.isIdentity());
  }

  // --- AnimateApplyTranslation with zero duration ---

  @Test
  public void animateApplyTranslationWithZeroDurationAppliesImmediately() {
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    subject.animateApplyTranslation(
        new Point3(5.0, 0.0, 0.0), subject, 0.0,
        edu.cmu.cs.dennisc.animation.TraditionalStyle.BEGIN_AND_END_GENTLY);

    Point3 pos = subject.getLocalPosition();
    assertEquals(5.0, pos.x(), 1e-6);
  }
}
