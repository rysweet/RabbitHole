package edu.cmu.cs.dennisc.java.lang;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for SystemUtilities — boolean property accessors, platform detection,
 * bit count, Java version, path parsing, and array creation.
 */
public class SystemUtilitiesTest {

  // --- Boolean property accessors ---

  @Test
  public void isPropertyTrue_setTrue() {
    String key = "test.system.utilities.true";
    System.setProperty(key, "true");
    try {
      assertTrue(SystemUtilities.isPropertyTrue(key));
    } finally {
      System.clearProperty(key);
    }
  }

  @Test
  public void isPropertyTrue_notSet() {
    assertFalse(SystemUtilities.isPropertyTrue("test.nonexistent.property.xyz"));
  }

  @Test
  public void isPropertyFalse_setFalse() {
    String key = "test.system.utilities.false";
    System.setProperty(key, "false");
    try {
      assertTrue(SystemUtilities.isPropertyFalse(key));
    } finally {
      System.clearProperty(key);
    }
  }

  @Test
  public void isPropertyFalse_notSet() {
    assertFalse(SystemUtilities.isPropertyFalse("test.nonexistent.property.xyz2"));
  }

  @Test
  public void getBooleanProperty_withDefault_true() {
    String key = "test.system.boolean.def";
    boolean result = SystemUtilities.getBooleanProperty(key, true);
    assertTrue(result);
  }

  @Test
  public void getBooleanProperty_setTrue() {
    String key = "test.system.boolean.set";
    System.setProperty(key, "true");
    try {
      assertTrue(SystemUtilities.getBooleanProperty(key, false));
    } finally {
      System.clearProperty(key);
    }
  }

  @Test
  public void getBooleanProperty_setFalse() {
    String key = "test.system.boolean.setf";
    System.setProperty(key, "false");
    try {
      assertFalse(SystemUtilities.getBooleanProperty(key, true));
    } finally {
      System.clearProperty(key);
    }
  }

  // --- Platform detection ---

  @Test
  public void platformDetection_exactlyOne() {
    boolean linux = SystemUtilities.isLinux();
    boolean mac = SystemUtilities.isMac();
    boolean windows = SystemUtilities.isWindows();
    // Exactly one should be true
    int count = (linux ? 1 : 0) + (mac ? 1 : 0) + (windows ? 1 : 0);
    assertEquals("Exactly one platform should be detected", 1, count);
  }

