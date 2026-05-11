package org.alice.ide;

import org.alice.ide.frametitle.IdeFrameTitleGenerator;
import org.alice.ide.projecturi.RecentProjectCountState;
import org.alice.ide.uricontent.UriProjectLoader;
import org.alice.tools.EatmeEditProcedure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.croquet.Application;
import org.lgna.croquet.Operation;
import org.lgna.croquet.history.UserActivity;
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

import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread end-to-end test: chains {@link EatmeEditProcedure} edit →
 * production save via {@code ProjectApplication.saveProjectTo} → readback,
 * proving that an edit injected by the CLI tool survives the full production
 * save round-trip.
 *
 * <p>Combines patterns from:
 * <ul>
 *   <li>{@link SilverThreadSaveTest} — production save via TestProjectApplication</li>
 *   <li>{@code EatmeEditProcedureTest} — CLI edit invocation and readback</li>
 * </ul>
 *
 * <p>Headless: no JavaFX, no 3D rendering, no gallery assets.
 *
 * <p><strong>Prerequisite:</strong> {@code EatmeEditProcedure.run()} must be
 * {@code public} (not package-private) for this cross-package test to compile.
 */
public class SilverThreadEditSaveReadbackTest {

  private static final String METHOD_NAME = "eatmeFirstLesson";
  private static final String COMMENT_MARKER = "silver-thread-edit-save-readback-proof";
  private static final String PROOF_SCHEMA = "eatme.alice-edit-save-readback-proof/v1";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // ── Main chain test: Edit → Save → Readback ──────────────────────────

