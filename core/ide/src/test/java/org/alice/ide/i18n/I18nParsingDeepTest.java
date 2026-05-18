package org.alice.ide.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Deep edge-case tests for the i18n parsing pipeline — {@link Page}, {@link Line}, and chunk types.
 */
public class I18nParsingDeepTest {

  // ---- Line: edge cases with tag patterns ----

  @Test
  public void adjacentTags_noTextBetween() {
    Line line = new Line("</alpha/></beta/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(2, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertTrue(chunks[1] instanceof PropertyChunk);
    assertEquals("alpha", ((PropertyChunk) chunks[0]).getPropertyName());
    assertEquals("beta", ((PropertyChunk) chunks[1]).getPropertyName());
  }

  @Test
  public void tagAtStart_textAfter() {
    Line line = new Line("</name/> says hello");
    Chunk[] chunks = line.getChunks();
    assertEquals(2, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertTrue(chunks[1] instanceof TextChunk);
  }

  @Test
  public void tagAtEnd_textBefore() {
    Line line = new Line("hello </world/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(2, chunks.length);
    assertTrue(chunks[0] instanceof TextChunk);
    assertTrue(chunks[1] instanceof PropertyChunk);
  }

  @Test
  public void numericPropertyName_parsed() {
    Line line = new Line("</prop123/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertEquals("prop123", ((PropertyChunk) chunks[0]).getPropertyName());
  }

  @Test
  public void underscoreInPropertyName_parsedInTag() {
    Line line = new Line("</my_prop/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof PropertyChunk);
    assertEquals("my_prop", ((PropertyChunk) chunks[0]).getPropertyName());
  }

  // ---- Line: method invocation tag variations ----

  @Test
  public void methodWithUnderscores_parsedCorrectly() {
    Line line = new Line("</do_something()/>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof MethodInvocationChunk);
    assertEquals("do_something", ((MethodInvocationChunk) chunks[0]).getMethodName());
  }

  // ---- Line: gets chunks in context ----

  @Test
  public void getsTowardLeading_inContextWithText() {
    Line line = new Line("x </_gets_toward_leading_/> y");
    Chunk[] chunks = line.getChunks();
    assertEquals(3, chunks.length);
    assertTrue(chunks[0] instanceof TextChunk);
    assertTrue(chunks[1] instanceof GetsChunk);
    assertTrue(chunks[2] instanceof TextChunk);
    assertTrue(((GetsChunk) chunks[1]).isTowardLeading());
  }

  @Test
  public void getsTowardTrailing_inContextWithText() {
    Line line = new Line("x </_gets_toward_other_/> y");
    Chunk[] chunks = line.getChunks();
    assertEquals(3, chunks.length);
    assertTrue(chunks[1] instanceof GetsChunk);
    assertFalse(((GetsChunk) chunks[1]).isTowardLeading());
  }

  // ---- Page: complex multi-line with mixed tags ----

  @Test
  public void complexMultiLine_allLinesCorrect() {
    String text = "set </name/> to </getValue()/>\n\t</condition/> do\n\t\trun </execute()/>";
    Page page = new Page(text);
    Line[] lines = page.getLines();
    assertEquals(3, lines.length);

    // Line 0: "set </name/> to </getValue()/>"
    assertEquals(0, lines[0].getIndentCount());
    Chunk[] c0 = lines[0].getChunks();
    assertEquals(4, c0.length);

    // Line 1: "\t</condition/> do"
    assertEquals(1, lines[1].getIndentCount());
    Chunk[] c1 = lines[1].getChunks();
    assertEquals(2, c1.length);

    // Line 2: "\t\trun </execute()/>"
    assertEquals(2, lines[2].getIndentCount());
    Chunk[] c2 = lines[2].getChunks();
    assertEquals(2, c2.length);
    assertTrue(c2[1] instanceof MethodInvocationChunk);
  }

  // ---- PropertyChunk: underscore edge cases ----

  @Test(expected = StringIndexOutOfBoundsException.class)
  public void emptyBetweenSingleUnderscores_throwsOnSubstring() {
    // "__" starts with _ and ends with _ => 1 underscore count
    // But substring(1, length-1) => substring(1, 1) = ""... actually substring(1, 2-1=1) works.
    // Wait, the input is "__" which is length 2, and starts with "_" and ends with "_"
    // underscoreCount = 1, then substring(1, 2-1) = substring(1,1) = ""
    // But actually the code does: substring(underscoreCount, length - underscoreCount)
    // = substring(1, 2 - 1) = substring(1, 1) = ""
    // Hmm, but for "__" with double underscore check: starts with "__" (yes) and ends with "__" (yes)
    // => underscoreCount = 2, then substring(2, 2 - 2) = substring(2, 0) which throws
    new PropertyChunk("__");
  }

  @Test
  public void tripleUnderscoreEachSide_onlyDoubleStripped() {
    // "___x___" starts with __ and ends with __ => underscore count 2
    // substring(2, length-2) = "_x_"
    PropertyChunk pc = new PropertyChunk("___x___");
    assertEquals(2, pc.getUnderscoreCount());
    assertEquals("_x_", pc.getPropertyName());
  }

  // ---- TextChunk: special characters ----

  @Test
  public void textWithHtmlEntities_preservedAsIs() {
    TextChunk tc = new TextChunk("&lt;hello&gt;");
    assertEquals("&lt;hello&gt;", tc.getText());
  }

  @Test
  public void textWithUnicode_preservedAsIs() {
    TextChunk tc = new TextChunk("日本語テスト");
    assertEquals("日本語テスト", tc.getText());
  }

  // ---- Line: no valid tags ----

  @Test
  public void incompleteTags_treatedAsPlainText() {
    // Missing closing />, so not a valid tag
    Line line = new Line("</notClosed");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof TextChunk);
    assertEquals("</notClosed", ((TextChunk) chunks[0]).getText());
  }

  @Test
  public void regularHtmlTag_notMatchedAsAliceTag() {
    // <div> is not a </tag/> pattern
    Line line = new Line("<div>content</div>");
    Chunk[] chunks = line.getChunks();
    assertEquals(1, chunks.length);
    assertTrue(chunks[0] instanceof TextChunk);
  }
}
