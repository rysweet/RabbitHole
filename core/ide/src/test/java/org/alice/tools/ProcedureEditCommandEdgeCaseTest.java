package org.alice.tools;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Edge-case contract tests for ProcedureEditCommand, complementing
 * the happy-path and mismatch tests in ProcedureEditCommandTest.
 */
public class ProcedureEditCommandEdgeCaseTest {

  @Test(expected = IllegalArgumentException.class)
  public void appendCommentRejectsNullMethod() {
    ProcedureEditCommand.appendComment("scene.eatmeFirstLesson", null, "eatmeFirstLesson", "proof");
  }

  @Test(expected = IllegalArgumentException.class)
  public void appendCommentRejectsNullCommentText() {
    UserMethod method = procedure("eatmeFirstLesson");
    ProcedureEditCommand.appendComment("scene.eatmeFirstLesson", method, "eatmeFirstLesson", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void appendCommentRejectsBlankCommentText() {
    UserMethod method = procedure("eatmeFirstLesson");
    ProcedureEditCommand.appendComment("scene.eatmeFirstLesson", method, "eatmeFirstLesson", "   ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void appendCommentRejectsFunction() {
    UserMethod function = new UserMethod(
        "computeAnswer",
        JavaType.INTEGER_PRIMITIVE_TYPE,
        new UserParameter[0],
        new BlockStatement());
    ProcedureEditCommand.appendComment("scene.computeAnswer", function, "computeAnswer", "proof");
  }

  @Test
  public void appendCommentInitializesNullBodyBeforeAppending() {
    UserMethod method = new UserMethod(
        "eatmeFirstLesson",
        JavaType.VOID_TYPE,
        new UserParameter[0],
        null);

    ProcedureEditCommand.Result result = ProcedureEditCommand.appendComment(
        "scene.eatmeFirstLesson", method, "eatmeFirstLesson", "null-body proof");

    assertEquals(0, result.beforeStatementCount());
    assertEquals(1, result.afterStatementCount());
    assertEquals(1, result.statementCountDelta());
    assertTrue(result.completed());
    Statement statement = method.body.getValue().statements.get(0);
    assertTrue(statement instanceof Comment);
    assertEquals("null-body proof", ((Comment) statement).text.getValue());
  }

  @Test
  public void appendCommentPreservesExistingStatements() {
    UserMethod method = procedure("eatmeFirstLesson");
    method.body.getValue().statements.add(new Comment("existing-1"));
    method.body.getValue().statements.add(new Comment("existing-2"));

    ProcedureEditCommand.Result result = ProcedureEditCommand.appendComment(
        "scene.eatmeFirstLesson", method, "eatmeFirstLesson", "new proof");

    assertEquals(2, result.beforeStatementCount());
    assertEquals(3, result.afterStatementCount());
    assertEquals(1, result.statementCountDelta());
    assertEquals("existing-1", ((Comment) method.body.getValue().statements.get(0)).text.getValue());
    assertEquals("existing-2", ((Comment) method.body.getValue().statements.get(1)).text.getValue());
    assertEquals("new proof", ((Comment) method.body.getValue().statements.get(2)).text.getValue());
  }

  @Test
  public void resultRecordsProcedureSelectorAndCommand() {
    UserMethod method = procedure("eatmeFirstLesson");

    ProcedureEditCommand.Result result = ProcedureEditCommand.appendComment(
        "scene.eatmeFirstLesson", method, "eatmeFirstLesson", "selector proof");

    assertEquals("scene.eatmeFirstLesson", result.procedureSelector());
    assertEquals("append-comment", result.command());
    assertEquals("eatmeFirstLesson", result.methodName());
    assertEquals("eatmeFirstLesson", result.selectedMethod());
  }

  private static UserMethod procedure(String name) {
    return new UserMethod(name, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement());
  }
}
