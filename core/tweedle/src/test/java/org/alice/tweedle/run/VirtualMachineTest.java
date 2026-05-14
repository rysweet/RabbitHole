package org.alice.tweedle.run;

import org.alice.tweedle.*;
import org.alice.tweedle.ast.TweedleExpression;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Characterization tests for VirtualMachine after dead-code removal.
 * These pin the contract of each public and protected method to prevent
 * regressions during future refactoring.
 *
 * No mocking framework — test doubles are hand-rolled inner classes.
 */
public class VirtualMachineTest {

  // ── Concrete subclass (VirtualMachine is abstract) ───────────────
  private static class TestableVM extends VirtualMachine {
    // Expose protected methods for direct testing
    public TweedleValue callGet(TweedleField f, TweedleObject o) {
      return get(f, o);
    }

    public void callSet(TweedleField f, TweedleObject o, TweedleValue v) {
      set(f, o, v);
    }

    public Object callInvoke(Frame frame, TweedleObject target, TweedleMethod method, TweedleValue... args) {
      return invoke(frame, target, method, args);
    }

    public TweedleValue callEvaluate(Frame frame, TweedleExpression expr) {
      return evaluate(frame, expr);
    }

    public void callExecute(Frame frame, TweedleStatement stmt) {
      execute(frame, stmt);
    }
  }

  // ── Recording test doubles ───────────────────────────────────────

  /** Expression that records its evaluation and returns a fixed value. */
  private static class SpyExpression extends TweedleExpression {
    final TweedleValue result;
    boolean evaluated = false;
    Frame capturedFrame;

    SpyExpression(TweedleValue result) {
      super();
      this.result = result;
    }

    @Override
    public TweedleValue evaluate(Frame frame) {
      evaluated = true;
      capturedFrame = frame;
      return result;
    }
  }

  /** Statement that records whether execute was called. */
  private static class SpyStatement extends TweedleStatement {
    boolean executeCalled = false;
    Frame capturedFrame;

    @Override
    public void execute(Frame frame) {
      if (isEnabled()) {
        executeCalled = true;
        capturedFrame = frame;
      }
    }
  }

  /** TweedleMethod that records invocations. */
  private static class SpyMethod extends TweedleMethod {
    boolean invoked = false;
    Frame capturedFrame;
    TweedleObject capturedTarget;
    TweedleValue[] capturedArgs;
    TweedleValue returnValue;

    SpyMethod(TweedleValue returnValue) {
      super(new TweedleType("Void"), "testMethod",
          Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
      this.returnValue = returnValue;
    }

    @Override
    public TweedleValue invoke(Frame frame, TweedleObject target, TweedleValue[] arguments) {
      invoked = true;
      capturedFrame = frame;
      capturedTarget = target;
      capturedArgs = arguments;
      return returnValue;
    }
  }

  /** Minimal concrete TweedleValue for test fixtures. */
  private static class StubValue extends TweedleValue {
    StubValue() {
      super(new TweedleType("StubType"));
    }
  }

  // ── Fixtures ─────────────────────────────────────────────────────

  private TestableVM vm;
  private TweedleClass testClass;
  private TweedleObject instance;

