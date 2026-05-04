# Decode Coverage Characterization

Alice decode coverage is the build contract for the decode-coverage expansion
feature. The feature adds characterization coverage around the current behavior
of the Tweedle parser, Tweedle AST decoder, player `.a3w` reader, type `.a3c`
reader, and resource archive readers.

The tests are characterization tests: they preserve the observed Alice 3
baseline while making decode regressions visible. They do not promise broader
Tweedle language support or a new archive format.

This lane intentionally exercises production decode paths instead of isolated
mocks. Test archives may be constructed in memory, but they must flow through
the same public readers and parsers used by Alice project loading.

## Build contract and non-goals

Build the lane as a compatibility safety net, not as a decoder redesign.

- Keep unsupported Tweedle AST constructs explicit: direct decoder calls throw
  `UnsupportedTweedleDecodeException`, while the current JSON archive readers
  preserve their documented `null` program/type behavior for unsupported
  Tweedle.
- Treat `version.txt` as part of Alice's normal written archive shape and the
  explicit version-check boundary. Do not require every `readProject(File)` or
  `readType(File)` characterization to exercise version checking.
- Treat archive resources as optional. Image and audio entries are required only
  when the manifest references them.
- Cover both JSON manifest-backed resources and legacy XML `resources.xml`
  resources where resource decode behavior is in scope.
- Do not test private helpers, use mocks for decode collaborators, or add
  assertions for implementation details that callers cannot observe.

## Coverage scope

| Decode area | Production boundary | Characterization tests |
| --- | --- | --- |
| Tweedle parser | `TweedleUnlinkedParser.parseType(String)` | `core/tweedle/src/test/java/org/alice/tweedle/unlinked/TweedleParseTest.java` |
| Tweedle AST decoder | `TweedleEncoderDecoder.decode(String)` | `core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java` |
| Player archive decode | `IoUtilities.readProject(File)` for `.a3w` files | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |
| Type archive decode | `IoUtilities.readType(File)` for `.a3c` files | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |
| Resource decode | JSON manifest-backed image and audio entries plus XML `resources.xml` archive entries | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |

The lane covers successful decode behavior and known edge behavior:

- empty and malformed Tweedle source;
- unsupported Tweedle declarations, members, methods, and superclasses;
- missing or malformed Tweedle entries in player archives;
- JSON player archives that decode a simple Tweedle program;
- JSON player and type archives whose unsupported Tweedle remains undecoded;
- JSON manifest-backed and XML resource reads;
- missing resource entries, missing resource UUIDs, unknown resource reference
  types, traversal references, and safe distinct resource-entry naming.

## Usage

Use this documentation when changing any code that reads Tweedle source, JSON
project manifests, JSON type manifests, manifest-backed resources, or XML
resource entries.

1. Identify the public decode boundary that the change affects.
2. Add or update a characterization test in the owning module.
3. Build the smallest representative archive or Tweedle snippet that reaches
   the production reader.
4. Assert the observable result or exception message that callers depend on.
5. Run the focused module gate before broader validation.

Do not add tests that call only private helpers, bypass archive readers, or
replace decode collaborators with mocks. A test can use temporary archives and
fixture bytes, but the final operation must pass through the production API.

## API behavior

### Tweedle parser

`TweedleUnlinkedParser.parseType(String source)` parses a single Tweedle type
declaration and returns a `TweedleType` when the current grammar accepts the
source.

Documented behavior:

| Source shape | Required characterization result |
| --- | --- |
| `class SThing {}` | Returns a `TweedleClass` named `SThing`. |
| Empty source | Returns `null`. |
| Malformed superclass boundary such as `class SThing extends {}` | Fails at the current parser boundary with `NullPointerException`. |
| Class with a field such as `WholeNumber count;` | Parses the field as a property on the returned Tweedle class. |

The parser tests do not normalize or improve these outcomes. They lock down the
current behavior so later parser work can make intentional, reviewed changes.

### Tweedle AST decoder

`TweedleEncoderDecoder.decode(String source)` decodes supported Tweedle class
source into Alice AST nodes.

Documented behavior:

