package org.lgna.croquet;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ClassLoadingSweepModalGuardTest {
  @Test
  public void reflectiveSweepDoesNotInvokeDocumentFrameSaveDialog() throws Exception {
    Method modalMethod = DocumentFrame.class.getDeclaredMethod(
        "showSaveFileDialog",
        java.io.File.class,
        String.class,
        String.class);

    Method guard;
    try {
      guard = ClassLoadingSweepSupport.class.getDeclaredMethod("isMethodInvocationAllowedDuringSweep", Method.class);
    } catch (NoSuchMethodException e) {
      fail("ClassLoadingSweepSupport must expose a method-level invocation guard for reflective sweep tests");
      return;
    }
    guard.setAccessible(true);

    assertFalse(
        "The reflective sweep must skip DocumentFrame.showSaveFileDialog to avoid blocking modal file choosers under Xvfb/CI",
        (Boolean) guard.invoke(null, modalMethod));
  }
}
