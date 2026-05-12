# Tutorial: Trace the FormattingEncoder Extraction

A guided walkthrough showing how formatting and indent requests flow through
`TweedleEncoder` and its new `FormattingEncoder` delegate. Use this to
understand the delegation pattern before extending formatting behavior or
adding new helper methods.

## Prerequisites

- Familiarity with the visitor pattern used by `SourceCodeGenerator`
- Access to the `core/ast` source in `org.alice.serialization.tweedle`
- Recommended: read the [StatementEncoder extraction tutorial](./trace-statement-encoder-extraction.md)
  and [ExpressionEncoder extraction tutorial](./trace-expression-encoder-extraction.md)
  for the step 1 and step 2 patterns

## Overview

When Tweedle source output requires indentation, argument formatting, list
rendering, or string quoting, the formatting passes through two classes:

```text
TweedleEncoder (@Override methods — visitor dispatch)
  └── FormattingEncoder (extracted formatting logic)
      ├── pushIndent() / popIndent()     — indent state management
      ├── appendIndent() / appendIndent(Statement) — indent output
      ├── appendVisibilityTag(FieldTemplate) — annotation formatting
      ├── appendInstantiation(String, Runnable) — new ClassName(args)
      ├── appendArg(label, value) × 2    — labeled argument
      ├── appendAnotherArg(label, value) × 2 — separator + argument
      ├── appendList(T[], Consumer, String)  — list rendering
      └── quoteString(String)            — quoted string
```

`TweedleEncoder` keeps the `@Override` stubs and delegates formatting
operations to `FormattingEncoder`. The indent state lives entirely on
`FormattingEncoder`.

## Trace 1: Opening and closing a block

Follow a method body through `openBlock()` and `closeBlockInline()`.

### Step 1: openBlock

When `processMethod` emits a method body, `appendStatement` eventually calls
`openBlock()`:

```java
// TweedleEncoder.java
@Override
protected void openBlock() {
  appendString(" {\n");
  formattingEncoder.pushIndent();
}
```

The `appendString(" {\n")` writes the opening brace and newline. Then
`formattingEncoder.pushIndent()` increments the indent level.

### Step 2: FormattingEncoder manages the indent state

```java
// FormattingEncoder.java
private int indent = 0;

void pushIndent() {
  indent++;
}

void popIndent() {
  indent--;
}
```

The `indent` field lives on `FormattingEncoder`, not `TweedleEncoder`. All
indent reads and writes go through this class.

### Step 3: Inside the block — indented lines

Each statement inside the block calls `appendIndent(stmt)` before writing:

```java
// TweedleEncoder.java
@Override
public void processSingleStatement(Statement stmt, Runnable appender) {
  formattingEncoder.appendIndent(stmt);
  super.processSingleStatement(stmt, appender);
}
```

### Step 4: FormattingEncoder computes the indent string

```java
// FormattingEncoder.java
void appendIndent(Statement stmt) {
  final int level = stmt.isEnabled.getValue() ? this.indent : this.indent - 1;
  encoder.forwardAppendString(indentString(level));
}

private static String indentString(int level) {
  if (level <= 0) {
    return "";
  }
  return level < MAX_CACHED_INDENT ? INDENT_CACHE[level] : INDENTION.repeat(level);
}
```

For enabled statements, the current indent level is used. For disabled
statements, the level is decremented by 1 (to account for the disabled-node
wrapper). The indent string is looked up from the pre-computed cache for
levels under 16, or computed via `String.repeat()` for deeper nesting.

**Key insight:** The indent cache (`INDENT_CACHE`) and its constants
(`INDENTION`, `MAX_CACHED_INDENT`) have moved from `TweedleEncoder` to
`FormattingEncoder`. The static initializer populates the cache at class load
time.

### Step 5: closeBlockInline

```java
// TweedleEncoder.java
@Override
protected void closeBlockInline() {
  formattingEncoder.popIndent();
  formattingEncoder.appendIndent();
  super.closeBlockInline();
}
```

The delegate decrements the indent, writes the reduced indent, and then the
inherited `closeBlockInline()` writes the closing brace `}`.

## Trace 2: Argument formatting (ResourceEncoder path)

Follow a resource field encoding through `appendArg` and `appendAnotherArg`.

### Step 1: ResourceEncoder calls the encoder

