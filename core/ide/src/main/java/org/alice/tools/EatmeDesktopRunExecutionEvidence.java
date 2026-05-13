package org.alice.tools;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.alice.stageide.program.RunProgramContext;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.virtualmachine.events.CountLoopIterationEvent;
import org.lgna.project.virtualmachine.events.EachInTogetherItemEvent;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.ForEachLoopIterationEvent;
import org.lgna.project.virtualmachine.events.StatementExecutionEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;
import org.lgna.project.virtualmachine.events.WhileLoopIterationEvent;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EatmeDesktopRunExecutionEvidence {
  public static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.desktopRunExecutionEvidenceDir";
  public static final String DESKTOP_RUN_EXECUTION_ARTIFACT = "desktop-run-execution.json";
  public static final String DESKTOP_RUN_RUNTIME_LOG = "desktop-run-runtime.log";
  public static final String DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT = "desktop-run-render-affordance.json";
  public static final String DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT = "desktop-run-pixel-boundary.json";
  public static final String DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT = "desktop-run-pixel-observation.json";
  public static final String DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT = "desktop-first-lesson-next-action.json";
  public static final String DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT = "desktop-save-menu-action-target.json";
  public static final String DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT = "desktop-run-status-summary.json";
  public static final String DESKTOP_RUN_EXECUTION_GAP_REPORT_ARTIFACT =
      "desktop-run-execution-gap-report.json";
  private static final int MAX_RECORDED_EVENTS = 200;
  private static final String RENDER_AFFORDANCE_CLAIM =
      "A Run view attachment signal was observed.";
  private static final String FULL_WORLD_EXECUTION_BLOCKER_REASON =
      "Missing deterministic proof that the Alice world actually advances through full runtime execution, "
          + "not merely that Run-window evidence artifacts exist.";

  private EatmeDesktopRunExecutionEvidence() {
  }

  public static Recorder install(RunProgramContext context, NamedUserType programType) {
    String evidenceDir = evidenceDirProperty();
    if (evidenceDir == null || evidenceDir.isBlank() || context == null) {
      return Recorder.disabled();
    }
    try {
      Recorder recorder = new Recorder(Path.of(evidenceDir), EatmeRunWindowEvidence.typeName(programType));
      context.getVirtualMachine().addVirtualMachineListener(recorder);
      recorder.recordLifecycle("listener-installed");
      return recorder;
    } catch (InvalidPathException | SecurityException ex) {
      Logger.throwable(ex, "eatme desktop Run execution evidence setup failed: " + evidenceDir);
      return Recorder.disabled();
    }
  }

  public static void recordRenderTargetAttached(
      Component renderTargetComponent,
      Component renderPanelComponent,
      Component runViewComponent,
      boolean controlPanelAttached) {
    Objects.requireNonNull(renderTargetComponent, "renderTargetComponent");
    Objects.requireNonNull(renderPanelComponent, "renderPanelComponent");
    Objects.requireNonNull(runViewComponent, "runViewComponent");

    String evidenceDir = evidenceDirProperty();
    if (evidenceDir == null || evidenceDir.isBlank()) {
      return;
    }
    try {
      writeRenderTargetAttached(
          Path.of(evidenceDir),
          renderTargetComponent,
          renderPanelComponent,
          runViewComponent,
          controlPanelAttached);
    } catch (IOException | InvalidPathException | SecurityException ex) {
      Logger.throwable(ex, "eatme desktop Run render-affordance evidence write failed: " + evidenceDir);
    }
  }

  static String evidenceDirProperty() {
    String evidenceDir = System.getProperty(EVIDENCE_DIR_PROPERTY);
    if (evidenceDir == null || evidenceDir.isBlank()) {
      return System.getProperty(EatmeRunWindowEvidence.EVIDENCE_DIR_PROPERTY);
    }
    return evidenceDir;
  }

  // =========================================================================
  // Forwarding delegates — preserve existing package-private API for tests
  // =========================================================================

  static Path writeDesktopRunExecution(
      Path evidenceDir,
      String programTypeName,
      boolean activeSceneInvokeStarted,
      boolean activeSceneInvokeReturned,
      int executingStatementCount,
      int executedStatementCount,
      String latestEvent,
      List<String> events) throws IOException {
    return EatmeEvidenceWriter.writeDesktopRunExecution(
        evidenceDir, programTypeName,
        activeSceneInvokeStarted, activeSceneInvokeReturned,
        executingStatementCount, executedStatementCount,
        latestEvent, events);
  }

  static Path writeDesktopRunExecutionGapReport(
      Path evidenceDir,
      List<String> evidenceArtifacts,
      String blockerReason) throws IOException {
    return EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(
        evidenceDir, evidenceArtifacts, blockerReason);
  }

  static void validateDesktopRunExecutionGapReport(
      List<String> evidenceArtifacts,
      String blockerReason,
      List<String> doesNotClaim) {
    EatmeEvidenceWriter.validateDesktopRunExecutionGapReport(
        evidenceArtifacts, blockerReason, doesNotClaim);
  }

  // =========================================================================
  // Recorder — VM listener inner class
  // =========================================================================

  public static final class Recorder implements VirtualMachineListener {
    private final Path evidenceDir;
    private final String programTypeName;
    private final List<String> events = new ArrayList<>();
    private int executingStatementCount;
    private int executedStatementCount;
    private String latestEvent = "";
    private boolean activeSceneInvokeStarted;
    private boolean activeSceneInvokeReturned;
    private boolean disabled;

    private Recorder(Path evidenceDir, String programTypeName) {
      this.evidenceDir = evidenceDir;
      this.programTypeName = programTypeName;
    }

    private Recorder() {
      this.evidenceDir = null;
      this.programTypeName = "";
      this.disabled = true;
    }

    static Recorder disabled() {
      return new Recorder();
    }

    public synchronized void recordActiveSceneInvokeStarted() {
      activeSceneInvokeStarted = true;
      recordLifecycle("set-active-scene-invoked");
    }

    public synchronized void recordActiveSceneInvokeReturned() {
      activeSceneInvokeReturned = true;
      recordLifecycle("set-active-scene-returned");
    }

    public synchronized void recordActiveSceneInvokeFailed(Throwable throwable) {
      latestEvent = "set-active-scene-failed:" + throwable.getClass().getSimpleName();
      recordEvent(latestEvent);
      writeArtifacts();
    }

    @Override
    public synchronized void statementExecuting(StatementExecutionEvent statementExecutionEvent) {
      executingStatementCount++;
      recordStatement("executing", statementExecutionEvent);
    }

    @Override
    public synchronized void statementExecuted(StatementExecutionEvent statementExecutionEvent) {
      executedStatementCount++;
      recordStatement("executed", statementExecutionEvent);
    }

    @Override
    public void whileLoopIterating(WhileLoopIterationEvent whileLoopIterationEvent) {
    }

    @Override
    public void whileLoopIterated(WhileLoopIterationEvent whileLoopIterationEvent) {
    }

    @Override
    public void countLoopIterating(CountLoopIterationEvent countLoopIterationEvent) {
    }

    @Override
    public void countLoopIterated(CountLoopIterationEvent countLoopIterationEvent) {
    }

    @Override
    public void forEachLoopIterating(ForEachLoopIterationEvent forEachLoopIterationEvent) {
    }

    @Override
    public void forEachLoopIterated(ForEachLoopIterationEvent forEachLoopIterationEvent) {
    }

    @Override
    public void eachInTogetherItemExecuting(EachInTogetherItemEvent eachInTogetherItemEvent) {
    }

    @Override
    public void eachInTogetherItemExecuted(EachInTogetherItemEvent eachInTogetherItemEvent) {
    }

    @Override
    public void expressionEvaluated(ExpressionEvaluationEvent expressionEvaluationEvent) {
    }

    private void recordLifecycle(String event) {
      latestEvent = event;
      recordEvent(event);
      writeArtifacts();
    }

    private void recordStatement(String phase, StatementExecutionEvent statementExecutionEvent) {
      String statementType = statementExecutionEvent.getStatement() != null
          ? statementExecutionEvent.getStatement().getClass().getSimpleName()
          : "";
      latestEvent = phase + ":" + statementType;
      recordEvent(latestEvent);
      if (executingStatementCount == 1 || executedStatementCount == 1) {
        writeArtifacts();
      }
    }

    private void recordEvent(String event) {
      if (events.size() < MAX_RECORDED_EVENTS) {
        events.add(event);
      } else if (events.size() == MAX_RECORDED_EVENTS) {
        events.add("event-log-truncated");
      }
    }

    private void writeArtifacts() {
      if (disabled) {
        return;
      }
      try {
        EatmeEvidenceWriter.writeDesktopRunExecution(
            evidenceDir,
            programTypeName,
            activeSceneInvokeStarted,
            activeSceneInvokeReturned,
            executingStatementCount,
            executedStatementCount,
            latestEvent,
            events);
      } catch (IOException | InvalidPathException | SecurityException ex) {
        Logger.throwable(ex, "eatme desktop Run execution evidence write failed: " + evidenceDir);
      }
    }
  }

  // =========================================================================
  // Render-target attachment orchestration
  // =========================================================================

  static Path writeRenderTargetAttached(
      Path evidenceDir,
      Component renderTargetComponent,
      Component renderPanelComponent,
      Component runViewComponent,
      boolean controlPanelAttached) throws IOException {
    // Null checks performed by the public recordRenderTargetAttached entry point.
    Files.createDirectories(evidenceDir);
    Path artifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT);
    Path pixelBoundaryArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT);
    Path pixelObservationArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT);
    Path nextActionArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT);
    Path saveMenuActionTargetArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT);
    Path statusSummaryArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT);

    EatmeEvidenceWriter.writeStringAtomically(artifact, renderAffordanceJson(
        renderTargetComponent, renderPanelComponent, runViewComponent, controlPanelAttached));
    EatmeEvidenceWriter.requireNonEmptyArtifact(artifact, "desktop Run render-affordance artifact");

    EatmeEvidenceWriter.writeStringAtomically(pixelBoundaryArtifact, pixelBoundaryJson());
    EatmeEvidenceWriter.requireNonEmptyArtifact(pixelBoundaryArtifact, "desktop Run pixel boundary artifact");

    PixelObservation pixelObservation = writePixelObservation(
        pixelObservationArtifact, evidenceDir,
        renderTargetComponent, renderPanelComponent, runViewComponent);
    EatmeEvidenceWriter.requireNonEmptyArtifact(pixelObservationArtifact, "desktop Run pixel observation artifact");

    EatmeEvidenceWriter.writeFirstLessonNextActionContract(nextActionArtifact);
    EatmeEvidenceWriter.requireNonEmptyArtifact(nextActionArtifact, "desktop first-lesson next-action artifact");

    EatmeEvidenceWriter.writeSaveMenuActionTargetNoGo(saveMenuActionTargetArtifact);
    EatmeEvidenceWriter.requireNonEmptyArtifact(saveMenuActionTargetArtifact, "desktop Save menu action-target artifact");

    EatmeEvidenceWriter.writeRunStatusSummary(statusSummaryArtifact, pixelObservation);
    EatmeEvidenceWriter.requireNonEmptyArtifact(statusSummaryArtifact, "desktop Run status summary artifact");

    EatmeEvidenceWriter.writeDesktopRunExecutionGapReport(
        evidenceDir,
        EatmeEvidenceWriter.REQUIRED_EXECUTION_GAP_EVIDENCE_ARTIFACTS,
        FULL_WORLD_EXECUTION_BLOCKER_REASON);
    return artifact;
  }

  private static String renderAffordanceJson(
      Component renderTargetComponent,
      Component renderPanelComponent,
      Component runViewComponent,
      boolean controlPanelAttached) {
    return "{\n"
        + "  \"evidenceKind\": \"desktop_run_render_affordance\",\n"
        + "  \"renderTargetAttachedToRunView\": true,\n"
        + "  \"renderTargetComponentClass\": \"" + EatmeRunWindowEvidence.escapeJson(EatmeWindowDetector.componentClassName(renderTargetComponent)) + "\",\n"
        + "  \"renderTargetComponentName\": \"" + EatmeRunWindowEvidence.escapeJson(EatmeWindowDetector.componentName(renderTargetComponent)) + "\",\n"
        + "  \"renderTargetDisplayable\": " + renderTargetComponent.isDisplayable() + ",\n"
        + "  \"renderTargetShowing\": " + renderTargetComponent.isShowing() + ",\n"
        + "  \"renderPanelComponentClass\": \"" + EatmeRunWindowEvidence.escapeJson(EatmeWindowDetector.componentClassName(renderPanelComponent)) + "\",\n"
        + "  \"runViewComponentClass\": \"" + EatmeRunWindowEvidence.escapeJson(EatmeWindowDetector.componentClassName(runViewComponent)) + "\",\n"
        + "  \"runViewComponentCountAfterAttach\": " + EatmeWindowDetector.childComponentCount(runViewComponent) + ",\n"
        + "  \"controlPanelAttached\": " + controlPanelAttached + ",\n"
        + "  \"claim\": \"" + RENDER_AFFORDANCE_CLAIM + "\",\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"visible rendering\",\n"
        + "    \"graphics or OpenGL rendering success\",\n"
        + "    \"pixel output validation\",\n"
        + "    \"screenshot validation\",\n"
        + "    \"end-to-end UI correctness\",\n"
        + "    \"lesson completion\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String pixelBoundaryJson() {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-desktop-run-pixel-boundary/v1\",\n"
        + "  \"status\": \"not_observed\",\n"
        + "  \"reason\": \"Run view attachment was observed, but this Alice-side signal does not inspect screenshots or pixel output.\",\n"
        + "  \"requiresSeparateEvidence\": [\n"
        + "    \"non-empty desktop screenshot captured after Run-window attachment\",\n"
        + "    \"pixel or image validation performed by an outside-in harness\"\n"
        + "  ],\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"visible rendering\",\n"
        + "    \"pixel output validation\",\n"
        + "    \"screenshot validation\",\n"
        + "    \"lesson completion\",\n"
        + "    \"grading\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static PixelObservation writePixelObservation(
      Path artifact,
      Path evidenceDir,
      Component renderTargetComponent,
      Component renderPanelComponent,
      Component runViewComponent) throws IOException {
    PixelObservation observation = EatmeScreenshotCapture.observePixel(
        evidenceDir, renderTargetComponent, renderPanelComponent);
    EatmeEvidenceWriter.writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-run-pixel-observation/v1\",\n"
            + "  \"status\": \"" + observation.status + "\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"claim\": \"" + EatmeRunWindowEvidence.escapeJson(observation.claim) + "\",\n"
            + "  \"component_state\": {\n"
            + "    \"graphicsEnvironmentHeadless\": " + GraphicsEnvironment.isHeadless() + ",\n"
            + "    \"renderTargetDisplayable\": " + renderTargetComponent.isDisplayable() + ",\n"
            + "    \"renderTargetShowing\": " + renderTargetComponent.isShowing() + ",\n"
            + "    \"renderTargetWidth\": " + renderTargetComponent.getWidth() + ",\n"
            + "    \"renderTargetHeight\": " + renderTargetComponent.getHeight() + ",\n"
            + "    \"renderPanelDisplayable\": " + renderPanelComponent.isDisplayable() + ",\n"
            + "    \"renderPanelShowing\": " + renderPanelComponent.isShowing() + ",\n"
            + "    \"renderPanelWidth\": " + renderPanelComponent.getWidth() + ",\n"
            + "    \"renderPanelHeight\": " + renderPanelComponent.getHeight() + ",\n"
            + "    \"runViewDisplayable\": " + runViewComponent.isDisplayable() + ",\n"
            + "    \"runViewShowing\": " + runViewComponent.isShowing() + ",\n"
            + "    \"runViewWidth\": " + runViewComponent.getWidth() + ",\n"
            + "    \"runViewHeight\": " + runViewComponent.getHeight() + "\n"
            + "  },\n"
            + observation.detailJson
            + "  \"doesNotClaim\": [\n"
            + "    \"desktop world execution\",\n"
            + "    \"visible rendering correctness\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"full lesson flow\",\n"
            + "    \"grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
    return observation;
  }
}
