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
import org.lgna.croquet.views.AwtComponentView;
import org.lgna.croquet.views.AwtContainerView;
import org.lgna.croquet.views.CascadeMenu;
import org.lgna.croquet.views.CascadeMenuItem;
import org.lgna.croquet.views.CheckBoxMenuItem;
import org.lgna.croquet.views.Menu;
import org.lgna.croquet.views.MenuItem;
import org.lgna.croquet.views.MenuItemContainer;
import org.lgna.croquet.views.MenuTextSeparator;
import org.lgna.croquet.views.ViewController;
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
import javax.swing.event.PopupMenuListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Proves the full Save menu item doClick → JFileChooser approved → project file written path.
 *
 * <p>This is the narrow increment after PR #273 (which proved
 * SaveProjectOperation.fire() → JFileChooser approved → .a3p written). That test
 * called fire() directly. PR #235 proved Save menu item doClick dispatches into
 * AbstractSaveOperation but used PROOF_ONLY mode and did not prove the write path.
 *
 * <p>This test closes the gap: it creates the actual Save menu item via
 * getMenuItemPrepModel().createMenuItemAndAddTo() (same as the real menu bar)
 * and triggers the full chain through menuItem.doClick() — not fire() directly.
 *
 * <p>Chain proved:
 * menuItem.doClick()                                              [Swing ActionEvent on EDT]
 *   → OperationSwingModel Croquet dispatch                        [same path as real menu bar]
 *   → SaveProjectOperation.fire(UserActivity)
 *   → AbstractSaveOperation.perform(activity)
 *   → SaveOperationFlow.run(context, ...)
 *   → context.showSaveFileDialog() → JFileChooser.showSaveDialog(root)
 *   → JFileChooser observed in Window.getWindows() poll
 *   → JFileChooser.setSelectedFile(targetFile) + approveSelection() by background probe
 *   → showSaveDialog returns APPROVE_OPTION; showSaveFileDialog returns targetFile
 *   → SaveOperationFlow calls saveAction.save(targetFile)
 *   → application.saveProjectTo(targetFile)
 *   → targetFile.a3p written to disk (non-empty)
 *
 * <p>The background probe uses a java.util.Timer (daemon thread, independent of EDT) so
 * it fires even while the EDT is blocked inside JFileChooser's secondary event loop.
 */
