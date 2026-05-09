# Characterize Project IO Corpus Behavior

Use this guide to add or review deterministic, LFS-free characterization tests
for Alice project/archive IO behavior in `core/story-api-migration`.

If the generated fixture shape becomes representative modernization evidence,
also follow the
[modernization corpus manifest reference](../reference/modernization-corpus-manifest.md)
and update `docs/reference/modernization-corpus-manifest.json`.
The manifest documents expected generated fixture paths only; it does not allow
checking in `.a3p`, `.a3w`, `.a3c`, or media payloads.

## Contents

- [Prerequisites](#prerequisites)
- [Choose the archive seam](#choose-the-archive-seam)
- [Build a deterministic fixture](#build-a-deterministic-fixture)
- [Assert archive entries and routing](#assert-archive-entries-and-routing)
- [Assert production readback](#assert-production-readback)
- [Maintain the canonical save/reopen/edit chain](#maintain-the-canonical-savereopenedit-chain)
- [Characterize fail-fast JSON player reads](#characterize-fail-fast-json-player-reads)
- [Update representative corpus evidence](#update-representative-corpus-evidence)
- [Run the focused tests](#run-the-focused-tests)

## Prerequisites

Work in the project IO test package:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/
```

Prefer extending:

```text
HistoricalArchiveRoundTripCharacterizationTest.java
IoUtilitiesTest.java
```

Use JUnit 4 and `TemporaryFolder`. Do not commit `.a3p`, `.a3w`, `.a3c`, image,
or audio binaries for this collection of test archives unless a reviewed binary
corpus policy explicitly allows them.

Initialize the Tweedle grammar submodule before broad Maven validation from a
fresh checkout or worktree:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Choose the archive seam

Start with the production boundary the change affects:

| Behavior | Test target |
| --- | --- |
| Generated editable project archive shape | `HistoricalArchiveRoundTripCharacterizationTest` with `IoUtilities.writeProject` and `IoUtilities.readProject`. |
| Generated player export archive shape | `HistoricalArchiveRoundTripCharacterizationTest` with `IoUtilities.exportProject` and `IoUtilities.readProject`. |
| Generated type archive shape | `HistoricalArchiveRoundTripCharacterizationTest` with `IoUtilities.writeType` and `IoUtilities.readType`. |
| Reader selection, corrupt manifests, missing entries, version checks, unsafe resources | `IoUtilitiesTest`. |
| New migration behavior | Add IO seam characterization first; do not start with a broad `ProjectMigrationManager` refactor. |

Use generated fixture names that describe the protected scenario:

```text
generated-historical-project.a3p
generated-historical-project-roundtrip.a3p
generated-historical-world.a3w
generated-historical-type.a3c
```

## Build a deterministic fixture

Use small in-memory project objects and tiny generated resource bytes.

For image-resource coverage, generate a one-pixel image in memory and construct
an `ImageResource` from it:

```java
BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
image.setRGB(0, 0, 0xFF996633);
ImageResource resource = new ImageResource(image, "historical-project-texture.png", "png");
```

Use a generated `NamedUserType` with `SProgram` as its supertype when the archive
should behave like a world/program fixture. Add a `ResourceExpression` when the
test needs to prove resource rebinding after readback.

## Assert archive entries and routing

Open the generated archive with `ZipFile` and assert the entries that define the
current contract.

For resource-bearing `.a3p` project archives:

```text
version.txt
manifest.json
programType.xml
resources.xml
resources/<resource-name>
```

Also assert that `src/<ProgramType>.twe` is absent. That absence documents the
current XML fallback boundary for editable project archives.

Generated `.a3p` archives without resources should omit `resources.xml` and
resource data entries.

For `.a3w` player archives:

```text
version.txt
manifest.json
src/<ProgramType>.twe
```

Assert manifest metadata, scene-camera type, the `SceneGraphLibrary`
prerequisite, and the Tweedle type reference.

When a `.a3w` fixture includes resource-expression constructs, document the
current decode boundary explicitly: simple program source can round trip, while
unsupported resource-expression source currently leaves the program type
undecoded even though binary resource data reads back.

For a manifest-declared type/resource boundary, keep the fixture small:

```text
version.txt
manifest.json
src/<ProgramType>.twe
resources/<resource-name>
```

Use a `manifest.json` that includes both a Tweedle `TypeReference` and a valid
resource reference. If the Tweedle source contains an unsupported member, assert
that `IoUtilities.readProject` returns no decoded program type while the resource
identity, name, original file name, content type, and bytes are still readable.
Do not describe that case as a full player archive program/type decode.

For resource-bearing `.a3c` type archives:

```text
version.txt
type.xml
resources.xml
resources/<resource-name>
```

For `.a3c` fixtures without `ResourceExpression`-backed resources, assert that
resource metadata and data entries are absent.

Assert that generated XML fallback type fixtures do not contain `manifest.json`.

## Assert production readback

After inspecting archive entries, read through the public production API:

```java
Project project = IoUtilities.readProject(projectArchive);
TypeResourcesPair pair = IoUtilities.readType(typeArchive);
```

Do not instantiate `JsonProjectIo` or `XmlProjectIo` directly unless the test is
explicitly about those lower-level boundaries. `IoUtilities` reader selection is
part of the behavior being characterized.

For resource-bearing fixtures, assert:

- resource UUID;
- resource name;
- original file name;
- content type;
- bytes;
- AST `ResourceExpression` binding to the decoded resource object when the
  archive contains a resource expression.

For JSON/player archives with unsupported Tweedle, assert the resource values
directly on the returned project resources and assert that the program type is
`null`. That pairing is the current honest boundary: resources are readable, but
the unsupported manifest-declared Tweedle program is not decoded into an
editable Alice program type.

For round-trip coverage, write the decoded object to a second archive and repeat
the same archive-entry and readback assertions.

## Maintain the canonical save/reopen/edit chain

Use `IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`
for the repository-owned archive IO seam that proves one editable project can be
saved, reopened, edited, saved again, reopened again, and exported. Keep this as
the only canonical test for that journey.

The test should continue to:

1. Create a synthetic `Project` with a `NamedUserType` program type.
2. Save the original project with `IoUtilities.writeProject`.
3. Reopen the original archive with `IoUtilities.readProject`.
4. Edit project-owned data after reopen, using `NamedUserType.name.setValue(...)`.
5. Save the edited project with `IoUtilities.writeProject`.
6. Reopen the edited archive with `IoUtilities.readProject`.
7. Assert the edited program type name survived the second reopen.
8. Inspect stable archive structure, including readable `manifest.json`,
   project/export file type metadata, `programType.xml` in the edited `.a3p`, and
   `src/<EditedProgram>.twe` in the exported `.a3w`.

Do not add desktop setup to this test. It must not launch Alice desktop, click
the Save menu, drive `JFileChooser`, use AWT Robot, depend on Croquet Save
actions, or claim full desktop Save completion. If desktop Save behavior needs
coverage, use the separate Save-menu proof lane and document its narrower UI
claim independently.

## Characterize fail-fast JSON player reads

When a `.a3w` player archive has a JSON manifest with a program name, read it as
a named program archive. The archive must either decode that named program type
or fail fast with `IOException`.

Use `IoUtilitiesTest` for this project archive reading behavior:

| Scenario | Expected behavior |
| --- | --- |
| Manifest has `description.name`, but no matching Tweedle type reference | `IoUtilities.readProject` throws `IOException` that names the manifest program and the missing type reference. |
| Manifest has `description.name`, but the referenced Tweedle source decodes to a different program type | `IoUtilities.readProject` throws `IOException` that names both the manifest program and decoded type. |

Do not characterize these cases as successful reads with a missing or null
program type. Production callers already use the public `IoUtilities.readProject`
contract, which declares `IOException` and `VersionNotSupportedException`, so the
safe production behavior is to propagate the checked failure rather than return a
partially decoded `Project`.

## Update representative corpus evidence

When the test adds a new representative generated archive shape, update the
modernization corpus manifest entry for that shape. The manifest entry should
name the generated fixture path, describe the Alice archive behavior, and list
the generated-fixture expectations protected by the test.

Use the dedicated guide for the manifest update:

```text
docs/howto/maintain-modernization-corpus-manifest.md
```

Then regenerate the scorecard:

```bash
python3 scripts/generate-modernization-scorecard.py \
  --output docs/reference/modernization-scorecard.md
```

The manifest remains representative evidence only. It is not full historical
archive coverage, and the fixture paths are not instructions to add checked-in
binary Alice archives.

## Run the focused tests

Run the focused historical archive suite first:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

If the change touches `IoUtilitiesTest`, include it explicitly:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
  test
```

Before handing off a code change that touches the module, run:

```bash
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test
```

The characterization is complete when the test uses generated fixtures, reaches
the production IO API, and asserts the observable archive contract rather than
only proving that helper methods can run.
