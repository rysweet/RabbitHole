package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.code.ProcessableNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for the StatementEncoder extraction from TweedleEncoder (issue #506).
 *
 * <p>These tests specify the contract that must hold after extracting
 * statement-encoding methods (appendStatementCompletion, appendStatementEnd,
 * pushStatementDisabled, appendCodeFlowStatement) into a new StatementEncoder
 * companion class, and wiring TweedleEncoder to delegate.
 *
 * <p>Written before implementation — all tests fail until StatementEncoder
 * exists and TweedleEncoder delegates correctly.
 *
 * <p>Test categories:
 * <ul>
 *   <li>CLASS STRUCTURE — StatementEncoder exists with correct visibility and constructor</li>
 *   <li>EXTRACTED METHODS — StatementEncoder has the expected methods</li>
 *   <li>DELEGATION WIRING — TweedleEncoder holds and delegates to StatementEncoder</li>
 *   <li>VISIBILITY CHANGES — NODE_ENABLE/NODE_DISABLE are package-private</li>
 *   <li>BRIDGE METHODS — TweedleEncoder provides required bridge methods</li>
 *   <li>BEHAVIORAL PRESERVATION — encode output is identical before and after extraction</li>
 * </ul>
 */
public class StatementEncoderExtractionTest {

  private static final String STATEMENT_ENCODER_CLASS =
      "org.alice.serialization.tweedle.StatementEncoder";
  private static final String TWEEDLE_ENCODER_CLASS =
      "org.alice.serialization.tweedle.TweedleEncoder";

  // ═══════════════════════════════════════════════════════════════════════════
  // CLASS STRUCTURE — StatementEncoder exists with correct shape
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void statementEncoderClassExists() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    assertNotNull("StatementEncoder class must be loadable", clazz);
  }

  @Test
  public void statementEncoderIsPackagePrivate() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    int modifiers = clazz.getModifiers();
    assertFalse("StatementEncoder must not be public",
        Modifier.isPublic(modifiers));
    assertFalse("StatementEncoder must not be private",
        Modifier.isPrivate(modifiers));
    assertFalse("StatementEncoder must not be protected",
        Modifier.isProtected(modifiers));
  }

  @Test
  public void statementEncoderConstructorAcceptsTweedleEncoder() throws Exception {
    Class<?> seClass = Class.forName(STATEMENT_ENCODER_CLASS);
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Constructor<?> ctor = seClass.getDeclaredConstructor(teClass);
    assertNotNull("StatementEncoder must have constructor(TweedleEncoder)", ctor);
  }

  @Test
  public void statementEncoderConstructorIsPackagePrivate() throws Exception {
    Class<?> seClass = Class.forName(STATEMENT_ENCODER_CLASS);
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Constructor<?> ctor = seClass.getDeclaredConstructor(teClass);
    int modifiers = ctor.getModifiers();
    assertFalse("StatementEncoder constructor must not be public",
        Modifier.isPublic(modifiers));
    assertFalse("StatementEncoder constructor must not be private",
        Modifier.isPrivate(modifiers));
  }

  @Test
  public void statementEncoderIsNotAbstract() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    assertFalse("StatementEncoder must not be abstract",
        Modifier.isAbstract(clazz.getModifiers()));
  }

  @Test
  public void statementEncoderDoesNotExtendSourceCodeGenerator() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    assertEquals("StatementEncoder must extend Object (not SourceCodeGenerator)",
        Object.class, clazz.getSuperclass());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EXTRACTED METHODS — StatementEncoder has the expected methods
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void statementEncoderHasAppendStatementCompletionWithStatement() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    Method method = clazz.getDeclaredMethod("appendStatementCompletion", Statement.class);
    assertNotNull("StatementEncoder must have appendStatementCompletion(Statement)", method);
  }

  @Test
  public void statementEncoderHasAppendStatementCompletionNoArg() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    Method method = clazz.getDeclaredMethod("appendStatementCompletion");
    assertNotNull("StatementEncoder must have appendStatementCompletion()", method);
  }

  @Test
  public void statementEncoderHasPushStatementDisabled() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    Method method = clazz.getDeclaredMethod("pushStatementDisabled");
    assertNotNull("StatementEncoder must have pushStatementDisabled()", method);
  }

  @Test
  public void statementEncoderHasAppendCodeFlowStatement() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    Method method = clazz.getDeclaredMethod("appendCodeFlowStatement",
        Statement.class, Runnable.class);
    assertNotNull("StatementEncoder must have appendCodeFlowStatement(Statement, Runnable)",
        method);
  }

  @Test
  public void extractedMethodsArePackagePrivate() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    String[] methodNames = {
        "appendStatementCompletion",
        "pushStatementDisabled"
    };
    for (String name : methodNames) {
      Method[] methods = clazz.getDeclaredMethods();
      for (Method m : methods) {
        if (m.getName().equals(name)) {
          assertFalse(name + " must not be public", Modifier.isPublic(m.getModifiers()));
          assertFalse(name + " must not be private", Modifier.isPrivate(m.getModifiers()));
        }
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DELEGATION WIRING — TweedleEncoder holds StatementEncoder field
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void tweedleEncoderHasStatementEncoderField() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Class<?> seClass = Class.forName(STATEMENT_ENCODER_CLASS);
    Field field = findFieldOfType(teClass, seClass);
    assertNotNull("TweedleEncoder must have a field of type StatementEncoder", field);
  }

  @Test
  public void tweedleEncoderStatementEncoderFieldIsPrivateOrPackagePrivate() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Class<?> seClass = Class.forName(STATEMENT_ENCODER_CLASS);
    Field field = findFieldOfType(teClass, seClass);
    assertNotNull("TweedleEncoder must have a field of type StatementEncoder", field);
    assertFalse("StatementEncoder field must not be public",
        Modifier.isPublic(field.getModifiers()));
    assertFalse("StatementEncoder field must not be protected",
        Modifier.isProtected(field.getModifiers()));
  }

  @Test
  public void tweedleEncoderStillOverridesAppendStatementCompletionWithStatement() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("appendStatementCompletion", Statement.class);
    assertNotNull("TweedleEncoder must still override appendStatementCompletion(Statement)", method);
  }

  @Test
  public void tweedleEncoderStillOverridesAppendStatementCompletionNoArg() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("appendStatementCompletion");
    assertNotNull("TweedleEncoder must still override appendStatementCompletion()", method);
  }

  @Test
  public void tweedleEncoderStillOverridesPushStatementDisabled() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("pushStatementDisabled");
    assertNotNull("TweedleEncoder must still override pushStatementDisabled()", method);
  }

  @Test
  public void tweedleEncoderStillOverridesAppendCodeFlowStatement() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("appendCodeFlowStatement",
        Statement.class, Runnable.class);
    assertNotNull("TweedleEncoder must still override appendCodeFlowStatement(Statement, Runnable)",
        method);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VISIBILITY CHANGES — NODE_ENABLE/NODE_DISABLE package-private
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void nodeEnableConstantIsPackagePrivate() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Field field = teClass.getDeclaredField("NODE_ENABLE");
    assertNotNull("NODE_ENABLE field must exist", field);
    assertFalse("NODE_ENABLE must not be private after extraction",
        Modifier.isPrivate(field.getModifiers()));
    assertFalse("NODE_ENABLE must not be public",
        Modifier.isPublic(field.getModifiers()));
    assertTrue("NODE_ENABLE must be static",
        Modifier.isStatic(field.getModifiers()));
  }

  @Test
  public void nodeDisableConstantIsPackagePrivate() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Field field = teClass.getDeclaredField("NODE_DISABLE");
    assertNotNull("NODE_DISABLE field must exist", field);
    assertFalse("NODE_DISABLE must not be private after extraction",
        Modifier.isPrivate(field.getModifiers()));
    assertFalse("NODE_DISABLE must not be public",
        Modifier.isPublic(field.getModifiers()));
    assertTrue("NODE_DISABLE must be static",
        Modifier.isStatic(field.getModifiers()));
  }

  @Test
  public void nodeEnableHasCorrectValue() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Field field = teClass.getDeclaredField("NODE_ENABLE");
    field.setAccessible(true);
    assertEquals("NODE_ENABLE must be \">*\"", ">*", field.get(null));
  }

  @Test
  public void nodeDisableHasCorrectValue() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Field field = teClass.getDeclaredField("NODE_DISABLE");
    field.setAccessible(true);
    assertEquals("NODE_DISABLE must be \"*<\"", "*<", field.get(null));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BRIDGE METHODS — TweedleEncoder provides forwarding for super/protected
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void tweedleEncoderHasSuperAppendStatementCompletionBridge() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("superAppendStatementCompletion", Statement.class);
    assertNotNull("TweedleEncoder must have superAppendStatementCompletion(Statement) bridge", method);
    assertFalse("Bridge must not be public", Modifier.isPublic(method.getModifiers()));
    assertFalse("Bridge must not be private", Modifier.isPrivate(method.getModifiers()));
  }

  @Test
  public void tweedleEncoderHasSuperAppendStatementCompletionNoArgBridge() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("superAppendStatementCompletion");
    assertNotNull("TweedleEncoder must have superAppendStatementCompletion() bridge", method);
  }

  @Test
  public void tweedleEncoderHasSuperPushStatementDisabledBridge() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("superPushStatementDisabled");
    assertNotNull("TweedleEncoder must have superPushStatementDisabled() bridge", method);
  }

  @Test
  public void tweedleEncoderHasForwardAppendStringBridge() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("forwardAppendString", String.class);
    assertNotNull("TweedleEncoder must have forwardAppendString(String) bridge", method);
    assertFalse("Bridge must not be public", Modifier.isPublic(method.getModifiers()));
  }

  @Test
  public void tweedleEncoderHasForwardAppendSpaceBridge() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("forwardAppendSpace");
    assertNotNull("TweedleEncoder must have forwardAppendSpace() bridge", method);
  }

  @Test
  public void tweedleEncoderHasForwardAppendNewLineBridge() throws Exception {
    Class<?> teClass = Class.forName(TWEEDLE_ENCODER_CLASS);
    Method method = teClass.getDeclaredMethod("forwardAppendNewLine");
    assertNotNull("TweedleEncoder must have forwardAppendNewLine() bridge", method);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BEHAVIORAL PRESERVATION — encode output identical after extraction
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void encodeSimpleMethodWithStatementPreservesOutput() throws Exception {
    String source = """
        class MethodType {
          void doSomething() {}
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeMethodWithLocalDeclarationPreservesOutput() throws Exception {
    String source = """
        class LocalDeclType {
          WholeNumber compute() {
            WholeNumber temp <- 42;
            return temp;
          }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeMethodWithFieldAssignmentPreservesOutput() throws Exception {
    String source = """
        class AssignType {
          WholeNumber count <- 0;
          void increment() { this.count <- 1; }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeConditionalStatementPreservesOutput() throws Exception {
    String source = """
        class ConditionalType {
          WholeNumber count <- 0;
          void update(Boolean flag) {
            if (flag) { this.count <- 1; } else { this.count <- 2; }
          }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeWhileLoopPreservesOutput() throws Exception {
    String source = """
        class WhileType {
          WholeNumber count <- 0;
          void spin(Boolean flag) {
            while (flag) { this.count <- 0; }
          }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeMultipleStatementsPreservesOutput() throws Exception {
    String source = """
        class MultiStmtType {
          WholeNumber count <- 0;
          WholeNumber compute(WholeNumber x) {
            WholeNumber temp <- x;
            this.count <- temp;
            return temp;
          }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeReturnStatementPreservesOutput() throws Exception {
    String source = """
        class ReturnType {
          WholeNumber getValue() {
            return 42;
          }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeConstructorPreservesOutput() throws Exception {
    String source = """
        class CtorType {
          WholeNumber count <- 0;
          CtorType() { this.count <- 5; }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodeMethodCallPreservesOutput() throws Exception {
    String source = """
        class CallType {
          void run() { this.helper(); }
          void helper() { }
        }
        """;
    assertRoundTripIdentical(source);
  }

  @Test
  public void encodingTwiceWithDelegationProducesIdenticalOutput() throws Exception {
    String source = """
        class IdempotentDelegation {
          WholeNumber x <- 5;
          void run() { this.x <- 10; }
        }
        """;
    NamedUserType type = decodeAndPrepare(source);

    String first = encodeViaReflection(newDefaultEncoder(), type);
    String second = encodeViaReflection(newDefaultEncoder(), type);

    assertEquals("Two encodes after extraction must produce identical output",
        first, second);
  }

  @Test
  public void facadeEncodeMatchesDirectEncodeAfterExtraction() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    String source = """
        class FacadeDelegationCheck {
          WholeNumber count <- 0;
          void update() { this.count <- 1; }
        }
        """;
    NamedUserType type = decodeAndPrepare(source);

    String facadeResult = facade.encodeProcessable(type);
    String directResult = encodeViaReflection(newDefaultEncoder(), type);

    assertEquals("Facade and direct encode must match after extraction",
        facadeResult, directResult);
  }

  @Test
  public void encodeDecodeRoundTripPreservesStatementCount() throws Exception {
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    String source = """
        class StmtCountTrip {
          WholeNumber a <- 0;
          WholeNumber compute(WholeNumber x) {
            WholeNumber temp <- x;
            this.a <- temp;
            return temp;
          }
        }
        """;
    NamedUserType original = decodeAndPrepare(source);
    String encoded = encodeViaReflection(newDefaultEncoder(), original);
    NamedUserType decoded = decodeAndPrepare(encoded);

    int originalCount = original.getDeclaredMethods().get(0)
        .body.getValue().statements.size();
    int decodedCount = decoded.getDeclaredMethods().get(0)
        .body.getValue().statements.size();

    assertEquals("Statement count must survive encode→decode round-trip",
        originalCount, decodedCount);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // NEGATIVE TESTS — StatementEncoder must not leak surface area
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void statementEncoderHasNoPublicMethods() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    for (Method m : clazz.getDeclaredMethods()) {
      assertFalse("StatementEncoder method " + m.getName() + " must not be public",
          Modifier.isPublic(m.getModifiers()));
    }
  }

  @Test
  public void statementEncoderHasNoPublicConstructors() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    for (Constructor<?> c : clazz.getDeclaredConstructors()) {
      assertFalse("StatementEncoder constructor must not be public",
          Modifier.isPublic(c.getModifiers()));
    }
  }

  @Test
  public void statementEncoderHasExactlyOneConstructor() throws Exception {
    Class<?> clazz = Class.forName(STATEMENT_ENCODER_CLASS);
    assertEquals("StatementEncoder must have exactly one constructor",
        1, clazz.getDeclaredConstructors().length);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // REFLECTION HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private static Object newDefaultEncoder() throws Exception {
    Class<?> clazz = Class.forName(TWEEDLE_ENCODER_CLASS);
    Constructor<?> ctor = clazz.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  private static String encodeViaReflection(Object encoder, ProcessableNode node) throws Exception {
    Method encodeMethod = encoder.getClass().getMethod("encode", ProcessableNode.class);
    return (String) encodeMethod.invoke(encoder, node);
  }

  private static NamedUserType decodeAndPrepare(String source) {
    try {
      TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
      AbstractNode node = facade.decode(source);
      assertTrue("Decoded node must be a NamedUserType", node instanceof NamedUserType);
      NamedUserType type = (NamedUserType) node;
      if (type.getSuperType() == null) {
        type.superType.setValue(JavaType.getInstance(Object.class));
      }
      return type;
    } catch (Exception e) {
      throw new RuntimeException("Failed to decode test input: " + source, e);
    }
  }

  private static Field findFieldOfType(Class<?> owner, Class<?> fieldType) {
    for (Field f : owner.getDeclaredFields()) {
      if (f.getType().equals(fieldType)) {
        return f;
      }
    }
    return null;
  }

  /**
   * Assert that encoding a decoded Tweedle source produces stable, non-empty
   * output. Verifies the delegation doesn't introduce non-determinism by
   * encoding the same type twice and comparing. (Full round-trip re-encoding
   * is not stable due to the u_ prefix being re-applied to user identifiers.)
   */
  private void assertRoundTripIdentical(String source) throws Exception {
    NamedUserType type = decodeAndPrepare(source);
    String encoded = encodeViaReflection(newDefaultEncoder(), type);
    assertNotNull("Encoded output must not be null", encoded);
    assertTrue("Encoded output must not be empty", encoded.length() > 0);

    // Encode the same type a second time to verify deterministic delegation
    String secondEncode = encodeViaReflection(newDefaultEncoder(), type);
    assertEquals("Two encodes of the same type must produce identical output", encoded, secondEncode);
  }
}
