#!/usr/bin/env python3
"""Probe the AT-SPI tree for Select Project tab structure and attempt a tab click.

Reads x-window-inventory.json, connects to AT-SPI, finds the Select Project
frame, dumps ALL widget roles (including unnamed nodes), identifies page-tab
and page-tab-list nodes, attempts DoAction("click") on each tab found, and
records the results as machine-readable JSON.

This is the next increment after PR #272, which proved Swing widgets are
visible through AT-SPI but found that tab labels are empty.  This probe
diagnoses the full tree (including unnamed nodes) to identify the tab
structure and attempts an AT-SPI action-based click.

Outputs tab-click-observation.json.

Requires:
- python3-pyatspi installed (sudo apt-get install -y python3-pyatspi)
- Alice launched with exec:exec@alice-ide-atk (NO_AT_BRIDGE=1,
  -Xbootclasspath/a:/usr/share/java/java-atk-wrapper.jar)
"""

from __future__ import annotations

import argparse
import json
import os
import time
from pathlib import Path
from typing import Any, Iterator

ATK_WRAPPER_JAR = "/usr/share/java/java-atk-wrapper.jar"
EXPECTED_SELECT_PROJECT_TITLE = "Select Project"
EXPECTED_TAB_LABELS = ["Blank Slates", "Starters", "My Projects", "Recent", "File System"]
EXPECTED_TARGET_STARTER_DISPLAY_NAME = "Africa Full"
EXPECTED_TARGET_STARTER_REPO_PATH = (
    "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
)
WIDGET_TREE_ALLOWED_NAMES = frozenset(
    [EXPECTED_SELECT_PROJECT_TITLE, "OK", "Open", *EXPECTED_TAB_LABELS]
)
# Depth limit high enough to see tab children even if they are nested
MAX_DEPTH = 12
# Alice's Select Project uses custom toggle buttons for tabs rather than standard
# JTabbedPane page tabs. These are the AT-SPI role strings to search for.
PAGE_TAB_LIST_ROLES = frozenset({"pagetablist", "page tab list"})
TAB_ROLES = frozenset({"pagetab", "page tab"})
TOGGLE_TAB_ROLES = frozenset({"togglebutton", "toggle button"})
BUTTON_ROLES = frozenset({"button", "pushbutton"})
LIST_ROLES = frozenset({"list"})
PREFERRED_ACTIONS = ("click", "select", "activate")
TARGET_STARTER_DISPLAY_ENV = "TARGET_STARTER_DISPLAY_NAME"
TARGET_STARTER_REPO_ENV = "TARGET_STARTER_REPO_PATH"
NodeInfo = tuple[Any, int, str, str]


def find_select_project_java_pid(inventory: dict[str, Any]) -> int | None:
    windows = inventory.get("windows", [])
    if not isinstance(windows, list):
        return None
    for window in windows:
        if not isinstance(window, dict):
            continue
        if (
            str(window.get("title", "")) == EXPECTED_SELECT_PROJECT_TITLE
            and str(window.get("processName", "")).lower() == "java"
        ):
            pid = window.get("pid")
            if isinstance(pid, int) and pid > 0:
                return pid
    return None


def safe_node_name(node: Any) -> str:
    try:
        return node.name or ""
    except Exception:
        return ""


def safe_role_name(node: Any) -> str:
    try:
        return node.getRoleName()
    except Exception:
        return ""


def safe_node_description(node: Any) -> str:
    try:
        return node.description or ""
    except Exception:
        return ""


def safe_child_count(node: Any) -> int:
    try:
        return int(node.childCount)
    except Exception:
        return 0


def node_properties(node: Any) -> dict[str, Any]:
    """Extract safe scalar properties from an AT-SPI node."""
    return {
        "name": safe_node_name(node),
        "role": safe_role_name(node),
        "description": safe_node_description(node),
        "childCount": safe_child_count(node),
    }


def widget_tree_properties(node: Any) -> dict[str, Any]:
    props = node_properties(node)
    raw_name = props["name"]
    raw_description = props["description"]
    safe_name = raw_name if raw_name in WIDGET_TREE_ALLOWED_NAMES else ""
    return {
        "name": safe_name,
        "role": props["role"],
        "description": "",
        "childCount": props["childCount"],
        "nameRedacted": bool(raw_name and raw_name != safe_name),
        "descriptionRedacted": bool(raw_description),
    }


def configured_target_starter() -> dict[str, str] | None:
    display_name = os.environ.get(TARGET_STARTER_DISPLAY_ENV, "").strip()
    repository_path = os.environ.get(TARGET_STARTER_REPO_ENV, "").strip()
    if not display_name and not repository_path:
        return None
    return {
        "displayName": display_name,
        "repositoryPath": repository_path,
    }


def validate_target_starter(target_starter: dict[str, str] | None) -> str:
    if target_starter is None:
        return ""
    display_name = target_starter.get("displayName", "")
    repository_path = target_starter.get("repositoryPath", "")
    if not display_name:
        return f"{TARGET_STARTER_DISPLAY_ENV} must be non-empty when target starter evidence is requested."
    if not repository_path:
        return f"{TARGET_STARTER_REPO_ENV} must be non-empty when target starter evidence is requested."
    path = Path(repository_path)
    if path.is_absolute():
        return f"{TARGET_STARTER_REPO_ENV} must be repository-relative, not absolute."
    if any(part == ".." for part in path.parts):
        return f"{TARGET_STARTER_REPO_ENV} must not contain .. path traversal."
    if display_name != EXPECTED_TARGET_STARTER_DISPLAY_NAME:
        return (
            f"{TARGET_STARTER_DISPLAY_ENV} must be "
            f"{EXPECTED_TARGET_STARTER_DISPLAY_NAME!r} for target-specific Select Project evidence."
        )
    if repository_path != EXPECTED_TARGET_STARTER_REPO_PATH:
        return (
            f"{TARGET_STARTER_REPO_ENV} must be {EXPECTED_TARGET_STARTER_REPO_PATH!r} "
            "for target-specific Select Project evidence."
        )
    return ""


