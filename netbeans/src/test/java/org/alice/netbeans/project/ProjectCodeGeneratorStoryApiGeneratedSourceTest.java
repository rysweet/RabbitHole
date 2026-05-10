package org.alice.netbeans.project;

import edu.cmu.cs.dennisc.animation.Animator;
import edu.cmu.cs.dennisc.animation.ClockBasedAnimator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LambdaExpression;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.TypeExpression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLambda;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.AddTimeListener;
import org.lgna.story.Color;
import org.lgna.story.Paint;
import org.lgna.story.SBox;
import org.lgna.story.SModel;
import org.lgna.story.SScene;
import org.lgna.story.SProgram;
import org.lgna.story.Say;
import org.lgna.story.SetAtmosphereColor;
import org.lgna.story.SetFogDensity;
import org.lgna.story.SetOpacity;
import org.lgna.story.SetPaint;
import org.lgna.story.event.SceneActivationEvent;
import org.lgna.story.event.SceneActivationListener;
import org.lgna.story.event.TimeEvent;
import org.lgna.story.event.TimeListener;
import org.lgna.story.implementation.ProgramImp;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.Assert.*;

public class ProjectCodeGeneratorStoryApiGeneratedSourceTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedSyntheticStoryApiCallSourceCompiles() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-story-api-call.a3p",
        programTypeWithStoryApiCall(),
        "generated-story-api-call-src");

    Path programPath = sourceDirectory.resolve("Program.java");
    String programSource = Files.readString(programPath);
    assertTrue(programSource.contains("void configureStory()"));
    assertTrue(programSource, programSource.contains("this.setSimulationSpeedFactor(1.5);"));
    compileProgramAndLauncher("generated-story-api-call-classes", programPath, sourceDirectory);
  }

  @Test
  public void generatedStoryApiSimulationSpeedCallUpdatesRuntimeStateHeadlessly() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-story-api-speed-runtime.a3p",
        programTypeWithStoryApiCall(),
        "generated-story-api-speed-runtime-src");

    Path programPath = sourceDirectory.resolve("Program.java");
    Path classesDirectory = temporaryFolder.newFolder("generated-story-api-speed-runtime-classes").toPath();
    compileJavaSources(classesDirectory, programPath);
    try (GeneratedProgramClassLoader classLoader = new GeneratedProgramClassLoader(
        new URL[] {classesDirectory.toUri().toURL()})) {
      Class<?> programClass = Class.forName("Program", true, classLoader);
      var constructor = programClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      ProgramImp.ACCEPTABLE_HACK_FOR_NOW_setClassForNextInstance(HeadlessProgramImp.class);
      SProgram program = (SProgram) constructor.newInstance();

      // Direct configureStory invocation characterizes generated runtime state without launching rendering.
      var configureStory = programClass.getDeclaredMethod("configureStory");
      configureStory.setAccessible(true);
      configureStory.invoke(program);

      assertEquals(1.5, program.getSimulationSpeedFactor(), 0.0);
    }
  }

  public static class HeadlessProgramImp extends ProgramImp {
    private final ClockBasedAnimator animator = new ClockBasedAnimator();

    public HeadlessProgramImp(SProgram abstraction) {
      super(abstraction, null);
    }

    @Override
    public Animator getAnimator() {
      return this.animator;
    }
  }

  @Test
  public void generatedSyntheticSceneActivationCallSourceCompiles() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-scene-activation-call.a3p",
        programTypeWithSceneActivationCall(),
        "generated-scene-activation-call-src");

    Path programPath = sourceDirectory.resolve("Program.java");
    String programSource = Files.readString(programPath);
    assertTrue(programSource.contains("void clearScene()"));
    assertTrue(programSource, programSource.contains("this.setActiveScene(null);"));
    compileProgramAndLauncher("generated-scene-activation-call-classes", programPath, sourceDirectory);
  }

  @Test
  public void generatedSyntheticSceneModelEventAndRenderingAdjacentSourcesCompile() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-scene-model-event-rendering-call.a3p",
        programTypeWithSceneModelEventAndRenderingCalls(),
        "generated-scene-model-event-rendering-call-src");

    Path programPath = sourceDirectory.resolve("Program.java");
    Path scenePath = sourceDirectory.resolve("Scene.java");
    String programSource = Files.readString(programPath);
    String sceneSource = Files.readString(scenePath);
    assertTrue(programSource, programSource.contains("this.setActiveScene(this.scene);"));
    assertTrue(programSource, programSource.contains("this.box.setPaint(Color.RED);"));
    assertTrue(programSource, programSource.contains("this.box.setOpacity(0.5);"));
    assertTrue(programSource, programSource.contains("this.box.say(\"hello box\");"));
    assertTrue(sceneSource, sceneSource.contains("this.setAtmosphereColor(Color.BLUE);"));
    assertTrue(sceneSource, sceneSource.contains("this.setFogDensity(0.25);"));
    assertTrue(sceneSource, sceneSource.contains("this.addTimeListener(null,1);"));
    assertTrue(sceneSource, sceneSource.contains("this.addSceneActivationListener(null);"));
    compileAllGeneratedSources("generated-scene-model-event-rendering-call-classes", sourceDirectory);
  }

  @Test
  public void generatedSyntheticSceneListenerRegistrationSourceCompiles() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-scene-listener-registration-call.a3p",
        programTypeWithSceneListenerRegistrationCalls(),
        "generated-scene-listener-registration-call-src");

    Path scenePath = sourceDirectory.resolve("Scene.java");
    String sceneSource = Files.readString(scenePath);
    assertTrue(sceneSource, sceneSource.contains("this.addTimeListener(null,2);"));
    assertTrue(sceneSource, sceneSource.contains("this.addSceneActivationListener(null);"));
    compileAllGeneratedSources("generated-scene-listener-registration-call-classes", sourceDirectory);
  }

  @Test
  public void generatedSceneActivationListenerParticipatesInHeadlessRuntimeDispatch() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-scene-activation-listener-runtime-dispatch.a3p",
        programTypeWithExecutableSceneActivationRuntimeDispatchProbe(),
        "generated-scene-activation-listener-runtime-dispatch-src");

    Path scenePath = sourceDirectory.resolve("Scene.java");
    String sceneSource = Files.readString(scenePath);
    assertTrue(sceneSource, sceneSource.contains("public void handleActiveChanged(Boolean isActive,Integer activationCount)"));
    assertTrue(sceneSource, sceneSource.contains("this.addSceneActivationListener((SceneActivationEvent p0) ->"));
    assertTrue(sceneSource, sceneSource.contains(
        "ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationRuntimeDispatch(p0);"));

    Path classesDirectory = compileAllGeneratedSources(
        "generated-scene-activation-listener-runtime-dispatch-classes",
        sourceDirectory);
    sceneActivationRuntimeDispatchLatch = new CountDownLatch(1);
    sceneActivationRuntimeDispatchCount = new AtomicInteger();
    recordedRuntimeSceneActivationEvent = null;
    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] {classesDirectory.toUri().toURL()},
        Thread.currentThread().getContextClassLoader())) {
      Object scene = instantiateGeneratedScene(classLoader);
      assertEquals("Generated listener registration should not fire before runtime scene activation",
          0,
          sceneActivationRuntimeDispatchCount.get());
      assertNull("Generated listener registration should not synthesize a runtime event payload",
          recordedRuntimeSceneActivationEvent);
      assertEquals("Generated listener should still be waiting for the runtime dispatch",
          1,
          sceneActivationRuntimeDispatchLatch.getCount());
      Object eventManager = eventManagerFor(scene);
      eventManager.getClass().getMethod("sceneActivated").invoke(eventManager);
      assertTrue("Generated scene activation listener should run when the runtime event fires",
          sceneActivationRuntimeDispatchLatch.await(5, TimeUnit.SECONDS));
      assertEquals("Generated scene activation listener should fire once for one runtime scene activation",
          1,
          sceneActivationRuntimeDispatchCount.get());
      assertNotNull("Generated listener should receive a SceneActivationEvent payload from runtime dispatch",
          recordedRuntimeSceneActivationEvent);
      assertSame("Generated listener should receive the runtime scene activation event payload type",
          SceneActivationEvent.class,
          recordedRuntimeSceneActivationEvent.getClass());
    } finally {
      recordedRuntimeSceneActivationEvent = null;
      sceneActivationRuntimeDispatchCount = null;
      sceneActivationRuntimeDispatchLatch = null;
    }
  }

  @Test
  public void generatedSyntheticTimeListenerDispatchesThroughTimerHandlerSeamHeadless() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-time-listener-runtime.a3p",
        programTypeWithExecutableTimeListenerRegistration(),
        "generated-time-listener-runtime-src");

    Path scenePath = sourceDirectory.resolve("Scene.java");
    String sceneSource = Files.readString(scenePath);
    assertTrue(sceneSource, sceneSource.contains("public void handleActiveChanged(Boolean isActive,Integer activationCount)"));
    assertTrue(sceneSource, sceneSource.contains("this.addTimeListener((TimeEvent p0) ->"));
    assertTrue(sceneSource, sceneSource.contains("ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordTimeEvent();"));

    Path classesDirectory = compileAllGeneratedSources(
        "generated-time-listener-runtime-classes",
        sourceDirectory);
    timeEventLatch = new CountDownLatch(1);
    Object timer = null;
    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] {classesDirectory.toUri().toURL()},
        Thread.currentThread().getContextClassLoader())) {
      Object scene = instantiateGeneratedScene(classLoader);
      timer = timerFor(scene);
      activateAndUpdateTimer(timer, 2.0);
      assertTrue("Generated time listener should run when the timer handler seam updates",
          timeEventLatch.await(5, TimeUnit.SECONDS));
    } finally {
      disableTimer(timer);
    }
  }

  @Test
  public void generatedSyntheticTimeListenerReceivesElapsedTimePayloadHeadless() throws Exception {
    Path sourceDirectory = generateProgramSource(
        "synthetic-time-listener-payload-runtime.a3p",
        programTypeWithExecutableTimeListenerElapsedProbe(),
        "generated-time-listener-payload-runtime-src");

    Path scenePath = sourceDirectory.resolve("Scene.java");
    String sceneSource = Files.readString(scenePath);
    assertTrue(sceneSource, sceneSource.contains("this.addTimeListener((TimeEvent p0) ->"));
    assertTrue(sceneSource, sceneSource.contains(
        "ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordTimeEventElapsed(p0.getTimeSinceLastFire());"));

    Path classesDirectory = compileAllGeneratedSources(
        "generated-time-listener-payload-runtime-classes",
        sourceDirectory);
    timeEventElapsedLatch = new CountDownLatch(1);
    recordedTimeSinceLastFire = null;
    Object timer = null;
    try (URLClassLoader classLoader = new URLClassLoader(
        new URL[] {classesDirectory.toUri().toURL()},
        Thread.currentThread().getContextClassLoader())) {
      Object scene = instantiateGeneratedScene(classLoader);
      timer = timerFor(scene);
      activateAndUpdateTimer(timer, 2.0);
      assertTrue("Generated time listener should receive elapsed time from the runtime timer event",
          timeEventElapsedLatch.await(5, TimeUnit.SECONDS));
      assertEquals(2.0, recordedTimeSinceLastFire, 0.0);
    } finally {
      disableTimer(timer);
      recordedTimeSinceLastFire = null;
      timeEventElapsedLatch = null;
    }
  }

  private static Object instantiateGeneratedScene(ClassLoader classLoader) throws Exception {
    Class<?> sceneClass = Class.forName("Scene", true, classLoader);
    var constructor = sceneClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object scene = constructor.newInstance();
    var handleActiveChanged = sceneClass.getDeclaredMethod("handleActiveChanged", Boolean.class, Integer.class);
    handleActiveChanged.setAccessible(true);
    handleActiveChanged.invoke(scene, Boolean.TRUE, 1);
    return scene;
  }

  private static Object timerFor(Object scene) throws Exception {
    Object eventManager = eventManagerFor(scene);
    var timerField = eventManager.getClass().getDeclaredField("timer");
    timerField.setAccessible(true);
    return timerField.get(eventManager);
  }

  private static Object eventManagerFor(Object scene) throws Exception {
    Object sceneImplementation = scene.getClass().getMethod("getImplementation").invoke(scene);
    return sceneImplementation.getClass().getMethod("getEventManager").invoke(sceneImplementation);
  }

  private static void activateAndUpdateTimer(Object timer, double currentTime) throws Exception {
    timer.getClass().getMethod("sceneActivated", SceneActivationEvent.class).invoke(timer, new SceneActivationEvent());
    var currentTimeField = timer.getClass().getDeclaredField("currentTime");
    currentTimeField.setAccessible(true);
    currentTimeField.set(timer, currentTime);
    var update = timer.getClass().getDeclaredMethod("update");
    update.setAccessible(true);
    update.invoke(timer);
  }

  private static void disableTimer(Object timer) throws Exception {
    if (timer != null) {
      timer.getClass().getMethod("disable").invoke(timer);
    }
  }

  public static void recordSceneActivationRuntimeDispatch(SceneActivationEvent event) {
    recordedRuntimeSceneActivationEvent = event;
    sceneActivationRuntimeDispatchCount.incrementAndGet();
    sceneActivationRuntimeDispatchLatch.countDown();
  }

  public static void recordTimeEvent() {
    timeEventLatch.countDown();
  }

  public static void recordTimeEventElapsed(Double timeSinceLastFire) {
    recordedTimeSinceLastFire = timeSinceLastFire;
    timeEventElapsedLatch.countDown();
  }

  private static CountDownLatch sceneActivationRuntimeDispatchLatch;
  private static CountDownLatch timeEventLatch;
  private static CountDownLatch timeEventElapsedLatch;
  private static volatile SceneActivationEvent recordedRuntimeSceneActivationEvent;
  private static AtomicInteger sceneActivationRuntimeDispatchCount;
  private static volatile Double recordedTimeSinceLastFire;

  private Path generateProgramSource(String projectFileName, NamedUserType programType, String sourceDirectoryName)
      throws Exception {
    File aliceProject = temporaryFolder.newFile(projectFileName);
    IoUtilities.writeProject(aliceProject, new Project(programType, Project.SceneCameraType.WindowCamera));
    File sourceDirectory = temporaryFolder.newFolder(sourceDirectoryName);
    ProjectCodeGenerator.generateCode(aliceProject, sourceDirectory, null, false);
    return sourceDirectory.toPath();
  }

  private void compileProgramAndLauncher(String classesDirectoryName, Path programPath, Path sourceDirectory)
      throws Exception {
    compileJavaSources(
        temporaryFolder.newFolder(classesDirectoryName).toPath(),
        programPath,
        sourceDirectory.resolve("AliceJavaFXLauncher.java"));
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    type.methods.add(mainMethod());
    return type;
  }

  private static NamedUserType programTypeWithStoryApiCall() {
    NamedUserType type = programType("Program");
    JavaMethod setSimulationSpeedFactor =
        AstUtilities.lookupMethod(SProgram.class, "setSimulationSpeedFactor", Number.class);
    UserMethod configureStory = new UserMethod(
        "configureStory",
        Void.TYPE,
        new UserParameter[0],
        new BlockStatement(AstUtilities.createMethodInvocationStatement(
            new ThisExpression(),
            setSimulationSpeedFactor,
            new DoubleLiteral(1.5))));
    type.methods.add(configureStory);
    return type;
  }

  private static NamedUserType programTypeWithSceneActivationCall() {
    NamedUserType type = programType("Program");
    JavaMethod setActiveScene = AstUtilities.lookupMethod(SProgram.class, "setActiveScene", SScene.class);
    UserMethod clearScene = new UserMethod(
        "clearScene",
        Void.TYPE,
        new UserParameter[0],
        new BlockStatement(AstUtilities.createMethodInvocationStatement(
            new ThisExpression(),
            setActiveScene,
            new NullLiteral())));
    type.methods.add(clearScene);
    return type;
  }

  private static NamedUserType programTypeWithSceneModelEventAndRenderingCalls() {
    NamedUserType type = programType("Program");
    NamedUserType sceneType = sceneTypeWithEventAndRenderingCalls();
    UserField scene = new UserField("scene", sceneType);
    UserField box = new UserField("box", SBox.class);
    type.fields.add(scene);
    type.fields.add(box);

    JavaMethod setActiveScene = AstUtilities.lookupMethod(SProgram.class, "setActiveScene", SScene.class);
    JavaMethod setPaint = AstUtilities.lookupMethod(SModel.class, "setPaint", Paint.class, SetPaint.Detail[].class);
    JavaMethod setOpacity = AstUtilities.lookupMethod(SModel.class, "setOpacity", Number.class, SetOpacity.Detail[].class);
    JavaMethod say = AstUtilities.lookupMethod(SModel.class, "say", String.class, Say.Detail[].class);
    UserMethod configureWorld = new UserMethod(
        "configureWorld",
        Void.TYPE,
        new UserParameter[0],
        new BlockStatement(
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                setActiveScene,
                new FieldAccess(new ThisExpression(), scene)),
            AstUtilities.createMethodInvocationStatement(
                new FieldAccess(new ThisExpression(), box),
                setPaint,
                AstUtilities.createStaticFieldAccess(Color.class, "RED")),
            AstUtilities.createMethodInvocationStatement(
                new FieldAccess(new ThisExpression(), box),
                setOpacity,
                new DoubleLiteral(0.5)),
            AstUtilities.createMethodInvocationStatement(
                new FieldAccess(new ThisExpression(), box),
                say,
                new org.lgna.project.ast.StringLiteral("hello box"))));
    type.methods.add(configureWorld);
    return type;
  }

  private static NamedUserType programTypeWithSceneListenerRegistrationCalls() {
    NamedUserType type = programType("Program");
    NamedUserType sceneType = sceneTypeWithListenerRegistrationCalls();
    type.fields.add(new UserField("scene", sceneType));
    return type;
  }

  private static NamedUserType programTypeWithExecutableSceneActivationRuntimeDispatchProbe() {
    NamedUserType type = programType("Program");
    NamedUserType sceneType = sceneTypeWithExecutableSceneActivationRuntimeDispatchProbe();
    type.fields.add(new UserField("scene", sceneType));
    return type;
  }

  private static NamedUserType programTypeWithExecutableTimeListenerRegistration() {
    NamedUserType type = programType("Program");
    NamedUserType sceneType = sceneTypeWithExecutableTimeListenerRegistration();
    type.fields.add(new UserField("scene", sceneType));
    return type;
  }

  private static NamedUserType programTypeWithExecutableTimeListenerElapsedProbe() {
    NamedUserType type = programType("Program");
    NamedUserType sceneType = sceneTypeWithExecutableTimeListenerElapsedProbe();
    type.fields.add(new UserField("scene", sceneType));
    return type;
  }

  private static NamedUserType sceneTypeWithListenerRegistrationCalls() {
    NamedUserType type = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    JavaMethod addTimeListener = AstUtilities.lookupMethod(
        SScene.class,
        "addTimeListener",
        TimeListener.class,
        Number.class,
        AddTimeListener.Detail[].class);
    JavaMethod addSceneActivationListener = AstUtilities.lookupMethod(
        SScene.class,
        "addSceneActivationListener",
        SceneActivationListener.class);
    UserMethod handleActiveChanged = new UserMethod(
        "handleActiveChanged",
        Void.TYPE,
        new UserParameter[] {
            new UserParameter("isActive", Boolean.class),
            new UserParameter("activationCount", Integer.class)
        },
        new BlockStatement(
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                addTimeListener,
                new NullLiteral(),
                new IntegerLiteral(2)),
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                addSceneActivationListener,
                new NullLiteral())));
    type.methods.add(handleActiveChanged);
    return type;
  }

  private static NamedUserType sceneTypeWithExecutableSceneActivationRuntimeDispatchProbe() {
    NamedUserType type = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    JavaMethod addSceneActivationListener = AstUtilities.lookupMethod(
        SScene.class,
        "addSceneActivationListener",
        SceneActivationListener.class);
    UserMethod handleActiveChanged = new UserMethod(
        "handleActiveChanged",
        Void.TYPE,
        new UserParameter[] {
            new UserParameter("isActive", Boolean.class),
            new UserParameter("activationCount", Integer.class)
        },
        new BlockStatement(AstUtilities.createMethodInvocationStatement(
            new ThisExpression(),
            addSceneActivationListener,
            sceneActivationRuntimeDispatchListenerLambda())));
    type.methods.add(handleActiveChanged);
    return type;
  }

  private static NamedUserType sceneTypeWithExecutableTimeListenerRegistration() {
    NamedUserType type = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    JavaMethod addTimeListener = AstUtilities.lookupMethod(
        SScene.class,
        "addTimeListener",
        TimeListener.class,
        Number.class,
        AddTimeListener.Detail[].class);
    UserMethod handleActiveChanged = new UserMethod(
        "handleActiveChanged",
        Void.TYPE,
        new UserParameter[] {
            new UserParameter("isActive", Boolean.class),
            new UserParameter("activationCount", Integer.class)
        },
        new BlockStatement(AstUtilities.createMethodInvocationStatement(
            new ThisExpression(),
            addTimeListener,
            listenerLambda(
                TimeListener.class,
                "recordTimeEvent",
                "time listener registered"),
            new IntegerLiteral(1))));
    type.methods.add(handleActiveChanged);
    return type;
  }

  private static NamedUserType sceneTypeWithExecutableTimeListenerElapsedProbe() {
    NamedUserType type = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    JavaMethod addTimeListener = AstUtilities.lookupMethod(
        SScene.class,
        "addTimeListener",
        TimeListener.class,
        Number.class,
        AddTimeListener.Detail[].class);
    UserMethod handleActiveChanged = new UserMethod(
        "handleActiveChanged",
        Void.TYPE,
        new UserParameter[] {
            new UserParameter("isActive", Boolean.class),
            new UserParameter("activationCount", Integer.class)
        },
        new BlockStatement(AstUtilities.createMethodInvocationStatement(
            new ThisExpression(),
            addTimeListener,
            elapsedTimeListenerLambda(),
            new IntegerLiteral(1))));
    type.methods.add(handleActiveChanged);
    return type;
  }

  private static LambdaExpression listenerLambda(Class<?> listenerClass, String recordMethodName, String commentText) {
    LambdaExpression expression = AstUtilities.createLambdaExpression(listenerClass);
    UserLambda lambda = (UserLambda) expression.value.getValue();
    JavaMethod recordEvent = AstUtilities.lookupMethod(
        ProjectCodeGeneratorStoryApiGeneratedSourceTest.class,
        recordMethodName);
    lambda.body.getValue().statements.add(new Comment(commentText));
    lambda.body.getValue().statements.add(AstUtilities.createMethodInvocationStatement(
        new org.lgna.project.ast.TypeExpression(recordEvent.getDeclaringType()),
        recordEvent));
    return expression;
  }

  private static LambdaExpression sceneActivationRuntimeDispatchListenerLambda() {
    LambdaExpression expression = AstUtilities.createLambdaExpression(SceneActivationListener.class);
    UserLambda lambda = (UserLambda) expression.value.getValue();
    UserParameter eventParameter = lambda.requiredParameters.get(0);
    JavaMethod recordEvent = AstUtilities.lookupMethod(
        ProjectCodeGeneratorStoryApiGeneratedSourceTest.class,
        "recordSceneActivationRuntimeDispatch",
        SceneActivationEvent.class);
    lambda.body.getValue().statements.add(AstUtilities.createMethodInvocationStatement(
        new TypeExpression(recordEvent.getDeclaringType()),
        recordEvent,
        new ParameterAccess(eventParameter)));
    return expression;
  }

  private static LambdaExpression elapsedTimeListenerLambda() {
    LambdaExpression expression = AstUtilities.createLambdaExpression(TimeListener.class);
    UserLambda lambda = (UserLambda) expression.value.getValue();
    UserParameter eventParameter = lambda.requiredParameters.get(0);
    JavaMethod timeSinceLastFire = AstUtilities.lookupMethod(TimeEvent.class, "getTimeSinceLastFire");
    JavaMethod recordEvent = AstUtilities.lookupMethod(
        ProjectCodeGeneratorStoryApiGeneratedSourceTest.class,
        "recordTimeEventElapsed",
        Double.class);
    lambda.body.getValue().statements.add(AstUtilities.createMethodInvocationStatement(
        new TypeExpression(recordEvent.getDeclaringType()),
        recordEvent,
        AstUtilities.createMethodInvocation(
            new ParameterAccess(eventParameter),
            timeSinceLastFire)));
    return expression;
  }

  private static NamedUserType sceneTypeWithEventAndRenderingCalls() {
    NamedUserType type = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    JavaMethod setAtmosphereColor = AstUtilities.lookupMethod(
        SScene.class,
        "setAtmosphereColor",
        Color.class,
        SetAtmosphereColor.Detail[].class);
    JavaMethod setFogDensity = AstUtilities.lookupMethod(
        SScene.class,
        "setFogDensity",
        Number.class,
        SetFogDensity.Detail[].class);
    JavaMethod addTimeListener = AstUtilities.lookupMethod(
        SScene.class,
        "addTimeListener",
        TimeListener.class,
        Number.class,
        AddTimeListener.Detail[].class);
    JavaMethod addSceneActivationListener = AstUtilities.lookupMethod(
        SScene.class,
        "addSceneActivationListener",
        SceneActivationListener.class);
    UserMethod handleActiveChanged = new UserMethod(
        "handleActiveChanged",
        Void.TYPE,
        new UserParameter[] {
            new UserParameter("isActive", Boolean.class),
            new UserParameter("activationCount", Integer.class)
        },
        new BlockStatement(
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                setAtmosphereColor,
                AstUtilities.createStaticFieldAccess(Color.class, "BLUE")),
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                setFogDensity,
                new DoubleLiteral(0.25)),
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                addTimeListener,
                new NullLiteral(),
                new IntegerLiteral(1)),
            AstUtilities.createMethodInvocationStatement(
                new ThisExpression(),
                addSceneActivationListener,
                new NullLiteral())));
    type.methods.add(handleActiveChanged);
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

  private Path compileAllGeneratedSources(String classesDirectoryName, Path sourceDirectory) throws Exception {
    List<Path> sources;
    try (var stream = Files.list(sourceDirectory)) {
      sources = stream
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .sorted()
          .toList();
    }
    Path classesDirectory = temporaryFolder.newFolder(classesDirectoryName).toPath();
    compileJavaSources(classesDirectory, sources.toArray(Path[]::new));
    return classesDirectory;
  }

  private static class GeneratedProgramClassLoader extends URLClassLoader {
    GeneratedProgramClassLoader(URL[] urls) {
      super(urls, ProjectCodeGeneratorStoryApiGeneratedSourceTest.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if ("Program".equals(name)) {
        synchronized (getClassLoadingLock(name)) {
          Class<?> loadedClass = findLoadedClass(name);
          if (loadedClass == null) {
            loadedClass = findClass(name);
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
}
