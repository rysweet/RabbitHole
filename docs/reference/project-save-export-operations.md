# Project Save and Export Operations

This reference describes the `core/ide` project Save, Save As, and Export operation layer and the characterization-test feature for it.

## Contents

- [Package](#package)
- [Characterization scope](#characterization-scope)
- [Operation responsibilities](#operation-responsibilities)
- [User-visible behavior](#user-visible-behavior)
- [API reference](#api-reference)
- [Testing notes](#testing-notes)
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
behavior. Archive file contents remain owned by lower-level classes that save
Alice projects, reopen them, edit them, save again, reopen again, and export
them.

## Characterization scope

The characterization-test layer covers existing operation behavior. It does not change production Save, Save As, or Export behavior.

The direct operation coverage includes:

| Scope | Expected implementation |
| --- | --- |
| Direct operation tests | JUnit 4 tests in `org.alice.ide.croquet.models.projecturi` cover prompt rules, extension selection, and toolbar text clobbering. |
| Compatibility assertions | Assert the exact behavior currently implemented by `SaveProjectOperation`, `SaveAsProjectOperation`, and `ExportProjectOperation`. |
| Portable file fixtures | Use real files from JUnit `TemporaryFolder`; use a missing file as the portable "cannot be reused" Save case. |

These areas stay outside direct operation tests:

| Deferred scope | Reason |
| --- | --- |
| Archive content verification | Archive bytes and project serialization belong to lower-level tests that save Alice projects, reopen them, edit them, save again, reopen again, and export them, not operation-routing tests. |
| New mocking framework | The intended feature should use existing JUnit 4 patterns and narrow production seams instead of PowerMock-style interception. |
| Display-backed Swing/JavaFX testing | Desktop launch evidence belongs to the outside-in QA lane and Xvfb-backed scenarios. |

## Operation responsibilities

| Class | Responsibility |
| --- | --- |
| `AbstractSaveOperation` | Coordinates active `StageIDE` lookup, current project URI detection, save dialog routing, backup copy naming, wait cursor handling, `IOException` retry behavior, and activity finish/cancel. |
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

## Configuration

There is no runtime configuration flag for Save, Save As, or Export routing. The behavior is fixed by the operation classes and the Alice project save and export constants.

Developer validation uses the existing Maven configuration:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
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
