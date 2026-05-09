#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
RUNNER="$BASE_DIR/runners/run-scenario.sh"
TAB_CLICK_PROBE="$BASE_DIR/runners/tab-click-probe.py"
POST_OPEN_RUNTIME_DISPLAY_PROBE="$BASE_DIR/runners/post-open-runtime-display-probe.py"
LAUNCH_SCENARIO="$BASE_DIR/scenarios/launch.yaml"
RUN_DEBUG_SCENARIO="$BASE_DIR/scenarios/run-debug.yaml"
RUNTIME_SCENARIO="$BASE_DIR/scenarios/post-open-runtime-display-accessibility-evidence.yaml"
SELECT_SCENARIO="$BASE_DIR/scenarios/select-project-tab-click-exec.yaml"
RUNTIME_REFERENCE_DOC="$REPO_ROOT/docs/reference/post-open-runtime-display-accessibility-evidence.md"
SELECT_REFERENCE_DOC="$REPO_ROOT/docs/reference/select-project-africa-full-atspi-evidence.md"
SILVER_THREAD_REFERENCE_DOC="$REPO_ROOT/docs/reference/silver-thread-status-report.md"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

for path in \
  "$VALIDATOR" \
  "$RUNNER" \
  "$TAB_CLICK_PROBE" \
  "$POST_OPEN_RUNTIME_DISPLAY_PROBE" \
  "$LAUNCH_SCENARIO" \
  "$RUN_DEBUG_SCENARIO" \
  "$RUNTIME_SCENARIO" \
  "$SELECT_SCENARIO" \
  "$RUNTIME_REFERENCE_DOC" \
  "$SELECT_REFERENCE_DOC" \
  "$SILVER_THREAD_REFERENCE_DOC"
do
  assert_file_exists "$path" "accessibility target discovery contract input exists: ${path#$REPO_ROOT/}"
done

all_scenarios_dumped=1
for scenario_id in \
  alice-desktop-launch \
  alice-desktop-run-debug \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  alice-desktop-select-project-tab-click-exec
do
  "$VALIDATOR" --dump-json "$scenario_id" >"$tmp_root/$scenario_id.json" 2>"$tmp_root/$scenario_id.err"
  status=$?
  assert_success "$status" "validator dumps $scenario_id for accessibility target discovery contract"
  if [ "$status" -ne 0 ]; then
    all_scenarios_dumped=0
  fi
done

if [ "$all_scenarios_dumped" -eq 1 ]; then
  python3 - \
    "$tmp_root/alice-desktop-launch.json" \
    "$tmp_root/alice-desktop-run-debug.json" \
    "$tmp_root/alice-desktop-post-open-runtime-display-accessibility-evidence.json" \
    "$tmp_root/alice-desktop-select-project-tab-click-exec.json" \
    "$RUNNER" \
    "$TAB_CLICK_PROBE" \
    "$POST_OPEN_RUNTIME_DISPLAY_PROBE" \
    "$LAUNCH_SCENARIO" \
    "$RUN_DEBUG_SCENARIO" \
    "$RUNTIME_SCENARIO" \
    "$SELECT_SCENARIO" \
    "$RUNTIME_REFERENCE_DOC" \
    "$SELECT_REFERENCE_DOC" \
    "$SILVER_THREAD_REFERENCE_DOC" \
    >"$tmp_root/silver-thread-contract.out" \
    2>"$tmp_root/silver-thread-contract.err" <<'PY'
import json
import re
import sys
from pathlib import Path

(
    launch_json,
    run_debug_json,
    runtime_json,
    select_json,
    runner_path,
    tab_click_probe_path,
    runtime_probe_path,
    launch_scenario_path,
    run_debug_scenario_path,
    runtime_scenario_path,
    select_scenario_path,
    runtime_doc_path,
    select_doc_path,
    silver_thread_doc_path,
) = [Path(arg) for arg in sys.argv[1:15]]

