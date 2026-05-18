package org.alice.ide.ast.export.type;

import org.junit.Test;

import static org.junit.Assert.*;

public class FieldInfoExtendedTest {

  @Test
  public void constructorStoresValuesCorrectly() {
    FieldInfo info = new FieldInfo("java.lang.String", "displayName");

    assertEquals("java.lang.String", info.getValueClassName());
    assertEquals("displayName", info.getName());
  }

  @Test
  public void gettersReturnExpectedValues() {
    FieldInfo info = new FieldInfo("java.lang.Integer", "score");

    assertEquals("java.lang.Integer", info.getValueClassName());
    assertEquals("score", info.getName());
  }

  @Test
  public void nullValuesAreRetained() {
    FieldInfo info = new FieldInfo(null, null);

    assertNull(info.getValueClassName());
    assertNull(info.getName());
  }

  @Test
  public void emptyStringsAreRetained() {
    FieldInfo info = new FieldInfo("", "");

    assertEquals("", info.getValueClassName());
    assertEquals("", info.getName());
  }

  @Test
  public void multipleInstancesWithSameValuesRemainIndependent() {
    FieldInfo first = new FieldInfo("java.lang.Double", "amount");
    FieldInfo second = new FieldInfo("java.lang.Double", "amount");

    assertNotSame(first, second);
    assertEquals(first.getValueClassName(), second.getValueClassName());
    assertEquals(first.getName(), second.getName());
  }

  @Test
  public void multipleInstancesWithDifferentValuesExposeThoseDifferences() {
    FieldInfo first = new FieldInfo("java.lang.Double", "amount");
    FieldInfo second = new FieldInfo("java.lang.Boolean", "enabled");

    assertNotEquals(first.getValueClassName(), second.getValueClassName());
    assertNotEquals(first.getName(), second.getName());
  }

  @Test
  public void longStringValuesAreRetained() {
    String valueClassName = "org.example.longpackage." + "segment.".repeat(12) + "ValueType";
    String name = "field" + "Name".repeat(25);
    FieldInfo info = new FieldInfo(valueClassName, name);

    assertEquals(valueClassName, info.getValueClassName());
    assertEquals(name, info.getName());
  }
}
