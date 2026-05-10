# TweedleEncoder Rename

This reference describes the rename of `Encoder` to `TweedleEncoder` in the
`org.alice.serialization.tweedle` package. The rename gives the Tweedle
encoding class a name that matches the `TweedleEncoderDecoder` facade and
removes the ambiguity with Java's `java.beans.Encoder` and the separate XML
`org.lgna.project.io.Encoder` class.

The rename is a pure identifier change. The public API surface —
`TweedleEncoderDecoder` — is unchanged. All existing encode behavior,
generated Tweedle source output, and round-trip fidelity are preserved
identically.

## Contents

- [Motivation](#motivation)
- [What changed](#what-changed)
- [Architecture](#architecture)
- [Public API](#public-api)
- [Implementor interface contracts](#implementor-interface-contracts)
- [Configuration](#configuration)
- [Validation](#validation)
- [Examples](#examples)
- [Claim boundaries](#claim-boundaries)

## Motivation

The original `Encoder.java` name collided with `java.beans.Encoder` and the
separate XML serializer `org.lgna.project.io.Encoder`. Adding the `Tweedle`
prefix makes the class self-documenting — it belongs to the Tweedle codec — and
aligns it with the public facade `TweedleEncoderDecoder`. The decode-side
classes (`Decoder`, `ExpressionDecoder`, `StatementDecoder`, `FieldDecoder`) do
not share this ambiguity, so they keep their shorter names.

RabbitHole issue #480 renames the file and class to `TweedleEncoder`, aligning
the naming convention across the entire `org.alice.serialization.tweedle`
package.

## What changed

| Before | After |
| --- | --- |
| `Encoder.java` | `TweedleEncoder.java` |
| `public class Encoder extends SourceCodeGenerator` | `public class TweedleEncoder extends SourceCodeGenerator` |
| `Encoder()` constructor | `TweedleEncoder()` constructor |
| `Encoder(Set<AbstractDeclaration>)` constructor | `TweedleEncoder(Set<AbstractDeclaration>)` constructor |
| `new Encoder()` in `TweedleEncoderDecoder` | `new TweedleEncoder()` |
| `new Encoder(terminals)` in `TweedleEncoderDecoder` | `new TweedleEncoder(terminals)` |
| `import ...tweedle.Encoder` in 7 files | `import ...tweedle.TweedleEncoder` |
| `Encoder` parameter type in 2 interfaces + 5 implementors | `TweedleEncoder` parameter type |

The file was renamed with `git mv` to preserve history.

### Files modified

| File | Change |
| --- | --- |
| `core/ast/.../tweedle/TweedleEncoder.java` | File rename + class + constructors |
| `core/ast/.../tweedle/TweedleEncoderDecoder.java` | 2 instantiation sites |
| `core/ast/.../code/IdentifiableTweedleNode.java` | Import + parameter type |
| `core/ast/.../code/InstantiableTweedleNode.java` | Import + parameter type |
| `core/story-api/.../resources/JointId.java` | Import + method signatures |
| `core/story-api/.../resources/JointArrayId.java` | Import + method signature |
| `core/story-api/.../resources/DynamicJointId.java` | Import + method signatures |
| `core/story-api/.../implementation/JointIdTransformationPair.java` | Import + method signature |
| `core/story-api/.../Pose.java` | Import + method signature |

### Files NOT modified

- `Decoder.java` — untouched, remains focused on decoding
- `ExpressionDecoder.java`, `StatementDecoder.java`, `FieldDecoder.java` — no
  encoder references
- `SourceCodeGenerator.java` — abstract base class, no rename needed
- `org.lgna.project.io.Encoder` — separate XML encoder, not part of this rename
- Test files — none import `Encoder` directly; all use the
  `TweedleEncoderDecoder` facade

## Architecture

```text
TweedleEncoderDecoder (public facade — unchanged)
├── TweedleEncoder (959 lines, all encoding logic)
│   └── extends SourceCodeGenerator
│   └── implements visitor pattern via ProcessableNode.process(AstProcessor)
└── Decoder (343 lines, all decoding logic)
    ├── ExpressionDecoder
    ├── StatementDecoder
    └── FieldDecoder
```

The encode side and decode side are now symmetrically named within the facade:

- `TweedleEncoderDecoder.encode(node)` → `new TweedleEncoder().encode(node)`
- `TweedleEncoderDecoder.decode(document)` → `new Decoder().decode(document)`

## Public API

The public API is exclusively `TweedleEncoderDecoder`. No API changes are made
by this rename.

```java
public class TweedleEncoderDecoder implements EncoderDecoder<String> {
  public <N extends AbstractNode & ProcessableNode> String encode(N node);
  public <N extends AbstractNode & ProcessableNode> String encode(N node,
      Set<AbstractDeclaration> terminals);
  public <N extends ProcessableNode> String encodeProcessable(N node);
  public AbstractNode decode(String document) throws VersionNotSupportedException;
  public AbstractNode decode(String document, Set<AbstractDeclaration> terminals)
      throws VersionNotSupportedException;
  public AbstractNode decode(String document, Set<AbstractDeclaration> terminals,
      boolean allowLiteralArithmeticFieldInitializers) throws VersionNotSupportedException;
  public AbstractNode copy(String document, Set<AbstractDeclaration> terminals)
      throws VersionNotSupportedException;
}
```

All encode entry points instantiate `TweedleEncoder`. Callers see only
`TweedleEncoderDecoder` and never reference `TweedleEncoder` directly in normal
usage.

## Implementor interface contracts

Two interfaces reference `TweedleEncoder` as a parameter type. These interfaces
define callback contracts used by the visitor-pattern encoding:

### IdentifiableTweedleNode

```java
public interface IdentifiableTweedleNode {
  String getCodeIdentifier(TweedleEncoder processor);
}
```

Implementors return their Tweedle-language identifier when visited by the
encoder. `JointId` implements this interface directly. `DynamicJointId`
inherits it from `JointId` and overrides `getCodeIdentifier`.

### InstantiableTweedleNode

```java
public interface InstantiableTweedleNode {
  void encodeDefinition(TweedleEncoder processor);
}
```

Implementors emit their Tweedle definition body into the encoder's output
buffer. `JointId`, `JointArrayId`, `JointIdTransformationPair`, and `Pose`
implement this directly. `DynamicJointId` inherits it from `JointId` but
does not override `encodeDefinition`.

## Configuration

No new configuration. The `TweedleEncoder` class has the same package-private
constructors as before:

```java
TweedleEncoder()                              // empty terminal set
TweedleEncoder(Set<AbstractDeclaration> terminals)  // explicit terminals
```

Both constructors are package-private and called only by `TweedleEncoderDecoder`.

### Non-interface methods

`JointId` and `DynamicJointId` also have a `protected getJointName(TweedleEncoder)`
method that takes the encoder as a parameter but is not part of either interface.
This helper is called inside `encodeDefinition` and `getCodeIdentifier` to
resolve the joint's display name. The rename changes its parameter type too.

## Validation

Run both affected test suites to confirm the rename:

```bash
# Core AST tests (includes 57 characterization tests)
git submodule update --init tweedle-lang && \
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test -q

# Story API migration tests (round-trip fidelity)
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test -q

# Silver thread round-trip (encode→decode structural identity)
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadTweedleDecoderRoundTripTest test -q
```

All three commands must exit 0 with zero test failures.

## Examples

### Before (Encoder)

```java
// In JointId.java
import org.alice.serialization.tweedle.Encoder;

@Override
public String getCodeIdentifier(Encoder encoder) {
  return encoder.getFieldReference(containingClass.getSimpleName(), getJointName(encoder));
}
```

### After (TweedleEncoder)

```java
// In JointId.java
import org.alice.serialization.tweedle.TweedleEncoder;

@Override
public String getCodeIdentifier(TweedleEncoder encoder) {
  return encoder.getFieldReference(containingClass.getSimpleName(), getJointName(encoder));
}
```

The method body is identical. Only the import and parameter type change.

### Encoding a NamedUserType

```java
// Usage is unchanged — callers use the facade
TweedleEncoderDecoder codec = new TweedleEncoderDecoder();
String tweedleSource = codec.encode(namedUserType, terminals);
```

The facade internally calls `new TweedleEncoder(terminals).encode(node)`.

## Claim boundaries

This rename:

- **Does** align Tweedle encoder naming with the `TweedleEncoderDecoder`
  facade and disambiguate from `java.beans.Encoder` / `org.lgna.project.io.Encoder`
- **Does** preserve git file history via `git mv`
- **Does** pass all 57 characterization tests, story-api-migration round-trip
  tests, and the silver thread round-trip test
- **Does not** change any public API
- **Does not** change any encoding behavior or output
- **Does not** touch `Decoder.java` or its delegates
- **Does not** affect the XML `org.lgna.project.io.Encoder` class
- **Does not** move, merge, or split any methods
- **Does not** introduce new files beyond the renamed `TweedleEncoder.java`
