# Project Load and Backup Recovery Characterization

This reference describes the `core/ide` project load and backup recovery
characterization that uses real temporary Alice project files.

## Contents

- [Feature scope](#feature-scope)
- [Direct saved-project load characterization](#direct-saved-project-load-characterization)
- [File-loader QA smoke scenario](#file-loader-qa-smoke-scenario)
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

`ProjectBackupRecoveryIoTest` includes these recovery and post-recovery
characterization cases:

| Journey | Protected behavior |
| --- | --- |
| Corrupt main project, corrupt newest backup, readable older backup | The corrupt main project loads as `null`, the unreadable newest backup is skipped, the next backup is offered, and the readable backup reopens with its program type and resources intact. |
| Corrupt main project and all backups corrupt | Alice attempts recovery in newest-first order, marks each failed backup unloadable, exhausts candidates, and plans the user-visible new-project failure dispatch. |
| Corrupt default backup with no other backups | A failed unsaved-project default backup reaches the unsaved-backups failure plan without trying to compare backup times. |
| Accepted recovered backup saved and exported | A readable recovered backup can be saved as an `.a3p` and exported as an `.a3w` while preserving the recovered program type and resource data. |

The examples below highlight the first two PR-relevant recovery journeys because
they exercise the backup-selection and all-backups-failed paths most directly.

## Direct saved-project load characterization

Use `FileProjectLoaderTest` when a change touches the IDE file-loader boundary
for a normal `.a3p` file. The characterization is deliberately narrower than
backup recovery:

| Temporary input | Loader behavior |
| --- | --- |
| Generated `.a3p` written by `IoUtilities.writeProject(...)` | The same-package test calls the protected `FileProjectLoader.load()` seam and receives a non-null `Project` whose generated program type name is still readable. |
| Corrupt `.a3p` containing deterministic invalid bytes | The same protected loader seam returns `null`, matching the current null-on-load-failure behavior used by recovery code. |

These tests create both files with JUnit `TemporaryFolder`. They do not read
sample projects, user files, external paths, LFS assets, or committed binary
fixtures.

The saved-project assertion should name only the generated temporary project load
behavior. The corrupt-project assertion should name only current rejection
behavior; it is not broad archive-recovery coverage. Keep these as focused
characterization tests rather than combining them with unrelated loader behavior.

## File-loader QA smoke scenario

The outside-in QA catalog includes one command-gated smoke scenario for the
direct file-loader boundary:

```text
qa/outside-in/alice-desktop/scenarios/file-loader-smoke.yaml
```

The scenario is:

| Field | Value |
| --- | --- |
| ID | `alice-desktop-file-loader-smoke` |
| Workflow | `file-loader-smoke` |
| Automation mode | `gated-command-smoke` |
| Focused command | `mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/ide -am -Dtest=org.alice.ide.uricontent.FileProjectLoaderTest test` |

Use this scenario when review needs QA evidence for the same saved-project and
corrupt-project loader seam protected by `FileProjectLoaderTest`. It is not a
desktop UI workflow, a broad project archive corpus run, or backup recovery
automation. For an executed gated run, the evidence should stay limited to:

1. The gated command outcome.
2. The command log.
3. Test output or a Surefire report naming
   `FileProjectLoaderTest.savedTemporaryProjectLoadsAndCorruptTemporaryProjectIsRejected`.
4. Review notes that identify the generated saved-project load assertion and the
   corrupt-project rejection assertion.

Validate that the scenario is present and normalized:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh \
  --dump-json alice-desktop-file-loader-smoke
```

Prepare checklist and status metadata without running the gated Maven command:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-file-loader-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/file-loader-smoke
```

`--prepare-only` writes the scenario environment, checklist, and `status.txt`
metadata with `outcome=gated-not-run` and `skipMode=prepare-only`. It
intentionally does not create `command.log`; `command.log` is produced only when
the gated command actually runs with `ALICE_QA_RUN_GATED_SMOKES=1`.

Run the gated smoke only when focused QA evidence is explicitly needed:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  ALICE_QA_RUN_GATED_SMOKES=1 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-file-loader-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/file-loader-smoke
```

The runner allowlist accepts only the exact focused command above for this
scenario. Do not add general shell commands, nested scenario paths, generated
fixtures, local absolute paths, or broader Maven test selections to the smoke
scenario.

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

### Load a saved temporary generated project

Mirror `FileProjectLoaderTest.savedTemporaryProjectLoadsAndCorruptTemporaryProjectIsRejected`
by creating a generated project archive with the existing project writer:

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
```

### Reject a corrupt temporary project

Create a second real temporary `.a3p` file with deterministic invalid bytes and
assert the current null-on-load-failure behavior through `FileProjectLoader`:

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
avoid adding fixtures, dependencies, or production rewrites. The stable saved
project assertions stop at the non-null loaded project and generated program
type name. The saved-project and corrupt-project checks should remain in one
focused characterization method so the test does not imply broader recovery
coverage.

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

The file-loader QA smoke scenario uses the existing Alice desktop QA runner
configuration:

| Setting | Default or required value | Purpose |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Gives Maven and QA validation enough heap for this checkout. |
| `ALICE_QA_SCENARIO_DIR` | `qa/outside-in/alice-desktop/scenarios` | Optional catalog override; leave unset for normal repository validation. |
| `ALICE_QA_RUN_GATED_SMOKES` | unset | Leave unset for `--prepare-only`; set to `1` only to execute the focused file-loader Maven smoke. |
| Scenario `automation.cwd` | `.` | Runs from the repository root. |
| Scenario `automation.timeoutSeconds` | `600` | Bounds the focused Maven smoke. |

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
9. The file-loader QA smoke scenario validates and runs only the focused
   `FileProjectLoaderTest` command through the Alice desktop QA runner allowlist.
10. Characterization fixtures are generated in temporary files; tests do not
    depend on Git LFS archives, Sims assets, desktop launch, or dialog clicking.

## Validation

Developer validation uses the existing Maven reactor. For a fresh checkout or
worktree, initialize the Tweedle grammar submodule before focused or broad Maven
validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the direct file-loader characterization from the repository root. This is
the same focused Maven command allowed by the QA smoke scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.uricontent.FileProjectLoaderTest \
  test
```

Validate the Alice desktop QA catalog and the file-loader smoke scenario from
the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh \
  --dump-json alice-desktop-file-loader-smoke
```

Check the runner path without executing the gated Maven smoke:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-file-loader-smoke \
  --prepare-only
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
