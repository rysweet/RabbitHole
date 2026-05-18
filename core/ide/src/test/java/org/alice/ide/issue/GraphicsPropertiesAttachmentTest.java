package org.alice.ide.issue;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link GraphicsPropertiesAttachment} — file name and basic contract.
 */
public class GraphicsPropertiesAttachmentTest {

  @Test
  public void getFileName_returnsGraphicsPropertiesXml() {
    GraphicsPropertiesAttachment attachment = new GraphicsPropertiesAttachment();
    assertEquals("graphicsProperties.xml", attachment.getFileName());
  }

  @Test
  public void getBytes_returnsNonNull() {
    GraphicsPropertiesAttachment attachment = new GraphicsPropertiesAttachment();
    byte[] bytes = attachment.getBytes();
    assertNotNull(bytes);
    assertTrue("Expected non-empty bytes", bytes.length > 0);
  }

  @Test
  public void getBytes_returnsValidUtf8() {
    GraphicsPropertiesAttachment attachment = new GraphicsPropertiesAttachment();
    byte[] bytes = attachment.getBytes();
    // Should be decodable as UTF-8 string
    String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    assertNotNull(content);
    assertFalse(content.isEmpty());
  }
}