def target_validation_failed_payload(
    target_starter: dict[str, str] | None,
    *,
    java_pid: int | None,
    target_validation_error: str,
) -> dict[str, Any]:
    return add_target_metadata(
        {
            "status": "failed",
            "blocker": "target-starter-metadata-invalid",
            "blockerDetail": target_validation_error,
            "javaPid": java_pid,
            "allWidgetTree": [],
            "roleCounts": {},
            "tabListNodes": [],
            "tabNodes": [],
            "tabClickAttempts": [],
            "widgetCountAfterClick": 0,
            "allWidgetTreeAfterClick": [],
            "projectOpenObserved": False,
            "projectOpenAttempt": {},
        },
        target_starter,
        evidence_status="failed",
        blocker=next_blocker(
            "Target starter metadata was provided to the probe but failed local validation.",
            "Validate exact target starter metadata before connecting to AT-SPI.",
            (
                f"Pass display name {EXPECTED_TARGET_STARTER_DISPLAY_NAME!r} and "
                f"repository path {EXPECTED_TARGET_STARTER_REPO_PATH!r}."
            ),
            target_validation_error,
        ),
    )


def next_blocker(
    observed_atspi_state: str,
    action_attempted: str,
    expected_next_action: str,
    reason_progress_stopped: str,
) -> dict[str, str]:
    return {
        "observedAtspiState": observed_atspi_state,
        "actionAttempted": action_attempted,
        "expectedNextAction": expected_next_action,
        "reasonProgressStopped": reason_progress_stopped,
    }


def add_target_metadata(
    payload: dict[str, Any],
    target_starter: dict[str, str] | None,
    *,
    evidence_status: str,
    blocker: dict[str, str] | None,
) -> dict[str, Any]:
    if target_starter is None:
        return payload
    payload.update(
        {
            "targetStarter": target_starter,
            "targetStarterObserved": None,
            "targetStarterSelected": False,
            "targetStarterOpenAttempted": False,
            "openedStarter": None,
            "evidenceStatus": evidence_status,
            "nextBlocker": blocker,
        }
    )
    return payload


def select_project_window_context(java_pid: int, select_project_frame: Any) -> dict[str, Any]:
    return {
        "title": safe_node_name(select_project_frame) or EXPECTED_SELECT_PROJECT_TITLE,
        "role": safe_role_name(select_project_frame),
        "javaPid": java_pid,
        "childCount": safe_child_count(select_project_frame),
        "states": state_names(select_project_frame),
    }


def starters_tab_safety(starters_tab_click: dict[str, Any]) -> dict[str, Any]:
    activated = bool(starters_tab_click.get("success", False))
    return {
        "tabName": "Starters",
        "activationAttempted": bool(starters_tab_click.get("attempted", False)),
        "activatedBeforeTargetSearch": activated,
        "activationDetail": str(starters_tab_click.get("detail", "")),
        "targetSearchScope": "active-starters-tab" if activated else "not-started",
    }


def state_names(node: Any) -> list[str]:
    try:
        state_set = node.getState()
    except Exception:
        return []
    try:
        states = state_set.getStates()
    except Exception:
        return []
    names: list[str] = []
    for state in states:
        if isinstance(state, str):
            names.append(state)
        elif hasattr(state, "name"):
            names.append(str(state.name))
        else:
            names.append(str(state))
    return names


def states_include(states: list[str], expected: str) -> bool:
    expected = expected.lower()
    return any(expected in state.lower() for state in states)


def normalise_role_name(role: str) -> str:
    return role.lower().replace(" ", "")


def node_index_in_parent(node: Any) -> int:
    try:
        return int(node.getIndexInParent())
    except Exception:
        try:
            parent = node.get_parent()
            if parent is None:
                return -1
            for index in range(safe_child_count(parent)):
                try:
                    if parent.getChildAtIndex(index) is node:
                        return index
                except Exception:
                    continue
        except Exception:
            return -1
    return -1


def query_selection_available(node: Any) -> bool:
    try:
        node.querySelection()
        return True
    except Exception:
        return False


def iter_nodes_with_paths(
    node: Any,
    *,
    depth: int = 0,
    path: tuple[int, ...] = (),
    max_depth: int = MAX_DEPTH,
) -> Iterator[tuple[Any, int, tuple[int, ...]]]:
    if node is None or depth > max_depth:
        return
    yield node, depth, path
    if depth < max_depth:
        for index in range(safe_child_count(node)):
            try:
                child = node.getChildAtIndex(index)
            except Exception:
                continue
            if child is not None:
                yield from iter_nodes_with_paths(
                    child,
                    depth=depth + 1,
                    path=path + (index,),
                    max_depth=max_depth,
                )


def child_display_name(node: Any) -> str:
    name = safe_node_name(node)
    if name:
        return name
    for index in range(safe_child_count(node)):
        try:
            child = node.getChildAtIndex(index)
        except Exception:
            continue
        child_name = safe_node_name(child)
        if child_name:
            return child_name
    return ""


def target_observation_record(
    item_node: Any,
    list_node: Any,
    *,
    depth: int,
    tree_path: tuple[int, ...],
    child_index: int,
) -> dict[str, Any]:
    return {
        "name": child_display_name(item_node),
        "role": safe_role_name(item_node),
        "description": safe_node_description(item_node),
        "states": state_names(item_node),
        "availableActions": get_available_actions(item_node),
        "treePath": list(tree_path),
        "depth": depth,
        "indexInParent": node_index_in_parent(item_node),
        "listChildIndex": child_index,
        "listName": safe_node_name(list_node),
        "listRole": safe_role_name(list_node),
        "listStates": state_names(list_node),
        "listChildCount": safe_child_count(list_node),
        "parentSelectionAvailable": query_selection_available(list_node),
    }


def enumerate_all_widgets(
    node: Any, depth: int = 0, max_depth: int = MAX_DEPTH
) -> list[dict[str, Any]]:
    """Recursively enumerate accessible structure without leaking arbitrary UI text."""
    if node is None or depth > max_depth:
        return []
    result: list[dict[str, Any]] = []
    props = widget_tree_properties(node)
    entry = {
        "depth": depth,
        "name": props["name"],
        "role": props["role"],
        "description": props["description"],
        "childCount": props["childCount"],
        "nameRedacted": props["nameRedacted"],
        "descriptionRedacted": props["descriptionRedacted"],
    }
    result.append(entry)
    if depth < max_depth:
        for i in range(props["childCount"]):
            try:
                child = node.getChildAtIndex(i)
                if child is not None:
                    result.extend(enumerate_all_widgets(child, depth + 1, max_depth))
            except Exception:
                continue
    return result


