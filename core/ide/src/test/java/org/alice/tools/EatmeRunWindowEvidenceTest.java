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
  public void writesRunWindowCreatedArtifact() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();

    Path artifact = EatmeRunWindowEvidence.writeRunWindowCreated(evidenceDir, "Run \"Alice\"", "Program\nType");

    assertEquals(evidenceDir.resolve("run-window-created.json"), artifact);
    assertTrue(Files.size(artifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-run-window-created/v1\""));
    assertTrue(json, json.contains("\"status\": \"created\""));
    assertTrue(json, json.contains("\"frame_title\": \"Run \\\"Alice\\\"\""));
    assertTrue(json, json.contains("\"program_type\": \"Program\\nType\""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentArtifactPath() {
    EatmeRunWindowEvidence.artifactPath(temporaryFolder.getRoot().toPath(), "../run-window-created.json");
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
}
