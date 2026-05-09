# Simple If-Statement Decode

This page defines the Tweedle AST decoder contract for simple
`if (condition) { ... }` statements whose condition uses existing Boolean,
logical, or comparison expression support and whose body contains an explicit
conditional-body allowlist, especially explicit zero-argument `this.method();`
calls.

This is not full Tweedle conditional decode. The feature does not add general
`if/else`, nested conditional, argument-bearing method-call, arbitrary receiver,
or new statement-family support.

This page describes the implemented simple-if slice. Decoder behavior also
continues to cover the pre-existing assignment-only `if/else` body case.

## Usage

The direct decoder entry point for raw Tweedle source accepts this shape:

```java
NamedUserType type = (NamedUserType) new TweedleEncoderDecoder().decode("""
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

The decoded `run` method contains one Alice
`ConditionalStatement`. Its first `BooleanExpressionBodyPair` preserves the
decoded condition expression. Its body contains the decoded allowlisted
statements in order: a `MethodInvocation` statement for `this.helper();` and an
assignment expression statement for `count <- 1;`.

## Supported shape

All of these conditions must be true:

| Requirement | Contract |
| --- | --- |
| Conditional form | A simple Tweedle `if (condition) { ... }` statement. |
| Else branch | No `else` branch for the new method-call body support. Existing assignment-only `if/else` behavior remains unchanged. |
| Condition type | The decoded condition is assignable to `Boolean`. |
| Condition expressions | Existing Boolean parameter/field access, logical expressions, comparisons, and combinations already supported by the Tweedle expression decoder. |
| Body statements | Explicit conditional-body allowlist only: existing assignment statements plus explicit zero-argument `this.method();` expression statements. |
| Method-call target | For method calls, the target is explicit `this`. |
| Method-call arguments | For method calls, the Tweedle call has no arguments and resolves to a same-type zero-argument `UserMethod`. |
| AST result | The decoder returns a `ConditionalStatement` with one `BooleanExpressionBodyPair` and an empty `elseBody`. |

The decoder preserves statement order in the `if` body. It must fail closed
before returning a partial conditional body when any body statement is
unsupported.

## API behavior

`TweedleEncoderDecoder.decode(String source)` accepts a simple-if method
body when the condition and every body statement are inside the supported shape.

```java
class Program {
  void helper() {
  }

  void run(WholeNumber count, Boolean ready) {
    if (ready && count < 3) {
      this.helper();
    }
  }
}
```

The decoded Alice AST shape is:

```text
UserMethod run
└── BlockStatement
    └── ConditionalStatement
        ├── booleanExpressionBodyPairs[0]
        │   ├── expression: supported Boolean logical/comparison expression
        │   └── body
        │       └── ExpressionStatement(MethodInvocation(this, helper))
        └── elseBody: empty BlockStatement
```

Existing assignment-only `if/else` decode remains valid:

```java
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
```

The new simple-if support must not broaden that existing `if/else` contract.
An `else` branch containing `this.helper();`, nested `if`, argument-bearing
calls, arbitrary receivers, local declarations, return statements, or other
unsupported statements remains outside this slice unless a separate decoder
contract implements it.

## Implementation boundary

Conditional branch body decoding changed without changing the public decoder API.
The simple-if path uses a small allowlisted conditional-body statement decoder
that accepts:

1. The assignment statements already supported in conditional bodies.
2. Explicit zero-argument `this.method();` expression statements that satisfy
   the existing direct-call slice.

Do not reuse unrestricted method-body decoding without the allowlist. This
avoids accidentally enabling local declarations, returns, nested conditionals,
arbitrary receiver calls, implicit receiver calls, arguments, overloads, or
optional-argument behavior inside conditional bodies.

## Player archive behavior

JSON player archives that already satisfy the existing JSON `.a3w` reader
requirements pass manifest-declared Tweedle types through this same decoder
slice.

```text
version.txt
manifest.json
src/Program.twe
```

For a supported simple-if program type:

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

the public archive read path should return a project whose decoded program type
contains the same `ConditionalStatement` shape:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
NamedUserType programType = project.getProgramType();
```

This archive behavior does not claim full player Tweedle decode. Unsupported
Tweedle inside manifest-declared types continues through the existing
unsupported-Tweedle diagnostics path and must not be silently skipped.

## Unsupported neighboring syntax

Direct decoder calls must fail with `UnsupportedTweedleDecodeException` for
adjacent forms that are outside the slice.

| Tweedle shape | Required boundary |
| --- | --- |
| `if (enabled) { this.helper(value: 1); }` | Argument-bearing explicit `this` method calls remain unsupported. |
| `if (enabled) { other.helper(); }` | Arbitrary receivers remain unsupported. |
| `if (enabled) { helper(); }` | Implicit receivers remain unsupported. |
| `if (enabled) { if (ready) { this.helper(); } }` | Nested conditionals remain unsupported inside this slice. |
| `if (enabled) { WholeNumber x <- 1; }` | New local declarations inside conditional bodies are outside this slice. |
| `if (enabled) { return 1; }` | Return statements inside conditional bodies are outside this slice. |
| `if (1) { this.helper(); }` | Non-Boolean conditions remain unsupported. |
| `if (enabled) { this.missing(); }` | Unknown same-type methods are not invented or late-bound. |
| `if (enabled) { this.helper().again(); }` | Chained calls remain unsupported. |

The argument-bearing explicit `this` boundary uses the same stable diagnostic
label as the direct call slice:

```text
argument-bearing explicit this method calls
```

Nested or unsupported body statements should use clear
`UnsupportedTweedleDecodeException` diagnostics that identify conditional body
decode rather than returning a partial `ConditionalStatement`.

## Configuration

There is no runtime configuration for simple-if decode. It uses the existing
Tweedle grammar, AST decoder, JSON archive reader, Maven, and JUnit
configuration.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Automation can keep the saved Node memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

`NODE_OPTIONS` is not an Alice decode option.

## Validation

Run the focused core AST decoder characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

Run the relevant archive reader characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

## Characterization coverage

Focused positive tests prove:

1. A simple `if` with a comparison condition decodes to `ConditionalStatement`.
2. Logical and comparison condition expressions are preserved in the
   `BooleanExpressionBodyPair`.
3. A body containing `this.helper();` decodes that statement as a
   `MethodInvocation`.
4. Multiple allowlisted body statements keep source order.
5. Existing assignment-only `if/else` behavior still decodes both branches.

Focused negative tests prove:

1. Argument-bearing `this.helper(value: 1);` inside the `if` body throws
   `UnsupportedTweedleDecodeException`.
2. Nested `if` inside the body throws `UnsupportedTweedleDecodeException`.
3. Arbitrary receiver calls such as `other.helper();` throw
   `UnsupportedTweedleDecodeException`.
4. Unsupported body statements fail closed and do not return partial
   conditional AST.

The direct decoder tests live in:

```text
core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java
```

Archive-level coverage lives in the existing JSON archive reader
characterization suite under:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/
```

## Non-goals

This feature does not decode full Tweedle conditionals, general `if/else`
bodies, `else if`, nested conditionals, argument-bearing method calls, arbitrary
receivers, implicit receivers, chained calls, overloads, optional arguments,
new expression families, new statement families, runtime dispatch, or full
Tweedle/player archives.
