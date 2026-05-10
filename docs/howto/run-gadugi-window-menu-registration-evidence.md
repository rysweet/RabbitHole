# Run Gadugi Window menu registration evidence

Use this how-to when you need to validate or run the Gadugi-compatible Window
menu registration evidence scenario for PR #401 review readiness.

## Prerequisites

Run commands from the repository root.

Confirm the build prerequisites:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Gadugi tooling must be installed and available on `PATH` for the `gadugi-test`
commands. The contract test and prepare-only runner path do not require
`gadugi-test`.

## Validate the scenario

Validate the Gadugi scenario YAML:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml
```

The validator confirms the scenario conforms to the Gadugi CLI schema. Do not
validate against the custom Alice scenario schema in `scenarios/`.

## Run the contract test

Run the structural contract test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh
```

The contract test uses PyYAML (`yaml.safe_load`) to check:

1. Scenario lives under `gadugi/`, not `scenarios/`.
2. Name is `window-menu-registration-evidence`.
3. Description mentions PR #401, Window menu model registration, and existing QA
   entry points.
4. Description explicitly avoids overclaiming visible rendering, rendered pixels,
   visible desktop windows, JavaFX scene-graph layout, full lesson completion,
   and the full Alice desktop workflow.
5. Metadata tags include `cli`, `gadugi`, `pr-401`, `menu-action`, and
   `window-menu-registration-evidence`.
6. Steps delegate to the three existing QA entry points with exact commands.
7. Assertions list is empty.

The test is also automatically discovered by `run-tests.sh`.

## Run the scenario with Gadugi

Run the full Gadugi scenario:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s window-menu-registration-evidence \
  --timeout 300000
```

This executes three steps:

1. Validates the outside-in scenario catalog.
2. Prepares menu-action smoke evidence using `--prepare-only`.
3. Runs the full QA test suite.

The default run uses `--prepare-only` and does not execute the gated Maven
smoke.

## Run the prepare-only path without Gadugi

If `gadugi-test` is not installed, use the runner directly:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/run-scenario.sh run \
    alice-desktop-menu-action-smoke \
    --prepare-only \
    --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration

NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/run-tests.sh
```

These three commands reproduce the same work the Gadugi scenario automates.

## Execute the gated Maven smoke

Use this path only when a review explicitly requires Maven command evidence:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration
```

Or run the focused contract test directly:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

## Review evidence

After a successful run, review the generated evidence:

```bash
ls qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration/
```

Evidence files are runtime artifacts and are not committed to the repository.
Attach them to the PR or review thread when a reviewer requests concrete
evidence output.

## Review checklist

Use this checklist when reviewing changes to the Gadugi Window menu
registration evidence lane:

1. Confirm the scenario YAML lives under `gadugi/`, not `scenarios/`.
2. Confirm the scenario name is `window-menu-registration-evidence`.
3. Confirm the description mentions PR #401 and lists explicit non-claims.
4. Confirm the three steps delegate to existing QA entry points with unchanged
   commands.
5. Confirm the assertions list is empty.
6. Confirm the contract test passes.
7. Confirm no scenario wording claims rendering, Save, assessment, or lesson
   completion.

## Related documentation

- [Gadugi Window menu registration evidence reference](../reference/gadugi-window-menu-registration-evidence.md)
- [Window menu action contract](../reference/window-menu-action-contract.md)
- [PR #401 UI Action Menu Contract Handoff](../reference/pr401-ui-action-menu-contract-evidence.md)
- [Run Alice desktop outside-in QA](./alice-desktop-outside-in-qa.md)
- [Tutorial: Trace the Gadugi Window menu registration evidence](../tutorials/gadugi-window-menu-registration-evidence.md)
