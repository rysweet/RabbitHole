# Robot Save Menu Dialog Write/Readback Proof

This reference documents the focused Java proof shard behind the `save-menu-dialog-write-proof` outside-in scenario. A `status: "proven"` artifact from this shard means AWT Robot used the rendered Alice desktop File menu, followed the production Save dialog path, controlled the live Swing chooser, wrote a `.a3p`, read it back, and verified `robotSaveMenuRoundTripMarker`.

For the canonical JSON field contract and validation rules, see [Save Proof Evidence](./save-proof-evidence.md). For the scenario-level QA contract, see [Save Menu Dialog Write/Readback Proof](./save-menu-dialog-write-proof.md).

## Purpose

`RobotSaveMenuDialogWriteReadbackProofTest` uses one continuous rendered path and keeps the historical seams as support only:

| Supporting seam | What it proves |
| --- | --- |
| `JMenuBarRobotClickSaveProofTest` | AWT Robot can open a rendered File menu and dispatch the Save menu item through Swing/Croquet action events. |
| `StageIdeSaveMenuDoClickToWriteProofTest` | The real Save menu item can reach a live Swing chooser and write a `.a3p` through `doClick()`. |
| `ProjectApplicationSaveProjectToTest` | `ProjectApplication.saveProjectTo(File)` writes readable projects and preserves direct save semantics. |

The Robot proof is the only one of these paths that can satisfy the Save proof scenario. The supporting seams remain useful regression baselines, but their artifacts cannot be aggregated into a completed rendered Save proof.

## Canonical proof shard

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

The target shard writes exactly one canonical artifact:

```text
core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json
```

## Proof chain

A `status: "proven"` artifact means every step in this chain completed in order during the same run:

```text
rendered StageIDE/Croquet JMenuBar
  -> AWT Robot click on File menu
  -> File popup becomes visible
  -> AWT Robot click on Save menu item identified by SaveProjectOperation action identity
  -> SaveProjectOperation dispatches through AbstractSaveOperation.perform(UserActivity)
  -> DocumentFrame.showSaveFileDialog(...)
  -> FileDialogUtilities.showSaveFileDialog(...)
  -> exactly one live Swing JFileChooser observed
  -> test-owned .a3p target selected inside core/ide/target/save-menu-proofs/
  -> chooser approval completes
  -> ProjectApplication.saveProjectTo(File)
  -> non-empty .a3p written
  -> IoUtilities.readProject(savedFile)
  -> readback project contains robotSaveMenuRoundTripMarker
```

The proof fails closed. It cannot report `status: "proven"` unless Robot menu activation, Save item attribution, dialog observation, chooser control, file write, project readback, and marker verification are all true in the same run. All Robot/menu/dialog/write waits are bounded inside the Java proof; exhausted waits produce `status: "blocked"` evidence and a failing JUnit result instead of relying on shell timeout.

## Evidence status

The shard delegates final evidence semantics to `SaveOperationCompletionEvidence`.

| Status | Shard behavior |
| --- | --- |
| `proven` | The JUnit test passes only after the canonical artifact is written with all required fields true and internally consistent. |
| `blocked` | The JUnit test writes the canonical artifact with exactly one known blocker and then fails so the scenario cannot pass on partial evidence. |

Blocked output is the executable blocker for the missing step. It is not proof of Save completion, and it must not be summarized as Robot Save readiness.

Use the JSON artifact as the source of truth. Do not treat Maven success by itself as proof. A blocked artifact is blocker evidence for its `blocker.kind` only; `headless_awt` blocked artifacts are only headless-display blocker evidence. If the artifact reports `status: "blocked"` with `blocker.kind` set to `headless_awt`, use that artifact only as headless-display blocker evidence. It does not prove Robot Save activation, full Save completion, or full desktop Save completion.

`requiresNextEvidence` is schema and reviewer guidance emitted with the artifact, not an incomplete-proof signal. For `status: "proven"`, treat it as guidance for future reruns and review expectations; the proof is complete when the status is `proven` and every required menu, dialog, control, write, and readback field is true. For `status: "blocked"`, it describes the next evidence needed before anyone may cite Save completion.

### Blocked headless-display artifact

