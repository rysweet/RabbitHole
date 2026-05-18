package org.alice.ide.ast.export.type;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceInfoExtendedTest {

  @Test
  public void constructorStoresValuesCorrectly() {
    ResourceInfo info = new ResourceInfo("org.example.resources.HeroResource", "DEFAULT");

    assertEquals("org.example.resources.HeroResource", info.getClassName());
    assertEquals("DEFAULT", info.getFieldName());
  }

  @Test
  public void gettersReturnExpectedValues() {
    ResourceInfo info = new ResourceInfo("org.example.resources.TreeResource", "OAK");

    assertEquals("org.example.resources.TreeResource", info.getClassName());
    assertEquals("OAK", info.getFieldName());
  }

  @Test
  public void nullValuesAreRetained() {
    ResourceInfo info = new ResourceInfo(null, null);

    assertNull(info.getClassName());
    assertNull(info.getFieldName());
  }

  @Test
  public void emptyStringsAreRetained() {
    ResourceInfo info = new ResourceInfo("", "");

    assertEquals("", info.getClassName());
    assertEquals("", info.getFieldName());
  }

  @Test
  public void multipleInstancesRetainTheirOwnValues() {
    ResourceInfo first = new ResourceInfo("org.example.resources.CarResource", "SPORT");
    ResourceInfo second = new ResourceInfo("org.example.resources.CarResource", "SPORT");
    ResourceInfo third = new ResourceInfo("org.example.resources.BikeResource", "CITY");

    assertNotSame(first, second);
    assertEquals(first.getClassName(), second.getClassName());
    assertEquals(first.getFieldName(), second.getFieldName());
    assertNotEquals(first.getClassName(), third.getClassName());
    assertNotEquals(first.getFieldName(), third.getFieldName());
  }
}
