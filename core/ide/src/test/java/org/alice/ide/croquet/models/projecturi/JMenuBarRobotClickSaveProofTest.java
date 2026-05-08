package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import org.alice.ide.croquet.models.menubar.FileMenuModel;
import org.alice.stageide.StageIDE;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.croquet.StandardMenuItemPrepModel;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Proves that AWT Robot mouse events can open a visible File JMenu rendered from
 * {@code FileMenuModel.createMenu()} and click Save, dispatching
 * {@code SaveProjectOperation} through the full Swing ActionEvent path.
 *
 * <p>Increment after PR #292 ({@link FileMenuSaveNavigationProofTest}), which proved
 * programmatic {@code doClick()} on the Save JMenuItem. This test replaces the
 * programmatic {@code doClick()} with real AWT Robot mouse-press/release events on the
 * rendered on-screen JMenu button and JMenuItem in an open popup.
 *
 * <p>The File JMenu is the same object built from the real {@code FileMenuModel} (same as
 * PR #292). It is placed in a minimal test JFrame to avoid the StageIDE
 * perspective/WindowOpened lifecycle that would replace the JMenuBar after
 * {@code setVisible(true)}.
 *
 * <p>What this test proves:
 * <ol>
 *   <li>A {@code JMenu} built from the real {@code FileMenuModel} renders on screen
 *       inside a visible JMenuBar.
 *   <li>AWT Robot {@code mousePress + mouseRelease} at the JMenu button's screen center
 *       opens the popup ({@code JMenu.isPopupMenuVisible()} becomes true).
 *   <li>The open popup contains a {@code JMenuItem} connected to
 *       {@code SaveProjectOperation}'s Swing action by identity.
 *   <li>AWT Robot {@code mousePress + mouseRelease} at that JMenuItem's screen center
 *       dispatches into {@code AbstractSaveOperation.perform()} through the Swing
 *       ActionEvent path — not a programmatic shortcut.
 *   <li>Evidence artifact reports {@code "status": "menu_item_dispatched"} and
 *       {@code "menu_item_dispatch": true}.
 * </ol>
 *
 * <p>Still unproven after this test: full save-to-disk in one Robot-driven path with
 * live FileDialog/JFileChooser interaction and a background dialog-control thread.
 * That requires Robot navigation of the StageIDE JMenuBar with the JFileChooser controlled
 * from a background thread while the EDT blocks in {@code JFileChooser.showSaveDialog()}.
 */
public class JMenuBarRobotClickSaveProofTest {

  private String previousEvidenceDir;
  private String previousProofOnly;

  @Before
  public void captureProperties() {
    previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    previousProofOnly = System.getProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY);
  }

  @After
  public void restorePropertiesAndActiveApplication() throws Exception {
    restoreProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
    restoreProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, previousProofOnly);
    resetActiveApplication();
  }

  @Test
  public void robotClickFileMenuInVisibleJMenuBarSelectsSaveDispatchesToSaveOperation()
      throws Exception {
    assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());

    Path evidenceDir = newTestDir();
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, "true");

    resetActiveApplication();

    // Step 1: On EDT — init StageIDE (for active-instance + localization), build the File
    // JMenu through a proper Croquet MenuBar so that AwtComponentView.lookup() resolves
    // correctly when the Croquet MenuSelectionManager listener fires on Robot clicks.
    AtomicReference<StageIDE> ideRef = new AtomicReference<>();
    AtomicReference<JFrame> testFrameRef = new AtomicReference<>();
    AtomicReference<JMenu> fileMenuRef = new AtomicReference<>();

    try {
    SwingUtilities.invokeAndWait(() -> {
      StageIDE ide = new StageIDE(new CrashDetector(JMenuBarRobotClickSaveProofTest.class));
      ide.initialize(new String[0]);
      ideRef.set(ide);

      FileMenuModel fileMenuModel = findFileMenuModel(ide);
      assertNotNull("FileMenuModel not found in AliceMenuBar children", fileMenuModel);

      // Wrap FileMenuModel in a fresh MenuBarComposite so that the resulting JMenuBar is
      // a Croquet MenuBar view registered in AwtComponentView.lookup() — preventing the
      // ClassCastException in MenuSelection.getMenuBarOrigin() that fires on Robot clicks.
      org.lgna.croquet.MenuBarComposite testMenuBarComposite =
          new org.lgna.croquet.MenuBarComposite(UUID.randomUUID());
      testMenuBarComposite.addItem(fileMenuModel);

      // Use a Croquet Frame so setMenuBarComposite() properly wires view registration.
      org.lgna.croquet.views.Frame croquetFrame = new org.lgna.croquet.views.Frame();
      croquetFrame.setMenuBarComposite(testMenuBarComposite);

      JFrame jFrame = croquetFrame.getAwtComponent();
      jFrame.setTitle("Save Robot Click Proof");
      jFrame.setSize(400, 200);
      jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      jFrame.pack();
      jFrame.setVisible(true);
      testFrameRef.set(jFrame);

      // Find the File JMenu in the Croquet MenuBar.
      JMenuBar jMenuBar = jFrame.getJMenuBar();
      assertNotNull("JMenuBar not set on Croquet Frame", jMenuBar);
      for (int i = 0; i < jMenuBar.getMenuCount(); i++) {
        JMenu menu = jMenuBar.getMenu(i);
        if (menu != null && "File".equals(menu.getText())) {
          fileMenuRef.set(menu);
          break;
        }
      }
      assertNotNull("File JMenu not found in MenuBar", fileMenuRef.get());
    });

    Robot robot = new Robot();

    // Step 2: Wait for the Swing paint pass so the JMenu has stable screen bounds.
    robot.waitForIdle();
    robot.delay(200);

    // Step 3: Get the screen center of the "File" JMenu button (EDT only).
    AtomicReference<Point> fileMenuCenter = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      JMenu fileMenu = fileMenuRef.get();
      if (!fileMenu.isShowing()) {
        return;
      }
      Point loc = fileMenu.getLocationOnScreen();
      fileMenuCenter.set(
          new Point(loc.x + fileMenu.getWidth() / 2, loc.y + fileMenu.getHeight() / 2));
    });
    assertNotNull(
        "File JMenu button not showing on screen after testFrame.setVisible(true)",
        fileMenuCenter.get());

    // Step 4: Robot-click the File JMenu button to open its popup.
    robot.mouseMove(fileMenuCenter.get().x, fileMenuCenter.get().y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    // Step 5: Poll for the File popup to become visible (up to 2 s).
    boolean popupVisible = false;
    for (int i = 0; i < 20; i++) {
      robot.delay(100);
      boolean[] vis = new boolean[1];
      SwingUtilities.invokeAndWait(() -> vis[0] = fileMenuRef.get().isPopupMenuVisible());
      if (vis[0]) {
        popupVisible = true;
        break;
      }
    }
    assertTrue("File popup menu did not open after Robot click on File JMenu button", popupVisible);

    // Step 6: Find the Save JMenuItem in the open popup by Action identity (EDT only).
    AtomicReference<Point> saveItemCenter = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      JMenu fileMenu = fileMenuRef.get();
      JPopupMenu popup = fileMenu.getPopupMenu();
      javax.swing.Action saveAction =
          SaveProjectOperation.getInstance().getImp().getSwingModel().getAction();
      for (Component c : popup.getComponents()) {
        if (c instanceof JMenuItem item && item.getAction() == saveAction && item.isShowing()) {
          Point loc = item.getLocationOnScreen();
          saveItemCenter.set(
              new Point(loc.x + item.getWidth() / 2, loc.y + item.getHeight() / 2));
          break;
        }
      }
    });
    assertNotNull(
        "Save JMenuItem (by SaveProjectOperation action identity) not found in open File popup",
        saveItemCenter.get());

    // Step 7: Robot-click the Save JMenuItem — this fires a real AWT ActionEvent.
    robot.mouseMove(saveItemCenter.get().x, saveItemCenter.get().y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();
    // Allow ActionEvent dispatch and evidence write on the EDT before reading artifact.
    robot.delay(300);

    // Step 8: Assert evidence artifact contents.
    Path artifact =
        evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT);
    assertTrue("Evidence artifact missing: " + artifact, Files.exists(artifact));
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"menu_item_dispatched\""));
    assertTrue(json, json.contains("\"menu_item_dispatch\": true"));
    assertTrue(json, json.contains("\"trigger_class\": \"org.lgna.croquet.triggers.ActionEventTrigger\""));
    assertTrue(json, json.contains("\"view_controller_class\": \"org.lgna.croquet.views.MenuItem\""));
    assertTrue(json, json.contains("\"awt_source_class\": \"javax.swing.JMenuItem\""));
    // "Save dialog displayed" appears in doesNotClaim — correctly not claimed in PROOF_ONLY mode.
    assertTrue(json, json.contains("Save dialog displayed"));
    // No full save artifact: PROOF_ONLY mode cancels before SaveOperationFlow runs.
    assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
    } finally {
      cleanupRobotProofResources(testFrameRef, ideRef);
    }
  }

  private static void cleanupRobotProofResources(
      AtomicReference<JFrame> testFrameRef,
      AtomicReference<StageIDE> ideRef) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JFrame testFrame = testFrameRef.getAndSet(null);
      if (testFrame != null) {
        testFrame.dispose();
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

  private static void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  private static void resetActiveApplication() throws Exception {
    Field singleton = Application.class.getDeclaredField("singleton");
    singleton.setAccessible(true);
    singleton.set(null, null);
  }

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(
        Path.of("target", "jmenubar-robot-click-save-proof-test", UUID.randomUUID().toString()));
  }
}
