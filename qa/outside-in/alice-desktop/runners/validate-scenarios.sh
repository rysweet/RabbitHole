#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
SCENARIO_DIR="${ALICE_QA_SCENARIO_DIR:-$BASE_DIR/scenarios}"
SCHEMA_PATH="$BASE_DIR/schema/scenario.schema.json"
REPO_ROOT=$(CDPATH= cd -- "$BASE_DIR/../../.." && pwd)

python3 - "$SCENARIO_DIR" "$SCHEMA_PATH" "$REPO_ROOT" "$@" <<'PY'
import json
import re
import sys
from pathlib import Path

scenario_dir = Path(sys.argv[1])
schema_path = Path(sys.argv[2])
repo_root = Path(sys.argv[3]).resolve(strict=True)
args = sys.argv[4:]

required_top = [
    "id",
    "title",
    "workflow",
    "automationMode",
    "preconditions",
    "userActions",
    "expectedOutcomes",
    "evidence",
    "fallback",
]
allowed_top = set(required_top) | {"automation", "supportingEvidence", "tags", "targetStarter", "name", "steps", "agents"}
EXPECTED_TARGET_STARTER = {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p",
}
TARGET_STARTER_SCENARIO_IDS = {
    "alice-desktop-select-project-tab-click-exec",
    "alice-desktop-post-project-open-window-state",
    "alice-desktop-post-open-runtime-display-accessibility-evidence",
    "alice-desktop-first-lesson-live-procedure-target-observation",
}
workflow_values = {
    "archive-fixture-smoke",
    "exported-project-ant-build-smoke",
    "failure-path-smoke",
    "file-loader-smoke",
    "first-lesson-live-procedure-target-observation",
    "future-ui-smoke",
    "instructor-student-setup",
    "launch",
    "menu-action-smoke",
    "model-export-boundary-smoke",
    "netbeans-package-smoke",
    "open-load-save",
    "package-install-smoke",
    "project-io-smoke",
    "scene-creation",
    "run-debug",
    "save-load",
    "save-menu-dialog-write-proof",
    "select-project-interaction-smoke",
    "select-project-widget-introspection-smoke",
    "select-project-atk-exec-smoke",
    "select-project-tab-click-smoke",
    "post-project-open-window-state-smoke",
    "post-open-runtime-display-accessibility-evidence",
    "procedure-edit-handoff-smoke",
    "procedure-edit-seam-smoke",
    "silver-thread-launch-build-run",
    "tweedle-decoder-boundary-smoke",
    "tweedle-decoder-this-call-smoke",
    "export",
    "wizard-palette-completion-smoke",
}
mode_values = {
    "gated-command-smoke",
    "xvfb-real-alice",
    "manual-evidence-required",
}
allowed_automation = {
    (
        "alice-ide",
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
    ),
    (
        "alice-ide",
        (
            "mvn",
            "-DincludeSims=false",
            "-Dinstall4j.skip",
            "-Dcheckstyle.skip",
            "-DskipTests",
            "compile",
            "exec:exec@alice-ide-atk",
        ),
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
        (
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
        ),
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
        (
            "qa/outside-in/alice-desktop/runners/netbeans-package-smoke.sh",
        ),
    ),
    (
        ".",
        (
            "qa/outside-in/alice-desktop/runners/package-install-smoke.sh",
        ),
    ),
    (
        ".",
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
    ),
    (
        ".",
        (
            "qa/outside-in/alice-desktop/runners/run-scenario.sh",
            "run",
            "alice-desktop-launch",
            "--timeout-seconds",
            "30",
            "--evidence-dir",
            "qa/outside-in/alice-desktop/evidence/future-ui-launch",
        ),
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
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
    ),
    (
        ".",
        (
            "mvn",
            "-DincludeSims=false",
            "-Dinstall4j.skip",
            "-DfailIfNoTests=false",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "-pl",
            "core/model-loading",
            "-am",
            "-Dtest=org.lgna.story.resourceutilities.ModelExportTest",
            "test",
        ),
    ),
}


class ScenarioError(Exception):
    """Raised for invalid scenario catalogs."""


def parse_scalar(value):
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    if value == "true":
        return True
    if value == "false":
        return False
    if re.fullmatch(r"-?[0-9]+", value):
        return int(value)
    return value


