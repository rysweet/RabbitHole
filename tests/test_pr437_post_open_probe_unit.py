# tests/test_pr437_post_open_probe_unit.py
"""TDD unit tests for post-project-open-probe.py pure functions.

Covers payload generation, target starter gating, Java PID extraction,
and blocker payload factories — all exercised without AT-SPI.
"""
from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from functools import cache
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
PROBE_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "post-project-open-probe.py"


@cache
def load_probe() -> Any:
    if not PROBE_PATH.exists():
        raise AssertionError(f"Expected post-project-open-probe.py at {PROBE_PATH}")
    spec = importlib.util.spec_from_file_location("post_project_open_probe", PROBE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


VALID_INVENTORY = {
    "windows": [
        {
            "pid": 12345,
            "processName": "java",
            "title": "Alice 3",
            "className": "alice",
        }
    ]
}

VALID_TAB_CLICK_OPENED = {
    "status": "observed",
    "projectOpenObserved": True,
    "evidenceStatus": "opened",
    "targetStarter": {
        "displayName": "Africa Full",
        "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
    },
    "targetStarterObserved": {
        "name": "Africa Full",
        "role": "panel",
    },
    "openedStarter": {
        "displayName": "Africa Full",
        "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
    },
    "targetSelectionObserved": True,
    "openAttempted": True,
    "startersTabSafety": {
        "tabName": "Starters",
        "activationAttempted": True,
        "activatedBeforeTargetSearch": True,
        "targetSearchScope": "active-starters-tab",
    },
}


class PostOpenProbePayloadTest(unittest.TestCase):
    """Unit contract: post_open_payload always returns the required keys."""

    def test_post_open_payload_has_required_keys(self) -> None:
        probe = load_probe()
        payload = probe.post_open_payload(
            status="blocked",
            blocker="test-blocker",
            blocker_detail="detail text",
            java_pid=1234,
        )
        for key in [
            "status",
            "blocker",
            "blockerDetail",
            "javaPid",
            "postOpenWindowObserved",
            "mainFrameNames",
            "mainFrameChildCounts",
            "mainWindowObservationBlocker",
        ]:
            with self.subTest(key=key):
                self.assertIn(key, payload)

    def test_post_open_payload_defaults(self) -> None:
        probe = load_probe()
        payload = probe.post_open_payload(
            status="blocked",
            blocker="none",
            blocker_detail="",
            java_pid=None,
        )
        self.assertFalse(payload["postOpenWindowObserved"])
        self.assertEqual([], payload["mainFrameNames"])
        self.assertEqual([], payload["mainFrameChildCounts"])
        self.assertIsNone(payload["javaPid"])

    def test_post_open_payload_with_extra_fields(self) -> None:
        probe = load_probe()
        payload = probe.post_open_payload(
            status="observed",
            blocker="none",
            blocker_detail="",
            java_pid=999,
            post_open_observed=True,
            frame_names=["Alice 3"],
            frame_child_counts=[42],
            extra={"customKey": "customValue"},
        )
        self.assertTrue(payload["postOpenWindowObserved"])
        self.assertEqual(["Alice 3"], payload["mainFrameNames"])
        self.assertEqual([42], payload["mainFrameChildCounts"])
        self.assertEqual("customValue", payload["customKey"])


class FindJavaPidTest(unittest.TestCase):
    """Unit contract: find_java_pid only returns PIDs for Java processes titled 'Alice 3'."""

    def test_valid_inventory_returns_pid(self) -> None:
        probe = load_probe()
        pid = probe.find_java_pid(VALID_INVENTORY)
        self.assertEqual(12345, pid)

    def test_empty_windows_returns_none(self) -> None:
        probe = load_probe()
        self.assertIsNone(probe.find_java_pid({"windows": []}))

    def test_non_list_windows_returns_none(self) -> None:
        probe = load_probe()
        self.assertIsNone(probe.find_java_pid({"windows": "not-a-list"}))

    def test_wrong_process_name_returns_none(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": 1, "processName": "python3", "title": "Alice 3"}
            ]
        }
        self.assertIsNone(probe.find_java_pid(inventory))

    def test_wrong_title_returns_none(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": 1, "processName": "java", "title": "Select Project"}
            ]
        }
        self.assertIsNone(probe.find_java_pid(inventory))

    def test_negative_pid_returns_none(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": -1, "processName": "java", "title": "Alice 3"}
            ]
        }
        self.assertIsNone(probe.find_java_pid(inventory))

    def test_zero_pid_returns_none(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": 0, "processName": "java", "title": "Alice 3"}
            ]
        }
        self.assertIsNone(probe.find_java_pid(inventory))

    def test_non_integer_pid_returns_none(self) -> None:
        probe = load_probe()
        inventory = {
            "windows": [
                {"pid": "12345", "processName": "java", "title": "Alice 3"}
            ]
        }
        self.assertIsNone(probe.find_java_pid(inventory))

    def test_non_dict_window_entries_skipped(self) -> None:
        probe = load_probe()
        inventory = {"windows": ["not-a-dict", 42, None]}
        self.assertIsNone(probe.find_java_pid(inventory))

    def test_missing_windows_key_returns_none(self) -> None:
        probe = load_probe()
        self.assertIsNone(probe.find_java_pid({}))


