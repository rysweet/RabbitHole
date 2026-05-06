package org.alice.tools;

import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SScene;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EatmeSaveProject {
  private static final String SUPPORTED_SELECTOR_PREFIX = "scene.";
  private static final String DEFAULT_SAVE_SELECTOR = "scene.eatmeFirstLessonStep";
  private static final String SAVED_PROJECT = "saved-project.a3p";
  private static final String SAVE_ARTIFACT = "project-save.json";

  private EatmeSaveProject() {
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
      ProjectSave save = saveProject(arguments);
      out.println(resultJson(save));
      return 0;
    } catch (IllegalArgumentException | IOException | VersionNotSupportedException ex) {
      err.println(ex.getMessage());
      return 2;
    } catch (RuntimeException ex) {
      err.println("project save failed: " + ex.getMessage());
      return 3;
    } finally {
      System.setOut(originalSystemOut);
      silentSystemOut.close();
    }
  }

  private static ProjectSave saveProject(Arguments arguments) throws IOException, VersionNotSupportedException {
    if (!Files.isRegularFile(arguments.project())) {
      throw new IllegalArgumentException("project file does not exist: " + arguments.project());
    }
    if (!DEFAULT_SAVE_SELECTOR.equals(arguments.saveSelector())) {
      throw new IllegalArgumentException("unsupported save selector: " + arguments.saveSelector());
    }
    String methodName = methodName(arguments.saveSelector());
    Files.createDirectories(arguments.evidenceDir());

    Project project = IoUtilities.readProject(arguments.project().toFile());
    NamedUserType sceneType = findSceneType(project);
    UserMethod method = findMethod(sceneType, methodName);
    if (method == null) {
      throw new IllegalArgumentException("save selector does not name a scene method in the project: " + arguments.saveSelector());
    }
    if (!method.getRequiredParameters().isEmpty()) {
      throw new IllegalArgumentException("save selector method must not require parameters: " + arguments.saveSelector());
    }

    Path savedProject = artifactPath(arguments.evidenceDir(), SAVED_PROJECT);
    IoUtilities.writeProject(savedProject.toFile(), project);
    requireNonEmptyArtifact(savedProject, "saved project artifact");

    Project rereadProject = IoUtilities.readProject(savedProject.toFile());
    NamedUserType rereadSceneType = findSceneType(rereadProject);
    if (findMethod(rereadSceneType, methodName) == null) {
      throw new IllegalArgumentException("saved project does not contain selected scene method: " + arguments.saveSelector());
    }

    ProjectSave save = new ProjectSave(
        arguments.saveSelector(),
        sceneType.getName(),
        methodName,
        SAVED_PROJECT,
        SAVE_ARTIFACT);
    Path saveArtifact = artifactPath(arguments.evidenceDir(), SAVE_ARTIFACT);
    Files.writeString(saveArtifact, saveArtifactJson(save), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(saveArtifact, "project save artifact");
    return save;
  }

  private static String methodName(String saveSelector) {
    if (!saveSelector.startsWith(SUPPORTED_SELECTOR_PREFIX)) {
      throw new IllegalArgumentException("unsupported save selector: " + saveSelector);
    }
    String methodName = saveSelector.substring(SUPPORTED_SELECTOR_PREFIX.length());
    if (!methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      throw new IllegalArgumentException("save selector must name one scene method: " + saveSelector);
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

  private static String resultJson(ProjectSave save) {
    return "{"
        + "\"schema_version\":\"eatme.alice-project-save-result/v1\","
        + "\"status\":\"saved\","
        + "\"save_selector\":\"" + escapeJson(save.saveSelector()) + "\","
        + "\"saved_project_artifact\":\"" + SAVED_PROJECT + "\","
        + "\"save_artifact\":\"" + SAVE_ARTIFACT + "\""
        + "}";
  }

  private static String saveArtifactJson(ProjectSave save) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-project-save-artifact/v1\",\n"
        + "  \"save_selector\": \"" + escapeJson(save.saveSelector()) + "\",\n"
        + "  \"scene_type\": \"" + escapeJson(save.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(save.methodName()) + "\",\n"
        + "  \"save_mode\": \"headless_project_persistence_roundtrip\",\n"
        + "  \"saved_project\": \"" + escapeJson(save.savedProject()) + "\",\n"
        + "  \"readable_after_save\": true\n"
        + "}\n";
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

  record Arguments(Path project, String saveSelector, Path evidenceDir) {
    static Arguments parse(String[] args) {
      Path project = null;
      String saveSelector = null;
      Path evidenceDir = null;
      boolean json = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--project" -> project = Path.of(nextValue(args, ++i, "--project"));
          case "--save-selector" -> saveSelector = nextValue(args, ++i, "--save-selector");
          case "--evidence-dir" -> evidenceDir = Path.of(nextValue(args, ++i, "--evidence-dir"));
          case "--json" -> json = true;
          default -> throw new IllegalArgumentException("unsupported argument: " + args[i]);
        }
      }
      if (project == null) {
        throw new IllegalArgumentException("--project is required");
      }
      if (saveSelector == null || saveSelector.isBlank()) {
        throw new IllegalArgumentException("--save-selector is required");
      }
      if (evidenceDir == null) {
        throw new IllegalArgumentException("--evidence-dir is required");
      }
      if (!json) {
        throw new IllegalArgumentException("--json is required");
      }
      return new Arguments(project, saveSelector, evidenceDir);
    }

    private static String nextValue(String[] args, int index, String name) {
      if (index >= args.length) {
        throw new IllegalArgumentException(name + " requires a value");
      }
      return args[index];
    }
  }

  record ProjectSave(
      String saveSelector,
      String sceneType,
      String methodName,
      String savedProject,
      String saveArtifact) {
  }
}
