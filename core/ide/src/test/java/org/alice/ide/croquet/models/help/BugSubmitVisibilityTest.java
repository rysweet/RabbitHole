package org.alice.ide.croquet.models.help;

import org.junit.Test;
import static org.junit.Assert.*;

public class BugSubmitVisibilityTest {

  @Test
  public void values_hasTwoConstants() {
    assertEquals(2, BugSubmitVisibility.values().length);
  }

  @Test
  public void valueOf_public() {
    assertEquals(BugSubmitVisibility.PUBLIC, BugSubmitVisibility.valueOf("PUBLIC"));
  }

  @Test
  public void valueOf_private() {
    assertEquals(BugSubmitVisibility.PRIVATE, BugSubmitVisibility.valueOf("PRIVATE"));
  }

  @Test
  public void ordinals_correct() {
    assertEquals(0, BugSubmitVisibility.PUBLIC.ordinal());
    assertEquals(1, BugSubmitVisibility.PRIVATE.ordinal());
  }

  @Test
  public void name_matchesConstant() {
    assertEquals("PUBLIC", BugSubmitVisibility.PUBLIC.name());
    assertEquals("PRIVATE", BugSubmitVisibility.PRIVATE.name());
  }

  @Test
  public void toString_matchesName() {
    for (BugSubmitVisibility v : BugSubmitVisibility.values()) {
      assertEquals(v.name(), v.toString());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalid_throws() {
    BugSubmitVisibility.valueOf("UNKNOWN");
  }

  @Test
  public void valuesArray_containsBothConstants() {
    BugSubmitVisibility[] values = BugSubmitVisibility.values();
    assertSame(BugSubmitVisibility.PUBLIC, values[0]);
    assertSame(BugSubmitVisibility.PRIVATE, values[1]);
  }
}
