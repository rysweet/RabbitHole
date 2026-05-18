package org.alice.ide.ast;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link AstEventManager}.
 */
public class AstEventManagerExtendedTest {

  @After
  public void cleanUp() {
    // Remove any listeners we added during tests to avoid side effects
  }

  @Test
  public void addListener_fireListeners_listenerInvoked() {
    final AtomicInteger count = new AtomicInteger(0);
    AstEventManager.TypeHierarchyListener listener = count::incrementAndGet;
    AstEventManager.addTypeHierarchyListener(listener);
    try {
      AstEventManager.fireTypeHierarchyListeners();
      assertEquals(1, count.get());
    } finally {
      AstEventManager.removeTypeHierarchyListener(listener);
    }
  }

  @Test
  public void addAndInvokeListener_invokedImmediately() {
    final AtomicInteger count = new AtomicInteger(0);
    AstEventManager.TypeHierarchyListener listener = count::incrementAndGet;
    AstEventManager.addAndInvokeTypeHierarchyListener(listener);
    try {
      assertEquals(1, count.get());
    } finally {
      AstEventManager.removeTypeHierarchyListener(listener);
    }
  }

  @Test
  public void addAndInvoke_thenFire_invokedTwice() {
    final AtomicInteger count = new AtomicInteger(0);
    AstEventManager.TypeHierarchyListener listener = count::incrementAndGet;
    AstEventManager.addAndInvokeTypeHierarchyListener(listener);
    try {
      AstEventManager.fireTypeHierarchyListeners();
      assertEquals(2, count.get());
    } finally {
      AstEventManager.removeTypeHierarchyListener(listener);
    }
  }

  @Test
  public void removeListener_noLongerInvoked() {
    final AtomicInteger count = new AtomicInteger(0);
    AstEventManager.TypeHierarchyListener listener = count::incrementAndGet;
    AstEventManager.addTypeHierarchyListener(listener);
    AstEventManager.removeTypeHierarchyListener(listener);
    AstEventManager.fireTypeHierarchyListeners();
    assertEquals(0, count.get());
  }

  @Test
  public void multipleListeners_allInvoked() {
    final AtomicInteger count1 = new AtomicInteger(0);
    final AtomicInteger count2 = new AtomicInteger(0);
    AstEventManager.TypeHierarchyListener l1 = count1::incrementAndGet;
    AstEventManager.TypeHierarchyListener l2 = count2::incrementAndGet;
    AstEventManager.addTypeHierarchyListener(l1);
    AstEventManager.addTypeHierarchyListener(l2);
    try {
      AstEventManager.fireTypeHierarchyListeners();
      assertEquals(1, count1.get());
      assertEquals(1, count2.get());
    } finally {
      AstEventManager.removeTypeHierarchyListener(l1);
      AstEventManager.removeTypeHierarchyListener(l2);
    }
  }

  @Test
  public void fireMultipleTimes_eachInvokesListener() {
    final AtomicInteger count = new AtomicInteger(0);
    AstEventManager.TypeHierarchyListener listener = count::incrementAndGet;
    AstEventManager.addTypeHierarchyListener(listener);
    try {
      AstEventManager.fireTypeHierarchyListeners();
      AstEventManager.fireTypeHierarchyListeners();
      AstEventManager.fireTypeHierarchyListeners();
      assertEquals(3, count.get());
    } finally {
      AstEventManager.removeTypeHierarchyListener(listener);
    }
  }

  @Test
  public void removeNonExistentListener_noException() {
    AstEventManager.TypeHierarchyListener listener = () -> {};
    // Should not throw
    AstEventManager.removeTypeHierarchyListener(listener);
  }
}
