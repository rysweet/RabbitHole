package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

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
  public void rejectsMissingEvidenceDirectoryWithoutCreatingIt() throws Exception {
    Path missingEvidenceDir = temporaryFolder.getRoot().toPath().resolve("missing-evidence");

    try {
      EatmeRunWindowEvidence.writeRunWindowCreated(missingEvidenceDir, "Run Alice", "Program");
      fail("missing evidence directory should be rejected");
    } catch (IOException expected) {
      assertTrue(Files.notExists(missingEvidenceDir));
    }
  }

  @Test
  public void refusesToFollowArtifactSymlinkOutsideEvidenceDirectory() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("symlink-evidence").toPath();
    Path outsideArtifact = temporaryFolder.newFile("outside-run-window-created.json").toPath();
    Files.writeString(outsideArtifact, "outside");
    Files.createSymbolicLink(evidenceDir.resolve(EatmeRunWindowEvidence.RUN_WINDOW_CREATED_ARTIFACT), outsideArtifact);

    try {
      EatmeRunWindowEvidence.writeRunWindowCreated(evidenceDir, "Run Alice", "Program");
      fail("artifact symlink should be rejected");
    } catch (IOException expected) {
      assertEquals("outside", Files.readString(outsideArtifact));
    }
  }

  @Test
  public void replacesHardLinkedArtifactWithoutMutatingLinkedTarget() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("hardlink-evidence").toPath();
    Path outsideArtifact = temporaryFolder.newFile("outside-hardlinked-run-window-created.json").toPath();
    Files.writeString(outsideArtifact, "outside");
    Path hardLinkedArtifact = evidenceDir.resolve(EatmeRunWindowEvidence.RUN_WINDOW_CREATED_ARTIFACT);
    try {
      Files.createLink(hardLinkedArtifact, outsideArtifact);
    } catch (IOException | SecurityException | UnsupportedOperationException ex) {
      assumeTrue("hard links are unavailable in this test environment", false);
    }

    EatmeRunWindowEvidence.writeRunWindowCreated(evidenceDir, "Run Alice", "Program");

    assertEquals("outside", Files.readString(outsideArtifact));
    String json = Files.readString(hardLinkedArtifact);
    assertTrue(json, json.contains("\"status\": \"created\""));
  }

  @Test
  public void rejectsSymlinkEvidenceDirectoryWithoutWritingOutsideScratchRoot() throws Exception {
    Path scratchRoot = temporaryFolder.newFolder("scratch-root").toPath();
    Path outsideEvidenceTarget = temporaryFolder.newFolder("outside-evidence-target").toPath();
    Path symlinkEvidenceDir = scratchRoot.resolve("linked-evidence");
    Files.createSymbolicLink(symlinkEvidenceDir, outsideEvidenceTarget);

    try {
      EatmeRunWindowEvidence.writeRunWindowCreated(symlinkEvidenceDir, "Run Alice", "Program");
      fail("symlink evidence directory should be rejected");
    } catch (IOException expected) {
      assertTrue(Files.notExists(outsideEvidenceTarget.resolve(EatmeRunWindowEvidence.RUN_WINDOW_CREATED_ARTIFACT)));
    }
  }

  @Test
  public void recordRunWindowCreatedSurfacesInvalidConfiguredPath() {
    String previous = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, "bad\0path");

      try {
        EatmeRunWindowEvidence.recordRunWindowCreated(null, null);
        fail("invalid configured evidence path should be surfaced");
      } catch (IllegalStateException expected) {
        assertTrue(expected.getMessage().contains("Run-window evidence write failed"));
      }
    } finally {
      if (previous == null) {
        System.clearProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, previous);
      }
    }
  }

  @Test
  public void recordRunWindowCreatedWritesConfiguredEvidenceArtifact() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("configured-evidence").toPath();
    String previous = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());

      EatmeRunWindowEvidence.recordRunWindowCreated(null, null);

      Path artifact = evidenceDir.resolve(EatmeRunWindowEvidence.RUN_WINDOW_CREATED_ARTIFACT);
      assertTrue(Files.isRegularFile(artifact));
      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-run-window-created/v1\""));
      assertTrue(json, json.contains("\"status\": \"created\""));
      assertTrue(json, json.contains("\"artifact\": \"run-window-created.json\""));
    } finally {
      if (previous == null) {
        System.clearProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
      } else {
        System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, previous);
      }
    }
  }

  @Test
  public void runnerConfiguredEvidenceDirectoryWritesScenarioArtifact() throws Exception {
    String configuredEvidenceDir = System.getenv("ALICE_RUN_WINDOW_EVIDENCE_DIR");
    assumeTrue("runner evidence directory is provided only by the outside-in scenario runner",
        configuredEvidenceDir != null && !configuredEvidenceDir.isBlank());
    Path evidenceDir = Path.of(configuredEvidenceDir);
    assertTrue(Files.isDirectory(evidenceDir));

    String previous = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, configuredEvidenceDir);

      EatmeRunWindowEvidence.recordRunWindowCreated(null, null);

      Path artifact = evidenceDir.resolve(EatmeRunWindowEvidence.RUN_WINDOW_CREATED_ARTIFACT);
      assertTrue(Files.isRegularFile(artifact));
      String json = Files.readString(artifact);
      assertTrue(json, json.contains("\"contract_scope\": \"run-window-creation-wiring\""));
      assertFalseClaim(json, "active_rendering_claimed");
      assertFalseClaim(json, "full_ui_automation_claimed");
    } finally {
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
