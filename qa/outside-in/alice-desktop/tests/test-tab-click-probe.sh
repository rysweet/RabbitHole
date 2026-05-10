#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROBE="$BASE_DIR/runners/tab-click-probe.py"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

python3 - "$PROBE" >"$tmp_root/tab-click-probe-contract.out" 2>"$tmp_root/tab-click-probe-contract.err" <<'PY'
import importlib.util
import json
import os
import sys
import types
from pathlib import Path

TARGET_DISPLAY_NAME = "Africa Full"
TARGET_REPOSITORY_PATH = "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"


class FakeStateSet:
    def __init__(self, states=None):
        self._states = set(states or [])

    def getStates(self):
        return sorted(self._states)

    def contains(self, state):
        return state in self._states


class FakeAction:
    def __init__(self, node):
        self._node = node
        self._names = list(node.actions)
        self.nActions = len(self._names)

    def getName(self, index):
        return self._names[index]

    def doAction(self, index):
        return bool(self._node.actions[self._names[index]]())


class FakeSelection:
    def __init__(self, node):
        self._node = node

    def selectChild(self, index):
        self._node.selected_child_index = index
        return True

    def isChildSelected(self, index):
        return self._node.selected_child_index == index


class FakeNode:
    def __init__(
        self,
        name,
        role,
        *,
        children=None,
        description="",
        actions=None,
        states=None,
        selection_supported=False,
        process_id=None,
        child_count_error=False,
    ):
        self.name = name
        self._role = role
        self.description = description
        self.children = list(children or [])
        self.actions = actions or {}
        self.states = FakeStateSet(states)
        self.selection_supported = selection_supported
        self.selected_child_index = None
        self.process_id = process_id
        self.child_count_error = child_count_error
        self.parent = None
        for child in self.children:
            child.parent = self

    @property
    def childCount(self):
        if self.child_count_error:
            raise RuntimeError(f"{self.name or self._role} childCount unavailable")
        return len(self.children)

    def getChildAtIndex(self, index):
        return self.children[index]

    def getRoleName(self):
        return self._role

    def queryAction(self):
        if not self.actions:
            raise RuntimeError(f"{self.name or self._role} exposes no actions")
        return FakeAction(self)

    def querySelection(self):
        if not self.selection_supported:
            raise RuntimeError(f"{self.name or self._role} exposes no selection interface")
        return FakeSelection(self)

    def getState(self):
        return self.states

    def get_parent(self):
        return self.parent

    def getIndexInParent(self):
        if self.parent is None:
            return -1
        return self.parent.children.index(self)

    def get_process_id(self):
        return self.process_id


class FakeRegistry:
    desktop = None

    @staticmethod
    def getDesktop(index):
        if index != 0:
            raise RuntimeError("only desktop 0 exists in this test")
        return FakeRegistry.desktop


def load_probe(path):
    module_name = "tab_click_probe_under_test"
    spec = importlib.util.spec_from_file_location(module_name, path)
    module = importlib.util.module_from_spec(spec)
    fake_pyatspi = types.SimpleNamespace(Registry=FakeRegistry)
    sys.modules["pyatspi"] = fake_pyatspi
    try:
        spec.loader.exec_module(module)
    finally:
        sys.modules["pyatspi"] = fake_pyatspi
    return module


def make_tab(name, selected=False, click_success=None):
    states = {"enabled", "visible", "showing"}
    if selected:
        states.add("selected")
    actions = {}
    if click_success is not None:
        actions["click"] = lambda: click_success
    return FakeNode(name, "toggle button", actions=actions, states=states)


