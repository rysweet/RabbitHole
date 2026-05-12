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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contract tests for EatmeEditProcedure edge cases not covered by the
 * primary FirstLessonCodeEditorActionProofTest or EatmeEditProcedureTest suites.
 * <p>
 * Covers: duplicate marker guard, escapeJson safety, argument parsing
 * validation, and the --json requirement.
 */
public class EatmeEditProcedureContractTest {
  private static final String FIRST_LESSON_TARGET = "scene.eatmeFirstLesson";
  private static final String MARKER = "wave4-code-editor-action-proof";
  private static final String ACTION_PROOF_ARTIFACT = "first-lesson-code-editor-action-proof.json";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // --- Duplicate marker guard ---

  @Test
  public void rejectsProjectWithExistingMarkerCommentInTargetMethod() throws Exception {
    File projectFile = temporaryFolder.newFile("duplicate-marker.a3p");
    Project project = projectWithSceneMethods("eatmeFirstLesson");
    UserMethod method = findMethod(sceneType(project), "eatmeFirstLesson");
    method.body.getValue().statements.add(new Comment(MARKER));
    IoUtilities.writeProject(projectFile, project);
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

    assertEquals("duplicate marker should be rejected", 2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("edit marker already exists in project"));
    assertFalse("no edited project on duplicate marker",
        Files.exists(evidenceDir.resolve("edited-project.a3p")));
    assertFalse("no proof artifact on duplicate marker",
        Files.exists(evidenceDir.resolve(ACTION_PROOF_ARTIFACT)));
  }

  @Test
  public void rejectsProjectWithExistingMarkerInNonTargetMethod() throws Exception {
    File projectFile = temporaryFolder.newFile("cross-marker.a3p");
    Project project = projectWithSceneMethods("eatmeFirstLesson", "otherProcedure");
    UserMethod otherMethod = findMethod(sceneType(project), "otherProcedure");
    otherMethod.body.getValue().statements.add(new Comment(MARKER));
    IoUtilities.writeProject(projectFile, project);
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

    assertEquals("marker in non-target method should also be rejected", 2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("edit marker already exists in project"));
  }

  // --- escapeJson safety ---

  @Test
  public void escapeJsonHandlesBackslashAndQuotes() {
    assertEquals("hello\\\\world", EatmeEditProcedure.escapeJson("hello\\world"));
    assertEquals("say \\\"hi\\\"", EatmeEditProcedure.escapeJson("say \"hi\""));
  }

  @Test
  public void escapeJsonHandlesControlCharacters() {
    assertEquals("line1\\nline2", EatmeEditProcedure.escapeJson("line1\nline2"));
    assertEquals("col1\\tcol2", EatmeEditProcedure.escapeJson("col1\tcol2"));
    assertEquals("cr\\r", EatmeEditProcedure.escapeJson("cr\r"));
    assertEquals("bs\\b", EatmeEditProcedure.escapeJson("bs\b"));
    assertEquals("ff\\f", EatmeEditProcedure.escapeJson("ff\f"));
  }

  @Test
  public void escapeJsonEscapesLowControlCharactersAsUnicodeSequences() {
    assertEquals("\\u0000", EatmeEditProcedure.escapeJson("\u0000"));
    assertEquals("\\u0001", EatmeEditProcedure.escapeJson("\u0001"));
    assertEquals("\\u001f", EatmeEditProcedure.escapeJson("\u001f"));
  }

  @Test
  public void escapeJsonPreservesNormalText() {
    assertEquals("eatmeFirstLesson", EatmeEditProcedure.escapeJson("eatmeFirstLesson"));
    assertEquals("wave4-code-editor-action-proof", EatmeEditProcedure.escapeJson("wave4-code-editor-action-proof"));
    assertEquals("", EatmeEditProcedure.escapeJson(""));
  }

