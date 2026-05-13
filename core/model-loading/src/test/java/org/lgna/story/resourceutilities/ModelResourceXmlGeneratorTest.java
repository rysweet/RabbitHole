package org.lgna.story.resourceutilities;

import org.alice.math.immutable.AxisAlignedBox;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for {@link ModelResourceXmlGenerator}.
 * Same package gives access to the package-private API.
 */
public class ModelResourceXmlGeneratorTest {

  // ── computeBoundingBoxUnion ─────────────────────────────────────

  @Test
  public void computeBoundingBoxUnionOfSingleBoxReturnsThatBox() {
    AxisAlignedBox box = AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0);
    AxisAlignedBox result = ModelResourceXmlGenerator.computeBoundingBoxUnion(Collections.singletonList(box));
    assertEquals(-1.0, result.getXMinimum(), 0.0);
    assertEquals(0.0, result.getYMinimum(), 0.0);
    assertEquals(-2.0, result.getZMinimum(), 0.0);
    assertEquals(1.0, result.getXMaximum(), 0.0);
    assertEquals(3.0, result.getYMaximum(), 0.0);
    assertEquals(2.0, result.getZMaximum(), 0.0);
  }

  @Test
  public void computeBoundingBoxUnionMergesMultipleBoxes() {
    AxisAlignedBox boxA = AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0);
    AxisAlignedBox boxB = AxisAlignedBox.createAxisAlignedBox(-3.0, -1.0, -4.0, 2.0, 4.0, 5.0);
    AxisAlignedBox result = ModelResourceXmlGenerator.computeBoundingBoxUnion(Arrays.asList(boxA, boxB));

    assertEquals(-3.0, result.getXMinimum(), 0.0);
    assertEquals(-1.0, result.getYMinimum(), 0.0);
    assertEquals(-4.0, result.getZMinimum(), 0.0);
    assertEquals(2.0, result.getXMaximum(), 0.0);
    assertEquals(4.0, result.getYMaximum(), 0.0);
    assertEquals(5.0, result.getZMaximum(), 0.0);
  }

  @Test
  public void computeBoundingBoxUnionOfEmptyListReturnsNaN() {
    AxisAlignedBox result = ModelResourceXmlGenerator.computeBoundingBoxUnion(Collections.emptyList());
    assertTrue(Double.isNaN(result.getXMinimum()));
  }

  // ── createXMLString ─────────────────────────────────────────────

  @Test
  public void createXMLStringProducesValidXmlDocument() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    String xml = ModelResourceXmlGenerator.createXMLString(exporter);
    assertNotNull(xml);
    Document doc = parseXml(xml);
    assertEquals("AliceModel", doc.getDocumentElement().getNodeName());
    assertEquals("TestProp", doc.getDocumentElement().getAttribute("name"));
  }

  @Test
  public void createXMLStringIncludesBoundingBoxElement() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    Document doc = parseXml(ModelResourceXmlGenerator.createXMLString(exporter));
    Element bbox = (Element) doc.getDocumentElement().getElementsByTagName("BoundingBox").item(0);
    assertNotNull("BoundingBox element should be present", bbox);
    Element min = (Element) bbox.getElementsByTagName("Min").item(0);
    assertEquals("-1.0", min.getAttribute("x"));
  }

  @Test
  public void createXMLStringIncludesResourceElements() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    Document doc = parseXml(ModelResourceXmlGenerator.createXMLString(exporter));
    Element resource = (Element) doc.getDocumentElement().getElementsByTagName("Resource").item(0);
    assertNotNull("Resource element should be present", resource);
    assertEquals("DEFAULT", resource.getAttribute("resourceName"));
  }

  @Test
  public void createXMLStringMarksDeprecatedExporter() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    exporter.setIsDeprecated(true);
    Document doc = parseXml(ModelResourceXmlGenerator.createXMLString(exporter));
    assertEquals("TRUE", doc.getDocumentElement().getAttribute("deprecated"));
  }

  @Test
  public void createXMLStringMarksPlaceOnGround() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    exporter.setPlaceOnGround(true);
    Document doc = parseXml(ModelResourceXmlGenerator.createXMLString(exporter));
    assertEquals("TRUE", doc.getDocumentElement().getAttribute("placeOnGround"));
  }

  // ── createXMLFile ───────────────────────────────────────────────

  @Test
  public void createXMLFileWritesValidXmlToExpectedPath() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    Path root = newTestWorkDir("xml-generator-file");
    File xmlFile = ModelResourceXmlGenerator.createXMLFile(exporter, root.toString(), true);

    assertTrue(Files.isRegularFile(xmlFile.toPath()));
    String content = Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8);
    Document doc = parseXml(content);
    assertEquals("TestProp", doc.getDocumentElement().getAttribute("name"));
  }

  @Test
  public void createXMLFileCopiesExistingWhenNotForceRebuild() throws Exception {
    Path root = newTestWorkDir("xml-copy-existing");
    Path existingXml = root.resolve("existing.xml");
    Files.writeString(existingXml, "<AliceModel name=\"Existing\"/>", StandardCharsets.UTF_8);

    ModelResourceExporter exporter = createMinimalPropExporter();
    exporter.setXMLFile(existingXml.toFile());

    File result = ModelResourceXmlGenerator.createXMLFile(exporter, root.toString(), false);

    String content = Files.readString(result.toPath(), StandardCharsets.UTF_8);
    assertTrue("Should copy existing XML content", content.contains("Existing"));
  }

  @Test
  public void createXMLFileForceRebuildIgnoresExistingFile() throws Exception {
    Path root = newTestWorkDir("xml-force-rebuild");
    Path existingXml = root.resolve("existing.xml");
    Files.writeString(existingXml, "<AliceModel name=\"Stale\"/>", StandardCharsets.UTF_8);

    ModelResourceExporter exporter = createMinimalPropExporter();
    exporter.setXMLFile(existingXml.toFile());

    File result = ModelResourceXmlGenerator.createXMLFile(exporter, root.toString(), true);

    String content = Files.readString(result.toPath(), StandardCharsets.UTF_8);
    assertTrue("Force rebuild should generate fresh XML", content.contains("TestProp"));
  }

  // ── helpers ─────────────────────────────────────────────────────

  private static ModelResourceExporter createMinimalPropExporter() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.setBoundingBox("TestProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("TestProp", "Default", "ALICE", null, null);
    return exporter;
  }

  private static Document parseXml(String xml) throws Exception {
    return javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }

  private static Path newTestWorkDir(String name) throws IOException {
    Path workRoot = Path.of("target", "test-work",
        ModelResourceXmlGeneratorTest.class.getSimpleName(), name).toAbsolutePath();
    deleteRecursively(workRoot);
    Files.createDirectories(workRoot);
    return workRoot;
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (Files.notExists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
