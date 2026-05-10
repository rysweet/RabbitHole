#!/usr/bin/env python3
"""Probe the Alice 3 main window AT-SPI state after Select Project is dismissed.

Reads x-window-inventory.json (to recover the Java PID) and
tab-click-observation.json (to confirm projectOpenObserved=true).  If the
project-open step was not observed, records an explicit blocker instead of
connecting to AT-SPI.

When projectOpenObserved=true this probe connects to the AT-SPI registry,
finds the Alice application by PID, and enumerates all top-level frame
children to characterise the IDE window state after project load.  It
records:

  postOpenWindowObserved (bool)
      true  – the alice_app is still accessible in AT-SPI and exposes at
              least one top-level frame whose name is NOT "Select Project".
  mainFrameNames (list[str])
      Names of the top-level AT-SPI frames/windows visible after project open.
  mainFrameChildCounts (list[int])
      childCount for each frame in mainFrameNames (shallow widget-presence
      signal without a deep tree walk).
  mainWindowObservationBlocker (str)
      "none" when postOpenWindowObserved=true, otherwise the machine-readable
      reason this step could not be proved.

Outputs post-project-open-observation.json.

Requires:
  - python3-pyatspi installed (sudo apt-get install -y python3-pyatspi)
  - Alice launched with exec:exec@alice-ide-atk (NO_AT_BRIDGE=1,
    -Xbootclasspath/a:/usr/share/java/java-atk-wrapper.jar)
  - tab-click-probe.py to have produced a tab-click-observation.json with
    projectOpenObserved=true.
"""

from __future__ import annotations

import argparse
import json
import os
import time
from pathlib import Path
from typing import Any

EXPECTED_SELECT_PROJECT_TITLE = "Select Project"
EXPECTED_ALICE_TITLE = "Alice 3"
EXPECTED_TARGET_STARTER = {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
}


def float_from_env(name: str, default: float, *, allow_zero: bool) -> float:
    value = os.environ.get(name)
    if value is None:
        return default
    try:
        parsed = float(value)
    except ValueError as exc:
        raise ValueError(f"{name} must be a number, got {value!r}") from exc
    if parsed < 0 or (parsed == 0 and not allow_zero):
        requirement = "non-negative" if allow_zero else "greater than 0"
        raise ValueError(f"{name} must be {requirement}, got {value!r}")
    return parsed


def int_from_env(name: str, default: int, *, min_value: int) -> int:
    value = os.environ.get(name)
    if value is None:
        return default
    try:
        parsed = int(value)
    except ValueError as exc:
        raise ValueError(f"{name} must be an integer, got {value!r}") from exc
    if parsed < min_value:
        raise ValueError(f"{name} must be at least {min_value}, got {value!r}")
    return parsed


POST_OPEN_WAIT_SECONDS = float_from_env(
    "ALICE_QA_POST_OPEN_PROBE_WAIT_SECONDS",
    5.0,
    allow_zero=True,
)
POST_OPEN_POLL_SECONDS = float_from_env(
    "ALICE_QA_POST_OPEN_PROBE_POLL_SECONDS",
    0.5,
    allow_zero=False,
)
ALICE_APP_FIND_ATTEMPTS = int_from_env(
    "ALICE_QA_POST_OPEN_PROBE_FIND_ATTEMPTS",
    5,
    min_value=1,
)
ALICE_APP_FIND_INTERVAL_SECONDS = float_from_env(
    "ALICE_QA_POST_OPEN_PROBE_FIND_INTERVAL_SECONDS",
    2.0,
    allow_zero=True,
)


def post_open_payload(
    *,
    status: str,
    blocker: str,
    blocker_detail: str,
    java_pid: int | None,
    post_open_observed: bool = False,
    frame_names: list[str] | None = None,
    frame_child_counts: list[int] | None = None,
    extra: dict[str, Any] | None = None,
) -> dict[str, Any]:
    payload = {
        "status": status,
        "blocker": blocker,
        "blockerDetail": blocker_detail,
        "javaPid": java_pid,
        "postOpenWindowObserved": post_open_observed,
        "mainFrameNames": frame_names or [],
        "mainFrameChildCounts": frame_child_counts or [],
        "mainWindowObservationBlocker": blocker,
    }
    if extra:
        payload.update(extra)
    return payload


