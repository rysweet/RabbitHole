package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link MethodInvocationChunk} — strips trailing "()" from method name.
 */
public class MethodInvocationChunkTest {

  @Test
  public void stripsTrailingParens() {
    MethodInvocationChunk mic = new MethodInvocationChunk("doWork()");
    assertEquals("doWork", mic.getMethodName());
  }

  @Test
  public void longerMethodName_stripsCorrectly() {
    MethodInvocationChunk mic = new MethodInvocationChunk("calculateTotal()");
    assertEquals("calculateTotal", mic.getMethodName());
  }

  @Test
  public void shortMethodName_stripsCorrectly() {
    MethodInvocationChunk mic = new MethodInvocationChunk("go()");
    assertEquals("go", mic.getMethodName());
  }

  @Test
  public void updateRepr_containsMethodName() {
    MethodInvocationChunk mic = new MethodInvocationChunk("test()");
    StringBuilder sb = new StringBuilder();
    mic.updateRepr(sb);
    assertTrue(sb.toString().contains("methodName=test"));
  }

  @Test
  public void isChunkSubtype() {
    MethodInvocationChunk mic = new MethodInvocationChunk("m()");
    assertTrue(mic instanceof Chunk);
  }
}
