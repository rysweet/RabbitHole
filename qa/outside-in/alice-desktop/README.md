# Alice desktop outside-in QA

This lane defines executable acceptance coverage for Alice desktop workflows without changing product modules. It keeps scenario intent, execution wrappers, and evidence requirements in one repo-owned QA area.

For user-facing instructions, see [Run Alice desktop outside-in QA](../../../docs/howto/alice-desktop-outside-in-qa.md). For the post-open runtime/display evidence contract, see [Post-open runtime/display accessibility evidence](../../../docs/reference/post-open-runtime-display-accessibility-evidence.md). For the target-specific Select Project starter path, see [Open Africa Full through Select Project with AT-SPI](../../../docs/howto/open-africa-full-through-select-project-atspi.md) and the [Select Project Africa Full AT-SPI evidence reference](../../../docs/reference/select-project-africa-full-atspi-evidence.md). For the live first-lesson procedure/code-editor target seam, see [First-Lesson Live Procedure Target Observation](../../../docs/reference/first-lesson-live-procedure-target-observation.md). For the complete scenario schema and runner interface, see the [Alice desktop outside-in QA reference](../../../docs/reference/alice-desktop-outside-in-qa.md).

## What belongs here

| Area | Owns | Does not own |
| --- | --- | --- |
| `scenarios/` | User-like workflows, expected outcomes, evidence requirements, automation mode | Java implementation details or brittle internal UI assumptions |
| `contracts/` | Declarative claim-boundary records, including the learner-world assessment blocker | Runner behavior or assessment implementation |
| `gadugi/` | Gadugi-compatible CLI scenarios for agentic QA tools, including exported launcher evidence contract checks | The custom Alice scenario schema or full desktop/rendering claims |
| `schema/` | Scenario structure and allowed field values | Business logic |
| `runners/` | Thin wrappers around existing Maven/Alice commands | New build systems, hidden dependencies, or product behavior changes |
| `evidence/` | Local run artifacts produced by the runner | Source-controlled product assets |

Generated evidence is ignored by Git. Commit only scenario definitions, schema changes, runner changes, and the evidence `.gitignore`.

## Scenario model

Each scenario uses the same fields:

- `id`
- `title`
- `workflow`
- `automationMode`
- `preconditions`
- `userActions`
- `expectedOutcomes`
- `evidence.required`
- `fallback`

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
the next blocker, `define-reviewed-assessment-contract`, and is not consumed by
the runner as behavior.

