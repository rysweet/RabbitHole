package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
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
    assertTrue(json, json.contains("Save dialog control"));
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

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "save-operation-completion-evidence-test",
        UUID.randomUUID().toString()));
  }
}
