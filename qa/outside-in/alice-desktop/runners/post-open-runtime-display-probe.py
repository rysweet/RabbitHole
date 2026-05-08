#!/usr/bin/env python3
"""Collect narrow post-open runtime/display accessibility evidence.

This probe is read-only. It requires an already-observed post-open Alice window
state, finds the Java process from x-window-inventory.json, then inspects the
AT-SPI tree for a visible runtime/display-like accessible component. It does not
click controls, save projects, decode project data, run worlds, grade work, or
claim rendering correctness.
"""

from __future__ import annotations

import argparse
import json
import time
from pathlib import Path
from typing import Any

CLAIM = "post-open-runtime-display-accessibility-evidence"
SELECT_PROJECT_TITLE = "Select Project"
ARTIFACT_NAME = "post-open-runtime-display-accessibility-evidence.json"
RUNTIME_NAME_TOKENS = (
    "scene",
    "display",
    "runtime",
    "world",
    "render",
    "simulation",
    "program",
)
RUNTIME_ROLE_TOKENS = (
    "canvas",
    "drawing",
    "viewport",
    "layered pane",
)
VISIBLE_STATE_NAMES = ("showing", "visible")


def read_json(path: Path) -> tuple[dict[str, Any] | None, str | None]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return None, f"Could not read {path}: {exc}"
    if not isinstance(payload, dict):
        return None, f"{path} did not contain a JSON object"
    return payload, None


def find_java_pid(inventory: dict[str, Any]) -> int | None:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None

    for preferred_title in ("Alice 3", SELECT_PROJECT_TITLE):
        for window in windows:
            if not isinstance(window, dict):
                continue
            if (
                str(window.get("title", "")) == preferred_title
                and str(window.get("processName", "")).lower() == "java"
            ):
                pid = window.get("pid")
                if isinstance(pid, int) and pid > 0:
                    return pid

    for window in windows:
        if not isinstance(window, dict):
            continue
        if str(window.get("processName", "")).lower() == "java":
            pid = window.get("pid")
            if isinstance(pid, int) and pid > 0:
                return pid
    return None


def base_payload(
    *,
    status: str,
    blocker: str,
    blocker_detail: str,
    scenario_id: str,
    automation_mode: str,
    java_pid: int | None = None,
    post_open_window_observed: bool = False,
    candidates: list[dict[str, Any]] | None = None,
    traversal_errors: list[str] | None = None,
) -> dict[str, Any]:
    runtime_candidates = candidates or []
    return {
        "status": status,
        "blocker": blocker,
        "blockerDetail": blocker_detail,
        "claim": CLAIM,
        "scenario": scenario_id,
        "automationMode": automation_mode,
        "javaPid": java_pid,
        "postOpenWindowObserved": post_open_window_observed,
        "postOpenRuntimeDisplayAccessibilityObserved": status == "observed",
        "runtimeDisplayCandidateCount": len(runtime_candidates),
        "runtimeDisplayCandidates": runtime_candidates,
        "traversalErrors": traversal_errors or [],
    }


def state_names(accessible: Any) -> list[str]:
    try:
        state_set = accessible.getState()
    except Exception as exc:  # AT-SPI objects can disappear while traversing.
        raise RuntimeError(f"failed to read state for accessible: {exc}") from exc

    states: list[str] = []
    try:
        raw_states = state_set.getStates()
    except Exception:
        raw_states = []
    for state in raw_states:
        states.append(str(state).rsplit(".", 1)[-1].lower())
    return sorted(set(states))


def has_visible_state(pyatspi: Any, accessible: Any) -> bool:
    try:
        state_set = accessible.getState()
    except Exception as exc:
        raise RuntimeError(f"failed to read visibility state: {exc}") from exc
    visible_constants = [
        getattr(pyatspi, "STATE_SHOWING", None),
        getattr(pyatspi, "STATE_VISIBLE", None),
    ]
    visible_constants = [state for state in visible_constants if state is not None]
    if not visible_constants:
        return True
    try:
        return any(state_set.contains(state) for state in visible_constants)
    except Exception as exc:
        raise RuntimeError(f"failed to evaluate visibility state: {exc}") from exc


