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

Do not treat Maven success by itself as proof. Blocked artifacts are only headless-display blocker evidence. If the artifact reports `status: "blocked"` for a headless AWT display, it is not Save evidence. Use that artifact only as headless-display blocker evidence. It does not prove Robot Save activation, and it does not prove full desktop Save completion or any full Save completion claim.

Example blocked artifact:

```json
{
  "schema_version": "eatme.alice-desktop-robot-save-menu-dialog-write-readback-proof/v1",
  "status": "blocked",
  "proofTarget": "Robot File menu Save activation joined to dialog/write/readback evidence",
  "reporting_summary": "Robot Save not proven: headless AWT display prevented the rendered menu/dialog/write/readback path from starting.",
  "blocker": {
    "kind": "headless_awt",
    "message": "AWT Robot requires a usable desktop display."
  },
  "trigger": {
    "robot_file_menu_opened": false,
    "robot_save_item_clicked": false,
    "save_action_identity_matched": false
  },
  "observed_dialog": {
    "dialogType": "Swing JFileChooser",
    "dialog_class": null,
    "dialog_showing": false,
    "chooser_observed": false,
    "approved_selection": false,
    "ambiguous_chooser_discovery": false,
    "poll_count": 0
  },
  "selected_file": {
    "normalized_selected_file": null,
    "expected_file": "projects/robot-save-menu-proof.a3p",
    "selected_file_verified": false,
    "selected_file_matches_expected": false,
    "target_inside_proof_root": false
  },
  "written_artifact": {
    "target_file": "projects/robot-save-menu-proof.a3p",
    "file_written": false,
    "file_nonempty": false,
    "file_extension": "a3p",
    "file_has_expected_extension": true,
    "file_size_bytes": 0
  },
  "readback": {
    "project_readable": false,
    "expected_marker": "robotSaveMenuRoundTripMarker",
    "marker_present": false
  },
  "baselinePreserved": [
    "StageIdeSaveMenuDoClickToWriteProofTest",
    "ProjectApplicationSaveProjectToTest",
    "JMenuBarRobotClickSaveProofTest"
  ],
  "requiresNextEvidence": [
    "Run the proof under a usable desktop display such as xvfb-run -a.",
    "Use status proven only for Robot Save activation or full desktop Save completion claims."
  ],
  "doesNotClaim": [
    "full desktop Save completion",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage",
    "all Save variants",
    "Save As coverage"
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
