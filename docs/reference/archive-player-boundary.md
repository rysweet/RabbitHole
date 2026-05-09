# Archive/Player Boundary

The archive/player boundary defines how JSON player archives are read when the
manifest-declared Tweedle program type either decodes cleanly or stops at a known
unsupported decoder edge.

This contract is intentionally narrow. It protects the `IoUtilities.readProject`
boundary for JSON `.a3w` player archives, the exact legacy image-resource
compatibility case, and clear fail-closed behavior for unsupported archive
shapes. It does not claim full Tweedle decoding, full UI automation, rendering
validation, grading validation, or broad legacy archive recovery.

## Contents

- [Usage](#usage)
- [Archive contracts](#archive-contracts)
- [Resource path safety](#resource-path-safety)
- [API behavior](#api-behavior)
- [Validation](#validation)
- [QA packaging](#qa-packaging)
- [Diagnostics](#diagnostics)
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
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

The reader treats a JSON `.a3w` manifest name as the expected player program
type. The archive must either decode that manifest-declared program type, match
the exact legacy image-resource compatibility shape, or fail at
`IoUtilities.readProject` with `IOException`.

The tests use generated temporary archives and production `IoUtilities` routing.
Do not add checked-in binary `.a3w` payloads for this boundary.

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
| Image data | The referenced image archive entry uses a safe relative archive path, exists, and reads successfully |
| Program Tweedle | The `Program` type reaches an unsupported Tweedle decode boundary |

For that exact shape, `IoUtilities.readProject(File)` returns a resource-only
`Project`. The returned project has `null` program type and one decoded
`ImageResource`. That result is compatibility readback for the image payload; it
is not a successful player-program decode.

### Fail-closed unsupported archives

Unsupported player archives fail closed when the archive has no safe legacy
image-resource recovery. Covered fail-closed shapes include:

| Archive shape | Required behavior |
| --- | --- |
| Unsupported manifest-named generated program with an image-resource expression | Throws `IOException`; no partial program readback. |
| Unsupported manifest-named generated program with a resource field initializer | Throws `IOException`; image bytes can be present without making the archive readable. |
| Unsupported manifest-declared sibling type | Throws `IOException`; no silent sibling omission. |
| `Program` extends an unresolved or unsupported parent and has no recoverable image | Throws `IOException`. |
| Unsupported `Program` plus an audio resource | Throws `IOException`; audio does not use image recovery. |
| Unsupported `Program` plus an image and an unsupported model reference | Throws `IOException`; no partial image recovery. |
| Unsupported `Program` plus an image whose data entry is missing | Throws `IOException` with the missing image entry available from the cause. |
| Unsupported `Program` plus a sibling Tweedle type and an image | Throws `IOException`; no partial recovery when extra type references exist. |
| Unsupported `Program` plus an image reference with an absolute, drive-letter, traversal, or normalized-escaping path | Throws `IOException`; unsafe archive paths never enter compatibility recovery. |

The exact legacy image-resource compatibility case is the only unsupported
Tweedle path that may return a resource-only project. All neighboring
unsupported shapes fail at the public read boundary.

## Resource path safety

Archive resource entries are untrusted input. Alice exporters write resource
payloads under `resources/`, but the JSON player reader's safety check is about
archive-entry safety, not a required namespace prefix. The reader accepts only
non-empty relative entry names with safe path segments.

Exported image references normally point at entries like:

```text
resources/picture.png
```

The reader also accepts another safe relative archive entry if a legacy archive
manifest already names one. It rejects paths that can escape or ambiguously
address archive content, including:

| Rejected path shape | Example |
| --- | --- |
| Parent traversal | `../evil.png` |
| Normalized escape | `resources/images/../../manifest.json` |
| Absolute POSIX path | `/tmp/picture.png` |
| Windows drive-letter path | `C:\temp\picture.png` |
| Empty or current-directory path | `.` |

An unsafe path is an archive-read failure. The reader must not rewrite it to a
nearby safe-looking entry, skip it, extract it to the filesystem, or return a
resource-only project from it.

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

The safe recovery check includes manifest shape, resource kind, resource count,
and safe-entry validation. If any check fails, the public API reports
`IOException` instead of returning a partial project.

## Validation

Run commands from the repository root.

Focused archive/player boundary tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

When reviewing the legacy compatibility helper tests directly, run the
`IoUtilitiesTest` suite as an additional local check. Keep the wrapper, scenario,
and direct Maven evidence centered on `HistoricalArchiveRoundTripCharacterizationTest`.

Primary story API migration gate:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Key fail-closed characterization methods include:

```text
generatedWorldArchiveWithUnsupportedResourceExpressionIsRejectedWithoutPartialProgramDecode
generatedJsonPlayerArchiveWithResourceFieldInitializerProgramTypeIsRejectedWithoutPartialProgramDecode
generatedJsonPlayerArchiveWithResourceFieldInitializerSiblingTypeIsRejectedWithoutSilentOmission
unsupportedLegacyProgramJsonArchiveWithAudioResourceDoesNotUseImageRecovery
unsupportedLegacyProgramJsonArchiveWithImageAndUnsupportedResourceDoesNotPartiallyRecover
unsupportedLegacyProgramJsonArchiveWithUnsafeImagePathFailsClosed
```

The exact compatibility characterization is:

```text
exportedPlayerArchiveImageResourceRemainsRecoverableWhenProgramTypeIsUnsupported
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

## QA packaging

The branch-installable QA wrapper packages the same bounded evidence:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack archive-player-boundary verify
```

Replace `<branch>` with the branch under review.

The wrapper validates scenario metadata, initializes `tweedle-lang`, and runs
the focused Maven characterization. It is validation packaging; it is not the
archive/player feature itself.

The outside-in scenario that packages this evidence is:

```text
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
```

The scenario is a gated command smoke for the focused archive characterization
suite. It does not drive the Alice desktop, sample rendered pixels, grade learner
work, or complete a lesson.

The documentation examples are guarded by:

```text
tests/test_archive_player_boundary_docs.py
```

That guard keeps the wrapper, scenario, and direct
Maven evidence stay aligned.

## Diagnostics

Tests assert stable message fragments rather than full-message equality.
Expected diagnostics include:

| Case | Diagnostic content |
| --- | --- |
| Unsupported legacy `Program` with no safe image recovery | `Unsupported legacy JSON project archive`, `Program Tweedle decode is unsupported`, and `no safe legacy resource recovery applies`. |
| Missing or mismatched manifest-named program type | `Project archive manifest names program type '<type>'` and decoded type context such as `decoded type names are [...]`. |
| Unsupported manifest-declared sibling type | `Project archive contains unsupported manifest-declared Tweedle type names [...]`. |
| Missing or unsafe legacy image entry | The public legacy error plus a cause naming the missing or unsafe archive entry. |

Diagnostics must not include raw archive payloads, full Tweedle source bodies,
credentials, stack traces, or user-specific filesystem data.

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
behavior, learner grading, rubric scoring, complete player-project semantics,
Save completion, Sims validation, deployed installer success, or first-lesson
completion.

Unsupported archives either match the exact legacy image-resource compatibility
shape or fail closed at `IoUtilities.readProject`.
