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

import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import edu.cmu.cs.dennisc.render.event.AutomaticDisplayListener;
import edu.cmu.cs.dennisc.render.gl.GlrRenderFactory;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.AbstractTransformable;
import edu.cmu.cs.dennisc.scenegraph.Element;
import edu.cmu.cs.dennisc.scenegraph.Joint;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.Silhouette;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import edu.cmu.cs.dennisc.scenegraph.Visual;
import org.alice.interact.event.ManipulationEvent;
import org.alice.interact.event.ManipulationEventManager;
import org.alice.interact.event.ManipulationListener;
import org.alice.interact.event.SelectionEvent;
import org.alice.interact.event.SelectionListener;
import org.alice.interact.handle.HandleManager;
import org.alice.interact.handle.HandleSet;
import org.alice.interact.handle.HandleStyle;
import org.alice.interact.handle.ManipulationHandle;
import org.alice.interact.condition.*;
import org.alice.interact.manipulator.*;
import org.alice.math.immutable.Angle;
import org.alice.math.immutable.AngleInRadians;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.AffineMatrix4x4;
import edu.cmu.cs.dennisc.animation.Animator;
import org.lgna.story.implementation.*;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Cosgrove
 *
 * inherited by RuntimeDragAdapter, CroquetSupporting/Global DragAdapter,
 * CreateAPersonDragAdapter, PoserAnimatorDragAdapter, SingleViewerDragAdapter
 *
 * Event handling delegated to {@link DragEventHandler}.
 * Camera management delegated to {@link DragCameraController}.
 */
public abstract class DragAdapter {
  public static final Element.Key<AxisAlignedBox> BOUNDING_BOX_KEY = Element.Key.createInstance("BOUNDING_BOX_KEY");

  protected static final MovementKey[] DEFAULT_MOVEMENT_KEYS = {
    new MovementKey(KeyEvent.VK_UP, new MovementDescription(MovementDirection.FORWARD)),
    new MovementKey(KeyEvent.VK_W, new MovementDescription(MovementDirection.FORWARD)),
    new MovementKey(KeyEvent.VK_DOWN, new MovementDescription(MovementDirection.BACKWARD)),
    new MovementKey(KeyEvent.VK_S, new MovementDescription(MovementDirection.BACKWARD)),
    new MovementKey(KeyEvent.VK_LEFT, new MovementDescription(MovementDirection.LEFT)),
    new MovementKey(KeyEvent.VK_A, new MovementDescription(MovementDirection.LEFT)),
    new MovementKey(KeyEvent.VK_RIGHT, new MovementDescription(MovementDirection.RIGHT)),
    new MovementKey(KeyEvent.VK_D,  new MovementDescription(MovementDirection.RIGHT)),
    new MovementKey(KeyEvent.VK_PAGE_UP, new MovementDescription(MovementDirection.UP, MovementType.LOCAL), .5d),
    new MovementKey(KeyEvent.VK_PAGE_DOWN, new MovementDescription(MovementDirection.DOWN, MovementType.LOCAL), .5d),
  };
  protected static final MovementKey[] DEFAULT_ZOOM_KEYS = {
      new MovementKey(KeyEvent.VK_MINUS, new MovementDescription(MovementDirection.BACKWARD, MovementType.LOCAL)),
      new MovementKey(KeyEvent.VK_SUBTRACT, new MovementDescription(MovementDirection.BACKWARD, MovementType.LOCAL)),
      new MovementKey(KeyEvent.VK_EQUALS, new MovementDescription(MovementDirection.FORWARD, MovementType.LOCAL)),
      new MovementKey(KeyEvent.VK_ADD, new MovementDescription(MovementDirection.FORWARD, MovementType.LOCAL)),
  };
  protected static final MovementKey[] DEFAULT_ROTATE_KEYS = {
      new MovementKey(KeyEvent.VK_OPEN_BRACKET, new MovementDescription(MovementDirection.LEFT, MovementType.LOCAL), 2.0d),
      new MovementKey(KeyEvent.VK_CLOSE_BRACKET, new MovementDescription(MovementDirection.RIGHT, MovementType.LOCAL), 2.0d),
  };