  @Test
  public void escapeJsonBlocksJsonInjection() {
    String malicious = "\",\"injected\":\"true";
    String escaped = EatmeEditProcedure.escapeJson(malicious);
    // Every " in the output must be preceded by a backslash
    for (int i = 0; i < escaped.length(); i++) {
      if (escaped.charAt(i) == '"') {
        assertTrue("quote at index " + i + " must be escaped",
            i > 0 && escaped.charAt(i - 1) == '\\');
      }
    }
    // When embedded in a JSON value, the injected structure is neutralized
    String jsonValue = "\"" + escaped + "\"";
    assertFalse("injection payload must not create extra JSON fields",
        jsonValue.contains("\"injected\""));
  }

  // --- Issue #521: selector validation edge cases ---

  @Test
  public void acceptsMyFirstMethodSelectorForAfricaProject() throws Exception {
    File projectFile = temporaryFolder.newFile("africa-selector.a3p");
    Project project = projectWithSceneMethods("myFirstMethod");
    IoUtilities.writeProject(projectFile, project);
    Path evidenceDir = temporaryFolder.newFolder("africa-evidence").toPath();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.myFirstMethod",
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals(stderr.toString(StandardCharsets.UTF_8), 0, status);
    assertTrue(stdout.toString(StandardCharsets.UTF_8),
        stdout.toString(StandardCharsets.UTF_8).contains("\"status\":\"proved\""));
  }

  @Test
  public void rejectsEmptyMethodNameAfterScenePrefix() throws Exception {
    File projectFile = temporaryFolder.newFile("empty-name.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("empty-name-evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.",
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals("empty method name after scene. should be rejected", 2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("procedure selector must name one scene method"));
  }

  @Test
  public void rejectsSelectorWithHyphenInMethodName() throws Exception {
    File projectFile = temporaryFolder.newFile("hyphen-name.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("hyphen-evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.my-method",
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals("hyphenated method name should be rejected", 2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("procedure selector must name one scene method"));
  }

  @Test
  public void rejectsSelectorWithDotInMethodName() throws Exception {
    File projectFile = temporaryFolder.newFile("dotted-name.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("dotted-evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.my.method",
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals("dotted method name should be rejected", 2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("procedure selector must name one scene method"));
  }

  @Test
  public void rejectsNonexistentMethodWithTargetNotFoundError() throws Exception {
    File projectFile = temporaryFolder.newFile("no-method.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("no-method-evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", "scene.noSuchMethod",
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals("nonexistent method should be rejected", 2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("target procedure not found"));
  }

  // --- Argument parsing: missing --json ---

  @Test
  public void rejectsMissingJsonFlagWithStatus2() throws Exception {
    File projectFile = temporaryFolder.newFile("no-json-flag.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString()
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("--json is required"));
  }

  // --- Argument parsing: missing required args ---

  @Test
  public void rejectsMissingProjectArgWithStatus2() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("--project is required"));
  }

  @Test
  public void rejectsMissingProcedureSelectorArgWithStatus2() throws Exception {
    File projectFile = temporaryFolder.newFile("missing-selector.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("--procedure-selector is required"));
  }

  @Test
  public void rejectsMissingEditSpecArgWithStatus2() throws Exception {
    File projectFile = temporaryFolder.newFile("missing-edit.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("--edit-spec is required"));
  }

  @Test
  public void rejectsMissingEvidenceDirArgWithStatus2() throws Exception {
    File projectFile = temporaryFolder.newFile("missing-evidence.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--json"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("--evidence-dir is required"));
  }

  @Test
  public void rejectsUnknownArgumentWithStatus2() throws Exception {
    File projectFile = temporaryFolder.newFile("unknown-arg.a3p");
    IoUtilities.writeProject(projectFile, projectWithSceneMethods("eatmeFirstLesson"));
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    int status = EatmeEditProcedure.run(
        new String[] {
            "--project", projectFile.getAbsolutePath(),
            "--procedure-selector", FIRST_LESSON_TARGET,
            "--edit-spec", "append-comment:" + MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json",
            "--verbose"
        },
        new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(stderr));

    assertEquals(2, status);
    assertTrue(stderr.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8).contains("unknown argument: --verbose"));
  }

  // --- Helpers ---

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

  private static UserMethod findMethod(NamedUserType sceneType, String methodName) {
    return sceneType.getDeclaredMethods().stream()
        .filter(method -> methodName.equals(method.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("method not found: " + methodName));
  }
}
