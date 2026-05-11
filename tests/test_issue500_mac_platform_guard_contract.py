"""Contract tests for issue #500: Mac platform detection in Robot menu tests.

Verifies that JMenuBarRobotClickSaveProofTest and
RobotSaveMenuDialogWriteReadbackProofTest have the required
assumeFalse(SystemUtilities.isMac()) guards so AWT Robot
screen-coordinate menu clicks skip on macOS where the menu bar
is native and outside the JFrame.
"""
import re
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]

JMENUBAR_TEST_PATH = (
    REPO_ROOT
    / "core"
    / "ide"
    / "src"
    / "test"
    / "java"
    / "org"
    / "alice"
    / "ide"
    / "croquet"
    / "models"
    / "projecturi"
    / "JMenuBarRobotClickSaveProofTest.java"
)

ROBOT_SAVE_TEST_PATH = (
    REPO_ROOT
    / "core"
    / "ide"
    / "src"
    / "test"
    / "java"
    / "org"
    / "alice"
    / "ide"
    / "croquet"
    / "models"
    / "projecturi"
    / "RobotSaveMenuDialogWriteReadbackProofTest.java"
)

REFERENCE_DOC_PATH = REPO_ROOT / "docs" / "reference" / "mac-compatible-test-guards.md"

SYSTEM_UTILITIES_IMPORT = "import edu.cmu.cs.dennisc.java.lang.SystemUtilities;"
ASSUME_FALSE_STATIC_IMPORT = "import static org.junit.Assume.assumeFalse;"
IS_MAC_GUARD_PATTERN = re.compile(
    r"assumeFalse\(\s*\".*macOS.*native.*menu.*bar.*\"\s*,\s*SystemUtilities\.isMac\(\)\s*\)"
)

# The Robot-driven test method in RobotSaveMenuDialogWriteReadbackProofTest
ROBOT_METHOD = "robotFileSaveApprovesChooserWritesReadableMarkedProjectOrWritesBlocker"

# Evidence-contract methods that must NOT have the isMac guard
EVIDENCE_METHODS = [
    "incompleteArtifactIsBlockedAndDoesNotClaimChooserWriteOrReadback",
    "artifactRequiresRobotSaveClickBeforeReportingProven",
    "completeArtifactReportsNarrowProvenClaim",
    "targetOutsideProofRootIsBlockedAndRedacted",
    "wrongSelectedFileDoesNotRewriteExpectedTargetBoundaryEvidence",
]


@lru_cache(maxsize=None)
def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _extract_method_body(source: str, method_name: str) -> str:
    """Extract the body of a Java method from source by name.

    Returns the text from the method signature through the next
    unindented closing brace or next @Test annotation, whichever
    comes first.
    """
    pattern = re.compile(
        rf"public\s+void\s+{re.escape(method_name)}\s*\(", re.DOTALL
    )
    match = pattern.search(source)
    if not match:
        raise AssertionError(f"Method {method_name} not found in source")
    start = match.start()
    # Find the end: next @Test annotation or end of class
    rest = source[start:]
    # Look for the next @Test to delimit
    next_test = re.search(r"\n\s*@Test\b", rest[1:])
    if next_test:
        return rest[: next_test.start() + 1]
    return rest


class JMenuBarRobotClickSaveProofTestMacGuardContract(unittest.TestCase):
    """Contract: JMenuBarRobotClickSaveProofTest must have the isMac guard."""

    def test_file_exists(self) -> None:
        self.assertTrue(
            JMENUBAR_TEST_PATH.exists(),
            f"Expected test file at {JMENUBAR_TEST_PATH.relative_to(REPO_ROOT)}",
        )

    def test_imports_system_utilities(self) -> None:
        source = _read(JMENUBAR_TEST_PATH)
        self.assertIn(
            SYSTEM_UTILITIES_IMPORT,
            source,
            "JMenuBarRobotClickSaveProofTest must import "
            "edu.cmu.cs.dennisc.java.lang.SystemUtilities",
        )

    def test_imports_assume_false(self) -> None:
        source = _read(JMENUBAR_TEST_PATH)
        self.assertIn(
            ASSUME_FALSE_STATIC_IMPORT,
            source,
            "JMenuBarRobotClickSaveProofTest must static-import assumeFalse",
        )

    def test_has_is_mac_guard(self) -> None:
        source = _read(JMENUBAR_TEST_PATH)
        self.assertRegex(
            source,
            IS_MAC_GUARD_PATTERN,
            "JMenuBarRobotClickSaveProofTest must call "
            "assumeFalse('macOS ...native...menu...bar...', SystemUtilities.isMac())",
        )

    def test_is_mac_guard_after_headless_guard(self) -> None:
        """The isMac guard must appear after the headless guard so CI
        distinguishes 'skipped because headless' from 'skipped because macOS'."""
        source = _read(JMENUBAR_TEST_PATH)
        headless_pos = source.find("GraphicsEnvironment.isHeadless()")
        self.assertGreater(
            headless_pos,
            -1,
            "Expected headless guard in JMenuBarRobotClickSaveProofTest",
        )
        mac_match = IS_MAC_GUARD_PATTERN.search(source)
        self.assertIsNotNone(
            mac_match, "Expected isMac guard in JMenuBarRobotClickSaveProofTest"
        )
        self.assertGreater(
            mac_match.start(),
            headless_pos,
            "isMac guard must appear after the headless guard",
        )

    def test_single_test_method_has_both_guards(self) -> None:
        """The single @Test method must contain both headless and isMac guards."""
        source = _read(JMENUBAR_TEST_PATH)
        method_body = _extract_method_body(
            source,
            "robotClickFileMenuInVisibleJMenuBarSelectsSaveDispatchesToSaveOperation",
        )
        self.assertIn(
            "GraphicsEnvironment.isHeadless()",
            method_body,
            "Test method must have headless guard",
        )
        self.assertRegex(
            method_body,
            IS_MAC_GUARD_PATTERN,
            "Test method must have isMac guard",
        )


