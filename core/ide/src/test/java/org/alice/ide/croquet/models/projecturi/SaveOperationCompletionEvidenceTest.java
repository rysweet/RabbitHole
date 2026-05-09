package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;
import org.lgna.croquet.history.UserActivity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SaveOperationCompletionEvidenceTest {
  @Test
  public void writesSaveOperationResultWhenOptedIn() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("evidence"));
    File savedFile = Files.writeString(testDir.resolve("classroom.a3p"), "project").toFile();
    SaveOperationFlow.Result result = new SaveOperationFlow.Result(true, false, 1, 2, savedFile);

    Path artifact = SaveOperationCompletionEvidence.write(
        evidenceDir,
        "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
        "a3p",
        result);

    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-desktop-save-operation-result/v1\""));
    assertTrue(json, json.contains("\"status\": \"finished\""));
    assertTrue(json, json.contains("\"operation\": \"org.alice.ide.croquet.models.projecturi.SaveProjectOperation\""));
    assertTrue(json, json.contains("\"extension\": \"a3p\""));
    assertTrue(json, json.contains("\"finished\": true"));
    assertTrue(json, json.contains("\"canceled\": false"));
    assertTrue(json, json.contains("\"prompt_count\": 1"));
    assertTrue(json, json.contains("\"save_attempts\": 2"));
    assertTrue(json, json.contains("\"saved_file\": \"" + SaveOperationCompletionEvidence.escapeJson(savedFile.getPath()) + "\""));
    assertTrue(json, json.contains("\"saved_file_exists\": true"));
    assertTrue(json, json.contains("\"saved_file_size_bytes\": " + savedFile.length()));
    assertTrue(json, json.contains("Save dialog control"));
  }

  @Test
  public void completedSaveDialogWriteEvidenceNamesSwingChooserAndNarrowClaims() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("dialog-write-evidence"));
    File savedFile = Files.writeString(testDir.resolve("classroom.a3p"), "project").toFile();
    SaveOperationFlow.Result result = new SaveOperationFlow.Result(true, false, 1, 1, savedFile);

    Path artifact = SaveOperationCompletionEvidence.write(
        evidenceDir,
        "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
        "a3p",
        result);

    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"dialogType\": \"Swing JFileChooser\""));
    assertTrue(json, json.contains("\"wroteFile\": true"));
    assertTrue(json, json.contains("\"fileExtension\": \"a3p\""));
    assertTrue(json, json.contains("\"claim\": \"Save control/dialog approval reached a non-empty .a3p project file write\""));
    assertTrue(json, json.contains("\"full lesson completion\""));
    assertTrue(json, json.contains("\"visible rendering correctness\""));
    assertTrue(json, json.contains("\"grading correctness\""));
    assertTrue(json, json.contains("\"broad UI automation coverage\""));
    assertTrue(json, json.contains("\"native dialog coverage\""));
  }

  @Test
  public void redactsAbsoluteSavedFileOutsideWorkspaceInEvidence() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("redacted-path-evidence"));
    Path externalSavedFile = Files.createTempFile("alice-save-evidence-", ".a3p");
    try {
      Files.writeString(externalSavedFile, "project");
      File savedFile = externalSavedFile.toFile();
      Path artifact = SaveOperationCompletionEvidence.write(
          evidenceDir,
          "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
          "a3p",
          new SaveOperationFlow.Result(true, false, 1, 1, savedFile));

      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"saved_file\": \"[redacted]/" + savedFile.getName() + "\""));
      assertFalse(json, json.contains(SaveOperationCompletionEvidence.escapeJson(externalSavedFile.getParent().toString())));
      assertTrue(json, json.contains("\"saved_file_exists\": true"));
      assertTrue(json, json.contains("\"wroteFile\": true"));
    } finally {
      Files.deleteIfExists(externalSavedFile);
    }
  }

  @Test
  public void wroteFileIsFalseUntilSavedA3pExistsAndIsNonEmpty() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("empty-file-evidence"));
    File emptyFile = Files.createFile(testDir.resolve("empty.a3p")).toFile();
    SaveOperationFlow.Result result = new SaveOperationFlow.Result(true, false, 1, 1, emptyFile);

    Path artifact = SaveOperationCompletionEvidence.write(
        evidenceDir,
        "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
        "a3p",
        result);

    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"saved_file_exists\": true"));
    assertTrue(json, json.contains("\"saved_file_size_bytes\": 0"));
    assertTrue(json, json.contains("\"wroteFile\": false"));
    assertFalse(json, json.contains("\"wroteFile\": true"));
    assertFalse(json, json.contains("\"claim\""));
    assertFalse(json, json.contains("Save control/dialog approval reached a non-empty .a3p project file write"));
    assertTrue(json, json.contains("\"reporting_summary\": \"Save operation evidence recorded status finished without proving a non-empty .a3p project file write\""));
  }

  @Test
  public void recordIsOptIn() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("disabled-evidence"));
    String previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    System.clearProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      SaveOperationCompletionEvidence.record(
          "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
          "a3p",
          new SaveOperationFlow.Result(false, true, 1, 0, null));

      assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
    } finally {
      if (previousEvidenceDir == null) {
        System.clearProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      }
    }
  }

  @Test
  public void recordWritesArtifactWhenPropertyIsSet() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("enabled-evidence"));
    String previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    try {
      SaveOperationCompletionEvidence.record(
          "org.alice.ide.croquet.models.projecturi.SaveAsProjectOperation",
          "a3p",
          new SaveOperationFlow.Result(false, true, 1, 0, null));

      Path artifact = evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT);
      assertTrue(Files.size(artifact) > 0);
      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"status\": \"canceled\""));
      assertTrue(json, json.contains("\"canceled\": true"));
      assertTrue(json, json.contains("\"save_attempts\": 0"));
      assertTrue(json, json.contains("\"saved_file\": null"));
      assertTrue(json, json.contains("\"saved_file_exists\": null"));
      assertTrue(json, json.contains("\"saved_file_size_bytes\": null"));
      assertTrue(json, json.contains("\"wroteFile\": false"));
      assertFalse(json, json.contains("\"claim\""));
      assertFalse(json, json.contains("Save control/dialog approval reached a non-empty .a3p project file write"));
      assertTrue(json, json.contains("\"reporting_summary\": \"Save operation evidence recorded status canceled without proving a non-empty .a3p project file write\""));

      Path dialogArtifact = evidenceDir.resolve(SaveOperationCompletionEvidence.DIALOG_CONTROL_ARTIFACT);
      assertTrue(Files.size(dialogArtifact) > 0);
      String dialogJson = Files.readString(dialogArtifact);
      assertTrue(dialogJson, dialogJson.contains("\"schema_version\": \"eatme.alice-desktop-save-dialog-control-target/v1\""));
      assertTrue(dialogJson, dialogJson.contains("\"status\": \"blocked\""));
      assertTrue(dialogJson, dialogJson.contains("\"prompt_count\": 1"));
      assertTrue(dialogJson, dialogJson.contains("\"swing_file_chooser\""));
      assertFalse(dialogJson, dialogJson.contains("\"native_chooser\""));
      assertTrue(dialogJson, dialogJson.contains("org.lgna.croquet.DocumentFrame#showSaveFileDialog(File,String,String)"));
      assertTrue(dialogJson, dialogJson.contains("edu.cmu.cs.dennisc.java.awt.FileDialogUtilities#showSaveFileDialog(Component,File,String,String)"));
      assertTrue(dialogJson, dialogJson.contains("desktop Save dialog control"));
      assertTrue(dialogJson, dialogJson.contains("doesNotClaim"));
    } finally {
      if (previousEvidenceDir == null) {
        System.clearProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      }
    }
  }

  @Test
  public void recordDoesNotInterruptSaveWhenEvidenceWriteFails() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("broken-evidence"));
    String previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    try {
      SaveOperationCompletionEvidence.record(
          "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
          "a3p",
          null);

      assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
    } finally {
      if (previousEvidenceDir == null) {
        System.clearProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      }
    }
  }

  @Test
  public void dialogControlTargetReportsUnsupportedWhenSaveDidNotPrompt() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("dialog-evidence"));

    Path artifact = SaveOperationCompletionEvidence.writeDialogControlTarget(
        evidenceDir,
        "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
        "a3p",
        new SaveOperationFlow.Result(true, false, 0, 1, new File("classroom.a3p")));

    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-desktop-save-dialog-control-target/v1\""));
    assertTrue(json, json.contains("\"status\": \"unsupported\""));
    assertTrue(json, json.contains("\"reason\": \"save_operation_did_not_request_dialog\""));
    assertTrue(json, json.contains("\"prompt_count\": 0"));
    assertTrue(json, json.contains("No Save dialog was requested, so this artifact cannot prove dialog discovery or control."));
    assertTrue(json, json.contains("desktop Save dialog control"));
    assertTrue(json, json.contains("full Alice UI automation"));
  }

  @Test
  public void saveProjectOperationReportsMissingActiveStageIdeBeforeDesktopDialog() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("action-invocation"));
    String previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    try {
      SaveProjectOperation.getInstance().fire(new UserActivity());

      Path artifact = evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT);
      assertTrue(Files.size(artifact) > 0);
      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-desktop-save-action-invocation-proof/v1\""));
      assertTrue(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"missing_active_stage_ide\""));
      assertTrue(json, json.contains("org.alice.stageide.StageIDE.getActiveInstance()"));
      assertTrue(json, json.contains("SaveProjectOperation.getInstance().fire(UserActivity)"));
      assertTrue(json, json.contains("application.getDocumentFrame().showSaveFileDialog"));
      assertTrue(json, json.contains("desktop Save dialog control"));
      assertTrue(json, json.contains("doesNotClaim"));
      assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
    } finally {
      if (previousEvidenceDir == null) {
        System.clearProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
      }
    }
  }

  @Test
  public void saveActionInvocationReportsMissingProjectDocumentFrame() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("missing-document-frame"));

    Path artifact = SaveOperationCompletionEvidence.writeSaveActionInvocationProof(
        evidenceDir,
        "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
        "a3p",
        true,
        false);

    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"reason\": \"missing_project_document_frame\""));
    assertTrue(json, json.contains("\"active_stage_ide_available\": true"));
    assertTrue(json, json.contains("\"project_document_frame_available\": false"));
    assertTrue(json, json.contains("StageIDE.getActiveInstance() resolved, but application.getDocumentFrame() returned null."));
    assertTrue(json, json.contains("invoke SaveProjectOperation from an initialized Alice desktop with a ProjectDocumentFrame"));
    assertTrue(json, json.contains("\"desktop Save menu item was clicked\""));
    assertTrue(json, json.contains("\"Save dialog displayed\""));
  }

  @Test
  public void saveActionInvocationReportsActionInvokedWhenDesktopOwnerIsAvailable() throws Exception {
    Path evidenceDir = Files.createDirectories(newTestDir().resolve("action-owner-available"));

    Path artifact = SaveOperationCompletionEvidence.writeSaveActionInvocationProof(
        evidenceDir,
        "org.alice.ide.croquet.models.projecturi.SaveProjectOperation",
        "a3p",
        true,
        true);

    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"action_invoked\""));
    assertTrue(json, json.contains("\"reason\": \"save_action_invoked\""));
    assertTrue(json, json.contains("\"active_stage_ide_available\": true"));
    assertTrue(json, json.contains("\"project_document_frame_available\": true"));
    assertTrue(json, json.contains("\"menu_item_dispatch\": false"));
    assertTrue(json, json.contains("SaveProjectOperation.fire(UserActivity) reached AbstractSaveOperation.perform with an active StageIDE and ProjectDocumentFrame."));
    assertTrue(json, json.contains("desktop Save dialog discovery artifact with target_resolved"));
    assertTrue(json, json.contains("\"desktop Save menu item was clicked\""));
    assertTrue(json, json.contains("\"saved file completed\""));
  }

  @Test
  public void canonicalSaveCompletionEvidenceUsesSingleRenderedProofArtifactContract() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("canonical-rendered-save-proof"));
    File savedFile = Files.writeString(testDir.resolve("robot-save-menu-proof.a3p"), "project").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(savedFile, testDir);

    Path artifact = evidence.write(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT));

    assertEquals(
        "robot-save-menu-dialog-write-readback-proof.json",
        artifact.getFileName().toString());
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schemaVersion\": \"eatme.alice-desktop-save-menu-dialog-write-readback-proof/v1\""));
    assertFalse(json, json.contains("\"schema_version\""));
    assertTrue(json, json.contains("\"scenario\": \"alice-desktop-save-menu-dialog-write-proof\""));
    assertTrue(json, json.contains("\"workflow\": \"save-menu-dialog-write-proof\""));
    assertTrue(json, json.contains("\"runId\": "));
    assertTrue(json, json.contains("\"generatedAtUtc\": "));
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"blocker\": {"));
    assertTrue(json, json.contains("\"kind\": \"file_menu_not_showing\""));
    assertFalse(json, json.contains("\"claim\": "));
  }

  @Test
  public void canonicalEvidenceDerivesEarliestPreciseBlockerWhenDialogWasNeverReached() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("canonical-missing-dialog-proof"));
    File targetFile = testDir.resolve("robot-save-menu-proof.a3p").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(targetFile, testDir);

    Path artifact = evidence.write(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT));

    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"blocker\": {"));
    assertTrue(json, json.contains("\"kind\": \"file_menu_not_showing\""));
    assertTrue(json, json.contains("\"observed\": "));
    assertTrue(json, json.contains("\"required\": "));
    assertTrue(json, json.contains("\"requiresNextEvidence\": ["));
    assertFalse(json, json.contains("\"status\": \"proven\""));
    assertFalse(json, json.contains("\"claim\": "));
  }

  @Test
  public void canonicalEvidenceDoesNotProveWriteOnlyArtifactWithoutReadbackMarker() throws Exception {
    Path testDir = newTestDir();
    Path evidenceDir = Files.createDirectories(testDir.resolve("canonical-write-without-readback-proof"));
    File savedFile = Files.writeString(testDir.resolve("write-only.a3p"), "not a readable Alice archive").toFile();
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(savedFile, testDir);
    evidence.robotFileMenuOpened = true;
    evidence.robotSaveItemClicked = true;
    evidence.saveActionIdentityMatched = true;
    evidence.chooserObserved = true;
    evidence.dialogShowing = true;
    evidence.selectedFileVerified = true;
    evidence.targetInsideProofRoot = true;
    evidence.approvedSelection = true;
    evidence.normalizedSelectedFile = savedFile.getCanonicalPath();

    Path artifact = evidence.write(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT));

    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"status\": \"blocked\""));
    assertTrue(json, json.contains("\"kind\": \"readback_failed\""));
    assertTrue(json, json.contains("\"fileWritten\": true"));
    assertTrue(json, json.contains("\"fileNonempty\": true"));
    assertTrue(json, json.contains("\"projectReadable\": false"));
    assertTrue(json, json.contains("\"markerPresent\": false"));
    assertFalse(json, json.contains("\"status\": \"proven\""));
    assertFalse(json, json.contains("\"claim\": "));
  }

  @Test
  public void configuredSaveProofArtifactMustStayUnderProofRoot() throws Exception {
    Path proofRoot = Files.createDirectories(newTestDir().resolve("proof-root")).toRealPath();
    Path outsideRoot = newTestDir().resolve("outside-root");
    Path outsideArtifact = outsideRoot.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT);
    String previousEvidencePath = System.getProperty(
        SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY);
    System.setProperty(
        SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY,
        outsideArtifact.toString());
    try {
      IllegalArgumentException thrown = assertThrows(
          IllegalArgumentException.class,
          () -> SaveOperationCompletionEvidence.configuredSaveProofArtifact(proofRoot));

      assertTrue(thrown.getMessage().contains("must stay under the proof root"));
      assertFalse(Files.exists(outsideRoot));
    } finally {
      restoreProperty(
          SaveOperationCompletionEvidence.SAVE_PROOF_EVIDENCE_PATH_PROPERTY,
          previousEvidencePath);
    }
  }

  @Test
  public void configuredSaveProofRunIdRejectsUnsafeTokens() {
    String previousRunId = System.getProperty(
        SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_PROPERTY);
    System.setProperty(
        SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_PROPERTY,
        "../unsafe run id");
    try {
      IllegalArgumentException thrown = assertThrows(
          IllegalArgumentException.class,
          SaveOperationCompletionEvidence::configuredSaveProofRunId);

      assertTrue(thrown.getMessage().contains("safe token"));
    } finally {
      restoreProperty(SaveOperationCompletionEvidence.SAVE_PROOF_RUN_ID_PROPERTY, previousRunId);
    }
  }

  @Test
  public void configuredSaveProofScenarioRejectsUnexpectedScenario() {
    String previousScenario = System.getProperty(
        SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_PROPERTY);
    System.setProperty(
        SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_PROPERTY,
        "alice-desktop-other-scenario");
    try {
      IllegalArgumentException thrown = assertThrows(
          IllegalArgumentException.class,
          SaveOperationCompletionEvidence::configuredSaveProofScenario);

      assertTrue(thrown.getMessage().contains(SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO));
    } finally {
      restoreProperty(SaveOperationCompletionEvidence.SAVE_PROOF_SCENARIO_PROPERTY, previousScenario);
    }
  }

  @Test
  public void canonicalProofArtifactRejectsOutsideParentWithoutCreatingIt() throws Exception {
    Path proofRoot = Files.createDirectories(newTestDir().resolve("proof-root")).toRealPath();
    Path target = Files.writeString(proofRoot.resolve("robot-save-menu-proof.a3p"), "project");
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(target.toFile(), proofRoot);
    Path outsideRoot = newTestDir().resolve("outside-root");
    Path outsideArtifact = outsideRoot.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT);

    IllegalArgumentException thrown = assertThrows(
        IllegalArgumentException.class,
        () -> evidence.write(outsideArtifact));

    assertTrue(thrown.getMessage().contains("escapes proof root"));
    assertFalse(Files.exists(outsideRoot));
  }

  @Test
  public void canonicalProofArtifactRefusesSymlinkOverwrite() throws Exception {
    Path proofRoot = Files.createDirectories(newTestDir().resolve("proof-root")).toRealPath();
    Path target = Files.writeString(proofRoot.resolve("robot-save-menu-proof.a3p"), "project");
    SaveOperationCompletionEvidence.SaveProofEvidence evidence =
        SaveOperationCompletionEvidence.saveProofEvidence(target.toFile(), proofRoot);
    Path outsideRoot = Files.createDirectories(newTestDir().resolve("outside-root")).toRealPath();
    Path outsideArtifact = Files.writeString(
        outsideRoot.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT),
        "{}");
    Path symlinkArtifact = proofRoot.resolve(SaveOperationCompletionEvidence.SAVE_PROOF_ARTIFACT);
    try {
      Files.createSymbolicLink(symlinkArtifact, outsideArtifact);
    } catch (IOException | SecurityException | UnsupportedOperationException e) {
      return;
    }

    IOException thrown = assertThrows(IOException.class, () -> evidence.write(symlinkArtifact));
    assertTrue(thrown.getMessage().contains("refuses to overwrite symlink"));
  }

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "save-operation-completion-evidence-test",
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
