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
import org.lgna.project.ast.AbstractCode;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SScene;

import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
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
  private static final String FIRST_LESSON_SELECTOR = "scene.eatmeFirstLesson";
  private static final String ACTION_PROOF_ARTIFACT = "first-lesson-code-editor-action-proof.json";
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
    PrintStream silentSystemOut = new PrintStream(OutputStream.nullOutputStream());
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
    if (!FIRST_LESSON_SELECTOR.equals(arguments.procedureSelector())) {
      throw new IllegalArgumentException("first-lesson code-editor action proof requires target " + FIRST_LESSON_SELECTOR);
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
    if (method == null) {
      throw new IllegalArgumentException("target procedure not found: " + arguments.procedureSelector());
    }
    MarkerCounts existingMarkerCounts = countCommentMarkers(sceneType, method, commentText);
    if (existingMarkerCounts.total() > 0) {
      throw new IllegalArgumentException("edit marker already exists in project: " + commentText);
    }
    ensureProcedureBody(method);
    ProcedureTabSelectionEvidence tabSelection = selectProcedureTab(method);
    ProcedureEditCommand.Result commandResult = ProcedureEditCommand.appendComment(
        arguments.procedureSelector(),
        method,
        tabSelection.selectedMethod(),
        commentText);
    int beforeStatementCount = commandResult.beforeStatementCount();
    int afterStatementCount = commandResult.afterStatementCount();
    List<String> afterMethods = methodNames(sceneType);
    MarkerCounts markerCounts = countCommentMarkers(sceneType, method, commentText);
    int targetMarkerCount = markerCounts.target();
    int wrongTargetMarkerCount = markerCounts.outsideTarget();
    if (targetMarkerCount != 1) {
      throw new IllegalStateException("target marker count mismatch for " + arguments.procedureSelector()
          + ": expected 1 but found " + targetMarkerCount);
    }
    if (wrongTargetMarkerCount != 0) {
      throw new IllegalStateException("marker leaked outside " + arguments.procedureSelector()
          + ": expected 0 but found " + wrongTargetMarkerCount);
    }

    Path editedProject = artifactPath(arguments.evidenceDir(), EDITED_PROJECT);
    IoUtilities.writeProject(editedProject.toFile(), project);
    if (!Files.isRegularFile(editedProject) || Files.size(editedProject) == 0) {
      throw new IOException("edited project was not written: " + editedProject);
    }

    ProcedureEdit edit = new ProcedureEdit(
        arguments.procedureSelector(),
        arguments.editSpec(),
        arguments.project().getFileName().toString(),
        sceneType.getName(),
        methodName,
        beforeStatementCount,
        afterStatementCount,
        editedProject.getFileName().toString(),
        tabSelection,
        commandResult,
        beforeMethods,
        afterMethods,
        commentText,
        targetMarkerCount,
        wrongTargetMarkerCount);
    Path actionProofArtifact = artifactPath(arguments.evidenceDir(), ACTION_PROOF_ARTIFACT);
    Files.writeString(actionProofArtifact, actionProofArtifactJson(edit), StandardCharsets.UTF_8);
    requireNonEmptyArtifact(actionProofArtifact, "first-lesson code-editor action proof artifact");
    return edit;
  }

  private static void ensureProcedureBody(UserMethod method) {
    if (method.body.getValue() == null) {
      method.body.setValue(new BlockStatement());
    }
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
      CodeComposite selectedComposite = ProcedureTabSelection.getSelectedProcedureCodeComposite(editor);
      if (selectedComposite == null || selectedComposite.getDeclaration() != method) {
        throw new IllegalStateException("selected CodeComposite does not match procedure: " + method.getName());
      }
      AbstractCode selectedCodeEditorCode = ProcedureTabSelection.getSelectedCodeEditorCode(editor);
      if (selectedCodeEditorCode != method) {
        throw new IllegalStateException("selected CodeEditor code does not match procedure: " + method.getName());
      }
      String codeEditorBacking = ProcedureTabSelection.getSelectedCodeEditorBackingClassName(editor);
      return new ProcedureTabSelectionEvidence(
          selected[0].getName(),
          selectedComposite.getDeclaration().getName(),
          codeEditorBacking,
          selectedCodeEditorCode.getName());
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

  private static MarkerCounts countCommentMarkers(NamedUserType sceneType, UserMethod target, String marker) {
    int targetCount = 0;
    int outsideTargetCount = 0;
    for (UserMethod method : sceneType.getDeclaredMethods()) {
      int methodCount = countCommentMarkers(method, marker);
      if (method == target) {
        targetCount += methodCount;
      } else {
        outsideTargetCount += methodCount;
      }
    }
    return new MarkerCounts(targetCount, outsideTargetCount);
  }

  private static int countCommentMarkers(UserMethod method, String marker) {
    BlockStatement body = method.body.getValue();
    if (body == null) {
      return 0;
    }
    int count = 0;
    for (Statement statement : body.statements) {
      if (statement instanceof Comment comment && marker.equals(comment.text.getValue())) {
        count++;
      }
    }
    return count;
  }

  private static String resultJson(ProcedureEdit edit) {
    return "{"
        + "\"schema_version\":\"eatme.alice-first-lesson-code-editor-action-proof-result/v1\","
        + "\"status\":\"proved\","
        + "\"procedure_selector\":\"" + escapeJson(edit.procedureSelector()) + "\","
        + "\"edited_project_artifact\":\"" + EDITED_PROJECT + "\","
        + "\"action_proof\":\"" + ACTION_PROOF_ARTIFACT + "\","
        + "\"doesNotClaim\":["
        + "\"first-lesson completion\","
        + "\"grading\","
        + "\"creative assessment\","
        + "\"visible rendering correctness\","
        + "\"broad UI automation\""
        + "]"
        + "}";
  }

  private static String actionProofArtifactJson(ProcedureEdit edit) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-first-lesson-code-editor-action-proof/v1\",\n"
        + "  \"status\": \"proved\",\n"
        + "  \"procedure_selector\": \"" + escapeJson(edit.procedureSelector()) + "\",\n"
        + "  \"edit_spec\": \"" + escapeJson(edit.editSpec()) + "\",\n"
        + "  \"input_project_artifact\": \"" + escapeJson(edit.inputProjectArtifact()) + "\",\n"
        + "  \"scene_type\": \"" + escapeJson(edit.sceneType()) + "\",\n"
        + "  \"method_name\": \"" + escapeJson(edit.methodName()) + "\",\n"
        + "  \"selection_mode\": \"in_editor_procedure_tab_operation\",\n"
        + "  \"selected_declaration\": \"" + escapeJson(edit.tabSelection().selectedMethod()) + "\",\n"
        + "  \"code_composite_declaration\": \"" + escapeJson(edit.tabSelection().codeCompositeDeclaration()) + "\",\n"
        + "  \"code_editor_backing\": \"" + escapeJson(edit.tabSelection().codeEditorBacking()) + "\",\n"
        + "  \"code_editor_code\": \"" + escapeJson(edit.tabSelection().codeEditorCode()) + "\",\n"
        + "  \"operation_fired\": true,\n"
        + "  \"action\": \"" + escapeJson(edit.commandResult().command()) + "\",\n"
        + "  \"marker\": \"" + escapeJson(edit.marker()) + "\",\n"
        + "  \"before_statement_count\": " + edit.beforeStatementCount() + ",\n"
        + "  \"after_statement_count\": " + edit.afterStatementCount() + ",\n"
        + "  \"statement_count_delta\": " + (edit.afterStatementCount() - edit.beforeStatementCount()) + ",\n"
        + "  \"target_marker_count\": " + edit.targetMarkerCount() + ",\n"
        + "  \"wrong_target_marker_count\": " + edit.wrongTargetMarkerCount() + ",\n"
        + "  \"before_methods\": " + jsonArray(edit.beforeMethods()) + ",\n"
        + "  \"after_methods\": " + jsonArray(edit.afterMethods()) + ",\n"
        + "  \"edited_project\": \"" + escapeJson(edit.editedProject()) + "\",\n"
        + "  \"success\": true,\n"
        + "  \"doesNotClaim\": [\n"
        + "    \"full first-lesson completion\",\n"
        + "    \"first-lesson completion\",\n"
        + "    \"grading\",\n"
        + "    \"creative assessment\",\n"
        + "    \"visible rendering correctness\",\n"
        + "    \"broad UI automation\",\n"
        + "    \"Save-menu completion\"\n"
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
      String inputProjectArtifact,
      String sceneType,
      String methodName,
      int beforeStatementCount,
      int afterStatementCount,
      String editedProject,
      ProcedureTabSelectionEvidence tabSelection,
      ProcedureEditCommand.Result commandResult,
      List<String> beforeMethods,
      List<String> afterMethods,
      String marker,
      int targetMarkerCount,
      int wrongTargetMarkerCount) {
  }

  record ProcedureTabSelectionEvidence(
      String selectedMethod,
      String codeCompositeDeclaration,
      String codeEditorBacking,
      String codeEditorCode) {
  }

  record MarkerCounts(int target, int outsideTarget) {
    int total() {
      return target + outsideTarget;
    }
  }
}
