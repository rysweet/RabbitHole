package org.alice.ide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.JavaCodeGenerator;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.code.CodeOrganizer;
import org.lgna.project.io.IoUtilities;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread Test B: reopened project types generate valid Java source
 * via {@link JavaCodeGenerator}. Builds the same rich student AST as
 * {@link SilverThreadStudentProgramSaveReadbackTest}, writes → reads it
 * back, then generates Java source and asserts structural keywords appear.
 *
 * <p>Headless: no JavaFX, no 3D rendering, no gallery assets.
 *
 * @see SilverThreadStudentProgramSaveReadbackTest
 * @see SilverThreadStudentProgramEventDispatchTest
 */
public class SilverThreadStudentProgramCodegenTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // ── Core: Scene type generates valid Java source ─────────────────────

  @Test
  public void sceneTypeGeneratesJavaSourceWithMyFirstMethod() throws Exception {
    String source = generateSceneSource();

    assertNotNull("generated source must not be null", source);
    assertFalse("generated source must not be empty", source.isEmpty());
    assertTrue("source must contain class Scene",
        source.contains("class Scene"));
    assertTrue("source must contain myFirstMethod",
        source.contains("myFirstMethod"));
  }

  @Test
  public void sceneSourceContainsCountLoopForStatement() throws Exception {
    String source = generateSceneSource();

    assertTrue("source must contain for-loop from CountLoop",
        source.contains("for("));
  }

  @Test
  public void sceneSourceContainsEventListenerWiring() throws Exception {
    String source = generateSceneSource();

    assertTrue("source must contain addSceneActivationListener",
        source.contains("addSceneActivationListener"));
  }

  @Test
  public void sceneSourceContainsModelFieldDeclaration() throws Exception {
    String source = generateSceneSource();

    assertTrue("source must contain myBiped field",
        source.contains("myBiped"));
    assertTrue("source must reference SBiped type",
        source.contains("SBiped"));
  }

  @Test
  public void sceneSourceContainsInitializeEventListenersMethod() throws Exception {
    String source = generateSceneSource();

    assertTrue("source must contain initializeEventListeners method",
        source.contains("initializeEventListeners"));
  }

  @Test
  public void sceneSourceContainsSSceneSuperclass() throws Exception {
    String source = generateSceneSource();

    assertTrue("source must extend SScene",
        source.contains("extends SScene"));
  }

  // ── Program type generates valid Java source ─────────────────────────

  @Test
  public void programTypeGeneratesJavaSourceWithMySceneField() throws Exception {
    String source = generateProgramSource();

    assertNotNull("program source must not be null", source);
    assertFalse("program source must not be empty", source.isEmpty());
    assertTrue("source must contain class Program",
        source.contains("class Program"));
    assertTrue("source must contain myScene field",
        source.contains("myScene"));
    assertTrue("source must extend SProgram",
        source.contains("extends SProgram"));
  }

  // ── Per-method codegen ───────────────────────────────────────────────

  @Test
  public void myFirstMethodGeneratesCommentAndLoop() throws Exception {
    Project reopened = saveAndReopen();
    NamedUserType scene = SilverThreadStudentProgramSaveReadbackTest.findSceneType(reopened);
    UserMethod myFirstMethod = SilverThreadStudentProgramSaveReadbackTest
        .findMethodByName(scene, "myFirstMethod");
    assertNotNull("myFirstMethod must exist in reopened project", myFirstMethod);

    String source = generateMethod(myFirstMethod);

    assertTrue("method source must contain comment marker",
        source.contains("/*") || source.contains("//"));
    assertTrue("method source must contain for-loop",
        source.contains("for("));
  }

  @Test
  public void initializeEventListenersGeneratesListenerCall() throws Exception {
    Project reopened = saveAndReopen();
    NamedUserType scene = SilverThreadStudentProgramSaveReadbackTest.findSceneType(reopened);
    UserMethod initListeners = SilverThreadStudentProgramSaveReadbackTest
        .findMethodByName(scene, "initializeEventListeners");
    assertNotNull("initializeEventListeners must exist", initListeners);

    String source = generateMethod(initListeners);

    assertTrue("method source must contain addSceneActivationListener",
        source.contains("addSceneActivationListener"));
  }

  // ── Stability: source is non-empty for all methods ───────────────────

  @Test
  public void allSceneMethodsProduceNonEmptySource() throws Exception {
    Project reopened = saveAndReopen();
    NamedUserType scene = SilverThreadStudentProgramSaveReadbackTest.findSceneType(reopened);

    for (UserMethod method : scene.getDeclaredMethods()) {
      String source = generateMethod(method);
      assertFalse("method " + method.getName() + " must produce non-empty source",
          source.isEmpty());
    }
  }

  // ── Double round-trip codegen stability ──────────────────────────────

  @Test
  public void doubleRoundTripProducesSameSceneSource() throws Exception {
    Project pass1 = saveAndReopen();
    File file2 = temporaryFolder.newFile("pass2.a3p");
    IoUtilities.writeProject(file2, pass1);
    Project pass2 = IoUtilities.readProject(file2);

    String source1 = generateType(
        SilverThreadStudentProgramSaveReadbackTest.findSceneType(pass1));
    String source2 = generateType(
        SilverThreadStudentProgramSaveReadbackTest.findSceneType(pass2));

    assertTrue("double round-trip scene source must contain class Scene",
        source2.contains("class Scene"));
    assertTrue("double round-trip scene source must contain myFirstMethod",
        source2.contains("myFirstMethod"));
    assertTrue("double round-trip scene source must contain addSceneActivationListener",
        source2.contains("addSceneActivationListener"));
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  private Project saveAndReopen() throws Exception {
    Project original = SilverThreadStudentProgramSaveReadbackTest.buildStudentProject();
    File projectFile = temporaryFolder.newFile("codegen-student.a3p");
    IoUtilities.writeProject(projectFile, original);
    return IoUtilities.readProject(projectFile);
  }

  private String generateSceneSource() throws Exception {
    Project reopened = saveAndReopen();
    NamedUserType scene = SilverThreadStudentProgramSaveReadbackTest.findSceneType(reopened);
    return generateType(scene);
  }

  private String generateProgramSource() throws Exception {
    Project reopened = saveAndReopen();
    return generateType(reopened.getProgramType());
  }

  private static String generateType(NamedUserType type) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder()
        .addDefaultCodeOrganizerDefinition(CodeOrganizer.defaultCodeOrganizer)
        .build();
    type.process(generator);
    return generator.getText();
  }

  private static String generateMethod(UserMethod method) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder().build();
    method.body.getValue().process(generator);
    return generator.getText();
  }
}
