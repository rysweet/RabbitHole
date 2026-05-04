package org.alice.netbeans.project;
import org.apache.tools.ant.launch.Launcher;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Alice3ProjectTemplateAntSmokeTest {
  private static final Path TARGET = Path.of("target");
  private static final String MODULE_EXTENSION_ROOT = "nbinst:/modules/ext/org.alice.netbeans/";
  private static final Set<String> OPTIONAL_LIBRARY_ARTIFACTS = Set.of("models-nonfree", "story-api-nonfree");
  private static final Map<String, Path> MODULE_OUTPUTS = moduleOutputs();

  @Test
  public void packagedProjectTemplateBuildsAndRunsGeneratedAliceProjectWithAlice3LibraryClasspath() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);

    unzip(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip"), projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    Path aliceProject = smokeRoot.resolve("synthetic-ant-smoke.a3p");
    IoUtilities.writeProject(
        aliceProject.toFile(),
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    generateProjectCodeWithoutFormatting(aliceProject, sourceDirectory);
    writeAntRunProbe(sourceDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antLog = executeAntJarTarget(projectDirectory, userProperties, antScratch);
    String antRunLog = executeAntRunTarget(projectDirectory, userProperties, antScratch);

    Path jarPath = projectDirectory.resolve("dist/Alice3JavaApplication.jar");
    assertTrue(antLog, Files.exists(projectDirectory.resolve("build/classes/Program.class")));
    assertTrue(antLog, Files.exists(projectDirectory.resolve("build/classes/AliceJavaFXLauncher.class")));
    assertTrue(antRunLog, Files.exists(projectDirectory.resolve("build/classes/AntRunProbe.class")));
    assertTrue(antRunLog, antRunLog.contains("ANT_RUN_PROBE_OK org.lgna.story.SProgram args=1"));
    assertTrue(antRunLog, !antRunLog.contains("Java Result:"));
    assertTrue(antLog, Files.exists(jarPath));
    assertJarContainsGeneratedProject(jarPath);
  }

  private static void generateProjectCodeWithoutFormatting(Path aliceProject, Path sourceDirectory) throws Exception {
    ProjectCodeGenerator.generateCode(
        aliceProject.toAbsolutePath().normalize().toFile(),
        sourceDirectory.toAbsolutePath().normalize().toFile(),
        null,
        false);
  }

  private static void writeAntRunProbe(Path sourceDirectory) throws Exception {
    Files.writeString(
        sourceDirectory.resolve("AntRunProbe.java"),
        """
        public class AntRunProbe {
            public static void main(String[] args) {
                if (!org.lgna.story.SProgram.class.equals(Program.class.getSuperclass())) {
                    throw new AssertionError(Program.class.getSuperclass().getName());
                }
                Program.main(args);
                System.out.println("ANT_RUN_PROBE_OK " + Program.class.getSuperclass().getName() + " args=" + args.length);
            }
        }
        """,
        StandardCharsets.UTF_8);
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    type.methods.add(mainMethod());
    return type;
  }

  private static UserMethod mainMethod() {
    UserParameter argsParameter = new UserParameter("args", String[].class);
    UserMethod mainMethod = new UserMethod(
        "main",
        Void.TYPE,
        new UserParameter[] {argsParameter},
        new BlockStatement());
    mainMethod.isStatic.setValue(true);
    mainMethod.isSignatureLocked.setValue(true);
    return mainMethod;
  }

  private static void assertJarContainsGeneratedProject(Path jarPath) throws Exception {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      assertTrue(jarPath.toString(), jarFile.getEntry("Program.class") != null);
      assertTrue(jarPath.toString(), jarFile.getEntry("AliceJavaFXLauncher.class") != null);
      Manifest manifest = jarFile.getManifest();
      assertTrue("Jar manifest should be present", manifest != null);
      assertEquals("AliceJavaFXLauncher", manifest.getMainAttributes().getValue("Main-Class"));
    }
  }

  private static String executeAntJarTarget(Path projectDirectory, Path userProperties, Path antScratch) {
    return executeAntTarget(projectDirectory, userProperties, antScratch, "jar", "ant-jar.log", Map.of());
  }

  private static String executeAntRunTarget(Path projectDirectory, Path userProperties, Path antScratch) {
    // The default AliceJavaFXLauncher needs a display; this headless probe proves the NetBeans run target,
    // generated Program class, and populated Alice3Library classpath up to that GUI boundary.
    return executeAntTarget(
        projectDirectory,
        userProperties,
        antScratch,
        "run",
        "ant-run.log",
        Map.of("main.class", "AntRunProbe", "application.args", "from-ant-run"));
  }

  private static String executeAntTarget(
      Path projectDirectory,
      Path userProperties,
      Path antScratch,
      String targetName,
      String logFileName,
      Map<String, String> antProperties) {
    Path buildFile = projectDirectory.resolve("build.xml").toAbsolutePath().normalize();
    Path outputFile = antScratch.resolve(logFileName);
    try {
      List<String> command = new ArrayList<>(List.of(
          Path.of(System.getProperty("java.home"), "bin", "java").toString(),
          "-Djava.io.tmpdir=" + antScratch.toAbsolutePath().normalize(),
          "-cp",
          antRuntimeClasspath(),
          Launcher.class.getName(),
          "-f",
          buildFile.toString(),
          "-Duser.properties.file=" + userProperties.toAbsolutePath().normalize()));
      antProperties.forEach((name, value) -> command.add("-D" + name + "=" + value));
      command.add(targetName);

      Process process = new ProcessBuilder(command)
          .directory(projectDirectory.toFile())
          .redirectErrorStream(true)
          .redirectOutput(outputFile.toFile())
          .start();
      boolean exited = process.waitFor(60, TimeUnit.SECONDS);
      if (!exited) {
        process.destroyForcibly();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
          throw new AssertionError("Ant smoke did not terminate after timeout");
        }
      }
      String output = Files.readString(outputFile, StandardCharsets.UTF_8);
      if (!exited) {
        throw new AssertionError("Ant smoke timed out running " + targetName + "\n" + output);
      }
      assertTrue(output, process.exitValue() == 0);
      return output;
    } catch (Exception ex) {
      throw new AssertionError("Unable to execute Ant smoke target " + targetName, ex);
    }
  }

  private static String antRuntimeClasspath() throws URISyntaxException {
    return String.join(
        File.pathSeparator,
        Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
        Path.of(org.apache.tools.ant.Project.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
  }

  private static void writeLibraryProperties(Path userProperties, Path antScratch) throws Exception {
    Files.createDirectories(userProperties.getParent());
    Files.createFile(antScratch.resolve("aliceSource.jar"));

    Properties properties = new Properties();
    properties.setProperty("libs.Alice3Library.classpath", aliceLibraryClasspath());
    properties.setProperty(
        "libs.Alice3Library.src",
        antScratch.resolve("aliceSource.jar").toAbsolutePath().normalize().toString());
    try (var writer = Files.newBufferedWriter(userProperties, StandardCharsets.UTF_8)) {
      properties.store(writer, "Alice3 Ant smoke test library bindings");
    }
  }

  private static String aliceLibraryClasspath() throws Exception {
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
    return String.join(File.pathSeparator, entries);
  }

  private static Optional<Path> resolveArtifact(String artifactId) {
    Path moduleOutput = MODULE_OUTPUTS.get(artifactId);
    if ((moduleOutput != null) && Files.exists(moduleOutput)) {
      return Optional.of(moduleOutput);
    }
    List<Path> matchingJars = testClasspathEntries().stream()
        .filter(path -> isJarForArtifact(path, artifactId))
        .toList();
    Optional<Path> jarWithClasses = matchingJars.stream()
        .filter(Alice3ProjectTemplateAntSmokeTest::containsClassEntry)
        .findFirst()
        .or(() -> matchingJars.stream().findFirst());
    return jarWithClasses;
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

  private static List<Path> testClasspathEntries() {
    String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path", ""));
    return List.of(classpath.split(File.pathSeparator)).stream()
        .filter(entry -> !entry.isBlank())
        .map(Path::of)
        .toList();
  }

  private static List<String> classpathResources() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    var builder = factory.newDocumentBuilder();
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    Document document = builder.parse(TARGET.resolve("classes/org/alice/netbeans/Alice3Library.xml").toFile());
    NodeList volumes = document.getElementsByTagName("volume");
    return IntStream.range(0, volumes.getLength())
        .mapToObj(index -> (Element) volumes.item(index))
        .filter(volume -> "classpath".equals(volume.getElementsByTagName("type").item(0).getTextContent()))
        .flatMap(volume -> elements(volume.getElementsByTagName("resource")).stream())
        .map(Element::getTextContent)
        .filter(resource -> resource.startsWith(MODULE_EXTENSION_ROOT))
        .toList();
  }

  private static List<Element> elements(NodeList nodes) {
    return IntStream.range(0, nodes.getLength())
        .mapToObj(index -> (Element) nodes.item(index))
        .toList();
  }

  private static void unzip(Path archive, Path destination) throws Exception {
    Files.createDirectories(destination);
    try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        Path output = destination.resolve(entry.getName()).normalize();
        assertTrue("Zip entry escapes destination: " + entry.getName(), output.startsWith(destination));
        if (entry.isDirectory()) {
          Files.createDirectories(output);
        } else {
          Files.createDirectories(output.getParent());
          Files.copy(input, output);
        }
      }
    }
  }

  private static void deleteRecursively(Path path) throws Exception {
    if (!Files.exists(path)) {
      return;
    }
    try (var paths = Files.walk(path)) {
      for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(child);
      }
    }
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
