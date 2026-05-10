# Silver Thread Save Round-Trip End-to-End Test

This reference defines the honest save round-trip silver thread test for Alice.
The executable proof is `org.alice.ide.SilverThreadSaveRoundTripTest`. It is a
JUnit 4 test in `core/ide` that proves the production save path works headlessly:
create a project, modify the AST, save via `ProjectApplication.saveProjectTo`,
verify the archive on disk and the adopted URI, reopen via
`IoUtilities.readProject`, and verify full AST fidelity.

The test does not start JavaFX, load gallery assets, render a 3D scene, exercise
drag-and-drop UI, execute code through the virtual machine, or require a display.

## Contents

- [Scope](#scope)
- [Implementation status](#implementation-status)
- [Design decisions](#design-decisions)
- [Test method](#test-method)
- [Usage](#usage)
- [Behavior contract](#behavior-contract)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Relationship to issue #476](#relationship-to-issue-476)
- [Claim boundaries](#claim-boundaries)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## Scope

The test covers exactly one journey:

### Create → Modify AST → Save via Production Path → Verify Disk → Reopen → Verify AST Fidelity

```text
1. Create a Project with a NamedUserType via AstUtilities.createType extending SProgram
2. Add a UserMethod containing a Comment("silver-thread-save-marker") statement
3. Boot a headless TestProjectApplication with the project
4. Save via ProjectApplication.saveProjectTo(tempFile)
5. Assert the .a3p file exists and is non-empty
6. Assert the application URI was adopted to point to the saved file
7. Reopen the saved file via IoUtilities.readProject
8. Assert program type name, method name, and Comment text survived the round-trip
```

## Implementation status

| Surface | Location |
| --- | --- |
| `SilverThreadSaveRoundTripTest` | `core/ide/src/test/java/org/alice/ide/SilverThreadSaveRoundTripTest.java` |
| Maven validation | `mvn -pl core/ide -am -Dtest=SilverThreadSaveRoundTripTest test` |

## Design decisions

Four deliberate decisions, informed by the existing test suite:

1. **`AstUtilities.createType` instead of manual `NamedUserType` construction.**
   `SilverThreadLaunchBuildRunTest` uses `new NamedUserType()` with manual
   `name.setValue` and `superType.setValue` calls. This test uses
   `AstUtilities.createType` — the production factory — following the pattern
   established by `ProjectApplicationSaveProjectToTest`. Both approaches produce
   valid program types; `AstUtilities.createType` is one line instead of three.

2. **Save via `ProjectApplication.saveProjectTo`, not `IoUtilities.writeProject`.**
   This is the defining difference from `SilverThreadLaunchBuildRunTest`. The
   production save path exercises `ensureProjectCodeUpToDate`, `createThumbnail`,
   archive serialization, URI adoption, save backup, and recent project
   recording. Bugs in any of these steps would be invisible to a test that calls
   `IoUtilities.writeProject` directly. This is what makes the test "honest"
   per issue #476.

3. **Reopen via `IoUtilities.readProject`, not `TestFileProjectLoader`.**
   The save path is where the honesty matters; the reopen path uses the
   standard archive reader to prove the production-saved archive is valid.
   `SilverThreadLaunchBuildRunTest` uses `TestFileProjectLoader.loadNow()` for
   its reopen because it needs the `FileProjectLoader` integration. This test
   does not need that integration — it only needs to read the archive.

4. **No VM execution.**
   `SilverThreadLaunchBuildRunTest` proves the deserialized AST executes through
   `ReleaseVirtualMachine`. This test does not re-prove that. Its scope is
   strictly "production save → standard reopen → AST fidelity." Adding VM
   execution would duplicate the existing test without adding save-path coverage.

## Test method

### createModifiedAst_saveViaProjectApplication_reopenAndVerifyAstFidelity

| Step | API | Assertion |
| --- | --- | --- |
| Create program type | `AstUtilities.createType("SilverThreadSaveProgram", JavaType.getInstance(SProgram.class))` | Type is non-null. |
| Add method with Comment | `new UserMethod("saveRoundTripMethod", ...)` with `new Comment("silver-thread-save-marker")` in a `BlockStatement` | Method body statement count is 1. |
| Create project | `new Project(programType, SceneCameraType.WindowCamera)` | Project is non-null. |
| Boot application | `applicationWith(project, new InMemoryProjectLoader())` | Application singleton is set. |
| Save via production path | `application.saveProjectTo(tempFile)` | File exists, non-empty. |
| Verify URI adoption | `application.getUri()` | Equals `tempFile.toURI()`. |
| Reopen | `IoUtilities.readProject(tempFile)` | Reopened project is non-null. |
| Verify program type name | `reopened.getProgramType().getName()` | Equals `"SilverThreadSaveProgram"`. |
| Verify method name | `findMethodByName(reopenedType, "saveRoundTripMethod")` | Method is non-null. |
| Verify Comment text | `((Comment) firstStatement).text.getValue()` | Equals `"silver-thread-save-marker"`. |

## Usage

Run from the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadSaveRoundTripTest \
  test
```

The test runs without a display server, JavaFX toolkit, or network access.

## Behavior contract

### Production save pipeline exercised

When `saveProjectTo(file)` executes, the following production steps run:

| Step | Production behavior | Test behavior |
| --- | --- | --- |
| `ProjectSaveTargetPlan.choose()` | Selects save strategy (new, overwrite, backup) | **Production code — not stubbed** |
| URI adoption | Sets `uriProjectLoader` to a `FileProjectLoader` for the target (before write, rolled back on failure) | **Production code — not stubbed** |
| `ensureProjectCodeUpToDate()` | Synchronizes IDE code editor state to AST | No-op (`TestProjectApplication` override) |
| `createThumbnail()` | Renders 3D scene to `BufferedImage` | Returns 1×1 `BufferedImage` (`TestProjectApplication` override) |
| `IoUtilities.writeProject(file, project, dataSources)` | Writes `.a3p` archive with thumbnail and manifest | **Production code — not stubbed** |
| `backupSavedProject()` | Creates a save backup of the previous archive | **Production code — not stubbed** |
| `RecentProjectsListData.handleSave()` | Adds URI to recent projects list | **Production code — not stubbed** |

Five of seven steps are production code. The two overrides (`ensureProjectCodeUpToDate`,
`createThumbnail`) are the minimum needed for headless operation
and match the overrides in `ProjectApplicationSaveProjectToTest`.

### AST fidelity contract

After saving via the production path and reopening via `IoUtilities.readProject`,
the test asserts:

| Property | Expected value |
| --- | --- |
| Program type name | `"SilverThreadSaveProgram"` |
| Method name | `"saveRoundTripMethod"` |
| Comment text | `"silver-thread-save-marker"` |

These three properties are the minimum needed to prove the AST survived the
production save pipeline intact. If the save path corrupted the AST, silently
dropped methods, or lost statement content, at least one assertion would fail.

## API reference

| Class | Module | Role in this test |
| --- | --- | --- |
| `ProjectApplication` | `core/ide` | Production application; `saveProjectTo(File)` is the method under test. |
| `TestProjectApplication` | test inner class | Headless subclass; overrides `updateTitle` (safety — not called during save but prevents NPE from other paths), `createThumbnail`, `ensureProjectCodeUpToDate`, `forceProjectCodeUpToDate`, plus required abstract method stubs. |
| `InMemoryProjectLoader` | test inner class | Stub `UriProjectLoader`; `isNewProject()` returns `false`. |
| `AstUtilities` | `core/ast` | `createType(name, superType)` — production factory for `NamedUserType`. |
| `Project` | `core/ast` | Top-level project container. |
| `NamedUserType` | `core/ast` | The program type. |
| `UserMethod` | `core/ast` | Method containing the `BlockStatement` body. |
| `Comment` | `core/ast` | Marker statement with known text. |
| `BlockStatement` | `core/ast` | Method body container. |
| `IoUtilities` | `core/story-api-migration` | `readProject(File)` — standard archive reader used for reopen. |
| `JavaType` | `core/ast` | `getInstance(SProgram.class)` — program supertype. |
| `Application` | `croquet-core` | Singleton reset via reflection in `resetApplicationSingleton()`. |

### Inner class patterns

Both `TestProjectApplication` and `InMemoryProjectLoader` are private inner
classes copied from `ProjectApplicationSaveProjectToTest`. They cannot be shared
as library classes because `TestProjectApplication` depends on protected
`ProjectApplication` methods and `InMemoryProjectLoader` is a minimal stub.

The reflection helpers `resetApplicationSingleton()` and `installLoader()` are
also copied from `ProjectApplicationSaveProjectToTest`. They access private
fields via `setAccessible(true)`.

## Configuration

No system properties or environment variables are required beyond
`NODE_OPTIONS=--max-old-space-size=32768` for the Maven reactor build.

The test uses JUnit's `@Rule TemporaryFolder` for the save target file. The
folder and its contents are automatically deleted after the test completes,
including on failure.

## Relationship to issue #476

[RabbitHole issue #476](https://github.com/rysweet/RabbitHole/issues/476) asks
for "an honest Save e2e silver thread test" that:

| Requirement | How this test satisfies it |
| --- | --- |
| Create a project with a known modification | `AstUtilities.createType` + `UserMethod` with `Comment("silver-thread-save-marker")`. |
| Save via `SaveProjectOperation`, not just `IoUtilities.writeProject` | `ProjectApplication.saveProjectTo(File)` — the production save path that `SaveProjectOperation` delegates to. |
| Verify file on disk | `assertTrue(target.isFile())` and `assertTrue(target.length() > 0)`. |
| Reopen and verify round-trip | `IoUtilities.readProject(target)` followed by program type name, method name, and Comment text assertions. |
| Must work headlessly | `TestProjectApplication` with no-op overrides; no display, JavaFX, or network. |

## Claim boundaries

This test proves:

- `ProjectApplication.saveProjectTo` writes a valid `.a3p` archive when given a
  headless `TestProjectApplication` with a synthetic project containing a
  `UserMethod` and `Comment`.
- The production save path adopts the target file as the project URI.
- The saved archive is readable by `IoUtilities.readProject`.
- The program type name, method name, and `Comment` text survive the production
  save → standard reopen cycle.

This test does **not** prove:

| Non-claim | Reason |
| --- | --- |
| 3D rendering correctness | Thumbnail is a 1×1 stub. |
| `ensureProjectCodeUpToDate` correctness | Stubbed as no-op. |
| VM execution of saved AST | Not tested; owned by `SilverThreadLaunchBuildRunTest`. |
| Save backup creation | Not tested; owned by `ProjectApplicationSaveProjectToTest`. |
| Save-As behavior | Not tested; owned by `ProjectApplicationSaveProjectToTest`. |
| Failed save recovery | Not tested; owned by `ProjectApplicationSaveProjectToTest`. |
| Drag-and-drop UI | No Swing/JavaFX automation. |
| Gallery or model resource loading | No resources referenced. |
| JavaFX display | No toolkit initialization. |
| Lesson completion or grading | No assessment logic. |
| Starter project round-trip | Not tested; owned by `SilverThreadLaunchBuildRunTest`. |
| Desktop UI automation | No menu, dialog, or windowing interaction. |

Adjacent claims are owned by their own documents:

| Claim | Document |
| --- | --- |
| Headless create → build → run → save → reopen | [Silver Thread Launch-Build-Run Test](./silver-thread-launch-build-run-test.md). |
| `saveProjectTo` edge cases (backup, Save-As, failure) | [Project Save and Export Operations](./project-save-export-operations.md). |
| Archive structure and validation | [Project IO Corpus Characterization](./project-io-corpus-characterization.md). |
| Robot Save menu dialog proof | [Robot Save Menu Dialog Write-Readback Proof](./robot-save-menu-dialog-write-readback-proof.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](./silver-thread-status-report.md). |

## Examples

### Minimal production-save round-trip pattern

```java
// Create a project with a known AST modification
NamedUserType programType = AstUtilities.createType(
    "MyProgram", JavaType.getInstance(SProgram.class));
Comment marker = new Comment("my-test-marker");
UserMethod method = new UserMethod(
    "myMethod", Void.TYPE, new UserParameter[0],
    new BlockStatement(marker));
programType.methods.add(method);
Project project = new Project(programType, Project.SceneCameraType.WindowCamera);

// Boot a headless application and save via the production path
TestProjectApplication app = applicationWith(project, new InMemoryProjectLoader());
File target = temporaryFolder.newFile("my-test.a3p");
app.saveProjectTo(target);

// Verify the archive is valid and the AST survived
Project reopened = IoUtilities.readProject(target);
assertEquals("MyProgram", reopened.getProgramType().getName());
```

### Resetting the Application singleton between tests

```java
private static void resetApplicationSingleton() throws Exception {
  Field field = Application.class.getDeclaredField("singleton");
  field.setAccessible(true);
  field.set(null, null);
}
```

This is required because `Application` enforces a single instance. Without the
reset, constructing a second `TestProjectApplication` throws an
`IllegalStateException`.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `require-tweedle-lang-submodule` enforcer failure | Grammar submodule not initialized. | Run `git submodule update --init tweedle-lang`. |
| `IllegalStateException: singleton already set` | Previous test left the `Application` singleton alive. | Verify `resetApplicationSingleton()` runs before each `TestProjectApplication` construction. |
| `NullPointerException` in `updateTitle` | Using production `updateTitle` instead of the no-op override. | Ensure `TestProjectApplication` overrides `updateTitle()`. |
| `IoUtilities.readProject` returns null | Archive file is empty or corrupt. | Check that `saveProjectTo` completed without exception before the read. |
| `OutOfMemoryError` during Maven build | Insufficient heap for the reactor. | Set `NODE_OPTIONS=--max-old-space-size=32768`. |
| Test not found by Surefire | Wrong test name or package. | Use `-Dtest=org.alice.ide.SilverThreadSaveRoundTripTest`. |
