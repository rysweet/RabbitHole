package org.alice.ide.ast.type.merge.help.diffsig.croquet;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link DifferentSignatureChoice} enum.
 */
public class DifferentSignatureChoiceTest {

  @Test
  public void values_hasExpectedCount() {
    assertEquals(2, DifferentSignatureChoice.values().length);
  }

  @Test
  public void valueOf_addAndRetainBoth() {
    assertEquals(DifferentSignatureChoice.ADD_AND_RETAIN_BOTH,
        DifferentSignatureChoice.valueOf("ADD_AND_RETAIN_BOTH"));
  }

  @Test
  public void valueOf_onlyRetainVersionAlreadyInProject() {
    assertEquals(DifferentSignatureChoice.ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT,
        DifferentSignatureChoice.valueOf("ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT"));
  }

  @Test
  public void ordinal_addAndRetainBothIsZero() {
    assertEquals(0, DifferentSignatureChoice.ADD_AND_RETAIN_BOTH.ordinal());
  }

  @Test
  public void ordinal_onlyRetainIsOne() {
    assertEquals(1, DifferentSignatureChoice.ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT.ordinal());
  }

  @Test
  public void name_matchesEnumConstant() {
    assertEquals("ADD_AND_RETAIN_BOTH",
        DifferentSignatureChoice.ADD_AND_RETAIN_BOTH.name());
    assertEquals("ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT",
        DifferentSignatureChoice.ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT.name());
  }

  @Test
  public void toString_returnsName() {
    for (DifferentSignatureChoice choice : DifferentSignatureChoice.values()) {
      assertEquals(choice.name(), choice.toString());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalid_throwsException() {
    DifferentSignatureChoice.valueOf("NONEXISTENT");
  }

  @Test
  public void allValues_areDistinct() {
    DifferentSignatureChoice[] values = DifferentSignatureChoice.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i], values[j]);
      }
    }
  }
}
