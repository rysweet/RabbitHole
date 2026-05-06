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

public class EatmeSaveProjectTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void savesProjectAndWritesEatmeProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("eatmeFirstLessonStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeSaveProject.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--save-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"schema_version\":\"eatme.alice-project-save-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"saved\""));
    assertTrue(result, result.contains("\"saved_project_artifact\":\"saved-project.a3p\""));
    assertTrue(result, result.contains("\"save_artifact\":\"project-save.json\""));
    assertEquals("", stderr.toString(StandardCharsets.UTF_8));
    assertTrue(Files.size(evidenceDir.resolve("saved-project.a3p")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("project-save.json")) > 0);
    String saveArtifact = Files.readString(evidenceDir.resolve("project-save.json"));
    assertTrue(saveArtifact, saveArtifact.contains("\"save_mode\": \"headless_project_persistence_roundtrip\""));

    Project savedProject = IoUtilities.readProject(evidenceDir.resolve("saved-project.a3p").toFile());
    NamedUserType sceneType = (NamedUserType) savedProject.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
    UserMethod method = sceneType.getDeclaredMethods().stream()
        .filter(candidate -> "eatmeFirstLessonStep".equals(candidate.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull("saved project should retain the selected scene method", method);
  }

  @Test
  public void rejectsDifferentSceneSaveSelectorWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("otherStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeSaveProject.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--save-selector", "scene.otherStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("unsupported save selector"));
    assertTrue(Files.notExists(evidenceDir.resolve("saved-project.a3p")));
  }

  @Test
  public void rejectsMissingSceneMethodWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeSaveProject.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--save-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("save selector does not name a scene method"));
    assertTrue(Files.notExists(evidenceDir.resolve("saved-project.a3p")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsArtifactPathThatNormalizesToEvidenceDirectory() {
    EatmeSaveProject.artifactPath(temporaryFolder.getRoot().toPath(), "foo/..");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentArtifactPath() {
    EatmeSaveProject.artifactPath(temporaryFolder.getRoot().toPath(), "../saved-project.a3p");
  }

  @Test
  public void rejectsMissingProjectWithoutProofArtifacts() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeSaveProject.run(
        new String[] {
            "--project", temporaryFolder.getRoot().toPath().resolve("missing.a3p").toString(),
            "--save-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("project file does not exist"));
    assertTrue(Files.notExists(evidenceDir.resolve("saved-project.a3p")));
  }

  @Test
  public void escapesJsonControlCharacters() {
    assertEquals(
        "quote\\\" slash\\\\ backspace\\b formfeed\\f newline\\n return\\r tab\\t low\\u0001",
        EatmeSaveProject.escapeJson("quote\" slash\\ backspace\b formfeed\f newline\n return\r tab\t low\u0001"));
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
    sceneType.methods.add(new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement(new Comment("save proof"))));
    return project;
  }
}
