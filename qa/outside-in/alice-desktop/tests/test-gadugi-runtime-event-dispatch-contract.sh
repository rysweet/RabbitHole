#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-gadugi-runtime-event-dispatch-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

SCENARIO="$BASE_DIR/gadugi/runtime-event-dispatch-evidence.yaml"

assert_file_exists "$SCENARIO" "Gadugi runtime event dispatch evidence scenario exists"

python3 - "$SCENARIO" >"$tmp_root/gadugi-contract.out" 2>"$tmp_root/gadugi-contract.err" <<'PY'
from pathlib import Path
import sys
import yaml

scenario_path = Path(sys.argv[1])
scenario = yaml.safe_load(scenario_path.read_text(encoding="utf-8"))

errors = []

if scenario_path.parent.name != "gadugi":
    errors.append("Gadugi scenario must live outside the custom scenarios catalog")
if "scenarios" in scenario_path.parts:
    errors.append("Gadugi scenario path must not be under qa/outside-in/alice-desktop/scenarios")

expected_name = "runtime-event-dispatch-evidence"
if scenario.get("name") != expected_name:
    errors.append(f"scenario name must be {expected_name!r} for gadugi-test run -s compatibility")

description = str(scenario.get("description", ""))
description_lower = description.lower()
for required in (
    "pr #403",
    "runtime event dispatch",
    "headless listener dispatch evidence contract",
):
    if required not in description_lower:
        errors.append(f"description must conservatively mention {required!r}")
for required_non_claim in (
    "does not validate visible rendering",
    "desktop runtime execution",
    "full world playback",
    "visible correctness",
    "grading",
    "save completion",
    "full ui automation",
):
    if required_non_claim not in description_lower:
        errors.append(f"description must explicitly avoid overclaiming: {required_non_claim!r}")

metadata_tags = set(scenario.get("metadata", {}).get("tags", []))
for tag in ("cli", "gadugi", "pr-403", "runtime-event-dispatch", "event-dispatch-evidence"):
    if tag not in metadata_tags:
        errors.append(f"metadata.tags must include {tag!r}")

agents = scenario.get("agents")
if not isinstance(agents, list) or len(agents) != 1:
    errors.append("scenario must define exactly one CLI agent")
else:
    agent = agents[0]
    if agent.get("type") != "cli":
        errors.append("scenario agent type must be cli")
    if agent.get("config", {}).get("workingDirectory") != ".":
        errors.append("scenario CLI agent must run from the repository root")

steps = scenario.get("steps")
if not isinstance(steps, list):
    errors.append("scenario must define CLI steps")
else:
    commands = [
        (step.get("params", {}).get("command"), tuple(step.get("params", {}).get("args", [])))
        for step in steps
    ]
    expected_commands = [
        ("qa/outside-in/alice-desktop/runners/validate-scenarios.sh", ()),
        (
            "qa/outside-in/alice-desktop/runners/run-scenario.sh",
            (
                "run",
                "alice-desktop-runtime-event-dispatch-smoke",
                "--prepare-only",
                "--evidence-dir",
                "qa/outside-in/alice-desktop/evidence/gadugi-runtime-event-dispatch",
            ),
        ),
        ("qa/outside-in/alice-desktop/tests/run-tests.sh", ()),
    ]
    if commands != expected_commands:
        errors.append(f"scenario commands must delegate to existing desktop outside-in QA entry points: {commands!r}")
    for index, step in enumerate(steps, 1):
        if step.get("action") != "run":
            errors.append(f"step {index} must use Gadugi CLI action run")
        if not isinstance(step.get("timeout"), int) or step["timeout"] <= 0:
            errors.append(f"step {index} must set a positive timeout")

assertions = scenario.get("assertions")
if assertions != []:
    errors.append("scenario should not add Gadugi assertions that overclaim runtime dispatch correctness")

if errors:
    raise AssertionError("\n".join(errors))
PY
status=$?
assert_success "$status" "Gadugi scenario delegates conservatively to existing QA commands"

finish
