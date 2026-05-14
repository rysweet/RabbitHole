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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Contract tests for VehicleManager — the static utility extracted from
 * AbstractTransformableImp that manages the StandIn object pool and
 * the calculateTurnToFaceAxes algorithm.
 *
 * These tests FAIL to compile until VehicleManager.java is created.
 */
public class VehicleManagerTest {

  private StandInImp vehicle;
  private StandInImp subject;

  @Before
  public void setUp() {
    vehicle = new StandInImp();
    subject = new StandInImp();
    subject.setVehicle(vehicle);
    subject.setLocalTransformation(AffineMatrix4x4.IDENTITY);
  }

  // --- StandIn Pool: acquire/release ---

  @Test
  public void acquireStandInReturnsNonNull() {
    StandInImp standIn = VehicleManager.acquireStandIn(vehicle);
    assertNotNull("acquireStandIn should return a non-null StandInImp", standIn);
    VehicleManager.releaseStandIn(standIn);
  }

  @Test
  public void acquireStandInSetsVehicle() {
    StandInImp standIn = VehicleManager.acquireStandIn(vehicle);
    assertEquals("Acquired StandIn should have given composite as vehicle",
        vehicle, standIn.getVehicle());
    VehicleManager.releaseStandIn(standIn);
  }

  @Test
  public void acquireStandInSetsIdentityTransform() {
    StandInImp standIn = VehicleManager.acquireStandIn(vehicle);
    assertTrue("Acquired StandIn should have identity local transform",
        standIn.getLocalTransformation().isIdentity());
    VehicleManager.releaseStandIn(standIn);
  }

  @Test
  public void releasedStandInCanBeReacquired() {
    StandInImp first = VehicleManager.acquireStandIn(vehicle);
    VehicleManager.releaseStandIn(first);
    StandInImp second = VehicleManager.acquireStandIn(vehicle);
    assertNotNull("Re-acquired StandIn should be non-null", second);
    // Pool should recycle the same instance
    assertSame("Pool should recycle released StandIn", first, second);
    VehicleManager.releaseStandIn(second);
  }

  @Test
  public void multipleAcquiresReturnDistinctInstances() {
    StandInImp a = VehicleManager.acquireStandIn(vehicle);
    StandInImp b = VehicleManager.acquireStandIn(vehicle);
    assertNotSame("Simultaneous acquires should return different instances", a, b);
    VehicleManager.releaseStandIn(a);
    VehicleManager.releaseStandIn(b);
  }

  // --- calculateTurnToFaceAxes ---

  @Test
  public void calculateTurnToFaceAxesReturnsNonNull() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, -5));

    OrthogonalMatrix3x3 result = VehicleManager.calculateTurnToFaceAxes(subject, target);
    assertNotNull("calculateTurnToFaceAxes should return non-null orientation", result);
  }

  @Test
  public void calculateTurnToFaceAxesProducesValidOrientation() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(5, 0, -5));

    OrthogonalMatrix3x3 result = VehicleManager.calculateTurnToFaceAxes(subject, target);
    assertFalse("Result orientation should not be NaN", result.isNaN());
  }

  @Test
  public void calculateTurnToFaceAxesReturnsCurrentOrientationWhenColocated() {
    // When target is at same XZ position as subject, should return current orientation
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 5, 0));

    OrthogonalMatrix3x3 result = VehicleManager.calculateTurnToFaceAxes(subject, target);
    OrthogonalMatrix3x3 current = subject.getLocalOrientation();
    assertTrue("When colocated in XZ, should return current orientation",
        result.isWithinEpsilonOf(current, 1e-6));
  }

  @Test
  public void calculateTurnToFaceAxesPointsTowardTarget() {
    StandInImp target = new StandInImp();
    target.setVehicle(vehicle);
    // Place target directly in front (-Z direction) at x=0, z=-10
    target.setLocalTransformation(AffineMatrix4x4.createTranslation(0, 0, -10));

    OrthogonalMatrix3x3 result = VehicleManager.calculateTurnToFaceAxes(subject, target);
    // When target is directly at -Z, the forward vector should point in -Z
    // Forward is the negative of the backward vector
    double forwardZ = -result.backward().z();
    assertTrue("Forward Z component should be negative (pointing toward -Z target)",
        forwardZ < 0);
  }

  @After
  public void tearDown() {
    subject = null;
    vehicle = null;
  }
}
