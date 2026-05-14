package org.lgna.story.implementation.alice;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TDD characterization tests for ResourceEnumResolver.
 *
 * These tests define the contract for enum↔camelCase conversion and
 * resource name resolution that will be extracted from AliceResourceUtilities.
 * They verify behavior parity with the original monolithic implementation.
 */
public class ResourceEnumResolverTest {

  // ── enumToCamelCase(String, boolean) ────────────────────

  @Test
  public void enumToCamelCase_simpleUpperSnake_returnsUpperCamelCase() {
    assertEquals("HelloWorld", ResourceEnumResolver.enumToCamelCase("HELLO_WORLD", false));
  }

  @Test
  public void enumToCamelCase_startWithLowerCase_returnsLowerCamelCase() {
    assertEquals("helloWorld", ResourceEnumResolver.enumToCamelCase("HELLO_WORLD", true));
  }

  @Test
  public void enumToCamelCase_singleWord_returnsCapitalized() {
    assertEquals("Default", ResourceEnumResolver.enumToCamelCase("DEFAULT", false));
  }

  @Test
  public void enumToCamelCase_singleWordLower_returnsLowercase() {
    assertEquals("default", ResourceEnumResolver.enumToCamelCase("DEFAULT", true));
  }

  @Test
  public void enumToCamelCase_emptyString_returnsEmpty() {
    assertEquals("", ResourceEnumResolver.enumToCamelCase("", false));
  }

  @Test
  public void enumToCamelCase_singleChar_returnsCapitalized() {
    assertEquals("A", ResourceEnumResolver.enumToCamelCase("A", false));
  }

  @Test
  public void enumToCamelCase_withDigits_preservesDigits() {
    assertEquals("Model2Variant", ResourceEnumResolver.enumToCamelCase("MODEL_2_VARIANT", false));
  }

  @Test
  public void enumToCamelCase_trailingUnderscore_handledGracefully() {
    // Trailing underscore: next char after _ should be uppercased, but there is none
    assertEquals("Hello", ResourceEnumResolver.enumToCamelCase("HELLO_", false));
  }

  // ── enumToCamelCase(String) — single-arg overload ───────

  @Test
  public void enumToCamelCase_oneArg_defaultsToUpperStart() {
    assertEquals("HelloWorld", ResourceEnumResolver.enumToCamelCase("HELLO_WORLD"));
  }

  // ── camelCaseToEnum ─────────────────────────────────────

  @Test
  public void camelCaseToEnum_simpleCamelCase_returnsUpperSnake() {
    assertEquals("HELLO_WORLD", ResourceEnumResolver.camelCaseToEnum("HelloWorld"));
  }

  @Test
  public void camelCaseToEnum_lowerCamelCase_returnsUpperSnake() {
    assertEquals("HELLO_WORLD", ResourceEnumResolver.camelCaseToEnum("helloWorld"));
  }

  @Test
  public void camelCaseToEnum_singleWord_returnsUppercase() {
    assertEquals("DEFAULT", ResourceEnumResolver.camelCaseToEnum("Default"));
  }

  @Test
  public void camelCaseToEnum_allUppercase_insertsUnderscoresEverywhere() {
    // Each uppercase letter after index 0 gets a preceding underscore
    assertEquals("A_B_C", ResourceEnumResolver.camelCaseToEnum("ABC"));
  }

  @Test
  public void camelCaseToEnum_emptyString_returnsEmpty() {
    assertEquals("", ResourceEnumResolver.camelCaseToEnum(""));
  }

  // ── isEnumName ──────────────────────────────────────────

  @Test
  public void isEnumName_allCapsAndUnderscores_returnsTrue() {
    assertTrue(ResourceEnumResolver.isEnumName("HELLO_WORLD"));
  }

  @Test
  public void isEnumName_withDigits_returnsTrue() {
    assertTrue(ResourceEnumResolver.isEnumName("MODEL_2"));
  }

  @Test
  public void isEnumName_lowercaseChars_returnsFalse() {
    assertFalse(ResourceEnumResolver.isEnumName("Hello"));
  }

  @Test
  public void isEnumName_emptyString_returnsTrue() {
    // No chars violate the rule, so vacuously true
    assertTrue(ResourceEnumResolver.isEnumName(""));
  }

  @Test
  public void isEnumName_singleLowercaseChar_returnsFalse() {
    assertFalse(ResourceEnumResolver.isEnumName("a"));
  }

  // ── makeEnumName ────────────────────────────────────────

  @Test
  public void makeEnumName_alreadyEnum_returnsSame() {
    assertEquals("HELLO_WORLD", ResourceEnumResolver.makeEnumName("HELLO_WORLD"));
  }

  @Test
  public void makeEnumName_camelCase_convertsToUpperSnake() {
    assertEquals("HELLO_WORLD", ResourceEnumResolver.makeEnumName("HelloWorld"));
  }

