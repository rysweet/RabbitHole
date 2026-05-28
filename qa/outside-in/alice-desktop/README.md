# Alice desktop outside-in QA

This lane defines executable acceptance coverage for Alice desktop workflows without changing product modules. It keeps scenario intent, execution wrappers, and evidence requirements in one repo-owned QA area.

For user-facing instructions, see Run Alice desktop outside-in QA. For current exported-project smoke behavior and the target exported Ant/NetBeans project build proof, see Exported NetBeans Ant Project Behavior. For the post-open runtime/display evidence contract, see Post-open runtime/display accessibility evidence. For the desktop Run execution gap report, see Desktop Run execution gap report. For the target-specific Select Project starter path, see Open Africa Full through Select Project with AT-SPI and the Select Project Africa Full AT-SPI evidence reference. For the live first-lesson procedure/code-editor target seam, see First-Lesson Live Procedure Target Observation. For the learner-world setup/open/save assessment boundary, see Learner-world assessment boundary. For the complete scenario schema and runner interface, see the Alice desktop outside-in QA reference.
For user-facing instructions, see Run Alice desktop outside-in QA. For the Run-window creation/wiring evidence contract and artifact API, see Run-Window Creation/Wiring Contract. For current exported-project smoke behavior and the target exported Ant/NetBeans project build proof, see Exported NetBeans Ant Project Behavior. For the post-open runtime/display evidence contract, see Post-open runtime/display accessibility evidence. For the target-specific Select Project starter path, see Open Africa Full through Select Project with AT-SPI and the Select Project Africa Full AT-SPI evidence reference. For the live first-lesson procedure/code-editor target seam, see First-Lesson Live Procedure Target Observation. For the learner-world setup/open/save assessment boundary, see Learner-world assessment boundary. For the Gadugi Run-window contract evidence scenario, see Gadugi run-window contract evidence. For the complete scenario schema and runner interface, see the Alice desktop outside-in QA reference.
For user-facing instructions, see Run Alice desktop outside-in QA. For current exported-project smoke behavior and the target exported Ant/NetBeans project build proof, see Exported NetBeans Ant Project Behavior. For the post-open runtime/display evidence contract, see Post-open runtime/display accessibility evidence. For the target-specific Select Project starter path, see Open Africa Full through Select Project with AT-SPI and the Select Project Africa Full AT-SPI evidence reference. For the live first-lesson procedure/code-editor target seam, see First-Lesson Live Procedure Target Observation. For the learner-world setup/open/save assessment boundary, see Learner-world assessment boundary. For the Gadugi Window menu registration evidence scenario, see Gadugi Window menu registration evidence. For the complete scenario schema and runner interface, see the Alice desktop outside-in QA reference.
For user-facing instructions, see Run Alice desktop outside-in QA. For current exported-project smoke behavior and the target exported Ant/NetBeans project build proof, see Exported NetBeans Ant Project Behavior. For the post-open runtime/display evidence contract, see Post-open runtime/display accessibility evidence. For the focused launch, run/runtime, and Select Project target-discovery contract, see Accessibility Target Discovery Silver-Thread Contract. For the target-specific Select Project starter path, see Open Africa Full through Select Project with AT-SPI and the Select Project Africa Full AT-SPI evidence reference. For the live first-lesson procedure/code-editor target seam, see First-Lesson Live Procedure Target Observation. For the learner-world setup/open/save assessment boundary, see Learner-world assessment boundary. For the complete scenario schema and runner interface, see the Alice desktop outside-in QA reference.

## What belongs here

