# Tutorial: Add a Project IO Corpus Characterization

This tutorial walks through adding a generated `.a3p` project IO
characterization test. The same pattern applies to generated `.a3w` player and
`.a3c` type archives.

## Contents

- [Goal](#goal)
- [1. Extend the existing suite](#1-extend-the-existing-suite)
- [2. Generate the project fixture](#2-generate-the-project-fixture)
- [3. Write and inspect the archive](#3-write-and-inspect-the-archive)
- [4. Read through IoUtilities](#4-read-through-ioutilities)
- [5. Prove the round trip](#5-prove-the-round-trip)
- [6. Run validation](#6-run-validation)

## Goal

Protect this current Alice behavior:

```text
Editable .a3p project archives include manifest metadata, but the editable
project payload remains XML-backed. IoUtilities therefore reads generated .a3p
fixtures through the XML fallback path, preserving program type, scene-camera
metadata, resource bytes, and ResourceExpression bindings.
```

The test uses generated temporary archives and in-memory resource data. It does
not commit a binary `.a3p` fixture.

## 1. Extend the existing suite

Add the test to:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Use a method name that states the behavior:

```java
@Test
public void generatedProjectArchiveCharacterizesXmlFallbackProjectRoundTripWithoutExternalFixture() throws Exception {
  // fixture, archive assertions, readback assertions, and round-trip assertions
}
```

The name documents three decisions:

1. The archive is generated at test runtime.
2. The `.a3p` project uses XML fallback behavior.
3. The test proves a read/write round trip without an external binary fixture.

## 2. Generate the project fixture

Create a one-pixel image resource with a deterministic name and color:

```java
ImageResource imageResource = generatedImageResource("historical-project-texture.png", 0xFF996633);
```

Create a program type that references the resource through a
`ResourceExpression`:

```java
Project project = new Project(
    typeReferencingImageResource("GeneratedHistoricalProject", imageResource),
    Project.SceneCameraType.VRHeadset);
project.addResource(imageResource);
```

This fixture is small, deterministic, and exercises the real AST/resource
boundary instead of merely checking zip entries.

## 3. Write and inspect the archive

Write the archive with the production API:

```java
File projectArchive = temporaryFolder.newFile("generated-historical-project.a3p");
IoUtilities.writeProject(projectArchive, project);
```

Open the archive with `ZipFile` and assert the current `.a3p` contract:

```text
version.txt
manifest.json
programType.xml
resources.xml
resources/historical-project-texture.png
```

Also assert this entry is absent:

```text
src/GeneratedHistoricalProject.twe
```

That absence is intentional. It documents that generated editable project
archives currently use XML program payloads even though they include
`manifest.json`.

Decode `manifest.json` and assert:

```text
description.name = GeneratedHistoricalProject
metadata.fileType = a3p
metadata.identifier.type = World
projectStructure.sceneCameraType = VRHeadset
```

These assertions protect manifest metadata without changing reader routing.

## 4. Read through IoUtilities

Read the archive through the public project reader:

```java
Project firstRead = IoUtilities.readProject(projectArchive);
```

Assert the observable project state:

```text
program type name = GeneratedHistoricalProject
program supertype = SProgram
scene camera type = VRHeadset
resource id/name/original name/content type/bytes = generated fixture values
ResourceExpression.resource = decoded project resource object
```

The final assertion matters because Alice projects do not only store resources
in the project resource collection. Program AST nodes can point at those
resources, and readback must rebind the expression to the decoded archive
resource.

## 5. Prove the round trip

Write the decoded project again:

```java
File roundTripArchive = temporaryFolder.newFile("generated-historical-project-roundtrip.a3p");
IoUtilities.writeProject(roundTripArchive, firstRead);
```

Repeat the same archive-entry, manifest, project, resource, and
`ResourceExpression` assertions against the second archive:

```java
Project secondRead = IoUtilities.readProject(roundTripArchive);
```

The second read proves the generated fixture is not only readable once; it
preserves the current Alice archive shape across a normal write/read/write/read
cycle.

## 6. Run validation

Run the focused test from the repository root:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Run the module gate before handing off IO behavior changes:

```bash
mvn -pl core/story-api-migration -am -DfailIfNoTests=false test
```

If Maven reports missing generated Tweedle parser classes in a fresh checkout,
initialize the grammar submodule and rerun the command:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```
