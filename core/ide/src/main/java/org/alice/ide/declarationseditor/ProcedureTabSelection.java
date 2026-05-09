package org.alice.ide.declarationseditor;

import org.alice.ide.IDE;
import org.alice.ide.codeeditor.CodeEditor;
import org.lgna.croquet.Operation;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.ast.AbstractCode;
import org.lgna.project.ast.UserMethod;

public final class ProcedureTabSelection {
  private ProcedureTabSelection() {
  }

  public static Operation getSelectionOperation(DeclarationsEditorComposite editor, UserMethod procedure) {
    requireProcedure(procedure);
    return editor.getTabState().getItemSelectionOperationForMethod(procedure);
  }

  public static UserMethod selectProcedure(DeclarationsEditorComposite editor, UserMethod procedure) {
    return selectProcedure(editor, procedure, null);
  }

  public static UserMethod selectProcedure(DeclarationsEditorComposite editor, UserMethod procedure, UserActivity activity) {
    requireActiveAliceIde();
    return selectProcedureInEditor(editor, procedure, activity);
  }

  public static UserMethod selectProcedureInEditor(DeclarationsEditorComposite editor, UserMethod procedure, UserActivity activity) {
    Operation operation = getSelectionOperation(editor, procedure);
    operation.fire(activity);
    UserMethod selectedProcedure = getSelectedProcedure(editor);
    if (selectedProcedure != procedure) {
      throw new IllegalStateException("failed to select procedure: " + procedure.getName());
    }
    return selectedProcedure;
  }

  public static UserMethod getSelectedProcedure(DeclarationsEditorComposite editor) {
    CodeComposite selectedProcedureComposite = getSelectedProcedureCodeComposite(editor);
    return selectedProcedureComposite != null ? (UserMethod) selectedProcedureComposite.getDeclaration() : null;
  }

  public static CodeComposite getSelectedProcedureCodeComposite(DeclarationsEditorComposite editor) {
    DeclarationComposite<?, ?> selection = editor.getTabState().getValue();
    if (selection instanceof CodeComposite codeComposite) {
      AbstractCode code = codeComposite.getDeclaration();
      if (code instanceof UserMethod method && method.isProcedure()) {
        return codeComposite;
      }
    }
    return null;
  }

  public static AbstractCode getSelectedCodeEditorCode(DeclarationsEditorComposite editor) {
    CodeEditor codeEditor = getSelectedCodeEditor(editor);
    return codeEditor != null ? codeEditor.getCode() : null;
  }

  public static String getSelectedCodeEditorBackingClassName(DeclarationsEditorComposite editor) {
    CodeEditor codeEditor = getSelectedCodeEditor(editor);
    return codeEditor != null ? codeEditor.getClass().getName() : null;
  }

  private static CodeEditor getSelectedCodeEditor(DeclarationsEditorComposite editor) {
    CodeComposite selectedProcedureComposite = getSelectedProcedureCodeComposite(editor);
    if (selectedProcedureComposite == null) {
      return null;
    }
    if (selectedProcedureComposite.getView().getCodePanelWithDropReceptor() instanceof CodeEditor codeEditor) {
      return codeEditor;
    }
    throw new IllegalStateException(
        "selected procedure tab is not backed by a CodeEditor: " + selectedProcedureComposite.getDeclaration().getName());
  }

  private static void requireProcedure(UserMethod method) {
    if (method == null) {
      throw new IllegalArgumentException("procedure is required");
    }
    if (!method.isProcedure()) {
      throw new IllegalArgumentException("method must be a procedure: " + method.getName());
    }
  }

  private static void requireActiveAliceIde() {
    if (IDE.getActiveInstance() == null) {
      throw new IllegalStateException("procedure selection requires an active Alice IDE");
    }
  }
}
