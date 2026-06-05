package org.lgna.croquet;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ClassLoadingSweepModalGuardTest {
  @Test
  public void reflectiveSweepIdentifiesDocumentFrameModalDialogs() throws Exception {
    Method saveDialog = DocumentFrame.class.getDeclaredMethod(
        "showSaveFileDialog",
        java.io.File.class,
        String.class,
        String.class);
    Method getFrame = DocumentFrame.class.getDeclaredMethod("getFrame");

    Method guard;
    try {
      guard = ClassLoadingSweepSupport.class.getDeclaredMethod("opensRealModalDialog", Class.class, Method.class);
    } catch (NoSuchMethodException e) {
      fail("ClassLoadingSweepSupport must keep the reflective sweep modal-dialog guard isolated");
      return;
    }
    guard.setAccessible(true);

    assertTrue(
        "The reflective sweep must skip DocumentFrame.showSaveFileDialog to avoid blocking modal file choosers under Xvfb/CI",
        (Boolean) guard.invoke(null, DocumentFrame.class, saveDialog));
    assertFalse(
        "The modal guard must stay narrow and allow ordinary DocumentFrame methods to be exercised",
        (Boolean) guard.invoke(null, DocumentFrame.class, getFrame));
  }
}
