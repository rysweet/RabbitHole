package org.alice.ide.croquet.models.projecturi;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SaveOperationFlowTest {
  private static final String PROJECT_EXTENSION = "a3p";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void writableCurrentFileSavesWithoutPromptAndFinishesActivity() throws Exception {
    File currentProject = temporaryFolder.newFile("world.a3p");
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.currentFile = currentProject;
    List<File> savedFiles = new ArrayList<>();

    SaveOperationFlow.Result result = SaveOperationFlow.run(context, file -> false, PROJECT_EXTENSION, savedFiles::add);

    assertTrue(context.dialogRequests.isEmpty());
    assertEquals(Arrays.asList(currentProject), savedFiles);
    assertEquals(Arrays.asList("showWaitCursor", "hideWaitCursor", "finish"), context.events);
    assertTrue(context.finished);
    assertFalse(context.canceled);
    assertTrue(result.finished());
    assertFalse(result.canceled());
    assertEquals(0, result.promptCount());
    assertEquals(1, result.saveAttempts());
    assertEquals(currentProject, result.savedFile());
  }

  @Test
  public void promptCancelCancelsActivityWithoutSaving() throws Exception {
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.promptFiles.add(null);

    SaveOperationFlow.Result result = SaveOperationFlow.run(
        context,
        file -> true,
        PROJECT_EXTENSION,
        file -> fail("Save should not run after cancel"));

    assertEquals(1, context.dialogRequests.size());
    assertEquals(new DialogRequest(temporaryFolder.getRoot(), null, PROJECT_EXTENSION), context.dialogRequests.get(0));
    assertTrue(context.canceled);
    assertFalse(context.finished);
    assertFalse(context.events.contains("showWaitCursor"));
    assertFalse(result.finished());
    assertTrue(result.canceled());
    assertEquals(1, result.promptCount());
    assertEquals(0, result.saveAttempts());
    assertNull(result.savedFile());
  }

  @Test
  public void backupSavePromptsWithMainProjectCopyName() throws Exception {
    File targetProject = new File(temporaryFolder.getRoot(), "world-copy.a3p");
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.currentFile = temporaryFolder.newFile("world.a3p");
    context.backup = true;
    context.mainProjectFile = new File(temporaryFolder.getRoot(), "world.a3p");
    context.promptFiles.add(targetProject);
    List<File> savedFiles = new ArrayList<>();

    SaveOperationFlow.Result result = SaveOperationFlow.run(context, file -> false, PROJECT_EXTENSION, savedFiles::add);

    assertEquals(1, context.dialogRequests.size());
    assertEquals(new DialogRequest(temporaryFolder.getRoot(), "world Copy", PROJECT_EXTENSION), context.dialogRequests.get(0));
    assertEquals(Arrays.asList(targetProject), savedFiles);
    assertTrue(context.finished);
    assertFalse(context.canceled);
    assertTrue(result.finished());
    assertFalse(result.canceled());
    assertEquals(1, result.promptCount());
    assertEquals(1, result.saveAttempts());
    assertEquals(targetProject, result.savedFile());
  }

  @Test
  public void ioExceptionShowsErrorThenRetriesWithPreviousProjectBaseName() throws Exception {
    File currentProject = temporaryFolder.newFile("world.a3p");
    File retryProject = new File(temporaryFolder.getRoot(), "world-retry.a3p");
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.currentFile = currentProject;
    context.promptFiles.add(retryProject);
    List<File> savedFiles = new ArrayList<>();
    AtomicInteger attempts = new AtomicInteger();

    SaveOperationFlow.Result result = SaveOperationFlow.run(context, file -> false, PROJECT_EXTENSION, file -> {
      savedFiles.add(file);
      if (attempts.getAndIncrement() == 0) {
        throw new IOException("disk full");
      }
    });

    assertEquals(Arrays.asList(currentProject, retryProject), savedFiles);
    assertEquals(1, context.dialogRequests.size());
    assertEquals(new DialogRequest(temporaryFolder.getRoot(), "world", PROJECT_EXTENSION), context.dialogRequests.get(0));
    assertEquals(Arrays.asList(new ErrorMessage("Unable to save file", "disk full")), context.errorMessages);
    assertEquals(Arrays.asList(
        "showWaitCursor",
        "hideWaitCursor",
        "showWaitCursor",
        "hideWaitCursor",
        "finish"), context.events);
    assertTrue(context.finished);
    assertFalse(context.canceled);
    assertTrue(result.finished());
    assertFalse(result.canceled());
    assertEquals(1, result.promptCount());
    assertEquals(2, result.saveAttempts());
    assertEquals(retryProject, result.savedFile());
  }

  @Test
  public void ioExceptionThenPromptCancelCancelsActivityAfterReportingFailure() throws Exception {
    File currentProject = temporaryFolder.newFile("world.a3p");
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.currentFile = currentProject;
    context.promptFiles.add(null);
    List<File> savedFiles = new ArrayList<>();

    SaveOperationFlow.Result result = SaveOperationFlow.run(context, file -> false, PROJECT_EXTENSION, file -> {
      savedFiles.add(file);
      throw new IOException("permission denied");
    });

    assertEquals(Arrays.asList(currentProject), savedFiles);
    assertEquals(1, context.dialogRequests.size());
    assertEquals(new DialogRequest(temporaryFolder.getRoot(), "world", PROJECT_EXTENSION), context.dialogRequests.get(0));
    assertEquals(Arrays.asList(new ErrorMessage("Unable to save file", "permission denied")), context.errorMessages);
    assertEquals(Arrays.asList("showWaitCursor", "hideWaitCursor", "cancel"), context.events);
    assertFalse(context.finished);
    assertTrue(context.canceled);
    assertFalse(result.finished());
    assertTrue(result.canceled());
    assertEquals(1, result.promptCount());
    assertEquals(1, result.saveAttempts());
    assertNull(result.savedFile());
  }

  @Test
  public void promptedJourneyIOExceptionRetriesWithCurrentProjectBaseName() throws Exception {
    File currentProject = temporaryFolder.newFile("world.a3p");
    File failedDestination = new File(temporaryFolder.getRoot(), "classroom-copy.a3p");
    File retryDestination = new File(temporaryFolder.getRoot(), "classroom-copy-retry.a3p");
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.currentFile = currentProject;
    context.promptFiles.add(failedDestination);
    context.promptFiles.add(retryDestination);
    List<File> savedFiles = new ArrayList<>();
    AtomicInteger attempts = new AtomicInteger();

    SaveOperationFlow.Result result = SaveOperationFlow.run(context, file -> true, PROJECT_EXTENSION, file -> {
      savedFiles.add(file);
      if (attempts.getAndIncrement() == 0) {
        throw new IOException("share unavailable");
      }
    });

    assertEquals(Arrays.asList(failedDestination, retryDestination), savedFiles);
    assertEquals(Arrays.asList(
        new DialogRequest(temporaryFolder.getRoot(), "world", PROJECT_EXTENSION),
        new DialogRequest(temporaryFolder.getRoot(), "world", PROJECT_EXTENSION)), context.dialogRequests);
    assertEquals(Arrays.asList(new ErrorMessage("Unable to save file", "share unavailable")), context.errorMessages);
    assertEquals(Arrays.asList(
        "showWaitCursor",
        "hideWaitCursor",
        "showWaitCursor",
        "hideWaitCursor",
        "finish"), context.events);
    assertTrue(context.finished);
    assertFalse(context.canceled);
    assertTrue(result.finished());
    assertFalse(result.canceled());
    assertEquals(2, result.promptCount());
    assertEquals(2, result.saveAttempts());
    assertEquals(retryDestination, result.savedFile());
  }

  @Test
  public void promptedJourneyWithoutCurrentFileRetriesCancelWithoutSuggestedBaseName() throws Exception {
    File failedDestination = new File(temporaryFolder.getRoot(), "new-world.a3p");
    FakeContext context = new FakeContext(temporaryFolder.getRoot());
    context.promptFiles.add(failedDestination);
    context.promptFiles.add(null);
    List<File> savedFiles = new ArrayList<>();

    SaveOperationFlow.Result result = SaveOperationFlow.run(context, file -> true, PROJECT_EXTENSION, file -> {
      savedFiles.add(file);
      throw new IOException("read only folder");
    });

    assertEquals(Arrays.asList(failedDestination), savedFiles);
    assertEquals(Arrays.asList(
        new DialogRequest(temporaryFolder.getRoot(), null, PROJECT_EXTENSION),
        new DialogRequest(temporaryFolder.getRoot(), null, PROJECT_EXTENSION)), context.dialogRequests);
    assertEquals(Arrays.asList(new ErrorMessage("Unable to save file", "read only folder")), context.errorMessages);
    assertEquals(Arrays.asList("showWaitCursor", "hideWaitCursor", "cancel"), context.events);
    assertFalse(context.finished);
    assertTrue(context.canceled);
    assertFalse(result.finished());
    assertTrue(result.canceled());
    assertEquals(2, result.promptCount());
    assertEquals(1, result.saveAttempts());
    assertNull(result.savedFile());
  }

  private static final class FakeContext implements SaveOperationFlow.Context {
    private final File defaultDirectory;
    private final Queue<File> promptFiles = new LinkedList<>();
    private final List<DialogRequest> dialogRequests = new ArrayList<>();
    private final List<ErrorMessage> errorMessages = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private File currentFile;
    private boolean backup;
    private File mainProjectFile;
    private boolean finished;
    private boolean canceled;

    private FakeContext(File defaultDirectory) {
      this.defaultDirectory = defaultDirectory;
    }

    @Override
    public File getCurrentFile() {
      return currentFile;
    }

    @Override
    public boolean isBackup() {
      return backup;
    }

    @Override
    public File getMainProjectFile() {
      return mainProjectFile;
    }

    @Override
    public File getDefaultDirectory() {
      return defaultDirectory;
    }

    @Override
    public File showSaveFileDialog(File directory, String filename, String extension) {
      dialogRequests.add(new DialogRequest(directory, filename, extension));
      return promptFiles.remove();
    }

    @Override
    public void showWaitCursor() {
      events.add("showWaitCursor");
    }

    @Override
    public void hideWaitCursor() {
      events.add("hideWaitCursor");
    }

    @Override
    public void showError(String title, String message) {
      errorMessages.add(new ErrorMessage(title, message));
    }

    @Override
    public void finish() {
      events.add("finish");
      finished = true;
    }

    @Override
    public void cancel() {
      events.add("cancel");
      canceled = true;
    }
  }

  private record DialogRequest(File directory, String filename, String extension) {
  }

  private record ErrorMessage(String title, String message) {
  }
}
