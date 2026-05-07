package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.junit.Test;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class SaveFileDialogShowControlProofTest {
  @Test(timeout = 15000)
  public void saveFileDialogShowDisplaysChooserAndAcceptsUiSelectedPathUnderXvfb() throws Exception {
    assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());
    Path testDir = newTestDir().resolve("displayed-chooser-ui-selection");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    File uiSelectedFile = projectsDir.resolve("ui-selected-save.a3p").toAbsolutePath().toFile();
    JFrame owner = displayableOwner("save-file-dialog-show-control-owner");
    SaveDialogShowProbe probe = new SaveDialogShowProbe(uiSelectedFile);
    AtomicReference<File> returnedFile = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        probe.start();
        returnedFile.set(FileDialogUtilities.showSaveFileDialog(
            owner,
            projectsDir.toFile(),
            "dialog-default-name",
            "a3p"));
      });

      probe.writeResult(evidenceDir, returnedFile.get());
      File actualFile = returnedFile.get() == null ? null : returnedFile.get().getAbsoluteFile();
      assertEquals(uiSelectedFile.getAbsoluteFile(), actualFile);
      String json = Files.readString(probe.artifactPath(evidenceDir));
      assertTrue(json, json.contains("\"status\": \"proven\""));
      assertTrue(json, json.contains("\"reason\": \"displayed_jfilechooser_approved_selected_file\""));
      assertTrue(json, json.contains("\"dialog_showing\": true"));
      assertTrue(json, json.contains("\"chooser_observed\": true"));
      assertTrue(json, json.contains("\"control_method\": \"JFileChooser.setSelectedFile + approveSelection\""));
      assertTrue(json, json.contains("\"returned_file\": \"" + FileDialogUtilities.escapeJson(uiSelectedFile.getAbsolutePath()) + "\""));
      assertTrue(json, json.contains("\"native java.awt.FileDialog peer display/control\""));
      assertFalse(json, json.contains("\"Save chooser was displayed\""));
    } finally {
      dispose(owner);
    }
  }

  @Test
  public void proofArtifactReportsUnsupportedWhenNoChooserWindowIsObserved() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("chooser-not-observed"));
    File selectedFile = evidenceDir.resolve("not-used.a3p").toFile();
    SaveDialogShowProbe probe = new SaveDialogShowProbe(selectedFile);

    probe.writeUnsupported(evidenceDir, "save_dialog_window_not_observed_before_timeout");

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"reason\": \"save_dialog_window_not_observed_before_timeout\""));
    assertTrue(json, json.contains("\"chooser_observed\": false"));
    assertTrue(json, json.contains("\"doesNotClaim\""));
    assertTrue(json, json.contains("\"Save chooser was displayed\""));
    assertFalse(json, json.contains("\"status\": \"proven\""));
  }

  private static JFrame displayableOwner(String title) throws Exception {
    AtomicReference<JFrame> frame = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      JFrame owner = new JFrame(title);
      owner.setSize(320, 200);
      owner.setLocationByPlatform(true);
      owner.setVisible(true);
      frame.set(owner);
    });
    assertTrue(frame.get().isDisplayable());
    assertTrue(frame.get().isShowing());
    return frame.get();
  }

  private static void dispose(JFrame frame) throws Exception {
    if (frame != null) {
      SwingUtilities.invokeAndWait(frame::dispose);
    }
  }

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "save-file-dialog-show-control-proof-test",
        UUID.randomUUID().toString()));
  }

  private static class SaveDialogShowProbe {
    private static final String ARTIFACT = "desktop-save-dialog-show-control-proof.json";
    private static final int MAX_POLLS = 120;

    private final File selectedFile;
    private boolean chooserObserved;
    private boolean approvedSelection;
    private boolean dialogShowing;
    private boolean dialogDisplayable;
    private String dialogClass;
    private String dialogTitle;
    private int pollCount;
    private Timer timer;

    SaveDialogShowProbe(File selectedFile) {
      this.selectedFile = selectedFile;
    }

    void start() {
      this.timer = new Timer(50, event -> poll());
      this.timer.start();
    }

    Path artifactPath(Path evidenceDir) {
      return evidenceDir.resolve(ARTIFACT);
    }

    void writeResult(Path evidenceDir, File returnedFile) throws Exception {
      String status = proven(returnedFile) ? "proven" : "unsupported";
      String reason = proven(returnedFile)
          ? "displayed_jfilechooser_approved_selected_file"
          : "save_dialog_show_or_control_not_completed";
      write(evidenceDir, status, reason, returnedFile);
    }

    void writeUnsupported(Path evidenceDir, String reason) throws Exception {
      write(evidenceDir, "unsupported", reason, null);
    }

    private void poll() {
      this.pollCount++;
      for (Window window : Window.getWindows()) {
        if ((window instanceof JDialog dialog) && window.isShowing()) {
          JFileChooser chooser = findChooser(dialog);
          if (chooser != null) {
            this.chooserObserved = true;
            this.dialogShowing = dialog.isShowing();
            this.dialogDisplayable = dialog.isDisplayable();
            this.dialogClass = dialog.getClass().getName();
            this.dialogTitle = dialog.getTitle();
            chooser.setSelectedFile(this.selectedFile);
            chooser.approveSelection();
            this.approvedSelection = true;
            this.timer.stop();
            return;
          }
        }
      }
      if (this.pollCount >= MAX_POLLS) {
        cancelOpenChoosers();
        this.timer.stop();
      }
    }

    private boolean proven(File returnedFile) {
      return this.chooserObserved
          && this.approvedSelection
          && returnedFile != null
          && returnedFile.getAbsoluteFile().equals(this.selectedFile.getAbsoluteFile());
    }

    private void write(Path evidenceDir, String status, String reason, File returnedFile) throws Exception {
      Files.createDirectories(evidenceDir);
      Files.writeString(
          artifactPath(evidenceDir),
          "{\n"
              + "  \"schema_version\": \"eatme.alice-desktop-save-dialog-show-control-proof/v1\",\n"
              + "  \"status\": \"" + status + "\",\n"
              + "  \"reason\": \"" + reason + "\",\n"
              + "  \"source\": \"edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)\",\n"
              + "  \"observed_dialog\": {\n"
              + "    \"dialog_class\": " + stringJson(this.dialogClass) + ",\n"
              + "    \"dialog_title\": " + stringJson(this.dialogTitle) + ",\n"
              + "    \"dialog_showing\": " + this.dialogShowing + ",\n"
              + "    \"dialog_displayable\": " + this.dialogDisplayable + ",\n"
              + "    \"chooser_observed\": " + this.chooserObserved + ",\n"
              + "    \"poll_count\": " + this.pollCount + "\n"
              + "  },\n"
              + "  \"control\": {\n"
              + "    \"control_method\": \"JFileChooser.setSelectedFile + approveSelection\",\n"
              + "    \"approved_selection\": " + this.approvedSelection + ",\n"
              + "    \"selected_file\": " + fileJson(this.selectedFile) + ",\n"
              + "    \"returned_file\": " + fileJson(returnedFile) + "\n"
              + "  },\n"
              + doesNotClaimJson(status)
              + "}\n",
          StandardCharsets.UTF_8);
    }

    private static String doesNotClaimJson(String status) {
      String saveChooserDisplayClaim = "proven".equals(status)
          ? ""
          : "    \"Save chooser was displayed\",\n";
      return "  \"doesNotClaim\": [\n"
          + "    \"StageIDE Save menu was clicked\",\n"
          + "    \"native java.awt.FileDialog peer display/control\",\n"
          + saveChooserDisplayClaim
          + "    \"desktop pixels or visible rendering were validated\",\n"
          + "    \"first-lesson completion\",\n"
          + "    \"grading\"\n"
          + "  ]\n";
    }

    private static JFileChooser findChooser(Component component) {
      if (component instanceof JFileChooser chooser) {
        return chooser;
      }
      if (component instanceof Container container) {
        for (Component child : container.getComponents()) {
          JFileChooser chooser = findChooser(child);
          if (chooser != null) {
            return chooser;
          }
        }
      }
      return null;
    }

    private static void cancelOpenChoosers() {
      for (Window window : Window.getWindows()) {
        if ((window instanceof JDialog dialog) && window.isShowing()) {
          JFileChooser chooser = findChooser(dialog);
          if (chooser != null) {
            chooser.cancelSelection();
          }
        }
      }
    }

    private static String fileJson(File file) {
      return file == null ? "null" : stringJson(file.getAbsolutePath());
    }

    private static String stringJson(String value) {
      return value == null ? "null" : "\"" + FileDialogUtilities.escapeJson(value) + "\"";
    }
  }
}
