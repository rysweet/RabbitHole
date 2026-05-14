# Tweedle VirtualMachine Dead Code Removal

This reference describes the removal of ~835 lines of commented-out dead code
from `VirtualMachine.java` in the `org.alice.tweedle.run` package. The cleanup
reduces the file from 938 lines to 104 lines while preserving all 11 active
methods with identical signatures.

The change is a pure dead-code removal. No methods are added, removed, renamed,
or moved. No public API changes. All 235 `core/tweedle` tests pass unchanged.

## Contents

- [Motivation](#motivation)
- [What changed](#what-changed)
- [Architecture](#architecture)
- [Public API](#public-api)
- [Configuration](#configuration)
- [Validation](#validation)
- [Examples](#examples)
- [Claim boundaries](#claim-boundaries)

## Motivation

`VirtualMachine.java` accumulated ~835 lines of commented-out code over several
years as execution logic migrated from the VM class to AST node visitor dispatch
(`evaluate(Frame)` / `execute(Frame)` on expression and statement nodes). The
comments were never cleaned up, leaving a 938-line file where only 49 lines of
active code existed.

This dead code created three problems:

1. **Misleading complexity** — static analysis counted 938 lines, flagging the
   file as a high-priority extraction target when no extraction was needed.
2. **Navigation cost** — developers scrolling through the file had to mentally
   separate dead comments from live code.
3. **Stale design signals** — the comments described an obsolete interpreter
   architecture (direct evaluation in the VM) that no longer reflects the
   actual visitor-dispatch design.

RabbitHole issue #579 removes the dead code, making the file's thin-delegation
role immediately visible.

## What changed

| Metric | Before | After |
| --- | --- | --- |
| Total lines | 938 | 104 |
| Active methods | 11 | 11 |
| Commented-out methods | ~30 | 0 |
| Imports | 1 wildcard + 1 specific | 1 wildcard + 1 specific |
| Public API surface | Unchanged | Unchanged |

### Lines removed

All removed lines were block comments (`/* ... */`) or line comments (`//`)
containing dead method bodies from the pre-visitor-dispatch interpreter. These
included commented-out implementations for:

- Expression evaluation (arithmetic, logical, string, comparison operators)
- Method/constructor invocation with argument binding
- Field access and assignment
- Array element access and creation
- Type casting and instanceof checks
- Lambda and closure creation
- Control flow execution (if/else, while, for-each, do-together, count loops)
- Variable declaration and local scope management
- Return statement handling
- Object instantiation and constructor chaining

None of these commented blocks contained TODO markers, design rationale, or
documentation that would be valuable to preserve. The active implementations
live in the AST node classes themselves.

### Files modified

| File | Change |
| --- | --- |
| `core/tweedle/.../run/VirtualMachine.java` | Remove ~835 lines of dead comments |

### Files NOT modified

- `Frame.java` — unchanged, still a minimal scope container
- `TweedleObject.java` — unchanged, field get/set/init delegation intact
- `ReleaseVirtualMachine` — extends `org.lgna.project.virtualmachine.VirtualMachine`
  (a different class in a different package), completely unaffected
- All test files — no test references the dead code

## Architecture

After cleanup, `VirtualMachine.java` is a thin abstract base with three tiers
of methods:

```text
VirtualMachine (abstract, 104 lines)
├── ENTRY_POINT methods (3) — create Frame, start execution
│   ├── ENTRY_POINT_evaluate  → evaluate() protected method
│   ├── ENTRY_POINT_invoke    → invoke() protected method
│   └── ENTRY_POINT_createInstance → TweedleClass.instantiate() directly
│
├── Scene-editor hooks (3) — special-case entry for IDE live-editing
│   ├── createAndSetFieldInstance → TweedleObject.initializeField() directly
│   ├── ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField → createAndSetFieldInstance()
│   └── ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement → execute() protected method
│
└── Protected delegation (5) — overridable methods that forward to AST nodes
    ├── get(TweedleField, TweedleObject) → instance.get()
    ├── set(TweedleField, TweedleObject, TweedleValue) → instance.set()
    ├── invoke(Frame, TweedleObject, TweedleMethod, TweedleValue...) → method.invoke()
    ├── evaluate(Frame, TweedleExpression) → expression.evaluate()
    └── execute(Frame, TweedleStatement) → statement.execute() with isEnabled() guard
```

The actual evaluation and execution logic lives in the AST nodes via the
visitor-dispatch pattern:

```text
TweedleExpression.evaluate(Frame) → TweedleValue
TweedleStatement.execute(Frame)   → void
TweedleMethod.invoke(Frame, ...)  → Object
TweedleClass.instantiate(Frame, ...) → TweedleObject
```

`VirtualMachine` serves as the entry-point facade, creating `Frame` objects and
delegating to the AST. Subclasses can override the protected methods to add
instrumentation (breakpoints, logging, scene-editor integration) without
modifying the AST nodes.

## Public API

All 11 methods are preserved with identical signatures. No API consumers are
affected.

### Entry-point methods

```java
public Object[] ENTRY_POINT_evaluate(
    TweedleObject instance, TweedleExpression[] expressions)
```

Evaluates an array of expressions in a new frame scoped to the given instance.
Returns the evaluated values as an `Object[]`.

```java
public void ENTRY_POINT_invoke(
    TweedleObject instance, TweedleMethod method, TweedleValue... arguments)
```

Invokes a method on an instance in a new frame. Fire-and-forget — return value
is discarded at the entry-point level.

```java
public TweedleObject ENTRY_POINT_createInstance(
    TweedleClass entryPointType, TweedleValue... arguments)
```

Instantiates a Tweedle class via its constructor in a new frame with no parent
instance. Delegates directly to `TweedleClass.instantiate()` (not through a
protected method), so subclasses cannot intercept instantiation at this level.

### Scene-editor hooks

```java
public TweedleValue createAndSetFieldInstance(
    Frame frame, TweedleObject instance, TweedleField field)
```

Initializes a field on an existing instance within an existing frame. Called
during object construction and scene-editor field updates.

```java
public Object ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(
    TweedleObject instance, TweedleField field)
```

Creates a new frame and initializes a single field. Used by the scene editor to
re-initialize fields outside normal construction flow.

```java
public void ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(
    TweedleObject instance, TweedleStatement statement)
```

Creates a new frame and executes a single statement. Used by the scene editor
to run property-setter statements during live editing.

### Protected delegation methods

```java
protected TweedleValue get(TweedleField field, TweedleObject instance)
protected void set(TweedleField field, TweedleObject instance, TweedleValue value)
protected Object invoke(Frame frame, TweedleObject target,
    TweedleMethod method, TweedleValue... arguments)
protected TweedleValue evaluate(Frame frame, TweedleExpression expression)
protected void execute(Frame frame, TweedleStatement statement)
```

Each delegates to the corresponding AST node method (`execute` also guards on
`statement.isEnabled()`). Subclasses override these to intercept execution
(e.g., for debugging or scene-editor synchronization).

## Configuration

No configuration. `VirtualMachine` is an abstract class with no constructors,
no static state, and no property files. Instantiation is handled by concrete
subclasses in other modules.

## Validation

Run the `core/tweedle` test suite to confirm the cleanup:

```bash
git submodule update --init tweedle-lang && \
mvn -pl core/tweedle -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

Expected result: BUILD SUCCESS, 235 tests, 0 failures, 0 errors.

The dead code can be recovered from git history if needed:

```bash
git show fb589a38e8:core/tweedle/src/main/java/org/alice/tweedle/run/VirtualMachine.java
```

## Examples

### Using the entry-point API (unchanged)

```java
VirtualMachine vm = getVirtualMachine();

// Evaluate expressions
TweedleExpression[] exprs = { colorExpr, sizeExpr };
Object[] values = vm.ENTRY_POINT_evaluate(instance, exprs);

// Invoke a method
vm.ENTRY_POINT_invoke(instance, walkMethod, distanceArg);

// Create a new instance
TweedleObject biped = vm.ENTRY_POINT_createInstance(bipedClass);
```

### Scene-editor field initialization (unchanged)

```java
VirtualMachine vm = getVirtualMachine();

// Re-initialize a field during live editing
vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(sceneInstance, colorField);

// Execute a setter statement during live editing
vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(sceneInstance, setOpacityStmt);
```

### Subclassing for instrumentation (unchanged)

```java
public class DebuggingVirtualMachine extends VirtualMachine {
  @Override
  protected void execute(Frame frame, TweedleStatement statement) {
    logStatement(statement);
    checkBreakpoint(statement);
    super.execute(frame, statement);
  }
}
```

The override pattern is identical before and after the cleanup. Only the
commented-out dead code is gone.

## Claim boundaries

This cleanup:

- **Does** remove ~835 lines of commented-out dead code from
  `VirtualMachine.java` (938 → 104 lines)
- **Does** preserve all 11 active methods with identical signatures
- **Does** pass all 235 `core/tweedle` tests with zero failures
- **Does** keep the copyright header, package declaration, and imports intact
- **Does not** change any public or protected API
- **Does not** change any runtime behavior
- **Does not** affect `ReleaseVirtualMachine` (different `VirtualMachine` class
  in `org.lgna.project.virtualmachine`)
- **Does not** extract, move, split, or rename any methods
- **Does not** modify `Frame.java` or `TweedleObject.java`
- **Does not** add or remove any files
- **Does not** require new dependencies or configuration
