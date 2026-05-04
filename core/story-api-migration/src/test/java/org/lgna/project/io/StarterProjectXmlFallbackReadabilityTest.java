package org.lgna.project.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.project.Project;
import org.lgna.story.resourceutilities.ModelResourceInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
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

  private static final List<String> REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES = Arrays.asList(
      "magic2.a3p",
      "lagoonMinimum.a3p",
      "wonderland.a3p");

  @Test
  public void representativeStarterProjectFixturesUseXmlFallbackArchives() throws Exception {
    for (String archiveName : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
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
    for (String archiveName : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
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

    for (String archiveName : REPRESENTATIVE_LEGACY_PROJECT_ARCHIVES) {
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
    return new String(zipFile.getInputStream(zipEntry).readAllBytes(), StandardCharsets.UTF_8).trim();
  }
}
