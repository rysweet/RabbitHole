# tests/test_pr437_tab_click_probe_unit.py
"""TDD unit tests for tab-click-probe.py pure functions.

Covers target starter validation, PID extraction, role normalization,
payload factories, and safety metadata — all exercised without AT-SPI.
"""
from __future__ import annotations

import importlib.util
import os
import sys
import unittest
from functools import cache
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
PROBE_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "tab-click-probe.py"


@cache
def load_probe() -> Any:
    if not PROBE_PATH.exists():
        raise AssertionError(f"Expected tab-click-probe.py at {PROBE_PATH}")
    spec = importlib.util.spec_from_file_location("tab_click_probe", PROBE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class FindSelectProjectJavaPidTest(unittest.TestCase):
    """Unit contract: find_select_project_java_pid extracts PID only for Select Project windows."""

    def test_valid_select_project_window(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": 555, "processName": "java", "title": "Select Project"}
            ]
        }
        self.assertEqual(555, probe.find_select_project_java_pid(inventory))

    def test_alice_window_not_matched(self) -> None:
        """Only 'Select Project' title matches, not 'Alice 3'."""
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": 123, "processName": "java", "title": "Alice 3"}
            ]
        }
        self.assertIsNone(probe.find_select_project_java_pid(inventory))

    def test_non_java_process_not_matched(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": 123, "processName": "python3", "title": "Select Project"}
            ]
        }
        self.assertIsNone(probe.find_select_project_java_pid(inventory))

    def test_empty_windows(self) -> None:
        probe = load_probe()
        self.assertIsNone(probe.find_select_project_java_pid({"windows": []}))

    def test_non_list_windows(self) -> None:
        probe = load_probe()
        self.assertIsNone(probe.find_select_project_java_pid({"windows": "bad"}))

    def test_negative_pid_rejected(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": -1, "processName": "java", "title": "Select Project"}
            ]
        }
        self.assertIsNone(probe.find_select_project_java_pid(inventory))

    def test_string_pid_rejected(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": "555", "processName": "java", "title": "Select Project"}
            ]
        }
        self.assertIsNone(probe.find_select_project_java_pid(inventory))


class ValidateTargetStarterTest(unittest.TestCase):
    """Unit contract: validate_target_starter enforces Africa Full metadata."""

    def test_none_target_returns_empty_string(self) -> None:
        probe = load_probe()
        self.assertEqual("", probe.validate_target_starter(None))

    def test_valid_africa_full_returns_empty_string(self) -> None:
        probe = load_probe()
        target = {
            "displayName": "Africa Full",
            "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
        }
        self.assertEqual("", probe.validate_target_starter(target))

    def test_empty_display_name_returns_error(self) -> None:
        probe = load_probe()
        target = {"displayName": "", "repositoryPath": "some/path"}
        result = probe.validate_target_starter(target)
        self.assertIn("TARGET_STARTER_DISPLAY_NAME", result)

    def test_empty_repository_path_returns_error(self) -> None:
        probe = load_probe()
        target = {"displayName": "Africa Full", "repositoryPath": ""}
        result = probe.validate_target_starter(target)
        self.assertIn("TARGET_STARTER_REPO_PATH", result)

    def test_absolute_path_rejected(self) -> None:
        probe = load_probe()
        target = {"displayName": "Africa Full", "repositoryPath": "/absolute/path.a3p"}
        result = probe.validate_target_starter(target)
        self.assertIn("repository-relative", result)

    def test_path_traversal_rejected(self) -> None:
        probe = load_probe()
        target = {"displayName": "Africa Full", "repositoryPath": "core/../etc/passwd"}
        result = probe.validate_target_starter(target)
        self.assertIn("..", result)

    def test_wrong_display_name_rejected(self) -> None:
        probe = load_probe()
        target = {
            "displayName": "Snow Full",
            "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
        }
        result = probe.validate_target_starter(target)
        self.assertIn("Africa Full", result)

    def test_wrong_repository_path_rejected(self) -> None:
        probe = load_probe()
        target = {
            "displayName": "Africa Full",
            "repositoryPath": "core/resources/wrong-path.a3p",
        }
        result = probe.validate_target_starter(target)
        self.assertIn("core/resources/src/application/resources/starter-projects/AfricaFull.a3p", result)


class NormaliseRoleNameTest(unittest.TestCase):
    """Unit contract: normalise_role_name lowercases and removes spaces."""

    def test_page_tab(self) -> None:
        probe = load_probe()
        self.assertEqual("pagetab", probe.normalise_role_name("page tab"))

    def test_toggle_button(self) -> None:
        probe = load_probe()
        self.assertEqual("togglebutton", probe.normalise_role_name("toggle button"))

    def test_already_normalised(self) -> None:
        probe = load_probe()
        self.assertEqual("pagetablist", probe.normalise_role_name("pagetablist"))

    def test_mixed_case(self) -> None:
        probe = load_probe()
        self.assertEqual("pushbutton", probe.normalise_role_name("Push Button"))


