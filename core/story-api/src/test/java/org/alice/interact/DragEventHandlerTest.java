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

import org.junit.Before;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for DragEventHandler — the extracted AWT listener routing,
 * handle* method helpers, mouse wheel state, and InputState transitions
 * from DragAdapter.
 *
 * These tests define the contract that the implementation must satisfy.
 * They will fail to compile until DragEventHandler is created.
 */
public class DragEventHandlerTest {

  private DragEventHandler eventHandler;
  private TestDragAdapter dragAdapter;

  @Before
  public void setUp() {
    dragAdapter = new TestDragAdapter();
    eventHandler = new DragEventHandler(dragAdapter);
  }

  // --- Listener registration ---

  @Test
  public void addListenersRegistersAllFourListenerTypes() {
    JPanel panel = new JPanel();
    eventHandler.addListeners(panel);

    assertTrue("MouseListener should be registered",
        hasListenerOfType(panel.getMouseListeners(), eventHandler.getMouseListener()));
    assertTrue("MouseMotionListener should be registered",
        hasListenerOfType(panel.getMouseMotionListeners(), eventHandler.getMouseMotionListener()));
    assertTrue("KeyListener should be registered",
        hasListenerOfType(panel.getKeyListeners(), eventHandler.getKeyListener()));
    assertTrue("MouseWheelListener should be registered",
        hasListenerOfType(panel.getMouseWheelListeners(), eventHandler.getMouseWheelListener()));
  }

  @Test
  public void addListenersIsIdempotent() {
    JPanel panel = new JPanel();
    eventHandler.addListeners(panel);
    int mouseListenerCount = panel.getMouseListeners().length;

    eventHandler.addListeners(panel);
    assertEquals("Duplicate addListeners should not add extra listeners",
        mouseListenerCount, panel.getMouseListeners().length);
  }

  @Test
  public void removeListenersRemovesAllFourListenerTypes() {
    JPanel panel = new JPanel();
    eventHandler.addListeners(panel);
    eventHandler.removeListeners(panel);

    assertFalse("MouseListener should be removed",
        hasListenerOfType(panel.getMouseListeners(), eventHandler.getMouseListener()));
    assertFalse("MouseMotionListener should be removed",
        hasListenerOfType(panel.getMouseMotionListeners(), eventHandler.getMouseMotionListener()));
    assertFalse("KeyListener should be removed",
        hasListenerOfType(panel.getKeyListeners(), eventHandler.getKeyListener()));
    assertFalse("MouseWheelListener should be removed",
        hasListenerOfType(panel.getMouseWheelListeners(), eventHandler.getMouseWheelListener()));
  }

  @Test
  public void removeListenersIsNoOpWhenNotRegistered() {
    JPanel panel = new JPanel();
    int beforeCount = panel.getMouseListeners().length;

    eventHandler.removeListeners(panel);
    assertEquals("removeListeners on unregistered component should be no-op",
        beforeCount, panel.getMouseListeners().length);
  }

  @Test
  public void isComponentListenerReturnsTrueAfterRegistration() {
    JPanel panel = new JPanel();
    eventHandler.addListeners(panel);

    assertTrue("isComponentListener should return true after addListeners",
        eventHandler.isComponentListener(panel));
  }

  @Test
  public void isComponentListenerReturnsFalseBeforeRegistration() {
    JPanel panel = new JPanel();

    assertFalse("isComponentListener should return false before addListeners",
        eventHandler.isComponentListener(panel));
  }

  @Test
  public void isComponentListenerReturnsFalseAfterRemoval() {
    JPanel panel = new JPanel();
    eventHandler.addListeners(panel);
    eventHandler.removeListeners(panel);

    assertFalse("isComponentListener should return false after removeListeners",
        eventHandler.isComponentListener(panel));
  }

  // --- Mouse wheel state management ---

  @Test
  public void mouseWheelIsNotActiveInitially() {
    assertFalse("Mouse wheel should not be active initially",
        eventHandler.isMouseWheelActive());
  }

  @Test
  public void stopMouseWheelClearsActiveState() {
    // Simulate mouse wheel activity by setting timeout directly
    eventHandler.setMouseWheelTimeoutForTest(1.0);
    assertTrue("Mouse wheel should be active after setting timeout",
        eventHandler.isMouseWheelActive());

    eventHandler.stopMouseWheel(dragAdapter.currentInputState);
    assertFalse("Mouse wheel should not be active after stopMouseWheel",
        eventHandler.isMouseWheelActive());
  }

