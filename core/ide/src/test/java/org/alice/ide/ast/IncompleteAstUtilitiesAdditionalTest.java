package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.*;

import static org.junit.Assert.*;

/**
 * Additional characterization tests for {@link IncompleteAstUtilities}.
 * Focus on factory methods that don't require IDE singletons.
 */
public class IncompleteAstUtilitiesAdditionalTest {

  @Test
  public void createIncompleteArithmeticInfixExpression_withClasses() {
    ArithmeticInfixExpression expr = IncompleteAstUtilities.createIncompleteArithmeticInfixExpression(
        Double.class, ArithmeticInfixExpression.Operator.PLUS, Double.class, Double.class);
    assertNotNull(expr);
    assertNotNull(expr.leftOperand.getValue());
    assertEquals(ArithmeticInfixExpression.Operator.PLUS, expr.operator.getValue());
  }

  @Test
  public void createIncompleteArithmeticInfixExpression_withTypes() {
    AbstractType<?, ?, ?> doubleType = JavaType.getInstance(Double.class);
    ArithmeticInfixExpression expr = IncompleteAstUtilities.createIncompleteArithmeticInfixExpression(
        doubleType, ArithmeticInfixExpression.Operator.MINUS, doubleType, doubleType);
    assertNotNull(expr);
  }

  @Test
  public void createIncompleteArithmeticInfixExpression_multiplyOperator() {
    ArithmeticInfixExpression expr = IncompleteAstUtilities.createIncompleteArithmeticInfixExpression(
        Integer.class, ArithmeticInfixExpression.Operator.TIMES, Integer.class, Integer.class);
    assertNotNull(expr);
    assertEquals(ArithmeticInfixExpression.Operator.TIMES, expr.operator.getValue());
  }

  @Test
  public void createIncompleteArithmeticInfixExpression_divideOperator() {
    ArithmeticInfixExpression expr = IncompleteAstUtilities.createIncompleteArithmeticInfixExpression(
        Double.class, ArithmeticInfixExpression.Operator.REAL_DIVIDE, Double.class, Double.class);
    assertNotNull(expr);
    assertEquals(ArithmeticInfixExpression.Operator.REAL_DIVIDE, expr.operator.getValue());
  }

  @Test
  public void createIncompleteArithmeticInfixExpression_withLeftExpression() {
    Expression left = new DoubleLiteral(42.0);
    AbstractType<?, ?, ?> doubleType = JavaType.getInstance(Double.class);
    ArithmeticInfixExpression expr = IncompleteAstUtilities.createIncompleteArithmeticInfixExpression(
        left, ArithmeticInfixExpression.Operator.PLUS, doubleType, doubleType);
    assertNotNull(expr);
    assertSame(left, expr.leftOperand.getValue());
  }

  @Test
  public void createIncompleteConditionalInfixExpression_and() {
    ConditionalInfixExpression expr = IncompleteAstUtilities.createIncompleteConditionalInfixExpression(
        ConditionalInfixExpression.Operator.AND);
    assertNotNull(expr);
    assertNotNull(expr.leftOperand.getValue());
    assertEquals(ConditionalInfixExpression.Operator.AND, expr.operator.getValue());
  }

  @Test
  public void createIncompleteConditionalInfixExpression_or() {
    ConditionalInfixExpression expr = IncompleteAstUtilities.createIncompleteConditionalInfixExpression(
        ConditionalInfixExpression.Operator.OR);
    assertNotNull(expr);
    assertEquals(ConditionalInfixExpression.Operator.OR, expr.operator.getValue());
  }

  @Test
  public void createIncompleteConditionalInfixExpression_withLeftOperand() {
    Expression left = new EmptyExpression(Boolean.class);
    ConditionalInfixExpression expr = IncompleteAstUtilities.createIncompleteConditionalInfixExpression(
        left, ConditionalInfixExpression.Operator.AND);
    assertNotNull(expr);
    assertSame(left, expr.leftOperand.getValue());
  }

  @Test
  public void createIncompleteRelationalInfixExpression_withClasses() {
    RelationalInfixExpression expr = IncompleteAstUtilities.createIncompleteRelationalInfixExpression(
        Double.class, RelationalInfixExpression.Operator.LESS, Double.class);
    assertNotNull(expr);
    assertEquals(RelationalInfixExpression.Operator.LESS, expr.operator.getValue());
  }

  @Test
  public void createIncompleteRelationalInfixExpression_greaterEquals() {
    RelationalInfixExpression expr = IncompleteAstUtilities.createIncompleteRelationalInfixExpression(
        Integer.class, RelationalInfixExpression.Operator.GREATER_EQUALS, Integer.class);
    assertNotNull(expr);
    assertEquals(RelationalInfixExpression.Operator.GREATER_EQUALS, expr.operator.getValue());
  }

  @Test
  public void createIncompleteRelationalInfixExpression_withTypes() {
    AbstractType<?, ?, ?> intType = JavaType.getInstance(Integer.class);
    RelationalInfixExpression expr = IncompleteAstUtilities.createIncompleteRelationalInfixExpression(
        intType, RelationalInfixExpression.Operator.GREATER, intType);
    assertNotNull(expr);
  }

  @Test
  public void createIncompleteLogicalComplement() {
    LogicalComplement complement = IncompleteAstUtilities.createIncompleteLogicalComplement();
    assertNotNull(complement);
    assertNotNull(complement.operand.getValue());
  }

  @Test
  public void createIncompleteLocalDeclarationStatement() {
    LocalDeclarationStatement stmt = IncompleteAstUtilities.createIncompleteLocalDeclarationStatement();
    assertNotNull(stmt);
    assertNotNull(stmt.local.getValue());
    assertEquals("???", stmt.local.getValue().name.getValue());
  }

  @Test
  public void createIncompleteCountLoop() {
    CountLoop loop = IncompleteAstUtilities.createIncompleteCountLoop();
    assertNotNull(loop);
    assertNotNull(loop.count.getValue());
  }

  @Test
  public void createIncompleteWhileLoop() {
    WhileLoop loop = IncompleteAstUtilities.createIncompleteWhileLoop();
    assertNotNull(loop);
    assertNotNull(loop.conditional.getValue());
  }

  @Test
  public void createIncompleteConditionalStatement() {
    ConditionalStatement stmt = IncompleteAstUtilities.createIncompleteConditionalStatement();
    assertNotNull(stmt);
  }

  @Test
  public void createIncompleteForEachInArrayLoop() {
    ForEachInArrayLoop loop = IncompleteAstUtilities.createIncompleteForEachInArrayLoop();
    assertNotNull(loop);
  }

  @Test
  public void createIncompleteEachInArrayTogether() {
    EachInArrayTogether together = IncompleteAstUtilities.createIncompleteEachInArrayTogether();
    assertNotNull(together);
  }

  @Test
  public void createIncompleteMethodInvocation_withExpressionAndMethod() {
    UserMethod method = new UserMethod();
    method.name.setValue("doSomething");
    method.managementLevel.setValue(ManagementLevel.NONE);
    method.returnType.setValue(JavaType.VOID_TYPE);
    Expression expression = new EmptyExpression(Object.class);
    MethodInvocation invocation = IncompleteAstUtilities.createIncompleteMethodInvocation(expression, method);
    assertNotNull(invocation);
    assertSame(expression, invocation.expression.getValue());
    assertSame(method, invocation.method.getValue());
  }
}
