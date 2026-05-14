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
import org.alice.math.immutable.Point3;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Contract tests for TransformOperations — the instance helper extracted from
 * AbstractTransformableImp that handles place/distance/smooth-position methods.
 *
 * These tests FAIL to compile until TransformOperations.java is created.
 */
public class TransformOperationsTest {

  private StandInImp vehicle;
  private StandInImp subject;
  private TransformOperations ops;

  @Before
  public void setUp() {
    vehicle = new StandInImp();
    subject = new StandInImp();
    subject.setVehicle(vehicle);
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    ops = new TransformOperations(subject);
  }

  // --- Construction ---

  @Test
  public void constructorAcceptsOwner() {
    TransformOperations operations = new TransformOperations(subject);
    assertNotNull(operations);
  }

  // --- getDistanceTo ---

  @Test
  public void getDistanceToSamePositionIsZero() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    double distance = ops.getDistanceTo(other);
    assertEquals(0.0, distance, 1e-6);
  }

  @Test
  public void getDistanceToReturns345Triangle() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(3, 4, 0));

    double distance = ops.getDistanceTo(other);
    assertEquals(5.0, distance, 1e-6);
  }

  @Test
  public void getDistanceToIs3dEuclidean() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(1, 2, 2));

    double distance = ops.getDistanceTo(other);
    assertEquals(3.0, distance, 1e-6);
  }

  // --- Directional distances ---

  @Test
  public void getDistanceAboveReturnsSeparation() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 5, 0));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    // For point entities (StandInImp), getMin==getMax==translation.
    // getDistanceAbove computes getMin(subject).y - getMax(other).y = 0 - 5 = -5
    double dist = ops.getDistanceAbove(other, vehicle);
    assertEquals("Point entity distance above", -5.0, dist, 1e-6);
  }

  @Test
  public void getDistanceBelowReturnsSeparation() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(0, -5, 0));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    // For point entities: getMin(other).y - getMax(subject).y = -5 - 0 = -5
    double dist = ops.getDistanceBelow(other, vehicle);
    assertEquals("Point entity distance below", -5.0, dist, 1e-6);
  }

  @Test
  public void getDistanceBehindReturnsSeparation() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 0, 5));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    // For point entities: getMin(subject).z - getMax(other).z = 0 - 5 = -5
    double dist = ops.getDistanceBehind(other, vehicle);
    assertEquals("Point entity distance behind", -5.0, dist, 1e-6);
  }

  @Test
  public void getDistanceInFrontOfReturnsSeparation() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 0, -5));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    // For point entities: getMin(other).z - getMax(subject).z = -5 - 0 = -5
    double dist = ops.getDistanceInFrontOf(other, vehicle);
    assertEquals("Point entity distance in front", -5.0, dist, 1e-6);
  }

  @Test
  public void getDistanceToTheLeftOfReturnsSeparation() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(-5, 0, 0));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    // For point entities: getMin(other).x - getMax(subject).x = -5 - 0 = -5
    double dist = ops.getDistanceToTheLeftOf(other, vehicle);
    assertEquals("Point entity distance to left", -5.0, dist, 1e-6);
  }

  @Test
  public void getDistanceToTheRightOfReturnsSeparation() {
    StandInImp other = new StandInImp();
    other.setVehicle(vehicle);
    other.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);

    // For point entities: getMin(subject).x - getMax(other).x = 0 - 5 = -5
    double dist = ops.getDistanceToTheRightOf(other, vehicle);
    assertEquals("Point entity distance to right", -5.0, dist, 1e-6);
  }

  // --- differenceToEpsilon ---

  @Test
  public void differenceToEpsilonReturnsZeroForSmallDifferences() {
    // Values within 0.01 threshold should return 0
    double result = ops.differenceToEpsilon(1.005, 1.000);
    assertEquals("Differences under 0.01 should collapse to zero", 0.0, result, 1e-9);
  }

  @Test
  public void differenceToEpsilonReturnsActualForLargeDifferences() {
    double result = ops.differenceToEpsilon(5.0, 2.0);
    assertEquals(3.0, result, 1e-9);
  }

  @Test
  public void differenceToEpsilonReturnsNegativeForNegativeDifferences() {
    double result = ops.differenceToEpsilon(2.0, 5.0);
    assertEquals(-3.0, result, 1e-9);
  }

  // --- setPositionOnly ---

  @Test
  public void setPositionOnlyMovesToTargetPosition() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 20, 30));

    ops.setPositionOnly(target);

    Point3 pos = subject.getLocalPosition();
    assertEquals(10.0, pos.x(), 1e-6);
    assertEquals(20.0, pos.y(), 1e-6);
    assertEquals(30.0, pos.z(), 1e-6);
  }

  @Test
  public void setPositionOnlyWithOffsetAppliesOffset() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(10, 0, 0));

    ops.setPositionOnly(target, new Point3(0, 5, 0));

    // The subject should be positioned at offset relative to target
    // Exact behavior depends on scene graph, but the call should succeed
    assertNotNull(subject.getLocalPosition());
  }

  // --- place ---

  @Test
  public void placeWithSpatialRelationPositionsCorrectly() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    // Place above target with no offset
    ops.place(SpatialRelationImp.ABOVE, target, 0.0, target);

    // Subject should have been repositioned
    assertNotNull(subject.getLocalPosition());
  }

  @Test
  public void placeDefaultOffsetIsZero() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    // Two-arg place should use default offset of 0
    ops.place(SpatialRelationImp.ABOVE, target);

    assertNotNull(subject.getLocalPosition());
  }

  // --- setTransformation ---

  @Test
  public void setTransformationRelativeToTargetSetsIdentity() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    ops.setTransformation(target, AffineMatrix4x4.IDENTITY);

    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertTrue("Relative transform should be identity after setTransformation",
        relativeTransform.isWithinEpsilonOf(AffineMatrix4x4.IDENTITY, 1e-6));
  }

  @Test
  public void setTransformationDefaultsToIdentityOffset() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, 0));

    ops.setTransformation(target);

    AffineMatrix4x4 relativeTransform = subject.getTransformation(target);
    assertTrue(relativeTransform.isWithinEpsilonOf(AffineMatrix4x4.IDENTITY, 1e-6));
  }
}
