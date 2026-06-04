package org.alice.netbeans.project;

import org.alice.netbeans.Alice3LibraryClasspathTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.AstMethodLookupHelpers;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.TypeExpression;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.Assert.*;

public class ProjectCodeGeneratorStandaloneProjectTest {

  private static final String RENDER_OBSERVATION_JSON_PREFIX =
      "ALICE_LAUNCHER_RENDER_OBSERVATION "
          + "{\"schema_version\":\"alice.launcher.render-observation/v1\",";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedStandaloneProjectCompilesAndLaunchesWithJavaFxStubs() throws Exception {
    File aliceProject = temporaryFolder.newFile("standalone-smoke.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    Path projectDirectory = temporaryFolder.newFolder("standalone-project").toPath();
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory.toFile(), null, false);
    writeJavaFxStubs(sourceDirectory);

    Path classesDirectory = projectDirectory.resolve("build").resolve("classes");
    compileJavaSources(classesDirectory, javaSourcesUnder(sourceDirectory));

    try (GeneratedProjectClassLoader classLoader = new GeneratedProjectClassLoader(
        new URL[] {classesDirectory.toUri().toURL()})) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);
      Class<?> applicationClass = Class.forName("javafx.application.Application", true, classLoader);
      Class<?> stageClass = Class.forName("javafx.stage.Stage", true, classLoader);
      String[] args = {"--project", "standalone-smoke.a3p"};

      String output = captureSystemOut(() -> {
        launcherClass.getMethod("main", String[].class).invoke(null, (Object) args);
        Thread.sleep(100L);
      });

