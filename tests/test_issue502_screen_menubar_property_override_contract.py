"""Contract tests for issue #502: Replace assumeFalse(isMac()) with
apple.laf.useScreenMenuBar property override in Robot menu tests.

TDD: These tests define the expected behavior for issue #502. They FAIL
before the Java source files are modified and PASS after implementation.

The implementation must:
1. Remove assumeFalse(SystemUtilities.isMac()) from both Robot menu tests.
2. Remove the SystemUtilities import (no longer needed for isMac()).
3. Add a SCREEN_MENU_BAR_PROPERTY constant = "apple.laf.useScreenMenuBar".
4. Add a previousScreenMenuBar field to capture the original value.
5. In @Before: capture the property and set it to "false".
6. In @After: restore via restoreProperty(SCREEN_MENU_BAR_PROPERTY, ...).
7. Add a defensive re-set to "false" after ide.initialize() inside the
   test method, because Application.initialize() sets the property to
   "true" on macOS.

Supersedes the #500 contract tests in test_issue500_mac_platform_guard_contract.py
which required the assumeFalse(isMac()) guard.
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
HOWTO_DOC_PATH = REPO_ROOT / "docs" / "howto" / "review-mac-compatible-test-guards.md"

# Patterns that must NOT appear after implementation
IS_MAC_GUARD_PATTERN = re.compile(
    r"assumeFalse\(\s*\".*macOS.*native.*menu.*bar.*\"\s*,\s*SystemUtilities\.isMac\(\)\s*\)"
)
SYSTEM_UTILITIES_IMPORT = "import edu.cmu.cs.dennisc.java.lang.SystemUtilities;"

# Patterns that MUST appear after implementation
SCREEN_MENU_BAR_CONSTANT = re.compile(
    r'private\s+static\s+final\s+String\s+SCREEN_MENU_BAR_PROPERTY\s*='
    r'\s*"apple\.laf\.useScreenMenuBar"\s*;'
)
PREVIOUS_SCREEN_MENU_BAR_FIELD = re.compile(
    r'private\s+String\s+previousScreenMenuBar\s*;'
)
CAPTURE_PATTERN = re.compile(
    r'previousScreenMenuBar\s*=\s*System\.getProperty\s*\(\s*SCREEN_MENU_BAR_PROPERTY\s*\)'
)
SET_FALSE_PATTERN = re.compile(
    r'System\.setProperty\s*\(\s*SCREEN_MENU_BAR_PROPERTY\s*,\s*"false"\s*\)'
)
RESTORE_PATTERN = re.compile(
    r'restoreProperty\s*\(\s*SCREEN_MENU_BAR_PROPERTY\s*,\s*previousScreenMenuBar\s*\)'
)
# Defensive re-set: System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false") after ide.initialize()
DEFENSIVE_RESET_AFTER_INIT = re.compile(
    r'ide\.initialize\s*\(\s*new\s+String\s*\[\s*0\s*\]\s*\)\s*;'
    r'.*?'
    r'System\.setProperty\s*\(\s*SCREEN_MENU_BAR_PROPERTY\s*,\s*"false"\s*\)',
    re.DOTALL,
)


@lru_cache(maxsize=None)
def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _extract_method_body(source: str, method_name: str) -> str:
    """Extract the body of a Java method from source by name."""
    pattern = re.compile(
        rf"(?:public|private)\s+(?:static\s+)?(?:\w+\s+)?void\s+{re.escape(method_name)}\s*\(",
        re.DOTALL,
    )
    match = pattern.search(source)
    if not match:
        raise AssertionError(f"Method {method_name} not found in source")
    start = match.start()
    rest = source[start:]
    next_method = re.search(r"\n\s*(?:@Test|@Before|@After|public\s+void|private\s+)", rest[1:])
    if next_method:
        return rest[: next_method.start() + 1]
    return rest


# ---------------------------------------------------------------------------
# JMenuBarRobotClickSaveProofTest contracts
# ---------------------------------------------------------------------------

class JMenuBarTestRemovesIsMacGuard(unittest.TestCase):
    """Issue #502: JMenuBarRobotClickSaveProofTest must NOT skip on macOS."""

    def test_file_exists(self) -> None:
        self.assertTrue(
            JMENUBAR_TEST_PATH.exists(),
            f"Expected test file at {JMENUBAR_TEST_PATH.relative_to(REPO_ROOT)}",
        )

    def test_no_assume_false_is_mac(self) -> None:
        """assumeFalse(isMac()) must be removed — tests should run on Mac."""
        source = _read(JMENUBAR_TEST_PATH)
        self.assertNotRegex(
            source,
            IS_MAC_GUARD_PATTERN,
            "JMenuBarRobotClickSaveProofTest must NOT have assumeFalse(isMac()) "
            "after issue #502 — the property override replaces the skip",
        )

    def test_no_system_utilities_import(self) -> None:
        """SystemUtilities import must be removed (was only used for isMac)."""
        source = _read(JMENUBAR_TEST_PATH)
        self.assertNotIn(
            SYSTEM_UTILITIES_IMPORT,
            source,
            "JMenuBarRobotClickSaveProofTest must not import SystemUtilities "
            "after issue #502 — isMac() is no longer called",
        )

    def test_headless_guard_still_present(self) -> None:
        """The headless guard must survive — it's a different concern."""
        source = _read(JMENUBAR_TEST_PATH)
        self.assertIn(
            "GraphicsEnvironment.isHeadless()",
            source,
            "Headless guard must remain — only the isMac guard is removed",
        )


