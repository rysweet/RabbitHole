# Project Load and Backup Recovery Characterization

This reference describes the `core/ide` project load and backup recovery
characterization that uses real temporary Alice project files.

## Contents

- [Feature scope](#feature-scope)
- [Direct saved-project load characterization](#direct-saved-project-load-characterization)
- [User-visible recovery behavior](#user-visible-recovery-behavior)
- [API and seam reference](#api-and-seam-reference)
- [Characterization examples](#characterization-examples)
- [Configuration](#configuration)
- [Compatibility rules](#compatibility-rules)
- [Validation](#validation)

## Feature scope

The recovery characterization lives in:

```text
core/ide/src/test/java/org/alice/ide/ProjectBackupRecoveryIoTest.java
```

The direct loader characterization lives in:

```text
core/ide/src/test/java/org/alice/ide/uricontent/FileProjectLoaderTest.java
```

Together, these tests protect the boundary between saved project load behavior,
corrupt project rejection, backup selection, recovery planning, user-visible
failure dispatch, and production archive read/write behavior. The tests create
`.a3p` files under JUnit `TemporaryFolder`; they do not use checked-in Alice
archives, Git LFS payloads, Sims assets, broad UI automation, or desktop launch
fixtures.

The direct loader characterization is intentionally narrow. In the current
implementation, its saved-project and corrupt-project behaviors live in one
focused JUnit 4 test method:

```text
savedTemporaryProjectLoadsAndCorruptTemporaryProjectIsRejected
```

It writes a generated project to a real temporary `.a3p` file through the
existing Alice project writer, loads that saved file through the same-package
`FileProjectLoader` protected `load()` seam, and checks the current negative
path by loading a second corrupt temporary `.a3p` file through the same boundary.

The recovery test covers two recovery journeys:

| Journey | Protected behavior |
| --- | --- |
| Corrupt main project, corrupt newest backup, readable older backup | The corrupt main project loads as `null`, the unreadable newest backup is skipped, the next backup is offered, and the readable backup reopens with its program type and resources intact. |
| Corrupt main project and all backups corrupt | Alice attempts recovery in newest-first order, marks each failed backup unloadable, exhausts candidates, and plans the user-visible new-project failure dispatch. |

## Direct saved-project load characterization

Use `FileProjectLoaderTest` when a change touches the IDE file-loader boundary
for a normal `.a3p` file. The characterization is deliberately narrower than
backup recovery:

| Temporary input | Loader behavior |
| --- | --- |
| Generated `.a3p` written by `IoUtilities.writeProject(...)` | The same-package test calls the protected `FileProjectLoader.load()` seam and receives a non-null `Project` whose generated program metadata is still readable. |
| Corrupt `.a3p` containing deterministic invalid bytes | The same protected loader seam returns `null`, matching the current safe rejection behavior used by recovery code. |

These tests create both files with JUnit `TemporaryFolder`. They do not read
sample projects, user files, external paths, LFS assets, or committed binary
fixtures.
The saved-project assertion should name only the generated temporary project load
behavior. The corrupt-project assertion should name only current rejection
behavior; it is not broad archive-recovery coverage. Keep these as focused
characterization tests rather than combining them with unrelated loader behavior.

## User-visible recovery behavior

When Alice cannot load the primary `.a3p` project, recovery follows this
observable contract:

1. The failed primary load does not replace the current project with a partially
   decoded project.
2. Alice checks trusted backup candidates inside the backup directory in
   newest-first order.
3. Missing, unsafe, symlinked, out-of-directory, or already unloadable backup
   candidates are not offered.
4. Alice offers the next trusted candidate for recovery.
5. If the user accepts a candidate and that backup fails to load, Alice records
   that backup as unloadable and offers the next candidate.
6. If no backup remains, Alice dispatches the same user-visible failure path as
   the current application flow: no project is loaded and the new-project path is
   shown.

The characterization deliberately stays above broad UI automation. It verifies
the dispatch plan that drives user-visible behavior instead of clicking dialogs.
The formal-spec artifacts use "readable backup" as the recovery outcome; the
Java implementation discovers readability by attempting the accepted backup load,
not by pre-decoding every candidate before prompting.

## API and seam reference

The recovery classes are `core/ide` implementation seams, not public extension
APIs. Tests live in the same package so they can characterize package-private
and protected behavior without widening production visibility.

| Seam | Contract |
| --- | --- |
| `FileProjectLoader` | Attempts to load a project file through the IDE file-loader boundary. Its inherited `load()` method is protected, so same-package tests call that protected seam directly. A saved generated `.a3p` returns a `Project`; corrupt load failures are represented as a `null` project so `ProjectApplication` can run backup recovery UI. |
| `ProjectBackupSelector.getNextBackup(...)` | Selects the next trusted backup candidate. For corrupt main projects, it returns the newest available candidate without comparing backup creation time. For recent-backup probes, it only returns a backup newer than the project. |
| `ProjectLoadFailurePlan.choose(...)` | Chooses the next recovery action after a project or backup load failure. Actions include prompting for a backup, showing backup load failure, showing all-backups failure, and prompting for the main project. |
| `ProjectLoadFailureDispatchPlan.afterUserChoice(...)` | Converts a recovery action and user acceptance into a load target (`BACKUP`, `MAIN_PROJECT`, or `NONE`) and whether Alice should show a new project. |

These seams preserve the existing load/recovery behavior while making the
decision points directly testable.

## Characterization examples

### Load a saved temporary project and reject a corrupt temporary project

Create a generated project archive with the existing project writer:

```java
File savedProject = temporaryFolder.newFile("saved-generated-world.a3p");
Project project = new Project(
    programType("GeneratedProgram"),
    Project.SceneCameraType.WindowCamera);
IoUtilities.writeProject(savedProject, project);
```

Load the saved archive through the same protected IDE file-loader seam that opens
a normal project file. This call belongs in a same-package test:

```java
Project loadedProject = new FileProjectLoader(savedProject).load();

assertNotNull(loadedProject);
assertEquals("GeneratedProgram", loadedProject.getProgramType().getName());
assertEquals(
    Project.SceneCameraType.WindowCamera,
    loadedProject.createSaveManifest().projectStructure.sceneCameraType);
```

Then create a second real temporary `.a3p` file with deterministic invalid bytes
and assert the current rejection behavior through `FileProjectLoader`:

```java
File corruptProject = temporaryFolder.newFile("corrupt-generated-world.a3p");
Files.writeString(
    corruptProject.toPath(),
    "not an Alice project archive",
    StandardCharsets.UTF_8);

Project rejectedProject = new FileProjectLoader(corruptProject).load();

assertNull(rejectedProject);
```

Keep helper code limited to constructing the minimal generated `Project` and
avoid adding fixtures, dependencies, or production rewrites. The saved-project
and corrupt-project checks should remain in one focused characterization method
so the test does not imply broader recovery coverage.

### Recover from a readable older backup

Use real temporary files for both the corrupt inputs and the readable backup:

```java
File corruptMainProject = temporaryFolder.newFile("world.a3p");
Files.writeString(corruptMainProject.toPath(), "not a project archive", StandardCharsets.UTF_8);

File backupDirectory = temporaryFolder.newFolder("world.bak");
File corruptNewestBackup = new File(backupDirectory, "auto20240102_140000.a3p");
Files.writeString(corruptNewestBackup.toPath(), "not a backup archive", StandardCharsets.UTF_8);

File validBackup = new File(backupDirectory, "auto20240102_130000.a3p");
Project backupProject = new Project(programType("RecoveredProgram"), Project.SceneCameraType.WindowCamera);
backupProject.addResource(new TestResource("note.txt", "text/plain", "recovered notes".getBytes(StandardCharsets.UTF_8)));
IoUtilities.writeProject(validBackup, backupProject);
```

Then load and plan through the production seams:

```java
Project mainProject = new TestFileProjectLoader(corruptMainProject).loadNow();
File backup = selector.getNextBackup(
    LocalDateTime.MIN,
    backupDirectory,
    new File[] {corruptNewestBackup, validBackup},
    true,
    Set.of(corruptNewestBackup.getName()));
ProjectLoadFailurePlan plan = ProjectLoadFailurePlan.choose(
    false,
    false,
    true,
    false,
    backup,
    corruptMainProject);
Project recoveredProject = new TestFileProjectLoader(plan.getBackupToLoad()).loadNow();
```

The assertions should prove that the main project failed safely, the readable
backup was selected, and the recovered project still contains the expected
program type and resource metadata.

### Exhaust corrupt backups and dispatch new project

When every candidate is corrupt, the characterization records each failed
candidate as unloadable and asks the selector again:

```java
Set<String> unloadableFiles = new HashSet<>();
unloadableFiles.add(corruptMainProject.getName());

File newestBackup = selector.getNextBackup(
    LocalDateTime.MIN,
    backupDirectory,
    newestFirstBackups,
    true,
    unloadableFiles);
Project newestBackupProject = new TestFileProjectLoader(newestBackup).loadNow();
unloadableFiles.add(corruptNewestBackup.getName());

File olderBackup = selector.getNextBackup(
    LocalDateTime.MIN,
    backupDirectory,
    newestFirstBackups,
    true,
    unloadableFiles);
Project olderBackupProject = new TestFileProjectLoader(olderBackup).loadNow();
unloadableFiles.add(corruptOlderBackup.getName());
```

After the final candidate fails, no backup remains:

```java
File exhaustedBackups = selector.getNextBackup(
    LocalDateTime.MIN,
    backupDirectory,
    newestFirstBackups,
    true,
    unloadableFiles);
ProjectLoadFailurePlan exhaustedRecoveryPlan = ProjectLoadFailurePlan.choose(
    true,
    true,
    true,
    false,
    exhaustedBackups,
    corruptOlderBackup);
ProjectLoadFailureDispatchPlan dispatch = ProjectLoadFailureDispatchPlan.afterUserChoice(
    exhaustedRecoveryPlan.getAction(),
    false);
```

The expected terminal state is:

```text
exhaustedBackups == null
exhaustedRecoveryPlan.getAction() == SHOW_PROJECT_AND_ALL_BACKUPS_LOAD_ERROR
dispatch.getLoadTarget() == NONE
dispatch.shouldShowNewProject() == true
```

That state is the documented user-visible failure path for a corrupt main
project when every backup also fails to load.

## Configuration

Project backup recovery has no runtime configuration flag. It uses the existing
Alice project load, backup directory, and recovery dialog behavior.

The direct `FileProjectLoader` characterization also has no runtime
configuration. It uses generated temporary files and the existing project writer
and loader.

## Compatibility rules

Changes to project load or recovery behavior preserve these rules:

1. Corrupt primary project loads fail safely and do not create a partial project.
2. Backup candidates are considered in newest-first order.
3. Known unloadable backup names are skipped during recovery.
4. Backup selection stays inside the trusted backup directory and rejects
   symlink candidates.
5. Readable backups are loaded through the same file loader path as normal
   project files.
6. Exhausted recovery reaches `SHOW_PROJECT_AND_ALL_BACKUPS_LOAD_ERROR` for a
   saved corrupt project and dispatches the new-project path.
7. A saved generated temporary `.a3p` project loaded through the same-package
   protected `FileProjectLoader` seam returns a non-null project.
8. A corrupt generated temporary `.a3p` project loaded through the same protected
   seam returns `null`.
9. Characterization fixtures are generated in temporary files; tests do not
   depend on Git LFS archives, Sims assets, desktop launch, or dialog clicking.

## Validation

Developer validation uses the existing Maven reactor. For a fresh checkout or
worktree, initialize the Tweedle grammar submodule before focused or broad Maven
validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the direct file-loader characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.uricontent.FileProjectLoaderTest \
  test
```

Run the focused recovery characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.ProjectBackupRecoveryIoTest \
  test
```

Run the broader `core/ide` module tests before handing off changes that alter
project load or backup recovery behavior:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  test
```
