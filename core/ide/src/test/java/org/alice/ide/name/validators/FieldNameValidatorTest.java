package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;

import static org.junit.Assert.*;

public class FieldNameValidatorTest {

  private static NamedUserType createTypeWithFields(String... fieldNames) {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestType");
    type.superType.setValue(JavaType.getInstance(Object.class));
    for (String name : fieldNames) {
      UserField field = new UserField();
      field.name.setValue(name);
      field.valueType.setValue(JavaType.getInstance(String.class));
      type.fields.add(field);
    }
    return type;
  }

  @Test
  public void constructor_withField_storesNodeAndType() {
    NamedUserType type = createTypeWithFields("alpha");
    UserField field = type.fields.get(0);

    FieldNameValidator validator = new FieldNameValidator(field);

    assertSame(field, validator.getNode());
    assertSame(type, validator.getType());
  }

  @Test
  public void constructor_withType_nodeIsNull() {
    NamedUserType type = createTypeWithFields("alpha");

    FieldNameValidator validator = new FieldNameValidator(type);

    assertNull(validator.getNode());
    assertSame(type, validator.getType());
  }

  @Test
  public void isNameAvailable_uniqueName_true() {
    NamedUserType type = createTypeWithFields("alpha");
    UserField field = type.fields.get(0);
    FieldNameValidator validator = new FieldNameValidator(field);

    assertTrue(validator.isNameAvailable("beta"));
  }

  @Test
  public void isNameAvailable_duplicateName_false() {
    NamedUserType type = createTypeWithFields("alpha", "beta");
    UserField field = type.fields.get(0);
    FieldNameValidator validator = new FieldNameValidator(field);

    assertFalse(validator.isNameAvailable("beta"));
  }

  @Test
  public void isNameAvailable_ownName_true() {
    NamedUserType type = createTypeWithFields("alpha");
    UserField field = type.fields.get(0);
    FieldNameValidator validator = new FieldNameValidator(field);

    assertTrue(validator.isNameAvailable("alpha"));
  }

  @Test
  public void isNameValid_validIdentifier_true() {
    FieldNameValidator validator = new FieldNameValidator(createTypeWithFields());

    assertTrue(validator.isNameValid("myField"));
  }

  @Test
  public void isNameValid_invalidIdentifier_false() {
    FieldNameValidator validator = new FieldNameValidator(createTypeWithFields());

    assertFalse(validator.isNameValid("123"));
  }
}
