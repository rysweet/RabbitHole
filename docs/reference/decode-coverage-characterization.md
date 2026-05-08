# Decode Coverage Characterization

Alice decode coverage is the build contract for decode behavior expansion. It
characterizes the current behavior of the Tweedle parser, Tweedle AST decoder,
project `.a3p` archive round trip, player `.a3w` reader, type `.a3c` reader,
and resource archive readers.

The tests are characterization tests: they preserve the observed Alice 3
baseline while making decode regressions visible. They do not promise broader
Tweedle language support or a new archive format.

This work intentionally exercises production decode paths instead of isolated
mocks. Test archives may be constructed in memory, but they must flow through
the same public readers and parsers used by Alice project loading.

## Build contract and non-goals

Build the coverage as a compatibility safety net, not as a decoder redesign.

- Keep unsupported Tweedle AST constructs explicit: direct decoder calls throw
  `UnsupportedTweedleDecodeException`, while JSON archive readers fail closed
  with `IOException` when a manifest-declared expected program/type cannot be
  decoded.
- Treat unsupported player-program superclass decoding as a characterized
  fail-closed boundary. A JSON player archive with `class Program extends
  MissingSuper {}` does not return a partial project shell for normal
  manifest-declared program decode.
- Treat `version.txt` as part of Alice's normal written archive shape and the
  explicit version-check boundary. Do not require every `readProject(File)` or
  `readType(File)` characterization to exercise version checking.
- Treat archive resources as optional. Image and audio entries are required only
  when the manifest references them.
- Cover both JSON manifest-backed resources and legacy XML `resources.xml`
  resources where resource decode behavior is in scope.
- Do not test private helpers, use mocks for decode collaborators, or add
  assertions for implementation details that callers cannot observe.
- Do not require LFS payloads, historical binary fixtures, user projects, or
  broad archive rewrites for decode characterization. Prefer small synthetic
  archives written under the JUnit temporary directory.

## Coverage scope

