package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.story.implementation.StoryApiDirectoryUtilities;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

public class StorytellingResourcesPromptBoundaryTest {
  private static final String MODEL_GALLERY_PREFERENCE_KEY = "MODEL_GALLERY_PREFRENCE_KEY";
  private static final String ALICE_RESOURCE_DIRECTORY_PREF_KEY = "ALICE_RESOURCE_DIRECTORY_PREF_KEY";

  private String originalRootDirectory;
  private String originalAlicePreference;
  private String originalGalleryPreference;
  private String originalModelGalleryPreference;

  @Before
  public void isolateResourceState() throws Exception {
    originalRootDirectory = System.getProperty("org.alice.ide.rootDirectory");
    Preferences preferences = Preferences.userRoot();
    originalAlicePreference = preferences.get(ALICE_RESOURCE_DIRECTORY_PREF_KEY, "");
    originalGalleryPreference = preferences.get(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, "");
    originalModelGalleryPreference = preferences.get(MODEL_GALLERY_PREFERENCE_KEY, "");

    File emptyInstall = Files.createTempDirectory("alice-empty-install").toFile();
    System.setProperty("org.alice.ide.rootDirectory", emptyInstall.getAbsolutePath());
    preferences.put(ALICE_RESOURCE_DIRECTORY_PREF_KEY, "");
    preferences.put(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, "");
    preferences.put(MODEL_GALLERY_PREFERENCE_KEY, "");
    ResourcePathManager.clearPaths(ResourcePathManager.MODEL_RESOURCE_KEY);
    resetStorytellingResources();
    setStaticField(StoryApiDirectoryUtilities.class, "modelGalleryDirectory", null);
  }

  @After
  public void restoreResourceState() throws Exception {
    if (originalRootDirectory == null) {
      System.clearProperty("org.alice.ide.rootDirectory");
    } else {
      System.setProperty("org.alice.ide.rootDirectory", originalRootDirectory);
    }
    Preferences preferences = Preferences.userRoot();
    preferences.put(ALICE_RESOURCE_DIRECTORY_PREF_KEY, originalAlicePreference == null ? "" : originalAlicePreference);
    preferences.put(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, originalGalleryPreference == null ? "" : originalGalleryPreference);
    preferences.put(MODEL_GALLERY_PREFERENCE_KEY, originalModelGalleryPreference == null ? "" : originalModelGalleryPreference);
    ResourcePathManager.clearPaths(ResourcePathManager.MODEL_RESOURCE_KEY);
    resetStorytellingResources();
    setStaticField(StoryApiDirectoryUtilities.class, "modelGalleryDirectory", null);
    UiPrompts.reset();
  }

  @Test
  public void missingAliceResourcesUseBoundaryInsteadOfSwingDialogs() {
    RecordingBoundary boundary = new RecordingBoundary();
    UiPrompts.install(boundary);

    assertTrue(StorytellingResources.INSTANCE.findAndLoadInstalledAliceResourcesIfNecessary().isEmpty());

    assertFalse(boundary.resourceRequests.isEmpty());
    assertFalse(boundary.messages.isEmpty());
    assertEquals("Alice gallery resources", boundary.resourceRequests.get(0).resourceName());
    assertEquals("Cannot find the Alice gallery resources.", boundary.messages.get(boundary.messages.size() - 1).message().split("\\n")[0]);
  }

  @Test
  public void explicitGalleryLocationRequestForcesBoundaryPrompt() {
    RecordingBoundary boundary = new RecordingBoundary(new File("/chosen/gallery"));
    UiPrompts.install(boundary);

    StorytellingResources.INSTANCE.getGalleryLocationFromUser();

    assertEquals(1, boundary.resourceRequests.size());
    assertTrue(boundary.resourceRequests.get(0).alwaysPrompt());
    assertEquals("/chosen/gallery", Preferences.userRoot().get(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, ""));
  }

  @Test
  public void reusableStorytellingResourceSourcesDoNotDirectlyImportSwingDialogs() throws Exception {
    assertSourceDoesNotContain("src/main/java/org/lgna/story/resourceutilities/StorytellingResources.java", "javax.swing.JOptionPane", "FindResourcesPanel");
    assertSourceDoesNotContain("src/main/java/org/lgna/story/implementation/StoryApiDirectoryUtilities.java", "FindResourcesPanel");
  }

  private static void resetStorytellingResources() throws Exception {
    setField(StorytellingResources.INSTANCE, "installedAliceClassesLoaded", null);
    setField(StorytellingResources.INSTANCE, "resourceClassLoaders", null);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void setStaticField(Class<?> cls, String name, Object value) throws Exception {
    Field field = cls.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }

  private static void assertSourceDoesNotContain(String path, String... forbidden) throws Exception {
    String source = Files.readString(new File(path).toPath());
    for (String token : forbidden) {
      assertFalse(path + " must not contain " + token, source.contains(token));
    }
  }

  private static class RecordingBoundary implements UiPromptBoundary {
    private final File selectedGalleryDirectory;
    private final List<ResourcePromptRequest> resourceRequests = new ArrayList<>();
    private final List<MessagePromptRequest> messages = new ArrayList<>();

    RecordingBoundary() {
      this(null);
    }

    RecordingBoundary(File selectedGalleryDirectory) {
      this.selectedGalleryDirectory = selectedGalleryDirectory;
    }

    @Override
    public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
      resourceRequests.add(request);
      return selectedGalleryDirectory == null ? ResourcePromptResult.noSelection() : ResourcePromptResult.selected(selectedGalleryDirectory);
    }

    @Override
    public void showMessage(MessagePromptRequest request) {
      messages.add(request);
    }

    @Override
    public boolean requestEulaAcceptance(EulaPromptRequest request) {
      return false;
    }
  }
}
