# Alice desktop outside-in QA tutorial

This tutorial walks through an outside-in QA evidence pass: validate the catalog, collect real launch evidence, review the target-specific Select Project proof, review the first-lesson procedure target observation, review post-open runtime/display accessibility evidence, and complete manual save/load evidence.

## What you will do

You will:

1. Validate the scenario catalog.
2. List the executable scenario catalog.
3. Run Alice under Xvfb for the launch workflow.
4. Review the target-specific Select Project `Africa Full` proof.
5. Review the first-lesson live procedure target observation.
6. Review post-open runtime/display accessibility evidence.
7. Generate a save/load evidence checklist.
8. Review the learner-world setup/open/save boundary.
9. Add user-visible evidence to the generated run directory.

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

Confirm the output includes the scenario IDs used later in this tutorial:

```text
alice-desktop-launch
alice-desktop-select-project-tab-click-exec
alice-desktop-first-lesson-live-procedure-target-observation
alice-desktop-post-open-runtime-display-accessibility-evidence
alice-desktop-save-load
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

## Step 4: Review the Select Project Africa Full proof

Run the bounded Select Project scenario:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs \
  --timeout-seconds 300
```

Open the generated run directory and review:

```text
tab-click-observation.json
select-project-window.json
x-window-inventory.json
status.txt
launch.log
xvfb.log
screenshot.png or screenshot.xwd
```

Accept this tutorial step only when `tab-click-observation.json` records `evidenceStatus=opened`, exact `Africa Full` `targetStarter` metadata, `targetStarterObserved.name=Africa Full`, `targetStarterSelected=true`, `targetStarterOpenAttempted=true`, matching `openedStarter`, and `projectOpenObserved=true`. A blocked artifact is still useful when it preserves `blocker` and `blockerDetail` and provides one structured `nextBlocker` with the observed AT-SPI state, action attempted, `expectedNextAction`, and reason progress stopped. Do not convert either result into a full Alice UI automation, visible rendering, grading, creative assessment, Save completion, first-lesson completion, unrelated launcher, or unrelated decoder claim.

## Step 5: Review the first-lesson live procedure target

Run the read-only first-lesson procedure target observation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs \
  --timeout-seconds 300
```

Open the generated run directory and review:

```text
first-lesson-live-procedure-target-observation.json
status.txt
tab-click-observation.json
post-project-open-observation.json
x-window-inventory.json
launch.log
xvfb.log
```

Accept this tutorial step only when the decision artifact records
`status=observed`, `blocker=none`, and an `observedTarget` for
`scene.eatmeFirstLesson` with a non-empty stable `automationPath`. If it records
`status=blocked`, keep it as the next-blocker artifact for the missing procedure
tab/code-editor target. Do not convert either result into a desktop edit, Save,
rendering, learner assessment, grading, creative assessment, or full
first-lesson completion claim.

## Step 6: Review post-open runtime/display accessibility evidence

Run the bounded post-open scenario:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs \
  --timeout-seconds 300
```

Open the generated run directory and review:

```text
post-open-runtime-display-accessibility-evidence.json
runtime-display-accessibility-status.txt
status.txt
tab-click-observation.json
post-project-open-observation.json
x-window-inventory.json
controlled-display-pixel-observation.json
launch.log
xvfb.log
screenshot.png or screenshot.xwd
```

Review the decision artifact without modifying the run:

```bash
python3 -m json.tool \
  <run-directory>/post-open-runtime-display-accessibility-evidence.json

sed -n '1,120p' <run-directory>/status.txt
```

Accept this tutorial step only as blocked at the rendered-pixel sampling seam when `status.txt` records `outcome=blocked`, `runtimeDisplayAccessibilityStatus=observed`, `controlledDisplayPixelStatus=observed`, `visibleRenderingPixelSamplingStatus=blocked`, and `visibleRenderingPixelSamplingArtifact=visible-rendering-pixel-sampling-blocker.json`, and when `post-open-runtime-display-accessibility-evidence.json` records `status=observed`, `postOpenRuntimeDisplayAccessibilityObserved=true`, `runtimeDisplayCandidateCount` greater than zero, and `blocker=none`. Future visible-rendering pass requires `outcome=passed` and `visibleRenderingPixelSamplingStatus=observed`; target readiness alone is not enough. `runtime-display-accessibility-status.txt` is probe-local; use final `status.txt` for the overall pass/block decision. Use `tab-click-observation.json`, `post-project-open-observation.json`, `controlled-display-pixel-observation.json`, and `visible-rendering-pixel-sampling-blocker.json` to understand the supporting project-open, controlled-display, target-readiness, and blocked pixel-sampling setup. If the artifact or final status records a blocker, keep it as the machine-readable gap report. Do not convert a blocker into a manual rendering, world execution, grading, lesson completion, Save, Select Project, installer, or decoder claim. The full review contract is documented in [Post-open runtime/display accessibility evidence](../reference/post-open-runtime-display-accessibility-evidence.md).

## Step 7: Generate a save/load checklist

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

## Step 8: Perform the save/load workflow

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

## Step 9: Review the learner-world boundary

RabbitHole learner-world QA currently supports setup/open/save evidence review
only for this lane.

Run the instructor/student setup scenario:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-instructor-student-setup \
  --evidence-dir qa/outside-in/alice-desktop/evidence/tutorial-runs
```

Open the generated run directory and review `manual-evidence-checklist.txt`,
`environment.txt`, and `status.txt`. The checklist describes the manual evidence
needed for instructor starter-project setup, student open, and student save
review. Checklist generation does not complete the scenario.

Before accepting the run, add the instructor launch log, starter project
screenshot, saved starter `.a3p`, student launch or open log, loaded project
screenshot, saved student copy `.a3p`, and `review-notes.txt`.
`review-notes.txt` should identify the reviewed files and the setup/open/save
decision only. Do not turn this evidence into learner-work grading, rubric
scoring, correctness assessment, or creativity assessment.

Review the checked-in blocker record:

```bash
python3 -m json.tool \
  qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

The record is documentation for the current boundary. It names
`define-reviewed-assessment-contract` as the next blocker before any future
learner-work grading, rubric scoring, correctness assessment, or creativity
assessment capability can be claimed.

## Step 9: Prepare a gated command smoke

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

## Step 10: Keep evidence out of commits

Evidence files are local run artifacts. Keep them for review or attach them to the relevant review record, but do not commit them.

Commit only documentation, scenario YAML, schema changes, and runner changes. Remove `qa/outside-in/alice-desktop/evidence/tutorial-runs` when you no longer need the tutorial artifacts.
