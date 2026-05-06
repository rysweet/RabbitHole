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
    assertTrue(result, result.contains("\"procedure_or_code_diff\":\"procedure.diff.json\""));
    assertTrue(Files.size(evidenceDir.resolve("procedure-edit.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("procedure.diff.json")) > 0);

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
    sceneType.methods.add(new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement(new Comment("existing"))));
    return project;
  }
}
