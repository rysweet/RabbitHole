# Tutorial: Trace the Silver Thread VM Execution Test

This tutorial walks through the virtual machine execution silver thread test.
You will trace how a two-method AST is built, executed through
`ReleaseVirtualMachine`, and verified via the 8-event statement listener
contract.

For the full contract, see the [Silver Thread VM Execution Test
reference](../reference/silver-thread-vm-execution-test.md).

## Contents

- [Goal](#goal)
- [1. Understand what this test adds](#1-understand-what-this-test-adds)
- [2. Trace the type construction](#2-trace-the-type-construction)
- [3. Trace the helper method](#3-trace-the-helper-method)
- [4. Trace the method invocation wiring](#4-trace-the-method-invocation-wiring)
- [5. Trace the entry method](#5-trace-the-entry-method)
- [6. Trace VM execution](#6-trace-vm-execution)
- [7. Understand the 8-event contract](#7-understand-the-8-event-contract)
- [8. Run the test](#8-run-the-test)
- [9. Understand the boundaries](#9-understand-the-boundaries)

## Goal

Understand how the VM executes a method that calls another method, and how the
statement listener events prove both methods ran. After this tutorial you will
be able to explain:

- Why both methods must belong to a `NamedUserType`
- How `ExpressionStatement` + `MethodInvocation` triggers a recursive VM call
- Why the event sequence is 8 events (not 4)
- What the `isValid()` guard protects against

This tutorial traces the implemented test. Open the source in
`core/ast/src/test/java/org/lgna/project/virtualmachine/SilverThreadVirtualMachineExecutionTest.java`
alongside this guide.

## 1. Understand what this test adds

The existing `VirtualMachineHeadlessRuntimeEventTest` proves the VM can execute
a single method containing a `Comment`:

```text
BlockStatement → Comment → done
4 events: executing:Block, executing:Comment, executed:Comment, executed:Block
```

The new silver thread test extends this to prove the VM can execute a method
that **calls another method**:

```text
BlockStatement → ExpressionStatement(MethodInvocation) → [enter helper] →
  BlockStatement → Comment → done → [return] → done
8 events
```

This covers the Run step of the student journey: when a student clicks Run,
Alice's virtual machine must execute method calls, not just flat statement
lists.

## 2. Trace the type construction

Find the `NamedUserType` creation:

```java
NamedUserType type = new NamedUserType();
type.name.setValue("VMExecutionProgram");
type.superType.setValue(JavaType.getInstance(SProgram.class));
```

**Why a `NamedUserType`?** Open `AbstractCode.java` line 72:

```java
public boolean isValid() {
  return getDeclaringType() != null;
}
```

A method must have a declaring type for `isValid()` to return `true`. The VM
checks `isValid()` inside `evaluateMethodInvocation` (line 648) before dispatching
the recursive call. Without the type, the VM silently skips the method call —
the `ExpressionStatement` events still fire, but the helper's body events do not,
yielding 4 events instead of 8.

This is the most common pitfall when writing VM tests. If your listener
receives 4 events instead of 8, check whether the target method has a
declaring type.

## 3. Trace the helper method

Find the helper method creation:

```java
Comment marker = new Comment("helper executed");
UserMethod helperMethod = new UserMethod(
    "helperMethod", Void.TYPE,
    new UserParameter[0],
    new BlockStatement(marker));
helperMethod.isStatic.setValue(true);
type.methods.getValue().add(helperMethod);
```

Key points:

- **`isStatic` is `true`** because `ENTRY_POINT_invoke(null, method)` passes
  `null` as the instance. The VM asserts `instance == null` for static methods.
- **The helper is added to the type before VM execution.** This is critical:
  `evaluateMethodInvocation` (line 648) checks `isValid()` at runtime. If
  the helper method has no declaring type at that point, the VM silently
  skips the recursive call, producing only 4 events.

## 4. Trace the method invocation wiring

Find the `MethodInvocation` construction:

```java
MethodInvocation call = new MethodInvocation();
call.method.setValue(helperMethod);
call.expression.setValue(new NullLiteral());

ExpressionStatement callStatement = new ExpressionStatement();
callStatement.expression.setValue(call);
```

Three nodes are wired together:

1. **`MethodInvocation`** — the expression that tells the VM to call
   `helperMethod`. The `method` property points to the target. The
   `expression` property is the receiver (`this`), which is `NullLiteral`
   because the call is static.

2. **`ExpressionStatement`** — wraps the `MethodInvocation` so it can appear
   in a `BlockStatement`. Expressions are not statements in Alice's AST; the
   `ExpressionStatement` adapter is required. This is the node that fires the
   `executing:ExpressionStatement` / `executed:ExpressionStatement` events.

3. **`NullLiteral`** — the simplest possible expression. For a static call the
   receiver is irrelevant. `NullLiteral` avoids the need for type
   infrastructure to create a valid instance expression.

## 5. Trace the entry method

Find the entry method creation:

```java
UserMethod entryMethod = new UserMethod(
    "entryMethod", Void.TYPE,
    new UserParameter[0],
    new BlockStatement(callStatement));
entryMethod.isStatic.setValue(true);
type.methods.getValue().add(entryMethod);
```

The entry method's body is `BlockStatement(ExpressionStatement(MethodInvocation))`.
When the VM executes this method:

1. It enters the `BlockStatement` → fires `executing:BlockStatement`
2. It enters the `ExpressionStatement` → fires `executing:ExpressionStatement`
3. It evaluates the `MethodInvocation` expression
4. It calls `invokeUserMethod(helperMethod, null)` — recursive entry
5. Inside the helper, the VM processes `BlockStatement(Comment)` (4 more events)
6. Control returns → fires `executed:ExpressionStatement`
7. Fires `executed:BlockStatement`

## 6. Trace VM execution

Find the execution call:

```java
ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
vm.addVirtualMachineListener(listener);

vm.ENTRY_POINT_invoke(null, entryMethod);
```

`ENTRY_POINT_invoke(null, entryMethod)` is the same entry point used by
`VirtualMachineHeadlessRuntimeEventTest`. The `null` first argument means
"no instance — this is a static call."

The `RecordingVirtualMachineListener` is the same inner class pattern used
by the existing test: it records `"executing:ClassName"` and
`"executed:ClassName"` strings for each statement event.

## 7. Understand the 8-event contract

The test asserts this exact sequence:

```text
1. executing:BlockStatement         ← entry method body begins
2. executing:ExpressionStatement    ← method-call statement begins
3. executing:BlockStatement         ← helper method body begins (recursive)
4. executing:Comment                ← Comment in helper begins
5. executed:Comment                 ← Comment in helper finishes
6. executed:BlockStatement          ← helper method body finishes
7. executed:ExpressionStatement     ← method-call statement finishes
8. executed:BlockStatement          ← entry method body finishes
```

Notice the nesting:

```text
[Block                                    ]   ← entry
  [ExpressionStatement                    ]
    [Block                                ]   ← helper
      [Comment                            ]
```

Each bracket opens with `executing` and closes with `executed`. The events
form a balanced stack, just like matching parentheses.

Compare with the 4-event contract from `VirtualMachineHeadlessRuntimeEventTest`:

```text
1. executing:BlockStatement
2. executing:Comment
3. executed:Comment
4. executed:BlockStatement
```

The new test nests this 4-event sequence inside an additional
`BlockStatement`/`ExpressionStatement` pair, doubling the event count.

## 8. Run the test

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SilverThreadVirtualMachineExecutionTest \
  test
```

Both test methods pass without a display server or network access.

To run alongside all other `core/ast` tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test
```

## 9. Understand the boundaries

After tracing both methods, confirm you can answer:

| Question | Answer |
| --- | --- |
| Does this prove rendering works? | No. No scene graph, no display. |
| Does this prove save/reopen works? | No. No serialization. |
| Does this prove instance methods work? | No. Both methods are static. |
| Does this prove loops or conditionals work? | No. Only `BlockStatement`, `ExpressionStatement`, and `Comment`. |
| Does this prove expression evaluation events fire? | No. Only statement events are asserted. |
| What does it prove? | The VM's recursive method-invocation execution path works: `execute → evaluateMethodInvocation → invokeUserMethod → execute`. |

The test is intentionally focused on one thing: proving that method calls work
through the VM. It is the "Run" layer of the silver thread. Broader tests in
`core/ide` (like `SilverThreadLaunchBuildRunTest`) build on this foundation by
adding save/reopen and starter-project loading around the same VM execution
pattern.
