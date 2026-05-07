package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.awt.FileDialogUtilities;
import org.junit.Test;

import javax.swing.JPanel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class SaveDialogDiscoveryTargetEvidenceTest {
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
}
