# Open 3D assets in RabbitHole: Java applicability study

This study evaluates whether the Alice web prototype open 3D asset pipeline can
be used in RabbitHole, the Java Alice modernization repository.

The TypeScript guide is merged in
[alice-web-prototype PR #133](https://github.com/rysweet/alice-web-prototype/pull/133).
That guide documents the web prototype's open replacement path for proprietary
Sims-style assets: procedural model profiles, glTF/GLB metadata, Blender export
conventions, quality scoring, and gallery integration.

## Summary

The same **asset ideas** can be reused in RabbitHole, but the same **runtime
pipeline** cannot be dropped into Java unchanged.

| Asset-pipeline part | RabbitHole fit | Notes |
| --- | --- | --- |
| License policy and source rules | Reusable now | Do not copy proprietary Sims or Java desktop model assets. Prefer CC0/MIT-compatible source assets with recorded provenance. |
| Model profile data | Reusable with translation | TypeScript `ModelProfile` records can become Java resource metadata or generator input. They are not directly executable by Java. |
| Procedural silhouettes | Reusable after porting | The generation ideas can be ported to Java scenegraph meshes or used to produce exported `.glb` files. |
| Blender export conventions | Reusable now | Bone naming, scale normalization, and glTF/GLB output conventions can be shared across repos. |
| glTF/GLB authored assets | Partially reusable | RabbitHole can export glTF today, but does not currently have a Java glTF importer for runtime gallery assets. |
| Quality scoring | Reusable after porting or as external tooling | The geometric checks can be rewritten in Java or run externally over generated assets. |
| Gallery browser integration | Requires Java-specific work | RabbitHole's gallery/resource model is Java enum and resource-class based, not TypeScript catalog based. |

## Current RabbitHole asset pipeline

RabbitHole already has a substantial Java model-loading toolchain:

- `core/model-loading` handles import/export and code generation for model
  resources.
- `JointedModelColladaImporter` loads Collada models into Alice
  `SkeletonVisual` scenegraph data.
- `JointedModelGltfExporter` exports Alice `SkeletonVisual` data as binary glTF
  `.glb`.
- `ModelResourceJavaGenerator` generates Java resource classes and enum values.
- `core/models` contains generated Java resource enums such as
  `BunnyResource`, with Alice `JointId` definitions and visual factory hooks.
- RabbitHole already depends on `de.javagl:jgltf-model`, but current production
  code uses it for glTF export, not glTF import.

The important gap is direction: Java currently has a Collada import path and a
glTF export path. The TypeScript web pipeline assumes glTF import/metadata for
open assets. Bringing authored GLB assets into RabbitHole would require either a
new Java glTF importer or an offline conversion step into the existing Java
resource format.

## Candidate integration paths

### Path A: Use TypeScript-generated GLB as external release assets

Use the TypeScript procedural/profile pipeline to generate `.glb` files and
ship them as external assets outside the Java runtime path.

This is useful for:

- web previews
- documentation images
- comparison artifacts
- future non-Java runtimes

It does not immediately replace RabbitHole's Java gallery assets because Java
Alice still needs resource classes, joint IDs, thumbnails, and visual factories
that connect to `JointedModelImp`.

### Path B: Add Java glTF import to `core/model-loading`

Implement a Java importer that reads glTF/GLB and produces the same internal
objects that Collada import currently produces:

- `SkeletonVisual`
- `Joint`
- mesh/weighted mesh geometry
- materials and textures
- base bounding boxes
- Alice joint names

This is the cleanest long-term path if RabbitHole should directly consume open
GLB assets. It should reuse the existing `jgltf-model` dependency where
possible, and it should mirror existing Collada importer tests:

- root-joint detection
- coordinate and scale normalization
- mesh index validity
- skin weights
- material/texture loading
- missing-joint repair
- error reporting through `ModelLoadingException`

### Path C: Convert open assets through Blender into Collada for existing import

Use Blender as a conversion stage:

1. Source or generate open `.blend`/`.glb` assets.
2. Normalize scale, orientation, and bone names with the shared Blender rules.
3. Export Collada `.dae`.
4. Feed the `.dae` through `JointedModelColladaImporter`.
5. Generate Java resource classes with the existing model-loading tools.

This path uses the importer RabbitHole already has, but it keeps Collada in the
pipeline and may lose glTF-specific material or animation data.

### Path D: Port procedural generation to Java

Port the TypeScript profile-driven procedural generator into Java and emit
`SkeletonVisual` or `Mesh` data directly.

This could provide a no-binary fallback set of open placeholder models in Java,
but it is a larger implementation project:

- Java mesh builders are needed for each distinctive feature.
- Generated geometry must match scenegraph and renderer expectations.
- The generated models still need Java resource enums and gallery integration.

## Recommended first implementation slice

Start with a narrow proof:

1. Select one CC0 or procedural test asset, preferably a biped with a simple
   skeleton.
2. Normalize it in Blender using the TypeScript guide's naming rules.
3. Export both `.glb` and `.dae`.
4. Import the `.dae` through `JointedModelColladaImporter`.
5. Generate or hand-author a minimal Java resource enum.
6. Add characterization tests for:
   - root joint exists
   - required `JointId` entries resolve
   - mesh vertex/index counts are non-empty and valid
   - thumbnail generation succeeds headlessly
   - the resource can be listed or loaded through the gallery/resource path

Only after that proof should RabbitHole decide whether to build a native glTF
importer or keep using Blender-to-Collada conversion.

## Testing requirements

Any RabbitHole open-asset import work should include:

- `core/model-loading` unit tests for importer/exporter conversion
- generated resource class tests for joint constants and root IDs
- model thumbnail tests with headless-safe assumptions
- gallery/resource discovery tests in `core/ide`
- no-Sims build/test coverage to prove the open path does not depend on Sims
  assets
- license/provenance checks for checked-in binary assets

For broad Maven validation, initialize the Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
```

## Risks and blockers

- Java Alice's model resource system is enum/class based; TypeScript's catalog
  is data-driven.
- Authored glTF assets are not loadable in RabbitHole without a new importer or
  conversion step.
- Skeleton and joint naming must match Java `JointId` constants, not just the
  TypeScript category names.
- Licensing must be rechecked for any binary committed to RabbitHole.
- Large assets can bloat repository size and installer size.
- Sims and legacy Alice gallery assets cannot be copied as substitutes or test
  fixtures.

## Conclusion

Use the TypeScript pipeline as the **source of conventions and candidate open
assets**, not as a drop-in Java runtime. The fastest safe RabbitHole path is a
small Blender-normalized asset proof that uses the existing Collada importer.
The best long-term path is a Java glTF importer that produces the same
`SkeletonVisual` and Java resource metadata the current gallery expects.
