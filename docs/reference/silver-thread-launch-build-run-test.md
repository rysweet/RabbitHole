# Silver Thread Launch-Build-Run End-to-End Test

This reference defines the first real end-to-end silver thread test for Alice.
The executable proof is
`org.alice.ide.SilverThreadLaunchBuildRunTest`. It is a JUnit 4 characterization
test in `core/ide` that proves the core student journey works headlessly:
create a project, add a statement, save, reopen, execute through the virtual
machine, verify execution via listener events, and verify save/reopen
round-trip fidelity.

The test does not start JavaFX, load gallery assets, render a 3D scene, exercise
drag-and-drop UI, or require a display.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Design decisions](#design-decisions)
- [Test methods](#test-methods)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Test resources](#test-resources)
- [Claim boundaries](#claim-boundaries)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Scope

The test covers exactly these two journeys:

### Journey 1: Create → Build → Run → Save → Reopen → Verify Round-Trip

```text
1. Create a Project with a NamedUserType extending SProgram
2. Add a UserMethod containing a Comment statement (simulates dragging a code tile)
3. Save the project to disk via IoUtilities.writeProject
4. Reopen the saved project via TestFileProjectLoader.loadNow
5. Execute the method through ReleaseVirtualMachine.ENTRY_POINT_invoke with a listener
6. Verify the statement actually executed via VM listener events
7. Save again and verify AST round-trip fidelity
```

### Journey 2: Load Real Starter → Inspect → Save Copy → Reopen

```text
1. Load indiaMinimum.a3p from core/ide/src/test/resources/starters/
2. Inspect the program type: name is not null/empty, assignable to SProgram
3. Save a copy via IoUtilities.writeProject
4. Reopen the copy via IoUtilities.readProject
5. Verify the program type name survives the round-trip
```

## Implementation status

Both test methods are implemented and passing.

| Surface | Current state |
| --- | --- |
| `SilverThreadLaunchBuildRunTest` | **Implemented.** `core/ide/src/test/java/org/alice/ide/SilverThreadLaunchBuildRunTest.java`. |
| `indiaMinimum.a3p` test resource | **Copied.** `core/ide/src/test/resources/starters/indiaMinimum.a3p`. |
| Maven validation | `mvn -pl core/ide -am -Dtest=SilverThreadLaunchBuildRunTest test` — 2 tests, 0 failures. |

## Design decisions

Three deliberate departures from the original issue spec, approved during
architecture review:

1. **Method names are more descriptive than the spec.**
   The spec proposed `createProjectAddStatementExecuteAndVerifyRoundTrip` and
   `starterProjectCanBeLoadedInspectedAndRoundTripped`. This design uses
   `createProjectAddCommentSaveReopenExecuteAndVerifyRoundTrip` and
   `loadRealStarterProjectInspectSaveCopyAndReopen` because the longer names
   encode the full journey — including which statement type, the save/reopen
   cycle, and the copy step — making test output self-documenting.

2. **Execution happens after deserialization, not before save.**
   The spec ordered the journey as create → execute → assert events → save →
   reopen → verify structure. This design orders it as create → add → save →
   reopen → **execute the deserialized AST** → save again → verify round-trip.
   Executing after deserialization is strictly stronger: it proves the serialized
   AST is executable, not just structurally intact.

3. **A second save/reopen cycle is added.**
   The spec had one save/reopen. This design adds a second cycle that asserts
   four properties survive (program type name, method name, statement count,
   Comment text). This proves full AST fidelity across two serialization cycles,
   not just file-exists.

## Test methods

### createProjectAddCommentSaveReopenExecuteAndVerifyRoundTrip

This method exercises the complete silver thread journey:

| Step | API | Assertion |
| --- | --- | --- |
| Create project | `new Project(programType, SceneCameraType.WindowCamera)` | Project is non-null. |
| Add Comment statement | `new Comment("silver thread step")` added to a `UserMethod` with `BlockStatement` | Statement count is 1. |
| Save to disk | `IoUtilities.writeProject(File, Project)` | File exists on disk. |
| Reopen | `TestFileProjectLoader(file).loadNow()` | Loaded project is non-null; program type name matches. |
| Execute via VM | `ReleaseVirtualMachine.ENTRY_POINT_invoke(null, method)` with `RecordingVirtualMachineListener` | Listener receives 4 events: `executing:BlockStatement`, `executing:Comment`, `executed:Comment`, `executed:BlockStatement`. |
| Save again (round-trip) | `IoUtilities.writeProject(File, Project)` followed by `IoUtilities.readProject(File)` | Round-tripped project program type name matches; method name matches; statement count matches; Comment text matches. |

The method under execution is declared `static` so `ENTRY_POINT_invoke` does not
require a scene instance. This follows the pattern established by
`VirtualMachineHeadlessRuntimeEventTest`.

### loadRealStarterProjectInspectSaveCopyAndReopen

This method exercises a real `.a3p` starter project:

| Step | API | Assertion |
| --- | --- | --- |
| Load starter | `IoUtilities.readProject(File)` from classpath resource | Project is non-null. |
| Inspect program type | `project.getProgramType()` | Name is not null and not empty; type is assignable to `SProgram`. |
| Save copy | `IoUtilities.writeProject(File, Project)` | Copy file exists on disk. |
| Reopen copy | `IoUtilities.readProject(File)` | Reopened project program type name equals original. |

## Usage

Run from the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadLaunchBuildRunTest \
  test
```

Both test methods run without a display server, JavaFX toolkit, or network
access.

## Behavior contract

### VM execution events

The `RecordingVirtualMachineListener` records `statementExecuting` and
`statementExecuted` callbacks. For a method body containing one `Comment`
inside one `BlockStatement`, the expected event sequence is:

```text
executing:BlockStatement
executing:Comment
executed:Comment
executed:BlockStatement
```

This is the same event contract verified by
`VirtualMachineHeadlessRuntimeEventTest` in `core/ast`.

### Round-trip fidelity

After saving and reopening a project, the test asserts:

- The program type name is unchanged.
- The method name on the reopened project's program type matches.
- The number of statements in the method body is unchanged.
- The Comment text content is unchanged.

## API reference

| Class | Module | Role |
| --- | --- | --- |
| `Project` | `core/ast` | Top-level project container; holds the program type and camera type. |
| `NamedUserType` | `core/ast` | The program type; extends `SProgram` via `JavaType.getInstance(SProgram.class)`. |
| `UserMethod` | `core/ast` | A method on the program type; contains a `BlockStatement` body. |
| `Comment` | `core/ast` | A no-op statement that carries text; simulates a student's first code tile. |
| `BlockStatement` | `core/ast` | The method body container. |
| `ReleaseVirtualMachine` | `core/ast` | Headless VM; `ENTRY_POINT_invoke(null, method)` runs a static method. |
| `VirtualMachineListener` | `core/ast` | Callback interface for statement execution events. |
| `IoUtilities` | `core/story-api-migration` | `writeProject`, `readProject` for `.a3p` archive serialization. |
| `FileProjectLoader` | `core/ide` | Production project loader; `TestFileProjectLoader` is a private inner class (same pattern as `ProjectOpenSaveExportJourneyTest`) that exposes the protected `load()` method. |
| `JavaType` | `core/ast` | `getInstance(SProgram.class)` provides the program supertype. |

## Configuration

No system properties or environment variables are required beyond
`NODE_OPTIONS=--max-old-space-size=32768` for the Maven reactor build.

The test uses `target/silver-thread-e2e/` and `target/silver-thread-starter/`
as working directories for temporary `.a3p` files. These files are created under Maven's build output
directory and are cleaned by `mvn clean`.

## Test resources

| Resource | Path | Source |
| --- | --- | --- |
| `indiaMinimum.a3p` | `core/ide/src/test/resources/starters/indiaMinimum.a3p` | Copied from `core/resources/src/application/resources/starter-projects/indiaMinimum.a3p`. |

This resource is a real Alice starter project. It is not a synthetic fixture.
The test treats it as opaque: it asserts structural properties (program type
name, `SProgram` assignability) without hardcoding internal type names that may
change between Alice versions.

## Claim boundaries

This test proves:

- A project with an `SProgram` type can be created, serialized, and deserialized
  headlessly.
- A `Comment` statement added to a method body survives save/reopen round-trips.
- `ReleaseVirtualMachine` dispatches `statementExecuting`/`statementExecuted`
  events for a static method containing a `Comment`.
- A real `.a3p` starter project can be loaded, copied, and reopened with program
  type fidelity.

This test does **not** prove:

| Non-claim | Reason |
| --- | --- |
| 3D rendering correctness | No scene graph, no display server. |
| Drag-and-drop UI | No Swing/JavaFX automation. |
| Gallery asset loading | No model resources referenced. |
| JavaFX display | No toolkit initialization. |
| Full lesson completion | No grading, assessment, or creative evaluation. |
| Multi-statement execution order | Only one `Comment` in the body. |
| Exception handling during execution | `Comment` is a no-op statement. |
| Installer behavior | No install4j or packaging. |
| Network or cloud features | Purely in-process. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Headless VM event dispatch (standalone) | `VirtualMachineHeadlessRuntimeEventTest` in `core/ast`. |
| Project Save/Export operation behavior | [Project Save and Export Operations](./project-save-export-operations.md). |
| First-lesson code-editor action proof | [First-Lesson Code-Editor Action Proof](./first-lesson-code-editor-action-proof.md). |
| Archive round-trip regression | [Project IO Corpus Characterization](./project-io-corpus-characterization.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](./silver-thread-status-report.md). |

## Examples

### Minimal project creation pattern

```java
NamedUserType programType = new NamedUserType();
programType.name.setValue("MyProgram");
programType.superType.setValue(JavaType.getInstance(SProgram.class));

Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
```

### Adding a statement to a method

```java
Comment comment = new Comment("student's first code tile");
UserMethod method = new UserMethod(
    "myFirstMethod",
    Void.TYPE,
    new UserParameter[0],
    new BlockStatement(comment));
method.isStatic.setValue(true);

programType.methods.getValue().add(method);
```

### VM execution with listener

```java
ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
vm.addVirtualMachineListener(listener);

vm.ENTRY_POINT_invoke(null, method);

// listener.statementEvents now contains the 4 expected events
```

### Loading a real starter project

```java
File starterFile = new File(
    getClass().getResource("/starters/indiaMinimum.a3p").toURI());
Project project = IoUtilities.readProject(starterFile);

NamedUserType programType = project.getProgramType();
// programType.getName() is not null and not empty
// programType is assignable to SProgram
```

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| `TestFileProjectLoader.loadNow()` returns null | Project file is empty or corrupt. | Check that `IoUtilities.writeProject` completed before the load call. |
| VM listener receives zero events | Method is not static and no instance was passed. | Set `method.isStatic.setValue(true)` and pass `null` as the instance. |
| `indiaMinimum.a3p` not found | Test resource not copied. | Verify `core/ide/src/test/resources/starters/indiaMinimum.a3p` exists. |
| `OutOfMemoryError` during Maven build | Insufficient heap for the reactor. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
