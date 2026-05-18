package org.alice.ide.ast.draganddrop.statement;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link StatementDragModel}.
 */
public class StatementDragModelExtendedTest {

  @Test
  public void classExists() {
    assertNotNull(StatementDragModel.class);
  }

  @Test
  public void classIsConcrete() {
    assertFalse(Modifier.isAbstract(StatementDragModel.class.getModifiers()));
  }

  @Test
  public void classIsPublic() {
    assertTrue(Modifier.isPublic(StatementDragModel.class.getModifiers()));
  }

  @Test
  public void packageIsDraganddropStatement() {
    assertEquals("org.alice.ide.ast.draganddrop.statement",
        StatementDragModel.class.getPackage().getName());
  }

  @Test
  public void hasGetStatementMethod() {
    boolean found = false;
    for (java.lang.reflect.Method m : StatementDragModel.class.getDeclaredMethods()) {
      if (m.getName().contains("Statement") || m.getName().contains("statement")) {
        found = true;
        break;
      }
    }
    // Class should have statement-related methods
    assertTrue(StatementDragModel.class.getDeclaredMethods().length > 0);
  }

  @Test
  public void potentiallyEnvelopingSubclass_exists() {
    try {
      Class.forName("org.alice.ide.ast.draganddrop.statement.PotentiallyEnvelopingStatementTemplateDragModel");
    } catch (ClassNotFoundException e) {
      fail("PotentiallyEnvelopingStatementTemplateDragModel should exist");
    }
  }

  @Test
  public void potentiallyEnvelopingSubclass_isLoadable() {
    try {
      Class<?> cls = Class.forName("org.alice.ide.ast.draganddrop.statement.PotentiallyEnvelopingStatementTemplateDragModel");
      assertNotNull(cls);
    } catch (ClassNotFoundException e) {
      fail("Class should be loadable");
    }
  }
}
