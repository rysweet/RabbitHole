# Gadugi archive/player boundary evidence scenario

This reference documents the Gadugi-compatible CLI scenario for archive/player
boundary evidence checks. The scenario gives Gadugi tooling a repo-owned entry
point for the archive/player boundary lane without changing the custom Alice
scenario schema.

Implementation files:

- `qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml`
- `qa/outside-in/alice-desktop/tests/test-gadugi-archive-player-boundary-contract.sh`

The scenario validates archive/player boundary evidence wiring by delegating to
the Alice outside-in runner. The default Gadugi path remains prepare-only; the
underlying checked-in Alice scenario is the bounded archive fixture
characterization smoke. The Gadugi lane does not prove visible rendering,
installer behavior, save behavior, grading, creative assessment, or full lesson
completion.

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

The Gadugi scenario covers the archive/player boundary evidence contract for the
Alice desktop outside-in QA lane. It is intentionally narrower than a full
desktop or lesson workflow:

1. It validates that Gadugi can discover, validate, and execute
   `archive-player-boundary-evidence`.
2. It delegates to the existing Alice outside-in QA runners instead of adding a
   second archive-characterization validation path.
3. It uses the outside-in runner's prepare-only mode for the current
   archive-fixture smoke so the default path proves evidence wiring without
   requiring the gated Maven smoke.
4. When the underlying gated smoke is executed intentionally, it runs
   `HistoricalArchiveRoundTripCharacterizationTest`. That proof remains bounded
   to archive IO characterization and must not claim rendered pixels, visible
   windows, installer behavior, save behavior, grading, creative assessment, or
   lesson completion.

For the archive/player boundary feature itself, see
[Archive/Player Boundary](./archive-player-boundary.md).
For the custom Alice outside-in scenario schema, see
[Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md).
For the exported launcher Gadugi evidence scenario, see
[Gadugi exported launcher evidence scenario](./gadugi-exported-launcher-evidence.md).

## Files

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/gadugi/` | Gadugi-compatible QA scenario directory. This directory is separate from the custom Alice scenario catalog. |
| `qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml` | Gadugi CLI scenario that verifies archive/player boundary evidence wiring through the Alice outside-in runner. |
| `qa/outside-in/alice-desktop/tests/test-gadugi-archive-player-boundary-contract.sh` | Dependency-free shell contract test that checks the scenario path, identity, metadata, delegated commands, conservative scope wording, and absence of stale config references using shell and the Python standard library only. |
| `qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml` | Custom-schema Alice scenario consumed by the repo-owned outside-in runner. This file remains on the Alice custom schema and is not a Gadugi scenario. |

The `gadugi/` directory exists because `gadugi-test validate` uses a different
schema than the Alice custom outside-in `scenarios/` directory. Keeping the
formats separate lets both validators stay strict.

## Usage

Run commands from the repository root.

Prerequisite: `gadugi-test` is installed and available on `PATH`.

Validate the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml
```

Run the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s archive-player-boundary-evidence \
  --timeout 300000
```

The scenario delegates to these existing Alice desktop outside-in QA entry
points:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-archive-fixture-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-archive-player-boundary

qa/outside-in/alice-desktop/tests/run-tests.sh
```

`--prepare-only` is the default evidence-contract lane for Gadugi execution. It
creates reviewable outside-in runner evidence for the current archive-fixture
smoke and returns success without enabling the gated Maven smoke.
`ALICE_QA_RUN_GATED_SMOKES=1` only matters when running the underlying Alice
runner without `--prepare-only`. Use the gated runner only when a review
explicitly requires command execution evidence.

## Configuration

The Gadugi scenario is intentionally small and repo-local.

| Field | Value | Contract |
| --- | --- | --- |
| Scenario directory | `qa/outside-in/alice-desktop/gadugi` | Gadugi discovers the dedicated Gadugi scenario files. |
| Scenario file | `qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml` | `gadugi-test validate -f` validates the runnable scenario. |
| Working directory | `.` | Scenario commands run from the repository root. |
| Interface | `cli` | The scenario is a command-line evidence check, not a browser, desktop, or rendering test. |
| Scenario name | `archive-player-boundary-evidence` | Stable name used with `gadugi-test run -s archive-player-boundary-evidence`. |
| Timeout | `180000` in the scenario, `300000` for the PR readiness wrapper run | The YAML keeps individual command timeouts short; the wrapper run leaves enough time for the delegated QA scripts. |
| Tags | `cli`, `gadugi`, `pr-438`, `archive-player-boundary`, `archive-boundary-evidence-contract` | Tags make the scenario discoverable as a PR #438 archive/player boundary evidence contract check. |

The surrounding QA environment may set:

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Keeps Node-based Gadugi orchestration within the preferred memory limit. |
| `ALICE_QA_RUN_GATED_SMOKES=1` | Enables the underlying Alice gated command smoke only when intentionally running `run-scenario.sh` without `--prepare-only`. That smoke targets `HistoricalArchiveRoundTripCharacterizationTest`; the default Gadugi command remains prepare-only. |

Do not point Gadugi at `qa/outside-in/alice-desktop/scenarios/`; that directory
uses the custom Alice outside-in schema. The runnable command selects the
Gadugi directory with `-d qa/outside-in/alice-desktop/gadugi`.

## Scenario contract

The scenario YAML uses the CLI schema accepted by `gadugi-test validate`.

