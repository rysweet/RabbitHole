package org.alice.ide.identifier;

import org.junit.Test;
import static org.junit.Assert.*;

public class IdentifierNameGeneratorExtendedTest {

  @Test
  public void convertConstant_allCaps_singleWord() {
    assertEquals("move", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("MOVE"));
  }

  @Test
  public void convertConstant_preservesDigits() {
    assertEquals("point3d", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("POINT_3D"));
  }

  @Test
  public void convertConstant_trailingUnderscore() {
    String result = IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("HELLO_");
    assertEquals("hello", result);
  }

  @Test
  public void convertConstant_onlyUnderscores() {
    assertEquals("", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("___"));
  }

  @Test
  public void convertConstant_mixedCase() {
    assertEquals("helloWorld", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("Hello_World"));
  }

  @Test
  public void convertConstant_withSetPrefix() {
    assertEquals("setColor", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("COLOR", "set"));
  }

  @Test
  public void convertConstant_withGetPrefix() {
    assertEquals("getValue", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("VALUE", "get"));
  }

  @Test
  public void convertConstant_withIsPrefix() {
    assertEquals("isReady", IdentifierNameGenerator.SINGLETON.convertConstantNameToMethodName("READY", "is"));
  }

  @Test
  public void createFromClassName_preservesMultiCase() {
    assertEquals("myClassName", IdentifierNameGenerator.SINGLETON.createIdentifierNameFromClassName("MyClassName"));
  }

  @Test
  public void createFromClassName_singleUpperChar() {
    assertEquals("a", IdentifierNameGenerator.SINGLETON.createIdentifierNameFromClassName("A"));
  }

  @Test
  public void createFromClassName_allUpperCase() {
    assertEquals("aBCDEF", IdentifierNameGenerator.SINGLETON.createIdentifierNameFromClassName("ABCDEF"));
  }

  @Test
  public void createFromClassName_preservesNumbers() {
    assertEquals("class123", IdentifierNameGenerator.SINGLETON.createIdentifierNameFromClassName("Class123"));
  }

  @Test
  public void createFromInstanceCreation_null_returnsEmptyString() {
    String result = IdentifierNameGenerator.SINGLETON.createIdentifierNameFromInstanceCreation(null);
    assertEquals("", result);
  }

  @Test
  public void singleton_isSameReference() {
    assertSame(IdentifierNameGenerator.SINGLETON, IdentifierNameGenerator.SINGLETON);
  }
}
