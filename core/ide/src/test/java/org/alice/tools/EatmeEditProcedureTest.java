package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
import org.lgna.story.SScene;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EatmeEditProcedureTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void editsSceneProcedureAndWritesEatmeProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:eatme edit proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"schema_version\":\"eatme.alice-procedure-edit-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"edited\""));
    assertTrue(result, result.contains("\"edited_project_artifact\":\"edited-project.a3p\""));
    assertTrue(result, result.contains("\"procedure_edit_command\":\"procedure-edit-command.json\""));
    assertTrue(result, result.contains("\"procedure_or_code_diff\":\"procedure.diff.json\""));
    assertTrue(result, result.contains("\"procedure_tab_selection\":\"procedure-tab-selection.json\""));
    assertTrue(result, result.contains("\"procedure_ui_action_no_go\":\"procedure-ui-action-no-go.json\""));
    assertTrue(Files.size(evidenceDir.resolve("procedure-edit.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("procedure-edit-command.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("procedure.diff.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("procedure-tab-selection.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("procedure-ui-action-no-go.json")) > 0);

    String tabSelection = Files.readString(evidenceDir.resolve("procedure-tab-selection.json"));
    assertTrue(tabSelection,
        tabSelection.contains("\"schema_version\": \"eatme.alice-procedure-tab-selection/v1\""));
    assertTrue(tabSelection, tabSelection.contains("\"selection_mode\": \"in_editor_procedure_tab_operation\""));
    assertTrue(tabSelection, tabSelection.contains("\"procedure_selector\": \"scene.eatmeFirstLesson\""));
    assertTrue(tabSelection, tabSelection.contains("\"selected_method\": \"eatmeFirstLesson\""));
    assertTrue(tabSelection, tabSelection.contains("\"operation_fired\": true"));
    assertTrue(tabSelection, tabSelection.contains("desktop UI action invoked"));

    String editCommand = Files.readString(evidenceDir.resolve("procedure-edit-command.json"));
    assertTrue(editCommand,
        editCommand.contains("\"schema_version\": \"eatme.alice-procedure-edit-command/v1\""));
    assertTrue(editCommand, editCommand.contains("\"procedure_selector\": \"scene.eatmeFirstLesson\""));
    assertTrue(editCommand, editCommand.contains("\"command\": \"append-comment\""));
    assertTrue(editCommand, editCommand.contains("\"selected_method\": \"eatmeFirstLesson\""));
    assertTrue(editCommand, editCommand.contains("\"completed\": true"));
    assertTrue(editCommand, editCommand.contains("\"before_statement_count\": 0"));
    assertTrue(editCommand, editCommand.contains("\"after_statement_count\": 1"));
    assertTrue(editCommand, editCommand.contains("\"statement_count_delta\": 1"));
    assertTrue(editCommand, editCommand.contains("\"doesNotClaim\""));
    assertTrue(editCommand, editCommand.contains("desktop UI action invoked"));

    String uiActionNoGo = Files.readString(evidenceDir.resolve("procedure-ui-action-no-go.json"));
    assertTrue(uiActionNoGo,
        uiActionNoGo.contains("\"schema_version\": \"eatme.alice-code-procedure-ui-action-no-go/v1\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"status\": \"blocked\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"source\": \"EatmeEditProcedure\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"procedure_selector\": \"scene.eatmeFirstLesson\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"ast_edit_artifact\": \"procedure-edit.json\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"procedure_edit_command\": \"procedure-edit-command.json\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"procedure_or_code_diff\": \"procedure.diff.json\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"procedure_tab_selection\": \"procedure-tab-selection.json\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"exact_missing_ui_edit_action_target\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("desktop code editor edit action after selecting scene.eatmeFirstLesson"));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("org.alice.ide.codeeditor.CodeEditor"));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("org.alice.ide.declarationseditor.CodeComposite"));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("org.alice.ide.declarationseditor.DeclarationsEditorComposite"));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"code_editor_action_target_not_exposed\""));
    assertTrue(uiActionNoGo, uiActionNoGo.contains("\"append_comment_ui_invocation_not_available\""));
    assertFalse(uiActionNoGo, uiActionNoGo.contains("\"procedure_selector_not_bound_to_ui_action\""));
    String doesNotClaim = jsonSection(uiActionNoGo, "doesNotClaim");
    assertTrue(doesNotClaim, doesNotClaim.contains("desktop UI action invoked"));
    assertTrue(doesNotClaim, doesNotClaim.contains("code editor/procedure action completion"));
    assertTrue(doesNotClaim, doesNotClaim.contains("full Alice UI automation"));
    assertTrue(doesNotClaim, doesNotClaim.contains("visible rendering correctness"));
    assertTrue(doesNotClaim, doesNotClaim.contains("first-lesson completion"));
    assertTrue(doesNotClaim, doesNotClaim.contains("grading"));
    assertTrue(doesNotClaim, doesNotClaim.contains("creative assessment"));

    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    NamedUserType sceneType = (NamedUserType) editedProject.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
    UserMethod method = sceneType.getDeclaredMethods().stream()
        .filter(candidate -> "eatmeFirstLesson".equals(candidate.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull("edited project should contain the selected method", method);
    Statement statement = method.body.getValue().statements.get(0);
    assertTrue("edit proof should be a comment statement", statement instanceof Comment);
    assertEquals("eatme edit proof", ((Comment) statement).text.getValue());
  }

  @Test
  public void chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff() throws Exception {
    File starterProject = temporaryFolder.newFile("starter.a3p");
    IoUtilities.writeProject(starterProject, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream placementStdout = new ByteArrayOutputStream();
    ByteArrayOutputStream placementStderr = new ByteArrayOutputStream();

    int placementStatus = EatmePlaceObject.run(
        new String[] {
            "--project", starterProject.getAbsolutePath(),
            "--object", "alice-gallery://animals/bunny",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(placementStdout),
        new PrintStream(placementStderr));

    assertEquals(placementStderr.toString(StandardCharsets.UTF_8), 0, placementStatus);
    String placementResult = placementStdout.toString(StandardCharsets.UTF_8);
    assertTrue(placementResult, placementResult.contains("\"schema_version\":\"eatme.alice-object-placement-result/v1\""));
    assertTrue(placementResult, placementResult.contains("\"status\":\"placed\""));
    assertNonEmptyFile(evidenceDir.resolve("placed-project.a3p"));
    assertNonEmptyFile(evidenceDir.resolve("placement.json"));
    assertNonEmptyFile(evidenceDir.resolve("scene.diff.json"));

    Project placedProject = IoUtilities.readProject(evidenceDir.resolve("placed-project.a3p").toFile());
    NamedUserType placedSceneType = sceneType(placedProject);
    assertNotNull("placed project should contain the bunny field before editing",
        findField(placedSceneType, "bunny"));

    ByteArrayOutputStream editStdout = new ByteArrayOutputStream();
    ByteArrayOutputStream editStderr = new ByteArrayOutputStream();
    int editStatus = EatmeEditProcedure.run(
        new String[] {
            "--project", evidenceDir.resolve("placed-project.a3p").toString(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:eatme placement to procedure proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(editStdout),
        new PrintStream(editStderr));

    assertEquals(editStderr.toString(StandardCharsets.UTF_8), 0, editStatus);
    String editResult = editStdout.toString(StandardCharsets.UTF_8);
    assertTrue(editResult, editResult.contains("\"schema_version\":\"eatme.alice-procedure-edit-result/v1\""));
    assertTrue(editResult, editResult.contains("\"status\":\"edited\""));
    assertNonEmptyFile(evidenceDir.resolve("edited-project.a3p"));
    assertNonEmptyFile(evidenceDir.resolve("procedure-edit.json"));
    assertNonEmptyFile(evidenceDir.resolve("procedure-edit-command.json"));
    assertNonEmptyFile(evidenceDir.resolve("procedure.diff.json"));
    assertNonEmptyFile(evidenceDir.resolve("procedure-tab-selection.json"));
    assertNonEmptyFile(evidenceDir.resolve("procedure-ui-action-no-go.json"));

    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    NamedUserType editedSceneType = sceneType(editedProject);
    assertNotNull("edited project should retain the placed bunny field",
        findField(editedSceneType, "bunny"));
    UserMethod method = findMethod(editedSceneType, "eatmeFirstLesson");
    assertNotNull("edited project should contain the selected method", method);
    assertEquals(1, method.body.getValue().statements.size());
    Statement statement = method.body.getValue().statements.get(0);
    assertTrue("chained edit proof should be a comment statement", statement instanceof Comment);
    assertEquals("eatme placement to procedure proof", ((Comment) statement).text.getValue());

    String editArtifact = Files.readString(evidenceDir.resolve("procedure-edit.json"));
    assertTrue("procedure edit artifact should record the placed-project handoff",
        editArtifact.contains("\"input_project_artifact\": \"placed-project.a3p\""));
  }

  @Test
  public void procedureEditArtifactsKeepClaimsScopedToProcedureEditSeam() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:narrow claim proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(new ByteArrayOutputStream()));

    assertEquals(0, status);
    String tabSelection = Files.readString(evidenceDir.resolve("procedure-tab-selection.json"));
    String editCommand = Files.readString(evidenceDir.resolve("procedure-edit-command.json"));
    String uiActionNoGo = Files.readString(evidenceDir.resolve("procedure-ui-action-no-go.json"));

    String tabDoesNotClaim = jsonSection(tabSelection, "doesNotClaim");
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("desktop UI action invoked"));
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("code editor/procedure action completion"));
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("full Alice UI automation"));
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("visible rendering correctness"));
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("first-lesson completion"));
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("grading"));
    assertTrue(tabDoesNotClaim, tabDoesNotClaim.contains("creative assessment"));

    String commandDoesNotClaim = jsonSection(editCommand, "doesNotClaim");
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("desktop UI action invoked"));
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("desktop code editor command completion"));
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("Save-menu completion"));
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("full Alice UI automation"));
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("visible rendering correctness"));
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("first-lesson completion"));
    assertTrue(commandDoesNotClaim, commandDoesNotClaim.contains("grading"));

    String uiDoesNotClaim = jsonSection(uiActionNoGo, "doesNotClaim");
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("desktop UI action invoked"));
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("code editor/procedure action completion"));
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("full Alice UI automation"));
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("visible rendering correctness"));
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("first-lesson completion"));
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("grading"));
    assertTrue(uiDoesNotClaim, uiDoesNotClaim.contains("creative assessment"));

    String scopedArtifacts = tabSelection + editCommand + uiActionNoGo;
    assertFalse(scopedArtifacts, scopedArtifacts.contains("full lesson completion"));
    assertFalse(scopedArtifacts, scopedArtifacts.contains("launcher"));
    assertFalse(scopedArtifacts, scopedArtifacts.contains("model exporter"));
    assertFalse(scopedArtifacts, scopedArtifacts.contains("hotspot"));
    assertFalse(scopedArtifacts, scopedArtifacts.contains("Select Project PID"));
  }

  private static String jsonSection(String json, String fieldName) {
    int fieldStart = json.indexOf("\"" + fieldName + "\"");
    assertTrue(fieldName + " field should exist", fieldStart >= 0);
    int nextFieldStart = json.indexOf("\n  \"", fieldStart + 1);
    if (nextFieldStart < 0) {
      nextFieldStart = json.lastIndexOf('}');
    }
    assertTrue(fieldName + " field should have an end", nextFieldStart > fieldStart);
    return json.substring(fieldStart, nextFieldStart);
  }

  @Test
  public void appendsToExistingSceneProcedure() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:second proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(new ByteArrayOutputStream()));

    assertEquals(0, status);
    String diff = Files.readString(evidenceDir.resolve("procedure.diff.json"));
    assertTrue(diff, diff.contains("\"created_method\": false"));
    assertTrue(diff, diff.contains("\"statement_count_delta\": 1"));
  }

  @Test
  public void rejectsUnsupportedEditSpecWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "replace-body",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("unsupported edit spec"));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-edit.json")));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));
  }

  @Test
  public void rejectsInvalidProcedureSelectorWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.123bad",
            "--edit-spec", "append-comment:proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("procedure selector must name one scene method"));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-edit.json")));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));
  }

  @Test
  public void rejectsProcedureSelectorWithoutScenePrefixWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "program.eatmeFirstLesson",
            "--edit-spec", "append-comment:proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("unsupported procedure selector"));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-edit.json")));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));
  }

  @Test
  public void rejectsBlankCommentEditSpecWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:   ",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("append-comment edit spec must include non-blank text"));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-edit.json")));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));
  }

  @Test
  public void rejectsMissingProjectWithoutProofArtifacts() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", temporaryFolder.getRoot().toPath().resolve("missing.a3p").toString(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("project file does not exist"));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-edit.json")));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));
  }

  @Test
  public void rejectsProjectWithoutSceneWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("no-scene.a3p");
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    IoUtilities.writeProject(projectFile, new Project(programType, Project.SceneCameraType.WindowCamera));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.eatmeFirstLesson",
            "--edit-spec", "append-comment:proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("project does not contain a program field typed by an SScene subtype"));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-edit.json")));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));
  }

  private static Project projectWithScene() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static Project projectWithSceneMethod(String methodName) {
    Project project = projectWithScene();
    NamedUserType sceneType = sceneType(project);
    sceneType.methods.add(new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement(new Comment("existing"))));
    return project;
  }

  private static NamedUserType sceneType(Project project) {
    return (NamedUserType) project.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
  }

  private static UserField findField(NamedUserType sceneType, String fieldName) {
    return sceneType.getDeclaredFields().stream()
        .filter(field -> fieldName.equals(field.getName()))
        .findFirst()
        .orElse(null);
  }

  private static UserMethod findMethod(NamedUserType sceneType, String methodName) {
    return sceneType.getDeclaredMethods().stream()
        .filter(method -> methodName.equals(method.getName()))
        .findFirst()
        .orElse(null);
  }

  private static void assertNonEmptyFile(Path path) throws Exception {
    assertTrue(path.getFileName() + " should exist", Files.isRegularFile(path));
    assertTrue(path.getFileName() + " should not be empty", Files.size(path) > 0);
  }
}