def load_yaml_subset(path):
    lines = []
    for line_number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        indent_text = raw[: len(raw) - len(raw.lstrip(" "))]
        if "\t" in indent_text:
            raise ScenarioError(f"{path}:{line_number}: tabs are not supported for indentation")
        lines.append((line_number, len(indent_text), raw.strip()))

    if not lines:
        raise ScenarioError(f"{path}: scenario is empty")

    def parse_block(index, indent):
        if index >= len(lines) or lines[index][1] < indent:
            return None, index
        if lines[index][1] != indent:
            line_number = lines[index][0]
            raise ScenarioError(f"{path}:{line_number}: expected indentation {indent}")

        if lines[index][2].startswith("- "):
            values = []
            while index < len(lines) and lines[index][1] == indent and lines[index][2].startswith("- "):
                line_number, _, text = lines[index]
                rest = text[2:].strip()
                if not rest:
                    value, index = parse_block(index + 1, indent + 2)
                else:
                    value = parse_scalar(rest)
                    index += 1
                if value is None:
                    raise ScenarioError(f"{path}:{line_number}: list item must have a value")
                values.append(value)
            return values, index

        values = {}
        while index < len(lines) and lines[index][1] == indent and not lines[index][2].startswith("- "):
            line_number, _, text = lines[index]
            if ":" not in text:
                raise ScenarioError(f"{path}:{line_number}: expected key: value")
            key, value_text = text.split(":", 1)
            key = key.strip()
            value_text = value_text.strip()
            if not key:
                raise ScenarioError(f"{path}:{line_number}: key must not be empty")
            if value_text:
                values[key] = parse_scalar(value_text)
                index += 1
            else:
                value, next_index = parse_block(index + 1, indent + 2)
                values[key] = {} if value is None else value
                index = next_index
        return values, index

    data, final_index = parse_block(0, lines[0][1])
    if final_index != len(lines):
        line_number = lines[final_index][0]
        raise ScenarioError(f"{path}:{line_number}: unsupported nested structure")
    if not isinstance(data, dict):
        raise ScenarioError(f"{path}: top-level scenario must be a mapping")
    return data


def require_string_list(errors, path, name, value):
    if not isinstance(value, list) or not value:
        errors.append(f"{name} must be a non-empty list")
        return
    for index, item in enumerate(value, 1):
        if not isinstance(item, str) or not item.strip():
            errors.append(f"{name}[{index}] must be a non-empty string")


def validate_automation_cwd(errors, cwd):
    if not isinstance(cwd, str) or not cwd.strip():
        errors.append("automation.cwd must be a non-empty string")
        return

    cwd_path = Path(cwd)
    if cwd_path.is_absolute():
        errors.append("automation.cwd must be repository-relative, not absolute")
        return
    if any(part == ".." for part in cwd_path.parts):
        errors.append("automation.cwd must not contain .. path traversal")
        return

    try:
        resolved = (repo_root / cwd_path).resolve(strict=True)
    except FileNotFoundError:
        errors.append("automation.cwd must be an existing directory inside repository root")
        return

    if not resolved.is_dir():
        errors.append("automation.cwd must be an existing directory inside repository root")
        return

    try:
        resolved.relative_to(repo_root)
    except ValueError:
        errors.append("automation.cwd must resolve inside repository root")


def validate_target_starter(errors, scenario_id, value):
    if value is None:
        if scenario_id in TARGET_STARTER_SCENARIO_IDS:
            errors.append("targetStarter is required for target-specific project-open evidence scenarios")
        return
    if not isinstance(value, dict):
        errors.append("targetStarter must be a mapping")
        return

    unknown_target = sorted(set(value) - {"displayName", "repositoryPath"})
    if unknown_target:
        errors.append(f"targetStarter has unknown field(s): {', '.join(unknown_target)}")

    display_name = value.get("displayName")
    repository_path = value.get("repositoryPath")
    if not isinstance(display_name, str) or not display_name.strip():
        errors.append("targetStarter.displayName must be a non-empty string")
    if not isinstance(repository_path, str) or not repository_path.strip():
        errors.append("targetStarter.repositoryPath must be a non-empty string")
    elif Path(repository_path).is_absolute():
        errors.append("targetStarter.repositoryPath must be repository-relative, not absolute")
    elif any(part == ".." for part in Path(repository_path).parts):
        errors.append("targetStarter.repositoryPath must not contain .. path traversal")

    if scenario_id in TARGET_STARTER_SCENARIO_IDS:
        if display_name != EXPECTED_TARGET_STARTER["displayName"]:
            errors.append("targetStarter.displayName must be Africa Full for target-specific project-open evidence scenarios")
        if repository_path != EXPECTED_TARGET_STARTER["repositoryPath"]:
            errors.append(
                "targetStarter.repositoryPath must be "
                f"{EXPECTED_TARGET_STARTER['repositoryPath']} for target-specific project-open evidence scenarios"
            )


