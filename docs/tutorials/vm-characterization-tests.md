# Tutorial: Trace the VM Characterization Tests

This tutorial walks through the five virtual machine characterization test
suites. You will trace how expression evaluation, statement execution, field
access, error handling, and Story API dispatch are tested through the headless
VM.

For the full contract, see the [VM Characterization Tests
reference](../reference/vm-characterization-tests.md).

## Contents

- [Goal](#goal)
- [1. Understand the test architecture](#1-understand-the-test-architecture)
- [2. Trace expression evaluation](#2-trace-expression-evaluation)
- [3. Trace statement execution](#3-trace-statement-execution)
- [4. Trace field access](#4-trace-field-access)
- [5. Trace error handling](#5-trace-error-handling)
- [6. Trace Story API dispatch](#6-trace-story-api-dispatch)
- [7. Run the tests](#7-run-the-tests)
- [8. Understand the boundaries](#8-understand-the-boundaries)

## Goal

Understand how the five characterization test suites document the VM's behavior
at each layer of the execution engine. After this tutorial you will be able to
explain:

- How expression values flow from AST nodes through `evaluate()` to test assertions
- Why statement events form a balanced stack
- How `UserInstance` + `UserField` enable field access characterization
- What error conditions the VM handles and how they are observable
- How Story API adapter registration enables dispatch testing

Open these source files alongside this guide:

```text
core/ast/src/test/java/org/lgna/project/virtualmachine/VmExpressionEvaluationCharacterizationTest.java
core/ast/src/test/java/org/lgna/project/virtualmachine/VmStatementExecutionCharacterizationTest.java
core/ast/src/test/java/org/lgna/project/virtualmachine/VmFieldAccessCharacterizationTest.java
core/ast/src/test/java/org/lgna/project/virtualmachine/VmErrorHandlingCharacterizationTest.java
core/ast/src/test/java/org/lgna/project/virtualmachine/VmStoryApiDispatchCharacterizationTest.java
```

## 1. Understand the test architecture

All five suites share a common pattern inherited from the silver thread tests:

```text
1. Create a NamedUserType  (satisfies AbstractCode.isValid())
2. Build AST nodes          (expressions, statements, fields)
3. Wire into a UserMethod   (static for simple tests, instance for field tests)
4. Add method to type       (required before VM execution)
5. Execute via VM            (ENTRY_POINT_invoke or ENTRY_POINT_createInstance)
6. Assert observable output  (return value, listener events, or exception)
```

**Observable output strategy.** The tests use two complementary observation
mechanisms:

| Mechanism | Used by | How it works |
| --- | --- | --- |
| Function return value | Expression, field access tests | Wrap the expression in `ReturnStatement` inside a function `UserMethod(name, ReturnType, ...)`. Call `ENTRY_POINT_invoke`; the return value is the evaluated expression. |
| Statement listener events | Statement, error tests | Register a `RecordingVirtualMachineListener`; assert the sequence of `"executing:Type"` / `"executed:Type"` strings. |

This separates expression semantics from listener dispatch. An expression test
that fails tells you the evaluator is wrong. A statement test that fails tells
you the dispatcher is wrong.

## 2. Trace expression evaluation

Open `VmExpressionEvaluationCharacterizationTest.java`. Find the integer literal test:

```java
UserMethod function = createStaticFunction("intLitFn", Integer.class,
    new ReturnStatement(new IntegerLiteral(42)));
type.methods.add(function);

Object result = vm.ENTRY_POINT_invoke(null, function);
assertEquals(42, result);
```

**What happens inside the VM:**

1. `ENTRY_POINT_invoke(null, function)` calls `invokeUserMethod(null, function)`.
2. `invokeUserMethod` pushes a method frame, then calls `execute(body)`.
3. `execute` dispatches to `executeReturnStatement`, which calls
   `evaluate(IntegerLiteral(42))`.
4. `evaluate` hits `case IntegerLiteral` in the switch, calling
   `evaluateIntegerLiteral`, which returns `42`.
5. `executeReturnStatement` throws `ReturnException(42)`.
6. `invokeUserMethod` catches `ReturnException`, returns `42`.

The test asserts the final return value — it does not need a listener.

**Arithmetic expressions** chain two literals through an operator:

```java
ArithmeticInfixExpression expr = new ArithmeticInfixExpression(
    new IntegerLiteral(2),
    ArithmeticInfixExpression.Operator.PLUS,
    new IntegerLiteral(3),
    Integer.class);
```

The VM evaluates left, evaluates right, calls `operator.operate(2, 3)`, returns `5`.

**Short-circuit conditionals** test that the VM does not evaluate the right
operand when the left determines the result:

```java
ConditionalInfixExpression expr = new ConditionalInfixExpression(
    new BooleanLiteral(false),
    ConditionalInfixExpression.Operator.AND,
    sideEffectExpression);  // not evaluated
```

Lines 594–599 of `VirtualMachine.java` show the AND short-circuit: when `operator` is `AND` and `leftOperand` is `false`, the method returns `false` without evaluating the right operand.

## 3. Trace statement execution

Open `VmStatementExecutionCharacterizationTest.java`. Find the count loop test:

```java
UserLocal variable = new UserLocal("i", Integer.class, false);
UserLocal constant = new UserLocal("n", Integer.class, true);
CountLoop loop = new CountLoop(variable, constant,
    new IntegerLiteral(3), new BlockStatement(new Comment("iter")));
```

**What happens inside the VM** (VirtualMachine.java line 869):

1. `executeCountLoop` pushes `variable` as a local (initial value -1).
2. Evaluates the count expression → `3`.
3. Pushes `constant` as a local (value 3).
4. Loops `i = 0, 1, 2`:
   - Fires `countLoopIterating` event.
   - Sets local `variable` to `i`.
   - Calls `execute(body)` → fires `executing:BlockStatement`, `executing:Comment`, `executed:Comment`, `executed:BlockStatement`.
   - Fires `countLoopIterated` event.
5. Pops `constant`, pops `variable`.

The test asserts that the `Comment` executing/executed pair appears exactly 3
times inside the outer block events.

**Disabled statement** tests the `isEnabled` guard at line 1110:

```java
Comment disabled = new Comment("skip me");
disabled.isEnabled.setValue(false);
```

When `execute(disabled)` is called, the `if (statement.isEnabled.getValue())`
check fails, so no events fire and no dispatch occurs.

**DoTogether with 0-1 statements** tests the deterministic paths (lines 913-918):

```java
// 0 statements: case 0 → break
DoTogether empty = new DoTogether(new BlockStatement());

// 1 statement: case 1 → execute directly (no threads)
DoTogether single = new DoTogether(new BlockStatement(new Comment("solo")));
```

## 4. Trace field access

Open `VmFieldAccessCharacterizationTest.java`. Field tests require a
`UserInstance`, which requires the full constructor chain:

```java
// Build constructor chain
NamedUserConstructor constructor = new NamedUserConstructor();
SuperConstructorInvocationStatement superCall = new SuperConstructorInvocationStatement();
superCall.contructorDeclaredInJava.setValue(
    JavaConstructor.getInstance(Object.class));
ConstructorBlockStatement constructorBody = new ConstructorBlockStatement(superCall);
constructor.body.setValue(constructorBody);
type.constructors.add(constructor);

// Create instance
UserInstance instance = vm.ENTRY_POINT_createInstance(constructor);
```

Now field access works through `ThisExpression`:

```java
UserField field = new UserField("score", Integer.class);
type.fields.add(field);

// Write: score = 42
AssignmentExpression assign = new AssignmentExpression(
    new FieldAccess(new ThisExpression(), field),
    AssignmentExpression.Operator.ASSIGN,
    new IntegerLiteral(42));

// Read: return this.score
ReturnStatement read = new ReturnStatement(
    new FieldAccess(new ThisExpression(), field));
```

**Inside the VM** (line 513): `evaluateAssignmentExpression` checks the
left-hand side type. For `FieldAccess`, it calls `this.set(field,
evaluate(expression), value)`, which delegates to `setUserField` →
`UserInstance.setFieldValue`.

## 5. Trace error handling

Open `VmErrorHandlingCharacterizationTest.java`. Find the null-operand test:

```java
RelationalInfixExpression expr = new RelationalInfixExpression(
    new IntegerLiteral(1),
    RelationalInfixExpression.Operator.LESS,
    new NullLiteral(),
    Integer.class, Integer.class);
```

**Inside the VM** (line 612): `evaluateRelationalInfixExpression` evaluates both
operands. When `rightOperand` is `null`, line 619 throws:

```java
throw new LgnaVmNullPointerException("right operand is null.", this);
```

The test uses `@Test(expected = LgnaVmNullPointerException.class)` or
`assertThrows` to verify the exception.

**Missing return** tests the guard at lines 429–432:

```java
UserMethod function = new UserMethod("noReturn", Integer.class, ...);
// body is BlockStatement(Comment("oops")) — no ReturnStatement
```

`invokeUserMethod` executes the body. The body completes without throwing
`ReturnException`. Line 429 checks `method.isProcedure()` — it's `false`
(return type is `Integer`), so line 432 throws `LgnaVmNoReturnException`.

**Invalid method invocation** tests the guard at line 648:

```java
MethodInvocation call = new MethodInvocation(new NullLiteral(), orphanMethod);
// orphanMethod has no declaring type → isValid() returns false
```

`evaluateMethodInvocation` checks `methodInvocation.isValid()` at line 648.
Since the method has no declaring type, `isValid()` returns `false`, and the
method logs a `Logger.severe` message and returns `null` — no exception.

## 6. Trace Story API dispatch

Open `VmStoryApiDispatchCharacterizationTest.java`. The adapter registration
test:

```java
vm.registerAbstractClassAdapter(MyInterface.class, MyAdapter.class);
```

Where `MyAdapter` is a test-only concrete class with a constructor matching
`(MethodContext, UserType, Object[])`. When the VM encounters a protected method
call on `MyInterface`, it looks up `MyAdapter` in `mapAbstractClsToAdapterCls`
and reflectively dispatches through the adapter.

The `createInstance` test builds the full constructor chain:

```java
NamedUserConstructor constructor = ...;
// SuperConstructorInvocationStatement → JavaConstructor.getInstance(Object.class)
UserInstance instance = vm.ENTRY_POINT_createInstance(constructor);
assertNotNull(instance);
```

`ENTRY_POINT_createInstance` resolves the constructor, pushes a constructor
frame, executes the `ConstructorBlockStatement`, and returns a new
`UserInstance`.

## 7. Run the tests

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='VmExpressionEvaluationCharacterizationTest,VmStatementExecutionCharacterizationTest,VmFieldAccessCharacterizationTest,VmErrorHandlingCharacterizationTest,VmStoryApiDispatchCharacterizationTest' \
  test
```

All test methods pass without a display server or network access.

## 8. Understand the boundaries

| Question | Answer |
| --- | --- |
| Does this prove rendering works? | No. No scene graph, no display. |
| Does this prove save/reopen works? | No. No serialization. |
| Does this prove drag-and-drop works? | No. No Swing/JavaFX. |
| Does this prove instance methods from loaded projects work? | No. Methods are either static or invoked on test-built instances. |
| Does this prove DoTogether concurrency is correct? | No. Only 0-1 statement paths are tested. |
| Does this prove lambda evaluation works? | No. `evaluateLambdaExpression` is not covered. |
| Does this prove loop termination for arbitrary programs? | No. All loops use bounded counts ≤ 5. |
| What does it prove? | The VM correctly evaluates expressions, executes statements, accesses fields, handles errors, and dispatches Story API methods through the headless `ReleaseVirtualMachine` engine. |

The five suites are intentionally focused on the execution engine layer. Each
test exercises one VM code path. Broader tests in `core/ide` (like
`SilverThreadLaunchBuildRunTest`) build on this foundation by adding
save/reopen and starter-project loading around the same VM execution pattern.
