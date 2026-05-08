# Desktop Procedure Edit and Save Automation

This reference describes the desktop-side automation path for observing a
procedure target, editing a procedure, and then saving the project. It names the
checked-in hook points, the live first-lesson procedure target
observation shard, the bounded Save proof for a real dialog/write path, and the
behavior that remains outside this slice.

## Current checked-in hook points

| User step | Class | What can be observed without launching the full desktop |
| --- | --- | --- |
| Open a procedure tab | `org.alice.ide.declarationseditor.ProcedureTabSelection` | Returns the Croquet `Operation` that selects a `UserMethod` procedure in a `DeclarationsEditorComposite`, has a guarded helper to fire it, and reports the currently selected procedure when one is active. |
| Run the current procedure edit implementation | `org.alice.tools.ProcedureEditCommand` | Applies the supported `append-comment` edit to the selected `UserMethod` and returns statement counts for `procedure-edit-command.json`. This is an implementation command, not a desktop code-editor command. |
| Select a procedure tab in Alice | `org.alice.ide.declarationseditor.DeclarationTabState` | Owns the real tab-selection operation used by the desktop declarations editor. |
| Show procedure code | `org.alice.ide.declarationseditor.CodeComposite` | Wraps the selected `UserMethod` and creates the code view when the desktop activates the tab. |
| Save the current project | `org.alice.ide.croquet.models.projecturi.SaveProjectOperation` | Keeps the user-facing Save command, prompt rule, icon, and toolbar behavior. |
| Run the save flow | `org.alice.ide.croquet.models.projecturi.SaveOperationFlow` | Covers prompt, cancel, retry, wait cursor, error, finish, and save-callback behavior without Swing dialogs. Returns whether the flow finished or canceled, how many prompts and save attempts ran, and which file saved after `saveProjectTo(File)` returned. |
| Connect Save to live Alice objects | `org.alice.ide.croquet.models.projecturi.AbstractSaveOperation` | Adapts the active `StageIDE`, `ProjectDocumentFrame`, Croquet `UserActivity`, wait cursor, and `ProjectApplication.saveProjectTo(File)` to `SaveOperationFlow`. |
| Prove Save menu/dialog/write path | `org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest` | Canonical shard for activating the real Save menu item with `doClick()`, controlling exactly one expected live Swing `JFileChooser`, approving a normalized temp-directory `.a3p` target, and asserting a non-empty project file write. |

`ProcedureTabSelection` is intentionally small. It does not edit code. It gives a
desktop automation runner one stable place to ask, "which real Croquet operation
selects this procedure tab?" The helper refuses to fire the operation until a
live Alice IDE is active, because the tab change creates the desktop code view.

## Live target observation and proposed next hooks

The live target observation shard owns the smallest unevidenced transition after
Select Project opens the configured first-lesson flow starter:

```text
Select Project opened first-lesson project
  -> live Alice desktop post-open
  -> procedure tab or code-editor target for scene.eatmeFirstLesson observable
```

The runner contract is documented in [First-Lesson Live Procedure Target
Observation](./first-lesson-live-procedure-target-observation.md). It is now a
supported read-only scenario and may be used only as observed-or-blocked target
evidence for the next desktop edit shard.

The command shape is:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

The decision artifact is `first-lesson-live-procedure-target-observation.json`.
`status=edit-ready` means a stable `scene.eatmeFirstLesson` target was found and
has a public desktop edit invocation contract for the next proof.
`status=blocked` means the artifact names the exact target, display/accessibility,
or public `CodeEditor`/`CodeComposite` edit-action contract blocker. In both
cases the shard is read-only: it does not mutate the procedure, save the project,
assert rendering correctness, assess learner work, or claim full first-lesson
completion.

Add the remaining hooks in order, each with a focused test before changing
behavior:

1. Add or strengthen a narrow code-editor observation helper that accepts the
   active `DeclarationsEditorComposite` and reports the selected `CodeComposite`
   and `CodeEditor.getCode()` value. This should prove the procedure tab is
   active, not that any visual layout is correct.
2. Replace the implementation edit command with a desktop code-editor edit hook
   only after the code editor exposes a real command for the intended edit. The
   hook should invoke that command; it should not call the implementation command
   a desktop edit.
3. Use `StageIdeSaveMenuDoClickToWriteProofTest` as the bounded Save proof shard
    for menu activation, Swing chooser approval, and `.a3p` write evidence. Keep
    procedure-edit automation separate from this Save proof.

## Test plan

Run the headless procedure target test:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.declarationseditor.ProcedureTabSelectionTest \
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

Run the outside-in desktop scenario only when a real display is prepared:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The first-lesson live procedure target action seam shard is for reviews about
the live desktop target and edit-action readiness after Select Project opens the
configured first-lesson flow starter. Its runner command is:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

Use its evidence only for the procedure tab/code-editor target observation seam;
do not cite it as edit, Save, rendering, learner assessment, or full
first-lesson completion proof.

## Still unproven

This slice does not prove any of the following:

- The code editor can perform the requested procedure edit through a desktop
  command.
- Save dialogs can be controlled for Save As, backup saves, or unwritable files.
- First-lesson completion, grading, visual rendering, or creative assessment.
