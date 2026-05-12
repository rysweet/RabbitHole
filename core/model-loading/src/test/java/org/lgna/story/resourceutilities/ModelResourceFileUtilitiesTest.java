package org.lgna.story.resourceutilities;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the extracted {@link ModelResourceFileUtilities} helper class.
 * Same package gives access to the package-private API.
 */
public class ModelResourceFileUtilitiesTest {

  // ── ensureOutputFile ──────────────────────────────────────────────

  @Test
  public void ensureOutputFileCreatesParentDirectoriesAndFile() throws Exception {
    Path dir = newTestWorkDir("ensure-creates");
    File outputFile = dir.resolve("a/b/c/output.xml").toFile();

    ModelResourceFileUtilities.ensureOutputFile(outputFile, "test");

    assertTrue(outputFile.isFile());
  }

  @Test
  public void ensureOutputFileSucceedsWhenFileAlreadyExists() throws Exception {
    Path dir = newTestWorkDir("ensure-exists");
    File outputFile = dir.resolve("existing.txt").toFile();
    Files.writeString(outputFile.toPath(), "content", StandardCharsets.UTF_8);

    ModelResourceFileUtilities.ensureOutputFile(outputFile, "pre-existing");

    assertTrue(outputFile.isFile());
  }

  @Test
  public void ensureOutputFileThrowsWhenPathIsDirectory() throws Exception {
    Path dir = newTestWorkDir("ensure-dir-conflict");
    File dirAsFile = dir.resolve("looks-like-a-file").toFile();
    Files.createDirectories(dirAsFile.toPath());

    IOException error = assertThrows(IOException.class,
        () -> ModelResourceFileUtilities.ensureOutputFile(dirAsFile, "XML resource"));

    assertTrue(error.getMessage().contains("XML resource"));
    assertTrue(error.getMessage().contains("not a file"));
  }

  @Test
  public void ensureOutputFileIncludesDescriptionInErrorMessage() throws Exception {
    Path dir = newTestWorkDir("ensure-description");
    File dirAsFile = dir.resolve("collision").toFile();
    Files.createDirectories(dirAsFile.toPath());

    IOException error = assertThrows(IOException.class,
        () -> ModelResourceFileUtilities.ensureOutputFile(dirAsFile, "custom-desc"));

    assertTrue(error.getMessage().contains("custom-desc"));
  }

  // ── getJavaFile ───────────────────────────────────────────────────

  @Test
  public void getJavaFileReturnsCorrectPath() {
    File result = ModelResourceFileUtilities.getJavaFile(
        "/root/", "org.lgna.story.resources.prop", "TestPropResource");

    String expected = "/root/" + "org" + File.separator + "lgna" + File.separator
        + "story" + File.separator + "resources" + File.separator + "prop" + File.separator
        + "TestPropResource.java";
    assertEquals(new File(expected), result);
  }

  // ── getXMLFile ────────────────────────────────────────────────────

  @Test
  public void getXMLFileReturnsCorrectPathForPropClass() {
    File result = ModelResourceFileUtilities.getXMLFile(
        "/root/", "org.lgna.story.resources.prop", "TestProp");

    assertTrue("XML path should end with TestProp.xml", result.getName().equals("TestProp.xml"));
    String path = result.getPath().replace("\\", "/");
    assertTrue("XML path should include package directory",
        path.contains("org/lgna/story/resources/prop") || path.contains("org" + File.separator));
  }

  @Test
  public void getXMLFileAppendsTrailingSeparatorToRootWhenMissing() {
    File withSlash = ModelResourceFileUtilities.getXMLFile(
        "/root/", "org.lgna.story.resources.prop", "TestProp");
    File withoutSlash = ModelResourceFileUtilities.getXMLFile(
        "/root", "org.lgna.story.resources.prop", "TestProp");

    assertEquals("Should produce same file regardless of trailing separator",
        withSlash.getPath(), withoutSlash.getPath());
  }

  @Test
  public void getXMLFileIncludesResourceSubDirectory() {
    File result = ModelResourceFileUtilities.getXMLFile(
        "/root/", "org.lgna.story.resources.prop", "TestProp");

    String path = result.getPath().replace("\\", "/");
    // The path should contain the package directory
    assertTrue("Should contain package path",
        path.contains("org/lgna/story/resources/prop") || path.contains("org" + File.separator));
  }

  // ── helpers ───────────────────────────────────────────────────────

  private static Path newTestWorkDir(String name) throws IOException {
    Path workRoot = Path.of("target", "test-work",
        ModelResourceFileUtilitiesTest.class.getSimpleName(), name).toAbsolutePath();
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
