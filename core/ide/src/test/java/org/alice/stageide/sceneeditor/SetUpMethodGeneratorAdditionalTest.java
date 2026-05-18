package org.alice.stageide.sceneeditor;

import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.stageide.ast.ExpressionCreator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.*;
import org.lgna.story.Orientation;
import org.lgna.story.Position;
import org.lgna.story.SBiped;
import org.lgna.story.SGround;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class SetUpMethodGeneratorAdditionalTest {
  private ExpressionCreator originalExpressionCreator;
  private StubExpressionCreator stubExpressionCreator;

  @Before
  public void installStubExpressionCreator() throws Exception {
    Field field = SetUpMethodGenerator.class.getDeclaredField("expressionCreator");
    field.setAccessible(true);
    originalExpressionCreator = (ExpressionCreator) field.get(null);
    stubExpressionCreator = new StubExpressionCreator();
    field.set(null, stubExpressionCreator);
  }

  @After
  public void restoreExpressionCreator() throws Exception {
    Field field = SetUpMethodGenerator.class.getDeclaredField("expressionCreator");
    field.setAccessible(true);
    field.set(null, originalExpressionCreator);
  }

  @Test
  public void createOrientationStatementWithDurationAddsKeyedArgument() throws Exception {
    UserField field = createField("biped", SBiped.class);
    Orientation orientation = new Orientation();

    MethodInvocation invocation = invocationOf(
        SetUpMethodGenerator.createOrientationStatement(true, field, orientation, 0.25));

    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertEquals("setOrientationRelativeToVehicle", invocation.method.getValue().getName());
    assertSame(orientation, stubExpressionCreator.lastValue);
    assertEquals(1, stubExpressionCreator.callCount);
    assertEquals(1, invocation.keyedArguments.size());
    assertDurationKey(invocation, 0.25);
  }

  @Test
  public void createOrientationStatementWithoutDurationUsesFieldAccess() throws Exception {
    UserField field = createField("biped", SBiped.class);

    MethodInvocation invocation = invocationOf(
        SetUpMethodGenerator.createOrientationStatement(false, field, new Orientation()));

    assertTrue(invocation.expression.getValue() instanceof FieldAccess);
    assertTrue(invocation.keyedArguments.isEmpty());
    assertEquals("Orientation", ((StringLiteral) invocation.requiredArguments.get(0).expression.getValue()).value.getValue());
  }

  @Test
  public void createPositionStatementWithDurationAddsKeyedArgument() throws Exception {
    UserField field = createField("biped", SBiped.class);
    Position position = new Position(1, 2, 3);

    MethodInvocation invocation = invocationOf(
        SetUpMethodGenerator.createPositionStatement(false, field, position, 1.5));

    assertTrue(invocation.expression.getValue() instanceof FieldAccess);
    assertEquals("setPositionRelativeToVehicle", invocation.method.getValue().getName());
    assertSame(position, stubExpressionCreator.lastValue);
    assertDurationKey(invocation, 1.5);
  }

  @Test
  public void getSetupStatementsForFieldWithoutTransformCreatesOnlyVehicleStatement() {
    UserField rider = createField("rider", SBiped.class);

    Statement[] statements = SetUpMethodGenerator.getSetupStatementsForField(false, rider, null, null, null);

    assertEquals(1, statements.length);
    MethodInvocation invocation = invocationOf(statements[0]);
    assertEquals("setVehicle", invocation.method.getValue().getName());
    assertTrue(invocation.requiredArguments.get(0).expression.getValue() instanceof ThisExpression);
  }

  @Test
  public void getSetupStatementsForFieldWithTransformCreatesVehicleOrientationAndPosition() {
    UserField rider = createField("rider", SBiped.class);
    UserField vehicle = createField("ground", SGround.class);

    Statement[] statements = SetUpMethodGenerator.getSetupStatementsForField(
        false,
        rider,
        null,
        vehicle,
        AffineMatrix4x4.createTranslation(1, 2, 3));

    assertEquals(3, statements.length);
    assertEquals("setVehicle", invocationOf(statements[0]).method.getValue().getName());
    assertEquals("setOrientationRelativeToVehicle", invocationOf(statements[1]).method.getValue().getName());
    assertEquals("setPositionRelativeToVehicle", invocationOf(statements[2]).method.getValue().getName());
    assertTrue(invocationOf(statements[0]).requiredArguments.get(0).expression.getValue() instanceof FieldAccess);
    assertDurationKey(invocationOf(statements[1]), 0.0);
    assertDurationKey(invocationOf(statements[2]), 0.0);
  }

  private static UserField createField(String name, Class<?> type) {
    UserField field = new UserField();
    field.name.setValue(name);
    field.valueType.setValue(JavaType.getInstance(type));
    return field;
  }

  private static MethodInvocation invocationOf(Statement statement) {
    assertTrue(statement instanceof ExpressionStatement);
    return (MethodInvocation) ((ExpressionStatement) statement).expression.getValue();
  }

  private static void assertDurationKey(MethodInvocation invocation, double expectedDuration) {
    assertEquals(1, invocation.keyedArguments.size());
    assertTrue(invocation.keyedArguments.get(0).expression.getValue() instanceof MethodInvocation);
    MethodInvocation durationInvocation = (MethodInvocation) invocation.keyedArguments.get(0).expression.getValue();
    assertEquals("duration", durationInvocation.method.getValue().getName());
    assertEquals(expectedDuration,
        ((DoubleLiteral) durationInvocation.requiredArguments.get(0).expression.getValue()).value.getValue(),
        0.0);
  }

  private static final class StubExpressionCreator extends org.alice.stageide.ast.ExpressionCreator {
    private Object lastValue;
    private int callCount;

    @Override
    protected Expression createCustomExpression(Object value) {
      lastValue = value;
      callCount++;
      return new StringLiteral(value.getClass().getSimpleName());
    }
  }
}
