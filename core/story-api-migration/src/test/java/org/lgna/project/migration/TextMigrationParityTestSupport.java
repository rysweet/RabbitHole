package org.lgna.project.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

final class TextMigrationParityTestSupport {
  static final String WRITE_JSON_PROPERTY = "org.lgna.project.migration.TextMigrationJsonGenerator.write";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Field PAIRS_FIELD = declaredField(TextMigration.class, "pairs");
  private static final Class<?> PAIR_CLASS = pairClass();
  private static final Field PATTERN_FIELD = declaredField(PAIR_CLASS, "pattern");
  private static final Field REPLACEMENT_FIELD = declaredField(PAIR_CLASS, "replacement");

  static TextMigration[] legacyRegistrySequence() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();
    TextMigration[] v3134 = TextMigrationRegistryV3134.create();
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();
    TextMigration[] v3159 = TextMigrationRegistryV3159.create();
    TextMigration[] late = TextMigrationRegistryLateVersions.create();

    TextMigration[] all = new TextMigration[early.length + v3134.length + mid.length + v3159.length + late.length];
    int offset = 0;
    System.arraycopy(early, 0, all, offset, early.length);
    offset += early.length;
    System.arraycopy(v3134, 0, all, offset, v3134.length);
    offset += v3134.length;
    System.arraycopy(mid, 0, all, offset, mid.length);
    offset += mid.length;
    System.arraycopy(v3159, 0, all, offset, v3159.length);
    offset += v3159.length;
    System.arraycopy(late, 0, all, offset, late.length);
    return all;
  }

  static TextMigration[] runtimeJsonRegistrySequence() throws Exception {
    return withUseLegacyRegistriesProperty(null, TextMigrationRegistry::createAll);
  }

  static TextMigration[] legacyPropertyRegistrySequence() throws Exception {
    return withUseLegacyRegistriesProperty(Boolean.TRUE.toString(), TextMigrationRegistry::createAll);
  }

  static List<MigrationData> legacyRegistryData() throws Exception {
    return dataOf(legacyRegistrySequence());
  }

  static List<MigrationData> runtimeJsonRegistryData() throws Exception {
    return dataOf(runtimeJsonRegistrySequence());
  }

  static List<MigrationData> legacyPropertyRegistryData() throws Exception {
    return dataOf(legacyPropertyRegistrySequence());
  }

  static List<MigrationData> loadedJsonData() throws Exception {
    return dataOf(TextMigrationJsonLoader.load());
  }

  static List<MigrationData> dataOf(TextMigration[] migrations) throws Exception {
    List<MigrationData> data = new ArrayList<>(migrations.length);
    for (TextMigration migration : migrations) {
      data.add(new MigrationData(migration.getResultVersion().toString(), pairsOf(migration)));
    }
    return data;
  }

  static List<String> versionsOf(List<MigrationData> migrations) {
    List<String> versions = new ArrayList<>(migrations.size());
    for (MigrationData migration : migrations) {
      versions.add(migration.version);
    }
    return versions;
  }

  static List<PairData> pairsOf(TextMigration textMigration) throws Exception {
    Object[] pairs = (Object[]) PAIRS_FIELD.get(textMigration);
    List<PairData> data = new ArrayList<>(pairs.length);
    for (Object pair : pairs) {
      data.add(new PairData(((Pattern) PATTERN_FIELD.get(pair)).pattern(), (String) REPLACEMENT_FIELD.get(pair)));
    }
    return data;
  }

  static TextMigration migrationForVersion(TextMigration[] migrations, String version) {
    for (TextMigration migration : migrations) {
      if (migration.getResultVersion().toString().equals(version)) {
        return migration;
      }
    }
    throw new AssertionError("No migration found for version " + version);
  }

  static boolean containsPair(TextMigration migration, String pattern, String replacement) throws Exception {
    return pairsOf(migration).contains(new PairData(pattern, replacement));
  }

  static TextMigration[] parseJson(String json) throws IOException {
    return toTextMigrations(OBJECT_MAPPER.readValue(json, TextMigrationJsonLoader.MigrationJson[].class));
  }

  static List<MigrationData> dataFromJson(Path jsonPath) throws Exception {
    return dataOf(toTextMigrations(OBJECT_MAPPER.readValue(jsonPath.toFile(), TextMigrationJsonLoader.MigrationJson[].class)));
  }

  static JsonNode jsonTree(Path jsonPath) throws IOException {
    return OBJECT_MAPPER.readTree(jsonPath.toFile());
  }

  static void writeLegacyRegistryJson(Path outputPath) throws Exception {
    Path parent = outputPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(outputPath, legacyRegistryJson(), StandardCharsets.UTF_8);
  }

  static String legacyRegistryJson() throws Exception {
    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(serialize(legacyRegistrySequence()));
  }

  static Path committedJsonPath() {
    Path moduleRelative = Paths.get("src/main/resources/migrations/text-migrations.json").toAbsolutePath().normalize();
    if (Files.isRegularFile(moduleRelative)) {
      return moduleRelative;
    }

    Path repoRelative = Paths.get("core/story-api-migration/src/main/resources/migrations/text-migrations.json").toAbsolutePath().normalize();
    if (Files.isRegularFile(repoRelative)) {
      return repoRelative;
    }

    throw new IllegalStateException("Unable to resolve committed text migration JSON");
  }

  private static List<TextMigrationJsonLoader.MigrationJson> serialize(TextMigration[] textMigrations) throws Exception {
    List<TextMigrationJsonLoader.MigrationJson> migrations = new ArrayList<>(textMigrations.length);
    for (TextMigration textMigration : textMigrations) {
      TextMigrationJsonLoader.MigrationJson migration = new TextMigrationJsonLoader.MigrationJson();
      migration.version = textMigration.getResultVersion().toString();

      List<PairData> pairs = pairsOf(textMigration);
      migration.replacements = new TextMigrationJsonLoader.ReplacementJson[pairs.size()];
      int i = 0;
      for (PairData pair : pairs) {
        TextMigrationJsonLoader.ReplacementJson replacement = new TextMigrationJsonLoader.ReplacementJson();
        replacement.pattern = pair.pattern;
        replacement.replacement = pair.replacement;
        migration.replacements[i++] = replacement;
      }
      migrations.add(migration);
    }
    return migrations;
  }

  private static TextMigration[] toTextMigrations(TextMigrationJsonLoader.MigrationJson[] migrations) {
    TextMigration[] textMigrations = new TextMigration[migrations.length];
    for (int i = 0; i < migrations.length; i++) {
      textMigrations[i] = migrations[i].toTextMigration();
    }
    return textMigrations;
  }

  private static TextMigration[] withUseLegacyRegistriesProperty(String value, Callable<TextMigration[]> supplier) throws Exception {
    String previousValue = System.getProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY);
    try {
      if (value == null) {
        System.clearProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY);
      } else {
        System.setProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY, value);
      }
      return supplier.call();
    } finally {
      restoreProperty(TextMigrationRegistry.USE_LEGACY_REGISTRIES_PROPERTY, previousValue);
    }
  }

  static void restoreProperty(String propertyName, String previousValue) {
    if (previousValue == null) {
      System.clearProperty(propertyName);
    } else {
      System.setProperty(propertyName, previousValue);
    }
  }

  private static Field declaredField(Class<?> owner, String name) {
    try {
      Field field = owner.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private static Class<?> pairClass() {
    try {
      return Class.forName(TextMigration.class.getName() + "$Pair");
    } catch (ClassNotFoundException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  static final class MigrationData {
    final String version;
    final List<PairData> pairs;

    MigrationData(String version, List<PairData> pairs) {
      this.version = version;
      this.pairs = new ArrayList<>(pairs);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof MigrationData)) {
        return false;
      }
      MigrationData that = (MigrationData) other;
      return Objects.equals(this.version, that.version) && Objects.equals(this.pairs, that.pairs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.version, this.pairs);
    }

    @Override
    public String toString() {
      return "MigrationData{version='" + this.version + "', pairs=" + this.pairs + "}";
    }
  }

  static final class PairData {
    final String pattern;
    final String replacement;

    PairData(String pattern, String replacement) {
      this.pattern = pattern;
      this.replacement = replacement;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof PairData)) {
        return false;
      }
      PairData pairData = (PairData) other;
      return Objects.equals(this.pattern, pairData.pattern) && Objects.equals(this.replacement, pairData.replacement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.pattern, this.replacement);
    }

    @Override
    public String toString() {
      return "PairData{pattern='" + this.pattern + "', replacement='" + this.replacement + "'}";
    }
  }

  private TextMigrationParityTestSupport() {
  }
}
