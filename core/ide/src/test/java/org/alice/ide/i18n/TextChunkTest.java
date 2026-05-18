package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link TextChunk} — holds plain text segments.
 */
public class TextChunkTest {

  @Test
  public void constructor_storesText() {
    TextChunk tc = new TextChunk("hello");
    assertEquals("hello", tc.getText());
  }

  @Test
  public void emptyText_allowed() {
    TextChunk tc = new TextChunk("");
    assertEquals("", tc.getText());
  }

  @Test
  public void nullText_allowed() {
    TextChunk tc = new TextChunk(null);
    assertNull(tc.getText());
  }

  @Test
  public void updateRepr_containsText() {
    TextChunk tc = new TextChunk("world");
    StringBuilder sb = new StringBuilder();
    tc.updateRepr(sb);
    String repr = sb.toString();
    assertTrue(repr.contains("text="));
    assertTrue(repr.contains("world"));
  }

  @Test
  public void isChunkSubtype() {
    TextChunk tc = new TextChunk("x");
    assertTrue(tc instanceof Chunk);
  }
}
