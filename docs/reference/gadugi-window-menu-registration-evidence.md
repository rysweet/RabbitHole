# Gadugi Window menu registration evidence scenario

This reference documents the Gadugi-compatible CLI scenario for Window menu
registration evidence checks. The scenario gives Gadugi tooling a repo-owned
entry point for the Alice desktop outside-in QA lane without changing the custom
Alice scenario schema.

Implementation files:

- `qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml`
- `qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh`

The scenario validates Window menu model registration evidence wiring by
delegating to the Alice outside-in runner. The default Gadugi path remains
prepare-only; the underlying checked-in Alice scenario is the bounded
`alice-desktop-menu-action-smoke` gated command smoke. The Gadugi lane does not
prove visible rendering, rendered pixels, visible desktop windows, JavaFX
scene-graph layout, full lesson completion, or the full Alice desktop workflow.

## Contents

- [Scope](#scope)
- [Files](#files)
- [Usage](#usage)
- [Configuration](#configuration)
- [Scenario contract](#scenario-contract)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Evidence boundaries](#evidence-boundaries)
- [Troubleshooting](#troubleshooting)

## Scope

The Gadugi scenario covers the Window menu registration evidence contract for
the Alice desktop outside-in QA lane. It is intentionally narrower than a full
desktop or lesson workflow:

1. It validates that Gadugi can discover, validate, and execute
   `window-menu-registration-evidence`.
2. It delegates to the existing Alice outside-in QA runners instead of adding a
   second menu/action validation path.
3. It uses the outside-in runner's prepare-only mode for the current
   `alice-desktop-menu-action-smoke` so the default path proves evidence wiring
   without requiring the gated Maven smoke.
4. When the underlying gated smoke is executed intentionally, it runs
   `AliceMenuBarContractTest`. That proof remains bounded to Window menu model
   registration, stable identity, and menu-bar membership lookup and must not
   claim rendered pixels, visible windows, live menu interaction, Save
   completion, assessment, or lesson completion.

For the Window menu action contract itself, see
[Window menu action contract](./window-menu-action-contract.md).
For the custom Alice outside-in scenario schema, see
[Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md).
For the PR #401 recovery handoff, see
[PR #401 UI Action Menu Contract Handoff](./pr401-ui-action-menu-contract-evidence.md).

## Files

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/gadugi/` | Gadugi-compatible QA scenario directory. This directory is separate from the custom Alice scenario catalog. |
| `qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml` | Gadugi CLI scenario that verifies Window menu registration evidence wiring through the Alice outside-in runner. |
| `qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh` | Shell contract test that validates the scenario path, identity, metadata, delegated commands, conservative description text, non-claims, and assertion emptiness using PyYAML (`yaml.safe_load`). |
| `qa/outside-in/alice-desktop/scenarios/menu-action-smoke.yaml` | Custom-schema Alice scenario consumed by the repo-owned outside-in runner. This file remains on the Alice custom schema and is not a Gadugi scenario. |

The `gadugi/` directory exists because `gadugi-test validate` uses a different
schema than the Alice custom outside-in `scenarios/` directory. Keeping the
formats separate lets both validators stay strict.

## Usage

Run commands from the repository root.

Prerequisite: `gadugi-test` is installed and available on `PATH`.

Validate the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml
```

Run the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s window-menu-registration-evidence \
  --timeout 300000
```

The scenario delegates to these existing Alice desktop outside-in QA entry
points:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration

qa/outside-in/alice-desktop/tests/run-tests.sh
```

`--prepare-only` is the default evidence-contract lane for Gadugi execution. It
creates reviewable outside-in runner evidence for the current menu-action smoke
and returns success without enabling the gated Maven smoke.
`ALICE_QA_RUN_GATED_SMOKES=1` only matters when running the underlying Alice
runner without `--prepare-only`. Use the gated runner only when a review
explicitly requires command execution evidence.

Run the contract test standalone:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh
```

The contract test is also picked up automatically by `run-tests.sh`.

## Configuration

The Gadugi scenario is intentionally small and repo-local.

| Field | Value | Contract |
| --- | --- | --- |
| Scenario directory | `qa/outside-in/alice-desktop/gadugi` | Gadugi discovers the dedicated Gadugi scenario files. |
| Scenario file | `qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml` | `gadugi-test validate -f` validates the runnable scenario. |
| Working directory | `.` | Scenario commands run from the repository root. |
| Interface | `cli` | The scenario is a command-line evidence check, not a browser, desktop, or rendering test. |
| Scenario name | `window-menu-registration-evidence` | Stable name used with `gadugi-test run -s window-menu-registration-evidence`. |
| Timeout | `180000` in the scenario, `300000` for the PR readiness wrapper run | The YAML keeps individual command timeouts short; the wrapper run leaves enough time for the delegated QA scripts. |
| Tags | `cli`, `gadugi`, `pr-401`, `menu-action`, `window-menu-registration-evidence` | Tags make the scenario discoverable as a PR #401 Window menu registration evidence check. |

The surrounding QA environment may set:

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Keeps Node-based Gadugi orchestration within the preferred memory limit. |
| `ALICE_QA_RUN_GATED_SMOKES=1` | Enables the underlying Alice gated command smoke only when intentionally running `run-scenario.sh` without `--prepare-only`. That smoke targets `AliceMenuBarContractTest`; the default Gadugi command remains prepare-only. |

Do not point Gadugi at `qa/outside-in/alice-desktop/scenarios/`; that directory
uses the custom Alice outside-in schema. The runnable command selects the
Gadugi directory with `-d qa/outside-in/alice-desktop/gadugi`.

## Scenario contract

The scenario YAML uses the CLI schema accepted by `gadugi-test validate`.

| Requirement | Contract |
| --- | --- |
| Identity | The scenario is runnable as `window-menu-registration-evidence`. |
| Location | The file lives at `qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml`, outside the custom Alice scenario catalog. |
| Interface | The scenario is a CLI evidence check, not a browser, desktop, rendering, Save, assessment, or lesson-completion test. |
| Delegated catalog validation | The first step runs `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`. |
| Delegated evidence preparation | The second step runs `qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke --prepare-only --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration`. |
| Delegated shell tests | The third step runs `qa/outside-in/alice-desktop/tests/run-tests.sh`. |
| Verification | The contract test confirms path, name, CLI agent metadata, tags, literal delegated commands, conservative description text, non-claim strings, and empty assertions. |
| Test dependencies | The shell contract test uses PyYAML (`yaml.safe_load`) via embedded Python, following the same dependency used by existing gadugi contract tests. |
| Evidence location | Generated outside-in runner evidence is written under `qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration`. Evidence output is generated runtime data and remains uncommitted. |

The scenario text uses conservative evidence-contract wording such as "Window
menu model registration characterization contract" and "existing QA entry points
only". It must not describe the scenario as rendering validation, pixel
validation, desktop window validation, JavaFX scene-graph validation,
assessment, or full lesson completion coverage.

## Validation commands

Use this command set when reviewing or changing the Gadugi Window menu
registration evidence lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s window-menu-registration-evidence \
  --timeout 300000

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/run-tests.sh
```

If a review explicitly requires the real current gated Maven evidence command,
initialize the Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Then run the focused menu/action contract test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

## Examples

### Review the scenario without executing the gated smoke

Use this path for normal PR readiness checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s window-menu-registration-evidence \
  --timeout 300000
```

The accepted result is a successful Gadugi run that delegates to the outside-in
runner and prepares menu-action smoke evidence. Review the generated runner
status as evidence that the Window menu registration evidence contract is wired
into the QA lane.

### Execute the underlying Alice gated smoke intentionally

Use this path only in a worktree prepared for the heavier Maven evidence
command:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration
```

This produces `command.log` and a pass/fail `status.txt` through the existing
outside-in runner. Treat the result as bounded Window menu model registration
and membership lookup evidence, not as visible rendering, live menu interaction,
Save completion, assessment, full GUI workflow, or full lesson completion
evidence.

### Run the contract test independently

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh
```

The test validates six structural contract assertions:

1. Scenario location is under `gadugi/`, not `scenarios/`.
2. Scenario name matches `window-menu-registration-evidence`.
3. Description contains required scope and non-claim strings.
4. Metadata tags include `cli`, `gadugi`, `pr-401`, `menu-action`, and
   `window-menu-registration-evidence`.
5. Steps delegate to the three existing QA entry points with exact commands.
6. Assertions list is empty (no Gadugi overclaims).

## Evidence boundaries

Accepted Gadugi Window menu registration evidence proves:

- The Gadugi CLI scenario is valid and runnable.
- The scenario delegates to the repo-owned Alice outside-in runner.
- The Alice runner can prepare the current menu-action smoke evidence contract.
- The evidence wording stays within Window menu model registration and
  membership lookup boundaries.

It does not prove:

- Visible rendering or rendered pixels.
- Visible desktop windows.
- JavaFX scene-graph layout.
- Live Swing menu opening or click behavior.
- Save completion.
- First-lesson completion.
- World execution or playback.
- Deployed installer success.
- Grading or learner assessment.
- Sims validation.
- Broad Tweedle or player archive decode behavior.

Display-backed, save/load, grading, creative assessment, and lesson-completion
evidence belongs in separate outside-in Alice desktop scenarios with their own
evidence artifacts.

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gadugi-test` is not found | Gadugi tooling is not installed or not on `PATH`. | Install the Gadugi CLI tooling used by the review environment before running the scenario commands. |
| `gadugi-test validate` rejects `qa/outside-in/alice-desktop/scenarios/menu-action-smoke.yaml` | That file uses the Alice custom outside-in schema, not the Gadugi schema. | Validate `qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml` with Gadugi and keep the custom scenario under `scenarios/`. |
| `gadugi-test run` cannot find `window-menu-registration-evidence` | The run command is not pointed at the Gadugi directory or the scenario name differs from the contract. | Use `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s window-menu-registration-evidence --timeout 300000` from the repository root. |
| The outside-in runner reports `gated-not-run` | The prepare-only runner path prepared a gated smoke without executing the heavy command. | This is expected for prepare-only evidence. Set `ALICE_QA_RUN_GATED_SMOKES=1` only when intentionally running the current gated smoke without `--prepare-only`. |
| Maven validation reports missing Tweedle parser grammar files | The Tweedle grammar submodule is not initialized. | Run `git submodule update --init tweedle-lang` and confirm `test -d tweedle-lang/Grammar`. |
| Contract test fails with `yaml.safe_load` error | The scenario YAML is malformed or contains unsupported YAML features. | Check for tabs, anchors, aliases, or flow-style collections; the scenario must use simple mappings and scalar values only. |
| A review comment says the scenario proves rendering, Save, or full lesson completion | The scenario wording is too broad. | Reword it to "Window menu model registration characterization" and "existing QA entry points" and keep broader claims out of the Gadugi scenario. |
