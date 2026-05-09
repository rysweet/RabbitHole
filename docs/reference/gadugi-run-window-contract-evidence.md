# Gadugi run-window contract evidence scenario

This reference documents the Gadugi-compatible CLI scenario for Run-window
creation/wiring contract evidence checks. The scenario gives Gadugi tooling a
repo-owned entry point for the Alice desktop outside-in QA lane without changing
the custom Alice scenario schema.

Implementation files:

- `qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml`
- `qa/outside-in/alice-desktop/tests/test-gadugi-run-window-contract.sh`

The scenario validates Run-window creation/wiring evidence by delegating to the
Alice outside-in runner. The default Gadugi path remains prepare-only; the
underlying checked-in Alice scenario is the bounded `gated-command-smoke` for
`EatmeRunWindowEvidenceTest`. The Gadugi lane does not prove active rendering,
run execution, world execution correctness, rendering correctness, Save behavior,
grading, creative assessment, lesson completion, or full UI automation.

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

The Gadugi scenario covers the Run-window creation/wiring evidence contract for
the Alice desktop outside-in QA lane. It is intentionally narrower than a full
desktop or lesson workflow:

1. It validates that Gadugi can discover, validate, and execute
   `run-window-contract-evidence`.
2. It delegates to the existing Alice outside-in QA runners instead of adding a
   second Run-window validation path.
3. It uses the outside-in runner's prepare-only mode for the current
   `alice-desktop-run-window-contract` scenario so the default path proves
   evidence wiring without requiring the gated Maven smoke.
4. When the underlying gated smoke is executed intentionally, it runs
   `EatmeRunWindowEvidenceTest`. That proof remains bounded to Run-window
   creation/wiring metadata and must not claim active rendering, run execution,
   world execution correctness, rendering correctness, Save behavior, grading,
   creative assessment, lesson completion, or full UI automation.

For the Run-window artifact contract itself, see
[Run-Window Creation/Wiring Contract](./run-window-creation-wiring-contract.md).
For the custom Alice outside-in scenario schema, see
[Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md).

## Files

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/gadugi/` | Gadugi-compatible QA scenario directory. This directory is separate from the custom Alice scenario catalog. |
| `qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml` | Gadugi CLI scenario that verifies Run-window creation/wiring evidence through the Alice outside-in runner. |
| `qa/outside-in/alice-desktop/tests/test-gadugi-run-window-contract.sh` | Shell contract test that checks the scenario path, identity, metadata, delegated commands, conservative scope wording, non-claim boundaries, and absence of stale config references using shell and Python with PyYAML. |
| `qa/outside-in/alice-desktop/scenarios/run-window-contract.yaml` | Custom-schema Alice scenario consumed by the repo-owned outside-in runner. This file remains on the Alice custom schema and is not a Gadugi scenario. |

The `gadugi/` directory exists because `gadugi-test validate` uses a different
schema than the Alice custom outside-in `scenarios/` directory. Keeping the
formats separate lets both validators stay strict.

## Usage

Run commands from the repository root.

Prerequisite: `gadugi-test` is installed and available on `PATH`.

Validate the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml
```

Run the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s run-window-contract-evidence \
  --timeout 300000
```

The scenario delegates to these existing Alice desktop outside-in QA entry
points:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-run-window-contract

qa/outside-in/alice-desktop/tests/run-tests.sh
```

`--prepare-only` is the default evidence-contract lane for Gadugi execution. It
creates reviewable outside-in runner evidence for the current Run-window contract
scenario and returns success without enabling the gated Maven smoke.
`ALICE_QA_RUN_GATED_SMOKES=1` only matters when running the underlying Alice
runner without `--prepare-only`. Use the gated runner only when a review
explicitly requires command execution evidence.

## Configuration

The Gadugi scenario is intentionally small and repo-local.

| Field | Value | Contract |
| --- | --- | --- |
| Scenario directory | `qa/outside-in/alice-desktop/gadugi` | Gadugi discovers the dedicated Gadugi scenario files. |
| Scenario file | `qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml` | `gadugi-test validate -f` validates the runnable scenario. |
| Working directory | `.` | Scenario commands run from the repository root. |
| Interface | `cli` | The scenario is a command-line evidence check, not a browser, desktop, or rendering test. |
| Scenario name | `run-window-contract-evidence` | Stable name used with `gadugi-test run -s run-window-contract-evidence`. |
| Timeout | `180000` in the scenario, `300000` for the PR readiness wrapper run | The YAML keeps individual command timeouts short; the wrapper run leaves enough time for the delegated QA scripts. |
| Tags | `cli`, `gadugi`, `pr-434`, `run-window-contract`, `run-window-evidence` | Tags make the scenario discoverable as a PR #434 Run-window creation/wiring evidence contract check. |

The surrounding QA environment may set:

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Keeps Node-based Gadugi orchestration within the preferred memory limit. |
| `ALICE_QA_RUN_GATED_SMOKES=1` | Enables the underlying Alice gated command smoke only when intentionally running `run-scenario.sh` without `--prepare-only`. That smoke targets `EatmeRunWindowEvidenceTest`; the default Gadugi command remains prepare-only. |

Do not point Gadugi at `qa/outside-in/alice-desktop/scenarios/`; that directory
uses the custom Alice outside-in schema. The runnable command selects the
Gadugi directory with `-d qa/outside-in/alice-desktop/gadugi`.

## Scenario contract

The scenario YAML uses the CLI schema accepted by `gadugi-test validate`.

