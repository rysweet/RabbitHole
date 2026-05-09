# Characterize Simple If-Statement Decode

Use this guide when adding or reviewing characterization tests for the
simple Tweedle `if (condition) { ... }` decode slice.

This guide covers the implemented feature: simple-if bodies with an
explicit conditional-body statement allowlist. The allowlist includes existing
assignment statements and explicit zero-argument `this.method();` expression
statements. It does not cover full `if/else`, nested conditionals, arbitrary
receiver calls, unrestricted method-body statement decoding, or full
Tweedle/player decode. See the
[simple if-statement decode reference](../reference/simple-if-statement-decode.md)
for the complete boundary.

## Prerequisites

Work in the focused Tweedle-to-AST decoder suite:

```text
core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java
```

Use the public decoder path:

```java
NamedUserType type = decodeUserType(source);
```

Do not test private decoder helpers or parser internals.

## Add the positive characterization

Add this characterization with the decoder change. Use the smallest source that
proves both the condition and body shape:

```java
NamedUserType type = decodeUserType("""
    class Program {
      WholeNumber count <- 0;

      void helper() {
      }

      void run(Boolean enabled) {
        if (enabled && count == 0) {
          this.helper();
          count <- 1;
        }
      }
    }
    """);
```

Assert the observable Alice AST:

1. `run` contains one body statement.
2. The statement is a `ConditionalStatement`.
3. The conditional contains one `BooleanExpressionBodyPair`.
4. The pair expression is the decoded logical/comparison condition.
5. The pair body contains the decoded allowlisted body statements in source
   order.
6. The `this.helper();` body statement is an `ExpressionStatement` wrapping a
   `MethodInvocation`.
7. The invocation resolves to the decoded same-type `helper` `UserMethod`.
8. The conditional `elseBody` is empty.

Keep assertions on public AST objects such as `ConditionalStatement`,
`BooleanExpressionBodyPair`, `ExpressionStatement`, `MethodInvocation`, and the
resolved `UserMethod`.

## Review the decoder boundary

The simple-if conditional body decoder uses a narrow allowlist rather than
delegating to unrestricted method-body decoding.

The allowlist accepts:

1. Existing assignment statements in conditional bodies.
2. Explicit zero-argument `this.method();` expression statements that satisfy
   the existing direct-call slice.

It must continue to reject local declarations, returns, while loops, nested
conditionals, argument-bearing calls, arbitrary receivers, implicit receivers,
chained calls, and unresolved same-type methods.

## Preserve existing if/else behavior

Keep the existing assignment-only `if/else` characterization:

```java
NamedUserType type = decodeUserType("""
    class Program {
      WholeNumber count <- 0;

      void toggle(Boolean enabled) {
        if (enabled) {
          count <- 1;
        } else {
          count <- 0;
        }
      }
    }
    """);
```

Assert both branches still contain assignment statements. Do not add
`this.helper();` to the `else` branch unless a separate decoder contract
implements non-assignment `if/else` bodies.

## Add unsupported-neighbor tests

Each unsupported neighbor should call `coder.decode(...)` and assert
`UnsupportedTweedleDecodeException`.

### Argument-bearing explicit this call

```java
UnsupportedTweedleDecodeException thrown = assertThrows(
    UnsupportedTweedleDecodeException.class,
    () -> coder.decode("""
        class Program {
          void helper(WholeNumber value) {
          }

          void run(Boolean enabled) {
            if (enabled) {
              this.helper(value: 1);
            }
          }
        }
        """));
assertTrue(thrown.getMessage().contains(
    "argument-bearing explicit this method calls"));
```

This proves the simple-if path does not decode arguments, bind labels, apply
optional parameters, or perform overload resolution.

### Nested conditional

```java
assertThrows(
    UnsupportedTweedleDecodeException.class,
    () -> coder.decode("""
        class Program {
          void helper() {
          }

          void run(Boolean enabled, Boolean ready) {
            if (enabled) {
              if (ready) {
                this.helper();
              }
            }
          }
        }
        """));
```

This protects the boundary against accidental recursive conditional support.

### Arbitrary receiver call

```java
assertThrows(
    UnsupportedTweedleDecodeException.class,
    () -> coder.decode("""
        class Program {
          Program other;

          void helper() {
          }

          void run(Boolean enabled) {
            if (enabled) {
              other.helper();
            }
          }
        }
        """));
```

This proves the simple-if body still reuses the zero-argument explicit
`this.method();` boundary rather than adding general member-call resolution.

## Add archive coverage when useful

Archive coverage is useful when the change touches `JsonProjectIo` behavior or
when a player archive fixture already exercises the direct decoder path.

Use a generated JSON `.a3w` archive with a manifest-declared `src/Program.twe`
entry:

```java
class Program extends SProgram {
  void helper() {
  }

  void run(Boolean enabled) {
    if (enabled) {
      this.helper();
    }
  }
}
```

Read through the public archive API:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
NamedUserType programType = project.getProgramType();
```

Once the direct decoder slice is implemented, assert only the supported shape:
the program type decodes, the `run` method contains a `ConditionalStatement`,
and the body contains the `this.helper();` `MethodInvocation`. Do not use this
test to claim full player archive Tweedle decode.

## Run focused validation

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

For archive reader coverage, also run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```