  @Test
  public void isLinux_onLinux() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("linux")) {
      assertTrue(SystemUtilities.isLinux());
    }
  }

  // --- Bit count ---

  @Test
  public void getBitCount_notNull() {
    Integer bits = SystemUtilities.getBitCount();
    assertNotNull("Expected non-null on x86_64 Linux", bits);
    assertTrue("Expected 32 or 64, got " + bits, bits == 32 || bits == 64);
  }

  @Test
  public void is64Bit_or_is32Bit() {
    Integer bits = SystemUtilities.getBitCount();
    if (bits != null) {
      if (bits == 64) {
        assertTrue(SystemUtilities.is64Bit());
        assertFalse(SystemUtilities.is32Bit());
      } else if (bits == 32) {
        assertFalse(SystemUtilities.is64Bit());
        assertTrue(SystemUtilities.is32Bit());
      }
    }
  }

  // --- Java version ---

  @Test
  public void getJavaVersionAsDouble_positive() {
    double version = SystemUtilities.getJavaVersionAsDouble();
    assertTrue("Java version should be > 0", version > 0);
  }

  @Test
  public void getJavaVersionAsDouble_atLeast1_8() {
    double version = SystemUtilities.getJavaVersionAsDouble();
    assertTrue("Java version should be >= 1.8", version >= 1.8);
  }

  // --- Path parsing ---

  @Test
  public void getClassPath_notEmpty() {
    String[] classPath = SystemUtilities.getClassPath();
    assertNotNull(classPath);
    assertTrue(classPath.length > 0);
  }

  @Test
  public void getLibraryPath_notNull() {
    String[] libPath = SystemUtilities.getLibraryPath();
    assertNotNull(libPath);
  }

  @Test
  public void PATH_SEPARATOR_notNull() {
    assertNotNull(SystemUtilities.PATH_SEPARATOR);
    assertTrue(SystemUtilities.PATH_SEPARATOR.length() > 0);
  }

  // --- Array creation ---

  @Test
  public void returnArray_returnsInput() {
    String[] input = {"a", "b", "c"};
    String[] result = SystemUtilities.returnArray(String.class, input);
    assertArrayEquals(input, result);
  }

  @Test
  public void createArray_concatenatesArrays() {
    String[] a1 = {"a", "b"};
    String[] a2 = {"c", "d"};
    @SuppressWarnings("unchecked")
    String[] result = SystemUtilities.createArray(String.class, a1, a2);
    assertNotNull(result);
    assertEquals(4, result.length);
    assertEquals("a", result[0]);
    assertEquals("d", result[3]);
  }

  @Test
  public void createArray_singleArray() {
    Integer[] a = {1, 2, 3};
    @SuppressWarnings("unchecked")
    Integer[] result = SystemUtilities.createArray(Integer.class, a);
    assertEquals(3, result.length);
  }

  @Test
  public void createArray_emptyArrays() {
    String[] a1 = {};
    String[] a2 = {};
    @SuppressWarnings("unchecked")
    String[] result = SystemUtilities.createArray(String.class, a1, a2);
    assertNotNull(result);
    assertEquals(0, result.length);
  }

  // --- Properties as XML ---

  @Test
  public void getPropertiesAsXMLByteArray_notEmpty() {
    byte[] xml = SystemUtilities.getPropertiesAsXMLByteArray();
    assertNotNull(xml);
    assertTrue(xml.length > 0);
  }

  @Test
  public void getPropertiesAsXMLString_containsProperties() {
    String xml = SystemUtilities.getPropertiesAsXMLString();
    assertNotNull(xml);
    assertTrue(xml.length() > 0);
  }

  // --- Property list ---

  @Test
  public void getPropertyList_notEmpty() {
    List<SystemProperty> props = SystemUtilities.getPropertyList();
    assertNotNull(props);
    assertFalse(props.isEmpty());
  }

  @Test
  public void getSortedPropertyList_isSorted() {
    List<SystemProperty> sorted = SystemUtilities.getSortedPropertyList();
    assertNotNull(sorted);
    assertFalse(sorted.isEmpty());
    for (int i = 1; i < sorted.size(); i++) {
      assertTrue("List should be sorted by key",
          sorted.get(i - 1).getKey().compareTo(sorted.get(i).getKey()) <= 0);
    }
  }

  // --- isArmArchitecture ---

  @Test
  public void isArmArchitecture_returnsBooleanWithoutThrowing() {
    boolean result = SystemUtilities.isArmArchitecture();
    // Verify consistency with os.arch — implementation checks for "arm" only
    String arch = System.getProperty("os.arch", "").toLowerCase(java.util.Locale.ENGLISH);
    if (arch.contains("arm")) {
      assertTrue("Should be true when os.arch contains 'arm'", result);
    } else {
      assertFalse("Should be false when os.arch does not contain 'arm' (" + arch + ")", result);
    }
  }

  // --- getEnvironmentVariableDirectory ---

  @Test
  public void getEnvironmentVariableDirectory_home() {
    // HOME is typically set on Linux
    if (System.getenv("HOME") != null) {
      java.io.File dir = SystemUtilities.getEnvironmentVariableDirectory("HOME");
      assertNotNull(dir);
      assertTrue(dir.exists());
    }
  }

  @Test
  public void getEnvironmentVariableDirectory_nonExistent() {
    // getEnvironmentVariableDirectory uses assert which throws AssertionError
    // when assertions are enabled; without assertions it returns a File
    try {
      java.io.File dir = SystemUtilities.getEnvironmentVariableDirectory("NONEXISTENT_VAR_XYZ_123");
      // If assertions disabled, verify it doesn't return a valid directory
      assertFalse("Should not return an existing directory for unset env var", dir.isDirectory());
    } catch (AssertionError expected) {
      // Expected: getEnvironmentVariableDirectory asserts env var is non-null
    }
  }
}