| Area | Owns | Does not own |
| --- | --- | --- |
| `scenarios/` | User-like workflows, expected outcomes, evidence requirements, automation mode | Java implementation details or brittle internal UI assumptions |
| `contracts/` | Declarative claim-boundary records, including the learner-world assessment blocker | Runner behavior or assessment implementation |
| `gadugi/` | Gadugi-compatible CLI scenarios for agentic QA tools, including exported launcher evidence contract checks and Select Project tab-click evidence contract checks (PR #437) | The custom Alice scenario schema or full desktop/rendering claims |
| `gadugi/` | Gadugi-compatible CLI scenarios for agentic QA tools, including exported launcher, run-render-affordance, and Run-window contract evidence checks | The custom Alice scenario schema or full desktop/rendering claims |
| `gadugi/` | Gadugi-compatible CLI scenarios for agentic QA tools, including exported launcher evidence and Window menu registration evidence contract checks | The custom Alice scenario schema or full desktop/rendering claims |
| `schema/` | Scenario structure and allowed field values | Business logic |
| `runners/` | Thin wrappers around existing Maven/Alice commands | New build systems, hidden dependencies, or product behavior changes |
| `evidence/` | Local run artifacts produced by the runner | Source-controlled product assets |

Generated evidence is ignored by Git. Commit only scenario definitions, schema changes, runner changes, and the evidence `.gitignore`.

## Scenario model

Each scenario uses the same required fields:

- `id`
- `name` — human-readable scenario name, always equal to `title`. Required by both the repo-owned validator and `gadugi-test validate`.
- `title`
- `workflow`
- `automationMode`
- `preconditions`
- `userActions`
- `expectedOutcomes`
- `evidence.required`
- `fallback`

Every scenario also carries two gadugi-test compatibility fields:

- `steps` — gadugi-test step list. Currently `["validate"]` for all scenarios.
- `agents` — gadugi-test agent list. Currently `["alice-desktop-qa"]` for all scenarios.

The `name` field is enforced by the JSON schema (`required`), the repo-owned `validate-scenarios.sh` (`required_top`), and a Python contract test suite. If `name` is present but does not equal `title`, the validator rejects the scenario. These fields let `gadugi-test validate scenarios/` pass against the canonical gadugi-test schema while the repo-owned `validate-scenarios.sh` remains the primary structural validator.

The target-specific Select Project scenario uses `targetStarter.displayName` and `targetStarter.repositoryPath` to bind AT-SPI evidence to a committed starter project instead of a generic chooser dismissal.

## Learner-world boundary

RabbitHole learner-world QA currently supports setup/open/save evidence review only.
The `alice-desktop-instructor-student-setup` scenario lets a reviewer collect
instructor starter-project setup evidence, student open evidence, and student
save evidence. It is a manual evidence workflow; checklist generation is not a
pass result and does not evaluate the learner's work.

The checked-in boundary record is
`qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json`.
That file is declarative documentation for the current claim boundary. It names
next boundary `define-reviewed-assessment-contract` and supplies the generated
manual status/checklist boundary wording.

The generated `manual-evidence-checklist.txt` for
`alice-desktop-instructor-student-setup` uses the standard manual checklist
sections and includes a generated `Assessment boundary` section. That section
states manual evidence required, setup/open/save evidence review only, no
automated grading, no rubric scoring, no correctness assessment, no creative
assessment, and next boundary `define-reviewed-assessment-contract`. It also
adds explicit manual/unsupported checklist lines and keeps learner-world
grading, rubric scoring, correctness assessment, and creative assessment
manual/unsupported until `define-reviewed-assessment-contract` is resolved with
a reviewed assessment contract, evidence mapping, and reviewed implementation.
The generated `status.txt` repeats the same boundary as plain status fields,
including `assessmentBoundary=define-reviewed-assessment-contract` and
`assessmentBoundaryMode=manual/unsupported`.

Do not use learner-world QA evidence to claim learner-world grading, rubric
scoring, correctness assessment, or creative assessment. Any future
assessment capability first needs a reviewed assessment contract, evidence
mapping, privacy and audit controls, and a separate implementation change.

Allowed `automationMode` values are:

| Mode | Meaning |
| --- | --- |
| `xvfb-real-alice` | Attempts to run the real Alice desktop under Xvfb and captures logs/screenshots. |
| `manual-evidence-required` | Produces an executable checklist with required evidence, but does not automate GUI interaction or mark the scenario complete. |
| `gated-command-smoke` | Produces status/checklist evidence by default; executes the configured CLI smoke only when `ALICE_QA_RUN_GATED_SMOKES=1`, including focused launch-adjacent menu/action plumbing tests. |

Do not use Playwright here unless Alice later exposes a browser/web UI.

Scenario YAML intentionally uses a strict subset: simple mappings, nested mappings, scalar values, and scalar lists. Do not use anchors, aliases, tags, multiline scalars, flow-style collections, or tabs for indentation. The JSON Schema is the published contract; the dependency-free validator must stay in parity with it.

## Commands

Run all commands from the repository root.

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --list
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json
qa/outside-in/alice-desktop/runners/run-scenario.sh list
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
qa/outside-in/alice-desktop/runners/run-scenario.sh run qa/outside-in/alice-desktop/scenarios/launch.yaml
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> amplihack alice-qa save-negative-contract
cd qa/outside-in/alice-desktop && gadugi-test validate scenarios/
```

Use one of the supported workflows:

```text
archive-fixture-smoke
export
exported-project-ant-build-smoke
failure-path-smoke
file-loader-smoke
first-lesson-live-procedure-target-observation
future-ui-smoke
generated-listener-runtime-dispatch-smoke
instructor-student-setup
launch
menu-action-smoke
netbeans-package-smoke
open-load-save
package-install-smoke
post-open-runtime-display-accessibility-evidence
post-project-open-window-state-smoke
procedure-edit-handoff-smoke
procedure-edit-seam-smoke
project-io-smoke
runtime-event-dispatch-smoke
run-debug
run-window-contract
save-load
save-menu-dialog-write-proof
save-negative-artifact-contract
scene-creation
select-project-atk-exec-smoke
select-project-interaction-smoke
select-project-tab-click-smoke
select-project-widget-introspection-smoke
silver-thread-launch-build-run
tweedle-decoder-boundary-smoke
tweedle-decoder-this-call-smoke
wizard-palette-completion-smoke
```

The Run-window creation/wiring contract is a supported bounded workflow.
Reviewers prepare it with:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

The default Run-window contract path is a `gated-command-smoke` preflight. It
writes `status.txt` with `outcome=gated-not-run` and a
`manual-evidence-checklist.txt` that names the fixed `run-window-created.json`
artifact. When `ALICE_QA_RUN_GATED_SMOKES=1` is set, the same scenario runs the
focused `EatmeRunWindowEvidenceTest` Maven command through the exact allowlisted
argv, injects the scenario evidence directory, and validates the resulting
`run-window-created.json`. That command verifies only the creation and wiring
metadata seam. It does not claim active rendering, run execution, world
execution correctness, rendering correctness, Save behavior, grading, creative
assessment, lesson completion, or full UI automation.
### Menu/action contract smoke

`alice-desktop-menu-action-smoke` is the outside-in entry point for the bounded
Window menu model contract. It is a gated command smoke that runs
`AliceMenuBarContractTest`; it does not drive a live Swing menu, inspect rendered
pixels, complete Save, complete the first lesson, validate a deployed installer,
or validate Sims.

Prepare the smoke without executing Maven:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/menu-action
```

Execute the focused command:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-menu-action-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/menu-action
```

With `NODE_OPTIONS` set in the environment, the checked-in scenario argv is
equivalent to:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

Accepted evidence is limited to `status.txt`, `command.log`, and test output
naming `AliceMenuBarContractTest`. The claim is only that the Window menu model
is registered and reachable through menu-bar membership lookup.
To run the silver thread end-to-end create→build→save→reopen→run smoke:

```bash
git submodule update --init tweedle-lang
export NODE_OPTIONS=--max-old-space-size=32768

ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-silver-thread-launch-build-run \
  --evidence-dir qa/outside-in/alice-desktop/evidence/silver-thread
```

The scenario delegates to `SilverThreadLaunchBuildRunTest` which proves the core
student journey headlessly: create a project, add a statement, save, reopen,
execute through the virtual machine, verify execution via listener events, and
verify round-trip fidelity. It also loads a real starter project, saves a copy,
and verifies the copy reopens with program type fidelity. No display server,
JavaFX toolkit, or gallery assets are needed. See the Silver Thread
Launch-Build-Run Test reference
for the full behavior contract and claim boundaries.

To observe only the live first-lesson procedure/code-editor target after Select
Project opens the configured starter:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir qa/outside-in/alice-desktop/evidence/first-lesson-procedure-target \
  --timeout-seconds 300
```

Review `first-lesson-live-procedure-target-observation.json` as the
edit-ready-or-named-blocker action-seam artifact for the narrow
`scene.eatmeFirstLesson` procedure tab/code-editor target. The shard is
read-only; it does not edit the procedure, Save, prove rendering correctness,
assess learner work, or claim full first-lesson completion.

`run-scenario.sh run` accepts either a scenario ID or a direct `.yaml` file inside the active scenario catalog. Use `--evidence-dir <dir>` to write evidence outside the repository, `--timeout-seconds <seconds>` to override argv-backed launch timeout, and `--prepare-only` to intentionally prepare gated smoke evidence without executing the gated command.

To execute the exported Ant project build smoke:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-exported-project-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/exported-project-smoke
```

The checked-in scenario maps to the focused no-Sims Ant/template Maven smoke:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl netbeans -am \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest \
  test
```

Passing evidence is limited to exported Ant build/runtime metadata behavior:
generated classes, exported jar and manifest contents, resource packaging,
runtime JVM metadata propagation up to the GUI launch boundary, Ant `jar`,
`run`, `run-test-with-main`, `clean`, and probe markers. It is not installer
validation, full GUI export journey coverage, visible rendering evidence, Sims
coverage, or grading behavior. If target execution cannot complete, preserve
`status.txt` and `command.log` with the exact command, failure point, and missing
condition.

To collect the narrow post-open runtime/display accessibility evidence:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

Review `post-open-runtime-display-accessibility-evidence.json` as the
implemented runtime/display decision artifact. Review
`controlled-display-pixel-observation.json` as the controlled-display
screenshot-consistency artifact. `worldCanvasPixelTarget.status=target-ready`
means exactly one visible/showing runtime/display candidate exposed valid
screen-coordinate extents for target-scoped sampling. It does not mean visible
rendering correctness is established. The runner attempts target-scoped raw
pixel sampling only after that target is valid. A bounded
`visible-rendering-pixel-observation.json` records checked RGBA samples with
`visibleRenderingCorrectnessEstablished=false`; otherwise
`visible-rendering-pixel-sampling-blocker.json` records the exact fail-closed
target or sampler blocker with `renderedWorldPixelsObserved=false`.
`status=blocked` means
`visible-rendering-pixel-target-blocker.json` names the exact missing target and
next unblocker. `geometryStatus=ambiguous-candidates` is reserved for more than
one visible/showing candidate with valid positive screen-coordinate extents; a
larger raw candidate count with missing, zero, negative, malformed, or otherwise
invalid geometry stays fail-closed without being reported as ambiguous.
`tab-click-observation.json` and
`post-project-open-observation.json` are supporting setup artifacts. An observed
result is limited to a live post-open runtime/display accessibility signal,
controlled-display screenshot consistency, and controlled target identification
and/or target-scoped raw pixel sampling. It does not prove world-canvas pixel
correctness, visible rendering correctness, deployed installer success, full
world execution, grading, lesson completion, active Save behavior, active Select
Project behavior, or decoder behavior.

To collect only the target-specific Select Project proof for the committed
`Africa Full` starter:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Review `tab-click-observation.json` as the Select Project decision artifact.
The target contract for an opened result requires `evidenceStatus=opened`,
exact `Africa Full` `targetStarter` metadata, safe `startersTabSafety`
(`activatedBeforeTargetSearch=true`, `targetSearchScope=active-starters-tab`),
`targetSelectionObserved=true`, `openAttempted=true`, and
`projectOpenObserved=true`. A blocked result preserves string `blocker` and
`blockerDetail` fields and adds one structured `nextBlocker`; it is not a full
Alice UI automation, visible rendering, grading, creative assessment, Save,
first-lesson, launcher, or decoder claim.

To validate the focused launch, run/runtime, and Select Project accessibility
target discovery silver thread without launching Alice or claiming full UI
automation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
```

The contract checks scenario metadata, runner/probe target discovery markers,
structured blocker fields, and bounded scope wording. It validates only the
narrow target discovery evidence path and does not prove visual correctness,
rendering correctness, world execution correctness, full world execution, or
general accessibility compliance.

The Gadugi exported launcher evidence scenario is a separate CLI scenario under
`gadugi/`, not a custom Alice scenario under `scenarios/`. Validate and run it
with `gadugi-test` installed on `PATH`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s exported-launcher-evidence \
  --timeout 300000
```

That Gadugi scenario delegates to the exported-project smoke runner in
prepare-only mode by default and writes evidence under
`qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher`.
`ALICE_QA_RUN_GATED_SMOKES=1` only applies when the underlying Alice runner is
invoked without `--prepare-only`. The Gadugi lane validates launcher evidence
wiring and JavaFX handoff/no-go checks only; it does not prove visible
rendering, save behavior, grading, creative assessment, or full lesson
completion.

The Gadugi select-project tab-click evidence scenario (PR #437) validates the
target-specific Select Project tab-click evidence wiring. Validate and run it
with `gadugi-test`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/select-project-tab-click-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s select-project-tab-click-evidence \
  --timeout 300000
```

That Gadugi scenario delegates to the Select Project tab-click smoke runner in
prepare-only mode by default and writes evidence under
`qa/outside-in/alice-desktop/evidence/gadugi-select-project-tab-click`. The
Gadugi lane validates Select Project tab-click evidence wiring and AT-SPI tab
enumeration/selection contract only; it does not prove visible rendering, AT-SPI
target opening, full project interaction, grading, creative assessment, Save
behavior, first-lesson completion, or the full Alice desktop workflow.
For branch- or commit-installable outside-in checks, run the thin `amplihack` wrapper from the checkout under review:
The Gadugi Run-window contract evidence scenario is also under `gadugi/`.
Validate and run it with `gadugi-test` installed on `PATH`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/run-window-contract-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s run-window-contract-evidence \
  --timeout 300000
```

That Gadugi scenario delegates to the `alice-desktop-run-window-contract` runner
in prepare-only mode by default and writes evidence under
`qa/outside-in/alice-desktop/evidence/gadugi-run-window-contract`.
`ALICE_QA_RUN_GATED_SMOKES=1` only applies when the underlying Alice runner is
invoked without `--prepare-only`. The Gadugi lane validates Run-window
creation/wiring evidence only; it does not prove active rendering, run
execution, world execution correctness, rendering correctness, Save behavior,
grading, creative assessment, lesson completion, or full UI automation. For the
full reference, see
Gadugi run-window contract evidence scenario.

For branch-installable outside-in checks, run the thin `amplihack` wrapper from a checkout of the branch:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> amplihack alice-qa list
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> amplihack alice-qa save-negative-contract
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Replace `<branch-or-commit>` with the PR branch or commit you are reviewing. The wrapper
delegates to the same repo-owned runners and intentionally requires an Alice
checkout as the current working tree.

The launch scenario first verifies the Alice exec root-directory property and
prepares the distribution root if it is missing:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests -pl core/resources process-resources
```

It then uses the Alice desktop Maven path with an explicit compile step before
`exec:java`, so `org.alice.stageide.EntryPoint` is present in
`alice-ide/target/classes` and the JVM sees
`org.alice.ide.rootDirectory=../core/resources/target/distribution` from
`alice-ide/pom.xml`:

```bash
cd alice-ide
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests compile exec:java -Dalice-ide
```

Scenario automation stores executable steps as argv lists, not shell command strings. The validator and runner allow only the checked-in Alice QA argv set, including custom catalogs selected with `ALICE_QA_SCENARIO_DIR`.

The runner records evidence under
`qa/outside-in/alice-desktop/evidence/<scenario-id>/<timestamp>/`.

| Artifact | Records |
| --- | --- |
| `root-directory-prep.json` | Whether `core/resources/target/distribution` was already present or prepared with Maven phase `process-resources`; blocked cases name the missing property, distribution path, or Maven failure. |
| Environment summary, Xvfb log, Alice launch log, and status file | Launch environment, display setup, process output, and final scenario outcome. |
| Screenshot (`screenshot.png` or `screenshot.xwd`) and optional screenshot pixel stats | Controlled-display capture output. |
| `x-window-inventory.json` | Alice-related visible X window title, class, process, and geometry after the readiness wait; unrelated visible desktop windows are not written to the JSON artifact. |
| `application-root-error.json` | Exact `Application Root Error` window blocker, observed JVM `org.alice.ide.rootDirectory` condition, expected dialog text, and next invocation change. |
| `license-dialog.json` and `license-acceptance.json` | Exact first-run License Agreement blocker details or the isolated `java.util.prefs.userRoot` state files under `.java/.userPrefs/` prepared when `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` is set. |
| `select-project-window.json` | Exact `Select Project` title, class, process, and geometry; widget labels remain resource-contract evidence until a live Swing accessibility/Jemmy probe exists. |
| `controlled-display-pixel-observation.json` | Controlled-display screenshot-consistency artifact with `schemaVersion=1`, `claimScope=controlled-display-screenshot-consistency`, relative screenshot path when captured, screenshot dimensions when metadata is available, pixel-observation metadata, `worldCanvasPixelTarget`, and explicit unsupported claims. |
| `visible-rendering-pixel-target-blocker.json` | Machine-readable blocker for world-canvas pixel target readiness when the Run-window target is missing, invalid, or ambiguous. |
| `visible-rendering-pixel-observation.json` | Bounded target-scoped raw pixel observation, written only after exactly one visible/showing Run-window/world-canvas target has valid positive screen-coordinate extents and the sampler returns checked raw RGBA samples inside that target. |
| `visible-rendering-pixel-sampling-blocker.json` | Machine-readable blocker for the sampling seam when the target is not ready, the sampler is unavailable, sampling fails, or returned pixels are incomplete, malformed, or unchecked. |

The controlled-display screenshot-consistency artifact does not assert Alice
world rendering correctness. The pixel-sampling seam is fail-closed: it writes
`visible-rendering-pixel-observation.json` only for checked raw RGBA samples
inside the validated target, and otherwise writes
`visible-rendering-pixel-sampling-blocker.json` with
`renderedWorldPixelsObserved=false`. The pixel observation artifact also does
not assert correctness; it preserves
`visibleRenderingCorrectnessEstablished=false` while recording only coordinates
and raw RGBA values sampled inside the validated target.
The runner treats `controlled-display-pixel-observation.json` as the fixed source
artifact for sampling. Target-ready metadata from the source artifact is only a
sampling prerequisite; missing, malformed, non-object, differently named,
semantically invalid, blocked, ambiguous, hidden, or invalid target data writes
`visible-rendering-pixel-sampling-blocker.json` rather than a success-shaped
observation.

The `alice-desktop-post-open-runtime-display-accessibility-evidence` scenario
goes one step beyond launch, window, and pixel evidence. It uses the existing
Xvfb and AT-SPI setup, reuses the supported project-open setup, then runs the
read-only `post-open-runtime-display-probe.py` probe against the live Alice
accessibility tree.

| Artifact | Role |
| --- | --- |
| `post-open-runtime-display-accessibility-evidence.json` | Runtime/display accessibility decision artifact. Observed requires `status=observed`, `postOpenRuntimeDisplayAccessibilityObserved=true`, at least one runtime/display candidate, and `blocker=none`. |
| `status.txt` | Final scenario status. Pass requires `outcome=passed`, `runtimeDisplayAccessibilityStatus=observed`, `controlledDisplayPixelStatus=observed`, `visibleRenderingPixelSamplingStatus=observed`, and `visibleRenderingCorrectnessEstablished=false`. |
| `runtime-display-accessibility-status.txt` | Probe-local status written before final scenario status; useful for debugging, not the final pass/fail artifact. |
| `tab-click-observation.json` and `post-project-open-observation.json` | Supporting project-open setup artifacts. |
| `controlled-display-pixel-observation.json` | Controlled-display screenshot-consistency artifact with screenshot path, dimensions when available, pixel-observation metadata, target-ready or blocked `worldCanvasPixelTarget`, and unsupported claims. Pixel blockers keep final `outcome=blocked`. |
| `visible-rendering-pixel-target-blocker.json` | Blocker artifact naming the exact next unblocker for missing, invalid, or ambiguous world-canvas pixel target readiness. Ambiguous means more than one visible/showing candidate has valid extents, not merely more than one raw candidate. |
| `visible-rendering-pixel-observation.json` | Observation artifact for the sampling seam after target readiness. It cites `controlled-display-pixel-observation.json` as `sourceArtifact`, preserves the validated target geometry, records sample points and raw RGBA values, and keeps `visibleRenderingCorrectnessEstablished=false`. |
| `visible-rendering-pixel-sampling-blocker.json` | Blocker artifact for the sampling seam when target validation or sampling cannot support the bounded observation. Artifacts cite `controlled-display-pixel-observation.json` as `sourceArtifact`, preserve `prerequisiteTargetStatus`, name the exact target/sampler blocker, and keep `renderedWorldPixelsObserved=false`. |

If Xvfb, display allocation/startup, root-directory prep, license prep, AT-SPI,
`python3-pyatspi`, the Java ATK wrapper, screenshot/pixel capture, screenshot
metadata, project-open setup, or the runtime/display candidate is unavailable,
the JSON/status artifacts record `status=blocked` or `outcome=blocked` plus
precise blocker fields; the runner must not silently pass. Target-ready metadata
is emitted only when one visible/showing candidate has valid positive
screen-coordinate extents; otherwise the exact blocker artifact is preserved.
This evidence supports only a live post-open runtime/display accessibility
signal, controlled-display screenshot consistency, controlled target
identification, and target-scoped raw pixel sampling or a precise sampling blocker. It does not prove
world-canvas pixel correctness, visible rendering correctness, deployed installer
success, full world execution, grading, lesson completion, active Save behavior,
active Select Project behavior, or decoder behavior. The stable artifact API,
target readiness and sampling contracts, configuration, examples, and review
rules are documented in Post-open
runtime/display accessibility evidence.

The first-lesson live procedure target action seam goes one step beyond the
Select Project first-lesson open path by observing whether the live post-open
desktop exposes a stable procedure tab or code-editor target for
`scene.eatmeFirstLesson`, then writing
`first-lesson-live-procedure-target-observation.json` with `status=edit-ready`
or the exact no-go blocker
`blocker.kind=missing-desktop-edit-action-contract`. Display, AT-SPI, and
target-not-found blockers are structured run failures, not accepted action-seam
proof. The shard must not mutate a procedure, Save, assert rendering
correctness, assess learner work, or claim full first-lesson completion. The
artifact API, configuration, examples, and review rules are
documented in First-Lesson Live Procedure Target
Observation.

The opt-in desktop Run evidence hook writes
`desktop-run-execution-gap-report.json` after the existing Run-window artifact
writers complete their non-empty checks, including
`desktop-run-status-summary.json`. The report states that the executable
evidence in this lane is bounded Run-window evidence and names the blocker to a
stronger execution claim: missing deterministic proof that the Alice world
advances through full runtime execution rather than merely producing Run-window
artifacts. Missing required artifact references, missing blocker text, or
missing non-claim categories fail validation for the report while preserving
normal Run behavior. The report does not prove full world execution, playback,
visible rendering correctness, full UI automation, Save completion, grading,
Sims validation, or deployed installer success. The report payload enforces the
implementation-backed `doesNotClaim` tokens documented in the reference; the
playback, Sims, and installer limits are review-language boundaries. See
Desktop Run execution gap
report.

To prepare the outside-in review container for this evidence family, generate
the manual `run-debug` checklist:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-debug \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-debug
```

The generated checklist is not a pass result. Reviewers place or reference the
supporting VM-listener artifacts (`desktop-run-execution.json` and
`desktop-run-runtime.log`) when opt-in desktop Run execution evidence is enabled,
the bounded Run-window artifacts named by the gap report
(`desktop-run-render-affordance.json`, `desktop-run-pixel-boundary.json`,
`desktop-run-pixel-observation.json`,
`desktop-first-lesson-next-action.json`,
`desktop-save-menu-action-target.json`, and
`desktop-run-status-summary.json`), the gap report, the run log,
workflow-context screenshots or screen captures, the saved `.a3p` used for the
run, and `review-notes.txt` in the timestamped directory. If artifacts are not
copied into the manual run directory, `review-notes.txt` must identify their
exact evidence location. Acceptance language must stay limited to bounded
Run-window evidence and the deterministic world-advance proof blocker.

Early Xvfb fallback directories may contain only the diagnostics available before launch plus a manual fallback checklist. For manual scenarios, the runner creates a status file and structured checklist so the workflow is repeatable and reviewable; the scenario is complete only after a human performs the workflow and adds the required evidence artifacts plus `review-notes.txt`. For gated command smokes, an unset gate records `outcome=gated-not-run` and exits non-zero; pass `--prepare-only` for intentional preflight/checklist preparation, or set `ALICE_QA_RUN_GATED_SMOKES=1` only in a worktree prepared for the configured Maven or display-backed argv.

## Configuration

| Variable | Purpose |
| --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Override the active scenario catalog. |
| `ALICE_QA_DISPLAY` | Reuse a specific X display for Xvfb runs. |
| `ALICE_QA_SCREEN` | Set Xvfb screen geometry. Defaults to `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Override launch readiness wait before screenshot capture. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | Set to `1` only for controlled QA launches that need to bypass first-run License Agreement dialogs with an isolated Java Preferences user root. |
| `ALICE_QA_WORLD_CANVAS_PIXEL_SAMPLER` | Override the target-scoped sampler path for contracts; defaults to `runners/world-canvas-pixel-sampler.py`. |
| `ALICE_QA_RUN_GATED_SMOKES` | Execute gated command smoke scenarios when set to `1`; otherwise they write `gated-not-run` evidence and exit non-zero unless `--prepare-only` is requested. |
| `NODE_OPTIONS` | Optional for surrounding Node-based orchestrators. Use `--max-old-space-size=32768` when needed; this lane itself does not require Node. |

The Select Project Africa Full scenario passes `targetStarter.displayName` and `targetStarter.repositoryPath` to the AT-SPI probe as `TARGET_STARTER_DISPLAY_NAME` and `TARGET_STARTER_REPO_PATH`. These variables are runner-managed evidence metadata, not user configuration knobs.

## Scenario authoring checklist

Use one of the supported workflows:

```text
archive-fixture-smoke
export
exported-project-ant-build-smoke
failure-path-smoke
file-loader-smoke
future-ui-smoke
generated-listener-runtime-dispatch-smoke
instructor-student-setup
launch
menu-action-smoke
netbeans-package-smoke
open-load-save
package-install-smoke
post-open-runtime-display-accessibility-evidence
post-project-open-window-state-smoke
procedure-edit-handoff-smoke
procedure-edit-seam-smoke
project-io-smoke
runtime-event-dispatch-smoke
run-debug
run-window-contract
save-load
save-menu-dialog-write-proof
save-negative-artifact-contract
scene-creation
select-project-atk-exec-smoke
select-project-interaction-smoke
select-project-tab-click-smoke
select-project-widget-introspection-smoke
tweedle-decoder-boundary-smoke
tweedle-decoder-this-call-smoke
wizard-palette-completion-smoke
```

Before adding or changing a scenario:

1. Keep actions and outcomes observable from the user-visible Alice desktop, except `gated-command-smoke` scenarios may describe bounded command-seam evidence when they make no default GUI interaction claim.
2. Use one of the supported workflow values.
3. Use `xvfb-real-alice` only when the runner can execute the real Alice command and collect logs/screenshots.
4. Use `manual-evidence-required` when human Swing interaction is required.
5. Name concrete required artifacts in `evidence.required`; manual workflows also require `review-notes.txt` for acceptance.
6. Keep YAML to the supported simple mapping/list subset.
7. Run `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`.

## Baseline preconditions

- Java 21 is available.
- Maven 3.9.9 or later is available.
- The Tweedle grammar submodule is initialized:

```bash
git submodule update --init tweedle-lang
```

If Maven reports missing generated Tweedle parser classes, first check:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Save negative artifact contract

The Save negative artifact contract is not a scenario and does not add a
workflow. It calls `run-scenario.sh validate-save-proof-evidence` directly to
prove missing validator context and missing, wrong-name, symlinked, malformed
JSON, non-object JSON, stale, future-dated, identity-mismatched, blocked,
partial, unknown-blocker, and internally inconsistent Save proof artifacts fail
closed with explicit diagnostics. It does not run the desktop Save path or
claim full desktop Save completion. It also does not claim Save As behavior,
visible rendering correctness, grading, lesson completion, learner assessment,
broad UI automation, or native dialog automation.

For runnable reviewer steps, see Run the Save Menu Dialog Negative Artifact
Contract.
For the complete rejection matrix, validator API, wrapper behavior, and review
rules, see Save Menu Dialog Negative Artifact
Contract.

Run the focused contract directly or through the branch-installable wrapper:

```bash
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> amplihack alice-qa save-negative-contract
```

Run the `uvx ... amplihack alice-qa save-negative-contract` command from the
root, or a child directory, of the checkout under review so the installed
wrapper delegates to that checkout's contract script.
