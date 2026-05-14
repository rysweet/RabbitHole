package org.lgna.project.io;

import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.TypeManifest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonProjectManifest}.
 * Verifies manifest reading, name extraction, scene camera type defaults,
 * and data source generation in isolation from the full I/O pipeline.
 */
public class JsonProjectManifestTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // ═══════════════════════════════════════════════════════════════════════════
  // readManifest
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void readManifestReturnsNullWhenEntryIsMissing() throws Exception {
    File archive = writeArchive();
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntryContainer container = new ZipEntryContainer(zipFile);
      ProjectManifest manifest = JsonProjectManifest.readManifest(container, ProjectManifest.class);
      assertNull(manifest);
    }
  }

  @Test
  public void readManifestParsesValidProjectManifest() throws Exception {
    ProjectManifest expected = new ProjectManifest();
    expected.description.name = "TestProgram";
    expected.metadata.fileType = "a3w";
    expected.projectStructure.sceneCameraType = Project.SceneCameraType.VRHeadset;
    File archive = writeArchiveWithManifest(ManifestEncoderDecoder.toJson(expected));
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntryContainer container = new ZipEntryContainer(zipFile);
      ProjectManifest manifest = JsonProjectManifest.readManifest(container, ProjectManifest.class);
      assertNotNull(manifest);
      assertEquals("TestProgram", manifest.description.name);
      assertEquals("a3w", manifest.metadata.fileType);
      assertEquals(Project.SceneCameraType.VRHeadset, manifest.projectStructure.sceneCameraType);
    }
  }

  @Test
  public void readManifestParsesValidTypeManifest() throws Exception {
    TypeManifest expected = new TypeManifest();
    expected.description.name = "TestType";
    expected.metadata.fileType = "a3c";
    File archive = writeArchiveWithManifest(ManifestEncoderDecoder.toJson(expected));
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntryContainer container = new ZipEntryContainer(zipFile);
      TypeManifest manifest = JsonProjectManifest.readManifest(container, TypeManifest.class);
      assertNotNull(manifest);
      assertEquals("TestType", manifest.description.name);
    }
  }

  @Test
  public void readManifestWrapsParseFailureInIOException() throws Exception {
    File archive = writeArchiveWithManifest("{not-valid-json");
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntryContainer container = new ZipEntryContainer(zipFile);
      IOException thrown = assertThrows(IOException.class,
          () -> JsonProjectManifest.readManifest(container, ProjectManifest.class));
      assertTrue(thrown.getMessage().contains(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull(thrown.getCause());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // manifestName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void manifestNameReturnsNullForNullManifest() {
    assertNull(JsonProjectManifest.manifestName(null));
  }

  @Test
  public void manifestNameReturnsNullForManifestWithoutName() {
    Manifest manifest = new ProjectManifest();
    manifest.description.name = null;
    assertNull(JsonProjectManifest.manifestName(manifest));
  }

  @Test
  public void manifestNameReturnsNameForNamedManifest() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "MyProgram";
    assertEquals("MyProgram", JsonProjectManifest.manifestName(manifest));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // hasNoManifestName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void hasNoManifestNameReturnsTrueForNullManifest() {
    assertTrue(JsonProjectManifest.hasNoManifestName(null));
  }

  @Test
  public void hasNoManifestNameReturnsTrueForNullName() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = null;
    assertTrue(JsonProjectManifest.hasNoManifestName(manifest));
  }

  @Test
  public void hasNoManifestNameReturnsTrueForEmptyName() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "";
    assertTrue(JsonProjectManifest.hasNoManifestName(manifest));
  }

  @Test
  public void hasNoManifestNameReturnsFalseForNamedManifest() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    assertFalse(JsonProjectManifest.hasNoManifestName(manifest));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // sceneCameraType
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void sceneCameraTypeDefaultsToWindowCameraForNullManifest() {
    assertEquals(Project.SceneCameraType.WindowCamera,
        JsonProjectManifest.sceneCameraType(null));
  }

  @Test
  public void sceneCameraTypeDefaultsToWindowCameraForNullProjectStructure() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.projectStructure = null;
    assertEquals(Project.SceneCameraType.WindowCamera,
        JsonProjectManifest.sceneCameraType(manifest));
  }

  @Test
  public void sceneCameraTypeDefaultsToWindowCameraForNullCameraType() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.projectStructure.sceneCameraType = null;
    assertEquals(Project.SceneCameraType.WindowCamera,
        JsonProjectManifest.sceneCameraType(manifest));
  }

  @Test
  public void sceneCameraTypeReturnsVRHeadsetWhenSet() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.VRHeadset;
    assertEquals(Project.SceneCameraType.VRHeadset,
        JsonProjectManifest.sceneCameraType(manifest));
  }

  @Test
  public void sceneCameraTypeReturnsWindowCameraWhenSet() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    assertEquals(Project.SceneCameraType.WindowCamera,
        JsonProjectManifest.sceneCameraType(manifest));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // versionDataSource
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void versionDataSourceUsesCorrectEntryName() {
    assertEquals(ProjectIo.VERSION_ENTRY_NAME,
        JsonProjectManifest.versionDataSource().getName());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // manifestDataSource
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void manifestDataSourceUsesCorrectEntryName() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Program";
    assertEquals(ProjectIo.MANIFEST_ENTRY_NAME,
        JsonProjectManifest.manifestDataSource(manifest).getName());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private File writeArchive() throws Exception {
    File file = temporaryFolder.newFile("empty.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
      zos.putNextEntry(new java.util.zip.ZipEntry("placeholder.txt"));
      zos.write("placeholder".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return file;
  }

  private File writeArchiveWithManifest(String manifestJson) throws Exception {
    File file = temporaryFolder.newFile("manifest.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
      zos.putNextEntry(new java.util.zip.ZipEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      zos.write(manifestJson.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return file;
  }
}