scenarios = {
    "launch": json.loads(launch_json.read_text(encoding="utf-8")),
    "run_debug": json.loads(run_debug_json.read_text(encoding="utf-8")),
    "runtime": json.loads(runtime_json.read_text(encoding="utf-8")),
    "select": json.loads(select_json.read_text(encoding="utf-8")),
}
texts = {
    "runner": runner_path.read_text(encoding="utf-8"),
    "tab_click_probe": tab_click_probe_path.read_text(encoding="utf-8"),
    "runtime_probe": runtime_probe_path.read_text(encoding="utf-8"),
    "launch_scenario": launch_scenario_path.read_text(encoding="utf-8"),
    "run_debug_scenario": run_debug_scenario_path.read_text(encoding="utf-8"),
    "runtime_scenario": runtime_scenario_path.read_text(encoding="utf-8"),
    "select_scenario": select_scenario_path.read_text(encoding="utf-8"),
    "runtime_doc": runtime_doc_path.read_text(encoding="utf-8"),
    "select_doc": select_doc_path.read_text(encoding="utf-8"),
    "silver_thread_doc": silver_thread_doc_path.read_text(encoding="utf-8"),
}
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def scenario_text(scenario, *fields):
    parts = []
    for field in fields:
        value = scenario
        for name in field.split("."):
            value = value.get(name, {}) if isinstance(value, dict) else {}
        if isinstance(value, list):
            parts.extend(str(item) for item in value)
        elif isinstance(value, dict):
            parts.append(json.dumps(value, sort_keys=True))
        else:
            parts.append(str(value))
    return "\n".join(parts)


def require_tokens(name, text, tokens):
    for token in tokens:
        require(token in text, f"{name} must include {token!r}")


def require_patterns(name, text, patterns):
    for pattern in patterns:
        require(re.search(pattern, text, flags=re.MULTILINE), f"{name} must match /{pattern}/")


launch = scenarios["launch"]
launch_evidence = scenario_text(launch, "evidence.required")
launch_fallback = scenario_text(launch, "fallback")
launch_claim_text = scenario_text(launch, "title", "expectedOutcomes", "evidence.required", "fallback")
require(launch.get("id") == "alice-desktop-launch", "launch scenario id must stay stable")
require(launch.get("workflow") == "launch", "launch workflow must stay scoped to launch")
require(launch.get("automationMode") == "xvfb-real-alice", "launch lane must remain executable through xvfb-real-alice")
require_tokens(
    "launch evidence",
    launch_evidence,
    [
        "root-directory-prep.json",
        "Launch log",
        "x-window-inventory.json",
        "application-root-error.json",
        "license-dialog.json",
        "controlled-display-pixel-observation.json",
        "Exit, status, or timeout record",
    ],
)
require("splash-only or crashed startup" in launch_claim_text, "launch evidence must distinguish ready launch from splash/crash states")
require("manual-evidence-required" in launch_fallback, "launch fallback must remain structured as manual evidence required")
require("preserve the failure log" in launch_fallback, "launch fallback must preserve exact failure evidence")

run_debug = scenarios["run_debug"]
run_debug_text = scenario_text(run_debug, "expectedOutcomes", "evidence.required", "fallback")
require(run_debug.get("workflow") == "run-debug", "run/debug workflow must stay scoped to run/debug evidence")
require(run_debug.get("automationMode") == "manual-evidence-required", "run/debug scenario must not pretend live automation by default")
require_tokens(
    "run/debug evidence",
    run_debug_text,
    [
        "desktop-run-execution.json",
        "desktop-run-render-affordance.json",
        "desktop-run-pixel-observation.json",
        "desktop-run-status-summary.json",
        "manual workflow context, not pixel-correctness evidence",
        "structural Run-view attachment evidence only",
        "not for rendering-engine correctness evidence",
    ],
)

runtime = scenarios["runtime"]
runtime_text = scenario_text(runtime, "userActions", "expectedOutcomes", "evidence.required", "fallback", "supportingEvidence")
require(runtime.get("id") == "alice-desktop-post-open-runtime-display-accessibility-evidence", "runtime scenario id must stay stable")
require(runtime.get("workflow") == "post-open-runtime-display-accessibility-evidence", "runtime workflow must stay scoped to post-open runtime/display evidence")
require(runtime.get("automationMode") == "xvfb-real-alice", "runtime discovery lane must remain executable")
target_starter = runtime.get("targetStarter") or {}
require(target_starter.get("displayName") == "Africa Full", "runtime lane must reuse fixed Africa Full starter metadata")
require(
    target_starter.get("repositoryPath") == "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
    "runtime lane must reuse fixed Africa Full repository path",
)
require_tokens(
    "runtime discovery evidence",
    runtime_text,
    [
        "post-open-runtime-display-accessibility-evidence.json",
        "runtimeDisplayCandidateCount",
        "runtimeDisplayCandidates",
        "geometryStatus",
        "screenExtents",
        "worldCanvasPixelTarget.status=target-ready",
        "visible-rendering-pixel-target-blocker.json",
        "missingTarget=run-window-world-canvas-screen-extents",
        "exactNextUnblocker=reliable-run-window-world-canvas-pixel-sampling-target",
        "visibleRenderingCorrectnessEstablished=false",
        "status=blocked with a precise blocker",
        "runtime-display-accessible-candidate-not-found",
    ],
)
require("full world execution" in runtime_text.lower(), "runtime lane must explicitly bound world execution as a non-claim")
require("does not assert full visible rendering correctness" in runtime_text, "runtime lane must explicitly avoid rendering correctness claims")

