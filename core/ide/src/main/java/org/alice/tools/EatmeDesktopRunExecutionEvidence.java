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

public final class EatmeDesktopRunExecutionEvidence {
  public static final String EVIDENCE_DIR_PROPERTY = "org.alice.eatme.desktopRunExecutionEvidenceDir";
  public static final String DESKTOP_RUN_EXECUTION_ARTIFACT = "desktop-run-execution.json";
  public static final String DESKTOP_RUN_RUNTIME_LOG = "desktop-run-runtime.log";
  public static final String DESKTOP_RUN_RENDER_AFFORDANCE_ARTIFACT = "desktop-run-render-affordance.json";
  private static final int MAX_RECORDED_EVENTS = 200;
  private static final String RENDER_AFFORDANCE_CLAIM =
      "desktop Run attached the onscreen render target AWT component into the Run view hierarchy";

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

  private static String evidenceDirProperty() {
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
            + "    \"pixel rendering correctness\",\n"
            + "    \"screenshot validation\",\n"
            + "    \"coordinate-based visual validation\",\n"
            + "    \"rendering-engine correctness\"\n"
            + "  ]\n"
            + "}\n");
    requireNonEmptyArtifact(artifact, "desktop Run render-affordance artifact");
    return artifact;
  }

  private static void writeStringAtomically(Path target, String content) throws IOException {
    Path temp = target.resolveSibling(target.getFileName() + ".tmp");
    Files.writeString(temp, content, StandardCharsets.UTF_8);
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
}
