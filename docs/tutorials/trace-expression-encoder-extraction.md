# Tutorial: Trace the ExpressionEncoder Extraction

A guided walkthrough showing how expression-encoding requests flow through
`TweedleEncoder` and its new `ExpressionEncoder` delegate. Use this to
understand the bridge and forwarding patterns before extending expression
encoding behavior.

## Prerequisites

- Familiarity with the visitor pattern used by `SourceCodeGenerator`
- Access to the `core/ast` source in `org.alice.serialization.tweedle`
- Recommended: read the [StatementEncoder extraction tutorial](./trace-statement-encoder-extraction.md)
  for the step 1 mirror pattern

## Overview

When an expression involving a target and member is encoded to Tweedle source,
the formatting passes through two classes:

```text
TweedleEncoder (@Override methods — visitor dispatch)
  └── ExpressionEncoder (extracted expression logic)
      ├── appendTargetAndMember(Expression, String, AbstractType)
      ├── targetIsMath(Expression)          (private helper)
      ├── tweedleModuleForMath(String, AbstractType) (private helper)
      └── processResourceExpression(ResourceExpression)
```

`TweedleEncoder` keeps the `@Override` stubs (required by `SourceCodeGenerator`)
and delegates the extracted logic to `ExpressionEncoder`. Forwarding methods
provide access to inherited methods that the delegate cannot (or should not)
call directly — two are `protected` and one is `public` but wrapped for
uniformity.

## Trace 1: A normal method invocation target

Follow a method call like `this.biped.turn(LEFT, 0.5)` where `biped` is
a field target (not a Math target).

### Step 1: Visitor dispatch

When `processMethodInvocation(MethodInvocation)` runs on `TweedleEncoder`,
it eventually calls `appendTargetAndMember(target, memberName, returnType)`.

### Step 2: TweedleEncoder @Override

```java
// TweedleEncoder.java
@Override
protected void appendTargetAndMember(Expression target, String member,
    AbstractType<?, ?, ?> returnType) {
  expressionEncoder.appendTargetAndMember(target, member, returnType);
}
```

The body is a one-line delegation. The `@Override` annotation stays on
`TweedleEncoder` because `SourceCodeGenerator` declares `appendTargetAndMember`
as a `protected` method, and Java requires the override on the subclass.

### Step 3: ExpressionEncoder logic

```java
// ExpressionEncoder.java
void appendTargetAndMember(Expression target, String member,
    AbstractType<?, ?, ?> returnType) {
  if (targetIsMath(target)) {
    encoder.forwardAppendString(tweedleModuleForMath(member, returnType));
  } else {
    encoder.forwardProcessExpression(target);
  }
  encoder.forwardAppendAccessSeparator();

  String tweedleName = TweedleEncoder.membersToRename.get(member);
  encoder.forwardAppendString(tweedleName == null ? member : tweedleName);
}
```

Since `biped` is not a Math target, `targetIsMath` returns `false`, so the
delegate calls `forwardProcessExpression(target)` to encode the target
expression. Then it appends the access separator (`.`). Finally, it looks up
the member name in the `membersToRename` map — if no rename exists, the
original name is used.

### Step 4: Forwarding methods

```java
// TweedleEncoder.java (package-private forwarding)
void forwardProcessExpression(Expression expression) {
  processExpression(expression);
}

void forwardAppendAccessSeparator() {
  appendAccessSeparator();
}
```

These exist because `appendAccessSeparator` is a `protected` method inherited
from `SourceCodeGenerator` (in package `org.lgna.project.ast`). Java
accessibility rules prevent `ExpressionEncoder` (same package as
`TweedleEncoder`, but not a subclass of `SourceCodeGenerator`) from calling
`protected` methods directly. Note: `processExpression` is actually `public` on
`SourceCodeGenerator`, so `forwardProcessExpression` is technically unnecessary
— it is included for uniformity with the other forwarding methods.

**Key difference from StatementEncoder:** The StatementEncoder bridges call
`super.method()` to invoke the parent implementation that the override would
otherwise replace. The ExpressionEncoder forwarding methods simply call the
inherited method — there is no `super` vs `this` distinction because the
extracted methods fully implement the behavior rather than wrapping a parent
implementation.

## Trace 2: A Math.sin() invocation

Follow a call like `Math.sin(angle)` where the target is a `TypeExpression`
wrapping `java.lang.Math`.

### Step 1: appendTargetAndMember delegates

Same as Trace 1: `TweedleEncoder.appendTargetAndMember` delegates to
`ExpressionEncoder.appendTargetAndMember`.

### Step 2: targetIsMath returns true

```java
// ExpressionEncoder.java
private boolean targetIsMath(Expression target) {
  if (target instanceof TypeExpression expression) {
    AbstractType<?, ?, ?> innerType = expression.value.getValue();
    return innerType instanceof JavaType && "Math".equals(innerType.getName());
  }
  return false;
}
```

The target is a `TypeExpression` containing a `JavaType` named `"Math"`, so
`targetIsMath` returns `true`.

### Step 3: tweedleModuleForMath routes to $Angle