class StatesIncludeTest(unittest.TestCase):
    """Unit contract: states_include performs case-insensitive substring matching."""

    def test_exact_match(self) -> None:
        probe = load_probe()
        self.assertTrue(probe.states_include(["showing", "visible"], "showing"))

    def test_case_insensitive(self) -> None:
        probe = load_probe()
        self.assertTrue(probe.states_include(["SHOWING"], "showing"))

    def test_substring_match(self) -> None:
        probe = load_probe()
        self.assertTrue(probe.states_include(["ATK_STATE_SHOWING"], "showing"))

    def test_no_match(self) -> None:
        probe = load_probe()
        self.assertFalse(probe.states_include(["visible", "enabled"], "showing"))

    def test_empty_states(self) -> None:
        probe = load_probe()
        self.assertFalse(probe.states_include([], "showing"))


class EmptyTabProbePayloadTest(unittest.TestCase):
    """Unit contract: empty_tab_probe_payload has required keys."""

    def test_required_keys(self) -> None:
        probe = load_probe()
        payload = probe.empty_tab_probe_payload(
            status="blocked",
            blocker="test",
            blocker_detail="detail",
            java_pid=123,
        )
        for key in [
            "status", "blocker", "blockerDetail", "javaPid",
            "allWidgetTree", "roleCounts", "tabListNodes", "tabNodes",
            "tabClickAttempts", "widgetCountAfterClick",
            "allWidgetTreeAfterClick", "projectOpenObserved",
            "projectOpenAttempt",
        ]:
            with self.subTest(key=key):
                self.assertIn(key, payload)

    def test_defaults(self) -> None:
        probe = load_probe()
        payload = probe.empty_tab_probe_payload(
            status="blocked",
            blocker="test",
            blocker_detail="",
            java_pid=None,
        )
        self.assertFalse(payload["projectOpenObserved"])
        self.assertEqual([], payload["allWidgetTree"])
        self.assertEqual({}, payload["roleCounts"])
        self.assertEqual({}, payload["projectOpenAttempt"])


class NextBlockerTest(unittest.TestCase):
    """Unit contract: next_blocker produces structured machine-readable records."""

    def test_all_fields_present(self) -> None:
        probe = load_probe()
        result = probe.next_blocker(
            "AT-SPI state text",
            "attempted action",
            "expected next action",
            "reason stopped",
        )
        self.assertEqual("AT-SPI state text", result["observedAtspiState"])
        self.assertEqual("attempted action", result["actionAttempted"])
        self.assertEqual("expected next action", result["expectedNextAction"])
        self.assertEqual("reason stopped", result["reasonProgressStopped"])


class AddTargetMetadataTest(unittest.TestCase):
    """Unit contract: add_target_metadata appends target fields only when target is not None."""

    def test_none_target_returns_payload_unchanged(self) -> None:
        probe = load_probe()
        payload = {"status": "blocked"}
        result = probe.add_target_metadata(
            payload,
            None,
            evidence_status="blocked",
            blocker=None,
        )
        self.assertNotIn("targetStarter", result)
        self.assertNotIn("evidenceStatus", result)

    def test_with_target_adds_all_fields(self) -> None:
        probe = load_probe()
        payload = {"status": "blocked"}
        target = {"displayName": "Africa Full", "repositoryPath": "some/path"}
        result = probe.add_target_metadata(
            payload,
            target,
            evidence_status="blocked",
            blocker={"observedAtspiState": "test"},
        )
        for key in [
            "targetStarter", "targetStarterObserved", "targetStarterSelected",
            "targetStarterOpenAttempted", "targetSelectionObserved",
            "openAttempted", "openedStarter", "evidenceStatus", "nextBlocker",
        ]:
            with self.subTest(key=key):
                self.assertIn(key, result)
        self.assertEqual(target, result["targetStarter"])
        self.assertEqual("blocked", result["evidenceStatus"])


class RoleCountsForTest(unittest.TestCase):
    """Unit contract: role_counts_for tallies roles from widget records."""

    def test_counts_roles(self) -> None:
        probe = load_probe()
        widgets = [
            {"role": "panel"},
            {"role": "button"},
            {"role": "panel"},
            {"role": "label"},
        ]
        counts = probe.role_counts_for(widgets)
        self.assertEqual(2, counts["panel"])
        self.assertEqual(1, counts["button"])
        self.assertEqual(1, counts["label"])

    def test_empty_list(self) -> None:
        probe = load_probe()
        self.assertEqual({}, probe.role_counts_for([]))


