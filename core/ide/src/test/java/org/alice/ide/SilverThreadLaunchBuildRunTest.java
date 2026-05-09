package org.alice.ide;

import org.alice.ide.uricontent.FileProjectLoader;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.virtualmachine.ReleaseVirtualMachine;
import org.lgna.project.virtualmachine.events.CountLoopIterationEvent;
import org.lgna.project.virtualmachine.events.EachInTogetherItemEvent;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.ForEachLoopIterationEvent;
import org.lgna.project.virtualmachine.events.StatementExecutionEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;
import org.lgna.project.virtualmachine.events.WhileLoopIterationEvent;
import org.lgna.story.SProgram;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread end-to-end test: proves the core student journey works headlessly.
 *
 * <p>Headless scope: no JavaFX, no 3D rendering, no gallery assets, no drag-and-drop UI.
 * This test verifies the create → build → run → save → reopen → verify round-trip
 * that underpins every Alice lesson.
 */
public class SilverThreadLaunchBuildRunTest {

  private static final String PROGRAM_NAME = "SilverThreadProgram";
  private static final String METHOD_NAME = "performSilverThreadStep";
  private static final String COMMENT_TEXT = "silver thread: student added this comment tile";

  // ── Test 1: Full 7-step E2E journey ────────────────────────────────────

  @Test
  public void createProjectAddCommentSaveReopenExecuteAndVerifyRoundTrip() throws Exception {
    Path workDir = Files.createDirectories(
        Path.of("target", "silver-thread-e2e", UUID.randomUUID().toString()));

    // Step 1: Create a project with a program type
    NamedUserType programType = programType(PROGRAM_NAME);
    Comment commentStatement = new Comment(COMMENT_TEXT);
    UserMethod storyMethod = new UserMethod(
        METHOD_NAME,
        Void.TYPE,
        new UserParameter[0],
        new BlockStatement(commentStatement));
    storyMethod.isStatic.setValue(true);

    // Step 2: Add the method to the program type (simulating dragging a code tile)
    programType.methods.add(storyMethod);
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);

    // Step 3: Save the project to disk
    File savedFile = workDir.resolve("silver-thread.a3p").toFile();
    IoUtilities.writeProject(savedFile, project);
    assertTrue("Saved project file must exist on disk", savedFile.isFile());

    // Step 4: Reopen the saved project via TestFileProjectLoader
    Project reopenedProject = new TestFileProjectLoader(savedFile).loadNow();
    assertNotNull("Reopened project must not be null", reopenedProject);
    assertEquals(PROGRAM_NAME, reopenedProject.getProgramType().getName());

    // Locate the method by name in the reopened AST
    UserMethod reopenedMethod = findMethodByName(reopenedProject.getProgramType(), METHOD_NAME);
    assertNotNull("Method '" + METHOD_NAME + "' must survive serialization round-trip", reopenedMethod);

    // Verify the comment text survived deserialization
    BlockStatement body = (BlockStatement) reopenedMethod.getBodyProperty().getValue();
    assertNotNull("Method body must not be null after deserialization", body);
    assertEquals("Method body must contain exactly one statement", 1, body.statements.size());
    Statement firstStatement = body.statements.get(0);
    assertTrue("First statement must be a Comment", firstStatement instanceof Comment);
    assertEquals(COMMENT_TEXT, ((Comment) firstStatement).text.getValue());

    // Step 5 & 6: Execute the program through ReleaseVirtualMachine with a listener
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    vm.ENTRY_POINT_invoke(null, reopenedMethod);

    assertEquals(
        "VM listener must record BlockStatement+Comment executing/executed events",
        Arrays.asList(
            "executing:BlockStatement",
            "executing:Comment",
            "executed:Comment",
            "executed:BlockStatement"),
        listener.statementEvents);

    vm.removeVirtualMachineListener(listener);

    // Step 7: Save again and verify round-trip
    File secondSave = workDir.resolve("silver-thread-round-trip.a3p").toFile();
    IoUtilities.writeProject(secondSave, reopenedProject);
    assertTrue("Second-save file must exist", secondSave.isFile());

