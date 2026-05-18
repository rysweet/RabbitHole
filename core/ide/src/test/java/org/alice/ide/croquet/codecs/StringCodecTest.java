package org.alice.ide.croquet.codecs;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringCodecTest {

  @Test
  public void singleton_isNotNull() {
    assertNotNull(StringCodec.SINGLETON);
  }

  @Test
  public void getValueClass_returnsStringClass() {
    assertEquals(String.class, StringCodec.SINGLETON.getValueClass());
  }

  @Test
  public void appendRepresentation_appendsValue() {
    StringBuilder sb = new StringBuilder();
    StringCodec.SINGLETON.appendRepresentation(sb, "hello");
    assertEquals("hello", sb.toString());
  }

  @Test
  public void appendRepresentation_emptyString() {
    StringBuilder sb = new StringBuilder();
    StringCodec.SINGLETON.appendRepresentation(sb, "");
    assertEquals("", sb.toString());
  }

  @Test
  public void appendRepresentation_appendsToExisting() {
    StringBuilder sb = new StringBuilder("prefix:");
    StringCodec.SINGLETON.appendRepresentation(sb, "value");
    assertEquals("prefix:value", sb.toString());
  }

  @Test
  public void appendRepresentation_specialChars() {
    StringBuilder sb = new StringBuilder();
    StringCodec.SINGLETON.appendRepresentation(sb, "<html>&amp;</html>");
    assertEquals("<html>&amp;</html>", sb.toString());
  }

  @Test
  public void appendRepresentation_unicode() {
    StringBuilder sb = new StringBuilder();
    StringCodec.SINGLETON.appendRepresentation(sb, "\u00e9\u00e8\u00ea");
    assertEquals("\u00e9\u00e8\u00ea", sb.toString());
  }

  @Test
  public void appendRepresentation_multipleCallsAccumulate() {
    StringBuilder sb = new StringBuilder();
    StringCodec.SINGLETON.appendRepresentation(sb, "one");
    StringCodec.SINGLETON.appendRepresentation(sb, "two");
    assertEquals("onetwo", sb.toString());
  }

  @Test
  public void getValueClass_isConsistent() {
    assertSame(StringCodec.SINGLETON.getValueClass(), StringCodec.SINGLETON.getValueClass());
  }
}
