package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import org.alice.tweedle.file.ModelManifest;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OpenAssetImportPipelineTest {
  @Test
  public void importColladaProducesVisualGlbAndAliceResourceOutputs() throws Exception {
    Path workDir = ExporterTestFixtures.workDir("open-asset-import-pipeline/success");
    Path sourceFile = writeTexturedColladaFixture(workDir.resolve("source"));
    Path outputDirectory = workDir.resolve("output");

    AliceModelImportData imported = new OpenAssetImportPipeline(Logger.getLogger("test"))
        .importCollada(sourceFile, outputDirectory, "OpenAssetProof");

    Path normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize();
    assertEquals(sourceFile.toRealPath(), imported.getSourceFile());
    assertEquals(normalizedOutputDirectory.resolve("OpenAssetProof.glb"), imported.getGltfBinaryFile());
    assertEquals(normalizedOutputDirectory.resolve("openassetproof.a3r"), imported.getAliceStructureFile());
    assertEquals(normalizedOutputDirectory.resolve("openassetproof.a3t"), imported.getAliceTextureFile().orElseThrow());

    SkeletonVisual visual = imported.getSkeletonVisual();
    assertNotNull(visual);
    assertNotNull(visual.skeleton.getValue());
    assertTrue(imported.getMeshCount() + imported.getWeightedMeshCount() > 0);
    assertEquals(imported.getMeshCount(), visual.geometries.getValue().length);
    assertEquals(imported.getWeightedMeshCount(), visual.weightedMeshes.getValue().length);
    assertNotNull(visual.textures.getValue());
    assertTrue(visual.textures.getValue().length > 0);

    assertFileExistsWithBytes(imported.getGltfBinaryFile());
    assertFileExistsWithBytes(imported.getAliceStructureFile());
    assertFileExistsWithBytes(imported.getAliceTextureFile().orElseThrow());
  }

  @Test
  public void importColladaCreatesMissingOutputDirectory() throws Exception {
    Path workDir = ExporterTestFixtures.workDir("open-asset-import-pipeline/create-output");
    Path sourceFile = writeTexturedColladaFixture(workDir.resolve("source"));
    Path outputDirectory = workDir.resolve("missing").resolve("nested");

    AliceModelImportData imported = new OpenAssetImportPipeline(Logger.getLogger("test"))
        .importCollada(sourceFile, outputDirectory, "CreatedOutputDirectoryProof");

    assertTrue(Files.isDirectory(outputDirectory));
    assertFileExistsWithBytes(imported.getGltfBinaryFile());
    assertFileExistsWithBytes(imported.getAliceStructureFile());
  }

  @Test
  public void importColladaRejectsMissingInputWithModelLoadingException() {
    Path missingInput = Path.of("target/open-asset-import-pipeline/missing/missing.dae");
    Path outputDirectory = Path.of("target/open-asset-import-pipeline/missing/output");

    ModelLoadingException exception = assertThrows(
        ModelLoadingException.class,
        () -> new OpenAssetImportPipeline(Logger.getLogger("test"))
            .importCollada(missingInput, outputDirectory, "MissingInputProof"));

    assertTrue(exception.getMessage().contains("missing.dae"));
  }

  @Test
  public void importColladaRejectsInvalidArgumentsBeforeWritingOutputs() {
    OpenAssetImportPipeline pipeline = new OpenAssetImportPipeline(Logger.getLogger("test"));
    Path sourceFile = Path.of("target/open-asset-import-pipeline/invalid/source.dae");
    Path outputDirectory = Path.of("target/open-asset-import-pipeline/invalid/output");

    assertThrows(NullPointerException.class, () -> new OpenAssetImportPipeline(null));
    assertThrows(NullPointerException.class, () -> pipeline.importCollada(null, outputDirectory, "InvalidProof"));
    assertThrows(NullPointerException.class, () -> pipeline.importCollada(sourceFile, null, "InvalidProof"));
    assertThrows(NullPointerException.class, () -> pipeline.importCollada(sourceFile, outputDirectory, null));
    assertThrows(IllegalArgumentException.class, () -> pipeline.importCollada(sourceFile, outputDirectory, ""));
    assertThrows(IllegalArgumentException.class, () -> pipeline.importCollada(sourceFile, outputDirectory, "   "));
    assertFalse(Files.exists(outputDirectory));
  }

  @Test
  public void importColladaRejectsPathLikeModelNamesBeforeWritingOutputs() {
    OpenAssetImportPipeline pipeline = new OpenAssetImportPipeline(Logger.getLogger("test"));
    Path sourceFile = Path.of("target/open-asset-import-pipeline/path-like/source.dae");
    Path outputDirectory = Path.of("target/open-asset-import-pipeline/path-like/output");

    for (String modelName : List.of("../Escape", "/tmp/Escape", "nested/Escape", "nested\\Escape", ".", "..")) {
      assertThrows(
          "Expected path-like modelName to be rejected: " + modelName,
          IllegalArgumentException.class,
          () -> pipeline.importCollada(sourceFile, outputDirectory, modelName));
    }
    assertFalse(Files.exists(outputDirectory));
  }

  private static Path writeTexturedColladaFixture(Path directory) throws Exception {
    Files.createDirectories(directory);
    SkeletonVisual sourceVisual = ExporterTestFixtures.createVisual(true);
    ModelManifest.ModelVariant variant = ExporterTestFixtures.createVariant("OpenAssetFixtureStructure", "OpenAssetFixtureTexture");
    JointedModelColladaExporter exporter = new JointedModelColladaExporter(
        sourceVisual,
        variant,
        "OpenAssetFixture",
        "",
        Map.of("ROOT", "root"));

    Path sourceFile = directory.resolve("open-asset-fixture.dae");
    try (OutputStream outputStream = Files.newOutputStream(sourceFile)) {
      exporter.writeCollada(outputStream);
    }
    assertFalse(exporter.saveTexturesToDirectory(directory.toFile()).isEmpty());
    return sourceFile;
  }

  private static void assertFileExistsWithBytes(Path path) throws Exception {
    assertTrue("Expected file to exist: " + path, Files.isRegularFile(path));
    assertTrue("Expected file to contain bytes: " + path, Files.size(path) > 0);
  }
}