def accessible_summary(accessible: Any, path: str) -> dict[str, Any]:
    try:
        name = str(accessible.name or "")
    except Exception as exc:
        raise RuntimeError(f"{path}: failed to read name: {exc}") from exc
    try:
        role = str(accessible.getRoleName() or "")
    except Exception as exc:
        raise RuntimeError(f"{path}: failed to read role: {exc}") from exc
    try:
        child_count = int(accessible.childCount)
    except Exception as exc:
        raise RuntimeError(f"{path}: failed to read childCount: {exc}") from exc
    states = state_names(accessible)
    return {
        "name": name,
        "role": role,
        "path": path,
        "childCount": child_count,
        "states": states,
    }


def is_select_project_surface(summary: dict[str, Any]) -> bool:
    text = f"{summary.get('name', '')} {summary.get('role', '')}".lower()
    return "select project" in text or "select-project" in text


def is_runtime_display_candidate(summary: dict[str, Any]) -> bool:
    if is_select_project_surface(summary):
        return False
    name = str(summary.get("name", "")).lower()
    role = str(summary.get("role", "")).lower()
    if any(token in role for token in ("dialog", "menu", "tool bar", "page tab")):
        return False
    role_match = any(token in role for token in RUNTIME_ROLE_TOKENS)
    name_match = any(token in name for token in RUNTIME_NAME_TOKENS)
    named_display_panel = "panel" in role and any(
        token in name for token in ("scene", "display", "world", "render")
    )
    return (role_match and (name_match or "canvas" in role or "drawing" in role)) or named_display_panel


def find_alice_app(pyatspi: Any, java_pid: int) -> tuple[Any | None, int, list[str]]:
    traversal_errors: list[str] = []
    desktop = pyatspi.Registry.getDesktop(0)
    app_count = 0
    for _attempt in range(5):
        try:
            app_count = int(desktop.childCount)
        except Exception as exc:
            traversal_errors.append(f"desktop childCount unavailable: {exc}")
            app_count = 0
        for index in range(min(app_count, 50)):
            try:
                app = desktop.getChildAtIndex(index)
            except Exception as exc:
                traversal_errors.append(f"desktop child {index} unavailable: {exc}")
                continue
            if app is None:
                continue
            try:
                app_pid = app.get_process_id()
            except Exception as exc:
                traversal_errors.append(f"app {index} process id unavailable: {exc}")
                app_pid = None
            if app_pid == java_pid:
                return app, app_count, traversal_errors
        time.sleep(1)
    return None, app_count, traversal_errors


def collect_candidates(pyatspi: Any, alice_app: Any) -> tuple[list[dict[str, Any]], list[str]]:
    candidates: list[dict[str, Any]] = []
    traversal_errors: list[str] = []
    queue: list[tuple[Any, str, int]] = [(alice_app, "application", 0)]
    visited = 0

    while queue and visited < 250:
        accessible, path, depth = queue.pop(0)
        visited += 1
        try:
            summary = accessible_summary(accessible, path)
        except RuntimeError as exc:
            traversal_errors.append(str(exc))
            continue

        try:
            visible = has_visible_state(pyatspi, accessible)
        except RuntimeError as exc:
            traversal_errors.append(f"{path}: {exc}")
            visible = False

        if visible and is_runtime_display_candidate(summary):
            candidates.append(summary)

        if depth >= 8:
            continue
        child_count = min(int(summary.get("childCount", 0)), 80)
        for index in range(child_count):
            try:
                child = accessible.getChildAtIndex(index)
            except Exception as exc:
                traversal_errors.append(f"{path}/{index}: child unavailable: {exc}")
                continue
            if child is not None:
                queue.append((child, f"{path}/{index}", depth + 1))

    return candidates, traversal_errors