  @Test
  public void makeEnumName_containsUnderscore_uppercases() {
    // If name contains underscore but not all caps, just toUpperCase
    assertEquals("HELLO_WORLD", ResourceEnumResolver.makeEnumName("hello_world"));
  }

  @Test
  public void makeEnumName_mixedCaseNoUnderscore_convertsCamelCase() {
    assertEquals("MY_MODEL", ResourceEnumResolver.makeEnumName("MyModel"));
  }

  // ── makeLocalizationKey ─────────────────────────────────

  @Test
  public void makeLocalizationKey_spacesToUnderscores() {
    assertEquals("hello_world", ResourceEnumResolver.makeLocalizationKey("hello world"));
  }

  @Test
  public void makeLocalizationKey_noSpaces_returnsSame() {
    assertEquals("hello", ResourceEnumResolver.makeLocalizationKey("hello"));
  }

  @Test
  public void makeLocalizationKey_multipleSpaces_allConverted() {
    assertEquals("a_b_c", ResourceEnumResolver.makeLocalizationKey("a b c"));
  }

  // ── arrayToEnum ─────────────────────────────────────────

  @Test
  public void arrayToEnum_fullRange_joinsWithUnderscores() {
    String[] arr = {"hello", "world"};
    assertEquals("HELLO_WORLD", ResourceEnumResolver.arrayToEnum(arr, 0, 2));
  }

  @Test
  public void arrayToEnum_subRange_joinsSubset() {
    String[] arr = {"a", "b", "c", "d"};
    assertEquals("B_C", ResourceEnumResolver.arrayToEnum(arr, 1, 3));
  }

  @Test
  public void arrayToEnum_emptyRange_returnsEmpty() {
    String[] arr = {"a", "b"};
    assertEquals("", ResourceEnumResolver.arrayToEnum(arr, 1, 1));
  }

  @Test
  public void arrayToEnum_skipsEmptyElements() {
    String[] arr = {"hello", "", "world"};
    assertEquals("HELLO_WORLD", ResourceEnumResolver.arrayToEnum(arr, 0, 3));
  }

  // ── getDefaultTextureEnumName ───────────────────────────

  @Test
  public void getDefaultTextureEnumName_alwaysReturnsDefault() {
    assertEquals("DEFAULT", ResourceEnumResolver.getDefaultTextureEnumName("anything"));
  }

  @Test
  public void getDefaultTextureEnumName_nullInput_stillReturnsDefault() {
    assertEquals("DEFAULT", ResourceEnumResolver.getDefaultTextureEnumName(null));
  }

  // ── getModelNameFromClassAndResource / getTextureNameFromClassAndResource ──
  // These require ModelResource instances (integration-level). Tested via
  // AliceResourceUtilitiesDelegateTest to avoid coupling to resource data.

  @Test
  public void getTextureNameFromClassAndResource_nullResourceName_returnsNull() {
    // This is a documented contract: null resourceName means class-level lookup
    // which returns null for texture name
    assertNull(ResourceEnumResolver.getTextureNameFromClassAndResource(new StubModelResource(), null));
  }

  @Test
  public void getModelNameFromClassAndResource_nullResourceName_returnsClassName() {
    // null resourceName: falls back to getName(resource.getClass()), which
    // returns the alice class name derived from the simple name
    StubModelResource stub = new StubModelResource();
    String result = ResourceEnumResolver.getModelNameFromClassAndResource(stub, null);
    assertNotNull(result);
  }

  // ── ResourceNames inner class contract ──────────────────

  @Test
  public void resourceNamesCache_storesVisualAndTexturePair() throws Exception {
    // Verify that the ResourceNames inner class exists in ResourceEnumResolver
    // and has visualName and textureName fields (reflection test for structural contract)
    Class<?> resourceNamesClass = Class.forName(ResourceEnumResolver.class.getName() + "$ResourceNames");
    assertNotNull("ResourceNames inner class should exist", resourceNamesClass);
    assertNotNull("Should have visualName field", resourceNamesClass.getDeclaredField("visualName"));
    assertNotNull("Should have textureName field", resourceNamesClass.getDeclaredField("textureName"));
  }

  @Test
  public void resourceIdentifierToResourceNamesMap_existsInEnumResolver() throws Exception {
    // The cache field should have moved from AliceResourceUtilities to ResourceEnumResolver
    assertNotNull("Cache field should exist",
        ResourceEnumResolver.class.getDeclaredField("resourceIdentifierToResourceNamesMap"));
  }

  // ── Stub implementation for testing ─────────────────────

  /**
   * Minimal ModelResource stub for unit tests that don't need real resource data.
   */
  private static class StubModelResource implements org.lgna.story.resources.ModelResource {
    @Override
    public String toString() {
      return "STUB";
    }
  }
}
