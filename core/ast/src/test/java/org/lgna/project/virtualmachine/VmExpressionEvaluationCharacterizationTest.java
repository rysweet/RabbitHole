package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.ConditionalInfixExpression;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LogicalComplement;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.StringConcatenation;
import org.lgna.project.ast.StringLiteral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for VirtualMachine expression evaluation.
 * Documents the current behavior of literal evaluation, arithmetic,
 * conditional/relational/logical operators, and string concatenation.
 *
 * <p>Headless scope: no JavaFX, no gallery assets, no scene graph, no display.
 *
 * @see VirtualMachine#evaluate(Expression)
 */
public class VmExpressionEvaluationCharacterizationTest {

  private ReleaseVirtualMachine vm;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
  }

  private Object eval(Expression expression) {
    Object[] results = vm.ENTRY_POINT_evaluate(null, new Expression[]{expression});
    return results[0];
  }

  // --- Literal evaluation ---

  @Test
  public void integerLiteralEvaluatesToBoxedInteger() {
    assertEquals(42, eval(new IntegerLiteral(42)));
  }

  @Test
  public void integerLiteralZeroEvaluatesToZero() {
    assertEquals(0, eval(new IntegerLiteral(0)));
  }

  @Test
  public void integerLiteralNegativeEvaluatesToNegative() {
    assertEquals(-7, eval(new IntegerLiteral(-7)));
  }

  @Test
  public void doubleLiteralEvaluatesToBoxedDouble() {
    assertEquals(3.14, eval(new DoubleLiteral(3.14)));
  }

  @Test
  public void doubleLiteralZeroEvaluatesToZero() {
    assertEquals(0.0, eval(new DoubleLiteral(0.0)));
  }

  @Test
  public void booleanLiteralTrueEvaluatesToTrue() {
    assertEquals(true, eval(new BooleanLiteral(true)));
  }

  @Test
  public void booleanLiteralFalseEvaluatesToFalse() {
    assertEquals(false, eval(new BooleanLiteral(false)));
  }

  @Test
  public void stringLiteralEvaluatesToString() {
    assertEquals("hello", eval(new StringLiteral("hello")));
  }

  @Test
  public void stringLiteralEmptyEvaluatesToEmpty() {
    assertEquals("", eval(new StringLiteral("")));
  }

  @Test
  public void nullLiteralEvaluatesToNull() {
    assertNull(eval(new NullLiteral()));
  }

  // --- Arithmetic expressions ---

  @Test
  public void integerAddition() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(3),
        ArithmeticInfixExpression.Operator.PLUS,
        new IntegerLiteral(4),
        Integer.class);
    assertEquals(7, eval(expr));
  }

  @Test
  public void integerSubtraction() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(10),
        ArithmeticInfixExpression.Operator.MINUS,
        new IntegerLiteral(3),
        Integer.class);
    assertEquals(7, eval(expr));
  }

  @Test
  public void integerMultiplication() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(6),
        ArithmeticInfixExpression.Operator.TIMES,
        new IntegerLiteral(7),
        Integer.class);
    assertEquals(42, eval(expr));
  }

  @Test
  public void integerDivisionProducesDouble() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(10),
        ArithmeticInfixExpression.Operator.REAL_DIVIDE,
        new IntegerLiteral(4),
        Double.class);
    assertEquals(2.5, eval(expr));
  }

  @Test
  public void integerDivisionTruncatesWithIntegerDivide() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(10),
        ArithmeticInfixExpression.Operator.INTEGER_DIVIDE,
        new IntegerLiteral(3),
        Integer.class);
    assertEquals(3, eval(expr));
  }

  @Test
  public void integerRemainderWithIntegerRemainder() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(10),
        ArithmeticInfixExpression.Operator.INTEGER_REMAINDER,
        new IntegerLiteral(3),
        Integer.class);
    assertEquals(1, eval(expr));
  }

  @Test
  public void doubleAddition() {
    Expression expr = new ArithmeticInfixExpression(
        new DoubleLiteral(1.5),
        ArithmeticInfixExpression.Operator.PLUS,
        new DoubleLiteral(2.5),
        Double.class);
    assertEquals(4.0, eval(expr));
  }

  @Test
  public void mixedIntegerDoubleAdditionPromotesToDouble() {
    Expression expr = new ArithmeticInfixExpression(
        new IntegerLiteral(1),
        ArithmeticInfixExpression.Operator.PLUS,
        new DoubleLiteral(2.5),
        Double.class);
    assertEquals(3.5, eval(expr));
  }

  @Test
  public void nestedArithmeticExpression() {
    // (3 + 4) * 2 via nested AST
    Expression inner = new ArithmeticInfixExpression(
        new IntegerLiteral(3),
        ArithmeticInfixExpression.Operator.PLUS,
        new IntegerLiteral(4),
        Integer.class);
    Expression outer = new ArithmeticInfixExpression(
        inner,
        ArithmeticInfixExpression.Operator.TIMES,
        new IntegerLiteral(2),
        Integer.class);
    assertEquals(14, eval(outer));
  }

  // --- Conditional (boolean) infix expressions ---

  @Test
  public void andTrueTrue() {
    Expression expr = new ConditionalInfixExpression(
        new BooleanLiteral(true),
        ConditionalInfixExpression.Operator.AND,
        new BooleanLiteral(true));
    assertEquals(true, eval(expr));
  }

  @Test
  public void andTrueFalse() {
    Expression expr = new ConditionalInfixExpression(
        new BooleanLiteral(true),
        ConditionalInfixExpression.Operator.AND,
        new BooleanLiteral(false));
    assertEquals(false, eval(expr));
  }

  @Test
  public void andFalseShortCircuits() {
    // When left is false, right is not evaluated (AND short-circuit)
    Expression expr = new ConditionalInfixExpression(
        new BooleanLiteral(false),
        ConditionalInfixExpression.Operator.AND,
        new BooleanLiteral(true));
    assertEquals(false, eval(expr));
  }

  @Test
  public void orTrueShortCircuits() {
    // When left is true, right is not evaluated (OR short-circuit)
    Expression expr = new ConditionalInfixExpression(
        new BooleanLiteral(true),
        ConditionalInfixExpression.Operator.OR,
        new BooleanLiteral(false));
    assertEquals(true, eval(expr));
  }

  @Test
  public void orFalseFalse() {
    Expression expr = new ConditionalInfixExpression(
        new BooleanLiteral(false),
        ConditionalInfixExpression.Operator.OR,
        new BooleanLiteral(false));
    assertEquals(false, eval(expr));
  }

  @Test
  public void orFalseTrue() {
    Expression expr = new ConditionalInfixExpression(
        new BooleanLiteral(false),
        ConditionalInfixExpression.Operator.OR,
        new BooleanLiteral(true));
    assertEquals(true, eval(expr));
  }

  // --- Relational infix expressions ---

  @Test
  public void lessThanTrue() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(3),
        RelationalInfixExpression.Operator.LESS,
        new IntegerLiteral(5),
        Integer.class, Integer.class);
    assertTrue((Boolean) eval(expr));
  }

  @Test
  public void lessThanFalse() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(5),
        RelationalInfixExpression.Operator.LESS,
        new IntegerLiteral(3),
        Integer.class, Integer.class);
    assertFalse((Boolean) eval(expr));
  }

  @Test
  public void equalsTrue() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(42),
        RelationalInfixExpression.Operator.EQUALS,
        new IntegerLiteral(42),
        Integer.class, Integer.class);
    assertTrue((Boolean) eval(expr));
  }

  @Test
  public void equalsFalse() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(1),
        RelationalInfixExpression.Operator.EQUALS,
        new IntegerLiteral(2),
        Integer.class, Integer.class);
    assertFalse((Boolean) eval(expr));
  }

  @Test
  public void greaterThanTrue() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(5),
        RelationalInfixExpression.Operator.GREATER,
        new IntegerLiteral(3),
        Integer.class, Integer.class);
    assertTrue((Boolean) eval(expr));
  }

  @Test
  public void notEqualsTrue() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(1),
        RelationalInfixExpression.Operator.NOT_EQUALS,
        new IntegerLiteral(2),
        Integer.class, Integer.class);
    assertTrue((Boolean) eval(expr));
  }

  @Test
  public void lessEqualsWhenEqual() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(3),
        RelationalInfixExpression.Operator.LESS_EQUALS,
        new IntegerLiteral(3),
        Integer.class, Integer.class);
    assertTrue((Boolean) eval(expr));
  }

  @Test
  public void greaterEqualsWhenGreater() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(5),
        RelationalInfixExpression.Operator.GREATER_EQUALS,
        new IntegerLiteral(3),
        Integer.class, Integer.class);
    assertTrue((Boolean) eval(expr));
  }

  // --- Logical complement ---

  @Test
  public void logicalComplementOfTrue() {
    assertFalse((Boolean) eval(new LogicalComplement(new BooleanLiteral(true))));
  }

  @Test
  public void logicalComplementOfFalse() {
    assertTrue((Boolean) eval(new LogicalComplement(new BooleanLiteral(false))));
  }

  // --- String concatenation ---

  @Test
  public void stringConcatenationOfTwoStrings() {
    Expression expr = new StringConcatenation(
        new StringLiteral("hello"),
        new StringLiteral(" world"));
    assertEquals("hello world", eval(expr));
  }

  @Test
  public void stringConcatenationWithInteger() {
    Expression expr = new StringConcatenation(
        new StringLiteral("count: "),
        new IntegerLiteral(42));
    assertEquals("count: 42", eval(expr));
  }

  @Test
  public void stringConcatenationWithNull() {
    Expression expr = new StringConcatenation(
        new StringLiteral("value="),
        new NullLiteral());
    assertEquals("value=null", eval(expr));
  }

  @Test
  public void stringConcatenationNullLeft() {
    Expression expr = new StringConcatenation(
        new NullLiteral(),
        new StringLiteral("!"));
    assertEquals("null!", eval(expr));
  }

  // --- Multiple expressions in batch ---

  @Test
  public void batchEvaluateMultipleExpressions() {
    Object[] results = vm.ENTRY_POINT_evaluate(null, new Expression[]{
        new IntegerLiteral(1),
        new StringLiteral("two"),
        new BooleanLiteral(true)
    });
    assertEquals(3, results.length);
    assertEquals(1, results[0]);
    assertEquals("two", results[1]);
    assertEquals(true, results[2]);
  }
}
