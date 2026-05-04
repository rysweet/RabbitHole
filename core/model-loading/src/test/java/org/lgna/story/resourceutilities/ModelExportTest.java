package org.lgna.story.resourceutilities;

import org.alice.math.immutable.AxisAlignedBox;
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ModelExportTest {

  @Test
  public void modelExporterCreatesXmlForClassAndDefaultResource() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();

    Document xml = parseXml(exporter.createXMLString());

    Element root = xml.getDocumentElement();
    assertEquals("AliceModel", root.getNodeName());
    assertEquals("TestProp", root.getAttribute("name"));
    assertEquals("Alice Test", root.getAttribute("creator"));
    assertEquals("2026", root.getAttribute("creationYear"));
    assertEquals("TRUE", root.getAttribute("placeOnGround"));
    assertEquals("class-tag", root.getElementsByTagName("Tag").item(0).getTextContent());
    assertEquals("class-group", root.getElementsByTagName("GroupTag").item(0).getTextContent());
    assertEquals("class-theme", root.getElementsByTagName("ThemeTag").item(0).getTextContent());

    Element resource = (Element) root.getElementsByTagName("Resource").item(0);
    assertNotNull(resource);
    assertEquals("DEFAULT", resource.getAttribute("resourceName"));
    assertEquals("TestProp", resource.getAttribute("modelName"));
    assertEquals("DEFAULT", resource.getAttribute("textureName"));
    assertEquals("Resource Artist", resource.getAttribute("creator"));
    assertEquals("2025", resource.getAttribute("creationYear"));
  }

  @Test
  public void modelExporterCreatesCompilableResourceJavaCode() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();

    String javaCode = exporter.createJavaCode();

    assertTrue(javaCode.contains("package org.lgna.story.resources.prop;"));
    assertTrue(javaCode.contains("public enum TestPropResource implements org.lgna.story.resources.PropResource"));
    assertTrue(javaCode.contains("DEFAULT;"));
    assertTrue(javaCode.contains("createImplementation"));
    assertCompiles("org/lgna/story/resources/prop/TestPropResource.java", javaCode);
  }

  @Test
  public void modelExporterKeepsGeneratedEnumConstantsAndResourceTypes() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    exporter.addResource("VariantProp", "Default", "SIMS2", null, null);

    String javaCode = exporter.createJavaCode();

    assertTrue(javaCode.contains("DEFAULT,"));
    assertTrue(javaCode.contains("VARIANT_PROP( ImplementationAndVisualType.SIMS2 )"));
    assertCompiles("org/lgna/story/resources/prop/TestPropResource.java", javaCode);
  }

  @Test
  public void modelExporterOnlyWritesSubResourceTagsUniqueFromParent() throws Exception {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addTags("shared-tag");
    exporter.addGroupTags("shared-group");
    exporter.addThemeTags("shared-theme");
    exporter.setBoundingBox("TestProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("VariantProp", "Default", "ALICE", null, null);
    exporter.setBoundingBox("VariantProp", AxisAlignedBox.createAxisAlignedBox(-0.5, 0.0, -0.5, 0.5, 1.0, 0.5));
    exporter.addSubResourceTags("VariantProp", "Default", "shared-tag", "variant-tag");
    exporter.addSubResourceGroupTags("VariantProp", "Default", "shared-group", "variant-group");
    exporter.addSubResourceThemeTags("VariantProp", "Default", "shared-theme", "variant-theme");

    Document xml = parseXml(exporter.createXMLString());

    Element resource = (Element) xml.getDocumentElement().getElementsByTagName("Resource").item(0);
    assertOnlyChildText(resource, "Tag", "variant-tag");
    assertOnlyChildText(resource, "GroupTag", "variant-group");
    assertOnlyChildText(resource, "ThemeTag", "variant-theme");
  }

  @Test
  public void createXmlFileSurfacesOutputFailures() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    Path rootFile = newTestWorkDir("xml-output-failure").resolve("not-a-directory");
    Files.writeString(rootFile, "blocks child paths", StandardCharsets.UTF_8);

    assertThrows(IOException.class, () -> exporter.createXMLFile(rootFile.toString(), true));

    assertTrue(Files.isRegularFile(rootFile));
  }

  @Test
  public void saveThumbnailsSurfacesBadThumbnailWithoutDeletingIt() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    Path root = newTestWorkDir("bad-thumbnail");
    String thumbnailName = AliceResourceUtilities.getThumbnailResourceFileName("TestProp", "Default");
    Path thumbnailPath = Path.of(exporter.getThumbnailPath(root.toString(), thumbnailName));
    Files.createDirectories(thumbnailPath.getParent());
    Files.writeString(thumbnailPath, "not an image", StandardCharsets.UTF_8);
    exporter.addExistingThumbnail(thumbnailName, thumbnailPath.toFile());

    IOException error = assertThrows(IOException.class, () -> exporter.saveThumbnailsToDir(root.toString()));

    assertTrue(error.getMessage().contains("Failed to create class thumbnail"));
    assertTrue("Bad thumbnail should be preserved for diagnosis", Files.exists(thumbnailPath));
  }

  private static ModelResourceExporter createSyntheticPropExporter() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addAttribution("Alice Test", "2026");
    exporter.setPlaceOnGround(true);
    exporter.addTags("class-tag");
    exporter.addGroupTags("class-group");
    exporter.addThemeTags("class-theme");
    exporter.setBoundingBox("TestProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("TestProp", "Default", "ALICE", "Resource Artist", "2025");
    return exporter;
  }

  private static Document parseXml(String xml) throws Exception {
    return javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }

  private static void assertOnlyChildText(Element parent, String childTag, String expectedText) {
    NodeList nodes = parent.getElementsByTagName(childTag);
    assertEquals(1, nodes.getLength());
    assertEquals(expectedText, nodes.item(0).getTextContent());
  }

  private static void assertCompiles(String sourcePath, String source) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull("Tests must run on a JDK, not a JRE", compiler);

    Path workRoot = newTestWorkDir("compiler");
    Path sourceRoot = workRoot.resolve("source");
    Path classRoot = workRoot.resolve("classes");
    Path sourceFile = sourceRoot.resolve(sourcePath);
    Files.createDirectories(sourceFile.getParent());
    Files.createDirectories(classRoot);
    Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

    ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
    int result = compiler.run(
        null,
        compilerOutput,
        compilerOutput,
        "-classpath",
        System.getProperty("java.class.path"),
        "-d",
        classRoot.toString(),
        sourceFile.toString()
    );

    assertEquals(compilerOutput.toString(StandardCharsets.UTF_8), 0, result);
  }

  private static Path newTestWorkDir(String name) throws IOException {
    Path workRoot = Path.of("target", "test-work", ModelExportTest.class.getSimpleName(), name).toAbsolutePath();
    deleteRecursively(workRoot);
    Files.createDirectories(workRoot);
    return workRoot;
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (Files.notExists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths.sorted(Comparator.reverseOrder()).forEach(ModelExportTest::deleteIfExists);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static void deleteIfExists(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
