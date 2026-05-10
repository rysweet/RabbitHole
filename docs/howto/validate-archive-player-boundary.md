# Validate the Archive/Player Boundary

Use this guide to validate the focused archive/player feature: legacy JSON
`.a3w` player archives with manifest-declared image resources must fail closed
when the manifest-declared program Tweedle type is unsupported.

This validation proves archive IO behavior only. It does not prove full
Tweedle/player decode, full historical archive migration, UI automation
coverage, visible rendering correctness, Save completion, grading, Sims
validation, deployed installer success, or first-lesson completion.

## Prerequisites

Run commands from the repository root.

Initialize the Tweedle grammar submodule before Maven validation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

`NODE_OPTIONS` is the saved automation memory setting. It is not an Alice
archive decoder option.

## Run the bounded validation sequence

Use the branch-installable wrapper when you want the complete evidence package:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack archive-player-boundary verify
```

Replace `<branch>` with the branch under review, and run it from an Alice
checkout of that branch. The command validates the archive fixture smoke
scenario contract, initializes `tweedle-lang`, and runs the focused historical
archive characterization suite. It does not run the desktop, broaden the player
claim, or execute unrelated Maven suites.

Use the direct Maven command when you are already in a prepared checkout:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

## Review the feature evidence

The selected boundary is the resource-recovery fail-closed case:

```text
JSON .a3w archive
manifest-declared Program type
manifest-declared image resource
Program Tweedle source is unsupported
```

The required public API behavior is:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

The key characterization methods are:

| Test method | Required proof |
| --- | --- |
| `generatedWorldArchiveWithUnsupportedResourceExpressionIsRejectedWithoutPartialProgramDecode` | Exported player archive with image-resource expression fails instead of returning partial program readback. |
| `generatedJsonPlayerArchiveWithResourceFieldInitializerProgramTypeIsRejectedWithoutPartialProgramDecode` | JSON `.a3w` with manifest-declared image resource and unsupported program resource initializer throws `IOException`. |
| `generatedJsonPlayerArchiveWithResourceFieldInitializerSiblingTypeIsRejectedWithoutSilentOmission` | Unsupported manifest-declared sibling type fails the archive read instead of being silently omitted. |

Supported JSON `.a3w` decode/readback tests in the same suite are neighboring
regression coverage. They make sure the fail-closed change does not break
already-supported archive slices; they are not a claim of complete player
support.

## Validate QA command-smoke packaging

Validate the desktop QA scenario schema and allowlist:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The archive fixture smoke scenario is:

```text
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
```

Its `automation.argv` is the focused
`HistoricalArchiveRoundTripCharacterizationTest` Maven command. The scenario is
review packaging around the archive characterization suite; it does not add
desktop UI automation, rendering correctness, Save completion, grading, Sims
validation, deployed installer, or lesson-completion evidence.

Prepare the scenario evidence without executing the gated command:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-archive-fixture-smoke --prepare-only
```

Run the gated command smoke only in a worktree prepared for Maven evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 ALICE_QA_RUN_GATED_SMOKES=1 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-archive-fixture-smoke
```

The gated run must produce `status.txt`, `command.log`, and test output or a
Surefire report naming `HistoricalArchiveRoundTripCharacterizationTest`.

## Review diagnostics

Use stable message fragments, not full-message equality.

For unsupported manifest-named program types, check for:

```text
Project archive manifest names program type
decoded type names are [...]
```

For unsupported manifest-declared sibling types, check for:

```text
Project archive contains unsupported manifest-declared Tweedle type names [...]
```

Do not change the tests to expect a `Project` with a null program type, a
resource-only success, or a silently omitted unsupported type.

## PR and documentation wording

Use conservative wording:

```text
This change makes legacy JSON .a3w player archives with manifest-declared image
resources fail closed when the manifest-declared program Tweedle type is
unsupported, while preserving existing supported JSON player archive readback
tests.
```

Do not use wording that implies:

- full Tweedle/player decode;
- full historical archive migration;
- UI automation coverage;
- visible rendering correctness;
- Save completion;
- grading correctness;
- Sims validation;
- deployed installer success;
- first-lesson completion.