select = scenarios["select"]
select_text = scenario_text(select, "userActions", "expectedOutcomes", "evidence.required", "fallback", "supportingEvidence")
select_target = select.get("targetStarter") or {}
require(select.get("id") == "alice-desktop-select-project-tab-click-exec", "select scenario id must stay stable")
require(select.get("workflow") == "select-project-tab-click-smoke", "select workflow must stay scoped to Select Project target evidence")
require(select.get("automationMode") == "xvfb-real-alice", "select lane must remain executable")
require(select_target.get("displayName") == "Africa Full", "select lane must declare Africa Full target starter")
require(
    select_target.get("repositoryPath") == "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
    "select lane must declare the Africa Full repository path",
)
require_tokens(
    "select target discovery evidence",
    select_text,
    [
        "tab-click-observation.json",
        "targetStarterObserved",
        "targetStarterSelected=true",
        "targetStarterOpenAttempted=true",
        "openedStarter",
        "projectOpenObserved=true",
        "nextBlocker.observedAtspiState",
        "nextBlocker.actionAttempted",
        "nextBlocker.expectedNextAction",
        "nextBlocker.reasonProgressStopped",
        "target role/name/states/actions/tree path",
        "parent selection-interface availability",
        "Do not claim Africa Full opening unless",
    ],
)
require("does not claim visible rendering" in select_text, "select lane must explicitly avoid downstream rendering claims")

runner_text = texts["runner"]
require_tokens(
    "runner silver-thread status wiring",
    runner_text,
    [
        'POST_OPEN_RUNTIME_DISPLAY_PROBE="$SCRIPT_DIR/post-open-runtime-display-probe.py"',
        'TAB_CLICK_PROBE="$SCRIPT_DIR/tab-click-probe.py"',
        "write_post_open_runtime_display_probe",
        "write_tab_click_probe",
        "runtimeDisplayAccessibilityEvidence=%s\\n",
        "runtimeDisplayAccessibilityStatus=%s\\n",
        "runtimeDisplayAccessibilityBlocker=%s\\n",
        "tabClickObservation=%s\\n",
        "selectProjectEvidenceStatus=%s\\n",
        "selectProjectTargetDisplayName=%s\\n",
        "selectProjectTargetRepositoryPath=%s\\n",
        "selectProjectOpenedStarterDisplayName=%s\\n",
        "selectProjectNextBlocker=%s\\n",
    ],
)
require_patterns(
    "runner launch structured fallback",
    runner_text,
    [
        r'"claim": "post-open-runtime-display-accessibility-evidence"',
        r'controlledDisplayPixelStatus=blocked',
        r'visibleRenderingCorrectnessEstablished=false',
        r'alice-desktop-select-project-tab-click-exec\|alice-desktop-post-project-open-window-state',
    ],
)

runtime_probe_text = texts["runtime_probe"]
require_tokens(
    "runtime probe structured evidence",
    runtime_probe_text,
    [
        'CLAIM = "post-open-runtime-display-accessibility-evidence"',
        '"blocker": blocker',
        '"blockerDetail": blocker_detail',
        '"postOpenRuntimeDisplayAccessibilityObserved": status == "observed"',
        '"runtimeDisplayCandidateCount": len(runtime_candidates)',
        '"runtimeDisplayCandidates": runtime_candidates',
        '"geometryStatus"',
        '"screenExtents"',
        '"runtime-display-accessible-candidate-not-found"',
    ],
)
require_patterns(
    "runtime probe candidate guardrails",
    runtime_probe_text,
    [
        r"def is_runtime_display_candidate",
        r"if is_select_project_surface\(summary\):\n\s+return False",
        r"if not candidates:\n\s+return base_payload\(",
    ],
)

