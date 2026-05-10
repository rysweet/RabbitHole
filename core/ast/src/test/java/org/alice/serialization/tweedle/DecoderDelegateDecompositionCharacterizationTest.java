package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.AssignmentExpression;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.ConditionalInfixExpression;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalAccess;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.LogicalComplement;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.StringConcatenation;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.WhileLoop;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests pinning Decoder.java behavior before delegate extraction
 * into ExpressionDecoder, StatementDecoder, and FieldDecoder (issue #480).
 *
 * <p>These tests target code paths NOT covered by TweedleEncoderDecoderTest and
 * focus on cross-delegate boundaries, error message preservation, and integration
 * scenarios that exercise multiple delegates in a single decode. Every test must
 * pass green against the current monolithic Decoder AND continue passing after
 * the decomposition.
 *
 * <p>Coverage strategy by delegate boundary:
 * <ul>
 *   <li>FieldDecoder: allowLiteralArithmeticFieldInitializers=false, nested arithmetic,
 *       type-mismatch arithmetic initializer, missing initializer error</li>
 *   <li>ExpressionDecoder: unsupported value expressions, ambiguous division type,
 *       return with arithmetic/logical, string concat in local init in constructor</li>
 *   <li>StatementDecoder: copy() path, constructor name mismatch, constructor body with
 *       unsupported statement, non-this field access in assignment, multiple while-loop
 *       body assignments, non-boolean if condition, non-void method without return</li>
 *   <li>Coordinator: terminal type resolution, superclass from terminals, combined
 *       integration scenarios</li>
 * </ul>
 */
public class DecoderDelegateDecompositionCharacterizationTest {
  private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

  // ═══════════════════════════════════════════════════════════════════════════
  // COORDINATOR: copy() path and terminal type resolution
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void copyDecodesIdenticallyToDecode() throws Exception {
    String source = "class CopyTarget { WholeNumber count <- 7; }";
    NamedUserType terminal = userTypeNamed("CopyTarget");

    AbstractNode decoded = coder.decode(source, Set.of(terminal));
    AbstractNode copied = coder.copy(source, Set.of(terminal));

    assertTrue(decoded instanceof NamedUserType);
    assertTrue(copied instanceof NamedUserType);
    NamedUserType decodedType = (NamedUserType) decoded;
    NamedUserType copiedType = (NamedUserType) copied;
    assertEquals(decodedType.getName(), copiedType.getName());
    assertEquals(decodedType.getDeclaredFields().size(), copiedType.getDeclaredFields().size());
    // copy returns the same terminal reference (reuses existing type)
    assertSame(terminal, copiedType);
    assertSame(terminal, decodedType);
  }

  @Test
  public void copyWithoutTerminalCreatesNewType() throws Exception {
    String source = "class FreshType { WholeNumber count <- 1; }";

    AbstractNode decoded = coder.decode(source);
    AbstractNode copied = coder.copy(source, Set.of());

    assertTrue(decoded instanceof NamedUserType);
    assertTrue(copied instanceof NamedUserType);
    assertNotSame(decoded, copied);
    assertEquals("FreshType", ((NamedUserType) decoded).getName());
    assertEquals("FreshType", ((NamedUserType) copied).getName());
  }

  @Test
  public void decodeResolvesTerminalTypeForFieldValueType() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType(
        "class SyntheticType { Friend companion <- null; }",
        Set.of(friendType));

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("companion", field.getName());
    assertSame(friendType, field.getValueType());
  }

  @Test
  public void decodeResolvesTerminalTypeForMethodParameter() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType(
        "class SyntheticType { void greet(Friend f) { } }",
        Set.of(friendType));

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.getRequiredParameters().size());
    assertSame(friendType, method.getRequiredParameters().get(0).getValueType());
  }

  @Test
  public void decodeResolvesTerminalTypeForMethodReturnType() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Friend companion <- null;
          Friend getCompanion() { return companion; }
        }
        """, Set.of(friendType));

    UserMethod method = type.getDeclaredMethods().get(0);
    assertSame(friendType, method.getReturnType());
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(friendType, ret.expressionType.getValue());
  }

  @Test
  public void decodeResolvesTerminalTypeForConstructorParameter() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType(
        "class SyntheticType { SyntheticType(Friend f) { } }",
        Set.of(friendType));

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.getRequiredParameters().size());
    assertSame(friendType, constructor.getRequiredParameters().get(0).getValueType());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FIELD DECODER: allowLiteralArithmeticFieldInitializers flag,
  // nested arithmetic, type mismatches
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void decodeWithLiteralArithmeticDisabledRejectsArithmeticFieldInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(
            "class SyntheticType { WholeNumber count <- 1 + 2; }",
            Set.of(),
            false));

    assertTrue(thrown.getMessage().contains("initializers"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeWithLiteralArithmeticEnabledAcceptsArithmeticFieldInitializer() throws Exception {
    AbstractNode decoded = coder.decode(
        "class SyntheticType { WholeNumber count <- 1 + 2; }",
        Set.of(),
        true);

    assertTrue(decoded instanceof NamedUserType);
    NamedUserType type = (NamedUserType) decoded;
    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.PLUS, infix.operator.getValue());
  }

  @Test
  public void decodeNestedArithmeticFieldInitializerCreatesNestedInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber count <- 1 + 2 + 3; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression outerInfix = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.PLUS, outerInfix.operator.getValue());
    // The left operand should be another ArithmeticInfixExpression (1 + 2)
    assertTrue(outerInfix.leftOperand.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression innerInfix = (ArithmeticInfixExpression) outerInfix.leftOperand.getValue();
    assertSame(ArithmeticInfixExpression.Operator.PLUS, innerInfix.operator.getValue());
    assertIntegerLiteral(innerInfix.leftOperand.getValue(), 1);
    assertIntegerLiteral(innerInfix.rightOperand.getValue(), 2);
    // Right operand of outer is the literal 3
    assertIntegerLiteral(outerInfix.rightOperand.getValue(), 3);
  }

  @Test
  public void decodeFieldArithmeticInitializerTypeMismatchReportsError() {
    // TextString field cannot hold arithmetic result — parser rejects before decoder sees it
    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> coder.decode("class SyntheticType { TextString label <- 1 + 2; }"));

    assertTrue(thrown.getMessage().contains("Unable to parse Tweedle type"));
  }

  @Test
  public void decodeMissingFieldInitializerCreatesNullInitializer() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber count; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    assertNull(field.initializer.getValue());
  }

  @Test
  public void decodeMultipleFieldsPreservesDeclarationOrder() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber alpha <- 1;
          TextString beta <- "b";
          DecimalNumber gamma <- 3.0;
          Boolean delta <- true;
        }
        """);

    assertEquals(4, type.getDeclaredFields().size());
    assertEquals("alpha", type.getDeclaredFields().get(0).getName());
    assertEquals("beta", type.getDeclaredFields().get(1).getName());
    assertEquals("gamma", type.getDeclaredFields().get(2).getName());
    assertEquals("delta", type.getDeclaredFields().get(3).getName());
    assertIntegerLiteral(type.getDeclaredFields().get(0).initializer.getValue(), 1);
    assertTrue(type.getDeclaredFields().get(1).initializer.getValue() instanceof StringLiteral);
    assertTrue(type.getDeclaredFields().get(2).initializer.getValue() instanceof DoubleLiteral);
    assertTrue(type.getDeclaredFields().get(3).initializer.getValue() instanceof BooleanLiteral);
  }

  @Test
  public void decodeDecimalSubtractionFieldInitializerCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { DecimalNumber dist <- 5.0 - 1.5; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.MINUS, infix.operator.getValue());
    assertSame(JavaType.getInstance(Double.class), infix.getType());
    assertTrue(infix.leftOperand.getValue() instanceof DoubleLiteral);
    assertEquals(5.0, ((DoubleLiteral) infix.leftOperand.getValue()).value.getValue(), 0.0);
    assertTrue(infix.rightOperand.getValue() instanceof DoubleLiteral);
    assertEquals(1.5, ((DoubleLiteral) infix.rightOperand.getValue()).value.getValue(), 0.0);
  }

  @Test
  public void decodeMultiplicationFieldInitializerCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber area <- 3 * 4; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.TIMES, infix.operator.getValue());
    assertIntegerLiteral(infix.leftOperand.getValue(), 3);
    assertIntegerLiteral(infix.rightOperand.getValue(), 4);
  }

  @Test
  public void decodeIntegerDivisionFieldInitializerCreatesIntegerDivideInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber half <- 10 / 2; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.INTEGER_DIVIDE, infix.operator.getValue());
    assertIntegerLiteral(infix.leftOperand.getValue(), 10);
    assertIntegerLiteral(infix.rightOperand.getValue(), 2);
  }

  @Test
  public void decodeDecimalDivisionFieldInitializerCreatesRealDivideInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { DecimalNumber ratio <- 10.0 / 3.0; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.REAL_DIVIDE, infix.operator.getValue());
    assertTrue(infix.leftOperand.getValue() instanceof DoubleLiteral);
    assertTrue(infix.rightOperand.getValue() instanceof DoubleLiteral);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STATEMENT DECODER: constructor edge cases, control flow boundaries
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void decodeConstructorNameMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { OtherName() { } }"));

    assertTrue(thrown.getMessage().contains("constructor name does not match"));
    assertTrue(thrown.getMessage().contains("OtherName"));
  }

  @Test
  public void decodeConstructorWithNonEmptyBodyCreatesSuperInvocationFirst() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { count <- 7; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    // Non-empty constructor body gets a SuperConstructorInvocationStatement prepended
    assertTrue(constructor.body.getValue().constructorInvocationStatement.getValue()
        instanceof SuperConstructorInvocationStatement);
  }

  @Test
  public void decodeConstructorWithEmptyBodyHasNoStatements() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { SyntheticType() { } }");

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertTrue(constructor.body.getValue().statements.isEmpty());
  }

  @Test
  public void decodeConstructorBodyWithMixedStatementTypesPreservesOrder() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType(WholeNumber val) {
            WholeNumber temp <- val;
            count <- temp;
            this.helper();
          }
          void helper() { }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(3, constructor.body.getValue().statements.size());
    // Statement 0: local declaration
    assertTrue(constructor.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    LocalDeclarationStatement localDecl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    assertEquals("temp", localDecl.local.getValue().getName());
    assertTrue(localDecl.initializer.getValue() instanceof ParameterAccess);
    // Statement 1: field assignment
    assertTrue(constructor.body.getValue().statements.get(1) instanceof ExpressionStatement);
    ExpressionStatement assignStmt = (ExpressionStatement) constructor.body.getValue().statements.get(1);
    assertTrue(assignStmt.expression.getValue() instanceof AssignmentExpression);
    // Statement 2: method call
    assertTrue(constructor.body.getValue().statements.get(2) instanceof ExpressionStatement);
    ExpressionStatement callStmt = (ExpressionStatement) constructor.body.getValue().statements.get(2);
    assertTrue(callStmt.expression.getValue() instanceof MethodInvocation);
  }

  @Test
  public void decodeConstructorWithUnknownThisFieldAssignmentTargetReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              SyntheticType() { this.missing <- 1; }
            }
            """));

    assertTrue(thrown.getMessage().contains("this.field assignment target is not a known field"));
    assertTrue(thrown.getMessage().contains("missing"));
  }

  @Test
  public void decodeConstructorThisFieldAssignmentWithTypeMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              SyntheticType() { this.count <- "bad"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("constructor field assignment value type is not assignable to"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeMethodWithNonThisFieldAccessAssignmentTargetReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad(WholeNumber x) { x.field <- 1; }
            }
            """));

    assertTrue(thrown.getMessage().contains("Only this.field"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeConstructorWithNonThisFieldAccessAssignmentTargetReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              SyntheticType(WholeNumber x) { x.field <- 1; }
            }
            """));

    assertTrue(thrown.getMessage().contains("Only this.field"));
    assertTrue(thrown.getMessage().contains("SyntheticType"));
  }

  @Test
  public void decodeMethodWithUnknownThisFieldAssignmentTargetReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad() { this.missing <- 7; }
            }
            """));

    assertTrue(thrown.getMessage().contains("this.field assignment target is not a known field"));
    assertTrue(thrown.getMessage().contains("missing"));
  }

  @Test
  public void decodeMethodThisFieldAssignmentTypeMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              void bad() { this.count <- "wrong"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("field assignment value type is not assignable to"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeNonVoidMethodWithoutReturnStatementReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              WholeNumber bad() { count <- 1; }
            }
            """));

    assertTrue(thrown.getMessage().contains("method bodies"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeIfConditionNonBooleanReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              void bad(WholeNumber n) {
                if (n) { count <- 0; }
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("if condition"));
    assertTrue(thrown.getMessage().contains("Boolean"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeWhileLoopWithMultipleAssignmentsCreatesMultipleStatements() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber a <- 0;
          WholeNumber b <- 0;
          void process(Boolean running) {
            while (running) { a <- 1; b <- 2; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    WhileLoop loop = (WhileLoop) method.body.getValue().statements.get(0);
    assertEquals(2, loop.body.getValue().statements.size());
    // First assignment: a <- 1
    ExpressionStatement stmt0 = (ExpressionStatement) loop.body.getValue().statements.get(0);
    AssignmentExpression assign0 = (AssignmentExpression) stmt0.expression.getValue();
    assertSame(type.getDeclaredFields().get(0),
        ((FieldAccess) assign0.leftHandSide.getValue()).field.getValue());
    assertIntegerLiteral(assign0.rightHandSide.getValue(), 1);
    // Second assignment: b <- 2
    ExpressionStatement stmt1 = (ExpressionStatement) loop.body.getValue().statements.get(1);
    AssignmentExpression assign1 = (AssignmentExpression) stmt1.expression.getValue();
    assertSame(type.getDeclaredFields().get(1),
        ((FieldAccess) assign1.leftHandSide.getValue()).field.getValue());
    assertIntegerLiteral(assign1.rightHandSide.getValue(), 2);
  }

  @Test
  public void decodeMethodWithIfThenAssignmentAndWhileLoopCreatesBothStatements() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void run(Boolean flag) {
            if (flag) { count <- 1; }
            while (flag) { count <- 0; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ConditionalStatement);
    assertTrue(method.body.getValue().statements.get(1) instanceof WhileLoop);
  }

  @Test
  public void decodeMethodWithLocalThenIfThenReturnPreservesStatementOrder() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          WholeNumber compute(Boolean flag) {
            WholeNumber result <- 0;
            if (flag) { result <- 1; }
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(3, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    assertTrue(method.body.getValue().statements.get(1) instanceof ConditionalStatement);
    assertTrue(method.body.getValue().statements.get(2) instanceof ReturnStatement);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(2);
    assertTrue(ret.expression.getValue() instanceof LocalAccess);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EXPRESSION DECODER: arithmetic return, logical return, unsupported exprs
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void decodeMethodReturnWithLogicalNotCreatesLogicalComplement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean negate(Boolean a) { return !a; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof LogicalComplement);
    assertSame(JavaType.BOOLEAN_OBJECT_TYPE, ret.expression.getValue().getType());
  }

  @Test
  public void decodeMethodReturnWithLogicalAndCreatesConditionalAnd() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean both(Boolean a, Boolean b) { return a && b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof ConditionalInfixExpression);
    ConditionalInfixExpression cond = (ConditionalInfixExpression) ret.expression.getValue();
    assertSame(ConditionalInfixExpression.Operator.AND, cond.operator.getValue());
  }

  @Test
  public void decodeMethodReturnWithLogicalOrCreatesConditionalOr() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean either(Boolean a, Boolean b) { return a || b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof ConditionalInfixExpression);
    ConditionalInfixExpression cond = (ConditionalInfixExpression) ret.expression.getValue();
    assertSame(ConditionalInfixExpression.Operator.OR, cond.operator.getValue());
  }

  @Test
  public void decodeMethodReturnWithStringConcatCreatesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          TextString greet(TextString name) { return "Hello " .. name; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof StringConcatenation);
    StringConcatenation concat = (StringConcatenation) ret.expression.getValue();
    assertTrue(concat.leftOperand.getValue() instanceof StringLiteral);
    assertTrue(concat.rightOperand.getValue() instanceof ParameterAccess);
  }

  @Test
  public void decodeMethodReturnWithComparisonCreatesRelationalInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean isPositive(WholeNumber n) { return n > 0; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof RelationalInfixExpression);
    RelationalInfixExpression rel = (RelationalInfixExpression) ret.expression.getValue();
    assertSame(RelationalInfixExpression.Operator.GREATER, rel.operator.getValue());
  }

  @Test
  public void decodeMethodReturnWithThisFieldAccessCreatesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 42;
          WholeNumber getCount() { return this.count; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) ret.expression.getValue()).field.getValue());
  }

  @Test
  public void decodeMethodReturnThisFieldTypeMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 42;
              TextString bad() { return this.count; }
            }
            """));

    assertTrue(thrown.getMessage().contains("not assignable to"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeMethodReturnNonThisFieldAccessReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber getCount(WholeNumber x) { return x.field; }
            }
            """));

    assertTrue(thrown.getMessage().contains("member expressions"));
    assertTrue(thrown.getMessage().contains("getCount"));
    assertTrue(thrown.getMessage().contains("x.field"));
  }

  @Test
  public void decodeMethodReturnStringConcatTypeMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber bad() { return "a" .. "b"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("string concatenation type is not assignable to"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeMethodReturnLogicalNotTypeMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              TextString bad(Boolean a) { return !a; }
            }
            """));

    assertTrue(thrown.getMessage().contains("not assignable to"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeValueExpressionWithFieldAccessAsValueCreatesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber x <- 1;
          WholeNumber y <- 2;
          void swap() { WholeNumber temp <- x; x <- y; y <- temp; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(3, method.body.getValue().statements.size());
    // temp <- x  (field access as value expression)
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(decl.initializer.getValue() instanceof FieldAccess);
    // x <- y  (field access as RHS of assignment)
    ExpressionStatement stmt1 = (ExpressionStatement) method.body.getValue().statements.get(1);
    AssignmentExpression assign1 = (AssignmentExpression) stmt1.expression.getValue();
    assertTrue(assign1.rightHandSide.getValue() instanceof FieldAccess);
    // y <- temp  (local access as RHS)
    ExpressionStatement stmt2 = (ExpressionStatement) method.body.getValue().statements.get(2);
    AssignmentExpression assign2 = (AssignmentExpression) stmt2.expression.getValue();
    assertTrue(assign2.rightHandSide.getValue() instanceof LocalAccess);
  }

  @Test
  public void decodeStringConcatInConstructorLocalInitializerCreatesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { TextString msg <- "hi" .. " there"; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    assertEquals("msg", decl.local.getValue().getName());
    assertTrue(decl.initializer.getValue() instanceof StringConcatenation);
  }

  @Test
  public void decodeComparisonInConstructorLocalInitializerCreatesRelationalInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType(WholeNumber a, WholeNumber b) { Boolean eq <- a == b; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    assertTrue(decl.initializer.getValue() instanceof RelationalInfixExpression);
    RelationalInfixExpression rel = (RelationalInfixExpression) decl.initializer.getValue();
    assertSame(RelationalInfixExpression.Operator.EQUALS, rel.operator.getValue());
  }

  @Test
  public void decodeLogicalNotInLocalInitializerCreatesLogicalComplement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a) { Boolean notA <- !a; return notA; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertTrue(decl.initializer.getValue() instanceof LogicalComplement);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CROSS-DELEGATE INTEGRATION: scenarios exercising all three delegates
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void decodeClassWithFieldsMethodsAndConstructorExercisesAllDelegates() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          TextString label <- "default";
          Boolean active <- true;

          SyntheticType(WholeNumber n) { count <- n; }

          WholeNumber getCount() { return count; }

          void setLabel(TextString s) { label <- s; }

          Boolean isActive() { return active; }
        }
        """);

    // Fields (FieldDecoder territory)
    assertEquals(3, type.getDeclaredFields().size());
    assertIntegerLiteral(type.getDeclaredFields().get(0).initializer.getValue(), 0);
    assertTrue(type.getDeclaredFields().get(1).initializer.getValue() instanceof StringLiteral);
    assertTrue(type.getDeclaredFields().get(2).initializer.getValue() instanceof BooleanLiteral);

    // Constructor (StatementDecoder territory)
    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());

    // Methods (StatementDecoder + ExpressionDecoder territory)
    assertEquals(3, type.getDeclaredMethods().size());
    UserMethod getCount = type.getDeclaredMethods().get(0);
    assertEquals("getCount", getCount.getName());
    ReturnStatement ret = (ReturnStatement) getCount.body.getValue().statements.get(0);
    assertTrue(ret.expression.getValue() instanceof FieldAccess);

    UserMethod setLabel = type.getDeclaredMethods().get(1);
    assertEquals("setLabel", setLabel.getName());
    assertEquals(1, setLabel.body.getValue().statements.size());

    UserMethod isActive = type.getDeclaredMethods().get(2);
    assertEquals("isActive", isActive.getName());
    ReturnStatement activeRet = (ReturnStatement) isActive.body.getValue().statements.get(0);
    assertTrue(activeRet.expression.getValue() instanceof FieldAccess);
  }

  @Test
  public void decodeClassWithArithmeticFieldsThenMethodUsingFieldsExercisesCrossDelegateAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber width <- 3 + 2;
          WholeNumber height <- 10 / 2;
          WholeNumber getWidth() { return width; }
          void setWidth(WholeNumber w) { width <- w; }
        }
        """);

    // Field initializers (FieldDecoder + ExpressionDecoder)
    UserField widthField = type.getDeclaredFields().get(0);
    assertTrue(widthField.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression widthInit = (ArithmeticInfixExpression) widthField.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.PLUS, widthInit.operator.getValue());

    UserField heightField = type.getDeclaredFields().get(1);
    assertTrue(heightField.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression heightInit = (ArithmeticInfixExpression) heightField.initializer.getValue();
    assertSame(ArithmeticInfixExpression.Operator.INTEGER_DIVIDE, heightInit.operator.getValue());

    // Method return referencing field (StatementDecoder + ExpressionDecoder)
    UserMethod getter = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) getter.body.getValue().statements.get(0);
    FieldAccess access = (FieldAccess) ret.expression.getValue();
    assertSame(widthField, access.field.getValue());

    // Method assignment from parameter to field (StatementDecoder + ExpressionDecoder)
    UserMethod setter = type.getDeclaredMethods().get(1);
    ExpressionStatement stmt = (ExpressionStatement) setter.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(widthField, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof ParameterAccess);
  }

  @Test
  public void decodeClassWithControlFlowReferencingFieldsExercisesAllDelegates() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          Boolean done <- false;

          void process(Boolean flag, WholeNumber n) {
            if (flag) { count <- n; }
            while (!done) { done <- true; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.body.getValue().statements.size());

    // If statement with parameter condition and field assignment
    ConditionalStatement conditional = (ConditionalStatement) method.body.getValue().statements.get(0);
    assertTrue(conditional.booleanExpressionBodyPairs.get(0).expression.getValue()
        instanceof ParameterAccess);
    ExpressionStatement thenStmt = (ExpressionStatement) conditional.booleanExpressionBodyPairs
        .get(0).body.getValue().statements.get(0);
    AssignmentExpression thenAssign = (AssignmentExpression) thenStmt.expression.getValue();
    assertSame(type.getDeclaredFields().get(0),
        ((FieldAccess) thenAssign.leftHandSide.getValue()).field.getValue());

    // While loop with logical not condition and boolean field assignment
    WhileLoop loop = (WhileLoop) method.body.getValue().statements.get(1);
    assertTrue(loop.conditional.getValue() instanceof LogicalComplement);
    assertEquals(1, loop.body.getValue().statements.size());
  }

  @Test
  public void decodeClassWithMultipleConstructorsPreservesAll() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { }
          SyntheticType(WholeNumber n) { count <- n; }
        }
        """);

    assertEquals(2, type.getDeclaredConstructors().size());
    NamedUserConstructor emptyConstructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertTrue(emptyConstructor.body.getValue().statements.isEmpty());
    assertTrue(emptyConstructor.getRequiredParameters().isEmpty());

    NamedUserConstructor paramConstructor = (NamedUserConstructor) type.getDeclaredConstructors().get(1);
    assertEquals(1, paramConstructor.getRequiredParameters().size());
    assertEquals("n", paramConstructor.getRequiredParameters().get(0).getName());
    assertEquals(1, paramConstructor.body.getValue().statements.size());
  }

  @Test
  public void decodeClassWithMethodCallingAnotherMethodInConstructorAndMethodBody() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { this.init(); }
          void init() { count <- 1; }
          void reset() { this.init(); }
        }
        """);

    // Constructor calls init
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    ExpressionStatement constructorCall = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    MethodInvocation constructorInvocation = (MethodInvocation) constructorCall.expression.getValue();
    UserMethod init = type.getDeclaredMethods().get(0);
    assertSame(init, constructorInvocation.method.getValue());

    // reset() calls init
    UserMethod reset = type.getDeclaredMethods().get(1);
    assertEquals(1, reset.body.getValue().statements.size());
    ExpressionStatement resetCall = (ExpressionStatement) reset.body.getValue().statements.get(0);
    MethodInvocation resetInvocation = (MethodInvocation) resetCall.expression.getValue();
    assertSame(init, resetInvocation.method.getValue());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR MESSAGE PRESERVATION: exact error text pinning for refactoring safety
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void unsupportedTypeErrorIncludesUsageContext() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType extends MissingSuper {}"));

    assertTrue(thrown.getMessage().contains("Unsupported Tweedle superclass"));
    assertTrue(thrown.getMessage().contains("MissingSuper"));
  }

  @Test
  public void unsupportedFieldTypeErrorIncludesTypeName() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { UnknownType field <- null; }"));

    assertTrue(thrown.getMessage().contains("Unsupported Tweedle field"));
    assertTrue(thrown.getMessage().contains("UnknownType"));
  }

  @Test
  public void unsupportedMethodParameterTypeErrorIncludesTypeName() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { void bad(UnknownType x) { } }"));

    assertTrue(thrown.getMessage().contains("Unsupported Tweedle method parameter"));
    assertTrue(thrown.getMessage().contains("UnknownType"));
  }

  @Test
  public void unsupportedMethodReturnTypeErrorIncludesTypeName() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { UnknownType bad() { } }"));

    assertTrue(thrown.getMessage().contains("Unsupported Tweedle method return"));
    assertTrue(thrown.getMessage().contains("UnknownType"));
  }

  @Test
  public void unsupportedConstructorParameterTypeErrorIncludesTypeName() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { SyntheticType(UnknownType x) { } }"));

    assertTrue(thrown.getMessage().contains("Unsupported Tweedle constructor parameter"));
    assertTrue(thrown.getMessage().contains("UnknownType"));
  }

  @Test
  public void unsupportedLocalVariableTypeErrorIncludesTypeName() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad() { UnknownType x <- null; }
            }
            """));

    assertTrue(thrown.getMessage().contains("Unsupported Tweedle local variable"));
    assertTrue(thrown.getMessage().contains("UnknownType"));
  }

  @Test
  public void malformedInputWithImportReportsOnlyClassDeclarationsSupported() {
    // Import syntax parses but result is not a class — decoder rejects it
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("import SomeModule; class SyntheticType {}"));

    assertTrue(thrown.getMessage().contains("Only Tweedle class declarations"));
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

  private NamedUserType decodeUserType(String source, Set<NamedUserType> terminals) throws Exception {
    AbstractNode decoded = coder.decode(source, Set.copyOf(terminals));
    assertTrue("Expected NamedUserType, got: " + decoded.getClass().getSimpleName(),
        decoded instanceof NamedUserType);
    return (NamedUserType) decoded;
  }

  private NamedUserType userTypeNamed(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    return type;
  }

  private static void assertIntegerLiteral(Expression expression, int expectedValue) {
    assertTrue("Expected IntegerLiteral, got: " + expression.getClass().getSimpleName(),
        expression instanceof IntegerLiteral);
    assertEquals(expectedValue, ((IntegerLiteral) expression).value.getValue().intValue());
  }
}
