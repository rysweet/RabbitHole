# Trace Save, Load, Export, and Recovery Behavior

This tutorial shows how to trace one behavior through the formal-spec lane: from
acceptance text, to formal recovery policy, to executable JUnit validation.

## Start with the acceptance contract

Open the Gherkin feature:

```shell
sed -n '112,144p' eatme/specs/save-load-export/project-archive.feature
```

The backup recovery scenarios describe this user-visible flow:

1. Alice attempts to load `world.a3p`.
2. The primary project is corrupt.
3. Alice checks the newest backups first.
4. Alice skips backups that cannot be loaded.
5. Alice offers the newest readable backup.
6. The user either accepts the backup or reaches a new-project outcome.

The scenarios avoid dialog implementation details. They define the observable
contract that users and tests rely on.

## Read the formal recovery model

Open the TLA+ model:

```shell
sed -n '51,122p' eatme/formal/backup-load-recovery/BackupLoadRecovery.tla
```

The model names the same recovery steps:

| TLA+ action | User-visible meaning |
| --- | --- |
| `MainLoadFails` | The primary project failed to load and is marked unloadable. |
| `OfferReadableBackup` | The newest remaining readable backup is offered to the user. |
| `SkipUnreadableBackup` | An unreadable backup is marked unloadable and skipped. |
| `NoBackupRemaining` | Alice reaches the new-project outcome because no backup remains. |
| `AcceptBackup` | The user accepts the offered backup. |
| `DeclineBackup` | The user declines backup recovery and starts the new-project path. |
| `BackupLoadSucceeds` | The accepted backup becomes the loaded project. |

The `NextBackup` definition chooses the remaining backup with the smallest
newest-first order index. That is the formal rule behind newest-readable backup
selection.

## Match the model to Java tests

Open the backup selector and recovery IO tests:

```shell
sed -n '23,172p' core/ide/src/test/java/org/alice/ide/ProjectBackupSelectorTest.java
sed -n '28,153p' core/ide/src/test/java/org/alice/ide/ProjectBackupRecoveryIoTest.java
```

These tests characterize the same rules:

| Test behavior | Formal rule |
| --- | --- |
| Corrupt primary uses latest available backup | `MainLoadFails` followed by `OfferReadableBackup` |
| Known unloadable backups are skipped | `SkipUnreadableBackup` |
| Missing candidates are ignored by recovery selection | Unloadable candidates do not become final loaded projects |
| No remaining candidates returns `null` | `NoBackupRemaining` |
| Corrupt primary plus corrupt newest backup loads the next readable temporary `.a3p` backup | `SkipUnreadableBackup`, `OfferReadableBackup`, and `BackupLoadSucceeds` |
| Corrupt primary plus all corrupt backups dispatches the new-project failure path | `NoBackupRemaining` and final-state invariants |

Run the focused validation:

```shell
mvn -DincludeSims=false -Dinstall4j.skip -pl core/ide -am \
  -Dtest=ProjectBackupSelectorTest,ProjectBackupRecoveryIoTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## Trace archive behavior

The same pattern applies to save and export behavior.

Open the archive scenarios:

```shell
sed -n '11,80p' eatme/specs/save-load-export/project-archive.feature
```

Then open the archive tests:

```shell
sed -n '57,132p' core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java
sed -n '281,327p' core/ide/src/test/java/org/alice/ide/ProjectFileUtilitiesTest.java
```

The acceptance scenarios describe the observable archive contract:

- Saved `.a3p` archives reopen as editable projects.
- A project saved as `.a3p` is covered by a regression that reopens the project,
  edits it, saves again, reopens again with the edited program metadata still
  present, and exports the edited project.
- Saved `.a3p` archives include `version.txt`, `manifest.json`,
  `programType.xml`, optional `resources.xml`, safe resource entries, and
  thumbnail metadata when a thumbnail is available.
- `IoUtilitiesTest` coverage confirms low-level manifest metadata, thumbnail
  behavior, XML entries, safe resource entries, saving, reopening, editing,
  saving again, reopening again, and exporting the edited project.
- `ProjectFileUtilitiesTest` coverage confirms the IDE save-copy flow writes the
  same user-visible editor archive shape.
- Exported `.a3w` archives contain manifest metadata and Tweedle source.
- Resources are preserved by identity and content.
- Unsafe resource paths are rejected.

Run the focused validation:

```shell
mvn -pl core/story-api-migration -am -Dtest=IoUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl core/ide -am -Dtest=ProjectFileUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Check the complete recovery model

TLC was not run for this PR validation because no local `tlc`, `tla2tools`, or
`tla2tools.jar` was found. When TLC is available, check the model with the
committed config:

```shell
cd eatme/formal/backup-load-recovery
java -cp /path/to/tla2tools.jar tlc2.TLC BackupLoadRecovery.cfg
```

The config is intended to check type safety, readable-backup selection,
skipped-unloadable ordering, final-state consistency, and eventual completion.

## Use the trace when implementing changes

For each behavior change, keep the trace complete:

| If you change | Then update |
| --- | --- |
| User-visible save, load, or export behavior | Gherkin scenario and the matching archive test (`IoUtilitiesTest` or `ProjectFileUtilitiesTest`) |
| Project archive state after saving, reopening, editing, saving again, reopening again, and exporting | Add/update `IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported` |
| Backup ordering or recovery state | Gherkin scenario, TLA+ model/config, and `core/ide` tests |
| Reader error handling | Gherkin failure scenario and `IoUtilitiesTest` |
| Internal structure only | Java tests as needed; leave the formal artifacts unchanged if the contract is unchanged |

The lane is complete when a reader can move from scenario to model to focused
test without guessing which behavior is authoritative.
