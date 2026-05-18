package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;

import static org.junit.Assert.*;

public class MarkerColorValidatorTest {

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
  public void constructor_withField_storesNode() {
    NamedUserType type = createTypeWithFields("alpha");
    UserField field = type.fields.get(0);

    MarkerColorValidator validator = new MarkerColorValidator(field);

    assertSame(field, validator.getNode());
    assertSame(type, validator.getType());
  }

  @Test
  public void constructor_withType_nodeIsNull() {
    NamedUserType type = createTypeWithFields("alpha");

    MarkerColorValidator validator = new MarkerColorValidator(type);

    assertNull(validator.getNode());
    assertSame(type, validator.getType());
  }

  @Test
  public void isNameAvailable_noFields_true() {
    MarkerColorValidator validator = new MarkerColorValidator(createTypeWithFields());

    assertTrue(validator.isNameAvailable("alpha"));
  }

  @Test
  public void isNameAvailable_nonMarkerField_sameName_false() {
    MarkerColorValidator validator = new MarkerColorValidator(createTypeWithFields("alpha"));

    assertFalse(validator.isNameAvailable("alpha"));
  }

  @Test
  public void isNameAvailable_nonMarkerField_differentName_true() {
    MarkerColorValidator validator = new MarkerColorValidator(createTypeWithFields("alpha"));

    assertTrue(validator.isNameAvailable("beta"));
  }

  @Test
  public void isNameValid_validIdentifier_true() {
    MarkerColorValidator validator = new MarkerColorValidator(createTypeWithFields());

    assertTrue(validator.isNameValid("markerColor"));
  }

  @Test
  public void isNameValid_invalidIdentifier_false() {
    MarkerColorValidator validator = new MarkerColorValidator(createTypeWithFields());

    assertFalse(validator.isNameValid("123"));
  }
}
