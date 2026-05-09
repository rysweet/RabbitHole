package org.alice.ide;

import org.alice.ide.uricontent.FileProjectLoader;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProjectOpenSaveExportJourneyTest {
  private static final String PROGRAM_NAME = "ClassroomProgram";

  @Test
  public void loadedProjectCanBeSavedAgainAndExportedWithoutUserInterface() throws Exception {
    Path workingDirectory = Files.createDirectories(Path.of(
        "target",
        "headless-project-journey",
        UUID.randomUUID().toString()));
    File originalProjectFile = workingDirectory.resolve("classroom.a3p").toFile();
    File savedProjectFile = workingDirectory.resolve("classroom-copy.a3p").toFile();
    File exportedProjectFile = workingDirectory.resolve("classroom-export.a3w").toFile();
    Project originalProject = new Project(programType(PROGRAM_NAME), Project.SceneCameraType.WindowCamera);

    IoUtilities.writeProject(originalProjectFile, originalProject);
    Project loadedProject = new TestFileProjectLoader(originalProjectFile).loadNow();
    assertNotNull(loadedProject);
    assertEquals(PROGRAM_NAME, loadedProject.getProgramType().getName());

    IoUtilities.writeProject(savedProjectFile, loadedProject);
    assertTrue(savedProjectFile.isFile());
    Project savedProject = new TestFileProjectLoader(savedProjectFile).loadNow();
    assertNotNull(savedProject);
    assertEquals(PROGRAM_NAME, savedProject.getProgramType().getName());

    IoUtilities.exportProject(exportedProjectFile, savedProject);
    assertTrue(exportedProjectFile.isFile());
    Project exportedProject = IoUtilities.readProject(exportedProjectFile);
    assertNotNull(exportedProject);
    assertEquals(PROGRAM_NAME, exportedProject.getProgramType().getName());
  }

  @Test
  public void classroomProjectWithSpacesInFileNameCanBeSavedAndExportedHeadlessly() throws Exception {
    Path workingDirectory = Files.createDirectories(Path.of(
        "target",
        "headless-project-journey",
        UUID.randomUUID().toString()));
    File originalProjectFile = workingDirectory.resolve("classroom unit one.a3p").toFile();
    File savedProjectFile = workingDirectory.resolve("classroom unit one copy.a3p").toFile();
    File exportedProjectFile = workingDirectory.resolve("classroom unit one export.a3w").toFile();
    Project originalProject = new Project(programType("ClassroomUnitProgram"), Project.SceneCameraType.WindowCamera);

    IoUtilities.writeProject(originalProjectFile, originalProject);
    Project loadedProject = new TestFileProjectLoader(originalProjectFile).loadNow();
    IoUtilities.writeProject(savedProjectFile, loadedProject);
    Project savedProject = new TestFileProjectLoader(savedProjectFile).loadNow();
    IoUtilities.exportProject(exportedProjectFile, savedProject);
    Project exportedProject = IoUtilities.readProject(exportedProjectFile);

    assertNotNull(loadedProject);
    assertEquals("ClassroomUnitProgram", loadedProject.getProgramType().getName());
    assertTrue(savedProjectFile.isFile());
    assertNotNull(savedProject);
    assertEquals("ClassroomUnitProgram", savedProject.getProgramType().getName());
    assertTrue(exportedProjectFile.isFile());
    assertNotNull(exportedProject);
    assertEquals("ClassroomUnitProgram", exportedProject.getProgramType().getName());
  }

  @Test
  public void editedLoadedProjectCanBeSavedReopenedAndExportedHeadlessly() throws Exception {
    Path workingDirectory = Files.createDirectories(Path.of(
        "target",
        "headless-project-journey",
        UUID.randomUUID().toString()));
    File originalProjectFile = workingDirectory.resolve("classroom-edit.a3p").toFile();
    File editedProjectFile = workingDirectory.resolve("classroom-edit-copy.a3p").toFile();
    File exportedProjectFile = workingDirectory.resolve("classroom-edit-export.a3w").toFile();
    Project originalProject = new Project(programType(PROGRAM_NAME), Project.SceneCameraType.WindowCamera);

    IoUtilities.writeProject(originalProjectFile, originalProject);
    Project loadedProject = new TestFileProjectLoader(originalProjectFile).loadNow();
    loadedProject.getProgramType().name.setValue("EditedClassroomProgram");
    loadedProject.getProgramType().methods.add(new UserMethod(
        "editedArchiveMarker",
        JavaType.VOID_TYPE,
        new UserParameter[0],
        new BlockStatement(new Comment("archive reopen edit marker"))));

    HeadlessProjectFileUtilities fileUtilities = new HeadlessProjectFileUtilities(loadedProject);
    fileUtilities.saveCopyOfProjectTo(editedProjectFile);
    Project reopenedProject = new TestFileProjectLoader(editedProjectFile).loadNow();
    assertNotNull(reopenedProject);
    assertEquals("EditedClassroomProgram", reopenedProject.getProgramType().getName());
    assertTrue(hasMethod(reopenedProject.getProgramType(), "editedArchiveMarker"));

    fileUtilities.exportCopyOfProjectTo(exportedProjectFile);
    assertTrue(exportedProjectFile.isFile());
    try (ZipFile zipFile = new ZipFile(exportedProjectFile)) {
      assertNotNull(zipFile.getEntry("src/EditedClassroomProgram.twe"));
    }
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static boolean hasMethod(NamedUserType type, String methodName) {
    return type.getDeclaredMethods().stream()
        .anyMatch(method -> methodName.equals(method.getName()));
  }

  private static class HeadlessProjectFileUtilities extends ProjectFileUtilities {
    private final Project project;

    HeadlessProjectFileUtilities(Project project) {
      super(null);
      this.project = project;
    }

    @Override
    Project getUpToDateProject() {
      return project;
    }

    @Override
    Project getForcedUpToDateProject() {
      return project;
    }

    @Override
    BufferedImage createThumbnail() {
      return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }
  }

  private static class TestFileProjectLoader extends FileProjectLoader {
    TestFileProjectLoader(File file) {
      super(file);
    }

    Project loadNow() {
      return load();
    }
  }
}
