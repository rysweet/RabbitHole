# Formal Spec Contracts Reference

This reference defines the formal-spec lane for Alice project archives and
backup recovery. The contract describes durable behavior documented by the
Gherkin and TLA+ artifacts and enforced by the JUnit artifacts listed here.

## Artifact inventory

| Artifact | Path | Role |
| --- | --- | --- |
| Gherkin feature | [`../../eatme/specs/save-load-export/project-archive.feature`](../../eatme/specs/save-load-export/project-archive.feature) | Acceptance contract for save, load, export, resource safety, and backup recovery scenarios. |
| TLA+ module | [`../../eatme/formal/backup-load-recovery/BackupLoadRecovery.tla`](../../eatme/formal/backup-load-recovery/BackupLoadRecovery.tla) | Formal state machine for corrupt primary load and backup recovery. |
| TLA+ config | [`../../eatme/formal/backup-load-recovery/BackupLoadRecovery.cfg`](../../eatme/formal/backup-load-recovery/BackupLoadRecovery.cfg) | Example model constants, invariants, and liveness property intended for TLC. |
| Archive I/O tests | `core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java` | Characterization tests for low-level archive reading, writing, export, resource safety, and reader failure modes. |
| IDE archive-flow tests | `core/ide/src/test/java/org/alice/ide/ProjectFileUtilitiesTest.java` | Characterization tests for IDE save-copy and export-copy archive flows. |
| Backup selector tests | `core/ide/src/test/java/org/alice/ide/ProjectBackupSelectorTest.java` | Characterization tests for backup ordering and unloadable candidate skipping. |
| Backup recovery IO tests | `core/ide/src/test/java/org/alice/ide/ProjectBackupRecoveryIoTest.java` | Characterization tests that use real temporary `.a3p` files for corrupt primary loads, corrupt backup retries, readable backup recovery, and all-backups failure dispatch. |
| Failure plan tests | `core/ide/src/test/java/org/alice/ide/ProjectLoadFailurePlanTest.java` | Characterization tests for choosing the next recovery action. |
| Dispatch plan tests | `core/ide/src/test/java/org/alice/ide/ProjectLoadFailureDispatchPlanTest.java` | Characterization tests for user-choice outcomes. |

## Public archive API surface

The archive contracts are implemented through the existing `IoUtilities` API:

| Method | Contract |
| --- | --- |
| `IoUtilities.readProject(File file)` | Reads editable `.a3p` projects and player `.a3w` archives through the selected project reader. Unsupported versions, missing metadata, malformed JSON manifests, and unsafe resource entries fail explicitly. |
| `IoUtilities.readProject(String path)` | Delegates to `readProject(File)` for path-based callers. |
| `IoUtilities.writeProject(File file, Project project, DataSource... dataSources)` | Writes an editable `.a3p` project archive and creates parent directories when needed. The editor archive contract includes manifest metadata and optional thumbnail metadata. |
| `IoUtilities.writeProject(OutputStream os, Project project, DataSource... dataSources)` | Writes an editable project archive to an output stream with the same archive shape as the file overload. |
| `IoUtilities.exportProject(File file, Project project, DataSource... dataSources)` | Writes a player `.a3w` archive with JSON manifest metadata, Tweedle source entries, and safe resource references. |

`ProjectBackupSelector`, `ProjectLoadFailurePlan`, and
`ProjectLoadFailureDispatchPlan` are package-private IDE implementation
boundaries. They are documented by their tests rather than exposed as public API.
See [Project Backup Recovery IO Characterization](./project-backup-recovery-io.md)
for the recovery IO contract, configuration, and examples.

## Editable project archive contract

Editable `.a3p` archives preserve the project for the Alice editor.

Required behavior:

- Include `version.txt`.
- Include `manifest.json` that names the project.
- Include the XML program type entry for editable projects.
- Include `resources.xml` when project resources exist.
- Store resource bytes under safe relative `resources/` entries.
- Include `thumbnail.png` and set the manifest icon to `thumbnail.png` when
  thumbnail creation succeeds.
- Remain readable when thumbnail creation is unavailable.
- Reopen through `IoUtilities.readProject(File)` with the original program type,
  scene camera type, resource identity, resource name, content type, and bytes.