| Source shape | Required characterization result |
| --- | --- |
| Empty class such as `class SyntheticType {}` | Returns a `NamedUserType` with the class name and no fields, methods, constructors, or supertype. |
| Supported `java.lang` superclass such as `extends String` | Resolves the superclass to the matching `JavaType`. |
| Unknown superclass | Throws `UnsupportedTweedleDecodeException` with the missing superclass name in the message. |
| Malformed superclass syntax | Throws `IllegalArgumentException` describing the parser boundary. |
| Class fields or methods | Throws `UnsupportedTweedleDecodeException` describing unsupported members. |
| Non-class declarations such as enums | Throws `UnsupportedTweedleDecodeException` describing the class-only boundary. |
| Empty source | Throws `UnsupportedTweedleDecodeException` describing the class-only boundary. |

The decoder API is intentionally narrow. It supports the currently implemented
class-declaration subset and reports unsupported Tweedle explicitly instead of
silently inventing AST nodes.

### Player `.a3w` archive decode

`IoUtilities.readProject(File file)` reads player archives through the JSON
project reader when the archive contains readable `manifest.json` metadata for a
JSON archive. For player archive characterizations, that metadata uses
`fileType: "a3w"`. Archives written by Alice also include `version.txt`, but
the version entry is read by the explicit version-check boundary rather than by
every `readProject(File)` call.

Typical Alice-written shape for a player archive with a decodable Tweedle
program:

```text
version.txt
manifest.json
src/<TypeName>.twe
```

Resource entries are optional and are required only when the manifest contains
image or audio references.

Documented behavior:

| Archive shape | Required characterization result |
| --- | --- |
| Manifest names a Tweedle program and the `src/*.twe` entry exists with supported source | Returns a project whose program type is decoded. |
| Exported or hand-built JSON player archive includes scene-camera metadata | Returns the decoded project with the manifest scene-camera type. |
| Manifest references a missing Tweedle entry | Throws `IOException` that includes the missing entry path. |
| Tweedle entry is malformed at the parser boundary | Throws `IOException` that includes `Unable to decode Tweedle type entry` and the entry path. |
| Tweedle entry contains unsupported members | Returns a project with a `null` program type for the unsupported decode, preserving current player-reader behavior. |
| Manifest references a non-`tweedle` type format | Throws `IOException` with type reference context. |
| Manifest has no Tweedle type reference | Returns a project with a `null` program type and default scene-camera handling. |
| Manifest references image or audio resources with valid archive entries | Returns resources with identity, name, content type, and bytes preserved. |
| Manifest references missing resource data or missing UUIDs | Throws `IOException` with resource context. |
| Manifest contains unsupported resource reference types | Ignores the unsupported references without crashing. |
| Manifest references traversal paths | Rejects the unsafe resource reference. |

### Type `.a3c` archive decode

`IoUtilities.readType(File file)` reads Alice type archives. JSON type archives
use manifest type references and Tweedle source entries when present.

Documented behavior:

| Archive shape | Required characterization result |
| --- | --- |
| JSON type manifest references supported Tweedle source | Returns the decoded type and manifest-backed resources. |
| Tweedle source contains unsupported members | Returns a `TypeResourcesPair` with `null` type while preserving decoded resources. |
| Tweedle source contains an unsupported superclass | Returns a `TypeResourcesPair` with `null` type while preserving decoded resources. |
| Tweedle entry is missing or malformed at the parser boundary | Throws `IOException` with entry context. |
| Manifest references a non-`tweedle` type format | Throws `IOException` with type reference context. |
| JSON type manifest names one type but references a different decoded type | Throws `IOException` with both expected and decoded type context. |
| JSON type manifest has no type reference | Throws `IOException` with missing type-reference context. |
| Manifest is missing and the archive has XML `type.xml` shape | Falls back to the XML type reader. |
| Manifest is missing from a JSON-style archive | Falls back to XML selection and fails with missing `type.xml` context. |

The type reader preserves resource decode coverage even when Tweedle type decode
cannot produce an AST type.

### Resource decode