  protected final Map<HandleStyle, InteractionGroup> mapHandleStyleToInteractionGroup = Maps.newHashMap();
  private final HandleManager handleManager = new HandleManager();
  private final List<SelectionListener> selectionListeners = Lists.newCopyOnWriteArrayList();
  final DragEventHandler eventHandler = new DragEventHandler(this);
  private final DragCameraController cameraController = new DragCameraController(this.handleManager);
  private final AutomaticDisplayListener automaticDisplayAdapter = e -> this.cameraController.handleDisplay(this);
  private final List<ManipulatorConditionSet> manipulators = Lists.newCopyOnWriteArrayList();
  private final ManipulationEventManager manipulationEventManager = new ManipulationEventManager();
  // TODO make currentInputState private
  protected final InputState currentInputState = new InputState();
  private final InputState previousInputState = new InputState();
  private AbstractTransformableImp toBeSelected = null;
  private boolean hasObjectToBeSelected = false;
  private InteractionGroup currentInteractionState = null;
  private AbstractTransformableImp selectedObject = null;
  private Silhouette sgSilhouette;
  private CameraMarkerImp selectedCameraMarker = null;
  private ObjectMarkerImp selectedObjectMarker = null;
  private OnscreenRenderTarget onscreenRenderTarget;
  private Component lookingGlassComponent = null;
  private Animator animator;
  private boolean isInStageChange = false;

  public void addListeners(Component component) { this.eventHandler.addListeners(component); }
  public void removeListeners(Component component) { this.eventHandler.removeListeners(component); }

  public void addManipulationListener(ManipulationListener listener) {
    this.manipulationEventManager.addManipulationListener(listener);
  }
  public void removeManipulationListener(ManipulationListener listener) {
    this.manipulationEventManager.removeManipulationListener(listener);
  }
  public void triggerManipulationEvent(ManipulationEvent event, boolean isActivate) {
    event.setInputState(this.currentInputState);
    this.manipulationEventManager.triggerEvent(event, isActivate);
  }
  public void addManipulatorConditionSet(ManipulatorConditionSet manipulator) {
    this.manipulators.add(manipulator);
    manipulator.getManipulator().setDragAdapter(this);
  }
  protected Iterable<ManipulatorConditionSet> getManipulatorConditionSets() { return this.manipulators; }
  public OnscreenRenderTarget getOnscreenRenderTarget() { return this.onscreenRenderTarget; }

  public void setOnscreenRenderTarget(OnscreenRenderTarget target) {
    if (this.onscreenRenderTarget != null) {
      GlrRenderFactory.getInstance().removeAutomaticDisplayListener(this.automaticDisplayAdapter);
    }
    this.onscreenRenderTarget = target;
    if (this.onscreenRenderTarget != null) {
      setAWTComponent(this.onscreenRenderTarget.getAwtComponent());
      GlrRenderFactory.getInstance().addAutomaticDisplayListener(this.automaticDisplayAdapter);
    } else {
      setAWTComponent(null);
    }
  }

  protected Component getAWTComponent() { return this.lookingGlassComponent; }

  private void setAWTComponent(Component awtComponent) {
    if (this.lookingGlassComponent != null) {
      this.eventHandler.removeListeners(this.lookingGlassComponent);
    }
    this.lookingGlassComponent = awtComponent;
    if (this.lookingGlassComponent != null) {
      this.eventHandler.addListeners(awtComponent);
    }
  }

  public Animator getAnimator() { return this.animator; }

  public void setAnimator(Animator animator) {
    this.animator = animator;
    for (ManipulatorConditionSet manipulatorSet : this.manipulators) {
      if (manipulatorSet.getManipulator() instanceof AnimatorDependentManipulator) {
        ((AnimatorDependentManipulator) manipulatorSet.getManipulator()).setAnimator(this.animator);
      }
    }
  }

  public void setLookingGlassOnManipulator(OnscreenPicturePlaneInformedManipulator manipulator) {
    manipulator.setOnscreenRenderTarget(this.onscreenRenderTarget);
  }

  private void setCurrentInteractionState(InteractionGroup interactionState) {
    this.currentInteractionState = interactionState;
    if (this.currentInteractionState != null) {
      InteractionGroup.InteractionInfo interactionInfo = this.currentInteractionState.getMatchingInfo(ObjectType.getObjectType(this.selectedObject));
      if (interactionInfo != null) {
        this.handleManager.setHandleSet(interactionInfo.getHandleSet());
      }
      this.currentInteractionState.enabledManipulators(true);
    }
  }

  public void setInteractionState(HandleStyle handleStyle) {
    if (this.currentInteractionState != null) {
      this.currentInteractionState.enabledManipulators(false);
    }
    setCurrentInteractionState(this.mapHandleStyleToInteractionGroup.get(handleStyle));
  }

  public void makeCameraActive(AbstractCamera camera) { this.cameraController.makeCameraActive(camera); }
  public AbstractCamera getActiveCamera() { return this.cameraController.getActiveCamera(); }

  public void setCameraOnManipulator(CameraInformedManipulator manipulator) {
    this.cameraController.setCameraOnManipulator(manipulator, this.currentInputState);
  }

  protected void addCameraMouseControl() { this.cameraController.addCameraMouseControl(this); }

  public void addSelectionListener(SelectionListener selectionListener) { this.selectionListeners.add(selectionListener); }