  @Test
  public void stopMouseWheelClearsInputStateMouseWheel() {
    dragAdapter.currentInputState.setMouseWheelState(5);
    eventHandler.setMouseWheelTimeoutForTest(1.0);

    eventHandler.stopMouseWheel(dragAdapter.currentInputState);
    assertEquals("InputState mouse wheel should be cleared",
        0, dragAdapter.currentInputState.getMouseWheelState());
  }

  @Test
  public void stopMouseWheelClearsStartLocation() {
    eventHandler.setMouseWheelStartLocationForTest(new Point(100, 200));
    eventHandler.setMouseWheelTimeoutForTest(1.0);

    eventHandler.stopMouseWheel(dragAdapter.currentInputState);
    assertNull("Mouse wheel start location should be cleared",
        eventHandler.getMouseWheelStartLocationForTest());
  }

  // --- shouldStopMouseWheel distance check ---

  @Test
  public void shouldStopMouseWheelReturnsFalseWhenNoStartLocation() {
    assertFalse("shouldStopMouseWheel should return false with no start location",
        eventHandler.shouldStopMouseWheel(new Point(100, 100)));
  }

  @Test
  public void shouldStopMouseWheelReturnsFalseWhenWithinThreshold() {
    eventHandler.setMouseWheelStartLocationForTest(new Point(100, 100));
    // Distance of 1 pixel is well within the CANCEL_MOUSE_WHEEL_DISTANCE of 3
    assertFalse("shouldStopMouseWheel should return false within threshold",
        eventHandler.shouldStopMouseWheel(new Point(101, 100)));
  }

  @Test
  public void shouldStopMouseWheelReturnsTrueWhenBeyondThreshold() {
    eventHandler.setMouseWheelStartLocationForTest(new Point(100, 100));
    // Distance of 10 pixels exceeds the CANCEL_MOUSE_WHEEL_DISTANCE of 3
    assertTrue("shouldStopMouseWheel should return true beyond threshold",
        eventHandler.shouldStopMouseWheel(new Point(110, 100)));
  }

  @Test
  public void shouldStopMouseWheelReturnsFalseExactlyAtThreshold() {
    eventHandler.setMouseWheelStartLocationForTest(new Point(100, 100));
    // Distance exactly at 3 should NOT trigger (> not >=)
    assertFalse("shouldStopMouseWheel should return false exactly at threshold",
        eventHandler.shouldStopMouseWheel(new Point(103, 100)));
  }

  // --- updateMouseWheelTimeout ---

  @Test
  public void updateMouseWheelTimeoutDecrementsTimeRemaining() {
    eventHandler.setMouseWheelTimeoutForTest(1.0);
    AtomicBoolean callbackFired = new AtomicBoolean(false);

    eventHandler.updateMouseWheelTimeout(0.3, callbackFired::set);

    assertTrue("Mouse wheel should still be active after partial timeout",
        eventHandler.isMouseWheelActive());
    assertFalse("Callback should not fire when time remains",
        callbackFired.get());
  }

  @Test
  public void updateMouseWheelTimeoutFiresCallbackWhenExpired() {
    eventHandler.setMouseWheelTimeoutForTest(0.5);
    AtomicBoolean callbackFired = new AtomicBoolean(false);

    eventHandler.updateMouseWheelTimeout(0.6, callbackFired::set);

    assertFalse("Mouse wheel should not be active after timeout expires",
        eventHandler.isMouseWheelActive());
    assertTrue("Callback should fire when timeout expires",
        callbackFired.get());
  }

  @Test
  public void updateMouseWheelTimeoutIsNoOpWhenInactive() {
    AtomicBoolean callbackFired = new AtomicBoolean(false);

    eventHandler.updateMouseWheelTimeout(1.0, callbackFired::set);

    assertFalse("Callback should not fire when mouse wheel is not active",
        callbackFired.get());
  }

  // --- getHandleForComponent ---

  @Test
  public void getHandleForComponentReturnsNullForNull() {
    assertNull("getHandleForComponent(null) should return null",
        eventHandler.getHandleForComponent(null));
  }

  @Test
  public void getHandleForComponentReturnsNullForPlainComponent() {
    JPanel panel = new JPanel();
    assertNull("getHandleForComponent should return null for non-handle component",
        eventHandler.getHandleForComponent(panel));
  }

  // --- Helper methods ---

  private <T> boolean hasListenerOfType(T[] listeners, T expected) {
    for (T listener : listeners) {
      if (listener == expected) {
        return true;
      }
    }
    return false;
  }

  /**
   * Minimal concrete DragAdapter for testing. Since DragAdapter is abstract,
   * we need a thin subclass to instantiate.
   */
  static class TestDragAdapter extends DragAdapter {
  }
}
