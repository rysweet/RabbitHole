package org.alice.ide.declarationseditor;

import org.alice.ide.IDE;
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
    Operation operation = getSelectionOperation(editor, procedure);
    requireActiveAliceIde();
    operation.fire(activity);
    UserMethod selectedProcedure = getSelectedProcedure(editor);
    if (selectedProcedure != procedure) {
      throw new IllegalStateException("failed to select procedure: " + procedure.getName());
    }
    return selectedProcedure;
  }

  public static UserMethod getSelectedProcedure(DeclarationsEditorComposite editor) {
    DeclarationComposite<?, ?> selection = editor.getTabState().getValue();
    if (selection instanceof CodeComposite codeComposite) {
      AbstractCode code = codeComposite.getDeclaration();
      if (code instanceof UserMethod method && method.isProcedure()) {
        return method;
      }
    }
    return null;
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
