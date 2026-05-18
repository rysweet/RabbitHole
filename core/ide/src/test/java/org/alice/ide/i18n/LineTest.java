package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link Line} — tab-based indentation counting,
 * tag parsing (property, method, gets), and chunk assembly.
 */
public class LineTest {

  // ---- indentation ----

  @Test
  public void noTabs_indentCountIsZero() {
    Line line = new Line("hello");
    assertEquals(0, line.getIndentCount());
  }

  @Test
  public void singleTab_indentCountIsOne() {
    Line line = new Line("\thello");
    assertEquals(1, line.getIndentCount());
  }

  @Test
  public void multipleTabs_indentCountCorrect() {
    Line line = new Line("\t\t\thello");
    assertEquals(3, line.getIndentCount());
  }

  @Test
  public void tabsOnly_indentCountMatchesTabCount() {
    Line line = new Line("\t\t");
    assertEquals(2, line.getIndentCount());
  }

  // ---- plain text (no tags) ----

  @Test
  public void plainText_singleTextChunk() {
    Line line = new Line("just plain text");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof TextChunk);
    assertEquals("just plain text", ((TextChunk) chunks[0]).getText());
  }

  @Test
  public void emptyString_noChunks() {
    Line line = new Line("");
    assertEquals(0, line.getChunks().length);
  }

  @Test
  public void indentedPlainText_indentStrippedFromChunks() {
    Line line = new Line("\tindented text");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertEquals("indented text", ((TextChunk) chunks[0]).getText());
  }

  // ---- property tags ----

  @Test
  public void propertyTag_createsPropertyChunk() {
    Line line = new Line("</myProp/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertEquals("myProp", ((PropertyChunk) chunks[0]).getPropertyName());
  }

  @Test
  public void propertyTagSurroundedByText_threeChunks() {
    Line line = new Line("before </prop/> after");
    Chunk[] chunks = line.getChunks();
    assertEquals(3, chunks.length);
    assertTrue(chunks[0] instanceof TextChunk);
    assertTrue(chunks[1] instanceof PropertyChunk);
    assertTrue(chunks[2] instanceof TextChunk);
    assertEquals("before ", ((TextChunk) chunks[0]).getText());
    assertEquals("prop", ((PropertyChunk) chunks[1]).getPropertyName());
    assertEquals(" after", ((TextChunk) chunks[2]).getText());
  }

  @Test
  public void multiplePropertyTags_allParsed() {
    Line line = new Line("</alpha/> and </beta/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(3, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertTrue(chunks[1] instanceof TextChunk);
    assertTrue(chunks[2] instanceof PropertyChunk);
  }

  // ---- method invocation tags ----

  @Test
  public void methodTag_createsMethodInvocationChunk() {
    Line line = new Line("</doSomething()/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof MethodInvocationChunk);
    assertEquals("doSomething", ((MethodInvocationChunk) chunks[0]).getMethodName());
  }

  @Test
  public void methodTagWithSurroundingText() {
    Line line = new Line("call </run()/> now");
    Chunk[] chunks = line.getChunks();
    assertEquals(3, chunks.length);
    assertTrue(chunks[1] instanceof MethodInvocationChunk);
    assertEquals("run", ((MethodInvocationChunk) chunks[1]).getMethodName());
  }

  // ---- gets chunks ----

  @Test
  public void getsTowardLeading_createsGetsChunk() {
    Line line = new Line("</_gets_toward_leading_/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof GetsChunk);
    assertTrue(((GetsChunk) chunks[0]).isTowardLeading());
  }

  @Test
  public void getsTowardTrailing_createsGetsChunkNotLeading() {
    Line line = new Line("</_gets_toward_trailing_/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof GetsChunk);
    assertFalse(((GetsChunk) chunks[0]).isTowardLeading());
  }

  // ---- isLoop ----

  @Test
  public void tabSpaceLoop_isLoopTrue() {
    Line line = new Line("\t loop");
    assertTrue(line.isLoop());
  }

  @Test
  public void regularText_isLoopFalse() {
    Line line = new Line("not a loop");
    assertFalse(line.isLoop());
  }

  @Test
  public void loopWithoutTab_isLoopFalse() {
    Line line = new Line("loop");
    assertFalse(line.isLoop());
  }

  // ---- mixed tags ----

  @Test
  public void mixedPropertyAndMethod_bothParsed() {
    Line line = new Line("</name/> calls </run()/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(3, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertTrue(chunks[1] instanceof TextChunk);
    assertTrue(chunks[2] instanceof MethodInvocationChunk);
  }

  @Test
  public void indentedWithTags_indentAndChunksBothCorrect() {
    Line line = new Line("\t\t</value/> text");
    assertEquals(2, line.getIndentCount());
    Chunk[] chunks = line.getChunks();
    assertEquals(2, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertTrue(chunks[1] instanceof TextChunk);
  }

  // ---- underscore property tags ----

  @Test
  public void singleUnderscoreProperty_parsedCorrectly() {
    Line line = new Line("</_wrapped_/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    PropertyChunk pc = (PropertyChunk) chunks[0];
    assertEquals("wrapped", pc.getPropertyName());
    assertEquals(1, pc.getUnderscoreCount());
  }

  @Test
  public void doubleUnderscoreProperty_parsedCorrectly() {
    Line line = new Line("</__bold__/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    PropertyChunk pc = (PropertyChunk) chunks[0];
    assertEquals("bold", pc.getPropertyName());
    assertEquals(2, pc.getUnderscoreCount());
  }
}
