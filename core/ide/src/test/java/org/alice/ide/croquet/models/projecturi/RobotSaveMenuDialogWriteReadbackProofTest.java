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
import javax.swing.Timer;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Joins the next Save seam: Robot File-menu activation, live Save dialog control,
 * .a3p write, IoUtilities readback, and marker verification.
 * Canonical evidence status is either "blocked" or "proven".
 */
public class RobotSaveMenuDialogWriteReadbackProofTest {
  private static final String ARTIFACT = "robot-save-menu-dialog-write-readback-proof.json";
  private static final String SCHEMA_VERSION =
      SaveOperationCompletionEvidence.SAVE_PROOF_SCHEMA_VERSION;
  private static final String READBACK_MARKER = SaveOperationCompletionEvidence.SAVE_PROOF_MARKER;
  private static final String TARGET_FILE_NAME = "robot-save-menu-proof.a3p";

  private String previousDiscoveryEvidenceDir;
  private String previousSelectedPath;
  private String previousSaveEvidenceDir;
  private String previousProofOnly;
  private String previousSaveProofScenario;
  private String previousSaveProofRunId;
  private String previousSaveProofEvidencePath;
  private Preferences licensePreferences;
  private String previousLicenseAccepted;

  @Before
  public void captureProperties() {
    previousDiscoveryEvidenceDir =
        System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    previousSaveEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    previousProofOnly = System.getProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY);
    previousSaveProofScenario = System.getProperty(SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_PROPERTY);
    previousSaveProofRunId = System.getProperty(SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_PROPERTY);
    previousSaveProofEvidencePath = System.getProperty(SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY);
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
    restoreProperty(SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_PROPERTY, previousSaveProofScenario);
    restoreProperty(SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_PROPERTY, previousSaveProofRunId);
    restoreProperty(SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY, previousSaveProofEvidencePath);
    resetActiveApplication();
  }

  @Test
  public void robotFileSaveApprovesChooserWritesReadableMarkedProjectOrWritesBlocker()
      throws Exception {
    Path proofRoot = canonicalProofRoot();
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    Path artifact = SaveOperationCompletionEvidence.configuredSaveProofArtifact(proofRoot);
    Files.deleteIfExists(artifact);
    File targetFile = projectsDir.resolve(TARGET_FILE_NAME).toAbsolutePath().toFile();
    Files.deleteIfExists(targetFile.toPath());

    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(targetFile, proofRoot);
    if (!isNonHeadlessAwtDisplayAvailable()) {
      blockAndFail(evidence, artifact, "headless_awt",
          "No available non-headless AWT display",
          "Xvfb or another non-headless AWT display capable of Robot mouse events and Swing JFileChooser display");
      return;
    }

    configureSaveProofRun(proofRoot);

    AtomicReference<StageIDE> ideRef = new AtomicReference<>();
    AtomicReference<JFrame> menuFrameRef = new AtomicReference<>();
    try {
      runRenderedSaveProofPath(artifact, targetFile, evidence, ideRef, menuFrameRef);
    } finally {
      cleanupRobotProofResources(menuFrameRef, ideRef);
    }
  }

  @Test
  public void incompleteArtifactIsBlockedAndDoesNotClaimChooserWriteOrReadback() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    File targetFile = proofRoot.resolve("projects").resolve(TARGET_FILE_NAME).toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(targetFile, proofRoot);

    evidence.write(proofRoot.resolve(ARTIFACT));

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"file_menu_not_showing\""));
    assertTrue(json, json.contains("\"fileMenuOpened\": false"));
    assertTrue(json, json.contains("\"saveMenuItemInvoked\": false"));
    assertTrue(json, json.contains("\"approvedSelection\": false"));
    assertTrue(json, json.contains("\"fileWritten\": false"));
    assertTrue(json, json.contains("\"projectReadable\": false"));
    assertTrue(json, json.contains("\"markerPresent\": false"));
    assertTrue(json, json.contains("\"requiresNextEvidence\""));
    assertFalse(json, json.contains("\"claim\""));
  }

  @Test
  public void artifactRequiresRobotSaveClickBeforeReportingProven() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    File targetFile = Files.writeString(projectsDir.resolve(TARGET_FILE_NAME), "not an Alice project").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(targetFile, proofRoot);
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

    evidence.write(proofRoot.resolve(ARTIFACT));

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"save_item_not_attributed\""));
    assertTrue(json, json.contains("\"fileMenuOpened\": true"));
    assertTrue(json, json.contains("\"saveMenuItemInvoked\": false"));
    assertTrue(json, json.contains("\"fileWritten\": false"));
    assertTrue(json, json.contains("\"projectReadable\": false"));
    assertTrue(json, json.contains("\"markerPresent\": false"));
    assertFalse(json, json.contains("\"status\": \"proven\""));
    assertFalse(json, json.contains("\"claim\""));
  }

  @Test
  public void completeArtifactReportsNarrowProvenClaim() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    File targetFile = Files.writeString(projectsDir.resolve(TARGET_FILE_NAME), "non-empty proof file").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(targetFile, proofRoot);
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

    evidence.write(proofRoot.resolve(ARTIFACT));

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"proven\""));
    assertTrue(json, json.contains("\"claim\": \"AWT Robot opened File, clicked the production Save menu item, controlled the rendered Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker\""));
    assertTrue(json, json.contains("\"normalizedSelectedPath\": \"projects/" + TARGET_FILE_NAME + "\""));
    assertTrue(json, json.contains("\"fileWritten\": true"));
    assertTrue(json, json.contains("\"fileNonempty\": true"));
    assertTrue(json, json.contains("\"projectReadable\": true"));
    assertTrue(json, json.contains("\"markerPresent\": true"));
    assertFalse(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"blocker\": null"));
  }

  @Test
  public void targetOutsideProofRootIsBlockedAndRedacted() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path outsideRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-outside-test", UUID.randomUUID().toString()));
    File outsideTarget = Files.writeString(outsideRoot.resolve(TARGET_FILE_NAME), "outside").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(outsideTarget, proofRoot);
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

    evidence.write(proofRoot.resolve(ARTIFACT));

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"target_path_rejected\""));
    assertTrue(json, json.contains("\"normalizedSelectedPath\": \"[outside-proof-root]\""));
    assertFalse(json, json.contains(FileDialogUtilities.escapeJson(outsideTarget.getCanonicalPath())));
    assertFalse(json, json.contains("\"claim\""));
  }

  @Test
  public void wrongSelectedFileDoesNotRewriteExpectedTargetBoundaryEvidence() throws Exception {
    Path proofRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-contract-test", UUID.randomUUID().toString()));
    Path projectsDir = Files.createDirectories(proofRoot.resolve("projects"));
    Path outsideRoot = Files.createDirectories(Path.of(
        "target", "robot-save-menu-proof-wrong-selection-test", UUID.randomUUID().toString()));
    File targetFile = projectsDir.resolve(TARGET_FILE_NAME).toFile();
    File outsideSelection = Files.writeString(outsideRoot.resolve(TARGET_FILE_NAME), "outside").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(targetFile, proofRoot);

    evidence.robotFileMenuOpened = true;
    evidence.robotSaveItemClicked = true;
    evidence.saveActionIdentityMatched = true;
    evidence.chooserObserved = true;
    evidence.dialogShowing = true;
    evidence.dialogClass = JDialog.class.getName();
    evidence.recordSelectedFile(outsideSelection);

    evidence.write(proofRoot.resolve(ARTIFACT));

    String json = Files.readString(proofRoot.resolve(ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"chooser_control_failed\""));
    assertTrue(json, json.contains("\"selectedPathMatchesExpected\": false"));
    assertTrue(json, json.contains("\"targetInsideProofRoot\": true"));
    assertTrue(json, json.contains("\"normalizedSelectedPath\": \"[outside-proof-root]\""));
    assertTrue(json, json.contains("\"expectedPath\": \"projects/" + TARGET_FILE_NAME + "\""));
    assertFalse(json, json.contains(FileDialogUtilities.escapeJson(outsideSelection.getCanonicalPath())));
    assertFalse(json, json.contains("\"claim\""));
  }

  private void configureSaveProofRun(Path proofRoot) throws Exception {
    System.clearProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, proofRoot.toString());
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, proofRoot.toString());
    System.clearProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY);
    useDistributionDirectoryIfAvailable();
    resetActiveApplication();
  }

  private void runRenderedSaveProofPath(
      Path artifact,
      File targetFile,
      SaveOperationCompletionEvidence.SaveProofEvidence evidence,
      AtomicReference<StageIDE> ideRef,
      AtomicReference<JFrame> menuFrameRef) throws Exception {
    AtomicReference<JMenu> fileMenuRef = new AtomicReference<>();
    initializeRenderedSaveMenuOnEdt(ideRef, menuFrameRef, fileMenuRef);
    drainEdt();
    disableThumbnailCameraRender();

    Robot robot = robotOrBlock(evidence, artifact);
    if (!robotOpenFileMenu(robot, fileMenuRef.get())) {
      blockAndFail(evidence, artifact, "file_menu_not_showing",
          "Robot could not open the rendered File menu popup",
          "A visible Croquet File menu that accepts Robot mouse events");
      return;
    }
    evidence.robotFileMenuOpened = true;

    AtomicReference<Point> saveItemCenter = saveItemCenterOnEdt(fileMenuRef.get(), evidence);
    if (saveItemCenter.get() == null) {
      blockAndFail(evidence, artifact, "save_item_not_attributed",
          "The rendered File popup did not expose the Save item by SaveProjectOperation action identity",
          "A Save menu item whose Swing Action is SaveProjectOperation");
      return;
    }

    clickSaveAndWaitForWrite(robot, saveItemCenter.get(), targetFile, evidence);
    recordReadbackIfWritten(targetFile, evidence);
    evidence.write(artifact);
    assertRenderedSaveProofArtifact(artifact, targetFile);
  }

  private static Robot robotOrBlock(
      SaveOperationCompletionEvidence.SaveProofEvidence evidence,
      Path artifact) throws Exception {
    try {
      return createProofRobot();
    } catch (AWTException | SecurityException ex) {
      blockAndFail(evidence, artifact, "robot_unavailable",
          "java.awt.Robot could not be created for this display",
          "A desktop session that permits java.awt.Robot mouse events");
      throw ex;
    }
  }

  private void initializeRenderedSaveMenuOnEdt(
      AtomicReference<StageIDE> ideRef,
      AtomicReference<JFrame> menuFrameRef,
      AtomicReference<JMenu> fileMenuRef) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      StageIDE ide = new StageIDE(new CrashDetector(RobotSaveMenuDialogWriteReadbackProofTest.class));
      licensePreferences.putBoolean("isLicenseAccepted", true);
      ide.initialize(new String[0]);
      installMinimalProject(ide);
      showIdeFrame(ide);
      ideRef.set(ide);

      JFrame menuFrame = createRenderedFileMenuFrame(ide);
      menuFrameRef.set(menuFrame);
      fileMenuRef.set(findRenderedFileMenu(menuFrame));
      assertNotNull("File JMenu not found in rendered MenuBar", fileMenuRef.get());
    });
  }

  private static void installMinimalProject(StageIDE ide) {
    try {
      ProjectDocumentState.getInstance().setValueTransactionlessly(
          new ProjectDocument(minimalProject(), new UserActivity()));
      injectUriProjectLoader(ide, new NewProjectLoader());
    } catch (Exception e) {
      throw new RuntimeException("project state injection failed", e);
    }
  }

  private static void showIdeFrame(StageIDE ide) {
    ide.getDocumentFrame().getFrame().pack();
    ide.getDocumentFrame().getFrame().setLocation(520, 80);
    ide.getDocumentFrame().getFrame().setVisible(true);
    assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isDisplayable());
    assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isShowing());
  }

  private static JFrame createRenderedFileMenuFrame(StageIDE ide) {
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
    return menuFrame;
  }

  private static JMenu findRenderedFileMenu(JFrame menuFrame) {
    JMenuBar jMenuBar = menuFrame.getJMenuBar();
    assertNotNull("JMenuBar not set on Croquet Frame", jMenuBar);
    for (int i = 0; i < jMenuBar.getMenuCount(); i++) {
      JMenu menu = jMenuBar.getMenu(i);
      if (menu != null && "File".equals(menu.getText())) {
        return menu;
      }
    }
    return null;
  }

  private static Robot createProofRobot() throws AWTException {
    Robot robot = new Robot();
    robot.setAutoDelay(40);
    robot.waitForIdle();
    robot.delay(250);
    return robot;
  }

  private static AtomicReference<Point> saveItemCenterOnEdt(
      JMenu fileMenu,
      SaveOperationCompletionEvidence.SaveProofEvidence evidence) throws Exception {
    AtomicReference<Point> saveItemCenter = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
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
    return saveItemCenter;
  }

  private static void clickSaveAndWaitForWrite(
      Robot robot,
      Point saveItemCenter,
      File targetFile,
      SaveOperationCompletionEvidence.SaveProofEvidence evidence) throws Exception {
    RobotSaveDialogController controller = new RobotSaveDialogController(targetFile, evidence);
    controller.start();
    try {
      robot.mouseMove(saveItemCenter.x, saveItemCenter.y);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      evidence.robotSaveItemClicked = true;
      robot.waitForIdle();
      waitForFileWrite(targetFile.toPath());
    } finally {
      controller.stop();
    }
  }

  private static void recordReadbackIfWritten(
      File targetFile,
      SaveOperationCompletionEvidence.SaveProofEvidence evidence) {
    if (!targetFile.isFile() || targetFile.length() <= 0) {
      return;
    }
    try {
      Project savedProject = IoUtilities.readProject(targetFile);
      evidence.recordReadback(savedProject != null, projectContainsMarker(savedProject, READBACK_MARKER));
    } catch (Exception ex) {
      evidence.block("readback_failed",
          "The written .a3p file could not be read back through IoUtilities.readProject",
          "A readable Alice project archive with the expected marker");
    }
  }

  private static void assertRenderedSaveProofArtifact(Path artifact, File targetFile) throws Exception {
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schemaVersion\": \"" + SCHEMA_VERSION + "\""));
    assertTrue(json, json.contains("\"proofTarget\": \"single rendered desktop Save path: menu, dialog, control, write, readback\""));
    assertTrue(json, json.contains("\"baselinePreserved\""));
    assertTrue(json, json.contains("\"StageIdeSaveMenuDoClickToWriteProofTest\""));
    assertTrue(json, json.contains("\"ProjectApplicationSaveProjectToTest\""));
    assertTrue(json, json.contains("\"JMenuBarRobotClickSaveProofTest\""));
    if (json.contains("\"status\": \"proven\"")) {
      assertProvenRenderedSaveProof(json);
    } else {
      assertBlockedRenderedSaveProof(json, artifact);
    }
    assertFalse(json, json.contains(FileDialogUtilities.escapeJson(targetFile.getCanonicalPath())));
    assertTrue(json, json.contains("\"all Save variants\""));
    assertTrue(json, json.contains("\"Save As coverage\""));
  }

  private static void assertProvenRenderedSaveProof(String json) {
    assertTrue(json, json.contains("\"claim\": \"AWT Robot opened File, clicked the production Save menu item, controlled the rendered Swing Save chooser, wrote a non-empty .a3p file, read it back, and verified robotSaveMenuRoundTripMarker\""));
    assertTrue(json, json.contains("\"fileMenuOpened\": true"));
    assertTrue(json, json.contains("\"saveMenuItemInvoked\": true"));
    assertTrue(json, json.contains("\"saveActionIdentityMatched\": true"));
    assertTrue(json, json.contains("\"saveDialogObserved\": true"));
    assertTrue(json, json.contains("\"approvedSelection\": true"));
    assertTrue(json, json.contains("\"fileWritten\": true"));
    assertTrue(json, json.contains("\"fileNonempty\": true"));
    assertTrue(json, json.contains("\"projectReadable\": true"));
    assertTrue(json, json.contains("\"marker\": \"" + READBACK_MARKER + "\""));
    assertTrue(json, json.contains("\"markerPresent\": true"));
    assertFalse(json, json.contains("\"status\": \"blocked\""));
  }

  private static void assertBlockedRenderedSaveProof(String json, Path artifact) {
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"blocker\""));
    assertTrue(json, json.contains("\"requiresNextEvidence\""));
    assertFalse(json, json.contains("\"claim\""));
    fail("Robot Save proof blocked; see " + artifact);
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

  private static void blockAndFail(
      SaveOperationCompletionEvidence.SaveProofEvidence evidence,
      Path artifact,
      String blockerKind,
      String observed,
      String required) throws Exception {
    evidence.block(blockerKind, observed, required);
    evidence.write(artifact);
    assertBlockedArtifact(artifact, blockerKind);
    String message = "Robot Save proof blocked at " + blockerKind + "; see " + artifact;
    if (isExplicitSaveProofRun()) {
      fail(message);
    }
    assumeTrue(message, false);
  }

  private static boolean isExplicitSaveProofRun() {
    return isConfigured(SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_PROPERTY,
        SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_ENV)
        || isConfigured(SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_PROPERTY,
            SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_ENV)
        || isConfigured(SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY,
            SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_ENV);
  }

  private static boolean isConfigured(String propertyName, String envName) {
    String propertyValue = System.getProperty(propertyName);
    if (propertyValue != null && !propertyValue.isBlank()) {
      return true;
    }
    String envValue = System.getenv(envName);
    return envValue != null && !envValue.isBlank();
  }

  private static class RobotSaveDialogController {
    private static final int MAX_POLLS = 500;
    private static final int MAX_CHOOSER_CANDIDATES = 2;
    private static final int POLL_INTERVAL_MILLIS = 100;

    private final File targetFile;
    private final SaveOperationCompletionEvidence.SaveProofEvidence evidence;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean approvalScheduled = new AtomicBoolean(false);
    private final AtomicBoolean approvalApplied = new AtomicBoolean(false);
    private final AtomicInteger pollCount = new AtomicInteger();
    private volatile Timer timer;

    RobotSaveDialogController(File targetFile, SaveOperationCompletionEvidence.SaveProofEvidence evidence) {
      this.targetFile = targetFile;
      this.evidence = evidence;
    }

    void start() {
      SwingUtilities.invokeLater(() -> {
        if (this.finished.get() || this.timer != null) {
          return;
        }
        Timer current = new Timer(POLL_INTERVAL_MILLIS, event -> pollOnEdt());
        current.setInitialDelay(0);
        this.timer = current;
        current.start();
      });
    }

    void stop() {
      finish();
    }

    private void pollOnEdt() {
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
      Runnable approve = () -> {
        if (!this.approvalApplied.compareAndSet(false, true)) {
          return;
        }
        try {
          chooser.setSelectedFile(this.targetFile);
          if (this.evidence.recordSelectedFile(chooser.getSelectedFile())) {
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
      };
      if (SwingUtilities.isEventDispatchThread()) {
        approve.run();
      } else {
        SwingUtilities.invokeLater(approve);
      }
    }

    private void finish() {
      this.finished.set(true);
      cancelTimer();
    }

    private void cancelTimer() {
      Runnable cancel = () -> {
        Timer current = this.timer;
        if (current != null) {
          current.stop();
          this.timer = null;
        }
      };
      if (SwingUtilities.isEventDispatchThread()) {
        cancel.run();
      } else {
        SwingUtilities.invokeLater(cancel);
      }
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
    Runnable cancel = () -> {
      for (ChooserCandidate candidate : findChooserCandidates(Integer.MAX_VALUE)) {
        candidate.chooser().cancelSelection();
      }
    };
    if (SwingUtilities.isEventDispatchThread()) {
      cancel.run();
    } else {
      SwingUtilities.invokeLater(cancel);
    }
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
    String configuredEvidencePath = System.getProperty(SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY);
    if (configuredEvidencePath == null || configuredEvidencePath.isBlank()) {
      configuredEvidencePath = System.getenv(SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_ENV);
    }
    if (configuredEvidencePath != null && !configuredEvidencePath.isBlank()) {
      Path configuredParent = Path.of(configuredEvidencePath).toAbsolutePath().normalize().getParent();
      if (configuredParent == null) {
        throw new IllegalArgumentException("Save proof evidence path must have a parent directory");
      }
      return Files.createDirectories(configuredParent).toRealPath();
    }
    return Files.createDirectories(Path.of("target", "save-menu-proofs")).toRealPath();
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
