# Zero-Argument This-Method Call Decode and Argument-Bearing Boundary

This page defines the narrow Tweedle AST decoder feature contract for
`this.someMethod()` where `someMethod` is a zero-argument `UserMethod` declared
on the same decoded `NamedUserType`, and the adjacent fail-fast boundary for
argument-bearing explicit `this.method(label: value, ...)` calls.

The feature is limited to same-type zero-argument calls on explicit `this`.
Argument-bearing explicit `this` calls are intentionally unsupported. This is
not a general Tweedle method-resolution system, and this document should not be
read as a claim that broader method-call decode already exists.

## API behavior

`TweedleEncoderDecoder.decode(String source)` accepts a Tweedle class whose
method or constructor body contains an expression statement that calls a known
zero-argument method on `this`.

```java
class Program {
  void helper() {
  }

  void run() {
    this.helper();
  }
}
```

The decoded `NamedUserType` should contain both methods. The `run` method body
should contain one `ExpressionStatement`, and that statement should wrap an
Alice `MethodInvocation` whose method declaration is the decoded `helper`
`UserMethod`.

## Supported shape

All of these conditions must be true:

| Requirement | Contract |
| --- | --- |
| Target | The call target is explicit `this`. |
| Method owner | The resolved method is a supported method declaration directly on the currently decoded type. |
| Parsed arguments | The Tweedle call has no arguments. |
| Resolved parameters | The resolved `UserMethod` has no required or optional parameters. |
| Statement form | The call appears as a method-body or constructor-body expression statement. |
| Resolution | The method name exactly matches one decoded method on the current type. |

The implementation must resolve methods declared before or after the calling
method in the same Tweedle class. Register same-type `UserMethod` declarations
before decoding any method bodies so forward calls and backward calls share the
same observable behavior.

## Unsupported adjacent syntax

Unsupported call forms must continue to fail with
`UnsupportedTweedleDecodeException` when called through the direct Tweedle
decoder. JSON player and type archive readers keep their documented
unsupported-Tweedle archive behavior.

| Tweedle shape | Reason it is unsupported |
| --- | --- |
| `this.helper(value: 1);` | Argument-bearing explicit `this` method calls intentionally fail fast. Current Tweedle argument syntax uses labels. |
| `this.missing();` | Unknown same-type methods are not invented or late-bound. |
| `other.helper();` | Non-`this` targets are outside the slice. |
| `helper();` | Implicit targets are outside the slice. |
| `Program.helper();` | Static-style calls are outside the slice. |
| `new Helper();` | Object construction calls are outside the slice. |
| `this.helper().again();` | Chained calls are outside the slice. |
| `this.helper` | Member access is a separate decoder contract. |

The focused implementation tests cover the nearest rejection boundaries:
argument-bearing `this` calls, unknown same-type methods, and non-`this`
targets. They also protect selected adjacent non-goals that currently route
through this decoder boundary, including optional-parameter targets, duplicate
zero-argument method names, static targets, chained calls, and implicit targets.

## Argument-bearing explicit this boundary

Argument-bearing explicit `this` calls are the named unsupported shard next to
the supported zero-argument call form. Current Tweedle represents this shape
with labeled arguments such as `this.method(value: 1)`. The decoder rejects this
syntax before decoding the argument expressions or attempting semantic method
binding.

```java
class Program {
  void helper(WholeNumber value) {
  }

  void run() {
    this.helper(value: 1);
  }
}
```

Direct calls to `TweedleEncoderDecoder.decode(String source)` throw
`UnsupportedTweedleDecodeException`. The diagnostic must contain:

```text
argument-bearing explicit this method calls
```

That message is the public boundary label for this shard. It is intentionally
generic: it does not log the argument values, source file path, full project
source, or user content.

This boundary does not support:

| Non-goal | Reason |
| --- | --- |
| Argument-expression decode | No argument AST is produced for this unsupported call. |
| Labeled or optional arguments | Parameter binding is outside this slice. |
| Overload resolution | The decoder does not choose among methods by argument shape. |
| Positional arguments | Current Tweedle argument syntax uses labels; this slice does not add positional-argument support. |
| Implicit receivers | `helper(value: 1)` remains outside this explicit-`this` boundary. |
| Non-`this` receivers | `other.helper(value: 1)` remains outside this boundary. |
| Chained calls | `this.helper(value: 1).again()` remains outside this boundary. |
| Static-style calls | `Program.helper(value: 1)` remains outside this boundary. |

Archive readers preserve the existing unsupported decode behavior: unsupported
manifest-declared Tweedle fails through the archive reader's fail-closed path
rather than becoming partial player decode support.

## Configuration

The zero-argument this-method call feature has no runtime configuration. It uses
the existing Tweedle parser, AST decoder, Maven, and JUnit configuration.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before broad Maven validation:

```bash
git submodule update --init tweedle-lang
```

Automation that runs repository tooling can keep the saved Node memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

That environment variable is not an Alice decode option.

## Validation command

Run the focused decoder characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

## Characterization tests

The focused positive test uses a name that states the narrow supported behavior:

```text
zeroArgumentThisMethodCallDecodeCreatesMethodInvocation
```

The selected argument-bearing explicit `this` boundary uses a name that states
the unsupported shard:

```text
decodeClassWithArgumentBearingExplicitThisMethodCallReportsUnsupportedBoundary
```

Additional negative tests cover adjacent unsupported syntax without implying
general method-call support:

```text
decodeClassWithArgumentBearingExplicitThisMethodCallReportsUnsupportedBoundary
zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall
zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod
zeroArgumentThisMethodCallDecodeRejectsUnknownMethod
zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName
zeroArgumentThisMethodCallDecodeRejectsNonThisTarget
zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod
zeroArgumentThisMethodCallDecodeRejectsChainedCall
zeroArgumentThisMethodCallDecodeRejectsImplicitTarget
zeroArgumentThisMethodCallInConstructorDecodeCreatesMethodInvocation
zeroArgumentThisMethodCallInConstructorDecodeRejectsArgumentBearingCall
zeroArgumentThisMethodCallInConstructorDecodeRejectsUnknownMethod
zeroArgumentThisMethodCallInConstructorDecodeRejectsNonThisTarget
zeroArgumentThisMethodCallInConstructorDecodeRejectsStaticTargetMethod
```

These tests belong in:

```text
core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java
```

## Non-goals

This feature must not decode general Tweedle calls, inherited calls, static
calls, object construction calls, implicit receiver calls, external targets,
chained calls, overloads, optional arguments, labeled arguments, or runtime
dispatch.

Keep new docs, tests, and exception messages scoped to zero-argument
`this.method()` decode and the named unsupported boundary for argument-bearing
explicit `this.method(label: value, ...)` calls.
