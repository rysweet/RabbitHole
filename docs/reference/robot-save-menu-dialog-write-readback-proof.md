# Robot Save Menu Dialog Write/Readback Proof

This reference documents the bounded Robot-driven Alice desktop Save proof. The proof activates File -> Save with AWT Robot mouse events, controls the live Swing Save dialog, writes a `.a3p` project file, reads the saved file back, and verifies the generated marker.

## Contents

- [Purpose](#purpose)
- [Canonical proof shard](#canonical-proof-shard)
- [Proof chain](#proof-chain)
- [Evidence artifact](#evidence-artifact)
- [Blocker artifact](#blocker-artifact)
- [API boundaries](#api-boundaries)
- [Configuration](#configuration)
- [Examples](#examples)
- [Non-claims](#non-claims)

## Purpose

`RobotSaveMenuDialogWriteReadbackProofTest` is the joined Save seam after the separate menu and write baselines:

| Baseline | What it proves |
| --- | --- |
| `JMenuBarRobotClickSaveProofTest` | AWT Robot can open a rendered File menu and dispatch the Save menu item through Swing/Croquet action events. |
| `StageIdeSaveMenuDoClickToWriteProofTest` | The real Save menu item can be activated with `doClick()`, can show and approve a live Swing `JFileChooser`, can write a non-empty `.a3p`, can read it back, and can verify the marker. |
| `ProjectApplicationSaveProjectToTest` | `ProjectApplication.saveProjectTo(File)` writes readable projects, updates the active save target, records recents, handles save-as targets, and preserves failure behavior. |

The Robot proof joins those seams in one executable path. A proven run means the Save action started from Robot File-menu activation, not from `doClick()`, `fire()`, `SaveOperationFlow`, or a direct save helper.

## Canonical proof shard

The canonical proof shard is:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

The proof or blocker artifact is written below:

```text
core/ide/target/save-menu-proofs/
```

The canonical artifact name is:

```text
robot-save-menu-dialog-write-readback-proof.json
```

## Proof chain

A `status: "proven"` artifact means every step in this chain completed in order:

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

The implementation fails closed. It cannot report `status: "proven"` unless Robot menu activation, Save item click attribution, dialog observation, chooser approval, file write, project readback, and marker verification are all true in the same run.

## Evidence artifact

The artifact schema is the reviewable API for this proof. Consumers must inspect the JSON rather than infer proof status from Maven output alone.

| Field | Contract |
| --- | --- |
| `schema_version` | `eatme.alice-desktop-robot-save-menu-dialog-write-readback-proof/v1`. |
| `status` | `proven` only for the complete Robot/menu/dialog/write/readback/marker chain; `blocked` when the chain is unsafe or environment-blocked. |
| `claim` | Present only for `status: "proven"` and limited to the bounded Robot File -> Save menu/dialog/write/readback/marker path. |
| `reporting_summary` | Present only for `status: "blocked"` and directs review to `blocker.kind`. |
| `proofTarget` | `Robot File menu Save activation joined to dialog/write/readback evidence`. |
| `trigger.robot_file_menu_opened` | `true` only after AWT Robot opens the rendered File menu popup. |
| `trigger.robot_save_item_clicked` | `true` only after AWT Robot clicks the Save menu item identified by `SaveProjectOperation` Swing action identity. |
| `trigger.save_action_identity_matched` | `true` only when the clicked item is the production Save action, not a label-only match. |
| `observed_dialog.dialogType` | `Swing JFileChooser`. |
| `observed_dialog.dialog_class` | Runtime dialog class name when a chooser dialog is observed; `null` before observation. |
| `observed_dialog.dialog_showing` | Whether the observed chooser dialog is showing at the evidence boundary. |
| `observed_dialog.chooser_observed` | `true` only after exactly one expected live Swing `JFileChooser` is found through `Window.getWindows()`. |
| `observed_dialog.approved_selection` | `true` only after the selected path is verified and `approveSelection()` completes. |
| `observed_dialog.ambiguous_chooser_discovery` | `true` blocks proof because multiple live choosers make attribution unsafe. |
| `observed_dialog.poll_count` | Number of bounded chooser-discovery polls completed before the dialog result was known. |
| `selected_file.normalized_selected_file` | Proof-root-relative `.a3p` target path. Absolute local paths are not stored. |
| `selected_file.expected_file` | Proof-root-relative target path the proof intended to select. |
| `selected_file.selected_file_verified` | `true` only after the chooser reports the selected target expected by the proof. |
| `selected_file.selected_file_matches_expected` | `true` only when the selected path exactly matches the expected target file. |
| `selected_file.target_inside_proof_root` | `true` only when the selected target remains under the test-owned proof root. |
| `written_artifact.target_file` | Proof-root-relative file path that the proof expects Save to write. |
| `written_artifact.file_written` | `true` only after the target exists and was written by this proof run. |
| `written_artifact.file_nonempty` | `true` only when the written file has non-zero size. |
| `written_artifact.file_extension` | `a3p`. |
| `written_artifact.file_has_expected_extension` | `true` only when the target filename ends with `.a3p`. |
| `written_artifact.file_size_bytes` | Size of the target file at artifact write time. |
| `readback.project_readable` | `true` only after `IoUtilities.readProject(savedFile)` returns a non-null project. |
| `readback.expected_marker` | `robotSaveMenuRoundTripMarker`. |
| `readback.marker_present` | `true` only when the readback project contains the expected marker. |
| `baselinePreserved` | Names the baselines that remain in scope: `StageIdeSaveMenuDoClickToWriteProofTest`, `ProjectApplicationSaveProjectToTest`, and `JMenuBarRobotClickSaveProofTest`. |
| `requiresNextEvidence` | Instructions for converting a blocked artifact into proof evidence and for avoiding overclaiming. |
| `doesNotClaim` | Explicit boundaries that prevent overstating the proof. |

## Blocker artifact

When the combined proof is unsafe or environment-blocked, the implemented test must write the same artifact with `status: "blocked"`. A blocked artifact is evidence of the exact missing precondition or unsafe ambiguity; it is not partial Save success.

Allowed blocker kinds are fixed strings:

| `blocker.kind` | Meaning |
| --- | --- |
| `headless_awt` | No non-headless AWT display is available. |
| `robot_unavailable` | `java.awt.Robot` cannot be created or cannot drive the display. |
| `file_menu_not_showing` | The rendered File menu cannot be located or opened by Robot. |
| `save_item_not_attributed` | The Save item cannot be identified by `SaveProjectOperation` action identity. |
| `dialog_not_observed` | No expected live Swing `JFileChooser` appears within the bounded wait. |
| `ambiguous_chooser_discovery` | More than one live `JFileChooser` is visible. |
| `chooser_control_failed` | The chooser target cannot be set and approved safely. |
| `target_path_rejected` | The selected path is outside the proof root or does not end with `.a3p`. |
| `write_not_observed` | The file write does not complete within the bounded wait. |
| `readback_failed` | The written file cannot be read with `IoUtilities.readProject(...)`. |
| `marker_missing` | The readback project does not contain `robotSaveMenuRoundTripMarker`. |

Blocked artifacts keep all success-shaped evidence fields false unless the field was directly observed before the blocker. For example, a `dialog_not_observed` blocker may report `robot_file_menu_opened: true` and `robot_save_item_clicked: true`, but it must keep chooser approval, write, readback, and marker fields false.

The blocker artifact must not include stack traces, environment dumps, local usernames, arbitrary absolute paths, or secrets. Use enum-style reasons and proof-root-relative paths.

## API boundaries

This proof is test-only. It does not add a public application API. The stable interface is the machine-readable artifact schema.

| Component | Role |
| --- | --- |
| `FileMenuModel` | Builds the production File menu used by the rendered menu bar. |
| `SaveProjectOperation` | Supplies the production Save action identity and dispatch target. |
| `AbstractSaveOperation.perform(UserActivity)` | Adapts Croquet action dispatch to active `StageIDE`, dialogs, wait cursor, and save callback. |
| `FileDialogUtilities.showSaveFileDialog(...)` | Shows the Swing Save chooser boundary controlled by the proof. |
| `ProjectApplication.saveProjectTo(File)` | Writes the selected `.a3p` project. |
| `IoUtilities.readProject(File)` | Reads the saved `.a3p` back as an Alice project. |
| Alice project/domain APIs | Verify that the readback project contains `robotSaveMenuRoundTripMarker`. |

Do not implement this proof by calling `SaveOperationFlow` directly. Direct calls belong to lower-level flow characterization and would bypass the Robot File-menu seam.

## Configuration

Run from the repository root. Initialize the grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use the saved memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Run the Robot proof under a usable display:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  test
```

Run the Robot proof with the required baselines:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest,org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest,org.alice.ide.croquet.models.projecturi.JMenuBarRobotClickSaveProofTest,org.alice.ide.ProjectApplicationSaveProjectToTest \
  test
```

## Examples

Numeric values such as `observed_dialog.poll_count` and `written_artifact.file_size_bytes` are run-specific. The examples below show the required field shape; review the generated artifact for the values from a specific run.

### Proven artifact

```json
{
  "schema_version": "eatme.alice-desktop-robot-save-menu-dialog-write-readback-proof/v1",
  "status": "proven",
  "proofTarget": "Robot File menu Save activation joined to dialog/write/readback evidence",
  "claim": "AWT Robot opened File, clicked the production Save menu item, controlled the Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker",
  "trigger": {
    "robot_file_menu_opened": true,
    "robot_save_item_clicked": true,
    "save_action_identity_matched": true
  },
  "observed_dialog": {
    "dialogType": "Swing JFileChooser",
    "dialog_class": "javax.swing.JDialog",
    "dialog_showing": true,
    "chooser_observed": true,
    "approved_selection": true,
    "ambiguous_chooser_discovery": false,
    "poll_count": 3
  },
  "selected_file": {
    "normalized_selected_file": "projects/robot-save-menu-proof.a3p",
    "expected_file": "projects/robot-save-menu-proof.a3p",
    "selected_file_verified": true,
    "selected_file_matches_expected": true,
    "target_inside_proof_root": true
  },
  "written_artifact": {
    "target_file": "projects/robot-save-menu-proof.a3p",
    "file_written": true,
    "file_nonempty": true,
    "file_extension": "a3p",
    "file_has_expected_extension": true,
    "file_size_bytes": 2379
  },
  "readback": {
    "project_readable": true,
    "expected_marker": "robotSaveMenuRoundTripMarker",
    "marker_present": true
  },
  "baselinePreserved": [
    "StageIdeSaveMenuDoClickToWriteProofTest",
    "ProjectApplicationSaveProjectToTest",
    "JMenuBarRobotClickSaveProofTest"
  ],
  "requiresNextEvidence": [
    "Run under xvfb-run -a or an equivalent desktop session when blocker.kind is environment-related",
    "Use status proven only when Robot menu activation, dialog control, write, readback, and marker verification all succeed"
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

### Headless blocker artifact

```json
{
  "schema_version": "eatme.alice-desktop-robot-save-menu-dialog-write-readback-proof/v1",
  "status": "blocked",
  "proofTarget": "Robot File menu Save activation joined to dialog/write/readback evidence",
  "reporting_summary": "Robot File menu Save dialog/write/readback path was not proven; see blocker.kind for the exact missing or unsafe precondition",
  "blocker": {
    "kind": "headless_awt",
    "observed": "No available non-headless AWT display",
    "required": "Xvfb or another non-headless AWT display capable of Robot mouse events and Swing JFileChooser display"
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
    "Run under xvfb-run -a or an equivalent desktop session when blocker.kind is environment-related",
    "Use status proven only when Robot menu activation, dialog control, write, readback, and marker verification all succeed"
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

## Non-claims

Even when `status` is `proven`, this proof does not claim:

1. Full desktop Save completion.
2. Save As, backup Save, overwrite prompt, cancellation, retry, unwritable-file, repeated-save, or every Save variant.
3. Native `java.awt.FileDialog` coverage.
4. Physical user click evidence.
5. Visible rendering correctness.
6. Lesson completion, grading, or learner workflow correctness.
7. Broad Alice desktop automation coverage.
