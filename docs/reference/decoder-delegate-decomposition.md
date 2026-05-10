# Decoder Delegate Decomposition

This reference describes the internal decomposition of the Tweedle AST
`Decoder` (1258 lines) into a thin coordinator plus three package-private
delegate classes: `ExpressionDecoder`, `StatementDecoder`, and `FieldDecoder`.

The decomposition is a pure internal refactor. The public API surface —
`TweedleEncoderDecoder` — is unchanged. All existing decode behavior, error
messages, and exception types are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
- [Decoder (coordinator)](#decoder-coordinator)
- [ExpressionDecoder](#expressiondecoder)
- [StatementDecoder](#statementdecoder)
- [FieldDecoder](#fielddecoder)
- [Public API](#public-api)
- [Package-private collaboration](#package-private-collaboration)
- [Security boundary](#security-boundary)
- [Error handling contract](#error-handling-contract)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

The original `Decoder.java` contained 1258 lines mixing three distinct
concerns: value expression decoding, statement/control-flow decoding, and field
declaration/initializer decoding. This made the class difficult to navigate,
review, and extend safely.

RabbitHole issue #480 decomposes the Decoder into focused delegates without
changing observable behavior. Each delegate is small enough to understand in
isolation and extend independently.

## Architecture

```text
TweedleEncoderDecoder (public facade — unchanged)
└── Decoder (package-private coordinator, ~280 lines)
    ├── ExpressionDecoder (package-private, ~340 lines)
    │   └── Value expressions, comparisons, arithmetic, logical ops, returns
    ├── StatementDecoder (package-private, ~440 lines)
    │   └── Method/constructor bodies, control flow, assignments, locals
    └── FieldDecoder (package-private, ~175 lines)
        └── Field declarations, initializers, arrays, null initializers
```

All four classes live in `org.alice.serialization.tweedle`. The delegates are
package-private with no public constructors. They are instantiated only by
`Decoder` and receive a back-reference to it for shared services.

## Class responsibilities

### Decoder (coordinator)

| Responsibility | Methods |
| --- | --- |
| Entry points | `decode(String)`, `copy(String)` |
| Class structure | `decodeClass(TweedleClass)` |
| Constructor dispatch | `decodeConstructor(...)` |
| Method signature | `decodeMethodSignature(TweedleMethod)` |
| Parameter decoding | `decodeRequiredParameters(...)`, `decodeAllParameters(...)` |
| Type resolution | `resolveType(String, String)`, `resolveType(TweedleType, String)`, `resolveReturnType(TweedleType)` |
| Scope finders | `findLocal(...)`, `findParameter(...)`, `findField(...)` |
| Zero-argument method index | `zeroArgumentMethodsByName(...)` |
| Type creation | `userTypeNamed(String)` |
| Diagnostic helpers | `describeMemberAccess(...)` (used by error factories in both ExpressionDecoder and StatementDecoder) |
| Delegate wiring | Creates `ExpressionDecoder`, `StatementDecoder`, `FieldDecoder` |

The coordinator owns type resolution and scope finders because they are shared
across all three delegates. The `resolveType` method's `Class.forName` call
uses a four-package allowlist (`JAVA_TYPE_PACKAGES`) that must stay centralized
for security review.

### ExpressionDecoder

| Responsibility | Methods |
| --- | --- |
| Value expressions | `decodeValueExpression(...)` |
| Primitive literals | `primitiveLiteral(Object)` |
| Relational expressions | `decodeRelationalExpression(...)`, `relationalOperator(...)`, `isComparisonExpression(...)` |
| Logical expressions | `decodeLogicalInfixExpression(...)`, `decodeLogicalNotExpression(...)` |
| Arithmetic expressions | `decodeBinaryNumericExpression(...)`, `arithmeticOperator(...)` |
| String concatenation | `decodeStringConcatenationExpression(...)` |
| Return expressions | `decodeMethodReturnExpression(...)`, `decodeMethodReturnFieldAccess(...)` |
| Assignment RHS | `decodeAssignmentRhs(...)` |

`ExpressionDecoder` calls back to `Decoder` for `resolveType`, `findLocal`,
`findParameter`, and `findField`. It has no mutable state beyond the `Decoder`
reference.

### StatementDecoder

| Responsibility | Methods |
| --- | --- |
| Method bodies | `decodeMethodBody(...)` |
| Constructor bodies | `decodeConstructorBody(...)` |
| If statements | `decodeIfStatement(...)`, `decodeSimpleIfBody(...)`, `decodeAssignmentOnlyConditionalBranchBody(...)` |
| While loops | `decodeWhileLoop(...)`, `decodeWhileLoopBody(...)` |
| Assignments | `decodeMethodAssignmentStatement(...)`, `decodeConstructorAssignmentStatement(...)` |
| Local declarations | `decodeLocalDeclarationStatement(...)` |
| Return statements | `decodeReturnStatement(...)` |
| Method calls | `decodeZeroArgumentSameClassMethodCallStatement(...)` |
| Description helpers | `describeMethodCall(...)` (calls `decoder.describeMemberAccess(...)`) |

`StatementDecoder` calls back to `Decoder` for type resolution and scope
finders, and to `ExpressionDecoder` for value expressions within statements.
It has no mutable state beyond the `Decoder` and `ExpressionDecoder`
references.

### FieldDecoder

| Responsibility | Methods |
| --- | --- |
| Field declarations | `decodeField(TweedleField)` |
| Field initializers | `decodeFieldInitializer(...)` |
| Null initializers | `decodeNullFieldInitializer(...)`, `isSupportedNullableField(...)` |
| Array initializers | `decodeArrayFieldInitializer(...)`, `decodeSizedArrayFieldInitializer(...)`, `decodeArrayInitializerElement(...)` |
| Arithmetic initializers | `decodeLiteralArithmeticFieldInitializer(...)`, `isLiteralOnlyArithmeticExpression(...)` |
| Resource detection | `isResourceType(...)`, `firstJavaTypeInHierarchy(...)` |

`FieldDecoder` calls back to `Decoder` for type resolution and to
`ExpressionDecoder` for `primitiveLiteral` and `decodeBinaryNumericExpression`.
It reads the `allowLiteralArithmeticFieldInitializers` flag from `Decoder`.

## Public API

The public API is exclusively `TweedleEncoderDecoder`. No API changes are made
by this decomposition.

```java
public class TweedleEncoderDecoder implements EncoderDecoder<String> {
  public AbstractNode decode(String document) throws VersionNotSupportedException;
  public AbstractNode decode(String document, Set<AbstractDeclaration> terminals)
      throws VersionNotSupportedException;
  public AbstractNode decode(String document, Set<AbstractDeclaration> terminals,
      boolean allowLiteralArithmeticFieldInitializers) throws VersionNotSupportedException;
  public AbstractNode copy(String document, Set<AbstractDeclaration> terminals)
      throws VersionNotSupportedException;
}
```

All four decode entry points instantiate `Decoder`, which internally creates
its three delegates. Callers never see the delegate classes.

## Package-private collaboration

The delegates access `Decoder` methods via package-private visibility. The
following `Decoder` methods are widened from `private` to package-private to
support delegate access:

| Method | Used by |
| --- | --- |
| `resolveType(String, String)` | All delegates |
| `resolveType(TweedleType, String)` | All delegates |
| `resolveReturnType(TweedleType)` | StatementDecoder |
| `findLocal(List<UserLocal>, String)` | ExpressionDecoder, StatementDecoder |
| `findParameter(UserParameter[], String)` | ExpressionDecoder |
| `findField(List<UserField>, String)` | ExpressionDecoder, StatementDecoder |
| `userTypeNamed(String)` | (remains coordinator-only) |
| `allowLiteralArithmeticFieldInitializers` field | FieldDecoder |
| `describeMemberAccess(FieldAccess)` | ExpressionDecoder, StatementDecoder |

No interfaces or inheritance are introduced. All collaboration uses direct
method calls within the same package.

## Security boundary

The `resolveType(String, String)` method uses `Class.forName` with a
four-package allowlist:

```java
private static final List<String> JAVA_TYPE_PACKAGES = List.of(
    "org.lgna.story.",
    "org.lgna.story.resources.",
    "org.lgna.common.resources.",
    "java.lang.");
```

This security-sensitive method stays in `Decoder` and is never duplicated in
delegates. Delegates call `decoder.resolveType(...)` through the package-private
back-reference.

The `TWEEDLE_TYPE_ALIASES` map (WholeNumber → Integer, DecimalNumber → Double,
TextString → String, Boolean → Boolean, Number → Number) also stays in
`Decoder`.

## Error handling contract

All error factories that produce `UnsupportedTweedleDecodeException` move with
their owning methods to the appropriate delegate class. Error messages are
preserved character-for-character.

| Error factory | Class |
| --- | --- |
| `unsupportedMethodBody` | StatementDecoder |
| `unsupportedSimpleIfBody` | StatementDecoder |
| `unsupportedZeroArgumentThisMethodCall` | StatementDecoder |
| `unsupportedArgumentBearingExplicitThisMethodCall` | StatementDecoder |
| `unsupportedConstructorBody` | StatementDecoder |
| `unsupportedMethodReturnExpression` | ExpressionDecoder |
| `unsupportedMethodReturnIdentifier` | ExpressionDecoder |
| `unsupportedMethodReturnMemberExpression` | ExpressionDecoder |
| `unsupportedFieldInitializer` | FieldDecoder |
| `unsupportedResourceFieldInitializer` | FieldDecoder |
| `unsupportedArrayInitializerElement` | FieldDecoder |
| `unsupportedArrayInitializerSize` | FieldDecoder |
| `unsupportedType` | Decoder (coordinator) |

The `ARGUMENT_BEARING_EXPLICIT_THIS_METHOD_CALLS` diagnostic label constant
moves to `StatementDecoder`.

Characterization tests written before the extraction enforce that all error
messages remain identical after the refactor.

## Configuration

There is no runtime configuration for the decoder decomposition. It uses the
existing Tweedle grammar, Maven reactor, and JUnit configuration.

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

Run the focused core AST decoder tests from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderDecoderTest \
  test
```

Run the story-api-migration round-trip test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Run the silver-thread Tweedle decoder round-trip test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.SilverThreadTweedleDecoderRoundTripTest \
  test
```

All three suites must pass with identical results before and after the
decomposition.

## Acceptance criteria

| Criterion | Verification |
| --- | --- |
| `Decoder.java` ≤ 500 lines | `wc -l Decoder.java` |
| Three new delegate classes created | `ExpressionDecoder.java`, `StatementDecoder.java`, `FieldDecoder.java` exist in `core/ast/src/main/java/org/alice/serialization/tweedle/` |
| All delegates are package-private | No `public` class keyword on delegates |
| No public API changes to `TweedleEncoderDecoder` | `TweedleEncoderDecoder.java` is unchanged |
| `mvn -pl core/ast -am test` passes | Zero test failures |
| `mvn -pl core/story-api-migration -am test` passes | Zero test failures |
| `SilverThreadTweedleDecoderRoundTripTest` passes | Zero test failures |
| Characterization tests written before extraction | Tests committed before extraction commits |
| Error messages preserved | Characterization tests assert exact exception messages |

## Claim boundaries

This decomposition proves:

- The 1258-line `Decoder` can be split into four focused classes without
  changing observable behavior.
- All existing `TweedleEncoderDecoderTest` assertions pass identically.
- All existing `SilverThreadTweedleDecoderRoundTripTest` assertions pass
  identically.
- All existing `core/story-api-migration` tests pass identically.
- Error messages and exception types are preserved.

This decomposition does **not** prove:

| Non-claim | Reason |
| --- | --- |
| New decode capabilities | No new Tweedle constructs are supported. |
| Performance improvement | Decomposition is structural, not algorithmic. |
| Thread safety | `Decoder` was not thread-safe before; delegates do not change this. |
| Public API expansion | No new public methods or classes are introduced. |
| Encoder changes | `Encoder.java` is not modified. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Unit-level Tweedle encode/decode | [Decode Coverage Characterization](./decode-coverage-characterization.md) |
| Simple if-statement decode | [Simple If-Statement Decode](./simple-if-statement-decode.md) |
| Zero-argument this-method call decode | [Zero-Argument This-Method Call Decode](./zero-argument-this-method-call-decode.md) |
| Tweedle round-trip silver thread | [Silver Thread Tweedle Decoder Round-Trip Test](./silver-thread-tweedle-decoder-round-trip-test.md) |