def find_nodes_with_roles(
    node: Any,
    target_roles: frozenset[str],
    depth: int = 0,
    max_depth: int = MAX_DEPTH,
) -> list[NodeInfo]:
    """Find all nodes whose normalised role is in target_roles.

    Returns a list of (node, depth, name, raw_role_name) tuples.
    The normalised role is lower-case with spaces removed.
    """
    return list(iter_nodes_with_roles(node, target_roles, depth=depth, max_depth=max_depth))


def iter_nodes_with_roles(
    node: Any,
    target_roles: frozenset[str],
    depth: int = 0,
    max_depth: int = MAX_DEPTH,
) -> Iterator[NodeInfo]:
    """Yield nodes whose normalised role is in target_roles."""
    if node is None or depth > max_depth:
        return
    raw_role = safe_role_name(node)
    name = safe_node_name(node)
    child_count = safe_child_count(node)
    normalised = normalise_role_name(raw_role)
    if normalised in target_roles:
        yield node, depth, name, raw_role
    if depth < max_depth:
        for i in range(child_count):
            try:
                child = node.getChildAtIndex(i)
                if child is not None:
                    yield from iter_nodes_with_roles(child, target_roles, depth + 1, max_depth)
            except Exception:
                continue


def find_first_named_node_with_roles(
    node: Any,
    target_roles: frozenset[str],
    preferred_names: tuple[str, ...],
) -> Any | None:
    """Find a named control without allocating every matching node first."""
    fallback_node: Any | None = None
    fallback_rank = len(preferred_names)
    name_rank = {name: rank for rank, name in enumerate(preferred_names)}
    for candidate, _depth, name, _raw_role in iter_nodes_with_roles(node, target_roles):
        rank = name_rank.get(name)
        if rank is None:
            continue
        if rank == 0:
            return candidate
        if rank < fallback_rank:
            fallback_node = candidate
            fallback_rank = rank
    return fallback_node


def collect_tab_structure_nodes(
    node: Any,
) -> tuple[list[NodeInfo], list[NodeInfo], list[NodeInfo]]:
    """Collect tab-related nodes in one AT-SPI tree walk."""
    tab_list_infos: list[NodeInfo] = []
    tab_infos: list[NodeInfo] = []
    toggle_infos: list[NodeInfo] = []
    stack: list[tuple[Any, int]] = [(node, 0)] if node is not None else []
    while stack:
        current, depth = stack.pop()
        if depth > MAX_DEPTH:
            continue
        raw_role = safe_role_name(current)
        normalised = normalise_role_name(raw_role)
        if (
            normalised in PAGE_TAB_LIST_ROLES
            or normalised in TAB_ROLES
            or normalised in TOGGLE_TAB_ROLES
        ):
            info = (current, depth, safe_node_name(current), raw_role)
            if normalised in PAGE_TAB_LIST_ROLES:
                tab_list_infos.append(info)
            if normalised in TAB_ROLES:
                tab_infos.append(info)
            if normalised in TOGGLE_TAB_ROLES:
                toggle_infos.append(info)
        if depth < MAX_DEPTH:
            for child_index in range(safe_child_count(current) - 1, -1, -1):
                try:
                    child = current.getChildAtIndex(child_index)
                except Exception:
                    continue
                if child is not None:
                    stack.append((child, depth + 1))
    return tab_list_infos, tab_infos, toggle_infos


def get_available_actions(node: Any) -> list[str]:
    """Return the list of AT-SPI action names for a node."""
    try:
        iface = node.queryAction()
        return [iface.getName(i) for i in range(iface.nActions)]
    except Exception:
        return []


def do_action(node: Any, action_name: str) -> tuple[bool, str]:
    """Attempt to invoke an AT-SPI action by name on node.

    Returns (success, detail_string).
    """
    try:
        iface = node.queryAction()
        target_action = action_name.lower()
        available = []
        for i in range(iface.nActions):
            available_name = iface.getName(i)
            available.append(available_name)
            if available_name.lower() == target_action:
                ok = iface.doAction(i)
                return bool(ok), f"invoked {action_name!r} at index {i}"
        return False, f"action {action_name!r} not found; available: {available}"
    except Exception as exc:
        return False, f"action invocation error: {exc}"


def attempt_tab_clicks(tab_nodes: list[NodeInfo]) -> list[dict[str, Any]]:
    """Attempt AT-SPI click actions on each tab node.

    Returns a list of attempt records.
    """
    attempts: list[dict[str, Any]] = []
    for node, depth, name, raw_role in tab_nodes:
        actions = get_available_actions(node)
        lowered_actions = {action.lower() for action in actions}
        record: dict[str, Any] = {
            "name": name,
            "role": raw_role,
            "depth": depth,
            "description": "",
            "availableActions": actions,
            "clickAttempted": False,
            "clickSuccess": False,
            "clickDetail": "",
        }
        record["description"] = safe_node_description(node)
        for preferred in PREFERRED_ACTIONS:
            if preferred in lowered_actions:
                ok, detail = do_action(node, preferred)
                record["clickAttempted"] = True
                record["clickSuccess"] = ok
                record["clickDetail"] = detail
                if ok:
                    time.sleep(1)
                break
        attempts.append(record)
    return attempts


def empty_tab_probe_payload(
    *,
    status: str,
    blocker: str,
    blocker_detail: str,
    java_pid: int | None,
    project_open_attempt: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "status": status,
        "blocker": blocker,
        "blockerDetail": blocker_detail,
        "javaPid": java_pid,
        "allWidgetTree": [],
        "roleCounts": {},
        "tabListNodes": [],
        "tabNodes": [],
        "tabClickAttempts": [],
        "widgetCountAfterClick": 0,
        "allWidgetTreeAfterClick": [],
        "projectOpenObserved": False,
        "projectOpenAttempt": project_open_attempt or {},
    }


def atspi_desktop_or_probe_payload(
    java_pid: int,
    target_starter: dict[str, str] | None,
) -> tuple[Any | None, dict[str, Any] | None]:
    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return None, add_target_metadata(
            empty_tab_probe_payload(
                status="blocked",
                blocker="pyatspi-not-installed",
                blocker_detail="python3-pyatspi is not installed.",
                java_pid=java_pid,
            ),
            target_starter,
            evidence_status="blocked",
            blocker=next_blocker(
                "AT-SPI probe could not import pyatspi before observing the Select Project tree.",
                "Import python3-pyatspi.",
                "Connect to AT-SPI and inspect the active Starters context.",
                "python3-pyatspi is not installed.",
            ),
        )

    try:
        return pyatspi.Registry.getDesktop(0), None
    except Exception as exc:
        return None, add_target_metadata(
            empty_tab_probe_payload(
                status="blocked",
                blocker="at-spi-registry-unavailable",
                blocker_detail=f"Cannot connect to AT-SPI registry: {exc}",
                java_pid=java_pid,
            ),
            target_starter,
            evidence_status="blocked",
            blocker=next_blocker(
                "AT-SPI registry was not reachable before observing the Select Project tree.",
                "Connect to the AT-SPI registry.",
                "Inspect the active Starters context and locate the target starter.",
                f"Cannot connect to AT-SPI registry: {exc}",
            ),
        )


