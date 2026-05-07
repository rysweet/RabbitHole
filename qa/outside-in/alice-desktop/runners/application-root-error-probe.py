#!/usr/bin/env python3
"""Classify the Java "Application Root Error" dialog seen before Alice desktop."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


ROOT_PROPERTY = "org.alice.ide.rootDirectory"
ERROR_TITLE = "Application Root Error"
APPLICATION_NAME = "Alice"


def read_cmdline(proc_root: Path, pid: int | None) -> tuple[str, list[str]]:
    if pid is None:
        return "missing-pid", []
    try:
        data = (proc_root / str(pid) / "cmdline").read_bytes()
    except OSError:
        return "unavailable", []
    values = [part.decode("utf-8", "replace") for part in data.split(b"\0") if part]
    return "available", values


def window_pid(window: dict[str, Any]) -> int | None:
    value = window.get("pid")
    if isinstance(value, int):
        return value
    if isinstance(value, str) and value.isdigit():
        return int(value)
    return None


def read_process_cwd(proc_root: Path, pid: int | None) -> str:
    if pid is None:
        return ""
    try:
        return str((proc_root / str(pid) / "cwd").resolve(strict=True))
    except OSError:
        return ""


def root_property_value(argv: list[str]) -> str:
    prefix = f"-D{ROOT_PROPERTY}="
    for arg in argv:
        if arg.startswith(prefix):
            return arg[len(prefix) :]
    return ""


def resolve_configured_root(configured: str, process_cwd: str) -> str:
    if not configured:
        return ""
    configured_path = Path(configured)
    if configured_path.is_absolute():
        return str(configured_path)
    if process_cwd:
        return str((Path(process_cwd) / configured_path).resolve(strict=False))
    return str(configured_path)


def maven_exec_next_change(process_cwd: str, cmdline: list[str]) -> str:
    if "exec:java" not in cmdline:
        return ""
    if Path(process_cwd).name != "alice-ide":
        return ""
    return (
        f"For this Maven exec:java launch path, add -D{ROOT_PROPERTY}=../core/resources/target/distribution "
        "to the Maven argv before EntryPoint starts, and ensure that directory exists by preparing the "
        "core/resources distribution first."
    )


def classify(
    configured_root: str, resolved_root: str, process_cwd: str, cmdline: list[str]
) -> tuple[str, str, str, bool]:
    if not configured_root:
        expected_text = (
            f"system property: {ROOT_PROPERTY} is not set.\n"
            f"{APPLICATION_NAME} will not work until this is addressed."
        )
        next_change = maven_exec_next_change(process_cwd, cmdline) or (
            f"Pass -D{ROOT_PROPERTY}=<existing Alice distribution root> to the Alice JVM "
            "before launching EntryPoint."
        )
        return "application-root-property-not-set", expected_text, next_change, False

    root_exists = Path(resolved_root).exists() if resolved_root else Path(configured_root).exists()
    if root_exists:
        return (
            "application-root-error-window-root-condition-unconfirmed",
            "",
            "The Application Root Error window was observed, but the configured root directory exists now; rerun with a fresh process and preserve launch.log plus application-root-error.json.",
            True,
        )

    expected_text = (
        f"system property: {ROOT_PROPERTY} is incorrectly set.\n"
        f"{configured_root} does not exist.\n"
        f"{APPLICATION_NAME} will not work until this is addressed."
    )
    next_change = (
        f"Ensure {ROOT_PROPERTY} points to an existing Alice distribution root before launching EntryPoint; "
        f"create {configured_root} or pass -D{ROOT_PROPERTY}=<existing distribution>."
    )
    return "application-root-directory-missing", expected_text, next_change, False


def find_error_window(inventory: dict[str, Any]) -> dict[str, Any] | None:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None

    title_matches = [
        window
        for window in windows
        if isinstance(window, dict) and window.get("title") == ERROR_TITLE
    ]
    for window in title_matches:
        process_name = str(window.get("processName", "")).lower()
        if process_name == "java":
            return window
    return title_matches[0] if title_matches else None


def not_observed_payload(inventory_path: Path) -> dict[str, Any]:
    return {
        "status": "not-observed",
        "blocker": "application-root-error-window-not-found",
        "blockerDetail": (
            f"No visible Java window titled {ERROR_TITLE!r} was present in "
            f"{inventory_path.name}; this probe will not infer dialog text without that exact window."
        ),
        "errorTitle": "",
        "expectedDialogText": "",
        "configuredRootDirectory": "",
        "resolvedRootDirectory": "",
        "rootDirectoryExists": "",
        "nextInvocationChange": "",
        "window": {},
        "processCommandLineStatus": "not-attempted",
        "processCommandLine": [],
    }


def observed_payload(window: dict[str, Any], proc_root: Path) -> dict[str, Any]:
    pid = window_pid(window)
    cmdline_status, cmdline = read_cmdline(proc_root, pid)
    process_cwd = read_process_cwd(proc_root, pid)
    configured_root = root_property_value(cmdline)
    resolved_root = resolve_configured_root(configured_root, process_cwd)
    blocker, expected_text, next_change, root_exists = classify(
        configured_root, resolved_root, process_cwd, cmdline
    )
    return {
        "status": "observed",
        "blocker": blocker,
        "blockerDetail": (
            "A Java window titled 'Application Root Error' was observed before any Alice "
            "desktop candidate; the expected dialog text is derived from "
            "edu.cmu.cs.dennisc.app.ApplicationRoot for the observed JVM rootDirectory condition."
        ),
        "errorTitle": ERROR_TITLE,
        "expectedDialogText": expected_text,
        "configuredRootDirectory": configured_root,
        "resolvedRootDirectory": resolved_root,
        "rootDirectoryExists": root_exists,
        "nextInvocationChange": next_change,
        "window": {
            "id": window.get("id", ""),
            "title": window.get("title", ""),
            "class": window.get("class", ""),
            "pid": pid,
            "processName": window.get("processName", ""),
            "geometry": window.get("geometry", {}),
        },
        "processCommandLineStatus": cmdline_status,
        "processCommandLine": cmdline,
        "processCwd": process_cwd,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("inventory")
    parser.add_argument("output")
    parser.add_argument("--proc-root", default="/proc")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    output_path = Path(args.output)
    proc_root = Path(args.proc_root)

    try:
        inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        payload = {
            "status": "blocked",
            "blocker": "window-inventory-unreadable",
            "blockerDetail": f"Could not read {inventory_path}: {exc}",
            "errorTitle": "",
            "expectedDialogText": "",
            "configuredRootDirectory": "",
            "resolvedRootDirectory": "",
            "rootDirectoryExists": "",
            "nextInvocationChange": "",
            "window": {},
            "processCommandLineStatus": "not-attempted",
            "processCommandLine": [],
        }
    else:
        window = find_error_window(inventory)
        payload = observed_payload(window, proc_root) if window else not_observed_payload(inventory_path)

    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
