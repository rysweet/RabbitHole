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

import com.jogamp.opengl.GLException;
import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import edu.cmu.cs.dennisc.render.PickFrontMostObserver;
import edu.cmu.cs.dennisc.render.PickResult;
import edu.cmu.cs.dennisc.render.PickSubElementPolicy;
import org.alice.interact.handle.HandleManager;
import org.alice.interact.handle.ManipulationHandle;
import org.alice.interact.condition.ManipulatorConditionSet;
import org.alice.interact.manipulator.AbstractManipulator;
import org.alice.interact.manipulator.CameraInformedManipulator;
import org.alice.interact.manipulator.OnscreenPicturePlaneInformedManipulator;
import org.lgna.story.implementation.AbstractTransformableImp;
import org.lgna.story.implementation.EntityImp;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles AWT listener routing, mouse/keyboard event processing, manipulator
 * state orchestration, mouse wheel state management, and scene picking for
 * {@link DragAdapter}. Protected-overridable events (mouseEntered, mouseMoved)
 * route through {@link DragAdapter} so subclass overrides still fire correctly.
 */
class DragEventHandler {
  private static final double MOUSE_WHEEL_TIMEOUT_TIME = 1.0;
  private static final double CANCEL_MOUSE_WHEEL_DISTANCE = 3;

  private final DragAdapter dragAdapter;
  private Component currentRolloverComponent;
  private double mouseWheelTimeoutTime;
  private Point mouseWheelStartLocation;

  private final MouseWheelListener mouseWheelListener = this::handleMouseWheelMoved;
  private final MouseMotionListener mouseMotionListener = new MouseMotionListener() {
    @Override
    public void mouseMoved(MouseEvent e) { dragAdapter.handleMouseMoved(e); }
    @Override
    public void mouseDragged(MouseEvent e) { handleMouseDragged(e); }
  };
  private final MouseListener mouseListener = new MouseListener() {
    @Override
    public void mouseEntered(MouseEvent e) { dragAdapter.handleMouseEntered(e); }
    @Override
    public void mouseExited(MouseEvent e) { handleMouseExited(e); }
    @Override
    public void mousePressed(MouseEvent e) { handleMousePressed(e); }
    @Override
    public void mouseReleased(MouseEvent e) { handleMouseReleased(e); }
    @Override
    public void mouseClicked(MouseEvent e) {}
  };
  private final KeyListener keyListener = new KeyListener() {
    @Override
    public void keyPressed(KeyEvent e) { handleKeyPressed(e); }
    @Override
    public void keyReleased(KeyEvent e) { handleKeyReleased(e); }
    @Override
    public void keyTyped(KeyEvent e) {}
  };

  DragEventHandler(DragAdapter dragAdapter) {
    this.dragAdapter = dragAdapter;
  }

  // --- Listener management ---

  boolean isComponentListener(Component component) {
    for (MouseListener listener : component.getMouseListeners()) {
      if (listener == mouseListener) {
        return true;
      }
    }
    for (MouseMotionListener listener : component.getMouseMotionListeners()) {
      if (listener == mouseMotionListener) {
        return true;
      }
    }
    for (KeyListener listener : component.getKeyListeners()) {
      if (listener == keyListener) {
        return true;
      }
    }
    for (MouseWheelListener listener : component.getMouseWheelListeners()) {
      if (listener == mouseWheelListener) {
        return true;
      }
    }
    return false;
  }

  void addListeners(Component component) {
    if (!isComponentListener(component)) {
      component.addMouseListener(this.mouseListener);
      component.addMouseMotionListener(this.mouseMotionListener);
      component.addKeyListener(this.keyListener);
      component.addMouseWheelListener(this.mouseWheelListener);
    }
  }

  void removeListeners(Component component) {
    if (isComponentListener(component)) {
      component.removeMouseListener(this.mouseListener);
      component.removeMouseMotionListener(this.mouseMotionListener);
      component.removeKeyListener(this.keyListener);
      component.removeMouseWheelListener(this.mouseWheelListener);
    }
  }

