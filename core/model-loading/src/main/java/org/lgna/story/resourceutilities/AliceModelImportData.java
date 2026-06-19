package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class AliceModelImportData {
  private final Path sourceFile;
  private final SkeletonVisual skeletonVisual;
  private final Path gltfBinaryFile;
  private final Path aliceStructureFile;
  private final Optional<Path> aliceTextureFile;
  private final int meshCount;
  private final int weightedMeshCount;

  public AliceModelImportData(
      Path sourceFile,
      SkeletonVisual skeletonVisual,
      Path gltfBinaryFile,
      Path aliceStructureFile,
      Optional<Path> aliceTextureFile,
      int meshCount,
      int weightedMeshCount) {
    if (meshCount < 0) {
      throw new IllegalArgumentException("meshCount must be non-negative");
    }
    if (weightedMeshCount < 0) {
      throw new IllegalArgumentException("weightedMeshCount must be non-negative");
    }
    this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
    this.skeletonVisual = Objects.requireNonNull(skeletonVisual, "skeletonVisual");
    this.gltfBinaryFile = Objects.requireNonNull(gltfBinaryFile, "gltfBinaryFile");
    this.aliceStructureFile = Objects.requireNonNull(aliceStructureFile, "aliceStructureFile");
    this.aliceTextureFile = Objects.requireNonNull(aliceTextureFile, "aliceTextureFile");
    this.meshCount = meshCount;
    this.weightedMeshCount = weightedMeshCount;
  }

  public Path getSourceFile() {
    return sourceFile;
  }

  public SkeletonVisual getSkeletonVisual() {
    return skeletonVisual;
  }

  public Path getGltfBinaryFile() {
    return gltfBinaryFile;
  }

  public Path getAliceStructureFile() {
    return aliceStructureFile;
  }

  public Optional<Path> getAliceTextureFile() {
    return aliceTextureFile;
  }

  public int getMeshCount() {
    return meshCount;
  }

  public int getWeightedMeshCount() {
    return weightedMeshCount;
  }
}
