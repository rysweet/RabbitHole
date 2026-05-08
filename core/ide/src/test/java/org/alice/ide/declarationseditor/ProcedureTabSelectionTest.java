package org.alice.ide.declarationseditor;

import org.junit.Test;
import org.alice.ide.IDE;
import org.lgna.croquet.Application;
import org.lgna.croquet.DocumentFrame;
import org.lgna.croquet.Operation;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.ast.AbstractCode;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.story.SScene;

import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import javax.swing.SwingUtilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ProcedureTabSelectionTest {
  @Test
  public void selectionOperationTargetsProcedureCodeTab() {
    DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
    UserMethod procedure = sceneProcedure("eatmeFirstLesson");

    Operation operation = ProcedureTabSelection.getSelectionOperation(editor, procedure);
    operation.initializeIfNecessary();

    assertNotNull(operation);
    assertEquals("eatmeFirstLesson", operation.getImp().getName());
    assertSame(IDE.DOCUMENT_UI_GROUP, operation.getGroup());
    assertNull(ProcedureTabSelection.getSelectedProcedure(editor));
    assertNull(ProcedureTabSelection.getSelectedProcedureCodeComposite(editor));
    assertNull(ProcedureTabSelection.getSelectedCodeEditorCode(editor));
  }

  @Test
  public void selectProcedureRequiresActiveAliceIde() {
    DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
    UserMethod procedure = sceneProcedure("eatmeFirstLesson");

    try {
      ProcedureTabSelection.selectProcedure(editor, procedure);
    } catch (IllegalStateException ex) {
      assertEquals("procedure selection requires an active Alice IDE", ex.getMessage());
      assertNull(ProcedureTabSelection.getSelectedProcedure(editor));
      return;
    }
    throw new AssertionError("selectProcedure should require an active Alice IDE");
  }

  @Test
  public void selectionOperationCanOpenProcedureTabInEditor() throws Exception {
    DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
    UserMethod procedure = sceneProcedure("eatmeFirstLesson");
    ensureCroquetApplication();
    editor.getTabState().getData().internalSetAllItems(List.of(CodeComposite.getInstance(procedure)));
    UserMethod[] selected = new UserMethod[1];

    SwingUtilities.invokeAndWait(() -> selected[0] = ProcedureTabSelection.selectProcedureInEditor(editor, procedure, null));

    assertSame(procedure, selected[0]);
    assertSame(procedure, ProcedureTabSelection.getSelectedProcedure(editor));
  }

  @Test
  public void selectProcedureLandsOnCodeEditorBackedByExpectedMethodCode() throws Exception {
    DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
    UserMethod procedure = sceneProcedure("eatmeFirstLesson");
    CodeComposite expectedComposite = CodeComposite.getInstance(procedure);
    ensureCroquetApplication();
    editor.getTabState().getData().internalSetAllItems(List.of(expectedComposite));
    UserMethod[] selected = new UserMethod[1];
    CodeComposite[] selectedComposite = new CodeComposite[1];
    AbstractCode[] selectedCodeEditorCode = new AbstractCode[1];

    SwingUtilities.invokeAndWait(() -> {
      selected[0] = ProcedureTabSelection.selectProcedureInEditor(editor, procedure, null);
      selectedComposite[0] = ProcedureTabSelection.getSelectedProcedureCodeComposite(editor);
      selectedCodeEditorCode[0] = ProcedureTabSelection.getSelectedCodeEditorCode(editor);
    });

    assertSame(procedure, selected[0]);
    assertSame(procedure, ProcedureTabSelection.getSelectedProcedure(editor));
    assertSame(expectedComposite, selectedComposite[0]);
    assertSame(procedure, selectedComposite[0].getDeclaration());
    assertSame(procedure, selectedCodeEditorCode[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsFunctionWhenProcedureSelectionIsRequired() {
    DeclarationsEditorComposite editor = new DeclarationsEditorComposite();
    UserMethod function = new UserMethod("answer", JavaType.INTEGER_PRIMITIVE_TYPE, new UserParameter[0], new BlockStatement());

    ProcedureTabSelection.getSelectionOperation(editor, function);
  }

  private static UserMethod sceneProcedure(String name) {
    NamedUserType sceneType = AstUtilities.createType("Scene", JavaType.getInstance(SScene.class));
    UserMethod procedure = new UserMethod(name, JavaType.VOID_TYPE, new UserParameter[0], new BlockStatement());
    sceneType.methods.add(procedure);
    return procedure;
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
        protected void handleOpenFiles(List<File> files) {
        }

        @Override
        protected void handleWindowOpened(WindowEvent e) {
        }

        @Override
        public void handleQuit(UserActivity activity) {
        }

        @Override
        public String getApplicationSubPath() {
          return "procedure-tab-selection-test";
        }
      };
    }
  }
}
