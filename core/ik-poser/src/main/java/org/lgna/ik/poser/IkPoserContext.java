package org.lgna.ik.poser;

import org.lgna.croquet.Triggerable;
import org.lgna.croquet.edits.AbstractEdit;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.Project;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserType;

public interface IkPoserContext {
  Project getCurrentProject();

  Expression createExpression(Object value) throws CannotCreateExpressionException;

  String getMethodNameValidationError(UserType<?> type, String candidate);

  AbstractEdit<?> createDeclareMethodEdit(UserActivity userActivity, UserType<?> declaringType, String methodName, AbstractType<?, ?, ?> returnType, BlockStatement body);

  AbstractEdit<?> createChangeMethodBodyEdit(UserActivity userActivity, UserMethod method, BlockStatement body);

  Triggerable createAddUnmanagedPoseFieldTrigger(NamedUserType declaringType, Expression initializer);
}
