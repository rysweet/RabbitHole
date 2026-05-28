package org.alice.ide.croquet.models.clipboard;

import org.junit.Test;
import org.lgna.croquet.AbstractElement;

import java.util.UUID;

import static org.junit.Assert.*;

public class ClipboardOperationsBehaviorTest {
  @Test
  public void clipboardOperations_getInstance_returnsMemoizedSingletons() {
    assertSame(CopyOperation.getInstance(), CopyOperation.getInstance());
    assertSame(CutOperation.getInstance(), CutOperation.getInstance());
    assertSame(PasteOperation.getInstance(), PasteOperation.getInstance());
  }

  @Test
  public void clipboardOperations_migrationIds_areStableAndDistinct() {
    assertEquals(UUID.fromString("4caee2f0-7d3c-427c-9816-f277bc2fcecb"), CopyOperation.getInstance().getMigrationId());
    assertEquals(UUID.fromString("48a0202c-8153-4772-89ca-08fe5a8f28b5"), CutOperation.getInstance().getMigrationId());
    assertEquals(UUID.fromString("b6c8d189-3529-4244-9530-d71701c6e75f"), PasteOperation.getInstance().getMigrationId());
    assertNotEquals(CopyOperation.getInstance().getMigrationId(), CutOperation.getInstance().getMigrationId());
    assertNotEquals(CopyOperation.getInstance().getMigrationId(), PasteOperation.getInstance().getMigrationId());
    assertNotEquals(CutOperation.getInstance().getMigrationId(), PasteOperation.getInstance().getMigrationId());
  }

  @Test
  public void copyOperation_localizedContent_supportsPlatformSpecificModifierText() {
    String alt = AbstractElement.findLocalizedText(CopyOperation.class, "alt");
    String control = AbstractElement.findLocalizedText(CopyOperation.class, "control");
    String content = AbstractElement.findLocalizedText(CopyOperation.class, "content");

    assertNotNull(alt);
    assertNotNull(control);
    assertNotNull(content);
    assertEquals(2, countOccurrences(content, "%s"));
    assertTrue(content.formatted(control, control).contains(control));
    assertTrue(content.formatted(alt, alt).contains(alt));
    assertEquals("Copy coming soon", AbstractElement.findLocalizedText(CopyOperation.class, "title"));
  }

  @Test
  public void cutAndPasteOperations_localizedMessages_describeDragOnlyWorkflow() {
    String cutContent = AbstractElement.findLocalizedText(CutOperation.class, "content");
    String pasteContent = AbstractElement.findLocalizedText(PasteOperation.class, "content");

    assertNotNull(cutContent);
    assertNotNull(pasteContent);
    assertTrue(cutContent.contains("dragging statements"));
    assertTrue(pasteContent.contains("dragging statements"));
    assertTrue(AbstractElement.findLocalizedText(CutOperation.class, "title").contains("coming soon"));
    assertTrue(AbstractElement.findLocalizedText(PasteOperation.class, "title").contains("coming soon"));
  }

  private static int countOccurrences(String text, String fragment) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(fragment, index)) != -1) {
      count++;
      index += fragment.length();
    }
    return count;
  }
}
