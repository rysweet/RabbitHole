# FormattingEncoder Extraction

This reference describes the extraction of formatting and utility helper methods
from the 623-line `TweedleEncoder` into a new package-private
`FormattingEncoder` class. This is the third step of RabbitHole issue #506,
isolating indent management, argument formatting, list rendering, string
quoting, instantiation helpers, and visibility tag writing into a focused
companion class.

The extraction is a pure internal refactor. The public API surface —
`TweedleEncoderDecoder` — is unchanged. All existing encode behavior, error
messages, and Tweedle output are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
  - [FormattingEncoder](#formattingencoder)
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
(step 1) and the [ExpressionEncoder extraction](./expression-encoder-extraction.md)
(step 2), `TweedleEncoder` still carries formatting and utility helpers —
indent management (`pushIndent`, `popIndent`, `indentString`, `appendIndent`),
argument formatting (`appendArg`, `appendAnotherArg`, `appendInstantiation`),
list rendering (`appendList`), string quoting (`quoteString`), and visibility
tagging (`appendVisibilityTag`). These methods are pure formatting concerns
with no visitor-pattern or encoding logic.

Extracting them into `FormattingEncoder` is the final extraction step that,
combined with the [StatementEncoder](./statement-encoder-extraction.md) (step 1)
and [ExpressionEncoder](./expression-encoder-extraction.md) (step 2) extractions
plus blank-line trimming, brings `TweedleEncoder` under 500 lines while keeping
each companion class focused on a single responsibility.

## Architecture

The architecture diagram below shows the **target state** after all three
extraction steps. If step 3 is applied before step 2, the ExpressionEncoder
node is absent until step 2 completes.

```text
TweedleEncoderDecoder (public facade — unchanged)
└── TweedleEncoder (coordinator, extends SourceCodeGenerator, ≤499 lines)
    ├── StatementEncoder (package-private, ~46 lines — step 1)
    │   └── Statement completion, disabled markers, statement-end formatting
    ├── ExpressionEncoder (package-private, ~60 lines — step 2)
    │   └── Target+member resolution, Math routing, resource expressions
    └── FormattingEncoder (package-private, ~122 lines — step 3)
        └── Indent state, argument formatting, list rendering, quoting,
            visibility tags, instantiation helpers
```

All delegate classes live in `org.alice.serialization.tweedle`.
`FormattingEncoder` is package-private with no public constructor. It is
instantiated only by `TweedleEncoder` and receives a back-reference to it for
shared formatting services.

## Class responsibilities

### FormattingEncoder

| Responsibility | Method |
| --- | --- |
| Indent state | `pushIndent()`, `popIndent()` — increment/decrement the indent level |
| Indent string lookup | `indentString(int)` — returns the cached or computed indent string for a level |
| Indent append (no-arg) | `appendIndent()` — appends the current indent level to the output buffer |
| Indent append (statement) | `appendIndent(Statement)` — adjusts indent for disabled statements, then appends |
| Visibility annotation tag | `appendVisibilityTag(FieldTemplate)` — writes `@CompletelyHidden`, `@PrimeTime`, or `@TuckedAway` based on annotation visibility |
| Instantiation helper | `appendInstantiation(String, Runnable)` — writes `new ClassName(args)` |
| Labeled argument | `appendArg(String, String)`, `appendArg(String, Runnable)` — writes `label: value` |
| Separator + argument | `appendAnotherArg(String, String)`, `appendAnotherArg(String, Runnable)` — writes `, label: value` |
| List rendering | `appendList(T[], Consumer<T>, String)` — writes `{item, item, ...}` |
| String quoting | `quoteString(String)` — writes `"value"` |

`FormattingEncoder` owns the indent state: the `indent` field and the
`INDENTION`, `MAX_CACHED_INDENT`, and `INDENT_CACHE` constants. These move
entirely from `TweedleEncoder` to `FormattingEncoder`.

The constructor signature:

```java
FormattingEncoder(TweedleEncoder encoder) {
  this.encoder = encoder;
}
```

### TweedleEncoder changes

| Change | Detail |
| --- | --- |
| New field | `private final FormattingEncoder formattingEncoder = new FormattingEncoder(this)` (inline initialization, matching `statementEncoder` and `resourceEncoder`) |
| Indent state removed | `indent` field, `INDENTION`, `MAX_CACHED_INDENT`, `INDENT_CACHE` constants, `pushIndent()`, `popIndent()`, `indentString(int)` all move to `FormattingEncoder` |
| `appendIndent()` | Body delegates to `formattingEncoder.appendIndent()` (bridge retained — called by `ResourceEncoder` via `encoder.appendIndent()`) |
| `appendIndent(Statement)` | **Removed entirely.** Call sites at `processSingleStatement` and `appendCodeFlowStatement` change to `formattingEncoder.appendIndent(stmt)` directly. No bridge needed — the method was `private`, called only within `TweedleEncoder`. |
| `openBlock()` | Calls `formattingEncoder.pushIndent()` instead of local `pushIndent()` |
| `closeBlockInline()` | Calls `formattingEncoder.popIndent()` and `formattingEncoder.appendIndent()` instead of local calls |
| `appendVisibilityTag(FieldTemplate)` | Body delegates to `formattingEncoder.appendVisibilityTag(fieldAnnotation)` |
| `appendInstantiation(String, Runnable)` | Body delegates to `formattingEncoder.appendInstantiation(className, args)` |
| `appendArg` (both overloads) | Body delegates to `formattingEncoder.appendArg(...)` |
| `appendAnotherArg` (both overloads) | Body delegates to `formattingEncoder.appendAnotherArg(...)` |
| `appendList(T[], Consumer<T>, String)` | Body delegates to `formattingEncoder.appendList(values, appendValue, separator)` |
| `quoteString(String)` | Body delegates to `formattingEncoder.quoteString(aString)` |
| New bridge methods | `forwardAppendChar(char)`, `forwardParenthesize(Runnable)`, `forwardGetListSeparator()` — package-private forwarding methods for inherited `protected` methods |

The `@Override` annotations on `openBlock()` and `closeBlockInline()` remain on
`TweedleEncoder` because `SourceCodeGenerator` requires the override stubs on
the subclass. Each stub calls `FormattingEncoder` for indent manipulation.

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
`StatementEncoder`, `ExpressionEncoder`, `ResourceEncoder`, and
`FormattingEncoder`. Callers never see the delegate classes.

## Package-private collaboration

`FormattingEncoder` accesses `TweedleEncoder` methods via package-private
forwarding methods. The following methods on `TweedleEncoder` are used by
`FormattingEncoder`:

| Method | Purpose |
| --- | --- |
| `forwardAppendString(String)` | Append raw string to output buffer (already exists from step 1) |
| `forwardAppendChar(char)` | Append single character to output buffer (new bridge) |
| `forwardParenthesize(Runnable)` | Call `parenthesize(appender)` to wrap content in parentheses (new bridge) |
| `forwardGetListSeparator()` | Call `getListSeparator()` to retrieve `, ` separator (new bridge) |

`ResourceEncoder` also calls formatting methods on `TweedleEncoder` that now
delegate through to `FormattingEncoder`. For example, `ResourceEncoder` calls
`encoder.appendVisibilityTag(annotation)`, `encoder.appendArg(label, value)`,
`encoder.appendInstantiation(name, args)`, etc. — these bridge methods on
`TweedleEncoder` forward to `FormattingEncoder`. `ResourceEncoder` continues
to call `TweedleEncoder` methods, not `FormattingEncoder` directly. This
maintains the established one-hop delegation pattern.

`ResourceEncoder` also calls `encoder.getListSeparator()` directly (e.g., in
`appendResourceFields` and `appendAddedJoints`). `getListSeparator()` is an
`@Override` method on `TweedleEncoder` and is **not** extracted to
`FormattingEncoder`. The `forwardGetListSeparator()` bridge exists solely for
`FormattingEncoder`'s internal use in `appendAnotherArg`.

No interfaces or inheritance are introduced. All collaboration uses direct
method calls within the same package, matching the
[StatementEncoder pattern](./statement-encoder-extraction.md),
[ExpressionEncoder pattern](./expression-encoder-extraction.md), and the
[Decoder delegate pattern](./decoder-delegate-decomposition.md).

## Bridge methods on TweedleEncoder

`FormattingEncoder` needs access to three inherited methods on
`SourceCodeGenerator` that are not accessible from a same-package non-subclass:

```java
// TweedleEncoder.java — new package-private forwarding methods
void forwardAppendChar(char c) {
  appendChar(c);
}

void forwardParenthesize(Runnable appender) {
  parenthesize(appender);
}

String forwardGetListSeparator() {
  return getListSeparator();
}
```

Unlike the `StatementEncoder` bridges which call `super.method()` for parent
implementation dispatch, the `FormattingEncoder` forwarding methods simply call
the inherited method — there is no `super` vs `this` distinction because the
extracted methods fully implement their behavior.

The existing bridge methods (`forwardAppendString`, `forwardAppendSpace`,
`forwardAppendNewLine`, `forwardAppendEscapedString`, `forwardBracketize`,
`forwardGetCodeStringBuilder`) remain available for `StatementEncoder`,
`ExpressionEncoder`, and `ResourceEncoder`. `FormattingEncoder` reuses
`forwardAppendString` from step 1.

## Visibility changes

| Symbol | Before | After | Reason |
| --- | --- | --- | --- |
| `INDENTION` | `private static final` | moved to `FormattingEncoder` | Owned by the indent implementation |
| `MAX_CACHED_INDENT` | `private static final` | moved to `FormattingEncoder` | Owned by the indent implementation |
| `INDENT_CACHE` | `private static final` | moved to `FormattingEncoder` | Owned by the indent implementation |
| `indent` | `private int` | moved to `FormattingEncoder` | Owned by the indent implementation |
| `pushIndent()` | `private` on `TweedleEncoder` | package-private on `FormattingEncoder` | Called by `TweedleEncoder.openBlock()` through the delegate |
| `popIndent()` | `private` on `TweedleEncoder` | package-private on `FormattingEncoder` | Called by `TweedleEncoder.closeBlockInline()` through the delegate |
| `indentString(int)` | `private static` on `TweedleEncoder` | `private static` on `FormattingEncoder` | Internal helper, not accessed from outside |
| `appendIndent()` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |
| `appendIndent(Statement)` | `private` on `TweedleEncoder` | package-private on `FormattingEncoder` | Called by `TweedleEncoder` at 2 call sites |
| `appendVisibilityTag(FieldTemplate)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |
| `appendInstantiation(String, Runnable)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` and `TweedleEncoder.processInstantiation` through `TweedleEncoder` |
| `appendArg(String, String)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |
| `appendArg(String, Runnable)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` and `TweedleEncoder.processInstantiation` through `TweedleEncoder` |
| `appendAnotherArg(String, String)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |
| `appendAnotherArg(String, Runnable)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |
| `appendList(T[], Consumer<T>, String)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |
| `quoteString(String)` | package-private on `TweedleEncoder` | package-private on `FormattingEncoder` (TweedleEncoder retains a delegate bridge) | Called by `ResourceEncoder` through `TweedleEncoder` |

The indent constants and field move entirely to `FormattingEncoder`. No
`TweedleEncoder` stubs remain for `pushIndent`/`popIndent`/`indentString` —
they are called via the delegate reference directly.

## Security boundary

No new I/O, network, reflection, or thread operations are introduced.
`FormattingEncoder` only formats string output via the existing
`SourceCodeGenerator` buffer. The indent cache is constant data populated
at class initialization, not user-controlled input.

The `FormattingEncoder` class is package-private and stored in a `private
final` field on `TweedleEncoder`. All method calls are compile-time
verified — no reflection is used for delegation.

The `StringBuilder` is not exposed across class boundaries by
`FormattingEncoder`. All output goes through the `forwardAppendString`,
`forwardAppendChar`, and `forwardParenthesize` forwarding methods, which
call inherited methods on `TweedleEncoder`/`SourceCodeGenerator`.

## Error handling contract

No error handling changes. The extracted methods do not throw checked exceptions
and contain no try/catch blocks. The `appendVisibilityTag` method handles
`null` input with an early return. The `appendIndent(Statement)` method reads
the `isEnabled` property, which is a non-null `BooleanProperty` on `Statement`.
The `indentString` method handles non-positive levels with a safe empty-string
return.

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
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest,SourceCodeGeneratorTest,FormattingEncoderExtractionTest,StatementEncoderExtractionTest,ExpressionEncoderExtractionTest \
  test
```

Run the story-api-migration round-trip tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Run the core/ide encoder tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='*Encoder*Test,*SourceCode*Test' \
  test
```

All suites must pass with identical results before and after the extraction.

## Acceptance criteria

| Criterion | Verification |
| --- | --- |
| `FormattingEncoder.java` exists | File present in `core/ast/src/main/java/org/alice/serialization/tweedle/` |
| `FormattingEncoder` is package-private | No `public` keyword on class declaration |
| Constructor takes `TweedleEncoder` | `FormattingEncoder(TweedleEncoder encoder)` |
| Indent state moved | `indent` field, `INDENTION`, `MAX_CACHED_INDENT`, `INDENT_CACHE` on `FormattingEncoder`, not `TweedleEncoder` |
| `pushIndent` extracted | Method on `FormattingEncoder`, no stub on `TweedleEncoder` |
| `popIndent` extracted | Method on `FormattingEncoder`, no stub on `TweedleEncoder` |
| `indentString` extracted | `private static` on `FormattingEncoder`, not on `TweedleEncoder` |
| `appendIndent()` extracted | Method on `FormattingEncoder`, bridge on `TweedleEncoder` |
| `appendIndent(Statement)` extracted | Method on `FormattingEncoder`, private method removed from `TweedleEncoder`, call sites at `processSingleStatement` and `appendCodeFlowStatement` use `formattingEncoder.appendIndent(stmt)` directly |
| `appendVisibilityTag` extracted | Method on `FormattingEncoder`, bridge on `TweedleEncoder` |
| `appendInstantiation` extracted | Method on `FormattingEncoder`, bridge on `TweedleEncoder` |
| `appendArg` (both overloads) extracted | Methods on `FormattingEncoder`, bridges on `TweedleEncoder` |
| `appendAnotherArg` (both overloads) extracted | Methods on `FormattingEncoder`, bridges on `TweedleEncoder` |
| `appendList` extracted | Method on `FormattingEncoder`, bridge on `TweedleEncoder` |
| `quoteString` extracted | Method on `FormattingEncoder`, bridge on `TweedleEncoder` |
| 3 new bridge methods on TweedleEncoder | `forwardAppendChar`, `forwardParenthesize`, `forwardGetListSeparator` |
| `TweedleEncoder` ≤499 lines | `wc -l` reports at most 499 (requires all three extraction steps + blank-line trimming) |
| `TweedleEncoderDecoder.java` unchanged | `git diff` shows no changes |
| `StatementEncoder.java` unchanged | Step 1 extraction unaffected |
| `ExpressionEncoder.java` unchanged | Step 2 extraction unaffected |
| `FormattingEncoderExtractionTest` passes | All structural and behavioral tests — zero failures |
| `StatementEncoderExtractionTest` passes | Step 1 contract unbroken — zero failures |
| `ExpressionEncoderExtractionTest` passes | Step 2 contract unbroken — zero failures |
| `TweedleEncoderTest` passes | Zero failures |
| `TweedleEncoderRenameContractTest` passes | Zero failures |
| `TweedleEncoderDecoderTest` passes | Zero failures |
| `SourceCodeGeneratorTest` passes | Zero failures |
| `core/story-api-migration` tests pass | Zero failures |
| `core/ide` encoder tests pass | Zero failures |
| Tweedle output is byte-identical | Encoder tests assert exact string output |

## Claim boundaries

This extraction proves:

- The 13 formatting and utility helper methods (`pushIndent`, `popIndent`,
  `indentString`, `appendIndent` ×2, `appendVisibilityTag`,
  `appendInstantiation`, `appendArg` ×2, `appendAnotherArg` ×2, `appendList`,
  `quoteString`) can be extracted to a delegate without changing observable
  behavior.
- The indent state (`indent` field + constants + cache) can be owned entirely
  by `FormattingEncoder` while `TweedleEncoder` accesses it through the
  delegate.
- `openBlock` and `closeBlockInline` work correctly when calling
  `formattingEncoder.pushIndent()` and `formattingEncoder.popIndent()`
  respectively.
- `appendIndent(Statement)` produces identical output for disabled and enabled
  statements when called through the delegate.
- `ResourceEncoder` calls to `appendVisibilityTag`, `appendArg`,
  `appendInstantiation`, etc. continue to work through the `TweedleEncoder`
  bridge methods.
- All existing encoder test assertions pass identically.
- The step 1 `StatementEncoder` and step 2 `ExpressionEncoder` extractions
  remain unaffected.
- After all three extraction steps (StatementEncoder, ExpressionEncoder,
  FormattingEncoder) plus blank-line trimming, `TweedleEncoder` is reduced to
  at most 499 lines, meeting the issue #506 target of under 500 lines.

This extraction does **not** prove:

| Non-claim | Reason |
| --- | --- |
| Full encoder decomposition complete | `EncoderMappings` and `ResourceStructureEncoder` remain future steps per the [Encoder Delegate Decomposition](./encoder-delegate-decomposition.md). |
| New encode capabilities | No new Tweedle constructs are supported. |
| Performance improvement | Extraction is structural, not algorithmic. Indent cache behavior is identical. |
| Thread safety | `TweedleEncoder` was not thread-safe before; extraction does not change this. |
| Public API expansion | No new public methods or classes are introduced. |
| ResourceEncoder changes | `ResourceEncoder` is unchanged; it calls through `TweedleEncoder` bridges as before. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Full encoder delegate decomposition | [Encoder Delegate Decomposition](./encoder-delegate-decomposition.md) |
| Statement encoder extraction (step 1) | [StatementEncoder Extraction](./statement-encoder-extraction.md) |
| Expression encoder extraction (step 2) | [ExpressionEncoder Extraction](./expression-encoder-extraction.md) |
| Decoder delegate decomposition | [Decoder Delegate Decomposition](./decoder-delegate-decomposition.md) |
| TweedleEncoder rename | [TweedleEncoder Rename](./tweedle-encoder-rename.md) |
