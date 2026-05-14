package org.lgna.project.io;

import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.ResourceReference;
import org.junit.Test;
import org.lgna.common.Resource;
import org.lgna.common.resources.AudioResource;
import org.lgna.common.resources.ImageResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonProjectResourceEntries}.
 * Verifies resource entry name generation, deduplication for duplicate
 * file names, and correct manifest resource references.
 */
public class JsonProjectResourceEntriesTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // addResources: empty set
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addResourcesWithEmptySetAddsNothing() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new HashSet<>();

    JsonProjectResourceEntries.addResources(manifest, dataSources, resources);

    assertTrue(dataSources.isEmpty());
    assertTrue(manifest.resources.isEmpty());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // addResources: single resource
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addResourcesCreatesSingleEntryForSingleResource() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new LinkedHashSet<>();
    ImageResource image = imageResource("picture.png");
    resources.add(image);

    JsonProjectResourceEntries.addResources(manifest, dataSources, resources);

    assertEquals(1, dataSources.size());
    assertEquals("resources/picture.png", dataSources.get(0).getName());
    assertEquals(1, manifest.resources.size());
    assertEquals("resources/picture.png", manifest.resources.get(0).file);
    assertEquals("picture.png", manifest.resources.get(0).name);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // addResources: duplicate filenames get incrementing directory suffix
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addResourcesDeduplicatesDuplicateFilenames() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new LinkedHashSet<>();
    resources.add(imageResource("image.png"));
    resources.add(imageResource("image.png"));

    JsonProjectResourceEntries.addResources(manifest, dataSources, resources);

    assertEquals(2, dataSources.size());
    Set<String> entryNames = new HashSet<>();
    for (DataSource ds : dataSources) {
      entryNames.add(ds.getName());
    }
    assertTrue(entryNames.contains("resources/image.png"));
    assertTrue(entryNames.contains("resources2/image.png"));
  }

  @Test
  public void addResourcesHandlesThreeDuplicateFilenames() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new LinkedHashSet<>();
    resources.add(imageResource("dup.png"));
    resources.add(imageResource("dup.png"));
    resources.add(imageResource("dup.png"));

    JsonProjectResourceEntries.addResources(manifest, dataSources, resources);

    assertEquals(3, dataSources.size());
    Set<String> entryNames = new HashSet<>();
    for (DataSource ds : dataSources) {
      entryNames.add(ds.getName());
    }
    assertTrue(entryNames.contains("resources/dup.png"));
    assertTrue(entryNames.contains("resources2/dup.png"));
    assertTrue(entryNames.contains("resources3/dup.png"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // addResources: audio resource reference
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addResourcesCreatesAudioReferenceForAudioResource() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new LinkedHashSet<>();
    AudioResource audio = audioResource("sound.wav");
    resources.add(audio);

    JsonProjectResourceEntries.addResources(manifest, dataSources, resources);

    assertEquals(1, dataSources.size());
    assertEquals("resources/sound.wav", dataSources.get(0).getName());
    assertEquals(1, manifest.resources.size());
    ResourceReference ref = manifest.resources.get(0);
    assertEquals("sound.wav", ref.name);
    assertEquals("resources/sound.wav", ref.file);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // addResources: unknown resource type throws
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addResourcesThrowsForUnknownResourceType() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new LinkedHashSet<>();
    resources.add(new UnknownResource());

    assertThrows(RuntimeException.class,
        () -> JsonProjectResourceEntries.addResources(manifest, dataSources, resources));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // addResources: mixed resources
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addResourcesHandlesMixedResourceTypes() {
    Manifest manifest = new ProjectManifest();
    List<DataSource> dataSources = new ArrayList<>();
    Set<Resource> resources = new LinkedHashSet<>();
    resources.add(imageResource("photo.png"));
    resources.add(audioResource("music.wav"));

    JsonProjectResourceEntries.addResources(manifest, dataSources, resources);

    assertEquals(2, dataSources.size());
    assertEquals(2, manifest.resources.size());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private static ImageResource imageResource(String fileName) {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent("png", new byte[] {1, 2, 3});
    resource.setWidth(1);
    resource.setHeight(1);
    return resource;
  }

  private static AudioResource audioResource(String fileName) {
    AudioResource resource = new AudioResource(UUID.randomUUID());
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent("audio.x_wav", new byte[] {0, 1, 2, 3});
    resource.setDuration(1.0);
    return resource;
  }

  private static class UnknownResource extends Resource {
    UnknownResource() {
      super("unknown.bin", "application/octet-stream", new byte[] {0});
    }
  }
}
