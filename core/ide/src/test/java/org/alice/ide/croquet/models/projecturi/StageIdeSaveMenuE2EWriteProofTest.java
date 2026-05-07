package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.alice.ide.ProjectDocument;
import org.alice.ide.project.ProjectDocumentState;
import org.alice.ide.uricontent.UriProjectLoader;
import org.alice.stageide.StageIDE;
import org.alice.stageide.sceneeditor.StorytellingSceneEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.License;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.story.SProgram;
import org.lgna.story.SScene;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimerTask;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Proves the full StageIDE Save menu → JFileChooser approved → project file written path.
 *
 * <p>This is the E2E continuation after PR #265 (which proved
 * DocumentFrame.showSaveFileDialog reaches a JFileChooser but cancelled the dialog).
 *
 * <p>Chain proved:
 * SaveProjectOperation.fire(UserActivity)
 *   → AbstractSaveOperation.perform(activity)
 *   → SaveOperationFlow.run(context, ...)
 *   → context.showSaveFileDialog() = application.getDocumentFrame().showSaveFileDialog(...)
 *   → FileDialogUtilities.showSaveFileDialog(JFrame, dir, name, ext)
 *   → SwingFileDialog.show()
 *   → JFileChooser.showSaveDialog(root)
 *   → JFileChooser observed in Window.getWindows() poll
 *   → JFileChooser.setSelectedFile(targetFile) + approveSelection() by background probe
 *   → showSaveDialog returns APPROVE_OPTION
 *   → showSaveFileDialog returns targetFile
 *   → SaveOperationFlow calls saveAction.save(targetFile)
 *   → application.saveProjectTo(targetFile)
 *   → targetFile.a3p written to disk (non-empty)
 *
 * <p>The background probe uses a java.util.Timer (daemon thread, independent of EDT) so
 * it fires even while the EDT is blocked inside JFileChooser initialization, before the
 * secondary event loop starts. The approval is posted via SwingUtilities.invokeLater so
 * it is processed by the JFileChooser's nested event pump.
 */
public class StageIdeSaveMenuE2EWriteProofTest {
  private String previousDiscoveryEvidenceDir;
  private String previousSelectedPath;
  private Preferences licensePreferences;
  private String previousLicenseAccepted;

  @Before
  public void captureProperties() {
    previousDiscoveryEvidenceDir = System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    licensePreferences = Preferences.userNodeForPackage(License.class);
    previousLicenseAccepted = licensePreferences.get("isLicenseAccepted", null);
  }

