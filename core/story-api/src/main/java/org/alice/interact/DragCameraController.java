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

import edu.cmu.cs.dennisc.clock.Clock;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import edu.cmu.cs.dennisc.scenegraph.event.AbsoluteTransformationListener;
import org.alice.interact.condition.ManipulatorConditionSet;
import org.alice.interact.condition.MouseDragCondition;
import org.alice.interact.condition.PickCondition;
import org.alice.interact.handle.HandleManager;
import org.alice.interact.manipulator.*;

import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Manages the camera registry, camera-manipulator wiring, camera mouse controls,
 * and display update loop for {@link DragAdapter}.
 * Extracted to reduce DragAdapter's size while preserving identical behavior.
 */
class DragCameraController {
  private final HandleManager handleManager;
  private final Map<DragAdapter.CameraView, CameraSet> cameraMap = Maps.newHashMap();
  private final AbsoluteTransformationListener cameraTransformationListener;
  private double timePrev = Double.NaN;
  private boolean hasSetCameraTransformables = false;

  DragCameraController(HandleManager handleManager) {
    this.handleManager = handleManager;
    this.cameraTransformationListener = event -> {
      if (event.getSource() instanceof SymmetricPerspectiveCamera camera) {
        if (getActiveCamera() == camera) {
          this.handleManager.updateCameraPosition(camera.getAbsoluteTransformation().translation());
        }
      }
    };
  }

  void addCameraView(DragAdapter.CameraView viewType, SymmetricPerspectiveCamera mainCamera) {
    addCameraView(viewType, mainCamera, null, null);
  }

  void addCameraView(DragAdapter.CameraView viewType, SymmetricPerspectiveCamera mainCamera,
                     SymmetricPerspectiveCamera layoutCamera, OrthographicCamera orthographicCamera) {
    addCameraView(viewType, new CameraSet(mainCamera, layoutCamera, orthographicCamera));
  }

  private void addCameraView(DragAdapter.CameraView viewType, CameraSet cameras) {
    if (cameras.mainCamera != null) {
      cameras.mainCamera.addAbsoluteTransformationListener(this.cameraTransformationListener);
      this.handleManager.updateCameraPosition(cameras.mainCamera.getAbsoluteTransformation().translation());
    }
    this.cameraMap.put(viewType, cameras);
  }

  void clearCameraViews() {
    for (CameraSet cameraSet : this.cameraMap.values()) {
      if (cameraSet.mainCamera != null) {
        cameraSet.mainCamera.removeAbsoluteTransformationListener(this.cameraTransformationListener);
      }
    }
    this.cameraMap.clear();
  }

  AbstractCamera getActiveCamera() {
    CameraSet activeCameraSet = this.cameraMap.get(DragAdapter.CameraView.MAIN);
    if ((activeCameraSet != null) && (activeCameraSet.getActiveCamera() != null)) {
      return activeCameraSet.getActiveCamera();
    }
    return null;
  }

  void makeCameraActive(AbstractCamera camera) {
    for (Map.Entry<DragAdapter.CameraView, CameraSet> cameras : this.cameraMap.entrySet()) {
      if (cameras.getValue().hasCamera(camera)) {
        cameras.getValue().setActiveCamera(camera);
      }
    }
    if (camera instanceof SymmetricPerspectiveCamera) {
      this.handleManager.updateCameraPosition(camera.getAbsoluteTransformation().translation());
    } else {
      this.handleManager.updateCameraPosition(null);
    }
  }

  AbstractCamera getCameraForManipulator(CameraInformedManipulator cameraManipulator) {
    DragAdapter.CameraView cameraView = cameraManipulator.getDesiredCameraView();
    if ((cameraView == DragAdapter.CameraView.ACTIVE_VIEW) || (cameraView == DragAdapter.CameraView.PICK_CAMERA)) {
      return getActiveCamera();
    }
    CameraSet cameras = this.cameraMap.get(cameraView);
    return cameras != null ? cameras.getActiveCamera() : null;
  }

  void setCameraOnManipulator(CameraInformedManipulator manipulator, InputState inputState) {
    if ((manipulator.getDesiredCameraView() == DragAdapter.CameraView.PICK_CAMERA) && (inputState.getPickCamera() != null)) {
      manipulator.setCamera(inputState.getPickCamera());
    } else {
      manipulator.setCamera(this.getCameraForManipulator(manipulator));
    }
  }

