/**
 * Copyright (c) 2006-2012, Carnegie Mellon University. All rights reserved.
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
 */
package org.lgna.ik.poser.scene;

import edu.cmu.cs.dennisc.animation.Animator;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.scenegraph.AsSeenBy;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import org.alice.interact.DragAdapter;
import org.alice.interact.InputState;
import org.alice.interact.MovementDirection;
import org.alice.interact.MovementType;
import org.alice.interact.QuaternionAndTranslation;
import org.alice.interact.animation.QuaternionAndTranslationTargetBasedAnimation;
import org.alice.interact.condition.MovementDescription;
import org.alice.interact.event.ManipulationEvent;
import org.alice.interact.manipulator.AnimatorDependentManipulator;
import org.alice.interact.manipulator.CameraManipulator;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.ClippedZPlane;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Vector3;
import org.lgna.story.implementation.ModelImp;

/**
 * @author Matt May
 */
public class PoserSceenMouseWheelManipulator extends CameraManipulator implements AnimatorDependentManipulator {
  private static final double CAMERA_SPEED = 10.0;
  private static final double ORTHOGRAPHIC_ZOOM_PER_WHEEL_CLICK = .2d;
  private static final double MAX_ORTHOGRAPHIC_ZOOM = 75.0d;
  private static final double MIN_ORTHOGRAPHIC_ZOOM = .01d;

  private ModelImp model;
  protected QuaternionAndTranslationTargetBasedAnimation cameraAnimation;
  private Animator animator;

  public void setModel(ModelImp model) {
    this.model = model;
  }

  @Override
  public Animator getAnimator() {
    return this.animator;
  }

  @Override
  public void setAnimator(Animator animator) {
    this.animator = animator;
  }

  @Override
  public String getUndoRedoDescription() {
    return "Camera Zoom";
  }

  @Override
  public DragAdapter.CameraView getDesiredCameraView() {
    return DragAdapter.CameraView.PICK_CAMERA;
  }

  @Override
  public boolean doStartManipulator(InputState startInput) {
    if (isTooClose()) {
      return false;
    }
    if (super.doStartManipulator(startInput)) {
      if ((this.cameraAnimation != null) && (this.animator != null)) {
        this.animator.removeFrameObserver(this.cameraAnimation);
      }
      this.cameraAnimation = new QuaternionAndTranslationTargetBasedAnimation(new QuaternionAndTranslation(this.manipulatedTransformable.getAbsoluteTransformation()), CAMERA_SPEED) {
        @Override
        protected void updateValue(QuaternionAndTranslation value) {
          if (PoserSceenMouseWheelManipulator.this.camera != null) {
            AffineMatrix4x4 matrix = value.getAffineMatrix();
            manipulatedTransformable.setTransformation(matrix, AsSeenBy.SCENE);
          }
        }
      };
      if (this.animator != null) {
        this.animator.addFrameObserver(this.cameraAnimation);
      }
      zoomCamera(startInput.getMouseWheelState());
      this.manipulatedTransformable.notifyTransformationListeners();
      return true;
    }
    return false;
  }

  private boolean isTooClose() {
    return getDistance() < .33;
  }

  private double getDistance() {
    if ((model == null) || (camera == null)) {
      return Double.POSITIVE_INFINITY;
    }
    Point3 modelLoc = model.getAbsoluteTransformation().translation().withZ(1);
    Point3 cameraLoc = camera.getAbsoluteTransformation().translation().withZ(1);
    return modelLoc.distanceFrom(cameraLoc);
  }

  @Override
  public void doDataUpdateManipulator(InputState currentInput, InputState previousInput) {
    if (!isTooClose() && !currentInput.isAnyMouseButtonDown() && !(currentInput.getMouseWheelState() < 0)) {
      zoomCamera(currentInput.getMouseWheelState());
      this.manipulatedTransformable.notifyTransformationListeners();
    }
  }

  @Override
  public void doTimeUpdateManipulator(double time, InputState currentInput) {
    // Do nothing
  }

  @Override
  public void doEndManipulator(InputState endInput, InputState previousInput) {
    if (this.cameraAnimation != null) {
      this.cameraAnimation.complete();
      if (this.animator != null) {
        this.animator.removeFrameObserver(this.cameraAnimation);
      }
      this.cameraAnimation = null;
    }
  }

  @Override
  public void doClickManipulator(InputState endInput, InputState previousInput) {
    // Do nothing
  }

  @Override
  protected void initializeEventMessages() {
    this.setMainManipulationEvent(new ManipulationEvent(ManipulationEvent.EventType.Zoom, null, this.manipulatedTransformable));
    this.clearManipulationEvents();
    this.addManipulationEvent(new ManipulationEvent(ManipulationEvent.EventType.Zoom, new MovementDescription(MovementDirection.FORWARD, MovementType.LOCAL), this.manipulatedTransformable));
    this.addManipulationEvent(new ManipulationEvent(ManipulationEvent.EventType.Zoom, new MovementDescription(MovementDirection.BACKWARD, MovementType.LOCAL), this.manipulatedTransformable));
  }

  private double getCameraZoom() {
    OrthographicCamera orthoCam = (OrthographicCamera) this.camera;
    ClippedZPlane picturePlane = orthoCam.picturePlane.getValue();
    return picturePlane.getHeight();
  }

  private void setCameraZoom(double amount) {
    OrthographicCamera orthoCam = (OrthographicCamera) this.camera;
    ClippedZPlane picturePlane = orthoCam.picturePlane.getValue();
    double newZoom = PoserCameraZoomMouseWheelManipulatorLogic.computeClampedOrthographicZoom(
        picturePlane.getHeight(), amount, MIN_ORTHOGRAPHIC_ZOOM, MAX_ORTHOGRAPHIC_ZOOM);
    orthoCam.picturePlane.setValue(picturePlane.withHeight(newZoom));
  }

  protected void zoomCamera(int direction) {
    if (this.camera instanceof SymmetricPerspectiveCamera) {
      if (this.cameraAnimation == null) {
        Logger.severe("Mouse Wheel Camera Zoom: null cameraAnimation.");
        return;
      }
      AffineMatrix4x4 originalTransformation = this.getManipulatedTransformable().getAbsoluteTransformation();
      OrthogonalMatrix3x3 orientation = originalTransformation.orientation();
      Vector3 movementDirection = orientation.backward().times(direction).normalized().times(getZoomSpeed());
      Point3 translation = originalTransformation.translation().plus(movementDirection);
      AffineMatrix4x4 targetTransform = new AffineMatrix4x4(orientation, translation);
      this.cameraAnimation.setTarget(new QuaternionAndTranslation(targetTransform));
    } else {
      double amountToZoom = PoserCameraZoomMouseWheelManipulatorLogic.computeOrthographicZoomAmount(direction, ORTHOGRAPHIC_ZOOM_PER_WHEEL_CLICK);
      this.applyZoom(amountToZoom);
    }
  }

  protected void applyZoom(double zoomAmount) {
    this.setCameraZoom(zoomAmount);
    for (ManipulationEvent event : this.getManipulationEvents()) {
      if (event.getMovementDescription().direction == MovementDirection.FORWARD) {
        this.dragAdapter.triggerManipulationEvent(event, zoomAmount < 0.0d);
      } else if (event.getMovementDescription().direction == MovementDirection.BACKWARD) {
        this.dragAdapter.triggerManipulationEvent(event, zoomAmount >= 0.0d);
      }
    }
  }

  private double getZoomSpeed() {
    return getDistance() / 10;
  }
}
