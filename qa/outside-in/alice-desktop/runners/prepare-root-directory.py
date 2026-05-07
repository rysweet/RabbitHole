#!/usr/bin/env python3
"""Prepare and verify Alice's org.alice.ide.rootDirectory launch prerequisite."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any


ROOT_PROPERTY = "org.alice.ide.rootDirectory"
EXPECTED_ROOT = "../core/resources/target/distribution"
MAVEN_PHASE = "process-resources"
MAVEN_PROJECT = "core/resources"
PREP_COMMAND = [
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-Dcheckstyle.skip",
    "-DskipTests",
    "-pl",
    MAVEN_PROJECT,
    MAVEN_PHASE,
]


def write_payload(output_path: Path, payload: dict[str, Any]) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def base_payload(repo_root: Path, alice_cwd: str, log_path: Path) -> dict[str, Any]:
    return {
        "rootDirectoryProperty": ROOT_PROPERTY,
        "expectedRootDirectory": EXPECTED_ROOT,
        "configuredRootDirectory": "",
        "resolvedRootDirectory": "",
        "distributionExists": False,
        "mavenProject": MAVEN_PROJECT,
        "mavenPhase": MAVEN_PHASE,
        "prepCommand": PREP_COMMAND,
        "prepAttempted": False,
        "prepExitCode": "",
        "prepLog": str(log_path),
        "repoRoot": str(repo_root),
        "aliceCwd": alice_cwd,
    }


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child_text(element: ET.Element, name: str) -> str:
    for child in element:
        if local_name(child.tag) == name and child.text:
            return child.text.strip()
    return ""


def configured_root_directory(pom_path: Path) -> str:
    try:
        tree = ET.parse(pom_path)
    except (OSError, ET.ParseError):
        return ""

    for element in tree.iter():
        if local_name(element.tag) != "systemProperty":
            continue
        if child_text(element, "key") == ROOT_PROPERTY:
            return child_text(element, "value")
    return ""


def resolve_root(repo_root: Path, alice_cwd: str, configured: str) -> Path:
    configured_path = Path(configured)
    if configured_path.is_absolute():
        return configured_path
    return (repo_root / alice_cwd / configured_path).resolve(strict=False)


def blocked(
    payload: dict[str, Any],
    blocker: str,
    detail: str,
    output_path: Path,
) -> int:
    payload.update({"status": "blocked", "blocker": blocker, "blockerDetail": detail})
    write_payload(output_path, payload)
    return 2


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", default=".")
    parser.add_argument("--alice-cwd", default="alice-ide")
    parser.add_argument("--output", required=True)
    parser.add_argument("--log", required=True)
    parser.add_argument(
        "--no-execute",
        action="store_true",
        help="Only report the missing distribution and required Maven phase; do not run Maven.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve(strict=True)
    alice_cwd = args.alice_cwd
    output_path = Path(args.output)
    log_path = Path(args.log)
    payload = base_payload(repo_root, alice_cwd, log_path)

    pom_path = repo_root / alice_cwd / "pom.xml"
    configured = configured_root_directory(pom_path)
    payload["configuredRootDirectory"] = configured

    if configured != EXPECTED_ROOT:
        return blocked(
            payload,
            "root-directory-property-missing",
            (
                f"{pom_path.relative_to(repo_root)} does not configure exec:java system property "
                f"{ROOT_PROPERTY}={EXPECTED_ROOT}; configure that property before launching EntryPoint."
            ),
            output_path,
        )

    resolved = resolve_root(repo_root, alice_cwd, configured)
    payload["resolvedRootDirectory"] = str(resolved)
    payload["distributionExists"] = resolved.exists()

    if resolved.exists():
        payload.update(
            {
                "status": "ready",
                "blocker": "none",
                "blockerDetail": (
                    f"{ROOT_PROPERTY} points to an existing Alice distribution root."
                ),
            }
        )
        write_payload(output_path, payload)
        return 0

    if args.no_execute:
        return blocked(
            payload,
            "core-resources-distribution-missing",
            (
                f"{ROOT_PROPERTY} points to {configured}, resolved as {resolved}, but that "
                f"directory does not exist. Run Maven phase {MAVEN_PHASE} for {MAVEN_PROJECT} "
                "before launching EntryPoint."
            ),
            output_path,
        )

    log_path.parent.mkdir(parents=True, exist_ok=True)
    payload["prepAttempted"] = True
    with log_path.open("w", encoding="utf-8") as stream:
        completed = subprocess.run(
            PREP_COMMAND,
            cwd=repo_root,
            stdout=stream,
            stderr=subprocess.STDOUT,
            check=False,
            text=True,
        )
    payload["prepExitCode"] = completed.returncode
    payload["distributionExists"] = resolved.exists()

    if completed.returncode != 0:
        return blocked(
            payload,
            "core-resources-distribution-prep-failed",
            (
                f"{' '.join(PREP_COMMAND)} failed with exit code {completed.returncode}; "
                f"inspect {log_path.name} for the exact Maven failure."
            ),
            output_path,
        )
    if not resolved.exists():
        return blocked(
            payload,
            "core-resources-distribution-not-created",
            (
                f"{' '.join(PREP_COMMAND)} completed, but {resolved} still does not exist."
            ),
            output_path,
        )

    payload.update(
        {
            "status": "prepared",
            "blocker": "none",
            "blockerDetail": (
                f"Prepared {resolved} with Maven phase {MAVEN_PHASE} for {MAVEN_PROJECT}."
            ),
        }
    )
    write_payload(output_path, payload)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