def find_alice_app(desktop: Any, java_pid: int) -> tuple[Any | None, int]:
    alice_app = None
    app_count = 0
    for _attempt in range(5):
        try:
            app_count = desktop.childCount
            for i in range(app_count):
                try:
                    app = desktop.getChildAtIndex(i)
                    if app is None:
                        continue
                    try:
                        app_pid = app.get_process_id()
                    except Exception:
                        app_pid = None
                    if app_pid == java_pid:
                        alice_app = app
                        break
                except Exception:
                    continue
        except Exception:
            app_count = 0
        if alice_app is not None and alice_app.childCount > 0:
            break
        time.sleep(2)
    return alice_app, app_count


def alice_app_unavailable_payload(
    *,
    java_pid: int,
    app_count: int,
    target_starter: dict[str, str] | None,
) -> dict[str, Any]:
    return add_target_metadata(
        empty_tab_probe_payload(
            status="blocked",
            blocker="atk-wrapper-not-loaded",
            blocker_detail=(
                f"Java process PID {java_pid} not found in AT-SPI registry "
                f"({app_count} total AT-SPI apps visible)."
            ),
            java_pid=java_pid,
        ),
        target_starter,
        evidence_status="blocked",
        blocker=next_blocker(
            f"Java process PID {java_pid} was not present in the AT-SPI registry.",
            "Find the Alice Java process in AT-SPI.",
            "Inspect the active Starters context and locate the target starter.",
            f"ATK wrapper application node was not visible ({app_count} total AT-SPI apps visible).",
        ),
    )


def find_select_project_frame(alice_app: Any) -> Any | None:
    for i in range(safe_child_count(alice_app)):
        try:
            child = alice_app.getChildAtIndex(i)
        except Exception:
            continue
        if child is not None and safe_node_name(child) == EXPECTED_SELECT_PROJECT_TITLE:
            return child
    return None


def top_level_child_summaries(alice_app: Any) -> list[str]:
    top_names = []
    for i in range(min(safe_child_count(alice_app), 20)):
        try:
            child = alice_app.getChildAtIndex(i)
        except Exception:
            continue
        if child is not None:
            top_names.append(f"{safe_node_name(child)!r}({safe_role_name(child)})")
    return top_names


def select_project_unavailable_payload(
    *,
    java_pid: int,
    alice_app: Any,
    target_starter: dict[str, str] | None,
) -> dict[str, Any]:
    top_names = top_level_child_summaries(alice_app)
    blocker_detail = (
        f"No frame named 'Select Project' found. "
        f"Observed top-level children: {top_names[:10]}"
    )
    return add_target_metadata(
        empty_tab_probe_payload(
            status="blocked",
            blocker="select-project-not-accessible",
            blocker_detail=blocker_detail,
            java_pid=java_pid,
        ),
        target_starter,
        evidence_status="blocked",
        blocker=next_blocker(
            f"Top-level AT-SPI children did not include {EXPECTED_SELECT_PROJECT_TITLE!r}.",
            "Locate the Select Project frame.",
            "Activate Starters and inspect its target starter entries.",
            f"Observed top-level children: {top_names[:10]}",
        ),
    )


def role_counts_for(widgets: list[dict[str, Any]]) -> dict[str, int]:
    role_counts: dict[str, int] = {}
    for widget in widgets:
        role = widget.get("role", "")
        role_counts[role] = role_counts.get(role, 0) + 1
    return role_counts


def tab_structure_observation(select_project_frame: Any) -> dict[str, Any]:
    all_widgets_before = enumerate_all_widgets(select_project_frame, depth=0)
    tab_list_infos, tab_infos, toggle_all = collect_tab_structure_nodes(select_project_frame)
    toggle_tab_infos = [
        (node, depth, name, raw_role)
        for node, depth, name, raw_role in toggle_all
        if name in EXPECTED_TAB_LABELS
    ]
    effective_tab_infos = tab_infos if tab_infos else toggle_tab_infos
    tab_click_attempts = attempt_tab_clicks(effective_tab_infos)
    any_click_success = any(a["clickSuccess"] for a in tab_click_attempts)
    all_widgets_after: list[dict[str, Any]] = []
    if any_click_success:
        time.sleep(2)
        all_widgets_after = enumerate_all_widgets(select_project_frame, depth=0)
    return {
        "allWidgetsBefore": all_widgets_before,
        "roleCounts": role_counts_for(all_widgets_before),
        "tabListInfos": tab_list_infos,
        "tabInfos": tab_infos,
        "toggleTabInfos": toggle_tab_infos,
        "tabClickAttempts": tab_click_attempts,
        "anyClickSuccess": any_click_success,
        "allWidgetsAfter": all_widgets_after,
    }


def node_info_records(node_infos: list[NodeInfo]) -> list[dict[str, Any]]:
    return [
        {"name": name, "role": raw_role, "depth": depth}
        for _, depth, name, raw_role in node_infos
    ]