  private void fireSelecting(SelectionEvent e) {
    for (SelectionListener selectionListener : this.selectionListeners) {
      selectionListener.selecting(e);
    }
  }
  private void fireSelected(SelectionEvent e) {
    for (SelectionListener selectionListener : this.selectionListeners) {
      selectionListener.selected(e);
    }
  }

  public void pushHandleSet(HandleSet handleSet) { this.handleManager.pushNewHandleSet(handleSet); }
  public void popHandleSet() { this.handleManager.popHandleSet(); }

  private void setToBeSelected(AbstractTransformableImp toBeSelected) {
    this.toBeSelected = toBeSelected;
    this.hasObjectToBeSelected = true;
  }

  protected void updateHandleSelection(AbstractTransformableImp selected) {}
  public boolean hasSceneEditor() { return false; }

  public void clear() {
    this.cameraController.clearCameraViews();
    this.handleManager.clear();
  }

  public void clearCameraViews() { this.cameraController.clearCameraViews(); }
  public void addCameraView(CameraView viewType, SymmetricPerspectiveCamera mainCamera) {
    this.cameraController.addCameraView(viewType, mainCamera);
  }
  public void addCameraView(CameraView viewType, SymmetricPerspectiveCamera mainCamera,
                             SymmetricPerspectiveCamera  layoutCamera, OrthographicCamera orthographicCamera) {
    this.cameraController.addCameraView(viewType, mainCamera, layoutCamera, orthographicCamera);
  }

  public void setSelectedCameraMarker(CameraMarkerImp selected) {
    if (selected != this.selectedCameraMarker) {
      this.fireSelecting(new SelectionEvent(this, selected));
      if (this.selectedCameraMarker != null) {
        this.selectedCameraMarker.opacity.setValue(.3f);
        if (this.selectedCameraMarker instanceof PerspectiveCameraMarkerImp imp) {
          imp.setDetailedViewShowing(false);
        }
      }
      this.selectedCameraMarker = selected;
      if (this.selectedCameraMarker != null) {
        this.selectedCameraMarker.opacity.setValue(1f);
        if (this.hasSceneEditor() && (this.selectedCameraMarker instanceof PerspectiveCameraMarkerImp imp)) {
          imp.setDetailedViewShowing(true);
        }
      }
    }
  }

  public void setSelectedObjectMarker(ObjectMarkerImp selected) {
    if (selected != this.selectedObjectMarker) {
      this.fireSelecting(new SelectionEvent(this, selected));
      if (this.selectedObjectMarker != null) {
        this.selectedObjectMarker.opacity.setValue(.3f);
      }
      this.selectedObjectMarker = selected;
      if (this.selectedObjectMarker != null) {
        this.selectedObjectMarker.opacity.setValue(1f);
      }
    }
  }

  protected void setHandleSelectionState(HandleStyle handleStyle) {
    this.setInteractionState(handleStyle);
  }

  public void setSelectedImplementation(AbstractTransformableImp selected) {
    if (this.isInStageChange) {
      this.setToBeSelected(selected);
      return;
    }
    if (selected != null) {
      if (selected.getSgComposite() instanceof Joint) {
        if ((this.selectedObject == null) || !(this.selectedObject.getSgComposite() instanceof Joint)) {
          if (this.getDefaultJointHandleStyle() != null) {
            this.setHandleSelectionState(this.getDefaultJointHandleStyle());
          }
        }
      }
      if (selected instanceof ObjectMarkerImp objectMarker) {
        setSelectedObjectMarker(objectMarker);
      } else if (selected instanceof CameraMarkerImp cameraMarker) {
        setSelectedCameraMarker(cameraMarker);
      } else {
        setSelectedSceneObjectImplementation(selected);
      }
      if (this.handleManager.getCurrentHandleSet() == null) {
        this.setCurrentInteractionState(this.currentInteractionState);
      }
      updateHandleSelection(selected);
    } else {
      setSelectedSceneObjectImplementation(null);
    }
  }

  protected void fireStateChange() {
    this.isInStageChange = true;
    try {
      this.eventHandler.handleStateChange();
    } finally {
      this.isInStageChange = false;
    }
    if (this.hasObjectToBeSelected) {
      this.hasObjectToBeSelected = false;
      this.setSelectedImplementation(this.toBeSelected);
    }
  }

  private void setSelectedObjectSilhouetteIfAppropriate(boolean isHaloed) {
    if (this.sgSilhouette != null) {
      if (this.selectedObject instanceof ModelImp modelImp) {
        for (Visual sgVisual : modelImp.getSgVisuals()) {
          sgVisual.silouette.setValue(isHaloed ? this.sgSilhouette : null);
        }
      }
    }
  }