class RobotSaveMenuDialogWriteReadbackProofTestMacGuardContract(unittest.TestCase):
    """Contract: RobotSaveMenuDialogWriteReadbackProofTest must have
    the isMac guard only in the Robot-driven method."""

    def test_file_exists(self) -> None:
        self.assertTrue(
            ROBOT_SAVE_TEST_PATH.exists(),
            f"Expected test file at {ROBOT_SAVE_TEST_PATH.relative_to(REPO_ROOT)}",
        )

    def test_imports_system_utilities(self) -> None:
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertIn(
            SYSTEM_UTILITIES_IMPORT,
            source,
            "RobotSaveMenuDialogWriteReadbackProofTest must import "
            "edu.cmu.cs.dennisc.java.lang.SystemUtilities",
        )

    def test_imports_assume_false(self) -> None:
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertIn(
            ASSUME_FALSE_STATIC_IMPORT,
            source,
            "RobotSaveMenuDialogWriteReadbackProofTest must static-import assumeFalse",
        )

    def test_robot_method_has_is_mac_guard(self) -> None:
        source = _read(ROBOT_SAVE_TEST_PATH)
        method_body = _extract_method_body(source, ROBOT_METHOD)
        self.assertRegex(
            method_body,
            IS_MAC_GUARD_PATTERN,
            f"Robot method {ROBOT_METHOD} must have isMac guard",
        )

    def test_evidence_methods_do_not_have_is_mac_guard(self) -> None:
        """The five evidence-contract methods must NOT have the isMac guard.
        They validate artifact schemas, not Robot UI interactions."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        for method_name in EVIDENCE_METHODS:
            method_body = _extract_method_body(source, method_name)
            self.assertNotRegex(
                method_body,
                IS_MAC_GUARD_PATTERN,
                f"Evidence method {method_name} must NOT have isMac guard",
            )

    def test_is_mac_guard_placement_early_in_robot_method(self) -> None:
        """The isMac guard must appear near the start of the Robot method,
        before the proof root setup."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        method_body = _extract_method_body(source, ROBOT_METHOD)
        mac_match = IS_MAC_GUARD_PATTERN.search(method_body)
        self.assertIsNotNone(
            mac_match,
            "Expected isMac guard in Robot method",
        )
        # Guard should appear before 'canonicalProofRoot()' call
        proof_root_pos = method_body.find("canonicalProofRoot()")
        if proof_root_pos > -1:
            self.assertLess(
                mac_match.start(),
                proof_root_pos,
                "isMac guard must appear before canonicalProofRoot() setup",
            )


class MacGuardDocumentationContract(unittest.TestCase):
    """Contract: reference doc must describe the isMac guard correctly."""

    def test_reference_doc_exists(self) -> None:
        self.assertTrue(
            REFERENCE_DOC_PATH.exists(),
            f"Expected reference doc at {REFERENCE_DOC_PATH.relative_to(REPO_ROOT)}",
        )

    def test_reference_doc_mentions_issue_500(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn("#500", text, "Reference doc must mention issue #500")

    def test_reference_doc_mentions_both_test_classes(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "JMenuBarRobotClickSaveProofTest",
            text,
            "Reference doc must mention JMenuBarRobotClickSaveProofTest",
        )
        self.assertIn(
            "RobotSaveMenuDialogWriteReadbackProofTest",
            text,
            "Reference doc must mention RobotSaveMenuDialogWriteReadbackProofTest",
        )

    def test_reference_doc_describes_system_utilities_api(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "SystemUtilities.isMac()",
            text,
            "Reference doc must describe SystemUtilities.isMac() API",
        )
        self.assertIn(
            "edu.cmu.cs.dennisc.java.lang.SystemUtilities",
            text,
            "Reference doc must name the fully qualified class",
        )

    def test_reference_doc_has_macos_native_menu_bar_section(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "macOS native-menu-bar skip guard",
            text,
            "Reference doc must have the macOS native-menu-bar skip guard section",
        )

    def test_reference_doc_shows_guard_code_examples(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "assumeFalse(",
            text,
            "Reference doc must show assumeFalse guard code example",
        )
        self.assertIn(
            "SystemUtilities.isMac()",
            text,
            "Reference doc must show SystemUtilities.isMac() in guard example",
        )

    def test_reference_doc_expected_behavior_table(self) -> None:
        """The reference doc must include an expected-behavior table that shows
        macOS rows with Skip(isMac) for Robot tests and Pass for evidence tests."""
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "Skip",
            text,
            "Expected behavior table must show Skip entries",
        )
        self.assertIn(
            "isMac",
            text,
            "Expected behavior table must reference isMac skip reason",
        )

    def test_reference_doc_compatibility_rule_for_mac_guard(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "SystemUtilities.isMac()",
            text,
        )
        self.assertIn(
            "Robot screen-coordinate",
            text,
            "Compatibility rules must mention Robot screen-coordinate clicks",
        )


if __name__ == "__main__":
    unittest.main()
