# Gadugi exported launcher evidence scenario

This reference documents the Gadugi-compatible CLI scenario for exported
launcher evidence checks. The scenario gives Gadugi tooling a repo-owned entry
point for the Alice desktop outside-in QA lane without changing the custom Alice
scenario schema or expanding the source-code-generator characterization scope.

Implementation files:

- `qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml`
- `qa/outside-in/alice-desktop/tests/test-gadugi-exported-launcher-contract.sh`

The scenario validates launcher evidence wiring and JavaFX handoff,
launcher-owned marker-observation, and no-go checks only. It does not prove
visible rendering correctness, Alice-world rendered pixels, Save completion,
grading, creative assessment, full first-lesson completion, or the full Alice
desktop workflow.

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

The Gadugi scenario covers the exported launcher evidence contract for the Alice
desktop outside-in QA lane. It is intentionally narrower than a full desktop or
lesson workflow:

1. It validates that Gadugi can discover, validate, and execute
   `exported-launcher-evidence`.
2. It delegates to the existing Alice outside-in QA runners instead of adding a
   second project-export validation path.
3. It uses the outside-in runner's prepare-only mode for the exported-project
   smoke so the default path proves evidence wiring without requiring the gated
   Maven smoke.
4. It records launcher evidence, JavaFX handoff, launcher marker-observation
   boundaries, and deterministic no-go boundaries without claiming Alice-world
   rendered pixels, visible-window correctness, Save completion, grading,
   creative assessment, or lesson completion.

For generated launcher behavior itself, see
[Exported NetBeans Ant Project Behavior](./exported-netbeans-ant-project-behavior.md).
For the custom Alice outside-in scenario schema, see
[Alice desktop outside-in QA reference](./alice-desktop-outside-in-qa.md).

## Files

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/gadugi/` | Gadugi-compatible QA scenario directory. This directory is separate from the custom Alice scenario catalog. |
| `qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml` | Gadugi CLI scenario that verifies exported launcher evidence wiring through the Alice outside-in runner. |
| `qa/outside-in/alice-desktop/tests/test-gadugi-exported-launcher-contract.sh` | Dependency-free shell contract test that checks the scenario path, identity, metadata, delegated commands, conservative scope wording, and absence of stale config references using shell and the Python standard library only. |
| `qa/outside-in/alice-desktop/scenarios/exported-project-smoke.yaml` | Custom-schema Alice scenario consumed by the repo-owned outside-in runner. This file remains on the Alice custom schema and is not a Gadugi scenario. |

The `gadugi/` directory exists because `gadugi-test validate` uses a different
schema than the Alice custom outside-in `scenarios/` directory. Keeping the
formats separate lets both validators stay strict.

## Command reference

Run commands from the repository root.

Prerequisite: `gadugi-test` is installed and available on `PATH`.

Validate the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml
```

Run the Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s exported-launcher-evidence \
  --timeout 300000
```

The scenario delegates to these existing Alice desktop outside-in QA entry
points:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-exported-project-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher

qa/outside-in/alice-desktop/tests/run-tests.sh
```

`--prepare-only` is the default evidence-contract lane for Gadugi execution. It
creates reviewable outside-in runner evidence for the exported-project smoke and
returns success without enabling the gated Maven smoke.
`ALICE_QA_RUN_GATED_SMOKES=1` only matters when running the underlying Alice
runner without `--prepare-only`. Use the gated runner only when a review
explicitly requires launcher command evidence.

## Configuration

The Gadugi scenario is intentionally small and repo-local.

| Field | Value | Contract |
| --- | --- | --- |
| Scenario directory | `qa/outside-in/alice-desktop/gadugi` | Gadugi discovers the dedicated Gadugi scenario files. |
| Scenario file | `qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml` | `gadugi-test validate -f` validates the runnable scenario. |
| Working directory | `.` | Scenario commands run from the repository root. |
| Interface | `cli` | The scenario is a command-line evidence check, not a browser, desktop, or rendering test. |
| Scenario name | `exported-launcher-evidence` | Stable name used with `gadugi-test run -s exported-launcher-evidence`. |
| Timeout | `180000` in the scenario, `300000` for readiness wrapper runs | The YAML keeps individual command timeouts short; wrapper runs leave enough time for the delegated QA scripts. |
| Tags | `cli`, `gadugi`, `pr-155`, `exported-launcher`, `launcher-evidence-contract` | `pr-155` is the historical Gadugi scenario identity retained by the existing QA contract; the source-generation characterization can reference that inherited launcher-evidence lane without retargeting the scenario metadata. |

The `pr-155` tag and description text are historical scenario metadata, not a
claim about the current review or source-generation characterization scope. The
source-generation characterization uses this existing Gadugi lane as supporting
launcher-evidence wiring. Retargeting the scenario identity would require
coordinated changes to
`exported-launcher-evidence.yaml` and
`test-gadugi-exported-launcher-contract.sh`, which is outside this reference
update.

The surrounding QA environment may set:

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Keeps Node-based Gadugi orchestration within the preferred memory limit. |
| `ALICE_QA_RUN_GATED_SMOKES=1` | Enables the underlying Alice gated command smoke only when intentionally running `run-scenario.sh` without `--prepare-only`. The default Gadugi command remains prepare-only. |