def validate(path, scenario):
    errors = []
    missing = [field for field in required_top if field not in scenario]
    if missing:
        errors.append(f"missing required field(s): {', '.join(missing)}")

    unknown = sorted(set(scenario) - allowed_top)
    if unknown:
        errors.append(f"unknown field(s): {', '.join(unknown)}")

    scenario_id = scenario.get("id")
    if not isinstance(scenario_id, str) or not re.fullmatch(r"alice-desktop-[a-z0-9-]+", scenario_id):
        errors.append("id must match alice-desktop-[a-z0-9-]+")
    elif path.stem != scenario_id.removeprefix("alice-desktop-"):
        errors.append("file name must match id without the alice-desktop- prefix")

    title = scenario.get("title")
    if not isinstance(title, str) or not title.strip():
        errors.append("title must be a non-empty string")

    name = scenario.get("name")
    if name is not None and name != title:
        errors.append("name must equal title when present")

    workflow = scenario.get("workflow")
    if workflow not in workflow_values:
        errors.append(f"workflow must be one of: {', '.join(sorted(workflow_values))}")

    automation_mode = scenario.get("automationMode")
    if automation_mode not in mode_values:
        errors.append(f"automationMode must be one of: {', '.join(sorted(mode_values))}")

    validate_target_starter(errors, scenario_id, scenario.get("targetStarter"))

    for name in ("preconditions", "userActions", "expectedOutcomes"):
        require_string_list(errors, path, name, scenario.get(name))

    evidence = scenario.get("evidence")
    if not isinstance(evidence, dict):
        errors.append("evidence must be a mapping")
    else:
        unknown_evidence = sorted(set(evidence) - {"required"})
        if unknown_evidence:
            errors.append(f"evidence has unknown field(s): {', '.join(unknown_evidence)}")
        require_string_list(errors, path, "evidence.required", evidence.get("required"))

    fallback = scenario.get("fallback")
    if not isinstance(fallback, dict):
        errors.append("fallback must be a mapping")
    else:
        unknown_fallback = sorted(set(fallback) - {"mode", "notes"})
        if unknown_fallback:
            errors.append(f"fallback has unknown field(s): {', '.join(unknown_fallback)}")
        if fallback.get("mode") not in mode_values:
            errors.append(f"fallback.mode must be one of: {', '.join(sorted(mode_values))}")
        require_string_list(errors, path, "fallback.notes", fallback.get("notes"))

    automation = scenario.get("automation")
    if automation is not None and not isinstance(automation, dict):
        errors.append("automation must be a mapping")
    elif isinstance(automation, dict):
        unknown_automation = sorted(
            set(automation) - {"cwd", "argv", "timeoutSeconds", "readyWaitSeconds"}
        )
        if unknown_automation:
            errors.append(f"automation has unknown field(s): {', '.join(unknown_automation)}")
        for field in ("cwd", "argv", "readyWaitSeconds"):
            if field not in automation:
                errors.append(f"automation must include {field} when present")
        if workflow == "save-menu-dialog-write-proof":
            if "timeoutSeconds" in automation:
                errors.append("save-menu-dialog-write-proof must not include automation.timeoutSeconds")
        elif "timeoutSeconds" not in automation:
            errors.append("automation must include timeoutSeconds when present")
        validate_automation_cwd(errors, automation.get("cwd"))
        require_string_list(errors, path, "automation.argv", automation.get("argv"))
        if "timeoutSeconds" in automation and (
            not isinstance(automation.get("timeoutSeconds"), int) or automation.get("timeoutSeconds", 0) < 1
        ):
            errors.append("automation.timeoutSeconds must be a positive integer")
        if not isinstance(automation.get("readyWaitSeconds"), int) or automation.get("readyWaitSeconds", 0) < 1:
            errors.append("automation.readyWaitSeconds must be a positive integer")
        cwd = automation.get("cwd")
        argv = automation.get("argv")
        if isinstance(cwd, str) and isinstance(argv, list) and all(isinstance(arg, str) for arg in argv):
            key = (cwd, tuple(argv))
            if key not in allowed_automation:
                errors.append(
                    "automation.argv is restricted to the allowed Alice QA command set"
                )
    if automation_mode in {"xvfb-real-alice", "gated-command-smoke"} and not isinstance(automation, dict):
        errors.append(f"{automation_mode} scenarios must include automation")

    if "supportingEvidence" in scenario:
        require_string_list(errors, path, "supportingEvidence", scenario.get("supportingEvidence"))
    if "tags" in scenario:
        require_string_list(errors, path, "tags", scenario.get("tags"))

    if errors:
        raise ScenarioError("\n".join(f"{path}: {error}" for error in errors))


