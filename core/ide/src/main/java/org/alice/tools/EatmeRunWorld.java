package org.alice.tools;

import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.virtualmachine.ReleaseVirtualMachine;
import org.lgna.project.virtualmachine.events.CountLoopIterationEvent;
import org.lgna.project.virtualmachine.events.EachInTogetherItemEvent;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.ForEachLoopIterationEvent;
import org.lgna.project.virtualmachine.events.StatementExecutionEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;
import org.lgna.project.virtualmachine.events.WhileLoopIterationEvent;
import org.lgna.story.SScene;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EatmeRunWorld {
  private static final String SUPPORTED_SELECTOR_PREFIX = "scene.";
  private static final String DEFAULT_RUN_SELECTOR = "scene.eatmeFirstLessonStep";
  private static final String RUN_ARTIFACT = "world-run.json";
  private static final String RUNTIME_LOG = "runtime.log";

  private EatmeRunWorld() {
  }

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) {
      System.exit(status);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    PrintStream originalSystemOut = System.out;
    PrintStream silentSystemOut = new PrintStream(new ByteArrayOutputStream());
    System.setOut(silentSystemOut);
    try {
      Arguments arguments = Arguments.parse(args);
      WorldRun worldRun = runWorld(arguments);
      out.println(resultJson(worldRun));
      return 0;
    } catch (IllegalArgumentException | IOException | VersionNotSupportedException ex) {
      err.println(ex.getMessage());
      return 2;
    } catch (RuntimeException ex) {
      err.println("world run failed: " + ex.getMessage());
      return 3;
    } finally {
      System.setOut(originalSystemOut);
      silentSystemOut.close();
    }
  }

  private static WorldRun runWorld(Arguments arguments) throws IOException, VersionNotSupportedException {
    if (!Files.isRegularFile(arguments.project())) {
      throw new IllegalArgumentException("project file does not exist: " + arguments.project());
    }
    if (!DEFAULT_RUN_SELECTOR.equals(arguments.runSelector())) {
      throw new IllegalArgumentException("unsupported run selector: " + arguments.runSelector());
    }
    String methodName = methodName(arguments.runSelector());
    Files.createDirectories(arguments.evidenceDir());

    Project project = IoUtilities.readProject(arguments.project().toFile());
    NamedUserType sceneType = findSceneType(project);
    UserMethod method = findMethod(sceneType, methodName);
    if (method == null) {
      throw new IllegalArgumentException("run selector does not name a scene method in the project: " + arguments.runSelector());
    }
    if (!method.getRequiredParameters().isEmpty()) {
      throw new IllegalArgumentException("run selector method must not require parameters: " + arguments.runSelector());
    }

    RecordingListener listener = new RecordingListener();
    ReleaseVirtualMachine virtualMachine = new ReleaseVirtualMachine();
    virtualMachine.addVirtualMachineListener(listener);
    boolean wasStatic = method.isStatic();
    method.isStatic.setValue(true);
    try {
      virtualMachine.ENTRY_POINT_invoke(null, method);
    } finally {
      method.isStatic.setValue(wasStatic);
      virtualMachine.removeVirtualMachineListener(listener);
      virtualMachine.stopExecution();
    }

    if (listener.executedStatementCount() == 0) {
      throw new IllegalArgumentException("run selector executed no statements: " + arguments.runSelector());
    }

    WorldRun worldRun = new WorldRun(
        arguments.runSelector(),
        sceneType.getName(),
        methodName,
        listener.executingStatementCount(),
        listener.executedStatementCount(),
        RUN_ARTIFACT,
        RUNTIME_LOG,
        listener.events());
    Path runArtifact = artifactPath(arguments.evidenceDir(), RUN_ARTIFACT);
    Files.writeString(runArtifact, runArtifactJson(worldRun), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(runArtifact, "world run artifact");
    Path runtimeLog = artifactPath(arguments.evidenceDir(), RUNTIME_LOG);
    Files.writeString(runtimeLog, runtimeLog(worldRun), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(runtimeLog, "runtime log artifact");
    return worldRun;
  }

  private static String methodName(String runSelector) {
    if (!runSelector.startsWith(SUPPORTED_SELECTOR_PREFIX)) {
      throw new IllegalArgumentException("unsupported run selector: " + runSelector);
    }
    String methodName = runSelector.substring(SUPPORTED_SELECTOR_PREFIX.length());
    if (!methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      throw new IllegalArgumentException("run selector must name one scene method: " + runSelector);
    }
    return methodName;
  }

  private static void requireNonEmptyArtifact(Path path, String label) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) == 0) {
      throw new IOException(label + " was not written: " + path);
    }
  }

  static Path artifactPath(Path evidenceDir, String relativePath) {
    Path path = Path.of(relativePath);
    Path normalized = path.normalize();
    if (path.isAbsolute()
        || normalized.toString().isEmpty()
        || normalized.getNameCount() != 1
        || normalized.startsWith("..")) {
      throw new IllegalArgumentException("artifact path must be a single relative file name: " + relativePath);
    }
    Path resolved = evidenceDir.resolve(normalized).normalize();
    if (!resolved.startsWith(evidenceDir)) {
      throw new IllegalArgumentException("artifact path escapes evidence dir: " + relativePath);
    }
    return resolved;
  }

  private static NamedUserType findSceneType(Project project) {
    for (UserField field : project.getProgramType().getDeclaredFields()) {
      AbstractType<?, ?, ?> valueType = field.getValueType();
      if (valueType instanceof NamedUserType namedUserType && valueType.isAssignableTo(SScene.class)) {
        return namedUserType;
      }
    }
    throw new IllegalArgumentException("project does not contain a program field typed by an SScene subtype");
  }

  private static UserMethod findMethod(NamedUserType sceneType, String methodName) {
    for (UserMethod method : sceneType.getDeclaredMethods()) {
      if (methodName.equals(method.getName())) {
        return method;
      }
    }
    return null;
  }

  private static String resultJson(WorldRun worldRun) {
    return "{"
        + "\"schema_version\":\"eatme.alice-world-run-result/v1\","
        + "\"status\":\"ran\","
        + "\"run_selector\":\"" + escapeJson(worldRun.runSelector()) + "\","
        + "\"run_artifact\":\"" + RUN_ARTIFACT + "\","
        + "\"runtime_or_log_evidence\":\"" + RUNTIME_LOG + "\""
        + "}";
  }

  private static String runArtifactJson(WorldRun worldRun) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-world-run-artifact/v1\",\n"
        + "  \"run_selector\": \"" + escapeJson(worldRun.runSelector()) + "\",\n"
        + "  \"scene_type\": \"" + escapeJson(worldRun.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(worldRun.methodName()) + "\",\n"
        + "  \"execution_mode\": \"headless_vm_scene_method_body\",\n"
        + "  \"executing_statement_count\": " + worldRun.executingStatementCount() + ",\n"
        + "  \"executed_statement_count\": " + worldRun.executedStatementCount() + ",\n"
        + "  \"runtime_log\": \"" + escapeJson(worldRun.runtimeLog()) + "\"\n"
        + "}\n";
  }

  private static String runtimeLog(WorldRun worldRun) {
    StringBuilder builder = new StringBuilder();
    builder.append("schema_version=eatme.alice-world-run-log/v1\n");
    builder.append("run_selector=").append(worldRun.runSelector()).append('\n');
    for (String event : worldRun.events()) {
      builder.append(event).append('\n');
    }
    return builder.toString();
  }

  static String escapeJson(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '\\' -> escaped.append("\\\\");
        case '"' -> escaped.append("\\\"");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (ch < 0x20) {
            escaped.append(String.format("\\u%04x", (int) ch));
          } else {
            escaped.append(ch);
          }
        }
      }
    }
    return escaped.toString();
  }

  record Arguments(Path project, String runSelector, Path evidenceDir) {
    static Arguments parse(String[] args) {
      Path project = null;
      String runSelector = null;
      Path evidenceDir = null;
      boolean json = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--project" -> project = Path.of(nextValue(args, ++i, "--project"));
          case "--run-selector" -> runSelector = nextValue(args, ++i, "--run-selector");
          case "--evidence-dir" -> evidenceDir = Path.of(nextValue(args, ++i, "--evidence-dir"));
          case "--json" -> json = true;
          default -> throw new IllegalArgumentException("unsupported argument: " + args[i]);
        }
      }
      if (project == null) {
        throw new IllegalArgumentException("--project is required");
      }
      if (runSelector == null || runSelector.isBlank()) {
        throw new IllegalArgumentException("--run-selector is required");
      }
      if (evidenceDir == null) {
        throw new IllegalArgumentException("--evidence-dir is required");
      }
      if (!json) {
        throw new IllegalArgumentException("--json is required");
      }
      return new Arguments(project, runSelector, evidenceDir);
    }

    private static String nextValue(String[] args, int index, String flag) {
      if (index >= args.length || args[index].startsWith("--")) {
        throw new IllegalArgumentException(flag + " requires a value");
      }
      return args[index];
    }
  }

  private static final class RecordingListener implements VirtualMachineListener {
    private final List<String> events = new ArrayList<>();
    private int executingStatementCount;
    private int executedStatementCount;

    @Override
    public void statementExecuting(StatementExecutionEvent statementExecutionEvent) {
      executingStatementCount++;
      events.add("executing:" + statementExecutionEvent.getStatement().getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent statementExecutionEvent) {
      executedStatementCount++;
      events.add("executed:" + statementExecutionEvent.getStatement().getClass().getSimpleName());
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

    int executingStatementCount() {
      return executingStatementCount;
    }

    int executedStatementCount() {
      return executedStatementCount;
    }

    List<String> events() {
      return events;
    }
  }

  record WorldRun(
      String runSelector,
      String sceneType,
      String methodName,
      int executingStatementCount,
      int executedStatementCount,
      String runArtifact,
      String runtimeLog,
      List<String> events) {
  }
}
