package org.alice.ide.croquet.models.help;

import org.junit.Test;
import static org.junit.Assert.*;

public class BugSubmitAttachmentTest {

  @Test
  public void values_hasTwoConstants() {
    assertEquals(2, BugSubmitAttachment.values().length);
  }

  @Test
  public void valueOf_yes() {
    assertEquals(BugSubmitAttachment.YES, BugSubmitAttachment.valueOf("YES"));
  }

  @Test
  public void valueOf_no() {
    assertEquals(BugSubmitAttachment.NO, BugSubmitAttachment.valueOf("NO"));
  }

  @Test
  public void ordinals_correct() {
    assertEquals(0, BugSubmitAttachment.YES.ordinal());
    assertEquals(1, BugSubmitAttachment.NO.ordinal());
  }

  @Test
  public void name_matchesConstant() {
    assertEquals("YES", BugSubmitAttachment.YES.name());
    assertEquals("NO", BugSubmitAttachment.NO.name());
  }

  @Test
  public void toString_matchesName() {
    for (BugSubmitAttachment a : BugSubmitAttachment.values()) {
      assertEquals(a.name(), a.toString());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOf_invalid_throws() {
    BugSubmitAttachment.valueOf("MAYBE");
  }

  @Test
  public void valuesArray_containsBothConstants() {
    BugSubmitAttachment[] values = BugSubmitAttachment.values();
    assertSame(BugSubmitAttachment.YES, values[0]);
    assertSame(BugSubmitAttachment.NO, values[1]);
  }
}
