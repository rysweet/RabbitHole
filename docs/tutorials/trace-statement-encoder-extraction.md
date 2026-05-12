# Tutorial: Trace the StatementEncoder Extraction

A guided walkthrough showing how statement-encoding requests flow through
`TweedleEncoder` and its new `StatementEncoder` delegate. Use this to
understand the bridge and forwarding patterns before extending statement
encoding behavior.

## Prerequisites

- Familiarity with the visitor pattern used by `SourceCodeGenerator`
- Access to the `core/ast` source in `org.alice.serialization.tweedle`
- Optional: read the [Decoder Delegate Decomposition tutorial](./trace-decoder-delegate-decomposition.md)
  for the mirror pattern on the decode side

## Overview

When a statement is encoded to Tweedle source, the formatting passes through
two classes:

```text
TweedleEncoder (@Override methods — visitor dispatch)
  └── StatementEncoder (extracted formatting logic)
      ├── appendStatementCompletion(Statement)
      ├── appendStatementCompletion()  (no-arg)
      ├── appendStatementEnd(Statement)
      └── pushStatementDisabled()
```

`TweedleEncoder` keeps the `@Override` stubs (required by `SourceCodeGenerator`)
and delegates the extracted logic to `StatementEncoder`. Bridge methods handle
the `super` calls that Java requires to stay on the subclass.

## Trace 1: A normal statement completion

Follow a `LocalDeclarationStatement` through the encoder.

### Step 1: Visitor dispatch

When `processLocalDeclaration(LocalDeclarationStatement)` runs on
`TweedleEncoder`, it calls `processSingleStatement(stmt, appender)`, which
eventually calls `appendStatementCompletion(stmt)`.

### Step 2: TweedleEncoder @Override

```java
// TweedleEncoder.java
@Override
protected void appendStatementCompletion(Statement stmt) {
  statementEncoder.appendStatementCompletion(stmt);
}
```

The body is a one-line delegation. No `super` call here — the delegate handles
it via a bridge.

### Step 3: StatementEncoder logic

```java
// StatementEncoder.java
void appendStatementCompletion(Statement stmt) {
  encoder.superAppendStatementCompletion(stmt);
  appendStatementEnd(stmt);
}
```

First, it calls the bridge to reach `SourceCodeGenerator.appendStatementCompletion`.
Then it appends the statement-end marker.

### Step 4: Bridge method

```java
// TweedleEncoder.java (package-private bridge)
void superAppendStatementCompletion(Statement stmt) {
  super.appendStatementCompletion(stmt);
}
```

This exists because Java does not allow `super.method()` from outside the
declaring subclass. The bridge is the only way for `StatementEncoder` to invoke
the inherited behavior.

### Step 5: Statement end

```java
// StatementEncoder.java
void appendStatementEnd(Statement stmt) {
  if (!stmt.isEnabled.getValue()) {
    encoder.forwardAppendSpace();
    encoder.forwardAppendString(TweedleEncoder.NODE_ENABLE);
  }
  encoder.forwardAppendNewLine();
}
```

If the statement is disabled, it appends ` >*` (space + `NODE_ENABLE` marker).
Then it always appends a newline.

**Key insight:** `NODE_ENABLE` was widened from `private` to package-private
so `StatementEncoder` can read it. The forwarding methods
(`forwardAppendString`, `forwardAppendSpace`, `forwardAppendNewLine`) exist
because `appendString`, `appendSpace`, and `appendNewLine` are `protected` on
`SourceCodeGenerator` — Java accessibility rules prevent a same-package class
from calling `protected` methods inherited from a class in a different package.

## Trace 2: A disabled statement

Follow a disabled `DoInOrder` through the encoder.

### Step 1: pushStatementDisabled

Before the statement body, the encoder calls `pushStatementDisabled()`:

```java
// TweedleEncoder.java
@Override
protected void pushStatementDisabled() {
  statementEncoder.pushStatementDisabled();
  super.pushStatementDisabled();
}
```

**Ordering matters:** The delegate writes `NODE_DISABLE` (`*<`) first, then
`super.pushStatementDisabled()` increments the disabled counter. If the order
were reversed, the counter would be wrong during the marker write.

### Step 2: StatementEncoder writes the marker

```java
// StatementEncoder.java
void pushStatementDisabled() {
  encoder.forwardAppendString(TweedleEncoder.NODE_DISABLE);
}
```

This just writes `*<` to the output. The `super` call back on
`TweedleEncoder` handles the counter increment.

### Step 3: Statement body encodes normally

The statement body is encoded with the disabled counter incremented. Each
sub-statement sees the disabled state.

### Step 4: appendStatementEnd adds NODE_ENABLE

When the statement completes, `appendStatementEnd` checks `isEnabled` and
appends ` >*` followed by a newline (see Trace 1, Step 5).

## Trace 3: Code flow statement

Follow a `CountLoop` through `appendCodeFlowStatement`.

### Step 1: processCountLoop dispatch

```java
// TweedleEncoder.java
@Override
public void processCountLoop(CountLoop loop) {
  appendCodeFlowStatement(loop, () -> {
    appendString("countUpTo( ");
    // ... loop body ...
    appendStatement(loop.body.getValue());
  });
}
```

### Step 2: appendCodeFlowStatement splits work

```java
// TweedleEncoder.java
@Override
protected void appendCodeFlowStatement(Statement stmt, Runnable appender) {
  appendIndent(stmt);
  appender.run();
  statementEncoder.appendStatementEnd(stmt);
}
```

The coordinator handles `appendIndent(stmt)` and runs the appender lambda.
The statement-end formatting delegates to `StatementEncoder`. This split keeps
indentation management on the coordinator while statement-end markers live on
the delegate.

## Summary of the delegation pattern

| TweedleEncoder @Override | Calls on StatementEncoder |
| --- | --- |
| `appendStatementCompletion(Statement)` | `statementEncoder.appendStatementCompletion(stmt)` |
| `appendStatementCompletion()` | `statementEncoder.appendStatementCompletion()` |
| `pushStatementDisabled()` | `statementEncoder.pushStatementDisabled()` then `super.pushStatementDisabled()` |
| `appendCodeFlowStatement(Statement, Runnable)` | `statementEncoder.appendStatementEnd(stmt)` (after indent + appender) |

## Exercises

1. **Find the no-arg completion path.** Starting from `appendResourceInstance`
   (which calls `appendStatementCompletion()` with no argument), trace through
   the delegate to see that it calls `encoder.superAppendStatementCompletion()` then
   `encoder.forwardAppendNewLine()`.

2. **Verify super ordering.** In `pushStatementDisabled`, confirm that
   `NODE_DISABLE` is written before `super.pushStatementDisabled()` increments
   the counter. What would break if the order were reversed?

3. **Compare with decode side.** Open `StatementDecoder.java` and compare its
   constructor pattern (`StatementDecoder(Decoder, ExpressionDecoder)`) with
   `StatementEncoder(TweedleEncoder)`. Note the symmetry: both delegates hold
   a back-reference to their coordinator.
