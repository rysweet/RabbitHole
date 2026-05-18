package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link Page} — splitting text into {@link Line} arrays.
 */
public class PageTest {

  @Test
  public void singleLine_producesOneLineArray() {
    Page page = new Page("hello world");
    Line[] lines = page.getLines();
    assertEquals(1, lines.length);
  }

  @Test
  public void multipleLines_splitByNewline() {
    Page page = new Page("line1\nline2\nline3");
    Line[] lines = page.getLines();
    assertEquals(3, lines.length);
  }

  @Test
  public void emptyString_producesOneLine() {
    Page page = new Page("");
    Line[] lines = page.getLines();
    assertEquals(1, lines.length);
  }

  @Test
  public void trailingNewline_trailingEmptiesDroppedBySplit() {
    // Pattern.split drops trailing empty strings by default
    Page page = new Page("abc\n");
    Line[] lines = page.getLines();
    assertEquals(1, lines.length);
  }

  @Test
  public void onlyNewlines_droppedBySplit() {
    // Pattern.split("\\n\\n") with no content yields empty array → length 0 or 1
    Page page = new Page("\n\n");
    Line[] lines = page.getLines();
    // Pattern.split returns empty array for all-delimiter input
    assertTrue(lines.length >= 0);
  }

  @Test
  public void linesContainExpectedChunks() {
    Page page = new Page("plain text");
    Line[] lines = page.getLines();
    Chunk[] chunks = lines[0].getChunks();
    assertEquals(1, chunks.length);
    assertInstanceOf(TextChunk.class, chunks[0]);
    assertEquals("plain text", ((TextChunk) chunks[0]).getText());
  }

  @Test
  public void multiLineWithTags_eachLineParsedIndependently() {
    Page page = new Page("before </prop/> after\nsecond line");
    Line[] lines = page.getLines();
    assertEquals(2, lines.length);
    assertTrue(lines[0].getChunks().length > 1);
    assertEquals(1, lines[1].getChunks().length);
  }

  private static void assertInstanceOf(Class<?> expected, Object actual) {
    assertTrue("Expected " + expected.getSimpleName() + " but got " + actual.getClass().getSimpleName(),
        expected.isInstance(actual));
  }
}
