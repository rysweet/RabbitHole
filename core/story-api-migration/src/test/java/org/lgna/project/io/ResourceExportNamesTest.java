package org.lgna.project.io;

import org.junit.Test;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourceExportNames}.
 * Verifies file name sanitization, absolute path stripping, entry name
 * validation for resource and source directories, and diagnostic formatting.
 *
 * <p>Security-critical path validation is also integration-tested in
 * IoUtilitiesTest; these tests verify the helper methods directly with
 * broader edge case coverage.
 */
public class ResourceExportNamesTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // entryFileName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void entryFileNamePrefersOriginalFileName() {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName("photo.png");
    resource.setName("friendly name");
    assertEquals("photo.png", ResourceExportNames.entryFileName(resource));
  }

  @Test
  public void entryFileNameFallsBackToNameWhenOriginalIsNull() {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName(null);
    resource.setName("fallback.png");
    assertEquals("fallback.png", ResourceExportNames.entryFileName(resource));
  }

  @Test
  public void entryFileNameFallsBackToNameWhenOriginalIsEmpty() {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName("");
    resource.setName("fallback.png");
    assertEquals("fallback.png", ResourceExportNames.entryFileName(resource));
  }

  @Test
  public void entryFileNameFallsBackToUuidWhenBothAreEmpty() {
    UUID uuid = UUID.randomUUID();
    ImageResource resource = new ImageResource(uuid);
    resource.setOriginalFileName("");
    resource.setName("");
    assertEquals(uuid.toString(), ResourceExportNames.entryFileName(resource));
  }

  @Test
  public void entryFileNameStripsUnixAbsolutePath() {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName("/Users/alice/photos/picture.png");
    assertEquals("picture.png", ResourceExportNames.entryFileName(resource));
  }

  @Test
  public void entryFileNameStripsWindowsAbsolutePath() {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName("C:\\Users\\alice\\photos\\picture.png");
    assertEquals("picture.png", ResourceExportNames.entryFileName(resource));
  }

  @Test
  public void entryFileNameReplacesSlashesInRelativePath() {
    ImageResource resource = new ImageResource(UUID.randomUUID());
    resource.setOriginalFileName("../folder/picture.png");
    assertEquals(".._folder_picture.png", ResourceExportNames.entryFileName(resource));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // metadataName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void metadataNameReturnsFallbackForNull() {
    assertEquals("fallback.png", ResourceExportNames.metadataName(null, "fallback.png"));
  }

  @Test
  public void metadataNameReturnsFallbackForBlank() {
    assertEquals("fallback.png", ResourceExportNames.metadataName("   ", "fallback.png"));
  }

  @Test
  public void metadataNameReturnsNameForRelativePath() {
    assertEquals("picture.png", ResourceExportNames.metadataName("picture.png", "fallback.png"));
  }

  @Test
  public void metadataNameSanitizesAbsoluteUnixPath() {
    assertEquals("picture.png",
        ResourceExportNames.metadataName("/Users/alice/picture.png", "fallback.png"));
  }

  @Test
  public void metadataNameSanitizesAbsoluteWindowsPath() {
    assertEquals("picture.png",
        ResourceExportNames.metadataName("C:\\Users\\alice\\picture.png", "fallback.png"));
  }

  @Test
  public void metadataNameReturnsFallbackForSanitizedEmptyResult() {
    assertEquals("fallback.png",
        ResourceExportNames.metadataName("/", "fallback.png"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // metadataOriginalFileName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void metadataOriginalFileNameReturnsFallbackForNull() {
    assertEquals("fallback.png",
        ResourceExportNames.metadataOriginalFileName(null, "fallback.png"));
  }

  @Test
  public void metadataOriginalFileNameReturnsEmptyForEmptyString() {
    assertEquals("",
        ResourceExportNames.metadataOriginalFileName("", "fallback.png"));
  }

  @Test
  public void metadataOriginalFileNameReturnsFallbackForBlankOnly() {
    // Blank (whitespace only) is treated as empty after trim → ""
    assertEquals("",
        ResourceExportNames.metadataOriginalFileName("   ", "fallback.png"));
  }

  @Test
  public void metadataOriginalFileNamePreservesRelativeName() {
    assertEquals("photo.png",
        ResourceExportNames.metadataOriginalFileName("photo.png", "fallback.png"));
  }

  @Test
  public void metadataOriginalFileNameSanitizesAbsolutePath() {
    assertEquals("photo.png",
        ResourceExportNames.metadataOriginalFileName("/Users/alice/photo.png", "fallback.png"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // fileNameFromEntry
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void fileNameFromEntryExtractsAfterLastSlash() {
    assertEquals("image.png", ResourceExportNames.fileNameFromEntry("resources/image.png"));
  }

  @Test
  public void fileNameFromEntryHandlesNoSlash() {
    assertEquals("image.png", ResourceExportNames.fileNameFromEntry("image.png"));
  }

  @Test
  public void fileNameFromEntryHandlesNestedPath() {
    assertEquals("image.png", ResourceExportNames.fileNameFromEntry("a/b/c/image.png"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // isResourceEntryName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void validResourceEntryNames() {
    assertTrue(ResourceExportNames.isResourceEntryName("resources/image.png"));
    assertTrue(ResourceExportNames.isResourceEntryName("resources2/image.png"));
    assertTrue(ResourceExportNames.isResourceEntryName("resources3/image.png"));
    assertTrue(ResourceExportNames.isResourceEntryName("resources99/deep/image.png"));
  }

  @Test
  public void invalidResourceEntryNames() {
    assertFalse(ResourceExportNames.isResourceEntryName(null));
    assertFalse(ResourceExportNames.isResourceEntryName(""));
    assertFalse(ResourceExportNames.isResourceEntryName("resources"));
    assertFalse(ResourceExportNames.isResourceEntryName("resources/"));
    assertFalse(ResourceExportNames.isResourceEntryName("resources2/"));
    assertFalse(ResourceExportNames.isResourceEntryName("resource/image.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("resourcesx/image.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("resources-2/image.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("resources/../evil.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("resources/./evil.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("resources//evil.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("/resources/evil.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("\\resources\\evil.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("C:/resources/evil.png"));
    assertFalse(ResourceExportNames.isResourceEntryName("src/Program.twe"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // isSourceEntryName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void validSourceEntryNames() {
    assertTrue(ResourceExportNames.isSourceEntryName("src/Program.twe"));
    assertTrue(ResourceExportNames.isSourceEntryName("src/sub/Nested.twe"));
  }

  @Test
  public void invalidSourceEntryNames() {
    assertFalse(ResourceExportNames.isSourceEntryName(null));
    assertFalse(ResourceExportNames.isSourceEntryName(""));
    assertFalse(ResourceExportNames.isSourceEntryName("src"));
    assertFalse(ResourceExportNames.isSourceEntryName("src/"));
    assertFalse(ResourceExportNames.isSourceEntryName("src/../Program.twe"));
    assertFalse(ResourceExportNames.isSourceEntryName("src/./Program.twe"));
    assertFalse(ResourceExportNames.isSourceEntryName("src//Program.twe"));
    assertFalse(ResourceExportNames.isSourceEntryName("/src/Program.twe"));
    assertFalse(ResourceExportNames.isSourceEntryName("source/Program.twe"));
    assertFalse(ResourceExportNames.isSourceEntryName("resources/image.png"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // diagnosticName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void diagnosticNameFormatsFileAndUuid() {
    UUID uuid = UUID.randomUUID();
    ImageResource resource = new ImageResource(uuid);
    resource.setOriginalFileName("test.png");
    assertEquals("test.png (" + uuid + ")", ResourceExportNames.diagnosticName(resource));
  }

  @Test
  public void diagnosticNameHandlesNull() {
    assertEquals("<null>", ResourceExportNames.diagnosticName(null));
  }

  @Test
  public void diagnosticNameStripsAbsolutePathFromDiagnosticOutput() {
    UUID uuid = UUID.randomUUID();
    ImageResource resource = new ImageResource(uuid);
    resource.setOriginalFileName("/Users/secret/private.png");
    String diagnostic = ResourceExportNames.diagnosticName(resource);
    assertFalse(diagnostic.contains("/Users/"));
    assertFalse(diagnostic.contains("secret"));
    assertTrue(diagnostic.contains("private.png"));
  }
}