| Requirement | Contract |
| --- | --- |
| Identity | The scenario is runnable as `archive-player-boundary-evidence`. |
| Location | The file lives at `qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml`, outside the custom Alice scenario catalog. |
| Interface | The scenario is a CLI evidence check, not a browser, desktop, rendering, save, grading, or lesson-completion test. |
| Delegated catalog validation | The first step runs `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`. |
| Delegated evidence preparation | The second step runs `qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-archive-fixture-smoke --prepare-only --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-archive-player-boundary`. |
| Delegated shell tests | The third step runs `qa/outside-in/alice-desktop/tests/run-tests.sh`. |
| Verification | The contract test confirms path, name, CLI agent metadata, tags, literal delegated commands, conservative description text, and absence of stale config references. |
| Test dependencies | The shell contract test uses shell and Python standard library checks only; it must not require PyYAML or other non-repo dependencies. |
| Evidence location | Generated outside-in runner evidence is written under `qa/outside-in/alice-desktop/evidence/gadugi-archive-player-boundary`. Evidence output is generated runtime data and remains uncommitted. |

The scenario text uses conservative evidence-contract wording such as
"archive/player boundary fail-closed wiring" and "existing QA entry points
only". It must not describe the scenario as rendering validation, save
validation, grading validation, creative assessment, or full lesson completion
coverage.

## Validation commands

Use this command set when reviewing or changing the Gadugi archive/player
boundary evidence lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s archive-player-boundary-evidence \
  --timeout 300000

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/run-tests.sh
```

Run the dependency-free contract test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 bash \
  qa/outside-in/alice-desktop/tests/test-gadugi-archive-player-boundary-contract.sh
```

If a review explicitly requires the real current gated Maven evidence command,
initialize the Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Then run the focused archive characterization suite:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

## Examples

### Review the scenario without executing the gated smoke

Use this path for normal PR readiness checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s archive-player-boundary-evidence \
  --timeout 300000
```

The accepted result is a successful Gadugi run that delegates to the outside-in
runner and prepares archive-fixture smoke evidence. Review the generated runner
status as evidence that the archive/player boundary evidence contract is wired
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
  alice-desktop-archive-fixture-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-archive-player-boundary
```

This produces `command.log` and a pass/fail `status.txt` through the existing
outside-in runner. Treat the result as bounded archive IO characterization
evidence, not as visible rendering, installer, save, grading, creative
assessment, full GUI export journey, or full lesson completion evidence.

### Validate the contract test in isolation

Run the shell contract test without Gadugi:

```bash
bash qa/outside-in/alice-desktop/tests/test-gadugi-archive-player-boundary-contract.sh
```

The contract test checks:
- scenario file existence and path
- scenario name matches `archive-player-boundary-evidence`
- CLI agent type and metadata tags include `pr-438` and `archive-player-boundary`
- delegated commands match the expected outside-in QA entry points
- description text stays within conservative evidence-contract wording
- no references to nonexistent configuration files

## Evidence boundaries

Accepted Gadugi archive/player boundary evidence proves:

- The Gadugi CLI scenario is valid and runnable.
- The scenario delegates to the repo-owned Alice outside-in runner.
- The Alice runner can prepare the current archive-fixture smoke evidence
  contract.
- The evidence wording stays within archive/player boundary evidence
  boundaries.
- The dependency-free contract test holds.

It does not prove:

- Visible rendering or rendered pixels.
- Installer validation.
- Save behavior.
- Grading.
- Creative assessment.
- Full lesson completion.
- A complete instructor or student desktop workflow.
- Full Tweedle language support.
- Broad legacy archive recovery beyond the exact image-resource compatibility
  shape.

Display-backed, save/load, grading, creative assessment, and lesson-completion
evidence belongs in separate outside-in Alice desktop scenarios with their own
evidence artifacts.

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gadugi-test` is not found | Gadugi tooling is not installed or not on `PATH`. | Install the Gadugi CLI tooling used by the review environment before running the scenario commands. |
| `gadugi-test validate` rejects `qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml` | That file uses the Alice custom outside-in schema, not the Gadugi schema. | Validate `qa/outside-in/alice-desktop/gadugi/archive-player-boundary-evidence.yaml` with Gadugi and keep the custom scenario under `scenarios/`. |
| `gadugi-test run` cannot find `archive-player-boundary-evidence` | The run command is not pointed at the Gadugi directory or the scenario name differs from the contract. | Use `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s archive-player-boundary-evidence --timeout 300000` from the repository root. |
| The outside-in runner reports `gated-not-run` | The prepare-only runner path prepared a gated smoke without executing the heavy command. | This is expected for prepare-only evidence. Set `ALICE_QA_RUN_GATED_SMOKES=1` only when intentionally running the current gated smoke without `--prepare-only`. |
| Maven validation reports missing Tweedle parser grammar files | The Tweedle grammar submodule is not initialized. | Run `git submodule update --init tweedle-lang` and confirm `test -d tweedle-lang/Grammar`. |
| Contract test fails on missing `pr-438` tag | The scenario YAML was edited and lost its PR tag. | Restore the `pr-438` tag in the `metadata.tags` list of `archive-player-boundary-evidence.yaml`. |
| A review comment says the scenario proves rendering, save behavior, grading, creative assessment, or full lesson completion | The scenario wording is too broad. | Reword it to "archive/player boundary evidence wiring" and keep those broader claims out of the Gadugi scenario. |
