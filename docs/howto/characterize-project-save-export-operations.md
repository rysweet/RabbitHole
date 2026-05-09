# Characterize Project Save and Export Operations

Use this guide to add or review compatibility tests for Alice project Save, Save As, and Export operations without changing user-visible behavior.

## Contents

- [Prerequisites](#prerequisites)
- [Feature scope](#feature-scope)
- [Choose the behavior](#choose-the-behavior)
- [Add a direct operation test](#add-a-direct-operation-test)
- [Add a flow-level test through SaveOperationFlow](#add-a-flow-level-test-through-saveoperationflow)
- [Run the Save menu dialog write proof](#run-the-save-menu-dialog-write-proof)
- [Add a project archive round-trip test](#add-a-project-archive-round-trip-test)
- [Run the focused tests](#run-the-focused-tests)

## Prerequisites

Work in the `core/ide` module and keep tests in the same Java package as the operation classes:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/
```

Use JUnit 4 and existing test fixtures. Do not add a mocking library for this package.

Archive-content regression tests for opening, decoding, editing, saving, and
exporting projects live in the story API migration module:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java
```

Use the existing `TemporaryFolder`, synthetic `Project`, synthetic
`NamedUserType`, and zip-entry inspection patterns in that class. Do not add LFS
fixtures, bundled media dependencies, JavaFX startup, Swing automation, or IDE
launches for archive round-trip coverage.

Initialize the grammar submodule before Maven validation from a fresh checkout or worktree:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Feature scope

The direct characterization-test suite for the operation classes covers:

| Class | Direct coverage |
| --- | --- |
| `SaveProjectOperation` | `null` file prompts, writable file does not prompt, non-reusable file prompts, project extension, toolbar text clobbering. |
| `SaveAsProjectOperation` | Always prompts and uses the project extension inherited from `AbstractSaveProjectOperation`. |
| `ExportProjectOperation` | Always prompts, uses the export extension, and clobbers toolbar text. |
| `AbstractSaveProjectOperation` | Shared project extension as observed through Save and Save As operations. |

Flow-level behavior is characterized through the package-private `SaveOperationFlow` seam. Tests supply the active file, save dialog result, error reporter, wait cursor hooks, activity outcome hooks, and save callback without static or UI interception.

## Choose the behavior

Start with the highest-value untested behavior in the operation layer:

| Behavior | Test target |
| --- | --- |
| Save prompt routing | `SaveProjectOperation` |
| Save As always prompts | `SaveAsProjectOperation` |
| Export always prompts and uses export extension | `ExportProjectOperation` |
| Shared project extension | `AbstractSaveProjectOperation` as observed through Save and Save As operations |
| Finish, cancel, wait cursor, and retry flow | `SaveOperationFlow` |
| Rendered File-menu Save, live Save chooser control, `.a3p` write, readback, and marker proof | `RobotSaveMenuDialogWriteReadbackProofTest` |
| Saved Alice project reopens, accepts an edit, saves again, reopens again with the edit, and exports | `IoUtilitiesTest` |

Keep archive-content tests in lower-level classes that save Alice projects,
reopen them, edit them, save again, reopen again, and export them. Direct
operation tests verify prompt routing and extensions. Flow tests verify activity
outcome, wait cursor, retry, and delegation decisions through
`SaveOperationFlow`. Project archive round-trip tests verify the saved bytes
remain editable and exportable after reopening.

## Add a direct operation test

Direct operation tests live in the operation package so they can exercise protected methods without widening production APIs.

Example: characterize Save prompt routing.

```java
package org.alice.ide.croquet.models.projecturi;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.io.IoUtilities;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SaveProjectOperationTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void promptsWhenCurrentFileIsMissing() {
    assertTrue(SaveProjectOperation.getInstance().isPromptNecessary(null));
  }

  @Test
  public void doesNotPromptForWritableCurrentFile() throws Exception {
    File projectFile = temporaryFolder.newFile("RobotDance.a3p");

    assertFalse(SaveProjectOperation.getInstance().isPromptNecessary(projectFile));
  }

  @Test
  public void promptsWhenCurrentFileCannotBeReused() {
    File missingProjectFile = new File(temporaryFolder.getRoot(), "MissingRobotDance.a3p");

    assertTrue(SaveProjectOperation.getInstance().isPromptNecessary(missingProjectFile));
  }

  @Test
  public void usesProjectExtension() {
    assertEquals(IoUtilities.PROJECT_EXTENSION, SaveProjectOperation.getInstance().getExtension());
  }

  @Test
  public void clobbersToolbarText() {
    assertTrue(SaveProjectOperation.getInstance().isToolBarTextClobbered());
  }
}
```

Use a missing file as the portable "cannot be reused" case. A file with permissions changed to non-writable can also characterize the same rule, but that assertion is more sensitive to the operating system and test user.

## Add a flow-level test through SaveOperationFlow

Use `SaveOperationFlow` when the behavior crosses the shared template in `AbstractSaveOperation`. The production adapter still calls static or UI-bound collaborators, but the seam lets tests supply those effects directly:

| Collaborator | Flow test substitute |
| --- | --- |
| `StageIDE.getActiveInstance()` | `SaveOperationFlow.Context.getCurrentFile()` and related context methods. |
| `DocumentFrame.showSaveFileDialog(...)` | `SaveOperationFlow.Context.showSaveFileDialog(...)`. |
| `Dialogs.showError(...)` | `SaveOperationFlow.Context.showError(...)`. |
| `ProjectApplication.saveProjectTo(File)` and `exportProjectTo(File)` | `SaveOperationFlow.SaveAction`. |

Add direct operation tests for prompt and extension behavior. Add flow tests when the behavior involves current file reuse, dialog cancellation, backup copy naming, wait cursor cleanup, error reporting, retry, or `UserActivity` finish/cancel.

Example scenario:

```text
Given a writable current project file
When Save runs
Then no save dialog is shown
And ProjectApplication.saveProjectTo(currentFile) is called
And the user activity is finished
```

For the currently characterized current-file Save retry behavior:

```text
Given the first save attempt to a writable current file throws IOException
And the next dialog selection returns a writable destination
When Save runs
Then the error is reported
And the wait cursor is hidden after the failed attempt
And Save prompts again
And the user activity is finished after the successful retry
```

For prompted Save As or Export-style retries, characterize the existing suggestion behavior explicitly: retries use the current project base name when one exists and no suggested base name when no current file exists.

## Run the Save menu dialog write proof

Use the target canonical proof shard when the behavior must be proven beyond operation dispatch and flow seams. The finished shard starts from the rendered File menu, uses AWT Robot to click the production Save item, controls exactly one expected live Swing `JFileChooser`, verifies the proof-root `.a3p` target, asserts a non-empty `.a3p` file write, reads the file back, and verifies `robotSaveMenuRoundTripMarker`.

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

The focused proof target emits `core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json`. A no-display, exhausted bounded wait, or unsafe UI state emits `status: "blocked"` with exactly one known blocker and fails the proof; use that artifact as the executable blocker, not as Save completion evidence.

Do not broaden this proof while reviewing Save behavior. It does not cover Save As, backup saves, retry behavior, overwrite prompts, cancellation, rendering correctness, grading, lesson completion, broad UI automation, or native dialog control. For the complete artifact contract, see [Save Proof Evidence](../reference/save-proof-evidence.md).

## Add a project archive round-trip test

Use `IoUtilitiesTest` when the behavior is about the actual project archive that
Alice saves, reopens, edits, saves again, reopens again, or exports. This test
sits below the UI operation layer and exercises the production archive APIs
directly:

| API | Role in the journey |
| --- | --- |
| `IoUtilities.writeProject(File, Project)` | Writes the original and edited `.a3p` editor archives. |
| `IoUtilities.readProject(File)` | Reopens the saved archives through the production project reader. |
| `IoUtilities.exportProject(File, Project)` | Writes a `.a3w` player archive from the edited project. |

Use the regression test
`IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`
for saving, reopening, editing, saving again, reopening again, and exporting
Alice projects:

```text
Given a synthetic project whose program type is named OriginalProgram
When the project is saved to original.a3p
And original.a3p is reopened through IoUtilities.readProject
And the reopened project's program type is renamed to EditedProgram with NamedUserType.name.setValue(...)
And the reopened edited project is saved to edited.a3p
Then reopening edited.a3p returns a project with program type named EditedProgram
And exporting the edited project writes a stable .a3w manifest/source archive
```

Assert the edited state after the second reopen. A file-exists assertion is not
strong enough because it would miss a stale-save regression where Alice writes
the pre-edit project state. Export assertions should stay structural and stable:
check entries such as `manifest.json` and the Tweedle source entry named by the
manifest type reference rather than UI behavior or broad player runtime behavior.
For the current edited-name scenario, use `src/EditedProgram.twe` if that is the
exporter output.

Keep the fixture synthetic. The test should create all `.a3p` and `.a3w` files
under `TemporaryFolder`, mutate the reopened `Project`, inspect zip entries
directly when needed, and let `IOException` or version failures fail the test.

## Run the focused tests

Run the existing module test goal:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  test
```

After adding the archive round-trip regression, run it with the focused story API
migration gate:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```
