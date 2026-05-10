# Tutorial: Trace the TweedleEncoder Rename

A guided walkthrough of the `Encoder` → `TweedleEncoder` rename, showing how
each changed file connects and why the rename is safe.

## What you will learn

- How `TweedleEncoderDecoder` instantiates `TweedleEncoder`
- How the visitor-pattern interfaces (`IdentifiableTweedleNode`,
  `InstantiableTweedleNode`) use `TweedleEncoder` as a parameter type
- How Story API resource classes implement those interfaces
- Why no test files changed

## 1. Start at the facade

Open `TweedleEncoderDecoder.java`:

```java
public <N extends ProcessableNode> String encodeProcessable(N node) {
  return new TweedleEncoder().encode(node);
}

public <N extends AbstractNode & ProcessableNode> String encode(
    N node, Set<AbstractDeclaration> terminals) {
  return new TweedleEncoder(terminals).encode(node);
}
```

The facade creates a fresh `TweedleEncoder` for each encode call. This is the
only place `TweedleEncoder` is instantiated — both constructors are
package-private.

**Key insight:** Because the constructors are package-private, no code outside
`org.alice.serialization.tweedle` can create a `TweedleEncoder` directly.

## 2. Follow the encode entry point

In `TweedleEncoder.java`:

```java
public String encode(ProcessableNode node) {
  node.process(this);
  return getCodeStringBuilder().toString();
}
```

The encoder passes itself (`this`) to the node's `process` method. The node
calls back into the encoder's visitor methods (`processMethod`,
`processConstructor`, `processField`, etc.) inherited from
`SourceCodeGenerator`.

## 3. Trace the interface contracts

Two interfaces accept `TweedleEncoder` as a parameter:

### IdentifiableTweedleNode

```java
public interface IdentifiableTweedleNode {
  String getCodeIdentifier(TweedleEncoder processor);
}
```

The encoder calls `getCodeIdentifier(this)` when it needs the Tweedle-language
name for a node. This is a callback — the node decides its own identifier.

### InstantiableTweedleNode

```java
public interface InstantiableTweedleNode {
  void encodeDefinition(TweedleEncoder processor);
}
```

The encoder calls `encodeDefinition(this)` when a resource node needs to emit
its body. The node writes into the encoder's buffer using public methods like
`appendNewJointId`, `appendNewPose`, and `getFieldReference`.

## 4. Check a Story API implementor

Open `JointId.java` to see both interfaces in action:

```java
import org.alice.serialization.tweedle.TweedleEncoder;

public class JointId implements InstantiableTweedleNode, IdentifiableTweedleNode {

  protected String getJointName(TweedleEncoder encoder) {
    return toString();
  }

  @Override
  public void encodeDefinition(TweedleEncoder encoder) {
    encoder.appendNewJointId(getJointName(encoder),
                             parent == null ? "null" : parent.getCodeIdentifier(encoder));
  }

  @Override
  public String getCodeIdentifier(TweedleEncoder encoder) {
    return encoder.getFieldReference(containingClass.getSimpleName(), getJointName(encoder));
  }
}
```

The method bodies are identical to the pre-rename code. Only the import and
parameter type changed from `Encoder` to `TweedleEncoder`. Notice that
`getJointName` is not part of either interface — it is a protected helper used
by the encoder-facing methods.

## 5. Understand why no tests changed

All tests use the `TweedleEncoderDecoder` facade:

```java
TweedleEncoderDecoder codec = new TweedleEncoderDecoder();
String source = codec.encode(userType, terminals);
AbstractNode decoded = codec.decode(source, terminals);
```

No test file imports `Encoder` or `TweedleEncoder` directly. The rename is
invisible to the test layer — all 57 characterization tests and the silver
thread round-trip test pass without modification.

## 6. Verify the naming convention

After the rename, all classes in `org.alice.serialization.tweedle` follow a
consistent pattern:

| Class | Role |
| --- | --- |
| `TweedleEncoderDecoder` | Public facade |
| `TweedleEncoder` | Encode path (AST → Tweedle source) |
| `Decoder` | Decode coordinator (Tweedle source → AST) |
| `ExpressionDecoder` | Decode delegate for expressions |
| `StatementDecoder` | Decode delegate for statements |
| `FieldDecoder` | Decode delegate for fields |
| `UnsupportedTweedleDecodeException` | Decode error type |

The encode side is a single class (`TweedleEncoder`) because the encoding
visitor pattern is inherently simpler than the decode parsing logic.

## Summary

The rename touches 9 files but changes zero behavior:

1. **1 file renamed:** `Encoder.java` → `TweedleEncoder.java` (class +
   constructors)
2. **1 facade updated:** `TweedleEncoderDecoder` instantiation sites
3. **2 interfaces updated:** parameter type in `IdentifiableTweedleNode` and
   `InstantiableTweedleNode`
4. **5 story-api files updated:** `JointId`, `JointArrayId`, `DynamicJointId`,
   `JointIdTransformationPair`, `Pose` — import and method parameter types
5. **0 tests changed:** all tests use the facade and never see the rename

`DynamicJointId` extends `JointId` (inheriting both interfaces) and overrides
`getCodeIdentifier` and `getJointName` — both take the encoder parameter.
`JointArrayId` implements only `InstantiableTweedleNode`.
