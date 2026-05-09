package org.alice.ide.croquet.models;

import org.alice.ide.croquet.models.menubar.EditMenuModel;
import org.alice.ide.croquet.models.menubar.FileMenuModel;
import org.alice.ide.croquet.models.menubar.HelpMenuModel;
import org.alice.ide.croquet.models.menubar.ProjectMenuModel;
import org.alice.ide.croquet.models.menubar.RunMenuModel;
import org.alice.ide.croquet.models.menubar.WindowMenuModel;
import org.junit.Test;
import org.lgna.croquet.StandardMenuItemPrepModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AliceMenuBarContractTest {
  @Test
  public void desktopMenuBarRegistersUserFacingMenusInStableOrder() {
    AliceMenuBar menuBar = new AliceMenuBar(null);

    List<Class<?>> childTypes = childTypes(menuBar);

    assertEquals(Arrays.asList(
        FileMenuModel.class,
        EditMenuModel.class,
        ProjectMenuModel.class,
        RunMenuModel.class,
        WindowMenuModel.class,
        HelpMenuModel.class), childTypes);
  }

  @Test
  public void desktopMenuBarContainsRegisteredMenusForControllerLookup() {
    AliceMenuBar menuBar = new AliceMenuBar(null);

    for (StandardMenuItemPrepModel child : menuBar.getChildren()) {
      assertTrue("menu bar should contain registered child " + child.getClass().getName(), menuBar.contains(child));
    }
  }

  @Test
  public void desktopMenuBarRegistersWindowMenuModel() {
    AliceMenuBar menuBar = new AliceMenuBar(null);

    StandardMenuItemPrepModel windowMenuModel = registeredMenuOfType(menuBar, WindowMenuModel.class);

    assertNotNull("WindowMenuModel should be registered in the desktop menu bar", windowMenuModel);
    assertTrue("WindowMenuModel should be reachable through menu bar registration", menuBar.contains(windowMenuModel));
  }

  private static List<Class<?>> childTypes(AliceMenuBar menuBar) {
    List<Class<?>> childTypes = new ArrayList<>();
    for (StandardMenuItemPrepModel child : menuBar.getChildren()) {
      childTypes.add(child.getClass());
    }
    return childTypes;
  }

  private static StandardMenuItemPrepModel registeredMenuOfType(AliceMenuBar menuBar, Class<?> menuType) {
    for (StandardMenuItemPrepModel child : menuBar.getChildren()) {
      if (menuType.isInstance(child)) {
        return child;
      }
    }
    return null;
  }
}