def probe_runtime_display(java_pid: int, scenario_id: str, automation_mode: str) -> dict[str, Any]:
    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return base_payload(
            status="blocked",
            blocker="pyatspi-not-installed",
            blocker_detail="python3-pyatspi is not installed.",
            scenario_id=scenario_id,
            automation_mode=automation_mode,
            java_pid=java_pid,
            post_open_window_observed=True,
        )

    try:
        alice_app, app_count, app_errors = find_alice_app(pyatspi, java_pid)
    except Exception as exc:
        return base_payload(
            status="blocked",
            blocker="at-spi-registry-unavailable",
            blocker_detail=f"Cannot connect to or traverse AT-SPI registry: {exc}",
            scenario_id=scenario_id,
            automation_mode=automation_mode,
            java_pid=java_pid,
            post_open_window_observed=True,
        )

    if alice_app is None:
        return base_payload(
            status="blocked",
            blocker="atk-wrapper-not-loaded",
            blocker_detail=(
                f"Java process PID {java_pid} was not found in the AT-SPI registry "
                f"({app_count} total AT-SPI apps visible)."
            ),
            scenario_id=scenario_id,
            automation_mode=automation_mode,
            java_pid=java_pid,
            post_open_window_observed=True,
            traversal_errors=app_errors,
        )

    candidates, candidate_errors = collect_candidates(pyatspi, alice_app)
    traversal_errors = app_errors + candidate_errors
    if not candidates:
        return base_payload(
            status="blocked",
            blocker="runtime-display-accessible-candidate-not-found",
            blocker_detail=(
                "No visible AT-SPI accessible component matched the narrow "
                "runtime/display candidate criteria after the post-open window "
                "state was observed."
            ),
            scenario_id=scenario_id,
            automation_mode=automation_mode,
            java_pid=java_pid,
            post_open_window_observed=True,
            traversal_errors=traversal_errors,
        )

    return base_payload(
        status="observed",
        blocker="none",
        blocker_detail="",
        scenario_id=scenario_id,
        automation_mode=automation_mode,
        java_pid=java_pid,
        post_open_window_observed=True,
        candidates=candidates[:10],
        traversal_errors=traversal_errors,
    )


def write_status_file(path: Path, payload: dict[str, Any], artifact_name: str) -> None:
    outcome = "passed" if payload.get("status") == "observed" else "blocked"
    lines = [
        f"scenario={payload.get('scenario', '')}",
        f"automationMode={payload.get('automationMode', '')}",
        f"outcome={outcome}",
        f"runtimeDisplayAccessibilityEvidence={artifact_name}",
        f"runtimeDisplayAccessibilityStatus={payload.get('status', '')}",
        f"runtimeDisplayAccessibilityBlocker={payload.get('blocker', '')}",
    ]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inventory", required=True, help="Path to x-window-inventory.json")
    parser.add_argument(
        "--post-open-window-observation",
        required=True,
        help="Path to post-project-open-observation.json",
    )
    parser.add_argument("--output", required=True, help=f"Path to write {ARTIFACT_NAME}")
    parser.add_argument("--status-file", required=True, help="Path to write status.txt")
    parser.add_argument("--scenario-id", required=True, help="Scenario id for status output")
    parser.add_argument("--automation-mode", required=True, help="Automation mode for status output")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    post_open_path = Path(args.post_open_window_observation)
    output_path = Path(args.output)
    status_path = Path(args.status_file)
    artifact_name = output_path.name

    inventory, inventory_error = read_json(inventory_path)
    if inventory_error is not None or inventory is None:
        payload = base_payload(
            status="blocked",
            blocker="input-unreadable",
            blocker_detail=inventory_error or "Inventory input was unreadable.",
            scenario_id=args.scenario_id,
            automation_mode=args.automation_mode,
        )
    else:
        post_open, post_open_error = read_json(post_open_path)
        if post_open_error is not None or post_open is None:
            payload = base_payload(
                status="blocked",
                blocker="input-unreadable",
                blocker_detail=post_open_error or "Post-open observation input was unreadable.",
                scenario_id=args.scenario_id,
                automation_mode=args.automation_mode,
            )
        elif post_open.get("postOpenWindowObserved") is not True:
            payload = base_payload(
                status="blocked",
                blocker="post-open-window-not-observed",
                blocker_detail=(
                    f"{post_open_path.name} does not record postOpenWindowObserved=true; "
                    "runtime/display accessibility evidence requires the existing "
                    "post-open window setup to be observed first."
                ),
                scenario_id=args.scenario_id,
                automation_mode=args.automation_mode,
            )
        else:
            java_pid = find_java_pid(inventory)
            if java_pid is None:
                payload = base_payload(
                    status="blocked",
                    blocker="java-pid-not-in-inventory",
                    blocker_detail=(
                        f"No Java window found in {inventory_path.name}; cannot identify "
                        "the Alice process for runtime/display AT-SPI introspection."
                    ),
                    scenario_id=args.scenario_id,
                    automation_mode=args.automation_mode,
                    post_open_window_observed=True,
                )
            else:
                payload = probe_runtime_display(java_pid, args.scenario_id, args.automation_mode)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    status_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    write_status_file(status_path, payload, artifact_name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
