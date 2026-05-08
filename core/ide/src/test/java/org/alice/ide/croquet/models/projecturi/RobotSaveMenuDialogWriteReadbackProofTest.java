package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.alice.ide.ProjectDocument;
import org.alice.ide.croquet.models.menubar.FileMenuModel;
import org.alice.ide.project.ProjectDocumentState;
import org.alice.ide.uricontent.UriProjectLoader;
import org.alice.stageide.StageIDE;
import org.alice.stageide.sceneeditor.StorytellingSceneEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.croquet.StandardMenuItemPrepModel;
import org.lgna.croquet.history.UserActivity;
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
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.AWTError;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Joins the next Save seam: Robot File-menu activation, live Save dialog control,
 * .a3p write, IoUtilities readback, and marker verification.
 */
public class RobotSaveMenuDialogWriteReadbackProofTest {
  private static final String ARTIFACT = "robot-save-menu-dialog-write-readback-proof.json";
  private static final String SCHEMA_VERSION =
      "eatme.alice-desktop-robot-save-menu-dialog-write-readback-proof/v1";
  private static final String READBACK_MARKER = "robotSaveMenuRoundTripMarker";
  private static final String TARGET_FILE_NAME = "robot-save-menu-proof.a3p";

  private String previousDiscoveryEvidenceDir;
  private String previousSelectedPath;
  private String previousSaveEvidenceDir;
  private String previousProofOnly;
  private Preferences licensePreferences;
  private String previousLicenseAccepted;

