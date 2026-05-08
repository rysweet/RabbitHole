#!/usr/bin/env python3
"""Observe the live first-lesson procedure/code-editor target after project open.

This probe is read-only. It only runs after the existing Select Project and
post-open window probes have produced supporting evidence, then searches the live
AT-SPI tree for a stable procedure tab or code-editor target for
``scene.eatmeFirstLesson``. It never clicks, edits, saves, runs, screenshots, or
records a broad accessibility tree.
"""

from __future__ import annotations

import argparse
from collections import deque
import json
from pathlib import Path
from typing import Any

SCHEMA_VERSION = "eatme.first-lesson-live-procedure-target-observation/v1"
WORKFLOW = "first-lesson-live-procedure-target-observation"
SEAM = "live-first-lesson-project-open-to-procedure-target-observable"
DOWNSTREAM_BLOCKED_STEP = "desktop-procedure-edit"
DESKTOP_EDIT_ACTION_BLOCKER_KIND = "missing-desktop-edit-action-contract"
DESKTOP_EDIT_ACTION_BLOCKER_MESSAGE = "missing public CodeEditor/CodeComposite edit invocation contract"
OUT_OF_SCOPE = [
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
    "full first-lesson completion",
]
MAX_DESKTOP_APPS = 50
MAX_ACCESSIBLES_TO_VISIT = 350
MAX_CHILDREN_PER_ACCESSIBLE = 80
MAX_ACCESSIBLE_DEPTH = 10


def read_json(path: Path) -> tuple[dict[str, Any] | None, str | None]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return None, f"Could not read {path.name}: {exc}"
    if not isinstance(payload, dict):
        return None, f"{path.name} did not contain a JSON object"
    return payload, None


def bool_field(payload: dict[str, Any], name: str) -> bool:
    return payload.get(name) is True


def base_payload(
    *,
    scenario_id: str,
    automation_mode: str,
    target_display_name: str,
    target_repo_path: str,
    procedure_selector: str,
    status: str,
    blocker: str,
    blocker_detail: str,
    opened_via_select_project: bool = False,
    post_open_window_observed: bool = False,
    observed_target: dict[str, Any] | None = None,
    desktop_edit_action: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "schemaVersion": SCHEMA_VERSION,
        "scenario": scenario_id,
        "workflow": WORKFLOW,
        "automationMode": automation_mode,
        "status": status,
        "seam": SEAM,
        "project": {
            "targetStarterDisplayName": target_display_name,
            "targetStarterRepositoryPath": target_repo_path,
            "openedViaSelectProject": opened_via_select_project,
            "postOpenWindowObserved": post_open_window_observed,
        },
        "requiredTarget": {
            "procedureSelector": procedure_selector,
            "targetKind": "procedure-tab-or-code-editor",
            "minimumStableAutomationTarget": "reacquirable live desktop procedure tab or code-editor target",
        },
        "observedTarget": observed_target,
        "desktopEditAction": desktop_edit_action,
        "blocker": blocker,
        "blockerDetail": blocker_detail,
        "downstreamBlockedStep": DOWNSTREAM_BLOCKED_STEP,
        "outOfScope": OUT_OF_SCOPE,
    }


def find_java_pid(inventory: dict[str, Any]) -> int | None:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None
    for preferred_title in ("Alice 3", "Select Project"):
        for window in windows:
            if not isinstance(window, dict):
                continue
            if str(window.get("title", "")) != preferred_title:
                continue
            if str(window.get("processName", "")).lower() != "java":
                continue
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


def target_starter_opened(tab_click: dict[str, Any], target_display_name: str, target_repo_path: str) -> bool:
    target = tab_click.get("targetStarter")
    opened = tab_click.get("openedStarter")
    return (
        isinstance(target, dict)
        and isinstance(opened, dict)
        and target.get("displayName") == target_display_name
        and target.get("repositoryPath") == target_repo_path
        and opened.get("displayName") == target_display_name
        and opened.get("repositoryPath") == target_repo_path
        and tab_click.get("evidenceStatus") == "opened"
        and bool_field(tab_click, "targetStarterSelected")
        and bool_field(tab_click, "targetStarterOpenAttempted")
        and bool_field(tab_click, "projectOpenObserved")
    )


def safe_name(accessible: Any) -> str:
    try:
        return str(accessible.name or "")
    except Exception:
        return ""


def safe_role(accessible: Any) -> str:
    try:
        return str(accessible.getRoleName() or "")
    except Exception:
        return ""


def safe_child_count(accessible: Any) -> int:
    try:
        return int(accessible.childCount)
    except Exception:
        return 0


