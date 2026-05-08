# Characterize Zero-Argument This-Method Call Decode

Use this guide when reviewing or extending decoder coverage for the implemented
`this.someMethod()` slice.

This guide applies only to same-type, zero-argument calls with an explicit
`this` target and the adjacent argument-bearing explicit
`this.method(label: value, ...)`
fail-fast boundary. It documents the feature boundary, not broader Tweedle
method support. See the
[zero-argument this-method call decode reference](../reference/zero-argument-this-method-call-decode.md)
for the full boundary.

## Prerequisites

Work in the focused Tweedle-to-AST decoder suite:

```text
core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java
```

Use the public decoder entry point:

```java
NamedUserType type = decodeUserType(source);
```

Do not test private helpers or bypass the Tweedle parser.

## Add the positive characterization

Use the smallest class that declares a zero-argument helper method and calls it
from another method on `this`:

```java
NamedUserType type = decodeUserType("""
    class Program {
      void helper() {
      }

      void run() {
        this.helper();
      }
    }
    """);
```

Assert the observable AST shape:

1. The decoded type contains `helper` and `run`.
2. `run` has one body statement.
3. The statement is an `ExpressionStatement`.
4. The expression is a `MethodInvocation`.
5. The invocation resolves to the `helper` `UserMethod`.
6. The invocation target is the current instance, not an external receiver.

Use a test name that states the implemented slice:

```java
@Test
public void zeroArgumentThisMethodCallDecodeCreatesMethodInvocation() throws Exception {
  // decode and assert the MethodInvocation shape
}
```

## Add negative characterizations

Add focused failures for adjacent syntax that the decoder still rejects.

### Argument-bearing explicit this call

```java
UnsupportedTweedleDecodeException thrown = assertThrows(
    UnsupportedTweedleDecodeException.class,
    () -> coder.decode("""
        class Program {
          void helper(WholeNumber value) {
          }

          void run() {
            this.helper(value: 1);
          }
        }
        """));
assertTrue(thrown.getMessage().contains(
    "argument-bearing explicit this method calls"));
```

The failure proves the decoder rejects argument-bearing explicit `this` calls
using current Tweedle labeled-argument syntax before decoding argument
expressions, binding parameters, applying optional arguments, or performing
overload resolution.

### Unknown this call

```java
assertThrows(
    UnsupportedTweedleDecodeException.class,
    () -> coder.decode("""
        class Program {
          void run() {
            this.missing();
          }
        }
        """));
```

The failure proves the decoder does not synthesize methods or perform runtime
lookup.

### Non-this target

```java
assertThrows(
    UnsupportedTweedleDecodeException.class,
    () -> coder.decode("""
        class Program {
          Program other;
          void helper() {
          }

          void run() {
            other.helper();
          }
        }
        """));
```

The failure proves the decoder does not resolve external targets or general
member calls.

The reference also lists implicit receiver calls, static-style calls, object
construction calls, chained calls, and member access as non-goals. Focused tests
may cover the forms that route through this slice; otherwise keep them
documented as outside the accepted method-call shape.

## Keep the scope narrow

Do not add assertions or documentation that imply support for:

- method arguments;
- static calls;
- object construction calls;
- chained calls;
- implicit receiver calls;
- inherited methods;
- external targets;
- overload resolution;
- general Tweedle/player decode.

If a new test needs one of those forms, document it as unsupported unless a
separate decoder slice explicitly implements it.

## Run the focused gate

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```
