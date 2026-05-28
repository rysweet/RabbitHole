package org.alice.ide.clipboard;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Structural tests for clipboard operation classes.
 */
public class ClipboardOperationStructureTest {

  // ---- CopyToClipboardOperation ----

  @Test
  public void copyToClipboardOperation_classIsAccessible() {
    assertNotNull(CopyToClipboardOperation.class);
  }

  @Test
  public void copyToClipboardOperation_isPublic() {
    assertTrue(Modifier.isPublic(CopyToClipboardOperation.class.getModifiers()));
  }

  @Test
  public void copyToClipboardOperation_isNotAbstract() {
    assertFalse(Modifier.isAbstract(CopyToClipboardOperation.class.getModifiers()));
  }

  // ---- CutToClipboardOperation ----

  @Test
  public void cutToClipboardOperation_classIsAccessible() {
    assertNotNull(CutToClipboardOperation.class);
  }

  @Test
  public void cutToClipboardOperation_isPublic() {
    assertTrue(Modifier.isPublic(CutToClipboardOperation.class.getModifiers()));
  }

  @Test
  public void cutToClipboardOperation_isNotAbstract() {
    assertFalse(Modifier.isAbstract(CutToClipboardOperation.class.getModifiers()));
  }

  // ---- PasteFromClipboardOperation ----

  @Test
  public void pasteFromClipboardOperation_classIsAccessible() {
    assertNotNull(PasteFromClipboardOperation.class);
  }

  @Test
  public void pasteFromClipboardOperation_isPublic() {
    assertTrue(Modifier.isPublic(PasteFromClipboardOperation.class.getModifiers()));
  }

  // ---- CopyFromClipboardOperation ----

  @Test
  public void copyFromClipboardOperation_classIsAccessible() {
    assertNotNull(CopyFromClipboardOperation.class);
  }

  @Test
  public void copyFromClipboardOperation_isPublic() {
    assertTrue(Modifier.isPublic(CopyFromClipboardOperation.class.getModifiers()));
  }

  // ---- FromClipboardOperation ----

  @Test
  public void fromClipboardOperation_classIsAccessible() {
    assertNotNull(FromClipboardOperation.class);
  }

  @Test
  public void fromClipboardOperation_isPublic() {
    assertTrue(Modifier.isPublic(FromClipboardOperation.class.getModifiers()));
  }

  @Test
  public void fromClipboardOperation_isAbstract() {
    assertTrue(Modifier.isAbstract(FromClipboardOperation.class.getModifiers()));
  }

  // ---- DragReceptorState ----

  @Test
  public void dragReceptorState_classIsAccessible() {
    assertNotNull(DragReceptorState.class);
  }

  @Test
  public void dragReceptorState_isPublic() {
    assertTrue(Modifier.isPublic(DragReceptorState.class.getModifiers()));
  }

  // ---- FromClipboard subclasses extend base ----

  @Test
  public void copyFromClipboard_extendsFromClipboard() {
    assertTrue(FromClipboardOperation.class.isAssignableFrom(CopyFromClipboardOperation.class));
  }

  @Test
  public void pasteFromClipboard_extendsFromClipboard() {
    assertTrue(FromClipboardOperation.class.isAssignableFrom(PasteFromClipboardOperation.class));
  }

  // ---- All clipboard operations are public ----

  @Test
  public void allClipboardOperations_arePublic() {
    Class<?>[] classes = {
      CopyToClipboardOperation.class, CutToClipboardOperation.class,
      PasteFromClipboardOperation.class, CopyFromClipboardOperation.class,
      FromClipboardOperation.class, DragReceptorState.class
    };
    for (Class<?> cls : classes) {
      assertTrue(cls.getSimpleName() + " should be public",
        Modifier.isPublic(cls.getModifiers()));
    }
  }
}