  // --- Component utilities ---

  ManipulationHandle getHandleForComponent(Component c) {
    if (c == null) {
      return null;
    }
    if (c instanceof ManipulationHandle handle) {
      return handle;
    }
    return getHandleForComponent(c.getParent());
  }

  // --- Picking ---

  void pickIntoSceneSuppressingErrors(Point mouseLocation, PickFrontMostObserver observer) {
    try {
      pickIntoScene(mouseLocation, observer);
    } catch (GLException gle) {
      Logger.errln("Error picking into scene", gle);
    }
  }

  void pickIntoScene(Point mouseLocation, PickFrontMostObserver observer) {
    OnscreenRenderTarget onscreenRenderTarget = dragAdapter.getOnscreenRenderTarget();
    assert onscreenRenderTarget != null;
    PickResult pickResult = onscreenRenderTarget.getSynchronousPicker().pickFrontMost(mouseLocation, PickSubElementPolicy.NOT_REQUIRED);
    observer.done(pickResult);
  }

  // --- Mouse wheel state ---

  boolean isMouseWheelActive() { return this.mouseWheelTimeoutTime > 0; }

  void stopMouseWheel(InputState inputState) {
    this.mouseWheelTimeoutTime = 0;
    inputState.setMouseWheelState(0);
    this.mouseWheelStartLocation = null;
  }

  boolean shouldStopMouseWheel(Point currentMouse) {
    if (this.mouseWheelStartLocation != null) {
      double distance = currentMouse.distance(this.mouseWheelStartLocation);
      return distance > CANCEL_MOUSE_WHEEL_DISTANCE;
    }
    return false;
  }

  void updateMouseWheelTimeout(double timeDelta, Consumer<Boolean> onExpired) {
    if (isMouseWheelActive()) {
      mouseWheelTimeoutTime -= timeDelta;
      if (!isMouseWheelActive()) {
        stopMouseWheel(dragAdapter.currentInputState);
        onExpired.accept(true);
      }
    }
  }

  // --- Manipulator state orchestration ---

  void handleStateChange() {
    InputState currentInputState = dragAdapter.currentInputState;
    InputState previousInputState = dragAdapter.getPreviousInputState();
    List<AbstractManipulator> toStart = Lists.newLinkedList();
    List<AbstractManipulator> toEnd = Lists.newLinkedList();
    List<AbstractManipulator> toUpdate = Lists.newLinkedList();
    List<AbstractManipulator> toClick = Lists.newLinkedList();
    for (ManipulatorConditionSet currentManipulatorSet : dragAdapter.getManipulators()) {
      currentManipulatorSet.update(currentInputState, previousInputState);
      if (currentManipulatorSet.isEnabled()) {
        if (currentManipulatorSet.stateChanged(currentInputState, previousInputState)) {
          if (currentManipulatorSet.shouldContinue(currentInputState, previousInputState)) {
            toUpdate.add(currentManipulatorSet.getManipulator());
          } else if (currentManipulatorSet.justStarted(currentInputState, previousInputState)) {
            toStart.add(currentManipulatorSet.getManipulator());
          } else if (currentManipulatorSet.justEnded(currentInputState, previousInputState)) {
            toEnd.add(currentManipulatorSet.getManipulator());
          } else if (currentManipulatorSet.clicked(currentInputState, previousInputState)) {
            toClick.add(currentManipulatorSet.getManipulator());
          }
        }
      } else {
        if (currentManipulatorSet.getManipulator().hasStarted()) {
          toEnd.add(currentManipulatorSet.getManipulator());
        }
      }
    }
    for (AbstractManipulator toEndManipulator : toEnd) {
      toEndManipulator.endManipulator(currentInputState, previousInputState);
    }
    for (AbstractManipulator toClickManipulator : toClick) {
      setManipulatorStartState(toClickManipulator);
      toClickManipulator.clickManipulator(currentInputState, previousInputState);
    }
    for (AbstractManipulator toStartManipulator : toStart) {
      setManipulatorStartState(toStartManipulator);
      toStartManipulator.startManipulator(currentInputState);
    }
    for (AbstractManipulator toUpdateManipulator : toUpdate) {
      if (toStart.contains(toUpdateManipulator)) {
        toUpdateManipulator.dataUpdateManipulator(currentInputState, currentInputState);
      } else {
        toUpdateManipulator.dataUpdateManipulator(currentInputState, previousInputState);
      }
    }
    updateRollover(currentInputState, previousInputState);
    previousInputState.copyState(currentInputState);
  }

