package org.alice.ide;

import org.alice.ide.uricontent.FileProjectLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.project.Project;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SProgram;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class ProjectBackupRecoveryIoTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void corruptMainProjectSkipsUnloadableBackupAndLoadsNextBackupWithResources() throws Exception {
    File corruptMainProject = temporaryFolder.newFile("world.a3p");
    Files.writeString(corruptMainProject.toPath(), "not a project archive", StandardCharsets.UTF_8);
    File backupDirectory = temporaryFolder.newFolder("world.bak");
    File corruptNewestBackup = new File(backupDirectory, "auto20240102_140000.a3p");
    Files.writeString(corruptNewestBackup.toPath(), "not a backup archive", StandardCharsets.UTF_8);
    File validBackup = new File(backupDirectory, "auto20240102_130000.a3p");
    byte[] data = "recovered notes".getBytes(StandardCharsets.UTF_8);
    Project backupProject = new Project(programType("RecoveredProgram"), Project.SceneCameraType.WindowCamera);
    TestResource resource = new TestResource("note.txt", "text/plain", data);
    backupProject.addResource(resource);
    IoUtilities.writeProject(validBackup, backupProject);
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    Project mainProject = new TestFileProjectLoader(corruptMainProject).loadNow();
    File backup = selector.getNextBackup(
        LocalDateTime.MIN,
        backupDirectory,
        new File[] {corruptNewestBackup, validBackup},
        true,
        Set.of(corruptNewestBackup.getName()));
    ProjectLoadFailurePlan plan = ProjectLoadFailurePlan.choose(
        false,
        false,
        true,
        false,
        backup,
        corruptMainProject);
    ProjectLoadFailureDispatchPlan dispatch = ProjectLoadFailureDispatchPlan.afterUserChoice(
        plan.getAction(),
        true);
    Project recoveredProject = new TestFileProjectLoader(plan.getBackupToLoad()).loadNow();

    assertNull(mainProject);
    assertEquals(ProjectLoadFailurePlan.Action.PROMPT_LOAD_BACKUP, plan.getAction());
    assertEquals(validBackup, plan.getBackupToLoad());
    assertEquals(ProjectLoadFailureDispatchPlan.LoadTarget.BACKUP, dispatch.getLoadTarget());
    assertFalse(dispatch.shouldShowNewProject());
    assertEquals("RecoveredProgram", recoveredProject.getProgramType().getName());
    assertEquals(1, recoveredProject.getResources().size());
    Resource readResource = recoveredProject.getResources().iterator().next();
    assertEquals(TestResource.class, readResource.getClass());
    assertEquals(resource.getId(), readResource.getId());
    assertEquals("note.txt", readResource.getOriginalFileName());
    assertEquals("text/plain", readResource.getContentType());
    assertArrayEquals(data, readResource.getData());
  }

  @Test
  public void corruptMainProjectAndAllBackupsPlanUserVisibleFailure() throws Exception {
    File corruptMainProject = temporaryFolder.newFile("world.a3p");
    Files.writeString(corruptMainProject.toPath(), "not a project archive", StandardCharsets.UTF_8);
    File backupDirectory = temporaryFolder.newFolder("world.bak");
    File corruptNewestBackup = new File(backupDirectory, "auto20240102_140000.a3p");
    Files.writeString(corruptNewestBackup.toPath(), "not a newest backup archive", StandardCharsets.UTF_8);
    File corruptOlderBackup = new File(backupDirectory, "auto20240102_130000.a3p");
    Files.writeString(corruptOlderBackup.toPath(), "not an older backup archive", StandardCharsets.UTF_8);
    File[] newestFirstBackups = new File[] {corruptNewestBackup, corruptOlderBackup};
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    Project mainProject = new TestFileProjectLoader(corruptMainProject).loadNow();
    Set<String> unloadableFiles = new HashSet<>();
    unloadableFiles.add(corruptMainProject.getName());
    File newestBackup = selector.getNextBackup(
        LocalDateTime.MIN,
        backupDirectory,
        newestFirstBackups,
        true,
        unloadableFiles);
    ProjectLoadFailurePlan initialRecoveryPlan = ProjectLoadFailurePlan.choose(
        false,
        false,
        true,
        false,
        newestBackup,
        corruptMainProject);
    Project newestBackupProject = new TestFileProjectLoader(initialRecoveryPlan.getBackupToLoad()).loadNow();

    unloadableFiles.add(corruptNewestBackup.getName());
    File olderBackup = selector.getNextBackup(
        LocalDateTime.MIN,
        backupDirectory,
        newestFirstBackups,
        true,
        unloadableFiles);
    ProjectLoadFailurePlan retryRecoveryPlan = ProjectLoadFailurePlan.choose(
        true,
        true,
        true,
        false,
        olderBackup,
        corruptNewestBackup);
    Project olderBackupProject = new TestFileProjectLoader(retryRecoveryPlan.getBackupToLoad()).loadNow();

    unloadableFiles.add(corruptOlderBackup.getName());
    File exhaustedBackups = selector.getNextBackup(
        LocalDateTime.MIN,
        backupDirectory,
        newestFirstBackups,
        true,
        unloadableFiles);
    ProjectLoadFailurePlan exhaustedRecoveryPlan = ProjectLoadFailurePlan.choose(
        true,
        true,
        true,
        false,
        exhaustedBackups,
        corruptOlderBackup);
    ProjectLoadFailureDispatchPlan dispatch = ProjectLoadFailureDispatchPlan.afterUserChoice(
        exhaustedRecoveryPlan.getAction(),
        false);

    assertNull(mainProject);
    assertEquals(ProjectLoadFailurePlan.Action.PROMPT_LOAD_BACKUP, initialRecoveryPlan.getAction());
    assertEquals(corruptNewestBackup, initialRecoveryPlan.getBackupToLoad());
    assertNull(newestBackupProject);
    assertEquals(ProjectLoadFailurePlan.Action.PROMPT_LOAD_BACKUP, retryRecoveryPlan.getAction());
    assertEquals(corruptOlderBackup, retryRecoveryPlan.getBackupToLoad());
    assertEquals(corruptNewestBackup.getName(), retryRecoveryPlan.getFailedBackupName());
    assertNull(olderBackupProject);
    assertNull(exhaustedBackups);
    assertEquals(ProjectLoadFailurePlan.Action.SHOW_PROJECT_AND_ALL_BACKUPS_LOAD_ERROR, exhaustedRecoveryPlan.getAction());
    assertNull(exhaustedRecoveryPlan.getBackupToLoad());
    assertEquals(ProjectLoadFailureDispatchPlan.LoadTarget.NONE, dispatch.getLoadTarget());
    assertTrue(dispatch.shouldShowNewProject());
  }

  @Test
  public void corruptDefaultBackupWithNoOtherBackupsPlansUnsavedBackupsFailure() throws Exception {
    File defaultBackupDirectory = temporaryFolder.newFolder(".defaultbak");
    File corruptDefaultBackup = new File(defaultBackupDirectory, "auto20240102_140000.a3p");
    Files.writeString(corruptDefaultBackup.toPath(), "not an unsaved project archive", StandardCharsets.UTF_8);
    FileProjectLoader loader = new FileProjectLoader(corruptDefaultBackup);
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted unsaved project recovery should not compare backup times");
    });
    Set<String> unloadableFiles = Set.of(corruptDefaultBackup.getName());

    Project project = new TestFileProjectLoader(corruptDefaultBackup).loadNow();
    File backup = selector.getNextBackup(
        LocalDateTime.MIN,
        defaultBackupDirectory,
        new File[] {corruptDefaultBackup},
        true,
        unloadableFiles);
    ProjectLoadFailurePlan plan = ProjectLoadFailurePlan.choose(
        loader.isBackup(),
        true,
        true,
        loader.isDefaultBackup(),
        backup,
        corruptDefaultBackup);
    ProjectLoadFailureDispatchPlan dispatch = ProjectLoadFailureDispatchPlan.afterUserChoice(
        plan.getAction(),
        false);

    assertNull(project);
    assertTrue(loader.isDefaultBackup());
    assertNull(loader.getMainProjectFile());
    assertNull(backup);
    assertEquals(ProjectLoadFailurePlan.Action.SHOW_UNSAVED_BACKUPS_LOAD_ERROR, plan.getAction());
    assertNull(plan.getBackupToLoad());
    assertEquals(ProjectLoadFailureDispatchPlan.LoadTarget.NONE, dispatch.getLoadTarget());
    assertTrue(dispatch.shouldShowNewProject());
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  public static class TestResource extends Resource {
    public TestResource(String fileName, String contentType, byte[] data) {
      super(fileName, contentType, data);
    }

    private TestResource(UUID uuid) {
      super(uuid);
    }

    public static TestResource valueOf(String uuidText) {
      return new TestResource(UUID.fromString(uuidText));
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
