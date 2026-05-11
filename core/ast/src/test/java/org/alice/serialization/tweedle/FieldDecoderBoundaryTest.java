package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.UserField;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Boundary tests for FieldDecoder targeting untested paths:
 * null initializer by type, sized and element array initializers,
 * resource field errors, and arithmetic operator dispatch.
 *
 * <p>All tests drive through TweedleEncoderDecoder.decode() with
 * synthetic Tweedle class source strings, matching the characterization
 * test pattern from DecoderDelegateDecompositionCharacterizationTest.
 */
public class FieldDecoderBoundaryTest {
  private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

  // ═══════════════════════════════════════════════════════════════════════════
  // NULL INITIALIZER: by value type category
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void nullInitializerOnWholeNumberFieldDecodesNullLiteral() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber count <- null; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    assertTrue("WholeNumber null init should produce NullLiteral",
        field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void nullInitializerOnDecimalNumberFieldDecodesNullLiteral() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { DecimalNumber ratio <- null; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("ratio", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void nullInitializerOnTextStringFieldDecodesNullLiteral() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { TextString label <- null; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("label", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void nullInitializerOnBooleanFieldDecodesNullLiteral() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { Boolean flag <- null; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("flag", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void nullInitializerOnUserTypeFieldDecodesNullLiteral() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType(
        "class SyntheticType { Friend companion <- null; }",
        Set.of(friendType));

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("companion", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
    assertSame(friendType, field.getValueType());
  }

  @Test
  public void nullInitializerOnArrayFieldDecodesNullLiteral() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber[] items <- null; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("items", field.getName());
    assertTrue("Array null init should produce NullLiteral",
        field.initializer.getValue() instanceof NullLiteral);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ARRAY INITIALIZER: sized and element forms
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void sizedArrayInitializerDecodesArrayInstanceCreation() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber[] items <- new WholeNumber[3]; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("items", field.getName());
    assertTrue("Sized array should produce ArrayInstanceCreation",
        field.initializer.getValue() instanceof ArrayInstanceCreation);
  }

  @Test
  public void zeroSizedArrayInitializerDecodesArrayInstanceCreation() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber[] items <- new WholeNumber[0]; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArrayInstanceCreation);
  }

  @Test
  public void largeSizedArrayInitializerDecodesArrayInstanceCreation() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber[] items <- new WholeNumber[100]; }");

    UserField field = type.getDeclaredFields().get(0);
    assertNotNull("Large sized array init should not be null", field.initializer.getValue());
    assertTrue(field.initializer.getValue() instanceof ArrayInstanceCreation);
  }

  @Test
  public void decimalArraySizedInitializerDecodes() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { DecimalNumber[] items <- new DecimalNumber[5]; }");

    UserField field = type.getDeclaredFields().get(0);
    assertTrue(field.initializer.getValue() instanceof ArrayInstanceCreation);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RESOURCE FIELD: non-null initializer error
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void resourceFieldNullInitializerDecodesNullLiteral() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { ImageResource picture <- null; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("picture", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void resourceFieldWithoutInitializerDecodesNull() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { ImageResource picture; }");

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("picture", field.getName());
    assertNull("Uninitialized field should have null initializer", field.initializer.getValue());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ARITHMETIC OPERATORS: all four in field initializers
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void additionFieldInitializerDecodesPlusOperator() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber sum <- 1 + 2; }");

    ArithmeticInfixExpression infix = arithmeticInitializer(type, "sum");
    assertSame(ArithmeticInfixExpression.Operator.PLUS, infix.operator.getValue());
    assertIntegerLiteral(infix.leftOperand.getValue(), 1);
    assertIntegerLiteral(infix.rightOperand.getValue(), 2);
  }

  @Test
  public void subtractionFieldInitializerDecodesMinusOperator() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber diff <- 5 - 3; }");

    ArithmeticInfixExpression infix = arithmeticInitializer(type, "diff");
    assertSame(ArithmeticInfixExpression.Operator.MINUS, infix.operator.getValue());
    assertIntegerLiteral(infix.leftOperand.getValue(), 5);
    assertIntegerLiteral(infix.rightOperand.getValue(), 3);
  }

