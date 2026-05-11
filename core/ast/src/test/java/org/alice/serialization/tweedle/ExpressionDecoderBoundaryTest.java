package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.ConditionalInfixExpression;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.LocalAccess;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.LogicalComplement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.StringConcatenation;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Boundary tests for ExpressionDecoder targeting untested paths:
 * unknown identifiers, all 6 relational operators, logical AND/OR/NOT,
 * string concatenation, integer vs. real division dispatch, and
 * return expression variants.
 *
 * <p>All tests drive through TweedleEncoderDecoder.decode() using
 * synthetic Tweedle class source strings.
 */
public class ExpressionDecoderBoundaryTest {
  private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

  // ═══════════════════════════════════════════════════════════════════════════
  // UNKNOWN IDENTIFIER: error path
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void unknownIdentifierInMethodBodyReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber compute() { return ghostVar; }
            }
            """));

    assertTrue(thrown.getMessage().contains("identifier"));
    assertTrue(thrown.getMessage().contains("ghostVar"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RELATIONAL OPERATORS: all six
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void equalToOperatorDecodesEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a == b; }
        }
        """);

    RelationalInfixExpression rel = returnRelational(type);
    assertSame(RelationalInfixExpression.Operator.EQUALS, rel.operator.getValue());
  }

  @Test
  public void notEqualToOperatorDecodesNotEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a != b; }
        }
        """);

    RelationalInfixExpression rel = returnRelational(type);
    assertSame(RelationalInfixExpression.Operator.NOT_EQUALS, rel.operator.getValue());
  }

  @Test
  public void lessThanOperatorDecodesLess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a < b; }
        }
        """);

    RelationalInfixExpression rel = returnRelational(type);
    assertSame(RelationalInfixExpression.Operator.LESS, rel.operator.getValue());
  }

  @Test
  public void lessThanOrEqualOperatorDecodesLessEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a <= b; }
        }
        """);

    RelationalInfixExpression rel = returnRelational(type);
    assertSame(RelationalInfixExpression.Operator.LESS_EQUALS, rel.operator.getValue());
  }

  @Test
  public void greaterThanOperatorDecodesGreater() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a > b; }
        }
        """);

    RelationalInfixExpression rel = returnRelational(type);
    assertSame(RelationalInfixExpression.Operator.GREATER, rel.operator.getValue());
  }

  @Test
  public void greaterThanOrEqualOperatorDecodesGreaterEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a >= b; }
        }
        """);

    RelationalInfixExpression rel = returnRelational(type);
    assertSame(RelationalInfixExpression.Operator.GREATER_EQUALS, rel.operator.getValue());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LOGICAL OPERATORS: AND, OR, NOT
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void logicalAndOperatorDecodesConditionalAnd() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a, Boolean b) { return a && b; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof ConditionalInfixExpression);
    ConditionalInfixExpression cond = (ConditionalInfixExpression) ret.expression.getValue();
    assertSame(ConditionalInfixExpression.Operator.AND, cond.operator.getValue());
  }

  @Test
  public void logicalOrOperatorDecodesConditionalOr() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a, Boolean b) { return a || b; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof ConditionalInfixExpression);
    ConditionalInfixExpression cond = (ConditionalInfixExpression) ret.expression.getValue();
    assertSame(ConditionalInfixExpression.Operator.OR, cond.operator.getValue());
  }

  @Test
  public void logicalNotOperatorDecodesLogicalComplement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a) { return !a; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof LogicalComplement);
    LogicalComplement complement = (LogicalComplement) ret.expression.getValue();
    assertTrue(complement.operand.getValue() instanceof ParameterAccess);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STRING CONCATENATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void stringConcatenationDecodesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          TextString greet(TextString name) { return "hello " .. name; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof StringConcatenation);
    StringConcatenation concat = (StringConcatenation) ret.expression.getValue();
    assertTrue(concat.leftOperand.getValue() instanceof StringLiteral);
    assertTrue(concat.rightOperand.getValue() instanceof ParameterAccess);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ARITHMETIC IN METHOD BODY: through local variable initializers
  // (return decoder doesn't support arithmetic directly)
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void integerDivisionInLocalInitDecodesIntegerDivide() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber result <- 10 / 3;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement local = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(local.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) local.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.INTEGER_DIVIDE, infix.operator.getValue());
  }

  @Test
  public void realDivisionInLocalInitDecodesRealDivide() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          DecimalNumber compute() {
            DecimalNumber result <- 10.0 / 3.0;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement local = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(local.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) local.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.REAL_DIVIDE, infix.operator.getValue());
  }

  @Test
  public void additionInLocalInitDecodesPlusOperator() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber result <- 1 + 2;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement local = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(local.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) local.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.PLUS, infix.operator.getValue());
  }

  @Test
  public void subtractionInLocalInitDecodesMinusOperator() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber result <- 5 - 3;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement local = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(local.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) local.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.MINUS, infix.operator.getValue());
  }

  @Test
  public void multiplicationInLocalInitDecodesTimesOperator() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber result <- 2 * 4;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement local = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(local.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) local.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.TIMES, infix.operator.getValue());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RETURN EXPRESSION: local, parameter, and field references
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void returnParameterDecodesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber identity(WholeNumber x) { return x; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof ParameterAccess);
  }

  @Test
  public void returnLocalDecodesLocalAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber temp <- 42;
            return temp;
          }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof LocalAccess);
  }

  @Test
  public void returnFieldDecodesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber stored <- 7;
          WholeNumber getStored() { return stored; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof org.lgna.project.ast.FieldAccess);
  }

  @Test
  public void returnThisFieldDecodesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber stored <- 7;
          WholeNumber getStored() { return this.stored; }
        }
        """);

    ReturnStatement ret = returnStatement(type);
    assertTrue(ret.expression.getValue() instanceof org.lgna.project.ast.FieldAccess);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // UNSUPPORTED EXPRESSION: error path
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void unsupportedReturnExpressionReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber bad() { return this.unknown(); }
            }
            """));

    assertTrue(thrown.getMessage().contains("bad"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private NamedUserType decodeUserType(String source) throws Exception {
    AbstractNode decoded = coder.decode(source);
    assertTrue("Expected NamedUserType, got: " + decoded.getClass().getSimpleName(),
        decoded instanceof NamedUserType);
    return (NamedUserType) decoded;
  }

  private static ReturnStatement returnStatement(NamedUserType type) {
    UserMethod method = type.getDeclaredMethods().get(0);
    BlockStatement body = method.body.getValue();
    for (int i = body.statements.size() - 1; i >= 0; i--) {
      if (body.statements.get(i) instanceof ReturnStatement ret) {
        return ret;
      }
    }
    throw new AssertionError("No ReturnStatement found in method " + method.getName());
  }

  private static RelationalInfixExpression returnRelational(NamedUserType type) {
    ReturnStatement ret = returnStatement(type);
    assertTrue("Expected RelationalInfixExpression, got: " + ret.expression.getValue().getClass().getSimpleName(),
        ret.expression.getValue() instanceof RelationalInfixExpression);
    return (RelationalInfixExpression) ret.expression.getValue();
  }
}
