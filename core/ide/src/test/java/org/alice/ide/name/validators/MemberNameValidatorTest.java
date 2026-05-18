package org.alice.ide.name.validators;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Node;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserType;

import static org.junit.Assert.*;

public class MemberNameValidatorTest {

  private static NamedUserType createType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(Object.class));
    return type;
  }

  private static class TestMemberNameValidator extends MemberNameValidator {
    TestMemberNameValidator(Node node, UserType<?> type) {
      super(node, type);
    }

    @Override
    public boolean isNameAvailable(String name) {
      return true;
    }
  }

  @Test
  public void getType_returnsConstructedType() {
    NamedUserType type = createType("TypeA");

    TestMemberNameValidator validator = new TestMemberNameValidator(new UserField(), type);

    assertSame(type, validator.getType());
  }

  @Test
  public void setType_changesType() {
    NamedUserType originalType = createType("TypeA");
    NamedUserType updatedType = createType("TypeB");
    TestMemberNameValidator validator = new TestMemberNameValidator(new UserField(), originalType);

    validator.setType(updatedType);

    assertSame(updatedType, validator.getType());
  }

  @Test
  public void getNode_returnsConstructedNode() {
    UserField field = new UserField();
    TestMemberNameValidator validator = new TestMemberNameValidator(field, createType("TypeA"));

    assertSame(field, validator.getNode());
  }

  @Test
  public void isNameValid_delegatesToParent() {
    TestMemberNameValidator validator = new TestMemberNameValidator(new UserField(), createType("TypeA"));

    assertTrue(validator.isNameValid("memberName"));
    assertFalse(validator.isNameValid("123"));
  }
}
