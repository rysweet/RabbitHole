# Gadugi select-project tab-click evidence scenario

This reference documents the Gadugi-compatible CLI scenario for Select Project
tab-click evidence checks added by PR #437. The scenario gives Gadugi tooling a
repo-owned entry point for the target-specific Select Project tab-click QA lane
without changing the custom Alice scenario schema or expanding the AT-SPI
automation scope.

Implementation files:

- `qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml`
- `qa/outside-in/alice-desktop/tests/test-gadugi-select-project-tab-click-contract.sh`

The scenario validates Select Project tab-click evidence wiring by delegating to
the Alice outside-in runner. The default Gadugi path remains prepare-only; the
underlying checked-in Alice scenario is the bounded target-specific Select
Project tab-click smoke targeting the committed Africa Full starter. The Gadugi
lane does not prove visible rendering, AT-SPI target opening, full project
interaction, grading, creative assessment, Save behavior, first-lesson
completion, or the full Alice desktop workflow.

## Contents

- [Scope](#scope)
- [Files](#files)
- [Command reference](#command-reference)
- [Configuration](#configuration)
- [Scenario contract](#scenario-contract)
- [Validation commands](#validation-commands)
- [Review checklists](#review-checklists)
- [Evidence boundaries](#evidence-boundaries)
- [Failure modes reference](#failure-modes-reference)

## Scope

The Gadugi scenario covers the Select Project tab-click evidence contract for
the Alice desktop outside-in QA lane. It is intentionally narrower than a full
desktop, rendering, or lesson workflow:

1. It validates that Gadugi can discover, validate, and execute
   `select-project-tab-click-evidence`.
2. It delegates to the existing Alice outside-in QA runners instead of adding a
   second Select Project validation path.
3. It uses the outside-in runner's prepare-only mode for the current
   `select-project-tab-click-smoke` workflow so the default path proves evidence
   wiring without requiring the gated xvfb-real-alice execution.
4. When the underlying scenario is executed intentionally, it runs the
   `alice-desktop-select-project-tab-click-exec` scenario with AT-SPI tab-click
   probing against the committed Africa Full starter. That proof remains bounded
   to target-specific AT-SPI tab enumeration and selection evidence and must not
   claim visible rendering, full project interaction, grading, creative
   assessment, Save behavior, or lesson completion.

For the target-specific Select Project contract itself, see
[Select Project Africa Full AT-SPI evidence reference](./select-project-africa-full-atspi-evidence.md).
For the custom Alice outside-in scenario schema, see
[Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md).

## Files

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/gadugi/` | Gadugi-compatible QA scenario directory. This directory is separate from the custom Alice scenario catalog. |
| `qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml` | Gadugi CLI scenario that verifies Select Project tab-click evidence wiring through the Alice outside-in runner. |
| `qa/outside-in/alice-desktop/tests/test-gadugi-select-project-tab-click-contract.sh` | Dependency-free shell contract test that checks the scenario path, identity, metadata, delegated commands, conservative scope wording, and absence of overclaiming using shell and the Python standard library only. |
| `qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml` | Custom-schema Alice scenario consumed by the repo-owned outside-in runner. This file remains on the Alice custom schema and is not a Gadugi scenario. |

The `gadugi/` directory exists because `gadugi-test validate` uses a different
schema than the Alice custom outside-in `scenarios/` directory. Keeping the
formats separate lets both validators stay strict.

## Command reference

Run commands from the repository root.

Prerequisite: `gadugi-test` is installed and available on `PATH`.

Validate the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml
```

Run the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s select-project-tab-click-evidence \
  --timeout 300000
```

The scenario delegates to these existing Alice desktop outside-in QA entry
points:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-select-project-tab-click

qa/outside-in/alice-desktop/tests/run-tests.sh
```

`--prepare-only` is the default evidence-contract lane for Gadugi execution. It
creates reviewable outside-in runner evidence for the current Select Project
tab-click smoke and returns success without enabling the full xvfb-real-alice
execution. Use the full runner only when a review explicitly requires live
AT-SPI evidence collection.

## Configuration

The Gadugi scenario is intentionally small and repo-local.

| Field | Value | Contract |
| --- | --- | --- |
| Scenario directory | `qa/outside-in/alice-desktop/gadugi` | Gadugi discovers the dedicated Gadugi scenario files. |
| Scenario file | `qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml` | `gadugi-test validate -f` validates the runnable scenario. |
| Working directory | `.` | Scenario commands run from the repository root. |
| Interface | `cli` | The scenario is a command-line evidence check, not a browser, desktop, or rendering test. |
| Scenario name | `select-project-tab-click-evidence` | Stable name used with `gadugi-test run -s select-project-tab-click-evidence`. |
| Timeout | `180000` in the scenario, `300000` for readiness wrapper runs | The YAML keeps individual command timeouts short; wrapper runs leave enough time for the delegated QA scripts. |
| Tags | `cli`, `gadugi`, `pr-437`, `select-project-tab-click`, `tab-click-evidence-contract` | `pr-437` identifies the PR that introduced the Select Project tab-click lane. |

The surrounding QA environment may set:

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Keeps Node-based Gadugi orchestration within the preferred memory limit. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` | Enables isolated license-dialog bypass when the underlying Alice runner is invoked without `--prepare-only`. The default Gadugi command remains prepare-only. |

Do not point Gadugi at `qa/outside-in/alice-desktop/scenarios/`; that directory
uses the custom Alice outside-in schema. The runnable command selects the
Gadugi directory with `-d qa/outside-in/alice-desktop/gadugi`.

## Scenario contract

The scenario YAML uses the CLI schema accepted by `gadugi-test validate`.

| Requirement | Contract |
| --- | --- |
| Identity | The scenario is runnable as `select-project-tab-click-evidence`. |
| Location | The file lives at `qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml`, outside the custom Alice scenario catalog. |
| Interface | The scenario is a CLI evidence check, not a browser, desktop automation, Alice-world rendering correctness, Save, grading, or lesson-completion test. |
| Delegated catalog validation | The first step runs `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`. |
| Delegated evidence preparation | The second step runs `qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-select-project-tab-click-exec --prepare-only --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-select-project-tab-click`. |
| Delegated shell tests | The third step runs `qa/outside-in/alice-desktop/tests/run-tests.sh`. |
| Verification | The contract test confirms path, name, CLI agent metadata, tags, literal delegated commands, conservative description text, and absence of overclaiming. |
| Test dependencies | The shell contract test uses shell and Python standard library checks only; it must not require PyYAML or other non-repo dependencies. |
| Evidence location | Generated outside-in runner evidence is written under `qa/outside-in/alice-desktop/evidence/gadugi-select-project-tab-click`. Evidence output is generated runtime data and remains uncommitted. |

The scenario text uses conservative evidence-contract wording such as "Select
Project tab-click evidence workflow", "AT-SPI tab enumeration and selection
evidence contract", and "tab-click evidence contract". It must not describe the
scenario as Alice-world rendering validation, AT-SPI target opening, full
project interaction, Save validation, grading validation, creative assessment,
or full lesson-completion coverage.

## Validation commands

Use this command set when reviewing or changing the Gadugi Select Project
tab-click evidence lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s select-project-tab-click-evidence \
  --timeout 300000

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/run-tests.sh
```

If a review explicitly requires live AT-SPI evidence collection, initialize the
Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Then run the Select Project tab-click scenario with the full runner:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Passing evidence is limited to target-specific AT-SPI tab enumeration and
selection for the committed Africa Full starter. It is not visible rendering
validation, full project interaction, grading, creative assessment, Save
behavior, or lesson completion evidence.

## Review checklists

### Review the scenario without executing the live AT-SPI run

Use this path for normal PR readiness checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s select-project-tab-click-evidence \
  --timeout 300000
```

The accepted result is a successful Gadugi run that delegates to the outside-in
runner and prepares Select Project tab-click smoke evidence. Review the
generated runner status as evidence that the Select Project tab-click evidence
contract is wired into the QA lane.

### Execute the underlying Alice AT-SPI scenario intentionally

Use this path only in a worktree prepared for the full xvfb-real-alice
execution with AT-SPI access:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-select-project-tab-click
```

This produces `tab-click-observation.json` and a pass/fail `status.txt`
through the existing outside-in runner. Treat the result as bounded
target-specific AT-SPI selection evidence, not as visible rendering, full
project interaction, Save, grading, creative assessment, or lesson completion
evidence.

## Evidence boundaries

Accepted Gadugi Select Project tab-click evidence proves:

- The Gadugi CLI scenario is valid and runnable.
- The scenario delegates to the repo-owned Alice outside-in runner.
- The Alice runner can prepare the current Select Project tab-click smoke
  evidence contract.
- The evidence wording stays within target-specific AT-SPI tab enumeration,
  tab-click activation, and selection boundaries.

It does not prove:

- Visible rendering or rendered pixels.
- AT-SPI target opening beyond selection evidence.
- Full project interaction.
- Grading.
- Creative assessment.
- Save behavior.
- First-lesson completion.
- Full Alice desktop workflow.

Display-backed world rendering, Save/load, grading, creative assessment, and
lesson-completion evidence belongs in separate outside-in Alice desktop
scenarios with their own evidence artifacts.

## Failure modes reference

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gadugi-test` is not found | Gadugi tooling is not installed or not on `PATH`. | Install the Gadugi CLI tooling used by the review environment before running the scenario commands. |
| `gadugi-test validate` rejects `qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml` | That file uses the Alice custom outside-in schema, not the Gadugi schema. | Validate `qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml` with Gadugi and keep the custom scenario under `scenarios/`. |
| `gadugi-test run` cannot find `select-project-tab-click-evidence` | The run command is not pointed at the Gadugi directory or the scenario name differs from the contract. | Use `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s select-project-tab-click-evidence --timeout 300000` from the repository root. |
| The outside-in runner reports `gated-not-run` | The prepare-only runner path prepared a gated smoke without executing the full AT-SPI scenario. | This is expected for prepare-only evidence. Remove `--prepare-only` and set `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` only when intentionally running the full scenario. |
| Maven validation reports missing Tweedle parser grammar files | The Tweedle grammar submodule is not initialized. | Run `git submodule update --init tweedle-lang` and confirm `test -d tweedle-lang/Grammar`. |
| A review comment says the scenario proves visible rendering, full project interaction, grading, creative assessment, Save behavior, or full lesson completion | The scenario wording is too broad. | Reword it to "Select Project tab-click evidence wiring", "AT-SPI tab enumeration and selection evidence", or "tab-click evidence contract" and keep those broader claims out of the Gadugi scenario. |
