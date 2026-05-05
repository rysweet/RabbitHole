package org.lgna.project.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.Version;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.migration.ProjectMigrationManager;
import org.lgna.story.resourceutilities.ModelResourceInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StarterProjectXmlFallbackReadabilityTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final List<StarterProjectExpectation> REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES = Arrays.asList(
      new StarterProjectExpectation("magic2.a3p", 113,
          "castleTowerBase", "magicStone", "stoneBridge", "castleGate"),
      new StarterProjectExpectation("lagoonMinimum.a3p", 32,
          "cave", "sandcastle34", "snailHouse", "oldShipWheel"),
      new StarterProjectExpectation("wonderland.a3p", 316,
          "mushroom", "teaTable", "hedge", "wonderlandTree"));

  private static final String PROGRAM_TYPE_ENTRY_NAME = "programType.xml";
  private static final String MOUSE_CLICK_OBJECT_SOURCE =
      "<method isVarArgs=\"false\" name=\"getModelAtMouseLocation\"><declaringClass name=\"org.lgna.story.event.MouseClickEvent\"/><parameters/></method>";
  private static final String DISTANCE_TO_STURNABLE_SOURCE =
      "<method isVarArgs=\"true\" name=\"getDistanceTo\"><declaringClass name=\"org.lgna.story.STurnable\"/><parameters><type name=\"org.lgna.story.STurnable\"/>";

  @Test
  public void representativeStarterProjectFixturesUseXmlFallbackArchives() throws Exception {
    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      String archiveName = expectation.archiveName;
      Path archive = starterProjectsDirectory().resolve(archiveName);

      try (ZipFile zipFile = new ZipFile(archive.toFile())) {
        ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
        assertNotNull(archiveName + " should declare a project version for XML fallback selection", versionEntry);
        assertFalse(archiveName + " should declare a non-empty project version",
            readTrimmedEntry(zipFile, versionEntry).isEmpty());
        assertNull(archiveName + " should exercise XML fallback rather than manifest loading",
            zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      }
    }
  }

  @Test
  public void representativeStarterProjectFixturesReadThroughXmlFallback() throws Exception {
    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      String archiveName = expectation.archiveName;
      Path archive = starterProjectsDirectory().resolve(archiveName);

      assertNull(archiveName + " historical version should not be treated as future",
          IoUtilities.projectReader(archive.toFile()).checkForFutureVersion());

      Project project = IoUtilities.readProject(archive.toFile());

      assertNotNull(archiveName + " should remain readable through project I/O XML fallback", project);
      assertNotNull(archiveName + " should decode a program type", project.getProgramType());
      assertFalse(archiveName + " should decode a named program type", project.getProgramType().getName().isEmpty());
    }
  }

  @Test
  public void representativeStarterProjectFixtureRestoresCommittedResources() throws Exception {
    boolean foundResourceBearingFixture = false;

    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      String archiveName = expectation.archiveName;
      Project project = IoUtilities.readProject(starterProjectsDirectory().resolve(archiveName).toFile());
      Collection<Resource> resources = project.getResources();

      if (!resources.isEmpty()) {
        foundResourceBearingFixture = true;
      }
      for (Resource resource : resources) {
        assertNotNull(archiveName + " should restore resource names", resource.getName());
        assertFalse(archiveName + " should restore non-empty resource names", resource.getName().isEmpty());
        assertNotNull(archiveName + " should restore resource content types", resource.getContentType());
        assertFalse(archiveName + " should restore non-empty content types", resource.getContentType().isEmpty());
        assertNotNull(archiveName + " should restore resource data", resource.getData());
        assertTrue(archiveName + " should restore non-empty resource data", resource.getData().length > 0);
      }
    }

    assertTrue("Representative fixtures should include at least one resource-bearing XML fallback archive",
        foundResourceBearingFixture);
  }

  @Test
  public void lagoonMinimumFixtureRestoresCommittedGroundTextureResourceMetadata() throws Exception {
    Project project = IoUtilities.readProject(starterProjectsDirectory().resolve("lagoonMinimum.a3p").toFile());
    Collection<Resource> resources = project.getResources();

    assertEquals("lagoonMinimum.a3p should restore its single committed texture resource",
        1, resources.size());
    Resource resource = resources.iterator().next();
    assertTrue("lagoonMinimum.a3p should restore the texture as an image resource",
        resource instanceof ImageResource);
    ImageResource imageResource = (ImageResource) resource;
    assertEquals("sandDunesLight_diffuse.png", imageResource.getName());
    assertEquals("sandDunesLight_diffuse.png", imageResource.getOriginalFileName());
    assertEquals("image/png", imageResource.getContentType());
    assertEquals(256, imageResource.getWidth());
    assertEquals(256, imageResource.getHeight());
    assertEquals(79305, imageResource.getData().length);
  }

  @Test
  public void readingCommittedTemplateAndGalleryMetadataDoesNotMutateFixtures() throws Exception {
    Path templateArchive = starterProjectsDirectory().resolve("lagoonMinimum.a3p");
    Path galleryMetadata = galleryDirectory()
        .resolve("assets/alice/aliceModelResources/org/lgna/story/resources/aircraft/SpaceShip.xml");
    byte[] templateBefore = sha256(templateArchive);
    byte[] galleryBefore = sha256(galleryMetadata);

    Project project = IoUtilities.readProject(templateArchive.toFile());
    ModelResourceInfo modelInfo = new ModelResourceInfo(
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(galleryMetadata.toFile()));

    assertNotNull(project.getProgramType());
    assertEquals("SpaceShip", modelInfo.getModelName());
    assertEquals("SpaceShip", modelInfo.createModelManifest().description.name);
    assertArrayEquals("starter template archive should be read-only", templateBefore, sha256(templateArchive));
    assertArrayEquals("gallery metadata should be read-only", galleryBefore, sha256(galleryMetadata));
  }

  @Test
  public void missingCommittedTemplateMediaFailsExplicitlyWithoutMutatingArchive() throws Exception {
    Path sourceArchive = starterProjectsDirectory().resolve("lagoonMinimum.a3p");
    String missingEntry = "resources/sandDunesLight_diffuse.png";
    Path damagedArchive = temporaryFolder.newFile("lagoon-missing-media.a3p").toPath();
    copyArchiveWithoutEntry(sourceArchive, damagedArchive, missingEntry);
    byte[] sourceBefore = sha256(sourceArchive);
    byte[] damagedBefore = sha256(damagedArchive);

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(damagedArchive.toFile()));

    assertTrue(thrown.getMessage().contains(missingEntry));
    assertArrayEquals("committed source fixture should not be mutated by failed read", sourceBefore, sha256(sourceArchive));
    assertArrayEquals("damaged archive should not be mutated by failed read", damagedBefore, sha256(damagedArchive));
  }

  @Test
  public void representativeStarterProjectFixturesDecodeSceneSemantics() throws Exception {
    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      Project project = IoUtilities.readProject(starterProjectsDirectory().resolve(expectation.archiveName).toFile());
      NamedUserType programType = project.getProgramType();

      assertEquals(expectation.archiveName + " should decode the committed program type", "Program",
          programType.getName());
      assertEquals(expectation.archiveName + " should decode a modern program superclass", "SProgram",
          programType.getSuperType().getName());
      assertEquals(expectation.archiveName + " should keep the program-to-scene field boundary", 1,
          programType.fields.size());
      assertTrue(expectation.archiveName + " should decode the program entry point",
          hasMethodNamed(programType, "main"));

      NamedUserType sceneType = sceneTypeFor(expectation.archiveName, programType);

      assertEquals(expectation.archiveName + " should decode the committed scene type", "Scene",
          sceneType.getName());
      assertEquals(expectation.archiveName + " should decode a modern scene superclass", "SScene",
          sceneType.getSuperType().getName());
      assertEquals(expectation.archiveName + " should preserve its committed scene field count",
          expectation.sceneFieldCount, sceneType.fields.size());
      assertTrue(expectation.archiveName + " should decode ground", hasFieldNamed(sceneType, "ground"));
      assertTrue(expectation.archiveName + " should decode camera", hasFieldNamed(sceneType, "camera"));
      for (String fieldName : expectation.representativeSceneFields) {
        assertTrue(expectation.archiveName + " should decode scene field " + fieldName,
            hasFieldNamed(sceneType, fieldName));
      }
      assertTrue(expectation.archiveName + " should decode generated setup",
          hasMethodNamed(sceneType, "performGeneratedSetUp"));
      assertTrue(expectation.archiveName + " should decode user-authored entry behavior",
          hasMethodNamed(sceneType, "myFirstMethod"));
      assertTrue(expectation.archiveName + " should decode event-listener setup",
          hasMethodNamed(sceneType, "initializeEventListeners"));
    }
  }

  @Test
  public void lagoonMinimumFixtureDecodesSpecificStarterModelTypeSemantics() throws Exception {
    String archiveName = "lagoonMinimum.a3p";
    Project project = IoUtilities.readProject(starterProjectsDirectory().resolve(archiveName).toFile());
    NamedUserType sceneType = sceneTypeFor(archiveName, project.getProgramType());

    NamedUserType sandcastleType = namedTypeForField(sceneType, "sandcastle34");

    assertEquals("lagoonMinimum.a3p should decode the sandcastle34 field as committed starter content",
        "Sandcastle", sandcastleType.getName());
    assertEquals("lagoonMinimum.a3p should preserve the starter model constructor shape",
        1, sandcastleType.constructors.size());
    NamedUserConstructor constructor = sandcastleType.constructors.get(0);
    assertEquals("lagoonMinimum.a3p should preserve the starter model resource parameter",
        1, constructor.getRequiredParameters().size());
    UserParameter resourceParameter = constructor.getRequiredParameters().get(0);
    assertEquals("resource", resourceParameter.getName());
    assertEquals("SandcastleResource", resourceParameter.getValueType().getName());
  }

  @Test
  public void representativeStarterProjectFixturesRoundTripAfterMigration() throws Exception {
    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      Project migratedProject = IoUtilities.readProject(starterProjectsDirectory().resolve(expectation.archiveName).toFile());
      assertReadableStarterProjectState(expectation, migratedProject, "migrated legacy read");

      Path savedArchive = temporaryFolder.newFile(expectation.archiveName.replace(".a3p", "-roundtrip.a3p")).toPath();
      IoUtilities.writeProject(savedArchive.toFile(), migratedProject);

      assertTrue(expectation.archiveName + " should save a non-empty migrated project copy",
          Files.size(savedArchive) > 0);

      Project reopenedProject = IoUtilities.readProject(savedArchive.toFile());
      assertReadableStarterProjectState(expectation, reopenedProject, "reopened migrated copy");
      assertEquals(expectation.archiveName + " should preserve resource count after save/reopen",
          migratedProject.getResources().size(), reopenedProject.getResources().size());
      assertEquals(expectation.archiveName + " should preserve resource identities after save/reopen",
          sortedResourceSignatures(migratedProject.getResources()), sortedResourceSignatures(reopenedProject.getResources()));
    }
  }

  @Test
  public void representativeStarterProjectFixtureVersionsEnterMigrationManagerPath() throws Exception {
    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      Path archive = starterProjectsDirectory().resolve(expectation.archiveName);

      try (ZipFile zipFile = new ZipFile(archive.toFile())) {
        ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
        String versionText = readTrimmedEntry(zipFile, versionEntry);
        Version version = new Version(versionText);

        assertEquals(expectation.archiveName + " should characterize the committed starter fixture vintage",
            "3.3.0.0.0", versionText);
        assertTrue(expectation.archiveName + " should still enter text migration selection",
            ProjectMigrationManager.getInstance().hasTextMigrationsFor(version));
        assertTrue(expectation.archiveName + " should still enter AST migration selection",
            ProjectMigrationManager.getInstance().hasAstMigrationsFor(version));
      }
    }
  }

  @Test
  public void representativeStarterProjectFixturesCurrentlyLackKnownPost33MigrationAnchors() throws Exception {
    for (StarterProjectExpectation expectation : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
      Path archive = starterProjectsDirectory().resolve(expectation.archiveName);

      try (ZipFile zipFile = new ZipFile(archive.toFile())) {
        String programXml = readEntry(zipFile, zipFile.getEntry(PROGRAM_TYPE_ENTRY_NAME));

        assertFalse(expectation.archiveName
                + " unexpectedly contains the 3.4 MouseClickEvent source anchor; update this boundary test to assert migrated semantics",
            programXml.contains(MOUSE_CLICK_OBJECT_SOURCE));
        assertFalse(expectation.archiveName
                + " unexpectedly contains the 3.9 getDistanceTo source anchor; update this boundary test to assert migrated semantics",
            programXml.contains(DISTANCE_TO_STURNABLE_SOURCE));
        assertFalse(expectation.archiveName
                + " unexpectedly contains the 3.6 FirTreeTrunk AST source type; update this boundary test to assert migrated semantics",
            programXml.contains("FirTreeTrunk"));
        assertFalse(expectation.archiveName
                + " unexpectedly contains the 3.6 IceFloe AST source type; update this boundary test to assert migrated semantics",
            programXml.contains("IceFloe"));
      }
    }
  }

  private static Path starterProjectsDirectory() {
    Path current = Paths.get("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("core/resources/src/application/resources/starter-projects");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    fail("Unable to find core/resources/src/application/resources/starter-projects from " + Paths.get("").toAbsolutePath());
    return null;
  }

  private static Path galleryDirectory() {
    Path current = Paths.get("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("core/resources/src/application/resources/gallery");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    fail("Unable to find core/resources/src/application/resources/gallery from " + Paths.get("").toAbsolutePath());
    return null;
  }

  private static byte[] sha256(Path file) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
  }

  private static void copyArchiveWithoutEntry(Path source, Path destination, String omittedEntry) throws IOException {
    try (ZipFile zipFile = new ZipFile(source.toFile());
         ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destination.toFile()))) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().equals(omittedEntry)) {
          continue;
        }
        ZipEntry copy = new ZipEntry(entry.getName());
        copy.setTime(entry.getTime());
        zipOutputStream.putNextEntry(copy);
        if (!entry.isDirectory()) {
          try (var inputStream = zipFile.getInputStream(entry)) {
            inputStream.transferTo(zipOutputStream);
          }
        }
        zipOutputStream.closeEntry();
      }
    }
  }

  private static String readTrimmedEntry(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
    return readEntry(zipFile, zipEntry).trim();
  }

  private static String readEntry(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
    assertNotNull("Expected archive entry to exist", zipEntry);
    return new String(zipFile.getInputStream(zipEntry).readAllBytes(), StandardCharsets.UTF_8);
  }

  private static NamedUserType sceneTypeFor(String archiveName, NamedUserType programType) {
    UserField sceneField = programType.fields.get(0);

    assertEquals(archiveName + " should keep the generated scene field name", "myScene",
        sceneField.getName());
    assertTrue(archiveName + " should decode myScene as a named user type",
        sceneField.getValueType() instanceof NamedUserType);
    return (NamedUserType) sceneField.getValueType();
  }

  private static void assertReadableStarterProjectState(
      StarterProjectExpectation expectation, Project project, String readStage) {
    assertNotNull(expectation.archiveName + " should produce a project during " + readStage, project);
    NamedUserType programType = project.getProgramType();
    assertNotNull(expectation.archiveName + " should decode a program type during " + readStage, programType);
    assertEquals(expectation.archiveName + " should decode the committed program type during " + readStage,
        "Program", programType.getName());
    assertEquals(expectation.archiveName + " should decode a modern program superclass during " + readStage,
        "SProgram", programType.getSuperType().getName());
    assertEquals(expectation.archiveName + " should keep the program-to-scene field boundary during " + readStage,
        1, programType.fields.size());
    assertTrue(expectation.archiveName + " should decode the program entry point during " + readStage,
        hasMethodNamed(programType, "main"));

    NamedUserType sceneType = sceneTypeFor(expectation.archiveName, programType);
    assertEquals(expectation.archiveName + " should decode the committed scene type during " + readStage,
        "Scene", sceneType.getName());
    assertEquals(expectation.archiveName + " should decode a modern scene superclass during " + readStage,
        "SScene", sceneType.getSuperType().getName());
    assertEquals(expectation.archiveName + " should preserve its committed scene field count during " + readStage,
        expectation.sceneFieldCount, sceneType.fields.size());
    assertTrue(expectation.archiveName + " should decode ground during " + readStage,
        hasFieldNamed(sceneType, "ground"));
    assertTrue(expectation.archiveName + " should decode camera during " + readStage,
        hasFieldNamed(sceneType, "camera"));
    for (String fieldName : expectation.representativeSceneFields) {
      assertTrue(expectation.archiveName + " should decode scene field " + fieldName + " during " + readStage,
          hasFieldNamed(sceneType, fieldName));
    }
    assertTrue(expectation.archiveName + " should decode generated setup during " + readStage,
        hasMethodNamed(sceneType, "performGeneratedSetUp"));
    assertTrue(expectation.archiveName + " should decode user-authored entry behavior during " + readStage,
        hasMethodNamed(sceneType, "myFirstMethod"));
    assertTrue(expectation.archiveName + " should decode event-listener setup during " + readStage,
        hasMethodNamed(sceneType, "initializeEventListeners"));
  }

  private static List<String> sortedResourceSignatures(Collection<Resource> resources) {
    List<String> signatures = new ArrayList<>();
    for (Resource resource : resources) {
      signatures.add(resource.getName() + "|" + resource.getContentType() + "|" + resource.getData().length);
    }
    Collections.sort(signatures);
    return signatures;
  }

  private static NamedUserType namedTypeForField(NamedUserType type, String fieldName) {
    UserField field = fieldNamed(type, fieldName);

    assertTrue(fieldName + " should decode as a named user type", field.getValueType() instanceof NamedUserType);
    return (NamedUserType) field.getValueType();
  }

  private static UserField fieldNamed(NamedUserType type, String fieldName) {
    for (UserField field : type.fields) {
      if (fieldName.equals(field.getName())) {
        return field;
      }
    }
    throw new AssertionError("Missing scene field " + fieldName);
  }

  private static boolean hasFieldNamed(NamedUserType type, String fieldName) {
    for (UserField field : type.fields) {
      if (fieldName.equals(field.getName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasMethodNamed(NamedUserType type, String methodName) {
    for (UserMethod method : type.methods) {
      if (methodName.equals(method.getName())) {
        return true;
      }
    }
    return false;
  }

  private static class StarterProjectExpectation {
    private final String archiveName;
    private final int sceneFieldCount;
    private final List<String> representativeSceneFields;

    private StarterProjectExpectation(String archiveName, int sceneFieldCount, String... representativeSceneFields) {
      this.archiveName = archiveName;
      this.sceneFieldCount = sceneFieldCount;
      this.representativeSceneFields = Arrays.asList(representativeSceneFields);
    }
  }
}
