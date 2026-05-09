# Tutorial: Trace the Silver Thread Launch-Build-Run Test

This tutorial walks through the first real end-to-end silver thread test for
Alice. You will trace both test methods to understand how the headless student
journey is proven: from project creation through virtual machine execution to
save/reopen round-trip fidelity.

For the full contract, see the [Silver Thread Launch-Build-Run Test
reference](../reference/silver-thread-launch-build-run-test.md).

## Contents

- [Goal](#goal)
- [1. Understand the silver thread](#1-understand-the-silver-thread)
- [2. Trace project creation](#2-trace-project-creation)
- [3. Trace statement addition](#3-trace-statement-addition)
- [4. Trace save and reopen](#4-trace-save-and-reopen)
- [5. Trace virtual machine execution](#5-trace-virtual-machine-execution)
- [6. Trace round-trip verification](#6-trace-round-trip-verification)
- [7. Trace the starter project journey](#7-trace-the-starter-project-journey)
- [8. Run the test](#8-run-the-test)
- [9. Understand the boundaries](#9-understand-the-boundaries)

## Goal

Understand what the silver thread test proves and, equally important, what it
does not prove. After this tutorial you will be able to explain the test's
seven-step journey, the VM listener event contract, and the round-trip fidelity
assertions.

This tutorial describes the designed behavior. If the test file does not exist
yet, use this document as a reading guide for the [reference
specification](../reference/silver-thread-launch-build-run-test.md) and then
follow its implementation.

## 1. Understand the silver thread

The "silver thread" is the thinnest possible end-to-end path through Alice's
core engine:

```text
Create a project
  → Add code to it (a Comment statement, simulating dragging a tile)
  → Save the project to disk
  → Reopen the saved project
  → Execute the program through the virtual machine
  → Verify the code actually ran (via listener events)
  → Save again and verify the project survives the round-trip
```

This path deliberately avoids 3D rendering, JavaFX display, drag-and-drop UI,
gallery assets, and grading. It proves the core engine pipeline works without
any GUI infrastructure.

## 2. Trace project creation

Open `SilverThreadLaunchBuildRunTest.java` and find the project creation:

```java
NamedUserType programType = new NamedUserType();
programType.name.setValue("SilverThreadProgram");
programType.superType.setValue(JavaType.getInstance(SProgram.class));

Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
```

This mirrors how Alice creates a project internally. The `NamedUserType`
represents the student's program class. Setting `superType` to `SProgram` makes
it a valid Alice program type that the virtual machine can execute.

Compare this with `ProjectOpenSaveExportJourneyTest.programType()` — the same
factory pattern.

## 3. Trace statement addition

Find the method creation with a `Comment`:

```java
Comment silverThreadStep = new Comment("silver thread step");
UserMethod entryMethod = new UserMethod(
    "runSilverThread",
    Void.TYPE,
    new UserParameter[0],
    new BlockStatement(silverThreadStep));
entryMethod.isStatic.setValue(true);
```

Key decisions:

- **`Comment`** is used because it is the simplest no-op statement. It simulates
  a student dragging their first code tile into the editor.
- **`isStatic` is true** so `ENTRY_POINT_invoke` can run the method without a
  scene instance. This matches `VirtualMachineHeadlessRuntimeEventTest`.
- The method is added to the program type's `methods` collection.

## 4. Trace save and reopen

Find the save/reopen sequence:

```java
IoUtilities.writeProject(projectFile, project);
Project loadedProject = new TestFileProjectLoader(projectFile).loadNow();
```

**`IoUtilities.writeProject`** writes an `.a3p` archive (a zip file containing
the serialized AST). **`TestFileProjectLoader`** extends `FileProjectLoader`
from `core/ide` and exposes the protected `load()` method for testing.

The test asserts:

- The saved file exists on disk.
- The loaded project is non-null.
- The loaded project's program type name matches the original.

## 5. Trace virtual machine execution

Find the VM execution:

```java
ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
vm.addVirtualMachineListener(listener);

vm.ENTRY_POINT_invoke(null, entryMethod);
```

**`ENTRY_POINT_invoke(null, method)`** runs a static method headlessly. The
first argument is `null` because the method is static (no instance needed).

The `RecordingVirtualMachineListener` records `statementExecuting` and
`statementExecuted` callbacks. For a method body of
`BlockStatement(Comment("silver thread step"))`, the expected events are:

```text
executing:BlockStatement   ← VM enters the method body
executing:Comment          ← VM enters the Comment statement
executed:Comment           ← VM finishes the Comment statement
executed:BlockStatement    ← VM finishes the method body
```

This is the same 4-event contract verified by
`VirtualMachineHeadlessRuntimeEventTest`. The silver thread test re-verifies it
after a save/reopen cycle to prove the deserialized AST executes identically.

## 6. Trace round-trip verification

Find the second save/reopen cycle:

```java
IoUtilities.writeProject(roundTripFile, loadedProject);
Project roundTrippedProject = IoUtilities.readProject(roundTripFile);
```

The test asserts four properties survive two serialization cycles:

| Property | Assertion |
| --- | --- |
| Program type name | `assertEquals("SilverThreadProgram", ...)` |
| Method name | `assertEquals("runSilverThread", ...)` |
| Statement count | `assertEquals(1, ...)` |
| Comment text | `assertEquals("silver thread step", ...)` |

This is stronger than a file-exists check. It proves full AST fidelity: the
program structure, method identity, statement content, and comment text are all
preserved through the complete serialize → deserialize → serialize → deserialize
pipeline.

## 7. Trace the starter project journey

Find the second test method, `loadRealStarterProjectInspectSaveCopyAndReopen`:

```java
File starterFile = new File(
    getClass().getResource("/starters/indiaMinimum.a3p").toURI());
Project project = IoUtilities.readProject(starterFile);
```

This loads a real Alice starter project — `indiaMinimum.a3p` — copied from
`core/resources/src/application/resources/starter-projects/`.

The test makes structural assertions without hardcoding internal type names:

- Program type name is not null and not empty.
- Program type is assignable to `SProgram`.

Then it saves a copy and reopens it to verify the name survives:

```java
IoUtilities.writeProject(copyFile, project);
Project reopenedProject = IoUtilities.readProject(copyFile);
assertEquals(originalName, reopenedProject.getProgramType().getName());
```

This proves Alice can load, serialize, and deserialize a real starter project
without data loss.

## 8. Run the test

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadLaunchBuildRunTest \
  test
```

Both tests pass without a display server or network access.

## 9. Understand the boundaries

The silver thread test proves a narrow but critical path. After tracing both
methods, confirm you can answer:

| Question | Answer |
| --- | --- |
| Does this prove rendering works? | No. No scene graph, no display. |
| Does this prove drag-and-drop works? | No. The `Comment` is added via the AST API. |
| Does this prove gallery assets load? | No. No model resources are referenced. |
| Does this prove a real lesson completes? | No. No grading or assessment logic. |
| What does it prove? | The core engine pipeline — create, build, run, save, reopen — works end-to-end without GUI infrastructure. |

The test is intentionally minimal. It is the foundation for broader tests that
will add scene setup, model resources, and UI automation in later iterations.
Keep the claim narrow: cite this test only for the headless engine pipeline, not
for visual or interactive behavior.
