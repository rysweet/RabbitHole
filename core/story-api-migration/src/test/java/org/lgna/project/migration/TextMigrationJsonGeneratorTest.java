package org.lgna.project.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class TextMigrationJsonGeneratorTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void generateTextMigrationJson() throws Exception {
    String previousValue = System.getProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY);
    try {
      System.setProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY, Boolean.TRUE.toString());
      List<MigrationJson> migrations = serialize(TextMigrationRegistry.createAll());
      Path outputPath = resolveOutputPath();
      Files.createDirectories(outputPath.getParent());
      OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), migrations);
      assertTrue(Files.exists(outputPath));
    } finally {
      restoreProperty(previousValue);
    }
  }

  private static List<MigrationJson> serialize(TextMigration[] textMigrations) throws Exception {
    Field pairsField = TextMigration.class.getDeclaredField("pairs");
    pairsField.setAccessible(true);

    Class<?> pairClass = Class.forName(TextMigration.class.getName() + "$Pair");
    Field patternField = pairClass.getDeclaredField("pattern");
    patternField.setAccessible(true);
    Field replacementField = pairClass.getDeclaredField("replacement");
    replacementField.setAccessible(true);

    List<MigrationJson> migrations = new ArrayList<>(textMigrations.length);
    for (TextMigration textMigration : textMigrations) {
      MigrationJson migration = new MigrationJson();
      migration.version = textMigration.getResultVersion().toString();

      Object[] pairs = (Object[]) pairsField.get(textMigration);
      migration.replacements = new ArrayList<>(pairs.length);
      for (Object pair : pairs) {
        ReplacementJson replacement = new ReplacementJson();
        replacement.pattern = ((Pattern) patternField.get(pair)).pattern();
        replacement.replacement = (String) replacementField.get(pair);
        migration.replacements.add(replacement);
      }
      migrations.add(migration);
    }
    return migrations;
  }

  private static Path resolveOutputPath() {
    Path moduleRelative = Paths.get("src/main/resources/migrations/text-migrations.json").toAbsolutePath().normalize();
    if (Files.isDirectory(moduleRelative.getParent())) {
      return moduleRelative;
    }

    Path repoRelative = Paths.get("core/story-api-migration/src/main/resources/migrations/text-migrations.json").toAbsolutePath().normalize();
    if (Files.isDirectory(repoRelative.getParent())) {
      return repoRelative;
    }

    throw new IllegalStateException("Unable to resolve text migration JSON output path");
  }

  private static void restoreProperty(String previousValue) {
    if (previousValue == null) {
      System.clearProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY);
    } else {
      System.setProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY, previousValue);
    }
  }

  static final class MigrationJson {
    public String version;
    public List<ReplacementJson> replacements = new ArrayList<>();
  }

  static final class ReplacementJson {
    public String pattern;
    public String replacement;
  }
}
