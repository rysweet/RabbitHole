package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import edu.cmu.cs.dennisc.scenegraph.Geometry;
import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import edu.cmu.cs.dennisc.scenegraph.TexturedAppearance;
import edu.cmu.cs.dennisc.scenegraph.WeightedMesh;
import org.alice.tweedle.file.ModelManifest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OpenAssetImportPipeline {
  private final Logger logger;

  public OpenAssetImportPipeline(Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public AliceModelImportData importCollada(Path colladaModelPath, Path outputDirectory, String modelName)
      throws IOException, ModelLoadingException {
    Objects.requireNonNull(colladaModelPath, "colladaModelPath");
    Objects.requireNonNull(outputDirectory, "outputDirectory");
    Objects.requireNonNull(modelName, "modelName");
    String trimmedModelName = modelName.trim();
    if (trimmedModelName.isEmpty()) {
      throw new IllegalArgumentException("modelName must not be blank");
    }

    Path sourceFile = validateSourceFile(colladaModelPath);
    Files.createDirectories(outputDirectory);
    if (!Files.isDirectory(outputDirectory)) {
      throw new IOException("Output path is not a directory: " + outputDirectory);
    }

    logger.log(Level.INFO, "Importing COLLADA open asset proof from {0}", sourceFile);
    SkeletonVisual skeletonVisual = new JointedModelColladaImporter(sourceFile.toFile(), logger).loadSkeletonVisual();
    Path gltfBinaryFile = outputDirectory.resolve(trimmedModelName + ".glb");
    Path aliceStructureFile = outputDirectory.resolve(trimmedModelName + ".a3r");
    Path aliceTextureFile = outputDirectory.resolve(trimmedModelName + ".a3t");

    ModelManifest.ModelVariant variant = createVariant(trimmedModelName);
    writeGltf(skeletonVisual, variant, trimmedModelName, outputDirectory, gltfBinaryFile);
    Optional<Path> textureOutput = writeAliceResources(skeletonVisual, variant, outputDirectory, aliceStructureFile, aliceTextureFile);

    int meshCount = countMeshes(skeletonVisual.geometries.getValue());
    int weightedMeshCount = countWeightedMeshes(skeletonVisual.weightedMeshes.getValue());
    logger.log(
        Level.INFO,
        "Imported COLLADA open asset proof {0}: {1} mesh(es), {2} weighted mesh(es)",
        new Object[]{trimmedModelName, meshCount, weightedMeshCount});
    return new AliceModelImportData(
        sourceFile,
        skeletonVisual,
        gltfBinaryFile,
        aliceStructureFile,
        textureOutput,
        meshCount,
        weightedMeshCount);
  }

  private Path validateSourceFile(Path colladaModelPath) throws IOException, ModelLoadingException {
    Path normalizedPath = colladaModelPath.toAbsolutePath().normalize();
    if (!Files.exists(normalizedPath)) {
      throw new ModelLoadingException("COLLADA input file does not exist: " + normalizedPath);
    }
    if (!Files.isRegularFile(normalizedPath)) {
      throw new ModelLoadingException("COLLADA input path is not a file: " + normalizedPath);
    }
    return normalizedPath.toRealPath();
  }

  private void writeGltf(
      SkeletonVisual skeletonVisual,
      ModelManifest.ModelVariant variant,
      String modelName,
      Path outputDirectory,
      Path gltfBinaryFile)
      throws IOException {
    JointedModelGltfExporter exporter = new JointedModelGltfExporter(
        skeletonVisual,
        variant,
        modelName,
        outputDirectory.toString(),
        Map.of());
    writeDataSource(exporter.createStructureDataSource(), gltfBinaryFile, "glTF binary proof");
  }

  private Optional<Path> writeAliceResources(
      SkeletonVisual skeletonVisual,
      ModelManifest.ModelVariant variant,
      Path outputDirectory,
      Path aliceStructureFile,
      Path aliceTextureFile)
      throws IOException {
    TexturedAppearance[] importedTextures = skeletonVisual.textures.getValue();
    boolean hasTextureData = hasTextureData(importedTextures);
    JointedModelAliceExporter exporter = new JointedModelAliceExporter(skeletonVisual, variant, outputDirectory.toString());
    try {
      writeDataSource(exporter.createStructureDataSource(), aliceStructureFile, "Alice structure proof");
      if (!hasTextureData) {
        return Optional.empty();
      }
      List<DataSource> textureDataSources = new ArrayList<>();
      exporter.addImageDataSources(textureDataSources, new ModelManifest(), new HashMap<>());
      if (textureDataSources.isEmpty()) {
        return Optional.empty();
      }
      writeDataSource(textureDataSources.get(0), aliceTextureFile, "Alice texture proof");
      return Optional.of(aliceTextureFile);
    } finally {
      skeletonVisual.textures.setValue(importedTextures);
    }
  }

  private static ModelManifest.ModelVariant createVariant(String modelName) {
    ModelManifest.ModelVariant variant = new ModelManifest.ModelVariant();
    variant.name = "DEFAULT";
    variant.structure = modelName;
    variant.textureSet = modelName;
    return variant;
  }

  private static boolean hasTextureData(TexturedAppearance[] textures) {
    return textures != null && textures.length > 0;
  }

  private static int countMeshes(Geometry[] geometries) {
    return geometries == null ? 0 : geometries.length;
  }

  private static int countWeightedMeshes(WeightedMesh[] weightedMeshes) {
    return weightedMeshes == null ? 0 : weightedMeshes.length;
  }

  private static void writeDataSource(DataSource dataSource, Path outputFile, String description) throws IOException {
    Files.createDirectories(outputFile.getParent());
    try (OutputStream outputStream = Files.newOutputStream(outputFile)) {
      dataSource.write(outputStream);
    } catch (IOException e) {
      throw new IOException("Failed to write " + description + " file: " + outputFile, e);
    }
  }
}
