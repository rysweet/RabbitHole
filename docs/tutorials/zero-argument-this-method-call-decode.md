# Tutorial: Add Zero-Argument This-Method Call Decode Coverage

This tutorial walks through the focused characterization to add for Tweedle
`this.helper()` decode. It uses raw Tweedle source and the public
`TweedleEncoderDecoder` path; it does not touch desktop Save or Select Project
proof scopes.

## Goal

Protect the implemented decoder behavior:

```text
A method body expression statement that calls this.<knownZeroArgMethod>() decodes
to an Alice MethodInvocation targeting the current instance and resolving to the
same-type UserMethod declaration.
```

All neighboring call forms remain unsupported unless a separate decoder slice
explicitly implements them.

## 1. Create the smallest Tweedle source

Use one helper method and one caller method:

```java
String source = """
    class Program {
      void helper() {
      }

      void run() {
        this.helper();
      }
    }
    """;
```

The helper has no parameters. The call uses explicit `this` and no arguments.

## 2. Decode through the public API

Decode the source using the existing test helper in
`TweedleEncoderDecoderTest`:

```java
NamedUserType type = decodeUserType(source);
```

This exercises the Tweedle parser and AST decoder together.

## 3. Locate the declared methods

Find the `helper` method and the `run` method from the decoded type:

```java
UserMethod helper = type.getDeclaredMethods().stream()
    .filter(method -> method.getName().equals("helper"))
    .findFirst()
    .orElseThrow();
UserMethod run = type.getDeclaredMethods().stream()
    .filter(method -> method.getName().equals("run"))
    .findFirst()
    .orElseThrow();
```

The assertion should compare against the decoded `UserMethod` object, not just
the method name.

## 4. Assert the invocation shape

Assert the caller body contains exactly one expression statement:

```java
assertEquals(1, run.body.getValue().statements.size());
assertTrue(run.body.getValue().statements.get(0) instanceof ExpressionStatement);
```

Then assert the expression is a `MethodInvocation` that resolves to `helper`:

```java
ExpressionStatement statement =
    (ExpressionStatement) run.body.getValue().statements.get(0);
assertTrue(statement.expression.getValue() instanceof MethodInvocation);

MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();
assertSame(helper, invocation.method.getValue());
assertTrue(invocation.expression.getValue() instanceof ThisExpression);
```

Keep the assertion on the decoded Alice AST. Do not assert parser internals or
private decoder helper names.

## 5. Add unsupported-neighbor tests

Add negative tests for the nearest adjacent unsupported forms:

```java
this.helper(1);     // arguments are unsupported
this.missing();     // unknown methods are unsupported
other.helper();     // non-this targets are unsupported
```

Each test should call `coder.decode(...)` and assert
`UnsupportedTweedleDecodeException`.

Other unsupported forms, including implicit receiver calls, static-style calls,
object construction calls, chained calls, and member access, are non-goals for
this tutorial. Keep any focused tests for those forms tied to this same narrow
decoder boundary.

## 6. Run validation

Run the focused test from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

The result protects only zero-argument `this.method()` decode. It does not prove
general method calls, object construction calls, member access, or full
Tweedle/player decode support.
