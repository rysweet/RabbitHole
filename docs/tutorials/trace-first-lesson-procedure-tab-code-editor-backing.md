# Tutorial: Trace the First-Lesson Procedure Tab Code-Editor Backing Seam

This tutorial walks through the focused proof that the selected
`scene.eatmeFirstLesson` procedure tab is backed by the expected code-editor
code model.

## What you will do

1. Prepare the repository for focused `core/ide` validation.
2. Run the focused `ProcedureTabSelectionTest` method.
3. Trace the selected `UserMethod` through the tab state, `CodeComposite`, and
   `CodeEditor.getCode()` observation.
4. Check the claim boundaries before citing the result.

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
  -Dtest=org.alice.ide.declarationseditor.ProcedureTabSelectionTest#selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode \
  test
```

The test is intentionally small. It exercises the declarations editor tab/model
surface without launching Select Project, mutating a project archive, saving, or
running the desktop renderer.

## Step 2: Find the target method

The proof builds a synthetic scene type with a procedure named:

```text
eatmeFirstLesson
```

That synthetic method stands in for the first-lesson selector:

```text
scene.eatmeFirstLesson
```

The proof cares about object identity, not just matching names. The same
`UserMethod` instance must be observed at every step.

## Step 3: Follow the selection operation

`ProcedureTabSelection.getSelectionOperation(editor, procedure)` returns the
Croquet operation owned by the declarations editor tab state. The test installs
the `CodeComposite` for the target procedure and fires the operation through:

```java
ProcedureTabSelection.selectProcedureInEditor(editor, procedure, null)
```

The selected procedure must be the expected `UserMethod`.

## Step 4: Follow the selected composite helper

After selection, the test asks for the selected procedure tab:

```java
CodeComposite selectedComposite =
    ProcedureTabSelection.getSelectedProcedureCodeComposite(editor);
```

The selected composite must exist, and its declaration must be the same method:

```java
assertSame(procedure, selectedComposite.getDeclaration());
```

This proves the tab state is not merely carrying a matching name or a different
method object.

## Step 5: Follow the code-editor backing model

The final observation asks the selected tab's backing editor for its code model:

```java
assertSame(procedure, ProcedureTabSelection.getSelectedCodeEditorCode(editor));
```

This proves the selected procedure tab is backed by a code editor whose
`getCode()` path returns the expected `UserMethod`.

## Step 6: Stop at the seam

This tutorial proves only the tab/code-editor backing handoff. It does not
prove:

- Desktop procedure edit mutation.
- Code-editor action invocation or completion.
- Save or Save As.
- Select Project.
- Rendering correctness.
- Learner grading or creative assessment.
- Full first-lesson completion.

For the task-oriented command, see [Run the First-Lesson Procedure Tab
Code-Editor Backing Proof](../howto/run-first-lesson-procedure-tab-code-editor-backing.md).
For the full contract, see the [First-Lesson Procedure Tab Code-Editor Backing
reference](../reference/first-lesson-procedure-tab-code-editor-backing.md).