def safe_child_count(node: Any) -> int:
    try:
        return int(node.childCount)
    except Exception:
        return 0


def safe_node_name(node: Any) -> str:
    try:
        return node.name or ""
    except Exception:
        return ""


def find_java_pid(inventory: dict[str, Any]) -> int | None:
    """Return the Java PID for the Alice 3 main window, if positively identified."""
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None
    for window in windows:
        if not isinstance(window, dict):
            continue
        pid = window.get("pid")
        if not isinstance(pid, int) or pid <= 0:
            continue
        if str(window.get("processName", "")).lower() != "java":
            continue
        if str(window.get("title", "")) == EXPECTED_ALICE_TITLE:
            return pid
    return None


def find_alice_app(desktop: Any, java_pid: int) -> tuple[Any | None, int]:
    app_count = 0
    for attempt in range(ALICE_APP_FIND_ATTEMPTS):
        try:
            app_count = desktop.childCount
        except Exception:
            app_count = 0
        for index in range(app_count):
            try:
                app = desktop.getChildAtIndex(index)
            except Exception:
                continue
            if app is None:
                continue
            try:
                app_pid = app.get_process_id()
            except Exception:
                app_pid = None
            if app_pid == java_pid and safe_child_count(app) > 0:
                return app, app_count
        if attempt < ALICE_APP_FIND_ATTEMPTS - 1:
            time.sleep(ALICE_APP_FIND_INTERVAL_SECONDS)
    return None, app_count


def top_level_frame_state(alice_app: Any) -> tuple[list[str], list[int]]:
    frame_names: list[str] = []
    frame_child_counts: list[int] = []
    for index in range(min(safe_child_count(alice_app), 20)):
        try:
            child = alice_app.getChildAtIndex(index)
        except Exception:
            continue
        if child is None:
            continue
        frame_names.append(safe_node_name(child))
        frame_child_counts.append(safe_child_count(child))
    return frame_names, frame_child_counts


def wait_for_post_open_frame_state(alice_app: Any) -> tuple[list[str], list[int]]:
    deadline = time.monotonic() + POST_OPEN_WAIT_SECONDS
    while True:
        frame_names, frame_child_counts = top_level_frame_state(alice_app)
        if any(name != EXPECTED_SELECT_PROJECT_TITLE for name in frame_names):
            return frame_names, frame_child_counts

        remaining = deadline - time.monotonic()
        if remaining <= 0:
            return frame_names, frame_child_counts
        time.sleep(min(POST_OPEN_POLL_SECONDS, remaining))


def atspi_desktop_or_payload(java_pid: int) -> tuple[Any | None, dict[str, Any] | None]:
    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return None, post_open_payload(
            status="blocked",
            blocker="pyatspi-not-installed",
            blocker_detail="python3-pyatspi is not installed.",
            java_pid=java_pid,
        )

    try:
        desktop = pyatspi.Registry.getDesktop(0)
    except Exception as exc:
        return None, post_open_payload(
            status="blocked",
            blocker="at-spi-registry-unavailable",
            blocker_detail=f"Cannot connect to AT-SPI registry: {exc}",
            java_pid=java_pid,
        )
    return desktop, None


def post_open_frame_payload(
    *,
    java_pid: int,
    frame_names: list[str],
    frame_child_counts: list[int],
) -> dict[str, Any]:
    post_open_observed = any(name != EXPECTED_SELECT_PROJECT_TITLE for name in frame_names)
    blocker = "none" if post_open_observed else "no-non-select-project-frame-visible"
    blocker_detail = ""
    if not post_open_observed:
        blocker_detail = (
            "After project-open wait, no top-level AT-SPI frame other than "
            f"'Select Project' is visible. Frames seen: {frame_names}"
        )
    return post_open_payload(
        status="observed" if post_open_observed else "not-observed",
        blocker=blocker,
        blocker_detail=blocker_detail,
        java_pid=java_pid,
        post_open_observed=post_open_observed,
        frame_names=frame_names,
        frame_child_counts=frame_child_counts,
    )


