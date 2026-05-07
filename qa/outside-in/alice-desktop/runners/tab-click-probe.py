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
import time
from pathlib import Path
from typing import Any

ATK_WRAPPER_JAR = "/usr/share/java/java-atk-wrapper.jar"
EXPECTED_SELECT_PROJECT_TITLE = "Select Project"
EXPECTED_TAB_LABELS = ["Blank Slates", "Starters", "My Projects", "Recent", "File System"]
# Depth limit high enough to see tab children even if they are nested
MAX_DEPTH = 12
# Alice's Select Project uses custom toggle buttons for tabs rather than standard
# JTabbedPane page tabs.  These are the AT-SPI role strings to search for.
TAB_ROLES = frozenset({"pagetab", "page tab"})
TOGGLE_TAB_ROLES = frozenset({"togglebutton", "toggle button"})


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


def node_properties(node: Any) -> dict[str, Any]:
    """Extract safe scalar properties from an AT-SPI node."""
    props: dict[str, Any] = {}
    try:
        props["name"] = node.name or ""
    except Exception:
        props["name"] = ""
    try:
        props["role"] = node.getRoleName()
    except Exception:
        props["role"] = ""
    try:
        props["description"] = node.description or ""
    except Exception:
        props["description"] = ""
    try:
        props["childCount"] = node.childCount
    except Exception:
        props["childCount"] = 0
    return props


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
    try:
        raw_role = node.getRoleName()
        name = node.name or ""
        child_count = node.childCount
    except Exception:
        return results
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
        for i in range(iface.nActions):
            if iface.getName(i).lower() == action_name.lower():
                ok = iface.doAction(i)
                return bool(ok), f"invoked {action_name!r} at index {i}"
        available = [iface.getName(i) for i in range(iface.nActions)]
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
        try:
            record["description"] = node.description or ""
        except Exception:
            pass
        # Prefer "click" but accept any action that suggests selection/activation.
        action_priority = ["click", "select", "activate"]
        for preferred in action_priority:
            if preferred in [a.lower() for a in actions]:
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
    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return {
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
        }

    try:
        desktop = pyatspi.Registry.getDesktop(0)
    except Exception as exc:
        return {
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
        }

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
            pass
        if alice_app is not None and alice_app.childCount > 0:
            break
        time.sleep(2)

    if alice_app is None:
        return {
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
        }

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
                    top_names.append(f"{child.name!r}({child.getRoleName()})")
            except Exception:
                pass
        return {
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
        }

    # Dump the full tree including unnamed nodes.
    all_widgets_before = enumerate_all_widgets(select_project_frame, depth=0)

    role_counts: dict[str, int] = {}
    for w in all_widgets_before:
        r = w.get("role", "")
        role_counts[r] = role_counts.get(r, 0) + 1

    # Locate tab list containers and tab page nodes.
    tab_list_infos = find_nodes_with_roles(
        select_project_frame, frozenset({"pagetablist", "page tab list"})
    )
    tab_infos = find_nodes_with_roles(
        select_project_frame, frozenset({"pagetab", "page tab"})
    )
    # Alice's Select Project uses custom toggle buttons for its tab selectors;
    # filter to those whose name is one of the expected tab labels.
    toggle_all = find_nodes_with_roles(
        select_project_frame, frozenset({"togglebutton", "toggle button"})
    )
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
    project_open_result = attempt_project_open(select_project_frame, alice_app)

    return {
        "status": "observed",
        "blocker": "none",
        "blockerDetail": "",
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


def attempt_project_open(
    select_project_frame: Any,
    alice_app: Any,
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
    record: dict[str, Any] = {
        "startersTabClick": {"attempted": False, "success": False, "detail": ""},
        "listItemClick": {"attempted": False, "success": False, "detail": "", "item": ""},
        "okButtonClick": {"attempted": False, "success": False, "detail": ""},
        "projectOpenObserved": False,
        "projectOpenDetail": "",
    }

    # Step 1 – re-click the "Starters" tab so its panel is active.
    starters_node: Any = None
    starters_infos = find_nodes_with_roles(
        select_project_frame, frozenset({"togglebutton", "toggle button"})
    )
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
    list_nodes_raw = find_nodes_with_roles(
        select_project_frame, frozenset({"list"})
    )
    # Pick the list with the most children (Starters).
    target_list_node: Any = None
    best_count = 0
    for node, _depth, _name, _role in list_nodes_raw:
        try:
            count = node.childCount
            if count > best_count:
                best_count = count
                target_list_node = node
        except Exception:
            continue

    if target_list_node is not None:
        # Step 3 – try to click the first child panel (or its label child).
        for candidate_index in range(min(target_list_node.childCount, 3)):
            try:
                item_node = target_list_node.getChildAtIndex(candidate_index)
                if item_node is None:
                    continue
                item_name = ""
                try:
                    item_name = item_node.name or ""
                    # If the panel itself has no name, try its first label child.
                    if not item_name and item_node.childCount > 0:
                        child = item_node.getChildAtIndex(0)
                        if child is not None:
                            item_name = child.name or ""
                except Exception:
                    pass
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
                if item_node.childCount > 0:
                    label_child = item_node.getChildAtIndex(0)
                    if label_child is not None:
                        label_actions = get_available_actions(label_child)
                        if "click" in [a.lower() for a in label_actions]:
                            label_name = ""
                            try:
                                label_name = label_child.name or ""
                            except Exception:
                                pass
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
    ok_buttons = find_nodes_with_roles(
        select_project_frame, frozenset({"button", "push button"})
    )
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
        except Exception:
            pass
        time.sleep(1)

    record["projectOpenDetail"] = (
        "OK button was clicked but the Select Project frame is still present after "
        "15 seconds; either the list item selection did not enable OK or project "
        "loading is still in progress."
    )
    return record


def not_observed_payload(inventory_path: Path) -> dict[str, Any]:
    return {
        "status": "not-observed",
        "blocker": "select-project-window-not-in-inventory",
        "blockerDetail": (
            f"No Java window titled '{EXPECTED_SELECT_PROJECT_TITLE}' was found in "
            f"{inventory_path.name}."
        ),
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
    }


def blocked_payload(inventory_path: Path, exc: Exception) -> dict[str, Any]:
    return {
        "status": "blocked",
        "blocker": "inventory-unreadable",
        "blockerDetail": f"Could not read {inventory_path}: {exc}",
        "javaPid": None,
        "allWidgetTree": [],
        "roleCounts": {},
        "tabListNodes": [],
        "tabNodes": [],
        "tabClickAttempts": [],
        "widgetCountAfterClick": 0,
        "allWidgetTreeAfterClick": [],
    }


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
