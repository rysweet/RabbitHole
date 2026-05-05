package org.alice.ide;

import org.alice.ide.uricontent.FileProjectLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.project.Project;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.io.File;

import static org.junit.Assert.*;

public class ProjectBackupSaveAsBehaviorTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void saveAsFromBackupWritesNormalProjectTargetWithoutChangingRecoveredBackup() throws Exception {
    File backupDirectory = temporaryFolder.newFolder("world.bak");
    File recoveredBackup = new File(backupDirectory, "auto20240102_120000.a3p");
    IoUtilities.writeProject(recoveredBackup, projectNamed("RecoveredProgram"));
    Project editedProject = projectNamed("EditedProgram");
    File saveAsTarget = new File(temporaryFolder.getRoot(), "saved-world.a3p");
    ProjectSaveTargetPlan plan = ProjectSaveTargetPlan.choose(new FileProjectLoader(recoveredBackup), saveAsTarget);
    ProjectFileUtilities saveUtilities = new ProjectFileUtilities(null) {
      @Override
      Project getUpToDateProject() {
        return editedProject;
      }

      @Override
      File savedProjectFile() {
        return new File(plan.getNextLoader().getUri());
      }
    };

    saveUtilities.saveProjectTo(saveAsTarget, plan.isBackupSave());

    assertFalse(plan.isBackupSave());
    assertFalse(plan.getNextLoader().isBackup());
    assertEquals("EditedProgram", IoUtilities.readProject(saveAsTarget).getProgramType().getName());
    assertEquals("RecoveredProgram", IoUtilities.readProject(recoveredBackup).getProgramType().getName());
    assertEquals(0, saveBackupsIn(backupDirectory).length);
    File[] targetSaveBackups = saveBackupsIn(new File(temporaryFolder.getRoot(), "saved-world.bak"));
    assertEquals(1, targetSaveBackups.length);
    assertEquals("EditedProgram", IoUtilities.readProject(targetSaveBackups[0]).getProgramType().getName());
  }

  private static File[] saveBackupsIn(File directory) {
    File[] files = directory.listFiles(file -> file.isFile()
        && file.getName().startsWith("save")
        && file.getName().endsWith(".a3p"));
    assertNotNull(files);
    return files;
  }

  private static Project projectNamed(String name) {
    return new Project(programType(name), Project.SceneCameraType.WindowCamera);
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }
}
