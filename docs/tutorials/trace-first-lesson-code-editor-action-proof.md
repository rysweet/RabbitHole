# Tutorial: Trace the First-Lesson Code-Editor Action Proof

This tutorial walks through the focused proof that the selected
`scene.eatmeFirstLesson` procedure tab is backed by the expected code-editor
model and accepts one deterministic edit action.

## What you will do

1. Prepare the repository for focused `core/ide` validation.
2. Run `FirstLessonCodeEditorActionProofTest`.
3. Trace the target through procedure selection, `CodeComposite`, and
   `CodeEditor.getCode()`.
4. Trace the `append-comment:wave4-code-editor-action-proof` action.
5. Review the negative checks and claim boundaries.

## Before you start

Open a terminal at the repository root:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Run the characterization

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.FirstLessonCodeEditorActionProofTest \
  test
```

The test is intentionally small. It does not launch Select Project, drive a
visible desktop workflow, save through the menu, render a scene, grade learner
work, or check lesson completion.

## Step 2: Locate the target

The proof starts from the exact selector:

```text
scene.eatmeFirstLesson
```

The fixture must resolve that selector to one existing scene `UserMethod`. The
proof uses object identity from this point forward; a matching name on a
different method is not enough. This proof fails closed when the target is
missing and does not create a missing method.

## Step 3: Follow the tab selection

The declarations editor operation is fired through:

```java
UserMethod selected = ProcedureTabSelection.selectProcedureInEditor(
    editor,
    target,
    null);
```

The selected method must be the same object as the target:

```java
assertSame(target, selected);
assertSame(target, ProcedureTabSelection.getSelectedProcedure(editor));
```

If the operation selects anything else, the proof fails before the edit action is
accepted.

## Step 4: Follow the `CodeComposite`

The proof asks the declarations editor for the selected procedure tab:

```java
CodeComposite selectedComposite =
    ProcedureTabSelection.getSelectedProcedureCodeComposite(editor);
```

The selected composite must declare the target method:

```java
assertSame(target, selectedComposite.getDeclaration());
```

This protects against a tab-state success that is not actually backed by the
target procedure.

## Step 5: Follow the code-editor backing model

The proof then observes the selected tab's backing editor:

```java
assertSame(target, ProcedureTabSelection.getSelectedCodeEditorCode(editor));
assertEquals(
    "org.alice.ide.codeeditor.CodeEditor",
    ProcedureTabSelection.getSelectedCodeEditorBackingClassName(editor));
```

This is the code-editor backing seam. It proves that the selected procedure tab
is backed by a `CodeEditor` whose `getCode()` path reports the same
`scene.eatmeFirstLesson` method.

## Step 6: Trace the deterministic action

The only supported action for this proof is:

```text
append-comment:wave4-code-editor-action-proof
```

The `EatmeEditProcedure` action path applies that action only after the target
and backing observations match. The after-state check looks for the marker in
the target method body, not in generated source text:

```java
assertEquals(1, markerObservation.targetCount());
assertEquals(0, markerObservation.nonTargetCount());
```

The proof succeeds only when the marker appears exactly once in
`scene.eatmeFirstLesson` and nowhere else. It writes the
`first-lesson-code-editor-action-proof.json` success artifact only in that case.
Older `procedure-edit*.json` and `procedure-ui-action-no-go.json` artifacts are
not this proof's success schema.

## Step 7: Review negative checks

Read the negative tests as part of the proof. They keep the success case honest:

| Negative check | Why it matters |
| --- | --- |
| Missing target | Prevents accidental method creation from masquerading as proof. |
| Wrong target | Prevents a different scene procedure from satisfying the selector. |
| Unsupported action | Keeps the action contract whitelisted and deterministic. |
| Blank marker | Prevents empty evidence from passing. |
| Mismatched marker | Ensures the requested marker is the marker that was observed. |
| Marker outside target | Ensures the edit did not broaden to other procedures. |

If the code-editor action path is unavailable, the fallback is exactly one
blocked artifact with the missing contract from the reference and no success
artifact. If the action proof succeeds, do not emit the blocker.

## Step 8: Stop at the seam

This tutorial proves only the first-lesson procedure/code-editor backing action
seam. It does not prove:

- Full first-lesson completion.
- Grading, scoring, or creative assessment.
- Visible rendering correctness.
- Broad UI automation.
- Save or Save As.
- Select Project.
- Launcher behavior.

For the task-oriented command, see [Run the First-Lesson Code-Editor Action
Proof](../howto/run-first-lesson-code-editor-action-proof.md). For the full
contract, see the [First-Lesson Code-Editor Action Proof
reference](../reference/first-lesson-code-editor-action-proof.md).