def state_names(accessible: Any) -> list[str]:
    try:
        state_set = accessible.getState()
        raw_states = state_set.getStates()
    except Exception:
        return []
    return sorted({str(state).rsplit(".", 1)[-1].lower() for state in raw_states})


def visible_state_constants(pyatspi: Any) -> tuple[Any, ...]:
    return tuple(
        state
        for state in (
            getattr(pyatspi, "STATE_SHOWING", None),
            getattr(pyatspi, "STATE_VISIBLE", None),
        )
        if state is not None
    )


def has_visible_state(accessible: Any, visible_constants: tuple[Any, ...]) -> bool:
    if not visible_constants:
        return True
    try:
        state_set = accessible.getState()
        return any(state_set.contains(state) for state in visible_constants)
    except Exception:
        return False


def find_alice_app(pyatspi: Any, java_pid: int) -> Any | None:
    desktop = pyatspi.Registry.getDesktop(0)
    try:
        app_count = int(desktop.childCount)
    except Exception:
        app_count = 0
    for index in range(min(app_count, MAX_DESKTOP_APPS)):
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
        if app_pid == java_pid:
            return app
    return None


def target_kind_for_summary(name: str, role: str) -> str | None:
    lower = f"{name} {role}".lower()
    if any(token in lower for token in ("code editor", "code-editor", "code view", "source")):
        return "code-editor"
    if any(token in lower for token in ("page tab", "tab list", "tab", "procedure")):
        return "procedure-tab"
    return None


def collect_target_candidates(pyatspi: Any, alice_app: Any, method_name: str) -> list[dict[str, Any]]:
    visible_constants = visible_state_constants(pyatspi)
    queue: deque[tuple[Any, str, int]] = deque([(alice_app, "application", 0)])
    visited = 0
    candidates: list[dict[str, Any]] = []

    while queue and visited < MAX_ACCESSIBLES_TO_VISIT:
        accessible, path, depth = queue.popleft()
        visited += 1
        name = safe_name(accessible)
        role = safe_role(accessible)
        child_count = safe_child_count(accessible)
        lower = f"{name} {role}".lower()
        target_kind = target_kind_for_summary(name, role)
        method_visible = method_name.lower() in lower
        visible = has_visible_state(accessible, visible_constants)

        if visible and method_visible and target_kind is not None:
            candidates.append(
                {
                    "targetKind": target_kind,
                    "accessibleName": name or None,
                    "accessibleRole": role or None,
                    "automationPath": f"at-spi:{path}",
                    "states": state_names(accessible),
                    "childCount": child_count,
                }
            )

        if depth >= MAX_ACCESSIBLE_DEPTH:
            continue
        for child_index in range(min(child_count, MAX_CHILDREN_PER_ACCESSIBLE)):
            try:
                child = accessible.getChildAtIndex(child_index)
            except Exception:
                continue
            if child is not None:
                queue.append((child, f"{path}/{child_index}", depth + 1))
    return candidates


def observed_payload(
    args: argparse.Namespace,
    target_display_name: str,
    target_repo_path: str,
    candidate: dict[str, Any],
) -> dict[str, Any]:
    observed = {
        "targetKind": candidate["targetKind"],
        "procedureSelector": args.procedure_selector,
        "accessibleName": candidate.get("accessibleName"),
        "accessibleRole": candidate.get("accessibleRole"),
        "automationPath": candidate["automationPath"],
        "readyForDesktopEditAction": False,
    }
    desktop_edit_action = {
        "status": "blocked",
        "readyForDesktopEditAction": False,
        "targetSelector": args.procedure_selector,
        "blocker": {
            "kind": DESKTOP_EDIT_ACTION_BLOCKER_KIND,
            "message": DESKTOP_EDIT_ACTION_BLOCKER_MESSAGE,
        },
        "requiredContract": "public CodeEditor/CodeComposite edit invocation contract",
        "doesNotClaim": [
            "desktop procedure edit mutation",
            "Save",
            "rendering correctness",
            "learner assessment",
            "full first-lesson completion",
        ],
    }
    return base_payload(
        scenario_id=args.scenario_id,
        automation_mode=args.automation_mode,
        target_display_name=target_display_name,
        target_repo_path=target_repo_path,
        procedure_selector=args.procedure_selector,
        status="observed",
        blocker="none",
        blocker_detail="",
        opened_via_select_project=True,
        post_open_window_observed=True,
        observed_target=observed,
        desktop_edit_action=desktop_edit_action,
    )