def observed_tab_base_payload(
    *,
    java_pid: int,
    observation: dict[str, Any],
    project_open_result: dict[str, Any],
) -> dict[str, Any]:
    tab_click_attempts = observation["tabClickAttempts"]
    project_open_attempt = dict(project_open_result)
    project_open_attempt.pop("nextBlocker", None)
    payload = {
        "status": "observed",
        "blocker": project_open_result.get("blocker", "none"),
        "blockerDetail": project_open_result.get("blockerDetail", ""),
        "javaPid": java_pid,
        "widgetCount": len(observation["allWidgetsBefore"]),
        "roleCounts": observation["roleCounts"],
        "allWidgetTree": observation["allWidgetsBefore"],
        "tabListNodeCount": len(observation["tabListInfos"]),
        "tabListNodes": node_info_records(observation["tabListInfos"]),
        "tabNodeCount": len(observation["tabInfos"]),
        "tabNodes": node_info_records(observation["tabInfos"]),
        "toggleTabNodeCount": len(observation["toggleTabInfos"]),
        "toggleTabNodes": node_info_records(observation["toggleTabInfos"]),
        "tabNamesFound": [a["name"] for a in tab_click_attempts if a["name"]],
        "tabDescriptionsFound": [a["description"] for a in tab_click_attempts if a["description"]],
        "expectedTabLabels": EXPECTED_TAB_LABELS,
        "tabClickAttempts": tab_click_attempts,
        "anyClickSuccess": observation["anyClickSuccess"],
        "widgetCountAfterClick": len(observation["allWidgetsAfter"]),
        "allWidgetTreeAfterClick": observation["allWidgetsAfter"],
        "projectOpenAttempt": project_open_attempt,
        "projectOpenObserved": project_open_result.get("projectOpenObserved", False),
    }
    return payload


def add_target_observed_metadata(
    payload: dict[str, Any],
    *,
    java_pid: int,
    select_project_frame: Any,
    target_starter: dict[str, str],
    project_open_result: dict[str, Any],
) -> dict[str, Any]:
    payload.update(
        {
            "targetStarter": target_starter,
            "targetStarterObserved": project_open_result.get("targetStarterObserved"),
            "targetStarterSelected": project_open_result.get("targetStarterSelected", False),
            "targetStarterOpenAttempted": project_open_result.get(
                "targetStarterOpenAttempted", False
            ),
            "openedStarter": project_open_result.get("openedStarter"),
            "evidenceStatus": project_open_result.get("evidenceStatus", "blocked"),
            "nextBlocker": project_open_result.get("nextBlocker"),
            "selectProjectWindowContext": select_project_window_context(
                java_pid, select_project_frame
            ),
            "startersTabSafety": project_open_result.get("startersTabSafety"),
        }
    )
    return payload


def observed_tab_probe_payload(
    *,
    java_pid: int,
    select_project_frame: Any,
    alice_app: Any,
    target_starter: dict[str, str] | None,
) -> dict[str, Any]:
    observation = tab_structure_observation(select_project_frame)
    project_open_result = attempt_project_open(
        select_project_frame,
        alice_app,
        target_starter=target_starter,
    )
    payload = observed_tab_base_payload(
        java_pid=java_pid,
        observation=observation,
        project_open_result=project_open_result,
    )
    if target_starter is None:
        return payload
    return add_target_observed_metadata(
        payload,
        java_pid=java_pid,
        select_project_frame=select_project_frame,
        target_starter=target_starter,
        project_open_result=project_open_result,
    )


def probe_tab_click(java_pid: int) -> dict[str, Any]:
    """Main probe: find Alice in AT-SPI, dump tab structure, attempt clicks."""
    target_starter = configured_target_starter()
    target_validation_error = validate_target_starter(target_starter)
    if target_validation_error:
        return target_validation_failed_payload(
            target_starter,
            java_pid=java_pid,
            target_validation_error=target_validation_error,
        )

    desktop, payload = atspi_desktop_or_probe_payload(java_pid, target_starter)
    if payload is not None:
        return payload

    alice_app, app_count = find_alice_app(desktop, java_pid)
    if alice_app is None:
        return alice_app_unavailable_payload(
            java_pid=java_pid,
            app_count=app_count,
            target_starter=target_starter,
        )

    select_project_frame = find_select_project_frame(alice_app)
    if select_project_frame is None:
        return select_project_unavailable_payload(
            java_pid=java_pid,
            alice_app=alice_app,
            target_starter=target_starter,
        )

    return observed_tab_probe_payload(
        java_pid=java_pid,
        select_project_frame=select_project_frame,
        alice_app=alice_app,
        target_starter=target_starter,
    )


def attempt_project_open(
    select_project_frame: Any,
    alice_app: Any,
    *,
    target_starter: dict[str, str] | None = None,
) -> dict[str, Any]:
    """After tab clicks, try to select a Starters project and click OK.

    Strategy:
    1. Click the "Starters" toggle button to make sure that panel is active.
    2. Find the first list inside the frame (the Starters list).
    3. Try to invoke click on the first panel child (a list item).
    4. Find the OK button and invoke click on it.
    5. Wait up to 15 s for the Select Project frame to disappear (title change),
       which is the observable evidence that a project was opened.

    Returns a machine-readable record of each step and its result.
    """
    if target_starter is not None:
        return attempt_target_project_open(select_project_frame, alice_app, target_starter)

    record = default_project_open_record()
    record["startersTabClick"] = select_starters_tab(select_project_frame)

    target_list_node = largest_list_node(select_project_frame)
    record["listItemClick"] = click_first_generic_list_item(target_list_node)
    record["okButtonClick"] = click_ok_button(select_project_frame)

    if not record["okButtonClick"]["success"]:
        record["projectOpenDetail"] = (
            "OK button click did not succeed; project opening not observed."
        )
        return record

    observed, detail = wait_for_generic_project_open(alice_app)
    record["projectOpenObserved"] = observed
    record["projectOpenDetail"] = detail
    return record


def default_project_open_record() -> dict[str, Any]:
    return {
        "startersTabClick": {"attempted": False, "success": False, "detail": ""},
        "listItemClick": {"attempted": False, "success": False, "detail": "", "item": ""},
        "okButtonClick": {"attempted": False, "success": False, "detail": ""},
        "projectOpenObserved": False,
        "projectOpenDetail": "",
    }


def largest_list_node(select_project_frame: Any) -> Any | None:
    list_nodes_raw = find_nodes_with_roles(select_project_frame, LIST_ROLES)
    target_list_node: Any = None
    best_count = 0
    for node, _depth, _name, _role in list_nodes_raw:
        count = safe_child_count(node)
        if count > best_count:
            best_count = count
            target_list_node = node
    return target_list_node


def empty_list_item_click_record(detail: str = "") -> dict[str, Any]:
    return {"attempted": False, "success": False, "detail": detail, "item": ""}


def click_action_record(node: Any, item_name: str) -> dict[str, Any]:
    ok, detail = do_action(node, "click")
    if ok:
        time.sleep(1)
    return {
        "attempted": True,
        "success": ok,
        "detail": detail,
        "item": item_name,
    }


