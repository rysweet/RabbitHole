package org.lgna.project.io;

import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import org.alice.tweedle.file.ImageReference;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.story.SProgram;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HistoricalArchiveRoundTripCharacterizationTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedClassArchiveCharacterizesXmlFallbackTypeRoundTripWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-type-texture.png", 0xFF336699);
    NamedUserType generatedType = typeReferencingImageResource("GeneratedHistoricalType", imageResource);
    File typeArchive = temporaryFolder.newFile("generated-historical-type.a3c");

    IoUtilities.writeType(typeArchive, generatedType);

    assertXmlTypeArchiveFacts(typeArchive);
    TypeResourcesPair firstRead = IoUtilities.readType(typeArchive);
    assertTypeFacts(firstRead, "GeneratedHistoricalType", imageResource);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-type-roundtrip.a3c");
    IoUtilities.writeType(roundTripArchive, firstRead.getType());

    assertXmlTypeArchiveFacts(roundTripArchive);
    TypeResourcesPair secondRead = IoUtilities.readType(roundTripArchive);
    assertTypeFacts(secondRead, "GeneratedHistoricalType", imageResource);
  }

  @Test
  public void generatedWorldArchiveCharacterizesManifestProgramRoundTripWithoutExternalFixture() throws Exception {
    Project project = new Project(programType("GeneratedHistoricalProgram"), Project.SceneCameraType.VRHeadset);
    File exportArchive = temporaryFolder.newFile("generated-historical-world.a3w");

    IoUtilities.exportProject(exportArchive, project);

    assertWorldManifestFacts(exportArchive, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);
    Project firstRead = IoUtilities.readProject(exportArchive);
    assertProgramFacts(firstRead, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-world-roundtrip.a3w");
    IoUtilities.exportProject(roundTripArchive, firstRead);

    assertWorldManifestFacts(roundTripArchive, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);
    Project secondRead = IoUtilities.readProject(roundTripArchive);
    assertProgramFacts(secondRead, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);
  }

  @Test
  public void generatedWorldArchiveCharacterizesManifestResourceReadbackLimitWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-world-texture.png", 0xFF663399);
    Project project = new Project(
        typeReferencingImageResource("GeneratedResourceWorld", imageResource),
        Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    File exportArchive = temporaryFolder.newFile("generated-resource-world.a3w");

    IoUtilities.exportProject(exportArchive, project);

    assertWorldResourceManifestFacts(exportArchive, "GeneratedResourceWorld", imageResource);
    Project readProject = IoUtilities.readProject(exportArchive);

    // This deliberately documents the current readback limit; the simple .a3w program test covers re-export roundtrip.
    assertNull("Resource expressions currently exceed supported Tweedle rehydration for .a3w program types.",
        readProject.getProgramType());
    Resource readResource = onlyResource(readProject.getResources());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals(imageResource.getName(), readResource.getName());
    assertEquals(imageResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(imageResource.getContentType(), readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void generatedProjectArchiveCharacterizesXmlFallbackProjectRoundTripWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-project-texture.png", 0xFF996633);
    Project project = new Project(
        typeReferencingImageResource("GeneratedHistoricalProject", imageResource),
        Project.SceneCameraType.VRHeadset);
    project.addResource(imageResource);
    File projectArchive = temporaryFolder.newFile("generated-historical-project.a3p");

    IoUtilities.writeProject(projectArchive, project);

    assertXmlProjectArchiveFacts(
        projectArchive,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);
    Project firstRead = IoUtilities.readProject(projectArchive);
    assertProjectResourceFacts(
        firstRead,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-project-roundtrip.a3p");
    IoUtilities.writeProject(roundTripArchive, firstRead);

    assertXmlProjectArchiveFacts(
        roundTripArchive,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);
    Project secondRead = IoUtilities.readProject(roundTripArchive);
    assertProjectResourceFacts(
        secondRead,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);
  }

  private static void assertXmlTypeArchiveFacts(File archive) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
      assertNotNull("Generated .a3c archives should declare a version", versionEntry);
      assertEquals(ProjectVersion.getCurrentVersion().toString(), readEntry(zipFile, versionEntry).trim());
      assertNotNull("Generated .a3c archives should contain the XML type payload", zipFile.getEntry("type.xml"));
      assertNotNull("Generated .a3c archives should contain resource metadata", zipFile.getEntry("resources.xml"));
      assertNotNull("Generated .a3c archives should contain generated image data",
          zipFile.getEntry("resources/historical-type-texture.png"));
      assertNull("Generated .a3c fixtures intentionally exercise XML fallback rather than JSON manifest loading",
          zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
    }
  }

  private static void assertXmlProjectArchiveFacts(
      File archive,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType,
      ImageResource expectedResource) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
      assertNotNull("Generated .a3p archives should declare a version", versionEntry);
      assertEquals(ProjectVersion.getCurrentVersion().toString(), readEntry(zipFile, versionEntry).trim());
      assertNotNull("Generated .a3p archives should contain a manifest",
          zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull("Generated .a3p archives should contain the XML program payload",
          zipFile.getEntry("programType.xml"));
      assertNotNull("Generated .a3p archives should contain resource metadata",
          zipFile.getEntry("resources.xml"));
      assertNotNull("Generated .a3p archives should contain generated image data",
          zipFile.getEntry("resources/" + expectedResource.getName()));
      assertNull("Generated .a3p fixtures intentionally exercise XML fallback despite manifest.json",
          zipFile.getEntry("src/" + expectedProgramName + ".twe"));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(expectedProgramName, manifest.description.name);
      assertEquals(IoUtilities.PROJECT_EXTENSION, manifest.metadata.fileType);
      assertEquals(Manifest.ProjectType.World, manifest.metadata.identifier.type);
      assertEquals(expectedSceneCameraType, manifest.projectStructure.sceneCameraType);
    }
  }

  private static void assertTypeFacts(TypeResourcesPair typeResourcesPair, String expectedTypeName, ImageResource expectedResource) {
    assertNotNull("Generated .a3c archive should read a type/resources pair", typeResourcesPair);
    NamedUserType readType = typeResourcesPair.getType();
    assertNotNull("Generated .a3c archive should rehydrate a type", readType);
    assertEquals(expectedTypeName, readType.getName());
    assertEquals("SProgram", readType.getSuperType().getName());

    Resource readResource = onlyResource(typeResourcesPair.getResources());
    assertEquals(expectedResource.getId(), readResource.getId());
    assertEquals(expectedResource.getName(), readResource.getName());
    assertEquals(expectedResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(expectedResource.getContentType(), readResource.getContentType());
    assertArrayEquals(expectedResource.getData(), readResource.getData());
    assertEquals(readResource, firstResourceExpressionResource(readType));
  }

  private static void assertProjectResourceFacts(
      Project project,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType,
      ImageResource expectedResource) throws Exception {
    assertNotNull("Generated .a3p archive should reopen as a project", project);
    NamedUserType readType = project.getProgramType();
    assertNotNull("Generated .a3p XML fallback should rehydrate the program type", readType);
    assertEquals(expectedProgramName, readType.getName());
    assertEquals("SProgram", readType.getSuperType().getName());
    assertEquals(expectedSceneCameraType, sceneCameraType(project));

    Resource readResource = onlyResource(project.getResources());
    assertEquals(expectedResource.getId(), readResource.getId());
    assertEquals(expectedResource.getName(), readResource.getName());
    assertEquals(expectedResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(expectedResource.getContentType(), readResource.getContentType());
    assertArrayEquals(expectedResource.getData(), readResource.getData());
    assertSame("Resource expressions should be rebound to the resource decoded from the archive",
        readResource,
        firstResourceExpressionResource(readType));
  }

  private static void assertWorldManifestFacts(
      File archive,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      assertNotNull("Generated .a3w archives should declare a version",
          zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull("Generated .a3w archives should contain a manifest",
          zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull("Generated .a3w archives should contain Tweedle source for the program",
          zipFile.getEntry("src/" + expectedProgramName + ".twe"));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(expectedProgramName, manifest.description.name);
      assertEquals(IoUtilities.EXPORT_EXTENSION, manifest.metadata.fileType);
      assertEquals(Manifest.ProjectType.World, manifest.metadata.identifier.type);
      assertEquals(expectedSceneCameraType, manifest.projectStructure.sceneCameraType);
      assertTrue("Generated .a3w archives should include the standard library prerequisite",
          manifest.prerequisites.stream().anyMatch(identifier -> "SceneGraphLibrary".equals(identifier.name)));
      assertTypeReference(manifest, expectedProgramName, "src/" + expectedProgramName + ".twe");
    }
  }

  private static void assertWorldResourceManifestFacts(
      File archive,
      String expectedProgramName,
      ImageResource expectedResource) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      assertWorldManifestFacts(archive, expectedProgramName, Project.SceneCameraType.WindowCamera);
      assertNotNull("Generated .a3w archives should contain exported image data",
          zipFile.getEntry("resources/" + expectedResource.getName()));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertImageReference(
          manifest,
          expectedResource.getId(),
          expectedResource.getName(),
          "resources/" + expectedResource.getName());
    }
  }

  private static void assertProgramFacts(
      Project project,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType) throws Exception {
    assertNotNull("Generated .a3w archive should reopen as a project", project);
    assertNotNull("Simple generated .a3w program source should decode", project.getProgramType());
    assertEquals(expectedProgramName, project.getProgramType().getName());
    assertEquals("SProgram", project.getProgramType().getSuperType().getName());
    assertEquals(expectedSceneCameraType, sceneCameraType(project));
    assertTrue(project.getResources().isEmpty());
  }

  private static void assertTypeReference(ProjectManifest manifest, String expectedName, String expectedFile) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference && expectedName.equals(typeReference.name)) {
        assertEquals(expectedFile, typeReference.file);
        assertEquals("tweedle", typeReference.format);
        return;
      }
    }
    throw new AssertionError("Missing type reference for " + expectedName);
  }

  private static void assertImageReference(ProjectManifest manifest, UUID expectedId, String expectedName, String expectedFile) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof ImageReference imageReference && expectedId.equals(imageReference.uuid)) {
        assertEquals(expectedName, imageReference.name);
        assertEquals(expectedFile, imageReference.file);
        assertEquals("png", imageReference.format);
        assertEquals(1.0, imageReference.width, 0.0);
        assertEquals(1.0, imageReference.height, 0.0);
        return;
      }
    }
    throw new AssertionError("Missing image reference for " + expectedId);
  }

  private static ProjectManifest readProjectManifest(ZipFile zipFile) throws Exception {
    return ManifestEncoderDecoder.fromJson(
        readEntry(zipFile, zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME)),
        ProjectManifest.class);
  }

  private static String readEntry(ZipFile zipFile, ZipEntry zipEntry) throws Exception {
    assertNotNull("Expected archive entry to exist", zipEntry);
    return new String(zipFile.getInputStream(zipEntry).readAllBytes(), StandardCharsets.UTF_8);
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static NamedUserType typeReferencingImageResource(String name, ImageResource imageResource) {
    NamedUserType type = programType(name);
    BlockStatement body = new BlockStatement();
    UserLocal resource = new UserLocal("resource", ImageResource.class, true);
    body.statements.add(new LocalDeclarationStatement(resource, new ResourceExpression(ImageResource.class, imageResource)));
    type.methods.add(new UserMethod("rememberGeneratedFixtureResource", Void.TYPE, new org.lgna.project.ast.UserParameter[0], body));
    return type;
  }

  private static ImageResource generatedImageResource(String fileName, int rgb) throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, rgb);
    return new ImageResource(image, fileName, "png");
  }

  private static Resource onlyResource(Collection<Resource> resources) {
    assertEquals(1, resources.size());
    return resources.iterator().next();
  }

  private static Resource firstResourceExpressionResource(NamedUserType type) {
    IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<ResourceExpression>(ResourceExpression.class) {
      @Override
      protected boolean isAcceptable(ResourceExpression resourceExpression) {
        return true;
      }
    };
    type.crawl(crawler, CrawlPolicy.COMPLETE);
    assertFalse(crawler.getList().isEmpty());
    return crawler.getList().get(0).resource.getValue();
  }

  private static Project.SceneCameraType sceneCameraType(Project project) throws Exception {
    Field field = Project.class.getDeclaredField("sceneCameraType");
    field.setAccessible(true);
    return (Project.SceneCameraType) field.get(project);
  }
}
