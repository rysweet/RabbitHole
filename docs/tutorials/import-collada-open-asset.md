---
title: "Tutorial: Import a COLLADA Open Asset"
description: Walks through preparing one open COLLADA asset and importing it through RabbitHole's Java model-loading pipeline.
last_updated: 2026-06-18
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: tutorial
---

# Tutorial: Import a COLLADA open asset

This tutorial walks through importing one open, normalized COLLADA asset through
RabbitHole and producing Alice and glTF proof outputs.

## What you will build

You will run one asset through this path:

```text
open COLLADA asset
  -> JointedModelColladaImporter
  -> SkeletonVisual
  -> JointedModelGltfExporter
  -> JointedModelAliceExporter
  -> .glb, .a3r, and optional .a3t output
```

## Prerequisites

- A `.dae` asset with an open redistribution license.
- Texture files referenced relative to the `.dae` file.
- A writable `target/open-asset-import/` directory.
- Maven available from the repository root.

## Step 1: Place the asset

Place the asset under the model-loading test resources:

```text
core/model-loading/src/test/resources/org/lgna/story/resourceutilities/open-assets/simple-rigged-character.dae
core/model-loading/src/test/resources/org/lgna/story/resourceutilities/open-assets/simple-rigged-character.png
core/model-loading/src/test/resources/org/lgna/story/resourceutilities/open-assets/simple-rigged-character-attribution.txt
```

The attribution file should name the asset source, author when known, license,
and any required notice text. Do not use Sims or legacy proprietary gallery
assets as proof fixtures.

## Step 2: Normalize the asset

Before export, check the model in the authoring tool:

| Check | Expected value |
| --- | --- |
| Scene scale | Meter units |
| Skeleton root | `root`, or a single first importable joint |
| Mesh data | Triangulated mesh with vertices, normals, texture coordinates, and indices |
| Textures | Relative image paths next to the `.dae` file |
| Animation data | Omit nested animation clips from the proof asset |

Export COLLADA with mesh and armature data enabled.

## Step 3: Import the asset

Create a small Java entry point or test helper that calls the pipeline:

```java
import org.lgna.story.resourceutilities.AliceModelImportData;
import org.lgna.story.resourceutilities.OpenAssetImportPipeline;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class SimpleRiggedCharacterImport {
  public static void main(String[] args) throws Exception {
    Path source =
        Path.of(
            "core/model-loading/src/test/resources/org/lgna/story/resourceutilities/open-assets",
            "simple-rigged-character.dae");
    Path output = Path.of("target", "open-asset-import");

    OpenAssetImportPipeline pipeline =
        new OpenAssetImportPipeline(Logger.getLogger("open-asset-import"));
    AliceModelImportData imported =
        pipeline.importCollada(source, output, "SimpleRiggedCharacter");

    System.out.println(imported.getSkeletonVisual().getName());
    System.out.println(imported.getGltfBinaryFile());
    System.out.println(imported.getAliceStructureFile());
    imported.getAliceTextureFile().ifPresent(System.out::println);
  }
}
```

The import succeeds only when RabbitHole can build a `SkeletonVisual` and write
the required proof outputs.

## Step 4: Inspect the result

Check the returned data:

```java
AliceModelImportData imported = pipeline.importCollada(source, output, "SimpleRiggedCharacter");

assert imported.getSkeletonVisual().skeleton.getValue() != null;
assert imported.getMeshCount() + imported.getWeightedMeshCount() > 0;
assert imported.getSkeletonVisual().textures.getValue() != null;
assert imported.getSkeletonVisual().textures.getValue().length > 0;
assert imported.getAliceTextureFile().isPresent();
assert java.nio.file.Files.size(imported.getGltfBinaryFile()) > 0;
assert java.nio.file.Files.size(imported.getAliceStructureFile()) > 0;
assert java.nio.file.Files.size(imported.getAliceTextureFile().get()) > 0;
```

Expected files:

```text
target/open-asset-import/SimpleRiggedCharacter.glb
target/open-asset-import/simpleriggedcharacter.a3r
target/open-asset-import/simpleriggedcharacter.a3t
```

The `.a3t` file appears when the imported visual contains texture data. The
returned `SkeletonVisual` should still expose texture references after the `.a3t`
export is complete.

## Step 5: Add a characterization test

Keep the proof in `core/model-loading` so it protects the importer and exporters
together:

```java
@Test
public void importsOpenColladaAssetAndWritesUsableOutputs() throws Exception {
  Path source =
      Path.of(
          "src/test/resources/org/lgna/story/resourceutilities/open-assets",
          "simple-rigged-character.dae");
  Path output = Path.of("target", "open-asset-import-test");

  OpenAssetImportPipeline pipeline =
      new OpenAssetImportPipeline(Logger.getLogger("open-asset-import-test"));
  AliceModelImportData imported =
      pipeline.importCollada(source, output, "SimpleRiggedCharacter");

  assertNotNull(imported.getSkeletonVisual());
  assertNotNull(imported.getSkeletonVisual().skeleton.getValue());
  assertTrue(imported.getMeshCount() + imported.getWeightedMeshCount() > 0);
  assertNotNull(imported.getSkeletonVisual().textures.getValue());
  assertTrue(imported.getSkeletonVisual().textures.getValue().length > 0);
  assertTrue(imported.getAliceTextureFile().isPresent());
  assertTrue(Files.size(imported.getGltfBinaryFile()) > 0);
  assertTrue(Files.size(imported.getAliceStructureFile()) > 0);
  assertTrue(Files.size(imported.getAliceTextureFile().get()) > 0);
}
```

Add a failure-path test for an invalid or missing `.dae` file so callers get an
explicit exception instead of a success-shaped empty result.

## Step 6: Run the focused test

Run the model-loading proof:

```bash
mvn -pl core/model-loading -Dtest=OpenAssetImportPipelineTest test
```

Run broad no-Sims validation when the import proof changes shared model-loading
behavior:

```bash
git submodule update --init tweedle-lang
mvn -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true test
```

## Where to go from here

Use the returned `.glb`, `.a3r`, and `.a3t` files to inspect whether the proof
asset is suitable for a fuller resource integration. If the asset should become
part of the gallery, use the existing model resource generation tools after this
pipeline proves import and serialization.
