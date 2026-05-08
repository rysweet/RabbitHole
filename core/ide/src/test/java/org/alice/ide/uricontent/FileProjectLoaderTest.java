package org.alice.ide.uricontent;

import org.lgna.project.Project;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class FileProjectLoaderTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void savedTemporaryProjectLoadsAndCorruptTemporaryProjectIsRejected() throws Exception {
    File savedProject = temporaryFolder.newFile("saved-generated-world.a3p");
    Project project = new Project(programType("GeneratedProgram"), Project.SceneCameraType.WindowCamera);
    IoUtilities.writeProject(savedProject, project);
    FileProjectLoader savedLoader = new FileProjectLoader(savedProject);

    Project loadedProject = savedLoader.load();

    assertNotNull(loadedProject);
    assertEquals("GeneratedProgram", loadedProject.getProgramType().getName());
    assertEquals(Project.SceneCameraType.WindowCamera, loadedProject.createSaveManifest().projectStructure.sceneCameraType);

    File corruptProject = temporaryFolder.newFile("corrupt-generated-world.a3p");
    Files.writeString(corruptProject.toPath(), "not an Alice project archive", StandardCharsets.UTF_8);
    FileProjectLoader corruptLoader = new FileProjectLoader(corruptProject);

    Project rejectedProject = corruptLoader.load();

    assertNull(rejectedProject);
  }

  @Test
  public void loadDelegatesIoFailureToHookAndReturnsNull() throws IOException {
    File corruptProject = temporaryFolder.newFile("corrupt-project.a3p");
    CapturingFileProjectLoader loader = new CapturingFileProjectLoader(corruptProject);

    Project project = loader.load();

    assertNull(project);
    assertEquals(corruptProject, loader.file);
    assertTrue(loader.exception instanceof IOException);
  }

  @Test
  public void nonVrLoaderUsesOriginalProjectUriAndDoesNotRequireSaveWhenFileExists() throws IOException {
    File project = temporaryFolder.newFile("saved-project.a3p");
    FileProjectLoader loader = new FileProjectLoader(project);

    assertEquals(project.toURI(), loader.getUri());
    assertFalse(loader.shouldBeSaved());
  }

  @Test
  public void vrReadyLoaderUsesRenamedProjectUriAndRequiresSaveWhenVrCopyDoesNotExist() throws IOException {
    File project = temporaryFolder.newFile("saved-project.a3p");
    File vrProject = vrProjectFor(project);
    vrProject.delete();
    FileProjectLoader loader = new FileProjectLoader(project, true);

    assertEquals(vrProject.toURI(), loader.getUri());
    assertTrue(loader.shouldBeSaved());
  }

  @Test
  public void vrReadyLoaderDoesNotRequireSaveWhenVrCopyAlreadyExists() throws IOException {
    File project = temporaryFolder.newFile("saved-project.a3p");
    File vrProject = vrProjectFor(project);
    assertTrue(vrProject.createNewFile());
    FileProjectLoader loader = new FileProjectLoader(project, true);

    assertEquals(vrProject.toURI(), loader.getUri());
    assertFalse(loader.shouldBeSaved());
  }

  @Test
  public void normalProjectFileIsNotBackupAndIsItsOwnMainProjectFile() throws IOException {
    File project = temporaryFolder.newFile("world.a3p");
    FileProjectLoader loader = new FileProjectLoader(project);

    assertFalse(loader.isBackup());
    assertFalse(loader.isDefaultBackup());
    assertEquals(project, loader.getMainProjectFile());
  }

  @Test
  public void namedBackupFileDerivesMainProjectFileFromBackupDirectoryName() throws IOException {
    File backupDirectory = temporaryFolder.newFolder("world.bak");
    File backup = new File(backupDirectory, "auto20240102_120000.a3p");
    assertTrue(backup.createNewFile());
    FileProjectLoader loader = new FileProjectLoader(backup);

    assertTrue(loader.isBackup());
    assertFalse(loader.isDefaultBackup());
    assertEquals(new File(temporaryFolder.getRoot(), "world.a3p"), loader.getMainProjectFile());
  }

  @Test
  public void defaultBackupFileIsBackupWithoutMainProjectFile() throws IOException {
    File defaultBackupDirectory = temporaryFolder.newFolder(".defaultbak");
    File defaultBackup = new File(defaultBackupDirectory, "auto20240102_120000.a3p");
    assertTrue(defaultBackup.createNewFile());
    FileProjectLoader loader = new FileProjectLoader(defaultBackup);

    assertTrue(loader.isBackup());
    assertTrue(loader.isDefaultBackup());
    assertNull(loader.getMainProjectFile());
  }

  @Test
  public void newProjectLoaderIsNotBackupAndHasNoMainProjectFile() {
    UriProjectLoader loader = new NewProjectLoader();

    assertFalse(loader.isBackup());
    assertFalse(loader.isDefaultBackup());
    assertNull(loader.getMainProjectFile());
  }

  private static File vrProjectFor(File project) {
    String source = project.getAbsolutePath();
    return new File(source.substring(0, source.length() - 4) + " VR" + source.substring(source.length() - 4));
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static class NewProjectLoader extends UriProjectLoader {
    NewProjectLoader() {
      super(false);
    }

    @Override
    public URI getUri() {
      return URI.create("blank://project");
    }

    @Override
    public boolean isNewProject() {
      return true;
    }

    @Override
    protected Project load() {
      throw new AssertionError("load is outside UriProjectLoader path classification");
    }
  }

  private static class CapturingFileProjectLoader extends AbstractFileProjectLoader {
    private File file;
    private Exception exception;

    CapturingFileProjectLoader(File file) {
      super(file, false);
    }

    @Override
    public URI getUri() {
      return getFile().toURI();
    }

    @Override
    protected void handleLoadException(File file, Exception e) {
      this.file = file;
      this.exception = e;
    }

    @Override
    public boolean isNewProject() {
      return false;
    }
  }
}
