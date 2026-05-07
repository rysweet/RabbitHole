#!/usr/bin/env python3
"""Classify first-run License Agreement dialogs from an X window inventory."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from license_agreement import (
    EXPECTED_CONTROLS,
    HEADER_TEXT,
    JAVA_PREFERENCES_PROPERTY,
    LICENSE_AGREEMENTS,
    agreement_for_title,
    agreement_payload,
)


def window_pid(window: dict[str, Any]) -> int | None:
    value = window.get("pid")
    if isinstance(value, int):
        return value
    if isinstance(value, str) and value.isdigit():
        return int(value)
    return None


def find_license_window(inventory: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]] | None:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None
    matches: list[tuple[dict[str, Any], dict[str, Any]]] = []
    for window in windows:
        if not isinstance(window, dict):
            continue
        agreement = agreement_for_title(str(window.get("title", "")))
        if agreement:
            matches.append((window, agreement))
    for window, agreement in matches:
        if str(window.get("processName", "")).lower() == "java":
            return window, agreement
    return matches[0] if matches else None


def not_observed_payload(inventory_path: Path) -> dict[str, Any]:
    return {
        "status": "not-observed",
        "blocker": "license-agreement-window-not-found",
        "blockerDetail": (
            f"No visible Java window titled as a known Alice first-run license agreement was "
            f"present in {inventory_path.name}; this probe will not infer controls without an exact title match."
        ),
        "dialogTitle": "",
        "expectedHeaderText": "",
        "expectedControls": [],
        "matchedLicense": {},
        "knownLicenses": [agreement_payload(agreement) for agreement in LICENSE_AGREEMENTS],
        "safeTestBypass": {
            "property": JAVA_PREFERENCES_PROPERTY,
            "requiredOptIn": "--accept-for-tests",
            "runner": "prepare-license-acceptance.py",
        },
        "window": {},
    }


def observed_payload(window: dict[str, Any], agreement: dict[str, Any]) -> dict[str, Any]:
    return {
        "status": "observed",
        "blocker": "first-run-license-agreement-visible",
        "blockerDetail": (
            "A first-run Alice License Agreement window is visible. It blocks project interaction "
            "until accepted. For controlled QA only, prepare an isolated Java Preferences user root "
            "with prepare-license-acceptance.py --accept-for-tests and launch with the recorded "
            f"-D{JAVA_PREFERENCES_PROPERTY}=... JVM option."
        ),
        "dialogTitle": window.get("title", ""),
        "expectedHeaderText": HEADER_TEXT,
        "expectedControls": EXPECTED_CONTROLS,
        "matchedLicense": agreement_payload(agreement),
        "safeTestBypass": {
            "property": JAVA_PREFERENCES_PROPERTY,
            "requiredOptIn": "--accept-for-tests",
            "runner": "prepare-license-acceptance.py",
        },
        "window": {
            "id": window.get("id", ""),
            "title": window.get("title", ""),
            "class": window.get("class", ""),
            "pid": window_pid(window),
            "processName": window.get("processName", ""),
            "geometry": window.get("geometry", {}),
        },
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
        payload = {
            "status": "blocked",
            "blocker": "window-inventory-unreadable",
            "blockerDetail": f"Could not read {inventory_path}: {exc}",
            "dialogTitle": "",
            "expectedHeaderText": "",
            "expectedControls": [],
            "matchedLicense": {},
            "window": {},
        }
    else:
        match = find_license_window(inventory)
        payload = observed_payload(*match) if match else not_observed_payload(inventory_path)

    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
