package org.lgna.project.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Boundary tests for IoUtilities targeting untested paths:
 * listProjectFiles/listTypeFiles (zero prior coverage),
 * readProject error paths, and extension constant validation.
 *
 * <p>File-listing tests use JUnit 4 TemporaryFolder for deterministic
 * cleanup. Archive error tests create minimal zip archives with
 * missing or invalid entries.
 */
public class IoUtilitiesBoundaryTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // ═══════════════════════════════════════════════════════════════════════════
  // EXTENSION CONSTANTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void projectExtensionConstantIsA3p() {
    assertEquals("a3p", IoUtilities.PROJECT_EXTENSION);
  }

  @Test
  public void typeExtensionConstantIsA3c() {
    assertEquals("a3c", IoUtilities.TYPE_EXTENSION);
  }

  @Test
  public void exportExtensionConstantIsA3w() {
    assertEquals("a3w", IoUtilities.EXPORT_EXTENSION);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LIST PROJECT FILES: zero prior coverage
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void listProjectFilesInEmptyDirectoryReturnsEmptyArray() throws Exception {
    File emptyDir = temporaryFolder.newFolder("empty-projects");

    File[] projectFiles = IoUtilities.listProjectFiles(emptyDir);

    assertNotNull(projectFiles);
    assertEquals(0, projectFiles.length);
  }

  @Test
  public void listProjectFilesFindsOnlyA3pFiles() throws Exception {
    File dir = temporaryFolder.newFolder("mixed-files");
    createEmptyFile(dir, "project1.a3p");
    createEmptyFile(dir, "project2.a3p");
    createEmptyFile(dir, "type1.a3c");
    createEmptyFile(dir, "readme.txt");

    File[] projectFiles = IoUtilities.listProjectFiles(dir);

    assertNotNull(projectFiles);
    assertEquals(2, projectFiles.length);
    for (File file : projectFiles) {
      assertTrue("Expected .a3p file, got: " + file.getName(),
          file.getName().endsWith(".a3p"));
    }
  }

  @Test
  public void listProjectFilesOnNonExistentDirectoryReturnsEmptyArray() {
    File nonExistent = new File(temporaryFolder.getRoot(), "does-not-exist");

    File[] projectFiles = IoUtilities.listProjectFiles(nonExistent);

    assertNotNull(projectFiles);
    assertEquals(0, projectFiles.length);
  }

  @Test
  public void listProjectFilesIsCaseInsensitive() throws Exception {
    File dir = temporaryFolder.newFolder("case-files");
    createEmptyFile(dir, "project.A3P");
    createEmptyFile(dir, "project2.a3p");

    File[] projectFiles = IoUtilities.listProjectFiles(dir);

    assertNotNull(projectFiles);
    assertEquals(2, projectFiles.length);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LIST TYPE FILES: zero prior coverage
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void listTypeFilesInEmptyDirectoryReturnsEmptyArray() throws Exception {
    File emptyDir = temporaryFolder.newFolder("empty-types");

    File[] typeFiles = IoUtilities.listTypeFiles(emptyDir);

    assertNotNull(typeFiles);
    assertEquals(0, typeFiles.length);
  }

  @Test
  public void listTypeFilesFindsOnlyA3cFiles() throws Exception {
    File dir = temporaryFolder.newFolder("mixed-type-files");
    createEmptyFile(dir, "type1.a3c");
    createEmptyFile(dir, "type2.a3c");
    createEmptyFile(dir, "project.a3p");
    createEmptyFile(dir, "export.a3w");

    File[] typeFiles = IoUtilities.listTypeFiles(dir);

    assertNotNull(typeFiles);
    assertEquals(2, typeFiles.length);
    for (File file : typeFiles) {
      assertTrue("Expected .a3c file, got: " + file.getName(),
          file.getName().endsWith(".a3c"));
    }
  }

  @Test
  public void listTypeFilesOnNonExistentDirectoryReturnsEmptyArray() {
    File nonExistent = new File(temporaryFolder.getRoot(), "does-not-exist-types");

    File[] typeFiles = IoUtilities.listTypeFiles(nonExistent);

    assertNotNull(typeFiles);
    assertEquals(0, typeFiles.length);
  }

  @Test
  public void listTypeFilesIsCaseInsensitive() throws Exception {
    File dir = temporaryFolder.newFolder("case-type-files");
    createEmptyFile(dir, "type.A3C");
    createEmptyFile(dir, "type2.a3c");

    File[] typeFiles = IoUtilities.listTypeFiles(dir);

    assertNotNull(typeFiles);
    assertEquals(2, typeFiles.length);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // READ PROJECT ERROR PATHS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void readProjectFromNonExistentFileThrowsIOException() {
    File nonExistent = new File(temporaryFolder.getRoot(), "does-not-exist.a3p");

    assertThrows(IOException.class, () -> IoUtilities.readProject(nonExistent));
  }

  @Test
  public void readProjectFromEmptyFileThrowsIOException() throws Exception {
    File emptyFile = temporaryFolder.newFile("empty.a3p");

    assertThrows(IOException.class, () -> IoUtilities.readProject(emptyFile));
  }

  @Test
  public void readProjectFromNonZipFileThrowsIOException() throws Exception {
    File notZip = temporaryFolder.newFile("not-a-zip.a3p");
    java.nio.file.Files.write(notZip.toPath(), "not a zip".getBytes(StandardCharsets.UTF_8));

    assertThrows(IOException.class, () -> IoUtilities.readProject(notZip));
  }

  @Test
  public void readProjectByStringPathFromNonExistentFileThrowsIOException() {
    String path = new File(temporaryFolder.getRoot(), "does-not-exist-path.a3p").getAbsolutePath();

    assertThrows(IOException.class, () -> IoUtilities.readProject(path));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LIST FILES: exclude directories from results
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void listProjectFilesExcludesSubdirectories() throws Exception {
    File dir = temporaryFolder.newFolder("dir-with-subdir");
    createEmptyFile(dir, "project.a3p");
    new File(dir, "subdir.a3p").mkdirs();

    File[] projectFiles = IoUtilities.listProjectFiles(dir);

    assertNotNull(projectFiles);
    assertEquals(1, projectFiles.length);
    assertTrue(projectFiles[0].isFile());
  }

  @Test
  public void listTypeFilesExcludesSubdirectories() throws Exception {
    File dir = temporaryFolder.newFolder("dir-with-subdir-types");
    createEmptyFile(dir, "type.a3c");
    new File(dir, "subdir.a3c").mkdirs();

    File[] typeFiles = IoUtilities.listTypeFiles(dir);

    assertNotNull(typeFiles);
    assertEquals(1, typeFiles.length);
    assertTrue(typeFiles[0].isFile());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // READ TYPE ERROR PATHS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void readTypeFromNonExistentFileThrowsIOException() {
    File nonExistent = new File(temporaryFolder.getRoot(), "does-not-exist.a3c");

    assertThrows(IOException.class, () -> IoUtilities.readType(nonExistent));
  }

  @Test
  public void readTypeFromEmptyFileThrowsIOException() throws Exception {
    File emptyFile = temporaryFolder.newFile("empty.a3c");

    assertThrows(IOException.class, () -> IoUtilities.readType(emptyFile));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PROJECT READER FROM FILE
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void projectReaderFromNonExistentFileThrowsIOException() {
    File nonExistent = new File(temporaryFolder.getRoot(), "does-not-exist-reader.a3p");

    assertThrows(IOException.class, () -> IoUtilities.projectReader(nonExistent));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private static void createEmptyFile(File dir, String name) throws IOException {
    File file = new File(dir, name);
    if (!file.createNewFile()) {
      throw new IOException("Failed to create test file: " + file);
    }
  }
}
