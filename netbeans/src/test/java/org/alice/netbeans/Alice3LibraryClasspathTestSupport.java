package org.alice.netbeans;

import org.apache.tools.ant.launch.Launcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertTrue;

public final class Alice3LibraryClasspathTestSupport {
  private static final Path TARGET = Path.of("target");
  private static final String MODULE_EXTENSION_ROOT = "nbinst:/modules/ext/org.alice.netbeans/";
  private static final Set<String> OPTIONAL_LIBRARY_ARTIFACTS = Set.of("models-nonfree", "story-api-nonfree");
  private static final Map<String, Path> MODULE_OUTPUTS = moduleOutputs();
  private static String cachedAliceLibraryClasspath;
  private static String cachedAntRuntimeClasspath;
  private static List<Path> cachedTestClasspathEntries;
  private static List<String> cachedClasspathResources;

  private Alice3LibraryClasspathTestSupport() {
  }

  public static synchronized String aliceLibraryClasspath() throws Exception {
    if (cachedAliceLibraryClasspath == null) {
      cachedAliceLibraryClasspath = buildAliceLibraryClasspath();
    }
    return cachedAliceLibraryClasspath;
  }

  private static String buildAliceLibraryClasspath() throws Exception {
    List<String> missing = new ArrayList<>();
    List<String> entries = new ArrayList<>();

    for (String resource : classpathResources()) {
      String artifactId = resource.substring(resource.lastIndexOf('/') + 1, resource.length() - ".jar".length());
      Optional<Path> entry = resolveArtifact(artifactId);
      if (entry.isPresent()) {
        entries.add(entry.get().toAbsolutePath().normalize().toString());
      } else if (!OPTIONAL_LIBRARY_ARTIFACTS.contains(artifactId)) {
        missing.add(artifactId);
      }
    }

    assertTrue("Missing Alice3Library classpath artifacts: " + missing, missing.isEmpty());
    assertTrue(
        "Alice3Library smoke classpath should include story-api",
        entries.stream().anyMatch(entry -> entry.contains("story-api")));
    assertTrue(
        "Alice3Library smoke classpath should include JavaFX graphics",
        entries.stream().anyMatch(entry -> entry.contains("javafx-graphics")));
    return String.join(File.pathSeparator, entries);
  }

  public static void writeLibraryProperties(Path userProperties, Path antScratch) throws Exception {
    Files.createDirectories(userProperties.getParent());
    Path aliceSource = antScratch.resolve("aliceSource.jar");
    if (!Files.exists(aliceSource)) {
      Files.createFile(aliceSource);
    }

    Properties properties = new Properties();
    properties.setProperty("libs.Alice3Library.classpath", aliceLibraryClasspath());
    properties.setProperty("libs.Alice3Library.src", aliceSource.toAbsolutePath().normalize().toString());
    try (var writer = Files.newBufferedWriter(userProperties, StandardCharsets.UTF_8)) {
      properties.store(writer, "Alice3 Ant smoke test library bindings");
    }
  }

  public static synchronized String antRuntimeClasspath() throws URISyntaxException {
    if (cachedAntRuntimeClasspath == null) {
      cachedAntRuntimeClasspath = String.join(
          File.pathSeparator,
          Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
          Path.of(org.apache.tools.ant.Project.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
    }
    return cachedAntRuntimeClasspath;
  }

  private static Optional<Path> resolveArtifact(String artifactId) {
    Path moduleOutput = MODULE_OUTPUTS.get(artifactId);
    if ((moduleOutput != null) && Files.exists(moduleOutput)) {
      return Optional.of(moduleOutput);
    }
    List<Path> matchingJars = testClasspathEntries().stream()
        .filter(path -> isJarForArtifact(path, artifactId))
        .toList();
    return matchingJars.stream()
        .filter(Alice3LibraryClasspathTestSupport::containsClassEntry)
        .findFirst()
        .or(() -> matchingJars.stream().findFirst());
  }

  private static boolean containsClassEntry(Path jarPath) {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      return jarFile.stream().anyMatch(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"));
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean isJarForArtifact(Path path, String artifactId) {
    String fileName = path.getFileName().toString();
    return fileName.equals(artifactId + ".jar")
        || (fileName.startsWith(artifactId + "-") && fileName.endsWith(".jar"));
  }

  private static synchronized List<Path> testClasspathEntries() {
    if (cachedTestClasspathEntries != null) {
      return cachedTestClasspathEntries;
    }
    String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path", ""));
    cachedTestClasspathEntries = List.of(classpath.split(File.pathSeparator)).stream()
        .filter(entry -> !entry.isBlank())
        .map(Path::of)
        .toList();
    return cachedTestClasspathEntries;
  }

  private static synchronized List<String> classpathResources() throws Exception {
    if (cachedClasspathResources != null) {
      return cachedClasspathResources;
    }
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    var builder = factory.newDocumentBuilder();
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    Document document = builder.parse(TARGET.resolve("classes/org/alice/netbeans/Alice3Library.xml").toFile());
    NodeList volumes = document.getElementsByTagName("volume");
    List<String> resources = IntStream.range(0, volumes.getLength())
        .mapToObj(index -> (Element) volumes.item(index))
        .filter(volume -> "classpath".equals(volume.getElementsByTagName("type").item(0).getTextContent()))
        .flatMap(volume -> elements(volume.getElementsByTagName("resource")).stream())
        .map(Element::getTextContent)
        .filter(resource -> resource.startsWith(MODULE_EXTENSION_ROOT))
        .toList();
    cachedClasspathResources = resources;
    return cachedClasspathResources;
  }

  private static List<Element> elements(NodeList nodes) {
    return IntStream.range(0, nodes.getLength())
        .mapToObj(index -> (Element) nodes.item(index))
        .toList();
  }

  private static Map<String, Path> moduleOutputs() {
    Map<String, Path> outputs = new LinkedHashMap<>();
    outputs.put("util", Path.of("../core/util/target/classes"));
    outputs.put("scenegraph", Path.of("../core/scenegraph/target/classes"));
    outputs.put("glrender", Path.of("../core/glrender/target/classes"));
    outputs.put("ast", Path.of("../core/ast/target/classes"));
    outputs.put("story-api", Path.of("../core/story-api/target/classes"));
    outputs.put("tweedle", Path.of("../core/tweedle/target/classes"));
    outputs.put("models", Path.of("../core/models/target/classes"));
    outputs.put("models-nonfree", Path.of("../core-nonfree/models/target/classes"));
    outputs.put("story-api-nonfree", Path.of("../core-nonfree/story-api/target/classes"));
    return outputs.entrySet().stream()
        .collect(
            LinkedHashMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue().toAbsolutePath().normalize()),
            Map::putAll);
  }
}