class JMenuBarTestHasPropertyOverride(unittest.TestCase):
    """Issue #502: JMenuBarRobotClickSaveProofTest must use the
    apple.laf.useScreenMenuBar property override pattern."""

    def test_declares_screen_menu_bar_property_constant(self) -> None:
        source = _read(JMENUBAR_TEST_PATH)
        self.assertRegex(
            source,
            SCREEN_MENU_BAR_CONSTANT,
            'Must declare: private static final String SCREEN_MENU_BAR_PROPERTY = '
            '"apple.laf.useScreenMenuBar";',
        )

    def test_declares_previous_screen_menu_bar_field(self) -> None:
        source = _read(JMENUBAR_TEST_PATH)
        self.assertRegex(
            source,
            PREVIOUS_SCREEN_MENU_BAR_FIELD,
            "Must declare: private String previousScreenMenuBar;",
        )

    def test_before_captures_property(self) -> None:
        """@Before must capture System.getProperty(SCREEN_MENU_BAR_PROPERTY)."""
        source = _read(JMENUBAR_TEST_PATH)
        before_body = _extract_method_body(source, "captureProperties")
        self.assertRegex(
            before_body,
            CAPTURE_PATTERN,
            "@Before must capture: previousScreenMenuBar = "
            "System.getProperty(SCREEN_MENU_BAR_PROPERTY)",
        )

    def test_before_sets_property_false(self) -> None:
        """@Before must set the property to 'false'."""
        source = _read(JMENUBAR_TEST_PATH)
        before_body = _extract_method_body(source, "captureProperties")
        self.assertRegex(
            before_body,
            SET_FALSE_PATTERN,
            '@Before must set: System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false")',
        )

    def test_after_restores_property(self) -> None:
        """@After must restore via restoreProperty(SCREEN_MENU_BAR_PROPERTY, ...)."""
        source = _read(JMENUBAR_TEST_PATH)
        after_body = _extract_method_body(
            source, "restorePropertiesAndActiveApplication"
        )
        self.assertRegex(
            after_body,
            RESTORE_PATTERN,
            "@After must call: restoreProperty(SCREEN_MENU_BAR_PROPERTY, previousScreenMenuBar)",
        )

    def test_capture_before_set_in_before_method(self) -> None:
        """In @Before, capture must appear before the set-to-false."""
        source = _read(JMENUBAR_TEST_PATH)
        before_body = _extract_method_body(source, "captureProperties")
        capture_match = CAPTURE_PATTERN.search(before_body)
        set_match = SET_FALSE_PATTERN.search(before_body)
        self.assertIsNotNone(capture_match, "Capture pattern missing in @Before")
        self.assertIsNotNone(set_match, "Set-false pattern missing in @Before")
        self.assertLess(
            capture_match.start(),
            set_match.start(),
            "Property capture must come before set-to-false in @Before",
        )

    def test_defensive_reset_after_ide_initialize(self) -> None:
        """System.setProperty(SCREEN_MENU_BAR_PROPERTY, 'false') must appear
        after ide.initialize() to counteract Application.initialize() setting
        it to 'true' on macOS."""
        source = _read(JMENUBAR_TEST_PATH)
        self.assertRegex(
            source,
            DEFENSIVE_RESET_AFTER_INIT,
            "Defensive re-set to 'false' must appear after ide.initialize()",
        )


# ---------------------------------------------------------------------------
# RobotSaveMenuDialogWriteReadbackProofTest contracts
# ---------------------------------------------------------------------------