  void addCameraMouseControl(DragAdapter dragAdapter) {
    MouseDragCondition leftAndNoModifiers = new MouseDragCondition(MouseEvent.BUTTON1, new PickCondition(PickHint.getNonInteractiveHint()), new ModifierMask(ModifierMask.NO_MODIFIERS_DOWN));
    MouseDragCondition leftAndShift = new MouseDragCondition(MouseEvent.BUTTON1, new PickCondition(PickHint.getNonInteractiveHint()), new ModifierMask(ModifierMask.JUST_SHIFT));
    MouseDragCondition leftAndControl = new MouseDragCondition(MouseEvent.BUTTON1, new PickCondition(PickHint.getNonInteractiveHint()), new ModifierMask(ModifierMask.JUST_CONTROL));
    MouseDragCondition middleMouseAndAnything = new MouseDragCondition(MouseEvent.BUTTON2, new PickCondition(PickHint.getAnythingHint()));
    MouseDragCondition rightMouseAndNonInteractive = new MouseDragCondition(MouseEvent.BUTTON3, new PickCondition(PickHint.getNonInteractiveHint()));

    ManipulatorConditionSet cameraOrbit = new ManipulatorConditionSet(new CameraOrbitDragManipulator());
    cameraOrbit.addCondition(middleMouseAndAnything);
    dragAdapter.addManipulatorConditionSet(cameraOrbit);

    ManipulatorConditionSet cameraTilt = new ManipulatorConditionSet(new CameraTiltDragManipulator());
    cameraTilt.addCondition(rightMouseAndNonInteractive);
    cameraTilt.addCondition(leftAndControl);
    dragAdapter.addManipulatorConditionSet(cameraTilt);

    ManipulatorConditionSet cameraMouseTranslate = new ManipulatorConditionSet(new CameraMoveDragManipulator());
    cameraMouseTranslate.addCondition(leftAndNoModifiers);
    dragAdapter.addManipulatorConditionSet(cameraMouseTranslate);

    ManipulatorConditionSet cameraMousePan = new ManipulatorConditionSet(new CameraPanDragManipulator());
    cameraMousePan.addCondition(leftAndShift);
    dragAdapter.addManipulatorConditionSet(cameraMousePan);
  }

  void handleDisplay(DragAdapter dragAdapter) {
    AbstractCamera sgCamera = dragAdapter.getSGCamera();
    if (sgCamera != null) {
      if (!hasSetCameraTransformables) {
        dragAdapter.setSGCamera(sgCamera);
        hasSetCameraTransformables = true;
      }
      double timeCurr = Clock.getCurrentTime();
      if (Double.isNaN(this.timePrev)) {
        this.timePrev = Clock.getCurrentTime();
      }
      double timeDelta = timeCurr - this.timePrev;
      dragAdapter.update(timeDelta);
      this.timePrev = timeCurr;
    }
  }

  AbsoluteTransformationListener getCameraTransformationListener() {
    return this.cameraTransformationListener;
  }

  int getCameraViewCount() {
    return this.cameraMap.size();
  }

  boolean hasCamera(AbstractCamera camera) {
    for (CameraSet cameraSet : this.cameraMap.values()) {
      if (cameraSet.hasCamera(camera)) {
        return true;
      }
    }
    return false;
  }

  private static final class CameraSet {
    CameraSet(SymmetricPerspectiveCamera mainCamera,
              SymmetricPerspectiveCamera layoutCamera,
              OrthographicCamera orthographicCamera) {
      this.mainCamera = mainCamera;
      this.layoutCamera = layoutCamera;
      this.orthographicCamera = orthographicCamera;
    }

    void setActiveCamera(AbstractCamera camera) { this.activeCamera = camera; }
    AbstractCamera getActiveCamera() { return this.activeCamera; }

    boolean hasCamera(AbstractCamera camera) {
      return mainCamera == camera || layoutCamera == camera || orthographicCamera == camera;
    }

    private final SymmetricPerspectiveCamera mainCamera;
    private final SymmetricPerspectiveCamera layoutCamera;
    private final OrthographicCamera orthographicCamera;
    private AbstractCamera activeCamera;
  }
}
