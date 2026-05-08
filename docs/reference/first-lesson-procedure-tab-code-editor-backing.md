# [PLANNED - Implementation Pending] First-Lesson Procedure Tab Code-Editor Backing

This reference defines the intended behavior seam for proving that selecting
`scene.eatmeFirstLesson` lands on a procedure tab whose selected
`CodeComposite` and backing `CodeEditor.getCode()` value both point at the
expected `UserMethod`.

The current checked-in `ProcedureTabSelection` surface selects a procedure tab
and reports the selected `UserMethod`. The selected `CodeComposite`,
`CodeEditor.getCode()` observation, and canonical backing test named below are
planned work and must not be cited as implemented until they land.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Examples](#examples)
- [Evidence boundaries](#evidence-boundaries)
- [Relationship to adjacent shards](#relationship-to-adjacent-shards)

## Scope

The planned seam covers exactly one handoff after the existing
procedure/code-editor action blocker proof:

```text
known UserMethod for scene.eatmeFirstLesson
  -> ProcedureTabSelection fires the real DeclarationsEditor tab operation
  -> the selected declaration tab is the expected CodeComposite
  -> the selected tab's code-editor view reports the same UserMethod from getCode()
```

This will be a read-only tab/model backing proof. It will not edit the
procedure, invoke a desktop code-editor edit action, save the project, assert
rendering, grade learner work, assess creative quality, or claim first-lesson
completion.

## Usage

Use this planned seam when a review needs future evidence that the procedure tab
selected for `scene.eatmeFirstLesson` is backed by the expected in-memory code
model before a desktop edit-action proof is attempted.

After the planned helper and test land, run the focused characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.declarationseditor.ProcedureTabSelectionTest#selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode \
  test
```

The planned canonical test method is named around the narrow claim:

```text
selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode
```

Do not replace this focused test with a broad first-lesson runner. Select
Project, Save, rendering, and assessment remain covered by their own boundaries.

## Behavior contract

`ProcedureTabSelectionTest` will own the executable proof. The planned test
builds a scene type with a procedure named `eatmeFirstLesson`, installs the
matching `CodeComposite` in a `DeclarationsEditorComposite`, selects the
procedure on the Swing event dispatch thread, and asserts these facts:

| Observation | Required fact | Status |
| --- | --- | --- |
| Selection operation | `ProcedureTabSelection.getSelectionOperation(editor, procedure)` returns the real Croquet operation for the procedure tab. | Implemented. |
| Selected procedure | `ProcedureTabSelection.selectProcedureInEditor(...)` returns the same `UserMethod` instance supplied by the test. | Implemented. |
| Selected composite | `ProcedureTabSelection.getSelectedProcedureCodeComposite(editor)` returns the selected `CodeComposite` for that `UserMethod`. | Planned. |
| Composite declaration | `selectedCodeComposite.getDeclaration()` is the expected `UserMethod`. | Planned. |
| Code-editor code | `ProcedureTabSelection.getSelectedCodeEditorCode(editor)` returns the same expected `UserMethod` through the selected tab's backing code editor. | Planned. |

The helper may initialize the selected `CodeComposite` view to inspect the code
editor backing. That view initialization is allowed because it observes the
existing selected tab and code model; it must not change the selected procedure,
mutate AST statements, invoke edit commands, or write project files.

## API reference

`ProcedureTabSelection` is an implementation/test utility in
`org.alice.ide.declarationseditor`. It is not a public Alice product API.

### `getSelectionOperation(...)`

```java
public static Operation getSelectionOperation(
    DeclarationsEditorComposite editor,
    UserMethod procedure)
```

Returns the Croquet operation that selects the procedure's declaration tab.
`procedure` must be non-null and must be a procedure. Functions are rejected with
`IllegalArgumentException`.

### `selectProcedureInEditor(...)`

```java
public static UserMethod selectProcedureInEditor(
    DeclarationsEditorComposite editor,
    UserMethod procedure,
    UserActivity activity)
```

Fires the selection operation in the provided editor and returns the selected
procedure. It throws `IllegalStateException` if the operation fires but the
editor's selected procedure is not the supplied method.

### `getSelectedProcedure(...)`

```java
public static UserMethod getSelectedProcedure(DeclarationsEditorComposite editor)
```

Returns the selected procedure when the current tab state is a `CodeComposite`
for a procedure. Returns `null` when there is no selected procedure tab.

### `[PLANNED] getSelectedProcedureCodeComposite(...)`

```java
public static CodeComposite getSelectedProcedureCodeComposite(
    DeclarationsEditorComposite editor)
```

This helper is not checked in yet. When implemented, it should return the
selected `CodeComposite` when the current tab is a procedure tab. It should
return `null` when the current tab is missing, is not a `CodeComposite`, or is
not backed by a procedure.

The helper must remain read-only. It exposes the selected composite so tests can
assert the tab's declaration identity without editing the code.

### `[PLANNED] getSelectedCodeEditorCode(...)`

```java
public static AbstractCode getSelectedCodeEditorCode(
    DeclarationsEditorComposite editor)
```

This helper is not checked in yet. When implemented, it should return the
`AbstractCode` reported by the selected procedure tab's backing
`CodeEditor.getCode()` path. It should return `null` when the current selection
is not a procedure `CodeComposite` or when the selected tab is not backed by a
`CodeEditor`.

The first-lesson backing proof will assert that this value is the same
`UserMethod` instance as `scene.eatmeFirstLesson`.

## Configuration

No product preference or runtime project configuration is required.

| Setting | Required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Preserves the repository's Node-backed orchestration memory setting when Maven is launched through scripts or wrappers. |
| `tweedle-lang` submodule | Initialized with `git submodule update --init tweedle-lang` | Required before broad or reactor Maven validation. |
| Maven flags | `-DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false` | Keeps the focused `core/ide` characterization bounded to the no-Sims test surface. |

Prepare the checkout:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Examples

The planned focused assertion pattern is:

```java
DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
UserMethod procedure = sceneProcedure("eatmeFirstLesson");
editor.getTabState().getData().internalSetAllItems(
    List.of(CodeComposite.getInstance(procedure)));

SwingUtilities.invokeAndWait(() -> {
  UserMethod selected = ProcedureTabSelection.selectProcedureInEditor(
      editor,
      procedure,
      null);

  assertSame(procedure, selected);
  assertSame(procedure, ProcedureTabSelection.getSelectedProcedure(editor));
  // [PLANNED] selected CodeComposite helper
  assertSame(
      procedure,
      ProcedureTabSelection.getSelectedProcedureCodeComposite(editor).getDeclaration());
  // [PLANNED] selected CodeEditor.getCode() helper
  assertSame(procedure, ProcedureTabSelection.getSelectedCodeEditorCode(editor));
});
```

Once implemented, the example will prove identity through the procedure tab and
code-editor backing model. It intentionally performs no desktop edit action.

## Evidence boundaries

Once implemented, this seam may claim only:

- Selecting the `scene.eatmeFirstLesson` `UserMethod` opens the expected
  procedure tab in the declarations editor.
- The selected tab is the expected `CodeComposite`.
- The selected tab's backing code-editor model reports the same expected
  `UserMethod` from `getCode()`.

This seam must not claim:

- Desktop procedure edit mutation.
- Code-editor action invocation or completion.
- Save, Save As, or project persistence.
- Select Project behavior.
- Rendering correctness.
- Learner grading or creative assessment.
- Full first-lesson completion.

## Relationship to adjacent shards

| Boundary | Owner |
| --- | --- |
| Select Project opens the configured starter | [Open Africa Full through Select Project with AT-SPI](./select-project-africa-full-atspi-evidence.md). |
| Live post-open target/action readiness | [First-Lesson Live Procedure Target Action Seam](./first-lesson-live-procedure-target-observation.md). |
| Procedure tab and code-editor backing | This planned reference and the future `ProcedureTabSelectionTest#selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode`. |
| AST/project-level procedure edit | [First-Lesson Procedure/Edit Seam](./first-lesson-procedure-edit-seam.md). |
| Save menu/dialog/write proof | [Save Menu Dialog Write Proof](./save-menu-dialog-write-proof.md) and [Robot Save Menu Dialog Write/Readback Proof](./robot-save-menu-dialog-write-readback-proof.md). |
| Learner-world assessment boundary | [Learner-world assessment boundary](./learner-world-assessment-boundary.md). |

Use the smallest owner for the claim under review. Once this seam exists,
passing it will be a prerequisite-style observation for a future desktop
edit-action proof, not proof that the edit action itself exists or completed.
