# Alice desktop outside-in QA reference

This reference describes the Alice desktop outside-in QA lane: file layout, runner commands, scenario schema, automation modes, configuration, and evidence artifacts.

## Contents

- [Directory layout](#directory-layout)
- [Scenario catalog](#scenario-catalog)
- [Runner commands](#runner-commands)
- [Environment variables](#environment-variables)
- [Scenario schema](#scenario-schema)
- [Automation modes](#automation-modes)
- [Evidence contract](#evidence-contract)
- [Workflow evidence requirements](#workflow-evidence-requirements)
- [Scenario authoring rules](#scenario-authoring-rules)
- [Extension rules](#extension-rules)

## Directory layout

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/README.md` | Local entry point for the QA lane. |
| `qa/outside-in/alice-desktop/scenarios/` | User-like acceptance scenario YAML files. |
| `qa/outside-in/alice-desktop/schema/scenario.schema.json` | Published JSON Schema contract for the scenario model. |
| `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` | Catalog validator and scenario JSON dumper. |
| `qa/outside-in/alice-desktop/runners/run-scenario.sh` | Scenario listing, validation, real launch execution, and manual checklist generation. |
| `qa/outside-in/alice-desktop/evidence/` | Local generated evidence. Contents are ignored by Git except `.gitignore`. |

## Scenario catalog

| Scenario ID | Workflow | Automation mode | Purpose |
| --- | --- | --- | --- |
| `alice-desktop-launch` | `launch` | `xvfb-real-alice` | Starts the real Alice desktop through Maven under Xvfb and captures launch evidence. |
| `alice-desktop-instructor-student-setup` | `instructor-student-setup` | `manual-evidence-required` | Covers instructor starter-project preparation and student project opening/saving. |
| `alice-desktop-scene-creation` | `scene-creation` | `manual-evidence-required` | Covers creating or selecting a starter scene and saving it as an Alice project. |
| `alice-desktop-run-debug` | `run-debug` | `manual-evidence-required` | Covers program run controls plus the closest baseline debug-like control, such as fast-forward or statement execution. |
| `alice-desktop-save-load` | `save-load` | `manual-evidence-required` | Covers saving an `.a3p` project, reopening it, and checking persistence. |
| `alice-desktop-open-load-save` | `open-load-save` | `manual-evidence-required` | Covers opening an existing `.a3p`, saving a copy, reopening it, and comparing visible state. |
| `alice-desktop-export` | `export` | `manual-evidence-required` | Covers the current Alice export path and verification of the exported artifact. |
| `alice-desktop-exported-project-smoke` | `exported-project-smoke` | `gated-command-smoke` | Covers generated Java project compile/launcher handoff evidence without running by default. |
| `alice-desktop-netbeans-package-smoke` | `netbeans-package-smoke` | `gated-command-smoke` | Covers NetBeans package command and representative NBM/support artifact checks. |
| `alice-desktop-package-install-smoke` | `package-install-smoke` | `gated-command-smoke` | Covers package build artifact inspection plus disposable install/launch evidence when artifacts are available. |
| `alice-desktop-project-io-smoke` | `project-io-smoke` | `gated-command-smoke` | Covers saving, reopening, editing, saving again, reopening again, and exporting a synthetic Alice project at the command seam. |
| `alice-desktop-failure-path-smoke` | `failure-path-smoke` | `gated-command-smoke` | Covers corrupt project input failure handling evidence. |
| `alice-desktop-future-ui-smoke` | `future-ui-smoke` | `gated-command-smoke` | Placeholder for controlled-display UI startup evidence; no-op unless gated on. |
| `alice-desktop-menu-action-smoke` | `menu-action-smoke` | `gated-command-smoke` | Covers launch-adjacent Alice desktop menu registration and controller lookup seams without display assumptions. |
| `alice-desktop-wizard-palette-completion-smoke` | `wizard-palette-completion-smoke` | `gated-command-smoke` | Covers focused wizard, palette, and completion affordance checks where current NetBeans tests can observe them. |

## Runner commands

Run commands from the repository root.

| Command | Purpose | Output contract |
| --- | --- | --- |
| `validate-scenarios.sh` | Validate the active scenario catalog. | Prints the number of valid scenarios and the active catalog directory. |
| `validate-scenarios.sh --list` | List normalized scenario records. | Prints scenario ID, automation mode, and title. |
| `validate-scenarios.sh --dump-json` | Dump the full normalized catalog. | Prints a JSON array sorted by scenario file path. |
| `validate-scenarios.sh --dump-json <scenario-id>` | Dump one normalized scenario. | Prints a JSON object for the requested scenario ID. |
| `run-scenario.sh list` | List runnable scenarios. | Prints the same user-facing list as the validator. |
| `run-scenario.sh validate` | Validate the active catalog through the runner. | Delegates to `validate-scenarios.sh`. |
| `run-scenario.sh run <scenario-id-or-path>` | Create evidence for one scenario. | Prints the created run directory and writes artifacts under the evidence directory. |
| `uvx --from git+<repo>@<branch> amplihack alice-qa list` | Install the QA wrapper from a branch and list scenarios in the current checkout. | Prints the same user-facing list as the runner. |
| `uvx --from git+<repo>@<branch> amplihack alice-qa run <scenario-id-or-path>` | Install the QA wrapper from a branch and create evidence in the current checkout. | Delegates to `run-scenario.sh run`. |

### Validate all scenarios

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

### List scenarios

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --list
qa/outside-in/alice-desktop/runners/run-scenario.sh list
```

### Dump a scenario as JSON

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json alice-desktop-launch
```

Without an argument, `--dump-json` prints the full catalog as an array. With a scenario ID, it prints exactly one scenario object. Unknown scenario IDs fail with a non-zero exit status.

### Run a scenario

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run <scenario-id-or-path>
```

The runner accepts either a scenario ID or a `.yaml` file path. Scenario paths must be direct files inside the active scenario directory; nested paths and paths outside the active catalog are rejected.

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  qa/outside-in/alice-desktop/scenarios/launch.yaml
```

This path form resolves the top-level `id` in the YAML file, validates that ID through the active catalog, and then runs the normalized scenario.

### Run through the branch-installable wrapper

```bash
uvx --from git+https://github.com/rysweet/alice3-modernization.git@feat/alice-qa-outside-in \
  amplihack alice-qa list

uvx --from git+https://github.com/rysweet/alice3-modernization.git@feat/alice-qa-outside-in \
  amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The `amplihack alice-qa` wrapper is intentionally thin. It must be run from an Alice checkout, locates the repository root from the current working directory, and delegates to the checked-out shell runners.

### Run with a custom evidence directory

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run <scenario-id> \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

### Run with a custom timeout

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch \
  --timeout-seconds 180
```

`--timeout-seconds` applies to `xvfb-real-alice` execution. Manual scenarios write checklists immediately.

### Prepare a gated smoke without execution

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-netbeans-package-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

`--prepare-only` is the intentional preflight mode for gated command smokes. It writes `outcome=gated-not-run` evidence and returns success without executing the configured command.

### Exit behavior

Runner and validator commands return a non-zero exit status when the catalog is invalid, a requested scenario is unknown, a scenario path is outside the active catalog, a timeout value is invalid, an automation mode is unsupported, a gated command smoke is not enabled and `--prepare-only` was not requested, or a required launch/evidence capture step fails.

## Environment variables

| Variable | Applies to | Default | Description |
| --- | --- | --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Validator and runner | `qa/outside-in/alice-desktop/scenarios` | Overrides the directory containing scenario YAML files. Scenario path arguments are resolved against this active catalog. |
| `ALICE_QA_DISPLAY` | Xvfb runs | First free display from `:90` through `:120` | Reuses a specific X display instead of selecting one automatically. |
| `ALICE_QA_SCREEN` | Xvfb runs | `1280x900x24` | Sets Xvfb screen geometry. |
| `ALICE_QA_READY_WAIT_SECONDS` | Xvfb runs | Scenario `automation.readyWaitSeconds` | Overrides the scenario readiness wait before screenshot capture. |
| `ALICE_QA_RUN_GATED_SMOKES` | Gated command smokes | unset | Set to `1` to execute configured command smokes. When unset, the runner writes `outcome=gated-not-run` status and a checklist, then exits non-zero unless `--prepare-only` was requested. |
| `NODE_OPTIONS` | Surrounding Node tooling | unset | Use `--max-old-space-size=32768` when a larger QA orchestrator invokes Node-based helpers around this lane. The lane itself does not require Node. |

Example:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_DISPLAY=:99 \
ALICE_QA_SCREEN=1600x1000x24 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

## Scenario schema

Each scenario is a YAML file whose file name matches its ID without the `alice-desktop-` prefix.

The JSON Schema is the published scenario contract. The validator uses a small built-in parser so this lane has no extra runtime dependency, and it must stay in parity with the schema's required fields, allowed values, and cross-field rules. When the schema changes, update the validator and its contract tests in the same change.

Scenario YAML must use the supported subset: simple mappings, nested mappings, and lists of scalar values. Do not use anchors, aliases, tags, multiline scalars, flow-style collections, tabs for indentation, or other advanced YAML features.

Example:

```yaml
id: alice-desktop-save-load
title: Save and load an Alice project
workflow: save-load
automationMode: manual-evidence-required
preconditions:
  - Java 21 is available.
  - Maven dependencies are available.
  - The tweedle-lang submodule is initialized.
userActions:
  - Launch Alice.
  - Create or open a small project.
  - Save the project as an a3p file in a known evidence location.
  - Close the project or restart Alice.
  - Open the saved a3p file.
  - Verify the loaded project matches the saved state.
expectedOutcomes:
  - Alice writes a usable a3p project file.
  - Alice opens the saved a3p project without uncaught application errors.
  - The visible scene or program state after loading matches the saved project.
evidence:
  required:
    - Save operation log or manual notes.
    - Saved a3p project file.
    - Screenshot before saving.
    - Screenshot after reopening.
    - review-notes.txt comparing the saved and loaded state.
fallback:
  mode: manual-evidence-required
  notes:
    - Link lower-level Maven or JUnit characterization evidence for project loading when available.
    - Manual evidence remains required until stable full GUI save and open automation exists.
supportingEvidence:
  - alice-desktop-launch
```

### Required fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | string | Scenario ID. Must match `alice-desktop-[a-z0-9-]+`. |
| `title` | string | Human-readable scenario title. |
| `workflow` | enum | Covered workflow. |
| `automationMode` | enum | How the runner handles the scenario. |
| `preconditions` | string list | Required starting conditions. |
| `userActions` | string list | User-like actions, written from outside the implementation. |
| `expectedOutcomes` | string list | Observable outcomes that show success. |
| `evidence.required` | string list | Files, screenshots, logs, artifacts, or notes required for review. |
| `fallback.mode` | enum | Fallback automation mode. |
| `fallback.notes` | string list | Specific fallback instructions. |

### Optional fields

| Field | Type | Description |
| --- | --- | --- |
| `automation.cwd` | string | Repository-relative working directory for argv-backed automation. Required for `xvfb-real-alice` and `gated-command-smoke`; absolute paths, `..`, and realpath escapes outside the repository are rejected. |
| `automation.argv` | string list | Argument vector executed directly by the runner without shell interpretation. Required for `xvfb-real-alice` and `gated-command-smoke`; only the checked-in Alice QA argv allowlist is accepted. |
| `automation.timeoutSeconds` | positive integer | Default timeout for argv-backed automation. Required for `xvfb-real-alice` and `gated-command-smoke`. |
| `automation.readyWaitSeconds` | positive integer | Wait before screenshot capture for UI automation; use `1` for command smokes. Required for `xvfb-real-alice` and `gated-command-smoke`. |
| `supportingEvidence` | string list | Scenario IDs or evidence sources that support this scenario. |
| `tags` | string list | Additional scenario labels. |

`automation` is required when `automationMode` is `xvfb-real-alice` or `gated-command-smoke`. Manual scenarios do not need an `automation` block because the runner generates a checklist instead of driving Swing interactions. Automation must be represented as `argv`; shell command strings are not accepted, including in custom catalogs selected with `ALICE_QA_SCENARIO_DIR`.

### Workflow values

```text
instructor-student-setup
launch
exported-project-smoke
failure-path-smoke
future-ui-smoke
netbeans-package-smoke
open-load-save
package-install-smoke
project-io-smoke
scene-creation
run-debug
save-load
export
wizard-palette-completion-smoke
```

## Automation modes

| Mode | Runner behavior |
| --- | --- |
| `xvfb-real-alice` | Starts Xvfb, launches Alice through the allowed scenario argv, waits for readiness, and captures environment data, logs, status, and screenshot when the launch reaches evidence capture. This is a launch evidence check, not a full semantic oracle for every startup log condition. |
| `manual-evidence-required` | Writes a structured checklist for human execution and evidence collection. Checklist generation does not complete the scenario. |
| `gated-command-smoke` | Writes environment, status, and checklist evidence by default without running heavy commands. When `ALICE_QA_RUN_GATED_SMOKES=1`, runs the configured command under `timeout`, captures `command.log`, and records pass/fail status. |

## Evidence contract

Every run creates a timestamped directory before scenario execution begins:

```text
<evidence-dir>/<scenario-id>/<timestamp>/
```

The default evidence directory is:

```text
qa/outside-in/alice-desktop/evidence/
```

The artifact set depends on the automation mode and how far execution gets.

Manual scenario preparation includes:

| Artifact | Description |
| --- | --- |
| `environment.txt` | UTC timestamp, repository root, display, Java version, Maven version, and OS details. |
| `status.txt` | Scenario ID, automation mode, generated checklist name, and `manual-evidence-required` outcome. |
| `manual-evidence-checklist.txt` | Scenario preconditions, actions, outcomes, required evidence, and fallback notes. This file prepares the work; it is not proof that the workflow has been executed. |

Gated command smoke preparation includes:

| Artifact | Description |
| --- | --- |
| `environment.txt` | UTC timestamp, repository root, display, Java version, Maven version, and OS details. |
| `status.txt` | Scenario ID, automation mode, `outcome=gated-not-run`, gate name, skip mode, command, working directory, timeout, and generated checklist name. |
| `manual-evidence-checklist.txt` | Review checklist describing what evidence is required when the gate is enabled or fulfilled elsewhere. |

Enabled gated command smoke execution also includes:

| Artifact | Description |
| --- | --- |
| `command.log` | Captured stdout/stderr for the configured command. |
| `status.txt` | Scenario ID, automation mode, command, working directory, timeout, command log name, exit code, and `outcome=passed` or `outcome=failed`. |

Successful `xvfb-real-alice` evidence capture includes:

| Artifact | Description |
| --- | --- |
| `environment.txt` | UTC timestamp, repository root, display, Java version, Maven version, and OS details. |
| `launch.log` | Alice Maven launch output. |
| `xvfb.log` | Xvfb output. |
| `status.txt` | Scenario ID, automation mode, display, readiness status, process status, screenshot status, and timeout. |
| `screenshot.png` or `screenshot.xwd` | Captured desktop image. |
| `screenshot.log` | Screenshot command output. |

For launch runs, `status.txt` records whether the process stayed alive, whether a visible window was detected when a detector is available, and whether screenshot capture succeeded. Acceptance still requires reviewing the generated evidence, especially `launch.log`; the runner does not currently scan the log for every possible uncaught application exception.

Early `xvfb-real-alice` fallback attempts may not produce the full launch artifact set. If Xvfb is missing or no display is available, the runner writes `environment.txt` plus `manual-evidence-checklist.txt` and exits non-zero. If Xvfb starts but exits before Alice launch, the run directory contains `xvfb.log` plus `manual-evidence-checklist.txt`. In these early fallback cases, `status.txt` is not written because the launch did not reach the evidence-capture phase.

Manual scenarios are complete only after a human performs the workflow and places the required artifacts in the same timestamped run directory. Every accepted manual run must include `review-notes.txt` with the scenario ID, run directory, evidence files reviewed, observed result, deviations from the checklist, and an explicit accept or reject decision.

## Workflow evidence requirements

| Workflow | Required evidence |
| --- | --- |
| Launch | Launch log, desktop screenshot, exit/status/timeout record, Java/Maven/display environment summary. |
| Instructor/student setup | Instructor launch log, starter project screenshot, starter `.a3p`, student launch or open log, loaded project screenshot, student copy `.a3p`, `review-notes.txt`. |
| Scene creation | Screenshot before scene creation, screenshot after object or scene appears, saved `.a3p`, notes identifying the selected template or object in `review-notes.txt`. |
| Run/debug | Screenshot before run, screenshot or screen capture during execution, notes naming run/debug-like controls in `review-notes.txt`, launch or run log, saved `.a3p`. |
| Save/load | Save log or notes, saved `.a3p`, screenshot before saving, screenshot after reopening, comparison notes in `review-notes.txt`. |
| Open/load/save | Open log or notes identifying the source `.a3p`, screenshot after first open, saved copy `.a3p`, screenshot after reopening the copy, comparison decision in `review-notes.txt`. |
| Export | Export log or notes, screenshot before export, screenshot after export completion, exported artifact, file listing or checksum, `review-notes.txt`. |
| Exported project smoke | `status.txt`, `command.log`, generated source or exported project listing, launcher handoff, compile evidence, or [exported Ant runtime metadata evidence](./exported-netbeans-ant-project-behavior.md) such as `ANT_RUNTIME_CONFIGURATION_PROBE_OK`. |
| NetBeans package smoke | `status.txt`, `command.log`, NetBeans target artifact listing or CI artifact link, representative jar/zip content listing. |
| Package/install smoke | `status.txt`, `command.log`, package or installer artifact listing, disposable install log or explicit not-produced note. |
| Project save, reopen, edit, save again, reopen again, and export smoke | `status.txt`, `command.log`, test output or surefire report naming `IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`, and review notes for metadata and export archive structure assertions. No durable saved-project artifact is required because the smoke uses test-local temporary files. |
| Failure path smoke | `status.txt`, `command.log`, failure classification or dispatch-plan output, corrupt input fixture name or generated fixture notes. |
| Future UI smoke | `status.txt`, `command.log` when gated, startup screenshot or first-window signal when collected, manual fallback notes otherwise. |
| Wizard/palette/completion smoke | `status.txt`, `command.log`, focused test output for wizard validation, palette wiring, and completion resources; manual screenshot notes when desktop evidence is added. |

## Scenario authoring rules

Scenario files are the public acceptance contract for this lane. A valid scenario:

1. Uses an ID in the `alice-desktop-<workflow>` family.
2. Keeps `userActions` and `expectedOutcomes` observable from the desktop user's point of view.
3. Names evidence that a reviewer can inspect without reconstructing hidden local state.
4. Uses `xvfb-real-alice` only for workflows the runner can execute through the real Alice desktop command.
5. Uses `manual-evidence-required` for Swing GUI workflows that still require human interaction.
6. Uses `gated-command-smoke` for expensive CLI/package or future UI smokes that must not be mandatory in lightweight validation.
7. Lists any dependent scenario evidence in `supportingEvidence`, such as using launch evidence to support save/load or export evidence.
8. Requires `review-notes.txt` for manual workflow acceptance.
9. Uses only the supported YAML subset: mappings, nested mappings, scalar values, and scalar lists with spaces for indentation.
10. Uses `automation.argv` rather than a shell command string; only the allowlisted Alice QA argv set is accepted.
11. Avoids implementation details such as Java class names, internal package names, or assumptions about private UI objects.

## Extension rules

When adding or changing scenarios:

1. Keep the scenario user-like. Describe what the instructor, student, or Alice user does and observes.
2. Prefer real Alice execution through the runner when it is stable.
3. Use `manual-evidence-required` when Swing GUI interaction is not stable enough to automate.
4. Use `gated-command-smoke` when the scenario is executable but too expensive or environment-sensitive for default validation.
5. Do not introduce Playwright unless Alice exposes a browser/web surface.
6. Do not use a virtual TTY for Swing GUI interaction.
7. Preserve Alice 3 baseline behavior unless a behavior change is explicitly documented and tested.
8. Validate the catalog before committing:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```