public class StageIdeSaveMenuDoClickToWriteProofTest {
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
   * Proves the full Save menu item doClick → live JFileChooser approval → written .a3p file.
   *
   * <p>The Save menu item is obtained via getMenuItemPrepModel().createMenuItemAndAddTo(),
   * the same factory the real StageIDE menu bar uses. doClick() on that item triggers the
   * same Croquet ActionEvent dispatch path as a real user click on the File→Save menu item.
   *
   * <p>The JFileChooser is approved (not cancelled) by the background probe. The resulting
   * .a3p file is verified to exist and be non-empty, proving the full write path from
   * menu item doClick through to disk.
   *
   * <p>doesNotClaim: desktop pixels or visible rendering were validated, native
   * java.awt.FileDialog peer, first-lesson completion, grading.
   */
  @Test(timeout = 60000)
  public void saveMenuDoClickApprovesChooserAndWritesProjectFile() throws Exception {
    Path testDir = newTestDir().resolve("doclick-to-written-file");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    File targetFile = projectsDir.resolve("doclick-save-proof.a3p").toAbsolutePath().toFile();
    if (!SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable()) {
      Path artifact = SaveMenuDoClickProbe.writeNoAvailableNonHeadlessAwtDisplayBlocker(evidenceDir);
      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"No available non-headless AWT display\""));
      assertTrue(json, json.contains("\"wroteFile\": false"));
      assertFalse(json, json.contains("\"wroteFile\": true"));
      return;
    }

    // Ensure path-injection bypass is NOT active so the real dialog is displayed.
    System.clearProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());

    // Point ApplicationRoot at the pre-built distribution directory so the JOGL native
    // library loader finds its libs without showing a blocking error dialog.
    Path distributionDir = Paths.get(System.getProperty("user.dir"))
        .resolve("../../core/resources/target/distribution").normalize();
    if (distributionDir.toFile().isDirectory()) {
      System.setProperty("org.alice.ide.rootDirectory", distributionDir.toString());
    }

    resetActiveApplication();

    // Capture the save menu item so doClick() can be called outside invokeAndWait.
    MenuItem[] capturedMenuItem = new MenuItem[1];

    // Step 1: Initialize StageIDE on the EDT and create the actual Save menu item.
    // Project state is injected before setVisible so the save path has a non-null project.
    SwingUtilities.invokeAndWait(() -> {
      StageIDE ide = new StageIDE(new CrashDetector(StageIdeSaveMenuDoClickToWriteProofTest.class));
      licensePreferences.putBoolean("isLicenseAccepted", true);
      ide.initialize(new String[0]);
      Project project = minimalProject();
      try {
        ProjectDocumentState.getInstance().setValueTransactionlessly(
            new ProjectDocument(project, new UserActivity()));
        injectUriProjectLoader(ide, new NewProjectLoader());
      } catch (Exception e) {
        throw new RuntimeException("project state injection failed", e);
      }
      ide.getDocumentFrame().getFrame().pack();
      ide.getDocumentFrame().getFrame().setVisible(true);
      assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isDisplayable());
      assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isShowing());

      // Create the real Save menu item via getMenuItemPrepModel() — same as StageIDE menu bar.
      CapturingMenuItemContainer menu = new CapturingMenuItemContainer();
      ViewController<?, ?> menuItem =
          SaveProjectOperation.getInstance().getMenuItemPrepModel().createMenuItemAndAddTo(menu);
      assertTrue("createMenuItemAndAddTo must produce a MenuItem", menuItem instanceof MenuItem);
      assertTrue("CapturingMenuItemContainer must capture the item", menu.menuItem == menuItem);
      capturedMenuItem[0] = menu.menuItem;
    });

    // Drain post-setVisible events (windowOpened, license checks).
    SwingUtilities.invokeAndWait(() -> { /* drain pass 1 */ });
    SwingUtilities.invokeAndWait(() -> { /* drain pass 2 */ });

    // Null out sceneCameraImp so ThumbnailGenerator.createThumbnail() returns null (fast path)
    // instead of attempting an offscreen OpenGL render that blocks forever.
    SwingUtilities.invokeAndWait(() -> {
      try {
        Field sceneCameraImpField = StorytellingSceneEditor.class.getDeclaredField("sceneCameraImp");
        sceneCameraImpField.setAccessible(true);
        sceneCameraImpField.set(StorytellingSceneEditor.getInstance(), null);
      } catch (Exception e) {
        throw new RuntimeException("failed to null sceneCameraImp on StorytellingSceneEditor", e);
      }
    });

    // Step 2: Start background probe (daemon timer, independent of EDT).
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetFile, testDir);
    probe.start();

    // Step 3: Trigger via doClick() on the actual Save menu item — not fire() directly.
    // This is the same dispatch path as a real user clicking File→Save in the menu bar.
    // The EDT blocks inside JFileChooser.showSaveDialog's secondary event loop until
    // the probe approves the selection (or times out).
    try {
      final MenuItem item = capturedMenuItem[0];
      SwingUtilities.invokeAndWait(item::doClick);
    } finally {
      probe.stop();
    }

    // Step 4: Write evidence artifact and assert the full chain.
    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"proven\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_approved_chooser_wrote_project_file\""));
    assertTrue(json, json.contains("\"menu_item_doclick\": true"));
    assertTrue(json, json.contains("\"chooser_observed\": true"));
    assertTrue(json, json.contains("\"approved_selection\": true"));
    assertTrue(json, json.contains("\"file_written\": true"));
    assertTrue(json, json.contains("\"file_nonempty\": true"));
    assertTrue(json, json.contains("\"target_inside_proof_root\": true"));
    assertTrue(json, json.contains("\"dialogType\": \"Swing JFileChooser\""));
    assertTrue(json, json.contains("\"wroteFile\": true"));
    assertTrue(json, json.contains("\"selected_file_verified\": true"));
    assertTrue(json, json.contains("\"ambiguous_chooser_discovery\": false"));
    assertTrue(json, json.contains("\"normalized_selected_file\": \"projects/doclick-save-proof.a3p\""));
    assertFalse(json, json.contains(FileDialogUtilities.escapeJson(targetFile.getCanonicalPath())));
    assertTrue(json, json.contains("\"claim\": \"Save menu item doClick opened a Swing JFileChooser, approved the selected .a3p path, and wrote a non-empty project file\""));
    assertTrue(json, json.contains("\"full lesson completion\""));
    assertTrue(json, json.contains("\"visible rendering correctness\""));
    assertTrue(json, json.contains("\"grading correctness\""));
    assertTrue(json, json.contains("\"broad UI automation coverage\""));
    assertTrue(json, json.contains("\"native dialog coverage\""));
    assertFalse(json, json.contains("\"status\": \"unsupported\""));

    assertTrue("target .a3p file must exist after save", targetFile.isFile());
    assertTrue("target .a3p file must be non-empty", targetFile.length() > 0);
    assertTrue("target file must use .a3p extension", targetFile.getName().endsWith(".a3p"));
    assertTrue("saved file must stay inside controlled temp projects dir",
        targetFile.getCanonicalFile().toPath().startsWith(projectsDir.toRealPath()));
  }

  @Test
  public void blockerArtifactReportsNoAvailableNonHeadlessAwtDisplay() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("no-display-blocker"));

    if (SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable()) {
      boolean refused = false;
      try {
        SaveMenuDoClickProbe.writeNoAvailableNonHeadlessAwtDisplayBlocker(evidenceDir);
      } catch (IllegalStateException expected) {
        refused = true;
        assertTrue(expected.getMessage().contains("non-headless AWT display is available"));
      }
      assertTrue("blocker artifact must not be written when a display is available", refused);
      assertFalse(Files.exists(evidenceDir.resolve(SaveMenuDoClickProbe.ARTIFACT)));
      return;
    }

    Path artifact = SaveMenuDoClickProbe.writeNoAvailableNonHeadlessAwtDisplayBlocker(evidenceDir);

    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"reason\": \"No available non-headless AWT display\""));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"Save menu/control/dialog/write path\""));
    assertTrue(json, json.contains("\"requiresNextEvidence\""));
    assertFalse(json, json.contains("\"wroteFile\": true"));
  }

  // ---- inner probe ----

  /**
   * Background daemon timer that polls Window.getWindows(), finds the JFileChooser
   * opened by the doClick() path, sets the target file, and approves via invokeLater.
   */
  private static class SaveMenuDoClickProbe {
    private static final String ARTIFACT = "stageide-save-menu-doclick-write-proof.json";
    private static final int MAX_POLLS = 400;

    private final File targetFile;
    private final Path proofRoot;
    private final String targetCanonicalPath;
    private volatile boolean chooserObserved;
    private volatile boolean approvedSelection;
    private volatile boolean selectedFileVerified;
    private volatile boolean ambiguousChooserDiscovery;
    private volatile boolean dialogShowing;
    private volatile String dialogClass;
    private volatile String normalizedSelectedFile;
    private volatile String failureReason;
    private volatile int pollCount;
    private java.util.Timer bgTimer;

    SaveMenuDoClickProbe(File targetFile, Path proofRoot) throws java.io.IOException {
      this.targetFile = targetFile;
      this.proofRoot = proofRoot.toRealPath();
      this.targetCanonicalPath = targetFile.getCanonicalPath();
    }

    void start() {
      this.bgTimer = new java.util.Timer("save-menu-doclick-probe", /* daemon= */ true);
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
      return artifactPathFor(evidenceDir);
    }

    static boolean isNonHeadlessAwtDisplayAvailable() {
      if (GraphicsEnvironment.isHeadless()) {
        return false;
      }
      java.awt.Frame probeFrame = null;
      try {
        probeFrame = new java.awt.Frame();
        probeFrame.pack();
        return true;
      } catch (java.awt.HeadlessException | java.awt.AWTError e) {
        return false;
      } finally {
        if (probeFrame != null) {
          probeFrame.dispose();
        }
      }
    }

    private static Path writeNoAvailableNonHeadlessAwtDisplayBlocker(Path evidenceDir) throws Exception {
      if (isNonHeadlessAwtDisplayAvailable()) {
        throw new IllegalStateException("Cannot write display blocker because a non-headless AWT display is available");
      }
      Files.createDirectories(evidenceDir);
      Path artifact = artifactPathFor(evidenceDir);
      Files.writeString(
          artifact,
          "{\n"
              + "  \"schema_version\": \"eatme.alice-desktop-stageide-save-menu-doclick-write-proof/v1\",\n"
              + "  \"status\": \"unsupported\",\n"
              + "  \"reason\": \"No available non-headless AWT display\",\n"
              + "  \"dialogType\": \"Swing JFileChooser\",\n"
              + "  \"wroteFile\": false,\n"
              + "  \"proofTarget\": \"Save menu/control/dialog/write path\",\n"
              + "  \"claim\": \"Save menu/control/dialog/write path requires a non-headless AWT display before it can be proven\",\n"
              + "  \"blocker\": {\n"
              + "    \"observed\": \"GraphicsEnvironment.isHeadless() is true or no usable desktop display is available\",\n"
              + "    \"required\": \"Xvfb or another non-headless AWT display capable of showing a Swing JFileChooser\"\n"
              + "  },\n"
              + "  \"requiresNextEvidence\": [\n"
              + "    \"Run this proof shard under xvfb-run -a or an equivalent desktop session\",\n"
              + "    \"Save menu/control/dialog/write path artifact with status proven\"\n"
              + "  ],\n"
              + "  \"doesNotClaim\": [\n"
              + "    \"full lesson completion\",\n"
              + "    \"visible rendering correctness\",\n"
              + "    \"grading correctness\",\n"
              + "    \"broad UI automation coverage\",\n"
              + "    \"native dialog coverage\"\n"
              + "  ]\n"
              + "}\n",
          StandardCharsets.UTF_8);
      if (!Files.isRegularFile(artifact) || Files.size(artifact) == 0) {
        throw new java.io.IOException("Save proof display blocker artifact was not written: " + artifact);
      }
      return artifact;
    }

    void writeResult(Path evidenceDir) throws Exception {
      String selectedCanonical = this.normalizedSelectedFile;
      String selectedEvidencePath = proofRelativePath(selectedCanonical);
      String targetEvidencePath = proofRelativePath(this.targetCanonicalPath);
      boolean fileWritten = this.targetFile.isFile();
      long fileSizeBytes = fileWritten ? this.targetFile.length() : 0;
      boolean fileNonempty = fileSizeBytes > 0;
      boolean fileHasExpectedExtension = this.targetFile.getName().endsWith(".a3p");
      boolean targetInsideProofRoot = proofContainsPath(this.targetCanonicalPath);
      boolean selectedFileMatchesExpected = this.targetCanonicalPath.equals(selectedCanonical);
      boolean wroteFile = fileWritten && fileNonempty && fileHasExpectedExtension && targetInsideProofRoot;
      boolean proven = this.chooserObserved
          && this.approvedSelection
          && this.selectedFileVerified
          && selectedFileMatchesExpected
          && !this.ambiguousChooserDiscovery
          && wroteFile
          && this.failureReason == null;
      String status = proven ? "proven" : "unsupported";
      String reason = proven
          ? "save_menu_doclick_approved_chooser_wrote_project_file"
          : this.failureReason == null ? "save_menu_doclick_e2e_not_completed" : this.failureReason;
      Files.createDirectories(evidenceDir);
      Files.writeString(
          artifactPath(evidenceDir),
          "{\n"
              + "  \"schema_version\": \"eatme.alice-desktop-stageide-save-menu-doclick-write-proof/v1\",\n"
               + "  \"status\": \"" + status + "\",\n"
               + "  \"reason\": \"" + reason + "\",\n"
              + "  \"dialogType\": \"Swing JFileChooser\",\n"
              + "  \"wroteFile\": " + wroteFile + ",\n"
              + "  \"claim\": \"Save menu item doClick opened a Swing JFileChooser, approved the selected .a3p path, and wrote a non-empty project file\",\n"
               + "  \"proof_chain\": {\n"
              + "    \"step1\": \"menuItem.doClick() on actual Save menu item (created via getMenuItemPrepModel().createMenuItemAndAddTo())\",\n"
              + "    \"step2\": \"Swing ActionEvent dispatched by doClick() → Croquet OperationSwingModel\",\n"
              + "    \"step3\": \"SaveProjectOperation.fire(UserActivity) called by Croquet\",\n"
              + "    \"step4\": \"AbstractSaveOperation.perform(activity) runs on EDT\",\n"
              + "    \"step5\": \"SaveOperationFlow.run() calls context.showSaveFileDialog()\",\n"
              + "    \"step6\": \"context.showSaveFileDialog() -> application.getDocumentFrame().showSaveFileDialog(dir, name, ext)\",\n"
              + "    \"step7\": \"DocumentFrame.showSaveFileDialog -> FileDialogUtilities -> JFileChooser.showSaveDialog(root)\",\n"
              + "    \"step8\": \"JFileChooser observed in Window.getWindows() poll under Xvfb\",\n"
              + "    \"step9\": \"JFileChooser.setSelectedFile(targetFile) + approveSelection() called by background probe via invokeLater\",\n"
              + "    \"step10\": \"showSaveDialog returns APPROVE_OPTION; showSaveFileDialog returns targetFile\",\n"
              + "    \"step11\": \"SaveOperationFlow calls saveAction.save(targetFile) -> application.saveProjectTo(targetFile)\",\n"
              + "    \"step12\": \"targetFile written to disk as non-empty .a3p\"\n"
              + "  },\n"
              + "  \"trigger\": {\n"
              + "    \"menu_item_doclick\": true,\n"
              + "    \"trigger_description\": \"menuItem.doClick() on save MenuItem created by getMenuItemPrepModel().createMenuItemAndAddTo()\"\n"
              + "  },\n"
              + "  \"observed_dialog\": {\n"
               + "    \"dialog_class\": " + stringJson(this.dialogClass) + ",\n"
               + "    \"dialog_showing\": " + this.dialogShowing + ",\n"
               + "    \"chooser_observed\": " + this.chooserObserved + ",\n"
              + "    \"dialogType\": \"Swing JFileChooser\",\n"
              + "    \"ambiguous_chooser_discovery\": " + this.ambiguousChooserDiscovery + ",\n"
               + "    \"approved_selection\": " + this.approvedSelection + ",\n"
               + "    \"poll_count\": " + this.pollCount + "\n"
               + "  },\n"
               + "  \"selected_file\": {\n"
               + "    \"selected_file_verified\": " + this.selectedFileVerified + ",\n"
               + "    \"normalized_selected_file\": " + stringJson(selectedEvidencePath) + ",\n"
               + "    \"expected_file\": " + stringJson(targetEvidencePath) + ",\n"
               + "    \"selected_file_matches_expected\": " + selectedFileMatchesExpected + "\n"
               + "  },\n"
                + "  \"written_artifact\": {\n"
               + "    \"target_file\": " + stringJson(targetEvidencePath) + ",\n"
                + "    \"file_written\": " + fileWritten + ",\n"
               + "    \"file_nonempty\": " + fileNonempty + ",\n"
               + "    \"file_extension\": \"a3p\",\n"
               + "    \"file_has_expected_extension\": " + fileHasExpectedExtension + ",\n"
               + "    \"target_inside_proof_root\": " + targetInsideProofRoot + ",\n"
               + "    \"file_size_bytes\": " + fileSizeBytes + "\n"
               + "  },\n"
               + "  \"doesNotClaim\": [\n"
              + "    \"full lesson completion\",\n"
              + "    \"visible rendering correctness\",\n"
              + "    \"grading correctness\",\n"
              + "    \"broad UI automation coverage\",\n"
              + "    \"native dialog coverage\"\n"
               + "  ]\n"
               + "}\n",
          StandardCharsets.UTF_8);
    }

    private void poll() {
      int count = ++this.pollCount;
      List<ChooserCandidate> candidates = findChooserCandidates();
      if (candidates.size() > 1) {
        this.chooserObserved = true;
        this.ambiguousChooserDiscovery = true;
        this.failureReason = "ambiguous_swing_jfilechooser_discovery";
        this.bgTimer.cancel();
        cancelChoosersOnEdt(candidates);
        return;
      }
      if (candidates.size() == 1) {
        ChooserCandidate candidate = candidates.get(0);
        this.chooserObserved = true;
        this.dialogShowing = candidate.dialog().isShowing();
        this.dialogClass = candidate.dialog().getClass().getName();
        this.bgTimer.cancel();
        approveChooserOnEdt(candidate.chooser());
        return;
      }
      if (count >= MAX_POLLS) {
        this.failureReason = "swing_jfilechooser_not_observed_before_timeout";
        this.bgTimer.cancel();
        cancelCurrentChoosersOnEdt();
      }
    }

    private void approveChooserOnEdt(JFileChooser chooser) {
      SwingUtilities.invokeLater(() -> {
        try {
          chooser.setSelectedFile(this.targetFile);
          File selectedFile = chooser.getSelectedFile();
          this.normalizedSelectedFile = selectedFile == null ? null : selectedFile.getCanonicalPath();
          this.selectedFileVerified = this.targetCanonicalPath.equals(this.normalizedSelectedFile);
          if (this.selectedFileVerified) {
            this.approvedSelection = true;
            chooser.approveSelection();
          } else {
            this.failureReason = "selected_file_did_not_match_expected_target";
            chooser.cancelSelection();
          }
        } catch (java.io.IOException ioe) {
          this.failureReason = "selected_file_canonicalization_failed";
          chooser.cancelSelection();
        }
      });
    }

    private static void cancelChoosersOnEdt(List<ChooserCandidate> candidates) {
      SwingUtilities.invokeLater(() -> {
        for (ChooserCandidate candidate : candidates) {
          candidate.chooser().cancelSelection();
        }
      });
    }

    private static void cancelCurrentChoosersOnEdt() {
      SwingUtilities.invokeLater(() -> {
        for (ChooserCandidate candidate : findChooserCandidates()) {
          candidate.chooser().cancelSelection();
        }
      });
    }

    private static List<ChooserCandidate> findChooserCandidates() {
      List<ChooserCandidate> candidates = new ArrayList<>();
      for (Window window : Window.getWindows()) {
        if (window instanceof JDialog dialog && dialog.isShowing()) {
          addChooserCandidates(dialog, dialog, candidates);
        }
      }
      return candidates;
    }

    private static void addChooserCandidates(JDialog dialog, Component component, List<ChooserCandidate> candidates) {
      if (component instanceof JFileChooser chooser) {
        candidates.add(new ChooserCandidate(dialog, chooser));
      }
      if (component instanceof Container container) {
        for (Component child : container.getComponents()) {
          addChooserCandidates(dialog, child, candidates);
        }
      }
    }

    private static Path artifactPathFor(Path evidenceDir) {
      Path artifact = evidenceDir.resolve(ARTIFACT).normalize();
      if (!artifact.startsWith(evidenceDir.normalize())) {
        throw new IllegalArgumentException("Save menu doClick proof artifact escapes evidence dir");
      }
      return artifact;
    }

    private String proofRelativePath(String canonicalPath) {
      if (canonicalPath == null) {
        return null;
      }
      Path path = Path.of(canonicalPath).normalize();
      if (!proofContainsPath(canonicalPath)) {
        return "[outside-proof-root]";
      }
      return this.proofRoot.relativize(path).toString().replace(File.separatorChar, '/');
    }

    private boolean proofContainsPath(String canonicalPath) {
      return canonicalPath != null && Path.of(canonicalPath).normalize().startsWith(this.proofRoot);
    }

    private static String stringJson(String value) {
      return value == null ? "null" : "\"" + FileDialogUtilities.escapeJson(value) + "\"";
    }

    private record ChooserCandidate(JDialog dialog, JFileChooser chooser) {
    }
  }

  // ---- CapturingMenuItemContainer ----

  /**
   * Minimal MenuItemContainer that captures the MenuItem added to it.
   * Used to obtain the actual Save menu item from getMenuItemPrepModel().createMenuItemAndAddTo().
   */
  private static final class CapturingMenuItemContainer implements MenuItemContainer {
    private static final AwtComponentView<?>[] NO_MENU_COMPONENTS = new AwtComponentView<?>[0];

    private MenuItem menuItem;

    @Override
    public ViewController<?, ?> getViewController() {
      return null;
    }

    @Override
    public void addPopupMenuListener(PopupMenuListener listener) {
    }

    @Override
    public void removePopupMenuListener(PopupMenuListener listener) {
    }

    @Override
    public UserActivity getActivity() {
      return new UserActivity();
    }

    @Override
    public AwtContainerView<?> getParent() {
      return null;
    }

    @Override
    public AwtComponentView<?>[] getMenuComponents() {
      return NO_MENU_COMPONENTS;
    }

    @Override
    public AwtComponentView<?> getMenuComponent(int i) {
      return null;
    }

    @Override
    public int getMenuComponentCount() {
      return 0;
    }

    @Override
    public void addMenu(Menu menu) {
    }

    @Override
    public void addMenuItem(MenuItem menuItem) {
      this.menuItem = menuItem;
    }

    @Override
    public void addCascadeMenu(CascadeMenu cascadeMenu) {
    }

    @Override
    public void addCascadeMenuItem(CascadeMenuItem cascadeMenuItem) {
    }

    @Override
    public void addCheckBoxMenuItem(CheckBoxMenuItem checkBoxMenuItem) {
    }

    @Override
    public void addCascadeCombo(CascadeMenuItem cascadeMenuItem, CascadeMenu cascadeMenu) {
    }

    @Override
    public void addSeparator() {
    }

    @Override
    public void addSeparator(MenuTextSeparator menuTextSeparator) {
    }

    @Override
    public void forgetAndRemoveAllMenuItems() {
    }

    @Override
    public void removeAllMenuItems() {
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
   * Injects a UriProjectLoader test double so ProjectSaveTargetPlan.choose() can
   * use the same new-project branch normally prepared by ProjectApplication.loadProject().
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
   */
  private static Project minimalProject() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static final class NewProjectLoader extends UriProjectLoader {
    NewProjectLoader() {
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
        "stageide-save-menu-doclick-write-proof-test",
        UUID.randomUUID().toString()));
  }
}
