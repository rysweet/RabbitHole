# Visible rendering evidence nonclaim contract

This reference documents the Alice desktop outside-in QA contract that keeps
render evidence separate from visible correctness claims.

The contract accepts render-adjacent evidence only as proof that an artifact was
produced or a bounded capture happened. Screenshots, generated files,
controlled-display pixel metadata, target-ready geometry, and sampled RGBA
values do not establish visible correctness. They remain evidence about capture,
target readiness, or raw sampling unless a separate visual-correctness
observation schema is present and accepted by a reviewed contract.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Configuration](#configuration)
- [Artifact API](#artifact-api)
- [Observation evidence boundary](#observation-evidence-boundary)
- [Current-head nonclaim review](#current-head-nonclaim-review)
- [Allowed wording](#allowed-wording)
- [Rejected wording](#rejected-wording)
- [Examples](#examples)
- [Review checklist](#review-checklist)

## Scope

The executable contract is:

```text
qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
```

It checks the render-evidence boundary for the post-open runtime/display QA
lane and its current static target fixtures:

```text
qa/outside-in/alice-desktop/tests/fixtures/visible-rendering/
```

Current checked fixtures:

```text
qa/outside-in/alice-desktop/tests/fixtures/visible-rendering/world-canvas-pixel-target-ready.json
qa/outside-in/alice-desktop/tests/fixtures/visible-rendering/world-canvas-pixel-target-blocked.json
```

Additional checked contract fixtures for the nonclaim wording lane:

```text
qa/outside-in/alice-desktop/tests/fixtures/visible-rendering/valid-nonclaim-render-evidence.json
qa/outside-in/alice-desktop/tests/fixtures/visible-rendering/invalid-overclaim-render-evidence.json
qa/outside-in/alice-desktop/tests/fixtures/visible-rendering/valid-negated-nonclaim-render-wording.txt
```

The contract covers these evidence classes:

| Evidence | Accepted meaning | Nonclaim boundary |
| --- | --- | --- |
| Generated render file | A file was produced at the expected path. | The rendered result is not judged correct. |
| Screenshot | A desktop image was captured. | The image is not visual correctness proof. |
| Screenshot pixel metadata | The screenshot has reported dimensions or pixel statistics. | Pixel metadata is not visual correctness proof. |
| `worldCanvasPixelTarget` | A target is ready, blocked, invalid, missing, or ambiguous. | Target readiness is not visual correctness proof. |
| `visible-rendering-pixel-observation.json` | Raw checked RGBA samples were collected inside one validated target. | Sampled pixels do not establish visible correctness. |
| `visible-rendering-pixel-sampling-blocker.json` | Sampling is blocked with a named blocker. | A blocker is not a substitute for visible correctness proof. |

The contract does not execute Alice worlds, simulations, visual comparison, or
manual visual validation flows. It uses static fixtures, synthetic JSON, runner
writer seams, and fail-closed artifact checks.

## Usage

Run the focused contract from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
```

When reviewing changes to target-scoped pixel sampling, also run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-world-canvas-pixel-sampler-contract.sh
```

These checks are contract checks. They validate static fixtures and generated
contract-shaped artifacts. They do not launch a world or certify visual output.

## Configuration

| Setting | Required value | Meaning |
| --- | --- | --- |
| `NODE_OPTIONS` | `--max-old-space-size=32768` | Standard memory setting for Alice desktop QA scripts. |
| `ALICE_QA_WORLD_CANVAS_PIXEL_SAMPLER` | Optional sampler path in sampler tests | Supplies a sampler implementation for target-scoped raw RGBA capture tests. The sampler output is rejected if it reports correctness instead of raw capture. |

There is no configuration flag that allows render evidence to become visible
correctness evidence. Any such change requires a separate reviewed contract with
visual-correctness observation fields.

## Artifact API

The JSON snippets below focus on the boundary fields this contract protects.
Executable artifacts may contain additional runner metadata, but they must not
weaken these nonclaim fields.

### `controlled-display-pixel-observation.json`

This artifact records controlled-display screenshot consistency. It must keep
the narrow claim scope:

```json
{
  "schemaVersion": 1,
  "status": "observed",
  "claim": "controlled-display-pixels-observed-rendering-not-asserted",
  "claimScope": "controlled-display-screenshot-consistency",
  "pixelObservation": {
    "status": "non-black-pixels",
    "pixelsObserved": true,
    "consistentWithScreenshot": true
  },
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness"
  ]
}
```

`status=observed` means screenshot-consistency evidence was observed. It does
not mean the rendered result was visibly correct.

### `visible-rendering-pixel-target-blocker.json`

This artifact records why target readiness is blocked:

```json
{
  "schemaVersion": 1,
  "status": "blocked",
  "claimScope": "visible-rendering-world-canvas-pixel-target",
  "claimScopeDetail": "target-readiness-only",
  "missingTarget": "run-window-world-canvas-screen-extents",
  "exactNextUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target",
  "worldCanvasPixelTarget": {
    "identified": false,
    "status": "blocked"
  }
}
```

The blocker names the missing target or ambiguity. It does not become evidence
that the visible rendering is correct.

### `visible-rendering-pixel-observation.json`

This artifact records bounded raw pixel sampling only:

```json
{
  "schemaVersion": 1,
  "status": "observed",
  "blocker": "none",
  "blockerDetail": "",
  "claim": "run-window-world-canvas-target-sampled-rendering-correctness-not-asserted",
  "claimScope": "visible-rendering-world-canvas-pixel-sampling",
  "claimScopeDetail": "target-scoped-raw-pixel-observation-only",
  "sourceArtifact": "controlled-display-pixel-observation.json",
  "targetSourceArtifact": "post-open-runtime-display-accessibility-evidence.json",
  "renderedWorldPixelsObserved": true,
  "visibleRenderingCorrectnessEstablished": false,
  "prerequisiteTargetStatus": "target-ready",
  "sampleCount": 1,
  "samplingMethod": "target-scoped-controlled-display-raw-rgba",
  "samples": [
    {
      "checked": true,
      "point": {
        "x": 320,
        "y": 240
      },
      "rgba": [32, 48, 64, 255]
    }
  ],
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "geometryStatus": "available",
    "screenExtents": {
      "coordinateType": "screen",
      "x": 160,
      "y": 120,
      "width": 320,
      "height": 240
    },
    "selectionRule": "single-visible-showing-runtime-display-candidate-with-valid-screen-extents",
    "sourceArtifact": "post-open-runtime-display-accessibility-evidence.json"
  },
  "pixelSampling": {
    "status": "observed",
    "pixelsSampled": true,
    "samplesChecked": true,
    "sampleCount": 1,
    "samplingMethod": "target-scoped-controlled-display-raw-rgba",
    "samplePoints": [
      {
        "checked": true,
        "point": {
          "x": 320,
          "y": 240
        },
        "rgba": [32, 48, 64, 255]
      }
    ],
    "samplePointRule": "inside-target-bounds-only",
    "correctnessCheck": "not-performed"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness"
  ]
}
```

`renderedWorldPixelsObserved=true` means raw pixels were sampled inside the
validated target. It must be paired with
`visibleRenderingCorrectnessEstablished=false` and
`pixelSampling.correctnessCheck=not-performed`. `correctnessCheck` is not a
top-level artifact invariant in the current runner shape.

### `visible-rendering-pixel-sampling-blocker.json`

This artifact records fail-closed sampling outcomes:

```json
{
  "schemaVersion": 1,
  "status": "blocked",
  "claimScope": "visible-rendering-world-canvas-pixel-sampling",
  "claimScopeDetail": "target-selection-blocked",
  "renderedWorldPixelsObserved": false,
  "visibleRenderingCorrectnessEstablished": false,
  "sampleCount": 0,
  "blocker": "world-canvas-pixel-target-not-ready",
  "worldCanvasPixelTarget": {
    "identified": false,
    "status": "blocked"
  },
  "pixelSampling": {
    "status": "blocked",
    "blocker": "world-canvas-pixel-target-not-ready",
    "pixelsSampled": false,
    "sampleCount": 0,
    "samplingMethod": null,
    "correctnessCheck": "not-performed"
  }
}
```

Blocked sampling artifacts are expected when target selection, sampler
availability, sample completeness, or sampler wording fails the contract.

## Observation evidence boundary

The word `observed` is not enough by itself. In this contract,
render-only observation fields establish capture, target readiness, or raw
sampling only; they do not establish visible correctness.

Render-only artifacts use these explicit observation fields:

| Artifact | Explicit observation fields | Established state |
| --- | --- | --- |
| `controlled-display-pixel-observation.json` | `screenshot.status`, `screenshot.dimensions`, `pixelObservation.pixelsObserved`, `pixelObservation.consistentWithScreenshot` | Screenshot capture and screenshot-consistency evidence only. |
| `controlled-display-pixel-observation.json` | `worldCanvasPixelTarget.identified`, `worldCanvasPixelTarget.status`, `worldCanvasPixelTarget.screenExtents`, `worldCanvasPixelTarget.selectionRule` | Target readiness for later raw sampling only. |
| `visible-rendering-pixel-observation.json` | `samples[].checked`, `samples[].point`, `samples[].rgba`, `sampleCount`, `samplingMethod` | Raw target-scoped RGBA sampling only. |
| `visible-rendering-pixel-sampling-blocker.json` | `blocker`, `blockerDetail`, `exactNextUnblocker`, `prerequisiteTargetStatus` | A named blocked prerequisite only. |

Render-only observation fields can support these states:

| Field | Allowed render-only value |
| --- | --- |
| `status` | `observed` or `blocked` |
| `renderedWorldPixelsObserved` | `true` only for checked raw target-scoped samples |
| `visibleRenderingCorrectnessEstablished` | `false` |
| `pixelSampling.correctnessCheck` | `not-performed` for observed sampling artifacts and blocked sampling artifacts |
| `unsupportedClaims` | Includes visible correctness and rendered-world correctness claim tokens |

Render-only evidence must not set a positive visible-correctness state, including
alternate success-shaped fields such as visible or visual correctness status
values. A future positive state requires a different reviewed contract that
defines visual-correctness observation fields, the observer, the observation
method, the accepted scope, and the exact criteria. This render-evidence
contract rejects artifacts that try to derive that state from file existence,
screenshots, generated output, target readiness, or sampled pixels.

## Current-head nonclaim review

Current-head review evidence uses the same nonclaim boundary as static fixtures.
The reviewer accepts only evidence generated after the PR branch or PR ref has
been reconciled with `origin/develop`. Stale screenshots, stale controlled-display
artifacts, stale sampled pixels, and stale generated files may be retained only as
superseded comparison material; they are not current review evidence.

A current-head render-adjacent artifact is reviewable when it is paired with
external review metadata for the reconciled `HEAD`, the scenario ID, the run
directory timestamp, and one of these bounded decisions. The current runner does
not emit Git SHAs into `environment.txt`; reviewers record the commit coordinates
in PR notes, review notes, or CI artifact metadata that points at the run
directory.

| Decision | Required nonclaim wording |
| --- | --- |
| Screenshot captured | The controlled-display screenshot was captured; visible correctness was not checked. |
| Target ready | Exactly one target-ready geometry was identified; target readiness is not visual correctness. |
| Pixels sampled | Raw target-scoped RGBA values were sampled; `pixelSampling.correctnessCheck=not-performed`. |
| Blocked | The artifact names the exact blocker and next unblocker; the blocked run is not a substitute for correctness evidence. |

Review summaries, readiness notes, PR descriptions, and documentation snippets
may say that runtime/display, target-readiness, or raw sampling signals were
observed only when the current-head artifacts contain those exact signals. They
must not say or imply that Alice rendering, world-canvas pixels, full UI behavior,
accessibility compliance, world execution, Save behavior, Select Project
behavior, grading, or lesson completion is correct.

## Allowed wording

Use wording that states the boundary directly:

```text
Render evidence shows the artifact was produced.
Screenshot evidence shows a capture was produced.
Sampled pixels show raw RGBA values were collected inside the validated target.
This evidence does not establish visible correctness.
The correctness check was not performed.
Visible rendering correctness remains an unsupported claim for this lane.
```

Negated and nonclaim language is valid when it keeps the boundary explicit.

## Rejected wording

Reject wording that turns render evidence into correctness evidence. The
contract fails artifacts, fixtures, generated text, or documentation snippets
that make a positive visible-correctness assertion from:

- generated files,
- screenshots,
- screenshot pixel statistics,
- target-ready geometry,
- raw RGBA samples,
- sampler success,
- blocked artifacts.

The same rejection applies when a sampler or fixture sets a success-shaped
correctness field without a separate visual-correctness observation contract.

## Examples

### Valid nonclaim render evidence

```json
{
  "schemaVersion": 1,
  "status": "observed",
  "blocker": "none",
  "claimScope": "visible-rendering-world-canvas-pixel-sampling",
  "claimScopeDetail": "target-scoped-raw-pixel-observation-only",
  "sourceArtifact": "controlled-display-pixel-observation.json",
  "renderedWorldPixelsObserved": true,
  "visibleRenderingCorrectnessEstablished": false,
  "sampleCount": 1,
  "samplingMethod": "target-scoped-controlled-display-raw-rgba",
  "samples": [
    {
      "checked": true,
      "point": {
        "x": 320,
        "y": 240
      },
      "rgba": [32, 48, 64, 255]
    }
  ],
  "worldCanvasPixelTarget": {
    "identified": true,
    "status": "target-ready",
    "geometryStatus": "available",
    "screenExtents": {
      "coordinateType": "screen",
      "x": 160,
      "y": 120,
      "width": 320,
      "height": 240
    }
  },
  "pixelSampling": {
    "status": "observed",
    "pixelsSampled": true,
    "samplesChecked": true,
    "sampleCount": 1,
    "samplingMethod": "target-scoped-controlled-display-raw-rgba",
    "samplePoints": [
      {
        "checked": true,
        "point": {
          "x": 320,
          "y": 240
        },
        "rgba": [32, 48, 64, 255]
      }
    ],
    "correctnessCheck": "not-performed"
  },
  "unsupportedClaims": [
    "world-canvas-pixel-correctness",
    "full-visible-rendering-correctness",
    "rendered-world-correctness"
  ]
}
```

This example is valid because it claims only raw pixel observation and explicitly
keeps visible correctness unestablished.

### Invalid overclaim render evidence

An artifact is invalid when it records raw render evidence and also sets a
positive visible-correctness state. For example, a sampled-pixel artifact is
rejected if it changes `visibleRenderingCorrectnessEstablished` away from
`false`, changes `pixelSampling.correctnessCheck` away from `not-performed`, or
omits the unsupported-claim tokens that keep visible correctness outside the
lane.

That shape is invalid because it derives visible correctness from render
evidence instead of a visual-correctness observation contract.

### Valid negated wording

```text
The screenshot was captured and raw target-scoped pixels were sampled.
This evidence does not establish visible correctness.
The visible correctness check was not performed.
```

This wording is valid because it records evidence production and states the
nonclaim boundary.

## Review checklist

Use this checklist for every render-evidence change:

1. Confirm the evidence is static fixture data, synthetic contract data, or a
   runner writer seam result.
2. Confirm no new contract check executes Alice worlds, simulations, visual
   comparison, or manual visual validation flows.
3. Confirm render-only artifacts keep
   `visibleRenderingCorrectnessEstablished=false`.
4. Confirm observed render-only sampling artifacts use
   `pixelSampling.correctnessCheck=not-performed`.
5. Confirm `unsupportedClaims` includes visible correctness and rendered-world
   correctness tokens.
6. Confirm positive correctness wording is rejected unless a separate
   visual-correctness observation schema is present.
7. Confirm negated wording such as "does not establish visible correctness" is
   accepted.
