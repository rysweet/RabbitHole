# Project IO Corpus Characterization

Project IO corpus characterization is the compatibility safety net for Alice
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
- [API reference](#api-reference)
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

The project IO corpus feature is centered on these existing suites:

| Test suite | Purpose |
| --- | --- |
| `HistoricalArchiveRoundTripCharacterizationTest` | Generated historical archive coverage for `.a3p`, `.a3w`, and `.a3c` files, including XML fallback and round-trip behavior. |
| `IoUtilitiesTest` | Focused reader/writer edge coverage for JSON manifests, Tweedle decode boundaries, resource failures, version checks, and safe archive entries. |

Prefer extending one of these suites over adding a new synthetic test class. A
new test belongs here only when it exercises a real Alice archive boundary
through `IoUtilities`, `JsonProjectIo`, or `XmlProjectIo` selection.

## Coverage scope

Project IO corpus characterization covers observable archive behavior:

| File shape | Primary behavior protected |
| --- | --- |
| `.a3p` project archive | Alice-written project archives contain a project manifest plus XML program payloads, include resource payloads when resources exist, and are read through the XML fallback path. |
| `.a3w` player archive | Exported player archives contain JSON manifest metadata, Tweedle source entries, and manifest-backed resource entries. |
| `.a3c` type archive | Generated type archives preserve XML type payloads, include resource payloads when resource expressions exist, and use XML fallback readback. |

This project archive reading coverage also protects current limitations. For
example, a `.a3w` program whose Tweedle source contains resource-expression
constructs remains at the currently documented decode boundary, while the binary
resource data still reads back.

This is not a migration-manager refactor effort. `ProjectMigrationManager`
refactors require characterization at the relevant IO seam first.

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
simple player archive round trip. Simple generated Tweedle source decodes back
to a program type, but generated source containing the current resource
expression shape remains undecoded; the archive still preserves and reads back
the referenced binary resource.

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

`IoUtilities` owns the public extension constants used by project IO tests and
desktop save/export operations:

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

## Configuration

There is no Alice runtime configuration for project IO corpus characterization.
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

Focused project IO corpus characterization:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Focused `IoUtilities` reader/writer edge coverage:

```bash
mvn -pl core/story-api-migration -am \
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