class TargetStarterGatePayloadTest(unittest.TestCase):
    """Integration contract: target_starter_gate_payload rejects invalid or missing starter metadata."""

    def test_no_target_starter_with_project_open_returns_metadata_missing(self) -> None:
        """If projectOpenObserved=true but no targetStarter, refuse generic proof."""
        probe = load_probe()
        tab_click = {"projectOpenObserved": True}
        result = probe.target_starter_gate_payload(Path("tab-click.json"), tab_click)
        self.assertIsNotNone(result)
        self.assertEqual("target-starter-metadata-missing", result["blocker"])

    def test_no_target_starter_without_project_open_returns_none(self) -> None:
        """If no targetStarter and no open, gate returns None (handled later)."""
        probe = load_probe()
        tab_click = {"projectOpenObserved": False}
        result = probe.target_starter_gate_payload(Path("tab-click.json"), tab_click)
        self.assertIsNone(result)

    def test_wrong_target_starter_returns_metadata_invalid(self) -> None:
        probe = load_probe()
        tab_click = {
            "targetStarter": {"displayName": "WrongName", "repositoryPath": "wrong/path"},
            "projectOpenObserved": True,
        }
        result = probe.target_starter_gate_payload(Path("tab-click.json"), tab_click)
        self.assertIsNotNone(result)
        self.assertEqual("target-starter-metadata-invalid", result["blocker"])

    def test_correct_target_but_not_opened_returns_not_proven(self) -> None:
        probe = load_probe()
        tab_click = {
            "targetStarter": {
                "displayName": "Africa Full",
                "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
            },
            "evidenceStatus": "selected",
            "projectOpenObserved": False,
        }
        result = probe.target_starter_gate_payload(Path("tab-click.json"), tab_click)
        self.assertIsNotNone(result)
        self.assertEqual("target-starter-open-not-proven", result["blocker"])

    def test_fully_valid_opened_returns_none(self) -> None:
        """Gate passes when all target-open evidence fields are satisfied."""
        probe = load_probe()
        result = probe.target_starter_gate_payload(Path("tab-click.json"), VALID_TAB_CLICK_OPENED)
        self.assertIsNone(result)


class TargetStarterObservedMatchesTest(unittest.TestCase):
    """Unit contract: target_starter_observed_matches requires name match."""

    def test_matching_observed(self) -> None:
        probe = load_probe()
        observed = {"name": "Africa Full", "role": "panel"}
        target = {"displayName": "Africa Full"}
        self.assertTrue(probe.target_starter_observed_matches(observed, target))

    def test_non_matching_observed(self) -> None:
        probe = load_probe()
        observed = {"name": "Wrong Name", "role": "panel"}
        target = {"displayName": "Africa Full"}
        self.assertFalse(probe.target_starter_observed_matches(observed, target))

    def test_none_observed(self) -> None:
        probe = load_probe()
        target = {"displayName": "Africa Full"}
        self.assertFalse(probe.target_starter_observed_matches(None, target))

    def test_non_dict_observed(self) -> None:
        probe = load_probe()
        target = {"displayName": "Africa Full"}
        self.assertFalse(probe.target_starter_observed_matches("string", target))


