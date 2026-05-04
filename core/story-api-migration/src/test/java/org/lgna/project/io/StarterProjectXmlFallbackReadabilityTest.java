package org.lgna.project.io;

import org.junit.Test;
import org.lgna.common.Resource;
import org.lgna.project.Project;
import org.lgna.project.Version;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.migration.ProjectMigrationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StarterProjectXmlFallbackReadabilityTest {
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
