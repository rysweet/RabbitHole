# Run the VM Characterization Tests

Use this guide to run and review the virtual machine execution engine and
StoryAPI dispatch characterization tests. These five test suites validate
expression evaluation, statement execution, field access, error handling, and
adapter dispatch without desktop startup, GUI toolkits, or Git LFS assets.

For the full contract, see the [VM Characterization Tests
reference](../reference/vm-characterization-tests.md).

## When to use this guide

Use this guide for changes near:

```text
core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java
core/ast/src/main/java/org/lgna/project/virtualmachine/ReleaseVirtualMachine.java
core/ast/src/main/java/org/lgna/project/virtualmachine/UserInstance.java
core/ast/src/main/java/org/lgna/project/ast/UserMethod.java
core/ast/src/main/java/org/lgna/project/ast/UserField.java
```

Also run these checks when changing:

- Any `evaluate*` or `execute*` method in `VirtualMachine.java`;
- `invokeUserMethod` or `ENTRY_POINT_invoke` dispatch;
- `ENTRY_POINT_createInstance` or constructor resolution;
- `AssignmentExpression` evaluation paths;
- `LgnaVmNullPointerException`, `LgnaVmNoReturnException`, or `ReturnException`;
- `statement.isEnabled` handling in `execute(Statement)`;
- Adapter class registration (`registerAbstractClassAdapter`).

Do not use this guide for desktop UI automation, visible rendering, Save
workflows, grading, or broad Tweedle decode coverage. Those claims require their
own evidence lanes.

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run all five suites

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='VmExpressionEvaluationCharacterizationTest,VmStatementExecutionCharacterizationTest,VmFieldAccessCharacterizationTest,VmErrorHandlingCharacterizationTest,VmStoryApiDispatchCharacterizationTest' \
  test
```

Expected outcome: all test methods pass without a display server, JavaFX
toolkit, or network access.

## Run a single suite

### Expression evaluation

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=VmExpressionEvaluationCharacterizationTest \
  test
```

Expected outcome: literal, arithmetic, relational, conditional, logical, string
concatenation, and parameter access expressions evaluate to their documented
values.

### Statement execution

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=VmStatementExecutionCharacterizationTest \
  test
```

Expected outcome: blocks, conditionals, count loops, while loops, forEach,
doInOrder, doTogether (0-1 statement), return, local declaration, and disabled
statements produce the documented event sequences or return values.

### Field access

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=VmFieldAccessCharacterizationTest \
  test
```

Expected outcome: `UserField` get/set, `FieldAccess`, `AssignmentExpression`,
`ParameterAccess`, and `LocalAccess` round-trip correctly through the VM.

### Error handling

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=VmErrorHandlingCharacterizationTest \
  test
```

Expected outcome: null operands, missing return, invalid invocation, disabled
statements, and null expressions produce the documented exceptions or
skip-behavior.

### Story API dispatch

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=VmStoryApiDispatchCharacterizationTest \
  test
```

Expected outcome: adapter registration maps correctly, `ENTRY_POINT_createInstance`
constructs `UserInstance` with full constructor chain, and method context
callbacks route through the adapter.

## Run alongside all core/ast tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test
```

The five new suites run alongside `VirtualMachineHeadlessRuntimeEventTest`,
`SilverThreadVirtualMachineExecutionTest`, and all other `core/ast` tests
without interference.

## Review the results

### Expression evaluation

| Assertion category | Meaning |
| --- | --- |
| Literal values | Each literal type (`Integer`, `Double`, `Boolean`, `String`, `Null`) evaluates to its expected Java value. |
| Arithmetic | `ArithmeticInfixExpression` delegates to `operator.operate()` and returns the correct number. |
| Relational | `RelationalInfixExpression` delegates to `operator.operate()` and returns the correct boolean. |
| Conditional | `ConditionalInfixExpression` short-circuits: `false AND x` → `false`; `true OR x` → `true`. |
| Logical complement | `LogicalComplement` negates the boolean operand. |
| String concatenation | `StringConcatenation` concatenates via `String.valueOf`. |
| Parameter access | `ParameterAccess` looks up the argument value from the current method frame. |

### Statement execution

| Assertion category | Meaning |
| --- | --- |
| Block nesting | `executing`/`executed` events form a balanced stack, even with nested blocks. |
| Conditional branching | `ConditionalStatement` evaluates each boolean expression body pair in order; falls through to `elseBody`. |
| Loop iteration | `CountLoop`, `WhileLoop`, `ForEachInArrayLoop` fire per-iteration events. |
| DoInOrder | Delegates directly to `execute(body)` — events are sequential. |
| DoTogether | 0 statements → no events; 1 statement → deterministic execution. |
| Return | `ReturnStatement` throws `ReturnException` carrying the value. |
| Local declaration | `LocalDeclarationStatement` pushes the local; `LocalAccess` retrieves it. |
| Disabled statement | `isEnabled=false` → no execution, no events. |

### Error handling

| Assertion category | Meaning |
| --- | --- |
| Null operands | `LgnaVmNullPointerException` with descriptive messages for relational, count, and while contexts. |
| Missing return | `LgnaVmNoReturnException` when a function body completes without `ReturnStatement`. |
| Invalid invocation | `evaluateMethodInvocation` logs `Logger.severe` and returns `null` when `MethodInvocation.isValid()` is false. |
| Disabled skip | No events, no side effects for disabled statements. |
| Null expression | Java `NullPointerException` when `null` is passed to `evaluate`. |

### Field access and Story API dispatch

| Assertion category | Meaning |
| --- | --- |
| Field round-trip | Write via `AssignmentExpression`, read via `FieldAccess` returns the same value. |
| Instance creation | `ENTRY_POINT_createInstance` builds `UserInstance` with full constructor chain. |
| Adapter dispatch | Registered adapter receives `MethodContext` callbacks with correct type and arguments. |

## Troubleshooting

### Missing Tweedle grammar classes

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

If the submodule is uninitialized, Maven fails with missing parser classes.
Always initialize before running characterization.

### Listener receives fewer events than expected

The most common cause is a method without a declaring type. `AbstractCode.isValid()`
requires `getDeclaringType() != null`. Add the method to the `NamedUserType`
before execution.

### Field access returns null when a value was assigned

Verify that:

1. The `UserInstance` was created via `ENTRY_POINT_createInstance` with the full
   constructor chain.
2. The assignment `ExpressionStatement(AssignmentExpression(...))` was executed
   before the read.
3. The `UserField` is the same object instance in both the assignment and the
   read.

### DoTogether test has non-deterministic events

DoTogether with ≥2 statements spawns threads. The characterization tests
restrict DoTogether to 0 or 1 statement for determinism. Do not add DoTogether
tests with ≥2 statements to the characterization suite.

## What this guide does NOT cover

- Desktop runtime execution or GUI toolkit startup
- Full world playback or visible scene rendering
- Save, Save As, or project recovery workflows
- Grading, creative assessment, or lesson completion
- Lambda expression evaluation
- DoTogether with ≥2 concurrent statements
- Instance method invocation via loaded projects
- Cross-module behavior outside `core/ast`

These behaviors require separate evidence lanes with their own fixtures and
review language.
