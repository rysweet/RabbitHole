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
    "name",
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
target_starter = schema["properties"].get("targetStarter")
if not isinstance(target_starter, dict):
    raise AssertionError("schema must accept targetStarter metadata for target-specific evidence scenarios")
if target_starter.get("additionalProperties") is not False:
    raise AssertionError("targetStarter must reject unknown metadata fields")
required_target = set(target_starter.get("required", []))
expected_target = {"displayName", "repositoryPath"}
missing_target = sorted(expected_target - required_target)
if missing_target:
    raise AssertionError(f"targetStarter must require displayName and repositoryPath: {missing_target}")
target_properties = target_starter.get("properties", {})
if target_properties.get("displayName", {}).get("minLength") != 1:
    raise AssertionError("targetStarter.displayName must be a non-empty string")
repo_schema = target_properties.get("repositoryPath", {})
repo_pattern = repo_schema.get("pattern", "")
if "(?!/)" not in repo_pattern or "\\.\\." not in repo_pattern:
    raise AssertionError("targetStarter.repositoryPath schema must reject absolute paths and parent traversal")
if repo_schema.get("minLength") != 1:
    raise AssertionError("targetStarter.repositoryPath must be a non-empty string")

workflow_enum = set(schema["properties"]["workflow"]["enum"])
if "first-lesson-live-procedure-target-observation" not in workflow_enum:
    raise AssertionError(
        "schema workflow enum must include first-lesson-live-procedure-target-observation"
    )
if "run-window-contract" not in workflow_enum:
    raise AssertionError("schema workflow enum must include run-window-contract")
if "exported-project-ant-build-smoke" not in workflow_enum:
    raise AssertionError("schema workflow enum must include exported-project-ant-build-smoke")
if "silver-thread-launch-build-run" not in workflow_enum:
    raise AssertionError("schema workflow enum must include silver-thread-launch-build-run")
if "exported-project-smoke" in workflow_enum:
    raise AssertionError("schema workflow enum must not keep the stale exported-project-smoke workflow")

required_automation = set(automation.get("required", []))
expected_automation = {"cwd", "argv", "readyWaitSeconds"}
missing_automation = sorted(expected_automation - required_automation)
if missing_automation:
    raise AssertionError(
        "automation object must require all command fields when present: "
        f"{missing_automation}"
    )
if "timeoutSeconds" in required_automation:
    raise AssertionError(
        "automation.timeoutSeconds must not be globally required; "
        "the Save proof scenario has a no-workflow-timeout contract"
    )

if "command" in automation.get("properties", {}):
    raise AssertionError("legacy command field must not be part of the schema")

argv_schema = automation["properties"]["argv"]
allowed_argv = {
    tuple(item.get("const") for item in option.get("prefixItems", []))
    for option in argv_schema.get("oneOf", [])
}
robot_save_menu_argv = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ide",
    "-am",
    "-Dtest=org.alice.ide.croquet.models.projecturi.RobotSaveMenuDialogWriteReadbackProofTest",
    "test",
)
stale_stage_save_menu_argv = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ide",
    "-am",
    "-Dtest=org.alice.ide.croquet.models.projecturi.StageIdeSaveMenuDoClickToWriteProofTest",
    "test",
)
run_window_contract_argv = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-pl",
    "core/ide",
    "-am",
    "-Dtest=org.alice.tools.EatmeRunWindowEvidenceTest",
    "test",
)
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
        "-Dcheckstyle.skip",
        "-DskipTests",
        "compile",
        "exec:exec@alice-ide-atk",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "netbeans",
        "-am",
        "-Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/story-api-migration",
        "-am",
        "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/story-api-migration",
        "-am",
        "-Dtest=org.lgna.project.migration.ProjectMigrationManagerTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/story-api-migration",
        "-am",
        "-Dtest=org.lgna.project.io.IoUtilitiesTest",
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
    robot_save_menu_argv,
    run_window_contract_argv,
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.ide.uricontent.FileProjectLoaderTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/issue-reporting",
        "-am",
        "-Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.ide.SilverThreadLaunchBuildRunTest",
        "test",
    ),
    ("qa/outside-in/alice-desktop/runners/netbeans-package-smoke.sh",),
    ("qa/outside-in/alice-desktop/runners/package-install-smoke.sh",),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ast",
        "-am",
        "-Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeCreatesMethodInvocation",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ast",
        "-am",
        "-Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall+zeroArgumentThisMethodCallDecodeRejectsImplicitTarget",
        "test",
    ),
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
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.tools.EatmeEditProcedureTest#editsSceneProcedureAndWritesEatmeProofArtifacts",
        "test",
    ),
    (
        "mvn",
        "-DincludeSims=false",
        "-Dinstall4j.skip",
        "-DfailIfNoTests=false",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "-pl",
        "core/ide",
        "-am",
        "-Dtest=org.alice.tools.EatmeEditProcedureTest#chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff",
        "test",
    ),
}
if allowed_argv != expected_argv:
    raise AssertionError("automation.argv must be restricted to the allowed Alice QA argv set")
if robot_save_menu_argv not in allowed_argv:
    raise AssertionError("schema must allow the Robot Save menu dialog write/readback proof argv")
if run_window_contract_argv not in allowed_argv:
    raise AssertionError("schema must allow the Run-window creation/wiring contract proof argv")
if stale_stage_save_menu_argv in allowed_argv:
    raise AssertionError("schema must not keep the stale Stage doClick proof argv for the QA scenario")
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
