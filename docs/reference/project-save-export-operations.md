# Project Save and Export Operations

This reference describes the `core/ide` project Save, Save As, and Export operation layer and the characterization-test feature for it.

## Contents

- [Package](#package)
- [Characterization scope](#characterization-scope)
- [Operation responsibilities](#operation-responsibilities)
- [User-visible behavior](#user-visible-behavior)
- [Archive reopen/edit/export seam](#archive-reopeneditexport-seam)
- [API reference](#api-reference)
- [Evidence artifacts](#evidence-artifacts)
- [Testing notes](#testing-notes)
- [Validation pointer](#validation-pointer)
- [Save menu dialog write proof](#save-menu-dialog-write-proof)
- [Robot Save menu dialog write/readback proof](#robot-save-menu-dialog-writereadback-proof)
- [Configuration](#configuration)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Package

The save/export operation layer is implemented in:

```text
core/ide/src/main/java/org/alice/ide/croquet/models/projecturi/
```

Characterization tests for this layer live in the matching test package:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/
```

The operation layer routes Croquet actions to `ProjectApplication` save/export
behavior. Archive file contents remain owned by lower-level classes that write
Alice project archives, reopen them, edit them, write them again, reopen them
again, and export them.

## Characterization scope

The characterization-test layer covers existing operation behavior. It does not change production Save, Save As, or Export behavior. The documented modernization seam is the `SaveOperationFlow` routing layer plus the `SaveOperationCompletionEvidence` evidence writer used by `AbstractSaveOperation.perform(UserActivity)`.

The operation coverage plan is organized as:

| Scope | Expected implementation |
| --- | --- |
| Direct operation tests | JUnit 4 tests in `org.alice.ide.croquet.models.projecturi` cover prompt rules, extension selection, and toolbar text clobbering. |
| Flow seam tests | `SaveOperationFlowTest` covers observable routing, prompt consultation, backup-save naming, retry behavior, wait cursor wrapping, finish/cancel outcomes, and returned result counts. |
| Completion result evidence tests | `SaveOperationCompletionEvidenceTest` covers opt-in result and dialog-control artifacts, finished/canceled/incomplete-style result summaries, `wroteFile` claim boundaries, path redaction, missing-active-`StageIDE` proof, and non-interruption when evidence writing fails. |
| Action-invocation proof tests | `StageIdeSaveActionInvocationProofTest` and `StageIdeSaveMenuItemDispatchProofTest` cover proof-only active-`StageIDE`/document-frame and Swing menu dispatch paths where the environment supports them. The `missing_project_document_frame` status remains part of the artifact contract for the production guard path and should be exercised by any future test seam that can construct that state without unsafe desktop interception. |
| Compatibility assertions | Assert the exact behavior currently implemented by `SaveProjectOperation`, `SaveAsProjectOperation`, and `ExportProjectOperation`. |
| Portable file fixtures | Use real files from JUnit `TemporaryFolder`; use a missing file as the portable "cannot be reused" Save case. |

These areas stay outside direct operation tests:

| Deferred scope | Reason |
| --- | --- |
| Archive content verification | Archive bytes and project serialization belong to lower-level tests that write Alice project archives, reopen them, edit them, write them again, reopen them again, and export them, not operation-routing tests. |
| New mocking framework | Save characterization uses existing JUnit 4 patterns and narrow production seams instead of PowerMock-style interception. |
| Display-backed Swing/JavaFX testing | Desktop launch evidence belongs to the outside-in QA lane and Xvfb-backed scenarios. |

## Operation responsibilities

| Class | Responsibility |
| --- | --- |
| `AbstractSaveOperation` | Coordinates active `StageIDE` lookup, current project URI detection, save dialog routing, backup copy naming, wait cursor handling, `IOException` retry behavior, and activity finish/cancel. |
| `SaveOperationFlow` | Provides the package-private, UI-free flow seam for current-file reuse, prompt selection, backup prompt naming, save attempts, retry after `IOException`, and observable result reporting. |
| `SaveOperationCompletionEvidence` | Writes opt-in save action, dialog-control target, and completion evidence artifacts without changing the save result. |
| `AbstractSaveProjectOperation` | Supplies the default projects directory, Alice project extension, and `ProjectApplication.saveProjectTo(File)` delegation used by project save operations. |
| `SaveProjectOperation` | Saves to the current writable file without prompting; prompts when no writable project file exists. |
| `SaveAsProjectOperation` | Always prompts for a destination and saves an Alice project file. |
| `ExportProjectOperation` | Always prompts for a destination and delegates to project export. |

## User-visible behavior

| User action | Prompt behavior | Extension | Delegation |
| --- | --- | --- | --- |
| Save | Prompt only when the current file is `null` or cannot be written. | `IoUtilities.PROJECT_EXTENSION` | `ProjectApplication.saveProjectTo(File)` |
| Save As | Always prompt. | `IoUtilities.PROJECT_EXTENSION` | `ProjectApplication.saveProjectTo(File)` |
| Export | Always prompt. | `IoUtilities.EXPORT_EXTENSION` | `ProjectApplication.exportProjectTo(File)` |

When Alice is saving a backup copy, the save dialog uses the main project file base name plus ` Copy`. For example, a main project file named `RobotDance.a3p` prompts with `RobotDance Copy`. If the main project file is not available, the dialog receives an empty suggested base name.

If the user cancels the save dialog, the Croquet `UserActivity` is canceled and no save/export call is made. If a save/export call succeeds, the activity is finished.

If a save/export call raises `IOException`, Alice shows an error dialog, hides the wait cursor, and prompts again. Current-file Save retries suggest the current project base name. Prompted Save As or Export-style retries also keep the current project base name when one exists; if there is no current file, the retry prompt has no suggested base name. The characterized retry loop continues until the user cancels or a later save/export attempt succeeds.

Completion evidence is developer opt-in and is not user-visible. Evidence writer failures are logged and do not convert a successful save/export into a failed save/export.

## Archive reopen/edit/export seam

The lower-level archive seam proves that Alice project bytes remain editable
after a save/reopen/edit cycle. It is separate from the desktop operation layer:
Save, Save As, and Export actions delegate to `ProjectApplication`, while
archive persistence is characterized in `core/story-api-migration`.

The canonical archive journey is:

```text
IoUtilities.writeProject(original.a3p, project)
-> IoUtilities.readProject(original.a3p)
-> edit reopened Project-owned state
-> IoUtilities.writeProject(edited.a3p, reopenedProject)
-> IoUtilities.readProject(edited.a3p)
-> IoUtilities.exportProject(edited.a3w, editedProject)
```

`IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`
backs this contract. The required assertion is the edited project-owned state
after the second `IoUtilities.readProject` call; a non-empty file, a successful
first reopen, or an export file alone is not enough.

Run the focused archive validation from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```

This validation supports only the repository-owned archive read/write/export
claim. It does not prove desktop Save-menu completion, full Save dialog
automation, visible rendering correctness, grading, full lesson automation, or
player runtime behavior. See
[Project Archive Reopen/Edit Seam](./project-archive-reopen-edit-seam.md) for
the detailed archive contract.

## API reference

### `AbstractSaveOperation`

`AbstractSaveOperation` defines the operation template:

```java
protected abstract boolean isPromptNecessary(File file);
protected abstract File getDefaultDirectory(StageIDE application);
protected abstract String getExtension();
protected abstract void save(ProjectApplication application, File file) throws IOException;
```

Concrete operations customize only the prompt rule, default directory, file extension, and final save/export delegation. The template owns the shared flow so Save, Save As, and Export stay behaviorally consistent.

### `SaveOperationFlow`

`SaveOperationFlow` is the package-private seam that makes the save routing behavior testable without desktop UI interception:

```java
static Result run(Context context, PromptDecision promptDecision, String extension, SaveAction saveAction)
```

The `Context` supplies the current file, backup state, default directory, dialog callback, wait cursor hooks, error display hook, and activity finish/cancel hooks. `PromptDecision` supplies the operation-specific prompt rule. `SaveAction` performs the final save/export callback and may throw `IOException`.

`Result` records observable completion facts:

| Field | Meaning |
| --- | --- |
| `finished` | `true` when the flow called the activity finish hook after a successful save/export attempt. |
| `canceled` | `true` when the flow called the activity cancel hook after dialog cancellation or unsupported setup. |
| `promptCount` | Number of save dialog requests made by the flow. |
| `saveAttempts` | Number of save/export callback attempts. |
| `savedFile` | The last successfully saved destination, or `null` when no successful write completed. |

The flow has no runtime configuration and no public API commitment outside the package. Its contract is the behavior preserved by `AbstractSaveOperation.perform(UserActivity)`.

### `SaveOperationCompletionEvidence`

`SaveOperationCompletionEvidence` is package-private developer evidence infrastructure. It writes artifacts only when explicitly opted in by system property:

```java
static void record(String operationClass, String extension, SaveOperationFlow.Result result)
static void recordSaveActionInvocation(String operationClass, String extension, boolean activeStageIdeAvailable, boolean projectDocumentFrameAvailable)
static Path write(Path evidenceDir, String operationClass, String extension, SaveOperationFlow.Result result)
static Path writeDialogControlTarget(Path evidenceDir, String operationClass, String extension, SaveOperationFlow.Result result)
static Path writeSaveActionInvocationProof(Path evidenceDir, String operationClass, String extension, boolean activeStageIdeAvailable, boolean projectDocumentFrameAvailable)
```

`record(...)` is non-authoritative telemetry for tests and modernization evidence. It is not part of the user save contract. A failure to create evidence is logged and does not change the `UserActivity` outcome.

Evidence path handling is bounded to the configured evidence directory. Saved paths under the current checkout are written relative to the checkout; absolute saved paths outside the checkout are redacted to `[redacted]/<file-name>`.

### `AbstractSaveProjectOperation`

`AbstractSaveProjectOperation` provides the project-save defaults:

```java
protected File getDefaultDirectory(StageIDE application);
protected String getExtension();
protected void save(ProjectApplication application, File file) throws IOException;
```

It uses `StageIDE.getProjectsDirectory()` as the default directory, `IoUtilities.PROJECT_EXTENSION` as the file extension, and `ProjectApplication.saveProjectTo(File)` as the save callback.

### `SaveProjectOperation`

Use the singleton instance:

```java
SaveProjectOperation operation = SaveProjectOperation.getInstance();
```

`SaveProjectOperation` prompts only when the current project file cannot be reused:

```java
((file != null) && file.canWrite()) == false
```

Toolbar text is clobbered for this operation, and the small save-document icon is used.

### `SaveAsProjectOperation`

Use the singleton instance:

```java
SaveAsProjectOperation operation = SaveAsProjectOperation.getInstance();
```

`SaveAsProjectOperation` always prompts, even when the current project file is writable.

### `ExportProjectOperation`

Create a new operation instance where export is needed:

```java
ExportProjectOperation operation = new ExportProjectOperation();
```

`ExportProjectOperation` always prompts, uses `IoUtilities.EXPORT_EXTENSION`, clobbers toolbar text, and delegates to `ProjectApplication.exportProjectTo(File)`.

## Evidence artifacts

Save operation evidence is written only when `org.alice.eatme.saveOperationEvidenceDir` is set to a non-blank directory.

| Artifact | Schema version | When written | Contract |
| --- | --- | --- | --- |
| `desktop-save-action-invocation-proof.json` | `eatme.alice-desktop-save-action-invocation-proof/v1` | Before the flow requests a dialog or save callback, when evidence is enabled. | Records whether the action reached `AbstractSaveOperation.perform`, whether `StageIDE.getActiveInstance()` and the project document frame were available, whether invocation came from a Swing menu item dispatch, and which next evidence is required. This artifact may be proof-only when `org.alice.eatme.saveActionInvocationProofOnly=true`. |
| `desktop-save-operation-result.json` | `eatme.alice-desktop-save-operation-result/v1` | After `SaveOperationFlow.run(...)` returns. | Records status, prompt count, save attempts, saved file metadata, and whether the evidence proves a non-empty file write with the expected extension. |
| `desktop-save-dialog-control-target.json` | `eatme.alice-desktop-save-dialog-control-target/v1` | Alongside completion evidence. | Reports whether the flow requested the production dialog seam and identifies the desktop dialog-control targets still needed for outside-in proof. |

Completion statuses are intentionally narrow:

| Status | Meaning |
| --- | --- |
| `finished` | The flow finished after a save/export callback completed without `IOException`. |
| `canceled` | The flow canceled because the user canceled a prompt, the retry prompt was canceled, or the action could not safely continue. |
| `incomplete` | The result did not finish or cancel; this is failure-style evidence and does not claim a completed save. |

Action invocation proof statuses describe how far the action reached:

| Status | Meaning |
| --- | --- |
| `action_invoked` | The operation reached `AbstractSaveOperation.perform` with an active `StageIDE` and document frame. |
| `menu_item_dispatched` | The operation was dispatched through the Swing menu item path and reached the operation owner. |
| `blocked` | The action was invoked but a required desktop owner, such as the project document frame, was missing. |
| `unsupported` | The current JVM has no active `StageIDE`, so the production dialog path cannot be reached. |

The `wroteFile` evidence field is computed from the recorded `saved_file`: it is `true` only when that file exists, is non-empty, and matches the operation extension. Only that state includes the file-write claim. Normal `SaveOperationFlow` canceled or incomplete results do not record a saved file and therefore emit only a reporting summary, but consumers should not treat status alone as the JSON invariant.

## Testing notes

Direct operation tests live in the same package as the operations. Those tests can call protected prompt and extension methods without changing production visibility.

| Behavior | Direct test target |
| --- | --- |
| Save prompts when the current file is `null` | `SaveProjectOperation.isPromptNecessary(null)` |
| Save does not prompt for a writable current file | `SaveProjectOperation.isPromptNecessary(writableFile)` |
| Save prompts for a current file that cannot be reused | `SaveProjectOperation.isPromptNecessary(missingFile)` |
| Save uses project extension | `SaveProjectOperation.getExtension()` |
| Save clobbers toolbar text | `SaveProjectOperation.isToolBarTextClobbered()` |
| Save As always prompts | `SaveAsProjectOperation.isPromptNecessary(...)` |
| Save As uses project extension | `SaveAsProjectOperation.getExtension()` |
| Export always prompts | `ExportProjectOperation.isPromptNecessary(...)` |
| Export uses export extension | `ExportProjectOperation.getExtension()` |
| Export clobbers toolbar text | `ExportProjectOperation.isToolBarTextClobbered()` |

Flow-level tests use the package-private `SaveOperationFlow` seam so they can avoid UI/static interception while preserving the production adapter in `AbstractSaveOperation.perform(UserActivity)`.

| Flow behavior | Flow test target |
| --- | --- |
| Writable current file saves without prompting | `SaveOperationFlowTest` supplies a writable file and asserts save callback, wait cursor, and finish behavior. |
| Prompt cancellation stops the action | `SaveOperationFlowTest` returns `null` from the dialog and asserts no save callback and canceled activity. |
| Backup save copy naming | `SaveOperationFlowTest` supplies the main project file and asserts the prompted base name ends with ` Copy`. |
| Current-file Save `IOException` retry | `SaveOperationFlowTest` throws from the first callback, asserts error reporting and wait cursor cleanup, then succeeds on retry. |
| Current-file Save `IOException` followed by cancel | `SaveOperationFlowTest` throws from the first callback, returns `null` from the retry prompt, and asserts canceled activity. |
| Prompted current-project retry | `SaveOperationFlowTest` prompts first, fails the selected destination, and asserts retry keeps the current project base name. |
| Prompted no-current-file retry cancellation | `SaveOperationFlowTest` prompts first, fails the selected destination, and asserts retry cancellation keeps no suggested base name. |

Evidence tests characterize the artifact contract rather than constructor or accessor behavior. Existing tests cover the completed result and proof-only paths listed below; future changes to the action-invocation guard should add focused coverage for any newly reachable status.

| Evidence behavior | Test target or coverage expectation |
| --- | --- |
| Successful completion evidence | `SaveOperationCompletionEvidenceTest` writes a finished result with a non-empty `.a3p` file and asserts `status: finished`, `wroteFile: true`, file metadata, and the bounded write claim. |
| Failure-style completion evidence | `SaveOperationCompletionEvidenceTest` writes an incomplete or non-writing result and asserts no write claim is emitted. |
| Cancellation evidence | `SaveOperationCompletionEvidenceTest` writes a canceled result and asserts `status: canceled`, null saved-file fields, `wroteFile: false`, and a reporting summary. |
| Action invocation success | `StageIdeSaveActionInvocationProofTest` asserts `action_invoked` when the operation reaches an active `StageIDE` and project document frame; headless runs assert the bounded `unsupported` artifact instead. |
| Menu-item dispatch proof | `StageIdeSaveMenuItemDispatchProofTest` asserts `menu_item_dispatched` for the Swing Save menu item path and verifies direct `fire(...)` does not pretend to be a menu click. |
| Action invocation blocked/unsupported | `SaveOperationCompletionEvidenceTest` asserts `unsupported` for a missing active `StageIDE`. The `blocked` / `missing_project_document_frame` state is a documented production guard and should be covered by a dedicated seam before any behavior change depends on it. |
| Opt-in behavior | `SaveOperationCompletionEvidenceTest` asserts no artifacts are written when `org.alice.eatme.saveOperationEvidenceDir` is unset. |
| Evidence safety | `SaveOperationCompletionEvidenceTest` asserts saved paths outside the checkout are redacted and evidence writing does not interrupt save flow; production logs evidence writer errors. |

## Validation pointer

When changing this seam, run the focused characterization tests that back the behavioral contract:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.SaveOperationFlowTest,org.alice.ide.croquet.models.projecturi.SaveOperationCompletionEvidenceTest,org.alice.ide.croquet.models.projecturi.StageIdeSaveActionInvocationProofTest,org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuItemDispatchProofTest \
  test
```

Display-backed proof tests remain intentionally separate and are documented in their proof-specific how-to pages. Coverage claims for this shard should be limited to `SaveOperationFlow` and `SaveOperationCompletionEvidence` behavior backed by these tests; do not infer aggregate target progress from trivial accessor tests or from desktop proof tests that exercise a narrower path.

## Save menu dialog write proof

The historical bounded desktop proof is `StageIdeSaveMenuDoClickToWriteProofTest`. Its target contract proves one path only: production Save menu item `doClick()`, live Swing `JFileChooser` approval, and a non-empty `.a3p` write under the JUnit temp directory.

This proof target is stronger than direct operation tests and flow-seam tests because it starts from the Save menu item and reaches the real dialog/write boundary. It is still a supporting baseline. It does not satisfy the rendered Save proof scenario by itself because it bypasses Robot activation of the rendered File menu.

| Proof boundary | Required observation |
| --- | --- |
| Menu activation | The Save menu item is created through `SaveProjectOperation.getInstance().getMenuItemPrepModel().createMenuItemAndAddTo(...)` and activated with `doClick()`. |
| Dialog control | Exactly one expected Swing `JFileChooser` is observed, receives the normalized temp-directory `.a3p` target, and is approved. |
| Write result | The target `.a3p` exists and has non-zero size after `ProjectApplication.saveProjectTo(File)` returns. |
| Evidence | The `stageide-save-menu-doclick-write-proof.json` artifact reports the dialog type as `Swing JFileChooser`, uses `status: proven`, `status: not_proven`, or `status: unsupported`, records `trigger.menu_item_doclick: true` only after the menu seam is reached, and sets `wroteFile` to `true` only after the file assertions pass. |

See [Save Menu Dialog Write/Readback Proof](./save-menu-dialog-write-proof.md) for the rendered scenario contract and [Save Proof Evidence](./save-proof-evidence.md) for the canonical artifact schema.

## Robot Save menu dialog write/readback proof

The target canonical rendered Save proof is `RobotSaveMenuDialogWriteReadbackProofTest`. Its contract proves one path only: AWT Robot opens the rendered File menu, AWT Robot clicks the production Save menu item by `SaveProjectOperation` action identity, exactly one live Swing `JFileChooser` is controlled, a proof-root `.a3p` file is written, the file reads back through `IoUtilities.readProject(File)`, and the readback project contains `robotSaveMenuRoundTripMarker`.

This proof is stronger than `JMenuBarRobotClickSaveProofTest` because it continues past menu dispatch into dialog control, file write, readback, and marker verification. It is stronger than `StageIdeSaveMenuDoClickToWriteProofTest` for menu attribution because it uses Robot mouse events instead of `doClick()`. It still does not claim Save behavior outside this single rendered path or all Save variants.

| Proof boundary | Required observation |
| --- | --- |
| Robot menu activation | The rendered File menu popup opens after AWT Robot mouse press/release at the File menu location. |
| Save item attribution | The clicked item is matched by `SaveProjectOperation` Swing action identity, not by label alone. |
| Dialog control | Exactly one expected Swing `JFileChooser` is observed, receives a normalized proof-root `.a3p` target, and completes approval. |
| Write/readback | The target `.a3p` exists, is non-empty, reads back with `IoUtilities.readProject(File)`, and contains `robotSaveMenuRoundTripMarker`. |
| Evidence | `robot-save-menu-dialog-write-readback-proof.json` must match `eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1`, report `status: proven` only after the complete chain succeeds, and fail scenario validation for missing, stale, blocked, partial, or internally inconsistent evidence. A `status: blocked` artifact is the executable blocker for the missing step, not passing proof evidence. Bounded Java waits replace shell timeout for menu, dialog, write, readback, and marker blockers. |

See [Save Proof Evidence](./save-proof-evidence.md) for the artifact schema, blocker contract, and fail-closed validation rules.

## Configuration

There is no runtime configuration flag for Save, Save As, or Export routing. The behavior is fixed by the operation classes and the Alice project save and export constants.

Developer evidence uses these optional JVM system properties:

| Property | Purpose |
| --- | --- |
| `org.alice.eatme.saveOperationEvidenceDir` | Enables save-operation evidence artifacts and selects the output directory. When unset or blank, no evidence artifacts are written. |
| `org.alice.eatme.saveActionInvocationProofOnly` | When evidence is enabled, stops after action-invocation evidence and cancels the activity before dialog or save callbacks. This is for bounded proof scenarios only, not normal user saves. |

Developer validation uses the existing Maven configuration:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  test
```

Run the bounded Save menu/dialog/write proof with a usable display:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest \
  test
```

Run the joined Robot Save menu/dialog/write/readback proof with the required baselines:

```bash
NODE_OPTIONS=--max-old-space-size=32768 xvfb-run -a mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest,org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest,org.alice.ide.croquet.models.projecturi.JMenuBarRobotClickSaveProofTest,org.alice.ide.ProjectApplicationSaveProjectToTest \
  test
```

For broad Maven validation from a fresh checkout or worktree, initialize the Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Compatibility rules

Tests and refactors in this package preserve these rules:

1. `SaveProjectOperation` keeps the exact writable-file prompt rule.
2. `SaveAsProjectOperation` always prompts.
3. `ExportProjectOperation` always prompts.
4. Project saves use `IoUtilities.PROJECT_EXTENSION`.
5. Project exports use `IoUtilities.EXPORT_EXTENSION`.
6. Save and Save As delegate to `ProjectApplication.saveProjectTo(File)`.
7. Export delegates to `ProjectApplication.exportProjectTo(File)`.
8. A successful save/export calls `UserActivity.finish()`.
9. A canceled dialog calls `UserActivity.cancel()` and does not save.
10. Characterized `IOException` retry paths keep retry loop behavior and surface the error.
11. Wait cursor show/hide wraps every attempted save/export.
12. Public operation identities and UUIDs stay unchanged.
13. Archive reopen/edit claims come only from `core/story-api-migration`
    `IoUtilities` validation, not from operation routing, menu dispatch, or
    desktop rendering evidence.

## Examples

### Save to the current project

When the active project URI resolves to a writable file:

```text
Action: Save
Current file: /home/dev/alice-projects/RobotDance.a3p
Prompt: no
Delegation: ProjectApplication.saveProjectTo(/home/dev/alice-projects/RobotDance.a3p)
Result: UserActivity.finish()
```

### Save when no writable project file exists

When there is no current file, or the current file cannot be written:

```text
Action: Save
Current file: null
Prompt: yes
Extension: IoUtilities.PROJECT_EXTENSION
Delegation after selection: ProjectApplication.saveProjectTo(selectedFile)
```

### Export a project

Export always asks for an export destination:

```text
Action: Export
Current file: /home/dev/alice-projects/RobotDance.a3p
Prompt: yes
Extension: IoUtilities.EXPORT_EXTENSION
Delegation after selection: ProjectApplication.exportProjectTo(selectedFile)
```

### Validate archive reopen/edit persistence

When reviewing the underlying archive IO behavior, use the focused
`IoUtilitiesTest` path:

```text
Fixture: synthetic Project named OriginalProgram
Archive write: original.a3p through IoUtilities.writeProject
First reopen: IoUtilities.readProject(original.a3p)
Edit: rename reopened program type to EditedProgram
Second write: edited.a3p through IoUtilities.writeProject
Second reopen: IoUtilities.readProject(edited.a3p)
Required assertion: reopened edited project program type is EditedProgram
Export: edited.a3w through IoUtilities.exportProject
Export assertion: manifest/source entries describe EditedProgram
Non-claims: desktop Save completion, visible rendering, grading, full lesson automation
```

### Capture save completion evidence

When a focused test opts in to completion evidence:

```text
Property: org.alice.eatme.saveOperationEvidenceDir=target/save-operation-evidence
Action: Save As
Prompt: yes
Selected file: target/save-operation-evidence/classroom.a3p
Result: UserActivity.finish()
Artifacts:
  target/save-operation-evidence/desktop-save-action-invocation-proof.json
  target/save-operation-evidence/desktop-save-operation-result.json
  target/save-operation-evidence/desktop-save-dialog-control-target.json
```

The completion artifact may claim a write only when `classroom.a3p` exists, is non-empty, and has the expected `.a3p` extension.

### Capture cancellation evidence

When the user cancels the prompted destination:

```text
Action: Save As
Prompt: yes
Selected file: null
Save callback: not called
Result: UserActivity.cancel()
Evidence status: canceled
Write claim: none
```
