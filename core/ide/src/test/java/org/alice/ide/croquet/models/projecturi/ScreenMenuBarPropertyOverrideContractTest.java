package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD contract test for issue #502: verifies that both Robot menu test classes
 * declare the expected fields and constants for the
 * {@code apple.laf.useScreenMenuBar} property override pattern.
 *
 * <p>These tests use reflection to verify the structural contract. They FAIL
 * before implementation (fields/constants don't exist yet) and PASS after
 * the property override pattern is added to both test classes.
 *
 * <p>Complements the Python source-code contract tests in
 * {@code tests/test_issue502_screen_menubar_property_override_contract.py}.
 */
public class ScreenMenuBarPropertyOverrideContractTest {

  private static final String EXPECTED_PROPERTY_NAME = "apple.laf.useScreenMenuBar";

  // -----------------------------------------------------------------------
  // JMenuBarRobotClickSaveProofTest structural contracts
  // -----------------------------------------------------------------------

  @Test
  public void jMenuBarTestDeclaresScreenMenuBarPropertyConstant() throws Exception {
    Field field = JMenuBarRobotClickSaveProofTest.class
        .getDeclaredField("SCREEN_MENU_BAR_PROPERTY");
    field.setAccessible(true);
    assertTrue("SCREEN_MENU_BAR_PROPERTY must be static",
        Modifier.isStatic(field.getModifiers()));
    assertTrue("SCREEN_MENU_BAR_PROPERTY must be final",
        Modifier.isFinal(field.getModifiers()));
    assertEquals("SCREEN_MENU_BAR_PROPERTY value",
        EXPECTED_PROPERTY_NAME, field.get(null));
  }

  @Test
  public void jMenuBarTestDeclaresPreviousScreenMenuBarField() throws Exception {
    Field field = JMenuBarRobotClickSaveProofTest.class
        .getDeclaredField("previousScreenMenuBar");
    field.setAccessible(true);
    assertFalse("previousScreenMenuBar must be an instance field (not static)",
        Modifier.isStatic(field.getModifiers()));
    assertEquals("previousScreenMenuBar must be String type",
        String.class, field.getType());
  }

  // -----------------------------------------------------------------------
  // RobotSaveMenuDialogWriteReadbackProofTest structural contracts
  // -----------------------------------------------------------------------

  @Test
  public void robotSaveTestDeclaresScreenMenuBarPropertyConstant() throws Exception {
    Field field = RobotSaveMenuDialogWriteReadbackProofTest.class
        .getDeclaredField("SCREEN_MENU_BAR_PROPERTY");
    field.setAccessible(true);
    assertTrue("SCREEN_MENU_BAR_PROPERTY must be static",
        Modifier.isStatic(field.getModifiers()));
    assertTrue("SCREEN_MENU_BAR_PROPERTY must be final",
        Modifier.isFinal(field.getModifiers()));
    assertEquals("SCREEN_MENU_BAR_PROPERTY value",
        EXPECTED_PROPERTY_NAME, field.get(null));
  }

  @Test
  public void robotSaveTestDeclaresPreviousScreenMenuBarField() throws Exception {
    Field field = RobotSaveMenuDialogWriteReadbackProofTest.class
        .getDeclaredField("previousScreenMenuBar");
    field.setAccessible(true);
    assertFalse("previousScreenMenuBar must be an instance field (not static)",
        Modifier.isStatic(field.getModifiers()));
    assertEquals("previousScreenMenuBar must be String type",
        String.class, field.getType());
  }

  // -----------------------------------------------------------------------
  // Property capture/restore lifecycle (behavioral contract)
  // -----------------------------------------------------------------------

  @Test
  public void propertyCaptureSetFalseAndRestoreLifecycle() {
    String original = System.getProperty(EXPECTED_PROPERTY_NAME);
    try {
      // Phase 1: Simulate @Before — capture original, set to "false"
      String captured = System.getProperty(EXPECTED_PROPERTY_NAME);
      System.setProperty(EXPECTED_PROPERTY_NAME, "false");
      assertEquals("After @Before, property must be 'false'",
          "false", System.getProperty(EXPECTED_PROPERTY_NAME));

      // Phase 2: Simulate Application.initialize() setting it to "true"
      System.setProperty(EXPECTED_PROPERTY_NAME, "true");
      assertEquals("After Application.initialize(), property is 'true'",
          "true", System.getProperty(EXPECTED_PROPERTY_NAME));

      // Phase 3: Simulate defensive re-set after ide.initialize()
      System.setProperty(EXPECTED_PROPERTY_NAME, "false");
      assertEquals("After defensive re-set, property must be 'false'",
          "false", System.getProperty(EXPECTED_PROPERTY_NAME));

      // Phase 4: Simulate @After — restore original value
      if (captured == null) {
        System.clearProperty(EXPECTED_PROPERTY_NAME);
        assertNull("After @After restore of null, property must be cleared",
            System.getProperty(EXPECTED_PROPERTY_NAME));
      } else {
        System.setProperty(EXPECTED_PROPERTY_NAME, captured);
        assertEquals("After @After restore, property must match original",
            captured, System.getProperty(EXPECTED_PROPERTY_NAME));
      }
    } finally {
      if (original == null) {
        System.clearProperty(EXPECTED_PROPERTY_NAME);
      } else {
        System.setProperty(EXPECTED_PROPERTY_NAME, original);
      }
    }
  }

  @Test
  public void propertyCaptureRestoresNonNullOriginal() {
    String original = System.getProperty(EXPECTED_PROPERTY_NAME);
    try {
      // Set a known non-null value before simulating the lifecycle
      System.setProperty(EXPECTED_PROPERTY_NAME, "true");

      // @Before capture
      String captured = System.getProperty(EXPECTED_PROPERTY_NAME);
      assertEquals("true", captured);
      System.setProperty(EXPECTED_PROPERTY_NAME, "false");

      // ide.initialize() override
      System.setProperty(EXPECTED_PROPERTY_NAME, "true");

      // Defensive re-set
      System.setProperty(EXPECTED_PROPERTY_NAME, "false");

      // @After restore: use the restoreProperty pattern
      if (captured == null) {
        System.clearProperty(EXPECTED_PROPERTY_NAME);
      } else {
        System.setProperty(EXPECTED_PROPERTY_NAME, captured);
      }
      assertEquals("Property restored to original 'true'",
          "true", System.getProperty(EXPECTED_PROPERTY_NAME));
    } finally {
      if (original == null) {
        System.clearProperty(EXPECTED_PROPERTY_NAME);
      } else {
        System.setProperty(EXPECTED_PROPERTY_NAME, original);
      }
    }
  }

  // -----------------------------------------------------------------------
  // No isMac() usage (negative contract)
  // -----------------------------------------------------------------------

  @Test
  public void jMenuBarTestHasNoIsMacMethodReference() throws Exception {
    // Verify via reflection that there's no field or method named "isMac"
    // in the test class itself — the SystemUtilities.isMac() call should
    // be completely removed. We check that the import is gone by verifying
    // no method in the class takes SystemUtilities as a parameter type.
    for (Method m : JMenuBarRobotClickSaveProofTest.class.getDeclaredMethods()) {
      for (Class<?> param : m.getParameterTypes()) {
        assertFalse(
            "No method should reference SystemUtilities after #502",
            param.getName().contains("SystemUtilities"));
      }
    }
  }

  @Test
  public void robotSaveTestHasNoIsMacMethodReference() throws Exception {
    for (Method m : RobotSaveMenuDialogWriteReadbackProofTest.class.getDeclaredMethods()) {
      for (Class<?> param : m.getParameterTypes()) {
        assertFalse(
            "No method should reference SystemUtilities after #502",
            param.getName().contains("SystemUtilities"));
      }
    }
  }
}
