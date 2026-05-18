package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;

import static org.junit.Assert.*;

public class LocalNameValidatorTest {

  private static UserLocal createLocalInMethod(String name) {
    UserMethod method = new UserMethod();
    method.name.setValue("testMethod");
    method.returnType.setValue(JavaType.getInstance(void.class));
    method.body.setValue(new BlockStatement());

    UserLocal local = new UserLocal();
    local.name.setValue(name);
    local.valueType.setValue(JavaType.getInstance(String.class));
    method.body.getValue().statements.add(new LocalDeclarationStatement(local, new StringLiteral("value")));
    return local;
  }

  @Test
  public void constructor_withLocal_storesNode() {
    UserLocal local = createLocalInMethod("alpha");

    LocalNameValidator validator = new LocalNameValidator(local);

    assertSame(local, validator.getNode());
  }

  @Test
  public void isNameValid_validIdentifier_true() {
    LocalNameValidator validator = new LocalNameValidator(createLocalInMethod("alpha"));

    assertTrue(validator.isNameValid("myLocal"));
  }
}
