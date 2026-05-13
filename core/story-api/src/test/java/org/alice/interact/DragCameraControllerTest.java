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
package org.alice.interact;

import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import org.alice.interact.handle.HandleManager;
import org.alice.interact.manipulator.CameraInformedManipulator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for DragCameraController — the extracted camera registry
 * (CameraView/CameraSet map), camera-manipulator wiring, and camera
 * lifecycle management from DragAdapter.
 *
 * These tests define the contract that the implementation must satisfy.
 * They will fail to compile until DragCameraController is created.
 */
public class DragCameraControllerTest {

  private DragCameraController cameraController;
  private HandleManager handleManager;

  @Before
  public void setUp() {
    handleManager = new HandleManager();
    cameraController = new DragCameraController(handleManager);
  }

  // --- Initial state ---

  @Test
  public void activeCameraIsNullInitially() {
    assertNull("Active camera should be null when no cameras registered",
        cameraController.getActiveCamera());
  }

  @Test
  public void cameraViewCountIsZeroInitially() {
    assertEquals("Camera view count should be 0 initially",
        0, cameraController.getCameraViewCount());
  }

  // --- addCameraView ---

  @Test
  public void addCameraViewWithMainCameraOnly() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);

    assertEquals("Camera view count should be 1 after adding",
        1, cameraController.getCameraViewCount());
  }

  @Test
  public void addCameraViewWithAllCameraTypes() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera layoutCamera = new SymmetricPerspectiveCamera();
    OrthographicCamera orthoCamera = new OrthographicCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN,
        mainCamera, layoutCamera, orthoCamera);

    assertEquals("Camera view count should be 1 after adding one view",
        1, cameraController.getCameraViewCount());
  }

  @Test
  public void addMultipleCameraViews() {
    SymmetricPerspectiveCamera main = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera topLeft = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN, main);
    cameraController.addCameraView(DragAdapter.CameraView.TOP_LEFT, topLeft);

    assertEquals("Camera view count should be 2",
        2, cameraController.getCameraViewCount());
  }

  // --- getActiveCamera ---

  @Test
  public void getActiveCameraReturnsMainViewActiveCamera() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.makeCameraActive(mainCamera);

    assertSame("Active camera should be the main camera",
        mainCamera, cameraController.getActiveCamera());
  }

  @Test
  public void getActiveCameraReturnsNullWhenNoCameraSetActive() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    // Don't call makeCameraActive

    // Active camera is null until explicitly set
    assertNull("Active camera should be null when none set active",
        cameraController.getActiveCamera());
  }

  // --- makeCameraActive ---

  @Test
  public void makeCameraActiveSetsActiveCameraOnMatchingSet() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera layoutCamera = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN,
        mainCamera, layoutCamera, null);
    cameraController.makeCameraActive(layoutCamera);

    assertSame("Layout camera should become the active camera",
        layoutCamera, cameraController.getActiveCamera());
  }

  @Test
  public void makeCameraActiveWithUnregisteredCameraIsNoOp() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera unregistered = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.makeCameraActive(mainCamera);
    cameraController.makeCameraActive(unregistered);

    // The active camera should still be the main camera for the MAIN view
    assertSame("Active camera should remain unchanged for unregistered camera",
        mainCamera, cameraController.getActiveCamera());
  }

  // --- clearCameraViews ---

  @Test
  public void clearCameraViewsRemovesAllViews() {
    SymmetricPerspectiveCamera main = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera topLeft = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN, main);
    cameraController.addCameraView(DragAdapter.CameraView.TOP_LEFT, topLeft);
    cameraController.clearCameraViews();

    assertEquals("Camera view count should be 0 after clearing",
        0, cameraController.getCameraViewCount());
    assertNull("Active camera should be null after clearing",
        cameraController.getActiveCamera());
  }

  @Test
  public void clearCameraViewsIsIdempotent() {
    cameraController.clearCameraViews();
    assertEquals("Clearing empty controller should not error",
        0, cameraController.getCameraViewCount());
  }

  // --- getCameraForManipulator ---

  @Test
  public void getCameraForManipulatorReturnsActiveCameraForActiveView() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.makeCameraActive(mainCamera);

    StubCameraManipulator manipulator = new StubCameraManipulator(DragAdapter.CameraView.ACTIVE_VIEW);
    AbstractCamera result = cameraController.getCameraForManipulator(manipulator);

    assertSame("ACTIVE_VIEW should return the active camera",
        mainCamera, result);
  }

  @Test
  public void getCameraForManipulatorReturnsActiveCameraForPickCamera() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.makeCameraActive(mainCamera);

    StubCameraManipulator manipulator = new StubCameraManipulator(DragAdapter.CameraView.PICK_CAMERA);
    AbstractCamera result = cameraController.getCameraForManipulator(manipulator);

    assertSame("PICK_CAMERA should return the active camera",
        mainCamera, result);
  }

  @Test
  public void getCameraForManipulatorReturnsSpecificViewCamera() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera topLeftCamera = new SymmetricPerspectiveCamera();

    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.addCameraView(DragAdapter.CameraView.TOP_LEFT, topLeftCamera);
    cameraController.makeCameraActive(mainCamera);
    cameraController.makeCameraActive(topLeftCamera);

    StubCameraManipulator manipulator = new StubCameraManipulator(DragAdapter.CameraView.TOP_LEFT);
    AbstractCamera result = cameraController.getCameraForManipulator(manipulator);

    assertSame("TOP_LEFT view should return the top-left camera",
        topLeftCamera, result);
  }

  @Test
  public void getCameraForManipulatorReturnsNullForUnregisteredView() {
    StubCameraManipulator manipulator = new StubCameraManipulator(DragAdapter.CameraView.BOTTOM_RIGHT);
    AbstractCamera result = cameraController.getCameraForManipulator(manipulator);

    assertNull("Unregistered view should return null",
        result);
  }

  // --- setCameraOnManipulator ---

  @Test
  public void setCameraOnManipulatorSetsResolvedCamera() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.makeCameraActive(mainCamera);

    StubCameraManipulator manipulator = new StubCameraManipulator(DragAdapter.CameraView.ACTIVE_VIEW);
    InputState inputState = new InputState();
    cameraController.setCameraOnManipulator(manipulator, inputState);

    assertSame("Manipulator should have the active camera set on it",
        mainCamera, manipulator.getCamera());
  }

  @Test
  public void setCameraOnManipulatorUsesPickCameraFromInputState() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);
    cameraController.makeCameraActive(mainCamera);

    // PICK_CAMERA with a non-null pick camera in InputState should use that instead
    StubCameraManipulator manipulator = new StubCameraManipulator(DragAdapter.CameraView.PICK_CAMERA);
    InputState inputState = new InputState();
    // When InputState has no pick camera, falls through to getCameraForManipulator
    cameraController.setCameraOnManipulator(manipulator, inputState);

    assertSame("With no pick camera in state, should fall back to active camera",
        mainCamera, manipulator.getCamera());
  }

  // --- Transformation listener ---

  @Test
  public void transformationListenerIsNotNull() {
    assertNotNull("Camera transformation listener should not be null",
        cameraController.getCameraTransformationListener());
  }

  // --- has camera check ---

  @Test
  public void hasCameraReturnsTrueForRegisteredMainCamera() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);

    assertTrue("Should have the main camera",
        cameraController.hasCamera(mainCamera));
  }

  @Test
  public void hasCameraReturnsFalseForUnregisteredCamera() {
    SymmetricPerspectiveCamera mainCamera = new SymmetricPerspectiveCamera();
    SymmetricPerspectiveCamera other = new SymmetricPerspectiveCamera();
    cameraController.addCameraView(DragAdapter.CameraView.MAIN, mainCamera);

    assertFalse("Should not have an unregistered camera",
        cameraController.hasCamera(other));
  }

  // --- Stub for CameraInformedManipulator ---

  private static class StubCameraManipulator implements CameraInformedManipulator {
    private DragAdapter.CameraView desiredView;
    private AbstractCamera camera;

    StubCameraManipulator(DragAdapter.CameraView desiredView) {
      this.desiredView = desiredView;
    }

    @Override
    public AbstractCamera getCamera() {
      return this.camera;
    }

    @Override
    public void setCamera(AbstractCamera camera) {
      this.camera = camera;
    }

    @Override
    public DragAdapter.CameraView getDesiredCameraView() {
      return this.desiredView;
    }

    @Override
    public void setDesiredCameraView(DragAdapter.CameraView cameraView) {
      this.desiredView = cameraView;
    }
  }
}
