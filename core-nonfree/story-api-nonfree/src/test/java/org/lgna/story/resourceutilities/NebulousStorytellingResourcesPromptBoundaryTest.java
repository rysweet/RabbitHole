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

public class NebulousStorytellingResourcesPromptBoundaryTest {
  private static final String MODEL_GALLERY_PREFERENCE_KEY = "MODEL_GALLERY_PREFRENCE_KEY";
  private static final String NEBULOUS_RESOURCE_DIRECTORY_PREF_KEY = "NEBULOUS_RESOURCE_DIRECTORY_PREF_KEY";

  private String originalRootDirectory;
  private String originalNebulousPreference;
  private String originalGalleryPreference;
  private String originalModelGalleryPreference;

  @Before
  public void isolateResourceState() throws Exception {
    originalRootDirectory = System.getProperty("org.alice.ide.rootDirectory");
    Preferences preferences = Preferences.userRoot();
    originalNebulousPreference = preferences.get(NEBULOUS_RESOURCE_DIRECTORY_PREF_KEY, "");
    originalGalleryPreference = preferences.get(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, "");
    originalModelGalleryPreference = preferences.get(MODEL_GALLERY_PREFERENCE_KEY, "");

    File emptyInstall = Files.createTempDirectory("alice-empty-sims-install").toFile();
    System.setProperty("org.alice.ide.rootDirectory", emptyInstall.getAbsolutePath());
    preferences.put(NEBULOUS_RESOURCE_DIRECTORY_PREF_KEY, "");
    preferences.put(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, "");
    preferences.put(MODEL_GALLERY_PREFERENCE_KEY, "");
    ResourcePathManager.clearPaths(ResourcePathManager.SIMS_RESOURCE_KEY);
    resetNebulousResources();
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
    preferences.put(NEBULOUS_RESOURCE_DIRECTORY_PREF_KEY, originalNebulousPreference == null ? "" : originalNebulousPreference);
    preferences.put(StorytellingResources.GALLERY_DIRECTORY_PREF_KEY, originalGalleryPreference == null ? "" : originalGalleryPreference);
    preferences.put(MODEL_GALLERY_PREFERENCE_KEY, originalModelGalleryPreference == null ? "" : originalModelGalleryPreference);
    ResourcePathManager.clearPaths(ResourcePathManager.SIMS_RESOURCE_KEY);
    resetNebulousResources();
    setStaticField(StoryApiDirectoryUtilities.class, "modelGalleryDirectory", null);
    UiPrompts.reset();
  }

  @Test
  public void missingSimsResourcesUseBoundaryInsteadOfSwingDialogs() {
    RecordingBoundary boundary = new RecordingBoundary();
    UiPrompts.install(boundary);

    NebulousStorytellingResources.INSTANCE.loadSimsBundles();

    assertFalse(boundary.resourceRequests.isEmpty());
    assertFalse(boundary.messages.isEmpty());
    assertTrue(boundary.resourceRequests.stream().anyMatch(request -> "The Sims (TM) 2 Art Assets".equals(request.resourceName())));
    assertEquals("Cannot find The Sims (TM) 2 Art Assets.", boundary.messages.get(boundary.messages.size() - 1).message().split("\\n")[0]);
  }

  @Test
  public void reusableNebulousResourceSourceDoesNotDirectlyImportSwingDialogs() throws Exception {
    assertSourceDoesNotContain("src/main/java/org/lgna/story/resourceutilities/NebulousStorytellingResources.java", "javax.swing.JOptionPane", "FindResourcesPanel");
  }

  @SuppressWarnings("unchecked")
  private static void resetNebulousResources() throws Exception {
    Field field = NebulousStorytellingResources.INSTANCE.getClass().getDeclaredField("simsPathsLoaded");
    field.setAccessible(true);
    ((List<File>) field.get(NebulousStorytellingResources.INSTANCE)).clear();
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
    private final List<ResourcePromptRequest> resourceRequests = new ArrayList<>();
    private final List<MessagePromptRequest> messages = new ArrayList<>();

    @Override
    public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
      resourceRequests.add(request);
      return ResourcePromptResult.noSelection();
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
