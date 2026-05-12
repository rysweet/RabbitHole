# Desktop Procedure Edit and Save Automation

This reference describes the desktop-side automation path for observing a
procedure target, editing a procedure, and then saving the project. It names the
checked-in hook points, the live first-lesson procedure target action
seam, the historical dialog/write baseline, the joined rendered Robot Save seam,
and the behavior that remains outside this slice.

## Current checked-in hook points

| User step | Class | What can be observed without launching the full desktop |
| --- | --- | --- |
| Open a procedure tab | `org.alice.ide.declarationseditor.ProcedureTabSelection` | Returns the Croquet `Operation` that selects a `UserMethod` procedure in a `DeclarationsEditorComposite`, has a guarded helper to fire it, reports the selected procedure, exposes the selected `CodeComposite`, and observes the backing `CodeEditor.getCode()` model. |
| Run the current procedure edit implementation | `org.alice.tools.EatmeEditProcedure` | Applies the supported `append-comment` edit to any valid `scene.<methodName>` target `UserMethod` and writes `first-lesson-code-editor-action-proof.json`. The canonical proof uses `scene.eatmeFirstLesson`; starter projects like `africa.a3p` use `scene.myFirstMethod`. This is a focused backing/action proof, not broad desktop UI automation. |
| Select a procedure tab in Alice | `org.alice.ide.declarationseditor.DeclarationTabState` | Owns the real tab-selection operation used by the desktop declarations editor. |
| Show procedure code | `org.alice.ide.declarationseditor.CodeComposite` | Wraps the selected `UserMethod` and creates the code view when the desktop activates the tab. |
| Save the current project | `org.alice.ide.croquet.models.projecturi.SaveProjectOperation` | Keeps the user-facing Save command, prompt rule, icon, and toolbar behavior. |
| Run the save flow | `org.alice.ide.croquet.models.projecturi.SaveOperationFlow` | Covers prompt, cancel, retry, wait cursor, error, finish, and save-callback behavior without Swing dialogs. Returns whether the flow finished or canceled, how many prompts and save attempts ran, and which file saved after `saveProjectTo(File)` returned. |
| Connect Save to live Alice objects | `org.alice.ide.croquet.models.projecturi.AbstractSaveOperation` | Adapts the active `StageIDE`, `ProjectDocumentFrame`, Croquet `UserActivity`, wait cursor, and `ProjectApplication.saveProjectTo(File)` to `SaveOperationFlow`. |
| Preserve Save menu dialog/write baseline | `org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest` | Supporting shard for activating the real Save menu item with `doClick()`, controlling exactly one expected live Swing `JFileChooser`, approving a normalized temp-directory `.a3p` target, and asserting a non-empty project file write. |
| Prove Robot Save menu/dialog/write/readback path | `org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest` | Canonical shard for AWT Robot File-menu Save activation, live Swing chooser control, proof-root `.a3p` write, `IoUtilities.readProject(File)` readback, marker verification, or an executable blocker artifact. |

`ProcedureTabSelection` is intentionally small. It does not edit code. It gives a
desktop automation runner one stable place to ask, "which real Croquet operation
selects this procedure tab?" and one read-only way to confirm the selected
procedure, selected `CodeComposite`, and selected `CodeEditor.getCode()` model.
The guarded live helper refuses to fire the selection operation until a live
Alice IDE is active, because the tab change creates the desktop code view.

`RobotSaveMenuDialogWriteReadbackProofTest` is the joined Save seam. It either
proves AWT Robot File-menu Save activation, live Swing chooser control,
proof-root `.a3p` write, `IoUtilities.readProject(File)` readback, and marker
verification, or writes a machine-readable blocker artifact.

## Live target action seam and code-editor backing hook

The live target action seam owns the smallest unevidenced transition
after Select Project opens the configured first-lesson flow starter:

```text
Select Project opened first-lesson project
  -> live Alice desktop post-open
  -> procedure tab or code-editor target for scene.eatmeFirstLesson observable
  -> target classified as edit-ready or blocked by the named edit-action contract gap
```

The runner contract is documented in [First-Lesson Live Procedure Target
Action Seam](./first-lesson-live-procedure-target-observation.md). Use that
contract as the implementation target for edit-ready-or-named-blocker evidence
for the next desktop edit shard.

The command shape is:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

The decision artifact is
`first-lesson-live-procedure-target-observation.json`. `status=edit-ready` means
a stable `scene.eatmeFirstLesson` target was found and has a public desktop edit
invocation contract for the next proof. The only accepted action-seam no-go is
`blocker.kind=missing-desktop-edit-action-contract` with
`blocker.message=missing public CodeEditor/CodeComposite edit invocation
contract`. Display, AT-SPI, and target-not-found blockers are structured run
failures, not accepted action-seam proof. In every case the shard remains
read-only: it does not mutate the procedure, save the project, assert rendering
correctness, assess learner work, assess creative quality, or claim full
first-lesson completion.

