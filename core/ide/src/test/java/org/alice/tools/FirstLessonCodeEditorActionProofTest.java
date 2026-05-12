package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.alice.ide.codeeditor.CodeEditor;
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

public class FirstLessonCodeEditorActionProofTest {
  private static final String FIRST_LESSON_TARGET = "scene.eatmeFirstLesson";
  private static final String FIRST_LESSON_METHOD = "eatmeFirstLesson";
  private static final String WRONG_METHOD = "otherProcedure";
  private static final String MARKER = "wave4-code-editor-action-proof";
  private static final String ACTION_PROOF_ARTIFACT = "first-lesson-code-editor-action-proof.json";
  private static final String BLOCKED_FALLBACK_ARTIFACT = "first-lesson-code-editor-action-blocked.json";
  private static final String RETIRED_NO_GO_ARTIFACT = "procedure-ui-action-no-go.json";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void provesFirstLessonCodeEditorActionAndWritesStructuredEvidence() throws Exception {
    File projectFile = temporaryFolder.newFile("first-lesson.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods(FIRST_LESSON_METHOD, WRONG_METHOD));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    NamedUserType editedSceneType = sceneType(editedProject);
    assertCommentMarkerCount(requireMethod(editedSceneType, FIRST_LESSON_METHOD), MARKER, 1);
    assertCommentMarkerCount(requireMethod(editedSceneType, WRONG_METHOD), MARKER, 0);

    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains(
        "\"schema_version\":\"eatme.alice-first-lesson-code-editor-action-proof-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"proved\""));
    assertTrue(result, result.contains("\"action_proof\":\"" + ACTION_PROOF_ARTIFACT + "\""));
    assertNonEmptyFile(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertNoBlockedOrRetiredArtifacts(evidenceDir);

    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(proof, proof.contains("\"schema_version\": \"eatme.alice-first-lesson-code-editor-action-proof/v1\""));
    assertTrue(proof, proof.contains("\"status\": \"proved\""));
    assertTrue(proof, proof.contains("\"procedure_selector\": \"" + FIRST_LESSON_TARGET + "\""));
    assertTrue(proof, proof.contains("\"selected_declaration\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"code_composite_declaration\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"code_editor_backing\": \"" + CodeEditor.class.getName() + "\""));
    assertTrue(proof, proof.contains("\"code_editor_code\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"action\": \"append-comment\""));
    assertTrue(proof, proof.contains("\"marker\": \"" + MARKER + "\""));
    assertTrue(proof, proof.contains("\"before_statement_count\": 1"));
    assertTrue(proof, proof.contains("\"after_statement_count\": 2"));
    assertTrue(proof, proof.contains("\"target_marker_count\": 1"));
    assertTrue(proof, proof.contains("\"wrong_target_marker_count\": 0"));

    // Verify remaining structural fields to ensure schema completeness
    assertTrue(proof, proof.contains("\"edit_spec\": \"append-comment:" + MARKER + "\""));
    assertTrue(proof, proof.contains("\"input_project_artifact\": \"first-lesson.a3p\""));
    assertTrue(proof, proof.contains("\"scene_type\": \"Scene\""));
    assertTrue(proof, proof.contains("\"method_name\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"selection_mode\": \"in_editor_procedure_tab_operation\""));
    assertTrue(proof, proof.contains("\"operation_fired\": true"));
    assertTrue(proof, proof.contains("\"statement_count_delta\": 1"));
    assertTrue(proof, proof.contains("\"success\": true"));
    assertTrue(proof, proof.contains("\"edited_project\": \"edited-project.a3p\""));
    assertTrue(proof, proof.contains("\"before_methods\":"));
    assertTrue(proof, proof.contains("\"after_methods\":"));

    assertAllProofFieldsPresent(proof);

    assertTrue(proof, proof.contains("\"doesNotClaim\""));
    assertOutOfScopeClaimsStayExplicit(proof + result);
  }

  @Test
  public void initializesNullFirstLessonBodyBeforeSelectingProcedureTab() throws Exception {
    File projectFile = temporaryFolder.newFile("null-body-first-lesson.a3p");
    Project project = projectWithSceneMethods(WRONG_METHOD);
    sceneType(project).methods.add(new UserMethod(
        FIRST_LESSON_METHOD,
        JavaType.VOID_TYPE,
        new UserParameter[0],
        null));
    IoUtilities.writeProject(projectFile, project);
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    UserMethod editedMethod = requireMethod(sceneType(editedProject), FIRST_LESSON_METHOD);
    assertNotNull("first-lesson body should be initialized before the edit command", editedMethod.body.getValue());
    assertCommentMarkerCount(editedMethod, MARKER, 1);

    String proof = Files.readString(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertTrue(proof, proof.contains("\"selected_declaration\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"code_composite_declaration\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"code_editor_code\": \"" + FIRST_LESSON_METHOD + "\""));
    assertTrue(proof, proof.contains("\"before_statement_count\": 0"));
    assertTrue(proof, proof.contains("\"after_statement_count\": 1"));
    assertTrue(proof, proof.contains("\"statement_count_delta\": 1"));
    assertAllProofFieldsPresent(proof);
    assertNoBlockedOrRetiredArtifacts(evidenceDir);
  }

  @Test
  public void missingFirstLessonProcedureFailsClosedWithoutCreatingTargetOrArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("missing-first-lesson.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods(WRONG_METHOD));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("target procedure not found: " + FIRST_LESSON_TARGET));
    assertFalse(Files.exists(evidenceDir.resolve("edited-project.a3p")));
    assertFalse(Files.exists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
    assertNoBlockedOrRetiredArtifacts(evidenceDir);

    Project originalProject = IoUtilities.readProject(projectFile);
    assertFalse("the proof must not auto-create scene.eatmeFirstLesson",
        hasMethod(sceneType(originalProject), FIRST_LESSON_METHOD));
  }

  @Test
  public void acceptsAlternativeProcedureTarget() throws Exception {
    File projectFile = temporaryFolder.newFile("alt-target.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods(FIRST_LESSON_METHOD, WRONG_METHOD));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene." + WRONG_METHOD,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"status\":\"proved\""));
    assertTrue(result, result.contains("\"procedure_selector\":\"scene." + WRONG_METHOD + "\""));
    assertNonEmptyFile(evidenceDir.resolve(ACTION_PROOF_ARTIFACT));
    assertNonEmptyFile(evidenceDir.resolve("edited-project.a3p"));

    Project editedProject = IoUtilities.readProject(evidenceDir.resolve("edited-project.a3p").toFile());
    NamedUserType editedSceneType = sceneType(editedProject);
    assertCommentMarkerCount(requireMethod(editedSceneType, WRONG_METHOD), MARKER, 1);
    assertCommentMarkerCount(requireMethod(editedSceneType, FIRST_LESSON_METHOD), MARKER, 0);
    assertNoBlockedOrRetiredArtifacts(evidenceDir);
  }

  private static Project projectWithSceneMethods(String... methodNames) {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    for (String methodName : methodNames) {
      sceneType.methods.add(new UserMethod(
          methodName,
          JavaType.VOID_TYPE,
          new UserParameter[0],
          new BlockStatement(new Comment("existing " + methodName))));
    }
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static NamedUserType sceneType(Project project) {
    return (NamedUserType) project.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
  }

  private static UserMethod requireMethod(NamedUserType sceneType, String methodName) {
    UserMethod method = findMethod(sceneType, methodName);
    assertNotNull("scene should contain method " + methodName, method);
    return method;
  }

  private static UserMethod findMethod(NamedUserType sceneType, String methodName) {
    return sceneType.getDeclaredMethods().stream()
        .filter(method -> methodName.equals(method.getName()))
        .findFirst()
        .orElse(null);
  }

  private static boolean hasMethod(NamedUserType sceneType, String methodName) {
    return findMethod(sceneType, methodName) != null;
  }

  private static void assertCommentMarkerCount(UserMethod method, String marker, int expectedCount) {
    int markerCount = 0;
    for (Statement statement : method.body.getValue().statements) {
      if (statement instanceof Comment comment && marker.equals(comment.text.getValue())) {
        markerCount++;
      }
    }
    assertEquals("unexpected marker count in " + method.getName(), expectedCount, markerCount);
  }

  private static void assertNonEmptyFile(Path path) throws Exception {
    assertTrue(path.getFileName() + " should exist", Files.isRegularFile(path));
    assertTrue(path.getFileName() + " should not be empty", Files.size(path) > 0);
  }

  private static void assertNoBlockedOrRetiredArtifacts(Path evidenceDir) {
    assertFalse("proof must not emit a blocked fallback artifact",
        Files.exists(evidenceDir.resolve(BLOCKED_FALLBACK_ARTIFACT)));
    assertFalse("proof must not keep the older no-go artifact as success evidence",
        Files.exists(evidenceDir.resolve(RETIRED_NO_GO_ARTIFACT)));
  }

  private static final String[] PROOF_ARTIFACT_REQUIRED_FIELDS = {
      "schema_version", "status", "procedure_selector", "edit_spec",
      "input_project_artifact", "scene_type", "method_name", "selection_mode",
      "selected_declaration", "code_composite_declaration", "code_editor_backing",
      "code_editor_code", "operation_fired", "action", "marker",
      "before_statement_count", "after_statement_count", "statement_count_delta",
      "target_marker_count", "wrong_target_marker_count",
      "before_methods", "after_methods", "edited_project", "success", "doesNotClaim"
  };

  private static void assertAllProofFieldsPresent(String proof) {
    for (String field : PROOF_ARTIFACT_REQUIRED_FIELDS) {
      assertTrue("proof artifact must contain field: " + field,
          proof.contains("\"" + field + "\""));
    }
  }

  private static void assertOutOfScopeClaimsStayExplicit(String evidence) {
    assertTrue(evidence, evidence.contains("first-lesson completion"));
    assertTrue(evidence, evidence.contains("grading"));
    assertTrue(evidence, evidence.contains("creative assessment"));
    assertTrue(evidence, evidence.contains("visible rendering correctness"));
    assertFalse(evidence, evidence.contains("\"timeout\""));
    assertFalse(evidence, evidence.contains("workflow timeout"));
  }
}
