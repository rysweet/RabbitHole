# Formal Specification Lane

The formal-spec lane is the documentation and characterization layer for Alice
project save, load, export, and backup recovery behavior.

It keeps the user-visible contract readable, the recovery policy precise, and
the executable checks focused on existing Java test infrastructure. It does not
add Cucumber, a Maven TLC plugin, or runtime configuration.

## Contract layers

The lane has three durable layers:

| Layer | Location | Purpose |
| --- | --- | --- |
| Acceptance contract | [`../../eatme/specs/save-load-export/project-archive.feature`](../../eatme/specs/save-load-export/project-archive.feature) | Describes the save, load, export, resource safety, and backup recovery behavior in Gherkin. |
| Formal recovery model | [`../../eatme/formal/backup-load-recovery/BackupLoadRecovery.tla`](../../eatme/formal/backup-load-recovery/BackupLoadRecovery.tla) and [`BackupLoadRecovery.cfg`](../../eatme/formal/backup-load-recovery/BackupLoadRecovery.cfg) | Defines the ordered backup recovery state machine and invariants. |
| Executable characterization | `core/ide` and `core/story-api-migration` JUnit tests | Enforces the contract against the Java implementation. |

The [`../../drinkme/formal-spec-save-load-export-evaluation.md`](../../drinkme/formal-spec-save-load-export-evaluation.md)
handoff records investigation context. It is not runtime input and is not the
product documentation surface.

## Why the lane exists

Alice archive handling combines old editable project archives, newer player
exports, resource serialization, and IDE recovery prompts. Those behaviors are
easy to regress when the implementation is modernized.

The formal-spec lane prevents drift by making each behavior visible in one of
three forms:

1. A human-readable scenario in the Gherkin feature.
2. A recovery rule or invariant in the TLA+ model.
3. A focused JUnit test in the module that owns the behavior.

## Feature contract

The lane defines the behavior the modernization work preserves. Each item is
tied to focused JUnit characterization so future modernization can detect drift.

- Saving an editable project writes a readable `.a3p` archive with `version.txt`,
  `manifest.json`, `programType.xml`, required resource metadata, and safe
  resource entries.
- Saving includes `thumbnail.png` and a matching manifest icon when thumbnail
  creation succeeds; thumbnail creation failure must not make the archive
  unreadable.
- Exporting a project writes a `.a3w` player archive with manifest metadata,
  Tweedle source references, and safe resource entries.
- Archive readers report missing, future, malformed, or unsafe archive metadata
  predictably instead of silently falling through to the wrong reader.
- Resource entries use safe relative paths and do not persist local filesystem
  paths.
- A corrupt primary project does not replace the current project before the user
  reaches a recovery or new-project outcome.
- Backup recovery considers candidates in newest-first order, skips known
  unloadable candidates, never escapes the backup directory, offers the newest
  readable backup, and reaches one terminal result.

## Implemented coverage

The implemented contract is covered at the Java boundary that owns each
behavior:

| Behavior | Implementation/characterization |
| --- | --- |
| Editable `.a3p` archives include `manifest.json`. | XML project writing emits save manifest metadata when callers do not supply it; `IoUtilitiesTest` validates the low-level archive entry and `ProjectFileUtilitiesTest` validates IDE save-copy output. |
| Editable `.a3p` archives include thumbnail metadata when thumbnail creation succeeds. | Thumbnail data sources are preserved and the saved archive remains readable without a thumbnail entry; `IoUtilitiesTest` validates both cases. |
| Backup recovery cannot follow traversal or out-of-directory backup candidates. | `ProjectBackupSelector` skips missing and symlink candidates; `ProjectBackupSelectorTest` validates candidate and backup-directory symlink rejection. |
| Backup recovery handles corrupt project files and corrupt backup files through real IO. | `ProjectBackupRecoveryIoTest` creates temporary corrupt `.a3p` files and generated readable backups to validate readable-backup recovery and all-backups-fail dispatch without Git LFS or Sims assets. |

## What remains outside the lane

The lane is not a new framework, runtime mode, or source generator.

- `.feature` files are not executed by Cucumber.
- TLA+ files are not wired into Maven or CI.
- `drinkme/` files remain investigation and handoff material.
- Java behavior remains compatible with the current Alice 3 baseline unless a
  behavior change is explicitly documented and covered by characterization
  tests.

## Ownership rule

When behavior changes, update the smallest complete set of artifacts:

| Change | Required update |
| --- | --- |
| User-visible archive behavior changes | Update the Gherkin scenario and the matching JUnit test. |
| Backup recovery policy changes | Update the Gherkin scenario, TLA+ model/config, and `core/ide` JUnit tests. |
| Archive entry, manifest, version, or resource safety changes | Update the Gherkin scenario and the matching archive test (`IoUtilitiesTest` for low-level I/O, `ProjectFileUtilitiesTest` for IDE save/export copy flows). |
| Only implementation structure changes | Keep specs stable and update or add characterization tests only if the observable contract is affected. |
