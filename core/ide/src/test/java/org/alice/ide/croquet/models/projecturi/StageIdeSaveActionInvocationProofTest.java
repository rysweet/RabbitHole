package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.crash.CrashDetector;
import org.alice.stageide.StageIDE;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.Application;
import org.lgna.croquet.history.UserActivity;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StageIdeSaveActionInvocationProofTest {
  private String previousEvidenceDir;
  private String previousProofOnly;

  @Before
  public void captureProperties() {
    previousEvidenceDir = System.getProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY);
    previousProofOnly = System.getProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY);
  }

  @After
  public void restorePropertiesAndActiveApplication() throws Exception {
    restoreProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
    restoreProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, previousProofOnly);
    resetActiveApplication();
  }

  @Test
  public void proofOnlySaveFireStopsAfterActiveStageIdeAndDocumentFrame() throws Exception {
    Path evidenceDir = newTestDir();
    System.setProperty(SaveOperationCompletionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    System.setProperty(SaveOperationCompletionEvidence.PROOF_ONLY_PROPERTY, "true");

    if (GraphicsEnvironment.isHeadless()) {
      SaveProjectOperation.getInstance().fire(new UserActivity());

      String json = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT));
      assertTrue(json, json.contains("\"status\": \"unsupported\""));
      assertTrue(json, json.contains("\"reason\": \"missing_active_stage_ide\""));
      assertTrue(json, json.contains("StageIDE.getActiveInstance() returned null"));
      return;
    }

    resetActiveApplication();
    SwingUtilities.invokeAndWait(() -> {
      StageIDE[] ide = new StageIDE[1];
      try {
        ide[0] = new StageIDE(new CrashDetector(StageIdeSaveActionInvocationProofTest.class));
        ide[0].initialize(new String[0]);
        SaveProjectOperation.getInstance().fire(new UserActivity());
      } finally {
        if (ide[0] != null
            && ide[0].getDocumentFrame() != null
            && ide[0].getDocumentFrame().getFrame() != null) {
          ide[0].getDocumentFrame().getFrame().release();
        }
      }
    });

    String json = Files.readString(evidenceDir.resolve(SaveOperationCompletionEvidence.SAVE_ACTION_INVOCATION_PROOF_ARTIFACT));
    assertTrue(json, json.contains("\"status\": \"action_invoked\""));
    assertTrue(json, json.contains("\"reason\": \"save_action_invoked\""));
    assertTrue(json, json.contains("\"active_stage_ide_available\": true"));
    assertTrue(json, json.contains("\"project_document_frame_available\": true"));
    assertTrue(json, json.contains("active StageIDE and ProjectDocumentFrame"));
    assertFalse(Files.exists(evidenceDir.resolve(SaveOperationCompletionEvidence.ARTIFACT)));
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
        "save-stageide-invocation-proof-test",
        UUID.randomUUID().toString()));
  }
}
