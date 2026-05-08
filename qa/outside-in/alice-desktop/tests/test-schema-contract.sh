#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-schema-contract.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

SCHEMA="$BASE_DIR/schema/scenario.schema.json"

python3 - "$SCHEMA" >"$tmp_root/schema-contract.out" 2>"$tmp_root/schema-contract.err" <<'PY'
import json
import sys

schema_path = sys.argv[1]
schema = json.load(open(schema_path, encoding="utf-8"))

required_top = set(schema.get("required", []))
expected_top = {
    "id",
    "title",
    "workflow",
    "automationMode",
    "preconditions",
    "userActions",
    "expectedOutcomes",
    "evidence",
    "fallback",
}
missing = sorted(expected_top - required_top)
if missing:
    raise AssertionError(f"schema is missing required top-level fields: {missing}")

automation = schema["properties"]["automation"]
required_automation = set(automation.get("required", []))
expected_automation = {"cwd", "argv", "timeoutSeconds", "readyWaitSeconds"}
missing_automation = sorted(expected_automation - required_automation)
if missing_automation:
    raise AssertionError(
        "automation object must require all command fields when present: "
        f"{missing_automation}"
    )

if "command" in automation.get("properties", {}):
    raise AssertionError("legacy command field must not be part of the schema")

argv_schema = automation["properties"]["argv"]
allowed_argv = {
    tuple(item.get("const") for item in option.get("prefixItems", []))
    for option in argv_schema.get("oneOf", [])
}
expected_argv = {
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-Dcheckstyle.skip",
        "-DskipTests",
        "compile",
        "exec:java",
        "-Dalice-ide",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "netbeans",
        "-am",
        "-Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.ide.ProjectSaveTargetPlanTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.ide.ProjectLoadFailureDispatchPlanTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest",
        "test",
    ),
    ("qa/outside-in/alice-desktop/runners/netbeans-package-smoke.sh",),
    ("qa/outside-in/alice-desktop/runners/package-install-smoke.sh",),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-pl",
        "netbeans",
        "-am",
        "-Dtest=org.alice.netbeans.Alice3ProjectTemplateWizardIteratorTest,org.alice.netbeans.palette.Alice3PaletteFactoryTest,org.alice.netbeans.palette.items.AliceComponentPaletteUtilitiesTest,org.alice.netbeans.palette.items.resources.PaletteBundleLocalizationTest,org.alice.netbeans.completion.Alice3CompletionItemTest",
        "test",
    ),
    (
        "qa/outside-in/alice-desktop/runners/run-scenario.sh",
        "run",
        "alice-desktop-launch",
        "--timeout-seconds",
        "30",
        "--evidence-dir",
        "qa/outside-in/alice-desktop/evidence/future-ui-launch",
    ),
}
if allowed_argv != expected_argv:
    raise AssertionError("automation.argv must be restricted to the allowed Alice QA argv set")
for option in argv_schema.get("oneOf", []):
    size = len(option.get("prefixItems", []))
    if option.get("minItems") != size or option.get("maxItems") != size or option.get("items") is not False:
        raise AssertionError("each automation.argv schema option must be exact length")

cwd_schema = automation["properties"]["cwd"]
if "const" in cwd_schema:
    raise AssertionError("automation.cwd must not rely on a single cwd const; validator enforces allowed cwd/argv pairs")
cwd_pattern = cwd_schema.get("pattern", "")
if "(?!/)" not in cwd_pattern or "\\.\\." not in cwd_pattern:
    raise AssertionError("automation.cwd schema must reject absolute paths and parent traversal")

def has_xvfb_condition(node):
    if isinstance(node, dict):
        if "if" in node and "then" in node:
            probe = json.dumps(node)
            return "xvfb-real-alice" in probe and "automation" in probe
        return any(has_xvfb_condition(value) for value in node.values())
    if isinstance(node, list):
        return any(has_xvfb_condition(value) for value in node)
    return False

if not has_xvfb_condition(schema):
    raise AssertionError(
        "schema must encode the validator contract that xvfb-real-alice scenarios "
        "require automation settings"
    )

print("schema contract satisfied")
PY
schema_status=$?
assert_success "$schema_status" "schema encodes top-level and xvfb automation requirements"

finish
