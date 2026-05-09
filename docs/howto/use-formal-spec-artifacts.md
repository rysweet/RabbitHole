# Use the Formal Spec Artifacts

Use the formal-spec artifacts when changing Alice project save, load, export, or
backup recovery behavior.

## Before changing behavior

1. Read the acceptance scenarios in
   [`../../eatme/specs/save-load-export/project-archive.feature`](../../eatme/specs/save-load-export/project-archive.feature).
2. For backup recovery behavior, read the TLA+ model in
   [`../../eatme/formal/backup-load-recovery/BackupLoadRecovery.tla`](../../eatme/formal/backup-load-recovery/BackupLoadRecovery.tla).
3. Find the executable boundary:
    - Low-level archive I/O behavior: `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java`
    - IDE save/export copy behavior: `core/ide/src/test/java/org/alice/ide/ProjectFileUtilitiesTest.java`
    - Backup candidate selection: `core/ide/src/test/java/org/alice/ide/ProjectBackupSelectorTest.java`
    - Backup recovery with real temporary project files: `core/ide/src/test/java/org/alice/ide/ProjectBackupRecoveryIoTest.java`
    - Backup failure decisions: `core/ide/src/test/java/org/alice/ide/ProjectLoadFailurePlanTest.java`
    - User-choice dispatch decisions: `core/ide/src/test/java/org/alice/ide/ProjectLoadFailureDispatchPlanTest.java`
4. Check the implemented coverage map in
   [`../reference/formal-spec-contracts.md`](../reference/formal-spec-contracts.md)
   before claiming a behavior is enforced.

## Update an archive contract

Use this workflow for `.a3p` editable project archives and `.a3w` player export
archives.

1. Update the scenario in `project-archive.feature`.
2. Add or update a focused Java test that proves the same behavior:
   `IoUtilitiesTest` for low-level archive I/O, or `ProjectFileUtilitiesTest`
   for IDE save/export copy flows.
3. Keep the test at the archive boundary. Prefer synthetic projects and
   generated archives over binary fixtures.
4. Preserve reader failure behavior. Malformed JSON manifests, missing
   `version.txt`, unsupported versions, and unsafe resource paths must fail
   explicitly.

For editable `.a3p` archives, do not preserve the legacy no-`manifest.json`
writer expectation as the final behavior. Characterize saved editor archives
with manifest metadata, while making any legacy no-manifest read compatibility
explicit.

### Example: safe resource export

The Gherkin scenario says duplicate resource names and path traversal names are
written as safe, distinct archive entries.

The matching Java validation belongs in `IoUtilitiesTest` and checks the zip
entries directly:

```shell
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am -Dtest=IoUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The Surefire flag keeps upstream modules without the focused test class from
failing the run.

## Update backup recovery behavior

Use this workflow for corrupt primary loads, backup candidate ordering, readable
backup recovery, and new-project fallback decisions.

1. Update the backup recovery scenarios in `project-archive.feature`.
2. Update `BackupLoadRecovery.tla` when the recovery state machine changes.
3. Update `BackupLoadRecovery.cfg` when the model constants or checked
   invariants change.
4. Add or update the focused `core/ide` tests that match the changed rule.
   Use `ProjectBackupRecoveryIoTest` when the rule depends on real temporary
   `.a3p` inputs, corrupt archive contents, backup load failure, readable backup
   readback, or all-backups-fail dispatch.

Backup candidate selection must not follow traversal or out-of-directory paths.
Keep that rule covered with a focused selector safety test when recovery logic
changes.

### Example: skip unloadable backups

The recovery model describes backup candidates as a newest-first sequence. A
candidate that cannot be loaded is added to `unloadable`, and the next candidate
is selected from the remaining backups.

The matching Java validation belongs in `ProjectBackupSelectorTest` for pure
candidate selection and `ProjectBackupRecoveryIoTest` when the path should load
or fail real temporary project archives:

```shell
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip -pl core/ide -am \
  -Dtest=ProjectBackupSelectorTest,ProjectBackupRecoveryIoTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

The Surefire flag is required when running with `-am` and a specific `-Dtest`
value.

## Check the TLA+ model locally

The repository stores the TLA+ module and config but does not require a Maven
TLC integration. When TLC is installed locally, run it from the model directory:

```shell
cd eatme/formal/backup-load-recovery
java -cp /path/to/tla2tools.jar tlc2.TLC BackupLoadRecovery.cfg
```

The invariants intended for TLC checking are listed in the config:

- `TypeOK`
- `CorruptPrimaryDoesNotReplaceCurrentBeforeFinal`
- `LoadedBackupWasReadable`
- `PromptedBackupsAreReadable`
- `PromptedBackupsAreSafe`
- `UnloadableBackupsSkipped`
- `FinalOutcomeExactlyOne`
- `FinalProjectMatchesOutcome`
- `NoStaleAsyncCompletion`

The intended liveness property is `EventuallyFinal`.

## Keep artifacts in the correct place

Use these locations:

| Artifact | Correct location |
| --- | --- |
| Acceptance scenarios | `eatme/specs/save-load-export/project-archive.feature` |
| TLA+ model and config | `eatme/formal/backup-load-recovery/` |
| Investigation handoff | `drinkme/formal-spec-save-load-export-evaluation.md` |
| Product documentation | `docs/` |
| Executable Java checks | `core/ide/src/test/java` and `core/story-api-migration/src/test/java` |

Do not move required deliverables into `drinkme/`. Keep `drinkme/` for
investigation artifacts only.
