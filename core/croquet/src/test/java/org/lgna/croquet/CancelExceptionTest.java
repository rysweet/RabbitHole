package org.lgna.croquet;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CancelException} — all four constructor overloads.
 */
public class CancelExceptionTest {

  @Test
  public void defaultConstructor_createsException() {
    CancelException ce = new CancelException();
    assertNotNull(ce);
    assertNull(ce.getMessage());
    assertNull(ce.getCause());
  }

  @Test
  public void messageConstructor_storesMessage() {
    CancelException ce = new CancelException("test msg");
    assertEquals("test msg", ce.getMessage());
    assertNull(ce.getCause());
  }

  @Test
  public void causeConstructor_storesCause() {
    RuntimeException cause = new RuntimeException("root");
    CancelException ce = new CancelException(cause);
    assertSame(cause, ce.getCause());
  }

  @Test
  public void messageAndCauseConstructor_storesBoth() {
    RuntimeException cause = new RuntimeException("root");
    CancelException ce = new CancelException("msg", cause);
    assertEquals("msg", ce.getMessage());
    assertSame(cause, ce.getCause());
  }

  @Test
  public void extendsRuntimeException() {
    // Verify the type hierarchy — CancelException must remain a RuntimeException
    // so it propagates through catch-free code paths in the UI framework.
    assertEquals(RuntimeException.class, CancelException.class.getSuperclass());
  }

  @Test
  public void canBeCaughtAsRuntimeException() {
    boolean caught = false;
    try {
      throw new CancelException("test");
    } catch (RuntimeException e) {
      caught = true;
    }
    assertTrue(caught);
  }
}