  @Before
  public void captureProperties() {
    previousDiscoveryEvidenceDir =
        System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    previousSaveEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    previousProofOnly = System.getProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY);
    licensePreferences = Preferences.userNodeForPackage(License.class);
    previousLicenseAccepted = licensePreferences.get("isLicenseAccepted", null);
  }

  @After
  public void restorePropertiesAndResetApplication() throws Exception {
    disposeAwtWindows();
    restoreLicensePreference();
    restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousDiscoveryEvidenceDir);
    restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
    restoreProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousSaveEvidenceDir);
    restoreProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, previousProofOnly);
    resetActiveApplication();
  }

  @Test(timeout = 90000)
  public void robotFileSaveApprovesChooserWritesReadableMarkedProjectOrWritesBlocker()
      throws Exception {
    Path proofRoot = canonicalProofRoot();
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    Path artifact = proofRoot.resolve(ARTIFACT);
    Files.deleteIfExists(artifact);
    File targetFile = projectsDir.resolve(TARGET_FILE_NAME).toAbsolutePath().toFile();
    Files.deleteIfExists(targetFile.toPath());

    RobotSaveProofEvidence evidence = new RobotSaveProofEvidence(targetFile, proofRoot);
    if (!isNonHeadlessAwtDisplayAvailable()) {
      evidence.block("headless_awt",
          "No available non-headless AWT display",
          "Xvfb or another non-headless AWT display capable of Robot mouse events and Swing JFileChooser display");
      evidence.write(proofRoot);
      assertBlockedArtifact(artifact, "headless_awt");
      return;
    }

    System.clearProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, proofRoot.toString());
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, proofRoot.toString());
    System.clearProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY);
    useDistributionDirectoryIfAvailable();
    resetActiveApplication();

    AtomicReference<StageIDE> ideRef = new AtomicReference<>();
    AtomicReference<JFrame> menuFrameRef = new AtomicReference<>();
    AtomicReference<JMenu> fileMenuRef = new AtomicReference<>();

    try {
      SwingUtilities.invokeAndWait(() -> {
        StageIDE ide = new StageIDE(new CrashDetector(RobotSaveMenuDialogWriteReadbackProofTest.class));
        licensePreferences.putBoolean("isLicenseAccepted", true);
        ide.initialize(new String[0]);
        try {
          ProjectDocumentState.getInstance().setValueTransactionlessly(
              new ProjectDocument(minimalProject(), new UserActivity()));
          injectUriProjectLoader(ide, new NewProjectLoader());
        } catch (Exception e) {
          throw new RuntimeException("project state injection failed", e);
        }
        ide.getDocumentFrame().getFrame().pack();
        ide.getDocumentFrame().getFrame().setLocation(520, 80);
        ide.getDocumentFrame().getFrame().setVisible(true);
        assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isDisplayable());
        assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isShowing());
        ideRef.set(ide);

        FileMenuModel fileMenuModel = findFileMenuModel(ide);
        assertNotNull("FileMenuModel not found in AliceMenuBar children", fileMenuModel);
        org.lgna.croquet.MenuBarComposite testMenuBarComposite =
            new org.lgna.croquet.MenuBarComposite(UUID.randomUUID());
        testMenuBarComposite.addItem(fileMenuModel);
        org.lgna.croquet.views.Frame croquetFrame = new org.lgna.croquet.views.Frame();
        croquetFrame.setMenuBarComposite(testMenuBarComposite);
        JFrame menuFrame = croquetFrame.getAwtComponent();
        menuFrame.setTitle("Robot Save Menu Dialog Write Readback Proof");
        menuFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        menuFrame.setSize(420, 180);
        menuFrame.setLocation(40, 80);
        menuFrame.pack();
        menuFrame.setVisible(true);
        menuFrameRef.set(menuFrame);

        JMenuBar jMenuBar = menuFrame.getJMenuBar();
        assertNotNull("JMenuBar not set on Croquet Frame", jMenuBar);
        for (int i = 0; i < jMenuBar.getMenuCount(); i++) {
          JMenu menu = jMenuBar.getMenu(i);
          if (menu != null && "File".equals(menu.getText())) {
            fileMenuRef.set(menu);
            break;
          }
        }
        assertNotNull("File JMenu not found in rendered MenuBar", fileMenuRef.get());
      });
      drainEdt();
      disableThumbnailCameraRender();

      Robot robot;
      try {
        robot = new Robot();
      } catch (AWTException | SecurityException ex) {
        evidence.block("robot_unavailable",
            "java.awt.Robot could not be created for this display",
            "A desktop session that permits java.awt.Robot mouse events");
        evidence.write(proofRoot);
        assertBlockedArtifact(artifact, "robot_unavailable");
        return;
      }
      robot.setAutoDelay(40);
      robot.waitForIdle();
      robot.delay(250);

      if (!robotOpenFileMenu(robot, fileMenuRef.get())) {
        evidence.block("file_menu_not_showing",
            "Robot could not open the rendered File menu popup",
            "A visible Croquet File menu that accepts Robot mouse events");
        evidence.write(proofRoot);
        assertBlockedArtifact(artifact, "file_menu_not_showing");
        return;
      }
      evidence.robotFileMenuOpened = true;

      AtomicReference<Point> saveItemCenter = new AtomicReference<>();
      SwingUtilities.invokeAndWait(() -> {
        JMenu fileMenu = fileMenuRef.get();
        JPopupMenu popup = fileMenu.getPopupMenu();
        javax.swing.Action saveAction =
            SaveProjectOperation.getInstance().getImp().getSwingModel().getAction();
        for (Component component : popup.getComponents()) {
          if (component instanceof JMenuItem item && item.getAction() == saveAction && item.isShowing()) {
            evidence.saveActionIdentityMatched = true;
            Point loc = item.getLocationOnScreen();
            saveItemCenter.set(new Point(loc.x + item.getWidth() / 2, loc.y + item.getHeight() / 2));
            break;
          }
        }
      });
      if (saveItemCenter.get() == null) {
        evidence.block("save_item_not_attributed",
            "The rendered File popup did not expose the Save item by SaveProjectOperation action identity",
            "A Save menu item whose Swing Action is SaveProjectOperation");
        evidence.write(proofRoot);
        assertBlockedArtifact(artifact, "save_item_not_attributed");
        return;
      }

      RobotSaveDialogController controller = new RobotSaveDialogController(targetFile, evidence);
      controller.start();
      try {
        robot.mouseMove(saveItemCenter.get().x, saveItemCenter.get().y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        evidence.robotSaveItemClicked = true;
        robot.waitForIdle();
        waitForFileWrite(targetFile.toPath());
      } finally {
        controller.stop();
      }

      if (targetFile.isFile() && targetFile.length() > 0) {
        try {
          Project savedProject = IoUtilities.readProject(targetFile);
          evidence.recordReadback(savedProject != null, projectContainsMarker(savedProject, READBACK_MARKER));
        } catch (Exception ex) {
          evidence.block("readback_failed",
              "The written .a3p file could not be read back through IoUtilities.readProject",
              "A readable Alice project archive with the expected marker");
        }
      }
      evidence.write(proofRoot);

      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"schema_version\": \"" + SCHEMA_VERSION + "\""));
      assertTrue(json, json.contains("\"proofTarget\": \"Robot File menu Save activation joined to dialog/write/readback evidence\""));
      assertTrue(json, json.contains("\"baselinePreserved\""));
      assertTrue(json, json.contains("\"StageIdeSaveMenuDoClickToWriteProofTest\""));
      assertTrue(json, json.contains("\"ProjectApplicationSaveProjectToTest\""));
      assertTrue(json, json.contains("\"JMenuBarRobotClickSaveProofTest\""));
      if (json.contains("\"status\": \"proven\"")) {
        assertTrue(json, json.contains("\"claim\": \"AWT Robot opened File, clicked the production Save menu item, controlled the Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker\""));
        assertTrue(json, json.contains("\"robot_file_menu_opened\": true"));
        assertTrue(json, json.contains("\"robot_save_item_clicked\": true"));
        assertTrue(json, json.contains("\"save_action_identity_matched\": true"));
        assertTrue(json, json.contains("\"chooser_observed\": true"));
        assertTrue(json, json.contains("\"approved_selection\": true"));
        assertTrue(json, json.contains("\"file_written\": true"));
        assertTrue(json, json.contains("\"file_nonempty\": true"));
        assertTrue(json, json.contains("\"project_readable\": true"));
        assertTrue(json, json.contains("\"expected_marker\": \"" + READBACK_MARKER + "\""));
        assertTrue(json, json.contains("\"marker_present\": true"));
        assertFalse(json, json.contains("\"status\": \"blocked\""));
      } else {
        assertTrue(json, json.contains("\"status\": \"blocked\""));
        assertTrue(json, json.contains("\"blocker\""));
        assertTrue(json, json.contains("\"requiresNextEvidence\""));
        assertFalse(json, json.contains("\"claim\""));
      }
      assertFalse(json, json.contains(FileDialogUtilities.escapeJson(targetFile.getCanonicalPath())));
      assertTrue(json, json.contains("\"full desktop Save completion\""));
      assertTrue(json, json.contains("\"all Save variants\""));
      assertTrue(json, json.contains("\"Save As coverage\""));
    } finally {
      cleanupRobotProofResources(menuFrameRef, ideRef);
    }
  }

  @Test
  public void incompleteArtifactIsBlockedAndDoesNotClaimChooserWriteOrReadback() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    File targetFile = proofRoot.resolve("projects").resolve(TARGET_FILE_NAME).toFile();
    RobotSaveProofEvidence evidence = new RobotSaveProofEvidence(targetFile, proofRoot);

    evidence.write(proofRoot);

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"file_menu_not_showing\""));
    assertTrue(json, json.contains("\"robot_file_menu_opened\": false"));
    assertTrue(json, json.contains("\"robot_save_item_clicked\": false"));
    assertTrue(json, json.contains("\"approved_selection\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertTrue(json, json.contains("\"requiresNextEvidence\""));
    assertFalse(json, json.contains("\"claim\""));
  }

  @Test
  public void artifactRequiresRobotSaveClickBeforeReportingProven() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    File targetFile = Files.writeString(projectsDir.resolve(TARGET_FILE_NAME), "not an Alice project").toFile();
    RobotSaveProofEvidence evidence = new RobotSaveProofEvidence(targetFile, proofRoot);
    evidence.robotFileMenuOpened = true;
    evidence.saveActionIdentityMatched = true;
    evidence.chooserObserved = true;
    evidence.dialogShowing = true;
    evidence.dialogClass = JDialog.class.getName();
    evidence.selectedFileVerified = true;
    evidence.normalizedSelectedFile = targetFile.getCanonicalPath();
    evidence.targetInsideProofRoot = true;
    evidence.approvedSelection = true;
    evidence.recordReadback(true, true);

    evidence.write(proofRoot);

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"save_item_not_attributed\""));
    assertTrue(json, json.contains("\"robot_file_menu_opened\": true"));
    assertTrue(json, json.contains("\"robot_save_item_clicked\": false"));
    assertTrue(json, json.contains("\"file_written\": false"));
    assertTrue(json, json.contains("\"project_readable\": false"));
    assertTrue(json, json.contains("\"marker_present\": false"));
    assertFalse(json, json.contains("\"status\": \"proven\""));
    assertFalse(json, json.contains("\"claim\""));
  }

  @Test
  public void completeArtifactReportsNarrowProvenClaim() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    File targetFile = Files.writeString(projectsDir.resolve(TARGET_FILE_NAME), "non-empty proof file").toFile();
    RobotSaveProofEvidence evidence = new RobotSaveProofEvidence(targetFile, proofRoot);
    evidence.robotFileMenuOpened = true;
    evidence.robotSaveItemClicked = true;
    evidence.saveActionIdentityMatched = true;
    evidence.chooserObserved = true;
    evidence.dialogShowing = true;
    evidence.dialogClass = JDialog.class.getName();
    evidence.selectedFileVerified = true;
    evidence.normalizedSelectedFile = targetFile.getCanonicalPath();
    evidence.targetInsideProofRoot = true;
    evidence.approvedSelection = true;
    evidence.recordReadback(true, true);

    evidence.write(proofRoot);

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"proven\""));
    assertTrue(json, json.contains("\"claim\": \"AWT Robot opened File, clicked the production Save menu item, controlled the Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker\""));
    assertTrue(json, json.contains("\"normalized_selected_file\": \"projects/" + TARGET_FILE_NAME + "\""));
    assertTrue(json, json.contains("\"file_written\": true"));
    assertTrue(json, json.contains("\"file_nonempty\": true"));
    assertTrue(json, json.contains("\"project_readable\": true"));
    assertTrue(json, json.contains("\"marker_present\": true"));
    assertTrue(json, json.contains("\"full desktop Save completion\""));
    assertFalse(json, json.contains("\"status\": \"blocked\""));
    assertFalse(json, json.contains("\"blocker\""));
  }

  @Test
  public void targetOutsideProofRootIsBlockedAndRedacted() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path outsideRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-outside-test", UUID.randomUUID().toString()));
    File outsideTarget = Files.writeString(outsideRoot.resolve(TARGET_FILE_NAME), "outside").toFile();
    RobotSaveProofEvidence evidence = new RobotSaveProofEvidence(outsideTarget, proofRoot);
    evidence.robotFileMenuOpened = true;
    evidence.robotSaveItemClicked = true;
    evidence.saveActionIdentityMatched = true;
    evidence.chooserObserved = true;
    evidence.dialogShowing = true;
    evidence.dialogClass = JDialog.class.getName();
    evidence.selectedFileVerified = true;
    evidence.normalizedSelectedFile = outsideTarget.getCanonicalPath();
    evidence.targetInsideProofRoot = false;
    evidence.approvedSelection = true;
    evidence.recordReadback(true, true);

    evidence.write(proofRoot);

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"target_path_rejected\""));
    assertTrue(json, json.contains("\"normalized_selected_file\": \"[outside-proof-root]\""));
    assertFalse(json, json.contains(FileDialogUtilities.escapeJson(outsideTarget.getCanonicalPath())));
    assertFalse(json, json.contains("\"claim\""));
  }

  private static boolean robotOpenFileMenu(Robot robot, JMenu fileMenu) throws Exception {
    AtomicReference<Point> fileMenuCenter = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      if (fileMenu.isShowing()) {
        Point loc = fileMenu.getLocationOnScreen();
        fileMenuCenter.set(new Point(loc.x + fileMenu.getWidth() / 2, loc.y + fileMenu.getHeight() / 2));
      }
    });
    if (fileMenuCenter.get() == null) {
      return false;
    }
    robot.mouseMove(fileMenuCenter.get().x, fileMenuCenter.get().y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();
    for (int i = 0; i < 30; i++) {
      robot.delay(100);
      boolean[] visible = new boolean[1];
      SwingUtilities.invokeAndWait(() -> visible[0] = fileMenu.isPopupMenuVisible());
      if (visible[0]) {
        return true;
      }
    }
    return false;
  }

  private static void waitForFileWrite(Path target) throws Exception {
    for (int i = 0; i < 100; i++) {
      if (Files.isRegularFile(target) && Files.size(target) > 0) {
        return;
      }
      Thread.sleep(100);
    }
  }

  private static void assertBlockedArtifact(Path artifact, String blockerKind) throws Exception {
    assertTrue("blocker artifact missing: " + artifact, Files.isRegularFile(artifact));
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"" + blockerKind + "\""));
    assertTrue(json, json.contains("\"requiresNextEvidence\""));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("\"status\": \"proven\""));
  }

  private static class RobotSaveDialogController {
    private static final int MAX_POLLS = 500;
    private static final int MAX_CHOOSER_CANDIDATES = 2;

    private final File targetFile;
    private final RobotSaveProofEvidence evidence;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean approvalScheduled = new AtomicBoolean(false);
    private final AtomicBoolean approvalApplied = new AtomicBoolean(false);
    private final AtomicInteger pollCount = new AtomicInteger();
    private volatile java.util.Timer timer;

    RobotSaveDialogController(File targetFile, RobotSaveProofEvidence evidence) {
      this.targetFile = targetFile;
      this.evidence = evidence;
    }

    void start() {
      this.timer = new java.util.Timer("robot-save-dialog-controller", true);
      this.timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          poll();
        }
      }, 0, 100);
    }

    void stop() {
      cancelTimer();
    }

    private synchronized void poll() {
      if (this.finished.get()) {
        return;
      }
      int count = this.pollCount.incrementAndGet();
      this.evidence.pollCount = count;
      List<ChooserCandidate> candidates = findChooserCandidates(MAX_CHOOSER_CANDIDATES);
      if (candidates.size() > 1) {
        this.evidence.chooserObserved = true;
        this.evidence.ambiguousChooserDiscovery = true;
        this.evidence.block("ambiguous_chooser_discovery",
            "More than one live Swing JFileChooser was visible during Robot Save proof",
            "Exactly one attributable Swing JFileChooser");
        finish();
        cancelCurrentChoosersOnEdt();
        return;
      }
      if (candidates.size() == 1) {
        ChooserCandidate candidate = candidates.get(0);
        this.evidence.chooserObserved = true;
        this.evidence.dialogShowing = candidate.dialog().isShowing();
        this.evidence.dialogClass = candidate.dialog().getClass().getName();
        if (this.approvalScheduled.compareAndSet(false, true)) {
          finish();
          approveChooserOnEdt(candidate.chooser());
        }
        return;
      }
      if (count >= MAX_POLLS) {
        this.evidence.block("dialog_not_observed",
            "No expected live Swing JFileChooser appeared before the bounded wait expired",
            "A Save dialog opened by the Robot-clicked Save menu item");
        finish();
        cancelCurrentChoosersOnEdt();
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
          this.evidence.normalizedSelectedFile =
              selectedFile == null ? null : selectedFile.getCanonicalPath();
          this.evidence.selectedFileVerified =
              this.targetFile.getCanonicalPath().equals(this.evidence.normalizedSelectedFile);
          this.evidence.targetInsideProofRoot =
              this.evidence.proofContainsPath(Path.of(this.targetFile.getCanonicalPath()));
          boolean hasExpectedExtension = this.targetFile.getName().endsWith(".a3p");
          if (this.evidence.selectedFileVerified && this.evidence.targetInsideProofRoot && hasExpectedExtension) {
            chooser.approveSelection();
            this.evidence.approvedSelection = true;
          } else {
            this.evidence.block("target_path_rejected",
                "The selected Save path was outside the proof root or did not end with .a3p",
                "A test-owned .a3p path below target/save-menu-proofs/projects");
            chooser.cancelSelection();
          }
        } catch (java.io.IOException ex) {
          this.evidence.block("chooser_control_failed",
              "The Save chooser selected file could not be canonicalized safely",
              "A canonical target path that can be compared against the proof root");
          chooser.cancelSelection();
        }
      });
    }

    private void finish() {
      this.finished.set(true);
      cancelTimer();
    }

    private void cancelTimer() {
      java.util.Timer current = this.timer;
      if (current != null) {
        current.cancel();
        this.timer = null;
      }
    }
  }

  private static class RobotSaveProofEvidence {
    private final File targetFile;
    private final Path proofRoot;
    private final String targetCanonicalPath;

    private volatile boolean robotFileMenuOpened;
    private volatile boolean robotSaveItemClicked;
    private volatile boolean saveActionIdentityMatched;
    private volatile boolean chooserObserved;
    private volatile boolean approvedSelection;
    private volatile boolean ambiguousChooserDiscovery;
    private volatile boolean selectedFileVerified;
    private volatile boolean targetInsideProofRoot;
    private volatile boolean dialogShowing;
    private volatile String dialogClass;
    private volatile String normalizedSelectedFile;
    private volatile int pollCount;
    private volatile boolean projectReadable;
    private volatile boolean markerPresent;
    private volatile String blockerKind;
    private volatile String blockerObserved;
    private volatile String blockerRequired;

    RobotSaveProofEvidence(File targetFile, Path proofRoot) throws java.io.IOException {
      this.targetFile = targetFile;
      this.proofRoot = proofRoot.toRealPath();
      this.targetCanonicalPath = targetFile.getCanonicalPath();
    }

    void block(String kind, String observed, String required) {
      if (this.blockerKind == null) {
        this.blockerKind = kind;
        this.blockerObserved = observed;
        this.blockerRequired = required;
      }
    }

    void recordReadback(boolean projectReadable, boolean markerPresent) {
      this.projectReadable = projectReadable;
      this.markerPresent = markerPresent;
    }

    void write(Path evidenceDir) throws Exception {
      Path targetPath = Path.of(this.targetCanonicalPath).normalize();
      Path selectedPath = this.normalizedSelectedFile == null
          ? null
          : Path.of(this.normalizedSelectedFile).normalize();
      boolean fileExists = this.targetFile.isFile();
      long fileSizeBytes = fileExists ? this.targetFile.length() : 0;
      boolean fileNonempty = fileSizeBytes > 0;
      boolean fileHasExpectedExtension = this.targetFile.getName().endsWith(".a3p");
      boolean selectedFileMatchesExpected =
          this.normalizedSelectedFile != null && this.targetCanonicalPath.equals(this.normalizedSelectedFile);
      boolean observedWrite = fileExists && fileNonempty && fileHasExpectedExtension && this.targetInsideProofRoot;
      boolean proven = this.robotFileMenuOpened
          && this.robotSaveItemClicked
          && this.saveActionIdentityMatched
          && this.chooserObserved
          && this.approvedSelection
          && !this.ambiguousChooserDiscovery
          && this.selectedFileVerified
          && selectedFileMatchesExpected
          && observedWrite
          && this.projectReadable
          && this.markerPresent
          && this.blockerKind == null;
      if (!proven && this.blockerKind == null) {
        block(inferBlockerKind(observedWrite),
            inferBlockerObserved(observedWrite),
            "A complete Robot File menu Save activation, dialog approval, write, readback, and marker path");
      }
      boolean claimApprovedSelection = proven && this.approvedSelection;
      boolean claimFileWritten = proven && fileExists;
      boolean claimFileNonempty = proven && fileNonempty;
      boolean claimProjectReadable = proven && this.projectReadable;
      boolean claimMarkerPresent = proven && this.markerPresent;
      String status = proven ? "proven" : "blocked";
      String claimOrSummary = proven
          ? "  \"claim\": \"AWT Robot opened File, clicked the production Save menu item, controlled the Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker\",\n"
          : "  \"reporting_summary\": \"Robot File menu Save dialog/write/readback path was not proven; see blocker.kind for the exact missing or unsafe precondition\",\n";
      Files.createDirectories(evidenceDir);
      Files.writeString(
          evidenceDir.resolve(ARTIFACT),
          "{\n"
              + "  \"schema_version\": \"" + SCHEMA_VERSION + "\",\n"
              + "  \"status\": \"" + status + "\",\n"
              + "  \"proofTarget\": \"Robot File menu Save activation joined to dialog/write/readback evidence\",\n"
              + claimOrSummary
              + blockerJson(proven)
              + "  \"trigger\": {\n"
              + "    \"robot_file_menu_opened\": " + this.robotFileMenuOpened + ",\n"
              + "    \"robot_save_item_clicked\": " + this.robotSaveItemClicked + ",\n"
              + "    \"save_action_identity_matched\": " + this.saveActionIdentityMatched + "\n"
              + "  },\n"
              + "  \"observed_dialog\": {\n"
              + "    \"dialogType\": \"Swing JFileChooser\",\n"
              + "    \"dialog_class\": " + stringJson(this.dialogClass) + ",\n"
              + "    \"dialog_showing\": " + this.dialogShowing + ",\n"
              + "    \"chooser_observed\": " + this.chooserObserved + ",\n"
              + "    \"approved_selection\": " + claimApprovedSelection + ",\n"
              + "    \"ambiguous_chooser_discovery\": " + this.ambiguousChooserDiscovery + ",\n"
              + "    \"poll_count\": " + this.pollCount + "\n"
              + "  },\n"
              + "  \"selected_file\": {\n"
              + "    \"normalized_selected_file\": " + stringJson(proofRelativePath(selectedPath)) + ",\n"
              + "    \"expected_file\": " + stringJson(proofRelativePath(targetPath)) + ",\n"
              + "    \"selected_file_verified\": " + this.selectedFileVerified + ",\n"
              + "    \"selected_file_matches_expected\": " + selectedFileMatchesExpected + ",\n"
              + "    \"target_inside_proof_root\": " + this.targetInsideProofRoot + "\n"
              + "  },\n"
              + "  \"written_artifact\": {\n"
              + "    \"target_file\": " + stringJson(proofRelativePath(targetPath)) + ",\n"
              + "    \"file_written\": " + claimFileWritten + ",\n"
              + "    \"file_nonempty\": " + claimFileNonempty + ",\n"
              + "    \"file_extension\": \"a3p\",\n"
              + "    \"file_has_expected_extension\": " + fileHasExpectedExtension + ",\n"
              + "    \"file_size_bytes\": " + fileSizeBytes + "\n"
              + "  },\n"
              + "  \"readback\": {\n"
              + "    \"project_readable\": " + claimProjectReadable + ",\n"
              + "    \"expected_marker\": \"" + READBACK_MARKER + "\",\n"
              + "    \"marker_present\": " + claimMarkerPresent + "\n"
              + "  },\n"
              + "  \"baselinePreserved\": [\n"
              + "    \"StageIdeSaveMenuDoClickToWriteProofTest\",\n"
              + "    \"ProjectApplicationSaveProjectToTest\",\n"
              + "    \"JMenuBarRobotClickSaveProofTest\"\n"
              + "  ],\n"
              + "  \"requiresNextEvidence\": [\n"
              + "    \"Run under xvfb-run -a or an equivalent desktop session when blocker.kind is environment-related\",\n"
              + "    \"Use status proven only when Robot menu activation, dialog control, write, readback, and marker verification all succeed\"\n"
              + "  ],\n"
              + "  \"doesNotClaim\": [\n"
              + "    \"full desktop Save completion\",\n"
              + "    \"full lesson completion\",\n"
              + "    \"visible rendering correctness\",\n"
              + "    \"grading correctness\",\n"
              + "    \"physical user click\",\n"
              + "    \"broad UI automation coverage\",\n"
              + "    \"native dialog coverage\",\n"
              + "    \"all Save variants\",\n"
              + "    \"Save As coverage\"\n"
              + "  ]\n"
              + "}\n",
          StandardCharsets.UTF_8);
    }

    boolean proofContainsPath(Path path) {
      return path != null && path.normalize().startsWith(this.proofRoot);
    }

    private String inferBlockerKind(boolean observedWrite) {
      if (!this.robotFileMenuOpened) {
        return "file_menu_not_showing";
      }
      if (!this.robotSaveItemClicked || !this.saveActionIdentityMatched) {
        return "save_item_not_attributed";
      }
      if (!this.chooserObserved) {
        return "dialog_not_observed";
      }
      if (this.ambiguousChooserDiscovery) {
        return "ambiguous_chooser_discovery";
      }
      if (!this.selectedFileVerified || !this.approvedSelection) {
        return "chooser_control_failed";
      }
      if (!this.targetInsideProofRoot || !this.targetFile.getName().endsWith(".a3p")) {
        return "target_path_rejected";
      }
      if (!observedWrite) {
        return "write_not_observed";
      }
      if (!this.projectReadable) {
        return "readback_failed";
      }
      return "marker_missing";
    }

    private String inferBlockerObserved(boolean observedWrite) {
      if (!this.robotFileMenuOpened) {
        return "The rendered File menu was not opened by Robot";
      }
      if (!this.robotSaveItemClicked || !this.saveActionIdentityMatched) {
        return "The production Save item click was not attributed to Robot";
      }
      if (!this.chooserObserved) {
        return "No live Swing JFileChooser was observed";
      }
      if (this.ambiguousChooserDiscovery) {
        return "Multiple live Swing JFileChoosers were observed";
      }
      if (!this.selectedFileVerified || !this.approvedSelection) {
        return "The live Save chooser could not be safely controlled";
      }
      if (!this.targetInsideProofRoot || !this.targetFile.getName().endsWith(".a3p")) {
        return "The selected Save target was outside the proof root or not an .a3p file";
      }
      if (!observedWrite) {
        return "No non-empty .a3p write was observed at the controlled target";
      }
      if (!this.projectReadable) {
        return "The written .a3p file could not be read back as an Alice project";
      }
      return "The readback project did not contain " + READBACK_MARKER;
    }

    private String blockerJson(boolean proven) {
      if (proven) {
        return "";
      }
      return "  \"blocker\": {\n"
          + "    \"kind\": \"" + escape(this.blockerKind) + "\",\n"
          + "    \"observed\": \"" + escape(this.blockerObserved) + "\",\n"
          + "    \"required\": \"" + escape(this.blockerRequired) + "\"\n"
          + "  },\n";
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
  }

  private static List<ChooserCandidate> findChooserCandidates(int maxCandidates) {
    List<ChooserCandidate> candidates = new ArrayList<>(maxCandidates);
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

  private static void cancelCurrentChoosersOnEdt() {
    SwingUtilities.invokeLater(() -> {
      for (ChooserCandidate candidate : findChooserCandidates(Integer.MAX_VALUE)) {
        candidate.chooser().cancelSelection();
      }
    });
  }

  private static boolean isNonHeadlessAwtDisplayAvailable() {
    if (GraphicsEnvironment.isHeadless()) {
      return false;
    }
    java.awt.Frame frame = null;
    try {
      frame = new java.awt.Frame();
      frame.pack();
      return true;
    } catch (java.awt.HeadlessException | AWTError e) {
      return false;
    } finally {
      if (frame != null) {
        frame.dispose();
      }
    }
  }

  private static void cleanupRobotProofResources(
      AtomicReference<JFrame> menuFrameRef,
      AtomicReference<StageIDE> ideRef) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JFrame menuFrame = menuFrameRef.getAndSet(null);
      if (menuFrame != null) {
        menuFrame.dispose();
      }
      StageIDE ide = ideRef.getAndSet(null);
      if (ide != null
          && ide.getDocumentFrame() != null
          && ide.getDocumentFrame().getFrame() != null) {
        ide.getDocumentFrame().getFrame().release();
      }
    });
  }

  private static FileMenuModel findFileMenuModel(StageIDE ide) {
    for (StandardMenuItemPrepModel child :
        ide.getDocumentFrame().getCodePerspective().getMenuBarComposite().getChildren()) {
      if (child instanceof FileMenuModel) {
        return (FileMenuModel) child;
      }
    }
    return null;
  }

  private static void drainEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
    });
    SwingUtilities.invokeAndWait(() -> {
    });
  }

  private static void disableThumbnailCameraRender() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        Field sceneCameraImpField = StorytellingSceneEditor.class.getDeclaredField("sceneCameraImp");
        sceneCameraImpField.setAccessible(true);
        sceneCameraImpField.set(StorytellingSceneEditor.getInstance(), null);
      } catch (Exception e) {
        throw new RuntimeException("failed to null sceneCameraImp on StorytellingSceneEditor", e);
      }
    });
  }

  private static void useDistributionDirectoryIfAvailable() {
    Path distributionDir = Paths.get(System.getProperty("user.dir"))
        .resolve("../../core/resources/target/distribution").normalize();
    if (distributionDir.toFile().isDirectory()) {
      System.setProperty("org.alice.ide.rootDirectory", distributionDir.toString());
    }
  }

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

  private static Project minimalProject() {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    sceneType.methods.add(new UserMethod(
        READBACK_MARKER,
        JavaType.VOID_TYPE,
        new UserParameter[0],
        new BlockStatement(new Comment("Robot Save menu round-trip marker"))));
    NamedUserType programType = AstUtilities.createType("Program", JavaType.getInstance(SProgram.class));
    programType.fields.add(new UserField("myScene", sceneType));
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  private static boolean projectContainsMarker(Project project, String marker) {
    return project != null && project.getNamedUserTypes().stream()
        .anyMatch(type -> type.getDeclaredMethod(marker) != null);
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

  private void restoreLicensePreference() {
    if (previousLicenseAccepted == null) {
      licensePreferences.remove("isLicenseAccepted");
    } else {
      licensePreferences.put("isLicenseAccepted", previousLicenseAccepted);
    }
  }

  private static void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  private static Path canonicalProofRoot() throws Exception {
    return Files.createDirectories(Path.of("target", "save-menu-proofs")).toRealPath();
  }

  private static String stringJson(String value) {
    return value == null ? "null" : "\"" + escape(value) + "\"";
  }

  private static String escape(String value) {
    return value == null ? "" : FileDialogUtilities.escapeJson(value);
  }

  private record ChooserCandidate(JDialog dialog, JFileChooser chooser) {
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
}
