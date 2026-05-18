package org.alice.ide.ast.type.merge.croquet;

import org.junit.Test;
import org.lgna.project.ast.*;

import static org.junit.Assert.*;

public class ActionStatusExtendedTest {

  private UserMethod createProcedure(String name) {
    UserMethod m = new UserMethod();
    m.name.setValue(name);
    m.returnType.setValue(JavaType.VOID_TYPE);
    m.managementLevel.setValue(ManagementLevel.NONE);
    return m;
  }

  private UserField createField(String name) {
    UserField f = new UserField();
    f.name.setValue(name);
    f.valueType.setValue(JavaType.DOUBLE_OBJECT_TYPE);
    f.managementLevel.setValue(ManagementLevel.NONE);
    return f;
  }

  @Test
  public void allStatuses_descriptionStartsWithHtml() {
    UserMethod m = createProcedure("test");
    for (ActionStatus status : ActionStatus.values()) {
      String desc = status.getDescriptionText(m);
      assertTrue(status.name() + " description should start with <html>", desc.startsWith("<html>"));
    }
  }

  @Test
  public void allStatuses_descriptionContainsStrongTag() {
    UserMethod m = createProcedure("highlight");
    for (ActionStatus status : ActionStatus.values()) {
      String desc = status.getDescriptionText(m);
      assertTrue(status.name() + " missing <strong>", desc.contains("<strong>"));
      assertTrue(status.name() + " missing </strong>", desc.contains("</strong>"));
    }
  }

  @Test
  public void omit_withField_containsOmitted() {
    UserField f = createField("myProp");
    String desc = ActionStatus.OMIT.getDescriptionText(f);
    assertTrue(desc.contains("omitted"));
  }

  @Test
  public void selectionRequired_containsOrKeyword() {
    UserMethod m = createProcedure("choose");
    String desc = ActionStatus.SELECTION_REQUIRED.getDescriptionText(m);
    assertTrue(desc.contains("or"));
  }

  @Test
  public void addUnique_containsAdded() {
    UserMethod m = createProcedure("newMethod");
    String desc = ActionStatus.ADD_UNIQUE.getDescriptionText(m);
    assertTrue(desc.contains("added"));
  }

  @Test
  public void specialCharactersInName_handled() {
    UserMethod m = createProcedure("method<With>Special");
    String desc = ActionStatus.OMIT.getDescriptionText(m);
    assertTrue(desc.contains("method<With>Special"));
  }

  @Test
  public void emptyName_handledGracefully() {
    UserMethod m = createProcedure("");
    for (ActionStatus status : ActionStatus.values()) {
      String desc = status.getDescriptionText(m);
      assertNotNull(desc);
      assertFalse(desc.isEmpty());
    }
  }

  @Test
  public void longName_handledGracefully() {
    String longName = "a".repeat(500);
    UserMethod m = createProcedure(longName);
    String desc = ActionStatus.ADD_UNIQUE.getDescriptionText(m);
    assertTrue(desc.contains(longName));
  }
}
