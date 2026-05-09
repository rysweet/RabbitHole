# Run Alice desktop outside-in QA

Use the Alice desktop outside-in QA lane to validate the scenario catalog and collect reviewable evidence for user-like workflows: launch, Select Project inventory, the first-lesson live procedure target action seam, instructor/student setup, scene creation, run/debug-like behavior, save/load, open/load/save, export, exported-project smoke, NetBeans package smoke, package/install smoke, saving, reopening, editing, saving again, reopening again, and exporting Alice projects, failure-path smoke, future UI smoke, menu/action smoke, wizard/palette/completion smoke, and post-open runtime/display accessibility evidence.

## Contents

- [Prerequisites](#prerequisites)
- [Validate the scenario catalog](#validate-the-scenario-catalog)
- [Validate a custom scenario catalog](#validate-a-custom-scenario-catalog)
- [List available scenarios](#list-available-scenarios)
- [Run branch-installable checks with uvx](#run-branch-installable-checks-with-uvx)
- [Run the real Alice launch scenario](#run-the-real-alice-launch-scenario)
- [Open Africa Full from Select Project](#open-africa-full-from-select-project)
- [Observe the first-lesson live procedure target](#observe-the-first-lesson-live-procedure-target)
- [Collect post-open runtime/display accessibility evidence](#collect-post-open-runtimedisplay-accessibility-evidence)
- [Prepare evidence for manual workflows](#prepare-evidence-for-manual-workflows)
- [Review the learner-world boundary](#review-the-learner-world-boundary)
- [Choose a custom evidence directory](#choose-a-custom-evidence-directory)
- [Configure scenario and Xvfb runs](#configure-scenario-and-xvfb-runs)
- [Review evidence](#review-evidence)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Run commands from the repository root.

Alice desktop QA uses the same build and launch prerequisites as the main project:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

The real desktop launch scenarios use Xvfb when available. The post-open runtime/display accessibility scenario also requires the same AT-SPI stack used by the live Swing probes: `python3-pyatspi`, `libatk-wrapper-java`, and an AT-SPI2 accessibility bus for the current user session. Manual scenarios do not require Xvfb; they generate structured evidence checklists. Gated command smokes do not run heavy Maven or GUI commands unless `ALICE_QA_RUN_GATED_SMOKES=1` is set.

No browser surface is part of this lane, so Playwright is not required. Virtual TTY tools are only useful for terminal wrappers and are not used for Swing GUI interaction.

## Validate the scenario catalog

Validate all checked-in scenario YAML files before running or reviewing them:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The validator prints the active catalog and valid scenario count:

```text
Validated <count> scenario(s) in .../qa/outside-in/alice-desktop/scenarios
```

## Validate a custom scenario catalog

Use `ALICE_QA_SCENARIO_DIR` when testing a local catalog before moving it into the checked-in `scenarios/` directory:

```bash
ALICE_QA_SCENARIO_DIR=qa/outside-in/alice-desktop/evidence/custom-scenarios \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Custom catalogs cannot introduce arbitrary shell commands. `xvfb-real-alice` and `gated-command-smoke` automation must use one of the schema's allowed argv lists; the runner executes argv directly without shell interpretation and validates cwd realpaths stay inside the repository.

List the same active catalog through either entry point:

```bash
ALICE_QA_SCENARIO_DIR=qa/outside-in/alice-desktop/evidence/custom-scenarios \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --list

ALICE_QA_SCENARIO_DIR=qa/outside-in/alice-desktop/evidence/custom-scenarios \
qa/outside-in/alice-desktop/runners/run-scenario.sh list
```

## List available scenarios

List scenario IDs, automation modes, and titles:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh list
```

Use the scenario ID from the first column when running a scenario.

You can also run a scenario by its checked-in YAML path. The path must point directly to a `.yaml` file inside the active scenario directory:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  qa/outside-in/alice-desktop/scenarios/save-load.yaml
```

## Run branch-installable checks with uvx

The repository exposes a small `amplihack alice-qa` command so reviewers can install the command wrapper from a PR branch and execute the checked-out QA lane:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack alice-qa list

uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> \
  amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Run these commands from the root of a checkout of the same branch. The installed wrapper delegates to `qa/outside-in/alice-desktop/runners/` in that checkout so the output and evidence contract match direct runner usage.
Replace `<branch>` with the PR branch or commit you are reviewing.

## Run the real Alice launch scenario

The launch scenario starts the real Alice desktop through Maven under Xvfb:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

On a first-run profile, the Alice License Agreement dialogs may be the expected
blocker. To keep that state out of real user preferences during a controlled QA
launch, opt in explicitly:

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

Before launch, the runner verifies `alice-ide/pom.xml` configures
`org.alice.ide.rootDirectory=../core/resources/target/distribution`. If
`core/resources/target/distribution` is missing, it prepares that distribution
from the repository root with:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests -pl core/resources process-resources
```

It writes `root-directory-prep.json` for both ready/prepared and blocked cases,
naming the exact property, distribution path, Maven project, and Maven phase.
The scenario then runs:

```bash
cd alice-ide
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests compile exec:java -Dalice-ide
```

The explicit `compile` step puts `org.alice.stageide.EntryPoint` in
`alice-ide/target/classes` before `exec:java`. The checkstyle and test gates are
run separately; this display runner keeps the launch proof focused on root
directory readiness, classpath, process lifetime, window readiness, and
screenshot evidence. The scenario YAML represents that launch as
`automation.argv`, not as a shell command string, and the validator rejects
unapproved argv entries before the runner starts Xvfb or Alice.

The runner writes evidence to:

```text
qa/outside-in/alice-desktop/evidence/alice-desktop-launch/<timestamp>/
```

A successful launch evidence capture includes `root-directory-prep.json`, `license-acceptance.json`, `license-dialog.json`, an environment summary, Xvfb log, Alice launch log, status file, screenshot, and `x-window-inventory.json`. The window inventory records Alice-related visible X window title, class, process, and geometry after the readiness wait so a blocked run names the exact Alice window signal that was or was not present. The license artifacts record the exact first-run dialog title/controls when observed, or the isolated `java.util.prefs.userRoot` state files under `.java/.userPrefs/` used when `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` is set. The runner checks root-directory preparation, first-run license state, process, window-readiness, and screenshot-capture status; it does not deeply classify every line in `launch.log` as a semantic pass/fail oracle. Review `root-directory-prep.json`, `license-acceptance.json`, `license-dialog.json`, `status.txt`, `x-window-inventory.json`, `launch.log`, and the screenshot before treating the launch evidence as accepted.

If Xvfb is unavailable, no display can be selected, or Xvfb exits before Alice starts, the runner exits non-zero and writes a manual fallback checklist with whichever early diagnostics are available. These early fallback directories may not contain `status.txt` because the launch did not reach the evidence-capture phase. The post-open runtime/display accessibility scenario is stricter: it also writes `post-open-runtime-display-accessibility-evidence.json` and `status.txt` with a blocked runtime/display accessibility outcome for those early prerequisites.

If launch evidence is collected in CI or another disposable workspace, pass an explicit evidence directory:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs \
  --timeout-seconds 180
```

### Run the Select Project inventory proof

After license opt-in, the Select Project proof waits for the real chooser window and records its title, class, process, and geometry without selecting or opening a project:

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-select-project-inventory \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-proof
```

Review `select-project-window.json` with `status=observed`, `interactionProof=select-project-window-visible`, and `projectWorldInteraction=not-observed`. The widget labels are resource-contract evidence only until live Swing widget introspection exists; the artifact names `swing-widget-inventory-not-collected` rather than claiming widget observation.

### Run the Select Project live Swing widget introspection proof

After the inventory proof, the widget introspection proof attempts live AT-SPI enumeration of the Select Project frame's accessible children. It requires `python3-pyatspi` and `libatk-wrapper-java`. The runner sets `JAVA_TOOL_OPTIONS` and `CLASSPATH` automatically when it detects this scenario:

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-select-project-widget-introspection \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-widget-introspection
```

Review `swing-widget-observation.json`. If `status=observed`, the Java process and Swing widgets are visible through AT-SPI (`widgetCount>0`); note that tab labels are not currently enumerated (`tabLabels` is empty, `tabLabelMatch` is false). If `status=blocked`, `blocker` and `blockerDetail` name the exact missing condition (e.g., `atk-wrapper-not-loaded` with the exact `JAVA_TOOL_OPTIONS` and `CLASSPATH` required).

### Run the Select Project AT-SPI exec:exec remediation proof

The `exec:java` scenario (above) hits a hard blocker: `exec:java` shares the Maven JVM where AWT is already initialised without the ATK wrapper. The `alice-desktop-select-project-atk-exec` scenario applies a two-step remediation: it uses `exec:exec@alice-ide-atk` to spawn a fresh JVM with `/usr/share/java/java-atk-wrapper.jar` on the **bootstrap classpath** (`-Xbootclasspath/a:`) and `-Djavax.accessibility.assistive_technologies=org.GNOME.Accessibility.AtkWrapper` so the wrapper is active before the first AWT call. It also sets `NO_AT_BRIDGE=1` because the successful run observed Swing widgets with that environment variable set; whether this variable is strictly necessary has not been A/B validated separately. If both steps work correctly, `swing-widget-observation.json` will record `status=observed` with live widget introspection data.

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-select-project-atk-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-atk-exec
```

Review `swing-widget-observation.json`. If `status=observed`, the Java process and Swing widgets are visible through AT-SPI (`widgetCount>0`, `dialog` role). Tab labels are not currently enumerated via AT-SPI (`tabLabels` is empty, `tabLabelMatch` is false). If `status=blocked`, `blocker` and `blockerDetail` name the exact remaining condition.

## Open Africa Full from Select Project

The Select Project tab-click scenario targets the committed starter project `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` with display name `Africa Full`. It starts from the existing Select Project/main-window proof path and advances only through AT-SPI automation.

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Review `status.txt`, `x-window-inventory.json`, `select-project-window.json`, and `tab-click-observation.json`. The target contract for this feature requires `evidenceStatus=opened`, exact `Africa Full` `targetStarter` metadata, safe `startersTabSafety` with `activatedBeforeTargetSearch=true` and `targetSearchScope=active-starters-tab`, `targetSelectionObserved=true`, `openAttempted=true`, `projectOpenObserved=true`, and Alice Java/window PID context from the same run.

If the probe cannot safely prove target-specific selection/opening, it must preserve the existing string `blocker` and `blockerDetail` fields, then add structured target-specific detail in `nextBlocker`, including the observed AT-SPI state, action attempted, `expectedNextAction`, and reason progress stopped. A blocked result is the correct output when continuing would turn generic Select Project dismissal or main-window state into an unsupported Africa Full claim.

For the full evidence contract, see [Select Project Africa Full AT-SPI evidence reference](../reference/select-project-africa-full-atspi-evidence.md).

## Observe the first-lesson live procedure target action seam

The first-lesson live procedure target action seam contract verifies only
whether a post-open live desktop exposes a stable procedure tab or code-editor
target for `scene.eatmeFirstLesson`, then classifies that target as edit-ready or
blocked by the missing public CodeEditor/CodeComposite edit invocation contract.
It reuses the Select Project and post-project-open evidence path, then writes a
machine-readable action-seam decision. It does not prove desktop editing, Save
behavior, rendering correctness, learner assessment, grading, creative
assessment, or full first-lesson completion.

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir qa/outside-in/alice-desktop/evidence/first-lesson-procedure-target \
  --timeout-seconds 300
```

Review `first-lesson-live-procedure-target-observation.json`, `status.txt`,
`tab-click-observation.json`, and `post-project-open-observation.json`. Accept
the action-seam proof only when the decision artifact records either
`status=edit-ready`, `observedTarget.readyForDesktopEditAction=true`,
`desktopEditAction.blocker.kind=none`, and top-level `blocker.kind=none`, or the
exact no-go blocker in both `desktopEditAction.blocker.kind` and top-level
`blocker.kind`: `missing-desktop-edit-action-contract` with
`blocker.message=missing public CodeEditor/CodeComposite edit invocation
contract`. Treat target-only observation, display, AT-SPI, and target-not-found
blockers as implementation or run-prerequisite gaps, not accepted action-seam
proof.

For the artifact API, configuration, examples, and claim boundaries, see
[First-Lesson Live Procedure Target Action Seam](../reference/first-lesson-live-procedure-target-observation.md).

## Collect post-open runtime/display and target-scoped pixel evidence

The `alice-desktop-post-open-runtime-display-accessibility-evidence` scenario
collects evidence for one bounded post-open rendering-adjacent step. It reuses
the supported Alice launch, isolated license acceptance, Xvfb, ATK wrapper, and
project-open setup, then runs a read-only AT-SPI probe against the live Alice
accessibility tree. The runner validates exactly one visible/showing
Run-window/world-canvas target with positive screen-coordinate extents, then
attempts target-scoped raw RGBA sampling inside that target under controlled
conditions. If target validation or sampling cannot support that bounded
observation, it writes the precise blocker instead. Neither result is a full rendering, visible
rendering correctness, world execution, grading, lesson completion, Save, Select
Project, installer, or decoder proof. For the complete artifact API and review contract,
see [Post-open runtime/display accessibility evidence](../reference/post-open-runtime-display-accessibility-evidence.md).

Command from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

The runner writes these artifacts in the timestamped run directory when execution reaches evidence capture:

```text
environment.txt
root-directory-prep.json
license-acceptance.json
license-dialog.json
xvfb.log
launch.log
x-window-inventory.json
tab-click-observation.json
post-project-open-observation.json
post-open-runtime-display-accessibility-evidence.json
runtime-display-accessibility-status.txt
controlled-display-pixel-observation.json
visible-rendering-pixel-sampling-blocker.json
visible-rendering-pixel-observation.json
status.txt
screenshot.png or screenshot.xwd
```

`post-open-runtime-display-accessibility-evidence.json` is the decision artifact. A passing observation emits these fields:

```json
{
  "automationMode": "xvfb-real-alice",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 1,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "name": "Scene display",
      "path": "application/0/3",
      "role": "canvas",
      "states": ["enabled", "showing", "visible"],
      "geometryStatus": "available",
      "screenExtents": {
        "coordinateType": "screen",
        "x": 144,
        "y": 188,
        "width": 996,
        "height": 642
      }
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
```

For acceptance, check the minimum decision fields:

```json
{
  "status": "observed",
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "runtimeDisplayCandidateCount": 1,
  "runtimeDisplayCandidates": [
    {
      "childCount": 0,
      "name": "Scene display",
      "path": "application/0/3",
      "role": "canvas",
      "states": ["enabled", "showing", "visible"],
      "geometryStatus": "available",
      "screenExtents": {
        "coordinateType": "screen",
        "x": 144,
        "y": 188,
        "width": 996,
        "height": 642
      }
    }
  ],
  "blocker": "none"
}
```

If an implementation or environment prerequisite is missing, the runner still writes the same JSON file and records a machine-readable blocker instead of passing:

```json
{
  "automationMode": "xvfb-real-alice",
  "blocker": "pyatspi-not-installed",
  "blockerDetail": "python3-pyatspi is not installed.",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "javaPid": 12345,
  "postOpenRuntimeDisplayAccessibilityObserved": false,
  "postOpenWindowObserved": true,
  "runtimeDisplayCandidateCount": 0,
  "runtimeDisplayCandidates": [],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "blocked",
  "traversalErrors": []
}
```

Use `status.txt` for automation and
`post-open-runtime-display-accessibility-evidence.json` for detailed review.
`runtime-display-accessibility-status.txt` is probe-local and useful for
debugging the AT-SPI probe result, but `status.txt` is the final scenario status
because it also records `controlledDisplayPixelStatus`,
`controlledDisplayPixelBlocker`, `visibleRenderingPixelSamplingStatus`, and
`visibleRenderingPixelSamplingArtifact`. Review `tab-click-observation.json`,
`post-project-open-observation.json`, `controlled-display-pixel-observation.json`,
and `visible-rendering-pixel-sampling-blocker.json` or
`visible-rendering-pixel-observation.json` as supporting setup and the bounded
sampling result.

To review the latest run directory without changing it:

```bash
run_dir=$(find qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  -path '*/alice-desktop-post-open-runtime-display-accessibility-evidence/*' \
  -type d | sort | tail -n 1)

sed -n '1,120p' "$run_dir/status.txt"
python3 -m json.tool \
  "$run_dir/post-open-runtime-display-accessibility-evidence.json"
```

Accept the run as runtime/display, controlled-display, and bounded sampling
evidence only when `status.txt` records
`outcome=passed`,
`runtimeDisplayAccessibilityStatus=observed`,
`controlledDisplayPixelStatus=observed`,
`visibleRenderingPixelSamplingStatus=observed`, and
`visibleRenderingCorrectnessEstablished=false`; the JSON decision artifact records
`status=observed`, `blocker=none`,
`postOpenRuntimeDisplayAccessibilityObserved=true`, and
`runtimeDisplayCandidateCount` greater than zero; and
`visible-rendering-pixel-observation.json` records
`visibleRenderingCorrectnessEstablished=false`, checked sample points inside the
validated target, and raw RGBA values. If the sampling status is blocked, review
`visible-rendering-pixel-sampling-blocker.json` and preserve `status=blocked` as
the correct machine-readable gap report when the environment, post-open setup,
controlled-display pixels, target validation, or sampler is unavailable.

## Prepare evidence for manual workflows

Manual workflows are still executable: the runner creates a checklist with preconditions, user actions, expected outcomes, evidence requirements, and fallback notes.

Example:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-save-load
```

The runner writes:

```text
qa/outside-in/alice-desktop/evidence/alice-desktop-save-load/<timestamp>/manual-evidence-checklist.txt
qa/outside-in/alice-desktop/evidence/alice-desktop-save-load/<timestamp>/environment.txt
qa/outside-in/alice-desktop/evidence/alice-desktop-save-load/<timestamp>/status.txt
```

Follow the checklist while using Alice, then place the required screenshots, project files, logs, or exported artifacts in the same run directory. Generating `manual-evidence-checklist.txt` only prepares the scenario; the manual scenario is complete only after a human performs the workflow and adds the required evidence artifacts plus `review-notes.txt`.

For example, a save/load evidence directory should contain the generated checklist plus the saved project, before/after screenshots, and `review-notes.txt` comparing the reopened project with the saved state.

Use this review note shape for manual acceptance:

```text
scenario: alice-desktop-save-load
runDirectory: qa/outside-in/alice-desktop/evidence/manual-runs/alice-desktop-save-load/<timestamp>
reviewedEvidence:
  - manual-evidence-checklist.txt
  - before-save.png
  - after-reopen.png
  - saved-project.a3p
observedResult: The reopened project matched the saved scene and program state.
deviations: None.
decision: accept
```

## Review the learner-world boundary

Use `alice-desktop-instructor-student-setup` when you need learner-world evidence
for an instructor starter project and a student copy:

RabbitHole learner-world QA currently supports setup/open/save evidence review
only for this lane.

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-instructor-student-setup \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The runner prepares a manual checklist. A reviewer then uses Alice to collect the
required setup/open/save artifacts in the generated run directory:

```text
manual-evidence-checklist.txt
environment.txt
status.txt
instructor-launch.log
starter-project-open.png
starter-project.a3p
student-open.log
student-project-open.png
student-copy.a3p
review-notes.txt
```

Open `manual-evidence-checklist.txt` before collecting the manual artifacts. The
checklist describes the manual evidence required for setup/open/save review and
includes a generated `Assessment boundary` section. `status.txt` repeats the
same boundary as plain fields, including
`assessmentBoundary=define-reviewed-assessment-contract` and
`assessmentBoundaryMode=manual/unsupported`. Use those generated lines and the
checked-in boundary record while reviewing this run, and treat missing
learner-world state extraction for grading or creative assessment as blocker
`define-reviewed-assessment-contract`, not as a hidden fallback.

Accept the manual run only as setup/open/save evidence. `review-notes.txt`
should list the reviewed files, state whether the starter project was prepared,
opened by the student, and saved as a separate copy, and end with `decision:
accept` or `decision: reject`. Do not use the run notes to claim learner-world
grading, rubric scoring, correctness assessment, or creative assessment.

The learner-world claim boundary is recorded in:

```text
qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

Treat that JSON file as documentation for the current boundary, not as runner
configuration. The current artifact records `id`, `selectedScenario`,
`automationMode`, `scope`, `currentCapability`, `supportedEvidence`,
`assessmentLimits`, `nonCapabilities`, `nextBoundary`,
`manualLimitationSummary`, `requiresReviewedAssessmentContractBefore`,
`nextBlocker`, and `blocker`. It keeps `define-reviewed-assessment-contract` as
the required next boundary before learner-world grading, rubric scoring,
correctness assessment, or creative assessment can be claimed. The boundary is
described in
[Learner-world assessment boundary](../reference/learner-world-assessment-boundary.md).

## Choose a custom evidence directory

Use `--evidence-dir` when evidence should live in a workspace-owned ignored directory, such as the QA lane evidence area or a CI artifact workspace:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-scene-creation \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

This creates:

```text
qa/outside-in/alice-desktop/evidence/manual-runs/alice-desktop-scene-creation/<timestamp>/
```

## Configure scenario and Xvfb runs

The runner accepts these environment variables:

| Variable | Purpose | Example |
| --- | --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Override the checked-in scenario catalog directory. | `ALICE_QA_SCENARIO_DIR=qa/outside-in/alice-desktop/evidence/custom-scenarios` |
| `ALICE_QA_DISPLAY` | Reuse a specific X display instead of selecting a free display from `:90` through `:120`. | `ALICE_QA_DISPLAY=:99` |
| `ALICE_QA_SCREEN` | Set Xvfb screen geometry. Defaults to `1280x900x24`. | `ALICE_QA_SCREEN=1600x1000x24` |
| `ALICE_QA_READY_WAIT_SECONDS` | Override the scenario readiness wait before screenshot capture. | `ALICE_QA_READY_WAIT_SECONDS=60` |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | Prepare isolated first-run License Agreement acceptance state for controlled QA launches only. | `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` |
| `ALICE_QA_RUN_GATED_SMOKES` | Run gated CLI/UI smoke commands instead of recording a non-success gated skip. | `ALICE_QA_RUN_GATED_SMOKES=1` |

Example:

```bash
ALICE_QA_SCREEN=1600x1000x24 \
ALICE_QA_READY_WAIT_SECONDS=60 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

Override the launch timeout when a workstation is slow:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch \
  --timeout-seconds 180
```

Do not use `--timeout-seconds`, scenario `automation.timeoutSeconds`, or shell `timeout` for the target `alice-desktop-save-menu-dialog-write-proof` workflow. That workflow is intentionally no-timeout and instead fails closed through bounded Java proof waits and the emitted Save proof evidence artifact.

Gated command smokes cover exported-project, NetBeans package, package/install, saving, reopening, editing, saving again, reopening again, and exporting Alice projects, failure path, future UI startup, menu/action plumbing, and wizard/palette/completion paths. Without `ALICE_QA_RUN_GATED_SMOKES=1`, those scenarios write `status.txt` with `outcome=gated-not-run` and exit non-zero so they cannot pass by accident. Use `--prepare-only` for intentional preflight/checklist preparation. Enable the gate only in a worktree prepared for the configured Maven, packaging, or display-backed command.

The QA lane itself does not require Node.js. If a surrounding QA orchestrator invokes Node-based tooling around this lane, use:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Review evidence

Every run directory is timestamped and self-contained. Review these files first:

| File | Meaning |
| --- | --- |
| `environment.txt` | Java, Maven, OS, repository root, timestamp, and display information. |
| `status.txt` | Run status. Real launch runs record display, readiness, process status, screenshot status, and timeout; manual runs record that human evidence is still required; gated smokes record whether the command was skipped, passed, or failed. |
| `launch.log` | Maven/Alice startup output for real launch scenarios. |
| `xvfb.log` | Xvfb startup and display output. |
| `x-window-inventory.json` | Alice-related visible X window title, class, process, and geometry captured after launch readiness wait, or an explicit unsupported/blocker record. |
| `select-project-window.json` | Select Project proof artifact recording exact title/class/process/geometry when observed, or an exact missing-window/widget-introspection blocker. |
| `tab-click-observation.json` | Supporting project-open setup artifact for Select Project tab activation/open attempts. |
| `post-project-open-observation.json` | Supporting project-open setup artifact recording whether the post-open Alice window signal was observed. |
| `post-open-runtime-display-accessibility-evidence.json` | Read-only AT-SPI observation for the post-open runtime/display accessibility scenario, with `status`, `postOpenRuntimeDisplayAccessibilityObserved`, runtime/display candidates, and exact blocker fields. |
| `screenshot.png` or `screenshot.xwd` | Captured desktop state. |
| `manual-evidence-checklist.txt` | Repeatable checklist for manual scenarios. |
| `command.log` | Captured stdout/stderr for gated command smokes when `ALICE_QA_RUN_GATED_SMOKES=1` is set. |
| `review-notes.txt` | Human acceptance notes for manual scenarios, including reviewed artifacts, observed result, deviations, and accept/reject decision. |

Generated evidence is ignored by Git. Commit scenario definitions, schema changes, runner changes, and documentation; do not commit local evidence artifacts.

Accept a run only when the generated artifacts agree with the expected automation mode and the listed evidence artifacts are present. For successful Xvfb launch runs, check `status.txt`. For early Xvfb fallback directories, review the fallback checklist and available diagnostics instead of expecting the full launch artifact set; for post-open runtime/display accessibility, also check the blocked JSON/status artifacts because that scenario emits machine-readable blockers for early prerequisites. For manual scenarios, `status.txt` records checklist generation; it is not a pass result until a human adds the required artifacts and `review-notes.txt`.

## Troubleshooting

### Missing Tweedle parser classes

Initialize the Tweedle grammar submodule in the current checkout or worktree:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
git submodule update --init tweedle-lang
```

### Xvfb is unavailable

Install Xvfb for the local environment, or keep the generated manual fallback checklist and collect launch evidence from a supported desktop environment. Early Xvfb fallback directories may include only `environment.txt` and `manual-evidence-checklist.txt`; that is a failed automated launch attempt, not accepted launch evidence.

### No Alice window is detected

Check the run directory:

```bash
sed -n '1,120p' qa/outside-in/alice-desktop/evidence/alice-desktop-launch/*/status.txt
python3 -m json.tool qa/outside-in/alice-desktop/evidence/alice-desktop-launch/*/x-window-inventory.json
sed -n '1,160p' qa/outside-in/alice-desktop/evidence/alice-desktop-launch/*/launch.log
```

If renderer initialization fails, preserve the logs and provide a manual launch screenshot as fallback evidence.

### Post-open runtime/display accessibility is blocked

Open the decision artifact first:

```bash
python3 -m json.tool \
  qa/outside-in/alice-desktop/evidence/post-open-runtime-display/alice-desktop-post-open-runtime-display-accessibility-evidence/*/post-open-runtime-display-accessibility-evidence.json
```

Blocker values include `x-server-unavailable`, `display-allocation-unavailable`, `pyatspi-not-installed`, `at-spi-registry-unavailable`, `atk-wrapper-not-loaded`, `post-open-window-not-observed`, `runtime-display-accessible-candidate-not-found`, `java-pid-not-in-inventory`, and `input-unreadable`. Treat a blocker as a precise gap report, not a pass. Do not replace it with a manual rendering claim.
