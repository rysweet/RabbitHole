# Run the Robot Save Menu Dialog Write/Readback Proof

Use this guide to run the focused proof that joins Robot File-menu Save activation to live Save dialog control, `.a3p` write, project readback, and marker verification.

## Proof shard

The executable proof shard is:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/RobotSaveMenuDialogWriteReadbackProofTest.java
```

Use the generated JSON artifact as evidence. Do not treat Maven success by itself as proof, because a safe display-precondition run can pass while writing `status: "blocked"`.

## Prerequisites

Run commands from the repository root. Initialize the Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the saved memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Provide a non-headless AWT display. On Linux CI or a headless workstation, run the proof under Xvfb:

```bash
xvfb-run -a true
```

## Run the focused proof

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest \
  test
```

The proof writes `robot-save-menu-dialog-write-readback-proof.json` below:

```text
core/ide/target/save-menu-proofs/
```

## Run the regression baseline set

Run the joined Robot proof with the existing Save baselines before using the artifact in review:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest,org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest,org.alice.ide.croquet.models.projecturi.JMenuBarRobotClickSaveProofTest,org.alice.ide.ProjectApplicationSaveProjectToTest \
  test
```

This command preserves the baseline evidence for menu-item `doClick()` write/readback, lower-level `ProjectApplication.saveProjectTo(File)` behavior, and Robot menu dispatch while adding the joined Robot/menu/dialog/write/readback seam.

## Interpret the artifact

Use the JSON artifact as the source of truth.

| Status | Meaning |
| --- | --- |
| `proven` | Robot opened File, clicked the production Save item, controlled exactly one Swing `JFileChooser`, approved a proof-root `.a3p` path, wrote a non-empty file, read it with `IoUtilities.readProject(...)`, and found `robotSaveMenuRoundTripMarker`. |
| `blocked` | The executable proof could not safely complete the chain. Read `blocker.kind`, `blocker.observed`, and `requiresNextEvidence` before rerunning. |

Do not treat Maven success by itself as proof. A display-precondition run can succeed while writing `status: "blocked"` so the review has a machine-readable blocker artifact instead of prose-only explanation.

## Review checklist

Before citing the result as evidence, confirm:

1. `status` is `proven`.
2. `trigger.robot_file_menu_opened`, `trigger.robot_save_item_clicked`, and `trigger.save_action_identity_matched` are `true`.
3. `observed_dialog.chooser_observed` and `observed_dialog.approved_selection` are `true`.
4. `observed_dialog.ambiguous_chooser_discovery` is `false`.
5. `selected_file.target_inside_proof_root` is `true`.
6. `written_artifact.file_written` and `written_artifact.file_nonempty` are `true`.
7. `readback.project_readable` and `readback.marker_present` are `true`.
8. `doesNotClaim` still excludes full desktop Save completion and all Save variants.

## Blocked run example

If a CI worker has no usable display, the artifact reports a blocker instead of a partial proof:

```json
{
  "status": "blocked",
  "blocker": {
    "kind": "headless_awt",
    "observed": "No available non-headless AWT display",
    "required": "Xvfb or another non-headless AWT display capable of Robot mouse events and Swing JFileChooser display"
  },
  "trigger": {
    "robot_file_menu_opened": false,
    "robot_save_item_clicked": false
  },
  "written_artifact": {
    "file_written": false
  },
  "readback": {
    "project_readable": false,
    "marker_present": false
  }
}
```

Rerun under `xvfb-run -a` or another desktop session to collect a `status: "proven"` artifact.