  private void setManipulatorStartState(AbstractManipulator manipulator) {
    if (manipulator instanceof OnscreenPicturePlaneInformedManipulator lookingGlassManipulator) {
      dragAdapter.setLookingGlassOnManipulator(lookingGlassManipulator);
    }
    if (manipulator instanceof CameraInformedManipulator cameraInformed) {
      dragAdapter.setCameraOnManipulator(cameraInformed);
    }
  }

  private void updateRollover(InputState currentInputState, InputState previousInputState) {
    HandleManager handleManager = dragAdapter.getHandleManager();
    if (currentInputState.getRolloverHandle() != previousInputState.getRolloverHandle()) {
      if (currentInputState.getRolloverHandle() != null) {
        handleManager.setHandleRollover(currentInputState.getRolloverHandle(), true);
      }
      if (previousInputState.getRolloverHandle() != null) {
        handleManager.setHandleRollover(previousInputState.getRolloverHandle(), false);
      }
    }
    if (!dragAdapter.getHasObjectToBeSelected() && (currentInputState.getCurrentlySelectedObject() != previousInputState.getCurrentlySelectedObject())) {
      dragAdapter.triggerImplementationSelection(EntityImp.getInstance(currentInputState.getCurrentlySelectedObject(), AbstractTransformableImp.class));
    }
  }

  // --- Event handle methods ---

  void handleMouseEntered(MouseEvent e) {
    this.currentRolloverComponent = e.getComponent();
    InputState inputState = dragAdapter.currentInputState;
    if (!inputState.isAnyMouseButtonDown()) {
      inputState.setMouseLocation(e.getPoint());
      if (e.getComponent() == dragAdapter.getAWTComponent()) {
        pickIntoSceneSuppressingErrors(e.getPoint(), inputState::setRolloverPickResult);
      } else {
        inputState.setRolloverHandle(getHandleForComponent(e.getComponent()));
      }
      inputState.setTimeCaptured();
      inputState.setInputEvent(e);
      dragAdapter.fireStateChange();
    }
  }

  void handleMouseMoved(MouseEvent e) {
    InputState inputState = dragAdapter.currentInputState;
    if (!inputState.getIsDragEvent()) {
      inputState.setMouseLocation(e.getPoint());
      if (e.getComponent() == dragAdapter.getAWTComponent()) {
        if (!inputState.isAnyMouseButtonDown()) {
          pickIntoSceneSuppressingErrors(e.getPoint(), inputState::setRolloverPickResult);
        }
      } else {
        inputState.setRolloverHandle(getHandleForComponent(e.getComponent()));
      }
      inputState.setTimeCaptured();
      inputState.setInputEvent(e);
      if (shouldStopMouseWheel(e.getPoint())) {
        stopMouseWheel(inputState);
      }
      dragAdapter.fireStateChange();
    }
  }

  private void handleMouseExited(MouseEvent e) {
    this.currentRolloverComponent = null;
    InputState inputState = dragAdapter.currentInputState;
    if (!inputState.isAnyMouseButtonDown()) {
      inputState.setMouseLocation(e.getPoint());
      inputState.setRolloverHandle(null);
      inputState.setRolloverPickResult(null);
      inputState.setTimeCaptured();
      inputState.setInputEvent(e);
      dragAdapter.fireStateChange();
    }
  }