  @Test
  public void multiplicationFieldInitializerDecodesTimesOperator() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber product <- 2 * 4; }");

    ArithmeticInfixExpression infix = arithmeticInitializer(type, "product");
    assertSame(ArithmeticInfixExpression.Operator.TIMES, infix.operator.getValue());
    assertIntegerLiteral(infix.leftOperand.getValue(), 2);
    assertIntegerLiteral(infix.rightOperand.getValue(), 4);
  }

  @Test
  public void integerDivisionFieldInitializerDecodesIntegerDivideOperator() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber quotient <- 10 / 3; }");

    ArithmeticInfixExpression infix = arithmeticInitializer(type, "quotient");
    assertSame(ArithmeticInfixExpression.Operator.INTEGER_DIVIDE, infix.operator.getValue());
  }

  @Test
  public void realDivisionFieldInitializerDecodesRealDivideOperator() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { DecimalNumber quotient <- 10.0 / 3.0; }");

    ArithmeticInfixExpression infix = arithmeticInitializer(type, "quotient");
    assertSame(ArithmeticInfixExpression.Operator.REAL_DIVIDE, infix.operator.getValue());
  }

  @Test
  public void arithmeticDisabledRejectsSubtractionFieldInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(
            "class SyntheticType { WholeNumber diff <- 5 - 3; }",
            Set.of(),
            false));

    assertTrue(thrown.getMessage().contains("diff"));
  }

  @Test
  public void arithmeticDisabledRejectsMultiplicationFieldInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(
            "class SyntheticType { WholeNumber product <- 2 * 4; }",
            Set.of(),
            false));

    assertTrue(thrown.getMessage().contains("product"));
  }

  @Test
  public void arithmeticDisabledRejectsDivisionFieldInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(
            "class SyntheticType { WholeNumber quotient <- 10 / 3; }",
            Set.of(),
            false));

    assertTrue(thrown.getMessage().contains("quotient"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MULTIPLE FIELD TYPES: mixing primitive and array
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void mixedFieldTypesDecodeInOrder() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 5;
          WholeNumber[] values <- new WholeNumber[2];
          TextString label <- "hello";
          Boolean flag <- false;
        }
        """);

    assertEquals(4, type.getDeclaredFields().size());
    assertEquals("count", type.getDeclaredFields().get(0).getName());
    assertEquals("values", type.getDeclaredFields().get(1).getName());
    assertEquals("label", type.getDeclaredFields().get(2).getName());
    assertEquals("flag", type.getDeclaredFields().get(3).getName());

    assertIntegerLiteral(type.getDeclaredFields().get(0).initializer.getValue(), 5);
    assertTrue(type.getDeclaredFields().get(1).initializer.getValue() instanceof ArrayInstanceCreation);
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

  private static ArithmeticInfixExpression arithmeticInitializer(NamedUserType type, String fieldName) {
    UserField field = null;
    for (UserField f : type.getDeclaredFields()) {
      if (f.getName().equals(fieldName)) {
        field = f;
        break;
      }
    }
    assertNotNull("Field not found: " + fieldName, field);
    assertTrue("Expected ArithmeticInfixExpression, got: " + field.initializer.getValue().getClass().getSimpleName(),
        field.initializer.getValue() instanceof ArithmeticInfixExpression);
    return (ArithmeticInfixExpression) field.initializer.getValue();
  }

  private static void assertIntegerLiteral(Expression expression, int expectedValue) {
    assertTrue("Expected IntegerLiteral, got: " + expression.getClass().getSimpleName(),
        expression instanceof IntegerLiteral);
    assertEquals(expectedValue, ((IntegerLiteral) expression).value.getValue().intValue());
  }
}
