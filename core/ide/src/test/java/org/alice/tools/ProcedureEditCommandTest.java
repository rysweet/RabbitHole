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

public class ProcedureEditCommandTest {
  @Test
  public void appendCommentCompletesAgainstSelectedProcedure() {
    UserMethod method = new UserMethod("eatmeFirstLesson", JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement());

    ProcedureEditCommand.Result result = ProcedureEditCommand.appendComment(
        "scene.eatmeFirstLesson",
        method,
        "eatmeFirstLesson",
        "command proof");

    assertEquals("scene.eatmeFirstLesson", result.procedureSelector());
    assertEquals("append-comment", result.command());
    assertEquals("eatmeFirstLesson", result.methodName());
    assertEquals("eatmeFirstLesson", result.selectedMethod());
    assertTrue(result.completed());
    assertEquals(0, result.beforeStatementCount());
    assertEquals(1, result.afterStatementCount());
    assertEquals(1, result.statementCountDelta());
    Statement statement = method.body.getValue().statements.get(0);
    assertTrue(statement instanceof Comment);
    assertEquals("command proof", ((Comment) statement).text.getValue());
  }

  @Test(expected = IllegalStateException.class)
  public void appendCommentRequiresSelectedProcedureToMatchTarget() {
    UserMethod method = new UserMethod("eatmeFirstLesson", JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement());

    ProcedureEditCommand.appendComment(
        "scene.eatmeFirstLesson",
        method,
        "otherProcedure",
        "command proof");
  }
}
