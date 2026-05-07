package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.io.FileUtilities;

import java.io.File;
import java.io.IOException;

final class SaveOperationFlow {
  interface Context {
    File getCurrentFile();

    boolean isBackup();

    File getMainProjectFile();

    File getDefaultDirectory();

    File showSaveFileDialog(File directory, String filename, String extension);

    void showWaitCursor();

    void hideWaitCursor();

    void showError(String title, String message);

    void finish();

    void cancel();
  }

  interface PromptDecision {
    boolean isPromptNecessary(File file);
  }

  interface SaveAction {
    void save(File file) throws IOException;
  }

  record Result(boolean finished, boolean canceled, int promptCount, int saveAttempts, File savedFile) {
  }

  private SaveOperationFlow() {
  }

  static Result run(Context context, PromptDecision promptDecision, String extension, SaveAction saveAction) {
    File filePrevious = context.getCurrentFile();
    boolean isExceptionRaised = false;
    int promptCount = 0;
    int saveAttempts = 0;
    File savedFile = null;
    boolean finished = false;
    boolean canceled = false;
    do {
      File fileNext;
      if (context.isBackup()) {
        File mainFile = context.getMainProjectFile();
        String newProjectName = "";

        if (mainFile != null) {
          newProjectName = FileUtilities.getBaseName(mainFile) + " Copy";
        }

        promptCount++;
        fileNext = context.showSaveFileDialog(context.getDefaultDirectory(), newProjectName, extension);
      } else if (isExceptionRaised || promptDecision.isPromptNecessary(filePrevious)) {
        promptCount++;
        fileNext = context.showSaveFileDialog(context.getDefaultDirectory(), FileUtilities.getBaseName(filePrevious), extension);
      } else {
        fileNext = filePrevious;
      }
      isExceptionRaised = false;
      if (fileNext != null) {
        try {
          context.showWaitCursor();
          saveAttempts++;
          saveAction.save(fileNext);
          savedFile = fileNext;
        } catch (IOException ioe) {
          isExceptionRaised = true;
          //TODO I18n
          context.showError("Unable to save file", ioe.getMessage());
        } finally {
          context.hideWaitCursor();
        }
        if (!isExceptionRaised) {
          context.finish();
          finished = true;
        }
      } else {
        context.cancel();
        canceled = true;
      }
    } while (isExceptionRaised);
    return new Result(finished, canceled, promptCount, saveAttempts, savedFile);
  }
}
