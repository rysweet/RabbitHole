import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ARCHIVE_FEATURE = Path("eatme/specs/save-load-export/project-archive.feature")
BACKUP_LOAD_RECOVERY_TLA = Path("eatme/formal/backup-load-recovery/BackupLoadRecovery.tla")
BACKUP_LOAD_RECOVERY_CFG = Path("eatme/formal/backup-load-recovery/BackupLoadRecovery.cfg")
IO_UTILITIES_TEST = Path(
    "core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java"
)
BACKUP_SELECTOR_TEST = Path(
    "core/ide/src/test/java/org/alice/ide/ProjectBackupSelectorTest.java"
)
BACKUP_RECOVERY_IO_TEST = Path(
    "core/ide/src/test/java/org/alice/ide/ProjectBackupRecoveryIoTest.java"
)
PROJECT_FILE_UTILITIES_TEST = Path(
    "core/ide/src/test/java/org/alice/ide/ProjectFileUtilitiesTest.java"
)


@lru_cache(maxsize=None)
def read_repo_text(relative_path: Path) -> str:
    return (REPO_ROOT / relative_path).read_text(encoding="utf-8")


class PR426FormalContractWiringTest(unittest.TestCase):
    def test_pr426_formal_and_spec_artifacts_exist(self) -> None:
        for relative_path in (
            PROJECT_ARCHIVE_FEATURE,
            BACKUP_LOAD_RECOVERY_TLA,
            BACKUP_LOAD_RECOVERY_CFG,
        ):
            self.assertTrue(
                (REPO_ROOT / relative_path).is_file(),
                f"Missing PR #426 contract artifact: {relative_path.as_posix()}",
            )

    def test_pr426_backup_recovery_tla_invariants_remain_named(self) -> None:
        tla_text = read_repo_text(BACKUP_LOAD_RECOVERY_TLA)
        cfg_text = read_repo_text(BACKUP_LOAD_RECOVERY_CFG)
        for marker in (
            "Spec ==",
            "UnsafeBackups",
            "PromptedBackupsAreSafe ==",
            "UnloadableBackupsSkipped ==",
            "NoStaleAsyncCompletion ==",
            "EventuallyFinal ==",
        ):
            self.assertIn(marker, tla_text)
        for invariant in (
            "PromptedBackupsAreSafe",
            "UnloadableBackupsSkipped",
            "NoStaleAsyncCompletion",
        ):
            self.assertIn(invariant, cfg_text)

    def test_pr426_project_archive_feature_maps_to_executable_junit_anchors(self) -> None:
        feature_text = read_repo_text(PROJECT_ARCHIVE_FEATURE)
        io_test_text = read_repo_text(IO_UTILITIES_TEST)
        for marker in (
            "@export @resources @safety",
            "@export @resources @security",
            "@load @failure @security",
            "Scenario: A future-version player archive reports its unsupported version",
            "Scenario: A player archive missing version metadata fails predictably",
            "Scenario: A corrupt JSON manifest does not fall back to the editor XML reader",
        ):
            self.assertIn(marker, feature_text)
        for anchor in (
            "pr426ProjectArchiveContractRejectsMalformedPlayerArchiveMetadataBeforeXmlFallback",
            "pr426ProjectArchiveContractRejectsUnsafeSupplementalEntryNames",
            "jsonPlayerExportUsesSafeDistinctResourceEntries",
            "jsonPlayerExportDoesNotLeakAbsoluteResourcePaths",
            "jsonPlayerReaderRejectsTraversalResourceReference",
            "jsonPlayerReaderReportsFutureVersion",
            "jsonPlayerReaderReportsMissingVersion",
            "corruptManifestDoesNotFallBackToXmlReader",
        ):
            self.assertIn(anchor, io_test_text)

    def test_pr426_backup_recovery_model_maps_to_executable_junit_anchors(self) -> None:
        feature_text = read_repo_text(PROJECT_ARCHIVE_FEATURE)
        selector_text = read_repo_text(BACKUP_SELECTOR_TEST)
        recovery_text = read_repo_text(BACKUP_RECOVERY_IO_TEST)
        file_utilities_text = read_repo_text(PROJECT_FILE_UTILITIES_TEST)
        for marker in (
            "@load @backup-recovery",
            "Alice marks backup \"auto20240102_140000.a3p\" unloadable",
            "Alice reports that the project and all backups could not be loaded",
            "Alice does not offer the escaping backup candidate",
        ):
            self.assertIn(marker, feature_text)
        for anchor in (
            "pr426BackupContractSelectsNewestSafeCandidateAndNeverReselectsFailedOrUnsafe",
            "corruptedMainProjectSkipsBackupSymlinkEscapingBackupDirectory",
            "corruptedMainProjectSkipsCandidatesFromSymlinkedBackupDirectory",
        ):
            self.assertIn(anchor, selector_text)
        for anchor in (
            "pr426BackupContractStopsAfterSuccessfulRecovery",
            "corruptMainProjectSkipsUnloadableBackupAndLoadsNextBackupWithResources",
            "corruptMainProjectAndAllBackupsPlanUserVisibleFailure",
        ):
            self.assertIn(anchor, recovery_text)
        self.assertIn(
            "pr426BackupPathContractKeepsNamedBackupDirectoryBesideProjectFile",
            file_utilities_text,
        )


if __name__ == "__main__":
    unittest.main()