  @Before
  public void setUp() {
    vm = new TestableVM();
    testClass = new TweedleClass("TestClass",
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    instance = new TweedleObject(testClass);
  }

  // ═══════════════════════════════════════════════════════════════════
  // ENTRY_POINT_evaluate
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void entryPointEvaluate_emptyArray_returnsEmptyArray() {
    Object[] results = vm.ENTRY_POINT_evaluate(instance, new TweedleExpression[0]);
    assertNotNull("Result array must not be null", results);
    assertEquals("Empty input → empty output", 0, results.length);
  }

  @Test
  public void entryPointEvaluate_singleExpression_returnsEvaluatedValue() {
    StubValue expected = new StubValue();
    SpyExpression expr = new SpyExpression(expected);

    Object[] results = vm.ENTRY_POINT_evaluate(instance, new TweedleExpression[]{expr});

    assertTrue("Expression must be evaluated", expr.evaluated);
    assertEquals(1, results.length);
    assertSame("Result must be the value returned by evaluate()", expected, results[0]);
  }

  @Test
  public void entryPointEvaluate_multipleExpressions_evaluatesAllInOrder() {
    StubValue v1 = new StubValue();
    StubValue v2 = new StubValue();
    StubValue v3 = new StubValue();
    SpyExpression e1 = new SpyExpression(v1);
    SpyExpression e2 = new SpyExpression(v2);
    SpyExpression e3 = new SpyExpression(v3);

    Object[] results = vm.ENTRY_POINT_evaluate(instance,
        new TweedleExpression[]{e1, e2, e3});

    assertEquals(3, results.length);
    assertSame(v1, results[0]);
    assertSame(v2, results[1]);
    assertSame(v3, results[2]);
    assertTrue(e1.evaluated);
    assertTrue(e2.evaluated);
    assertTrue(e3.evaluated);
  }

  @Test
  public void entryPointEvaluate_nullInstance_doesNotThrow() {
    StubValue expected = new StubValue();
    SpyExpression expr = new SpyExpression(expected);

    Object[] results = vm.ENTRY_POINT_evaluate(null, new TweedleExpression[]{expr});

    assertEquals(1, results.length);
    assertSame(expected, results[0]);
  }

  // ═══════════════════════════════════════════════════════════════════
  // ENTRY_POINT_invoke
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void entryPointInvoke_delegatesToMethod() {
    StubValue retVal = new StubValue();
    SpyMethod method = new SpyMethod(retVal);
    StubValue arg1 = new StubValue();

    vm.ENTRY_POINT_invoke(instance, method, arg1);

    assertTrue("Method must be invoked", method.invoked);
    assertSame("Target must be the instance", instance, method.capturedTarget);
    assertEquals("Arguments count", 1, method.capturedArgs.length);
    assertSame(arg1, method.capturedArgs[0]);
  }

  @Test
  public void entryPointInvoke_noArguments() {
    SpyMethod method = new SpyMethod(null);

    vm.ENTRY_POINT_invoke(instance, method);

    assertTrue(method.invoked);
    assertEquals(0, method.capturedArgs.length);
  }

  // ═══════════════════════════════════════════════════════════════════
  // ENTRY_POINT_createInstance
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void entryPointCreateInstance_returnsNewObject() {
    TweedleObject created = vm.ENTRY_POINT_createInstance(testClass);

    assertNotNull("Must return a new TweedleObject", created);
  }

  @Test
  public void entryPointCreateInstance_objectHasCorrectType() {
    TweedleObject created = vm.ENTRY_POINT_createInstance(testClass);

    // TweedleObject extends TweedleValue which has getType()
    assertSame("Created object must reference the class", testClass, created.getType());
  }

  @Test
  public void entryPointCreateInstance_acceptsVarargs() {
    StubValue arg1 = new StubValue();
    StubValue arg2 = new StubValue();

    TweedleObject created = vm.ENTRY_POINT_createInstance(testClass, arg1, arg2);
    assertNotNull(created);
  }

  // ═══════════════════════════════════════════════════════════════════
  // createAndSetFieldInstance
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void createAndSetFieldInstance_delegatesToInitializeField() {
    TweedleField field = new TweedleField(
        Collections.emptyList(), new TweedleType("Number"), "count");
    Frame frame = new Frame(instance);

    TweedleValue result = vm.createAndSetFieldInstance(frame, instance, field);

    // TweedleObject.initializeField returns null — the contract is delegation to instance
    assertNull("initializeField returns null for uninitialized field", result);
  }

  // ═══════════════════════════════════════════════════════════════════
  // ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void hackInitializeField_createsFrameAndDelegates() {
    TweedleField field = new TweedleField(
        Collections.emptyList(), new TweedleType("Number"), "x");

    Object result = vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);

    // Delegates through createAndSetFieldInstance → instance.initializeField
    assertNull("initializeField returns null for uninitialized field", result);
  }

  // ═══════════════════════════════════════════════════════════════════
  // ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void hackExecuteStatement_executesEnabledStatement() {
    SpyStatement stmt = new SpyStatement();

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(instance, stmt);