| Decode area | Production boundary | Characterization tests |
| --- | --- | --- |
| Tweedle parser | `TweedleUnlinkedParser.parseType(String)` | `core/tweedle/src/test/java/org/alice/tweedle/unlinked/TweedleParseTest.java` |
| Tweedle AST decoder | `TweedleEncoderDecoder.decode(String)` | `core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java` |
| Tweedle resource field initializer boundary | `TweedleEncoderDecoder.decode(String)` | `core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java` |
| Project archive round trip | `IoUtilities.writeProject(File, Project)`, `IoUtilities.readProject(File)`, and structural `IoUtilities.exportProject(File, Project)` output | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |
| Player archive decode | `IoUtilities.readProject(File)` for `.a3w` files | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |
| Type archive decode | `IoUtilities.readType(File)` for `.a3c` files | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |
| Resource decode | JSON manifest-backed image and audio entries plus XML `resources.xml` archive entries | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` |

The intended coverage covers successful decode behavior and known edge behavior:

- empty and malformed Tweedle source;
- unsupported Tweedle declarations, unsupported superclasses, and unsupported
  adjacent method-call forms around the zero-argument `this.method()`
  slice;
- resource fields initialized to `null`, plus explicit fail-fast diagnostics for
  non-null resource field initializers that require archive binding context;
- same-type zero-argument `this.method()` calls decoded to Alice
  `MethodInvocation` statements in method and constructor bodies;
- missing or malformed Tweedle entries in player archives;
- JSON player archives that decode a simple Tweedle program;
- JSON player and type archives whose unsupported manifest-declared Tweedle
  fails closed with archive context;
- synthetic `.a3p` archives that are saved, reopened, edited, saved again, and
  reopened with the edited program metadata preserved;
- exported `.a3w` archives produced from the edited project, checked at stable
  archive-entry and manifest boundaries;
- JSON manifest-backed and XML resource reads;
- missing resource entries, missing resource UUIDs, unknown resource reference
  types, traversal references, and safe distinct resource-entry naming.

## Usage

Use this documentation when changing any code that reads or writes Alice project
archives, reads Tweedle source, reads JSON project manifests, reads JSON type
manifests, handles manifest-backed resources, or handles XML resource entries.

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

Documented behavior, including the zero-argument this-method slice:

| Source shape | Required characterization result |
| --- | --- |
| Empty class such as `class SyntheticType {}` | Returns a `NamedUserType` with the class name and no fields, methods, constructors, or supertype. |
| Supported `java.lang` superclass such as `extends String` | Resolves the superclass to the matching `JavaType`. |
| Unknown superclass | Throws `UnsupportedTweedleDecodeException` with the missing superclass name in the message. |
| Malformed superclass syntax | Throws `IllegalArgumentException` describing the parser boundary. |
| Supported class fields and supported method declarations | Decodes supported fields and supported `UserMethod` declarations. |
| Resource field initializer `ImageResource picture <- null` | Decodes as a resource-typed field with a `NullLiteral` initializer. |
| Non-null resource field initializers such as `ImageResource picture <- someImage` or `AudioResource sound <- sound0` | Throws `UnsupportedTweedleDecodeException` describing the resource field initializer, the fact that the initializer is non-null, the resource type, the field name, and the missing archive resource manifest/binding context. The diagnostic does not promise to include the initializer token such as `someImage`. This is a fail-fast boundary, not full resource binding support. |
| Method or constructor body expression statement `this.helper();` where `helper` is a known same-type zero-argument method | Decodes to an `ExpressionStatement` containing a `MethodInvocation` that resolves to the declared `helper` `UserMethod`; the implementation registers same-type methods before decoding bodies so declaration order does not matter. |
| Argument-bearing calls, unknown methods, non-`this` targets, implicit calls, static-style calls, object construction calls, or chained calls | Throws `UnsupportedTweedleDecodeException`; this is not general method-call support. Focused tests cover argument-bearing calls, unknown methods, and non-`this` targets; the remaining forms are documented non-goals unless a later slice routes them through this boundary. |
| Non-class declarations such as enums | Throws `UnsupportedTweedleDecodeException` describing the class-only boundary. |
| Empty source | Throws `UnsupportedTweedleDecodeException` describing the class-only boundary. |

The decoder API is intentionally narrow. It supports the currently implemented
class-declaration subset and reports unsupported Tweedle explicitly instead of
silently inventing AST nodes.

#### Resource field initializer boundary

Resource field initializer support is intentionally limited to the direct
decoder shapes that can be represented without archive resource binding context.

Supported Tweedle source:

```java
class SyntheticType {
  ImageResource picture <- null;
}
```

The decoded `NamedUserType` has one `UserField` named `picture`, the field type
is `ImageResource`, and the field initializer is a `NullLiteral`.

Unsupported Tweedle source:

```java
class SyntheticType {
  ImageResource picture <- someImage;
}
```

```java
class SyntheticType {
  AudioResource sound <- sound0;
}
```

These non-null resource initializers fail with
`UnsupportedTweedleDecodeException`. The diagnostic must identify the construct
as a Tweedle resource field initializer with a non-null value and include the
field name, the resource type, and the missing archive resource manifest or
binding context. The decoder must not silently coerce the initializer to `null`,
invent a resource object, read archive entries, resolve filenames, or perform
manifest lookup from `core/ast`.

When the same unsupported Tweedle appears inside JSON player or type archives,
the archive readers keep unsupported Tweedle explicit at the archive boundary:
direct decoder calls throw `UnsupportedTweedleDecodeException`, and
manifest-declared expected program/type reads fail closed with `IOException`
that names the expected type and the decoded/unsupported manifest-declared type
set.
Manifest-backed image and audio resource coverage remains separate from this
field-initializer binding boundary.

For the focused method-call slice, see
[Zero-Argument This-Method Call Decode Reference](./zero-argument-this-method-call-decode.md).

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
| Tweedle entry contains unsupported members or an unsupported superclass for the manifest-declared program type | Throws `IOException` with archive context, including the expected program type, decoded type names, and unsupported manifest-declared Tweedle type names when available. |
| Tweedle entry is `class Program extends MissingSuper {}` | Throws `IOException`; the normal player reader does not return a partial project shell for this unsupported manifest-declared program. |
| Manifest declares a supported Tweedle `TypeReference` and a valid image resource | Returns a project whose program type and resource identity, name, original file name, content type, and bytes are preserved. |
| Manifest references a non-`tweedle` type format | Throws `IOException` with type reference context. |
| Named manifest has no Tweedle type reference for the program | Throws `IOException` with missing type-reference context. |
| Manifest references image or audio resources with valid archive entries and the program type decodes | Returns resources with identity, name, content type, and bytes preserved. |
| Manifest references missing resource data or missing UUIDs | Throws `IOException` with resource context. |
| Manifest contains unsupported resource reference types | Ignores the unsupported references without crashing. |
| Manifest references traversal paths | Rejects the unsafe resource reference. |

#### Unsupported-superclass player program boundary

The focused player-program boundary is a synthetic JSON `.a3w` archive with a
single Tweedle program source:

```java
class Program extends MissingSuper {}
```

The archive uses the normal player-reader shape:

```text
version.txt
manifest.json
src/Program.twe
```

The manifest identifies the archive as `fileType: "a3w"`, names `Program` as
the description, includes deterministic project and world identifiers, records
scene-camera metadata such as `WindowCamera`, and points one `tweedle`
`TypeReference` at `src/Program.twe`.

Current behavior is intentionally fail-closed:

1. `IoUtilities.readProject(exportFile)` throws `IOException`.
2. The archive does not require image, audio, or historical Alice payloads.
3. The exception message identifies the manifest-declared expected program type,
   the decoded type names, and the unsupported manifest-declared Tweedle type
   names.
4. The result is not treated as completed Tweedle support and does not imply
   that unknown superclasses are resolved.

This boundary is different from malformed Tweedle syntax. Malformed syntax, for
example `class Program extends {}`, stays at the parser failure boundary and is
reported as an archive decode `IOException`. An unresolved superclass is valid
enough to reach the Tweedle AST decoder, where the decoder cannot create a
program type and the JSON player reader fails closed instead of returning a
partial project shell.

### Type `.a3c` archive decode

`IoUtilities.readType(File file)` reads Alice type archives. JSON type archives
use manifest type references and Tweedle source entries when present.

Documented behavior:

| Archive shape | Required characterization result |
| --- | --- |
| JSON type manifest references supported Tweedle source | Returns the decoded type and manifest-backed resources. |
| Tweedle source contains unsupported members | Throws `IOException` with archive context instead of returning a `TypeResourcesPair` with a `null` type. |
| Tweedle source contains an unsupported superclass | Throws `IOException` with archive context instead of returning a `TypeResourcesPair` with a `null` type. |
| Tweedle entry is missing or malformed at the parser boundary | Throws `IOException` with entry context. |
| Manifest references a non-`tweedle` type format | Throws `IOException` with type reference context. |
| JSON type manifest names one type but references a different decoded type | Throws `IOException` with both expected and decoded type context. |
| JSON type manifest has no type reference | Throws `IOException` with missing type-reference context. |
| Manifest is missing and the archive has XML `type.xml` shape | Falls back to the XML type reader. |
| Manifest is missing from a JSON-style archive | Falls back to XML selection and fails with missing `type.xml` context. |

Type archive resource coverage is still characterized at the manifest/archive
entry level, but `readType(File)` fails closed when the manifest-declared type
cannot be decoded.

### Resource decode

JSON resource decode coverage is manifest-backed archive resource reading, not
Tweedle field-initializer binding. Tests should create the manifest resource
reference and the corresponding archive entry, then read through `IoUtilities`.
XML project and type resource coverage uses the legacy `resources.xml` entry plus
the referenced resource data entries.

Documented behavior:

| Resource case | Required characterization result |
| --- | --- |
| Image resources in successful player reads or the current legacy single-image unsupported `Program` recovery path | Decode with resource identity, name, content type, and bytes preserved. |
| Audio resources in successful player reads | Decode with resource identity, name, content type, and bytes preserved; exported audio resources next to unsupported program Tweedle remain manifested, but `readProject(File)` fails closed. |
| Type archive resources attached to unsupported Tweedle | Remain manifested in the written archive; `readType(File)` fails closed when the manifest-declared type is unsupported. |
| Duplicate resource names | Write and read distinct safe archive entries. |
| Absolute or traversal-like original names | Do not leak unsafe filesystem paths into archive entries. |
| Repeated UUIDs across separate JSON resource reads | Preserve each read resource's own name and bytes without mutating earlier reads. |
| Missing entries or missing UUIDs | Fail explicitly with `IOException`. |
| Unknown resource classes in XML resource manifests | Fail with contextual `IOException`. |

### Project `.a3p` archive round trip

`IoUtilities.writeProject(File file, Project project)` writes Alice editor
project archives. `IoUtilities.readProject(File file)` reopens those archives
through the same reader selection, version metadata, manifest, XML program type,
resource, and migration-compatible decode path used by project loading.

Use the round-trip regression test
`IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`
to document saving, reopening, editing, saving again, reopening again, and
exporting Alice projects:

1. Alice has a synthetic editable project with a deterministic program type name.
2. The project is saved to a temporary `.a3p` archive.
3. The saved archive is reopened through `IoUtilities.readProject(File)`.
4. The reopened project is edited by changing observable program metadata.
5. The edited project is saved to a second `.a3p` archive.
6. The second archive is reopened through `IoUtilities.readProject(File)`.
7. The edited program metadata is still present after the second reopen.
8. The edited project can be exported to `.a3w`, and the export contains the
   stable player archive structure Alice relies on.

Documented behavior:

| Journey step | Required characterization result |
| --- | --- |
| Initial `.a3p` save | Archive includes Alice version metadata, `manifest.json`, and `programType.xml`. |
| First reopen/decode | `readProject(File)` returns a project with the original `NamedUserType` program name and scene-camera metadata. |
| Edit after reopen | The edit is applied to the reopened `Project`, not the original in-memory project, so stale-save regressions are visible. Use the existing `NamedUserType.name.setValue("EditedProgram")` pattern. |
| Second `.a3p` save | The archive is written from the edited reopened project. |
| Second reopen/decode | `readProject(File)` returns the edited program name, proving saving after reopening uses current state. |
| `.a3w` export | `exportProject(File, Project)` writes a player archive with stable manifest/source structure for the edited project. Assert `manifest.json` and the Tweedle source entry named by the manifest type reference; for the current edited-name scenario this should be `src/EditedProgram.twe` if that is the exporter output. |

The target test should use only `TemporaryFolder`, synthetic `Project` and
`NamedUserType` instances, direct zip-entry inspection, and public `IoUtilities`
methods. It must not start JavaFX, launch the IDE, automate Swing dialogs, read
user projects, or depend on LFS-backed fixtures.

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
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/tweedle,core/ast,core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tweedle.unlinked.TweedleParseTest,org.alice.serialization.tweedle.TweedleEncoderDecoderTest,org.lgna.project.io.IoUtilitiesTest \
  test
```

