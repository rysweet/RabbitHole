package org.alice.ide.croquet.edits.ast;

import org.junit.Test;
import org.lgna.croquet.CompletionModel;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.ExpressionProperty;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserMethod;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class StatementEditUtilitiesExtendedTest {

  @Test
  public void statementEditBaseClassStoresStatement() {
    Comment statement = new Comment("hello");

    TestStatementEdit edit = new TestStatementEdit(statement);

    assertSame(statement, edit.getStatement());
  }

  @Test
  public void statementEditSubclassCanRunItsInternalHooks() {
    TestStatementEdit edit = new TestStatementEdit(new Comment("hook test"));

    edit.doOrRedoInternal(true);
    edit.undoInternal();

    assertTrue(edit.didDoOrRedo);
    assertTrue(edit.didUndo);
  }

  @Test
  public void expressionPropertyEditAppliesAndRestoresValues() {
    ExpressionStatement statement = new ExpressionStatement();
    ExpressionProperty property = statement.expression;
    IntegerLiteral previous = new IntegerLiteral(1);
    IntegerLiteral next = new IntegerLiteral(2);
    property.setValue(previous);

    ExpressionPropertyEdit edit = new ExpressionPropertyEdit(null, property, previous, next);
    edit.doOrRedoInternal(true);
    assertSame(next, property.getValue());

    edit.undoInternal();
    assertSame(previous, property.getValue());
  }

  @Test
  public void declareMethodEditStoresMetadataAndBody() throws Exception {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestType");
    type.superType.setValue(JavaType.getInstance(Object.class));
    BlockStatement body = new BlockStatement();

    DeclareMethodEdit edit = new DeclareMethodEdit(null, type, "compute", JavaType.VOID_TYPE, body);

    assertSame(type, edit.getDeclaringType());
    assertEquals("compute", edit.getMethodName());
    assertSame(JavaType.VOID_TYPE, edit.getReturnType());
    assertSame(body, readPrivateField(edit, "body"));
  }

  @Test
  public void declareMethodEpicHackSetterStoresMethodReference() throws Exception {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestType");
    type.superType.setValue(JavaType.getInstance(Object.class));
    DeclareMethodEdit edit = new DeclareMethodEdit(null, type, "compute", JavaType.VOID_TYPE);
    UserMethod method = new UserMethod();
    method.name.setValue("compute");

    edit.EPIC_HACK_FOR_TUTORIAL_GENERATION_setMethod(method);

    assertSame(method, readPrivateField(edit, "method"));
  }

  private Object readPrivateField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }

  private static final class TestStatementEdit extends StatementEdit<CompletionModel> {
    private boolean didDoOrRedo;
    private boolean didUndo;

    private TestStatementEdit(Statement statement) {
      super((UserActivity) null, statement);
    }

    @Override
    protected void doOrRedoInternal(boolean isDo) {
      didDoOrRedo = true;
    }

    @Override
    protected void undoInternal() {
      didUndo = true;
    }

    @Override
    protected void appendDescription(StringBuilder rv, DescriptionStyle descriptionStyle) {
      rv.append("test");
    }
  }
}
