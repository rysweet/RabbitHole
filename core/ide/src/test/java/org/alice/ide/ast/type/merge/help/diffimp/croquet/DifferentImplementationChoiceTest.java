package org.alice.ide.ast.type.merge.help.diffimp.croquet;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link DifferentImplementationChoice} enum.
 */
public class DifferentImplementationChoiceTest {

  @Test
  public void values_hasExpectedCount() {
    assertEquals(3, DifferentImplementationChoice.values().length);
  }

  @Test
  public void valueOf_addAndRetainBoth() {
    assertEquals(DifferentImplementationChoice.ADD_AND_RETAIN_BOTH,
        DifferentImplementationChoice.valueOf("ADD_AND_RETAIN_BOTH"));
  }

  @Test
  public void valueOf_onlyAddVersionInClassFile() {
    assertEquals(DifferentImplementationChoice.ONLY_ADD_VERSION_IN_CLASS_FILE,
        DifferentImplementationChoice.valueOf("ONLY_ADD_VERSION_IN_CLASS_FILE"));
  }

  @Test
  public void valueOf_onlyRetainVersionAlreadyInProject() {
    assertEquals(DifferentImplementationChoice.ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT,
        DifferentImplementationChoice.valueOf("ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT"));
  }

  @Test
  public void ordinals_areSequential() {
    assertEquals(0, DifferentImplementationChoice.ADD_AND_RETAIN_BOTH.ordinal());
    assertEquals(1, DifferentImplementationChoice.ONLY_ADD_VERSION_IN_CLASS_FILE.ordinal());
    assertEquals(2, DifferentImplementationChoice.ONLY_RETAIN_VERSION_ALREADY_IN_PROJECT.ordinal());
  }

  @Test
  public void name_matchesConstant() {
    for (DifferentImplementationChoice choice : DifferentImplementationChoice.values()) {
      assertEquals(choice, DifferentImplementationChoice.valueOf(choice.name()));
    }
  }

  @Test
  public void toString_returnsName() {
    for (DifferentImplementationChoice choice : DifferentImplementationChoice.values()) {
      assertEquals(choice.name(), choice.toString());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalid_throwsException() {
    DifferentImplementationChoice.valueOf("BOGUS");
  }

  @Test
  public void allValues_areDistinct() {
    DifferentImplementationChoice[] values = DifferentImplementationChoice.values();
    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i], values[j]);
      }
    }
  }
}