class RobotSaveTestRemovesIsMacGuard(unittest.TestCase):
    """Issue #502: RobotSaveMenuDialogWriteReadbackProofTest must NOT skip on macOS."""

    def test_file_exists(self) -> None:
        self.assertTrue(
            ROBOT_SAVE_TEST_PATH.exists(),
            f"Expected test file at {ROBOT_SAVE_TEST_PATH.relative_to(REPO_ROOT)}",
        )

    def test_no_assume_false_is_mac(self) -> None:
        """assumeFalse(isMac()) must be removed — tests should run on Mac."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertNotRegex(
            source,
            IS_MAC_GUARD_PATTERN,
            "RobotSaveMenuDialogWriteReadbackProofTest must NOT have "
            "assumeFalse(isMac()) after issue #502",
        )

    def test_no_system_utilities_import(self) -> None:
        """SystemUtilities import must be removed (was only used for isMac)."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertNotIn(
            SYSTEM_UTILITIES_IMPORT,
            source,
            "RobotSaveMenuDialogWriteReadbackProofTest must not import "
            "SystemUtilities after issue #502",
        )

    def test_headless_detection_still_present(self) -> None:
        """The headless/non-headless AWT detection must survive."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertIn(
            "isNonHeadlessAwtDisplayAvailable",
            source,
            "Headless AWT detection must remain — only the isMac guard is removed",
        )


class RobotSaveTestHasPropertyOverride(unittest.TestCase):
    """Issue #502: RobotSaveMenuDialogWriteReadbackProofTest must use the
    apple.laf.useScreenMenuBar property override pattern."""

    def test_declares_screen_menu_bar_property_constant(self) -> None:
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertRegex(
            source,
            SCREEN_MENU_BAR_CONSTANT,
            'Must declare: private static final String SCREEN_MENU_BAR_PROPERTY = '
            '"apple.laf.useScreenMenuBar";',
        )

    def test_declares_previous_screen_menu_bar_field(self) -> None:
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertRegex(
            source,
            PREVIOUS_SCREEN_MENU_BAR_FIELD,
            "Must declare: private String previousScreenMenuBar;",
        )

    def test_before_captures_property(self) -> None:
        """@Before must capture System.getProperty(SCREEN_MENU_BAR_PROPERTY)."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        before_body = _extract_method_body(source, "captureProperties")
        self.assertRegex(
            before_body,
            CAPTURE_PATTERN,
            "@Before must capture: previousScreenMenuBar = "
            "System.getProperty(SCREEN_MENU_BAR_PROPERTY)",
        )

    def test_before_sets_property_false(self) -> None:
        """@Before must set the property to 'false'."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        before_body = _extract_method_body(source, "captureProperties")
        self.assertRegex(
            before_body,
            SET_FALSE_PATTERN,
            '@Before must set: System.setProperty(SCREEN_MENU_BAR_PROPERTY, "false")',
        )

    def test_after_restores_property(self) -> None:
        """@After must restore via restoreProperty(SCREEN_MENU_BAR_PROPERTY, ...)."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        after_body = _extract_method_body(
            source, "restorePropertiesAndResetApplication"
        )
        self.assertRegex(
            after_body,
            RESTORE_PATTERN,
            "@After must call: restoreProperty(SCREEN_MENU_BAR_PROPERTY, previousScreenMenuBar)",
        )

    def test_capture_before_set_in_before_method(self) -> None:
        """In @Before, capture must appear before the set-to-false."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        before_body = _extract_method_body(source, "captureProperties")
        capture_match = CAPTURE_PATTERN.search(before_body)
        set_match = SET_FALSE_PATTERN.search(before_body)
        self.assertIsNotNone(capture_match, "Capture pattern missing in @Before")
        self.assertIsNotNone(set_match, "Set-false pattern missing in @Before")
        self.assertLess(
            capture_match.start(),
            set_match.start(),
            "Property capture must come before set-to-false in @Before",
        )

    def test_defensive_reset_after_ide_initialize(self) -> None:
        """System.setProperty(SCREEN_MENU_BAR_PROPERTY, 'false') must appear
        after ide.initialize() in the test body."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        self.assertRegex(
            source,
            DEFENSIVE_RESET_AFTER_INIT,
            "Defensive re-set to 'false' must appear after ide.initialize()",
        )

    def test_evidence_methods_unchanged(self) -> None:
        """Evidence-contract methods must NOT have any Mac guard or property
        override — they validate artifact schemas, not Robot UI."""
        source = _read(ROBOT_SAVE_TEST_PATH)
        evidence_methods = [
            "incompleteArtifactIsBlockedAndDoesNotClaimChooserWriteOrReadback",
            "artifactRequiresRobotSaveClickBeforeReportingProven",
            "completeArtifactReportsNarrowProvenClaim",
            "targetOutsideProofRootIsBlockedAndRedacted",
            "wrongSelectedFileDoesNotRewriteExpectedTargetBoundaryEvidence",
        ]
        for method_name in evidence_methods:
            method_body = _extract_method_body(source, method_name)
            self.assertNotRegex(
                method_body,
                IS_MAC_GUARD_PATTERN,
                f"Evidence method {method_name} must NOT have isMac guard",
            )
            self.assertNotIn(
                "SCREEN_MENU_BAR_PROPERTY",
                method_body,
                f"Evidence method {method_name} must not reference "
                "SCREEN_MENU_BAR_PROPERTY",
            )


