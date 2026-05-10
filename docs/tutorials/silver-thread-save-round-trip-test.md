# Tutorial: Trace the Silver Thread Save Round-Trip Test

This tutorial walks through the honest save round-trip silver thread test for
Alice. You will trace the test method to understand how the production save path
is proven headlessly: from AST modification through `ProjectApplication.saveProjectTo`
to archive verification and AST fidelity confirmation.

For the full contract, see the [Silver Thread Save Round-Trip Test
reference](../reference/silver-thread-save-round-trip-test.md).

## Contents

- [Goal](#goal)
- [1. Understand the motivation](#1-understand-the-motivation)
- [2. Trace project creation and AST modification](#2-trace-project-creation-and-ast-modification)
- [3. Trace the TestProjectApplication bootstrap](#3-trace-the-testprojectapplication-bootstrap)
- [4. Trace the production save path](#4-trace-the-production-save-path)
- [5. Trace disk verification](#5-trace-disk-verification)
- [6. Trace reopen and AST fidelity verification](#6-trace-reopen-and-ast-fidelity-verification)
- [7. Run the test](#7-run-the-test)
- [8. Compare with the existing silver thread tests](#8-compare-with-the-existing-silver-thread-tests)
- [9. Understand the boundaries](#9-understand-the-boundaries)

## Goal

Understand how `SilverThreadSaveRoundTripTest` proves that Alice's production
save pipeline preserves AST fidelity without requiring a display, JavaFX, or
user interaction. After this tutorial you will be able to explain why this test
uses `ProjectApplication.saveProjectTo` instead of `IoUtilities.writeProject`,
what the `TestProjectApplication` pattern enables, and what the round-trip
fidelity assertions prove.

Open the source in
`core/ide/src/test/java/org/alice/ide/SilverThreadSaveRoundTripTest.java`
alongside this guide.

## 1. Understand the motivation

The existing `SilverThreadLaunchBuildRunTest` saves via `IoUtilities.writeProject`.
That proves the low-level archive writer works, but it skips the production code
path that students actually use. When a student clicks File → Save in the IDE,
the call goes through:

```text
ProjectApplication.saveProjectTo(File)
  → ProjectSaveTargetPlan.choose() — plan the save
  → uriProjectLoader = nextLoader — adopt new URI before the write
  → ProjectFileUtilities.saveCopyOfProjectTo()
      → getUpToDateProject() → ensureProjectCodeUpToDate()
      → thumbnailAndManifestDataSources() → createThumbnail()
      → IoUtilities.writeProject(file, project, dataSources)
  → backupSavedProject() — create save backup
  → RecentProjectsListData.handleSave() — record in recent projects
  → updateHistoryIndexFileSync() — persist save index
```

`SilverThreadSaveRoundTripTest` exercises this full path. It is "honest" because
a bug in `ensureProjectCodeUpToDate`, `createThumbnail`, URI adoption, or the
save-plan logic would break this test but pass the lower-level
`IoUtilities.writeProject` test.

## 2. Trace project creation and AST modification

Find the project creation in the test method:

```java
NamedUserType programType = AstUtilities.createType(
    "SilverThreadSaveProgram",
    JavaType.getInstance(SProgram.class));
```

Unlike `SilverThreadLaunchBuildRunTest`, which uses `new NamedUserType()` with
manual property assignment, this test uses `AstUtilities.createType`. This is
the production factory used by `ProjectApplicationSaveProjectToTest` — it sets
up the type hierarchy in one call.

Next, find the AST modification:

```java
Comment marker = new Comment("silver-thread-save-marker");
UserMethod method = new UserMethod(
    "saveRoundTripMethod",
    Void.TYPE,
    new UserParameter[0],
    new BlockStatement(marker));
programType.methods.add(method);
```

Key decisions:

- **`Comment`** is chosen because it is the simplest no-op statement. It
  provides a known text marker that can be verified after the round-trip.
- The method does **not** need `isStatic` because this test does not execute
  the method through the virtual machine. The save path does not invoke code.
- The marker text `"silver-thread-save-marker"` is deliberately distinct from
  the `SilverThreadLaunchBuildRunTest` marker to avoid confusion in test output.

## 3. Trace the TestProjectApplication bootstrap

Find the application bootstrap:

```java
TestProjectApplication application = applicationWith(project, new InMemoryProjectLoader());
```

This calls two helper methods:

1. **`resetApplicationSingleton()`** — Uses reflection to null the
   `Application.singleton` static field. Without this, a previous test's
   `Application` instance would prevent construction. This is the same
   pattern used in `ProjectApplicationSaveProjectToTest`.

2. **`installLoader()`** — Uses reflection to set the `uriProjectLoader`
   field on the `ProjectApplication`. The `InMemoryProjectLoader` returns
   `isNewProject() = false`, making `saveProjectTo` treat this as an
   existing (not brand-new) project.

The `TestProjectApplication` class overrides four methods to enable headless
operation, plus the abstract methods required by `ProjectApplication`:

| Override | Purpose |
| --- | --- |
| `updateTitle()` | No-op. Not called during `saveProjectTo`, but prevents NPE if other code paths trigger it. Production version requires `documentFrame`. |
| `createThumbnail()` | Returns a 1×1 `BufferedImage`. Production version renders the 3D scene. Called during `saveCopyOfProjectTo`. |
| `ensureProjectCodeUpToDate()` | No-op. Production version synchronizes the code editor state. Called via `getUpToDateProject()`. |
| `forceProjectCodeUpToDate()` | No-op. Same as above but used by the export path. |

Additional abstract method stubs (`getAboutOperation`, `getPreferencesOperation`,
`handleOpenFiles`, `handleWindowOpened`, `handleQuit`, `getApplicationSubPath`)
are required for compilation but not exercised by the save path.

## 4. Trace the production save path

Find the save call:

```java
File target = temporaryFolder.newFile("silver-thread-save.a3p");
application.saveProjectTo(target);
```

`saveProjectTo` is the production method on `ProjectApplication`. Tracing into
the production code, it:

1. Creates a `ProjectSaveTargetPlan` for the target file.
2. Adopts the new URI by setting `uriProjectLoader` to the plan's next loader.
   This happens **before** the write, so a failed write can roll back the loader.
3. Delegates to `ProjectFileUtilities.saveCopyOfProjectTo`, which calls
   `getUpToDateProject()` → `ensureProjectCodeUpToDate()` (no-op in
   `TestProjectApplication`) and then
   `IoUtilities.writeProject(file, project, thumbnailAndManifestDataSources)`.
4. `thumbnailAndManifestDataSources` calls `createThumbnail()` — returns the
   1×1 stub in `TestProjectApplication`.
5. Creates a save backup via `backupSavedProject()`.
6. Records the URI via `RecentProjectsListData.handleSave()`.

The test is honest about the save path because all six steps execute. The
`TestProjectApplication` stubs only make `ensureProjectCodeUpToDate` and
`createThumbnail` safe in a headless environment — the save plan, URI adoption,
archive write, backup, and recent project recording are all production code.

## 5. Trace disk verification

Find the disk assertions:

```java
assertTrue("Saved file must exist on disk", target.isFile());
assertTrue("Saved file must be non-empty", target.length() > 0);
assertEquals(target.toURI(), application.getUri());
```

Three facts are verified:

1. The file exists — `saveProjectTo` wrote something.
2. The file has content — the archive is not a zero-byte stub.
3. The application URI points to the saved file — `saveProjectTo` adopted the
   target, which is the production behavior that determines where future saves go.

## 6. Trace reopen and AST fidelity verification

Find the reopen and assertions:

```java
Project reopened = IoUtilities.readProject(target);
assertNotNull(reopened);
assertEquals("SilverThreadSaveProgram", reopened.getProgramType().getName());
```

The reopen deliberately uses `IoUtilities.readProject` — the standard archive
reader — rather than `TestFileProjectLoader`. This proves the archive written by
the production save path is readable by the standard reader. It's a one-way
honesty test: production save → standard reopen.

The AST fidelity assertions verify three properties survived the round-trip:

| Property | Assertion |
| --- | --- |
| Program type name | `assertEquals("SilverThreadSaveProgram", ...)` |
| Method name | `assertEquals("saveRoundTripMethod", ...)` |
| Comment marker text | `assertEquals("silver-thread-save-marker", ...)` |

This is stronger than a file-exists check. It proves the production save
pipeline preserved the AST structure, method identity, and statement content
through the full serialize → archive → deserialize pipeline.

## 7. Run the test

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadSaveRoundTripTest \
  test
```

The test passes without a display server or network access.

## 8. Compare with the existing silver thread tests

| Dimension | `SilverThreadLaunchBuildRunTest` | `SilverThreadSaveRoundTripTest` |
| --- | --- | --- |
| Save path | `IoUtilities.writeProject` (low-level) | `ProjectApplication.saveProjectTo` (production) |
| Reopen path | `TestFileProjectLoader.loadNow()` | `IoUtilities.readProject` |
| VM execution | Yes — proves the deserialized AST executes | No — save fidelity only |
| Requires `ProjectApplication` | No | Yes — `TestProjectApplication` with reflection setup |
| URI adoption assertion | No | Yes — verifies application URI points to saved file |
| Starter project journey | Yes (`indiaMinimum.a3p`) | No — synthetic project only |
| Double round-trip | Yes (save → reopen → save → reopen) | No — single production-save round-trip |

The two tests are complementary:

- `SilverThreadLaunchBuildRunTest` proves the core engine pipeline works
  (create → build → run → save → reopen) using the low-level archive API.
- `SilverThreadSaveRoundTripTest` proves the production save pipeline works
  (modify → save via `saveProjectTo` → reopen → verify AST fidelity) using
  the same code path as File → Save.

## 9. Understand the boundaries

After tracing the test, confirm you can answer:

| Question | Answer |
| --- | --- |
| Does this prove rendering works? | No. Thumbnail is a 1×1 stub. |
| Does this prove `ensureProjectCodeUpToDate` works? | No. It is stubbed as a no-op. |
| Does this prove Save-As works? | No. `ProjectApplicationSaveProjectToTest` covers that. |
| Does this prove save backups are created? | No. `ProjectApplicationSaveProjectToTest` covers that. |
| Does this prove the VM can execute the saved project? | No. `SilverThreadLaunchBuildRunTest` covers that. |
| What does it prove? | The production `saveProjectTo` path writes a valid archive whose AST content survives a standard reopen. |

The test is intentionally focused on the one gap the existing silver thread
tests leave open: the production save path. It does not re-prove rendering,
VM execution, backup mechanics, or UI automation — those are owned by
adjacent tests.
