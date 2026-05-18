package org.alice.stageide.sceneeditor;

import org.junit.Test;

import javax.swing.Icon;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class SceneEditorInitializerExtendedTest {
  @Test
  public void constructorRetainsEditorReference() throws Exception {
    StorytellingSceneEditor editor = (StorytellingSceneEditor) getUnsafe().allocateInstance(StorytellingSceneEditor.class);
    SceneEditorInitializer initializer = new SceneEditorInitializer(editor);

    Field editorField = SceneEditorInitializer.class.getDeclaredField("editor");
    editorField.setAccessible(true);
    assertSame(editor, editorField.get(initializer));
  }

  @Test
  public void iconFieldsAreFinalAndNonNull() throws Exception {
    Field expandField = SceneEditorInitializer.class.getDeclaredField("EXPAND_ICON");
    Field contractField = SceneEditorInitializer.class.getDeclaredField("CONTRACT_ICON");
    expandField.setAccessible(true);
    contractField.setAccessible(true);

    assertTrue(Modifier.isFinal(expandField.getModifiers()));
    assertTrue(Modifier.isFinal(contractField.getModifiers()));
    assertNotNull(expandField.get(null));
    assertNotNull(contractField.get(null));
  }

  @Test
  public void iconFieldsUseTwentyFourPixelIcons() throws Exception {
    Icon expand = getIcon("EXPAND_ICON");
    Icon contract = getIcon("CONTRACT_ICON");

    assertEquals(24, expand.getIconWidth());
    assertEquals(24, expand.getIconHeight());
    assertEquals(24, contract.getIconWidth());
    assertEquals(24, contract.getIconHeight());
  }

  @Test
  public void initializeMethodIsDeclaredExactlyOnce() {
    long count = java.util.Arrays.stream(SceneEditorInitializer.class.getDeclaredMethods())
        .filter(method -> method.getName().equals("initialize"))
        .count();
    assertEquals(1, count);
  }

  private static Icon getIcon(String name) throws Exception {
    Field field = SceneEditorInitializer.class.getDeclaredField(name);
    field.setAccessible(true);
    return (Icon) field.get(null);
  }

  private static sun.misc.Unsafe getUnsafe() throws Exception {
    Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    return (sun.misc.Unsafe) field.get(null);
  }
}
