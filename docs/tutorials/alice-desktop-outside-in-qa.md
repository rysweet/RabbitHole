# Alice desktop outside-in QA tutorial

This tutorial walks through an outside-in QA evidence pass: validate the catalog, collect real launch evidence, review post-open runtime/display accessibility evidence, and complete manual save/load evidence.

## What you will do

You will:

1. Validate the scenario catalog.
2. List the executable scenario catalog.
3. Run Alice under Xvfb for the launch workflow.
4. Review post-open runtime/display accessibility evidence.
5. Generate a save/load evidence checklist.
6. Add user-visible evidence to the generated run directory.

## Before you start

Open a terminal at the repository root and confirm the build prerequisites:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Step 1: Validate scenarios

Run:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The catalog is ready when the command reports the active scenario directory and a valid scenario count.

## Step 2: List the scenario catalog

Run:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh list
```

The list includes:

```text
alice-desktop-launch
alice-desktop-instructor-student-setup
alice-desktop-scene-creation
alice-desktop-run-debug
alice-desktop-save-load
alice-desktop-export
alice-desktop-exported-project-smoke
alice-desktop-netbeans-package-smoke
alice-desktop-project-io-smoke
alice-desktop-failure-path-smoke
alice-desktop-future-ui-smoke
```

You can run scenarios by ID or by direct YAML path. In later steps, use the ID form shown in the commands. When reviewing a scenario file, replace the ID with the direct path:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  qa/outside-in/alice-desktop/scenarios/save-load.yaml \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

## Step 3: Run Alice under Xvfb

Run the launch scenario:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

When the launch is successful, the runner prints the evidence directory. Open that directory and review:

```text
environment.txt
launch.log
status.txt
xvfb.log
screenshot.png or screenshot.xwd
```

The screenshot captures the observed desktop state. The launch log and status file explain how the runner started Alice, whether it detected a visible window, and whether the process stayed alive through evidence capture. Review these artifacts before accepting the launch evidence.

If the command exits before producing this full set, keep the generated fallback checklist and diagnostics. A fallback checklist is useful for manual follow-up, but it is not accepted launch evidence by itself.

## Step 4: Review post-open runtime/display accessibility evidence

Run the bounded post-open scenario:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

Open the generated run directory and review:

```text
post-open-runtime-display-accessibility-evidence.json
status.txt
x-window-inventory.json
launch.log
xvfb.log
screenshot.png or screenshot.xwd
```

Accept this tutorial step only when `post-open-runtime-display-accessibility-evidence.json` records `status=observed`, `postOpenRuntimeDisplayAccessibilityObserved=true`, `runtimeDisplayCandidateCount` greater than zero, and `blocker=none`. If the artifact records `status=blocked`, keep it as the machine-readable gap report. Do not convert a blocker into a manual rendering, world execution, grading, lesson completion, Save, Select Project, installer, or decoder claim.

## Step 5: Generate a save/load checklist

Run:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-save-load \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

The runner creates a manual evidence checklist because save/load uses real Swing interactions that are not automated by this lane. Checklist generation is preparation, not completion.

Open:

```text
qa/outside-in/alice-desktop/evidence/tutorial-runs/alice-desktop-save-load/<timestamp>/manual-evidence-checklist.txt
```

## Step 6: Perform the save/load workflow

Follow the checklist in Alice:

1. Launch Alice.
2. Create or open a small project.
3. Save the project as an `.a3p` file in the run directory.
4. Close the project or restart Alice.
5. Open the saved `.a3p` file.
6. Confirm the loaded scene or program state matches the saved state.

Add these files to the same timestamped run directory:

```text
before-save.png
after-reopen.png
saved-project.a3p
review-notes.txt
```

`review-notes.txt` should identify the visible object, template, or program state you used to compare the saved and loaded project. It should also list the evidence files reviewed, note any deviations from the checklist, and end with `decision: accept` or `decision: reject`.

The save/load scenario is complete only after the workflow has been performed in Alice and the required evidence, including `review-notes.txt`, has been added to the run directory.

## Step 7: Prepare a gated command smoke

Prepare gated smoke evidence without enabling heavy execution:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-netbeans-package-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

The runner writes `status.txt` with `outcome=gated-not-run` plus a checklist. Omitting both `--prepare-only` and `ALICE_QA_RUN_GATED_SMOKES=1` exits non-zero so a gated skip cannot pass by accident. To execute the configured Maven/package command in a prepared worktree, rerun with:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-netbeans-package-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

## Step 8: Keep evidence out of commits

Evidence files are local run artifacts. Keep them for review or attach them to the relevant review record, but do not commit them.

Commit only documentation, scenario YAML, schema changes, and runner changes. Remove `qa/outside-in/alice-desktop/evidence/tutorial-runs` when you no longer need the tutorial artifacts.
