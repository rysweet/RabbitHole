# Run the First-Lesson Procedure Tab Code-Editor Backing Proof

Use this guide to run and review the focused proof that selecting
`scene.eatmeFirstLesson` lands on the expected procedure tab and that the tab is
backed by the expected code-editor code model.

For the full API and claim boundaries, see the [First-Lesson Procedure Tab
Code-Editor Backing reference](../reference/first-lesson-procedure-tab-code-editor-backing.md).

## Before you start

Run commands from the repository root.

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the focused proof

Run the canonical method directly:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.declarationseditor.ProcedureTabSelectionTest#selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode \
  test
```

This is the canonical validation surface for the tab/code-editor backing seam.
It runs headlessly against the `core/ide` test surface and does not launch a
full Alice desktop scenario.

## Review the proof

The focused test includes the characterization named:

```text
selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode
```

Review the assertions for these facts:

| Assertion | Meaning |
| --- | --- |
| `selectProcedureInEditor(...)` returns the expected `UserMethod`. | The Croquet tab-selection operation selected the target procedure. |
| `getSelectedProcedure(editor)` returns the expected `UserMethod`. | The editor tab state reports the target procedure as selected. |
| `getSelectedProcedureCodeComposite(editor).getDeclaration()` is the expected `UserMethod`. | The selected declaration tab is the expected `CodeComposite`. |
| `getSelectedCodeEditorCode(editor)` is the expected `UserMethod`. | The selected tab's backing code-editor model is the target procedure code. |
| `getSelectedCodeEditorBackingClassName(editor)` is `org.alice.ide.codeeditor.CodeEditor`. | The seam records the concrete backing editor class. |

The test should use a synthetic scene method named `eatmeFirstLesson`. It does
not need the real first-lesson starter, Select Project automation, Save proof,
rendering proof, or assessment artifacts.

## Keep the result narrow

After this proof passes, cite it only for the in-editor procedure-tab to
code-editor backing handoff. Do not cite it as evidence that a desktop edit
action ran, that a project was saved, that rendering is correct, that learner
assessment is automated, or that the first lesson is complete.

Use the adjacent shards for adjacent claims:

| Claim | Use |
| --- | --- |
| Select Project opened the starter | `alice-desktop-select-project-tab-click-exec` and the Select Project evidence reference. |
| Live desktop target readiness | `alice-desktop-first-lesson-live-procedure-target-observation`. |
| Deterministic code-editor backing action | [First-Lesson Code-Editor Action Proof](../reference/first-lesson-code-editor-action-proof.md). |
| AST/project-level procedure edit | `EatmeEditProcedureTest` procedure/edit handoff proof. |
| Save menu/dialog/write behavior | Save menu/dialog/write proof tests. |
| Learner-world assessment | Manual boundary evidence only. |
