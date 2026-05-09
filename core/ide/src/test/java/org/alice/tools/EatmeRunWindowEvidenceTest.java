package org.alice.tools;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EatmeRunWindowEvidenceTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void writesCreationWiringOnlyRunWindowContractArtifact() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();

    Path artifact = EatmeRunWindowEvidence.writeRunWindowCreated(evidenceDir, "Run \"Alice\"", "Program\nType");

    assertEquals(evidenceDir.resolve("run-window-created.json"), artifact);
    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-run-window-created/v1\""));
    assertTrue(json, json.contains("\"status\": \"created\""));
    assertTrue(json, json.contains("\"contract_scope\": \"run-window-creation-wiring\""));
    assertTrue(json, json.contains("\"evidence_source\": \"org.alice.stageide.run.RunComposite#handlePreShowWindow\""));
    assertTrue(json, json.contains("\"artifact\": \"run-window-created.json\""));
    assertTrue(json, json.contains("\"frame_title\": \"Run \\\"Alice\\\"\""));
    assertTrue(json, json.contains("\"program_type\": \"Program\\nType\""));
    assertFalseClaim(json, "active_rendering_claimed");
    assertFalseClaim(json, "run_program_claimed");
    assertFalseClaim(json, "run_execution_claimed");
    assertFalseClaim(json, "world_execution_claimed");
    assertFalseClaim(json, "rendering_correctness_claimed");
    assertFalseClaim(json, "save_claimed");
    assertFalseClaim(json, "grading_claimed");
    assertFalseClaim(json, "full_ui_automation_claimed");
    assertDoesNotClaim(json, "active-rendering");
    assertDoesNotClaim(json, "run-execution");
    assertDoesNotClaim(json, "world-execution-correctness");
    assertDoesNotClaim(json, "rendering-correctness");
    assertDoesNotClaim(json, "save");
    assertDoesNotClaim(json, "grading");
    assertDoesNotClaim(json, "full-ui-automation");
    assertTrue(json, !json.contains("\"run_program_claimed\": true"));
    assertTrue(json, !json.contains("\"rendering_correctness_claimed\": true"));
    assertTrue(json, !json.contains("\"save_claimed\": true"));
    assertTrue(json, !json.contains("\"grading_claimed\": true"));
  }

  @Test
  public void escapesUntrustedDisplayMetadataWithoutChangingContractFields() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("escaping-evidence").toPath();

    Path artifact = EatmeRunWindowEvidence.writeRunWindowCreated(
        evidenceDir,
        "Run \\ \"Alice\"\t\u0001",
        "Program\r\nType");

    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"frame_title\": \"Run \\\\ \\\"Alice\\\"\\t\\u0001\""));
    assertTrue(json, json.contains("\"program_type\": \"Program\\r\\nType\""));
    assertTrue(json, json.contains("\"artifact\": \"run-window-created.json\""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(temporaryFolder.getRoot().toPath(), "../run-window-created.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsNestedArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(temporaryFolder.getRoot().toPath(), "nested/run-window-created.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsAbsoluteArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(temporaryFolder.getRoot().toPath(), "/tmp/run-window-created.json");
  }

  @Test
  public void recordRunWindowCreatedDoesNotAbortRunWhenConfiguredPathIsInvalid() {
    String previous = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    Level previousLevel = Logger.getLevel();
    try {
      Logger.setLevel(Level.OFF);
      System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, "bad\0path");

      EatmeRunWindowEvidence.recordRunWindowCreated(null, null);
    } finally {
      Logger.setLevel(previousLevel);
      if (previous == null) {
        System.clearProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, previous);
      }
    }
  }

  private static void assertFalseClaim(String json, String fieldName) {
    assertTrue(json, json.contains("\"" + fieldName + "\": false"));
  }

  private static void assertDoesNotClaim(String json, String claim) {
    assertTrue(json, json.contains("\"" + claim + "\""));
  }
}
