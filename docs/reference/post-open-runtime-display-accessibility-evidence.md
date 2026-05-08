# Post-open runtime/display accessibility evidence

This reference documents the implemented Alice desktop outside-in QA scenario
contract for post-open runtime/display accessibility evidence, controlled-display
screenshot-consistency evidence, Run-window world-canvas pixel sampling target
readiness, and the fail-closed pixel-sampling blocker after target readiness.

The implemented scenario proves two narrow claims: after Alice opens a project
through the existing supported launch/open path, the live accessibility tree
exposes at least one runtime/display candidate, and the controlled-display
screenshot artifact is internally consistent with the pixel metadata recorded
for that same artifact. It also records either one bounded screen-coordinate
world-canvas pixel sampling target or the exact blocker that prevents target
identification. When a target is ready, the current implementation records a
pixel-sampling blocker instead of claiming correctness because it has not sampled
rendered-world pixels inside that target. None of these claims prove
world-canvas pixel correctness, deployed installer success, full world execution,
grading, lesson completion, active Save behavior, active Select Project behavior,
or decoder behavior.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Configuration](#configuration)
- [Scenario interface](#scenario-interface)
- [Evidence API](#evidence-api)
- [Controlled-display screenshot-consistency API](#controlled-display-screenshot-consistency-api)
- [World-canvas pixel target readiness API](#world-canvas-pixel-target-readiness-api)
- [World-canvas pixel sampling blocker API](#world-canvas-pixel-sampling-blocker-api)
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
invokes the read-only runtime/display probe. The implemented probe records
bounded runtime/display candidate summaries: `name`, `role`, `path`,
`childCount`, `states`, `geometryStatus`, and `screenExtents`.

The controlled-display artifact records screenshot-consistency metadata for
that same screenshot and embeds `worldCanvasPixelTarget`. The target is
`target-ready` only when exactly one visible/showing runtime/display candidate
has positive screen-coordinate extents. Otherwise the runner writes
`visible-rendering-pixel-target-blocker.json` with the exact missing target and
next unblocker. The runner also writes
`visible-rendering-pixel-sampling-blocker.json` until it can sample and check
pixels inside a target-ready world-canvas region.

The probe, screenshot-consistency step, and target-readiness step are
observational. They do not click controls, save projects, select new starters,
execute worlds, grade work, inspect decoder output, mutate project data,
sample world-canvas pixels, compare screenshots, or infer rendered-world
correctness from a generic desktop screenshot.

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
`post-open-runtime-display-accessibility-evidence.json` for the runtime/display
accessibility decision and `controlled-display-pixel-observation.json` for the
controlled-display screenshot-consistency decision. Review
`worldCanvasPixelTarget.status`: `target-ready` means the runner found exactly
one repeatable screen-coordinate target for future pixel sampling; `blocked`
means `visible-rendering-pixel-target-blocker.json` names the exact missing
target and next unblocker. Review
`visible-rendering-pixel-sampling-blocker.json` for the next fail-closed seam;
`renderedWorldPixelsObserved=false` means no visible rendered-world correctness
claim was made.

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
| Final status artifact | `status.txt` with `outcome=passed` only when runtime/display accessibility, controlled-display screenshot consistency, and world-canvas pixel sampling are observed. Until sampling exists, target-ready runs remain `outcome=blocked` and point to the pixel-sampling blocker. |
| Probe-local status artifact | `runtime-display-accessibility-status.txt`, written by the probe before the runner writes final scenario status. Use it for debugging the probe result, not as the final pass/fail decision. |
| Screenshot-consistency artifact | `controlled-display-pixel-observation.json` with `schemaVersion=1`, `claimScope=controlled-display-screenshot-consistency`, relative screenshot path when captured, dimensions when metadata is available, pixel-observation metadata, `worldCanvasPixelTarget`, and explicit unsupported claims. |
| World-pixel blocker artifact | `visible-rendering-pixel-target-blocker.json`, written only when target identification is missing, invalid, or ambiguous. |
| Pixel-sampling blocker artifact | `visible-rendering-pixel-sampling-blocker.json`, written until rendered-world pixels inside the target-ready region are sampled and checked. |
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
visibleRenderingPixelTargetBlocker=visible-rendering-pixel-target-blocker.json
visibleRenderingPixelSamplingStatus=blocked
visibleRenderingPixelSamplingArtifact=visible-rendering-pixel-sampling-blocker.json
visibleRenderingPixelSamplingBlocker=<blocker>
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
      "geometryStatus": "available",
      "name": "Scene display",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": {
        "coordinateType": "screen",
        "x": 144,
        "y": 188,
        "width": 996,
        "height": 642
      },
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
      "geometryStatus": "available",
      "name": "Scene display",
      "path": "application/0/3",
      "role": "canvas",
      "screenExtents": {
        "coordinateType": "screen",
        "x": 144,
        "y": 188,
        "width": 996,
        "height": 642
      },
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
| `runtimeDisplayCandidates` | Bounded AT-SPI summaries for accepted candidates: `childCount`, `name`, `path`, `role`, `states`, `geometryStatus`, and `screenExtents`. |
| `scenario` | Scenario ID that produced the artifact. |
| `status` | `observed` or `blocked`. |
| `traversalErrors` | Non-fatal AT-SPI traversal errors collected while searching; empty when none were seen. |

The artifact must stay small and safe: no credentials, environment dumps,
arbitrary process dumps, unrelated desktop windows, saved project contents,
decoder output, grading state, lesson state, or world execution traces.

Candidate `geometryStatus` values are:

| Value | Meaning |
| --- | --- |
| `available` | The candidate exposes positive screen-coordinate extents and can be considered for target readiness. |
| `missing-component-interface` | AT-SPI did not expose the component interface needed to query extents. |
| `missing-extents` | The component interface was present, but screen extents were unavailable. |
| `invalid-extents` | Extents were present but not usable because width or height was non-positive, numeric fields were missing, or the coordinate type was not screen coordinates. |
| `ambiguous-candidates` | More than one visible/showing candidate had valid extents, so the runner refused to pick one without stronger metadata. |

Ambiguity is based on the count of eligible pixel-target candidates, not the
raw count of runtime/display candidates. A candidate is eligible only when it is
visible/showing and has `geometryStatus=available` with numeric screen extents
where `width > 0` and `height > 0`. Multiple candidates whose extents are
missing, zero-sized, negative, malformed, or non-screen-coordinate are not
ambiguous; they remain fail-closed geometry blockers.

## Controlled-display screenshot-consistency API

The screenshot-consistency artifact is:

```text
controlled-display-pixel-observation.json
```

It is a controlled-display evidence artifact, not a rendered-world oracle. The
runner derives screenshot dimensions and pixel-observation metadata from the
same relative screenshot artifact named in the JSON. It must not record absolute
paths, hostnames, usernames, environment variables, credentials, or binary image
data.

### Observed screenshot-consistency artifact

An observed result emits these fields. Field order is not part of the contract.

```json
{
  "schemaVersion": 1,
  "status": "observed",
  "blocker": "none",
  "claim": "controlled-display-pixels-observed-rendering-not-asserted",
  "claimScope": "controlled-display-screenshot-consistency",
  "screenshotStatus": "screenshot-captured",
  "screenshotFile": "screenshot.png",
  "screenshotPixelStatus": "non-black-pixels",
  "screenshot": {
    "path": "screenshot.png",
    "status": "screenshot-captured",
    "tool": "import",
    "metadataSource": "screenshot-pixels.txt.raw",
    "dimensions": {
      "width": 1280,
      "height": 900
    }
  },
  "pixelObservation": {
    "status": "non-black-pixels",
    "pixelsObserved": true,
    "consistentWithScreenshot": true,
    "detail": "Screenshot is 1280x900 with non-black pixel data."
  },
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
    "candidatePath": "application/0/3",
    "candidateName": "Scene display",
    "candidateRole": "canvas",
    "candidateStates": ["enabled", "showing", "visible"],
    "geometryStatus": "available",
    "screenExtents": {
      "coordinateType": "screen",
      "x": 144,
      "y": 188,
      "width": 996,
      "height": 642
    },
    "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents",
    "runtimeDisplayCandidateCount": 1
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "full-ui-automation",
    "world-execution",
    "grading",
    "save-behavior",
    "first-lesson-completion"
  ]
}
```

The minimum decision fields for accepting screenshot-consistency evidence are:

```json
{
  "schemaVersion": 1,
  "status": "observed",
  "blocker": "none",
  "claimScope": "controlled-display-screenshot-consistency",
  "screenshot": {
    "path": "screenshot.png",
    "dimensions": {
      "width": 1280,
      "height": 900
    }
  },
  "pixelObservation": {
    "status": "non-black-pixels",
    "pixelsObserved": true,
    "consistentWithScreenshot": true
  },
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "geometryStatus": "available",
    "screenExtents": {
      "coordinateType": "screen",
      "x": 144,
      "y": 188,
      "width": 996,
      "height": 642
    }
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness"
  ]
}
```

An observed screenshot-consistency result means only that the runner captured a
controlled-display screenshot, read its dimensions, and recorded pixel metadata
that points back to that same screenshot. A `target-ready`
`worldCanvasPixelTarget` means only that the runner found a repeatable
screen-coordinate region for future pixel sampling. It does not mean the runner
sampled pixels inside a world canvas or validated rendered-world content.

### Blocked screenshot-consistency artifact

When screenshot capture or metadata cannot be read safely, the controlled-display
artifact records a blocked or attempted result and the runner also writes the
world-pixel blocker artifact described below.

```json
{
  "schemaVersion": 1,
  "status": "blocked",
  "blocker": "screenshot-capture-failed",
  "claim": "no-visible-pixel-proof",
  "claimScope": "controlled-display-screenshot-consistency",
  "screenshot": {
    "path": null,
    "status": "not-attempted",
    "tool": null,
    "metadataSource": null,
    "dimensions": {
      "width": null,
      "height": null
    }
  },
  "pixelObservation": {
    "status": "not-attempted",
    "pixelsObserved": false,
    "consistentWithScreenshot": false
  },
  "worldCanvasPixelTarget": {
    "identified": false,
    "status": "blocked",
    "missingTarget": "run-window-world-canvas-screen-extents",
    "exactNextUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target",
    "geometryStatus": "missing-extents",
    "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
    "runtimeDisplayCandidateCount": 0
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "full-ui-automation",
    "world-execution",
    "grading",
    "save-behavior",
    "first-lesson-completion"
  ]
}
```

## World-canvas pixel target readiness API

The world-canvas pixel target readiness contract has two valid outcomes:

1. `controlled-display-pixel-observation.json` embeds
   `worldCanvasPixelTarget.identified=true` when exactly one visible/showing
   runtime/display candidate has valid positive screen-coordinate extents.
2. `visible-rendering-pixel-target-blocker.json` records the exact blocker when
   target identification is missing, invalid, or ambiguous.

The target-ready shape is:

```json
{
  "identified": true,
  "status": "target-ready",
  "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
  "candidatePath": "application/0/3",
  "candidateName": "Scene display",
  "candidateRole": "canvas",
  "geometryStatus": "available",
  "screenExtents": {
    "coordinateType": "screen",
    "x": 144,
    "y": 188,
    "width": 996,
    "height": 642
  },
  "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents"
}
```

Target-ready evidence is valid only when:

- `identified=true`
- `status=target-ready`
- `geometryStatus=available`
- `screenExtents.coordinateType=screen`
- `screenExtents.x`, `screenExtents.y`, `screenExtents.width`, and
  `screenExtents.height` are numbers
- `screenExtents.width > 0`
- `screenExtents.height > 0`
- exactly one accepted runtime/display candidate satisfies those rules

The runner does not default or coerce candidate geometry. Missing dimensions,
non-numeric dimensions, zero or negative `width`/`height`, and non-screen
coordinate extents make that candidate ineligible for target readiness and
ineligible for ambiguity counting.

Target-ready evidence is the handoff point for a later pixel sampler. That
sampler may crop or sample inside `screenExtents` from the same controlled
display screenshot. This shard does not define color expectations, image
baselines, visual diffs, grading rules, world execution assertions, or rendered
content pass/fail logic.

### World-canvas pixel target blocker

The blocker artifact is:

```text
visible-rendering-pixel-target-blocker.json
```

It is the machine-readable next blocker when the runner cannot identify a
reliable Run-window/world-canvas pixel sampling target:

```json
{
  "schemaVersion": 1,
  "status": "blocked",
  "blocker": "world-canvas-pixel-target-not-identified",
  "claimScope": "visible-rendering-world-canvas-pixel-target",
  "missingTarget": "run-window-world-canvas-screen-extents",
  "exactNextUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target",
  "sourceArtifact": "controlled-display-pixel-observation.json",
  "screenshotPath": "screenshot.png",
  "screenshotStatus": "screenshot-captured",
  "screenshotPixelStatus": "non-black-pixels",
  "runtimeDisplayCandidateCount": 1,
  "geometryStatus": "missing-extents",
  "worldCanvasPixelTarget": {
    "identified": false,
    "status": "blocked",
    "missingTarget": "run-window-world-canvas-screen-extents",
    "exactNextUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target",
    "geometryStatus": "missing-extents",
    "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
    "runtimeDisplayCandidateCount": 1
  },
  "claimScopeDetail": "target-readiness-only",
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "full-ui-automation",
    "world-execution",
    "grading",
    "save-behavior",
    "first-lesson-completion"
  ]
}
```

This artifact is not a failure to document. It is the machine-readable blocker
for the next implementation step. Do not replace it with generic screenshot
evidence or a success-shaped target object.

Target blocker `geometryStatus` values use the same enum as runtime/display
candidates: `missing-component-interface`, `missing-extents`,
`invalid-extents`, and `ambiguous-candidates`. The runner fails closed to this
artifact whenever the candidate geometry is absent, non-screen-coordinate,
non-positive, malformed, or ambiguous.

`ambiguous-candidates` is emitted only when more than one visible/showing
candidate has valid positive screen-coordinate extents. If two or more
runtime/display candidates exist but none is eligible, the blocker keeps a
non-ambiguous geometry status such as `missing-extents` or `invalid-extents`.
The blocked artifact must still keep `worldCanvasPixelTarget.identified=false`
and must not claim readiness, rendered-world visibility, or pixel correctness.

### Candidate geometry examples

The examples below show only the fields relevant to the geometry decision.

Multiple missing or invalid candidates are blocked, but not ambiguous:

```json
{
  "status": "blocked",
  "geometryStatus": "invalid-extents",
  "runtimeDisplayCandidateCount": 3,
  "worldCanvasPixelTarget": {
    "identified": false,
    "status": "blocked",
    "geometryStatus": "invalid-extents",
    "runtimeDisplayCandidateCount": 3
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness"
  ]
}
```

The same raw candidate count becomes ambiguous only when more than one
visible/showing candidate has valid positive screen-coordinate extents:

```json
{
  "status": "blocked",
  "geometryStatus": "ambiguous-candidates",
  "runtimeDisplayCandidateCount": 3,
  "worldCanvasPixelTarget": {
    "identified": false,
    "status": "blocked",
    "geometryStatus": "ambiguous-candidates",
    "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness"
  ]
}
```

Both examples are blockers. Neither example is a substitute for a pixel sampler,
visible-rendering proof, rendered-world oracle, or world-canvas pixel
correctness claim.

## World-canvas pixel sampling blocker API

The next seam after target readiness is:

```text
visible-rendering-pixel-sampling-blocker.json
```

The artifact is intentionally blocked until the runner samples pixels inside a
target-ready `screenExtents` region and checks them against an explicit
rendered-world expectation. Its source artifact is always
`controlled-display-pixel-observation.json`; target metadata inside that source
is a prerequisite for sampling, not proof that rendered-world pixels were
observed.

The executable contract treats the source artifact as untrusted input. Missing,
unreadable, malformed, non-object, or semantically invalid source JSON must
produce a blocked pixel-sampling artifact, never a success-shaped rendering
claim. A controlled-display artifact with `worldCanvasPixelTarget.status`
`target-ready` can only move the blocker to
`world-canvas-pixel-sampling-not-implemented`. It cannot set
`renderedWorldPixelsObserved=true`, increase `sampleCount`, or name a
`samplingMethod`.

A target-ready blocker has this shape:

```json
{
  "schemaVersion": 1,
  "status": "blocked",
  "blocker": "world-canvas-pixel-sampling-not-implemented",
  "claimScope": "visible-rendering-world-canvas-pixel-sampling",
  "claimScopeDetail": "target-ready-sampling-not-observed",
  "sourceArtifact": "controlled-display-pixel-observation.json",
  "prerequisiteTargetStatus": "target-ready",
  "exactNextUnblocker": "sample-run-window-world-canvas-pixels",
  "renderedWorldPixelsObserved": false,
  "sampleCount": 0,
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "screenExtents": {
      "coordinateType": "screen",
      "x": 144,
      "y": 188,
      "width": 996,
      "height": 642
    }
  },
  "pixelSampling": {
    "status": "blocked",
    "blocker": "world-canvas-pixel-sampling-not-implemented",
    "pixelsSampled": false,
    "sampleCount": 0,
    "samplingMethod": null
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness"
  ]
}
```

The minimum decision fields for accepting this blocker proof are:

```json
{
  "schemaVersion": 1,
  "status": "blocked",
  "blocker": "world-canvas-pixel-sampling-not-implemented",
  "claimScope": "visible-rendering-world-canvas-pixel-sampling",
  "claimScopeDetail": "target-ready-sampling-not-observed",
  "sourceArtifact": "controlled-display-pixel-observation.json",
  "prerequisiteTargetStatus": "target-ready",
  "renderedWorldPixelsObserved": false,
  "sampleCount": 0,
  "pixelSampling": {
    "status": "blocked",
    "pixelsSampled": false,
    "sampleCount": 0,
    "samplingMethod": null
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness"
  ]
}
```

The `unsupportedClaims` values above are an include-at-least set. A valid
artifact may list additional unsupported claims, but it must not omit these
three exclusions while pixel sampling remains blocked.

If target selection is blocked, the same artifact uses
`blocker=world-canvas-pixel-target-not-ready` and
`prerequisiteTargetStatus=blocked` or `unavailable`. In both cases,
`renderedWorldPixelsObserved=false`, `pixelsSampled=false`, and `sampleCount=0`
are required. Do not treat the controlled-display screenshot or target-ready
geometry as rendered-world pixel evidence.

When the source artifact is malformed, absent, non-object, or semantically
invalid for the controlled-display screenshot-consistency contract, the runner
records `prerequisiteTargetStatus=unavailable`, preserves
`sourceArtifact=controlled-display-pixel-observation.json`, and keeps the same
non-claim fields: `renderedWorldPixelsObserved=false`,
`pixelSampling.pixelsSampled=false`, `sampleCount=0`, and
`pixelSampling.samplingMethod=null`. Reviewers should treat that artifact as the
exact blocker for the source-artifact boundary, not as evidence that the world
canvas is invisible or incorrectly rendered.

## Review workflow

Use this review sequence for every run:

1. Open `status.txt` and confirm it points to
   `post-open-runtime-display-accessibility-evidence.json`.
2. Open the JSON decision artifact and read `status`, `blocker`,
   `postOpenRuntimeDisplayAccessibilityObserved`,
   `runtimeDisplayCandidateCount`, and `runtimeDisplayCandidates`.
3. Open `controlled-display-pixel-observation.json` and confirm
    `schemaVersion=1`, `status=observed`,
    `claimScope=controlled-display-screenshot-consistency`, a relative screenshot
    path, non-null screenshot dimensions,
    `pixelObservation.consistentWithScreenshot=true`, `worldCanvasPixelTarget`,
    and explicit `unsupportedClaims`.
4. Confirm the target path. If `worldCanvasPixelTarget.status=target-ready`,
    `screenExtents.coordinateType=screen` and width/height are positive. If
    `worldCanvasPixelTarget.status=blocked`,
    `visible-rendering-pixel-target-blocker.json` is present and names
    `missingTarget=run-window-world-canvas-screen-extents` plus
    `exactNextUnblocker=reliable-run-window-world-canvas-pixel-sampling-target`.
5. Review `tab-click-observation.json` and
    `post-project-open-observation.json` to understand the supporting project-open
    setup.
6. Accept the implemented runtime/display and screenshot-consistency setup only
     when `runtimeDisplayAccessibilityStatus=observed` and
     `controlledDisplayPixelStatus=observed`; then review
     `visible-rendering-pixel-sampling-blocker.json` as the honest next blocker
     unless `visibleRenderingPixelSamplingStatus=observed` is implemented in a
     future slice.

If the decision artifact is `status=blocked`, preserve it as the run result. A
blocked artifact is useful evidence about the missing prerequisite or missing
runtime/display candidate. If the screenshot-consistency artifact is blocked or
the world-pixel target blocker artifact is present, preserve that exact blocker.
None of these artifacts is a manual substitute for world-canvas pixel
correctness, deployed installer success, full world execution, grading, lesson
completion, active Save behavior, active Select Project behavior, or decoder
behavior.

Target-ready metadata is accepted only as pixel sampling target readiness. It is
not a rendering correctness assertion, and target-ready runs remain blocked at
the pixel-sampling seam until rendered-world pixels are sampled and checked.

## Examples

### Review observed setup with blocked pixel sampling

```bash
run_dir=qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/<timestamp>

python3 -m json.tool \
  "$run_dir/post-open-runtime-display-accessibility-evidence.json"

sed -n '1,120p' "$run_dir/status.txt"
```

The current target-ready path remains blocked at the pixel-sampling seam. Review
it only as runtime/display and screenshot-consistency evidence when `status.txt`
records:

```text
outcome=blocked
runtimeDisplayAccessibilityStatus=observed
runtimeDisplayAccessibilityBlocker=none
controlledDisplayPixelStatus=observed
controlledDisplayPixelBlocker=none
visibleRenderingPixelTargetStatus=<target-ready|blocked>
visibleRenderingPixelTargetArtifact=<controlled-display-pixel-observation.json|visible-rendering-pixel-target-blocker.json>
visibleRenderingPixelSamplingStatus=blocked
visibleRenderingPixelSamplingArtifact=visible-rendering-pixel-sampling-blocker.json
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
the project-open setup and run environment. Review
`controlled-display-pixel-observation.json` for screenshot-consistency evidence
and the embedded `worldCanvasPixelTarget`. If the target is blocked, review
`visible-rendering-pixel-target-blocker.json` for the exact next unblocker. If
the target is ready, review `visible-rendering-pixel-sampling-blocker.json` and
confirm `renderedWorldPixelsObserved=false`.

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

### Review the screenshot-consistency artifact

```bash
run_dir=qa/outside-in/alice-desktop/evidence/post-open-runtime-display/\
alice-desktop-post-open-runtime-display-accessibility-evidence/<timestamp>

python3 -m json.tool "$run_dir/controlled-display-pixel-observation.json"
```

Accept the screenshot-consistency artifact only when it records:

```text
schemaVersion=1
status=observed
blocker=none
claimScope=controlled-display-screenshot-consistency
screenshot.path=screenshot.png
screenshot.dimensions.width > 0
screenshot.dimensions.height > 0
pixelObservation.status=non-black-pixels
pixelObservation.pixelsObserved=true
pixelObservation.consistentWithScreenshot=true
worldCanvasPixelTarget.status=<target-ready|blocked>
```

If the target is blocked, keep `visible-rendering-pixel-target-blocker.json` with
the run evidence. It is the precise blocker for target readiness, not a
substitute for target-ready evidence.

## Review rules

1. The JSON artifact is the runtime/display decision artifact.
2. Final visible-rendering acceptance requires `status.txt` to record
   `outcome=passed`, `controlledDisplayPixelStatus=observed`, and
   `visibleRenderingPixelSamplingStatus=observed`.
3. `status=observed` is accepted only with `blocker=none` and at least one
   runtime/display candidate.
4. `controlled-display-pixel-observation.json` is accepted only as
   screenshot-consistency evidence, with
   `claimScope=controlled-display-screenshot-consistency`.
5. `worldCanvasPixelTarget.identified=true` means only that the runner has
   identified a repeatable screen-coordinate target for future pixel sampling. It
   does not prove Run-window/world-canvas pixel correctness.
6. `visible-rendering-pixel-target-blocker.json` means target readiness is
   blocked and the exact unblocker is
   `reliable-run-window-world-canvas-pixel-sampling-target`.
7. `visible-rendering-pixel-sampling-blocker.json` means the source artifact was
   read only as a sampling prerequisite and target readiness was not enough to
   prove visible rendering; no rendered-world pixels were sampled.
8. `status=blocked` is an honest blocked result, not a failed documentation
   claim and not a success substitute.
9. `tab-click-observation.json`, `post-project-open-observation.json`, launch,
   window, pixel, and post-open accessibility artifacts support this lane, but
   none of them expands it into full rendering correctness.
10. Generated evidence stays under `qa/outside-in/alice-desktop/evidence/` or a
    caller-provided evidence directory and remains uncommitted.

## Validation commands

Use the focused desktop QA checks when changing this lane:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-contract.sh
bash qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh
bash qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
```

The current contract tests cover schema/validator/runner parity, the
runtime/display artifact name, status fields, blocked fallback behavior,
runtime/display candidate summaries, and the narrow runtime/display claim token.
The visible-rendering evidence contract test covers the screenshot-consistency
artifact fields, `target-ready` shape, exact target-blocker shape, the
pixel-sampling blocker/source-artifact boundary, geometry metadata, blocked
fallback behavior, forbidden overclaiming language, and narrow
screenshot-consistency claim tokens.

## Troubleshooting

| Symptom | Meaning | Action |
| --- | --- | --- |
| `pyatspi-not-installed` | The runner cannot find a Python interpreter with `pyatspi`. | Install `python3-pyatspi` or run in an environment where it is available to the selected interpreter. |
| `atk-wrapper-not-loaded` | Swing accessibility is not visible through AT-SPI. | Confirm `/usr/share/java/java-atk-wrapper.jar` exists and the `alice-ide-atk` launch path is active. |
| `post-open-window-not-observed` | The prerequisite project-open setup did not prove a post-open Alice window. | Review `post-project-open-observation.json`, `tab-click-observation.json`, and `launch.log`. |
| `runtime-display-accessible-candidate-not-found` | The probe reached the post-open accessibility tree but did not find an accepted runtime/display candidate. | Preserve the blocker artifact and use it to guide the next implementation step; do not broaden the evidence claim. |
| `missing-component-interface` | A runtime/display-like candidate did not expose AT-SPI component extents. | Preserve `visible-rendering-pixel-target-blocker.json`; the next implementation step is to expose or discover screen extents for the Run-window world canvas. |
| `missing-extents` | A candidate exposed component metadata but not usable screen extents. | Preserve the blocker and inspect runtime/display candidate metadata before adding any pixel sampler. |
| `invalid-extents` | Candidate extents were malformed, non-screen-coordinate, or non-positive. | Fix target geometry collection before sampling pixels; do not coerce invalid values into a target-ready artifact. |
| `ambiguous-candidates` | More than one visible/showing candidate had valid extents. | Add deterministic target metadata or a stricter candidate rule before treating one region as the world canvas. |
| `x-server-unavailable`, `display-allocation-unavailable`, or `x-server-start-failed` | Controlled display setup failed before runtime/display observation. | Fix the display environment or collect the same scenario in a supported desktop QA environment. |
| `root-directory-property-missing`, `core-resources-distribution-prep-failed`, or `core-resources-distribution-not-created` | Alice root-directory launch preparation failed. | Review `root-directory-prep.json` and `root-directory-prep.log`; initialize/build the resources distribution before rerunning. |
| `license-acceptance-prep-failed` or `first-run-license-agreement-visible` | First-run license handling blocked launch automation. | Use `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` only in controlled QA launches and review `license-acceptance.json` plus `license-dialog.json`. |
| `screenshot-capture-failed`, `screenshot-captured-uniform-black`, or `screenshot-pixel-analysis-unavailable` | Controlled-display pixel evidence is unavailable, so `outcome=passed` is not valid even if the runtime/display JSON is observed. | Review `controlled-display-pixel-observation.json`, `screenshot.log`, and the screenshot artifact. |
| `world-canvas-pixel-sampling-not-implemented` | Target readiness exists, but the runner has not sampled pixels inside the target region. | Preserve `visible-rendering-pixel-sampling-blocker.json`; do not claim rendered-world correctness until sampled pixels are checked. |
| `screenshot-metadata-unavailable` | The runner captured a screenshot but could not safely read dimensions from that same artifact. | Preserve `controlled-display-pixel-observation.json` with a non-observed status and review `visible-rendering-pixel-target-blocker.json`; do not invent dimensions. |
| `reliable-run-window-world-canvas-pixel-sampling-target` | The implementation has not identified a stable Run-window/world-canvas pixel target. | Treat `visible-rendering-pixel-target-blocker.json` as the exact next blocker for target readiness. |
