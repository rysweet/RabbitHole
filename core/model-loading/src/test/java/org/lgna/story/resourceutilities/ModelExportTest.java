package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.alice.math.immutable.AxisAlignedBox;
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
  public void addResourceOmitsRedundantAndBlankAttributionFromXml() throws Exception {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addAttribution("Alice Test", "2026");
    exporter.addResource("MatchingAttributionProp", "Default", "ALICE", "Alice Test", "2026");
    exporter.addResource("BlankAttributionProp", "Default", "ALICE", "", "");

    Document xml = parseXml(exporter.createXMLString());
    Element root = xml.getDocumentElement();
    assertEquals("Alice Test", root.getAttribute("creator"));
    assertEquals("2026", root.getAttribute("creationYear"));

    NodeList resources = root.getElementsByTagName("Resource");
    assertEquals(2, resources.getLength());
    assertResourceWithoutAttribution(findResourceByModelName(resources, "MatchingAttributionProp"));
    assertResourceWithoutAttribution(findResourceByModelName(resources, "BlankAttributionProp"));
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
  public void modelExporterPreservesDeprecatedMetadataInXmlAndGeneratedJava() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    exporter.setIsDeprecated(true);

    Document xml = parseXml(exporter.createXMLString());
    String javaCode = exporter.createJavaCode();

    assertEquals("TRUE", xml.getDocumentElement().getAttribute("deprecated"));
    assertFalse(((Element) xml.getDocumentElement().getElementsByTagName("Resource").item(0)).hasAttribute("deprecated"));
    assertTrue(javaCode.contains("@Deprecated"));
    assertAppearsBefore(javaCode, "@Deprecated", "public enum TestPropResource");
    assertTrue(javaCode.contains("public enum TestPropResource implements org.lgna.story.resources.PropResource"));
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
  public void modelExporterHonorsForcedEnumNamesWithoutTrailingComma() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    exporter.addResource("VariantProp", "Default", "SIMS2", null, null);
    exporter.addForcedEnumNames(null, Collections.singletonList("DEFAULT"));

    String javaCode = exporter.createJavaCode();

    assertTrue(javaCode.contains("\tDEFAULT;"));
    assertFalse(javaCode.contains("VARIANT_PROP"));
    assertCompiles("org/lgna/story/resources/prop/TestPropResource.java", javaCode);
  }

  @Test
  public void modelExporterWritesJointFieldsInParentReadyOrder() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    exporter.setJointMap(Arrays.asList(
        Tuple2.createInstance("hand", "arm"),
        Tuple2.createInstance("root", null),
        Tuple2.createInstance("finger", "hand"),
        Tuple2.createInstance("arm", "root")));

    String javaCode = exporter.createJavaCode();

    assertAppearsBefore(javaCode, "JointId root =", "JointId arm =");
    assertAppearsBefore(javaCode, "JointId arm =", "JointId hand =");
    assertAppearsBefore(javaCode, "JointId hand =", "JointId finger =");
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
  public void subResourceTagsWithNullTextureApplyToEveryMatchingModel() throws Exception {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.setBoundingBox("TestProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("VariantProp", "Default", "ALICE", null, null);
    exporter.addResource("VariantProp", "Blue", "ALICE", null, null);
    exporter.setBoundingBox("VariantProp", AxisAlignedBox.createAxisAlignedBox(-0.5, 0.0, -0.5, 0.5, 1.0, 0.5));

    exporter.addSubResourceTags("VariantProp", null, "variant-tag");

    Document xml = parseXml(exporter.createXMLString());
    NodeList resources = xml.getDocumentElement().getElementsByTagName("Resource");

    assertEquals(2, resources.getLength());
    assertOnlyChildText((Element) resources.item(0), "Tag", "variant-tag");
    assertOnlyChildText((Element) resources.item(1), "Tag", "variant-tag");
  }

  @Test
  public void modelExporterComputesClassBoundingBoxFromSubResourceBounds() throws Exception {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addResource("FirstProp", "Default", "ALICE", null, null);
    exporter.addResource("SecondProp", "Default", "ALICE", null, null);
    exporter.setBoundingBox("FirstProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.setBoundingBox("SecondProp", AxisAlignedBox.createAxisAlignedBox(-3.0, -1.0, -4.0, 2.0, 4.0, 5.0));

    Document xml = parseXml(exporter.createXMLString());
    Element classBox = (Element) xml.getDocumentElement().getElementsByTagName("BoundingBox").item(0);
    Element min = (Element) classBox.getElementsByTagName("Min").item(0);
    Element max = (Element) classBox.getElementsByTagName("Max").item(0);

    assertEquals("-3.0", min.getAttribute("x"));
    assertEquals("-1.0", min.getAttribute("y"));
    assertEquals("-4.0", min.getAttribute("z"));
    assertEquals("2.0", max.getAttribute("x"));
    assertEquals("4.0", max.getAttribute("y"));
    assertEquals("5.0", max.getAttribute("z"));
  }

  @Test
  public void createXmlStringPopulatesMissingBoundingBoxes() throws Exception {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addResource("VariantProp", "Default", "ALICE", null, null);
    AxisAlignedBox variantBox = AxisAlignedBox.createAxisAlignedBox(-0.5, 0.0, -0.5, 0.5, 1.0, 0.5);
    exporter.setBoundingBox("VariantProp", variantBox);
    ModelSubResourceExporter subResource = exporter.getSubResources().get(0);

    assertNull(exporter.getBoundingBox("TestProp"));
    assertNull(subResource.getBbox());

    assertNotNull(exporter.createXMLString());

    assertEquals(variantBox, exporter.getBoundingBox("TestProp"));
    assertEquals(variantBox, subResource.getBbox());
  }

  @Test
  public void createXmlFileWritesPackageResourcePathAndGeneratedXml() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    Path root = newTestWorkDir("xml-file");

    File xmlFile = exporter.createXMLFile(root.toString(), true);

    assertEquals(root.resolve("org/lgna/story/resources/prop/TestProp.xml"), xmlFile.toPath());
    Document xml = parseXml(Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8));
    assertEquals("TestProp", xml.getDocumentElement().getAttribute("name"));
    assertEquals("DEFAULT", ((Element) xml.getDocumentElement().getElementsByTagName("Resource").item(0)).getAttribute("resourceName"));
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

  @Test
  public void saveThumbnailsFailsWhenNoSubResourcesWereRegistered() throws Exception {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    Path root = newTestWorkDir("no-subresources");

    IOException error = assertThrows(IOException.class, () -> exporter.saveThumbnailsToDir(root.toString()));

    assertTrue(error.getMessage().contains("no sub resources were registered"));
  }

  @Test
  public void saveThumbnailsFailsWhenRegisteredThumbnailDisappearsBeforeSave() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    Path root = newTestWorkDir("deleted-thumbnail");
    String thumbnailName = AliceResourceUtilities.getThumbnailResourceFileName("TestProp", "Default");
    Path thumbnailPath = Path.of(exporter.getThumbnailPath(root.toString(), thumbnailName));
    Files.createDirectories(thumbnailPath.getParent());
    Files.writeString(thumbnailPath, "removed before save", StandardCharsets.UTF_8);
    exporter.addExistingThumbnail(thumbnailName, thumbnailPath.toFile());
    Files.delete(thumbnailPath);

    FileNotFoundException error = assertThrows(FileNotFoundException.class, () -> exporter.saveThumbnailsToDir(root.toString()));

    assertTrue(error.getMessage().contains(thumbnailPath.toString()));
  }

  @Test
  public void saveThumbnailsCreatesClassThumbnailFromFirstResourceThumbnail() throws Exception {
    ModelResourceExporter exporter = createSyntheticPropExporter();
    Path root = newTestWorkDir("valid-thumbnail");
    String thumbnailName = AliceResourceUtilities.getThumbnailResourceFileName("TestProp", "Default");
    Path thumbnailPath = Path.of(exporter.getThumbnailPath(root.toString(), thumbnailName));
    Files.createDirectories(thumbnailPath.getParent());
    BufferedImage thumbnail = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
    thumbnail.setRGB(0, 0, 0xFFFF0000);
    ImageIO.write(thumbnail, "png", thumbnailPath.toFile());
    exporter.addExistingThumbnail(thumbnailName, thumbnailPath.toFile());

    List<File> savedThumbnails = exporter.saveThumbnailsToDir(root.toString());

    String classThumbnailName = AliceResourceUtilities.getThumbnailResourceFileName("TestProp", null);
    Path classThumbnailPath = Path.of(exporter.getThumbnailPath(root.toString(), classThumbnailName));
    assertEquals(2, savedThumbnails.size());
    assertTrue(savedThumbnails.contains(thumbnailPath.toFile()));
    assertTrue(savedThumbnails.contains(classThumbnailPath.toFile()));
    BufferedImage classThumbnail = ImageIO.read(classThumbnailPath.toFile());
    assertNotNull(classThumbnail);
    assertEquals(3, classThumbnail.getWidth());
    assertEquals(2, classThumbnail.getHeight());
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

  private static void assertResourceWithoutAttribution(Element resource) {
    assertFalse(resource.hasAttribute("creator"));
    assertFalse(resource.hasAttribute("creationYear"));
  }

  private static Element findResourceByModelName(NodeList resources, String modelName) {
    for (int i = 0; i < resources.getLength(); i++) {
      Element resource = (Element) resources.item(i);
      if (modelName.equals(resource.getAttribute("modelName"))) {
        return resource;
      }
    }
    throw new AssertionError("Resource not found for modelName " + modelName);
  }

  private static void assertAppearsBefore(String text, String first, String second) {
    int firstIndex = text.indexOf(first);
    int secondIndex = text.indexOf(second);
    assertTrue(first + " should be present", firstIndex >= 0);
    assertTrue(second + " should be present", secondIndex >= 0);
    assertTrue(first + " should appear before " + second, firstIndex < secondIndex);
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
