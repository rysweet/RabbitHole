package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.AssignmentExpression;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.WhileLoop;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Boundary tests for StatementDecoder targeting untested paths:
 * empty void/non-void methods, local/field assignments, while-in-non-void error,
 * if/else bodies, constructor validation, and constructor field assignments.
 *
 * <p>All tests drive through TweedleEncoderDecoder.decode() using
 * synthetic Tweedle class source strings.
 */
public class StatementDecoderBoundaryTest {
  private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

  // ═══════════════════════════════════════════════════════════════════════════
  // EMPTY METHOD BODIES: void vs. non-void
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void emptyVoidMethodDecodesEmptyBlockStatement() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { void doNothing() { } }");

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("doNothing", method.getName());
    assertEquals(0, method.body.getValue().statements.size());
  }

  @Test
  public void emptyNonVoidMethodRejectsWithError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode(
            "class SyntheticType { WholeNumber bad() { } }"));

    assertTrue(thrown.getMessage().contains("return"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LOCAL VARIABLE DECLARATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void localVariableDeclarationDecodesWithCorrectType() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber temp <- 42;
            return temp;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(2, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    LocalDeclarationStatement local = (LocalDeclarationStatement) method.body.getValue().statements.get(0);
    assertEquals("temp", local.local.getValue().getName());
    assertTrue(method.body.getValue().statements.get(1) instanceof ReturnStatement);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ASSIGNMENT STATEMENTS: local and field
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void localAssignmentStatementDecodesInMethodBody() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber compute() {
            WholeNumber temp <- 1;
            temp <- 2;
            return temp;
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(3, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof LocalDeclarationStatement);
    assertTrue(method.body.getValue().statements.get(2) instanceof ReturnStatement);
  }

  @Test
  public void fieldAssignmentByIdentifierDecodesInMethodBody() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void increment() { count <- 1; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals("increment", method.getName());
    assertEquals(1, method.body.getValue().statements.size());
  }

  @Test
  public void thisFieldAssignmentDecodesInMethodBody() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void increment() { this.count <- 1; }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
  }

  @Test
  public void unknownAssignmentTargetRejectsWithError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void bad() { ghostTarget <- 1; }
            }
            """));

    assertTrue(thrown.getMessage().contains("ghostTarget"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // WHILE LOOP: void-only constraint
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void whileLoopInVoidMethodDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void process(Boolean running) {
            while (running) { count <- 1; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof WhileLoop);
    WhileLoop whileLoop = (WhileLoop) method.body.getValue().statements.get(0);
    assertEquals(1, whileLoop.body.getValue().statements.size());
  }

  @Test
  public void whileLoopInNonVoidMethodRejectsWithError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              WholeNumber bad(Boolean running) {
                while (running) { count <- 1; }
                return count;
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("while"));
    assertTrue(thrown.getMessage().contains("void"));
    assertTrue(thrown.getMessage().contains("bad"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IF/ELSE: simple if and if/else with assignment-only bodies
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void simpleIfWithFieldAssignmentDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void update(Boolean flag) {
            if (flag) { count <- 1; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ConditionalStatement);
    ConditionalStatement cond = (ConditionalStatement) method.body.getValue().statements.get(0);
    assertEquals(1, cond.booleanExpressionBodyPairs.size());
    assertEquals(1, cond.booleanExpressionBodyPairs.get(0).body.getValue().statements.size());
    assertEquals(0, cond.elseBody.getValue().statements.size());
  }

  @Test
  public void ifElseWithAssignmentBodiesDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          void update(Boolean flag) {
            if (flag) { count <- 1; } else { count <- 2; }
          }
        }
        """);

    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(1, method.body.getValue().statements.size());
    ConditionalStatement cond = (ConditionalStatement) method.body.getValue().statements.get(0);
    assertEquals(1, cond.booleanExpressionBodyPairs.get(0).body.getValue().statements.size());
    assertEquals(1, cond.elseBody.getValue().statements.size());
  }

  @Test
  public void simpleIfWithMethodCallDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void update(Boolean flag) {
            if (flag) { this.helper(); }
          }
          void helper() { }
        }
        """);

    UserMethod method = userMethodNamed(type, "update");
    ConditionalStatement cond = (ConditionalStatement) method.body.getValue().statements.get(0);
    BooleanExpressionBodyPair pair = cond.booleanExpressionBodyPairs.get(0);
    assertEquals(1, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) pair.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONSTRUCTOR: empty, field assignment, this.field assignment
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void emptyConstructorDecodesWithSuperInvocation() throws Exception {
    NamedUserType type = decodeUserType(
        "class SyntheticType { SyntheticType() { } }");

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    ConstructorBlockStatement body = constructor.body.getValue();
    assertNotNull(body);
    assertTrue(body.constructorInvocationStatement.getValue() instanceof SuperConstructorInvocationStatement);
    assertEquals(0, body.statements.size());
  }

  @Test
  public void constructorWithFieldAssignmentDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { count <- 5; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    ConstructorBlockStatement body = constructor.body.getValue();
    assertTrue(body.constructorInvocationStatement.getValue() instanceof SuperConstructorInvocationStatement);
    assertEquals(1, body.statements.size());
  }

  @Test
  public void constructorWithThisFieldAssignmentDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType() { this.count <- 7; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    ConstructorBlockStatement body = constructor.body.getValue();
    assertTrue(body.constructorInvocationStatement.getValue() instanceof SuperConstructorInvocationStatement);
    assertEquals(1, body.statements.size());
  }

  @Test
  public void constructorWithParameterAssignmentDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          WholeNumber count <- 0;
          SyntheticType(WholeNumber initial) { count <- initial; }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    ConstructorBlockStatement body = constructor.body.getValue();
    assertEquals(1, body.statements.size());
    assertEquals(1, constructor.getRequiredParameters().size());
  }

  @Test
  public void constructorWithMethodCallDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          SyntheticType() { this.helper(); }
          void helper() { }
        }
        """);

    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    ConstructorBlockStatement body = constructor.body.getValue();
    assertEquals(1, body.statements.size());
  }

  @Test
  public void constructorNameMismatchReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WrongName() { }
            }
            """));

    assertTrue(thrown.getMessage().contains("constructor name"));
    assertTrue(thrown.getMessage().contains("WrongName"));
  }

  @Test
  public void constructorWithUnsupportedStatementReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              SyntheticType(Boolean flag) {
                if (flag) { }
              }
            }
            """));

    assertTrue(thrown.getMessage().contains("constructor"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ZERO-ARGUMENT METHOD CALLS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void implicitSameClassMethodCallDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void run() { helper(); }
          void helper() { }
        }
        """);

    UserMethod run = userMethodNamed(type, "run");
    assertEquals(1, run.body.getValue().statements.size());
    assertTrue(run.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement stmt = (ExpressionStatement) run.body.getValue().statements.get(0);
    assertTrue(stmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) stmt.expression.getValue();
    assertEquals("helper", invocation.method.getValue().getName());
  }

  @Test
  public void explicitThisMethodCallDecodes() throws Exception {
    NamedUserType type = decodeUserType("""
        class SyntheticType {
          void run() { this.helper(); }
          void helper() { }
        }
        """);

    UserMethod run = userMethodNamed(type, "run");
    assertEquals(1, run.body.getValue().statements.size());
  }

  @Test
  public void methodCallToUnknownMethodReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              void run() { this.nonExistent(); }
            }
            """));

    assertTrue(thrown.getMessage().contains("nonExistent"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // NON-VOID METHOD WITHOUT RETURN
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void nonVoidMethodWithoutReturnAsLastStatementReportsError() {
    UnsupportedTweedleDecodeException thrown = assertThrows(
        UnsupportedTweedleDecodeException.class,
        () -> coder.decode("""
            class SyntheticType {
              WholeNumber count <- 0;
              WholeNumber bad() { count <- 1; }
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

  private static UserMethod userMethodNamed(NamedUserType type, String name) {
    for (UserMethod method : type.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    throw new AssertionError("Method not found: " + name);
  }
}
