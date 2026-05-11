# Encoder Delegate Decomposition

This reference describes the internal decomposition of the 959-line
`TweedleEncoder` into a thin coordinator plus four package-private delegate
classes: `StatementEncoder`, `ExpressionEncoder`, `EncoderMappings`, and
`ResourceStructureEncoder`.

The decomposition is a pure internal refactor. The public API surface —
`TweedleEncoderDecoder` — is unchanged. All existing encode behavior, error
messages, and Tweedle output are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
  - [TweedleEncoder (coordinator)](#tweedleencoder-coordinator)
  - [StatementEncoder](#statementencoder)
  - [ExpressionEncoder](#expressionencoder)
  - [EncoderMappings](#encodermappings)
  - [ResourceStructureEncoder](#resourcestructureencoder)
- [Public API](#public-api)
- [Package-private collaboration](#package-private-collaboration)
- [Bridge methods](#bridge-methods)
- [Security boundary](#security-boundary)
- [Error handling contract](#error-handling-contract)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

The original `TweedleEncoder.java` contained 959 lines mixing five distinct
concerns: static rename/label/wrapping maps (184 lines of `static {}` block),
statement encoding, expression encoding, resource class generation via
reflection, and visitor-pattern coordination. This made the class difficult to
navigate, review, and extend safely.

RabbitHole issue #483 decomposes the encoder into focused delegates, mirroring
the [Decoder delegate decomposition](./decoder-delegate-decomposition.md)
pattern (issue #480). Each delegate is small enough to understand in isolation
and extend independently.

## Architecture

```text
TweedleEncoderDecoder (public facade — unchanged)
└── TweedleEncoder (coordinator, ≤ 500 lines)
    ├── EncoderMappings (package-private, ~170 lines)
    │   └── Static rename maps, parameter labels, wrapped args, system identifiers
    ├── StatementEncoder (package-private, ~70 lines)
    │   └── Local declarations, super constructor, statement completion, disabled nodes
    ├── ExpressionEncoder (package-private, ~200 lines)
    │   └── Instantiation dispatch, argument labeling/wrapping, keyed arguments,
    │       target+member resolution, math module routing
    └── ResourceStructureEncoder (package-private, ~170 lines)
        └── Resource type/dynamic resource classes, fields via reflection,
            constructors, instances, joint IDs, poses, transformations
```

All five classes live in `org.alice.serialization.tweedle`. The delegates are
package-private with no public constructors. They are instantiated only by
`TweedleEncoder` and receive a back-reference to it for shared services.

## Class responsibilities

### TweedleEncoder (coordinator)

| Responsibility | Methods |
| --- | --- |
| Entry point | `encode(ProcessableNode)` |
| Class structure | `appendClassHeader(NamedUserType)`, `appendClassFooter(String)` |
| Constructor encoding | `processConstructor(NamedUserConstructor)` |
| Method encoding | `processMethod(UserMethod)`, `appendMethodHeader(AbstractMethod)` |
| Statement completion | `appendStatementCompletion(Statement)`, `appendStatementCompletion()` |
| Code flow | `processCountLoop(CountLoop)`, `processDoInOrder(DoInOrder)`, `processDoTogether(DoTogether)`, `processEachInTogether(AbstractEachInTogether)` |
| Formatting primitives | `openBlock()`, `closeBlock()`, `closeBlockInline()`, `appendAssignmentOperator()`, `appendConcatenationOperator()`, `appendForEachToken()`, `appendInEachToken()` |
| Shared instantiation helpers | `appendInstantiation(String, Runnable)`, `appendArg(String, String)`, `appendArg(String, Runnable)`, `appendAnotherArg(String, String)`, `appendAnotherArg(String, Runnable)` |
| User joint identifiers | `getUserJointIdentifier(String)` |
| Indentation | `pushIndent()`, `popIndent()`, `appendIndent()`, `appendIndent(Statement)` |
| Type names | `processTypeName(AbstractType)`, `tweedleTypeName(String)` |
| Identifiers | `identifierName(AbstractDeclaration)` |
| Comments | `appendSingleLineComment(String)`, `getLocalizedComment(...)` |
| List and syntax | `getListSeparator()`, `appendSingleCodeLine(Runnable)`, `processSingleStatement(Statement, Runnable)`, `appendCodeFlowStatement(Statement, Runnable)` |
| Delegate wiring | Creates `StatementEncoder`, `ExpressionEncoder`, `ResourceStructureEncoder` |

The coordinator owns visitor-pattern `@Override` methods because
`SourceCodeGenerator` requires they reside on the subclass. Method bodies
delegate to the appropriate encoder.

### StatementEncoder

| Responsibility | Methods |
| --- | --- |
| Local declaration | `processLocalDeclaration(LocalDeclarationStatement, TweedleEncoder)` |
| Super constructor | `processSuperConstructor(SuperConstructorInvocationStatement, TweedleEncoder)` |
| Statement disabled markers | `pushStatementDisabled(TweedleEncoder)` |
| Statement end | `appendStatementEnd(Statement, TweedleEncoder)` |

`StatementEncoder` calls back to `TweedleEncoder` for `processSingleStatement`,
`processExpression`, `processTypeName`, `processVariableIdentifier`,
`appendStatement`, `appendSpace`, `appendString`, `appendNewLine`,
`appendEachArgument`, `processSuperReference`, and `parenthesize`. It has no
mutable state beyond the `TweedleEncoder` reference.

### ExpressionEncoder

| Responsibility | Methods |
| --- | --- |
| Instantiation dispatch | `processInstantiation(InstanceCreation, TweedleEncoder)` |
| Person resource | Evaluates `PersonResource` creation via `ReleaseVirtualMachine` |
| Class name extraction | `getDeclaringJavaClassName(InstanceCreation)` — resolves Java constructor class name |
| Double boxing | `$DecimalNumber.from(wholeNumber: ...)` wrapping |
| Dynamic resource | `DynamicXxxResource` → `XxxResource.DEFAULT` shorthand |
| Keyed arguments | `processKeyedArgument(JavaKeyedArgument, TweedleEncoder)` |
| Labeled arguments | `processArgument(AbstractParameter, AbstractArgument, TweedleEncoder)` |
| Wrapped arguments | `appendWrappedArg(ProcessableNode, String, Map)` |
| Parameter labels | `getParameterLabel(AbstractParameter)` — constructor relabeling, missing names, type fallback |
| Single argument dispatch | `appendOneArgument(MethodInvocation)` — extracts single required argument with wrapping |
| Parameter index | `parameterIndex(JavaMethodParameter)` — ordinal position of parameter in its method |
| Target and member | `appendTargetAndMember(Expression, String, AbstractType, TweedleEncoder)` |
| Math module routing | `tweedleModuleForMath(String, AbstractType)` — `$WholeNumber`, `$Angle`, `$DecimalNumber` |
| Resource expressions | `processResourceExpression(ResourceExpression, TweedleEncoder)` |

`ExpressionEncoder` reads static maps from `EncoderMappings` for rename
lookups, parameter label resolution, constructor relabeling, and argument
wrapping. It calls back to `TweedleEncoder` for `processExpression`,
`appendString`, `appendSpace`, `parenthesize`, and `appendEscapedString`.

### EncoderMappings

| Responsibility | Fields |
| --- | --- |
| Type renames | `typesToRename` — `Double` → `DecimalNumber`, `String` → `TextString`, etc. |
| Added code blocks | `typesWithAddedCode` — `Person` facial joint tracking overrides |
| Member renames | `membersToRename` — `rint` → `round`, `ceil` → `ceiling`, etc. |
| Missing parameter names | `methodsMissingParameterNames` — `say(text)`, `pow(b, power)`, etc. |
| Wrapped arguments | `methodsWithWrappedArgs` — duration, angle, portion, joint name wrapping |
| Optional param wraps | `optionalParamsToWrap` — `duration` → `new Duration(seconds: ...)` |
| Parameter relabeling | `methodParamsToRelabel` — `multipleEventPolicy` → `overlappingEventPolicy` |
| Constructor relabeling | `constructorsWithRelabeledParams` — `Size(width, height, depth)`, `Position(x, y, z)` |
| Angle members | `angleMembers` — `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `PI` |
| System identifiers | `systemIdentifiers` — `args`, `index`, `value`, `event`, `myScene`, etc. |
| Code organizer defs | `codeOrganizerDefinitionMap` — `Scene`, `Program` organizers |

All maps are populated in a `static {}` initializer and are effectively
unmodifiable after class initialization. `EncoderMappings` has no instance
methods — all fields are package-private static.

### ResourceStructureEncoder

| Responsibility | Methods |
| --- | --- |
| Resource type classes | `processResourceType(String, TweedleEncoder)` |
| Dynamic resource classes | `processDynamicResource(String, String, InstantiableTweedleNode[], TweedleEncoder)` |
| Resource constructors | `appendResourceConstructor(String, String, TweedleEncoder)` — superclass-specific constructor params |
| Resource fields | `appendResourceFields(String, Class, TweedleEncoder)` — reflection over `JointId`, array fields |
| Resource instances | `appendResourceInstances(Class, TweedleEncoder)`, `appendResourceInstance(String, String, TweedleEncoder)` |
| Added joints | `appendAddedJoints(String, Collection, TweedleEncoder)` — `ADDED_JOINTS`, `ALL_JOINTS`, `getJointIds()` |
| Static fields | `appendStaticField(Field, Runnable, TweedleEncoder)`, `appendStaticField(FieldTemplate, String, String, Runnable, TweedleEncoder)` |
| Joint IDs | `appendNewJointId(String, String, TweedleEncoder)`, `appendNewJointArrayId(String, String, TweedleEncoder)` |
| Field references | `getFieldReference(String, String, TweedleEncoder)` |
| Poses | `appendNewPose(InstantiableTweedleNode[], TweedleEncoder)` |
| Transformations | `appendNewJointTransformation(String, AffineMatrix4x4, TweedleEncoder)` |
| Visibility tags | `appendVisibilityTag(FieldTemplate, TweedleEncoder)` |
| List helper | `appendList(T[], Consumer, String, TweedleEncoder)` |
| Quote helper | `quoteString(String, TweedleEncoder)` |

`ResourceStructureEncoder` uses `Class.forName` for resource class reflection,
the only reflective code in the encoder. All reflection is consolidated in this
class for security review.

`ResourceStructureEncoder` calls shared instantiation helpers
(`appendInstantiation`, `appendArg`, `appendAnotherArg`) via the
`TweedleEncoder` coordinator reference. These helpers are shared because
`ExpressionEncoder` also uses them for `PersonResource` and `Double` boxing.

## Public API

The public API is exclusively `TweedleEncoderDecoder`. No API changes are made
by this decomposition.

```java
public class TweedleEncoderDecoder implements EncoderDecoder<String> {
  // Decode methods (unchanged, delegated to Decoder)
  public AbstractNode decode(String document) throws VersionNotSupportedException;
  public AbstractNode decode(String document, Set<AbstractDeclaration> terminals)
      throws VersionNotSupportedException;
  // ...

  // Encode methods (unchanged, delegated to TweedleEncoder)
  public String encode(ProcessableNode node);
  public String encode(ProcessableNode node, Set<AbstractDeclaration> terminals);
}
```

All encode entry points instantiate `TweedleEncoder`, which internally creates
its four delegates. Callers never see the delegate classes.

## Package-private collaboration

The delegates access `TweedleEncoder` methods via package-private visibility.
The following `TweedleEncoder` methods are package-private (widened from
`private` or inherited `protected` as needed) to support delegate access:

| Method | Used by |
| --- | --- |
| `appendString(String)` | All delegates |
| `appendSpace()` | StatementEncoder, ExpressionEncoder |
| `appendNewLine()` | StatementEncoder, ResourceStructureEncoder |
| `appendChar(char)` | ResourceStructureEncoder |
| `appendEscapedString(String)` | ExpressionEncoder, ResourceStructureEncoder |
| `processExpression(Expression)` | StatementEncoder, ExpressionEncoder |
| `processTypeName(AbstractType)` | StatementEncoder |
| `processVariableIdentifier(UserLocal)` | StatementEncoder |
| `processSingleStatement(Statement, Runnable)` | StatementEncoder, ExpressionEncoder |
| `parenthesize(Runnable)` | StatementEncoder, ExpressionEncoder, ResourceStructureEncoder |
| `appendStatement(BlockStatement)` | StatementEncoder |
| `appendEachArgument(SuperConstructorInvocationStatement)` | StatementEncoder |
| `processSuperReference()` | StatementEncoder |
| `appendAssignmentOperator()` | StatementEncoder |
| `getCodeStringBuilder()` | ResourceStructureEncoder |
| `tweedleTypeName(String)` | ExpressionEncoder, ResourceStructureEncoder |
| `getListSeparator()` | ResourceStructureEncoder |
| `appendInstantiation(String, Runnable)` | ExpressionEncoder, ResourceStructureEncoder |
| `appendArg(String, String/Runnable)` | ExpressionEncoder, ResourceStructureEncoder |
| `appendAnotherArg(String, String/Runnable)` | ResourceStructureEncoder |
| `getUserJointIdentifier(String)` | ResourceStructureEncoder (also public for AST node callbacks) |

No interfaces or inheritance are introduced. All collaboration uses direct
method calls within the same package, matching the Decoder delegate pattern.

## Bridge methods

Because `TweedleEncoder` extends `SourceCodeGenerator` (which defines the
visitor-pattern `@Override` methods), the `@Override` stubs must remain on
`TweedleEncoder`. Each stub delegates to the appropriate encoder:

```java
@Override
public void processInstantiation(InstanceCreation creation) {
  expressionEncoder.processInstantiation(creation, this);
}

@Override
public void processLocalDeclaration(LocalDeclarationStatement stmt) {
  statementEncoder.processLocalDeclaration(stmt, this);
}

@Override
public void processResourceType(String jointedModelResource) {
  resourceStructureEncoder.processResourceType(jointedModelResource, this);
}
```

Methods that call `super.method()` remain in `TweedleEncoder` because `super`
references cannot be forwarded to delegates:

```java
@Override
protected void appendClassFooter(String userTypeName) {
  appendString(EncoderMappings.typesWithAddedCode.getOrDefault(userTypeName, ""));
  super.appendClassFooter(userTypeName);
}
```

## Security boundary

The `Class.forName` call in `ResourceStructureEncoder.processResourceType` and
`processDynamicResource` loads resource classes by fully-qualified name. This
reflective code is consolidated in `ResourceStructureEncoder` and does not
accept user-provided class names — only names from the Alice AST's
`ProcessableNode` tree. The same reflection pattern existed in the original
`TweedleEncoder`.

The `ReleaseVirtualMachine` evaluation in `ExpressionEncoder.processInstantiation`
evaluates `PersonResource` instance creations to extract the summary string.
This is confined to `ExpressionEncoder` and was previously in `TweedleEncoder`.

No new I/O, network, or thread operations are introduced.

## Error handling contract

All error handling is preserved identically:

| Error pattern | Class | Behavior |
| --- | --- | --- |
| `RuntimeException` on missing resource class | ResourceStructureEncoder | `Class.forName` failure in `processResourceType` / `processDynamicResource` |
| `Logger.errln` on unexpected argument count | ExpressionEncoder | `appendOneArgument` single-arg expectation |
| `Logger.info` on skipped fields | ResourceStructureEncoder | Inaccessible or non-generator field during resource export |
| `Dialogs.showError` on unlabeled parameter | ExpressionEncoder | Parameter label fallback in `getParameterLabel` |

Error messages are preserved character-for-character. No new exception types
are introduced.

## Configuration

There is no runtime configuration for the encoder decomposition. It uses the
existing Tweedle grammar, Maven reactor, and JUnit configuration.

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
  -Dtest=TweedleEncoderTest,TweedleEncoderRenameContractTest,TweedleEncoderDecoderTest \
  test
```

Run the decoder delegate decomposition characterization test (exercises the
full encode→decode round-trip):

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=DecoderDelegateDecompositionCharacterizationTest \
  test
```

Run the silver-thread Tweedle round-trip test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SilverThreadTweedleDecoderRoundTripTest \
  test
```

Run the story-api-migration round-trip tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

All four suites must pass with identical results before and after the
decomposition.

## Acceptance criteria

| Criterion | Verification |
| --- | --- |
| `TweedleEncoder.java` ≤ 500 lines | `wc -l TweedleEncoder.java` |
| Four new delegate classes created | `EncoderMappings.java`, `StatementEncoder.java`, `ExpressionEncoder.java`, `ResourceStructureEncoder.java` exist in `core/ast/src/main/java/org/alice/serialization/tweedle/` |
| All delegates are package-private | No `public` class keyword on delegates |
| No public API changes to `TweedleEncoderDecoder` | `TweedleEncoderDecoder.java` is unchanged |
| `mvn -pl core/ast -am test` passes | Zero test failures |
| `mvn -pl core/story-api-migration -am test` passes | Zero test failures |
| `SilverThreadTweedleDecoderRoundTripTest` passes | Zero test failures |
| `DecoderDelegateDecompositionCharacterizationTest` passes | Zero test failures |
| Tweedle output is byte-identical | Encoder tests assert exact string output |

## Claim boundaries

This decomposition proves:

- The 959-line `TweedleEncoder` can be split into five focused classes without
  changing observable behavior.
- All existing `TweedleEncoderTest` assertions pass identically.
- All existing `TweedleEncoderRenameContractTest` assertions pass identically.
- All existing `TweedleEncoderDecoderTest` assertions pass identically.
- All existing `DecoderDelegateDecompositionCharacterizationTest` assertions
  pass identically.
- All existing `core/story-api-migration` tests pass identically.
- Error messages and exception types are preserved.

This decomposition does **not** prove:

| Non-claim | Reason |
| --- | --- |
| New encode capabilities | No new Tweedle constructs are supported. |
| Performance improvement | Decomposition is structural, not algorithmic. |
| Thread safety | `TweedleEncoder` was not thread-safe before; delegates do not change this. |
| Public API expansion | No new public methods or classes are introduced. |
| Decoder changes | Decoder classes are not modified. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Decoder delegate decomposition | [Decoder Delegate Decomposition](./decoder-delegate-decomposition.md) |
| Tweedle round-trip silver thread | [Silver Thread Tweedle Decoder Round-Trip Test](./silver-thread-tweedle-decoder-round-trip-test.md) |
| TweedleEncoder rename | [TweedleEncoder Rename](./tweedle-encoder-rename.md) |
| Encode/decode unit coverage | [Decode Coverage Characterization](./decode-coverage-characterization.md) |
