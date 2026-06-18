package org.lgna.story.resourceutilities;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import org.alice.tweedle.file.ModelManifest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JointedModelGltfExporterModelTest {
  @Test
  public void createStructureDataSourceWritesBinaryGlbWithEmbeddedJsonChunks() throws Exception {
    ModelManifest.ModelVariant variant = ExporterTestFixtures.createVariant("robotStructure", "RobotTexture");
    JointedModelGltfExporter exporter = new JointedModelGltfExporter(
        ExporterTestFixtures.createVisual(true),
        variant,
        "robot",
        "resource/path",
        Map.of("ROOT", "ROOT_ALIAS"));

    DataSource dataSource = exporter.createStructureDataSource();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    dataSource.write(outputStream);
    byte[] bytes = outputStream.toByteArray();
    String payload = new String(bytes, StandardCharsets.ISO_8859_1);

    assertEquals("resource/path/RobotTexture.glb", dataSource.getName());
    assertArrayEquals(new byte[]{0x67, 0x6c, 0x54, 0x46}, Arrays.copyOf(bytes, 4));
    assertTrue(bytes.length > 32);
    assertTrue(payload.contains("RobotTexture"));
    assertTrue(payload.contains("ROOT_ALIAS"));
  }

  @Test
  public void generatedOpenAssetVisualExportsAsParseableGlb() throws Exception {
    ModelManifest.ModelVariant variant = ExporterTestFixtures.createVariant(
        "openFoxStructure",
        "OpenFoxDefault");
    JointedModelGltfExporter exporter = new JointedModelGltfExporter(
        ExporterTestFixtures.createVisual(true),
        variant,
        "openFox",
        "open-assets/proof",
        Map.of("ROOT", "ROOT"));

    DataSource dataSource = exporter.createStructureDataSource();
    byte[] bytes = write(dataSource);
    GltfModel gltfModel = new GltfModelReader()
        .readWithoutReferences(new ByteArrayInputStream(bytes));

    assertEquals("open-assets/proof/OpenFoxDefault.glb", dataSource.getName());
    assertTrue("exported proof should contain at least one scene",
        gltfModel.getSceneModels().size() > 0);
    assertTrue("exported proof should preserve the open asset root node",
        gltfModel.getNodeModels().stream()
            .map(NodeModel::getName)
            .anyMatch("OpenFoxDefault"::equals));
    assertTrue("exported proof should preserve a ROOT joint",
        gltfModel.getNodeModels().stream()
            .map(NodeModel::getName)
            .anyMatch("ROOT"::equals));
    assertTrue("exported proof should include mesh geometry",
        gltfModel.getNodeModels().stream()
            .flatMap(node -> node.getMeshModels().stream())
            .flatMap(mesh -> mesh.getMeshPrimitiveModels().stream())
            .anyMatch(JointedModelGltfExporterModelTest::hasPositionIndicesAndSkinWeights));
    assertTrue("exported proof should include a skin with joints",
        gltfModel.getNodeModels().stream()
            .map(NodeModel::getSkinModel)
            .anyMatch(skin -> (skin != null) && !skin.getJoints().isEmpty()));
  }

  private static byte[] write(DataSource dataSource) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    dataSource.write(outputStream);
    return outputStream.toByteArray();
  }

  private static boolean hasPositionIndicesAndSkinWeights(MeshPrimitiveModel primitive) {
    return primitive.getIndices() != null
        && primitive.getAttributes().containsKey("POSITION")
        && primitive.getAttributes().containsKey("JOINTS_0")
        && primitive.getAttributes().containsKey("WEIGHTS_0");
  }
}
