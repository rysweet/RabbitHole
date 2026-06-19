package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class AliceModelImportDataTest {
  @Test
  public void constructorStoresProofMetadataAndImportedVisual() {
    Path sourceFile = Path.of("target/open-asset/source/open-asset.dae").toAbsolutePath();
    SkeletonVisual skeletonVisual = ExporterTestFixtures.createVisual(true);
    Path gltfBinaryFile = Path.of("target/open-asset/output/OpenAssetProof.glb").toAbsolutePath();
    Path aliceStructureFile = Path.of("target/open-asset/output/OpenAssetProof.a3r").toAbsolutePath();
    Optional<Path> aliceTextureFile = Optional.of(Path.of("target/open-asset/output/OpenAssetProof.a3t").toAbsolutePath());

    AliceModelImportData data = new AliceModelImportData(
        sourceFile,
        skeletonVisual,
        gltfBinaryFile,
        aliceStructureFile,
        aliceTextureFile,
        1,
        1);

    assertEquals(sourceFile, data.getSourceFile());
    assertSame(skeletonVisual, data.getSkeletonVisual());
    assertEquals(gltfBinaryFile, data.getGltfBinaryFile());
    assertEquals(aliceStructureFile, data.getAliceStructureFile());
    assertEquals(aliceTextureFile, data.getAliceTextureFile());
    assertEquals(1, data.getMeshCount());
    assertEquals(1, data.getWeightedMeshCount());
  }

  @Test
  public void constructorAcceptsMissingTextureOutputWhenImportHasNoTextureData() {
    AliceModelImportData data = new AliceModelImportData(
        Path.of("target/open-asset/source/untextured.dae"),
        ExporterTestFixtures.createVisual(false),
        Path.of("target/open-asset/output/Untextured.glb"),
        Path.of("target/open-asset/output/Untextured.a3r"),
        Optional.empty(),
        1,
        0);

    assertEquals(Optional.empty(), data.getAliceTextureFile());
  }

  @Test
  public void constructorRejectsNullRequiredValues() {
    Path sourceFile = Path.of("target/open-asset/source/open-asset.dae");
    SkeletonVisual skeletonVisual = ExporterTestFixtures.createVisual(true);
    Path gltfBinaryFile = Path.of("target/open-asset/output/OpenAssetProof.glb");
    Path aliceStructureFile = Path.of("target/open-asset/output/OpenAssetProof.a3r");
    Optional<Path> aliceTextureFile = Optional.of(Path.of("target/open-asset/output/OpenAssetProof.a3t"));

    assertThrows(NullPointerException.class, () -> new AliceModelImportData(
        null, skeletonVisual, gltfBinaryFile, aliceStructureFile, aliceTextureFile, 1, 1));
    assertThrows(NullPointerException.class, () -> new AliceModelImportData(
        sourceFile, null, gltfBinaryFile, aliceStructureFile, aliceTextureFile, 1, 1));
    assertThrows(NullPointerException.class, () -> new AliceModelImportData(
        sourceFile, skeletonVisual, null, aliceStructureFile, aliceTextureFile, 1, 1));
    assertThrows(NullPointerException.class, () -> new AliceModelImportData(
        sourceFile, skeletonVisual, gltfBinaryFile, null, aliceTextureFile, 1, 1));
    assertThrows(NullPointerException.class, () -> new AliceModelImportData(
        sourceFile, skeletonVisual, gltfBinaryFile, aliceStructureFile, null, 1, 1));
  }

  @Test
  public void constructorRejectsNegativeMeshCounts() {
    Path sourceFile = Path.of("target/open-asset/source/open-asset.dae");
    SkeletonVisual skeletonVisual = ExporterTestFixtures.createVisual(true);
    Path gltfBinaryFile = Path.of("target/open-asset/output/OpenAssetProof.glb");
    Path aliceStructureFile = Path.of("target/open-asset/output/OpenAssetProof.a3r");
    Optional<Path> aliceTextureFile = Optional.of(Path.of("target/open-asset/output/OpenAssetProof.a3t"));

    assertThrows(IllegalArgumentException.class, () -> new AliceModelImportData(
        sourceFile, skeletonVisual, gltfBinaryFile, aliceStructureFile, aliceTextureFile, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new AliceModelImportData(
        sourceFile, skeletonVisual, gltfBinaryFile, aliceStructureFile, aliceTextureFile, 0, -1));
  }
}