JSON resource decode coverage is manifest-backed. Tests should create the
manifest resource reference and the corresponding archive entry, then read
through `IoUtilities`. XML project and type resource coverage uses the legacy
`resources.xml` entry plus the referenced resource data entries.

Documented behavior:

| Resource case | Required characterization result |
| --- | --- |
| Image resources in player archives | Decode with resource identity, name, content type, and bytes preserved. |
| Audio resources in player archives | Decode with resource identity, name, content type, and bytes preserved. |
| Type archive resources attached to unsupported Tweedle | Decode independently of the unsupported type. |
| Duplicate resource names | Write and read distinct safe archive entries. |
| Absolute or traversal-like original names | Do not leak unsafe filesystem paths into archive entries. |
| Repeated UUIDs across separate JSON resource reads | Preserve each read resource's own name and bytes without mutating earlier reads. |
| Missing entries or missing UUIDs | Fail explicitly with `IOException`. |
| Unknown resource classes in XML resource manifests | Fail with contextual `IOException`. |

## Configuration

Decode coverage has no Alice runtime configuration. It uses the repository's
existing Maven and JUnit configuration.

For broad Maven validation from a fresh checkout or worktree, initialize the
Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
```

Focused reactor test runs should include the Surefire flag that allows upstream
modules without the named test class to participate in `-am` builds:

```bash
-Dsurefire.failIfNoSpecifiedTests=false
```

Coverage reporting uses the existing no-Sims JaCoCo profile:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify
python3 scripts/summarize-jacoco-coverage.py --output coverage-summary.md --min-aggregate-line-percent 8.0
```

Automation that runs Node-based repository tooling can keep the saved memory
setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

This setting is not an Alice decode runtime option; it is an automation
environment preference.

## Validation commands

Run commands from the repository root.

Focused decode characterization:

```bash
mvn -pl core/tweedle,core/ast,core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tweedle.unlinked.TweedleParseTest,org.alice.serialization.tweedle.TweedleEncoderDecoderTest,org.lgna.project.io.IoUtilitiesTest \
  test
```

Module-appropriate gates:

```bash
mvn -pl core/tweedle -am -DfailIfNoTests=false test
mvn -pl core/ast -am -DfailIfNoTests=false test
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test
```