This is the complete blocked example for a headless AWT display. It records no successful Save evidence and must not be cited as partial Robot Save proof.

```json
{
  "schemaVersion": "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1",
  "scenario": "alice-desktop-save-menu-dialog-write-proof",
  "workflow": "save-menu-dialog-write-proof",
  "runId": "example-run-1",
  "generatedAtUtc": "2026-05-09T05:39:23Z",
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

### Proven artifact

This is the minimum successful example. It is valid only when every menu, dialog, selected-file, write, and readback field is true for the same rendered run.

The `requiresNextEvidence` entries in this proven example are not extra unmet requirements. They remain in the artifact so reviewers can see the same collection guidance and success threshold on every run.

```json
{
  "schemaVersion": "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1",
  "scenario": "alice-desktop-save-menu-dialog-write-proof",
  "workflow": "save-menu-dialog-write-proof",
  "runId": "example-run-1",
  "generatedAtUtc": "2026-05-09T05:39:23Z",
  "status": "proven",
  "proofTarget": "single rendered desktop Save path: menu, dialog, control, write, readback",
  "claim": "AWT Robot opened File, clicked the production Save menu item, controlled the rendered Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker",
  "blocker": null,
  "menu": {
    "fileMenuOpened": true,
    "saveMenuItemInvoked": true,
    "saveActionIdentityMatched": true
  },
  "dialog": {
    "saveDialogObserved": true,
    "dialogType": "Swing JFileChooser",
    "dialogClass": "javax.swing.JDialog",
    "dialogShowing": true,
    "ambiguousChooserDiscovery": false,
    "pollCount": 4
  },
  "control": {
    "selectedPathSet": true,
    "approvedSelection": true,
    "selectedPathMatchesExpected": true,
    "targetInsideProofRoot": true,
    "normalizedSelectedPath": "projects/robot-save-menu-proof.a3p",
    "expectedPath": "projects/robot-save-menu-proof.a3p"
  },
  "write": {
    "fileWritten": true,
    "fileNonempty": true,
    "fileHasExpectedExtension": true,
    "outputPath": "projects/robot-save-menu-proof.a3p",
    "outputSizeBytes": 12345
  },
  "readback": {
    "projectReadable": true,
    "marker": "robotSaveMenuRoundTripMarker",
    "markerPresent": true
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

## API boundaries

This proof is test-only. It does not add a public application API. The stable review interface is the machine-readable artifact documented in [Save Proof Evidence](./save-proof-evidence.md).

| Component | Role |
| --- | --- |
| `FileMenuModel` | Builds the production File menu used by the rendered menu bar. |
| `SaveProjectOperation` | Supplies the production Save action identity and dispatch target. |
| `AbstractSaveOperation.perform(UserActivity)` | Adapts Croquet action dispatch to active `StageIDE`, dialogs, wait cursor, and save callback. |
| `FileDialogUtilities.showSaveFileDialog(...)` | Shows the Swing Save chooser boundary controlled by the proof. |
| `ProjectApplication.saveProjectTo(File)` | Writes the selected `.a3p` project. |
| `IoUtilities.readProject(File)` | Reads the saved `.a3p` back as an Alice project. |
| Alice project/domain APIs | Verify that the readback project contains `robotSaveMenuRoundTripMarker`. |

Do not implement this proof by calling `SaveOperationFlow`, `ProjectApplication.saveProjectTo(File)`, or a direct dialog bypass. Those paths belong to lower-level characterization and do not exercise the rendered menu/dialog/control chain.

## Configuration

Run from the repository root. Initialize the grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use the saved QA memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Run under a usable desktop display:

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

The outside-in runner supplies the same proof properties automatically and validates the artifact after Maven exits. Prefer the scenario wrapper for review evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof
```

## Non-claims

Even when `status` is `proven`, this proof does not claim:

1. Save As, overwrite prompt, cancellation, retry, unwritable-file, repeated-save, or every Save variant.
2. Native `java.awt.FileDialog` coverage.
3. Physical user click evidence.
4. Visible rendering correctness beyond the observed controls needed for this path.
5. Lesson completion, grading, or learner workflow correctness.
6. Broad Alice desktop automation coverage.
