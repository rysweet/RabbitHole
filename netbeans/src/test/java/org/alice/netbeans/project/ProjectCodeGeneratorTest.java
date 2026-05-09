package org.alice.netbeans.project;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
import org.openide.filesystems.FileObject;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.Assert.*;

public class ProjectCodeGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedLauncherMatchesNetBeansMainClassTemplate() throws Exception {
    Properties projectProperties = new Properties();
    Path projectPropertiesPath = Path.of("src/main/resources/ProjectTemplate/nbproject/project.properties");
    assertTrue(Files.exists(projectPropertiesPath));
    try (Reader reader = Files.newBufferedReader(projectPropertiesPath)) {
      projectProperties.load(reader);
    }

    File sourceDirectory = temporaryFolder.newFolder("src");
    FileObject launcherFileObject = ProjectCodeGenerator.generateLauncher(sourceDirectory);
    assertNotNull(launcherFileObject);

    Path launcherPath = sourceDirectory.toPath().resolve(launcherFileObject.getNameExt());
    String launcherSource = Files.readString(launcherPath);
    String launcherClassName = launcherFileObject.getName();

    assertEquals("AliceJavaFXLauncher.java", launcherFileObject.getNameExt());
    assertEquals("AliceJavaFXLauncher", launcherClassName);
    assertEquals(launcherClassName, projectProperties.getProperty("main.class"));
    assertTrue(launcherSource.contains("public class " + launcherClassName + " extends Application"));
    assertTrue(launcherSource.contains("Program.main(startingArgs)"));
    assertTrue(launcherSource.contains("Application.launch(args)"));
  }

  @Test
  public void generatedLauncherDefinesJavaFxHandoffAndVisiblePixelObservationEvidence() throws Exception {
    File sourceDirectory = temporaryFolder.newFolder("launcher-evidence-src");
    FileObject launcherFileObject = ProjectCodeGenerator.generateLauncher(sourceDirectory);
    Path launcherPath = sourceDirectory.toPath().resolve(launcherFileObject.getNameExt());

    String launcherSource = Files.readString(launcherPath);

    assertTrue(launcherSource.contains("import javafx.scene.Group;"));
    assertTrue(launcherSource.contains("import javafx.scene.Scene;"));
    assertTrue(launcherSource.contains("import javafx.scene.paint.Color;"));
    assertTrue(launcherSource.contains("import javafx.scene.robot.Robot;"));
    assertTrue(launcherSource.contains("import javafx.scene.shape.Rectangle;"));
    assertTrue(launcherSource.contains("import javafx.stage.Window;"));
    assertTrue(launcherSource.contains("\"ALICE_LAUNCHER_EVIDENCE\""));
    assertTrue(launcherSource.contains("\"ALICE_LAUNCHER_NO_GO\""));
    assertTrue(launcherSource.contains("\"ALICE_LAUNCHER_RENDER_OBSERVATION\""));
    assertTrue(launcherSource.contains("evidence(\"main-entered\")"));
    assertTrue(launcherSource.contains("evidence(\"javafx-launch-attempted\")"));
    assertTrue(launcherSource.contains("Application.launch(args)"));
    assertTrue(launcherSource.contains("evidence(\"javafx-application-started\")"));
    assertTrue(launcherSource.contains("noGo(\"primary-stage-unavailable\")"));
    assertTrue(launcherSource.contains("private static final Color OBSERVATION_MARKER_COLOR"));
    assertTrue(launcherSource.contains("new Scene(root, 64.0, 64.0, OBSERVATION_MARKER_COLOR)"));
    assertTrue(launcherSource.contains("new Rectangle(64.0, 64.0, OBSERVATION_MARKER_COLOR)"));
    assertTrue(launcherSource.contains("evidence(\"scene-configured observation-marker\")"));
    assertTrue(launcherSource.contains("evidence(\"stage-show-attempted\")"));
    assertTrue(launcherSource.contains("primaryStage.show()"));
    assertTrue(launcherSource.contains("primaryStage.isShowing()"));
    assertTrue(launcherSource.contains("renderObservation("));
    assertTrue(launcherSource.contains("jsonField(\"schema_version\", \"alice.launcher.render-observation/v1\")"));
    assertTrue(launcherSource.contains("\"render-target-absent\""));
    assertTrue(launcherSource.contains("\"shown-target-pixel-observed\""));
    assertTrue(launcherSource.contains("\"pixel-observation-unsupported\""));
    assertTrue(launcherSource.contains("\"stage-window-screen-bounds\""));
    assertTrue(launcherSource.contains("Robot robot = new Robot()"));
    assertTrue(launcherSource.contains("robot.getPixelColor(screenX, screenY)"));
    assertTrue(launcherSource.contains("evidence(\"pixels-observed shown-stage-marker\")"));
    assertTrue(launcherSource.contains("noGo(pixelObservation.status)"));
    assertTrue(launcherSource.contains("noGo(\"render-target-unavailable\")"));
    assertTrue(launcherSource.contains("evidence(\"program-main-delegated rendering-not-asserted\")"));
    assertTrue(launcherSource.contains("Program.main(startingArgs)"));
    assertTrue(launcherSource.contains("isDisplayUnavailableFailure"));
    assertTrue(launcherSource.contains("noGo(\"display-unavailable\")"));
    assertTrue(launcherSource.contains("PixelObservation.observed("));
    assertFalse(launcherSource.toLowerCase().contains("world pixels observed"));
  }

  @Test
  public void generatedLauncherReportsVisiblePixelObservedWhenRobotSamplesMarker() throws Exception {
    String output = runGeneratedLauncherWithRobot(
        "launcher-visible-pixel-observed",
        """
        package javafx.scene.robot;

        public class Robot {
          public javafx.scene.paint.Color getPixelColor(double screenX, double screenY) {
            return javafx.scene.paint.Color.rgb(32, 96, 160);
          }
        }
        """);

    assertTrue(output.contains("ALICE_LAUNCHER_RENDER_OBSERVATION"));
    assertTrue(output.contains("\"status\":\"shown-target-pixel-observed\""));
    assertTrue(output.contains("\"renderTargetShowing\":true"));
    assertTrue(output.contains("\"pixelsObserved\":true"));
    assertTrue(output.contains("ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker"));
    assertFalse(output.contains("ALICE_LAUNCHER_NO_GO pixel-observation"));
  }

  @Test
  public void generatedLauncherReportsUnsupportedPixelObservationWithoutPretendingSuccess() throws Exception {
    String output = runGeneratedLauncherWithRobot(
        "launcher-visible-pixel-unsupported",
        """
        package javafx.scene.robot;

        public class Robot {
          public javafx.scene.paint.Color getPixelColor(double screenX, double screenY) {
            throw new UnsupportedOperationException("screen capture unsupported in this runtime");
          }
        }
        """);

    assertTrue(output.contains("ALICE_LAUNCHER_RENDER_OBSERVATION"));
    assertTrue(output.contains("\"status\":\"pixel-observation-unsupported\""));
    assertTrue(output.contains("\"renderTargetShowing\":true"));
    assertTrue(output.contains("\"pixelsObserved\":false"));
    assertTrue(output.contains("\"missingObservationMechanism\":\"javafx.scene.robot.Robot\""));
    assertTrue(output.contains("ALICE_LAUNCHER_NO_GO pixel-observation-unsupported"));
    assertFalse(output.contains("\"status\":\"shown-target-pixel-observed\""));
    assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker"));
    assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted"));
  }

  @Test
  public void generatedLauncherEscapesRenderObservationDetailAsJsonString() throws Exception {
    String output = runGeneratedLauncherWithRobot(
        "launcher-visible-pixel-json-escaping",
        """
        package javafx.scene.robot;

        public class Robot {
          public javafx.scene.paint.Color getPixelColor(double screenX, double screenY) {
            throw new UnsupportedOperationException("screen capture \\"denied\\"\\nbackslash \\\\ end");
          }
        }
        """);

    assertTrue(output.contains("ALICE_LAUNCHER_RENDER_OBSERVATION"));
    assertTrue(output.contains("\"status\":\"pixel-observation-unsupported\""));
    assertTrue(
        output,
        output.contains("\"detail\":\"JavaFX Robot screen capture was unavailable: "
            + "java.lang.UnsupportedOperationException: screen capture \\\"denied\\\"\\nbackslash \\\\ end\""));
    assertFalse(output.contains("screen capture \\\"denied\\\"\nbackslash"));
    assertTrue(output.contains("ALICE_LAUNCHER_NO_GO pixel-observation-unsupported"));
    assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted"));
  }

  @Test
  public void generatedLauncherReportsPixelMismatchWithoutDelegatingProgramMain() throws Exception {
    String output = runGeneratedLauncherWithRobot(
        "launcher-visible-pixel-mismatch",
        """
        package javafx.scene.robot;

        public class Robot {
          public javafx.scene.paint.Color getPixelColor(double screenX, double screenY) {
            return javafx.scene.paint.Color.rgb(255, 0, 0);
          }
        }
        """);

    assertTrue(output.contains("ALICE_LAUNCHER_RENDER_OBSERVATION"));
    assertTrue(output.contains("\"status\":\"pixel-observation-mismatch\""));
    assertTrue(output.contains("\"renderTargetShowing\":true"));
    assertTrue(output.contains("\"pixelsObserved\":false"));
    assertTrue(output.contains("\"missingObservationMechanism\":\"expected-observation-marker-pixel\""));
    assertTrue(output.contains("ALICE_LAUNCHER_NO_GO pixel-observation-mismatch"));
    assertFalse(output.contains("\"status\":\"shown-target-pixel-observed\""));
    assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker"));
    assertFalse(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted"));
  }

  @Test
  public void generatedLauncherPassesStartingArgsToProgramMain() throws Exception {
    assertGeneratedLauncherPassesStartingArgsToProgramMain(
        "launcher-runtime",
        new String[] {"alpha", "beta"});
  }

  @Test
  public void generatedLauncherPreservesEmptyStartingArgsToProgramMain() throws Exception {
    String[] args = {};
    assertSame(
        args,
        assertGeneratedLauncherPassesStartingArgsToProgramMain("launcher-empty-args-runtime", args));
  }

  private String runGeneratedLauncherWithRobot(String folderName, String robotSource) throws Exception {
    Path sourceDirectory = temporaryFolder.newFolder(folderName + "-src").toPath();
    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramMainCaptureStub(sourceDirectory);
    writeJavaFxApplicationStub(sourceDirectory);
    writeJavaFxObservationStubs(sourceDirectory, robotSource);
    Path classesDirectory = temporaryFolder.newFolder(folderName + "-classes").toPath();
    compileLauncherRuntimeSources(sourceDirectory, classesDirectory);

    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] {classesDirectory.toUri().toURL()},
        ClassLoader.getPlatformClassLoader())) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);
      Class<?> programClass = Class.forName("Program", true, classLoader);

      return captureSystemOut(() -> {
        launcherClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        waitForStringArray(programClass.getField("receivedArgs"));
      });
    }
  }

  private String[] assertGeneratedLauncherPassesStartingArgsToProgramMain(String folderName, String[] args)
      throws Exception {
    Path sourceDirectory = temporaryFolder.newFolder(folderName + "-src").toPath();
    ProjectCodeGenerator.generateLauncher(sourceDirectory.toFile());
    writeProgramMainCaptureStub(sourceDirectory);
    writeJavaFxApplicationStub(sourceDirectory);
    writeJavaFxObservationStubs(sourceDirectory, defaultRobotSource());
    Path classesDirectory = temporaryFolder.newFolder(folderName + "-classes").toPath();
    compileLauncherRuntimeSources(sourceDirectory, classesDirectory);

    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] {classesDirectory.toUri().toURL()},
        ClassLoader.getPlatformClassLoader())) {
      Class<?> launcherClass = Class.forName("AliceJavaFXLauncher", true, classLoader);
      Class<?> programClass = Class.forName("Program", true, classLoader);

      final String[][] receivedArgs = new String[1][];
      String output = captureSystemOut(() -> {
        launcherClass.getMethod("main", String[].class).invoke(null, (Object) args);
        receivedArgs[0] = waitForStringArray(programClass.getField("receivedArgs"));
      });
      assertTrue(output.contains("ALICE_LAUNCHER_EVIDENCE javafx-application-started"));
      assertTrue(output.contains("ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted"));
      assertArrayEquals(args, receivedArgs[0]);
      return receivedArgs[0];
    }
  }

  private static void writeProgramMainCaptureStub(Path sourceDirectory) throws Exception {
    writeJavaSource(
        sourceDirectory.resolve("Program.java"),
        """
        public class Program {
          public static volatile String[] receivedArgs;

          public static void main(String[] args) {
            receivedArgs = args;
          }
        }
        """);
  }

  private static void writeJavaFxApplicationStub(Path sourceDirectory) throws Exception {
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
  }

  private static void writeJavaFxObservationStubs(Path sourceDirectory, String robotSource) throws Exception {
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
        sourceDirectory.resolve("javafx/stage/Stage.java"),
        """
        package javafx.stage;

        public class Stage extends Window {
          private boolean showing;

          public void setScene(javafx.scene.Scene scene) {
            scene.setWindow(this);
          }

          public void show() {
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
    writeJavaSource(sourceDirectory.resolve("javafx/scene/robot/Robot.java"), robotSource);
  }

  private static String defaultRobotSource() {
    return """
        package javafx.scene.robot;

        public class Robot {
          public javafx.scene.paint.Color getPixelColor(double screenX, double screenY) {
            return javafx.scene.paint.Color.rgb(32, 96, 160);
          }
        }
        """;
  }

  private static void compileLauncherRuntimeSources(Path sourceDirectory, Path classesDirectory) throws Exception {
    compileJavaSources(
        classesDirectory,
        sourceDirectory.resolve("AliceJavaFXLauncher.java"),
        sourceDirectory.resolve("Program.java"),
        sourceDirectory.resolve("javafx/application/Application.java"),
        sourceDirectory.resolve("javafx/stage/Stage.java"),
        sourceDirectory.resolve("javafx/stage/Window.java"),
        sourceDirectory.resolve("javafx/scene/Group.java"),
        sourceDirectory.resolve("javafx/scene/Scene.java"),
        sourceDirectory.resolve("javafx/scene/paint/Color.java"),
        sourceDirectory.resolve("javafx/scene/shape/Rectangle.java"),
        sourceDirectory.resolve("javafx/scene/robot/Robot.java"));
  }

  @Test
  public void generatesProgramAndLauncherFromSyntheticAliceProject() throws Exception {
    File aliceProject = temporaryFolder.newFile("synthetic.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder("generated-src");

    Collection<FileObject> filesToOpen = ProjectCodeGenerator.generateCode(
        aliceProject,
        sourceDirectory,
        null,
        false);

    Path programPath = sourceDirectory.toPath().resolve("Program.java");
    Path launcherPath = sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java");
    assertTrue(Files.exists(programPath));
    assertTrue(Files.exists(launcherPath));
    assertTrue(filesToOpen.stream()
        .anyMatch(fileObject -> "AliceJavaFXLauncher.java".equals(fileObject.getNameExt())));
    String programSource = Files.readString(programPath);
    assertTrue(programSource.contains("class Program extends SProgram"));
  }

  @Test
  public void generateCodePreservesUnrelatedDestinationJavaFile() throws Exception {
    File aliceProject = temporaryFolder.newFile("synthetic-preserve-destination.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder("non-empty-generated-src");
    Path notesPath = sourceDirectory.toPath().resolve("Notes.java");
    String notesSource = """
        public class Notes {
          String authorNote = "keep me";
        }
        """;
    Files.writeString(notesPath, notesSource);

    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    assertEquals(notesSource, Files.readString(notesPath));
    assertTrue(Files.exists(sourceDirectory.toPath().resolve("Program.java")));
    assertTrue(Files.exists(sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java")));
  }

  @Test
  public void generateCodeRejectsGeneratedSourceConflictWithoutOverwritingDestination() throws Exception {
    File aliceProject = temporaryFolder.newFile("synthetic-conflicting-destination.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder("conflicting-generated-src");
    Path programPath = sourceDirectory.toPath().resolve("Program.java");
    Path notesPath = sourceDirectory.toPath().resolve("Notes.java");
    String handAuthoredProgram = """
        public class Program {
          String owner = "student";
        }
        """;
    String notesSource = "class Notes {}\n";
    Files.writeString(programPath, handAuthoredProgram);
    Files.writeString(notesPath, notesSource);

    try {
      ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
      fail("Expected IOException for existing generated destination");
    } catch (java.io.IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("Generated destination already exists"));
    }

    assertEquals(handAuthoredProgram, Files.readString(programPath));
    assertEquals(notesSource, Files.readString(notesPath));
    assertFalse(Files.exists(sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java")));
  }

  @Test
  public void generatesResourceFileAndResourcesTypeFromSyntheticAliceProject() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new TestResource("note.txt", "text/plain", data));
    File aliceProject = temporaryFolder.newFile("synthetic-resource.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("generated-resource-src");

    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    Path generatedResourcePath = sourceDirectory.toPath().resolve("resources").resolve("note.txt");
    Path resourcesTypePath = sourceDirectory.toPath().resolve("Resources.java");
    assertArrayEquals(data, Files.readAllBytes(generatedResourcePath));
    assertTrue(Files.exists(resourcesTypePath));
    String resourcesSource = Files.readString(resourcesTypePath);
    assertTrue(resourcesSource.contains("class Resources"));
    assertTrue(resourcesSource.contains("note.txt"));
  }

  @Test
  public void generateCodeRejectsResourceFileConflictWithoutOverwritingDestination() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new TestResource("note.txt", "text/plain", data));
    File aliceProject = temporaryFolder.newFile("synthetic-resource-conflict.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("conflicting-resource-src");
    Path resourcePath = sourceDirectory.toPath().resolve("resources").resolve("note.txt");
    String handAuthoredResource = "hand-authored note\n";
    Files.createDirectories(resourcePath.getParent());
    Files.writeString(resourcePath, handAuthoredResource);

    try {
      ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
      fail("Expected IOException for existing resource destination");
    } catch (java.io.IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("Generated destination already exists"));
      assertTrue(expected.getMessage(), expected.getMessage().contains("resources"));
    }

    assertEquals(handAuthoredResource, Files.readString(resourcePath));
    assertFalse(Files.exists(sourceDirectory.toPath().resolve("Program.java")));
    assertFalse(Files.exists(sourceDirectory.toPath().resolve("Resources.java")));
    assertFalse(Files.exists(sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java")));
  }

  @Test
  public void generatedSyntheticAliceProjectSourcesCompile() throws Exception {
    File aliceProject = temporaryFolder.newFile("synthetic-compile.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("Program"), Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder("compiled-source-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    compileJavaSources(
        temporaryFolder.newFolder("compiled-classes").toPath(),
        sourceDirectory.toPath().resolve("Program.java"),
        sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java"));
  }

  @Test
  public void generatedSyntheticResourceProjectSourcesCompile() throws Exception {
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new TestResource("note.txt", "text/plain", "hello alice".getBytes(StandardCharsets.UTF_8)));
    File aliceProject = temporaryFolder.newFile("synthetic-resource-compile.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("compiled-resource-source-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    compileJavaSources(
        temporaryFolder.newFolder("compiled-resource-classes").toPath(),
        sourceDirectory.toPath().resolve("Program.java"),
        sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java"),
        sourceDirectory.toPath().resolve("Resources.java"));
  }

  @Test
  public void generatedSyntheticResourcesLoadCopiedResourceBytes() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new TestResource("note.txt", "text/plain", data));
    File aliceProject = temporaryFolder.newFile("synthetic-resource-runtime.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("runtime-resource-source-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    assertGeneratedResourceLoads(sourceDirectory.toPath(), data);
  }

  @Test
  public void generatedSyntheticResourcesLoadOriginalFileNameWhenDisplayNameDiffers() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    TestResource resource = new TestResource("note.txt", "text/plain", data);
    resource.setName("friendly note");
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(resource);
    File aliceProject = temporaryFolder.newFile("synthetic-resource-original-name.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("runtime-original-name-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    assertTrue(Files.exists(sourceDirectory.toPath().resolve("resources").resolve("note.txt")));
    assertFalse(Files.exists(sourceDirectory.toPath().resolve("resources").resolve("friendly note")));
    assertGeneratedResourceLoads(sourceDirectory.toPath(), data);
  }

  @Test
  public void generatedSyntheticResourcesLoadDuplicateOriginalFileNames() throws Exception {
    TestResource first = new TestResource("note.txt", "text/plain", "first".getBytes(StandardCharsets.UTF_8));
    first.setName("first note");
    TestResource second = new TestResource("note.txt", "text/plain", "second".getBytes(StandardCharsets.UTF_8));
    second.setName("second note");
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(first);
    project.addResource(second);
    File aliceProject = temporaryFolder.newFile("synthetic-resource-duplicate-original-name.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("runtime-duplicate-original-name-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    assertTrue(Files.exists(sourceDirectory.toPath().resolve("resources").resolve("note.txt")));
    assertTrue(Files.exists(sourceDirectory.toPath().resolve("resources2").resolve("note.txt")));
    List<Resource> generatedResources = loadGeneratedResources(sourceDirectory.toPath());
    Set<String> generatedResourceText = generatedResources.stream()
        .map(resource -> new String(resource.getData(), StandardCharsets.UTF_8))
        .collect(Collectors.toSet());
    assertEquals(Set.of("first", "second"), generatedResourceText);
  }

  @Test
  public void generatedSyntheticResourcesLoadBlankOriginalFileName() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    TestResource resource = new TestResource("note.txt", "text/plain", data);
    resource.setOriginalFileName("");
    resource.setName("friendly note");
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(resource);
    File aliceProject = temporaryFolder.newFile("synthetic-resource-blank-original-name.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("runtime-blank-original-name-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    assertTrue(Files.exists(sourceDirectory.toPath().resolve("resources").resolve("friendly_note")));
    assertGeneratedResourceLoads(sourceDirectory.toPath(), data);
  }

  @Test
  public void generatedSyntheticResourcesLoadUnsafeOriginalFileName() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    TestResource resource = new TestResource("note.txt", "text/plain", data);
    resource.setOriginalFileName("../folder\\note.txt");
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(resource);
    File aliceProject = temporaryFolder.newFile("synthetic-resource-unsafe-original-name.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFolder("runtime-unsafe-original-name-src");
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);

    assertTrue(Files.exists(sourceDirectory.toPath().resolve("resources").resolve(".._folder_note.txt")));
    assertFalse(Files.exists(sourceDirectory.toPath().resolve("folder").resolve("note.txt")));
    assertGeneratedResourceLoads(sourceDirectory.toPath(), data);
  }

  @Test
  public void rejectsUnsafeAliceTypeNamesBeforeWritingGeneratedSources() throws Exception {
    String absolutePathName = temporaryFolder.getRoot().toPath().resolve("absolute").toAbsolutePath().toString();
    for (String typeName : List.of("../Outside", "nested/Outside", "nested\\Outside", absolutePathName, "World Name", "class", "")) {
      File aliceProject = temporaryFolder.newFile("unsafe-type-" + Math.abs(typeName.hashCode()) + ".a3p");
      IoUtilities.writeProject(
          aliceProject,
          new Project(programType(typeName), Project.SceneCameraType.WindowCamera));
      File sourceDirectory = temporaryFolder.newFolder("unsafe-type-src-" + Math.abs(typeName.hashCode()));

      try {
        ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
        fail("Expected IOException for unsafe type name: " + typeName);
      } catch (java.io.IOException expected) {
        assertTrue(expected.getMessage(), expected.getMessage().contains("Unsafe Alice type name"));
      }
    }
  }

  @Test
  public void rejectedTraversingAliceTypeNameDoesNotWriteOutsideSourceDirectory() throws Exception {
    File aliceProject = temporaryFolder.newFile("unsafe-traversal-type.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("../Outside"), Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder("unsafe-traversal-type-src");
    Path outsidePath = sourceDirectory.toPath().getParent().resolve("Outside.java");

    try {
      ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
      fail("Expected IOException");
    } catch (java.io.IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("Unsafe Alice type name"));
    }

    assertFalse(Files.exists(outsidePath));
  }

  @Test
  public void rejectsUnsafeAliceDeclarationNamesBeforeWritingGeneratedSources() throws Exception {
    for (NamedUserType type : List.of(
        programTypeWithMethodName("bad(){}"),
        programTypeWithFieldName("bad;"),
        programTypeWithParameterName("bad-name"),
        programTypeWithLocalName("bad name"))) {
      File aliceProject = temporaryFolder.newFile("unsafe-declaration-" + Math.abs(type.getName().hashCode()) + UUID.randomUUID() + ".a3p");
      IoUtilities.writeProject(
          aliceProject,
          new Project(type, Project.SceneCameraType.WindowCamera));
      File sourceDirectory = temporaryFolder.newFolder("unsafe-declaration-src-" + UUID.randomUUID());

      try {
        ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
        fail("Expected IOException for unsafe declaration in type: " + type.getName());
      } catch (java.io.IOException expected) {
        assertTrue(expected.getMessage(), expected.getMessage().contains("Unsafe Alice declaration name"));
      }

      assertFalse(Files.exists(sourceDirectory.toPath().resolve("Program.java")));
    }
  }

  @Test
  public void rejectsGeneratedTypeNameThatCollidesWithLauncherSource() throws Exception {
    File aliceProject = temporaryFolder.newFile("launcher-collision-type.a3p");
    IoUtilities.writeProject(
        aliceProject,
        new Project(programType("AliceJavaFXLauncher"), Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder("launcher-collision-type-src");

    try {
      ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
      fail("Expected IOException");
    } catch (java.io.IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("Duplicate generated Java source file"));
    }

    assertFalse(Files.exists(sourceDirectory.toPath().resolve("AliceJavaFXLauncher.java")));
  }

  @Test
  public void resourceGenerationFailsWhenSourceDirectoryIsNotAFolder() throws Exception {
    byte[] data = "hello alice".getBytes(StandardCharsets.UTF_8);
    Project project = new Project(programType("Program"), Project.SceneCameraType.WindowCamera);
    project.addResource(new TestResource("note.txt", "text/plain", data));
    File aliceProject = temporaryFolder.newFile("synthetic-resource-invalid-source.a3p");
    IoUtilities.writeProject(aliceProject, project);
    File sourceDirectory = temporaryFolder.newFile("not-a-source-directory");

    try {
      ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
      fail("Expected IOException");
    } catch (java.io.IOException expected) {
      assertTrue(expected.getMessage().contains("Java source directory is not available"));
    }
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    type.methods.add(mainMethod());
    return type;
  }

  private static NamedUserType programTypeWithMethodName(String methodName) {
    NamedUserType type = programType("Program");
    type.methods.add(new UserMethod(methodName, Void.TYPE, new UserParameter[0], new BlockStatement()));
    return type;
  }

  private static NamedUserType programTypeWithFieldName(String fieldName) {
    NamedUserType type = programType("Program");
    type.fields.add(new UserField(fieldName, String.class, new StringLiteral("hello alice")));
    return type;
  }

  private static NamedUserType programTypeWithParameterName(String parameterName) {
    NamedUserType type = programType("Program");
    type.methods.add(new UserMethod(
        "remember",
        Void.TYPE,
        new UserParameter[] {new UserParameter(parameterName, String.class)},
        new BlockStatement()));
    return type;
  }

  private static NamedUserType programTypeWithLocalName(String localName) {
    NamedUserType type = programType("Program");
    UserLocal local = new UserLocal(localName, String.class, true);
    type.methods.add(new UserMethod(
        "remember",
        Void.TYPE,
        new UserParameter[0],
        new BlockStatement(new LocalDeclarationStatement(local, new StringLiteral("hello alice")))));
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

  private static void compileJavaSources(Path outputDirectory, Path... sources) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull("Tests must run on a JDK with the Java compiler available", compiler);
    StringWriter compilerOutput = new StringWriter();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      List<String> options = Arrays.asList(
          "-classpath",
          System.getProperty("java.class.path"),
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

  private static void writeJavaSource(Path sourcePath, String source) throws Exception {
    Files.createDirectories(sourcePath.getParent());
    Files.writeString(sourcePath, source);
  }

  private static String[] waitForStringArray(Field field) throws Exception {
    for (int attempt = 0; attempt < 100; attempt++) {
      String[] value = (String[]) field.get(null);
      if (value != null) {
        return value;
      }
      Thread.sleep(10L);
    }
    return (String[]) field.get(null);
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

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private void assertGeneratedResourceLoads(Path sourceDirectory, byte[] expectedData) throws Exception {
    List<Resource> generatedResources = loadGeneratedResources(sourceDirectory);
    assertEquals(1, generatedResources.size());
    Resource generatedResource = generatedResources.get(0);
    assertEquals("text/plain", generatedResource.getContentType());
    assertArrayEquals(expectedData, generatedResource.getData());
  }

  private List<Resource> loadGeneratedResources(Path sourceDirectory) throws Exception {
    Path classesDirectory = temporaryFolder.newFolder("generated-resource-classes").toPath();
    compileJavaSources(
        classesDirectory,
        sourceDirectory.resolve("Program.java"),
        sourceDirectory.resolve("AliceJavaFXLauncher.java"),
        sourceDirectory.resolve("Resources.java"));
    copyGeneratedResourceFiles(sourceDirectory, classesDirectory);

    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] {classesDirectory.toUri().toURL()},
        ProjectCodeGeneratorTest.class.getClassLoader())) {
      Class<?> resourcesClass = Class.forName("Resources", true, classLoader);
      return Arrays.stream(resourcesClass.getFields())
          .filter(field -> TestResource.class.isAssignableFrom(field.getType()))
          .map(field -> getGeneratedResource(field))
          .toList();
    }
  }

  private static Resource getGeneratedResource(Field field) {
    try {
      field.setAccessible(true);
      return (Resource) field.get(null);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static void copyGeneratedResourceFiles(Path sourceDirectory, Path classesDirectory) throws Exception {
    try (Stream<Path> paths = Files.walk(sourceDirectory)) {
      Iterator<Path> regularFiles = paths.filter(Files::isRegularFile).iterator();
      while (regularFiles.hasNext()) {
        Path path = regularFiles.next();
        Path relativePath = sourceDirectory.relativize(path);
        if (relativePath.getNameCount() > 1 && relativePath.getName(0).toString().startsWith("resources")) {
          Path classpathResourcePath = classesDirectory.resolve(relativePath);
          Files.createDirectories(classpathResourcePath.getParent());
          Files.copy(path, classpathResourcePath, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  public static class TestResource extends Resource {
    public TestResource(String fileName, String contentType, byte[] data) {
      super(fileName, contentType, data);
    }

    public TestResource(Class<?> resourceClass, String resourceName, String contentType) {
      super(resourceClass, resourceName, contentType);
    }

    private TestResource(UUID uuid) {
      super(uuid);
    }

    public static TestResource valueOf(String uuidText) {
      return new TestResource(UUID.fromString(uuidText));
    }
  }
}