Coverage gate:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify
python3 scripts/summarize-jacoco-coverage.py --output coverage-summary.md --min-aggregate-line-percent 8.0
```

## Example: add a player archive decode characterization

Use this pattern when a decode bug or edge case crosses the player archive
reader.

1. Add the test to `IoUtilitiesTest`.
2. Create a temporary `.a3w` file.
3. Write `manifest.json`, the relevant `src/*.twe` or `resources/*` entries,
   and `version.txt` when exercising version-check behavior or Alice's normal
   written archive shape.
4. Call `IoUtilities.readProject(exportFile)`.
5. Assert the decoded project state or the `IOException` message.

Example scenario:

```java
@Test
public void jsonPlayerReaderReportsMissingTweedleTypeEntry() throws Exception {
  TypeReference typeReference =
      new TypeReference("Program", "src/MissingProgram.twe", "tweedle");
  File exportFile = temporaryFolder.newFile("missing-tweedle-entry.a3w");
  writePlayerArchiveManifestOnly(exportFile, typeReference);

  IOException thrown =
      assertThrows(IOException.class, () -> IoUtilities.readProject(exportFile));

  assertTrue(thrown.getMessage().contains(typeReference.file));
}
```

The archive is synthetic, but the decode path is real because the assertion is
made after `IoUtilities.readProject(File)` selects and runs the production
reader.

## Tutorial: characterize a new decode edge

This tutorial adds coverage for a new unsupported Tweedle construct without
changing production behavior.

### 1. Choose the owning boundary

Use the boundary that receives the input in production:

| Input | Test location |
| --- | --- |
| Raw Tweedle parser behavior | `TweedleParseTest` in `core/tweedle` |
| Tweedle-to-AST decoder behavior | `TweedleEncoderDecoderTest` in `core/ast` |
| Player archive behavior | `IoUtilitiesTest` in `core/story-api-migration` |
| Type archive behavior | `IoUtilitiesTest` in `core/story-api-migration` |
| Resource manifest or XML behavior | `IoUtilitiesTest` in `core/story-api-migration` |

### 2. Build the smallest real input

For raw parser or decoder tests, use the shortest Tweedle source that reaches
the edge:

```java
class SyntheticType { WholeNumber count; }
```

For archive tests, write the smallest Alice-shaped archive around that source:

```text
version.txt
manifest.json
src/SyntheticType.twe
```

### 3. Assert current behavior

Characterization assertions should be direct and observable:

- decoded type name;
- resolved superclass;
- `null` program or type when current archive readers leave unsupported Tweedle
  undecoded;
- exception type and message context for explicit failures;
- resource identity, name, content type, and bytes for resource decode.

Do not assert implementation details such as private helper names or temporary
entry order unless the archive format contract requires them.

### 4. Run the focused gate

Run the focused decode command first. If it passes, run the touched module's full
test command. Use the coverage gate when the change is part of a coverage lane
or pull request.

## Contract-to-test map

| Contract | Test |
| --- | --- |
| Empty Tweedle parser source returns the current parser result. | `TweedleParseTest.emptySourceShouldReturnNullType` |
| Malformed Tweedle superclass stays documented at the current parser failure boundary. | `TweedleParseTest.malformedSuperclassShouldFailAtCurrentParserBoundary` |
| Tweedle fields remain parser-visible as properties. | `TweedleParseTest.classWithFieldShouldHaveProperty` |
| Empty class decode creates an AST user type. | `TweedleEncoderDecoderTest.decodeEmptyClassCreatesNamedUserType` |
| Supported Java superclass decode resolves to `JavaType`. | `TweedleEncoderDecoderTest.decodeSupportedJavaLangSuperclassResolvesJavaType` |
| Unknown Tweedle superclass reports unsupported decode context. | `TweedleEncoderDecoderTest.decodeUnknownSuperclassReportsUnsupportedTweedle` |
| Malformed Tweedle superclass reports parser decode context. | `TweedleEncoderDecoderTest.decodeMalformedSuperclassReportsMalformedTweedle` |
| Tweedle members and methods report unsupported decode context. | `TweedleEncoderDecoderTest.decodeClassWithFieldReportsUnsupportedMembers`; `TweedleEncoderDecoderTest.decodeClassWithMethodReportsUnsupportedMembers` |
| Non-class and empty Tweedle source are rejected by the AST decoder. | `TweedleEncoderDecoderTest.decodeEnumReportsOnlyClassDeclarationsSupported`; `TweedleEncoderDecoderTest.decodeEmptySourceReportsOnlyClassDeclarationsSupported` |
| Player archives with supported Tweedle decode program types through `IoUtilities.readProject(File)`. | `IoUtilitiesTest.readsSimpleJsonPlayerArchiveTweedleProgram`; `IoUtilitiesTest.jsonPlayerReaderDecodesProgramTypeWhenManifestReferencesSimpleTweedleSource` |
| Player archive scene-camera metadata is preserved, and missing project-structure metadata defaults to window camera. | `IoUtilitiesTest.jsonPlayerReaderPreservesSceneCameraTypeFromManifestWithSimpleTweedleDecoding`; `IoUtilitiesTest.jsonPlayerReaderDefaultsMissingProjectStructureToWindowCamera` |
| Player archive missing Tweedle entries fail with entry context. | `IoUtilitiesTest.jsonPlayerReaderReportsMissingTweedleTypeEntry` |
| Player archive malformed Tweedle entries are wrapped as archive decode failures. | `IoUtilitiesTest.jsonPlayerReaderWrapsMalformedTweedleTypeEntry` |
| Non-`tweedle` player type references fail with type-reference context. | `IoUtilitiesTest.jsonProjectReaderReportsUnsupportedTypeReferenceFormat` |
| Unsupported JSON manifest references are ignored without becoming binary project resources. | `IoUtilitiesTest.ignoresUnsupportedJsonResourceReferencesWithoutCrashing`; `IoUtilitiesTest.readsExportedPlayerArchiveModelAndGeneratedTypeReferencesWithoutBinaryResources` |
| Unsupported player Tweedle members remain undecoded. | `IoUtilitiesTest.unsupportedJsonPlayerTweedleConstructsRemainUndecoded` |
| Type archives with supported Tweedle decode types through `IoUtilities.readType(File)`. | `IoUtilitiesTest.readsSimpleJsonTypeArchiveTweedleClass` |
| JSON type manifest mismatches and missing type references fail with archive context. | `IoUtilitiesTest.jsonTypeReaderReportsManifestNameMismatchInsteadOfFallback`; `IoUtilitiesTest.jsonTypeReaderReportsMissingTypeReferenceInsteadOfReturningNull` |
| JSON type archives with non-`tweedle`, missing, or malformed type entries fail with archive context. | `IoUtilitiesTest.jsonTypeReaderReportsUnsupportedTypeReferenceFormat`; `IoUtilitiesTest.jsonTypeReaderReportsMissingTweedleTypeEntry`; `IoUtilitiesTest.jsonTypeReaderWrapsMalformedTweedleTypeEntry` |
| Unsupported type Tweedle members and superclasses remain undecoded while resources still read. | `IoUtilitiesTest.unsupportedJsonTypeTweedleConstructsRemainUndecoded`; `IoUtilitiesTest.unsupportedJsonTypeTweedleSuperclassRemainsUndecoded`; `IoUtilitiesTest.readsJsonTypeArchiveResourcesWhenUnsupportedTweedleRemainsUndecoded` |
| Missing manifests select the XML fallback boundary, while corrupt manifests do not silently fall back. | `IoUtilitiesTest.missingTypeManifestUsesXmlTypeFallback`; `IoUtilitiesTest.jsonStyleTypeArchiveWithoutManifestFailsInXmlFallback`; `IoUtilitiesTest.jsonStylePlayerArchiveWithoutManifestFailsInXmlFallback`; `IoUtilitiesTest.corruptManifestDoesNotFallBackToXmlReader`; `IoUtilitiesTest.corruptTypeManifestDoesNotFallBackToXmlReader` |
| Version checks remain isolated to `checkForFutureVersion()`. | `IoUtilitiesTest.jsonPlayerReaderReportsFutureVersion`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingVersion`; `IoUtilitiesTest.jsonTypeReaderReportsFutureVersion`; `IoUtilitiesTest.jsonTypeReaderReportsMissingVersion`; `IoUtilitiesTest.jsonTypeReaderMatchesPlayerReaderForCorruptVersion` |
| Player image and audio resources decode through manifest-backed entries. | `IoUtilitiesTest.readsExportedPlayerArchiveImageResource`; `IoUtilitiesTest.readsExportedPlayerArchiveAudioResource` |
| Resource decode failures include meaningful archive context. | `IoUtilitiesTest.jsonPlayerReaderReportsMissingImageResourceData`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingAudioResourceData`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingImageResourceUuid`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingAudioResourceUuid` |
| Resource archive entries are safe and distinct for duplicate, traversal-like, and absolute original names. | `IoUtilitiesTest.jsonPlayerExportUsesSafeDistinctResourceEntries`; `IoUtilitiesTest.jsonPlayerExportDoesNotLeakAbsoluteResourcePaths`; `IoUtilitiesTest.xmlProjectUsesSafeDistinctResourceEntries`; `IoUtilitiesTest.xmlProjectExportDoesNotLeakAbsoluteResourcePaths` |
| Repeated UUID resource reads keep each read resource independent. | `IoUtilitiesTest.jsonPlayerImageReadsWithSameUuidDoNotMutateEarlierRead`; `IoUtilitiesTest.jsonPlayerAudioReadsWithSameUuidDoNotMutateEarlierRead` |
| XML resource failures report missing data and unknown resource classes with context. | `IoUtilitiesTest.xmlProjectReaderReportsMissingResourceDataArchiveEntry`; `IoUtilitiesTest.xmlProjectReaderReportsUnknownResourceClassWithContext` |
