package org.lgna.project.migration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TextMigrationJsonGeneratorTest {
  private static final String COMMITTED_JSON_RESOURCE =
      "core/story-api-migration/src/main/resources/migrations/text-migrations.json";
  private static final String GENERATED_JSON_DRIFT_MESSAGE =
      "Generated text migrations JSON drift: legacy registry JSON must match the committed resource by exact UTF-8 string comparison: "
          + COMMITTED_JSON_RESOURCE
          + ". Run the strict drift check without "
          + TextMigrationParityTestSupport.WRITE_JSON_PROPERTY
          + "; use that property only for explicit regeneration.";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedTextMigrationJsonMatchesCommittedResourceExactly() throws Exception {
    String generatedJson = TextMigrationParityTestSupport.legacyRegistryJson();

    if (Boolean.getBoolean(TextMigrationParityTestSupport.WRITE_JSON_PROPERTY)) {
      TextMigrationParityTestSupport.writeLegacyRegistryJson(TextMigrationParityTestSupport.committedJsonPath());
    }

    assertGeneratedJsonMatchesCommittedText(generatedJson, TextMigrationParityTestSupport.committedJsonPath());
  }

  @Test
  public void generatorIsReadOnlyUnlessExplicitWritePropertyIsSet() throws Exception {
    String previousValue = System.getProperty(TextMigrationParityTestSupport.WRITE_JSON_PROPERTY);
    Path committedJsonPath = TextMigrationParityTestSupport.committedJsonPath();
    FileTime modifiedBefore = Files.getLastModifiedTime(committedJsonPath);
    try {
      System.clearProperty(TextMigrationParityTestSupport.WRITE_JSON_PROPERTY);
      TextMigrationParityTestSupport.legacyRegistryJson();

      assertEquals("Normal generator validation must not modify text-migrations.json",
          modifiedBefore, Files.getLastModifiedTime(committedJsonPath));
    } finally {
      TextMigrationParityTestSupport.restoreProperty(TextMigrationParityTestSupport.WRITE_JSON_PROPERTY, previousValue);
    }
  }

  @Test
  public void strictComparisonFailsOnFormattingOnlyJsonDrift() throws Exception {
    String generatedJson = TextMigrationParityTestSupport.legacyRegistryJson();
    Path formattedPath = temporaryFolder.newFile("formatted-text-migrations.json").toPath();
    Files.writeString(formattedPath, generatedJson + "\n", StandardCharsets.UTF_8);

    AssertionError error = assertThrows(AssertionError.class,
        () -> assertGeneratedJsonMatchesCommittedText(generatedJson, formattedPath));
    assertTrue(error.getMessage().contains("exact UTF-8 string comparison"));
  }

  @Test
  public void strictDriftCheckDoesNotModifyStaleJson() throws Exception {
    String generatedJson = TextMigrationParityTestSupport.legacyRegistryJson();
    Path stalePath = temporaryFolder.newFile("stale-text-migrations.json").toPath();
    String staleJson = generatedJson + "\n";
    Files.writeString(stalePath, staleJson, StandardCharsets.UTF_8);
    FileTime modifiedBefore = Files.getLastModifiedTime(stalePath);

    assertThrows(AssertionError.class, () -> assertGeneratedJsonMatchesCommittedText(generatedJson, stalePath));

    assertEquals("Strict drift check must not rewrite stale JSON",
        staleJson, Files.readString(stalePath, StandardCharsets.UTF_8));
    assertEquals("Strict drift check must leave stale JSON metadata untouched",
        modifiedBefore, Files.getLastModifiedTime(stalePath));
  }

  @Test
  public void generatedTextMigrationJsonMatchesCommittedResourceCanonically() throws Exception {
    Path generatedPath = temporaryFolder.newFile("text-migrations.json").toPath();

    TextMigrationParityTestSupport.writeLegacyRegistryJson(generatedPath);

    assertEquals(TextMigrationParityTestSupport.jsonTree(TextMigrationParityTestSupport.committedJsonPath()),
        TextMigrationParityTestSupport.jsonTree(generatedPath));
  }

  @Test
  public void generatedTextMigrationJsonLoadsToLegacyRegistryDefinitions() throws Exception {
    Path generatedPath = temporaryFolder.newFile("text-migrations.json").toPath();

    TextMigrationParityTestSupport.writeLegacyRegistryJson(generatedPath);

    assertEquals(TextMigrationParityTestSupport.legacyRegistryData(),
        TextMigrationParityTestSupport.dataFromJson(generatedPath));
  }

  @Test
  public void committedTextMigrationJsonRemainsLoadableByDefaultRegistryPath() throws Exception {
    assertTrue(TextMigrationParityTestSupport.committedJsonPath().toFile().isFile());
    assertEquals(TextMigrationParityTestSupport.dataFromJson(TextMigrationParityTestSupport.committedJsonPath()),
        TextMigrationParityTestSupport.runtimeJsonRegistryData());
  }

  private static void assertGeneratedJsonMatchesCommittedText(String generatedJson, Path committedJsonPath) throws Exception {
    assertEquals(GENERATED_JSON_DRIFT_MESSAGE, Files.readString(committedJsonPath, StandardCharsets.UTF_8), generatedJson);
  }
}