def blocked_payload(
    args: argparse.Namespace,
    target_display_name: str,
    target_repo_path: str,
    blocker: str,
    detail: str,
    *,
    opened_via_select_project: bool = False,
    post_open_window_observed: bool = False,
) -> dict[str, Any]:
    return base_payload(
        scenario_id=args.scenario_id,
        automation_mode=args.automation_mode,
        target_display_name=target_display_name,
        target_repo_path=target_repo_path,
        procedure_selector=args.procedure_selector,
        status="blocked",
        blocker=blocker,
        blocker_detail=detail,
        opened_via_select_project=opened_via_select_project,
        post_open_window_observed=post_open_window_observed,
        observed_target=None,
    )


def probe(args: argparse.Namespace) -> dict[str, Any]:
    target_display_name = args.target_starter_display_name
    target_repo_path = args.target_starter_repository_path
    inventory, inventory_error = read_json(Path(args.inventory))
    if inventory_error or inventory is None:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "display-prerequisite-unavailable",
            inventory_error or "x-window-inventory.json was unreadable.",
        )

    tab_click, tab_click_error = read_json(Path(args.tab_click_observation))
    if tab_click_error or tab_click is None:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "select-project-open-not-observed",
            tab_click_error or "tab-click-observation.json was unreadable.",
        )
    opened = target_starter_opened(tab_click, target_display_name, target_repo_path)
    if not opened:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "select-project-open-not-observed",
            (
                "tab-click-observation.json does not prove the configured first-lesson "
                "starter was selected and opened through Select Project."
            ),
        )

    post_open, post_open_error = read_json(Path(args.post_open_window_observation))
    if post_open_error or post_open is None:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "post-open-window-not-observed",
            post_open_error or "post-project-open-observation.json was unreadable.",
            opened_via_select_project=True,
        )
    if post_open.get("postOpenWindowObserved") is not True:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "post-open-window-not-observed",
            "post-project-open-observation.json does not record postOpenWindowObserved=true.",
            opened_via_select_project=True,
        )

    java_pid = find_java_pid(inventory)
    if java_pid is None:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "display-prerequisite-unavailable",
            "x-window-inventory.json does not identify a Java Alice window PID for AT-SPI target observation.",
            opened_via_select_project=True,
            post_open_window_observed=True,
        )

    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "at-spi-or-atk-unavailable",
            "python3-pyatspi is not installed.",
            opened_via_select_project=True,
            post_open_window_observed=True,
        )

    try:
        alice_app = find_alice_app(pyatspi, java_pid)
    except Exception as exc:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "at-spi-or-atk-unavailable",
            f"Cannot connect to or traverse AT-SPI registry: {exc}",
            opened_via_select_project=True,
            post_open_window_observed=True,
        )
    if alice_app is None:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "at-spi-or-atk-unavailable",
            f"Java process PID {java_pid} was not found in the AT-SPI registry.",
            opened_via_select_project=True,
            post_open_window_observed=True,
        )

    method_name = args.procedure_selector.split(".", 1)[-1]
    candidates = collect_target_candidates(pyatspi, alice_app, method_name)
    if not candidates:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "procedure-target-not-found",
            (
                "The first-lesson project opened, but the live desktop did not expose "
                f"a stable procedure tab or code-editor target for {args.procedure_selector}."
            ),
            opened_via_select_project=True,
            post_open_window_observed=True,
        )

    stable_candidates = [
        candidate
        for candidate in candidates
        if isinstance(candidate.get("automationPath"), str)
        and candidate["automationPath"].startswith("at-spi:application/")
    ]
    if not stable_candidates:
        return blocked_payload(
            args,
            target_display_name,
            target_repo_path,
            "procedure-target-not-stable",
            (
                f"A candidate for {args.procedure_selector} was visible, but it did "
                "not expose a reacquirable AT-SPI automation path."
            ),
            opened_via_select_project=True,
            post_open_window_observed=True,
        )
    return observed_payload(args, target_display_name, target_repo_path, stable_candidates[0])


def write_payload(output_path: Path, payload: dict[str, Any]) -> None:
    if output_path.exists() and output_path.is_symlink():
        raise RuntimeError(f"refusing to overwrite symlink artifact: {output_path}")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inventory", required=True, help="Path to x-window-inventory.json")
    parser.add_argument("--tab-click-observation", required=True, help="Path to tab-click-observation.json")
    parser.add_argument(
        "--post-open-window-observation",
        required=True,
        help="Path to post-project-open-observation.json",
    )
    parser.add_argument("--output", required=True, help="Path to write the decision artifact")
    parser.add_argument("--scenario-id", required=True)
    parser.add_argument("--automation-mode", required=True)
    parser.add_argument("--target-starter-display-name", required=True)
    parser.add_argument("--target-starter-repository-path", required=True)
    parser.add_argument("--procedure-selector", required=True)
    args = parser.parse_args()

    output_path = Path(args.output)
    payload = probe(args)
    write_payload(output_path, payload)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