Do not use learner-world QA evidence to claim learner-work grading, rubric
scoring, correctness assessment, or creativity assessment. Any future
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
```

Use one of the supported workflows:

```text
archive-fixture-smoke
export
exported-project-smoke
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
save-load
save-menu-dialog-write-proof
scene-creation
select-project-atk-exec-smoke
select-project-interaction-smoke
select-project-tab-click-smoke
select-project-widget-introspection-smoke
tweedle-decoder-boundary-smoke
tweedle-decoder-this-call-smoke
wizard-palette-completion-smoke
```

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
observed-or-blocked decision artifact for the narrow
`scene.eatmeFirstLesson` procedure tab/code-editor target. The shard is
read-only; it does not edit the procedure, Save, prove rendering correctness,
assess learner work, or claim full first-lesson completion.

`run-scenario.sh run` accepts either a scenario ID or a direct `.yaml` file inside the active scenario catalog. Use `--evidence-dir <dir>` to write evidence outside the repository, `--timeout-seconds <seconds>` to override argv-backed launch timeout, and `--prepare-only` to intentionally prepare gated smoke evidence without executing the gated command.

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
screen-coordinate extents for pixel sampling. It is not visible rendered-world
proof. `status=blocked` means `visible-rendering-pixel-target-blocker.json`
names the exact missing target and next unblocker.
`visible-rendering-pixel-sampling-blocker.json` records the next seam after
target readiness: rendered-world pixels are unavailable, unsampled, and
unchecked, so visible rendered-world correctness remains blocked with
`sampleCount=0`. `geometryStatus=ambiguous-candidates` is reserved for more than
one visible/showing candidate with valid positive screen-coordinate extents; a
larger raw candidate count with missing, zero, negative, malformed, or otherwise
invalid geometry stays fail-closed without being reported as ambiguous.
`tab-click-observation.json` and
`post-project-open-observation.json` are supporting setup artifacts. An observed
result is limited to a live post-open runtime/display accessibility signal,
controlled-display screenshot consistency, pixel target readiness or its exact
blocker, and a fail-closed rendered-pixel sampling blocker. It does not prove
world-canvas pixel correctness, deployed installer success, full world
execution, grading, lesson completion, active Save behavior, active Select
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
An opened result requires `evidenceStatus=opened`, exact `Africa Full`
`targetStarter` metadata, `targetStarterObserved.name=Africa Full`,
`targetStarterSelected=true`, `targetStarterOpenAttempted=true`, matching `openedStarter`, and
`projectOpenObserved=true`. A blocked result preserves string `blocker` and
`blockerDetail` fields and adds one structured `nextBlocker`; it is not a full
Alice UI automation, visible rendering, grading, creative assessment, Save,
first-lesson, launcher, or decoder claim.

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

For branch-installable outside-in checks, run the thin `amplihack` wrapper from a checkout of the branch:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> amplihack alice-qa list
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch> amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

Replace `<branch>` with the PR branch or commit you are reviewing. The wrapper
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
| `visible-rendering-pixel-sampling-blocker.json` | Machine-readable blocker for the post-target-readiness rendered-pixel sampling seam. It records that target readiness is not rendered-world proof and keeps `renderedPixelsAvailable=false`, `renderedPixelsSampled=false`, `renderedPixelsChecked=false`, and `sampleCount=0` until real rendered pixels are observed and checked. |

The controlled-display screenshot-consistency artifact does not assert Alice
world rendering correctness.

The `alice-desktop-post-open-runtime-display-accessibility-evidence` scenario
goes one step beyond launch, window, and pixel evidence. It uses the existing
Xvfb and AT-SPI setup, reuses the supported project-open setup, then runs the
read-only `post-open-runtime-display-probe.py` probe against the live Alice
accessibility tree.

| Artifact | Role |
| --- | --- |
| `post-open-runtime-display-accessibility-evidence.json` | Runtime/display accessibility decision artifact. Observed requires `status=observed`, `postOpenRuntimeDisplayAccessibilityObserved=true`, at least one runtime/display candidate, and `blocker=none`. |
| `status.txt` | Final scenario status. Pass requires `outcome=passed`, `runtimeDisplayAccessibilityStatus=observed`, and `controlledDisplayPixelStatus=observed`; it also records `visibleRenderingPixelTargetStatus`, `visibleRenderingPixelTargetArtifact`, `visibleRenderingPixelSamplingStatus`, and the rendered-pixel sampling blocker artifact. |
| `runtime-display-accessibility-status.txt` | Probe-local status written before final scenario status; useful for debugging, not the final pass/fail artifact. |
| `tab-click-observation.json` and `post-project-open-observation.json` | Supporting project-open setup artifacts. |
| `controlled-display-pixel-observation.json` | Controlled-display screenshot-consistency artifact with screenshot path, dimensions when available, pixel-observation metadata, target-ready or blocked `worldCanvasPixelTarget`, and unsupported claims. Controlled-display blockers keep final `outcome=blocked`; the rendered-pixel sampling blocker blocks only the visible rendered-world claim. |
| `visible-rendering-pixel-target-blocker.json` | Blocker artifact naming the exact next unblocker for missing, invalid, or ambiguous world-canvas pixel target readiness. Ambiguous means more than one visible/showing candidate has valid extents, not merely more than one raw candidate. |
| `visible-rendering-pixel-sampling-blocker.json` | Blocker artifact naming `reliable-rendered-world-pixel-observation` as the next unblocker after target readiness. Target-ready metadata alone must not be accepted as sampled or checked rendered pixels. |

If Xvfb, display allocation/startup, root-directory prep, license prep, AT-SPI,
`python3-pyatspi`, the Java ATK wrapper, screenshot/pixel capture, screenshot
metadata, project-open setup, or the runtime/display candidate is unavailable,
the JSON/status artifacts record `status=blocked` or `outcome=blocked` plus
precise blocker fields; the runner must not silently pass. Target-ready metadata
is emitted only when one visible/showing candidate has valid positive
screen-coordinate extents; otherwise the exact blocker artifact is preserved.
This evidence supports only a live post-open runtime/display accessibility
signal, controlled-display screenshot consistency, pixel target readiness or
its exact blocker, and a fail-closed pixel-sampling blocker. It does not prove
world-canvas pixel correctness, deployed installer success, full world
execution, grading, lesson completion, active Save behavior, active Select
Project behavior, or decoder behavior. The stable artifact API, target
readiness and post-readiness pixel-sampling contracts, configuration, examples,
and review rules are documented in [Post-open
runtime/display accessibility evidence](../../../docs/reference/post-open-runtime-display-accessibility-evidence.md).

The first-lesson live procedure target observation seam goes one step beyond the
Select Project first-lesson open path by observing whether the live post-open
desktop exposes a stable procedure tab or code-editor target for
`scene.eatmeFirstLesson`, then writing
`first-lesson-live-procedure-target-observation.json` with `status=observed` or
an exact `status=blocked` reason. It must not mutate a procedure, Save, assert
rendering correctness, assess learner work, or claim full first-lesson
completion. The artifact API, configuration, examples, and review rules are
documented in [First-Lesson Live Procedure Target
Observation](../../../docs/reference/first-lesson-live-procedure-target-observation.md).

Early Xvfb fallback directories may contain only the diagnostics available before launch plus a manual fallback checklist. For manual scenarios, the runner creates a status file and structured checklist so the workflow is repeatable and reviewable; the scenario is complete only after a human performs the workflow and adds the required evidence artifacts plus `review-notes.txt`. For gated command smokes, an unset gate records `outcome=gated-not-run` and exits non-zero; pass `--prepare-only` for intentional preflight/checklist preparation, or set `ALICE_QA_RUN_GATED_SMOKES=1` only in a worktree prepared for the configured Maven or display-backed argv.

## Configuration

| Variable | Purpose |
| --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Override the active scenario catalog. |
| `ALICE_QA_DISPLAY` | Reuse a specific X display for Xvfb runs. |
| `ALICE_QA_SCREEN` | Set Xvfb screen geometry. Defaults to `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Override launch readiness wait before screenshot capture. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | Set to `1` only for controlled QA launches that need to bypass first-run License Agreement dialogs with an isolated Java Preferences user root. |
| `ALICE_QA_RUN_GATED_SMOKES` | Execute gated command smoke scenarios when set to `1`; otherwise they write `gated-not-run` evidence and exit non-zero unless `--prepare-only` is requested. |
| `NODE_OPTIONS` | Optional for surrounding Node-based orchestrators. Use `--max-old-space-size=32768` when needed; this lane itself does not require Node. |

The Select Project Africa Full scenario passes `targetStarter.displayName` and `targetStarter.repositoryPath` to the AT-SPI probe as `TARGET_STARTER_DISPLAY_NAME` and `TARGET_STARTER_REPO_PATH`. These variables are runner-managed evidence metadata, not user configuration knobs.

## Scenario authoring checklist

Use one of the supported workflows:

```text
archive-fixture-smoke
export
exported-project-smoke
failure-path-smoke
file-loader-smoke
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
save-load
save-menu-dialog-write-proof
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

1. Keep actions and outcomes observable from the user-visible Alice desktop.
2. Use one of the supported [workflow values](../../../docs/reference/alice-desktop-outside-in-qa.md#workflow-values).
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
