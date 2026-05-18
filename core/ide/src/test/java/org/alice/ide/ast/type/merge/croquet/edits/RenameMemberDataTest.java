package org.alice.ide.ast.type.merge.croquet.edits;

import org.junit.Test;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link RenameMemberData}.
 */
public class RenameMemberDataTest {

  @Test
  public void constructor_storesMemberAndNextName() {
    UserMethod method = new UserMethod();
    method.name.setValue("oldName");
    RenameMemberData data = new RenameMemberData(method, "newName");
    assertSame(method, data.getMember());
    assertEquals("newName", data.getNextName());
  }

  @Test
  public void getPrevName_initiallyNull() {
    UserMethod method = new UserMethod();
    method.name.setValue("test");
    RenameMemberData data = new RenameMemberData(method, "renamed");
    assertNull(data.getPrevName());
  }

  @Test
  public void setPrevName_thenGetPrevName() {
    UserMethod method = new UserMethod();
    method.name.setValue("test");
    RenameMemberData data = new RenameMemberData(method, "renamed");
    data.setPrevName("original");
    assertEquals("original", data.getPrevName());
  }

  @Test
  public void setPrevName_canBeOverwritten() {
    UserMethod method = new UserMethod();
    method.name.setValue("test");
    RenameMemberData data = new RenameMemberData(method, "renamed");
    data.setPrevName("first");
    data.setPrevName("second");
    assertEquals("second", data.getPrevName());
  }

  @Test
  public void getNextName_isImmutable() {
    UserField field = new UserField();
    field.name.setValue("fieldName");
    RenameMemberData data = new RenameMemberData(field, "renamedField");
    assertEquals("renamedField", data.getNextName());
    assertEquals("renamedField", data.getNextName());
  }

  @Test
  public void getMember_withField() {
    UserField field = new UserField();
    field.name.setValue("myField");
    RenameMemberData data = new RenameMemberData(field, "newFieldName");
    assertSame(field, data.getMember());
  }

  @Test
  public void setPrevName_toNull() {
    UserMethod method = new UserMethod();
    method.name.setValue("test");
    RenameMemberData data = new RenameMemberData(method, "renamed");
    data.setPrevName("something");
    data.setPrevName(null);
    assertNull(data.getPrevName());
  }

  @Test
  public void nextName_canBeEmpty() {
    UserMethod method = new UserMethod();
    method.name.setValue("test");
    RenameMemberData data = new RenameMemberData(method, "");
    assertEquals("", data.getNextName());
  }

  @Test
  public void memberReference_isStable() {
    UserMethod method = new UserMethod();
    method.name.setValue("original");
    RenameMemberData data = new RenameMemberData(method, "newName");
    method.name.setValue("changed");
    assertSame(method, data.getMember());
    assertEquals("changed", data.getMember().getName());
  }
}