When `ResourceEncoder` builds a resource instantiation, it calls:

```java
// ResourceEncoder.java
encoder.appendInstantiation("JointId", () -> {
  encoder.appendArg("id", () -> encoder.quoteString(joint));
  encoder.appendAnotherArg("parent", parentReference);
});
```

`ResourceEncoder` calls `encoder.appendInstantiation(...)` — calling the
method on `TweedleEncoder`, which bridges to `FormattingEncoder`.

### Step 2: TweedleEncoder bridges to FormattingEncoder

```java
// TweedleEncoder.java
void appendInstantiation(String className, Runnable args) {
  formattingEncoder.appendInstantiation(className, args);
}

void appendArg(String label, Runnable value) {
  formattingEncoder.appendArg(label, value);
}

void appendAnotherArg(String label, String value) {
  formattingEncoder.appendAnotherArg(label, value);
}
```

Each method on `TweedleEncoder` is a one-line delegation. `ResourceEncoder`
continues to call `TweedleEncoder` — it never references `FormattingEncoder`
directly.

### Step 3: FormattingEncoder formats the output

```java
// FormattingEncoder.java
void appendInstantiation(String className, Runnable args) {
  encoder.forwardAppendString("new ");
  encoder.forwardAppendString(className);
  encoder.forwardParenthesize(args);
}

void appendArg(String label, Runnable value) {
  encoder.forwardAppendString(label);
  encoder.forwardAppendString(": ");
  value.run();
}

void appendAnotherArg(String label, String value) {
  encoder.forwardAppendString(encoder.forwardGetListSeparator());
  appendArg(label, value);
}
```

The `forwardParenthesize` bridge wraps the args `Runnable` in parentheses
by calling the inherited `parenthesize()` on `SourceCodeGenerator`. The
`forwardGetListSeparator()` bridge returns `", "` from the inherited
`getListSeparator()` method.

### Step 4: Output produced

For `JointId` with `id="head"` and `parent="JOINT_ROOT"`:

```
new JointId(id: "head", parent: JOINT_ROOT)
```

## Trace 3: List rendering

Follow an array of joint transformations through `appendList`.

### Step 1: ResourceEncoder calls appendList

```java
// ResourceEncoder.java (simplified)
encoder.appendList(values, v -> encoder.quoteString(v), ", ");
```

### Step 2: Delegation chain

```java
// TweedleEncoder.java
<T> void appendList(T[] values, Consumer<T> appendValue, String separator) {
  formattingEncoder.appendList(values, appendValue, separator);
}
```

### Step 3: FormattingEncoder renders the list

```java
// FormattingEncoder.java
<T> void appendList(T[] values, Consumer<T> appendValue, String separator) {
  encoder.forwardAppendChar('{');
  int i = 0;
  while (i < values.length) {
    appendValue.accept(values[i]);
    i++;
    if (i < values.length) {
      encoder.forwardAppendString(separator);
    }
  }
  encoder.forwardAppendChar('}');
}
```

The list is wrapped in `{` and `}`. Each value is rendered by the provided
`Consumer`, with separators between values. The `forwardAppendChar` bridge
calls the inherited `appendChar(char)` method on `SourceCodeGenerator`.

### Output

For an array `["a", "b", "c"]` with `quoteString` and `", "` separator:

```
{"a", "b", "c"}
```

## Trace 4: Visibility tag

Follow a `@PrimeTime` field through `appendVisibilityTag`.

### Step 1: ResourceEncoder annotates a field

```java
// ResourceEncoder.java
encoder.appendVisibilityTag(fieldAnnotation);
```

### Step 2: Delegation

```java
// TweedleEncoder.java
void appendVisibilityTag(FieldTemplate fieldAnnotation) {
  formattingEncoder.appendVisibilityTag(fieldAnnotation);
}
```

### Step 3: FormattingEncoder writes the tag

```java
// FormattingEncoder.java
void appendVisibilityTag(FieldTemplate fieldAnnotation) {
  if (fieldAnnotation == null) {
    return;
  }
  switch (fieldAnnotation.visibility()) {
  case COMPLETELY_HIDDEN:
    encoder.forwardAppendString("@CompletelyHidden ");
    break;
  case PRIME_TIME:
    encoder.forwardAppendString("@PrimeTime ");
    break;
  case TUCKED_AWAY:
    encoder.forwardAppendString("@TuckedAway ");
    break;
  default:
  }
}
```

