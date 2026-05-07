package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import org.alice.stageide.StageIDE;
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

import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuListener;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StageIdeSaveMenuItemDispatchProofTest {
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
  public void proofOnlySaveMenuItemClickDispatchesIntoSaveAction() throws Exception {
    Path evidenceDir = newTestDir();
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, "true");

    if (GraphicsEnvironment.isHeadless()) {
      SaveProjectOperation.getInstance().fire(new UserActivity());

      String json = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT));
      assertTrue(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"missing_active_stage_ide\""));
      assertTrue(json, json.contains("\"menu_item_dispatch\": false"));
      assertTrue(json, json.contains("desktop Save menu item was clicked"));
      return;
    }

    resetActiveApplication();
    SwingUtilities.invokeAndWait(() -> {
      StageIDE[] ide = new StageIDE[1];
      try {
        ide[0] = new StageIDE(new CrashDetector(StageIdeSaveMenuItemDispatchProofTest.class));
        ide[0].initialize(new String[0]);
        CapturingMenuItemContainer menu = new CapturingMenuItemContainer();
        ViewController<?, ?> menuItem =
            SaveProjectOperation.getInstance().getMenuItemPrepModel().createMenuItemAndAddTo(menu);
        assertTrue(menuItem instanceof MenuItem);
        assertTrue(menu.menuItem == menuItem);
        menu.menuItem.doClick();
      } finally {
        if (ide[0] != null
            && ide[0].getDocumentFrame() != null
            && ide[0].getDocumentFrame().getFrame() != null) {
          ide[0].getDocumentFrame().getFrame().release();
        }
      }
    });

    String json = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"menu_item_dispatched\""));
    assertTrue(json, json.contains("\"reason\": \"save_menu_item_dispatched\""));
    assertTrue(json, json.contains("\"menu_item_dispatch\": true"));
    assertTrue(json, json.contains("\"trigger_class\": \"org.lgna.croquet.triggers.ActionEventTrigger\""));
    assertTrue(json, json.contains("\"view_controller_class\": \"org.lgna.croquet.views.MenuItem\""));
    assertTrue(json, json.contains("\"awt_source_class\": \"javax.swing.JMenuItem\""));
    assertTrue(json, json.contains("SaveProjectOperation Swing menu item doClick dispatched through OperationSwingModel"));
    assertTrue(json, json.contains("Save dialog displayed"));
    assertFalse(json, json.contains("desktop Save menu item was clicked"));
    assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
  }

  @Test
  public void directSaveFireDoesNotPretendMenuItemClick() throws Exception {
    Path evidenceDir = newTestDir();
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, "true");

    SaveProjectOperation.getInstance().fire(new UserActivity());

    String json = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT));
    assertTrue(json, json.contains("\"menu_item_dispatch\": false"));
    assertTrue(json, json.contains("desktop Save menu item was clicked"));
    assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
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

  private static final class CapturingMenuItemContainer implements MenuItemContainer {
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
      return new AwtComponentView<?>[0];
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

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "save-menu-item-dispatch-proof-test",
        UUID.randomUUID().toString()));
  }
}
