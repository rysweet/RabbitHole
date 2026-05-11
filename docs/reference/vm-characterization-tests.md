# Virtual Machine Characterization Tests

Five focused characterization test suites document the `ReleaseVirtualMachine`
execution engine behavior: expression evaluation, statement execution, field
access, error handling, and Story API dispatch. Each suite is a JUnit 4 test in
`core/ast` that exercises the headless VM path without JavaFX, gallery assets,
scene rendering, or a display server.

The tests are organized by VM responsibility so each file stays under 500 lines
and each assertion maps to a single VM code path.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Test suites](#test-suites)
  - [VmExpressionEvaluationCharacterizationTest](#vmexpressionevaluationcharacterizationtest)
  - [VmStatementExecutionCharacterizationTest](#vmstatementexecutioncharacterizationtest)
  - [VmFieldAccessCharacterizationTest](#vmfieldaccesscharacterizationtest)
  - [VmErrorHandlingCharacterizationTest](#vmerrorhandlingcharacterizationtest)
  - [VmStoryApiDispatchCharacterizationTest](#vmstoryapidispatchcharacterizationtest)
- [Design decisions](#design-decisions)
- [Usage](#usage)
- [Behavior contracts](#behavior-contracts)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Relationship to issue #504](#relationship-to-issue-504)
- [Claim boundaries](#claim-boundaries)
- [Troubleshooting](#troubleshooting)

## Scope

These tests cover five responsibility areas of the VM execution engine:

```text
1. Expression evaluation  — literals, arithmetic, conditional, relational,
                            logical, string concatenation
2. Statement execution    — blocks, conditionals, loops (count, while, forEach),
                            doInOrder, doTogether, return, local declarations
3. Field access           — UserField get/set, FieldAccess, AssignmentExpression,
                            ParameterAccess, LocalAccess
4. Error handling         — LgnaVmNullPointerException, null operands,
                            ReturnException/LgnaVmNoReturnException flow,
                            disabled statement skip
5. Story API dispatch     — adapter class registration, ENTRY_POINT_createInstance,
                            MethodContext callback routing
```

All five suites reuse the same AST construction patterns established by
`SilverThreadVirtualMachineExecutionTest`: static `UserMethod` bodies wired to
a `NamedUserType`, executed through `ReleaseVirtualMachine.ENTRY_POINT_invoke`.

## Implementation status

| Test suite | Location |
| --- | --- |
| `VmExpressionEvaluationCharacterizationTest` | `core/ast/src/test/java/org/lgna/project/virtualmachine/VmExpressionEvaluationCharacterizationTest.java` |
| `VmStatementExecutionCharacterizationTest` | `core/ast/src/test/java/org/lgna/project/virtualmachine/VmStatementExecutionCharacterizationTest.java` |
| `VmFieldAccessCharacterizationTest` | `core/ast/src/test/java/org/lgna/project/virtualmachine/VmFieldAccessCharacterizationTest.java` |
| `VmErrorHandlingCharacterizationTest` | `core/ast/src/test/java/org/lgna/project/virtualmachine/VmErrorHandlingCharacterizationTest.java` |
| `VmStoryApiDispatchCharacterizationTest` | `core/ast/src/test/java/org/lgna/project/virtualmachine/VmStoryApiDispatchCharacterizationTest.java` |
| Maven validation | `mvn -pl core/ast -am -Dtest=VmExpressionEvaluationCharacterizationTest,VmStatementExecutionCharacterizationTest,VmFieldAccessCharacterizationTest,VmErrorHandlingCharacterizationTest,VmStoryApiDispatchCharacterizationTest test` |

## Test suites

### VmExpressionEvaluationCharacterizationTest

Documents how `VirtualMachine.evaluate(Expression)` processes each expression
node type. Each test method builds a minimal AST containing one expression,
wraps it in a function `UserMethod` that returns the evaluated result via
`ReturnStatement`, and asserts the return value from `ENTRY_POINT_invoke`.

| Test method | Expression node | VM method | Expected result |
| --- | --- | --- | --- |
| `integerLiteralEvaluatesToValue` | `IntegerLiteral(42)` | `evaluateIntegerLiteral` | `42` |
| `doubleLiteralEvaluatesToValue` | `DoubleLiteral(3.14)` | `evaluateDoubleLiteral` | `3.14` |
| `booleanLiteralEvaluatesToValue` | `BooleanLiteral(true)` | `evaluateBooleanLiteral` | `true` |
| `stringLiteralEvaluatesToValue` | `StringLiteral("hello")` | `evaluateStringLiteral` | `"hello"` |
| `nullLiteralEvaluatesToNull` | `NullLiteral` | `evaluateNullLiteral` | `null` |
| `arithmeticAddition` | `ArithmeticInfixExpression(2 + 3)` | `evaluateArithmeticInfixExpression` | `5` |
| `arithmeticSubtraction` | `ArithmeticInfixExpression(10 - 4)` | `evaluateArithmeticInfixExpression` | `6` |
| `arithmeticMultiplication` | `ArithmeticInfixExpression(3 * 7)` | `evaluateArithmeticInfixExpression` | `21` |
| `arithmeticDivision` | `ArithmeticInfixExpression(10 / 2)` | `evaluateArithmeticInfixExpression` | `5` |
| `relationalLessThanTrue` | `RelationalInfixExpression(1 < 2)` | `evaluateRelationalInfixExpression` | `true` |
| `relationalLessThanFalse` | `RelationalInfixExpression(3 < 2)` | `evaluateRelationalInfixExpression` | `false` |
| `relationalGreaterThanOrEqual` | `RelationalInfixExpression(5 >= 5)` | `evaluateRelationalInfixExpression` | `true` |
| `conditionalAndShortCircuits` | `ConditionalInfixExpression(false AND ...)` | `evaluateConditionalInfixExpression` | `false` |
| `conditionalOrShortCircuits` | `ConditionalInfixExpression(true OR ...)` | `evaluateConditionalInfixExpression` | `true` |
| `logicalComplementNegatesTrue` | `LogicalComplement(true)` | `evaluateLogicalComplement` | `false` |
| `stringConcatenation` | `StringConcatenation("ab", "cd")` | `evaluateStringConcatenation` | `"abcd"` |
| `parameterAccessReturnsArgument` | `ParameterAccess(param)` | `evaluateParameterAccess` | The argument passed at invocation |

### VmStatementExecutionCharacterizationTest

Documents how `VirtualMachine.execute(Statement)` dispatches each statement
type. Tests verify behavior through statement listener events and/or return
values from enclosing functions.

| Test method | Statement node(s) | VM method | Assertion |
| --- | --- | --- | --- |
| `emptyBlockStatementProducesTwoEvents` | `BlockStatement()` | `executeBlockStatement` | Events: `executing:Block`, `executed:Block` |
| `nestedBlockStatementEventNesting` | `BlockStatement(BlockStatement(Comment))` | `executeBlockStatement` | Events are balanced and correctly nested |
| `conditionalStatementTakesTrueBranch` | `ConditionalStatement(true → return 1, else → return 2)` | `executeConditionalStatement` | Returns `1` |
| `conditionalStatementTakesFalseBranch` | `ConditionalStatement(false → return 1, else → return 2)` | `executeConditionalStatement` | Returns `2` |
| `countLoopExecutesNTimes` | `CountLoop(count=3, body=Comment)` | `executeCountLoop` | 3 Comment executing/executed event pairs |
| `countLoopZeroCountSkipsBody` | `CountLoop(count=0, body=Comment)` | `executeCountLoop` | No Comment events |
| `whileLoopExecutesUntilConditionFalse` | `WhileLoop(counter < 3)` | `executeWhileLoop` | 3 iterations |
| `forEachInArrayLoopIteratesElements` | `ForEachInArrayLoop(array, body)` | `executeForEachInArrayLoop` | One iteration per element |
| `doInOrderExecutesSequentially` | `DoInOrder(BlockStatement(A, B))` | `executeDoInOrder` | Events for A before events for B |
| `doTogetherWithZeroStatementsCompletes` | `DoTogether(BlockStatement())` | `executeDoTogether` | Completes, no body events |
| `doTogetherWithOneStatementExecutes` | `DoTogether(BlockStatement(Comment))` | `executeDoTogether` | Deterministic: Comment events fire |
| `returnStatementReturnsValue` | `ReturnStatement(IntegerLiteral(99))` | `executeReturnStatement` | Function returns `99` |
| `localDeclarationAndAccessRoundTrip` | `LocalDeclarationStatement + ReturnStatement(LocalAccess)` | `executeLocalDeclarationStatement` | Returns the declared value |
| `disabledStatementIsSkipped` | `Comment(isEnabled=false)` | `execute` | No events for the disabled statement |

### VmFieldAccessCharacterizationTest

Documents `UserField` get/set and the `FieldAccess`/`AssignmentExpression`
evaluation paths through the VM. These tests require a `UserInstance` (created
via `ENTRY_POINT_createInstance`) to hold field state.

| Test method | Path | VM method | Assertion |
| --- | --- | --- | --- |
| `userFieldInitialValueIsNull` | `FieldAccess(ThisExpression, field)` | `evaluateFieldAccess` → `getUserField` | Returns `null` for uninitialized field |
| `assignmentExpressionSetsField` | `AssignmentExpression(FieldAccess(this, f), IntegerLiteral(42))` | `evaluateAssignmentExpression` → `setUserField` | Subsequent FieldAccess returns `42` |
| `fieldAccessReadsAssignedValue` | Assign then read | `evaluateFieldAccess` | Returns value written by assignment |
| `parameterAccessInMethodReturnsArgument` | `UserMethod(param) { return param; }` | `evaluateParameterAccess` | Returns the argument value passed at invocation |
| `localAccessReturnsLocalValue` | `LocalDeclarationStatement(x=7)` + `ReturnStatement(LocalAccess(x))` | `evaluateLocalAccess` | Returns `7` |
| `assignmentToLocalUpdatesLocal` | `LocalDecl(x=0)` + `Assign(LocalAccess(x), 5)` + `Return(LocalAccess(x))` | `evaluateAssignmentExpression` (local path) | Returns `5` |

### VmErrorHandlingCharacterizationTest

Documents exceptional and boundary behavior in the VM execution engine.

| Test method | Scenario | Expected |
| --- | --- | --- |
| `relationalWithNullRightThrowsLgnaVmNpe` | `RelationalInfixExpression(1 < null)` | `LgnaVmNullPointerException` with "right operand is null" |
| `relationalWithNullLeftThrowsLgnaVmNpe` | `RelationalInfixExpression(null < 1)` | `LgnaVmNullPointerException` with "left operand is null" |
| `relationalWithBothNullThrowsLgnaVmNpe` | `RelationalInfixExpression(null < null)` | `LgnaVmNullPointerException` with "left and right operands are both null" |
| `functionWithNoReturnThrowsNoReturnException` | `UserMethod(Integer)` with body `BlockStatement(Comment)` — no `ReturnStatement` | `LgnaVmNoReturnException` |
| `returnExceptionCarriesValue` | `UserMethod(Integer)` with `ReturnStatement(42)` | `invokeUserMethod` catches `ReturnException`, returns `42` |
| `disabledStatementFiresNoEvents` | `statement.isEnabled.setValue(false)` | `execute` returns immediately; no listener callbacks |
| `invalidMethodInvocationSkipsSilently` | `MethodInvocation` with method not attached to a type (`isValid() → false`) | No exception; `evaluateMethodInvocation` logs `Logger.severe` and returns `null` |
| `nullExpressionThrowsNpe` | `evaluate(null)` | `NullPointerException` (Java-level) |
| `countLoopWithNullCountThrowsLgnaVmNpe` | `CountLoop` with `NullLiteral` as count | `LgnaVmNullPointerException` with "count expression is null" |
| `whileLoopWithNullConditionThrowsLgnaVmNpe` | `WhileLoop` with `NullLiteral` as condition | `LgnaVmNullPointerException` with "while condition is null" |

### VmStoryApiDispatchCharacterizationTest

Documents the adapter class registration and `ENTRY_POINT_createInstance`
dispatch paths used when the VM creates instances of user-defined types that
extend Java Story API classes.

| Test method | Path | Assertion |
| --- | --- | --- |
| `adapterClassRegistrationMapsInterface` | `registerAbstractClassAdapter(Interface.class, Adapter.class)` | `mapAbstractClsToAdapterCls` contains the mapping |
| `createInstanceConstructsUserInstance` | `ENTRY_POINT_createInstance(constructor)` | Returns a non-null `UserInstance` |
| `createInstanceSetsFieldValues` | Build type with `UserField(Integer)`, assign in constructor body | `UserInstance.getFieldValue(field)` returns the assigned value |
| `methodContextCallbackRoutes` | Register an adapter that records `MethodContext` invocations | The adapter's method receives the expected `UserType`, method name, and arguments |
| `createInstanceWithSuperConstructorChain` | `NamedUserConstructor` → `SuperConstructorInvocationStatement` → `JavaConstructor.getInstance(Object.class)` | `ENTRY_POINT_createInstance` completes without exception |

## Design decisions

1. **Static methods for expression and statement tests.** Avoids `UserInstance`
   construction overhead except where field access requires it.
   `ENTRY_POINT_invoke(null, method)` is the same entry point used by
   `SilverThreadVirtualMachineExecutionTest`.

2. **Function return as observable output.** Expression evaluation results are
   captured by wrapping each expression in `ReturnStatement` inside a function
   `UserMethod(name, ReturnType, ...)`. This avoids coupling tests to the
   `expressionEvaluated` listener callback, which is a separate concern.

3. **Statement events as observable output.** Statement execution behavior is
   verified through `RecordingVirtualMachineListener` callbacks — the same
   pattern used by `VirtualMachineHeadlessRuntimeEventTest` and
   `SilverThreadVirtualMachineExecutionTest`.

4. **Full constructor chain for UserInstance.** Field access tests build the
   complete `NamedUserConstructor` → `ConstructorBlockStatement` →
   `SuperConstructorInvocationStatement` → `JavaConstructor.getInstance(Object.class)`
   chain required by `ENTRY_POINT_createInstance`. This is the production path.

5. **Bounded loop iterations.** All loops use a count ≤ 5 and no recursive AST
   structures. This prevents hangs and keeps test execution time bounded.

6. **DoTogether restricted to 0-1 statements.** DoTogether with ≥ 2 statements
   spawns threads, making event ordering non-deterministic. The tests only
   assert on the deterministic 0-statement and 1-statement paths.

7. **One file per responsibility.** Each file targets one area of VM behavior so
   failures isolate to the changed code path. All files stay under 500 lines.

## Usage

Run all five suites:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='VmExpressionEvaluationCharacterizationTest,VmStatementExecutionCharacterizationTest,VmFieldAccessCharacterizationTest,VmErrorHandlingCharacterizationTest,VmStoryApiDispatchCharacterizationTest' \
  test
```

Run a single suite:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=VmExpressionEvaluationCharacterizationTest \
  test
```

Run alongside all `core/ast` tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test
```

All suites run without a display server, JavaFX toolkit, or network access.

## Behavior contracts

### Expression evaluation

The `evaluate(Expression)` method dispatches via a `switch` expression on the
concrete `Expression` subclass. Each branch delegates to a
`evaluate<NodeType>` method. After evaluation, the VM fires
`expressionEvaluated` to all registered `VirtualMachineListener` instances.

The characterization tests verify the return value of each evaluator method
through function return, not the `expressionEvaluated` callback. This
separates expression semantics from listener dispatch.

### Statement execution

The `execute(Statement)` method:

1. Checks `isStopped` — returns immediately if the VM is stopped.
2. Checks `statement.isEnabled.getValue()` — skips disabled statements.
3. Fires `statementExecuting` to all registered listeners.
4. Dispatches to the type-specific `execute<NodeType>` method.
5. Fires `statementExecuted` in the `finally` block.

The `executing`/`executed` events form a balanced stack: each `executing` has a
matching `executed` in reverse order, even when methods recurse.

### Error handling

| Error | Source | VM behavior |
| --- | --- | --- |
| Null operand in relational expression | `evaluateRelationalInfixExpression` | Throws `LgnaVmNullPointerException` with a descriptive message. |
| Null count in CountLoop | `evaluateInt` called by `executeCountLoop` | Throws `LgnaVmNullPointerException("count expression is null")`. |
| Null condition in WhileLoop | `evaluateBoolean` called by `executeWhileLoop` | Throws `LgnaVmNullPointerException("while condition is null")`. |
| Function with no return statement | `invokeUserMethod` | Throws `LgnaVmNoReturnException`. |
| Function with return statement | `invokeUserMethod` | Catches `ReturnException`, returns `.getValue()`. |
| Invalid method invocation | `evaluateMethodInvocation` | Logs `Logger.severe` and returns `null`. |
| Disabled statement | `execute(Statement)` | `isEnabled` check returns without executing or firing events. |
| Null expression | `evaluate(Expression)` | Throws Java `NullPointerException`. |

### Field access

The VM provides two field access paths:

- **`getUserField(UserField, Object)`** — delegates to
  `UserInstance.getFieldValue(field)`.
- **`setUserField(UserField, Object, Object)`** — delegates to
  `UserInstance.setFieldValue(field, value)`.

`AssignmentExpression` dispatches to `set` via `evaluateAssignmentExpression`,
which inspects the left-hand side:

- `FieldAccess` → `set(field, evaluate(expression), value)`
- `LocalAccess` → `setLocal(local, value)`
- `ArrayAccess` → `setItemAtIndex(...)`

### Story API dispatch

`ENTRY_POINT_createInstance` constructs a `UserInstance` by:

1. Resolving the `NamedUserConstructor` from the type.
2. Pushing a constructor frame.
3. Executing the `ConstructorBlockStatement` body, which contains a
   `SuperConstructorInvocationStatement` targeting `JavaConstructor.getInstance(Object.class)`.
4. Returning the new `UserInstance`.

Adapter classes registered via `registerAbstractClassAdapter` allow the VM to
dispatch protected methods on abstract Java Story API interfaces through
concrete adapter implementations that receive a `MethodContext` callback.

## API reference

| Class | Module | Role |
| --- | --- | --- |
| `ReleaseVirtualMachine` | `core/ast` | Headless VM; `ENTRY_POINT_invoke` for static methods, `ENTRY_POINT_createInstance` for instances. |
| `VirtualMachine` | `core/ast` | Abstract base; all `evaluate*` and `execute*` methods live here. |
| `UserInstance` | `core/ast` | Holds field values for user-defined type instances. |
| `NamedUserType` | `core/ast` | Declaring type; satisfies `AbstractCode.isValid()`. |
| `UserMethod` | `core/ast` | Method node; `isStatic`, return type, parameters, body. |
| `UserField` | `core/ast` | Field node; `valueType`, `initializer`. |
| `UserParameter` | `core/ast` | Parameter node; paired with `ParameterAccess`. |
| `UserLocal` | `core/ast` | Local variable node; paired with `LocalAccess`, `LocalDeclarationStatement`. |
| `BlockStatement` | `core/ast` | Statement container; method/constructor body. |
| `ConditionalStatement` | `core/ast` | if/else-if/else control flow. |
| `CountLoop` | `core/ast` | Count-based loop (variable, constant, count, body). |
| `WhileLoop` | `core/ast` | Condition-based loop. |
| `ForEachInArrayLoop` | `core/ast` | Array-iterating loop. |
| `DoInOrder` | `core/ast` | Sequential compound; delegates to `execute(body)`. |
| `DoTogether` | `core/ast` | Parallel compound; 0-1 = synchronous, 2+ = threaded. |
| `ReturnStatement` | `core/ast` | Throws `ReturnException`; caught by `invokeUserMethod`. |
| `ExpressionStatement` | `core/ast` | Wraps an `Expression` as a `Statement`. |
| `LocalDeclarationStatement` | `core/ast` | Pushes a local variable onto the frame. |
| `IntegerLiteral`, `DoubleLiteral`, `BooleanLiteral`, `StringLiteral`, `NullLiteral` | `core/ast` | Literal expression nodes. |
| `ArithmeticInfixExpression` | `core/ast` | Binary arithmetic (+, -, *, /). |
| `RelationalInfixExpression` | `core/ast` | Binary comparison (<, >, <=, >=). |
| `ConditionalInfixExpression` | `core/ast` | Boolean AND/OR with short-circuit. |
| `LogicalComplement` | `core/ast` | Boolean NOT. |
| `StringConcatenation` | `core/ast` | String + String. |
| `FieldAccess` | `core/ast` | Expression that reads a field from an instance. |
| `AssignmentExpression` | `core/ast` | Expression that writes to a field, local, or array. |
| `ParameterAccess` | `core/ast` | Expression that reads a method parameter. |
| `LocalAccess` | `core/ast` | Expression that reads a local variable. |
| `MethodInvocation` | `core/ast` | Expression that invokes a method. |
| `ThisExpression` | `core/ast` | Expression that returns the current instance. |
| `ArrayInstanceCreation` | `core/ast` | Expression that creates a Java array. |
| `NamedUserConstructor` | `core/ast` | Constructor node; `ConstructorBlockStatement` body. |
| `ConstructorBlockStatement` | `core/ast` | Body with `SuperConstructorInvocationStatement` + statements. |
| `SuperConstructorInvocationStatement` | `core/ast` | Delegates to parent constructor. |
| `JavaConstructor` | `core/ast` | Reference to a Java-level constructor (e.g., `Object()`). |
| `VirtualMachineListener` | `core/ast` | Callback interface for statement/expression/loop events. |
| `LgnaVmNullPointerException` | `core/ast` | VM-level null pointer with descriptive message. |
| `LgnaVmNoReturnException` | `core/ast` | Thrown when a function body ends without `ReturnStatement`. |
| `ReturnException` | `core/ast` | Control-flow exception carrying a return value. |

## Configuration

No system properties or environment variables are required beyond
`NODE_OPTIONS=--max-old-space-size=32768` for the Maven reactor build.

All test state is in-memory. No files are created on disk.

## Relationship to issue #504

[RabbitHole issue #504](https://github.com/rysweet/RabbitHole/issues/504) asks
for characterization tests covering the VM execution engine and StoryAPI
dispatch.

| Requirement | How these tests satisfy it |
| --- | --- |
| Method invocation characterization | `VmExpressionEvaluationCharacterizationTest` covers `MethodInvocation` via `parameterAccessReturnsArgument`; method invocation is also covered by the existing `SilverThreadVirtualMachineExecutionTest`. |
| Field access characterization | `VmFieldAccessCharacterizationTest` covers `FieldAccess`, `AssignmentExpression`, `ParameterAccess`, `LocalAccess`. |
| Expression evaluation characterization | `VmExpressionEvaluationCharacterizationTest` covers literals, arithmetic, relational, conditional, logical, and string concatenation. |
| Error handling characterization | `VmErrorHandlingCharacterizationTest` covers null operands, missing return, invalid invocation, disabled statements. |
| StoryAPI dispatch characterization | `VmStoryApiDispatchCharacterizationTest` covers adapter registration, instance creation, and method context routing. |
| Each file under 500 lines | Five files, one per responsibility area. |
| Target headless execution path | All tests use `ReleaseVirtualMachine.ENTRY_POINT_invoke` or `ENTRY_POINT_createInstance` — no display, no JavaFX, no scene graph. |

## Claim boundaries

These tests prove:

- `ReleaseVirtualMachine` correctly evaluates literal, arithmetic, relational,
  conditional, logical, and string concatenation expressions.
- `ReleaseVirtualMachine` correctly executes block, conditional, count loop,
  while loop, forEach, doInOrder, doTogether (0-1 statement), return, and
  local declaration statements.
- `UserField` get/set through `FieldAccess` and `AssignmentExpression` round-trips
  correctly through the VM.
- The VM raises typed exceptions (`LgnaVmNullPointerException`,
  `LgnaVmNoReturnException`) for documented error conditions.
- `ENTRY_POINT_createInstance` constructs a `UserInstance` with the full
  constructor chain.
- Adapter class registration enables Story API method dispatch.

These tests do **not** prove:

| Non-claim | Reason |
| --- | --- |
| 3D rendering | No scene graph, no display server. |
| Save/reopen round-trip | No serialization; purely in-memory. |
| Drag-and-drop UI | No Swing/JavaFX automation. |
| Gallery asset loading | No model resources. |
| JavaFX display | No toolkit initialization. |
| Lesson completion | No grading or assessment. |
| DoTogether with ≥2 statements | Non-deterministic thread ordering. |
| Lambda expression evaluation | `evaluateLambdaExpression` throws `RuntimeException("todo")`. |
| Instance method invocation via loaded project | All methods are static or invoked on test-constructed instances. |
| Cross-module behavior | Tests are entirely within `core/ast`. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Silver thread recursive method invocation | [Silver Thread VM Execution Test](./silver-thread-vm-execution-test.md) |
| Headless VM event dispatch (flat body) | `VirtualMachineHeadlessRuntimeEventTest` in `core/ast` |
| Create → build → run → save → reopen | [Silver Thread Launch-Build-Run Test](./silver-thread-launch-build-run-test.md) |
| Production save round-trip | [Silver Thread Save Round-Trip Test](./silver-thread-save-round-trip-test.md) |
| Runtime event dispatch and Story API listener source | [Generated Story API Listener Source Characterization](./generated-story-api-listener-source-characterization.md) |

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| `OutOfMemoryError` during Maven build | Insufficient heap for the reactor. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
| Test not found by Surefire | Wrong test name or missing failIfNoTests flags. | Use `-DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`. |
| Listener receives fewer events than expected | Method has no declaring type (`isValid()` returns `false`). | Add method to `NamedUserType` via `type.methods.add(method)` before execution. |
| `AssertionError` in `invokeUserMethod` | Static method called with non-null instance. | Pass `null` as the first argument to `ENTRY_POINT_invoke` for static methods. |
| `LgnaVmNoReturnException` in expression test | Function body has no `ReturnStatement`. | Wrap expression in `ReturnStatement` inside the function body. |
| Field access test returns `null` unexpectedly | `UserInstance` not created via `ENTRY_POINT_createInstance`, or assignment not executed before read. | Build full constructor chain; execute assignment before field read. |
| DoTogether test has non-deterministic events | Body has ≥2 statements, spawning threads. | Restrict DoTogether tests to 0 or 1 statement. |
| `NullPointerException` in `evaluate` | `null` expression passed to `evaluate`. | Check that AST node properties are wired before execution. |
