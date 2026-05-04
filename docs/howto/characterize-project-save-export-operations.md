# Characterize Project Save and Export Operations

Use this guide to add or review compatibility tests for Alice project Save, Save As, and Export operations without changing user-visible behavior.

## Contents

- [Prerequisites](#prerequisites)
- [Feature scope](#feature-scope)
- [Choose the behavior](#choose-the-behavior)
- [Add a direct operation test](#add-a-direct-operation-test)
- [Add a flow-level test through SaveOperationFlow](#add-a-flow-level-test-through-saveoperationflow)
- [Run the focused tests](#run-the-focused-tests)

## Prerequisites

Work in the `core/ide` module and keep tests in the same Java package as the operation classes:

```text
core/ide/src/test/java/org/alice/ide/croquet/models/projecturi/
```

Use JUnit 4 and existing test fixtures. Do not add a mocking library for this package.

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

Keep archive-content tests in lower-level project IO test classes. Direct operation tests verify prompt routing and extensions. Flow tests verify activity outcome, wait cursor, retry, and delegation decisions through `SaveOperationFlow`.

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

## Run the focused tests

Run the existing module test goal:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  test
```
