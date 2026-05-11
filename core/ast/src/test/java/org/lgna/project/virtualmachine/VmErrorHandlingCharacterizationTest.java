package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaConstructor;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.WhileLoop;
import org.lgna.project.virtualmachine.events.CountLoopIterationEvent;
import org.lgna.project.virtualmachine.events.EachInTogetherItemEvent;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.ForEachLoopIterationEvent;
import org.lgna.project.virtualmachine.events.StatementExecutionEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;
import org.lgna.project.virtualmachine.events.WhileLoopIterationEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Characterization tests for VirtualMachine error handling behavior.
 * Documents the current behavior for null pointer exceptions, missing
 * return statements, null operands, disabled statements, and invalid
 * method invocations.
 *
 * <p>Headless scope: no JavaFX, no gallery assets, no scene graph, no display.
 *
 * @see VirtualMachine#evaluate(Expression)
 * @see VirtualMachine#invokeUserMethod(Object, UserMethod, Object...)
 */
public class VmErrorHandlingCharacterizationTest {

  private ReleaseVirtualMachine vm;
  private NamedUserType type;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
    type = createProgramType();
  }

  // --- Null expression → NullPointerException ---

  @Test(expected = NullPointerException.class)
  public void evaluateNullExpressionThrowsNPE() {
    vm.ENTRY_POINT_evaluate(null, new Expression[]{null});
  }

  // --- Relational expression with null operands ---

  @Test
  public void relationalWithNullRightThrowsLgnaVmNullPointerException() {
    Expression expr = new RelationalInfixExpression(
        new IntegerLiteral(1),
        RelationalInfixExpression.Operator.LESS,
        new NullLiteral(),
        Integer.class, Integer.class);
    try {
      vm.ENTRY_POINT_evaluate(null, new Expression[]{expr});
      fail("Should throw LgnaVmNullPointerException for null right operand");
    } catch (LgnaVmNullPointerException e) {
      assertEquals("right operand is null.", e.getMessage());
    }
  }

  @Test
  public void relationalWithNullLeftThrowsLgnaVmNullPointerException() {
    Expression expr = new RelationalInfixExpression(
        new NullLiteral(),
        RelationalInfixExpression.Operator.LESS,
        new IntegerLiteral(1),
        Integer.class, Integer.class);
    try {
      vm.ENTRY_POINT_evaluate(null, new Expression[]{expr});
      fail("Should throw LgnaVmNullPointerException for null left operand");
    } catch (LgnaVmNullPointerException e) {
      assertEquals("left operand is null.", e.getMessage());
    }
  }

  @Test
  public void relationalWithBothNullThrowsLgnaVmNullPointerExceptionWithBothMessage() {
    Expression expr = new RelationalInfixExpression(
        new NullLiteral(),
        RelationalInfixExpression.Operator.LESS,
        new NullLiteral(),
        Integer.class, Integer.class);
    try {
      vm.ENTRY_POINT_evaluate(null, new Expression[]{expr});
      fail("Should throw LgnaVmNullPointerException for both null operands");
    } catch (LgnaVmNullPointerException e) {
      assertEquals("left and right operands are both null.", e.getMessage());
    }
  }

  // --- Function without return statement → LgnaVmNoReturnException ---

  @Test(expected = LgnaVmNoReturnException.class)
  public void functionWithoutReturnThrowsLgnaVmNoReturnException() {
    // A function (non-void) that lacks a ReturnStatement
    UserMethod fn = new UserMethod("missingReturn", Integer.class,
        new UserParameter[0], new BlockStatement(new Comment("no return")));
    fn.isStatic.setValue(true);
    type.methods.add(fn);

    vm.ENTRY_POINT_invoke(null, fn);
  }

  // --- Procedure (void) without return is normal ---

  @Test
  public void procedureWithoutReturnReturnsNull() {
    UserMethod proc = new UserMethod("noop", Void.TYPE,
        new UserParameter[0], new BlockStatement(new Comment("no return needed")));
    proc.isStatic.setValue(true);
    type.methods.add(proc);

    Object result = vm.ENTRY_POINT_invoke(null, proc);
    assertNull("Procedure should return null without needing a ReturnStatement", result);
  }

  // --- ReturnException propagation from function ---

  @Test
  public void returnStatementPropagatesValueFromFunction() {
    ReturnStatement ret = new ReturnStatement(
        JavaType.getInstance(Integer.class), new IntegerLiteral(42));
    UserMethod fn = new UserMethod("returnsFortyTwo", Integer.class,
        new UserParameter[0], new BlockStatement(ret));
    fn.isStatic.setValue(true);
    type.methods.add(fn);

    Object result = vm.ENTRY_POINT_invoke(null, fn);
    assertEquals("ReturnStatement value should propagate through ReturnException", 42, result);
  }

  // --- Disabled statement is skipped ---

  @Test
  public void disabledStatementIsSkippedSilently() {
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    Comment disabled = new Comment("should be skipped");
    disabled.isEnabled.setValue(false);

    UserMethod method = new UserMethod("withDisabled", Void.TYPE,
        new UserParameter[0], new BlockStatement(disabled));
    method.isStatic.setValue(true);
    type.methods.add(method);

    vm.ENTRY_POINT_invoke(null, method);

    long commentEvents = listener.statementEvents.stream()
        .filter(e -> e.contains("Comment")).count();
    assertEquals("Disabled Comment should produce no events", 0, commentEvents);
  }

  // --- Invalid method invocation → Logger.severe, returns null ---

  @Test
  public void invalidMethodInvocationReturnsNullAndContinues() {
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    // Create an orphan method NOT added to type → isValid() returns false
    UserMethod orphan = new UserMethod("orphan", Void.TYPE,
        new UserParameter[0], new BlockStatement(new Comment("unreachable")));
    orphan.isStatic.setValue(true);
    // Deliberately NOT adding to type

    MethodInvocation invalidCall = new MethodInvocation(new NullLiteral(), orphan);
    ExpressionStatement callStmt = new ExpressionStatement(invalidCall);

    UserMethod entry = new UserMethod("entry", Void.TYPE,
        new UserParameter[0], new BlockStatement(callStmt));
    entry.isStatic.setValue(true);
    type.methods.add(entry);

    // Should not throw — VM logs Logger.severe and returns null
    vm.ENTRY_POINT_invoke(null, entry);

    // Orphan's body Comment should NOT fire
    long commentEvents = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Invalid method body should not execute", 0, commentEvents);
  }

  // --- Null while-loop condition → LgnaVmNullPointerException ---

  @Test
  public void nullWhileConditionThrowsLgnaVmNullPointerException() {
    WhileLoop loop = new WhileLoop(
        new NullLiteral(),
        new BlockStatement(new Comment("unreachable")));

    UserMethod method = new UserMethod("nullWhile", Void.TYPE,
        new UserParameter[0], new BlockStatement(loop));
    method.isStatic.setValue(true);
    type.methods.add(method);

    try {
      vm.ENTRY_POINT_invoke(null, method);
      fail("Should throw LgnaVmNullPointerException for null while condition");
    } catch (LgnaVmNullPointerException e) {
      assertEquals("while condition is null", e.getMessage());
    }
  }

  // --- Instance method on null target → LgnaVmNullPointerException ---

  @Test
  public void instanceMethodOnNullTargetThrowsLgnaVmNullPointerException() {
    NamedUserType instType = createTypeWithConstructor();
    UserMethod instanceMethod = new UserMethod("doSomething", Void.TYPE,
        new UserParameter[0], new BlockStatement(new Comment("body")));
    // NOT static → instance method
    instType.methods.add(instanceMethod);

    try {
      vm.ENTRY_POINT_invoke(null, instanceMethod);
      fail("Should throw LgnaVmNullPointerException for null instance target");
    } catch (LgnaVmNullPointerException e) {
      assertEquals("Instance method target is null", e.getMessage());
    }
  }

  // --- stopExecution halts loop ---

  @Test
  public void stopExecutionHaltsWhileLoop() {
    // WhileLoop with true condition — should loop forever unless stopped
    WhileLoop loop = new WhileLoop(
        new BooleanLiteral(true),
        new BlockStatement(new Comment("looping")));

    UserMethod method = new UserMethod("infiniteLoop", Void.TYPE,
        new UserParameter[0], new BlockStatement(loop));
    method.isStatic.setValue(true);
    type.methods.add(method);

    // Stop immediately — test that stopExecution flag is respected
    vm.stopExecution();
    vm.ENTRY_POINT_invoke(null, method);

    // If we reach here, the stop worked
    assertTrue("Stopped VM should exit the while loop", true);
  }

  // --- Array index out of bounds ---

  @Test
  public void arrayIndexOutOfBoundsThrowsLgnaVmException() {
    Object[] testArray = new Object[]{10, 20, 30};
    try {
      vm.getItemAtIndex(JavaType.getInstance(Integer[].class), testArray, 5);
      fail("Accessing index 5 of 3-element array should throw");
    } catch (LgnaVmArrayIndexOutOfBoundsException e) {
      // Expected: VM wraps java ArrayIndexOutOfBoundsException
    }
  }

  @Test
  public void negativeArrayIndexThrowsLgnaVmException() {
    Object[] testArray = new Object[]{1, 2};
    try {
      vm.getItemAtIndex(JavaType.getInstance(Integer[].class), testArray, -1);
      fail("Negative index should throw");
    } catch (LgnaVmArrayIndexOutOfBoundsException e) {
      // Expected
    }
  }

  @Test
  public void nullArrayThrowsNullPointerException() {
    try {
      vm.getItemAtIndex(JavaType.getInstance(Integer[].class), null, 0);
      fail("Null array should throw");
    } catch (NullPointerException e) {
      // Expected: VM checkNotNull throws NullPointerException
    }
  }

  // --- Helpers ---

  private static NamedUserType createProgramType() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("VmErrorTestProgram");
    programType.superType.setValue(JavaType.OBJECT_TYPE);
    return programType;
  }

  private static NamedUserType createTypeWithConstructor() {
    NamedUserType userType = new NamedUserType();
    userType.name.setValue("VmErrorTestInstanceType");
    userType.superType.setValue(JavaType.OBJECT_TYPE);

    JavaConstructor objectConstructor = JavaConstructor.getInstance(Object.class);
    SuperConstructorInvocationStatement superCall = new SuperConstructorInvocationStatement(objectConstructor);
    ConstructorBlockStatement constructorBody = new ConstructorBlockStatement(superCall);
    NamedUserConstructor constructor = new NamedUserConstructor(new UserParameter[0], constructorBody);
    userType.constructors.add(constructor);

    return userType;
  }

  private static class RecordingListener implements VirtualMachineListener {
    final List<String> statementEvents = new ArrayList<>();

    @Override
    public void statementExecuting(StatementExecutionEvent e) {
      statementEvents.add("executing:" + e.getStatement().getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent e) {
      statementEvents.add("executed:" + e.getStatement().getClass().getSimpleName());
    }

    @Override public void expressionEvaluated(ExpressionEvaluationEvent e) {}
    @Override public void whileLoopIterating(WhileLoopIterationEvent e) {}
    @Override public void whileLoopIterated(WhileLoopIterationEvent e) {}
    @Override public void countLoopIterating(CountLoopIterationEvent e) {}
    @Override public void countLoopIterated(CountLoopIterationEvent e) {}
    @Override public void forEachLoopIterating(ForEachLoopIterationEvent e) {}
    @Override public void forEachLoopIterated(ForEachLoopIterationEvent e) {}
    @Override public void eachInTogetherItemExecuting(EachInTogetherItemEvent e) {}
    @Override public void eachInTogetherItemExecuted(EachInTogetherItemEvent e) {}
  }
}
