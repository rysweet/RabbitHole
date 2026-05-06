package org.alice.tools;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

  @Test
  public void renderTargetAffordanceRecorderIsOptIn() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("disabled-evidence").toPath();
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    String previousRunWindowEvidenceDir = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    System.clearProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    System.clearProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          new JPanel(),
          new JPanel(),
          new JPanel(),
          false);

      assertFalse(Files.exists(evidenceDir.resolve("desktop-run-render-affordance.json")));
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
      restoreRunWindowEvidenceDirProperty(previousRunWindowEvidenceDir);
    }
  }

  @Test
  public void writesConservativeRenderTargetAffordanceArtifact() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("render-affordance").toPath();
    JPanel renderTargetComponent = namedPanel("render \"target\"\ncomponent");
    JPanel renderPanelComponent = new JPanel(new BorderLayout());
    JPanel runViewComponent = new JPanel(new BorderLayout());
    runViewComponent.add(renderPanelComponent, BorderLayout.CENTER);

    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    try {
      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          renderTargetComponent,
          renderPanelComponent,
          runViewComponent,
          true);
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
    }

    Path artifact = evidenceDir.resolve("desktop-run-render-affordance.json");
    Path pixelBoundaryArtifact = evidenceDir.resolve("desktop-run-pixel-boundary.json");
    assertTrue(Files.size(artifact) > 0);
    assertTrue(Files.size(pixelBoundaryArtifact) > 0);
    String json = Files.readString(artifact);
    assertTrue(json, json.contains("\"evidenceKind\": \"desktop_run_render_affordance\""));
    assertTrue(json, json.contains("\"renderTargetAttachedToRunView\": true"));
    assertTrue(json, json.contains("\"renderTargetComponentClass\": \"javax.swing.JPanel\""));
    assertTrue(json, json.contains("\"renderTargetComponentName\": \"render \\\"target\\\"\\ncomponent\""));
    assertTrue(json, json.contains("\"renderTargetDisplayable\": false"));
    assertTrue(json, json.contains("\"renderTargetShowing\": false"));
    assertTrue(json, json.contains("\"renderPanelComponentClass\": \"javax.swing.JPanel\""));
    assertTrue(json, json.contains("\"runViewComponentClass\": \"javax.swing.JPanel\""));
    assertTrue(json, json.contains("\"runViewComponentCountAfterAttach\": 1"));
    assertTrue(json, json.contains("\"controlPanelAttached\": true"));
    assertTrue(json, json.contains("\"claim\": \"A Run view attachment signal was observed.\""));
    assertTrue(json, json.contains("\"doesNotClaim\""));
    assertTrue(json, json.contains("visible rendering"));
    assertTrue(json, json.contains("graphics or OpenGL rendering success"));
    assertTrue(json, json.contains("pixel output validation"));
    assertTrue(json, json.contains("screenshot validation"));
    assertTrue(json, json.contains("end-to-end UI correctness"));
    assertTrue(json, json.contains("lesson completion"));
    assertNoField(json, "x");
    assertNoField(json, "y");
    assertNoField(json, "width");
    assertNoField(json, "height");
    assertNoField(json, "bounds");
    assertNoField(json, "color");
    assertNoField(json, "pixel");
    assertNoField(json, "pixels");
    assertNoField(json, "screenshot");
    assertNoField(json, "screenLocation");
    assertNoField(json, "mousePosition");

    String pixelBoundaryJson = Files.readString(pixelBoundaryArtifact);
    assertTrue(pixelBoundaryJson,
        pixelBoundaryJson.contains("\"schema_version\": \"eatme.alice-desktop-run-pixel-boundary/v1\""));
    assertTrue(pixelBoundaryJson, pixelBoundaryJson.contains("\"status\": \"not_observed\""));
    assertTrue(pixelBoundaryJson, pixelBoundaryJson.contains("does not inspect screenshots or pixel output"));
    assertTrue(pixelBoundaryJson, pixelBoundaryJson.contains("requiresSeparateEvidence"));
    assertTrue(pixelBoundaryJson, pixelBoundaryJson.contains("visible rendering"));
    assertTrue(pixelBoundaryJson, pixelBoundaryJson.contains("pixel output validation"));
    assertTrue(pixelBoundaryJson, pixelBoundaryJson.contains("grading"));
  }

  @Test
  public void renderTargetAffordanceRecorderFallsBackToExistingRunWindowEvidenceProperty() throws Exception {
    Path evidenceDir = temporaryFolder.newFolder("legacy-run-window-evidence").toPath();
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    String previousRunWindowEvidenceDir = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    System.clearProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());
    try {
      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          new JPanel(),
          new JPanel(),
          new JPanel(),
          true);
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
      restoreRunWindowEvidenceDirProperty(previousRunWindowEvidenceDir);
    }

    Path artifact = evidenceDir.resolve("desktop-run-render-affordance.json");
    assertTrue(Files.size(artifact) > 0);
  }

  @Test
  public void renderTargetAffordanceRecorderPrefersDedicatedDesktopRunEvidenceProperty() throws Exception {
    Path legacyEvidenceDir = temporaryFolder.newFolder("legacy-run-window-evidence").toPath();
    Path dedicatedEvidenceDir = temporaryFolder.newFolder("desktop-run-evidence").toPath();
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    String previousRunWindowEvidenceDir = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, dedicatedEvidenceDir.toString());
    System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, legacyEvidenceDir.toString());
    try {
      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          new JPanel(),
          new JPanel(),
          new JPanel(),
          true);
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
      restoreRunWindowEvidenceDirProperty(previousRunWindowEvidenceDir);
    }

    assertFalse(Files.exists(legacyEvidenceDir.resolve("desktop-run-render-affordance.json")));
    assertTrue(Files.size(dedicatedEvidenceDir.resolve("desktop-run-render-affordance.json")) > 0);
  }

  @Test
  public void evidenceDirResolverFallsBackToLegacyPropertyWhenDedicatedPropertyIsMissingOrBlank() throws Exception {
    Path legacyEvidenceDir = temporaryFolder.newFolder("legacy-run-window-evidence").toPath();
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    String previousRunWindowEvidenceDir = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    System.clearProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, legacyEvidenceDir.toString());
    try {
      assertEquals(legacyEvidenceDir.toString(), EatmeDesktopRunExecutionEvidence.evidenceDirProperty());

      System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, "   ");

      assertEquals(legacyEvidenceDir.toString(), EatmeDesktopRunExecutionEvidence.evidenceDirProperty());
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
      restoreRunWindowEvidenceDirProperty(previousRunWindowEvidenceDir);
    }
  }

  @Test
  public void evidenceDirResolverPrefersDedicatedDesktopRunEvidenceProperty() throws Exception {
    Path legacyEvidenceDir = temporaryFolder.newFolder("legacy-run-window-evidence").toPath();
    Path dedicatedEvidenceDir = temporaryFolder.newFolder("desktop-run-evidence").toPath();
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    String previousRunWindowEvidenceDir = System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, dedicatedEvidenceDir.toString());
    System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, legacyEvidenceDir.toString());
    try {
      assertEquals(dedicatedEvidenceDir.toString(), EatmeDesktopRunExecutionEvidence.evidenceDirProperty());
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
      restoreRunWindowEvidenceDirProperty(previousRunWindowEvidenceDir);
    }
  }

  @Test(expected = NullPointerException.class)
  public void renderTargetAffordanceRecorderRequiresRenderTargetComponent() {
    expectNullPointerForRenderAffordance(null, new JPanel(), new JPanel());
  }

  @Test(expected = NullPointerException.class)
  public void renderTargetAffordanceRecorderRequiresRenderPanelComponent() {
    expectNullPointerForRenderAffordance(new JPanel(), null, new JPanel());
  }

  @Test(expected = NullPointerException.class)
  public void renderTargetAffordanceRecorderRequiresRunViewComponent() {
    expectNullPointerForRenderAffordance(new JPanel(), new JPanel(), null);
  }

  @Test
  public void renderTargetAffordanceRecorderKeepsRunBehaviorWhenEvidenceDirectoryCannotBeWritten() throws Exception {
    Path evidenceDirFile = temporaryFolder.newFile("not-a-directory").toPath();
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    Level previousLogLevel = Logger.getLevel();
    System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDirFile.toString());
    try {
      Logger.setLevel(Level.OFF);
      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          new JPanel(),
          new JPanel(),
          new JPanel(),
          false);
    } finally {
      Logger.setLevel(previousLogLevel);
      restoreEvidenceDirProperty(previousEvidenceDir);
    }

    assertTrue(Files.isRegularFile(evidenceDirFile));
  }

  private static JPanel namedPanel(String name) {
    JPanel panel = new JPanel();
    panel.setName(name);
    return panel;
  }

  private static void expectNullPointerForRenderAffordance(Component renderTargetComponent, Component renderPanelComponent, Component runViewComponent) {
    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    System.clearProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          renderTargetComponent,
          renderPanelComponent,
          runViewComponent,
          false);
    } finally {
      restoreEvidenceDirProperty(previousEvidenceDir);
    }
  }

  private static void restoreEvidenceDirProperty(String previousEvidenceDir) {
    if (previousEvidenceDir == null) {
      System.clearProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    } else {
      System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
    }
  }

  private static void restoreRunWindowEvidenceDirProperty(String previousEvidenceDir) {
    if (previousEvidenceDir == null) {
      System.clearProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    } else {
      System.setProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY, previousEvidenceDir);
    }
  }

  private static void assertNoField(String json, String fieldName) {
    assertFalse(json, json.contains("\"" + fieldName + "\":"));
  }
}