class DefaultProjectOpenRecordTest(unittest.TestCase):
    """Unit contract: default_project_open_record has required fields."""

    def test_required_fields(self) -> None:
        probe = load_probe()
        record = probe.default_project_open_record()
        for key in [
            "startersTabClick", "listItemClick", "okButtonClick",
            "projectOpenObserved", "projectOpenDetail",
        ]:
            with self.subTest(key=key):
                self.assertIn(key, record)
        self.assertFalse(record["projectOpenObserved"])


class NodeInfoRecordsTest(unittest.TestCase):
    """Unit contract: node_info_records extracts name/role/depth from tuples."""

    def test_conversion(self) -> None:
        probe = load_probe()

        class FakeNode:
            pass

        infos = [(FakeNode(), 3, "Starters", "toggle button")]
        result = probe.node_info_records(infos)
        self.assertEqual(1, len(result))
        self.assertEqual("Starters", result[0]["name"])
        self.assertEqual("toggle button", result[0]["role"])
        self.assertEqual(3, result[0]["depth"])


class StartersTabSafetyTest(unittest.TestCase):
    """Unit contract: starters_tab_safety produces correct metadata from click records."""

    def test_successful_click(self) -> None:
        probe = load_probe()
        click_record = {"attempted": True, "success": True, "detail": "ok"}
        safety = probe.starters_tab_safety(click_record)
        self.assertEqual("Starters", safety["tabName"])
        self.assertTrue(safety["activationAttempted"])
        self.assertTrue(safety["activatedBeforeTargetSearch"])
        self.assertEqual("active-starters-tab", safety["targetSearchScope"])

    def test_failed_click(self) -> None:
        probe = load_probe()
        click_record = {"attempted": True, "success": False, "detail": "action failed"}
        safety = probe.starters_tab_safety(click_record)
        self.assertTrue(safety["activationAttempted"])
        self.assertFalse(safety["activatedBeforeTargetSearch"])
        self.assertEqual("not-started", safety["targetSearchScope"])

    def test_no_attempt(self) -> None:
        probe = load_probe()
        click_record = {}
        safety = probe.starters_tab_safety(click_record)
        self.assertFalse(safety["activationAttempted"])
        self.assertFalse(safety["activatedBeforeTargetSearch"])


class WidgetTreePropertiesTest(unittest.TestCase):
    """Unit contract: widget_tree_properties redacts non-allowlisted names."""

    def test_allowed_name_not_redacted(self) -> None:
        probe = load_probe()

        class FakeNode:
            name = "Select Project"
            description = ""
            childCount = 5

            def getRoleName(self):
                return "frame"

        result = probe.widget_tree_properties(FakeNode())
        self.assertEqual("Select Project", result["name"])
        self.assertFalse(result["nameRedacted"])

    def test_disallowed_name_redacted(self) -> None:
        probe = load_probe()

        class FakeNode:
            name = "user-specific-project-name"
            description = ""
            childCount = 2

            def getRoleName(self):
                return "panel"

        result = probe.widget_tree_properties(FakeNode())
        self.assertEqual("", result["name"])
        self.assertTrue(result["nameRedacted"])

    def test_description_always_redacted(self) -> None:
        probe = load_probe()

        class FakeNode:
            name = "OK"
            description = "some desc"
            childCount = 0

            def getRoleName(self):
                return "button"

        result = probe.widget_tree_properties(FakeNode())
        self.assertEqual("", result["description"])
        self.assertTrue(result["descriptionRedacted"])


class ConfiguredTargetStarterTest(unittest.TestCase):
    """Unit contract: configured_target_starter reads from environment variables."""

    def test_no_env_vars_returns_none(self) -> None:
        probe = load_probe()
        env_backup = {}
        for key in ("TARGET_STARTER_DISPLAY_NAME", "TARGET_STARTER_REPO_PATH"):
            env_backup[key] = os.environ.pop(key, None)
        try:
            result = probe.configured_target_starter()
            self.assertIsNone(result)
        finally:
            for key, value in env_backup.items():
                if value is not None:
                    os.environ[key] = value

    def test_with_env_vars_returns_dict(self) -> None:
        probe = load_probe()
        env_backup = {}
        for key in ("TARGET_STARTER_DISPLAY_NAME", "TARGET_STARTER_REPO_PATH"):
            env_backup[key] = os.environ.get(key)
        os.environ["TARGET_STARTER_DISPLAY_NAME"] = "Africa Full"
        os.environ["TARGET_STARTER_REPO_PATH"] = "core/resources/path.a3p"
        try:
            result = probe.configured_target_starter()
            self.assertIsNotNone(result)
            self.assertEqual("Africa Full", result["displayName"])
            self.assertEqual("core/resources/path.a3p", result["repositoryPath"])
        finally:
            for key, value in env_backup.items():
                if value is not None:
                    os.environ[key] = value
                else:
                    os.environ.pop(key, None)


if __name__ == "__main__":
    unittest.main()