def probe_post_open(java_pid: int) -> dict[str, Any]:
    """Connect to AT-SPI and enumerate alice_app frames after project open."""
    desktop, payload = atspi_desktop_or_payload(java_pid)
    if payload is not None:
        return payload

    alice_app, app_count = find_alice_app(desktop, java_pid)
    if alice_app is None:
        return post_open_payload(
            status="blocked",
            blocker="atk-wrapper-not-loaded",
            blocker_detail=(
                f"Java process PID {java_pid} not found in AT-SPI registry "
                f"({app_count} total AT-SPI apps visible)."
            ),
            java_pid=java_pid,
        )

    frame_names, frame_child_counts = wait_for_post_open_frame_state(alice_app)
    return post_open_frame_payload(
        java_pid=java_pid,
        frame_names=frame_names,
        frame_child_counts=frame_child_counts,
    )


def blocked_payload(path: Path, exc: Exception) -> dict[str, Any]:
    reason = exc.strerror if isinstance(exc, OSError) and exc.strerror else str(exc)
    return post_open_payload(
        status="blocked",
        blocker="input-unreadable",
        blocker_detail=f"Could not read {path.name}: {reason}",
        java_pid=None,
    )


def project_not_opened_payload(tab_click_path: Path) -> dict[str, Any]:
    return post_open_payload(
        status="blocked",
        blocker="project-not-opened",
        blocker_detail=(
            f"{tab_click_path.name} does not record projectOpenObserved=true; "
            "post-project-open window state cannot be proved without a prior "
            "confirmed project open."
        ),
        java_pid=None,
    )


def target_starter_open_not_proven_payload(tab_click_path: Path, tab_click: dict[str, Any]) -> dict[str, Any]:
    blocker_detail = (
        f"{tab_click_path.name} contains targetStarter metadata but does not record "
        "evidenceStatus=opened with targetStarterObserved identifying the target, "
        "safe startersTabSafety, targetSelectionObserved=true, openAttempted=true, "
        "openedStarter matching targetStarter, and projectOpenObserved=true; "
        "generic main-window observation cannot prove the Africa Full starter "
        "was opened."
    )
    return post_open_payload(
        status="blocked",
        blocker="target-starter-open-not-proven",
        blocker_detail=blocker_detail,
        java_pid=None,
        extra=target_opened_context(tab_click),
    )


def target_starter_metadata_missing_payload(tab_click_path: Path) -> dict[str, Any]:
    blocker_detail = (
        f"{tab_click_path.name} records projectOpenObserved=true but does not "
        "include validated targetStarter metadata; refusing to use generic "
        "project-open evidence as Africa Full proof."
    )
    return post_open_payload(
        status="blocked",
        blocker="target-starter-metadata-missing",
        blocker_detail=blocker_detail,
        java_pid=None,
        extra={
            "expectedTargetStarter": EXPECTED_TARGET_STARTER,
        },
    )


def target_starter_metadata_invalid_payload(
    tab_click_path: Path,
    tab_click: dict[str, Any],
) -> dict[str, Any]:
    blocker_detail = (
        f"{tab_click_path.name} contains targetStarter metadata that does not match "
        "the committed Africa Full target; refusing to use it as target-specific "
        "post-open proof."
    )
    return post_open_payload(
        status="blocked",
        blocker="target-starter-metadata-invalid",
        blocker_detail=blocker_detail,
        java_pid=None,
        extra={
            "expectedTargetStarter": EXPECTED_TARGET_STARTER,
            "evidenceStatus": tab_click.get("evidenceStatus"),
            "targetStarterMatchesExpected": False,
            "openedStarterMatchesExpected": tab_click.get("openedStarter") == EXPECTED_TARGET_STARTER,
        },
    )


def target_opened_context(tab_click: dict[str, Any]) -> dict[str, Any]:
    return {
        "targetStarter": tab_click.get("targetStarter"),
        "targetStarterObserved": tab_click.get("targetStarterObserved"),
        "openedStarter": tab_click.get("openedStarter"),
        "evidenceStatus": tab_click.get("evidenceStatus"),
        "targetStarterSelected": tab_click.get("targetStarterSelected"),
        "targetStarterOpenAttempted": tab_click.get("targetStarterOpenAttempted"),
        "targetSelectionObserved": tab_click.get("targetSelectionObserved"),
        "openAttempted": tab_click.get("openAttempted"),
        "targetProjectOpenObserved": bool(tab_click.get("projectOpenObserved", False)),
        "targetProjectOpenDetail": tab_click.get("projectOpenDetail", ""),
        "selectProjectWindowContext": tab_click.get("selectProjectWindowContext"),
        "startersTabSafety": tab_click.get("startersTabSafety"),
    }


