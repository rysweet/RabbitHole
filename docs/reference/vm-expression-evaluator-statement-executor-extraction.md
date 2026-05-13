# VmExpressionEvaluator and VmStatementExecutor Extraction

This reference documents the extraction of expression evaluation and statement
execution methods from `VirtualMachine.java` into two package-private delegate
classes (issue #551). The extraction reduces `VirtualMachine.java` from 1193
lines to under 500 while preserving every public and protected method signature.

## Contents

- [Motivation](#motivation)
- [Extracted classes](#extracted-classes)
- [File inventory](#file-inventory)
- [Delegation pattern](#delegation-pattern)
- [Visibility changes](#visibility-changes)
- [CopyOnWriteArrayList upgrade](#copyonwritearraylist-upgrade)
- [Methods that move entirely](#methods-that-move-entirely)
- [Methods that become thin wrappers](#methods-that-become-thin-wrappers)
- [Methods that stay on VirtualMachine](#methods-that-stay-on-virtualmachine)
- [Cross-delegate calls](#cross-delegate-calls)
- [Test coverage](#test-coverage)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [API reference](#api-reference)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Motivation

`VirtualMachine.java` was 1193 lines with three distinct responsibilities
mixed together:

1. **VM lifecycle and frame management** — abstract frame operations,
   entry points, field/array access, method invocation, listener
   management, stop control
2. **Expression evaluation** — 28+ `evaluate*` methods dispatched by
   the `evaluate(Expression)` switch
3. **Statement execution** — 17 `execute*` methods dispatched by the
   `execute(Statement)` switch

Groups 2 and 3 are internally cohesive and only call back to group 1
through a small set of VM operations. Extracting them into delegates
follows the same pattern used for `StorytellingSceneEditor` (PR #534)
and `TweedleEncoder` (PR #406).

## Extracted classes

### VmExpressionEvaluator

```
core/ast/src/main/java/org/lgna/project/virtualmachine/VmExpressionEvaluator.java
```

- **Visibility**: package-private `final class`
- **Role**: Owns all `evaluate*` methods and the central
  `evaluate(Expression)` switch dispatch
- **Line count**: ~452 lines
- **Constructor**: `VmExpressionEvaluator(VirtualMachine vm)` — stores a
  back-reference used for VM operations (`getThis()`, `lookup()`,
  `get()`, `set()`, `invoke()`, `getLocal()`, `setLocal()`,
  `getItemAtIndex()`, `setItemAtIndex()`, `getArrayLength()`,
  `createArrayInstance()`, `pushLambdaFrame()`, `popFrame()`,
  `execute()`, `checkNotNull()`, `isStopped`,
  `virtualMachineListeners`, `mapAbstractClsToAdapterCls`)

### VmStatementExecutor

```
core/ast/src/main/java/org/lgna/project/virtualmachine/VmStatementExecutor.java
```

- **Visibility**: package-private `final class`
- **Role**: Owns all `execute*` methods and the central
  `execute(Statement)` switch dispatch
- **Line count**: ~369 lines
- **Constructor**: `VmStatementExecutor(VirtualMachine vm)` — stores a
  back-reference used for VM operations (`evaluate()`,
  `evaluateArguments()`, `getFrameForThread()`, `pushCurrentThread()`,
  `popCurrentThread()`, `pushLocal()`, `getLocal()`, `setLocal()`,
  `popLocal()`, `invoke()`, `isStopped`, `virtualMachineListeners`,
  `checkNotNull`) and cross-delegate calls to
  `vm.expressionEvaluator.evaluateBoolean()` /
  `vm.expressionEvaluator.evaluateInt()`

## File inventory

| File | Action | Lines (approx) |
|------|--------|----------------|
| `VmExpressionEvaluator.java` | Create | 452 |
| `VmStatementExecutor.java` | Create | 369 |
| `VirtualMachine.java` | Modify | 1193 → ~470 |
| `VmContractTest.java` | Create (test) | 548 |
| `ReleaseVirtualMachine.java` | Unchanged | 494 |

No files outside `core/ast` are modified.

## Delegation pattern

`VirtualMachine` instantiates delegates in field initializers:

```java
final VmExpressionEvaluator expressionEvaluator = new VmExpressionEvaluator(this);
final VmStatementExecutor statementExecutor = new VmStatementExecutor(this);
```

Each former method becomes a one-line delegation:

```java
protected Object evaluate(Expression expression) {
  return expressionEvaluator.evaluate(expression);
}

protected void execute(Statement statement) throws ReturnException {
  statementExecutor.execute(statement);
}
```

The `evaluate` and `execute` methods remain `protected` on
`VirtualMachine` because `ReleaseVirtualMachine` and external callers
reference them. The delegates call through `vm.evaluate()` and
`vm.execute()` for cross-delegate operations (expression evaluation
needs statement execution in lambdas; statement execution needs
expression evaluation for conditionals).

## Visibility changes

Four members are promoted from `private` to package-private so the
delegates can access them without reflection:

| Member | Was | Now | Reason |
|--------|-----|-----|--------|
| `isStopped` | `private boolean` | `boolean` (package-private) | Checked by both delegates for early-exit |
| `virtualMachineListeners` | `private final List<>` | `final CopyOnWriteArrayList<>` (package-private) | Event firing in both delegates |
| `mapAbstractClsToAdapterCls` | `private final Map<>` | `final Map<>` (package-private) | Lambda EPIC_HACK in evaluator |
| `checkNotNull(Object, String)` | `private void` | `void` (package-private) | Null-guard in evaluator (`evaluateBoolean`, `evaluateInt`) and executor loops |

No new `public` or `protected` members are introduced.

## CopyOnWriteArrayList upgrade

The `virtualMachineListeners` field changes from:

```java
private final List<VirtualMachineListener> virtualMachineListeners = Lists.newLinkedList();
```

to:

```java
final CopyOnWriteArrayList<VirtualMachineListener> virtualMachineListeners =
    new CopyOnWriteArrayList<>();
```

**Rationale**: The original code used `synchronized (virtualMachineListeners)`
blocks for every read and write. `CopyOnWriteArrayList` provides thread-safe
iteration without explicit synchronization, which is both simpler and correct
for the read-heavy, write-rare listener pattern. This removes 5 synchronized
blocks from the codebase.

The `addVirtualMachineListener`, `removeVirtualMachineListener`, and
`getVirtualMachineListeners` methods drop their synchronized blocks. The
`execute(Statement)` and `evaluate(Expression)` dispatch methods in the
delegates iterate the list directly (COWAL snapshot semantics guarantee
no `ConcurrentModificationException`).

`VmContractTest` includes a field-type assertion:

```java
@Test
public void virtualMachineListeners_isCopyOnWriteArrayList() {
  Field field = getDeclaredField("virtualMachineListeners");
  assertEquals(CopyOnWriteArrayList.class, field.getType());
}
```

## Methods that move entirely

### Into VmExpressionEvaluator

These methods move wholesale — the signatures, bodies, and Javadoc
transfer without modification:

| Method | Lines |
|--------|-------|
| `evaluateAssignmentExpression(AssignmentExpression)` | 19 |
| `evaluateBooleanLiteral(BooleanLiteral)` | 1 |
| `evaluateArrayInstanceCreation(ArrayInstanceCreation)` | 10 |
| `evaluateArrayAccess(ArrayAccess)` | 1 |
| `evaluateArrayLength(ArrayLength)` | 1 |
| `evaluateFieldAccess(FieldAccess)` | 16 |
| `evaluateLocalAccess(LocalAccess)` | 1 |
| `evaluateArithmeticInfixExpression(ArithmeticInfixExpression)` | 4 |
| `evaluateBitwiseInfixExpression(BitwiseInfixExpression)` | 4 |
| `evaluateConditionalInfixExpression(ConditionalInfixExpression)` | 16 |
| `evaluateRelationalInfixExpression(RelationalInfixExpression)` | 16 |
| `evaluateShiftInfixExpression(ShiftInfixExpression)` | 4 |
| `evaluateLogicalComplement(LogicalComplement)` | 2 |
| `evaluateStringConcatenation(StringConcatenation)` | 3 |
| `evaluateMethodInvocation(MethodInvocation)` | 26 |
| `evaluateNullLiteral(NullLiteral)` | 1 |
| `evaluateDoubleLiteral(DoubleLiteral)` | 1 |
| `evaluateFloatLiteral(FloatLiteral)` | 1 |
| `evaluateIntegerLiteral(IntegerLiteral)` | 1 |
| `evaluateParameterAccess(ParameterAccess)` | 1 |
| `evaluateStringLiteral(StringLiteral)` | 1 |
| `evaluateThisExpression(ThisExpression)` | 3 |
| `evaluateTypeExpression(TypeExpression)` | 1 |
| `evaluateTypeLiteral(TypeLiteral)` | 1 |
| `evaluateResourceExpression(ResourceExpression)` | 1 |
| `EPIC_HACK_evaluateLambdaExpression(LambdaExpression, AbstractArgument)` | 40 |
| `evaluateLambdaExpression(LambdaExpression)` | 1 |
| `evaluate(Expression)` — central switch | 44 |
| `evaluate(Expression, Class)` — typed evaluate | 12 |
| `evaluateBoolean(Expression, String)` | 8 |
| `evaluateInt(Expression, String)` | 8 |
| `evaluateArgument(AbstractArgument)` | 8 |
| `evaluateArguments(...)` | 55 |

### Into VmStatementExecutor

| Method | Lines |
|--------|-------|
| `executeBlockStatement(BlockStatement, listeners)` | 6 |
| `executeConditionalStatement(ConditionalStatement, listeners)` | 7 |
| `executeComment(Comment, listeners)` | 1 |
| `executeCountLoop(CountLoop, listeners)` | 34 |
| `executeDoInOrder(DoInOrder, listeners)` | 1 |
| `executeDoTogether(DoTogether, listeners)` | 30 |
| `executeExpressionStatement(ExpressionStatement, listeners)` | 1 |
| `excecuteForEachLoop(AbstractForEachLoop, Object[], listeners)` | 30 |
| `executeForEachInArrayLoop(ForEachInArrayLoop, listeners)` | 3 |
| `executeForEachInIterableLoop(ForEachInIterableLoop, listeners)` | 3 |
| `excecuteEachInTogether(AbstractEachInTogether, Object[], listeners)` | 55 |
| `executeEachInArrayTogether(EachInArrayTogether, listeners)` | 3 |
| `executeEachInIterableTogether(EachInIterableTogether, listeners)` | 3 |
| `executeReturnStatement(ReturnStatement, listeners)` | 3 |
| `executeWhileLoop(WhileLoop, listeners)` | 20 |
| `executeLocalDeclarationStatement(LocalDeclarationStatement, listeners)` | 2 |
| `execute(Statement)` — central switch | 52 |

Note: the misspelling `excecuteForEachLoop` and `excecuteEachInTogether`
is preserved from the original to avoid changing method signatures.

## Methods that become thin wrappers

These methods stay on `VirtualMachine` as one-line delegations:

```java
// Expression evaluation — delegates to VmExpressionEvaluator
protected Object evaluate(Expression expression)
protected final <E> E evaluate(Expression expression, Class<E> cls)
public Object[] evaluateArguments(AbstractCode, NodeListProperty, NodeListProperty, NodeListProperty)

// Statement execution — delegates to VmStatementExecutor
protected void execute(Statement statement) throws ReturnException
```

`evaluateArguments()` stays `public` on `VirtualMachine` because
`InstanceCreation.java` calls `vm.evaluateArguments(...)` directly.
The wrapper delegates to `expressionEvaluator.evaluateArguments(...)`.

## Methods that stay on VirtualMachine

These methods remain in `VirtualMachine.java` without delegation:

- All 13 `abstract` frame/thread methods (`getStackTrace`, `getThis`,
  `pushBogusFrame`, `pushConstructorFrame`, etc.)
- Entry points: `ENTRY_POINT_evaluate`, `ENTRY_POINT_invoke`,
  `ENTRY_POINT_createInstance`
- Field/array access: `get`, `set`, `getItemAtIndex`, `setItemAtIndex`,
  `getArrayLength`, `getUserField`, `setUserField`,
  `getFieldDeclaredInJavaWithField`, `setFieldDeclaredInJavaWithField`
- Method invocation: `invoke`, `invokeUserMethod`,
  `invokeMethodDeclaredInJava`, `checkArguments`
- Instance creation: `createInstance`, `createArrayInstance`,
  `createUserArrayInstance`, `createJavaArrayInstance`,
  `getConstructor`
- Adapter registration: `registerAbstractClassAdapter`
- Scene setup: `createAndSetFieldInstance`,
  `ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField`,
  `ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement`
- Listener management: `addVirtualMachineListener`,
  `removeVirtualMachineListener`, `getVirtualMachineListeners`
- Control: `stopExecution`, `setForSceneEditor`

## Cross-delegate calls

The lambda `EPIC_HACK_evaluateLambdaExpression` in `VmExpressionEvaluator`
calls `vm.execute(...)` to run a `UserLambda` body. This crosses into
statement execution territory. Rather than creating a circular delegate
dependency, the evaluator calls back through `vm.execute()`, which
delegates to `statementExecutor.execute()`. The call chain:

```
VmExpressionEvaluator.EPIC_HACK_evaluateLambdaExpression
  → vm.execute(userLambda.body)       [calls VirtualMachine.execute()]
    → statementExecutor.execute(...)  [delegates to VmStatementExecutor]
```

Similarly, `VmStatementExecutor` calls `vm.evaluate(...)` for
expression evaluation in conditionals, loops, and expression statements.

### evaluateBoolean / evaluateInt shared access

`evaluateBoolean(Expression, String)` and `evaluateInt(Expression, String)`
are private helpers that move to `VmExpressionEvaluator`, but they are
also called by three `VmStatementExecutor` methods:

- `executeConditionalStatement` → `evaluateBoolean`
- `executeCountLoop` → `evaluateInt`
- `executeWhileLoop` → `evaluateBoolean`

Since both delegates are package-private in the same package, the
executor accesses these through the vm's evaluator field:

```
VmStatementExecutor.executeConditionalStatement
  → vm.expressionEvaluator.evaluateBoolean(expr, msg)
    → evaluates + null-check + Boolean cast

VmStatementExecutor.executeCountLoop
  → vm.expressionEvaluator.evaluateInt(expr, msg)
    → evaluates + null-check + Integer cast
```

These two methods are package-private on `VmExpressionEvaluator`
(not private) to support this cross-delegate access.

## Test coverage

### VmContractTest

```
core/ast/src/test/java/org/lgna/project/virtualmachine/VmContractTest.java
```

A reflection-based API surface lock that prevents accidental changes to
`VirtualMachine`'s public contract. It asserts:

- **Exact public method count** (20 methods) — if a method is added or
  removed, the test fails with a descriptive message
- **Exact abstract method count** (16 methods) — protects the
  `ReleaseVirtualMachine` contract
- **Named method existence** — every public and abstract method is
  checked by name and parameter types
- **Field type assertion** — `virtualMachineListeners` is
  `CopyOnWriteArrayList`
- **Delegate field existence** — `expressionEvaluator` and
  `statementExecutor` fields exist with correct types
- **No new public fields** — only the expected fields are public

### Existing characterization tests

These existing tests validate behavioral preservation:

| Test class | What it covers |
|------------|---------------|
| `SilverThreadVirtualMachineExecutionTest` | Recursive method invocation + 8-event listener sequence |
| `VmExpressionEvaluationCharacterizationTest` | Literal, arithmetic, relational, conditional, string expressions |
| `VmStatementExecutionCharacterizationTest` | Block, conditional, count loop, while loop, for-each, doInOrder |
| `VmFieldAccessCharacterizationTest` | UserField get/set, FieldAccess, LocalAccess, ParameterAccess |
| `VmErrorHandlingCharacterizationTest` | Null pointer, no-return, class cast, disabled statement |
| `VmStoryApiDispatchCharacterizationTest` | Adapter registration, ENTRY_POINT_createInstance |
| `VirtualMachineHeadlessRuntimeEventTest` | Statement/expression event firing |

All seven existing tests pass without modification after the extraction.

## Validation commands

Build and test the `core/ast` module (and its dependencies):

```bash
mvn -pl core/ast -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

Quick line-count check:

```bash
wc -l core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java
# Expected: < 500

wc -l core/ast/src/main/java/org/lgna/project/virtualmachine/VmExpressionEvaluator.java
# Expected: ~452

wc -l core/ast/src/main/java/org/lgna/project/virtualmachine/VmStatementExecutor.java
# Expected: ~369
```

Verify no files outside `core/ast` are changed:

```bash
git diff --name-only develop
# All paths should start with core/ast/
```

Verify `ReleaseVirtualMachine` is unchanged:

```bash
git diff develop -- core/ast/src/main/java/org/lgna/project/virtualmachine/ReleaseVirtualMachine.java
# Expected: empty (no diff)
```

## Compatibility rules

1. **ReleaseVirtualMachine is untouched.** It extends `VirtualMachine`
   and overrides none of the extracted methods. The extraction changes
   no protected signatures that `ReleaseVirtualMachine` depends on.

2. **InstanceCreation.java calls `vm.evaluateArguments()` directly.**
   This method stays `public` on `VirtualMachine` as a thin wrapper.
   `InstanceCreation.java` requires zero changes.

3. **No public or protected members are added.** External callers see
   the identical API surface.

4. **The `isStopped` one-way latch is preserved.** `stopExecution()`
   sets `isStopped = true` on `VirtualMachine`; both delegates read
   the field directly (package-private access). There is no reset path
   and the semantics are identical to the original.

5. **Thread safety is improved, not degraded.** COWAL provides stronger
   iteration guarantees than the previous `synchronized` + `LinkedList`
   approach. The `DoTogether` and `EachInTogether` threading patterns
   (which use `ThreadUtilities.doTogether` and push/pop frames across
   threads) are unchanged.

## API reference

### VirtualMachine (post-extraction)

**Public methods** (20 total):

| Method | Return | Description |
|--------|--------|-------------|
| `ENTRY_POINT_evaluate(UserInstance, Expression[])` | `Object[]` | Evaluate expressions in a bogus frame |
| `ENTRY_POINT_invoke(UserInstance, AbstractMethod, Object...)` | `Object` | Entry point for method invocation |
| `ENTRY_POINT_createInstance(NamedUserType, Object...)` | `UserInstance` | Create a user type instance |
| `createAndSetFieldInstance(UserInstance, UserField)` | `void` | Initialize a field on an instance |
| `ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(UserInstance, UserField)` | `void` | Scene editor field init |
| `ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(UserInstance, Statement)` | `void` | Scene editor statement exec |
| `registerAbstractClassAdapter(Class, Class)` | `void` | Register adapter for abstract class |
| `evaluateArguments(AbstractCode, NodeListProperty, NodeListProperty, NodeListProperty)` | `Object[]` | Evaluate method arguments |
| `get(AbstractField, Object)` | `Object` | Get field value |
| `set(AbstractField, Object, Object)` | `void` | Set field value |
| `getItemAtIndex(AbstractType, Object, Integer)` | `Object` | Array element access |
| `setItemAtIndex(AbstractType, Object, Integer, Object)` | `void` | Array element mutation |
| `invokeUserMethod(Object, UserMethod, Object...)` | `Object` | Invoke a user-defined method |
| `invokeMethodDeclaredInJava(Object, JavaMethod, Object...)` | `Object` | Invoke a Java method |
| `stopExecution()` | `void` | Set the stop flag |
| `addVirtualMachineListener(VirtualMachineListener)` | `void` | Add listener |
| `removeVirtualMachineListener(VirtualMachineListener)` | `void` | Remove listener |
| `getVirtualMachineListeners()` | `List<VirtualMachineListener>` | Snapshot of listeners |
| `setForSceneEditor()` | `void` | Mark VM for scene editor use |
| `getStackTrace(Thread)` | `LgnaStackTraceElement[]` | Get stack trace (abstract) |

**Abstract methods** (16 total):

| Method | Description |
|--------|-------------|
| `getStackTrace(Thread)` | Stack trace for a thread |
| `getThis()` | Current `this` instance |
| `pushBogusFrame(UserInstance)` | Push a placeholder frame |
| `pushConstructorFrame(NamedUserType, Map)` | Push constructor frame |
| `setConstructorFrameUserInstance(UserInstance)` | Set instance on constructor frame |
| `pushMethodFrame(UserInstance, UserMethod, Map)` | Push method frame |
| `pushLambdaFrame(UserInstance, UserLambda, AbstractMethod, Map)` | Push lambda frame |
| `popFrame()` | Pop current frame |
| `lookup(UserParameter)` | Resolve parameter value |
| `pushLocal(UserLocal, Object)` | Push local variable |
| `getLocal(UserLocal)` | Get local variable value |
| `setLocal(UserLocal, Object)` | Set local variable value |
| `popLocal(UserLocal)` | Pop local variable |
| `getFrameForThread(Thread)` | Get frame for a thread |
| `pushCurrentThread(Frame)` | Push thread frame |
| `popCurrentThread()` | Pop thread frame |

### VmExpressionEvaluator (package-private)

| Method | Visibility | Description |
|--------|-----------|-------------|
| `evaluate(Expression)` | package | Central expression switch dispatch |
| `evaluate(Expression, Class)` | package | Typed expression evaluation |
| `evaluateArguments(...)` | package | Full argument evaluation |
| 28 `evaluate*` methods | package | Individual expression type handlers |

### VmStatementExecutor (package-private)

| Method | Visibility | Description |
|--------|-----------|-------------|
| `execute(Statement)` | package | Central statement switch dispatch |
| 16 `execute*` methods | package | Individual statement type handlers |

## Examples

### Before extraction — adding a new expression evaluator

Previously, adding support for a new expression type required editing
the 1193-line `VirtualMachine.java`:

```java
// In VirtualMachine.java (1193 lines) — hard to find the right section
protected Object evaluateMyNewExpression(MyNewExpression expr) {
    return /* ... */;
}

// Then add to the giant evaluate() switch — also in VirtualMachine.java
case MyNewExpression e -> evaluateMyNewExpression(e);
```

### After extraction — adding a new expression evaluator

```java
// In VmExpressionEvaluator.java (~452 lines) — focused, cohesive file
Object evaluateMyNewExpression(MyNewExpression expr) {
    return /* ... */;
}

// Add to the evaluate() switch — same file, easy to find
case MyNewExpression e -> evaluateMyNewExpression(e);
```

The `VirtualMachine.java` wrapper (`protected Object evaluate(Expression)`)
requires no change — it delegates to `expressionEvaluator.evaluate()`.

### Before extraction — adding a new statement executor

Same problem: the 1193-line file.

### After extraction — adding a new statement executor

```java
// In VmStatementExecutor.java (~369 lines)
void executeMyNewStatement(MyNewStatement stmt, VirtualMachineListener[] listeners)
    throws ReturnException {
    /* ... */
}

// Add to execute() switch — same file
case MyNewStatement s -> executeMyNewStatement(s, listeners);
```

## Troubleshooting

### Compilation error in ReleaseVirtualMachine

`ReleaseVirtualMachine` overrides `protected` methods on `VirtualMachine`.
If you accidentally remove or change the signature of `evaluate(Expression)`
or `execute(Statement)` wrappers, `ReleaseVirtualMachine` will fail to
compile. Ensure both thin wrappers remain `protected` on `VirtualMachine`.

### "method not visible" in delegate class

If a delegate calls `vm.someMethod()` and gets a visibility error, the
method is still `private`. Promote it to package-private (remove the
`private` modifier). Only promote the minimum set listed in
[Visibility changes](#visibility-changes).

### ConcurrentModificationException in listener iteration

If you see this after the COWAL upgrade, you are likely still using
`synchronized (virtualMachineListeners)` somewhere. Remove all
`synchronized` blocks around the listener list — COWAL handles thread
safety internally.

### Wrong method count in VmContractTest

If the test reports a different public or abstract method count, compare
the expected set in the test against the [API reference](#api-reference)
tables above. The test asserts exact counts of public (20) and abstract
(16) methods. If you added or removed a method on `VirtualMachine`,
update the test counts and add/remove the corresponding named assertion.

### "package-private access" errors in delegates

The four promoted members (`isStopped`, `virtualMachineListeners`,
`mapAbstractClsToAdapterCls`, `checkNotNull`) must be package-private
(no access modifier), not `private`. Check that no `private` keyword
appears before these declarations.