- Reject traversal resource entries instead of reading outside the archive.

Implemented coverage:

- `IoUtilitiesTest.writtenProjectContainsVersionManifestAndProgramTypeEntries`
  validates that low-level saved editor archives include manifest metadata.
- `ProjectFileUtilitiesTest.saveCopyWritesReadableEditorArchiveWithResourceManifestAndThumbnail`
  validates the IDE save-copy archive shape.
- `IoUtilitiesTest.writeProjectIncludesProvidedThumbnailAndManifestIcon` and
  `IoUtilitiesTest.writeProjectRemainsReadableWithoutThumbnailEntry` validate
  thumbnail success and unavailable-thumbnail behavior.

## Player export archive contract

Player `.a3w` archives preserve the project for player/archive readers.

Required behavior:

- Include `version.txt`.
- Include `manifest.json`.
- Include Tweedle source entries under `src/`.
- Reference resources through safe relative manifest paths.
- Use distinct archive entries when resource names collide.
- Sanitize imported resource names that contain filesystem paths or traversal
  segments.
- Read supported image and audio resources without requiring Tweedle program
  type decoding.
- Fail predictably for missing resource data, unsupported future versions,
  missing `version.txt`, and corrupt JSON manifests.

## Backup recovery model

The TLA+ module models recovery after the primary project cannot be loaded.

The executable recovery IO characterization uses temporary files to exercise the
same policy through Java seams: `FileProjectLoader` returns `null` for corrupt
archives, `ProjectBackupSelector` chooses the next trusted candidate,
`ProjectLoadFailurePlan` chooses the recovery action, and
`ProjectLoadFailureDispatchPlan` reaches the final user-visible load target.

### Constants

| Constant | Meaning |
| --- | --- |
| `Backups` | Set of available backup identifiers. |
| `BackupOrder` | Sequence of backups in newest-first order. Each backup appears exactly once. |
| `ReadableBackups` | Subset of backups that can be loaded. |
| `UnsafeBackups` | Subset of backup candidates that must never be offered or loaded because they escape the backup boundary. |
| `MAIN` | Sentinel for the primary project load attempt. |
| `NONE` | Sentinel for no current candidate or attempt. |

### State variables

| Variable | Meaning |
| --- | --- |
| `pc` | Current recovery state. |
| `attempt` | Project or backup currently being loaded. |
| `unloadable` | Main project or backup names already known to be unloadable. |
| `candidate` | Backup currently offered to the user. |
| `outcome` | Recovery outcome: `Undecided`, `LoadedBackup`, or `NewProject`. |
| `currentProject` | Project state visible to Alice. |

### Recovery states

| State | Meaning |
| --- | --- |
| `LoadingMain` | Alice is attempting to load the primary project. |
| `SelectingBackup` | Alice is selecting the newest remaining backup candidate. |
| `PromptBackup` | Alice is offering a readable backup to the user. |
| `LoadingBackup` | Alice is loading the accepted backup. |
| `Final` | Alice has reached a terminal recovery outcome. |

### Intended TLC properties

| Invariant | Required behavior |
| --- | --- |
| `TypeOK` | All variables stay within the modeled domains. |
| `CorruptPrimaryDoesNotReplaceCurrentBeforeFinal` | A failed primary load does not replace the current project before a terminal decision. |
| `LoadedBackupWasReadable` | A loaded backup must be one of the readable backups. |
| `PromptedBackupsAreReadable` | Alice offers only readable backups to the user. |
| `PromptedBackupsAreSafe` | Alice never offers unsafe backup candidates to the user. |
| `UnloadableBackupsSkipped` | Earlier backups in the newest-first order are skipped only after they are marked unloadable or unsafe. |
| `FinalOutcomeExactlyOne` | Final states have one terminal outcome and no pending attempt or candidate. |
| `FinalProjectMatchesOutcome` | A loaded-backup outcome points to a readable backup; a new-project outcome points to `NewProject`. |
| `NoStaleAsyncCompletion` | Final states do not leave stale load attempts or backup candidates behind. |

The liveness property `EventuallyFinal` is intended to require each modeled
recovery path to reach `Final` when the model is checked with TLC.

## Configuration

The formal-spec lane has no runtime configuration.

