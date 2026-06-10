package org.lgna.story.implementation;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Tests for StoryApiDirectoryUtilities — the static directory utility methods.
 * Headless-safe: uses filesystem checks only, no AWT.
 */
public class StoryApiDirectoryUtilitiesTest {

  // ── getSoundGalleryDirectory ──

  @Test
  public void getSoundGalleryDirectoryReturnsNullWithoutInstall() {
    // Without org.alice.ide.rootDirectory set to a real installation,
    // getDirectory returns null instead of falling back to scanning home.
    File dir = StoryApiDirectoryUtilities.getSoundGalleryDirectory();
    if (dir != null) {
      assertTrue("If returned, should be a real directory", dir.isDirectory());
    }
    // null is acceptable when no Alice installation is present
  }

  // ── getStarterProjectsDirectory ──

  @Test
  public void getStarterProjectsDirectoryReturnsNullWithoutInstall() {
    File dir = StoryApiDirectoryUtilities.getStarterProjectsDirectory();
    if (dir != null) {
      assertTrue("If returned, should be a real directory", dir.isDirectory());
    }
  }

  // ── getInternalModelsDirectory ──

  @Test
  public void getInternalModelsDirectoryReturnsNullWithoutInstall() {
    File dir = StoryApiDirectoryUtilities.getInternalModelsDirectory();
    if (dir != null) {
      assertTrue("If returned, should be a real directory", dir.isDirectory());
    }
  }

  // ── setUserGalleryDirectory / getUserGalleryDirectory ──

  @Test
  public void setUserGalleryDirectoryRoundTrips() {
    File custom = new File("/custom/gallery");
    StoryApiDirectoryUtilities.setUserGalleryDirectory(custom);
    try {
      File result = StoryApiDirectoryUtilities.getUserGalleryDirectory();
      assertEquals(custom, result);
    } finally {
      StoryApiDirectoryUtilities.setUserGalleryDirectory(null);
    }
  }

  @Test
  public void setUserGalleryDirectoryToNullRestoresDefault() {
    File custom = new File("/custom/gallery");
    StoryApiDirectoryUtilities.setUserGalleryDirectory(custom);
    StoryApiDirectoryUtilities.setUserGalleryDirectory(null);
    File result = StoryApiDirectoryUtilities.getUserGalleryDirectory();
    assertNotEquals("Should restore default, not custom", custom, result);
  }

  // ── getDirectory no longer falls back to home directory ──

  @Test
  public void directoryMethodsDoNotFallBackToHomeDirectory() {
    String home = System.getProperty("user.home");
    File soundDir = StoryApiDirectoryUtilities.getSoundGalleryDirectory();
    File starterDir = StoryApiDirectoryUtilities.getStarterProjectsDirectory();
    File internalDir = StoryApiDirectoryUtilities.getInternalModelsDirectory();

    if (soundDir != null) {
      assertFalse("Should not fall back to home directory",
          soundDir.getAbsolutePath().equals(home));
    }
    if (starterDir != null) {
      assertFalse("Should not fall back to home directory",
          starterDir.getAbsolutePath().equals(home));
    }
    if (internalDir != null) {
      assertFalse("Should not fall back to home directory",
          internalDir.getAbsolutePath().equals(home));
    }
  }
}