```java
// ExpressionEncoder.java
private String tweedleModuleForMath(String member, AbstractType<?, ?, ?> returnType) {
  if (returnType != null && "int".equals(returnType.getName())) {
    return "$WholeNumber";
  }
  if (TweedleEncoder.angleMembers.contains(member)) {
    return "$Angle";
  }
  return "$DecimalNumber";
}
```

`sin` is in the `angleMembers` set (populated in `TweedleEncoder`'s `static {}`
block), so the method returns `"$Angle"`.

**Key insight:** `angleMembers` was widened from `private` to package-private
so `ExpressionEncoder` can read it directly. No bridge method is needed for
static field access — package-private visibility is sufficient.

### Step 4: Output produced

The delegate calls:
1. `encoder.forwardAppendString("$Angle")` — writes the Tweedle module name
2. `encoder.forwardAppendAccessSeparator()` — writes `.`
3. `encoder.forwardAppendString("sin")` — no rename exists for `sin`

Result: `$Angle.sin`

### Routing table

| Condition | Module |
| --- | --- |
| Return type is `int` | `$WholeNumber` |
| Member is in `angleMembers` (`sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `PI`) | `$Angle` |
| Otherwise | `$DecimalNumber` |

## Trace 3: A resource expression

Follow a `ResourceExpression` through the encoder.

### Step 1: Visitor dispatch

When `processResourceExpression(ResourceExpression)` is called by the visitor
pattern on `TweedleEncoder`:

```java
// TweedleEncoder.java
@Override
public void processResourceExpression(ResourceExpression resourceExpression) {
  expressionEncoder.processResourceExpression(resourceExpression);
}
```

### Step 2: ExpressionEncoder encodes the resource name

```java
// ExpressionEncoder.java
void processResourceExpression(ResourceExpression resourceExpression) {
  encoder.forwardAppendEscapedString(resourceExpression.resource.getValue().getName());
}
```

The delegate retrieves the resource name and writes it as a quoted, escaped
string via the `forwardAppendEscapedString` bridge method.

### Step 3: Forwarding method

```java
// TweedleEncoder.java
void forwardAppendEscapedString(String value) {
  appendEscapedString(value);
}
```

`appendEscapedString` is a `protected` method on `SourceCodeGenerator` that
writes the value wrapped in quotes with proper escaping.

## Trace 4: Member rename (Math.round)

Follow `Math.rint(x)` — the Java `rint` method is renamed to `round` in
Tweedle.

### Step 1–2: Math target detected

Same as Trace 2. Since `rint` returns `double`, and `rint` is not in
`angleMembers`, `tweedleModuleForMath` returns `"$DecimalNumber"`.

### Step 3: Member rename lookup

```java
String tweedleName = TweedleEncoder.membersToRename.get("rint");
// Returns "round" — populated in static initializer: membersToRename.put("rint", "round")
encoder.forwardAppendString("round");
```

Result: `$DecimalNumber.round`

**Key insight:** `membersToRename` was widened from `private` to package-private
so `ExpressionEncoder` can read it. The rename map is immutable after class
initialization and contains entries like `rint→round`, `ceil→ceiling`, etc.

## Summary of the delegation pattern

| TweedleEncoder @Override | Calls on ExpressionEncoder |
| --- | --- |
| `appendTargetAndMember(Expression, String, AbstractType)` | `expressionEncoder.appendTargetAndMember(target, member, returnType)` |
| `processResourceExpression(ResourceExpression)` | `expressionEncoder.processResourceExpression(resourceExpression)` |

| ExpressionEncoder method | Visibility | Calls back to |
| --- | --- | --- |
| `appendTargetAndMember` | package-private | `forwardProcessExpression`, `forwardAppendAccessSeparator`, `forwardAppendString` |
| `targetIsMath` | private | (pure logic, no callbacks) |
| `tweedleModuleForMath` | private | reads `TweedleEncoder.angleMembers` |
| `processResourceExpression` | package-private | `forwardAppendEscapedString` |

## Exercises

1. **Trace Math.round manually.** Starting from `appendTargetAndMember` with
   target `TypeExpression(Math)` and member `"rint"`, trace through
   `targetIsMath`, `tweedleModuleForMath`, and the member rename to confirm the
   output is `$DecimalNumber.round`.

2. **Compare with StatementEncoder bridges.** In `StatementEncoder`,
   `superAppendStatementCompletion(stmt)` calls `super.appendStatementCompletion(stmt)`.
   In `ExpressionEncoder`, `forwardProcessExpression(target)` calls
   `processExpression(target)` (not `super`). Why is `super` not needed here?
   (Answer: The extracted methods fully implement the behavior — they do not
   wrap a parent implementation.)

3. **Verify static map access.** Open `TweedleEncoder.java` and confirm that
   `angleMembers` and `membersToRename` have no `private` modifier. Then open
   `ExpressionEncoder.java` and confirm they are accessed as
   `TweedleEncoder.angleMembers` and `TweedleEncoder.membersToRename`.

4. **Compare with decode side.** Open `ExpressionDecoder.java` and compare its
   constructor pattern with `ExpressionEncoder(TweedleEncoder)`. Note the
   symmetry: both delegates hold a back-reference to their coordinator.