Do not point Gadugi at `qa/outside-in/alice-desktop/scenarios/`; that directory
uses the custom Alice outside-in schema. The runnable command selects the
Gadugi directory with `-d qa/outside-in/alice-desktop/gadugi`.

## Scenario contract

The scenario YAML uses the CLI schema accepted by `gadugi-test validate`.

| Requirement | Contract |
| --- | --- |
| Identity | The scenario is runnable as `exported-launcher-evidence`. |
| Location | The file lives at `qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml`, outside the custom Alice scenario catalog. |
| Interface | The scenario is a CLI evidence check, not a browser, desktop automation, Alice-world rendering correctness, Save, grading, or lesson-completion test. |
| Delegated catalog validation | The first step runs `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`. |
| Delegated evidence preparation | The second step runs `qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-exported-project-smoke --prepare-only --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher`. |
| Delegated shell tests | The third step runs `qa/outside-in/alice-desktop/tests/run-tests.sh`. |
| Verification | The contract test confirms path, name, CLI agent metadata, tags, literal delegated commands, conservative description text, and absence of nonexistent config references. |
| Test dependencies | The shell contract test uses shell and Python standard library checks only; it must not require PyYAML or other non-repo dependencies. |
| Evidence location | Generated outside-in runner evidence is written under `qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher`. Evidence output is generated runtime data and remains uncommitted. |

The scenario text uses conservative evidence-contract wording such as "launcher
evidence workflow", "JavaFX handoff/no-go evidence contract", and "launcher
evidence contract". It may refer to the generated launcher's own
marker-observation boundary, but it must not describe the scenario as Alice-world
rendering validation, Save validation, grading validation, creative assessment,
or full lesson-completion coverage.

## Validation commands

Use this command set when reviewing or changing the Gadugi launcher evidence
lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s exported-launcher-evidence \
  --timeout 300000

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/run-tests.sh
```

If a review explicitly requires the real gated Maven evidence command,
initialize the Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Then run the focused exported-project launcher evidence test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest \
  test
```

## Review checklists

### Review the scenario without executing the gated smoke

Use this path for normal PR readiness checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s exported-launcher-evidence \
  --timeout 300000
```

The accepted result is a successful Gadugi run that delegates to the outside-in
runner and prepares exported-project smoke evidence. Review the generated runner
status as evidence that the exported launcher evidence contract is wired into
the QA lane.

### Execute the underlying Alice gated smoke intentionally

Use this path only in a worktree prepared for the heavier Maven evidence
command:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-exported-project-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher
```

This produces `command.log` and a pass/fail `status.txt` through the existing
outside-in runner. Treat the result as exported launcher evidence command output,
not as visible rendering correctness, Save completion, grading, creative
assessment, or full lesson-completion evidence.

## Evidence boundaries

Accepted Gadugi launcher evidence proves:

- The Gadugi CLI scenario is valid and runnable.
- The scenario delegates to the repo-owned Alice outside-in runner.
- The Alice runner can prepare the exported-project smoke evidence contract.
- The evidence wording stays within launcher handoff, marker-observation, and
  no-go boundaries.

It does not prove:

- Visible rendering correctness or Alice-world rendered pixels.
- Save completion.
- Grading.
- Creative assessment.
- Full first-lesson completion.
- A complete instructor or student desktop workflow.

Display-backed world rendering, Save/load, grading, creative assessment, and
lesson-completion evidence belongs in separate outside-in Alice desktop
scenarios with their own evidence artifacts.

## Failure modes reference

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gadugi-test` is not found | Gadugi tooling is not installed or not on `PATH`. | Install the Gadugi CLI tooling used by the review environment before running the scenario commands. |
| `gadugi-test validate` rejects `qa/outside-in/alice-desktop/scenarios/exported-project-smoke.yaml` | That file uses the Alice custom outside-in schema, not the Gadugi schema. | Validate `qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml` with Gadugi and keep the custom scenario under `scenarios/`. |
| `gadugi-test run` cannot find `exported-launcher-evidence` | The run command is not pointed at the Gadugi directory or the scenario name differs from the contract. | Use `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s exported-launcher-evidence --timeout 300000` from the repository root. |
| The outside-in runner reports `gated-not-run` | The prepare-only runner path prepared a gated smoke without executing the heavy command. | This is expected for prepare-only evidence. Set `ALICE_QA_RUN_GATED_SMOKES=1` only when intentionally running the gated smoke without `--prepare-only`. |
| Maven validation reports missing Tweedle parser grammar files | The Tweedle grammar submodule is not initialized. | Run `git submodule update --init tweedle-lang` and confirm `test -d tweedle-lang/Grammar`. |
| A review comment says the scenario proves Alice-world rendering correctness, Save completion, grading, creative assessment, or full first-lesson completion | The scenario wording is too broad. | Reword it to "launcher evidence wiring", "JavaFX handoff", "launcher marker-observation boundary", or "display no-go evidence" and keep those broader claims out of the Gadugi scenario. |
