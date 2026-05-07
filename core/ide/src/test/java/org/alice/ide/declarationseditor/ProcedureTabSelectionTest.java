package org.alice.ide.declarationseditor;

import org.junit.Test;
import org.alice.ide.IDE;
import org.lgna.croquet.Operation;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.story.SScene;

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
}
