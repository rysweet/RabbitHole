package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;

import static org.junit.Assert.*;

public class MethodNameValidatorTest {

  private static NamedUserType createTypeWithMethods(String... methodNames) {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestType");
    type.superType.setValue(JavaType.getInstance(Object.class));
    for (String name : methodNames) {
      UserMethod method = new UserMethod();
      method.name.setValue(name);
      method.returnType.setValue(JavaType.getInstance(void.class));
      method.body.setValue(new BlockStatement());
      type.methods.add(method);
    }
    return type;
  }

  @Test
  public void constructor_withMethod_storesNodeAndType() {
    NamedUserType type = createTypeWithMethods("alpha");
    UserMethod method = type.methods.get(0);

    MethodNameValidator validator = new MethodNameValidator(method);

    assertSame(method, validator.getNode());
    assertSame(type, validator.getType());
  }

  @Test
  public void constructor_withType_nodeIsNull() {
    NamedUserType type = createTypeWithMethods("alpha");

    MethodNameValidator validator = new MethodNameValidator(type);

    assertNull(validator.getNode());
    assertSame(type, validator.getType());
  }

  @Test
  public void isNameAvailable_uniqueName_true() {
    NamedUserType type = createTypeWithMethods("alpha");
    UserMethod method = type.methods.get(0);
    MethodNameValidator validator = new MethodNameValidator(method);

    assertTrue(validator.isNameAvailable("beta"));
  }

  @Test
  public void isNameAvailable_duplicateName_false() {
    NamedUserType type = createTypeWithMethods("alpha", "beta");
    UserMethod method = type.methods.get(0);
    MethodNameValidator validator = new MethodNameValidator(method);

    assertFalse(validator.isNameAvailable("beta"));
  }

  @Test
  public void isNameAvailable_ownName_true() {
    NamedUserType type = createTypeWithMethods("alpha");
    UserMethod method = type.methods.get(0);
    MethodNameValidator validator = new MethodNameValidator(method);

    assertTrue(validator.isNameAvailable("alpha"));
  }

  @Test
  public void isNameValid_validIdentifier_true() {
    MethodNameValidator validator = new MethodNameValidator(createTypeWithMethods());

    assertTrue(validator.isNameValid("myMethod"));
  }

  @Test
  public void isNameValid_invalidIdentifier_false() {
    MethodNameValidator validator = new MethodNameValidator(createTypeWithMethods());

    assertFalse(validator.isNameValid("123"));
  }
}