def load_scenarios():
    if not schema_path.exists():
        raise ScenarioError(f"{schema_path}: schema file is missing")
    try:
        json.loads(schema_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ScenarioError(f"{schema_path}:{exc.lineno}: invalid JSON schema: {exc.msg}") from exc

    paths = sorted(scenario_dir.glob("*.yaml"))
    if not paths:
        raise ScenarioError(f"{scenario_dir}: no scenario YAML files found")

    scenarios = []
    seen_ids = {}
    for path in paths:
        scenario = load_yaml_subset(path)
        validate(path, scenario)
        scenario_id = scenario["id"]
        if scenario_id in seen_ids:
            raise ScenarioError(f"{path}: duplicate id also used by {seen_ids[scenario_id]}")
        seen_ids[scenario_id] = path
        scenarios.append((path, scenario))

    workflow_to_paths = {}
    for path, scenario in scenarios:
        workflow_to_paths.setdefault(scenario["workflow"], []).append(path)

    catalog_errors = []
    missing_workflows = sorted(workflow_values - set(workflow_to_paths))
    if missing_workflows:
        catalog_errors.append(f"missing workflow coverage: {', '.join(missing_workflows)}")

    duplicate_workflows = {
        workflow: paths
        for workflow, paths in workflow_to_paths.items()
        if len(paths) > 1
    }
    for workflow, duplicate_paths in sorted(duplicate_workflows.items()):
        joined_paths = ", ".join(str(path) for path in duplicate_paths)
        catalog_errors.append(f"duplicate workflow coverage for {workflow}: {joined_paths}")

    for path, scenario in scenarios:
        for reference in scenario.get("supportingEvidence", []):
            if reference not in seen_ids:
                catalog_errors.append(f"{path}: supportingEvidence reference not found: {reference}")

    if catalog_errors:
        raise ScenarioError("\n".join(catalog_errors))

    return scenarios


try:
    scenarios = load_scenarios()
except ScenarioError as exc:
    print(exc, file=sys.stderr)
    sys.exit(1)

if args[:1] == ["--list"]:
    width = max(len(scenario["id"]) for _, scenario in scenarios)
    for _, scenario in scenarios:
        print(f"{scenario['id']:<{width}}  {scenario['automationMode']:<24}  {scenario['title']}")
elif args[:1] == ["--dump-json"]:
    if len(args) == 1:
        print(json.dumps([scenario for _, scenario in scenarios], indent=2, sort_keys=True))
    elif len(args) == 2:
        requested_id = args[1]
        for _, scenario in scenarios:
            if scenario["id"] == requested_id:
                print(json.dumps(scenario, indent=2, sort_keys=True))
                break
        else:
            print(f"unknown scenario id: {requested_id}", file=sys.stderr)
            sys.exit(1)
    else:
        print("usage: validate-scenarios.sh --dump-json [scenario-id]", file=sys.stderr)
        sys.exit(2)
elif args:
    print("usage: validate-scenarios.sh [--list|--dump-json [scenario-id]]", file=sys.stderr)
    sys.exit(2)
else:
    print(f"Validated {len(scenarios)} scenario(s) in {scenario_dir}")
PY
