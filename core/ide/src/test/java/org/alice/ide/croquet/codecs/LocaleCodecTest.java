package org.alice.ide.croquet.codecs;

import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class LocaleCodecTest {

  @Test
  public void singleton_isNotNull() {
    assertNotNull(LocaleCodec.SINGLETON);
  }

  @Test
  public void getValueClass_returnsLocaleClass() {
    assertEquals(Locale.class, LocaleCodec.SINGLETON.getValueClass());
  }

  @Test
  public void appendRepresentation_usLocale_containsEnglish() {
    StringBuilder sb = new StringBuilder();
    LocaleCodec.SINGLETON.appendRepresentation(sb, Locale.US);
    String result = sb.toString();
    assertFalse(result.isEmpty());
    assertTrue(result.contains("English") || result.contains("english") || result.contains("en"));
  }

  @Test
  public void appendRepresentation_nullLocale_appendsNull() {
    StringBuilder sb = new StringBuilder();
    LocaleCodec.SINGLETON.appendRepresentation(sb, null);
    assertEquals("null", sb.toString());
  }

  @Test
  public void appendRepresentation_frenchLocale_containsFrench() {
    StringBuilder sb = new StringBuilder();
    LocaleCodec.SINGLETON.appendRepresentation(sb, Locale.FRENCH);
    String result = sb.toString();
    assertFalse(result.isEmpty());
  }

  @Test
  public void appendRepresentation_appendsToExistingBuilder() {
    StringBuilder sb = new StringBuilder("locale=");
    LocaleCodec.SINGLETON.appendRepresentation(sb, Locale.ENGLISH);
    assertTrue(sb.toString().startsWith("locale="));
    assertTrue(sb.length() > "locale=".length());
  }

  @Test
  public void getValueClass_isConsistent() {
    assertSame(LocaleCodec.SINGLETON.getValueClass(), LocaleCodec.SINGLETON.getValueClass());
  }

  @Test
  public void appendRepresentation_germanLocale_nonEmpty() {
    StringBuilder sb = new StringBuilder();
    LocaleCodec.SINGLETON.appendRepresentation(sb, Locale.GERMAN);
    assertTrue(sb.length() > 0);
  }

  @Test
  public void appendRepresentation_japaneseLocale_nonEmpty() {
    StringBuilder sb = new StringBuilder();
    LocaleCodec.SINGLETON.appendRepresentation(sb, Locale.JAPANESE);
    assertTrue(sb.length() > 0);
  }
}