def no_java_pid_payload(inventory_path: Path) -> dict[str, Any]:
    return post_open_payload(
        status="blocked",
        blocker="alice-window-java-pid-not-identified",
        blocker_detail=(
            "Unable to identify the Java process for the Alice 3 main window "
            f"from {inventory_path.name}. Refusing to introspect an arbitrary "
            "Java process."
        ),
        java_pid=None,
    )


def write_payload(output_path: Path, payload: dict[str, Any]) -> None:
    output_path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )


def read_json_or_blocked(
    path: Path,
) -> tuple[dict[str, Any] | None, dict[str, Any] | None]:
    try:
        return json.loads(path.read_text(encoding="utf-8")), None
    except (OSError, json.JSONDecodeError) as exc:
        return None, blocked_payload(path, exc)


def target_starter_observed_matches(observed: Any, target_starter: dict[str, Any]) -> bool:
    return (
        isinstance(observed, dict)
        and observed.get("name") == target_starter.get("displayName")
    )


def starters_tab_safety_matches(safety: Any) -> bool:
    return (
        isinstance(safety, dict)
        and safety.get("tabName") == "Starters"
        and safety.get("activationAttempted") is True
        and safety.get("activatedBeforeTargetSearch") is True
        and safety.get("targetSearchScope") == "active-starters-tab"
    )


def target_starter_gate_payload(
    tab_click_path: Path,
    tab_click: dict[str, Any],
) -> dict[str, Any] | None:
    target_starter = tab_click.get("targetStarter")
    if not isinstance(target_starter, dict):
        if tab_click.get("projectOpenObserved", False):
            return target_starter_metadata_missing_payload(tab_click_path)
        return None
    if target_starter != EXPECTED_TARGET_STARTER:
        return target_starter_metadata_invalid_payload(tab_click_path, tab_click)
    if (
        tab_click.get("evidenceStatus") != "opened"
        or tab_click.get("openedStarter") != target_starter
        or not target_starter_observed_matches(tab_click.get("targetStarterObserved"), target_starter)
        or not starters_tab_safety_matches(tab_click.get("startersTabSafety"))
        or tab_click.get("targetSelectionObserved") is not True
        or tab_click.get("openAttempted") is not True
        or not tab_click.get("projectOpenObserved", False)
    ):
        return target_starter_open_not_proven_payload(tab_click_path, tab_click)
    return None


def post_open_result_payload(
    inventory: dict[str, Any],
    inventory_path: Path,
    tab_click_path: Path,
    tab_click: dict[str, Any],
) -> dict[str, Any]:
    gated_payload = target_starter_gate_payload(tab_click_path, tab_click)
    if gated_payload is not None:
        return gated_payload

    target_starter = tab_click.get("targetStarter")
    if not tab_click.get("projectOpenObserved", False):
        return project_not_opened_payload(tab_click_path)

    java_pid = find_java_pid(inventory)
    if java_pid is None:
        payload = no_java_pid_payload(inventory_path)
    else:
        payload = probe_post_open(java_pid)

    if isinstance(target_starter, dict):
        payload.update(target_opened_context(tab_click))
    return payload


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inventory", help="Path to x-window-inventory.json")
    parser.add_argument("tab_click", help="Path to tab-click-observation.json")
    parser.add_argument("output", help="Path to write post-project-open-observation.json")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    tab_click_path = Path(args.tab_click)
    output_path = Path(args.output)

    inventory, payload = read_json_or_blocked(inventory_path)
    if payload is not None:
        write_payload(output_path, payload)
        return 0

    tab_click, payload = read_json_or_blocked(tab_click_path)
    if payload is not None:
        write_payload(output_path, payload)
        return 0

    payload = post_open_result_payload(inventory, inventory_path, tab_click_path, tab_click)
    write_payload(output_path, payload)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
