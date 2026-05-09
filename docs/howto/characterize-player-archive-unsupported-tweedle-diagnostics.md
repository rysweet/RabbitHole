# Characterize Player Archive Unsupported Tweedle Diagnostics

Use this guide to add or review focused tests for JSON `.a3w` player archives
whose manifest-declared Tweedle types decode successfully, fail at a known
unsupported decoder boundary, or are missing from the archive.

This guide covers the argument-bearing explicit `this` seam:

```java
this.helper(value: 1);
```

It documents failure clarity only. Do not add broader method-call decode support
when following this guide.

## Review checklist

Use this checklist when adding or reviewing the bounded archive/player evidence:

| Item | Required check |
| --- | --- |
| Production entry point | Exercise `IoUtilities.readProject(File)` instead of private decoder helpers. |
| Generated fixtures | Build temporary JSON `.a3w` fixtures in the test; do not commit generated archives or binary corpus payloads. |
| Unsupported argument-bearing explicit `this` diagnostic | Keep the expected program type, decoded sibling context, unsupported reason, and `caller.this.helper` context adjacent to the claim. |
| Missing manifest-declared `src/*.twe` entry diagnostics | Keep missing-entry failures separate from unsupported syntax failures and assert the missing archive entry name. |
| Gated command-smoke evidence | Distinguish `gated-not-run` metadata from enabled Maven success evidence. |
| Claim limits | Do not cite this evidence for full Tweedle/player decode, migration completeness, UI automation, rendering, grading, Save/Open guarantees, or lesson completion. |

## Recover stale PR #463 boundary evidence

When PR #463 is stale or conflicted against current `origin/develop`, repair the
existing branch. Do not replace the pull request, merge it manually, or use a
no-op or owner-free bypass. The recovery is complete only after the branch is
reconciled, the final head SHA is captured, and the focused evidence below is
refreshed.

Before editing this boundary, inspect the current pull request head, the current
`origin/develop` head, and the archive/player evidence surfaces. Treat conflicts
in `archive-fixture-smoke.yaml` as evidence wording conflicts, not permission to
broaden the scenario into general archive, player, desktop workflow, rendering,
grading, Save/Open, or lesson-completion coverage.

Keep the repair narrow:

| Blocker | Allowed repair |
| --- | --- |
| Stale archive diagnostic wording | Update the stable substring assertions and this documentation together. |
| Missing manifest-entry evidence drift | Repair the generated fixture characterization and the matching archive I/O wording. |
| Gated QA smoke drift | Update only the scenario contract, runner allowlist, schema entry, and validation text needed for the affected workflow. |
| Decoder-boundary selector drift | Update only the focused core AST selector list and the decoder-boundary smoke description. |
| PR #463 branch conflict or staleness | Reconcile the existing PR branch with current `origin/develop`, preserve archive/player boundary wording, and refresh PR evidence at the final branch SHA. |

Do not repair a recovery blocker by adding checked-in generated archives,
weakening fail-closed assertions, replacing archive evidence with desktop
workflow evidence, or claiming support for unsupported Tweedle/player behavior.

Refresh the PR evidence after the final edit, not before. The evidence includes
the repaired branch SHA, the `origin/develop` base SHA, the mergeability or
comparison status, the archive/player evidence surfaces that were rechecked, the
actual repair diff files, focused validation commands with outcomes at the final
head SHA, and explicit `false` values for manual merge, replacement pull
request, and no-op mode.

## Prerequisites

Work in the historical archive characterization suite:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Generate archive fixtures in that test's JUnit temporary folder. That is the
approved fixture location for this boundary because the fixtures are synthetic
and deterministic. There is no stable checked-in fixture directory for these
unsupported Tweedle diagnostics.

Use production reader selection:

```java
IoUtilities.readProject(playerArchiveFile);
```

Do not instantiate private decoder helpers or bypass the JSON archive reader.

## Build the smallest player archive

Create a generated JSON `.a3w` archive in the JUnit temporary folder used by
`HistoricalArchiveRoundTripCharacterizationTest`. The archive contains:

```text
version.txt
manifest.json
src/Program.twe
src/DecodedSibling.twe
```

Use manifest metadata for a player archive:

```text
metadata.fileType = a3w
description.name = Program
projectStructure.sceneCameraType = WindowCamera
```

