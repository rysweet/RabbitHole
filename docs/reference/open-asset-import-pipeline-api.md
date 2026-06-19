---
title: Open Asset Import Pipeline API Reference
description: Reference for OpenAssetImportPipeline and AliceModelImportData in the RabbitHole model-loading module.
last_updated: 2026-06-18
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: reference
---

# Open asset import pipeline API reference

The open asset import pipeline is a narrow Java facade for proving that a
normalized open COLLADA asset can become Alice scenegraph data and usable output
files.

## Contents

- [Package](#package)
- [`OpenAssetImportPipeline`](#openassetimportpipeline)
- [`AliceModelImportData`](#alicemodelimportdata)
- [Output files](#output-files)
- [Texture preservation contract](#texture-preservation-contract)
- [Errors](#errors)
- [Configuration](#configuration)
- [Thread-safety](#thread-safety)
- [Related APIs](#related-apis)

## Package

```java
package org.lgna.story.resourceutilities;
```

The API lives in `core/model-loading` with the existing COLLADA importer, glTF
exporter, Alice resource exporter, and model resource utilities.

## `OpenAssetImportPipeline`

```java
public final class OpenAssetImportPipeline {
  public OpenAssetImportPipeline(Logger logger);

  public AliceModelImportData importCollada(
      Path colladaModelPath,
      Path outputDirectory,
      String modelName)
      throws IOException, ModelLoadingException;
}
```

`OpenAssetImportPipeline` owns the vertical proof path. It is intentionally
small: one public import method, one input format, and one result object.

### Constructor

```java
public OpenAssetImportPipeline(Logger logger)
```

Creates a pipeline instance.

| Parameter | Required | Description |
| --- | --- | --- |
| `logger` | Yes | Logger passed to `JointedModelColladaImporter` for importer warnings and diagnostics. |

Passing `null` throws `NullPointerException`.

### `importCollada`

```java
public AliceModelImportData importCollada(
    Path colladaModelPath,
    Path outputDirectory,
    String modelName)
    throws IOException, ModelLoadingException
```

Imports one normalized COLLADA model and writes proof outputs.

| Parameter | Required | Description |
| --- | --- | --- |
| `colladaModelPath` | Yes | Path to a `.dae` file. Texture references are resolved relative to this file's parent directory. |
| `outputDirectory` | Yes | Directory for generated `.glb`, `.a3r`, and optional `.a3t` files. The pipeline creates it when it does not exist. |
| `modelName` | Yes | Alice-friendly model name used for generated output names and glTF root naming. |

Behavior:

1. Validates non-null arguments and a non-blank `modelName`.
2. Imports the model with `JointedModelColladaImporter.loadSkeletonVisual()`.
3. Exports a binary glTF proof file with `JointedModelGltfExporter`.
4. Exports Alice structure and texture resources with `JointedModelAliceExporter`.
5. Returns `AliceModelImportData` containing the source path, `SkeletonVisual`,
   output paths, and mesh counts.

The returned `SkeletonVisual` remains the imported visual for caller inspection,
including `textures` for textured imports. If exporter bookkeeping temporarily
separates texture resources during serialization, the facade preserves or
restores the visual state before returning the result.

The method does not generate Java resource enums, modify gallery manifests, or
copy files into production gallery directories.

## `AliceModelImportData`

```java
public final class AliceModelImportData {
  public AliceModelImportData(
      Path sourceFile,
      SkeletonVisual skeletonVisual,
      Path gltfBinaryFile,
      Path aliceStructureFile,
      Optional<Path> aliceTextureFile,
      int meshCount,
      int weightedMeshCount);

  public Path getSourceFile();

  public SkeletonVisual getSkeletonVisual();

  public Path getGltfBinaryFile();

  public Path getAliceStructureFile();

  public Optional<Path> getAliceTextureFile();

  public int getMeshCount();

  public int getWeightedMeshCount();
}
```

`AliceModelImportData` is an immutable result object for the proof path.

| Accessor | Description |
| --- | --- |
| `getSourceFile()` | Normalized absolute path to the imported `.dae` file. |
| `getSkeletonVisual()` | Imported Alice scenegraph `SkeletonVisual`, with texture references still inspectable for textured imports. |
| `getGltfBinaryFile()` | Generated `.glb` file. |
| `getAliceStructureFile()` | Generated Alice `.a3r` visual resource file. |
| `getAliceTextureFile()` | Generated Alice `.a3t` texture resource file, when texture data exists. |
| `getMeshCount()` | Count of static scenegraph meshes on the imported visual. |
| `getWeightedMeshCount()` | Count of weighted meshes on the imported visual. |

The constructor rejects:

| Invalid value | Exception |
| --- | --- |
| `null` source, visual, glTF path, Alice structure path, or texture optional | `NullPointerException` |
| Negative mesh counts | `IllegalArgumentException` |

The result object keeps the `SkeletonVisual` reference because Alice scenegraph
objects are mutable by design. The result object's paths, optional texture path,
and counts do not change after construction.

## Output files

The pipeline writes files directly to `outputDirectory`.

| Output | Required | Producer | Use |
| --- | --- | --- | --- |
| `<modelName>.glb` | Yes | `JointedModelGltfExporter` | glTF proof output for interchange and inspection. |
| `<modelName>.a3r` lowercased by Alice resource naming | Yes | `JointedModelAliceExporter` | Alice visual resource structure output. |
| `<modelName>.a3t` lowercased by Alice resource naming | No | `JointedModelAliceExporter` | Alice texture resource output when texture data exists. |

Output files are overwritten for the same `modelName` and output directory.
Callers that need retention should choose a clean output directory before
calling the pipeline.

## Texture preservation contract

`JointedModelAliceExporter` writes textures separately from the `.a3r` visual
structure. The import facade must not leak that export-side mutation to callers.

For a textured COLLADA input:

```java
AliceModelImportData imported = pipeline.importCollada(source, output, "SimpleRiggedCharacter");

assert imported.getSkeletonVisual().textures.getValue() != null;
assert imported.getSkeletonVisual().textures.getValue().length > 0;
assert imported.getAliceTextureFile().isPresent();
```

`OpenAssetImportPipelineTest` should assert both conditions. The first assertion
proves caller-visible texture references remain inspectable on the returned
visual. The second assertion proves Alice texture serialization produced an
`.a3t` resource for the same import.

## Errors

The pipeline fails visibly. It does not return partial success when required
outputs cannot be produced.

| Error | Type |
| --- | --- |
| Missing or malformed COLLADA | `ModelLoadingException` |
| Unsupported COLLADA scene shape | `ModelLoadingException` |
| No importable mesh data | `ModelLoadingException` |
| Output directory cannot be created | `IOException` |
| Generated output cannot be written | `IOException` |
| Invalid Java arguments | `NullPointerException` or `IllegalArgumentException` |

Importer warnings continue to use the logger supplied to the pipeline
constructor.

## Configuration

The API has no global configuration.

| Setting | How to configure |
| --- | --- |
| Input asset | Pass `colladaModelPath` to `importCollada`. |
| Output location | Pass `outputDirectory` to `importCollada`. |
| Output names | Pass `modelName` to `importCollada`. |
| Import logging | Pass a `Logger` to the constructor. |
| Texture lookup | Keep texture files relative to the COLLADA file. |

No environment variable, system property, preferences file, Node.js runtime, or
desktop UI setting changes import behavior.

## Thread-safety

`OpenAssetImportPipeline` does not store per-import mutable state after a call
returns. It may be reused sequentially.

Do not share one pipeline instance for concurrent imports unless callers also
coordinate output directories and logger handling. Use one pipeline instance per
worker when importing in parallel.

## Related APIs

| API | Role |
| --- | --- |
| `JointedModelColladaImporter` | Reads COLLADA and creates `SkeletonVisual`. |
| `JointedModelGltfExporter` | Writes binary glTF from `SkeletonVisual`. |
| `JointedModelAliceExporter` | Writes Alice `.a3r` and `.a3t` resource outputs. |
| `ModelResourceJavaGenerator` | Generates Java resource classes when a proof asset graduates into gallery integration. |

See [Import an open 3D asset](../howto/import-open-3d-asset.md) for usage
examples and [Tutorial: Import a COLLADA open asset](../tutorials/import-collada-open-asset.md)
for a complete walkthrough.
