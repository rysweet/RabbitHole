package org.alice.ide;

import org.alice.ide.uricontent.FileProjectLoader;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

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

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
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
