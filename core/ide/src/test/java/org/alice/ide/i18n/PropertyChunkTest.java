package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link PropertyChunk} — handles underscore-wrapped property names.
 */
public class PropertyChunkTest {

  @Test
  public void noUnderscores_propertyNameUnchanged() {
    PropertyChunk pc = new PropertyChunk("myProperty");
    assertEquals("myProperty", pc.getPropertyName());
    assertEquals(0, pc.getUnderscoreCount());
  }

  @Test
  public void singleUnderscore_strippedFromBothEnds() {
    PropertyChunk pc = new PropertyChunk("_wrapped_");
    assertEquals("wrapped", pc.getPropertyName());
    assertEquals(1, pc.getUnderscoreCount());
  }

  @Test
  public void doubleUnderscore_strippedFromBothEnds() {
    PropertyChunk pc = new PropertyChunk("__bold__");
    assertEquals("bold", pc.getPropertyName());
    assertEquals(2, pc.getUnderscoreCount());
  }

  @Test
  public void underscoreOnlyAtStart_notStripped() {
    PropertyChunk pc = new PropertyChunk("_onlyStart");
    assertEquals("_onlyStart", pc.getPropertyName());
    assertEquals(0, pc.getUnderscoreCount());
  }

  @Test
  public void underscoreOnlyAtEnd_notStripped() {
    PropertyChunk pc = new PropertyChunk("onlyEnd_");
    assertEquals("onlyEnd_", pc.getPropertyName());
    assertEquals(0, pc.getUnderscoreCount());
  }

  @Test
  public void singleCharBetweenUnderscores_stripped() {
    PropertyChunk pc = new PropertyChunk("_x_");
    assertEquals("x", pc.getPropertyName());
    assertEquals(1, pc.getUnderscoreCount());
  }

  @Test
  public void updateRepr_containsPropertyName() {
    PropertyChunk pc = new PropertyChunk("test");
    StringBuilder sb = new StringBuilder();
    pc.updateRepr(sb);
    assertTrue(sb.toString().contains("propertyName=test"));
  }

  @Test
  public void isChunkSubtype() {
    PropertyChunk pc = new PropertyChunk("p");
    assertTrue(pc instanceof Chunk);
  }
}
