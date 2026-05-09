package org.alice.netbeans.project;
import org.alice.netbeans.Alice3LibraryClasspathTestSupport;
import org.alice.netbeans.Alice3ProjectTemplateWizardIterator;
import org.apache.tools.ant.launch.Launcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectManagerImplementation;
import org.lgna.common.resources.AudioResource;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Alice3ProjectTemplateAntSmokeTest {
  private static final Path TARGET = Path.of("target");

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

  @Test
  public void exportedProjectJarTargetReportsCommandLineClasspath() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-jar-command-line-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);

    unzip(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip"), projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    Path aliceProject = smokeRoot.resolve("jar-command-line-world.a3p");
    IoUtilities.writeProject(
        aliceProject.toFile(),
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    generateProjectCodeWithoutFormatting(aliceProject, sourceDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antLog = executeAntJarTarget(projectDirectory, userProperties, antScratch);

    Path jarPath = projectDirectory.resolve("dist/Alice3JavaApplication.jar");
    assertTrue(antLog, Files.exists(jarPath));
    assertJarContainsGeneratedProject(jarPath);
    assertAntJarLogReportsCommandLineClasspath(antLog, jarPath);
  }

  @Test
  public void exportedResourceProjectAntJarPackagesGeneratedResourcesAndRunTargetLoadsThem() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-resource-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);
    Files.createDirectories(smokeRoot);

    byte[] resourceData = "alice ant resource\n".getBytes(StandardCharsets.UTF_8);
    Path audioFile = smokeRoot.resolve("probe.wav");
    Files.write(audioFile, resourceData);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new AudioResource(audioFile.toFile(), "audio.x_wav"));
    Path aliceProject = smokeRoot.resolve("resource-world.a3p");
    IoUtilities.writeProject(aliceProject.toFile(), project);

    unzip(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip"), projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    generateProjectCodeWithoutFormatting(aliceProject, sourceDirectory);
    assertTrue(Files.exists(sourceDirectory.resolve("Resources.java")));
    assertArrayEquals(resourceData, Files.readAllBytes(sourceDirectory.resolve("resources").resolve("probe.wav")));
    writeAntResourceProbe(sourceDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antLog = executeAntJarTarget(projectDirectory, userProperties, antScratch);
    String antRunLog = executeAntTarget(
        projectDirectory,
        userProperties,
        antScratch,
        "run",
        "ant-resource-run.log",
        Map.of("main.class", "AntResourceProbe"));

    Path jarPath = projectDirectory.resolve("dist/Alice3JavaApplication.jar");
    assertTrue(antLog, Files.exists(jarPath));
    assertJarContainsEntry(jarPath, "Resources.class");
    assertJarContainsEntry(jarPath, "AntResourceProbe.class");
    assertJarEntryBytes(jarPath, "resources/probe.wav", resourceData);
    assertTrue(antRunLog, antRunLog.contains("ANT_RESOURCE_PROBE_OK audio.x_wav alice ant resource"));
    assertTrue(antRunLog, !antRunLog.contains("Java Result:"));
  }

  @Test
  public void exportedProjectAntRunTargetAppliesRuntimeJvmArgumentsUpToGuiBoundary() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-runtime-configuration-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);

    unzip(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip"), projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    Path aliceProject = smokeRoot.resolve("runtime-configuration-world.a3p");
    IoUtilities.writeProject(
        aliceProject.toFile(),
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    generateProjectCodeWithoutFormatting(aliceProject, sourceDirectory);
    writeAntRuntimeConfigurationProbe(sourceDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antRunLog = executeAntTarget(
        projectDirectory,
        userProperties,
        antScratch,
        "run",
        "ant-runtime-configuration-run.log",
        Map.of("main.class", "AntRuntimeConfigurationProbe"));

    assertTrue(antRunLog, Files.exists(projectDirectory.resolve("build/classes/AntRuntimeConfigurationProbe.class")));
    assertTrue(antRunLog, antRunLog.contains("ANT_RUNTIME_CONFIGURATION_PROBE_OK "));
    assertTrue(antRunLog, antRunLog.contains("aliceSource.jar_root"));
    assertTrue(antRunLog, !antRunLog.contains("Java Result:"));
  }

  @Test
  public void exportedProjectRunTestWithMainTargetCompilesAndRunsTestMainWithAlice3LibraryClasspath() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-run-test-main-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);

    unzip(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip"), projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    Path aliceProject = smokeRoot.resolve("test-main-world.a3p");
    IoUtilities.writeProject(
        aliceProject.toFile(),
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    generateProjectCodeWithoutFormatting(aliceProject, sourceDirectory);

    Path testDirectory = projectDirectory.resolve("test");
    writeAntTestMainProbe(testDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antRunLog = executeAntTarget(
        projectDirectory,
        userProperties,
        antScratch,
        "run-test-with-main",
        "ant-run-test-with-main.log",
        Map.of(
            "run.class", "AntTestMainProbe",
            "javac.includes", "AntTestMainProbe.java"));

    assertTrue(antRunLog, Files.exists(projectDirectory.resolve("build/classes/Program.class")));
    assertTrue(antRunLog, Files.exists(projectDirectory.resolve("build/test/classes/AntTestMainProbe.class")));
    assertTrue(antRunLog, antRunLog.contains("ANT_TEST_MAIN_PROBE_OK org.lgna.story.SProgram "));
    assertTrue(antRunLog, antRunLog.contains("aliceSource.jar_root"));
    assertTrue(antRunLog, !antRunLog.contains("Java Result:"));
  }

  @Test
  public void exportedProjectAntCleanTargetRemovesGeneratedBuildOutputs() throws Exception {
    Path smokeRoot = TARGET.resolve("ant-clean-smoke");
    Path projectDirectory = smokeRoot.resolve("project");
    deleteRecursively(smokeRoot);

    unzip(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip"), projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);
    Path aliceProject = smokeRoot.resolve("clean-world.a3p");
    IoUtilities.writeProject(
        aliceProject.toFile(),
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    generateProjectCodeWithoutFormatting(aliceProject, sourceDirectory);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antJarLog = executeAntJarTarget(projectDirectory, userProperties, antScratch);

    Path buildDirectory = projectDirectory.resolve("build");
    Path distDirectory = projectDirectory.resolve("dist");
    Path jarPath = distDirectory.resolve("Alice3JavaApplication.jar");
    assertTrue(antJarLog, Files.exists(buildDirectory.resolve("classes/Program.class")));
    assertTrue(antJarLog, Files.exists(jarPath));

    String antCleanLog = executeAntTarget(
        projectDirectory,
        userProperties,
        antScratch,
        "clean",
        "ant-clean.log",
        Map.of());

    assertTrue(antCleanLog, Files.notExists(buildDirectory));
    assertTrue(antCleanLog, Files.notExists(distDirectory));
    assertTrue(antCleanLog, Files.exists(sourceDirectory.resolve("Program.java")));
    assertTrue(antCleanLog, Files.exists(projectDirectory.resolve("build.xml")));
  }

  @Test
  public void exportsResourceBearingProjectWithAlice3LibraryAndPackagesResources() throws Exception {
    Path smokeRoot = temporaryFolder.newFolder("wizard-resource-smoke").toPath();
    Path projectDirectory = smokeRoot.resolve("ExportedResourceWorld");
    Files.createDirectories(smokeRoot);

    byte[] resourceData = "alice wizard resource\n".getBytes(StandardCharsets.UTF_8);
    Path audioFile = smokeRoot.resolve("probe.wav");
    Files.write(audioFile, resourceData);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new AudioResource(audioFile.toFile(), "audio.x_wav"));
    Path aliceProject = smokeRoot.resolve("resource-world.a3p");
    IoUtilities.writeProject(aliceProject.toFile(), project);

    instantiateAliceProjectThroughWizard(aliceProject, projectDirectory);

    Path sourceDirectory = projectDirectory.resolve("src");
    assertGeneratedProjectStructure(projectDirectory, sourceDirectory);
    assertProjectMetadataUsesAlice3Library(projectDirectory, "ExportedResourceWorld");
    assertResourceSourceAndBytes(sourceDirectory, smokeRoot, resourceData);

    Path antScratch = smokeRoot.resolve("ant-scratch");
    Files.createDirectories(antScratch);
    Path userProperties = smokeRoot.resolve("user.properties");
    writeLibraryProperties(userProperties, antScratch);

    String antLog = executeAntJarTarget(projectDirectory, userProperties, antScratch);

    Path jarPath = projectDirectory.resolve("dist/ExportedResourceWorld.jar");
    assertTrue(antLog, Files.exists(jarPath));
    assertJarContainsGeneratedProject(jarPath);
    assertJarContainsEntry(jarPath, "Resources.class");
    assertJarEntryBytes(jarPath, "resources/probe.wav", resourceData);
  }

  private static void generateProjectCodeWithoutFormatting(Path aliceProject, Path sourceDirectory) throws Exception {
    ProjectCodeGenerator.generateCode(
        aliceProject.toAbsolutePath().normalize().toFile(),
        sourceDirectory.toAbsolutePath().normalize().toFile(),
        null,
        false);
  }

  private static void instantiateAliceProjectThroughWizard(Path aliceProject, Path projectDirectory) throws Exception {
    Files.createDirectories(projectDirectory);
    File templateFile = FileUtil.normalizeFile(TARGET.resolve("classes/org/alice/netbeans/ProjectTemplate.zip").toAbsolutePath().toFile());
    File normalizedProjectDirectory = FileUtil.normalizeFile(projectDirectory.toFile());
    File normalizedAliceProject = FileUtil.normalizeFile(aliceProject.toFile());
    FileObject template = FileUtil.toFileObject(templateFile);
    assertNotNull("ProjectTemplate.zip must be available as a test resource", template);

    WizardDescriptor wizard = new WizardDescriptor(new WizardDescriptor.Panel[0]);
    wizard.putProperty("targetTemplate", template);
    wizard.putProperty("projdir", normalizedProjectDirectory);
    wizard.putProperty("aliceProjectFile", normalizedAliceProject);

    Alice3ProjectTemplateWizardIterator iterator = Alice3ProjectTemplateWizardIterator.createIterator();
    iterator.initialize(wizard);
    try {
      Set<FileObject> instantiated = instantiateWithNetBeansProjectLookup(iterator);
      assertTrue(instantiated.contains(FileUtil.toFileObject(normalizedProjectDirectory)));
      assertTrue(instantiated.contains(FileUtil.toFileObject(FileUtil.normalizeFile(projectDirectory.resolve("src").toFile()))));
    } finally {
      iterator.uninitialize(wizard);
    }
  }

  private static Set<FileObject> instantiateWithNetBeansProjectLookup(Alice3ProjectTemplateWizardIterator iterator) throws Exception {
    Lookup originalLookup = Lookup.getDefault();
    Lookup lookup = new ProxyLookup(
        Lookups.fixed(new TestProjectManagerImplementation()),
        originalLookup);
    List<Exception> failures = new ArrayList<>();
    List<Set<FileObject>> result = new ArrayList<>();
    Lookups.executeWith(lookup, () -> {
      try {
        result.add(iterator.instantiate(null));
      } catch (Exception ex) {
        failures.add(ex);
      }
    });
    if (!failures.isEmpty()) {
      throw failures.get(0);
    }
    return result.get(0);
  }

  private static void assertGeneratedProjectStructure(Path projectDirectory, Path sourceDirectory) {
    assertTrue(Files.exists(projectDirectory.resolve("build.xml")));
    assertTrue(Files.exists(projectDirectory.resolve("manifest.mf")));
    assertTrue(Files.exists(projectDirectory.resolve("nbproject/project.xml")));
    assertTrue(Files.exists(projectDirectory.resolve("nbproject/project.properties")));
    assertTrue(Files.exists(projectDirectory.resolve("nbproject/build-impl.xml")));
    assertTrue(Files.exists(sourceDirectory.resolve("Program.java")));
    assertTrue(Files.exists(sourceDirectory.resolve("AliceJavaFXLauncher.java")));
    assertTrue(Files.exists(sourceDirectory.resolve("Resources.java")));
    assertTrue(Files.exists(sourceDirectory.resolve("resources/probe.wav")));
  }

  private static void assertProjectMetadataUsesAlice3Library(Path projectDirectory, String projectName) throws Exception {
    Properties properties = loadProperties(projectDirectory.resolve("nbproject/project.properties"));
    assertEquals("src", properties.getProperty("src.dir"));
    assertEquals("build", properties.getProperty("build.dir"));
    assertEquals("${build.dir}/classes", properties.getProperty("build.classes.dir"));
    assertEquals("${libs.Alice3Library.classpath}", properties.getProperty("javac.classpath").trim());
    assertEquals("AliceJavaFXLauncher", properties.getProperty("main.class"));
    assertEquals(projectName, properties.getProperty("application.title").trim());
    assertEquals("${dist.dir}/" + projectName + ".jar", properties.getProperty("dist.jar").trim());
    String runClasspath = properties.getProperty("run.classpath");
    assertTrue(runClasspath, runClasspath.contains("${build.classes.dir}"));
    assertTrue(runClasspath, runClasspath.contains("${javac.classpath}"));
  }

  private static Properties loadProperties(Path propertiesPath) throws Exception {
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(propertiesPath, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static void assertResourceSourceAndBytes(Path sourceDirectory, Path smokeRoot, byte[] resourceData) throws Exception {
    Path resourcePath = sourceDirectory.resolve("resources/probe.wav");
    assertArrayEquals(resourceData, Files.readAllBytes(resourcePath));
    String resourcesSource = Files.readString(sourceDirectory.resolve("Resources.java"));
    assertTrue(resourcesSource, resourcesSource.contains("resources/probe.wav"));
    assertTrue(resourcesSource, resourcesSource.contains("audio.x_wav"));
    assertTrue(resourcesSource, !resourcesSource.contains(smokeRoot.toAbsolutePath().normalize().toString()));
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

  private static void writeAntResourceProbe(Path sourceDirectory) throws Exception {
    Files.writeString(
        sourceDirectory.resolve("AntResourceProbe.java"),
        """
        public class AntResourceProbe {
            public static void main(String[] args) {
                org.lgna.common.resources.AudioResource resource = Resources.probe_wav;
                String body = new String(resource.getData(), java.nio.charset.StandardCharsets.UTF_8);
                if (!"alice ant resource\\n".equals(body)) {
                    throw new AssertionError(body);
                }
                if (!"audio.x_wav".equals(resource.getContentType())) {
                    throw new AssertionError(resource.getContentType());
                }
                if (AntResourceProbe.class.getClassLoader().getResource("resources/probe.wav") == null) {
                    throw new AssertionError("resources/probe.wav missing from runtime classpath");
                }
                System.out.println("ANT_RESOURCE_PROBE_OK " + resource.getContentType() + " " + body.trim());
            }
        }
        """,
        StandardCharsets.UTF_8);
  }

  private static void writeAntRuntimeConfigurationProbe(Path sourceDirectory) throws Exception {
    Files.writeString(
        sourceDirectory.resolve("AntRuntimeConfigurationProbe.java"),
        """
        public class AntRuntimeConfigurationProbe {
            public static void main(String[] args) {
                boolean assertionsEnabled = false;
                assert assertionsEnabled = true;
                if (!assertionsEnabled) {
                    throw new AssertionError("Assertions were not enabled by run.jvmargs");
                }
                String aliceRootDirectory = System.getProperty("org.alice.ide.rootDirectory");
                if ((aliceRootDirectory == null) || aliceRootDirectory.isBlank()) {
                    throw new AssertionError("org.alice.ide.rootDirectory was not set");
                }
                if (aliceRootDirectory.contains("${")) {
                    throw new AssertionError("Unresolved Alice root directory: " + aliceRootDirectory);
                }
                String normalizedAliceRootDirectory = aliceRootDirectory.replace('\\\\', '/');
                if (!normalizedAliceRootDirectory.endsWith("aliceSource.jar_root")) {
                    throw new AssertionError("Unexpected Alice root directory: " + aliceRootDirectory);
                }
                System.out.println("ANT_RUNTIME_CONFIGURATION_PROBE_OK " + normalizedAliceRootDirectory);
            }
        }
        """,
        StandardCharsets.UTF_8);
  }

  private static void writeAntTestMainProbe(Path testDirectory) throws Exception {
    Files.createDirectories(testDirectory);
    Files.writeString(
        testDirectory.resolve("AntTestMainProbe.java"),
        """
        public class AntTestMainProbe {
            public static void main(String[] args) {
                if (!org.lgna.story.SProgram.class.equals(Program.class.getSuperclass())) {
                    throw new AssertionError(Program.class.getSuperclass().getName());
                }
                boolean assertionsEnabled = false;
                assert assertionsEnabled = true;
                if (!assertionsEnabled) {
                    throw new AssertionError("Assertions were not enabled by run.jvmargs");
                }
                String aliceRootDirectory = System.getProperty("org.alice.ide.rootDirectory");
                if ((aliceRootDirectory == null) || aliceRootDirectory.isBlank()) {
                    throw new AssertionError("org.alice.ide.rootDirectory was not set");
                }
                if (aliceRootDirectory.contains("${")) {
                    throw new AssertionError("Unresolved Alice root directory: " + aliceRootDirectory);
                }
                String normalizedAliceRootDirectory = aliceRootDirectory.replace('\\\\', '/');
                if (!normalizedAliceRootDirectory.endsWith("aliceSource.jar_root")) {
                    throw new AssertionError("Unexpected Alice root directory: " + aliceRootDirectory);
                }
                System.out.println("ANT_TEST_MAIN_PROBE_OK "
                    + Program.class.getSuperclass().getName()
                    + " "
                    + normalizedAliceRootDirectory);
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

  private static void assertAntJarLogReportsCommandLineClasspath(String antLog, Path jarPath) {
    String normalizedLog = antLog.replace(File.separatorChar, '/');
    String normalizedJarPath = jarPath.toAbsolutePath().normalize().toString().replace(File.separatorChar, '/');
    assertTrue(antLog, antLog.contains("To run this application from the command line without Ant, try:"));
    assertTrue(antLog, antLog.contains(" -cp "));
    assertTrue(normalizedLog, normalizedLog.contains(normalizedJarPath));
    assertTrue(antLog, antLog.contains("AliceJavaFXLauncher"));
    assertTrue(antLog, antLog.contains("story-api"));
    assertTrue(antLog, antLog.contains("javafx-graphics"));
  }

  private static void assertJarContainsEntry(Path jarPath, String entryName) throws Exception {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      assertTrue(jarPath + " should contain " + entryName, jarFile.getEntry(entryName) != null);
    }
  }

  private static void assertJarEntryBytes(Path jarPath, String entryName, byte[] expected) throws Exception {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      ZipEntry entry = jarFile.getEntry(entryName);
      assertNotNull(jarPath + " should contain " + entryName, entry);
      assertArrayEquals(expected, jarFile.getInputStream(entry).readAllBytes());
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

  static String executeAntTarget(
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
      System.out.println("----- BEGIN " + targetName + " Ant log: " + logFileName + " -----");
      System.out.print(output);
      if (!output.endsWith(System.lineSeparator())) {
        System.out.println();
      }
      System.out.println("----- END " + targetName + " Ant log: " + logFileName + " -----");
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
    return Alice3LibraryClasspathTestSupport.antRuntimeClasspath();
  }

  static void writeLibraryProperties(Path userProperties, Path antScratch) throws Exception {
    Alice3LibraryClasspathTestSupport.writeLibraryProperties(userProperties, antScratch);
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

  private static final class TestProjectManagerImplementation implements ProjectManagerImplementation {
    private final Mutex mutex = new Mutex();

    @Override
    public void init(ProjectManagerCallBack callback) {
    }

    @Override
    public Mutex getMutex() {
      return mutex;
    }

    @Override
    public Mutex getMutex(boolean autoSave, org.netbeans.api.project.Project project, org.netbeans.api.project.Project... otherProjects) {
      return mutex;
    }

    @Override
    public org.netbeans.api.project.Project findProject(FileObject projectDirectory) {
      return null;
    }

    @Override
    public ProjectManager.Result isProject(FileObject projectDirectory) {
      return null;
    }

    @Override
    public void clearNonProjectCache() {
    }

    @Override
    public Set<org.netbeans.api.project.Project> getModifiedProjects() {
      return Set.of();
    }

    @Override
    public boolean isModified(org.netbeans.api.project.Project project) {
      return false;
    }

    @Override
    public boolean isValid(org.netbeans.api.project.Project project) {
      return true;
    }

    @Override
    public void saveProject(org.netbeans.api.project.Project project) {
    }

    @Override
    public void saveAllProjects() {
    }
  }
}
