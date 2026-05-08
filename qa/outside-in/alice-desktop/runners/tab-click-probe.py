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
    return ""


def target_blocker(
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
            "targetStarterBlocker": blocker,
        }
    )
    return payload


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
    """Recursively enumerate ALL accessible children including unnamed nodes."""
    if node is None or depth > max_depth:
        return []
    result: list[dict[str, Any]] = []
    props = node_properties(node)
    entry = {
        "depth": depth,
        "name": props["name"],
        "role": props["role"],
        "description": props["description"],
        "childCount": props["childCount"],
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
) -> list[tuple[Any, int, str, str]]:
    """Find all nodes whose normalised role is in target_roles.

    Returns a list of (node, depth, name, raw_role_name) tuples.
    The normalised role is lower-case with spaces removed.
    """
    if node is None or depth > max_depth:
        return []
    results: list[tuple[Any, int, str, str]] = []
    raw_role = safe_role_name(node)
    name = safe_node_name(node)
    child_count = safe_child_count(node)
    normalised = raw_role.lower().replace(" ", "")
    if normalised in target_roles:
        results.append((node, depth, name, raw_role))
    if depth < max_depth:
        for i in range(child_count):
            try:
                child = node.getChildAtIndex(i)
                if child is not None:
                    results.extend(
                        find_nodes_with_roles(child, target_roles, depth + 1, max_depth)
                    )
            except Exception:
                continue
    return results


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


def attempt_tab_clicks(
    tab_nodes: list[tuple[Any, int, str, str]],
) -> list[dict[str, Any]]:
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


