package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.junit.Test;

import javax.swing.JPanel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

public class SaveDialogDiscoveryTargetEvidenceTest {
  @Test
  public void selectedPathPropertyReturnsControlledSavePathWithoutClaimingDialogDisplay() throws Exception {
    Path testDir = newTestDir().resolve("selected-path");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    File requestedDirectory = Files.createDirectories(testDir.resolve("projects")).toFile();
    Path selectedPathWithoutExtension = testDir.resolve("projects").resolve("classroom").toAbsolutePath();
    String previousEvidenceDir = System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    String previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, selectedPathWithoutExtension.toString());
    try {
      File selectedFile = FileDialogUtilities.showSaveFileDialog(
          new JPanel(),
          requestedDirectory,
          "ignored-in-automation",
          "a3p");

      assertTrue(selectedFile.getAbsolutePath().endsWith("classroom.a3p"));
      assertTrue(selectedFile.toPath().toAbsolutePath().normalize().startsWith(requestedDirectory.toPath().toAbsolutePath().normalize()));
      String json = Files.readString(evidenceDir.resolve(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT));
      assertTrue(json, json.contains("\"selected_path_automation\""));
      assertTrue(json, json.contains("\"status\": \"selected_path_injected\""));
      assertTrue(json, json.contains("\"reason\": \"selected_path_property_accepted\""));
      assertTrue(json, json.contains("\"property\": \"" + FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY + "\""));
      assertTrue(json, json.contains("\"selected_file\": \"" + FileDialogUtilities.escapeJson(selectedFile.getPath()) + "\""));
      assertTrue(json, json.contains("\"safe_under_requested_directory\": true"));
      assertTrue(json, json.contains("\"doesNotClaim\""));
      assertTrue(json, json.contains("\"Save dialog displayed\""));
      assertTrue(json, json.contains("\"selected Save path supplied by UI automation\""));
    } finally {
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
    }
  }

  @Test
  public void selectedPathPropertyOutsideRequestedDirectoryCancelsAndReportsUnsupported() throws Exception {
    Path testDir = newTestDir().resolve("selected-path-outside-requested-directory");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    File requestedDirectory = Files.createDirectories(testDir.resolve("projects")).toFile();
    Path outsidePath = Files.createDirectories(testDir.resolve("outside")).resolve("classroom.a3p").toAbsolutePath();
    String previousEvidenceDir = System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    String previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, outsidePath.toString());
    try {
      File selectedFile = FileDialogUtilities.showSaveFileDialog(
          new JPanel(),
          requestedDirectory,
          "ignored-in-automation",
          "a3p");

      assertTrue(selectedFile == null);
      String json = Files.readString(evidenceDir.resolve(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT));
      assertTrue(json, json.contains("\"selected_path_automation\""));
      assertTrue(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"selected_path_outside_requested_directory\""));
      assertTrue(json, json.contains("\"safe_under_requested_directory\": false"));
      assertTrue(json, json.contains("\"selected_file\": null"));
      assertTrue(json, json.contains("Configured selected Save path must stay under the requested directory."));
      assertTrue(json, json.contains("\"doesNotClaim\""));
      assertTrue(json, json.contains("\"Save dialog displayed\""));
    } finally {
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
    }
  }

  @Test
  public void selectedPathPropertyThroughSymlinkedDirectoryCancelsAndReportsUnsupported() throws Exception {
    Path testDir = newTestDir().resolve("selected-path-through-symlink");
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    Path requestedDirectory = Files.createDirectories(testDir.resolve("projects"));
    Path outsideDirectory = Files.createDirectories(testDir.resolve("outside"));
    Path linkedDirectory = requestedDirectory.resolve("linked-outside");
    try {
      Files.createSymbolicLink(linkedDirectory, outsideDirectory.toAbsolutePath());
    } catch (IOException | SecurityException | UnsupportedOperationException ex) {
      assumeNoException(ex);
    }
    String previousEvidenceDir = System.getProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY);
    String previousSelectedPath = System.getProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY);
    System.setProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(
        FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY,
        linkedDirectory.resolve("classroom.a3p").toAbsolutePath().toString());
    try {
      File selectedFile = FileDialogUtilities.showSaveFileDialog(
          new JPanel(),
          requestedDirectory.toFile(),
          "ignored-in-automation",
          "a3p");

      assertTrue(selectedFile == null);
      String json = Files.readString(evidenceDir.resolve(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_TARGET_ARTIFACT));
      assertTrue(json, json.contains("\"selected_path_automation\""));
      assertTrue(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"selected_path_outside_requested_directory\""));
      assertTrue(json, json.contains("\"safe_under_requested_directory\": false"));
      assertTrue(json, json.contains("\"selected_file\": null"));
    } finally {
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_DISCOVERY_EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      restoreProperty(FileDialogUtilities.SAVE_DIALOG_SELECTED_PATH_PROPERTY, previousSelectedPath);
    }
  }

  @Test
  public void writesBlockedDialogDiscoveryTargetWhenOwnerHasNoRootWindow() throws Exception {
    Path evidenceDir = newTestDir().resolve("dialog-discovery");

    Path artifact = FileDialogUtilities.writeSaveDialogDiscoveryTarget(
        evidenceDir,
        new JPanel(),
        new File("target/projects"),
        "classroom",
        "a3p");

    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-desktop-save-dialog-discovery-target/v1\""));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"reason\": \"missing_dialog_root_window\""));
    assertTrue(json, json.contains("\"source\": \"edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)\""));
    assertTrue(json, json.contains("\"owner_component_class\": \"javax.swing.JPanel\""));
    assertTrue(json, json.contains("\"root_component_class\": null"));
    assertTrue(json, json.contains("\"directory\": \"" + FileDialogUtilities.escapeJson(new File("target/projects").getPath()) + "\""));
    assertTrue(json, json.contains("\"filename\": \"classroom\""));
    assertTrue(json, json.contains("\"extension\": \"a3p\""));
    assertTrue(json, json.contains("displayable Alice ProjectDocumentFrame root window"));
    assertTrue(json, json.contains("FileDialogUtilities.showSaveFileDialog"));
    assertTrue(json, json.contains("Save dialog displayed"));
    assertTrue(json, json.contains("Save dialog controlled"));
  }

  @Test
  public void writesUnsupportedDialogDiscoveryTargetWhenOwnerComponentIsMissing() throws Exception {
    Path evidenceDir = newTestDir().resolve("missing-owner");

    Path artifact = FileDialogUtilities.writeSaveDialogDiscoveryTarget(
        evidenceDir,
        null,
        null,
        null,
        "a3p");

    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"reason\": \"missing_owner_component\""));
    assertTrue(json, json.contains("\"owner_component_class\": null"));
    assertTrue(json, json.contains("\"directory\": null"));
    assertTrue(json, json.contains("\"filename\": null"));
    assertTrue(json, json.contains("No owner Component was supplied, so Alice cannot discover or control a Save dialog from this seam."));
    assertTrue(json, json.contains("doesNotClaim"));
  }

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "save-dialog-discovery-target-evidence-test",
        UUID.randomUUID().toString()));
  }

  private static void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }
}