def make_fixture(
    *,
    active_starter_names,
    target_action=True,
    target_parent_selection=True,
    hidden_target_outside_starters=False,
    starters_tab_click_success=True,
):
    counters = {"wonderland": 0, "africa": 0, "ok": 0}
    app = FakeNode("", "application", process_id=2468)

    def item_node(name, counter_key, enabled=True):
        actions = {}
        if enabled:
            actions["click"] = lambda key=counter_key: counters.__setitem__(key, counters[key] + 1) or True
        return FakeNode(
            name,
            "panel",
            children=[FakeNode(name, "label", states={"visible", "showing"})],
            actions=actions,
            states={"enabled", "visible", "showing"},
        )

    active_children = []
    for name in active_starter_names:
        if name == TARGET_DISPLAY_NAME:
            active_children.append(item_node(name, "africa", enabled=target_action))
        else:
            active_children.append(item_node(name, "wonderland", enabled=True))
    starters_list = FakeNode(
        "Starters",
        "list",
        children=active_children,
        states={"enabled", "visible", "showing"},
        selection_supported=target_parent_selection,
    )
    starters_panel = FakeNode("Starters", "panel", children=[starters_list], states={"visible", "showing"})

    inactive_children = []
    if hidden_target_outside_starters:
        inactive_children.append(item_node(TARGET_DISPLAY_NAME, "africa", enabled=True))
    inactive_list = FakeNode("Blank Slates", "list", children=inactive_children, states={"enabled"})
    inactive_panel = FakeNode("Blank Slates", "panel", children=[inactive_list], states={"enabled"})

    def ok_action():
        counters["ok"] += 1
        app.children = []
        return True

    frame = FakeNode(
        "Select Project",
        "frame",
        children=[
            make_tab("Blank Slates"),
            make_tab("Starters", selected=True, click_success=starters_tab_click_success),
            make_tab("My Projects"),
            make_tab("Recent"),
            make_tab("File System"),
            starters_panel,
            inactive_panel,
            FakeNode("OK", "push button", actions={"click": ok_action}, states={"enabled", "visible", "showing"}),
        ],
        states={"enabled", "visible", "showing"},
    )
    app.children = [frame]
    frame.parent = app
    desktop = FakeNode("desktop", "desktop frame", children=[app])
    FakeRegistry.desktop = desktop
    return counters


def assert_blocker_shape(payload):
    blocker = payload.get("nextBlocker")
    if not isinstance(blocker, dict):
        raise AssertionError("blocked target-starter evidence must include nextBlocker")
    if "nextBlocker" in payload.get("projectOpenAttempt", {}):
        raise AssertionError("blocked target-starter evidence must publish only one top-level nextBlocker")
    for field in (
        "observedAtspiState",
        "actionAttempted",
        "expectedNextAction",
        "reasonProgressStopped",
    ):
        value = blocker.get(field)
        if not isinstance(value, str) or not value.strip():
            raise AssertionError(f"nextBlocker.{field} must be a non-empty string")


def assert_widget_tree_minimized(payload):
    for field in ("allWidgetTree", "allWidgetTreeAfterClick"):
        tree = payload.get(field)
        if not isinstance(tree, list):
            raise AssertionError(f"{field} must be a list")
        rendered = json.dumps(tree, sort_keys=True)
        for forbidden in ("Wonderland", TARGET_DISPLAY_NAME):
            if forbidden in rendered:
                raise AssertionError(f"{field} must redact arbitrary widget text, found {forbidden!r}")
        for entry in tree:
            if entry.get("description"):
                raise AssertionError(f"{field} must redact widget descriptions: {entry!r}")


probe = load_probe(Path(sys.argv[1]))
os.environ["TARGET_STARTER_DISPLAY_NAME"] = TARGET_DISPLAY_NAME
os.environ["TARGET_STARTER_REPO_PATH"] = TARGET_REPOSITORY_PATH

success_counters = make_fixture(
    active_starter_names=["Wonderland", TARGET_DISPLAY_NAME],
    target_action=True,
    target_parent_selection=True,
)
success = probe.probe_tab_click(2468)
if success.get("evidenceStatus") != "opened":
    raise AssertionError(f"expected evidenceStatus=opened for target-specific open, got {success.get('evidenceStatus')!r}")
if success.get("targetStarter") != {
    "displayName": TARGET_DISPLAY_NAME,
    "repositoryPath": TARGET_REPOSITORY_PATH,
}:
    raise AssertionError(f"targetStarter metadata mismatch: {success.get('targetStarter')!r}")
observed = success.get("targetStarterObserved")
if not isinstance(observed, dict) or observed.get("name") != TARGET_DISPLAY_NAME:
    raise AssertionError(f"targetStarterObserved must identify Africa Full, got {observed!r}")
if success.get("targetStarterSelected") is not True:
    raise AssertionError("targetStarterSelected must be true after selecting Africa Full")
if success.get("targetStarterOpenAttempted") is not True:
    raise AssertionError("targetStarterOpenAttempted must be true when OK/Open is clicked for Africa Full")
if success.get("targetSelectionObserved") is not True:
    raise AssertionError("targetSelectionObserved must be true after target-specific Africa Full selection")
if success.get("openAttempted") is not True:
    raise AssertionError("openAttempted must be true only after target-specific Africa Full selection")
if success.get("openedStarter") != {
    "displayName": TARGET_DISPLAY_NAME,
    "repositoryPath": TARGET_REPOSITORY_PATH,
}:
    raise AssertionError(f"openedStarter must record Africa Full, got {success.get('openedStarter')!r}")
if success.get("projectOpenObserved") is not True:
    raise AssertionError("opened target evidence must still require projectOpenObserved=true")
