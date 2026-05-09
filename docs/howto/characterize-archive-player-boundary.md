# Characterize the Archive/Player Boundary

Use this guide to add or review focused tests for the JSON player archive
boundary in `core/story-api-migration`.

This guide covers the current narrow behavior: supported `.a3w` player archives
decode their manifest-declared program type, the exact legacy image-resource
shape remains resource-readable, and nearby unsupported legacy shapes fail
closed with `IOException`.

## Prerequisites

Work in the project IO test package:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/
```

Prefer extending:

```text
IoUtilitiesTest.java
```

Use generated temporary archives. Do not add checked-in `.a3w`, image, audio, or
model payloads for this boundary.

## Choose the scenario

Start with the archive shape the change affects:

| Scenario | Expected assertion |
| --- | --- |
| Supported `description.name` and matching Tweedle type | `IoUtilities.readProject` returns a project with that program type. |
| Supported program type plus image reference | Program type and image resource both read back. |
| Unsupported legacy `Program` plus exactly one valid image reference | Project reads back with `null` program type and one `ImageResource`. |
| Unsupported legacy `Program` with audio, model, sibling type, missing image data, unsafe image path, or no recoverable image | `IoUtilities.readProject` throws `IOException`. |

Do not broaden the test into UI, rendering, grading, or full Tweedle language
coverage.

## Build the smallest archive

Use a JSON `.a3w` manifest and a single Tweedle source entry:

```text
version.txt
manifest.json
src/Program.twe
```

For a fail-closed legacy fixture, keep the unsupported source minimal:

```java
class Program extends MissingSuper {}
```

For a supported fixture, use a small decodable source:

```java
class ProgramWithField {
  WholeNumber count;
}
```

When testing the image-resource compatibility shape, add only one image
reference and its matching archive entry:

```text
resources/picture.png
```

Adding an audio reference, model reference, sibling type reference, or missing
image entry moves the fixture out of the characterized compatibility shape and
should fail closed.

Keep the image entry path safe. Exported fixtures should use a relative entry
under `resources/`, such as `resources/picture.png`. The reader safety check
accepts safe relative archive entries; `resources/` is the exporter convention,
not a required reader prefix. The following paths are outside the compatibility
shape and should fail at the public read boundary:

```text
../evil.png
resources/../Program.twe
resources/images/../../manifest.json
/tmp/picture.png
C:\temp\picture.png
```

## Assert through the public API

Read through production reader selection:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
```

Do not instantiate `JsonProjectIo` directly unless the test is specifically
about a lower-level helper. The public `IoUtilities` boundary is part of the
contract.

For supported readback, assert:

```java
assertNotNull(project.getProgramType());
assertEquals("ProgramWithField", project.getProgramType().getName());
```

For the legacy image-resource compatibility path, assert:

```java
assertNull(project.getProgramType());
Resource resource = onlyResource(project);
assertEquals(ImageResource.class, resource.getClass());
```

For fail-closed paths, assert stable message fragments:

```java
IOException thrown = assertThrows(IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
assertTrue(thrown.getMessage().contains("Unsupported legacy JSON project archive"));
assertTrue(thrown.getMessage().contains("Program Tweedle decode is unsupported"));
assertTrue(thrown.getMessage().contains("no safe legacy resource recovery applies"));
```

If missing image data causes the failure, assert the missing entry on the cause
rather than replacing the bounded top-level message:

```java
assertNotNull(thrown.getCause());
assertTrue(thrown.getCause().getMessage().contains(imageReference.file));
```

If an unsafe resource path causes unsupported legacy recovery to fail, keep the
bounded top-level legacy assertions and assert the unsafe entry on the cause:

```java
ImageReference imageReference = imageReference(UUID.randomUUID(), "evil.png", "png");
imageReference.file = "../evil.png";

// Build the fixture as an unsupported legacy Program archive with exactly one
// Program TypeReference and this one ImageReference.

IOException thrown = assertThrows(IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
assertTrue(thrown.getMessage().contains("Unsupported legacy JSON project archive"));
assertTrue(thrown.getMessage().contains("Program Tweedle decode is unsupported"));
assertTrue(thrown.getMessage().contains("no safe legacy resource recovery applies"));
assertNotNull(thrown.getCause());
assertTrue(thrown.getCause().getMessage().contains(imageReference.file));
```

Direct supported-resource read failures may surface the unsafe entry in the
top-level message. Unsupported legacy recovery wraps resource-read failures as
the cause under the stable legacy error.

## Keep the boundary conservative

Do not describe resource-only legacy readback as a decoded player program. A
`Project` with `null` program type is compatibility evidence for one recovered
image payload only.

Do not add silent fallback parsing. Unsupported legacy archives must either match
the exact image-resource compatibility shape or surface the checked
`IOException` at `IoUtilities.readProject`.

Do not sanitize unsafe resource paths into valid archive entries during readback.
Path validation is part of deciding whether safe legacy image-resource recovery
applies.

## Run the focused checks

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
  test
```

Then run the module gate:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

For workflow readiness evidence, run the scenario and contract checks without
claiming UI rendering or grading validation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
```

For Gadugi evidence wiring, validate and run the archive/player boundary
evidence scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s archive-player-boundary-evidence \
  --timeout 300000
NODE_OPTIONS=--max-old-space-size=32768 bash \
  qa/outside-in/alice-desktop/tests/test-gadugi-archive-player-boundary-contract.sh
```

For full Gadugi scenario configuration and contract details, see
[Gadugi archive/player boundary evidence scenario](../reference/gadugi-archive-player-boundary-evidence.md).
