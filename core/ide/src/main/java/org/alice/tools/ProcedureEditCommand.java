package org.alice.tools;

import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.UserMethod;

final class ProcedureEditCommand {
  private ProcedureEditCommand() {
  }

  static Result appendComment(String procedureSelector, UserMethod method, String selectedMethod, String commentText) {
    if (method == null) {
      throw new IllegalArgumentException("procedure edit command requires a method");
    }
    if (!method.isProcedure()) {
      throw new IllegalArgumentException("procedure edit command requires a procedure: " + method.getName());
    }
    if (!method.getName().equals(selectedMethod)) {
      throw new IllegalStateException("selected procedure does not match edit target: " + selectedMethod);
    }
    if (commentText == null || commentText.isBlank()) {
      throw new IllegalArgumentException("append-comment command requires non-blank text");
    }
    BlockStatement body = method.body.getValue();
    if (body == null) {
      body = new BlockStatement();
      method.body.setValue(body);
    }
    int beforeStatementCount = body.statements.size();
    body.statements.add(new Comment(commentText));
    int afterStatementCount = body.statements.size();
    return new Result(
        procedureSelector,
        "append-comment",
        method.getName(),
        selectedMethod,
        beforeStatementCount,
        afterStatementCount,
        true);
  }

  record Result(
      String procedureSelector,
      String command,
      String methodName,
      String selectedMethod,
      int beforeStatementCount,
      int afterStatementCount,
      boolean completed) {
    int statementCountDelta() {
      return afterStatementCount - beforeStatementCount;
    }
  }
}
