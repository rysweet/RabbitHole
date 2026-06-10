package org.alice.ide.projecturi;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StartersTabTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void constructorUsesEmptyStarterListWhenStarterProjectsDirectoryIsMissing() throws Exception {
    String previousRootDirectory = System.getProperty("org.alice.ide.rootDirectory");
    try {
      System.setProperty("org.alice.ide.rootDirectory", temporaryFolder.newFolder("missing-alice-install").getAbsolutePath());

      StartersTab startersTab = new StartersTab();

      assertNotNull(startersTab.getListSelectionState());
      assertEquals(0, startersTab.getListSelectionState().getItemCount());
    } finally {
      if (previousRootDirectory == null) {
        System.clearProperty("org.alice.ide.rootDirectory");
      } else {
        System.setProperty("org.alice.ide.rootDirectory", previousRootDirectory);
      }
    }
  }
}
