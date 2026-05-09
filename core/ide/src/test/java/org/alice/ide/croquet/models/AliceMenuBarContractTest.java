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

    WindowMenuModel windowMenuModel = onlyRegisteredMenuOfType(menuBar, WindowMenuModel.class);

    assertTrue("WindowMenuModel should be reachable through menu bar membership lookup",
        menuBar.contains(windowMenuModel));
  }

  private static <T extends StandardMenuItemPrepModel> T onlyRegisteredMenuOfType(AliceMenuBar menuBar, Class<T> menuType) {
    List<T> registeredMenus = new ArrayList<>();
    for (StandardMenuItemPrepModel child : menuBar.getChildren()) {
      if (menuType.isInstance(child)) {
        registeredMenus.add(menuType.cast(child));
      }
    }
    assertEquals("desktop menu bar should register exactly one " + menuType.getSimpleName(),
        1, registeredMenus.size());
    return registeredMenus.get(0);
  }
}
