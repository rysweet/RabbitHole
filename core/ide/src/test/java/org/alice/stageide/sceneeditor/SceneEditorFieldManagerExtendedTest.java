package org.alice.stageide.sceneeditor;

import org.junit.Test;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.JavaType;
import org.lgna.story.SBiped;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SceneEditorFieldManagerExtendedTest {
  @Test
  public void constructorCreatesCodeGeneratorEvenWithNullEditor() throws Exception {
    SceneEditorFieldManager manager = new SceneEditorFieldManager(null);

    Field editorField = SceneEditorFieldManager.class.getDeclaredField("editor");
    editorField.setAccessible(true);
    Field generatorField = SceneEditorFieldManager.class.getDeclaredField("codeGenerator");
    generatorField.setAccessible(true);

    assertNull(editorField.get(manager));
    assertNotNull(generatorField.get(manager));
  }

  @Test
  public void getUndoStatementsForAddFieldDelegatesToCodeGenerator() {
    SceneEditorFieldManager manager = new SceneEditorFieldManager(null);
    UserField rider = createField("rider", SBiped.class);

    org.lgna.project.ast.ExpressionStatement statement =
        (org.lgna.project.ast.ExpressionStatement) manager.getUndoStatementsForAddField(rider)[0];
    MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();

    assertEquals("setVehicle", invocation.method.getValue().getName());
    assertTrue(invocation.requiredArguments.get(0).expression.getValue() instanceof org.lgna.project.ast.NullLiteral);
  }

  @Test
  public void getGoodPointOfViewInSceneForObjectStillThrowsTodo() {
    SceneEditorFieldManager manager = new SceneEditorFieldManager(null);

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> manager.getGoodPointOfViewInSceneForObject(null));

    assertEquals("todo", exception.getMessage());
  }

  @Test
  public void sourceShowsCurrentStateAndSetupForwardToCodeGenerator() throws Exception {
    String source = Files.readString(findSourceFile());

    assertTrue(source.contains("return codeGenerator.getCurrentStateCodeForField(field);"));
    assertTrue(source.contains("codeGenerator.generateCodeForSetUp(bodyStatementsProperty);"));
  }

  @Test
  public void sourceShowsRemoveFieldDelegationToCodeGenerator() throws Exception {
    String source = Files.readString(findSourceFile());

    assertTrue(source.contains("return codeGenerator.getDoStatementsForRemoveField(field, riders);"));
    assertTrue(source.contains("return codeGenerator.getUndoStatementsForRemoveField(field, riders);"));
  }

  private static UserField createField(String name, Class<?> type) {
    UserField field = new UserField();
    field.name.setValue(name);
    field.valueType.setValue(JavaType.getInstance(type));
    return field;
  }

  private static Path findSourceFile() {
    Path fromRoot = Paths.get("core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditorFieldManager.java");
    return Files.exists(fromRoot)
        ? fromRoot
        : Paths.get("src/main/java/org/alice/stageide/sceneeditor/SceneEditorFieldManager.java");
  }
}
