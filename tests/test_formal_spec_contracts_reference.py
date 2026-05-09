import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
REFERENCE_DOC = Path("docs/reference/formal-spec-contracts.md")
PROJECT_ARCHIVE_FEATURE = Path("eatme/specs/save-load-export/project-archive.feature")
BACKUP_LOAD_RECOVERY_TLA = Path("eatme/formal/backup-load-recovery/BackupLoadRecovery.tla")
BACKUP_SELECTOR_TEST = Path(
    "core/ide/src/test/java/org/alice/ide/ProjectBackupSelectorTest.java"
)
FORMAL_SPEC_BACKUP_SAFETY_CONTRACT = "Formal/spec backup safety reference mapping"


EXPECTED_MARKERS = (
    (
        REFERENCE_DOC,
        (
            FORMAL_SPEC_BACKUP_SAFETY_CONTRACT,
            "Backup recovery model",
            PROJECT_ARCHIVE_FEATURE.as_posix(),
            BACKUP_LOAD_RECOVERY_TLA.as_posix(),
            "tests/test_formal_spec_contracts_reference.py",
            "@backup-recovery",
            "@security",
            "UnsafeBackups",
            "PromptedBackupsAreSafe",
            "Alice never offers unsafe backup candidates",
            "backup recovery/security Gherkin scenario",
            BACKUP_SELECTOR_TEST.as_posix(),
            "ProjectBackupSelectorTest.corruptedMainProjectSkipsBackupSymlinkEscapingBackupDirectory",
            "ProjectBackupSelectorTest.corruptedMainProjectSkipsBackupSymlinkEvenWhenTargetStaysInBackupDirectory",
            "ProjectBackupSelectorTest.corruptedMainProjectSkipsCandidatesFromSymlinkedBackupDirectory",
        ),
    ),
    (
        PROJECT_ARCHIVE_FEATURE,
        (
            "@load @backup-recovery @security",
            "Scenario: Backup recovery ignores a candidate that escapes the backup directory",
            "Alice does not offer the escaping backup candidate",
            "Alice offers backup \"auto20240102_130000.a3p\" for recovery",
        ),
    ),
    (
        BACKUP_LOAD_RECOVERY_TLA,
        (
            "UnsafeBackups",
            "PromptedBackupsAreSafe ==",
            "pc = \"PromptBackup\" => candidate \\notin UnsafeBackups",
        ),
    ),
    (
        BACKUP_SELECTOR_TEST,
        (
            "corruptedMainProjectSkipsBackupSymlinkEscapingBackupDirectory",
            "escapingBackup",
            "outsideBackup",
            "assertEquals(safeBackup, backup)",
            "corruptedMainProjectSkipsBackupSymlinkEvenWhenTargetStaysInBackupDirectory",
            "corruptedMainProjectSkipsCandidatesFromSymlinkedBackupDirectory",
            "Files.createSymbolicLink",
            "assertNull(backup)",
        ),
    ),
)


class FormalSpecContractsReferenceTest(unittest.TestCase):
    def test_formal_spec_backup_safety_contract_maps_to_artifacts(self) -> None:
        self.assertTrue(
            (REPO_ROOT / BACKUP_SELECTOR_TEST).is_file(),
            f"Documented Java boundary is missing: {BACKUP_SELECTOR_TEST.as_posix()}",
        )

        for relative_path, markers in EXPECTED_MARKERS:
            path_label = relative_path.as_posix()
            text = (REPO_ROOT / relative_path).read_text(encoding="utf-8")
            missing_markers = [marker for marker in markers if marker not in text]
            self.assertEqual(
                [],
                missing_markers,
                f"{path_label} is missing documented contract markers.",
            )

    def test_reference_doc_avoids_point_in_time_validation_claims(self) -> None:
        text = (REPO_ROOT / REFERENCE_DOC).read_text(encoding="utf-8").lower()

        for phrase in (
            "this pr validation",
            "was not run",
            "current head",
            "green ci",
            "merge-ready",
        ):
            self.assertNotIn(phrase, text)
