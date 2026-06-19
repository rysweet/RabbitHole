package org.alice.tools;

import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import org.alice.ide.IdeTestWait;
import org.alice.ide.ast.ExpressionCreator;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.stageide.StageIDE;
import org.alice.stageide.StoryApiConfigurationManager;
import org.alice.stageide.ast.BootstrapUtilities;
import org.alice.stageide.program.RunProgramContext;
import org.alice.stageide.sceneeditor.SetUpMethodGenerator;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.virtualmachine.UserInstance;
import org.lgna.story.Color;
import org.lgna.story.Position;
import org.lgna.story.Resizable;
import org.lgna.story.SBiped;
import org.lgna.story.SGround;
import org.lgna.story.SScene;
import org.lgna.story.Scale;
import org.lgna.story.SetScale;
import org.lgna.story.resources.biped.BunnyResource;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class RabbitHoleDefaultOpenAssetWorkflowTest {
  private static final String BUNNY_URI = "alice-gallery://animals/bunny";
  private static final String BUNNY_FIELD_NAME = "bunny";
  private static final AffineMatrix4x4 BUNNY_TRANSFORM = AffineMatrix4x4.createTranslation(0.35, 0.0, 0.0);
  private static final Scale BUNNY_VISIBLE_SCALE = new Scale(2.0, 2.0, 2.0);
  private static final int RENDER_WIDTH = 640;
  private static final int RENDER_HEIGHT = 480;
  private static final long RENDER_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(20);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
  public void clearRunningState() {
    org.alice.ide.issue.UserProgramRunningStateUtilities.setUserProgramRunning(false);
  }

  @Test
  public void placesRendersManipulatesAndRoundTripsOpenBunnyWithoutSims() throws Exception {
    requireDisplayWhenContractIsMandatory();
    assertFalse("default open asset workflow must run with Sims disabled", Boolean.getBoolean("includeSims"));
    assertFalse("BunnyResource.DEFAULT must resolve to the open bundled asset, not Sims",
        BunnyResource.DEFAULT.getImplementationAndVisualFactory().isSims());
    useDistributionDirectoryIfAvailable();

    File starterProjectFile = temporaryFolder.newFile("starter-open-assets.a3p");
    Project starterProject = openStarterProject();
    IoUtilities.writeProject(starterProjectFile, starterProject);
    BufferedImage starterRender = renderProject(IoUtilities.readProject(starterProjectFile), null).image();

    Path evidenceDir = temporaryFolder.newFolder("bunny-placement-evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    int status = EatmePlaceObject.run(
        new String[] {
            "--project", starterProjectFile.getAbsolutePath(),
            "--object", BUNNY_URI,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    assertTrue(stdout.toString(StandardCharsets.UTF_8), stdout.toString(StandardCharsets.UTF_8).contains("\"status\":\"placed\""));

    Project placedProject = IoUtilities.readProject(evidenceDir.resolve("placed-project.a3p").toFile());
    UserField placedBunny = requireSceneField(placedProject, BUNNY_FIELD_NAME);
    assertEquals("SBiped", placedBunny.getValueType().getName());

    appendPersistentBunnyTransform(placedProject, placedBunny);
    File manipulatedProjectFile = temporaryFolder.newFile("manipulated-open-bunny.a3p");
    IoUtilities.writeProject(manipulatedProjectFile, placedProject);

    Project reopenedProject = IoUtilities.readProject(manipulatedProjectFile);
    UserField reopenedBunny = requireSceneField(reopenedProject, BUNNY_FIELD_NAME);
    assertEquals("round-tripped Bunny field should stay on the open asset type", "SBiped", reopenedBunny.getValueType().getName());

    RenderedProject bunnyRender = renderProject(reopenedProject, reopenedBunny);
    assertPositionEquals("round-tripped persistent 3D transform should execute in the Alice scene",
        BUNNY_TRANSFORM.translation().x(),
        BUNNY_TRANSFORM.translation().y(),
        BUNNY_TRANSFORM.translation().z(),
        bunnyRender.bunnyPosition());
    assertScaleEquals("round-tripped persistent 3D scale should execute in the Alice scene",
        BUNNY_VISIBLE_SCALE,
        bunnyRender.bunnyScale());
    assertImageHasVisibleContent("starter scene render should contain visible pixels", starterRender);
    assertImageHasVisibleContent("Bunny scene render should contain visible pixels", bunnyRender.image());
    assertImagesDiffer("Bunny render should visibly differ from the starter scene render", starterRender, bunnyRender.image());
  }

  private static void requireDisplayWhenContractIsMandatory() {
    boolean required = Boolean.getBoolean("rabbithole.defaultAssetWorkflow.required");
    boolean missingDisplay = GraphicsEnvironment.isHeadless()
        || System.getenv("DISPLAY") == null
        || System.getenv("DISPLAY").isBlank();
    if (required && missingDisplay) {
      fail("RabbitHole default open asset workflow requires Xvfb or another usable display");
    }
    assumeFalse("RabbitHole default open asset workflow requires Xvfb or another usable display", missingDisplay);
  }

  private static Project openStarterProject() {
    NamedUserType programType = BootstrapUtilities.createProgramType(
        SGround.SurfaceAppearance.GRASS,
        new Color(150 / 255.0, 226 / 255.0, 252 / 255.0),
        Double.NaN,
        Color.WHITE,
        Color.WHITE,
        1.0,
        false);
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static void useDistributionDirectoryIfAvailable() {
    String configuredRoot = System.getProperty("org.alice.ide.rootDirectory");
    if (configuredRoot != null && Files.isDirectory(Paths.get(configuredRoot))) {
      return;
    }
    Path distributionDir = Paths.get(System.getProperty("user.dir"))
        .resolve("../../core/resources/target/distribution")
        .normalize();
    if (Files.isDirectory(distributionDir)) {
      System.setProperty("org.alice.ide.rootDirectory", distributionDir.toString());
    }
  }

  private static void appendPersistentBunnyTransform(Project project, UserField bunnyField)
      throws ExpressionCreator.CannotCreateExpressionException {
    UserMethod setupMethod = requireSceneMethod(project, StageIDE.PERFORM_GENERATED_SET_UP_METHOD_NAME);
    Statement[] setupStatements = SetUpMethodGenerator.getSetupStatementsForField(
        false,
        bunnyField,
        null,
        null,
        BUNNY_TRANSFORM);
    setupMethod.body.getValue().statements.add(setupStatements);
    setupMethod.body.getValue().statements.add(AstUtilities.createMethodInvocationStatement(
        new FieldAccess(bunnyField),
        JavaMethod.getInstance(Resizable.class, "setScale", Scale.class, SetScale.Detail[].class),
        StoryApiConfigurationManager.getInstance().getExpressionCreator().createExpression(BUNNY_VISIBLE_SCALE)));
    assertTrue("Bunny manipulation must be persisted as generated setup statements", setupStatements.length >= 3);
  }

  private static UserMethod requireSceneMethod(Project project, String methodName) {
    return sceneType(project).getDeclaredMethods().stream()
        .filter(method -> methodName.equals(method.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("scene method not found: " + methodName));
  }

  private static UserField requireSceneField(Project project, String fieldName) {
    return sceneType(project).getDeclaredFields().stream()
        .filter(field -> fieldName.equals(field.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("scene field not found: " + fieldName));
  }

  private static NamedUserType sceneType(Project project) {
    return project.getProgramType().getDeclaredFields().stream()
        .filter(field -> field.getValueType().isAssignableTo(SScene.class))
        .map(field -> (NamedUserType) field.getValueType())
        .findFirst()
        .orElseThrow(() -> new AssertionError("project does not contain a scene field"));
  }

  private static RenderedProject renderProject(Project project, UserField bunnyField) throws Exception {
    RunProgramContext context = new RunProgramContext(project.getProgramType());
    JFrame frame = new JFrame("RabbitHole default open asset workflow");
    try {
      initialize(context, frame);
      context.setActiveScene();
      Position bunnyPosition = null;
      Scale bunnyScale = null;
      if (bunnyField != null) {
        UserField sceneField = project.getProgramType().getDeclaredFields().stream()
            .filter(field -> field.getValueType().isAssignableTo(SScene.class))
            .findFirst()
            .orElseThrow(() -> new AssertionError("project does not contain a scene field"));
        UserInstance sceneInstance = (UserInstance) context.getProgramInstance().getFieldValue(sceneField);
        SBiped bunny = sceneInstance.getFieldValueInstanceInJava(bunnyField, SBiped.class);
        assertNotNull("runtime scene should instantiate the round-tripped Bunny", bunny);
        bunnyPosition = bunny.getPositionRelativeToVehicle();
        bunnyScale = bunny.getScale();
      }
      return new RenderedProject(captureColorBuffer(context.getOnscreenRenderTarget()), bunnyPosition, bunnyScale);
    } finally {
      context.cleanUpProgram();
      SwingUtilities.invokeAndWait(frame::dispose);
    }
  }

  private static void initialize(RunProgramContext context, JFrame frame) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      frame.setLayout(new BorderLayout());
      frame.setSize(RENDER_WIDTH, RENDER_HEIGHT);
      context.initializeInContainer((onscreenRenderTarget, controlPanel) -> {
        if (controlPanel != null) {
          frame.add(controlPanel, BorderLayout.PAGE_START);
        }
        frame.add(onscreenRenderTarget.getAwtComponent(), BorderLayout.CENTER);
      });
      frame.setVisible(true);
      frame.validate();
    });
  }

  private static BufferedImage captureColorBuffer(OnscreenRenderTarget target) throws Exception {
    assertNotNull("run context should create an onscreen render target", target);
    long deadline = System.nanoTime() + RENDER_TIMEOUT_NANOS;
    while (System.nanoTime() < deadline) {
      SwingUtilities.invokeAndWait(() -> {
        target.getAwtComponent().setSize(RENDER_WIDTH, RENDER_HEIGHT);
        target.getAwtComponent().validate();
        target.repaint();
      });
      BufferedImage image = target.getSynchronousImageCapturer().getColorBuffer();
      if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
        return image;
      }
      IdeTestWait.sleepForSemanticTime(50, TimeUnit.MILLISECONDS, "render target color buffer");
    }
    fail("render target did not produce a color buffer before timeout");
    return null;
  }

  private static void assertPositionEquals(String message, double right, double up, double backward, Position actual) {
    assertNotNull(message, actual);
    assertEquals(message + " (right)", right, actual.getRight(), 0.0001);
    assertEquals(message + " (up)", up, actual.getUp(), 0.0001);
    assertEquals(message + " (backward)", backward, actual.getBackward(), 0.0001);
  }

  private static void assertScaleEquals(String message, Scale expected, Scale actual) {
    assertNotNull(message, actual);
    assertEquals(message + " (leftToRight)", expected.getLeftToRight(), actual.getLeftToRight(), 0.0001);
    assertEquals(message + " (bottomToTop)", expected.getBottomToTop(), actual.getBottomToTop(), 0.0001);
    assertEquals(message + " (frontToBack)", expected.getFrontToBack(), actual.getFrontToBack(), 0.0001);
  }

  private static void assertImageHasVisibleContent(String message, BufferedImage image) {
    assertNotNull(message, image);
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;
    int sampled = 0;
    for (int y = 0; y < image.getHeight(); y += 8) {
      for (int x = 0; x < image.getWidth(); x += 8) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        long luminance = (red * 299L) + (green * 587L) + (blue * 114L);
        min = Math.min(min, luminance);
        max = Math.max(max, luminance);
        sampled++;
      }
    }
    assertTrue(message + " should sample pixels", sampled > 0);
    assertTrue(message + " should have non-background luminance variance", max - min > 10_000);
  }

  private static void assertImagesDiffer(String message, BufferedImage expectedDifferentFrom, BufferedImage actual) {
    int width = Math.min(expectedDifferentFrom.getWidth(), actual.getWidth());
    int height = Math.min(expectedDifferentFrom.getHeight(), actual.getHeight());
    long totalDelta = 0;
    int changedPixels = 0;
    int sampled = 0;
    for (int y = 0; y < height; y += 4) {
      for (int x = 0; x < width; x += 4) {
        int first = expectedDifferentFrom.getRGB(x, y);
        int second = actual.getRGB(x, y);
        int delta = Math.abs(((first >> 16) & 0xff) - ((second >> 16) & 0xff))
            + Math.abs(((first >> 8) & 0xff) - ((second >> 8) & 0xff))
            + Math.abs((first & 0xff) - (second & 0xff));
        totalDelta += delta;
        if (delta > 12) {
          changedPixels++;
        }
        sampled++;
      }
    }
    assertTrue(message + " should sample comparable pixels", sampled > 0);
    double changedPortion = changedPixels / (double) sampled;
    double averageDelta = totalDelta / (double) sampled;
    assertTrue(
        message + " should change a visible area; changedPixels=" + changedPixels
            + ", sampled=" + sampled + ", changedPortion=" + changedPortion
            + ", averageDelta=" + averageDelta,
        changedPixels > 20 && changedPortion > 0.001);
    assertTrue(
        message + " should have meaningful average pixel delta; averageDelta=" + averageDelta
            + ", changedPixels=" + changedPixels + ", sampled=" + sampled,
        averageDelta > 0.1);
  }

  private record RenderedProject(BufferedImage image, Position bunnyPosition, Scale bunnyScale) {
  }
}
