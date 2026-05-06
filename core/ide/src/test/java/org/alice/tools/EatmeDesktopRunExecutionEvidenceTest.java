package org.alice.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EatmeDesktopRunExecutionEvidenceTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void writesDesktopRunExecutionArtifacts() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("evidence").toPath();

    Path artifact = EatmeDesktopRunExecutionEvidence.writeDesktopRunExecution(
        evidenceDir,
        "Program\nType",
        true,
        true,
        1,
        1,
        "executed:Comment",
        List.of("listener-installed", "set-active-scene-invoked", "executing:Comment", "executed:Comment"));

    assertEquals(evidenceDir.resolve("desktop-run-execution.json"), artifact);
    assertTrue(Files.size(artifact) > 0);
    assertTrue(Files.size(evidenceDir.resolve("desktop-run-runtime.log")) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"schema_version\": \"eatme.alice-desktop-run-execution/v1\""));
    assertTrue(json, json.contains("\"status\": \"statement_execution_observed\""));
    assertTrue(json, json.contains("\"program_type\": \"Program\\nType\""));
    assertTrue(json, json.contains("\"executing_statement_count\": 1"));
    assertTrue(json, json.contains("\"executed_statement_count\": 1"));
    String log = Files.readString(evidenceDir.resolve("desktop-run-runtime.log"));
    assertTrue(log, log.contains("schema_version=eatme.alice-desktop-run-execution-log/v1"));
    assertTrue(log, log.contains("executing:Comment"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsParentArtifactPathThroughSharedGuard() {
    EatmeRunWindowEvidence.artifactPath(temporaryFolder.getRoot().toPath(), "../desktop-run-execution.json");
  }
}
