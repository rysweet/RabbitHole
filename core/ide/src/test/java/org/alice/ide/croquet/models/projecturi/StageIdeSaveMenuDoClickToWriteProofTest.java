package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.alice.ide.ProjectDocument;
import org.alice.ide.project.ProjectDocumentState;
import org.alice.ide.uricontent.UriProjectLoader;
import org.alice.stageide.StageIDE;
import org.alice.stageide.sceneeditor.StorytellingSceneEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.croquet.MenuModel;
import org.lgna.croquet.history.UserActivity;
import org.lgna.croquet.views.Menu;
import org.lgna.croquet.views.MenuItem;
import org.lgna.croquet.views.ViewController;
import org.lgna.project.License;
import org.lgna.project.Project;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
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
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

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
 *   → IoUtilities.readProject(targetFile)
 *   → readback project contains saveMenuDoClickRoundTripMarker
 *
 * <p>The background probe uses a java.util.Timer (daemon thread, independent of EDT) so
 * it fires even while the EDT is blocked inside JFileChooser's secondary event loop.
 */
public class StageIdeSaveMenuDoClickToWriteProofTest {
  private static final String READBACK_MARKER = "saveMenuDoClickRoundTripMarker";

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
   * Proves the full Save menu item doClick → live JFileChooser approval → written readable .a3p file.
   *
   * <p>The Save menu item is obtained via getMenuItemPrepModel().createMenuItemAndAddTo(),
   * the same factory the real StageIDE menu bar uses. doClick() on that item triggers the
   * Croquet ActionEvent dispatch path used by File→Save menu activation.
   *
   * <p>The JFileChooser is approved (not cancelled) by the background probe. The resulting
   * .a3p file is verified to exist, be non-empty, read back as a project, and contain
   * the synthetic marker from the project created by this test.
   *
   * <p>doesNotClaim: desktop pixels or visible rendering were validated, native
   * java.awt.FileDialog peer, first-lesson completion, grading, physical user click.
   */
  @Test(timeout = 60000)
  public void saveMenuDoClickApprovesChooserAndWritesProjectFile() throws Exception {
    Path testDir = newTestDir().resolve("doclick-to-written-file");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    File targetFile = projectsDir.resolve("doclick-save-proof.a3p").toAbsolutePath().toFile();
    assumeTrue("Skipping in headless — no AWT display available",
        SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable());

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

      capturedMenuItem[0] = createSaveMenuItem();
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
    // This is the same dispatch path used by File→Save menu activation.
    // The EDT blocks inside JFileChooser.showSaveDialog's secondary event loop until
    // the probe approves the selection (or times out).
    try {
      final MenuItem item = capturedMenuItem[0];
      SwingUtilities.invokeAndWait(() -> {
        probe.markMenuItemDoClickTriggered();
        item.doClick();
      });
    } finally {
      probe.stop();
    }

    assertTrue("target .a3p file must exist after save", targetFile.isFile());
    assertTrue("target .a3p file must be non-empty", targetFile.length() > 0);
    assertTrue("target file must use .a3p extension", targetFile.getName().endsWith(".a3p"));
    assertTrue("saved file must stay inside controlled temp projects dir",
        targetFile.getCanonicalFile().toPath().startsWith(projectsDir.toRealPath()));

    Project savedProject = IoUtilities.readProject(targetFile);
    assertNotNull("saved .a3p must read back as an Alice project", savedProject);
    boolean markerPresent = projectContainsMarker(savedProject, READBACK_MARKER);
    assertTrue("saved .a3p readback must contain " + READBACK_MARKER, markerPresent);
    probe.recordReadback(READBACK_MARKER, true, markerPresent);

    // Step 4: Write evidence artifact and assert the full chain.
    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"proven\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_approved_chooser_wrote_readable_project_with_marker\""));
    assertTrue(json, json.contains("\"claim\": \"Save menu item doClick opened a Swing JFileChooser, approved the selected .a3p path, wrote a non-empty project file, read it back, and verified the expected marker\""));
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
    assertTrue(json, json.contains("\"project_readable\": true"));
    assertTrue(json, json.contains("\"expected_marker\": \"" + READBACK_MARKER + "\""));
    assertTrue(json, json.contains("\"marker_present\": true"));
    assertFalse(json, json.contains(FileDialogUtilities.escapeJson(targetFile.getCanonicalPath())));
    assertTrue(json, json.contains("\"full lesson completion\""));
    assertTrue(json, json.contains("\"visible rendering correctness\""));
    assertTrue(json, json.contains("\"grading correctness\""));
    assertTrue(json, json.contains("\"physical user click\""));
    assertTrue(json, json.contains("\"broad UI automation coverage\""));
    assertTrue(json, json.contains("\"native dialog coverage\""));
    assertTrue(json, json.contains("\"all Save variants\""));
    assertFalse(json, json.contains("\"status\": \"unsupported\""));
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
    assertFalse(json, json.contains("\"status\": \"not_proven\""));
    assertTrue(json, json.contains("\"reason\": \"No available non-headless AWT display\""));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"approved_selection\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertTrue(json, json.contains("\"Save menu/control/dialog/write/readback/marker path\""));
    assertTrue(json, json.contains("\"requiresNextEvidence\""));
    assertTrue(json, json.contains("\"full desktop Save completion\""));
    assertTrue(json, json.contains("\"physical user click\""));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("\"wroteFile\": true"));
    assertFalse(json, json.contains("\"Save menu/control/dialog/write path\""));
    assertFalse(json, json.contains("approved the selected .a3p path"));
    assertFalse(json, json.contains("wrote a non-empty project file"));
  }

  @Test
  public void blockerArtifactRequiresResilientXvfbCommandWhenDisplayIsUnavailable() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("no-display-blocker-xvfb-command"));
    Boolean previousCachedDisplayAvailable = SaveMenuDoClickProbe.cachedNonHeadlessAwtDisplayAvailable;
    try {
      SaveMenuDoClickProbe.cachedNonHeadlessAwtDisplayAvailable = false;

      Path artifact = SaveMenuDoClickProbe.writeNoAvailableNonHeadlessAwtDisplayBlocker(evidenceDir);

      String json = Files.readString(artifact);
      assertTrue(json, json.contains("xvfb-run --auto-servernum -s \\\"-screen 0 1024x768x24 -ac\\\""));
      assertFalse(json, json.contains("xvfb-run" + " -a"));
    } finally {
      SaveMenuDoClickProbe.cachedNonHeadlessAwtDisplayAvailable = previousCachedDisplayAvailable;
    }
  }

  @Test
  public void incompleteArtifactDoesNotClaimChooserApprovalOrFileWrite() throws Exception {
    Path testDir = newTestDir().resolve("incomplete-evidence");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    File targetFile = testDir.resolve("projects/doclick-save-proof.a3p").toAbsolutePath().toFile();
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetFile, testDir);

    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"not_proven\""));
    assertFalse(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_e2e_not_completed\""));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"menu_item_doclick\": false"));
    assertTrue(json, json.contains("\"approved_selection\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertTrue(json, json.contains("\"reporting_summary\": \"Save menu item doClick write/readback path was not proven; chooser approval, file writing, project readback, and marker verification remain unproven\""));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("\"menu_item_doclick\": true"));
    assertFalse(json, json.contains("approved the selected .a3p path"));
    assertFalse(json, json.contains("wrote a non-empty project file"));
    assertFalse(json, json.contains("approveSelection() called"));
    assertFalse(json, json.contains("targetFile written to disk as non-empty .a3p"));
  }

  @Test
  public void chooserAndFileSignalsWithoutMenuDoClickDoNotProduceProvenArtifact() throws Exception {
    Path testDir = newTestDir().resolve("shortcut-signals-without-menu-doclick");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path targetPath = Files.createDirectories(testDir.resolve("projects")).resolve("doclick-save-proof.a3p");
    Files.writeString(targetPath, "preexisting file is not menu Save proof", StandardCharsets.UTF_8);
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetPath.toAbsolutePath().toFile(), testDir);
    probe.chooserObserved = true;
    probe.dialogShowing = true;
    probe.dialogClass = JDialog.class.getName();
    probe.normalizedSelectedFile = targetPath.toFile().getCanonicalPath();
    probe.selectedFileVerified = true;
    probe.approvedSelection.set(true);

    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"not_proven\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_not_recorded\""));
    assertTrue(json, json.contains("\"menu_item_doclick\": false"));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"approved_selection\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"file_nonempty\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertTrue(json, json.contains("\"file_size_bytes\": " + Files.size(targetPath)));
    assertFalse(json, json.contains("\"status\": \"proven\""));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("\"menu_item_doclick\": true"));
    assertFalse(json, json.contains("\"wroteFile\": true"));
    assertFalse(json, json.contains("\"approved_selection\": true"));
    assertFalse(json, json.contains("\"file_written\": true"));
    assertFalse(json, json.contains("approved the selected .a3p path"));
    assertFalse(json, json.contains("wrote a non-empty project file"));
  }

  @Test
  public void chooserAndFileSignalsWithMenuDoClickWithoutReadbackDoNotProduceProvenArtifact() throws Exception {
    Path testDir = newTestDir().resolve("completed-signals-with-menu-doclick-without-readback");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path targetPath = Files.createDirectories(testDir.resolve("projects")).resolve("doclick-save-proof.a3p");
    Files.writeString(targetPath, "saved project file", StandardCharsets.UTF_8);
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetPath.toAbsolutePath().toFile(), testDir);
    probe.markMenuItemDoClickTriggered();
    probe.chooserObserved = true;
    probe.dialogShowing = true;
    probe.dialogClass = JDialog.class.getName();
    probe.normalizedSelectedFile = targetPath.toFile().getCanonicalPath();
    probe.selectedFileVerified = true;
    probe.approvedSelection.set(true);

    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"not_proven\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_written_project_not_readable\""));
    assertTrue(json, json.contains("\"menu_item_doclick\": true"));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"approved_selection\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"file_nonempty\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertFalse(json, json.contains("\"status\": \"proven\""));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("\"wroteFile\": true"));
    assertFalse(json, json.contains("\"approved_selection\": true"));
    assertFalse(json, json.contains("\"file_written\": true"));
    assertFalse(json, json.contains("\"project_readable\": true"));
    assertFalse(json, json.contains("\"marker_present\": true"));
  }

  @Test
  public void chooserAndFileSignalsWithMenuDoClickAndReadbackMarkerProduceProvenArtifact() throws Exception {
    Path testDir = newTestDir().resolve("completed-signals-with-menu-doclick-readback-marker");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path targetPath = Files.createDirectories(testDir.resolve("projects")).resolve("doclick-save-proof.a3p");
    Files.writeString(targetPath, "saved project file", StandardCharsets.UTF_8);
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetPath.toAbsolutePath().toFile(), testDir);
    probe.markMenuItemDoClickTriggered();
    probe.chooserObserved = true;
    probe.dialogShowing = true;
    probe.dialogClass = JDialog.class.getName();
    probe.normalizedSelectedFile = targetPath.toFile().getCanonicalPath();
    probe.selectedFileVerified = true;
    probe.approvedSelection.set(true);
    probe.recordReadback(READBACK_MARKER, true, true);

    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"proven\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_approved_chooser_wrote_readable_project_with_marker\""));
    assertTrue(json, json.contains("\"menu_item_doclick\": true"));
    assertTrue(json, json.contains("\"wroteFile\": true"));
    assertTrue(json, json.contains("\"approved_selection\": true"));
    assertTrue(json, json.contains("\"file_written\": true"));
    assertTrue(json, json.contains("\"file_nonempty\": true"));
    assertTrue(json, json.contains("\"project_readable\": true"));
    assertTrue(json, json.contains("\"expected_marker\": \"" + READBACK_MARKER + "\""));
    assertTrue(json, json.contains("\"marker_present\": true"));
    assertTrue(json, json.contains("\"claim\": \"Save menu item doClick opened a Swing JFileChooser, approved the selected .a3p path, wrote a non-empty project file, read it back, and verified the expected marker\""));
    assertFalse(json, json.contains("\"status\": \"not_proven\""));
    assertFalse(json, json.contains("\"reporting_summary\""));
  }

  @Test
  public void existingFileWithoutFullProofDoesNotClaimSaveWrite() throws Exception {
    Path testDir = newTestDir().resolve("existing-file-without-proof");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path targetPath = Files.createDirectories(testDir.resolve("projects")).resolve("doclick-save-proof.a3p");
    Files.writeString(targetPath, "preexisting", StandardCharsets.UTF_8);
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetPath.toAbsolutePath().toFile(), testDir);

    probe.writeResult(evidenceDir);

    String json = Files.readString(probe.artifactPath(evidenceDir));
    assertTrue(json, json.contains("\"status\": \"not_proven\""));
    assertFalse(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertFalse(json, json.contains("\"wroteFile\": true"));
    assertFalse(json, json.contains("\"file_written\": true"));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("wrote a non-empty project file"));
    assertFalse(json, json.contains("targetFile written to disk as non-empty .a3p"));
  }

  @Test
  public void repeatedPollsApproveChooserOnlyOnceAndDoNotClaimIncompleteWriteProof() throws Exception {
    assumeTrue("requires Xvfb or another headful AWT display",
        SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable());
    disposeAwtWindows();
    Path testDir = newTestDir().resolve("poll-race-incomplete-proof");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path targetPath = Files.createDirectories(testDir.resolve("projects")).resolve("doclick-save-proof.a3p");
    SaveMenuDoClickProbe probe = new SaveMenuDoClickProbe(targetPath.toAbsolutePath().toFile(), testDir);
    CountingFileChooser[] chooser = new CountingFileChooser[1];
    JDialog[] dialog = new JDialog[1];
    CountDownLatch edtBlocked = new CountDownLatch(1);
    CountDownLatch releaseEdt = new CountDownLatch(1);

    try {
      SwingUtilities.invokeAndWait(() -> {
        chooser[0] = new CountingFileChooser();
        dialog[0] = new JDialog();
        dialog[0].add(chooser[0]);
        dialog[0].pack();
        dialog[0].setVisible(true);
      });
      SwingUtilities.invokeLater(() -> {
        edtBlocked.countDown();
        try {
          if (!releaseEdt.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("EDT release latch timed out");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AssertionError("Interrupted while holding EDT for poll race test", e);
        }
      });
      assertTrue("EDT blocker must start", edtBlocked.await(5, TimeUnit.SECONDS));

      int pollCallbacks = 8;
      CountDownLatch startPolls = new CountDownLatch(1);
      ExecutorService executor = Executors.newFixedThreadPool(pollCallbacks);
      try {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < pollCallbacks; i++) {
          futures.add(executor.submit(() -> {
            if (!startPolls.await(5, TimeUnit.SECONDS)) {
              throw new AssertionError("poll start latch timed out");
            }
            probe.poll();
            return null;
          }));
        }
        startPolls.countDown();
        for (Future<?> future : futures) {
          future.get(5, TimeUnit.SECONDS);
        }
      } finally {
        executor.shutdownNow();
      }

      releaseEdt.countDown();
      SwingUtilities.invokeAndWait(() -> { /* drain queued approvals */ });
      assertEquals("repeated poll callbacks must enqueue one chooser approval", 1, chooser[0].approvalCount());
      assertEquals("repeated callbacks after terminal discovery must not mutate poll count", 1, probe.pollCount.get());

      probe.writeResult(evidenceDir);
      String json = Files.readString(probe.artifactPath(evidenceDir));
      assertTrue(json, json.contains("\"status\": \"not_proven\""));
      assertFalse(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"save_menu_doclick_e2e_not_completed\""));
      assertTrue(json, json.contains("\"poll_count\": 1"));
      assertTrue(json, json.contains("\"approved_selection\": false"));
      assertTrue(json, json.contains("\"wroteFile\": false"));
      assertTrue(json, json.contains("\"file_written\": false"));
      assertTrue(json, json.contains("\"project_readable\": false"));
      assertTrue(json, json.contains("\"marker_present\": false"));
      assertFalse(json, json.contains("\"approved_selection\": true"));
      assertFalse(json, json.contains("\"wroteFile\": true"));
      assertFalse(json, json.contains("\"file_written\": true"));
      assertFalse(json, json.contains("\"claim\""));
      assertFalse(json, json.contains("approved the selected .a3p path"));
      assertFalse(json, json.contains("wrote a non-empty project file"));
    } finally {
      releaseEdt.countDown();
      SwingUtilities.invokeAndWait(() -> {
        if (dialog[0] != null) {
          dialog[0].dispose();
        }
      });
      probe.stop();
    }
  }

  // ---- inner probe ----

  private static class CountingFileChooser extends JFileChooser {
    private final AtomicInteger approvalCount = new AtomicInteger();

    @Override
    public void approveSelection() {
      this.approvalCount.incrementAndGet();
      super.approveSelection();
    }

    int approvalCount() {
      return this.approvalCount.get();
    }
  }

  /**
   * Background daemon timer that polls Window.getWindows(), finds the JFileChooser
   * opened by the doClick() path, sets the target file, and approves via invokeLater.
   */
  private static class SaveMenuDoClickProbe {
    private static final String ARTIFACT = "stageide-save-menu-doclick-write-proof.json";
    private static final int MAX_POLLS = 400;
    private static final int MAX_CHOOSER_CANDIDATES = 2;
    private static volatile Boolean cachedNonHeadlessAwtDisplayAvailable;

    private final File targetFile;
    private final Path proofRoot;
    private final String targetCanonicalPath;
    private volatile boolean chooserObserved;
    private final AtomicBoolean pollingFinished = new AtomicBoolean(false);
    private final AtomicBoolean approvalScheduled = new AtomicBoolean(false);
    private final AtomicBoolean approvalApplied = new AtomicBoolean(false);
    private final AtomicBoolean approvedSelection = new AtomicBoolean(false);
    private volatile boolean selectedFileVerified;
    private volatile boolean ambiguousChooserDiscovery;
    private volatile boolean dialogShowing;
    private volatile String dialogClass;
    private volatile String normalizedSelectedFile;
    private volatile String expectedReadbackMarker = READBACK_MARKER;
    private volatile boolean projectReadable;
    private volatile boolean readbackMarkerPresent;
    private volatile String failureReason;
    private final AtomicInteger pollCount = new AtomicInteger();
    private final AtomicBoolean menuItemDoClickTriggered = new AtomicBoolean(false);
    private volatile java.util.Timer bgTimer;

    SaveMenuDoClickProbe(File targetFile, Path proofRoot) throws java.io.IOException {
      this.targetFile = targetFile;
      this.proofRoot = proofRoot.toRealPath();
      this.targetCanonicalPath = targetFile.getCanonicalPath();
    }

    void start() {
      this.pollingFinished.set(false);
      this.bgTimer = new java.util.Timer("save-menu-doclick-probe", /* daemon= */ true);
      this.bgTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          poll();
        }
      }, 0, 100);
    }

    void stop() {
      cancelTimer();
    }

    void markMenuItemDoClickTriggered() {
      this.menuItemDoClickTriggered.set(true);
    }

    void recordReadback(String expectedMarker, boolean projectReadable, boolean markerPresent) {
      this.expectedReadbackMarker = expectedMarker;
      this.projectReadable = projectReadable;
      this.readbackMarkerPresent = markerPresent;
    }

    Path artifactPath(Path evidenceDir) {
      return artifactPathFor(evidenceDir);
    }

    static boolean isNonHeadlessAwtDisplayAvailable() {
      Boolean cached = cachedNonHeadlessAwtDisplayAvailable;
      if (cached != null) {
        return cached;
      }
      boolean available = detectNonHeadlessAwtDisplayAvailable();
      cachedNonHeadlessAwtDisplayAvailable = available;
      return available;
    }

    private static boolean detectNonHeadlessAwtDisplayAvailable() {
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
               + "  \"approved_selection\": false,\n"
               + "  \"file_written\": false,\n"
               + "  \"file_nonempty\": false,\n"
               + "  \"readback\": {\n"
               + "    \"project_readable\": false,\n"
               + "    \"expected_marker\": \"" + READBACK_MARKER + "\",\n"
               + "    \"marker_present\": false\n"
               + "  },\n"
               + "  \"proofTarget\": \"Save menu/control/dialog/write/readback/marker path\",\n"
              + "  \"reporting_summary\": \"Save menu/control/dialog/write/readback/marker path requires a non-headless AWT display before it can be proven\",\n"
              + "  \"blocker\": {\n"
              + "    \"observed\": \"GraphicsEnvironment.isHeadless() is true or no usable desktop display is available\",\n"
              + "    \"required\": \"Xvfb or another non-headless AWT display capable of showing a Swing JFileChooser\"\n"
              + "  },\n"
              + "  \"requiresNextEvidence\": [\n"
              + "    \"Run this proof shard under xvfb-run --auto-servernum -s \\\"-screen 0 1024x768x24 -ac\\\" or an equivalent desktop session\",\n"
              + "    \"Save menu/control/dialog/write/readback/marker path artifact with status proven\"\n"
              + "  ],\n"
              + "  \"doesNotClaim\": [\n"
              + "    \"full desktop Save completion\",\n"
              + "    \"full lesson completion\",\n"
              + "    \"visible rendering correctness\",\n"
              + "    \"grading correctness\",\n"
              + "    \"physical user click\",\n"
              + "    \"broad UI automation coverage\",\n"
              + "    \"native dialog coverage\",\n"
              + "    \"all Save variants\"\n"
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
      Path selectedPath = normalizedPath(selectedCanonical);
      Path targetPath = normalizedPath(this.targetCanonicalPath);
      String selectedEvidencePath = proofRelativePath(selectedPath);
      String targetEvidencePath = proofRelativePath(targetPath);
      boolean fileWritten = this.targetFile.isFile();
      long fileSizeBytes = fileWritten ? this.targetFile.length() : 0;
      boolean fileNonempty = fileSizeBytes > 0;
      boolean fileHasExpectedExtension = this.targetFile.getName().endsWith(".a3p");
      boolean targetInsideProofRoot = proofContainsPath(targetPath);
      boolean selectedFileMatchesExpected = this.targetCanonicalPath.equals(selectedCanonical);
      boolean observedExpectedFile = fileWritten && fileNonempty && fileHasExpectedExtension && targetInsideProofRoot;
      boolean chooserApproved = this.approvedSelection.get();
      boolean menuDoClickTriggered = this.menuItemDoClickTriggered.get();
      boolean readbackVerified = this.projectReadable && this.readbackMarkerPresent;
      boolean wouldBeProvenExceptMenu = this.chooserObserved
          && chooserApproved
          && this.selectedFileVerified
          && selectedFileMatchesExpected
          && !this.ambiguousChooserDiscovery
          && observedExpectedFile
          && this.failureReason == null;
      boolean proven = menuDoClickTriggered && wouldBeProvenExceptMenu && readbackVerified;
      boolean claimedApprovedSelection = proven && chooserApproved;
      boolean claimedWroteFile = proven && observedExpectedFile;
      boolean claimedFileWritten = proven && fileWritten;
      boolean claimedFileNonempty = proven && fileNonempty;
      boolean claimedProjectReadable = proven && this.projectReadable;
      boolean claimedMarkerPresent = proven && this.readbackMarkerPresent;
      String status = proven ? "proven" : "not_proven";
      String reason = proofReason(proven, menuDoClickTriggered, wouldBeProvenExceptMenu, this.projectReadable);
      String claimOrSummaryJson = proven
          ? "Save menu item doClick opened a Swing JFileChooser, approved the selected .a3p path, wrote a non-empty project file, read it back, and verified the expected marker"
          : "Save menu item doClick write/readback path was not proven; chooser approval, file writing, project readback, and marker verification remain unproven";
      claimOrSummaryJson = proven
          ? "  \"claim\": \"" + FileDialogUtilities.escapeJson(claimOrSummaryJson) + "\",\n"
          : "  \"reporting_summary\": \"" + FileDialogUtilities.escapeJson(claimOrSummaryJson) + "\",\n";
      String observedChooserStep = this.chooserObserved && !this.ambiguousChooserDiscovery
          ? "JFileChooser observed in Window.getWindows() poll under Xvfb"
          : "JFileChooser observation was not proven in this run";
      String chooserApprovalStep = claimedApprovedSelection && this.selectedFileVerified
          ? "JFileChooser.setSelectedFile(targetFile) + approveSelection() called by background probe via invokeLater"
          : "JFileChooser approval was not proven in this run";
      String approvedReturnStep = claimedApprovedSelection
          ? "showSaveDialog returns APPROVE_OPTION; showSaveFileDialog returns targetFile"
          : "showSaveDialog approval return was not proven in this run";
      String saveActionStep = claimedWroteFile
          ? "SaveOperationFlow calls saveAction.save(targetFile) -> application.saveProjectTo(targetFile)"
          : "Save action write to the target file was not proven in this run";
      String writtenArtifactStep = claimedWroteFile
          ? "targetFile written to disk as non-empty .a3p"
          : "Non-empty target .a3p write was not proven in this run";
      String readbackStep = claimedProjectReadable
          ? "IoUtilities.readProject(targetFile) returned a non-null project"
          : "Readable project round-trip was not proven in this run";
      String markerStep = claimedMarkerPresent
          ? "readback project contains expected marker " + this.expectedReadbackMarker
          : "Readback marker verification was not proven in this run";
      String menuDoClickStep = menuDoClickTriggered
          ? "menuItem.doClick() on actual Save menu item (created via getMenuItemPrepModel().createMenuItemAndAddTo())"
          : "menuItem.doClick() was not recorded in this run";
      String menuDoClickDescription = menuDoClickTriggered
          ? "menuItem.doClick() on save MenuItem created by getMenuItemPrepModel().createMenuItemAndAddTo()"
          : "menuItem.doClick() was not recorded before this evidence artifact was written";
      Files.createDirectories(evidenceDir);
      Files.writeString(
          artifactPath(evidenceDir),
          "{\n"
              + "  \"schema_version\": \"eatme.alice-desktop-stageide-save-menu-doclick-write-proof/v1\",\n"
               + "  \"status\": \"" + status + "\",\n"
               + "  \"reason\": \"" + reason + "\",\n"
               + "  \"dialogType\": \"Swing JFileChooser\",\n"
               + "  \"wroteFile\": " + claimedWroteFile + ",\n"
              + claimOrSummaryJson
                + "  \"proof_chain\": {\n"
              + "    \"step1\": \"" + menuDoClickStep + "\",\n"
              + "    \"step2\": \"Swing ActionEvent dispatched by doClick() → Croquet OperationSwingModel\",\n"
              + "    \"step3\": \"SaveProjectOperation.fire(UserActivity) called by Croquet\",\n"
              + "    \"step4\": \"AbstractSaveOperation.perform(activity) runs on EDT\",\n"
              + "    \"step5\": \"SaveOperationFlow.run() calls context.showSaveFileDialog()\",\n"
              + "    \"step6\": \"context.showSaveFileDialog() -> application.getDocumentFrame().showSaveFileDialog(dir, name, ext)\",\n"
              + "    \"step7\": \"DocumentFrame.showSaveFileDialog -> FileDialogUtilities -> JFileChooser.showSaveDialog(root)\",\n"
              + "    \"step8\": \"" + observedChooserStep + "\",\n"
              + "    \"step9\": \"" + chooserApprovalStep + "\",\n"
              + "    \"step10\": \"" + approvedReturnStep + "\",\n"
              + "    \"step11\": \"" + saveActionStep + "\",\n"
              + "    \"step12\": \"" + writtenArtifactStep + "\",\n"
              + "    \"step13\": \"" + readbackStep + "\",\n"
              + "    \"step14\": \"" + markerStep + "\"\n"
               + "  },\n"
               + "  \"trigger\": {\n"
               + "    \"menu_item_doclick\": " + menuDoClickTriggered + ",\n"
               + "    \"trigger_description\": \"" + menuDoClickDescription + "\"\n"
               + "  },\n"
              + "  \"observed_dialog\": {\n"
               + "    \"dialog_class\": " + stringJson(this.dialogClass) + ",\n"
               + "    \"dialog_showing\": " + this.dialogShowing + ",\n"
               + "    \"chooser_observed\": " + this.chooserObserved + ",\n"
              + "    \"dialogType\": \"Swing JFileChooser\",\n"
              + "    \"ambiguous_chooser_discovery\": " + this.ambiguousChooserDiscovery + ",\n"
               + "    \"approved_selection\": " + claimedApprovedSelection + ",\n"
               + "    \"poll_count\": " + this.pollCount.get() + "\n"
               + "  },\n"
               + "  \"selected_file\": {\n"
               + "    \"selected_file_verified\": " + this.selectedFileVerified + ",\n"
               + "    \"normalized_selected_file\": " + stringJson(selectedEvidencePath) + ",\n"
               + "    \"expected_file\": " + stringJson(targetEvidencePath) + ",\n"
               + "    \"selected_file_matches_expected\": " + selectedFileMatchesExpected + "\n"
               + "  },\n"
               + "  \"written_artifact\": {\n"
               + "    \"target_file\": " + stringJson(targetEvidencePath) + ",\n"
                 + "    \"file_written\": " + claimedFileWritten + ",\n"
               + "    \"file_nonempty\": " + claimedFileNonempty + ",\n"
               + "    \"file_extension\": \"a3p\",\n"
               + "    \"file_has_expected_extension\": " + fileHasExpectedExtension + ",\n"
               + "    \"target_inside_proof_root\": " + targetInsideProofRoot + ",\n"
               + "    \"file_size_bytes\": " + fileSizeBytes + "\n"
               + "  },\n"
               + "  \"readback\": {\n"
               + "    \"project_readable\": " + claimedProjectReadable + ",\n"
               + "    \"expected_marker\": " + stringJson(this.expectedReadbackMarker) + ",\n"
               + "    \"marker_present\": " + claimedMarkerPresent + "\n"
               + "  },\n"
                + "  \"doesNotClaim\": [\n"
               + "    \"full desktop Save completion\",\n"
               + "    \"full lesson completion\",\n"
               + "    \"visible rendering correctness\",\n"
               + "    \"grading correctness\",\n"
               + "    \"physical user click\",\n"
               + "    \"broad UI automation coverage\",\n"
               + "    \"native dialog coverage\",\n"
               + "    \"all Save variants\"\n"
               + "  ]\n"
               + "}\n",
          StandardCharsets.UTF_8);
    }

    private synchronized void poll() {
      if (this.pollingFinished.get()) {
        return;
      }
      int count = this.pollCount.incrementAndGet();
      List<ChooserCandidate> candidates = findChooserCandidates();
      if (candidates.size() > 1) {
        this.chooserObserved = true;
        this.ambiguousChooserDiscovery = true;
        this.failureReason = "ambiguous_swing_jfilechooser_discovery";
        finishPolling();
        cancelCurrentChoosersOnEdt();
        return;
      }
      if (candidates.size() == 1) {
        ChooserCandidate candidate = candidates.get(0);
        this.chooserObserved = true;
        this.dialogShowing = candidate.dialog().isShowing();
        this.dialogClass = candidate.dialog().getClass().getName();
        if (this.approvalScheduled.compareAndSet(false, true)) {
          finishPolling();
          approveChooserOnEdt(candidate.chooser());
        }
        return;
      }
      if (count >= MAX_POLLS) {
        this.failureReason = "swing_jfilechooser_not_observed_before_timeout";
        finishPolling();
        cancelCurrentChoosersOnEdt();
      }
    }

    private String proofReason(
        boolean proven,
        boolean menuDoClickTriggered,
        boolean wouldBeProvenExceptMenu,
        boolean projectReadable) {
      if (proven) {
        return "save_menu_doclick_approved_chooser_wrote_readable_project_with_marker";
      }
      if (this.failureReason != null) {
        return this.failureReason;
      }
      if (!wouldBeProvenExceptMenu) {
        return "save_menu_doclick_e2e_not_completed";
      }
      if (!menuDoClickTriggered) {
        return "save_menu_doclick_not_recorded";
      }
      return projectReadable
          ? "save_menu_doclick_written_project_marker_missing"
          : "save_menu_doclick_written_project_not_readable";
    }

    private void finishPolling() {
      this.pollingFinished.set(true);
      cancelTimer();
    }

    private void cancelTimer() {
      java.util.Timer timer = this.bgTimer;
      if (timer != null) {
        timer.cancel();
        this.bgTimer = null;
      }
    }

    private void approveChooserOnEdt(JFileChooser chooser) {
      SwingUtilities.invokeLater(() -> {
        if (!this.approvalApplied.compareAndSet(false, true)) {
          return;
        }
        try {
          chooser.setSelectedFile(this.targetFile);
          File selectedFile = chooser.getSelectedFile();
          this.normalizedSelectedFile = selectedFile == null ? null : selectedFile.getCanonicalPath();
          this.selectedFileVerified = this.targetCanonicalPath.equals(this.normalizedSelectedFile);
          if (this.selectedFileVerified) {
            chooser.approveSelection();
            this.approvedSelection.set(true);
          } else {
            this.failureReason = "selected_file_did_not_match_expected_target";
            chooser.cancelSelection();
          }
        } catch (java.io.IOException ioe) {
          Logger.throwable(ioe, "Save proof selected file canonicalization failed for: " + this.targetFile);
          this.failureReason = "selected_file_canonicalization_failed";
          chooser.cancelSelection();
        }
      });
    }

    private static void cancelCurrentChoosersOnEdt() {
      SwingUtilities.invokeLater(() -> {
        for (ChooserCandidate candidate : findChooserCandidates(Integer.MAX_VALUE)) {
          candidate.chooser().cancelSelection();
        }
      });
    }

    private static List<ChooserCandidate> findChooserCandidates() {
      return findChooserCandidates(MAX_CHOOSER_CANDIDATES);
    }

    private static List<ChooserCandidate> findChooserCandidates(int maxCandidates) {
      List<ChooserCandidate> candidates = new ArrayList<>(Math.min(maxCandidates, MAX_CHOOSER_CANDIDATES));
      for (Window window : Window.getWindows()) {
        if (window instanceof JDialog dialog && dialog.isShowing()) {
          if (addChooserCandidates(dialog, dialog, candidates, maxCandidates)) {
            break;
          }
        }
      }
      return candidates;
    }

    private static boolean addChooserCandidates(
        JDialog dialog,
        Component component,
        List<ChooserCandidate> candidates,
        int maxCandidates) {
      if (component instanceof JFileChooser chooser) {
        candidates.add(new ChooserCandidate(dialog, chooser));
        if (candidates.size() >= maxCandidates) {
          return true;
        }
      }
      if (component instanceof Container container) {
        for (Component child : container.getComponents()) {
          if (addChooserCandidates(dialog, child, candidates, maxCandidates)) {
            return true;
          }
        }
      }
      return false;
    }

    private static Path artifactPathFor(Path evidenceDir) {
      Path artifact = evidenceDir.resolve(ARTIFACT).normalize();
      if (!artifact.startsWith(evidenceDir.normalize())) {
        throw new SecurityException("Security violation: Save menu doClick proof artifact attempts path traversal outside evidence dir");
      }
      return artifact;
    }

    private String proofRelativePath(Path path) {
      if (path == null) {
        return null;
      }
      if (!proofContainsPath(path)) {
        return "[outside-proof-root]";
      }
      return this.proofRoot.relativize(path).toString().replace(File.separatorChar, '/');
    }

    private boolean proofContainsPath(Path path) {
      return path != null && path.startsWith(this.proofRoot);
    }

    private static Path normalizedPath(String canonicalPath) {
      return canonicalPath == null ? null : Path.of(canonicalPath).normalize();
    }

    private static String stringJson(String value) {
      return value == null ? "null" : "\"" + FileDialogUtilities.escapeJson(value) + "\"";
    }

    private record ChooserCandidate(JDialog dialog, JFileChooser chooser) {
    }
  }

  // ---- helpers ----

  private static MenuItem createSaveMenuItem() {
    Menu hostMenu = new Menu(new MenuModel(UUID.fromString("132f2a47-2e7c-4af0-910c-0cb10f74afe8")) {
    });
    ViewController<?, ?> menuItem =
        SaveProjectOperation.getInstance().getMenuItemPrepModel().createMenuItemAndAddTo(hostMenu);
    assertTrue("createMenuItemAndAddTo must produce a MenuItem", menuItem instanceof MenuItem);
    assertTrue("host menu must contain the returned Save item", hostMenuContainsReturnedItem(hostMenu, (MenuItem) menuItem));
    return (MenuItem) menuItem;
  }

  private static boolean hostMenuContainsReturnedItem(Menu hostMenu, MenuItem menuItem) {
    Component menuComponent = menuItem.getAwtComponent();
    for (int i = 0; i < hostMenu.getAwtComponent().getMenuComponentCount(); i++) {
      if (hostMenu.getAwtComponent().getMenuComponent(i) == menuComponent) {
        return true;
      }
    }
    return false;
  }

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
    sceneType.methods.add(new UserMethod(
        READBACK_MARKER,
        JavaType.VOID_TYPE,
        new UserParameter[0],
        new BlockStatement(new Comment("Save menu doClick round-trip marker"))));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static boolean projectContainsMarker(Project project, String marker) {
    return project != null && project.getNamedUserTypes().stream()
        .anyMatch(type -> type.getDeclaredMethod(marker) != null);
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
