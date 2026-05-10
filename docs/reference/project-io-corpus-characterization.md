# Project Archive Corpus Characterization

Project archive corpus characterization is the compatibility safety net for Alice
archive reader and writer behavior in `core/story-api-migration`. It documents
the current generated-archive shape for project, player, and type files without
committing binary corpus fixtures.

The feature uses deterministic JUnit fixtures that create small archives in a
temporary folder, inspect the zip entries, read the archives through production
`IoUtilities` APIs, and write/read them again when round-trip behavior is part
of the contract.

## Contents

- [Usage](#usage)
- [Coverage scope](#coverage-scope)
- [Archive contracts](#archive-contracts)
- [Canonical save/reopen/edit chain](#canonical-savereopenedit-chain)
- [API reference](#api-reference)
- [Archive security protections](#archive-security-protections)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Compatibility rules](#compatibility-rules)

## Usage

Use this testing approach when a change touches Alice project/archive IO behavior in
`core/story-api-migration`, especially reader selection, manifest handling,
resource serialization, archive entry naming, or Tweedle/XML project payload
boundaries.

The canonical tests live in:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/
```

The project archive corpus feature is centered on these existing suites:

| Test suite | Purpose |
| --- | --- |
| `HistoricalArchiveRoundTripCharacterizationTest` | Generated historical archive coverage for `.a3p`, `.a3w`, and `.a3c` files, including XML fallback and round-trip behavior. |
| `IoUtilitiesTest` | Focused reader/writer edge coverage for JSON manifests, Tweedle decode boundaries, resource failures, version checks, safe archive entries, and the canonical save -> reopen -> edit -> save/reopen archive seam chain. |

Prefer extending one of these suites over adding a new synthetic test class. A
new test belongs here only when it exercises a real Alice archive boundary
through `IoUtilities`, `JsonProjectIo`, or `XmlProjectIo` selection.

## Coverage scope

Project archive corpus characterization covers observable archive behavior:

| File shape | Primary behavior protected |
| --- | --- |
| `.a3p` project archive | Alice-written project archives contain a project manifest plus XML program payloads, include resource payloads when resources exist, and are read through the XML fallback path. |
| `.a3w` player archive | Exported player archives contain JSON manifest metadata, Tweedle source entries, and manifest-backed resource entries. |
| `.a3c` type archive | Generated type archives preserve XML type payloads, include resource payloads when resource expressions exist, and use XML fallback readback. |

This project archive reading coverage also protects current limitations. For
example, a `.a3w` program whose Tweedle source contains resource-expression
constructs remains at the currently documented decode boundary. Resource-only
readback is limited to the exact legacy image-resource compatibility shape;
nearby unsupported player archive shapes fail at the public read boundary.
example, `.a3w` programs whose Tweedle source contains unsupported
resource-expression constructs remain at the currently documented decode
boundary. Non-legacy generated player archives fail fast there; resource payloads
are still archive-shape evidence, and only the legacy `Program` resource-recovery
path can return a resource-only project.

This is not a migration-manager refactor effort. `ProjectMigrationManager`
refactors require characterization at the relevant IO seam first.

The save/reopen/edit chain is intentionally scoped to repository-owned archive IO
APIs. It does not launch Alice desktop, click the Save menu, control a save
dialog, automate Swing/AWT, or prove full desktop Save completion. Desktop Save
menu proof artifacts remain separate from this archive seam.

## Archive contracts

### Project `.a3p` XML fallback archive

`IoUtilities.writeProject(File, Project, DataSource...)` writes the current Alice
project archive shape.

A generated resource-bearing project archive includes:

```text
version.txt
manifest.json
programType.xml
resources.xml
resources/<resource-name>
```

Generated project archives without resources omit `resources.xml` and
`resources/<resource-name>` entries.

The `manifest.json` metadata identifies the archive as an Alice project:

| Manifest field | Required behavior |
| --- | --- |
| `metadata.fileType` | `a3p` |
| `metadata.identifier.type` | `World` |
| `description.name` | Matches the generated program type name. |
| `projectStructure.sceneCameraType` | Preserves the project scene-camera type. |

The `.a3p` reader selection is intentionally characterized as XML fallback even
when `manifest.json` exists. `IoUtilities` routes JSON archives only for readable
JSON file types `a3w` and `a3c`; `a3p` archives continue through `XmlProjectIo`.

The generated project archive does not contain:

```text
src/<ProgramType>.twe
```

Readback through `IoUtilities.readProject(File)` preserves:

- the project program type name;
- the `SProgram` supertype relationship;
- the scene-camera type;
- the single generated image resource's id, name, original file name, content
  type, and bytes;
- `ResourceExpression` rebinding to the decoded resource object in the project
  resource collection.

Round-trip characterization writes the read project to a second `.a3p` archive
and repeats the same archive-entry and readback assertions.

## Canonical save/reopen/edit chain

`IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`
is the single canonical characterization for editable project archives that are
saved, reopened, edited, saved again, reopened again, and exported through
repository-owned IO seams.

The chain uses only production archive APIs:

| Step | API or object | Required behavior |
| --- | --- | --- |
| Create fixture | `new Project(programType("OriginalProgram"), Project.SceneCameraType.WindowCamera)` | Builds a synthetic, in-memory Alice project with no checked-in binary fixture. |
| Save original archive | `IoUtilities.writeProject(originalProjectFile, project)` | Writes an editable `.a3p` project archive under the test temporary directory. |
| Reopen original archive | `IoUtilities.readProject(originalProjectFile)` | Returns a `Project` whose program type is present and still named `OriginalProgram`. |
| Edit reopened project | `NamedUserType.name.setValue("EditedProgram")` | Mutates project-owned AST data after the first reopen. |
| Save edited archive | `IoUtilities.writeProject(editedProjectFile, reopenedProject)` | Writes a second editable `.a3p` archive from the reopened, edited project. |
| Reopen edited archive | `IoUtilities.readProject(editedProjectFile)` | Returns a `Project` whose program type is present and named `EditedProgram`. |
| Inspect edited archive | `ZipFile` plus `manifest.json` | Confirms the edited archive manifest names `EditedProgram`, uses `a3p`, and still contains `programType.xml`. |
| Export edited project | `IoUtilities.exportProject(exportFile, editedProject)` | Writes a `.a3w` player archive from the edited project without using desktop UI. |
| Inspect export archive | `ZipFile` plus `manifest.json` | Confirms the export manifest names `EditedProgram`, uses `a3w`, and references `src/EditedProgram.twe`. |

The edit assertion must happen after the second `IoUtilities.readProject` call.
A file-exists or non-empty archive assertion is not sufficient because it would
miss stale-save regressions where Alice writes the pre-edit project state.

Keep exactly one canonical test for this chain in `IoUtilitiesTest`. Related
tests may cover neighboring archive shapes, resources, corrupt manifests, or
decoder boundaries, but they should not duplicate the save -> reopen -> edit ->
save/reopen journey under a different name unless the production seam changes.

### Player `.a3w` manifest archive

`IoUtilities.exportProject(File, Project, DataSource...)` writes player archives
with JSON manifest metadata and Tweedle source.

A generated simple player archive includes:

```text
version.txt
manifest.json
src/<ProgramType>.twe
```

The manifest preserves:

| Manifest field | Required behavior |
| --- | --- |
| `metadata.fileType` | `a3w` |
| `metadata.identifier.type` | `World` |
| `description.name` | Matches the exported program type name. |
| `projectStructure.sceneCameraType` | Preserves the exported scene-camera type. |
| `prerequisites` | Includes the `SceneGraphLibrary` prerequisite. |
| type reference | Points at `src/<ProgramType>.twe` with format `tweedle`. |

Readback through `IoUtilities.readProject(File)` decodes simple generated
Tweedle program source into a project program type and preserves scene-camera
metadata.

When an exported `.a3w` manifest references image data, the archive contains:

```text
resources/<resource-name>
```

The manifest image reference preserves the resource UUID, name, PNG format,
entry path, and 1.0-by-1.0 generated dimensions for the deterministic one-pixel
fixture image.

Resource-bearing `.a3w` characterization is intentionally narrower than the
simple player archive round trip. Supported generated Tweedle source decodes
back to a program type and can read manifest-backed resources. Unsupported
generated source fails at the JSON/player read boundary for non-legacy archive
names; the raw manifest and zip entries still prove the referenced binary
resource was written, but they do not prove successful project readback.

#### Manifest-declared Tweedle boundary with resources

A JSON/player archive can declare a Tweedle program type and manifest-backed
resources in the same `manifest.json`. The reader treats those as two observable
boundaries:

| Manifest entry | Current readback behavior |
| --- | --- |
| `TypeReference` with format `tweedle` and supported `src/<Program>.twe` source | `IoUtilities.readProject(File)` returns a project with a decoded program type. |
| `TypeReference` with format `tweedle` and unsupported Tweedle members in `src/<Program>.twe` | Generated named archives fail with `IOException` instead of returning a partial project whose program type is `null`. The legacy player-resource recovery path is narrower: a `.a3w` archive named `Program` with exactly one `Program` type reference and exactly one recovered image resource can still return a resource-only project with no program type. |
| Valid image resource reference with matching archive data and supported Tweedle source | Resource identity, name, original file name, content type, and bytes remain readable through `IoUtilities.readProject(File)`. |

This boundary keeps player/export resource compatibility honest. Tests may
assert resource readback for manifest-declared resources, but they must not infer
that an unsupported Tweedle `TypeReference` has produced an editable Alice program
type or a complete player-project decode. Tests that expect fail-fast behavior use
non-legacy generated archive names so they do not exercise the compatibility-only
resource recovery path.

When unsupported Tweedle causes an archive read to fail, tests may still inspect
the raw manifest and zip entries to prove resources were written into the archive.
That is archive-shape evidence, not successful project readback.

#### Current Tweedle decoder status

The current archive tests cover the important fail-fast edges around the partial
Tweedle decoder. Covered boundaries include:

- missing, malformed, unnamed, or unsupported manifest-declared Tweedle type
  entries for player (`.a3w`) and type (`.a3c`) archives;
- non-legacy manifest names that do not decode to the required player program
  type;
- unresolved parent types;
- method-bearing and constructor-bearing program and sibling types;
- complex, resource-expression, and null field initializers;
- corrupt manifests that must not fall back to the XML reader; and
- safe resource entry names and manifest resource references.

The decoder itself is still intentionally narrow. It decodes simple Tweedle class
declarations, supported superclass/type names, primitive field values, and only
the explicitly documented method/constructor body slices. It does not broadly
decode method bodies, constructor bodies, broad null semantics, or the full
Tweedle language.

For the narrow generated JSON `.a3c` constructor assignment slice, see
[JSON `.a3c` Constructor Assignment Characterization](./json-a3c-constructor-assignment-characterization.md).

The next larger implementation beyond focused slices should start with a decoder
design/spec before more one-off boundary tests. That design should spell out the
supported Tweedle subset, how null values map into Alice AST nodes, how method and
constructor bodies map to statements and expressions, how sibling type references
are resolved, and which archive failures should remain clear `IOException`s at
the `IoUtilities` boundary. It should also decide whether the legacy
resource-only player recovery path stays as-is, becomes more explicit, or is
retired behind new compatibility tests.

For the focused legacy player boundary, see
[Archive/Player Boundary](./archive-player-boundary.md).

### Type `.a3c` XML fallback archive

`IoUtilities.writeType(File, NamedUserType, DataSource...)` writes type archives
through the current readable writer.

A generated resource-bearing type archive includes:

```text
version.txt
type.xml
resources.xml
resources/<resource-name>
```

Generated type archives without resource expressions omit resource metadata and
data entries.

The generated `.a3c` XML fallback fixture intentionally contains no
`manifest.json`.

Readback through `IoUtilities.readType(File)` preserves:

- the type/resources pair;
- the user type name;
- the `SProgram` supertype relationship used by the generated fixture type;
- the single generated image resource's id, name, original file name, content
  type, and bytes;
- `ResourceExpression` rebinding to the decoded resource object.

Round-trip characterization writes the read type to a second `.a3c` archive and
repeats the same archive-entry and readback assertions.

## API reference

### File extensions

`IoUtilities` owns the public extension constants used by project archive tests
and desktop save/export operations:

```java
IoUtilities.PROJECT_EXTENSION // "a3p"
IoUtilities.EXPORT_EXTENSION  // "a3w"
IoUtilities.TYPE_EXTENSION    // "a3c"
```

### Project read/write APIs

```java
Project IoUtilities.readProject(File file)
Project IoUtilities.readProject(String path)
void IoUtilities.writeProject(File file, Project project, DataSource... dataSources)
void IoUtilities.writeProject(OutputStream os, Project project, DataSource... dataSources)
void IoUtilities.exportProject(File file, Project project, DataSource... dataSources)
```

Use `writeProject` for editable `.a3p` project archives. Use `exportProject` for
player `.a3w` archives. Both APIs write through production code and create parent
directories when writing to a `File`.

Both `readProject` overloads declare `IOException` and
`VersionNotSupportedException`. Named JSON player archives whose manifest names a
program type must either decode that program type or fail fast with `IOException`;
returning a `Project` with a missing or null program type is not valid readback
behavior. Existing production callers such as `ProjectCodeGenerator` already
declare and propagate the same checked exceptions through the public
`IoUtilities.readProject` boundary.

#### Missing or mismatched JSON player program types

For `.a3w` player archives, `description.name` in `manifest.json` identifies the
program type that readback must produce. The reader treats that manifest name as
part of the archive contract:

| Archive state | Required readback behavior |
| --- | --- |
| Manifest names a program, but no matching Tweedle type reference exists | `IoUtilities.readProject` throws `IOException` with context for the manifest program and missing type reference. |
| Manifest names a program, but the referenced Tweedle source decodes to a different type name | `IoUtilities.readProject` throws `IOException` with context for both names. |
| Manifest names a program, but the referenced Tweedle source extends an unresolved legacy parent type | `IoUtilities.readProject` throws `IOException` at the project read boundary instead of returning a partial project whose program type is missing. |
| Manifest names a program and the referenced Tweedle source decodes to the same type name | `IoUtilities.readProject` returns a `Project` with that program type. |

This fail-fast behavior is production-compatible because the public read API is
already checked-exception based. A caller that can read a project archive can
already handle or propagate `IOException`; returning a partially decoded
`Project` with a null program type would hide archive corruption and make later
call sites fail farther from the project archive reading boundary.

### Type read/write APIs

```java
TypeResourcesPair IoUtilities.readType(File file)
TypeResourcesPair IoUtilities.readType(ZipFile zipFile)
void IoUtilities.writeType(File file, NamedUserType type, DataSource... dataSources)
```

Use these APIs for `.a3c` type archive characterization. Tests should assert
both the decoded type and the associated resource collection when resources are
part of the behavior under test.

### Reader routing

`IoUtilities` selects the archive reader from `manifest.json`:

| Manifest state | Reader behavior |
| --- | --- |
| Readable manifest with `metadata.fileType` equal to `a3w` | `JsonProjectIo.reader(...)` |
| Readable manifest with `metadata.fileType` equal to `a3c` | `JsonProjectIo.reader(...)` |
| Missing manifest | `XmlProjectIo.reader(...)` |
| Readable manifest with `metadata.fileType` equal to `a3p` | `XmlProjectIo.reader(...)` |
| Corrupt manifest | Fails with manifest-read context instead of silently falling back. |

This routing is an observable compatibility contract. A change that routes
`.a3p` archives through JSON must update the archive contract, reader behavior,
and characterization tests together.

## Archive security protections

All archive readers and writers enforce defense-in-depth protections. These are
part of the characterization contract and must not be relaxed when adding new
corpus coverage.

### XXE protection

`XmlProjectIo.readArchiveXml()` disables DOCTYPE declarations and external
entity resolution via `DocumentBuilderFactory` feature flags before parsing any
XML entry. This prevents XXE injection through crafted `.a3p` or `.a3c` archives.
`IoUtilitiesTest` includes a negative characterization (`externalEntityResourcesXml`)
that constructs an archive with an external-entity payload and asserts that the
reader rejects it.

### Entry safety

`ResourceExportNames.isSafeRelativeEntryName(String)` validates every archive
entry name before read or write processing. It rejects absolute paths, backslash
separators, `..` parent traversal, `.` self-reference, and Windows drive
prefixes. `assertZipEntryNamesDoNotLeakLocalPaths` in `IoUtilitiesTest` asserts
that no generated archive entry leaks local file system paths.

`ResourceExportNames.isResourceEntryName(String)` allowlists resource entries to
`resources/` and `resourcesN/` prefixes. `isSourceEntryName(String)` allowlists
source entries to `src/` prefixes. Both guards are applied in `XmlProjectIo` and
`JsonProjectIo` read paths to skip unexpected entries.

### Resource leak prevention

Manifest, version, resource, and type stream reads in both `XmlProjectIo` and
`JsonProjectIo` use try-with-resources to prevent file descriptor exhaustion.
Archive read failures release resources cleanly at the IO boundary.

### Info-leak limits

Error messages use contextual summaries (file path, entry name, failure reason)
without dumping full manifest payloads, archive contents, or resource bytes.
`JsonProjectIo` truncates unsupported Tweedle decode reasons to 512 characters.

## Configuration

There is no Alice runtime configuration for project archive corpus characterization.
The feature uses the repository's existing Maven, JUnit 4, and temporary-folder
test setup.

Fresh checkouts and worktrees must initialize the Tweedle grammar submodule
before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Validation commands

Run commands from the repository root.

Focused project archive corpus characterization:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Focused `IoUtilities` reader/writer edge coverage:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
  test
```

Affected module gate:

```bash
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test
```

## Examples

### Characterize generated `.a3p` project behavior

```text
Given a generated project named GeneratedHistoricalProject
And the project contains a one-pixel PNG ImageResource
And the project program type contains a ResourceExpression for that resource
When IoUtilities.writeProject writes generated-historical-project.a3p
Then the archive contains version.txt, manifest.json, programType.xml,
     resources.xml, and resources/historical-project-texture.png
And the manifest metadata fileType is a3p
And there is no src/GeneratedHistoricalProject.twe entry
When IoUtilities.readProject reads the archive
Then the program type, scene-camera type, resource bytes, and ResourceExpression
     binding are preserved
When the read project is written again
Then the second archive preserves the same observable contract
```

### Characterize the canonical save/reopen/edit chain

```text
Given a synthetic project whose program type is named OriginalProgram
When IoUtilities.writeProject writes original-program.a3p
And IoUtilities.readProject reopens original-program.a3p
And the reopened NamedUserType is renamed to EditedProgram
And IoUtilities.writeProject writes edited-program.a3p from the reopened project
And IoUtilities.readProject reopens edited-program.a3p
Then the reopened edited project has program type EditedProgram
And edited-program.a3p still has coherent project manifest metadata and
    programType.xml structure
When IoUtilities.exportProject writes edited-program.a3w
Then the export manifest names EditedProgram
And the export archive contains src/EditedProgram.twe
```

This example is archive IO seam coverage only. It is not evidence that the
desktop Save menu, Save dialog, Croquet action path, or full UI automation path
completed.

### Characterize generated `.a3w` player behavior

```text
Given a generated project named GeneratedHistoricalProgram
When IoUtilities.exportProject writes generated-historical-world.a3w
Then the archive contains version.txt, manifest.json, and
     src/GeneratedHistoricalProgram.twe
And the manifest fileType is a3w
And the manifest type reference points at the Tweedle source
When IoUtilities.readProject reads the archive
Then the simple Tweedle program type decodes and scene-camera metadata is
     preserved
```

### Characterize supported manifest-declared `.a3w` type/resource readback

```text
Given a JSON/player archive named manifest-type-resource.a3w
And manifest.json declares description.name ProgramWithResource
And manifest.json includes a tweedle TypeReference to
    src/ProgramWithResource.twe
And that Tweedle entry contains a supported simple class declaration
And manifest.json includes an image resource reference to
    resources/picture.png
When IoUtilities.readProject reads the archive
Then the returned Project has a decoded program type
And the image resource id, name, original file name, content type, and bytes are
    preserved
```

This is the supported-source resource readback case. Unsupported Tweedle source in
non-legacy generated archives fails at the JSON/player read boundary instead of
returning a partial project with a missing program type. Resource entries can still
be checked directly in the manifest and zip file when a test is documenting archive
shape rather than successful readback.

### Characterize unsupported manifest-declared `.a3w` Tweedle fail-fast

```text
Given a JSON/player archive named generated-json-player-method-boundary.a3w
And manifest.json declares description.name GeneratedProgramWithMethodBoundary
And manifest.json includes a tweedle TypeReference to
    src/GeneratedProgramWithMethodBoundary.twe
And that Tweedle entry contains an unsupported method declaration
When IoUtilities.readProject reads the archive
Then reading fails with IOException
And the message names the expected program type and decoded type names
```

Keep the legacy player-resource recovery case separate from generated fail-fast
tests. That compatibility path is limited to a `.a3w` manifest named `Program`
whose unsupported `Program` Tweedle source still has exactly one recovered image
resource; it may return a resource-only project with no program type.

### Characterize generated `.a3c` type behavior

```text
Given a generated type named GeneratedHistoricalType
And the type references a generated one-pixel PNG ImageResource
When IoUtilities.writeType writes generated-historical-type.a3c
Then the archive contains version.txt, type.xml, resources.xml, and resource data
And the archive does not contain manifest.json
When IoUtilities.readType reads the archive
Then the type, resource bytes, and ResourceExpression binding are preserved
When the read type is written again
Then the second archive preserves the same observable contract
```

## Compatibility rules

1. Generated fixtures stay LFS-free: create archives and tiny resource bytes at
   test runtime instead of committing binary `.a3p`, `.a3w`, or `.a3c` files.
2. Tests exercise production IO boundaries. Do not replace project readers,
   writers, manifest codecs, or resource codecs with mocks.
3. Archive assertions focus on stable, observable entries and metadata.
4. `.a3p` project archives preserve XML fallback routing until a documented and
   tested format change intentionally replaces it.
5. `.a3w` player archives preserve JSON manifest and Tweedle-source routing.
6. `.a3c` type archives preserve current XML fallback behavior for generated
   historical fixtures.
7. Resource assertions include identity, original name, content type, bytes, and
   expression rebinding when the behavior crosses AST/resource boundaries.
8. Current limitations are documented as assertions rather than hidden behind
   broad fallbacks.
9. `ProjectMigrationManager` refactors require characterization at the relevant
   IO seam first.
10. Missing or mismatched manifest-named JSON player program types fail fast with
    `IOException`; `IoUtilitiesTest` covers both the missing type-reference and
    mismatched program-name cases.
11. Manifest-declared JSON player resources remain readable with unsupported
    Tweedle only for the exact legacy image-resource compatibility shape; other
    unsupported JSON player shapes fail at the public read boundary instead of
    returning partial projects.
11. The legacy JSON/player resource-recovery path is limited to the compatibility
    archive named `Program` whose unsupported `Program` Tweedle source has exactly
    one recovered image resource; it may return a resource-only project with no
    program type. Non-legacy generated named player archives with unsupported
    Tweedle fail fast with `IOException` instead of returning readable resources
    with a null program type.