class StartersTabSafetyMatchesTest(unittest.TestCase):
    """Unit contract: starters_tab_safety_matches requires all safety fields."""

    def test_valid_safety(self) -> None:
        probe = load_probe()
        safety = {
            "tabName": "Starters",
            "activationAttempted": True,
            "activatedBeforeTargetSearch": True,
            "targetSearchScope": "active-starters-tab",
        }
        self.assertTrue(probe.starters_tab_safety_matches(safety))

    def test_wrong_tab_name(self) -> None:
        probe = load_probe()
        safety = {
            "tabName": "My Projects",
            "activationAttempted": True,
            "activatedBeforeTargetSearch": True,
            "targetSearchScope": "active-starters-tab",
        }
        self.assertFalse(probe.starters_tab_safety_matches(safety))

    def test_not_activated_before_search(self) -> None:
        probe = load_probe()
        safety = {
            "tabName": "Starters",
            "activationAttempted": True,
            "activatedBeforeTargetSearch": False,
            "targetSearchScope": "active-starters-tab",
        }
        self.assertFalse(probe.starters_tab_safety_matches(safety))

    def test_wrong_scope(self) -> None:
        probe = load_probe()
        safety = {
            "tabName": "Starters",
            "activationAttempted": True,
            "activatedBeforeTargetSearch": True,
            "targetSearchScope": "not-started",
        }
        self.assertFalse(probe.starters_tab_safety_matches(safety))

    def test_none_safety(self) -> None:
        probe = load_probe()
        self.assertFalse(probe.starters_tab_safety_matches(None))

    def test_non_dict_safety(self) -> None:
        probe = load_probe()
        self.assertFalse(probe.starters_tab_safety_matches("invalid"))


class BlockerPayloadFactoriesTest(unittest.TestCase):
    """Unit contract: blocker payload factories produce valid machine-readable payloads."""

    def test_project_not_opened_payload(self) -> None:
        probe = load_probe()
        result = probe.project_not_opened_payload(Path("tab-click.json"))
        self.assertEqual("blocked", result["status"])
        self.assertEqual("project-not-opened", result["blocker"])
        self.assertIn("tab-click.json", result["blockerDetail"])

    def test_no_java_pid_payload(self) -> None:
        probe = load_probe()
        result = probe.no_java_pid_payload(Path("inventory.json"))
        self.assertEqual("blocked", result["status"])
        self.assertEqual("alice-window-java-pid-not-identified", result["blocker"])
        self.assertIsNone(result["javaPid"])

    def test_blocked_payload_on_os_error(self) -> None:
        probe = load_probe()
        exc = OSError(2, "No such file or directory")
        result = probe.blocked_payload(Path("missing.json"), exc)
        self.assertEqual("blocked", result["status"])
        self.assertEqual("input-unreadable", result["blocker"])
        self.assertIn("missing.json", result["blockerDetail"])

    def test_blocked_payload_on_json_error(self) -> None:
        probe = load_probe()
        exc = json.JSONDecodeError("msg", "doc", 0)
        result = probe.blocked_payload(Path("bad.json"), exc)
        self.assertEqual("blocked", result["status"])
        self.assertEqual("input-unreadable", result["blocker"])

    def test_target_starter_metadata_missing_payload(self) -> None:
        probe = load_probe()
        result = probe.target_starter_metadata_missing_payload(Path("tab-click.json"))
        self.assertEqual("blocked", result["status"])
        self.assertEqual("target-starter-metadata-missing", result["blocker"])
        self.assertIn("expectedTargetStarter", result)

    def test_target_starter_metadata_invalid_payload(self) -> None:
        probe = load_probe()
        tab_click = {
            "targetStarter": {"displayName": "Wrong", "repositoryPath": "wrong/path"},
            "openedStarter": None,
            "evidenceStatus": "blocked",
        }
        result = probe.target_starter_metadata_invalid_payload(Path("tab.json"), tab_click)
        self.assertEqual("blocked", result["status"])
        self.assertEqual("target-starter-metadata-invalid", result["blocker"])
        self.assertIn("expectedTargetStarter", result)
        self.assertFalse(result["targetStarterMatchesExpected"])