# ---------------------------------------------------------------------------
# Cross-file consistency contracts
# ---------------------------------------------------------------------------

class PropertyOverrideConsistency(unittest.TestCase):
    """Both Robot menu test classes must use the same pattern."""

    def test_both_files_use_same_constant_name(self) -> None:
        jmenubar_src = _read(JMENUBAR_TEST_PATH)
        robot_save_src = _read(ROBOT_SAVE_TEST_PATH)
        self.assertRegex(jmenubar_src, SCREEN_MENU_BAR_CONSTANT)
        self.assertRegex(robot_save_src, SCREEN_MENU_BAR_CONSTANT)

    def test_both_files_use_same_field_name(self) -> None:
        jmenubar_src = _read(JMENUBAR_TEST_PATH)
        robot_save_src = _read(ROBOT_SAVE_TEST_PATH)
        self.assertRegex(jmenubar_src, PREVIOUS_SCREEN_MENU_BAR_FIELD)
        self.assertRegex(robot_save_src, PREVIOUS_SCREEN_MENU_BAR_FIELD)

    def test_neither_file_references_is_mac(self) -> None:
        """No remaining isMac() calls in either file."""
        for path in (JMENUBAR_TEST_PATH, ROBOT_SAVE_TEST_PATH):
            source = _read(path)
            self.assertNotIn(
                "isMac()",
                source,
                f"{path.name} must not call isMac() after issue #502",
            )

    def test_neither_file_imports_system_utilities(self) -> None:
        for path in (JMENUBAR_TEST_PATH, ROBOT_SAVE_TEST_PATH):
            source = _read(path)
            self.assertNotIn(
                SYSTEM_UTILITIES_IMPORT,
                source,
                f"{path.name} must not import SystemUtilities after issue #502",
            )


# ---------------------------------------------------------------------------
# Documentation contracts
# ---------------------------------------------------------------------------

class PropertyOverrideDocumentation(unittest.TestCase):
    """Reference doc must describe the property override, not the old skip."""

    def test_reference_doc_exists(self) -> None:
        self.assertTrue(
            REFERENCE_DOC_PATH.exists(),
            f"Expected reference doc at {REFERENCE_DOC_PATH.relative_to(REPO_ROOT)}",
        )

    def test_reference_doc_mentions_issue_502(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn("#502", text, "Reference doc must mention issue #502")

    def test_reference_doc_describes_property_override(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "apple.laf.useScreenMenuBar",
            text,
            "Reference doc must describe the apple.laf.useScreenMenuBar property",
        )

    def test_reference_doc_describes_defensive_reset(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            "ide.initialize()",
            text,
            "Reference doc must explain the defensive re-set after ide.initialize()",
        )

    def test_reference_doc_describes_before_after_pattern(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn("@Before", text, "Must describe @Before capture")
        self.assertIn("@After", text, "Must describe @After restore")
        self.assertIn("restoreProperty", text, "Must mention restoreProperty helper")

    def test_reference_doc_shows_property_override_code(self) -> None:
        text = _read(REFERENCE_DOC_PATH)
        self.assertIn(
            'SCREEN_MENU_BAR_PROPERTY',
            text,
            "Reference doc must show SCREEN_MENU_BAR_PROPERTY constant in code examples",
        )
        self.assertIn(
            "previousScreenMenuBar",
            text,
            "Reference doc must show previousScreenMenuBar field in code examples",
        )

    def test_reference_doc_expected_behavior_no_skip_on_mac(self) -> None:
        """The expected behavior table must show Pass (not Skip) for
        Robot tests on macOS with display available."""
        text = _read(REFERENCE_DOC_PATH)
        self.assertNotIn(
            "Skip(isMac)",
            text,
            "Expected behavior must NOT show Skip(isMac) — tests now pass on Mac",
        )


class IssueNumber500ContractSuperseded(unittest.TestCase):
    """The old #500 contract must be superseded — its assertions about
    assumeFalse(isMac()) being REQUIRED are now wrong."""

    def test_old_contract_file_exists(self) -> None:
        """The #500 contract file should still exist (for git history),
        but the #502 contracts here take precedence."""
        old_path = REPO_ROOT / "tests" / "test_issue500_mac_platform_guard_contract.py"
        # It's OK if the file is deleted or kept — this test just documents
        # that #502 supersedes #500
        if old_path.exists():
            source = _read(old_path)
            # The old tests assert isMac guard is REQUIRED — that's now wrong
            self.assertIn(
                "assumeFalse",
                source,
                "Old #500 contract should reference the old pattern (for history)",
            )


if __name__ == "__main__":
    unittest.main()
