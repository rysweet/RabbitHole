package org.alice.ide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.*;

public class ProjectBackupSelectorTest {
  private static final LocalDateTime PROJECT_MODIFIED_TIME = LocalDateTime.of(2024, 1, 2, 12, 0);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void corruptedMainProjectUsesLatestAvailableBackup() throws IOException {
    File newest = backup("auto20240102_130000.a3p");
    File older = backup("auto20240102_120000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {newest, older},
        true,
        Set.of());

    assertEquals(newest, backup);
  }

  @Test
  public void skipsBackupsAlreadyKnownToBeUnloadable() throws IOException {
    File newest = backup("auto20240102_130000.a3p");
    File older = backup("auto20240102_120000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {newest, older},
        true,
        Set.of(newest.getName()));

    assertEquals(older, backup);
  }

  @Test
  public void corruptedMainProjectSkipsMissingBackupCandidate() throws IOException {
    File missingNewest = missingBackup("auto20240102_140000.a3p");
    File older = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {missingNewest, older},
        true,
        Set.of());

    assertEquals(older, backup);
  }

  @Test
  public void corruptedMainProjectSkipsBackupSymlinkEscapingBackupDirectory() throws IOException {
    Path backupDirectory = temporaryFolder.newFolder("world.bak").toPath();
    Path outsideBackup = temporaryFolder.newFile("auto20240102_150000.a3p").toPath();
    Files.writeString(outsideBackup, "outside backup", StandardCharsets.UTF_8);
    Path escapingBackup = backupDirectory.resolve("auto20240102_140000.a3p");
    Files.createSymbolicLink(escapingBackup, outsideBackup);
    File safeBackup = backupDirectory.resolve("auto20240102_130000.a3p").toFile();
    Files.writeString(safeBackup.toPath(), "safe backup", StandardCharsets.UTF_8);
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {escapingBackup.toFile(), safeBackup},
        true,
        Set.of());

    assertEquals(safeBackup, backup);
  }

  @Test
  public void corruptedMainProjectSkipsBackupSymlinkEvenWhenTargetStaysInBackupDirectory() throws IOException {
    Path backupDirectory = temporaryFolder.newFolder("world.bak").toPath();
    Path symlinkTarget = backupDirectory.resolve("auto20240102_140000-target.a3p");
    Files.writeString(symlinkTarget, "symlink target backup", StandardCharsets.UTF_8);
    Path symlinkBackup = backupDirectory.resolve("auto20240102_140000.a3p");
    Files.createSymbolicLink(symlinkBackup, symlinkTarget);
    File safeBackup = backupDirectory.resolve("auto20240102_130000.a3p").toFile();
    Files.writeString(safeBackup.toPath(), "safe backup", StandardCharsets.UTF_8);
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {symlinkBackup.toFile(), safeBackup},
        true,
        Set.of());

    assertEquals(safeBackup, backup);
  }

  @Test
  public void corruptedMainProjectSkipsCandidatesFromSymlinkedBackupDirectory() throws IOException {
    Path outsideDirectory = temporaryFolder.newFolder("outside-backups").toPath();
    Path outsideBackup = outsideDirectory.resolve("auto20240102_140000.a3p");
    Files.writeString(outsideBackup, "outside backup", StandardCharsets.UTF_8);
    Path symlinkedBackupDirectory = temporaryFolder.getRoot().toPath().resolve("world.bak");
    Files.createSymbolicLink(symlinkedBackupDirectory, outsideDirectory);
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {symlinkedBackupDirectory.resolve(outsideBackup.getFileName()).toFile()},
        true,
        Set.of());

    assertNull(backup);
  }

  @Test
  public void corruptedMainProjectSkipsBackupCandidateWithoutParentDirectory() throws IOException {
    File parentlessCandidate = new File("pom.xml");
    assertTrue(parentlessCandidate.isFile());
    File safeBackup = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should not compare backup times");
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {parentlessCandidate, safeBackup},
        true,
        Set.of());

    assertEquals(safeBackup, backup);
  }

  @Test
  public void recentBackupProbeUsesLatestBackupOnlyWhenNewerThanProject() throws IOException {
    File newest = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(
        timeSource(Map.of(newest.getName(), PROJECT_MODIFIED_TIME.plusMinutes(10))));

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {newest},
        false,
        Set.of());

    assertEquals(newest, backup);
  }

  @Test
  public void recentBackupProbeStopsWhenLatestCandidateIsNotNewerThanProject() throws IOException {
    File newest = backup("auto20240102_110000.a3p");
    File older = backup("auto20240102_100000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      assertEquals(newest.getName(), file.getName());
      return PROJECT_MODIFIED_TIME.minusMinutes(10);
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {newest, older},
        false,
        Set.of());

    assertNull(backup);
  }

  @Test
  public void recentBackupProbeSkipsUnloadableNewestAndUsesNextOnlyWhenNewerThanProject() throws IOException {
    File unloadableNewest = backup("auto20240102_140000.a3p");
    File next = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      assertEquals(next.getName(), file.getName());
      return PROJECT_MODIFIED_TIME.plusMinutes(10);
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {unloadableNewest, next},
        false,
        Set.of(unloadableNewest.getName()));

    assertEquals(next, backup);
  }

  @Test
  public void recentBackupProbeSkipsUnloadableNewestAndStopsWhenNextIsNotNewerThanProject()
      throws IOException {
    File unloadableNewest = backup("auto20240102_140000.a3p");
    File older = backup("auto20240102_110000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      assertEquals(older.getName(), file.getName());
      return PROJECT_MODIFIED_TIME.minusMinutes(10);
    });

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {unloadableNewest, older},
        false,
        Set.of(unloadableNewest.getName()));

    assertNull(backup);
  }

  @Test
  public void missingMainProjectTimestampUsesLatestAvailableBackup() throws IOException {
    File newest = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("missing main project timestamp should not compare backup times");
    });

    File backup = getNextBackup(selector,
        LocalDateTime.MIN,
        new File[] {newest},
        false,
        Set.of());

    assertEquals(newest, backup);
  }

  @Test
  public void minimumMainProjectTimestampValueUsesLatestAvailableBackup() throws IOException {
    File newest = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("minimum main project timestamp should not compare backup times");
    });

    File backup = getNextBackup(selector,
        LocalDateTime.of(LocalDate.MIN, LocalTime.MIN),
        new File[] {newest},
        false,
        Set.of());

    assertEquals(newest, backup);
  }

  @Test
  public void returnsNullWhenNoBackupCandidatesRemain() throws IOException {
    File newest = backup("auto20240102_130000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(
        timeSource(Map.of(newest.getName(), PROJECT_MODIFIED_TIME.plusMinutes(10))));

    File backup = getNextBackup(selector,
        PROJECT_MODIFIED_TIME,
        new File[] {newest},
        false,
        Set.of(newest.getName()));

    assertNull(backup);
  }

  @Test
  public void pr426BackupContractSelectsNewestSafeCandidateAndNeverReselectsFailedOrUnsafe()
      throws IOException {
    Path backupDirectory = temporaryFolder.newFolder("world.bak").toPath();
    File failedNewest = backup(backupDirectory, "auto20240102_150000.a3p");
    Path outsideBackup = temporaryFolder.newFile("auto20240102_140000.a3p").toPath();
    Files.writeString(outsideBackup, "outside backup", StandardCharsets.UTF_8);
    Path escapingBackup = backupDirectory.resolve("auto20240102_140000.a3p");
    Files.createSymbolicLink(escapingBackup, outsideBackup);
    File previouslyFailed = backup(backupDirectory, "auto20240102_130000.a3p");
    File safeBackup = backup(backupDirectory, "auto20240102_120000.a3p");
    ProjectBackupSelector selector = new ProjectBackupSelector(file -> {
      throw new AssertionError("corrupted main project should select by order without probing timestamps");
    });

    File backup = selector.getNextBackup(
        PROJECT_MODIFIED_TIME,
        backupDirectory.toFile(),
        new File[] {failedNewest, escapingBackup.toFile(), previouslyFailed, safeBackup},
        true,
        Set.of(failedNewest.getName(), previouslyFailed.getName()));

    assertEquals(safeBackup, backup);
  }

  private File getNextBackup(ProjectBackupSelector selector, LocalDateTime modifiedTime, File[] backups,
                              boolean isMainProjectCorrupted, Set<String> unloadableFiles) {
    return selector.getNextBackup(
        modifiedTime,
        backupDirectory(backups),
        backups,
        isMainProjectCorrupted,
        unloadableFiles);
  }

  private File backupDirectory(File[] backups) {
    return Arrays.stream(backups)
        .filter(Objects::nonNull)
        .map(File::getParentFile)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(temporaryFolder.getRoot());
  }

  private File backup(String name) throws IOException {
    return backup(temporaryFolder.getRoot().toPath(), name);
  }

  private File backup(Path directory, String name) throws IOException {
    File backup = directory.resolve(name).toFile();
    Files.writeString(backup.toPath(), "backup", StandardCharsets.UTF_8);
    return backup;
  }

  private File missingBackup(String name) {
    return new File(temporaryFolder.getRoot(), name);
  }

  private static ProjectBackupSelector.BackupTimeSource timeSource(Map<String, LocalDateTime> createdTimes) {
    return file -> createdTimes.get(file.getName());
  }
}
