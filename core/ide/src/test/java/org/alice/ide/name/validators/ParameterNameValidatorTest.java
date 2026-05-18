package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import static org.junit.Assert.*;

public class ParameterNameValidatorTest {

  private static UserMethod createMethodWithParameter(String parameterName) {
    UserMethod method = new UserMethod();
    method.name.setValue("testMethod");
    method.returnType.setValue(JavaType.getInstance(void.class));
    method.body.setValue(new BlockStatement());

    UserParameter parameter = new UserParameter();
    parameter.name.setValue(parameterName);
    parameter.valueType.setValue(JavaType.getInstance(String.class));
    method.requiredParameters.add(parameter);
    return method;
  }

  @Test
  public void constructor_withParameter_storesNode() {
    UserMethod method = createMethodWithParameter("alpha");
    UserParameter parameter = method.requiredParameters.get(0);

    ParameterNameValidator validator = new ParameterNameValidator(parameter);

    assertSame(parameter, validator.getNode());
  }

  @Test
  public void constructor_withCode_nodeIsNull() {
    UserMethod method = createMethodWithParameter("alpha");

    ParameterNameValidator validator = new ParameterNameValidator(method);

    assertNull(validator.getNode());
  }

  @Test
  public void isNameValid_validIdentifier_true() {
    ParameterNameValidator validator = new ParameterNameValidator(createMethodWithParameter("alpha"));

    assertTrue(validator.isNameValid("myParameter"));
  }

  @Test
  public void isNameValid_invalidIdentifier_false() {
    ParameterNameValidator validator = new ParameterNameValidator(createMethodWithParameter("alpha"));

    assertFalse(validator.isNameValid("123"));
  }
}
