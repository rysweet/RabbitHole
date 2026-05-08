# Save Menu Dialog Written-Project Proof

This reference is the target contract for the strengthened bounded desktop Save proof shard for Alice project Save. The feature to build must prove actual Save menu item activation, completed Swing `JFileChooser` approval, a real `.a3p` write, readable project round-trip, and verification that the saved project contains the expected synthetic marker.

Implementation status: this document is a retcon spec for the strengthened proof, not evidence that the current shard already performs readback and marker verification. Until the test and artifact are updated to this contract, existing passing runs must be treated as bounded write proof only.

## Contents

- [Purpose](#purpose)
- [Canonical proof shard](#canonical-proof-shard)
- [Execution contract](#execution-contract)
- [Evidence artifacts](#evidence-artifacts)
- [API boundaries](#api-boundaries)
- [Configuration](#configuration)
- [Examples](#examples)
- [Non-claims](#non-claims)

## Purpose

The strengthened proof shard must show one real desktop Save menu path beyond the earlier bounded write-only seam. It does not attempt comprehensive Save coverage. The successful path is:

```text
Save menu item doClick()
  -> Croquet SaveProjectOperation dispatch
  -> AbstractSaveOperation.perform(UserActivity)
  -> DocumentFrame.showSaveFileDialog(...)
  -> FileDialogUtilities.showSaveFileDialog(...)
  -> Swing JFileChooser approval
  -> ProjectApplication.saveProjectTo(File)
  -> .a3p file under the JUnit temp directory
  -> IoUtilities.readProject(savedFile)
  -> readback project contains saveMenuDoClickRoundTripMarker
```

Linux Swing `JFileChooser` is the expected controlled dialog. The proof does not require a native `java.awt.FileDialog` peer.

## Canonical proof shard

The canonical test target to strengthen is:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/StageIdeSaveMenuDoClickToWriteProofTest.java
```

The strengthened test must create a deterministic marked project, obtain the production Save menu item through `SaveProjectOperation.getInstance().getMenuItemPrepModel().createMenuItemAndAddTo(...)`, activate it with `doClick()`, control the live Swing `JFileChooser`, and assert the approved target file is a non-empty `.a3p`.

The strengthened implementation must then read the written file back through `IoUtilities.readProject(targetFile)`. The readback project must be non-null and must contain the marker `saveMenuDoClickRoundTripMarker` through normal Alice project/domain APIs. A non-empty file alone is not enough for this proof.

The strengthened proof must not call `SaveOperationFlow` directly. `SaveOperationFlow` remains the production coordination layer reached through the menu operation.

## Execution contract

### Success

A successful strengthened run must prove all of the following in one bounded path:

| Required observation | Meaning |
| --- | --- |
| Save menu item `doClick()` ran | The proof starts from the production menu item dispatch path, not a direct `fire()` or `SaveOperationFlow` call, and the artifact records `trigger.menu_item_doclick: true`. |
| Exactly one expected live Swing `JFileChooser` was controlled | The test reached the production dialog boundary without ambiguous chooser discovery. |
| Chooser approval completed | `approved_selection` means the EDT callback verified the selected path and called `approveSelection()`; a queued approval is not enough. |
| Selected path matched the normalized expected target | The proof writes only inside the JUnit temp directory. |
| The target file exists, ends with `.a3p`, and is non-empty | The Save path reached the project write boundary. |
| The target file can be read back with `IoUtilities.readProject(targetFile)` | The saved payload is a readable Alice project, not merely bytes on disk. |
| The readback project contains `saveMenuDoClickRoundTripMarker` | The written payload is the deterministic project created by the test, not an unrelated or stale project file. |
| Canonical evidence reports `wroteFile: true` and `readback.marker_present: true` only after all assertions pass | Machine-readable evidence cannot report a success-shaped write or marker result without the real readable project. |

Any unproven strengthened run must fail closed. Preexisting target files, shortcut chooser/file signals without the recorded menu `doClick()` seam, incomplete chooser observations, timeouts, path mismatches, canonicalization failures, readback failures, missing markers, and multiple live `JFileChooser` instances must not produce trigger, approval, write, readback, or marker success claims.

### Unsupported display result

If the proof cannot run because the JVM cannot create a non-headless desktop, the strengthened executable proof must write an unsupported-result artifact and return without claiming success. The precise reason is:

```text
No available non-headless AWT display
```

This blocker is the desktop-precondition blocker for the canonical shard. It means the environment must provide a display, such as Xvfb, before the Save dialog/control/write/readback/marker path can be exercised.

The strengthened proof shard must keep result states distinct:

| State | Meaning |
| --- | --- |
| `proven` | Menu activation, chooser approval, selected path verification, non-empty `.a3p` write, `IoUtilities.readProject(...)`, and expected marker verification all completed. |
| `not_proven` | The proof ran under a display but the complete chain did not finish. |
| `unsupported` | The proof did not exercise the dialog path because no non-headless AWT display was available. |
| `gated-not-run` | Outside-in QA wrapper state only; the gated command was not executed. |

The strengthened Maven proof must write generated artifacts under its test target directory. It must not write directly to the outside-in QA evidence path. After the strengthened unsupported artifact lands, if a PR cannot provide the display-backed proof and needs persistent review evidence, copy the generated unsupported artifact to:

```text
qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof-blocker.json
```

That copied artifact remains an unsupported-result artifact. It must preserve the single reason, `No available non-headless AWT display`, keep all success-shaped fields false, avoid inferring chooser behavior, avoid claiming that a project file was written, and avoid claiming readable-project or marker verification.

## Evidence artifacts

The reviewer-facing proof artifact for the strengthened contract is:

```text
stageide-save-menu-doclick-write-proof.json
```

The test-owned artifact location is under:

```text
core/ide/target/stageide-save-menu-doclick-write-proof-test/<run-id>/doclick-to-written-file/evidence/
```

This artifact must stay separate from `SaveOperationCompletionEvidence`. `SaveOperationCompletionEvidence` records lower-level Save completion facts such as redacted/relative `saved_file`, `saved_file_exists`, `saved_file_size_bytes`, and bounded write facts. It is not the source for the full menu activation, chooser approval, selected-file, readable-project, or marker proof.

The strengthened canonical proof artifact must record:

| Field | Contract |
| --- | --- |
| `status` | `proven` only when menu activation, chooser approval, file write assertions, project readback, and marker verification all pass; `not_proven` for display-backed runs that do not complete the chain; `unsupported` for missing non-headless AWT display. |
| `dialogType` | `Swing JFileChooser` for the controlled Linux Swing chooser path. |
| `wroteFile` | `true` only after the target exists inside the proof root, has `.a3p` extension, and has non-zero size. |
| `claim` / `reporting_summary` | `claim` is present only for `status: proven`; unsupported or not-proven runs use `reporting_summary` and must not claim chooser approval, file writing, readback, or marker verification. |
| `trigger.menu_item_doclick` | `true` only after the proof records `menuItem.doClick()` on the Save menu item created by `getMenuItemPrepModel().createMenuItemAndAddTo(...)`; incomplete or shortcut artifacts keep it `false`. |
| `observed_dialog.approved_selection` | `true` only when the EDT approval callback completed after selected-file verification; scheduling approval does not set this claim. |
| `selected_file.normalized_selected_file` | The proof-root-relative normalized approved path, or a redacted outside-root marker, so reviewer artifacts do not expose machine-specific absolute paths. |
| `written_artifact.target_file` | The proof-root-relative target path, or a redacted outside-root marker; evidence must not store absolute machine paths. |
| `written_artifact.target_inside_proof_root` | `true` only when the written target stayed inside the controlled proof root. |
| `readback.project_readable` | `true` only after `IoUtilities.readProject(targetFile)` returns a non-null project. |
| `readback.expected_marker` | The deterministic marker string `saveMenuDoClickRoundTripMarker`. |
| `readback.marker_present` | `true` only after the readback project contains the expected marker through project/domain API inspection. |
| `proof_chain` | The production Save menu path from `menuItem.doClick()` through `SaveProjectOperation`, `AbstractSaveOperation.perform`, dialog approval, project write, project readback, and marker verification. |
| `doesNotClaim` | Explicit exclusions for full desktop Save completion, lesson completion, rendering, grading, physical user clicks, broad UI automation, native dialog coverage, and all Save variants. |

The strengthened unsupported display artifact has this contract:

| Field | Contract |
| --- | --- |
| `status` | `unsupported`; never `proven`. |
| `reason` | Exactly `No available non-headless AWT display`. |
| `dialogType` | `Swing JFileChooser`, naming the dialog path that could not be exercised. |
| `wroteFile` | `false`; the unsupported artifact never represents a successful Save write. |
| `approved_selection`, `file_written`, `file_nonempty` | `false`; chooser approval and file write remain unproven. |
| `readback.project_readable`, `readback.marker_present` | `false`; readable-project and marker verification remain unproven. |
| `reporting_summary` | States that the Save menu/control/dialog/write/readback/marker path requires a non-headless AWT display before it can be proven. |
| `blocker.observed` | States that `GraphicsEnvironment.isHeadless()` is true or no usable desktop display is available. |
| `blocker.required` | States that Xvfb or another non-headless AWT display capable of showing a Swing `JFileChooser` is required. |
| `requiresNextEvidence` | Includes running the proof under `xvfb-run -a` or an equivalent desktop session and collecting a `status: proven` artifact. |
| `doesNotClaim` | Explicitly excludes full desktop Save completion, full lesson completion, visible rendering correctness, grading correctness, physical user clicks, broad UI automation coverage, and native dialog coverage. |

The dialog-discovery companion artifact is:

```text
desktop-save-dialog-discovery-target.json
```

It is written when `org.alice.eatme.saveDialogDiscoveryEvidenceDir` is set and documents the `FileDialogUtilities.showSaveFileDialog(Component,File,String,String)` owner/root/selection target before the Swing chooser appears. Use this artifact to inspect dialog boundary discovery, not final file-write, readback, or marker success.

Evidence write failures are logged and do not change production Save behavior.

Multiple live `JFileChooser` instances are an ambiguity blocker. The strengthened proof must cancel candidate choosers where appropriate and leave `wroteFile`, `observed_dialog.approved_selection`, `written_artifact.file_written`, `readback.project_readable`, and `readback.marker_present` false.

## API boundaries

The strengthened proof must observe production behavior through existing Save APIs and seams:

| Component | Role in this proof |
| --- | --- |
| `SaveProjectOperation` | Owns the production Save action and menu item prep model. |
| `AbstractSaveOperation.perform(UserActivity)` | Adapts the active `StageIDE`, document frame, wait cursor, and project save callback. |
| `SaveOperationFlow` | Coordinates selected file, prompt count, save attempts, finish/cancel, and save callback; the proof reaches it through the production operation. |
| `DocumentFrame.showSaveFileDialog(File,String,String)` | Production frame/dialog boundary. |
| `FileDialogUtilities.showSaveFileDialog(Component,File,String,String)` | Production Swing chooser boundary used by the proof. |
| `ProjectApplication.saveProjectTo(File)` | Production project-save write boundary. |
| `ProjectFileUtilities.saveCopyOfProjectTo(...)` and `IoUtilities.writeProject(...)` | Lower-level write path reached by a successful project save. |
| `IoUtilities.readProject(File)` | Readback boundary proving the saved `.a3p` is a readable project. |
| Alice project/domain APIs | Marker inspection boundary proving the readback payload contains `saveMenuDoClickRoundTripMarker`. |

Do not duplicate Save orchestration inside the proof. If a new proof needs a different Save path, add a separate narrowly named shard instead of widening this one.

## Configuration

Run from the repository root. Initialize the Tweedle grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the memory option for large Maven runs:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Run the focused proof with a display after implementing the strengthened readback and marker contract:

```bash
xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

If the environment already has a usable non-headless AWT display, `xvfb-run -a` is optional.

The outside-in QA scenario delegates to the same focused proof command and remains gated:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-save-menu-dialog-write-proof \
  --evidence-dir qa/outside-in/alice-desktop/evidence/save-menu-dialog-write-proof
```

Use the direct Maven command for the canonical proof artifact after the strengthened implementation lands. Use the QA scenario when review also needs the standard outside-in `status.txt` and `command.log` wrapper evidence.

The QA scenario executes the checked-in Maven argv directly. It relies on the ambient process environment for a usable display and options such as `NODE_OPTIONS`; it does not prepend `xvfb-run` or set memory options itself.

To collect dialog-discovery evidence for this proof, set:

```bash
-Dorg.alice.eatme.saveDialogDiscoveryEvidenceDir=target/save-dialog-proof-evidence
```

## Examples

### Target successful proof result

```json
{
  "status": "proven",
  "dialogType": "Swing JFileChooser",
  "wroteFile": true,
  "claim": "Save menu item doClick opened a Swing JFileChooser, approved the selected .a3p path, wrote a non-empty project file, read it back, and verified saveMenuDoClickRoundTripMarker",
  "trigger": {
    "menu_item_doclick": true
  },
  "observed_dialog": {
    "approved_selection": true,
    "ambiguous_chooser_discovery": false
  },
  "selected_file": {
    "normalized_selected_file": "projects/doclick-save-proof.a3p"
  },
  "written_artifact": {
    "target_file": "projects/doclick-save-proof.a3p",
    "file_written": true,
    "file_extension": "a3p"
  },
  "readback": {
    "project_readable": true,
    "expected_marker": "saveMenuDoClickRoundTripMarker",
    "marker_present": true
  },
  "doesNotClaim": [
    "full desktop Save completion",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage",
    "all Save variants"
  ]
}
```

### Target preexisting target without full proof

A preexisting file at the expected target is not write proof. In the strengthened contract, if the complete menu, chooser, approval, write, readback, and marker chain does not complete, success-shaped fields remain false even when a file is already present.

```json
{
  "status": "not_proven",
  "reason": "save_menu_doclick_e2e_not_completed",
  "dialogType": "Swing JFileChooser",
  "wroteFile": false,
  "reporting_summary": "Save menu item doClick written-project path was not proven; chooser approval, file writing, readback, and marker verification remain unproven",
  "trigger": {
    "menu_item_doclick": false
  },
  "observed_dialog": {
    "approved_selection": false,
    "ambiguous_chooser_discovery": false
  },
  "written_artifact": {
    "target_file": "projects/doclick-save-proof.a3p",
    "file_written": false,
    "file_nonempty": false
  },
  "readback": {
    "project_readable": false,
    "expected_marker": "saveMenuDoClickRoundTripMarker",
    "marker_present": false
  }
}
```

### Target ambiguous chooser blocker

```json
{
  "status": "not_proven",
  "reason": "ambiguous_swing_jfilechooser_discovery",
  "dialogType": "Swing JFileChooser",
  "wroteFile": false,
  "reporting_summary": "Save menu item doClick written-project path was not proven; chooser approval, file writing, readback, and marker verification remain unproven",
  "observed_dialog": {
    "approved_selection": false,
    "ambiguous_chooser_discovery": true
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

### Target unsupported display result

```json
{
  "schema_version": "eatme.alice-desktop-stageide-save-menu-doclick-write-proof/v1",
  "status": "unsupported",
  "reason": "No available non-headless AWT display",
  "dialogType": "Swing JFileChooser",
  "wroteFile": false,
  "approved_selection": false,
  "file_written": false,
  "file_nonempty": false,
  "proofTarget": "Save menu/control/dialog/write/readback/marker path",
  "reporting_summary": "Save menu/control/dialog/write/readback/marker path requires a non-headless AWT display before it can be proven",
  "readback": {
    "project_readable": false,
    "expected_marker": "saveMenuDoClickRoundTripMarker",
    "marker_present": false
  },
  "blocker": {
    "observed": "GraphicsEnvironment.isHeadless() is true or no usable desktop display is available",
    "required": "Xvfb or another non-headless AWT display capable of showing a Swing JFileChooser"
  },
  "requiresNextEvidence": [
    "Run this proof shard under xvfb-run -a or an equivalent desktop session",
    "Save menu/control/dialog/write/readback/marker path artifact with status proven"
  ],
  "doesNotClaim": [
    "full desktop Save completion",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage"
  ]
}
```

## Non-claims

This strengthened proof shard must not claim:

1. Full desktop Save completion.
2. Full lesson completion.
3. Visible rendering correctness.
4. Grading correctness.
5. Physical user click.
6. Broad UI automation coverage.
7. Save As, backup Save, unwritable-file retry, repeated Save, overwrite prompt, cancellation, or all Save permutations.
8. Native `java.awt.FileDialog` display or control.
