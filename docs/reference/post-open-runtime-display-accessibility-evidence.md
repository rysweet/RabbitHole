# Post-open runtime/display accessibility evidence

This reference documents the Alice desktop outside-in QA scenario contract for
post-open runtime/display accessibility evidence and the planned
controlled-display screenshot-consistency evidence contract.

The scenario proves one implemented narrow claim today: after Alice opens a
project through the existing supported launch/open path, the live accessibility
tree exposes at least one runtime/display candidate. The visible-rendering shard
will add a second narrow claim: the controlled-display screenshot artifact is
internally consistent with the pixel metadata recorded for that same artifact.
Neither claim proves world-canvas pixel correctness, deployed installer success,
full world execution, grading, lesson completion, active Save behavior, active
Select Project behavior, or decoder behavior.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Configuration](#configuration)
- [Scenario interface](#scenario-interface)
- [Evidence API](#evidence-api)
- [Controlled-display screenshot-consistency API](#controlled-display-screenshot-consistency-api)
- [Review workflow](#review-workflow)
- [Examples](#examples)
- [Review rules](#review-rules)
- [Validation commands](#validation-commands)
- [Troubleshooting](#troubleshooting)

## Scope

The scenario is part of the Alice desktop outside-in QA lane:

```text
qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml
qa/outside-in/alice-desktop/runners/post-open-runtime-display-probe.py
qa/outside-in/alice-desktop/runners/run-scenario.sh
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/schema/scenario.schema.json
```

It reuses the existing real Alice launch/open path under Xvfb and AT-SPI. The
runner starts Alice with the `alice-ide-atk` Maven execution, performs the
supported project-open setup, captures a controlled-display screenshot, then
invokes the read-only runtime/display probe. The planned visible-rendering shard
will strengthen the controlled-display artifact so it records
screenshot-consistency metadata for that same screenshot.

The probe and planned screenshot-consistency step are observational. They do not
click controls, save projects, select new starters, execute worlds, grade work,
inspect decoder output, mutate project data, or infer rendered-world correctness
from a generic desktop screenshot.

## Usage

Run commands from the repository root.

Validate the scenario catalog first:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Collect post-open runtime/display accessibility evidence:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

The runner writes a timestamped directory:

```text
qa/outside-in/alice-desktop/evidence/post-open-runtime-display/
  alice-desktop-post-open-runtime-display-accessibility-evidence/
    <timestamp>/
```

Generated evidence is local run output. Keep it uncommitted. Use
`post-open-runtime-display-accessibility-evidence.json` for the implemented
runtime/display accessibility decision. The planned visible-rendering shard will
also use `controlled-display-pixel-observation.json` for the strengthened
screenshot-consistency decision.

## Configuration

| Variable | Purpose |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Preferred memory setting for surrounding Node-based QA orchestration. The shell runner itself does not require Node. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` | Enables isolated first-run License Agreement acceptance state for controlled QA launches only. |
| `ALICE_QA_DISPLAY` | Reuses a specific X display instead of selecting one automatically. |
| `ALICE_QA_SCREEN` | Sets Xvfb screen geometry. Defaults to `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Overrides the scenario readiness wait before screenshot capture and probe steps. |

Runtime prerequisites:

| Requirement | Why it is needed |
| --- | --- |
| Java 21 and Maven | Build and launch Alice through the existing Maven path. |
| `git submodule update --init tweedle-lang` | Initializes the required Tweedle grammar submodule before broad Maven validation. |
| Xvfb | Provides the controlled display session. |
| `python3-pyatspi` | Lets the probe read the AT-SPI accessibility tree. |
| `/usr/share/java/java-atk-wrapper.jar` | Makes Swing accessibility data visible to AT-SPI for the spawned Java process. |
| AT-SPI2 accessibility bus | Provides the live accessibility registry for the user session. |

## Scenario interface

The public runner interface is intentionally small and fixed:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  [--evidence-dir <directory>] \
  [--timeout-seconds <seconds>]
```

The same scenario may also be addressed by its checked-in YAML file:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml
```

| Contract | Value |
| --- | --- |
| Scenario ID | `alice-desktop-post-open-runtime-display-accessibility-evidence` |
| Workflow | `post-open-runtime-display-accessibility-evidence` |
| Automation mode | `xvfb-real-alice` |
| Launch path | Existing Alice IDE Maven launch path with the AT-SPI wrapper execution. |
| Decision artifact | `post-open-runtime-display-accessibility-evidence.json` |
| Final status artifact | `status.txt` with `outcome=passed` only when required checks for the implemented lane are observed. After the visible-rendering shard lands, that also requires controlled-display screenshot consistency. |
| Probe-local status artifact | `runtime-display-accessibility-status.txt`, written by the probe before the runner writes final scenario status. Use it for debugging the probe result, not as the final pass/fail decision. |
| Screenshot-consistency artifact | `[PLANNED - Implementation Pending]` `controlled-display-pixel-observation.json` with the strengthened schema in this reference. |
| World-pixel blocker artifact | `[PLANNED - Implementation Pending]` `visible-rendering-pixel-target-blocker.json`, written for the `screenshot-metadata-unavailable` fallback and reused by the next rendered-world-pixel shard when it requires a world-canvas pixel target. |
| Supporting setup artifacts | `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, launch log, Xvfb log, screenshot, and environment summary. |
| Default evidence root | `qa/outside-in/alice-desktop/evidence/` unless `--evidence-dir` is supplied. |

Callers provide only runner flags and environment variables. Scenario YAML is
declarative input; it never supplies shell fragments. The schema, validator, and
runner keep workflow names and argv values allowlisted so the lane fails closed
when scenario wiring drifts.

## Evidence API

The decision artifact is:

```text
post-open-runtime-display-accessibility-evidence.json
```

The final `status.txt` points to that artifact and records the overall scenario
decision:

```text
runtimeDisplayAccessibilityEvidence=post-open-runtime-display-accessibility-evidence.json
runtimeDisplayAccessibilityStatus=<observed|blocked>
runtimeDisplayAccessibilityBlocker=<blocker>
outcome=<passed|blocked>
controlledDisplayPixelStatus=<observed|blocked|attempted>
controlledDisplayPixelBlocker=<blocker>
```

`runtime-display-accessibility-status.txt` is a probe-local status file with the
same runtime/display accessibility keys. The runner writes it before final
scenario status is assembled; reviewers should use `status.txt` for the final
decision because `status.txt` also accounts for controlled-display pixel status.

### Observed artifact

An observed result emits these fields. Field order is not part of the contract.

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
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "status": "observed",
  "traversalErrors": []
}
```

The minimum decision fields for accepting an observed result are:

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
      "states": ["enabled", "showing", "visible"]
    }
  ],
  "blocker": "none"
}
```

An observed result means the live post-open accessibility tree exposed at least
one runtime/display candidate. It is not a visual rendering oracle and is not a
claim about project execution, grading, lesson completion, Save, Select Project,
installer success, or decoder behavior.

### Blocked artifact

When the runner cannot collect the narrow evidence, it still writes the same
artifact with a precise blocker:

```json
{
  "automationMode": "xvfb-real-alice",
  "blocker": "runtime-display-accessible-candidate-not-found",
  "blockerDetail": "No visible AT-SPI accessible component matched the narrow runtime/display candidate criteria after the post-open window state was observed.",
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

Runtime/display accessibility blocker values include:

| Blocker | Meaning |
| --- | --- |
| `x-server-unavailable` | Xvfb or another required X server path is unavailable. |
| `display-allocation-unavailable` | The runner could not allocate a controlled display. |
| `pyatspi-not-installed` | No Python interpreter with `pyatspi` is available. |
| `at-spi-registry-unavailable` | The AT-SPI registry is not reachable. |
| `atk-wrapper-not-loaded` | The Java ATK wrapper is unavailable or not active for the Alice process. |
| `post-open-window-not-observed` | The prerequisite post-open Alice window signal was not observed. |
| `runtime-display-accessible-candidate-not-found` | The probe reached the accessibility tree but found no accepted runtime/display candidate. |
| `java-pid-not-in-inventory` | The probe could not map the Alice Java process from the window inventory. |
| `input-unreadable` | A required input artifact could not be read. |

Runner-level blockers can prevent the scenario from passing before or after the
runtime/display probe. Common examples include:

| Blocker | Meaning |
| --- | --- |
| `root-directory-property-missing` | `alice-ide/pom.xml` does not configure the expected Alice root-directory property. |
| `core-resources-distribution-prep-failed` | Maven could not prepare `core/resources/target/distribution`. |
| `core-resources-distribution-not-created` | The preparation command finished but the distribution directory was still missing. |
| `license-acceptance-prep-failed` | The isolated first-run license acceptance state could not be prepared. |
| `x-server-start-failed` | Xvfb was found but exited before Alice launch. |
| `screenshot-capture-failed` | The runner could not capture the controlled-display screenshot. |
| `application-exited-before-pixel-capture` | Alice exited before controlled-display pixel capture. |
| `application-exited-before-window-ready` | Alice exited before the window-readiness check completed. |
| `application-root-directory-missing` | The Application Root Error dialog was observed. |
| `first-run-license-agreement-visible` | A first-run License Agreement dialog blocked the controlled launch. |
| `alice-window-not-found` | No visible Alice desktop window was found before screenshot capture. |
| `screenshot-captured-uniform-black` | Screenshot capture succeeded, but every sampled pixel was black. |
| `screenshot-pixel-analysis-unavailable` | Screenshot capture succeeded, but pixel classification could not establish observed controlled-display pixels. |

The full artifact fields are:

| Field | Meaning |
| --- | --- |
| `automationMode` | Scenario automation mode, currently `xvfb-real-alice`. |
| `blocker` | `none` on an observed result, otherwise the exact blocker. |
| `blockerDetail` | Human-readable blocker detail. Empty only when `blocker=none`. |
| `claim` | Stable claim token: `post-open-runtime-display-accessibility-evidence`. |
| `javaPid` | Alice Java process ID used for AT-SPI lookup, or `null` when unavailable. |
| `postOpenRuntimeDisplayAccessibilityObserved` | `true` only when accepted runtime/display candidates were found. |
| `postOpenWindowObserved` | Whether `post-project-open-observation.json` already recorded the prerequisite post-open window signal. |
| `runtimeDisplayCandidateCount` | Count of accepted runtime/display candidates emitted in the artifact. |
| `runtimeDisplayCandidates` | Bounded AT-SPI summaries for accepted candidates: `childCount`, `name`, `path`, `role`, and `states`. |
| `scenario` | Scenario ID that produced the artifact. |
| `status` | `observed` or `blocked`. |
| `traversalErrors` | Non-fatal AT-SPI traversal errors collected while searching; empty when none were seen. |

The artifact must stay small and safe: no credentials, environment dumps,
arbitrary process dumps, unrelated desktop windows, saved project contents,
decoder output, grading state, lesson state, or world execution traces.

## Controlled-display screenshot-consistency API

`[PLANNED - Implementation Pending]`

This section describes the finished visible-rendering shard contract to build.
The current runner writes `controlled-display-pixel-observation.json`, but it
does not yet emit every field below, including `schemaVersion`, `claimScope`,
structured `screenshot`, structured `pixelObservation`, `worldCanvasPixelTarget`,
or `unsupportedClaims`.

The screenshot-consistency artifact is:

```text
controlled-display-pixel-observation.json
```

It will be a controlled-display evidence artifact, not a rendered-world oracle.
The runner will derive screenshot dimensions and pixel-observation metadata from
the same relative screenshot artifact named in the JSON. It must not record
absolute paths, hostnames, usernames, environment variables, credentials, or
binary image data.

### Planned observed screenshot-consistency artifact

An observed result will emit these fields. Field order is not part of the
contract.

```json
{
  "schemaVersion": 1,
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "automationMode": "xvfb-real-alice",
  "status": "observed",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "controlled-display-screenshot-consistency",
  "claimScope": "controlled-display-screenshot-consistency-only",
  "screenshot": {
    "relativePath": "screenshot.png",
    "format": "png",
    "dimensions": {
      "width": 1280,
      "height": 900
    }
  },
  "pixelObservation": {
    "status": "observed",
    "sourceArtifact": "screenshot.png",
    "dimensionsConsistentWithScreenshot": true,
    "sampledPixelCount": 64,
    "nonBlackSampledPixelCount": 12,
    "uniformBlack": false
  },
  "worldCanvasPixelTarget": {
    "identified": false,
    "targetDescription": "",
    "sampleCoordinates": [],
    "missingUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "world-execution",
    "grading",
    "lesson-completion",
    "save-behavior",
    "select-project-behavior",
    "installer-success",
    "decoder-behavior"
  ]
}
```

The minimum decision fields for accepting planned screenshot-consistency
evidence are:

```json
{
  "schemaVersion": 1,
  "status": "observed",
  "blocker": "none",
  "claim": "controlled-display-screenshot-consistency",
  "claimScope": "controlled-display-screenshot-consistency-only",
  "screenshot": {
    "relativePath": "screenshot.png",
    "dimensions": {
      "width": 1280,
      "height": 900
    }
  },
  "pixelObservation": {
    "status": "observed",
    "sourceArtifact": "screenshot.png",
    "dimensionsConsistentWithScreenshot": true
  },
  "worldCanvasPixelTarget": {
    "identified": false,
    "missingUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness"
  ]
}
```

An observed screenshot-consistency result will mean only that the runner captured
a controlled-display screenshot, read its dimensions, and recorded pixel
metadata that points back to that same screenshot. It will not mean the runner
identified the Run window's world canvas, sampled pixels inside that canvas, or
validated rendered-world content.

### Planned blocked screenshot-consistency artifact

When screenshot metadata cannot be read safely, the controlled-display artifact
will record a blocked result and the runner will also write the world-pixel
blocker artifact described below.

```json
{
  "schemaVersion": 1,
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "automationMode": "xvfb-real-alice",
  "status": "blocked",
  "blocker": "screenshot-metadata-unavailable",
  "blockerDetail": "The runner captured screenshot.png but could not safely read dimensions from that same artifact.",
  "claim": "controlled-display-screenshot-consistency",
  "claimScope": "controlled-display-screenshot-consistency-only",
  "screenshot": {
    "relativePath": "screenshot.png",
    "format": "png",
    "dimensions": null
  },
  "pixelObservation": {
    "status": "blocked",
    "sourceArtifact": "screenshot.png",
    "dimensionsConsistentWithScreenshot": false
  },
  "worldCanvasPixelTarget": {
    "identified": false,
    "targetDescription": "",
    "sampleCoordinates": [],
    "missingUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "world-execution",
    "grading",
    "lesson-completion",
    "save-behavior",
    "select-project-behavior",
    "installer-success",
    "decoder-behavior"
  ]
}
```

### Planned world-canvas pixel target blocker

The blocker artifact is:

```text
visible-rendering-pixel-target-blocker.json
```

It will be the machine-readable next blocker for true rendered-world pixel
correctness when screenshot metadata cannot be read safely. The next
rendered-world-pixel shard should also use this shape when it explicitly
requires world-canvas pixel evidence but lacks a reliable Run-window/world-canvas
pixel sampling target:

```json
{
  "schemaVersion": 1,
  "scenario": "alice-desktop-post-open-runtime-display-accessibility-evidence",
  "automationMode": "xvfb-real-alice",
  "status": "blocked",
  "claim": "visible-rendering-world-pixel-evidence",
  "missingUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target",
  "worldCanvasPixelTarget": {
    "identified": false,
    "targetDescription": "",
    "sampleCoordinates": []
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "world-execution",
    "grading",
    "lesson-completion",
    "save-behavior",
    "select-project-behavior",
    "installer-success",
    "decoder-behavior"
  ]
}
```

This artifact will not be a failure to document. It is the planned
machine-readable blocker until the implementation has a reliable
Run-window/world-canvas pixel sampling target. Do not replace it with generic
screenshot evidence.

## Review workflow

Use this review sequence for every run:

1. Open `status.txt` and confirm it points to
   `post-open-runtime-display-accessibility-evidence.json`.
2. Open the JSON decision artifact and read `status`, `blocker`,
   `postOpenRuntimeDisplayAccessibilityObserved`,
   `runtimeDisplayCandidateCount`, and `runtimeDisplayCandidates`.
3. After the visible-rendering shard lands, open
   `controlled-display-pixel-observation.json` and confirm `schemaVersion=1`,
   `status=observed`, `claim=controlled-display-screenshot-consistency`,
   `claimScope=controlled-display-screenshot-consistency-only`, a relative
   screenshot path, non-null screenshot dimensions,
   `pixelObservation.dimensionsConsistentWithScreenshot=true`,
   `worldCanvasPixelTarget.identified=false`, and explicit `unsupportedClaims`.
4. Review `visible-rendering-pixel-target-blocker.json` when present. Preserve it
   as the exact next blocker for world-canvas pixel correctness:
   `reliable-run-window-world-canvas-pixel-sampling-target`.
5. Review `tab-click-observation.json` and
   `post-project-open-observation.json` to understand the supporting project-open
   setup.
6. Accept the implemented runtime/display run only when `status.txt` records
   `outcome=passed` and `runtimeDisplayAccessibilityStatus=observed`, and the
   decision artifact records `status=observed`, `blocker=none`,
   `postOpenRuntimeDisplayAccessibilityObserved=true`, and
   `runtimeDisplayCandidateCount` greater than zero. After the
   visible-rendering shard lands, also require
   `controlledDisplayPixelStatus=observed`.

If the decision artifact is `status=blocked`, preserve it as the run result. A
blocked artifact is useful evidence about the missing prerequisite or missing
runtime/display candidate. If the screenshot-consistency or world-pixel blocker
artifact is blocked, preserve that exact blocker. None of these artifacts is a
manual substitute for world-canvas pixel correctness, deployed installer
success, full world execution, grading, lesson completion, active Save behavior,
active Select Project behavior, or decoder behavior.

## Examples

### Review a successful observation

```bash
run_dir=qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/<timestamp>

python3 -m json.tool \
  "$run_dir/post-open-runtime-display-accessibility-evidence.json"

sed -n '1,120p' "$run_dir/status.txt"
```

Accept the implemented runtime/display run only when `status.txt` records:

```text
outcome=passed
runtimeDisplayAccessibilityStatus=observed
runtimeDisplayAccessibilityBlocker=none
```

After the visible-rendering shard lands, also require:

```text
controlledDisplayPixelStatus=observed
controlledDisplayPixelBlocker=none
```

and the JSON artifact records:

```text
status=observed
postOpenRuntimeDisplayAccessibilityObserved=true
runtimeDisplayCandidateCount > 0
blocker=none
```

Also review `tab-click-observation.json`,
`post-project-open-observation.json`, `launch.log`, `xvfb.log`,
`x-window-inventory.json`, and the captured screenshot as supporting evidence for
the project-open setup and run environment. After the visible-rendering shard
lands, also review `controlled-display-pixel-observation.json` for
screenshot-consistency evidence.

### Review a blocked run

```bash
python3 -m json.tool \
  qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/*/\
post-open-runtime-display-accessibility-evidence.json
```

Treat `status=blocked` as a machine-readable gap report. Do not replace it with
a manual claim that rendering, world execution, grading, lesson completion,
Save, Select Project, installer success, or decoder behavior passed.

### Review the planned screenshot-consistency artifact

```bash
run_dir=qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/<timestamp>

python3 -m json.tool "$run_dir/controlled-display-pixel-observation.json"
```

After the visible-rendering shard lands, accept the screenshot-consistency
artifact only when it records:

```text
schemaVersion=1
status=observed
blocker=none
claim=controlled-display-screenshot-consistency
claimScope=controlled-display-screenshot-consistency-only
screenshot.relativePath=screenshot.png
screenshot.dimensions.width > 0
screenshot.dimensions.height > 0
pixelObservation.status=observed
pixelObservation.sourceArtifact=screenshot.png
pixelObservation.dimensionsConsistentWithScreenshot=true
worldCanvasPixelTarget.identified=false
worldCanvasPixelTarget.missingUnblocker=reliable-run-window-world-canvas-pixel-sampling-target
```

If `visible-rendering-pixel-target-blocker.json` is present, keep it with the
run evidence. In this planned contract, it is required for the
`screenshot-metadata-unavailable` fallback and is the precise next blocker for
world-canvas pixel evidence, not a substitute for that evidence.

## Review rules

1. The JSON artifact is the runtime/display decision artifact.
2. Final scenario acceptance requires `status.txt` to record `outcome=passed`.
   After the visible-rendering shard lands, final acceptance also requires
   `controlledDisplayPixelStatus=observed`.
3. `status=observed` is accepted only with `blocker=none` and at least one
   runtime/display candidate.
4. `[PLANNED - Implementation Pending]`
   `controlled-display-pixel-observation.json` is accepted only as
   screenshot-consistency evidence, with
   `claimScope=controlled-display-screenshot-consistency-only`.
5. `[PLANNED - Implementation Pending]` `worldCanvasPixelTarget.identified=false`
   means the runner has not proved Run-window/world-canvas pixel correctness.
   The exact unblocker is
   `reliable-run-window-world-canvas-pixel-sampling-target`.
6. `status=blocked` is an honest blocked result, not a failed documentation
   claim and not a success substitute.
7. `tab-click-observation.json`, `post-project-open-observation.json`, launch,
   window, pixel, and post-open accessibility artifacts support this lane, but
   none of them expands it into full rendering correctness.
8. Generated evidence stays under `qa/outside-in/alice-desktop/evidence/` or a
   caller-provided evidence directory and remains uncommitted.

## Validation commands

Use the focused desktop QA checks when changing this lane:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh
bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh
```

The current contract tests cover schema/validator/runner parity, the
runtime/display artifact name, status fields, blocked fallback behavior, and the
narrow runtime/display claim token. The visible-rendering shard should add
`bash qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh`
to cover the planned screenshot-consistency artifact fields, world-canvas blocker
boundary, blocked fallback behavior, and narrow screenshot-consistency claim
tokens.

## Troubleshooting

| Symptom | Meaning | Action |
| --- | --- | --- |
| `pyatspi-not-installed` | The runner cannot find a Python interpreter with `pyatspi`. | Install `python3-pyatspi` or run in an environment where it is available to the selected interpreter. |
| `atk-wrapper-not-loaded` | Swing accessibility is not visible through AT-SPI. | Confirm `/usr/share/java/java-atk-wrapper.jar` exists and the `alice-ide-atk` launch path is active. |
| `post-open-window-not-observed` | The prerequisite project-open setup did not prove a post-open Alice window. | Review `post-project-open-observation.json`, `tab-click-observation.json`, and `launch.log`. |
| `runtime-display-accessible-candidate-not-found` | The probe reached the post-open accessibility tree but did not find an accepted runtime/display candidate. | Preserve the blocker artifact and use it to guide the next implementation step; do not broaden the evidence claim. |
| `x-server-unavailable`, `display-allocation-unavailable`, or `x-server-start-failed` | Controlled display setup failed before runtime/display observation. | Fix the display environment or collect the same scenario in a supported desktop QA environment. |
| `root-directory-property-missing`, `core-resources-distribution-prep-failed`, or `core-resources-distribution-not-created` | Alice root-directory launch preparation failed. | Review `root-directory-prep.json` and `root-directory-prep.log`; initialize/build the resources distribution before rerunning. |
| `license-acceptance-prep-failed` or `first-run-license-agreement-visible` | First-run license handling blocked launch automation. | Use `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` only in controlled QA launches and review `license-acceptance.json` plus `license-dialog.json`. |
| `screenshot-capture-failed`, `screenshot-captured-uniform-black`, or `screenshot-pixel-analysis-unavailable` | Controlled-display pixel evidence is unavailable, so `outcome=passed` is not valid even if the runtime/display JSON is observed. | Review `controlled-display-pixel-observation.json`, `screenshot.log`, and the screenshot artifact. |
| `screenshot-metadata-unavailable` | `[PLANNED - Implementation Pending]` The runner captured a screenshot but could not safely read dimensions from that same artifact. | Preserve `controlled-display-pixel-observation.json` with `status=blocked` and review `visible-rendering-pixel-target-blocker.json`; do not invent dimensions. |
| `reliable-run-window-world-canvas-pixel-sampling-target` | `[PLANNED - Implementation Pending]` The implementation has not identified a stable Run-window/world-canvas pixel target. | Treat `visible-rendering-pixel-target-blocker.json` as the exact next blocker for true rendered-world pixel correctness. |