| Requirement | Contract |
| --- | --- |
| Identity | The scenario is runnable as `run-window-contract-evidence`. |
| Location | The file lives at `qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml`, outside the custom Alice scenario catalog. |
| Interface | The scenario is a CLI evidence check, not a browser, desktop, rendering, save, grading, or lesson-completion test. |
| Delegated catalog validation | The first step runs `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`. |
| Delegated evidence preparation | The second step runs `qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-run-window-contract --prepare-only --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-run-window-contract`. |
| Delegated shell tests | The third step runs `qa/outside-in/alice-desktop/tests/run-tests.sh`. |
| Verification | The contract test confirms path, name, CLI agent metadata, tags, literal delegated commands, conservative description text, non-claim boundaries, and absence of nonexistent config references. |
| Test dependencies | The shell contract test uses shell and Python with PyYAML for YAML parsing, matching the `test-gadugi-compatibility-contract.sh` precedent. |
| Evidence location | Generated outside-in runner evidence is written under `qa/outside-in/alice-desktop/evidence/gadugi-run-window-contract`. Evidence output is generated runtime data and remains uncommitted. |

The scenario text uses conservative evidence-contract wording such as
"Run-window creation/wiring evidence", "creation/wiring contract", and
"bounded gated-command-smoke". It must not describe the scenario as rendering
validation, run-execution validation, save validation, grading validation,
creative assessment, or full lesson completion coverage.

The description must explicitly state non-claims for active rendering, run
execution, world execution correctness, rendering correctness, Save behavior,
grading, creative assessment, lesson completion, and full UI automation.

## Validation commands

Use this command set when reviewing or changing the Gadugi Run-window contract
evidence lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s run-window-contract-evidence \
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

Then run the focused Run-window creation/wiring seam:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest \
  test
```

## Examples

### Review the scenario without executing the gated smoke

Use this path for normal PR readiness checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s run-window-contract-evidence \
  --timeout 300000
```

The accepted result is a successful Gadugi run that delegates to the outside-in
runner and prepares Run-window contract evidence. Review the generated runner
status as evidence that the Run-window creation/wiring evidence contract is
wired into the QA lane.

### Execute the underlying Alice gated smoke intentionally

Use this path only in a worktree prepared for the heavier Maven evidence
command:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-run-window-contract
```

This produces `command.log` and a pass/fail `status.txt` through the existing
outside-in runner. Treat the result as bounded Run-window creation/wiring
evidence with explicit non-claim fields, not as active rendering, run execution,
world execution correctness, rendering correctness, Save behavior, grading,
creative assessment, lesson completion, or full UI automation evidence.

### Run only the contract test

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/tests/test-gadugi-run-window-contract.sh
```

The contract test validates the Gadugi YAML structure, non-claim wording,
metadata tags, delegated commands, and absence of overclaiming without requiring
`gadugi-test` or Maven.

## Evidence boundaries

Accepted Gadugi Run-window contract evidence proves:

- The Gadugi CLI scenario is valid and runnable.
- The scenario delegates to the repo-owned Alice outside-in runner.
- The Alice runner can prepare the current Run-window contract evidence.
- The evidence wording stays within Run-window creation/wiring boundaries.
- The artifact carries explicit non-claim fields for all out-of-scope
  capabilities.

It does not prove:

- Active rendering.
- Run execution or program behavior.
- World execution correctness.
- Visible rendering correctness or rendered pixels.
- Save behavior.
- Grading.
- Creative assessment.
- Lesson completion.
- Full UI automation.
- A complete instructor or student desktop workflow.

Display-backed, save/load, grading, creative assessment, and lesson-completion
evidence belongs in separate outside-in Alice desktop scenarios with their own
evidence artifacts.

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gadugi-test` is not found | Gadugi tooling is not installed or not on `PATH`. | Install the Gadugi CLI tooling used by the review environment before running the scenario commands. |
| `gadugi-test validate` rejects `qa/outside-in/alice-desktop/scenarios/run-window-contract.yaml` | That file uses the Alice custom outside-in schema, not the Gadugi schema. | Validate `qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml` with Gadugi and keep the custom scenario under `scenarios/`. |
| `gadugi-test run` cannot find `run-window-contract-evidence` | The run command is not pointed at the Gadugi directory or the scenario name differs from the contract. | Use `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s run-window-contract-evidence --timeout 300000` from the repository root. |
| The outside-in runner reports `gated-not-run` | The prepare-only runner path prepared a gated smoke without executing the heavy command. | This is expected for prepare-only evidence. Set `ALICE_QA_RUN_GATED_SMOKES=1` only when intentionally running the current gated smoke without `--prepare-only`. |
| Maven validation reports missing Tweedle parser grammar files | The Tweedle grammar submodule is not initialized. | Run `git submodule update --init tweedle-lang` and confirm `test -d tweedle-lang/Grammar`. |
| A review comment says the scenario proves rendering, save behavior, grading, creative assessment, or full lesson completion | The scenario wording is too broad. | Reword it to "Run-window creation/wiring evidence", "creation/wiring contract", or "bounded gated-command-smoke" and keep those broader claims out of the Gadugi scenario. |
| The contract test fails on missing non-claim wording | The scenario description was edited to remove explicit non-claim boundaries. | Restore the `does not validate` clause listing active rendering, run execution, world execution correctness, rendering correctness, Save, grading, creative assessment, lesson completion, and full UI automation. |
