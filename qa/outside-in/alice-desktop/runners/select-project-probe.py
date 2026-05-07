#!/usr/bin/env python3
"""Classify the Alice Select Project window from an X window inventory."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

SELECT_PROJECT_TITLE = "Select Project"
EXPECTED_TAB_LABELS = ["Blank Slates", "Starters", "My Projects", "Recent", "File System"]
EXPECTED_SIDEKICK_LABELS = ["Open for VR"]
EXPECTED_WIDGET_LABELS = EXPECTED_TAB_LABELS + EXPECTED_SIDEKICK_LABELS


def window_pid(window: dict[str, Any]) -> int | None:
    value = window.get("pid")
    if isinstance(value, int):
        return value
    if isinstance(value, str) and value.isdigit():
        return int(value)
    return None


def find_select_project_window(inventory: dict[str, Any]) -> dict[str, Any] | None:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None
    matches = [
        window
        for window in windows
        if isinstance(window, dict) and str(window.get("title", "")) == SELECT_PROJECT_TITLE
    ]
    for window in matches:
        if str(window.get("processName", "")).lower() == "java":
            return window
    return None


def window_payload(window: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": window.get("id", ""),
        "title": window.get("title", ""),
        "class": window.get("class", ""),
        "pid": window_pid(window),
        "processName": window.get("processName", ""),
        "geometry": window.get("geometry", {}),
    }


def non_java_title_present(inventory: dict[str, Any]) -> bool:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return False
    return any(
        isinstance(window, dict)
        and str(window.get("title", "")) == SELECT_PROJECT_TITLE
        and str(window.get("processName", "")).lower() != "java"
        for window in windows
    )


def not_observed_payload(inventory_path: Path, inventory: dict[str, Any]) -> dict[str, Any]:
    blocker = "select-project-window-not-found"
    detail = (
        f"No visible Java window titled '{SELECT_PROJECT_TITLE}' was present in "
        f"{inventory_path.name}; this probe will not infer Select Project controls "
        "without the exact Java window title."
    )
    if non_java_title_present(inventory):
        blocker = "select-project-window-not-java"
        detail = (
            f"A visible window titled '{SELECT_PROJECT_TITLE}' was present in "
            f"{inventory_path.name}, but it did not belong to the Java Alice process; "
            "this probe will not count non-Alice windows as project interaction."
        )
    return {
        "status": "not-observed",
        "blocker": blocker,
        "blockerDetail": detail,
        "interactionProof": "not-proven",
        "projectWorldInteraction": "not-observed",
        "dialogTitle": "",
        "expectedWindowTitle": SELECT_PROJECT_TITLE,
        "expectedWidgetLabels": [],
        "widgetObservationStatus": "not-attempted",
        "widgetObservationBlocker": blocker,
        "window": {},
    }


def observed_payload(window: dict[str, Any]) -> dict[str, Any]:
    return {
        "status": "observed",
        "blocker": "none",
        "blockerDetail": "",
        "interactionProof": "select-project-window-visible",
        "projectWorldInteraction": "not-observed",
        "projectWorldInteractionDetail": (
            "Only the Select Project chooser window was observed. No starter/project was "
            "selected, no project was opened, and no world execution was attempted."
        ),
        "dialogTitle": SELECT_PROJECT_TITLE,
        "expectedWindowTitle": SELECT_PROJECT_TITLE,
        "expectedWidgetLabels": EXPECTED_WIDGET_LABELS,
        "widgetObservationStatus": "resource-contract-only",
        "widgetObservationBlocker": "swing-widget-inventory-not-collected",
        "widgetObservationDetail": (
            "The X window inventory proves title/class/process/geometry only. Swing child "
            "widgets are listed from the checked-in SelectProjectUriComposite resource "
            "contract; a future accessibility/Jemmy probe is needed to observe live widgets."
        ),
        "resourceContract": {
            "labelsFile": "core/ide/src/main/resources/org/alice/ide/projecturi/croquet.properties",
            "compositeClass": "org.alice.ide.projecturi.SelectProjectUriComposite",
            "tabStateOrder": EXPECTED_TAB_LABELS,
            "sidekickLabels": EXPECTED_SIDEKICK_LABELS,
        },
        "window": window_payload(window),
    }


def blocked_payload(inventory_path: Path, exc: Exception) -> dict[str, Any]:
    return {
        "status": "blocked",
        "blocker": "window-inventory-unreadable",
        "blockerDetail": f"Could not read {inventory_path}: {exc}",
        "interactionProof": "not-proven",
        "projectWorldInteraction": "not-observed",
        "dialogTitle": "",
        "expectedWindowTitle": SELECT_PROJECT_TITLE,
        "expectedWidgetLabels": [],
        "widgetObservationStatus": "not-attempted",
        "widgetObservationBlocker": "window-inventory-unreadable",
        "window": {},
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("inventory")
    parser.add_argument("output")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    output_path = Path(args.output)
    try:
        inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        payload = blocked_payload(inventory_path, exc)
    else:
        match = find_select_project_window(inventory)
        payload = observed_payload(match) if match else not_observed_payload(inventory_path, inventory)

    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
