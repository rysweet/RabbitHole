package org.lgna.story.resourceutilities;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
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

  // ── add (JAR methods) ────────────────────────────────────────────

  @Test
  public void addSingleFileToJar() throws Exception {
    Path workDir = newTestWorkDir("jar-single-file");
    Path sourceFile = workDir.resolve("hello.txt");
    Files.writeString(sourceFile, "hello world", StandardCharsets.UTF_8);
    Path jarPath = workDir.resolve("test.jar");

    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
      ModelResourceFileUtilities.add(sourceFile.toFile(), jos,
          sourceFile.getParent().toAbsolutePath().toString().replace("\\", "/") + "/",
          "prefix/", false);
    }

    Set<String> entryNames = jarEntryNames(jarPath);
    assertTrue("JAR should contain prefixed file entry",
        entryNames.contains("prefix/hello.txt"));
  }

  @Test
  public void addDirectoryToJarRecursivelyIncludesSubdirectories() throws Exception {
    Path workDir = newTestWorkDir("jar-recursive");
    Path sourceDir = workDir.resolve("src");
    Files.createDirectories(sourceDir.resolve("sub"));
    Files.writeString(sourceDir.resolve("top.txt"), "top", StandardCharsets.UTF_8);
    Files.writeString(sourceDir.resolve("sub/nested.txt"), "nested", StandardCharsets.UTF_8);
    Path jarPath = workDir.resolve("test.jar");

    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
      ModelResourceFileUtilities.add(sourceDir.toFile(), jos, "dest/", true);
    }

    Set<String> entryNames = jarEntryNames(jarPath);
    assertTrue("Should contain top-level file", entryNames.contains("dest/top.txt"));
    assertTrue("Should contain nested file", entryNames.contains("dest/sub/nested.txt"));
  }

  @Test
  public void addDirectoryNonRecursiveSkipsSubdirectories() throws Exception {
    Path workDir = newTestWorkDir("jar-non-recursive");
    Path sourceDir = workDir.resolve("src");
    Files.createDirectories(sourceDir.resolve("sub"));
    Files.writeString(sourceDir.resolve("top.txt"), "top", StandardCharsets.UTF_8);
    Files.writeString(sourceDir.resolve("sub/nested.txt"), "nested", StandardCharsets.UTF_8);
    Path jarPath = workDir.resolve("test.jar");

    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
      ModelResourceFileUtilities.add(sourceDir.toFile(), jos, "out/", false);
    }

    Set<String> entryNames = jarEntryNames(jarPath);
    assertTrue("Should contain top-level file", entryNames.contains("out/top.txt"));
    for (String name : entryNames) {
      assertTrue("Nested files should not be present: " + name,
          !name.contains("nested"));
    }
  }

  @Test
  public void addHandlesNullDestPathPrefix() throws Exception {
    Path workDir = newTestWorkDir("jar-null-prefix");
    Path sourceFile = workDir.resolve("data.txt");
    Files.writeString(sourceFile, "data", StandardCharsets.UTF_8);
    Path jarPath = workDir.resolve("test.jar");

    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
      ModelResourceFileUtilities.add(sourceFile.toFile(), jos,
          sourceFile.getParent().toAbsolutePath().toString().replace("\\", "/") + "/",
          "", false);
    }

    Set<String> entryNames = jarEntryNames(jarPath);
    assertTrue("Should contain file without prefix", entryNames.contains("data.txt"));
  }

  @Test
  public void addFourArgOverloadSetsRootFromSource() throws Exception {
    Path workDir = newTestWorkDir("jar-four-arg");
    Path sourceDir = workDir.resolve("content");
    Files.createDirectories(sourceDir);
    Files.writeString(sourceDir.resolve("file.txt"), "content", StandardCharsets.UTF_8);
    Path jarPath = workDir.resolve("test.jar");

    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
      ModelResourceFileUtilities.add(sourceDir.toFile(), jos, "pkg/", true);
    }

    Set<String> entryNames = jarEntryNames(jarPath);
    assertTrue("Should contain file under prefix", entryNames.contains("pkg/file.txt"));
  }

  // ── getJavaCodeDir ────────────────────────────────────────────────

  @Test
  public void getJavaCodeDirReturnsPackageDirectory() {
    File result = ModelResourceFileUtilities.getJavaCodeDir("/root/", "org.lgna.story.resources.prop");

    String expected = "/root/" + "org" + File.separator + "lgna" + File.separator
        + "story" + File.separator + "resources" + File.separator + "prop" + File.separator;
    assertEquals(new File(expected), result);
  }

  @Test
  public void getJavaCodeDirWithSimplePackage() {
    File result = ModelResourceFileUtilities.getJavaCodeDir("/out/", "com.example");

    String expected = "/out/" + "com" + File.separator + "example" + File.separator;
    assertEquals(new File(expected), result);
  }

  // ── getJavaClassFile ──────────────────────────────────────────────

  @Test
  public void getJavaClassFileReturnsCorrectPath() {
    File result = ModelResourceFileUtilities.getJavaClassFile(
        "/root/", "org.lgna.story.resources.prop", "TestPropResource");

    String expected = "/root/" + "org" + File.separator + "lgna" + File.separator
        + "story" + File.separator + "resources" + File.separator + "prop" + File.separator
        + "TestPropResource.class";
    assertEquals(new File(expected), result);
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

  @Test
  public void getJavaFileAndClassFileOnlyDifferByExtension() {
    String root = "/output/";
    String pkg = "org.lgna.story.resources.prop";
    String className = "MyResource";

    File javaFile = ModelResourceFileUtilities.getJavaFile(root, pkg, className);
    File classFile = ModelResourceFileUtilities.getJavaClassFile(root, pkg, className);

    assertEquals(javaFile.getParent(), classFile.getParent());
    assertEquals(javaFile.getName().replace(".java", ""),
        classFile.getName().replace(".class", ""));
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

  // ── path method contract: file methods share same package directory ────

  @Test
  public void allFileMethodsShareSamePackageDirectory() {
    String root = "/base/";
    String pkg = "org.lgna.story.resources.prop";
    String className = "TestProp";
    String javaClassName = "TestPropResource";

    File codeDir = ModelResourceFileUtilities.getJavaCodeDir(root, pkg);
    File javaFile = ModelResourceFileUtilities.getJavaFile(root, pkg, javaClassName);
    File classFile = ModelResourceFileUtilities.getJavaClassFile(root, pkg, javaClassName);

    assertEquals("Java file should be in code directory", codeDir.getPath(), javaFile.getParentFile().getPath());
    assertEquals("Class file should be in code directory", codeDir.getPath(), classFile.getParentFile().getPath());
  }

  // ── helpers ───────────────────────────────────────────────────────

  private static Set<String> jarEntryNames(Path jarPath) throws IOException {
    Set<String> names = new HashSet<>();
    try (JarFile jar = new JarFile(jarPath.toFile())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        names.add(entries.nextElement().getName());
      }
    }
    return names;
  }

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
