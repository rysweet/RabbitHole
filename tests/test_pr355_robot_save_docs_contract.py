import json
import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
HOWTO_PATH = REPO_ROOT / "docs" / "howto" / "run-robot-save-menu-dialog-write-readback-proof.md"
REFERENCE_PATH = REPO_ROOT / "docs" / "reference" / "robot-save-menu-dialog-write-readback-proof.md"

SCHEMA_VERSION = "eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1"
SCENARIO = "alice-desktop-save-menu-dialog-write-proof"
WORKFLOW = "save-menu-dialog-write-proof"
PROOF_TARGET = "single rendered desktop Save path: menu, dialog, control, write, readback"
EXPECTED_CLAIM = (
    "AWT Robot opened File, clicked the production Save menu item, controlled the rendered "
    "Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified "
    "robotSaveMenuRoundTripMarker"
)
EXPECTED_FILE = "projects/robot-save-menu-proof.a3p"
EXPECTED_MARKER = "robotSaveMenuRoundTripMarker"
EXPECTED_BASELINES = [
    "StageIdeSaveMenuDoClickToWriteProofTest",
    "ProjectApplicationSaveProjectToTest",
    "JMenuBarRobotClickSaveProofTest",
]
EXPECTED_NON_CLAIMS = [
    "Save As coverage",
    "all Save variants",
    "full lesson completion",
    "visible rendering correctness",
    "grading correctness",
    "physical user click",
    "broad UI automation coverage",
    "native dialog coverage",
]
BLOCKED_TOP_LEVEL_FIELDS = [
    "schemaVersion",
    "scenario",
    "workflow",
    "runId",
    "generatedAtUtc",
    "status",
    "proofTarget",
    "reportingSummary",
    "blocker",
    "menu",
    "dialog",
    "control",
    "write",
    "readback",
    "baselinePreserved",
    "requiresNextEvidence",
    "doesNotClaim",
]
PROVEN_TOP_LEVEL_FIELDS = [
    "schemaVersion",
    "scenario",
    "workflow",
    "runId",
    "generatedAtUtc",
    "status",
    "proofTarget",
    "claim",
    "blocker",
    "menu",
    "dialog",
    "control",
    "write",
    "readback",
    "baselinePreserved",
    "requiresNextEvidence",
    "doesNotClaim",
]
FULL_ARTIFACT_SECTIONS = [
    "menu",
    "dialog",
    "control",
    "write",
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
    def test_howto_headless_awt_blocked_example_uses_canonical_artifact_shape(self) -> None:
        blocked = artifact_with_status(HOWTO_PATH, "blocked")

        self.assertEqual(BLOCKED_TOP_LEVEL_FIELDS, list(blocked.keys()))
        self.assertEqual(SCHEMA_VERSION, blocked["schemaVersion"])
        self.assertEqual(SCENARIO, blocked["scenario"])
        self.assertEqual(WORKFLOW, blocked["workflow"])
        self.assertEqual(PROOF_TARGET, blocked["proofTarget"])
        self.assertEqual("headless_awt", blocked["blocker"]["kind"])
        self.assertIn("observed", blocked["blocker"])
        self.assertIn("required", blocked["blocker"])
        self.assertNotIn("claim", blocked)
        self.assertIn("not proven", blocked["reportingSummary"].lower())
        self.assertNotIn("completed", blocked["reportingSummary"].lower())

    def test_howto_headless_awt_blocked_example_keeps_success_evidence_false(self) -> None:
        blocked = artifact_with_status(HOWTO_PATH, "blocked")

        self.assertEqual(
            {
                "fileMenuOpened": False,
                "saveMenuItemInvoked": False,
                "saveActionIdentityMatched": False,
            },
            blocked["menu"],
        )
        self.assertEqual(
            {
                "saveDialogObserved": False,
                "dialogType": "Swing JFileChooser",
                "dialogClass": None,
                "dialogShowing": False,
                "ambiguousChooserDiscovery": False,
                "pollCount": 0,
            },
            blocked["dialog"],
        )
        self.assertEqual(
            {
                "selectedPathSet": False,
                "approvedSelection": False,
                "selectedPathMatchesExpected": False,
                "targetInsideProofRoot": True,
                "normalizedSelectedPath": None,
                "expectedPath": EXPECTED_FILE,
            },
            blocked["control"],
        )
        self.assertEqual(
            {
                "fileWritten": False,
                "fileNonempty": False,
                "fileHasExpectedExtension": True,
                "outputPath": EXPECTED_FILE,
                "outputSizeBytes": 0,
            },
            blocked["write"],
        )
        self.assertEqual(
            {
                "projectReadable": False,
                "marker": EXPECTED_MARKER,
                "markerPresent": False,
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
        self.assertEqual(PROVEN_TOP_LEVEL_FIELDS, list(reference_proven.keys()))
        self.assertEqual(EXPECTED_NON_CLAIMS, reference_proven["doesNotClaim"])
        self.assertEqual(EXPECTED_CLAIM, reference_proven["claim"])
        self.assertIsNone(reference_proven["blocker"])

    def test_reference_proven_example_requires_all_success_evidence_true(self) -> None:
        proven = artifact_with_status(REFERENCE_PATH, "proven")

        self.assertEqual(
            {"fileMenuOpened": True, "saveMenuItemInvoked": True, "saveActionIdentityMatched": True},
            proven["menu"],
        )
        self.assertTrue(proven["dialog"]["saveDialogObserved"])
        self.assertTrue(proven["dialog"]["dialogShowing"])
        self.assertFalse(proven["dialog"]["ambiguousChooserDiscovery"])
        self.assertTrue(proven["control"]["selectedPathSet"])
        self.assertTrue(proven["control"]["approvedSelection"])
        self.assertTrue(proven["control"]["selectedPathMatchesExpected"])
        self.assertTrue(proven["control"]["targetInsideProofRoot"])
        self.assertTrue(proven["write"]["fileWritten"])
        self.assertTrue(proven["write"]["fileNonempty"])
        self.assertTrue(proven["write"]["fileHasExpectedExtension"])
        self.assertTrue(proven["readback"]["projectReadable"])
        self.assertTrue(proven["readback"]["markerPresent"])
        self.assertEqual(EXPECTED_MARKER, proven["readback"]["marker"])

    def test_howto_text_limits_the_scope_to_blocker_evidence_until_status_is_proven(self) -> None:
        text = HOWTO_PATH.read_text(encoding="utf-8")

        self.assertIn("Use the JSON artifact as the source of truth.", text)
        self.assertIn("Do not treat Maven success by itself as proof.", text)
        self.assertIn("A blocked artifact is blocker evidence for its `blocker.kind` only", text)
        self.assertIn("Use that artifact only as headless-display blocker evidence.", text)
        self.assertIn("It does not prove Robot Save activation", text)
        self.assertIn("full desktop Save completion", text)


if __name__ == "__main__":
    unittest.main()
