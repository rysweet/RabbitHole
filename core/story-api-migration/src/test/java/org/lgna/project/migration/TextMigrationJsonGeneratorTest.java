package org.lgna.project.migration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextMigrationJsonGeneratorTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
}