def probe_tab_click(java_pid: int) -> dict[str, Any]:
    """Main probe: find Alice in AT-SPI, dump tab structure, attempt clicks."""
    target_starter = configured_target_starter()
    target_validation_error = validate_target_starter(target_starter)
    if target_validation_error:
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
            blocker=target_blocker(
                "Target starter metadata was provided to the probe but failed local validation.",
                "Validate target starter metadata before connecting to AT-SPI.",
                "Pass a non-empty display name and repository-relative committed starter path.",
                target_validation_error,
            ),
        )
    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return add_target_metadata(
            {
                "status": "blocked",
                "blocker": "pyatspi-not-installed",
                "blockerDetail": "python3-pyatspi is not installed.",
                "javaPid": java_pid,
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
            blocker=target_blocker(
                "AT-SPI probe could not import pyatspi before observing the Select Project tree.",
                "Import python3-pyatspi.",
                "Connect to AT-SPI and inspect the active Starters context.",
                "python3-pyatspi is not installed.",
            ),
        )

    try:
        desktop = pyatspi.Registry.getDesktop(0)
    except Exception as exc:
        return add_target_metadata(
            {
                "status": "blocked",
                "blocker": "at-spi-registry-unavailable",
                "blockerDetail": f"Cannot connect to AT-SPI registry: {exc}",
                "javaPid": java_pid,
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
            blocker=target_blocker(
                "AT-SPI registry was not reachable before observing the Select Project tree.",
                "Connect to the AT-SPI registry.",
                "Inspect the active Starters context and locate the target starter.",
                f"Cannot connect to AT-SPI registry: {exc}",
            ),
        )

    # Find Alice by PID with retries.
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

    if alice_app is None:
        return add_target_metadata(
            {
                "status": "blocked",
                "blocker": "atk-wrapper-not-loaded",
                "blockerDetail": (
                    f"Java process PID {java_pid} not found in AT-SPI registry "
                    f"({app_count} total AT-SPI apps visible)."
                ),
                "javaPid": java_pid,
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
            blocker=target_blocker(
                f"Java process PID {java_pid} was not present in the AT-SPI registry.",
                "Find the Alice Java process in AT-SPI.",
                "Inspect the active Starters context and locate the target starter.",
                f"ATK wrapper application node was not visible ({app_count} total AT-SPI apps visible).",
            ),
        )

    # Locate the Select Project frame.
    select_project_frame = None
    for i in range(alice_app.childCount):
        try:
            child = alice_app.getChildAtIndex(i)
            if child is not None and child.name == EXPECTED_SELECT_PROJECT_TITLE:
                select_project_frame = child
                break
        except Exception:
            continue

    if select_project_frame is None:
        top_names = []
        for i in range(min(alice_app.childCount, 20)):
            try:
                child = alice_app.getChildAtIndex(i)
                if child is not None:
                    top_names.append(f"{safe_node_name(child)!r}({safe_role_name(child)})")
            except Exception:
                continue
        return add_target_metadata(
            {
                "status": "blocked",
                "blocker": "select-project-not-accessible",
                "blockerDetail": (
                    f"No frame named 'Select Project' found. "
                    f"Observed top-level children: {top_names[:10]}"
                ),
                "javaPid": java_pid,
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
            blocker=target_blocker(
                f"Top-level AT-SPI children did not include {EXPECTED_SELECT_PROJECT_TITLE!r}.",
                "Locate the Select Project frame.",
                "Activate Starters and inspect its target starter entries.",
                f"Observed top-level children: {top_names[:10]}",
            ),
        )

    # Dump the full tree including unnamed nodes.
    all_widgets_before = enumerate_all_widgets(select_project_frame, depth=0)

    role_counts: dict[str, int] = {}
    for w in all_widgets_before:
        r = w.get("role", "")
        role_counts[r] = role_counts.get(r, 0) + 1

    # Locate tab list containers and tab page nodes.
    tab_list_infos = find_nodes_with_roles(select_project_frame, PAGE_TAB_LIST_ROLES)
    tab_infos = find_nodes_with_roles(select_project_frame, TAB_ROLES)
    # Alice's Select Project uses custom toggle buttons for its tab selectors;
    # filter to those whose name is one of the expected tab labels.
    toggle_all = find_nodes_with_roles(select_project_frame, TOGGLE_TAB_ROLES)
    toggle_tab_infos = [
        (node, depth, name, raw_role)
        for node, depth, name, raw_role in toggle_all
        if name in EXPECTED_TAB_LABELS
    ]
    # Use toggle-button tabs if no standard page-tab nodes found.
    effective_tab_infos = tab_infos if tab_infos else toggle_tab_infos

    # Attempt AT-SPI click actions on each tab (prefer toggle-button tabs).
    tab_click_attempts = attempt_tab_clicks(effective_tab_infos)

    # Re-enumerate the tree if at least one click succeeded, to capture the result.
    any_click_success = any(a["clickSuccess"] for a in tab_click_attempts)
    all_widgets_after: list[dict[str, Any]] = []
    if any_click_success:
        time.sleep(2)
        all_widgets_after = enumerate_all_widgets(select_project_frame, depth=0)

    tab_names_found = [a["name"] for a in tab_click_attempts if a["name"]]
    tab_descriptions_found = [a["description"] for a in tab_click_attempts if a["description"]]

    # Attempt project selection and opening via the Starters list + OK button.
    project_open_result = attempt_project_open(
        select_project_frame,
        alice_app,
        target_starter=target_starter,
    )

    payload = {
        "status": "observed",
        "blocker": project_open_result.get("blocker", "none"),
        "blockerDetail": project_open_result.get("blockerDetail", ""),
        "javaPid": java_pid,
        "widgetCount": len(all_widgets_before),
        "roleCounts": role_counts,
        "allWidgetTree": all_widgets_before,
        "tabListNodeCount": len(tab_list_infos),
        "tabListNodes": [
            {"name": name, "role": raw_role, "depth": depth}
            for _, depth, name, raw_role in tab_list_infos
        ],
        "tabNodeCount": len(tab_infos),
        "tabNodes": [
            {"name": name, "role": raw_role, "depth": depth}
            for _, depth, name, raw_role in tab_infos
        ],
        "toggleTabNodeCount": len(toggle_tab_infos),
        "toggleTabNodes": [
            {"name": name, "role": raw_role, "depth": depth}
            for _, depth, name, raw_role in toggle_tab_infos
        ],
        "tabNamesFound": tab_names_found,
        "tabDescriptionsFound": tab_descriptions_found,
        "expectedTabLabels": EXPECTED_TAB_LABELS,
        "tabClickAttempts": tab_click_attempts,
        "anyClickSuccess": any_click_success,
        "widgetCountAfterClick": len(all_widgets_after),
        "allWidgetTreeAfterClick": all_widgets_after,
        "projectOpenAttempt": project_open_result,
        "projectOpenObserved": project_open_result.get("projectOpenObserved", False),
    }
    if target_starter is not None:
        payload.update(
            {
                "targetStarter": target_starter,
                "targetStarterObserved": project_open_result.get("targetStarterObserved"),
                "targetStarterSelected": project_open_result.get("targetStarterSelected", False),
                "targetStarterOpenAttempted": project_open_result.get("targetStarterOpenAttempted", False),
                "openedStarter": project_open_result.get("openedStarter"),
                "evidenceStatus": project_open_result.get("evidenceStatus", "blocked"),
                "targetStarterBlocker": project_open_result.get("targetStarterBlocker"),
            }
        )
    return payload


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
    5. Wait up to 8 s for the Select Project frame to disappear (title change),
       which is the observable evidence that a project was opened.

    Returns a machine-readable record of each step and its result.
    """
    if target_starter is not None:
        return attempt_target_project_open(select_project_frame, alice_app, target_starter)

    record: dict[str, Any] = {
        "startersTabClick": {"attempted": False, "success": False, "detail": ""},
        "listItemClick": {"attempted": False, "success": False, "detail": "", "item": ""},
        "okButtonClick": {"attempted": False, "success": False, "detail": ""},
        "projectOpenObserved": False,
        "projectOpenDetail": "",
    }

    # Step 1 – re-click the "Starters" tab so its panel is active.
    starters_node: Any = None
    starters_infos = find_nodes_with_roles(select_project_frame, TOGGLE_TAB_ROLES)
    for node, _depth, name, _role in starters_infos:
        if name == "Starters":
            starters_node = node
            break

    if starters_node is not None:
        ok, detail = do_action(starters_node, "click")
        record["startersTabClick"] = {"attempted": True, "success": ok, "detail": detail}
        if ok:
            time.sleep(1)

    # Step 2 – locate the Starters list specifically (the list with the most children,
    # i.e., the starters list with 34 items vs blank slates with 19).
    # All panels are always in the AT-SPI tree even when hidden; we need the one
    # that corresponds to the active "Starters" tab.  The Starters list has more
    # children than any other list in the dialog (34 vs 19 for Blank Slates).
    list_nodes_raw = find_nodes_with_roles(select_project_frame, LIST_ROLES)
    # Pick the list with the most children (Starters).
    target_list_node: Any = None
    best_count = 0
    for node, _depth, _name, _role in list_nodes_raw:
        count = safe_child_count(node)
        if count > best_count:
            best_count = count
            target_list_node = node

    if target_list_node is not None:
        # Step 3 – try to click the first child panel (or its label child).
        for candidate_index in range(min(safe_child_count(target_list_node), 3)):
            try:
                item_node = target_list_node.getChildAtIndex(candidate_index)
                if item_node is None:
                    continue
                item_name = ""
                item_name = child_display_name(item_node)
                actions_on_item = get_available_actions(item_node)
                if "click" in [a.lower() for a in actions_on_item]:
                    ok, detail = do_action(item_node, "click")
                    record["listItemClick"] = {
                        "attempted": True,
                        "success": ok,
                        "detail": detail,
                        "item": item_name,
                    }
                    if ok:
                        time.sleep(1)
                    break
                # No click action directly; try first label child.
                if safe_child_count(item_node) > 0:
                    label_child = item_node.getChildAtIndex(0)
                    if label_child is not None:
                        label_actions = get_available_actions(label_child)
                        if "click" in [a.lower() for a in label_actions]:
                            label_name = safe_node_name(label_child)
                            ok, detail = do_action(label_child, "click")
                            record["listItemClick"] = {
                                "attempted": True,
                                "success": ok,
                                "detail": detail,
                                "item": label_name or item_name,
                            }
                            if ok:
                                time.sleep(1)
                            break
            except Exception as exc:
                record["listItemClick"]["detail"] = f"exception on item {candidate_index}: {exc}"
                break

    if not record["listItemClick"]["attempted"]:
        record["listItemClick"]["detail"] = (
            "no list with children found, or no click action on any list item"
        )

    # Step 4 – click OK.
    ok_buttons = find_nodes_with_roles(select_project_frame, BUTTON_ROLES)
    ok_node: Any = None
    for node, _depth, name, _role in ok_buttons:
        if name == "OK":
            ok_node = node
            break

    if ok_node is not None:
        ok, detail = do_action(ok_node, "click")
        record["okButtonClick"] = {"attempted": True, "success": ok, "detail": detail}
    else:
        record["okButtonClick"]["detail"] = "OK button not found in AT-SPI tree"

    if not record["okButtonClick"]["success"]:
        record["projectOpenDetail"] = (
            "OK button click did not succeed; project opening not observed."
        )
        return record

    # Step 5 – wait for Select Project frame to disappear (project loading).
    time.sleep(1)
    for _wait in range(15):
        try:
            # If the frame is still present, Select Project is still open.
            still_open = False
            for i in range(alice_app.childCount):
                try:
                    child = alice_app.getChildAtIndex(i)
                    if child is not None and child.name == EXPECTED_SELECT_PROJECT_TITLE:
                        still_open = True
                        break
                except Exception:
                    continue
            if not still_open:
                record["projectOpenObserved"] = True
                record["projectOpenDetail"] = (
                    "Select Project frame is no longer present in the AT-SPI tree; "
                    "project opening is observed."
                )
                return record
        except Exception as exc:
            record["projectOpenDetail"] = (
                f"AT-SPI error while waiting for Select Project dismissal: {exc}"
            )
            return record
        time.sleep(1)

    record["projectOpenDetail"] = (
        "OK button was clicked but the Select Project frame is still present after "
        "15 seconds; either the list item selection did not enable OK or project "
        "loading is still in progress."
    )
    return record


def select_starters_tab(select_project_frame: Any) -> dict[str, Any]:
    record: dict[str, Any] = {"attempted": False, "success": False, "detail": ""}
    starters_infos = find_nodes_with_roles(select_project_frame, TOGGLE_TAB_ROLES)
    for node, _depth, name, _role in starters_infos:
        if name == "Starters":
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
        if raw_role.lower().replace(" ", "") != "list":
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
            return True, attempt

    try:
        selection = list_node.querySelection()
    except Exception as exc:
        attempt["detail"] = (
            "target starter exposed no successful click/select/activate action and "
            f"parent list selection interface was unavailable: {exc}"
        )
        return False, attempt

    attempt["parentSelectionAttempted"] = True
    try:
        selected = bool(selection.selectChild(child_index))
        try:
            selected = selected and bool(selection.isChildSelected(child_index))
        except Exception as exc:
            attempt["detail"] = (
                f"parent list selection selected child {child_index}, but verification failed: {exc}"
            )
            return False, attempt
    except Exception as exc:
        attempt["detail"] = f"parent list selection interface failed for target child {child_index}: {exc}"
        return False, attempt

    attempt["parentSelectionSuccess"] = selected
    attempt["selected"] = selected
    if selected:
        time.sleep(1)
        attempt["detail"] = f"target starter selected via parent list selection child {child_index}"
    else:
        attempt["detail"] = f"parent list selection did not select child {child_index}"
    return selected, attempt


def find_ok_or_open_button(select_project_frame: Any) -> Any | None:
    ok_buttons = find_nodes_with_roles(select_project_frame, BUTTON_ROLES)
    for preferred_name in ("OK", "Open"):
        for node, _depth, name, _role in ok_buttons:
            if name == preferred_name:
                return node
    return None


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


def apply_target_blocker(
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
            "targetStarterBlocker": blocker,
            "projectOpenDetail": blocker["reasonProgressStopped"],
        }
    )
    return record


def attempt_target_project_open(
    select_project_frame: Any,
    alice_app: Any,
    target_starter: dict[str, str],
) -> dict[str, Any]:
    target_display_name = target_starter["displayName"]
    target_repository_path = target_starter["repositoryPath"]
    record: dict[str, Any] = {
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
        "targetStarterBlocker": None,
    }

    record["startersTabClick"] = select_starters_tab(select_project_frame)
    if not record["startersTabClick"]["success"]:
        detail = record["startersTabClick"].get("detail") or "Starters tab click did not succeed."
        blocker = target_blocker(
            f"Starters tab activation failed before target search: {detail}",
            "Activate the Starters tab before searching for the target starter.",
            (
                f"Expose the active Starters context, then locate {target_display_name!r}, "
                "select it, and click OK/Open."
            ),
            "Target starter lookup stopped because the Starters tab was not confirmed active.",
        )
        return apply_target_blocker(
            record,
            blocker_name="target-starter-tab-activation-failed",
            blocker=blocker,
        )

    target_match, list_records = find_target_starter(select_project_frame, target_display_name)
    if target_match is None:
        observed_state = (
            f"Active Starters list candidates after Starters tab activation: {active_list_summary(list_records)}"
        )
        blocker = target_blocker(
            observed_state,
            f"Search active Starters context for starter named {target_display_name!r}.",
            (
                f"Expose and select {target_display_name!r} from the Starters list, then click OK/Open "
                f"and record repositoryPath {target_repository_path!r}."
            ),
            f"{target_display_name!r} was not found in the active Starters AT-SPI list candidates.",
        )
        return apply_target_blocker(
            record,
            blocker_name="target-starter-not-found",
            blocker=blocker,
        )

    record["targetStarterObserved"] = target_match["observation"]
    selected, selection_attempt = attempt_target_selection(target_match)
    record["targetSelectionAttempt"] = selection_attempt
    record["targetStarterSelected"] = selected
    if not selected:
        blocker = target_blocker(
            target_observed_state(record),
            "Try target item click/select/activate actions, then parent list selection interface.",
            f"Select {target_display_name!r}, then click OK/Open only after target-specific selection evidence.",
            selection_attempt.get("detail", "No target-specific selection method succeeded."),
        )
        return apply_target_blocker(
            record,
            blocker_name="target-starter-selection-unavailable",
            blocker=blocker,
        )

    ok_node = find_ok_or_open_button(select_project_frame)
    if ok_node is None:
        blocker = target_blocker(
            target_observed_state(record),
            f"Click OK/Open after selecting {target_display_name!r}.",
            "Find an OK or Open button and invoke its click action.",
            "No OK or Open button was found in the Select Project AT-SPI tree.",
        )
        return apply_target_blocker(
            record,
            blocker_name="target-starter-open-not-observed",
            blocker=blocker,
            evidence_status="selected",
        )

    ok, detail = do_action(ok_node, "click")
    record["targetStarterOpenAttempted"] = True
    record["okButtonClick"] = {"attempted": True, "success": ok, "detail": detail}
    if not ok:
        blocker = target_blocker(
            target_observed_state(record),
            f"Invoke OK/Open click after selecting {target_display_name!r}.",
            "Observe Select Project dismissal after target-specific selection and OK/Open activation.",
            f"OK/Open action did not succeed: {detail}",
        )
        return apply_target_blocker(
            record,
            blocker_name="target-starter-open-not-observed",
            blocker=blocker,
            evidence_status="selected",
        )

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
                "targetStarterBlocker": None,
            }
        )
        return record

    blocker = target_blocker(
        target_observed_state(record),
        f"Clicked OK/Open after selecting {target_display_name!r}.",
        "Observe Select Project dismissal and record openedStarter matching the Africa Full metadata.",
        project_open_detail,
    )
    return apply_target_blocker(
        record,
        blocker_name="target-starter-open-not-observed",
        blocker=blocker,
        evidence_status="selected",
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
        blocker=target_blocker(
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
        blocker=target_blocker(
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
