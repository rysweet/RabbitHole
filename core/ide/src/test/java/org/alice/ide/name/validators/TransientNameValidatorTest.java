package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.Node;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserCode;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import static org.junit.Assert.*;

public class TransientNameValidatorTest {

  private static class TestTransientNameValidator extends TransientNameValidator {
    TestTransientNameValidator(Node node, UserCode code, BlockStatement block) {
      super(node, code, block);
    }
  }

  private static UserMethod createMethod() {
    UserMethod method = new UserMethod();
    method.name.setValue("testMethod");
    method.returnType.setValue(JavaType.getInstance(void.class));
    method.body.setValue(new BlockStatement());
    return method;
  }

  private static UserMethod createMethodWithParameters(String... parameterNames) {
    UserMethod method = createMethod();
    for (String name : parameterNames) {
      UserParameter parameter = new UserParameter();
      parameter.name.setValue(name);
      parameter.valueType.setValue(JavaType.getInstance(String.class));
      method.requiredParameters.add(parameter);
    }
    return method;
  }

  private static UserMethod createMethodWithLocals(String... localNames) {
    UserMethod method = createMethod();
    for (String name : localNames) {
      UserLocal local = new UserLocal();
      local.name.setValue(name);
      local.valueType.setValue(JavaType.getInstance(String.class));
      method.body.getValue().statements.add(new LocalDeclarationStatement(local, new StringLiteral("value")));
    }
    return method;
  }

  @Test
  public void isNameAvailable_noCode_true() {
    TestTransientNameValidator validator = new TestTransientNameValidator(null, null, null);

    assertTrue(validator.isNameAvailable("x"));
  }

  @Test
  public void isNameAvailable_codeWithParameter_sameName_false() {
    UserMethod method = createMethodWithParameters("x");
    TestTransientNameValidator validator = new TestTransientNameValidator(null, method, method.body.getValue());

    assertFalse(validator.isNameAvailable("x"));
  }

  @Test
  public void isNameAvailable_codeWithParameter_differentName_true() {
    UserMethod method = createMethodWithParameters("x");
    TestTransientNameValidator validator = new TestTransientNameValidator(null, method, method.body.getValue());

    assertTrue(validator.isNameAvailable("y"));
  }

  @Test
  public void isNameAvailable_codeWithLocal_sameName_false() {
    UserMethod method = createMethodWithLocals("temp");
    TestTransientNameValidator validator = new TestTransientNameValidator(null, method, method.body.getValue());

    assertFalse(validator.isNameAvailable("temp"));
  }

  @Test
  public void isNameAvailable_codeWithLocal_differentName_true() {
    UserMethod method = createMethodWithLocals("temp");
    TestTransientNameValidator validator = new TestTransientNameValidator(null, method, method.body.getValue());

    assertTrue(validator.isNameAvailable("otherTemp"));
  }
}
