package org.alice.stageide.modelresource;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link RootResourceKey} — the root node key in the resource gallery tree.
 */
public class RootResourceKeyTest {

  private RootResourceKey createKey(String keyText, String defaultText) {
    return new RootResourceKey(keyText, defaultText);
  }

  // ---- getSearchText ----

  @Test
  public void getSearchText_returnsNull() {
    RootResourceKey key = createKey("allClasses", "All Classes");
    assertNull(key.getSearchText());
  }

  // ---- getInternalName ----

  @Test
  public void getInternalName_returnsDefaultDisplayText() {
    RootResourceKey key = createKey("allClasses", "All Classes");
    assertEquals("All Classes", key.getInternalName());
  }

  // ---- getLocalizedCreationText ----

  @Test
  public void getLocalizedCreationText_returnsLocalizedName() {
    RootResourceKey key = createKey("nonExistentKey", "Fallback Name");
    // Since the key won't exist in the resource bundle, it should fall back to default
    String text = key.getLocalizedCreationText();
    assertNotNull(text);
  }

  // ---- isLeaf ----

  @Test
  public void isLeaf_returnsFalse() {
    RootResourceKey key = createKey("root", "Root");
    assertFalse(key.isLeaf());
  }

  // ---- isInstanceCreator ----

  @Test
  public void isInstanceCreator_returnsFalse() {
    RootResourceKey key = createKey("root", "Root");
    assertFalse(key.isInstanceCreator());
  }

  // ---- getIconFactory ----

  @Test
  public void getIconFactory_returnsNull() {
    RootResourceKey key = createKey("root", "Root");
    assertNull(key.getIconFactory());
  }

  // ---- createInstanceCreation ----

  @Test(expected = Error.class)
  public void createInstanceCreation_throwsError() {
    RootResourceKey key = createKey("root", "Root");
    key.createInstanceCreation(new java.util.HashSet<>());
  }

  // ---- getTags ----

  @Test
  public void getTags_returnsNull() {
    RootResourceKey key = createKey("root", "Root");
    assertNull(key.getTags());
  }

  @Test
  public void getGroupTags_returnsNull() {
    RootResourceKey key = createKey("root", "Root");
    assertNull(key.getGroupTags());
  }

  @Test
  public void getThemeTags_returnsNull() {
    RootResourceKey key = createKey("root", "Root");
    assertNull(key.getThemeTags());
  }

  // ---- getLeftClickOperation / getDropOperation ----

  @Test
  public void getLeftClickOperation_returnsNull() {
    RootResourceKey key = createKey("root", "Root");
    assertNull(key.getLeftClickOperation(null, null));
  }

  @Test
  public void getDropOperation_returnsNull() {
    RootResourceKey key = createKey("root", "Root");
    assertNull(key.getDropOperation(null, null, null));
  }

  // ---- toString ----

  @Test
  public void toString_containsClassName() {
    RootResourceKey key = createKey("root", "Root");
    String str = key.toString();
    assertTrue(str.contains("RootResourceKey"));
  }

  // ---- getLocalizedName with missing bundle key uses default ----

  @Test
  public void getLocalizedName_missingBundleKey_fallsBackToDefault() {
    RootResourceKey key = createKey("thisKeyWontExistInAnyBundle", "Default Value");
    assertEquals("Default Value", key.getLocalizedName());
  }
}
