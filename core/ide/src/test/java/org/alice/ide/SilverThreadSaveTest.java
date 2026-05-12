package org.alice.ide;

import org.alice.ide.uricontent.UriProjectLoader;
import org.alice.ide.frametitle.IdeFrameTitleGenerator;
import org.alice.ide.projecturi.RecentProjectCountState;
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
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread end-to-end save test: proves that an AST modification survives
 * the full production save path ({@code ProjectApplication.saveProjectTo})
 * and round-trip reopen.
 *
 * <p>This test is distinct from the existing silver thread tests:
 * <ul>
 *   <li>{@code SilverThreadLaunchBuildRunTest} injects AST modifications but
 *       saves with raw {@code IoUtilities.writeProject}.</li>
 *   <li>{@code ProjectApplicationSaveProjectToTest} exercises the production save
 *       path but does not inject AST modifications.</li>
 *   <li><b>This test combines both</b>: injects a {@link Comment}, saves through
 *       {@code ProjectApplication.saveProjectTo}, and verifies the modification
 *       survives the full round-trip.</li>
 * </ul>
 *
 * <p>Headless: no JavaFX, no 3D rendering, no gallery assets.
 */
public class SilverThreadSaveTest {

  private static final String PROGRAM_NAME = "SilverThreadSaveProgram";
  private static final String METHOD_NAME = "silverThreadSaveStep";
  private static final String COMMENT_TEXT = "silver-thread-save-proof";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  // ── Test: Inject Comment → saveProjectTo → reopen → verify round-trip ──

  @Test
  public void modifiedProjectSurvivesProductionSaveAndRoundTrip() throws Exception {
    // Step 1: Build a project with an identifiable AST modification
    NamedUserType programType = AstUtilities.createType(
        PROGRAM_NAME, JavaType.getInstance(SProgram.class));
    Comment commentStatement = new Comment(COMMENT_TEXT);
    UserMethod storyMethod = new UserMethod(
        METHOD_NAME,
        Void.TYPE,
        new UserParameter[0],
        new BlockStatement(commentStatement));
    storyMethod.isStatic.setValue(true);
    programType.methods.add(storyMethod);
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);

    // Step 2: Bootstrap TestProjectApplication with the modified project
    TestProjectApplication application = applicationWith(project, new InMemoryProjectLoader());

    // Step 3: Save through the production path (ProjectApplication.saveProjectTo)
    File savedFile = new File(temporaryFolder.getRoot(), "silver-thread-save.a3p");
    RecentProjectCountState.getInstance().setValueTransactionlessly(10);
    application.saveProjectTo(savedFile);

    // Step 4: Verify file exists on disk and is non-empty
    assertTrue("Saved project file must exist on disk", savedFile.isFile());
    assertTrue("Saved project file must be non-empty", savedFile.length() > 0);

    // Step 5: Reopen the saved project and verify the Comment text survived
    Project reopenedProject = IoUtilities.readProject(savedFile);
    assertNotNull("Reopened project must not be null", reopenedProject);
    assertEquals("Program type name must survive round-trip",
        PROGRAM_NAME, reopenedProject.getProgramType().getName());

    UserMethod reopenedMethod = findMethodByName(
        reopenedProject.getProgramType(), METHOD_NAME);
    assertNotNull("Method '" + METHOD_NAME + "' must survive serialization round-trip",
        reopenedMethod);

    BlockStatement body = (BlockStatement) reopenedMethod.getBodyProperty().getValue();
    assertNotNull("Method body must not be null after deserialization", body);
    assertEquals("Method body must contain exactly one statement",
        1, body.statements.size());

    Statement firstStatement = body.statements.get(0);
    assertTrue("First statement must be a Comment", firstStatement instanceof Comment);
    assertEquals("Comment text must survive production save round-trip",
        COMMENT_TEXT, ((Comment) firstStatement).text.getValue());
  }

  // ── Test: Load indiaMinimum.a3p → save → verify non-zero file size ───

  @Test
  public void verifyFileSizeIsNonZero() throws Exception {
    // Copy bundled starter project from classpath to a temp file
    File starterFile = new File(temporaryFolder.getRoot(), "indiaMinimum.a3p");
    try (InputStream in = getClass().getResourceAsStream("/starters/indiaMinimum.a3p")) {
      assertNotNull("indiaMinimum.a3p must be on the test classpath", in);
      Files.copy(in, starterFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Load the starter project
    Project loadedProject = IoUtilities.readProject(starterFile);
    assertNotNull("Loaded project must not be null", loadedProject);

    // Save to a new temp file
    File savedFile = new File(temporaryFolder.getRoot(), "indiaMinimum-saved.a3p");
    IoUtilities.writeProject(savedFile, loadedProject);

    // Assert the saved file exists and has non-zero size
    assertTrue("Saved file must exist on disk", savedFile.isFile());
    assertTrue("Saved file size must be greater than zero", savedFile.length() > 0);
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private static UserMethod findMethodByName(NamedUserType type, String name) {
    for (UserMethod method : type.getDeclaredMethods()) {
      if (name.equals(method.getName())) {
        return method;
      }
    }
    return null;
  }

  private static TestProjectApplication applicationWith(
      Project project, UriProjectLoader loader) throws Exception {
    resetApplicationSingleton();
    TestProjectApplication application = new TestProjectApplication(project);
    installLoader(application, loader);
    return application;
  }

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

  // ── Test doubles (same pattern as ProjectApplicationSaveProjectToTest) ─

  private static final class TestProjectApplication extends ProjectApplication {
    TestProjectApplication(Project project) {
      super((ProjectDocumentFrame) null);
      setProject(project);
    }

    @Override
    protected IdeFrameTitleGenerator createFrameTitleGenerator() {
      return (projectLoader, isDocumentUpToDateWithUri) -> "SilverThreadSaveTest";
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
      return "silver-thread-save-test";
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