if success.get("projectOpenAttempt", {}).get("startersTabClick", {}).get("success") is not True:
    raise AssertionError("target starter opening must confirm Starters tab activation before target search")
window_context = success.get("selectProjectWindowContext")
if not isinstance(window_context, dict):
    raise AssertionError("opened evidence must include selectProjectWindowContext")
if window_context.get("title") != "Select Project":
    raise AssertionError(f"selectProjectWindowContext must name Select Project, got {window_context!r}")
if window_context.get("javaPid") != 2468:
    raise AssertionError(f"selectProjectWindowContext must include the Alice Java/window PID, got {window_context!r}")
starters_safety = success.get("startersTabSafety")
if not isinstance(starters_safety, dict):
    raise AssertionError("opened evidence must include startersTabSafety")
if starters_safety.get("tabName") != "Starters":
    raise AssertionError(f"startersTabSafety must identify the Starters tab, got {starters_safety!r}")
if starters_safety.get("activatedBeforeTargetSearch") is not True:
    raise AssertionError(f"startersTabSafety must prove Starters activation before target search, got {starters_safety!r}")
if starters_safety.get("targetSearchScope") != "active-starters-tab":
    raise AssertionError(f"target search must be scoped to the active Starters tab, got {starters_safety!r}")
if success.get("nextBlocker") is not None:
    raise AssertionError(f"opened evidence must not publish a next blocker, got {success.get('nextBlocker')!r}")
if "nextBlocker" in success.get("projectOpenAttempt", {}):
    raise AssertionError("opened evidence must not duplicate nextBlocker inside projectOpenAttempt")
assert_widget_tree_minimized(success)
if success_counters["wonderland"] != 0:
    raise AssertionError("probe must not click/open the first starter when it is not Africa Full")
if success_counters["africa"] != 1:
    raise AssertionError("probe must click/select the Africa Full starter exactly once")
if success_counters["ok"] != 1:
    raise AssertionError("probe must click OK/Open exactly once after Africa Full selection evidence")

absent_counters = make_fixture(
    active_starter_names=["Wonderland"],
    target_action=True,
    target_parent_selection=True,
    hidden_target_outside_starters=True,
)
absent = probe.probe_tab_click(2468)
if absent.get("evidenceStatus") != "blocked":
    raise AssertionError(f"expected evidenceStatus=blocked when Africa Full is absent from active Starters, got {absent.get('evidenceStatus')!r}")
if absent.get("blocker") != "target-starter-not-found":
    raise AssertionError(f"expected target-starter-not-found blocker, got {absent.get('blocker')!r}")
if absent.get("targetStarterSelected") is not False:
    raise AssertionError("targetStarterSelected must be false when the active Starters context lacks Africa Full")
if absent.get("targetStarterOpenAttempted") is not False:
    raise AssertionError("OK/Open must not be attempted when Africa Full was not observed in active Starters")
if absent.get("targetSelectionObserved") is not False:
    raise AssertionError("targetSelectionObserved must be false when active Starters lacks Africa Full")
if absent.get("openAttempted") is not False:
    raise AssertionError("openAttempted must be false when Africa Full was not observed in active Starters")
if absent_counters["ok"] != 0:
    raise AssertionError("probe must not click OK/Open when only a non-active/hidden Africa Full node was observed")
assert_blocker_shape(absent)
tab_blocked_counters = make_fixture(
    active_starter_names=[TARGET_DISPLAY_NAME],
    target_action=True,
    target_parent_selection=True,
    starters_tab_click_success=False,
)
tab_blocked = probe.probe_tab_click(2468)
if tab_blocked.get("evidenceStatus") != "blocked":
    raise AssertionError(f"expected evidenceStatus=blocked for Starters tab activation failure, got {tab_blocked.get('evidenceStatus')!r}")
if tab_blocked.get("blocker") != "target-starter-tab-activation-failed":
    raise AssertionError(f"expected target-starter-tab-activation-failed blocker, got {tab_blocked.get('blocker')!r}")
if tab_blocked.get("targetStarterObserved") is not None:
    raise AssertionError("targetStarterObserved must remain empty when Starters tab activation fails")
if tab_blocked.get("targetStarterSelected") is not False:
    raise AssertionError("targetStarterSelected must be false when Starters tab activation fails")
if tab_blocked.get("targetStarterOpenAttempted") is not False:
    raise AssertionError("OK/Open must not be attempted when Starters tab activation fails")
if tab_blocked.get("targetSelectionObserved") is not False:
    raise AssertionError("targetSelectionObserved must be false when Starters tab activation fails")
if tab_blocked.get("openAttempted") is not False:
    raise AssertionError("openAttempted must be false when Starters tab activation fails")
