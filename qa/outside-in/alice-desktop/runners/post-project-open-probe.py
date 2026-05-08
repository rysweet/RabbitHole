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
import time
from pathlib import Path
from typing import Any

EXPECTED_SELECT_PROJECT_TITLE = "Select Project"
EXPECTED_ALICE_TITLE = "Alice 3"
POST_OPEN_WAIT_SECONDS = 5


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
    for _attempt in range(5):
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
        time.sleep(2)
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


def probe_post_open(java_pid: int) -> dict[str, Any]:
    """Connect to AT-SPI and enumerate alice_app frames after project open."""
    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return post_open_payload(
            status="blocked",
            blocker="pyatspi-not-installed",
            blocker_detail="python3-pyatspi is not installed.",
            java_pid=java_pid,
        )

    try:
        desktop = pyatspi.Registry.getDesktop(0)
    except Exception as exc:
        return post_open_payload(
            status="blocked",
            blocker="at-spi-registry-unavailable",
            blocker_detail=f"Cannot connect to AT-SPI registry: {exc}",
            java_pid=java_pid,
        )

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

    # Wait briefly to allow Alice to finish loading the project.
    time.sleep(POST_OPEN_WAIT_SECONDS)

    frame_names, frame_child_counts = top_level_frame_state(alice_app)

    # The proof criterion: at least one frame present that is NOT "Select Project".
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


def blocked_payload(path: Path, exc: Exception) -> dict[str, Any]:
    return post_open_payload(
        status="blocked",
        blocker="input-unreadable",
        blocker_detail=f"Could not read {path}: {exc}",
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
    target = tab_click.get("targetStarter")
    status = tab_click.get("evidenceStatus")
    opened = tab_click.get("openedStarter")
    blocker_detail = (
        f"{tab_click_path.name} contains targetStarter metadata but does not record "
        "evidenceStatus=opened with openedStarter matching targetStarter; generic "
        "main-window observation cannot prove the Africa Full starter was opened."
    )
    return post_open_payload(
        status="blocked",
        blocker="target-starter-open-not-proven",
        blocker_detail=blocker_detail,
        java_pid=None,
        extra={
            "targetStarter": target,
            "evidenceStatus": status,
            "openedStarter": opened,
        },
    )


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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inventory", help="Path to x-window-inventory.json")
    parser.add_argument("tab_click", help="Path to tab-click-observation.json")
    parser.add_argument("output", help="Path to write post-project-open-observation.json")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    tab_click_path = Path(args.tab_click)
    output_path = Path(args.output)

    # Load inventory.
    try:
        inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        payload = blocked_payload(inventory_path, exc)
        output_path.write_text(
            json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )
        return 0

    # Load tab-click observation.
    try:
        tab_click = json.loads(tab_click_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        payload = blocked_payload(tab_click_path, exc)
        output_path.write_text(
            json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )
        return 0

    # Target-specific scenarios must prove the selected/opened starter before the
    # generic main-window state can be used as downstream evidence.
    target_starter = tab_click.get("targetStarter")
    if isinstance(target_starter, dict):
        if (
            tab_click.get("evidenceStatus") != "opened"
            or tab_click.get("openedStarter") != target_starter
            or not tab_click.get("projectOpenObserved", False)
        ):
            payload = target_starter_open_not_proven_payload(tab_click_path, tab_click)
            output_path.write_text(
                json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
            )
            return 0

    # Require projectOpenObserved=true before connecting to AT-SPI.
    if not tab_click.get("projectOpenObserved", False):
        payload = project_not_opened_payload(tab_click_path)
        output_path.write_text(
            json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )
        return 0

    java_pid = find_java_pid(inventory)
    if java_pid is None:
        payload = no_java_pid_payload(inventory_path)
        output_path.write_text(
            json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )
        return 0

    payload = probe_post_open(java_pid)
    output_path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
