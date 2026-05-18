package org.alice.ide.ast.type.merge.croquet;

import org.junit.Test;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link ActionStatus} enum.
 */
public class ActionStatusTest {

  private UserMethod createProcedure(String name) {
    UserMethod method = new UserMethod();
    method.name.setValue(name);
    method.managementLevel.setValue(org.lgna.project.ast.ManagementLevel.NONE);
    return method;
  }

  private UserField createField(String name) {
    UserField field = new UserField();
    field.name.setValue(name);
    return field;
  }

  @Test
  public void values_has13Constants() {
    assertEquals(13, ActionStatus.values().length);
  }

  @Test
  public void valueOf_allConstants() {
    assertNotNull(ActionStatus.valueOf("OMIT"));
    assertNotNull(ActionStatus.valueOf("ADD_UNIQUE"));
    assertNotNull(ActionStatus.valueOf("ADD_AND_RENAME"));
    assertNotNull(ActionStatus.valueOf("REPLACE_OVER_ORIGINAL"));
    assertNotNull(ActionStatus.valueOf("OMIT_IN_FAVOR_OF_ORIGINAL"));
    assertNotNull(ActionStatus.valueOf("DELETE_IN_FAVOR_OF_REPLACEMENT"));
    assertNotNull(ActionStatus.valueOf("KEEP_OVER_DIFFERENT_SIGNATURE"));
    assertNotNull(ActionStatus.valueOf("KEEP_OVER_REPLACEMENT"));
    assertNotNull(ActionStatus.valueOf("KEEP_AND_RENAME"));
    assertNotNull(ActionStatus.valueOf("KEEP_IDENTICAL"));
    assertNotNull(ActionStatus.valueOf("KEEP_UNIQUE"));
    assertNotNull(ActionStatus.valueOf("RENAME_REQUIRED"));
    assertNotNull(ActionStatus.valueOf("SELECTION_REQUIRED"));
  }

  @Test
  public void omit_descriptionContainsName() {
    UserMethod m = createProcedure("walkMethod");
    String desc = ActionStatus.OMIT.getDescriptionText(m);
    assertTrue(desc.contains("walkMethod"));
    assertTrue(desc.contains("omitted"));
  }

  @Test
  public void addUnique_descriptionContainsAdded() {
    UserMethod m = createProcedure("jumpMethod");
    String desc = ActionStatus.ADD_UNIQUE.getDescriptionText(m);
    assertTrue(desc.contains("jumpMethod"));
    assertTrue(desc.contains("added"));
  }

  @Test
  public void addAndRename_descriptionContainsAddedAndRenamed() {
    UserMethod m = createProcedure("runMethod");
    String desc = ActionStatus.ADD_AND_RENAME.getDescriptionText(m);
    assertTrue(desc.contains("runMethod"));
    assertTrue(desc.contains("added and renamed"));
  }

  @Test
  public void replaceOverOriginal_descriptionContainsReplace() {
    UserMethod m = createProcedure("swim");
    String desc = ActionStatus.REPLACE_OVER_ORIGINAL.getDescriptionText(m);
    assertTrue(desc.contains("swim"));
    assertTrue(desc.contains("replace"));
  }

  @Test
  public void omitInFavorOfOriginal_descriptionContainsOmitted() {
    UserMethod m = createProcedure("fly");
    String desc = ActionStatus.OMIT_IN_FAVOR_OF_ORIGINAL.getDescriptionText(m);
    assertTrue(desc.contains("fly"));
    assertTrue(desc.contains("omitted in favor of"));
  }

  @Test
  public void deleteInFavorOfReplacement_descriptionContainsReplaced() {
    UserMethod m = createProcedure("crawl");
    String desc = ActionStatus.DELETE_IN_FAVOR_OF_REPLACEMENT.getDescriptionText(m);
    assertTrue(desc.contains("crawl"));
    assertTrue(desc.contains("replaced"));
  }

  @Test
  public void keepOverDifferentSignature_descriptionContainsRetained() {
    UserMethod m = createProcedure("climb");
    String desc = ActionStatus.KEEP_OVER_DIFFERENT_SIGNATURE.getDescriptionText(m);
    assertTrue(desc.contains("climb"));
    assertTrue(desc.contains("retained"));
  }

  @Test
  public void keepOverReplacement_descriptionContainsRetained() {
    UserMethod m = createProcedure("slide");
    String desc = ActionStatus.KEEP_OVER_REPLACEMENT.getDescriptionText(m);
    assertTrue(desc.contains("slide"));
    assertTrue(desc.contains("retained"));
  }

  @Test
  public void keepAndRename_descriptionContainsRetained() {
    UserMethod m = createProcedure("roll");
    String desc = ActionStatus.KEEP_AND_RENAME.getDescriptionText(m);
    assertTrue(desc.contains("roll"));
    assertTrue(desc.contains("retained"));
  }

  @Test
  public void keepIdentical_descriptionContainsRetained() {
    UserMethod m = createProcedure("bounce");
    String desc = ActionStatus.KEEP_IDENTICAL.getDescriptionText(m);
    assertTrue(desc.contains("bounce"));
    assertTrue(desc.contains("retained"));
  }

  @Test
  public void keepUnique_descriptionContainsRetained() {
    UserMethod m = createProcedure("spin");
    String desc = ActionStatus.KEEP_UNIQUE.getDescriptionText(m);
    assertTrue(desc.contains("spin"));
    assertTrue(desc.contains("retained"));
  }

  @Test
  public void renameRequired_descriptionContainsRename() {
    UserMethod m = createProcedure("twist");
    String desc = ActionStatus.RENAME_REQUIRED.getDescriptionText(m);
    assertTrue(desc.contains("twist"));
    assertTrue(desc.contains("rename"));
  }

  @Test
  public void selectionRequired_descriptionContainsSelect() {
    UserMethod m = createProcedure("dance");
    String desc = ActionStatus.SELECTION_REQUIRED.getDescriptionText(m);
    assertTrue(desc.contains("dance"));
    assertTrue(desc.contains("select"));
  }

  @Test
  public void allDescriptionsAreHtml() {
    UserMethod m = createProcedure("testMethod");
    for (ActionStatus status : ActionStatus.values()) {
      String desc = status.getDescriptionText(m);
      assertTrue("Description for " + status + " should start with <html>",
          desc.startsWith("<html>"));
    }
  }

  @Test
  public void allDescriptionsContainStrongTag() {
    UserMethod m = createProcedure("myMethod");
    for (ActionStatus status : ActionStatus.values()) {
      String desc = status.getDescriptionText(m);
      assertTrue("Description for " + status + " should contain <strong>",
          desc.contains("<strong>"));
      assertTrue("Description for " + status + " should contain </strong>",
          desc.contains("</strong>"));
    }
  }

  @Test
  public void descriptionWithField_containsFieldName() {
    UserField field = createField("myProperty");
    String desc = ActionStatus.OMIT.getDescriptionText(field);
    assertTrue(desc.contains("myProperty"));
  }

  @Test
  public void renameRequired_withField_containsProperties() {
    UserField field = createField("speed");
    String desc = ActionStatus.RENAME_REQUIRED.getDescriptionText(field);
    assertTrue(desc.contains("speed"));
    assertTrue(desc.contains("properties"));
  }

  @Test
  public void selectionRequired_descriptionContainsOr() {
    UserMethod m = createProcedure("action");
    String desc = ActionStatus.SELECTION_REQUIRED.getDescriptionText(m);
    assertTrue(desc.contains("or"));
  }
}