Add a `tweedle` `TypeReference` for `Program` and another supported sibling type
such as `DecodedSibling`. `Program` should extend `SProgram`, and the sibling
should be a decodable `SScene`. The sibling proves the reader can report both
decoded and unsupported manifest-declared type sets in one failure.

## Use the unsupported program source

The `Program` source should contain the unsupported argument-bearing explicit
`this` call:

```java
class Program extends SProgram {
  void caller() {
    this.helper(value: 1);
  }

  void helper(WholeNumber value) {
  }
}
```

Use a simple supported sibling source:

```java
class DecodedSibling extends SScene {
}
```

Keep the fixture synthetic and deterministic. Do not commit generated `.a3w`
archives or add binary corpus payloads for this test.

## Characterize missing manifest entries

Use a separate generated archive when the manifest declares a type reference but
the ZIP omits the corresponding source entry. Cover both directions:

```text
manifest references src/Program.twe, but src/Program.twe is absent
manifest references src/DecodedSibling.twe, but src/DecodedSibling.twe is absent
```

Read through the same public entry point:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

Assert the stable missing-entry substring. The archive entry name is contractual;
additional exception wrapper text is not:

```java
assertTrue(thrown.getMessage().contains(
    "Archive does not contain type entry src/Program.twe"));
```

The exact entry name should match the omitted manifest-declared type. Do not
replace this with a generic unsupported-type assertion; missing archive content
and unsupported Tweedle syntax are separate boundary failures.

## Assert the fail-closed archive behavior

Read through the public API and assert a checked archive failure:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

Assert stable message substrings. These substrings are contractual for the
argument-bearing explicit `this` seam:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Program"));
assertTrue(message.contains("DecodedSibling"));
assertTrue(message.contains("unsupported"));
assertTrue(message.contains("argument-bearing explicit this method calls"));
assertTrue(message.contains("caller.this.helper"));
```

Prefer stable substrings over full-message equality. Full messages may gain
optional archive path, manifest entry, decoder phase, source location, or wrapper
exception context, but they must continue to name the affected manifest type, the
unsupported decoder reason, and the affected call-site context.

## Keep the boundary conservative

The expected result is failure, not partial success:

```text
IoUtilities.readProject(...) throws IOException
```

Do not assert that a project is returned with a `null` program type. Do not
describe the sibling decode as completed player decode. The sibling name is
diagnostic context only; the unsupported expected program type still fails the
archive read.

When publishing or reviewing evidence, keep the claim to this scope:

```text
Generated JSON .a3w archive fixtures validate manifest routing, generated
fixture handling, unsupported Tweedle diagnostics, and missing-entry failures
through IoUtilities.readProject(File).
```

Do not describe this evidence as proving full Tweedle/player decode, historical
archive migration completeness, full UI automation, visible rendering
correctness, grading, Save/Open guarantees, or lesson completion.

## Run the focused gate

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Then run the module gate:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Run the focused core AST decoder-boundary characterization when the recovery
changes or cites the decoder-level unsupported-call evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeCreatesMethodInvocation+zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall \
  test
```

If the QA smoke scenario metadata changed, validate the scenario catalog:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The `alice-desktop-archive-fixture-smoke` and
`alice-desktop-tweedle-decoder-boundary-smoke` scenarios are gated command smoke
contracts. `--prepare-only` and an unset `ALICE_QA_RUN_GATED_SMOKES` record
`gated-not-run` evidence; they do not prove the Maven command ran.

Keep those scenario descriptions scoped: `archive-fixture-smoke` covers only the
generated archive fixture evidence for manifest routing, unsupported Tweedle
diagnostics, and missing-entry failures; `tweedle-decoder-boundary-smoke` covers
only the core AST decoder rejection checks for adjacent unsupported method-call
forms.

For PR #463 recovery, also run the focused Python contract tests that guard the
recovery evidence and this boundary wording:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest \
  tests.test_pr463_owner_free_recovery_gate \
  tests.test_pr463_archive_player_boundary_contract
```

The owner-free name is retained only because it is the existing test module name.
The documented recovery behavior rejects owner-free and no-op success paths.

Run the Alice desktop scenario/schema contracts for touched scenarios:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

Record each PR #463 validation as structured evidence with the command, outcome,
and final head SHA. A `passed` string without the command and SHA is not enough
for the recovery gate because it cannot prove which branch state was checked.

Do not cite unrelated QA, UI, rendering, grading, Save/Open, or lesson-completion
evidence as a substitute for these focused checks.
