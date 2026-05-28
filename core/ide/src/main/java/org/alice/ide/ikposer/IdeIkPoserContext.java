package org.alice.ide.ikposer;

import org.alice.ide.ProjectStack;
import org.alice.ide.ast.ExpressionCreator;
import org.alice.ide.croquet.edits.ast.ChangeMethodBodyEdit;
import org.alice.ide.croquet.edits.ast.DeclareMethodEdit;
import org.alice.ide.name.validators.MethodNameValidator;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.edits.AbstractEdit;
import org.lgna.croquet.history.UserActivity;
import org.lgna.ik.poser.CannotCreateExpressionException;
import org.lgna.ik.poser.IkPoserContext;
import org.lgna.project.Project;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserType;

public class IdeIkPoserContext implements IkPoserContext {
  @Override
  public Project getCurrentProject() {
    return ProjectStack.peekProject();
  }

  @Override
  public Expression createExpression(Object value) throws CannotCreateExpressionException {
    try {
      return new org.alice.stageide.ast.ExpressionCreator().createExpression(value);
    } catch (ExpressionCreator.CannotCreateExpressionException e) {
      throw new CannotCreateExpressionException(e.getValue(), e);
    }
  }

  @Override
  public String getMethodNameValidationError(UserType<?> type, String candidate) {
    return new MethodNameValidator(type).getExplanationIfOkButtonShouldBeDisabled(candidate);
  }

  @Override
  public AbstractEdit<?> createDeclareMethodEdit(UserActivity userActivity, UserType<?> declaringType, String methodName, AbstractType<?, ?, ?> returnType, BlockStatement body) {
    return new DeclareMethodEdit(userActivity, declaringType, methodName, returnType, body);
  }

  @Override
  public AbstractEdit<?> createChangeMethodBodyEdit(UserActivity userActivity, UserMethod method, BlockStatement body) {
    return new ChangeMethodBodyEdit(userActivity, method, body);
  }

  @Override
  public Triggerable createAddUnmanagedPoseFieldTrigger(NamedUserType declaringType, Expression initializer) {
    IdeAddUnmanagedPoseFieldComposite addUnmanagedPoseFieldComposite = IdeAddUnmanagedPoseFieldComposite.getInstance(declaringType);
    addUnmanagedPoseFieldComposite.setInitializerInitialValue(initializer);
    return addUnmanagedPoseFieldComposite.getLaunchOperation();
  }
}