def try_click_list_item_or_label(item_node: Any) -> dict[str, Any] | None:
    item_name = child_display_name(item_node)
    actions_on_item = {action.lower() for action in get_available_actions(item_node)}
    if "click" in actions_on_item:
        return click_action_record(item_node, item_name)
    if safe_child_count(item_node) <= 0:
        return None

    label_child = item_node.getChildAtIndex(0)
    if label_child is None:
        return None
    label_actions = {action.lower() for action in get_available_actions(label_child)}
    if "click" not in label_actions:
        return None
    return click_action_record(label_child, safe_node_name(label_child) or item_name)


def click_first_generic_list_item(target_list_node: Any | None) -> dict[str, Any]:
    no_click_detail = "no list with children found, or no click action on any list item"
    if target_list_node is None:
        return empty_list_item_click_record(no_click_detail)

    for candidate_index in range(min(safe_child_count(target_list_node), 3)):
        try:
            item_node = target_list_node.getChildAtIndex(candidate_index)
            if item_node is None:
                continue
            record = try_click_list_item_or_label(item_node)
            if record is not None:
                return record
        except Exception as exc:
            return empty_list_item_click_record(f"exception on item {candidate_index}: {exc}")
    return empty_list_item_click_record(no_click_detail)


def click_ok_button(select_project_frame: Any) -> dict[str, Any]:
    ok_node = find_ok_or_open_button(select_project_frame)
    if ok_node is None:
        return {
            "attempted": False,
            "success": False,
            "detail": "OK button not found in AT-SPI tree",
        }
    ok, detail = do_action(ok_node, "click")
    return {"attempted": True, "success": ok, "detail": detail}


def wait_for_generic_project_open(alice_app: Any) -> tuple[bool, str]:
    time.sleep(1)
    for _wait in range(15):
        try:
            still_open = False
            for i in range(safe_child_count(alice_app)):
                try:
                    child = alice_app.getChildAtIndex(i)
                    if child is not None and safe_node_name(child) == EXPECTED_SELECT_PROJECT_TITLE:
                        still_open = True
                        break
                except Exception:
                    continue
            if not still_open:
                return True, (
                    "Select Project frame is no longer present in the AT-SPI tree; "
                    "project opening is observed."
                )
        except Exception as exc:
            return False, f"AT-SPI error while waiting for Select Project dismissal: {exc}"
        time.sleep(1)

    return False, (
        "OK button was clicked but the Select Project frame is still present after "
        "15 seconds; either the list item selection did not enable OK or project "
        "loading is still in progress."
    )


def select_starters_tab(select_project_frame: Any) -> dict[str, Any]:
    record: dict[str, Any] = {"attempted": False, "success": False, "detail": ""}
    node = find_first_named_node_with_roles(
        select_project_frame,
        TOGGLE_TAB_ROLES,
        ("Starters",),
    )
    if node is not None:
        ok, detail = do_action(node, "click")
        record = {"attempted": True, "success": ok, "detail": detail}
        if ok:
            time.sleep(1)
        return record
    record["detail"] = "Starters tab toggle button not found in AT-SPI tree"
    return record


def active_starter_lists(select_project_frame: Any) -> list[dict[str, Any]]:
    candidates: list[dict[str, Any]] = []
    for node, depth, path in iter_nodes_with_paths(select_project_frame):
        raw_role = safe_role_name(node)
        if normalise_role_name(raw_role) != "list":
            continue
        states = state_names(node)
        child_count = safe_child_count(node)
        name = safe_node_name(node)
        showing = states_include(states, "showing")
        visible = states_include(states, "visible")
        score = child_count
        if showing:
            score += 10_000
        if visible:
            score += 1_000
        if name == "Starters":
            score += 100
        candidates.append(
            {
                "node": node,
                "depth": depth,
                "path": path,
                "name": name,
                "role": raw_role,
                "states": states,
                "childCount": child_count,
                "score": score,
                "showing": showing,
                "visible": visible,
            }
        )
    showing_candidates = [candidate for candidate in candidates if candidate["showing"]]
    if showing_candidates:
        return sorted(showing_candidates, key=lambda candidate: candidate["score"], reverse=True)
    visible_candidates = [candidate for candidate in candidates if candidate["visible"]]
    if visible_candidates:
        return sorted(visible_candidates, key=lambda candidate: candidate["score"], reverse=True)
    starters_named = [candidate for candidate in candidates if candidate["name"] == "Starters"]
    if starters_named:
        return sorted(starters_named, key=lambda candidate: candidate["score"], reverse=True)
    return sorted(candidates, key=lambda candidate: candidate["score"], reverse=True)[:1]


def starter_list_summary_records(list_records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "name": record["name"],
            "role": record["role"],
            "states": record["states"],
            "childCount": record["childCount"],
            "treePath": list(record["path"]),
            "showing": record["showing"],
            "visible": record["visible"],
        }
        for record in list_records
    ]


def find_target_starter(
    select_project_frame: Any,
    target_display_name: str,
) -> tuple[dict[str, Any] | None, list[dict[str, Any]]]:
    list_records = active_starter_lists(select_project_frame)
    for list_record in list_records:
        list_node = list_record["node"]
        for child_index in range(safe_child_count(list_node)):
            try:
                item_node = list_node.getChildAtIndex(child_index)
            except Exception:
                continue
            if item_node is None:
                continue
            item_name = child_display_name(item_node)
            if item_name != target_display_name:
                continue
            item_path = tuple(list_record["path"]) + (child_index,)
            observation = target_observation_record(
                item_node,
                list_node,
                depth=int(list_record["depth"]) + 1,
                tree_path=item_path,
                child_index=child_index,
            )
            observation.update(
                {
                    "activeListCandidates": starter_list_summary_records(list_records),
                }
            )
            return {
                "itemNode": item_node,
                "listNode": list_node,
                "childIndex": child_index,
                "observation": observation,
            }, list_records
    return None, list_records


def try_target_item_actions(item_node: Any, attempt: dict[str, Any]) -> bool:
    lowered_actions = [action.lower() for action in get_available_actions(item_node)]
    for action_name in PREFERRED_ACTIONS:
        if action_name not in lowered_actions:
            continue
        ok, detail = do_action(item_node, action_name)
        attempt["actionAttempts"].append(
            {
                "action": action_name,
                "success": ok,
                "detail": detail,
            }
        )
        if ok:
            time.sleep(1)
            attempt["selected"] = True
            attempt["detail"] = f"target starter selected via {action_name} action"
            return True
    return False


