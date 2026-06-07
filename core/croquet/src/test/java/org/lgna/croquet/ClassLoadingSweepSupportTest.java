package org.lgna.croquet;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassLoadingSweepSupportTest {
  @Test
  public void blocksWindowMutatorsAndDialogLaunchers() throws Exception {
    assertBlocked(SweepFixture.class, "showDialog");
    assertBlocked(SweepFixture.class, "setVisible", boolean.class);
    assertBlocked(SweepFixture.class, "dispose");
    assertBlocked(SweepFixture.class, "launch");
    assertBlocked(SweepFixture.class, "openInSystemEditor");
    assertBlocked(SweepFixture.class, "browseForDirectory");
  }

  @Test
  public void leavesAccessorsAvailable() throws Exception {
    assertAllowed(SweepFixture.class, "getDialogTitle");
    assertAllowed(SweepFixture.class, "setDialogTitle", String.class);
    assertAllowed(SweepFixture.class, "isDialogVisible");
  }

  @Test
  public void keepsDocumentFrameDialogsBlocked() throws Exception {
    assertBlocked(DocumentFrame.class, "showSaveFileDialog", File.class, String.class, String.class);
    assertBlocked(DocumentFrame.class, "showOpenFileDialog", String.class, File.class, String.class);
  }

  private static void assertBlocked(Class<?> owner, String methodName, Class<?>... parameterTypes) throws Exception {
    assertTrue(invokeFilter(owner, methodName, parameterTypes));
  }

  private static void assertAllowed(Class<?> owner, String methodName, Class<?>... parameterTypes) throws Exception {
    assertFalse(invokeFilter(owner, methodName, parameterTypes));
  }

  private static boolean invokeFilter(Class<?> owner, String methodName, Class<?>... parameterTypes) throws Exception {
    Method candidate = owner.getDeclaredMethod(methodName, parameterTypes);
    Method filter = ClassLoadingSweepSupport.class.getDeclaredMethod("opensRealModalDialog", Class.class, Method.class);
    filter.setAccessible(true);
    return (Boolean) filter.invoke(null, owner, candidate);
  }

  private static final class SweepFixture {
    public void showDialog() {
    }

    public void setVisible(boolean value) {
    }

    public void dispose() {
    }

    public void launch() {
    }

    public void openInSystemEditor() {
    }

    public void browseForDirectory() {
    }

    public String getDialogTitle() {
      return "dialog";
    }

    public void setDialogTitle(String value) {
    }

    public boolean isDialogVisible() {
      return false;
    }
  }
}
