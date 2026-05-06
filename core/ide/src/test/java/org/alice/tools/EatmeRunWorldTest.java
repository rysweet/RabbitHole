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
import static org.junit.Assert.assertTrue;

public class EatmeRunWorldTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void invokesSelectedSceneProcedureAndWritesEatmeProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("eatmeFirstLessonStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeRunWorld.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--run-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    String result = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(result, result.contains("\"schema_version\":\"eatme.alice-world-run-result/v1\""));
    assertTrue(result, result.contains("\"status\":\"ran\""));
    assertTrue(result, result.contains("\"run_artifact\":\"world-run.json\""));
    assertTrue(result, result.contains("\"runtime_or_log_evidence\":\"runtime.log\""));
    assertEquals("", stderr.toString(StandardCharsets.UTF_8));
    assertTrue(Files.size(evidenceDir.resolve("world-run.json")) > 0);
    assertTrue(Files.size(evidenceDir.resolve("runtime.log")) > 0);
    String runArtifact = Files.readString(evidenceDir.resolve("world-run.json"));
    assertTrue(runArtifact, runArtifact.contains("\"run_selector\": \"scene.eatmeFirstLessonStep\""));
    assertTrue(runArtifact, runArtifact.contains("\"executed_statement_count\": 2"));
    String runtimeLog = Files.readString(evidenceDir.resolve("runtime.log"));
    assertTrue(runtimeLog, runtimeLog.contains("executing:Comment"));
    assertTrue(runtimeLog, runtimeLog.contains("executed:Comment"));
  }

  @Test
  public void rejectsMissingSceneMethodWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithScene());
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeRunWorld.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--run-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("run selector does not name a scene method"));
    assertTrue(Files.notExists(evidenceDir.resolve("world-run.json")));
  }

  @Test
  public void rejectsUnsupportedRunSelectorWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("eatmeFirstLessonStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeRunWorld.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--run-selector", "program.main",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("unsupported run selector"));
    assertTrue(Files.notExists(evidenceDir.resolve("world-run.json")));
  }

  @Test
  public void rejectsDifferentSceneRunSelectorWithoutProofArtifacts() throws Exception {
    File projectFile = temporaryFolder.newFile("edited.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethod("otherStep"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeRunWorld.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--run-selector", "scene.otherStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("unsupported run selector"));
    assertTrue(Files.notExists(evidenceDir.resolve("world-run.json")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsArtifactPathThatNormalizesToEvidenceDirectory() {
    EatmeRunWorld.artifactPath(temporaryFolder.getRoot().toPath(), "foo/..");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentArtifactPath() {
    EatmeRunWorld.artifactPath(temporaryFolder.getRoot().toPath(), "../world-run.json");
  }

  @Test
  public void rejectsMissingProjectWithoutProofArtifacts() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeRunWorld.run(
        new String[] {
            "--project", temporaryFolder.getRoot().toPath().resolve("missing.a3p").toString(),
            "--run-selector", "scene.eatmeFirstLessonStep",
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("project file does not exist"));
    assertTrue(Files.notExists(evidenceDir.resolve("world-run.json")));
  }

  @Test
  public void escapesJsonControlCharacters() {
    assertEquals(
        "quote\\\" slash\\\\ backspace\\b formfeed\\f newline\\n return\\r tab\\t low\\u0001",
        EatmeRunWorld.escapeJson("quote\" slash\\ backspace\b formfeed\f newline\n return\r tab\t low\u0001"));
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
    sceneType.methods.add(new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement(new Comment("run proof"))));
    return project;
  }
}