def try_parent_list_selection(
    list_node: Any,
    child_index: int,
    attempt: dict[str, Any],
) -> bool:
    try:
        selection = list_node.querySelection()
    except Exception as exc:
        attempt["detail"] = (
            "target starter exposed no successful click/select/activate action and "
            f"parent list selection interface was unavailable: {exc}"
        )
        return False

    attempt["parentSelectionAttempted"] = True
    try:
        selected = bool(selection.selectChild(child_index))
        try:
            selected = selected and bool(selection.isChildSelected(child_index))
        except Exception as exc:
            attempt["detail"] = (
                f"parent list selection selected child {child_index}, but verification failed: {exc}"
            )
            return False
    except Exception as exc:
        attempt["detail"] = f"parent list selection interface failed for target child {child_index}: {exc}"
        return False

    attempt["parentSelectionSuccess"] = selected
    attempt["selected"] = selected
    if selected:
        time.sleep(1)
        attempt["detail"] = f"target starter selected via parent list selection child {child_index}"
    else:
        attempt["detail"] = f"parent list selection did not select child {child_index}"
    return selected


def attempt_target_selection(
    target_match: dict[str, Any],
) -> tuple[bool, dict[str, Any]]:
    item_node = target_match["itemNode"]
    list_node = target_match["listNode"]
    child_index = int(target_match["childIndex"])
    attempt: dict[str, Any] = {
        "actionAttempts": [],
        "parentSelectionAttempted": False,
        "parentSelectionSuccess": False,
        "selected": False,
        "detail": "",
    }

    if try_target_item_actions(item_node, attempt):
        return True, attempt

    selected = try_parent_list_selection(list_node, child_index, attempt)
    return selected, attempt


def find_ok_or_open_button(select_project_frame: Any) -> Any | None:
    return find_first_named_node_with_roles(
        select_project_frame,
        BUTTON_ROLES,
        ("OK", "Open"),
    )


def wait_for_select_project_dismissal(alice_app: Any) -> tuple[bool, str]:
    time.sleep(1)
    for _wait in range(15):
        try:
            still_open = False
            for index in range(safe_child_count(alice_app)):
                try:
                    child = alice_app.getChildAtIndex(index)
                except Exception:
                    continue
                if child is not None and safe_node_name(child) == EXPECTED_SELECT_PROJECT_TITLE:
                    still_open = True
                    break
            if not still_open:
                return True, (
                    "Select Project frame is no longer present in the AT-SPI tree; "
                    "target starter project opening is observed."
                )
        except Exception as exc:
            return False, f"AT-SPI error while waiting for Select Project dismissal: {exc}"
        time.sleep(1)
    return False, (
        "OK/Open was clicked after target starter selection, but the Select Project "
        "frame is still present after 15 seconds."
    )


def active_list_summary(list_records: list[dict[str, Any]]) -> str:
    return json.dumps(starter_list_summary_records(list_records), sort_keys=True)


def target_observed_state(record: dict[str, Any]) -> str:
    return json.dumps(record["targetStarterObserved"], sort_keys=True)


def apply_next_blocker(
    record: dict[str, Any],
    *,
    blocker_name: str,
    blocker: dict[str, str],
    evidence_status: str | None = None,
) -> dict[str, Any]:
    if evidence_status is not None:
        record["evidenceStatus"] = evidence_status
    record.update(
        {
            "blocker": blocker_name,
            "blockerDetail": blocker["reasonProgressStopped"],
            "nextBlocker": blocker,
            "projectOpenDetail": blocker["reasonProgressStopped"],
        }
    )
    return record


def target_project_open_record(target_starter: dict[str, str]) -> dict[str, Any]:
    return {
        "targetStarter": target_starter,
        "startersTabClick": {"attempted": False, "success": False, "detail": ""},
        "targetStarterObserved": None,
        "targetStarterSelected": False,
        "targetStarterOpenAttempted": False,
        "openedStarter": None,
        "targetSelectionAttempt": {},
        "okButtonClick": {"attempted": False, "success": False, "detail": ""},
        "projectOpenObserved": False,
        "projectOpenDetail": "",
        "evidenceStatus": "blocked",
        "blocker": "target-starter-not-found",
        "blockerDetail": "",
        "nextBlocker": None,
        "startersTabSafety": starters_tab_safety({}),
    }


def activate_starters_tab_for_target(
    record: dict[str, Any],
    select_project_frame: Any,
    target_display_name: str,
) -> dict[str, Any] | None:
    record["startersTabClick"] = select_starters_tab(select_project_frame)
    record["startersTabSafety"] = starters_tab_safety(record["startersTabClick"])
    if record["startersTabClick"]["success"]:
        return None

    detail = record["startersTabClick"].get("detail") or "Starters tab click did not succeed."
    blocker = next_blocker(
        f"Starters tab activation failed before target search: {detail}",
        "Activate the Starters tab before searching for the target starter.",
        (
            f"Expose the active Starters context, then locate {target_display_name!r}, "
            "select it, and click OK/Open."
        ),
        "Target starter lookup stopped because the Starters tab was not confirmed active.",
    )
    return apply_next_blocker(
        record,
        blocker_name="target-starter-tab-activation-failed",
        blocker=blocker,
    )


def find_target_match_for_open(
    record: dict[str, Any],
    select_project_frame: Any,
    target_display_name: str,
    target_repository_path: str,
) -> tuple[dict[str, Any] | None, dict[str, Any] | None]:
    target_match, list_records = find_target_starter(select_project_frame, target_display_name)
    if target_match is not None:
        return target_match, None

    observed_state = (
        f"Active Starters list candidates after Starters tab activation: {active_list_summary(list_records)}"
    )
    blocker = next_blocker(
        observed_state,
        f"Search active Starters context for starter named {target_display_name!r}.",
        (
            f"Expose and select {target_display_name!r} from the Starters list, then click OK/Open "
            f"and record repositoryPath {target_repository_path!r}."
        ),
        f"{target_display_name!r} was not found in the active Starters AT-SPI list candidates.",
    )
    return None, apply_next_blocker(
        record,
        blocker_name="target-starter-not-found",
        blocker=blocker,
    )