tab_click_text = texts["tab_click_probe"]
require_tokens(
    "select probe structured evidence",
    tab_click_text,
    [
        '"targetStarter": target_starter',
        '"targetStarterObserved": None',
        '"targetStarterSelected": False',
        '"targetStarterOpenAttempted": False',
        '"openedStarter": None',
        '"evidenceStatus": evidence_status',
        '"nextBlocker": blocker',
        '"observedAtspiState"',
        '"actionAttempted"',
        '"expectedNextAction"',
        '"reasonProgressStopped"',
        "target_observation_record",
        "parentSelectionAvailable",
    ],
)
require_patterns(
    "select probe target-safety ordering",
    tab_click_text,
    [
        r"targetSearchScope.*active-starters-tab",
        r"target-starter-not-found",
        r"target-starter-selection-unavailable",
        r"target-starter-tab-activation-failed",
    ],
)

bounded_claim_terms = {
    "full UI automation": [r"\bfull\s+UI\s+automation\b", r"\bbroad\s+UI\s+automation\b"],
    "visible correctness": [r"\bvisible\s+correctness\b", r"\bvisual\s+correctness\b", r"\bvisible\s+UI\s+correctness\b"],
    "rendering correctness": [
        r"\brendering\s+correctness\b",
        r"\bvisible\s+rendering\s+correctness\b",
        r"\brendering-engine\s+correctness\b",
        r"\brendered-world\s+correctness\b",
    ],
    "world execution correctness": [
        r"\bworld\s+execution\s+correctness\b",
        r"\bfull\s+world\s+execution\b",
        r"\bworld\s+execution\b",
    ],
    "general accessibility compliance": [r"\bgeneral\s+accessibility\s+compliance\b", r"\baccessibility\s+compliance\b"],
}
limiting_markers = (
    "does not",
    "do not",
    "must not",
    "not prove",
    "not assert",
    "not a",
    "without",
    "unsupported",
    "non-claim",
    "non-goal",
    "out of scope",
    "limited to",
    "bounded",
    "blocked",
    "gap",
    "instead of",
    "avoid",
)

claim_scope_inputs = {
    "launch_scenario": texts["launch_scenario"],
    "run_debug_scenario": texts["run_debug_scenario"],
    "runtime_scenario": texts["runtime_scenario"],
    "select_scenario": texts["select_scenario"],
    "runtime_doc": texts["runtime_doc"],
    "select_doc": texts["select_doc"],
    "silver_thread_doc": texts["silver_thread_doc"],
}

for name, text in claim_scope_inputs.items():
    normalized = re.sub(r"\n(?=\S)", " ", text)
    paragraphs = re.split(r"\n\s*\n", normalized)
    for paragraph in paragraphs:
        lower = paragraph.lower()
        matched_labels = [
            label
            for label, patterns in bounded_claim_terms.items()
            if any(re.search(pattern, paragraph, flags=re.IGNORECASE) for pattern in patterns)
        ]
        if not matched_labels:
            continue
        if not any(marker in lower for marker in limiting_markers):
            errors.append(
                f"{name} has an unbounded accessibility target discovery claim for "
                f"{', '.join(matched_labels)}: {paragraph[:180]}"
            )

combined_scope_text = "\n".join(claim_scope_inputs.values()).lower()
for required_non_claim in (
    "full ui automation",
    "visible rendering correctness",
    "full world execution",
):
    require(required_non_claim in combined_scope_text, f"scope docs/scenarios must explicitly mention {required_non_claim!r} as a bounded non-claim")
require(
    "target discovery" in combined_scope_text or "target-specific" in combined_scope_text,
    "scope docs/scenarios must keep the silver-thread wording tied to target discovery evidence",
)

if errors:
    raise AssertionError("\n".join(errors))

print("accessibility target discovery silver-thread contract satisfied")
PY
  status=$?
else
  printf 'scenario JSON was unavailable; skipping detailed accessibility target discovery silver-thread contract\n' >"$tmp_root/silver-thread-contract.err"
  status=1
fi
assert_success "$status" "accessibility target discovery silver-thread contract validates launch, run/runtime, and select lanes"

finish
