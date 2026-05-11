# Silver Thread Virtual Machine Execution Test

This reference defines the silver thread test that proves the virtual machine
can execute a recursive method invocation and fire the expected listener events.
The executable proof is
`org.lgna.project.virtualmachine.SilverThreadVirtualMachineExecutionTest`. It is
a JUnit 4 characterization test in `core/ast` that proves the Run step of the
student journey: build a type with two methods where one calls the other,
execute headlessly via `ReleaseVirtualMachine`, and verify the full 8-event
statement execution sequence.

The test does not start JavaFX, load gallery assets, render a 3D scene, exercise
drag-and-drop UI, serialize to disk, or require a display.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Design decisions](#design-decisions)
- [Test methods](#test-methods)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Relationship to issue #491](#relationship-to-issue-491)
- [Claim boundaries](#claim-boundaries)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Scope

The test covers exactly one journey:

### Build Type → Add Two Methods → Execute Entry Method → Verify Recursive Execution Events

```text
1. Create a NamedUserType ("VMExecutionProgram")
2. Add a static helper UserMethod with body BlockStatement(Comment("helper executed"))
3. Add the helper to the type (required before VM execution; isValid() checks declaring type)
4. Add a static entry UserMethod whose body is
   BlockStatement(ExpressionStatement(MethodInvocation(NullLiteral, helper)))
5. Add the entry method to the type
6. Execute the entry method via ReleaseVirtualMachine.ENTRY_POINT_invoke(null, entryMethod)
7. Verify 8 statement events from the RecordingVirtualMachineListener
8. Verify no exceptions propagated
```

This is the first silver thread test that exercises `ExpressionStatement` +
`MethodInvocation` through the VM. The existing
`VirtualMachineHeadlessRuntimeEventTest` only executes a flat
`BlockStatement(Comment)` — four events. This test proves the recursive
`execute → evaluateMethodInvocation → invokeUserMethod → execute` path
produces the correct nested event sequence.

## Implementation status

| Surface | Location |
| --- | --- |
| `SilverThreadVirtualMachineExecutionTest` | `core/ast/src/test/java/org/lgna/project/virtualmachine/SilverThreadVirtualMachineExecutionTest.java` |
| Maven validation | `mvn -pl core/ast -am -Dtest=SilverThreadVirtualMachineExecutionTest test` |

## Design decisions

Four deliberate decisions, informed by the existing `VirtualMachineHeadlessRuntimeEventTest`:

1. **Both methods are owned by a `NamedUserType`.**
   `AbstractCode.isValid()` (line 72) requires `getDeclaringType() != null`.
   `MethodInvocation.isValid()` (line 107) delegates to the target method's
   `isValid()` check (line 112). Without a declaring type,
   `evaluateMethodInvocation` (line 648) silently skips the recursive call —
   the `ExpressionStatement` events still fire, but the helper's body events
   do not, yielding 4 events instead of 8. The `NamedUserType` is not used
   for serialization — it exists only to satisfy the validity guard.

2. **Helper method is added to the type before VM execution.**
   `evaluateMethodInvocation` (line 648) checks `methodInvocation.isValid()`,
   which delegates to the target method's `isValid()` — requiring
   `getDeclaringType() != null`. The helper must be a member of a
   `NamedUserType` at execution time. Ordering relative to `MethodInvocation`
   construction is irrelevant when using the no-arg constructor with property
   setting.

3. **Static methods invoked with `null` target.**
   `VirtualMachine.invokeUserMethod` (line 414) asserts `instance == null` for
   static methods at line 416. This matches the pattern established by
   `VirtualMachineHeadlessRuntimeEventTest` and avoids the need for a scene
   instance or object graph.

4. **`NullLiteral` as the method invocation expression.**
   For a static call the expression (the `this` reference) is irrelevant — the
   VM evaluates it but ignores the result. `NullLiteral` is the simplest
   expression node that satisfies the `MethodInvocation` constructor without
   requiring additional type infrastructure.

## Test methods

### vmExecutesMethodInvocationAndFiresExpectedEvents

This method exercises the recursive execution path:

| Step | API | Assertion |
| --- | --- | --- |
| Create type | `new NamedUserType()`, set name and supertype | Type is non-null. |
| Create helper method | `new UserMethod("helperMethod", Void.TYPE, ..., BlockStatement(Comment))` | — |
| Add helper to type | `type.methods.getValue().add(helperMethod)` | — |
| Create method invocation | `new MethodInvocation()` with `method.setValue(helperMethod)` and `expression.setValue(new NullLiteral())` | — |
| Wrap in ExpressionStatement | `new ExpressionStatement()` with `expression.setValue(call)` | — |
| Create entry method | `new UserMethod("entryMethod", Void.TYPE, ..., BlockStatement(callStatement))` | — |
| Add entry to type | `type.methods.getValue().add(entryMethod)` | — |
| Execute via VM | `ReleaseVirtualMachine.ENTRY_POINT_invoke(null, entryMethod)` with `RecordingVirtualMachineListener` | Listener receives 8 events (see [Event contract](#vm-execution-events)). |

### vmExecutionCompletesWithoutExceptions

This method uses the same AST structure and verifies that
`ENTRY_POINT_invoke` completes without throwing any exception. It is a
smoke test that guards against regressions in the
`executeExpressionStatement` → `evaluateMethodInvocation` →
`invokeUserMethod` code path.

| Step | API | Assertion |
| --- | --- | --- |
| Build identical AST | Same as above | — |
| Execute via VM | `ReleaseVirtualMachine.ENTRY_POINT_invoke(null, entryMethod)` | No exception thrown. |

## Usage

Run from the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SilverThreadVirtualMachineExecutionTest \
  test
```

Both test methods run without a display server, JavaFX toolkit, or network
access.

## Behavior contract

### VM execution events

The `RecordingVirtualMachineListener` records `statementExecuting` and
`statementExecuted` callbacks. For an entry method that calls a helper method
via `ExpressionStatement(MethodInvocation)`, the expected 8-event sequence is:

```text
executing:BlockStatement         ← VM enters entry method body
executing:ExpressionStatement    ← VM enters the method-call statement
executing:BlockStatement         ← VM enters helper method body (recursive)
executing:Comment                ← VM enters the Comment in the helper
executed:Comment                 ← VM finishes the Comment
executed:BlockStatement          ← VM finishes helper method body
executed:ExpressionStatement     ← VM finishes the method-call statement
executed:BlockStatement          ← VM finishes entry method body
```

The events form a balanced stack: each `executing` has a matching `executed`
in reverse order. The inner `BlockStatement`/`Comment` pair is the helper
method's body. The outer `BlockStatement`/`ExpressionStatement` pair is the
entry method's body.

Compare with `VirtualMachineHeadlessRuntimeEventTest`, which verifies the
4-event subset for a flat `BlockStatement(Comment)` body.

### Execution path traced

The 8-event sequence proves these VM code paths execute:

| VM method | Triggered by |
| --- | --- |
| `execute(BlockStatement)` | Entry method body |
| `executeExpressionStatement(ExpressionStatement)` | The method-call wrapper |
| `evaluateMethodInvocation(MethodInvocation)` | Expression evaluation inside ExpressionStatement |
| `invokeUserMethod(UserMethod, null)` | Static method dispatch |
| `execute(BlockStatement)` | Helper method body (recursive re-entry) |
| `execute(Comment)` | The no-op statement inside the helper |

## API reference

| Class | Module | Role in this test |
| --- | --- | --- |
| `NamedUserType` | `core/ast` | Declaring type for both methods; satisfies `AbstractCode.isValid()`. |
| `UserMethod` | `core/ast` | The entry and helper methods; `isStatic` set to `true`. |
| `BlockStatement` | `core/ast` | Method body container for both methods. |
| `ExpressionStatement` | `core/ast` | Wraps the `MethodInvocation` as a statement. |
| `MethodInvocation` | `core/ast` | Invokes `helperMethod` with `NullLiteral` as the expression. |
| `NullLiteral` | `core/ast` | The `this` expression for the static method call. |
| `Comment` | `core/ast` | No-op statement in the helper; its `executing`/`executed` events prove the helper ran. |
| `ReleaseVirtualMachine` | `core/ast` | Headless VM; `ENTRY_POINT_invoke(null, method)` runs a static method. |
| `VirtualMachineListener` | `core/ast` | Callback interface for statement execution events. |
| `RecordingVirtualMachineListener` | test inner class | Records `statementExecuting`/`statementExecuted` events as `"executing:ClassName"` / `"executed:ClassName"` strings. |

## Configuration

No system properties or environment variables are required beyond
`NODE_OPTIONS=--max-old-space-size=32768` for the Maven reactor build.

The test creates no files on disk. All state is in-memory.

## Relationship to issue #491

[RabbitHole issue #491](https://github.com/rysweet/RabbitHole/issues/491) asks
for a virtual machine execution silver thread test:

| Requirement | How this test satisfies it |
| --- | --- |
| Load a starter project, create a simple program with a known method call | Builds a `NamedUserType` with two `UserMethod`s; entry method calls helper via `MethodInvocation`. |
| Execute headlessly via `ReleaseVirtualMachine` | `ENTRY_POINT_invoke(null, entryMethod)` — no scene, no display. |
| Verify the method executed without exceptions | `vmExecutionCompletesWithoutExceptions` asserts no exception propagates. |
| Extends silver thread to cover the Run step | The 8-event contract proves recursive method execution, extending the 4-event contract from `VirtualMachineHeadlessRuntimeEventTest`. |

## Claim boundaries

This test proves:

- `ReleaseVirtualMachine` can execute a `UserMethod` whose body contains an
  `ExpressionStatement(MethodInvocation)` that calls another `UserMethod`.
- The VM fires `statementExecuting`/`statementExecuted` events for all four
  statement types in the execution tree: outer `BlockStatement`,
  `ExpressionStatement`, inner `BlockStatement`, and `Comment`.
- The 8-event sequence is balanced and correctly nested.
- The recursive `execute → evaluateMethodInvocation → invokeUserMethod →
  execute` path completes without exceptions.

This test does **not** prove:

| Non-claim | Reason |
| --- | --- |
| 3D rendering correctness | No scene graph, no display server. |
| Save/reopen round-trip | No serialization; purely in-memory. |
| Drag-and-drop UI | No Swing/JavaFX automation. |
| Gallery asset loading | No model resources referenced. |
| JavaFX display | No toolkit initialization. |
| Full lesson completion | No grading, assessment, or creative evaluation. |
| Instance method execution | Both methods are static; `null` instance. |
| Expression evaluation events | `expressionEvaluated` callbacks are not asserted (only statement events). |
| Loop or conditional execution | Only `BlockStatement`, `ExpressionStatement`, and `Comment` are exercised. |
| Exception handling during execution | `Comment` is a no-op; `MethodInvocation` targets a valid static method. |
| Cross-module behavior | Test is entirely within `core/ast`. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Headless VM event dispatch (flat body) | `VirtualMachineHeadlessRuntimeEventTest` in `core/ast`. |
| Create → build → run → save → reopen end-to-end | [Silver Thread Launch-Build-Run Test](./silver-thread-launch-build-run-test.md). |
| Production save round-trip | [Silver Thread Save Round-Trip Test](./silver-thread-save-round-trip-test.md). |
| Edit → save → readback chain | [Silver Thread Edit-Save-Readback Test](./silver-thread-edit-save-readback-test.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](./silver-thread-status-report.md). |

## Examples

### Building the two-method AST

```java
NamedUserType type = new NamedUserType();
type.name.setValue("VMExecutionProgram");
type.superType.setValue(JavaType.getInstance(SProgram.class));

// Helper method: BlockStatement(Comment)
Comment marker = new Comment("helper executed");
UserMethod helper = new UserMethod(
    "helperMethod", Void.TYPE,
    new UserParameter[0],
    new BlockStatement(marker));
helper.isStatic.setValue(true);
type.methods.getValue().add(helper);   // must be added before MethodInvocation

// Entry method: BlockStatement(ExpressionStatement(MethodInvocation(NullLiteral, helper)))
MethodInvocation call = new MethodInvocation();
call.method.setValue(helper);
call.expression.setValue(new NullLiteral());

ExpressionStatement callStatement = new ExpressionStatement();
callStatement.expression.setValue(call);

UserMethod entry = new UserMethod(
    "entryMethod", Void.TYPE,
    new UserParameter[0],
    new BlockStatement(callStatement));
entry.isStatic.setValue(true);
type.methods.getValue().add(entry);
```

### Executing and verifying events

```java
ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
vm.addVirtualMachineListener(listener);

vm.ENTRY_POINT_invoke(null, entry);

assertEquals(Arrays.asList(
    "executing:BlockStatement",
    "executing:ExpressionStatement",
    "executing:BlockStatement",
    "executing:Comment",
    "executed:Comment",
    "executed:BlockStatement",
    "executed:ExpressionStatement",
    "executed:BlockStatement"
), listener.statementEvents);
```

### Running with the full core/ast test suite

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test
```

The new test runs alongside `VirtualMachineHeadlessRuntimeEventTest` and all
other `core/ast` tests without interference.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| VM listener receives zero events | Listener not added to VM before `ENTRY_POINT_invoke`, or entry method body is empty. | Call `vm.addVirtualMachineListener(listener)` before execution; verify body has statements. |
| VM listener receives 4 events instead of 8 | Helper method has no declaring type (`isValid()` returns `false`) so `evaluateMethodInvocation` silently skips the recursive call; or `ExpressionStatement`/`MethodInvocation` not wired correctly. | Add helper to `NamedUserType` via `type.methods.getValue().add(helperMethod)` before execution. Verify entry body is `BlockStatement(ExpressionStatement(MethodInvocation))`. |
| `AssertionError` in `invokeUserMethod` | Method is static but a non-null instance was passed. | Pass `null` as the first argument to `ENTRY_POINT_invoke` for static methods. |
| `MethodInvocation.isValid()` returns false | Helper method has no declaring type. | Add helper to `NamedUserType` via `type.methods.getValue().add(helperMethod)` before VM execution. |
| `OutOfMemoryError` during Maven build | Insufficient heap for the reactor. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
| Test not found by Surefire | Wrong test name or missing `-Dsurefire.failIfNoSpecifiedTests=false`. | Use `-Dtest=SilverThreadVirtualMachineExecutionTest -Dsurefire.failIfNoSpecifiedTests=false`. |