if tab_blocked_counters["africa"] != 0:
    raise AssertionError("probe must not select Africa Full when Starters tab activation fails")
if tab_blocked_counters["ok"] != 0:
    raise AssertionError("probe must not click OK/Open when Starters tab activation fails")
assert_blocker_shape(tab_blocked)
blocked_counters = make_fixture(
    active_starter_names=[TARGET_DISPLAY_NAME],
    target_action=False,
    target_parent_selection=False,
)
blocked = probe.probe_tab_click(2468)
if blocked.get("evidenceStatus") != "blocked":
    raise AssertionError(f"expected evidenceStatus=blocked for target selection capability gap, got {blocked.get('evidenceStatus')!r}")
if blocked.get("blocker") != "target-starter-selection-unavailable":
    raise AssertionError(f"expected target-starter-selection-unavailable blocker, got {blocked.get('blocker')!r}")
if blocked.get("targetStarterSelected") is not False:
    raise AssertionError("targetStarterSelected must be false when no target action or parent selection interface works")
if blocked.get("targetStarterOpenAttempted") is not False:
    raise AssertionError("OK/Open must not be attempted without target-specific selection evidence")
if blocked.get("targetSelectionObserved") is not False:
    raise AssertionError("targetSelectionObserved must be false when target-specific selection fails")
if blocked.get("openAttempted") is not False:
    raise AssertionError("openAttempted must be false when target-specific selection fails")
if blocked_counters["ok"] != 0:
    raise AssertionError("probe must not click OK/Open after a target-specific selection capability gap")
assert_blocker_shape(blocked)

unreadable_app = FakeNode("Alice", "application", process_id=2468, child_count_error=True)
FakeRegistry.desktop = FakeNode("desktop", "desktop frame", children=[unreadable_app])
child_count_blocked = probe.probe_tab_click(2468)
if child_count_blocked.get("status") != "blocked":
    raise AssertionError(f"expected blocked status for unreadable AT-SPI app childCount, got {child_count_blocked!r}")
if child_count_blocked.get("blocker") != "select-project-not-accessible":
    raise AssertionError(
        "unreadable AT-SPI app childCount must produce a precise blocker instead of crashing, "
        f"got {child_count_blocked.get('blocker')!r}"
    )
PY
status=$?
assert_success "$status" "tab-click probe emits target-specific Africa Full opened/blocked evidence"

inventory="$tmp_root/missing-inventory.json"
output="$tmp_root/blocked-output.json"
TARGET_STARTER_DISPLAY_NAME="Africa Full" \
TARGET_STARTER_REPO_PATH="core/resources/src/application/resources/starter-projects/AfricaFull.a3p" \
python3 "$PROBE" "$inventory" "$output"
status=$?
assert_success "$status" "tab-click probe writes blocked output when inventory is unreadable"
assert_contains "$output" '"targetStarter": \{' "blocked probe output preserves targetStarter metadata"
assert_contains "$output" '"displayName": "Africa Full"' "blocked probe output records target display name"
assert_contains "$output" '"repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull\.a3p"' "blocked probe output records target repository path"
assert_contains "$output" '"evidenceStatus": "blocked"' "blocked probe output uses blocked evidenceStatus"
assert_contains "$output" '"targetSelectionObserved": false' "blocked probe output records targetSelectionObserved=false"
assert_contains "$output" '"openAttempted": false' "blocked probe output records openAttempted=false"
assert_contains "$output" '"nextBlocker": \{' "blocked probe output includes structured next blocker"
assert_contains "$output" '"blockerDetail": "Could not read missing-inventory\.json:' "blocked probe output reports only the inventory basename"
assert_contains "$output" '"observedAtspiState": "missing-inventory\.json could not be read' "blocked probe nextBlocker reports only the inventory basename"
assert_not_contains "$output" "$tmp_root" "blocked probe output does not disclose the scratch path"

invalid_target_output="$tmp_root/invalid-target-output.json"
TARGET_STARTER_DISPLAY_NAME="Wonderland" \
TARGET_STARTER_REPO_PATH="core/resources/src/application/resources/starter-projects/Wonderland.a3p" \
python3 "$PROBE" "$inventory" "$invalid_target_output"
status=$?
assert_success "$status" "tab-click probe exits 0 for invalid target metadata"
assert_contains "$invalid_target_output" '"status": "failed"' "invalid target metadata records failed status"
assert_contains "$invalid_target_output" '"blocker": "target-starter-metadata-invalid"' "invalid target metadata names exact blocker"
assert_contains "$invalid_target_output" 'Africa Full' "invalid target metadata error names the required target"

finish