    Project secondReopened = new TestFileProjectLoader(secondSave).loadNow();
    assertNotNull("Second-reopened project must not be null", secondReopened);
    assertEquals(PROGRAM_NAME, secondReopened.getProgramType().getName());

    UserMethod secondMethod = findMethodByName(secondReopened.getProgramType(), METHOD_NAME);
    assertNotNull("Method must survive second serialization round-trip", secondMethod);
    BlockStatement secondBody = (BlockStatement) secondMethod.getBodyProperty().getValue();
    assertEquals("Statement count must survive second round-trip", 1, secondBody.statements.size());
    assertEquals(COMMENT_TEXT, ((Comment) secondBody.statements.get(0)).text.getValue());
  }

  // ── Test 2: Load a real .a3p starter project ───────────────────────────

  @Test
  public void loadRealStarterProjectInspectSaveCopyAndReopen() throws Exception {
    Path workDir = Files.createDirectories(
        Path.of("target", "silver-thread-starter", UUID.randomUUID().toString()));

    // Copy the bundled starter project from test resources to a temp file
    File starterFile = workDir.resolve("indiaMinimum.a3p").toFile();
    try (InputStream starterStream = getClass().getResourceAsStream("/starters/indiaMinimum.a3p")) {
      assertNotNull("indiaMinimum.a3p must be on the test classpath", starterStream);
      Files.copy(starterStream, starterFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Load via IoUtilities.readProject (the other reopen path)
    Project starterProject = IoUtilities.readProject(starterFile);
    assertNotNull("Starter project must load successfully", starterProject);

    // Inspect the program type
    NamedUserType starterProgramType = starterProject.getProgramType();
    assertNotNull("Starter programType must not be null", starterProgramType);
    String starterName = starterProgramType.getName();
    assertNotNull("Starter programType name must not be null", starterName);
    assertFalse("Starter programType name must not be empty", starterName.isEmpty());
    assertTrue(
        "Starter programType must be assignable to SProgram",
        starterProgramType.isAssignableTo(SProgram.class));

    // Save a copy
    File copyFile = workDir.resolve("indiaMinimum-copy.a3p").toFile();
    IoUtilities.writeProject(copyFile, starterProject);
    assertTrue("Saved copy must exist", copyFile.isFile());

    // Reopen the copy and verify name survived
    Project copiedProject = IoUtilities.readProject(copyFile);
    assertNotNull("Copied project must load successfully", copiedProject);
    assertEquals(
        "Program type name must survive save/reopen cycle",
        starterName,
        copiedProject.getProgramType().getName());
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static UserMethod findMethodByName(NamedUserType type, String name) {
    for (UserMethod method : type.getDeclaredMethods()) {
      if (name.equals(method.getName())) {
        return method;
      }
    }
    return null;
  }

  private static class TestFileProjectLoader extends FileProjectLoader {
    TestFileProjectLoader(File file) {
      super(file);
    }

    Project loadNow() {
      return load();
    }
  }

  private static class RecordingVirtualMachineListener implements VirtualMachineListener {
    private final List<String> statementEvents = new ArrayList<>();

    @Override
    public void statementExecuting(StatementExecutionEvent event) {
      statementEvents.add("executing:" + event.getStatement().getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent event) {
      statementEvents.add("executed:" + event.getStatement().getClass().getSimpleName());
    }

    @Override
    public void whileLoopIterating(WhileLoopIterationEvent event) {
    }

    @Override
    public void whileLoopIterated(WhileLoopIterationEvent event) {
    }

    @Override
    public void countLoopIterating(CountLoopIterationEvent event) {
    }

    @Override
    public void countLoopIterated(CountLoopIterationEvent event) {
    }

    @Override
    public void forEachLoopIterating(ForEachLoopIterationEvent event) {
    }

    @Override
    public void forEachLoopIterated(ForEachLoopIterationEvent event) {
    }

    @Override
    public void eachInTogetherItemExecuting(EachInTogetherItemEvent event) {
    }

    @Override
    public void eachInTogetherItemExecuted(EachInTogetherItemEvent event) {
    }

    @Override
    public void expressionEvaluated(ExpressionEvaluationEvent event) {
    }
  }
}