  private void setSelectedSceneObjectImplementation(AbstractTransformableImp selected) {
    if (this.selectedObject != selected) {
      this.fireSelecting(new SelectionEvent(this, selected));
      this.setSelectedObjectSilhouetteIfAppropriate(false);
      AbstractTransformable sgTransformable = selected != null ? selected.getSgComposite() : null;
      if (HandleManager.isSelectable(sgTransformable)) {
        this.handleManager.setHandlesShowing(true);
        this.handleManager.setSelectedObject(sgTransformable);
      } else {
        this.handleManager.setSelectedObject(null);
      }
      this.currentInputState.setCurrentlySelectedObject(sgTransformable);
      this.currentInputState.setTimeCaptured();
      selectedObject = selected;
      this.setSelectedObjectSilhouetteIfAppropriate(true);
      this.fireStateChange();
    }
  }

  public void setHandleShowingForSelectedImplementation(AbstractTransformableImp object, boolean handlesShowing) {
    if (this.selectedObject == object) {
      this.handleManager.setHandlesShowing(handlesShowing);
    }
  }

  public void setHandleVisibility(boolean isVisible) { this.handleManager.setHandlesShowing(isVisible); }

  public void triggerImplementationSelection(AbstractTransformableImp selected) {
    if (this.selectedObject != selected) {
      this.fireSelected(new SelectionEvent(this, selected));
    }
  }
  public void triggerSgObjectSelection(AbstractTransformable selected) {
    triggerImplementationSelection(EntityImp.getInstance(selected, AbstractTransformableImp.class));
  }

  protected void setSgSilhouette(Silhouette sgSilhouette) { this.sgSilhouette = sgSilhouette; }

  AbstractCamera getSGCamera() {
    OnscreenRenderTarget rt = this.getOnscreenRenderTarget();
    if (rt != null && 0 < rt.getSgCameraCount()) {
      return rt.getSgCameraAt(0);
    }
    return null;
  }

  public void addHandle(ManipulationHandle handle) { this.handleManager.addHandle(handle); }
  private HandleStyle getDefaultJointHandleStyle() { return HandleStyle.ROTATION; }
  public boolean shouldSnapToGround() { return false; }
  public boolean shouldSnapToGrid() { return false; }
  public boolean shouldSnapToRotation() { return false; }
  public double getGridSpacing() { return 1.0; }
  public Angle getRotationSnapAngle() { return new AngleInRadians(Math.PI / 16.0); }
  public void undoRedoEndManipulation(AbstractManipulator manipulator, AffineMatrix4x4 originalTransformation) {}

  public void clearMouseAndKeyboardState() {
    this.currentInputState.clearKeyState();
    this.currentInputState.clearMouseState();
    this.currentInputState.clearMouseWheelState();
    this.fireStateChange();
  }

  protected void handleMouseEntered(MouseEvent e) { this.eventHandler.handleMouseEntered(e); }
  protected void handleMouseMoved(MouseEvent e) { this.eventHandler.handleMouseMoved(e); }
  public void setSGCamera(AbstractCamera camera) {}

  protected void update(double timeDelta) {
    this.eventHandler.updateMouseWheelTimeout(timeDelta, fired -> this.fireStateChange());
    for (ManipulatorConditionSet currentManipulatorSet : this.manipulators) {
      if (currentManipulatorSet.getManipulator().hasStarted() && currentManipulatorSet.shouldContinue(this.currentInputState, this.previousInputState)) {
        currentManipulatorSet.getManipulator().timeUpdateManipulator(timeDelta, this.currentInputState);
      }
    }
  }

  // Package-private accessors for DragEventHandler and DragCameraController
  List<ManipulatorConditionSet> getManipulators() { return this.manipulators; }
  InputState getPreviousInputState() { return this.previousInputState; }
  HandleManager getHandleManager() { return this.handleManager; }
  boolean getHasObjectToBeSelected() { return this.hasObjectToBeSelected; }

  public enum ObjectType {
    JOINT, MODEL, CAMERA_MARKER, OBJECT_MARKER, MAIN_CAMERA, UNKNOWN, ANY;

    public static ObjectType getObjectType(AbstractTransformableImp selected) {
      if (selected instanceof JointImp) {
        return ObjectType.JOINT;
      } else if (selected instanceof ObjectMarkerImp) {
        return ObjectType.OBJECT_MARKER;
      } else if (selected instanceof CameraMarkerImp) {
        return ObjectType.CAMERA_MARKER;
      } else if (selected instanceof ModelImp) {
        return ObjectType.MODEL;
      } else if (selected instanceof CameraImp || selected instanceof VrUserImp) {
        return ObjectType.MAIN_CAMERA;
      } else {
        return ObjectType.UNKNOWN;
      }
    }
  }

  public enum CameraView {
    MAIN, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ACTIVE_VIEW, PICK_CAMERA
  }
}
