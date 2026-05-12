# StatementEncoder Extraction

This reference describes the extraction of statement-encoding methods from the
959-line `TweedleEncoder` into a new package-private `StatementEncoder` class.
This is the first step of RabbitHole issue #506, isolating statement completion,
statement-end markers, disabled-node markers, and code-flow statement wiring
into a focused companion class.

The extraction is a pure internal refactor. The public API surface —
`TweedleEncoderDecoder` — is unchanged. All existing encode behavior, error
messages, and Tweedle output are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
  - [StatementEncoder](#statementencoder)
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

`TweedleEncoder.java` mixes statement-encoding logic (completion markers,
disabled-node wrapping, code-flow statement assembly) with expression encoding,
resource structure generation, and visitor coordination. Extracting statement
methods into `StatementEncoder` follows the same delegate pattern established by
the [Decoder delegate decomposition](./decoder-delegate-decomposition.md) on the
decode side, where `StatementDecoder` owns statement-level concerns.

This extraction is the first incremental step toward the full
[Encoder delegate decomposition](./encoder-delegate-decomposition.md). It can be
reviewed, tested, and merged independently.

## Architecture

```text
TweedleEncoderDecoder (public facade — unchanged)
└── TweedleEncoder (coordinator, extends SourceCodeGenerator)
    └── StatementEncoder (package-private, ~60 lines)
        └── Statement completion, disabled markers, statement-end formatting
```

Both classes live in `org.alice.serialization.tweedle`. `StatementEncoder` is
package-private with no public constructor. It is instantiated only by
`TweedleEncoder` and receives a back-reference to it for shared services.

## Class responsibilities

### StatementEncoder

| Responsibility | Method |
| --- | --- |
| Statement end marker | `appendStatementEnd(Statement)` — appends `NODE_ENABLE` marker for disabled statements, then newline |
| Disabled statement prefix | `pushStatementDisabled()` — appends `NODE_DISABLE` marker |
| Statement completion (with statement) | `appendStatementCompletion(Statement)` — calls `super` bridge then `appendStatementEnd` |
| Statement completion (no-arg) | `appendStatementCompletion()` — calls `super` bridge then newline |

`StatementEncoder` stores a `TweedleEncoder` reference passed at construction
and uses it for all bridge and forwarding calls. This matches the
`StatementDecoder(Decoder, ExpressionDecoder)` field-storage pattern on the
decode side. Methods do not take the encoder as an additional parameter.

The constructor signature:

```java
StatementEncoder(TweedleEncoder encoder) {
  this.encoder = encoder;
}
```

### TweedleEncoder changes

| Change | Detail |
| --- | --- |
| New field | `private final StatementEncoder statementEncoder` |
| Constructor wiring | `this.statementEncoder = new StatementEncoder(this)` |
| `NODE_ENABLE` visibility | Changed from `private` to package-private (no modifier) so `StatementEncoder` can read it |
| `appendStatementCompletion(Statement)` | Body delegates to `statementEncoder.appendStatementCompletion(stmt)` |
| `appendStatementCompletion()` | Body delegates to `statementEncoder.appendStatementCompletion()` |
| `pushStatementDisabled()` | Body delegates to `statementEncoder.pushStatementDisabled()`, then calls `super` |
| `appendCodeFlowStatement(Statement, Runnable)` | Calls `appendIndent(stmt)`, runs appender, then delegates `statementEncoder.appendStatementEnd(stmt)` |
| Bridge methods added | `superAppendStatementCompletion(Statement)`, `superAppendStatementCompletion()` — package-private methods that call `super.appendStatementCompletion(...)` on behalf of the delegate |
| Forwarding methods added | `forwardAppendString(String)`, `forwardAppendSpace()`, `forwardAppendNewLine()` — package-private methods that call the inherited `protected` methods, since `StatementEncoder` cannot call `protected` methods inherited from `SourceCodeGenerator` (different package) |

The `@Override` annotations remain on `TweedleEncoder` because the visitor
pattern in `SourceCodeGenerator` requires the override stubs on the subclass.
Each stub delegates to `StatementEncoder` for the extracted logic.

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
`StatementEncoder`. Callers never see the delegate class.

## Package-private collaboration

`StatementEncoder` accesses `TweedleEncoder` methods via package-private
forwarding methods. The following methods on `TweedleEncoder` are used by
`StatementEncoder`:

| Method | Purpose |
| --- | --- |
| `forwardAppendString(String)` | Append raw string to output buffer |
| `forwardAppendSpace()` | Append single space character |
| `forwardAppendNewLine()` | Append platform newline |
| `superAppendStatementCompletion(Statement)` | Call `SourceCodeGenerator.appendStatementCompletion(Statement)` |
| `superAppendStatementCompletion()` | Call `SourceCodeGenerator.appendStatementCompletion()` |
| `NODE_ENABLE` | Package-private static constant (was `private`) |

No interfaces or inheritance are introduced. All collaboration uses direct
method calls within the same package, matching the
[Decoder delegate pattern](./decoder-delegate-decomposition.md).

## Bridge methods on TweedleEncoder

Because `TweedleEncoder` extends `SourceCodeGenerator`, calls to `super` cannot
be forwarded to a delegate — Java requires `super.method()` to appear in the
subclass itself. The bridge pattern used:

```java
// TweedleEncoder.java — @Override stays here for polymorphic dispatch
@Override
protected void appendStatementCompletion(Statement stmt) {
  statementEncoder.appendStatementCompletion(stmt);
}

// Package-private bridge: delegate calls this to reach super
void superAppendStatementCompletion(Statement stmt) {
  super.appendStatementCompletion(stmt);
}
```

```java
@Override
protected void pushStatementDisabled() {
  statementEncoder.pushStatementDisabled();
  super.pushStatementDisabled();
}
```

```java
@Override
protected void appendCodeFlowStatement(Statement stmt, Runnable appender) {
  appendIndent(stmt);
  appender.run();
  statementEncoder.appendStatementEnd(stmt);
}
```

The `super` call ordering is preserved exactly: `pushStatementDisabled` writes
`NODE_DISABLE` before incrementing the disabled counter via `super`, and
`appendStatementCompletion` increments via `super` before writing the end
marker.

## Visibility changes

| Symbol | Before | After | Reason |
| --- | --- | --- | --- |
| `NODE_ENABLE` | `private static final` | `static final` (package-private) | Read by `StatementEncoder.appendStatementEnd` |
| `NODE_DISABLE` | `private static final` | `static final` (package-private) | Read by `StatementEncoder.pushStatementDisabled` via `forwardAppendString(NODE_DISABLE)` |

`NODE_ENABLE` and `NODE_DISABLE` are `String` constants (`">*"` and `"*<"`).
Widening from `private` to package-private has negligible security impact —
they remain inaccessible outside the package.

## Security boundary

No new I/O, network, reflection, or thread operations are introduced.
`StatementEncoder` only formats string output via the existing
`SourceCodeGenerator` buffer. The `NODE_ENABLE` / `NODE_DISABLE` markers are
constant strings, not user-controlled input.

## Error handling contract

No error handling changes. The extracted methods do not throw exceptions and
contain no try/catch blocks. The `isEnabled` property read in
`appendStatementEnd` is a non-null `BooleanProperty` on `Statement`.

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
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest,SourceCodeGeneratorTest \
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
| `StatementEncoder.java` exists | File present in `core/ast/src/main/java/org/alice/serialization/tweedle/` |
| `StatementEncoder` is package-private | No `public` keyword on class declaration |
| Constructor takes `TweedleEncoder` | `StatementEncoder(TweedleEncoder encoder)` |
| 3 `appendStatement*` methods extracted | `appendStatementCompletion(Statement)`, `appendStatementCompletion()`, `appendStatementEnd(Statement)` |
| `pushStatementDisabled` extracted | `pushStatementDisabled()` |
| `NODE_ENABLE` widened to package-private | No `private` modifier on `NODE_ENABLE` in `TweedleEncoder` |
| `TweedleEncoder` delegates `@Override` bodies | `appendStatementCompletion`, `pushStatementDisabled`, `appendCodeFlowStatement` delegate to `statementEncoder` |
| `TweedleEncoderDecoder.java` unchanged | `git diff` shows no changes |
| `TweedleEncoderTest` passes | `mvn -Dtest=TweedleEncoderTest test` — zero failures |
| `TweedleEncoderRenameContractTest` passes | Zero failures |
| `TweedleEncoderDecoderTest` passes | Zero failures |
| `SourceCodeGeneratorTest` passes | Zero failures |
| `core/story-api-migration` tests pass | Zero failures |
| Tweedle output is byte-identical | Encoder tests assert exact string output |

## Claim boundaries

This extraction proves:

- The `appendStatementCompletion`, `appendStatementEnd`, and
  `pushStatementDisabled` methods can be extracted to a delegate without
  changing observable behavior.
- The `super` call ordering in `pushStatementDisabled` and
  `appendStatementCompletion` is preserved correctly through the bridge pattern.
- The `appendCodeFlowStatement` split (indent stays on coordinator,
  statement-end delegates) produces identical output.
- All existing encoder test assertions pass identically.

This extraction does **not** prove:

| Non-claim | Reason |
| --- | --- |
| Full encoder decomposition complete | Only `StatementEncoder` is extracted; `ExpressionEncoder`, `EncoderMappings`, and `ResourceStructureEncoder` are future steps. |
| New encode capabilities | No new Tweedle constructs are supported. |
| Performance improvement | Extraction is structural, not algorithmic. |
| Thread safety | `TweedleEncoder` was not thread-safe before; extraction does not change this. |
| Public API expansion | No new public methods or classes are introduced. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Full encoder delegate decomposition | [Encoder Delegate Decomposition](./encoder-delegate-decomposition.md) |
| Decoder delegate decomposition | [Decoder Delegate Decomposition](./decoder-delegate-decomposition.md) |
| TweedleEncoder rename | [TweedleEncoder Rename](./tweedle-encoder-rename.md) |