def select_target_starter_for_open(
    record: dict[str, Any],
    target_match: dict[str, Any],
    target_display_name: str,
) -> dict[str, Any]:
    record["targetStarterObserved"] = target_match["observation"]
    selected, selection_attempt = attempt_target_selection(target_match)
    record["targetSelectionAttempt"] = selection_attempt
    record["targetStarterSelected"] = selected
    if selected:
        return record

    blocker = next_blocker(
        target_observed_state(record),
        "Try target item click/select/activate actions, then parent list selection interface.",
        f"Select {target_display_name!r}, then click OK/Open only after target-specific selection evidence.",
        selection_attempt.get("detail", "No target-specific selection method succeeded."),
    )
    return apply_next_blocker(
        record,
        blocker_name="target-starter-selection-unavailable",
        blocker=blocker,
    )


def click_ok_for_target_open(
    record: dict[str, Any],
    select_project_frame: Any,
    target_display_name: str,
) -> dict[str, Any]:
    ok_node = find_ok_or_open_button(select_project_frame)
    if ok_node is None:
        return apply_next_blocker(
            record,
            blocker_name="target-starter-open-not-observed",
            blocker=next_blocker(
                target_observed_state(record),
                f"Click OK/Open after selecting {target_display_name!r}.",
                "Find an OK or Open button and invoke its click action.",
                "No OK or Open button was found in the Select Project AT-SPI tree.",
            ),
            evidence_status="selected",
        )

    ok, detail = do_action(ok_node, "click")
    record["targetStarterOpenAttempted"] = True
    record["okButtonClick"] = {"attempted": True, "success": ok, "detail": detail}
    if ok:
        return record

    return apply_next_blocker(
        record,
        blocker_name="target-starter-open-not-observed",
        blocker=next_blocker(
            target_observed_state(record),
            f"Invoke OK/Open click after selecting {target_display_name!r}.",
            "Observe Select Project dismissal after target-specific selection and OK/Open activation.",
            f"OK/Open action did not succeed: {detail}",
        ),
        evidence_status="selected",
    )


def observe_target_open_result(
    record: dict[str, Any],
    alice_app: Any,
    target_starter: dict[str, str],
    target_display_name: str,
) -> dict[str, Any]:
    project_open_observed, project_open_detail = wait_for_select_project_dismissal(alice_app)
    record["projectOpenObserved"] = project_open_observed
    record["projectOpenDetail"] = project_open_detail
    if project_open_observed:
        record.update(
            {
                "evidenceStatus": "opened",
                "blocker": "none",
                "blockerDetail": "",
                "openedStarter": target_starter,
                "nextBlocker": None,
            }
        )
        return record

    return apply_next_blocker(
        record,
        blocker_name="target-starter-open-not-observed",
        blocker=next_blocker(
            target_observed_state(record),
            f"Clicked OK/Open after selecting {target_display_name!r}.",
            "Observe Select Project dismissal and record openedStarter matching the Africa Full metadata.",
            project_open_detail,
        ),
        evidence_status="selected",
    )


def attempt_target_project_open(
    select_project_frame: Any,
    alice_app: Any,
    target_starter: dict[str, str],
) -> dict[str, Any]:
    target_display_name = target_starter["displayName"]
    record = target_project_open_record(target_starter)

    blocked = activate_starters_tab_for_target(record, select_project_frame, target_display_name)
    if blocked is not None:
        return blocked

    target_match, blocked = find_target_match_for_open(
        record,
        select_project_frame,
        target_display_name,
        target_starter["repositoryPath"],
    )
    if blocked is not None:
        return blocked
    if target_match is None:
        return record

    record = select_target_starter_for_open(record, target_match, target_display_name)
    if not record["targetStarterSelected"]:
        return record

    record = click_ok_for_target_open(record, select_project_frame, target_display_name)
    if not record["okButtonClick"]["success"]:
        return record

    return observe_target_open_result(
        record,
        alice_app,
        target_starter,
        target_display_name,
    )


def not_observed_payload(inventory_path: Path) -> dict[str, Any]:
    target_starter = configured_target_starter()
    blocker_detail = (
        f"No Java window titled '{EXPECTED_SELECT_PROJECT_TITLE}' was found in "
        f"{inventory_path.name}."
    )
    return add_target_metadata(
        {
            "status": "not-observed",
            "blocker": "select-project-window-not-in-inventory",
            "blockerDetail": blocker_detail,
            "javaPid": None,
            "allWidgetTree": [],
            "roleCounts": {},
            "tabListNodes": [],
            "tabNodes": [],
            "tabClickAttempts": [],
            "widgetCountAfterClick": 0,
            "allWidgetTreeAfterClick": [],
            "projectOpenObserved": False,
            "projectOpenAttempt": {},
        },
        target_starter,
        evidence_status="blocked",
        blocker=next_blocker(
            f"{inventory_path.name} did not contain a Select Project Java window.",
            "Read the X window inventory and identify the Select Project Java PID.",
            "Connect to AT-SPI and inspect the active Starters context.",
            blocker_detail,
        ),
    )


def blocked_payload(inventory_path: Path, exc: Exception) -> dict[str, Any]:
    target_starter = configured_target_starter()
    blocker_detail = f"Could not read {inventory_path}: {exc}"
    return add_target_metadata(
        {
            "status": "blocked",
            "blocker": "inventory-unreadable",
            "blockerDetail": blocker_detail,
            "javaPid": None,
            "allWidgetTree": [],
            "roleCounts": {},
            "tabListNodes": [],
            "tabNodes": [],
            "tabClickAttempts": [],
            "widgetCountAfterClick": 0,
            "allWidgetTreeAfterClick": [],
        },
        target_starter,
        evidence_status="blocked",
        blocker=next_blocker(
            f"{inventory_path} could not be read, so no AT-SPI state was observed.",
            "Read x-window-inventory.json before AT-SPI probing.",
            "Identify the Select Project Java PID, then inspect and select the target starter.",
            blocker_detail,
        ),
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inventory", help="Path to x-window-inventory.json")
    parser.add_argument("output", help="Path to write tab-click-observation.json")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    output_path = Path(args.output)

    target_starter = configured_target_starter()
    target_validation_error = validate_target_starter(target_starter)
    if target_validation_error:
        payload = target_validation_failed_payload(
            target_starter,
            java_pid=None,
            target_validation_error=target_validation_error,
        )
    else:
        try:
            inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            payload = blocked_payload(inventory_path, exc)
        else:
            java_pid = find_select_project_java_pid(inventory)
            if java_pid is None:
                payload = not_observed_payload(inventory_path)
            else:
                payload = probe_tab_click(java_pid)

    output_path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
