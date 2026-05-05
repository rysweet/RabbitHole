# Project Backup Recovery IO Characterization

This reference describes the `core/ide` project load and backup recovery
characterization that uses real temporary Alice project files.

## Contents

- [Feature scope](#feature-scope)
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

It protects the boundary between corrupt project loads, backup selection,
recovery planning, user-visible failure dispatch, and production archive IO.
The test creates `.a3p` files under JUnit `TemporaryFolder`; it does not use
checked-in Alice archives, Git LFS payloads, Sims assets, broad UI automation,
or desktop launch fixtures.

The test covers two recovery journeys:

| Journey | Protected behavior |
| --- | --- |
| Corrupt main project, corrupt newest backup, readable older backup | The corrupt main project loads as `null`, the unreadable newest backup is skipped, the next backup is offered, and the readable backup reopens with its program type and resources intact. |
| Corrupt main project and all backups corrupt | Alice attempts recovery in newest-first order, marks each failed backup unloadable, exhausts candidates, and plans the user-visible new-project failure dispatch. |

## User-visible recovery behavior

When Alice cannot load the primary `.a3p` project, recovery follows this
observable contract:

1. The failed primary load does not replace the current project with a partially
   decoded project.
2. Alice checks backup candidates inside the trusted backup directory in
   newest-first order.
3. Missing, unsafe, symlinked, out-of-directory, or already unloadable backup
   candidates are not offered.
4. A readable backup is offered for recovery.
5. If an offered backup also fails during recovery, Alice records that backup as
   unloadable and offers the next candidate.
6. If no backup remains, Alice dispatches the same user-visible failure path as
   the current application flow: no project is loaded and the new-project path is
   shown.

The characterization deliberately stays above broad UI automation. It verifies
the dispatch plan that drives user-visible behavior instead of clicking dialogs.

## API and seam reference

The recovery classes are `core/ide` implementation seams, not public extension
APIs. Tests live in the same package so they can characterize package-private
behavior without widening production visibility.

| Seam | Contract |
| --- | --- |
| `FileProjectLoader` | Attempts to load a project file. Load failures are represented as a `null` project so `ProjectApplication` can run backup recovery UI. |
| `ProjectBackupSelector.getNextBackup(...)` | Selects the next trusted backup candidate. For corrupt main projects, it returns the newest available candidate without comparing backup creation time. For recent-backup probes, it only returns a backup newer than the project. |
| `ProjectLoadFailurePlan.choose(...)` | Chooses the next recovery action after a project or backup load failure. Actions include prompting for a backup, showing backup load failure, showing all-backups failure, and prompting for the main project. |
| `ProjectLoadFailureDispatchPlan.afterUserChoice(...)` | Converts a recovery action and user acceptance into a load target (`BACKUP`, `MAIN_PROJECT`, or `NONE`) and whether Alice should show a new project. |

These seams preserve the existing load/recovery behavior while making the
decision points directly testable.

## Characterization examples

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

Developer validation uses the existing Maven reactor. For a fresh checkout or
worktree, initialize the Tweedle grammar submodule before broad Maven
validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

For no-Sims validation, keep Sims assets and installer packaging disabled:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  test
```

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
7. Characterization fixtures are generated in temporary files; tests do not
   depend on Git LFS archives, Sims assets, desktop launch, or dialog clicking.

## Validation

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
