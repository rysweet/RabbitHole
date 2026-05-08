# Run Alice desktop outside-in QA

Use the Alice desktop outside-in QA lane to validate the scenario catalog and collect reviewable evidence for user-like workflows: launch, Select Project inventory, instructor/student setup, scene creation, run/debug-like behavior, save/load, open/load/save, export, exported-project smoke, NetBeans package smoke, package/install smoke, saving, reopening, editing, saving again, reopening again, and exporting Alice projects, failure-path smoke, future UI smoke, menu/action smoke, wizard/palette/completion smoke, and post-open runtime/display accessibility evidence.

## Contents

- [Prerequisites](#prerequisites)
- [Validate the scenario catalog](#validate-the-scenario-catalog)
- [Validate a custom scenario catalog](#validate-a-custom-scenario-catalog)
- [List available scenarios](#list-available-scenarios)
- [Run branch-installable checks with uvx](#run-branch-installable-checks-with-uvx)
- [Run the real Alice launch scenario](#run-the-real-alice-launch-scenario)
- [Collect post-open runtime/display accessibility evidence](#collect-post-open-runtimedisplay-accessibility-evidence)
- [Prepare evidence for manual workflows](#prepare-evidence-for-manual-workflows)
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
uvx --from git+https://github.com/rysweet/alice3-modernization.git@feat/alice-qa-outside-in \
  amplihack alice-qa list

uvx --from git+https://github.com/rysweet/alice3-modernization.git@feat/alice-qa-outside-in \
  amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Run these commands from the root of a checkout of the same branch. The installed wrapper delegates to `qa/outside-in/alice-desktop/runners/` in that checkout so the output and evidence contract match direct runner usage.

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

A successful launch evidence capture includes `root-directory-prep.json`, `license-acceptance.json`, `license-dialog.json`, an environment summary, Xvfb log, Alice launch log, status file, screenshot, and `x-window-inventory.json`. The window inventory records visible X window title, class, process, and geometry after the readiness wait so a blocked run names the exact window signal that was or was not present. The license artifacts record the exact first-run dialog title/controls when observed, or the isolated `java.util.prefs.userRoot` state files under `.java/.userPrefs/` used when `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` is set. The runner checks root-directory preparation, first-run license state, process, window-readiness, and screenshot-capture status; it does not deeply classify every line in `launch.log` as a semantic pass/fail oracle. Review `root-directory-prep.json`, `license-acceptance.json`, `license-dialog.json`, `status.txt`, `x-window-inventory.json`, `launch.log`, and the screenshot before treating the launch evidence as accepted.

If Xvfb is unavailable, no display can be selected, or Xvfb exits before Alice starts, the runner exits non-zero and writes a manual fallback checklist with whichever early diagnostics are available. These early fallback directories may not contain `status.txt` because the launch did not reach the evidence-capture phase.

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

## Collect post-open runtime/display accessibility evidence

The `alice-desktop-post-open-runtime-display-accessibility-evidence` scenario collects evidence for one narrow post-open runtime/display signal. It reuses the supported Alice launch, isolated license acceptance, Xvfb, ATK wrapper, and project-open setup, then runs a read-only AT-SPI probe against the live Alice accessibility tree. The claim is limited to accessibility-visible runtime/display state after a project is open; it is not a full rendering, world execution, grading, lesson completion, Save, Select Project, installer, or decoder proof.

Command from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display
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
post-open-runtime-display-accessibility-evidence.json
status.txt
screenshot.png or screenshot.xwd
```

`post-open-runtime-display-accessibility-evidence.json` is the decision artifact. A passing observation has this shape:

```json
{
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "postOpenRuntimeDisplayAccessibilityObserved": true,
  "runtimeDisplayCandidateCount": 1,
  "runtimeDisplayCandidates": [
    {
      "role": "canvas",
      "name": "Scene display",
      "childCount": 0,
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "blocker": "none",
  "blockerDetail": ""
}
```

If an implementation or environment prerequisite is missing, the runner still writes the same JSON file and records a machine-readable blocker instead of passing:

```json
{
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "blocked",
  "claim": "post-open-runtime-display-accessibility-evidence",
  "postOpenRuntimeDisplayAccessibilityObserved": false,
  "runtimeDisplayCandidateCount": 0,
  "runtimeDisplayCandidates": [],
  "blocker": "pyatspi-not-installed",
  "blockerDetail": "python3-pyatspi is not installed."
}
```

Use `status.txt` for automation and `post-open-runtime-display-accessibility-evidence.json` for detailed review. `status.txt` records the scenario ID, automation mode, launch display when available, `runtimeDisplayAccessibilityEvidence=post-open-runtime-display-accessibility-evidence.json`, `runtimeDisplayAccessibilityStatus`, `runtimeDisplayAccessibilityBlocker`, and `outcome=passed` or `outcome=blocked`.

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
| `x-window-inventory.json` | Visible X window title, class, process, and geometry captured after launch readiness wait, or an explicit unsupported/blocker record. |
| `select-project-window.json` | Select Project proof artifact recording exact title/class/process/geometry when observed, or an exact missing-window/widget-introspection blocker. |
| `post-open-runtime-display-accessibility-evidence.json` | Read-only AT-SPI observation for the post-open runtime/display accessibility scenario, with `status`, `postOpenRuntimeDisplayAccessibilityObserved`, runtime/display candidates, and exact blocker fields. |
| `screenshot.png` or `screenshot.xwd` | Captured desktop state. |
| `manual-evidence-checklist.txt` | Repeatable checklist for manual scenarios. |
| `command.log` | Captured stdout/stderr for gated command smokes when `ALICE_QA_RUN_GATED_SMOKES=1` is set. |
| `review-notes.txt` | Human acceptance notes for manual scenarios, including reviewed artifacts, observed result, deviations, and accept/reject decision. |

Generated evidence is ignored by Git. Commit scenario definitions, schema changes, runner changes, and documentation; do not commit local evidence artifacts.

Accept a run only when the generated artifacts agree with the expected automation mode and the listed evidence artifacts are present. For successful Xvfb launch runs, check `status.txt`. For early Xvfb fallback directories, review the fallback checklist and available diagnostics instead of expecting the full launch artifact set. For manual scenarios, `status.txt` records checklist generation; it is not a pass result until a human adds the required artifacts and `review-notes.txt`.

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
