package org.alice.ide.croquet.models;

import org.alice.ide.croquet.models.menubar.WindowMenuModel;
import org.junit.Test;
import org.lgna.croquet.StandardMenuItemPrepModel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AliceMenuBarContractTest {
  @Test
  public void desktopMenuBarRegistersWindowMenuModel() {
    AliceMenuBar menuBar = new AliceMenuBar(null);

    List<WindowMenuModel> windowMenuModels = registeredMenusOfType(menuBar, WindowMenuModel.class);

    assertEquals("desktop menu bar should register exactly one WindowMenuModel", 1, windowMenuModels.size());
    assertTrue("WindowMenuModel should be reachable through menu bar membership lookup",
        menuBar.contains(windowMenuModels.get(0)));
  }

  private static <T extends StandardMenuItemPrepModel> List<T> registeredMenusOfType(AliceMenuBar menuBar, Class<T> menuType) {
    List<T> registeredMenus = new ArrayList<>();
    for (StandardMenuItemPrepModel child : menuBar.getChildren()) {
      if (menuType.isInstance(child)) {
        registeredMenus.add(menuType.cast(child));
      }
    }
    return registeredMenus;
  }
}