| Concern | Configuration |
| --- | --- |
| Gherkin execution | None. The `.feature` file is a committed acceptance contract and is not wired to Cucumber. |
| TLA+ execution | Optional local TLC invocation using `BackupLoadRecovery.cfg`; no Maven or CI plugin is required, and TLC was not available for this PR validation. |
| Java validation | Existing Maven/JUnit module tests. |

## Focused validation commands

Run commands from the repository root.

```shell
mvn -pl core/story-api-migration -am -Dtest=IoUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl core/ide -am -Dtest=ProjectFileUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl core/ide -am -Dtest=ProjectBackupSelectorTest,ProjectBackupRecoveryIoTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl core/ide -am -Dtest=ProjectLoadFailurePlanTest,ProjectLoadFailureDispatchPlanTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The Surefire flag keeps upstream modules without the named test from failing the
focused run.

Run the TLA+ model when `tla2tools.jar` is available. TLC was not run for this
PR validation because no local `tlc`, `tla2tools`, or `tla2tools.jar` was found.

```shell
cd eatme/formal/backup-load-recovery
java -cp /path/to/tla2tools.jar tlc2.TLC BackupLoadRecovery.cfg
```

## Contract-to-test map

| Contract area | Source artifact | Executable boundary |
| --- | --- | --- |
| Editable project archive writes and reads required entries | Gherkin `@save @editor-archive` scenarios | `ProjectFileUtilitiesTest` coverage for IDE save-copy archives and `IoUtilitiesTest` coverage for low-level manifest-bearing editor archives. |
| Optional thumbnail handling | Gherkin `@thumbnail` scenarios | `IoUtilitiesTest` coverage for thumbnail success and unavailable-thumbnail fallback. |
| Player archive export with Tweedle source | Gherkin `@export @player-archive` scenarios | `IoUtilitiesTest` |
| Resource preservation and safe entries | Gherkin `@export @resources` and `@security` scenarios | `IoUtilitiesTest` |
| Missing, future, or corrupt archive metadata | Gherkin `@load @failure` scenarios | `IoUtilitiesTest` |
| Corrupt primary backup recovery | Gherkin `@backup-recovery` scenarios and TLA+ `MainLoadFails` | `ProjectBackupSelectorTest`, `ProjectBackupRecoveryIoTest`, `ProjectLoadFailurePlanTest`, `ProjectLoadFailureDispatchPlanTest` |
| Newest readable backup selection | TLA+ `NextBackup`, `OfferReadableBackup`, and `SkipUnreadableBackup` | `ProjectBackupSelectorTest` |
| Terminal recovery outcome | TLA+ final-state invariants | `ProjectBackupRecoveryIoTest`, `ProjectLoadFailurePlanTest`, and `ProjectLoadFailureDispatchPlanTest` |

## Implemented coverage

| Behavior | Validation |
| --- | --- |
| Saved `.a3p` archives include `manifest.json`. | `IoUtilitiesTest.writtenProjectContainsVersionManifestAndProgramTypeEntries`; `ProjectFileUtilitiesTest.saveCopyWritesReadableEditorArchiveWithResourceManifestAndThumbnail` |
| Saved `.a3p` thumbnail behavior is characterized. | `IoUtilitiesTest.writeProjectIncludesProvidedThumbnailAndManifestIcon` and `IoUtilitiesTest.writeProjectRemainsReadableWithoutThumbnailEntry` |
| Backup recovery rejects traversal or out-of-directory candidates. | `ProjectBackupSelectorTest.corruptedMainProjectSkipsBackupSymlinkEscapingBackupDirectory`; `ProjectBackupSelectorTest.corruptedMainProjectSkipsBackupSymlinkEvenWhenTargetStaysInBackupDirectory`; `ProjectBackupSelectorTest.corruptedMainProjectSkipsCandidatesFromSymlinkedBackupDirectory` |
| Backup recovery uses real temporary archives for readable-backup and all-backups-fail paths. | `ProjectBackupRecoveryIoTest.corruptMainProjectSkipsUnloadableBackupAndLoadsNextBackupWithResources`; `ProjectBackupRecoveryIoTest.corruptMainProjectAndAllBackupsPlanUserVisibleFailure` |