      assertArrayEquals(args, (String[]) applicationClass.getField("launchedArgs").get(null));
      assertTrue((Boolean) applicationClass.getField("startInvoked").get(null));
      assertTrue((Boolean) stageClass.getField("sceneConfigured").get(null));
      assertTrue((Boolean) stageClass.getField("showInvoked").get(null));
      assertTrue((Boolean) stageClass.getField("showing").get(null));
      assertOutputContainsInOrder(
          output,
          "ALICE_LAUNCHER_EVIDENCE main-entered",
          "ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted",
          "ALICE_LAUNCHER_EVIDENCE javafx-application-started",
          "ALICE_LAUNCHER_EVIDENCE stage-received",
          "ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker",
          "ALICE_LAUNCHER_EVIDENCE stage-show-attempted",
          RENDER_OBSERVATION_JSON_PREFIX,
          "\"status\":\"shown-target-pixel-observed\"",
          "\"renderTargetShowing\":true",
          "\"pixelsObserved\":true",
          "\"missingObservationMechanism\":\"none\"",
          "ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker");
      assertFalse(output.contains("ALICE_LAUNCHER_NO_GO pixel-observation"));
    }
  }

  @Test
  public void generatedLauncherInvokesProgramMainThroughStubbedJavaFxLaunchPath() throws Exception {
    File aliceProject = temporaryFolder.newFile("launcher-runtime.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programTypeWithMainProbe("Program"), Project.SceneCameraType.WindowCamera));
    Path projectDirectory = temporaryFolder.newFolder("launcher-runtime-project").toPath();
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory.toFile(), null, false);
    writeJavaFxStubs(sourceDirectory);

    Path classesDirectory = projectDirectory.resolve("build").resolve("classes");
    compileJavaSources(classesDirectory, javaSourcesUnder(sourceDirectory));

    CountDownLatch latch = new CountDownLatch(1);
    synchronized (GENERATED_PROGRAM_PROBE_LOCK) {
      generatedProgramMainArgs = null;
      generatedProgramMainThreadName = null;
      generatedProgramMainLatch = latch;
    }
    try (GeneratedProjectClassLoader classLoader = new GeneratedProjectClassLoader(
        new URL[] {classesDirectory.toUri().toURL()})) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);
      String[] args = {"--project", "launcher-runtime.a3p"};

      String output = captureSystemOut(() -> {
        launcherClass.getMethod("main", String[].class).invoke(null, (Object) args);
        assertTrue(
            "Stubbed JavaFX launch path should reach the generated Program.main probe",
            latch.await(5, TimeUnit.SECONDS));
      });
      assertArrayEquals(args, generatedProgramMainArgs);
      assertEquals("AliceJavaFXLauncher-ProgramMain", generatedProgramMainThreadName);
      assertOutputContainsInOrder(
          output,
          "ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker",
          "ALICE_LAUNCHER_EVIDENCE stage-show-attempted",
          RENDER_OBSERVATION_JSON_PREFIX,
          "\"status\":\"shown-target-pixel-observed\"",
          "\"renderTargetShowing\":true",
          "\"pixelsObserved\":true",
          "\"missingObservationMechanism\":\"none\"",
          "ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker",
          "ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted");
    } finally {
      synchronized (GENERATED_PROGRAM_PROBE_LOCK) {
        generatedProgramMainArgs = null;
        generatedProgramMainThreadName = null;
        generatedProgramMainLatch = null;
      }
    }
  }

  @Test
  public void generatedLauncherReportsRenderTargetUnavailableWhenStageShowCannotPresent() throws Exception {
    Path projectDirectory = temporaryFolder.newFolder("launcher-render-target-unavailable-project").toPath();
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramUnexpectedRunMarkerSource(sourceDirectory);
    writeJavaFxRenderTargetUnavailableStubs(sourceDirectory);

    Path classesDirectory = projectDirectory.resolve("build").resolve("classes");
    compileJavaSources(classesDirectory, javaSourcesUnder(sourceDirectory));

    Path programMarker = projectDirectory.resolve("program-main-marker.txt");
    String previousMarker = System.getProperty("alice.test.program.marker");
    System.setProperty("alice.test.program.marker", programMarker.toAbsolutePath().normalize().toString());
    try (GeneratedProjectClassLoader classLoader = new GeneratedProjectClassLoader(
        new URL[] {classesDirectory.toUri().toURL()})) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);

      String output = captureSystemOut(() ->
          launcherClass.getMethod("main", String[].class).invoke(null, (Object) new String[] {"render-target"}));
      assertOutputContainsInOrder(
          output,
          "ALICE_LAUNCHER_EVIDENCE main-entered",
          "ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted",
          "ALICE_LAUNCHER_EVIDENCE javafx-application-started",
          "ALICE_LAUNCHER_EVIDENCE stage-received",
          "ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker",
          "ALICE_LAUNCHER_EVIDENCE stage-show-attempted",
          RENDER_OBSERVATION_JSON_PREFIX,
          "\"status\":\"render-target-absent\"",
          "\"renderTargetShowing\":false",
          "\"pixelsObserved\":false",
          "\"missingObservationMechanism\":\"stage-show\"",
          "ALICE_LAUNCHER_NO_GO render-target-unavailable");
      assertTrue(output.contains("\"detail\":\"Stage.show failed before a render target could be observed.\"}"));
      assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE render-target-ready"));
      assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated"));
    } finally {
      if (previousMarker == null) {
        System.clearProperty("alice.test.program.marker");
      } else {
        System.setProperty("alice.test.program.marker", previousMarker);
      }
    }
    assertFalse(
        "Program.main must not run when the generated launcher cannot present a render target",
        Files.exists(programMarker));
  }

  @Test
  public void generatedLauncherReportsDisplayUnavailableWhenJavaFxLaunchCannotCreateDisplay() throws Exception {
    Path projectDirectory = temporaryFolder.newFolder("launcher-display-unavailable-project").toPath();
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramUnexpectedRunMarkerSource(sourceDirectory);
    writeJavaFxDisplayUnavailableStubs(sourceDirectory);

    Path classesDirectory = projectDirectory.resolve("build").resolve("classes");
    compileJavaSources(classesDirectory, javaSourcesUnder(sourceDirectory));

    Path programMarker = projectDirectory.resolve("program-main-marker.txt");
    String previousMarker = System.getProperty("alice.test.program.marker");
    System.setProperty("alice.test.program.marker", programMarker.toAbsolutePath().normalize().toString());
    try (GeneratedProjectClassLoader classLoader = new GeneratedProjectClassLoader(
        new URL[] {classesDirectory.toUri().toURL()})) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);

      String output = captureSystemOut(() ->
          launcherClass.getMethod("main", String[].class).invoke(null, (Object) new String[] {"headless"}));
      assertOutputContainsInOrder(
          output,
          "ALICE_LAUNCHER_EVIDENCE main-entered",
          "ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted",
          RENDER_OBSERVATION_JSON_PREFIX,
          "\"status\":\"render-target-absent\"",
          "\"renderTargetShowing\":false",
          "\"pixelsObserved\":false",
          "\"missingObservationMechanism\":\"javafx-display\"",
          "ALICE_LAUNCHER_NO_GO display-unavailable");
      assertTrue(output.contains("\"detail\":\"JavaFX launch failed before a Stage/render target was available.\"}"));
      assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE javafx-application-started"));
      assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated"));
    } finally {
      if (previousMarker == null) {
        System.clearProperty("alice.test.program.marker");
      } else {
        System.setProperty("alice.test.program.marker", previousMarker);
      }
    }
    assertFalse(
        "Program.main must not run when JavaFX reports a display-unavailable launch boundary",
        Files.exists(programMarker));
  }

  @Test
  public void generatedTemplateProjectSourcesCompileWithAliceLibraryClasspath() throws Exception {
    File aliceProject = temporaryFolder.newFile("template-smoke.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    Path projectDirectory = temporaryFolder.newFolder("template-project").toPath();
    extractProjectTemplate(projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory.toFile(), null, false);

    Properties properties = loadProperties(projectDirectory.resolve("nbproject").resolve("project.properties"));
    assertEquals("src", properties.getProperty("src.dir"));
    assertEquals("AliceJavaFXLauncher", properties.getProperty("main.class"));
    assertEquals("${libs.Alice3Library.classpath}", properties.getProperty("javac.classpath").trim());
    assertTemplateCompilerStructure(properties);
    assertTrue(Files.exists(projectDirectory.resolve("build.xml")));
    assertTrue(Files.exists(projectDirectory.resolve("nbproject").resolve("build-impl.xml")));

    Path classesDirectory = resolveBuildClassesDirectory(projectDirectory, properties);
    compileJavaSources(classesDirectory, Alice3LibraryClasspathTestSupport.aliceLibraryClasspath(), javaSourcesUnder(sourceDirectory));

    assertTrue(Files.exists(classesDirectory.resolve("Program.class")));
    assertTrue(Files.exists(classesDirectory.resolve("AliceJavaFXLauncher.class")));
  }

  @Test
  public void templatePackagedLauncherJarFailsBeforeMainWhenJavaFxClassesAreAbsent() throws Exception {
    Path projectDirectory = temporaryFolder.newFolder("template-packaged-runtime").toPath();
    extractProjectTemplate(projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramMarkerSource(sourceDirectory);
    writeJavaFxLaunchMarkerStubs(sourceDirectory);

    Properties properties = loadProperties(projectDirectory.resolve("nbproject").resolve("project.properties"));
    assertEquals("AliceJavaFXLauncher", properties.getProperty("main.class"));
    Path classesDirectory = resolveBuildClassesDirectory(projectDirectory, properties);
    compileJavaSources(classesDirectory, javaSourcesUnder(sourceDirectory));

    Path distJar = packageDistJarFromTemplate(
        projectDirectory,
        classesDirectory,
        properties,
        entryName -> !entryName.startsWith("javafx/"));
    assertEquals("AliceJavaFXLauncher", mainClassInJar(distJar));

    Path launchMarker = projectDirectory.resolve("javafx-launch-marker.txt");
    Path programMarker = projectDirectory.resolve("program-main-marker.txt");
    ProcessResult result = runJarInForkedJava(projectDirectory, distJar, launchMarker, programMarker, "alpha", "beta");

    assertNotEquals("Forked java launcher should fail without JavaFX classes on the classpath", 0, result.exitCode);
    assertTrue(
        "Forked java launcher failed for an unexpected reason:\n" + result.output,
        result.output.contains("javafx/application/Application")
            || result.output.contains("javafx.application.Application"));
    assertFalse(
        "The JavaFX stub marker must not be written when the java launcher rejects the runtime before main()",
        Files.exists(launchMarker));
    assertFalse(
        "Program.main must not run when the java launcher rejects the runtime before main()",
        Files.exists(programMarker));
  }

  @Test
  public void templatePackagedLauncherWithRealJavaFxModulesStopsAtDisplayBoundaryWhenHeadless() throws Exception {
    org.junit.Assume.assumeFalse(
        "macOS CI display is available even headless; JavaFX starts instead of failing",
        System.getProperty("os.name").toLowerCase().contains("mac"));
    Path projectDirectory = temporaryFolder.newFolder("template-real-javafx-runtime").toPath();
    extractProjectTemplate(projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramMarkerSource(sourceDirectory);

    List<Path> javaFxModulePath = javaFxRuntimeModulePath();
    ProcessResult moduleResult = runForkedJava(
        projectDirectory,
        List.of(
            "--module-path", pathList(javaFxModulePath),
            "--add-modules", "javafx.graphics,javafx.media",
            "--list-modules"));
    assertEquals(moduleResult.output, 0, moduleResult.exitCode);
    assertFalse(moduleResult.output, moduleResult.timedOut);
    assertTrue(moduleResult.output, moduleResult.output.contains("javafx.base@"));
    assertTrue(moduleResult.output, moduleResult.output.contains("javafx.graphics@"));
    assertTrue(moduleResult.output, moduleResult.output.contains("javafx.media@"));

    Properties properties = loadProperties(projectDirectory.resolve("nbproject").resolve("project.properties"));
    Path classesDirectory = resolveBuildClassesDirectory(projectDirectory, properties);
    compileJavaSources(classesDirectory, pathList(javaFxModulePath), javaSourcesUnder(sourceDirectory));
    Path distJar = packageDistJarFromTemplate(projectDirectory, classesDirectory, properties);
    assertEquals("AliceJavaFXLauncher", mainClassInJar(distJar));

    Path programMarker = projectDirectory.resolve("program-main-marker.txt");
    ProcessResult launchResult = runJarWithJavaFxModulesInForkedJava(
        projectDirectory,
        distJar,
        javaFxModulePath,
        programMarker,
        "real-javafx", "display-boundary");

    if (launchResult.output.contains("ALICE_LAUNCHER_NO_GO display-unavailable")) {
      assertFalse(
          "Program.main must not run after a deterministic display-unavailable no-go",
          Files.exists(programMarker));
      assertOutputContainsInOrder(
          launchResult.output,
          "ALICE_LAUNCHER_EVIDENCE main-entered",
          "ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted",
          "ALICE_LAUNCHER_NO_GO display-unavailable");
      return;
    }

    if ((launchResult.exitCode == 0) || Files.exists(programMarker)) {
      assertProgramMarker(programMarker, "real-javafx", "display-boundary");
      assertOutputContainsInOrder(
          launchResult.output,
          "ALICE_LAUNCHER_EVIDENCE main-entered",
          "ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted",
          "ALICE_LAUNCHER_EVIDENCE javafx-application-started",
          "ALICE_LAUNCHER_EVIDENCE stage-received",
          "ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker",
          "ALICE_LAUNCHER_EVIDENCE stage-show-attempted",
          RENDER_OBSERVATION_JSON_PREFIX,
          "\"status\":\"shown-target-pixel-observed\"",
          "\"renderTargetShowing\":true",
          "\"pixelsObserved\":true",
          "\"missingObservationMechanism\":\"none\"",
          "ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker",
          "ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted");
      return;
    }

    assertFalse(
        "Program.main must not run when the real JavaFX toolkit stops at the display precondition",
        Files.exists(programMarker));
    assertTrue(
        "Real JavaFX launch failed for an unexpected reason:\n" + launchResult.output,
        launchResult.output.contains("Unable to open DISPLAY"));
  }

  @Test
  public void templatePackagedLauncherWithRealJavaFxModulesRunsOnXvfbDisplay() throws Exception {
    Path xvfbRun = findExecutableOnPath("xvfb-run");
    org.junit.Assume.assumeTrue(
        "xvfb-run is required to prove the real JavaFX display launch path",
        xvfbRun != null);
    org.junit.Assume.assumeTrue(
        "xvfb-run must be able to start a Java process before proving the real JavaFX display launch path",
        xvfbRunStartsJava(xvfbRun));
    org.junit.Assume.assumeFalse(
        "xvfb-run behaves differently on macOS even if found on PATH",
        System.getProperty("os.name").toLowerCase().contains("mac"));

    List<Path> javaFxModulePath = javaFxRuntimeModulePath();
    org.junit.Assume.assumeTrue(
        "JavaFX runtime modules must be on classpath for this test",
        !javaFxModulePath.isEmpty());

    Path projectDirectory = temporaryFolder.newFolder("template-real-javafx-xvfb-runtime").toPath();
    extractProjectTemplate(projectDirectory);
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramMarkerAndExitSource(sourceDirectory);

    Properties properties = loadProperties(projectDirectory.resolve("nbproject").resolve("project.properties"));
    Path classesDirectory = resolveBuildClassesDirectory(projectDirectory, properties);
    compileJavaSources(classesDirectory, pathList(javaFxModulePath), javaSourcesUnder(sourceDirectory));
    Path distJar = packageDistJarFromTemplate(projectDirectory, classesDirectory, properties);

    Path programMarker = projectDirectory.resolve("program-main-marker.txt");
    ProcessResult launchResult = runJarWithJavaFxModulesUnderXvfb(
        projectDirectory,
        xvfbRun,
        distJar,
        javaFxModulePath,
        programMarker,
        "real-javafx", "xvfb-display");

    assertFalse(launchResult.output, launchResult.timedOut);
    assertEquals(launchResult.output, 0, launchResult.exitCode);
    assertProgramMarker(programMarker, "real-javafx", "xvfb-display");
    assertOutputContainsInOrder(
        launchResult.output,
        "ALICE_LAUNCHER_EVIDENCE main-entered",
        "ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted",
        "ALICE_LAUNCHER_EVIDENCE javafx-application-started",
        "ALICE_LAUNCHER_EVIDENCE stage-received",
        "ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker",
        "ALICE_LAUNCHER_EVIDENCE stage-show-attempted",
        RENDER_OBSERVATION_JSON_PREFIX,
        "\"status\":\"shown-target-pixel-observed\"",
        "\"renderTargetShowing\":true",
        "\"pixelsObserved\":true",
        "\"missingObservationMechanism\":\"none\"",
        "ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker",
        "ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted");
  }

  @Test
  public void generatedLauncherNullStageGuardPreventsProgramMain() throws Exception {
    Path projectDirectory = temporaryFolder.newFolder("launcher-stage-precondition-project").toPath();
    Path sourceDirectory = projectDirectory.resolve("src");
    Files.createDirectories(sourceDirectory);

    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramUnexpectedRunMarkerSource(sourceDirectory);

    Path classesDirectory = projectDirectory.resolve("build").resolve("classes");
    compileJavaSources(classesDirectory, pathList(javaFxRuntimeModulePath()), javaSourcesUnder(sourceDirectory));

    Path programMarker = projectDirectory.resolve("program-main-marker.txt");
    String previousMarker = System.getProperty("alice.test.program.marker");
    System.setProperty("alice.test.program.marker", programMarker.toAbsolutePath().normalize().toString());
    try (GeneratedProjectClassLoader classLoader = new GeneratedProjectClassLoader(
        new URL[] {classesDirectory.toUri().toURL()})) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);
      Class<?> stageClass = Class.forName("javafx.stage.Stage", true, classLoader);
      Object launcher = launcherClass.getDeclaredConstructor().newInstance();

      String output = captureSystemOut(() ->
          launcherClass.getMethod("start", stageClass).invoke(launcher, new Object[] {null}));
      assertOutputContainsInOrder(
          output,
          "ALICE_LAUNCHER_EVIDENCE javafx-application-started",
          "ALICE_LAUNCHER_NO_GO primary-stage-unavailable");
      assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE stage-received"));
      assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated"));
    } finally {
      if (previousMarker == null) {
        System.clearProperty("alice.test.program.marker");
      } else {
        System.setProperty("alice.test.program.marker", previousMarker);
      }
    }
    assertFalse(
        "Program.main must not run when the generated launcher's null Stage guard fails",
        Files.exists(programMarker));
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    type.methods.add(mainMethod());
    return type;
  }

  private static NamedUserType programTypeWithMainProbe(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    type.methods.add(mainMethodWithProbe());
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

  private static UserMethod mainMethodWithProbe() {
    UserParameter argsParameter = new UserParameter("args", String[].class);
    JavaMethod recorder = AstMethodLookupHelpers.lookupMethod(
        ProjectCodeGeneratorStandaloneProjectTest.class,
        "recordGeneratedProgramMainArgs",
        String[].class);
    UserMethod mainMethod = new UserMethod(
        "main",
        Void.TYPE,
        new UserParameter[] {argsParameter},
        new BlockStatement(AstUtilities.createMethodInvocationStatement(
            new TypeExpression(recorder.getDeclaringType()),
            recorder,
            new ParameterAccess(argsParameter))));
    mainMethod.isStatic.setValue(true);
    mainMethod.isSignatureLocked.setValue(true);
    return mainMethod;
  }

  public static void recordGeneratedProgramMainArgs(String[] args) {
    synchronized (GENERATED_PROGRAM_PROBE_LOCK) {
      generatedProgramMainArgs = args;
      generatedProgramMainThreadName = Thread.currentThread().getName();
      if (generatedProgramMainLatch != null) {
        generatedProgramMainLatch.countDown();
      }
    }
  }

  private static final Object GENERATED_PROGRAM_PROBE_LOCK = new Object();
  private static volatile String[] generatedProgramMainArgs;
  private static volatile String generatedProgramMainThreadName;
  private static CountDownLatch generatedProgramMainLatch;

  private static void writeJavaFxStubs(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("javafx/application/Application.java"),
        """
        package javafx.application;

        public abstract class Application {
          public static volatile String[] launchedArgs;
          public static volatile boolean startInvoked;

          public abstract void start(javafx.stage.Stage stage) throws Exception;

          public static void launch(String[] args) {
            try {
              // Stub only: enough Application.launch() behavior to exercise generated launcher wiring.
              // This does not implement JavaFX toolkit initialization, lifecycle callbacks, an event
              // loop, or real Stage behavior.
              launchedArgs = args;
              String callerClassName = StackWalker
                  .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                  .walk(frames -> frames.skip(1).findFirst().orElseThrow().getDeclaringClass().getName());
              Application application = (Application) Class
                  .forName(callerClassName)
                  .getDeclaredConstructor()
                  .newInstance();
              application.start(new javafx.stage.Stage());
              startInvoked = true;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/stage/Stage.java"),
        """
        package javafx.stage;

        public class Stage {
          public static volatile boolean sceneConfigured;
          public static volatile boolean showInvoked;
          public static volatile boolean showing;

          public void setScene(javafx.scene.Scene scene) {
            sceneConfigured = scene != null;
          }

          public void show() {
            showInvoked = true;
            showing = true;
          }

          public boolean isShowing() {
            return showing;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Group.java"),
        """
        package javafx.scene;

        public class Group {
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Scene.java"),
        """
        package javafx.scene;

        public class Scene {
          public Scene(Group root) {
          }
        }
        """);
    writeShownJavaFxPixelObservationStubs(sourceDirectory);
  }

  private static void writeProgramMarkerSource(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("Program.java"),
        """
        public class Program {
          public static void main(String[] args) {
            try {
              String marker = System.getProperty("alice.test.program.marker");
              if (marker != null) {
                java.nio.file.Files.write(
                    java.nio.file.Path.of(marker),
                    java.util.Arrays.asList(args),
                    java.nio.charset.StandardCharsets.UTF_8);
              }
            } catch (java.io.IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
        """);
  }

  private static void writeProgramMarkerAndExitSource(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("Program.java"),
        """
        public class Program {
          public static void main(String[] args) {
            try {
              String marker = System.getProperty("alice.test.program.marker");
              if (marker != null) {
                java.nio.file.Files.write(
                    java.nio.file.Path.of(marker),
                    java.util.Arrays.asList(args),
                    java.nio.charset.StandardCharsets.UTF_8);
              }
            } catch (java.io.IOException e) {
              throw new RuntimeException(e);
            } finally {
              javafx.application.Platform.exit();
              System.exit(0);
            }
          }
        }
        """);
  }

  private static void writeProgramUnexpectedRunMarkerSource(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("Program.java"),
        """
        public class Program {
          public static void main(String[] args) {
            try {
              String marker = System.getProperty("alice.test.program.marker");
              if (marker != null) {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of(marker),
                    "Program.main ran despite the generated launcher's null Stage guard");
              }
            } catch (java.io.IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
        """);
  }

  private static void writeJavaFxLaunchMarkerStubs(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("javafx/application/Application.java"),
        """
        package javafx.application;

        public abstract class Application {
          public abstract void start(javafx.stage.Stage stage) throws Exception;

          public static void launch(String[] args) {
            try {
              String callerClassName = StackWalker
                  .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                  .walk(frames -> frames.skip(1).findFirst().orElseThrow().getDeclaringClass().getName());
              writeMarker(callerClassName, args);
              Application application = (Application) Class
                  .forName(callerClassName)
                  .getDeclaredConstructor()
                  .newInstance();
              application.start(new javafx.stage.Stage());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }

          private static void writeMarker(String callerClassName, String[] args) throws Exception {
            String marker = System.getProperty("alice.test.javafx.launch.marker");
            if (marker != null) {
              java.util.List<String> lines = new java.util.ArrayList<>();
              lines.add(callerClassName);
              lines.addAll(java.util.Arrays.asList(args));
              java.nio.file.Files.write(
                  java.nio.file.Path.of(marker),
                  lines,
                  java.nio.charset.StandardCharsets.UTF_8);
            }
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/stage/Stage.java"),
        """
        package javafx.stage;

        public class Stage {
          public void setScene(javafx.scene.Scene scene) {
          }

          public void show() {
          }

          public boolean isShowing() {
            return true;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Group.java"),
        """
        package javafx.scene;

        public class Group {
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Scene.java"),
        """
        package javafx.scene;

        public class Scene {
          public Scene(Group root) {
          }
        }
        """);
    writeShownJavaFxPixelObservationStubs(sourceDirectory);
  }

  private static void writeJavaFxRenderTargetUnavailableStubs(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("javafx/application/Application.java"),
        """
        package javafx.application;

        public abstract class Application {
          public abstract void start(javafx.stage.Stage stage) throws Exception;

          public static void launch(String[] args) {
            try {
              String callerClassName = StackWalker
                  .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                  .walk(frames -> frames.skip(1).findFirst().orElseThrow().getDeclaringClass().getName());
              Application application = (Application) Class
                  .forName(callerClassName)
                  .getDeclaredConstructor()
                  .newInstance();
              application.start(new javafx.stage.Stage());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/stage/Stage.java"),
        """
        package javafx.stage;

        public class Stage {
          public void setScene(javafx.scene.Scene scene) {
          }

          public void show() {
            throw new UnsupportedOperationException("No render target available for launcher test");
          }

          public boolean isShowing() {
            return false;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Group.java"),
        """
        package javafx.scene;

        public class Group {
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Scene.java"),
        """
        package javafx.scene;

        public class Scene {
          public Scene(Group root) {
          }
        }
        """);
    writeRenderTargetUnavailableJavaFxPixelObservationStubs(sourceDirectory);
  }

  private static void writeJavaFxDisplayUnavailableStubs(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("javafx/application/Application.java"),
        """
        package javafx.application;

        public abstract class Application {
          public abstract void start(javafx.stage.Stage stage) throws Exception;

          public static void launch(String[] args) {
            throw new RuntimeException(new UnsupportedOperationException("No display available for launcher test"));
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/stage/Stage.java"),
        """
        package javafx.stage;

        public class Stage {
          public void setScene(javafx.scene.Scene scene) {
          }

          public void show() {
          }

          public boolean isShowing() {
            return true;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Group.java"),
        """
        package javafx.scene;

        public class Group {
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Scene.java"),
        """
        package javafx.scene;

        public class Scene {
          public Scene(Group root) {
          }
        }
        """);
    writeShownJavaFxPixelObservationStubs(sourceDirectory);
  }

  private static void writeShownJavaFxPixelObservationStubs(Path sourceDirectory) throws Exception {
    writeJavaFxPixelObservationStubs(
        sourceDirectory,
        """
        package javafx.stage;

        public class Stage extends Window {
          public static volatile boolean sceneConfigured;
          public static volatile boolean showInvoked;
          public static volatile boolean showing;

          public void setScene(javafx.scene.Scene scene) {
            sceneConfigured = scene != null;
            if (scene != null) {
              scene.setWindow(this);
            }
          }

          public void show() {
            showInvoked = true;
            showing = true;
          }

          public boolean isShowing() {
            return showing;
          }
        }
        """);
  }

  private static void writeRenderTargetUnavailableJavaFxPixelObservationStubs(Path sourceDirectory) throws Exception {
    writeJavaFxPixelObservationStubs(
        sourceDirectory,
        """
        package javafx.stage;

        public class Stage extends Window {
          public void setScene(javafx.scene.Scene scene) {
            if (scene != null) {
              scene.setWindow(this);
            }
          }

          public void show() {
            throw new UnsupportedOperationException("No render target available for launcher test");
          }

          public boolean isShowing() {
            return false;
          }
        }
        """);
  }

  private static void writeJavaFxPixelObservationStubs(Path sourceDirectory, String stageSource) throws Exception {
    writeJavaSource(sourceDirectory.resolve("javafx/stage/Stage.java"), stageSource);
    writeJavaSource(
        sourceDirectory.resolve("javafx/stage/Window.java"),
        """
        package javafx.stage;

        public class Window {
          public double getX() {
            return 100.0;
          }

          public double getY() {
            return 120.0;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Group.java"),
        """
        package javafx.scene;

        public class Group {
          public Group(Object... children) {
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/Scene.java"),
        """
        package javafx.scene;

        public class Scene {
          private final double width;
          private final double height;
          private javafx.stage.Window window;

          public Scene(Group root, double width, double height, javafx.scene.paint.Color fill) {
            this.width = width;
            this.height = height;
          }

          public double getX() {
            return 0.0;
          }

          public double getY() {
            return 0.0;
          }

          public double getWidth() {
            return width;
          }

          public double getHeight() {
            return height;
          }

          public javafx.stage.Window getWindow() {
            return window;
          }

          public void setWindow(javafx.stage.Window window) {
            this.window = window;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/paint/Color.java"),
        """
        package javafx.scene.paint;

        public class Color {
          private final double red;
          private final double green;
          private final double blue;
          private final double opacity;

          private Color(double red, double green, double blue, double opacity) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.opacity = opacity;
          }

          public static Color rgb(int red, int green, int blue) {
            return new Color(red / 255.0, green / 255.0, blue / 255.0, 1.0);
          }

          public double getRed() {
            return red;
          }

          public double getGreen() {
            return green;
          }

          public double getBlue() {
            return blue;
          }

          public double getOpacity() {
            return opacity;
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/shape/Rectangle.java"),
        """
        package javafx.scene.shape;

        public class Rectangle {
          public Rectangle(double width, double height, javafx.scene.paint.Color fill) {
          }
        }
        """);
    writeJavaSource(
        sourceDirectory.resolve("javafx/scene/robot/Robot.java"),
        """
        package javafx.scene.robot;

        public class Robot {
          public javafx.scene.paint.Color getPixelColor(double screenX, double screenY) {
            return javafx.scene.paint.Color.rgb(32, 96, 160);
          }
        }
        """);
  }

  private static void compileJavaSources(Path outputDirectory, Path... sources) throws Exception {
    compileJavaSources(outputDirectory, System.getProperty("java.class.path"), sources);
  }

  private static void compileJavaSources(Path outputDirectory, String classpath, Path... sources) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull("Tests must run on a JDK with the Java compiler available", compiler);
    Files.createDirectories(outputDirectory);
    StringWriter compilerOutput = new StringWriter();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      List<String> options = Arrays.asList(
          "-classpath",
          classpath,
          "-proc:none",
          "-d",
          outputDirectory.toString());
      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
          Arrays.stream(sources).map(Path::toFile).toList());
      Boolean result = compiler.getTask(
          compilerOutput,
          fileManager,
          null,
          options,
          null,
          compilationUnits).call();
      assertTrue(compilerOutput.toString(), result);
    }
  }

  private static Path[] javaSourcesUnder(Path sourceDirectory) throws Exception {
    try (Stream<Path> paths = Files.walk(sourceDirectory)) {
      return paths
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .toArray(Path[]::new);
    }
  }

  private static void extractProjectTemplate(Path projectDirectory) throws Exception {
    Path archive = Path.of("target/classes/org/alice/netbeans/ProjectTemplate.zip");
    assertTrue("ProjectTemplate.zip must be built as a test resource", Files.exists(archive));
    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        Path entryPath = projectDirectory.resolve(entry.getName()).normalize();
        assertTrue(entry.getName(), entryPath.startsWith(projectDirectory));
        if (entry.isDirectory()) {
          Files.createDirectories(entryPath);
        } else {
          Files.createDirectories(entryPath.getParent());
          Files.copy(zipInputStream, entryPath);
        }
      }
    }
  }

  private static Properties loadProperties(Path propertiesPath) throws Exception {
    Properties properties = new Properties();
    try (java.io.Reader reader = Files.newBufferedReader(propertiesPath)) {
      properties.load(reader);
    }
    return properties;
  }

  private static void assertTemplateCompilerStructure(Properties properties) {
    assertEquals("build", properties.getProperty("build.dir"));
    assertEquals("${build.dir}/classes", properties.getProperty("build.classes.dir"));
    String runClasspath = properties.getProperty("run.classpath");
    assertTrue(runClasspath, runClasspath.contains("${build.classes.dir}"));
  }

  private static Path resolveBuildClassesDirectory(Path projectDirectory, Properties properties) {
    String buildClassesDirectory = properties.getProperty("build.classes.dir")
        .replace("${build.dir}", properties.getProperty("build.dir"));
    return projectDirectory.resolve(buildClassesDirectory);
  }

  private static Path packageDistJarFromTemplate(
      Path projectDirectory,
      Path classesDirectory,
      Properties properties) throws Exception {
    return packageDistJarFromTemplate(projectDirectory, classesDirectory, properties, entryName -> true);
  }

  private static Path packageDistJarFromTemplate(
      Path projectDirectory,
      Path classesDirectory,
      Properties properties,
      Predicate<String> includeEntry) throws Exception {
    Path distJar = resolveDistJar(projectDirectory, properties);
    Files.createDirectories(distJar.getParent());
    Manifest manifest;
    try (java.io.InputStream inputStream =
             Files.newInputStream(projectDirectory.resolve(properties.getProperty("manifest.file")))) {
      manifest = new Manifest(inputStream);
    }
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, properties.getProperty("main.class"));

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(distJar), manifest);
         Stream<Path> paths = Files.walk(classesDirectory)) {
      for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
        String entryName = classesDirectory.relativize(path).toString().replace(File.separatorChar, '/');
        if (!includeEntry.test(entryName)) {
          continue;
        }
        JarEntry entry = new JarEntry(entryName);
        jarOutputStream.putNextEntry(entry);
        Files.copy(path, jarOutputStream);
        jarOutputStream.closeEntry();
      }
    }
    return distJar;
  }

  private static Path resolveDistJar(Path projectDirectory, Properties properties) {
    return projectDirectory.resolve(
        properties.getProperty("dist.jar").replace("${dist.dir}", properties.getProperty("dist.dir")));
  }

  private static String mainClassInJar(Path jarPath) throws Exception {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
    }
  }

  private static List<Path> javaFxRuntimeModulePath() throws Exception {
    Map<String, Path> modules = new LinkedHashMap<>();
    for (String entry : System.getProperty(
        "surefire.test.class.path",
        System.getProperty("java.class.path", "")).split(File.pathSeparator)) {
      if (entry.isBlank()) {
        continue;
      }
      Path path = Path.of(entry);
      String artifact = javaFxArtifact(path);
      if ((artifact != null) && containsClassEntry(path)) {
        modules.putIfAbsent(artifact, path.toAbsolutePath().normalize());
      }
    }
    assertTrue("Missing JavaFX base runtime jar on test classpath", modules.containsKey("javafx-base"));
    assertTrue("Missing JavaFX graphics runtime jar on test classpath", modules.containsKey("javafx-graphics"));
    assertTrue("Missing JavaFX media runtime jar on test classpath", modules.containsKey("javafx-media"));
    return List.of(
        modules.get("javafx-base"),
        modules.get("javafx-graphics"),
        modules.get("javafx-media"));
  }

  private static String javaFxArtifact(Path path) {
    String fileName = path.getFileName().toString();
    for (String artifact : List.of("javafx-base", "javafx-graphics", "javafx-media")) {
      if (fileName.startsWith(artifact + "-") && fileName.endsWith(".jar")) {
        return artifact;
      }
    }
    return null;
  }

  private static boolean containsClassEntry(Path jarPath) {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      return jarFile.stream().anyMatch(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"));
    } catch (Exception e) {
      return false;
    }
  }

  private static String pathList(List<Path> paths) {
    return paths.stream()
        .map(path -> path.toAbsolutePath().normalize().toString())
        .collect(java.util.stream.Collectors.joining(File.pathSeparator));
  }

  private static ProcessResult runJarInForkedJava(
      Path workingDirectory,
      Path distJar,
      Path launchMarker,
      Path programMarker,
      String... args) throws Exception {
    List<String> command = new java.util.ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-Dalice.test.javafx.launch.marker=" + launchMarker.toAbsolutePath().normalize());
    command.add("-Dalice.test.program.marker=" + programMarker.toAbsolutePath().normalize());
    command.add("-jar");
    command.add(distJar.toAbsolutePath().normalize().toString());
    command.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder(command)
        .directory(workingDirectory.toFile())
        .redirectErrorStream(true)
        .start();
    assertTrue("Timed out waiting for forked java launcher", process.waitFor(10, TimeUnit.SECONDS));
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new ProcessResult(process.exitValue(), output);
  }

  private static ProcessResult runJarWithJavaFxModulesInForkedJava(
      Path workingDirectory,
      Path distJar,
      List<Path> javaFxModulePath,
      Path programMarker,
      String... args) throws Exception {
    List<String> command = new ArrayList<>();
    command.add("-Dalice.test.program.marker=" + programMarker.toAbsolutePath().normalize());
    command.add("--module-path");
    command.add(pathList(javaFxModulePath));
    command.add("--add-modules");
    command.add("javafx.graphics,javafx.media");
    command.add("-jar");
    command.add(distJar.toAbsolutePath().normalize().toString());
    command.addAll(Arrays.asList(args));
    return runForkedJava(workingDirectory, command);
  }

  private static ProcessResult runJarWithJavaFxModulesUnderXvfb(
      Path workingDirectory,
      Path xvfbRun,
      Path distJar,
      List<Path> javaFxModulePath,
      Path programMarker,
      String... args) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(xvfbRun.toAbsolutePath().normalize().toString());
    command.add("-a");
    command.add("-s");
    command.add("-screen 0 1024x768x24");
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-Dalice.test.program.marker=" + programMarker.toAbsolutePath().normalize());
    command.add("--module-path");
    command.add(pathList(javaFxModulePath));
    command.add("--add-modules");
    command.add("javafx.graphics,javafx.media");
    command.add("-jar");
    command.add(distJar.toAbsolutePath().normalize().toString());
    command.addAll(Arrays.asList(args));
    return runCommand(workingDirectory, command);
  }

  private static ProcessResult runForkedJava(Path workingDirectory, List<String> javaArguments) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.addAll(javaArguments);
    return runCommand(workingDirectory, command);
  }

  private static ProcessResult runCommand(Path workingDirectory, List<String> command) throws Exception {
    Process process = new ProcessBuilder(command)
        .directory(workingDirectory.toFile())
        .redirectErrorStream(true)
        .start();
    ByteArrayOutputStream drainBuffer = new ByteArrayOutputStream(4096);
    Thread drainThread = new Thread(() -> {
      byte[] buf = new byte[8192];
      try {
        int n;
        while ((n = process.getInputStream().read(buf)) != -1) {
          drainBuffer.write(buf, 0, n);
        }
      } catch (IOException ignored) {
        // Stream closed by destroyForcibly — partial output preserved in drainBuffer
      }
    }, "process-stdout-drain");
    drainThread.setDaemon(true);
    drainThread.start();
    boolean exited = process.waitFor(30, TimeUnit.SECONDS);
    if (!exited) {
      process.destroyForcibly();
      assertTrue("Timed out waiting for forked java to terminate", process.waitFor(5, TimeUnit.SECONDS));
    }
    drainThread.join(5000);
    String output = drainBuffer.toString(StandardCharsets.UTF_8);
    return new ProcessResult(exited ? process.exitValue() : -1, output, !exited);
  }

  private static Path findExecutableOnPath(String executableName) {
    String path = System.getenv("PATH");
    if (path == null) {
      return null;
    }
    for (String entry : path.split(File.pathSeparator)) {
      if (entry.isBlank()) {
        continue;
      }
      Path candidate = Path.of(entry, executableName);
      if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private static boolean xvfbRunStartsJava(Path xvfbRun) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(xvfbRun.toAbsolutePath().normalize().toString());
    command.add("-a");
    command.add("-s");
    command.add("-screen 0 1024x768x24");
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-version");

    ProcessResult result = runCommand(Path.of(".").toAbsolutePath().normalize(), command);
    return !result.timedOut && result.exitCode == 0;
  }

  private static void assertProgramMarker(Path programMarker, String... expectedArgs) throws Exception {
    assertTrue("Program.main marker should be written after real JavaFX start", Files.exists(programMarker));
    assertEquals(Arrays.asList(expectedArgs), Files.readAllLines(programMarker, StandardCharsets.UTF_8));
  }

  private static void writeJavaSource(Path sourcePath, String source) throws Exception {
    Files.createDirectories(sourcePath.getParent());
    Files.writeString(sourcePath, source);
  }

  private static String captureSystemOut(ThrowingRunnable runnable) throws Exception {
    PrintStream previousOut = System.out;
    ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
    try (PrintStream capture = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
      System.setOut(capture);
      runnable.run();
      capture.flush();
    } finally {
      System.setOut(previousOut);
    }
    return capturedOutput.toString(StandardCharsets.UTF_8);
  }

  private static void assertOutputContainsInOrder(String output, String... expectedLines) {
    int cursor = -1;
    for (String expectedLine : expectedLines) {
      int next = output.indexOf(expectedLine, cursor + 1);
      assertTrue("Expected output to contain in order: " + expectedLine + "\nActual output:\n" + output, next >= 0);
      cursor = next;
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private static class GeneratedProjectClassLoader extends URLClassLoader {
    GeneratedProjectClassLoader(URL[] urls) {
      super(urls, ProjectCodeGeneratorStandaloneProjectTest.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("javafx.") || "Program".equals(name) || "AliceJavaFXLauncher".equals(name)) {
        synchronized (getClassLoadingLock(name)) {
          Class<?> loadedClass = findLoadedClass(name);
          if (loadedClass == null) {
            try {
              loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
              loadedClass = super.loadClass(name, false);
            }
          }
          if (resolve) {
            resolveClass(loadedClass);
          }
          return loadedClass;
        }
      }
      return super.loadClass(name, resolve);
    }
  }

  private static class ProcessResult {
    private final int exitCode;
    private final String output;
    private final boolean timedOut;

    private ProcessResult(int exitCode, String output) {
      this(exitCode, output, false);
    }

    private ProcessResult(int exitCode, String output, boolean timedOut) {
      this.exitCode = exitCode;
      this.output = output;
      this.timedOut = timedOut;
    }
  }
}
