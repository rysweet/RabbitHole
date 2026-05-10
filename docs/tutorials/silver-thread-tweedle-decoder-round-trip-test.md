# Tutorial: Trace the Silver Thread Tweedle Decoder Round-Trip Test

This tutorial walks through the Tweedle decoder round-trip silver thread test
for Alice. You will trace the test method to understand how the Tweedle
encode→decode path is proven headlessly: from loading a real `.a3p` starter
project through encoding each `NamedUserType` to Tweedle source, decoding it
back, and verifying structural equality.

For the full contract, see the [Silver Thread Tweedle Decoder Round-Trip Test
reference](../reference/silver-thread-tweedle-decoder-round-trip-test.md).

## Contents

- [Goal](#goal)
- [1. Understand the motivation](#1-understand-the-motivation)
- [2. Trace project loading](#2-trace-project-loading)
- [3. Trace the encode step](#3-trace-the-encode-step)
- [4. Understand Encoder type renaming](#4-understand-encoder-type-renaming)
- [5. Trace the decode step](#5-trace-the-decode-step)
- [6. Trace the structural assertions](#6-trace-the-structural-assertions)
- [7. Trace UnsupportedTweedleDecodeException handling](#7-trace-unsupportedtweedledecodeexception-handling)
- [8. Run the test](#8-run-the-test)
- [9. Compare with adjacent tests](#9-compare-with-adjacent-tests)
- [10. Understand the boundaries](#10-understand-the-boundaries)

## Goal

Understand how `SilverThreadTweedleDecoderRoundTripTest` proves that Tweedle
source encoding and decoding preserve AST structural identity for types from a
real Alice project. After this tutorial you will be able to explain:

- Why the comparison baseline is the encoded Tweedle (not the original Java AST).
- What structural equality means (and does not mean) in this context.
- How the test documents decoder gaps without failing.
- What the terminals parameter provides to the Decoder.

Open the source in
`core/ide/src/test/java/org/alice/ide/SilverThreadTweedleDecoderRoundTripTest.java`
alongside this guide.

## 1. Understand the motivation

Alice has three distinct serialization paths:

```text
1. Binary .a3p archive:  Project → IoUtilities.writeProject → .a3p → IoUtilities.readProject → Project
2. Tweedle source:       NamedUserType → Encoder → String → Decoder → NamedUserType
3. JSON .a3w player:     Project → manifest + Tweedle source → .a3w → reader → Project
```

The existing silver thread tests (`SilverThreadLaunchBuildRunTest` and
`SilverThreadSaveRoundTripTest`) cover path 1 — the binary archive round-trip.
The unit-level `TweedleEncoderDecoderTest` covers path 2 for synthetic types.

This test fills the gap: it exercises path 2 with **real types from a real
project**. Real types have field initializers, resource references, constructor
assignments, and method invocations that synthetic test types do not.

## 2. Trace project loading

Find the project loading code:

```java
File starterFile = new File(
    getClass().getResource("/starters/indiaMinimum.a3p").toURI());
Project project = IoUtilities.readProject(starterFile);
```

This is the same loading pattern as `SilverThreadLaunchBuildRunTest`'s starter
project test. The `indiaMinimum.a3p` resource lives at
`core/ide/src/test/resources/starters/indiaMinimum.a3p`.

The project contains multiple `NamedUserType` declarations: the program type,
one or more scene types, and potentially model-specific types.

## 3. Trace the encode step

Find the encoding loop:

```java
TweedleEncoderDecoder codec = new TweedleEncoderDecoder();
String tweedleSource = codec.encode(type);
```

`TweedleEncoderDecoder.encode(NamedUserType)` delegates to `new Encoder().encode(node)`.
The Encoder walks the AST and produces Tweedle source code — a text
representation of the type with its fields, methods, and constructors.

Key point: the Encoder **renames** types. Java `Double` becomes Tweedle
`DecimalNumber`, `Integer` becomes `WholeNumber`, `String` becomes `TextString`.
Member names may also change (`rint` → `round`, `ceil` → `ceiling`). This means
the encoded output is structurally equivalent to the original AST but uses
Tweedle naming conventions.

## 4. Understand Encoder type renaming

The Encoder maintains static rename maps:

```java
typesToRename.put("Double", "DecimalNumber");
typesToRename.put("Integer", "WholeNumber");
typesToRename.put("String", "TextString");
```

When comparing encoded vs. decoded types, use the Tweedle names. For example:
- A Java `UserField` with `valueType = Double` encodes as `DecimalNumber x`.
- The Decoder reads `DecimalNumber x` and produces a `UserField` whose type
  name is `DecimalNumber`, not `Double`.

The test compares the decoded structure against the encoded source, not the
original Java AST. This is deliberate.

## 5. Trace the decode step

Find the decoding step:

```java
Set<AbstractDeclaration> terminals = new HashSet<>(project.getNamedUserTypes());
AbstractNode decoded = codec.decode(tweedleSource, terminals);
```

Three critical decisions here:

1. **Terminals**: The project's `NamedUserType` declarations are passed as
   terminals. The Decoder uses these to resolve type references within the
   Tweedle source. Without terminals, custom scene type references would fail.

2. **Return type**: `decode()` returns `AbstractNode`, not `NamedUserType`. The
   test casts and verifies: `assertTrue(decoded instanceof NamedUserType)`.

3. **Checked exception**: `TweedleEncoderDecoder.decode()` declares
   `throws VersionNotSupportedException` (checked). The test method declares
   `throws Exception` so this propagates without an explicit catch. It is
   unlikely to be thrown for raw Tweedle source, but the signature requires it.

## 6. Trace the structural assertions

Find the structural comparison:

```java
assertEquals(originalType.getName(), decodedType.getName());
assertEquals(originalMethods.size(), decodedMethods.size());
assertEquals(originalMethodNames, decodedMethodNames);
assertEquals(originalFields.size(), decodedFields.size());
```

"Structural equality" means:

| Property | Compared? | Notes |
| --- | --- | --- |
| Class name | ✅ Yes | Exact string match. |
| Method count | ✅ Yes | Integer equality. |
| Method names | ✅ Yes | Set equality (order-independent). |
| Field count | ✅ Yes | Integer equality. |
| Constructor presence | ✅ Yes | Boolean (has/doesn't have). |
| Non-empty method body | ✅ Yes | At least one method has statements. |
| UUIDs | ❌ No | Decoded nodes get new UUIDs. |
| Object identity | ❌ No | Different instances. |
| Supertype resolution | ❌ No | Decoder may create placeholder types. |
| Field initializer values | ❌ No | Complex initializers may not round-trip. |
| Statement deep equality | ❌ No | Only count and presence checked. |

This level of comparison catches structural regressions (lost methods, wrong
names, missing fields) without coupling to Decoder implementation details.

## 7. Trace UnsupportedTweedleDecodeException handling

Find the exception handling:

```java
try {
    AbstractNode decoded = codec.decode(tweedleSource, terminals);
    // ... structural assertions ...
    successCount++;
} catch (UnsupportedTweedleDecodeException e) {
    // Document the gap — this type uses unsupported AST constructs
    gapCount++;
}
```

Real starter project types may use AST constructs the Decoder does not yet
support. Rather than failing the entire test, the test:

1. Catches the exception per-type.
2. Increments a gap counter.
3. Continues to the next type.
4. Asserts at least one type succeeded (fail-safe).

This design makes the test a characterization test: it documents what works
today while signaling where expansion is needed.

## 8. Run the test

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadTweedleDecoderRoundTripTest \
  test
```

The test passes without a display server or network access.

## 9. Compare with adjacent tests

| Test | What it proves | Serialization path |
| --- | --- | --- |
| `SilverThreadLaunchBuildRunTest` | Binary `.a3p` archive round-trip + VM execution | `IoUtilities.writeProject` / `readProject` |
| `SilverThreadSaveRoundTripTest` | Production save pipeline round-trip | `ProjectApplication.saveProjectTo` |
| `TweedleEncoderDecoderTest` | Unit-level encode/decode for synthetic types | `TweedleEncoderDecoder` with hand-built types |
| **This test** | Tweedle source round-trip for **real project types** | `TweedleEncoderDecoder.encode` → `decode` |

This test is unique because it exercises the Tweedle text codec with types that
come from a real `.a3p` project — types that have real field initializers,
real method bodies, and real type references that synthetic tests skip.

## 10. Understand the boundaries

After tracing the test, confirm you can answer:

| Question | Answer |
| --- | --- |
| Does this prove the `.a3p` binary format works? | No. That is `SilverThreadLaunchBuildRunTest`. |
| Does this prove the production save works? | No. That is `SilverThreadSaveRoundTripTest`. |
| Does this prove the Decoder handles all Tweedle? | No. Some types throw `UnsupportedTweedleDecodeException`. |
| Does this prove deep AST equality? | No. Only structural properties (names, counts). |
| What does it prove? | Tweedle source encoding and decoding preserve structural identity for real project types. |

The test is a characterization of current decoder capability against real
project data. It is the foundation for expanding decoder coverage — each
previously-unsupported type that begins round-tripping successfully will appear
as an increase in the success count.
