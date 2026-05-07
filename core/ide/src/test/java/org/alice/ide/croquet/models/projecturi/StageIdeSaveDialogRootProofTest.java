package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.alice.stageide.StageIDE;
import org.junit.After;
import org.junit.Test;
import org.lgna.croquet.Application;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class StageIdeSaveDialogRootProofTest {
  @After
  public void resetApplication() throws Exception {
    resetActiveApplication();
  }

  @Test
  public void documentFrameSaveDialogDiscoveryResolvesDisplayableRootUnderXvfb() throws Exception {
    assumeFalse("requires Xvfb or another headful AWT display", GraphicsEnvironment.isHeadless());
    Path testDir = newTestDir().resolve("stageide-displayable-root");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path projectsDir = Files.createDirectories(testDir.resolve("projects"));
    Path selectedPath = projectsDir.resolve("classroom-root-proof").toAbsolutePath();
    String previousEvidenceDir = System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    String previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, selectedPath.toString());
    try {
      AtomicReference<File> selectedFile = new AtomicReference<>();
      SwingUtilities.invokeAndWait(() -> {
        StageIDE ide = null;
        try {
          ide = new StageIDE(new CrashDetector(StageIdeSaveDialogRootProofTest.class));
          ide.initialize(new String[0]);
          ide.getDocumentFrame().getFrame().pack();
          ide.getDocumentFrame().getFrame().setVisible(true);
          assertTrue(ide.getDocumentFrame().getFrame().getAwtComponent().isDisplayable());
          selectedFile.set(ide.getDocumentFrame().showSaveFileDialog(
              projectsDir.toFile(),
              "classroom-root-proof",
              "a3p"));
        } finally {
          if (ide != null
              && ide.getDocumentFrame() != null
              && ide.getDocumentFrame().getFrame() != null) {
            ide.getDocumentFrame().getFrame().release();
          }
        }
      });

      assertEquals(projectsDir.resolve("classroom-root-proof.a3p").toAbsolutePath().toFile(), selectedFile.get());
      String json = Files.readString(evidenceDir.resolve(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT));
      assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-desktop-save-dialog-discovery-target/v1\""));
      assertTrue(json, json.contains("\"status\": \"target_resolved\""));
      assertTrue(json, json.contains("\"reason\": \"target_resolved\""));
      assertTrue(json, json.contains("\"owner_component_class\": \"javax.swing.JFrame\""));
      assertTrue(json, json.contains("\"owner_displayable\": true"));
      assertTrue(json, json.contains("\"root_component_class\": \"javax.swing.JFrame\""));
      assertTrue(json, json.contains("\"root_displayable\": true"));
      assertTrue(json, json.contains("owner Component and displayable root Component resolved"));
      assertTrue(json, json.contains("\"status\": \"selected_path_injected\""));
      assertFalse(json, json.contains("displayable Alice ProjectDocumentFrame root window at FileDialogUtilities.showSaveFileDialog"));
      assertTrue(json, json.contains("Save dialog displayed result from FileDialogUtilities after FileDialog.show() returns"));
      assertTrue(json, json.contains("\"Save dialog displayed\""));
    } finally {
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
    }
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
    return Files.createDirectories(Path.of(
        "target",
        "save-stageide-dialog-root-proof-test",
        UUID.randomUUID().toString()));
  }
}
