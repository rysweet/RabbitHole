package org.lgna.story.resourceutilities;

import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for {@link ModelResourceThumbnailWriter}.
 * Same package gives access to the package-private API.
 */
public class ModelResourceThumbnailWriterTest {

  // ── getThumbnailPath ────────────────────────────────────────────

  @Test
  public void getThumbnailPathIncludesPackageAndClassDirectories() {
    String result = ModelResourceThumbnailWriter.getThumbnailPath(
        "/root/", "org.lgna.story.resources.prop", "TestProp", "thumb.png");
    assertNotNull(result);
    // getResourceSubDirWithSeparator lowercases the alice class name
    assertTrue("Should contain lowercased class name in path", result.contains("testprop"));
    assertTrue("Should end with thumbnail name", result.endsWith("thumb.png"));
  }

  @Test
  public void getThumbnailPathHandlesRootWithoutTrailingSlash() {
    String withSlash = ModelResourceThumbnailWriter.getThumbnailPath(
        "/root/", "org.lgna.story.resources.prop", "TestProp", "thumb.png");
    String withoutSlash = ModelResourceThumbnailWriter.getThumbnailPath(
        "/root", "org.lgna.story.resources.prop", "TestProp", "thumb.png");
    assertEquals("Should produce same path regardless of trailing slash", withSlash, withoutSlash);
  }

  // ── createClassThumb ────────────────────────────────────────────

  @Test
  public void createClassThumbReturnsInputImageUnchanged() {
    BufferedImage input = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
    input.setRGB(0, 0, 0xFFFF0000);
    BufferedImage result = ModelResourceThumbnailWriter.createClassThumb(input);
    assertNotNull(result);
    assertEquals(10, result.getWidth());
    assertEquals(10, result.getHeight());
    assertEquals(0xFFFF0000, result.getRGB(0, 0));
  }

  @Test
  public void createClassThumbReturnsSameInstance() {
    BufferedImage input = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
    BufferedImage result = ModelResourceThumbnailWriter.createClassThumb(input);
    assertTrue("Current implementation returns the same instance", input == result);
  }
}
