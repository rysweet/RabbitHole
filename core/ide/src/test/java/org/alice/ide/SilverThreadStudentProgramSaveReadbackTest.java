package org.alice.ide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.CountLoop;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LambdaExpression;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SBiped;
import org.lgna.story.SProgram;
import org.lgna.story.SScene;
import org.lgna.story.event.SceneActivationListener;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread Test A: a rich student-shaped AST with Scene type plus
 * {@code myFirstMethod} procedure (Comment + CountLoop), event listener
 * wiring ({@code addSceneActivationListener}), and a model field survives
 * production save via {@link IoUtilities#writeProject} and reopen via
 * {@link IoUtilities#readProject}.
 *
 * <p>Headless: no JavaFX, no 3D rendering, no gallery assets.
 *
 * @see SilverThreadStudentProgramCodegenTest
 * @see SilverThreadStudentProgramEventDispatchTest
 */
public class SilverThreadStudentProgramSaveReadbackTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // ── Test: full student AST survives write → read ─────────────────────

  @Test
  public void richStudentAstSurvivesSaveAndReopen() throws Exception {
    Project original = buildStudentProject();
    File projectFile = temporaryFolder.newFile("student.a3p");
    IoUtilities.writeProject(projectFile, original);

    assertTrue("saved file must exist", projectFile.isFile());
    assertTrue("saved file must be non-empty", projectFile.length() > 0);

    Project reopened = IoUtilities.readProject(projectFile);
    assertNotNull("reopened project must not be null", reopened);

    NamedUserType reopenedScene = findSceneType(reopened);
    assertNotNull("reopened project must contain a Scene type", reopenedScene);

    // Verify myFirstMethod with Comment + CountLoop
    UserMethod myFirstMethod = findMethodByName(reopenedScene, "myFirstMethod");
    assertNotNull("Scene must contain myFirstMethod", myFirstMethod);
    BlockStatement body = myFirstMethod.body.getValue();
    assertNotNull("myFirstMethod body must not be null", body);
    assertEquals("myFirstMethod should have 2 statements (Comment + CountLoop)",
        2, body.statements.size());

    Statement first = body.statements.get(0);
    assertTrue("first statement must be a Comment", first instanceof Comment);
    assertEquals("comment text must match",
        "student work goes here", ((Comment) first).text.getValue());

    Statement second = body.statements.get(1);
    assertTrue("second statement must be a CountLoop", second instanceof CountLoop);
    CountLoop loop = (CountLoop) second;
    assertNotNull("CountLoop body must not be null", loop.body.getValue());

    // Verify initializeEventListeners with addSceneActivationListener
    UserMethod initListeners = findMethodByName(reopenedScene, "initializeEventListeners");
    assertNotNull("Scene must contain initializeEventListeners", initListeners);
    BlockStatement listenerBody = initListeners.body.getValue();
    assertEquals("initializeEventListeners should have 1 statement",
        1, listenerBody.statements.size());
    Statement listenerStmt = listenerBody.statements.get(0);
    assertTrue("statement must be an ExpressionStatement",
        listenerStmt instanceof ExpressionStatement);
    ExpressionStatement exprStmt = (ExpressionStatement) listenerStmt;
    assertTrue("expression must be a MethodInvocation",
        exprStmt.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) exprStmt.expression.getValue();
    assertEquals("method name must be addSceneActivationListener",
        "addSceneActivationListener", invocation.method.getValue().getName());

    // Verify model field
    UserField modelField = findFieldByName(reopenedScene, "myBiped");
    assertNotNull("Scene must contain myBiped field", modelField);
    assertEquals("field type must be SBiped",
        JavaType.getInstance(SBiped.class), modelField.getValueType());
  }

  @Test
  public void sceneTypeSuperTypePreservedAcrossRoundTrip() throws Exception {
    Project original = buildStudentProject();
    File projectFile = temporaryFolder.newFile("supertype-check.a3p");
    IoUtilities.writeProject(projectFile, original);

    Project reopened = IoUtilities.readProject(projectFile);
    NamedUserType scene = findSceneType(reopened);
    assertEquals("Scene supertype must be SScene",
        JavaType.getInstance(SScene.class), scene.getSuperType());
  }

  @Test
  public void programTypeSuperTypePreservedAcrossRoundTrip() throws Exception {
    Project original = buildStudentProject();
    File projectFile = temporaryFolder.newFile("program-check.a3p");
    IoUtilities.writeProject(projectFile, original);

    Project reopened = IoUtilities.readProject(projectFile);
    NamedUserType programType = reopened.getProgramType();
    assertEquals("Program supertype must be SProgram",
        JavaType.getInstance(SProgram.class), programType.getSuperType());
  }

  @Test
  public void countLoopCountExpressionPreservedAcrossRoundTrip() throws Exception {
    Project original = buildStudentProject();
    File projectFile = temporaryFolder.newFile("countloop-check.a3p");
    IoUtilities.writeProject(projectFile, original);

    Project reopened = IoUtilities.readProject(projectFile);
    NamedUserType scene = findSceneType(reopened);
    UserMethod myFirstMethod = findMethodByName(scene, "myFirstMethod");
    CountLoop loop = (CountLoop) myFirstMethod.body.getValue().statements.get(1);
    assertTrue("count expression must be an IntegerLiteral",
        loop.count.getValue() instanceof IntegerLiteral);
    assertEquals("count value must be 3",
        Integer.valueOf(3), ((IntegerLiteral) loop.count.getValue()).value.getValue());
  }

  @Test
  public void lambdaExpressionInListenerSurvivesRoundTrip() throws Exception {
    Project original = buildStudentProject();
    File projectFile = temporaryFolder.newFile("lambda-check.a3p");
    IoUtilities.writeProject(projectFile, original);

    Project reopened = IoUtilities.readProject(projectFile);
    NamedUserType scene = findSceneType(reopened);
    UserMethod initListeners = findMethodByName(scene, "initializeEventListeners");
    ExpressionStatement stmt = (ExpressionStatement) initListeners.body.getValue().statements.get(0);
    MethodInvocation inv = (MethodInvocation) stmt.expression.getValue();
    assertTrue("listener argument must contain a LambdaExpression",
        inv.requiredArguments.get(0).expression.getValue() instanceof LambdaExpression);
  }

  @Test
  public void doubleWriteReadProducesIdenticalStructure() throws Exception {
    Project original = buildStudentProject();
    File file1 = temporaryFolder.newFile("pass1.a3p");
    IoUtilities.writeProject(file1, original);
    Project pass1 = IoUtilities.readProject(file1);

    File file2 = temporaryFolder.newFile("pass2.a3p");
    IoUtilities.writeProject(file2, pass1);
    Project pass2 = IoUtilities.readProject(file2);

    NamedUserType scene1 = findSceneType(pass1);
    NamedUserType scene2 = findSceneType(pass2);
    assertEquals("method count must match after double round-trip",
        scene1.getDeclaredMethods().size(), scene2.getDeclaredMethods().size());
    assertEquals("field count must match after double round-trip",
        scene1.getDeclaredFields().size(), scene2.getDeclaredFields().size());

    UserMethod m1 = findMethodByName(scene1, "myFirstMethod");
    UserMethod m2 = findMethodByName(scene2, "myFirstMethod");
    assertEquals("statement count in myFirstMethod must match",
        m1.body.getValue().statements.size(),
        m2.body.getValue().statements.size());
  }

  // ── AST construction ─────────────────────────────────────────────────

  static Project buildStudentProject() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));

    // myFirstMethod: Comment + CountLoop(3)
    Comment comment = new Comment("student work goes here");
    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(3));
    countLoop.body.getValue().statements.add(new Comment("loop body"));
    UserMethod myFirstMethod = new UserMethod(
        "myFirstMethod", JavaType.VOID_TYPE, new UserParameter[0],
        new BlockStatement(comment, countLoop));
    sceneType.methods.add(myFirstMethod);

    // initializeEventListeners: this.addSceneActivationListener(lambda)
    JavaMethod addListener = AstUtilities.lookupMethod(
        SScene.class, "addSceneActivationListener", SceneActivationListener.class);
    LambdaExpression lambda = AstUtilities.createLambdaExpression(SceneActivationListener.class);
    ExpressionStatement listenerStmt = AstUtilities.createMethodInvocationStatement(
        new ThisExpression(), addListener, lambda);
    UserMethod initListeners = new UserMethod(
        "initializeEventListeners", JavaType.VOID_TYPE, new UserParameter[0],
        new BlockStatement(listenerStmt));
    sceneType.methods.add(initListeners);

    // Model field: myBiped
    sceneType.fields.add(new UserField("myBiped", JavaType.getInstance(SBiped.class), new NullLiteral()));

    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  // ── Lookup helpers ───────────────────────────────────────────────────

  static NamedUserType findSceneType(Project project) {
    return (NamedUserType) project.getProgramType()
        .getDeclaredFields().get(0).getValueType();
  }

  static UserMethod findMethodByName(NamedUserType type, String name) {
    for (UserMethod method : type.getDeclaredMethods()) {
      if (name.equals(method.getName())) {
        return method;
      }
    }
    return null;
  }

  private static UserField findFieldByName(NamedUserType type, String name) {
    for (UserField field : type.getDeclaredFields()) {
      if (name.equals(field.getName())) {
        return field;
      }
    }
    return null;
  }
}