The hook after the live target/action seam is the procedure tab code-editor
backing proof. Its contract is documented in [First-Lesson Procedure Tab
Code-Editor Backing](./first-lesson-procedure-tab-code-editor-backing.md). It
proves that selecting `scene.eatmeFirstLesson` lands on the expected
`CodeComposite` and that the selected tab's backing code-editor model reports
the same `UserMethod` from `getCode()`. It remains read-only: it does not invoke
a desktop edit action, mutate AST statements, save the project, assert
rendering, assess learner work, assess creative quality, or claim first-lesson
completion.

Add the remaining hooks in order, each with a focused test before changing
behavior:

1. Replace the implementation edit command with a desktop code-editor edit hook
   only after the code editor exposes a real command for the intended edit. The
   hook should invoke that command; it should not call the implementation command
   a desktop edit.
2. Use `RobotSaveMenuDialogWriteReadbackProofTest` as the joined rendered Save
   proof shard for Robot File-menu activation, Swing chooser approval, `.a3p`
   write, readback, and marker evidence. Keep procedure-edit automation
   separate from this Save proof.

## Test plan

Run the current headless procedure target test when the existing selection
operation or selected-procedure behavior changes:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.declarationseditor.ProcedureTabSelectionTest \
  test
```

Run the code-editor backing seam canonical method when tab/model backing
behavior changes:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.declarationseditor.ProcedureTabSelectionTest#selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode \
  test
```

Run the headless edit proof tests when `EatmeEditProcedure` or the edit command
artifact changes:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeEditProcedureTest,org.alice.ide.declarationseditor.ProcedureTabSelectionTest \
  test
```

Run the existing save-flow tests when Save routing changes:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.SaveOperationFlowTest,org.alice.ide.croquet.models.projecturi.SaveOperationCompletionEvidenceTest,org.alice.ide.croquet.models.projecturi.SaveDialogDiscoveryTargetEvidenceTest,org.alice.ide.croquet.models.projecturi.SaveProjectOperationTest \
  test
```

Run the bounded Save menu/dialog/write proof only when a usable display is
prepared:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

The intended executable display-precondition blocker is `No available non-headless AWT display`.

Run the joined Robot Save menu/dialog/write/readback proof only when a usable
display is prepared:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest,org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest,org.alice.ide.croquet.models.projecturi.JMenuBarRobotClickSaveProofTest,org.alice.ide.ProjectApplicationSaveProjectToTest \
  -Dorg.alice.eatme.saveProof.scenario=alice-desktop-save-menu-dialog-write-proof \
  -Dorg.alice.eatme.saveProof.runId=save-proof-$(date -u +%Y%m%dT%H%M%SZ)-manual \
  -Dorg.alice.eatme.saveProof.evidencePath=core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json \
  test
```

The target Robot proof writes `robot-save-menu-dialog-write-readback-proof.json` under
`core/ide/target/save-menu-proofs/`. Use `status: "proven"` only for the
complete Robot/menu/dialog/control/write/readback/marker path. Use
`status: "blocked"` as the executable blocker result when the environment or UI
state makes the combined path unsafe to prove. The outside-in Save proof scenario
fails closed on blocked, stale, partial, or internally inconsistent evidence and
does not use workflow-level timeout wiring. Bounded waits belong inside the Java
proof and must produce named blockers instead of relying on shell timeout.

When `org.alice.eatme.saveOperationEvidenceDir` is set for flow-seam-only Save
runs, Save operation evidence also writes
`desktop-save-dialog-control-target.json`. This artifact names the dialog seams
that the canonical Save menu/dialog/write proof exercises and reports
`unsupported` when no Save dialog was requested.

When `org.alice.eatme.saveDialogDiscoveryEvidenceDir` is set,
`FileDialogUtilities.showSaveFileDialog(Component,File,String,String)` also
writes `desktop-save-dialog-discovery-target.json` before displaying the Swing
Save chooser. In headless or rootless tests this is a machine-readable no-go
artifact; in a real desktop it records owner/root target resolution before the
bounded Save proof controls the chooser.

Use the menu/action smoke only when a review needs the bounded Window menu model
registration contract. It is not Save evidence and does not drive a live Swing
menu:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The first-lesson live procedure target action seam shard is for reviews
about the live desktop target and edit-action readiness after Select Project
opens the configured first-lesson flow starter. Its runner command is:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

Use its evidence only for the procedure tab/code-editor target action
seam; do not cite it as edit, Save, rendering, learner assessment, creative
assessment, or full first-lesson completion proof.

## Still unproven

This slice does not prove any of the following:

- The code editor can perform the requested procedure edit through a desktop
  command.
- Save dialogs can be controlled for Save As, backup saves, or unwritable files.
- First-lesson completion, grading, visual rendering, or creative assessment.
