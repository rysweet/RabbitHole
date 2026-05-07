package org.alice.tools;

import org.alice.ide.declarationseditor.CodeComposite;
import org.alice.ide.declarationseditor.DeclarationsEditorComposite;
import org.alice.ide.declarationseditor.ProcedureTabSelection;
import org.lgna.croquet.Application;
import org.lgna.croquet.DocumentFrame;
import org.lgna.croquet.Operation;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SScene;

import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public final class EatmeEditProcedure {
  private static final String SUPPORTED_SELECTOR_PREFIX = "scene.";
  private static final String SUPPORTED_EDIT_PREFIX = "append-comment:";
  private static final String EDIT_ARTIFACT = "procedure-edit.json";
  private static final String EDIT_COMMAND_ARTIFACT = "procedure-edit-command.json";
  private static final String DIFF_ARTIFACT = "procedure.diff.json";
  private static final String PROCEDURE_TAB_SELECTION_ARTIFACT = "procedure-tab-selection.json";
  private static final String UI_ACTION_NO_GO_ARTIFACT = "procedure-ui-action-no-go.json";
  private static final String EDITED_PROJECT = "edited-project.a3p";

  private EatmeEditProcedure() {
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
      ProcedureEdit edit = editProcedure(arguments);
      out.println(resultJson(edit));
      return 0;
    } catch (IllegalArgumentException | IOException | VersionNotSupportedException ex) {
      err.println(ex.getMessage());
      return 2;
    } catch (RuntimeException ex) {
      err.println("procedure edit failed: " + ex.getMessage());
      return 3;
    } finally {
      System.setOut(originalSystemOut);
      silentSystemOut.close();
    }
  }

  private static ProcedureEdit editProcedure(Arguments arguments) throws IOException, VersionNotSupportedException {
    if (!Files.isRegularFile(arguments.project())) {
      throw new IllegalArgumentException("project file does not exist: " + arguments.project());
    }
    if (!arguments.procedureSelector().startsWith(SUPPORTED_SELECTOR_PREFIX)) {
      throw new IllegalArgumentException("unsupported procedure selector: " + arguments.procedureSelector());
    }
    if (!arguments.editSpec().startsWith(SUPPORTED_EDIT_PREFIX)) {
      throw new IllegalArgumentException("unsupported edit spec: " + arguments.editSpec());
    }
    String methodName = arguments.procedureSelector().substring(SUPPORTED_SELECTOR_PREFIX.length());
    if (!methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      throw new IllegalArgumentException("procedure selector must name one scene method: " + arguments.procedureSelector());
    }
    String commentText = arguments.editSpec().substring(SUPPORTED_EDIT_PREFIX.length());
    if (commentText.isBlank()) {
      throw new IllegalArgumentException("append-comment edit spec must include non-blank text");
    }
    Files.createDirectories(arguments.evidenceDir());

    Project project = IoUtilities.readProject(arguments.project().toFile());
    NamedUserType sceneType = findSceneType(project);
    List<String> beforeMethods = methodNames(sceneType);
    UserMethod method = findMethod(sceneType, methodName);
    boolean createdMethod = false;
    if (method == null) {
      method = new UserMethod(methodName, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement());
      sceneType.methods.add(method);
      createdMethod = true;
    }
    BlockStatement body = method.body.getValue();
    if (body == null) {
      body = new BlockStatement();
      method.body.setValue(body);
    }
    ProcedureTabSelectionEvidence tabSelection = selectProcedureTab(method);
    ProcedureEditCommand.Result commandResult = ProcedureEditCommand.appendComment(
        arguments.procedureSelector(),
        method,
        tabSelection.selectedMethod(),
        commentText);
    int beforeStatementCount = commandResult.beforeStatementCount();
    int afterStatementCount = commandResult.afterStatementCount();
    List<String> afterMethods = methodNames(sceneType);

    Path editedProject = artifactPath(arguments.evidenceDir(), EDITED_PROJECT);
    IoUtilities.writeProject(editedProject.toFile(), project);
    if (!Files.isRegularFile(editedProject) || Files.size(editedProject) == 0) {
      throw new IOException("edited project was not written: " + editedProject);
    }

    ProcedureEdit edit = new ProcedureEdit(
        arguments.procedureSelector(),
        arguments.editSpec(),
        sceneType.getName(),
        methodName,
        createdMethod,
        beforeStatementCount,
        afterStatementCount,
        editedProject.getFileName().toString(),
        tabSelection.selectedMethod(),
        commandResult,
        beforeMethods,
        afterMethods);
    Path editArtifact = artifactPath(arguments.evidenceDir(), EDIT_ARTIFACT);
    Files.writeString(editArtifact, editArtifactJson(edit), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(editArtifact, "procedure edit artifact");
    Path editCommandArtifact = artifactPath(arguments.evidenceDir(), EDIT_COMMAND_ARTIFACT);
    Files.writeString(editCommandArtifact, editCommandArtifactJson(edit), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(editCommandArtifact, "procedure edit command artifact");
    Path diffArtifact = artifactPath(arguments.evidenceDir(), DIFF_ARTIFACT);
    Files.writeString(diffArtifact, diffArtifactJson(edit), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(diffArtifact, "procedure diff artifact");
    Path tabSelectionArtifact = artifactPath(arguments.evidenceDir(), PROCEDURE_TAB_SELECTION_ARTIFACT);
    Files.writeString(tabSelectionArtifact, tabSelectionArtifactJson(edit), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(tabSelectionArtifact, "procedure tab selection artifact");
    Path uiActionNoGoArtifact = artifactPath(arguments.evidenceDir(), UI_ACTION_NO_GO_ARTIFACT);
    Files.writeString(uiActionNoGoArtifact, uiActionNoGoArtifactJson(edit), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(uiActionNoGoArtifact, "procedure UI action no-go artifact");
    return edit;
  }

  private static ProcedureTabSelectionEvidence selectProcedureTab(UserMethod method) {
    ensureCroquetApplication();
    BlockStatement body = method.body.getValue();
    List<Statement> statementSnapshot = null;
    if (body != null && !body.statements.isEmpty()) {
      statementSnapshot = new ArrayList<>();
      for (Statement statement : body.statements) {
        statementSnapshot.add(statement);
      }
      body.statements.clear();
    }
    try {
      DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
      editor.getTabState().getData().internalSetAllItems(List.of(CodeComposite.getInstance(method)));
      UserMethod[] selected = new UserMethod[1];
      try {
        SwingUtilities.invokeAndWait(() -> selected[0] = ProcedureTabSelection.selectProcedureInEditor(editor, method, null));
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("procedure tab selection was interrupted", ex);
      } catch (InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtimeException) {
          throw runtimeException;
        }
        throw new IllegalStateException("procedure tab selection failed", cause);
      }
      if (selected[0] != method) {
        throw new IllegalStateException("procedure tab selection did not select: " + method.getName());
      }
      return new ProcedureTabSelectionEvidence(selected[0].getName());
    } finally {
      if (statementSnapshot != null) {
        body.statements.clear();
        body.statements.addAll(statementSnapshot);
      }
    }
  }

  private static void ensureCroquetApplication() {
    if (Application.getActiveInstance() == null) {
      new Application<DocumentFrame>() {
        @Override
        public DocumentFrame getDocumentFrame() {
          return null;
        }

        @Override
        protected Operation getAboutOperation() {
          return null;
        }

        @Override
        protected Operation getPreferencesOperation() {
          return null;
        }

        @Override
        protected void handleOpenFiles(List<java.io.File> files) {
        }

        @Override
        protected void handleWindowOpened(WindowEvent e) {
        }

        @Override
        public void handleQuit(UserActivity activity) {
        }

        @Override
        public String getApplicationSubPath() {
          return "eatme-procedure-tab-selection";
        }
      };
    }
  }

  private static void requireNonEmptyArtifact(Path path, String label) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) == 0) {
      throw new IOException(label + " was not written: " + path);
    }
  }

  private static Path artifactPath(Path evidenceDir, String relativePath) {
    Path path = Path.of(relativePath);
    if (path.isAbsolute() || path.normalize().getNameCount() != 1 || path.startsWith("..")) {
      throw new IllegalArgumentException("artifact path must be a single relative file name: " + relativePath);
    }
    Path resolved = evidenceDir.resolve(path).normalize();
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

  private static List<String> methodNames(NamedUserType type) {
    List<String> names = new ArrayList<>();
    for (UserMethod method : type.getDeclaredMethods()) {
      names.add(method.getName());
    }
    return names;
  }

  private static String resultJson(ProcedureEdit edit) {
    return "{"
        + "\"schema_version\":\"eatme.alice-procedure-edit-result/v1\","
        + "\"status\":\"edited\","
        + "\"procedure_selector\":\"" + escapeJson(edit.procedureSelector()) + "\","
        + "\"edited_project_artifact\":\"" + EDITED_PROJECT + "\","
        + "\"procedure_edit_command\":\"" + EDIT_COMMAND_ARTIFACT + "\","
        + "\"procedure_or_code_diff\":\"" + DIFF_ARTIFACT + "\","
        + "\"procedure_tab_selection\":\"" + PROCEDURE_TAB_SELECTION_ARTIFACT + "\","
        + "\"procedure_ui_action_no_go\":\"" + UI_ACTION_NO_GO_ARTIFACT + "\""
        + "}";
  }

  private static String editArtifactJson(ProcedureEdit edit) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-procedure-edit-artifact/v1\",\n"
        + "  \"procedure_selector\": \"" + escapeJson(edit.procedureSelector()) + "\",\n"
        + "  \"edit_spec\": \"" + escapeJson(edit.editSpec()) + "\",\n"
        + "  \"scene_type\": \"" + escapeJson(edit.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(edit.methodName()) + "\",\n"
        + "  \"created_method\": " + edit.createdMethod() + ",\n"
        + "  \"before_statement_count\": " + edit.beforeStatementCount() + ",\n"
        + "  \"after_statement_count\": " + edit.afterStatementCount() + ",\n"
        + "  \"edited_project\": \"" + escapeJson(edit.editedProject()) + "\"\n"
        + "}\n";
  }

  private static String tabSelectionArtifactJson(ProcedureEdit edit) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-procedure-tab-selection/v1\",\n"
        + "  \"procedure_selector\": \"" + escapeJson(edit.procedureSelector()) + "\",\n"
        + "  \"scene_type\": \"" + escapeJson(edit.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(edit.methodName()) + "\",\n"
        + "  \"selection_mode\": \"in_editor_procedure_tab_operation\",\n"
        + "  \"selected_method\": \"" + escapeJson(edit.selectedMethod()) + "\",\n"
        + "  \"operation_fired\": true,\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop UI action invoked\",\n"
        + "    \"code editor/procedure action completion\",\n"
        + "    \"full Alice UI automation\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"grading\",\n"
        + "    \"creative assessment\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String diffArtifactJson(ProcedureEdit edit) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-procedure-edit-diff/v1\",\n"
        + "  \"scene_type\": \"" + escapeJson(edit.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(edit.methodName()) + "\",\n"
        + "  \"created_method\": " + edit.createdMethod() + ",\n"
        + "  \"before_methods\": " + jsonArray(edit.beforeMethods()) + ",\n"
        + "  \"after_methods\": " + jsonArray(edit.afterMethods()) + ",\n"
        + "  \"statement_count_delta\": " + (edit.afterStatementCount() - edit.beforeStatementCount()) + ",\n"
        + "  \"edited_project\": \"" + escapeJson(edit.editedProject()) + "\"\n"
        + "}\n";
  }

  private static String editCommandArtifactJson(ProcedureEdit edit) {
    ProcedureEditCommand.Result result = edit.commandResult();
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-procedure-edit-command/v1\",\n"
        + "  \"procedure_selector\": \"" + escapeJson(result.procedureSelector()) + "\",\n"
        + "  \"command\": \"" + escapeJson(result.command()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(result.methodName()) + "\",\n"
        + "  \"selected_method\": \"" + escapeJson(result.selectedMethod()) + "\",\n"
        + "  \"completed\": " + result.completed() + ",\n"
        + "  \"before_statement_count\": " + result.beforeStatementCount() + ",\n"
        + "  \"after_statement_count\": " + result.afterStatementCount() + ",\n"
        + "  \"statement_count_delta\": " + result.statementCountDelta() + ",\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop UI action invoked\",\n"
        + "    \"desktop code editor command completion\",\n"
        + "    \"Save-menu completion\",\n"
        + "    \"full Alice UI automation\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"grading\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String uiActionNoGoArtifactJson(ProcedureEdit edit) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-code-procedure-ui-action-no-go/v1\",\n"
        + "  \"status\": \"blocked\",\n"
        + "  \"source\": \"EatmeEditProcedure\",\n"
        + "  \"procedure_selector\": \"" + escapeJson(edit.procedureSelector()) + "\",\n"
        + "  \"edit_spec\": \"" + escapeJson(edit.editSpec()) + "\",\n"
        + "  \"ast_edit_artifact\": \"" + EDIT_ARTIFACT + "\",\n"
        + "  \"procedure_edit_command\": \"" + EDIT_COMMAND_ARTIFACT + "\",\n"
        + "  \"procedure_or_code_diff\": \"" + DIFF_ARTIFACT + "\",\n"
        + "  \"procedure_tab_selection\": \"" + PROCEDURE_TAB_SELECTION_ARTIFACT + "\",\n"
        + "  \"proven\": \"Deterministic project edit command appends the requested procedure comment and writes edited-project.a3p, a command artifact, a procedure diff artifact, and an in-editor procedure tab selection artifact.\",\n"
        + "  \"exact_missing_ui_edit_action_target\": {\n"
        + "    \"expected_target\": \"desktop code editor edit action after selecting " + escapeJson(edit.procedureSelector()) + "\",\n"
        + "    \"missing_target\": \"No stable public action or invoker is exposed from org.alice.ide.codeeditor.CodeEditor or org.alice.ide.declarationseditor.CodeComposite for selecting "
        + escapeJson(edit.procedureSelector()) + " and applying " + escapeJson(edit.editSpec()) + " through the desktop code editor. The in-editor procedure tab operation is now fired separately and recorded in "
        + PROCEDURE_TAB_SELECTION_ARTIFACT + ".\"\n"
        + "  },\n"
        + "  \"examined_code_targets\": [\n"
        + "    {\n"
        + "      \"class\": \"org.alice.ide.codeeditor.CodeEditor\",\n"
        + "      \"observation\": \"Exposes getCode(), getTrackableShape(DropSite), and statement-list view construction, but no named operation that invokes a code edit with an action result.\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"class\": \"org.alice.ide.declarationseditor.CodeComposite\",\n"
        + "      \"observation\": \"Wraps AbstractCode/UserMethod into a declaration tab/view, but does not expose a deterministic code-edit action invoker.\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"class\": \"org.alice.ide.declarationseditor.DeclarationsEditorComposite\",\n"
        + "      \"observation\": \"Exposes DeclarationMenu and DeclarationTabState; the tab state now supports in-editor procedure selection evidence, but not a code edit action result.\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"blocker_codes\": [\n"
        + "    \"code_editor_action_target_not_exposed\",\n"
        + "    \"append_comment_ui_invocation_not_available\"\n"
        + "  ],\n"
        + "  \"required_next\": [\n"
        + "    \"Expose a stable code editor edit action target for the selected UserMethod.\",\n"
        + "    \"Return an invocation result that names the selected procedure and the changed code/procedure artifact.\"\n"
        + "  ],\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"desktop UI action invoked\",\n"
        + "    \"code editor/procedure action completion\",\n"
        + "    \"full Alice UI automation\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"grading\",\n"
        + "    \"creative assessment\"\n"
        + "  ]\n"
        + "}\n";
  }

  private static String jsonArray(List<String> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append('"').append(escapeJson(values.get(i))).append('"');
    }
    return builder.append(']').toString();
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

  record Arguments(Path project, String procedureSelector, String editSpec, Path evidenceDir) {
    static Arguments parse(String[] args) {
      Path project = null;
      String procedureSelector = null;
      String editSpec = null;
      Path evidenceDir = null;
      boolean json = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--project" -> project = Path.of(requireValue(args, ++i, "--project")).toAbsolutePath().normalize();
          case "--procedure-selector" -> procedureSelector = requireValue(args, ++i, "--procedure-selector");
          case "--edit-spec" -> editSpec = requireValue(args, ++i, "--edit-spec");
          case "--evidence-dir" -> evidenceDir = Path.of(requireValue(args, ++i, "--evidence-dir")).toAbsolutePath().normalize();
          case "--json" -> json = true;
          default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
        }
      }
      if (project == null) {
        throw new IllegalArgumentException("--project is required");
      }
      if (procedureSelector == null) {
        throw new IllegalArgumentException("--procedure-selector is required");
      }
      if (editSpec == null) {
        throw new IllegalArgumentException("--edit-spec is required");
      }
      if (evidenceDir == null) {
        throw new IllegalArgumentException("--evidence-dir is required");
      }
      if (!json) {
        throw new IllegalArgumentException("--json is required");
      }
      return new Arguments(project, procedureSelector, editSpec, evidenceDir);
    }

    private static String requireValue(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new IllegalArgumentException(option + " requires a value");
      }
      return args[index];
    }
  }

  record ProcedureEdit(
      String procedureSelector,
      String editSpec,
      String sceneType,
      String methodName,
      boolean createdMethod,
      int beforeStatementCount,
      int afterStatementCount,
      String editedProject,
      String selectedMethod,
      ProcedureEditCommand.Result commandResult,
      List<String> beforeMethods,
      List<String> afterMethods) {
  }

  record ProcedureTabSelectionEvidence(String selectedMethod) {
  }
}
