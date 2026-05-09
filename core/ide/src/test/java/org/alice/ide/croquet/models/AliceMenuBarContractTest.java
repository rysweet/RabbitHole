package org.alice.ide.croquet.models;

import org.alice.ide.croquet.models.menubar.WindowMenuModel;
import org.junit.Test;
import org.lgna.croquet.StandardMenuItemPrepModel;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AliceMenuBarContractTest {
  private static final UUID WINDOW_MENU_MODEL_ID = UUID.fromString("58a7297b-a5f8-499a-abd1-db6fca4083c8");

  @Test
  public void desktopMenuBarRegistersWindowMenuModel() {
    AliceMenuBar menuBar = new AliceMenuBar(null);

    WindowMenuModel windowMenuModel = null;
    for (StandardMenuItemPrepModel child : menuBar.getChildren()) {
      if (child instanceof WindowMenuModel candidateWindowMenuModel) {
        assertNull("desktop menu bar should register exactly one WindowMenuModel", windowMenuModel);
        windowMenuModel = candidateWindowMenuModel;
      }
    }

    assertNotNull("desktop menu bar should register exactly one WindowMenuModel", windowMenuModel);
    assertEquals("WindowMenuModel should keep the registered menu identity",
        WINDOW_MENU_MODEL_ID, windowMenuModel.getMigrationId());
    assertTrue("WindowMenuModel should be reachable through menu bar membership lookup",
        menuBar.contains(windowMenuModel));
  }
}