The null check ensures no output is produced for unannotated fields. The switch
writes the corresponding Tweedle annotation string.

## Trace 5: String quoting

Follow a resource name through `quoteString`.

```java
// FormattingEncoder.java
void quoteString(String aString) {
  encoder.forwardAppendChar('"');
  encoder.forwardAppendString(aString);
  encoder.forwardAppendChar('"');
}
```

This wraps the string in literal double-quote characters using `appendChar`.
Unlike `appendEscapedString` (which handles escape sequences), `quoteString`
performs raw quoting for identifiers that need no escaping.

## Summary of the delegation pattern

| TweedleEncoder method | Calls on FormattingEncoder |
| --- | --- |
| `openBlock()` | `formattingEncoder.pushIndent()` |
| `closeBlockInline()` | `formattingEncoder.popIndent()`, `formattingEncoder.appendIndent()` |
| `processSingleStatement(stmt, appender)` | `formattingEncoder.appendIndent(stmt)` |
| `appendCodeFlowStatement(stmt, appender)` | `formattingEncoder.appendIndent(stmt)` |
| `appendIndent()` (no-arg bridge) | `formattingEncoder.appendIndent()` |
| `appendVisibilityTag(annotation)` | `formattingEncoder.appendVisibilityTag(annotation)` |
| `appendInstantiation(name, args)` | `formattingEncoder.appendInstantiation(name, args)` |
| `appendArg(label, value)` ×2 | `formattingEncoder.appendArg(label, value)` |
| `appendAnotherArg(label, value)` ×2 | `formattingEncoder.appendAnotherArg(label, value)` |
| `appendList(values, consumer, sep)` | `formattingEncoder.appendList(values, consumer, sep)` |
| `quoteString(str)` | `formattingEncoder.quoteString(str)` |

| FormattingEncoder method | Visibility | Calls back to |
| --- | --- | --- |
| `pushIndent` | package-private | (internal state only) |
| `popIndent` | package-private | (internal state only) |
| `indentString` | private static | (pure computation) |
| `appendIndent()` | package-private | `forwardAppendString` |
| `appendIndent(Statement)` | package-private | `forwardAppendString` |
| `appendVisibilityTag` | package-private | `forwardAppendString` |
| `appendInstantiation` | package-private | `forwardAppendString`, `forwardParenthesize` |
| `appendArg` ×2 | package-private | `forwardAppendString`, value.run() |
| `appendAnotherArg` ×2 | package-private | `forwardGetListSeparator`, delegates to `appendArg` |
| `appendList` | package-private | `forwardAppendChar`, `forwardAppendString`, consumer.accept() |
| `quoteString` | package-private | `forwardAppendChar`, `forwardAppendString` |

## Exercises

1. **Trace disabled-statement indentation.** Starting from
   `processSingleStatement` with a disabled `LocalDeclarationStatement`, trace
   through `formattingEncoder.appendIndent(stmt)` and confirm that the indent
   level is reduced by 1 compared to enabled statements.

2. **Count the forwarding methods.** List all `forward*` methods on
   `TweedleEncoder` across all three extraction steps. Verify that each exists
   because the target method is `protected` on `SourceCodeGenerator` or because
   it was added for uniformity.

3. **Compare with ResourceEncoder.** Open `ResourceEncoder.java` and confirm
   it calls `encoder.appendArg(...)`, not `formattingEncoder.appendArg(...)`.
   Trace the path: `ResourceEncoder` → `TweedleEncoder` bridge →
   `FormattingEncoder` implementation. This two-hop path maintains the
   convention that delegates never reference each other directly.

4. **Verify indent cache.** Open `FormattingEncoder` and confirm the static
   initializer builds `INDENT_CACHE` from level 0 to `MAX_CACHED_INDENT - 1`.
   Call `indentString(0)` and verify it returns `""`. Call `indentString(3)`
   and verify it returns `"      "` (6 spaces = 3 × 2-space indention).

5. **Compare with decode side.** The decoder decomposition does not have a
   separate formatting delegate — formatting is simpler on the decode side.
   Why does the encode side benefit from a `FormattingEncoder` while the
   decode side does not need one? (Hint: the encoder generates structured
   Tweedle source with indentation and argument labels; the decoder consumes
   tokens.)
