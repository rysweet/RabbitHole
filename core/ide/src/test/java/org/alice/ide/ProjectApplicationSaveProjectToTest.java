package org.alice.ide;

import org.alice.ide.frametitle.IdeFrameTitleGenerator;
import org.alice.ide.project.ProjectDocumentState;
import org.alice.ide.projecturi.ProjectSnapshot;
import org.alice.ide.projecturi.RecentProjectCountState;
import org.alice.ide.recentprojects.RecentProjectsListData;
import org.alice.ide.uricontent.FileProjectLoader;
import org.alice.ide.uricontent.UriProjectLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.croquet.Application;
import org.lgna.croquet.Operation;
import org.lgna.croquet.State;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;

public class ProjectApplicationSaveProjectToTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void productionUpdateTitleFailsFastWhenDocumentFrameIsMissing() throws Exception {
    TestProjectApplication application = applicationWith(projectNamed("FrameInvariantProgram"), new InMemoryProjectLoader());

    NullPointerException thrown = assertThrows(NullPointerException.class, application::callProductionUpdateTitle);
    assertEquals("ProjectApplication requires documentFrame before updating title", thrown.getMessage());
  }

  @Test
  public void saveNewProjectToFileWritesArchiveAdoptsTargetAndRecordsRecentProject() throws Exception {
    Project project = projectNamed("NewProgram");
    TestProjectApplication application = applicationWith(project, new InMemoryProjectLoader());
    File target = new File(temporaryFolder.getRoot(), "new-world.a3p");
    RecentProjectCountState.getInstance().setValueTransactionlessly(10);

    application.saveProjectTo(target);

    assertReadableProject(target, "NewProgram");
    assertEquals(target.toURI(), application.getUri());
    assertEquals(target.getCanonicalFile(), application.getMainProjectFile().getCanonicalFile());
    assertFalse(application.isNewProject());
    assertFalse(application.isBackup());
    assertEquals(target.toURI(), RecentProjectsListData.getInstance().toArray(ProjectSnapshot.class)[0].getUri());
  }

  @Test
  public void saveOverCurrentTargetKeepsDefaultTargetAndWritesSaveBackup() throws Exception {
    File currentFile = new File(temporaryFolder.getRoot(), "current-world.a3p");
    IoUtilities.writeProject(currentFile, projectNamed("CurrentProgramBeforeSave"));
    TestProjectApplication application = applicationWith(
        projectNamed("CurrentProgramAfterSave"),
        new FileProjectLoader(currentFile));

    application.saveProjectTo(currentFile);

    assertReadableProject(currentFile, "CurrentProgramAfterSave");
    assertEquals(currentFile.toURI(), application.getUri());
    assertEquals(currentFile.getCanonicalFile(), application.getMainProjectFile().getCanonicalFile());
    assertFalse(application.isBackup());
    File[] saveBackups = saveBackupsIn(new File(temporaryFolder.getRoot(), "current-world.bak"));
    assertEquals(1, saveBackups.length);
    assertReadableProject(saveBackups[0], "CurrentProgramAfterSave");
  }

  @Test
  public void saveAsDifferentFileSwitchesDefaultTargetWithoutChangingOriginalArchive() throws Exception {
    File originalFile = new File(temporaryFolder.getRoot(), "original-world.a3p");
    File saveAsFile = new File(temporaryFolder.getRoot(), "renamed-world.a3p");
    IoUtilities.writeProject(originalFile, projectNamed("OriginalProgram"));
    TestProjectApplication application = applicationWith(
        projectNamed("RenamedProgram"),
        new FileProjectLoader(originalFile));

    application.saveProjectTo(saveAsFile);

    assertReadableProject(saveAsFile, "RenamedProgram");
    assertReadableProject(originalFile, "OriginalProgram");
    assertEquals(saveAsFile.toURI(), application.getUri());
    assertEquals(saveAsFile.getCanonicalFile(), application.getMainProjectFile().getCanonicalFile());
    assertFalse("Save-as should not leave the original file as the active save target",
        originalFile.getCanonicalFile().equals(application.getMainProjectFile().getCanonicalFile()));
    File[] newTargetBackups = saveBackupsIn(new File(temporaryFolder.getRoot(), "renamed-world.bak"));
    assertEquals(1, newTargetBackups.length);
    assertReadableProject(newTargetBackups[0], "RenamedProgram");
    assertFalse(new File(temporaryFolder.getRoot(), "original-world.bak").exists());
  }

  @Test
  public void failedSaveAsKeepsPreviousTargetAndOriginalArchive() throws Exception {
    File originalFile = new File(temporaryFolder.getRoot(), "stable-world.a3p");
    IoUtilities.writeProject(originalFile, projectNamed("StableProgram"));
    TestProjectApplication application = applicationWith(
        projectNamed("UnsavedProgram"),
        new FileProjectLoader(originalFile));
    File invalidTarget = temporaryFolder.newFolder("directory-cannot-be-archive.a3p");

    assertThrows(IOException.class, () -> application.saveProjectTo(invalidTarget));

    assertReadableProject(originalFile, "StableProgram");
    assertEquals(originalFile.toURI(), application.getUri());
    assertEquals(originalFile.getCanonicalFile(), application.getMainProjectFile().getCanonicalFile());
    assertFalse(application.isBackup());
    assertFalse(new File(temporaryFolder.getRoot(), "directory-cannot-be-archive.bak").exists());
  }

  @Test
  public void saveOverBackupTargetKeepsBackupLoaderAndDoesNotCreateDefaultSaveBackup() throws Exception {
    File backupDirectory = temporaryFolder.newFolder("backup-source.bak");
    File backupFile = new File(backupDirectory, "auto20240102_120000.a3p");
    IoUtilities.writeProject(backupFile, projectNamed("BackupBeforeSave"));
    TestProjectApplication application = applicationWith(
        projectNamed("BackupAfterSave"),
        new FileProjectLoader(backupFile));

    application.saveProjectTo(backupFile);

    assertReadableProject(backupFile, "BackupAfterSave");
    assertEquals(backupFile.toURI(), application.getUri());
    assertTrue(application.isBackup());
    assertEquals(new File(temporaryFolder.getRoot(), "backup-source.a3p").getCanonicalFile(),
        application.getMainProjectFile().getCanonicalFile());
    assertEquals(0, saveBackupsIn(backupDirectory).length);
  }

  private static TestProjectApplication applicationWith(Project project, UriProjectLoader loader) throws Exception {
    resetApplicationSingleton();
    clearProjectDocumentListeners();
    TestProjectApplication application = new TestProjectApplication(project);
    installLoader(application, loader);
    return application;
  }

  private static void resetApplicationSingleton() throws Exception {
    Field field = Application.class.getDeclaredField("singleton");
    field.setAccessible(true);
    field.set(null, null);
  }

  private static void clearProjectDocumentListeners() throws Exception {
    Field field = State.class.getDeclaredField("newSchoolValueListeners");
    field.setAccessible(true);
    ((List<?>) field.get(ProjectDocumentState.getInstance())).clear();
  }

  private static void installLoader(ProjectApplication application, UriProjectLoader loader) throws Exception {
    Field field = ProjectApplication.class.getDeclaredField("uriProjectLoader");
    field.setAccessible(true);
    field.set(application, loader);
  }

  private static void assertReadableProject(File file, String expectedProgramName) throws Exception {
    assertTrue(file.isFile());
    assertTrue("Expected saved archive to contain bytes", file.length() > 0);
    assertEquals(expectedProgramName, IoUtilities.readProject(file).getProgramType().getName());
  }

  private static File[] saveBackupsIn(File directory) {
    File[] backups = directory.listFiles(file -> file.isFile()
        && file.getName().startsWith("save")
        && file.getName().endsWith(".a3p"));
    assertNotNull(backups);
    return backups;
  }

  private static Project projectNamed(String name) {
    return new Project(programType(name), Project.SceneCameraType.WindowCamera);
  }

  private static NamedUserType programType(String name) {
    return AstUtilities.createType(name, JavaType.getInstance(SProgram.class));
  }

  private static final class TestProjectApplication extends ProjectApplication {
    TestProjectApplication(Project project) {
      super((ProjectDocumentFrame) null);
      setProject(project);
    }

    @Override
    protected IdeFrameTitleGenerator createFrameTitleGenerator() {
      return (projectLoader, isDocumentUpToDateWithUri) -> "ProjectApplicationSaveProjectToTest";
    }

    @Override
    protected void updateTitle() {
    }

    void callProductionUpdateTitle() {
      super.updateTitle();
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
      return "project-application-save-project-to-test";
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