    assertTrue("Enabled statement must be executed", stmt.executeCalled);
  }

  @Test
  public void hackExecuteStatement_skipsDisabledStatement() {
    SpyStatement stmt = new SpyStatement();
    stmt.disable();

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(instance, stmt);

    assertFalse("Disabled statement must NOT be executed", stmt.executeCalled);
  }

  // ═══════════════════════════════════════════════════════════════════
  // Protected: execute(Frame, TweedleStatement)
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void execute_enabledStatement_callsExecute() {
    SpyStatement stmt = new SpyStatement();
    Frame frame = new Frame(instance);

    vm.callExecute(frame, stmt);

    assertTrue("execute() must call statement.execute() when enabled", stmt.executeCalled);
    assertSame("Frame must be passed through", frame, stmt.capturedFrame);
  }

  @Test
  public void execute_disabledStatement_doesNotCallExecute() {
    SpyStatement stmt = new SpyStatement();
    stmt.disable();
    Frame frame = new Frame(instance);

    vm.callExecute(frame, stmt);

    assertFalse("execute() must skip disabled statements", stmt.executeCalled);
  }

  // ═══════════════════════════════════════════════════════════════════
  // Protected: evaluate(Frame, TweedleExpression)
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void evaluate_delegatesToExpression() {
    StubValue expected = new StubValue();
    SpyExpression expr = new SpyExpression(expected);
    Frame frame = new Frame(instance);

    TweedleValue result = vm.callEvaluate(frame, expr);

    assertTrue(expr.evaluated);
    assertSame(expected, result);
    assertSame(frame, expr.capturedFrame);
  }

  // ═══════════════════════════════════════════════════════════════════
  // Protected: invoke(Frame, TweedleObject, TweedleMethod, ...)
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void protectedInvoke_delegatesToMethod() {
    StubValue retVal = new StubValue();
    SpyMethod method = new SpyMethod(retVal);
    Frame frame = new Frame(instance);
    StubValue arg = new StubValue();

    Object result = vm.callInvoke(frame, instance, method, arg);

    assertTrue(method.invoked);
    assertSame(retVal, result);
    assertSame(frame, method.capturedFrame);
    assertSame(instance, method.capturedTarget);
  }

  // ═══════════════════════════════════════════════════════════════════
  // Protected: get / set
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void get_delegatesToInstance() {
    TweedleField field = new TweedleField(
        Collections.emptyList(), new TweedleType("Number"), "x");

    TweedleValue result = vm.callGet(field, instance);

    // TweedleObject.get() is a stub returning null
    assertNull(result);
  }

  @Test
  public void set_delegatesToInstance_doesNotThrow() {
    TweedleField field = new TweedleField(
        Collections.emptyList(), new TweedleType("Number"), "x");
    StubValue value = new StubValue();

    // Must not throw — TweedleObject.set() is a no-op stub
    vm.callSet(field, instance, value);
  }

  // ═══════════════════════════════════════════════════════════════════
  // Subclass override interception points
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void subclass_canOverrideEvaluate() {
    StubValue override = new StubValue();
    VirtualMachine overrideVM = new VirtualMachine() {
      @Override
      protected TweedleValue evaluate(Frame frame, TweedleExpression expression) {
        return override;
      }
    };

    StubValue original = new StubValue();
    SpyExpression expr = new SpyExpression(original);
    Object[] results = overrideVM.ENTRY_POINT_evaluate(instance, new TweedleExpression[]{expr});

    assertSame("Subclass override must be used", override, results[0]);
    assertFalse("Original evaluate must NOT be called", expr.evaluated);
  }

  @Test
  public void subclass_canOverrideExecute() {
    final boolean[] overrideCalled = {false};
    VirtualMachine overrideVM = new VirtualMachine() {
      @Override
      protected void execute(Frame frame, TweedleStatement statement) {
        overrideCalled[0] = true;
      }
    };

    SpyStatement stmt = new SpyStatement();
    overrideVM.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(instance, stmt);

    assertTrue("Subclass execute override must be called", overrideCalled[0]);
    assertFalse("Original statement.execute must NOT be called", stmt.executeCalled);
  }

  @Test
  public void subclass_canOverrideInvoke() {
    final Object[] captured = {null};
    VirtualMachine overrideVM = new VirtualMachine() {
      @Override
      protected Object invoke(Frame frame, TweedleObject target, TweedleMethod method, TweedleValue... arguments) {
        captured[0] = target;
        return null;
      }
    };

    SpyMethod method = new SpyMethod(null);
    overrideVM.ENTRY_POINT_invoke(instance, method);

    assertSame("Override must receive the target", instance, captured[0]);
    assertFalse("Original method.invoke must NOT be called", method.invoked);
  }
}
