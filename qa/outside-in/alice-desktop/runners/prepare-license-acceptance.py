#!/usr/bin/env python3
"""Create test-only Java Preferences state that accepts first-run Alice licenses."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any
from xml.sax.saxutils import escape

from license_agreement import (
    JAVA_PREFERENCES_PROPERTY,
    JAVA_USER_PREFERENCES_DIRECTORY,
    LICENSE_AGREEMENTS,
    PREFERENCE_KEY,
    agreement_payload,
)


def write_payload(output_path: Path, payload: dict[str, Any]) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def preferences_xml(key: str, value: str) -> str:
    return "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8" standalone="no"?>',
            '<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">',
            '<map MAP_XML_VERSION="1.0">',
            f'  <entry key="{escape(key)}" value="{escape(value)}"/>',
            "</map>",
            "",
        ]
    )


def empty_preferences_xml() -> str:
    return "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8" standalone="no"?>',
            '<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">',
            '<map MAP_XML_VERSION="1.0"/>',
            "",
        ]
    )


def base_payload(user_root: Path) -> dict[str, Any]:
    user_root_value = str(user_root)
    return {
        "status": "",
        "blocker": "",
        "blockerDetail": "",
        "testOnly": True,
        JAVA_PREFERENCES_PROPERTY: user_root_value,
        "javaPreferencesDirectory": f"{user_root_value}/{JAVA_USER_PREFERENCES_DIRECTORY}",
        "jvmOption": f"-D{JAVA_PREFERENCES_PROPERTY}={user_root_value}",
        "launchEnvironmentVariable": "JAVA_TOOL_OPTIONS",
        "preferencesKey": PREFERENCE_KEY,
        "licenses": [agreement_payload(agreement, user_root_value) for agreement in LICENSE_AGREEMENTS],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--user-root", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument(
        "--accept-for-tests",
        action="store_true",
        help="Explicit opt-in required before writing acceptance state.",
    )
    args = parser.parse_args()

    user_root = Path(args.user_root).resolve(strict=False)
    output_path = Path(args.output)
    payload = base_payload(user_root)

    if not args.accept_for_tests:
        payload.update(
            {
                "status": "blocked",
                "blocker": "license-test-acceptance-not-enabled",
                "blockerDetail": (
                    "Refusing to write Alice first-run license acceptance preferences without "
                    "--accept-for-tests. This bypass is for controlled QA launch proof only."
                ),
                "writtenPreferenceFiles": [],
            }
        )
        write_payload(output_path, payload)
        return 2

    written_files: list[str] = []
    for agreement in LICENSE_AGREEMENTS:
        preferences_dir = user_root / JAVA_USER_PREFERENCES_DIRECTORY
        preference_dir = preferences_dir / agreement["preferencesPackagePath"]
        preference_dir.mkdir(parents=True, exist_ok=True)
        package_parts = Path(agreement["preferencesPackagePath"]).parts
        for index in range(1, len(package_parts)):
            parent_file = preferences_dir.joinpath(*package_parts[:index]) / "prefs.xml"
            if not parent_file.exists():
                parent_file.write_text(empty_preferences_xml(), encoding="utf-8")
        preference_file = preference_dir / "prefs.xml"
        preference_file.write_text(preferences_xml(PREFERENCE_KEY, "true"), encoding="utf-8")
        written_files.append(str(preference_file))

    payload.update(
        {
            "status": "prepared",
            "blocker": "none",
            "blockerDetail": (
                "Prepared a dedicated Java Preferences user root with accepted Alice first-run "
                "license keys. Launch Alice with the recorded jvmOption to keep this state "
                "isolated from real user preferences."
            ),
            "writtenPreferenceFiles": written_files,
        }
    )
    write_payload(output_path, payload)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
