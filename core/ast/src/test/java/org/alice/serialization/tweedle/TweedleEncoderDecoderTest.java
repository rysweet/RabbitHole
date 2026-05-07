package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserArrayType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;

import java.util.Set;

import static org.junit.Assert.assertEquals;
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
  public void decodeClassWithNonLiteralInitializedFieldReportsUnsupportedInitializer() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count <- 1 + 2; }"));

    assertTrue(thrown.getMessage().contains("initializers"));
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
  public void decodeClassWithNonEmptyMethodReportsUnsupportedMethodBody() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count() { return 1; } }"));

    assertTrue(thrown.getMessage().contains("method bodies"));
  }

  @Test
  public void decodeClassWithMethodParameterReportsUnsupportedMethodParameters() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { void initialize(WholeNumber count) { } }"));

    assertTrue(thrown.getMessage().contains("method parameters"));
  }

  @Test
  public void decodeClassWithNonVoidEmptyMethodReportsUnsupportedReturnValues() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { WholeNumber count() { } }"));

    assertTrue(thrown.getMessage().contains("method return values"));
  }

  @Test
  public void decodeClassWithConstructorReportsUnsupportedMembers() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("class SyntheticType { SyntheticType() { } }"));

    assertTrue(thrown.getMessage().contains("constructors"));
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

}
