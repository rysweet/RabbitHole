# ASG Scene Graph I/O Decomposition

This reference describes the internal decomposition of the 1241-line `ASG.java`
into a thin façade plus two package-private delegate classes: `ASGEncoder` and
`ASGDecoder`.

The decomposition is a pure internal refactor. The public API surface — `ASG` —
is unchanged. All existing encode/decode behavior, binary format versioning,
ZIP packaging, and error messages are preserved identically.

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
  - [ASG (façade)](#asg-façade)
  - [ASGEncoder](#asgencoder)
  - [ASGDecoder](#asgdecoder)
- [Public API](#public-api)
- [Package-private collaboration](#package-private-collaboration)
- [Inner classes](#inner-classes)
- [Security boundary](#security-boundary)
- [Error handling contract](#error-handling-contract)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

The original `ASG.java` contained 1241 lines mixing two distinct concerns:
encoding scene graph components to XML+ZIP archives, and decoding XML+ZIP
archives back into scene graph components. Additionally, it contained helper
inner classes, DOM traversal utilities, legacy classname conversion, and matrix
utility code. This made the class difficult to navigate, review, and extend
safely.

RabbitHole issue #560 decomposes `ASG` into focused delegates, mirroring the
[Encoder delegate decomposition](./encoder-delegate-decomposition.md) and
[Decoder delegate decomposition](./decoder-delegate-decomposition.md) patterns
from the Tweedle serialization layer. Each delegate is small enough to
understand in isolation and extend independently.

## Architecture

```text
ASG (public façade — all public static methods retained, ~120 lines)
├── ASGEncoder (package-private, ~500 lines)
│   ├── MatrixUtilities (package-private inner class)
│   │   └── getRow(double[], AffineMatrix4x4, int)
│   │   └── getRow(double[], Matrix3x3, int)
│   ├── Binary encoders
│   │   └── encodeVertexArrayInBinary, encodeIntArrayInBinary, encodeDoubleArrayInBinary
│   ├── XML element encoding
│   │   └── encodeElement, encodeComponent, encodeInternal
│   └── ZIP packaging
│       └── encode(Component, OutputStream), encode(Component, File), encode(Component, String)
└── ASGDecoder (package-private, ~700 lines)
    ├── AbstractPropertyReference (package-private inner class)
    ├── PropertyReferenceToElement (package-private inner class)
    ├── DOM helpers
    │   └── getFirstChild, getChildren, getNodeText, valueOf
    ├── Legacy classname conversion
    │   └── isDeadProperty, convertPropertyIfNecessary, convertClassnameIfNecessary
    ├── Binary decoders
    │   └── decodeVertexArrayInBinary, decodeIntArrayInBinary, decodeDoubleArrayInBinary
    ├── XML element decoding
    │   └── decodeElement, decodeComponent, decodeInternal
    └── ZIP extraction
        └── decode(InputStream, HashMap), decodeZip(InputStream), decode(File), decode(String)
```

All three classes live in `edu.cmu.cs.dennisc.scenegraph.io`. The delegates are
package-private with no public constructors. They contain only static methods
and are not instantiated.

## Class responsibilities

### ASG (façade)

| Responsibility | Members |
| --- | --- |
| Format version constant | `public static final double VERSION = 1.0` |
| Root filename constant | `static final String ROOT_FILENAME = "root.xml"` (package-private) |
| Encode to stream | `public static void encode(Component, OutputStream)` → delegates to `ASGEncoder.encode(...)` |
| Encode to file | `public static void encode(Component, File)` → delegates to `ASGEncoder.encode(...)` |
| Encode to path | `public static void encode(Component, String)` → delegates to `ASGEncoder.encode(...)` |
| Encode vertex binary | `public static void encodeVertexArrayInBinary(Vertex[], OutputStream)` → delegates to `ASGEncoder` |
| Encode int binary | `public static void encodeIntArrayInBinary(int[], OutputStream)` → delegates to `ASGEncoder` |
| Encode double binary | `public static void encodeDoubleArrayInBinary(double[], OutputStream)` → delegates to `ASGEncoder` |
| Decode from stream | `public static Component decode(InputStream, HashMap)` → delegates to `ASGDecoder.decode(...)` |
| Decode ZIP stream | `public static Component decodeZip(InputStream)` → delegates to `ASGDecoder.decodeZip(...)` |
| Decode from file | `public static Component decode(File)` → delegates to `ASGDecoder.decode(...)` |
| Decode from path | `public static Component decode(String)` → delegates to `ASGDecoder.decode(...)` |
| Decode vertex binary | `public static Vertex[] decodeVertexArrayInBinary(InputStream)` → delegates to `ASGDecoder` |
| Decode int binary | `public static int[] decodeIntArrayInBinary(InputStream)` → delegates to `ASGDecoder` |
| Decode double binary | `public static double[] decodeDoubleArrayInBinary(InputStream)` → delegates to `ASGDecoder` |

Every public method is a one-line delegation. The façade contains no logic
beyond forwarding.

### ASGEncoder

| Responsibility | Methods |
| --- | --- |
| Matrix row extraction | `MatrixUtilities.getRow(double[], AffineMatrix4x4, int)`, `MatrixUtilities.getRow(double[], Matrix3x3, int)` |
| Element key generation | `getKey(edu.cmu.cs.dennisc.scenegraph.Element)` — returns `Integer.toString(element.hashCode())` |
| XML element encoding | `encodeElement(scenegraph.Element, Document, String, HashMap, HashMap, boolean)` — encodes a scene graph element to an XML DOM element including all properties |
| Component encoding | `encodeComponent(Component, Document, String, HashMap, HashMap, boolean)` — recursive component tree encoding |
| Internal encode | `encodeInternal(Component, OutputStream, HashMap, boolean)` — XML document creation and serialization |
| ZIP packaging | `encode(Component, OutputStream)` — packages encoded XML and binary resources into ZIP with CRC32 for PNG entries |
| File encoding | `encode(Component, File)` — FileOutputStream wrapper |
| Path encoding | `encode(Component, String)` — File wrapper |
| Vertex binary encode | `encodeVertexArrayInBinary(Vertex[], OutputStream)` — version 3 binary vertex format |
| Int array binary encode | `encodeIntArrayInBinary(int[], OutputStream)` — version 2 binary int array format |
| Double array binary encode | `encodeDoubleArrayInBinary(double[], OutputStream)` — version 2 binary double array format |
| Small array threshold | `SMALL_ENOUGH_PRIMITIVE_ARRAY_LENGTH_TO_ENCODE_AS_TEXT` — arrays smaller than this are text-encoded in XML |
| Text serialization helpers | Private methods: `encodeIntArray`, `encodeDoubleArray`, `encodeTuple3d`, `encodeTuple3f`, `encodeTexCoord2f`, `encodeColor4f` — move with `encodeElement` |

`ASGEncoder` references `ASG.VERSION` for the version attribute written to
encoded XML documents and `ASG.ROOT_FILENAME` for the ZIP entry name of the
root XML document.

### ASGDecoder

| Responsibility | Methods |
| --- | --- |
| DOM child access | `getFirstChild(Node, String)` — first child element with given tag name |
| DOM children list | `getChildren(Node, String)` — all child elements with given tag name |
| DOM text extraction | `getNodeText(Node)` — text content of a node |
| Enum value resolution | `valueOf(Class, String)` — calls `valueOf` reflectively on enum-like classes |
| Dead property check | `isDeadProperty(String)` — returns true for known removed/dead properties |
| Property conversion | `convertPropertyIfNecessary(String)` — renames legacy property names |
| Classname conversion | `convertClassnameIfNecessary(String)` — maps legacy class names to current names (e.g. `Vertex3d` → `Vertex`, `TextureMap` → `Texture`) |
| Legacy package constant | `OLD_PACKAGE` (`"edu.cmu.cs.stage3."`) — private constant used by `convertClassnameIfNecessary` |
| XML element decoding | `decodeElement(org.w3c.dom.Element, HashMap, HashMap, Vector)` — decodes a single XML element into a scene graph element |
| Component decoding | `decodeComponent(org.w3c.dom.Element, HashMap, HashMap, Vector)` — recursive component tree decoding |
| Internal decode | `decodeInternal(InputStream, HashMap)` — XML document parsing and delegation |
| Stream decode | `decode(InputStream, HashMap)` — buffered input wrapper |
| ZIP decode | `decodeZip(InputStream)` — extracts ZIP entries, locates root XML, delegates to `decode` |
| File decode | `decode(File)` — tries ZIP first, falls back to plain XML |
| Path decode | `decode(String)` — File wrapper |
| Vertex binary decode | `decodeVertexArrayInBinary(InputStream)` — supports versions 1, 2, and 3 |
| Int array binary decode | `decodeIntArrayInBinary(InputStream)` — supports versions 1 and 2 with winding-order swap |
| Double array binary decode | `decodeDoubleArrayInBinary(InputStream)` — supports version 2 (version 1 is a no-op returning null) |
| Text deserialization helpers | Private methods: `decodeIntArray`, `decodeDoubleArray`, `decodePoint3`, `decodeVector3f`, `decodeVector2f`, `decodeTexCoord2f`, `decodeColor4f` — move with `decodeElement` |
| Mutable state | `s_deadProperties` (lazily-initialized `Set<String>`) — moves with `isDeadProperty` |

`ASGDecoder` references `ASG.ROOT_FILENAME` when locating the root XML entry
in ZIP archives.

## Public API

The public API is exclusively `ASG`. No API changes are made by this
decomposition.

```java
public class ASG {
  public static final double VERSION = 1.0;

  // Encode
  public static void encode(Component component, OutputStream os);
  public static void encode(Component component, File file);
  public static void encode(Component component, String path);
  public static void encodeVertexArrayInBinary(Vertex[] vertices, OutputStream os);
  public static void encodeIntArrayInBinary(int[] array, OutputStream os);
  public static void encodeDoubleArrayInBinary(double[] array, OutputStream os);

  // Decode
  public static Component decode(InputStream is, HashMap<String, InputStream> filenameToStreamMap);
  public static Component decodeZip(InputStream is);
  public static Component decode(File file);
  public static Component decode(String path);
  public static Vertex[] decodeVertexArrayInBinary(InputStream is);
  public static int[] decodeIntArrayInBinary(InputStream is);
  public static double[] decodeDoubleArrayInBinary(InputStream is);
}
```

All thirteen public methods delegate to the corresponding `ASGEncoder` or
`ASGDecoder` static method. Callers never see the delegate classes.

## Package-private collaboration

The delegates access `ASG` constants via package-private visibility. The
following `ASG` members are accessed by delegates:

| Member | Used by |
| --- | --- |
| `VERSION` (public) | `ASGEncoder` (writes version attribute to XML root) |
| `ROOT_FILENAME` (package-private) | `ASGEncoder` (ZIP entry name), `ASGDecoder` (ZIP entry lookup) |

No interfaces or inheritance are introduced. All collaboration uses direct
static method calls and constant references within the same package.

## Inner classes

Three helper classes currently defined as top-level package-private classes in
the `ASG.java` compilation unit are relocated as inner classes of their
respective delegate:

| Class | New location | Visibility |
| --- | --- | --- |
| `MatrixUtilities` | Inner class in `ASGEncoder.java` | package-private |
| `AbstractPropertyReference` | Inner class in `ASGDecoder.java` | package-private |
| `PropertyReferenceToElement` | Inner class in `ASGDecoder.java` | package-private |

All three classes retain their package-private visibility and identical behavior.

## Security boundary

### XML parsing (decode path)

`ASGDecoder.decodeInternal` uses `DocumentBuilderFactory.newInstance()` to parse
XML. The pre-existing code does **not** disable external entities or DTD
processing (a latent XXE vulnerability). This decomposition preserves the
existing behavior exactly — the security posture is neither improved nor
degraded.

> **Note:** Hardening the `DocumentBuilderFactory` against XXE is a separate
> improvement tracked outside this decomposition.

### Reflection (decode path)

`ASGDecoder.decodeElement` uses `ReflectionUtilities.newInstance` and
`Method.invoke` (via `valueOf`) to reconstruct scene graph elements from
serialized class names. This is the pre-existing deserialization behavior,
moved verbatim to `ASGDecoder`. No new reflection call sites are introduced.

### XML writing (encode path)

`ASGEncoder.encodeInternal` uses `DocumentBuilderFactory.newInstance()` to
create an empty DOM document, and `TransformerFactory` to serialize it. The
`DocumentBuilderFactory` usage in the encode path creates new documents only
(no untrusted input parsing), so it carries no XXE risk. This is standard
JAXP output with no security implications beyond the pre-existing behavior.

### Access widening

No private methods are made public. Private methods in the original `ASG.java`
remain private in their new delegate classes. The only shared surface is two
constants (`VERSION`, `ROOT_FILENAME`) that were already accessible within the
package.

## Error handling contract

All `RuntimeException` wrapping of checked exceptions is preserved in the
delegate that owns the method. Error messages and exception chains are
unchanged.

| Error pattern | Class | Preserved behavior |
| --- | --- | --- |
| `ParserConfigurationException` → `RuntimeException` | ASGEncoder (`encodeInternal`), ASGDecoder (`decodeInternal`) | Identical |
| `SAXException` → `RuntimeException` | ASGDecoder (`decodeInternal`) | Identical |
| `IOException` → `RuntimeException` | ASGEncoder (`encode`, `encode(File)`, binary encode methods), ASGDecoder (`decodeZip`, `decode(File)`, binary decode methods) | Identical |
| `TransformerException` → `RuntimeException` | ASGEncoder (`encodeInternal`) | Identical |
| `FileNotFoundException` → `RuntimeException` | ASGEncoder (`encode(File)`), ASGDecoder (`decode(File)`) | Identical |
| Invalid binary version → `RuntimeException` | ASGDecoder (`decodeVertexArrayInBinary`, `decodeIntArrayInBinary`, `decodeDoubleArrayInBinary`) | Identical |
| Unresolved property reference → `RuntimeException` | ASGDecoder (`PropertyReferenceToElement.resolve`) | Identical |
| Non-public valueOf → `RuntimeException` | ASGDecoder (`valueOf`) | Identical |

## Configuration

There is no runtime configuration for the ASG decomposition. It uses the
existing Maven reactor and JUnit configuration.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
```

Automation can keep the saved Node memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

`NODE_OPTIONS` is not an ASG option — it is a saved environment preference for
the build toolchain.

## Validation

Run the focused `core/scenegraph` module tests from the repository root:

```bash
mvn -pl core/scenegraph -am \
  -DfailIfNoTests=false \
  -Dcheckstyle.skip \
  test
```

This validates that all scene graph serialization tests pass with the
decomposed classes.

No new test files are introduced by this decomposition. The existing test suite
exercises the public `ASG` methods which now delegate to the internal encoders
and decoders.

## Acceptance criteria

| Criterion | Verification |
| --- | --- |
| `ASG.java` ≤ 500 lines | `wc -l ASG.java` (expected ~120 lines) |
| Two new delegate classes created | `ASGEncoder.java` and `ASGDecoder.java` exist in `core/scenegraph/src/main/java/edu/cmu/cs/dennisc/scenegraph/io/` |
| All delegates are package-private | No `public` class keyword on delegates |
| No public API changes to `ASG` | All 13 public static methods retained with identical signatures |
| `VERSION` and `ROOT_FILENAME` stay on `ASG` | Constants not duplicated in delegates |
| `mvn -pl core/scenegraph -am -DfailIfNoTests=false -Dcheckstyle.skip test` passes | Zero test failures |
| Binary format compatibility preserved | Encode/decode version numbers unchanged (vertex v3, int v2, double v2) |
| Error messages preserved | All RuntimeException messages identical |

## Claim boundaries

This decomposition proves:

- The 1241-line `ASG` can be split into three focused classes without changing
  observable behavior.
- All existing scene graph serialization tests pass identically.
- Binary format versions are preserved (vertex version 3 for encode; versions
  1, 2, 3 for decode; int/double version 2 for encode; versions 1, 2 for
  decode).
- ZIP packaging behavior is preserved (STORED method with CRC32 for PNGs,
  DEFLATED for XML).
- Error messages and exception types are preserved.

This decomposition does **not** prove:

| Non-claim | Reason |
| --- | --- |
| New serialization capabilities | No new element types or properties are supported. |
| Performance improvement | Decomposition is structural, not algorithmic. |
| Thread safety | `ASG` was not thread-safe before; delegates do not change this. |
| Public API expansion | No new public methods or classes are introduced. |
| XXE hardening | DocumentBuilderFactory configuration is preserved as-is. |
| Reflection security | Deserialization allowlisting is not added. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Encoder delegate patterns | [Encoder Delegate Decomposition](./encoder-delegate-decomposition.md) |
| Decoder delegate patterns | [Decoder Delegate Decomposition](./decoder-delegate-decomposition.md) |
