package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.SourceCodeGenerator;
import org.lgna.project.code.ProcessableNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for the Encoder → TweedleEncoder rename (issue #480).
 *
 * <p>These tests specify the contract that must hold after
 * extracting / renaming Encoder.java to TweedleEncoder.java:
 * <ul>
 *   <li>TweedleEncoder exists and extends SourceCodeGenerator</li>
 *   <li>Default and terminal-set constructors work</li>
 *   <li>encode(ProcessableNode) produces valid Tweedle output</li>
 *   <li>TweedleEncoderDecoder facade delegates to TweedleEncoder</li>
 *   <li>Encode→decode round-trip preserves structure</li>
 * </ul>
 *
 * <p>Uses reflection to reference TweedleEncoder so these tests compile
 * before the rename and fail at runtime until TweedleEncoder exists.
 */
public class TweedleEncoderTest {

  private static final String TWEEDLE_ENCODER_CLASS = "org.alice.serialization.tweedle.TweedleEncoder";

  // ═══════════════════════════════════════════════════════════════════════════
  // CLASS EXISTENCE AND HIERARCHY
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void tweedleEncoderClassExists() throws Exception {
    Object encoder = newDefaultEncoder();
    assertNotNull("TweedleEncoder should be instantiable via default constructor", encoder);
  }

  @Test
  public void tweedleEncoderExtendsSourceCodeGenerator() throws Exception {
    Object encoder = newDefaultEncoder();
    assertTrue("TweedleEncoder must extend SourceCodeGenerator",
        encoder instanceof SourceCodeGenerator);
  }

  @Test
  public void tweedleEncoderWithTerminalsConstructorWorks() throws Exception {
    Set<AbstractDeclaration> terminals = new HashSet<>();
    Object encoder = newEncoderWithTerminals(terminals);
    assertNotNull("TweedleEncoder(terminals) constructor must work", encoder);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ENCODE BASIC BEHAVIOR
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void encodeReturnsNonNullStringForProcessableNode() throws Exception {
    NamedUserType type = decodeType("class EncodeTest {}");
    String result = encodeViaReflection(newDefaultEncoder(), type);

    assertNotNull("encode() must return non-null String", result);
    assertTrue("encode() must return non-empty String for a type", result.length() > 0);
  }

  @Test
  public void encodedOutputContainsClassName() throws Exception {
    NamedUserType type = decodeType("class MyTestType {}");
    String encoded = encodeViaReflection(newDefaultEncoder(), type);

    assertTrue("Encoded output must contain the class name",
        encoded.contains("MyTestType"));
  }

  @Test
  public void encodedOutputContainsClassKeyword() throws Exception {
    NamedUserType type = decodeType("class SimpleType {}");
    String encoded = encodeViaReflection(newDefaultEncoder(), type);

    assertTrue("Encoded output must contain 'class' keyword",
        encoded.contains("class"));
  }

  @Test
  public void encodedTypeWithFieldIncludesFieldName() throws Exception {
    NamedUserType type = decodeType(
        "class FieldType { WholeNumber count <- 42; }");
    String encoded = encodeViaReflection(newDefaultEncoder(), type);

    assertTrue("Encoded output must contain field name 'count'",
        encoded.contains("count"));
  }

  @Test
  public void encodedTypeWithMethodIncludesMethodName() throws Exception {
    NamedUserType type = decodeType(
        "class MethodType { void doSomething() {} }");
    String encoded = encodeViaReflection(newDefaultEncoder(), type);

    assertTrue("Encoded output must contain method name 'doSomething'",
        encoded.contains("doSomething"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FACADE DELEGATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void facadeEncodeProducesSameOutputAsTweedleEncoder() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    NamedUserType type = decodeType("class FacadeTest {}");

    String facadeResult = facade.encodeProcessable(type);
    String directResult = encodeViaReflection(newDefaultEncoder(), type);

    assertEquals("Facade and direct TweedleEncoder must produce identical output",
        facadeResult, directResult);
  }

  @Test
  public void facadeEncodeWithTerminalsProducesSameOutput() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    NamedUserType type = decodeType("class TerminalTest {}");
    Set<AbstractDeclaration> terminals = new HashSet<>();

    String facadeResult = facade.encode(type, terminals);
    String directResult = encodeViaReflection(newEncoderWithTerminals(terminals), type);

    assertEquals("Facade with terminals and direct TweedleEncoder must match",
        facadeResult, directResult);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ENCODE → DECODE ROUND-TRIP
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void encodeDecodeRoundTripPreservesClassName() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    NamedUserType original = decodeType("class RoundTrip {}");

    String encoded = encodeViaReflection(newDefaultEncoder(), original);
    NamedUserType decoded = (NamedUserType) facade.decode(encoded);

    assertEquals("Class name must survive encode→decode round-trip",
        original.getName(), decoded.getName());
  }

  @Test
  public void encodeDecodeRoundTripPreservesFieldCount() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    NamedUserType original = decodeType(
        "class FieldRoundTrip { WholeNumber a <- 1; DecimalNumber b <- 2.0; }");

    String encoded = encodeViaReflection(newDefaultEncoder(), original);
    NamedUserType decoded = (NamedUserType) facade.decode(encoded);

    assertEquals("Field count must survive encode→decode round-trip",
        original.getDeclaredFields().size(), decoded.getDeclaredFields().size());
  }

  @Test
  public void encodeDecodeRoundTripPreservesMethodCount() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    NamedUserType original = decodeType("""
        class MethodRoundTrip {
          void alpha() {}
          void beta() {}
        }
        """);

    String encoded = encodeViaReflection(newDefaultEncoder(), original);
    NamedUserType decoded = (NamedUserType) facade.decode(encoded);

    assertEquals("Method count must survive encode→decode round-trip",
        original.getDeclaredMethods().size(), decoded.getDeclaredMethods().size());
  }

  @Test
  public void encodeDecodeRoundTripPreservesMethodNames() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    NamedUserType original = decodeType("""
        class NameRoundTrip {
          void first() {}
          void second() {}
        }
        """);

    String encoded = encodeViaReflection(newDefaultEncoder(), original);
    NamedUserType decoded = (NamedUserType) facade.decode(encoded);

    for (int i = 0; i < original.getDeclaredMethods().size(); i++) {
      assertEquals("Method name must survive round-trip",
          original.getDeclaredMethods().get(i).getName(),
          decoded.getDeclaredMethods().get(i).getName());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ENCODER IDEMPOTENCY
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void encodingTwiceProducesIdenticalOutput() throws Exception {
    NamedUserType type = decodeType(
        "class IdempotentTest { WholeNumber x <- 5; void run() {} }");

    String first = encodeViaReflection(newDefaultEncoder(), type);
    String second = encodeViaReflection(newDefaultEncoder(), type);

    assertEquals("Two separate encoder instances must produce identical output",
        first, second);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ENCODER INSTANCE INDEPENDENCE
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void separateEncoderInstancesAreIndependent() throws Exception {
    NamedUserType typeA = decodeType("class TypeA {}");
    NamedUserType typeB = decodeType("class TypeB {}");

    String outputA = encodeViaReflection(newDefaultEncoder(), typeA);
    String outputB = encodeViaReflection(newDefaultEncoder(), typeB);

    assertTrue("TypeA output must contain 'TypeA'", outputA.contains("TypeA"));
    assertTrue("TypeB output must contain 'TypeB'", outputB.contains("TypeB"));
    assertFalse("TypeA output must not contain 'TypeB'", outputA.contains("TypeB"));
    assertFalse("TypeB output must not contain 'TypeA'", outputB.contains("TypeA"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DECODER ISOLATION — no encode methods on Decoder
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void decoderClassHasNoEncodeMethod() {
    java.lang.reflect.Method[] methods = Decoder.class.getDeclaredMethods();
    for (java.lang.reflect.Method m : methods) {
      assertFalse("Decoder must not have any method starting with 'encode', found: " + m.getName(),
          m.getName().startsWith("encode"));
    }
  }

  @Test
  public void decoderClassHasNoProcessResourceTypeMethod() {
    java.lang.reflect.Method[] methods = Decoder.class.getDeclaredMethods();
    for (java.lang.reflect.Method m : methods) {
      assertFalse("Decoder must not have processResourceType, found: " + m.getName(),
          m.getName().equals("processResourceType"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EMPTY TERMINALS EDGE CASE
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void encodeWithEmptyTerminalsMatchesDefaultConstructor() throws Exception {
    NamedUserType type = decodeType("class EmptyTerminals {}");

    String defaultOutput = encodeViaReflection(newDefaultEncoder(), type);
    String terminalOutput = encodeViaReflection(
        newEncoderWithTerminals(new HashSet<>()), type);

    assertEquals("Empty terminals should produce same output as default constructor",
        defaultOutput, terminalOutput);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // REFLECTION HELPERS — compile without TweedleEncoder in classpath
  // ═══════════════════════════════════════════════════════════════════════════

  private static Object newDefaultEncoder() throws Exception {
    Class<?> clazz = Class.forName(TWEEDLE_ENCODER_CLASS);
    Constructor<?> ctor = clazz.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  private static Object newEncoderWithTerminals(Set<AbstractDeclaration> terminals) throws Exception {
    Class<?> clazz = Class.forName(TWEEDLE_ENCODER_CLASS);
    Constructor<?> ctor = clazz.getDeclaredConstructor(Set.class);
    ctor.setAccessible(true);
    return ctor.newInstance(terminals);
  }

  private static String encodeViaReflection(Object encoder, ProcessableNode node) throws Exception {
    Method encodeMethod = encoder.getClass().getMethod("encode", ProcessableNode.class);
    return (String) encodeMethod.invoke(encoder, node);
  }

  private static NamedUserType decodeType(String source) {
    try {
      TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
      AbstractNode node = facade.decode(source);
      assertTrue("Decoded node must be a NamedUserType", node instanceof NamedUserType);
      return (NamedUserType) node;
    } catch (Exception e) {
      throw new RuntimeException("Failed to decode test input: " + source, e);
    }
  }
}