  @Test
  public void editedCommentSurvivesProductionSaveAndReadback() throws Exception {
    // ── Step 1: Build starter project with Scene.eatmeFirstLesson ──
    Project starterProject = projectWithSceneMethod(METHOD_NAME);
    File starterFile = temporaryFolder.newFile("starter.a3p");
    IoUtilities.writeProject(starterFile, starterProject);
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();

    // ── Step 2: Run EatmeEditProcedure to append marker comment ──
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    int editStatus = EatmeEditProcedure.run(
        new String[] {
            "--project", starterFile.getAbsolutePath(),
            "--procedure-selector", "scene." + METHOD_NAME,
            "--edit-spec", "append-comment:" + COMMENT_MARKER,
            "--evidence-dir", evidenceDir.toString(),
            "--json"
        },
        new PrintStream(stdout),
        new PrintStream(stderr));

    assertEquals("EatmeEditProcedure.run() must return 0; stderr: "
        + stderr.toString(StandardCharsets.UTF_8), 0, editStatus);

    // ── Step 3: Verify edited-project.a3p exists ──
    Path editedProjectPath = evidenceDir.resolve("edited-project.a3p");
    assertTrue("edited-project.a3p must exist after edit",
        Files.isRegularFile(editedProjectPath));
    assertTrue("edited-project.a3p must be non-empty",
        Files.size(editedProjectPath) > 0);

    // ── Step 4: Read back and verify marker in edited project ──
    Project editedProject = IoUtilities.readProject(editedProjectPath.toFile());
    NamedUserType editedSceneType = findSceneType(editedProject);
    UserMethod editedMethod = findMethodByName(editedSceneType, METHOD_NAME);
    assertNotNull("edited project must contain " + METHOD_NAME, editedMethod);
    BlockStatement editedBody = editedMethod.body.getValue();
    assertNotNull("edited method body must not be null", editedBody);
    assertEquals("edited method should have 2 statements (existing + marker)",
        2, editedBody.statements.size());
    Statement markerStatement = editedBody.statements.get(1);
    assertTrue("second statement must be a Comment", markerStatement instanceof Comment);
    assertEquals("marker comment text must match",
        COMMENT_MARKER, ((Comment) markerStatement).text.getValue());

    // ── Step 5: Reset singleton (EatmeEditProcedure installed an anonymous Application) ──
    resetApplicationSingleton();

    // ── Step 6: Bootstrap TestProjectApplication and save via production path ──
    TestProjectApplication application = new TestProjectApplication(editedProject);
    installLoader(application, new InMemoryProjectLoader());
    File savedFile = new File(temporaryFolder.getRoot(), "production-save.a3p");
    RecentProjectCountState.getInstance().setValueTransactionlessly(10);
    application.saveProjectTo(savedFile);

    assertTrue("production-saved file must exist on disk", savedFile.isFile());
    assertTrue("production-saved file must be non-empty", savedFile.length() > 0);

    // ── Step 7: Readback — verify marker survived full chain ──
    Project reopenedProject = IoUtilities.readProject(savedFile);
    assertNotNull("reopened project must not be null", reopenedProject);
    NamedUserType reopenedSceneType = findSceneType(reopenedProject);
    UserMethod reopenedMethod = findMethodByName(reopenedSceneType, METHOD_NAME);
    assertNotNull("reopened project must contain " + METHOD_NAME, reopenedMethod);

    BlockStatement reopenedBody = reopenedMethod.body.getValue();
    assertNotNull("reopened method body must not be null", reopenedBody);
    assertEquals("reopened method should have 2 statements (existing + marker)",
        2, reopenedBody.statements.size());

    Statement firstStatement = reopenedBody.statements.get(0);
    assertTrue("first statement must be original Comment", firstStatement instanceof Comment);
    assertEquals("original comment must survive",
        "existing", ((Comment) firstStatement).text.getValue());

    Statement secondStatement = reopenedBody.statements.get(1);
    assertTrue("second statement must be marker Comment", secondStatement instanceof Comment);
    assertEquals("marker comment must survive production save round-trip",
        COMMENT_MARKER, ((Comment) secondStatement).text.getValue());

    // ── Step 8: Write proof artifact ──
    Path proofPath = evidenceDir.resolve("edit-save-readback-proof.json");
    String proofJson = proofArtifactJson(
        starterFile.getName(),
        editedProjectPath.getFileName().toString(),
        savedFile.getName(),
        editStatus,
        savedFile.length(),
        COMMENT_MARKER);
    Files.writeString(proofPath, proofJson, StandardCharsets.UTF_8);
    assertTrue("proof artifact must exist", Files.isRegularFile(proofPath));
    assertTrue("proof artifact must be non-empty", Files.size(proofPath) > 0);

    String proof = Files.readString(proofPath);
    assertTrue("proof must contain schema version",
        proof.contains("\"schema_version\": \"" + PROOF_SCHEMA + "\""));
    assertTrue("proof must record chain_step edit",
        proof.contains("\"chain_step\": \"edit\""));
    assertTrue("proof must record chain_step production-save",
        proof.contains("\"chain_step\": \"production-save\""));
    assertTrue("proof must record chain_step readback",
        proof.contains("\"chain_step\": \"readback\""));
  }

  // ── AST helpers (from EatmeEditProcedureTest pattern) ─────────────────

