---
title: Default Open 3D Asset Workflow Test Reference
description: Reference for the Xvfb-backed RabbitHole integration test that proves the bundled Bunny asset works in real Alice workflows without Sims assets.
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: reference
related:
  - ../howto/verify-default-open-3d-asset-workflow.md
  - ../howto/use-open-3d-assets.md
  - ./ci-gui-eatme-xvfb-validation.md
---

# Default open 3D asset workflow test reference

`RabbitHoleDefaultOpenAssetWorkflowTest` is the executable integration contract
for RabbitHole's default open gallery asset path. It verifies that the bundled
Bunny asset can be placed, rendered, manipulated, saved, reopened, and run as a
real Alice scene object under Xvfb without Sims assets.

## Test identity

| Item | Value |
| --- | --- |
| Test class | `org.alice.tools.RabbitHoleDefaultOpenAssetWorkflowTest` |
| Module | `core/ide` |
| Asset | `BunnyResource.DEFAULT` |
| Gallery identifier | `alice-gallery://animals/bunny` |
| Required display mode | `java.awt.headless=false` with a usable X display |
| Sims mode | `-DincludeSims=false` |
| Required-validation property | `-Drabbithole.defaultAssetWorkflow.required=true` |

## Maven contract

Required CI-compatible command:

```bash
mvn --settings .github/maven/jogamp-ci-settings.xml \
  -pl core/ide -am \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=false \
  -DincludeSims=false \
  -Drabbithole.defaultAssetWorkflow.required=true \
  -Dtest=RabbitHoleDefaultOpenAssetWorkflowTest \
  test
```

Use the shared Xvfb harness on machines without a physical display:

```bash
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds 1800 \
  --expect success \
  -- \
  mvn --settings .github/maven/jogamp-ci-settings.xml \
    -pl core/ide -am \
    -Dinstall4j.skip \
    -Dcheckstyle.skip \
    -Djava.awt.headless=false \
    -DincludeSims=false \
    -Drabbithole.defaultAssetWorkflow.required=true \
    -Dtest=RabbitHoleDefaultOpenAssetWorkflowTest \
    test
```

## Configuration

| Setting | Required value | Purpose |
| --- | --- | --- |
| `java.awt.headless` | `false` | Exercises the display-backed Alice render path. |
| `includeSims` | `false` | Proves the default workflow does not need Sims/nonfree assets. |
| `rabbithole.defaultAssetWorkflow.required` | `true` in CI and release validation | Converts missing display/render support from a skip into a failure. |
| `test` | `RabbitHoleDefaultOpenAssetWorkflowTest` | Runs the focused integration contract. |
| Maven settings | `.github/maven/jogamp-ci-settings.xml` | Uses the CI-approved JOGL/GlueGen repository mirror behavior. |
| `NODE_OPTIONS` | `--max-old-space-size=32768` when invoking QA wrappers around the lane | Preserves the repository QA memory default. |

When `rabbithole.defaultAssetWorkflow.required` is not `true`, ordinary headless
developer test runs may skip the display-dependent assertions. CI and
merge-readiness validation set the property to `true`.

## Workflow contract

The test uses repository-owned Alice seams instead of isolated mocks.

| Phase | Contract |
| --- | --- |
| Starter project | Load the checked-in starter Alice project used by the Eatme placement path. |
| Placement | Add `alice-gallery://animals/bunny` through `EatmePlaceObject.run` and the existing application/project placement infrastructure. |
| First reopen | Read the placed project back through Alice project I/O before applying persistent transform assertions. |
| Manipulation | Persist 3D transform and scale changes through Alice setup-statement infrastructure so the changes survive project serialization. |
| Save/reopen | Save the manipulated project and reopen it from disk. |
| Runtime | Create a real `RunProgramContext` for the reopened project and obtain the runtime Bunny object. |
| Rendering | Capture the scene from `getOnscreenRenderTarget().getSynchronousImageCapturer().getColorBuffer()` and assert visible pixel evidence. |

## Assertion contract

| Assertion | Failure means |
| --- | --- |
| The Bunny field/resource exists after placement | Gallery placement or project mutation regressed. |
| The workflow uses `BunnyResource.DEFAULT` / `alice-gallery://animals/bunny` | The test would no longer prove the default open asset path. |
| The captured Bunny render differs from the starter render | The asset may load but be invisible, off-camera, unrendered, or rendered as background only. |
| Pixel variance/delta exceeds the minimum visible-image threshold | The capture path produced an empty, flat, or false-positive image. |
| Runtime Bunny position and scale match the manipulated state | 3D manipulation did not persist or runtime object state did not hydrate correctly. |
| The transformed and scaled Bunny survives save/reopen | Project serialization/deserialization regressed. |
| The world run completes under Xvfb | Display-backed runtime execution regressed. |

The rendering assertion must compare starter-scene pixels against Bunny-scene
pixels and require real pixel variance/delta. A non-null `ImageBuffer`, non-zero
dimensions, successful project load, or one flat-color screenshot is not
sufficient evidence.

## Asset and licensing rules

The workflow test is intentionally narrow:

| Rule | Reason |
| --- | --- |
| Use only `BunnyResource.DEFAULT` from the bundled open/default gallery. | The Bunny is redistributable and already belongs to the default open path. |
| Keep `-DincludeSims=false`. | Default RabbitHole workflows must run without proprietary Sims assets. |
| Do not copy Sims files into open asset directories or test resources. | Keeps the repository legally clean and portable. |
| Keep Sims compatibility tests separate and opt-in with `-DincludeSims=true`. | Preserves legacy compatibility without making nonfree assets part of the default contract. |

## CI contract

The headed Ubuntu Xvfb lane runs the focused workflow command after Xvfb setup.
It remains separate from headless Maven validation because
`java.awt.headless=true` cannot prove display-backed rendering.

The CI lane must:

1. Initialize `tweedle-lang`.
2. Install or provide `xvfb-run`.
3. Run Maven with `-Djava.awt.headless=false`.
4. Run Maven with `-DincludeSims=false`.
5. Set `-Drabbithole.defaultAssetWorkflow.required=true`.
6. Fail the job on missing display, render capture failure, invisible render,
   failed transform persistence, failed save/reopen, or runtime failure.

See [Verify the Default Open 3D Asset Workflow](../howto/verify-default-open-3d-asset-workflow.md)
for the runnable command and
[CI GUI, Eatme, and Xvfb Validation Reference](./ci-gui-eatme-xvfb-validation.md)
for the shared Xvfb harness contract.
