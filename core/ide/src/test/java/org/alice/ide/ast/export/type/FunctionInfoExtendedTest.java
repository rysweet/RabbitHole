package org.alice.ide.ast.export.type;

import org.junit.Test;

import static org.junit.Assert.*;

public class FunctionInfoExtendedTest {

  @Test
  public void constructorStoresValuesCorrectly() {
    FunctionInfo info = new FunctionInfo("java.lang.String", "getLabel");

    assertEquals("java.lang.String", info.getReturnClassName());
    assertEquals("getLabel", info.getName());
  }

  @Test
  public void gettersReturnExpectedValues() {
    FunctionInfo info = new FunctionInfo("java.lang.Boolean", "isReady");

    assertEquals("java.lang.Boolean", info.getReturnClassName());
    assertEquals("isReady", info.getName());
  }

  @Test
  public void nullValuesAreRetained() {
    FunctionInfo info = new FunctionInfo(null, null);

    assertNull(info.getReturnClassName());
    assertNull(info.getName());
  }

  @Test
  public void emptyStringsAreRetained() {
    FunctionInfo info = new FunctionInfo("", "");

    assertEquals("", info.getReturnClassName());
    assertEquals("", info.getName());
  }

  @Test
  public void multipleInstancesWithSameValuesRemainIndependent() {
    FunctionInfo first = new FunctionInfo("java.lang.Integer", "getCount");
    FunctionInfo second = new FunctionInfo("java.lang.Integer", "getCount");

    assertNotSame(first, second);
    assertEquals(first.getReturnClassName(), second.getReturnClassName());
    assertEquals(first.getName(), second.getName());
  }

  @Test
  public void multipleInstancesWithDifferentValuesExposeThoseDifferences() {
    FunctionInfo first = new FunctionInfo("java.lang.Integer", "getCount");
    FunctionInfo second = new FunctionInfo("java.lang.Double", "getRatio");

    assertNotEquals(first.getReturnClassName(), second.getReturnClassName());
    assertNotEquals(first.getName(), second.getName());
  }

  @Test
  public void longStringValuesAreRetained() {
    String returnClassName = "org.example.api." + "ReturnType.".repeat(10) + "Final";
    String name = "get" + "Segment".repeat(20);
    FunctionInfo info = new FunctionInfo(returnClassName, name);

    assertEquals(returnClassName, info.getReturnClassName());
    assertEquals(name, info.getName());
  }
}