  @After
  public void restorePropertiesAndResetApplication() throws Exception {
    disposeAwtWindows();
    restoreLicensePreference();
    restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousDiscoveryEvidenceDir);
    restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
    resetActiveApplication();
  }

  /**
   * Proves the full StageIDE Save action → live JFileChooser approval → written .a3p file.
   *
   * <p>SaveProjectOperation.fire(UserActivity) is the exact entry point called when the
   * Save menu item is dispatched (proved in PR #235). This test does not repeat the
   * doClick proof; it proves the downstream path: fire → perform → dialog → approval → write.
   *
   * <p>The JFileChooser is approved (not cancelled) by the background probe. The resulting
   * .a3p file is verified to exist and be non-empty, proving the full write path.
   *
   * <p>doesNotClaim: Save menu item doClick was the trigger (that was PR #235),
   * desktop pixels or visible rendering, first-lesson completion, grading.
   */
  @Test(timeout = 60000)
  public void stageIdeSaveFireApprovesChooserAndWritesProjectFile() throws Exception {
    assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());

    Path testDir = newTestDir().resolve("save-fire-to-written-file");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    File targetFile = projectsDir.resolve("e2e-save-proof.a3p").toAbsolutePath().toFile();

    // Ensure path-injection bypass is NOT active so the real dialog is displayed.
    System.clearProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());

    // Point ApplicationRoot at the pre-built distribution directory so the JOGL native
    // library loader finds its libs without showing a blocking error dialog.
    // The distribution dir is built by core/resources process-resources and contains
    // platform/linux-amd64/jogl/natives/linux-amd64/libgluegen_rt.so etc.
    // Without this, ApplicationRoot.initializeIfNecessary() would show a JOptionPane
    // (blocking the EDT indefinitely) before calling System.exit(-1).
    Path distributionDir = Paths.get(System.getProperty("user.dir"))
        .resolve("../../core/resources/target/distribution").normalize();
    if (distributionDir.toFile().isDirectory()) {
      System.setProperty("org.alice.ide.rootDirectory", distributionDir.toString());
    }

    resetActiveApplication();

    // Step 1: Initialize StageIDE on the EDT so its X11 peer is fully up.
    // Project state (document + uriProjectLoader) is injected before setVisible so
    // the save path has a non-null project and loader before any dialog can appear.
    SwingUtilities.invokeAndWait(() -> {
      StageIDE ide = new StageIDE(new CrashDetector(StageIdeSaveMenuE2EWriteProofTest.class));
      // Pre-accept EULA so handleWindowOpened → promptForLicenseAgreements does not show
      // a blocking dialog. The preference is checked by EULAUtilities and skips the dialog
      // if already true. This mirrors what happens after a user first-runs and accepts.
      licensePreferences.putBoolean("isLicenseAccepted", true);
      ide.initialize(new String[0]);
      // Inject project state before setVisible to avoid setProject dialog side-effects.
      Project project = minimalProject();
      try {
        // Set project document without calling ide.setProject() (which shows sanity-check
        // dialogs). ProjectDocumentState.setValueTransactionlessly is the low-level write.
        ProjectDocumentState.getInstance().setValueTransactionlessly(
            new ProjectDocument(project, new UserActivity()));
        // Inject a stub UriProjectLoader so ProjectSaveTargetPlan.choose() does not NPE.
        injectUriProjectLoader(ide, new NewProjectStub());
      } catch (Exception e) {
        throw new RuntimeException("project state injection failed", e);
      }
      ide.getDocumentFrame().getFrame().pack();
      ide.getDocumentFrame().getFrame().setVisible(true);
      assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isDisplayable());
      assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isShowing());
    });

    // Drain any post-setVisible events (windowOpened, license checks) by submitting
    // no-op invokeAndWait calls. These ensure the EDT is idle before starting the probe.
    SwingUtilities.invokeAndWait(() -> {
      // intentional no-op: drain window-opened side effects
    });
    // A second drain handles any events queued inside the first drain (e.g., from
    // WindowEvent listeners that use invokeLater internally).
    SwingUtilities.invokeAndWait(() -> {
      // intentional no-op: second drain pass
    });
    // Third drain: null out sceneCameraImp on StorytellingSceneEditor so that
    // ThumbnailGenerator.createThumbnail() returns null (fast path) instead of
    // attempting an offscreen OpenGL render that blocks forever in a secondary event loop.
    // getSgCameraForCreatingThumbnails() returns null when sceneCameraImp is null,
    // causing thumbnailDataSource() to receive null and skip thumbnail generation entirely.
    SwingUtilities.invokeAndWait(() -> {
      try {
        Field sceneCameraImpField = StorytellingSceneEditor.class.getDeclaredField("sceneCameraImp");
        sceneCameraImpField.setAccessible(true);
        sceneCameraImpField.set(StorytellingSceneEditor.getInstance(), null);
      } catch (Exception e) {
        throw new RuntimeException("failed to null sceneCameraImp on StorytellingSceneEditor", e);
      }
    });

    // Step 2: Start background probe. The probe runs on a daemon timer thread,
    // independent of the EDT, and approves the JFileChooser via invokeLater.
    SaveMenuE2EProbe probe = new SaveMenuE2EProbe(targetFile);
    probe.start();

    // Step 3: Fire SaveProjectOperation on the EDT (same as doClick dispatch path).
    // This blocks the EDT inside JFileChooser.showSaveDialog's secondary event loop
    // until the probe approves the selection or times out.
    try {
      SwingUtilities.invokeAndWait(() -> {
        SaveProjectOperation.getInstance().fire(new UserActivity());
      });
    } finally {
      probe.stop();
    }

    // Step 4: Write evidence artifact and assert the full chain.
    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"proven\""));
    assertTrue(json, json.contains("\"reason\": \"save_fire_approved_chooser_wrote_project_file\""));
    assertTrue(json, json.contains("\"chooser_observed\": true"));
    assertTrue(json, json.contains("\"approved_selection\": true"));
    assertTrue(json, json.contains("\"file_written\": true"));
    assertTrue(json, json.contains("\"file_nonempty\": true"));
    assertFalse(json, json.contains("\"status\": \"unsupported\""));

    assertTrue("target .a3p file must exist after save", targetFile.isFile());
    assertTrue("target .a3p file must be non-empty", targetFile.length() > 0);
  }

  // ---- inner probe ----

  /**
   * Background daemon timer that polls Window.getWindows(), finds the JFileChooser
   * that AbstractSaveOperation.perform opened, sets the target file as the selected
   * file, and calls approveSelection() via invokeLater so the secondary event pump
   * processes it.
   *
   * <p>Using java.util.Timer (daemon thread) so the probe fires even while the EDT is
   * blocked during JFileChooser initialization before its nested event loop starts.
   */
  private static class SaveMenuE2EProbe {
    private static final String ARTIFACT = "stageide-save-menu-e2e-write-proof.json";
    private static final int MAX_POLLS = 400;

    private final File targetFile;
    private volatile boolean chooserObserved;
    private volatile boolean approvedSelection;
    private volatile boolean dialogShowing;
    private volatile String dialogClass;
    private volatile int pollCount;
    private java.util.Timer bgTimer;

    SaveMenuE2EProbe(File targetFile) {
      this.targetFile = targetFile;
    }

    void start() {
      this.bgTimer = new java.util.Timer("save-menu-e2e-probe", /* daemon= */ true);
      this.bgTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          poll();
        }
      }, 100, 100);
    }

    void stop() {
      if (this.bgTimer != null) {
        this.bgTimer.cancel();
      }
    }

    Path artifactPath(Path evidenceDir) {
      return evidenceDir.resolve(ARTIFACT);
    }

    void writeResult(Path evidenceDir) throws Exception {
      boolean fileWritten = this.targetFile.isFile();
      boolean fileNonempty = fileWritten && this.targetFile.length() > 0;
      boolean proven = this.chooserObserved && this.approvedSelection && fileWritten && fileNonempty;
      String status = proven ? "proven" : "unsupported";
      String reason = proven
          ? "save_fire_approved_chooser_wrote_project_file"
          : "save_fire_e2e_not_completed";
      Files.createDirectories(evidenceDir);
      Files.writeString(
          artifactPath(evidenceDir),
          "{\n"
              + "  \"schema_version\": \"eatme.alice-desktop-stageide-save-menu-e2e-write-proof/v1\",\n"
              + "  \"status\": \"" + status + "\",\n"
              + "  \"reason\": \"" + reason + "\",\n"
              + "  \"proof_chain\": {\n"
              + "    \"step1\": \"SaveProjectOperation.fire(UserActivity) called (same entry as Save menu doClick, proved in PR #235)\",\n"
              + "    \"step2\": \"AbstractSaveOperation.perform(activity) runs on EDT\",\n"
              + "    \"step3\": \"SaveOperationFlow.run() calls context.showSaveFileDialog()\",\n"
              + "    \"step4\": \"context.showSaveFileDialog() -> application.getDocumentFrame().showSaveFileDialog(dir, name, ext)\",\n"
              + "    \"step5\": \"DocumentFrame.showSaveFileDialog -> FileDialogUtilities.showSaveFileDialog(JFrame, ...) -> SwingFileDialog.show() -> JFileChooser.showSaveDialog(root)\",\n"
              + "    \"step6\": \"JFileChooser observed in Window.getWindows() poll under Xvfb\",\n"
              + "    \"step7\": \"JFileChooser.setSelectedFile(targetFile) + approveSelection() called by background probe via invokeLater\",\n"
              + "    \"step8\": \"showSaveDialog returns APPROVE_OPTION; showSaveFileDialog returns targetFile\",\n"
              + "    \"step9\": \"SaveOperationFlow calls saveAction.save(targetFile) -> application.saveProjectTo(targetFile)\",\n"
              + "    \"step10\": \"targetFile written to disk as non-empty .a3p\"\n"
              + "  },\n"
              + "  \"observed_dialog\": {\n"
              + "    \"dialog_class\": " + stringJson(this.dialogClass) + ",\n"
              + "    \"dialog_showing\": " + this.dialogShowing + ",\n"
              + "    \"chooser_observed\": " + this.chooserObserved + ",\n"
              + "    \"approved_selection\": " + this.approvedSelection + ",\n"
              + "    \"poll_count\": " + this.pollCount + "\n"
              + "  },\n"
              + "  \"written_artifact\": {\n"
              + "    \"target_file\": " + stringJson(this.targetFile.getAbsolutePath()) + ",\n"
              + "    \"file_written\": " + fileWritten + ",\n"
              + "    \"file_nonempty\": " + fileNonempty + ",\n"
              + "    \"file_size_bytes\": " + (fileWritten ? this.targetFile.length() : 0) + "\n"
              + "  },\n"
              + "  \"doesNotClaim\": [\n"
              + "    \"Save menu item doClick was the trigger (that was PR #235)\",\n"
              + "    \"native java.awt.FileDialog peer display/control\",\n"
              + "    \"desktop pixels or visible rendering were validated\",\n"
              + "    \"first-lesson completion\",\n"
              + "    \"grading\"\n"
              + "  ]\n"
              + "}\n",
          StandardCharsets.UTF_8);
    }

    private void poll() {
      int count = ++this.pollCount;
      for (Window window : Window.getWindows()) {
        if (window instanceof JDialog dialog) {
          JFileChooser chooser = findChooser(dialog);
          if (chooser != null) {
            this.chooserObserved = true;
            this.dialogShowing = dialog.isShowing();
            this.dialogClass = dialog.getClass().getName();
            this.bgTimer.cancel();
            // Approve on EDT so JFileChooser's modal loop processes it.
            final JFileChooser fc = chooser;
            SwingUtilities.invokeLater(() -> {
              fc.setSelectedFile(this.targetFile);
              fc.approveSelection();
              this.approvedSelection = true;
            });
            return;
          }
        }
      }
      if (count >= MAX_POLLS) {
        this.bgTimer.cancel();
        // Timed out - cancel any open choosers so invokeAndWait can unblock.
        SwingUtilities.invokeLater(() -> {
          for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d) {
              JFileChooser fc = findChooser(d);
              if (fc != null) {
                fc.cancelSelection();
              }
            }
          }
        });
      }
    }

    private static JFileChooser findChooser(Component component) {
      if (component instanceof JFileChooser chooser) {
        return chooser;
      }
      if (component instanceof Container container) {
        for (Component child : container.getComponents()) {
          JFileChooser found = findChooser(child);
          if (found != null) {
            return found;
          }
        }
      }
      return null;
    }

    private static String stringJson(String value) {
      return value == null ? "null" : "\"" + FileDialogUtilities.escapeJson(value) + "\"";
    }
  }

  // ---- helpers ----

  private static void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  private void restoreLicensePreference() {
    if (previousLicenseAccepted == null) {
      licensePreferences.remove("isLicenseAccepted");
    } else {
      licensePreferences.put("isLicenseAccepted", previousLicenseAccepted);
    }
  }

  private static void disposeAwtWindows() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      for (Window window : Window.getWindows()) {
        window.dispose();
      }
    });
  }

  private static void resetActiveApplication() throws Exception {
    Field singleton = Application.class.getDeclaredField("singleton");
    singleton.setAccessible(true);
    singleton.set(null, null);
  }

  /**
   * Injects a UriProjectLoader stub directly into the application's private field
   * so that ProjectSaveTargetPlan.choose() can call isNewProject()/isDefaultBackup()
   * without throwing a NullPointerException. This field is normally set by
   * ProjectApplication.loadProject(), but we avoid the full async load here.
   */
  private static void injectUriProjectLoader(StageIDE ide, UriProjectLoader loader) throws Exception {
    Class<?> c = ide.getClass();
    while (c != null) {
      try {
        Field f = c.getDeclaredField("uriProjectLoader");
        f.setAccessible(true);
        f.set(ide, loader);
        return;
      } catch (NoSuchFieldException e) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchFieldException("uriProjectLoader not found in class hierarchy");
  }

  /**
   * Creates a minimal valid project containing a program type with a scene field.
   * This is the same pattern used by EatmeSaveProjectTest.projectWithScene().
   */
  private static Project minimalProject() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  /**
   * Minimal UriProjectLoader stub for a new project.
   * Only isNewProject() and isDefaultBackup() are consulted by ProjectSaveTargetPlan.
   */
  private static final class NewProjectStub extends UriProjectLoader {
    NewProjectStub() {
      super(false);
    }

    @Override
    public URI getUri() {
      return URI.create("blank://new-project");
    }

    @Override
    public boolean isNewProject() {
      return true;
    }

    @Override
    protected Project load() {
      return minimalProject();
    }
  }

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "stageide-save-menu-e2e-write-proof-test",
        UUID.randomUUID().toString()));
  }
}