Focused AST decoder gate for resource initializer work:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest \
  test
```

Focused story API migration gate for player-program and project archive work:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
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

## Example: characterize resource field initializer decode

Use this pattern when changing direct Tweedle field initializer handling for
resource types. Keep the test in `TweedleEncoderDecoderTest` because the direct
decoder owns this boundary.

1. Add or update a positive `null` initializer test:

   ```java
   NamedUserType type =
       decodeUserType("class SyntheticType { ImageResource picture <- null; }");

   UserField field = type.getDeclaredFields().get(0);
   assertEquals("picture", field.getName());
   assertSame(JavaType.getInstance(ImageResource.class), field.getValueType());
   assertTrue(field.initializer.getValue() instanceof NullLiteral);
   ```

2. Add a non-null image-resource failure test:

   ```java
   UnsupportedTweedleDecodeException thrown =
       assertThrows(
           UnsupportedTweedleDecodeException.class,
           () -> coder.decode(
               "class SyntheticType { ImageResource picture <- someImage; }"));

   assertTrue(thrown.getMessage().contains("resource field initializer"));
   assertTrue(thrown.getMessage().contains("non-null"));
   assertTrue(thrown.getMessage().contains("manifest"));
   assertTrue(thrown.getMessage().contains("picture"));
   ```

3. Add the same failure shape for at least one audio resource field:

   ```java
   UnsupportedTweedleDecodeException thrown =
       assertThrows(
           UnsupportedTweedleDecodeException.class,
           () -> coder.decode(
               "class SyntheticType { AudioResource sound <- sound0; }"));

   assertTrue(thrown.getMessage().contains("AudioResource"));
   assertTrue(thrown.getMessage().contains("sound"));
   ```

These tests document that `null` resource field initializers are safe to decode
as AST literals, while non-null resource identifiers are intentionally outside
the direct decoder because no archive manifest or resource binding context is
available.

## Example: characterize unsupported player-program superclass decode

Use this pattern for the player `.a3w` unsupported-superclass boundary. The test
archive is synthetic and contains only manifest metadata plus Tweedle source; it
does not use LFS-backed Alice payloads or rewrite a historical archive.

1. Add the test to `IoUtilitiesTest`.
2. Create a temporary file named like `unsupported-superclass-program.a3w`.
3. Write `version.txt`, `manifest.json`, and `src/Program.twe`.
4. In `manifest.json`, set `fileType` to `a3w`, describe the program as
   `Program`, include stable project/world identifiers, record scene-camera
   metadata such as `WindowCamera`, and add a `tweedle` type reference for
   `src/Program.twe`.
5. Write this Tweedle source:

   ```java
   class Program extends MissingSuper {}
   ```

6. Read the archive with `IoUtilities.readProject(exportFile)`.
7. Assert it throws `IOException`.
8. Assert the message names the manifest-declared program type, decoded type
   names, and unsupported manifest-declared Tweedle type names.

The assertion documents the current incomplete decode behavior: Alice recognizes
the JSON player archive, records the unsupported Tweedle type, and fails closed
instead of returning a project shell without a decoded program type.

## Example: verify saving after reopening writes current state

Use this pattern when a change touches project archive read/write, migration, or
export code and the risk is losing edits made after a project has been reopened.

1. Add the test to `IoUtilitiesTest`.
2. Create a synthetic `Project` with a program type named `OriginalProgram`.
3. Write it to a temporary `.a3p` file with `IoUtilities.writeProject(File, Project)`.
4. Reopen that archive with `IoUtilities.readProject(File)`.
5. Change the reopened project's program type name to `EditedProgram` with
   `reopenedProject.getProgramType().name.setValue("EditedProgram")` or the
   equivalent existing local variable.
6. Write the reopened edited project to a second temporary `.a3p` file.
7. Reopen the second archive and assert the program type name is `EditedProgram`.
8. Export the edited project to a temporary `.a3w` file and assert stable archive
   structure such as `manifest.json` and the Tweedle source entry named by the
   manifest type reference. For this scenario, prefer `src/EditedProgram.twe`
   when it matches the exporter output.

This scenario proves the archive seam preserves user-visible edits across
saving, reopening, editing, saving again, reopening again, and exporting, and
that export runs from the edited project state rather than stale pre-open state.

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
- fail-closed `IOException` context when archive readers cannot decode a
  manifest-declared expected Tweedle program/type;
- exception type and message context for explicit failures;
- resource identity, name, content type, and bytes for resource decode.

Do not assert implementation details such as private helper names or temporary
entry order unless the archive format contract requires them.

### 4. Run the focused gate

Run the focused decode command first. If it passes, run the touched module's full
test command. Use the coverage gate when the change is part of a coverage update
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
| Supported Tweedle fields, supported method declarations, expressions, while loops, and returns decode through the AST decoder. | `TweedleEncoderDecoderTest` focused field, method, expression, while-loop, and return tests. |
| Resource fields initialized to `null` decode to resource-typed fields with `NullLiteral` initializers. | `TweedleEncoderDecoderTest.decodeClassWithResourceNullInitializedFieldCreatesNullLiteralInitializer` |
| Non-null image resource field initializers fail fast with unsupported resource-binding context instead of being coerced or resolved. | `TweedleEncoderDecoderTest.decodeClassWithResourceIdentifierInitializedFieldReportsUnsupportedBoundary` |
| Non-null audio resource field initializers fail fast with the same unsupported resource-binding boundary. | `TweedleEncoderDecoderTest.decodeClassWithAudioResourceIdentifierInitializedFieldReportsUnsupportedBoundary` |
| Same-type zero-argument `this.method()` calls decode to Alice `MethodInvocation` statements after same-type methods are registered before body decode. | `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallDecodeCreatesMethodInvocation`; `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallInConstructorDecodeCreatesMethodInvocation` |
| Argument-bearing calls, unknown methods, and non-`this` targets remain unsupported next to the zero-argument this-method slice. | `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall`; `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallDecodeRejectsUnknownMethod`; `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallDecodeRejectsNonThisTarget`; `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallInConstructorDecodeRejectsArgumentBearingCall`; `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallInConstructorDecodeRejectsUnknownMethod`; `TweedleEncoderDecoderTest.zeroArgumentThisMethodCallInConstructorDecodeRejectsNonThisTarget` |
| Non-class and empty Tweedle source are rejected by the AST decoder. | `TweedleEncoderDecoderTest.decodeEnumReportsOnlyClassDeclarationsSupported`; `TweedleEncoderDecoderTest.decodeEmptySourceReportsOnlyClassDeclarationsSupported` |
| Player archives with supported Tweedle decode program types through `IoUtilities.readProject(File)`. | `IoUtilitiesTest.readsSimpleJsonPlayerArchiveTweedleProgram`; `IoUtilitiesTest.jsonPlayerReaderDecodesProgramTypeWhenManifestReferencesSimpleTweedleSource` |
| Saved `.a3p` archives reopen, accept edits on the reopened project, save again, reopen again with edited program metadata, and export with stable player archive structure. | `IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported` |
| Player archive scene-camera metadata is preserved, and missing project-structure metadata defaults to window camera. | `IoUtilitiesTest.jsonPlayerReaderPreservesSceneCameraTypeFromManifestWithSimpleTweedleDecoding`; `IoUtilitiesTest.jsonPlayerReaderDefaultsMissingProjectStructureToWindowCamera` |
| Player archive missing Tweedle entries fail with entry context. | `IoUtilitiesTest.jsonPlayerReaderReportsMissingTweedleTypeEntry` |
| Player archive malformed Tweedle entries are wrapped as archive decode failures. | `IoUtilitiesTest.jsonPlayerReaderWrapsMalformedTweedleTypeEntry` |
| Non-`tweedle` player type references fail with type-reference context. | `IoUtilitiesTest.jsonProjectReaderReportsUnsupportedTypeReferenceFormat` |
| Named player manifests without a program type reference fail with missing type-reference context. | `IoUtilitiesTest.jsonPlayerReaderReportsMissingProgramTypeReferenceForNamedManifest` |
| Unsupported JSON manifest references are ignored without becoming binary project resources. | `IoUtilitiesTest.ignoresUnsupportedJsonResourceReferencesWithoutCrashing`; `IoUtilitiesTest.readsExportedPlayerArchiveModelAndGeneratedTypeReferencesWithoutBinaryResources` |
| JSON player archive with `class Program extends MissingSuper {}` fails closed instead of returning a project shell with no decoded program type. | `IoUtilitiesTest.unsupportedJsonPlayerTweedleSuperclassFailsClosed` |
| JSON player archive with supported Tweedle fields and manifest-backed image resources decodes the program and keeps resources readable. | `IoUtilitiesTest.jsonPlayerManifestTypeReadsFieldAndKeepsResourcesReadable` |
| Type archives with supported Tweedle decode types through `IoUtilities.readType(File)`. | `IoUtilitiesTest.readsSimpleJsonTypeArchiveTweedleClass` |
| JSON type manifest mismatches and missing type references fail with archive context. | `IoUtilitiesTest.jsonTypeReaderReportsManifestNameMismatchInsteadOfFallback`; `IoUtilitiesTest.jsonTypeReaderReportsMissingTypeReferenceInsteadOfReturningNull` |
| JSON type archives with non-`tweedle`, missing, or malformed type entries fail with archive context. | `IoUtilitiesTest.jsonTypeReaderReportsUnsupportedTypeReferenceFormat`; `IoUtilitiesTest.jsonTypeReaderReportsMissingTweedleTypeEntry`; `IoUtilitiesTest.jsonTypeReaderWrapsMalformedTweedleTypeEntry` |
| JSON type archive with `class SyntheticType extends MissingSuper {}` fails closed instead of returning a `TypeResourcesPair` with a `null` type. | `IoUtilitiesTest.unsupportedJsonTypeTweedleSuperclassFailsClosed` |
| Type archive resources generated next to unsupported Tweedle remain manifested in the archive, and `readType(File)` fails closed for the unsupported manifest-declared type. | `IoUtilitiesTest.jsonTypeArchiveResourceRemainsManifestedWhenUnsupportedTypeFailsClosed` |
| Missing manifests select the XML fallback boundary, while corrupt manifests do not silently fall back. | `IoUtilitiesTest.missingTypeManifestUsesXmlTypeFallback`; `IoUtilitiesTest.jsonStyleTypeArchiveWithoutManifestFailsInXmlFallback`; `IoUtilitiesTest.jsonStylePlayerArchiveWithoutManifestFailsInXmlFallback`; `IoUtilitiesTest.corruptManifestDoesNotFallBackToXmlReader`; `IoUtilitiesTest.corruptTypeManifestDoesNotFallBackToXmlReader` |
| Version checks remain isolated to `checkForFutureVersion()`. | `IoUtilitiesTest.jsonPlayerReaderReportsFutureVersion`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingVersion`; `IoUtilitiesTest.jsonTypeReaderReportsFutureVersion`; `IoUtilitiesTest.jsonTypeReaderReportsMissingVersion`; `IoUtilitiesTest.jsonTypeReaderMatchesPlayerReaderForCorruptVersion` |
| Exported player image resources remain recoverable on the current legacy single-image unsupported `Program` compatibility path. | `IoUtilitiesTest.exportedPlayerArchiveImageResourceRemainsRecoverableWhenProgramTypeIsUnsupported` |
| Exported player audio resources remain manifested in the archive, and `readProject(File)` fails closed for the unsupported manifest-declared program type. | `IoUtilitiesTest.exportedPlayerArchiveAudioResourceRemainsManifestedWhenUnsupportedProgramTypeFailsClosed` |
| Resource decode failures include meaningful archive context. | `IoUtilitiesTest.jsonPlayerReaderReportsMissingImageResourceData`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingAudioResourceData`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingImageResourceUuid`; `IoUtilitiesTest.jsonPlayerReaderReportsMissingAudioResourceUuid` |
| Resource archive entries are safe and distinct for duplicate, traversal-like, and absolute original names. | `IoUtilitiesTest.jsonPlayerExportUsesSafeDistinctResourceEntries`; `IoUtilitiesTest.jsonPlayerExportDoesNotLeakAbsoluteResourcePaths`; `IoUtilitiesTest.xmlProjectUsesSafeDistinctResourceEntries`; `IoUtilitiesTest.xmlProjectExportDoesNotLeakAbsoluteResourcePaths` |
| Repeated UUID resource reads keep each read resource independent. | `IoUtilitiesTest.jsonPlayerImageReadsWithSameUuidDoNotMutateEarlierRead`; `IoUtilitiesTest.jsonPlayerAudioReadsWithSameUuidDoNotMutateEarlierRead` |
| XML resource failures report missing data and unknown resource classes with context. | `IoUtilitiesTest.xmlProjectReaderReportsMissingResourceDataArchiveEntry`; `IoUtilitiesTest.xmlProjectReaderReportsUnknownResourceClassWithContext` |
