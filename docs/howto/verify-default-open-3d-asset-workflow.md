---
title: Verify the Default Open 3D Asset Workflow
description: Xvfb-backed integration test contract for placing, rendering, manipulating, saving, reopening, and running the default Bunny asset without Sims assets.
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: howto
related:
  - ../reference/default-open-3d-asset-workflow-test.md
  - ./use-open-3d-assets.md
  - ../reference/ci-gui-eatme-xvfb-validation.md
---

# Verify the default open 3D asset workflow

Use this workflow when changing Alice gallery loading, project placement, scene
rendering, 3D transforms, project save/reopen behavior, Xvfb CI setup, or the
default open-asset packaging path.

The focused workflow test proves that `alice-gallery://animals/bunny` works as a
real Alice scene object without Sims assets. It creates a project from the
starter scene, places the bundled Bunny gallery resource, renders it visibly,
applies persistent 3D transform and scale changes, saves and reopens the
project, and runs the reopened world under Xvfb.

## Prerequisites

Run commands from the repository root.

```bash
git submodule update --init tweedle-lang
export NODE_OPTIONS=--max-old-space-size=32768
```

On Ubuntu, install Xvfb if it is not already available:

```bash
sudo apt-get update
sudo apt-get install -y --no-install-recommends xvfb
```

## Run the workflow test under Xvfb

Use the shared harness so local validation matches CI display behavior:

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

The command intentionally sets `-DincludeSims=false`. Do not add Sims jars, Sims
gallery files, Sims EULA files, or copied proprietary assets to make this test
pass.

## Run the focused Maven command directly

If a real desktop display is already available, run the Maven test without the
Xvfb wrapper:

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

Keep `rabbithole.defaultAssetWorkflow.required=true` for required validation.
With that property set, the test fails instead of silently skipping when no
usable display or render target is available.

## What the test proves

| Step | Assertion |
| --- | --- |
| Default asset selection | The workflow uses `BunnyResource.DEFAULT` through `alice-gallery://animals/bunny`. |
| Sims independence | The run uses `-DincludeSims=false` and does not require nonfree modules or assets. |
| Placement | The Bunny is placed through the existing `EatmePlaceObject.run` Alice project/gallery placement path, not an isolated mock. |
| Rendering | A captured Bunny render differs from the starter render and contains real pixel variance/delta, not merely a non-null image. |
| 3D manipulation | A persistent 3D transform changes the Bunny state and is visible through runtime object state. |
| Round trip | The saved project reopens with the Bunny and transformed state intact. |
| Runtime execution | The reopened world can run under Xvfb with `java.awt.headless=false`. |

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| The test reports no usable display | Run through `scripts/validate-gui-with-xvfb.sh` or use a machine with a real Java AWT display. |
| Maven cannot find generated Tweedle parser classes | Run `git submodule update --init tweedle-lang` and confirm `tweedle-lang/Grammar` exists. |
| JOGL or GlueGen resolution fails in CI-like validation | Keep `--settings .github/maven/jogamp-ci-settings.xml` on the Maven command. |
| The Bunny loads but the render assertion fails | Treat the failure as a rendering or scene-setup regression; a non-null project is not enough. |
| The Bunny disappears after reopen | Treat the failure as a project serialization/deserialization regression. |
| Maven tries to resolve Sims/nonfree artifacts | Remove `-DincludeSims=true`; this workflow must pass with `-DincludeSims=false`. |

## Related documentation

- [Default Open 3D Asset Workflow Test Reference](../reference/default-open-3d-asset-workflow-test.md)
- [Use Open 3D Assets](./use-open-3d-assets.md)
- [CI GUI, Eatme, and Xvfb Validation Reference](../reference/ci-gui-eatme-xvfb-validation.md)
