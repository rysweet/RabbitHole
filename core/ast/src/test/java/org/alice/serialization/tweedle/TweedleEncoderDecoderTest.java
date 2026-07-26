package org.alice.serialization.tweedle;

/** Audit note: this characterization-heavy test is ~2395 LOC and should be split into focused suites in a future refactoring. */

import org.junit.Test;
import org.lgna.common.resources.AudioResource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.ArrayInstanceCreation;
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
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserArrayType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.WhileLoop;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TweedleEncoderDecoderTest {
  private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

  @Test
  public void decodeEmptyClassCreatesNamedUserType() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType {}");

    assertEquals("SyntheticType", type.getName());
    assertNull(type.getSuperType());
    assertTrue(type.getDeclaredFields().isEmpty());
    assertTrue(type.getDeclaredMethods().isEmpty());
    assertTrue(type.getDeclaredConstructors().isEmpty());
  }

  @Test
  public void decodeSupportedJavaLangSuperclassResolvesJavaType() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType extends String {}");

    assertEquals("SyntheticType", type.getName());
    assertSame(JavaType.getInstance(String.class), type.getSuperType());
  }

  @Test
  public void decodeUnknownSuperclassReportsUnsupportedTweedle() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType extends MissingSuper {}"));

    assertTrue(thrown.getMessage().contains("MissingSuper"));
  }

  @Test
  public void decodeMalformedSuperclassReportsMalformedTweedle() {
    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> coder.decode("class SyntheticType extends {}"));

    assertTrue(thrown.getMessage().contains("Unable to parse Tweedle type"));
  }

  @Test
  public void decodeClassWithFieldCreatesUserField() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber count; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    assertSame(JavaType.getInstance(Integer.class), field.getValueType());
  }

  @Test
  public void decodeClassWithPrimitiveInitializedFieldsCreatesLiteralInitializers() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 1;
          DecimalNumber distance <- 2.5;
          TextString label <- "hello";
          Boolean enabled <- true;
        }
        """);

    assertEquals(4, type.getDeclaredFields().size());
    assertIntegerInitializer(type.getDeclaredFields().get(0), "count", 1);
    assertDoubleInitializer(type.getDeclaredFields().get(1), "distance", 2.5);
    assertStringInitializer(type.getDeclaredFields().get(2), "label", "hello");
    assertBooleanInitializer(type.getDeclaredFields().get(3), "enabled", true);
  }

  @Test
  public void decodeClassWithLiteralArithmeticInitializedFieldCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber count <- 1 + 2; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    assertSame(JavaType.getInstance(Integer.class), field.getValueType());
    assertArithmeticInfix(field.initializer.getValue(),
        ArithmeticInfixExpression.Operator.PLUS, JavaType.getInstance(Integer.class), 1, 2);
  }

  @Test
  public void decodeClassWithIdentifierInitializedFieldReportsUnsupportedInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count <- seed; }"));

    assertTrue(thrown.getMessage().contains("initializers"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithMixedIdentifierArithmeticInitializedFieldReportsUnsupportedInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber seed <- 1; WholeNumber count <- seed + 2; }"));

    assertTrue(thrown.getMessage().contains("initializers"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithThisFieldAccessInitializedFieldReportsUnsupportedBoundary() {
    assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber seed <- 1; WholeNumber count <- this.seed; }"));
  }

  @Test
  public void decodeClassWithMethodCallInitializedFieldReportsUnsupportedBoundary() {
    assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- this.getCount();
              WholeNumber getCount() { return 1; }
            }
            """));
  }

  @Test
  public void decodeClassWithNullInitializedFieldCreatesNullLiteralInitializer() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { TextString label <- null; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("label", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void decodeClassWithResourceNullInitializedFieldCreatesNullLiteralInitializer() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { ImageResource picture <- null; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("picture", field.getName());
    assertSame(JavaType.getInstance(ImageResource.class), field.getValueType());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void decodeClassWithAudioResourceNullInitializedFieldCreatesNullLiteralInitializer() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { AudioResource sound <- null; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("sound", field.getName());
    assertSame(JavaType.getInstance(AudioResource.class), field.getValueType());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void decodeClassWithResourceIdentifierInitializedFieldReportsUnsupportedBoundary() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { ImageResource picture <- someImage; }"));

    assertUnsupportedResourceFieldInitializer(thrown, "picture");
  }

  @Test
  public void decodeClassWithAudioResourceIdentifierInitializedFieldReportsUnsupportedBoundary() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { AudioResource sound <- sound0; }"));

    assertUnsupportedResourceFieldInitializer(thrown, "sound");
    assertTrue(thrown.getMessage().contains("AudioResource"));
  }

  @Test
  public void decodeClassWithTerminalUserTypeNullInitializedFieldCreatesNullLiteralInitializer() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType(
        "class SyntheticType { Friend companion <- null; }",
        Set.of(friendType));

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("companion", field.getName());
    assertSame(friendType, field.getValueType());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void decodeClassWithJavaArrayNullInitializedFieldCreatesNullLiteralInitializer() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber[] counts <- null; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("counts", field.getName());
    assertSame(JavaType.getInstance(Integer[].class), field.getValueType());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void decodeClassWithTerminalUserArrayNullInitializedFieldCreatesNullLiteralInitializer() throws Exception {
    NamedUserType friendType = userTypeNamed("Friend");
    NamedUserType type = decodeUserType(
        "class SyntheticType { Friend[] companions <- null; }",
        Set.of(friendType));

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("companions", field.getName());
    assertSame(UserArrayType.getInstance(friendType, 1), field.getValueType());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  @Test
  public void decodeClassWithWholeNumberArrayInitializerCreatesArrayInstanceCreation() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber[] counts <- new WholeNumber[] {1, 2}; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("counts", field.getName());
    assertSame(JavaType.getInstance(Integer[].class), field.getValueType());
    Expression initializer = field.initializer.getValue();
    assertTrue(initializer instanceof ArrayInstanceCreation);
    ArrayInstanceCreation array = (ArrayInstanceCreation) initializer;
    assertSame(JavaType.getInstance(Integer[].class), array.arrayType.getValue());
    assertEquals(1, array.lengths.size());
    assertEquals(Integer.valueOf(2), array.lengths.get(0));
    assertEquals(2, array.expressions.size());
    assertIntegerLiteral(array.expressions.get(0), 1);
    assertIntegerLiteral(array.expressions.get(1), 2);
  }

  @Test
  public void decodeClassWithSizedWholeNumberArrayInitializerCreatesArrayInstanceCreation() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber[] counts <- new WholeNumber[2]; }");

    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals("counts", field.getName());
    assertSame(JavaType.getInstance(Integer[].class), field.getValueType());
    Expression initializer = field.initializer.getValue();
    assertTrue(initializer instanceof ArrayInstanceCreation);
    ArrayInstanceCreation array = (ArrayInstanceCreation) initializer;
    assertSame(JavaType.getInstance(Integer[].class), array.arrayType.getValue());
    assertEquals(1, array.lengths.size());
    assertEquals(Integer.valueOf(2), array.lengths.get(0));
    assertTrue(array.expressions.isEmpty());
  }

  @Test
  public void decodeClassWithNonLiteralArrayInitializerElementReportsUnsupportedInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber[] counts <- new WholeNumber[] {1, 1 + 2}; }"));

    assertTrue(thrown.getMessage().contains("array initializer elements"));
    assertTrue(thrown.getMessage().contains("counts"));
  }

  @Test
  public void decodeClassWithNonLiteralArrayInitializerSizeReportsUnsupportedInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber[] counts <- new WholeNumber[1 + 1]; }"));

    assertTrue(thrown.getMessage().contains("array initializer sizes"));
    assertTrue(thrown.getMessage().contains("counts"));
  }

  @Test
  public void decodeClassWithNumericAndBooleanNullInitializedFieldsCreatesNullLiteralInitializers() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- null;
          DecimalNumber distance <- null;
          Number amount <- null;
          Boolean enabled <- null;
        }
        """);

    assertEquals(4, type.getDeclaredFields().size());
    assertNullInitializer(type.getDeclaredFields().get(0), "count");
    assertNullInitializer(type.getDeclaredFields().get(1), "distance");
    assertNullInitializer(type.getDeclaredFields().get(2), "amount");
    assertNullInitializer(type.getDeclaredFields().get(3), "enabled");
  }

  @Test
  public void decodeClassWithEmptyVoidMethodCreatesUserMethod() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { void initialize() { } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("initialize", method.getName());
    assertSame(JavaType.VOID_TYPE, method.getReturnType());
    assertTrue(method.getRequiredParameters().isEmpty());
  }

  @Test
  public void decodeStaticMethodPreservesStaticModifier() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { static void main() { } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("main", method.getName());
    assertTrue("static modifier must survive Tweedle decode", method.isStatic());
  }

  @Test
  public void decodeInstanceMethodRemainsNonStatic() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { void initialize() { } }");

    UserMethod method = type.getDeclaredMethods().get(0);
    assertFalse("instance method must not gain a static modifier on decode", method.isStatic());
  }

  @Test
  public void decodeClassWithNonEmptyMethodReportsUnsupportedMethodBody() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count() { WholeNumber local <- 1; } }"));

    assertTrue(thrown.getMessage().contains("method bodies"));
  }

  @Test
  public void decodeClassWithPrimitiveReturnMethodBodyCreatesReturnStatement() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber count() { return 1; } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("count", method.getName());
    assertSame(JavaType.getInstance(Integer.class), method.getReturnType());
    assertTrue(method.getRequiredParameters().isEmpty());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertIntegerLiteral(returnStatement.expression.getValue(), 1);
  }

  @Test
  public void decodeClassWithParameterReturnMethodBodyCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { WholeNumber count(WholeNumber value) { return value; } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("count", method.getName());
    assertEquals(1, method.getRequiredParameters().size());
    UserParameter parameter = method.getRequiredParameters().get(0);
    assertEquals("value", parameter.getName());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertTrue(returnStatement.expression.getValue() instanceof ParameterAccess);
    ParameterAccess access = (ParameterAccess) returnStatement.expression.getValue();
    assertSame(parameter, access.parameter.getValue());
  }

  @Test
  public void decodeClassWithNonLiteralReturnMethodBodyReportsUnsupportedReturnExpression() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count() { return 1 + 2; } }"));

    assertTrue(thrown.getMessage().contains("method return expressions"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithUnknownIdentifierReturnMethodBodyReportsUnsupportedReturnIdentifier() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count(WholeNumber value) { return missing; } }"));

    assertTrue(thrown.getMessage().contains("method return identifiers"));
    assertTrue(thrown.getMessage().contains("missing"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithFieldReturnMethodBodyCreatesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 7;
          WholeNumber getCount() { return count; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("getCount", method.getName());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertTrue(returnStatement.expression.getValue() instanceof FieldAccess);
    FieldAccess access = (FieldAccess) returnStatement.expression.getValue();
    assertSame(field, access.field.getValue());
  }

  @Test
  public void decodeClassWithThisFieldReturnMethodBodyCreatesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 7;
          WholeNumber getCount() { return this.count; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("getCount", method.getName());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertTrue(returnStatement.expression.getValue() instanceof FieldAccess);
    FieldAccess access = (FieldAccess) returnStatement.expression.getValue();
    assertSame(field, access.field.getValue());
  }

  @Test
  public void decodeClassWithParameterMemberReturnMethodBodyReportsUnsupportedMemberExpression() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber getCount(WholeNumber value) { return value.count; }
            }
            """));

    assertTrue(thrown.getMessage().contains("method return member expressions"));
    assertTrue(thrown.getMessage().contains("getCount"));
    assertTrue(thrown.getMessage().contains("value.count"));
  }

  @Test
  public void decodeClassWithMismatchedFieldReturnMethodBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 7;
              TextString getCount() { return count; }
            }
            """));

    assertTrue(thrown.getMessage().contains("return identifier type is not assignable"));
    assertTrue(thrown.getMessage().contains("getCount"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithMismatchedParameterReturnMethodBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { TextString label(WholeNumber value) { return value; } }"));

    assertTrue(thrown.getMessage().contains("return identifier type is not assignable"));
    assertTrue(thrown.getMessage().contains("label"));
    assertTrue(thrown.getMessage().contains("value"));
  }

  @Test
  public void decodeClassWithLiteralLocalThenReturnLocalCreatesLocalAccess() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber count() { WholeNumber local <- 1; return local; } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("count", method.getName());
    assertEquals(2, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    LocalDeclarationStatement localStatement =
        (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    UserLocal local = localStatement.local.getValue();
    assertEquals("local", local.getName());
    assertSame(JavaType.getInstance(Integer.class), local.getValueType());
    assertIntegerLiteral(localStatement.initializer.getValue(), 1);

    assertTrue(method.body.getValue().statements.get(1) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(1);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertTrue(returnStatement.expression.getValue() instanceof LocalAccess);
    LocalAccess access = (LocalAccess) returnStatement.expression.getValue();
    assertSame(local, access.local.getValue());
  }

  @Test
  public void decodeClassWithAdditionLocalInitializerInMethodBodyCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber count() { WholeNumber local <- 1 + 2; return local; } }");

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertEquals("local", decl.local.getValue().getName());
    assertArithmeticInfix(decl.initializer.getValue(),
        ArithmeticInfixExpression.Operator.PLUS, JavaType.getInstance(Integer.class), 1, 2);
  }

  @Test
  public void decodeClassWithParameterIdentifierLocalInitializerInMethodBodyCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber doubleIt(WholeNumber val) { WholeNumber copy <- val; return copy; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    UserParameter param = method.getRequiredParameters().get(0);
    assertEquals("val", param.getName());
    assertEquals(2, method.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    UserLocal local = decl.local.getValue();
    assertEquals("copy", local.getName());
    assertSame(JavaType.getInstance(Integer.class), local.getValueType());
    assertTrue(decl.initializer.getValue() instanceof ParameterAccess);
    assertSame(param, ((ParameterAccess) decl.initializer.getValue()).parameter.getValue());
  }

  @Test
  public void decodeClassWithLocalIdentifierLocalInitializerInMethodBodyCreatesLocalAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void update() { WholeNumber x <- 1; WholeNumber y <- x; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.body.getValue().statements.size());
    LocalDeclarationStatement xDecl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    UserLocal xLocal = xDecl.local.getValue();
    assertEquals("x", xLocal.getName());
    LocalDeclarationStatement yDecl = (LocalDeclarationStatement) method.body.getValue().statements.get(1);
    UserLocal yLocal = yDecl.local.getValue();
    assertEquals("y", yLocal.getName());
    assertTrue(yDecl.initializer.getValue() instanceof LocalAccess);
    assertSame(xLocal, ((LocalAccess) yDecl.initializer.getValue()).local.getValue());
  }

  @Test
  public void decodeClassWithFieldIdentifierLocalInitializerInMethodBodyCreatesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          WholeNumber getCount() { WholeNumber result <- count; return result; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    UserLocal local = decl.local.getValue();
    assertEquals("result", local.getName());
    assertTrue(decl.initializer.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) decl.initializer.getValue()).field.getValue());
  }

  @Test
  public void decodeClassWithUnknownIdentifierLocalInitializerInMethodBodyReportsUnknownIdentifier() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void update() { WholeNumber x <- missing; }
            }
            """));

    assertTrue(thrown.getMessage().contains("value expression identifier is not a known local, parameter, or field"));
    assertTrue(thrown.getMessage().contains("update.missing"));
    assertTrue(thrown.getMessage().contains("missing"));
  }

  @Test
  public void decodeClassWithTypeMismatchIdentifierLocalInitializerInMethodBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              TextString label <- "hi";
              void update() { WholeNumber x <- label; }
            }
            """));

    assertTrue(thrown.getMessage().contains("local variable initializer type is not assignable to"));
    assertTrue(thrown.getMessage().contains("update.x"));
  }

  @Test
  public void decodeClassWithRequiredMethodParameterCreatesUserMethodParameter() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { void initialize(WholeNumber count) { } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("initialize", method.getName());
    assertSame(JavaType.VOID_TYPE, method.getReturnType());
    assertEquals(1, method.getRequiredParameters().size());
    UserParameter parameter = method.getRequiredParameters().get(0);
    assertEquals("count", parameter.getName());
    assertSame(JavaType.getInstance(Integer.class), parameter.getValueType());
  }

  @Test
  public void decodeClassWithOptionalMethodParameterCreatesUserMethodParameter() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { void initialize(WholeNumber count <- 1) { } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("initialize", method.getName());
    assertEquals(1, method.getRequiredParameters().size());
    UserParameter parameter = method.getRequiredParameters().get(0);
    assertEquals("count", parameter.getName());
    assertSame(JavaType.getInstance(Integer.class), parameter.getValueType());
  }

  @Test
  public void decodeClassWithRequiredAndOptionalMethodParametersCreatesAllUserMethodParameters() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { void initialize(WholeNumber x, WholeNumber y <- 2) { } }");

    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.getRequiredParameters().size());
    assertEquals("x", method.getRequiredParameters().get(0).getName());
    assertEquals("y", method.getRequiredParameters().get(1).getName());
    assertSame(JavaType.getInstance(Integer.class), method.getRequiredParameters().get(1).getValueType());
  }

  @Test
  public void decodeClassWithOptionalMethodParameterInReturnCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { WholeNumber getCount(WholeNumber count <- 0) { return count; } }");

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.getRequiredParameters().size());
    UserParameter parameter = method.getRequiredParameters().get(0);
    assertEquals("count", parameter.getName());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertTrue(returnStatement.expression.getValue() instanceof ParameterAccess);
    ParameterAccess access = (ParameterAccess) returnStatement.expression.getValue();
    assertSame(parameter, access.parameter.getValue());
  }

  @Test
  public void decodeClassWithNonVoidEmptyMethodReportsUnsupportedReturnValues() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count() { } }"));

    assertTrue(thrown.getMessage().contains("method return values"));
  }

  @Test
  public void decodeClassWithEmptyConstructorCreatesNamedUserConstructor() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { SyntheticType() { } }");

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertTrue(constructor.getRequiredParameters().isEmpty());
    assertTrue(constructor.body.getValue().statements.isEmpty());
  }

  @Test
  public void decodeClassWithRequiredConstructorParameterCreatesNamedUserConstructorParameter() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { SyntheticType(WholeNumber count) { } }");

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.getRequiredParameters().size());
    UserParameter parameter = constructor.getRequiredParameters().get(0);
    assertEquals("count", parameter.getName());
    assertSame(JavaType.getInstance(Integer.class), parameter.getValueType());
    assertTrue(constructor.body.getValue().statements.isEmpty());
  }

  @Test
  public void decodeClassWithLiteralLocalConstructorBodyCreatesLocalDeclaration() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { SyntheticType() { WholeNumber local <- 1; } }");

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    assertTrue(constructor.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    LocalDeclarationStatement localStatement =
        (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    UserLocal local = localStatement.local.getValue();
    assertEquals("local", local.getName());
    assertSame(JavaType.getInstance(Integer.class), local.getValueType());
    assertIntegerLiteral(localStatement.initializer.getValue(), 1);
  }

  @Test
  public void decodeClassWithOptionalConstructorParameterCreatesUserConstructorParameter() throws Exception {
    NamedUserType type = decodeUserType("class SyntheticType { SyntheticType(WholeNumber count <- 1) { } }");

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.getRequiredParameters().size());
    UserParameter parameter = constructor.getRequiredParameters().get(0);
    assertEquals("count", parameter.getName());
    assertSame(JavaType.getInstance(Integer.class), parameter.getValueType());
    assertTrue(constructor.body.getValue().statements.isEmpty());
  }

  @Test
  public void decodeClassWithRequiredAndOptionalConstructorParametersCreatesAllConstructorParameters() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { SyntheticType(WholeNumber x, WholeNumber y <- 2) { } }");

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(2, constructor.getRequiredParameters().size());
    assertEquals("x", constructor.getRequiredParameters().get(0).getName());
    assertEquals("y", constructor.getRequiredParameters().get(1).getName());
    assertSame(JavaType.getInstance(Integer.class), constructor.getRequiredParameters().get(1).getValueType());
  }

  @Test
  public void decodeClassWithAdditionLocalInitializerInConstructorBodyCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { SyntheticType() { WholeNumber local <- 1 + 2; } }");

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    assertEquals("local", decl.local.getValue().getName());
    assertArithmeticInfix(decl.initializer.getValue(),
        ArithmeticInfixExpression.Operator.PLUS, JavaType.getInstance(Integer.class), 1, 2);
  }

  @Test
  public void decodeClassWithParameterIdentifierLocalInitializerInConstructorBodyCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType(WholeNumber val) { WholeNumber copy <- val; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    UserParameter param = constructor.getRequiredParameters().get(0);
    assertEquals("val", param.getName());
    assertEquals(1, constructor.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    UserLocal local = decl.local.getValue();
    assertEquals("copy", local.getName());
    assertSame(JavaType.getInstance(Integer.class), local.getValueType());
    assertTrue(decl.initializer.getValue() instanceof ParameterAccess);
    assertSame(param, ((ParameterAccess) decl.initializer.getValue()).parameter.getValue());
  }

  @Test
  public void decodeClassWithLocalIdentifierLocalInitializerInConstructorBodyCreatesLocalAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { WholeNumber x <- 1; WholeNumber y <- x; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(2, constructor.body.getValue().statements.size());
    LocalDeclarationStatement xDecl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    UserLocal xLocal = xDecl.local.getValue();
    assertEquals("x", xLocal.getName());
    LocalDeclarationStatement yDecl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(1);
    assertEquals("y", yDecl.local.getValue().getName());
    assertTrue(yDecl.initializer.getValue() instanceof LocalAccess);
    assertSame(xLocal, ((LocalAccess) yDecl.initializer.getValue()).local.getValue());
  }

  @Test
  public void decodeClassWithFieldIdentifierLocalInitializerInConstructorBodyCreatesFieldAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { WholeNumber snap <- count; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    assertEquals("snap", decl.local.getValue().getName());
    assertTrue(decl.initializer.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) decl.initializer.getValue()).field.getValue());
  }

  @Test
  public void decodeClassWithUnknownIdentifierLocalInitializerInConstructorBodyReportsUnknownIdentifier() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { SyntheticType() { WholeNumber local <- missing; } }"));

    assertTrue(thrown.getMessage().contains("value expression identifier is not a known local, parameter, or field"));
    assertTrue(thrown.getMessage().contains("SyntheticType.missing"));
    assertTrue(thrown.getMessage().contains("missing"));
  }

  @Test
  public void decodeClassWithUnknownFieldAssignmentInConstructorBodyReportsUnknownTarget() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { SyntheticType() { unknown <- 1; } }"));

    assertTrue(thrown.getMessage().contains("assignment target is not a known local or field"));
    assertTrue(thrown.getMessage().contains("unknown"));
    assertTrue(thrown.getMessage().contains("SyntheticType"));
  }

  @Test
  public void decodeClassWithPrimitiveLiteralFieldAssignmentInConstructorBodyCreatesAssignmentStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { count <- 7; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    assertTrue(constructor.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(AssignmentExpression.Operator.ASSIGN, assign.operator.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof IntegerLiteral);
    assertEquals(7, ((IntegerLiteral) assign.rightHandSide.getValue()).value.getValue().intValue());
  }

  @Test
  public void decodeClassWithThisFieldAssignmentInConstructorBodyCreatesAssignmentStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { this.count <- 7; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    assertTrue(constructor.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.rightHandSide.getValue() instanceof IntegerLiteral);
    assertEquals(7, ((IntegerLiteral) assign.rightHandSide.getValue()).value.getValue().intValue());
  }

  @Test
  public void decodeClassWithAdditionRhsInConstructorAssignmentCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { count <- 1 + 2; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertArithmeticInfix(assign.rightHandSide.getValue(),
        ArithmeticInfixExpression.Operator.PLUS, JavaType.getInstance(Integer.class), 1, 2);
  }

  @Test
  public void decodeClassWithTypeMismatchFieldAssignmentInConstructorBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              SyntheticType() { count <- "hello"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("value type is not assignable to"));
    assertTrue(thrown.getMessage().contains("SyntheticType"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithFieldAssignmentInVoidMethodBodyCreatesAssignmentStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void setCount() { count <- 7; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("setCount", method.getName());
    assertSame(JavaType.VOID_TYPE, method.getReturnType());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(AssignmentExpression.Operator.ASSIGN, assign.operator.getValue());
    assertTrue(assign.leftHandSide.getValue() instanceof FieldAccess);
    FieldAccess lhs = (FieldAccess) assign.leftHandSide.getValue();
    assertSame(field, lhs.field.getValue());
    assertIntegerLiteral(assign.rightHandSide.getValue(), 7);
  }

  @Test
  public void decodeClassWithThisFieldAssignmentInVoidMethodBodyCreatesAssignmentStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void setCount() { this.count <- 7; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("setCount", method.getName());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.leftHandSide.getValue() instanceof FieldAccess);
    FieldAccess lhs = (FieldAccess) assign.leftHandSide.getValue();
    assertSame(field, lhs.field.getValue());
    assertIntegerLiteral(assign.rightHandSide.getValue(), 7);
  }

  @Test
  public void decodeClassWithFieldAssignmentThenReturnInMethodBodyCreatesOrderedStatements() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          WholeNumber update() { count <- 7; return count; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("update", method.getName());
    assertEquals(2, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertIntegerLiteral(assign.rightHandSide.getValue(), 7);
    assertTrue(method.body.getValue().statements.get(1) instanceof ReturnStatement);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(1);
    assertTrue(ret.expression.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) ret.expression.getValue()).field.getValue());
  }

  @Test
  public void decodeClassWithIntegerAdditionRhsInMethodAssignmentCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void setCount() { count <- 1 + 2; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertArithmeticInfix(assign.rightHandSide.getValue(),
        ArithmeticInfixExpression.Operator.PLUS, JavaType.getInstance(Integer.class), 1, 2);
  }

  @Test
  public void decodeClassWithDecimalSubtractionRhsInMethodAssignmentCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          DecimalNumber dist <- 0.0;
          void update() { dist <- 5.0 - 1.5; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    ArithmeticInfixExpression infix = assertArithmeticInfixOperator(assign.rightHandSide.getValue(),
        ArithmeticInfixExpression.Operator.MINUS, JavaType.getInstance(Double.class));
    assertTrue(infix.leftOperand.getValue() instanceof DoubleLiteral);
    assertEquals(5.0, ((DoubleLiteral) infix.leftOperand.getValue()).value.getValue(), 0.0);
    assertTrue(infix.rightOperand.getValue() instanceof DoubleLiteral);
    assertEquals(1.5, ((DoubleLiteral) infix.rightOperand.getValue()).value.getValue(), 0.0);
  }

  @Test
  public void decodeClassWithIntegerMultiplicationRhsInMethodAssignmentCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber area <- 0;
          void compute() { area <- 3 * 4; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertArithmeticInfix(assign.rightHandSide.getValue(),
        ArithmeticInfixExpression.Operator.TIMES, JavaType.getInstance(Integer.class), 3, 4);
  }

  @Test
  public void decodeClassWithIntegerDivisionRhsInMethodAssignmentCreatesIntegerDivide() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber half <- 0;
          void compute() { half <- 10 / 2; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertArithmeticInfix(assign.rightHandSide.getValue(),
        ArithmeticInfixExpression.Operator.INTEGER_DIVIDE, JavaType.getInstance(Integer.class), 10, 2);
  }

  @Test
  public void decodeClassWithDecimalDivisionRhsInMethodAssignmentCreatesRealDivide() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          DecimalNumber ratio <- 0.0;
          void compute() { ratio <- 10.0 / 3.0; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    ArithmeticInfixExpression infix = assertArithmeticInfixOperator(assign.rightHandSide.getValue(),
        ArithmeticInfixExpression.Operator.REAL_DIVIDE, JavaType.getInstance(Double.class));
    assertTrue(infix.leftOperand.getValue() instanceof DoubleLiteral);
    assertEquals(10.0, ((DoubleLiteral) infix.leftOperand.getValue()).value.getValue(), 0.0);
    assertTrue(infix.rightOperand.getValue() instanceof DoubleLiteral);
    assertEquals(3.0, ((DoubleLiteral) infix.rightOperand.getValue()).value.getValue(), 0.0);
  }

  @Test
  public void decodeLocalInitWithAdditionRhsCreatesArithmeticInfix() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void compute() { WholeNumber x <- 2 + 3; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertEquals("x", decl.local.getValue().getName());
    assertArithmeticInfix(decl.initializer.getValue(),
        ArithmeticInfixExpression.Operator.PLUS, JavaType.getInstance(Integer.class), 2, 3);
  }

  @Test
  public void decodeClassWithStringConcatRhsInMethodAssignmentCreatesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          TextString label <- "";
          void tag() { label <- "hello" .. " world"; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof StringConcatenation);
    StringConcatenation concat = (StringConcatenation) assign.rightHandSide.getValue();
    assertSame(JavaType.STRING_TYPE, concat.getType());
    assertTrue(concat.leftOperand.getValue() instanceof StringLiteral);
    assertEquals("hello", ((StringLiteral) concat.leftOperand.getValue()).value.getValue());
    assertTrue(concat.rightOperand.getValue() instanceof StringLiteral);
    assertEquals(" world", ((StringLiteral) concat.rightOperand.getValue()).value.getValue());
  }

  @Test
  public void decodeClassWithStringConcatLocalInitializerInMethodBodyCreatesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void greet() { TextString msg <- "hi" .. " there"; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertEquals("msg", decl.local.getValue().getName());
    assertSame(JavaType.STRING_TYPE, decl.local.getValue().getValueType());
    assertTrue(decl.initializer.getValue() instanceof StringConcatenation);
    StringConcatenation concat = (StringConcatenation) decl.initializer.getValue();
    assertTrue(concat.leftOperand.getValue() instanceof StringLiteral);
    assertEquals("hi", ((StringLiteral) concat.leftOperand.getValue()).value.getValue());
    assertTrue(concat.rightOperand.getValue() instanceof StringLiteral);
    assertEquals(" there", ((StringLiteral) concat.rightOperand.getValue()).value.getValue());
  }

  @Test
  public void decodeClassWithStringConcatRhsInConstructorAssignmentCreatesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          TextString label <- "";
          SyntheticType() { label <- "foo" .. "bar"; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof StringConcatenation);
    StringConcatenation concat = (StringConcatenation) assign.rightHandSide.getValue();
    assertSame(JavaType.STRING_TYPE, concat.getType());
    assertTrue(concat.leftOperand.getValue() instanceof StringLiteral);
    assertEquals("foo", ((StringLiteral) concat.leftOperand.getValue()).value.getValue());
    assertTrue(concat.rightOperand.getValue() instanceof StringLiteral);
    assertEquals("bar", ((StringLiteral) concat.rightOperand.getValue()).value.getValue());
  }

  @Test
  public void decodeClassWithStringConcatReturnInMethodBodyCreatesStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          TextString greet() { return "hello" .. " world"; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.STRING_TYPE, ret.expressionType.getValue());
    assertTrue(ret.expression.getValue() instanceof StringConcatenation);
    StringConcatenation concat = (StringConcatenation) ret.expression.getValue();
    assertTrue(concat.leftOperand.getValue() instanceof StringLiteral);
    assertEquals("hello", ((StringLiteral) concat.leftOperand.getValue()).value.getValue());
    assertTrue(concat.rightOperand.getValue() instanceof StringLiteral);
    assertEquals(" world", ((StringLiteral) concat.rightOperand.getValue()).value.getValue());
  }

  @Test
  public void decodeClassWithNestedStringConcatRhsInMethodAssignmentCreatesNestedStringConcatenation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          TextString label <- "";
          void tag() { label <- "a" .. "b" .. "c"; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.rightHandSide.getValue() instanceof StringConcatenation);
    StringConcatenation outer = (StringConcatenation) assign.rightHandSide.getValue();
    assertSame(JavaType.STRING_TYPE, outer.getType());
  }

  @Test
  public void decodeClassWithUnknownIdentifierFieldAssignmentInMethodBodyReportsUnsupportedTarget() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { void setCount() { unknown <- 7; } }"));

    assertTrue(thrown.getMessage().contains("assignment target is not a known local or field"));
    assertTrue(thrown.getMessage().contains("unknown"));
    assertTrue(thrown.getMessage().contains("setCount"));
  }

  @Test
  public void decodeClassWithTypeMismatchFieldAssignmentInMethodBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              void setCount() { count <- "hello"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("Tweedle field assignment value type is not assignable to"));
    assertTrue(thrown.getMessage().contains("setCount"));
    assertTrue(thrown.getMessage().contains("count"));
  }

  @Test
  public void decodeClassWithLiteralLocalReassignmentInMethodBodyCreatesAssignmentStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void update() { WholeNumber x <- 1; x <- 5; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    UserLocal local = decl.local.getValue();
    assertEquals("x", local.getName());
    assertIntegerLiteral(decl.initializer.getValue(), 1);
    assertTrue(method.body.getValue().statements.get(1) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(1);
    assertTrue(stmt.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(AssignmentExpression.Operator.ASSIGN, assign.operator.getValue());
    assertTrue(assign.leftHandSide.getValue() instanceof LocalAccess);
    assertSame(local, ((LocalAccess) assign.leftHandSide.getValue()).local.getValue());
    assertIntegerLiteral(assign.rightHandSide.getValue(), 5);
  }

  @Test
  public void decodeClassWithLiteralLocalReassignmentInConstructorBodyCreatesAssignmentStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { WholeNumber x <- 1; x <- 5; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(2, constructor.body.getValue().statements.size());
    assertTrue(constructor.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) constructor.body.getValue().statements.get(0);
    UserLocal local = decl.local.getValue();
    assertEquals("x", local.getName());
    assertIntegerLiteral(decl.initializer.getValue(), 1);
    assertTrue(constructor.body.getValue().statements.get(1) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(1);
    assertTrue(stmt.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertSame(AssignmentExpression.Operator.ASSIGN, assign.operator.getValue());
    assertTrue(assign.leftHandSide.getValue() instanceof LocalAccess);
    assertSame(local, ((LocalAccess) assign.leftHandSide.getValue()).local.getValue());
    assertIntegerLiteral(assign.rightHandSide.getValue(), 5);
  }

  @Test
  public void decodeClassWithTypeMismatchLocalReassignmentInMethodBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void update() { WholeNumber x <- 1; x <- "bad"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("local variable assignment value type is not assignable to"));
    assertTrue(thrown.getMessage().contains("update"));
    assertTrue(thrown.getMessage().contains("x"));
  }

  @Test
  public void decodeClassWithTypeMismatchLocalReassignmentInConstructorBodyReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              SyntheticType() { WholeNumber x <- 1; x <- "bad"; }
            }
            """));

    assertTrue(thrown.getMessage().contains("constructor local variable assignment value type is not assignable to"));
    assertTrue(thrown.getMessage().contains("SyntheticType"));
    assertTrue(thrown.getMessage().contains("x"));
  }

  @Test
  public void decodeClassWithParameterIdentifierRhsInMethodAssignmentCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void setCount(WholeNumber val) { count <- val; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    UserMethod method = type.getDeclaredMethods().get(0);
    UserParameter param = method.getRequiredParameters().get(0);
    assertEquals("val", param.getName());
    assertEquals(1, method.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.leftHandSide.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof ParameterAccess);
    assertSame(param, ((ParameterAccess) assign.rightHandSide.getValue()).parameter.getValue());
  }

  @Test
  public void decodeClassWithLocalIdentifierRhsInMethodAssignmentCreatesLocalAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void update() { WholeNumber x <- 1; WholeNumber y <- 3; x <- y; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(3, method.body.getValue().statements.size());
    LocalDeclarationStatement xDecl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    UserLocal xLocal = xDecl.local.getValue();
    LocalDeclarationStatement yDecl = (LocalDeclarationStatement) method.body.getValue().statements.get(1);
    UserLocal yLocal = yDecl.local.getValue();
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(2);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.leftHandSide.getValue() instanceof LocalAccess);
    assertSame(xLocal, ((LocalAccess) assign.leftHandSide.getValue()).local.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof LocalAccess);
    assertSame(yLocal, ((LocalAccess) assign.rightHandSide.getValue()).local.getValue());
  }

  @Test
  public void decodeClassWithParameterIdentifierRhsInConstructorAssignmentCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType(WholeNumber val) { count <- val; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    UserParameter param = constructor.getRequiredParameters().get(0);
    assertEquals("val", param.getName());
    assertEquals(1, constructor.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.leftHandSide.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof ParameterAccess);
    assertSame(param, ((ParameterAccess) assign.rightHandSide.getValue()).parameter.getValue());
  }

  @Test
  public void decodeClassWithParameterIdentifierRhsInConstructorThisFieldAssignmentCreatesParameterAccess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType(WholeNumber val) { this.count <- val; }
        }
        """);

    UserField field = type.getDeclaredFields().get(0);
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    UserParameter param = constructor.getRequiredParameters().get(0);
    assertEquals("val", param.getName());
    assertEquals(1, constructor.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertTrue(assign.leftHandSide.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) assign.leftHandSide.getValue()).field.getValue());
    assertTrue(assign.rightHandSide.getValue() instanceof ParameterAccess);
    assertSame(param, ((ParameterAccess) assign.rightHandSide.getValue()).parameter.getValue());
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void caller() { this.helper(); }
          void helper() { }
        }
        """);

    UserMethod caller = userMethodNamed(type, "caller");
    UserMethod helper = userMethodNamed(type, "helper");
    assertEquals(1, caller.body.getValue().statements.size());
    assertTrue(caller.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) caller.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
  }

  @Test
  public void argumentBearingExplicitThisMethodCallDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void caller() { this.helper(value: 1); }
          void helper(WholeNumber value) { }
        }
        """);

    UserMethod caller = userMethodNamed(type, "caller");
    UserMethod helper = userMethodNamed(type, "helper");
    MethodInvocation invocation = onlyMethodInvocation(caller);
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertEquals(1, invocation.requiredArguments.size());
    assertSame(helper.getRequiredParameters().get(0), invocation.requiredArguments.get(0).parameter.getValue());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
  }

  @Test
  public void argumentBearingImplicitSameClassCallWithUnknownMethodReportsUnsupportedBoundary() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void caller() { missing(value: 1); }
            }
            """));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("argument-bearing explicit this method calls"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("caller.missing"));
  }

  @Test
  public void argumentBearingSameClassCallWithMismatchedLabelReportsUnsupportedBoundary() {
    assertUnsupportedArgumentBearingExplicitThisMethodCallDecode("""
        class SyntheticType {
          void caller() { this.helper(other: 1); }
          void helper(WholeNumber value) { }
        }
        """, "caller.this.helper");
  }

  @Test
  public void argumentBearingExplicitThisMethodCallReportsBoundaryBeforeMethodLookup() {
    assertUnsupportedArgumentBearingExplicitThisMethodCallDecode("""
        class SyntheticType {
          void caller() { this.missing(value: 1); }
        }
        """, "caller.this.missing");
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          void caller() { this.helper(); }
          void helper(WholeNumber value <- 1) { }
        }
        """, "this.helper");
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeRejectsUnknownMethod() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          void caller() { this.missing(); }
        }
        """, "this.missing");
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void caller() { this.helper(); }
              void helper() { }
              void helper() { }
            }
            """));

    assertTrue(thrown.getMessage().contains("Duplicate zero-argument Tweedle methods"));
    assertTrue(thrown.getMessage().contains("helper"));
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeRejectsNonThisTarget() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          TextString label <- "";
          void caller() { label.helper(); }
          void helper() { }
        }
        """, "label.helper");
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          void caller() { this.helper(); }
          static void helper() { }
        }
        """, "this.helper");
  }

  @Test
  public void zeroArgumentThisMethodCallDecodeRejectsChainedCall() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          void caller() { this.helper().other(); }
          void helper() { }
          void other() { }
        }
        """, "this.helper");
  }

  @Test
  public void implicitZeroArgumentSameClassMethodCallDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void caller() { helper(); }
          void helper() { }
        }
        """);

    UserMethod caller = userMethodNamed(type, "caller");
    UserMethod helper = userMethodNamed(type, "helper");
    assertEquals(1, caller.body.getValue().statements.size());
    assertTrue(caller.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) caller.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
  }

  @Test
  public void implicitSameClassMethodCallDecodeRejectsUnknownMethod() {
    assertUnsupportedImplicitSameClassMethodCallDecode("""
        class SyntheticType {
          void caller() { missing(); }
        }
        """, "missing");
  }

  @Test
  public void implicitSameClassMethodCallDecodeCreatesArgumentBearingMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void caller() { helper(value: 1); }
          void helper(WholeNumber value) { }
        }
        """);

    MethodInvocation invocation = onlyMethodInvocation(userMethodNamed(type, "caller"));
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(userMethodNamed(type, "helper"), invocation.method.getValue());
    assertEquals(1, invocation.requiredArguments.size());
  }

  @Test
  public void implicitSameClassMethodCallDecodeRejectsOptionalParameterTargetMethod() {
    assertUnsupportedImplicitSameClassMethodCallDecode("""
        class SyntheticType {
          void caller() { helper(); }
          void helper(WholeNumber value <- 1) { }
        }
        """, "helper");
  }

  @Test
  public void implicitSameClassMethodCallDecodeRejectsStaticTargetMethod() {
    assertUnsupportedImplicitSameClassMethodCallDecode("""
        class SyntheticType {
          void caller() { helper(); }
          static void helper() { }
        }
        """, "helper");
  }

  @Test
  public void zeroArgumentThisMethodCallInConstructorDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { this.helper(); }
          void helper() { }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    UserMethod helper = userMethodNamed(type, "helper");
    assertEquals(1, constructor.body.getValue().statements.size());
    assertTrue(constructor.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
  }

  @Test
  public void implicitZeroArgumentSameClassMethodCallInConstructorDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { helper(); }
          void helper() { }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    UserMethod helper = userMethodNamed(type, "helper");
    assertEquals(1, constructor.body.getValue().statements.size());
    assertTrue(constructor.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
  }

  @Test
  public void argumentBearingThisMethodCallInConstructorDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { this.helper(value: 1); }
          void helper(WholeNumber value) { }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertEquals(1, constructor.body.getValue().statements.size());
    ExpressionStatement stmt = (ExpressionStatement) constructor.body.getValue().statements.get(0);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(userMethodNamed(type, "helper"), invocation.method.getValue());
    assertEquals(1, invocation.requiredArguments.size());
  }

  @Test
  public void zeroArgumentThisMethodCallInConstructorDecodeRejectsUnknownMethod() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          SyntheticType() { this.missing(); }
        }
        """, "SyntheticType.this.missing");
  }

  @Test
  public void zeroArgumentThisMethodCallInConstructorDecodeRejectsNonThisTarget() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          TextString label <- "";
          SyntheticType() { label.helper(); }
          void helper() { }
        }
        """, "SyntheticType.label.helper");
  }

  @Test
  public void zeroArgumentThisMethodCallInConstructorDecodeRejectsStaticTargetMethod() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          SyntheticType() { this.helper(); }
          static void helper() { }
        }
        """, "SyntheticType.this.helper");
  }

  @Test
  public void decodeEnumReportsOnlyClassDeclarationsSupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("enum Direction {UP, DOWN}"));

    assertTrue(thrown.getMessage().contains("Only Tweedle class declarations"));
  }

  @Test
  public void decodeEmptySourceReportsOnlyClassDeclarationsSupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(""));

    assertTrue(thrown.getMessage().contains("Only Tweedle class declarations"));
  }

  @Test
  public void decodeClassWithEqualityLocalInitializerInMethodBodyCreatesRelationalEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { Boolean result <- a == b; return result; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertRelationalInfix(decl.initializer.getValue(), RelationalInfixExpression.Operator.EQUALS);
  }

  @Test
  public void decodeClassWithLessThanLocalInitializerInMethodBodyCreatesRelationalLess() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { Boolean result <- a < b; return result; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertRelationalInfix(decl.initializer.getValue(), RelationalInfixExpression.Operator.LESS);
  }

  @Test
  public void decodeClassWithGreaterThanReturnInMethodCreatesRelationalGreater() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean isGreater(WholeNumber a, WholeNumber b) { return a > b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.BOOLEAN_OBJECT_TYPE, ret.expressionType.getValue());
    assertRelationalInfix(ret.expression.getValue(), RelationalInfixExpression.Operator.GREATER);
  }

  @Test
  public void decodeClassWithNotEqualToRhsInMethodAssignmentCreatesRelationalNotEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean flag <- false;
          void check(WholeNumber a, WholeNumber b) { flag <- a != b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertRelationalInfix(assign.rightHandSide.getValue(), RelationalInfixExpression.Operator.NOT_EQUALS);
  }

  @Test
  public void decodeClassWithLessThanOrEqualReturnInMethodCreatesRelationalLessEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a <= b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertRelationalInfix(ret.expression.getValue(), RelationalInfixExpression.Operator.LESS_EQUALS);
  }

  @Test
  public void decodeClassWithGreaterThanOrEqualReturnInMethodCreatesRelationalGreaterEquals() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(WholeNumber a, WholeNumber b) { return a >= b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertRelationalInfix(ret.expression.getValue(), RelationalInfixExpression.Operator.GREATER_EQUALS);
  }

  @Test
  public void decodeClassWithComparisonReturnTypeMismatchReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              TextString bad(WholeNumber a, WholeNumber b) { return a == b; }
            }
            """));

    assertTrue(thrown.getMessage().contains("not assignable to"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  // --- Logical && (ConditionalInfixExpression.AND) ---

  @Test
  public void decodeClassWithAndLocalInitCreatesConditionalAnd() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a, Boolean b) {
            Boolean result <- a && b;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertConditionalInfix(decl.initializer.getValue(), ConditionalInfixExpression.Operator.AND);
  }

  @Test
  public void decodeClassWithAndRhsInMethodAssignmentCreatesConditionalAnd() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean flag <- false;
          void check(Boolean a, Boolean b) { flag <- a && b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertConditionalInfix(assign.rightHandSide.getValue(), ConditionalInfixExpression.Operator.AND);
  }

  @Test
  public void decodeClassWithAndReturnInMethodCreatesConditionalAnd() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a, Boolean b) { return a && b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertConditionalInfix(ret.expression.getValue(), ConditionalInfixExpression.Operator.AND);
  }

  @Test
  public void decodeClassWithAndReturnTypeMismatchReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              TextString bad(Boolean a, Boolean b) { return a && b; }
            }
            """));

    assertTrue(thrown.getMessage().contains("not assignable to"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  // --- Logical || (ConditionalInfixExpression.OR) ---

  @Test
  public void decodeClassWithOrReturnInMethodCreatesConditionalOr() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a, Boolean b) { return a || b; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertConditionalInfix(ret.expression.getValue(), ConditionalInfixExpression.Operator.OR);
  }

  @Test
  public void decodeClassWithOrReturnTypeMismatchReportsTypeError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              TextString bad(Boolean a, Boolean b) { return a || b; }
            }
            """));

    assertTrue(thrown.getMessage().contains("not assignable to"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  // --- Logical ! (LogicalComplement) ---

  @Test
  public void decodeClassWithNotLocalInitCreatesLogicalComplement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a) {
            Boolean result <- !a;
            return result;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    LocalDeclarationStatement decl = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertLogicalComplement(decl.initializer.getValue());
  }

  @Test
  public void decodeClassWithNotRhsInMethodAssignmentCreatesLogicalComplement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean flag <- false;
          void check(Boolean a) { flag <- !a; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ExpressionStatement stmt = (ExpressionStatement) method.body.getValue().statements.get(0);
    AssignmentExpression assign = (AssignmentExpression) stmt.expression.getValue();
    assertLogicalComplement(assign.rightHandSide.getValue());
  }

  @Test
  public void decodeClassWithNotReturnInMethodCreatesLogicalComplement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          Boolean check(Boolean a) { return !a; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ReturnStatement ret = (ReturnStatement) method.body.getValue().statements.get(0);
    assertLogicalComplement(ret.expression.getValue());
  }

  @Test
  public void decodeClassWithNotReturnTypeMismatchReportsTypeError() {
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

  // --- if/else (ConditionalStatement) ---

  @Test
  public void decodeClassWithIfStatementInVoidMethodCreatesConditionalStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void reset(Boolean flag) {
            if (flag) { count <- 0; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ConditionalStatement);
    ConditionalStatement conditional = (ConditionalStatement) method.body.getValue().statements.get(0);
    assertEquals(1, conditional.booleanExpressionBodyPairs.size());
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertTrue(pair.expression.getValue() instanceof org.lgna.project.ast.ParameterAccess);
    assertEquals(1, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    assertEquals(0, conditional.elseBody.getValue().statements.size());
  }

  @Test
  public void decodeClassWithIfElseStatementInVoidMethodCreatesBothBranches() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void toggle(Boolean flag) {
            if (flag) { count <- 1; } else { count <- 0; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    ConditionalStatement conditional = (ConditionalStatement) method.body.getValue().statements.get(0);
    assertEquals(1, conditional.booleanExpressionBodyPairs.size());
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertEquals(1, pair.body.getValue().statements.size());
    assertEquals(1, conditional.elseBody.getValue().statements.size());
    ExpressionStatement elseStmt = (ExpressionStatement) conditional.elseBody.getValue().statements.get(0);
    AssignmentExpression elseAssign = (AssignmentExpression) elseStmt.expression.getValue();
    assertIntegerLiteral(elseAssign.rightHandSide.getValue(), 0);
  }

  @Test
  public void decodeClassWithIfStatementWithRelationalConditionCreatesConditionalStatement() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void clampToZero(WholeNumber n) {
            if (n < 0) { count <- 0; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    ConditionalStatement conditional = (ConditionalStatement) method.body.getValue().statements.get(0);
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertRelationalInfix(pair.expression.getValue(), RelationalInfixExpression.Operator.LESS);
  }

  @Test
  public void decodeClassWithSimpleIfMethodCallBodyCreatesConditionalMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void run(WholeNumber n) {
            if (n > 0) { this.helper(); }
          }
          void helper() { }
        }
        """);

    UserMethod run = userMethodNamed(type, "run");
    UserMethod helper = userMethodNamed(type, "helper");
    assertEquals(1, run.body.getValue().statements.size());
    assertTrue(run.body.getValue().statements.get(0) instanceof ConditionalStatement);
    ConditionalStatement conditional = (ConditionalStatement) run.body.getValue().statements.get(0);
    assertEquals(1, conditional.booleanExpressionBodyPairs.size());
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertRelationalInfix(pair.expression.getValue(), RelationalInfixExpression.Operator.GREATER);
    assertEquals(1, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) pair.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
    assertEquals(0, conditional.elseBody.getValue().statements.size());
  }

  @Test
  public void decodeClassWithSimpleIfLogicalConditionAndMixedSupportedBodyCreatesOrderedStatements() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void run(Boolean a, Boolean b) {
            if (a && b) { count <- 1; this.helper(); }
          }
          void helper() { }
        }
        """);

    UserMethod run = userMethodNamed(type, "run");
    UserMethod helper = userMethodNamed(type, "helper");
    ConditionalStatement conditional = (ConditionalStatement) run.body.getValue().statements.get(0);
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertConditionalInfix(pair.expression.getValue(), ConditionalInfixExpression.Operator.AND);
    assertEquals(2, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement assignmentStatement = (ExpressionStatement) pair.body.getValue().statements.get(0);
    assertTrue(assignmentStatement.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assignment = (AssignmentExpression) assignmentStatement.expression.getValue();
    assertIntegerLiteral(assignment.rightHandSide.getValue(), 1);
    assertTrue(pair.body.getValue().statements.get(1) instanceof ExpressionStatement);
    ExpressionStatement methodStatement = (ExpressionStatement) pair.body.getValue().statements.get(1);
    assertTrue(methodStatement.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) methodStatement.expression.getValue();
    assertSame(helper, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
  }

  @Test
  public void decodeClassWithLocalDeclarationInIfBodyReportsUnsupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad(Boolean flag) {
                if (flag) { WholeNumber x <- 1; }
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("simple if"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void argumentBearingThisMethodCallInIfBodyDecodeCreatesMethodInvocation() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void run(Boolean flag) {
            if (flag) { this.helper(value: 1); }
          }
          void helper(WholeNumber value) { }
        }
        """);

    UserMethod run = userMethodNamed(type, "run");
    ConditionalStatement conditional = (ConditionalStatement) run.body.getValue().statements.get(0);
    BlockStatement thenBody = conditional.booleanExpressionBodyPairs.get(0).body.getValue();
    assertEquals(1, thenBody.statements.size());
    ExpressionStatement stmt = (ExpressionStatement) thenBody.statements.get(0);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertSame(userMethodNamed(type, "helper"), invocation.method.getValue());
    assertEquals(1, invocation.requiredArguments.size());
  }

  @Test
  public void decodeClassWithArbitraryReceiverMethodCallInIfBodyReportsUnsupportedBoundary() {
    assertUnsupportedZeroArgumentThisMethodCallDecode("""
        class SyntheticType {
          TextString label <- "";
          void run(Boolean flag) {
            if (flag) { label.helper(); }
          }
          void helper() { }
        }
        """, "run.label.helper");
  }

  @Test
  public void decodeClassWithMethodCallInIfElseBodyReportsUnsupportedBoundary() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              void run(Boolean flag) {
                if (flag) { this.helper(); } else { count <- 0; }
              }
              void helper() { }
            }
            """));

    assertTrue(thrown.getMessage().contains("if/else"));
    assertTrue(thrown.getMessage().contains("run"));
  }

  @Test
  public void decodeClassWithNestedIfInIfBodyReportsUnsupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              void bad(Boolean a, Boolean b) {
                if (a) { if (b) { count <- 1; } }
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("simple if"));
    assertTrue(thrown.getMessage().contains("bad"));
  }


  private NamedUserType decodeUserType(String source) throws Exception {
    AbstractNode decoded = coder.decode(source);

    assertTrue(decoded instanceof NamedUserType);
    return (NamedUserType) decoded;
  }

  private NamedUserType decodeUserType(String source, Set<NamedUserType> terminals) throws Exception {
    AbstractNode decoded = coder.decode(source, Set.copyOf(terminals));

    assertTrue(decoded instanceof NamedUserType);
    return (NamedUserType) decoded;
  }

  private NamedUserType userTypeNamed(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    return type;
  }

  private static UserMethod userMethodNamed(NamedUserType type, String name) {
    return type.getDeclaredMethods().stream()
        .filter(method -> name.equals(method.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing method: " + name));
  }

  private static MethodInvocation onlyMethodInvocation(UserMethod method) {
    BlockStatement body = method.body.getValue();
    assertEquals(1, body.statements.size());
    assertTrue(body.statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement statement = (ExpressionStatement) body.statements.get(0);
    assertTrue(statement.expression.getValue() instanceof MethodInvocation);
    return (MethodInvocation) statement.expression.getValue();
  }

  private void assertUnsupportedZeroArgumentThisMethodCallDecode(String source, String expectedDetail) {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(source));

    assertTrue(thrown.getMessage().contains("zero-argument this-method calls"));
    assertTrue(thrown.getMessage().contains(expectedDetail));
  }

  private void assertUnsupportedArgumentBearingExplicitThisMethodCallDecode(String source, String expectedDetail) {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(source));

    assertTrue(thrown.getMessage().contains("argument-bearing explicit this method calls"));
    assertTrue(thrown.getMessage().contains(expectedDetail));
  }

  private void assertUnsupportedImplicitSameClassMethodCallDecode(String source, String expectedDetail) {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(source));

    assertTrue(thrown.getMessage(), thrown.getMessage().contains(expectedDetail));
  }

  private static void assertUnsupportedResourceFieldInitializer(
      UnsupportedTweedleDecodeException thrown,
      String expectedFieldName) {
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("resource field initializer"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("non-null"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("not yet supported"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("manifest or binding context"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains("Only null resource field initializers"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains(expectedFieldName));
  }

  private static void assertIntegerInitializer(UserField field, String expectedName, int expectedValue) {
    assertEquals(expectedName, field.getName());
    Expression initializer = field.initializer.getValue();
    assertIntegerLiteral(initializer, expectedValue);
  }

  private static void assertIntegerLiteral(Expression initializer, int expectedValue) {
    assertTrue(initializer instanceof IntegerLiteral);
    assertEquals(expectedValue, ((IntegerLiteral) initializer).value.getValue().intValue());
  }

  private static void assertDoubleInitializer(UserField field, String expectedName, double expectedValue) {
    assertEquals(expectedName, field.getName());
    Expression initializer = field.initializer.getValue();
    assertTrue(initializer instanceof DoubleLiteral);
    assertEquals(expectedValue, ((DoubleLiteral) initializer).value.getValue(), 0.0);
  }

  private static void assertStringInitializer(UserField field, String expectedName, String expectedValue) {
    assertEquals(expectedName, field.getName());
    Expression initializer = field.initializer.getValue();
    assertTrue(initializer instanceof StringLiteral);
    assertEquals(expectedValue, ((StringLiteral) initializer).value.getValue());
  }

  private static void assertBooleanInitializer(UserField field, String expectedName, boolean expectedValue) {
    assertEquals(expectedName, field.getName());
    Expression initializer = field.initializer.getValue();
    assertTrue(initializer instanceof BooleanLiteral);
    assertEquals(expectedValue, ((BooleanLiteral) initializer).value.getValue());
  }

  private static void assertNullInitializer(UserField field, String expectedName) {
    assertEquals(expectedName, field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);
  }

  private static ArithmeticInfixExpression assertArithmeticInfixOperator(
      Expression expression,
      ArithmeticInfixExpression.Operator expectedOperator,
      org.lgna.project.ast.AbstractType<?, ?, ?> expectedType) {
    assertTrue("Expected ArithmeticInfixExpression, got: " + expression.getClass().getSimpleName(),
        expression instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression infix = (ArithmeticInfixExpression) expression;
    assertSame(expectedOperator, infix.operator.getValue());
    assertSame(expectedType, infix.getType());
    return infix;
  }

  private static void assertArithmeticInfix(
      Expression expression,
      ArithmeticInfixExpression.Operator expectedOperator,
      org.lgna.project.ast.AbstractType<?, ?, ?> expectedType,
      int expectedLeft,
      int expectedRight) {
    ArithmeticInfixExpression infix = assertArithmeticInfixOperator(expression, expectedOperator, expectedType);
    assertIntegerLiteral(infix.leftOperand.getValue(), expectedLeft);
    assertIntegerLiteral(infix.rightOperand.getValue(), expectedRight);
  }

  private static void assertRelationalInfix(Expression expression, RelationalInfixExpression.Operator expectedOperator) {
    assertTrue("Expected RelationalInfixExpression, got: " + expression.getClass().getSimpleName(),
        expression instanceof RelationalInfixExpression);
    RelationalInfixExpression infix = (RelationalInfixExpression) expression;
    assertSame(expectedOperator, infix.operator.getValue());
    assertSame(JavaType.BOOLEAN_OBJECT_TYPE, infix.getType());
  }
  private static void assertConditionalInfix(Expression expression, ConditionalInfixExpression.Operator expectedOperator) {
    assertTrue("Expected ConditionalInfixExpression, got: " + expression.getClass().getSimpleName(),
        expression instanceof ConditionalInfixExpression);
    ConditionalInfixExpression infix = (ConditionalInfixExpression) expression;
    assertSame(expectedOperator, infix.operator.getValue());
    assertSame(JavaType.BOOLEAN_OBJECT_TYPE, infix.getType());
  }

  private static void assertLogicalComplement(Expression expression) {
    assertTrue("Expected LogicalComplement, got: " + expression.getClass().getSimpleName(),
        expression instanceof LogicalComplement);
    LogicalComplement complement = (LogicalComplement) expression;
    assertSame(JavaType.BOOLEAN_OBJECT_TYPE, complement.getType());
  }

  // --- WhileLoop ---

  @Test
  public void decodeClassWithWhileLoopInVoidMethodCreatesWhileLoop() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void reset(Boolean flag) {
            while (flag) { count <- 0; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof WhileLoop);
    WhileLoop loop = (WhileLoop) method.body.getValue().statements.get(0);
    assertTrue(loop.conditional.getValue() instanceof ParameterAccess);
    assertSame(JavaType.BOOLEAN_OBJECT_TYPE, loop.conditional.getValue().getType());
    assertEquals(1, loop.body.getValue().statements.size());
    assertTrue(loop.body.getValue().statements.get(0) instanceof ExpressionStatement);
  }

  @Test
  public void decodeClassWithWhileLoopWithRelationalConditionCreatesWhileLoop() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 10;
          void countDown(WholeNumber n) {
            while (n > 0) { count <- 0; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof WhileLoop);
    WhileLoop loop = (WhileLoop) method.body.getValue().statements.get(0);
    assertRelationalInfix(loop.conditional.getValue(), RelationalInfixExpression.Operator.GREATER);
    assertEquals(1, loop.body.getValue().statements.size());
  }

  @Test
  public void decodeClassWithEmptyWhileLoopBodyCreatesWhileLoopWithEmptyBody() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void spin(Boolean flag) {
            while (flag) { }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof WhileLoop);
    WhileLoop loop = (WhileLoop) method.body.getValue().statements.get(0);
    assertEquals(0, loop.body.getValue().statements.size());
  }

  @Test
  public void decodeClassWithWhileLoopNonBooleanConditionReportsUnsupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad(WholeNumber n) {
                while (n) { }
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("while condition"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeClassWithWhileLoopWithLocalDeclarationInBodyReportsUnsupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad(Boolean flag) {
                while (flag) { WholeNumber x <- 1; }
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("while loop bodies"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  @Test
  public void decodeClassWithWhileLoopInNonVoidMethodReportsUnsupported() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber bad(Boolean flag) {
                while (flag) { }
                return 0;
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("while loops"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

}
