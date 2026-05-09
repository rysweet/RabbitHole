# Run the Robot Save Menu Dialog Write/Readback Proof

Use this guide when you need to run the focused Java proof shard directly. For normal QA evidence, prefer [Run the Save Menu Dialog Write/Readback Proof](./run-save-menu-dialog-write-proof.md), which runs the outside-in scenario and validates the canonical artifact fail-closed.

## Proof shard

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

When its artifact reports `status: "proven"`, the shard proves one rendered Save path: Robot opens File, clicks the production Save item, observes and controls the Swing Save chooser, writes a proof-root `.a3p`, reads it back, and verifies `robotSaveMenuRoundTripMarker`.

## Prerequisites

Run commands from the repository root. Initialize the Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the saved QA memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Provide a non-headless AWT display. On Linux CI or a headless workstation, run the proof under Xvfb.

## Run the focused proof

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  -Dorg.alice.eatme.saveProof.scenario=alice-desktop-save-menu-dialog-write-proof \
  -Dorg.alice.eatme.saveProof.runId=save-proof-$(date -u +%Y%m%dT%H%M%SZ)-manual \
  -Dorg.alice.eatme.saveProof.evidencePath=core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json \
  test
```

The target proof writes:

```text
core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json
```

## Interpret the artifact

Use the JSON artifact as the source of truth.

| Status | Meaning |
| --- | --- |
| `proven` | Robot menu activation, Save action attribution, dialog observation, chooser control, file write, readback, and marker verification all completed in the same rendered run. |
| `blocked` | The proof recorded exactly one known blocker for the earliest missing or unsafe step and the JUnit run failed. |

Do not treat blocked evidence as partial success. Do not combine this shard with `StageIdeSaveMenuDoClickToWriteProofTest`, `JMenuBarRobotClickSaveProofTest`, or direct save tests to claim the rendered Save path passed.

The Java proof must not rely on shell timeout to stop. Missing menu, missing dialog, ambiguous chooser discovery, failed chooser control, write timeout, readback failure, or missing marker must be bounded in Java and reported as `status: "blocked"`.

## Review checklist

Before citing the artifact as proof, confirm:

1. `schemaVersion` is `eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1`.
2. `scenario` is `alice-desktop-save-menu-dialog-write-proof`.
3. `runId` matches the value passed to Maven.
4. `status` is `proven`.
5. Every required `menu`, `dialog`, `control`, `write`, and `readback` flag is true.
6. `dialog.chooserObserved` is `true` and `dialog.ambiguousChooserDiscovery` is `false`.
7. `selection.targetInsideProofRoot` and `selection.selectedFileMatchesExpected` are `true`.
8. `write.outputPath` resolves to an existing proof-root `.a3p`.
9. `write.outputSizeBytes` matches the filesystem size.
10. `readback.projectReadable` and `readback.markerPresent` are `true`.

For the complete field contract, see [Save Proof Evidence](../reference/save-proof-evidence.md). For the scenario contract and no-timeout rule, see [Save Menu Dialog Write/Readback Proof](../reference/save-menu-dialog-write-proof.md).

## Non-claims

A proven run covers only the rendered File -> Save menu/dialog/control/write/readback path. It does not cover Save As, overwrite prompts, cancellation, retry, native file dialogs, all Save variants, lesson completion, grading, or broad desktop automation.
