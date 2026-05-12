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
  private static final String ACTION_PROOF_ARTIFACT = "first-lesson-code-editor-action-proof.json";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void editsSceneProcedureAndWritesEatmeProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("eatmeFirstLesson"));
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
    assertTrue(result, result.contains(
        "\"schema_version\":\"eatme.alice-first-lesson-code-editor-action-proof-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"proved\""));
    assertTrue(result, result.contains("\"edited_project_artifact\":\"edited-project.a3p\""));
    assertTrue(result, result.contains("\"action_proof\":\"" + ACTION_PROOF_ARTIFACT + "\""));
    assertNonEmptyFile(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));

    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(proof,
        proof.contains("\"schema_version\": \"eatme.alice-first-lesson-code-editor-action-proof/v1\""));
    assertTrue(proof, proof.contains("\"status\": \"proved\""));
    assertTrue(proof, proof.contains("\"procedure_selector\": \"scene.eatmeFirstLesson\""));
    assertTrue(proof, proof.contains("\"selected_declaration\": \"eatmeFirstLesson\""));
    assertTrue(proof, proof.contains("\"code_composite_declaration\": \"eatmeFirstLesson\""));
    assertTrue(proof, proof.contains("\"code_editor_backing\": \"org.alice.ide.codeeditor.CodeEditor\""));
    assertTrue(proof, proof.contains("\"code_editor_code\": \"eatmeFirstLesson\""));
    assertTrue(proof, proof.contains("\"action\": \"append-comment\""));
    assertTrue(proof, proof.contains("\"before_statement_count\": 1"));
    assertTrue(proof, proof.contains("\"after_statement_count\": 2"));
    assertTrue(proof, proof.contains("\"target_marker_count\": 1"));
    assertTrue(proof, proof.contains("\"wrong_target_marker_count\": 0"));
    String doesNotClaim = jsonSection(proof, "doesNotClaim");
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
    Statement statement = method.body.getValue().statements.get(1);
    assertTrue("edit proof should be a comment statement", statement instanceof Comment);
    assertEquals("eatme edit proof", ((Comment) statement).text.getValue());
  }

  @Test
  public void chainsObjectPlacementIntoProcedureEditAndRecordsPlacedProjectHandoff() throws Exception {
    File starterProject = temporaryFolder.newFile("starter.a3p");
    IoUtilities.writeProject(starterProject, projectWithSceneMethod("eatmeFirstLesson"));
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
    assertTrue(editResult, editResult.contains(
        "\"schema_version\":\"eatme.alice-first-lesson-code-editor-action-proof-result/v1\""));
    assertTrue(editResult, editResult.contains("\"status\":\"proved\""));
    assertNonEmptyFile(evidenceDir.resolve("edited-project.a3p"));
    assertNonEmptyFile(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(Files.notExists(evidenceDir.resolve("procedure-ui-action-no-go.json")));

    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    NamedUserType editedSceneType = sceneType(editedProject);
    assertNotNull("edited project should retain the placed bunny field",
        findField(editedSceneType, "bunny"));
    UserMethod method = findMethod(editedSceneType, "eatmeFirstLesson");
    assertNotNull("edited project should contain the selected method", method);
    assertEquals(2, method.body.getValue().statements.size());
    Statement statement = method.body.getValue().statements.get(1);
    assertTrue("chained edit proof should be a comment statement", statement instanceof Comment);
    assertEquals("eatme placement to procedure proof", ((Comment) statement).text.getValue());

    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue("action proof artifact should record the placed-project handoff",
        proof.contains("\"input_project_artifact\": \"placed-project.a3p\""));
  }

  @Test
  public void procedureEditArtifactsKeepClaimsScopedToProcedureEditSeam() throws Exception {
    File projectFile = temporaryFolder.newFile("placed.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("eatmeFirstLesson"));
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
    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    String doesNotClaim = jsonSection(proof, "doesNotClaim");
    assertTrue(doesNotClaim, doesNotClaim.contains("full first-lesson completion"));
    assertTrue(doesNotClaim, doesNotClaim.contains("visible rendering correctness"));
    assertTrue(doesNotClaim, doesNotClaim.contains("first-lesson completion"));
    assertTrue(doesNotClaim, doesNotClaim.contains("grading"));
    assertTrue(doesNotClaim, doesNotClaim.contains("creative assessment"));
    assertTrue(doesNotClaim, doesNotClaim.contains("broad UI automation"));

    String scopedArtifacts = proof;
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

  // --- Issue #521: alternative selectors (e.g., africa.a3p's myFirstMethod) ---

  @Test
  public void editsAfricaStyleMyFirstMethodProcedure() throws Exception {
    File projectFile = temporaryFolder.newFile("africa.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("myFirstMethod"));
    Path evidenceDir = temporaryFolder.newFolder("africa-evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.myFirstMethod",
            "--edit-spec", "append-comment:africa edit proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"status\":\"proved\""));
    assertTrue(result, result.contains("\"procedure_selector\":\"scene.myFirstMethod\""));
    assertNonEmptyFile(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertNonEmptyFile(evidenceDir.resolve("edited-project.a3p"));

    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(proof, proof.contains("\"procedure_selector\": \"scene.myFirstMethod\""));
    assertTrue(proof, proof.contains("\"method_name\": \"myFirstMethod\""));
    assertTrue(proof, proof.contains("\"selected_declaration\": \"myFirstMethod\""));
    assertTrue(proof, proof.contains("\"target_marker_count\": 1"));
    assertTrue(proof, proof.contains("\"wrong_target_marker_count\": 0"));

    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    NamedUserType editedSceneType = sceneType(editedProject);
    UserMethod method = findMethod(editedSceneType, "myFirstMethod");
    assertNotNull("edited project should contain myFirstMethod", method);
    Statement lastStatement = method.body.getValue().statements.get(1);
    assertTrue("appended edit should be a comment", lastStatement instanceof Comment);
    assertEquals("africa edit proof", ((Comment) lastStatement).text.getValue());
  }

  @Test
  public void editsUnderscorePrefixedProcedure() throws Exception {
    File projectFile = temporaryFolder.newFile("custom.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("_setup"));
    Path evidenceDir = temporaryFolder.newFolder("underscore-evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene._setup",
            "--edit-spec", "append-comment:underscore proof",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"status\":\"proved\""));
    assertNonEmptyFile(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));

    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(proof, proof.contains("\"method_name\": \"_setup\""));
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
    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(proof, proof.contains("\"before_statement_count\": 1"));
    assertTrue(proof, proof.contains("\"after_statement_count\": 2"));
    assertTrue(proof, proof.contains("\"statement_count_delta\": 1"));
    assertTrue(proof, proof.contains("\"target_marker_count\": 1"));
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
    assertTrue(Files.notExists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
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
    assertTrue(Files.notExists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
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
    assertTrue(Files.notExists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
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
    assertTrue(Files.notExists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
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
    assertTrue(Files.notExists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
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
    assertTrue(Files.notExists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
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
