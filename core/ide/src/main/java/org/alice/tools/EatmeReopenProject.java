package org.alice.tools;

import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SScene;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EatmeReopenProject {
  private static final String SUPPORTED_SELECTOR_PREFIX = "scene.";
  private static final String REOPENED_PROJECT = "reopened.a3p";
  private static final String REOPEN_ARTIFACT = "reopen-evidence.json";
  private static final String REOPENED_STATE_ARTIFACT = "reopened-state.json";

  private EatmeReopenProject() {
  }

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) {
      System.exit(status);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    PrintStream originalSystemOut = System.out;
    PrintStream silentSystemOut = new PrintStream(OutputStream.nullOutputStream());
    System.setOut(silentSystemOut);
    try {
      Arguments arguments = Arguments.parse(args);
      ProjectReopen reopen = reopenProject(arguments);
      out.println(resultJson(reopen));
      return 0;
    } catch (IllegalArgumentException | IOException | VersionNotSupportedException ex) {
      err.println(ex.getMessage());
      return 2;
    } catch (RuntimeException ex) {
      err.println("project reopen failed: " + ex.getMessage());
      return 3;
    } finally {
      System.setOut(originalSystemOut);
      silentSystemOut.close();
    }
  }

  private static ProjectReopen reopenProject(Arguments arguments) throws IOException, VersionNotSupportedException {
    if (!Files.isRegularFile(arguments.savedProject())) {
      throw new IllegalArgumentException("project file does not exist: " + arguments.savedProject());
    }
    String methodName = methodName(arguments.reopenSelector());
    Files.createDirectories(arguments.evidenceDir());

    Project project = IoUtilities.readProject(arguments.savedProject().toFile());
    NamedUserType sceneType = findSceneType(project);
    UserMethod method = findMethod(sceneType, methodName);
    if (method == null) {
      throw new IllegalArgumentException("reopen selector does not name a scene method in the project: " + arguments.reopenSelector());
    }
    if (!method.getRequiredParameters().isEmpty()) {
      throw new IllegalArgumentException("reopen selector method must not require parameters: " + arguments.reopenSelector());
    }

    Path reopenedProject = artifactPath(arguments.evidenceDir(), REOPENED_PROJECT);
    IoUtilities.writeProject(reopenedProject.toFile(), project);
    requireNonEmptyArtifact(reopenedProject, "reopened project artifact");

    Project rereadProject = IoUtilities.readProject(reopenedProject.toFile());
    NamedUserType rereadSceneType = findSceneType(rereadProject);
    boolean sceneTypeMatches = sceneType.getName().equals(rereadSceneType.getName());
    boolean methodPresent = findMethod(rereadSceneType, methodName) != null;
    if (!methodPresent) {
      throw new IllegalArgumentException("reopened project does not contain selected scene method: " + arguments.reopenSelector());
    }

    String sourceSavedProject = Path.of("").toAbsolutePath()
        .relativize(arguments.savedProject().toAbsolutePath()).toString();

    ProjectReopen reopen = new ProjectReopen(
        arguments.reopenSelector(),
        sceneType.getName(),
        methodName,
        sourceSavedProject,
        REOPENED_PROJECT,
        sceneTypeMatches,
        rereadSceneType.getName());

    Path reopenArtifactPath = artifactPath(arguments.evidenceDir(), REOPEN_ARTIFACT);
    Files.writeString(reopenArtifactPath, reopenArtifactJson(reopen), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(reopenArtifactPath, "reopen evidence artifact");

    Path reopenedStatePath = artifactPath(arguments.evidenceDir(), REOPENED_STATE_ARTIFACT);
    Files.writeString(reopenedStatePath,
        reopenedStateJson(sceneTypeMatches, methodPresent, sceneType.getName(), rereadSceneType.getName()),
        StandardCharsets.UTF_8);
    requireNonEmptyArtifact(reopenedStatePath, "reopened state artifact");

    return reopen;
  }

  private static String methodName(String reopenSelector) {
    if (!reopenSelector.startsWith(SUPPORTED_SELECTOR_PREFIX)) {
      throw new IllegalArgumentException("unsupported reopen selector: " + reopenSelector);
    }
    String methodName = reopenSelector.substring(SUPPORTED_SELECTOR_PREFIX.length());
    if (!methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      throw new IllegalArgumentException("reopen selector must name one scene method: " + reopenSelector);
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

  static String resultJson(ProjectReopen reopen) {
    String verification = reopen.sceneTypeMatches() ? "passed" : "failed";
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"schema_version\":\"eatme.alice-project-reopen-result/v1\",");
    json.append("\"status\":\"reopened\",");
    json.append("\"source_saved_project_artifact\":\"").append(escapeJson(reopen.sourceSavedProject())).append("\",");
    json.append("\"reopen_selector\":\"").append(escapeJson(reopen.reopenSelector())).append("\",");
    json.append("\"reopened_project_artifact\":\"").append(REOPENED_PROJECT).append("\",");
    json.append("\"reopen_artifact\":\"").append(REOPEN_ARTIFACT).append("\",");
    json.append("\"reopened_state_artifact\":\"").append(REOPENED_STATE_ARTIFACT).append("\",");
    json.append("\"state_verification\":\"").append(verification).append("\"");
    if (!reopen.sceneTypeMatches()) {
      json.append(",\"scene_type_mismatch\":{");
      json.append("\"original_scene_type\":\"").append(escapeJson(reopen.sceneType())).append("\",");
      json.append("\"reopened_scene_type\":\"").append(escapeJson(reopen.reopenedSceneType())).append("\"");
      json.append("}");
    }
    json.append("}");
    return json.toString();
  }

  private static String reopenArtifactJson(ProjectReopen reopen) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-project-reopen-artifact/v1\",\n"
        + "  \"reopen_selector\": \"" + escapeJson(reopen.reopenSelector()) + "\",\n"
        + "  \"scene_type\": \"" + escapeJson(reopen.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(reopen.methodName()) + "\",\n"
        + "  \"reopen_mode\": \"headless_project_persistence_roundtrip\",\n"
        + "  \"source_saved_project\": \"" + escapeJson(reopen.sourceSavedProject()) + "\",\n"
        + "  \"reopened_project\": \"" + escapeJson(reopen.reopenedProject()) + "\",\n"
        + "  \"readable_after_reopen\": true\n"
        + "}\n";
  }

  static String reopenedStateJson(boolean sceneTypeMatches, boolean methodPresent,
      String originalSceneType, String reopenedSceneType) {
    String verification = sceneTypeMatches ? "passed" : "failed";
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"schema_version\": \"eatme.alice-project-reopen-state/v1\",\n");
    json.append("  \"scene_type_matches\": ").append(sceneTypeMatches).append(",\n");
    json.append("  \"method_present_after_roundtrip\": ").append(methodPresent).append(",\n");
    json.append("  \"state_verification\": \"").append(verification).append("\"");
    if (!sceneTypeMatches) {
      json.append(",\n");
      json.append("  \"scene_type_mismatch\": {\n");
      json.append("    \"original_scene_type\": \"").append(escapeJson(originalSceneType)).append("\",\n");
      json.append("    \"reopened_scene_type\": \"").append(escapeJson(reopenedSceneType)).append("\"\n");
      json.append("  }");
    }
    json.append("\n}\n");
    return json.toString();
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

  record Arguments(Path savedProject, String reopenSelector, Path evidenceDir) {
    static Arguments parse(String[] args) {
      Path savedProject = null;
      String reopenSelector = null;
      Path evidenceDir = null;
      boolean json = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--saved-project" -> savedProject = Path.of(nextValue(args, ++i, "--saved-project"));
          case "--reopen-selector" -> reopenSelector = nextValue(args, ++i, "--reopen-selector");
          case "--evidence-dir" -> evidenceDir = Path.of(nextValue(args, ++i, "--evidence-dir"));
          case "--json" -> json = true;
          default -> throw new IllegalArgumentException("unsupported argument: " + args[i]);
        }
      }
      if (savedProject == null) {
        throw new IllegalArgumentException("--saved-project is required");
      }
      if (reopenSelector == null || reopenSelector.isBlank()) {
        throw new IllegalArgumentException("--reopen-selector is required");
      }
      if (evidenceDir == null) {
        throw new IllegalArgumentException("--evidence-dir is required");
      }
      if (!json) {
        throw new IllegalArgumentException("--json is required");
      }
      return new Arguments(savedProject, reopenSelector, evidenceDir);
    }

    private static String nextValue(String[] args, int index, String name) {
      if (index >= args.length) {
        throw new IllegalArgumentException(name + " requires a value");
      }
      return args[index];
    }
  }

  record ProjectReopen(
      String reopenSelector,
      String sceneType,
      String methodName,
      String sourceSavedProject,
      String reopenedProject,
      boolean sceneTypeMatches,
      String reopenedSceneType) {
  }
}
