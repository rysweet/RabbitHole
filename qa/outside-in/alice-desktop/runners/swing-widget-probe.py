#!/usr/bin/env python3
"""Probe the live AT-SPI accessibility tree for the Alice Select Project window.

Reads the X window inventory written by run-scenario.sh, finds the Alice Java
process PID, then queries the AT-SPI registry with pyatspi to enumerate the
Select Project frame's accessible children (tab labels, buttons, panels).

Requires:
- python3-pyatspi installed (sudo apt-get install -y python3-pyatspi)
- Alice launched with the ATK wrapper registered in the Java process. Note that
  exec:java (Maven exec-maven-plugin) uses an isolated URLClassLoader and does not
  honour CLASSPATH or additionalClasspathElements for Toolkit.loadAssistiveTechnologies.
  If Alice is not in the AT-SPI registry the probe records the exact atk-wrapper-not-loaded
  blocker with the precise steps needed to enable introspection.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

ATK_WRAPPER_JAR = "/usr/share/java/java-atk-wrapper.jar"
ATK_WRAPPER_CLASS = "org.GNOME.Accessibility.AtkWrapper"
EXPECTED_SELECT_PROJECT_TITLE = "Select Project"
EXPECTED_TAB_LABELS = ["Blank Slates", "Starters", "My Projects", "Recent", "File System"]


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


def enumerate_accessible_children(
    node: Any, depth: int = 0, max_depth: int = 8
) -> list[dict[str, Any]]:
    """Recursively enumerate accessible children up to max_depth."""
    if node is None or depth > max_depth:
        return []
    result: list[dict[str, Any]] = []
    try:
        role = node.getRoleName()
        name = node.name or ""
        child_count = node.childCount
    except Exception:
        return result
    entry: dict[str, Any] = {"name": name, "role": role, "depth": depth}
    result.append(entry)
    if depth < max_depth:
        for i in range(child_count):
            try:
                child = node.getChildAtIndex(i)
                if child is not None:
                    result.extend(
                        enumerate_accessible_children(child, depth + 1, max_depth)
                    )
            except Exception:
                continue
    return result


def probe_at_spi(java_pid: int) -> dict[str, Any]:
    """Attempt to enumerate the Select Project accessible widget tree via AT-SPI."""
    import time  # noqa: PLC0415

    try:
        import pyatspi  # noqa: PLC0415
    except ImportError:
        return {
            "status": "blocked",
            "blocker": "pyatspi-not-installed",
            "blockerDetail": (
                "python3-pyatspi is not installed. "
                "Install with: sudo apt-get install -y python3-pyatspi"
            ),
            "javaPid": java_pid,
            "atkWrapperJar": ATK_WRAPPER_JAR,
            "atkWrapperClass": ATK_WRAPPER_CLASS,
            "observedAppName": "",
            "observedChildCount": 0,
            "tabLabels": [],
            "widgetLabels": [],
        }

    try:
        desktop = pyatspi.Registry.getDesktop(0)
    except Exception as exc:
        return {
            "status": "blocked",
            "blocker": "at-spi-registry-unavailable",
            "blockerDetail": f"Cannot connect to AT-SPI registry: {exc}",
            "javaPid": java_pid,
            "atkWrapperJar": ATK_WRAPPER_JAR,
            "atkWrapperClass": ATK_WRAPPER_CLASS,
            "observedAppName": "",
            "observedChildCount": 0,
            "tabLabels": [],
            "widgetLabels": [],
        }

    alice_app = None
    app_count = 0
    # Retry up to 5 times with 2-second gaps to allow the AT-SPI tree to populate.
    for _attempt in range(5):
        try:
            desktop = pyatspi.Registry.getDesktop(0)
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
        except Exception as exc:
            return {
                "status": "blocked",
                "blocker": "at-spi-desktop-enumeration-failed",
                "blockerDetail": f"Failed to enumerate AT-SPI desktop apps: {exc}",
                "javaPid": java_pid,
                "atkWrapperJar": ATK_WRAPPER_JAR,
                "atkWrapperClass": ATK_WRAPPER_CLASS,
                "observedAppName": "",
                "observedChildCount": 0,
                "tabLabels": [],
                "widgetLabels": [],
            }
        # If Alice is found and has children, stop retrying.
        if alice_app is not None and alice_app.childCount > 0:
            break
        time.sleep(2)

    if alice_app is None:
        return {
            "status": "blocked",
            "blocker": "atk-wrapper-not-loaded",
            "blockerDetail": (
                f"Java process PID {java_pid} is visible in the X window inventory but is "
                f"not registered in the AT-SPI accessibility tree "
                f"({app_count} total AT-SPI apps visible). "
                f"Alice is running via exec:java (Maven exec-maven-plugin) which uses an "
                f"isolated URLClassLoader. The ATK wrapper must be loaded from that "
                f"classloader, but exec:java's Toolkit.loadAssistiveTechnologies() "
                f"fallback uses the AppClassLoader instead of the context classloader, "
                f"preventing the class from loading even when the jar is on the classpath. "
                f"To enable Swing widget introspection from this JVM: "
                f"(1) run Alice as a standalone 'java' process (exec:exec) so CLASSPATH "
                f"is honoured at JVM startup, setting CLASSPATH={ATK_WRAPPER_JAR} and "
                f"JAVA_TOOL_OPTIONS='-Djavax.accessibility.assistive_technologies="
                f"{ATK_WRAPPER_CLASS}'; "
                f"OR (2) add {ATK_WRAPPER_JAR} as a <dependency> in alice-ide/pom.xml "
                f"so it appears on exec:java's compile-scope URLClassLoader. "
                f"libatk-wrapper-java 0.42.1 is installed at {ATK_WRAPPER_JAR}."
            ),
            "javaPid": java_pid,
            "atSpiDesktopAppCount": app_count,
            "atkWrapperJar": ATK_WRAPPER_JAR,
            "atkWrapperClass": ATK_WRAPPER_CLASS,
            "observedAppName": "",
            "observedChildCount": 0,
            "tabLabels": [],
            "widgetLabels": [],
        }

    app_name = alice_app.name or ""
    app_child_count = alice_app.childCount

    select_project_frame = None
    for i in range(app_child_count):
        try:
            child = alice_app.getChildAtIndex(i)
            if child is None:
                continue
            if child.name == EXPECTED_SELECT_PROJECT_TITLE:
                select_project_frame = child
                break
        except Exception:
            continue

    if select_project_frame is None:
        top_names: list[str] = []
        for i in range(min(app_child_count, 20)):
            try:
                child = alice_app.getChildAtIndex(i)
                if child is not None:
                    top_names.append(f"{child.name!r}({child.getRoleName()})")
            except Exception:
                pass
        zero_children_detail = (
            " The libatk-wrapper.so JNI bridge in the JRE registered the process with "
            "AT-SPI but did not map Swing components to AT-SPI nodes. The Java ATK "
            "wrapper class (org.GNOME.Accessibility.AtkWrapper) must be initialized in "
            "exec:java's thread context to bridge Swing components. "
            "To fix: run Alice as exec:exec (separate JVM) with CLASSPATH="
            f"{ATK_WRAPPER_JAR} and JAVA_TOOL_OPTIONS="
            "'-Djavax.accessibility.assistive_technologies="
            f"{ATK_WRAPPER_CLASS}'; "
            f"or add {ATK_WRAPPER_JAR} as a Maven <dependency> in alice-ide/pom.xml."
        ) if app_child_count == 0 else ""
        return {
            "status": "blocked",
            "blocker": "select-project-not-accessible",
            "blockerDetail": (
                f"Alice (PID {java_pid}, AT-SPI name {app_name!r}) has {app_child_count} "
                f"accessible top-level children but none named 'Select Project'. "
                f"Observed: {top_names[:10]}.{zero_children_detail}"
            ),
            "javaPid": java_pid,
            "observedAppName": app_name,
            "observedChildCount": app_child_count,
            "observedTopLevelChildren": top_names[:10],
            "atkWrapperJar": ATK_WRAPPER_JAR,
            "tabLabels": [],
            "widgetLabels": [],
        }

    all_widgets = enumerate_accessible_children(select_project_frame, depth=0, max_depth=12)

    tab_role_names = {"page tab", "pagetab"}
    # Alice's Select Project uses custom toggle buttons for its tab selectors.
    # Collect names from both standard page-tab roles and toggle-button nodes
    # whose name matches an expected tab label.
    tab_labels_from_page_tab = [
        w["name"]
        for w in all_widgets
        if w.get("role", "").lower().replace(" ", "") in tab_role_names
        and w.get("name")
    ]
    tab_labels_from_toggle = [
        w["name"]
        for w in all_widgets
        if w.get("role", "").lower() == "toggle button"
        and w.get("name") in EXPECTED_TAB_LABELS
    ]
    tab_labels = tab_labels_from_page_tab if tab_labels_from_page_tab else tab_labels_from_toggle
    widget_labels = [
        {"depth": w["depth"], "name": w["name"], "role": w["role"]}
        for w in all_widgets
        if w.get("name")
    ]

    return {
        "status": "observed",
        "blocker": "none",
        "blockerDetail": "",
        "javaPid": java_pid,
        "observedAppName": app_name,
        "observedChildCount": app_child_count,
        "selectProjectFrameName": select_project_frame.name,
        "selectProjectFrameRole": select_project_frame.getRoleName(),
        "widgetCount": len(all_widgets),
        "widgetLabels": widget_labels,
        "tabLabels": tab_labels,
        "tabLabelSource": "page-tab" if tab_labels_from_page_tab else "toggle-button",
        "expectedTabLabels": EXPECTED_TAB_LABELS,
        "tabLabelMatch": sorted(tab_labels) == sorted(EXPECTED_TAB_LABELS),
        "atkWrapperJar": ATK_WRAPPER_JAR,
    }


def not_observed_payload(inventory_path: Path) -> dict[str, Any]:
    return {
        "status": "not-observed",
        "blocker": "select-project-window-not-in-inventory",
        "blockerDetail": (
            f"No Java window titled '{EXPECTED_SELECT_PROJECT_TITLE}' was found in "
            f"{inventory_path.name}; the AT-SPI probe requires a running Alice Java "
            "process with the Select Project window visible before it can enumerate "
            "Swing widgets."
        ),
        "javaPid": None,
        "observedAppName": "",
        "observedChildCount": 0,
        "atkWrapperJar": ATK_WRAPPER_JAR,
        "tabLabels": [],
        "widgetLabels": [],
    }


def blocked_payload(inventory_path: Path, exc: Exception) -> dict[str, Any]:
    return {
        "status": "blocked",
        "blocker": "inventory-unreadable",
        "blockerDetail": f"Could not read {inventory_path}: {exc}",
        "javaPid": None,
        "observedAppName": "",
        "observedChildCount": 0,
        "atkWrapperJar": ATK_WRAPPER_JAR,
        "tabLabels": [],
        "widgetLabels": [],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inventory", help="Path to x-window-inventory.json")
    parser.add_argument("output", help="Path to write swing-widget-observation.json")
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
            payload = probe_at_spi(java_pid)

    output_path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
