# Tutorial: Trace the Gadugi Window menu registration evidence

This tutorial walks through the Gadugi Window menu registration evidence
scenario from contract test to delegated QA runner to underlying menu/action
smoke.

## What you will do

You will:

1. Read the Gadugi scenario YAML and understand its three-step delegation.
2. Run the contract test to verify structural correctness.
3. Trace each delegated step to its existing QA entry point.
4. Run the full scenario validation chain.
5. Review evidence boundaries and confirm no overclaims.

## Before you start

Open a terminal at the repository root and confirm the build prerequisites:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Step 1: Read the scenario YAML

Open the Gadugi scenario:

```bash
cat qa/outside-in/alice-desktop/gadugi/window-menu-registration-evidence.yaml
```

Key observations:

- **Name:** `window-menu-registration-evidence` — this is the stable Gadugi
  identity.
- **Description:** Conservative wording with explicit non-claims about
  rendering, pixels, desktop windows, JavaFX layout, lesson completion, and the
  full Alice desktop workflow.
- **Agent type:** `cli` with working directory `.` — commands run from the
  repository root, not a subdirectory.
- **Tags:** `cli`, `gadugi`, `pr-401`, `menu-action`,
  `window-menu-registration-evidence` — discoverable by PR and feature.
- **Three steps:** validate catalog, prepare evidence, run tests.
- **Empty assertions:** The scenario intentionally avoids adding Gadugi-level
  assertions that could overclaim.

## Step 2: Run the contract test

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/test-gadugi-window-menu-contract.sh
```

The contract test uses an embedded Python script with `yaml.safe_load` to
validate six structural properties:

1. **Path check:** The scenario file lives under `gadugi/`, not `scenarios/`.
2. **Name check:** The scenario name equals `window-menu-registration-evidence`.
3. **Description check:** Required phrases (`PR #401`, `Window menu model
   registration`, `existing QA entry points`) are present and required non-claim
   phrases (`does not validate visible rendering`, `rendered pixels`, `visible
   desktop windows`, `JavaFX scene-graph layout`, `full lesson completion`,
   `full Alice desktop workflow`) confirm conservative wording.
4. **Metadata check:** Tags include all five required values.
5. **Step check:** Exact delegated commands and arguments match the expected
   three-step pipeline.
6. **Assertion check:** The assertions list is empty.

If any check fails, the test prints the specific failure reason.

## Step 3: Trace the delegated steps

### Step 3a: Validate the scenario catalog

The first delegated step runs:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

This validates all custom Alice scenarios under `scenarios/`. It is the same
validator used by the existing QA lane. The Gadugi scenario calls it to
confirm the underlying catalog is healthy before proceeding.

### Step 3b: Prepare menu-action evidence

The second delegated step runs:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/gadugi-window-menu-registration
```

Key points:

- `alice-desktop-menu-action-smoke` is the custom Alice scenario that declares
  the `AliceMenuBarContractTest` gated command smoke.
- `--prepare-only` creates reviewable evidence without running the Maven
  command.
- Evidence is written to a directory unique to this Gadugi scenario, separate
  from other evidence paths.

### Step 3c: Run the test suite

The third delegated step runs:

```bash
qa/outside-in/alice-desktop/tests/run-tests.sh
```

This runs all `test-*.sh` scripts, including the Gadugi Window menu contract
test itself. This confirms the entire test suite passes with the new contract
test included.

## Step 4: Run the full validation chain

Execute all three steps manually to see the full evidence chain:

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

All three commands should exit successfully. The evidence directory will contain
generated runner artifacts.

## Step 5: Review evidence boundaries

Open the scenario YAML description again and confirm:

- The description says "existing QA entry points **only**".
- The description lists six explicit non-claims.
- The assertions list is empty.
- No step introduces a new validation command beyond the existing runners.

Compare with the related Gadugi scenarios:

```bash
ls qa/outside-in/alice-desktop/gadugi/
```

You should see three scenarios:

- `exported-launcher-evidence.yaml` — PR #155 exported launcher evidence
- `run-render-affordance-evidence.yaml` — run/render affordance evidence
- `window-menu-registration-evidence.yaml` — PR #401 Window menu registration

Each follows the same three-step delegation pattern with conservative
descriptions and empty assertions.

## What you learned

- Gadugi scenarios live in `gadugi/`, separate from the custom Alice
  `scenarios/` catalog.
- The Window menu registration evidence scenario delegates to three existing QA
  entry points instead of implementing its own validation.
- The contract test enforces structural correctness, conservative wording, and
  absence of overclaiming assertions.
- The default `--prepare-only` path creates reviewable evidence without the
  heavy gated Maven command.
- Evidence boundaries explicitly exclude rendering, pixels, desktop windows,
  JavaFX layout, lesson completion, and full desktop workflow claims.

## Related documentation

- [Gadugi Window menu registration evidence reference](../reference/gadugi-window-menu-registration-evidence.md)
- [Run Gadugi Window menu registration evidence](../howto/run-gadugi-window-menu-registration-evidence.md)
- [Window menu action contract](../reference/window-menu-action-contract.md)
- [PR #401 UI Action Menu Contract Handoff](../reference/pr401-ui-action-menu-contract-evidence.md)
- [Gadugi exported launcher evidence scenario](../reference/gadugi-exported-launcher-evidence.md)
