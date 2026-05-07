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

import java.awt.AWTException;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;

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
  private static final String DESKTOP_RUN_RENDER_TARGET_SCREENSHOT = "desktop-run-render-target.png";
  private static final int MAX_RECORDED_EVENTS = 200;
  private static final String RENDER_AFFORDANCE_CLAIM =
      "A Run view attachment signal was observed.";

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
        writeDesktopRunExecution(
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

  static Path writeDesktopRunExecution(
      Path evidenceDir,
      String programTypeName,
      boolean activeSceneInvokeStarted,
      boolean activeSceneInvokeReturned,
      int executingStatementCount,
      int executedStatementCount,
      String latestEvent,
      List<String> events) throws IOException {
    Files.createDirectories(evidenceDir);
    Path artifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_EXECUTION_ARTIFACT);
    Path runtimeLog = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_RUNTIME_LOG);
    writeStringAtomically(runtimeLog, runtimeLog(programTypeName, events));
    requireNonEmptyArtifact(runtimeLog, "desktop Run runtime log");
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-run-execution/v1\",\n"
            + "  \"status\": \"" + (executingStatementCount > 0 ? "statement_execution_observed" : "preparing") + "\",\n"
            + "  \"execution_mode\": \"desktop_run_frame_vm_listener\",\n"
            + "  \"program_type\": \"" + EatmeRunWindowEvidence.escapeJson(programTypeName) + "\",\n"
            + "  \"active_scene_invoke_started\": " + activeSceneInvokeStarted + ",\n"
            + "  \"active_scene_invoke_returned\": " + activeSceneInvokeReturned + ",\n"
            + "  \"executing_statement_count\": " + executingStatementCount + ",\n"
            + "  \"executed_statement_count\": " + executedStatementCount + ",\n"
            + "  \"latest_event\": \"" + EatmeRunWindowEvidence.escapeJson(latestEvent) + "\",\n"
            + "  \"runtime_log\": \"" + DESKTOP_RUN_RUNTIME_LOG + "\"\n"
            + "}\n");
    requireNonEmptyArtifact(artifact, "desktop Run execution artifact");
    return artifact;
  }

  static Path writeRenderTargetAttached(
      Path evidenceDir,
      Component renderTargetComponent,
      Component renderPanelComponent,
      Component runViewComponent,
      boolean controlPanelAttached) throws IOException {
    Objects.requireNonNull(renderTargetComponent, "renderTargetComponent");
    Objects.requireNonNull(renderPanelComponent, "renderPanelComponent");
    Objects.requireNonNull(runViewComponent, "runViewComponent");

    Files.createDirectories(evidenceDir);
    Path artifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT);
    Path pixelBoundaryArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_PIXEL_BOUNDARY_ARTIFACT);
    Path pixelObservationArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT);
    Path nextActionArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT);
    Path saveMenuActionTargetArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT);
    Path statusSummaryArtifact = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_STATUS_SUMMARY_ARTIFACT);
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"evidenceKind\": \"desktop_run_render_affordance\",\n"
            + "  \"renderTargetAttachedToRunView\": true,\n"
            + "  \"renderTargetComponentClass\": \"" + componentClassName(renderTargetComponent) + "\",\n"
            + "  \"renderTargetComponentName\": \"" + EatmeRunWindowEvidence.escapeJson(componentName(renderTargetComponent)) + "\",\n"
            + "  \"renderTargetDisplayable\": " + renderTargetComponent.isDisplayable() + ",\n"
            + "  \"renderTargetShowing\": " + renderTargetComponent.isShowing() + ",\n"
            + "  \"renderPanelComponentClass\": \"" + componentClassName(renderPanelComponent) + "\",\n"
            + "  \"runViewComponentClass\": \"" + componentClassName(runViewComponent) + "\",\n"
            + "  \"runViewComponentCountAfterAttach\": " + childComponentCount(runViewComponent) + ",\n"
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
            + "}\n");
    requireNonEmptyArtifact(artifact, "desktop Run render-affordance artifact");
    writeStringAtomically(
        pixelBoundaryArtifact,
        "{\n"
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
            + "}\n");
    requireNonEmptyArtifact(pixelBoundaryArtifact, "desktop Run pixel boundary artifact");
    PixelObservation pixelObservation = writePixelObservation(
        pixelObservationArtifact,
        evidenceDir,
        renderTargetComponent,
        renderPanelComponent,
        runViewComponent);
    requireNonEmptyArtifact(pixelObservationArtifact, "desktop Run pixel observation artifact");
    writeFirstLessonNextActionContract(nextActionArtifact);
    requireNonEmptyArtifact(nextActionArtifact, "desktop first-lesson next-action artifact");
    writeSaveMenuActionTargetNoGo(saveMenuActionTargetArtifact);
    requireNonEmptyArtifact(saveMenuActionTargetArtifact, "desktop Save menu action-target artifact");
    writeRunStatusSummary(statusSummaryArtifact, pixelObservation);
    requireNonEmptyArtifact(statusSummaryArtifact, "desktop Run status summary artifact");
    return artifact;
  }

  private static void writeFirstLessonNextActionContract(Path artifact) throws IOException {
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-first-lesson-next-action/v1\",\n"
            + "  \"status\": \"blocked\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"evaluated_after\": \"" + DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT + "\",\n"
            + "  \"candidate_actions\": [\n"
            + "    \"desktop_save_menu_action\",\n"
            + "    \"desktop_code_editor_or_procedure_action\"\n"
            + "  ],\n"
            + "  \"blocker\": {\n"
            + "    \"reason\": \"The Run render attachment seam does not receive or invoke a stable desktop Save menu or code editor/procedure action target.\",\n"
            + "    \"codes\": [\n"
            + "      \"desktop_save_menu_action_not_bound\",\n"
            + "      \"procedure_editor_action_not_bound\",\n"
            + "      \"no_ui_action_invoker_at_run_render_attachment\"\n"
            + "    ],\n"
            + "    \"details\": [\n"
            + "      {\n"
            + "        \"observed\": \"recordRenderTargetAttached receives render target, render panel, Run view, and control-panel attachment state only\",\n"
            + "        \"required\": \"stable desktop Save command/menu target plus invocation result\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"observed\": \"no code editor or procedure operation target is exposed at this seam\",\n"
            + "        \"required\": \"stable code editor/procedure action target plus invocation result\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"requiresNextEvidence\": [\n"
            + "    \"desktop Save menu readiness or invocation artifact from the menu/action owner\",\n"
            + "    \"code editor/procedure action readiness or invocation artifact from the editor/action owner\"\n"
            + "  ],\n"
            + "  \"doesNotClaim\": [\n"
            + "    \"full Alice UI automation\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"code editor/procedure action completion\",\n"
            + "    \"first-lesson completion\",\n"
            + "    \"grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
  }

  private static void writeSaveMenuActionTargetNoGo(Path artifact) throws IOException {
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-save-menu-action-target/v1\",\n"
            + "  \"status\": \"blocked\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"target\": {\n"
            + "    \"menu_owner\": \"org.alice.ide.croquet.models.menubar.FileMenuModel#createModels\",\n"
            + "    \"operation\": \"org.alice.ide.croquet.models.projecturi.SaveProjectOperation.getInstance()\",\n"
            + "    \"menu_item\": \"SaveProjectOperation.getInstance().getMenuItemPrepModel()\",\n"
            + "    \"operation_uuid\": \"44ffba8a-3fb3-4cb5-97b6-55cd93c88e9d\"\n"
            + "  },\n"
            + "  \"blocker\": {\n"
            + "    \"reason\": \"The current evidence seam runs after Run render-target attachment and does not receive the desktop File menu owner, ProjectDocumentFrame, or SaveProjectOperation target needed to prove Save menu readiness or invocation.\",\n"
            + "    \"codes\": [\n"
            + "      \"desktop_save_menu_owner_not_available_at_render_attachment\",\n"
            + "      \"desktop_save_project_operation_not_invoked\",\n"
            + "      \"desktop_save_menu_readiness_not_observed\"\n"
            + "    ],\n"
            + "    \"details\": [\n"
            + "      {\n"
            + "        \"observed\": \"recordRenderTargetAttached receives render target, render panel, Run view, and control-panel attachment state only\",\n"
            + "        \"required\": \"FileMenuModel or ProjectDocumentFrame evidence that SaveProjectOperation.getInstance().getMenuItemPrepModel() is present and enabled\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"observed\": \"no SaveProjectOperation invocation result is emitted by this seam\",\n"
            + "        \"required\": \"SaveProjectOperation.getInstance() invocation attempt with a success, prompt, or blocked result artifact\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"requiresNextEvidence\": [\n"
            + "    \"desktop File menu artifact from FileMenuModel showing SaveProjectOperation.getInstance().getMenuItemPrepModel() is installed\",\n"
            + "    \"desktop SaveProjectOperation.getInstance() readiness or invocation result from the menu/action owner\"\n"
            + "  ],\n"
            + "  \"doesNotClaim\": [\n"
            + "    \"full Alice UI automation\",\n"
            + "    \"visible rendering correctness\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"first-lesson completion\",\n"
            + "    \"grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
  }

  private static void writeRunStatusSummary(Path artifact, PixelObservation pixelObservation) throws IOException {
    writeStringAtomically(
        artifact,
        "{\n"
            + "  \"schema_version\": \"eatme.alice-desktop-run-status-summary/v1\",\n"
            + "  \"status\": \"partial\",\n"
            + "  \"source\": \"desktop_run_render_target_attachment\",\n"
            + "  \"pixel_observation_status\": \"" + pixelObservation.status + "\",\n"
            + "  \"artifact_statuses\": [\n"
            + "    {\n"
            + "      \"artifact\": \"" + DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"present\",\n"
            + "      \"reports\": \"Run view attachment evidence only\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"" + pixelObservation.status + "\",\n"
            + "      \"reports\": \"" + pixelObservationSummary(pixelObservation) + "\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"blocked\",\n"
            + "      \"reports\": \"desktop action evidence still missing\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"artifact\": \"" + DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT + "\",\n"
            + "      \"evidence_present\": true,\n"
            + "      \"status\": \"blocked\",\n"
            + "      \"reports\": \"Save menu readiness or invocation not observed here\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"reporting_note\": \"" + pixelObservationReportingNote(pixelObservation) + "\",\n"
            + "  \"observed_artifacts\": {\n"
            + "    \"run_attachment_observed\": \"" + DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT + "\",\n"
            + "    \"pixel_observation\": \"" + DESKTOP_RUN_PIXEL_OBSERVATION_ARTIFACT + "\",\n"
            + "    \"next_action\": \"" + DESKTOP_FIRST_LESSON_NEXT_ACTION_ARTIFACT + "\",\n"
            + "    \"save_menu_action_target\": \"" + DESKTOP_SAVE_MENU_ACTION_TARGET_ARTIFACT + "\"\n"
            + "  },\n"
            + "  \"exact_next_user_action\": [\n"
            + "    \"Open desktop-run-pixel-observation.json and use its status plus blocker details before claiming desktop pixels were sampled.\",\n"
            + "    \"Open desktop-first-lesson-next-action.json to see which desktop action evidence is still missing.\",\n"
            + "    \"Open desktop-save-menu-action-target.json before claiming Save menu readiness or completion.\"\n"
            + "  ],\n"
            + "  \"remaining_unproven_behavior\": [\n"
            + "    \"visible rendering correctness\",\n"
            + "    \"desktop save-menu completion\",\n"
            + "    \"full Alice UI automation\",\n"
            + "    \"first-lesson completion\",\n"
            + "    \"learner-world grading\",\n"
            + "    \"creative assessment\"\n"
            + "  ]\n"
            + "}\n");
  }

  private static String pixelObservationSummary(PixelObservation pixelObservation) {
    return pixelObservation.isObserved()
        ? "desktop pixel sample observed; inspect the screenshot and sample details before reporting"
        : "desktop pixel sample blocked; inspect blocker details before reporting";
  }

  private static String pixelObservationReportingNote(PixelObservation pixelObservation) {
    return pixelObservation.isObserved()
        ? "Report desktop-run-pixel-observation.json as observed only for pixel sampling, not visible rendering correctness."
        : "Report desktop-run-pixel-observation.json as blocked until its blocker details are resolved or separate manual evidence is supplied.";
  }

  private static PixelObservation writePixelObservation(
      Path artifact,
      Path evidenceDir,
      Component renderTargetComponent,
      Component renderPanelComponent,
      Component runViewComponent) throws IOException {
    PixelObservation observation = observePixel(evidenceDir, renderTargetComponent, renderPanelComponent);
    writeStringAtomically(
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

  private static PixelObservation observePixel(
      Path evidenceDir,
      Component renderTargetComponent,
      Component renderPanelComponent) {
    if (GraphicsEnvironment.isHeadless()) {
      List<BlockerDetail> blockers = new ArrayList<>();
      blockers.add(new BlockerDetail(
          "java_awt_headless",
          "graphicsEnvironmentHeadless=true",
          "graphicsEnvironmentHeadless=false"));
      blockers.addAll(componentReadinessBlockers(renderTargetComponent, "render_target", "renderTarget"));
      if (renderTargetComponent != renderPanelComponent) {
        blockers.addAll(componentReadinessBlockers(renderPanelComponent, "render_panel", "renderPanel"));
      }
      return PixelObservation.blocked(blockers, "");
    }

    PixelObservation renderTargetObservation = observeComponentPixel(
        evidenceDir,
        renderTargetComponent,
        "render_target",
        "renderTarget",
        "render_target_component");
    if (renderTargetObservation.isObserved() || renderTargetComponent == renderPanelComponent) {
      return renderTargetObservation;
    }

    PixelObservation renderPanelObservation = observeComponentPixel(
        evidenceDir,
        renderPanelComponent,
        "render_panel",
        "renderPanel",
        "render_panel_component");
    if (renderPanelObservation.isObserved()) {
      return renderPanelObservation;
    }

    List<BlockerDetail> blockers = new ArrayList<>(renderTargetObservation.blockers);
    blockers.addAll(renderPanelObservation.blockers);
    return PixelObservation.blocked(blockers, firstNonBlank(
        renderTargetObservation.exceptionType,
        renderPanelObservation.exceptionType));
  }

  private static PixelObservation observeComponentPixel(
      Path evidenceDir,
      Component component,
      String blockerPrefix,
      String statePrefix,
      String captureRole) {
    List<BlockerDetail> blockers = componentReadinessBlockers(component, blockerPrefix, statePrefix);
    Point screenLocation = null;
    if (blockers.isEmpty()) {
      try {
        screenLocation = component.getLocationOnScreen();
      } catch (IllegalComponentStateException ex) {
        blockers.add(new BlockerDetail(
            blockerPrefix + "_screen_location_unavailable",
            exceptionObserved(ex),
            captureRole + " screen location available"));
      } catch (SecurityException ex) {
        blockers.add(new BlockerDetail(
            blockerPrefix + "_screen_location_denied",
            exceptionObserved(ex),
            "screen-location access permitted"));
      }
    }

    if (!blockers.isEmpty()) {
      return PixelObservation.blocked(blockers, "");
    }

    try {
      Rectangle captureArea = new Rectangle(
          screenLocation.x,
          screenLocation.y,
          component.getWidth(),
          component.getHeight());
      BufferedImage screenshot = new Robot().createScreenCapture(captureArea);
      if (screenshot.getWidth() <= 0 || screenshot.getHeight() <= 0) {
        return PixelObservation.blocked(List.of(new BlockerDetail(
            blockerPrefix + "_screenshot_has_no_positive_size",
            "screenshotWidth=" + screenshot.getWidth() + ", screenshotHeight=" + screenshot.getHeight(),
            "screenshotWidth>0 and screenshotHeight>0")), "");
      }
      Path screenshotPath = EatmeRunWindowEvidence.artifactPath(evidenceDir, DESKTOP_RUN_RENDER_TARGET_SCREENSHOT);
      writePngAtomically(screenshotPath, screenshot);
      int sampleX = screenshot.getWidth() / 2;
      int sampleY = screenshot.getHeight() / 2;
      int argb = screenshot.getRGB(sampleX, sampleY);
      return PixelObservation.observed(
          captureRole,
          component,
          DESKTOP_RUN_RENDER_TARGET_SCREENSHOT,
          captureArea,
          screenshot.getWidth(),
          screenshot.getHeight(),
          sampleX,
          sampleY,
          argb);
    } catch (AWTException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          "java_awt_robot_unavailable",
          exceptionObserved(ex),
          "java.awt.Robot screen capture available")), ex.getClass().getSimpleName());
    } catch (IOException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_screenshot_write_failed",
          exceptionObserved(ex),
          "desktop-run-render-target.png writable in evidence directory")), ex.getClass().getSimpleName());
    } catch (IllegalArgumentException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_screen_capture_area_invalid",
          exceptionObserved(ex),
          "valid positive screen capture rectangle")), ex.getClass().getSimpleName());
    } catch (SecurityException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_screen_capture_denied",
          exceptionObserved(ex),
          "screen-capture access permitted")), ex.getClass().getSimpleName());
    } catch (RuntimeException ex) {
      return PixelObservation.blocked(List.of(new BlockerDetail(
          blockerPrefix + "_pixel_sample_failed",
          exceptionObserved(ex),
          "center pixel sample readable from captured image")), ex.getClass().getSimpleName());
    }
  }

  private static List<BlockerDetail> componentReadinessBlockers(
      Component component,
      String blockerPrefix,
      String statePrefix) {
    List<BlockerDetail> blockers = new ArrayList<>();
    if (!component.isDisplayable()) {
      blockers.add(new BlockerDetail(
          blockerPrefix + "_not_displayable",
          statePrefix + "Displayable=false",
          statePrefix + "Displayable=true"));
    }
    if (!component.isShowing()) {
      blockers.add(new BlockerDetail(
          blockerPrefix + "_not_showing",
          statePrefix + "Showing=false",
          statePrefix + "Showing=true"));
    }
    if (component.getWidth() <= 0 || component.getHeight() <= 0) {
      blockers.add(new BlockerDetail(
          blockerPrefix + "_has_no_positive_size",
          statePrefix + "Width=" + component.getWidth()
              + ", " + statePrefix + "Height=" + component.getHeight(),
          statePrefix + "Width>0 and " + statePrefix + "Height>0"));
    }
    return blockers;
  }

  private static String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    if (second != null && !second.isBlank()) {
      return second;
    }
    return "";
  }

  private static String exceptionObserved(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getSimpleName();
    }
    return throwable.getClass().getSimpleName() + ": " + message;
  }

  private static void writeStringAtomically(Path target, String content) throws IOException {
    Path temp = target.resolveSibling(target.getFileName() + ".tmp");
    Files.writeString(temp, content, StandardCharsets.UTF_8);
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private static void writePngAtomically(Path target, BufferedImage image) throws IOException {
    Path temp = target.resolveSibling(target.getFileName() + ".tmp");
    if (!ImageIO.write(image, "png", temp.toFile())) {
      throw new IOException("PNG writer unavailable: " + target);
    }
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private static void requireNonEmptyArtifact(Path path, String label) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) == 0) {
      throw new IOException(label + " was not written: " + path);
    }
  }

  private static String runtimeLog(String programTypeName, List<String> events) {
    StringBuilder builder = new StringBuilder();
    builder.append("schema_version=eatme.alice-desktop-run-execution-log/v1\n");
    builder.append("program_type=").append(programTypeName).append('\n');
    builder.append("recorded_at=").append(Instant.now()).append('\n');
    for (String event : events) {
      builder.append(event).append('\n');
    }
    return builder.toString();
  }

  private static String componentClassName(Component component) {
    return EatmeRunWindowEvidence.escapeJson(component.getClass().getName());
  }

  private static String componentName(Component component) {
    String name = component.getName();
    return name != null ? name : "";
  }

  private static int childComponentCount(Component component) {
    if (component instanceof java.awt.Container container) {
      return container.getComponentCount();
    }
    return 0;
  }

  private static final class PixelObservation {
    private final String status;
    private final String claim;
    private final String detailJson;
    private final List<BlockerDetail> blockers;
    private final String exceptionType;

    private PixelObservation(
        String status,
        String claim,
        String detailJson,
        List<BlockerDetail> blockers,
        String exceptionType) {
      this.status = status;
      this.claim = claim;
      this.detailJson = detailJson;
      this.blockers = blockers;
      this.exceptionType = exceptionType;
    }

    private boolean isObserved() {
      return "observed".equals(status);
    }

    private static PixelObservation blocked(List<BlockerDetail> blockers, String exceptionType) {
      return new PixelObservation(
          "blocked",
          "No desktop pixel was sampled.",
          "  \"blocker\": {\n"
              + "    \"reason\": \"A desktop screenshot requires a non-headless graphics environment, a showing Run render target, positive component size, and screen-capture access.\",\n"
              + "    \"codes\": " + blockerCodesJson(blockers) + ",\n"
              + "    \"details\": " + blockerDetailsJson(blockers) + ",\n"
              + "    \"exceptionType\": \"" + EatmeRunWindowEvidence.escapeJson(exceptionType) + "\"\n"
              + "  },\n",
          blockers,
          exceptionType);
    }

    private static PixelObservation observed(
        String captureRole,
        Component captureComponent,
        String screenshot,
        Rectangle captureArea,
        int screenshotWidth,
        int screenshotHeight,
        int sampleX,
        int sampleY,
        int argb) {
      return new PixelObservation(
          "observed",
          "A desktop screenshot of the Run render target area was captured and its center pixel was sampled.",
          "  \"captureTarget\": {\n"
              + "    \"role\": \"" + EatmeRunWindowEvidence.escapeJson(captureRole) + "\",\n"
              + "    \"componentClass\": \"" + componentClassName(captureComponent) + "\",\n"
              + "    \"componentName\": \"" + EatmeRunWindowEvidence.escapeJson(componentName(captureComponent)) + "\"\n"
              + "  },\n"
              + "  \"screenshot\": {\n"
              + "    \"file\": \"" + EatmeRunWindowEvidence.escapeJson(screenshot) + "\",\n"
              + "    \"width\": " + screenshotWidth + ",\n"
              + "    \"height\": " + screenshotHeight + "\n"
              + "  },\n"
              + "  \"captureArea\": {\n"
              + "    \"coordinateSystem\": \"screen\",\n"
              + "    \"x\": " + captureArea.x + ",\n"
              + "    \"y\": " + captureArea.y + ",\n"
              + "    \"width\": " + captureArea.width + ",\n"
              + "    \"height\": " + captureArea.height + "\n"
              + "  },\n"
              + "  \"sample\": {\n"
              + "    \"coordinateSystem\": \"screenshot\",\n"
              + "    \"x\": " + sampleX + ",\n"
              + "    \"y\": " + sampleY + ",\n"
              + "    \"argb\": \"" + String.format("0x%08X", argb) + "\"\n"
              + "  },\n",
          List.of(),
          "");
    }
  }

  private static final class BlockerDetail {
    private final String code;
    private final String observed;
    private final String required;

    private BlockerDetail(String code, String observed, String required) {
      this.code = code;
      this.observed = observed;
      this.required = required;
    }
  }

  private static String blockerCodesJson(List<BlockerDetail> blockers) {
    List<String> values = new ArrayList<>();
    for (BlockerDetail blocker : blockers) {
      values.add(blocker.code);
    }
    return jsonArray(values);
  }

  private static String blockerDetailsJson(List<BlockerDetail> blockers) {
    StringBuilder builder = new StringBuilder("[\n");
    for (int i = 0; i < blockers.size(); i++) {
      BlockerDetail blocker = blockers.get(i);
      if (i > 0) {
        builder.append(",\n");
      }
      builder.append("      {\n")
          .append("        \"code\": \"").append(EatmeRunWindowEvidence.escapeJson(blocker.code)).append("\",\n")
          .append("        \"observed\": \"").append(EatmeRunWindowEvidence.escapeJson(blocker.observed)).append("\",\n")
          .append("        \"required\": \"").append(EatmeRunWindowEvidence.escapeJson(blocker.required)).append("\"\n")
          .append("      }");
    }
    builder.append("\n    ]");
    return builder.toString();
  }

  private static String jsonArray(List<String> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append('"').append(EatmeRunWindowEvidence.escapeJson(values.get(i))).append('"');
    }
    builder.append(']');
    return builder.toString();
  }
}
