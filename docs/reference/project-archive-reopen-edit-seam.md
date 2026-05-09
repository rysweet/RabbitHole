# Project Archive Reopen/Edit Seam

The project archive reopen/edit seam is the repository-owned contract for
opening an Alice `.a3p` archive from disk, editing the loaded project model,
writing the project again, reopening it, and exporting it as `.a3w` without
desktop UI automation.

There are two coverage levels. The lower-bound headless journey protects archive
write, reopen, second write, second reopen, export, and readback. The complete
reopen/edit seam additionally requires an edit persistence assertion.

Use this reference with
[Validate the Project Archive Reopen/Edit Seam](../howto/validate-project-archive-reopen-edit-seam.md)
and
[Tutorial: Trace the Project Archive Reopen/Edit Seam](../tutorials/trace-project-archive-reopen-edit-seam.md).

## Contents

- [Contract](#contract)
- [API surfaces](#api-surfaces)
- [Loader behavior](#loader-behavior)
- [Archive write and export behavior](#archive-write-and-export-behavior)
- [Root-detection contract for guard scripts](#root-detection-contract-for-guard-scripts)
- [Configuration](#configuration)
- [Validation](#validation)
- [Boundaries](#boundaries)

## Contract

The seam proves this bounded journey:

```text
Given a valid Alice project archive on disk
When FileProjectLoader opens it
And the loaded Project is edited in memory
And the edited Project is written to a new .a3p archive
And the edited archive is reopened
And the reopened Project is exported to .a3w
Then the edited project-owned state survives the second reopen
And the exported archive is readable through the production project archive reader
```

The journey stays below Alice desktop Save UI. It uses production project archive
read/write/export code and direct file-backed loader behavior.

## API surfaces

| Surface | Role |
| --- | --- |
| `org.alice.ide.uricontent.FileProjectLoader(File)` | Opens an existing file-backed project archive and reports the original archive URI. |
| `org.alice.ide.uricontent.FileProjectLoader(File, boolean)` | Opens the same archive with optional VR-ready save URI remapping. |
| `org.alice.ide.uricontent.AbstractFileProjectLoader.load()` | Protected loader seam that rejects non-project paths, uses `IoUtilities.projectReader(File)`, applies resource helpers, and returns `null` for rejected or failed loads. |
| `org.alice.ide.uricontent.AbstractFileProjectLoader.handleLoadException(File, Exception)` | Hook for loader-specific IO failure handling. |
| `org.alice.ide.uricontent.UriProjectLoader.getUri()` | Reports the active project URI used for save/reopen classification. |
| `org.alice.ide.uricontent.UriProjectLoader.shouldBeSaved()` | Reports whether the loader points at a save destination that does not exist yet. |
| `org.alice.ide.uricontent.UriProjectLoader.getMainProjectFile()` | Maps normal project archives and named backup archives back to their main project file. |
| `org.alice.ide.ProjectFileUtilities.saveCopyOfProjectTo(File)` | Adjacent IDE save-copy surface covered by `ProjectFileUtilitiesTest`; not exercised by `ProjectOpenSaveExportJourneyTest`. |
| `org.alice.ide.ProjectFileUtilities.exportCopyOfProjectTo(File)` | Adjacent IDE export-copy surface covered by `ProjectFileUtilitiesTest`; not exercised by `ProjectOpenSaveExportJourneyTest`. |
| `org.lgna.project.io.IoUtilities.writeProject(File, Project)` | Lower-level archive writer used by headless characterization tests. |
| `org.lgna.project.io.IoUtilities.exportProject(File, Project)` | Lower-level player archive exporter used by headless characterization tests. |
| `org.lgna.project.io.IoUtilities.readProject(File)` | Production reader used to verify saved and exported archives. |

### Headless loader example

Tests that need to call the protected loader seam expose it through a small
subclass instead of widening production API:

```java
private static class TestFileProjectLoader extends FileProjectLoader {
  TestFileProjectLoader(File file) {
    super(file);
  }

  Project loadNow() {
    return load();
  }
}
```

A complete headless reopen/write/export flow uses real temporary files. To make
it the reopen/edit seam, mutate the loaded project before the second write and
assert the edited state after the second reopen and export readback:

```java
File originalProjectFile = workingDirectory.resolve("classroom.a3p").toFile();
File savedProjectFile = workingDirectory.resolve("classroom-copy.a3p").toFile();
File exportedProjectFile = workingDirectory.resolve("classroom-export.a3w").toFile();

Project originalProject = new Project(programType("ClassroomProgram"), Project.SceneCameraType.WindowCamera);
IoUtilities.writeProject(originalProjectFile, originalProject);

Project loadedProject = new TestFileProjectLoader(originalProjectFile).loadNow();
loadedProject.getProgramType().name.setValue("EditedClassroomProgram");

IoUtilities.writeProject(savedProjectFile, loadedProject);
Project reopenedProject = new TestFileProjectLoader(savedProjectFile).loadNow();

IoUtilities.exportProject(exportedProjectFile, reopenedProject);
Project exportedProject = IoUtilities.readProject(exportedProjectFile);
```

The required assertion is the edited project-owned state after the second reopen
and exported archive readback. A file-exists assertion alone is not enough.

## Loader behavior

`FileProjectLoader` accepts valid Alice 3 project archives and rejects invalid
or unsupported inputs without returning a partial project.

| Input | Loader behavior |
| --- | --- |
| Existing valid `.a3p` archive | Returns a `Project` whose program type and resources were read through the production project archive reader. |
| Corrupt archive bytes | Returns `null` after delegating the IO failure to `handleLoadException(File, Exception)`. |
| Missing file | Returns `null` after surfacing the existing unable-to-open-file path. |
| Alice 2 `.a2w` file | Returns `null`; Alice 3 does not load Alice 2 worlds through this loader. |
| Alice type archive `.a3c` | Returns `null`; type archives are not project files. |
| Future-version project that the user declines to open | Returns `null`. |

URI classification remains part of the seam:

| Loader state | Expected classification |
| --- | --- |
| Normal project file | `getUri()` is the project file URI, `shouldBeSaved()` is `false` when the file exists, `isBackup()` is `false`, and `getMainProjectFile()` returns the file. |
| VR-ready loader | `getUri()` points to the sibling ` VR.a3p` save target and `shouldBeSaved()` is `true` until that target exists. |
| Named backup file in `<project>.bak/` | `isBackup()` is `true`, `isDefaultBackup()` is `false`, and `getMainProjectFile()` resolves to the sibling `<project>.a3p`. |
| Default backup in `.defaultbak/` | `isBackup()` and `isDefaultBackup()` are `true`; no main project file is inferred. |
| New project loader | Not a backup and no main project file. |

## Archive write and export behavior

The complete saving, reopening, editing, saving again, reopening again, and
exporting journey preserves project-owned model data through real archive bytes:

1. Write the original project to `.a3p`.
2. Reopen the `.a3p` through `FileProjectLoader`.
3. Edit the reopened `Project`.
4. Write the edited project to another `.a3p`.
5. Reopen the edited `.a3p` through `FileProjectLoader`.
6. Assert the edit survived.
7. Export the reopened edited project to `.a3w`.
8. Read the `.a3w` with `IoUtilities.readProject(File)`.

Project archive tests may inspect stable archive entries such as
`manifest.json`, `programType.xml`, and exported Tweedle source entries when
that structure is the behavior under review. They should not depend on desktop
rendering, native file choosers, or user event timing.

## Root-detection contract for guard scripts

The repo-owned no-op guard entrypoint is
`scripts/project-archive-reopen-edit-noop-guard.sh`. It must evaluate the actual
git-linked worktree root before it decides whether a change is empty. It must not
compare a copied session directory, detached artifact directory, or non-git path.

The guard resolves the repository root with git:

```bash
git rev-parse --show-toplevel
```

When a candidate path is supplied, the guard runs root detection from that path:

```bash
git -C "$candidate_path" rev-parse --show-toplevel
```

The resolved root is the only directory used for git status or diff checks:

```bash
git -C "$repo_root" status --short
git -C "$repo_root" diff --name-only
```

If root detection fails, the guard must fail clearly instead of treating the path
as a clean no-op. This protects linked worktrees and avoids false failures caused
by session copies.

## Configuration

Set the saved Node memory preference before Maven validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

Initialize the required Tweedle grammar submodule in every fresh checkout or
worktree before focused or broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

No network service, credential, GitHub token, desktop display, or new product
preference is required for the archive reopen/edit seam.

## Validation

Run the focused `core/ide` characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.ProjectOpenSaveExportJourneyTest,org.alice.ide.uricontent.FileProjectLoaderTest \
  test
```

The focused command is ready for the complete seam when
`ProjectOpenSaveExportJourneyTest` includes the deterministic edit assertion and
proves the headless project archive journey, while `FileProjectLoaderTest` proves
valid, invalid, VR-ready, and backup classification behavior.

## Boundaries

This seam does not claim:

- desktop Save menu completion;
- native or Swing file chooser automation;
- full UI automation;
- visible rendering correctness;
- grading or learner assessment correctness;
- full first-lesson completion;
- runtime player behavior beyond reading the exported archive through the project archive reader.

Use the separate Save-menu proof lane for bounded desktop Save evidence. Keep
this seam focused on repository-owned project archive reader/writer behavior.
