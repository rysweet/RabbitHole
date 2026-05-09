# Tutorial: Add Archive/Player Boundary Characterization

This tutorial walks through the focused fail-closed characterization for the
archive/player feature: a legacy JSON `.a3w` player archive can contain valid
image resources and still be unreadable when its manifest-declared program
Tweedle type is unsupported.

The goal is to protect this behavior:

```text
Resource recovery is not archive success. If the manifest-named player program
type is unsupported, IoUtilities.readProject(...) throws IOException instead of
returning a partial Project.
```

This tutorial does not add full Tweedle/player decode, full historical archive
migration, UI automation coverage, visible rendering correctness, Save
completion, grading, Sims validation, deployed installer success, or
first-lesson completion evidence.

## 1. Extend the existing test suite

Add or review the characterization in:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Use or review the committed test method named for the bounded behavior:

```text
generatedJsonPlayerArchiveWithResourceFieldInitializerProgramTypeIsRejectedWithoutPartialProgramDecode
```

The test belongs in this suite because it protects archive routing and
production readback behavior, not a private decoder helper.

## 2. Build the smallest player archive

Create a temporary `.a3w` archive with:

```text
version.txt
manifest.json
src/Program.twe
resources/picture.png
```

Use manifest metadata that selects the JSON player path:

```json
{
  "metadata": {
    "fileType": "a3w"
  },
  "description": {
    "name": "Program"
  },
  "projectStructure": {
    "sceneCameraType": "WindowCamera"
  }
}
```

Declare both the `Program` Tweedle source and the image resource in
`manifest.json`. The image bytes should be valid and deterministic so the test
proves the failure is caused by unsupported program decode, not by a corrupt
resource.

## 3. Use the unsupported resource initializer

Use a program type with an unsupported resource field initializer:

```java
class Program extends SProgram {
  ImageResource texture <- "picture.png";
}
```

This shape represents the legacy recovery trap: the archive has a recoverable
image resource, but the manifest-named program type is unsupported.

## 4. Read through IoUtilities

Read through the public production API:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

Do not instantiate private decoder helpers. Do not call lower-level resource
loading code directly. The boundary is the archive read result returned by
`IoUtilities`.

## 5. Assert fail-closed behavior

Assert that the archive read fails with stable context:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Project archive manifest names program type 'Program'"));
assertTrue(message.contains("decoded type names are"));
```

The exact generated type name can differ by test fixture, but the result must be
a checked `IOException`. The reader must not return:

- a `Project` with no decoded program type;
- a `Project` that only contains recovered resources;
- a project that silently omits an unsupported manifest-declared type.

## 6. Add a sibling-type negative neighbor

Also protect silent omission of unsupported manifest-declared sibling types. Use
a supported program that references a sibling scene type whose Tweedle source
contains the same unsupported resource initializer:

```java
class Program extends SProgram {
  SceneWithTexture scene;
}

class SceneWithTexture extends SScene {
  ImageResource texture <- "picture.png";
}
```

The expected result is still:

```text
IoUtilities.readProject(...) throws IOException
```

The diagnostic should include:

```text
Project archive contains unsupported manifest-declared Tweedle type names
```

## 7. Preserve supported neighbors

Keep existing positive JSON `.a3w` readback tests for supported program and
sibling type slices. Those tests verify the fail-closed change does not regress
supported archive behavior.

Do not describe those neighboring positives as complete player support. They
are compatibility guards for the narrow fail-closed feature.

## 8. Run validation

Initialize the Tweedle grammar submodule:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the focused archive/player evidence suite:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

For the branch-installable evidence package, run:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack archive-player-boundary verify
```

Replace `<branch>` with the branch under review.

The wrapper validates scenario metadata, initializes `tweedle-lang`, and runs
the focused Maven characterization. It is not desktop UI, rendering, grading,
Save completion, Sims, installer, or first-lesson automation.

## 9. Document the claim boundary

Use this summary when documenting or reviewing the change:

```text
The evidence proves that legacy JSON .a3w player archives with
manifest-declared image resources fail closed when the manifest-declared program
Tweedle type is unsupported.
```

Do not describe the characterization as full player support, visible rendering
proof, Save completion proof, grading proof, Sims validation, deployed
installer success, or lesson completion proof.
