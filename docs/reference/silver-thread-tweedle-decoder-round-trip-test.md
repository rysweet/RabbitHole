# Silver Thread Tweedle Decoder Round-Trip Test

This reference defines the Tweedle decoder round-trip silver thread test for
Alice. The executable proof is
`org.alice.ide.SilverThreadTweedleDecoderRoundTripTest`. It is a JUnit 4
characterization test in `core/ide` that proves the Tweedle encode→decode path
preserves AST structural identity for all `NamedUserType` declarations in a
real `.a3p` starter project, entirely headlessly.

The test does not start JavaFX, load gallery assets, render a 3D scene, exercise
drag-and-drop UI, execute code through the virtual machine, or require a
display.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Design decisions](#design-decisions)
- [Test method](#test-method)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Test resources](#test-resources)
- [Relationship to issue #478](#relationship-to-issue-478)
- [Claim boundaries](#claim-boundaries)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Scope

The test covers exactly one journey:

### Load Real Starter → Encode Each Type → Decode Tweedle → Assert Structural Equality

```text
1. Load indiaMinimum.a3p from core/ide/src/test/resources/starters/
2. Collect all NamedUserType declarations from the project
3. Build a terminals set from the project's NamedUserTypes
4. For each NamedUserType:
   a. Encode to Tweedle source via TweedleEncoderDecoder.encode(type)
   b. Assert the Tweedle source is non-null and non-empty
   c. Decode back via TweedleEncoderDecoder.decode(tweedleSource, terminals)
   d. Assert the decoded node is a NamedUserType
   e. Assert structural equality: class name, method count, method names,
      field count, constructor presence, non-empty method body
   f. Catch UnsupportedTweedleDecodeException per-type and document the gap
5. Assert at least one type completed the full round-trip
```

## Implementation status

| Surface | Location |
| --- | --- |
| `SilverThreadTweedleDecoderRoundTripTest` | `core/ide/src/test/java/org/alice/ide/SilverThreadTweedleDecoderRoundTripTest.java` |
| `indiaMinimum.a3p` test resource | `core/ide/src/test/resources/starters/indiaMinimum.a3p` |
| Maven validation | `mvn -pl core/ide -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SilverThreadTweedleDecoderRoundTripTest test` |

## Design decisions

Five deliberate decisions, informed by the existing test suite and the Decoder's
current capabilities:

1. **Compare against encoded structure, not original Java AST.**
   The Encoder renames Java types to Tweedle types (`Double` → `DecimalNumber`,
   `Integer` → `WholeNumber`, `String` → `TextString`) and may rename members
   (`rint` → `round`, `ceil` → `ceiling`). Comparing the decoded AST against
   the original pre-encode AST would produce false negatives. The test extracts
   structural properties from the encoded Tweedle source and compares them
   against the decoded AST.

2. **Pass project NamedUserTypes as terminals to the Decoder.**
   The Decoder requires terminals to resolve custom type references. Without
   terminals, a Scene type referencing a custom model type would fail resolution.
   The test passes `project.getNamedUserTypes()` as the terminals set, mirroring
   production usage.

3. **Per-type exception handling with gap documentation.**
   Real starter project types may use AST constructs unsupported by the Decoder
   (complex field initializers, resource references, advanced control flow).
   Rather than failing the entire test on the first unsupported type, the test
   catches `UnsupportedTweedleDecodeException` per-type, increments a gap
   counter, and continues. This is a characterization pattern: the test documents
   current capability while providing a clear signal for decoder expansion.

4. **Structural equality, not deep AST equality.**
   The test asserts class names, method counts, method names (set equality),
   field counts, constructor presence, and non-empty method bodies. It does not
   assert UUID equality, object identity, supertype resolution depth, field
   initializer values, or statement-level deep equality. This level catches
   structural regressions without coupling to Decoder implementation details.

5. **At-least-one success fail-safe.**
   After iterating all types, the test asserts that at least one type completed
   the full round-trip. This prevents a silent total failure where every type
   throws `UnsupportedTweedleDecodeException` — the test would still pass its
   per-type catches but fail this aggregate assertion.

## Test method

### allNamedUserTypesRoundTripThroughTweedleEncoderDecoder

| Step | API | Assertion |
| --- | --- | --- |
| Load project | `IoUtilities.readProject(File)` from classpath resource | Project is non-null. |
| Collect types | `project.getNamedUserTypes()` | At least one `NamedUserType` exists. |
| Build terminals | `new HashSet<>(project.getNamedUserTypes())` | Terminals set for Decoder resolution. |
| Encode | `TweedleEncoderDecoder.encode(type)` | Tweedle source is non-null and non-empty. |
| Decode | `TweedleEncoderDecoder.decode(tweedleSource, terminals)` | Decoded node is a `NamedUserType`. |
| Class name | Compare decoded name vs. encoded name | Names match. |
| Method count | Compare method list sizes | Counts match. |
| Method names | Compare method name sets | Set equality. |
| Field count | Compare field list sizes | Counts match. |
| Constructor | Compare constructor presence | Boolean match. |
| Non-empty body | Check at least one method body has statements | At least one non-empty body. |
| Aggregate | Check success count ≥ 1 | Fail-safe against total decoder gaps. |

## Usage

Run from the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadTweedleDecoderRoundTripTest \
  test
```

The test runs without a display server, JavaFX toolkit, or network access.

## Behavior contract

### Encoder output

The `TweedleEncoderDecoder.encode(NamedUserType)` method produces Tweedle
source code. The Encoder:

- Renames Java primitive wrappers to Tweedle types (`Double` → `DecimalNumber`,
  `Integer` → `WholeNumber`, `String` → `TextString`).
- Renames certain members (`rint` → `round`, `ceil` → `ceiling`,
  `nextIntegerFrom0ToNExclusive` → `wholeNumberFrom0ToNExclusive`).
- Organizes code sections by type (Scene, Program) using `CodeOrganizer`.
- Emits field declarations, method signatures, method bodies, and constructors.

### Decoder parsing

The `TweedleEncoderDecoder.decode(String, Set<AbstractDeclaration>)` method:

1. Parses the Tweedle source via `TweedleUnlinkedParser.parseType(String)`.
2. Links parsed Tweedle types against the provided terminals.
3. Constructs Alice AST nodes (`NamedUserType`, `UserMethod`, `UserField`,
   `NamedUserConstructor`, etc.) from the linked Tweedle AST.

If the Tweedle source contains constructs the Decoder does not support, it
throws `UnsupportedTweedleDecodeException` with a diagnostic message describing
the unsupported construct.

### Structural equality contract

After a successful round-trip, the following structural properties are preserved:

| Property | Type | Comparison |
| --- | --- | --- |
| Class name | `String` | `assertEquals` |
| Method count | `int` | `assertEquals` |
| Method names | `Set<String>` | Set equality |
| Field count | `int` | `assertEquals` |
| Constructor presence | `boolean` | `assertEquals` |
| Non-empty method body | `boolean` | At least one method has ≥1 statement |

Properties explicitly **not** compared:

| Property | Reason |
| --- | --- |
| UUIDs | Decoded nodes receive new UUIDs. |
| Object identity | Different object instances. |
| Supertype resolution | Decoder may create placeholder or linked types differently. |
| Field initializer values | Complex initializers may throw `UnsupportedTweedleDecodeException`. |
| Statement deep equality | Only count and presence are checked. |
| Field names | Count is the stable invariant for initial characterization; names may be added later. |
| Parameter types and names | Method signature depth is not yet characterized. |

## API reference

| Class | Module | Role |
| --- | --- | --- |
| `TweedleEncoderDecoder` | `core/ast` | Public codec facade. `encode(NamedUserType)` → `String`. `decode(String, terminals)` → `AbstractNode`. `decode()` declares `throws VersionNotSupportedException` (checked). |
| `Encoder` | `core/ast` | Walks Alice AST, produces Tweedle source with type renaming and code organization. |
| `Decoder` | `core/ast` | Parses Tweedle source via `TweedleUnlinkedParser`, links against terminals, produces Alice AST. |
| `UnsupportedTweedleDecodeException` | `core/ast` | Unchecked `RuntimeException` thrown by `Decoder` for AST constructs it does not yet support. |
| `VersionNotSupportedException` | `core/ast` | Checked exception declared on `decode()`. Unlikely for raw Tweedle source but must be handled or declared. |
| `NamedUserType` | `core/ast` | Alice user-defined class. Contains `UserMethod`, `UserField`, `NamedUserConstructor`. |
| `UserMethod` | `core/ast` | Method on a `NamedUserType`. Has name, return type, parameters, and `BlockStatement` body. |
| `UserField` | `core/ast` | Field on a `NamedUserType`. Has name, value type, and optional initializer. |
| `NamedUserConstructor` | `core/ast` | Constructor on a `NamedUserType`. Contains `ConstructorBlockStatement` body. |
| `IoUtilities` | `core/story-api-migration` | `readProject(File)` for loading `.a3p` archives. |
| `Project` | `core/story-api` | Top-level container. `getNamedUserTypes()` returns all user-defined types. |
| `AbstractDeclaration` | `core/ast` | Base class for terminals passed to the Decoder for type resolution. |

## Configuration

No system properties or environment variables are required beyond
`NODE_OPTIONS=--max-old-space-size=32768` for the Maven reactor build.

The test does not create temporary files. All encode/decode operations happen
in memory. The only file access is reading `indiaMinimum.a3p` from the test
classpath.

## Test resources

| Resource | Path | Source |
| --- | --- | --- |
| `indiaMinimum.a3p` | `core/ide/src/test/resources/starters/indiaMinimum.a3p` | Copied from `core/resources/src/application/resources/starter-projects/indiaMinimum.a3p`. |

This resource is a real Alice starter project. It contains multiple
`NamedUserType` declarations with real field initializers, method bodies, and
type references.

## Relationship to issue #478

This test implements RabbitHole issue #478: "Write a Tweedle decoder round-trip
silver thread test." The issue requested a test that loads a real starter
project, decodes Tweedle code, re-encodes the AST, and verifies the round-trip
preserves structure.

The test fulfills this by encoding first (AST → Tweedle), then decoding
(Tweedle → AST), and comparing the decoded AST against the encoded source
structure. This direction (encode then decode) is chosen because the Encoder is
mature and handles all production types, while the Decoder has known gaps that
the test characterizes.

## Claim boundaries

This test proves:

- `TweedleEncoderDecoder.encode()` produces non-empty Tweedle source for every
  `NamedUserType` in `indiaMinimum.a3p`.
- `TweedleEncoderDecoder.decode()` parses at least one encoded type back to a
  `NamedUserType` with preserved structural properties.
- Class names, method counts, method names, field counts, and constructor
  presence survive the encode→decode round-trip for supported types.
- Unsupported types are documented via `UnsupportedTweedleDecodeException`
  without failing the overall test.

This test does **not** prove:

| Non-claim | Reason |
| --- | --- |
| Full Tweedle language decode support | Some types may throw `UnsupportedTweedleDecodeException`. |
| `.a3p` binary archive round-trip | Owned by `SilverThreadLaunchBuildRunTest`. |
| Production save path round-trip | Owned by `SilverThreadSaveRoundTripTest`. |
| Deep AST equality | UUIDs, initializers, and statement bodies are not deeply compared. |
| VM execution of decoded types | No `ReleaseVirtualMachine` execution. |
| 3D rendering correctness | No scene graph, no display server. |
| Drag-and-drop UI | No Swing/JavaFX automation. |
| Gallery asset loading | No model resources referenced during decode. |
| Player archive (`.a3w`) round-trip | Owned by `HistoricalArchiveRoundTripCharacterizationTest`. |
| Type archive (`.a3c`) round-trip | Owned by `IoUtilitiesTest`. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Headless create → build → run → save → reopen | [Silver Thread Launch-Build-Run Test](./silver-thread-launch-build-run-test.md). |
| Production save round-trip | [Silver Thread Save Round-Trip Test](./silver-thread-save-round-trip-test.md). |
| Unit-level Tweedle encode/decode | [Decode Coverage Characterization](./decode-coverage-characterization.md). |
| Zero-argument this-method call decode | [Zero-Argument This-Method Call Decode](./zero-argument-this-method-call-decode.md). |
| Simple if-statement decode | [Simple If-Statement Decode](./simple-if-statement-decode.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](./silver-thread-status-report.md). |

## Examples

### Encoding a NamedUserType to Tweedle source

```java
TweedleEncoderDecoder codec = new TweedleEncoderDecoder();
String tweedleSource = codec.encode(namedUserType);
// tweedleSource is now Tweedle source code, e.g.:
// class MyScene extends SScene {
//   DecimalNumber speed <- 1.0;
//   void myFirstMethod() { ... }
// }
```

### Decoding Tweedle source back to an AST

`decode()` declares `throws VersionNotSupportedException` (checked). The test
method declares `throws Exception` so it propagates without an explicit catch.

```java
Set<AbstractDeclaration> terminals = new HashSet<>(project.getNamedUserTypes());
// decode() throws VersionNotSupportedException (checked)
AbstractNode decoded = codec.decode(tweedleSource, terminals);

if (decoded instanceof NamedUserType decodedType) {
    assertEquals(originalName, decodedType.getName());
}
```

### Building the terminals set from a project

```java
Project project = IoUtilities.readProject(starterFile);
Set<AbstractDeclaration> terminals = new HashSet<>(project.getNamedUserTypes());
// Pass terminals to decode() so the Decoder can resolve custom type references
```

### Handling UnsupportedTweedleDecodeException

```java
try {
    AbstractNode decoded = codec.decode(tweedleSource, terminals);
    // ... assertions on the decoded type ...
} catch (UnsupportedTweedleDecodeException e) {
    // This type uses AST constructs the Decoder does not yet support.
    // Document the gap; do not fail the overall test.
    System.out.println("Decoder gap for " + type.getName() + ": " + e.getMessage());
}
```

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| `indiaMinimum.a3p` not found | Test resource not copied. | Verify `core/ide/src/test/resources/starters/indiaMinimum.a3p` exists. |
| All types throw `UnsupportedTweedleDecodeException` | Decoder regression or terminals not provided. | Check that the terminals set is built from `project.getNamedUserTypes()` and is non-empty. |
| `ClassCastException` on decode result | Decoder returned `AbstractNode` instead of `NamedUserType`. | The Tweedle source may not declare a class. Check that `encode()` produced a valid class declaration. |
| `VersionNotSupportedException` compile error | Test method does not declare checked exception. | Add `throws Exception` to the test method signature. |
| `OutOfMemoryError` during Maven build | Insufficient heap for the reactor. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
| Method name set mismatch | Encoder renamed a method. | Compare against names extracted from the encoded Tweedle source, not the original Java AST. |
