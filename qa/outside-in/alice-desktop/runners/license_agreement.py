#!/usr/bin/env python3
"""Shared first-run license agreement facts for Alice desktop QA probes."""

from __future__ import annotations

from typing import Any


PREFERENCE_KEY = "isLicenseAccepted"
JAVA_PREFERENCES_PROPERTY = "java.util.prefs.userRoot"
JAVA_USER_PREFERENCES_DIRECTORY = ".java/.userPrefs"
HEADER_TEXT = "Please read the following license agreement carefully."
EXPECTED_CONTROLS = [
    "I accept the terms in the License Agreement",
    "I do not accept the terms in the License Agreement",
    "OK",
    "Cancel",
]

LICENSE_AGREEMENTS: list[dict[str, Any]] = [
    {
        "id": "alice-3",
        "name": "Alice",
        "title": "License Agreement (Part 1 of 2): Alice 3",
        "preferencesClass": "org.lgna.project.License",
        "preferencesPackagePath": "org/lgna/project",
        "preferencesKey": PREFERENCE_KEY,
        "alternateTitles": [],
    },
    {
        "id": "sims-art-assets",
        "name": "The Sims (TM) 2 Art Assets",
        "title": "License Agreement (Part 2 of 2): The Sims (TM) 2 Art Assets",
        "preferencesClass": "edu.cmu.cs.dennisc.nebulous.License",
        "preferencesPackagePath": "edu/cmu/cs/dennisc/nebulous",
        "preferencesKey": PREFERENCE_KEY,
        "alternateTitles": ["License Agreement: The Sims (TM) 2 Art Assets"],
    },
]


def agreement_for_title(title: str) -> dict[str, Any] | None:
    for agreement in LICENSE_AGREEMENTS:
        titles = [agreement["title"], *agreement["alternateTitles"]]
        if title in titles:
            return agreement
    return None


def agreement_payload(agreement: dict[str, Any], user_root: str = "") -> dict[str, Any]:
    preference_file = f"{JAVA_USER_PREFERENCES_DIRECTORY}/{agreement['preferencesPackagePath']}/prefs.xml"
    payload = {
        "id": agreement["id"],
        "name": agreement["name"],
        "title": agreement["title"],
        "alternateTitles": agreement["alternateTitles"],
        "preferencesClass": agreement["preferencesClass"],
        "preferencesPackagePath": agreement["preferencesPackagePath"],
        "preferencesKey": agreement["preferencesKey"],
        "preferenceFile": preference_file,
    }
    if user_root:
        payload["preferenceFilePath"] = f"{user_root.rstrip('/')}/{preference_file}"
    return payload
