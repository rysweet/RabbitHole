# ExpressionEncoder Extraction

This reference describes the extraction of expression-encoding methods from the
`TweedleEncoder` into a new package-private `ExpressionEncoder` class. This is
the second step of RabbitHole issue #506, isolating target-and-member resolution,
Math module routing, and resource expression encoding into a focused companion
class.

The extraction is a pure internal refactor. The public API surface —
`TweedleEncoderDecoder` — is unchanged. All existing encode behavior, error
messages, and Tweedle output are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
  - [ExpressionEncoder](#expressionencoder)
  - [TweedleEncoder changes](#tweedleencoder-changes)
- [Public API](#public-api)
- [Package-private collaboration](#package-private-collaboration)
- [Bridge methods on TweedleEncoder](#bridge-methods-on-tweedleencoder)
- [Visibility changes](#visibility-changes)
- [Security boundary](#security-boundary)
- [Error handling contract](#error-handling-contract)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

After the [StatementEncoder extraction](./statement-encoder-extraction.md)
(step 1), `TweedleEncoder` still mixes expression-encoding logic
(target-and-member resolution, Math module routing, resource expression
encoding) with visitor coordination, formatting, and resource structure
generation. Extracting expression methods into `ExpressionEncoder` continues the
incremental decomposition toward the full
[Encoder delegate decomposition](./encoder-delegate-decomposition.md).

This extraction follows the same delegate pattern established by
`StatementEncoder` on the encode side and
[`ExpressionDecoder`](./decoder-delegate-decomposition.md) on the decode side.

## Architecture

```text
TweedleEncoderDecoder (public facade — unchanged)
└── TweedleEncoder (coordinator, extends SourceCodeGenerator)
    ├── StatementEncoder (package-private, ~46 lines — step 1)
    │   └── Statement completion, disabled markers, statement-end formatting
    └── ExpressionEncoder (package-private, ~60 lines — step 2)
        └── Target+member resolution, Math routing, resource expressions
```

Both delegate classes live in `org.alice.serialization.tweedle`.
`ExpressionEncoder` is package-private with no public constructor. It is
instantiated only by `TweedleEncoder` and receives a back-reference to it for
shared services.

## Class responsibilities

### ExpressionEncoder

| Responsibility | Method |
| --- | --- |
| Target and member resolution | `appendTargetAndMember(Expression, String, AbstractType)` — resolves Math targets, renames members, appends access separator |
| Math target detection | `targetIsMath(Expression)` — returns `true` for `TypeExpression` wrapping `java.lang.Math` |
| Math module routing | `tweedleModuleForMath(String, AbstractType)` — maps to `$WholeNumber`, `$Angle`, or `$DecimalNumber` |
| Resource expression encoding | `processResourceExpression(ResourceExpression)` — encodes resource name as escaped string |

`ExpressionEncoder` stores a `TweedleEncoder` reference passed at construction
and uses it for all bridge and forwarding calls. This matches the
`StatementEncoder(TweedleEncoder)` field-storage pattern from step 1. Methods
do not take the encoder as an additional parameter.

The constructor signature:

```java
ExpressionEncoder(TweedleEncoder encoder) {
  this.encoder = encoder;
}
```

### TweedleEncoder changes

| Change | Detail |
| --- | --- |
| New field | `private final ExpressionEncoder expressionEncoder` |
| Constructor wiring | `this.expressionEncoder = new ExpressionEncoder(this)` alongside existing `statementEncoder` |
| `angleMembers` visibility | Changed from `private static final` to `static final` (package-private) so `ExpressionEncoder.tweedleModuleForMath` can read it |
| `membersToRename` visibility | Changed from `private static final` to `static final` (package-private) so `ExpressionEncoder.appendTargetAndMember` can read it |
| `appendTargetAndMember(Expression, String, AbstractType)` | Body delegates to `expressionEncoder.appendTargetAndMember(target, member, returnType)` |
| `processResourceExpression(ResourceExpression)` | Body delegates to `expressionEncoder.processResourceExpression(resourceExpression)` |
| Bridge methods added | `forwardProcessExpression(Expression)`, `forwardAppendAccessSeparator()`, `forwardAppendEscapedString(String)` — package-private forwarding methods (`forwardAppendAccessSeparator` and `forwardAppendEscapedString` bridge inherited `protected` methods; `forwardProcessExpression` wraps the `public` `processExpression` for uniformity) |

The `@Override` annotations remain on `TweedleEncoder` because the visitor
pattern in `SourceCodeGenerator` requires the override stubs on the subclass.
Each stub delegates to `ExpressionEncoder` for the extracted logic.

## Public API

The public API is exclusively `TweedleEncoderDecoder`. No API changes are made
by this extraction.

```java
public class TweedleEncoderDecoder implements EncoderDecoder<String> {
  public <N extends AbstractNode & ProcessableNode> String encode(N node);
  public <N extends AbstractNode & ProcessableNode> String encode(N node,
      Set<AbstractDeclaration> terminals);
  public <N extends ProcessableNode> String encodeProcessable(N node);
}
```

All encode entry points instantiate `TweedleEncoder`, which internally creates
both `StatementEncoder` and `ExpressionEncoder`. Callers never see the delegate
classes.

## Package-private collaboration

`ExpressionEncoder` accesses `TweedleEncoder` methods via package-private
forwarding methods. The following methods on `TweedleEncoder` are used by
`ExpressionEncoder`:

| Method | Purpose |
| --- | --- |
| `forwardAppendString(String)` | Append raw string to output buffer (already exists from step 1) |
| `forwardProcessExpression(Expression)` | Call `processExpression(target)` to encode the target expression (new — consistency bridge; `processExpression` is `public` but wrapped for uniformity) |
| `forwardAppendAccessSeparator()` | Call `appendAccessSeparator()` to write the `.` separator (new bridge) |
| `forwardAppendEscapedString(String)` | Call `appendEscapedString(value)` to write a quoted, escaped string (new bridge) |
| `membersToRename` | Package-private static map for member name translation (was `private`) |
| `angleMembers` | Package-private static set for Math angle function detection (was `private`) |

No interfaces or inheritance are introduced. All collaboration uses direct
method calls within the same package, matching the
[StatementEncoder pattern](./statement-encoder-extraction.md) and the
[Decoder delegate pattern](./decoder-delegate-decomposition.md).

## Bridge methods on TweedleEncoder

Because `TweedleEncoder` extends `SourceCodeGenerator` (in a different package),
`ExpressionEncoder` cannot call inherited `protected` methods directly — Java
accessibility rules prevent a same-package class from calling `protected`
methods inherited from a class in a different package. Two of the three
forwarding methods (`forwardAppendAccessSeparator`, `forwardAppendEscapedString`)
are required for this reason. The third (`forwardProcessExpression`) wraps
`processExpression`, which is actually `public` on `SourceCodeGenerator`, so it
is technically callable without a bridge. It is included for consistency with
the other forwarding methods, keeping all ExpressionEncoder→TweedleEncoder
callbacks uniform. The bridge pattern used:

```java
// TweedleEncoder.java — @Override stays here for polymorphic dispatch
@Override
protected void appendTargetAndMember(Expression target, String member,
    AbstractType<?, ?, ?> returnType) {
  expressionEncoder.appendTargetAndMember(target, member, returnType);
}

// Package-private bridges: delegate calls these to reach inherited methods
void forwardProcessExpression(Expression expression) {
  processExpression(expression);
}

void forwardAppendAccessSeparator() {
  appendAccessSeparator();
}

void forwardAppendEscapedString(String value) {
  appendEscapedString(value);
}
```

```java
// TweedleEncoder.java — @Override stays here
@Override
public void processResourceExpression(ResourceExpression resourceExpression) {
  expressionEncoder.processResourceExpression(resourceExpression);
}
```

Unlike the `StatementEncoder` bridges which need `super` call bridges
(`superAppendStatementCompletion`), `ExpressionEncoder` does not need `super`
bridges. The extracted methods (`appendTargetAndMember`,
`processResourceExpression`) do not call `super` — they implement the behavior
entirely. The forwarding methods provide access to inherited utility methods
(`processExpression`, `appendAccessSeparator`, `appendEscapedString`) rather
than `super` method invocations.

## Visibility changes

| Symbol | Before | After | Reason |
| --- | --- | --- | --- |
| `angleMembers` | `private static final Set<String>` | `static final Set<String>` (package-private) | Read by `ExpressionEncoder.tweedleModuleForMath` to detect angle Math functions |
| `membersToRename` | `private static final Map<String, String>` | `static final Map<String, String>` (package-private) | Read by `ExpressionEncoder.appendTargetAndMember` to translate member names |

Both collections are populated in a `static {}` initializer block and are
effectively unmodifiable after class initialization. Widening from `private`
to package-private has negligible security impact — they remain inaccessible
outside the package.

This follows the same pattern as `NODE_ENABLE` and `NODE_DISABLE` which were
widened to package-private in step 1 for `StatementEncoder`.

## Security boundary

No new I/O, network, reflection, or thread operations are introduced.
`ExpressionEncoder` only formats string output via the existing
`SourceCodeGenerator` buffer. The `membersToRename` and `angleMembers`
collections are constant data, not user-controlled input.

The `ExpressionEncoder` class is package-private and `final`-by-reference
(stored in a `private final` field on `TweedleEncoder`). All method calls are
compile-time verified — no reflection is used for delegation.

## Error handling contract

No error handling changes. The extracted methods do not throw checked exceptions
and contain no try/catch blocks. The `targetIsMath` method performs a safe
`instanceof` check and null-safe `getName()` comparison. The
`processResourceExpression` method reads the resource name from a non-null
`ResourceExpression.resource` property.

## Configuration

No runtime configuration changes. The extraction uses the existing Maven
reactor, Tweedle grammar submodule, and JUnit configuration.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the Node memory preference when running Maven:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Validation

Run the focused core AST encoder tests from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest,SourceCodeGeneratorTest,ExpressionEncoderExtractionTest,StatementEncoderExtractionTest \
  test
```

Run the story-api-migration round-trip tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

All suites must pass with identical results before and after the extraction.

## Acceptance criteria

| Criterion | Verification |
| --- | --- |
| `ExpressionEncoder.java` exists | File present in `core/ast/src/main/java/org/alice/serialization/tweedle/` |
| `ExpressionEncoder` is package-private | No `public` keyword on class declaration |
| Constructor takes `TweedleEncoder` | `ExpressionEncoder(TweedleEncoder encoder)` |
| `appendTargetAndMember` extracted | Method present on `ExpressionEncoder` with `(Expression, String, AbstractType)` signature |
| `targetIsMath` extracted | Private method on `ExpressionEncoder`, not on `TweedleEncoder` |
| `tweedleModuleForMath` extracted | Private method on `ExpressionEncoder`, not on `TweedleEncoder` |
| `processResourceExpression` extracted | Method present on `ExpressionEncoder` |
| `angleMembers` widened to package-private | No `private` modifier on the field in `TweedleEncoder` |
| `membersToRename` widened to package-private | No `private` modifier on the field in `TweedleEncoder` |
| 3 new bridge methods on TweedleEncoder | `forwardProcessExpression`, `forwardAppendAccessSeparator`, `forwardAppendEscapedString` |
| `TweedleEncoder` delegates `@Override` bodies | `appendTargetAndMember`, `processResourceExpression` delegate to `expressionEncoder` |
| `TweedleEncoderDecoder.java` unchanged | `git diff` shows no changes |
| `ExpressionEncoderExtractionTest` passes | All structural and behavioral tests — zero failures |
| `StatementEncoderExtractionTest` passes | Step 1 contract unbroken — zero failures |
| `TweedleEncoderTest` passes | Zero failures |
| `TweedleEncoderRenameContractTest` passes | Zero failures |
| `TweedleEncoderDecoderTest` passes | Zero failures |
| `SourceCodeGeneratorTest` passes | Zero failures |
| `core/story-api-migration` tests pass | Zero failures |
| Tweedle output is byte-identical | Encoder tests assert exact string output |

## Claim boundaries

This extraction proves:

- The `appendTargetAndMember`, `targetIsMath`, `tweedleModuleForMath`, and
  `processResourceExpression` methods can be extracted to a delegate without
  changing observable behavior.
- The Math module routing logic (`$WholeNumber`, `$Angle`, `$DecimalNumber`)
  works correctly through the delegate indirection.
- The member rename lookup via `membersToRename` produces identical output
  when accessed as a package-private static field by the delegate.
- The `angleMembers` set lookup produces identical results when accessed from
  `ExpressionEncoder` rather than `TweedleEncoder`.
- All existing encoder test assertions pass identically.
- The step 1 `StatementEncoder` extraction remains unaffected.

This extraction does **not** prove:

| Non-claim | Reason |
| --- | --- |
| Full encoder decomposition complete | Only `StatementEncoder` and `ExpressionEncoder` are extracted; `EncoderMappings` and `ResourceStructureEncoder` are future steps. |
| New encode capabilities | No new Tweedle constructs are supported. |
| Performance improvement | Extraction is structural, not algorithmic. |
| Thread safety | `TweedleEncoder` was not thread-safe before; extraction does not change this. |
| Public API expansion | No new public methods or classes are introduced. |
| Full ExpressionEncoder scope | The full [Encoder delegate decomposition](./encoder-delegate-decomposition.md) assigns more methods to `ExpressionEncoder` (instantiation dispatch, argument labeling, keyed arguments). This step extracts only the 4 methods at lines 790–826. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Full encoder delegate decomposition | [Encoder Delegate Decomposition](./encoder-delegate-decomposition.md) |
| Statement encoder extraction (step 1) | [StatementEncoder Extraction](./statement-encoder-extraction.md) |
| Decoder delegate decomposition | [Decoder Delegate Decomposition](./decoder-delegate-decomposition.md) |
| TweedleEncoder rename | [TweedleEncoder Rename](./tweedle-encoder-rename.md) |