  private void handleMousePressed(MouseEvent e) {
    InputState inputState = dragAdapter.currentInputState;
    inputState.setMouseState(e.getButton(), true);
    inputState.setMouseLocation(e.getPoint());
    inputState.setInputEventType(InputState.InputEventType.MOUSE_DOWN);
    inputState.setInputEvent(e);
    e.getComponent().requestFocus();
    if (e.getComponent() == dragAdapter.getAWTComponent()) {
      pickIntoScene(e.getPoint(), inputState::setClickPickResult);
    } else {
      inputState.setClickHandle(getHandleForComponent(e.getComponent()));
    }
    inputState.setTimeCaptured();
    stopMouseWheel(inputState);
    dragAdapter.fireStateChange();
  }

  private void handleMouseReleased(MouseEvent e) {
    InputState inputState = dragAdapter.currentInputState;
    inputState.setMouseState(e.getButton(), false);
    inputState.setMouseLocation(e.getPoint());
    inputState.setInputEventType(InputState.InputEventType.MOUSE_UP);
    inputState.setInputEvent(e);
    if (this.currentRolloverComponent == dragAdapter.getAWTComponent()) {
      pickIntoScene(e.getPoint(), inputState::setRolloverPickResult);
    } else {
      inputState.setRolloverHandle(getHandleForComponent(this.currentRolloverComponent));
    }
    inputState.setTimeCaptured();
    dragAdapter.fireStateChange();
  }

  private void handleMouseDragged(MouseEvent e) {
    try {
      InputState inputState = dragAdapter.currentInputState;
      inputState.setMouseLocation(e.getPoint());
      inputState.setInputEventType(InputState.InputEventType.MOUSE_DRAGGED);
      inputState.setTimeCaptured();
      inputState.setInputEvent(e);
      dragAdapter.fireStateChange();
    } catch (RuntimeException re) {
      re.printStackTrace();
    }
  }

  private void handleMouseWheelMoved(MouseWheelEvent e) {
    InputState inputState = dragAdapter.currentInputState;
    inputState.setMouseWheelState(e.getWheelRotation());
    inputState.setInputEventType(InputState.InputEventType.MOUSE_WHEEL);
    inputState.setTimeCaptured();
    inputState.setInputEvent(e);
    if (this.mouseWheelStartLocation == null) {
      this.mouseWheelStartLocation = new Point(e.getPoint());
    }
    this.mouseWheelTimeoutTime = MOUSE_WHEEL_TIMEOUT_TIME;
    dragAdapter.fireStateChange();
  }

  private void handleKeyPressed(KeyEvent e) {
    InputState inputState = dragAdapter.currentInputState;
    inputState.setKeyState(e.getKeyCode(), true);
    inputState.setInputEventType(InputState.InputEventType.KEY_DOWN);
    inputState.setTimeCaptured();
    inputState.setInputEvent(e);
    dragAdapter.fireStateChange();
  }

  private void handleKeyReleased(KeyEvent e) {
    InputState inputState = dragAdapter.currentInputState;
    inputState.setKeyState(e.getKeyCode(), false);
    inputState.setInputEventType(InputState.InputEventType.KEY_UP);
    inputState.setTimeCaptured();
    inputState.setInputEvent(e);
    dragAdapter.fireStateChange();
  }

  // --- Listener getters ---

  MouseListener getMouseListener() { return this.mouseListener; }
  MouseMotionListener getMouseMotionListener() { return this.mouseMotionListener; }
  KeyListener getKeyListener() { return this.keyListener; }
  MouseWheelListener getMouseWheelListener() { return this.mouseWheelListener; }

  // --- Test accessors ---

  void setMouseWheelTimeoutForTest(double timeout) { this.mouseWheelTimeoutTime = timeout; }
  void setMouseWheelStartLocationForTest(Point location) { this.mouseWheelStartLocation = location; }
  Point getMouseWheelStartLocationForTest() { return this.mouseWheelStartLocation; }
}
