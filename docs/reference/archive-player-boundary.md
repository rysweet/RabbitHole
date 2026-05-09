# Archive/Player Boundary

The archive/player boundary defines how JSON player archives are read when the
manifest-declared Tweedle program type either decodes cleanly or stops at a known
unsupported decoder edge.

This contract is intentionally narrow. It protects the `IoUtilities.readProject`
boundary for JSON `.a3w` player archives, the legacy resource-only image
compatibility case, and clear fail-closed behavior for unsupported archive shapes.
It does not claim full Tweedle decoding, full UI automation, rendering
validation, or grading validation.

## Contents

- [Usage](#usage)
- [Archive contracts](#archive-contracts)
- [API behavior](#api-behavior)
- [Configuration](#configuration)
- [Examples](#examples)
- [Validation](#validation)
- [Recovery readiness](#recovery-readiness)
- [Non-goals](#non-goals)

## Usage

Read player archives through the production project IO API:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
```

Use this boundary when reviewing or changing archive behavior in:

```text
core/story-api-migration/src/main/java/org/lgna/project/io/JsonProjectIo.java
core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java
```

The reader treats a JSON `.a3w` manifest name as the expected player program
type. The archive must either decode that manifest-declared program type, match
the narrow legacy image-resource compatibility shape, or fail at
`IoUtilities.readProject` with `IOException`.

## Archive contracts

### Supported JSON player archive

A supported player archive contains a JSON manifest, at least the
manifest-declared Tweedle program type for this boundary example, and optional
supported resources:

```text
version.txt
manifest.json
src/<ProgramName>.twe
resources/<resource-name>
```

The manifest identifies the archive as a player archive:

```json
{
  "metadata": {
    "fileType": "a3w"
  },
  "description": {
    "name": "ProgramWithField"
  },
  "projectStructure": {
    "sceneCameraType": "WindowCamera"
  }
}
```

If `description.name` is `ProgramWithField` and `src/ProgramWithField.twe`
decodes to the same type name, `IoUtilities.readProject(File)` returns a
`Project` with a non-null program type. Image resources declared in the same
manifest remain readable as project resources when their archive entries are
present and valid.

Model references remain manifest entries at this boundary. A decoded program
archive that also contains a model reference remains readable, but the model
reference is not exposed as a binary `Project` resource by this reader path.

### Legacy image-resource compatibility archive

The only unsupported-program compatibility readback shape is a legacy player
archive with all of these properties:

| Required property | Value |
| --- | --- |
| Manifest file type | `a3w` |
| Manifest description name | `Program` |
| Type references | Exactly one `TypeReference` named `Program` |
| Resource references | Exactly one `ImageReference` |
| Image data | The referenced image archive entry exists and reads successfully |
| Program Tweedle | The `Program` type reaches an unsupported Tweedle decode boundary |

For that exact shape, `IoUtilities.readProject(File)` returns a resource-only
`Project`. The returned project has `null` program type and one decoded
`ImageResource`. That result is compatibility readback for the image payload; it
is not a successful player-program decode.

### Fail-closed unsupported archives

Unsupported legacy player archives fail closed when the archive has no safe
legacy image-resource recovery. Covered fail-closed shapes include:

| Archive shape | Required behavior |
| --- | --- |
| `Program` extends an unresolved or unsupported parent and has no recoverable image | Throws `IOException`. |
| Unsupported `Program` plus an audio resource | Throws `IOException`; audio does not use image recovery. |
| Unsupported `Program` plus an image and an unsupported model reference | Throws `IOException`; no partial image recovery. |
| Unsupported `Program` plus an image whose data entry is missing | Throws `IOException` with the missing image entry available from the cause. |
| Unsupported `Program` plus a sibling Tweedle type and an image | Throws `IOException`; no partial recovery when extra type references exist. |

The stable message fragments for the fail-closed legacy boundary are:

```text
Unsupported legacy JSON project archive
Program Tweedle decode is unsupported
no safe legacy resource recovery applies
```

Tests should assert stable fragments instead of full-message equality.

## API behavior

The public project reader remains the API boundary:

```java
Project IoUtilities.readProject(File file)
Project IoUtilities.readProject(String path)
```

Both overloads can throw `IOException`. Callers should treat the fail-closed
legacy boundary as an archive-read failure and should not expect a partial
project unless the archive matches the exact legacy image-resource compatibility
shape.

`JsonProjectIo` records unsupported Tweedle decode failures for
manifest-declared types. For the legacy `Program` player archive path, it checks
whether safe image-resource recovery applies before deciding whether to return a
resource-only project or throw the bounded legacy `IOException`.

## Configuration

There is no runtime configuration for this behavior. It uses the existing JSON
player archive reader, Tweedle decoder, JUnit 4 tests, and Maven reactor setup.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Automation can keep the saved Node memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

`NODE_OPTIONS` is not an Alice archive reader setting.

## Examples

### Decode a supported player program

```java
File archive = temporaryFolder.newFile("manifest-type-field-resource.a3w");
writeJsonPlayerArchive(archive, "ProgramWithField",
    "class ProgramWithField { WholeNumber count; }");

Project project = IoUtilities.readProject(archive);

assertNotNull(project.getProgramType());
assertEquals("ProgramWithField", project.getProgramType().getName());
```

### Assert the legacy fail-closed boundary

```java
File archive = temporaryFolder.newFile("json-unsupported-super-program.a3w");
writeJsonPlayerArchive(archive, "Program", "class Program extends MissingSuper {}");

IOException thrown = assertThrows(IOException.class,
    () -> IoUtilities.readProject(archive));

assertTrue(thrown.getMessage().contains("Unsupported legacy JSON project archive"));
assertTrue(thrown.getMessage().contains("Program Tweedle decode is unsupported"));
assertTrue(thrown.getMessage().contains("no safe legacy resource recovery applies"));
```

### Assert the legacy image-resource compatibility readback

```java
Project project = IoUtilities.readProject(exportedImagePlayerArchive);

assertNull(project.getProgramType());
Resource resource = project.getResources().iterator().next();
assertEquals(ImageResource.class, resource.getClass());
```

Keep this assertion paired with archive-shape checks that prove the manifest has
exactly one `Program` type reference and exactly one valid image reference.

## Validation

Run commands from the repository root.

Focused archive/player boundary tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.IoUtilitiesTest \
  test
```

Primary story API migration gate:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

QA scenario and contract readiness checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-silver-thread-status-report.sh
```

These QA checks validate scenario, gated-command, workflow, and silver-thread
contracts. They are not proof of full UI automation, rendering correctness, or
grading behavior.

## Recovery readiness

Before finalizing a recovery PR for this boundary, keep the evidence tied to the
current HEAD:

```bash
git --no-pager status --short --branch
git --no-pager diff --name-status origin/develop...HEAD
gh pr view <pr-number> --json number,state,isDraft,baseRefName,headRefName,url
```

The PR is ready only when the review confirms the expected branch, an open
non-draft PR targeting `develop`, and a diff limited to the archive/player
boundary implementation plus its focused characterization tests. Do not treat
these checks as merge approval, full UI automation, rendering validation, or
grading validation.

## Non-goals

This boundary does not add broad legacy archive recovery. It does not decode
unsupported Tweedle parent types, method bodies, model references, audio resource
recovery for unsupported programs, sibling-type partial recovery, rendering
behavior, learner grading, rubric scoring, or complete player-project semantics.

Unsupported archives either match the exact legacy image-resource compatibility
shape or fail closed at `IoUtilities.readProject`.
