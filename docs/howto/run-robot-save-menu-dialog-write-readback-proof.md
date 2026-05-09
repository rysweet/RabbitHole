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

Use the JSON artifact as the source of truth. Treat blocked JSON artifacts as negative evidence only: they explain why proof could not complete and are blocker evidence only, never partial success.

| Status | Meaning |
| --- | --- |
| `proven` | Robot menu activation, Save action attribution, dialog observation, chooser control, file write, readback, and marker verification all completed in the same rendered run. |
| `blocked` | The proof recorded exactly one known blocker for the earliest missing or unsafe step and the JUnit run failed. This is blocker evidence, not partial Save proof. |

Do not treat Maven success by itself as proof. A blocked artifact is blocker evidence for its `blocker.kind` only; `headless_awt` blocked artifacts are only headless-display blocker evidence. If the artifact reports `status: "blocked"` for a headless AWT display, it is not Save evidence. Use that artifact only as headless-display blocker evidence. It does not prove Robot Save activation. It does not prove full Save completion, including full desktop Save completion.

Example blocked artifact:

```json
{
  "schemaVersion": "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1",
  "scenario": "alice-desktop-save-menu-dialog-write-proof",
  "workflow": "save-menu-dialog-write-proof",
  "runId": "example-run-1",
  "generatedAtUtc": "2099-01-01T00:00:00Z",
  "status": "blocked",
  "proofTarget": "single rendered desktop Save path: menu, dialog, control, write, readback",
  "reportingSummary": "Robot File menu Save dialog/write/readback path was not proven; blocker.kind identifies the first missing or unsafe step.",
  "blocker": {
    "kind": "headless_awt",
    "observed": "No available non-headless AWT display",
    "required": "Xvfb or another non-headless AWT display capable of Robot mouse events and Swing JFileChooser display"
  },
  "menu": {
    "fileMenuOpened": false,
    "saveMenuItemInvoked": false,
    "saveActionIdentityMatched": false
  },
  "dialog": {
    "saveDialogObserved": false,
    "dialogType": "Swing JFileChooser",
    "dialogClass": null,
    "dialogShowing": false,
    "ambiguousChooserDiscovery": false,
    "pollCount": 0
  },
  "control": {
    "selectedPathSet": false,
    "approvedSelection": false,
    "selectedPathMatchesExpected": false,
    "targetInsideProofRoot": true,
    "normalizedSelectedPath": null,
    "expectedPath": "projects/robot-save-menu-proof.a3p"
  },
  "write": {
    "fileWritten": false,
    "fileNonempty": false,
    "fileHasExpectedExtension": true,
    "outputPath": "projects/robot-save-menu-proof.a3p",
    "outputSizeBytes": 0
  },
  "readback": {
    "projectReadable": false,
    "marker": "robotSaveMenuRoundTripMarker",
    "markerPresent": false
  },
  "baselinePreserved": [
    "StageIdeSaveMenuDoClickToWriteProofTest",
    "ProjectApplicationSaveProjectToTest",
    "JMenuBarRobotClickSaveProofTest"
  ],
  "requiresNextEvidence": [
    "Run under xvfb-run -a or an equivalent desktop session when blocker.kind is environment-related",
    "Use status proven only when Robot menu activation, dialog control, write, readback, and marker verification all succeed in one rendered path"
  ],
  "doesNotClaim": [
    "Save As coverage",
    "all Save variants",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage"
  ]
}
```

Do not treat blocked evidence as partial success. Do not combine this shard with `StageIdeSaveMenuDoClickToWriteProofTest`, `JMenuBarRobotClickSaveProofTest`, or direct save tests to claim the rendered Save path passed.

The Java proof must not rely on shell timeout to stop. Missing menu, missing dialog, ambiguous chooser discovery, failed chooser control, write timeout, readback failure, or missing marker must be bounded in Java and reported as `status: "blocked"`.

## Review checklist

Before citing the artifact as proof, confirm:

1. `schemaVersion` is `eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1`.
2. `scenario` is `alice-desktop-save-menu-dialog-write-proof`.
3. `runId` matches the value passed to Maven.
4. `status` is `proven`.
5. Every required `menu`, `dialog`, `control`, `write`, and `readback` flag is true.
6. `dialog.saveDialogObserved` is `true` and `dialog.ambiguousChooserDiscovery` is `false`.
7. `control.targetInsideProofRoot` and `control.selectedPathMatchesExpected` are `true`.
8. `write.outputPath` resolves to an existing proof-root `.a3p`.
9. `write.outputSizeBytes` matches the filesystem size.
10. `readback.projectReadable` and `readback.markerPresent` are `true`.

For the complete field contract, see [Save Proof Evidence](../reference/save-proof-evidence.md). For the scenario contract and no-timeout rule, see [Save Menu Dialog Write/Readback Proof](../reference/save-menu-dialog-write-proof.md).

## Non-claims

A proven run covers only the rendered File -> Save menu/dialog/control/write/readback path. It does not cover Save As, overwrite prompts, cancellation, retry, native file dialogs, all Save variants, lesson completion, grading, or broad desktop automation.