class ReadJsonOrBlockedTest(unittest.TestCase):
    """Unit contract: read_json_or_blocked returns data or a structured blocker."""

    def test_valid_json_file(self) -> None:
        probe = load_probe()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            json.dump({"key": "value"}, f)
            f.flush()
            data, payload = probe.read_json_or_blocked(Path(f.name))
        self.assertIsNotNone(data)
        self.assertIsNone(payload)
        self.assertEqual("value", data["key"])

    def test_missing_file_returns_blocker(self) -> None:
        probe = load_probe()
        data, payload = probe.read_json_or_blocked(Path("/nonexistent/file.json"))
        self.assertIsNone(data)
        self.assertIsNotNone(payload)
        self.assertEqual("input-unreadable", payload["blocker"])

    def test_invalid_json_returns_blocker(self) -> None:
        probe = load_probe()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            f.write("not valid json {{{")
            f.flush()
            data, payload = probe.read_json_or_blocked(Path(f.name))
        self.assertIsNone(data)
        self.assertIsNotNone(payload)
        self.assertEqual("input-unreadable", payload["blocker"])


class PostOpenFramePayloadTest(unittest.TestCase):
    """Unit contract: post_open_frame_payload sets observed vs not-observed correctly."""

    def test_non_select_project_frame_is_observed(self) -> None:
        probe = load_probe()
        result = probe.post_open_frame_payload(
            java_pid=123,
            frame_names=["Alice 3", "Select Project"],
            frame_child_counts=[10, 5],
        )
        self.assertTrue(result["postOpenWindowObserved"])
        self.assertEqual("none", result["blocker"])
        self.assertEqual("observed", result["status"])

    def test_only_select_project_frame_is_not_observed(self) -> None:
        probe = load_probe()
        result = probe.post_open_frame_payload(
            java_pid=123,
            frame_names=["Select Project"],
            frame_child_counts=[5],
        )
        self.assertFalse(result["postOpenWindowObserved"])
        self.assertEqual("no-non-select-project-frame-visible", result["blocker"])
        self.assertEqual("not-observed", result["status"])

    def test_empty_frames_is_not_observed(self) -> None:
        probe = load_probe()
        result = probe.post_open_frame_payload(
            java_pid=123,
            frame_names=[],
            frame_child_counts=[],
        )
        self.assertFalse(result["postOpenWindowObserved"])


class PostOpenResultPayloadIntegrationTest(unittest.TestCase):
    """Integration contract: post_open_result_payload routes through gate correctly."""

    def test_project_not_opened_routes_to_blocker(self) -> None:
        probe = load_probe()
        tab_click = {"projectOpenObserved": False}
        result = probe.post_open_result_payload(
            VALID_INVENTORY,
            Path("inventory.json"),
            Path("tab-click.json"),
            tab_click,
        )
        self.assertEqual("blocked", result["status"])
        self.assertEqual("project-not-opened", result["blocker"])

    def test_gate_blocks_wrong_target_starter(self) -> None:
        probe = load_probe()
        tab_click = {
            "targetStarter": {"displayName": "Wrong", "repositoryPath": "wrong"},
            "projectOpenObserved": True,
        }
        result = probe.post_open_result_payload(
            VALID_INVENTORY,
            Path("inventory.json"),
            Path("tab-click.json"),
            tab_click,
        )
        self.assertEqual("blocked", result["status"])
        self.assertEqual("target-starter-metadata-invalid", result["blocker"])

    def test_no_java_pid_returns_pid_blocker(self) -> None:
        probe = load_probe()
        empty_inventory = {"windows": []}
        result = probe.post_open_result_payload(
            empty_inventory,
            Path("inventory.json"),
            Path("tab-click.json"),
            VALID_TAB_CLICK_OPENED,
        )
        self.assertEqual("blocked", result["status"])
        self.assertEqual("alice-window-java-pid-not-identified", result["blocker"])


class WritePayloadTest(unittest.TestCase):
    """Unit contract: write_payload produces valid sorted JSON."""

    def test_writes_sorted_json(self) -> None:
        probe = load_probe()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            output_path = Path(f.name)
        payload = {"z_key": 1, "a_key": 2}
        probe.write_payload(output_path, payload)
        content = output_path.read_text(encoding="utf-8")
        decoded = json.loads(content)
        self.assertEqual({"a_key": 2, "z_key": 1}, decoded)
        keys = list(json.loads(content).keys())
        self.assertEqual(sorted(keys), keys, "JSON keys should be sorted")
        self.assertTrue(content.endswith("\n"), "Output should end with newline")


if __name__ == "__main__":
    unittest.main()
