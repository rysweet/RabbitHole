# Tutorial: Trace Simple If-Statement Decode

This tutorial walks through the implemented simple Tweedle
`if (condition) { ... }` decode slice. It uses raw Tweedle source
and the public `TweedleEncoderDecoder` path.

## Goal

Protect this narrow behavior:

```text
A simple Tweedle if statement with a supported Boolean condition and a body
containing allowlisted decoded statements becomes an Alice ConditionalStatement.
The body can include an explicit zero-argument this.method(); call.
```

This tutorial proves the focused simple-if behavior. It does not prove full
conditional decode, broad method-call decode, or full player archive decode.

## Implementation boundary

The decoder change must be narrow. Conditional branch body decoding should use
an explicit allowlist:

1. Existing assignment statements in conditional bodies.
2. Explicit zero-argument `this.method();` expression statements.

Do not delegate to unrestricted method-body decoding. That would risk enabling
locals, returns, nested conditionals, arbitrary receivers, implicit receivers,
arguments, overloads, or optional-argument behavior inside conditional bodies.

## 1. Create the smallest source

Use a helper method and a caller method with a simple condition:

```java
String source = """
    class Program {
      void helper() {
      }

      void run(Boolean enabled) {
        if (enabled) {
          this.helper();
        }
      }
    }
    """;
```

The helper has no parameters. The call uses explicit `this` and no arguments.
The condition is already a supported Boolean parameter access.

## 2. Decode through the public API

Decode the source using the existing test helper in
`TweedleEncoderDecoderTest`:

```java
NamedUserType type = decodeUserType(source);
```

This is the intended public test path because it exercises the Tweedle parser
and AST decoder together.

## 3. Locate the methods

Find the helper method and the caller method:

```java
UserMethod helper = userMethodNamed(type, "helper");
UserMethod run = userMethodNamed(type, "run");
```

Compare the decoded invocation against the `helper` `UserMethod` object, not
only the method name.

## 4. Assert the conditional shape

The caller body contains one `ConditionalStatement`:

```java
assertEquals(1, run.body.getValue().statements.size());
assertTrue(run.body.getValue().statements.get(0) instanceof ConditionalStatement);

ConditionalStatement conditional =
    (ConditionalStatement) run.body.getValue().statements.get(0);
assertEquals(1, conditional.booleanExpressionBodyPairs.size());
assertEquals(0, conditional.elseBody.getValue().statements.size());
```

The conditional has one condition/body pair. The pair expression preserves the
decoded Boolean condition:

```java
BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
assertTrue(pair.expression.getValue() instanceof ParameterAccess);
```

## 5. Assert the body statement

The `if` body contains the decoded `this.helper();` call:

```java
assertEquals(1, pair.body.getValue().statements.size());
assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);

ExpressionStatement statement =
    (ExpressionStatement) pair.body.getValue().statements.get(0);
assertTrue(statement.expression.getValue() instanceof MethodInvocation);

MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();
assertSame(helper, invocation.method.getValue());
assertTrue(invocation.expression.getValue() instanceof ThisExpression);
```

These assertions document the public AST contract. They do not depend on
private decoder helper names.

## 6. Add a logical/comparison condition example

Add a second source that proves existing logical and comparison support is used
inside the `if` condition:

```java
String source = """
    class Program {
      WholeNumber count <- 0;

      void helper() {
      }

      void run(Boolean enabled) {
        if (enabled && count < 3) {
          this.helper();
          count <- 1;
        }
      }
    }
    """;
```

Assert the condition is the decoded logical expression and the allowlisted body
statements remain in source order.

## 7. Add unsupported-neighbor checks

Protect nearby unsupported cases:

```java
if (enabled) { this.helper(value: 1); }  // argument-bearing calls fail fast
if (enabled) { other.helper(); }         // arbitrary receivers remain unsupported
if (enabled) { if (ready) { this.helper(); } } // nested conditionals remain unsupported
```

Each test should assert `UnsupportedTweedleDecodeException`. For the
argument-bearing explicit `this` call, also assert the diagnostic contains:

```text
argument-bearing explicit this method calls
```

Unsupported body statements must fail closed. Do not assert that the decoder
returns a partial conditional with unsupported statements skipped.

## 8. Run validation

Run the focused test from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

For JSON player archive coverage, run the focused archive reader test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

The result should protect simple-if decode with supported conditions and
allowlisted body statements. It does not prove full conditional, general
method-call, arbitrary receiver, nested control-flow, or full Tweedle/player
archive support.
