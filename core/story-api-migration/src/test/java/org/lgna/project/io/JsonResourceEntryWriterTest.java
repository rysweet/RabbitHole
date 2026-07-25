package org.lgna.project.io;

import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeManifest;
import org.alice.tweedle.file.TypeReference;
import org.junit.Test;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.ProjectVersion;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.story.SProgram;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonResourceEntryWriter}.
 * Verifies type manifest creation, entry generation, name deduplication,
 * SandDunes→Terrain special case, resource name sanitization,
 * and static helper methods.
 */
public class JsonResourceEntryWriterTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // createTypeManifest
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void createTypeManifestSetsNameAndVersion() {
    NamedUserType type = programType("TestType");
    TypeManifest manifest = JsonResourceEntryWriter.createTypeManifest(type);

    assertEquals("TestType", manifest.description.name);
    assertEquals(ProjectVersion.getCurrentVersion().toString(), manifest.provenance.aliceVersion);
    assertEquals(IoUtilities.TYPE_EXTENSION, manifest.metadata.fileType);
    assertNotNull(manifest.metadata.identifier.name);
    assertEquals(Manifest.ProjectType.Library, manifest.metadata.identifier.type);
  }

  @Test
  public void createTypeManifestUsesTypeIdAsIdentifier() {
    NamedUserType type = programType("Prop");
    TypeManifest manifest = JsonResourceEntryWriter.createTypeManifest(type);
    assertEquals(type.getId().toString(), manifest.metadata.identifier.name);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // manifestResourceNames
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void manifestResourceNamesExtractsNamesFromResourceReferences() {
    Manifest manifest = new ProjectManifest();
    manifest.resources.add(new TypeReference("TypeA", "src/TypeA.twe", "tweedle"));
    manifest.resources.add(new TypeReference("TypeB", "src/TypeB.twe", "tweedle"));

    Set<String> names = JsonResourceEntryWriter.manifestResourceNames(manifest);

    assertTrue(names.contains("TypeA"));
    assertTrue(names.contains("TypeB"));
    assertEquals(2, names.size());
  }

  @Test
  public void manifestResourceNamesSkipsNullNames() {
    Manifest manifest = new ProjectManifest();
    TypeReference ref = new TypeReference(null, "src/Unnamed.twe", "tweedle");
    manifest.resources.add(ref);

    Set<String> names = JsonResourceEntryWriter.manifestResourceNames(manifest);

    assertTrue(names.isEmpty());
  }

  @Test
  public void manifestResourceNamesReturnsEmptySetForEmptyManifest() {
    Manifest manifest = new ProjectManifest();
    Set<String> names = JsonResourceEntryWriter.manifestResourceNames(manifest);
    assertTrue(names.isEmpty());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // collectEntries
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void collectEntriesIncludesVersionEntry() {
    Manifest manifest = new ProjectManifest();
    Set<Resource> resources = new HashSet<>();
    DataSource[] extra = new DataSource[0];

    List<DataSource> entries = JsonResourceEntryWriter.collectEntries(manifest, resources, extra);

    boolean hasVersion = entries.stream()
        .anyMatch(ds -> ProjectIo.VERSION_ENTRY_NAME.equals(ds.getName()));
    assertTrue("collectEntries must include version data source", hasVersion);
  }

  @Test
  public void collectEntriesIncludesProvidedDataSources() {
    Manifest manifest = new ProjectManifest();
    Set<Resource> resources = new HashSet<>();
    DataSource thumbnail = new edu.cmu.cs.dennisc.java.util.zip.ByteArrayDataSource(
        "thumbnail.png", new byte[] {1, 2, 3});
    DataSource[] extra = new DataSource[] {thumbnail};

    List<DataSource> entries = JsonResourceEntryWriter.collectEntries(manifest, resources, extra);

    boolean hasThumbnail = entries.stream()
        .anyMatch(ds -> "thumbnail.png".equals(ds.getName()));
    assertTrue("collectEntries must include provided data sources", hasThumbnail);
  }

  @Test
  public void collectEntriesIncludesResourceEntries() {
    Manifest manifest = new ProjectManifest();
    Set<Resource> resources = new LinkedHashSet<>();
    ImageResource image = imageResource("test.png");
    resources.add(image);
    DataSource[] extra = new DataSource[0];

    List<DataSource> entries = JsonResourceEntryWriter.collectEntries(manifest, resources, extra);

    boolean hasResourceEntry = entries.stream()
        .anyMatch(ds -> ds.getName().startsWith("resources/"));
    assertTrue("collectEntries must include resource entries", hasResourceEntry);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // compareResources (warning printer)
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void compareResourcesDoesNotThrowWhenSetsMatch() {
    ImageResource image = imageResource("same.png");
    Set<Resource> project = Set.of(image);
    Set<Resource> crawled = Set.of(image);

    // Should not throw
    JsonResourceEntryWriter.compareResources(project, crawled);
  }

  @Test
  public void compareResourcesDoesNotThrowWhenCrawledHasExtra() {
    Set<Resource> project = new HashSet<>();
    Set<Resource> crawled = Set.of(imageResource("extra.png"));

    // Should not throw (prints warning only)
    JsonResourceEntryWriter.compareResources(project, crawled);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // getResources
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void getResourcesReturnsEmptySetForTypeWithNoResources() {
    NamedUserType type = programType("EmptyProgram");
    Set<Resource> resources = JsonResourceEntryWriter.getResources(
        type, org.lgna.project.ast.CrawlPolicy.COMPLETE);
    assertTrue(resources.isEmpty());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ResourceNames value object
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void resourceNamesStoresNameAndOriginalFileName() {
    JsonResourceEntryWriter.ResourceNames names =
        new JsonResourceEntryWriter.ResourceNames("friendly", "original.png");
    assertEquals("friendly", names.name);
    assertEquals("original.png", names.originalFileName);
  }

  @Test
  public void resourceNamesStoresNullValues() {
    JsonResourceEntryWriter.ResourceNames names =
        new JsonResourceEntryWriter.ResourceNames(null, null);
    assertNull(names.name);
    assertNull(names.originalFileName);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // restoreResourceNames
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void restoreResourceNamesRevertsSanitizedNames() {
    ImageResource resource = imageResource("safe.png");
    String originalName = "/Users/secret/photo.png";
    String originalFileName = "/Users/secret/original.png";
    resource.setName(originalName);
    resource.setOriginalFileName(originalFileName);

    Map<Resource, JsonResourceEntryWriter.ResourceNames> originals = new IdentityHashMap<>();
    originals.put(resource, new JsonResourceEntryWriter.ResourceNames(originalName, originalFileName));

    resource.setName("sanitized");
    resource.setOriginalFileName("sanitized");

    JsonResourceEntryWriter.restoreResourceNames(originals);

    assertEquals(originalName, resource.getName());
    assertEquals(originalFileName, resource.getOriginalFileName());
  }

  @Test
  public void restoreResourceNamesHandlesEmptyMap() {
    // Should not throw
    JsonResourceEntryWriter.restoreResourceNames(new IdentityHashMap<>());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void dependencyNamesIncludesReferencedUserSupertypeButNotSelf() {
    NamedUserType base = programType("BaseProp");
    NamedUserType derived = new NamedUserType();
    derived.name.setValue("DerivedProp");
    derived.superType.setValue(base);

    List<String> dependencies = JsonResourceEntryWriter.dependencyNames(derived);

    assertNotNull("A type extending a user type must record that dependency", dependencies);
    assertTrue("Dependencies must include the referenced user supertype", dependencies.contains("BaseProp"));
    assertFalse("A type must not list itself as a dependency", dependencies.contains("DerivedProp"));
  }

  @Test
  public void dependencyNamesReturnsNullWhenOnlyBuiltInTypesAreReferenced() {
    NamedUserType type = programType("Standalone");

    assertNull(
        "A type that references only built-in (JavaType) types has no user dependencies",
        JsonResourceEntryWriter.dependencyNames(type));
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static ImageResource imageResource(String fileName) {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent("png", new byte[] {1, 2, 3});
    resource.setWidth(1);
    resource.setHeight(1);
    return resource;
  }
}
