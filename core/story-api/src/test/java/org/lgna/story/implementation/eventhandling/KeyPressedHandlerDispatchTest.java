package org.lgna.story.implementation.eventhandling;

import org.junit.Test;
import org.lgna.story.HeldKeyPolicy;
import org.lgna.story.MultipleEventPolicy;
import org.lgna.story.event.ArrowKeyEvent;
import org.lgna.story.event.ArrowKeyPressListener;
import org.lgna.story.event.KeyPressListener;
import org.lgna.story.event.NumberKeyEvent;
import org.lgna.story.event.NumberKeyPressListener;

import java.awt.Canvas;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeyPressedHandlerDispatchTest {
  private static final Canvas SOURCE = new Canvas();

  /**
   * Generous timeout for <em>positive</em> dispatch assertions. {@link KeyPressedHandler} dispatches
   * asynchronously on a {@link org.lgna.common.ComponentExecutor} backed by a shared cached thread
   * pool, so a correctly-dispatched latch is already at zero by the time we await it — this timeout
   * therefore adds no latency to passing runs and exists solely to absorb pathological CI-load
   * scheduling delay that a tight window would flake on. Negative "nothing dispatched" checks
   * intentionally use a short window instead.
   */
  private static final long DISPATCH_TIMEOUT_SECONDS = 10L;

  private static org.lgna.story.event.KeyEvent keyPressed(int code, char keyChar) {
    return new org.lgna.story.event.KeyEvent(new java.awt.event.KeyEvent(
        SOURCE,
        java.awt.event.KeyEvent.KEY_PRESSED,
        System.currentTimeMillis(),
        0,
        code,
        keyChar));
  }

  private static org.lgna.story.event.KeyEvent keyReleased(int code, char keyChar) {
    return new org.lgna.story.event.KeyEvent(new java.awt.event.KeyEvent(
        SOURCE,
        java.awt.event.KeyEvent.KEY_RELEASED,
        System.currentTimeMillis(),
        0,
        code,
        keyChar));
  }

  @Test
  public void fireOnceOnPressOnlyDispatchesAfterRelease() throws Exception {
    KeyPressedHandler handler = new KeyPressedHandler();
    AtomicInteger count = new AtomicInteger();
    CountDownLatch firstDispatch = new CountDownLatch(1);
    CountDownLatch secondDispatch = new CountDownLatch(1);

    handler.addListener((KeyPressListener) e -> {
      if (count.incrementAndGet() == 1) {
        firstDispatch.countDown();
      } else if (count.get() == 2) {
        secondDispatch.countDown();
      }
    }, MultipleEventPolicy.IGNORE, HeldKeyPolicy.FIRE_ONCE_ON_PRESS);

    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_A, 'a'));
    assertTrue("first press should dispatch", firstDispatch.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));

    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_A, 'a'));
    assertFalse("repeat press without release should not dispatch again",
        secondDispatch.await(200, TimeUnit.MILLISECONDS));

    handler.handleKeyRelease(keyReleased(java.awt.event.KeyEvent.VK_A, 'a'));
    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_A, 'a'));
    assertTrue("press after release should dispatch again", secondDispatch.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertEquals(2, count.get());
  }

  @Test
  public void fireOnceOnReleaseDispatchesOnlyOnRelease() throws Exception {
    KeyPressedHandler handler = new KeyPressedHandler();
    CountDownLatch released = new CountDownLatch(1);
    AtomicInteger count = new AtomicInteger();

    handler.addListener((KeyPressListener) e -> {
      count.incrementAndGet();
      released.countDown();
    }, MultipleEventPolicy.IGNORE, HeldKeyPolicy.FIRE_ONCE_ON_RELEASE);

    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_B, 'b'));
    assertEquals("press should not dispatch release-only listener", 0, count.get());

    handler.handleKeyRelease(keyReleased(java.awt.event.KeyEvent.VK_B, 'b'));
    assertTrue("release should dispatch listener", released.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertEquals(1, count.get());
  }

  @Test
  public void arrowListenerFiltersToArrowKeys() throws Exception {
    KeyPressedHandler handler = new KeyPressedHandler();
    CountDownLatch arrowPressed = new CountDownLatch(1);
    AtomicInteger count = new AtomicInteger();

    handler.addListener((ArrowKeyPressListener) e -> {
      count.incrementAndGet();
      arrowPressed.countDown();
    }, MultipleEventPolicy.IGNORE, ArrowKeyEvent.ARROWS, HeldKeyPolicy.FIRE_ONCE_ON_PRESS);

    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_A, 'a'));
    assertEquals(0, count.get());

    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.KeyEvent.CHAR_UNDEFINED));
    assertTrue("left arrow should dispatch", arrowPressed.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertEquals(1, count.get());
  }

  @Test
  public void numberListenerFiltersToNumberKeys() throws Exception {
    KeyPressedHandler handler = new KeyPressedHandler();
    CountDownLatch numberPressed = new CountDownLatch(1);
    AtomicInteger count = new AtomicInteger();

    handler.addListener((NumberKeyPressListener) e -> {
      count.incrementAndGet();
      numberPressed.countDown();
    }, MultipleEventPolicy.IGNORE, NumberKeyEvent.NUMBERS, HeldKeyPolicy.FIRE_ONCE_ON_PRESS);

    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_1, '1'));
    assertTrue("numeric key should dispatch", numberPressed.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertEquals(1, count.get());
  }

  @Test
  public void silencedHandlerDoesNotDispatchUntilRestored() throws Exception {
    KeyPressedHandler handler = new KeyPressedHandler();
    CountDownLatch restoredDispatch = new CountDownLatch(1);
    AtomicInteger count = new AtomicInteger();

    handler.addListener((KeyPressListener) e -> {
      count.incrementAndGet();
      restoredDispatch.countDown();
    }, MultipleEventPolicy.IGNORE, HeldKeyPolicy.FIRE_ONCE_ON_PRESS);

    handler.silenceListeners();
    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_C, 'c'));
    assertEquals(0, count.get());

    handler.restoreListeners();
    handler.handleKeyPress(keyPressed(java.awt.event.KeyEvent.VK_C, 'c'));
    assertTrue("restored handler should dispatch", restoredDispatch.await(DISPATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertEquals(1, count.get());
  }
}
