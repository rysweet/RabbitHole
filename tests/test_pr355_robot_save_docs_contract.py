import json
import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
HOWTO_PATH = REPO_ROOT / "docs" / "howto" / "run-robot-save-menu-dialog-write-readback-proof.md"
REFERENCE_PATH = REPO_ROOT / "docs" / "reference" / "robot-save-menu-dialog-write-readback-proof.md"

SCHEMA_VERSION = "eatme.alice-desktop-robot-save-menu-dialog-write-readback-proof/v1"
PROOF_TARGET = "Robot File menu Save activation joined to dialog/write/readback evidence"
EXPECTED_FILE = "projects/robot-save-menu-proof.a3p"
EXPECTED_MARKER = "robotSaveMenuRoundTripMarker"
EXPECTED_BASELINES = [
    "StageIdeSaveMenuDoClickToWriteProofTest",
    "ProjectApplicationSaveProjectToTest",
    "JMenuBarRobotClickSaveProofTest",
]
EXPECTED_NON_CLAIMS = [
    "full desktop Save completion",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage",
    "all Save variants",
    "Save As coverage",
]
BLOCKED_TOP_LEVEL_FIELDS = [
    "schema_version",
    "status",
    "proofTarget",
    "reporting_summary",
    "blocker",
    "trigger",
    "observed_dialog",
    "selected_file",
    "written_artifact",
    "readback",
    "baselinePreserved",
    "requiresNextEvidence",
    "doesNotClaim",
]
FULL_ARTIFACT_SECTIONS = [
    "trigger",
    "observed_dialog",
    "selected_file",
    "written_artifact",
    "readback",
    "baselinePreserved",
    "requiresNextEvidence",
    "doesNotClaim",
]


def json_blocks(path: Path) -> list[dict]:
    text = path.read_text(encoding="utf-8")
    blocks = re.findall(r"```json\n(.*?)\n```", text, flags=re.DOTALL)
    return [json.loads(block) for block in blocks]


def artifact_with_status(path: Path, status: str) -> dict:
    matches = [block for block in json_blocks(path) if block.get("status") == status]
    if not matches:
        raise AssertionError(f"{path.relative_to(REPO_ROOT)} must include a {status!r} JSON artifact example.")
    if len(matches) > 1:
        raise AssertionError(f"{path.relative_to(REPO_ROOT)} must include exactly one {status!r} JSON artifact example.")
    return matches[0]


class Pr355RobotSaveDocsContractTest(unittest.TestCase):
    def test_howto_headless_awt_blocked_example_uses_complete_artifact_shape(self) -> None:
        blocked = artifact_with_status(HOWTO_PATH, "blocked")

        self.assertEqual(BLOCKED_TOP_LEVEL_FIELDS, list(blocked.keys()))
        self.assertEqual(SCHEMA_VERSION, blocked["schema_version"])
        self.assertEqual(PROOF_TARGET, blocked["proofTarget"])
        self.assertEqual("headless_awt", blocked["blocker"]["kind"])
        self.assertNotIn("claim", blocked)
        self.assertIn("not proven", blocked["reporting_summary"].lower())
        self.assertNotIn("completed", blocked["reporting_summary"].lower())

    def test_howto_headless_awt_blocked_example_keeps_success_evidence_false(self) -> None:
        blocked = artifact_with_status(HOWTO_PATH, "blocked")

        self.assertEqual(
            {
                "robot_file_menu_opened": False,
                "robot_save_item_clicked": False,
                "save_action_identity_matched": False,
            },
            blocked["trigger"],
        )
        self.assertEqual(
            {
                "dialogType": "Swing JFileChooser",
                "dialog_class": None,
                "dialog_showing": False,
                "chooser_observed": False,
                "approved_selection": False,
                "ambiguous_chooser_discovery": False,
                "poll_count": 0,
            },
            blocked["observed_dialog"],
        )
        self.assertEqual(
            {
                "normalized_selected_file": None,
                "expected_file": EXPECTED_FILE,
                "selected_file_verified": False,
                "selected_file_matches_expected": False,
                "target_inside_proof_root": False,
            },
            blocked["selected_file"],
        )
        self.assertEqual(
            {
                "target_file": EXPECTED_FILE,
                "file_written": False,
                "file_nonempty": False,
                "file_extension": "a3p",
                "file_has_expected_extension": True,
                "file_size_bytes": 0,
            },
            blocked["written_artifact"],
        )
        self.assertEqual(
            {
                "project_readable": False,
                "expected_marker": EXPECTED_MARKER,
                "marker_present": False,
            },
            blocked["readback"],
        )

    def test_howto_headless_awt_blocked_example_preserves_baselines_and_non_claims(self) -> None:
        blocked = artifact_with_status(HOWTO_PATH, "blocked")

        self.assertEqual(EXPECTED_BASELINES, blocked["baselinePreserved"])
        self.assertEqual(EXPECTED_NON_CLAIMS, blocked["doesNotClaim"])
        self.assertTrue(
            any("xvfb-run -a" in item for item in blocked["requiresNextEvidence"]),
            "Blocked headless_awt evidence should tell reviewers how to collect desktop evidence next.",
        )
        self.assertTrue(
            any("Use status proven only" in item for item in blocked["requiresNextEvidence"]),
            "Blocked evidence should prevent Maven success from being treated as Save completion proof.",
        )

    def test_howto_and_reference_examples_share_the_full_artifact_sections(self) -> None:
        howto_blocked = artifact_with_status(HOWTO_PATH, "blocked")
        reference_blocked = artifact_with_status(REFERENCE_PATH, "blocked")
        reference_proven = artifact_with_status(REFERENCE_PATH, "proven")

        for field in FULL_ARTIFACT_SECTIONS:
            with self.subTest(field=field):
                self.assertIn(field, howto_blocked)
                self.assertIn(field, reference_blocked)
                self.assertIn(field, reference_proven)

        self.assertEqual(howto_blocked, reference_blocked)
        self.assertEqual(EXPECTED_NON_CLAIMS, reference_proven["doesNotClaim"])

    def test_howto_text_limits_the_scope_to_blocker_evidence_until_status_is_proven(self) -> None:
        text = HOWTO_PATH.read_text(encoding="utf-8")

        self.assertIn("Use the JSON artifact as the source of truth.", text)
        self.assertIn("Do not treat Maven success by itself as proof.", text)
        self.assertIn("Use that artifact only as headless-display blocker evidence.", text)
        self.assertIn("It does not prove Robot Save activation", text)
        self.assertIn("full desktop Save completion", text)


if __name__ == "__main__":
    unittest.main()
