# Archive/Player Boundary

This reference defines the feature boundary for legacy JSON `.a3w` player
archives that include manifest-declared image resources while the
manifest-declared Tweedle program type is unsupported.

The feature is fail-closed archive readback. `IoUtilities.readProject(...)`
must reject the unsupported player shape instead of returning a partial
`Project`, a project with no decoded program type, or a resource-only success.
The image resource can still be present and well-formed; resource recovery is
not enough to make the player archive readable.

## Contents

- [Usage](#usage)
- [Covered archive shape](#covered-archive-shape)
- [Required behavior](#required-behavior)
- [API and implementation boundary](#api-and-implementation-boundary)
- [Validation](#validation)
- [QA packaging](#qa-packaging)
- [Diagnostics](#diagnostics)
- [Compatibility rules](#compatibility-rules)
- [Non-goals](#non-goals)

## Usage

Use this boundary when changing JSON player archive readback in
`core/story-api-migration`, especially code that handles resources after a
manifest-declared Tweedle type fails to decode.

The public entry point is:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
```

Tests for this boundary live in:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

The tests use generated temporary archives and production `IoUtilities`
routing. Do not add checked-in binary `.a3w` payloads for this boundary.

## Covered archive shape

The selected feature covers a JSON player archive with:

```text
version.txt
manifest.json
src/Program.twe
resources/picture.png
```

The manifest identifies a player archive and names the expected program type:

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

The same manifest declares the Tweedle program type and the image resource.
The fail-closed case is when the declared program type is outside the supported
Tweedle subset, for example a resource field initializer:

```java
class Program extends SProgram {
  ImageResource texture <- "picture.png";
}
```

The archive can contain valid image bytes and a valid image resource manifest
entry. The expected result is still failure because the program type is not
decoded.

## Required behavior

For the selected legacy/resource-recovery boundary:

1. `IoUtilities.readProject(File)` throws `IOException` when the
   manifest-declared program type is unsupported.
2. The reader does not return a `Project` with a null or missing program type.
3. The reader does not treat recovered image resources as archive success.
4. Diagnostics identify the manifest-named program type and decoded sibling
   context when applicable.
5. Existing supported JSON `.a3w` readback paths remain covered by neighboring
   tests; they are regression protection, not a broad player claim.

Sibling-type failures follow the same fail-closed rule. If a manifest-declared
sibling type is unsupported, the archive read fails instead of silently omitting
that type from the returned project.

## API and implementation boundary

The behavior is owned by the JSON project read path behind:

```java
Project IoUtilities.readProject(File file)
```

`JsonProjectIo` may decode supported manifest-declared types and resources, but
it must reject the archive when the manifest-named program type is missing from
the decoded type set because that type was unsupported. The relevant regression
shape is resource recovery after unsupported Tweedle, not general archive
import.

The feature does not change the public archive API. It tightens the result for
an unsupported legacy player shape from fail-open partial recovery to checked
failure.

## Validation

Run focused validation from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

The key fail-closed characterization methods are:

```text
generatedWorldArchiveWithUnsupportedResourceExpressionIsRejectedWithoutPartialProgramDecode
generatedJsonPlayerArchiveWithResourceFieldInitializerProgramTypeIsRejectedWithoutPartialProgramDecode
generatedJsonPlayerArchiveWithResourceFieldInitializerSiblingTypeIsRejectedWithoutSilentOmission
```

The branch-installable QA wrapper packages the same bounded evidence:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack archive-player-boundary verify
```

The wrapper validates scenario metadata, initializes `tweedle-lang`, and runs
the focused Maven characterization. It is validation packaging; it is not the
archive/player feature itself.

## QA packaging

The outside-in scenario that packages this evidence is:

```text
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
```

The scenario is a gated command smoke for the focused archive characterization
suite. It does not drive the Alice desktop, sample rendered pixels, grade
learner work, or complete a lesson.

Validate the scenario catalog with:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

## Diagnostics

Tests assert stable message fragments rather than full-message equality.
Expected diagnostics include:

| Case | Diagnostic content |
| --- | --- |
| Unsupported manifest-named program type | `Project archive manifest names program type '<type>'` and decoded type context such as `decoded type names are [...]`. |
| Unsupported manifest-declared sibling type | `Project archive contains unsupported manifest-declared Tweedle type names [...]`. |
| Resource initializer boundary | The affected generated type name and a checked `IOException`; no returned partial project. |

Diagnostics must not include raw archive payloads, full Tweedle source bodies,
credentials, stack traces, or user-specific filesystem data.

## Compatibility rules

1. Keep fixtures generated at test runtime; do not add binary archive payloads.
2. Keep reads routed through `IoUtilities.readProject(...)`.
3. Preserve supported JSON `.a3w` decode/readback paths already covered by
   neighboring tests.
4. Reject unsupported manifest-declared Tweedle instead of silently omitting it.
5. Treat image resource recovery as subordinate to program type decode success.

## Non-goals

This boundary does not claim:

- full Tweedle/player decode;
- full historical archive migration;
- broad JSON `.a3w`, `.a3c`, or `.a3p` archive support;
- UI automation coverage;
- visible rendering correctness;
- Save completion;
- grading correctness;
- Sims validation;
- deployed installer success;
- first-lesson completion;
- successful project readback for unsupported legacy player shapes.