  private static Project projectWithSceneMethod(String methodName) {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    sceneType.methods.add(new UserMethod(
        methodName, JavaType.VOID_TYPE, new UserParameter[0],
        new BlockStatement(new Comment("existing"))));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static NamedUserType findSceneType(Project project) {
    return (NamedUserType) project.getProgramType()
        .getDeclaredFields()
        .get(0)
        .getValueType();
  }

  private static UserMethod findMethodByName(NamedUserType type, String name) {
    for (UserMethod method : type.getDeclaredMethods()) {
      if (name.equals(method.getName())) {
        return method;
      }
    }
    return null;
  }

  // ── Application singleton helpers (from SilverThreadSaveTest pattern) ─

  private static void resetApplicationSingleton() throws Exception {
    Field field = Application.class.getDeclaredField("singleton");
    field.setAccessible(true);
    field.set(null, null);
  }

  private static void installLoader(
      ProjectApplication application, UriProjectLoader loader) throws Exception {
    Field field = ProjectApplication.class.getDeclaredField("uriProjectLoader");
    field.setAccessible(true);
    field.set(application, loader);
  }

  // ── Proof artifact ────────────────────────────────────────────────────

  private static String proofArtifactJson(
      String starterArtifact, String editedArtifact, String savedArtifact,
      int editStatus, long savedFileSize, String marker) {
    return "{\n"
        + "  \"schema_version\": \"" + PROOF_SCHEMA + "\",\n"
        + "  \"status\": \"proved\",\n"
        + "  \"chain\": [\n"
        + "    {\n"
        + "      \"chain_step\": \"edit\",\n"
        + "      \"tool\": \"EatmeEditProcedure\",\n"
        + "      \"input\": \"" + escapeJson(starterArtifact) + "\",\n"
        + "      \"output\": \"" + escapeJson(editedArtifact) + "\",\n"
        + "      \"exit_status\": " + editStatus + "\n"
        + "    },\n"
        + "    {\n"
        + "      \"chain_step\": \"production-save\",\n"
        + "      \"tool\": \"ProjectApplication.saveProjectTo\",\n"
        + "      \"input\": \"" + escapeJson(editedArtifact) + "\",\n"
        + "      \"output\": \"" + escapeJson(savedArtifact) + "\",\n"
        + "      \"saved_file_size\": " + savedFileSize + "\n"
        + "    },\n"
        + "    {\n"
        + "      \"chain_step\": \"readback\",\n"
        + "      \"tool\": \"IoUtilities.readProject\",\n"
        + "      \"input\": \"" + escapeJson(savedArtifact) + "\",\n"
        + "      \"marker_found\": \"" + escapeJson(marker) + "\",\n"
        + "      \"statement_count\": 2\n"
        + "    }\n"
        + "  ],\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"full first-lesson completion\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"grading\",\n"
        + "    \"creative assessment\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"broad UI automation\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String escapeJson(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '\\' -> escaped.append("\\\\");
        case '"' -> escaped.append("\\\"");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> escaped.append(ch);
      }
    }
    return escaped.toString();
  }

  // ── Test doubles (from SilverThreadSaveTest pattern) ──────────────────

  private static final class TestProjectApplication extends ProjectApplication {
    TestProjectApplication(Project project) {
      super((ProjectDocumentFrame) null);
      setProject(project);
    }

    @Override
    protected IdeFrameTitleGenerator createFrameTitleGenerator() {
      return (projectLoader, isDocumentUpToDateWithUri) -> "SilverThreadEditSaveReadbackTest";
    }

    @Override
    protected void updateTitle() {
    }

    @Override
    protected BufferedImage createThumbnail() {
      return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void forceProjectCodeUpToDate() {
    }

    @Override
    public void ensureProjectCodeUpToDate() {
    }

    @Override
    protected Operation getAboutOperation() {
      return null;
    }

    @Override
    protected Operation getPreferencesOperation() {
      return null;
    }

    @Override
    protected void handleOpenFiles(List<File> files) {
    }

    @Override
    protected void handleWindowOpened(WindowEvent e) {
    }

    @Override
    public void handleQuit(UserActivity activity) {
    }

    @Override
    public String getApplicationSubPath() {
      return "silver-thread-edit-save-readback-test";
    }
  }

  private static final class InMemoryProjectLoader extends UriProjectLoader {
    InMemoryProjectLoader() {
      super(false);
    }

    @Override
    public URI getUri() {
      return null;
    }

    @Override
    protected Project load() {
      return null;
    }

    @Override
    public boolean isNewProject() {
      return false;
    }

    @Override
    public boolean isBackup() {
      return false;
    }

    @Override
    public boolean isDefaultBackup() {
      return false;
    }

    @Override
    public File getMainProjectFile() {
      return null;
    }
  }
}
