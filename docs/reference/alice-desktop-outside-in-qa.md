# Alice desktop outside-in QA reference

This reference describes the Alice desktop outside-in QA lane: file layout, runner commands, scenario schema, automation modes, configuration, and evidence artifacts.

## Contents

- [Directory layout](#directory-layout)
- [Scenario catalog](#scenario-catalog)
- [Runner commands](#runner-commands)
- [Environment variables](#environment-variables)
- [Scenario schema](#scenario-schema)
- [Automation modes](#automation-modes)
- [Evidence contract](#evidence-contract)
- [Run-window creation/wiring contract](#run-window-creationwiring-contract)
- [Current-head evidence refresh](#current-head-evidence-refresh)
- [Learner-world boundary](#learner-world-boundary)
- [Workflow evidence requirements](#workflow-evidence-requirements)
- [Scenario authoring rules](#scenario-authoring-rules)
- [Extension rules](#extension-rules)

## Directory layout

| Path | Purpose |
| --- | --- |
| `qa/outside-in/alice-desktop/README.md` | Local entry point for the QA lane. |
| `qa/outside-in/alice-desktop/scenarios/` | User-like acceptance scenario YAML files. |
| `qa/outside-in/alice-desktop/contracts/` | Declarative boundary records for QA claims that are intentionally not runner behavior. |
| `qa/outside-in/alice-desktop/gadugi/` | Gadugi-compatible CLI scenarios for agentic QA tools. |
| `qa/outside-in/alice-desktop/schema/scenario.schema.json` | Published JSON Schema contract for the scenario model. |
| `qa/outside-in/alice-desktop/runners/validate-scenarios.sh` | Catalog validator and scenario JSON dumper. |
| `qa/outside-in/alice-desktop/runners/run-scenario.sh` | Scenario listing, validation, real launch execution, and manual checklist generation. |
| `qa/outside-in/alice-desktop/runners/tab-click-probe.py` | AT-SPI Select Project tab and starter selection/opening probe for the target-specific Africa Full evidence path. |
| `qa/outside-in/alice-desktop/runners/post-project-open-probe.py` | AT-SPI post-open main-window probe gated by prior target-specific Select Project opened evidence. |
| `qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh` | Focused executable contract that validates bounded launch, run/runtime, and Select Project accessibility target discovery evidence and structured blockers. |
| `qa/outside-in/alice-desktop/evidence/` | Local generated evidence. Contents are ignored by Git except `.gitignore`. |

## Scenario catalog

| Scenario ID | Workflow | Automation mode | Purpose |
| --- | --- | --- | --- |
| `alice-desktop-archive-fixture-smoke` | `archive-fixture-smoke` | `gated-command-smoke` | Covers the focused generated legacy fixture round-trip characterization lane for `.a3p`, `.a3w`, `.a3c`, JSON boundary, and fail-closed unsupported archive behavior. |
| `alice-desktop-launch` | `launch` | `xvfb-real-alice` | Starts the real Alice desktop through Maven under Xvfb and captures launch evidence. |
| `alice-desktop-select-project-inventory` | `select-project-interaction-smoke` | `xvfb-real-alice` | Waits for the real Select Project chooser after isolated license opt-in and records title, class, process, and geometry without opening a project. |
| `alice-desktop-select-project-widget-introspection` | `select-project-widget-introspection-smoke` | `xvfb-real-alice` | Enumerates live Select Project Swing widgets through AT-SPI when the ATK wrapper is active, or records the exact ATK/AT-SPI blocker. |
| `alice-desktop-select-project-atk-exec` | `select-project-atk-exec-smoke` | `xvfb-real-alice` | Launches Alice through the AT-SPI exec:exec path and records live Select Project widget evidence or exact blockers. |
| `alice-desktop-select-project-tab-click-exec` | `select-project-tab-click-smoke` | `xvfb-real-alice` | Uses the AT-SPI exec:exec launch path to activate Select Project tabs, select/open the committed `Africa Full` starter, or record the exact blocker. |
| `alice-desktop-post-project-open-window-state` | `post-project-open-window-state-smoke` | `xvfb-real-alice` | Characterizes the Alice main-window AT-SPI frame state after project open. It is gated by prior Africa Full Select Project evidence. |
| `alice-desktop-procedure-edit-seam-smoke` | `procedure-edit-seam-smoke` | `gated-command-smoke` | Covers deterministic first-lesson procedure edit artifacts and the exact missing UI-action target at the command seam. |
| `alice-desktop-procedure-edit-handoff-smoke` | `procedure-edit-handoff-smoke` | `gated-command-smoke` | Covers object-placement-to-procedure-edit handoff evidence at the command seam. |
| `alice-desktop-instructor-student-setup` | `instructor-student-setup` | `manual-evidence-required` | Covers instructor starter-project preparation and student project opening/saving. |
| `alice-desktop-scene-creation` | `scene-creation` | `manual-evidence-required` | Covers creating or selecting a starter scene and saving it as an Alice project. |
| `alice-desktop-run-debug` | `run-debug` | `manual-evidence-required` | Covers program run controls plus the closest baseline debug-like control, such as fast-forward or statement execution. |
| `alice-desktop-run-window-contract` | `run-window-contract` | `gated-command-smoke` | Covers opt-in Run-window creation/wiring metadata and explicit non-claim boundaries without active rendering or run-execution claims. |
| `alice-desktop-save-load` | `save-load` | `manual-evidence-required` | Covers saving an `.a3p` project, reopening it, and checking persistence. |
| `alice-desktop-open-load-save` | `open-load-save` | `manual-evidence-required` | Covers opening an existing `.a3p`, saving a copy, reopening it, and comparing visible state. |
| `alice-desktop-export` | `export` | `manual-evidence-required` | Covers the current Alice export path and verification of the exported artifact. |
| `alice-desktop-exported-project-smoke` | `exported-project-ant-build-smoke` | `gated-command-smoke` | Bounded no-Sims exported Ant/NetBeans template build proof through `Alice3ProjectTemplateAntSmokeTest`; it covers Ant target execution and concrete build/JAR output only. |
| `alice-desktop-netbeans-package-smoke` | `netbeans-package-smoke` | `gated-command-smoke` | Covers NetBeans package command and representative NBM/support artifact checks. |
| `alice-desktop-package-install-smoke` | `package-install-smoke` | `gated-command-smoke` | Covers package build artifact inspection plus disposable install/launch evidence when artifacts are available. |
| `alice-desktop-project-io-smoke` | `project-io-smoke` | `gated-command-smoke` | Covers saving, reopening, editing, saving again, reopening again, and exporting a synthetic Alice project at the command seam. |
| `alice-desktop-file-loader-smoke` | `file-loader-smoke` | `gated-command-smoke` | Covers file-loader and recovery dispatch behavior at the command/test seam. |
| `alice-desktop-first-lesson-live-procedure-target-observation` | `first-lesson-live-procedure-target-observation` | `xvfb-real-alice` | Action-seam contract for observing the post-Select-Project live `scene.eatmeFirstLesson` procedure/code-editor target and recording either edit-ready evidence or the named missing CodeEditor/CodeComposite edit-action contract blocker. |
| `alice-desktop-failure-path-smoke` | `failure-path-smoke` | `gated-command-smoke` | Covers corrupt project input failure handling evidence. |
| `alice-desktop-future-ui-smoke` | `future-ui-smoke` | `gated-command-smoke` | Gated controlled-display UI startup evidence; no-op unless gated on. |
| `alice-desktop-future-ui-smoke` | `future-ui-smoke` | `gated-command-smoke` | Reserved controlled-display UI startup evidence lane; runs only when gated on. |
| `alice-desktop-menu-action-smoke` | `menu-action-smoke` | `gated-command-smoke` | Covers launch-adjacent Alice desktop menu registration and controller lookup seams without display assumptions. |
| `alice-desktop-future-ui-smoke` | `future-ui-smoke` | `gated-command-smoke` | Placeholder for controlled-display UI startup evidence; no-op unless gated on. |
| `alice-desktop-menu-action-smoke` | `menu-action-smoke` | `gated-command-smoke` | Covers bounded Window menu model registration and menu-bar membership lookup through `AliceMenuBarContractTest`, without display, rendering, Save, first-lesson, installer, or Sims claims. |
| `alice-desktop-save-menu-dialog-write-proof` | `save-menu-dialog-write-proof` | `gated-command-smoke` | Attempts one bounded rendered File-menu Save -> controlled chooser -> written `.a3p` -> readback marker path through `RobotSaveMenuDialogWriteReadbackProofTest`; only a validated `status: "proven"` artifact completes it. |
| `alice-desktop-tweedle-decoder-boundary-smoke` | `tweedle-decoder-boundary-smoke` | `gated-command-smoke` | Covers unsupported adjacent Tweedle method-call boundaries for the narrow decoder slice. |
| `alice-desktop-tweedle-decoder-this-call-smoke` | `tweedle-decoder-this-call-smoke` | `gated-command-smoke` | Covers explicit same-type zero-argument `this.method()` decoder acceptance without claiming broader decode. |
| `alice-desktop-wizard-palette-completion-smoke` | `wizard-palette-completion-smoke` | `gated-command-smoke` | Covers focused wizard, palette, and completion affordance checks where current NetBeans tests can observe them. |
| `alice-desktop-silver-thread-launch-build-run` | `silver-thread-launch-build-run` | `gated-command-smoke` | Proves the core student journey headlessly: create→build→save→reopen→execute→verify round-trip plus real starter project load→inspect→copy→reopen through `SilverThreadLaunchBuildRunTest`. |
| `alice-desktop-post-open-runtime-display-accessibility-evidence` | `post-open-runtime-display-accessibility-evidence` | `xvfb-real-alice` | Collects narrow read-only post-open runtime/display accessibility evidence, or a precise structured blocker. |
| `alice-desktop-procedure-edit-handoff-smoke` | `procedure-edit-handoff-smoke` | `gated-command-smoke` | Covers the object-placement artifact handoff into the deterministic procedure edit seam. |
| `alice-desktop-procedure-edit-seam-smoke` | `procedure-edit-seam-smoke` | `gated-command-smoke` | Covers deterministic procedure edit artifacts and the exact missing UI edit action target. |
| `alice-desktop-save-negative-artifact-contract` | `save-negative-artifact-contract` | `manual-evidence-required` | Proves the `validate-save-proof-evidence` seam rejects missing context, missing, wrong-name, symlinked, malformed, non-object, stale, future-dated, identity-mismatched, blocked, unknown-blocker, partial, and inconsistent Save proof artifacts with explicit diagnostics. |

The first-lesson live procedure target action seam is a read-only contract. It
records only whether the live desktop exposes a stable `scene.eatmeFirstLesson`
procedure/code-editor target and whether that target is ready for a public
desktop edit action; it does not perform a desktop edit, Save, rendering
correctness check, learner assessment, creative assessment, or full first-lesson
completion proof. See [First-Lesson Live Procedure Target Action
Seam](./first-lesson-live-procedure-target-observation.md).

The Run-window contract scenario is a non-executing creation/wiring lane: its
default `--prepare-only` path records scenario wiring and checklist evidence, and
its gated command runs only the focused `EatmeRunWindowEvidenceTest` seam test.
It does not claim active rendering, run execution, world execution correctness,
rendering correctness, Save behavior, grading, creative assessment, lesson
completion, or full UI automation.
The menu/action smoke is a gated command contract for the headless-safe
`AliceMenuBarContractTest` only. When `ALICE_QA_RUN_GATED_SMOKES=1`, the runner
executes the checked-in scenario argv. With `NODE_OPTIONS` set in the
environment, that argv is equivalent to:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.ide.croquet.models.AliceMenuBarContractTest \
  test
```

The accepted evidence is `status.txt`, `command.log`, and Maven/Surefire output
naming `AliceMenuBarContractTest`. It proves only Window menu model registration
and menu-bar membership lookup. It is not full UI automation, rendered menu
verification, Save completion, first-lesson completion, deployed installer
success, or Sims validation.
The accessibility target discovery silver-thread contract is a focused shell
contract over existing launch, run/debug, post-open runtime/display, and Select
Project evidence paths. It validates target discovery signals, structured
blockers, and bounded scope wording only; it does not launch Alice, add a new
scenario workflow, or claim full UI automation, visual correctness, rendering
correctness, world execution correctness, full world execution, or general
accessibility compliance. See [Accessibility Target Discovery Silver-Thread
Contract](./accessibility-target-discovery-silver-thread.md).

## Learner-world boundary

RabbitHole learner-world QA currently supports setup/open/save evidence review
only. It does not provide learner-world grading, rubric scoring, correctness
assessment, or creative assessment. Any future learner-world assessment
capability beyond that evidence boundary requires a separate reviewed
assessment contract and evidence mapping. The current declarative blocker record
is `qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json`;
it names blocker `define-reviewed-assessment-contract` and supplies the current
manual status/checklist boundary wording.

The selected boundary scenario is
`alice-desktop-instructor-student-setup`. Its generated
`manual-evidence-checklist.txt` contains the standard manual sections:
preconditions, user actions, expected outcomes, required evidence, fallback
notes, completion status, and an `Assessment boundary` section that states
manual evidence required, setup/open/save evidence review only, no automated
grading, no rubric scoring, no correctness assessment, no creative assessment,
and blocker `define-reviewed-assessment-contract`. It also renders the contract
summary that learner-world grading, rubric scoring, correctness assessment, and
creative assessment remain manual/unsupported until a reviewed assessment
contract exists. The generated `status.txt` repeats the same boundary as plain
status fields, including `assessmentBoundary=define-reviewed-assessment-contract`
and `assessmentBoundaryMode=manual/unsupported`.
See
[Learner-world assessment boundary](./learner-world-assessment-boundary.md) for
the current artifact fields, generated checklist text, review workflow, and
extension rules.

### Boundary artifact

`learner-world-assessment-boundary.json` is a checked-in declarative contract
for the current learner-world claim boundary. It is not an executable scenario,
runner input, or assessment engine.

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | string | Stable artifact identifier. Current value: `learner-world-assessment-boundary`. |
| `selectedScenario` | string | Scenario covered by the boundary: `alice-desktop-instructor-student-setup`. |
| `automationMode` | string | Current mode for this boundary: `manual-evidence-required`. |
| `scope` | string | The bounded QA area: instructor/student learner-world setup, open, and save evidence. |
| `currentCapability` | string | The current capability statement. It is limited to collecting evidence for setup, open, and save workflow review. |
| `supportedEvidence` | string array | Supported evidence categories, including setup/open/save evidence review only. |
| `assessmentLimits` | string array | Current generated-evidence limits: no automated grading, no rubric scoring, no correctness assessment, and no creative assessment. |
| `nonCapabilities` | string array | Current capabilities not claimed by this lane: learner-world grading, rubric scoring, correctness assessment, and creative assessment. |
| `nextBoundary` | string | The next required boundary before assessment work can be claimed: `define-reviewed-assessment-contract`. |
| `manualLimitationSummary` | string | User-facing summary rendered in generated evidence and documentation. It states that learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported. |
| `requiresReviewedAssessmentContractBefore` | string array | Required contract topics before any future assessment implementation can be claimed: learner-world grading, rubric scoring, correctness assessment, and creative assessment. |
| `nextBlocker.id` | string | The next required blocker before future assessment work can be claimed. Current value: `define-reviewed-assessment-contract`. |
| `nextBlocker.description` | string | Human-readable explanation that a reviewed assessment contract and evidence mapping are required before learner-world grading, rubric scoring, correctness assessment, or creative assessment can be claimed. |
| `blocker` | object | Runner checklist blocker: learner-world state extraction for grading or creative assessment is blocked until safe rubric inputs and limits are defined. |

Documentation, scenarios, generated checklists, and review notes may point to
this artifact when they need a stable boundary reference. Runners must not treat
it as grading, scoring, or creative-assessment behavior.

The boundary contract is not a place to configure automation. Current contract
tests reject `gradingAlgorithm`, `assessmentAlgorithm`, `scoreSchema`,
`rubricSchema`, `creativeAssessmentEngine`, and `runnerIntegration` because
those fields imply assessment behavior that this lane does not provide.

## Runner commands

Run commands from the repository root.

| Command | Purpose | Output contract |
| --- | --- | --- |
| `validate-scenarios.sh` | Validate the active scenario catalog. | Prints the number of valid scenarios and the active catalog directory. |
| `validate-scenarios.sh --list` | List normalized scenario records. | Prints scenario ID, automation mode, and title. |
| `validate-scenarios.sh --dump-json` | Dump the full normalized catalog. | Prints a JSON array sorted by scenario file path. |
| `validate-scenarios.sh --dump-json <scenario-id>` | Dump one normalized scenario. | Prints a JSON object for the requested scenario ID. |
| `run-scenario.sh validate-save-proof-evidence <artifact> --scenario <id> --workflow <workflow> --run-id <run-id> --started-at-epoch <epoch>` | Validate one Save proof artifact at the existing evidence-validation seam. | Exits zero only for a fresh, canonical, internally consistent `status: "proven"` Save proof artifact; all missing, malformed, stale, blocked, partial, or inconsistent artifacts exit non-zero with explicit diagnostics. |
| `run-scenario.sh list` | List runnable scenarios. | Prints the same user-facing list as the validator. |
| `run-scenario.sh validate` | Validate the active catalog through the runner. | Delegates to `validate-scenarios.sh`. |
| `run-scenario.sh run <scenario-id-or-path>` | Create evidence for one scenario. | Prints the created run directory and writes artifacts under the evidence directory. |
| `gadugi-test validate -f qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml` | Validate the Gadugi exported launcher evidence scenario. | Confirms the scenario uses the Gadugi CLI schema, not the custom Alice scenario schema. |
| `gadugi-test validate scenarios/` | Validate all 30 Alice scenario YAML files against the gadugi-test canonical schema. | Reports valid/invalid counts; expects 0 invalid files. Run from `qa/outside-in/alice-desktop/`. |
| `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s exported-launcher-evidence --timeout 300000` | Run the Gadugi exported launcher evidence scenario. | Delegates to the outside-in exported-project smoke runner in prepare-only mode by default. |
| `uvx --from git+<repo>@<branch-or-commit> amplihack alice-qa list` | Install the QA wrapper from a branch or commit and list scenarios in the current checkout. | Prints the same user-facing list as the runner. |
| `uvx --from git+<repo>@<branch-or-commit> amplihack alice-qa save-negative-contract` | Install the QA wrapper from a branch or commit and run the Save negative artifact contract in the current checkout. | Proves invalid Save proof artifacts fail closed with explicit diagnostics; it is not desktop Save completion evidence. |
| `uvx --from git+<repo>@<branch-or-commit> amplihack alice-qa run <scenario-id-or-path>` | Install the QA wrapper from a branch or commit and create evidence in the current checkout. | Delegates to `run-scenario.sh run`. |
| `gadugi-test validate -f qa/outside-in/alice-desktop/gadugi/archive-fixture-evidence.yaml` | Validate the Gadugi archive fixture evidence scenario. | Confirms the scenario uses the Gadugi CLI schema. |
| `gadugi-test run -d qa/outside-in/alice-desktop/gadugi -s archive-fixture-evidence --timeout 600000` | Run the Gadugi archive fixture evidence scenario. | Delegates to the outside-in archive-fixture smoke runner and contract test suite. |
| `uvx --from git+<repo>@<branch> amplihack alice-qa list` | Install the QA wrapper from a branch and list scenarios in the current checkout. | Prints the same user-facing list as the runner. |
| `uvx --from git+<repo>@<branch> amplihack alice-qa run <scenario-id-or-path>` | Install the QA wrapper from a branch and create evidence in the current checkout. | Delegates to `run-scenario.sh run`. |

### Validate all scenarios

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

### List scenarios

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --list
qa/outside-in/alice-desktop/runners/run-scenario.sh list
```

### Dump a scenario as JSON

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json alice-desktop-launch
```

Without an argument, `--dump-json` prints the full catalog as an array. With a scenario ID, it prints exactly one scenario object. Unknown scenario IDs fail with a non-zero exit status.

### Run a scenario

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run <scenario-id-or-path>
```

The runner accepts either a scenario ID or a `.yaml` file path. Scenario paths must be direct files inside the active scenario directory; nested paths and paths outside the active catalog are rejected.

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  qa/outside-in/alice-desktop/scenarios/launch.yaml
```

This path form resolves the top-level `id` in the YAML file, validates that ID through the active catalog, and then runs the normalized scenario.

### Run through the branch-installable wrapper

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> \
  amplihack alice-qa list

uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> \
  amplihack alice-qa save-negative-contract

uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> \
  amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Replace `<branch-or-commit>` with the PR branch or commit you are reviewing. The
`amplihack alice-qa` wrapper is intentionally thin. It must be run from an Alice
checkout, locates the repository root from the current working directory, and
delegates to the checked-out shell runners.

### Run with a custom evidence directory

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run <scenario-id> \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

### Run with a custom timeout

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch \
  --timeout-seconds 180
```

`--timeout-seconds` applies to `xvfb-real-alice` execution. Manual scenarios write checklists immediately.

### Post-open runtime/display accessibility evidence

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

This command uses the existing Alice launch/open path and the same Xvfb/AT-SPI infrastructure as the live Swing probes. The runner invokes `post-open-runtime-display-probe.py` after the supported project-open setup and writes `post-open-runtime-display-accessibility-evidence.json` plus probe-local `runtime-display-accessibility-status.txt`. The implementation records runtime/display and controlled-display success separately from the target-scoped sampling seam: `status.txt` can show `visibleRenderingPixelSamplingStatus=observed` only when checked raw RGBA samples were collected inside one validated target with `visibleRenderingCorrectnessEstablished=false`; otherwise it points to the exact sampling blocker. Failure or missing runtime/display, display, screenshot/pixel, root-directory, license, AT-SPI, target, or sampling prerequisites are recorded as structured blockers. For the dedicated usage, configuration, artifact API, examples, and review boundaries, see [Post-open runtime/display accessibility evidence](./post-open-runtime-display-accessibility-evidence.md).

### Prepare a gated smoke without execution

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-netbeans-package-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

`--prepare-only` is the intentional preflight mode for gated command smokes. It writes `outcome=gated-not-run` evidence and returns success without executing the configured command.

### Run-window creation/wiring command interface

Prepare-only mode is the default review path for scenario wiring:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

When the focused seam check should run, reviewers enable the gated command
explicitly:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

The scenario's allowed argv is exact:

```text
mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/ide -am -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest test
```

No shell command string, alternate Maven goal, broader test selector, or
rendering/execution probe may be part of this scenario.

### Run the exported Ant project build smoke

Use the checked-in exported-project smoke for bounded no-Sims exported
Ant/NetBeans template build evidence. It is a gated Maven smoke, so it runs only
when `ALICE_QA_RUN_GATED_SMOKES=1` is set:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar

ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-exported-project-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/exported-project-smoke
```

The scenario executes this focused Maven command:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl netbeans -am \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest \
  test
```

Accept the scenario only as exported Ant project build evidence: `status.txt`
must record a passed gated command and `command.log` must show
`Alice3ProjectTemplateAntSmokeTest` completed with Ant `jar`, `run`,
`run-test-with-main`, `clean`, generated classes, jar manifest, resource
packaging, and probe marker evidence. If target execution cannot complete,
preserve the failed `status.txt` and `command.log` as the blocker record,
including the exact Maven command, failing Ant target or prerequisite, and
missing condition.

### Run the Gadugi exported launcher evidence scenario

The Gadugi scenario is stored outside the custom Alice scenario catalog because
it uses the Gadugi CLI schema. Prerequisite: `gadugi-test` must be installed and
available on `PATH`.

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s exported-launcher-evidence \
  --timeout 300000
```

The scenario prepares the `alice-desktop-exported-project-smoke` evidence lane
through the existing outside-in runner. The default Gadugi path uses
`--prepare-only` and validates only delegated evidence wiring; it does not
execute the exported Ant build proof. It does not prove visible rendering,
installer behavior, save behavior, grading, creative assessment, or full lesson
completion. The default Gadugi path uses the underlying runner's `--prepare-only`
mode;
`ALICE_QA_RUN_GATED_SMOKES=1` only applies when the underlying Alice runner is
invoked without `--prepare-only`.

### Run the Gadugi archive fixture evidence scenario

The archive fixture evidence Gadugi scenario validates PR #433 legacy fixture
round-trip characterization through the outside-in runner and contract tests.
Like all Gadugi scenarios, it lives under `qa/outside-in/alice-desktop/gadugi/`.

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/archive-fixture-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s archive-fixture-evidence \
  --timeout 600000
```

The scenario delegates to `validate-scenarios.sh`, `run-scenario.sh run
alice-desktop-archive-fixture-smoke --prepare-only`, and `run-tests.sh`. It uses
conservative delegation with `assertions: []`. It does not prove full historical
archive migration, full Tweedle decode, full player decode, arbitrary user
archive support, UI automation, visible rendering, or desktop save/open behavior.

### Exit behavior

Runner and validator commands return a non-zero exit status when the catalog is invalid, a requested scenario is unknown, a scenario path is outside the active catalog, a timeout value is invalid, an automation mode is unsupported, a gated command smoke is not enabled and `--prepare-only` was not requested, or a required launch/evidence capture step fails.

## Environment variables

| Variable | Applies to | Default | Description |
| --- | --- | --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Validator and runner | `qa/outside-in/alice-desktop/scenarios` | Overrides the directory containing scenario YAML files. Scenario path arguments are resolved against this active catalog. |
| `ALICE_QA_DISPLAY` | Xvfb runs | First free display from `:90` through `:120` | Reuses a specific X display instead of selecting one automatically. |
| `ALICE_QA_SCREEN` | Xvfb runs | `1280x900x24` | Sets Xvfb screen geometry. |
| `ALICE_QA_READY_WAIT_SECONDS` | Xvfb runs | Scenario `automation.readyWaitSeconds` | Overrides the scenario readiness wait before screenshot capture. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | Xvfb runs | unset | Set to `1` only for controlled QA launches that need isolated first-run License Agreement acceptance state. The runner records the generated `java.util.prefs.userRoot` and `.java/.userPrefs/` state files in `license-acceptance.json`. |
| `ALICE_QA_RUN_GATED_SMOKES` | Gated command smokes | unset | Set to `1` to execute configured command smokes. When unset, the runner writes `outcome=gated-not-run` status and a checklist, then exits non-zero unless `--prepare-only` was requested. |
| `ALICE_QA_DISABLE_WINDOW_DETECTOR` | Xvfb runs | unset | Set to `1` only for contract tests to force a plain `window-detector-unavailable` inventory record. |
| `NODE_OPTIONS` | Surrounding Node tooling | unset | Use `--max-old-space-size=32768` when a larger QA orchestrator invokes Node-based helpers around this lane. The lane itself does not require Node. |

Example:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_DISPLAY=:99 \
ALICE_QA_SCREEN=1600x1000x24 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

## Scenario schema

Each scenario is a YAML file whose file name matches its ID without the `alice-desktop-` prefix.

The JSON Schema is the published scenario contract. The validator uses a small built-in parser so this lane has no extra runtime dependency, and it must stay in parity with the schema's required fields, allowed values, and cross-field rules. When the schema changes, update the validator and its contract tests in the same change.

Scenario YAML must use the supported subset: simple mappings, nested mappings, and lists of scalar values. Do not use anchors, aliases, tags, multiline scalars, flow-style collections, tabs for indentation, or other advanced YAML features.

Example:

```yaml
id: alice-desktop-save-load
name: Save and load an Alice project
title: Save and load an Alice project
workflow: save-load
automationMode: manual-evidence-required
preconditions:
  - Java 21 is available.
  - Maven dependencies are available.
  - The tweedle-lang submodule is initialized.
userActions:
  - Launch Alice.
  - Create or open a small project.
  - Save the project as an a3p file in a known evidence location.
  - Close the project or restart Alice.
  - Open the saved a3p file.
  - Verify the loaded project matches the saved state.
expectedOutcomes:
  - Alice writes a usable a3p project file.
  - Alice opens the saved a3p project without uncaught application errors.
  - The visible scene or program state after loading matches the saved project.
evidence:
  required:
    - Save operation log or manual notes.
    - Saved a3p project file.
    - Screenshot before saving.
    - Screenshot after reopening.
    - review-notes.txt comparing the saved and loaded state.
fallback:
  mode: manual-evidence-required
  notes:
    - Link lower-level Maven or JUnit characterization evidence for project loading when available.
    - Manual evidence remains required until stable full GUI save and open automation exists.
supportingEvidence:
  - alice-desktop-launch
steps:
  - validate
agents:
  - alice-desktop-qa
```

### Required fields

| Field | Type | Description |
| --- | --- | --- |
| `id` | string | Scenario ID. Must match `alice-desktop-[a-z0-9-]+`. |
| `title` | string | Human-readable scenario title. |
| `workflow` | enum | Covered workflow. |
| `automationMode` | enum | How the runner handles the scenario. |
| `preconditions` | string list | Required starting conditions. |
| `userActions` | string list | User-like actions, written from outside the implementation. |
| `expectedOutcomes` | string list | Observable outcomes that show success. |
| `evidence.required` | string list | Files, screenshots, logs, artifacts, or notes required for review. |
| `fallback.mode` | enum | Fallback automation mode. |
| `fallback.notes` | string list | Specific fallback instructions. |

### Optional fields

| Field | Type | Description |
| --- | --- | --- |
| `name` | string | Human-readable scenario name for gadugi-test compatibility. Must equal `title`. Present on every scenario; required by `gadugi-test validate`. |
| `steps` | string list | Gadugi-test step list. Currently `["validate"]` for all scenarios. |
| `agents` | string list | Gadugi-test agent list. Currently `["alice-desktop-qa"]` for all scenarios. |
| `automation.cwd` | string | Repository-relative working directory for argv-backed automation. Required for `xvfb-real-alice` and `gated-command-smoke`; absolute paths, `..`, and realpath escapes outside the repository are rejected. |
| `automation.argv` | string list | Argument vector executed directly by the runner without shell interpretation. Required for `xvfb-real-alice` and `gated-command-smoke`; only the checked-in Alice QA argv allowlist is accepted. |
| `automation.timeoutSeconds` | positive integer | Default timeout for argv-backed automation. Required for `xvfb-real-alice` and `gated-command-smoke` except `save-menu-dialog-write-proof` and `run-window-contract`, where workflow-level timeouts are invalid. |
| `automation.readyWaitSeconds` | positive integer | Wait before screenshot capture for UI automation; use `1` for command smokes. Required for `xvfb-real-alice` and `gated-command-smoke`. |
| `targetStarter.displayName` | string | Display name of the committed starter project targeted by Select Project AT-SPI automation. Required for `alice-desktop-select-project-tab-click-exec`. |
| `targetStarter.repositoryPath` | string | Repository-relative path recorded as target evidence metadata. For the Select Project AT-SPI target scenario and first-lesson live target observation this must be `core/resources/src/application/resources/starter-projects/AfricaFull.a3p`. |
| `supportingEvidence` | string list | Scenario IDs or evidence sources that support this scenario. |
| `tags` | string list | Additional scenario labels. |

`automation` is required when `automationMode` is `xvfb-real-alice` or `gated-command-smoke`. Manual scenarios do not need an `automation` block because the runner generates a checklist instead of driving Swing interactions. Automation must be represented as `argv`; shell command strings are not accepted, including in custom catalogs selected with `ALICE_QA_SCENARIO_DIR`.

The `save-menu-dialog-write-proof` and `run-window-contract` workflows are no-timeout exceptions. Their scenarios omit `automation.timeoutSeconds`, and the runner does not wrap their Maven argv in shell `timeout`. Validator and runner contract tests reject timeout wiring for those workflows while preserving timeout requirements for the other argv-backed scenarios.

### Workflow values

```text
archive-fixture-smoke
export
exported-project-ant-build-smoke
failure-path-smoke
file-loader-smoke
first-lesson-live-procedure-target-observation
future-ui-smoke
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

## Automation modes

| Mode | Runner behavior |
| --- | --- |
| `xvfb-real-alice` | Starts Xvfb, launches Alice through the allowed scenario argv, waits for readiness, and captures environment data, logs, status, and screenshot when the launch reaches evidence capture. This is a launch evidence check, not a full semantic oracle for every startup log condition. |
| `manual-evidence-required` | Writes a structured checklist for human execution and evidence collection. Checklist generation does not complete the scenario. |
| `gated-command-smoke` | Writes environment, status, and checklist evidence by default without running heavy commands. When `ALICE_QA_RUN_GATED_SMOKES=1`, runs the configured command under `timeout`, captures `command.log`, and records pass/fail status. |

## Evidence contract

Every run creates a timestamped directory before scenario execution begins:

```text
<evidence-dir>/<scenario-id>/<timestamp>/
```

The default evidence directory is:

```text
qa/outside-in/alice-desktop/evidence/
```

The artifact set depends on the automation mode and how far execution gets.

Manual scenario preparation includes:

| Artifact | Description |
| --- | --- |
| `environment.txt` | UTC timestamp, repository root, display, Java version, Maven version, and OS details. |
| `status.txt` | Scenario ID, automation mode, generated checklist name, and `manual-evidence-required` outcome. |
| `manual-evidence-checklist.txt` | Scenario preconditions, actions, outcomes, required evidence, fallback notes, and completion status. For `alice-desktop-instructor-student-setup`, this includes a generated assessment-boundary section. This file prepares the work; it is not proof that the workflow has been executed. |

Gated command smoke preparation includes:

| Artifact | Description |
| --- | --- |
| `environment.txt` | UTC timestamp, repository root, display, Java version, Maven version, and OS details. |
| `status.txt` | Scenario ID, automation mode, `outcome=gated-not-run`, gate name, skip mode, command, working directory, timeout, and generated checklist name. |
| `manual-evidence-checklist.txt` | Review checklist describing what evidence is required when the gate is enabled or fulfilled elsewhere. |

Enabled gated command smoke execution also includes:

| Artifact | Description |
| --- | --- |
| `command.log` | Captured stdout/stderr for the configured command. |
| `status.txt` | Scenario ID, automation mode, command, working directory, timeout, command log name, exit code, and `outcome=passed` or `outcome=failed`. |

### Run-window creation/wiring contract

The Run-window contract lane is centered on one opt-in product seam:
`EatmeRunWindowEvidence`. The seam is enabled only by the JVM property
`org.alice.eatme.runWindowEvidenceDir`. When the property is unset, no artifact
is written. When it is set, the configured directory must already exist, and the
seam writes only the fixed artifact `run-window-created.json` inside that
directory without following a pre-existing artifact symlink. The lane is passive:
it records that the Run-window creation hook reached the evidence writer, not
that the Run window rendered, executed, or completed learner-facing work.

The artifact is a creation/wiring metadata record. It is not a render-affordance
artifact, runtime result, Save artifact, grading artifact, creative-assessment
artifact, lesson-completion artifact, screenshot, pixel sample, or full UI
automation transcript.

For the dedicated usage guide, artifact API, Java seam API, configuration, path
safety rules, examples, and tutorial, see [Run-Window Creation/Wiring
Contract](./run-window-creation-wiring-contract.md).

Required `run-window-created.json` fields:

| Field | Type | Required value or meaning |
| --- | --- | --- |
| `schema_version` | string | `eatme.alice-run-window-created/v1`. |
| `status` | string | `created`. |
| `contract_scope` | string | `run-window-creation-wiring`. |
| `evidence_source` | string | `org.alice.stageide.run.RunComposite#handlePreShowWindow`. |
| `artifact` | string | `run-window-created.json`; alternate names are invalid. |
| `frame_title` | string | Optional display metadata from the created frame title, JSON-escaped. Empty string is valid when unavailable. |
| `program_type` | string | Optional display metadata from the active program type name, JSON-escaped. Empty string is valid when unavailable. |
| `active_rendering_claimed` | boolean | Always `false`. |
| `run_program_claimed` | boolean | Always `false`. |
| `run_execution_claimed` | boolean | Always `false`. |
| `world_execution_claimed` | boolean | Always `false`. |
| `rendering_correctness_claimed` | boolean | Always `false`. |
| `save_claimed` | boolean | Always `false`. |
| `grading_claimed` | boolean | Always `false`. |
| `full_ui_automation_claimed` | boolean | Always `false`. |
| `does_not_claim` | string array | Must include `active-rendering`, `run-execution`, `world-execution-correctness`, `rendering-correctness`, `save`, `grading`, and `full-ui-automation`. |

The v1 artifact does not expose separate creative-assessment or
lesson-completion booleans. Those claims remain outside the Run-window
creation/wiring scope and must not be inferred from a passing artifact.

Representative artifact:

```json
{
  "schema_version": "eatme.alice-run-window-created/v1",
  "status": "created",
  "contract_scope": "run-window-creation-wiring",
  "evidence_source": "org.alice.stageide.run.RunComposite#handlePreShowWindow",
  "artifact": "run-window-created.json",
  "frame_title": "Run Alice",
  "program_type": "Scene",
  "active_rendering_claimed": false,
  "run_program_claimed": false,
  "run_execution_claimed": false,
  "world_execution_claimed": false,
  "rendering_correctness_claimed": false,
  "save_claimed": false,
  "grading_claimed": false,
  "full_ui_automation_claimed": false,
  "does_not_claim": [
    "active-rendering",
    "run-execution",
    "world-execution-correctness",
    "rendering-correctness",
    "save",
    "grading",
    "full-ui-automation"
  ]
}
```

Path safety is part of the API: callers and tests reject absolute paths,
parent traversal, nested artifact names, empty artifact names, and any resolved
artifact path that escapes the configured evidence directory. Invalid evidence
directories and write failures are logged as seam failures; they do not produce
success-shaped fallback artifacts.

Successful `xvfb-real-alice` evidence capture can include these common and scenario-specific artifacts:

| Artifact | Description |
| --- | --- |
| `environment.txt` | UTC timestamp, repository root, display, Java version, Maven version, and OS details. |
| `launch.log` | Alice Maven launch output. |
| `xvfb.log` | Xvfb output. |
| `status.txt` | Scenario ID, automation mode, display, readiness status, process status, screenshot status, window inventory status, Select Project status when applicable, Alice candidate count, and timeout. |
| `x-window-inventory.json` | Alice-related visible X window title, class, process, and geometry after launch readiness wait, or an explicit unsupported/blocker record. |
| `select-project-window.json` | Select Project title/class/process/geometry proof when the exact chooser window is observed; otherwise records the exact missing-window blocker. Widget labels are resource-contract evidence only and name `swing-widget-inventory-not-collected` until live Swing widget introspection exists. |
| `tab-click-observation.json` | Supporting project-open setup artifact for Select Project tab activation/open attempts. |
| `post-project-open-observation.json` | Supporting project-open setup artifact recording `postOpenWindowObserved` before the runtime/display probe runs. |
| `post-open-runtime-display-accessibility-evidence.json` | Post-open runtime/display accessibility evidence for `alice-desktop-post-open-runtime-display-accessibility-evidence`, or the exact blocker that prevents collecting that evidence. |
| `controlled-display-pixel-observation.json` | Controlled-display screenshot-consistency artifact and target-readiness source for bounded world-canvas pixel sampling. |
| `visible-rendering-pixel-observation.json` or `visible-rendering-pixel-sampling-blocker.json` | Final target-scoped sampling result for post-open runtime/display evidence. Observed runs write bounded raw pixel samples inside one validated Run-window/world-canvas target and keep `visibleRenderingCorrectnessEstablished=false`; blocked runs write the exact target or sampler blocker with `renderedWorldPixelsObserved=false`. |
| `screenshot.png` or `screenshot.xwd` | Captured desktop image. |
| `screenshot.log` | Screenshot command output. |

For launch runs, `status.txt` records whether the process stayed alive, whether a visible window was detected when a detector is available, whether window inventory was captured, and whether screenshot capture succeeded. Acceptance still requires reviewing the generated evidence, especially `x-window-inventory.json` and `launch.log`; the runner does not currently scan the log for every possible uncaught application exception.

For post-open runtime/display accessibility runs, `status.txt` records
`runtimeDisplayAccessibilityEvidence=post-open-runtime-display-accessibility-evidence.json`,
`runtimeDisplayAccessibilityStatus`, `runtimeDisplayAccessibilityBlocker`,
`controlledDisplayPixelStatus`, `controlledDisplayPixelBlocker`,
`visibleRenderingPixelSamplingStatus`, `visibleRenderingPixelSamplingArtifact`,
`visibleRenderingPixelSamplingBlocker`, and `outcome=passed` or
`outcome=blocked`. `runtime-display-accessibility-status.txt` is probe-local; use
`status.txt` for the final scenario decision because it also accounts for
controlled-display pixel status and the current fail-closed target-scoped
sampling seam. The JSON artifact is the runtime/display machine-readable
contract:

| Field | Type | Meaning |
| --- | --- | --- |
| `automationMode` | string | Scenario automation mode, currently `xvfb-real-alice`. |
| `blocker` | string | `none` on success, otherwise a precise blocker such as `x-server-unavailable`, `display-allocation-unavailable`, `pyatspi-not-installed`, `at-spi-registry-unavailable`, `atk-wrapper-not-loaded`, `post-open-window-not-observed`, `runtime-display-accessible-candidate-not-found`, `java-pid-not-in-inventory`, or `input-unreadable`. |
| `blockerDetail` | string | Human-readable detail for the blocker. |
| `claim` | string | Always `post-open-runtime-display-accessibility-evidence`. |
| `javaPid` | integer or null | Alice Java process ID used for AT-SPI lookup, or `null` when unavailable. |
| `postOpenRuntimeDisplayAccessibilityObserved` | boolean | `true` only when the post-open runtime/display candidate was observed. |
| `postOpenWindowObserved` | boolean | `true` only when `post-project-open-observation.json` recorded the prerequisite post-open window signal. |
| `runtimeDisplayCandidateCount` | integer | Number of accepted runtime/display candidates. |
| `runtimeDisplayCandidates` | array | Bounded AT-SPI metadata for accepted candidates: child count, name, accessibility tree path, role, visible/enabled state names, `geometryStatus`, and `screenExtents`. |
| `scenario` | string | Always `alice-desktop-post-open-runtime-display-accessibility-evidence`. |
| `status` | enum | `observed` or `blocked`. |
| `traversalErrors` | array | Non-fatal AT-SPI traversal errors collected while searching; empty when none were seen. |

The artifact must not include environment variables, credentials, process dumps,
unrelated desktop windows, saved project contents, decoder output, grading state,
lesson state, or world execution traces. Acceptance requires the JSON
runtime/display artifact, final `status.txt`, controlled-display source artifact,
and either the bounded sampling observation or the exact sampling blocker. A
bounded pixel observation artifact may replace the sampling blocker only when
`visibleRenderingPixelSamplingStatus=observed` and
`visibleRenderingCorrectnessEstablished=false`. JSON `status=observed` alone is
not enough if `controlledDisplayPixelStatus` is blocked or attempted, or if
sampling remains blocked. JSON `status=blocked` remains the machine-readable
runtime/display gap report.

Early `xvfb-real-alice` fallback attempts may not produce the full launch artifact set. If Xvfb is missing or no display is available, the runner writes `environment.txt` plus `manual-evidence-checklist.txt` and exits non-zero. If Xvfb starts but exits before Alice launch, the run directory contains `xvfb.log` plus `manual-evidence-checklist.txt`. In these early fallback cases, most scenarios do not write `status.txt` because launch did not reach the evidence-capture phase. The post-open runtime/display accessibility scenario is the exception: it writes `post-open-runtime-display-accessibility-evidence.json` and `status.txt` with a blocked runtime/display accessibility outcome when an early prerequisite prevents collection.

Manual scenarios are complete only after a human performs the workflow and places the required artifacts in the same timestamped run directory. Every accepted manual run must include `review-notes.txt` with the scenario ID, run directory, evidence files reviewed, observed result, deviations from the checklist, and an explicit accept or reject decision.

For `alice-desktop-instructor-student-setup`, the manual checklist includes an
`Assessment boundary` section that keeps the run artifact aligned with the
checked-in boundary contract: setup/open/save evidence review only, manual
evidence required, no automated grading, no rubric scoring, no correctness
assessment, no creative assessment, next boundary
`define-reviewed-assessment-contract`, and explicit manual/unsupported checklist
lines for learner-world grading, rubric scoring, correctness assessment, and
creative assessment. Those capabilities remain manual/unsupported until that
reviewed assessment contract and evidence mapping exist.

## Current-head evidence refresh

PR readiness and review evidence is current only when it is produced from the PR
branch or PR ref after reconciliation with `origin/develop`. The reviewer records
the PR head SHA, reconciled `HEAD`, `origin/develop` SHA, merge base, scenario
ID, run directory, timestamp, and blocker or observation decision in review
notes, PR text, or CI artifact metadata. The runner-emitted `environment.txt`
currently records timestamp/repository/display/Java/Maven/OS details, not Git
SHAs. Evidence from `develop`, from the pre-merge PR head, or from a different
worktree is stale for the current review unless it is explicitly marked
`superseded`.

The current-head refresh workflow is:

1. Fetch `origin/develop` and the PR ref.
2. Check out the PR branch or PR ref.
3. Merge `origin/develop` into that branch; do not rebase shared PR history for
   this lane.
4. Resolve all conflicts and verify no merge state, unmerged path, or conflict
   marker remains.
5. Validate the scenario catalog and focused contract checks.
6. Run the target scenario when live prerequisites are available, or preserve the
   exact blocked current-head artifact when they are not.
7. Review only the final run directory for readiness claims.

Generated current-head evidence remains local and ignored by Git unless a
separate process publishes it as a CI artifact. Documentation and PR text may
point to the run directory, contract checks, and blocker IDs, but they must keep
claims bounded to observed runtime/display, controlled-display, target-ready, or
raw target-scoped sampling signals.

## Workflow evidence requirements

| Workflow | Required evidence |
| --- | --- |
| Launch | Launch log, `x-window-inventory.json`, desktop screenshot, controlled display observation, exit/status/timeout record, Java/Maven/display environment summary. |
| Select Project interaction smoke | `select-project-window.json` with `interactionProof=select-project-window-visible`, `x-window-inventory.json`, screenshot, license artifacts showing no first-run dialog, status with `selectProjectWaitStatus`, and Java/Maven/display environment summary. |
| Post-open runtime/display accessibility evidence | `post-open-runtime-display-accessibility-evidence.json` with `status=observed`, `postOpenRuntimeDisplayAccessibilityObserved=true`, `runtimeDisplayCandidateCount>0`, candidate `geometryStatus`/`screenExtents`, and `blocker=none`, plus final `status.txt` with runtime/display, controlled-display, target, and sampling status fields. Pass requires `visibleRenderingPixelSamplingStatus=observed` and `visibleRenderingCorrectnessEstablished=false`; blocked target or sampler paths write precise blockers. Supporting artifacts include `runtime-display-accessibility-status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `controlled-display-pixel-observation.json`, `visible-rendering-pixel-sampling-blocker.json` or `visible-rendering-pixel-observation.json`, `x-window-inventory.json`, launch log, Xvfb log, screenshot, and Java/Maven/display environment summary. If prerequisites are unavailable, the JSON/status artifacts record blocked outcomes with precise blockers. |
| Select Project tab-click smoke | `tab-click-observation.json` with `targetStarter.displayName=Africa Full`, `targetStarter.repositoryPath=core/resources/src/application/resources/starter-projects/AfricaFull.a3p`, `evidenceStatus=opened`, matching `openedStarter` metadata, `targetStarterObserved.name=Africa Full`, `targetStarterSelected=true`, `targetStarterOpenAttempted=true`, and `projectOpenObserved=true`, or existing `blocker`/`blockerDetail` fields plus structured target-specific `nextBlocker` details. See [Select Project Africa Full AT-SPI evidence reference](./select-project-africa-full-atspi-evidence.md). |
| Select Project widget introspection smoke | `swing-widget-observation.json`, `x-window-inventory.json`, status, launch log, Xvfb log, screenshot, and exact blocker details when AT-SPI or the Java ATK wrapper is unavailable. |
| Select Project AT-SPI exec smoke | `swing-widget-observation.json` from the AT-SPI exec:exec launch path, launch log, Xvfb log, screenshot, and exact blocker details when the wrapper/process/widget condition is unmet. |
| Post-project open window-state smoke | `post-project-open-observation.json` characterizing main-window AT-SPI state. It must be gated by prior `tab-click-observation.json` Africa Full evidence with `evidenceStatus=opened`, matching target/opened metadata, `targetStarterObserved.name=Africa Full`, `targetStarterSelected=true`, `targetStarterOpenAttempted=true`, and `projectOpenObserved=true`; generic main-window presence is not Africa Full proof. |
| First-lesson live procedure target action seam | `first-lesson-live-procedure-target-observation.json` with either `status=edit-ready`, `observedTarget.readyForDesktopEditAction=true`, `desktopEditAction.blocker.kind=none`, and top-level `blocker.kind=none`, or the exact no-go blocker `missing-desktop-edit-action-contract` in both nested and top-level blocker objects. Supporting artifacts include `status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, launch log, Xvfb log, and Java/Maven/display environment summary. Target-only observation and display, AT-SPI, or target-not-found blockers are structured run failures, not accepted action-seam proof. |
| Procedure edit seam smoke | `status.txt`, `command.log`, focused test output naming `editsSceneProcedureAndWritesEatmeProofArtifacts`, and procedure edit artifacts named by the focused test. |
| Procedure edit handoff smoke | `status.txt`, `command.log`, focused test output naming `chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff`, and handoff evidence recording `placed-project.a3p` as the procedure edit input project artifact. |
| Instructor/student setup | Instructor launch log, starter project screenshot, starter `.a3p`, student launch or open log, loaded project screenshot, student copy `.a3p`, `review-notes.txt`. This workflow is setup/open/save evidence only; pair it with `contracts/learner-world-assessment-boundary.json` when reviewing the current learner-world claim boundary. |
| Scene creation | Screenshot before scene creation, screenshot after object or scene appears, saved `.a3p`, notes identifying the selected template or object in `review-notes.txt`. |
| Run/debug | Screenshot before run, screenshot or screen capture during execution, notes naming run/debug-like controls in `review-notes.txt`, launch or run log, saved `.a3p`. |
| Save/load | Save log or notes, saved `.a3p`, screenshot before saving, screenshot after reopening, comparison notes in `review-notes.txt`. |
| Open/load/save | Open log or notes identifying the source `.a3p`, screenshot after first open, saved copy `.a3p`, screenshot after reopening the copy, comparison decision in `review-notes.txt`. |
| Export | Export log or notes, screenshot before export, screenshot after export completion, exported artifact, file listing or checksum, `review-notes.txt`. |
| Exported project smoke | `status.txt`, `command.log`, and the focused no-Sims Maven command targeting `Alice3ProjectTemplateAntSmokeTest`; required evidence covers Ant `jar`, `run`, `run-test-with-main`, `clean`, generated classes, jar manifest and contents, resource packaging, probe markers, and no `Java Result:` line. It is not installer validation or full GUI export journey evidence. See [Exported NetBeans Ant Project Behavior](./exported-netbeans-ant-project-behavior.md). |
| NetBeans package smoke | `status.txt`, `command.log`, NetBeans target artifact listing or CI artifact link, representative jar/zip content listing. |
| Package/install smoke | `status.txt`, `command.log`, package or installer artifact listing, disposable install log or explicit not-produced note. |
| Archive fixture smoke | `status.txt`, `command.log`, archive fixture path or generated fixture notes, and focused test output proving the fixture seam. |
| File loader smoke | `status.txt`, `command.log`, focused file-loader/recovery test output, and review notes for any generated fixture or failure-path metadata. |
| Project save, reopen, edit, save again, reopen again, and export smoke | `status.txt`, `command.log`, test output or surefire report naming `IoUtilitiesTest.savedProjectCanBeReopenedEditedSavedAgainReopenedAndExported`, and review notes for metadata and export archive structure assertions. No durable saved-project artifact is required because the smoke uses test-local temporary files. |
| Failure path smoke | `status.txt`, `command.log`, failure classification or dispatch-plan output, corrupt input fixture name or generated fixture notes. |
| Future UI smoke | `status.txt`, `command.log` when gated, startup screenshot or first-window signal when collected, manual fallback notes otherwise. |
| Run-window creation/wiring contract | Evidence contract: `status.txt`, `command.log`, focused seam test output naming `EatmeRunWindowEvidenceTest`, and canonical `run-window-created.json` evidence with `schema_version=eatme.alice-run-window-created/v1`, `status=created`, `contract_scope=run-window-creation-wiring`, `evidence_source=org.alice.stageide.run.RunComposite#handlePreShowWindow`, false capability booleans, and `does_not_claim` boundaries. This workflow proves creation/wiring only and does not claim active rendering, run execution, world execution correctness, rendering correctness, Save behavior, grading, creative assessment, lesson completion, or full UI automation. |
| Save menu dialog write/readback proof | Evidence contract: `status.txt`, `command.log`, focused Robot Save menu/dialog/write/readback proof test output naming `RobotSaveMenuDialogWriteReadbackProofTest`, and fresh canonical `robot-save-menu-dialog-write-readback-proof.json` evidence with `schemaVersion=eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1`, matching `scenario` and `runId`, `status=proven`, all required menu/dialog/control/write/readback marker flags true, an existing `.a3p` output with matching size, and marker readback verified. Missing, stale, blocked, partial, internally inconsistent, or unknown-blocker artifacts fail closed. Stale `StageIdeSaveMenuDoClickToWriteProofTest` output or `save-menu-dialog-write-proof.json` artifacts do not satisfy this scenario. See [Save Proof Evidence](./save-proof-evidence.md). |
| Save menu dialog negative artifact contract | `status.txt`, `command.log`, and `test-save-menu-dialog-negative-artifact-contract.sh` output proving the `validate-save-proof-evidence` seam rejects missing context, missing, wrong-name, symlinked, malformed, non-object, stale, future-dated, identity-mismatched, blocked, unknown-blocker, partial, and inconsistent Save proof artifacts with explicit diagnostics. This is not Save completion evidence. See [Save Menu Dialog Negative Artifact Contract](./save-menu-dialog-negative-artifact-contract.md). |
| Wizard/palette/completion smoke | `status.txt`, `command.log`, focused test output for wizard validation, palette wiring, and completion resources; manual screenshot notes when desktop evidence is added. |

## Scenario authoring rules

Scenario files are the public acceptance contract for this lane. A valid scenario:

1. Uses an ID in the `alice-desktop-<workflow>` family.
2. Keeps `userActions` and `expectedOutcomes` observable from the desktop user's point of view.
3. Names evidence that a reviewer can inspect without reconstructing hidden local state.
4. Uses `xvfb-real-alice` only for workflows the runner can execute through the real Alice desktop command.
5. Uses `manual-evidence-required` for Swing GUI workflows that still require human interaction.
6. Uses `gated-command-smoke` for expensive CLI/package or future UI smokes that must not be mandatory in lightweight validation.
7. Lists any dependent scenario evidence in `supportingEvidence`, such as using launch evidence to support save/load or export evidence.
8. Requires `review-notes.txt` for manual workflow acceptance.
9. Uses only the supported YAML subset: mappings, nested mappings, scalar values, and scalar lists with spaces for indentation.
10. Uses `automation.argv` rather than a shell command string; only the allowlisted Alice QA argv set is accepted.
11. Avoids implementation details such as Java class names, internal package names, or assumptions about private UI objects.
12. Keeps post-open runtime/display evidence narrow: do not use that scenario to claim full rendering correctness, full world execution, grading, lesson completion, deployed installer success, Save behavior, active Select Project behavior, or decoder behavior.
13. Keeps learner-world setup narrow: do not use instructor/student setup evidence to claim learner-world grading, rubric scoring, correctness assessment, or creative assessment.
14. Keeps any future Run-window contract narrow: do not use `run-window-created.json`, the scenario checklist, or the focused seam test to claim active rendering, run execution, world execution correctness, rendering correctness, Save behavior, grading, creative assessment, lesson completion, or full UI automation.
14. Keeps accessibility target discovery evidence narrow: do not use launch, run/runtime, or Select Project target discovery markers to claim full UI automation, visual correctness, rendering correctness, world execution correctness, full world execution, or general accessibility compliance.

## Extension rules

When adding or changing scenarios:

1. Keep the scenario user-like. Describe what the instructor, student, or Alice user does and observes.
2. Prefer real Alice execution through the runner when it is stable.
3. Use `manual-evidence-required` when Swing GUI interaction is not stable enough to automate.
4. Use `gated-command-smoke` when the scenario is executable but too expensive or environment-sensitive for default validation.
5. Do not introduce Playwright unless Alice exposes a browser/web surface.
6. Do not use a virtual TTY for Swing GUI interaction.
7. Preserve Alice 3 baseline behavior unless a behavior change is explicitly documented and tested.
8. Validate the catalog before committing:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
cd qa/outside-in/alice-desktop && gadugi-test validate scenarios/
```

9. Include `name`, `steps`, and `agents` in every new scenario. Set `name` equal to `title`, `steps` to `["validate"]`, and `agents` to `["alice-desktop-qa"]`. These fields satisfy `gadugi-test validate` while the repo-owned validator remains the primary structural check.
