---
title: Import an Open 3D Asset
description: Shows how to run a normalized COLLADA asset through RabbitHole model loading and export usable Alice and glTF outputs.
last_updated: 2026-06-18
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: howto
---

# Import an open 3D asset

Use `OpenAssetImportPipeline` when you need to prove that an open 3D asset can
enter RabbitHole through the Java model-loading path and leave as usable Alice
and glTF output.

## Contents

- [Before you start](#before-you-start)
- [Configure the asset](#configure-the-asset)
- [Import the asset](#import-the-asset)
- [Use the returned visual](#use-the-returned-visual)
- [Use the exported files](#use-the-exported-files)
- [Validate the import path](#validate-the-import-path)
- [Troubleshooting](#troubleshooting)

## Before you start

Prepare one open asset as normalized COLLADA:

| Requirement | Value |
| --- | --- |
| Source format | `.dae` COLLADA |
| License | Open license that permits redistribution in RabbitHole |
| Geometry | At least one mesh with valid vertices and indices |
| Skeleton | A `root` joint or one importable joint for rigged assets |
| Textures | Files reachable from the `.dae` parent directory |
| Scale | Meter-based scene scale suitable for Alice characters |
| Output target | Writable directory under `target/` for generated proof artifacts |

The Java POC uses the existing COLLADA importer first. The Alice web prototype
provides naming, licensing, and authoring conventions only; the RabbitHole
pipeline is Java-native and does not run TypeScript or copy prototype runtime
code.

## Configure the asset

Keep the asset, textures, and attribution together:

```text
core/model-loading/src/test/resources/org/lgna/story/resourceutilities/open-assets/
├── simple-rigged-character.dae
├── simple-rigged-character.png
└── simple-rigged-character-attribution.txt
```

Use these authoring rules before exporting COLLADA:

| Area | Rule |
| --- | --- |
| Joint names | Use stable Alice-friendly identifiers. Prefer `root` for the skeleton root. |
| Mesh names | Use descriptive names without spaces, for example `bodyMesh` or `headMesh`. |
| Texture paths | Store texture references relative to the `.dae` file. |
| Units | Export in meters and let the importer apply the COLLADA unit scale. |
| Orientation | Preserve the COLLADA up-axis metadata; RabbitHole normalizes it during import. |
| Provenance | Keep attribution text next to the asset when the asset is checked in. |

No Node.js configuration is used by this path. `NODE_OPTIONS` does not affect
the Java import or export behavior.

## Import the asset

Call the facade from a test, model-loading utility, or local proof harness:

```java
import org.lgna.story.resourceutilities.AliceModelImportData;
import org.lgna.story.resourceutilities.OpenAssetImportPipeline;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class OpenAssetImportExample {
  public AliceModelImportData importCharacter() throws Exception {
    Path source =
        Path.of(
            "core/model-loading/src/test/resources/org/lgna/story/resourceutilities/open-assets",
            "simple-rigged-character.dae");
    Path outputDirectory = Path.of("target", "open-asset-import");

    OpenAssetImportPipeline pipeline =
        new OpenAssetImportPipeline(Logger.getLogger("open-asset-import"));

    return pipeline.importCollada(source, outputDirectory, "SimpleRiggedCharacter");
  }
}
```

The pipeline performs one vertical slice:

1. Reads the COLLADA file with `JointedModelColladaImporter`.
2. Builds an Alice `SkeletonVisual`.
3. Runs the existing weighted-mesh and bounding-box processing.
4. Writes a binary glTF `.glb` proof output.
5. Writes Alice `.a3r` structure output and `.a3t` texture output when textures exist.
6. Returns immutable `AliceModelImportData` with the visual and generated file paths.

## Use the returned visual

Use the returned `SkeletonVisual` for assertions, inspection, or handoff to
existing Alice resource tooling:

```java
AliceModelImportData imported = importCharacter();

SkeletonVisual visual = imported.getSkeletonVisual();
if (visual.skeleton.getValue() == null) {
  throw new IllegalStateException("Imported asset has no skeleton.");
}
if (imported.getMeshCount() + imported.getWeightedMeshCount() == 0) {
  throw new IllegalStateException("Imported asset has no usable geometry.");
}
if (visual.textures.getValue() == null || visual.textures.getValue().length == 0) {
  throw new IllegalStateException("Textured proof asset has no inspectable textures.");
}
```

The facade does not register a gallery resource enum. Resource-class generation
remains owned by `ModelResourceJavaGenerator` and related model resource tools.
For textured imports, the returned visual still exposes imported texture
references after Alice `.a3t` export bookkeeping runs.

## Use the exported files

The result object exposes the generated outputs:

```java
AliceModelImportData imported = importCharacter();

Path glb = imported.getGltfBinaryFile();
Path aliceStructure = imported.getAliceStructureFile();
Optional<Path> aliceTexture = imported.getAliceTextureFile();

System.out.println("glTF proof: " + glb);
System.out.println("Alice structure: " + aliceStructure);
aliceTexture.ifPresent(path -> System.out.println("Alice texture: " + path));
```

Expected output names are derived from the supplied model name:

| Model name | Output |
| --- | --- |
| `SimpleRiggedCharacter` | `SimpleRiggedCharacter.glb` |
| `SimpleRiggedCharacter` | `SimpleRiggedCharacter.a3r` |
| `SimpleRiggedCharacter` | `SimpleRiggedCharacter.a3t`, when texture data exists |

Use the `.glb` file for interchange proof and visual inspection. Use the `.a3r`
and `.a3t` files to prove the imported visual can be serialized into Alice's
resource format.

## Validate the import path

Run the focused model-loading tests:

```bash
mvn -pl core/model-loading -Dtest=OpenAssetImportPipelineTest test
```

For broad Maven validation, initialize the Tweedle grammar submodule before
running lanes that reach Tweedle:

```bash
git submodule update --init tweedle-lang
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true test
```

The focused test should assert:

| Assertion | Purpose |
| --- | --- |
| `SkeletonVisual` is returned | Proves the existing Java importer accepted the asset. |
| Mesh count is non-zero | Proves the proof asset has usable geometry. |
| Root joint is present | Proves rigged import works through Alice scenegraph data. |
| Texture references remain inspectable for textured assets | Proves export bookkeeping did not strip caller-visible import state. |
| `.glb` exists and is non-empty | Proves glTF export works from the imported visual. |
| `.a3r` exists and is non-empty | Proves Alice structure serialization works. |
| `.a3t` exists and is non-empty for textured assets | Proves Alice texture serialization works without hiding imported texture references. |
| Invalid input throws `ModelLoadingException` or `IOException` | Proves failures are explicit and not silently accepted. |

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Failed to load collada file` | The `.dae` file is missing, malformed, or contains unsupported nested animation data. | Re-export the asset as plain COLLADA with mesh, skeleton, and texture references only. |
| `No scene found` | The COLLADA document has no active visual scene. | Export a scene that contains the mesh and armature. |
| `No valid meshes found` | The file contains only an armature, cameras, lights, or unsupported mesh data. | Include at least one triangulated mesh with vertex and index data. |
| Empty texture output | The asset has no texture image data that maps to Alice texture resources. | Check relative texture paths and image file availability. |
| Texture references missing from the returned visual | Export bookkeeping cleared the visual texture array without restoring it. | Preserve or restore the imported texture array before constructing `AliceModelImportData`. |
| Large or misplaced model | Unit scale or up-axis metadata is wrong. | Normalize the asset in the authoring tool and re-export with meter units. |

See [Open asset import pipeline API](../reference/open-asset-import-pipeline-api.md)
for method and result-object details.
