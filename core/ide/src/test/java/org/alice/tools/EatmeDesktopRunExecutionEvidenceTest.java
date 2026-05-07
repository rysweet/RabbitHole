package org.alice.tools;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
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
    Path pixelObservationArtifact = evidenceDir.resolve("desktop-run-pixel-observation.json");
    Path nextActionArtifact = evidenceDir.resolve("desktop-first-lesson-next-action.json");
    Path saveMenuActionTargetArtifact = evidenceDir.resolve("desktop-save-menu-action-target.json");
    Path statusSummaryArtifact = evidenceDir.resolve("desktop-run-status-summary.json");
    assertTrue(Files.size(artifact) > 0);
    assertTrue(Files.size(pixelBoundaryArtifact) > 0);
    assertTrue(Files.size(pixelObservationArtifact) > 0);
    assertTrue(Files.size(nextActionArtifact) > 0);
    assertTrue(Files.size(saveMenuActionTargetArtifact) > 0);
    assertTrue(Files.size(statusSummaryArtifact) > 0);
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
    assertNoField(pixelBoundaryJson, "x");
    assertNoField(pixelBoundaryJson, "y");
    assertNoField(pixelBoundaryJson, "width");
    assertNoField(pixelBoundaryJson, "height");
    assertNoField(pixelBoundaryJson, "bounds");
    assertNoField(pixelBoundaryJson, "color");
    assertNoField(pixelBoundaryJson, "pixel");
    assertNoField(pixelBoundaryJson, "pixels");
    assertNoField(pixelBoundaryJson, "screenshot");
    assertNoField(pixelBoundaryJson, "screenLocation");
    assertNoField(pixelBoundaryJson, "mousePosition");

    String pixelObservationJson = Files.readString(pixelObservationArtifact);
    assertTrue(pixelObservationJson,
        pixelObservationJson.contains("\"schema_version\": \"eatme.alice-desktop-run-pixel-observation/v1\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"status\": \"blocked\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"source\": \"desktop_run_render_target_attachment\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"component_state\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetDisplayable\": false"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetShowing\": false"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetWidth\": 0"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetHeight\": 0"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"blocker\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"render_target_not_displayable\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"render_target_not_showing\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"render_target_has_no_positive_size\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"details\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"observed\": \"renderTargetDisplayable=false\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"required\": \"renderTargetDisplayable=true\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"observed\": \"renderTargetShowing=false\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"required\": \"renderTargetShowing=true\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"observed\": \"renderTargetWidth=0, renderTargetHeight=0\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"required\": \"renderTargetWidth>0 and renderTargetHeight>0\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("desktop world execution"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("visible rendering correctness"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("desktop save-menu completion"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("full lesson flow"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("grading"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("creative assessment"));
    assertNoField(pixelObservationJson, "mousePosition");

    String nextActionJson = Files.readString(nextActionArtifact);
    assertTrue(nextActionJson,
        nextActionJson.contains("\"schema_version\": \"eatme.alice-desktop-first-lesson-next-action/v1\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"status\": \"blocked\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"source\": \"desktop_run_render_target_attachment\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"evaluated_after\": \"desktop-run-pixel-observation.json\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"reporting_summary\""));
    assertTrue(nextActionJson,
        nextActionJson.contains("Run window attachment evidence was recorded in desktop-run-render-affordance.json."));
    assertTrue(nextActionJson,
        nextActionJson.contains("Read desktop-run-pixel-observation.json before reporting whether desktop pixels were sampled."));
    assertTrue(nextActionJson, nextActionJson.contains("\"next_action_status\": \"blocked\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"missing_evidence\""));
    assertTrue(nextActionJson, nextActionJson.contains("desktop Save menu readiness or invocation result"));
    assertTrue(nextActionJson, nextActionJson.contains("code editor/procedure action readiness or invocation result"));
    assertTrue(nextActionJson, nextActionJson.contains("\"candidate_actions\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"desktop_save_menu_action\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"desktop_code_editor_or_procedure_action\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"blocker\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"desktop_save_menu_action_not_bound\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"procedure_editor_action_not_bound\""));
    assertTrue(nextActionJson, nextActionJson.contains("\"no_ui_action_invoker_at_run_render_attachment\""));
    assertTrue(nextActionJson, nextActionJson.contains("stable desktop Save command/menu target plus invocation result"));
    assertTrue(nextActionJson, nextActionJson.contains("stable code editor/procedure action target plus invocation result"));
    assertTrue(nextActionJson, nextActionJson.contains("\"requiresNextEvidence\""));
    assertTrue(nextActionJson, nextActionJson.contains("desktop Save menu readiness or invocation artifact"));
    assertTrue(nextActionJson, nextActionJson.contains("code editor/procedure action readiness or invocation artifact"));
    assertTrue(nextActionJson, nextActionJson.contains("full Alice UI automation"));
    assertTrue(nextActionJson, nextActionJson.contains("desktop save-menu completion"));
    assertTrue(nextActionJson, nextActionJson.contains("code editor/procedure action completion"));
    assertTrue(nextActionJson, nextActionJson.contains("first-lesson completion"));
    assertTrue(nextActionJson, nextActionJson.contains("grading"));
    assertTrue(nextActionJson, nextActionJson.contains("creative assessment"));

    String saveMenuActionTargetJson = Files.readString(saveMenuActionTargetArtifact);
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("\"schema_version\": \"eatme.alice-desktop-save-menu-action-target/v1\""));
    assertTrue(saveMenuActionTargetJson, saveMenuActionTargetJson.contains("\"status\": \"blocked\""));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("\"source\": \"desktop_run_render_target_attachment\""));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("org.alice.ide.croquet.models.menubar.FileMenuModel#createModels"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("org.alice.ide.croquet.models.projecturi.SaveProjectOperation.getInstance()"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("SaveProjectOperation.getInstance().getMenuItemPrepModel()"));
    assertTrue(saveMenuActionTargetJson, saveMenuActionTargetJson.contains("44ffba8a-3fb3-4cb5-97b6-55cd93c88e9d"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("desktop_save_menu_owner_not_available_at_render_attachment"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("desktop_save_project_operation_not_invoked"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("desktop_save_menu_readiness_not_observed"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("FileMenuModel or ProjectDocumentFrame evidence"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("SaveProjectOperation.getInstance() invocation attempt"));
    assertTrue(saveMenuActionTargetJson, saveMenuActionTargetJson.contains("\"requiresNextEvidence\""));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("desktop File menu artifact from FileMenuModel"));
    assertTrue(saveMenuActionTargetJson,
        saveMenuActionTargetJson.contains("desktop SaveProjectOperation.getInstance() readiness"));
    assertTrue(saveMenuActionTargetJson, saveMenuActionTargetJson.contains("desktop save-menu completion"));
    assertFalse(saveMenuActionTargetJson, saveMenuActionTargetJson.contains("procedure"));
    assertFalse(saveMenuActionTargetJson, saveMenuActionTargetJson.contains("code editor"));

    String statusSummaryJson = Files.readString(statusSummaryArtifact);
    assertTrue(statusSummaryJson,
        statusSummaryJson.contains("\"schema_version\": \"eatme.alice-desktop-run-status-summary/v1\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"status\": \"partial\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"source\": \"desktop_run_render_target_attachment\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"pixel_observation_status\": \"blocked\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact_statuses\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"evidence_present\": true"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact\": \"desktop-run-render-affordance.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact\": \"desktop-run-pixel-boundary.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact\": \"desktop-run-pixel-observation.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact\": \"desktop-first-lesson-next-action.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact\": \"desktop-save-menu-action-target.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"missing_evidence\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"artifact\": \"procedure-ui-action-no-go.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"source_tool\": \"tools/eatme-edit-procedure\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"evidence_present\": false"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("SaveProjectOperation invocation result"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"reporting_note\""));
    assertTrue(statusSummaryJson,
        statusSummaryJson.contains("Report desktop-run-pixel-observation.json as blocked until its blocker details are resolved or separate manual evidence is supplied."));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"run_attachment_observed\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"pixel_observation\": \"desktop-run-pixel-observation.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"next_action\": \"desktop-first-lesson-next-action.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"save_menu_action_target\": \"desktop-save-menu-action-target.json\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"exact_next_user_action\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("Open desktop-run-pixel-observation.json"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("Open desktop-first-lesson-next-action.json"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("\"remaining_unproven_behavior\""));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("visible rendering correctness"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("desktop save-menu completion"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("full Alice UI automation"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("first-lesson completion"));
    assertTrue(statusSummaryJson, statusSummaryJson.contains("learner-world grading"));
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
  public void writesObservedPixelArtifactWhenDesktopCaptureIsAvailable() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless());
    Path evidenceDir = temporaryFolder.newFolder("observed-pixel").toPath();
    JPanel renderTargetComponent = new JPanel();
    renderTargetComponent.setBackground(Color.BLUE);
    renderTargetComponent.setPreferredSize(new Dimension(24, 24));
    JFrame frame = new JFrame("Run pixel observation test");
    frame.getContentPane().add(renderTargetComponent, BorderLayout.CENTER);
    frame.pack();

    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      frame.setVisible(true);
      System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());

      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          renderTargetComponent,
          renderTargetComponent,
          frame.getContentPane(),
          false);
    } finally {
      frame.dispose();
      restoreEvidenceDirProperty(previousEvidenceDir);
    }

    Path pixelObservationArtifact = evidenceDir.resolve("desktop-run-pixel-observation.json");
    String pixelObservationJson = Files.readString(pixelObservationArtifact);
    assumeTrue(pixelObservationJson, pixelObservationJson.contains("\"status\": \"observed\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"captureTarget\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"role\": \"render_target_component\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"screenshot\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"file\": \"desktop-run-render-target.png\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetWidth\": 24"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetHeight\": 24"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"captureArea\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"coordinateSystem\": \"screen\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"sample\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"coordinateSystem\": \"screenshot\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"argb\": \"0x"));
    assertTrue(Files.size(evidenceDir.resolve("desktop-run-render-target.png")) > 0);
  }

  @Test
  public void writesObservedPixelArtifactFromRenderPanelWhenRawRenderTargetIsNotShowing() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless());
    Path evidenceDir = temporaryFolder.newFolder("observed-render-panel-pixel").toPath();
    JPanel rawRenderTargetComponent = new JPanel();
    JPanel renderPanelComponent = new JPanel();
    renderPanelComponent.setBackground(Color.BLUE);
    renderPanelComponent.setPreferredSize(new Dimension(24, 24));
    JFrame frame = new JFrame("Run panel pixel observation test");
    frame.getContentPane().add(renderPanelComponent, BorderLayout.CENTER);
    frame.pack();

    String previousEvidenceDir = System.getProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY);
    try {
      frame.setVisible(true);
      System.setProperty(EatmeDesktopRunExecutionEvidence.EVIDENCE_DIR_PROPERTY, evidenceDir.toString());

      EatmeDesktopRunExecutionEvidence.recordRenderTargetAttached(
          rawRenderTargetComponent,
          renderPanelComponent,
          frame.getContentPane(),
          false);
    } finally {
      frame.dispose();
      restoreEvidenceDirProperty(previousEvidenceDir);
    }

    Path pixelObservationArtifact = evidenceDir.resolve("desktop-run-pixel-observation.json");
    String pixelObservationJson = Files.readString(pixelObservationArtifact);
    assumeTrue(pixelObservationJson, pixelObservationJson.contains("\"status\": \"observed\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"role\": \"render_panel_component\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetDisplayable\": false"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderTargetShowing\": false"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderPanelDisplayable\": true"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"renderPanelShowing\": true"));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"screenshot\""));
    assertTrue(pixelObservationJson, pixelObservationJson.contains("\"sample\""));
    assertTrue(Files.size(evidenceDir.resolve("desktop-run-render-target.png")) > 0);
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
