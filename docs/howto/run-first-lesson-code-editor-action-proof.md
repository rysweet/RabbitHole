# Run the First-Lesson Code-Editor Action Proof

Use this guide to run and review the focused Java proof that
`scene.eatmeFirstLesson` is selected through the declarations editor/code-editor
backing seam and receives the deterministic
`append-comment:wave4-code-editor-action-proof` edit action.

For the full contract, see the [First-Lesson Code-Editor Action Proof
reference](../reference/first-lesson-code-editor-action-proof.md).

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the focused proof

Run the canonical characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.FirstLessonCodeEditorActionProofTest \
  test
```

This command does not use a QA workflow timeout. Do not add `--timeout-seconds`,
sleeps, polling loops, or timeout-based success criteria.

## Review the proof

Review the focused test for these assertions:

| Assertion | Meaning |
| --- | --- |
| `scene.eatmeFirstLesson` resolves to one target `UserMethod`. | The proof has an exact procedure target. |
| `ProcedureTabSelection.selectProcedureInEditor(...)` returns that same method. | The real declarations editor tab operation selected the target. |
| `getSelectedProcedureCodeComposite(...).getDeclaration()` is the same method. | The selected declaration tab is the expected `CodeComposite`. |
| `getSelectedCodeEditorCode(...)` is the same method. | The backing code-editor model is the expected procedure code. |
| `getSelectedCodeEditorBackingClassName(...)` is `org.alice.ide.codeeditor.CodeEditor`. | The proof names the concrete backing editor seam. |
| `append-comment:wave4-code-editor-action-proof` completes. | The deterministic action was accepted and applied through the implemented action surface. |
| The marker appears once in the target and zero times elsewhere. | The after-state observation is target-only. |

The proof will write `first-lesson-code-editor-action-proof.json` in its JUnit
temporary workspace only on success. That file must be asserted by the test and
then discarded with the temporary directory.

Do not substitute older `procedure-edit.json`, `procedure-edit-command.json`,
`procedure.diff.json`, `procedure-tab-selection.json`, or
`procedure-ui-action-no-go.json` artifacts for this success schema. The
implemented proof writes `first-lesson-code-editor-action-proof.json`.

## Review negative checks

The same characterization must include negative checks for failed proof conditions.
Confirm that failure cases do not write the success artifact.

| Failure case | Expected behavior |
| --- | --- |
| Missing target procedure | Reports the missing `scene.eatmeFirstLesson` target and stops before mutation; it must not create the method for this proof. |
| Wrong selected procedure | Rejects the evidence because the selected method is not the target. |
| Unsupported action | Rejects edit specs outside `append-comment:<marker>`. |
| Blank marker | Rejects the action before mutation. |
| Mismatched marker | Fails if the requested marker is not found exactly once in the target after the action. |
| Marker outside target | Fails if any non-target procedure contains the marker. |

The implemented success path must not emit the blocked artifact. If the
deterministic action path regresses and becomes unavailable, use the single
blocked artifact contract described in the reference instead of a success
artifact.

## Keep the claim narrow

Cite this proof only for the first-lesson procedure/code-editor backing action
seam. Do not cite it as evidence for full first-lesson completion, grading,
creative assessment, visible rendering correctness, Save behavior, Select Project
behavior, launcher behavior, or broad desktop UI automation.

Adjacent claims remain owned by their own documents:

| Claim | Use |
| --- | --- |
| Tab and `CodeEditor.getCode()` backing without mutation | [First-Lesson Procedure Tab Code-Editor Backing](../reference/first-lesson-procedure-tab-code-editor-backing.md). |
| AST/project procedure edit utility behavior | [First-Lesson Procedure/Edit Seam](../reference/first-lesson-procedure-edit-seam.md). |
| Live desktop target readiness | [First-Lesson Live Procedure Target Action Seam](../reference/first-lesson-live-procedure-target-observation.md). |
| Save menu/dialog/write behavior | [Save Menu Dialog Write Proof](../reference/save-menu-dialog-write-proof.md). |
| Learner-world assessment | [Learner-world assessment boundary](../reference/learner-world-assessment-boundary.md). |
