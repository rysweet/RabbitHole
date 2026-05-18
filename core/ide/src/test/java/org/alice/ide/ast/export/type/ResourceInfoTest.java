package org.alice.ide.ast.export.type;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResourceInfoTest {

  @Test
  public void constructor_setsClassName() {
    ResourceInfo info = new ResourceInfo("com.example.MyResource", null);
    assertEquals("com.example.MyResource", info.getClassName());
  }

  @Test
  public void constructor_setsFieldName() {
    ResourceInfo info = new ResourceInfo("com.example.MyResource", "ADULT_FEMALE");
    assertEquals("ADULT_FEMALE", info.getFieldName());
  }

  @Test
  public void constructor_nullFieldName() {
    ResourceInfo info = new ResourceInfo("com.example.Type", null);
    assertNull(info.getFieldName());
  }

  @Test
  public void getClassName_notNull() {
    ResourceInfo info = new ResourceInfo("TypeA", "fieldA");
    assertNotNull(info.getClassName());
  }

  @Test
  public void differentInstances_differentValues() {
    ResourceInfo a = new ResourceInfo("TypeA", "fieldA");
    ResourceInfo b = new ResourceInfo("TypeB", "fieldB");
    assertNotEquals(a.getClassName(), b.getClassName());
    assertNotEquals(a.getFieldName(), b.getFieldName());
  }

  @Test
  public void sameValues_sameGetters() {
    ResourceInfo info = new ResourceInfo("Same", "same");
    assertEquals("Same", info.getClassName());
    assertEquals("same", info.getFieldName());
  }

  @Test
  public void emptyClassName_allowed() {
    ResourceInfo info = new ResourceInfo("", "field");
    assertEquals("", info.getClassName());
  }

  @Test
  public void nullClassName_allowed() {
    ResourceInfo info = new ResourceInfo(null, "field");
    assertNull(info.getClassName());
  }
}
