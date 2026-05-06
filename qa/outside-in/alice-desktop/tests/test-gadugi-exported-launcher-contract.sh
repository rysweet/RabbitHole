#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-gadugi-exported-launcher-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

SCENARIO="$BASE_DIR/gadugi/exported-launcher-evidence.yaml"

assert_file_exists "$SCENARIO" "Gadugi exported launcher evidence scenario exists"

python3 - "$SCENARIO" >"$tmp_root/gadugi-exported-launcher-contract.out" 2>"$tmp_root/gadugi-exported-launcher-contract.err" <<'PY'
from pathlib import Path
import re
import sys

scenario_path = Path(sys.argv[1])
scenario_text = scenario_path.read_text(encoding="utf-8")

errors = []

def top_level_scalar(name):
    match = re.search(rf"^{re.escape(name)}:\s*(.+)$", scenario_text, re.MULTILINE)
    if not match:
        return None
    value = match.group(1).strip()
    if len(value) >= 2 and value[0] == value[-1] == '"':
        value = value[1:-1]
    return value

def top_level_block(name):
    match = re.search(rf"^{re.escape(name)}:\n((?:[ \t].*(?:\n|$))*)", scenario_text, re.MULTILINE)
    return match.group(1) if match else ""

if scenario_path.parent.name != "gadugi":
    errors.append("Gadugi scenario must live outside the custom scenarios catalog")
if "scenarios" in scenario_path.parts:
    errors.append("Gadugi scenario path must not be under qa/outside-in/alice-desktop/scenarios")

expected_name = "exported-launcher-evidence"
if top_level_scalar("name") != expected_name:
    errors.append(f"scenario name must be {expected_name!r} for gadugi-test run -s compatibility")

description = top_level_scalar("description") or ""
description_lower = description.lower()
for stale_reference in (
    "agentic-test.config.yaml",
):
    if stale_reference in scenario_text:
        errors.append(f"scenario must not reference nonexistent config {stale_reference!r}")
for required in (
    "pr #155",
    "exported launcher evidence workflow",
    "javafx handoff/no-go evidence contract",
):
    if required not in description_lower:
        errors.append(f"description must conservatively mention {required!r}")
for required_non_claim in (
    "does not validate visible rendering",
    "rendered pixels",
    "visible desktop windows",
    "save behavior",
    "project grading",
    "creative assessment",
    "full lesson completion",
    "full alice desktop workflow",
):
    if required_non_claim not in description_lower:
        errors.append(f"description must explicitly avoid overclaiming: {required_non_claim!r}")

metadata = top_level_block("metadata")
for tag in ("cli", "gadugi", "pr-155", "exported-launcher", "launcher-evidence-contract"):
    if f"    - {tag}" not in metadata:
        errors.append(f"metadata.tags must include {tag!r}")

agents = top_level_block("agents")
if len(re.findall(r"^  -\s+", agents, re.MULTILINE)) != 1:
    errors.append("scenario must define exactly one CLI agent")
else:
    if not re.search(r"^    type:\s*cli\s*$", agents, re.MULTILINE):
        errors.append("scenario agent type must be cli")
    if not re.search(r"^      workingDirectory:\s*\.\s*$", agents, re.MULTILINE):
        errors.append("scenario CLI agent must run from the repository root")

steps = top_level_block("steps")
step_blocks = re.split(r"(?m)^  - name:\s*", steps)[1:]
if not step_blocks:
    errors.append("scenario must define CLI steps")
else:
    commands = []
    for block in step_blocks:
        command_match = re.search(r"^      command:\s*(.+?)\s*$", block, re.MULTILINE)
        args = tuple(
            line.split("- ", 1)[1].strip()
            for line in block.splitlines()
            if line.startswith("        - ")
        )
        commands.append((command_match.group(1) if command_match else None, args))
    expected_commands = [
        ("qa/outside-in/alice-desktop/runners/validate-scenarios.sh", ()),
        (
            "qa/outside-in/alice-desktop/runners/run-scenario.sh",
            (
                "run",
                "alice-desktop-exported-project-smoke",
                "--prepare-only",
                "--evidence-dir",
                "qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher",
            ),
        ),
        ("qa/outside-in/alice-desktop/tests/run-tests.sh", ()),
    ]
    if commands != expected_commands:
        errors.append(f"scenario commands must delegate to existing desktop outside-in QA entry points: {commands!r}")
    for index, block in enumerate(step_blocks, 1):
        if not re.search(r"^    action:\s*run\s*$", block, re.MULTILINE):
            errors.append(f"step {index} must use Gadugi CLI action run")
        timeout_match = re.search(r"^    timeout:\s*([0-9]+)\s*$", block, re.MULTILINE)
        if not timeout_match or int(timeout_match.group(1)) <= 0:
            errors.append(f"step {index} must set a positive timeout")

if top_level_scalar("assertions") != "[]":
    errors.append("scenario should not add Gadugi assertions that overclaim launcher rendering or full workflow")

if errors:
    raise AssertionError("\n".join(errors))
PY
status=$?
assert_success "$status" "Gadugi scenario delegates conservatively to existing QA commands"

finish
