package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the sealed {@link Chunk} hierarchy — verifies all permitted subtypes.
 */
public class ChunkTest {

  @Test
  public void textChunk_isChunk() {
    Chunk c = new TextChunk("text");
    assertNotNull(c);
  }

  @Test
  public void propertyChunk_isChunk() {
    Chunk c = new PropertyChunk("prop");
    assertNotNull(c);
  }

  @Test
  public void methodInvocationChunk_isChunk() {
    Chunk c = new MethodInvocationChunk("method()");
    assertNotNull(c);
  }

  @Test
  public void getsChunk_isChunk() {
    Chunk c = new GetsChunk(true);
    assertNotNull(c);
  }

  @Test
  public void sealedPermits_exactlyFourSubtypes() {
    Class<?>[] permitted = Chunk.class.getPermittedSubclasses();
    assertNotNull(permitted);
    assertEquals(4, permitted.length);
  }

  @Test
  public void allPermittedSubclasses_areCorrectTypes() {
    Class<?>[] permitted = Chunk.class.getPermittedSubclasses();
    assertNotNull(permitted);
    boolean hasText = false, hasProperty = false, hasMethod = false, hasGets = false;
    for (Class<?> cls : permitted) {
      if (cls == TextChunk.class) hasText = true;
      if (cls == PropertyChunk.class) hasProperty = true;
      if (cls == MethodInvocationChunk.class) hasMethod = true;
      if (cls == GetsChunk.class) hasGets = true;
    }
    assertTrue("Missing TextChunk", hasText);
    assertTrue("Missing PropertyChunk", hasProperty);
    assertTrue("Missing MethodInvocationChunk", hasMethod);
    assertTrue("Missing GetsChunk", hasGets);
  }
}
