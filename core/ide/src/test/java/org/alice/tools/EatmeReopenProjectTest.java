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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EatmeReopenProjectTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void reopensProjectAndWritesEatmeProofArtifacts() throws Exception {
    File savedProjectFile = temporaryFolder.newFile("saved-project.a3p");
    IoUtilities.writeProject(savedProjectFile, projectWithSceneMethod("eatmeFirstLessonStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeReopenProject.run(
        new String[] {
            "--saved-project", savedProjectFile.getAbsolutePath(),
            "--reopen-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"schema_version\":\"eatme.alice-project-reopen-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"reopened\""));
    assertTrue(result, result.contains("\"reopen_selector\":\"scene.eatmeFirstLessonStep\""));
    assertTrue(result, result.contains("\"reopened_project_artifact\":\"reopened.a3p\""));
    assertTrue(result, result.contains("\"reopen_artifact\":\"reopen-evidence.json\""));
    assertTrue(result, result.contains("\"reopened_state_artifact\":\"reopened-state.json\""));
    assertTrue(result, result.contains("\"state_verification\":\"passed\""));
    assertTrue(result, result.contains("\"source_saved_project_artifact\":\""));
    assertEquals("", stderr.toString(StandardCharsets.UTF_8));

    assertTrue(Files.size(evidenceDir.resolve("reopened.a3p")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("reopen-evidence.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("reopened-state.json")) > 0);

    String reopenArtifact = Files.readString(evidenceDir.resolve("reopen-evidence.json"));
    assertTrue(reopenArtifact, reopenArtifact.contains("\"schema_version\": \"eatme.alice-project-reopen-artifact/v1\""));
    assertTrue(reopenArtifact, reopenArtifact.contains("\"reopen_mode\": \"headless_project_persistence_roundtrip\""));
    assertTrue(reopenArtifact, reopenArtifact.contains("\"readable_after_reopen\": true"));

    String stateArtifact = Files.readString(evidenceDir.resolve("reopened-state.json"));
    assertTrue(stateArtifact, stateArtifact.contains("\"schema_version\": \"eatme.alice-project-reopen-state/v1\""));
    assertTrue(stateArtifact, stateArtifact.contains("\"scene_type_matches\": true"));
    assertTrue(stateArtifact, stateArtifact.contains("\"method_present_after_roundtrip\": true"));
    assertTrue(stateArtifact, stateArtifact.contains("\"state_verification\": \"passed\""));

    Project reopenedProject = IoUtilities.readProject(evidenceDir.resolve("reopened.a3p").toFile());
    NamedUserType sceneType = (NamedUserType) reopenedProject.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
    UserMethod method = sceneType.getDeclaredMethods().stream()
        .filter(candidate -> "eatmeFirstLessonStep".equals(candidate.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull("reopened project should retain the selected scene method", method);
  }

  @Test
  public void acceptsDifferentSceneReopenSelectorWhenMethodExists() throws Exception {
    File savedProjectFile = temporaryFolder.newFile("saved-project.a3p");
    IoUtilities.writeProject(savedProjectFile, projectWithSceneMethod("otherStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeReopenProject.run(
        new String[] {
            "--saved-project", savedProjectFile.getAbsolutePath(),
            "--reopen-selector", "scene.otherStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("\"reopen_selector\":\"scene.otherStep\""));
    assertEquals("", stderr.toString(StandardCharsets.UTF_8));
    assertTrue(Files.size(evidenceDir.resolve("reopened.a3p")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("reopen-evidence.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("reopened-state.json")) > 0);
  }

  @Test
  public void rejectsMissingSceneMethodWithoutProofArtifacts() throws Exception {
    File savedProjectFile = temporaryFolder.newFile("saved-project.a3p");
    IoUtilities.writeProject(savedProjectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeReopenProject.run(
        new String[] {
            "--saved-project", savedProjectFile.getAbsolutePath(),
            "--reopen-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("reopen selector does not name a scene method"));
    assertTrue(Files.notExists(evidenceDir.resolve("reopened.a3p")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsArtifactPathThatNormalizesToEvidenceDirectory() {
    EatmeReopenProject.artifactPath(temporaryFolder.getRoot().toPath(), "foo/..");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentArtifactPath() {
    EatmeReopenProject.artifactPath(temporaryFolder.getRoot().toPath(), "../reopened.a3p");
  }

  @Test
  public void rejectsMissingSavedProjectWithoutProofArtifacts() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeReopenProject.run(
        new String[] {
            "--saved-project", temporaryFolder.getRoot().toPath().resolve("missing.a3p").toString(),
            "--reopen-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("project file does not exist"));
    assertTrue(Files.notExists(evidenceDir.resolve("reopened.a3p")));
  }

  @Test
  public void escapesJsonControlCharacters() {
    assertEquals(
        "quote\\\" slash\\\\ backspace\\b formfeed\\f newline\\n return\\r tab\\t low\\u0001",
        EatmeReopenProject.escapeJson("quote\" slash\\ backspace\b formfeed\f newline\n return\r tab\t low\u0001"));
  }

  @Test
  public void sceneTypeMismatchSetsStateVerificationToFailed_reopenedStateJson() {
    String json = EatmeReopenProject.reopenedStateJson(false, true, "OriginalScene", "RenamedScene");

    assertTrue(json, json.contains("\"state_verification\": \"failed\""));
    assertTrue(json, json.contains("\"scene_type_matches\": false"));
    assertTrue(json, json.contains("\"method_present_after_roundtrip\": true"));
    assertTrue(json, json.contains("\"scene_type_mismatch\""));
    assertTrue(json, json.contains("\"original_scene_type\": \"OriginalScene\""));
    assertTrue(json, json.contains("\"reopened_scene_type\": \"RenamedScene\""));
  }

  @Test
  public void sceneTypeMismatchSetsStateVerificationToFailed_resultJson() {
    EatmeReopenProject.ProjectReopen reopen = new EatmeReopenProject.ProjectReopen(
        "scene.myMethod", "OriginalScene", "myMethod",
        "saved.a3p", "reopened.a3p", false, "RenamedScene");

    String json = EatmeReopenProject.resultJson(reopen);

    assertTrue(json, json.contains("\"state_verification\":\"failed\""));
    assertTrue(json, json.contains("\"scene_type_mismatch\""));
    assertTrue(json, json.contains("\"original_scene_type\":\"OriginalScene\""));
    assertTrue(json, json.contains("\"reopened_scene_type\":\"RenamedScene\""));
    assertTrue(json, json.contains("\"status\":\"reopened\""));
  }

  @Test
  public void sceneTypeMatchRetainsPassedVerification_reopenedStateJson() {
    String json = EatmeReopenProject.reopenedStateJson(true, true, "Scene", "Scene");

    assertTrue(json, json.contains("\"state_verification\": \"passed\""));
    assertTrue(json, json.contains("\"scene_type_matches\": true"));
    assertTrue(json, !json.contains("scene_type_mismatch"));
  }

  @Test
  public void sceneTypeMatchRetainsPassedVerification_resultJson() {
    EatmeReopenProject.ProjectReopen reopen = new EatmeReopenProject.ProjectReopen(
        "scene.myMethod", "Scene", "myMethod",
        "saved.a3p", "reopened.a3p", true, "Scene");

    String json = EatmeReopenProject.resultJson(reopen);

    assertTrue(json, json.contains("\"state_verification\":\"passed\""));
    assertTrue(json, !json.contains("scene_type_mismatch"));
  }

  private static Project projectWithScene() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static Project projectWithSceneMethod(String methodName) {
    Project project = projectWithScene();
    NamedUserType sceneType = (NamedUserType) project.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
    sceneType.methods.add(new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement(new Comment("reopen proof"))));
    return project;
  }
}
